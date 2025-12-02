/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.geo;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.ID;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CityDBV extends DBVerticle {

    public static final String ACTION_GET_CITIES_TERMINALS = "ParcelsManifestDBV.getCitiesTerminals";

    @Override
    public String getTableName() {
        return "city";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_GET_CITIES_TERMINALS:
                this.getCitiesTerminals(message);
                break;
        }
    }

    private void getCitiesTerminals(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_GET_ACTIVE_CITIES, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> cities = reply.result().getRows();

                    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                    for (JsonObject city : cities) {
                        tasks.add(getTerminals(city));
                    }

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }

                            message.reply(new JsonArray(cities));
                        } catch (Throwable t) {
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<Boolean> getTerminals(JsonObject city) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            int cityId = city.getInteger(ID);
            this.dbClient.queryWithParams(QUERY_GET_TERMINALS_BY_CITY_ID, new JsonArray().add(cityId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> terminals = reply.result().getRows();
                    city.put("terminals", terminals);

                    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                    for (JsonObject terminal : terminals) {
                        tasks.add(getTerminalsReceivingOf(terminal));
                    }

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            future.complete(true);
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

    private CompletableFuture<Boolean> getTerminalsReceivingOf(JsonObject branchoffice) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer branchofficeId = branchoffice.getInteger(ID);
        this.dbClient.queryWithParams(QUERY_GET_TERMINALS_RECEIVING, new JsonArray().add(branchofficeId), reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                branchoffice.put("virtuals", branches);
                future.complete(true);

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_GET_ACTIVE_CITIES = "SELECT\n" +
            "   c.*\n" +
            "FROM city c\n" +
            "INNER JOIN branchoffice b ON b.city_id = c.id\n" +
            "WHERE c.status = 1\n" +
            "   AND b.status = 1\n" +
            "GROUP BY c.id";

    private static final String QUERY_GET_TERMINALS_BY_CITY_ID = "SELECT\n" +
            "   b.*\n" +
            "FROM branchoffice b\n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "WHERE b.status = 1\n" +
            "   AND b.branch_office_type = 'T'\n" +
            "   AND c.status = 1\n" +
            "   AND c.id = ?\n" +
            "GROUP BY b.id";

    private static final String QUERY_GET_TERMINALS_RECEIVING = "SELECT\n" +
            "   b.*,\n" +
            "    c.name city_name,\n" +
            "    s.name state_name\n" +
            "FROM branchoffice_parcel_receiving_config bprc\n" +
            "INNER JOIN branchoffice b ON b.id = bprc.of_branchoffice_id\n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "INNER JOIN state s ON s.id = b.state_id\n" +
            "   AND bprc.status = 1\n" +
            "WHERE b.branch_office_type = 'T'\n" +
            "AND b.status = 1\n" +
            "AND bprc.receiving_branchoffice_id = ?\n" +
            "GROUP BY b.id\n" +
            "ORDER BY b.state_id, b.name";
}
