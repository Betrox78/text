/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.customers;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

/**
 *
 * @author ulises
 */
public class CustomersBillingInfoDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "customer_billing_information";
    }

    public static final String GET_BY_RFC = "CustomersBillingInfoDBV.getByRFC";
    public static final String ASSIGN_TO_CUSTOMER = "CustomersBillingInfoDBV.assignToCustomer";
    public static final String GET_USO_CFDI = "CustomersBillingInfoDBV.getUsoCFDI";
    public static final String GET_REGIMEN_FISCAL = "CustomersBillingInfoDBV.getRegimenFiscal";
    public static final String ACTION_REMOVE_RELATION = "CustomersBillingInfoDBV.removeRelation";
    public static final String ACTION_SEARCH_BY_NAME_AND_RFC = "CustomersBillingInfoDBV.searchByNameRFC";


    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case GET_BY_RFC:
                this.getByRFC(message);
                break;
            case ASSIGN_TO_CUSTOMER:
                this.assignToCustomer(message);
                break;
            case GET_USO_CFDI:
                this.getUsoCFDI(message);
                break;
            case GET_REGIMEN_FISCAL:
                this.getRegimenFiscal(message);
                break;
            case ACTION_REMOVE_RELATION:
                this.removeRelation(message);
                break;
            case ACTION_SEARCH_BY_NAME_AND_RFC:
                this.searchByNameRFC(message);
                break;
        }
    }

    private void getByRFC(Message<JsonObject> message) {
        String RFC = message.body().getString(_RFC);
        this.dbClient.queryWithParams(QUERY_GET_BY_RFC, new JsonArray().add(RFC), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("Element not found");
                }
                message.reply(reply.result().getRows().get(0));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void assignToCustomer(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            GenericQuery assign = generateGenericCreateSendTableName("customer_customer_billing_info", body);
            startTransaction(message, conn -> {
                try {
                    conn.updateWithParams(assign.getQuery(), assign.getParams(), reply -> {
                       try {
                           if (reply.failed()) {
                               throw reply.cause();
                           }
                           this.commit(conn, message, new JsonObject()
                                   .put(ID, reply.result().getKeys().getInteger(0)));
                       } catch (Throwable t) {
                           this.rollback(conn, t, message);
                       }
                    });
                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void getUsoCFDI(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_USO_CFDI, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(new JsonArray(reply.result().getRows()));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void getRegimenFiscal(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_REGIMEN_FISCAL, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(new JsonArray(reply.result().getRows()));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void removeRelation(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int customerId = body.getInteger(_CUSTOMER_ID);
            int customerBillingInformationId = body.getInteger(_CUSTOMER_BILLING_INFORMATION_ID);
            startTransaction(message, conn -> {
                conn.updateWithParams(QUERY_DELETE_RELATION, new JsonArray().add(customerId).add(customerBillingInformationId), reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        this.commit(conn, message, new JsonObject());
                    } catch (Throwable t) {
                        this.rollback(conn, t, message);
                    }
                });
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void searchByNameRFC(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String search = body.getString("search");
            Integer limit = body.getInteger("limit");

            doSearchByNameRFC(search, limit).whenComplete((results, err) -> {
                try {
                    if (err != null) {
                        throw err;
                    }
                    message.reply(new JsonArray(results));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Exception ex) {
            reportQueryError(message, ex);
        }
    }

    private CompletableFuture<List<JsonObject>> doSearchByNameRFC(String search, Integer limit) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String term = (search == null) ? "" : search.trim();
            String like = "%" + term.replace(' ', '%') + "%";
            int lim = (limit != null && limit > 0) ? limit : 30;

            JsonArray params = new JsonArray()
                    .add(like) // name LIKE ?
                    .add(like) // rfc  LIKE ?
                    .add(lim); // LIMIT ?

            dbClient.queryWithParams(QUERY_SEARCH_CBI_BY_NAME_RFC, params, reply -> {
                try {
                    if (reply.failed()) throw reply.cause();
                    future.complete(reply.result().getRows());
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private static final String QUERY_GET_BY_RFC = "SELECT cbi.*, " +
            "c_UsoCFDI.c_UsoCFDI, c_UsoCFDI.description AS c_UsoCFDI_description, " +
            "c_RegimenFiscal.c_RegimenFiscal, c_RegimenFiscal.description AS c_RegimenFiscal_description, " +
            "country.name AS country_name, state.name AS state_name, " +
            "street.name AS street_name," +
            "county.name AS county_name, city.name AS city_name " +
            "FROM customer_billing_information AS cbi " +
            "LEFT JOIN c_UsoCFDI ON cbi.c_UsoCFDI_id=c_UsoCFDI.id " +
            "LEFT JOIN c_RegimenFiscal ON cbi.c_RegimenFiscal_id=c_RegimenFiscal.id " +
            "LEFT JOIN country ON cbi.country_id=country.id " +
            "LEFT JOIN state ON cbi.state_id=state.id " +
            "LEFT JOIN county ON cbi.county_id=county.id " +
            "LEFT JOIN city ON cbi.city_id=city.id " +
            "LEFT JOIN street ON cbi.street_id = street.id  " +
            "WHERE cbi.status != 3 AND cbi.rfc = ?";

    private static final String QUERY_GET_USO_CFDI = "SELECT * FROM c_UsoCFDI;";

    private static final String QUERY_GET_REGIMEN_FISCAL = "SELECT * FROM c_RegimenFiscal;";

    private static final String QUERY_DELETE_RELATION = "DELETE FROM customer_customer_billing_info WHERE customer_id = ? AND customer_billing_information_id = ?;";

    private static final String QUERY_SEARCH_CBI_BY_NAME_RFC =
            "SELECT id, customer_id, name, rfc, address, zip_code, legal_person " +
                    "FROM customer_billing_information " +
                    "WHERE status = 1 " +
                    "  AND (name LIKE ? OR rfc LIKE ?) " +
                    "ORDER BY name ASC " +
                    "LIMIT ?";

}
