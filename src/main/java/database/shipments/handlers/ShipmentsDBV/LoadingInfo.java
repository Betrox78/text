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

public class LoadingInfo extends DBHandler<ShipmentsDBV> {

    public LoadingInfo(ShipmentsDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer shipmentId = body.getInteger(_SHIPMENT_ID);

            getShipmentInfo(shipmentId).whenComplete((shipment, errShipment) -> {
                try {
                    if (errShipment != null) {
                        throw errShipment;
                    }

                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                    tasks.add(getHitchedTrailers(shipment));
                    tasks.add(getVehicleInfo(shipment));
                    Integer driverId = shipment.getInteger("driver_id");
                    tasks.add(getDriverInfo(shipment, driverId, "driver"));

                    Integer secondDriverId = shipment.getInteger("second_driver_id");
                    if (Objects.nonNull(secondDriverId)) {
                        tasks.add(getDriverInfo(shipment, secondDriverId, "second_driver"));
                    }


                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            message.reply(shipment);
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

    private CompletableFuture<JsonObject> getShipmentInfo(Integer shipmentId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_GET_SHIPMENT_INFO, new JsonArray().add(shipmentId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Shipment info not found");
                    }
                    future.complete(result.get(0));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getDriverInfo(JsonObject shipment, Integer driverId, String value) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_GET_DRIVER_INFO, new JsonArray().add(driverId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Driver info not found");
                    }
                    shipment.put(value, result.get(0));
                    future.complete(shipment);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getHitchedTrailers(JsonObject shipment) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer scheduleRouteId = shipment.getInteger(_SCHEDULE_ROUTE_ID);
        try {
            this.dbClient.queryWithParams(QUERY_GET_HITCHED_TRAILERS, new JsonArray().add(scheduleRouteId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> trailers = reply.result().getRows();
                    shipment.put("hitched_trailers", trailers);
                    future.complete(shipment);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getVehicleInfo(JsonObject shipment) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer vehicleId = shipment.getInteger(_VEHICLE_ID);
        try {
            this.dbClient.queryWithParams(QUERY_GET_VEHICLE_INFO, new JsonArray().add(vehicleId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Vehicle info not found");
                    }
                    shipment.put("vehicle", results.get(0));
                    future.complete(shipment);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT\n" +
            "   s.id AS shipment_id,\n" +
            "    s.schedule_route_id,\n" +
            "    cr.name AS config_route_name,\n" +
            "    cd.order_origin,\n" +
            "    cd.order_destiny,\n" +
            "    s.driver_id,\n" +
            "    v.id AS vehicle_id,\n" +
            "    s.second_driver_id,\n" +
            "    tl.travel_log_code,\n" +
            "    tl.id AS travel_logs_id,\n" +
            "    bo.id AS origin_terminal_id,\n" +
            "    bo.prefix AS origin_terminal_prefix,\n" +
            "    bd.id AS destiny_terminal_id,\n" +
            "    bd.prefix AS destiny_terminal_prefix,\n" +
            "    s.left_stamp,\n" +
            "    s.right_stamp,\n" +
            "    s.replacement_stamp,\n" +
            "    s.additional_stamp,\n" +
            "    s.fifth_stamp,\n" +
            "    s.sixth_stamp,\n" +
            "    s.second_left_stamp,\n" +
            "    s.second_right_stamp,\n" +
            "    s.second_replacement_stamp,\n" +
            "    s.second_additional_stamp,\n" +
            "    s.second_fifth_stamp,\n" +
            "    s.second_sixth_stamp\n" +
            "FROM shipments s\n" +
            "INNER JOIN travel_logs tl ON tl.load_id = s.id\n" +
            "INNER JOIN schedule_route sr ON sr.id = s.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = s.terminal_id\n" +
            "INNER JOIN config_destination cd ON cd.terminal_origin_id = srd.terminal_origin_id\n" +
            "   AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "    AND cd.order_destiny = cd.order_origin + 1\n" +
            "INNER JOIN branchoffice bo ON bo.id = cd.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = cd.terminal_destiny_id\n" +
            "INNER JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "WHERE s.id = ?;";

    private final static String QUERY_GET_HITCHED_TRAILERS = "SELECT\n" +
            "   t.id,\n" +
            "   t.name,\n" +
            "   t.economic_number,\n" +
            "   t.plate,\n" +
            "   tr.Clave_tipo_remolque,\n" +
            "   tr.Remolque_o_semirremolque\n" +
            "FROM shipments_trailers st\n" +
            "INNER JOIN trailers t ON t.id = st.trailer_id\n" +
            "INNER JOIN c_SubTipoRem tr ON tr.id = t.c_SubTipoRem_id\n" +
            "WHERE st.schedule_route_id = ?\n" +
            "AND st.action IN ('assign', 'transfer', 'hitch')\n" +
            "AND st.latest_movement IS TRUE\n" +
            "AND t.in_use IS TRUE;";

    private final static String QUERY_GET_DRIVER_INFO = "SELECT\n" +
            "   id,\n" +
            "   name AS first_name,\n" +
            "   last_name,\n" +
            "   cellphone\n" +
            "FROM employee e\n" +
            "WHERE id = ?\n" +
            "   AND status = 1;";

    private final static String QUERY_GET_VEHICLE_INFO = "SELECT\n" +
            "   id,\n" +
            "   alias,\n" +
            "   can_use_trailer,\n" +
            "   description,\n" +
            "   economic_number,\n" +
            "   model,\n" +
            "   name,\n" +
            "   work_type\n" +
            "FROM vehicle v\n" +
            "WHERE id = ?\n" +
            "   AND status = 1;";
}
