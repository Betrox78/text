package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.money.PaymentDBV;
import database.parcel.GuiappDBV;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.parcel.GuiappDBV.GET_PERCENT_DISCOUNT;
import static service.commons.Constants.*;
import static utils.UtilsDate.sdfDataBase;

public class Register extends DBHandler<GuiappDBV> {

    public Register(GuiappDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {

            this.startTransaction(message, (SQLConnection conn) -> {
                JsonObject parcel = message.body();
                this.register(conn, parcel).whenComplete((resultRegister, errorRegister) -> {
                    try {
                        if (errorRegister != null){
                            throw errorRegister;
                        }
                        this.commit(conn, message, resultRegister);
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject guiapp) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {

            Integer cashOutId = (Integer) guiapp.remove(CASHOUT_ID);
            Integer cashRegisterId = guiapp.getInteger(CASH_REGISTER_ID);
            JsonObject customerCreditData = (JsonObject) guiapp.remove("customer_credit_data");
            Double availableCredit = customerCreditData.getDouble("available_credit");
            String reservation_code = UtilsID.generateID("PP");
            final Boolean is_credit = guiapp.containsKey("is_credit") ? ((Boolean) guiapp.remove("is_credit")) : false;
            JsonObject ppDetail = (JsonObject) guiapp.remove("pp_detail");
            JsonArray payments = (JsonArray) guiapp.remove("payments");
            JsonObject objPayment = new JsonObject();
            JsonArray packages = guiapp.getJsonArray("packages");
            //borrar
            if(!is_credit){
                objPayment =  payments.getJsonObject(0);
                objPayment.put("alias", guiapp.getString("payment_condition"));
            }

            Integer promo_id = (Integer) guiapp.remove("promo_id");

            final JsonObject finalObjPayment = objPayment;
            final double ivaPercent = (Double) guiapp.remove("iva_percent");
            final int currencyId = (Integer) guiapp.remove("currency_id");
            final Integer createdBy = guiapp.getInteger("created_by");

            double creditBalance = 0;
            double debt = 0;

            Double totalAmountInner = guiapp.getDouble("total_amount");
            if(is_credit) {
                creditBalance = availableCredit - totalAmountInner;
                debt = totalAmountInner;
            }

            final Double finalDebt = debt;
            final Double finalCreditBalance = creditBalance;
            final Double percentDiscountApplied = guiapp.getDouble("percent_discount_applied", 0.00);
            final Double percentDiscountAppliedRounded = Math.round((percentDiscountApplied > 0 ? percentDiscountApplied : 0) * 100.0) / 100.0;

            JsonObject guiappObject = new JsonObject();
            guiappObject.put("tracking_code", reservation_code)
                    .put("shipment_type", guiapp.getString("shipment_type"))
                    .put("payment_condition", guiapp.getString("payment_condition"))
                    .put("purchase_origin", guiapp.getString("purchase_origin"))
                    .put("customer_id", guiapp.getInteger("customer_id"))
                    .put("crated_by", guiapp.getInteger("created_by"))
                    .put("created_at", UtilsDate.sdfDataBase(new Date()))
                    .put("parcel_status",1)
                    .put("status", 1)
                    .put("cash_register_id",guiapp.getInteger("cash_register_id"))
                    .put("total_count_guipp", guiapp.getInteger("total_count_guipp"))
                    .put("total_count_guipp_remaining", 0)
                    .put("amount",guiapp.getInteger("amount"))
                    .put("discount", guiapp.getDouble("discount"))
                    .put("has_insurance", false)
                    .put("insurance_value", guiapp.getInteger("insurance_value"))
                    .put("insurance_amount", guiapp.getInteger("insurance_amount"))
                    .put("extra_charges", guiapp.getInteger("extra_charges"))
                    .put("iva",guiapp.getDouble("iva_percent") )
                    .put("parcel_iva", guiapp.getDouble("parcel_iva"))
                    .put("total_amount", totalAmountInner)
                    .put("debt", debt)
                    .put("expire_at", UtilsDate.sdfDataBase(UtilsDate.addYears(new Date(), 1)))
                    .put("branchoffice_id", guiapp.getInteger("terminal_origin_id"))
                    .put("seller_user_id", guiapp.containsKey("seller_id")?guiapp.getInteger("seller_id"):null)
                    .put("billing_address_id", guiapp.getInteger("billing_address_id"))
                    .put("percent_discount_applied", percentDiscountAppliedRounded)
                    .put("customer_billing_information_id", guiapp.getInteger("customer_billing_information_id"));

            if(promo_id != null) {
                guiappObject.put("promo_id", promo_id);
            }

            JsonObject basePackage = packages.getJsonObject(0);
            Double unitPrice = UtilsMoney.round(totalAmountInner / basePackage.getInteger(_QUANTITY), 2);
            JsonObject bodyPD = new JsonObject()
                    .put(_PACKAGE_PRICE_ID, basePackage.getInteger("idRango"))
                    .put(_PACKAGE_PRICE_KM_ID, basePackage.getInteger("idKm"))
                    .put(_UNIT_PRICE, unitPrice);
            this.getVertx().eventBus().send(GuiappDBV.class.getSimpleName(), bodyPD, new DeliveryOptions().addHeader(ACTION, GET_PERCENT_DISCOUNT), (AsyncResult<Message<JsonObject>> replyPD) -> {
                try {
                    if (replyPD.failed()) {
                        throw replyPD.cause();
                    }
                    JsonObject percentDiscountObj = replyPD.result().body();
                    Double percentDiscount = percentDiscountObj.getDouble(_PERCENT_DISCOUNT_APPLIED);

                    guiappObject.put(_PERCENT_DISCOUNT_APPLIED, (Math.round((percentDiscount > 0 ? percentDiscount : 0) * 100.0) / 100.0));

                    GenericQuery gen = this.generateGenericCreate(guiappObject);
                    conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> guiappReply) -> {
                        try {
                            if(guiappReply.failed()) {
                                throw guiappReply.cause();
                            }

                            final int guiappID = guiappReply.result().getKeys().getInteger(0);
                            guiappObject.put(ID, guiappID);

                            this.insertPrepaidDetail(conn, guiappObject, packages).whenComplete((resultDetail, errorDetail) -> {
                                try {
                                    if(errorDetail != null) {
                                        throw  errorDetail;
                                    }

                                    this.endRegister(conn, guiappObject, finalObjPayment, createdBy, cashOutId, cashRegisterId, guiappID, ivaPercent, currencyId, ppDetail, is_credit, customerCreditData, finalCreditBalance, finalDebt).whenComplete((resultRegister, errorRegister) -> {
                                        try {
                                            if (errorRegister != null) {
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

    protected CompletableFuture<JsonObject> insertPrepaidDetail(SQLConnection conn, JsonObject guiappObject, JsonArray packages){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            final int numberPackages = packages.size();
            List<CompletableFuture<JsonObject>> detailTask = new ArrayList<>();
            for (int i = 0;i < numberPackages; i++) {
                int numInside = packages.getJsonObject(i).getInteger("quantity");
                for(int x = 0; x < numInside; x++) {
                    detailTask.add(insertPrepaidDetailPP(conn, guiappObject, packages.getJsonObject(i) ));
                }
            }

            CompletableFuture.allOf(detailTask.toArray(new CompletableFuture[detailTask.size()])).whenComplete((dP, dpError) -> {
                try {
                    if( dpError != null) {
                        throw  dpError;
                    }
                    future.complete(guiappObject);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    protected CompletableFuture<JsonObject> endRegister(SQLConnection conn, JsonObject guiapp, JsonObject objPayment, Integer createdBy,  Integer cashOutId, Integer cashRegisterId, Integer guiappID, Double ivaPercent, Integer currencyId, JsonObject ppDetail, Boolean isCredit, JsonObject customerCreditData,  Double finalCreditBalance, Double finalDebt ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Double paid = isCredit ? guiapp.getDouble(AMOUNT) : objPayment.getDouble(_AMOUNT);
            Double total = guiapp.getDouble(_TOTAL_AMOUNT);
            Double iva = this.getIva(total, ivaPercent);
            Double paid_change = paid - total;

            JsonArray ticketArray = new JsonArray();
            List<CompletableFuture<JsonObject>> ticketsTask = new ArrayList<>();
            ticketArray.add(new JsonObject());

            final int numTickets = ticketArray.size();
            for(int i = 0; i < numTickets ; i++){
                ticketsTask.add(insertTicketsPP(conn, cashOutId, guiappID, objPayment, cashRegisterId, createdBy, true, total, iva, paid, paid_change, ticketArray.getJsonObject(i), ppDetail, isCredit ));
            }

            CompletableFuture.allOf(ticketsTask.toArray(new CompletableFuture[ticketsTask.size()])).whenComplete((st, tError) -> {
                try {
                    if(tError != null) {
                        throw tError;
                    }

                    this.insertTicketDetail(conn, ticketArray, createdBy, guiapp).whenComplete((Boolean detailSuccess, Throwable dError) -> {
                        try {
                            if(dError != null) {
                                throw  dError;
                            }

                            if(isCredit) {

                                this.updateCustomerCredit(conn, customerCreditData, finalDebt, createdBy, false, false, finalCreditBalance)
                                        .whenComplete( (replyCustomer, errorCustomer) -> {
                                            try {
                                                if(errorCustomer != null) {
                                                    throw new Exception(errorCustomer);
                                                }

                                                JsonObject result = new JsonObject().put(ID, guiappID);
                                                result.put("tracking_code", guiapp.getString("tracking_code"));
                                                result.put("ticket_prepaid", ticketArray.getValue(0));

                                                future.complete(result);
                                            } catch (Throwable t) {
                                                t.printStackTrace();
                                                future.completeExceptionally(t);
                                            }
                                        });

                            } else {

                                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();

                                for (int pNum = 0; pNum < ticketArray.size(); pNum++) {
                                    pTasks.add(insertPaymentAndCashOutMove(conn, objPayment, ticketArray.getJsonObject(pNum), guiapp ,createdBy, cashOutId, cashRegisterId, currencyId));
                                }

                                CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pTasks.size()])).whenComplete((pSuccess, pError) -> {
                                    try {
                                        if(pError != null) {
                                            throw pError;
                                        }

                                        JsonObject result = new JsonObject().put("id", guiappID);
                                        result.put("tracking_code", guiapp.getString("tracking_code"));
                                        result.put("ticket_prepaid", ticketArray.getValue(0));

                                        future.complete(result);

                                    } catch (Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });

                            }

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
        return future;
    }

    private CompletableFuture<JsonObject> insertPrepaidDetailPP(SQLConnection conn, JsonObject ppObject, JsonObject ppPack) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {

            String shipping_type = ppPack.getString("shipping_type");
            Integer idRango = ppPack.getInteger("idRango");

            conn.queryWithParams(QUERY_VALID_PACKAGE, new JsonArray().add(shipping_type).add(idRango), replyValidation -> {
                try {
                    if (replyValidation.failed()) {
                        throw replyValidation.cause();
                    }
                    if (replyValidation.result().getRows().isEmpty()) {
                        throw new Exception("El rango no es del mismo tipo de shipping");
                    }

                    conn.queryWithParams(QUERY_GET_SHIPPING_TYPE, new JsonArray().add(shipping_type), replyShippingType -> {
                        try {
                            if(replyShippingType.failed()) {
                                throw replyShippingType.cause();
                            }

                            Integer shippingTypeId  =  replyShippingType.result().getRows().get(0).getInteger("id");
                            JsonObject detailObject = new JsonObject()
                                    .put("guiapp_code", UtilsID.generateGuiaPpID("GPP"))
                                    .put("parcel_prepaid_id", ppObject.getInteger("id"))
                                    .put("price_km", ppPack.getDouble("price_km"))
                                    .put("price_km_id",  ppPack.getInteger("idKm"))
                                    .put("price", ppPack.getDouble("price"))
                                    .put("price_id", idRango)
                                    .put("amount", 0)
                                    .put("discount",0)
                                    .put("parcel_status", 0)
                                    .put("crated_by", ppObject.getInteger("created_by") )
                                    .put("created_at", ppObject.getString("created_at"))
                                    .put("expire_at", ppObject.getString("expire_at"))
                                    .put("package_type_id", shippingTypeId);

                            GenericQuery insert = this.generateGenericCreate("parcels_prepaid_detail", detailObject);

                            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                try {
                                    if (reply.succeeded()) {
                                        future.complete(detailObject);
                                    } else {
                                        future.completeExceptionally(reply.cause());
                                    }
                                } catch (Exception e){
                                    future.completeExceptionally(e);
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
        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private Double getIva(Double amount, Double ivaPercent) {
        return amount - (amount /(1 + (ivaPercent/100)));
    }

    private CompletableFuture<JsonObject> insertTicketsPP(SQLConnection conn, Integer cashOutId, Integer idService, JsonObject objPayment, Integer cashRegisterId, Integer createdBy, Boolean isPrepaid, Double total, Double iva, Double paid, Double paidChange, JsonObject ticket, JsonObject ppDetail, Boolean isCredit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {

            String action = isCredit ? "voucher" : "purchase";

            if(isPrepaid) {
                ticket.put("parcel_prepaid_id", idService);
            }

            ticket.put("iva",iva);
            ticket.put("total", total);
            ticket.put("paid", paid);
            ticket.put("paid_change", paidChange);
            //Qué es payback y para que se utiliza?
            ticket.put("payback_before", 0);
            ticket.put("payback_money", 0);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("created_by", createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));

            ticket.put("action", action);
            ticket.put("created_at", sdfDataBase(new Date()));
            ticket.put("extra_charges",0);
            ticket.put("has_extras",0);

            GenericQuery insert = this.generateGenericCreate("tickets", ticket);

            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try {
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

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, JsonArray tickets, Integer createdBy, JsonObject guiapp) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<GenericQuery> inserts = new ArrayList<>();

        JsonArray details = new JsonArray();
        try {
            for(int i = 0 ; i < tickets.size(); i++){
                JsonObject tick = tickets.getJsonObject(i);

                JsonObject detailTicket = new JsonObject();
                detailTicket.put("ticket_id", tick.getInteger("id"));
                detailTicket.put("detail", "Paquete PP");
                detailTicket.put("unit_price", tick.getDouble("total"));
                detailTicket.put("amount", tick.getDouble("total"));
                detailTicket.put("discount",guiapp.getDouble("discount"));
                detailTicket.put("created_at", UtilsDate.sdfDataBase(new Date()));
                detailTicket.put("created_by",createdBy);
                detailTicket.put("quantity", 1);
                details.add(detailTicket);

                JsonObject packagesPP = new JsonObject();
                packagesPP.put("ticket_id", tick.getInteger("id"));
                packagesPP.put("detail", "Paquetes canjeables");
                packagesPP.put("unit_price", 0.0);
                packagesPP.put("amount", 0.0);
                packagesPP.put("discount",0.0);
                packagesPP.put("created_at", UtilsDate.sdfDataBase(new Date()));
                packagesPP.put("created_by",createdBy);
                packagesPP.put("quantity", guiapp.getInteger("total_count_guipp"));
                details.add(packagesPP);
            }

            for(int z = 0; z < details.size(); z++) {
                inserts.add(this.generateGenericCreate("tickets_details", details.getJsonObject(z)));
            }

            List<JsonArray> params = inserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());

            conn.batchWithParams(inserts.get(0).getQuery(), params, (AsyncResult<List<Integer>> replyInsert) -> {
                try {
                    if (replyInsert.failed()){
                        throw new Exception(replyInsert.cause());
                    }
                    future.complete(replyInsert.succeeded());

                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt, Integer createdBy, boolean is_cancel, boolean reissue, Double creditBalance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            Double actualCreditBalance = customerCreditData.getDouble("credit_balance");
            JsonObject customerObject = new JsonObject();

            double creditAvailable = 0;
            if(is_cancel){
                creditAvailable = actualCreditAvailable + debt;
            } else {
                if (reissue){
                    //double diffPaidDebt = reissuePaid - debt;
                    customerObject.put("credit_balance", creditBalance > 0 ? actualCreditBalance + creditBalance : actualCreditBalance);
                    creditAvailable = debt > 0 ? actualCreditAvailable - debt : actualCreditAvailable;
                } else {
                    creditAvailable = actualCreditAvailable - debt;
                }
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
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject objPayment, JsonObject ticket, JsonObject guiapp, Integer createdBy, Integer cashOutId, Integer cashRegisterId,  Integer currencyId ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject payment = new JsonObject();
            JsonObject cashOutMove = new JsonObject();

            if(ticket.getInteger("parcel_prepaid_id") != null) {
                payment.put("parcel_prepaid_id",ticket.getInteger("parcel_prepaid_id"));
            }

            payment.put("payment_method_id", objPayment.getInteger("payment_method_id"));
            payment.put("payment_method", objPayment.getString("alias"));
            payment.put("amount", ticket.getDouble("total"));
            payment.put("currency_id",currencyId );
            payment.put("created_by",createdBy);
            payment.put("created_at",UtilsDate.sdfDataBase(new Date()));
            payment.put("ticket_id",ticket.getInteger("id"));

            PaymentDBV objPaymentDBV = new PaymentDBV();
            objPaymentDBV.insertPayment(conn, payment).whenComplete((resultPayment, errorPayment) -> {
                if (errorPayment != null) {
                    future.completeExceptionally(errorPayment);
                } else {
                    payment.put("id", resultPayment.getInteger("id"));
                    cashOutMove.put("payment_id", resultPayment.getInteger("id"));
                    cashOutMove.put("cash_out_id", cashOutId);
                    cashOutMove.put("quantity", payment.getDouble("amount"));
                    cashOutMove.put("move_type", "0");
                    cashOutMove.put("created_by", createdBy);
                    GenericQuery insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);

                    conn.updateWithParams(insertCashOutMove.getQuery(), insertCashOutMove.getParams(), (AsyncResult<UpdateResult> replyMove) -> {
                        try {
                            if (replyMove.failed()) {
                                throw new Exception(replyMove.cause());
                            }

                            future.complete(payment);

                        } catch (Exception e) {
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private static final String QUERY_VALID_PACKAGE = "select distinct * from package_price \n" +
            "where shipping_type = ? \n" +
            "and id = ? ;";

    private static final String QUERY_GET_SHIPPING_TYPE = "select *\n" +
            "from package_types \n" +
            "where shipping_type = ? limit 1";

}
