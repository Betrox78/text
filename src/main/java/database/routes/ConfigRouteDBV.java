/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.routes;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.boardingpass.BoardingPassDBV.TERMINAL_ORIGIN_ID;
import static database.routes.ConfigDestinationDBV.CONFIG_ROUTE_ID;
import static service.commons.Constants.*;

import utils.UtilsDate;
import utils.UtilsValidation;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigRouteDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "ConfigRouteDBV.register";
    public static final String ACTION_LIST_ROUTES = "ConfigRouteDBV.listRoutes";
    public static final String ACTION_ONE_ROUTE = "ConfigRouteDBV.oneRoute";
    public static final String ACTION_ROUTE_DESTINATIONS = "ConfigRouteDBV.routeDestinations";
    public static final String ACTION_ROUTE_DELETE = "ConfigRouteDBV.routeDelete";
    public static final String ACTION_ROUTE_RETURN = "ConfigRouteDBV.routeReturn";
    public static final String ACTION_EXPIRE_ROUTES = "ConfigRouteDBV.routeExpireRoutes";
    public static final String ACTION_GET_LIST_ROUTES_BY_BRANCHOFFICE_AND_DATE = "ConfigRouteDBV.listRoutesByBranchofficeAndDate";
    public static final String ACTION_GET_LIST_PARCEL_ROUTES = "ConfigRouteDBV.listParcelRoutes";
    public static final String ACTION_GET_LIST_SCHEDULED_PARCEL_ROUTES = "ConfigRouteDBV.getListScheduledParcelRoutes";

    @Override
    public String getTableName() {
        return "config_route";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_LIST_ROUTES:
                this.listRoutes(message);
                break;
            case ACTION_ONE_ROUTE:
                this.getOneRoute(message);
                break;
            case ACTION_ROUTE_DESTINATIONS:
                this.routeDestinations(message);
                break;
            case ACTION_ROUTE_DELETE:
                this.routeDelete(message);
                break;
            case ACTION_ROUTE_RETURN:
                this.routeReturn(message);
                break;
            case ACTION_EXPIRE_ROUTES:
                this.expireRoutes(message);
                break;
            case ACTION_GET_LIST_ROUTES_BY_BRANCHOFFICE_AND_DATE:
                this.listRoutesByBranchofficeAndDate(message);
                break;
            case ACTION_GET_LIST_PARCEL_ROUTES:
                this.listParcelRoutes(message);
                break;
            case ACTION_GET_LIST_SCHEDULED_PARCEL_ROUTES:
                this.getListScheduledParcelRoutes(message);
                break;
        }

    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            JsonObject obj = message.body().copy();
            obj.remove("destinations"); //remove the nested elements
            obj.remove("schedules");
            obj.remove("return");
            JsonArray destinations = message.body().getJsonArray("destinations");
            JsonArray destinationsUnique = getDestinationsUnique(destinations);

            GenericQuery gc = this.generateGenericCreate(obj);
            con.updateWithParams(gc.getQuery(), gc.getParams(), reply -> {
                if (reply.succeeded()) {
                    final int id = reply.result().getKeys().getInteger(0);
                    //insert destinations
                    try {
                        List<String> destinationsInserts = queriesInsertDestinations(destinationsUnique, id, message.body().getInteger("created_by"));
                        con.batch(destinationsInserts, insert -> {
                            if (insert.succeeded()) {
                                //insert schedules
                                JsonArray schedules = message.body().getJsonArray("schedules");
                                insertSchedules(con, schedules, id, message.body().getInteger("created_by"), 0, null, message, insert2 -> {
                                    if (insert2.succeeded()) {
                                        Boolean oneWay;
                                        try {
                                            oneWay = message.body().getBoolean("one_way");
                                            if (oneWay != null) { // it can be null because de default value in database
                                                if (!oneWay) { //if not one_way that means has to have return object
                                                    //persist the details destinations and details schedules for return
                                                    JsonObject returnRoute = message.body().getJsonObject("return");
                                                    if (returnRoute != null) {
                                                        JsonObject returnRouteAlone = returnRoute.copy();
                                                        returnRouteAlone.remove("destinations"); //remove the nested elements
                                                        returnRouteAlone.remove("schedules");
                                                        returnRouteAlone.put("created_by", message.body().getInteger("created_by"));
                                                        returnRouteAlone.put("from_id", id);
                                                        GenericQuery gcr = this.generateGenericCreate(returnRouteAlone);
                                                        con.updateWithParams(gcr.getQuery(), gcr.getParams(), returnInsert -> {
                                                            if (returnInsert.succeeded()) {
                                                                final int returnId = returnInsert.result().getKeys().getInteger(0);
                                                                //insert return destinations
                                                                JsonArray returnDestinations = returnRoute.getJsonArray("destinations");
                                                                try {
                                                                    List<String> returnDestinationsInserts = queriesInsertDestinations(returnDestinations, returnId, message.body().getInteger("created_by"));
                                                                    con.batch(returnDestinationsInserts, returnDest -> {
                                                                        if (returnDest.succeeded()) {
                                                                            //insert return schedules
                                                                            JsonArray returnSchedules = returnRoute.getJsonArray("schedules");
                                                                            insertSchedules(con, returnSchedules, returnId, message.body().getInteger("created_by"), 0,
                                                                                    schedules, message, returnScheduleIns -> {
                                                                                        if (returnScheduleIns.succeeded()) {
                                                                                            this.commit(con, message, new JsonObject().put("id", id));
                                                                                        } else {
                                                                                            this.rollback(con, returnScheduleIns.cause(), message);
                                                                                        }
                                                                                    });
                                                                        } else {
                                                                            this.rollback(con, returnDest.cause(), message);
                                                                        }
                                                                    });
                                                                } catch (PropertyValueException ex) {
                                                                    this.rollback(con, ex, message);
                                                                }
                                                            } else {
                                                                this.rollback(con, returnInsert.cause(), message);
                                                            }
                                                        });
                                                    } else {
                                                        this.commit(con, message, new JsonObject().put("id", id));
                                                    }
                                                } else {
                                                    this.commit(con, message, new JsonObject().put("id", id));
                                                }
                                            } else {
                                                this.commit(con, message, new JsonObject().put("id", id));
                                            }
                                        } catch (ClassCastException ex) {
                                            this.rollback(con, ex, message);
                                        }
                                    } else {
                                        this.rollback(con, insert2.cause(), message);
                                    }
                                });
                            } else {
                                this.rollback(con, insert.cause(), message);
                            }
                        });
                    } catch (PropertyValueException ex) {
                        this.rollback(con, ex, message);
                    }
                } else {
                    this.rollback(con, reply.cause(), message);
                }
            });

        });
    }

    private JsonArray getDestinationsUnique(JsonArray destinations){
        JsonArray destinationsUnique = destinations.copy();
        for(int i = 0; i < destinations.size(); i++){
            JsonObject destinationI = destinations.getJsonObject(i);
            Integer terminalOriginIdI = destinationI.getInteger(TERMINAL_ORIGIN_ID);
            Integer terminalDestinyIdI = destinationI.getInteger(TERMINAL_DESTINY_ID);
            int find = 0;
            if(terminalOriginIdI.equals(terminalDestinyIdI)){
                destinationsUnique.remove(destinationI);
            } else {
                JsonObject match = null;
                for(int j = 0; j < destinationsUnique.size(); j++){
                    JsonObject destinationJ = destinationsUnique.getJsonObject(j);
                    Integer terminalOriginIdJ = destinationJ.getInteger(TERMINAL_ORIGIN_ID);
                    Integer terminalDestinyIdJ = destinationJ.getInteger(TERMINAL_DESTINY_ID);
                    if (terminalOriginIdI.equals(terminalOriginIdJ) && terminalDestinyIdI.equals(terminalDestinyIdJ)){
                        find++;
                        if (find > 0) {
                            if(match == null){
                                match = destinationJ;
                            } else {
                                match = match.getInteger("order_origin") < destinationJ.getInteger("order_origin") ? match : destinationJ;
                            }
                        }
                    }
                }
                if (match != null && find == 2){
                    destinationsUnique.remove(match);
                }
            }
        }
        return destinationsUnique;
    }

    private List<String> queriesInsertDestinations(JsonArray destinations, final int configRouteId, final int createdBy) throws PropertyValueException {
        List<String> destinationsInserts = new ArrayList<>(destinations.size());
        for (int i = 0; i < destinations.size(); i++) {
            JsonObject destination = destinations.getJsonObject(i);
            UtilsValidation.isHour(destination, "travel_time");
            destination.put("config_route_id", configRouteId);
            destination.put("created_by", createdBy);
            destinationsInserts.add(this.generateGenericCreate("config_destination", destination));
        }
        return destinationsInserts;
    }

    private void listRoutes(Message<JsonObject> message) {
        this.dbClient.query(QUERY_LIST_ROUTES, reply -> {
            if (reply.succeeded()) {
                JsonArray routes = new JsonArray(reply.result().getRows());
                for(int i = 0; i < routes.size(); i++){
                    Integer num_routes = routes.getJsonObject(i).getInteger("schedule_routes");
                    routes.getJsonObject(i).put("has_routes", num_routes > 0 );
                }
                message.reply(routes);
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void getOneRoute(Message<JsonObject> message) {
        final int id = message.body().getInteger("id");
        JsonArray params = new JsonArray().add(id);
        this.dbClient.queryWithParams(QUERY_ONE_ROUTE, params, reply -> {
            if (reply.succeeded()) {
                List<JsonObject> rows = reply.result().getRows();
                if (rows.size() > 0) {
                    message.reply(rows.get(0));
                } else {
                    reportQueryError(message, new Throwable("Not found"));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void routeDestinations(Message<JsonObject> message) {
        final int id = message.body().getInteger("id");
        JsonArray params = new JsonArray().add(id);
        this.dbClient.queryWithParams(QUERY_ROUTE_DESTINATIONS, params, reply -> {
            if (reply.succeeded()) {
                message.reply(new JsonArray(reply.result().getRows()));
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void routeDelete(Message<JsonObject> message) {
        final int id = message.body().getInteger(CONFIG_ROUTE_ID);
        JsonArray params = new JsonArray().add(id).add(id);
        this.dbClient.updateWithParams(QUERY_DELETE_CONFIG_ROUTE, params, reply -> {
            if (reply.succeeded()) {
                message.reply(new JsonObject().put("success", true));
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void insertSchedules(SQLConnection conn, JsonArray schedules, final int configRouteId, final int createdBy, final int parentScheduleId, JsonArray exitSchedules, Message<JsonObject> message, Handler<AsyncResult<UpdateResult>> resultHandler) {
        int length = schedules.size();
        List<CompletableFuture<Integer>> tasks = new ArrayList<CompletableFuture<Integer>>();
        for (int i = 0; i < length; i++) {
            try {
                int exitScheduleId = 0;
                JsonObject schedule = schedules.getJsonObject(i);
                if (exitSchedules != null) {
                    Integer order = schedule.getInteger("order");
                    if (order == null) {
                        this.rollback(conn, new Throwable("Send order property for config schedule"), message);
                        return;
                    }
                    for (int j = 0, jlength = exitSchedules.size(); j < jlength; j++) {
                        JsonObject exitSchedule = exitSchedules.getJsonObject(j);
                        Integer exitOrder = exitSchedule.getInteger("order");
                        if (exitOrder == null) {
                            this.rollback(conn, new Throwable("Send order property for config schedule"), message);
                            return;
                        }
                        if (order == exitOrder) {
                            exitScheduleId = exitSchedule.getInteger("id");
                            break;
                        }
                    }
                }
                tasks.add(insertScheduleAndChildren(conn, schedule, configRouteId, createdBy, parentScheduleId, exitScheduleId, message));
            } catch (PropertyValueException ex) {
                this.rollback(conn, ex, message);
                return;
            } catch (NullPointerException e) {
                System.out.println("ERROR: " + e);
                this.rollback(conn, e, message);
                return;
            }

        }
        CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[length]));
        all.whenComplete((s, t) -> {
            resultHandler.handle(new AsyncResult<UpdateResult>() {
                @Override
                public UpdateResult result() {
                    return new UpdateResult();
                }

                @Override
                public Throwable cause() {
                    return t;
                }

                @Override
                public boolean succeeded() {
                    return t == null;
                }

                @Override
                public boolean failed() {
                    return t != null;
                }
            });
        });
    }

    private CompletableFuture<Integer> insertScheduleAndChildren(SQLConnection conn, JsonObject schedule, final int configRouteId, final int createdBy, final int parentScheduleId, final int returnScheduleId, Message<JsonObject> message) throws PropertyValueException {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        JsonArray children = schedule.getJsonArray("children");
        Integer orderSchedule = schedule.getInteger("order");
        schedule.remove("children");
        schedule.remove("order");
        UtilsValidation.isHour24(schedule, "travel_hour");
        schedule.put("config_route_id", configRouteId);
        schedule.put("created_by", createdBy);
        if (parentScheduleId > 0) {
            schedule.put("config_schedule_origin_id", parentScheduleId);
        }
        if (returnScheduleId > 0) {
            schedule.put("from_id", returnScheduleId);
        }
        String insert = this.generateGenericCreate("config_schedule", schedule);
        schedule.put("order", orderSchedule);
        conn.update(insert, insertReply -> {
            if (insertReply.succeeded()) {
                final int id = insertReply.result().getKeys().getInteger(0);
                schedule.put("id", id);
                if (children == null) {
                    future.complete(id);
                } else {
                    insertSchedules(conn, children, configRouteId, createdBy, id, null, message, childrenReply -> {
                        if (childrenReply.succeeded()) {
                            future.complete(id);
                        } else {
                            future.completeExceptionally(childrenReply.cause());
                        }
                    });
                }
            } else {
                future.completeExceptionally(insertReply.cause());
            }

        });
        return future;
    }

    private void routeReturn(Message<JsonObject> message) {
        final int id = message.body().getInteger("id");
        JsonArray params = new JsonArray().add(id);
        this.dbClient.queryWithParams(QUERY_ROUTE_RETURN, params, reply -> {
            if (reply.succeeded()) {
                message.reply(new JsonArray(reply.result().getRows()));
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void expireRoutes(Message<JsonObject> message) {
        this.dbClient.update(QUERY_EXPIRE_ROUTES, reply -> {
            if (reply.succeeded()) {
                message.reply(
                        new JsonObject().put("updated", reply.result().getUpdated())
                );
            } else {
                message.fail(0, reply.cause().getMessage());
            }
        });
    }

    private void listRoutesByBranchofficeAndDate(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer branchofficeId = body.getInteger("branchoffice_id");
            String date = body.getString("date");
            String type = body.getString("type");
            boolean allDay = body.getBoolean("all_day");
            JsonArray params = new JsonArray().add(branchofficeId).add(date);
            String QUERY = "";

            if(type.equals("load")){
                QUERY = QUERY_GET_LIST_BY_BRANCHOFFICE_AND_DATE_LOAD;
                QUERY += allDay ? " AND DATE(srd.travel_date) = DATE(?) " : COMPLEMENT_GET_LIST_BY_BRANCHOFFICE_AND_DATE_GENERAL_SETTING_LOAD;
            } else {
                QUERY = QUERY_GET_LIST_BY_BRANCHOFFICE_AND_DATE_DOWNLOAD;
                QUERY += allDay ? " AND DATE(srd.arrival_date) = DATE(?) " : COMPLEMENT_GET_LIST_BY_BRANCHOFFICE_AND_DATE_GENERAL_SETTING_DOWNLOAD;
            }
            QUERY += " GROUP BY cr.id ";

            this.dbClient.queryWithParams(QUERY, params,reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> configRoutes = reply.result().getRows();

                    List<CompletableFuture<JsonArray>> originsTasks = new ArrayList<>();
                    for(int i = 0; i < configRoutes.size(); i++){
                        JsonObject configRoute = configRoutes.get(i);
                        originsTasks.add(this.getOriginsByConfigRouteAndDate(configRoute, date));
                    }

                    CompletableFuture.allOf(originsTasks.toArray(new CompletableFuture[originsTasks.size()])).whenComplete((result, error)-> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonArray(configRoutes));
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

    private CompletableFuture<JsonArray> getOriginsByConfigRouteAndDate(JsonObject configRoute, String date){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer configRouteId = configRoute.getInteger(CONFIG_ROUTE_ID);
        JsonArray params = new JsonArray().add(configRouteId).add(date);
        this.dbClient.queryWithParams(QUERY_GET_LIST_ORIGINS_BY_CONFIG_ROUTE_AND_DATE, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                configRoute.put("origins", new JsonArray(result));
                future.complete(new JsonArray(result));
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void listParcelRoutes(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String date = body.getString(DATE);
            Integer terminalId = body.getInteger(TERMINAL_ID);
            boolean flagScheduled = body.getBoolean("flag_scheduled");
            boolean flagLoad = body.getString("type").equals("load");
            Integer parcelRoutesTimeBeforeTravelDate = body.getInteger("parcel_routes_time_before_travel_date");

            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            Date parcelDate1 = format1.parse(date);
            calendar.setTime(parcelDate1);
            calendar.add(Calendar.MINUTE,(parcelRoutesTimeBeforeTravelDate.equals(1) ? 60 : parcelRoutesTimeBeforeTravelDate) * -1);
            String parcelDate = format1.format(calendar.getTime());
            calendar.setTime(parcelDate1);
            calendar.add(Calendar.MINUTE, parcelRoutesTimeBeforeTravelDate.equals(1) ? 360 : parcelRoutesTimeBeforeTravelDate);
            String parcelDate2 = format1.format(calendar.getTime());

            JsonArray params = new JsonArray();
            String QUERY;

            if (flagScheduled){
                QUERY = QUERY_GET_PARCEL_SCHEDULE_ROUTES.replace("{TERMINAL_PARAM}",
                        flagLoad ? QUERY_GET_PARCEL_SCHEDULE_ROUTES_LOAD_COMPLEMENT_QUERY
                                : QUERY_GET_PARCEL_SCHEDULE_ROUTES_DOWNLOAD_COMPLEMENT_QUERY);
                params.add(parcelDate).add(parcelDate2);
            } else {
                QUERY = QUERY_GET_PARCEL_ROUTES;
            }
            params.add(terminalId);

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> configRoutes = reply.result().getRows();
                    message.reply(new JsonArray(configRoutes));

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

    private void getListScheduledParcelRoutes(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String QUERY = QUERY_GET_SCHEDULED_PARCEL_ROUTES;
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            String type = body.getString(_TYPE);
            JsonArray params = new JsonArray()
                    .add(initDate).add(endDate);
            switch (type) {
                case "arrivals":
                    QUERY = QUERY.concat("AND srd.terminal_destiny_id = ? \n");
                    params.add(terminalId);
                    break;
                case "departures":
                    QUERY = QUERY.concat("AND srd.terminal_origin_id = ? \n");
                    params.add(terminalId);
                    break;
                case "both":
                    QUERY = QUERY.concat("AND (srd.terminal_origin_id = ? OR srd.terminal_destiny_id = ?) \n");
                    params.add(terminalId).add(terminalId);
                    break;
            }
            QUERY = QUERY.concat("GROUP BY cr.id;");

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> configRoutes = reply.result().getRows();
                    message.reply(new JsonArray(configRoutes));

                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_LIST_ROUTES = "SELECT " +
            "cr.id, IF(cr.parcel_route, CONCAT('PAQUETERIA - ', cr.name), cr.name) AS name, cr.description, cr.status, cr.travel_time, cr.type_travel, cr.one_way, bo.id AS terminal_origin_id, bo.prefix AS terminal_origin_prefix, " +
            "bo.name AS terminal_origin_name, bd.id AS terminal_destiny_id, bd.prefix AS terminal_destiny_prefix, bd.name AS terminal_destiny_name, \n" +
            "( SELECT COUNT( cd.id ) FROM config_destination cd WHERE cd.config_route_id = cr.id AND cd.status = 1 ) AS total_destinies, \n" +
            "( SELECT COUNT(crr.id) " +
            "   FROM config_route crr " +
            "   LEFT JOIN config_destination cdr ON crr.id = cdr.config_route_id " +
            "   LEFT JOIN config_ticket_price ctpr ON cdr.id=ctpr.config_destination_id " +
            "   WHERE (crr.id=cr.id or crr.from_id=cr.id)  and ctpr.special_ticket_id IS NULL) AS prices_not_defined, \n" +
            " (SELECT COUNT(id) FROM schedule_route sr WHERE sr.travel_date > DATE(CURRENT_DATE()) AND sr.config_route_id=cr.id) AS schedule_routes, \n" +
            " cr.parcel_route \n" +
            "FROM config_route cr " +
            "JOIN branchoffice bo ON bo.id = terminal_origin_id " +
            "JOIN branchoffice bd ON bd.id = terminal_destiny_id " +
            "WHERE cr.status != 3 AND cr.from_id IS NULL";

    private static final String QUERY_ONE_ROUTE = QUERY_LIST_ROUTES + " AND cr.id = ? ";

    private static final String QUERY_DELETE_CONFIG_ROUTE = "update config_route cr\n" +
            "left join (select icr.id, isr.travel_date from config_route icr\n" +
            "inner join schedule_route isr on isr.config_route_id = icr.id\n" +
            "where icr.id = ? AND isr.status = 1\n" +
            "order by isr.travel_date desc\n" +
            "limit 1) as ltd on ltd.id = cr.id\n" +
            "SET cr.expire_at = if(ltd.travel_date is null, now(), DATE_ADD(ltd.travel_date, INTERVAL 1 DAY)),\n" +
            "\tcr.status = if(ltd.travel_date is null, 3, 0)\n" +
            "where cr.id = ?";

    private static final String QUERY_ROUTE_DESTINATIONS = "SELECT	\n"
            + "	/*destinies*/\n"
            + "	cd.*,\n"
            + "	bod.id AS destination_terminal_origin_id,\n"
            + "	bod.prefix AS destination_terminal_origin_prefix,\n"
            + "	bod.name AS destination_terminal_origin_name,\n"
            + "	bod.time_checkpoint AS destination_terminal_origin_time_checkpoint,\n"
            + "	bod.time_zone AS destination_terminal_origin_time_zone,\n"
            + "	bdd.id AS destination_terminal_destiny_id,\n"
            + "	bdd.prefix AS destination_terminal_destiny_prefix,\n"
            + "	bdd.name AS destination_terminal_destiny_name,\n"
            + "	bdd.time_checkpoint AS destination_terminal_destiny_time_checkpoint,\n"
            + "	bdd.time_zone AS destination_terminal_destiny_time_zone\n"
            + "FROM config_destination cd\n"
            + "JOIN branchoffice bdd ON\n"
            + "	bdd.id = cd.terminal_destiny_id\n"
            + "JOIN branchoffice bod ON\n"
            + "	bod.id = cd.terminal_origin_id\n"
            + "WHERE\n"
            + "	cd.status = 1\n"
            + "	AND \n"
            + "	cd.config_route_id = ?";

    private static final String QUERY_ROUTE_RETURN = "SELECT\n"
            + "	cr.id,\n"
            + "	cr.name,\n"
            + "	cr.description,\n"
            + "	cr.status,\n"
            + "	cr.travel_time,\n"
            + "	cr.type_travel,	\n"
            + "	cr.one_way,	\n"
            + "	bo.id AS terminal_origin_id,\n"
            + "	bo.prefix AS terminal_origin_prefix,\n"
            + "	bo.name AS terminal_origin_name,\n"
            + "	bd.id AS terminal_destiny_id,\n"
            + "	bd.prefix AS terminal_destiny_prefix,\n"
            + "	bd.name AS terminal_destiny_name,\n"
            + "	( SELECT\n"
            + "		COUNT( cd.id )\n"
            + "	FROM\n"
            + "		config_destination cd\n"
            + "	WHERE\n"
            + "		cd.config_route_id = cr.id\n"
            + "		AND cd.status = 1 ) AS total_destinies\n"
            + "FROM\n"
            + "	config_route cr\n"
            + "JOIN branchoffice bo ON\n"
            + "	bo.id = terminal_origin_id\n"
            + "JOIN branchoffice bd ON\n"
            + "	bd.id = terminal_destiny_id\n"
            + "	WHERE cr.status != 3\n"
            + "	AND cr.from_id = ?";

    private static final String QUERY_EXPIRE_ROUTES = "UPDATE\n"
            + "	config_route\n"
            + "SET\n"
            + "	status = 3\n"
            + "WHERE\n"
            + "	expire_at < NOW()\n"
            + "	AND status != 3";

    private static final String QUERY_GET_LIST_BY_BRANCHOFFICE_AND_DATE_LOAD = "SELECT DISTINCT\n" +
            "   cr.id AS config_route_id,\n" +
            "   CONCAT(" +
            "v.economic_number, \n" +
            "   ' ESCALA ', bosrd.prefix, '-', bdsrd.prefix, '(', DATE_FORMAT(srd.travel_date, \"%H:%i\") , ' HRS)') AS name, \n" +
            "   srd.terminal_origin_id,\n" +
            "   srd.terminal_destiny_id\n" +
            " FROM config_route cr\n" +
            " LEFT JOIN schedule_route sr ON sr.config_route_id = cr.id\n" +
            " INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            " LEFT JOIN config_destination cd ON cd.config_route_id = cr.id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id \n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN branchoffice bosrd ON bosrd.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bdsrd ON bdsrd.id = srd.terminal_destiny_id\n" +
            " WHERE cr.status = 1 AND cd.status = 1 AND cr.parcel_route IS FALSE\n " +
            " AND srd.terminal_origin_id = ? \n" +
            " AND ((sr.schedule_status = 'scheduled' AND cd.order_origin = 1) \n" +
            "   OR srd.destination_status IN ('loading', 'ready-to-go', 'ready-to-load'))";

    private static final String COMPLEMENT_GET_LIST_BY_BRANCHOFFICE_AND_DATE_GENERAL_SETTING_LOAD =
            " AND ? BETWEEN DATE_SUB(srd.travel_date, \n" +
            " INTERVAL \n" +
            " (SELECT value FROM general_setting WHERE FIELD = 'routes_time_before_travel_date_pos')\n" +
            " MINUTE) AND \n" +
            " DATE_ADD(srd.travel_date, \n" +
            " INTERVAL (SELECT value FROM general_setting WHERE FIELD = 'routes_time_before_travel_date_pos')\n" +
            " MINUTE) \n";

    private static final String QUERY_GET_LIST_BY_BRANCHOFFICE_AND_DATE_DOWNLOAD = "SELECT DISTINCT\n" +
            "   cr.id AS config_route_id,\n" +
            "   CONCAT(" +
            "v.economic_number, \n" +
            "   ' ESCALA ', bosrd.prefix, '-', bdsrd.prefix, '(', DATE_FORMAT(srd.arrival_date, \"%H:%i\") , ' HRS)') AS name, \n" +
            "   srd.terminal_origin_id,\n" +
            "   srd.terminal_destiny_id\n" +
            " FROM config_route cr\n" +
            " LEFT JOIN schedule_route sr ON sr.config_route_id = cr.id\n" +
            " INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            " LEFT JOIN config_destination cd ON cd.config_route_id = cr.id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id \n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN branchoffice bosrd ON bosrd.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bdsrd ON bdsrd.id = srd.terminal_destiny_id\n" +
            " WHERE cr.status = 1 AND cd.status = 1 AND cr.parcel_route IS FALSE \n" +
            " AND srd.terminal_destiny_id = ?\n" +
            " AND (srd.destination_status IN ('in-transit', 'stopped', 'downloading')) \n" +
            " AND cd.order_destiny = cd.order_origin + 1 \n";

    private static final String COMPLEMENT_GET_LIST_BY_BRANCHOFFICE_AND_DATE_GENERAL_SETTING_DOWNLOAD =
            " AND ? BETWEEN DATE_SUB(srd.arrival_date, \n" +
            " INTERVAL \n" +
            " (SELECT value FROM general_setting WHERE FIELD = 'routes_time_before_travel_date_pos')\n" +
            " MINUTE) AND \n" +
            " DATE_ADD(srd.arrival_date, \n" +
            " INTERVAL (SELECT value FROM general_setting WHERE FIELD = 'routes_time_before_travel_date_pos')\n" +
            " MINUTE) \n";

    private static final String QUERY_GET_LIST_ORIGINS_BY_CONFIG_ROUTE_AND_DATE = "SELECT DISTINCT\n" +
            "   cd.terminal_origin_id,\n" +
            "   bo.prefix AS terminal_origin_prefix,\n" +
            "   cd.terminal_destiny_id,\n" +
            "   bd.prefix AS terminal_destiny_prefix\n" +
            " FROM config_destination cd\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.config_destination_id = cd.id\n" +
            " LEFT JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            " LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cd.terminal_destiny_id\n" +
            " WHERE cr.id = ?\n" +
            " AND cd.order_destiny = cd.order_origin + 1\n" +
            " AND DATE(srd.travel_date) = ?;";

    private static final String QUERY_GET_PARCEL_ROUTES = "SELECT\n" +
            "   cs.id AS config_schedule_id, cs.travel_hour AS config_schedule_travel_hour,\n" +
            "   cr.id, cr.name, cr.description, cr.terminal_origin_id, cr.terminal_destiny_id, cr.travel_time, cr.type_travel, cr.one_way, cr.parcel_route\n" +
            "FROM config_route cr\n" +
            "INNER JOIN config_schedule cs ON cr.id = cs.config_route_id\n" +
            "   AND cs.terminal_origin_id = cr.terminal_origin_id\n" +
            "INNER JOIN config_destination cd ON cd.config_route_id = cr.id AND cd.status = 1\n" +
            "WHERE cr.status = 1 AND cr.parcel_route IS TRUE AND cr.terminal_origin_id = ?\n" +
            "GROUP BY cr.id, cr.name, cr.description, cr.terminal_origin_id, cr.terminal_destiny_id, cr.travel_time, cr.type_travel, cr.one_way, cr.parcel_route, cs.id, cs.travel_hour";

    private static final String QUERY_GET_PARCEL_SCHEDULE_ROUTES = "SELECT\n" +
            "   cs.id AS config_schedule_id, \n" +
            "   v.id AS vehicle_id, \n" +
            "   CONCAT(v.economic_number, \n" +
            "   ' ESCALA ', bosrd.prefix, '-', bdsrd.prefix, '(', DATE_FORMAT(srd.travel_date, \"%H:%i\") , ' HRS)') AS name, \n" +
            "   srd.terminal_origin_id,\n" +
            "   srd.terminal_destiny_id\n" +
            "FROM config_route cr\n" +
            "INNER JOIN config_schedule cs ON cr.id = cs.config_route_id\n" +
            "   AND cs.terminal_origin_id = cr.terminal_origin_id\n" +
            " INNER JOIN schedule_route sr ON sr.config_route_id = cr.id \n" +
            " INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            " INNER JOIN config_destination cd ON cd.config_route_id = cr.id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN branchoffice bosrd ON bosrd.id = cd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bdsrd ON bdsrd.id = cd.terminal_destiny_id\n" +
            "WHERE cr.status = 1 AND cd.status = 1 \n" +
            "AND cr.parcel_route IS TRUE \n" +
            "{TERMINAL_PARAM}\n" +
            "GROUP BY cs.id";

    private static final String QUERY_GET_PARCEL_SCHEDULE_ROUTES_LOAD_COMPLEMENT_QUERY = "AND DATE(CONVERT_TZ(srd.travel_date, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')) BETWEEN ? AND ? \n" +
            "AND srd.terminal_origin_id = ?\n" +
            "AND ((sr.schedule_status = 'scheduled' AND cd.order_origin = 1) \n" +
            "\tOR srd.destination_status IN ('loading', 'ready-to-go', 'ready-to-load')) \n";

    private static final String QUERY_GET_PARCEL_SCHEDULE_ROUTES_DOWNLOAD_COMPLEMENT_QUERY = "AND DATE(CONVERT_TZ(srd.arrival_date, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')) BETWEEN ? AND ? \n" +
            "AND srd.terminal_destiny_id = ?\n" +
            "AND (srd.destination_status IN ('in-transit', 'stopped', 'downloading')) \n";

    private static final String QUERY_GET_SCHEDULED_PARCEL_ROUTES = "SELECT \n" +
            "   cr.id,\n" +
            "   cr.name,\n" +
            "    cd.order_origin,\n" +
            "    srd.terminal_origin_id,\n" +
            "    bo.prefix AS prefix_origin,\n" +
            "    cd.order_destiny,\n" +
            "    srd.terminal_destiny_id,\n" +
            "    bd.prefix AS prefix_destiny\n" +
            "FROM schedule_route sr\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " AND cr.parcel_route IS TRUE\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "WHERE \n" +
            " cd.order_destiny = cd.order_origin + 1\n" +
            " AND DATE(srd.travel_date) BETWEEN ? AND ? \n";

//</editor-fold>

}
