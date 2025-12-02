package database.routes.handlers.TravelTrackingDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.routes.TravelTrackingDBV;
import database.routes.handlers.enums.TRAVELTRACKING_ACTION;
import database.routes.handlers.enums.TRAVELTRACKING_STATUS;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID;
import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import static database.parcel.ParcelDBV.PARCEL_ID;
import static database.routes.TravelTrackingDBV.*;
import static service.commons.Constants.*;

public class CloseLoad extends DBHandler<TravelTrackingDBV> {

    RegisterNoTraveled registerNoTraveled;

    public CloseLoad(TravelTrackingDBV dbVerticle) {
        super(dbVerticle);
        registerNoTraveled = new RegisterNoTraveled(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message){
        startTransaction(message,conn->{
            try {
                JsonObject body = message.body();

                Integer shipmentId = body.getInteger("shipment_id");
                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer createdBy = body.getInteger(CREATED_BY);
                Integer trailerId = body.getInteger(_TRAILER_ID);
                String leftStamp = body.getString(LEFT_STAMP);
                String rightStamp = body.getString(RIGHT_STAMP);
                String additionalStamp = body.getString(ADDITIONAL_STAMP);
                String replacementStamp = body.getString(REPLACEMENT_STAMP);
                String fifthStamp = body.getString(_FIFTH_STAMP);
                String sixthStamp = body.getString(_SIXTH_STAMP);
                Integer secondTrailerId = body.getInteger(_SECOND_TRAILER_ID);
                String secondLeftStamp = body.getString(_SECOND_LEFT_STAMP);
                String secondRightStamp = body.getString(_SECOND_RIGHT_STAMP);
                String secondAdditionalStamp = body.getString(_SECOND_ADDITIONAL_STAMP);
                String secondReplacementStamp = body.getString(_SECOND_REPLACEMENT_STAMP);
                String secondFifthStamp = body.getString(_SECOND_FIFTH_STAMP);
                String secondSixthStamp = body.getString(_SECOND_SIXTH_STAMP);
                closeShipment(conn,shipmentId, terminalId,
                        trailerId, leftStamp, rightStamp, additionalStamp, replacementStamp, fifthStamp, sixthStamp,
                        secondTrailerId, secondLeftStamp, secondRightStamp, secondAdditionalStamp, secondReplacementStamp, secondFifthStamp, secondSixthStamp,
                        createdBy).whenComplete((res, err)->{
                    try {
                        if(err != null) {
                            throw err;
                        }
                        this.commit(conn, message, res);
                    }catch (Throwable t) {
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> closeShipment(SQLConnection conn, Integer shipmentId, Integer branchofficeId,
                                                        Integer trailerId, String leftStamp, String rightStamp, String additionalStamp, String replacementStamp, String fifthStamp, String sixthStamp,
                                                        Integer secondTrailerId, String secondLeftStamp, String secondRightStamp, String secondAdditionalStamp, String secondReplacementStamp, String secondFifthStamp, String secondSixthStamp,
                                                        Integer createdBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        getShipment(conn, shipmentId, branchofficeId).whenComplete((shipment, err)->{
            try {
                if(err != null){
                    throw err;
                }
                Integer terminalId = shipment.getInteger(_TERMINAL_ID);
                Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);

                SHIPMENT_TRACKING_STATUS shipmentTrackingStatus = ShipmentsDBV.SHIPMENT_TYPES.LOAD.getTrackingStatusByShipmentType();

                this.getTotalsByShipment(conn, shipmentTrackingStatus.getName(), shipmentId).whenComplete((totalsShipment, errorTS) -> {
                    try {
                        if (errorTS != null){
                            throw errorTS;
                        }

                        JsonObject update = new JsonObject()
                                .put(ID, shipmentId)
                                .put(TOTAL_TICKETS, totalsShipment.getInteger(TOTAL_TICKETS))
                                .put(TOTAL_COMPLEMENTS, totalsShipment.getInteger(TOTAL_COMPLEMENTS))
                                .put(TOTAL_PARCELS, totalsShipment.getInteger(TOTAL_PARCELS))
                                .put(TOTAL_PACKAGES, totalsShipment.getInteger(TOTAL_PACKAGES))
                                .put(_SHIPMENT_STATUS, SHIPMENT_STATUS.CLOSE.ordinal())
                                .put(_TRAILER_ID, trailerId)
                                .put(LEFT_STAMP, leftStamp)
                                .put(RIGHT_STAMP, rightStamp)
                                .put(ADDITIONAL_STAMP, additionalStamp)
                                .put(REPLACEMENT_STAMP, replacementStamp)
                                .put(_FIFTH_STAMP, fifthStamp)
                                .put(_SIXTH_STAMP, sixthStamp)
                                .put(_SECOND_TRAILER_ID, secondTrailerId)
                                .put(_SECOND_LEFT_STAMP, secondLeftStamp)
                                .put(_SECOND_RIGHT_STAMP, secondRightStamp)
                                .put(_SECOND_ADDITIONAL_STAMP, secondAdditionalStamp)
                                .put(_SECOND_REPLACEMENT_STAMP, secondReplacementStamp)
                                .put(_SECOND_FIFTH_STAMP, secondFifthStamp)
                                .put(_SECOND_SIXTH_STAMP, secondSixthStamp)
                                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                                .put(UPDATED_BY, createdBy);

                        GenericQuery gq = this.generateGenericUpdate("shipments",update);
                        conn.updateWithParams(gq.getQuery(),gq.getParams(),reply->{
                            try {
                                if(reply.failed()) {
                                    throw new Exception(reply.cause());
                                }

                                this.changeStatus(conn, scheduleRouteId, terminalId, createdBy).whenComplete((res, error)->{
                                    try{
                                        if(error!=null){
                                            throw new Exception(error);
                                        }
                                        future.complete(res);
                                    }
                                    catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                            }catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });

                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });

            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getShipment(SQLConnection conn, Integer shipmentId, Integer branchofficeId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_SHIPMENT, new JsonArray().add(shipmentId), reply -> {
            try{
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()) {
                    throw new Exception("SHIPMENT NOT FOUND");
                }

                JsonObject shipment = result.get(0);

                SHIPMENT_STATUS shipmentStatus = SHIPMENT_STATUS.fromValue(shipment.getInteger("shipment_status"));
                ShipmentsDBV.SHIPMENT_TYPES currentShipmentType = ShipmentsDBV.SHIPMENT_TYPES.valueOf(shipment.getString("shipment_type").toUpperCase());
                Integer terminalId = shipment.getInteger("terminal_id");

                if (shipmentStatus.equals(SHIPMENT_STATUS.CLOSE)) {
                    throw new Exception("THE SHIPMENT IS ALREADY CLOSE");
                }
                if (shipmentStatus.equals(SHIPMENT_STATUS.CANCELED)) {
                    throw new Exception("THE SHIPMENT IS CANCELED");
                }
                if(!currentShipmentType.equals(ShipmentsDBV.SHIPMENT_TYPES.LOAD)){
                    throw new Exception("THE SHIPMENT TYPE NOT MATCH");
                }
                if(!terminalId.equals(branchofficeId)){
                    throw new Exception("THE BRANCH OFFICE OF THE EMPLOYEE NOT MATCH WITH THE TERMINAL OF SHIPMENT");
                }

                future.complete(shipment);
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getTotalsByShipment(SQLConnection conn, String shipmentTrackingStatus, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(shipmentTrackingStatus)
                .add(shipmentTrackingStatus)
                .add(shipmentTrackingStatus)
                .add(shipmentTrackingStatus)
                .add(shipmentId);

        conn.queryWithParams(QUERY_GET_TOTALS_BY_SHIPMENT, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                future.complete(reply.result().getRows().get(0));

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> changeStatus(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.getStatusTravel(conn, new JsonArray().add(scheduleRouteId)).whenComplete((res, err) -> {
            try {
                if (err != null) {
                    throw new Exception(err);
                }

                TRAVELTRACKING_STATUS scheduleStatus = TRAVELTRACKING_STATUS.valueOf(res.getString("schedule_status").toUpperCase());
                this.isValidStatus(scheduleStatus);

                this.getDestinations(conn, scheduleRouteId, terminalId).whenComplete((destinations, desErr)->{
                    try{
                        if(desErr!=null){
                            throw new Exception(desErr);
                        }

                        List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                        for(JsonObject destination :  destinations){
                            Integer destinationId = destination.getInteger(ID);
                            tasks.add(this.updateScheduleRouteDestination(conn, scheduleRouteId, destinationId, userId));
                        }
                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((__, errTasks) -> {
                            try {
                                if (errTasks != null) {
                                    throw new Exception(errTasks);
                                }

                                this.getOriginTerminalId(conn , scheduleRouteId).whenComplete( (terminalOriginId , errorT) ->{
                                    try{
                                        if(errorT!= null){
                                            throw new Exception(errorT);
                                        }

                                        this.updateRouteStatus(conn, scheduleRouteId, terminalId, terminalOriginId, userId).whenComplete((resu,erro)->{
                                            try {
                                                if(erro!= null){
                                                    throw new Exception(erro);
                                                }

                                                this.inTransitParcelsActions(conn, scheduleRouteId, terminalId, userId).whenComplete((resIT, errIT) -> {
                                                   try {
                                                       if (errIT != null) {
                                                           throw errIT;
                                                       }
                                                       future.complete(new JsonObject().put("success", true));
                                                   } catch (Throwable t) {
                                                       future.completeExceptionally(t);
                                                   }
                                                });

                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                            } catch(Exception ex) {
                                future.completeExceptionally(ex);
                            }
                        });
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

    private CompletableFuture<JsonObject> getStatusTravel(SQLConnection conn, JsonArray params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_STATUS_TRACKING_TRAVEL,params, reply->{
            try{
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    new Exception("STATUS NOT FOUND FOR THAT ROUTE AND DESTINATION");
                }
                //result.put("success",true);
                future.complete(result.get(0));
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getDestinations(SQLConnection conn, Integer schedule_route_id, Integer terminal_id){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try{

            JsonArray params = new JsonArray()
                .add(schedule_route_id)
                .add(terminal_id)
                .add(terminal_id);

            conn.queryWithParams(GET_ROUTE_DESTINATIONS_ALL, params, reply->{
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

    private void isValidStatus(TRAVELTRACKING_STATUS actStatus) throws Exception {
        int valid = actStatus.equals(TRAVELTRACKING_STATUS.LOADING)
                ? RESPONSE_CHANGE_STATUS.OK.ordinal()
                : RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_READY_TO_GO.ordinal();

        if(!RESPONSE_LIST.getString(0).equals(RESPONSE_LIST.getString(valid))) {
            throw new Exception(RESPONSE_LIST.getString(valid) + "FROM ready-to-go");
        }
    }

    private CompletableFuture<Boolean> updateScheduleRouteDestination(SQLConnection conn, Integer scheduleRouteId, Integer destinationId, Integer userId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray().add(destinationId);
            conn.updateWithParams(UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_GO, params, reply->{
                try{
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    GenericQuery create = this.generateGenericCreate("travel_tracking", new JsonObject()
                            .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                            .put(SCHEDULE_ROUTE_DESTINATION_ID, destinationId)
                            .put(CREATED_BY, userId)
                            .put(Constants.STATUS, TRAVELTRACKING_STATUS.READY_TO_GO.getValue()));
                    conn.updateWithParams(create.getQuery(), create.getParams(), replyInsertTravelTracking -> {
                        try {
                            if (replyInsertTravelTracking.failed()){
                                throw replyInsertTravelTracking.cause();
                            }
                            future.complete(true);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                }catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Integer> getOriginTerminalId(SQLConnection conn, Integer scheduleRouteId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray()
                    .add(scheduleRouteId);
            conn.queryWithParams(GET_TERMINAL_ORIGIN,params,reply->{
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    Integer destiny = reply.result().getRows().get(0).getInteger("terminal_origin_id");
                    future.complete(destiny);

                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateRouteStatus(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer terminalOriginId, Integer userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            String query = terminalId.equals(terminalOriginId) ? UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT : UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT;
            JsonArray params = new JsonArray()
                    .add(TRAVELTRACKING_STATUS.READY_TO_GO.getValue())
                    .add(userId)
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(scheduleRouteId);

            conn.updateWithParams(query, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> inTransitParcelsActions(SQLConnection conn, Integer scheduleRouteId, Integer terminalOriginId, int userId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();

        this.getParcelsPackagesByShipment(conn, scheduleRouteId, terminalOriginId).whenComplete((parcelsPackages, errorParcels) -> {
            try {
                if (errorParcels != null){
                    throw errorParcels;
                }
                if (parcelsPackages.isEmpty()){
                    future.complete(true);
                }

                this.setParcelsInTransit(conn, parcelsPackages, userId).whenComplete((resPIT, errPIT) -> {
                    try {
                        if (errPIT != null) {
                            throw errPIT;
                        }
                        this.setPackagesInTransit(conn, parcelsPackages, userId).whenComplete((resPPIT, errPPIT) -> {
                            try {
                                if (errPPIT != null) {
                                    throw errPPIT;
                                }
                                this.insertParcelsPackagesTracking(conn, parcelsPackages, terminalOriginId, userId).whenComplete((resIPPT, errIPPT) -> {
                                    try {
                                        if (errIPPT != null) {
                                            throw errIPPT;
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
                });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> getParcelsPackagesByShipment(SQLConnection conn, Integer scheduleRouteId, Integer terminalOriginId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PARCELS_PACKAGES_BY_SHIPMENT, new JsonArray().add(scheduleRouteId).add(terminalOriginId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                future.complete(reply.result().getRows());

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> setParcelsInTransit(SQLConnection conn, List<JsonObject> parcelsPackages, int userId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        String parcelIdParams = parcelsPackages.stream()
                .map(p -> "\'" + p.getInteger("parcel_id") + "\'").distinct()
                .collect(Collectors.joining(", "));
        conn.update(String.format(QUERY_UPDATE_PARCELS_IN_TRANSIT, userId, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), parcelIdParams), replyUP -> {
            try {
                if(replyUP.failed()) {
                    throw replyUP.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> setPackagesInTransit(SQLConnection conn, List<JsonObject> parcelsPackages, int userId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        String packageIdParams = parcelsPackages.stream()
                .map(p -> "\'" + p.getInteger("parcel_package_id") + "\'").distinct()
                .collect(Collectors.joining(", "));
        conn.update(String.format(QUERY_SET_PARCELS_PACKAGES_INTRANSIT, userId, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), packageIdParams), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> insertParcelsPackagesTracking(SQLConnection conn, List<JsonObject> parcelsPackages, Integer terminalOriginId, Integer userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            ArrayList<GenericQuery> tasks = new ArrayList<>();
            for (JsonObject pack : parcelsPackages) {
                Integer parcelId = pack.getInteger(PARCEL_ID);
                Integer parcelPackageId = pack.getInteger("parcel_package_id");
                GenericQuery create = generateGenericCreate("parcels_packages_tracking", new JsonObject()
                    .put("parcel_id",parcelId)
                    .put("parcel_package_id", parcelPackageId)
                    .put("terminal_id",terminalOriginId)
                    .put("action", TRAVELTRACKING_ACTION.IN_TRANSIT.getValue())
                    .put(CREATED_BY, userId));
                tasks.add(create);
            }

            List<JsonArray> createParams = tasks.stream().map(GenericQuery::getParams).collect(Collectors.toList());
            conn.batchWithParams(tasks.get(0).getQuery(), createParams, res ->{
                try {
                    if(res.failed()){
                        throw res.cause();
                    }

                    future.complete(true);
                }catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_GET_SHIPMENT = "SELECT * FROM shipments where id = ? ;";

    private final static String QUERY_STATUS_TRACKING_TRAVEL = "SELECT schedule_status FROM schedule_route where id = ? ";

    private final static String GET_ROUTE_DESTINATIONS_ALL = "SELECT * FROM schedule_route_destination where schedule_route_id = ? and (terminal_origin_id = ? or terminal_destiny_id = ?) and destination_status != 'canceled' and destination_status != 'finished-ok'";

    private static final String GET_TERMINAL_ORIGIN = "SELECT cr.terminal_origin_id FROM schedule_route AS sr JOIN config_route AS cr ON cr.id=sr.config_route_id where sr.id = ?";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?, updated_at = ?, finished_at = ? WHERE id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?,updated_at = ?, started_at = ? WHERE id = ?  ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_GO = "UPDATE schedule_route_destination set destination_status='ready-to-go' where id = ? and destination_status='loading'";

    private final static String QUERY_GET_PARCELS_PACKAGES_BY_SHIPMENT = "SELECT DISTINCT \n" +
            "  shipppt.parcel_id, \n" +
            "  shipppt.parcel_package_id \n" +
            "FROM shipments_parcel_package_tracking shipppt \n" +
            "INNER JOIN shipments AS s ON s.id = shipppt.shipment_id \n" +
            "LEFT JOIN shipments ship ON ship.id = shipppt.shipment_id \n" +
            "INNER JOIN travel_logs tl ON tl.load_id = ship.id \n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id \n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id AND srd.terminal_origin_id = ship.terminal_id \n" +
            "INNER JOIN parcels p ON p.id = shipppt.parcel_id \n" +
            "WHERE p.parcel_status NOT IN (2, 4, 6) \n" +
            "AND sr.id = ? \n" +
            "AND srd.terminal_origin_id = ? \n" +
            "GROUP BY shipppt.parcel_id, shipppt.parcel_package_id;";

    private static final String QUERY_UPDATE_PARCELS_IN_TRANSIT = "UPDATE parcels SET parcel_status = "+ PARCEL_STATUS.IN_TRANSIT.ordinal() +", updated_by = %d, updated_at = '%s'" +
            "WHERE id IN (%s);";

    private static final String QUERY_SET_PARCELS_PACKAGES_INTRANSIT = "UPDATE parcels_packages SET package_status = "+ PACKAGE_STATUS.IN_TRANSIT.ordinal() +", updated_by = %d, updated_at = '%s'" +
            "WHERE id IN (%s);";

    private static final String QUERY_GET_TOTALS_BY_SHIPMENT = "SELECT\n" +
            " COUNT(DISTINCT IF(shiptt.status = ?, shiptt.boarding_pass_ticket_id, NULL)) AS total_tickets,\n" +
            " COUNT(DISTINCT IF(shipct.status = ?, shipct.boarding_pass_complement_id, NULL)) AS total_complements,\n" +
            " COUNT(DISTINCT IF(shipppt.status = ?, shipppt.parcel_id, NULL)) AS total_parcels,\n" +
            " COUNT(DISTINCT IF(shipppt.status = ?, shipppt.parcel_package_id, NULL)) AS total_packages\n" +
            "FROM shipments ship\n" +
            "LEFT JOIN shipments_ticket_tracking shiptt ON shiptt.shipment_id = ship.id\n" +
            "LEFT JOIN shipments_parcel_package_tracking shipppt ON shipppt.shipment_id = ship.id\n" +
            "LEFT JOIN shipments_complement_tracking shipct ON shipct.shipment_id = ship.id\n" +
            "WHERE ship.id = ?;";

}
