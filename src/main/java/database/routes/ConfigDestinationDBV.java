/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.routes;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.shipments.ShipmentsDBV;
import database.shipments.ShipmentsDBV.SHIPMENT_TYPES;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static database.shipments.ShipmentsDBV.TRAVEL_DATE;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigDestinationDBV extends DBVerticle {

    public static final String ACTION_DESTINATION_PRICES = "ConfigDestinationDBV.destinationPrices";
    public static final String ACTION_GET_SEGMENTS = "ConfigDestinationDBV.getSegments";

    public static final String CONFIG_ROUTE_ID = "config_route_id";

    @Override
    public String getTableName() {
        return "config_destination";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_DESTINATION_PRICES:
                this.getDestinationPrices(message);
                break;
            case ACTION_GET_SEGMENTS:
                this.getSegments(message);
                break;
        }
    }

    private void getDestinationPrices(Message<JsonObject> message) {
        int configRouteId = message.body().getInteger("config_route_id");
        if (configRouteId <= 0) {
            reportQueryError(message, new Throwable("Send config route id"));
            return;
        }
        JsonArray params = new JsonArray().add(configRouteId);
        this.dbClient.queryWithParams(QUERY_DESTINATION_BY_ROUTE, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> destinations = reply.result().getRows();
                int length = destinations.size();
                List<CompletableFuture<List<JsonObject>>> tasks = new ArrayList<CompletableFuture<List<JsonObject>>>();
                for (int i = 0; i < length; i++) {
                    JsonObject destination = destinations.get(i);
                    tasks.add(getDestinationPricesList(destination));
                }
                CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[length]));
                all.whenComplete((s, t) -> {
                    message.reply(new JsonArray(destinations));
                });

            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
    }

    private CompletableFuture<List<JsonObject>> getDestinationPricesList(JsonObject destination) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(destination.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_DESTINATION_PRICES, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> prices = reply.result().getRows();
                destination.put("config_ticket", prices);
                future.complete(prices);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }

    private void getSegments(Message<JsonObject> message){
        try {

            JsonObject body = message.body();
            String shipmentType = body.getString("shipment_type");
            StringBuilder QUERY = new StringBuilder(QUERY_GET_SEGMENTS);
            JsonArray params = new JsonArray().add(shipmentType);

            this.completeQuery(QUERY, params, body, TRAVEL_DATE, " AND DATE(sr.travel_date) = ? ");
            this.completeQuery(QUERY, params, body, TERMINAL_ID, SHIPMENT_TYPES.LOAD.getName().equals(shipmentType) ? " AND cd.terminal_origin_id = ? " : " AND cd.terminal_destiny_id = ? ");
            this.completeQuery(QUERY, null, null, null, SEGMENTS_GROUP_BY_ORDER_BY);

            this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();
                    message.reply(new JsonArray(result));

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



    private static final String QUERY_DESTINATION_BY_ROUTE = "SELECT \n"
            + " config_destination.id AS id, config_destination.travel_time AS travel_time, \n"
            + " origin.name AS terminal_origin_name, origin.prefix AS terminal_origin_prefix, \n"
            + " destiny.name AS terminal_destiny_name, destiny.prefix AS terminal_destiny_prefix \n"
            + " FROM config_destination \n"
            + " RIGHT JOIN branchoffice AS origin ON origin.id=config_destination.terminal_origin_id \n"
            + " RIGHT JOIN branchoffice AS destiny ON destiny.id=config_destination.terminal_destiny_id \n"
            + " WHERE config_destination.config_route_id = ? AND config_destination.status = 1";

    private static final String QUERY_DESTINATION_PRICES = "SELECT \n"
            + " config_ticket_price.id AS id, config_ticket_price.amount AS amount, \n"
            + " config_ticket_price.discount AS discount, config_ticket_price.total_amount AS total_amount, \n"
            + " config_ticket_price.price_status AS price_status, config_ticket_price.price_status AS price_status, \n"
            + " config_ticket_price.special_ticket_id AS special_ticket_id, \n"
            + " config_ticket_price.config_prices_route_id AS config_prices_route_id, \n"
            + " special_ticket.base AS base, \n"
            + " special_ticket.name AS special_ticket_name \n"
            + " FROM config_ticket_price \n"
            + " RIGHT JOIN special_ticket ON special_ticket.id=config_ticket_price.special_ticket_id \n"
            + " WHERE config_ticket_price.config_destination_id = ? "
            + " AND special_ticket.status = 1 "
            + " AND config_ticket_price.status = 1 "
            + " AND config_ticket_price.price_status = 1";

    private static final String QUERY_GET_SEGMENTS = "SELECT DISTINCT\n" +
            " cr.name AS config_route_name,\n" +
            " cd.id AS config_destination_id,\n" +
            " bo.prefix AS prefix_origin,\n" +
            " bd.prefix AS prefix_destiny\n" +
            "FROM shipments ship\n" +
            "LEFT JOIN schedule_route sr ON sr.id = ship.schedule_route_id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "LEFT JOIN branchoffice bo ON bo.id = cd.terminal_origin_id\n" +
            "LEFT JOIN branchoffice bd ON bd.id = cd.terminal_destiny_id\n" +
            "LEFT JOIN config_route cr ON cr.id = cd.config_route_id\n" +
            "WHERE cd.order_destiny = cd.order_origin + 1 \n" +
            "AND ship.shipment_type = ? ";

    private static final String SEGMENTS_GROUP_BY_ORDER_BY = " GROUP BY cd.id\n" +
            " ORDER BY cd.config_route_id, cd.order_origin, cd.order_destiny;";

}
