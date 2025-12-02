package database.boardingpass.handlers;

import database.boardingpass.BoardingPassDBV;
import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.money.PaymentDBV;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.*;
import static database.boardingpass.BoardingPassDBV.TICKET_TYPE;
import static database.promos.PromosDBV.*;
import static database.promos.PromosDBV.ACTION_APPLY_PROMO_CODE;
import static service.commons.Constants.*;
import static service.commons.Constants.DISCOUNT;

public class PartnerEndRegister  extends DBHandler<BoardingPassDBV> {

    private static final String INTEGRATION_PARTNER_SESSION_ID = "integration_partner_session_id";
    public static final String TOKEN_ID = "token_id";

    private final BoardingPassDBV boardingPassDBV;
    public PartnerEndRegister(BoardingPassDBV dbVerticle) {
        super(dbVerticle);
        this.boardingPassDBV = dbVerticle;
    }

    @Override
    public void handle(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                final boolean flagPromo = (boolean) body.remove(PromosDBV.FLAG_PROMO);
                final int boardingPassId = body.getInteger("id");
                final int sessionTokenId = body.getInteger(TOKEN_ID);
                final int currencyId = body.getInteger("currency_id");
                final double ivaPercent = body.getInteger("iva_percent");
                JsonArray payments = (JsonArray) body.remove("payments");
                JsonObject promoDiscount = (JsonObject) body.remove(DISCOUNT);
                int expireOpenTicketsAfter = (int) body.remove("expire_open_tickets_after");

                boardingPassDBV.getBoardingPassById(conn, boardingPassId).whenComplete((boardingPass, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        String reservationCode = boardingPass.getString(RESERVATION_CODE);
                        Integer boardingPassStatus = boardingPass.getInteger(BOARDINGPASS_STATUS);
                        String ticketType = boardingPass.getString(TICKET_TYPE);
                        String createdAt = boardingPass.getString(CREATED_AT);

                        Integer integrationPartnerSessionId = boardingPass.getInteger(INTEGRATION_PARTNER_SESSION_ID);
                        if (!integrationPartnerSessionId.equals(sessionTokenId)) {
                            throw new Exception("Boarding pass not found");
                        }

                        if (boardingPassDBV.validateBoardingPassStatusRegister(boardingPassStatus)) {
                            boardingPassDBV.getTotalAmountTicketsForBoardingPassById(conn, boardingPassId).whenComplete((JsonObject tickets, Throwable terror) -> {
                                try {
                                    if (terror != null) {
                                        throw terror;
                                    }
                                    Double amount = tickets.getDouble("amount");
                                    Double discount = tickets.getDouble("discount");
                                    Double totalAmount = tickets.getDouble("total_amount");
                                    final Double iva = UtilsMoney.round(boardingPassDBV.getIva(totalAmount, ivaPercent), 2);

                                    boardingPassDBV.getBoardingPassTicketsList(conn, boardingPassId).whenComplete((resultBPT, errorBPT) -> {
                                        try {
                                            if (errorBPT != null){
                                                throw errorBPT;
                                            }

                                            JsonObject bodyService = new JsonObject()
                                                    .put("id", boardingPassId)
                                                    .put("boardingpass_status", 1)
                                                    .put("amount", amount)
                                                    .put("discount", discount)
                                                    .put("iva", iva)
                                                    .put("total_amount", totalAmount)
                                                    .put("updated_at", UtilsDate.sdfDataBase(new Date()));

                                            JsonObject bodyPromo = new JsonObject()
                                                    .put(USER_ID, 1)
                                                    .put(FLAG_USER_PROMO, false)
                                                    .put(DISCOUNT, promoDiscount)
                                                    .put(SERVICE, SERVICES.boardingpass)
                                                    .put(BODY_SERVICE, bodyService)
                                                    .put(PRODUCTS, resultBPT)
                                                    .put(OTHER_PRODUCTS, new JsonArray())
                                                    .put(FLAG_PROMO, flagPromo);
                                            this.getVertx().eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                try {
                                                    if(replyPromos.failed()) {
                                                        throw replyPromos.cause();
                                                    }
                                                    JsonObject resultApplyDiscount = replyPromos.result().body();
                                                    JsonObject service = resultApplyDiscount.getJsonObject(PromosDBV.SERVICE);
                                                    Double innerTotalAmount = UtilsMoney.round(service.getDouble(TOTAL_AMOUNT), 2);
                                                    service.put("payment_condition", body.getString("payment_condition"));

                                                    String expiresAt = null;
                                                    if (ticketType.contains("abierto")){
                                                        Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt), Calendar.DAY_OF_YEAR, expireOpenTicketsAfter);
                                                        expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
                                                    }
                                                    service.put(EXPIRES_AT, expiresAt);

                                                    GenericQuery update = this.generateGenericUpdate(SERVICES.boardingpass.getTable(), service, true);

                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                        try {
                                                            if (replyUpdate.failed()){
                                                                throw replyUpdate.cause();
                                                            }


                                                            boardingPassDBV.insertTicket(conn,"purchase", boardingPassId, null, innerTotalAmount, null, null, ivaPercent, null, 0.0).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                                try {
                                                                    if (ticketError != null){
                                                                        throw ticketError;
                                                                    }
                                                                    Integer ticketId = ticket.getInteger(ID);

                                                                    boardingPassDBV.insertTicketDetail(conn, boardingPassId, ticketId, true, 0.0, null, new JsonArray(), boardingPassStatus).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                        try {
                                                                            if (dError != null) {
                                                                                throw dError;
                                                                            }

                                                                            this.insertPaymentsBoardingPassRegister(conn, payments, ticketId, currencyId, boardingPassId,
                                                                                    innerTotalAmount).whenComplete((resultInsertPayment, errorInsertPayment) -> {
                                                                                try {
                                                                                    if (errorInsertPayment != null) {
                                                                                        throw errorInsertPayment;
                                                                                    }

                                                                                    this.commitTransaction(message, conn, new JsonObject()
                                                                                            .put("reservation_code", reservationCode)
                                                                                            .put(DISCOUNT_APPLIED, flagPromo));
                                                                                    boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);

                                                                                } catch (Throwable t) {
                                                                                    t.printStackTrace();
                                                                                    this.rollbackTransaction(message, conn, t);
                                                                                    boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                                                                }
                                                                            });
                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            this.rollbackTransaction(message, conn, t);
                                                                            boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                                                        }
                                                                    });
                                                                } catch (Throwable t){
                                                                    t.printStackTrace();
                                                                    this.rollbackTransaction(message, conn, t);
                                                                    boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                                                }
                                                            });




                                                        } catch (Throwable t){
                                                            t.printStackTrace();
                                                            this.rollbackTransaction(message, conn, t);
                                                            boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                                        }
                                                    });

                                                } catch (Throwable t){
                                                    t.printStackTrace();
                                                    this.rollbackTransaction(message, conn, t);
                                                    boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                                }
                                            });
                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollbackTransaction(message, conn, t);
                                            boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                        }
                                    });
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    this.rollbackTransaction(message, conn, t);
                                    boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                                }
                            });
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollbackTransaction(message, conn, t);
                        boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), boardingPassId);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollbackTransaction(message, conn, t);
                boardingPassDBV.setInPaymentStatus(boardingPassDBV.getTableName(), message.body().getInteger(ID));
            }
        });
    }

    private CompletableFuture<Boolean> insertPaymentsBoardingPassRegister(SQLConnection conn, JsonArray payments, Integer ticketId, Integer currencyId, Integer boardingPassId, Double innerTotalAmount){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double totalPayments = 0.0;
            for (int i = 0; i < payments.size(); i++) {
                JsonObject payment = payments.getJsonObject(i);
                Double paymentAmount = payment.getDouble("amount");
                if (paymentAmount == null || paymentAmount < 0.0) {
                    throw new Exception("Invalid payment amount: " + paymentAmount);
                }
                totalPayments += UtilsMoney.round(paymentAmount, 2);
            }
            if (totalPayments > innerTotalAmount) {
                throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
            } else if (totalPayments < innerTotalAmount) {
                throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
            } else {
                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                for (int i = 0; i < payments.size(); i++) {
                    JsonObject payment = payments.getJsonObject(i);
                    payment.put("ticket_id", ticketId);
                    pTasks.add(this.insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId));
                }
                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[payments.size()]));
                allPayments.whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        future.complete(true);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer boardingPassId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            payment.put("currency_id", currencyId)
                    .put("boarding_pass_id", boardingPassId);

            PaymentDBV objPayment = new PaymentDBV();
            objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                try {
                    if (error != null) {
                        throw new Exception(error);
                    }
                    payment.put("id", resultPayment.getInteger("id"));
                    future.complete(payment);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

}
