package database.prepaid;

import database.commons.ErrorCodes;
import database.conekta.conektaDBV;
import io.conekta.Error;
import io.conekta.ErrorList;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.sql.ResultSet;
import org.json.JSONObject;
import static database.conekta.conektaDBV.SAVE_PAYMENT_PREPAID;
import static database.promos.PromosDBV.FLAG_PROMO;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import database.money.PaymentDBV;
import io.vertx.core.json.JsonObject;
import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import static service.commons.Constants.*;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;
import utils.UtilsValidation;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;
import static utils.UtilsDate.sdfDataBase;
import static utils.UtilsValidation.INVALID_FORMAT;
public class PrepaidTravelDBV extends DBVerticle {
    //Vertex actions
    public static final String EXPIRES_AT = "expires_at";

    public static final String ACTION_REGISTER_TICKETS = "PrepaidTravelDBV.register";
    public static final String ACTION_REGISTER_TICKETS_WEB = "PrepaidTravelDBV.registerWeb";
    public static final String ACTION_GET_PACKAGE_TRAVEL = "PrepaidTravelDBV.findPrepaidTravel";
    public static final String ACTION_GET_BOARDINGPASSES_BY_ID = "PrepaidTravelDBV.searchPassesById";
    public static final String ACTION_GET_DESTINY_TERMINALS_BY_ORIGIN_DISTANCE = "PrepaidTravelDBV.getTerminalDestinyByOriginDistance";
    public static final String PREPAID_SALES_REPORT = "PrepaidTravelDBV.reportSales";

    @Override
    public String getTableName() { return "prepaid_package_travel"; }

