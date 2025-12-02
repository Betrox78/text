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
import utils.UtilsDate;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static database.promos.PromosDBV.*;

/**
 *
 * @author Indq tech - Gerardo Valdes Uriarte
 */
public class CustomersPromosDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "CustomersPromosDBV.register";
    public static final String ACTION_GET_LIST = "CustomersPromosDBV.getList";
    public static final String ACTION_GET_CUSTOMERS_LIST = "CustomersPromosDBV.getListCustomers";
    public static final String ACTION_GET_PROMOS_BY_CUSTOMER_ID = "CustomersPromosDBV.getListByCustomerId";


    @Override
    public String getTableName() {
        return "customers_promos";
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
            case ACTION_GET_CUSTOMERS_LIST:
                this.getListCustomers(message);
                break;
            case ACTION_GET_PROMOS_BY_CUSTOMER_ID:
                this.getListByCustomerId(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();

            dbClient.queryWithParams(QUERY_GET_COMPUEST_PROMO, new JsonArray().add(body.getInteger(PROMO_ID))
                    .add(body.getInteger(CUSTOMER_ID)), replyGet -> {
                try {
                    if (replyGet.failed()) {
                        throw replyGet.cause();
                    }
                    boolean newElement = true;
                    final Integer[] customerPromoId = new Integer[1];
                    GenericQuery genQuery = this.generateGenericCreate(body);
                    List<JsonObject> results = replyGet.result().getRows();
                    if (!results.isEmpty()) {
                        JsonObject customerPromo = results.get(0);
                        customerPromoId[0] = customerPromo.getInteger(Constants.ID);
                        body.put(Constants.ID, customerPromoId[0])
                                .put(Constants.STATUS, 1)
                                .put(Constants.UPDATED_BY, body.getInteger(Constants.CREATED_BY))
                                .put(Constants.UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                        genQuery = this.generateGenericUpdate(this.getTableName(), body);
                        newElement = false;
                    }

                    boolean finalNewElement = newElement;
                    conn.updateWithParams(genQuery.getQuery(), genQuery.getParams(), reply -> {
                        try {
                            if (reply.failed()){
                                throw new Exception(reply.cause());
                            }

                            JsonArray resultsCP = reply.result().getKeys();
                            if (finalNewElement)
                                customerPromoId[0] = resultsCP.getInteger(0);

                            this.commit(conn, message, new JsonObject().put(Constants.ID, customerPromoId[0]));
                        } catch (Exception e){
                            e.printStackTrace();
                            this.rollback(conn, e, message);
                        }
                    });

                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private void getList(Message<JsonObject> message) {
        JsonObject body = message.body();

        String QUERY = QUERY_GET_LIST_AVAILABLE_PROMOS;
        JsonArray params = new JsonArray();

        if(body.containsKey(CUSTOMER_ID)){
            QUERY = QUERY.concat(" AND c.id = ? ");
            Integer customerId = body.getInteger(CUSTOMER_ID);
            params.add(customerId);
            QUERY = QUERY.concat(" AND (p.customer_limit = 0 OR (p.customer_limit != 0 AND (p.customer_limit > \n" +
                    " (SELECT\n" +
                    "  SUM(cusprom.used) \n" +
                    " FROM customers_promos cusprom \n" +
                    " LEFT JOIN promos prom ON prom.id = cusprom.promo_id \n" +
                    " WHERE prom.id = p.id \n" +
                    " AND cusprom.customer_id = ? \n" +
                    " AND cusprom.is_promo_customer = FALSE) \n" +
                    " OR (p.customer_limit > (SELECT COUNT(bp.id) FROM boarding_pass bp WHERE bp.customer_id = ? AND bp.promo_id = p.id))))) ");
            params.add(customerId).add(customerId);
            body.remove(CUSTOMER_ID);
        }

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
                    throw new Exception(reply.cause());
                }
                List<JsonObject> resultList = reply.result().getRows();
                if (resultList.isEmpty()){
                    message.reply(new JsonArray());
                } else {
                    message.reply(new JsonArray(resultList));
                }
            } catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private String buildParam(String field){
        String returnString = " AND %s = ? ";
        return String.format(returnString, field);
    }

    private void getListCustomers(Message<JsonObject> message) {
        JsonObject body = message.body();

        int limit = body.getInteger("limit");
        int page = body.getInteger("page");
        String search = body.getString("search");
        Integer numberId = body.getInteger("search_id");

        int lengthSearch = search.length();
        String orderBy = body.getString("orderBy");

        String QUERY = QUERY_DISCOUNTS_SELECT;


        JsonArray params = new JsonArray();

        if(lengthSearch > 0) {
            QUERY = QUERY.concat(QUERY_DISCOUNTS_LEFT_JOINS);
        }
        if(numberId != null){
            QUERY = QUERY.concat(QUERY_DISCOUNTS_LEFT_JOINS);
        }

        //WHERE clause
        QUERY = QUERY.concat(QUERY_DISCOUNTS_WHERE);

        if(lengthSearch > 0) {
            QUERY = QUERY.concat(QUERY_DISCOUNTS_APPEND_WHERE);
            params.add(search).add(search).add(search).add(search);
        }

        if(numberId != null){
            QUERY = QUERY.concat(QUERY_DISCOUNTS_APPEND_WHERE_NUMBER);
            params.add(numberId).add(numberId);
        }

        //GROUP BY clause
        QUERY = QUERY.concat(QUERY_DISCOUNTS_GROUP_BY);

        //ORDER BY clause
        QUERY = QUERY.concat(QUERY_DISCOUNTS_ORDER_BY).concat(orderBy);

        // LIMIT clause
        QUERY = QUERY.concat(QUERY_DISCOUNTS_LIMIT);
        params.add(limit).add((page - 1) * limit);

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    JsonObject res = new JsonObject()
                            .put("count", 0)
                            .put("items", limit)
                            .put("results", result);
                    message.reply(res);
                }

                String  QUERY_COUNT = QUERY_DISCOUNTS_SELECT_COUNT;

                if(lengthSearch > 0) {
                    QUERY_COUNT = QUERY_COUNT.concat(QUERY_DISCOUNTS_LEFT_JOINS);
                }
                if(numberId != null){
                    QUERY_COUNT = QUERY_COUNT.concat(QUERY_DISCOUNTS_LEFT_JOINS);
                }

                //WHERE clause

                QUERY_COUNT = QUERY_COUNT.concat(QUERY_DISCOUNTS_WHERE);
                if(lengthSearch > 0) {
                    QUERY_COUNT = QUERY_COUNT.concat(QUERY_DISCOUNTS_APPEND_WHERE);
                }

                if(numberId != null){
                    QUERY_COUNT = QUERY_COUNT.concat(QUERY_DISCOUNTS_APPEND_WHERE_NUMBER);
                }

                params.remove(params.size() -1);
                params.remove(params.size() -1);

                this.dbClient.queryWithParams(QUERY_COUNT ,params, replyCount -> {
                    try {
                        if (reply.failed()) {
                            throw replyCount.cause();
                        }
                        List<JsonObject> count = replyCount.result().getRows();
                        int countInTable = count.get(0).getInteger("count");
                        if(count.isEmpty()) {
                            JsonObject res = new JsonObject()
                                    .put("count", 0)
                                    .put("items", limit)
                                    .put("results", result);
                            message.reply(res);
                        }

                        JsonObject res = new JsonObject()
                                .put("count", countInTable)
                                .put("items", limit)
                                .put("results", result);

                        message.reply(res);

                    } catch (Throwable t) {
                        t.printStackTrace();
                        reportQueryError(message,t);
                    }
                });

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getListByCustomerId(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer customerId = body.getInteger("id");
        JsonArray params = new JsonArray().add(customerId);

        this.dbClient.queryWithParams(QUERY_GET_PROMO_BY_CUSTOMER_ID, params, reply -> {
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
            "   cusprom.usage_limit,\n" +
            "   cusprom.used,\n" +
            "   cusprom.created_by AS assigned_by,\n" +
            "   cusprom.created_at AS assigned_at\n" +
            " FROM customers_promos cusprom\n" +
            " LEFT JOIN promos p ON p.id = cusprom.promo_id\n" +
            " LEFT JOIN customer c ON c.id = cusprom.customer_id\n" +
            " WHERE c.status = 1\n" +
            " AND p.status = 1\n" +
            " AND cusprom.status = 1\n" +
            " AND (cusprom.usage_limit = 0 OR cusprom.used < cusprom.usage_limit)\n";

    private static final String QUERY_GET_COMPUEST_PROMO = "SELECT * FROM customers_promos\n" +
            "WHERE promo_id = ? and customer_id = ? LIMIT 1;";

    private static final String QUERY_GET_CUSTOMER_DISCOUNT = "SELECT \n" +
            "IFNULL(concat( c.id , ' | ' ,concat(c.first_name , ' ', c.last_name)), 'N/A') as customer_name  \n" +
            "FROM customers_promos cp\n" +
            "LEFT JOIN customer c ON cp.customer_id = c.id\n" +
            "WHERE cp.promo_id = ? ";

    private static final String QUERY_GET_PROMO_BY_CUSTOMER_ID = "SELECT \n" +
            "p.*\n" +
            "FROM customers_promos cp \n" +
            "LEFT JOIN promos p ON cp.promo_id = p.id\n" +
            "WHERE p.status != 3 and cp.customer_id = ? ";

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
