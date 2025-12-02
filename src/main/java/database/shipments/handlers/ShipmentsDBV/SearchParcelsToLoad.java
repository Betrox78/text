package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class SearchParcelsToLoad extends DBHandler<ShipmentsDBV> {

    public SearchParcelsToLoad(ShipmentsDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String parcelTrackingCode = body.getString(_PARCEL_TRACKING_CODE);
            Integer scheduleRouteId = body.getInteger(_SCHEDULE_ROUTE_ID);
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Integer terminalDestinyId = body.getInteger(_TERMINAL_DESTINY_ID);

            String QUERY = GET_PARCELS_TO_LOAD;
            JsonArray params = new JsonArray().add(terminalId).add(scheduleRouteId);
            if (Objects.nonNull(parcelTrackingCode)) {
                params.add(parcelTrackingCode);
                QUERY += " AND p.parcel_tracking_code =  ?";
            } else {
                params.add(terminalDestinyId);
                QUERY += " AND p.terminal_destiny_id = ?";
            }

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> parcels = reply.result().getRows();
                    if (parcels.isEmpty()) {
                        message.reply(new JsonArray());
                        return;
                    }

                    List<CompletableFuture<Boolean>> tasksPackages = new ArrayList<>();
                    for (JsonObject parcel : parcels) {
                        tasksPackages.add(getPackagesListPackages(parcel, terminalId));
                    }

                    CompletableFuture.allOf(tasksPackages.toArray(new CompletableFuture[tasksPackages.size()])).whenComplete((res, err) -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            message.reply(new JsonArray(parcels));
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

    private CompletableFuture<Boolean> getPackagesListPackages(JsonObject parcel, Integer terminalId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelId = parcel.getInteger(_PARCEL_ID);
            JsonArray params = new JsonArray().add(parcelId).add(terminalId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGES_TO_SHIPMENT_BY_TERMINAL_AND_PARCEL,params,reply->{
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> resultList = reply.result().getRows();
                    parcel.put(_PACKAGES, resultList);
                    future.complete(true);

                }catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private static final String GET_PARCELS_TO_LOAD = "SELECT DISTINCT\n" +
            "   ppt.parcel_id,\n" +
            "   p.parcel_status,\n" +
            "   p.terminal_origin_id,\n" +
            "   p.terminal_destiny_id,\n" +
            "   p.total_packages, \n" +
            "   TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date) AS delivery_time,\n" +
            "   CONCAT(bo.prefix, ' - ', bd.prefix) AS segment,\n" +
            "   p.parcel_tracking_code, \n" +
            "   CONCAT(c.first_name, ' ', c.last_name) customer_full_name, \n" +
            "   p.waybill, \n" +
            "   p.status, \n" +
            "   p.created_at\n" +
            " FROM parcels_packages_tracking ppt\n" +
            " INNER JOIN parcels p ON p.id = ppt.parcel_id\n" +
            " INNER JOIN customer c ON c.id = p.customer_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            " INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            " WHERE (p.parcel_status IN (0, 7, 8) AND pp.package_status IN (0, 8)) \n" +
            " AND ? = (SELECT ppt2.terminal_id FROM parcels_packages_tracking ppt2 \n" +
            "       WHERE parcel_id = p.id AND action NOT IN ('printed', 'incidence') \n" +
            "       ORDER BY ppt2.id DESC LIMIT 1)\n" +
            " AND p.terminal_destiny_id NOT IN (SELECT srd.id FROM schedule_route_destination srd \n" +
            "                               WHERE srd.schedule_route_id = ?)\n";

    private static final String QUERY_GET_PACKAGES_TO_SHIPMENT_BY_TERMINAL_AND_PARCEL = "SELECT \n" +
            "\tpp.id, \n" +
            "\tpp.shipping_type, \n" +
            "\tpp.package_status, \n" +
            "\tpp.package_code, \n" +
            "\tpp.total_amount, \n" +
            "\tpp.weight, \n" +
            "\tpp.height, \n" +
            "\tpp.width, \n" +
            "\tpp.length, \n" +
            "\tpp.notes, \n" +
            "\tpp.status, \n" +
            "\tpt.name AS package_type \n" +
            "FROM parcels_packages_tracking ppt\n" +
            "LEFT JOIN parcels_packages pp ON pp.id = ppt.parcel_package_id \n" +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id \n" +
            "WHERE pp.parcel_id = ?\n" +
            "AND (pp.package_status = 0 \n" +
            "   OR (pp.package_status = 8 AND ? = (SELECT ppt2.terminal_id FROM parcels_packages_tracking ppt2 \n" +
            "       WHERE ppt2.parcel_package_id = pp.id AND ppt2.parcel_id = pp.parcel_id \n" +
            "       AND ppt2.terminal_id IS NOT NULL ORDER BY ppt2.id DESC LIMIT 1))) \n" +
            "GROUP BY pp.id";
}
