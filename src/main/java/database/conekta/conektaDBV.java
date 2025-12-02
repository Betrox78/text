/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.conekta;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.e_wallet.EwalletDBV;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import io.conekta.Conekta;
import io.conekta.Error;
import io.conekta.ErrorList;
import io.conekta.Order;
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

import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import service.commons.Constants;
import service.commons.MailVerticle;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;
import static database.boardingpass.BoardingPassDBV.*;
import static database.promos.CustomersPromosDBV.PROMO_ID;
import static database.promos.PromosDBV.*;
import static service.commons.Constants.*;
import static service.commons.Constants.DISCOUNT;
import static service.commons.Constants.TICKET_TYPE;

/**
 *
 * @author Saul
 */
public class conektaDBV extends DBVerticle{
    public static final String GET_CUSTOMER_INFO = "conektaDBV.getCustomerInfo";
    public static final String GET_ORDER_INFO = "conektaDBV.getOrderInfo";
    public static final String SAVE_IDCONEKTA = "conektaDBV.saveIdConekta";
    public static final String GET_CITIES_NAME = "conektaDBV.getCitiesName";
    public static final String SAVE_IDORDER = "conektaDBV.saveIdOrder";
    public static final String SAVE_PAYMENT = "conektaDBV.endPayment";
    public static final String GET_TOTAL_AMOUNT = "conektaDBV.getTotalAmount";
    public static final String SAVE_PAYMENT_PREPAID = "conektaDBV.endPaymentWebPrepaid";
    public static final String SAVE_WALLET_RECHARGE = "conektaDBV.saveWalletRecharge";
    @Override
    public String getTableName() {
        return "";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Conekta.setApiVerion("2.0.0");
        Conekta.setApiKey(config().getString("conekta_api_key"));
        super.start(startFuture);
    }