    @Override
    protected void onMessage(Message<JsonObject> message){
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_REGISTER_TICKETS:
                this.register(message);
                break;
            case ACTION_REGISTER_TICKETS_WEB:
                this.registerWeb(message);
                break;
            case ACTION_GET_PACKAGE_TRAVEL:
                this.findPrepaidTravel(message);
                break;
            case ACTION_GET_BOARDINGPASSES_BY_ID:
                this.searchPassesById(message);
                break;
            case ACTION_GET_DESTINY_TERMINALS_BY_ORIGIN_DISTANCE:
                this.getTerminalDestinyByOriginDistance(message);
                break;
            case PREPAID_SALES_REPORT:
                this.reportSales(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject prepaid = message.body().copy();
            this.register(conn, prepaid).whenComplete( (resultRegister, errorRegister) -> {
                try {
                    if (errorRegister != null){
                        throw errorRegister;
                    }
                    this.commit(conn, message, resultRegister);
                } catch (Throwable t) {
                    t.printStackTrace();
                    this.rollback(conn, t , message);
                }
            });
        });
    }

    private void registerWeb(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject prepaid = message.body().copy();
            JsonObject prepaidPayment = message.body();
            this.registerWeb(conn, prepaid).whenComplete((resultRegister, errorRegister) -> {
                try {
                    if (errorRegister != null) {
                        throw errorRegister;
                    }

                    prepaidPayment.put("reservation_code",resultRegister.getValue("tracking_code"))
                            .put("prepaid_travel_id",resultRegister.getJsonObject("ticket_prepaid").getInteger("prepaid_travel_id"));
                    this.getOrderInfoWeb(conn, prepaidPayment).whenComplete((resulTPaymentConekta,errorPaymentConekta) ->{
                        try {
                            if (errorPaymentConekta != null) {
                                throw errorPaymentConekta;
                            }
                            this.commit(conn, message, resulTPaymentConekta);

                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                            this.rollback(conn, throwable, message);
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject prepaid ){
        CompletableFuture future = new CompletableFuture();
        try {
            JsonObject prepaid_obj = new JsonObject();
            Integer cashOutId = (Integer) prepaid.remove(CASHOUT_ID);
            Integer cashRegisterId = (Integer) prepaid.remove(CASH_REGISTER_ID);
            JsonObject creditCustomerData = (JsonObject) prepaid.remove("customer_credit_data");
            Boolean isCredit = (Boolean) prepaid.remove("is_credit");
            JsonArray boardingPasses = (JsonArray) prepaid.remove("boarding_passes");
            JsonObject customer = (JsonObject)  prepaid.remove("customer");
            JsonObject seller = (JsonObject) prepaid.remove("seller");
            int expire_open_tickets_after = (int) prepaid.remove("expire_open_tickets_after");
            String createdAt = UtilsDate.sdfDataBase(new Date());

            if( seller != null) {
                prepaid_obj.put("seller_user_id", seller.getInteger("id"));
            }

            JsonObject prepaid_package = (JsonObject) prepaid.remove("prepaid_package");

            if( prepaid_package != null) {
                Double aObj = prepaid_package.getDouble("money");
                prepaid.put("amount", aObj);
                prepaid.put("total_amount", aObj);
                if(isCredit) {
                    prepaid.put("debt", aObj);
                    prepaid.put("payment_condition", "credit");

                    //pass it to BD insert obj
                    prepaid_obj.put("debt", aObj);
                    prepaid_obj.put("payment_condition", "credit");
                }
            }

            JsonArray payments = (JsonArray) prepaid.remove("payments");
            JsonObject objPayment = payments.getJsonObject(0);
            Integer createdBy = prepaid.getInteger("created_by");
            Double ivaPercent = (Double) prepaid.remove("iva_percent");
            Integer currencyId = (Integer) prepaid.remove("currency_id");

            //We create prepaid object
            prepaid.put("created_by", createdBy);
            prepaid.put("customer_id", customer.getInteger("id"));
            prepaid.put("prepaid_status", 0 );
            prepaid.put("active_tickets", prepaid_package.getInteger("tickets_quantity"));
            prepaid.put("total_tickets", prepaid_package.getInteger("tickets_quantity"));
            prepaid.put("prepaid_package_config_id", prepaid_package.getInteger("id"));
            //prepaid.put("created_at", sdfDataBase(new Date()));
            prepaid.put("created_at", createdAt);

            prepaid_obj.put("reservation_code", prepaid.getString("reservation_code"));
            prepaid_obj.put("amount", prepaid.getDouble("amount"));
            prepaid_obj.put("total_amount", prepaid.getDouble("total_amount"));
            prepaid_obj.put("iva", this.getIva(prepaid.getDouble("total_amount"), ivaPercent));
            prepaid_obj.put("created_by", createdBy);
            prepaid_obj.put("customer_id", customer.getInteger("id"));
            prepaid_obj.put("branchoffice_id", prepaid.getString("branchoffice_id"));
            prepaid_obj.put("prepaid_status", 0);
            prepaid_obj.put("active_tickets", prepaid_package.getInteger("tickets_quantity"));
            prepaid_obj.put("total_tickets", prepaid_package.getInteger("tickets_quantity"));
            prepaid_obj.put("prepaid_package_config_id", prepaid_package.getInteger("id"));
            prepaid_obj.put("created_at", createdAt);

            GenericQuery gen = this.generateGenericCreate(prepaid_obj);
            conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> prepaidReply) -> {
                try {
                    if(prepaidReply.failed()) {
                        throw  prepaidReply.cause();
                    }
                    final int prepaidTravelId = prepaidReply.result().getKeys().getInteger(0);
                    final int numBoardingPasses = boardingPasses.size();

                    List<CompletableFuture<JsonObject>> boardingPassesTask = new ArrayList<>();

                    prepaid_obj.put(ID, prepaidTravelId);

                    for (int i = 0; i < numBoardingPasses; i++) {
                        boardingPasses.getJsonObject(i).put("terminal_origin_id", prepaid.getJsonObject("terminal_origin").getInteger("id"));
                        boardingPasses.getJsonObject(i).put("terminal_destiny_id", prepaid.getJsonObject("terminal_destiny").getInteger("id"));
                        boardingPassesTask.add(registerBoardingPassesInPrepaid(conn, boardingPasses.getJsonObject(i), prepaidTravelId, createdBy , expire_open_tickets_after, createdAt, isCredit));
                    }

                    CompletableFuture.allOf(boardingPassesTask.toArray(new CompletableFuture[numBoardingPasses])).whenComplete((ps, pt) -> {
                        try {
                            if (pt != null){
                                throw pt;
                            }

                            this.endRegister(conn, objPayment, createdBy, prepaid_obj, boardingPasses, cashOutId ,cashRegisterId, ivaPercent, currencyId, isCredit, creditCustomerData).whenComplete((resultRegister, errorRegister) -> {
                                try {
                                    if (errorRegister != null) {
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
        return future;
    }

    private CompletableFuture<JsonObject> registerWeb(SQLConnection conn, JsonObject prepaid) {
        CompletableFuture future = new CompletableFuture();
        try {
            JsonArray boardingPasses = (JsonArray) prepaid.remove("boarding_passes");
            JsonObject customer = (JsonObject) prepaid.remove("customer");
            int expire_open_tickets_after = (int) prepaid.remove("expire_open_tickets_after");
            String createdAt = UtilsDate.sdfDataBase(new Date());
            JsonObject prepaid_package = (JsonObject) prepaid.remove("prepaid_package");

            if (prepaid_package != null) {
                Double aObj = prepaid_package.getDouble("money");
                prepaid.put("amount", aObj);
                prepaid.put("total_amount", aObj);
            }

            JsonObject objPayment = (JsonObject) prepaid.remove("payment");

            Integer createdBy = prepaid.getInteger("created_by");
            Double ivaPercent = (Double) prepaid.remove("iva_percent");
            Integer currencyId = (Integer) prepaid.remove("currency_id");

            //We create prepaid object
            prepaid.put("created_by", createdBy);
            prepaid.put("customer_id", customer.getInteger("id"));
            prepaid.put("prepaid_status", 0);
            prepaid.put("active_tickets", prepaid_package.getInteger("tickets_quantity"));
            prepaid.put("total_tickets", prepaid_package.getInteger("tickets_quantity"));
            prepaid.put("prepaid_package_config_id", prepaid_package.getInteger("id"));

            prepaid.remove("special_ticket");
            prepaid.remove("id");
            prepaid.remove("holder");
            prepaid.remove("token_id");
            prepaid.remove("pay_amount");
            prepaid.remove("user_id");
            prepaid.remove("idPrepaidPackage");
            prepaid.remove("seller");
            prepaid.put("branchoffice_id", 5);
            prepaid.remove("payments");
            prepaid.put("created_at", createdAt);
            GenericQuery gen = this.generateGenericCreate(prepaid);

            conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> prepaidReply) -> {
                try {
                    if (prepaidReply.failed()) {
                        throw prepaidReply.cause();
                    }

                    final int prepaidTravelId = prepaidReply.result().getKeys().getInteger(0);
                    final int numBoardingPasses = boardingPasses.size();
                    List<CompletableFuture<JsonObject>> boardingPassesTask = new ArrayList<>();
                    prepaid.put(ID, prepaidTravelId);

                    for (int i = 0; i < numBoardingPasses; i++) {
                        boardingPassesTask.add(registerBoardingPassesInPrepaid(conn, boardingPasses.getJsonObject(i), prepaidTravelId, createdBy, expire_open_tickets_after, createdAt, false));
                    }

                    CompletableFuture.allOf(boardingPassesTask.toArray(new CompletableFuture[numBoardingPasses])).whenComplete((ps, pt) -> {
                        try {
                            if (pt != null) {
                                throw pt;
                            }

                            this.endRegister(conn, objPayment, createdBy, prepaid, boardingPasses, null, null, ivaPercent, currencyId, false, null).whenComplete((resultRegister, errorRegister) -> {
                                try {
                                    if (errorRegister != null) {
                                        throw errorRegister;
                                    }
                                    future.complete(resultRegister);
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

    private CompletableFuture<JsonObject> registerBoardingPassesInPrepaid(SQLConnection conn, JsonObject boardingPass, Integer prepaidTravelId, Integer createdBy, int expiresInt, String createdAt, Boolean isCredit){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            boardingPass.remove("ticket_type");
            boardingPass.put("ticket_type","abierto_sencillo");

            boardingPass.remove("special_ticket");

            if(boardingPass.getInteger("purchase_origin") == 1){
                boardingPass.remove("purchase_origin");
                boardingPass.put("purchase_origin", "sucursal");
            }

            boardingPass.put("created_by", createdBy);
            // Preguntar qué es el payback y los puntos
            boardingPass.put("payback", 0);

            //Expires date
            String expiresAt = null;
            if(boardingPass.getString("ticket_type").contains("abierto")){
                boardingPass.remove("is_open");
                Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd(createdAt), Calendar.DAY_OF_YEAR, expiresInt);
                expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
            }
            boardingPass.put(EXPIRES_AT, expiresAt);

            if(isCredit) {
                boardingPass.put("payment_condition","credit");
                boardingPass.put("debt", boardingPass.getDouble("total_amount"));
            }

            if(!boardingPass.getBoolean("haveDiscount")){
                boardingPass.remove("haveDiscount");
            }

            boardingPass.put("reservation_code", UtilsID.generateID("BPP"));
            boardingPass.remove("branchoffice_id");
            boardingPass.put("prepaid_id", prepaidTravelId);

            String update = this.generateGenericCreate("boarding_pass", boardingPass);

            conn.update(update, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    final int id = reply.result().getKeys().getInteger(0);
                    boardingPass.put("id",id);
                    future.complete(boardingPass);

                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    protected CompletableFuture<JsonObject> endRegister(SQLConnection conn, JsonObject objPayment, Integer createdBy, JsonObject prepaid, JsonArray boardingPasses, Integer cashOutId, Integer cashRegisterId, Double ivaPercent, Integer currencyId, Boolean isCredit, JsonObject creditCustomerData){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Double total = prepaid.getDouble("total_amount");
        Double iva = this.getIva(total, ivaPercent);
        Double paid = objPayment.getDouble("amount");
        Double paid_change = paid - total;

        JsonArray ticketArray = new JsonArray();
        //Tickets prepaid and the boarding_passes

        List<CompletableFuture<JsonObject>> ticketsTask = new ArrayList<>();

        ticketArray.add(new JsonObject());
        //Prepaid first
        final int numTickets = ticketArray.size();
        for(int i = 0; i < numTickets ; i++){
            ticketsTask.add(insertTicketsPrepaid(conn, cashOutId, prepaid.getInteger("id"), objPayment, cashRegisterId, createdBy, true, total, iva, paid, paid_change, ticketArray.getJsonObject(i), isCredit ));
        }

        /*final int numBoardingPasses = boardingPasses.size();

        for (int i = 0; i < numBoardingPasses; i++) {
            ticketArray.add(new JsonObject());
            ticketsTask.add(insertTicketsPrepaid(conn, cashOutId, boardingPasses.getJsonObject(i).getInteger("id"), objPayment, cashRegisterId, createdBy, false, 0.0,0.0,0.0,0.0, ticketArray.getJsonObject(i + 1), isCredit ));
        }*/

        CompletableFuture.allOf(ticketsTask.toArray(new CompletableFuture[ticketsTask.size()])).whenComplete(( st,  tError ) -> {
            try {
                if (tError != null) {
                    throw tError;
                }

                //TICKETS DETALLE
                this.insertTicketDetail(conn, ticketArray, createdBy ,prepaid).whenComplete((Boolean detailSucces , Throwable dError) -> {
                    try {
                        if(dError != null) {
                            throw dError;
                        }
                        if(isCredit) {
                            this.updateCustomerCredit(conn, createdBy, prepaid, creditCustomerData)
                                    .whenComplete((replyCustomer, errorCustomer) -> {
                                        try {
                                            if(errorCustomer != null) {
                                                throw new Exception(errorCustomer);
                                            }
                                            JsonObject result = new JsonObject().put("id", prepaid.getInteger("id"));
                                            result.put("tracking_code", prepaid.getString("reservation_code"));
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
                                pTasks.add(insertPaymentAndCashOutMove(conn, objPayment, ticketArray.getJsonObject(pNum), prepaid ,createdBy, cashOutId, cashRegisterId, currencyId));
                            }
                            CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pTasks.size()])).whenComplete((pSuccess, pError) -> {
                                try {
                                    if(pError != null) {
                                        throw pError;
                                    }
                                    JsonObject result = new JsonObject().put("id", prepaid.getInteger("id"));
                                    result.put("tracking_code", prepaid.getString("reservation_code"));
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

        return future;
    }

    private CompletableFuture<JsonObject> insertTicketsPrepaid(SQLConnection conn, Integer cashOutId, Integer idService, JsonObject objPaymnet, Integer cashRegisterId, Integer createdBy, Boolean isPrepaid, Double total, Double iva, Double paid, Double paidChange , JsonObject ticket, Boolean isCredit ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if(isPrepaid){
                ticket.put("prepaid_travel_id",idService);
            } else {
                ticket.put("boarding_pass_id", idService);
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

            if(isCredit) {
                ticket.put("action", "voucher");
            } else {
                ticket.put("action", "purchase");
            }

            ticket.put("created_at", sdfDataBase(new Date()));
            ticket.put("extra_charges",0);
            ticket.put("has_extras",0);

            String insert = this.generateGenericCreate("tickets", ticket);

            conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
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
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private Double getIva(Double amount, Double ivaPercent) {
        Double iva = 0.00;
        iva = amount - (amount /(1 + (ivaPercent/100)));
        return iva;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, JsonArray tickets, Integer createdBy, JsonObject prepaid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();
        JsonArray details = new JsonArray();

        try {
            for(int i = 0 ; i < tickets.size(); i++){
                JsonObject tick = tickets.getJsonObject(i);
                JsonObject detailTicket = new JsonObject();
                detailTicket.put("ticket_id", tick.getInteger("id"));
                if(tick.getInteger("prepaid_travel_id") != null) {
                    detailTicket.put("detail", "Paquete prepago");
                    JsonObject boardPasses = new JsonObject();
                    boardPasses.put("ticket_id", tick.getInteger("id"));
                    boardPasses.put("unit_price", 0.0);
                    boardPasses.put("amount", 0.0);
                    boardPasses.put("discount",0.0);
                    boardPasses.put("created_at", UtilsDate.sdfDataBase(new Date()));
                    boardPasses.put("created_by",createdBy);
                    boardPasses.put("quantity", prepaid.getInteger("total_tickets"));
                    boardPasses.put("detail", "Boletos prepago");

                    details.add(boardPasses);
                } else {
                    detailTicket.put("detail", "Boleto prepago perteneciente a paquete " + prepaid.getString("reservation_code"));
                }
                detailTicket.put("unit_price", tick.getDouble("total"));
                detailTicket.put("amount", tick.getDouble("total"));
                detailTicket.put("discount",0.0);
                detailTicket.put("created_at", UtilsDate.sdfDataBase(new Date()));
                detailTicket.put("created_by",createdBy);
                detailTicket.put("quantity", 1);

                details.add(detailTicket);
            }

            for(int z = 0; z < details.size(); z++) {
                inserts.add(this.generateGenericCreate("tickets_details", details.getJsonObject(z)));
            }

            conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                try {
                    if (replyInsert.failed()){
                        throw new Exception(replyInsert.cause());
                    }
                    future.complete(replyInsert.succeeded());

                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject objPayment, JsonObject ticket, JsonObject prepaid, Integer createdBy, Integer cashOutId, Integer cashRegisterId, Integer currencyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject payment = new JsonObject();
            JsonObject cashOutMove = new JsonObject();
            if(ticket.getInteger("prepaid_travel_id") != null) {
                payment.put("prepaid_travel_id",ticket.getInteger("prepaid_travel_id"));
            }

            payment.put("payment_method_id", objPayment.getInteger("id"));
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
                    if(cashOutId!=null) {
                        String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                        conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyMove) -> {
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
                    } else {
                        future.complete(payment);
                    }
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, Integer createdBy, JsonObject prepaid, JsonObject creditCustomerData) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = creditCustomerData.getDouble("available_credit");
            JsonObject customerObject = new JsonObject();
            double creditAvailable = actualCreditAvailable - prepaid.getDouble("total_amount");

            customerObject
                    .put(ID,creditCustomerData.getInteger(ID))
                    .put("credit_available", creditAvailable)
                    .put(UPDATED_BY,createdBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

            String updateCostumer = this.generateGenericUpdateString("customer", customerObject);

            conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCustomer) -> {
                try {
                    if(replyCustomer.failed()) {
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

    private  CompletableFuture<JsonObject> getOrderInfoWeb(SQLConnection conn, JsonObject body) {
        CompletableFuture future = new CompletableFuture();
        try {
            // Validar datos del body aqui
            if (body.getInteger("idPrepaidPackage") == null) {
                throw new UtilsValidation.PropertyValueException("idPrepaidPackage", INVALID_FORMAT);
            }
            if (body.getString("token_id") == null) {
                throw new UtilsValidation.PropertyValueException("token_id", INVALID_FORMAT);
            }

            JsonArray paments = body.getJsonArray("payments");
            if(paments==null || !paments.getJsonObject(0).containsKey("amount") || paments.size()>1 ){
                throw new UtilsValidation.PropertyValueException("payments", INVALID_FORMAT);
            }
            UtilsValidation.isGrater(paments.getJsonObject(0), "amount", -1);

            String customerName = body.getString("holder");
            Integer customerID = body.getJsonObject("customer").getInteger("id");
            body.put("id", body.getInteger("prepaid_travel_id")); // id del registro del paquete
            body.put("idCustomer", customerID);
            body.put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
            body.put(UPDATED_BY,context.<Integer>get(USER_ID));
            String token_id = body.getString("token_id"); // token_id Card

            List<CompletableFuture<JsonObject>> allTask = new ArrayList<>();
            allTask.add(this.getTotalAmountPackagePrepaid(body.getInteger("idPrepaidPackage")));
            allTask.add(this.getCustomerInfo(body.getJsonObject("customer").getInteger("id")));

            this.getTotalAmountPackagePrepaid(body.getInteger("idPrepaidPackage")).whenComplete((resultConfigPrepaid,ErrorConfigPrepaid)->{

                try {
                    if (ErrorConfigPrepaid != null) {
                        throw ErrorConfigPrepaid;
                    }
                    JsonObject resultAcum=resultConfigPrepaid;
                    String amountAcum = resultAcum.getValue("total_amount").toString();
                    String discountAcum = "0";
                    String totalAcum = resultAcum.getValue("total_amount").toString();

                    this.getCustomerInfo(body.getJsonObject("customer").getInteger("id")).whenComplete((resultCustomer,ErrorCustomer)->{

                        try {
                            if (ErrorCustomer != null) {
                                throw ErrorCustomer;
                            }

                            String currency_id = "";
                            String currency = "";
                            JsonObject datos = resultCustomer;
                            if (datos != null) {

                                if (customerID == null) {
                                    JsonObject first = resultCustomer;
                                    datos.put("name", customerName);
                                    datos.put("phone", first.getValue("phone"));
                                    datos.put("email", first.getValue("email"));
                                }

                                JSONObject customer = customerCreateJson(datos, token_id);

                                List<JsonObject> data = new ArrayList<>();

                                if (body.containsKey("reservation_code")) {
                                    resultAcum.put("reservation_code",String.valueOf((body.getString("reservation_code"))));
                                }
                                resultAcum.put("quantity",1);
                                data.add(resultAcum);

                                if (resultAcum.containsKey("currency_id")) {
                                    currency_id = String.valueOf((resultAcum.getInteger("currency_id")));
                                }
                                if (resultAcum.containsKey("abr")) {
                                    currency = String.valueOf((resultAcum.getString("abr")));
                                }

                                String order = orderCreatePackagePrepaid(data, customer, currency, token_id, totalAcum);
                                Double total = (Double.parseDouble(totalAcum));
                                body.put("currency",currency_id);
                                body.put("payment_method","card");
                                body.put("amount",total);
                                //body.put("reference",reference);
                                body.put("finalAmount",amountAcum);
                                body.put("finalDiscount",discountAcum);
                                //  body.put("ivaPercent",ivaPercent);
                                body.put("finalTotalAmount",totalAcum);
                                body.put("orderJson", order);

                                DeliveryOptions optionsInsertP = new DeliveryOptions()
                                        .addHeader(ACTION, SAVE_PAYMENT_PREPAID);
                                vertx.eventBus().send( conektaDBV.class.getSimpleName(), body, optionsInsertP,
                                        replySavePayment -> {
                                            try{
                                                if(replySavePayment.failed()){
                                                    throw new Exception(replySavePayment.cause());
                                                }
                                                Message<Object> resultInsert = replySavePayment.result();
                                                MultiMap headers = resultInsert.headers();
                                                if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                                    future.completeExceptionally(new Throwable("Error when paying"));

                                                } else {
                                                    future.complete(resultInsert.body());

                                                }

                                            }catch(Exception ex){
                                                ex.printStackTrace();
                                                future.cancel(true);
                                            }
                                        });
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    public CompletableFuture<JsonObject> getTotalAmountPackagePrepaid(Integer idPrepaidPackage) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            int id = idPrepaidPackage;
            JsonArray params = new JsonArray().add(id);
            dbClient.queryWithParams(QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_PACKAGE_PREPAID, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    ResultSet resultTickets = reply.result();
                    if (resultTickets.getNumRows() == 0) {
                        future.completeExceptionally(new Throwable("Tickets not found for boarding pass"));
                    } else {
                        JsonObject accumTickets = resultTickets.getRows().get(0);
                        future.complete(accumTickets);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        }catch  (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void findPrepaidTravel(Message<JsonObject> message) {
        this.startTransaction(message,conn -> {
            JsonObject body = message.body();

            String code  = body.getString("code");

            JsonArray params = new JsonArray().add(code);

            conn.queryWithParams(QUERY_GET_PACKAGE_TRAVEL, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultPrepaid = reply.result().getRows();
                    if(resultPrepaid.isEmpty()){
                        throw new Exception("Element not found");
                    }
                    JsonObject prepaid = resultPrepaid.get(0);
                    this.commit(conn, message,prepaid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    private void searchPassesById(Message<JsonObject> message) {
        this.startTransaction(message,conn -> {
            JsonObject body = message.body();

            Integer id = body.getInteger("id");

            JsonArray params = new JsonArray().add(id);

            conn.queryWithParams(QUERY_GET_BOARDING_PASSES_BY_PREPAID_PACKAGE_ID, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultPrepaid = reply.result().getRows();
                    if(resultPrepaid.isEmpty()){
                        throw new Exception("Element not found");
                    }
                    message.reply(new JsonArray(resultPrepaid));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    public static String remove1(String input) {
        // Cadena de caracteres original a sustituir.
        String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
        // Cadena de caracteres ASCII que reemplazarán los originales.
        String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
        String output = input;
        for (int i = 0; i < original.length(); i++) {
            // Reemplazamos los caracteres especiales.
            output = output.replace(original.charAt(i), ascii.charAt(i));
        }//for i
        return output;
    }

    public CompletableFuture<JsonObject> getCustomerInfo(Integer idCustomer) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            // JsonObject body = idCustomer;
            Integer id = idCustomer;

            String query = "select CONCAT(customer.first_name,' ',customer.last_name) as name, " +
                    "customer.conekta_id as 'idConekta' , customer.phone, customer.email, customer.created_at " +
                    /*"customer_billing_information.address as street, city.name as \"City\", state.name as \"State\", country.name as \"Country\", " +
                    "customer_billing_information.zip_code " +
                    */
                    "from customer ";
                    /*"right join customer_billing_information ON customer_billing_information.customer_id=customer.id " +
                    "right join city on city.id=customer_billing_information.city_id " +
                    "right join state on customer_billing_information.state_id=state.id " +
                    "right join country on customer_billing_information.country_id=country.id " +*/
            JsonArray params = new JsonArray();
            // IF id is null get general public customer
            if (id == null) {
                query += "where customer.email = ? AND customer.phone = ?;";
                params.add("noreply@allabordo.com");
                params.add("6688569071");
            } else {
                query += "where customer.id = ?";
                params.add(id);
            }
            dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.succeeded()) {
                        if (reply.result().getNumRows() > 0) {
                            JsonObject job = reply.result().getRows().get(0);
                            job.put("name", remove1(job.getString("name", "Sin nombre")));
                            job.put("City", remove1(job.getString("City", "Los Mochis")));
                            job.put("Country", remove1(job.getString("Country", "Mexico")));
                            job.put("street", remove1(job.getString("street", "Gabriel Leyva")));
                            job.put("zip_code", job.getInteger("zip_code", 81200));

                            future.complete(job);
                        } else {
                            future.complete(null);

                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }

            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }


    /**
     * Return a JSONObject to create a Customer in Conekta
     *
     * @param datos         JsonObject
     * @param token_Conekta String
     * @return JSONObject with the info to create a customer in Conekta
     * @throws ParseException
     */
    private JSONObject customerCreateJson(JsonObject datos, String token_Conekta) throws ParseException {

        // Parse String Date to Instant to insert in Conekta
        String s;
        String date = datos.getValue("created_at").toString();
        DateTimeFormatter format = DateTimeFormatter.ISO_INSTANT;
        s = getTimestamp(format.parse(date).toString());

        String name, phone, email, code, state, city, country, street;

        name = datos.getString("name");
        phone = datos.getString("phone");
        email = datos.getString("email");
        code = datos.getString("code");
        state = datos.getString("state");
        city = datos.getString("city");
        country = datos.getString("country");
        street = datos.getString("street");

        if (datos.containsKey("zip_code")) {
            if (datos.getValue("zip_code").toString().equals("")) {
                code = "";
            } else {
                code = String.valueOf(datos.getInteger("zip_code"));
            }
        } else {
            code = "";
        }

        if (datos.containsKey("State")) {
            if (datos.getValue("State") == null || datos.getString("State").equals("")) {
                state = "";
            } else {
                state = datos.getString("State");
            }
        } else {
            state = "";
        }

        if (datos.containsKey("street")) {
            if (datos.getValue("street") == null || datos.getString("street").equals("")) {
                street = "";
            } else {
                String[] streetSplit = datos.getString("street").split(",");
                street = streetSplit[0];
            }
        } else {
            street = "";
        }

        if (datos.containsKey("City")) {
            if (datos.getValue("City") == null || datos.getString("City").equals("")) {
                city = "";
            } else {
                city = datos.getString("City");
            }
        } else {
            city = "";
        }

        if (datos.containsKey("Country")) {
            if (datos.getValue("Country") == null || datos.getString("Country").equals("")) {
                country = "";
            } else {
                country = datos.getString("Country");
            }
        } else {
            country = "";
        }

        s = s.replace(",ISO", "");

        JSONObject asd = new JSONObject(
                "{" + "'name':  '" + name + "', " + "'email': '" + email + "'," + "'phone': '" + phone + "',"
                        // + "'plan_id': 'gold-plan',"
                        + "'corporate': false," + "'antifraud_info': {'paid_transactions': 0, " + "'account_created_at': " + s + ","
                        + "'first_paid_at': " + s + "}," + "'payment_sources': [{" + "'token_id': '" + token_Conekta
                        + "'," + "'type': 'card'" + "}]," + "'shipping_contacts': [{" + "'phone': '" + phone + "',"
                        + "'receiver': '" + name + "'," + "'between_streets': '" + street + "'," + "'address': {"
                        + "'street1': '" + street + "'," + "'street2': '" + street + "'," + "'city': '" + city + "',"
                        + "'state': '" + state + "'," + "'country': 'MX'," + "'postal_code': '" + code + "',"
                        + "'residential': true" + "}" + "}]" + "}");

        return asd;
    }

    /**
     * Return the Instant time in milliseconds of the a date in a String with format
     * "2018-08-31T09:15:00Z"
     *
     * @param data
     * @return
     */
    private String getTimestamp(String data) {
        data = data.replace("{", "");
        data = data.replace("[", "");
        data = data.replaceAll("]", "");
        data = data.replaceAll("}", "");

        // Esto va a causar problemas en street, el campo street contiene ','
        String[] dat = data.split(", ");

        for (String da : dat) {
            String[] d = da.split("=");
            if (d[0].equals("InstantSeconds")) {
                return d[1].replace(",ISO", "");
            }
        }
        return "";
    }

    /**
     *
     * @param items  List<JsonObject>
     * @param cus    Conekta.Customer info of the customer to create the Order and
     *               Charge
     * @param token  String contains the data of the card
     * @param amount String total to pay
     * @return A Order in conekta
     * @throws ErrorList
     * @throws Error
     */

    private String orderCreatePackagePrepaid(List<JsonObject> items, JSONObject cus, String currency, String token, String amount)
            throws ParseException {
        String lineItems = lineItemsCreatePackagePrepaid(items);
        JSONObject anti = cus.getJSONObject("antifraud_info");

        String cad = "{" + "'currency': '" + currency + "'," + "'metadata': {" + "    'test': true" + "},"
                + "'line_items': " + lineItems + "," + "'customer_info': {" + "'antifraud_info': " + anti.toString() + ","
                + "'email': '" + cus.getString("email") + "'," + "'name': '" + cus.getString("name") + "'," + "'phone': '" + cus.getString("phone") + "'"
                + "}," + "'charges': [{" + "    'payment_method': {" + "        'type': 'card',"
                + "        'token_id': '" + token + "'" + "    }, " + "    'amount': "
                + Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(amount) * 100,0)))) + "" + "}]" + "}";

        return cad;
    }
    public String lineItemsCreatePackagePrepaid(List<JsonObject> items) throws ParseException {
        String it = "";
        it = it.concat("[");


        for (JsonObject item : items) {
            it = it.concat("{");
            it = it.concat("'name': '" + item.getString("name") + "',");
            Integer unit_price = Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(item.getDouble("total_amount") * 100, 0))));
            it = it.concat("'unit_price': '" + unit_price + "',");
            it = it.concat("'quantity': '" + item.getInteger("quantity") + "',");
            it = it.concat("'antifraud_info': {");
            it = it.concat("'prepaid_travel_id': '" + item.getInteger("id") + "',");
            it = it.concat("'reservation_code': '" + item.getString("reservation_code") + "',");
            it = it.concat("'trip_id': '" + item.getInteger("id") + "'," );

            it = it.concat("'departs_at': 1628406000,");
            it = it.concat("'arrives_at': 1628409600,");
            it = it.concat("'ticket_class': 'economic',");
            it = it.concat("'seat_number': '23A',");
            it = it.concat("'origin': 'Mexico City',");
            it = it.concat("'destination': 'Torren',");
            it = it.concat("'passenger_type': 'adulto");
            it = it.concat("'}");
            it = it.concat("}");

            if (!items.get(items.size() - 1).equals(item)) {
                it = it.concat(",");
            }
        }
        it = it.concat("]");
        return it;
    }

    private void getTerminalDestinyByOriginDistance(Message<JsonObject> message) {
        try {
            JsonObject params = message.body();
            JsonArray paramsDistance = new JsonArray()
                    .add(params.getInteger("terminal_origin_id"))
                    .add(params.getDouble("max_km"));

            this.dbClient.queryWithParams(PrepaidTravelDBV.QUERY_DESTINY_BY_DISTANCE_AND_ORIGIN, paramsDistance, reply -> {
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()){
                        message.reply(new JsonArray());
                    }
                    message.reply(new JsonArray(results));
                }catch(Throwable t) {
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void reportSales(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String QUERY = QUERY_PREPAID_SALES_REPORT;
        boolean flagTotals = body.getBoolean("flag_totals");
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        JsonArray customers = body.getJsonArray("customer_array");
        Integer sellerId = body.getInteger("seller_name_id");
        String purchaseOrigin = body.getString("purchase_origin");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        Integer purchaseBranchofficeId = body.getInteger("purchase_branchoffice_id");
        params.add(initDate).add(endDate);

        if (body.getInteger("origin_branchoffice_id") != null || body.getInteger("destiny_branchoffice_id") != null) {
            // is query from /reports/travelsAccountant on admin website, return empty because filters doesnt match
            message.reply(new JsonArray());
        } else {
            if(sellerId != null) {
                params.add(sellerId);
                QUERY = QUERY.concat("AND ppt.seller_user_id = ? ");
            }

            if (purchaseOrigin != null) {
                params.add(purchaseOrigin);
                QUERY = QUERY.concat("AND ppt.purchase_origin = ? ");
                if (purchaseOrigin.equals("sucursal")) {
                    if (purchaseCityId != null) {
                        params.add(purchaseCityId);
                        QUERY = QUERY.concat("AND ci.id = ? ");
                    }
                    if (purchaseBranchofficeId != null) {
                        params.add(purchaseBranchofficeId);
                        QUERY = QUERY.concat("AND ppt.branchoffice_id = ? ");
                    }
                }
            }

            if(customers != null) {
                String concatQuery = "";
                QUERY = QUERY.concat("AND ppt.customer_id IN (");

                for(int i = 0; i <= customers.size(); i++) {
                    if(i < customers.size()) {
                        Integer customer_id = customers.getJsonObject(i).getInteger("id");
                        concatQuery += customer_id;
                    }

                    if(i < customers.size() - 1) {
                        concatQuery += ", ";
                    }
                }
                QUERY = QUERY.concat(concatQuery + ")");
            }

            QUERY = QUERY.concat(" GROUP BY ppt.reservation_code ").concat("ORDER BY ppt.created_at");

            if(!flagTotals) {
                Integer page = body.getInteger(PAGE);
                String QUERY_COUNT = "SELECT COUNT(*) AS count FROM ("+QUERY+") AS prepaid_sales_report;";
                List<Future> taskList = new ArrayList<>();

                if(page != null) {
                    Future f1 = Future.future();
                    this.dbClient.queryWithParams(QUERY_COUNT, params.copy(), f1.completer());
                    taskList.add(f1);
                    Integer limit = body.getInteger(LIMIT);
                    QUERY += " LIMIT ? OFFSET ? ";
                    params.add(limit).add((page - 1) * limit);
                }

                Future f2 = Future.future();
                this.dbClient.queryWithParams(QUERY, params, f2.completer());
                taskList.add(f2);

                CompositeFuture.all(taskList).setHandler(reply -> {
                    try{
                        if(reply.failed()) {
                            throw reply.cause();
                        }
                        JsonObject result = new JsonObject();
                        Integer index = taskList.size() == 1 ? 0 : 1;
                        List<JsonObject> passesList = reply.result().<ResultSet>resultAt(index).getRows();

                        if(passesList.isEmpty()) {
                            if(page != null) {
                                result.put("count", 0)
                                        .put(LIMIT, passesList.size())
                                        .put(RESULTS, passesList);
                                message.reply(result);
                            } else {
                                message.reply(new JsonArray(passesList));
                            }
                        } else {
                            Integer count = 0;
                            if(page != null) {
                                count = reply.result().<ResultSet>resultAt(0).getRows().get(0).getInteger("count");
                                result.put("count", count)
                                        .put("items", passesList.size())
                                        .put(RESULTS, passesList);
                                message.reply(result);
                            } else {
                                message.reply(new JsonArray(passesList));
                            }
                        }
                    } catch(Throwable t) {
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            } else {
                QUERY = QUERY_PREPAID_SALES_REPORT_TOTALS + QUERY + ") AS t;";

                this.dbClient.queryWithParams(QUERY, params, reply -> {
                    try {
                        if(reply.failed()) {
                            throw reply.cause();
                        }
                        List<JsonObject> passesList = reply.result().getRows();
                        message.reply(new JsonArray(passesList));
                    } catch(Throwable t) {
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            }
        }
    }

    private static final String QUERY_PREPAID_SALES_REPORT = "SELECT DISTINCT \n" +
            "ppt.created_at, ppt.id, ppt.reservation_code, ppt.total_tickets, ppt.payment_condition, ppt.total_amount,\n" +
            " IF(ISNULL(em.name), 'N/A', em.name) AS seller_name,\n" +
            " CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            " ppc.max_km,\n" +
            " b.name AS terminal_name\n" +
            " FROM prepaid_package_travel as ppt\n" +
            " LEFT JOIN users AS em ON em.id = ppt.seller_user_id\n" +
            " LEFT JOIN customer AS c ON c.id = ppt.customer_id\n" +
            " LEFT JOIN branchoffice AS b ON b.id = ppt.branchoffice_id\n" +
            " LEFT JOIN city ci ON b.city_id = ci.id\n" +
            " LEFT JOIN prepaid_package_config AS ppc ON ppc.id = ppt.prepaid_package_config_id\n" +
            " WHERE ppt.created_at BETWEEN ? AND ? AND ppt.prepaid_status != 2 \n";

    private static final String QUERY_PREPAID_SALES_REPORT_TOTALS = "SELECT SUM(IF(t.payment_condition = 'cash', t.total_amount, 0)) AS cash,\n" +
            "       SUM(IF(t.payment_condition = 'credit', t.total_amount, 0)) AS credit,\n" +
            "       COUNT(t.id) AS total\n" +
            "       FROM (";

    private static final String QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_PACKAGE_PREPAID = "SELECT pp.id, pp.money AS total_amount ,\n" +
            "pp.tickets_quantity as total_ticket,\n" +
            "c.abr,\n" +
            "c.id as currency_id,\n" +
            "pp.name\n" +
            "FROM prepaid_package_config AS pp \n" +
            "left join currency as c on  c.id=22\n" +
            "WHERE pp.id = ?;";

    private static final String QUERY_GET_PACKAGE_TRAVEL = "select \n" +
            "ppt.id,\n" +
            "ppt.reservation_code,\n" +
            "ppt.amount,\n" +
            "ppt.total_amount,\n" +
            "em.name as employee_name,\n" +
            "concat(c.first_name, ' ', c.last_name) as customer_name,\n" +
            "ppt.created_at,\n" +
            "ppt.customer_id,\n" +
            "ppt.seller_user_id,\n" +
            "ppt.branchoffice_id,\n" +
            "ppt.prepaid_status,\n" +
            "ppt.active_tickets,\n" +
            "ppt.used_tickets,\n" +
            "ppt.total_tickets,\n" +
            "ppt.prepaid_package_config_id, \n" +
            "ppt.payment_condition,\n" +
            "ppt.created_at,\n" +
            "ppt.iva,\n" +
            "ppt.purchase_origin,\n" +
            "ppt.debt\n" +
            "from prepaid_package_travel ppt\n" +
            "left join customer c ON ppt.customer_id = c.id \n" +
            "left join users AS em ON em.id = ppt.seller_user_id\n" +
            "WHERE ppt.reservation_code =  ? ";

    private static final String QUERY_GET_BOARDING_PASSES_BY_PREPAID_PACKAGE_ID = "SELECT \n" +
            "bp.id,\n" +
            "bp.boardingpass_status,\n" +
            "bp.reservation_code,\n" +
            "bp.terminal_origin_id,\n" +
            "bp.terminal_destiny_id,\n" +
            "bpp.id as passenger_id,\n" +
            "concat(bpp.first_name, ' ' , bpp.last_name) as passenger_name,\n" +
            "bo.prefix as 'origen',\n" +
            "bd.prefix as 'destiny',\n" +
            "bp.travel_date,\n" +
            "t.id AS ticket_id\n" +
            "FROM boarding_pass bp\n" +
            "LEFT JOIN prepaid_package_travel ppt ON bp.prepaid_id = ppt.id\n" +
            "LEFT JOIN boarding_pass_passenger bpp ON bp.principal_passenger_id = bpp.id\n" +
            "LEFT JOIN tickets t ON t.boarding_pass_id = bp.id\n" +
            "left join branchoffice bo ON bp.terminal_origin_id = bo.id\n" +
            "left join branchoffice bd ON bp.terminal_destiny_id = bd.id\n" +
            "WHERE ppt.id = ? ";

    private static final String QUERY_DESTINY_BY_DISTANCE_AND_ORIGIN = "SELECT ptd.*, bd.*\n" +
            " FROM package_terminals_distance AS ptd\n" +
            " LEFT JOIN branchoffice bd on ptd.terminal_destiny_id = bd.id\n" +
            " WHERE ptd.terminal_origin_id = ? AND ptd.distance_km <= ? and ptd.status = 1" +
            " AND bd.status= 1 AND bd.branch_office_type = 'T';";
}

