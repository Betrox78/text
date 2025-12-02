package database.routes.handlers.TravelTrackingDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.routes.TravelTrackingDBV;
import database.routes.handlers.enums.SCHEDULE_STATUS;
import database.routes.handlers.enums.TRAVELTRACKING_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID;
import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import static service.commons.Constants.*;

public class InitDownload extends DBHandler<TravelTrackingDBV> {

    public InitDownload(TravelTrackingDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer terminalId = body.getInteger(TERMINAL_ID);
            Integer scheduleRouteId = body.getInteger(_SCHEDULE_ROUTE_ID);
            Integer createdBy = body.getInteger(CREATED_BY);
//                Integer trailerId = body.getInteger(_TRAILER_ID);
//                String leftStamp = body.getString(LEFT_STAMP);
//                String rightStamp = body.getString(RIGHT_STAMP);
//                String additionalStamp = body.getString(ADDITIONAL_STAMP);
//                String replacementStamp = body.getString(REPLACEMENT_STAMP);
//                String fifthStamp = body.getString(_FIFTH_STAMP);
//                String sixthStamp = body.getString(_SIXTH_STAMP);
//                Integer secondTrailerId = body.getInteger(_SECOND_TRAILER_ID);
//                String secondLeftStamp = body.getString(_SECOND_LEFT_STAMP);
//                String secondRightStamp = body.getString(_SECOND_RIGHT_STAMP);
//                String secondAdditionalStamp = body.getString(_SECOND_ADDITIONAL_STAMP);
//                String secondReplacementStamp = body.getString(_SECOND_REPLACEMENT_STAMP);
//                String secondFifthStamp = body.getString(_SECOND_FIFTH_STAMP);
//                String secondSixthStamp = body.getString(_SECOND_SIXTH_STAMP);

            getLoadInfo(scheduleRouteId, terminalId).whenComplete((load, errLoad) -> {
               try {
                   if(errLoad != null) {
                       throw errLoad;
                   }
                   validateDestinationStatus(scheduleRouteId, terminalId).whenComplete((resVDS, errVDS) -> {
                       try {
                           if (errVDS != null) {
                               throw errVDS;
                           }
                           startTransaction(message, conn -> updateRouteAndDestinationsStatus(conn, scheduleRouteId, terminalId, createdBy).whenComplete((result, errorCS)->{
                               try {
                                   if(errorCS != null){
                                       throw errorCS;
                                   }
                                   registerShipment(conn, scheduleRouteId, body).whenComplete((downloadId, error)-> {
                                       try {
                                           if (error != null) {
                                               throw error;
                                           }
                                           Integer travelLogsId = load.getInteger(_TRAVEL_LOGS_ID);
                                           updateTravelLog(conn, travelLogsId, downloadId).whenComplete((resUTL, errUTL) -> {
                                               try {
                                                   if (errUTL != null) {
                                                       throw errUTL;
                                                   }
                                                   this.commit(conn, message, new JsonObject().put(ID, downloadId));
                                               } catch (Throwable t) {
                                                   this.rollback(conn, t, message);
                                               }
                                           });
                                       } catch(Throwable t){
                                           this.rollback(conn, t, message);
                                       }
                                   });
                               } catch (Throwable t) {
                                   this.rollback(conn, t, message);
                               }
                           }));
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

    private CompletableFuture<JsonObject> getLoadInfo(Integer scheduleRouteId, Integer terminalId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(terminalId).add(scheduleRouteId);
        dbClient.queryWithParams(GET_SHIPMENT_LOAD_FOR_ROUTE_BY_TERMINAL, params, replyS -> {
            try {
                if (replyS.failed()) {
                    throw replyS.cause();
                }
                List<JsonObject> result = replyS.result().getRows();
                if(result.isEmpty()) {
                    throw new Exception("The previous shipment record is not a shipment load");
                }

                JsonObject shipment = result.get(0);
                future.complete(shipment);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Integer> registerShipment(SQLConnection conn, Integer scheduleRouteId, JsonObject body){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            checkLastShipment(conn, scheduleRouteId).whenComplete((res, err)->{
                try {
                    if(err != null){
                        throw new Exception(err);
                    }

                    GenericQuery createShipment = this.generateGenericCreate("shipments", body);
                    conn.updateWithParams(createShipment.getQuery(), createShipment.getParams(), reply -> {
                        try {
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            Integer downloadId = reply.result().getKeys().getInteger(0);

                            future.complete(downloadId);
                        }catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateTravelLog(SQLConnection conn, Integer travelLogsId, Integer downloadId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        GenericQuery updateTravelLog = this.generateGenericUpdate("travel_logs", new JsonObject()
                .put(ID, travelLogsId)
                .put("download_id", downloadId)
                .put(STATUS, "open"));
        conn.updateWithParams(updateTravelLog.getQuery(), updateTravelLog.getParams(), replyUpdateTravelLog ->{
            try {
                if(replyUpdateTravelLog.failed()) {
                    throw new Exception(replyUpdateTravelLog.cause());
                }
                future.complete(true);
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    private CompletableFuture<Boolean> validateDestinationStatus(Integer scheduleRouteId, Integer terminalId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(scheduleRouteId)
                .add(terminalId);
        dbClient.queryWithParams(GET_DESTINATION_DESTINY_STATUS, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()) {
                    throw new Exception("NOT DESTINATIONS FOUND");
                }

                String destinationStatus = result.get(0).getString(_DESTINATION_STATUS);
                if(!destinationStatus.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                    throw new Exception("CAN NOT INIT THE DOWNLOADING");
                }

                future.complete(true);
            } catch(Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> checkLastShipment(SQLConnection conn, Integer scheduleRouteId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_SHIPMENT_BY_STATUS, new JsonArray().add(scheduleRouteId), reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if(!result.isEmpty()){
                    throw new Exception("SHIPMENT LOADING OR DOWNLOADING IS OPEN FOR THIS ROUTE");
                }

                future.complete(true);
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void isValidStatus(String actStatus) throws Exception {
        boolean isStopped = Objects.nonNull(actStatus) && actStatus.equals(TRAVELTRACKING_STATUS.STOPPED.getValue());
        if(!isStopped){
            throw new Exception("CANNOT BE CHANGED TO DOWNLOADING FROM " + actStatus);
        }
    }

    private CompletableFuture<Boolean> updateDestinationsStatus(SQLConnection conn, Integer scheduleRouteId, Integer createdBy){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(createdBy)
                .add(UtilsDate.sdfDataBase(new Date()))
                .add(scheduleRouteId);
        conn.updateWithParams(UPDATE_SCHEDULE_ROUTE_DESTINATIONS_DOWNLOADING, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                future.complete(true);

            } catch(Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> insertTravelTracking(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        this.getDestinations(conn, scheduleRouteId, terminalId).whenComplete((destinations, desErr)-> {
            try {
                if (desErr != null) {
                    throw new Exception(desErr);
                }

                List<GenericQuery> travelTrackingBatch = new ArrayList<>();
                for (JsonObject destination : destinations) {
                    Integer destinationId = destination.getInteger(ID);
                    travelTrackingBatch.add(this.generateGenericCreate("travel_tracking",
                            new JsonObject()
                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                    .put(SCHEDULE_ROUTE_DESTINATION_ID, destinationId)
                                    .put(CREATED_BY, createdBy)
                                    .put(Constants.STATUS, "downloading")));
                }
                List<JsonArray> params = travelTrackingBatch.stream().map(GenericQuery::getParams).collect(Collectors.toList());
                conn.batchWithParams(travelTrackingBatch.get(0).getQuery(), params, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }

                        future.complete(true);

                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateRouteStatus(SQLConnection conn, Integer scheduleRouteId, Integer updatedBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery update = this.generateGenericUpdate("schedule_route", new JsonObject()
                    .put(ID, scheduleRouteId)
                    .put(_SCHEDULE_STATUS, SCHEDULE_STATUS.DOWNLOADING.getValue())
                    .put(UPDATED_BY, updatedBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(UtilsDate.getLocalDate())));

            conn.updateWithParams(update.getQuery(), update.getParams(), reply->{
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateRouteAndDestinationsStatus(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        this.getStatusTravel(conn, scheduleRouteId).whenComplete((res, err) -> {
            try {
                if (err != null) {
                    throw err;
                }
                String scheduleStatus = res.getString(_SCHEDULE_STATUS);
                this.isValidStatus(scheduleStatus);

                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                tasks.add(this.updateDestinationsStatus(conn, scheduleRouteId, terminalId));
                tasks.add(this.updateRouteStatus(conn, scheduleRouteId, createdBy));
                tasks.add(this.insertTravelTracking(conn, scheduleRouteId, terminalId, createdBy));
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((__, errTasks) -> {
                    try {
                        if (errTasks != null) {
                            throw new Exception(errTasks);
                        }

                        future.complete(true);
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

    private CompletableFuture<List<JsonObject>> getDestinations(SQLConnection conn, Integer scheduleRouteId, Integer terminalId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray()
                    .add(scheduleRouteId)
                    .add(terminalId);

            conn.queryWithParams(GET_ROUTE_DESTINATIONS_BY_TERMINAL_TYPE, params, reply->{
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> result = reply.result().getRows();

                    if (result.isEmpty()){
                        throw new Exception("DESTINATIONS NOT FOUND");
                    }

                    future.complete(result);
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
            return future;
        }catch (Exception e){
            future.completeExceptionally(e);
            return future;
        }

    }

    private CompletableFuture<JsonObject> getStatusTravel(SQLConnection conn, int scheduleRouteId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_STATUS_TRACKING_TRAVEL, new JsonArray().add(scheduleRouteId), reply->{
            try{
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Throwable("STATUS NOT FOUND FOR THAT ROUTE AND DESTINATION");
                }
                future.complete(result.get(0));
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String GET_SHIPMENT_LOAD_FOR_ROUTE_BY_TERMINAL = "SELECT s.*, tl.id AS travel_logs_id FROM shipments AS s \n" +
            "INNER JOIN schedule_route AS sr ON sr.id = s.schedule_route_id \n" +
            "INNER JOIN travel_logs AS tl ON tl.load_id = s.id \n" +
            "INNER JOIN (select cd.* from config_destination AS cd " +
            "   INNER JOIN config_route AS cr ON cd.config_route_id = cr.id AND (cd.order_origin+1 = cd.order_destiny) ) AS config ON " +
            "   (config.terminal_destiny_id=? AND config.config_route_id=sr.config_route_id)\n" +
            "WHERE s.schedule_route_id = ? AND s.shipment_status <> 0 order by s.id desc LIMIT 1;";

    private static final String QUERY_GET_SHIPMENT_BY_STATUS = "SELECT * FROM shipments WHERE schedule_route_id = ? AND (shipment_type = 'load' OR shipment_type = 'download') AND shipment_status = 1;";

    private final static String GET_DESTINATION_DESTINY_STATUS = "SELECT destination_status FROM schedule_route_destination where schedule_route_id = ? AND terminal_destiny_id = ? AND destination_status NOT IN ('canceled', 'finished-ok') LIMIT 1 ;";

    private final static String QUERY_STATUS_TRACKING_TRAVEL = "SELECT schedule_status FROM schedule_route where id = ? ";

    private final static String GET_ROUTE_DESTINATIONS_BY_TERMINAL_TYPE = "SELECT * FROM schedule_route_destination " +
            "where schedule_route_id = ? \n" +
            "and destination_status != 'canceled' \n" +
            "and destination_status != 'finished-ok' \n" +
            "AND terminal_destiny_id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_DOWNLOADING = "UPDATE schedule_route_destination set destination_status = 'downloading', updated_by = ?, updated_at = ? " +
            "WHERE schedule_route_id = ? and destination_status='stopped'";

}
