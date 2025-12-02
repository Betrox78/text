/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.branchoffices;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.products.ProductsDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;
import utils.UtilsGoogleMaps;
import utils.UtilsValidation;
import utils.UtilsValidation.PropertyValueException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.shipments.ShipmentsDBV.TERMINAL_ORIGIN_ID;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class BranchofficeDBV extends DBVerticle {

    public static final String RECEIVE_TRANSHIPMENTS = "receive_transhipments";

    public static final String REGISTER = "BranchofficeDBV.register";
    public static final String ACTION_REPORT = "BranchofficeDBV.report";
    public static final String ACTION_REPORT_SCHEDULES = "BranchofficeDBV.report.schedules";
    public static final String IS_ACTIVE_BRANCH = "BranchofficeDBV.isActive>Branch";
    public static final String ACTION_REPORT_SCHEDULES_PUBLIC= "BranchofficeDBV.report.schedulesPublic";
    public static final String ACTION_REPORT_BRANCHOFFICE = "BranchofficeDBV.reportBranchoficce";
    public static final String ACTION_REPORT_SCHEDULES_PUBLIC_WEB= "BranchofficeDBV.report.schedulesPublicWeb";
    public static final String ACTION_REPORT_BRANCHOFFICE_CORP = "BranchofficeDBV.ACTION_REPORT_BRANCHOFFICE_CORP";
    public static final String ACTION_REPORT_BRANCHOFFICE_CORP_SITES = "BranchofficeDBV.ACTION_REPORT_BRANCHOFFICE_CORP_SITES";
    public static final String ACTION_GET_TERMINALS = "BranchofficeDBV.getTerminals";
    public static final String ACTION_GET_DISTANCE_DURATION_BETWEEN_TERMINALS = "BranchofficeDBV.getDistanceDurationBetweenTerminals";
    public static final String ACTION_GET_TERMINALS_DISTANCE_AND_TIME = "BranchofficeDBV.getTerminalsDistanceAndTime";
    public static final String ACTION_CREATE_DISTANCE_AND_TIME = "BranchofficeDBV.createDistanceTime";
    public static final String ACTION_UPDATE_DISTANCE_AND_TIME = "BranchofficeDBV.updateDistanceTime";
    public static final String ACTION_GET_DISTANCE = "BranchofficeDBV.getDistance";
    public static final String ACTION_GET_TERMINALS_BY_DISTANCE = "BranchofficeDBV.getTerminalsByDistance";

    @Override
    public String getTableName() {
        return "branchoffice";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message); //To change body of generated methods, choose Tools | Templates.
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register(message);
                break;
            case ACTION_REPORT_SCHEDULES:
                this.reportSchedules(message);
                break;
            case ACTION_REPORT:
                this.report(message);
                break;
            case IS_ACTIVE_BRANCH:
                this.isActiveBranch(message);
                break;
            case ACTION_REPORT_SCHEDULES_PUBLIC:
                this.reportSchedulesPublic(message);
                break;
            case ACTION_REPORT_BRANCHOFFICE:
                this.reportBranchoficce(message);
                break;
            case ACTION_REPORT_SCHEDULES_PUBLIC_WEB:
                this.reportSchedulesPublicWeb(message);
                break;
            case ACTION_REPORT_BRANCHOFFICE_CORP:
                this.reportBranchofficeCorp(message);
                break;
            case ACTION_REPORT_BRANCHOFFICE_CORP_SITES:
                this.reportBranchofficesAndSites(message);
                break;
            case ACTION_GET_TERMINALS:
                this.getTerminals(message);
                break;
            case ACTION_GET_DISTANCE_DURATION_BETWEEN_TERMINALS:
                this.getDistanceDurationBetweenTerminals(message);
                break;
            case ACTION_GET_TERMINALS_DISTANCE_AND_TIME:
                this.getTerminalsDistanceAndTime(message);
                break;
            case ACTION_CREATE_DISTANCE_AND_TIME:
                this.createDistanceTime(message);
                break;
            case ACTION_UPDATE_DISTANCE_AND_TIME:
                this.updateDistanceTime(message);
                break;
            case ACTION_GET_DISTANCE:
                this.getDistance(message);
                break;
            case ACTION_GET_TERMINALS_BY_DISTANCE:
                this.getTerminalsByDistance(message);
                break;

        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                //insert into branchoffice
                JsonObject obj = message.body().copy();
                int createdBy = obj.getInteger(CREATED_BY);
                obj.remove("schedules"); //remove de schedules, that persist after the branchoffice, from the copy to dont alter the original message
                obj.remove("products");
                JsonArray distances = (JsonArray) obj.remove("distances");
                GenericQuery model = this.generateGenericCreate(obj);
                conn.updateWithParams(model.getQuery(), model.getParams(), result -> {
                    try {
                        if (result.failed()){
                            throw result.cause();
                        }
                        final int id = result.result().getKeys().getInteger(0); //id of the inserted element
                        createDistanceTimeBatch(conn, distances, id, createdBy).whenComplete((resultDistanceTime, errorDistanceTime) -> {
                            try {
                                if (errorDistanceTime != null) {
                                    throw errorDistanceTime;
                                }
                                if(message.body().getJsonArray("products") != null){
                                    JsonArray products = message.body().getJsonArray("products");
                                    ProductsDBV objProduct = new ProductsDBV();
                                    objProduct.createRelation(conn, "branchoffice", id, products).whenComplete((relationResult, error) ->{
                                        try {
                                            if (error != null){
                                                throw error;
                                            }
                                            insertSchedules(conn, message, id);
                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } else {
                                    insertSchedules(conn, message, id);
                                }
                            } catch (Throwable t) {
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

    private void insertSchedules(SQLConnection conn, Message<JsonObject> message, Integer id){
        //insert all the schedules with a batch
        try {
            JsonArray schedules = message.body().getJsonArray("schedules");
            List<String> queriesInsert = new ArrayList<>();
            for (int i = 0; i < schedules.size(); i++) {
                JsonObject schedule = schedules.getJsonObject(i);
                isValidCreateSchedule(schedule);
                schedule.put("branchoffice_id", id);
                schedule.put("created_by", message.body().getInteger("created_by"));
                queriesInsert.add(this.generateGenericCreate("branchoffice_schedule", schedule));
            }
            conn.batch(queriesInsert, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    this.commit(conn, message, new JsonObject().put("id", id));
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        } catch (PropertyValueException e) {
            e.printStackTrace();
            this.rollback(conn, e, message);
        }
    }

    private void report(Message<JsonObject> message) {
        Integer branchofficeId = message.body().getInteger("id");
        String QUERY_GET_BRANCH = "";
        if (branchofficeId != null){
            QUERY_GET_BRANCH = String.format(QUERY_BRANCHE_OFFICES_BY_ID, branchofficeId);
        } else {
            QUERY_GET_BRANCH = QUERY_BRANCHE_OFFICES;
        }
        this.dbClient.query(QUERY_GET_BRANCH, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                this.execGetProductsByBranch(new JsonArray(branches)).whenComplete((resutlBranches, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        message.reply(resutlBranches);
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
    private void reportBranchoffice(Message<JsonObject> message) {
        Integer branchofficeId = message.body().getInteger("id");
        String QUERY_GET_BRANCH = "";
        if (branchofficeId != null){
            QUERY_GET_BRANCH = String.format(QUERY_BRANCHE_OFFICES_BY_ID, branchofficeId);
        } else {
            QUERY_GET_BRANCH = QUERY_BRANCHE_OFFICES;
        }
        this.dbClient.query(QUERY_GET_BRANCH, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                this.execGetProductsByBranch(new JsonArray(branches)).whenComplete((resutlBranches, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        message.reply(resutlBranches);
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

    private CompletableFuture<JsonArray> execGetProductsByBranch(JsonArray branches){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(branches.stream()
                .map(rel -> getProductsByBranch((JsonObject) rel))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        future.complete(branches);
                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonObject> getProductsByBranch(JsonObject branch){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_RELATION_BRANCH_PRODUCTS, new JsonArray().add(branch.getInteger("id")), replyP ->{
            try {
                if (replyP.failed()){
                    throw replyP.cause();
                }
                List<JsonObject> productsRelation = replyP.result().getRows();
                branch.put("products", new JsonArray(productsRelation));
                future.complete(branch);
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void reportSchedules(Message<JsonObject> message) {
        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        this.dbClient.query(QUERY_BRANCHE_OFFICES_REPORT, f1.completer());
        this.dbClient.query(QUERY_BRANCHE_OFFICES_SCHEDULES, f2.completer());
        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> schedules = reply.result().<ResultSet>resultAt(1).getRows();
                for (JsonObject branch : branches) {
                    List<JsonObject> branchSchedules = schedules.stream()
                            .filter(o -> o.getInteger("branchoffice_id").equals(branch.getInteger("id")))
                            .collect(toList());
                    branch.put("schedules", new JsonArray(branchSchedules));
                }
                message.reply(new JsonArray(branches));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void isActiveBranch(Message<JsonObject> message) {
        Integer branchId = message.body().getInteger("id");
        JsonArray params = new JsonArray().add(branchId);
        this.dbClient.queryWithParams(QUERY_IS_ACTIVE_BRANCH, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()){
                    throw new Exception("Branchoffice_id: "+ branchId + " not active or not exists");
                }
                message.reply(result.get(0));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void isValidCreateSchedule(JsonObject body) throws UtilsValidation.PropertyValueException {
        UtilsValidation.isHour24AndNotNull(body, "hour_start");
        UtilsValidation.isHour24AndNotNull(body, "hour_end");
        UtilsValidation.isHour24(body, "break_start");
        UtilsValidation.isHour24(body, "break_end");
        UtilsValidation.isDate(body, "date_start");
        UtilsValidation.isDate(body, "date_end");
    }
    private void reportSchedulesPublic(Message<JsonObject> message) {
        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        this.dbClient.query(QUERY_BRANCHE_OFFICES_PUBLIC, f1.completer());
        this.dbClient.query(QUERY_BRANCHE_OFFICES_SCHEDULES_PUBLIC, f2.completer());
        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> schedules = reply.result().<ResultSet>resultAt(1).getRows();
                for (JsonObject branch : branches) {
                    List<JsonObject> branchSchedules = schedules.stream()
                            .filter(o -> o.getInteger("branchoffice_id").equals(branch.getInteger("id")))
                            .collect(toList());
                    branch.put("schedules", new JsonArray(branchSchedules));
                }
                message.reply(new JsonArray(branches));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private void reportBranchoficce(Message<JsonObject> message) {

        this.dbClient.query(QUERY_BRANCHE_OFFICES_PREFIX, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(reply.result().toJson());

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private void reportSchedulesPublicWeb(Message<JsonObject> message) {

        this.dbClient.query(QUERY_BRANCHE_OFFICES_SCHEDULES_PUBLIC_WEB,reply->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> shedules = reply.result().getRows();
                message.reply(new JsonArray(shedules));

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void reportBranchofficeCorp(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_TERMINALS_AND_CORP, reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                message.reply(new JsonArray(branches));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void reportBranchofficesAndSites(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_TERMINALS_AND_CORP_AND_TRANSHIPMENTS_SITES, reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                message.reply(new JsonArray(branches));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getTerminals(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_TERMINALS, reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                for (JsonObject branch : branches) {
                    tasks.add(getTerminalsReceivingOf(branch));
                }

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                    try {
                        if (err != null) {
                            throw err;
                        }
                        message.reply(new JsonArray(branches));
                    } catch (Throwable t) {
                        reportQueryError(message, t);
                    }
                });

            } catch (Throwable t) {
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<Boolean> getTerminalsReceivingOf(JsonObject branchoffice) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer branchofficeId = branchoffice.getInteger(ID);
        this.dbClient.queryWithParams(QUERY_GET_TERMINALS_RECEIVING, new JsonArray().add(branchofficeId), reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                branchoffice.put("virtuals", branches);
                future.complete(true);

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getDistanceDurationBetweenTerminals(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String origin = body.getString(ORIGIN);
            JsonArray destiniesParam = body.getJsonArray(DESTINIES);
            UtilsGoogleMaps utilsGoogleMaps = new UtilsGoogleMaps(geoApiContext);
            JsonArray destinies = utilsGoogleMaps.getDistanceAndTime(origin, destiniesParam);
            message.reply(new JsonObject()
                    .put(ORIGIN, origin)
                    .put(DESTINIES, destinies));
        } catch (Exception ex) {
            reportQueryError(message, ex);
        }
    }

    private void getTerminalsDistanceAndTime(Message<JsonObject> message) {
        JsonObject body = message.body();
        int terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
        JsonArray param = new JsonArray().add(terminalOriginId);
        dbClient.queryWithParams(QUERY_GET_TERMINALS_DISTANCE_AND_TIME_TRAVEL_INFO, param, reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                message.reply(new JsonArray(branches));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getDistance(Message<JsonObject> message) {
        JsonObject body = message.body();
        int terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
        int terminalDestinyId = body.getInteger(TERMINAL_DESTINY_ID);
        JsonArray params = new JsonArray()
                .add(terminalDestinyId).add(terminalOriginId);
        this.dbClient.queryWithParams(QUERY_GET_DISTANCE_BY_TERMINALS, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> distances = reply.result().getRows();
                if (distances.isEmpty()) {
                    throw new Exception("Distance not found");
                }

                message.reply(new JsonArray(distances));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void createDistanceTime(Message<JsonObject> message) {
        startTransaction(message, conn -> {
            JsonObject body = message.body();
            int createdBy = body.getInteger(CREATED_BY);
            try {
                JsonArray destinies = body.getJsonArray(DESTINIES);
                createDistanceTimeBatch(conn, destinies, null, createdBy).whenComplete((result, error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        this.commit(conn, message, new JsonObject().put("success", result));
                    } catch (Throwable t) {
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<Boolean> createDistanceTimeBatch(SQLConnection conn, JsonArray destinies, Integer branchOfficeId, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<GenericQuery> creates = destinies.stream().map(d -> {
                JsonObject destiny = (JsonObject) d;
                destiny.put(CREATED_BY, createdBy);
                if (Objects.nonNull(branchOfficeId)) {
                    destiny.put(TERMINAL_ORIGIN_ID, branchOfficeId);
                }
                return this.generateGenericCreateSendTableName("package_terminals_distance", destiny);
            }).collect(toList());
            List<JsonArray> params = creates.stream().map(GenericQuery::getParams).collect(toList());

            conn.batchWithParams(creates.get(0).getQuery(), params, reply -> {
                try {
                    if (reply.failed()) {
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

    private void updateDistanceTime(Message<JsonObject> message) {
        startTransaction(message, conn -> {
            JsonObject body = message.body();
            try {
                JsonArray destinies = body.getJsonArray(DESTINIES);
                List<GenericQuery> creates = destinies.stream().map(d -> {
                    JsonObject destiny = (JsonObject) d;
                    destiny.put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                    return this.generateGenericUpdate("package_terminals_distance", destiny);
                }).collect(toList());
                List<JsonArray> params = creates.stream().map(GenericQuery::getParams).collect(toList());

                conn.batchWithParams(creates.get(0).getQuery(), params, reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        this.commit(conn, message, new JsonObject().put("success", true));
                    } catch (Throwable t) {
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
                this.rollback(conn, t, message);
            }
        });
    }

    private void getTerminalsByDistance(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Double distanceKm = body.getDouble(_DISTANCE_KM);
            JsonArray params = new JsonArray()
                    .add(terminalId).add(terminalId)
                    .add(terminalId).add(distanceKm);

            this.dbClient.queryWithParams(QUERY_GET_TERMINALS_BY_DISTANCE, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_BRANCHE_OFFICES_SCHEDULES_PUBLIC_WEB
            = "SELECT  id, branchoffice_id, hour_start, hour_end, break_start, break_end, date_start, date_end, sun, mon, thu, wen, tue, fri, sat, status FROM branchoffice_schedule WHERE status = 1";
    private static final String QUERY_BRANCHE_OFFICES_REPORT
            = "SELECT * FROM branchoffice WHERE (status = 1 OR status = 2);";
    private static final String QUERY_BRANCHE_OFFICES
            = "SELECT * FROM branchoffice WHERE (status = 1 OR status = 2) AND (prefix!='GDL02' AND prefix!='GDL01' AND prefix!='LMM02' AND prefix!='TIJ02' AND prefix!='CUL02' AND prefix!='GML01');";
    private static final String QUERY_BRANCHE_OFFICES_BY_ID
            = "SELECT * FROM branchoffice WHERE id = %d AND (status = 1 OR status = 2) AND (prefix!='GML01' AND prefix!='TEC01' AND prefix!='LMM02' AND prefix!='CUL02');";
    private static final String QUERY_RELATION_BRANCH_PRODUCTS = "SELECT\n" +
            " p.id,\n" +
            " p.sku,\n" +
            " p.name,\n" +
            " p.description,\n" +
            " p.cost,\n" +
            " p.expires,\n" +
            " p.has_stock,\n" +
            " p.status \n" +
            "FROM products AS p \n" +
            "LEFT JOIN branchoffices_products AS bp ON p.id = bp.product_id \n" +
            "WHERE bp.branchoffice_id = ? AND p.status = 1;";
    private static final String QUERY_BRANCHE_OFFICES_SCHEDULES
            = "SELECT * FROM branchoffice_schedule WHERE status = 1 OR status = 2;";
    private static final String QUERY_IS_ACTIVE_BRANCH
            = "SELECT status, is_virtual FROM branchoffice WHERE id = ?;";
    private static final String QUERY_BRANCHE_OFFICES_PUBLIC
            = "SELECT b.id," +
            "b.prefix," +
            "b.name," +
            "b.address," +
            "b.branch_office_type," +
            "b.business_segment," +
            "b.address," +
            "b.latitude," +
            "b.longitude," +
            "s.name as state " +
            "FROM branchoffice AS b " +
            "LEFT JOIN state AS s ON s.id = b.state_id " +
            "WHERE b.status IN (1, 2) " +
            "AND b.prefix NOT IN ('GDL02', 'GDL01', 'LMM02', 'TIJ02', 'CUL02', 'GML01');";
    private static final String QUERY_BRANCHE_OFFICES_SCHEDULES_PUBLIC
            = "SELECT id, branchoffice_id,hour_start,hour_end,break_start,break_end FROM branchoffice_schedule WHERE status = 1 OR status = 2;";
    private static final String QUERY_BRANCHE_OFFICES_PREFIX
            = "SELECT id,prefix,name,city_id,zip_code FROM branchoffice WHERE (status = 1 OR status = 2) AND (prefix!='GML01' AND prefix!='CUL02' AND prefix!='LMM02');";
    private static final String QUERY_GET_TERMINALS_AND_CORP
            = "SELECT b.*, ci.name as city_name FROM \n" +
            "branchoffice b\n" +
            "LEFT JOIN city ci ON b.city_id = ci.id\n" +
            "WHERE branch_office_type in ('A','T') AND b.status = 1";
    private static final String QUERY_GET_TERMINALS_AND_CORP_AND_TRANSHIPMENTS_SITES
            = "(SELECT b.id, b.name, FALSE AS transhipments FROM branchoffice b\n" +
            "WHERE b.branch_office_type in ('A','T') AND b.status = 1)\n" +
            "UNION (\n" +
            "SELECT b.id, b.transhipment_site_name AS name, TRUE AS transhipments FROM branchoffice b\n" +
            "WHERE b.branch_office_type in ('A','T') \n" +
            "AND b.status = 1 AND b.transhipment_site_name IS NOT NULL);";

    private static final String QUERY_GET_TERMINALS = "SELECT\n" +
            "\tb.*,\n" +
            "    c.name city_name,\n" +
            "    s.name state_name\n" +
            "FROM branchoffice b\n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "INNER JOIN state s ON s.id = b.state_id\n" +
            "WHERE b.branch_office_type = 'T'\n" +
            "AND b.status = 1\n" +
            "ORDER BY b.state_id, b.name;";

    private static final String QUERY_GET_TERMINALS_RECEIVING = "SELECT\n" +
            "   b.*,\n" +
            "    c.name city_name,\n" +
            "    s.name state_name\n" +
            "FROM branchoffice_parcel_receiving_config bprc\n" +
            "INNER JOIN branchoffice b ON b.id = bprc.of_branchoffice_id\n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "INNER JOIN state s ON s.id = b.state_id\n" +
            "   AND bprc.status = 1\n" +
            "WHERE b.branch_office_type = 'T'\n" +
            "AND b.status = 1\n" +
            "AND bprc.receiving_branchoffice_id = ?\n" +
            "GROUP BY b.id\n" +
            "ORDER BY b.state_id, b.name";

    private static final String QUERY_GET_TERMINALS_DISTANCE_AND_TIME_TRAVEL_INFO = "SELECT \n" +
            "\tCOALESCE(ptd.id, \n" +
            "\t\t(SELECT ptd2.id FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id \n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS id,\n" +
            "    bo.id AS terminal_origin_id,\n" +
            "    bo.name AS origin_name,\n" +
            "    bo.prefix AS origin_prefix,\n" +
            "    co.name AS origin_city_name,\n" +
            "    bd.id AS terminal_destiny_id,\n" +
            "    bd.name AS destiny_name,\n" +
            "    bd.prefix AS destiny_prefix,\n" +
            "    cd.name AS destiny_city_name,\n" +
            "    IF(ptd.travel_time IS NOT NULL AND ptd.travel_time != '00:00', ptd.travel_time, \n" +
            "\t\t(SELECT ptd2.travel_time FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id \n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS travel_time,\n" +
            "\tIF(ptd.distance_km IS NOT NULL AND ptd.distance_km != 0, ptd.distance_km,\n" +
            "\t\t(SELECT ptd2.distance_km FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id \n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS distance_km,\n" +
            "\tIF(ptd.promise_time_ocu IS NOT NULL AND ptd.promise_time_ocu != '', ptd.promise_time_ocu, \n" +
            "\t\t(SELECT ptd2.promise_time_ocu FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id \n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS promise_time_ocu,\n" +
            "\tIF(ptd.promise_time_ead IS NOT NULL AND ptd.promise_time_ead != '', ptd.promise_time_ead, \n" +
            "\t\t(SELECT ptd2.promise_time_ead FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id \n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS promise_time_ead\n" +
            "FROM branchoffice bo\n" +
            "LEFT JOIN city co ON co.id = bo.city_id\n" +
            "LEFT JOIN branchoffice bd ON bd.status = 1\n" +
            "LEFT JOIN city cd ON cd.id = bd.city_id \n" +
            "LEFT JOIN package_terminals_distance ptd ON ptd.terminal_origin_id = bo.id\n" +
            "\tAND ptd.terminal_destiny_id = bd.id\n" +
            "WHERE bo.id = ?\n" +
            "AND bo.status = 1\n" +
            "AND bd.branch_office_type = 'T'\n" +
            "AND bo.id != bd.id;";
    private static final String QUERY_GET_DISTANCE_BY_TERMINALS = "SELECT \n" +
            "   IF(ptd.travel_time IS NOT NULL AND ptd.travel_time != '00:00', ptd.travel_time, \n" +
            "       (SELECT ptd2.travel_time FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id\n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS travel_time,\n" +
            "    IF(ptd.distance_km IS NOT NULL AND ptd.distance_km != 0, ptd.distance_km, \n" +
            "       (SELECT ptd2.distance_km FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id\n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS distance_km,\n" +
            "   IF(ptd.promise_time_ocu IS NOT NULL AND ptd.promise_time_ocu != '', ptd.promise_time_ocu,\n" +
            "       (SELECT ptd2.promise_time_ocu FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id\n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS promise_time_ocu,\n" +
            "   IF(ptd.promise_time_ead IS NOT NULL AND ptd.promise_time_ead != '', ptd.promise_time_ead,\n" +
            "       (SELECT ptd2.promise_time_ead FROM package_terminals_distance ptd2 \n" +
            "        WHERE ptd2.terminal_origin_id = bd.id\n" +
            "        AND ptd2.terminal_destiny_id = bo.id\n" +
            "        AND ptd2.status = 1)) AS promise_time_ead\n" +
            "FROM branchoffice bo\n" +
            "INNER JOIN branchoffice bd ON bd.id = ?\n" +
            "LEFT JOIN package_terminals_distance ptd ON ptd.terminal_origin_id = bo.id\n" +
            "\tAND ptd.terminal_destiny_id = bd.id AND ptd.status = 1\n" +
            "WHERE bo.id = ?;";

    private static final String QUERY_GET_TERMINALS_BY_DISTANCE = "SELECT\n" +
            "   b.*,\n" +
            "    c.name AS city_name,\n" +
            "    s.name AS street_name\n" +
            "FROM branchoffice b \n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "INNER JOIN street s ON s.id = b.street_id\n" +
            "INNER JOIN package_terminals_distance ptd ON ptd.terminal_destiny_id = b.id\n" +
            "   OR ptd.terminal_origin_id = b.id\n" +
            "WHERE b.branch_office_type = 'T'\n" +
            "   AND b.status = 1\n" +
            "   AND b.id != ?\n" +
            "   AND (ptd.terminal_origin_id = ? OR ptd.terminal_destiny_id = ?)\n" +
            "   AND ptd.distance_km <= ?\n" +
            "GROUP BY b.id;";
//</editor-fold>
}
