package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.insurances.handlers.InsurancesDBV.models.Insurance;
import database.money.PaybackDBV;
import database.money.PaymentDBV;
import database.parcel.GuiappDBV;
import database.parcel.ParcelDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.parcel.enums.SHIPMENT_TYPE;
import database.parcel.handlers.ParcelDBV.models.Parcel;
import database.parcel.handlers.ParcelsPackagesDBV.models.ParcelPackage;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.parcel.GuiappDBV.GET_COST_EXCHANGE;
import static database.parcel.ParcelDBV.*;
import static database.promos.PromosDBV.PACKAGE_PRICE_NAME;
import static service.commons.Constants.*;
import static utils.UtilsDate.sdfDataBase;

public class ExchangeV3 extends DBHandler<ParcelDBV> {

    public ExchangeV3(ParcelDBV dbVerticle) {
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
            parcel.remove(_IVA_PERCENT);
            parcel.remove(REISSUE);
            parcel.remove(REWORK);
            parcel.remove(CUMULATIVE_COST);
            parcel.remove(WITHOUT_FREIGHT);
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
                    createParcel(conn, parcel, cashRegisterId, internalCustomer, integrationPartnerSessionId, purchaseOrigin).whenComplete((parcelObj, errorCreateParcel) -> {
                        try {
                            if (errorCreateParcel != null) {
                                throw errorCreateParcel;
                            }

                            JsonObject bodyCostExchange = new JsonObject()
                                    .put(_COST_BREAKDOWN, true)
                                    .put(_SHIPMENT_TYPE, shipmentType.getValue())
                                    .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                                    .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                                    .put(_CUSTOMER_ID, customerId)
                                    .put(_CUSTOMER_BILLING_INFORMATION_ID, customerBillingInfoId)
                                    .put(_INSURANCE_VALUE, insuranceValue)
                                    .put(_PARCEL_PACKAGES, parcelPackages)
                                    .put(_PARCEL_PACKINGS, parcelPackings);
                            this.getVertx().eventBus().send(GuiappDBV.class.getSimpleName(), bodyCostExchange, new DeliveryOptions().addHeader(ACTION, GET_COST_EXCHANGE), (AsyncResult<Message<JsonObject>> replyCostExchange) -> {
                                try {
                                    if(replyCostExchange.failed()) {
                                        throw replyCostExchange.cause();
                                    }
                                    JsonObject costExchange = replyCostExchange.result().body();
                                    JsonArray finalParcelPackages = costExchange.getJsonArray(_PARCEL_PACKAGES);
                                    JsonArray finalParcelPackings = costExchange.getJsonArray(_PARCEL_PACKINGS);
                                    Insurance insurance = costExchange.getJsonObject(_INSURANCE).mapTo(Insurance.class);
                                    Double distanceKM = costExchange.getDouble(_DISTANCE_KM);
                                    Double totalAmount = costExchange.getDouble(_TOTAL_AMOUNT, 0.0);
                                    Double servicesRadAmount = costExchange.getDouble(_SERVICES_RAD_AMOUNT, 0.0);
                                    Double servicesEadAmount = costExchange.getDouble(_SERVICES_EAD_AMOUNT, 0.0);

                                    parcelObj.setAmount(costExchange.getDouble(_AMOUNT, 0.0));
                                    parcelObj.setDiscount(costExchange.getDouble(_DISCOUNT, 0.0));
                                    parcelObj.setIva(costExchange.getDouble(_IVA, 0.0));
                                    parcelObj.setExtraCharges(costExchange.getDouble(_EXTRA_CHARGES, 0.0));
                                    parcelObj.setExcessAmount(costExchange.getDouble(_EXCESS_AMOUNT, 0.0));
                                    parcelObj.setExcessDiscount(costExchange.getDouble(_EXCESS_DISCOUNT, 0.0));
                                    parcelObj.setServicesAmount(servicesRadAmount + servicesEadAmount);
                                    parcelObj.setParcelIva(costExchange.getDouble(_PARCEL_IVA, 0.0));
                                    parcelObj.setTotalAmount(totalAmount);
                                    parcelObj.setHasInsurance(insurance.isHasInsurance());
                                    parcelObj.setInsuranceId(insurance.getInsuranceId());
                                    parcelObj.setInsuranceAmount(insurance.getInsuranceAmount());
                                    if(isInternalParcel) {
                                        parcelObj.setDiscount(totalAmount);
                                        parcelObj.setTotalAmount(0.0);
                                    }
                                    if(isCredit){
                                        parcelObj.setDebt(parcelObj.getTotalAmount());
                                    }

                                    new PaybackDBV().calculatePointsParcel(conn, distanceKM, finalParcelPackages.size(), false).whenComplete((resultCalculate, error) -> {
                                        try {
                                            if (error != null) {
                                                throw error;
                                            }

                                            Double paybackMoney = resultCalculate.getDouble("money");
                                            Double paybackPoints = resultCalculate.getDouble("points");
                                            parcelObj.setPayback(paybackMoney);

                                            List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                                            tasks.add(generateMovementPayback(conn, parcelObj, paybackPoints, paybackMoney));

                                            JsonArray finalCodes = new JsonArray();
                                            for (int i = 0; i < finalParcelPackages.size(); i++) {
                                                JsonObject parcelPackageGrouped = finalParcelPackages.getJsonObject(i);
                                                Integer parcelPrepaidId = parcelPackageGrouped.getInteger(_PARCEL_PREPAID_ID);
                                                Double realPercentDiscountApplied = parcelPackageGrouped.getDouble(_REAL_PERCENT_DISCOUNT_APPLIED);
                                                JsonObject details = parcelPackageGrouped.getJsonObject(_DETAILS);
                                                JsonArray parcelPrepaidDetailIds = details.getJsonArray(_PARCEL_PREPAID_DETAIL_ID);

                                                tasks.add(updateParcelPrepaid(conn, parcelPrepaidId, realPercentDiscountApplied, createdBy));
                                                tasks.add(updateParcelPackageDetails(conn, parcelPrepaidDetailIds, parcelObj.getId(), createdBy));

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
                                                        tasks.add(registerPackagesInParcel(conn, code, parcelObj.getId(), finalPaysSender, createdBy, terminalOriginId, isInternalParcel, isPendingCollection));
                                                    }
                                                }
                                            }

                                            for (int i = 0; i < finalParcelPackings.size(); i++) {
                                                JsonObject parcelPacking = finalParcelPackings.getJsonObject(i);
                                                tasks.add(registerPackingsInParcel(conn, parcelPacking, parcelObj.getId(), createdBy));
                                            }

                                            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((ps, pt) -> {
                                                try {
                                                    if (pt != null) {
                                                        throw pt;
                                                    }

                                                    this.internalCustomerValidation(internalCustomer,isInternalParcel, parcelObj);
                                                    if (internalCustomer && isInternalParcel) {
                                                        parcelObj.setPaysSender(true);
                                                    }

                                                    if(isCredit){
                                                        parcelObj.setDebt(parcelObj.getTotalAmount());
                                                    }

                                                    List<CompletableFuture> tasksEnd = new ArrayList<>();
                                                    GenericQuery update = this.generateGenericUpdate("parcels", parcelObj.toJsonObject());
                                                    tasksEnd.add(dbVerticle.executeUpdate(conn, update));

                                                    if (!parcelObj.getPaysSender()) {
                                                        if (isCredit) {
                                                            tasksEnd.add(this.updateCustomerCredit(conn, customerCreditData, parcelObj));
                                                        }
                                                        tasksEnd.add(this.insertService(conn, parcelObj, shipmentType, typeServiceId, zipCodeService));
                                                    } else {
                                                        double totalPayments = 0.0;
                                                        if (payments == null) {
                                                            if (isComplement) {
                                                                future.complete(parcelObj.toJsonObject());
                                                                return;
                                                            }
                                                            if (!internalCustomer && !isInternalParcel) {
                                                                throw new Exception("No payment object was found");
                                                            }
                                                        }
                                                        int paymentsSize = payments == null ? 0 : payments.size();
                                                        for (int i = 0; i < paymentsSize; i++) {
                                                            JsonObject payment = payments.getJsonObject(i);
                                                            Double paymentAmount = payment.getDouble(_AMOUNT);
                                                            if (paymentAmount == null || paymentAmount < 0.0) {
                                                                throw new Exception("Invalid payment amount: " + paymentAmount);
                                                            }
                                                            totalPayments += paymentAmount;
                                                        }
                                                        totalPayments = UtilsMoney.round(totalPayments, 2);

                                                        if(!isCredit) {
                                                            if (totalPayments > parcelObj.getTotalAmount()) {
                                                                throw new Exception("The payment " + totalPayments + " is greater than the total " + parcelObj.getTotalAmount());
                                                            }
                                                            if (totalPayments < parcelObj.getTotalAmount()) {
                                                                throw new Exception("The payment " + totalPayments + " is lower than the total " + parcelObj.getTotalAmount());
                                                            }
                                                        }

                                                        tasksEnd.add(insertTickets(conn, cashOutId, parcelObj, finalCodes, finalParcelPackings, payments, shipmentType, cashChange, isCredit, isInternalParcel, currencyId));
                                                        tasksEnd.add(this.insertService(conn, parcelObj, shipmentType, typeServiceId, zipCodeService));

                                                        if (isCredit) {
                                                            tasksEnd.add(this.updateCustomerCredit(conn, customerCreditData, parcelObj));
                                                        }
                                                    }

                                                    CompletableFuture.allOf(tasksEnd.toArray(new CompletableFuture[tasksEnd.size()])).whenComplete((res, err) -> {
                                                        try {
                                                            if (err != null) {
                                                                throw err;
                                                            }

                                                            future.complete(parcelObj.toJsonObject());
                                                        } catch (Throwable t) {
                                                            t.printStackTrace();
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

    private CompletableFuture<Parcel> createParcel(SQLConnection conn, JsonObject parcel, Integer cashRegisterId, boolean internalCustomer, Integer integrationPartnerSessionId, String purchaseOrigin) {
        CompletableFuture<Parcel> future = new CompletableFuture<>();
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
                                Parcel parcelObj = parcel.mapTo(Parcel.class);
                                parcelObj.setId(parcelId);
                                future.complete(parcelObj);
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

    private CompletableFuture<Boolean> registerPackingsInParcel(SQLConnection conn, JsonObject parcelPacking, Integer parcelId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Integer packingId = parcelPacking.getInteger(_PACKING_ID);
        if (packingId == null) {
            future.complete(true);
            return future;
        }

        parcelPacking.put(_PARCEL_ID, parcelId);
        parcelPacking.put(CREATED_BY, createdBy);

        GenericQuery insert = this.generateGenericCreate("parcels_packings", parcelPacking);
        conn.updateWithParams(insert.getQuery(), insert.getParams(), replyInsert -> {
            try {
                if (replyInsert.failed()){
                    throw new Exception(replyInsert.cause());
                }
                future.complete(true);
            }catch(Exception e ){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    protected void internalCustomerValidation(Boolean flagInternalCustomer, Boolean isInternalParcel, Parcel parcelObj){
        if (flagInternalCustomer && isInternalParcel){
            Double insuranceAmount = parcelObj.getInsuranceAmount();
            Double extraCharges = parcelObj.getExtraCharges();
            Double servicesAmount = parcelObj.getServicesAmount();
            Double excessAmount = parcelObj.getExcessAmount();
            Double amount = parcelObj.getAmount();
            parcelObj.setDiscount(amount + insuranceAmount + extraCharges + servicesAmount);
            parcelObj.setExcessDiscount(excessAmount);
            parcelObj.setIva(0.0);
            parcelObj.setParcelIva(0.0);
            parcelObj.setTotalAmount(0.0);
        }
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer packageId, Integer cashOutId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject cashOutMove = new JsonObject();
            payment.put("currency_id", currencyId);
            payment.put(_PARCEL_ID, packageId);
            payment.put(CREATED_BY, createdBy);
            cashOutMove.put(_QUANTITY, payment.getDouble(_AMOUNT));
            cashOutMove.put("move_type", "0");
            cashOutMove.put(CASHOUT_ID, cashOutId);
            cashOutMove.put(CREATED_BY, createdBy);
            PaymentDBV objPayment = new PaymentDBV();
            objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                if (error != null) {
                    future.completeExceptionally(error);
                } else {
                    payment.put(ID, resultPayment.getInteger(ID));
                    cashOutMove.put("payment_id", resultPayment.getInteger(ID));
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
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Parcel parcelObj) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            JsonObject customerObject = new JsonObject();
            Double debt = parcelObj.getDebt();

            double creditAvailable = actualCreditAvailable - debt;

            customerObject
                    .put(ID, customerCreditData.getInteger(ID))
                    .put("credit_available", creditAvailable)
                    .put(UPDATED_BY, parcelObj.getCreatedBy())
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

    private CompletableFuture<JsonObject> insertService(SQLConnection conn, Parcel parcelObj, SHIPMENT_TYPE shipmentType, Integer typeServiceId, Integer zipCodeService) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject service = new JsonObject();
            if(shipmentType.includeRAD() || shipmentType.includeEAD()){

                service.put(_PARCEL_ID, parcelObj.getId());
                service.put(_AMOUNT, parcelObj.getServicesAmount());
                service.put("id_type_service", typeServiceId);
                service.put("zip_code", zipCodeService);
                service.put(CREATED_BY, parcelObj.getCreatedBy());
                service.put(CREATED_AT, sdfDataBase(new Date()));

                if (shipmentType.includeRAD() || shipmentType.includeEAD()) {
                    service.put("confirme_rad", shipmentType.includeEAD() ? 1 : 0);
                    service.put(STATUS, 1);
                }

                GenericQuery insert = this.generateGenericCreate("parcels_rad_ead", service);
                conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                    try{
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        future.complete(service);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else {
                future.complete(service);
            }
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Boolean> generateMovementPayback(SQLConnection conn, Parcel parcelObj, Double paybackPoints, Double paybackMoney) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonObject paramMovPayback = new JsonObject()
                    .put("customer_id", parcelObj.getSenderId())
                    .put("points", paybackPoints)
                    .put("money", paybackMoney)
                    .put("type_movement", "I")
                    .put("motive", "Envio de paquetería(sender)")
                    .put("id_parent", parcelObj.getId())
                    .put("employee_id", parcelObj.getCreatedBy());
            new PaybackDBV().generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                try {
                    if (errorMP != null) {
                        throw errorMP;
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertTickets(SQLConnection conn, Integer cashOutId, Parcel parcelObj, JsonArray parcelPackages, JsonArray parcelPackings, JsonArray payments, SHIPMENT_TYPE shipmentType, JsonObject cashChange, boolean isCredit, boolean isInternalParcel, Integer currencyId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            insertTicket(conn, cashOutId, parcelObj, cashChange, isCredit)
                    .whenComplete((ticketId, ticketError) -> {
                        try {
                            if (ticketError != null) {
                                throw ticketError;
                            }

                            this.insertTicketDetail(conn, ticketId, parcelObj, parcelPackages, parcelPackings, isInternalParcel, shipmentType).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                try {
                                    if (dError != null) {
                                        throw dError;
                                    }

                                    if (!isCredit) {
                                        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                        for (int i = 0; i < payments.size(); i++) {
                                            JsonObject payment = payments.getJsonObject(i);
                                            payment.put(TICKET_ID, ticketId);
                                            pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcelObj.getId(), cashOutId, parcelObj.getCreatedBy()));
                                        }
                                        CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pTasks.size()])).whenComplete((s, tt) -> {
                                            try {
                                                if (tt != null) {
                                                    throw tt;
                                                }

                                                future.complete(true);
                                            } catch (Throwable t) {
                                                future.completeExceptionally(t);
                                            }
                                        });
                                    } else {
                                        future.complete(true);
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
        return future;
    }

    private CompletableFuture<Integer> insertTicket(SQLConnection conn, Integer cashOutId, Parcel parcelObj, JsonObject cashChange, boolean isCredit) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();

            ticket.put(_PARCEL_ID, parcelObj.getId());
            ticket.put(CASHOUT_ID, cashOutId);
            ticket.put(_IVA, parcelObj.getIva());
            ticket.put(_PARCEL_IVA, parcelObj.getParcelIva());
            ticket.put(TOTAL, parcelObj.getTotalAmount());
            ticket.put(CREATED_BY, parcelObj.getCreatedBy());
            ticket.put("ticket_code", UtilsID.generateID("T"));
            String action = isCredit ? "voucher" : "purchase";
            ticket.put(_ACTION, action);

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble(TOTAL);
                double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                double differencePaid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if(!Objects.equals(action, "voucher")) {
                    if (parcelObj.getTotalAmount() < total) {
                        throw new Throwable("The payment " + total + " is greater than the total " + parcelObj.getTotalAmount());
                    } else if (parcelObj.getTotalAmount() > total) {
                        throw new Throwable("The payment " + total + " is lower than the total " + parcelObj.getTotalAmount());
                    } else if (paid_change > differencePaid) {
                        throw new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                    } else if (paid_change < differencePaid) {
                        throw new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                    }
                }
            } else {
                ticket.put("paid", parcelObj.getTotalAmount());
                ticket.put("paid_change", 0.0);
            }

            GenericQuery insert = this.generateGenericCreate("tickets", ticket);

            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if (reply.succeeded()) {
                        final int id = reply.result().getKeys().getInteger(0);
                        parcelObj.setTicketId(id);
                        future.complete(id);
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

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer ticketId, Parcel parcelObj, JsonArray parcelPackages, JsonArray parcelPackings, Boolean isInternalParcel, SHIPMENT_TYPE shipmentType) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            Map<String, List<JsonObject>> groupedPackages = parcelPackages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString(_NAME_PRICE)));
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
                    ticketDetail.put(TICKET_ID, ticketId);
                    ticketDetail.put(_QUANTITY, packQuantity);
                    ticketDetail.put(_DETAIL, "Envío de " + shippingType + " con rango " + packagePriceName);
                    ticketDetail.put(_UNIT_PRICE, packAmount);
                    ticketDetail.put(_DISCOUNT, packDiscount);
                    ticketDetail.put(_AMOUNT, packTotalAmount);
                    ticketDetail.put(CREATED_BY, parcelObj.getCreatedBy());
                    details.add(ticketDetail);
                }
            });

            for (int i = 0; i < parcelPackings.size(); i++) {
                JsonObject packing = parcelPackings.getJsonObject(i);
                JsonObject ticketDetail = new JsonObject();

                ticketDetail.put(TICKET_ID, ticketId);
                ticketDetail.put(_QUANTITY, packing.getInteger(_QUANTITY));
                ticketDetail.put(_DETAIL, "Embalaje");
                ticketDetail.put(_UNIT_PRICE, packing.getDouble(_UNIT_PRICE));
                ticketDetail.put(_AMOUNT, packing.getDouble(_AMOUNT));
                ticketDetail.put(CREATED_BY, parcelObj.getCreatedBy());

                details.add(ticketDetail);
            }

            if(parcelObj.getHasInsurance()){
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put(TICKET_ID, ticketId);
                ticketDetail.put(_QUANTITY, 1);
                ticketDetail.put(_DETAIL, "Seguro de envío");
                ticketDetail.put(_UNIT_PRICE, parcelObj.getInsuranceAmount());
                ticketDetail.put(_AMOUNT, parcelObj.getInsuranceAmount());
                ticketDetail.put(CREATED_BY, parcelObj.getCreatedBy());

                details.add(ticketDetail);
            }

            if(shipmentType.includeRAD() || shipmentType.includeEAD()){
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put(TICKET_ID, ticketId);
                ticketDetail.put(_QUANTITY, 1);
                ticketDetail.put(_DETAIL, "Servicio " + shipmentType.getValue());
                ticketDetail.put(_UNIT_PRICE, parcelObj.getServicesAmount());
                ticketDetail.put(_AMOUNT, parcelObj.getServicesAmount());
                ticketDetail.put(CREATED_BY, parcelObj.getCreatedBy());

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
