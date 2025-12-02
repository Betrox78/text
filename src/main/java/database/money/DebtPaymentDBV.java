package database.money;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class DebtPaymentDBV extends DBVerticle {
    public static final String ACTION_REGISTER = "DebtPaymentDBV.register";
    public static final String ACTION_MULTIPLE_PAYMENT = "DebtPaymentDBV.registerMultiplePayment";
    public static final String ACTION_GET_INI_END_DEBT = "DebtPaymentDBV.getIniEndDebt";

    @Override
    public String getTableName() {
        return "debt_payment";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_GET_INI_END_DEBT:
                this.getIniEndDebt(message);
                break;
            case ACTION_MULTIPLE_PAYMENT:
                this.registerMultiplePayment(message);
                break;
        }
    }
    private void registerMultiplePayment(Message<JsonObject> message){
        this.startTransaction(message, conn ->{
            try{
                JsonObject debt_payment = message.body().copy();
                JsonArray payments = (JsonArray) debt_payment.remove("payments");
                JsonObject debts = (JsonObject) debt_payment.remove("debts");
                JsonObject cashChange = (JsonObject) debt_payment.remove("cash_change");
                final Integer createdBy = debt_payment.getInteger(CREATED_BY);
                JsonObject customerCreditData = (JsonObject) debt_payment.remove("customer_credit_data");
                JsonArray services = (JsonArray) debt_payment.remove("services");
                Double totalDebt = customerCreditData.getDouble("total_debt");
                String paymentDate = debt_payment.getString("payment_date") != null && !debt_payment.getString("payment_date").isEmpty()
                        ? debt_payment.getString("payment_date")
                        : UtilsDate.sdfDataBase(new Date());
                Boolean isCash = false;
                //Double totalAmountServices = services.stream().map(val -> (JsonObject) val).mapToDouble(o -> o.getDouble("amount")).sum();
                //Double totalAmountPayments = payments.stream().map(val -> (JsonObject) val).mapToDouble(o -> o.getDouble("amount")).sum();

                BigDecimal totalAmountServices = services.stream()
                        .map(val -> (JsonObject) val)
                        .map(o -> BigDecimal.valueOf(o.getDouble("amount")))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalAmountPayments = payments.stream()
                        .map(val -> (JsonObject) val)
                        .map(o -> BigDecimal.valueOf(o.getDouble("amount")))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalAmountServices.compareTo(totalAmountPayments) > 0) {
                    throw new Exception("Totals payments is less than total services");
                }
                if (services == null || services.isEmpty())
                    throw new Exception("No services object was found");

                Double totalPayments = 0.0;
                final int pLen = payments == null ? 0 : payments.size();

                for (int i = 0; i < pLen; i++) {
                    JsonObject payment = payments.getJsonObject(i);
                    Double paymentAmount = payment.getDouble("amount");
                    if (paymentAmount == null || paymentAmount < 0.0) {
                        this.rollback(conn, new Throwable("Invalid payment amount: " + paymentAmount), message);
                        return;
                    }
                    totalPayments += UtilsMoney.round(paymentAmount, 2);
                    if(payment.getInteger("payment_method_id") == 1) isCash = true;
                }
                this.insertTicket(conn, totalPayments, cashChange, createdBy, debt_payment.getInteger(CASHOUT_ID), "income", isCash).whenComplete((JsonObject ticket, Throwable ticketError)->{
                    try{
                        if (ticketError != null) {
                            throw ticketError;
                        }
                        List<CompletableFuture<JsonObject>> dpTasks = new ArrayList<>();
                        final Double[] debtIni = {totalDebt};
                        final Integer[] iterator = {0};
                        final Double[] servicesTotalAmount = {0.0};

                        for (Object pay : payments) {
                            JsonObject payment = new JsonObject(String.valueOf(pay));
                            payment.put("ticket_id", ticket.getInteger("id"))
                                    .put(CREATED_BY, createdBy);
                            this.insertPaymentAndCashOutMove(conn, payment, debt_payment.getInteger(CASHOUT_ID)).whenComplete((JsonObject paymentResult, Throwable paymentError)->{
                            try{
                                if (paymentError != null) {
                                    throw paymentError;
                                }
                                Integer paymentId = paymentResult.getInteger("id") == null ? Integer.valueOf(0) : paymentResult.getInteger("id");
                                JsonArray debtsArray = new JsonArray();
                                if(iterator[0] == payments.size() - 1) {
                                    for (Object s : services) {
                                        JsonObject serviceObject = new JsonObject(String.valueOf(s));
                                        String service = serviceObject.getString("service");
                                        debt_payment.put("payment_id", paymentId);
                                        debt_payment.put("amount", paymentResult.getDouble("amount"));
                                        Double paymentAmount = serviceObject.getDouble("amount");
                                        if (debts.size() == 0) {
                                            throw new Exception("No debts availables");
                                        }
                                        for (Object d : debts.getJsonArray(service)) {
                                            JsonObject debtObject = new JsonObject(String.valueOf(d));
                                            if (paymentAmount > 0.0) {
                                                if (debtObject.getDouble("debt") == 0) continue;
                                                if (serviceObject.getString("code").equals(debtObject.getString("code"))) {
                                                    Double initialDebt = debtObject.getDouble("debt");
                                                    Double debtEnd = debtIni[0];
                                                    debtEnd -= serviceObject.getDouble("amount");
                                                    debtObject.put(UPDATED_BY, createdBy)
                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                    debtObject.put("debt", initialDebt - paymentAmount)
                                                            .put("debtPayment", paymentAmount);
                                                    paymentAmount -= paymentAmount;
                                                    String waybill = (String) debtObject.remove("waybill");
                                                    String code = (String) debtObject.remove("code");
                                                    JsonObject ticketDetail = new JsonObject()
                                                            .put("unit_price", initialDebt)
                                                            .put("ticket_id", ticket.getInteger("id"))
                                                            .put("amount", debtObject.getDouble("debtPayment"))
                                                            .put("quantity", debtObject.getDouble("debtPayment"))
                                                            .put("created_by", createdBy);
                                                    if (service.equals("parcel")) {
                                                        String parcelTicketDetail = "Carta Porte " + waybill + " " + code;
                                                        String documentId = (String) debtObject.remove("document_id");
                                                        if(documentId != null)
                                                            parcelTicketDetail = parcelTicketDetail.concat(" Fact: " + documentId);
                                                        ticketDetail.put("detail", parcelTicketDetail);
                                                    }
                                                    else if (service.equals("boarding_pass")) {
                                                        ticketDetail.put("detail", "Boletos " + code);
                                                    }
                                                    else if (service.equals("prepaid")) {
                                                        ticketDetail.put("detail", "Boletos Prepago " + code);
                                                    }
                                                    else if (service.equals("guiapp")) {
                                                        String parcelTicketDetail = "Guia PP " + code;
                                                        String documentId = (String) debtObject.remove("document_id");
                                                        if(documentId != null)
                                                            parcelTicketDetail = parcelTicketDetail.concat(" Fact: " + documentId);
                                                        ticketDetail.put("detail", parcelTicketDetail);
                                                    }
                                                    dpTasks.add(updateDebt(conn, debtObject, ticketDetail, paymentId, debt_payment.getInteger("customer_id"), debtIni[0], debtEnd, service, ticket.getInteger("id"), paymentDate));
                                                    debtIni[0] = debtEnd;
                                                    debtObject.put("waybill", waybill)
                                                            .put("code", code);
                                                }
                                            }
                                            debtsArray.add(debtObject);
                                        }

                                    }
                                }
                                debts.put("debts", debtsArray);
                                if (payments.size() == 1)
                                    servicesTotalAmount[0] += payment.getDouble("amount");
                                else if (payments.size() > 1 && payment.getInteger("payment_method_id") == 0)
                                    servicesTotalAmount[0] = payment.getDouble("amount");

                                CompletableFuture<Void> allUpdateDebts = CompletableFuture.allOf(dpTasks.toArray(new CompletableFuture[dpTasks.size()]));
                                Double finalServicesTotalAmount = servicesTotalAmount[0];
                                allUpdateDebts.whenComplete((replyUpdateDebt, updateDebtError) -> {
                                    try {
                                        if (updateDebtError != null) {
                                            throw new Exception(updateDebtError);
                                        }

                                        Double creditBalance = 0.0;
                                        if (debt_payment.getBoolean("has_credit_balance")) {
                                            creditBalance = debt_payment.getDouble("payment_difference");
                                        }
                                        this.updateCustomerCredit(conn, customerCreditData, debt_payment.getDouble("services_total_amount"), debt_payment.getInteger("customer_id"),
                                                createdBy, creditBalance, false, finalServicesTotalAmount, iterator[0] == payments.size() - 1)
                                                .whenComplete((replyCustomer, errorCustomer) -> {
                                                    try {
                                                        if (errorCustomer != null) {
                                                            throw new Exception(errorCustomer);
                                                        }

                                                        if(iterator[0] == payments.size() - 1) {
                                                            JsonObject result = new JsonObject();
                                                            result.put("payment_id", paymentId);
                                                            result.put("ticket_id", ticket.getInteger("id"));
                                                            this.commit(conn, message, result);
                                                        }

                                                        iterator[0]++;

                                                    } catch (Throwable t) {
                                                        t.printStackTrace();
                                                        this.rollback(conn, t, message);
                                                    }
                                                });

                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            }catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        }
                    }catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
        });
    }
    private void register(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                JsonObject debt_payment = message.body().copy();
                JsonArray payments = (JsonArray) debt_payment.remove("payments");
                JsonObject debts = (JsonObject) debt_payment.remove("debts");
                JsonObject cashChange = (JsonObject) debt_payment.remove("cash_change");
                final Integer createdBy = debt_payment.getInteger(CREATED_BY);
                JsonObject customerCreditData = (JsonObject) debt_payment.remove("customer_credit_data");
                JsonArray services = (JsonArray) debt_payment.remove("services");
                Double totalDebt = customerCreditData.getDouble("total_debt");
                Boolean payWithCredit = debt_payment.containsKey("pay_with_credit") ? debt_payment.getBoolean("pay_with_credit") : false;
                Boolean isCash = false;
                Double totalAmountServices = services.stream().map(val -> (JsonObject) val).mapToDouble(o -> o.getDouble("amount")).sum();
                Double totalAmountPayments = payments.stream().map(val -> (JsonObject) val).mapToDouble(o -> o.getDouble("amount")).sum();
                String paymentDate = debt_payment.getString("payment_date") != null && !debt_payment.getString("payment_date").isEmpty()
                        ? debt_payment.getString("payment_date")
                        : UtilsDate.sdfDataBase(new Date());

                if(totalAmountServices > totalAmountPayments){
                    throw new Exception("Totals payments is less than total services");
                }
                if (services == null || services.isEmpty())
                    throw new Exception("No services object was found");

                Double totalPayments = 0.0;
                final int pLen = payments == null ? 0 : payments.size();

                for (int i = 0; i < pLen; i++) {
                    JsonObject payment = payments.getJsonObject(i);
                    Double paymentAmount = payment.getDouble("amount");
                    if (paymentAmount == null || paymentAmount < 0.0) {
                        this.rollback(con, new Throwable("Invalid payment amount: " + paymentAmount), message);
                        return;
                    }
                    totalPayments += UtilsMoney.round(paymentAmount, 2);
                    if(payment.getInteger("payment_method_id") == 1) isCash = true;
                }

                //Insert Ticket
                this.insertTicket(con, totalPayments, cashChange, createdBy, debt_payment.getInteger(CASHOUT_ID), "income", isCash).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                    try {
                        if (ticketError != null) {
                            throw ticketError;
                        }

                        //Insert payments
                        List<CompletableFuture<JsonObject>> dpTasks = new ArrayList<>();
                        final Double[] debtIni = {totalDebt};
                        final Integer[] iterator = {0};
                        final Double[] servicesTotalAmount = {0.0};
                        for (Object pay : payments) {
                            JsonObject payment = new JsonObject(String.valueOf(pay));
                            payment.put("ticket_id", ticket.getInteger("id"))
                                    .put(CREATED_BY, createdBy);
                            this.insertPaymentAndCashOutMove(con, payment, debt_payment.getInteger(CASHOUT_ID)).whenComplete((JsonObject paymentResult, Throwable paymentError) -> {
                                try {
                                    if (paymentError != null) {
                                        throw paymentError;
                                    }

                                    Integer paymentId = paymentResult.getInteger("id") == null ? Integer.valueOf(0) : paymentResult.getInteger("id");

                                    JsonArray debtsArray = new JsonArray();
                                    if(iterator[0] == payments.size() - 1) {
                                        for (Object s : services) {
                                            JsonObject serviceObject = new JsonObject(String.valueOf(s));
                                            String service = serviceObject.getString("service");
                                            debt_payment.put("payment_id", paymentId);
                                            debt_payment.put("amount", paymentResult.getDouble("amount"));
                                            Double paymentAmount = serviceObject.getDouble("amount");
                                            if (debts.size() == 0) throw new Exception("No debts availables");
                                            for (Object d : debts.getJsonArray(service)) {
                                                JsonObject debtObject = new JsonObject(String.valueOf(d));
                                                if (paymentAmount > 0.0) {
                                                    if (debtObject.getDouble("debt") == 0) continue;
                                                    Double initialDebt = debtObject.getDouble("debt");
                                                    Double debtEnd = debtIni[0];
                                                    if (paymentAmount >= debtObject.getDouble("debt")) {
                                                        debtObject.put("debt", 0)
                                                                .put("debtPayment", initialDebt);
                                                        paymentAmount -= initialDebt;
                                                    } else {
                                                        debtObject.put("debt", initialDebt - paymentAmount)
                                                                .put("debtPayment", paymentAmount);
                                                        paymentAmount -= paymentAmount;
                                                    }

                                                    debtEnd -= debtObject.getDouble("debtPayment");
                                                    debtObject.put(UPDATED_BY, createdBy)
                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                                    String waybill = (String) debtObject.remove("waybill");
                                                    String code = (String) debtObject.remove("code");

                                                    JsonObject ticketDetail = new JsonObject()
                                                            .put("unit_price", initialDebt)
                                                            .put("ticket_id", ticket.getInteger("id"))
                                                            .put("amount", debtObject.getDouble("debtPayment"))
                                                            .put("quantity", debtObject.getDouble("debtPayment"))
                                                            .put("created_by", createdBy);

                                                    if (service.equals("parcel")) {
                                                        String parcelTicketDetail = "Carta Porte " + waybill + " " + code;
                                                        String documentId = (String) debtObject.remove("document_id");
                                                        if(documentId != null)
                                                            parcelTicketDetail = parcelTicketDetail.concat(" Fact: " + documentId);
                                                        ticketDetail.put("detail", parcelTicketDetail);
                                                    }
                                                    else if (service.equals("boarding_pass")) {
                                                        ticketDetail.put("detail", "Boletos " + code);
                                                    }
                                                    else if (service.equals("guiapp")) {
                                                        String parcelTicketDetail = "Guia PP " + code;
                                                        String documentId = (String) debtObject.remove("document_id");
                                                        if(documentId != null)
                                                            parcelTicketDetail = parcelTicketDetail.concat(" Fact: " + documentId);
                                                        ticketDetail.put("detail", parcelTicketDetail);
                                                    }
                                                    else if (service.equals("prepaid")) {
                                                        ticketDetail.put("detail", "Paquete prepago " + code);
                                                    }

                                                    dpTasks.add(updateDebt(con, debtObject, ticketDetail, paymentId, debt_payment.getInteger("customer_id"), debtIni[0], debtEnd, service, ticket.getInteger("id"), paymentDate));
                                                    debtIni[0] = debtEnd;
                                                    debtObject.put("waybill", waybill)
                                                            .put("code", code);
                                                }
                                                debtsArray.add(debtObject);
                                            }
                                        }
                                    }
                                    debts.put("debts", debtsArray);
                                    if (payments.size() == 1)
                                        servicesTotalAmount[0] += payment.getDouble("amount");
                                    else if (payments.size() > 1 && payment.getInteger("payment_method_id") == 0)
                                        servicesTotalAmount[0] = payment.getDouble("amount");

                                    CompletableFuture<Void> allUpdateDebts = CompletableFuture.allOf(dpTasks.toArray(new CompletableFuture[dpTasks.size()]));
                                    Double finalServicesTotalAmount = servicesTotalAmount[0];
                                    allUpdateDebts.whenComplete((replyUpdateDebt, updateDebtError) -> {
                                        try {
                                            if (updateDebtError != null) {
                                                throw new Exception(updateDebtError);
                                            }

                                            Double creditBalance = 0.0;
                                            if (debt_payment.getBoolean("has_credit_balance")) {
                                                creditBalance = debt_payment.getDouble("payment_difference");
                                            }
                                            this.updateCustomerCredit(con, customerCreditData, debt_payment.getDouble("services_total_amount"), debt_payment.getInteger("customer_id"),
                                                    createdBy, creditBalance, payWithCredit, finalServicesTotalAmount, iterator[0] == payments.size() - 1)
                                                    .whenComplete((replyCustomer, errorCustomer) -> {
                                                        try {
                                                            if (errorCustomer != null) {
                                                                throw new Exception(errorCustomer);
                                                            }

                                                            if(iterator[0] == payments.size() - 1) {
                                                                JsonObject result = new JsonObject();
                                                                result.put("ticket_id", ticket.getInteger("id"));
                                                                result.put("payment_id", paymentId);
                                                                this.commit(con, message, result);
                                                            }

                                                            iterator[0]++;

                                                        } catch (Throwable t) {
                                                            t.printStackTrace();
                                                            this.rollback(con, t, message);
                                                        }
                                                    });

                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                            this.rollback(con, t, message);
                                        }
                                    });
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(con, t, message);
                                }
                            });
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Double totalPayments, JsonObject cashChange, Integer createdBy, Integer cashOutId, String action, Boolean isCash) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject ticket = new JsonObject()
                .put("total", totalPayments)
                .put("cash_out_id", cashOutId)
                .put("created_by", createdBy)
                .put("ticket_code", UtilsID.generateID("T"));
        if (action != null) {
            ticket.put("action", action);
        }

        if (cashChange != null) {
            Double paid = cashChange.getDouble("paid");
            Double total = cashChange.getDouble("total");
            Double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
            Double difference_paid = UtilsMoney.round(paid - total, 2);

            ticket.put("paid", paid)
                    .put("paid_change", paid_change);

            if(isCash) {
                if (totalPayments < total) {
                    future.completeExceptionally(new Throwable("The payment " + total + " is greater than the total " + totalPayments));
                } else if (totalPayments > total) {
                    future.completeExceptionally(new Throwable("The payment " + total + " is lower than the total " + totalPayments));
                } else if (paid_change > difference_paid) {
                    future.completeExceptionally(new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")"));
                } else if (paid_change < difference_paid) {
                    future.completeExceptionally(new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")"));
                }
            }

        } else {
            ticket.put("paid", totalPayments);
            ticket.put("paid_change", 0.0);
        }

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
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, JsonObject ticketDetail) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("tickets_details", ticketDetail);
        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }
                future.complete(reply.succeeded());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer cashOutId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if(payment.getInteger("payment_method_id") != 0) {
            PaymentDBV objPayment = new PaymentDBV();
            if (cashOutId == null) {
                objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        payment.put("id", resultPayment.getInteger("id"));
                        future.complete(payment);
                    }
                });
            } else {
                JsonObject cashOutMove = new JsonObject()
                        .put("quantity", payment.getDouble("amount"))
                        .put("move_type", "0")
                        .put(CASHOUT_ID, cashOutId)
                        .put(CREATED_BY, payment.getInteger(CREATED_BY));
                objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        payment.put("id", resultPayment.getInteger("id"));
                        cashOutMove.put("payment_id", resultPayment.getInteger("id"));
                        String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                        conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyMove) -> {
                            try {
                                if (replyMove.failed()) {
                                    throw new Exception(replyMove.cause());
                                }
                                future.complete(payment);

                            } catch (Throwable t) {
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });
                    }
                });
            }
        }
        else{
            future.complete(payment);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateDebt(SQLConnection conn, JsonObject Debt, JsonObject ticketDetail, Integer paymentId, Integer customerId, Double debtIni, Double debtEnd, String service, Integer ticketId, String paymentDate) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Double amount = (Double) Debt.remove("debtPayment");
        String tableName = service.equals("parcel") ? "parcels" : service.equals("guiapp") ? "parcels_prepaid": service.equals("prepaid") ? "prepaid_package_travel" : "boarding_pass";
        GenericQuery update = this.generateGenericUpdate(tableName, Debt);
        conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> replyUpdate) -> {
            try {
                if (replyUpdate.failed()) {
                    throw new Exception(replyUpdate.cause());
                }
                this.insertTicketDetail(conn, ticketDetail).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                    try {
                        if (dError != null) {
                            throw dError;
                        }

                        if(service.equals("parcel")) Debt.put("parcel_id", Debt.getInteger("id"));
                        if (service.equals("guiapp")) Debt.put("parcel_prepaid_id", Debt.getInteger("id"));
                        if(service.equals("prepaid")) Debt.put("prepaid_travel_id", Debt.getInteger("id"));
                        else Debt.put("boarding_pass_id", Debt.getInteger("id"));

                        Debt.put("amount", amount)
                                .put("created_by", Debt.getInteger("updated_by"))
                                .put("payment_id", paymentId)
                                .put("ticket_id", ticketId)
                                .put("customer_id", customerId)
                                .put("debt_ini", debtIni)
                                .put("debt_end", debtEnd)
                                .put("payment_date", paymentDate);
                        Debt.remove("id");
                        Debt.remove("debt");
                        Debt.remove("updated_by");
                        Debt.remove("waybill");
                        Debt.remove("code");

                        GenericQuery debtPaymentCreate = this.generateGenericCreate(Debt);
                        conn.updateWithParams(debtPaymentCreate.getQuery(), debtPaymentCreate.getParams(), debtPaymentReply -> {
                            try {
                                if (debtPaymentReply.failed()) {
                                    throw new Exception(debtPaymentReply.cause());
                                }

                                future.complete(new JsonObject().put("status", "ok"));
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

            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private void getIniEndDebt(Message<JsonObject> message) {
        JsonObject body = message.body();
        String ticketId = body.getString("ticket_id");
        JsonArray params = new JsonArray().add(ticketId).add(ticketId).add(ticketId);
        dbClient.queryWithParams(QUERY_GET_INI_END_DEBT, params, (reply) -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }

                List<JsonObject> debts = reply.result().getRows();
                if(debts.size() > 0) {
                    JsonObject debt = debts.get(0);
                    if (debt.getValue("debt_ini") == null && debt.getValue("debt_end") == null)
                        message.reply(new JsonObject());
                    else message.reply(debt);
                } else {
                    message.reply(new JsonObject());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
        });
    }

    private CompletableFuture<JsonObject> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt,
                                                            Integer customerId, Integer createdBy, Double paymentDifference,
                                                            Boolean payWithCredit, Double servicesCreditAmount, Boolean lastIteration) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if(lastIteration) {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            Double actualCreditBalance = customerCreditData.getDouble("credit_balance");
            Double creditBalance = actualCreditBalance + paymentDifference;
            Double creditAvailable = actualCreditAvailable + debt;
            if (payWithCredit) {
                creditBalance = actualCreditBalance - servicesCreditAmount;
            }
            if (creditAvailable > customerCreditData.getDouble("credit_limit"))
                creditAvailable = customerCreditData.getDouble("credit_limit");

            JsonObject customerObject = new JsonObject()
                    .put(ID, customerId)
                    .put("credit_available", creditAvailable)
                    .put("credit_balance", creditBalance)
                    .put(UPDATED_BY, createdBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            String updateCostumer = this.generateGenericUpdateString("customer", customerObject);
            conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCustomer) -> {
                try {
                    if (replyCustomer.failed()) {
                        throw replyCustomer.cause();
                    }
                    future.complete(customerObject);
                } catch (Throwable t) {
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        }
        else{
            future.complete(customerCreditData);
        }
        return future;
    }

    private static final String QUERY_GET_INI_END_DEBT = "SELECT \n" +
            "c.credit_limit,\n" +
            "c.credit_available,\n" +
            "c.credit_balance,\n" +
            "concat_ws(' ', c.first_name, c.last_name) customer,\n" +
            "(select debt_ini\n" +
            "from debt_payment\n" +
            "where ticket_id = ? order by id limit 1) debt_ini,\n" +
            "(select debt_end\n" +
            "from debt_payment \n" +
            "where ticket_id = ? order by id desc limit 1) debt_end\n" +
            "from debt_payment dp\n" +
            "inner join customer c on c.id = dp.customer_id\n" +
            "where dp.ticket_id = ?\n" +
            "limit 1;";
}
