package database.routes.handlers.TravelTrackingDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.routes.TravelTrackingDBV;
import database.routes.handlers.enums.DESTINATION_STATUS;
import database.routes.handlers.enums.SCHEDULE_STATUS;
import database.routes.handlers.enums.TRAVELTRACKING_STATUS;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_TYPE;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.*;
import static database.routes.TravelTrackingDBV.*;
import static database.shipments.ShipmentsDBV.LOAD_ID;
import static service.commons.Constants.*;

public class InitLoad extends DBHandler<TravelTrackingDBV> {

    public InitLoad(TravelTrackingDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message){
        startTransaction(message,conn->{
            try {
                JsonObject body = message.body();
                Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer createdBy = body.getInteger(CREATED_BY);
                String origin = body.getString(_ORIGIN, "app");
                Integer driverId = body.getInteger(DRIVER_ID);
                Integer secondDriverId = body.getInteger(SECOND_DRIVER_ID);

                changeRouteStatus(conn, scheduleRouteId, terminalId, createdBy).whenComplete((resultChangeStatus, errorChangeStatus)->{
                    try {
                        if(errorChangeStatus!=null){
                            throw errorChangeStatus;
                        }
                        registerShipment(conn, scheduleRouteId, terminalId, origin, driverId, secondDriverId, createdBy).whenComplete((resultRegisterShipment, errorRegisterShipment)-> {
                            try {
                                if(errorRegisterShipment != null){
                                    throw errorRegisterShipment;
                                }
                                this.commit(conn, message, resultRegisterShipment);
                            }catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<Boolean> changeRouteStatus(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        this.getScheduleStatus(conn, scheduleRouteId).whenComplete((scheduleStatus, errSS) -> {
            try {
                if (errSS != null) {
                    throw errSS;
                }

                this.getDestinationStatus(conn, scheduleRouteId, terminalId).whenComplete((destinationStatus, errDS) -> {
                    try {
                        if (errDS != null) {
                            throw errDS;
                        }

                        this.getDestinations(scheduleRouteId, terminalId).whenComplete((destinations, errorDestinations)->{
                            try{
                                if(errorDestinations != null){
                                    throw errorDestinations;
                                }

                                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                                for(JsonObject destination:destinations){
                                    Integer destinationId = destination.getInteger(ID);
                                    tasks.add(this.updateDestination(conn, scheduleRouteId, destinationId, createdBy));
                                }
                                tasks.add(this.updateRouteStatus(conn, scheduleRouteId, createdBy));
                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((__, errTasks) -> {
                                    try {
                                        if (errTasks != null) {
                                            throw errTasks;
                                        }
                                        future.complete(true);
                                    } catch(Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch(Throwable t){
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
        });
        return future;
    }

    private CompletableFuture<SCHEDULE_STATUS> getScheduleStatus(SQLConnection conn, Integer scheduleRouteId){
        CompletableFuture<SCHEDULE_STATUS> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_STATUS_TRACKING_TRAVEL, new JsonArray().add(scheduleRouteId), reply -> {
            try{
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("STATUS NOT FOUND FOR THAT ROUTE AND DESTINATION");
                }

                SCHEDULE_STATUS scheduleStatus = SCHEDULE_STATUS.fromValue(reply.result().getRows().get(0).getString(_SCHEDULE_STATUS));
                if (!scheduleStatus.equals(SCHEDULE_STATUS.SCHEDULED) && !scheduleStatus.equals(SCHEDULE_STATUS.READY_TO_LOAD)) {
                    throw new Exception("CANNOT BE CHANGED TO LOADING ");
                }
                future.complete(scheduleStatus);
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<DESTINATION_STATUS> getDestinationStatus(SQLConnection conn, Integer scheduleRouteId, Integer terminalId){
        CompletableFuture<DESTINATION_STATUS> future = new CompletableFuture<>();
        conn.queryWithParams(GET_DESTINATION_ORIGIN_STATUS, new JsonArray().add(scheduleRouteId).add(terminalId), reply -> {
            try{
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("STATUS NOT FOUND FOR THAT DESTINATION");
                }

                DESTINATION_STATUS destinationStatus = DESTINATION_STATUS.fromValue(reply.result().getRows().get(0).getString(_DESTINATION_STATUS));
                if (!destinationStatus.equals(DESTINATION_STATUS.SCHEDULED) && !destinationStatus.equals(DESTINATION_STATUS.READY_TO_LOAD)) {
                    throw new Exception("CANNOT BE CHANGED TO LOADING ");
                }
                future.complete(destinationStatus);
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getDestinations(Integer scheduleRouteId, Integer terminalId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(GET_ROUTE_DESTINATIONS_BY_TERMINAL_TYPE + " AND terminal_origin_id = ? ", new JsonArray().add(scheduleRouteId).add(terminalId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()){
                    throw new Exception("DESTINATIONS NOT FOUND");
                }

                future.complete(result);
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateDestination(SQLConnection conn, Integer scheduleRouteId, Integer scheduleRouteDestinationId, Integer createdBy){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        try{
            conn.updateWithParams(UPDATE_SCHEDULE_ROUTE_DESTINATIONS_LOADING, new JsonArray().add(scheduleRouteDestinationId), reply ->{
                try{
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    GenericQuery update = this.generateGenericCreate("travel_tracking", new JsonObject().put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                            .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId)
                            .put(CREATED_BY, createdBy)
                            .put(Constants.STATUS, TRAVELTRACKING_STATUS.LOADING.getValue()));
                    conn.updateWithParams(update.getQuery(), update.getParams(), replyInsertTravelTracking -> {
                        try {
                            if (replyInsertTravelTracking.failed()){
                                throw replyInsertTravelTracking.cause();
                            }
                            future.complete(true);
                        } catch(Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } catch(Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch(Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateRouteStatus(SQLConnection conn, Integer scheduleRouteId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(SCHEDULE_STATUS.LOADING.getValue())
                    .add(createdBy)
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(scheduleRouteId);

            conn.updateWithParams(UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT,params,reply->{
                if(reply.succeeded()){
                    future.complete(true);
                }else {
                    future.completeExceptionally(reply.cause());
                }

            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> getShipmentsStatusByRoute(SQLConnection conn, Integer scheduleRouteId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_SHIPMENT_BY_STATUS, new JsonArray().add(scheduleRouteId), reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(!reply.result().getRows().isEmpty()){
                    throw new Exception("SHIPMENT LOADING OR DOWNLOADING IS OPEN FOR THIS ROUTE");
                }
                future.complete(true);
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> registerShipment(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, String origin, Integer driverId,
                                                           Integer secondDriverId, Integer createdBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            getShipmentsStatusByRoute(conn, scheduleRouteId).whenComplete((res, err)->{
                try {
                    if(err != null){
                        throw err;
                    }

                    GenericQuery create = this.generateGenericCreate("shipments", new JsonObject()
                            .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                            .put(TERMINAL_ID, terminalId)
                            .put(_SHIPMENT_TYPE, SHIPMENT_TYPE.load.name())
                            .put(DRIVER_ID, driverId)
                            .put(SECOND_DRIVER_ID, secondDriverId)
                            .put(_ORIGIN, origin)
                            .put(CREATED_BY, createdBy));
                    conn.updateWithParams(create.getQuery(), create.getParams(), reply->{
                        try {
                            if(reply.failed()) {
                                throw reply.cause();
                            }
                            Integer shipmentId = reply.result().getKeys().getInteger(0);

                            registerTravelLog(conn, scheduleRouteId, terminalId, origin, shipmentId).whenComplete((resultRTL, errorRTL)->{
                                try {
                                    if(errorRTL != null){
                                        throw errorRTL;
                                    }
                                    future.complete(new JsonObject().put(ID, shipmentId));
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
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerTravelLog(SQLConnection conn, Integer scheduleRouteId, Integer terminalOriginId, String origin, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_TERMINAL_DESTINY, new JsonArray().add(scheduleRouteId).add(terminalOriginId), reply->{
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()) {
                    throw new Exception("Schedule route doesnt have terminal destiny");
                }
                Integer terminalDestinyId = reply.result().getRows().get(0).getInteger(_TERMINAL_DESTINY_ID);
                // Generar el travel_log_code
                getTravelLogCode(conn, scheduleRouteId, terminalOriginId).whenComplete((travelLogCode,errorT)->{
                    try {
                        if(errorT != null){
                            throw errorT;
                        }
                        // Guardar el travel_log
                        JsonObject bodyTravelLogs = new JsonObject()
                                .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                .put(LOAD_ID, shipmentId)
                                .put(_ORIGIN, origin)
                                .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                                .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                                .put("travel_log_code", travelLogCode);
                        GenericQuery create = this.generateGenericCreate("travel_logs", bodyTravelLogs);
                        conn.updateWithParams(create.getQuery(), create.getParams(),replyGq->{
                            try {
                                if(replyGq.failed()) {
                                    throw replyGq.cause();
                                }
                                Integer travelLogId = replyGq.result().getKeys().getInteger(0);
                                bodyTravelLogs.put(ID, travelLogId);
                                future.complete(bodyTravelLogs);
                            } catch(Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
                    } catch(Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<String> getTravelLogCode(SQLConnection conn, Integer scheduleRouteId, Integer terminalOriginId){
        CompletableFuture<String> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_TRAVEL_LOG_CODE, new JsonArray().add(scheduleRouteId).add(terminalOriginId), reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0) {
                    throw new Exception("Schedule route doesnt have terminal destiny");
                }
                JsonObject route = reply.result().getRows().get(0);
                // Generar el travel_log_code
                conn.queryWithParams(QUERY_GET_SCHEDULE_ROW, new JsonArray().add(route.getInteger("config_route_id")), replyCode->{
                    try {
                        if(replyCode.failed()) {
                            throw new Exception(replyCode.cause());
                        }
                        if(replyCode.result().getNumRows() == 0) {
                            throw new Exception("Element not found from schedule elements");
                        }
                        List<JsonObject> schedules = replyCode.result().getRows();
                        Integer rowSchedule = 0;
                        String routeId = route.getInteger("config_route_id").toString();
                        if(routeId.length() < 3){
                            int iRouteId = route.getInteger("config_route_id");
                            routeId = String.format("%03d", iRouteId);
                        }

                        for (JsonObject schedule : schedules) {
                            if (Objects.equals(schedule.getInteger(ID), route.getInteger("config_schedule_id"))) {
                                rowSchedule = schedule.getInteger("row");
                            }
                        }
                        String travel = UtilsDate.format_DD_MM_YYYY(UtilsDate.parse_yyyy_MM_dd(route.getString("travel")));
                        travel = travel.replace("/", "");

                        String code = routeId + "-" + travel + "-" + rowSchedule + "-" + route.getInteger("segment").toString();
                        future.complete(code);
                    }catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private final static String QUERY_STATUS_TRACKING_TRAVEL = "SELECT schedule_status FROM schedule_route where id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_LOADING = "UPDATE schedule_route_destination set destination_status = 'loading' where id = ? and (destination_status='scheduled' or destination_status='ready-to-load')";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?,updated_at = ?, started_at = ? WHERE id = ?  ";

    private final static String GET_ROUTE_DESTINATIONS_BY_TERMINAL_TYPE = "SELECT * FROM schedule_route_destination " +
            "where schedule_route_id = ? \n" +
            "and destination_status != 'canceled' \n" +
            "and destination_status != 'finished-ok' ";

    private final static String GET_DESTINATION_ORIGIN_STATUS = "SELECT destination_status FROM schedule_route_destination where schedule_route_id = ? AND terminal_origin_id = ? AND destination_status!='canceled' and finished_at IS NULL LIMIT 1 ;";

    private final static String QUERY_GET_TERMINAL_DESTINY = "SELECT srd.terminal_destiny_id, cd.order_destiny, sr.config_route_id, sr.config_schedule_id " +
            " FROM schedule_route_destination AS srd " +
            " JOIN config_destination AS cd ON cd.id=srd.config_destination_id " +
            " JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id " +
            " where srd.schedule_route_id = ? AND srd.terminal_origin_id = ? AND srd.destination_status!='canceled' order by cd.order_destiny asc limit 1;";

    private final static String QUERY_GET_TRAVEL_LOG_CODE = "SELECT sr.config_route_id, DATE(sr.travel_date) AS travel, sr.config_schedule_id, (cd.order_destiny - 1 ) AS segment " +
            " FROM schedule_route_destination AS srd " +
            " JOIN config_destination AS cd ON cd.id=srd.config_destination_id " +
            " JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id " +
            " where srd.schedule_route_id = ? AND srd.terminal_origin_id = ? AND srd.destination_status!='canceled' order by cd.order_destiny asc limit 1;";

    private final static String QUERY_GET_SCHEDULE_ROW = "select @row := @row + 1 as 'row', cs.id, cs.config_route_id " +
            " from config_schedule AS cs, (SELECT @row := 0) r where cs.config_route_id = ? and cs.config_schedule_origin_id IS NULL;";

    private static final String QUERY_GET_SHIPMENT_BY_STATUS = "SELECT * FROM shipments WHERE schedule_route_id = ? AND (shipment_type = 'load' OR shipment_type = 'download') AND shipment_status = 1;";

}