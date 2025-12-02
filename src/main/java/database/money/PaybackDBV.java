package database.money;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.UPDATED_AT;
import static service.commons.Constants.UPDATED_BY;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gerardo Valdés Uriarte - gerardo@indqtech.com
 */
public class PaybackDBV extends DBVerticle {

    public static final String ACTION_UPDATE = "PaybackDBV.updatePayback";

    @Override
    public String getTableName() {
        return "payback";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_UPDATE:
                this.updatePayback(message);
                break;
        }
    }

    private void updatePayback(Message<JsonObject> message){
        try {
            JsonObject customer = message.body().copy();
            JsonArray params = new JsonArray()
                    .add(customer.getDouble("points"))
                    .add(customer.getInteger("km"))
                    .add(customer.getDouble("money"))
                    .add(customer.getString(UPDATED_AT))
                    .add(customer.getInteger(UPDATED_BY))
                    .add(customer.getString("service_type"));

            this.dbClient.updateWithParams(QUERY_UPDATE_PAYBACK, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().getUpdated() < 1){
                        throw new Exception("The field service_type is incorrect");
                    } else {
                        message.reply(new JsonObject().put("updated", true));
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    public CompletableFuture<JsonObject> generateMovementPayback(SQLConnection conn, JsonObject customer){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer customerId = customer.getInteger("customer_id");
            Double points = customer.getDouble("points");
            Double money = customer.getDouble("money");
            this.existsCustomerInPayback(conn, customerId).whenComplete((existsCustomer, error) ->{
                try {
                    if (error != null){
                        throw error;
                    }
                    if (existsCustomer) {
                        this.getCustomerData(conn, customerId).whenComplete((customerData, errorCD) -> {
                            try {
                                if (errorCD != null){
                                    throw errorCD;
                                }
                                //realiza suma de puntos y money
                                Double cDataPoints = customerData.getDouble("points");
                                Double cDataMoney = customerData.getDouble("amount");
                                Double movPoints = customer.getDouble("points");
                                Double movMoney = customer.getDouble("money");
                                String typeMovement = customer.getString("type_movement");
                                if (typeMovement.equals("I")){
                                    customer.put("points", points + cDataPoints);
                                    customer.put("money", money + cDataMoney);
                                } else if (typeMovement.equals("O")){
                                    customer.put("points", cDataMoney - points);
                                    customer.put("money", cDataMoney - money);
                                }
                                this.updateCustomerPayback(conn, customer).whenComplete((updateCustomer, errorUC) ->{
                                    try {
                                        if (errorUC != null){
                                            throw errorUC;
                                        }
                                        customer.put("points", movPoints);
                                        customer.put("money", movMoney);
                                        this.insertCustomerPaybackMoves(conn, customer).whenComplete((customerMovement, errorCM) ->{
                                            try {
                                                if (errorCM != null){
                                                    throw errorCM;
                                                }
                                                future.complete(new JsonObject()
                                                        .put("customer_id_payback", customerId)
                                                        .put("before_points_payback", cDataPoints)
                                                        .put("before_money_payback", cDataMoney)
                                                        .put("points_payback", customer.getDouble("points"))
                                                        .put("money_payback", customer.getDouble("money")));
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                future.completeExceptionally(t);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });
                    } else {
                        //registrar customer en customer_payback
                        this.insertCustomerPayback(conn, customer).whenComplete((insertCustomer, errorIC) ->{
                            try {
                                if (errorIC != null){
                                    throw errorIC;
                                }
                                this.insertCustomerPaybackMoves(conn, customer).whenComplete((customerMovement, errorCM) ->{
                                    try {
                                        if (errorCM != null){
                                            throw errorCM;
                                        }
                                        future.complete(new JsonObject()
                                                .put("customer_id_payback", customerId)
                                                .put("customer_id_payback", customerId)
                                                .put("before_points_payback", 0)
                                                .put("before_money_payback", 0)
                                                .put("points_payback", customer.getDouble("points"))
                                                .put("money_payback", customer.getDouble("money")));
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertCustomerPayback(SQLConnection conn, JsonObject customer){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(customer.getInteger("customer_id"))
                    .add(customer.getDouble("points"))
                    .add(customer.getDouble("money"))
                    .add(customer.getInteger("employee_id"));
            conn.queryWithParams(QUERY_INSERT_CUSTOMER_PAYBACK, params, reply ->{
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerPayback(SQLConnection conn, JsonObject customer){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(customer.getDouble("points"))
                    .add(customer.getDouble("money"))
                    .add(customer.getInteger("employee_id"))
                    .add(customer.getInteger("customer_id"));
            conn.updateWithParams(QUERY_UPDATE_CUSTOMER_PAYBACK, params, reply ->{
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertCustomerPaybackMoves(SQLConnection conn, JsonObject customer){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(customer.getInteger("customer_id"))
                    .add(customer.getString("type_movement"))
                    .add(customer.getDouble("points"))
                    .add(customer.getDouble("money"))
                    .add(customer.getString("motive"))
                    .add(customer.getInteger("id_parent"))
                    .add(customer.getInteger("employee_id"));
            conn.queryWithParams(QUERY_INSERT_CUSTOMER_PAYBACK_MOVES, params, reply ->{
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getCustomerData(SQLConnection conn, Integer customerId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_CUSTOMER_DATA, new JsonArray().add(customerId), reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                future.complete(reply.result().getRows().get(0));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> existsCustomerInPayback(SQLConnection conn, Integer customerId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_CUSTOMER_EXISTS_IN_CUSTOMER_PAYBACK, new JsonArray().add(customerId), reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                Boolean result = reply.result().getNumRows() > 0;
                future.complete(result);
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<JsonObject> calculatePointsBoardingPass(SQLConnection conn, Double km, Integer seatings){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.getPaybackData(conn,"boarding").whenComplete((paybackData, errorP) ->{
            try {
                if (errorP != null){
                    throw errorP;
                }
                Integer pDataKm = paybackData.getInteger("km");
                Double pDataMoney = paybackData.getDouble("money");
                Double resultPoints = km * pDataKm;
                Double resultMoney = resultPoints * pDataMoney;
                future.complete(new JsonObject()
                        .put("money", resultMoney * seatings)
                        .put("points", resultPoints * seatings));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<JsonObject> calculatePointsParcel(SQLConnection conn, Double km, Integer numPackages, Boolean reissue){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (reissue){
                future.complete(new JsonObject()
                    .put("money", 0.00)
                    .put("points", 0));
            } else {
                this.getPaybackData(conn, "parcel").whenComplete((paybackData, errorP) ->{
                    try {
                        if (errorP != null){
                            throw errorP;
                        }
                        Integer pDataKm = paybackData.getInteger("km");
                        Double pDataMoney = paybackData.getDouble("money");
                        Double resultPoints = km * pDataKm;
                        Double resultMoney = resultPoints * pDataMoney;
                        future.complete(new JsonObject()
                            .put("money", resultMoney * numPackages)
                            .put("points", resultPoints * numPackages));
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    public CompletableFuture<JsonObject> calculatePointsRental(SQLConnection conn, Double km){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.getPaybackData(conn, "rental").whenComplete((paybackData, errorP) ->{
            try {
                if (errorP != null){
                    throw errorP;
                }
                Integer pDataKm = paybackData.getInteger("km");
                Double pDataMoney = paybackData.getDouble("money");
                Double resultPoints = km * pDataKm;
                Double resultMoney = resultPoints * pDataMoney;
                future.complete(new JsonObject()
                        .put("money", resultMoney)
                        .put("points", resultPoints));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getPaybackData(SQLConnection conn, String serviceType){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(serviceType);
            conn.queryWithParams(QUERY_GET_PAYBACK_DATA, param, reply ->{
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    future.complete(new JsonObject()
                        .put("km", result.get(0).getInteger("km"))
                        .put("money", result.get(0).getDouble("money")));
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private static final String QUERY_UPDATE_PAYBACK = "UPDATE payback SET points = ?, km = ?, amount = ?, updated_at = ?, updated_by = ? WHERE service_type = ?;";

    private static final String QUERY_GET_PAYBACK_DATA = "SELECT km, money FROM payback WHERE service_type = ? LIMIT 1;";

    private static final String QUERY_GET_CUSTOMER_DATA = "SELECT points, amount FROM customer_payback WHERE customer_id = ? LIMIT 1;";

    private static final String QUERY_CUSTOMER_EXISTS_IN_CUSTOMER_PAYBACK = "SELECT customer_id FROM customer_payback WHERE customer_id = ?;";

    private static final String QUERY_INSERT_CUSTOMER_PAYBACK = "INSERT INTO customer_payback (customer_id, currency_id, points, amount, created_by) " +
            "VALUES (?, (SELECT value FROM general_setting WHERE FIELD='currency_id'), ?, ?, ?);";

    private static final String QUERY_UPDATE_CUSTOMER_PAYBACK = "UPDATE customer_payback SET points = ?, amount = ?, updated_by = ?, updated_at = NOW() WHERE customer_id = ?;";

    private static final String QUERY_INSERT_CUSTOMER_PAYBACK_MOVES = "INSERT INTO customer_payback_moves " +
            "(customer_id, type_movement, currency_id, points, amount, motive, id_parent, created_by) " +
            "VALUES " +
            "(?, ?, (SELECT value FROM general_setting WHERE FIELD='currency_id'), ?, ?, ?, ?, ?);";

}