package database.promos.handlers.PromosDBV;

import database.commons.DBHandler;
import database.customers.models.CustomerBillingInformation;
import database.insurances.InsurancesDBV;
import database.insurances.handlers.InsurancesDBV.models.Insurance;
import database.parcel.GuiappDBV;
import database.parcel.PackingsDBV;
import database.parcel.ParcelsPackagesDBV;
import database.parcel.enums.SHIPMENT_TYPE;
import database.parcel.enums.TYPE_SERVICE;
import database.promos.PromosDBV;
import database.promos.enums.DISCOUNT_TYPES;
import database.promos.handlers.PromosDBV.models.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.promos.PromosDBV.SERVICE;
import static service.commons.Constants.*;
import static utils.UtilsDate.getDateConvertedTimeZone;
import static utils.UtilsDate.timezone;

public class CalculateMultiplePromo extends DBHandler<PromosDBV> {

    public CalculateMultiplePromo(PromosDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body().copy();
            String origin = body.getString(_ORIGIN);
            String service = body.getString(SERVICE);
            int terminalOriginId = body.getInteger(_TERMINAL_ORIGIN_ID);
            int terminalDestinyId = body.getInteger(_TERMINAL_DESTINY_ID);
            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(body.getString(_SHIPMENT_TYPE));
            Integer customerBillingInfoId = body.getInteger(_CUSTOMER_BILLING_INFORMATION_ID, 0);
            boolean paysSender = body.getBoolean(_PAYS_SENDER, false);
            Integer senderId = body.getInteger(_SENDER_ID);
            Integer addresseeId = body.getInteger(_ADDRESSEE_ID);
            List<ParcelPackage> parcelPackages = mapParcelPackages(body.getJsonArray(_PARCEL_PACKAGES));
            List<ParcelPacking> parcelPackings = mapParcelPackings(body.getJsonArray(_PARCEL_PACKINGS, new JsonArray()));
            Integer customerId = paysSender ? senderId : addresseeId;
            Integer secondCustomerId = paysSender ? addresseeId : senderId;
            Double invoiceValue = body.getDouble(_INVOICE_VALUE, null);
            Double invoiceValuePerPackage = null;
            if (Objects.nonNull(invoiceValue)) {
                int qtyPackages = parcelPackages.stream().mapToInt(ParcelPackage::getQuantity).sum();
                invoiceValuePerPackage = invoiceValue / qtyPackages;
            }
            Double insuranceValue = body.getDouble(_INSURANCE_VALUE, 0.0);
            Insurance insurance = new Insurance();
            insurance.setInsuranceValue(insuranceValue);

            List<CompletableFuture<JsonObject>> tasksGetCost = new ArrayList<>();
            for (ParcelPackage parcelPackage : parcelPackages) {
                tasksGetCost.add(getCostPackage(parcelPackage, terminalOriginId, terminalDestinyId, invoiceValuePerPackage));
            }
            for (ParcelPacking parcelPacking : parcelPackings) {
                tasksGetCost.add(getCostPacking(parcelPacking));
            }
            if (insuranceValue > 0) {
                tasksGetCost.add(getCostInsurance(service, insurance));
            }

            CompletableFuture.allOf(tasksGetCost.toArray(new CompletableFuture[tasksGetCost.size()])).whenComplete((resultCost, errCost) -> {
                try {
                    if (Objects.nonNull(errCost)) {
                        throw errCost;
                    }

                    List<ParcelPackagePromo> parcelPackagePromoList = new ArrayList<>();
                    List<CompletableFuture<Boolean>> tasksGetPromos = new ArrayList<>();
                    for (ParcelPackage parcelPackage : parcelPackages) {
                        ParcelPackagePromo parcelPackagePromo = new ParcelPackagePromo(parcelPackage);
                        parcelPackagePromoList.add(parcelPackagePromo);
                        tasksGetPromos.add(getCustomerPromoValidations(parcelPackagePromo, service, origin, paysSender, senderId, addresseeId));
                    }

                    CompletableFuture.allOf(tasksGetPromos.toArray(new CompletableFuture[tasksGetPromos.size()])).whenComplete((resultPromosList, errPromosList) -> {
                        try {
                            if (Objects.nonNull(errPromosList)) {
                                throw errPromosList;
                            }

                            List<CompletableFuture<ParcelPackage>> taskCalculate = new ArrayList<>();
                            for (ParcelPackagePromo parcelPackagePromo : parcelPackagePromoList) {
                                Promo promo = parcelPackagePromo.getPromo();
                                if (Objects.nonNull(promo)) {
                                    parcelPackagePromo.getParcelPackage().setPromoId(promo.getId());
                                    parcelPackagePromo.getParcelPackage().setCustomersPromosId(promo.getCustomersPromosId());
                                    parcelPackagePromo.getParcelPackage().setApplyRad(promo.isApplyRad());
                                    parcelPackagePromo.getParcelPackage().setApplyEad(promo.isApplyEad());
                                    parcelPackagePromo.getParcelPackage().setPromoAppliedInfo(new PromoAppliedInfo(promo.getId(), promo.getDiscountCode(), promo.getName(), promo.getDescription(), promo.getDiscountType().name(), promo.getDiscount(), promo.isApplyRad(), promo.isApplyEad(), promo.isApplySenderAddressee()));
                                    DISCOUNT_TYPES discountType = promo.getDiscountType();
                                    taskCalculate.add(calculate(discountType, parcelPackagePromo.getParcelPackage(), promo.getDiscount()));
                                }
                            }

                            CompletableFuture.allOf(taskCalculate.toArray(new CompletableFuture[taskCalculate.size()])).whenComplete((resultCalculate, errCalculate) -> {
                                try {
                                    if (Objects.nonNull(errCalculate)) {
                                        throw errCalculate;
                                    }

                                    if (Objects.isNull(invoiceValue)) {
                                        applyDiscountExcess(message, parcelPackagePromoList, parcelPackings, insurance, service, origin, customerId, secondCustomerId, terminalOriginId, terminalDestinyId, shipmentType, customerBillingInfoId);
                                    } else {
                                        finishProcess(message, parcelPackagePromoList, parcelPackings, insurance, shipmentType, customerId, customerBillingInfoId);
                                    }

                                } catch (Throwable t) {
                                    reportQueryError(message, t);
                                }
                            });

                        } catch (Throwable t) {
                            reportQueryError(message, t);
                        }
                    });

                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private List<ParcelPackage> mapParcelPackages(JsonArray packages) {
        return packages.stream().map(p -> {
            JsonObject parcelPackage = (JsonObject) p;
            return parcelPackage.mapTo(ParcelPackage.class);
        }).collect(Collectors.toList());
    }

    private List<ParcelPacking> mapParcelPackings(JsonArray packings) {
        return packings.stream().map(p -> {
            JsonObject parcelPacking = (JsonObject) p;
            return parcelPacking.mapTo(ParcelPacking.class);
        }).collect(Collectors.toList());
    }

    private CompletableFuture<JsonObject> getCostPackage(ParcelPackage parcelPackage, int terminalOriginId, int terminalDestinyId, Double invoiceValue) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String shippingType = parcelPackage.getShippingType();
            Double weight = parcelPackage.getWeight();
            Double width = parcelPackage.getWidth();
            Double height = parcelPackage.getHeight();
            Double length = parcelPackage.getLength();

            JsonObject body = new JsonObject()
                    .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                    .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                    .put(_SHIPPING_TYPE, shippingType)
                    .put(_WEIGHT, weight).put(_WIDTH, width)
                    .put(_HEIGHT, height).put(_LENGTH, length);

            if(shippingType.equals("parcel")) {
                body.put(_LINEAR_VOLUME, Float.valueOf(String.format("%.3f", (width * height * length) / 1000000)));
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.CALCULATE_COST_V2);
            this.getVertx().eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), body, options, (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }

                    JsonObject packageJson = JsonObject.mapFrom(parcelPackage).mergeIn(reply.result().body(), true);
                    if(Objects.nonNull(invoiceValue)) {
                        parcelPackage.setValuesInvoiceValue(packageJson, invoiceValue);
                    } else {
                        parcelPackage.setValues(packageJson);
                    }
                    future.complete(packageJson);
                } catch(Throwable t){
                    future.completeExceptionally(t);

                }

            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }

        return future;
    }

