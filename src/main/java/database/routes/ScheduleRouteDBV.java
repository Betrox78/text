/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.routes;

import database.boardingpass.BoardingPassDBV;
import database.boardingpass.TicketPricesRulesDBV;
import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import database.routes.handlers.ScheduleRouteDBV.RouteSeatLock;
import database.routes.handlers.ScheduleRouteDBV.RouteSeatLocksExpiration;
import database.routes.handlers.ScheduleRouteDBV.RouteSeatUnlock;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.joda.time.DateTime;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsDate.InvalidRangeDateException;
import utils.UtilsDate.RangeDate;
import utils.UtilsGoogleMaps;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.authorizationcodes.AuthorizationCodesDBV.CODE;
import static database.boardingpass.BoardingPassDBV.*;
import static database.boardingpass.TicketPricesRulesDBV.ACTION_BATCH_APPLY_TICKET_PRICE_RULE;
import static database.promos.PromosDBV.*;
import static database.shipments.ShipmentsDBV.TRAVEL_DATE;
import static service.commons.Constants.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ScheduleRouteDBV extends DBVerticle {

    public static final String CURRENT_POSITION = "current_position";
    public static final String TERMINAL_ORIGIN_ID = "toid";
    public static final String TERMINAL_DESTINY_ID = "tdid";
    public static final String DATE_TRAVEL = "dt";
    public static final String HOUR_TRAVEL = "ht";
    public static final String SPECIAL_TICKETS_LIST = "stl";
    public static final String CONFIG_ROUTE_ID = "crid";
    public static final String SCHEDULE_ROUTE_ID = "srid";
    public static final String ACTION_REMOVE_SCHEDULE_ROUTE = "ScheduleRouteDBV.removeScheduleRoute";
    public static final String ACTION_SEARCH_SCHEDULE_DETAIL = "ScheduleRouteDBV.searchScheduleRouteDetail";
    public static final String ACTION_CREATE_TEMPLATE = "ScheduleRouteDBV.createTemplate";
    public static final String ACTION_REGISTER = "ScheduleRouteDBV.register";
    public static final String ACTION_SCHEDULE_ROUTES_LIST = "ScheduleRouteDBV.scheduleRoutesList";
    public static final String ACTION_GET_TEMPLATE_LIST = "ScheduleRouteDBV.templateList";
    public static final String ACTION_GET_TEMPLATE_DETAIL = "ScheduleRouteDBV.templateDetail";
    public static final String ACTION_SEARCH_BY_TERMINALS = "ScheduleRouteDBV.serachByTerminals";
    public static final String ACTION_SEARCH_STOPS = "ScheduleRouteDBV.serachStops";
    public static final String ACTION_SUMMARY_COST = "ScheduleRouteDBV.summaryCost";
    public static final String ACTION_SEARCH_SEATS = "ScheduleRouteDBV.serachSeats";
    public static final String DEPARTURES_CALENDAR_LIST = "ScheduleRouteDBV.getDeparturesCalendar";
    public static final String GET_SCHEDULE_ROUTE_DETAIL = "ScheduleRouteDBV.getScheduleRouteDetail";
    public static final String ACTION_GET_ROUTES_BY_TERMINALS = "ScheduleRouteDBV.getRoutesByTerminals";
    public static final String ACTION_AVAILABLE_SEATS_BY_DESTINATION = "ScheduleRouteDBV.availableSeatsByDestination";
    public static final String CHANGE_DRIVER = "ScheduleRouteDBV.changeDriver";
    public static final String CHANGE_SECOND_DRIVER = "ScheduleRouteDBV.changeSecondDriver";
    public static final String ACTION_DRIVER_SCHEDULE_ROUTES = "ScheduleRouteDBV.driverScheduleRoutes";
    public static final String ACTION_DRIVER_SCHEDULE_ROUTES_BY_ID = "ScheduleRouteDBV.driverScheduleRoutesById";
    public static final String ACTION_DRIVER_SCHEDULE_ROUTE_AVAILABLE_SEATS = "ScheduleRouteDBV.driverScheduleRouteAvailableSeats";
    public static final String ACTION_DRIVER_START_SCHEDULE_ROUTE = "ScheduleRouteDBV.driverStartScheduleRoute";
    public static final String ACTION_DRIVER_START_SCHEDULE_ROUTE_DESTINATION = "ScheduleRouteDBV.driverStartScheduleRouteDestinations";
    public static final String ACTION_AVAILABLE_ROUTES_FROM_ORIGIIN = "ScheduleRouteDBV.availableRoutesFromOrigin";
    public static final String ACTION_GET_DRIVER_DESTINATIONS = "ScheduleRouteDBV.getDriverDestinations";
    public static final String ACTION_GET_TERMINALS_BY_DESTINATION = "ScheduleRouteDBV.getTerminalsByDestination";
    public static final String ACTION_DRIVER_CALCULATE_TICKET_PRICE = "ScheduleRouteDBV.driverCalculateTicketPrice";
    public static final String ACTION_DRIVER_GET_AVAILABLE_SEAT = "ScheduleRouteDBV.getDriverAvailableSeat";
    public static final String ACTION_GET_SCHEDULE_ROUTE_INFO_BY_VEHICLE_ID = "ScheduleRouteDBV.getScheduleRouteInfoByVehicleId";
    public static final String ACTION_GET_SCHEDULE_ROUTE_INFO_BY_DESTINATION_ID = "ScheduleRouteDBV.getScheduleRouteInfoByDestinationId";
    public static final String GET_CONFIG_ROUTE_STATUS = "ScheduleRouteDBV.getConfigRouteStatus";
    public static final String ACTION_CHANGE_VEHICLE = "ScheduleRouteDBV.changeVehicle";
    public static final String TERMINAL_ORIGIN_ID_DRIVER = "terminal_origin_id";
    public static final String TERMINAL_DESTINY_ID_DRIVER = "terminal_destiny_id";
    public static final String GET_ROUTE_STATUS_REPORT = "ScheduleRouteDBV.statusRouteReport";
    public static final String CHANGE_STATUS_HIDE_ROUTE = "ScheduleRouteDBV.changeHideStatus";

    public static final String SEAT_LOCK = "ScheduleRouteDBV.seatLock";

    public static final String SEAT_UNLOCK = "ScheduleRouteDBV.seatUnlock";

    public static final String EXPIRE_SEAT_LOCKS = "ScheduleRouteDBV.expireSeatLocks";

    private RouteSeatLock routeSeatLock;
    private RouteSeatUnlock routeSeatUnlock;
    private RouteSeatLocksExpiration routeSeatLocksExpiration;

    @Override
    public String getTableName() {
        return "schedule_route";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.routeSeatLock = new RouteSeatLock(this);
        this.routeSeatUnlock = new RouteSeatUnlock(this);
        this.routeSeatLocksExpiration = new RouteSeatLocksExpiration(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_SCHEDULE_ROUTES_LIST:
                this.scheduleRouteList(message);
                break;
            case ACTION_GET_TEMPLATE_LIST:
                this.templateList(message);
                break;
            case ACTION_GET_TEMPLATE_DETAIL:
                this.templateDetail(message);
                break;
            case ACTION_DRIVER_SCHEDULE_ROUTES:
                this.driverScheduleRoutes(message);
                break;
            case ACTION_DRIVER_SCHEDULE_ROUTES_BY_ID:
                this.driverScheduleRoutesById(message);
                break;
            case CHANGE_DRIVER:
                this.changeDriver(message);
                break;
            case CHANGE_SECOND_DRIVER:
                this.changeSecondDriver(message);
                break;
            case ACTION_DRIVER_SCHEDULE_ROUTE_AVAILABLE_SEATS:
                this.driverScheduleRouteAvailableSeats(message);
                break;
            case ACTION_DRIVER_START_SCHEDULE_ROUTE:
                this.driverStartScheduleRoute(message);
                break;
            case ACTION_DRIVER_START_SCHEDULE_ROUTE_DESTINATION:
                this.driverStartScheduleRouteDestinations(message);
                break;
            case ACTION_REMOVE_SCHEDULE_ROUTE:
                this.removeScheduleRoute(message);
                break;
            case ACTION_SEARCH_SCHEDULE_DETAIL:
                this.searchScheduleRouteDetail(message);
                break;
            case ACTION_SEARCH_BY_TERMINALS:
                this.searchByTerminals(message);
                break;
            case ACTION_SEARCH_STOPS:
                this.searchStops(message);
                break;
            case ACTION_CREATE_TEMPLATE:
                this.createTemplate(message);
                break;
            case ACTION_SUMMARY_COST:
                this.summaryCost(message);
                break;
            case ACTION_SEARCH_SEATS:
                this.searchSeatsGoodOne(message);
                break;
            case DEPARTURES_CALENDAR_LIST:
                this.getDeparturesCalendar(message);
                break;
            case GET_SCHEDULE_ROUTE_DETAIL:
                this.getScheduleRouteDetail(message);
                break;
            case ACTION_GET_ROUTES_BY_TERMINALS:
                this.getRoutesByTerminals(message);
                break;
            case ACTION_AVAILABLE_SEATS_BY_DESTINATION:
                this.getAvailableSeatsByDestination(message);
                break;
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_AVAILABLE_ROUTES_FROM_ORIGIIN:
                this.getRoutesByOrigin(message);
                break;
            case ACTION_GET_DRIVER_DESTINATIONS:
                this.getDriverDestinations(message);
                break;
            case ACTION_GET_TERMINALS_BY_DESTINATION:
                this.getTerminalsByDestination(message);
                break;
            case ACTION_DRIVER_CALCULATE_TICKET_PRICE:
                this.driverCalculateTicketPrice(message);
                break;
            case ACTION_DRIVER_GET_AVAILABLE_SEAT:
                this.getDriverAvailableSeat(message);
                break;
            case GET_CONFIG_ROUTE_STATUS:
                this.getConfigRouteStatus(message);
                break;
            case ACTION_GET_SCHEDULE_ROUTE_INFO_BY_VEHICLE_ID:
                this.getScheduleRouteInfoByVehicleId(message);
                break;
            case ACTION_GET_SCHEDULE_ROUTE_INFO_BY_DESTINATION_ID:
                this.getScheduleRouteInfoByDestinationId(message);
                break;
            case ACTION_CHANGE_VEHICLE:
            this.changeVehicle(message);
                break;
            case GET_ROUTE_STATUS_REPORT:
                this.statusRouteReport(message);
                break;
            case CHANGE_STATUS_HIDE_ROUTE:
                this.changeHideStatus(message);
                break;
            case SEAT_LOCK:
                this.routeSeatLock.handle(message);
                break;
            case SEAT_UNLOCK:
                this.routeSeatUnlock.handle(message);
                break;
            case EXPIRE_SEAT_LOCKS:
                this.routeSeatLocksExpiration.handle(message);
                break;
        }
    }
    private void removeScheduleRoute(Message<JsonObject> message){
        startTransaction(message , conn ->{
            try{
                JsonObject body = message.body();
                int scheduleRouteDestinationID = body.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
                final JsonArray params = new JsonArray().add(scheduleRouteDestinationID);
                conn.queryWithParams(QUERY_GET_ORDERS_BY_DESTINATION_REMOVE.concat(GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_DESTINATION_PARAM), params, reply -> {
                            try{
                                if(reply.failed()) {
                                    throw new Exception(reply.cause());
                                }
                                List<JsonObject> results = reply.result().getRows();
                                if (results.size() > 0) {
                                    JsonObject scheduleRouteDestination = results.get(0);
                                    Integer scheduleRouteId = scheduleRouteDestination.getInteger("schedule_route_id");
                                    Integer orderOrigin = scheduleRouteDestination.getInteger("order_origin");
                                    Integer orderDestiny = scheduleRouteDestination.getInteger("order_destiny");
                                    String QUERY = UPDATE_REMOVE_SCHEDULE_ROUTE_DESTINATION;
                                    final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                                            .add(orderOrigin).add(orderDestiny)
                                            .add(orderDestiny).add(orderDestiny)
                                            .add(orderOrigin).add(orderDestiny);
                                    if(!orderOrigin.equals(1)){
                                        QUERY += " OR (cd.order_origin >= ? ) ";
                                        paramsList.add(orderDestiny);
                                    }
                                    QUERY += " );";
                                    conn.updateWithParams(QUERY, paramsList, replyList -> {
                                        try{
                                            if(reply.failed()) {
                                                throw new Exception(reply.cause());
                                            }
                                            JsonArray paramsCancel = new JsonArray().add(scheduleRouteId);
                                            Future<ResultSet> cancelRoutes = Future.future();
                                            Future<ResultSet> schedulesRoutes = Future.future();
                                            conn.queryWithParams(QUERY_GET_SCHEDULES_DESTINATIONS,paramsCancel , cancelRoutes.completer());
                                            conn.queryWithParams(QUERY_GET_COUNT_SCHEDULES_DESTINATIONS , paramsCancel, schedulesRoutes.completer());
                                            CompositeFuture.all(cancelRoutes, schedulesRoutes).setHandler((AsyncResult<CompositeFuture> replyCanceled) -> {
                                                try{
                                                    if(replyCanceled.failed()){
                                                        throw new Exception(replyCanceled.cause());
                                                    }
                                                    ResultSet cancels = replyCanceled.result().resultAt(0);
                                                    ResultSet schedules = replyCanceled.result().resultAt(1);
                                                    List<JsonObject> listCancels = cancels.getRows();
                                                    List<JsonObject> listSchedules = schedules.getRows();
                                                    JsonObject scheduleDetail = listSchedules.get(0);
                                                    String scheduleRouteStatus = scheduleDetail.getString("schedule_status");
                                                    Integer totalSchedules = scheduleDetail.getInteger("total_schedules");
                                                    List<Integer>  destinations = new ArrayList<>();
                                                    for(int i=0; i < listCancels.size(); i++){
                                                        destinations.add(listCancels.get(i).getInteger("id"));
                                                    }
                                                    if(totalSchedules > 0){
                                                        JsonObject result = new JsonObject().put("schedule_status", scheduleRouteStatus).put("destinations", destinations);
                                                        this.commit(conn,message,new JsonObject().put("response","UPDATED").put("result", result));
                                                        return;
                                                    }
                                                    conn.updateWithParams(QUERY_UPDATE_SCHEDULE_STATUS , paramsCancel, replyUpdated ->{
                                                        try{
                                                            if(replyUpdated.failed()){
                                                                throw new Exception(replyUpdated.cause());
                                                            }
                                                            JsonObject result = new JsonObject().put("schedule_status", "canceled").put("destinations", destinations);
                                                            this.commit(conn,message,new JsonObject().put("response","UPDATED").put("result", result));
                                                        }catch (Exception e){
                                                            e.printStackTrace();
                                                            this.rollback(conn , e , message);
                                                        }
                                                    });
                                                }catch (Exception e){
                                                    e.printStackTrace();
                                                    this.rollback(conn, e , message);
                                                }
                                            });
                                        }catch (Exception ex) {
                                            ex.printStackTrace();
                                            this.rollback(conn,ex,message);
                                        }
                                    });
                                } else {
                                    throw new Exception("Failed updated schedule route destinations");
                                }
                            }catch (Exception ex) {
                                ex.printStackTrace();
                                this.rollback(conn,ex,message);
                            }
                        });
            }catch (Exception ex){
                this.rollback(conn,ex, message);
            }
        });
    }

    private void getDriverDestinations(Message<JsonObject> message){
        startTransaction(message,conn->{
            try{
                JsonObject body = message.body();
                Integer driverId = body.getInteger("driver_id");
                Integer scheduleRouteId = body.getInteger("schedule_route_id");
                Integer terminalId = body.getInteger("terminal_id");
                getDriverDestinyTerminals(conn, scheduleRouteId, driverId).whenComplete((reply,error)->{
                    try {
                        if(error != null){
                            throw error;
                        }
                        List<CompletableFuture<List<JsonObject>>> task = new ArrayList<>();
                        final int len = reply.size();
                        for(int i=0; i<len; i++){
                            JsonObject terminal = reply.get(i);
                            task.add(getDriverDestinyInfo(conn,scheduleRouteId,terminal.getInteger("terminal_destiny_id"), terminalId));
                        }
                        CompletableFuture.allOf(task.toArray(new CompletableFuture[len])).whenComplete((tRes,tErr)->{
                            try {
                                if(tErr!=null){
                                    throw tErr;
                                }
                                List<JsonObject> result = new ArrayList<>();
                                for (int i=0; i<task.size(); i++){
                                    result.addAll(task.get(i).get());
                                }
                                this.commit(conn,message,new JsonObject().put("destinations",result));
                            }catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    }catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Exception e){
                this.rollback(conn,e, message);
            }
        });
    }

    private void getScheduleRouteDetail(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer scheduleRouteId = body.getInteger("schedule_route_id");
        this.dbClient.queryWithParams(QUERY_GET_SCHEDULE_ROUTE_DETAIL, new JsonArray().add(scheduleRouteId), reply ->{
           try{
               if(reply.failed()){
                   throw new Exception(reply.cause());
               }
               List<JsonObject> result = reply.result().getRows();
               if(result.isEmpty()){
                   throw new Exception("Schedule route detail not found");
               }

               JsonObject configVehicle = result.get(0);
               Integer configRouteId = configVehicle.getInteger("config_route_id");
               this.dbClient.queryWithParams(QUERY_GET_STOPS_BY_SCHEDULE_ROUTE, new JsonArray().add(scheduleRouteId).add(configRouteId), repply->{
                    try{
                        if (repply.failed()){
                            throw new Exception(repply.cause());
                        }
                        List<JsonObject> resultStops = repply.result().getRows();
                        
                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<CompletableFuture<JsonObject>>();
                        for (int i = 0; i < resultStops.size(); i++) {
                            JsonObject stop = resultStops.get(i);
                            Integer orderOrigin = stop.getInteger("order_origin");
                            Integer orderDestiny = stop.getInteger("order_destiny");
                            tasks.add(getAvailableSeats(orderOrigin, orderDestiny , scheduleRouteId , resultStops, i));
                        }

                        CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                        all.whenComplete((resultt, error) -> {
                            try {
                                if (error != null){
                                    throw error;
                                }

                                this.dbClient.queryWithParams(QUERY_GET_TRAILERS_HISTORY_BY_SCHEDULE_ROUTE_ID, new JsonArray().add(scheduleRouteId), replyTH -> {
                                    try {
                                        if (replyTH.failed()) {
                                            throw replyTH.cause();
                                        }
                                        List<JsonObject> resultTrailersHistory = replyTH.result().getRows();
                                        message.reply(new JsonObject()
                                                .put("schedule_route_detail",result.get(0))
                                                .put("config_vehicle", configVehicle)
                                                .put("destinations" , resultStops)
                                                .put("trailers_history" , resultTrailersHistory));
                                    } catch (Throwable t) {
                                        reportQueryError(message, t);
                                    }
                                });

                            } catch (Throwable t){
                                t.printStackTrace();
                                 reportQueryError(message, t);
                            }
                        });
                    } catch (Exception ex){
                      ex.printStackTrace();
                      reportQueryError(message, ex);
                    }
               });
           } catch (Exception ex){
               ex.printStackTrace();
               reportQueryError(message, ex);
           }
        });
    }

    private void getDeparturesCalendar(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originBranchofficeId = body.getInteger("origin_branchoffice_id");
            Integer destinyBranchofficeId = body.getInteger("destiny_branchoffice_id");
            Integer vehicleId = body.getInteger("vehicle_id");
            boolean isCanceled = body.getBoolean("is_canceled", false);
            boolean isParcelRoute = body.getBoolean("is_parcel_route", false);
            String QUERY = QUERY_GET_DEPARTURES_CALENDAR;

            JsonArray params = new JsonArray().add(initDate).add(endDate);

            if(originBranchofficeId != null || destinyBranchofficeId != null){
                QUERY = QUERY_GET_DEPARTURES_CALENDAR_BY_TERMINAL;

                if(originBranchofficeId != null && destinyBranchofficeId != null){
                    QUERY = QUERY.concat(" AND srd.terminal_origin_id = ?  \n  AND srd.terminal_destiny_id = ? \n");
                    params.add(originBranchofficeId);
                    params.add(destinyBranchofficeId);
                }
                if(originBranchofficeId != null && destinyBranchofficeId == null){
                    QUERY = QUERY.concat(" AND srd.terminal_origin_id = ? AND srd.terminal_destiny_id = cr.terminal_destiny_id");
                    params.add(originBranchofficeId);
                }
                if(destinyBranchofficeId != null && originBranchofficeId == null){
                    QUERY = QUERY.concat(" AND srd.terminal_destiny_id = ? AND srd.terminal_origin_id = cr.terminal_origin_id");
                    params.add(destinyBranchofficeId);
                }
            }

            if(vehicleId != null){
                params.add(vehicleId);
                QUERY = QUERY.concat(" AND v.id = ? ");
            }

            QUERY = QUERY.concat(" AND cr.parcel_route = ? ");
            params.add(isParcelRoute);

            QUERY = isCanceled ? QUERY.concat(" AND sr.schedule_status = 'canceled' ") : QUERY.concat(" AND sr.schedule_status != 'canceled' ");

            QUERY = QUERY.concat(QUERY_FILTER_STATUS_DEPARTURES_CALENDAR);
            this.getCalendarList(QUERY, params).whenComplete( (result, error) ->{
                try{
                    if(error != null){
                        throw new Exception( error);
                    }
                    message.reply(result);
                }catch (Exception e){
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getAvailableSeats(Integer orderOrigin,Integer orderDestiny ,Integer scheduleRouteId ,List<JsonObject> resultStops,Integer i ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                .add(orderOrigin).add(orderDestiny)
                .add(orderDestiny).add(orderDestiny)
                .add(orderOrigin).add(orderDestiny);
        this.dbClient.queryWithParams(QUERY_AVAILABLE_SEAT_BY_DESTINATION, paramsList, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> resultsList = reply.result().getRows();
                List<String> boardingSeats = resultsList.stream()
                        .map(p -> p.getString("seat")).collect(Collectors.toList());
                resultStops.get(i).put("busy_seats", boardingSeats);
                future.complete(new JsonObject()
                        .put("busy_seats", new JsonArray(boardingSeats))
                        );
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getCalendarList(String QUERY, JsonArray params) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                if(result.isEmpty()){
                    throw new Exception("Schedules routes not found");
                }
                JsonObject res = new JsonObject().put("schedules_routes", result);
                future.complete(res);
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getDriverDestinyInfo(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer origin){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(scheduleRouteId)
                .add(terminalId)
                .add(origin);
        conn.queryWithParams(QUERY_GET_DRIVER_DESTINATIONS, params, reply->{
            try{
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(result);
            }catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return  future;
    }

    private CompletableFuture<List<JsonObject>> getDriverDestinyTerminals(SQLConnection conn, Integer scheduleRouteId, Integer userId){
        JsonArray params = new JsonArray()
                .add(userId)
                .add(scheduleRouteId);
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_TERMINALS_DESTINY, params, reply->{
            try{
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(result);
            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body().copy();
                JsonArray schedules = body.getJsonArray("schedules");
                Integer createdBy = body.getInteger("created_by");
                if (schedules == null) {
                    throw new Exception("schedules: missing required value");
                }
                int length = schedules.size();
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    JsonObject schedule = schedules.getJsonObject(i);
                    tasks.add(registerOneSchedule(conn, schedule, createdBy, message));
                }

                CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[length]));
                all.whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        this.commit(conn, message, new JsonObject().put("schedules" , schedules));
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private void createTemplate(Message<JsonObject> message){
        this.startTransaction(message , conn ->{
            try{
                JsonObject body = message.body().copy();
                JsonArray routes = body.getJsonObject("routes").getJsonArray("schedules");
                String templateName = body.getString("template_name");
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                for(int i = 0; i < routes.size(); i++){
                    tasks.add(updateScheduleRouteTemplate(conn, templateName));
                }
                for(int i = 0; i < routes.size(); i++){
                    JsonObject route = routes.getJsonObject(i);
                    tasks.add(registerTemplate(conn, route.getInteger("id") , templateName));
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]))
                        .whenComplete((s, t) -> {
                            try{
                                if(t != null){
                                    throw new Exception("Update schedule route template error");
                                }
                                this.commit(conn, message, body);
                            }catch (Exception e){
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
            }catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }
    private CompletableFuture<JsonObject> updateScheduleRouteTemplate(SQLConnection conn,String templateName){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.updateWithParams(UPDATE_SCHEDULE_ROUTE_TEMPLATE, new JsonArray().add(templateName) , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                Integer result = reply.result().getUpdated();
                future.complete(new JsonObject().put("result", result));
            }catch (Exception e ){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    private CompletableFuture<JsonObject> registerTemplate(SQLConnection conn , Integer id, String templateName){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.updateWithParams(UPDATE_NEW_SCHEDULE_ROUTE_TEMPLATE, new JsonArray().add(templateName).add(id) , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                Integer result = reply.result().getUpdated();
                future.complete(new JsonObject().put("result", result));
            }catch (Exception e ){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private boolean isValidDatesVehicleSchedule(JsonObject schedule, JsonObject vehicles) throws ParseException, InvalidRangeDateException {
        Date travelDate = UtilsDate.parseSdfDatabase(schedule.getString("travel_date"));
        Date arrivalDate = UtilsDate.parseSdfDatabase(schedule.getString("arrival_date"));
        RangeDate range = new RangeDate(travelDate, arrivalDate);
        Integer vehicleId = schedule.getInteger("vehicle_id");
        JsonArray vehicleDates = vehicles.getJsonArray(vehicleId.toString());
        if (vehicleDates == null) {
            vehicleDates = new JsonArray()
                    .add(new JsonObject()
                            .put("travel_date", UtilsDate.sdfDataBase(travelDate))
                            .put("arrival_date", UtilsDate.sdfDataBase(arrivalDate))
                    );
            vehicles.put(vehicleId.toString(), vehicleDates);
        } else {
            for (int i = 0, max = vehicleDates.size(); i < max; i++) {
                JsonObject vehicleDate = vehicleDates.getJsonObject(i);
                Date vehicleArrivalDate = UtilsDate.parseSdfDatabase(vehicleDate.getString("arrival_date"));
                Date vehicleTravelDate = UtilsDate.parseSdfDatabase(vehicleDate.getString("travel_date"));
                RangeDate vehicleRange = new RangeDate(vehicleTravelDate, vehicleArrivalDate);
                if (UtilsDate.rangeIntersects(range, vehicleRange)) {
                    return false;
                }
            }

            vehicleDates.add(new JsonObject()
                    .put("travel_date", UtilsDate.sdfDataBase(travelDate))
                    .put("arrival_date", UtilsDate.sdfDataBase(arrivalDate))
            );
            vehicles.put(vehicleId.toString(), vehicleDates);
        }

        return true;
    }

    private void registerScheduleDrivers(SQLConnection conn, Integer scheduleId, JsonArray drivers, Integer createdBy, Message<JsonObject> message, Handler<List<JsonObject>> handler) {
        try {
            List<String> inserts = new ArrayList<>();
            List<JsonObject> scheduleDrivers = new ArrayList<>();
            for (int i = 0, max = drivers.size(); i < max; i++) {
                JsonObject driver = drivers.getJsonObject(i);
                driver.put("schedule_route_id", scheduleId);
                driver.put("created_by", createdBy);
                driver.remove("time_checkpoint");
                inserts.add(this.generateGenericCreate("schedule_route_driver", driver));
            }
            conn.batch(inserts, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    handler.handle(scheduleDrivers);
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
    }

    private CompletableFuture<JsonObject> registerOneSchedule(SQLConnection conn, JsonObject schedule, Integer createdBy, Message<JsonObject> message) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String travelDate = schedule.getString("travel_date");
            String arrivalDate = schedule.getString("arrival_date");
            Integer vehicleId = schedule.getInteger("vehicle_id");
            Integer configScheduleId = schedule.getInteger("config_schedule_id");
            JsonArray drivers = schedule.getJsonArray("drivers");
            if (drivers == null || drivers.isEmpty()) {
                future.completeExceptionally(new Throwable("drivers: missing required value"));
                return future;
            }
            createScheduleRoute(conn, vehicleId, travelDate, arrivalDate, configScheduleId, createdBy, message, scheduleRoute -> {
                Integer configRouteId = scheduleRoute.getInteger("config_route_id");
                Integer scheduleRouteId = scheduleRoute.getInteger("id");
                getConfigDestinations(conn, configRouteId, message, destinations -> {
                    try {
                        JsonObject checkpoints = new JsonObject();
                        for (JsonObject destination : destinations) {
                            Integer terminalOrigin = destination.getInteger("terminal_origin_id");
                            Integer terminalDestiny = destination.getInteger("terminal_destiny_id");
                            Optional<JsonObject> stop = drivers.stream()
                                    .map(val ->(JsonObject) val)
                                    .filter(p -> p.getInteger("terminal_origin_id").equals(terminalOrigin)
                                            && p.getInteger("terminal_destiny_id").equals(terminalDestiny))
                                    .findFirst();
                            if(stop.isPresent()) {
                                Integer checkpointOrigin = stop.get().getInteger("time_checkpoint");
                                destination.put("origin_time_checkpoint", checkpointOrigin);
                                checkpoints.put(destination.getInteger("order_origin").toString(), checkpointOrigin);
                            }
                        }
                        createScheduleRouteDestinations(conn, scheduleRoute, travelDate, arrivalDate, destinations, checkpoints, message, routeDestinations -> {
                            schedule.put("id", scheduleRouteId);
                            registerScheduleDrivers(conn, scheduleRouteId, drivers, createdBy, message, scheduleDrivers -> future.complete(scheduleRoute));
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private void countSchedules(SQLConnection conn, Integer vehicleId, String travelDate, String arrivalDate, Message<JsonObject> message, Handler<JsonObject> handler) {
        try {
            JsonArray params = new JsonArray().add(vehicleId)
                    .add(travelDate).add(arrivalDate)
                    .add(travelDate).add(arrivalDate);
            conn.queryWithParams(QUERY_SCHEDULE_VEHICLE_BETWEEN_DATES, params, betweenReply -> {
                try {
                    if (betweenReply.failed()){
                        throw betweenReply.cause();
                    }
                    JsonObject result = betweenReply.result().getRows().get(0);
                    Integer count = result.getInteger("schedules");
                    handler.handle(result);
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
    }

    private void createScheduleRoute(SQLConnection conn, Integer vehicleId, String travelDate, String arrivalDate, Integer configScheduleId, Integer createdBy, Message<JsonObject> message, Handler<JsonObject> handler) {
        try {
            conn.queryWithParams(QUERY_GET_CONFIG_ROUTE, new JsonArray().add(configScheduleId), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Config schedule not found");
                    }

                    JsonObject configRoute = results.get(0);
                    Integer configRouteId = configRoute.getInteger(ID);

                    this.getScheduleRouteCode(conn, travelDate, configScheduleId, configRouteId).whenComplete((scheduleRouteCode, errorSRC) -> {
                       try {
                           if (errorSRC != null){
                               throw errorSRC;
                           }

                           JsonObject body = new JsonObject()
                                   .put(CODE, scheduleRouteCode)
                                   .put("vehicle_id", vehicleId)
                                   .put("travel_date", travelDate)
                                   .put("arrival_date", arrivalDate)
                                   .put("config_route_id", configRouteId)
                                   .put("config_schedule_id", configScheduleId)
                                   .put("schedule_status", "scheduled")
                                   .put("created_by", createdBy);
                           GenericQuery gc = this.generateGenericCreate(body);
                           conn.updateWithParams(gc.getQuery(), gc.getParams(), insertReply -> {
                               try {
                                   if (insertReply.failed()){
                                       throw insertReply.cause();
                                   }
                                   final int id = insertReply.result().getKeys().getInteger(0);
                                   body.put("id", id);

                                   JsonObject scheduleRouteLog = new JsonObject()
                                           .put(BoardingPassDBV.SCHEDULE_ROUTE_ID, id)
                                           .put(VEHICLE_ID, vehicleId)
                                           .put(ACTION, "assigned")
                                           .put(CREATED_BY, createdBy);
                                   GenericQuery registerScheduleRouteLog = this.generateGenericCreateSendTableName("schedule_route_vehicle_tracking", scheduleRouteLog);
                                   conn.updateWithParams(registerScheduleRouteLog.getQuery(), registerScheduleRouteLog.getParams(), replyLog -> {
                                       try {
                                           if (replyLog.failed()){
                                               throw replyLog.cause();
                                           }

                                           handler.handle(body);

                                       } catch (Throwable t){
                                           t.printStackTrace();
                                           this.rollback(conn, t, message);
                                       }
                                   });

                               } catch (Throwable t){
                                   t.printStackTrace();
                                   this.rollback(conn, t, message);
                               }
                           });

                       } catch (Throwable t){
                           t.printStackTrace();
                           this.rollback(conn, t, message);
                       }
                    });

                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
    }

    private CompletableFuture<String> getScheduleRouteCode(SQLConnection conn, String travelDate, Integer configScheduleId, Integer configRouteId){
        CompletableFuture<String> future = new CompletableFuture<>();

        try {

            this.validateConfigScheduleOrder(conn, configScheduleId, travelDate, configRouteId).whenComplete((newOrderRoute, errorSOR) -> {
               try {
                   if (errorSOR != null){
                       throw errorSOR;
                   }

                   String travelDateFormat = UtilsDate.format_DD_MM_YYYY(UtilsDate.parseSdfDatabase(travelDate)).replace("/", "");
                   String code = String.format("%03d", configRouteId) + "-" + travelDateFormat + "-" + newOrderRoute;

                   future.complete(code);

               } catch (Throwable t){
                   future.completeExceptionally(t);
               }
            });

        } catch (Throwable t){
            future.completeExceptionally(t);
        }

        return future;
    }

    private CompletableFuture<Integer> validateConfigScheduleOrder(SQLConnection conn, Integer configScheduleId, String travelDate, Integer configRouteId){
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();

        this.dbClient.queryWithParams(QUERY_GET_CONFIG_SCHEDULE_MAJOR_TRAVEL_HOUR, new JsonArray()
                .add(configScheduleId)
                .add(travelDate)
                .add(configRouteId), f1.completer());

        this.dbClient.queryWithParams(QUERY_GET_QUANTITY_ROUTES_BY_TRAVEL_DATE_AND_CONFIG_ROUTE_ID, new JsonArray()
                .add(travelDate)
                .add(configRouteId), f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> resultsMajorTravelHour = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> resultQuantity = reply.result().<ResultSet>resultAt(1).getRows();

                int count = resultQuantity.get(0).getInteger("quantity");
                int newOrderRoute = 1;

                List<String> updateList = new ArrayList<>();

                if (!resultsMajorTravelHour.isEmpty()){
                    for (int i = 0; i < resultsMajorTravelHour.size(); i++) {
                        JsonObject res = resultsMajorTravelHour.get(i);
                        if (i == resultsMajorTravelHour.size() - 1) {
                            String code = res.getString(CODE);
                            newOrderRoute = Integer.parseInt(code.substring(code.length() - 1)) - 1;
                        }
                        res.remove(ORDER_ROUTE);
                        updateList.add(this.generateGenericUpdateString(this.getTableName(), res));
                    }
                } else if (count != 0){
                    newOrderRoute = count + 1;
                }

                Integer finalNewOrderRoute = newOrderRoute;
                conn.batch(updateList, replyBatch -> {
                    try {
                        if (replyBatch.failed()){
                            throw replyBatch.cause();
                        }

                        future.complete(finalNewOrderRoute);

                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });


            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void createScheduleRouteDestinations(SQLConnection conn, JsonObject scheduleRoute, String travelDate, String arrivalDate, List<JsonObject> destinations, JsonObject checkPointOrigins, Message<JsonObject> message, Handler<List<JsonObject>> handler) throws ParseException {
        try {
            List<String> inserts = new ArrayList<>();
            Date arrivalDateValue = UtilsDate.parseSdfDatabase(arrivalDate);
            List<JsonObject> scheduleDestinations = new ArrayList<>();
            JsonObject travelDateOrigins = new JsonObject();
            travelDateOrigins.put("1", travelDate);
            JsonObject baseScheduleDestination = new JsonObject()
                    .put("destination_status", "scheduled")
                    .put("schedule_route_id", scheduleRoute.getInteger("id"))
                    .put("travel_date", travelDate)
                    .put("created_by", scheduleRoute.getInteger("created_by"));
            for (JsonObject destination : destinations) {
                JsonObject scheduleDestination = baseScheduleDestination.copy();
                Integer orderOrigin = destination.getInteger("order_origin");
                Integer orderDestiny = destination.getInteger("order_destiny");
                if (orderOrigin == null || orderDestiny == null) {
                    throw new Exception("Destination: Bad config, missing order properties");
                }
                String travelDateOrigin = travelDateOrigins.getString(orderOrigin.toString());
                Date dateTravel = UtilsDate.parseSdfDatabase(travelDateOrigin);

                GregorianCalendar travelCalendar = new GregorianCalendar();
                Integer[] travelTime = getHoursMinutes(destination.getString("travel_time"));
                travelCalendar.setTime(dateTravel);
                travelCalendar.add(Calendar.HOUR, travelTime[0]);
                travelCalendar.add(Calendar.MINUTE, travelTime[1]);

                Integer valCheckPoints = accCheckPoints(orderOrigin, orderDestiny, checkPointOrigins);
                travelCalendar.add(Calendar.MINUTE, valCheckPoints);
                // TODO: Add timezone difference

                Date destinationArrivalDate = travelCalendar.getTime();

                if (UtilsDate.isGreaterThan(destinationArrivalDate, arrivalDateValue)) {
                    throw new Exception("Arrival date: Bad value, destinations arrival dates are greater than schedule arrival date " + orderOrigin + " -> " + orderDestiny + " " + UtilsDate.sdfDataBase(destinationArrivalDate) + " > " + UtilsDate.sdfDataBase(arrivalDateValue));
                }

                String arrivalDateDestiny = UtilsDate.sdfDataBase(travelCalendar.getTime());

                scheduleDestination
                        .put("travel_date", travelDateOrigin)
                        .put("arrival_date", arrivalDateDestiny)
                        .put("time_checkpoint", destination.getInteger("origin_time_checkpoint"))
                        .put("terminal_origin_id", destination.getInteger("terminal_origin_id"))
                        .put("terminal_destiny_id", destination.getInteger("terminal_destiny_id"))
                        .put("config_destination_id", destination.getInteger("id"));

                scheduleDestinations.add(scheduleDestination);

                // Calculate travel date for destiny
                String travelDateDestiny = travelDateOrigins.getString(orderDestiny.toString());
                if (travelDateDestiny == null) {
                    Date dateTravelDestiny = UtilsDate.parseSdfDatabase(arrivalDateDestiny);

                    Integer checkpoint = checkPointOrigins.getInteger(orderDestiny.toString());
                    if (checkpoint != null) {
                        travelCalendar.setTime(dateTravelDestiny);
                        travelCalendar.add(Calendar.MINUTE, checkpoint);
                    }

                    travelDateDestiny = UtilsDate.sdfDataBase(travelCalendar.getTime());
                    travelDateOrigins.put(orderDestiny.toString(), travelDateDestiny);
                }

                inserts.add(this.generateGenericCreate("schedule_route_destination", scheduleDestination));
            }
            conn.batch(inserts, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    handler.handle(scheduleDestinations);
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
    }

    private Integer[] getHoursMinutes(String time) {
        Integer[] values = new Integer[2];
        String[] travelTime = time.split(":");
        for (int i = 0; i < 2; i++) {
            values[i] = Integer.parseInt(travelTime[i]);
        }
        return values;
    }

    private Integer accCheckPoints(Integer orderOrigin, Integer orderDestiny, JsonObject checkpoints) {
        Integer sum = 0;
        Object[] keys = checkpoints.fieldNames().toArray();
        for (int i = 0, max = keys.length; i < max; i++) {
            Integer orderTerminal = Integer.valueOf((String) keys[i]);
            if (orderOrigin < orderTerminal && orderDestiny > orderTerminal) {
                Integer check = checkpoints.getInteger(orderTerminal.toString());
                sum += check != null ? check : 0;
            }
        }
        return sum;
    }

    private void getConfigDestinations(SQLConnection conn, Integer configRouteId, Message<JsonObject> message, Handler<List<JsonObject>> handler) {
        conn.queryWithParams(QUERY_GET_CONFIG_DESTINATION, new JsonArray().add(configRouteId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> destinations = reply.result().getRows();
                if (destinations.isEmpty()) {
                    throw new Exception("Config destinations not found");
                }
                destinations.sort(new Comparator<JsonObject>() {
                    @Override
                    public int compare(JsonObject a, JsonObject b) {
                        Integer aOrderOrigin = a.getInteger("order_origin");
                        Integer aOrderDestiny = a.getInteger("order_destiny");
                        Integer bOrderOrigin = b.getInteger("order_origin");
                        Integer bOrderDestiny = b.getInteger("order_destiny");
                        if (aOrderOrigin > bOrderOrigin) {
                            return 1;
                        } else if (aOrderOrigin == bOrderOrigin) {
                            if (aOrderDestiny > bOrderDestiny) {
                                return 1;
                            } else if (aOrderDestiny == bOrderDestiny) {
                                return 0;
                            } else {
                                return -1;
                            }
                        } else {
                            return -1;
                        }
                    }
                });
                handler.handle(destinations);
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private void changeDriver(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer scheduleRouteId = body.getInteger("schedule_route_id");
        Integer employeeId = body.getInteger("employee_id");
        Integer terminalOriginId = body.getInteger("terminal_origin_id");
        JsonArray params = new JsonArray();
        params.add(scheduleRouteId).add(terminalOriginId);
        this.dbClient.queryWithParams(QUERY_SCHEDULE_ROUTE_DRIVER_CHANGE, params, reply->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("Schedule route driver not found");
                }
                Integer scheduleRouteDriverId = result.get(0).getInteger("id");
                JsonArray updateParams = new JsonArray().add(employeeId).add(scheduleRouteDriverId).add(scheduleRouteId);
                this.dbClient.updateWithParams(UPDATE_DRIVER_IN_SCHEDULE_ROUTE_DRIVER, updateParams, repply->{
                    try{
                        if(repply.failed()){
                            throw new Exception(repply.cause());
                        }
                        Integer resultDriver = repply.result().getUpdated();
                        if(resultDriver < 1){
                            throw new Exception("Schedule route driver id not found");
                        }
                        message.reply(new JsonObject());
                    }catch (Exception ex){
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private void changeSecondDriver(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer scheduleRouteId = body.getInteger("schedule_route_id");
        Integer employeeId = body.getInteger("employee_id");
        Integer terminalOriginId = body.getInteger("terminal_origin_id");
        JsonArray params = new JsonArray();
        params.add(scheduleRouteId).add(terminalOriginId);
        this.dbClient.queryWithParams(QUERY_SCHEDULE_ROUTE_DRIVER_CHANGE, params, reply->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("Schedule route driver not found");
                }
                Integer scheduleRouteDriverId = result.get(0).getInteger("id");
                JsonArray updateParams = new JsonArray().add(employeeId).add(scheduleRouteDriverId).add(scheduleRouteId);
                this.dbClient.updateWithParams(UPDATE_SECOND_DRIVER_IN_SCHEDULE_ROUTE_DRIVER, updateParams, repply->{
                    try{
                        if(repply.failed()){
                            throw new Exception(repply.cause());
                        }
                        Integer resultDriver = repply.result().getUpdated();
                        if(resultDriver < 1){
                            throw new Exception("Schedule route driver id not found");
                        }
                        message.reply(new JsonObject());
                    }catch (Exception ex){
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private void getConfigRouteStatus(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer configScheduleId = body.getInteger("config_schedule_id");
        String travelDate = body.getString("travel_date");
        JsonArray params = new JsonArray().add(configScheduleId).add(travelDate).add(travelDate).add(travelDate)
                                        .add(configScheduleId).add(travelDate).add(travelDate).add(travelDate);

        this.dbClient.queryWithParams(QUERY_CONFIG_ROUTE_STATUS, params, reply ->{
           try{
               if(reply.failed()){
                   throw new Exception(reply.cause());
               }
               List<JsonObject> result = reply.result().getRows();
               message.reply(new JsonObject().put("schedule", !result.isEmpty()));
           } catch (Exception e){
               e.printStackTrace();
               reportQueryError(message, e);
           }
        });
    }

    private void scheduleRouteList(Message<JsonObject> message) {
        // TODO: Paginate query
        JsonArray params = new JsonArray();
        this.dbClient.queryWithParams(QUERY_SCHEDULE_ROUTES_LIST, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> schedules = reply.result().getRows();
                int length = schedules.size();
                List<CompletableFuture<List<JsonObject>>> tasks = new ArrayList<CompletableFuture<List<JsonObject>>>();
                for (int i = 0; i < length; i++) {
                    JsonObject schedule = schedules.get(i);
                    tasks.add(getDrivers(schedule));
                }
                CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[length]));
                all.whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        message.reply(new JsonArray(schedules));
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void templateDetail(Message<JsonObject> message){
        JsonObject body = message.body();
        String templateName = body.getString("template_name");
        this.dbClient.queryWithParams(QUERY_GET_SCHEDULE_ROUTE_TEMPLATE, new JsonArray().add(templateName), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                for(JsonObject res : result){
                    pTasks.add(getSchedulesRoutesDetail(res));
                }
                CompletableFuture<Void> allSchedules = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pTasks.size()]));
                allSchedules.whenComplete((resultt, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        message.reply(new JsonObject().put("result" , result));
                    } catch (Throwable t){
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private CompletableFuture<JsonObject> getSchedulesRoutesDetail(JsonObject scheduleRoute){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            Integer scheduleRouteId = scheduleRoute.getInteger("id");
            Integer configRouteId = scheduleRoute.getInteger("config_route_id");
            JsonArray params = new JsonArray().add(scheduleRouteId).add(configRouteId);
            this.dbClient.queryWithParams(QUERY_GET_STOPS_SCHEDULES, params, reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> drivers = reply.result().getRows();
                    scheduleRoute.put("drivers", drivers);
                    future.complete(new JsonObject().put("drivers", drivers));
                }catch (Exception ex){
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }
    private void templateList(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer configRouteId = body.getInteger("config_route_id");
        Integer configScheduleId = body.getInteger("config_schedule_id");

        JsonArray params = new JsonArray().add(configRouteId).add(configScheduleId);

        this.dbClient.queryWithParams(QUERY_GET_TEMPLATE_LIST, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                message.reply(new JsonObject().put("result" , result));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private void driverScheduleRoutes(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject employee = body.getJsonObject("driver");
            JsonArray params = new JsonArray().add(employee.getInteger("id")).add(body.getString("date"));

            this.dbClient.queryWithParams(QUERY_DRIVER_SCHEDULE_ROUTES + QUERY_DRIVER_SCHEDULE_ROUTES_ORDER, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> schedules = reply.result().getRows();
                    final int len = schedules.size();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        JsonObject schedule = schedules.get(i);
                        tasks.add(this.appendAvailableSeatsToSchedule(schedule));
                        tasks.add(this.getDetailsForRoutes(false, null, false, schedules, i, new JsonArray(), "driver", "ida"));
                    }
                    CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len]));
                    all.whenComplete((result, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonArray(schedules));
                        } catch (Throwable t){
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
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

    private void driverScheduleRoutesById(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject employee = body.getJsonObject("driver");
            String query = QUERY_DRIVER_SCHEDULE_ROUTES
                    .concat(" AND sd.id = ? ")
                    .concat(QUERY_DRIVER_SCHEDULE_ROUTES_ORDER);
            JsonArray params = new JsonArray()
                    .add(employee.getInteger("id"))
                    .add(body.getString("date"))
                    .add(body.getInteger("scheduleRouteDestinationId"));

            System.out.println(query);
            System.out.println(params.encodePrettily());
            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> schedules = reply.result().getRows();
                    final int len = schedules.size();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        JsonObject schedule = schedules.get(i);
                        tasks.add(this.appendAvailableSeatsToSchedule(schedule));
                        tasks.add(this.getDetailsForRoutes(false, null, false, schedules, i, new JsonArray(), "driver", "ida"));
                    }
                    CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len]));
                    all.whenComplete((result, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonArray(schedules));
                        } catch (Throwable t){
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
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

    private CompletableFuture<JsonObject> appendAvailableSeatsToSchedule(JsonObject schedule) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer scheduleRouteDestinationId = schedule.getInteger("schedule_route_destination_id");
            this.availableSeatsByDestination(scheduleRouteDestinationId, new JsonArray())
                    .whenComplete((JsonObject data, Throwable error) -> {
                        try {
                            if (error != null) {
                                throw error;
                            }
                            schedule.mergeIn(data);
                            future.complete(data);
                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }


    private void driverScheduleRouteAvailableSeats(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject employee = body.getJsonObject("driver");
            Integer terminalOriginId = body.getInteger("terminal_origin_id");
            Integer scheduleRouteId = body.getInteger("schedule_route_id");
            JsonArray params = new JsonArray()
                    .add(terminalOriginId)
                    .add(scheduleRouteId);
            String query ;
            if(employee==null){
                query = QUERY_SCHEDULE_ROUTE_AVAILABLE_SEATS;
            }else{
                query = QUERY_DRIVER_SCHEDULE_ROUTE_AVAILABLE_SEATS;
                params.add(employee.getInteger("id"));
            }
            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> schedules = reply.result().getRows();
                    if (schedules.isEmpty()) {
                        throw new Exception("Schedule route not found");
                    }
                    JsonObject schedule = schedules.get(0);
                    this.appendAvailableSeatsToSchedule(schedule)
                            .whenComplete((JsonObject data, Throwable error) -> {
                                try {
                                    if (error != null) {
                                        throw error;
                                    }
                                    message.reply(schedule.mergeIn(data));
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    reportQueryError(message, t);
                                }
                            });
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

    // DRIVER START ROUTE METHODS <!- START -!>
    private void driverStartScheduleRoute(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                JsonObject driver = body.getJsonObject("driver");
                Integer userID = driver.getInteger("user_id");
                Integer scheduleRouteID = body.getInteger("schedule_route_id");

                driverDoStartScheduleRoute(conn, scheduleRouteID, userID).whenComplete((JsonObject schedule, Throwable schError) -> {
                    try {
                        if (schError != null) {
                            throw schError;
                        }
                        Integer terminalOriginID = schedule.getInteger("terminal_origin_id");
                        driverDoStartScheduleRouteDestinations(conn, terminalOriginID, schedule, userID)
                                .whenComplete((List<JsonObject> destinations, Throwable destError) -> {
                                    try {
                                        if (destError != null) {
                                            throw destError;
                                        }
                                        this.commit(conn, message, destinations.get(0));
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> driverDoStartScheduleRoute(SQLConnection conn, Integer scheduleRouteID, Integer userID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String today = UtilsDate.sdfDataBase(new Date());

            driverGetScheduleRouteLoading(conn, scheduleRouteID).whenComplete((JsonObject schedule, Throwable error) -> {
                try {
                    if (error != null){
                        throw error;
                    }
                    JsonObject data = new JsonObject()
                            .put("id", schedule.getInteger("id"))
                            .put("schedule_status", "in-transit")
                            .put("started_at", today)
                            .put("updated_at", today)
                            .put("updated_by", userID);

                    GenericQuery update = this.generateGenericUpdate("schedule_route", data);
                    conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> reply) -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            schedule.mergeIn(data);
                            future.complete(schedule);
                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }

        return future;
    }

    private CompletableFuture<JsonObject> driverGetScheduleRouteLoading(SQLConnection conn, Integer scheduleRouteID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            final String scheduleStatusLoading = "loading";
            JsonArray params = new JsonArray()
                    .add(scheduleRouteID)
                    .add(scheduleStatusLoading);
            conn.queryWithParams(QUERY_SCHEDULE_ROUTE_WITH_TERMINALS, params, (AsyncResult<ResultSet> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    ResultSet result = reply.result();
                    if (result.getNumRows() == 0) {
                        throw new Exception("Schedule route: Not found");
                    }
                    JsonObject schedule = result.getRows().get(0);
                    Integer scheduleStatus = schedule.getInteger("schedule_status");
                    if (!scheduleStatusLoading.equals(scheduleStatus)) {
                        throw new Exception("Schedule route: Status must be loading");
                    }
                    future.complete(schedule);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    // DRIVER START ROUTE METHODS <!- END -!>

    // DRIVER START ROUTE DESTINATION METHODS <!- START -!>
    private void driverStartScheduleRouteDestinations(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            this.rollback(conn, new Throwable("No implemented"), message);
        });
    }

    private CompletableFuture<List<JsonObject>> driverDoStartScheduleRouteDestinations(SQLConnection conn, Integer terminalOriginID, JsonObject scheduleInTransit, Integer userID) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String today = UtilsDate.sdfDataBase(new Date());
            Integer scheduleRouteID = scheduleInTransit.getInteger("id");

            driverGetNextDestinations(conn, scheduleRouteID, terminalOriginID).whenComplete((List<JsonObject> destinations, Throwable error) -> {
                try {
                    if (error != null){
                        throw error;
                    }
                    JsonObject data = new JsonObject()
                            .put("destination_status", "in-transit")
                            .put("started_at", today)
                            .put("updated_at", today)
                            .put("updated_by", userID);

                    List<String> updates = new ArrayList<>();
                    for (JsonObject destination: destinations) {
                        JsonObject update = data.copy().put("id", destination.getInteger("id"));
                        updates.add(this.generateGenericUpdateString("schedule_route_destination", update));
                        destination.mergeIn(update);
                    }

                    conn.batch(updates, reply -> {
                        try {
                          if (reply.failed()){
                              throw reply.cause();
                          }
                            future.complete(destinations);
                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> driverGetNextDestinations(SQLConnection conn, Integer scheduleRouteID, Integer terminalOriginID) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            final String scheduleStatusLoading = "loading";
            JsonArray params = new JsonArray()
                    .add(scheduleRouteID)
                    .add(terminalOriginID)
                    .add(scheduleStatusLoading);
            conn.queryWithParams(QUERY_SCHEDULE_ROUTE_DESTINATIONS, params, (AsyncResult<ResultSet> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    ResultSet result = reply.result();
                    if (result.getNumRows() == 0) {
                        throw new Exception("Schedule route: Destinations not found");
                    }
                    future.complete(result.getRows());
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }
    // DRIVER START ROUTE DESTINATION METHODS <!- END -!>

    private void searchByTerminals(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            params.add(body.getInteger(TERMINAL_ORIGIN_ID));
            params.add(body.getInteger(TERMINAL_DESTINY_ID));
            params.add(body.getString(DATE_TRAVEL));
            this.dbClient.queryWithParams(QUERY_SEARCH_BY_TERMINALS, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
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
    private void searchScheduleRouteDetail(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            Integer scheduleRouteID = body.getInteger("schedule_route_id");
            Integer terminalOriginId = body.getInteger("terminal_origin_id");
            Integer terminalDestinyId = body.getInteger("terminal_destiny_id");
            JsonArray searchParams = new JsonArray().add(scheduleRouteID).add(terminalOriginId).add(terminalDestinyId);
            this.dbClient.queryWithParams(SEARCH_SCHEDULE_ROUTE_STATUS, searchParams, replySr ->{
                try{
                    if(replySr.failed()){
                        throw new Exception(replySr.cause());
                    }
                    List<JsonObject> searchResult = replySr.result().getRows();
                    if(searchResult.isEmpty()){
                        throw new Exception("Schedule route not found");
                    }
                    JsonObject resultScheduleRouteDestination = searchResult.get(0);
                    String searchScheduleRouteStatus = resultScheduleRouteDestination.getString("schedule_status");
                    if(!searchScheduleRouteStatus.equals("scheduled")){
                        throw new Exception("Schedule route status is different from scheduled");
                    }
                    Integer scheduleRouteDestinationID = resultScheduleRouteDestination.getInteger("id");
                    message.reply(new JsonObject().put("schedule_route_destination_id", scheduleRouteDestinationID));
                } catch (Exception ex){
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        }catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getRoutesByOrigin(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray queryParams = new JsonArray();

            String travelDateStart = body.getString("travel_date");
            Integer minutesPrevious = body.getInteger("minutes");
            Date dateTravelDateTime = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(travelDateStart, "SDF");
            Date dateTravelDateTimeLess30Minutes = new Date(dateTravelDateTime.getTime() - (1000 * 60 * minutesPrevious));

            queryParams.add(UtilsDate.sdfDataBase(dateTravelDateTimeLess30Minutes));

            String travelDateEnd = UtilsDate.sdfDataBase(UtilsDate.getEndOfDay(UtilsDate.parseSdfDatabase(travelDateStart)));

            queryParams.add(travelDateEnd);

            queryParams.add(body.getInteger("terminal_origin"));

            this.dbClient.queryWithParams(QUERY_AVAILABLE_ORIGIN_ROUTES, queryParams, reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> results = reply.result().getRows();
                    final int max = results.size();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<CompletableFuture<JsonObject>>();
                    for (int i = 0; i < max; i++) {
                        tasks.add(getDetailsForRoutes(false, null, false, results, i, new JsonArray(), "", "ida"));
                    }
                    CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[max]));
                    all.whenComplete((s, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonArray(results));
                        } catch(Throwable t) {
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
                } catch(Exception ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch(Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void getRoutesByTerminals(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            Boolean flagPromo = body.getBoolean(FLAG_PROMO);
            JsonObject promo = body.getJsonObject(PROMO_DISCOUNT);
            String travelDateTime;
            String travelDate = body.getString(DATE_TRAVEL);
            String travelHour = body.getString(HOUR_TRAVEL);
            String origenRequest = body.getString("origen_request");
            Boolean isReturn = body.getBoolean("is_return", false);
            JsonArray boardingPasses = body.getJsonArray("stl");
            String ticketTypeRoute = body.getString(Constants.TICKET_TYPE_ROUTE, "ida");

            String QUERY = QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE;
            if (travelHour != null) {
                travelDateTime = travelDate.concat(" ").concat(travelHour);
            } else {
                travelDateTime = travelDate.concat(" 00:00:00");
            }

            if (origenRequest != null ){
                if(origenRequest.toUpperCase().equals("SITE") || origenRequest.toUpperCase().equals("APPCLIENT")  || origenRequest.toUpperCase().equals("POS")){
                    QUERY = QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE_ORIGEN+" AND sr.config_route_id!= all (select css.config_route_id from config_schedule as css where css.terminal_origin_id=9);";
                }else{
                    QUERY = QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE_ORIGEN;
                }


            }
            Integer minutesPrevious = body.getInteger("minutes");
            /*
            if(origenRequest.equals("pos")){
                minutesPrevious=60;
            }
             */
            Date dateTravelDateTime = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(travelDateTime, "SDF");
            Date dateTravelDateTimeLess30Minutes = new Date(dateTravelDateTime.getTime() - (1000 * 60 * minutesPrevious));
            params.add(UtilsDate.sdfDataBase(dateTravelDateTimeLess30Minutes));
            params.add(travelDate.concat(" 23:59:59"));
            params.add(body.getInteger(TERMINAL_ORIGIN_ID));
            params.add(body.getInteger(TERMINAL_DESTINY_ID));
            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    final int max = results.size();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                    for (int i = 0; i < max; i++) {
                        tasks.add(getVehicleCharacteristics(flagPromo, promo, isReturn,results, i, boardingPasses, origenRequest, ticketTypeRoute));
                        tasks.add(getArrivalDate(results.get(i)));
                    }

                    CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[max]));
                    all.whenComplete((s, error) -> {
                        try {
                            if (error != null) {
                                throw error;
                            }
                            message.reply(new JsonArray(results));
                        } catch (Throwable t){
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch(Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }


    private CompletableFuture<JsonObject> getVehicleCharacteristics(Boolean flagPromo, JsonObject promo, Boolean isReturn, List<JsonObject> routes, Integer index, JsonArray passes, String origenRequest, String ticketTypeRoute){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray();
            JsonObject route = routes.get(index);
            params.add(route.getInteger("vehicle_id"));

            this.dbClient.queryWithParams(QUERY_VEHICLE_CHARACTERISTICS, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> characteristics = reply.result().getRows();
                    route.put("characteristics", characteristics);
                    getDetailsForRoutes(flagPromo, promo, isReturn, routes, index, passes, origenRequest, ticketTypeRoute)
                            .whenComplete((stList, stThrow) -> {
                                try {
                                    if (stThrow != null) {
                                        throw stThrow;
                                    }
                                    future.complete(route);
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    future.completeExceptionally(t);
                                }
                            });
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getDetailsForRoutes(Boolean flagPromo, JsonObject promo, Boolean isReturn, List<JsonObject> routes, Integer index, JsonArray passes, String origenRequest, String ticketTypeRoute) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray();
            JsonObject route = routes.get(index);
            params.add(route.getInteger("schedule_route_id"));
            params.add(route.getInteger("config_route_id"));
            params.add(route.getInteger("order_origin"));
            params.add(route.getInteger("order_destiny"));
            this.dbClient.queryWithParams(QUERY_STOPS_BY_ROUTE, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> stops = reply.result().getRows();
                    if(route.containsKey("schedule_travel_date")) {
                        DateTime arrivalDate = DateTime.parse(route.getString("schedule_travel_date"));
                        for (JsonObject stop : stops) {
                            String[] travelTime = stop.getString("stop_travel_time").split(":");
                            DateTime arrival = arrivalDate.plusHours(Integer.parseInt(travelTime[0])).plusMinutes(Integer.parseInt(travelTime[1]));
                            DateTime departure = arrival.plusMinutes(stop.getInteger("terminal_origin_time_checkpoint"));
                            stop.put("departure", departure.toString())
                                    .put("arrival", arrival.toString());
                            arrivalDate = departure;
                        }
                    }
                    route.put("stops", stops);
                    JsonArray pricesParams = new JsonArray();
                    pricesParams.add(route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID));
                    //pricesParams.add(origenRequest);
                    this.dbClient.queryWithParams(QUERY_PRICES_BY_ROUTE_V2, pricesParams, replyPrices -> {
                        try {
                            if (replyPrices.failed()){
                                throw replyPrices.cause();
                            }
                            List<JsonObject> pricesParam = replyPrices.result().getRows().stream().map(p -> p.put(Constants.TICKET_TYPE_ROUTE, ticketTypeRoute)).collect(Collectors.toList());
                            vertx.eventBus().send(TicketPricesRulesDBV.class.getSimpleName(), new JsonObject().put("prices", pricesParam), new DeliveryOptions().addHeader(ACTION, ACTION_BATCH_APPLY_TICKET_PRICE_RULE), replyC -> {
                                try {
                                    if (replyC.failed()) {
                                        throw replyC.cause();
                                    }

                                    JsonObject pricesResult = (JsonObject) replyC.result().body();
                                    List<JsonObject> prices = pricesResult.getJsonArray("prices").getList();
                                    route.put("prices", pricesResult.getJsonArray("prices"));

                                    JsonObject service = new JsonObject()
                                            .put(ID, 1)
                                            .put(AMOUNT, 1)
                                            .put(Constants.DISCOUNT, 1);

                                    JsonArray products = new JsonArray();
                                    prices.forEach(p -> products.add(p.copy().put("ticket_type_route", "ida")));
                                    if (isReturn) {
                                        prices.forEach(p -> products.add(p.copy().put("ticket_type_route", "regreso")));
                                    }

                                    JsonObject bodyPromo = new JsonObject()
                                            .put(FLAG_USER_PROMO, false)
                                            .put(PromosDBV.DISCOUNT, promo)
                                            .put(SERVICE, SERVICES.boardingpass)
                                            .put(BODY_SERVICE, service)
                                            .put(PRODUCTS, products)
                                            .put(OTHER_PRODUCTS, new JsonArray())
                                            .put(FLAG_PROMO, flagPromo);
                                    vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                        try {
                                            if(replyPromos.failed()) {
                                                throw replyPromos.cause();
                                            }
                                            JsonObject resultApplyDiscount = replyPromos.result().body();

                                            if(flagPromo){
                                                String discountCode = promo.getString(DISCOUNT_CODE);
                                                JsonArray productsDiscount = resultApplyDiscount.getJsonArray(PRODUCTS);
                                                JsonObject pricesDiscount = new JsonObject()
                                                        .put(DISCOUNT_CODE, discountCode)
                                                        .put("rule", promo.getString("rule"))
                                                        .put("prices_discount", productsDiscount);
                                                route.put("discount", pricesDiscount);
                                            }

                                            // Get available seatings by route destination
                                            availableSeatsByDestination(route.getInteger("schedule_route_destination_id"), passes)
                                                    .whenComplete((sr, st) -> {
                                                        if (st != null) {
                                                            route.put("available_seatings", 0);
                                                        } else {
                                                            route.put("available_seatings", sr.getInteger("available_seatings"));
                                                        }
                                                        // Get available discounts by special ticket
                                                        getDiscountsBySpecialTicketByRoute(route)
                                                                .whenComplete((stList, stThrow) -> {
                                                                    if (stThrow != null) {
                                                                        route.put("available_tickets", new JsonArray());
                                                                    } else {
                                                                        route.put("available_tickets", new JsonArray(stList));
                                                                    }
                                                                    future.complete(route);
                                                                });
                                                    });

                                        } catch (Throwable t){
                                            future.completeExceptionally(t);
                                        }
                                    });

                                } catch (Throwable t) {
                                    future.completeExceptionally(t);
                                }
                            });

                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getDiscountsBySpecialTicketByRoute(JsonObject route) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<List<JsonObject>>();
        try {
            Boolean discountTickets = route.getBoolean("discount_tickets");
            Integer scheduleRouteId = route.getInteger("schedule_route_id");
            String query = QUERY_GET_SPECIAL_TICKETS_AVAILABLE_BY_ROUTE;
            JsonArray params = new JsonArray().add(scheduleRouteId);
            if (discountTickets != null && discountTickets ) {
                if( (route.getInteger("terminal_origin_id").equals(6) || route.getInteger("terminal_destiny_id").equals(6))){
                    System.out.println("Promo desactivada para mexicali");
                }else{
                    Integer terminalOriginId = route.getInteger("terminal_origin_id");
                    query = QUERY_GET_SPECIAL_TICKETS_AVAIABLES_BY_ORIGIN;
                    params.add(terminalOriginId);
                }

            }

            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    future.complete(reply.result().getRows());
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private void summaryCost(Message<JsonObject> message) {
        JsonArray params = new JsonArray();
        JsonObject body = message.body();
        params.add(body.getInteger(TERMINAL_ORIGIN_ID));
        params.add(body.getInteger(TERMINAL_DESTINY_ID));
        params.add(body.getString(DATE_TRAVEL));
        this.dbClient.queryWithParams(QUERY_SUMMARY_COST, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    message.reply(null);
                } else {
                    message.reply(reply.result().getRows().get(0));
                }
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void searchStops(Message<JsonObject> message) {
        JsonArray params = new JsonArray();
        params.add(message.body().getInteger(CONFIG_ROUTE_ID));
        this.dbClient.queryWithParams(QUERY_SEARCH_STOPS, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(new JsonArray(reply.result().getRows()));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<List<JsonObject>> getDrivers(JsonObject schedule) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray();
        params.add(schedule.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_SCHEDULE_ROUTE_DRIVERS, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> drivers = reply.result().getRows();
                schedule.put("drivers", drivers);
                future.complete(drivers);
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getAvailableSeatsByDestination(Message<JsonObject> message) {
        JsonObject body = message.body();
        final int scheduleRouteDestinationID = body.getInteger("schedule_route_destination_id");
        availableSeatsByDestination(scheduleRouteDestinationID, new JsonArray())
            .whenComplete((sr, st) -> {
                try {
                    if (st != null){
                        throw st;
                    }
                    message.reply(sr);
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
    }

    private CompletableFuture<JsonObject> availableSeatsByDestination(Integer scheduleRouteDestinationID, JsonArray passes) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        final JsonArray params = new JsonArray().add(scheduleRouteDestinationID);

        int pasajes = 0;
        int size = passes.size();

        if(size > 0) {
            for (int i = 0; i < passes.size(); i++ ) {
                JsonObject passObject = passes.getJsonObject(i);
                int passNumber = passObject.getInteger("number");
                pasajes = pasajes + passNumber ;
            }
        }
        final int pasaje_final = pasajes;

        this.dbClient.queryWithParams(QUERY_GET_ORDERS_BY_DESTINATION, params, reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }

                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    throw new Exception("Schedule route destination not found");
                }

                JsonObject scheduleRouteDestination = results.get(0);
                Integer scheduleRouteId = scheduleRouteDestination.getInteger("schedule_route_id");
                Integer orderOrigin = scheduleRouteDestination.getInteger("order_origin");
                Integer orderDestiny = scheduleRouteDestination.getInteger("order_destiny");
                final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                        .add(orderOrigin).add(orderDestiny)
                        .add(orderDestiny).add(orderDestiny)
                        .add(orderOrigin).add(orderDestiny);

                this.dbClient.queryWithParams(QUERY_AVAILABLE_SEATING_BY_DESTINATION, paramsList, replyList -> {
                    try {
                        if (replyList.failed()) {
                            throw new Exception(replyList.cause());
                        }

                        List<JsonObject> resultsList = replyList.result().getRows();
                        if (resultsList.isEmpty()) {
                            throw new Exception("Results not found");
                        }
                        //Checamos si es a tijuana, borrar despues
                        JsonArray paramTj = new JsonArray().add(scheduleRouteDestinationID).add(scheduleRouteId);
                        this.dbClient.query("SELECT * FROM general_setting WHERE FIELD = 'max_seats_to_hide'", replyMax -> {
                            try{
                                if (replyMax.failed()) {
                                    throw new Exception(replyMax.cause());
                                }
                                JsonObject maxSeatObj = replyMax.result().getRows().get(0);
                                Integer seatNum = Integer.parseInt(maxSeatObj.getString("value"));

                                this.dbClient.queryWithParams(QUERY_DESTINO_TIJUANA, paramTj, replyTj -> {
                                    try {
                                        if (replyTj.failed()) {
                                            throw new Exception(replyTj.cause());
                                        }

                                        JsonObject resultsListTj = replyTj.result().getRows().get(0);
                                        Integer padreDestino = resultsListTj.getInteger("padre_terminal_destiny_id");
                                        String padreDestinyName = resultsListTj.getString("padre_terminal_destiny_name");
                                        Integer padreOrigen = resultsListTj.getInteger("padre_terminal_origin_id");
                                        String padreOrigenName = resultsListTj.getString("padre_terminal_origin_name");
                                        Integer hijoOrigen = resultsListTj.getInteger("hijo_terminal_origin_id");
                                        Integer hijoDestino = resultsListTj.getInteger("hijo_terminal_destiny_id");
                                        String hijoOrigenName = resultsListTj.getString("hijo_terminal_origin_name");
                                        String hijoDestinyName = resultsListTj.getString("hijo_terminal_destiny_name");
                                        //Suponiendo que Tijuana es ID 11
                                        if(padreDestino == 11 || padreDestinyName.equals("Tijuana")) {
                                            if(hijoDestino == 11 || (hijoOrigen == 4 && hijoDestino == 5 )) {
                                                future.complete(resultsList.get(0));
                                            } else {
                                                JsonObject obj = resultsList.get(0);
                                                Integer seatings = obj.getInteger("seatings");
                                                Integer available_seatings = obj.getInteger("available_seatings") - pasaje_final;
                                                //Integer ocupados = (seatings - 19) - pasaje_final;
                                                Integer ocupados = (seatings - available_seatings);
                                                Integer routeID = obj.getInteger("schedule_route_id");
                                                if((ocupados) <= seatNum) {
                                                    future.complete(resultsList.get(0));
                                                } else {
                                                    JsonObject newResult = new JsonObject().put("seatings", 42)
                                                            .put("available_seatings", 0)
                                                            .put("schedule_route_id", routeID);

                                                    future.complete(newResult);

                                                }

                                            }
                                        } else {
                                            future.complete(resultsList.get(0));
                                        }

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        future.completeExceptionally(ex);
                                    }
                                });
                                //future.complete(resultsList.get(0));
                        } catch (Exception ex) {
                                ex.printStackTrace();
                                future.completeExceptionally(ex);
                        }
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }

                });
            } catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private void searchSeatsGoodOne(Message<JsonObject> message) {
        final int scheduleRouteId = message.body().getInteger(SCHEDULE_ROUTE_ID);

        JsonArray params = new JsonArray().add(scheduleRouteId);

        Future<ResultSet> fQuerySearchSeats = Future.future();
        Future<ResultSet> fQueryUsedSpecialSeats = Future.future();
        Future<ResultSet> fQuerySpecialTicket = Future.future();

        this.dbClient.callWithParams(QUERY_SEARCH_SEATS, params, null, fQuerySearchSeats.completer());
        this.dbClient.callWithParams(QUERY_SEAR_USED_SEAT_BY_SPECIAL_TICKET, params, null, fQueryUsedSpecialSeats.completer());
        this.dbClient.queryWithParams(QUERY_SEARCH_SPECIAL_TICKETS_TOTAL_DISCOUNTS, params, fQuerySpecialTicket.completer());

        CompositeFuture.all(fQuerySearchSeats, fQueryUsedSpecialSeats, fQuerySpecialTicket).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                JsonObject response = new JsonObject();

                List<JsonObject> replyAvailableSeats = reply.result().<ResultSet>resultAt(0).getRows();
                JsonObject result = replyAvailableSeats.get(0);

                final Integer totalSeats = result.getInteger("total_seats");
                if (totalSeats == null) {
                    message.reply(new JsonObject()
                                    .put("name", "config_vehicle")
                                    .put("error", "no configuration set to vehicle"),
                            new DeliveryOptions()
                                    .addHeader(ErrorCodes.DB_ERROR.toString(),
                                            "no configuration set to vehicle"));
                    return;
                }
                final Integer usedBoardingPass = result.getInteger("used_boarding_pass");
                final Integer usedPreBoardingPass = result.getInteger("used_preboarding_pass");

                final int availableSeats = totalSeats - usedBoardingPass - usedPreBoardingPass;

                JsonArray specialTicketList = new JsonArray();
                List<JsonObject> usedSpecialTickets = reply.result().<ResultSet>resultAt(1).getRows();
                List<JsonObject> specialTickesTotals = reply.result().<ResultSet>resultAt(2).getRows();

                for (JsonObject specialTicket : specialTickesTotals) {
                    int specialTicketId = specialTicket.getInteger("special_ticket_id");
                    int totalDiscount = specialTicket.getInteger("total_discount");
                    int price = specialTicket.getInteger("total_amount");
                    for (JsonObject usedTikect : usedSpecialTickets) {
                        int usedSpecialTicketId = usedTikect.getInteger("special_ticket_id");
                        if (specialTicketId == usedSpecialTicketId) {
                            totalDiscount -= usedTikect.getInteger("used_seats");
                        }
                    }
                    specialTicketList.add(new JsonObject()
                            .put("special_ticket_id", specialTicketId)
                            .put("total", totalDiscount)
                            .put("price", price)
                    );
                }
                response.put("available_seats", availableSeats);
                response.put("seats_special_ticket", specialTicketList);
                message.reply(response);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getTerminalsByDestination(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer scheduleRouteDestinationId = body.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            JsonArray param = new JsonArray().add(scheduleRouteDestinationId);

            this.dbClient.queryWithParams("SELECT terminal_origin_id, terminal_destiny_id FROM schedule_route_destination WHERE id = ?;", param, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> terminals = reply.result().getRows();
                    if (terminals.isEmpty()){
                        throw new Exception("Destination not found");
                    }

                    message.reply(terminals.get(0));

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

    private void driverCalculateTicketPrice(Message<JsonObject> message){
        JsonObject body = message.body();
        try {
            String currentPosition = body.getString(CURRENT_POSITION);
            Integer scheduleRouteDestinationId = body.getInteger(BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID);

            this.getPositionsByRoute(scheduleRouteDestinationId).whenComplete((positions, errorPositions) -> {
                try {
                    if (errorPositions != null){
                        throw errorPositions;
                    }

                    String originPosition = positions.getString("origin_position");
                    String destinyPosition = positions.getString("destiny_position");

                    UtilsGoogleMaps googleMaps = new UtilsGoogleMaps(geoApiContext);
                    Integer remainingPercent = googleMaps.getRemainingPercentOfRoute(originPosition, destinyPosition, currentPosition);
                   this.getPricesByRoute(remainingPercent, scheduleRouteDestinationId).whenComplete((prices, errorPrices) -> {
                       try {
                           if (errorPrices != null){
                               throw errorPrices;
                           }

                           message.reply(new JsonObject()
                                   .put("prices", prices)
                                   .put("remaining_percent_of_route", remainingPercent));

                       } catch (Throwable t){
                           t.printStackTrace();
                           reportQueryError(message, t);
                       }
                   });
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

    /**
     * Get the position (latitude, longitude) of the origin and destination terminal of a route
     * @param scheduleRouteDestinationId
     * @return Object with origin_position and destiny_position attributes
     */
    private CompletableFuture<JsonObject> getPositionsByRoute(Integer scheduleRouteDestinationId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_POSITIONS_TERMINALS_BY_ROUTE, new JsonArray().add(scheduleRouteDestinationId), reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }
               List<JsonObject> results = reply.result().getRows();
               if (results.isEmpty()){
                   throw new Exception("Positions by route not found");
               }
               future.complete(results.get(0));
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    /**
     * Get the prices of each type of passenger calculating based on the remaining percentage of the route
     * @param scheduleRouteDestinationId
     * @return Price list by passenger type
     */
    private CompletableFuture<JsonArray> getPricesByRoute(Integer remainingPercent, Integer scheduleRouteDestinationId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PRICES_BY_REMAINING_PERCENT_OF_ROUTE, new JsonArray().add(remainingPercent).add(scheduleRouteDestinationId), reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }
               List<JsonObject> results = reply.result().getRows();
               if (results.isEmpty()){
                   throw new Exception("Prices by route not found");
               }
               future.complete(new JsonArray(results));
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private void getDriverAvailableSeat(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer quantitySeats = (Integer) body.remove("quantity");
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.SEATS_WITH_DESTINATION);
        vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options, reply -> {
            try{
                if(reply.failed()) {
                    throw reply.cause();
                }
                Message<Object> result = reply.result();
                JsonObject resultSeats = (JsonObject) result.body();
                JsonArray busySeats = resultSeats.getJsonArray("busy_seats");

                JsonArray availableSeats = this.loadAvailableSeats(resultSeats);

                JsonArray selectedSeat = new JsonArray();
                for(int i = 0; i < availableSeats.size() && selectedSeat.size() < quantitySeats; i++){
                    JsonObject seat = availableSeats.getJsonObject(i);
                    String seatName = seat.getString("seat_name");
                    if (busySeats.isEmpty()){
                        selectedSeat.add(new JsonObject().put("seat", seatName));
                    } else {
                        boolean flagAvailableSeat = true;
                        for(int j = 0; j < busySeats.size(); j++){
                            JsonObject busySeat = busySeats.getJsonObject(j);
                            String busySeatName = busySeat.getString("seat");
                            if (seatName.equals(busySeatName)){
                                flagAvailableSeat = false;
                                break;
                            }
                        }
                        if (flagAvailableSeat){
                            selectedSeat.add(new JsonObject().put("seat", seatName));
                        }
                    }
                }

                if (selectedSeat.isEmpty()){
                    throw new Exception("Seats not available");
                }

                message.reply(selectedSeat);

            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    public JsonArray loadAvailableSeats(JsonObject body){

        JsonArray busySeats = body.getJsonArray("busy_seats");
        JsonObject configVehicle = body.getJsonObject("config");
        JsonArray availableSeats = new JsonArray();

        Integer totalCol = configVehicle.getInteger("total_col")+1;
        Integer totalRow = configVehicle.getInteger("total_row")+1;
        Integer enumeration = Integer.parseInt(configVehicle.getString("enumeration"));
        String enumType = configVehicle.getString("enum_type");
        String notSeats = configVehicle.getString("not_seats");

        Integer div_col = Integer.parseInt(configVehicle.getString("division_by_col"))+1;

        for (int row = 1; row <= totalRow; row++) {
            for (int col = 1; col <= totalCol; col++) {
                JsonObject seat = new JsonObject();
                seat.put("position", (row+","+col));
                seat.put("seat_name", getSeatName(enumType, totalCol, col, row));
                seat.put("busy", getSeatState(busySeats, notSeats, seat));

                if(!seat.getBoolean("busy")){
                    availableSeats.add(seat);
                }
            }
        }
        return availableSeats;
    }

    private boolean getSeatState(JsonArray busySeats,String notSeats, JsonObject seat){
        String[] noSeats = notSeats.split("\\|");
        if(busySeats.contains(seat.getString("seat_name"))){
            return true;
        }else {
            for(int i=0; i<noSeats.length; i++){
                if(noSeats[i].equals(seat.getString("position"))){
                    return true;
                }
            }
            return false;
        }
    }

    private String getSeatName(String enumType, Integer total_col, Integer col, Integer row){
        String seatName = "";
        switch(enumType){
            case "0":
                seatName = String.valueOf(((row-1) * total_col) + col);
                break;
            case "1":
                String ch = Character.toString((char) (64+row));
                seatName = String.valueOf(ch + col);
                break;
            case "2":
                String chh = Character.toString((char) (64+col));
                seatName = String.valueOf(chh+row);
                break;
        }
        return seatName;
    }

    private void getScheduleRouteInfoByVehicleId(Message<JsonObject> message){
        try {
            JsonObject body = message.body();

            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer terminalOriginId = body.getInteger(BoardingPassDBV.TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = body.getInteger(BoardingPassDBV.TERMINAL_DESTINY_ID);
            Integer vehicleId = body.getInteger(VEHICLE_ID);

            String queryOptionals = "";
            JsonArray params = new JsonArray()
                    .add(initDate)
                    .add(endDate)
                    .add(vehicleId);

            if(terminalOriginId != null){
                queryOptionals = queryOptionals.concat("AND srd.terminal_origin_id = ?\n");
                params.add(terminalOriginId);
            }

            if(terminalDestinyId != null){
                queryOptionals = queryOptionals.concat("AND srd.terminal_destiny_id = ?");
                params.add(terminalDestinyId);
            }

            this.dbClient.queryWithParams(QUERY_GET_SCHEDULE_ROUTE_INFO_BY_VEHICLE_ID, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    message.reply(new JsonArray(reply.result().getRows()));

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

    private void getScheduleRouteInfoByDestinationId(Message<JsonObject> message){
        try {
            JsonObject body = message.body();

            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer terminalOriginId = body.getInteger(BoardingPassDBV.TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = body.getInteger(BoardingPassDBV.TERMINAL_DESTINY_ID);
            Integer vehicleId = body.getInteger(VEHICLE_ID);

            String queryOptionals = "";
            JsonArray params = new JsonArray()
                    .add(initDate)
                    .add(endDate);

            if(terminalOriginId != null){
                queryOptionals = queryOptionals.concat("AND srd.terminal_origin_id = ?\n");
                params.add(terminalOriginId);
            }

            if(terminalDestinyId != null){
                queryOptionals = queryOptionals.concat("AND srd.terminal_destiny_id = ?\n");
                params.add(terminalDestinyId);
            }

            if(vehicleId != null){
                queryOptionals = queryOptionals.concat("AND v.id = ?");
                params.add(vehicleId);
            }

            String query = QUERY_GET_SCHEDULE_ROUTE_INFO_BY_DESTINATION_ID.replace("{OPTIONAL_QUERY}", queryOptionals);
            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> finalResult = new ArrayList<>();
                    Map<String, List<JsonObject>> result = reply.result().getRows().stream().map(obj -> (JsonObject)obj).collect(Collectors.groupingBy(obj -> obj.getString("prefix")));

                    for (Map.Entry<String, List<JsonObject>> entry : result.entrySet()) {
                        JsonObject item = entry.getValue().get(0).copy();
                        item.remove("prefix");
                        JsonArray schedules_routes = new JsonArray(entry.getValue().stream()
                                    .map(obj -> (JsonObject)obj)
                                    .mapToInt(obj -> obj.getInteger("schedule_route_id"))
                                    .boxed().collect(Collectors.toList()));

                        item.put("schedule_route_id", schedules_routes);

                        finalResult.add(item);
                    }

                    finalResult.sort(new Comparator<JsonObject>() {
                        @Override
                        public int compare(JsonObject a, JsonObject b) {
                            String t1 = a.getString("time");
                            String t2 = b.getString("time");
                            return t1.compareTo(t2);
                        }
                    });

                    message.reply(new JsonArray(finalResult));

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

    private void changeVehicle(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            int scheduleRouteId = body.getInteger(BoardingPassDBV.SCHEDULE_ROUTE_ID);
            int vehicleId = body.getInteger(VEHICLE_ID);
            int updatedBy = (int) body.remove(UPDATED_BY);
            String updatedAt = UtilsDate.sdfDataBase(new Date());

            this.changeVehicleScheduleRouteValidation(scheduleRouteId, vehicleId).whenComplete((scheduleRoute, error) -> {
                try {
                   if (error != null){
                       throw error;
                   }

                   String scheduleTravelDate = (String) scheduleRoute.remove(TRAVEL_DATE);
                   String scheduleArrivalDate = (String) scheduleRoute.remove("arrival_date");

                   this.changeDriverScheduleRouteDatesValidation(scheduleTravelDate, scheduleArrivalDate, vehicleId).whenComplete((resultDV, errorDV) -> {
                       try {
                           if (errorDV != null){
                               throw errorDV;
                           }

                           List<String> batchList = new ArrayList<>();

                           batchList.add(this.generateGenericUpdateString(this.getTableName(),
                                   scheduleRoute
                                   .put(VEHICLE_ID, vehicleId)
                                   .put(UPDATED_BY, updatedBy)
                                   .put(UPDATED_AT, updatedAt)));

                           batchList.add(this.generateGenericInsertWithValues("schedule_route_vehicle_tracking",
                                   new JsonArray().add(new JsonObject()
                                           .put(BoardingPassDBV.SCHEDULE_ROUTE_ID, scheduleRouteId)
                                           .put(VEHICLE_ID, vehicleId)
                                           .put(ACTION, "change-vehicle")
                                           .put(CREATED_BY, updatedBy)
                                           .put(CREATED_AT, updatedAt))));

                           this.startTransaction(message, conn -> {
                               conn.batch(batchList, replyBatch -> {
                                   try {
                                       if(replyBatch.failed()){
                                           throw replyBatch.cause();
                                       }

                                       this.commit(conn, message, scheduleRoute);

                                   } catch (Throwable t){
                                       t.printStackTrace();
                                       this.rollback(conn, t, message);
                                   }
                               });
                           });

                       } catch (Throwable t){
                           t.printStackTrace();
                           reportQueryError(message, t);
                       }
                   });

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

    private CompletableFuture<JsonObject> changeVehicleScheduleRouteValidation(int scheduleRouteId, int vehicleId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.dbClient.queryWithParams("SELECT id, schedule_status, vehicle_id, travel_date, arrival_date FROM schedule_route WHERE id = ? AND status = 1;", new JsonArray().add(scheduleRouteId), replyInfo -> {
            try {
                if (replyInfo.failed()) {
                    throw replyInfo.cause();
                }

                List<JsonObject> resultScheduleRoute = replyInfo.result().getRows();
                if (resultScheduleRoute.isEmpty()) {
                    throw new Exception("Schedule route not found");
                }

                JsonObject scheduleRoute = resultScheduleRoute.get(0);
                String scheduleStatus = (String) scheduleRoute.remove("schedule_status");
                int scheduleVehicleId = (int) scheduleRoute.remove(VEHICLE_ID);

                if (scheduleStatus.equals("canceled") || scheduleStatus.equals("in-transit") || scheduleStatus.equals("finished-ok")) {
                    throw new Exception("Schedule status is canceled or in-transit or finished-ok");
                }

                if (vehicleId == scheduleVehicleId) {
                    throw new Exception("Vehicle is already assigned in this route");
                }

                future.complete(scheduleRoute);

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> changeDriverScheduleRouteDatesValidation(String travelDate, String arrivalDate, int vehicleId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        JsonArray paramsSR = new JsonArray()
                .add(travelDate).add(arrivalDate)
                .add(travelDate).add(arrivalDate)
                .add(vehicleId);

        this.dbClient.queryWithParams("SELECT COUNT(IF(sr.schedule_status NOT IN ('canceled', 'in-transit', 'finished-ok')\n" +
                "AND sr.travel_date BETWEEN ? AND ?\n" +
                "AND sr.arrival_date BETWEEN ? AND ?, 1, NULL)) AS count, v.status FROM vehicle v\n" +
                "LEFT JOIN schedule_route sr ON sr.vehicle_id = v.id\n" +
                "WHERE v.id = ?;", paramsSR, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();


                JsonObject scheduleRoute = result.get(0);
                int count = scheduleRoute.getInteger("count");

                if (scheduleRoute.getInteger(STATUS) == null) {
                    throw new Exception("Vehicle not found");
                }

                if (count > 0) {
                    throw new Exception("Vehicle not available in this date");
                }

                int vehicleStatus = scheduleRoute.getInteger(STATUS);
                if (vehicleStatus != 1) {
                    throw new Exception("Vehicle isn't active");
                }

                future.complete(true);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void statusRouteReport(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originBranchofficeId = body.getInteger("terminal_origin");
            Integer destinyBranchofficeId = body.getInteger("terminal_destiny");
            Integer routeId = body.getInteger("route_origin");
            boolean isCanceled = body.getBoolean("is_canceled", false);
            String QUERY = QUERY_GET_DEPARTURES_CALENDAR;

            JsonArray params = new JsonArray().add(initDate).add(endDate);

            if(originBranchofficeId != null || destinyBranchofficeId != null){
                QUERY = QUERY_GET_DEPARTURES_CALENDAR_BY_TERMINAL_V2;

                if(originBranchofficeId != null && destinyBranchofficeId != null){
                    QUERY = QUERY.concat(" AND srd.terminal_origin_id = ?  \n AND srd.terminal_destiny_id = ? \n");
                    params.add(originBranchofficeId);
                    params.add(destinyBranchofficeId);
                }
                if(originBranchofficeId != null && destinyBranchofficeId == null){
                    QUERY = QUERY.concat(" AND srd.terminal_origin_id = ? AND srd.terminal_destiny_id = cr.terminal_destiny_id");
                }
                if(destinyBranchofficeId != null && originBranchofficeId == null){
                    QUERY = QUERY.concat(" AND srd.terminal_destiny_id = ? AND srd.terminal_origin_id = cr.terminal_origin_id");
                    params.add(destinyBranchofficeId);
                }
            }

            if(routeId != null){
                QUERY = QUERY.concat(" AND sr.id = ? ");
                params.add(routeId);
            }

            QUERY = isCanceled ? QUERY.concat(" AND sr.schedule_status = 'canceled' ") : QUERY.concat(" AND sr.schedule_status != 'canceled'");

            QUERY = QUERY.concat(QUERY_FILTER_STATUS_DEPARTURES_CALENDAR);
            QUERY = QUERY.concat(" ORDER BY sr.travel_date ");
            this.getRouteInShipmentsList(QUERY, params , message).whenComplete( (result, error) -> {
                try{
                    if(error != null){
                        throw new Exception(error);
                    }

                    message.reply(result);
                } catch (Exception e){
                    e.printStackTrace();
                    reportQueryError(message , e);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getRouteInShipmentsList(String QUERY, JsonArray params , Message<JsonObject> message) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                if(result.isEmpty()){
                    throw new Exception("Schedules routes not found");
                }

                List<CompletableFuture<JsonObject>> routeTask = new ArrayList<>();

                result.forEach(route-> {
                    // routeTask.add(getShipmentByRoute(route));
                    routeTask.add(getShipmentByRoute(route));
                    routeTask.add(scheduleByRouteReport(route, message));
                });
                CompletableFuture.allOf(routeTask.toArray(new CompletableFuture[routeTask.size()])).whenComplete((ps, pt) -> {
                    try {
                        if (pt != null) {
                            reportQueryError(message, pt.getCause());
                        } else {
                            JsonObject res = new JsonObject().put("schedules_routes", result);
                            future.complete(res);
                        }
                    } catch (Exception e){
                        reportQueryError(message, e.getCause());
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getShipmentByRoute(JsonObject route ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray params = new JsonArray().add(route.getInteger("schedule_route_id"));

        this.dbClient.queryWithParams(QUERY_GET_SHIPMENT_BY_ROUTE_ID, params , handler -> {
            try{
                if (handler.succeeded()) {
                    List<JsonObject> resultsTracking = handler.result().getRows();
                    if (!resultsTracking.isEmpty()) {
                        route.put("shipments", resultsTracking);
                    } else {
                        route.put("shipments", new JsonArray());
                    }
                    future.complete(route);
                } else {
                    future.completeExceptionally(handler.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e.getCause());
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> scheduleByRouteReport(JsonObject route , Message<JsonObject> message ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject body = message.body();

        Integer originBranchofficeId = body.getInteger("terminal_origin");
        Integer destinyBranchofficeId = body.getInteger("terminal_destiny");

        Integer scheduleRouteId = route.getInteger("schedule_route_id");
        JsonArray params = new JsonArray().add(scheduleRouteId);

        this.dbClient.queryWithParams(QUERY_GET_SCHEDULE_ROUTE_DETAIL, params , handler -> {
            try{
                if (handler.succeeded()) {
                    List<JsonObject> result = handler.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Schedule route detail not found");
                    }
                    JsonObject configVehicle = result.get(0);
                    Integer configRouteId = configVehicle.getInteger("config_route_id");
                    JsonArray paramSchedule = new JsonArray().add(scheduleRouteId);

                    String QUERY_SCHEDULE =  "";

                    if(originBranchofficeId != null && destinyBranchofficeId != null){
                        QUERY_SCHEDULE = QUERY_SCHEDULE.concat(QUERY_GET_STOPS_BY_SCHEDULE_ROUTE_NO_DRIVER);
                        paramSchedule.add(route.getInteger("schedule_route_destination_id"));
                    } else {
                        paramSchedule.add(configRouteId);
                        QUERY_SCHEDULE =  QUERY_SCHEDULE.concat(QUERY_GET_STOPS_BY_SCHEDULE_ROUTE_v2);
                    }

                    QUERY_SCHEDULE = QUERY_SCHEDULE.concat("GROUP BY srdd.id");

                    this.dbClient.queryWithParams(QUERY_SCHEDULE, paramSchedule , repply->{
                        try{
                            if (repply.failed()){
                                throw new Exception(repply.cause());
                            }
                            List<JsonObject> resultStops = repply.result().getRows();

                            List<CompletableFuture<JsonObject>> tasks = new ArrayList<CompletableFuture<JsonObject>>();
                            for (int i = 0; i < resultStops.size(); i++) {
                                JsonObject stop = resultStops.get(i);
                                Integer orderOrigin = stop.getInteger("order_origin");
                                Integer orderDestiny = stop.getInteger("order_destiny");
                                tasks.add(getAvailableSeats(orderOrigin, orderDestiny , scheduleRouteId , resultStops, i));
                                //tasks.add(getShipmentByRoute(stop, stop.getInteger("terminal_origin_id")));
                            }

                            CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                            all.whenComplete((resultt, error) -> {
                                try {
                                    if (error != null){
                                        throw error;
                                    }
                                    route.put("schedule_route_detail",result.get(0)).put("config_vehicle", configVehicle).put("destinations" , resultStops);
                                    future.complete(route);
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    future.completeExceptionally(handler.cause());
                                }
                            });
                        } catch (Exception ex){
                            future.completeExceptionally(ex.getCause());
                        }
                    });
                } else {
                    future.completeExceptionally(handler.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e.getCause());
            }
        });
        return future;
    }

    private void changeHideStatus(Message<JsonObject> message){
        try{
            startTransaction(message,conn -> {
                JsonObject body = message.body().getJsonObject("search");
                Integer id = body.getInteger("id");
                try{

                    String query = generateGenericUpdateString("schedule_route", body );
                    conn.update(query, reply -> {
                        try {
                            if(reply.failed()){
                                throw reply.cause();
                            }
                            this.commit(conn, message, new JsonObject().put("success", true ).put( "ruta" ,body));

                        }catch (Throwable ex){
                            this.rollback(conn, ex, message);
                            ex.printStackTrace();

                        }
                    });

                }catch (Exception ex){
                    this.rollback(conn, ex , message);
                }
            });
        }catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message,t);
        }
    }

    private CompletableFuture<JsonObject> getArrivalDate(JsonObject schedule){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray();
            String origenTimezone = schedule.getString("origen_timezone");
            String destinyTimezone = schedule.getString("destiny_timezone");
            String departureDateString = schedule.getString("departure_date");
            String arrivalDateString = schedule.getString("arrival_date");
            String travelTime = schedule.getString("travel_time");
            //Sacamos la zona horaria del origen y el destino
            TimeZone tzOrigen = TimeZone.getTimeZone(origenTimezone);
            TimeZone tzDestiny = TimeZone.getTimeZone(destinyTimezone);
            // Sacamos el offset y la fecha de partida
            Date departureDate = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX")).parse(departureDateString);
            Date arrivalDateFormat = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX")).parse(arrivalDateString);
            Integer  offsetDeparture = tzOrigen.getOffset(departureDate.getTime()) / 1000 / 60;
            // Sacamos la fecha de arrivo
            String replacedTravelHour = travelTime.replaceAll(":.*", ".00");
            String replacedTravelMinutes = travelTime.replaceAll(".+?(?=:)", "").replace(":", "");
            Double hoursArrival = Double.parseDouble(replacedTravelHour);
            Duration baseDuration = Duration.ofHours((new Double(hoursArrival)).longValue());
            Duration minuteDuration = Duration.ofMinutes(Long.valueOf(replacedTravelMinutes));
            //Date arrivalDate = Date.from(departureDate.toInstant().plus(baseDuration).plus(minuteDuration));
            Date arrivalDate = Date.from(arrivalDateFormat.toInstant());
            // Sacamos el offset de la fecha de arrivo
            Integer offsetArrival = tzDestiny.getOffset(arrivalDateFormat.getTime()) / 1000 / 60;
            // Restamos o sumamos sea el caso
            Integer longoDeparture = offsetDeparture / 60;
            Integer longoArrival = offsetArrival / 60;
            Integer sumInt = Math.abs(longoDeparture - longoArrival);

            if(longoDeparture < longoArrival) {
                arrivalDate = Date.from(arrivalDate.toInstant().minus(Duration.ofHours(sumInt)));
                SimpleDateFormat format = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String output = format.format(arrivalDate);
                schedule.put("arrival_date", output);
            } else if(longoDeparture > longoArrival) {
                arrivalDate = Date.from(arrivalDate.toInstant().plus(Duration.ofHours(sumInt)));
                SimpleDateFormat format = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String output = format.format(arrivalDate);
                //Date Stringo = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX")).parse(departureDateString);
                schedule.put("arrival_date", output);
            }

            future.complete(schedule);

        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> availableSeatsByDestinationToTijuana(Integer scheduleRouteDestinationID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        final JsonArray params = new JsonArray().add(scheduleRouteDestinationID);
        this.dbClient.queryWithParams(QUERY_GET_ORDERS_BY_DESTINATION, params, reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }

                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    throw new Exception("Schedule route destination not found");
                }

                JsonObject scheduleRouteDestination = results.get(0);
                Integer scheduleRouteId = scheduleRouteDestination.getInteger("schedule_route_id");
                Integer orderOrigin = scheduleRouteDestination.getInteger("order_origin");
                Integer orderDestiny = scheduleRouteDestination.getInteger("order_destiny");
                final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                        .add(orderOrigin).add(orderDestiny)
                        .add(orderDestiny).add(orderDestiny)
                        .add(orderOrigin).add(orderDestiny);

                this.dbClient.queryWithParams(QUERY_AVAILABLE_SEATING_BY_DESTINATION, paramsList, replyList -> {
                    try {
                        if (replyList.failed()) {
                            throw new Exception(replyList.cause());
                        }

                        List<JsonObject> resultsList = replyList.result().getRows();
                        if (resultsList.isEmpty()) {
                            throw new Exception("Results not found");
                        }

                        future.complete(resultsList.get(0));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }

                });
            } catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_SCHEDULE_VEHICLE_BETWEEN_DATES = "SELECT \n"
            + " COUNT(sr.id) AS schedules, \n"
            + " v.name\n"
            + " FROM schedule_route AS sr \n "
            + " LEFT JOIN vehicle v ON sr.vehicle_id = v.id"
            + " WHERE sr.schedule_status NOT IN ('canceled') AND sr.status = 1 AND sr.vehicle_id = ? \n "
            + " AND ( \n"
            + "         (sr.travel_date BETWEEN ? AND ?) \n"
            + "     OR (sr.arrival_date BETWEEN ? AND ?) \n"
            + ");";
    private static final String QUERY_GET_CONFIG_ROUTE = "SELECT \n"
            + " cr.* \n"
            + " FROM config_schedule AS cs \n "
            + " JOIN config_route AS cr ON cs.config_route_id=cr.id \n"
            + " WHERE cs.id = ? AND cs.status = 1 AND cr.status = 1 LIMIT 1;";
    private static final String QUERY_GET_CONFIG_DESTINATION = "SELECT \n"
            + " cd.*, origin.time_checkpoint AS origin_time_checkpoint, \n"
            + " destiny.time_checkpoint AS destiny_time_checkpoint \n"
            + "FROM config_destination AS cd \n"
            + " INNER JOIN config_route AS cr ON cr.id=cd.config_route_id \n"
            + " INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n"
            + " INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id \n"
            + " WHERE cr.id = ? AND cd.status = 1 AND cr.status = 1;\n";

    private static final String QUERY_GET_STOPS_SCHEDULES = "SELECT   \n" +
            "cd.order_origin,\n" +
            "            cd.order_destiny,\n" +
            "            origin.id AS terminal_origin_id,  \n" +
            "            origin.name AS terminal_origin_name,  \n" +
            "            origin.prefix as terminal_origin_prefix,    \n" +
            "            destiny.id AS terminal_destiny_id,  \n" +
            "            destiny.name AS terminal_destiny_name,   \n" +
            "            destiny.prefix as terminal_destiny_prefix,\n" +
            "            srdd.time_checkpoint,\n" +
            "            srdd.id,\n" +
            "            cd.distance_km,\n" +
            "            e.id AS employee_id,\n" +
            "            CONCAT(e.name , ' ', e.last_name) AS driver\n" +
            "            FROM schedule_route AS sr   \n" +
            "               INNER JOIN schedule_route_destination AS srdd ON srdd.schedule_route_id = sr.id\n" +
            "               INNER JOIN schedule_route_driver AS srd ON srd.schedule_route_id = sr.id AND srd.terminal_origin_id=srdd.terminal_origin_id AND srd.terminal_destiny_id=srdd.terminal_destiny_id\n" +
            "               INNER JOIN config_destination AS cd ON cd.id = srdd.config_destination_id\n" +
            "               INNER JOIN employee AS e ON e.id = srd.employee_id\n" +
            "               INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id   \n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            WHERE sr.id = ? AND cd.config_route_id = ? AND (COALESCE(cd.order_destiny, 0) - COALESCE(cd.order_origin, 0)) = 1";
    private static final String QUERY_GET_SCHEDULE_ROUTE_TEMPLATE = "SELECT sr.id,sr.config_route_id,sr.config_schedule_id, sr.vehicle_id, sr.travel_date, sr.arrival_date FROM schedule_route AS sr \n" +
            "    WHERE sr.template_name = ? and is_template = 1 ";
    private static final String QUERY_GET_TEMPLATE_LIST = "SELECT DISTINCT template_name FROM schedule_route WHERE is_template = true AND config_route_id = ? AND config_schedule_id = ?;";
    private static final String QUERY_SCHEDULE_ROUTE_DRIVER_CHANGE = "SELECT * FROM schedule_route_driver where schedule_route_id = ? AND terminal_origin_id = ? AND status = 1;";

    private static final String QUERY_CONFIG_ROUTE_STATUS = "SELECT * FROM schedule_route\n" +
            "WHERE \n" +
            "config_route_id  = ?\n" +
            "AND DATE(travel_date) between DATE(?) AND date_add(DATE(?), interval 6 day) \n" +
            "AND TIME(travel_date) = TIME(?)\n" +
            "AND status = 1 AND schedule_status != 'canceled'\n" +
            "UNION\n" +
            "select sr.* from schedule_route sr\n" +
            "left join config_route cr on cr.id = sr.config_route_id AND cr.parcel_route IS FALSE\n" +
            "left join config_schedule cs on cs.id = sr.config_schedule_id\n" +
            "left join schedule_route sr2 on cs.from_id = sr2.config_schedule_id and sr.travel_date = concat(DATE_FORMAT(sr2.travel_date, '%Y-%m-%d'), ' ', cs.travel_hour)\n" +
            "WHERE \n" +
            "sr2.config_route_id  = ?\n" +
            "AND DATE(sr2.travel_date) between DATE(?) AND date_add(DATE(?), interval 6 day) \n" +
            "AND TIME(sr2.travel_date) = TIME(?)\n" +
            "AND sr.status = 1 AND sr.schedule_status != 'canceled';";

    private static final String UPDATE_DRIVER_IN_SCHEDULE_ROUTE_DRIVER = "UPDATE schedule_route_driver SET employee_id = ? where id >= ? AND schedule_route_id = ? ";

    private static final String UPDATE_SECOND_DRIVER_IN_SCHEDULE_ROUTE_DRIVER = "UPDATE schedule_route_driver SET second_employee_id = ? where id >= ? AND schedule_route_id = ? ";

    private static final String QUERY_SCHEDULE_ROUTES_LIST = "SELECT \n"
            + " sr.id AS id,\n"
            + " vh.economic_number AS economic_number,\n"
            + " origin.name AS terminal_origin_name,\n"
            + " origin.prefix AS terminal_origin_prefix,\n"
            + " destiny.name AS terminal_destiny_name,\n"
            + " destiny.prefix AS terminal_destiny_prefix,\n"
            + " sr.arrival_date AS arrival_date,\n"
            + " sr.travel_date AS travel_date,\n"
            + " sr.schedule_status AS schedule_status\n"
            + " FROM schedule_route AS sr\n"
            + " JOIN vehicle AS vh \n"
            + " ON vh.id=sr.vehicle_id \n"
            + " JOIN config_route AS cr \n"
            + " ON cr.id=sr.config_route_id AND cr.parcel_route IS FALSE\n"
            + " JOIN branchoffice AS origin \n"
            + " ON origin.id=cr.terminal_origin_id \n"
            + " JOIN branchoffice destiny \n"
            + " ON destiny.id=cr.terminal_destiny_id \n"
            + " WHERE sr.status = 1 AND origin.status = 1 AND destiny.status = 1\n";

    private static final String QUERY_SCHEDULE_ROUTE_DRIVERS = "SELECT \n"
            + " CONCAT_WS(\" \", emp.name, emp.last_name) AS full_name \n"
            + " FROM schedule_route_driver AS srd \n"
            + " JOIN employee AS emp \n"
            + " ON emp.id=srd.employee_id \n"
            + " WHERE emp.status = 1 AND srd.status = 1 AND srd.driver_status = '1' \n"
            + " AND srd.schedule_route_id = ? \n";

    private static final String QUERY_DRIVER_SCHEDULE_ROUTES = "SELECT sr.id AS schedule_route_id, \n" +
            "sd.id AS schedule_route_destination_id, \n" +
            "vh.economic_number AS vehicle_economic_number, \n" +
            "vh.iave_code iave_code, \n" +
            "vh.id AS vehicle_id, \n" +
            "origin.id AS terminal_origin_id, \n" +
            "origin.name AS terminal_origin_name, \n" +
            "origin.prefix AS terminal_origin_prefix,\n" +
            "origin_city.name AS terminal_origin_city,\n" +
            "origin_state.name AS terminal_origin_state,\n" +
            "destiny.id AS terminal_destiny_id, \n" +
            "destiny.name AS terminal_destiny_name,\n" +
            "destiny.prefix AS terminal_destiny_prefix, \n" +
            "destiny_city.name AS terminal_destiny_city,\n" +
            "destiny_state.name AS terminal_destiny_state,\n" +
            "cr.travel_time AS schedule_travel_time, \n" +
            "sr.arrival_date AS schedule_arrival_date, \n" +
            "sr.travel_date AS schedule_travel_date, \n" +
            "sr.schedule_status AS schedule_status, \n" +
            "cr.id AS config_route_id, cd.order_origin, cd.order_destiny, cd.distance_km, \n" +
            "sd.config_destination_id \n" +
            "FROM schedule_route AS sr \n" +
            "JOIN schedule_route_driver AS srd ON sr.id=srd.schedule_route_id \n" +
            "JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n" +
            "JOIN config_route AS cr ON cr.id=sr.config_route_id AND cr.parcel_route IS FALSE\n" +
            "JOIN branchoffice AS origin ON origin.id=cr.terminal_origin_id \n" +
            "JOIN branchoffice destiny ON destiny.id=cr.terminal_destiny_id \n" +
            "LEFT JOIN city AS origin_city ON origin_city.id = origin.city_id \n" +
            "LEFT JOIN city AS destiny_city ON destiny_city.id = destiny.city_id \n" +
            "LEFT JOIN state AS origin_state ON origin_state.id = origin.state_id \n" +
            "LEFT JOIN state AS destiny_state ON destiny_state.id = destiny.state_id\n" +
            "JOIN schedule_route_destination AS sd ON sd.schedule_route_id=sr.id AND sd.terminal_origin_id=origin.id AND sd.terminal_destiny_id=destiny.id \n" +
            "JOIN config_destination AS cd ON cd.id = sd.config_destination_id " +
            "WHERE sr.status = 1 AND origin.status = 1 AND destiny.status = 1 AND srd.employee_id = ? " +
            "AND DATE(sr.travel_date) = ? ";

    private static final String QUERY_DRIVER_SCHEDULE_ROUTES_ORDER = " GROUP BY sr.id ORDER BY sr.travel_date ASC ";


    private static final String QUERY_DRIVER_SCHEDULE_ROUTE_AVAILABLE_SEATS = "SELECT\n"
            + " sr.id AS schedule_route_id,\n"
            + " sd.id AS schedule_route_destination_id,\n"
            + " sr.schedule_status AS schedule_status,\n"
            + " sd.terminal_origin_id, sd.terminal_destiny_id\n"
            + " FROM schedule_route AS sr\n"
            + " JOIN config_route AS cr ON cr.id=sr.config_route_id AND cr.parcel_route IS FALSE\n"
            + " JOIN schedule_route_driver AS srd ON sr.id=srd.schedule_route_id\n"
            + " JOIN schedule_route_destination AS sd ON sd.schedule_route_id=sr.id\n"
            + " AND sd.terminal_origin_id = ? AND sd.terminal_destiny_id=cr.terminal_destiny_id\n"
            + " WHERE sr.id = ? AND sr.status > 0 AND srd.employee_id = ?\n" // TODO: Validate sr.schedule_status are equals to 2 (in-transit)
            + " ORDER BY sr.travel_date ASC LIMIT 1;";

    private static final String QUERY_SCHEDULE_ROUTE_WITH_TERMINALS = "SELECT\n"
            + " sr.*, cr.terminal_origin_id, cr.terminal_destiny_id\n"
            + " FROM schedule_route AS sr\n"
            + " INNER JOIN config_route AS cr ON  cr.id = sr.config_route_id AND cr.parcel_route IS FALSE\n"
            + " WHERE sr.id = ? AND sr.status = 1 AND sr.schedule_status = ? LIMIT 1;";

    private static final String QUERY_SCHEDULE_ROUTE_DESTINATIONS = "SELECT\n"
            + " srd.*, cd.order_origin, cd.order_destiny\n"
            + " FROM schedule_route_destination AS srd\n"
            + " INNER JOIN config_destination AS cd ON cd.id=srd.config_destination_id\n"
            + " WHERE schedule_route_id = ? AND srd.terminal_origin_id = ?\n"
            + " AND srd.status = 1 AND srd.destination_status = ?"
            + " ORDER BY cd.order_origin ASC, cd.order_destiny ASC;";


    private static final String QUERY_SEARCH_BY_TERMINALS = "SELECT\n"
            + "	cr.id as config_route_id,\n"
            + "	cr.terminal_origin_id,\n"
            + "	cr.terminal_destiny_id,\n"
            + "	sr.id as schedule_route_id,\n"
            + "	sr.travel_date as departure_date,\n"
            + "	cs.travel_hour as departure_time\n"
            + "FROM\n"
            + "	config_route cr\n"
            + "JOIN schedule_route sr on\n"
            + "	sr.config_route_id = cr.id\n"
            + "JOIN config_schedule cs on\n"
            + "	sr.config_schedule_id = cs.id\n"
            + "WHERE\n"
            + "	cr.terminal_origin_id = ?\n"
            + "	AND cr.terminal_destiny_id = ?\n"
            + "	AND sr.travel_date = ?"
            + " AND cr.status = 1"
            + " AND sr.status = 1"
            + " AND cs.status = 1";

    private static final String QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE = "SELECT \n"
            + "	srd.id AS schedule_route_destination_id, \n"
            + "	srd.schedule_route_id AS schedule_route_id, \n"
            + "	srd.destination_status, \n"
            + "	srd.config_destination_id AS config_destination_id, \n"
            + "	srd.travel_date AS departure_date, \n"
            + "	srd.arrival_date AS arrival_date, \n"
            + "	cd.order_origin AS order_origin, \n"
            + "	cd.order_destiny AS order_destiny, \n"
            + " cd.terminal_origin_id AS terminal_origin_id, \n"
            + " cd.terminal_destiny_id AS terminal_destiny_id, \n"
            + " cd.travel_time, \n"
            + "	cr.discount_tickets AS discount_tickets, \n"
            + " vh.config_vehicle_id AS config_vehicle_id, \n"
            + " vh.economic_number AS economic_number, \n"
            + " cv.allow_pets, \n"
            + " cv.seatings AS seatings, \n"
            + " cv.allow_frozen, \n"
            + " sr.vehicle_id AS vehicle_id, \n"
            + " sr.config_schedule_id AS config_schedule_id, \n"
            + " sr.config_route_id AS config_route_id, \n"
            + " sr.code AS schedule_route_code, \n"
            + " sr.status_hide"
            + " FROM schedule_route_destination AS srd \n"
            + " INNER JOIN schedule_route AS sr \n"
            + " ON sr.id=srd.schedule_route_id \n"
            + " INNER JOIN vehicle AS vh \n"
            + " ON vh.id=sr.vehicle_id \n"
            + " INNER JOIN config_vehicle AS cv \n"
            + " ON cv.id=vh.config_vehicle_id \n"
            + " INNER JOIN config_destination AS cd \n"
            + " ON cd.id=srd.config_destination_id \n"
            + " INNER JOIN config_route AS cr \n"
            + " ON cr.id=cd.config_route_id AND cr.parcel_route IS FALSE\n"
            + " WHERE \n"
            + "	srd.travel_date BETWEEN ? AND ? \n"
            + "	AND srd.terminal_origin_id = ?  \n"
            + "	AND srd.terminal_destiny_id = ? \n"
            + " AND srd.status = 1 AND sr.status_hide != 1 AND (srd.destination_status = 'scheduled' OR srd.destination_status = 'loading' OR srd.destination_status = 'ready-to-load' OR srd.destination_status = 'ready-to-load'  OR srd.destination_status = 'downloading')\n"
            + " AND sr.status = 1 ";

    private static final String QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE_ORIGEN = "SELECT \n"
            + "	srd.id AS schedule_route_destination_id, \n"
            + "	srd.schedule_route_id AS schedule_route_id, \n"
            + "	srd.destination_status, \n"
            + "	srd.config_destination_id AS config_destination_id, \n"
            + "	srd.travel_date AS departure_date, \n"
            + "	srd.arrival_date AS arrival_date, \n"
            + "	cd.order_origin AS order_origin, \n"
            + "	cd.order_destiny AS order_destiny, \n"
            + " cd.terminal_origin_id AS terminal_origin_id, \n"
            + " cd.terminal_destiny_id AS terminal_destiny_id, \n"
            + " cd.travel_time, \n"
            + "	cr.discount_tickets AS discount_tickets, \n"
            + " vh.config_vehicle_id AS config_vehicle_id, \n"
            + " vh.economic_number AS economic_number, \n"
            + " cv.allow_pets, \n"
            + " cv.seatings AS seatings, \n"
            + " cv.allow_frozen, \n"
            + " sr.vehicle_id AS vehicle_id, \n"
            + " sr.config_schedule_id AS config_schedule_id, \n"
            + " sr.config_route_id AS config_route_id, \n"
            + " sr.code AS schedule_route_code, \n"
            + " branchO.timezone as origen_timezone,\n"
            + " branchD.timezone as destiny_timezone, \n"
            + " sr.status_hide"
            + " FROM schedule_route_destination AS srd \n"
            + " INNER JOIN schedule_route AS sr \n"
            + " ON sr.id=srd.schedule_route_id \n"
            + " INNER JOIN vehicle AS vh \n"
            + " ON vh.id=sr.vehicle_id \n"
            + " INNER JOIN config_vehicle AS cv \n"
            + " ON cv.id=vh.config_vehicle_id \n"
            + " INNER JOIN config_destination AS cd \n"
            + " ON cd.id=srd.config_destination_id \n"
            + " INNER JOIN config_route AS cr \n"
            + " ON cr.id=cd.config_route_id AND cr.parcel_route IS FALSE\n"
            + " LEFT JOIN branchoffice AS branchO\n"
            + " ON branchO.id = srd.terminal_origin_id\n"
            + " LEFT JOIN branchoffice AS branchD\n"
            + " ON branchD.id = srd.terminal_destiny_id\n"
            + " WHERE \n"
            + "	srd.travel_date BETWEEN ? AND ? \n"
            + "	AND srd.terminal_origin_id = ?  \n"
            + "	AND srd.terminal_destiny_id = ? \n"
            + " AND srd.status = 1  AND (srd.destination_status != 'canceled')\n"
            + " AND sr.status = 1";

    private static final String QUERY_AVAILABLE_ORIGIN_ROUTES = "SELECT \n"
            + "	srd.id AS schedule_route_destination_id, \n"
            + "	srd.schedule_route_id AS schedule_route_id, \n"
            + "	srd.destination_status, \n"
            + "	srd.config_destination_id AS config_destination_id, \n"
            + "	srd.travel_date AS departure_date, \n"
            + "	srd.arrival_date AS arrival_date, \n"
            + "	srd.destination_status+0 AS destination_status, \n"
            + "	cd.order_origin AS order_origin, \n"
            + "	cd.order_destiny AS order_destiny, \n"
            + " cd.terminal_origin_id AS terminal_origin_id, \n"
            + " cd.terminal_destiny_id AS terminal_destiny_id, \n"
            + " cd.travel_time, \n"
            + " origin.name AS terminal_origin_name, \n"
            + " destiny.name AS terminal_destiny_name, \n"
            + " origin_city.name AS terminal_origin_city_name, \n"
            + " destiny_city.name AS terminal_destiny_city_name, \n"
            + "	cr.discount_tickets AS discount_tickets, \n"
            + " vh.config_vehicle_id AS config_vehicle_id, \n"
            + " vh.economic_number AS economic_number, \n"
            + " cv.seatings AS seatings, \n"
            + " sr.vehicle_id AS vehicle_id, \n"
            + " sr.config_schedule_id AS config_schedule_id, \n"
            + " sr.config_route_id AS config_route_id \n"
            + " FROM schedule_route_destination AS srd \n"
            + " INNER JOIN schedule_route AS sr \n"
            + " ON sr.id=srd.schedule_route_id \n"
            + " INNER JOIN vehicle AS vh \n"
            + " ON vh.id=sr.vehicle_id \n"
            + " INNER JOIN config_vehicle AS cv \n"
            + " ON cv.id=vh.config_vehicle_id \n"
            + " INNER JOIN config_destination AS cd \n"
            + " ON cd.id=srd.config_destination_id \n"
            + " INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id \n"
            + " INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n"
            + " INNER JOIN city AS destiny_city ON destiny_city.id=destiny.city_id \n"
            + " INNER JOIN city AS origin_city ON origin_city.id=origin.city_id \n"
            + " INNER JOIN config_route AS cr \n"
            + " ON cr.id=cd.config_route_id AND cr.parcel_route IS FALSE\n"
            + " WHERE \n"
            + "	srd.travel_date BETWEEN ? AND ? \n"
            + "	AND srd.terminal_origin_id = ?  \n"
            + " AND srd.status = 1 AND (srd.destination_status != 'canceled') \n"
            + " AND sr.status = 1 \n"
            + " ORDER BY srd.travel_date \n"
            + " LIMIT 10 ";

    private static final String QUERY_STOPS_BY_ROUTE = "SELECT \n" +
            "            cd.order_origin," +
            "            cd.order_destiny, " +
            "            origin.id AS terminal_origin_id,\n" +
            "            origin.name AS terminal_origin_name,\n" +
            "            origin.prefix as terminal_origin_prefix, "  +
            "            origin.latitude AS terminal_origin_latitude,\n" +
            "            origin.longitude AS terminal_origin_longitude,\n" +
            "            destiny.id AS terminal_destiny_id,\n" +
            "            destiny.name AS terminal_destiny_name, \n" +
            "            destiny.prefix as terminal_destiny_prefix, " +
            "            destiny.latitude AS terminal_destiny_latitude,\n" +
            "            destiny.longitude AS terminal_destiny_longitude,\n" +
            "            origin.time_checkpoint AS terminal_origin_time_checkpoint, \n" +
            "            destiny.time_checkpoint AS terminal_destiny_time_checkpoint,\n" +
            "            cd.distance_km,\n" +
            "            srd.destination_status,\n"+
            "            srd.id AS schedule_route_destination_id, \n"+
            "            cd.travel_time AS stop_travel_time, \n"+
            "            if(origin.city_id = destiny.city_id , true , false ) as same_city \n"+
            "            FROM config_destination AS cd \n" +
            "            INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id \n" +
            "            INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            INNER JOIN schedule_route_destination AS srd ON srd.config_destination_id = cd.id AND srd.schedule_route_id = ? \n"+
            "            WHERE config_route_id = ? AND cd.order_origin >= ? AND cd.order_destiny <= ? AND (COALESCE(cd.order_destiny, 0) - COALESCE(cd.order_origin, 0)) = 1;";

    private static final String QUERY_PRICES_BY_ROUTE = "SELECT \n"
            + " st.id AS special_ticket_id, st.name AS special_ticket_name, \n"
            + " st.base AS is_base, \n"
            + " st.is_default, \n"
            + " st.origin_allowed, \n"
            + " ctp.amount AS amount, \n"
            + " ctp.discount AS discount, \n"
            + " ctp.total_amount AS total_amount \n"
            + " FROM config_ticket_price AS ctp \n"
            + " INNER JOIN config_destination AS cd ON cd.id=ctp.config_destination_id \n"
            + " INNER JOIN special_ticket AS st ON st.id=ctp.special_ticket_id AND st.status = 1 \n"
            + " WHERE ctp.config_destination_id = ? \n"
            + " AND ctp.status = 1 AND ctp.price_status = 1 AND st.base = false \n";

    private static final String QUERY_PRICES_BY_ROUTE_V2 = "SELECT \n" +
            "   st.id AS special_ticket_id, st.name AS special_ticket_name, \n" +
            "   st.base AS is_base, \n" +
            "   st.is_default, \n" +
            "   st.origin_allowed, \n" +
            "   ctp.amount AS amount, \n" +
            "   ctp.discount AS discount, \n" +
            "   ctp.total_amount AS total_amount,\n" +
            "   cd.terminal_origin_id,\n" +
            "   cd.terminal_destiny_id,\n" +
            "   srd.travel_date,\n" +
            "   cd.config_route_id\n" +
            "FROM config_ticket_price AS ctp \n" +
            "INNER JOIN config_destination AS cd ON cd.id=ctp.config_destination_id \n" +
            "INNER JOIN schedule_route_destination srd ON srd.config_destination_id = cd.id\n" +
            "   AND srd.terminal_origin_id = cd.terminal_origin_id AND srd.terminal_destiny_id = cd.terminal_destiny_id\n" +
            "INNER JOIN special_ticket AS st ON st.id=ctp.special_ticket_id AND st.status = 1 \n" +
            "WHERE srd.id = ?\n" +
            "AND ctp.status = 1 AND ctp.price_status = 1 AND st.base = false";
 
     // PARAMS: $SCHEDULE_ROUTE_ID=76, $ORDER_ORIGIN=2, $ORDER_DESTINY=3
     /** WHERE: srd.schedule_route_id = $SCHEDULE_ROUTE_ID 
         AND ((cd.order_destiny > $ORDER_ORIGIN AND cd.order_destiny <= $ORDER_DESTINY) 
        OR ($ORDER_DESTINY > cd.order_origin AND $ORDER_DESTINY <= cd.order_destiny)
        OR (cd.order_origin = $ORDER_ORIGIN AND cd.order_destiny = $ORDER_DESTINY)
        )
        AND bp.status = 1 AND (bp.boardingpass_status = 1 OR bp.boardingpass_status = 4)
        GROUP BY srd.schedule_route_id;
        */
    private static final String QUERY_AVAILABLE_SEATING_BY_DESTINATION = " SELECT \n" +
             " bps.seatings, bps.seatings - SUM(IF(bps.status IS NOT NULL AND bps.ticket_status IN (1, 2), 1, 0)) AS available_seatings, bps.schedule_route_id FROM \n" +
             " (SELECT cv.seatings, bpt.id, bpt.seat, bpt.ticket_status, bp.status, bp.boardingpass_status, srd.schedule_route_id \n" +
             " FROM schedule_route_destination AS srd \n" +
             " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n" +
             " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n" +
             " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n" +
             " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n" +
             " LEFT JOIN boarding_pass_route AS bpr ON srd.id=bpr.schedule_route_destination_id \n" +
             " LEFT JOIN boarding_pass AS bp ON bp.id=bpr.boarding_pass_id AND bp.status = 1 AND bp.boardingpass_status IN (1, 2, 4)\n" +
             " LEFT JOIN boarding_pass_ticket AS bpt ON bpr.id=bpt.boarding_pass_route_id AND bpt.status = 1 AND bpt.seat != '' \n" +
             " WHERE srd.schedule_route_id = ?\n" +
             " AND ((cd.order_destiny > ? AND cd.order_destiny <= ?) \n" +
             "     OR (? > cd.order_origin AND ? <= cd.order_destiny) \n" +
             "     OR (cd.order_origin = ? AND cd.order_destiny = ?) \n" +
             //" ) and bp.status IS NOT NULL AND bpt.ticket_status IN (1, 2)\n" +
             " ) \n" +
             " group by bpt.seat ) AS bps \n" +
             " GROUP BY bps.schedule_route_id;";

    private static final String QUERY_GET_SCHEDULES_DESTINATIONS = "SELECT id FROM schedule_route_destination where schedule_route_id = ? AND destination_status = 'canceled' ";
    private static final String QUERY_GET_COUNT_SCHEDULES_DESTINATIONS = "SELECT COUNT(srd.id) AS total_schedules , sr.schedule_status FROM schedule_route_destination AS srd INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id where srd.schedule_route_id = ? AND srd.destination_status != 'canceled'";
    private static final String QUERY_UPDATE_SCHEDULE_STATUS = "UPDATE schedule_route SET schedule_status = 'canceled' WHERE id = ? ";
    private static final String SEARCH_SCHEDULE_ROUTE_DESTINATION = "select srdd.* from schedule_route_destination srd inner join schedule_route sr on sr.id = srd.schedule_route_id inner join config_route cr on cr.id = sr.config_route_id inner join schedule_route_destination srdd on sr.id = srdd.schedule_route_id and srd.terminal_origin_id = srdd.terminal_origin_id and cr.terminal_destiny_id = srdd.terminal_destiny_id where srd.id = ?;";
    private static final String SEARCH_SCHEDULE_ROUTE_STATUS = " select srd.id, sr.schedule_status\n" +
            " FROM schedule_route AS sr\n" +
            "    INNER JOIN schedule_route_destination AS srd ON srd.schedule_route_id = sr.id\n" +
            "    WHERE sr.id = ? AND srd.terminal_origin_id = ? AND srd.terminal_destiny_id = ? ";
    private static final String UPDATE_REMOVE_SCHEDULE_ROUTE_DESTINATION = "UPDATE schedule_route_destination AS srd  \n" +
            " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n" +
            " SET srd.destination_status = 'canceled'\n" +
            " WHERE srd.schedule_route_id = ? \n" +
            " AND ((cd.order_destiny > ? AND cd.order_destiny <= ?)  \n" +
            "     OR (? > cd.order_origin AND ? <= cd.order_destiny) \n" +
            "     OR (cd.order_origin = ? AND cd.order_destiny = ?) \n";
    // PARAMS: $SCHEDULE_ROUTE_DESTINATION_ID=40
    private static final String QUERY_GET_ORDERS_BY_DESTINATION = "SELECT \n"
        + " srd.schedule_route_id, cd.order_origin, cd.order_destiny \n"
        + " FROM schedule_route_destination AS srd \n"
        + " INNER JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
        + " WHERE srd.id = ?";
    private static final String QUERY_GET_ORDERS_BY_DESTINATION_REMOVE = "SELECT \n"
            + " cv.*, srd.schedule_route_id, cd.order_origin, cd.order_destiny \n"
            + " FROM schedule_route_destination AS srd \n"
            + " INNER JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
            + " INNER JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
            + " INNER JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
            + " INNER JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
            + " WHERE ";
    // PARAMS: $SCHEDULE_ROUTE_ID=108
    private static final String QUERY_GET_SPECIAL_TICKETS_AVAILABLE_BY_ROUTE = "SELECT \n"
        + " st.id, st.available_tickets - COALESCE(COUNT(rst.seat),0) AS available_tickets, st.name \n"
        + " FROM special_ticket AS st \n"
        + " LEFT JOIN (SELECT bp.id AS boarding_pass_id, bpt.seat, srd.schedule_route_id, ctp.special_ticket_id, st.name \n"
        + " FROM schedule_route_destination AS srd \n"
        + " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
        + " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
        + " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
        + " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
        + " LEFT JOIN boarding_pass_route AS bpr ON srd.id=bpr.schedule_route_destination_id \n"
        + " LEFT JOIN boarding_pass AS bp ON bp.id=bpr.boarding_pass_id \n"
        + " AND bp.status = 1 AND (bp.boardingpass_status = 1 OR bp.boardingpass_status = 4) \n"
        + " LEFT JOIN boarding_pass_ticket AS bpt ON bpr.id=bpt.boarding_pass_route_id AND bpt.status = 1 \n"
        + " INNER JOIN config_ticket_price AS ctp ON ctp.id=bpt.config_ticket_price_id \n"
        + " LEFT JOIN special_ticket AS st ON st.id=ctp.special_ticket_id \n"
        + " WHERE srd.schedule_route_id = ?) AS rst \n"
        + " ON st.id=rst.special_ticket_id AND rst.boarding_pass_id IS NOT NULL \n"
        + " WHERE st.status = 1 AND st.available_tickets > 0 GROUP BY st.id;";

    // PARAMS: $SCHEDULE_ROUTE_ID=108, $TERMINAL_ORIGIN_ID=83
    private static final String QUERY_GET_SPECIAL_TICKETS_AVAIABLES_BY_ORIGIN = "SELECT \n"
        + " st.id, st.available_tickets - COALESCE(COUNT(rst.seat),0) AS available_tickets, st.name \n"
        + " FROM special_ticket AS st \n"
        + " LEFT JOIN (SELECT bp.id AS boarding_pass_id, bpt.seat, srd.schedule_route_id, ctp.special_ticket_id, st.name \n"
        + " FROM schedule_route_destination AS srd \n"
        + " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
        + " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
        + " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
        + " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
        + " LEFT JOIN boarding_pass_route AS bpr ON srd.id=bpr.schedule_route_destination_id \n"
        + " LEFT JOIN boarding_pass AS bp ON bp.id=bpr.boarding_pass_id \n"
        + " AND bp.status = 1 AND (bp.boardingpass_status = 1 OR bp.boardingpass_status = 4) \n"
        + " LEFT JOIN boarding_pass_ticket AS bpt ON bpr.id=bpt.boarding_pass_route_id AND bpt.status = 1 \n"
        + " INNER JOIN config_ticket_price AS ctp ON ctp.id=bpt.config_ticket_price_id \n"
        + " LEFT JOIN special_ticket AS st ON st.id=ctp.special_ticket_id \n"
        + " WHERE srd.schedule_route_id = ? AND srd.terminal_origin_id = ?) AS rst \n"
        + " ON st.id=rst.special_ticket_id AND rst.boarding_pass_id IS NOT NULL \n"
        + " WHERE st.status = 1 AND st.available_tickets > 0 GROUP BY st.id;";

    private static final String QUERY_SEARCH_STOPS = "SELECT\n"
            + "	cr.id,\n"
            + "	cr.name,\n"
            + "	cd.terminal_origin_id,\n"
            + "	cd.terminal_origin_id,\n"
            + "	cd.travel_time\n"
            + "FROM\n"
            + "	config_route cr\n"
            + "JOIN config_destination cd ON\n"
            + "	cd.config_route_id = cr.id\n"
            + "WHERE\n"
            + "	cr.id = ?"
            + " AND cd.status = 1";

    private static final String GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_DESTINATION_PARAM = " srd.id = ? ";

    private static final String QUERY_SUMMARY_COST = "/*resumen por fecha*/\n"
            + "SELECT\n"
            + "	count(sr.id) AS total_schedules_routes,\n"
            + "	MAX( total_amount ) AS expensivest,\n"
            + "	MIN( total_amount ) AS chepest\n"
            + "FROM\n"
            + "	config_ticket_price ctp\n"
            + "JOIN config_price_route cpr ON\n"
            + "	cpr.id = ctp.config_prices_route_id\n"
            + "JOIN config_route cr ON\n"
            + "	cr.id = cpr.config_route_id AND cr.parcel_route IS FALSE\n"
            + "JOIN schedule_route sr ON\n"
            + "	sr.config_route_id = cr.id\n"
            + "WHERE\n"
            + "	cr.terminal_origin_id = ?\n"
            + "	AND cr.terminal_destiny_id = ?\n"
            + "	AND sr.travel_date = ?\n"
            + "	AND cr.status = 1\n"
            + "	AND sr.status = 1\n"
            + "	AND ctp.status = 1\n"
            + "	AND cpr.status = 1\n"
            + "GROUP BY sr.id";

    /**
     * needs the schedule route id to search the total of used seats
     */
    private static final String QUERY_SEARCH_SEATS = "CALL sp_used_seats(?);";

    /**
     * needs the schedule route id to search the total of used by special tickets
     */
    private static final String QUERY_SEAR_USED_SEAT_BY_SPECIAL_TICKET = "CALL sp_used_seat_by_special_ticket(?);";

    /**
     * needs the schedule route id to search the total ticket with discount and the price
     */
    private static final String QUERY_SEARCH_SPECIAL_TICKETS_TOTAL_DISCOUNTS = "SELECT\n"
            + "	st.id AS special_ticket_id,\n"
            + "	st.total_discount,\n"
            + "	ctp.total_amount\n"
            + "FROM\n"
            + "	special_ticket st\n"
            + "JOIN config_ticket_price ctp \n"
            + "	ON ctp.special_ticket_id = st.id\n"
            + "JOIN config_route cr\n"
            + "	ON ctp.config_route_id = cr.id AND cr.parcel_route IS FALSE\n"
            + "JOIN schedule_route sr ON sr.config_route_id = cr.id\n"
            + "WHERE\n"
            + "	st.has_discount = TRUE\n"
            + "	AND sr.id = ?\n"
            + "	AND st.status = 1\n"
            + "	AND ctp.status = 1\n"
            + "	AND cr.status = 1";

    private static final String UPDATE_SCHEDULE_ROUTE_TEMPLATE = "UPDATE schedule_route SET is_template = false WHERE template_name = ?";
    private static final String UPDATE_NEW_SCHEDULE_ROUTE_TEMPLATE = "UPDATE schedule_route SET template_name = ?, is_template = true WHERE id = ?";
    private static final String QUERY_VEHICLE_CHARACTERISTICS = "SELECT vc.characteristic_id, c.name AS characteristic, c.icon\n" +
            " FROM vehicle_characteristic vc \n" +
            " JOIN characteristic c ON c.id = vc.characteristic_id\n" +
            " WHERE vc.status= 1 AND vc.vehicle_id = ?;";
    
    private static final String QUERY_SCHEDULE_ROUTE_AVAILABLE_SEATS = "SELECT "+
            "             sr.id AS schedule_route_id, "+
            "             sd.id AS schedule_route_destination_id, "+
            "             sr.schedule_status AS schedule_status, "+
            "             sd.terminal_origin_id, sd.terminal_destiny_id "+
            "             FROM schedule_route AS sr "+
            "             JOIN config_route AS cr ON cr.id=sr.config_route_id "+
            "             JOIN schedule_route_driver AS srd ON sr.id=srd.schedule_route_id "+
            "             JOIN schedule_route_destination AS sd ON sd.schedule_route_id=sr.id "+
            "             AND sd.terminal_origin_id = ? AND sd.terminal_destiny_id=cr.terminal_destiny_id "+
            "             WHERE sr.id = ? AND sr.status > 0 "+
            "             ORDER BY sr.travel_date ASC LIMIT 1;";

    // params schedule_route_id = 121 , employee_id = 1
    private static final String QUERY_GET_TERMINALS_DESTINY = "SELECT \n" +
            "distinct srd.terminal_destiny_id " +
            "FROM schedule_route as sr " +
            "inner join schedule_route_destination as srd on srd.schedule_route_id = sr.id " +
            "inner join employee as e on e.user_id = ? " +
            "inner join schedule_route_driver as srdd on srdd.schedule_route_id = sr.id " +
            "where sr.id = ? and srdd.employee_id = e.id and srd.destination_status != 'canceled' and srd.destination_status != 'finished-ok' " ;

    private static final String QUERY_GET_DEPARTURES_CALENDAR = "SELECT \n" +
            "\tsrd.schedule_route_id,\n" +
            "    srd.destination_status, \n" +
            "    srd.travel_date, \n" +
            "    srd.arrival_date, \n" +
            "    v.economic_number, \n" +
            "    IF(cr.parcel_route, CONCAT('PAQUETERIA - ', bo.prefix), bo.prefix) AS terminal_origin_prefix, \n" +
            "    bd.prefix AS terminal_destiny_prefix, \n" +
            "    bo.id AS terminal_origin_id, \n" +
            "    bd.id AS terminal_destiny_id , \n" +
            "    sr.status_hide,\n" +
            "    cr.parcel_route\n" +
            "FROM schedule_route_destination AS srd \n" +
            "INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN config_route AS cr ON cr.id = sr.config_route_id -- AND cr.parcel_route IS FALSE\n" +
            "INNER JOIN branchoffice AS bo ON bo.id = srd.terminal_origin_id \n" +
            "INNER JOIN branchoffice AS bd ON bd.id = srd.terminal_destiny_id \n" +
            "INNER JOIN vehicle AS v ON v.id = sr.vehicle_id \n" +
            "WHERE srd.travel_date BETWEEN ? AND ?\n" +
            "\tAND srd.terminal_origin_id = cr.terminal_origin_id  \n" +
            "\tAND srd.terminal_destiny_id = cr.terminal_destiny_id \n" +
            "\tAND srd.status = 1 \n" +
            "    AND sr.status = 1";
    private static final String QUERY_GET_DEPARTURES_CALENDAR_BY_TERMINAL = "SELECT \n" +
            "\tsrd.id as schedule_route_destination_id , \n" +
            "    srd.schedule_route_id,\n" +
            "    srd.destination_status, \n" +
            "    srd.travel_date, \n" +
            "    srd.arrival_date, \n" +
            "    v.economic_number, \n" +
            "    IF(cr.parcel_route, CONCAT('PAQUETERIA - ', bo.prefix), bo.prefix) AS terminal_origin_prefix, \n" +
            "    bd.prefix AS terminal_destiny_prefix, \n" +
            "    bo.id AS terminal_origin_id, \n" +
            "    bd.id AS terminal_destiny_id , \n" +
            "    sr.status_hide,\n" +
            "    cr.parcel_route\n" +
            "FROM schedule_route_destination AS srd \n" +
            "INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN config_route AS cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN branchoffice AS bo ON bo.id = srd.terminal_origin_id \n" +
            "INNER JOIN branchoffice AS bd ON bd.id = srd.terminal_destiny_id \n" +
            "INNER JOIN vehicle AS v ON v.id = sr.vehicle_id \n" +
            "WHERE srd.travel_date BETWEEN ? AND ?\n" ;

    private static final String QUERY_GET_DEPARTURES_CALENDAR_BY_TERMINAL_V2 = "SELECT srd.id as schedule_route_destination_id , srd.schedule_route_id,srd.destination_status, srd.travel_date, srd.arrival_date, v.economic_number, bo.prefix AS terminal_origin_prefix, bd.prefix AS terminal_destiny_prefix, bo.id AS terminal_origin_id, bd.id AS terminal_destiny_id FROM schedule_route_destination AS srd \n" +
            "                INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n" +
            "                INNER JOIN config_route AS cr ON cr.id = sr.config_route_id AND cr.parcel_route IS FALSE\n" +
            "                INNER JOIN branchoffice AS bo ON bo.id = srd.terminal_origin_id \n" +
            "                INNER JOIN branchoffice AS bd ON bd.id = srd.terminal_destiny_id \n" +
            "                INNER JOIN vehicle AS v ON v.id = sr.vehicle_id \n" +
            "             WHERE srd.travel_date BETWEEN ? AND ?\n" ;

    private static final String QUERY_FILTER_STATUS_DEPARTURES_CALENDAR = " AND srd.status = 1 \n" +
            "             AND sr.status = 1";
    private static final String QUERY_GET_SCHEDULE_ROUTE_DETAIL = "SELECT cv.*, sr.config_route_id, sr.arrival_date, v.economic_number , sr.id AS schedule_route_id, cr.terminal_origin_id, cr.terminal_destiny_id, bo.prefix AS terminal_origin_prefix, bd.prefix AS terminal_destiny_prefix, bo.name AS terminal_origin_name, bd.name AS terminal_destiny_name, sr.travel_date , sr.schedule_status, cr.name AS schedule_route_name, sr.code AS schedule_route_code, v.policy_insurance, v.sct_license, v.plate_state, v.policy, v.plate, v.vehicle_year, v.model,v.id as vehicle_id, tp.clave as tipoPermiso \n" +
            "    FROM schedule_route AS sr\n" +
            "    INNER JOIN vehicle AS v ON v.id = sr.vehicle_id\n" +
            "    INNER JOIN config_route AS cr ON cr.id = sr.config_route_id\n"+
            "    LEFT JOIN config_vehicle AS cv ON cv.id = v.config_vehicle_id\n" +
            "    INNER JOIN branchoffice AS bo ON bo.id = cr.terminal_origin_id \n" +
            "    INNER JOIN branchoffice AS bd ON bd.id = cr.terminal_destiny_id \n" +
            "    LEFT JOIN c_TipoPermiso AS tp ON tp.id = v.sat_permit_id \n" +
            "    where sr.id = ?";

    private static final String QUERY_AVAILABLE_SEAT_BY_DESTINATION = "SELECT \n"
            + " DISTINCT bpt.seat, srd.schedule_route_id, cd.terminal_destiny_id, branch.prefix AS terminal_destiny_prefix\n"
            + " FROM schedule_route_destination AS srd \n"
            + " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
            + " LEFT JOIN branchoffice AS branch ON branch.id=cd.terminal_destiny_id \n"
            + " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
            + " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
            + " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
            + " LEFT JOIN boarding_pass_route AS bpr ON srd.id=bpr.schedule_route_destination_id \n"
            + " INNER JOIN boarding_pass AS bp ON bp.id=bpr.boarding_pass_id \n"
            + " AND bp.status = 1 AND (bp.boardingpass_status != 4 and bp.boardingpass_status != 0) \n"
            + " LEFT JOIN boarding_pass_ticket AS bpt ON bpr.id=bpt.boarding_pass_route_id AND bpt.status = 1 \n"
            + " WHERE srd.schedule_route_id = ? AND bpt.seat != '' \n"
            + " AND ((cd.order_destiny > ? AND cd.order_destiny <= ?)  \n"
            + "     OR (? > cd.order_origin AND ? <= cd.order_destiny) \n"
            + "     OR (cd.order_origin = ? AND cd.order_destiny = ?) \n"
            + " )";
    private static final String QUERY_GET_STOPS_BY_SCHEDULE_ROUTE= " SELECT   \n" +
            "            cd.order_origin,\n" +
            "            cd.order_destiny,\n" +
            "            origin.id AS terminal_origin_id,  \n" +
            "            origin.name AS terminal_origin_name,  \n" +
            "            origin.prefix as terminal_origin_prefix,    \n" +
            "            destiny.id AS terminal_destiny_id,  \n" +
            "            destiny.name AS terminal_destiny_name,   \n" +
            "            destiny.prefix as terminal_destiny_prefix,\n" +
            "            srdd.time_checkpoint,\n" +
            "            srdd.id,\n" +
            "            srdd.arrival_date,\n"+
            "            srdd.travel_date,\n"+
            "            cd.distance_km,\n" +
            "            srdd.destination_status,\n"+
            "            e.id AS driver_id,\n"+
            "            e.rfc AS driver_rfc,\n"+
            "            e.driver_license AS driver_license,\n"+
            "            CONCAT(e.name , ' ', e.last_name) AS driver\n" +
            "            FROM schedule_route AS sr   \n" +
            "               INNER JOIN schedule_route_destination AS srdd ON srdd.schedule_route_id = sr.id\n" +
            "               INNER JOIN schedule_route_driver AS srd ON srd.schedule_route_id = sr.id AND srd.terminal_origin_id = srdd.terminal_origin_id AND srd.terminal_destiny_id = srdd.terminal_destiny_id\n" +
            "               INNER JOIN config_destination AS cd ON cd.id = srdd.config_destination_id\n" +
            "               LEFT JOIN employee AS e ON e.id = srd.employee_id\n" +
            "               INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id   \n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            WHERE sr.id = ? AND cd.config_route_id = ? AND (COALESCE(cd.order_destiny, 0) - COALESCE(cd.order_origin, 0)) = 1 GROUP BY srdd.id";
    // params schedule_route_id = 121 , terminal_destiny_id = 83

    private static final String QUERY_GET_TRAILERS_HISTORY_BY_SCHEDULE_ROUTE_ID = "SELECT\n" +
            "   b.name AS terminal_name, \n" +
            "    b.prefix AS terminal_prefix, \n" +
            "    t.economic_number AS trailer_name,\n" +
            "    st.action,\n" +
            "    tt.economic_number AS transfer_trailer_name,\n" +
            "    u.name AS created_by_name,\n" +
            "    st.created_at\n" +
            "FROM shipments_trailers st\n" +
            "INNER JOIN schedule_route sr ON sr.id = st.schedule_route_id\n" +
            "INNER JOIN shipments s ON s.id = st.shipment_id\n" +
            "INNER JOIN branchoffice b ON b.id = s.terminal_id\n" +
            "INNER JOIN trailers t ON t.id = st.trailer_id\n" +
            "LEFT JOIN trailers tt ON tt.id = st.transfer_trailer_id\n" +
            "INNER JOIN users u ON u.id = st.created_by\n" +
            "WHERE st.schedule_route_id = ?;";

    private static final String QUERY_GET_DRIVER_DESTINATIONS = "SELECT " +
            "            srd.id as schedule_route_destination_id, " +
            "            srd.terminal_destiny_id, " +
            "            destiny.name as terminal_destiny_name, " +
            "            destiny.prefix as terminal_destiny_prefix, " +
            "            destinyCity.id as city_id,  " +
            "            destinyCity.name as city_name , " +
            "            destinyState.name as state_name " +
            "            FROM schedule_route_destination as srd " +
            "            inner join config_destination as cd on srd.config_destination_id = cd.id " +
            "            inner join branchoffice as destiny on destiny.id = srd.terminal_destiny_id " +
            "            inner join city as destinyCity on destinyCity.id = destiny.city_id " +
            "            inner join county as destinyCounty on destinyCounty.id = destinyCity.county_id " +
            "            inner join state as destinyState on destinyState.id = destinyCounty.state_id " +
            "            where srd.schedule_route_id = ? and srd.terminal_destiny_id = ? and srd.terminal_origin_id = ? ;";

    private static final String QUERY_GET_POSITIONS_TERMINALS_BY_ROUTE = "SELECT DISTINCT\n" +
            "   CONCAT(bo.latitude, ', ', bo.longitude) AS origin_position,\n" +
            "   CONCAT(bd.latitude, ', ', bd.longitude) AS destiny_position\n" +
            " FROM  schedule_route_destination srd\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN config_route cr ON cr.id = cd.config_route_id AND cr.parcel_route IS FALSE\n" +
            " LEFT JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            " WHERE srd.id = ?;";

    private static final String QUERY_GET_PRICES_BY_REMAINING_PERCENT_OF_ROUTE = "SELECT\n" +
            " ctp.special_ticket_id,\n" +
            " ctp.id AS config_ticket_price_id,\n" +
            " st.name AS special_ticket_name,\n" +
            " st.base AS is_base,\n" +
            " st.origin_allowed,\n" +
            " ctp.amount,\n" +
            " ctp.discount,\n" +
            " CEILING(ctp.total_amount * (?/100)) AS total_amount\n" +
            " FROM schedule_route_destination srd\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN config_route cr ON cr.id = cd.config_route_id AND cr.parcel_route IS FALSE\n" +
            " AND cr.terminal_origin_id = srd.terminal_origin_id \n" +
            " AND cr.terminal_destiny_id = srd.terminal_destiny_id\n" +
            " LEFT JOIN config_ticket_price ctp ON ctp.config_destination_id = cd.id\n" +
            " LEFT JOIN special_ticket st ON st.id = ctp.special_ticket_id\n" +
            " WHERE st.status = 1 \n" +
            " AND srd.id = ?;";

    private static final String QUERY_GET_CONFIG_SCHEDULE_MAJOR_TRAVEL_HOUR = "SELECT\n" +
            "   sr.id,\n" +
            "   CAST(SUBSTRING_INDEX(code, '-', -1) AS UNSIGNED) AS order_route,\n" +
            "   CONCAT(LPAD(cr.id, 3, '0'), '-', DATE_FORMAT(travel_date, '%d%m%Y'), '-', COALESCE(SUBSTRING_INDEX(code, '-', -1), 0)+1) AS code\n" +
            "FROM schedule_route sr\n" +
            "LEFT JOIN config_schedule cs ON sr.config_schedule_id = cs.id\n" +
            "LEFT JOIN config_schedule css ON css.id = ?\n" +
            "LEFT JOIN config_route cr ON cr.id = cs.config_route_id\n" +
            "WHERE DATE(sr.travel_date) = DATE(?)\n" +
            "AND cs.travel_hour > css.travel_hour\n" +
            "AND cr.id = ?\n" +
            "ORDER BY order_route DESC;";

    private static final String QUERY_GET_QUANTITY_ROUTES_BY_TRAVEL_DATE_AND_CONFIG_ROUTE_ID = "SELECT\n" +
            "\tCOUNT(sr.id) AS quantity\n" +
            "FROM schedule_route sr\n" +
            "LEFT JOIN config_schedule cs ON sr.config_schedule_id = cs.id\n" +
            "LEFT JOIN config_route cr ON cr.id = cs.config_route_id\n" +
            "WHERE DATE(sr.travel_date) = DATE(?)\n" +
            "AND cr.id = ?;";

    private static final String QUERY_GET_SCHEDULE_ROUTE_INFO_BY_VEHICLE_ID = "SELECT \n" +
            "\tsrd.id AS schedule_route_destination_id, \n" +
            "    srd.schedule_route_id, \n" +
            "    srd.travel_date,\n" +
            "    v.economic_number\n" +
            "FROM schedule_route_destination srd\n" +
            "LEFT JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "WHERE DATE(srd.travel_date) BETWEEN ? AND ?\n" +
            "AND v.id = ?\n" +
            "{OPTIONAL_QUERY};";

    private static final String QUERY_GET_SCHEDULE_ROUTE_INFO_BY_DESTINATION_ID = "SELECT distinct \n" +
            "    srd.schedule_route_id, \n" +
            "    CONCAT(v.economic_number, \"-\", TIME(srd.travel_date)) as prefix, TIME(srd.travel_date) as time, \n" +
            "    srd.travel_date,\n" +
            "    v.economic_number,\n" +
            "    v.work_type,\n" +
            "    v.name,\n" +
            "    v.id vehicle_id,\n" +
            "    srd.terminal_origin_id,\n" +
            "    srd.terminal_destiny_id,\n" +
            "    cr.terminal_destiny_id as terminal_route_destiny_id,\n" +
            "    cr.terminal_origin_id as terminal_route_origin_id\n" +
            "FROM schedule_route_destination srd\n" +
            "LEFT JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "LEFT JOIN config_route cr ON cr.id = sr.config_route_id AND cr.parcel_route IS FALSE\n" +
            "LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "WHERE DATE(srd.travel_date) BETWEEN ? AND ?\n" +
            "{OPTIONAL_QUERY}\n order by TIME(srd.travel_date), v.economic_number";

    private static final String QUERY_GET_SHIPMENT_BY_ROUTE_ID = "select \n" +
            "sh.id,\n" +
            "sh.schedule_route_id,\n" +
            "br.prefix as terminal_id,\n" +
            "sh.shipment_type,\n" +
            "sh.shipment_status,\n" +
            "concat(emp.name , ' ' , emp.last_name) as driver_name,\n" +
            "usr.name as created_by,\n" +
            "usrUp.name as updated_by,\n" +
            "usr.email ,\n" +
            "sh.created_at,\n" +
            "sh.updated_at\n" +
            " from shipments sh \n" +
            " left join employee emp ON sh.driver_id = emp.id\n" +
            " left join users usr ON sh.created_by = usr.id\n" +
            " left join users usrUp ON sh.updated_by = usrUp.id\n" +
            " left join branchoffice br ON sh.terminal_id = br.id\n" +
            " where sh.schedule_route_id = ? ";

    private static final String QUERY_GET_STOPS_BY_SCHEDULE_ROUTE_NO_DRIVER = " SELECT   \n" +
            "            cd.order_origin,\n" +
            "            cd.order_destiny,\n" +
            "            origin.id AS terminal_origin_id,  \n" +
            "            origin.name AS terminal_origin_name,  \n" +
            "            origin.prefix as terminal_origin_prefix,    \n" +
            "            destiny.id AS terminal_destiny_id,  \n" +
            "            destiny.name AS terminal_destiny_name,   \n" +
            "            destiny.prefix as terminal_destiny_prefix,\n" +
            "            srdd.time_checkpoint,\n" +
            "            srdd.id,\n" +
            "            srdd.arrival_date,\n"+
            "            srdd.travel_date,\n"+
            "            cd.distance_km,\n" +
            "            srdd.destination_status,\n"+
            "            e.id AS driver_id,\n"+
            "            CONCAT(e.name , ' ', e.last_name) AS driver,\n" +
            "            sr.id AS schedule_route_id\n" +
            "            FROM schedule_route AS sr   \n" +
            "               INNER JOIN schedule_route_destination AS srdd ON srdd.schedule_route_id = sr.id\n" +
            "               INNER JOIN schedule_route_driver AS srd ON srd.schedule_route_id = sr.id \n" +
            "               INNER JOIN config_destination AS cd ON cd.id = srdd.config_destination_id\n" +
            "               INNER JOIN employee AS e ON e.id = srd.employee_id\n" +
            "               INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id   \n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            WHERE sr.id = ?  AND srdd.id = ? ";

    private static final String QUERY_GET_STOPS_BY_SCHEDULE_ROUTE_v2= " SELECT   \n" +
            "            cd.order_origin,\n" +
            "            cd.order_destiny,\n" +
            "            origin.id AS terminal_origin_id,  \n" +
            "            origin.name AS terminal_origin_name,  \n" +
            "            origin.prefix as terminal_origin_prefix,    \n" +
            "            destiny.id AS terminal_destiny_id,  \n" +
            "            destiny.name AS terminal_destiny_name,   \n" +
            "            destiny.prefix as terminal_destiny_prefix,\n" +
            "            srdd.time_checkpoint,\n" +
            "            srdd.id,\n" +
            "            srdd.arrival_date,\n"+
            "            srdd.travel_date,\n"+
            "            cd.distance_km,\n" +
            "            srdd.destination_status,\n"+
            "            e.id AS driver_id,\n"+
            "            CONCAT(e.name , ' ', e.last_name) AS driver,\n" +
            "            sr.id AS schedule_route_id\n" +
            "            FROM schedule_route AS sr   \n" +
            "               INNER JOIN schedule_route_destination AS srdd ON srdd.schedule_route_id = sr.id\n" +
            "               INNER JOIN schedule_route_driver AS srd ON srd.schedule_route_id = sr.id AND srd.terminal_origin_id = srdd.terminal_origin_id AND srd.terminal_destiny_id = srdd.terminal_destiny_id\n" +
            "               INNER JOIN config_destination AS cd ON cd.id = srdd.config_destination_id\n" +
            "               INNER JOIN employee AS e ON e.id = srd.employee_id\n" +
            "               INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id   \n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            WHERE sr.id = ? AND cd.config_route_id = ?   AND (COALESCE(cd.order_destiny, 0) - COALESCE(cd.order_origin, 0)) = 1   ";

    private static final String QUERY_GET_ORIGIN_DESTINY_BY_CONFIG_ID = "select \n" +
            "cr.terminal_origin_id as padre_terminal_origin_id,\n" +
            " cr.terminal_destiny_id as padre_terminal_destiny_id\n" +
            "from schedule_route sr\n" +
            "inner join config_route cr on sr.config_route_id = cr.id\n" +
            "where sr.id = ? ";

    private static final String QUERY_DESTINO_TIJUANA = " SELECT \n" +
            " srd.terminal_origin_id as hijo_terminal_origin_id,\n" +
            " srd.terminal_destiny_id as hijo_terminal_destiny_id,\n" +
            " cr.terminal_origin_id as padre_terminal_origin_id,\n" +
            " cr.terminal_destiny_id as padre_terminal_destiny_id,\n" +
            " bo.name as padre_terminal_origin_name,\n" +
            " po.name as hijo_terminal_origin_name,\n" +
            " pd.name as hijo_terminal_destiny_name,\n" +
            " bd.name as padre_terminal_destiny_name\n" +
            " FROM schedule_route_destination srd\n" +
            " INNER JOIN schedule_route sr ON srd.schedule_route_id = sr.id\n" +
            " INNER JOIN config_route cr ON sr.config_route_id = cr.id\n" +
            " LEFT JOIN branchoffice bo ON cr.terminal_origin_id = bo.id\n" +
            " LEFT JOIN branchoffice bd ON cr.terminal_destiny_id = bd.id\n" +
            " LEFT JOIN branchoffice po ON srd.terminal_origin_id = po.id\n" +
            " LEFT JOIN branchoffice pd ON srd.terminal_destiny_id = pd.id\n" +
            " WHERE srd.id = ? \n" +
            " AND sr.id = ? ";
//</editor-fold>
}
