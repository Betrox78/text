package database.prepaid;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.parcel.ParcelsPackagesDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import static service.commons.Constants.ACTION;
import static utils.UtilsDate.sdfDataBase;

public class PrepaidPackageDBV extends DBVerticle{

    public static  final String ACTION_PREPAID_REGISTER = "PrepaidPackageDBV.register";
    public static  final String ACTION_GET_PREPAID_LIST = "PrepaidPackageDBV.getPrepaidList";
    public static  final String ACTION_GET_LIST_BY_TERMINALS_DISTANCE = "PrepaidPackageDBV.getListByTerminalDistance";
    public static  final String ACTION_UPDATE_PREPAID_PACKAGE_STATUS = "PrepaidPackageDBV.updateStatus";
    public static  final String ACTION_GET_ONE_PACKAGE = "PrepaidPackageDBV.findOne";
    public static  final String ACTION_UPDATE_PACKAGE = "PrepaidPackageDBV.updatePackage";

    @Override
    public String getTableName() {
        return "prepaid_package_config";
    }

    @Override
    protected void onMessage(Message<JsonObject> message){
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_GET_PREPAID_LIST:
                this.getPrepaidList(message);
                break;
            case ACTION_UPDATE_PREPAID_PACKAGE_STATUS:
                this.updateStatus(message);
                break;
            case ACTION_GET_ONE_PACKAGE:
                this.findOne(message);
                break;
            case ACTION_PREPAID_REGISTER:
                this.register(message);
                break;
            case ACTION_UPDATE_PACKAGE:
                this.updatePackage(message);
                break;
            case ACTION_GET_LIST_BY_TERMINALS_DISTANCE:
                this.getListByTerminalDistance(message);
                break;
        }
    }

    public static final String NAME = "name";
    public static final String APPLY_WEB = "apply_web";
    public static final String APPLY_APP = "apply_app";
    public static final String APPLY_POS = "apply_pos";
    public static final String ID = "id";

    private void getListByTerminalDistance(Message<JsonObject> message) {
        try {
            JsonObject params = message.body();
            JsonArray paramsDistance = new JsonArray()
                    .add(params.getInteger("terminal_origin"))
                    .add(params.getInteger("terminal_destiny"))
                    .add(params.getInteger("terminal_destiny"))
                    .add(params.getInteger("terminal_origin"));

            this.dbClient.queryWithParams(ParcelsPackagesDBV.QUERY_DISTANCE_KM_BY_TERMINALS_ID, paramsDistance, reply -> {
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> resultsDistance = reply.result().getRows();
                    if(resultsDistance.isEmpty()){
                        message.reply(new JsonArray());
                    }
                    double distanceKm = resultsDistance.get(0).getDouble("distance_km");
                    JsonArray paramsList = new JsonArray()
                            .add(params.getInteger("status"))
                            .add(distanceKm);
                    this.dbClient.queryWithParams(QUERY_FILTER_PREPAID_LIST.concat(this.getTypeStr(params)), paramsList, replyList -> {
                        try{
                            if(replyList.failed()){
                                throw replyList.cause();
                            }
                            List<JsonObject> results = replyList.result().getRows();
                            if(results.isEmpty()){
                                message.reply(new JsonArray());
                            }
                            message.reply(new JsonArray(results));
                        }catch(Throwable t) {
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
                }catch(Throwable t) {
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private void getPrepaidList(Message<JsonObject> message){
        try {
            this.dbClient.query(QUERY_GET_PREPAID_LIST, reply -> {
                try{
                    if( reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()){
                        message.reply(new JsonArray());
                    }

                    List<CompletableFuture<JsonObject>> resultTasks = new ArrayList<>();

                    for (JsonObject result : results) {
                        resultTasks.add(getSegmentsById(result));
                    }

                    CompletableFuture.allOf(resultTasks.toArray(new CompletableFuture[resultTasks.size()])).whenComplete((ps,pt) -> {
                        try {
                            if(pt != null){
                                reportQueryError(message, pt.getCause());
                            } else {
                                message.reply(new JsonArray(results));
                            }
                        } catch (Exception e){
                            reportQueryError(message, e.getCause());
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

    private void updateStatus(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer status = body.getInteger("status");
        JsonArray params = new JsonArray()
                .add(status)
                .add(sdfDataBase(new Date()))
                .add(body.getInteger("updated_by"))
                .add(body.getInteger("id"));

        this.dbClient.queryWithParams(QUERY_UPDATE_STATUS_PREPAID_PACKAGE , params, reply -> {
            if(reply.succeeded()){

                message.reply(reply.succeeded());
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void register(Message<JsonObject> message){
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            this.register(conn, body).whenComplete((result, error) -> {
                try {
                    if (error != null){
                        throw error;
                    }

                    this.commit(conn, message, result);

                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject prepaid){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {

            JsonArray segments = (JsonArray) prepaid.remove ("segments");
            prepaid.remove("segments");
            final Integer createdBy = prepaid.getInteger("created_by");

            GenericQuery createPrepaid = this.generateGenericCreate(prepaid);

            conn.updateWithParams(createPrepaid.getQuery(), createPrepaid.getParams(), (AsyncResult<UpdateResult> prepaidReply) -> {
                try {
                    if (prepaidReply.failed()) {
                        throw prepaidReply.cause();
                    }
                    final int prepaidId = prepaidReply.result().getKeys().getInteger(0);
                    final int numSegments = segments.size();

                    List<CompletableFuture<JsonObject>> segmentsTasks = new ArrayList<>();

                    prepaid.put(ID, prepaidId);

                    for (int i = 0; i < numSegments; i++){
                        segmentsTasks.add(registerSegmentsInPrepaid(conn ,segments.getJsonObject(i) ,prepaidId, createdBy));
                    }

                    CompletableFuture.allOf(segmentsTasks.toArray(new CompletableFuture[numSegments])).whenComplete((ps, pt) -> {
                        try {
                            if(pt != null) {
                                throw pt;
                            }

                            future.complete(new JsonObject().put(ID, prepaidId));
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

        return future;
    }

    private CompletableFuture<JsonObject> registerSegmentsInPrepaid(SQLConnection conn, JsonObject segment,Integer prepaidId , Integer createdBy ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        if(!segment.containsKey("created_by")){
            segment.put("created_by", createdBy);
        }

        if(!segment.containsKey("prepaid_package_config_id")) {
            segment.put("prepaid_package_config_id",prepaidId);
        }

        String createSegments = this.generateGenericCreate("prepaid_package_segment", segment );
        conn.update(createSegments, (AsyncResult<UpdateResult> reply) -> {
            try {
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                future.complete(segment);
            } catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private void findOne(Message<JsonObject> message){
        try {
            Integer id = message.body().getInteger("id");
            JsonArray params = new JsonArray().add(id);

            this.dbClient.queryWithParams(QUERY_GET_ONE_PREPAID_PACKAGE , params , reply -> {
                try{
                    if( reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()){
                        throw new Exception("Prepaid packages not found");
                    }

                    List<CompletableFuture<JsonObject>> resultTasks = new ArrayList<>();
                    for (JsonObject result : results) {
                        resultTasks.add(getSegmentsById(result));
                    }
                    CompletableFuture.allOf(resultTasks.toArray(new CompletableFuture[resultTasks.size()])).whenComplete((ps,pt) -> {
                        try {
                            if(pt != null){
                                reportQueryError(message, pt.getCause());
                            } else {
                                message.reply(new JsonArray(results));
                            }
                        } catch (Exception e){
                            reportQueryError(message, e.getCause());
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

    private void updatePackage(Message<JsonObject> message) {
        try {
            this.startTransaction(message, conn -> {
                JsonObject body = message.body();
                JsonObject service = new JsonObject()
                        .put("id", body.getInteger("id"))
                        .put("tickets_quantity", body.getInteger("tickets_quantity"))
                        .put("money", body.getFloat("money"))
                        .put("max_km", body.getFloat("max_km"))
                        .put("apply_web", body.getInteger("apply_web"))
                        .put("apply_app", body.getInteger("apply_app"))
                        .put("apply_pos", body.getInteger("apply_pos"))
                        .put("name", body.getString("name"))
                        .put("updated_at", sdfDataBase(new Date()))
                        .put("updated_by", body.getInteger("updated_by"));

                GenericQuery gen = this.generateGenericUpdate("prepaid_package_config", service);
                conn.updateWithParams(gen.getQuery(), gen.getParams(), replyUpdate -> {
                    try {
                        if (replyUpdate.failed()) {
                            throw replyUpdate.cause();
                        } else {
                            List<CompletableFuture<JsonObject>> segmentsTasks = new ArrayList<>();
                            JsonArray segments = body.getJsonArray ("segments");
                            final int numSegments = segments.size();

                            conn.updateWithParams("DELETE from prepaid_package_segment where prepaid_package_config_id = ?", new JsonArray().add(body.getInteger("id")), replyDelSegment -> {
                                for (int i = 0; i < numSegments; i++){
                                    JsonObject segment = new JsonObject()
                                            .put("terminal_origin_id", segments.getJsonObject(i).getInteger("terminal_origin_id"))
                                            .put("terminal_destiny_id", segments.getJsonObject(i).getInteger("terminal_destiny_id"));

                                    segmentsTasks.add(registerSegmentsInPrepaid(conn ,segment ,body.getInteger("id"), body.getInteger("updated_by")));
                                }
                                CompletableFuture.allOf(segmentsTasks.toArray(new CompletableFuture[numSegments])).whenComplete((ps, pt) -> {
                                    try {
                                        if(pt != null) {
                                            throw pt;
                                        }
                                        this.commit(conn, message, new JsonObject().put("message", "Updated"));
                                        // message.reply(new JsonObject().put("message", "Updated"));
                                    } catch (Throwable t) {
                                        reportQueryError(message, t);
                                    }
                                });
                            });
                        }
                    } catch (Throwable e) {
                        reportQueryError(message, e);
                    }
                });
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private String getTypeStr(JsonObject body) {
        if(body.containsKey(APPLY_POS)) {
            return " AND " + APPLY_POS + " = 1";
        }
        if(body.containsKey(APPLY_WEB)) {
            return " AND " + APPLY_WEB + " = 1";
        }
        if(body.containsKey(APPLY_APP)) {
            return " AND " + APPLY_APP + " = 1";
        }
        return "";
    }
    public CompletableFuture<JsonObject> getSegmentsById( JsonObject prepaidPackage){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray params = new JsonArray().add(prepaidPackage.getInteger("id"));

        this.dbClient.queryWithParams(QUERY_GET_SEGMENT_BY_ID, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }

                List<JsonObject> resultsSegments = reply.result().getRows();
                if(!resultsSegments.isEmpty()){
                    prepaidPackage.put("segments", resultsSegments);
                } else {
                    prepaidPackage.put("segments", new JsonArray());
                }

                future.complete(prepaidPackage);
            } catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }
    private static final String QUERY_GET_PREPAID_LIST = "select\n" +
            "ppc.name,\n" +
            "ppc.id,\n" +
            "ppc.tickets_quantity,\n" +
            "ppc.money,\n" +
            "ppc.max_km,\n" +
            "ppc.apply_web,\n" +
            "ppc.apply_app,\n" +
            "ppc.apply_pos,\n" +
            "ppc.status\n" +
            "from prepaid_package_config as ppc";

    private static final String QUERY_FILTER_PREPAID_LIST = "select\n" +
            "ppc.name,\n" +
            "ppc.id,\n" +
            "ppc.tickets_quantity,\n" +
            "ppc.money,\n" +
            "ppc.max_km,\n" +
            "ppc.apply_web,\n" +
            "ppc.apply_app,\n" +
            "ppc.apply_pos,\n" +
            "ppc.status\n" +
            "from prepaid_package_config as ppc \n" +
            "where ppc.status = ? AND ppc.max_km >= ?";

    public static final String QUERY_GET_SEGMENT_BY_ID = "SELECT\n" +
            " pps.id,\n" +
            " pps.terminal_origin_id, \n" +
            " pps.terminal_destiny_id,\n" +
            " bo.prefix as 'prefix_origin',\n" +
            " bd.prefix as 'prefix_destiny',\n" +
            " bo.name as 'name_origin',\n" +
            " bd.name as 'name_destiny'\n" +
            "FROM prepaid_package_segment pps\n" +
            "LEFT JOIN branchoffice bo ON bo.id = pps.terminal_origin_id\n" +
            "LEFT JOIN branchoffice bd ON bd.id = pps.terminal_destiny_id\n" +
            "WHERE pps.prepaid_package_config_id = ? ";

    private static final String QUERY_GET_ONE_PREPAID_PACKAGE = "select * from prepaid_package_config where id = ?";

    private static final String QUERY_UPDATE_STATUS_PREPAID_PACKAGE = "update prepaid_package_config set status = ? , updated_at = ? ,updated_by = ? where id = ? ";
}
