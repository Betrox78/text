package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import database.money.PaybackDBV;
import database.money.PaymentDBV;
import database.parcel.GuiappDBV;
import database.parcel.ParcelDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.parcel.enums.SHIPMENT_TYPE;
import database.parcel.handlers.ParcelsPackagesDBV.models.ParcelPackage;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static database.parcel.GuiappDBV.GET_COST_EXCHANGE;
import static database.parcel.ParcelDBV.ACTION_GET_PROMISE_DELIVERY_DATE;
import static database.parcel.ParcelDBV.PAYS_SENDER;
import static service.commons.Constants.*;
import static utils.UtilsDate.format_yyyy_MM_dd;
import static utils.UtilsDate.sdfDataBase;

public class ExchangeV2 extends DBHandler<ParcelDBV> {

    public ExchangeV2(ParcelDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject parcel = message.body().copy();
            Integer customerId = parcel.getInteger(_CUSTOMER_ID);
            JsonArray parcelPackages = parcel.getJsonArray(_PARCEL_PACKAGES);
            validateCodes(parcelPackages, customerId).whenComplete((resVC, errVC) -> {
               try {
                   if (errVC != null) {
                       throw errVC;
                   }
                   this.startTransaction(message, (SQLConnection conn) -> {
                       this.exchange(conn, parcel).whenComplete((resultRegister, errorRegister) -> {
                           try {
                               if (errorRegister != null){
                                   throw errorRegister;
                               }
                               this.commit(conn, message, resultRegister);
                           } catch (Throwable t){
                               this.rollback(conn, t, message);
                           }
                       });
                   });
               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<Boolean> validateCodes(JsonArray parcelPackages, Integer customerId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String parcelPrepaidDetailIdParams = parcelPackages.stream()
                    .map(JsonObject::mapFrom)
                    .map(pp -> pp.getJsonObject(_DETAILS))
                    .map(pp -> pp.getJsonArray(_PARCEL_PREPAID_DETAIL_ID))
                    .filter(Objects::nonNull)
                    .flatMap(arr -> arr.stream().map(String::valueOf))
                    .collect(Collectors.joining(","));
            String QUERY = String.format(QUERY_VALIDATE_CODES, parcelPrepaidDetailIdParams);

            this.dbClient.query(QUERY, reply -> {
               try {
                   if (reply.failed()) {
                       throw reply.cause();
                   }
                   List<JsonObject> results = reply.result().getRows();
                   if (results.isEmpty()) {
                       throw new Exception("Ivalid codes to exchange");
                   }

                   for (JsonObject result : results) {
                       String code = result.getString("guiapp_code");
                       Integer codeCustomerId = result.getInteger(_CUSTOMER_ID);
                       Integer codeParcelStatus = result.getInteger(_PARCEL_STATUS);
                       boolean wasExpired = result.getInteger("was_expired").equals(1);
                       if (!codeCustomerId.equals(customerId)) {
                           throw new Exception("Code does not belong to the client: " + code);
                       }
                       if (!codeParcelStatus.equals(0)) {
                           throw new Exception("Code was exchanged or canceled: " + code);
                       }
                       if (wasExpired) {
                           throw new Exception("Code was expired: " + code);
                       }
                   }
                   future.complete(true);
               } catch (Throwable t) {
                   future.completeExceptionally(t);
               }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private int getTotalPackages(JsonArray parcelPackages) {
        return  parcelPackages.stream()
                .map(JsonObject::mapFrom)
                .map(pp -> pp.getJsonObject(_DETAILS))
                .map(pp -> pp.getJsonArray(_CODES))
                .filter(Objects::nonNull)
                .flatMap(arr -> arr.stream().map(JsonObject::mapFrom))
                .map(code -> code.getInteger(_QUANTITY, 0))
                .reduce(0, Integer::sum);
    }

    private CompletableFuture<JsonObject> exchange(SQLConnection conn, JsonObject parcel) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            boolean internalCustomer = parcel.containsKey(INTERNAL_CUSTOMER) ? (Boolean) parcel.remove(INTERNAL_CUSTOMER) : false;
            Boolean isInternalParcel = parcel.containsKey("is_internal_parcel") ? parcel.getBoolean("is_internal_parcel") : false;
            Boolean paysSender;
            Integer terminalOriginId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
            Integer senderId = parcel.getInteger(_SENDER_ID);
            Integer addresseeId = parcel.getInteger(_ADDRESSEE_ID);
            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(parcel.getString(_SHIPMENT_TYPE));
            Integer typeServiceId = parcel.containsKey(_ID_TYPE_SERVICE) ? (Integer) parcel.remove(_ID_TYPE_SERVICE) : null;
            Integer zipCodeService = parcel.containsKey(_ZIP_CODE_SERVICE) ? (Integer) parcel.remove(_ZIP_CODE_SERVICE) : 0;
            Integer customerId = parcel.getInteger(_CUSTOMER_ID);
            Integer customerBillingInfoId = parcel.getInteger(_CUSTOMER_BILLING_INFORMATION_ID);
            String purchaseOrigin = parcel.getString(_PURCHASE_ORIGIN, "sucursal");
            try {
                paysSender = parcel.getBoolean(PAYS_SENDER);
            } catch (Exception e){
                paysSender = parcel.getInteger(PAYS_SENDER).equals(1);
            }
            parcel.put(PAYS_SENDER, isInternalParcel || paysSender);
            Double insuranceValue = parcel.getDouble("insurance_value");
            Integer cashOutId = (Integer) parcel.remove(CASHOUT_ID);
            Integer cashRegisterId = parcel.getInteger(CASH_REGISTER_ID);
            Integer integrationPartnerSessionId = parcel.getInteger(INTEGRATION_PARTNER_SESSION_ID);
            JsonObject customerCreditData = (JsonObject) parcel.remove("customer_credit_data");
            final Boolean isComplement = parcel.containsKey("is_complement") ? ((Boolean) parcel.remove("is_complement")) : false;
            final boolean isCredit = parcel.containsKey("is_credit") ? ((Boolean) parcel.remove("is_credit")) : false;
            JsonArray parcelPackages = (JsonArray) parcel.remove("parcel_packages");
            JsonArray parcelPackings = (JsonArray) parcel.remove("parcel_packings");
            JsonArray payments = (JsonArray) parcel.remove("payments");
            JsonObject cashChange = (JsonObject) parcel.remove("cash_change");
            final double ivaPercent = (Double) parcel.remove("iva_percent");
            final int currencyId = (Integer) parcel.remove("currency_id");
            final Integer createdBy = parcel.getInteger("created_by");
            parcel.put("total_packages", getTotalPackages(parcelPackages));
            boolean isPendingCollection = (boolean) parcel.remove(_IS_PENDING_COLLECTION);
            if(isPendingCollection) {
                parcel.put(_PARCEL_STATUS, PARCEL_STATUS.PENDING_COLLECTION.ordinal());
            }
            Boolean finalPaysSender = paysSender;

            List<CompletableFuture<Boolean>> cityValidations = new ArrayList<>();
            cityValidations.add(this.comapreCityId("addressee", addresseeId, terminalDestinyId, isComplement));
            cityValidations.add(this.comapreCityId("sender", senderId, terminalOriginId, isComplement));
            CompletableFuture.allOf(cityValidations.toArray(new CompletableFuture[cityValidations.size()])).whenComplete((rCityValidations, errCityValidations) -> {
                try {
                    if (errCityValidations != null) {
                        throw errCityValidations;
                    }
                    createParcel(conn, parcel, cashRegisterId, internalCustomer, integrationPartnerSessionId, purchaseOrigin).whenComplete((parcelId, errorCreateParcel) -> {
                        try {
                            if (errorCreateParcel != null) {
                                throw errorCreateParcel;
                            }
                            parcel.put(ID, parcelId);

                            JsonObject bodyCostExchange = new JsonObject()
                                    .put(_COST_BREAKDOWN, true)
                                    .put(_SHIPMENT_TYPE, shipmentType.getValue())
                                    .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                                    .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                                    .put(_CUSTOMER_ID, customerId)
                                    .put(_CUSTOMER_BILLING_INFORMATION_ID, customerBillingInfoId)
                                    .put(_PARCEL_PACKAGES, parcelPackages);
                            this.getVertx().eventBus().send(GuiappDBV.class.getSimpleName(), bodyCostExchange, new DeliveryOptions().addHeader(ACTION, GET_COST_EXCHANGE), (AsyncResult<Message<JsonObject>> replyCostExchange) -> {
                                try {
                                    if(replyCostExchange.failed()) {
                                        throw replyCostExchange.cause();
                                    }
                                    JsonObject costExchange = replyCostExchange.result().body();
                                    parcel.put(_AMOUNT, costExchange.getDouble(_AMOUNT));
                                    parcel.put(_DISCOUNT, costExchange.getDouble(_DISCOUNT));
                                    parcel.put(_IVA, costExchange.getDouble(_IVA));
                                    parcel.put(_TOTAL_AMOUNT, costExchange.getDouble(_TOTAL_AMOUNT));

                                    JsonArray finalParcelPackages = costExchange.getJsonArray(_PARCEL_PACKAGES);
                                    Double distanceKM = costExchange.getDouble(_DISTANCE_KM);

                                    PaybackDBV objPayback = new PaybackDBV();
                                    objPayback.calculatePointsParcel(conn, distanceKM, finalParcelPackages.size(), false).whenComplete((resultCalculate, error) -> {
                                        try {
                                            if (error != null) {
                                                throw error;
                                            }

                                            Double paybackMoney = resultCalculate.getDouble("money");
                                            Double paybackPoints = resultCalculate.getDouble("points");
                                            parcel.put("payback", paybackMoney);
                                            JsonArray finalCodes = new JsonArray();
                                            List<CompletableFuture<Boolean>> packagesTasks = new ArrayList<>();
                                            for (int i = 0; i < finalParcelPackages.size(); i++) {
                                                JsonObject parcelPackageGrouped = finalParcelPackages.getJsonObject(i);
                                                Integer parcelPrepaidId = parcelPackageGrouped.getInteger(_PARCEL_PREPAID_ID);
                                                Double realPercentDiscountApplied = parcelPackageGrouped.getDouble(_REAL_PERCENT_DISCOUNT_APPLIED);
//                                                Double percentDiscountApplied = parcelPackageGrouped.getDouble(_PERCENT_DISCOUNT_APPLIED);
                                                JsonObject details = parcelPackageGrouped.getJsonObject(_DETAILS);
                                                JsonArray parcelPrepaidDetailIds = details.getJsonArray(_PARCEL_PREPAID_DETAIL_ID);

                                                packagesTasks.add(updateParcelPrepaid(conn, parcelPrepaidId, realPercentDiscountApplied, createdBy));
                                                packagesTasks.add(updateParcelPackageDetails(conn, parcelPrepaidDetailIds, parcelId, createdBy));

                                                JsonArray codes = details.getJsonArray(_CODES);
                                                for (int j = 0; j < codes.size(); j++) {
                                                    JsonObject code = codes.getJsonObject(j);
                                                    finalCodes.add(code);
                                                    Integer quantity = code.getInteger(_QUANTITY);

                                                    if (!quantity.equals(parcelPrepaidDetailIds.size())) {
                                                        throw new Exception("Packages quantity not equals to codes exchanged quantity");
                                                    }

                                                    for (int k = 0; k < quantity; k++) {
                                                        Integer parcelPrepaidDetailId = parcelPrepaidDetailIds.getInteger(k);
                                                        code.put(_PARCEL_PREPAID_DETAIL_ID, parcelPrepaidDetailId);
                                                        packagesTasks.add(registerPackagesInParcel(conn, code, parcelId, finalPaysSender, createdBy, terminalOriginId, isInternalParcel, isPendingCollection));
                                                    }
                                                }
                                            }

                                            CompletableFuture.allOf(packagesTasks.toArray(new CompletableFuture[packagesTasks.size()])).whenComplete((ps, pt) -> {
                                                try {
                                                    if (pt != null) {
                                                        throw pt;
                                                    }
                                                    CompletableFuture.allOf(parcelPackings.stream()
                                                                    .map(p -> registerPackingsInParcel(conn, (JsonObject) p, parcelId, createdBy))
                                                                    .toArray(CompletableFuture[]::new))
                                                            .whenComplete((ks, kt) -> {
                                                                try {
                                                                    if (kt != null) {
                                                                        throw kt;
                                                                    }

                                                                    JsonObject parcelTotalAmount = this.getParcelTotalAmount(parcel, parcelPackings);

                                                                    Double amount = parcelTotalAmount.getDouble(_AMOUNT, 0.0);
                                                                    Double discount = parcelTotalAmount.getDouble(_DISCOUNT, 0.0);
                                                                    Double extraCharges = parcelTotalAmount.getDouble(_EXTRA_CHARGES, 0.0);
                                                                    Double iva = parcelTotalAmount.getDouble(_IVA, 0.0);
                                                                    Double parcelIva = costExchange.getDouble(_PARCEL_IVA, 0.0);
                                                                    Double totalAmount = parcelTotalAmount.getDouble(_TOTAL_AMOUNT, 0.0);
                                                                    Double excessAmount = costExchange.getDouble(_EXCESS_AMOUNT, 0.0);
                                                                    Double excessDiscount = costExchange.getDouble(_EXCESS_DISCOUNT, 0.0);
                                                                    Double servicesRadAmount = costExchange.getDouble(_SERVICES_RAD_AMOUNT, 0.0);
                                                                    Double servicesEadAmount = costExchange.getDouble(_SERVICES_EAD_AMOUNT, 0.0);

                                                                    parcel.put(_AMOUNT, amount)
                                                                        .put(_DISCOUNT, discount)
                                                                        .put(_EXTRA_CHARGES, extraCharges)
                                                                        .put(_EXCESS_AMOUNT, excessAmount)
                                                                        .put(_EXCESS_DISCOUNT, excessDiscount)
                                                                        .put(_SERVICES_AMOUNT, servicesRadAmount + servicesEadAmount)
                                                                        .put(_IVA, iva)
                                                                        .put(_PARCEL_IVA, parcelIva)
                                                                        .put(_TOTAL_AMOUNT, totalAmount)
                                                                        .put(UPDATED_BY, createdBy)
                                                                        .put(UPDATED_AT, sdfDataBase(new Date()));

                                                                    insuranceValidations(insuranceValue, ivaPercent).whenComplete((insuranceObj, errInsurance) -> {
                                                                        try {
                                                                            if (errInsurance != null) {
                                                                                throw errInsurance;
                                                                            }

                                                                            boolean hasInsurance = insuranceObj.getBoolean("has_insurance");
                                                                            parcel.put("has_insurance", hasInsurance);

                                                                            if (hasInsurance) {
                                                                                int maxInsuranceValue = insuranceObj.getInteger("max_insurance_value");
                                                                                double insuranceAmountBeforeIva = insuranceObj.getDouble(_INSURANCE_AMOUNT_BEFORE_IVA);
                                                                                double insuranceAmountIva = insuranceObj.getDouble(_INSURANCE_AMOUNT_IVA);
                                                                                int insuranceId = insuranceObj.getInteger("insurance_id");
                                                                                parcel.put("insurance_id", insuranceId);

                                                                                double ivaI = parcel.getDouble(_IVA, 0.0);
                                                                                double totalAmountI = parcel.getDouble(_TOTAL_AMOUNT, 0.0);

                                                                                if (insuranceAmountBeforeIva > maxInsuranceValue) {
                                                                                    double insuranceMaxValueBeforeIva = UtilsMoney.round((maxInsuranceValue / (ivaPercent / 100)), 2);
                                                                                    double insuranceMaxValueIva = UtilsMoney.round(insuranceMaxValueBeforeIva * ivaPercent, 2);

                                                                                    ivaI = UtilsMoney.round(ivaI + insuranceMaxValueIva, 2);
                                                                                    totalAmountI = UtilsMoney.round(totalAmountI + insuranceMaxValueBeforeIva + insuranceMaxValueIva, 2);

                                                                                    parcel.put(_INSURANCE_AMOUNT, insuranceMaxValueBeforeIva);
                                                                                } else {
                                                                                    ivaI = UtilsMoney.round(ivaI + insuranceAmountIva, 2);
                                                                                    totalAmountI = UtilsMoney.round(totalAmountI + insuranceAmountBeforeIva + insuranceAmountIva, 2);
                                                                                    parcel.put(_INSURANCE_AMOUNT, insuranceAmountBeforeIva);
                                                                                }

                                                                                parcel.put(_IVA, UtilsMoney.round(ivaI, 2))
                                                                                        .put(_TOTAL_AMOUNT, UtilsMoney.round(totalAmountI, 2));

                                                                            }

                                                                            if(isInternalParcel) {
                                                                                parcel.put(_DISCOUNT, parcel.getDouble(_TOTAL_AMOUNT))
                                                                                        .put(_TOTAL_AMOUNT, 0.0);
                                                                            }

                                                                            this.endRegister(conn, cashOutId, parcel, parcelId, createdBy, payments, isComplement,
                                                                                        cashChange, currencyId, finalCodes, parcelPackings, objPayback,
                                                                                        paybackPoints, paybackMoney, internalCustomer, isInternalParcel, isCredit, customerCreditData, false , shipmentType, typeServiceId, zipCodeService, purchaseOrigin).whenComplete((resultRegister, errorRegister) -> {
                                                                                try {
                                                                                    if (errorRegister != null){
                                                                                        throw errorRegister;
                                                                                    }
                                                                                    future.complete(resultRegister);
                                                                                } catch (Throwable t){
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

    private CompletableFuture<Integer> createParcel(SQLConnection conn, JsonObject parcel, Integer cashRegisterId, boolean internalCustomer, Integer integrationPartnerSessionId, String purchaseOrigin) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Integer resourceId = cashRegisterId;
        if ("web service".equals(purchaseOrigin)) {
            resourceId = integrationPartnerSessionId;
        }
        this.dbVerticle.generateCompuestID("parcel", resourceId, internalCustomer, purchaseOrigin).whenComplete((resultCompuestId, errorCompuestId) -> {
            try {
                if (errorCompuestId != null) {
                    throw errorCompuestId;
                }
                parcel.put("waybill", resultCompuestId);
                this.getPromiseDeliveryDate(parcel).whenComplete((promiseDeliveryDate, errPDD) -> {
                    try {
                        if (errPDD != null) {
                            throw errPDD;
                        }
                        if(Objects.nonNull(promiseDeliveryDate)) {
                            parcel.put(_PROMISE_DELIVERY_DATE, UtilsDate.sdfDataBase(promiseDeliveryDate));
                        }
                        GenericQuery gen = this.generateGenericCreate(parcel);
                        conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> parcelReply) -> {
                            try {
                                if (parcelReply.failed()) {
                                    throw parcelReply.cause();
                                }
                                final int parcelId = parcelReply.result().getKeys().getInteger(0);
                                future.complete(parcelId);
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

    private CompletableFuture<Date> getPromiseDeliveryDate(JsonObject parcel) {
        CompletableFuture<Date> future = new CompletableFuture<>();
        try {
            int terminalOriginId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
            int terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
            String shipmentType = parcel.getString(_SHIPMENT_TYPE);
            JsonObject body = new JsonObject()
                    .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                    .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                    .put(_SHIPMENT_TYPE, shipmentType);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_GET_PROMISE_DELIVERY_DATE);
            this.getVertx().eventBus().send(ParcelDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    JsonObject result = (JsonObject) reply.result().body();
                    String promiseDeliveryDateString = result.getString(_PROMISE_DELIVERY_DATE);
                    Date promiseDeliveryDate = null;
                    if (Objects.nonNull(promiseDeliveryDateString)) {
                        promiseDeliveryDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(promiseDeliveryDateString);
                    }
                    future.complete(promiseDeliveryDate);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insuranceValidations(Double insuranceValue, Double ivaPercent) throws ParseException {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        if (Objects.nonNull(insuranceValue) && insuranceValue > 0) {
            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<ResultSet> f3 = Future.future();

            this.getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "insurance_percent"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            this.getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "max_insurance_value"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            String now = format_yyyy_MM_dd(new Date());
            this.dbClient.queryWithParams(QUERY_INSURANCE_VALIDATION, new JsonArray().add(now), f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject field1 = reply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = reply.result().<Message<JsonObject>>resultAt(1).body();
                    List<JsonObject> field3 = reply.result().<ResultSet>resultAt(2).getRows();

                    if (field3.isEmpty()) {
                        throw new Exception("There are no insurance policies available");
                    }

                    int insuranceId = field3.get(0).getInteger(ID);
                    double insurancePercent = Double.parseDouble(field1.getString("value"));
                    int maxInsuranceValue = Integer.parseInt(field2.getString("value"));
                    double insuranceAmount = UtilsMoney.round((insuranceValue * insurancePercent) / 100, 2);
                    double insuranceAmountBeforeIva = UtilsMoney.round(insuranceAmount / (1 + (ivaPercent / 100)), 2);
                    double insuranceAmountIva = UtilsMoney.round(insuranceAmountBeforeIva * (ivaPercent / 100), 2);
                    boolean hasInsurance = insuranceAmount > 0;

                    future.complete(new JsonObject()
                            .put("insurance_id", insuranceId)
                            .put("max_insurance_value", maxInsuranceValue)
                            .put(_INSURANCE_AMOUNT_BEFORE_IVA, insuranceAmountBeforeIva)
                            .put(_INSURANCE_AMOUNT_IVA, insuranceAmountIva)
                            .put("has_insurance", hasInsurance));

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } else {
            future.complete(new JsonObject().put("has_insurance", false));
        }

        return future;
    }

    private JsonObject getParcelTotalAmount(JsonObject parcel, JsonArray parcelPackings){

        Double amount = parcel.getDouble(_AMOUNT, 0.0);
        Double discount = parcel.getDouble(_DISCOUNT, 0.0);
        Double iva = parcel.getDouble(_IVA, 0.0);
        Double totalAmount = parcel.getDouble(_TOTAL_AMOUNT, 0.0);
        double extraCharges = 0.0;
        for (int i = 0; i < parcelPackings.size(); i++) {
            JsonObject parcelPacking = parcelPackings.getJsonObject(i);
            Double packingAmount = parcelPacking.getDouble(_AMOUNT, 0.0);
            Double packingIva = parcelPacking.getDouble(_IVA, 0.0);
            Double packingTotalAmount = parcelPacking.getDouble(_TOTAL_AMOUNT, 0.0);
            extraCharges += packingAmount;
            iva += packingIva;
            totalAmount += packingTotalAmount;
        }

        return new JsonObject()
                .put(_AMOUNT, amount)
                .put(_DISCOUNT, discount)
                .put(_IVA, iva)
                .put(_TOTAL_AMOUNT, totalAmount)
                .put(_EXTRA_CHARGES, extraCharges);
    }

    private CompletableFuture<Boolean> comapreCityId(String typeCustomer, Integer customerId, Integer terminalId, Boolean is_complement){

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            if(is_complement){
                future.complete(true);
                return future;
            }
            String QUERY_GET_CITY = null;
            if (typeCustomer.equals("sender")){
                QUERY_GET_CITY = QUERY_GET_PARCEL_CITY_SENDER;
            } else if (typeCustomer.equals("addressee")){
                QUERY_GET_CITY = QUERY_GET_PARCEL_CITY_ADDRESSEE;
            }

            this.dbClient.queryWithParams(QUERY_GET_CITY, new JsonArray().add(customerId),replyCustomer ->{
                try{
                    if(replyCustomer.failed()){
                        throw new Exception(replyCustomer.cause());
                    }
                    List<JsonObject> result = replyCustomer.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("city_id of " + typeCustomer + " not found");
                    }
                    int customerCityId = result.get(0).getInteger("city_id");
                    this.dbClient.queryWithParams(QUERY_GET_TERMINAL_CITY, new JsonArray().add(terminalId),replyTerminal ->{
                        try {
                            if (replyTerminal.failed()){
                                throw replyTerminal.cause();
                            }
                            List<JsonObject> resultTerminal = replyCustomer.result().getRows();
                            if (resultTerminal.isEmpty()){
                                throw new Exception("city_id of " + typeCustomer + " not found");
                            }
                            int terminalCityId = resultTerminal.get(0).getInteger("city_id");
                            if (customerCityId == terminalCityId){
                                future.complete(true);
                            } else {
                                future.complete(false);
                            }
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });

                }catch (Exception ex){
                    future.completeExceptionally(ex);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateParcelPrepaid(SQLConnection conn, Integer parcelPrepaidId, Double percentDiscountApplied, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(percentDiscountApplied)
                    .add(UtilsDate.sdfDataBase(new Date()))
                    .add(createdBy)
                    .add(parcelPrepaidId);

            conn.updateWithParams(QUERY_UPDATE_PARCEL_PREPAID_INFO, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateParcelPackageDetails(SQLConnection conn, JsonArray parcelPrepaidDetailIds, Integer parcelId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String parcelPrepaidDetailIdParams = parcelPrepaidDetailIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            String QUERY_UPDATE = String.format(QUERY_UPDATE_PARCEL_PREPAID_DETAIL_INFO, parcelPrepaidDetailIdParams);
            JsonArray params = new JsonArray()
                    .add(parcelId)
                    .add(UtilsDate.sdfDataBase(new Date()))
                    .add(createdBy);

            conn.updateWithParams(QUERY_UPDATE, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> registerPackagesInParcel(SQLConnection conn, JsonObject parcelPackage, Integer parcelId, Boolean paysSender, Integer createdBy, Integer terminalId, Boolean isInternalParcel, boolean isPendingCollection) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonObject pack = parcelPackage.copy();
            JsonArray incidences = pack.getJsonArray("packages_incidences");
            pack.put(_PARCEL_ID, parcelId);
            pack.put(CREATED_BY, createdBy);
            pack.put(_PACKAGE_CODE, UtilsID.generateID("P"));

            Integer quantity = pack.getInteger(_QUANTITY);

            Double excessAmount = pack.getDouble(_EXCESS_AMOUNT);
            Double excessDiscount = pack.getDouble(_EXCESS_DISCOUNT);
            pack.put(_EXCESS_COST, UtilsMoney.round(excessAmount / quantity, 2));
            pack.put(_EXCESS_DISCOUNT, UtilsMoney.round(excessDiscount / quantity, 2));
            Double iva = pack.getDouble(_IVA);
            Double parcelIva = pack.getDouble(_PARCEL_IVA);
            Double totalAmount = isInternalParcel ? 0.00 : pack.getDouble(_TOTAL_AMOUNT);

            Integer packagePriceId;
            if (excessAmount > 0) {
                packagePriceId = pack.getInteger(_BASE_PACKAGE_PRICE_ID);
            } else {
                packagePriceId = pack.getInteger(_PACKAGE_PRICE_ID);
            }
            pack.put(_PACKAGE_PRICE_ID, packagePriceId);

            pack.put(IVA, UtilsMoney.round(iva / quantity, 2));
            pack.put(_PARCEL_IVA, UtilsMoney.round(parcelIva / quantity, 2));
            pack.put(_TOTAL_AMOUNT, UtilsMoney.round(totalAmount / quantity, 2));

            String notes = pack.getString("notes");

            Boolean isDiscountByExcess = pack.getBoolean(_IS_DISCOUNT_BY_EXCESS, false);
            if (isDiscountByExcess) {
                JsonObject promoAppliedInfo = pack.getJsonObject(_PROMO_APPLIED_INFO, new JsonObject());
                pack.put(_EXCESS_PROMO_ID, promoAppliedInfo.getInteger(ID));
            }

            if (isPendingCollection) {
                pack.put(_PACKAGE_STATUS, PACKAGE_STATUS.PENDING_COLLECTION.ordinal());
            }

            JsonObject parcelPackageObj = JsonObject.mapFrom(pack.mapTo(ParcelPackage.class));
            GenericQuery create = this.generateGenericCreate("parcels_packages", parcelPackageObj);
            conn.updateWithParams(create.getQuery(), create.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    final int parcelPackageId = reply.result().getKeys().getInteger(0);
                    pack.put(ID, parcelPackageId);

                    this.addParcelPackageTracking(conn, parcelId, parcelPackageId, notes, createdBy, paysSender, terminalId, isPendingCollection).whenComplete((resultAddPPT, errAddPPT) -> {
                        try {
                            if (errAddPPT != null){
                                throw new Exception(errAddPPT);
                            }

                            if (incidences.isEmpty()) {
                                future.complete(true);
                            } else {
                                final int len = incidences.size();
                                List<GenericQuery> inserts = new ArrayList<>();

                                for(int i = 0; i < len; i++) {
                                    JsonObject incidence = incidences.getJsonObject(i);
                                    incidence.put("parcel_id", parcelId);
                                    incidence.put("parcel_package_id", parcelPackageId);
                                    incidence.put("created_by", createdBy);
                                    inserts.add(this.generateGenericCreate("parcels_incidences", incidence));
                                }

                                List<JsonArray> params = inserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());
                                conn.batchWithParams(inserts.get(0).getQuery(), params, (AsyncResult<List<Integer>> ar) -> {
                                    try {
                                        if (ar.failed()) {
                                            throw ar.cause();
                                        }
                                        future.complete(true);
                                    } catch (Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });
                            }
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });

                }catch (Exception ex){
                    future.completeExceptionally(ex);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> addParcelPackageTracking(SQLConnection conn, Integer parcelId, int parcelPackageId, String notes, Integer createdBy, Boolean paysSender, Integer terminalId, boolean isPendingCollection) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            List<GenericQuery> trackingInserts = new ArrayList<>();
            JsonObject parcelPackageTracking = new JsonObject()
                    .put(_PARCEL_ID, parcelId)
                    .put(_PARCEL_PACKAGE_ID, parcelPackageId)
                    .put(_ACTION, PARCELPACKAGETRACKING_STATUS.REGISTER.getValue())
                    .put(_TERMINAL_ID, terminalId)
                    .put(_NOTES, notes)
                    .put(CREATED_BY, createdBy);
            trackingInserts.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));

            if(paysSender) {
                parcelPackageTracking.put(_ACTION, PARCELPACKAGETRACKING_STATUS.PAID.getValue());
                trackingInserts.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));
            }

            parcelPackageTracking.put(_ACTION,
                    isPendingCollection ? PARCELPACKAGETRACKING_STATUS.PENDING_COLLECTION.getValue()
                    : PARCELPACKAGETRACKING_STATUS.IN_ORIGIN.getValue());
            trackingInserts.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));

            List<JsonArray> params = trackingInserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());

            conn.batchWithParams(trackingInserts.get(0).getQuery(), params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(parcelPackageTracking);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerPackingsInParcel(SQLConnection conn, JsonObject parcelPacking, Integer parcelId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        parcelPacking.put("parcel_id", parcelId)
                .put("created_by", createdBy);

        Integer packingsId = parcelPacking.getInteger("packing_id");

        if (packingsId == null) {
            future.complete(new JsonObject());
            return future;
        }

//        JsonArray params = new JsonArray()
//                .add(packingsId);

//        conn.queryWithParams(QUERY_PACKINGS_BY_ID, params, reply -> {
//            try{
//                if(reply.failed()){
//                    throw new Exception(reply.cause());
//                }
//                List<JsonObject> rows = reply.result().getRows();
//                if (rows.isEmpty()) {
//                    future.completeExceptionally(new Throwable("Packing: Not found"));
//                } else {
//                    JsonObject packing = rows.get(0);
////                    Double cost = packing.getDouble("cost", 0.0);
                    Double unitPrice = parcelPacking.getDouble("unit_price");
                    Double iva = parcelPacking.getDouble(_IVA);
                    Integer quantity = parcelPacking.getInteger(_QUANTITY);
                    double amount = unitPrice * quantity;
                    double totalIva = iva * quantity;
                    parcelPacking.put("amount", UtilsMoney.round(amount, 2));
                    parcelPacking.put("discount", 0.0);
                    parcelPacking.put(_IVA, UtilsMoney.round(totalIva, 2));
                    parcelPacking.put("total_amount", UtilsMoney.round(amount + totalIva, 2));

                    GenericQuery insert = this.generateGenericCreate("parcels_packings", parcelPacking);
                    conn.updateWithParams(insert.getQuery(), insert.getParams(), replyInsert -> {
                        try {
                            if (replyInsert.failed()){
                                throw new Exception(replyInsert.cause());
                            }
                            future.complete(parcelPacking);
                        }catch(Exception e ){
                            future.completeExceptionally(e);
                        }
                    });
//                }
//            }catch (Exception ex){
//                future.completeExceptionally(ex);
//            }
//        });
        return future;
    }

    protected CompletableFuture<JsonObject> internalCustomerValidation(Boolean flagInternalCustomer,Boolean isInternalParcel, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (flagInternalCustomer && isInternalParcel){
                Double insuranceAmount = body.getDouble(INSURANCE_AMOUNT, 0.00);
                Double extraCharges = body.getDouble(EXTRA_CHARGES, 0.00);
                Double servicesAmount = body.getDouble(_SERVICES_AMOUNT, 0.00);
                Double excessAmount = body.getDouble(_EXCESS_AMOUNT);
                Double amount = body.getDouble(AMOUNT);
                body.put(_DISCOUNT, amount + insuranceAmount + extraCharges + servicesAmount);
                body.put(_EXCESS_DISCOUNT, excessAmount);
                body.put(_IVA, 0.0);
                body.put(_PARCEL_IVA, 0.0);
                body.put(TOTAL_AMOUNT, 0.00);
                future.complete(body);
            } else {
                future.complete(body);
            }
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    protected CompletableFuture<JsonObject> endRegister(SQLConnection conn, Integer cashOutId, JsonObject parcel, Integer parcelId, Integer createdBy,
                                                        JsonArray payments, Boolean is_complement, JsonObject cashChange, Integer currencyId, JsonArray parcelPackages,
                                                        JsonArray parcelPackings, PaybackDBV objPayback, Double paybackPoints, Double paybackMoney,  Boolean internalCustomer, Boolean isInternalParcel, Boolean is_credit,
                                                        JsonObject customerCreditData, boolean reissue, SHIPMENT_TYPE shipmentType, Integer typeServiceId, Integer zipCodeService, String purchaseOrigin){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.internalCustomerValidation(internalCustomer,isInternalParcel, parcel).whenComplete((resultInternalCustomer, errorInternalCustomer) -> {
                try {
                    if (errorInternalCustomer != null) {
                        throw new Exception(errorInternalCustomer);
                    }
                    parcel.mergeIn(resultInternalCustomer, true);
                    if (internalCustomer && isInternalParcel) {
                        parcel.put(PAYS_SENDER, true);
                    }

                    String parcelAction = is_credit ? "voucher" : "purchase";
                    boolean paysSender = parcel.getBoolean(PAYS_SENDER);
                    Double innerTotalAmount = UtilsMoney.round(parcel.getDouble(TOTAL_AMOUNT), 2);
                    double creditBalance = 0;
                    double debt = 0;
                    double iva = parcel.getDouble(_IVA);
                    double parcelIva = parcel.getDouble(_PARCEL_IVA);

                    if(is_credit){
                        if (reissue){
                            Double previousDebt = (Double) parcel.remove("reissue_debt");
                            Double reissuePaid = parcel.containsKey("reissue_paid") ? (Double) parcel.remove("reissue_paid") : 0.00;
                            boolean reissueHasDebt = (reissuePaid > 0 && reissuePaid < innerTotalAmount) || previousDebt > 0;
                            if (reissueHasDebt){
                                debt = innerTotalAmount - reissuePaid;
                            } else {
                                creditBalance = reissuePaid - innerTotalAmount;
                            }
                        } else {
                            debt = innerTotalAmount;
                        }
                        parcel.put("debt", debt);
                    }

                    GenericQuery update = this.generateGenericUpdate("parcels", parcel);

                    double finalCreditBalance = creditBalance;
                    double finalDebt = debt;
                    Double servicesAmount = parcel.getDouble(_SERVICES_AMOUNT, 0.0);
                    if (!paysSender) {

                        conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                            try {
                                if (replyUpdate.failed()){
                                    throw replyUpdate.cause();
                                }
                                if (is_credit) {
                                    this.updateCustomerCredit(conn, customerCreditData, finalDebt, createdBy, reissue, finalCreditBalance)
                                            .whenComplete((replyCustomer, errorCustomer) -> {
                                                try {
                                                    if (errorCustomer != null) {
                                                        throw new Exception(errorCustomer);
                                                    }
                                                    this.insertService(conn, createdBy, parcelId, servicesAmount, shipmentType, typeServiceId, zipCodeService).whenComplete((replyService , errorService) -> {
                                                        try{
                                                            if (errorService != null){
                                                                throw errorService;
                                                            }

                                                            JsonObject result = new JsonObject().put("id", parcelId);
                                                            result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                            future.complete(result);

                                                        } catch (Throwable t){
                                                            future.completeExceptionally(t);
                                                        }
                                                    });

                                                } catch (Throwable t) {
                                                    future.completeExceptionally(t);
                                                }
                                            });
                                } else {
                                    JsonObject result = new JsonObject().put("id", parcelId);
                                    result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                                    this.insertService(conn, createdBy, parcelId, servicesAmount, shipmentType, typeServiceId, zipCodeService).whenComplete((replyService , errorService) -> {
                                        try{
                                            if (errorService != null){
                                                throw errorService;
                                            }

                                            future.complete(result);

                                        } catch (Throwable t){
                                            future.completeExceptionally(t);
                                        }
                                    });

                                }
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    } else {
                        double totalPayments = 0.0;
                        if(payments == null && !is_complement && !internalCustomer && !isInternalParcel){
                            throw new Exception("No payment object was found");
                        }
                        if (payments == null && is_complement){
                            JsonObject result = new JsonObject().put("id", parcelId);
                            result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                            future.complete(result);
                        } else {
                            final int pLen = payments == null ? 0 : payments.size();

                            for (int i = 0; i < pLen; i++) {
                                JsonObject payment = payments.getJsonObject(i);
                                Double paymentAmount = payment.getDouble("amount");
                                if (paymentAmount == null || paymentAmount < 0.0) {
                                    throw new Exception("Invalid payment amount: " + paymentAmount);
                                }
                                totalPayments += UtilsMoney.round(paymentAmount, 2);
                            }

                            if(!is_credit) {
                                if (totalPayments > innerTotalAmount) {
                                    throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
                                }
                                if (totalPayments < innerTotalAmount) {
                                    throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
                                }
                            }
                            // Insert ticket
                            //double finalCreditBalance = creditBalance;
                            this.insertTicket(conn, cashOutId, parcelId, innerTotalAmount, cashChange, createdBy, iva, parcelIva, parcelAction).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                try {
                                    if (ticketError != null) {
                                        throw ticketError;
                                    }

                                    // Insert ticket details
                                    this.insertTicketDetail(conn, ticket.getInteger("id"), createdBy, parcelPackages, parcelPackings, parcel, isInternalParcel, shipmentType, servicesAmount).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                        try {
                                            if (dError != null) {
                                                throw dError;
                                            }

                                            if (is_credit) {
                                                conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                    try {
                                                        if (replyUpdate.failed()) {
                                                            throw replyUpdate.cause();
                                                        }
                                                        JsonObject paramMovPayback = new JsonObject()
                                                                .put("customer_id", parcel.getInteger("sender_id"))
                                                                .put("points", paybackPoints)
                                                                .put("money", paybackMoney)
                                                                .put("type_movement", "I")
                                                                .put("motive", "Envio de paquetería(sender)")
                                                                .put("id_parent", parcelId)
                                                                .put("employee_id", createdBy);
                                                        objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                                                            try {
                                                                if (errorMP != null) {
                                                                    throw errorMP;
                                                                }

                                                                this.updateCustomerCredit(conn, customerCreditData, finalDebt, createdBy, reissue, finalCreditBalance)
                                                                        .whenComplete((replyCustomer, errorCustomer) -> {
                                                                            try{
                                                                                if (errorCustomer != null) {
                                                                                    throw new Exception(errorCustomer);
                                                                                }
                                                                                this.insertService(conn, createdBy, parcelId, servicesAmount, shipmentType, typeServiceId, zipCodeService).whenComplete((replyService , errorService) -> {
                                                                                    try{
                                                                                        if (errorService != null){
                                                                                            throw errorService;
                                                                                        }

                                                                                        JsonObject result = new JsonObject().put("id", parcelId);
                                                                                        result.put("ticket_id", ticket.getInteger("id"))
                                                                                                .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                                        future.complete(result);

                                                                                    } catch (Throwable t){
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
                                                });
                                            } else {
                                                // insert payments
                                                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                for (int i = 0; i < pLen; i++) {
                                                    JsonObject payment = payments.getJsonObject(i);
                                                    payment.put("ticket_id", ticket.getInteger("id"));
                                                    pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcelId, cashOutId, createdBy, is_credit));
                                                }
                                                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));

                                                allPayments.whenComplete((s, tt) -> {
                                                    try {
                                                        if (tt != null) {
                                                            throw tt;
                                                        }
                                                        conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                            try {
                                                                if (replyUpdate.failed()) {
                                                                    throw replyUpdate.cause();
                                                                }
                                                                JsonObject paramMovPayback = new JsonObject()
                                                                        .put("customer_id", parcel.getInteger(Constants.CUSTOMER_ID))
                                                                        .put("points", paybackPoints)
                                                                        .put("money", paybackMoney)
                                                                        .put("type_movement", "I")
                                                                        .put("motive", "Envio de paquetería(sender)")
                                                                        .put("id_parent", parcelId)
                                                                        .put("employee_id", createdBy);
                                                                objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                                                                    try {
                                                                        if (errorMP != null) {
                                                                            throw errorMP;
                                                                        }

                                                                        this.insertService(conn, createdBy, parcelId, servicesAmount, shipmentType, typeServiceId, zipCodeService).whenComplete((replyService , errorService) -> {
                                                                            try{
                                                                                if (errorService != null){
                                                                                    throw errorService;
                                                                                }

                                                                                JsonObject result = new JsonObject().put("id", parcelId);
                                                                                result.put("ticket_id", ticket.getInteger("id"))
                                                                                        .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                                future.complete(result);

                                                                            } catch (Throwable t){
                                                                                future.completeExceptionally(t);
                                                                            }
                                                                        });
                                                                    } catch (Throwable t){
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
                                            }
                                        } catch (Throwable t){
                                            future.completeExceptionally(t);
                                        }
                                    });
                                } catch (Throwable t){
                                    future.completeExceptionally(t);
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer packageId, Integer cashOutId, Integer createdBy, Boolean is_credit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (!is_credit) {
                JsonObject cashOutMove = new JsonObject();
                payment.put("currency_id", currencyId);
                payment.put("parcel_id", packageId);
                payment.put("created_by", createdBy);
                cashOutMove.put("quantity", payment.getDouble("amount"));
                cashOutMove.put("move_type", "0");
                cashOutMove.put("cash_out_id", cashOutId);
                cashOutMove.put("created_by", createdBy);
                PaymentDBV objPayment = new PaymentDBV();
                objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        payment.put("id", resultPayment.getInteger("id"));
                        cashOutMove.put("payment_id", resultPayment.getInteger("id"));
                        GenericQuery insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                        conn.updateWithParams(insertCashOutMove.getQuery(), insertCashOutMove.getParams(), (AsyncResult<UpdateResult> replyMove) -> {
                            try {
                                if (replyMove.failed()) {
                                    throw new Exception(replyMove.cause());
                                }
                                future.complete(payment);


                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
                    }
                });
            } else {
                future.complete(payment);
            }
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt, Integer createdBy, boolean reissue, Double creditBalance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            Double actualCreditBalance = customerCreditData.getDouble("credit_balance");
            JsonObject customerObject = new JsonObject();

            double creditAvailable;
            if (reissue){
                customerObject.put("credit_balance", creditBalance > 0 ? actualCreditBalance + creditBalance : actualCreditBalance);
                creditAvailable = debt > 0 ? actualCreditAvailable - debt : actualCreditAvailable;
            } else {
                creditAvailable = actualCreditAvailable - debt;
            }

            customerObject
                    .put(ID, customerCreditData.getInteger(ID))
                    .put("credit_available", creditAvailable)
                    .put(UPDATED_BY, createdBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            GenericQuery updateCostumer = this.generateGenericUpdate("customer", customerObject);

            conn.updateWithParams(updateCostumer.getQuery(), updateCostumer.getParams(), (AsyncResult<UpdateResult> replyCustomer) -> {
                try {
                    if (replyCustomer.failed()) {
                        throw replyCustomer.cause();
                    }
                    future.complete(replyCustomer.succeeded());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertService(SQLConnection conn, Integer createdBy, Integer parcelId, Double servicesAmount, SHIPMENT_TYPE shipmentType, Integer typeServiceId, Integer zipCodeService) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject service = new JsonObject();
            if(shipmentType.includeRAD() || shipmentType.includeEAD()){

                service.put("parcel_id", parcelId);
                service.put("amount", servicesAmount);
                service.put("id_type_service", typeServiceId);
                service.put("zip_code", zipCodeService);
                service.put("created_by", createdBy);
                service.put("created_at", sdfDataBase(new Date()));
                if(shipmentType.includeRAD()){
                    service.put("confirme_rad", 0);
                    service.put("status", 1);
                }

                if(shipmentType.includeRAD() && shipmentType.includeEAD()){
                    service.put("confirme_rad", 1);
                    service.put("status", 1);
                }

                if(shipmentType.includeEAD() ){
                    service.put("confirme_rad", 1);
                    service.put("status", 1);
                }

                GenericQuery insert = this.generateGenericCreate("parcels_rad_ead", service);
                conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                    try{
                        if (reply.succeeded()) {

                            future.complete(service);

                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            }
            else{
                future.complete(service);
            }


        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Integer parcelId, Double totalPayments, JsonObject cashChange, Integer createdBy, Double iva, Double parcelIva, String action) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();

            ticket.put("parcel_id", parcelId);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
            ticket.put("parcel_iva", parcelIva);
            ticket.put("total", totalPayments);
            ticket.put("created_by", createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));
            if(action != null){
                ticket.put("action", action);
            }

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble("total");
                double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                double differencePaid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if(!Objects.equals(action, "voucher")) {
                    if (totalPayments < total) {
                        throw new Throwable("The payment " + total + " is greater than the total " + totalPayments);
                    } else if (totalPayments > total) {
                        throw new Throwable("The payment " + total + " is lower than the total " + totalPayments);
                    } else if (paid_change > differencePaid) {
                        throw new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                    } else if (paid_change < differencePaid) {
                        throw new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                    }
                }
            } else {
                ticket.put("paid", totalPayments);
                ticket.put("paid_change", 0.0);
            }

            GenericQuery insert = this.generateGenericCreate("tickets", ticket);

            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if (reply.succeeded()) {
                        final int id = reply.result().getKeys().getInteger(0);
                        ticket.put("id", id);
                        future.complete(ticket);
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer ticketId, Integer createdBy, JsonArray packages, JsonArray packings, JsonObject parcel, Boolean isInternalParcel, SHIPMENT_TYPE shipmentType, Double servicesAmount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            Map<String, List<JsonObject>> groupedPackages = packages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString(_BASE_PACKAGE_PRICE_NAME)));
            JsonArray details = new JsonArray();

            groupedPackages.forEach((packagePriceName, groupedPackagesByPackagePriceName) -> {
                for (JsonObject pack : groupedPackagesByPackagePriceName) {
                    String shippingType = pack.getString(_SHIPPING_TYPE);
                    Integer packQuantity = pack.getInteger(_QUANTITY);
                    Double packAmount = pack.getDouble(_AMOUNT);
                    Double packDiscount = pack.getDouble(_DISCOUNT);
                    Double packTotalAmount = pack.getDouble(_TOTAL_AMOUNT);
                    JsonObject ticketDetail = new JsonObject();
                    switch (shippingType){
                        case "parcel":
                            shippingType = "paquetería";
                            break;
                        case "courier":
                            shippingType = "mensajería";
                            break;
                        case "pets":
                            shippingType = "mascota";
                            break;
                        case "frozen":
                            shippingType = "carga refrigerada";
                            break;
                    }
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put(_QUANTITY, packQuantity);
                    ticketDetail.put("detail", "Envío de " + shippingType + " con rango " + packagePriceName);
                    ticketDetail.put("unit_price", packAmount);
                    ticketDetail.put(_DISCOUNT, packDiscount);
                    ticketDetail.put(_AMOUNT, packTotalAmount);
                    ticketDetail.put(CREATED_BY, createdBy);
                    details.add(ticketDetail);
                }
            });

            int len = packings.size();
            for (int i = 0; i < len; i++) {
                JsonObject packing = packings.getJsonObject(i);
                JsonObject ticketDetail = new JsonObject();

                ticketDetail.put("ticket_id", ticketId);
                ticketDetail.put(_QUANTITY, packing.getInteger("quantity"));
                ticketDetail.put("detail", "Embalaje");
                ticketDetail.put("unit_price", packing.getDouble("unit_price"));
                ticketDetail.put(_AMOUNT, packing.getDouble("amount"));
                ticketDetail.put("created_by", createdBy);

                details.add(ticketDetail);
            }

            if(parcel.getBoolean("has_insurance") != null && parcel.getBoolean("has_insurance")){
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put("ticket_id", ticketId);
                ticketDetail.put(_QUANTITY, 1);
                ticketDetail.put("detail", "Seguro de envío");
                ticketDetail.put("unit_price", parcel.getDouble("insurance_amount"));
                ticketDetail.put(_AMOUNT, parcel.getDouble("insurance_amount"));
                ticketDetail.put("created_by", createdBy);

                details.add(ticketDetail);
            }

            if(shipmentType.includeRAD() || shipmentType.includeEAD()){
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put("ticket_id", ticketId);
                ticketDetail.put(_QUANTITY, 1);
                ticketDetail.put("detail", "Servicio " + shipmentType.getValue());
                ticketDetail.put("unit_price", servicesAmount);
                ticketDetail.put(_AMOUNT, servicesAmount);
                ticketDetail.put("created_by", createdBy);

                details.add(ticketDetail);
            }

            List<String> inserts = new ArrayList<>();
            for(int i = 0; i < details.size(); i++){
                if (isInternalParcel){
                    details.getJsonObject(i).put(_AMOUNT, 0.00);
                }
                inserts.add(this.dbVerticle.generateGenericCreate("tickets_details", details.getJsonObject(i)));
            }

            conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                try {
                    if (replyInsert.failed()){
                        throw new Exception(replyInsert.cause());
                    }
                    future.complete(replyInsert.succeeded());
                }catch(Exception e ){
                    future.completeExceptionally(e);
                }
            });

        } catch (Exception ex){
            future.completeExceptionally(ex);
        }
        return future;
    }

    private static final String QUERY_UPDATE_PARCEL_PREPAID_INFO = "UPDATE parcels_prepaid SET percent_discount_applied = ?, updated_at = ?, updated_by = ? WHERE id = ?;";

    private static final String QUERY_UPDATE_PARCEL_PREPAID_DETAIL_INFO = "UPDATE parcels_prepaid_detail SET parcel_id = ?, updated_at = ?, updated_by = ?, parcel_status = 1 WHERE id IN (%s);";

    private static final String QUERY_VALIDATE_CODES = "SELECT\n" +
            "   ppd.guiapp_code,\n" +
            "   IF(DATE(ppd.expire_at) >= DATE(NOW()), 0, 1) AS was_expired,\n" +
            "   ppd.parcel_status,\n" +
            "   pp.customer_id\n" +
            "FROM parcels_prepaid_detail ppd\n" +
            "INNER JOIN parcels_prepaid pp ON pp.id = ppd.parcel_prepaid_id\n" +
            "WHERE ppd.id IN (%s)";

    private static final String QUERY_INSURANCE_VALIDATION = "SELECT id FROM insurances WHERE ? BETWEEN init AND end AND status = 1;";

    private static final String QUERY_GET_PARCEL_CITY_SENDER = "SELECT \n" +
            "city_id \n" +
            "FROM customer_addresses AS ca \n" +
            "LEFT JOIN parcels AS p \n" +
            "ON ca.id = p.sender_address_id \n" +
            "WHERE ca.customer_id = ?;";

    private static final String QUERY_GET_PARCEL_CITY_ADDRESSEE = "SELECT \n" +
            "city_id \n" +
            "FROM customer_addresses AS ca \n" +
            "LEFT JOIN parcels AS p \n" +
            "ON ca.id = p.addressee_address_id \n" +
            "WHERE ca.customer_id = ?;";

    private static final String QUERY_GET_TERMINAL_CITY = "SELECT \n"+
            "city_id \n"+
            "FROM branchoffice\n"+
            "WHERE id = ?;";

}
