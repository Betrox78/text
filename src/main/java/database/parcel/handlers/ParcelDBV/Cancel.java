package database.parcel.handlers.ParcelDBV;

import database.boardingpass.BoardingPassDBV;
import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.money.PaymentDBV;
import database.parcel.ParcelDBV;
import database.parcel.ParcelsPackagesTrackingDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.routes.ScheduleRouteDBV;
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
import utils.UtilsValidation;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID;
import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static database.parcel.ParcelDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsDate.*;
import static utils.UtilsValidation.MISSING_REQUIRED_VALUE;

public class Cancel extends DBHandler<ParcelDBV> {

    public Cancel(ParcelDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        int parcelId = body.getInteger(PARCEL_ID);
        int cancelBy = body.getInteger(UPDATED_BY);
        Integer cashOutId = body.getInteger(CASHOUT_ID);
        int cancelReasonId = body.getInteger(PARCELS_CANCEL_REASON_ID);
        int currencyId = body.getInteger("currency_id");
        double ivaPercent = (Double) body.remove("iva_percent");
        Boolean applyInsurance = body.getBoolean("apply_insurance");
        String notes = null;
        String cancelCode = null;
        JsonArray payments = new JsonArray();
        JsonObject cashChange = new JsonObject();

        if(body.getString(CANCEL_CODE) != null){
            cancelCode = body.getString(CANCEL_CODE);
        }
        if(body.getString(NOTES) != null){
            notes = body.getString(NOTES);
        }
        if(body.getJsonArray("payments") != null && !body.getJsonArray("payments").isEmpty()){
            payments = body.getJsonArray("payments");
        }
        if(body.getJsonObject("cash_change") != null && !body.getJsonObject("cash_change").isEmpty()){
            cashChange = body.getJsonObject("cash_change");
        }

        JsonArray finalPayments = payments;
        JsonObject finalCashChange = cashChange;
        String finalNotes = notes;
        String finalCancelCode = cancelCode;

        getParcelDetail(parcelId).whenComplete((parcel, pError) -> {
            try {
                if (pError != null) {
                    throw pError;
                }

                checkCancelReason(cancelReasonId).whenComplete((resultCancelReason, errorCancelReason) -> {
                    try{
                        if (errorCancelReason != null){
                            throw errorCancelReason;
                        }
                        String cancelReasonResponsable = resultCancelReason.getString("responsable");
                        CANCEL_RESPONSABLE cancelResponsable = CANCEL_RESPONSABLE.valueOf(cancelReasonResponsable.toUpperCase());
                        String cancelReasonType = resultCancelReason.getString("cancel_type");
                        CANCEL_TYPE cancelType = CANCEL_TYPE.valueOf(cancelReasonType.toUpperCase());

                        switch (cancelType){
                            case FAST_CANCEL:
                                parcelFastCancel(message, cashOutId, cancelBy, finalNotes, parcel, finalCancelCode, currencyId, cancelReasonId, ivaPercent, cancelResponsable);
                                break;
                            case END_CANCEL:
                                parcelEndCancel(message, finalNotes, cancelBy, finalPayments, cashOutId, finalCashChange, finalCancelCode, applyInsurance, parcel, cancelReasonId);
                                break;
                            case REWORK:
                                throw new Exception("Rework not implemented");
//                                try {
//                                    UtilsValidation.isGrater(body, SCHEDULE_ROUTE_DESTINATION_ID, 0);
//                                    UtilsValidation.isGrater(body, REWORK_SCHEDULE_ROUTE_DESTINATION_ID, 0);
//                                    UtilsValidation.isBooleanAndNotNull(body, PAYS_SENDER);
//                                    UtilsValidation.isGrater(body, ADDRESSEE_ID, 0);
//                                    UtilsValidation.isEmpty(body, ADDRESSEE_NAME);
//                                    UtilsValidation.isEmpty(body, ADDRESSEE_LAST_NAME);
//                                    UtilsValidation.isPhoneNumber(body, ADDRESSEE_PHONE);
//                                    UtilsValidation.isMail(body, ADDRESSEE_EMAIL);
//                                    UtilsValidation.isGrater(body, ADDRESSEE_ADDRESS_ID, 0);
//                                    parcel.put("payment_condition", "cash");
//                                    parcel.put("debt", 0);
//                                    this.parcelReworkCancel(conn, message, cancelResponsable, parcel, body, finalNotes, cancelBy, finalCancelCode, finalPayments, finalCashChange, cancelReasonId);
//                                } catch (UtilsValidation.PropertyValueException e){
//                                    this.rollback(conn, e, message);
//                                }
//                                break;
                            case RETURN:
                                throw new Exception("Return not implemented");
//                                try {
//                                    UtilsValidation.isGrater(body, SCHEDULE_ROUTE_DESTINATION_ID, 0);
//                                    UtilsValidation.isBooleanAndNotNull(body, PAYS_SENDER);
//                                    parcel.put("payment_condition", "cash");
//                                    parcel.put("debt", 0);
//                                    this.parcelReturnCancel(conn, message, cancelReasonResponsable, parcel, body, finalNotes, cancelBy, finalCancelCode, finalPayments, finalCashChange, cancelReasonId);
//                                } catch (UtilsValidation.PropertyValueException e){
//                                    this.rollback(conn, e, message);
//                                }
//                                break;
                        }
                    } catch (Throwable t){
                        reportQueryError(message, t);
                    }
                });

            } catch (Throwable t) {
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<JsonObject> getParcelDetail(Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PARCEL_INFO, new JsonArray().add(parcelId), resultHandler-> {
            try {
                if (resultHandler.failed()) {
                    throw new Exception(resultHandler.cause());
                }

                List<JsonObject> resultParcel = resultHandler.result().getRows();
                if (resultParcel.isEmpty()) {
                    throw new Exception("Parcel wasn't found");
                }

                JsonObject parcel = resultParcel.get(0);
                Integer canceledBy = parcel.getInteger("canceled_by");
                if (Objects.nonNull(canceledBy)) {
                    throw new Exception("Parcel was canceled");
                }
                Integer invoiceId = parcel.getInteger("invoice_id");
                if (Objects.nonNull(invoiceId)) {
                    throw new Exception("Parcel was invoiced");
                }

                getParcelPackagesDetail(parcelId).whenComplete((packages, errPackages) -> {
                    try {
                        if (errPackages != null) {
                            throw errPackages;
                        }

                        future.complete(parcel);
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

    private CompletableFuture<List<JsonObject>> getParcelPackagesDetail(Integer parcelId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PARCEL_PACKAGE_BY_PARCEL_ID, new JsonArray().add(parcelId), resultHandler-> {
            try {
                if (resultHandler.failed()) {
                    throw new Exception(resultHandler.cause());
                }

                List<JsonObject> packages = resultHandler.result().getRows();
                if (packages.isEmpty()) {
                    future.complete(packages);
                    return;
                }

                List<JsonObject> packagesLoaded = packages.stream()
                        .filter(p -> {
                            PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[p.getInteger(_PACKAGE_STATUS)];
                            return packageStatus.equals(PACKAGE_STATUS.LOADED);
                        })
                        .collect(Collectors.toList());

                if (!packagesLoaded.isEmpty()) {
                    String travelLogCode = packagesLoaded.get(0).getString("travel_log_code");
                    throw new Exception("Cannot be canceled, it is on load on " + travelLogCode);
                }

                future.complete(packages);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> checkCancelReason(Integer cancelReasonId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams("SELECT id, responsable, cancel_type FROM parcels_cancel_reasons WHERE id = ?", new JsonArray().add(cancelReasonId), reply -> {
            try{
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()){
                    throw new Exception("The parcels_cancel_reason_id not exists in the catalog");
                }

                JsonObject cancelReason = result.get(0);
                future.complete(cancelReason);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void parcelFastCancel(Message<JsonObject> message, Integer cashOutId,
                                  Integer cancelBy, String finalNotes, JsonObject parcel, String cancelCode, Integer currencyId, Integer cancelReasonId, double ivaPercent, CANCEL_RESPONSABLE cancelResponsable){
        try {
            Integer parcelId = parcel.getInteger("id");
            PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            Boolean paysSender = parcel.getBoolean("pays_sender");
            boolean isCredit = parcel.getString("payment_condition").equals("credit");

            if (!parcelStatus.equals(PARCEL_STATUS.DOCUMENTED)
                    && !parcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)
                    && !parcelStatus.equals(PARCEL_STATUS.ARRIVED)
                    && !parcelStatus.equals(PARCEL_STATUS.ARRIVED_INCOMPLETE)
                    && !parcelStatus.equals(PARCEL_STATUS.LOCATED)
                    && !parcelStatus.equals(PARCEL_STATUS.LOCATED_INCOMPLETE)){
                throw new Exception("Parcel was delivered or canceled");
            }

            String createdAt = format_yyyy_MM_dd(getDateConvertedTimeZone(timezone, parse_yyyy_MM_dd_T_HH_mm_ss(parcel.getString(CREATED_AT))));
            String today = format_yyyy_MM_dd(getDateConvertedTimeZone(timezone, new Date()));
            if(!createdAt.equals(today)) {
                throw new Throwable("Parcel can't cancel");
            }

            checkParcelPrepaid(parcelId).whenComplete((parcelPrepaids, errPP) -> {
               try {
                   if (errPP != null) {
                       throw errPP;
                   }

                   startTransaction(message, conn -> {
                       try {
                           remakeParcelPrepaid(conn, parcelPrepaids, cancelResponsable, cancelBy).whenComplete((newGuides, errRPP) -> {
                               try {
                                   if (errRPP != null) {
                                       throw errRPP;
                                   }
                                   if(paysSender && !isCredit){
                                       this.dbClient.queryWithParams("SELECT total_amount FROM parcels WHERE id = ?;", new JsonArray().add(parcelId), replyTAParcel -> {
                                           try{
                                               if(replyTAParcel.succeeded()){
                                                   Double amountToReturn = replyTAParcel.result().getRows().get(0).getDouble("total_amount");
                                                   Integer senderId = parcel.getInteger("sender_id");
                                                   this.returnMoney(conn, cashOutId, currencyId,
                                                           "Devolución por cancelación de paquetería", "parcel_id", parcelId, amountToReturn,
                                                           cancelBy, false, senderId, ivaPercent, newGuides)
                                                           .whenComplete((resultReturnMoney, errorReturnMoney) -> {
                                                       try {
                                                           if (errorReturnMoney != null){
                                                               throw errorReturnMoney;
                                                           }
                                                           this.setParcelStatus(conn, parcelId,
                                                                           PARCEL_STATUS.CANCELED.ordinal(), PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                                                           ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), resultReturnMoney, cancelBy, cancelCode, cancelReasonId, false)
                                                                   .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                                                       try{
                                                                           if (errorParcelStatus != null) {
                                                                               throw errorParcelStatus;
                                                                           }
                                                                           this.setRadEadStatus(conn, parcelId).whenComplete((resultRadEadCancel , errorResultRadEadCancel) -> {
                                                                               try{
                                                                                   if(errorResultRadEadCancel != null){
                                                                                       throw errorResultRadEadCancel;
                                                                                   }
                                                                                   if(!newGuides.isEmpty()) {
                                                                                       resultParcelStatus.put("new_guides", newGuides);
                                                                                   }
                                                                                   this.commit(conn, message, resultParcelStatus);
                                                                               } catch (Throwable t){
                                                                                   this.rollback(conn, t, message);
                                                                               }
                                                                           });
                                                                       } catch (Throwable t){
                                                                           this.rollback(conn, t, message);
                                                                       }
                                                                   });
                                                       } catch (Throwable t){
                                                           this.rollback(conn, t, message);
                                                       }
                                                   });
                                               } else {
                                                   this.rollback(conn, replyTAParcel.cause(), message);
                                               }
                                           } catch (Exception e){
                                               this.rollback(conn, e, message);
                                           }
                                       });
                                   } else {
                                       this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                                       PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                                       ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                               .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                                   try{
                                                       if (errorParcelStatus != null) {
                                                           this.rollback(conn, errorParcelStatus, message);
                                                       } else {
                                                           this.insertTicket(conn, null, parcelId, 0.00, null, cancelBy, ivaPercent,  "voucher").whenComplete((JsonObject ticketV, Throwable ticketErrorV) -> {
                                                               try{
                                                                   if (ticketErrorV != null) {
                                                                       this.rollback(conn, ticketErrorV, message);
                                                                   } else {
                                                                       JsonObject ticketDetail = new JsonObject();
                                                                       ticketDetail.put("ticket_id", ticketV.getInteger("id"));
                                                                       ticketDetail.put("quantity", 1);
                                                                       StringBuilder detail = new StringBuilder("Comprobante de cancelación de paquetería");
                                                                       if (!newGuides.isEmpty()) {
                                                                           detail.append(", nuevas guías generadas: ").append(newGuides);
                                                                       }
                                                                       ticketDetail.put("detail", detail);
                                                                       ticketDetail.put("unit_price", 0.00);
                                                                       ticketDetail.put("amount", 0.00);
                                                                       ticketDetail.put("created_by", cancelBy);

                                                                       GenericQuery insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);

                                                                       conn.updateWithParams(insertTicketDetail.getQuery(), insertTicketDetail.getParams(), replyInsertTicketDetail -> {
                                                                           try {
                                                                               if (replyInsertTicketDetail.succeeded()){
                                                                                   if(isCredit){
                                                                                       DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                                                                       JsonObject paramsCredit = new JsonObject().put(Constants.CUSTOMER_ID, parcel.getInteger(Constants.CUSTOMER_ID));
                                                                                       getVertx().eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                                                                           try{
                                                                                               if(replyCredit.failed()) {
                                                                                                   throw new Exception(replyCredit.cause());
                                                                                               }
                                                                                               Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                                                                               JsonObject customerCreditData = customerCreditDataMsg.body();

                                                                                               this.updateCustomerCredit(conn, customerCreditData, parcel.getDouble("debt"), cancelBy)
                                                                                                       .whenComplete((replyCustomer, errorCustomer) -> {
                                                                                                           try{
                                                                                                               if (errorCustomer != null) {
                                                                                                                   throw new Exception(errorCustomer);
                                                                                                               }

                                                                                                               resultParcelStatus.put("voucher_ticket_id", ticketV.getInteger("id"));
                                                                                                               if(!newGuides.isEmpty()) {
                                                                                                                   resultParcelStatus.put("new_guides", newGuides);
                                                                                                               }
                                                                                                               this.commit(conn, message, resultParcelStatus);

                                                                                                           } catch (Throwable t) {
                                                                                                               this.rollback(conn, t, message);
                                                                                                           }
                                                                                                       });

                                                                                           } catch (Exception e) {
                                                                                               this.rollback(conn, e, message);
                                                                                           }
                                                                                       });
                                                                                   } else {
                                                                                       resultParcelStatus.put("voucher_ticket_id", ticketV.getInteger("id"));
                                                                                       if(!newGuides.isEmpty()) {
                                                                                           resultParcelStatus.put("new_guides", newGuides);
                                                                                       }
                                                                                       this.commit(conn, message, resultParcelStatus);
                                                                                   }
                                                                               } else {
                                                                                   this.rollback(conn, replyInsertTicketDetail.cause(), message);
                                                                               }
                                                                           } catch (Exception e){
                                                                               this.rollback(conn, e, message);
                                                                           }
                                                                       });
                                                                   }
                                                               } catch (Exception e){
                                                                   this.rollback(conn, e, message);
                                                               }
                                                           });
                                                       }
                                                   } catch (Exception e){
                                                       this.rollback(conn, e, message);
                                                   }
                                               });
                                   }
                               } catch (Throwable t) {
                                   this.rollback(conn, t, message);
                               }
                           });
                       } catch (Throwable t) {
                           this.rollback(conn, t, message);
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

    private CompletableFuture<List<JsonObject>> checkParcelPrepaid(Integer parcelId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_GET_PARCEL_PREPAID_DETAILS, new JsonArray().add(parcelId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    future.complete(result);

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<String> remakeParcelPrepaid(SQLConnection conn, List<JsonObject> parcelPrepaids, CANCEL_RESPONSABLE cancelResponsable, Integer userId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            if (parcelPrepaids.isEmpty() || !cancelResponsable.equals(CANCEL_RESPONSABLE.COMPANY)) {
                future.complete("");
            } else {
                List<GenericQuery> newGuides = new ArrayList<>();
                String newGuiappCodes = "";
                for (JsonObject parcelPrepaid : parcelPrepaids) {
                    boolean isLast = (parcelPrepaids.lastIndexOf(parcelPrepaid) + 1) == parcelPrepaids.size();
                    String newGuiappCode = UtilsID.generateGuiaPpID("GPPR");
                    newGuiappCodes = newGuiappCodes.concat(newGuiappCode).concat(isLast ? "" : ", ");
                    parcelPrepaid.put("guiapp_code", newGuiappCode)
                            .put(_PARCEL_STATUS, 0)
                            .put(CREATED_AT, UtilsDate.sdfDataBase(new Date()))
                            .put("crated_by", userId);
                    parcelPrepaid.remove(ID);
                    parcelPrepaid.remove(_PARCEL_ID);
                    parcelPrepaid.remove(UPDATED_BY);
                    parcelPrepaid.remove(UPDATED_AT);
                    newGuides.add(this.generateGenericCreate("parcels_prepaid_detail", parcelPrepaid));
                }
                List<JsonArray> params = newGuides.stream().map(GenericQuery::getParams).collect(Collectors.toList());

                String finalNewGuiappCodes = newGuiappCodes;
                conn.batchWithParams(newGuides.get(0).getQuery(), params, reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        future.complete(finalNewGuiappCodes);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void parcelEndCancel(Message<JsonObject> message, String finalNotes, Integer cancelBy, JsonArray finalPayments,
                                 Integer cashOutId, JsonObject finalCashChange, String cancelCode, Boolean applyInsurance, JsonObject parcel, Integer cancelReasonId){
        try {
            PARCEL_STATUS currentParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            if(currentParcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)) {
                throw new Throwable("Parcel is in transit");
            }

            Integer parcelId = parcel.getInteger("id");
            Boolean paysSender = parcel.getBoolean("pays_sender");
            Boolean hasInsurance = parcel.getBoolean("has_insurance");
            Double totalAmount = parcel.getDouble("total_amount");
            Integer customerId = parcel.getInteger(BoardingPassDBV.CUSTOMER_ID);
            boolean isCredit = parcel.getString("payment_condition").equals("credit");
            startTransaction(message, conn -> {
                try {
                    if(paysSender){
                        int parcelStatus = PARCEL_STATUS.DELIVERED_OK.ordinal();
                        int packageStatus = PACKAGE_STATUS.DELIVERED.ordinal();
                        String packageTracking = ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase();
                        if(isCredit) {
                            parcelStatus = PARCEL_STATUS.CANCELED.ordinal();
                            packageStatus = PACKAGE_STATUS.CANCELED.ordinal();
                            packageTracking = ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase();
                        }
                        this.setParcelStatus(conn, parcelId, parcelStatus,
                                        packageStatus, finalNotes, packageTracking, null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } else {
                        if(finalPayments.isEmpty()){
                            this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                            PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                            ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                    .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                        try{
                                            if (errorParcelStatus != null) {
                                                throw new Exception(errorParcelStatus);
                                            }

                                            this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                        } catch (Exception e){
                                            this.rollback(conn, e, message);
                                        }
                                    });
                        } else {
                            if (hasInsurance){
                                if (applyInsurance){
                                    //REGISTRA PAGO
                                    this.dbClient.queryWithParams("SELECT id FROM parcels_packages WHERE parcel_id = ?", new JsonArray().add(parcelId), replyGetPackages -> {
                                        try{
                                            if (replyGetPackages.succeeded()) {
                                                List<JsonObject> packages = replyGetPackages.result().getRows();
                                                this.paymentInsert(conn, finalPayments, parcel, new JsonArray(packages), totalAmount, cashOutId, finalCashChange, cancelBy).whenComplete((resultPaymentInsert, errorPaymentInsert) -> {
                                                    try {
                                                        if(errorPaymentInsert != null){
                                                            this.rollback(conn, errorPaymentInsert, message);
                                                        } else {
                                                            this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                                                            PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                                                            ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), resultPaymentInsert, cancelBy, cancelCode, cancelReasonId, isCredit)
                                                                    .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                                                        try{
                                                                            if (errorParcelStatus != null) {
                                                                                this.rollback(conn, errorParcelStatus, message);
                                                                            } else {
                                                                                this.commit(conn, message, resultParcelStatus);
                                                                            }
                                                                        } catch (Exception e){
                                                                            this.rollback(conn, e, message);
                                                                        }
                                                                    });
                                                        }
                                                    } catch (Exception e){
                                                        this.rollback(conn, e, message);
                                                    }
                                                });
                                            } else {
                                                this.rollback(conn, replyGetPackages.cause(), message);
                                            }
                                        } catch (Exception e){
                                            this.rollback(conn, e, message);
                                        }
                                    });
                                } else {
                                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                                    PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                                    ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                                try{
                                                    if (errorParcelStatus != null) {
                                                        throw new Exception(errorParcelStatus);
                                                    }

                                                    this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                                } catch (Exception e){
                                                    this.rollback(conn, e, message);
                                                }
                                            });
                                }
                            } else {
                                this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                                PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                                ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                        .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                            try{
                                                if (errorParcelStatus != null) {
                                                    throw new Exception(errorParcelStatus);
                                                }

                                                this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                            } catch (Exception e){
                                                this.rollback(conn, e, message);
                                            }
                                        });
                            }
                        }
                    }
                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void parcelReworkCancel(SQLConnection conn, Message<JsonObject> message, CANCEL_RESPONSABLE cancelResponsable, JsonObject parcel,
                                    JsonObject cancelBody, String finalNotes, Integer cancelBy, String cancelCode, JsonArray payments, JsonObject cashChange, Integer cancelReasonId){
        try {
            PARCEL_STATUS currentParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            if (!currentParcelStatus.equals(PARCEL_STATUS.DOCUMENTED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED_INCOMPLETE)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED_INCOMPLETE)){
                throw new Exception("Parcel was delivered or canceled");
            }

            Boolean paysSender = parcel.getBoolean(PAYS_SENDER);
            Integer parcelId = parcel.getInteger(ID);
            Integer scheduleRouteDestinationId = cancelBody.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            Integer reworkScheduleRouteDestinationId = cancelBody.getInteger(REWORK_SCHEDULE_ROUTE_DESTINATION_ID);
            parcel.put(PAYS_SENDER, cancelBody.getBoolean(PAYS_SENDER))
                    .put(REISSUE, true)
                    .put(REWORK, true)
                    .put(REWORK_SCHEDULE_ROUTE_DESTINATION_ID, reworkScheduleRouteDestinationId)
                    .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId)
                    .put("credit_customer_id", paysSender ? parcel.getInteger(BoardingPassDBV.CUSTOMER_ID): parcel.getInteger(ADDRESSEE_ID));

            if (cancelResponsable.equals(CANCEL_RESPONSABLE.CUSTOMER)){
                try {
                    if (payments == null || payments.isEmpty()){
                        throw new UtilsValidation.PropertyValueException("payments", MISSING_REQUIRED_VALUE);
                    }
                    if (cashChange == null){
                        throw new UtilsValidation.PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
                    }
                    parcel.put(WITHOUT_FREIGHT, false);
                    if (paysSender){
                        parcel.put(CUMULATIVE_COST, false);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                        PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } else {
                        parcel.put(CUMULATIVE_COST, true);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode,cancelReasonId, false)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    }
                } catch (UtilsValidation.PropertyValueException ex){
                    this.rollback(conn, ex, message);
                }
            } else if (cancelResponsable.equals(CANCEL_RESPONSABLE.COMPANY)){
                parcel.put(CUMULATIVE_COST, false);
                if (paysSender){
                    parcel.put(WITHOUT_FREIGHT, true);
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                    PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    parcel.mergeIn(resultParcelStatus);

                                    this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                        try {
                                            if (errorParcelRegisterObject != null){
                                                throw errorParcelRegisterObject;
                                            }

                                            this.commit(conn, message, parcelRegisterObject);

                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });

                                } catch (Exception e){
                                    this.rollback(conn, e, message);
                                }
                            });
                } else {
                    parcel.put(WITHOUT_FREIGHT, false);
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                    PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    parcel.mergeIn(resultParcelStatus);

                                    this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                        try {
                                            if (errorParcelRegisterObject != null){
                                                throw errorParcelRegisterObject;
                                            }

                                            this.commit(conn, message, parcelRegisterObject);

                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });

                                } catch (Exception e){
                                    this.rollback(conn, e, message);
                                }
                            });
                }
            } else {
                throw new Throwable("Parcel can't cancel");
            }
        } catch (Throwable t) {
            this.rollback(conn, t, message);
        }
    }

    private void parcelReturnCancel(SQLConnection conn, Message<JsonObject> message, String cancelReasonResponsable, JsonObject parcel,
                                    JsonObject cancelBody, String finalNotes, Integer cancelBy, String cancelCode, JsonArray payments, JsonObject cashChange, Integer cancelReasonId){
        try {
            PARCEL_STATUS currentParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            if (!currentParcelStatus.equals(PARCEL_STATUS.DOCUMENTED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED_INCOMPLETE)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED_INCOMPLETE)){
                throw new Exception("Parcel was delivered or canceled");
            }

            parcel.put(REISSUE, true);
            Boolean paysSender = parcel.getBoolean(PAYS_SENDER);
            parcel.put(PAYS_SENDER, cancelBody.getBoolean(PAYS_SENDER));
            Integer parcelId = parcel.getInteger(ID);
            Integer scheduleRouteDestinationId = cancelBody.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            parcel.put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId)
                    .put("credit_customer_id", paysSender ? parcel.getInteger(BoardingPassDBV.CUSTOMER_ID): parcel.getInteger(ADDRESSEE_ID));

            this.swapResponsable(parcel);

            if (cancelReasonResponsable.equalsIgnoreCase(CANCEL_RESPONSABLE.CUSTOMER.name())){
                try {
                    if (payments == null || payments.isEmpty()){
                        throw new UtilsValidation.PropertyValueException("payments", MISSING_REQUIRED_VALUE);
                    }
                    if (cashChange == null){
                        throw new UtilsValidation.PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
                    }
                    parcel.put(WITHOUT_FREIGHT, false);
                    if (paysSender){
                        parcel.put(CUMULATIVE_COST, false);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                        PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } else {
                        parcel.put(CUMULATIVE_COST, true);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    }
                } catch (UtilsValidation.PropertyValueException ex){
                    this.rollback(conn, ex, message);
                }
            } else if (cancelReasonResponsable.equalsIgnoreCase(CANCEL_RESPONSABLE.COMPANY.name())){
                if (paysSender){
                    parcel.put(CUMULATIVE_COST, false);
                    parcel.put(WITHOUT_FREIGHT, true);
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                    PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    parcel.mergeIn(resultParcelStatus);

                                    this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                        try {
                                            if (errorParcelRegisterObject != null){
                                                throw errorParcelRegisterObject;
                                            }

                                            this.commit(conn, message, parcelRegisterObject);

                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });

                                } catch (Exception e){
                                    this.rollback(conn, e, message);
                                }
                            });
                } else {
                    try {
                        if (payments == null || payments.isEmpty()){
                            throw new UtilsValidation.PropertyValueException("payments", MISSING_REQUIRED_VALUE);
                        }
                        if (cashChange == null){
                            throw new UtilsValidation.PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
                        }
                        parcel.put(CUMULATIVE_COST, false);
                        parcel.put(WITHOUT_FREIGHT, false);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, false)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } catch (UtilsValidation.PropertyValueException ex){
                        this.rollback(conn, ex, message);
                    }
                }
            } else {
                this.rollback(conn, new Throwable("Parcel can't cancel"), message);
            }
        } catch (Throwable t) {
            this.rollback(conn, t, message);
        }
    }

    protected CompletableFuture<Integer> returnMoney(SQLConnection conn, Integer cashOutId, Integer currencyId, String expenceConcept, String referenceField, Integer referenceId,
                                                     Double amountToReturn, Integer createdBy, boolean isCredit, Integer customerId, double ivaPercent, String newGuides) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Future f1 = Future.future();
        Future f2 = Future.future();

        conn.queryWithParams(QUERY_EXPENSE_CONCEPT_RETURN, new JsonArray().add(expenceConcept), f1.completer());

        conn.query("SELECT id FROM payment_method WHERE is_cash = 1 AND status = 1 LIMIT 1;", f2.completer());

        CompositeFuture.all(f1, f2).setHandler(r -> {
            try {
                if (r.failed()) {
                    throw new Exception(r.cause());
                }
                List<JsonObject> expenses = r.result().<ResultSet>resultAt(0).getRows();
                Integer paymentMethodId = r.result().<ResultSet>resultAt(1).getRows().get(0).getInteger("id");
                Integer expenseConceptId = null;
                if (!expenses.isEmpty()) {
                    expenseConceptId = expenses.get(0).getInteger("id");
                }
                JsonObject expense = new JsonObject()
                        .put(referenceField, referenceId)
                        .put("payment_method_id", paymentMethodId)
                        .put("amount", amountToReturn)
                        .put("reference", expenceConcept)
                        .put("currency_id", currencyId)
                        .put("created_by", createdBy)
                        .put("expense_concept_id", expenseConceptId)
                        .put("description", expenceConcept);

                JsonObject cashChange = new JsonObject().put("paid", amountToReturn).put("total", amountToReturn).put("paid_change", 0.0);

                this.insertTicket(conn, cashOutId, referenceId, amountToReturn, cashChange, createdBy, ivaPercent, "cancel")
                        .whenComplete((JsonObject ticket, Throwable ticketError) -> {
                            try {
                                if (ticketError != null) {
                                    throw new Exception(ticketError);
                                }

                                JsonObject ticketDetail = new JsonObject();
                                ticketDetail.put("ticket_id", ticket.getInteger("id"));
                                ticketDetail.put("quantity", 1);
                                StringBuilder detail = new StringBuilder("Comprobante de cancelación de paquetería");
                                if (!newGuides.isEmpty()) {
                                    detail.append(", nuevas guías generadas: ").append(newGuides);
                                }
                                ticketDetail.put("detail", detail);
                                ticketDetail.put("unit_price", amountToReturn);
                                ticketDetail.put("amount", amountToReturn);
                                ticketDetail.put("created_by", createdBy);

                                GenericQuery insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);
                                conn.updateWithParams(insertTicketDetail.getQuery(), insertTicketDetail.getParams(), replyInsertTicketDetail -> {
                                    try {
                                        if (replyInsertTicketDetail.failed()) {
                                            throw new Exception(replyInsertTicketDetail.cause());
                                        }

                                        if (isCredit) {
                                            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                            JsonObject paramsCredit = new JsonObject().put("customer_id", customerId);
                                            getVertx().eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                                try {
                                                    if (replyCredit.failed()) {
                                                        throw new Exception(replyCredit.cause());
                                                    }
                                                    Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                                    JsonObject customerCreditData = customerCreditDataMsg.body();
                                                    this.updateCustomerCredit(conn, customerCreditData, amountToReturn, createdBy)
                                                            .whenComplete((replyCustomer, errorCustomer) -> {
                                                                try{
                                                                    if (errorCustomer != null) {
                                                                        throw new Exception(errorCustomer);
                                                                    }

                                                                    future.complete(ticket.getInteger("id"));

                                                                } catch (Throwable t) {
                                                                    future.completeExceptionally(t);
                                                                }
                                                            });

                                                } catch (Exception e) {
                                                    future.completeExceptionally(e);
                                                }
                                            });

                                        } else {
                                            expense.put("ticket_id", ticket.getInteger("id"));
                                            this.registerExpense(conn, expense, cashOutId, createdBy).whenComplete((JsonObject expenseReturn, Throwable expenseError) -> {
                                                try {
                                                    if (expenseError != null) {
                                                        throw new Exception(expenseError);
                                                    }


                                                    //TODO: registrar cashout move

                                                    future.complete(ticket.getInteger("id"));

                                                } catch (Exception e) {
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        }

                                    } catch (Exception e) {
                                        future.completeExceptionally(e);
                                    }
                                });
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Integer parcelId, Double totalPayments, JsonObject cashChange, Integer createdBy, Double ivaPercent, String action) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            Double iva = this.getIva(totalPayments, ivaPercent);

            // Create ticket_code
            // ticket.put("ticket_code", ticketCode);
            ticket.put("parcel_id", parcelId);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
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
                double difference_paid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if(!Objects.equals(action, "voucher")) {
                    if (totalPayments < total) {
                        throw new Throwable("The payment " + total + " is greater than the total " + totalPayments);
                    } else if (totalPayments > total) {
                        throw new Throwable("The payment " + total + " is lower than the total " + totalPayments);
                    } else if (paid_change > difference_paid) {
                        throw new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                    } else if (paid_change < difference_paid) {
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

    private Double getIva(Double amount, Double ivaPercent){
        return amount - (amount / (1 + (ivaPercent/100)));
    }

    private CompletableFuture<JsonObject> setParcelStatus(SQLConnection conn, Integer parcelId, Integer parcelStatus, Integer packageStatus, String notes, String trackingAction, Integer ticketId, Integer updatedBy, String cancelCode, Integer cancelReasonId, boolean isCredit){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject parcel = new JsonObject()
                .put(ID, parcelId)
                .put(_PARCEL_STATUS, parcelStatus)
                .put(CANCELED_AT, sdfDataBase(new Date()))
                .put(CANCELED_BY, updatedBy);

        if(parcelStatus.equals(PARCEL_STATUS.DELIVERED_OK.ordinal())){
            parcel.put(DELIVERED_AT, sdfDataBase(new Date()));
        }

        if (cancelCode != null){
            parcel.put(CANCEL_CODE, cancelCode);
        }
        if(cancelReasonId != null ){
            parcel.put(PARCELS_CANCEL_REASON_ID, cancelReasonId);
        }
        if (notes != null){
            parcel.put(NOTES, notes);
        }
        if(cancelReasonId != null){
            parcel.put("parcels_cancel_reason_id", cancelReasonId);
        }
        if(isCredit){
            parcel.put("debt", 0.00);
        }

        GenericQuery updateParcel = this.generateGenericUpdate("parcels", parcel);
        conn.queryWithParams("select id from parcels_prepaid_detail where parcel_status!=4 and parcel_id=?", new JsonArray().add(parcelId), replyGetPrepaid -> {
            try {
                if (replyGetPrepaid.succeeded()){
                    List<JsonObject> guiapp_prepaid_id = replyGetPrepaid.result().getRows();
                    if(!guiapp_prepaid_id.isEmpty()){
                        String query_status_cancel_prepaid="update  parcels_prepaid_detail set parcel_status=4 WHERE parcel_status!=4 AND id="+guiapp_prepaid_id.get(0).getInteger("id");
                        conn.update(query_status_cancel_prepaid, replyPrepaid -> {
                            try {
                                if (replyPrepaid.succeeded()){
                                    conn.queryWithParams("SELECT id FROM parcels_packages WHERE parcel_id = ?", new JsonArray().add(parcelId), replyGetPackages -> {
                                        try {
                                            if (replyGetPackages.succeeded()){
                                                List<JsonObject> packages = replyGetPackages.result().getRows();
                                                //if (!packages.isEmpty()){
                                                List<GenericQuery> updatePackages = new ArrayList<>();
                                                for (JsonObject pack : packages) {
                                                    pack.put(PARCEL_ID, parcelId);
                                                    pack.put("package_status", packageStatus);
                                                    if (notes != null) {
                                                        pack.put(NOTES, notes);
                                                    }
                                                    updatePackages.add(this.generateGenericUpdate("parcels_packages", pack));
                                                }

                                                conn.updateWithParams(updateParcel.getQuery(), updateParcel.getParams(), replyParcel -> {
                                                    try {
                                                        if (replyParcel.succeeded()){
                                                            dbVerticle.insertTracking(conn, new JsonArray().add(parcel),"parcels_packages_tracking", null, "parcel_id", null, trackingAction, updatedBy)
                                                                    .whenComplete((resultTrackingParcel, errorTrackingParcel) -> {
                                                                        try {
                                                                            if (errorTrackingParcel != null){
                                                                                throw errorTrackingParcel;
                                                                            }
                                                                            List<JsonArray> updatePackagesParams = updatePackages.stream()
                                                                                    .map(GenericQuery::getParams).collect(Collectors.toList());
                                                                            conn.batchWithParams(updatePackages.get(0).getQuery(), updatePackagesParams, updatePackagesReply -> {
                                                                                try {
                                                                                    if (updatePackagesReply.succeeded()) {
                                                                                        dbVerticle.insertTracking(conn, new JsonArray(packages), "parcels_packages_tracking", "parcel_id", "parcel_package_id", null, trackingAction, updatedBy)
                                                                                                .whenComplete((resultTracking, errorTrackingPackages) -> {
                                                                                                    try {
                                                                                                        if(errorTrackingPackages != null){
                                                                                                            future.completeExceptionally(errorTrackingPackages);
                                                                                                        } else {
                                                                                                            JsonObject result = new JsonObject()
                                                                                                                    .put("updated", true);
                                                                                                            if (ticketId != null){
                                                                                                                result.put("cancel_ticket_id", ticketId);
                                                                                                            }
                                                                                                            future.complete(result);
                                                                                                        }
                                                                                                    } catch (Exception e){
                                                                                                        future.completeExceptionally(e);
                                                                                                    }
                                                                                                });
                                                                                    } else {
                                                                                        future.completeExceptionally(updatePackagesReply.cause());
                                                                                    }
                                                                                } catch (Exception e){
                                                                                    future.completeExceptionally(e);
                                                                                }
                                                                            });
                                                                        } catch (Throwable t){
                                                                            future.completeExceptionally(t);
                                                                        }
                                                                    });
                                                        } else {
                                                            future.completeExceptionally(replyParcel.cause());
                                                        }
                                                    } catch (Exception e) {
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                                //} // TODO : agregar else
                                            } else {
                                                future.completeExceptionally(replyGetPackages.cause());
                                            }
                                        } catch (Exception e){
                                            future.completeExceptionally(e);
                                        }
                                    });
                                } else {
                                    future.completeExceptionally(replyPrepaid.cause());
                                }
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
                    }else{
                        conn.queryWithParams("SELECT id FROM parcels_packages WHERE parcel_id = ?", new JsonArray().add(parcelId), replyGetPackages -> {
                            try {
                                if (replyGetPackages.succeeded()){
                                    List<JsonObject> packages = replyGetPackages.result().getRows();
                                    //if (!packages.isEmpty()){
                                    List<GenericQuery> updatePackages = new ArrayList<>();
                                    for (JsonObject pack : packages) {
                                        pack.put(PARCEL_ID, parcelId);
                                        pack.put("package_status", packageStatus);
                                        if (notes != null) {
                                            pack.put(NOTES, notes);
                                        }
                                        updatePackages.add(this.generateGenericUpdate("parcels_packages", pack));
                                    }

                                    conn.updateWithParams(updateParcel.getQuery(), updateParcel.getParams(), replyParcel -> {
                                        try {
                                            if (replyParcel.succeeded()){
                                                dbVerticle.insertTracking(conn, new JsonArray().add(parcel),"parcels_packages_tracking", null, "parcel_id", null, trackingAction, updatedBy)
                                                        .whenComplete((resultTrackingParcel, errorTrackingParcel) -> {
                                                            try {
                                                                if (errorTrackingParcel != null){
                                                                    future.completeExceptionally(errorTrackingParcel);
                                                                } else {
                                                                    List<JsonArray> updatePackagesParams = updatePackages.stream()
                                                                            .map(GenericQuery::getParams).collect(Collectors.toList());
                                                                    conn.batchWithParams(updatePackages.get(0).getQuery(), updatePackagesParams, updatePackagesReply -> {
                                                                        try {
                                                                            if (updatePackagesReply.succeeded()) {
                                                                                dbVerticle.insertTracking(conn, new JsonArray(packages), "parcels_packages_tracking", "parcel_id", "parcel_package_id", null, trackingAction, updatedBy)
                                                                                        .whenComplete((resultTracking, errorTrackingPackages) -> {
                                                                                            try {
                                                                                                if(errorTrackingPackages != null){
                                                                                                    future.completeExceptionally(errorTrackingPackages);
                                                                                                } else {
                                                                                                    JsonObject result = new JsonObject()
                                                                                                            .put("updated", true);
                                                                                                    if (ticketId != null){
                                                                                                        result.put("cancel_ticket_id", ticketId);
                                                                                                    }
                                                                                                    future.complete(result);
                                                                                                }
                                                                                            } catch (Exception e){
                                                                                                future.completeExceptionally(e);
                                                                                            }
                                                                                        });
                                                                            } else {
                                                                                future.completeExceptionally(updatePackagesReply.cause());
                                                                            }
                                                                        } catch (Exception e){
                                                                            future.completeExceptionally(e);
                                                                        }
                                                                    });
                                                                }
                                                            } catch (Exception e){
                                                                future.completeExceptionally(e);
                                                            }
                                                        });
                                            } else {
                                                future.completeExceptionally(replyParcel.cause());
                                            }
                                        } catch (Exception e) {
                                            future.completeExceptionally(e);
                                        }
                                    });
                                    //} // TODO : agregar else
                                } else {
                                    future.completeExceptionally(replyGetPackages.cause());
                                }
                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    }
                } else {
                    future.completeExceptionally(replyGetPrepaid.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> setRadEadStatus(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcelId);
        this.dbClient.queryWithParams(GET_PARCEL_RAD_EAD_BY_PARCELID,params, replySelect -> {
            try{
                if (replySelect.succeeded()){
                    List<JsonObject> resultSelect = replySelect.result().getRows();
                    JsonObject objectRadEad = new JsonObject();
                    if(resultSelect.isEmpty()){
                        // future.completeExceptionally(replySelect.cause());
                        future.complete(objectRadEad);
                    }

                    objectRadEad.put("id",resultSelect.get(0).getInteger("id"));
                    objectRadEad.put("status", 7);
                    GenericQuery queryUpdate = this.generateGenericUpdate("parcels_rad_ead", objectRadEad);
                    conn.queryWithParams(queryUpdate.getQuery(), queryUpdate.getParams(), replyUpdate -> {
                        try {
                            if(replyUpdate.succeeded()){
                                //this.commit(conn, message , objectRadEad);
                                future.complete(objectRadEad);
                            } else {
                                future.completeExceptionally(replyUpdate.cause());
                            }
                            // future.completeExceptionally(e);

                        } catch (Exception  e){
                            future.completeExceptionally(e);
                        }
                    });

                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            JsonObject customerObject = new JsonObject();

            double creditAvailable;
            creditAvailable = actualCreditAvailable + debt;

            customerObject
                    .put(ID, customerCreditData.getInteger(ID))
                    .put("credit_available", creditAvailable)
                    .put(UPDATED_BY, createdBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            GenericQuery updateCustomer = this.generateGenericUpdate("customer", customerObject);
            conn.updateWithParams(updateCustomer.getQuery(), updateCustomer.getParams(), (AsyncResult<UpdateResult> replyCustomer) -> {
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

    private void insertTicketEndCancel(SQLConnection conn, Message<JsonObject> message, JsonObject resultParcelStatus, Integer parcelId, Integer cancelBy, Boolean is_credit, Integer customerId, Double total_amount){
        this.insertTicket(conn, null, parcelId, 0.00, null, cancelBy, 0.00,  "voucher").whenComplete((JsonObject ticketV, Throwable ticketErrorV) -> {
            try{
                if (ticketErrorV != null) {
                    this.rollback(conn, ticketErrorV, message);
                } else {
                    Integer ticketId = ticketV.getInteger(ID);
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Comprobante de cancelación de paquetería");
                    ticketDetail.put("unit_price", 0.00);
                    ticketDetail.put(AMOUNT, 0.00);
                    ticketDetail.put(CREATED_BY, cancelBy);

                    GenericQuery insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);

                    conn.updateWithParams(insertTicketDetail.getQuery(), insertTicketDetail.getParams(), replyInsertTicketDetail -> {
                        try {
                            if (replyInsertTicketDetail.failed()){
                                throw new Exception(replyInsertTicketDetail.cause());
                            }

                            resultParcelStatus.put("voucher_ticket_id", ticketId);
                            if (is_credit) {
                                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                JsonObject paramsCredit = new JsonObject().put("customer_id", customerId);
                                getVertx().eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                    try {
                                        if (replyCredit.failed()) {
                                            throw new Exception(replyCredit.cause());
                                        }
                                        Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                        JsonObject customerCreditData = customerCreditDataMsg.body();
                                        this.updateCustomerCredit(conn, customerCreditData, total_amount, cancelBy)
                                                .whenComplete((replyCustomer, errorCustomer) -> {
                                                    try{
                                                        if (errorCustomer != null) {
                                                            throw new Exception(errorCustomer);
                                                        }
                                                        this.commit(conn, message, resultParcelStatus);

                                                    } catch (Throwable t) {
                                                        this.rollback(conn, t, message);
                                                    }
                                                });

                                    } catch (Exception e) {
                                        this.rollback(conn, e, message);
                                    }
                                });

                            } else {
                                this.commit(conn, message, resultParcelStatus);
                            }


                        } catch (Exception e){
                            this.rollback(conn, e, message);
                        }
                    });
                }
            } catch (Exception e){
                this.rollback(conn, e, message);
            }
        });
    }

    private CompletableFuture<Integer> paymentInsert(SQLConnection conn, JsonArray payments, JsonObject parcel, JsonArray packagesArray,
                                                     Double innerTotalAmount, Integer cashOutId, JsonObject cashChange, Integer updatedBy) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Future f1 = Future.future();
        getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"),
                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());

        Future f2 = Future.future();
        getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"),
                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());

        CompositeFuture.all(f1, f2).setHandler(r -> {
            try {
                if (r.succeeded()) {
                    JsonObject currencyMsg = r.result().<Message<JsonObject>>resultAt(0).body();
                    Integer currencyId = Integer.valueOf(currencyMsg.getString("value"));
                    JsonObject ivaMsg = r.result().<Message<JsonObject>>resultAt(0).body();
                    Double ivaPercent = Double.valueOf(ivaMsg.getString("value"));
                    //change this , its a test to the rad parcel
                    JsonObject rad = new JsonObject();

                    double totalPayments = 0.0;
                    final int pLen = payments.size();

                    for (int i = 0; i < pLen; i++) {
                        JsonObject payment = payments.getJsonObject(i);
                        Double paymentAmount = payment.getDouble("amount");
                        if (paymentAmount == null || paymentAmount < 0.0) {
                            throw new Exception("Invalid payment amount: " + paymentAmount);
                        }
                        totalPayments += UtilsMoney.round(paymentAmount, 2);
                    }

                    if (totalPayments > innerTotalAmount) {
                        future.completeExceptionally(new Throwable("The payment " + totalPayments + " is greater than the total " + innerTotalAmount));
                    } else if (totalPayments < innerTotalAmount) {
                        future.completeExceptionally(new Throwable("The payment " + totalPayments + " is lower than the total " + innerTotalAmount));
                    } else {

                        // Insert ticket
                        this.insertTicket(conn, cashOutId, parcel.getInteger("id"), innerTotalAmount, cashChange, updatedBy, ivaPercent, "purchase").whenComplete((JsonObject ticket, Throwable ticketError) -> {
                            if (ticketError != null) {
                                future.completeExceptionally(ticketError);
                            } else {
                                // Insert ticket detail
                                this.insertTicketDetail(conn, ticket.getInteger("id"), updatedBy, packagesArray, new JsonArray(), parcel, rad).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                    if (dError != null) {
                                        future.completeExceptionally(dError);
                                    } else {
                                        // insert payments
                                        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                        for (int i = 0; i < pLen; i++) {
                                            JsonObject payment = payments.getJsonObject(i);
                                            payment.put("ticket_id", ticket.getInteger("id"));
                                            pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcel.getInteger("id"), cashOutId, updatedBy));
                                        }
                                        CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));

                                        allPayments.whenComplete((s, t) -> {
                                            try {
                                                if (t != null) {
                                                    future.completeExceptionally(t.getCause());
                                                } else {
                                                    future.complete(ticket.getInteger("id"));
                                                }
                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                } else {
                    future.completeExceptionally(r.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> parcelRegisterFromCancel(SQLConnection conn, JsonObject cancelBody, JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.createParcelRegisterObject(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
            try {
                if (errorParcelRegisterObject != null){
                    throw errorParcelRegisterObject;
                }

                this.prepareRegister(parcel).whenComplete((resultPrepare, errorPrepare) -> {
                    try {
                        if (errorPrepare != null){
                            throw errorPrepare;
                        }

                        this.register(conn, resultPrepare).whenComplete((resultRegister, errorRegister) -> {
                            try {
                                if (errorRegister != null){
                                    throw errorRegister;
                                }

                                future.complete(resultRegister);

                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> createParcelRegisterObject(SQLConnection conn, JsonObject body, JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray compareArray = new JsonArray()
                .add(ADDRESSEE_ID)
                .add(ADDRESSEE_NAME)
                .add(ADDRESSEE_LAST_NAME)
                .add(ADDRESSEE_PHONE)
                .add(ADDRESSEE_EMAIL)
                .add(ADDRESSEE_ZIP_CODE)
                .add(ADDRESSEE_ADDRESS_ID);

        for (int i=0; i<compareArray.size(); i++){
            if (body.containsKey(compareArray.getString(i)) && body.getValue(compareArray.getString(i)) != null){
                parcel.put(compareArray.getString(i), body.getValue(compareArray.getString(i)));
            }
        }

        Integer parcelId = body.getInteger(PARCEL_ID);
        boolean isCredit = parcel.getString(PAYMENT_CONDITION).equals("credit");
        parcel.put("is_credit", isCredit);
        parcel.put(STATUS, 1);
        parcel.put(PARENT_ID, parcelId);
        parcel.put(INTERNAL_CUSTOMER, body.getBoolean(INTERNAL_CUSTOMER, false));
        parcel.put(CASHOUT_ID, body.getInteger(CASHOUT_ID));
        parcel.put(CASH_REGISTER_ID, body.getInteger(CASH_REGISTER_ID));
        parcel.put(CREATED_BY, body.getInteger(UPDATED_BY));
        parcel.put(CREATED_AT, sdfDataBase(new Date()));
        parcel.put("reissue_debt", parcel.getDouble("debt"));

        if (isCredit){
            Double totalAmount = parcel.getDouble(TOTAL_AMOUNT);
            Double debt = parcel.getDouble("debt", 0.00);
            parcel.put("reissue_paid", totalAmount - debt);
        }

        String newNotes = "Carta porte: ".concat(parcel.getString(WAYBILL));
        if (body.getString(CANCEL_CODE) != null){
            newNotes = newNotes.concat(" Código de cancelación: ").concat(body.getString(CANCEL_CODE));
        }

        parcel.put(NOTES, newNotes);

        this.getPackagesByParcelId(conn, parcelId).whenComplete((resultPackages, errorPackages) -> {
            try {
                if (errorPackages != null){
                    throw errorPackages;
                }

                parcel.put(PARCEL_PACKAGES, resultPackages);

                this.getPackingsByParcelId(conn, parcelId).whenComplete((resultPackings, errorPackings) -> {
                    try {
                        if (errorPackings != null){
                            throw errorPackings;
                        }

                        JsonArray payments = body.getJsonArray("payments");
                        JsonObject cashChange = body.getJsonObject("cash_change");

                        parcel.put(PARCEL_PACKINGS, resultPackings)
                                .put("payments", payments)
                                .put("cash_change", cashChange);

                        this.cleanRegisterObject(parcel);

                        future.complete(parcel);

                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getPackagesByParcelId(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKAGES_BY_PARCEL_ID, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(new JsonArray(result));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPackingsByParcelId(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKINGS_BY_PARCEL_ID, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(new JsonArray(result));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void cleanRegisterObject(JsonObject parcel){
        parcel.remove(ID);
        parcel.remove(TICKET_ID);
        parcel.remove("promo_id");
        parcel.remove(PARCELS_CANCEL_REASON_ID);
        parcel.remove(PAYMENT_CONDITION);
        parcel.remove(_PARCEL_STATUS);
        parcel.remove(STATUS);
        parcel.remove("updated");
        parcel.remove(UPDATED_BY);
        parcel.remove(UPDATED_AT);
    }

    private CompletableFuture<JsonObject> prepareRegister(JsonObject body){

        Boolean isCreditParcel = Boolean.FALSE;
        if (body.containsKey("is_credit")){
            isCreditParcel = body.getBoolean("is_credit");
        }
        boolean paysSender = body.getBoolean(PAYS_SENDER);
        JsonObject customerId = new JsonObject().put(BoardingPassDBV.CUSTOMER_ID, (Integer) body.remove("credit_customer_id"));

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Future<Message<JsonObject>> f1 = Future.future();
        Future<Message<JsonObject>> f2 = Future.future();
        Future<Message<JsonObject>> f3 = Future.future();
        Future<Message<JsonObject>> f4 = Future.future();
        Future<Message<JsonObject>> f5 = Future.future();
        getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
        getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
        getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "parcel_iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
        getVertx().eventBus().send(ScheduleRouteDBV.class.getSimpleName(), new JsonObject().put(SCHEDULE_ROUTE_DESTINATION_ID, body.getInteger(SCHEDULE_ROUTE_DESTINATION_ID)), new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.ACTION_GET_TERMINALS_BY_DESTINATION), f4.completer());
        getVertx().eventBus().send(CustomerDBV.class.getSimpleName(), customerId, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f5.completer());
        Boolean finalIsCreditParcel = isCreditParcel;
        CompositeFuture.all(f1, f2, f3, f4, f5).setHandler(detailReply -> {
            try {
                if (detailReply.failed()) {
                    throw detailReply.cause();
                }
                Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(0);
                Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(1);
                Message<JsonObject> parcelIvaMsg = detailReply.result().resultAt(2);
                Message<JsonObject> terminalsDestinationMsg = detailReply.result().resultAt(3);
                Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(4);

                JsonObject ivaPercent = ivaPercentMsg.body();
                JsonObject currencyId = currencyIdMsg.body();
                JsonObject parcelIva = parcelIvaMsg.body();
                JsonObject terminalsDestination = terminalsDestinationMsg.body();
                JsonObject customerCreditData = customerCreditDataMsg.body();

                body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                body.put("parcel_iva", Double.valueOf(parcelIva.getString("value")));
                body.put("parcel_tracking_code", UtilsID.generateID("G"));
                body.put(_TERMINAL_ORIGIN_ID, terminalsDestination.getInteger(_TERMINAL_ORIGIN_ID));
                body.put(_TERMINAL_DESTINY_ID, terminalsDestination.getInteger(_TERMINAL_DESTINY_ID));
                body.put("customer_credit_data", customerCreditData);
                body.put("payment_condition", finalIsCreditParcel ? "credit" : "cash");

                if (finalIsCreditParcel && paysSender) {
                    Double reissueDebt = body.getDouble("reissue_debt", 0.00);
                    Double parcelAvailableCredit = customerCreditData.getDouble("available_credit", 0.00);
                    Boolean parcelHasCredit = customerCreditData.getBoolean("has_credit", false);
                    if (parcelAvailableCredit == null) parcelAvailableCredit = (double) 0;
                    Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                    body.put("debt", parcelPaymentsAmount);

                    if (!parcelHasCredit) {
                        throw new Exception("Customer: no credit available");
                    }
                    if (parcelAvailableCredit < (parcelPaymentsAmount + reissueDebt)) {
                        throw new Exception("Customer: Insufficient funds to apply credit");
                    }
                    if(!customerCreditData.getString("services_apply_credit").contains("parcel"))
                        throw new Exception("Customer: service not applicable");
                }

                future.complete(body);

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject parcel) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            throw new Exception("Register not implemented");
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void swapResponsable(JsonObject parcel){
        Integer originId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
        Integer destinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
        Integer senderId = parcel.getInteger("sender_id");
        String senderName = parcel.getString("sender_name");
        String senderLastName = parcel.getString("sender_last_name");
        String senderPhone = parcel.getString("sender_phone");
        String senderEmail = parcel.getString("sender_email");
        Integer senderZipCode = parcel.getInteger("sender_zip_code");
        Integer senderAddressId = parcel.getInteger("sender_address_id");

        Integer addresseeId = parcel.getInteger(ADDRESSEE_ID);
        String addresseeName = parcel.getString(ADDRESSEE_NAME);
        String addresseeLastName = parcel.getString(ADDRESSEE_LAST_NAME);
        String addresseePhone = parcel.getString(ADDRESSEE_PHONE);
        String addresseeEmail = parcel.getString(ADDRESSEE_EMAIL);
        Integer addresseeZipCode = parcel.getInteger(ADDRESSEE_ZIP_CODE);
        Integer addresseeAddressId = parcel.getInteger(ADDRESSEE_ADDRESS_ID);

        parcel.put("terminal_origin_id", destinyId)
                .put("terminal_destiny_id", originId)
                .put("sender_id", addresseeId)
                .put("sender_name", addresseeName)
                .put("sender_last_name", addresseeLastName)
                .put("sender_phone", addresseePhone)
                .put("sender_email", addresseeEmail)
                .put("sender_zip_code", addresseeZipCode)
                .put("sender_address_id", addresseeAddressId)
                .put(ADDRESSEE_ID, senderId)
                .put(ADDRESSEE_NAME, senderName)
                .put(ADDRESSEE_LAST_NAME, senderLastName)
                .put(ADDRESSEE_PHONE, senderPhone)
                .put(ADDRESSEE_EMAIL, senderEmail)
                .put(ADDRESSEE_ZIP_CODE, senderZipCode)
                .put(ADDRESSEE_ADDRESS_ID, senderAddressId);
    }

    private CompletableFuture<JsonObject> registerExpense(SQLConnection conn, JsonObject expense, Integer cashOutId, Integer createdBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        GenericQuery insertExpenses = generateGenericCreate("expense", expense);
        conn.updateWithParams(insertExpenses.getQuery(), insertExpenses.getParams(), (AsyncResult<UpdateResult> reply) -> {
            try {
                if (reply.succeeded()) {
                    final int id = reply.result().getKeys().getInteger(0);
                    expense.put("id", id);

                    JsonObject cashOutMove = new JsonObject()
                            .put("cash_out_id", cashOutId)
                            .put("quantity", expense.getDouble("amount"))
                            .put("move_type", "1")
                            .put("created_by", createdBy)
                            .put("expense_id", id);
                    GenericQuery insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                    conn.updateWithParams(insertCashOutMove.getQuery(), insertCashOutMove.getParams(), (AsyncResult<UpdateResult> replyInsert) -> {
                        try {
                            if (replyInsert.succeeded()) {
                                future.complete(expense);
                            } else {
                                future.completeExceptionally(replyInsert.cause());
                            }
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer ticketId, Integer createdBy, JsonArray packages, JsonArray packings, JsonObject parcel, JsonObject serviceObject) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        conn.query("SELECT id, name_price, shipping_type FROM package_price;", replyPP -> {
            try{
                if(replyPP.failed()){
                    throw new Exception(replyPP.cause());
                }
                List<JsonObject> resultPP = replyPP.result().getRows();
                Map<String, List<JsonObject>> groupedPackages = packages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString("shipping_type")));
                JsonArray details = new JsonArray();

                for (String s : groupedPackages.keySet()) {
                    JsonObject packagePrice = new JsonObject();
                    AtomicReference<Integer> quantity = new AtomicReference<>(0);
                    AtomicReference<Double> unitPrice = new AtomicReference<>(0.00);
                    AtomicReference<Double> amount = new AtomicReference<>(0.00);
                    Optional<JsonObject> packageName = resultPP.stream().filter(x -> x.getInteger("id").equals(groupedPackages.get(s).get(0).getInteger("package_price_id"))).findFirst();
                    String packageRange = packageName.get().getString("name_price");
                    groupedPackages.get(s).forEach(x -> {
                        quantity.getAndSet(quantity.get() + 1);
                        unitPrice.updateAndGet(v -> v + x.getDouble("total_amount"));
                        amount.updateAndGet(v -> v + x.getDouble("total_amount"));
                        packagePrice.put(DISCOUNT, x.getDouble(DISCOUNT));
                    });
                    packagePrice.put("shipping_type", s);
                    packagePrice.put("unit_price", unitPrice.get());
                    packagePrice.put("amount", amount.get());
                    packagePrice.put("quantity", quantity.get());
                    if(packagePrice.getInteger("quantity") != null){
                        if(packagePrice.getInteger("quantity") > 0){
                            JsonObject ticketDetail = new JsonObject();
                            String shippingType = packagePrice.getString("shipping_type");
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
                            ticketDetail.put("quantity", packagePrice.getInteger("quantity"));
                            ticketDetail.put("detail", "Envío de " + shippingType + " con rango " + packageRange);
                            ticketDetail.put("unit_price", packagePrice.getDouble("unit_price"));
                            ticketDetail.put(DISCOUNT, packagePrice.getDouble(DISCOUNT));
                            ticketDetail.put("amount", packagePrice.getDouble("amount"));
                            ticketDetail.put("created_by", createdBy);
                            details.add(ticketDetail);
                        }
                    }
                }

                int len = packings.size();
                for (int i = 0; i < len; i++) {
                    JsonObject packing = packings.getJsonObject(i);
                    JsonObject ticketDetail = new JsonObject();

                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", packing.getInteger("quantity"));
                    ticketDetail.put("detail", "Embalaje");
                    ticketDetail.put("unit_price", packing.getDouble("unit_price"));
                    ticketDetail.put("amount", packing.getDouble("amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                if(parcel.getBoolean("has_insurance") != null && parcel.getBoolean("has_insurance")){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Seguro de envío");
                    ticketDetail.put("unit_price", parcel.getDouble("insurance_amount"));
                    ticketDetail.put("amount", parcel.getDouble("insurance_amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                if(serviceObject.getBoolean("is_rad") != null || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Servicio " +
                            serviceObject.getString("service"));
                    ticketDetail.put("unit_price", serviceObject.getDouble("service_amount"));
                    ticketDetail.put("amount", serviceObject.getDouble("service_amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }


                if(packages.isEmpty() && packings.isEmpty()) {
                    JsonObject ticketDetail = new JsonObject()
                            .put("ticket_id", ticketId)
                            .put("quantity", 0.00)
                            .put("detail", "Comprobante de entrega de paquetería")
                            .put("unit_price", 0.00)
                            .put("amount", 0.00)
                            .put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                List<GenericQuery> inserts = new ArrayList<>();
                for(int i = 0; i < details.size(); i++){
                    inserts.add(this.generateGenericCreate("tickets_details", details.getJsonObject(i)));
                }

                List<JsonArray> insertsParams = inserts.stream()
                                .map(GenericQuery::getParams).collect(Collectors.toList());
                conn.batchWithParams(inserts.get(0).getQuery(), insertsParams, (AsyncResult<List<Integer>> replyInsert) -> {
                    try {
                        if (replyInsert.failed()){
                            throw new Exception(replyInsert.cause());
                        }
                        future.complete(replyInsert.succeeded());
                    }catch(Exception e ){
                        future.completeExceptionally(e);
                    }
                });


            }catch (Exception ex){
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer packageId, Integer cashOutId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject cashOutMove = new JsonObject();
            payment.put("currency_id", currencyId);
            payment.put("parcel_id", packageId);
            payment.put("created_by", createdBy);
            cashOutMove.put("quantity", payment.getDouble("amount"));
            cashOutMove.put("move_type", "0");
            cashOutMove.put("cash_out_id", cashOutId);
            cashOutMove.put("created_by", createdBy);
            //String insert = this.generateGenericCreate("payment", payment);
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
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private static final String QUERY_GET_PARCEL_INFO ="SELECT \n" +
            " id, waybill, customer_id, branchoffice_id, boarding_pass_id, is_customer, \n" +
            " total_packages, shipment_type, \n" +
            " sender_id, sender_name, sender_last_name, sender_phone, sender_email, sender_zip_code, sender_address_id, terminal_origin_id, \n" +
            " terminal_destiny_id, has_invoice, num_invoice, exchange_rate_id, \n" +
            " has_insurance, insurance_value, insurance_amount, amount, total_amount, has_multiple_addressee, payment_condition,\n" +
            " addressee_id, addressee_name, addressee_last_name, addressee_phone, addressee_email, addressee_zip_code, addressee_address_id, \n" +
            " pays_sender, parcel_status, status, created_at,\n" +
            " schedule_route_destination_id, purchase_origin, insurance_id, invoice_id, invoice_is_global, debt, canceled_by\n" +
            "FROM parcels\n" +
            "WHERE id = ?";

    private static final String QUERY_GET_PARCEL_PACKAGE_BY_PARCEL_ID ="SELECT\n" +
            "   pp.id,\n" +
            "   pp.package_status,\n" +
            "   tl.travel_log_code\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_package_id = pp.id\n" +
            "   AND sppt.status = 'loaded'\n" +
            "INNER JOIN travel_logs tl ON tl.load_id = sppt.shipment_id\n" +
            "WHERE pp.parcel_id = ?\n" +
            "ORDER BY sppt.id DESC;";


    private static final String QUERY_EXPENSE_CONCEPT_RETURN = "SELECT\n"
            + "	id\n"
            + "FROM\n"
            + "	expense_concept\n"
            + "WHERE\n"
            + "	name = ?";

    private static final String GET_PARCEL_RAD_EAD_BY_PARCELID = "select * from parcels_rad_ead where parcel_id = ?";

    private static final String QUERY_GET_PACKAGES_BY_PARCEL_ID = "SELECT shipping_type, is_valid, need_auth, need_documentation, " +
            " parcels_deliveries_id, package_type_id, weight, height, " +
            " width, length, notes, schedule_route_destination_id, contains " +
            " FROM parcels_packages " +
            " WHERE parcel_id = ?;";

    private static final String QUERY_GET_PACKINGS_BY_PARCEL_ID = "SELECT \n" +
            " packing_id, quantity\n" +
            " FROM parcels_packings \n" +
            " WHERE parcel_id = ?;";

    private static final String QUERY_GET_PARCEL_PREPAID_DETAILS = "SELECT \n" +
            "   ppd.*,\n" +
            "   COALESCE(pp.id, \n" +
            "   (SELECT pp2.id FROM parcels_packages pp2 \n" +
            "           WHERE pp2.parcel_id = p.id LIMIT 1)\n" +
            "           ) AS cancel_parcel_package_id\n" +
            "FROM parcels_prepaid_detail ppd\n" +
            "INNER JOIN parcels p ON p.id = ppd.parcel_id\n" +
            "LEFT JOIN parcels_packages pp ON pp.parcel_prepaid_detail_id = ppd.id\n" +
            "WHERE ppd.parcel_id = ? \n" +
            "   AND ppd.parcel_status = 1\n" +
            "GROUP BY ppd.id;";

}