    private CompletableFuture<JsonObject> getCostPacking(ParcelPacking parcelPacking) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject()
                    .put(_PACKING_ID, parcelPacking.getPackingId())
                    .put(_QUANTITY, parcelPacking.getQuantity());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PackingsDBV.ACTION_GET_COST);
            this.getVertx().eventBus().send(PackingsDBV.class.getSimpleName(), body, options, (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }

                    JsonObject packingJson = JsonObject.mapFrom(parcelPacking).mergeIn(reply.result().body(), true);
                    parcelPacking.setValues(packingJson);
                    future.complete(packingJson);
                } catch(Throwable t){
                    future.completeExceptionally(t);

                }

            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getCostInsurance(String service, Insurance insurance) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject()
                    .put(_SERVICE, service)
                    .put(_INSURANCE_VALUE, insurance.getInsuranceValue());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InsurancesDBV.GET_COST);
            this.getVertx().eventBus().send(InsurancesDBV.class.getSimpleName(), body, options, (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }

                    JsonObject insuranceJson = JsonObject.mapFrom(insurance).mergeIn(reply.result().body(), true);
                    insurance.setValues(insuranceJson);
                    future.complete(insuranceJson);
                } catch(Throwable t){
                    future.completeExceptionally(t);

                }

            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> getCustomerPromoValidations(ParcelPackagePromo parcelPackagePromo, String service, String origin, Boolean paysSender, Integer senderId, Integer addresseeId) throws ParseException {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getCustomerPromo(parcelPackagePromo, service, origin, senderId).whenComplete((promoSender, errPromoSender) -> {
            try {
                if (errPromoSender != null) {
                    throw errPromoSender;
                }
                if (paysSender || (Objects.nonNull(promoSender) && promoSender.isApplySenderAddressee())) {
                    if (promoSender != null) {
                        parcelPackagePromo.setPromo(promoSender);
                    }
                    future.complete(true);
                } else {
                    getCustomerPromo(parcelPackagePromo, service, origin, addresseeId).whenComplete((promoAddressee, errPromoAddressee) -> {
                        try {
                            if (errPromoAddressee != null) {
                                throw errPromoAddressee;
                            }
                            if (promoAddressee != null) {
                                parcelPackagePromo.setPromo(promoAddressee);
                            }
                            future.complete(true);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Promo> getCustomerPromo(ParcelPackagePromo parcelPackagePromo, String service, String origin, Integer customerId) throws ParseException {
        CompletableFuture<Promo> future = new CompletableFuture<>();
        ParcelPackage parcelPackage = parcelPackagePromo.getParcelPackage();
        String today = UtilsDate.format_yyyy_MM_dd(getDateConvertedTimeZone(timezone, new Date()));
        JsonArray params = new JsonArray()
                .add(service)
                .add(origin)
                .add(today)
                .add(parcelPackage.getPackagePriceName())
                .add(parcelPackage.getPackagePriceDistanceId())
                .add(parcelPackage.getPackageTypeId())
                .add(parcelPackage.getShippingType())
                .add(today)
                .add(customerId);
        dbClient.queryWithParams(QUERY_GET_PROMO_BY_PACKAGE, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                Promo promo = null;
                if(!result.isEmpty()) {
                    promo = result.get(0).mapTo(Promo.class);
                }
                future.complete(promo);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<ParcelPackage> calculate(DISCOUNT_TYPES discountType, ParcelPackage parcelPackage, Double referenceDiscount){
        CompletableFuture<ParcelPackage> future = new CompletableFuture<>();
        try {
            parcelPackage.setAmount(parcelPackage.getCost());
            Double amount = parcelPackage.getAmount();
            Double discount = parcelPackage.getDiscount();
            Double totalAmount = parcelPackage.getTotalAmount();
            if (discountType.isFree()){
                double operation = discount + totalAmount;
                future.complete(execCalculate(parcelPackage, amount, discount, operation, true));
            } else if (discountType.isAsPrice()){
                double operation;
                if (referenceDiscount >= totalAmount) {
                    amount = referenceDiscount;
                    operation = 0.0;
                } else {
                    operation = totalAmount - referenceDiscount;
                }
                future.complete(execCalculate(parcelPackage, amount, discount, operation, false));
            } else {
                if (discountType.isPercent()){
                    double operation = (amount * (referenceDiscount / 100));
                    future.complete(execCalculate(parcelPackage, amount, discount, operation, false));
                } else {
                    future.complete(execCalculate(parcelPackage, amount, discount, referenceDiscount, false));
                }
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private ParcelPackage execCalculate(ParcelPackage parcelPackage, Double amount, Double discount, Double operation, Boolean appliedToAmount){
        if (appliedToAmount){
            discount = operation;
        } else {
            discount = discount + operation;
        }
        double finalAmount = UtilsMoney.round(amount, 2);
        double finalDiscount = UtilsMoney.round(discount, 2);
        double finalTotalAmount = UtilsMoney.round(finalAmount - finalDiscount, 2);
        parcelPackage.setCost(finalAmount);
        parcelPackage.setAmount(finalAmount);
        parcelPackage.setTotalAmount(finalTotalAmount);
        parcelPackage.setDiscount(finalDiscount);
        parcelPackage.setPromoDiscount(UtilsMoney.round(operation, 2));
        return parcelPackage;
    }

    private Service getTotals(List<ParcelPackagePromo> parcelPackagePromoList, List<ParcelPacking> parcelPackings, Insurance insurance, double iva, double parcelIva, SHIPMENT_TYPE shipmentType, TypeServiceCost typeServiceCost, CustomerBillingInformation customerBillingInformation) {
        double amountBeforeIva = 0.0;
        double excessAmountBeforeIva = 0.0;
        double discountBeforeIva = 0.0;
        double excessDiscountBeforeIva = 0.0;
        double servicesAmountBeforeIva = 0.0;
        double totalIva = 0.0;
        double totalParcelIva = 0.0;

        boolean flagPromoApplyRad = parcelPackagePromoList.stream().anyMatch(p -> Objects.nonNull(p.getParcelPackage().getPromoAppliedInfo()) && p.getParcelPackage().getPromoAppliedInfo().isApplyRad());
        boolean flagPromoApplyEad = parcelPackagePromoList.stream().anyMatch(p -> Objects.nonNull(p.getParcelPackage().getPromoAppliedInfo()) && p.getParcelPackage().getPromoAppliedInfo().isApplyEad());
        SubstractServices substractServices = getSubstractServices(shipmentType, typeServiceCost, flagPromoApplyRad, flagPromoApplyEad);

        double totalSubstractServicesByPackage = substractServices.getTotal() / parcelPackagePromoList.stream().map(ParcelPackagePromo::getParcelPackage)
                .mapToInt(ParcelPackage::getQuantity)
                .sum();

        for (ParcelPackagePromo parcelPackagePromo : parcelPackagePromoList) {
            JsonObject costBreakdown = this.calculatePackageCostBreakdown(parcelPackagePromo, iva, parcelIva, customerBillingInformation, totalSubstractServicesByPackage);
            amountBeforeIva += costBreakdown.getDouble(_AMOUNT_BEFORE_IVA);
            excessAmountBeforeIva += costBreakdown.getDouble(_EXCESS_AMOUNT_BEFORE_IVA);
            discountBeforeIva += costBreakdown.getDouble(_DISCOUNT_BEFORE_IVA);
            excessDiscountBeforeIva += costBreakdown.getDouble(_EXCESS_DISCOUNT_BEFORE_IVA);
            servicesAmountBeforeIva += costBreakdown.getDouble(_SERVICES_AMOUNT_BEFORE_IVA);
            totalParcelIva += costBreakdown.getDouble(_TOTAL_PARCEL_IVA);
        }

        double extraChargesBeforeIva = 0.0;
        for (ParcelPacking parcelPacking : parcelPackings) {
            double packingAmount = UtilsMoney.round(parcelPacking.getAmount(), 2);
            double packingIva = UtilsMoney.round(parcelPacking.getIva(), 2);
            double packingDiscount = UtilsMoney.round(parcelPacking.getDiscount(), 2);
            extraChargesBeforeIva += packingAmount;
            amountBeforeIva += packingAmount;
            totalIva += packingIva;
            discountBeforeIva += packingDiscount;
        }

        amountBeforeIva += insurance.getInsuranceAmount();

        substractServices.checkBreakdown(servicesAmountBeforeIva, iva, flagPromoApplyRad, flagPromoApplyEad);

        double totalBeforeIva = (amountBeforeIva + excessAmountBeforeIva + substractServices.getRadAmount() + substractServices.getEadAmount()) - (discountBeforeIva + excessDiscountBeforeIva);
        totalIva += totalBeforeIva * iva;
        double finalTotalAmount = totalBeforeIva + totalIva - totalParcelIva;

        return new Service(
                UtilsMoney.round(amountBeforeIva, 2),
                UtilsMoney.round(extraChargesBeforeIva, 2),
                UtilsMoney.round(excessAmountBeforeIva, 2),
                UtilsMoney.round(discountBeforeIva, 2),
                UtilsMoney.round(excessDiscountBeforeIva, 2),
                UtilsMoney.round(substractServices.getRadAmount(), 2),
                UtilsMoney.round(substractServices.getEadAmount(), 2),
                UtilsMoney.round(totalIva, 2),
                UtilsMoney.round(totalParcelIva, 2),
                UtilsMoney.round(finalTotalAmount, 2));
    }

    private SubstractServices getSubstractServices(SHIPMENT_TYPE shipmentType, TypeServiceCost typeServiceCost, boolean flagPromoApplyRad, boolean flagPromoApplyEad) {
        double serviceRadAmount = 0;
        double serviceEadAmount = 0;
        double totalSubstractServices = 0;
        if (shipmentType.includeRAD() || flagPromoApplyRad) {
            serviceRadAmount += typeServiceCost.getRADOCUCost();
            if (flagPromoApplyRad) {
                totalSubstractServices += typeServiceCost.getRADOCUCost();
            }
        }
        if (shipmentType.includeEAD() || flagPromoApplyEad) {
            serviceEadAmount += typeServiceCost.getEADCost();
            if (flagPromoApplyEad) {
                totalSubstractServices += typeServiceCost.getEADCost();
            }
        }
        return new SubstractServices(totalSubstractServices, serviceRadAmount, serviceEadAmount);
    }

    private JsonObject calculatePackageCostBreakdown(ParcelPackagePromo parcelPackagePromo, double iva, double parcelIva, CustomerBillingInformation customerBillingInformation, double totalSubstractServicesByPackage) {
        double amountBeforeIva = 0.0;
        double excessAmountBeforeIva = 0.0;
        double discountBeforeIva = 0.0;
        double excessDiscountBeforeIva = 0.0;
        double totalParcelIva = 0.0;

        ParcelPackage parcelPackage = parcelPackagePromo.getParcelPackage();
        int quantity = parcelPackage.getQuantity();
        double weight = parcelPackage.getWeight();
        double finalIva = iva;
        boolean flagRetention = false;
        if (parcelPackage.getShippingType().equals("parcel") && weight > 31.5 && Objects.nonNull(customerBillingInformation) && customerBillingInformation.getLegalPerson().equals(_MORAL)) {
            flagRetention = true;
            finalIva = iva - parcelIva;
        }

        double packAmount = (parcelPackage.getAmount());
        double packExcessAmount = parcelPackage.getExcessCost();
        double totalPackageCost = parcelPackage.getTotalAmount() ;
        double finalTotalSubstractServicesAmount = 0;
        double finalTotalSubstractServicesExcess = 0;
        if (totalSubstractServicesByPackage > totalPackageCost) {
            totalSubstractServicesByPackage = totalPackageCost;
            finalTotalSubstractServicesAmount = totalSubstractServicesByPackage;
        }
        if (packAmount >= totalSubstractServicesByPackage) {
            packAmount -= totalSubstractServicesByPackage;
            finalTotalSubstractServicesAmount = totalSubstractServicesByPackage;
        } else if(packExcessAmount >= totalSubstractServicesByPackage) {
            packExcessAmount -= totalSubstractServicesByPackage;
            finalTotalSubstractServicesAmount = totalSubstractServicesByPackage;
        } else {
            double sub = packAmount - totalSubstractServicesByPackage;
            if (sub < 0) {
                double remaining = totalSubstractServicesByPackage - sub;
                packAmount = 0;
                packExcessAmount -= remaining;
                finalTotalSubstractServicesExcess = remaining;
            }
        }

        packAmount = packAmount / (finalIva + 1);
        parcelPackagePromo.getParcelPackage().setAmount(UtilsMoney.round(packAmount, 2));
        parcelPackagePromo.getParcelPackage().setCost(UtilsMoney.round(packAmount, 2));
        amountBeforeIva += (packAmount * quantity);

        packExcessAmount = packExcessAmount / (finalIva + 1);
        parcelPackagePromo.getParcelPackage().setExcessCost(UtilsMoney.round(finalTotalSubstractServicesExcess + packExcessAmount, 2));
        excessAmountBeforeIva += packExcessAmount * quantity;

        double packDiscount = parcelPackage.getDiscount() / (finalIva + 1);
        parcelPackagePromo.getParcelPackage().setDiscount(UtilsMoney.round(packDiscount, 2));
        discountBeforeIva += packDiscount * quantity;

        double packExcessDiscount = parcelPackage.getExcessDiscount() / (finalIva + 1);
        parcelPackagePromo.getParcelPackage().setExcessDiscount(UtilsMoney.round(packExcessDiscount, 2));
        excessDiscountBeforeIva += packExcessDiscount * quantity;

        double packSubTotal = (packAmount + packExcessAmount) - (packDiscount + packExcessDiscount);
        double packParcelIva = flagRetention ? (packSubTotal * parcelIva) : 0;
        double packIva = packSubTotal * iva;
        totalParcelIva += (packParcelIva * quantity);
        parcelPackagePromo.getParcelPackage().setParcelIva(UtilsMoney.round(packParcelIva, 2));
        parcelPackagePromo.getParcelPackage().setIva(UtilsMoney.round(packIva, 2));
        double totalAmountPackage = packSubTotal + packIva - packParcelIva;
        parcelPackagePromo.getParcelPackage().setTotalAmount(UtilsMoney.round(totalAmountPackage, 2));

        double finalSubstractServices = (finalTotalSubstractServicesAmount + finalTotalSubstractServicesExcess) / (iva + 1);
        double finalTotalSubstractServices = finalSubstractServices * quantity;

        return new JsonObject()
                .put(_AMOUNT_BEFORE_IVA, amountBeforeIva)
                .put(_EXCESS_AMOUNT_BEFORE_IVA, excessAmountBeforeIva)
                .put(_DISCOUNT_BEFORE_IVA, discountBeforeIva)
                .put(_EXCESS_DISCOUNT_BEFORE_IVA, excessDiscountBeforeIva)
                .put(_SERVICES_AMOUNT_BEFORE_IVA, finalTotalSubstractServices)
                .put(_TOTAL_PARCEL_IVA, totalParcelIva);
    }

    private void applyDiscountExcess(Message<JsonObject> message, List<ParcelPackagePromo> parcelPackagePromoList, List<ParcelPacking> parcelPackings, Insurance insurance, String service, String origin, int customerId, int secondCustomerId, int terminalOriginId,
                                     int terminalDestinyId, SHIPMENT_TYPE shipmentType, Integer customerBillingInfoId) {
        try {
            List<ParcelPackagePromo> parcelPackagesPromoApplied = parcelPackagePromoList.stream()
                    .filter(p -> p.getParcelPackage().getShippingType().equals("parcel") && Objects.nonNull(p.getPromo()))
                    .collect(Collectors.toList());

            if (parcelPackagesPromoApplied.size() == parcelPackagePromoList.size()) {
                finishProcess(message, parcelPackagePromoList, parcelPackings, insurance, shipmentType, customerId, customerBillingInfoId);
            } else if (parcelPackagesPromoApplied.size() < parcelPackagePromoList.size()) {

                List<CompletableFuture<Boolean>> getExcessCostList = new ArrayList<>();
                List<ParcelPackagePromo> parcelPackageWithoutPromo = parcelPackagePromoList.stream()
                        .filter(p -> p.getParcelPackage().getShippingType().equals("parcel") && Objects.isNull(p.getPromo()))
                        .sorted(Comparator.comparingDouble(p -> p.getParcelPackage().getPrice()))
                        .collect(Collectors.toList());

                for (ParcelPackagePromo parcelPackagePromo : parcelPackageWithoutPromo) {
                    getExcessCostList.add(doExcessCostProcess(parcelPackagePromo, service, origin, customerId, secondCustomerId, terminalOriginId, terminalDestinyId));
                }

                CompletableFuture.allOf(getExcessCostList.toArray(new CompletableFuture[getExcessCostList.size()])).whenComplete((resultGEL, errGEL) -> {
                    try {
                        if (Objects.nonNull(errGEL)) {
                            throw errGEL;
                        }

                        finishProcess(message, parcelPackagePromoList, parcelPackings, insurance, shipmentType, customerId, customerBillingInfoId);

                    } catch (Throwable t) {
                        reportQueryError(message, t);
                    }
                });

            } else {
                finishProcess(message, parcelPackagePromoList, parcelPackings, insurance, shipmentType, customerId, customerBillingInfoId);
            }
        } catch (Throwable t) {
            reportQueryError(message, t);
        }

    }

    private CompletableFuture<Boolean> doExcessCostProcess(ParcelPackagePromo parcelPackagePromo, String service, String origin, int customerId, int secondCustomerId, int terminalOriginId, int terminalDestinyId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        getMaxPackagePriceByApplicablePromo(parcelPackagePromo, service, origin, customerId, secondCustomerId).whenComplete((maxPackagePrice, errMaxPackagePrice) -> {
            try {
                if (errMaxPackagePrice != null) {
                    throw errMaxPackagePrice;
                }
                if(Objects.isNull(maxPackagePrice)) {
                    future.complete(true);
                    return;
                }

                ParcelPackage parcelPackage = parcelPackagePromo.getParcelPackage();
                String maxPackagePriceNameApplicable = maxPackagePrice.getString(_MAX_PACKAGE_PRICE_NAME_APPLICABLE);
                Integer maxPackagePriceIdApplicable = maxPackagePrice.getInteger(_MAX_PACKAGE_PRICE_ID_APPLICABLE);
                DISCOUNT_TYPES promoDiscountType = DISCOUNT_TYPES.fromValue(maxPackagePrice.getString(_DISCOUNT_TYPE));
                Integer promoId = maxPackagePrice.getInteger(ID);
                Double promoDiscount = maxPackagePrice.getDouble(_DISCOUNT);
                String discountCode = maxPackagePrice.getString(_DISCOUNT_CODE);
                String name = maxPackagePrice.getString(_NAME);
                String description = maxPackagePrice.getString(_DESCRIPTION);
                boolean applyRad = maxPackagePrice.getBoolean(_APPLY_RAD);
                boolean applyEad = maxPackagePrice.getBoolean(_APPLY_EAD);
                boolean applySenderAddressee = maxPackagePrice.getBoolean(_APPLY_SENDER_ADDRESSEE);

                getExcessCost(maxPackagePriceNameApplicable, terminalOriginId, terminalDestinyId,
                        parcelPackage.getShippingType(),
                        parcelPackage.getWeight(), parcelPackage.getHeight(),
                        parcelPackage.getLength(), parcelPackage.getWidth())
                        .whenComplete((resExcessCost, errExcessCost) -> {
                            try {
                                if (errExcessCost != null) {
                                    throw errExcessCost;
                                }

                                Double excessCost = resExcessCost.getDouble(_EXCESS_COST);
                                if(excessCost == 0) {
                                    future.complete(true);
                                    return;
                                }

                                String excessBy = resExcessCost.getString(_EXCESS_BY);
                                getMaxCostByExcessType(excessBy, maxPackagePriceNameApplicable, terminalOriginId, terminalDestinyId).whenComplete((maxCost, errMaxCost) -> {
                                    try {
                                        if (errMaxCost != null) {
                                            throw errMaxCost;
                                        }

                                        switch (promoDiscountType) {
                                            case direct_percent:
                                                Double discountPercentDP = UtilsMoney.round(promoDiscount / 100, 2);
                                                double maxCostDiscountDP = UtilsMoney.round(maxCost * discountPercentDP, 2);
                                                double finalMaxCostDP = UtilsMoney.round(maxCost - maxCostDiscountDP, 2);
                                                double discountDP = UtilsMoney.round(excessCost * discountPercentDP, 2);
                                                double finalCostDP = UtilsMoney.round(excessCost - discountDP, 2);
                                                parcelPackagePromo.getParcelPackage().setDiscountByExcess(true);
                                                parcelPackagePromo.getParcelPackage().setCost(maxCost);
                                                parcelPackagePromo.getParcelPackage().setAmount(maxCost);
                                                parcelPackagePromo.getParcelPackage().setDiscount(maxCostDiscountDP);
                                                parcelPackagePromo.getParcelPackage().setExcessCost(excessCost);
                                                parcelPackagePromo.getParcelPackage().setExcessDiscount(discountDP);
                                                parcelPackagePromo.getParcelPackage().setTotalAmount(UtilsMoney.round(finalMaxCostDP + finalCostDP, 2));
                                                parcelPackagePromo.getParcelPackage().setPromoAppliedInfo(new PromoAppliedInfo(promoId, discountCode, name, description, promoDiscountType.name(), promoDiscount, applyRad, applyEad, applySenderAddressee));
                                                parcelPackagePromo.getParcelPackage().setPackagePriceId(maxPackagePriceIdApplicable);
                                                parcelPackagePromo.getParcelPackage().setPackagePriceName(maxPackagePriceNameApplicable);
                                                future.complete(true);
                                                break;
                                            case as_price:
                                            case direct_amount:
                                                double discountOperation = maxCost - promoDiscount;
                                                double discountAppliedAP = UtilsMoney.round(discountOperation > 0 ? discountOperation : maxCost, 2);
                                                double discountPercentAP = UtilsMoney.round((discountAppliedAP / maxCost * 100), 2);
                                                double finalDiscountAP = UtilsMoney.round(excessCost * ((discountPercentAP <= 50 ? discountPercentAP : 50 )/ 100), 2);
                                                double finalCostAP = UtilsMoney.round((promoDiscount + excessCost) - finalDiscountAP, 2);
                                                parcelPackagePromo.getParcelPackage().setDiscountByExcess(true);
                                                if (discountPercentAP == 100) {
                                                    parcelPackagePromo.getParcelPackage().setCost(promoDiscount);
                                                    parcelPackagePromo.getParcelPackage().setAmount(promoDiscount);
                                                    parcelPackagePromo.getParcelPackage().setDiscount(0);
                                                } else {
                                                    parcelPackagePromo.getParcelPackage().setCost(maxCost);
                                                    parcelPackagePromo.getParcelPackage().setAmount(maxCost);
                                                    parcelPackagePromo.getParcelPackage().setDiscount(discountAppliedAP);
                                                }
                                                parcelPackagePromo.getParcelPackage().setExcessCost(excessCost);
                                                parcelPackagePromo.getParcelPackage().setExcessDiscount(finalDiscountAP);
                                                parcelPackagePromo.getParcelPackage().setTotalAmount(UtilsMoney.round(finalCostAP, 2));
                                                parcelPackagePromo.getParcelPackage().setPromoAppliedInfo(new PromoAppliedInfo(promoId, discountCode, name, description, promoDiscountType.name(), promoDiscount, applyRad, applyEad, applySenderAddressee));
                                                parcelPackagePromo.getParcelPackage().setPackagePriceId(maxPackagePriceIdApplicable);
                                                parcelPackagePromo.getParcelPackage().setPackagePriceName(maxPackagePriceNameApplicable);
                                                future.complete(true);
                                                break;
                                            default:
                                                future.complete(true);
                                                break;
                                        }

                                    } catch (Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });

                            } catch (Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getExcessCost(String basePackagePriceName, int terminalOriginId, int terminalDestinyId, String shippingType, double weight, double height, double length, double width) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject()
                .put(_COST_BREAKDOWN, false)
                .put(_BASE_PACKAGE_PRICE_NAME, basePackagePriceName)
                .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                .put(_SHIPPING_TYPE, shippingType)
                .put(_WEIGHT, weight)
                .put(_HEIGHT, height)
                .put(_LENGTH, length)
                .put(_WIDTH, width);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_EXCESS_COST);
            this.getVertx().eventBus().send(GuiappDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    future.complete((JsonObject) reply.result().body());
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Double> getMaxCostByExcessType(String excessBy, String basePackagePriceName, int terminalOriginId, int terminalDestinyId) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject()
                .put(_EXCESS_BY, excessBy)
                .put(_BASE_PACKAGE_PRICE_NAME, basePackagePriceName)
                .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                .put(_TERMINAL_DESTINY_ID, terminalDestinyId);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_MAX_COST);
            this.getVertx().eventBus().send(GuiappDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    JsonObject result = (JsonObject) reply.result().body();
                    Double cost = result.getDouble(_COST);
                    future.complete(cost);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void finishProcess(Message<JsonObject> message, List<ParcelPackagePromo> parcelPackagePromoList, List<ParcelPacking> parcelPackings, Insurance insurance, SHIPMENT_TYPE shipmentType, Integer customerId, Integer customerBillingInfoId) {
        this.getIvaValue().whenComplete((iva, errorIva) -> {
            try {
                if (errorIva != null) {
                    throw errorIva;
                }
                this.getParcelIvaValue().whenComplete((parcelIva, errorParcelIva) -> {
                    try {
                        if (errorParcelIva != null) {
                            throw errorParcelIva;
                        }
                        this.getCustomerBillingInfoById(customerId, customerBillingInfoId).whenComplete((customerBillingInformation, errCBI) -> {
                            try {
                                if (errCBI != null) {
                                    throw errCBI;
                                }
                                this.getTypeServiceCost().whenComplete((typeServiceCost, errorTSC) -> {
                                    try {
                                        if (errorTSC != null) {
                                            throw errorTSC;
                                        }
                                        Service resultService = getTotals(parcelPackagePromoList, parcelPackings, insurance, iva, parcelIva, shipmentType, typeServiceCost, customerBillingInformation);
                                        JsonArray resultPackages = new JsonArray(parcelPackagePromoList.stream()
                                                .map(p -> JsonObject.mapFrom(p.getParcelPackage()))
                                                .collect(Collectors.toList()));
                                        JsonArray resultPackings = new JsonArray(parcelPackings.stream()
                                                .map(JsonObject::mapFrom)
                                                .collect(Collectors.toList()));
                                        JsonObject resultInsurance = JsonObject.mapFrom(insurance);
                                        JsonObject result = new JsonObject()
                                                .put(SERVICE, JsonObject.mapFrom(resultService))
                                                .put(_PARCEL_PACKAGES, resultPackages)
                                                .put(_PARCEL_PACKINGS, resultPackings)
                                                .put(_INSURANCE, resultInsurance);
                                        replyResult(message, result);
                                    } catch (Throwable t) {
                                        reportQueryError(message, t);
                                    }
                                });
                            } catch (Throwable t) {
                                reportQueryError(message, t);
                            }
                        });
                    } catch (Throwable t) {
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable t) {
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<JsonObject> getMaxPackagePriceByApplicablePromo(ParcelPackagePromo parcelPackagePromo, String service, String origin, Integer customerId, Integer secondCustomerId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            ParcelPackage parcelPackage = parcelPackagePromo.getParcelPackage();
            String today = UtilsDate.format_yyyy_MM_dd(getDateConvertedTimeZone(timezone, new Date()));
            JsonArray params = new JsonArray()
                    .add(service)
                    .add(origin)
                    .add(today)
                    .add(parcelPackage.getPackagePriceDistanceId())
                    .add(parcelPackage.getPackageTypeId())
                    .add("parcel")
                    .add(today)
                    .add(customerId).add(secondCustomerId)
                    .add(customerId).add(secondCustomerId)
                    .add(customerId);
            this.dbClient.queryWithParams(QUERY_GET_MAX_PACKAGE_PRICE_BY_APPLICABLE_PROMO, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        future.complete(null);
                    } else {
                        future.complete(result.get(0));
                    }

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<TypeServiceCost> getTypeServiceCost() {
        CompletableFuture<TypeServiceCost> future = new CompletableFuture<>();
        this.dbClient.query(QUERY_GET_SERVICE_AMOUNT, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Services amount not found");
                }
                double RADOCUCost = result.stream().filter(s -> s.getString(_TYPE_SERVICE).equals(TYPE_SERVICE.RAD_OCU.getValue())).map(s -> s.getDouble(_AMOUNT)).collect(Collectors.toList()).get(0);
                double EADCost = result.stream().filter(s -> s.getString(_TYPE_SERVICE).equals(TYPE_SERVICE.EAD.getValue())).map(s -> s.getDouble(_AMOUNT)).collect(Collectors.toList()).get(0);
                future.complete(new TypeServiceCost(RADOCUCost, EADCost));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getIvaValue() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        this.dbClient.query(QUERY_GET_IVA_VALUE, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Iva value not found");
                }
                future.complete(Double.parseDouble(result.get(0).getString(VALUE)) / 100);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getParcelIvaValue() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        this.dbClient.query(QUERY_GET_PARCEL_IVA_VALUE, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Parcel iva value not found");
                }
                future.complete(Double.parseDouble(result.get(0).getString(VALUE)) / 100);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<CustomerBillingInformation> getCustomerBillingInfoById(Integer customerId, Integer customerBillingInfoId) {
        CompletableFuture<CustomerBillingInformation> future = new CompletableFuture<>();
        if (Objects.isNull(customerBillingInfoId) || customerBillingInfoId == 0) {
            future.complete(null);
        } else {
            this.dbClient.queryWithParams(QUERY_GET_CUSTOMER_BILLING_INFO_BY_ID_AND_CUSTOMER_ID, new JsonArray().add(customerBillingInfoId).add(customerId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Customer billing info not found");
                    }
                    future.complete(result.get(0).mapTo(CustomerBillingInformation.class));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private static final String QUERY_GET_PROMO_BY_PACKAGE = "SELECT p.id,\n" +
            "    cp.id AS customers_promos_id,\n" +
            "    p.discount_code,\n" +
            "    p.discount_type,\n" +
            "    p.discount,\n" +
            "    p.name,\n" +
            "    p.description,\n" +
            "    p.num_products,\n" +
            "    p.rule_for_packages,\n" +
            "    p.type_packages,\n" +
            "    p.apply_rad,\n" +
            "    p.apply_ead,\n" +
            "    p.apply_sender_addressee\n" +
            "FROM promos p\n" +
            "INNER JOIN customers_promos cp ON cp.promo_id = p.id \n" +
            "WHERE p.status = 1\n" +
            "AND cp.status = 1 \n" +
            "AND (cp.usage_limit = 0 OR cp.used < cp.usage_limit)\n" +
            "AND p.service = ?\n" +
            "AND p.purchase_origin = ?\n" +
            "AND ? BETWEEN p.since AND p.until\n" +
            "AND (p.apply_to_package_price IS NULL OR FIND_IN_SET(?, p.apply_to_package_price))\n" +
            "AND (p.apply_to_package_price_distance IS NULL OR FIND_IN_SET(?, p.apply_to_package_price_distance))\n" +
            "AND (p.apply_to_package_type IS NULL OR FIND_IN_SET(?, p.apply_to_package_type))\n" +
            "AND (p.type_packages IS NULL OR p.type_packages IN ('all', ?)) \n" +
            "AND (p.available_days = 'all' OR p.available_days REGEXP LOWER(LEFT(DAYNAME(?), 3)))\n" +
            "AND cp.customer_id = ? \n" +
            "ORDER BY p.created_at, cp.created_at DESC LIMIT 1;";

    private static final String QUERY_GET_MAX_PACKAGE_PRICE_BY_APPLICABLE_PROMO = "WITH RECURSIVE numbers AS (\n" +
            "    SELECT 0 AS digit\n" +
            "    UNION ALL\n" +
            "    SELECT digit + 1\n" +
            "    FROM numbers\n" +
            "    WHERE digit < 999\n" +
            ")\n" +
            "SELECT \n" +
            "    SUBSTRING_INDEX(SUBSTRING_INDEX(REPLACE(p.apply_to_package_price, 'RS', ''), ',', n.digit + 1), ',', -1) AS max_package_price_name_applicable,\n" +
            "    (SELECT id FROM package_price WHERE name_price = max_package_price_name_applicable) AS max_package_price_id_applicable, \n" +
            "    p.id,\n" +
            "    p.discount_type,\n" +
            "    p.discount,\n" +
            "    p.discount_code,\n" +
            "    p.name,\n" +
            "    p.description,\n" +
            "    p.apply_rad,\n" +
            "    p.apply_ead,\n" +
            "    p.apply_sender_addressee,\n" +
            "    cp.customer_id\n" +
            "FROM \n" +
            "    promos p\n" +
            "INNER JOIN \n" +
            "    customers_promos cp ON cp.promo_id = p.id \n" +
            "JOIN \n" +
            "    numbers n \n" +
            "    ON LENGTH(REPLACE(REPLACE(p.apply_to_package_price, 'RS', ''), ',' , '')) <= LENGTH(REPLACE(p.apply_to_package_price, 'RS', '')) - n.digit\n" +
            "WHERE \n" +
            "    p.status = 1\n" +
            "    AND cp.status = 1 \n" +
            "    AND (cp.usage_limit = 0 OR cp.used < cp.usage_limit)\n" +
            "    AND p.service = ?\n" +
            "    AND p.purchase_origin = ?\n" +
            "    AND ? BETWEEN p.since AND p.until\n" +
            "    AND (p.apply_to_package_price_distance IS NULL OR FIND_IN_SET(?, p.apply_to_package_price_distance))\n" +
            "    AND (p.apply_to_package_type IS NULL OR FIND_IN_SET(?, p.apply_to_package_type))\n" +
            "    AND (p.type_packages IS NULL OR p.type_packages IN ('all', ?)) \n" +
            "    AND (p.available_days = 'all' OR p.available_days REGEXP LOWER(LEFT(DAYNAME(?), 3)))\n" +
            "    AND cp.customer_id IN (?, ?)\n" +
            "    HAVING\n" +
            "       CASE \n" +
            "           WHEN p.apply_sender_addressee = TRUE THEN cp.customer_id IN (?, ?)\n" +
            "           ELSE cp.customer_id IN (?)\n" +
            "       END\n" +
            "    ORDER BY max_package_price_name_applicable DESC LIMIT 1;";

    private static final String QUERY_GET_SERVICE_AMOUNT = "SELECT type_service, COALESCE(amount, 0) AS amount FROM parcel_service_type WHERE type_service IN ('RAD/OCU', 'EAD');";

    private static final String QUERY_GET_IVA_VALUE = "SELECT value FROM general_setting WHERE FIELD = 'iva';";

    private static final String QUERY_GET_PARCEL_IVA_VALUE = "SELECT value FROM general_setting WHERE FIELD = 'parcel_iva';";

    private static final String QUERY_GET_CUSTOMER_BILLING_INFO_BY_ID_AND_CUSTOMER_ID = "SELECT cbi.* FROM customer_billing_information cbi " +
            "INNER JOIN customer_customer_billing_info ccbi ON ccbi.customer_billing_information_id = cbi.id " +
            "WHERE cbi.id = ? " +
            "AND ccbi.customer_id = ? " +
            "AND cbi.status = 1;";

}