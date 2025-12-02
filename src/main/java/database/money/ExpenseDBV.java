
package database.money;

import database.commons.DBVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ID;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gerardo Valdes Uriarte - gerardo@indqtech.com
 */
public class ExpenseDBV extends DBVerticle {

    public static final String EXPENSE_CONCEPT_ID = "expense_concept_id";
    public static final String SERVICE = "service";
    public static final String REFERENCE = "reference";
    public static final String DESCRIPTION = "description";
    public static final String ACTION = "action";
    public static final String PAYMENT_METHOD_ID = "payment_method_id";
    public static final String CURRENCY_ID = "currency_id";

    @Override
    public String getTableName() {
        return "expense";
    }

    public enum ACTIONS {
        PURCHASE("purchase"),
        INCOME("income"),
        CHANGE("change"),
        CANCEL("cancel"),
        EXPENSE("expense"),
        WITHDRAWAL("withdrawal"),
        RETURN("return"),
        VOUCHER("voucher");

        String name;

        ACTIONS(String name) {
            this.name = name;
        }

        public String getName(){ return name; }
    }

    public CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject body) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String service = (String) body.remove(SERVICE);
            ACTIONS action = ACTIONS.valueOf(body.remove(ACTION).toString().toUpperCase());

            String QUERY_GET_CASH_PAYMENT_METHOD = "SELECT id FROM payment_method WHERE is_cash = 1 AND alias = 'cash';";
            conn.query(QUERY_GET_CASH_PAYMENT_METHOD, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> resultsCashPaymentMethod = reply.result().getRows();
                    if (resultsCashPaymentMethod.isEmpty()){
                        throw new Exception("Cash payment method not exists");
                    }
                    Integer cashPaymentMethodId = resultsCashPaymentMethod.get(0).getInteger(ID);

                    this.getExpenseConcept(conn, service, action.getName()).whenComplete((resultExpenseConcept, errorExpenseConcept) -> {
                        try {
                            if (errorExpenseConcept != null){
                                throw errorExpenseConcept;
                            }

                            System.out.println("resultExpenseConcept");
                            System.out.println(resultExpenseConcept);

                            Integer expenseConceptId = resultExpenseConcept.getInteger(ID);
                            String expenseConceptDescription = resultExpenseConcept.getString(DESCRIPTION);

                            body
                                    .put(PAYMENT_METHOD_ID, cashPaymentMethodId)
                                    .put(EXPENSE_CONCEPT_ID, expenseConceptId)
                                    .put(DESCRIPTION, expenseConceptDescription)
                                    .put(REFERENCE, expenseConceptDescription);

                            String insert = this.generateGenericCreate(this.getTableName(), body);

                            conn.update(insert, replyInsert -> {
                                try {
                                    if (replyInsert.failed()){
                                        throw replyInsert.cause();
                                    }

                                    Integer expenseId = replyInsert.result().getKeys().getInteger(0);

                                    System.out.println("result esxpense");
                                    System.out.println(new JsonObject()
                                            .put("expense_id", expenseId)
                                            .put(DESCRIPTION, expenseConceptDescription));

                                    future.complete(new JsonObject()
                                            .put("expense_id", expenseId)
                                            .put(DESCRIPTION, expenseConceptDescription));

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
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getExpenseConcept(SQLConnection conn, String service, String action){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(service).add(action);
            conn.queryWithParams("SELECT * FROM expense_concept WHERE service = ? AND action = ? AND status = 1;", params, replyEC -> {
                try {
                    if (replyEC.failed()){
                        throw replyEC.cause();
                    }
                    List<JsonObject> results = replyEC.result().getRows();
                    if (results.isEmpty()){
                        throw new Exception("Expense concept not found");
                    }
                    future.complete(results.get(0));
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

}
