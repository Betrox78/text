package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.customers.models.CustomerBillingInformation;
import database.insurances.InsurancesDBV;
import database.insurances.handlers.InsurancesDBV.models.Insurance;
import database.parcel.GuiappDBV;
import database.parcel.PackingsDBV;
import database.parcel.enums.SHIPMENT_TYPE;
import database.parcel.enums.TYPE_SERVICE;
import database.promos.handlers.PromosDBV.models.ParcelPacking;
import database.promos.handlers.PromosDBV.models.TypeServiceCost;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsMoney;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class CostExchange extends DBHandler<GuiappDBV> {

    public CostExchange(GuiappDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            boolean costBreakdown = body.getBoolean(_COST_BREAKDOWN, false);
            int terminalOriginId = body.getInteger(_TERMINAL_ORIGIN_ID);
            int terminalDestinyId = body.getInteger(_TERMINAL_DESTINY_ID);
            Integer customerId = body.getInteger(_CUSTOMER_ID);
            Integer customerBillingInformationId = body.getInteger(_CUSTOMER_BILLING_INFORMATION_ID);
            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(body.getString(_SHIPMENT_TYPE, "OCU"));
            JsonArray parcelPackages = body.getJsonArray(_PARCEL_PACKAGES);
            List<ParcelPacking> parcelPackings = mapParcelPackings(body.getJsonArray(_PARCEL_PACKINGS, new JsonArray()));
            Double insuranceValue = body.getDouble(_INSURANCE_VALUE, 0.0);
            Insurance insurance = new Insurance();
            insurance.setInsuranceValue(insuranceValue);

            Future<TypeServiceCost> f1 = Future.future();
            Future<CustomerBillingInformation> f2 = Future.future();
            Future<Double> f3 = Future.future();
            Future<Double> f4 = Future.future();
            getTypeServiceCost().setHandler(f1.completer());
            getCustomerBillingInfoById(customerId, customerBillingInformationId).setHandler(f2.completer());
            getIvaValue().setHandler(f3.completer());
            getParcelIvaValue().setHandler(f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    TypeServiceCost typeServiceCost = reply.result().resultAt(0);
                    CustomerBillingInformation customerBillingInformation = reply.result().resultAt(1);
                    Double ivaPercent = reply.result().resultAt(2);
                    Double parcelIvaPercent = reply.result().resultAt(3);

                    List<CompletableFuture> tasksCodes = new ArrayList<>();
                    for (Object pp : parcelPackages) {
                        JsonObject pack = (JsonObject) pp;
                        JsonObject details = pack.getJsonObject(_DETAILS);
                        int parcelPrepaidId = pack.getInteger(_PARCEL_PREPAID_ID);
                        JsonArray codes = details.getJsonArray(_CODES);
                        for (Object code : codes) {
                            tasksCodes.add(calculateCode(costBreakdown, customerBillingInformation, terminalOriginId, terminalDestinyId, parcelPrepaidId, (JsonObject) code, typeServiceCost, ivaPercent, parcelIvaPercent));
                        }
                    }

                    for (ParcelPacking parcelPacking : parcelPackings) {
                        tasksCodes.add(getCostPacking(parcelPacking));
                    }
                    if (insuranceValue > 0) {
                        tasksCodes.add(getCostInsurance("parcel", insurance));
                    }

                    CompletableFuture.allOf(tasksCodes.toArray(new CompletableFuture[tasksCodes.size()])).whenComplete((replyTasks, errTasks) -> {
                        try {
                            if (errTasks != null) {
                                throw errTasks;
                            }

                            JsonObject totals = getTotals(shipmentType, typeServiceCost, ivaPercent, parcelPackages, parcelPackings, insurance);

                            message.reply(body.mergeIn(totals)
                                    .put(_PARCEL_PACKINGS, new JsonArray(parcelPackings.stream()
                                            .map(JsonObject::mapFrom)
                                            .collect(Collectors.toList())))
                                    .put(_INSURANCE, JsonObject.mapFrom(insurance)));
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

    private List<ParcelPacking> mapParcelPackings(JsonArray packings) {
        return packings.stream().map(p -> {
            JsonObject parcelPacking = (JsonObject) p;
            return parcelPacking.mapTo(ParcelPacking.class);
        }).collect(Collectors.toList());
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

    private CompletableFuture<Boolean> calculateCode(boolean costBreakdown, CustomerBillingInformation customerBillingInformation, int terminalOriginId, int terminalDestinyId, int parcelPrepaidId, JsonObject codeInfo, TypeServiceCost typeServiceCost, double ivaPercent, double parcelIvaPercent) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            int quantity = codeInfo.getInteger(_QUANTITY, 1);
            double height = codeInfo.getDouble(_HEIGHT, 0.0);
            double length = codeInfo.getDouble(_LENGTH, 0.0);
            double weight = codeInfo.getDouble(_WEIGHT, 0.0);
            double width = codeInfo.getDouble(_WIDTH, 0.0);
            float volume = Float.parseFloat(String.format("%.3f", (width * height * length) / 1000000));
            String shippingType = codeInfo.getString(_SHIPPING_TYPE);

            getParcelPrepaidInfo(parcelPrepaidId).setHandler(replyPPI -> {
                try {
                    if (replyPPI.failed()) {
                        throw replyPPI.cause();
                    }
                    JsonObject parcelPrepaidInfo = replyPPI.result();

                    SHIPMENT_TYPE parcelPrepaidShipmentType = SHIPMENT_TYPE.fromValue(parcelPrepaidInfo.getString(_SHIPMENT_TYPE));
                    processServicesCost(typeServiceCost, parcelPrepaidShipmentType);

                    double parcelPrepaidTotalAmount = parcelPrepaidInfo.getDouble(_PARCEL_PREPAID_TOTAL_AMOUNT);
                    int totalCountGuipp = parcelPrepaidInfo.getInteger(_TOTAL_COUNT_GUIPP);
                    Integer basePackagePriceId = parcelPrepaidInfo.getInteger(_BASE_PACKAGE_PRICE_ID);
                    String basePackagePriceName = parcelPrepaidInfo.getString(_BASE_PACKAGE_PRICE_NAME);

                    getExcessCost(costBreakdown, basePackagePriceName, terminalOriginId, terminalDestinyId, shippingType, weight, height, length, width)
                        .whenComplete((resExcessCost, errExcessCost) -> {
                        try {
                            if (errExcessCost != null) {
                                throw errExcessCost;
                            }

                            String excessBy = resExcessCost.getString(_EXCESS_BY);
                            getMaxCostByExcessType(excessBy, basePackagePriceName, terminalOriginId, terminalDestinyId).whenComplete((maxCost, errMaxCost) -> {
                                try {
                                    if (errMaxCost != null) {
                                        throw errMaxCost;
                                    }

                                    double realPercentDiscountApplied = getPercentDiscountApplied(maxCost, parcelPrepaidTotalAmount, totalCountGuipp);
                                    double excessDiscountPercent = realPercentDiscountApplied > 50 ? 50.0 : realPercentDiscountApplied;
                                    Double excessCost = resExcessCost.getDouble(_EXCESS_COST);

                                    double finalIvaPercent = ivaPercent;
                                    boolean flagRetention = false;
                                    if (shippingType.equals("parcel") && weight > 31.5 && Objects.nonNull(customerBillingInformation) && customerBillingInformation.getLegalPerson().equals(_MORAL)) {
                                        flagRetention = true;
                                        finalIvaPercent = ivaPercent - parcelIvaPercent;
                                    }

                                    double excessAmount = UtilsMoney.round(excessCost / (finalIvaPercent + 1), 2);
                                    double excessDiscount = UtilsMoney.round((excessCost * (excessDiscountPercent / 100) / (finalIvaPercent + 1)), 2);
                                    double subTotal = excessAmount - excessDiscount;
                                    double iva = UtilsMoney.round(subTotal * ivaPercent, 2);
                                    double parcelIva = UtilsMoney.round(flagRetention ? (subTotal * parcelIvaPercent) : 0, 2);
                                    double totalAmount = subTotal + iva - parcelIva;

                                    codeInfo.mergeIn(resExcessCost)
                                            .put(_VOLUME, volume * quantity)
                                            .put(_BASE_PACKAGE_PRICE_ID, basePackagePriceId)
                                            .put(_BASE_PACKAGE_PRICE_NAME, basePackagePriceName)
                                            .put(_REAL_PERCENT_DISCOUNT_APPLIED, realPercentDiscountApplied)
                                            .put(_PERCENT_DISCOUNT_APPLIED, excessDiscountPercent)
                                            .put(_MAX_COST, maxCost)
                                            .put(_EXCESS_AMOUNT, excessAmount * quantity)
                                            .put(_IVA, iva * quantity)
                                            .put(_PARCEL_IVA, parcelIva * quantity)
                                            .put(_AMOUNT, 0.0)
                                            .put(_DISCOUNT, 0.0)
                                            .put(_EXCESS_DISCOUNT, excessDiscount * quantity)
                                            .put(_TOTAL_AMOUNT, UtilsMoney.round(totalAmount * quantity, 2));

                                    future.complete(true);
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
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void processServicesCost(TypeServiceCost typeServiceCost, SHIPMENT_TYPE parcelPrepaidShipmentType) {
        if (!parcelPrepaidShipmentType.includeRAD() && typeServiceCost.isIncludesRad()) {
            typeServiceCost.setIncludesRad(false);
        }
        if (!parcelPrepaidShipmentType.includeEAD() && typeServiceCost.isIncludesEad()) {
            typeServiceCost.setIncludesEad(false);
        }
    }

    private Future<JsonObject> getParcelPrepaidInfo(int parcelPrepaidId) {
        Future<JsonObject> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_PARCELS_PREPAID_INFO, new JsonArray().add(parcelPrepaidId), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> results = reply.result().getRows();
                if(results.isEmpty()) {
                    throw new Exception("Parcel prepaid info not found");
                }
                future.complete(results.get(0));
            } catch (Throwable t){
                future.fail(t);
            }
        });
        return future;
    }

    private Future<TypeServiceCost> getTypeServiceCost() {
        Future<TypeServiceCost> future = Future.future();
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
                future.complete(new TypeServiceCost(RADOCUCost, EADCost, true, true));
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<CustomerBillingInformation> getCustomerBillingInfoById(Integer customerId, Integer customerBillingInfoId) {
        Future<CustomerBillingInformation> future = Future.future();
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
                    future.fail(t);
                }
            });
        }
        return future;
    }

    private Future<Double> getIvaValue() {
        Future<Double> future = Future.future();
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
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Double> getParcelIvaValue() {
        Future<Double> future = Future.future();
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
                future.fail(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getExcessCost(boolean costBreakdown, String basePackagePriceName, int terminalOriginId, int terminalDestinyId, String shippingType, double weight, double height, double length, double width) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject()
                    .put(_COST_BREAKDOWN, costBreakdown)
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

    private double getPercentDiscountApplied(double maxCost, double parcelPrepaidTotalAmount, double totalCountGuipp) {
        double ppTotalAmountUnit = parcelPrepaidTotalAmount / totalCountGuipp;
        double percentDiscount = UtilsMoney.round(((maxCost - ppTotalAmountUnit) / maxCost) * 100, 2);
        return percentDiscount > 0 ? percentDiscount : 0.0;
    }

    private JsonObject getTotals(SHIPMENT_TYPE shipmentType, TypeServiceCost typeServiceCost, double ivaPercent, JsonArray parcelPackages, List<ParcelPacking> parcelPackings, Insurance insurance) {
        double volume = 0.0;
        double excessAmount = 0.0;
        double excessDiscount = 0.0;
        double amount = 0.0;
        double extraCharges = 0.0;
        double iva = 0.0;
        double parcelIva = 0.0;
        double discount = 0.0;
        double totalAmount = 0.0;
        double distanceKm = 0.0;

        for (Object pp : parcelPackages) {
            JsonObject pack = (JsonObject) pp;
            JsonObject details = pack.getJsonObject(_DETAILS);
            JsonArray codes = details.getJsonArray(_CODES);
            for (Object c : codes) {
                JsonObject code = (JsonObject) c;
                volume += code.getDouble(_VOLUME);
                excessAmount += code.getDouble(_EXCESS_AMOUNT);
                excessDiscount += code.getDouble(_EXCESS_DISCOUNT);
                iva += code.getDouble(_IVA);
                parcelIva += code.getDouble(_PARCEL_IVA);
                amount += code.getDouble(_AMOUNT);
                discount += code.getDouble(_DISCOUNT);
                totalAmount += code.getDouble(_TOTAL_AMOUNT);
                distanceKm = (double) code.remove(_DISTANCE_KM);
            }
        }

        for (ParcelPacking parcelPacking : parcelPackings) {
            extraCharges += parcelPacking.getAmount();
            amount += parcelPacking.getAmount();
            iva += parcelPacking.getIva();
            totalAmount += (parcelPacking.getAmount() * (ivaPercent + 1));
        }

        amount += insurance.getInsuranceAmount();
        iva += insurance.getIva();
        totalAmount += (insurance.getInsuranceAmount() * (ivaPercent + 1));

        double servicesRadAmount = 0.0;
        if (shipmentType.includeRAD() && !typeServiceCost.isIncludesRad()) {
            iva += (typeServiceCost.getRADOCUCost()) * ivaPercent;
            servicesRadAmount = UtilsMoney.round(typeServiceCost.getRADOCUCost() / (ivaPercent + 1), 2);
        }

        double servicesEadAmount = 0.0;
        if (shipmentType.includeEAD() && !typeServiceCost.isIncludesEad()) {
            iva += (typeServiceCost.getEADCost()) * ivaPercent;
            servicesEadAmount = UtilsMoney.round(typeServiceCost.getEADCost() / (ivaPercent + 1), 2);
        }

        totalAmount += UtilsMoney.round((servicesRadAmount + servicesEadAmount) * (ivaPercent + 1), 2);

        return new JsonObject()
                .put(_DISTANCE_KM, distanceKm)
                .put(_VOLUME, Float.parseFloat(String.format("%.3f", volume)))
                .put(_SERVICES_RAD_AMOUNT, UtilsMoney.round(servicesRadAmount, 2))
                .put(_SERVICES_EAD_AMOUNT, UtilsMoney.round(servicesEadAmount, 2))
                .put(_EXCESS_AMOUNT, UtilsMoney.round(excessAmount, 2))
                .put(_EXCESS_DISCOUNT, UtilsMoney.round(excessDiscount, 2))
                .put(_IVA, UtilsMoney.round(iva, 2))
                .put(_PARCEL_IVA, UtilsMoney.round(parcelIva, 2))
                .put(_AMOUNT, UtilsMoney.round(amount, 2))
                .put(_EXTRA_CHARGES, UtilsMoney.round(extraCharges, 2))
                .put(_DISCOUNT, UtilsMoney.round(discount, 2))
                .put(_TOTAL_AMOUNT, UtilsMoney.round(totalAmount, 2));
    }


    private static final String QUERY_GET_PARCELS_PREPAID_INFO = "SELECT \n" +
            "   pp.total_amount AS parcel_prepaid_total_amount, \n" +
            "    pp.total_count_guipp,\n" +
            "    pp.shipment_type,\n" +
            "    (SELECT pprice.name_price FROM parcels_prepaid_detail ppd\n" +
            "       INNER JOIN package_price pprice ON pprice.id = ppd.price_id\n" +
            "        WHERE ppd.parcel_prepaid_id = pp.id LIMIT 1) AS base_package_price_name,\n" +
            "    (SELECT pprice.id FROM parcels_prepaid_detail ppd\n" +
            "       INNER JOIN package_price pprice ON pprice.id = ppd.price_id\n" +
            "        WHERE ppd.parcel_prepaid_id = pp.id LIMIT 1) AS base_package_price_id\n" +
            "FROM parcels_prepaid pp \n" +
            "WHERE pp.id = ?;";

    private static final String QUERY_GET_SERVICE_AMOUNT = "SELECT type_service, COALESCE(amount, 0) AS amount FROM parcel_service_type WHERE type_service IN ('RAD/OCU', 'EAD');";

    private static final String QUERY_GET_CUSTOMER_BILLING_INFO_BY_ID_AND_CUSTOMER_ID = "SELECT cbi.* FROM customer_billing_information cbi " +
            "INNER JOIN customer_customer_billing_info ccbi ON ccbi.customer_billing_information_id = cbi.id " +
            "WHERE cbi.id = ? " +
            "AND ccbi.customer_id = ? " +
            "AND cbi.status = 1;";

    private static final String QUERY_GET_IVA_VALUE = "SELECT value FROM general_setting WHERE FIELD = 'iva';";

    private static final String QUERY_GET_PARCEL_IVA_VALUE = "SELECT value FROM general_setting WHERE FIELD = 'parcel_iva';";

}
