package database.routes.handlers.TravelTrackingDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.routes.TravelTrackingDBV;
import database.routes.handlers.enums.TRAVELTRACKING_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID;
import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import static database.routes.TravelTrackingDBV.DRIVER_ID;
import static database.routes.TravelTrackingDBV.RESPONSE_LIST;
import static service.commons.Constants.*;

public class InitTravel extends DBHandler<TravelTrackingDBV> {

    public InitTravel(TravelTrackingDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn)->{
            try{
                JsonObject body = message.body();
                Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer driverId = body.getInteger(DRIVER_ID);
                String coordinates = body.getString("location_started");
                int userId = body.getInteger(CREATED_BY);

                this.changeStatus(conn, body, userId).whenComplete((resCS, errCS)->{
                    try{
                        if(errCS != null ){
                            throw errCS;
                        }
                        this.InsertTracking(conn, terminalId, scheduleRouteId, userId).whenComplete((resIT, errorIT) -> {
                            try {
                                if(errorIT != null){
                                    throw errorIT;
                                }
                                this.insertDriverTracking(conn, scheduleRouteId, driverId, terminalId, coordinates, userId).whenComplete((resIDT, errorIDT)->{
                                    try {
                                        if(errorIDT != null){
                                            throw errorIDT;
                                        }
                                        initScheduleRouteDriver(conn, terminalId, driverId, scheduleRouteId, userId).whenComplete((resISRD, errISRD)->{
                                            try {
                                                if(errISRD != null){
                                                    throw errISRD;
                                                }
                                                this.commit(conn, message, resCS);
                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> changeStatus(SQLConnection conn, JsonObject body, int userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray paramsGetStatus = new JsonArray()
                .add(body.getInteger("schedule_route_id"));
        body.remove("shipment_id");

        Integer scheduleRouteId = body.getInteger("schedule_route_id");
        Integer terminalOriginId = body.getInteger("terminal_id");
        this.getStatusTravel(conn, paramsGetStatus).whenComplete((res, err) -> {
            try {
                if (err != null) {
                    throw new Exception(err);
                }

                String scheduleStatus = res.getString("schedule_status");

                this.getDestinations(conn, scheduleRouteId, terminalOriginId).whenComplete((destinations,desErr)->{
                    try{
                        if(desErr!=null){
                            throw new Exception(desErr);
                        }
                        this.isValidStatus(scheduleStatus != null ? scheduleStatus : "").whenComplete((result, error) -> {
                            try {
                                if (error!=null){
                                    throw new Exception(error);
                                }
                                if(!RESPONSE_LIST.getString(0).equals(RESPONSE_LIST.getString(result))) {
                                    throw new Exception(RESPONSE_LIST.getString(result) + "FROM "
                                            + (res.getString("status") != null ? res.getString("status") : scheduleStatus !=null ? scheduleStatus:" TRAVEL STATUS NOT DEFINED"));
                                }
                                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                                for(JsonObject destination : destinations){
                                    int destinationId = destination.getInteger("id");
                                    tasks.add(this.updDestination(conn, scheduleRouteId, destinationId, userId));
                                }
                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((__, errTasks) -> {
                                    try {
                                        if (errTasks != null) {
                                            throw new Exception(errTasks);
                                        }

                                        this.getOriginTerminal(conn , body.getInteger("schedule_route_id"))
                                                .whenComplete( (resulT , errorT) ->{
                                                    try{
                                                        if(errorT!= null){
                                                            throw new Exception(errorT);
                                                        }
                                                        body.put("terminal_origin" , resulT);
                                                        this.updateRouteStatus(conn,body)
                                                                .whenComplete((resu,erro)->{
                                                                    try {
                                                                        if(erro!= null){
                                                                            throw new Exception(erro);
                                                                        }

                                                                        future.complete(new JsonObject().put("success", true));
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
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getStatusTravel(SQLConnection conn, JsonArray params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(InitTravel.QUERY_STATUS_TRACKING_TRAVEL,params, reply->{
            try{
                if(reply.succeeded()){
                    if(reply.result().getNumRows()>0){
                        JsonObject result = reply.result().getRows().get(0);
                        result.put("success",true);
                        future.complete(result);
                    }else{
                        future.completeExceptionally(new Throwable("STATUS NOT FOUND FOR THAT ROUTE AND DESTINATION"));
                    }
                }else{
                    future.completeExceptionally(reply.cause());
                }
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getDestinations(SQLConnection conn, Integer schedule_route_id, Integer terminal_id){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try{
            conn.queryWithParams(GET_ROUTE_DESTINATIONS, new JsonArray().add(schedule_route_id).add(terminal_id), reply -> {
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

    private CompletableFuture<Integer> isValidStatus(String actStatus){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            boolean responseOK = actStatus.equals(TRAVELTRACKING_STATUS.READY_TO_GO.getValue()) || actStatus.equals(TRAVELTRACKING_STATUS.PAUSED.getValue());
            future.complete(responseOK ? TravelTrackingDBV.RESPONSE_CHANGE_STATUS.OK.ordinal() : TravelTrackingDBV.RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_IN_TRANSIT.ordinal());
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updDestination(SQLConnection conn, Integer scheduleRoute, Integer scheduleRouteDestination, int userId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray()
                .add(UtilsDate.sdfDataBase(UtilsDate.getDateConvertedTimeZone(UtilsDate.timezone, new Date())))
                .add(scheduleRouteDestination);

            conn.updateWithParams(UPDATE_SCHEDULE_ROUTE_DESTINATIONS_IN_TRANSIT,params, reply->{
                try{
                    if(reply.succeeded()){

                        GenericQuery create = this.generateGenericCreate("travel_tracking", new JsonObject()
                                .put(SCHEDULE_ROUTE_ID, scheduleRoute)
                                .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestination)
                                .put(CREATED_BY, userId)
                                .put(Constants.STATUS, "in-transit"));
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

    private CompletableFuture<Integer> getOriginTerminal(SQLConnection conn,Integer schedule_route){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray()
                    .add(schedule_route);
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

    private CompletableFuture<Boolean> updateRouteStatus(SQLConnection conn,JsonObject body){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer id = body.getInteger("schedule_route_id");
            Integer updated_by = body.getInteger(CREATED_BY);

            String query = UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT;
            JsonArray params = new JsonArray()
                    .add(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())
                    .add(updated_by)
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()));
            if(body.getInteger("terminal_id").equals(body.getInteger("terminal_origin"))){
                query = UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT;
            }

            params.add(id);

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

    private CompletableFuture<Boolean> InsertTracking(SQLConnection conn, Integer terminalId, Integer scheduleRouteId, int userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
            JsonArray params = new JsonArray()
                    .add(terminalId)
                    .add(scheduleRouteId)
                    .add("loaded");
            conn.queryWithParams(GET_COMPLEMENTS_TO_INSERT_TRACKING, params, replyComplements->{
                try {
                    if (replyComplements.failed()) {
                        throw replyComplements.cause();
                    }

                    List<JsonObject> complements = replyComplements.result().getRows();
                    for (JsonObject shipmentComDetail : complements) {
                        Integer bpComplementId = shipmentComDetail.getInteger("boarding_pass_complement_id");
                        tasks.add(insertTrackingBoardingComplements(conn, bpComplementId, userId));
                    }

                    conn.queryWithParams(GET_TICKETS_TO_INSERT_TRACKING, params, replyTickets->{
                        try {
                            if (replyTickets.failed()) {
                                throw replyTickets.cause();
                            }

                            List<JsonObject> tickets = replyTickets.result().getRows();
                            for (JsonObject shipmentTickDetail : tickets) {
                                Integer bpticketId = shipmentTickDetail.getInteger("boarding_pass_ticket_id");
                                tasks.add(insertTrackingBoardingTickets(conn, bpticketId, userId));
                            }

                            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((bol,err)->{
                                try {
                                    if(err!=null){
                                        throw err;
                                    }
                                    future.complete(true);
                                }catch (Throwable t){
                                    future.completeExceptionally(t);
                                }
                            });
                        }catch (Throwable t){
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

    private CompletableFuture<Boolean> insertTrackingBoardingTickets(SQLConnection conn, Integer id, int userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(QUERY_GET_BOARDING_PASS_ID_OF_TICKET, new JsonArray().add(id), reply -> {
                try {
                    if(reply.succeeded()){
                        Integer boardingPassId = reply.result().getRows().get(0).getInteger("boarding_pass_id");
                        JsonArray items = new JsonArray()
                                .add(new JsonObject()
                                        .put(ID,id)
                                        .put("boardingpass_id",boardingPassId));

                        this.insertTracking(conn,items,"boarding_pass_tracking","boardingpass_id", "boardingpass_ticket_id", "intransit", userId)
                                .whenComplete((res,err)->{
                                    try {
                                        if(err!=null){
                                            future.completeExceptionally(err);
                                        }else{
                                            future.complete(res);
                                        }
                                    }catch (Exception e){
                                        future.completeExceptionally(e);
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

    private CompletableFuture<Boolean> insertTrackingBoardingComplements(SQLConnection conn, Integer id, int userId)                      {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(QUERY_GET_BOARDING_PASS_ID_OF_COMPLEMENT, new JsonArray().add(id), reply -> {
                try {
                    if(reply.succeeded()){
                        Integer boardingPassId = reply.result().getRows().get(0).getInteger("boarding_pass_id");
                        JsonArray items = new JsonArray()
                                .add(new JsonObject()
                                        .put(ID,id)
                                        .put("boardingpass_id",boardingPassId));

                        this.insertTracking(conn,items,"boarding_pass_tracking","boardingpass_id", "boardingpass_complement_id","intransit", userId)
                                .whenComplete((res,err)->{
                                    try {
                                        if(err!=null){
                                            future.completeExceptionally(err);
                                        }else{
                                            future.complete(res);
                                        }
                                    }catch (Exception e){
                                        future.completeExceptionally(e);
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

    protected CompletableFuture<Boolean> insertTracking(SQLConnection conn, JsonArray items, String trackingTable, String principalField, String reference, String action, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<CompletableFuture<Boolean>> execQueryList = new ArrayList<>();
            for(int i=0; i<items.size(); i++){
                JsonObject obj = items.getJsonObject(i);
                JsonObject track = new JsonObject()
                        .put(reference, obj.getInteger("id"))
                        .put("action", action)
                        .put("created_by", createdBy);
                if(principalField != null){
                    track.put(principalField, obj.getInteger(principalField));
                }
                if(obj.getInteger("terminal_id") != null){
                    track.put("terminal_id", obj.getInteger("terminal_id"));
                }
                if(action.equals("printed")){
                    Integer prints = obj.getInteger("prints_counter");
                    if(prints == 1){
                        track.put("notes", "Impresión");
                    } else {
                        int reprints = prints-1;
                        track.put("notes", "Reimpresión #"+reprints);
                    }
                }
                execQueryList.add(UtilsTravel.execGenericQuery(conn, this.generateGenericCreate(trackingTable, track)));
            }
            CompletableFuture.allOf(execQueryList.toArray(new CompletableFuture[execQueryList.size()])).whenComplete((reply, err) -> {
                try {
                    if (err != null){
                        throw err;
                    }

                    future.complete(true);

                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertDriverTracking(SQLConnection conn, Integer scheduleRouteId, Integer driverId, Integer terminalId, String location, int userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            JsonArray params = new JsonArray()
                    .add(driverId)
                    .add(terminalId)
                    .add(terminalId);

            conn.queryWithParams(QUERY_GET_DRIVER_TRACKING_ACTIVE, params, reply->{
                try {
                    if (reply.succeeded()){
                        if (reply.result().getNumRows()>0){
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");

                            JsonObject tracking = reply.result().getRows().get(0);
                            Date date = formatter.parse(tracking.getString("created_at"));
                            long milliseconds = calendar.getTime().getTime()-date.getTime();
                            long h = TimeUnit.MILLISECONDS.toHours(milliseconds);
                            Long min = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - h*60;
                            Long seg = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
                            String timeTracking = Time.valueOf(String.format("%d:%d:%d", h, min, seg)).toString();

                            conn.queryWithParams(QUERY_GET_ORDER_ORIGIN, new JsonArray().add(terminalId).add(scheduleRouteId), result->{
                                try{
                                    if(result.succeeded()){
                                        if(result.result().getNumRows()>0){
                                            JsonObject configDestination = result.result().getRows().get(0);
                                            GenericQuery updateDriverTracking = this.generateGenericUpdate("driver_tracking", new JsonObject()
                                                    .put(ID,tracking.getInteger(ID))
                                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                                    .put(EMPLOYEE_ID, driverId)
                                                    .put("location_finished",location)
                                                    .put(UPDATED_AT,formatter.format(calendar.getTime()))
                                                    .put(UPDATED_BY, userId)
                                                    .put("time_tracking",timeTracking)
                                                    .put("status", TRAVELTRACKING_STATUS.FINISHED.getValue()));
                                            conn.updateWithParams(updateDriverTracking.getQuery(), updateDriverTracking.getParams(), res -> {
                                                try{
                                                    if(res.succeeded()){
                                                        GenericQuery createDriverTracking = this.generateGenericCreate("driver_tracking", configDestination
                                                                .put(EMPLOYEE_ID, driverId)
                                                                .put("time_tracking",Time.valueOf("00:00:00").toString())
                                                                .put("was_completed",0)
                                                                .put(CREATED_BY, userId)
                                                                .put("location_started", location)
                                                                .put("action","driving"));
                                                        conn.updateWithParams(createDriverTracking.getQuery(), createDriverTracking.getParams(), resp -> {
                                                            try{
                                                                if(resp.succeeded()){
                                                                    future.complete(resp.succeeded());
                                                                }else {
                                                                    future.completeExceptionally(resp.cause());
                                                                }
                                                            }catch (Exception e){
                                                                future.completeExceptionally(e);
                                                            }
                                                        });
                                                    }else {
                                                        future.completeExceptionally(res.cause());
                                                    }
                                                }catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });


                                        }else {
                                            future.completeExceptionally(new Throwable("No se pudo obtener la configuracion de la ruta"));
                                        }
                                    }else{
                                        future.completeExceptionally(new Throwable("Error insertando el tracking del driver"));
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }
                        else{
                            conn.queryWithParams(QUERY_GET_ORDER_ORIGIN, new JsonArray().add(terminalId).add(scheduleRouteId), result->{
                                try {
                                    if(result.succeeded()){
                                        if(result.result().getNumRows()>0){
                                            JsonObject info = result.result().getRows().get(0);
                                            GenericQuery createDriverTracking = this.generateGenericCreate("driver_tracking", info
                                                    .put(EMPLOYEE_ID, driverId)
                                                    .put("time_tracking", "00:00:00")
                                                    .put("was_completed",0)
                                                    .put(CREATED_BY, userId)
                                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                                    .put("location_started", location));
                                            conn.updateWithParams(createDriverTracking.getQuery(), createDriverTracking.getParams(), res -> {
                                                try {
                                                    if(res.succeeded()){
                                                        future.complete(res.succeeded());
                                                    }else{
                                                        future.completeExceptionally(res.cause());
                                                    }
                                                }catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        }else{
                                            future.completeExceptionally(new Throwable("No se pudo obtener la configuracion de la ruta"));
                                        }
                                    }else{
                                        future.completeExceptionally(result.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }
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

    private CompletableFuture<Boolean> initScheduleRouteDriver(SQLConnection conn, Integer terminalId, Integer driverId, Integer scheduleRouteId, int userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_SCHEDULE_DRIVER + " AND terminal_origin_id = ? " + QUERY_GET_SCHEDULE_DRIVER_ORDER_BY_ASC;
            JsonArray params =  new JsonArray()
                    .add(driverId)
                    .add(scheduleRouteId)
                    .add(terminalId);

            conn.queryWithParams(QUERY, params, reply->{
                try {
                    if(reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()){
                        future.completeExceptionally(new Throwable("Not found schedule_route_driver open"));
                    }

                    JsonObject currentSRD = reply.result().getRows().get(0);
                    GenericQuery update = this.generateGenericUpdate("schedule_route_driver",
                            currentSRD.put("driver_status", "2")
                                    .put(Constants.STATUS, 1)
                                    .put(UPDATED_BY, userId)
                                    .put(UPDATED_AT, UtilsTravel.FormatDate(Calendar.getInstance().getTime())));

                    conn.updateWithParams(update.getQuery(), update.getParams(), replyBatch -> {
                        try {
                            if (replyBatch.failed()){
                                throw replyBatch.cause();
                            }

                            future.complete(replyBatch.succeeded());

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

    private final static String QUERY_STATUS_TRACKING_TRAVEL = "SELECT schedule_status FROM schedule_route where id = ? ";

    private final static String GET_ROUTE_DESTINATIONS = "SELECT * FROM schedule_route_destination " +
            "where schedule_route_id = ? \n" +
            "and destination_status != 'canceled' \n" +
            "and destination_status != 'finished-ok' \n" +
            "AND terminal_origin_id = ?";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_IN_TRANSIT = "UPDATE schedule_route_destination set destination_status='in-transit', started_at = ? where id= ?  and (destination_status='ready-to-go' or destination_status='paused')";

    private static final String GET_TERMINAL_ORIGIN = "SELECT cr.terminal_origin_id FROM schedule_route AS sr JOIN config_route AS cr ON cr.id=sr.config_route_id where sr.id = ?";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?, updated_at = ?, finished_at = ? WHERE id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?,updated_at = ?, started_at = ? WHERE id = ?  ";

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

    private final static String GET_COMPLEMENTS_TO_INSERT_TRACKING = "SELECT " +
            "s.total_complements, " +
            "sc.* " +
            "FROM shipments_complement_tracking AS sc " +
            "INNER JOIN shipments AS s ON s.id = sc.shipment_id " +
            "WHERE s.terminal_id = ? AND s.schedule_route_id = ? AND sc.status = ? ";

    private final static String GET_TICKETS_TO_INSERT_TRACKING = "SELECT " +
            "s.total_tickets, " +
            "sc.* " +
            "FROM shipments_ticket_tracking AS sc " +
            "INNER JOIN shipments AS s ON s.id = sc.shipment_id " +
            "WHERE s.terminal_id = ? AND s.schedule_route_id = ? AND sc.status = ? ";

    private final static String GET_PACKAGES_TO_INSERT_TRACKING = "SELECT " +
            "s.total_packages, " +
            "sc.* " +
            "FROM shipments_parcel_package_tracking AS sc " +
            "INNER JOIN shipments AS s ON s.id = sc.shipment_id " +
            "WHERE s.terminal_id = ? AND s.schedule_route_id = ? AND sc.status = ? ";

    private static final String QUERY_GET_BOARDING_PASS_ID_OF_COMPLEMENT = "SELECT boarding_pass_id FROM boarding_pass_complement WHERE id = ? ; ";

    private static final String QUERY_GET_BOARDING_PASS_ID_OF_TICKET = "SELECT bpp.boarding_pass_id FROM boarding_pass_ticket AS bpt " +
            "INNER JOIN boarding_pass_passenger AS bpp ON bpp.id=bpt.boarding_pass_passenger_id " +
            "WHERE bpt.id = ? ;";

    private static final String QUERY_GET_DRIVER_TRACKING_ACTIVE = "SELECT id, time_tracking, status, action, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at FROM driver_tracking WHERE employee_id = ? and (terminal_origin_id = ? or terminal_destiny_id = ?) AND status = 'in-transit' ;";

    private static final String QUERY_GET_ORDER_ORIGIN = "SELECT " +
            "sr.vehicle_id, " +
            "cd.terminal_origin_id, " +
            "cd.terminal_destiny_id " +
            "FROM config_destination as cd " +
            "INNER JOIN schedule_route_destination as srd ON srd.config_destination_id = cd.id " +
            "INNER JOIN schedule_route as sr ON sr.id = srd.schedule_route_id " +
            "INNER JOIN branchoffice as bo ON bo.id = cd.terminal_origin_id " +
            "INNER JOIN branchoffice as bd ON bd.id = cd.terminal_destiny_id " +
            "where cd.terminal_origin_id = ? AND srd.schedule_route_id = ? AND (cd.order_origin+1)=cd.order_destiny;";

    private static final String QUERY_GET_SCHEDULE_DRIVER = "SELECT " +
            "  id, " +
            "  DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at, " +
            "  DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updated_at " +
            "FROM schedule_route_driver " +
            "WHERE employee_id = ? " +
            "AND schedule_route_id = ? " +
            "AND status = 1 ";
    private static final String QUERY_GET_SCHEDULE_DRIVER_ORDER_BY_ASC = " ORDER BY id ASC LIMIT 1; ";

    private static final String QUERY_UPDATE_PARCELS_IN_TRANSIT = "UPDATE parcels SET parcel_status = "+ PARCEL_STATUS.IN_TRANSIT.ordinal() +", updated_by = %d, updated_at = '%s'" +
            "WHERE id IN (%s);";

    private static final String QUERY_SET_PARCELS_PACKAGES_INTRANSIT = "UPDATE parcels_packages SET package_status = "+ PACKAGE_STATUS.IN_TRANSIT.ordinal() +", updated_by = %d, updated_at = '%s'" +
            "WHERE id IN (%s);";

}