    @Override
    protected void onMessage(Message<JsonObject> message){
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action){
            case GET_CUSTOMER_INFO:
                this.getCustomerInfo(message);
                break;
            case GET_ORDER_INFO:
                this.getOrderInfo(message);
                break;
            case SAVE_IDCONEKTA:
                this.saveIdConekta(message);
                break;
            case GET_CITIES_NAME:
                this.getCitiesName(message);
                break;
            case SAVE_IDORDER:
                this.saveIdOrder(message);
                break;
            case SAVE_PAYMENT:
                this.endPayment(message);
                break;
            case GET_TOTAL_AMOUNT:
                this.getTotalAmount(message);
                break;
            case SAVE_PAYMENT_PREPAID:
                this.endPaymentWebPrepaid(message);
                break;
            case SAVE_WALLET_RECHARGE:
                this.saveWalletRecharge(message);
                break;
        }
    }
           
    private void endPayment(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            JsonArray payments = (JsonArray) body.remove("payments");
            //data to update Boarding Pass
            final int boardingPassId = body.getInteger("id");
            Double ivaPercent = body.getDouble("ivaPercent");

            int expireOpenTicketsAfter = (int) body.remove("expire_open_tickets_after");

            final String currency_id = body.getString("currency");
            final String exchange = body.getString("exchange_id");
            
            final JSONObject orderJson = new JSONObject(body.getString("orderJson"));

            final boolean flagPromo = (boolean) body.remove(FLAG_PROMO);
            final JsonObject promoDiscount = (JsonObject) body.remove(DISCOUNT);
            final int updatedBy = body.getInteger(UPDATED_BY);

            this.getBoardingPassById(conn, boardingPassId).whenComplete((JsonObject boardingPass, Throwable error) -> {
                try {
                    if(error != null){
                        throw error;
                    }
                    Integer boardingPassStatus = boardingPass.getInteger(BOARDINGPASS_STATUS);
                    String createdAt = boardingPass.getString(CREATED_AT);
                    String ticketType = boardingPass.getString(TICKET_TYPE);

                    if (this.validateBoardingPassStatusRegister(boardingPassStatus)){

                        this.getTotalAmountTicketsForBoardingPassById(conn, boardingPassId).whenComplete((JsonObject tickets, Throwable terror) -> {
                            try {
                                if (terror != null) {
                                    throw terror;
                                }

                                Double amount = tickets.getDouble("amount");
                                Double discount = tickets.getDouble("discount");
                                Double totalAmount = tickets.getDouble("total_amount");

                                this.getBoardingPassTicketsList(conn, boardingPassId).whenComplete((resultBPT, errorBPT) -> {
                                    try {
                                        if (errorBPT != null) {
                                            throw errorBPT;
                                        }

                                        JsonObject bodyService = new JsonObject()
                                                .put("id", boardingPassId)
                                                .put("boardingpass_status", 1)
                                                .put("amount", amount)
                                                .put("discount", discount)
                                                .put("total_amount", totalAmount)
                                                .put("updated_by", updatedBy)
                                                .put("updated_at", UtilsDate.sdfDataBase(new Date()));

                                        JsonObject bodyPromo = new JsonObject()
                                                .put(USER_ID, updatedBy)
                                                .put(FLAG_USER_PROMO, false)
                                                .put(DISCOUNT, promoDiscount)
                                                .put(SERVICE, SERVICES.boardingpass)
                                                .put(BODY_SERVICE, bodyService)
                                                .put(PRODUCTS, resultBPT)
                                                .put(OTHER_PRODUCTS, new JsonArray())
                                                .put(FLAG_PROMO, flagPromo);
                                        vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                            try {
                                                if(replyPromos.failed()) {
                                                    throw replyPromos.cause();
                                                }
                                                JsonObject resultApplyDiscount = replyPromos.result().body();
                                                JsonObject service = resultApplyDiscount.getJsonObject(PromosDBV.SERVICE);
                                                JsonArray products = resultApplyDiscount.getJsonArray(PromosDBV.PRODUCTS);
                                                JsonObject productsCopy = new JsonObject();
                                                String ticketClass = orderJson.getJSONArray("line_items").getJSONObject(0).getJSONObject("antifraud_info").getString("ticket_class");
                                                if(ticketClass.equals("abierto_redondo")){
                                                    for(int x = 0; x < products.size(); x++){
                                                        Double total;
                                                        JsonObject items = products.getJsonObject(x);
                                                        String item = items.getInteger("boarding_pass_passenger_id").toString();
                                                        total = productsCopy.containsKey(item) ? productsCopy.getDouble(item) + items.getDouble("total_amount") : items.getDouble("total_amount");
                                                        productsCopy.put(item , total);
                                                    }
                                                    int countItems = 0;
                                                     for(Iterator i = productsCopy.iterator(); i.hasNext();){
                                                         Map.Entry<String, Double> finalTotalAmount = (Map.Entry<String, Double>) i.next();
                                                         String totalLineItem = finalTotalAmount.getValue().toString();
                                                         orderJson.getJSONArray("line_items").getJSONObject(countItems).put("unit_price", Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(String.valueOf(totalLineItem)) * 100,0)))));
                                                         countItems++;
                                                     }
                                                }else{
                                                    for(int i = 0; i < orderJson.getJSONArray("line_items").length(); i++){
                                                        String totalLineItem = products.getJsonObject(i).getDouble("total_amount").toString();
                                                        orderJson.getJSONArray("line_items").getJSONObject(i).put("unit_price", Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(String.valueOf(totalLineItem)) * 100,0)))));
                                                    }
                                                }


                                                JsonObject discountObj = resultApplyDiscount.getJsonObject(DISCOUNT);
                                                final Integer promoId = discountObj != null ? discountObj.getInteger(ID) : null;
                                                double innerAmount = service.getDouble(AMOUNT);
                                                double innerDiscount = service.getDouble(DISCOUNT);
                                                double innerTotalAmount = UtilsMoney.round(service.getDouble(TOTAL_AMOUNT), 2);
                                                final Double iva = UtilsMoney.round(this.getIva(innerTotalAmount, ivaPercent), 2);

                                                // Insert payments
                                                Double totalPayments = 0.0;
                                                final int pLen = payments.size();
                                                for (int i = 0; i < pLen; i++) {
                                                    JsonObject payment = payments.getJsonObject(i);
                                                    Double paymentAmount = payment.getDouble("amount");
                                                    if (paymentAmount == null || paymentAmount < 0.0) {
                                                        throw new Exception("Invalid payment amount: " + paymentAmount);
                                                    }
                                                    totalPayments += paymentAmount;
                                                }
                                                if (totalPayments > innerTotalAmount) {
                                                    throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
                                                }
                                                if (totalPayments < innerTotalAmount) {
                                                    throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
                                                }

                                                if (innerTotalAmount == 0) {
                                                    this.updateBoardingPass(conn, createdAt, ticketType, expireOpenTicketsAfter, updatedBy, boardingPassId, promoId, innerAmount, innerDiscount, innerTotalAmount, iva, null).whenComplete((resultUpdate, errorUpdate) -> {
                                                        try {
                                                            if (errorUpdate != null){
                                                                throw new Exception(errorUpdate);
                                                            }

                                                            this.commit(conn, message, resultUpdate);
                                                            this.doSendBoardingPassEmail(boardingPassId, null);

                                                        } catch (Exception e){
                                                            e.printStackTrace();
                                                            this.rollback(conn, e, message);
                                                        }
                                                    });
                                                } else {
                                                    if (innerTotalAmount < 3){
                                                        throw new Exception("Total amount must be major or equals to 3");
                                                    }
                                                    this.vertx.executeBlocking((Future<Order> future) -> {
                                                        try {
                                                            //Pago en Conekta
                                                            orderJson.getJSONArray("charges").getJSONObject(0).put("amount", Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(String.valueOf(innerTotalAmount)) * 100,0)))));
                                                            System.out.println("orderJson");
                                                            System.out.println(orderJson);
                                                            Order order = Order.create(orderJson);
                                                            future.complete(order);
                                                        } catch (ErrorList ex) {
                                                            ex.details.forEach(e -> System.out.println(e.message));
                                                            future.fail(new Exception("CONEKTA: " + ex.details.get(0).message));
                                                        } catch (Error ex) {
                                                            future.fail(new Exception("CONEKTA: " + ex.message));
                                                        }
                                                    }, res -> {
                                                        try {
                                                            if (res.failed()) {
                                                                throw new Exception(res.cause());
                                                            }

                                                            Order order = res.result();
                                                            final String conektaOrderId = order.id;
                                                            String referencePre = "";
                                                            String o = order.charges.get(0).toString();
                                                            int index = o.indexOf("last4=");
                                                            int index_name = o.indexOf("name=");
                                                            int index_exp = o.indexOf("exp_month");
                                                            int index_type = o.indexOf("type=");
                                                            int index_brand = o.indexOf("brand=");
                                                            int index_auth = o.indexOf("auth_code=");

                                                            String type = o.substring(index_type + 5, index_brand - 2);

                                                            referencePre += "Name " + o.substring(index_name + 5, index_exp - 2) + ", ";
                                                            referencePre += "last4=" + o.substring(index + 6, index + 10) + ", ";
                                                            referencePre += "type=" + type + ", ";
                                                            referencePre += "brand=" + o.substring(index_brand + 6, index_auth - 2) + " ";

                                                            final String reference = referencePre;

                                                            this.updateBoardingPass(conn, createdAt, ticketType, expireOpenTicketsAfter, updatedBy, boardingPassId, promoId, innerAmount,
                                                                    innerDiscount, innerTotalAmount, iva, conektaOrderId).whenComplete((resultUpdate, errorUpdate) -> {
                                                                try {
                                                                    if (errorUpdate != null) {
                                                                        throw new Exception(errorUpdate);
                                                                    }

                                                                    // Insert ticket
                                                                    this.insertTicket(conn , innerTotalAmount, ivaPercent, boardingPassId, "boarding_pass").whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                                        try {
                                                                            if (ticketError != null){
                                                                                throw ticketError;
                                                                            }
                                                                            // Insert ticket detail
                                                                            this.insertTicketDetail(conn, boardingPassId, ticket.getInteger("id"), true,0.0).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                                try {
                                                                                    if (dError != null) {
                                                                                        throw dError;
                                                                                    }

                                                                                    this.savePayment(conn, boardingPassId,currency_id, type, innerTotalAmount, reference, exchange, ticket.getInteger("id"), "boarding_pass")
                                                                                            .whenComplete((Integer paymentId, Throwable Error)->{
                                                                                                try {
                                                                                                    if (Error != null) {
                                                                                                        throw Error;
                                                                                                    }

                                                                                                    JsonObject referralObj = new JsonObject()
                                                                                                            .put("customer_id", body.getInteger("idCustomer"))
                                                                                                            .put("total_amount", innerTotalAmount)
                                                                                                            .put("bonus_referral", body.getDouble("bonus_referral"));

                                                                                                    EwalletDBV eWalletDBV = new EwalletDBV();
                                                                                                    eWalletDBV.verifyReferral(conn, referralObj).whenComplete((Boolean resultReferral, Throwable ErrorReferral)->{
                                                                                                        try {
                                                                                                            if(ErrorReferral != null) {
                                                                                                                throw ErrorReferral;
                                                                                                            }

                                                                                                            JsonObject bonificationObj = new JsonObject()
                                                                                                                    .put("wallet_type", "bonus")
                                                                                                                    .put("bonus_bp", body.getDouble("bonus_bp"))
                                                                                                                    .put("service_type", "boarding_pass")
                                                                                                                    .put("customer_id", body.getInteger("idCustomer"))
                                                                                                                    .put("service_amount", innerTotalAmount);

                                                                                                            eWalletDBV.calculateBonificationByService(conn, bonificationObj)
                                                                                                                    .whenComplete((JsonObject resultBonification, Throwable ErrorBonification)->{
                                                                                                                        try {
                                                                                                                            if(ErrorBonification != null) {
                                                                                                                                throw ErrorBonification;
                                                                                                                            }

                                                                                                                            resultUpdate.put("e_wallet_bonification", resultBonification);
                                                                                                                            this.commit(conn, message, resultUpdate);

                                                                                                                            JsonObject walletDataMove = new JsonObject()
                                                                                                                                    .put("wallet_type", "bonus")
                                                                                                                                    .put("service_type", "boarding_pass")
                                                                                                                                    .put("customer_id", body.getInteger("idCustomer"))
                                                                                                                                    .put("service_amount", innerTotalAmount)
                                                                                                                                    .put("payment_id", paymentId);

                                                                                                                            DeliveryOptions optionsWallet = new DeliveryOptions()
                                                                                                                                    .addHeader(ACTION, EwalletDBV.ACTION_REGISTER_WALLET_MOVE_SERVICE);
                                                                                                                            this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), walletDataMove, optionsWallet, replyWalletMove -> {
                                                                                                                                if (replyWalletMove.failed()) {
                                                                                                                                    replyWalletMove.cause().printStackTrace();
                                                                                                                                } else {
                                                                                                                                    this.doSendBoardingPassEmail(boardingPassId, resultBonification);
                                                                                                                                }
                                                                                                                            });
                                                                                                                        } catch(Throwable t) {
                                                                                                                            t.printStackTrace();
                                                                                                                            this.rollback(conn, t, message);
                                                                                                                        }
                                                                                                            });
                                                                                                        } catch(Throwable t) {
                                                                                                            t.printStackTrace();
                                                                                                            this.rollback(conn, t, message);
                                                                                                        }
                                                                                                    });
                                                                                                } catch (Throwable t){
                                                                                                    t.printStackTrace();
                                                                                                    this.rollback(conn ,t , message);
                                                                                                }
                                                                                            });
                                                                                } catch (Throwable t){
                                                                                    t.printStackTrace();
                                                                                    this.rollback(conn, t, message);
                                                                                }
                                                                            });
                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            this.rollback(conn, t, message);
                                                                        }
                                                                    });
                                                                } catch (Exception e){
                                                                    e.printStackTrace();
                                                                    this.rollback(conn, e, message);
                                                                }
                                                            });
                                                        } catch (Exception ex) {
                                                            ex.printStackTrace();
                                                            this.rollback(conn, ex, message);
                                                        }
                                                    });
                                                }
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private void saveWalletRecharge(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            Double ivaPercent = body.getDouble("ivaPercent");
            final String currency_id = body.getString("currency");
            final String exchange = body.getString("exchange_id");
            final JSONObject orderJson = new JSONObject(body.getString("orderJson"));

            try {
                for(int i = 0; i < orderJson.getJSONArray("line_items").length(); i++) {
                    String totalLineItem = body.getDouble("finalTotalAmount").toString();
                    orderJson.getJSONArray("line_items").getJSONObject(i).put("unit_price", Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(String.valueOf(totalLineItem)) * 100,0)))));
                }
                final Double iva = UtilsMoney.round(this.getIva(body.getDouble("finalTotalAmount"), ivaPercent), 2);

                this.vertx.executeBlocking((Future<Order> future) -> {
                    try {
                        //Pago en Conekta
                        orderJson.getJSONArray("charges").getJSONObject(0).put("amount",
                                Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(String.valueOf(body.getDouble("finalTotalAmount"))) * 100,0)))));
                        Order order = Order.create(orderJson);
                        System.out.println("orderJson");
                        System.out.println(orderJson);
                        future.complete(order);
                    } catch (ErrorList ex) {
                        ex.details.forEach(e -> System.out.println(e.message));
                        future.fail(new Exception("CONEKTA: " + ex.details.get(0).message));
                    } catch (Error ex) {
                        future.fail(new Exception("CONEKTA: " + ex.message));
                    }
                }, res -> {
                    try {
                        if (res.failed()) {
                            throw new Exception(res.cause());
                        }

                        Order order = res.result();
                        final String conektaOrderId = order.id;
                        String referencePre = "";
                        String o = order.charges.get(0).toString();
                        int index = o.indexOf("last4=");
                        int index_name = o.indexOf("name=");
                        int index_exp = o.indexOf("exp_month");
                        int index_type = o.indexOf("type=");
                        int index_brand = o.indexOf("brand=");
                        int index_auth = o.indexOf("auth_code=");

                        String type = o.substring(index_type + 5, index_brand - 2);

                        referencePre += "Name " + o.substring(index_name + 5, index_exp - 2) + ", ";
                        referencePre += "last4=" + o.substring(index + 6, index + 10) + ", ";
                        referencePre += "type=" + type + ", ";
                        referencePre += "brand=" + o.substring(index_brand + 6, index_auth - 2) + " ";

                        final String reference = referencePre;
                        EwalletDBV eWalletDBV = new EwalletDBV();

                        JsonObject walletRechargeData = new JsonObject()
                                .put("e_wallet_id", body.getJsonObject("wallet").getInteger("id"))
                                .put("code", UtilsID.generateRechargeID("RE"))
                                .put("purchase_origin", body.getString("purchase_origin"))
                                .put("description", reference)
                                .put("amount", body.getDouble("finalTotalAmount"))
                                .put("total_amount", body.getDouble("finalTotalAmount"))
                                .put("bonification", body.getJsonObject("totals_recharge").getDouble("bonus_amount"))
                                .put("payment_status", "paid")
                                .put("conekta_order_id", conektaOrderId)
                                .put("totals_recharge", body.getJsonObject("totals_recharge"));

                        eWalletDBV.registerWalletRecharge(conn, walletRechargeData).whenComplete((JsonObject resultRecharge, Throwable ErrorRecharge)->{
                            try {
                                if(ErrorRecharge != null) {
                                    throw ErrorRecharge;
                                }

                                Integer rechargeId = resultRecharge.getInteger("id");
                                this.insertTicket(conn , body.getDouble("finalTotalAmount"), ivaPercent, rechargeId,"wallet")
                                        .whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                            try {
                                                if (ticketError != null){
                                                    throw ticketError;
                                                }
                                                // Insert ticket detail
                                                JsonObject detailTicketObj = new JsonObject()
                                                        .put("quantity", 1)
                                                        .put("detail", "Recarga de saldo en monedero electrónico")
                                                        .put("unit_price", body.getDouble("finalTotalAmount"))
                                                        .put(DISCOUNT, 0.0)
                                                        .put("amount", body.getDouble("finalTotalAmount"));

                                                this.insertTicketDetailRecharge(conn, ticket.getInteger("id"), detailTicketObj).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                    try {
                                                        if (dError != null) {
                                                            throw dError;
                                                        }
                                                        this.savePayment(conn, rechargeId, currency_id, type, body.getDouble("finalTotalAmount"), reference, exchange, ticket.getInteger("id"), "wallet")
                                                                .whenComplete((Integer paymentId, Throwable Error)->{
                                                                    try {
                                                                        if (Error != null) {
                                                                            throw Error;
                                                                        }

                                                                        // ACTUALIZAR SALDO DE RECARGAS
                                                                        JsonObject walletUpdateObj = new JsonObject()
                                                                                .put("wallet", body.getJsonObject("wallet"))
                                                                                .put("wallet_type", "wallet_recharge")
                                                                                .put("amount", body.getDouble("finalTotalAmount"));

                                                                        eWalletDBV.updateWalletAmount(conn, walletUpdateObj).whenComplete((Boolean replyWalletUpdate, Throwable ErrorWalletUpdate) -> {
                                                                            try {
                                                                                if(ErrorWalletUpdate != null) {
                                                                                    throw ErrorWalletUpdate;
                                                                                }

                                                                                // ACTUALIZAR SALDO DE MONEDERO
                                                                                JsonObject walletBonusUpdateObj = new JsonObject()
                                                                                        .put("wallet", body.getJsonObject("wallet"))
                                                                                        .put("wallet_type", "bonus")
                                                                                        .put("amount", body.getJsonObject("totals_recharge").getDouble("bonus_amount"));

                                                                                eWalletDBV.updateWalletAmount(conn, walletBonusUpdateObj)
                                                                                        .whenComplete((Boolean replyWalletBonusUpdate, Throwable ErrorWalletBonusUpdate) -> {
                                                                                        try {
                                                                                            if(ErrorWalletBonusUpdate != null) {
                                                                                                throw ErrorWalletBonusUpdate;
                                                                                            }

                                                                                            this.commit(conn, message, new JsonObject());

                                                                                            Double newAmount = UtilsMoney.round(((body.getJsonObject("wallet").getDouble("available_amount") +
                                                                                                    body.getDouble("finalTotalAmount"))), 2);

                                                                                            JsonObject walletDataMove = new JsonObject()
                                                                                                    .put("e_wallet_id", body.getJsonObject("wallet").getInteger("id"))
                                                                                                    .put("wallet_type", "wallet_recharge")
                                                                                                    .put("move_type", "income")
                                                                                                    .put("service_type", "wallet_recharge")
                                                                                                    .put("before_amount", body.getJsonObject("wallet").getDouble("available_amount"))
                                                                                                    .put("amount", body.getDouble("finalTotalAmount"))
                                                                                                    .put("after_amount", newAmount)
                                                                                                    .put("payment_id", paymentId);

                                                                                            Double newAmountBonus = UtilsMoney.round(((body.getJsonObject("wallet").getDouble("available_bonus") +
                                                                                                    body.getJsonObject("totals_recharge").getDouble("bonus_amount"))), 2);

                                                                                            JsonObject walletDataMoveBonus = new JsonObject()
                                                                                                    .put("e_wallet_id", body.getJsonObject("wallet").getInteger("id"))
                                                                                                    .put("wallet_type", "bonus")
                                                                                                    .put("move_type", "income")
                                                                                                    .put("service_type", "wallet_recharge")
                                                                                                    .put("before_amount", body.getJsonObject("wallet").getDouble("available_bonus"))
                                                                                                    .put("amount", body.getJsonObject("totals_recharge").getDouble("bonus_amount"))
                                                                                                    .put("after_amount", newAmountBonus)
                                                                                                    .put("payment_id", paymentId);

                                                                                            JsonObject walletMovesObjs = new JsonObject()
                                                                                                    .put("recharge_move", walletDataMove)
                                                                                                    .put("bonus_move", walletDataMoveBonus);

                                                                                            this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), walletMovesObjs,
                                                                                                    new DeliveryOptions().addHeader(ACTION, EwalletDBV.ACTION_REGISTER_WALLET_MOVE_RECHARGE_AND_BONUS), replyWalletMove -> {
                                                                                                        if (replyWalletMove.failed()) {
                                                                                                            replyWalletMove.cause().printStackTrace();
                                                                                                        }
                                                                                                    });
                                                                                        } catch(Throwable t) {
                                                                                            t.printStackTrace();
                                                                                            this.rollback(conn, t, message);
                                                                                        }
                                                                                });
                                                                            } catch(Throwable t) {
                                                                                t.printStackTrace();
                                                                                this.rollback(conn, t, message);
                                                                            }
                                                                        });
                                                                    } catch(Throwable t){
                                                                        t.printStackTrace();
                                                                        this.rollback(conn, t, message);
                                                                    }
                                                                });
                                                    } catch (Throwable t){
                                                        t.printStackTrace();
                                                        this.rollback(conn, t, message);
                                                    }
                                                });
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                            } catch(Throwable t) {
                                t.printStackTrace();
                                this.rollback(conn, t.getCause(), message);
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> updateBoardingPass(SQLConnection conn, String createdAt, String ticketType, Integer expireOpenTicketsAfter, Integer updatedBy, Integer boardingPassId, Integer promoId,
                                    Double innerAmount, Double innerDiscount, Double innerTotalAmount, Double iva, String conektaOrderId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject updateBody = new JsonObject()
                    .put(AMOUNT, innerAmount)
                    .put(DISCOUNT, innerDiscount)
                    .put(IVA, iva)
                    .put(TOTAL_AMOUNT, innerTotalAmount)
                    .put(BOARDINGPASS_STATUS, 1)
                    .put(UPDATED_BY, updatedBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                    .put(ID, boardingPassId);

            if (promoId != null) {
                updateBody.put(PROMO_ID, promoId);
            }

            if (conektaOrderId != null){
                updateBody.put("conekta_order_id", conektaOrderId);
            }

            String expiresAt = null;
            if (ticketType.contains("abierto")){
                Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt), Calendar.DAY_OF_YEAR, expireOpenTicketsAfter);
                expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
            }
            updateBody.put(EXPIRES_AT, expiresAt);

            GenericQuery update = generateGenericUpdate("boarding_pass", updateBody, true);

            conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdateOrderId -> {
                try{
                    if(replyUpdateOrderId.failed()){
                        throw  new Exception(replyUpdateOrderId.cause());
                    }
                    if (replyUpdateOrderId.result().getUpdated() == 0) { //does not exist element with id
                        throw new Exception("Does not exist element with id");
                    }

                    JsonObject finalResult = new JsonObject();

                    future.complete(finalResult);

                } catch(Exception e){
                    future.completeExceptionally(e);
                }

            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private boolean validateBoardingPassStatusRegister(Integer boardingPassStatus) throws Exception {
        if (boardingPassStatus == 1) {
            throw new Exception("Boarding pass already paid");
        } else if (boardingPassStatus != 4) {
            throw new Exception("Boarding pass status is not pre boarding");
        } else {
            return true;
        }
    }
    
    private CompletableFuture<JsonObject> getTotalAmountTicketsForBoardingPassById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(id);
        conn.queryWithParams(QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_BOARDING_PASS, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                ResultSet resultTickets = reply.result();
                if (resultTickets.getNumRows() == 0) {
                    future.completeExceptionally(new Throwable("Tickets not found for boarding pass"));
                } else {
                    JsonObject accumTickets = resultTickets.getRows().get(0);
                    future.complete(accumTickets);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getBoardingPassTicketsList(SQLConnection conn, Integer boardingPassId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_BOARDING_PASS_TICKETS_LIST, new JsonArray().add(boardingPassId), reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                future.complete(new JsonArray(results));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    
    private CompletableFuture<JsonObject> getBoardingPassById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String query = "SELECT * FROM boarding_pass WHERE id=? AND status=1 LIMIT 1;";
        conn.queryWithParams(query, new JsonArray().add(id), (AsyncResult<ResultSet> reply) -> {

            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                ResultSet result = reply.result();
                if (result.getNumRows() == 0) {
                    future.completeExceptionally(new Throwable("Boarding pass not found"));
                } else {
                    JsonObject boardingPass = result.getRows().get(0);
                    future.complete(boardingPass);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Double totalPayments, Double ivaPercent, Integer serviceId, String serviceType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject ticket = new JsonObject();
        Double iva = this.getIva(totalPayments, ivaPercent);

        switch(serviceType) {
            case "boarding_pass":
                ticket.put("boarding_pass_id", serviceId);
                break;
            case "wallet":
                ticket.put("e_wallet_recharge_id", serviceId);
                break;
        }

        ticket.put("action", "purchase");
        ticket.put("iva", iva);
        ticket.put("total", totalPayments);
        ticket.put("ticket_code", UtilsID.generateID("T"));

        ticket.put("paid", totalPayments);
        ticket.put("paid_change", 0.0);
        
        String insert = this.generateGenericCreate("tickets", ticket);

        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }

                final int id = reply.result().getKeys().getInteger(0);
                ticket.put("id", id);
                future.complete(ticket);

            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer boardingPassId, Integer ticketId, Boolean completePurchase, Double extras) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();
        if(completePurchase){

            this.getBoardingPassDetail(conn, boardingPassId).whenComplete((List<JsonObject> details, Throwable detailsError) -> {
                if (detailsError != null) {
                    future.completeExceptionally(detailsError);
                } else {
                    int detailLen = details.size();
                    JsonObject ticketDetail = new JsonObject();
                    if(extras > 0f){
                        ticketDetail.put("ticket_id", ticketId);
                        ticketDetail.put("quantity", 1);
                        ticketDetail.put("detail", "Cargos extras en documentación");
                        ticketDetail.put("unit_price", extras);
                        ticketDetail.put("amount", extras);
                        inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    }

                    for (int i = 0; i < detailLen; i++) {
                        JsonObject detail = details.get(i);
                        ticketDetail.put("ticket_id", ticketId);
                        ticketDetail.put("quantity", detail.getInteger("quantity"));
                        ticketDetail.put("detail", detail.getString("detail"));
                        ticketDetail.put("unit_price", detail.getDouble("unit_price"));
                        ticketDetail.put(DISCOUNT, detail.getDouble(DISCOUNT));
                        ticketDetail.put("amount", detail.getDouble("amount"));

                        inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    }
                    conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                        try{
                            if(replyInsert.failed()){
                                throw  new Exception(replyInsert.cause());
                            }
                            future.complete(replyInsert.succeeded());

                        }catch(Exception e){
                            future.completeExceptionally(replyInsert.cause());
                        }
                    });
                }
            });
        } else {
            if(extras > 0f){
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put("ticket_id", ticketId);
                ticketDetail.put("quantity", 1);
                ticketDetail.put("detail", "Cargos extras en documentación");
                ticketDetail.put("unit_price", extras);
                ticketDetail.put("amount", extras);
                //ticketDetail.put("created_by", createdBy);
                inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));

                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                    try{
                        if(replyInsert.failed()){
                            throw  new Exception(replyInsert.cause());
                        }
                        future.complete(replyInsert.succeeded());


                    }catch(Exception e){
                        future.completeExceptionally(replyInsert.cause());

                    }

                });
            } else {
                future.completeExceptionally(new Throwable("Nothing to put on ticket details"));
            }
        }

        return future;
    }
    
    private CompletableFuture<List<JsonObject>> getBoardingPassDetail(SQLConnection conn, Integer boardingPassId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        List<JsonObject> ticketDetails = new ArrayList<>();
        conn.queryWithParams(QUERY_TRAVEL_TICKET_DETAIL, new JsonArray().add(boardingPassId), (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> results = reply.result().getRows();
                if (reply.result().getNumRows() == 0) {
                    future.completeExceptionally(new Throwable("Boarding pass detail not found: "));
                } else {
                    int rLen = results.size();
                    for (int i = 0; i < rLen; i++) {
                        JsonObject detailResult = results.get(i);
                        JsonObject detail = new JsonObject();
                        String detailText = detailResult.getString("type_passanger")
                                + " De " + detailResult.getString("terminal_origin_city")
                                + ", " + detailResult.getString("terminal_origin_state")
                                + " a " + detailResult.getString("terminal_destiny_city")
                                + ", " + detailResult.getString("terminal_destiny_state");

                        detail.put("quantity", detailResult.getInteger("quantity"));
                        detail.put("detail", detailText);
                        detail.put(DISCOUNT, detailResult.getDouble(DISCOUNT));
                        detail.put("unit_price", detailResult.getDouble("unit_price"));
                        detail.put("amount", detailResult.getDouble("amount"));

                        ticketDetails.add(detail);
                    }
                    future.complete(ticketDetails);
                }

            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }

    private Double getIva(Double amount, Double ivaPercent){
        Double iva = 0.00;

        iva = amount - (amount / (1 + (ivaPercent/100)));

        return iva;
    }
    
    public void getTotalAmount(Message<JsonObject> message){
        
        int id = message.body().getInteger("id");
        JsonArray params = new JsonArray().add(id);
        dbClient.queryWithParams(QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_BOARDING_PASS, params, reply ->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                ResultSet resultTickets = reply.result();
                if (resultTickets.getNumRows() == 0) {
                    message.reply(new Throwable("Tickets not found for boarding pass"));
                } else {
                    JsonObject accumTickets = resultTickets.getRows().get(0);
                    message.reply(accumTickets);
                }

            }catch (Exception ex){
                ex.printStackTrace();
                message.reply(reply.cause());
            }
        });
        
    }

    public void getTotalAmountPackagePrepaid(Message<JsonObject> message){
        int id = message.body().getInteger("id");
        JsonArray params = new JsonArray().add(id);
        dbClient.queryWithParams(QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_PACKAGE_PREPAID, params, reply ->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                ResultSet resultTickets = reply.result();
                if (resultTickets.getNumRows() == 0) {
                    message.reply(new Throwable("Tickets not found for boarding pass"));
                } else {
                    JsonObject accumTickets = resultTickets.getRows().get(0);
                    message.reply(accumTickets);
                }
            } catch (Exception ex){
                ex.printStackTrace();
                message.reply(reply.cause());
            }
        });
    }

    public void getIdPaymentMethod(Message<JsonObject> message){
        JsonArray params = new JsonArray();
        params.add(message.body().getValue("idCustomer"));
               
        JsonObject row = new JsonObject();
        dbClient.query(this.QUERY_GET_CONEKTAID,  reply -> {
            if(reply.succeeded()){
                if(reply.result().getNumRows()>0){
                    List<JsonObject> Order = reply.result().getRows();
                    
                    message.reply(new JsonArray(Order));
                }else{
                    message.reply(null);
                }
            }else{
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void endPaymentWebPrepaid(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            final JSONObject orderJson = new JSONObject(body.getString("orderJson"));
            
            this.vertx.executeBlocking((Future<Order> future) -> {
                try {
                    //Pago en Conekta
                    Order order = Order.create(orderJson);
                    future.complete(order);
                } catch (ErrorList ex) {
                    ex.details.forEach(e -> System.out.println(e.message));
                    future.fail(new Exception("CONEKTA: " + ex.details.get(0).message));
                } catch (Error ex) {
                    future.fail(new Exception("CONEKTA: " + ex.message));
                }
            }, res -> {
                try {
                    if (res.failed()) {
                        throw new Exception(res.cause());
                    }

                    Order order = res.result();
                    String referencePre = "";
                    String o = order.charges.get(0).toString();
                    int index = o.indexOf("last4=");
                    int index_name = o.indexOf("name=");
                    int index_exp = o.indexOf("exp_month");
                    int index_type = o.indexOf("type=");
                    int index_brand = o.indexOf("brand=");
                    int index_auth = o.indexOf("auth_code=");

                    String type = o.substring(index_type + 5, index_brand - 2);

                    referencePre += "Name " + o.substring(index_name + 5, index_exp - 2) + ", ";
                    referencePre += "last4=" + o.substring(index + 6, index + 10) + ", ";
                    referencePre += "type=" + type + ", ";
                    referencePre += "brand=" + o.substring(index_brand + 6, index_auth - 2) + " ";

                    JsonObject result = new JsonObject().put("devMessage","Paid");
                    this.commit(conn, message, result);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    public CompletableFuture<Integer> savePayment(SQLConnection conn, int id, String currency, String paymentMethod, Double Amount, String Reference, String exchange_id, int ticket_id, String serviceType){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String currency_id = currency;
        String payment_method = paymentMethod.equals("credit") ? "card" : paymentMethod;
        Double amount = Amount;
        String reference = Reference;
        String exchange = exchange_id;

        conn.queryWithParams("SELECT id FROM payment_method WHERE alias = ?", new JsonArray().add(payment_method), (AsyncResult<ResultSet> replyMethod) -> {
            try{
                if(replyMethod.failed()){
                    throw  new Exception(replyMethod.cause());
                }
                List<JsonObject> resultMethod = replyMethod.result().getRows();
                JsonObject method = resultMethod.get(0);

                JsonObject payment = new JsonObject();

                switch(serviceType) {
                    case "boarding_pass":
                        payment.put("boarding_pass_id", id);
                        break;
                    case "wallet":
                        payment.put("e_wallet_recharge_id", id);
                        break;
                }
                payment.put("payment_method_id", method.getInteger("id"));
                payment.put("payment_method", payment_method);
                payment.put("amount", amount);
                payment.put("reference", reference);
                payment.put("currency_id", currency_id);
                payment.put("ticket_id", ticket_id);
                if(exchange != "null") {
                    payment.put("exchange_rate_id", exchange);
                }

                String insert = this.generateGenericCreate("payment", payment);

                conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                    try {
                        if (reply.succeeded()) {
                            Integer paymentId = reply.result().getKeys().getInteger(0);
                            future.complete(paymentId);
                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }catch(Exception e){
                future.completeExceptionally(replyMethod.cause());
            }
        });
        return future;
    }
    
    public void saveIdOrder(Message<JsonObject> message){
        int pass_id = message.body().getInteger("idOrder");
        String idOrder = message.body().getString("idOrderConekta");
                        
        String query = "UPDATE boarding_pass set boarding_pass.conekta_order_id = '"+idOrder+"' where boarding_pass.id = "+pass_id;
        
        dbClient.update(query, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                if (reply.result().getUpdated() == 0) { //does not exist element with id
                    message.reply(null, new DeliveryOptions().addHeader(ErrorCodes.DB_ERROR.toString(), "Element not found"));
                } else {
                    message.reply(null);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
    }
    
    public void getCustomerId(Message<JsonObject> message){
        JsonArray params = new JsonArray();
        params.add(message.body().getValue("idCustomer"));
               
        dbClient.queryWithParams(this.QUERY_GET_CONEKTAID, params,  reply -> {
            try{
                if(reply.failed()){
                    throw  new Exception(reply.cause());
                }
                if(reply.result().getNumRows()>0){
                    List<JsonObject> Order = reply.result().getRows();

                    message.reply(new JsonArray(Order));
                }else{
                    message.reply(null);
                }

            }catch(Exception e){
                reportQueryError(message, reply.cause());
            }

        });
    }
    
    public void getCustomerInfo(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer id = body.getInteger("idCustomer");

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
            dbClient.queryWithParams(query, params, reply ->{
                try {
                    if (reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject job = reply.result().getRows().get(0);
                            job.put("name", remove1(job.getString("name", "Sin nombre")));
                            job.put("City", remove1(job.getString("City", "Los Mochis")));
                            job.put("Country", remove1(job.getString("Country", "Mexico")));
                            job.put("street", remove1(job.getString("street", "Gabriel Leyva")));
                            job.put("zip_code", job.getInteger("zip_code", 81200));

                            message.reply(job);
                        }else {
                            message.reply(null);
                        }
                    }else{
                        reportQueryError(message, reply.cause());
                    }
                    
                } catch (Exception e) {
                    reportQueryError(message, e);
                }

            });
        } catch (Exception e) {
            reportQueryError(message, e);
        }

    }
    
    public static String remove1(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
    // Cadena de caracteres ASCII que reemplazarán los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i=0; i<original.length(); i++) {
        // Reemplazamos los caracteres especiales.
        output = output.replace(original.charAt(i), ascii.charAt(i));
    }//for i
    return output;
}

    public void saveIdConekta(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer id = body.getInteger("idCustomer");
        String idConekta = body.getString("idCustomerConekta");
        JsonArray params = new JsonArray();
        String query = "UPDATE customer SET conekta_id = ? WHERE id = ?;";
        params.add(idConekta);

        // IF id is null update general public customer
        if (id == null) {
            query = "UPDATE customer SET conekta_id = ? WHERE email = ? AND phone = ?;";
            params.add("noreply@allabordo.com");
            params.add("6688569071");
        } else {
            params.add(id);
        }

        dbClient.updateWithParams(query, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                if (reply.result().getUpdated() == 0) { //does not exist element with id
                    message.reply(null, new DeliveryOptions().addHeader(ErrorCodes.DB_ERROR.toString(), "Element not found"));
                } else {
                    message.reply(null);
                }

            }catch (Exception ex){
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
        
    }
    
    public void getOrderInfo(Message<JsonObject> message){
        
        int id = message.body().getInteger("id");
        String query = QUERY_ITEMS_DETAILS;
        JsonArray params = new JsonArray();
        params.add(id);
        dbClient.queryWithParams(query, params,  reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows()>0){
                    List<JsonObject> Order = reply.result().getRows();

                    message.reply(new JsonArray(Order));
                }else{
                    message.reply(null);
                }

            }catch (Exception ex){
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
        
    }

    private void doSendBoardingPassEmail(Integer boardingPassId, JsonObject bonificationData) {
        this.sendBoardingPassEmail(boardingPassId).setHandler(reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                JsonObject detail = reply.result();
                String email = (String) detail.remove("passenger_email");
                String customerEmail = (String) detail.remove("customer_email");
                email = email != null ? email : customerEmail;
                if(bonificationData != null) {
                    detail.put("e_wallet_bonification", bonificationData);
                }
                JsonObject body = new JsonObject()
                        .put("template", "boardingpass.html")
                        .put("subject", "AllAbordo | Compra de boletos")
                        .put("to", email)
                        .put("pdf", new JsonObject()
                                .put("template", "boardingpass-pdf.html")
                                .put("body", detail)
                                .put("fileName", detail.getString("reservation_code"))
                        )
                        .put("body", detail);
                DeliveryOptions options = new DeliveryOptions()
                        .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);
                this.vertx.eventBus().send(MailVerticle.class.getSimpleName(), body, options, replySend -> {
                    if (replySend.failed()) {
                        replySend.cause().printStackTrace();
                    }
                });
            }catch (Exception ex){
                ex.printStackTrace();
                reply.cause().printStackTrace();
            }
        });

    }

    private Future<JsonObject> sendBoardingPassEmail(Integer boardingPassId) {
        Future<JsonObject> future = Future.future();
        Future<ResultSet> detailFuture = Future.future();
        Future<ResultSet> passengersFuture = Future.future();
        Future<ResultSet> ticketsFuture = Future.future();
        Future<ResultSet> routesFuture = Future.future();

        JsonArray params = new JsonArray().add(boardingPassId);

        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_DETAIL, params, detailFuture.completer());
        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_PASSENGERS, params, passengersFuture.completer());
        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_TICKETS, params, ticketsFuture.completer());
        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_ROUTES, params, routesFuture.completer());

        CompositeFuture.all(detailFuture, passengersFuture, ticketsFuture, routesFuture).setHandler(reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }

                ResultSet rsDetail = reply.result().resultAt(0);
                ResultSet rsPassengers = reply.result().resultAt(1);
                ResultSet rsTickets = reply.result().resultAt(2);
                ResultSet rsRoutes = reply.result().resultAt(3);

                JsonObject detail = rsDetail.getRows().get(0);

                // Set customer name
                String customer = (String) detail.remove("customer_fullname");
                String passenger = (String) detail.remove("passenger_fullname");
                customer = customer != null ? customer : passenger;
                detail.put("customer", customer);

                // Set payment method
                detail.put("payment_method", "Tarjeta");

                // Set date
                String createdAt = (String) detail.remove("created_at");
                Date date = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt);
                detail.put("date", UtilsDate.format_D_MM_YYYY(date));
                detail.put("hour", UtilsDate.format_HH_MM(date));

                // Set passengers
                List<JsonObject> passengers = rsPassengers.getRows();
                List<JsonObject> tickets = rsTickets.getRows();
                for (JsonObject p: passengers) {
                    for (JsonObject t: tickets) {
                        Integer passengerId = p.getInteger("id");
                        Integer ticketPassengerId = t.getInteger("boarding_pass_passenger_id");
                        String ticketTypeRoute = t.getString("ticket_type_route");
                        if (passengerId.equals(ticketPassengerId)) {
                            String key = ticketTypeRoute.equals("ida") ? "departure_seat" : "return_seat";
                            String seat = t.getString("seat");
                            p.put(key, seat);
                        }
                    }
                }

                // Set routes
                List<JsonObject> routes = rsRoutes.getRows();

                for (JsonObject route: routes) {
                    Date travelDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss((String) route.remove("travel_date"));
                    Date arrivalDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss((String) route.remove("arrival_date"));
                    route.put("departure_date", UtilsDate.format_D_MM_YYYY(travelDate));
                    route.put("departure_hour", UtilsDate.format_HH_MM(travelDate));
                    route.put("arrival_date", UtilsDate.format_D_MM_YYYY(arrivalDate));
                    route.put("arrival_hour", UtilsDate.format_HH_MM(arrivalDate));

                    String travelType = (String) route.remove("type_travel");
                    route.put("travel_type", travelType.equals("0") ? "Express" :
                            travelType.equals("1") ? "Directo" :
                                    "Ordinario");
                }

                // Set payments
                JsonArray paymentConcepts = new JsonArray()
                        .add(new JsonObject()
                                .put("name", "Subtotal")
                                .put("amount", detail.remove("amount")))
                        .add(new JsonObject()
                                .put("name", "Descuento")
                                .put("amount", detail.remove("discount")))
                        .add(new JsonObject()
                                .put("name", "Total")
                                .put("amount", detail.getValue("total_amount")));

                JsonObject result = detail.copy()
                        .put("passengers", passengers)
                        .put("routes", routes)
                        .put("payment_concepts", paymentConcepts);

                future.complete(result);

            } catch (Exception e) {
                e.printStackTrace();
                future.fail(e);
            }
        });

        return future;
    }
    
    private void getCitiesName(Message<JsonObject> message) {
        JsonArray params = new JsonArray();
        params.add(message.body().getValue("origin_id"));
        params.add(message.body().getValue("destiny_id"));
        
        String query = "select " +
        "city.name " +
        "from city " +
        "left join branchoffice on city.id = branchoffice.city_id " +
        "where branchoffice.id = "
                + message.body().getValue("origin_id")
                + " or branchoffice.id = "
                + message.body().getValue("destiny_id")
                + ";";

        JsonObject row = new JsonObject();
        this.dbClient.query(query, reply -> {
            if (reply.result().getNumRows()==2) {
                row.put("origin", remove1(reply.result().getRows().get(0).getString("name")));
                row.put("destination", remove1(reply.result().getRows().get(1).getString("name")));
                
                List<JsonObject> res = new ArrayList<>();
                res.add(row);
                
                message.reply(new JsonArray(res));
            } else {
                row.put("origin", "");
                row.put("destination", "");
                
                List<JsonObject> res = new ArrayList<>();
                res.add(row);
                
                message.reply(new JsonArray(res));
                
            }
        });
    }

    private CompletableFuture<Boolean> insertTicketDetailRecharge(SQLConnection conn, Integer ticketId, JsonObject data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        JsonObject ticketDetail = new JsonObject();
        ticketDetail.put("ticket_id", ticketId);
        ticketDetail.put("quantity", data.getInteger("quantity"));
        ticketDetail.put("detail", data.getString("detail"));
        ticketDetail.put("unit_price", data.getDouble("unit_price"));
        ticketDetail.put(DISCOUNT, data.getDouble(DISCOUNT));
        ticketDetail.put("amount", data.getDouble("amount"));

        String insert = this.generateGenericCreate("tickets_details", ticketDetail);

        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                future.complete(reply.succeeded());
            } catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }
    
    private String QUERY_ITEMS_DETAILS = "SELECT " +
    "1 as 						'quantity', " +
    "schedule_route_destination.id as 			'trip_id', " +
    "boarding_pass.email, boarding_pass.phone, boarding_pass_passenger.id AS boarding_pass_passenger_id,boarding_pass_route.id AS boarding_pass_route_id," +
    "schedule_route_destination.travel_date as          'departs_at', " +
    "schedule_route_destination.arrival_date as 	'arrives_at', " +
    "schedule_route_destination.terminal_origin_id as	'origin_id', " +
    "schedule_route_destination.terminal_destiny_id as	'destiny_id', " +
    "boarding_pass.ticket_type as 			'ticket_class', " +
    "boarding_pass_ticket.seat as 			'seat_number', " +
    "special_ticket.name as				'passenger_type', " +
    "boarding_pass_ticket.total_amount as		'unit_price', " +
    "boarding_pass.total_amount as                      'total_amount', " +
    "currency.id as 					'currency_id', "+
    "currency.abr as 					'currency',"+
    "boarding_pass.exchange_rate_id as                  'exchange_id', "+
    "origin.name as                                     'origin', "+
    "destination.name as 				'destiny', "+
    "concat(origin.name,' - ',destination.name) as      'name' "+
    "FROM boarding_pass_route " +
    "left join schedule_route_destination on schedule_route_destination.id = boarding_pass_route.schedule_route_destination_id " +
    "left join boarding_pass on boarding_pass.id = boarding_pass_route.boarding_pass_id " +
    "left join boarding_pass_ticket on boarding_pass_ticket.boarding_pass_route_id=boarding_pass_route.id " +
    "left join boarding_pass_passenger on boarding_pass_ticket.boarding_pass_passenger_id = boarding_pass_passenger.id " +
    "left join special_ticket on special_ticket.id = boarding_pass_passenger.special_ticket_id " +
    "left join general_setting on general_setting.FIELD = 'currency_id' " +
    "left join currency on currency.id = general_setting.value "+
    "left join branchoffice as bo on bo.id = schedule_route_destination.terminal_origin_id "+
    "left join branchoffice as bd on bd.id = schedule_route_destination.terminal_destiny_id "+
    "left join city as origin on origin.id = bo.city_id "+
    "left join city as destination on destination.id = bd.city_id "+
    "where boarding_pass_route.boarding_pass_id = ?";
    
    private String QUERY_GETNAME_ORIGIN_DESTINATION = "select " +
    "city.name " +
    "from city " +
    "left join branchoffice on city.id = branchoffice.city_id " +
    "where branchoffice.id = ? or branchoffice.id = ? ";

    private static final String QUERY_EMAIL_BOARDING_PASS_DETAIL = "SELECT bp.reservation_code, \n" +
            "bp.amount, bp.discount, \n" +
            "bp.total_amount, bp.created_at, (bp.ticket_type + 0) AS ticket_type, \n" +
            "CONCAT(cst.first_name, ' ', cst.last_name) AS customer_fullname, \n" +
            "CONCAT(bpp.first_name, ' ', bpp.last_name) AS passenger_fullname, \n" +
            "bp.email AS passenger_email, cst.email AS customer_email \n " +
            "FROM boarding_pass AS bp \n" +
            "LEFT JOIN customer AS cst ON cst.id = bp.customer_id \n" +
            "LEFT JOIN boarding_pass_passenger AS bpp ON bp.principal_passenger_id = bpp.id \n" +
            "WHERE bp.id = ?;";

    private static final String QUERY_EMAIL_BOARDING_PASS_PASSENGERS = "SELECT bpp.id, \n" +
            "CONCAT(bpp.first_name, ' ', bpp.last_name) AS name \n" +
            "FROM boarding_pass_passenger AS bpp \n" +
            "WHERE bpp.boarding_pass_id = ?;";

    private static final String QUERY_EMAIL_BOARDING_PASS_TICKETS = "SELECT bpt.id, bpt.boarding_pass_passenger_id, bpt.seat, bpr.ticket_type_route \n" +
            "FROM boarding_pass_ticket AS bpt\n" +
            "INNER JOIN boarding_pass_passenger AS bpp ON bpt.boarding_pass_passenger_id = bpp.id\n" +
            "INNER JOIN boarding_pass AS bp ON bpp.boarding_pass_id = bp.id\n" +
            "INNER JOIN boarding_pass_route AS bpr ON bpt.boarding_pass_route_id = bpr.id\n" +
            "WHERE bp.id = ?;";

    private static final String QUERY_EMAIL_BOARDING_PASS_ROUTES = "SELECT origin.prefix AS origin_prefix, \n" +
            "destination.prefix AS destination_prefix,\n" +
            "o_state.name AS origin_state,\n" +
            "o_city.name AS origin_city,\n" +
            "d_state.name AS destination_state,\n" +
            "d_city.name AS destination_city, \n" +
            "srd.travel_date, \n" +
            "srd.arrival_date,\n" +
            "cr.type_travel,\n" +
            "cd.travel_time\n" +
            "FROM boarding_pass_route AS bpr \n" +
            "INNER JOIN schedule_route_destination AS srd ON bpr.schedule_route_destination_id = srd.id \n" +
            "INNER JOIN config_destination AS cd ON srd.config_destination_id = cd.id \n" +
            "INNER JOIN schedule_route AS sr ON srd.schedule_route_id = sr.id\n" +
            "INNER JOIN config_route AS cr ON sr.config_route_id = cr.id\n" +
            "INNER JOIN branchoffice AS origin ON cd.terminal_origin_id = origin.id\n" +
            "INNER JOIN state AS o_state ON origin.state_id = o_state.id \n" +
            "INNER JOIN city AS o_city ON origin.city_id = o_city.id \n" +
            "INNER JOIN branchoffice AS destination ON cd.terminal_destiny_id = destination.id\n" +
            "INNER JOIN state AS d_state ON destination.state_id = d_state.id \n" +
            "INNER JOIN city AS d_city ON destination.city_id = d_city.id \n" +
            "WHERE bpr.boarding_pass_id = ?;";
    
    private String QUERY_GET_CONEKTAID = "SELECT conekta_id FROM customer where id=?";
    
    private static final String QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_BOARDING_PASS = "SELECT bp.id, "
        + "	COALESCE(SUM(bpt.amount),0) AS amount, "
        + "	COALESCE(SUM(bpt.discount),0) AS discount, "
        + "	COALESCE(SUM(bpt.total_amount),0) AS total_amount "
        + "	FROM boarding_pass AS bp "
        + "	LEFT JOIN boarding_pass_route AS bpr ON bpr.boarding_pass_id=bp.id "
        + "	LEFT JOIN boarding_pass_ticket AS bpt ON bpt.boarding_pass_route_id=bpr.id "
        + "	WHERE bp.id = ? "
        + "	GROUP BY bp.id;";
    
    private static final String QUERY_TRAVEL_TICKET_DETAIL = "SELECT \n" +
            "   bp.id, \n" +
            "   bpp.special_ticket_id, \n" +
            "   sp.name AS type_passanger, \n" +
            "   COUNT(bpp.special_ticket_id) AS quantity,\n" +
            "   COALESCE(AVG(bpt.total_amount - bpt.extra_charges), 0) AS unit_price,\n" +
            "   IF(p.id IS NOT NULL, IF(p.discount_per_base, COALESCE(SUM(bpt.discount), 0), COALESCE(SUM(bpt.discount - ctp.discount), 0)), 0) AS discount,\n" +
            "   COALESCE(SUM(bpt.total_amount - bpt.extra_charges), 0) AS amount,\n" +
            "   origin.prefix AS terminal_origin_prefix, \n" +
            "   origin_city.name AS terminal_origin_city, \n" +
            "   origin_state.name AS terminal_origin_state, \n" +
            "   destiny.prefix AS terminal_destiny_prefix, \n" +
            "   destiny_city.name AS terminal_destiny_city, \n" +
            "   destiny_state.name AS terminal_destiny_state \n" +
            " FROM boarding_pass AS bp \n" +
            " LEFT JOIN promos p ON p.id = bp.promo_id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bpr.boarding_pass_id=bp.id \n" +
            " LEFT JOIN boarding_pass_ticket AS bpt ON bpt.boarding_pass_route_id=bpr.id \n" +
            " LEFT JOIN boarding_pass_passenger as bpp ON bpp.id = bpt.boarding_pass_passenger_id AND bpp.is_child_under_age = 0 \n" +
            " LEFT JOIN special_ticket AS sp ON bpp.special_ticket_id = sp.id \n" +
            " LEFT JOIN schedule_route_destination AS srd ON bpr.schedule_route_destination_id = srd.id \n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN config_ticket_price ctp ON ctp.config_destination_id = cd.id AND ctp.special_ticket_id = sp.id\n" +
            " LEFT JOIN branchoffice AS origin ON origin.id = srd.terminal_origin_id \n" +
            " LEFT JOIN branchoffice AS destiny ON destiny.id = srd.terminal_destiny_id \n" +
            " LEFT JOIN city AS origin_city ON origin_city.id = origin.city_id \n" +
            " LEFT JOIN city AS destiny_city ON destiny_city.id = destiny.city_id \n" +
            " LEFT JOIN state AS origin_state ON origin_state.id = origin.state_id \n" +
            " LEFT JOIN state AS destiny_state ON destiny_state.id = destiny.state_id \n" +
            " WHERE bp.id = ?\n" +
            " AND bpp.special_ticket_id = ctp.special_ticket_id \n" +
            " GROUP BY \n" +
            " bp.id, \n" +
            " bpp.special_ticket_id, \n" +
            " origin_city.name, \n" +
            " origin_state.name, \n" +
            " destiny_state.name, \n" +
            " origin.prefix, \n" +
            " destiny.prefix, \n" +
            " destiny_city.name;";

    private static final String QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_PACKAGE_PREPAID = "SELECT pp.id, \n" +
            " pp.amount AS amount,\n" +
            " pp.total_amount AS total_amount ,\n" +
            " pp.total_tickets as total_ticket,\n" +
            " c.abr,\n" +
            " c.id as currency_id,\n" +
            " pc.name,\n" +
            " pp.reservation_code\n" +
            " FROM prepaid_package_travel AS pp \n" +
            " inner join  prepaid_package_config as  pc on pc.id=pp.prepaid_package_config_id\n" +
            " left join currency as c on  c.id=22\n" +
            " WHERE pp.id = ?;";

    private static final String QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_PREPAID_PACKAGE = "SELECT bp.id, "
            + "	COALESCE(SUM(bpt.amount),0) AS amount, "
            + "	COALESCE(SUM(bpt.discount),0) AS discount, "
            + "	COALESCE(SUM(bpt.total_amount),0) AS total_amount "
            + "	FROM boarding_pass AS bp "
            + "	LEFT JOIN boarding_pass_route AS bpr ON bpr.boarding_pass_id=bp.id "
            + "	LEFT JOIN boarding_pass_ticket AS bpt ON bpt.boarding_pass_route_id=bpr.id "
            + "	WHERE bp.id = ? "
            + "	GROUP BY bp.id;";

}
