package database.routes.handlers.TravelTrackingDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.routes.TravelTrackingDBV;
import database.routes.handlers.enums.SCHEDULE_STATUS;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.*;
import static database.branchoffices.BranchofficeDBV.RECEIVE_TRANSHIPMENTS;
import static database.routes.TravelTrackingDBV.*;
import static database.shipments.ShipmentsDBV.CHECK_CODES;
import static service.commons.Constants.*;

public class CloseDownload extends DBHandler<TravelTrackingDBV> {

    RegisterNoTraveled registerNoTraveled;

    public CloseDownload(TravelTrackingDBV dbVerticle) {
        super(dbVerticle);
        registerNoTraveled = new RegisterNoTraveled(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message){
        this.startTransaction(message,conn->{
            try {
                JsonObject body = message.body();
                Integer shipmentId = body.getInteger("shipment_id");
                Integer terminalId = body.getInteger(TERMINAL_ID);
                int userId = body.getInteger(CREATED_BY);
                closeShipment(conn,shipmentId, terminalId, userId).whenComplete((scheduleRouteId, errCloseShipment)->{
                    try {
                        if (errCloseShipment != null) {
                            throw errCloseShipment;
                        }
                        this.changeStatus(conn, scheduleRouteId, terminalId, userId).whenComplete((resultChangeStatus, error)->{
                            try{
                                if(error!=null){
                                    throw new Exception(error);
                                }
                                this.commit(conn, message, resultChangeStatus);
                            } catch (Exception e){
                                this.rollback(conn, e, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn, e, message);
            }
        });
    }

    private CompletableFuture<Integer> closeShipment(SQLConnection conn, Integer shipmentId, Integer branchofficeId, int userId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        getShipment(conn,shipmentId).whenComplete((shipment,err)->{
            try {
                if(err!=null){
                    throw new Exception(err);
                }
                SHIPMENT_STATUS shipmentStatus = SHIPMENT_STATUS.fromValue(shipment.getInteger("shipment_status"));
                ShipmentsDBV.SHIPMENT_TYPES currentShipmentType = ShipmentsDBV.SHIPMENT_TYPES.valueOf(shipment.getString("shipment_type").toUpperCase());
                Integer terminalId = shipment.getInteger("terminal_id");
                Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);

                if (shipmentStatus.equals(SHIPMENT_STATUS.CLOSE)) {
                    throw new Exception("THE SHIPMENT IS ALREADY CLOSE");
                }
                if (shipmentStatus.equals(SHIPMENT_STATUS.CANCELED)) {
                    throw new Exception("THE SHIPMENT IS CANCELED");
                }
                if(!currentShipmentType.equals(ShipmentsDBV.SHIPMENT_TYPES.DOWNLOAD)){
                    throw new Exception("THE SHIPMENT TYPE NOT MATCH");
                }
                if(!terminalId.equals(branchofficeId)){
                    throw new Exception("THE BRANCH OFFICE OF THE EMPLOYEE NOT MATCH WITH THE TERMINAL OF SHIPMENT");
                }

                TravelTrackingDBV.SHIPMENT_TRACKING_STATUS shipmentTrackingStatus = ShipmentsDBV.SHIPMENT_TYPES.DOWNLOAD.getTrackingStatusByShipmentType();

                this.getTotalsByShipment(conn, shipmentTrackingStatus.getName(), shipmentId).whenComplete((totalsShipment, errorTS) -> {
                    try {
                        if (errorTS != null){
                            throw errorTS;
                        }

                        Integer totalShipmentsTickets = totalsShipment.getInteger(TOTAL_TICKETS);
                        Integer totalShipmentsComplements = totalsShipment.getInteger(TOTAL_COMPLEMENTS);
                        Integer totalShipmentsParcels = totalsShipment.getInteger(TOTAL_PARCELS);
                        Integer totalShipmentsPackages = totalsShipment.getInteger(TOTAL_PACKAGES);
                        JsonObject update = new JsonObject()
                                .put(ID, shipmentId)
                                .put(TOTAL_TICKETS, totalShipmentsTickets)
                                .put(TOTAL_COMPLEMENTS, totalShipmentsComplements)
                                .put(TOTAL_PARCELS, totalShipmentsParcels)
                                .put(TOTAL_PACKAGES, totalShipmentsPackages)
                                .put("shipment_status", SHIPMENT_STATUS.CLOSE.ordinal())
                                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                                .put(UPDATED_BY, userId);


                        GenericQuery gq = this.generateGenericUpdate("shipments",update);
                        conn.updateWithParams(gq.getQuery(),gq.getParams(),reply->{
                            try {
                                if(reply.failed()) {
                                    throw new Exception(reply.cause());
                                }

                                future.complete(scheduleRouteId);

                            }catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });

                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });

            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getShipment(SQLConnection conn, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(shipmentId);
        conn.queryWithParams(QUERY_GET_SHIPMENT,params, reply->{
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0) {
                    throw new Exception("SHIPMENT NOT FOUND");
                }
                future.complete(reply.result().getRows().get(0));
            }catch (Exception e){
                future.completeExceptionally(e);
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

        String QUERY_GET_TOTALS_BY_SHIPMENT = "SELECT\n" +
                " COUNT(DISTINCT IF(shiptt.status = ?, shiptt.boarding_pass_ticket_id, NULL)) AS total_tickets,\n" +
                " COUNT(DISTINCT IF(shipct.status = ?, shipct.boarding_pass_complement_id, NULL)) AS total_complements,\n" +
                " COUNT(DISTINCT IF(shipppt.status = ?, shipppt.parcel_id, NULL)) AS total_parcels,\n" +
                " COUNT(DISTINCT IF(shipppt.status = ?, shipppt.parcel_package_id, NULL)) AS total_packages\n" +
                "FROM shipments ship\n" +
                "LEFT JOIN shipments_ticket_tracking shiptt ON shiptt.shipment_id = ship.id\n" +
                "LEFT JOIN shipments_parcel_package_tracking shipppt ON shipppt.shipment_id = ship.id\n" +
                "LEFT JOIN shipments_complement_tracking shipct ON shipct.shipment_id = ship.id\n" +
                "WHERE ship.id = ?;";
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

    private CompletableFuture<JsonObject> changeStatus(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, int userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.getStatusTravel(conn, scheduleRouteId).whenComplete((route, err) -> {
            try {
                if (err != null) {
                    throw err;
                }

                SCHEDULE_STATUS scheduleStatusActual = SCHEDULE_STATUS.fromValue(route.getString(_SCHEDULE_STATUS));
                this.isValidStatus(scheduleStatusActual);

                this.getDestinations(conn, scheduleRouteId, terminalId).whenComplete((destinations, desErr)->{
                    try {
                        if(desErr != null){
                            throw desErr;
                        }
                        List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                        for(JsonObject destination : destinations){
                            tasks.add(this.updateScheduleRouteDestination(conn, scheduleRouteId, destination, terminalId, userId));
                        }
                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((__, errTasks) -> {
                            try {
                                if (errTasks != null) {
                                    throw errTasks;
                                }
                                int finishedOKSize = (int) destinations.stream().filter(d -> d.getString(STATUS).equalsIgnoreCase(SCHEDULE_STATUS.FINISHED_OK.getValue())).count();
                                SCHEDULE_STATUS scheduleStatus = finishedOKSize == destinations.size() ? SCHEDULE_STATUS.FINISHED_OK : SCHEDULE_STATUS.READY_TO_LOAD;

                                this.getTerminalOriginId(conn, scheduleRouteId).whenComplete( (terminalOriginId , errorT) ->{
                                    try{
                                        if(errorT != null){
                                            throw errorT;
                                        }
                                        this.updateRouteStatus(conn, scheduleRouteId, scheduleStatus, terminalId, terminalOriginId, userId).whenComplete((resu,erro)->{
                                            try {
                                                if(erro != null){
                                                    throw new Exception(erro);
                                                }

                                                Integer routeTerminalDestinyId = route.getInteger(_TERMINAL_DESTINY_ID);
                                                String hitchedTrailers = route.getString(HITCHED_TRAILERS);
                                                JsonArray hitchedTrailersArray = new JsonArray();
                                                if (Objects.nonNull(hitchedTrailers)) {
                                                    for (String ht : hitchedTrailers.split(",")) {
                                                        hitchedTrailersArray.add(Integer.parseInt(ht));
                                                    }
                                                }

                                                future.complete(new JsonObject()
                                                        .put(APPLY_RELEASE, routeTerminalDestinyId.equals(terminalId) && !hitchedTrailersArray.isEmpty())
                                                        .put(HITCHED_TRAILERS, hitchedTrailersArray));
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
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
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

    private CompletableFuture<List<JsonObject>> getDestinations(SQLConnection conn, Integer scheduleRouteId, Integer terminalId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try{
            conn.queryWithParams(GET_ROUTE_DESTINATIONS, new JsonArray().add(scheduleRouteId).add(terminalId).add(terminalId), reply->{
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

    private void isValidStatus(SCHEDULE_STATUS actStatus) throws Exception {
        boolean isDownloading = actStatus.equals(SCHEDULE_STATUS.DOWNLOADING);
        int valid = isDownloading ? RESPONSE_CHANGE_STATUS.OK.ordinal() : RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_READY_TO_LOAD.ordinal();
        if(!RESPONSE_LIST.getString(0).equals(RESPONSE_LIST.getString(valid))) {
            throw new Exception(RESPONSE_LIST.getString(valid) + "FROM ready-to-load");
        }
    }

    private CompletableFuture<Boolean> updateScheduleRouteDestination(SQLConnection conn, Integer scheduleRoute, JsonObject destination, Integer terminal, int userId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        try{
            int destinationId = destination.getInteger(ID);
            Integer destinationTerminalDestinyId = destination.getInteger("terminal_destiny_id");
            SCHEDULE_STATUS scheduleStatus;
            String QUERY;

            JsonArray params = new JsonArray();
            if(destinationTerminalDestinyId.equals(terminal)){
                QUERY = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_FINISHED_OK;
                params.add(UtilsDate.sdfDataBase(UtilsDate.getDateConvertedTimeZone(UtilsDate.timezone, new Date())));
                scheduleStatus = SCHEDULE_STATUS.FINISHED_OK;
            } else {
                QUERY = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_LOAD;
                scheduleStatus = SCHEDULE_STATUS.READY_TO_LOAD;
            }
            destination.put(STATUS, scheduleStatus.getValue());
            params.add(destinationId);

            conn.updateWithParams(QUERY,params, reply->{
                try{
                    if(reply.succeeded()){
                        GenericQuery create = this.generateGenericCreate("travel_tracking", new JsonObject()
                                .put(SCHEDULE_ROUTE_ID, scheduleRoute)
                                .put(SCHEDULE_ROUTE_DESTINATION_ID, destinationId)
                                .put(CREATED_BY, userId)
                                .put(STATUS, scheduleStatus.getValue()));
                        conn.updateWithParams(create.getQuery(), create.getParams(), replyInsertTravelTracking -> {
                            try {
                                if (replyInsertTravelTracking.failed()){
                                    throw replyInsertTravelTracking.cause();
                                }

                                future.complete(reply.succeeded());

                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });

                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Integer> getTerminalOriginId(SQLConnection conn, Integer scheduleRouteId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(GET_TERMINAL_ORIGIN, new JsonArray().add(scheduleRouteId), reply -> {
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("TERMINALS ORIGIN NOT FOUND");
                    }
                    future.complete(result.get(0).getInteger(_TERMINAL_ORIGIN_ID));
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateRouteStatus(SQLConnection conn, int scheduleRouteId, SCHEDULE_STATUS scheduleStatus, int terminalId, int terminalOriginId, int userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            String query = terminalId == terminalOriginId ? UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT : UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT;
            JsonArray params = new JsonArray()
                    .add(scheduleStatus.getValue())
                    .add(userId)
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(scheduleRouteId);

            conn.updateWithParams(query,params,reply->{
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

    private static final String QUERY_GET_IN_TRANSIT_CODES = "select bpt.tracking_code from shipments AS s       \n" +
            "\tINNER JOIN shipments_ticket_tracking AS stt ON stt.shipment_id = s.id AND s.shipment_type = 'load'\n" +
            "    INNER JOIN boarding_pass_ticket AS bpt ON bpt.id = stt.boarding_pass_ticket_id\n" +
            "    INNER JOIN boarding_pass_route AS bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "    INNER JOIN schedule_route_destination AS srd ON srd.id = bpr.schedule_route_destination_id \n" +
            "\t\tAND srd.terminal_destiny_id = (SELECT terminal_id FROM shipments WHERE id = ?) \n" +
            "\t\tAND s.schedule_route_id = (SELECT schedule_route_id FROM shipments WHERE id = ?) AND bpt.ticket_status = 2;";

    private static final String QUERY_GET_IN_TRANSIT_COMPLEMENTS = "select bpc.tracking_code from shipments AS s       \n" +
            "\tINNER JOIN shipments_ticket_tracking AS stt ON stt.shipment_id = s.id AND s.shipment_type = 'load'\n" +
            "    INNER JOIN boarding_pass_ticket AS bpt ON bpt.id = stt.boarding_pass_ticket_id\n" +
            "    LEFT JOIN boarding_pass_complement AS bpc ON bpc.boarding_pass_ticket_id = bpt.id\n" +
            "    INNER JOIN boarding_pass_route AS bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "    INNER JOIN schedule_route_destination AS srd ON srd.id = bpr.schedule_route_destination_id \n" +
            "\t\tAND srd.terminal_destiny_id = (SELECT terminal_id FROM shipments WHERE id = ?) \n" +
            "\t\tAND s.schedule_route_id = (SELECT schedule_route_id FROM shipments WHERE id = ?) AND bpt.ticket_status = 2;";

    private static final String QUERY_GET_SHIPMENT = "SELECT * FROM shipments where id = ? ;";

    private final static String QUERY_STATUS_TRACKING_TRAVEL = "SELECT \n" +
            "   sr.schedule_status,\n" +
            "   cr.terminal_destiny_id,\n" +
            "   (SELECT GROUP_CONCAT(st.trailer_id) FROM shipments_trailers st\n" +
            "       WHERE st.schedule_route_id = sr.id\n" +
            "       AND st.action IN ('assign', 'hitch', 'transfer')\n" +
            "       AND st.latest_movement IS TRUE\n" +
            "   ) AS hitched_trailers\n" +
            "FROM schedule_route sr\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "WHERE sr.id = ?;";

    private final static String GET_ROUTE_DESTINATIONS = "SELECT * FROM schedule_route_destination \n" +
            "WHERE schedule_route_id = ? \n" +
            "AND (terminal_origin_id = ? OR terminal_destiny_id = ?) \n" +
            "AND destination_status NOT IN ('canceled', 'finished-ok')";

    private static final String GET_TERMINAL_ORIGIN = "SELECT cr.terminal_origin_id FROM schedule_route AS sr JOIN config_route AS cr ON cr.id=sr.config_route_id where sr.id = ?";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?, updated_at = ?, finished_at = ? WHERE id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?,updated_at = ?, started_at = ? WHERE id = ?  ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_LOAD = "UPDATE schedule_route_destination set destination_status='ready-to-load' where id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_FINISHED_OK = "UPDATE schedule_route_destination set destination_status = 'finished-ok', finished_at = ? where id = ? and (destination_status='downloading')";

    private final static String UPDATE_SHIPMENT_TOTALS = "UPDATE shipments SET total_tickets = total_tickets + ?, total_complements = total_complements + ? WHERE id = ?;";

}
