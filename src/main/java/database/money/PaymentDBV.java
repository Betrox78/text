/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.money;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;

/**
 *
 * @author ulises
 */
public class PaymentDBV extends DBVerticle{

    public static final String GENERIC_REGISTER = "PaymentDBV.genericRegister";
    public static final String REGISTER = "PaymentDBV.register";
    public static final String ID = "id";
    public static final String ACTION_SET_IN_PAYMENT_STATUS = "PaymentDBV.setInPaymentStatus";
    public static final String ACTION_GET_IN_PAYMENT_STATUS = "PaymentDBV.getInPaymentStatus";

    @Override
    public String getTableName() {
        return "payment";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case GENERIC_REGISTER:
                this.genericRegister(message);
                break;
            case REGISTER:
                this.register(message);
                break;
            case ACTION_SET_IN_PAYMENT_STATUS:
                this.setInPaymentStatus(message);
                break;
            case ACTION_GET_IN_PAYMENT_STATUS:
                this.getInPaymentStatus(message);
                break;
        }
    }

    private void genericRegister(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            this.insertPayment(conn, body).whenComplete((result, error) -> {
                if(error != null){
                    this.rollback(conn, error, message);
                } else {
                    this.commit(conn, message, result);
                }
            });
        });
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            JsonArray payments = body.getJsonArray("payments");
            this.insertPayments(conn, payments).whenComplete((resultProducts, error) -> {
                if(error != null){
                    reportQueryError(message, error);
                } else {
                    message.reply(resultProducts);
                }
            });
        });
    }

    public CompletableFuture<JsonArray> insertPayments(SQLConnection conn, JsonArray payments){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(payments.stream()
                .map(pay -> insertPayment(conn, (JsonObject) pay))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(payments);
                    }
                });
        return future;
    }

    public CompletableFuture<JsonObject> insertPayment(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer paymentMethodId = body.getInteger("payment_method_id");

            conn.queryWithParams("SELECT alias FROM payment_method WHERE id = ?;", new JsonArray().add(paymentMethodId), replyAlias -> {
                try {
                    if (replyAlias.failed()){
                        throw new Exception(replyAlias.cause());
                    }
                    List<JsonObject> paymentMethods = replyAlias.result().getRows();
                    if (paymentMethods.isEmpty()){
                        throw new Exception("Payment method not found");
                    }
                    String alias = paymentMethods.get(0).getString("alias");
                    body.put("payment_method", alias);
                    GenericQuery model = this.generateGenericCreate(body);
                    conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
                        try {
                            if (reply.failed()){
                                throw new Exception(reply.cause());
                            }
                            Integer paymentId = reply.result().getKeys().getInteger(0);
                            body.put("id", paymentId);
                            future.complete(new JsonObject().put(ID, paymentId));
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } catch( Exception e){
                    future.completeExceptionally(e);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void setInPaymentStatus(Message<JsonObject> message) {
        String table = message.body().getString("origin");
        Integer id = message.body().getInteger("origin_id");
        JsonObject params = new JsonObject().put(ID, id).put("in_payment", true);
        String update = this.generateGenericUpdateString(table, params).concat(" AND in_payment = false");
        this.dbClient.update(update, queryReply -> {
            try {
                if (queryReply.failed())
                    throw new Exception(queryReply.cause());

                if(queryReply.result().getUpdated() == 0)
                    throw new Exception("payment in process");

                message.reply(queryReply.succeeded());

            } catch (Exception e) {
                reportQueryError(message, e);
            }
        });
    }

    private void getInPaymentStatus(Message<JsonObject> message) {
        String table = message.body().getString("origin");
        Integer id = message.body().getInteger("origin_id");
        String query = "SELECT in_payment from " + table + " WHERE id = ?;";
        this.dbClient.queryWithParams(query, new JsonArray().add(id), queryReply -> {
            try {
                if (queryReply.succeeded()) {
                    List<JsonObject> res = queryReply.result().getRows();
                    if (res.isEmpty()) {
                        reportQueryError(message, new Throwable("Boarding Pass not found"));
                    } else {
                        message.reply(res.get(0));
                    }
                } else {
                    reportQueryError(message, queryReply.cause());
                }
            } catch (Exception e) {
                reportQueryError(message, e);
            }
        });
    }

}
