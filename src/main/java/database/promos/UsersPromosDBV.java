/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.promos;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import service.commons.Constants;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static database.promos.PromosDBV.*;
import static service.commons.Constants.ID;
import static service.commons.Constants.USER_ID;

/**
 *
 * @author - Gerardo Valdes Uriarte
 */
public class UsersPromosDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "UsersPromosDBV.register";
    public static final String ACTION_GET_LIST = "UsersPromosDBV.getList";
    public static final String ACTION_GET_PROMOS_BY_USER_ID = "UsersPromosDBV.getListByUserId";


    @Override
    public String getTableName() {
        return "users_promos";
    }

    public static final String PROMO_ID = "promo_id";

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_GET_LIST:
                this.getList(message);
                break;
            case ACTION_GET_PROMOS_BY_USER_ID:
                this.getListByUserId(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            Integer promoID = body.getInteger(PROMO_ID);
            Integer userID = body.getInteger(USER_ID);

            checkIfUserPromoExists(promoID, userID).whenComplete((resExists, errorExists) -> {
                try {
                    if (errorExists != null){
                        throw errorExists;
                    }

                    GenericQuery genQuery = resExists == null ?
                            this.generateGenericCreate(body) :
                            this.generateGenericUpdate(this.getTableName(),
                                    body.put("status", 1).mergeIn(resExists));

                    conn.updateWithParams(genQuery.getQuery(), genQuery.getParams(), reply -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }

                            final int id = resExists == null ? reply.result().getKeys().getInteger(0) : resExists.getInteger(ID);

                            this.commit(conn, message, new JsonObject()
                                    .put(ID, id)
                                    .put(PROMO_ID, promoID)
                                    .put(USER_ID, userID));
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
        });
    }

    private CompletableFuture<JsonObject> checkIfUserPromoExists(Integer promoID, Integer userID){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams("SELECT id FROM users_promos WHERE promo_id = ? AND user_id = ?;",
                new JsonArray().add(promoID).add(userID), replyE -> {
            try {
                if (replyE.failed()){
                    throw replyE.cause();
                }
                List<JsonObject> result = replyE.result().getRows();
                if(result.isEmpty()){
                    future.complete(null);
                } else {
                    future.complete(result.get(0));
                }
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getList(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String QUERY = QUERY_GET_LIST_AVAILABLE_PROMOS;
            JsonArray params = new JsonArray();

            Integer userID = body.getInteger(USER_ID);
            QUERY = QUERY.concat(" AND u.id = ? AND (p.customer_limit = 0 OR (p.customer_limit != 0 AND (p.customer_limit > \n" +
                    " (SELECT\n" +
                    "  SUM(usrprom.used) \n" +
                    " FROM users_promos usrprom \n" +
                    " LEFT JOIN promos prom ON prom.id = usrprom.promo_id \n" +
                    " WHERE prom.id = p.id \n" +
                    " AND usrprom.user_id = ? ))))");
            params.add(userID).add(userID);
            body.remove(USER_ID);
            body.remove(CUSTOMER_ID);
            body.remove(HAS_SPECIFIC_CUSTOMER);

            if(body.containsKey(DATE)){
                QUERY = QUERY.concat(" AND ((? BETWEEN p.since AND p.until) " +
                        " AND (p.available_days = 'all' OR p.available_days REGEXP LOWER(LEFT(DAYNAME(?), 3)))) ");
                String date = body.getString(DATE);
                params.add(date).add(date);
                body.remove(DATE);
            }

            if (body.containsKey(APPLY_TO_SPECIAL_TICKETS)){
                JsonArray specialTickets = body.getJsonArray(APPLY_TO_SPECIAL_TICKETS);
                QUERY = QUERY.concat("%s");
                if (!specialTickets.isEmpty()){
                    String availableST = "";
                    for (int i = 0; i < specialTickets.size(); i++){
                        if (i == 0){
                            availableST += " AND (FIND_IN_SET(?, p.apply_to_special_tickets) ";
                        } else {
                            availableST += " OR FIND_IN_SET(?, p.apply_to_special_tickets) ";
                        }
                        params.add(String.valueOf(specialTickets.getValue(i)));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availableST);
                }
                body.remove(APPLY_TO_SPECIAL_TICKETS);
            }

            if (body.containsKey(APPLY_TO_PACKAGE_PRICE)){
                JsonArray packagePriceNames = body.getJsonArray(APPLY_TO_PACKAGE_PRICE);
                QUERY = QUERY.concat("%s");
                if (!packagePriceNames.isEmpty()){
                    String availablePP = "";
                    for (int i = 0; i < packagePriceNames.size(); i++){
                        if (i == 0) {
                            availablePP += " AND (p.apply_to_package_price IS NULL OR FIND_IN_SET(?, p.apply_to_package_price) ";
                        } else {
                            availablePP += " OR FIND_IN_SET(?, p.apply_to_package_price) ";
                        }
                        params.add(String.valueOf(packagePriceNames.getValue(i)));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availablePP);
                }
                body.remove(APPLY_TO_PACKAGE_PRICE);
            }

            if (body.containsKey(APPLY_TO_PACKAGE_PRICE_DISTANCE)){
                JsonArray packagePriceDistances = body.getJsonArray(APPLY_TO_PACKAGE_PRICE_DISTANCE);
                QUERY = QUERY.concat("%s");
                if (!packagePriceDistances.isEmpty()){
                    String availablePP = "";
                    for (int i = 0; i < packagePriceDistances.size(); i++){
                        if (i == 0) {
                            availablePP += " AND (p.apply_to_package_price_distance IS NULL OR FIND_IN_SET(?, p.apply_to_package_price_distance) ";
                        } else {
                            availablePP += " OR FIND_IN_SET(?, p.apply_to_package_price_distance) ";
                        }
                        params.add(String.valueOf(packagePriceDistances.getInteger(i)));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availablePP);
                }
                body.remove(APPLY_TO_PACKAGE_PRICE_DISTANCE);
            }

            if (body.containsKey(APPLY_TO_PACKAGE_TYPE)){
                JsonArray packageTypes = body.getJsonArray(APPLY_TO_PACKAGE_TYPE);
                QUERY = QUERY.concat("%s");
                if (!packageTypes.isEmpty()){
                    String availablePP = "";
                    for (int i = 0; i < packageTypes.size(); i++){
                        if (i == 0) {
                            availablePP += " AND (p.apply_to_package_type IS NULL OR FIND_IN_SET(?, p.apply_to_package_type) ";
                        } else {
                            availablePP += " OR FIND_IN_SET(?, p.apply_to_package_type) ";
                        }
                        params.add(packageTypes.getInteger(i));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availablePP);
                }
                body.remove(APPLY_TO_PACKAGE_TYPE);
            }

            if (body.getString(RULE) == null || !body.containsKey(RULE)){
                QUERY = QUERY.concat(" AND p.rule IS NULL ");
                body.remove(RULE);
            }

            if(body.containsKey(NUM_PRODUCTS)){
                QUERY = QUERY.concat(" AND ((p.num_products IS NULL OR p.num_products = 0) OR ? <= p.num_products) ");
                Integer numProducts = body.getInteger(NUM_PRODUCTS);
                params.add(numProducts);
                body.remove(NUM_PRODUCTS);
            }

            for (Map.Entry<String, Object> field : body) {
                QUERY = QUERY.concat(this.buildParam(field.getKey()));
                params.add(field.getValue());
            }

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> resultList = reply.result().getRows();
                    if (resultList.isEmpty()) {
                        message.reply(new JsonObject().put("data", new JsonArray()));
                    } else {
                        message.reply(new JsonObject().put("data", new JsonArray(resultList)));
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

    private String buildParam(String field){
        String returnString = " AND %s = ? ";
        return String.format(returnString, field);
    }

    private void getListByUserId(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer userId = body.getInteger(USER_ID);
        JsonArray params = new JsonArray().add(userId);

        this.dbClient.queryWithParams(QUERY_GET_PROMO_BY_USER_ID, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    message.reply(new JsonArray());
                }
                message.reply(new JsonArray(result));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }


    private static final String QUERY_GET_LIST_AVAILABLE_PROMOS = "SELECT\n" +
            "   p.*,\n" +
            "   usrprom.usage_limit,\n" +
            "   usrprom.used,\n" +
            "   usrprom.created_by AS assigned_by,\n" +
            "   usrprom.created_at AS assigned_at\n" +
            " FROM users_promos usrprom\n" +
            " LEFT JOIN promos p ON p.id = usrprom.promo_id\n" +
            " LEFT JOIN users u ON u.id = usrprom.user_id\n" +
            " WHERE u.status = 1\n" +
            " AND p.status = 1\n" +
            " AND usrprom.status = 1\n" +
            " AND (usrprom.usage_limit = 0 OR usrprom.used < usrprom.usage_limit)\n";

    private static final String QUERY_GET_PROMO_BY_USER_ID = "SELECT \n" +
            "p.*\n" +
            "FROM users_promos up \n" +
            "LEFT JOIN promos p ON up.promo_id = p.id\n" +
            "WHERE p.status != 3 and up.user_id = ? ";

    private static final  String QUERY_DISCOUNTS_SELECT = "SELECT \n" +
            "pr.*\n" +
            "FROM promos pr \n";

    private static final  String QUERY_DISCOUNTS_SELECT_COUNT = "SELECT \n" +
            "COUNT(pr.id) as count \n" +
            "FROM promos pr \n";

    private static final String QUERY_DISCOUNTS_LEFT_JOINS = "LEFT JOIN customers_promos cp ON pr.id = cp.promo_id\n" +
            "LEFT JOIN customer c ON cp.customer_id = c.id \n";

    private static final  String QUERY_DISCOUNTS_APPEND_WHERE = "AND (c.first_name like ? OR pr.name like ? OR pr.discount_code like ? OR pr.description like ? )\n";

    private static final  String QUERY_DISCOUNTS_APPEND_WHERE_NUMBER = "AND ( c.id = ? OR pr.id = ? )\n";

    private static final  String QUERY_DISCOUNTS_WHERE = "WHERE pr.status != 3 AND pr.parent_id IS NULL ";

    private static final String QUERY_DISCOUNTS_GROUP_BY = " GROUP BY pr.id\n";

    private static final  String QUERY_DISCOUNTS_ORDER_BY = "ORDER BY ";

    private static final String QUERY_DISCOUNTS_LIMIT = "\n limit ? OFFSET ? ";

}
