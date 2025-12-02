package database.sellerSalesTarget;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static service.commons.Constants.CREATED_BY;
import static service.commons.Constants.ID;
import static utils.UtilsDate.sdfDataBase;

public class SellerSalesTargetDBV  extends DBVerticle {
    public static final String ACTION_GET_LIST = "SellerSalesTargetDBV.getList";
    public static final String ACTION_REGISTER = "SellerSalesTargetDBV.register";
    public static final String ACTION_UPDATE_SELLER_TARGET = "SellerSalesTargetDBV.updateSellerTarget";
    public static final String ACTION_DELETE_SELLER_TARGET = "SellerSalesTargetDBV.deleteSellerTarget";
    public static final String ACTION_REGISTER_SELLER = "SellerSalesTargetDBV.registerSellerTarget";


    public static  final String ACTION_FIND_ONE = "SellerSalesTargetDBV.findOne";
    public static final String SELLERS = "sellers";
    public static final String YEAR = "c_year";
    public static final String MONTH = "c_month";
    public static final Integer DELETED = 3;
    public static final String  SELLERS_SALES_TARGET_ID = "sellers_sales_target_id";
    public static final String  SALES_TARGET = "sales_target";

    public enum MONTHS {
        ENERO("enero"),
        FEBRERO("febrero"),
        MARZO("marzo"),
        ABRIL("abril"),
        MAYO("mayo"),
        JUNIO("junio"),
        JULIO("julio"),
        AGOSTO("agosto"),
        SEPTIEMBRE("septiembre"),
        OCTUBRE("octubre"),
        NOVIEMBRE("noviembre"),
        DICIEMBRE("diciembre");

        private final String name;

        MONTHS(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static MONTHS fromValue(String value) {
            try {
                int monthNumber = Integer.parseInt(value);
                if (monthNumber < 1 || monthNumber > values().length) {
                    throw new IllegalArgumentException("Invalid value for month: " + value);
                }
                return values()[monthNumber - 1];
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Month value is not valid format: " + value);
            }
        }

    }

    @Override
    public String getTableName() { return "sellers_sales_target"; }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case ACTION_GET_LIST:
                this.getList(message);
                break;
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_FIND_ONE:
                this.findOne(message);
                break;
            case ACTION_UPDATE_SELLER_TARGET:
                this.updateSellerTarget(message);
                break;
            case ACTION_DELETE_SELLER_TARGET:
                this.deleteSellerTarget(message);
                break;
            case ACTION_REGISTER_SELLER:
                this.registerSellerTarget(message);
                break;
        }
    }

    private void getList(Message<JsonObject> message) {
        JsonObject body = message.body();

        int limit = body.getInteger("limit");
        int page = body.getInteger("page");

        String QUERY = QUERY_SELLER_SALES_TARGET_SELECT;
        JsonArray params = new JsonArray();

        //GROUP BY clause
        QUERY = QUERY.concat(QUERY_SELLER_SALES_TARGET_GROUP_BY);

        //ORDER BY clause
        QUERY = QUERY.concat(QUERY_SELLER_SALES_TARGET_ORDER_BY);

        // LIMIT clause
        QUERY = QUERY.concat(QUERY_SELLER_SALES_TARGET_LIMIT);
        params.add(limit).add((page - 1) * limit);

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    JsonObject res = new JsonObject()
                            .put("count", 0)
                            .put("items", limit)
                            .put("results", result);
                    message.reply(res);
                }

                params.remove(params.size() -1);
                params.remove(params.size() -1);

                this.dbClient.queryWithParams(QUERY_SELLER_SALES_TARGET_SELECT_COUNT, params, replyCount -> {
                    try {
                        if (reply.failed()) {
                            throw replyCount.cause();
                        }
                        List<JsonObject> count = replyCount.result().getRows();
                        int countInTable = count.get(0).getInteger("count");
                        if(count.isEmpty()) {
                            JsonObject res = new JsonObject()
                                    .put("count", 0)
                                    .put("items", limit)
                                    .put("results", result);
                            message.reply(res);
                        }

                        JsonObject res = new JsonObject()
                                .put("count", countInTable)
                                .put("items", limit)
                                .put("results", result);

                        message.reply(res);

                    } catch (Throwable t) {
                        t.printStackTrace();
                        reportQueryError(message,t);
                    }
                });

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
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

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            MONTHS monthEnum = MONTHS.fromValue(body.getString(MONTH));
            String monthName = monthEnum.getName();
            conn.queryWithParams(QUERY_VERIFY_IF_TARGETS_EXIST, new JsonArray().add(body.getInteger(YEAR)).add(monthName), replyExist -> {
                try {
                    if (replyExist.failed()){
                        throw new Exception(replyExist.cause());
                    }
                    List<JsonObject> rows = replyExist.result().getRows();
                    if (rows.isEmpty()){
                        Integer createdBy = body.getInteger(CREATED_BY);
                        JsonArray sellers = body.getValue(SELLERS) != null ? (JsonArray) body.remove(SELLERS) : new JsonArray();

                        GenericQuery createTargets = this.generateGenericCreate(body);
                        conn.updateWithParams(createTargets.getQuery(), createTargets.getParams(), replyCreatePromo -> {
                            try {
                                if (replyCreatePromo.failed()){
                                    throw new Exception(replyCreatePromo.cause());
                                }

                                Integer sellerSalesTId = replyCreatePromo.result().getKeys().getInteger(0);
                                List<String> batch = new ArrayList<>();
                                sellers.forEach(s -> {
                                    JsonObject seller = (JsonObject) s;
                                    seller.put(SELLERS_SALES_TARGET_ID, sellerSalesTId)
                                            .put(CREATED_BY, createdBy);

                                    batch.add(this.generateGenericCreate("sellers_sales_target_details", seller));
                                });

                                conn.batch(batch, replyCreateRelation -> {
                                    try {
                                        if (replyCreateRelation.failed()){
                                            throw new Exception(replyCreateRelation.cause());
                                        }
                                        future.complete(new JsonObject().put(ID, sellerSalesTId));
                                    } catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    } else {
                        throw new Throwable("El periodo que se está intentando capturar ya existe");
                    }
                } catch(Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private void findOne(Message<JsonObject> message){
        try {
            Integer id = message.body().getInteger("id");
            JsonArray params = new JsonArray().add(id);

            this.dbClient.queryWithParams(QUERY_FIND_ONE_TARGET, params , reply -> {
                try{
                    if( reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()){
                        throw new Exception("Seller sales target not found");
                    }
                    JsonObject salesTarget = results.get(0);

                    this.dbClient.queryWithParams(QUERY_GET_DETAILS_BY_TARGET_ID, params, replyDetails -> {
                        try{
                            if(replyDetails.failed()){
                                throw reply.cause();
                            }
                            List<JsonObject> details = replyDetails.result().getRows();
                            if(results.isEmpty()){
                                throw new Exception("Seller targets not found");
                            }
                            salesTarget.put("sellers", details);
                            message.reply(salesTarget);
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

    private void updateSellerTarget(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Double salesTarget = Double.parseDouble(String.valueOf(body.getString("sales_target")));
            JsonArray params = new JsonArray()
                    .add(salesTarget)
                    .add(sdfDataBase(new Date()))
                    .add(body.getInteger("updated_by"))
                    .add(body.getInteger("id"));

            this.dbClient.queryWithParams(QUERY_UPDATE_SELLER_SALES_TARGET, params, reply -> {
                if (reply.succeeded()) {
                    message.reply(reply.succeeded());
                } else {
                    reportQueryError(message, reply.cause());
                }
            });
        }  catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void deleteSellerTarget(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray()
                .add(DELETED)
                .add(sdfDataBase(new Date()))
                .add(body.getInteger("updated_by"))
                .add(body.getInteger("id"));

        this.dbClient.queryWithParams(QUERY_DELETE_SELLER_SALES_TARGET, params, reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void registerSellerTarget(Message<JsonObject> message){
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            this.registerSellerTarget(conn, body).whenComplete((result, error) -> {
                try {
                    if (error != null){
                        throw error;
                    }

                    this.commit(conn, message, new JsonObject().put("id", result));
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<Integer> registerSellerTarget(SQLConnection conn, JsonObject body){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        GenericQuery create = this.generateGenericCreateSendTableName("sellers_sales_target_details", body);
        conn.updateWithParams(create.getQuery(), create.getParams(), replyCreate -> {
            try {
                if (replyCreate.failed()){
                    throw replyCreate.cause();
                }
                Integer id = replyCreate.result().getKeys().getInteger(0);
                future.complete(id);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final  String QUERY_SELLER_SALES_TARGET_SELECT = "SELECT \n" +
            "sst.*, \n" +
            "COUNT(ssd.id) as details_count \n" +
            "FROM sellers_sales_target sst \n" +
            "LEFT JOIN \n" +
            "sellers_sales_target_details ssd ON sst.id = ssd.sellers_sales_target_id";
    private static final String QUERY_SELLER_SALES_TARGET_GROUP_BY = " GROUP BY sst.id\n";
    private static final  String QUERY_SELLER_SALES_TARGET_ORDER_BY = "ORDER BY sst.created_at DESC";
    private static final String QUERY_SELLER_SALES_TARGET_LIMIT = "\n limit ? OFFSET ? ";
    private static final  String QUERY_SELLER_SALES_TARGET_SELECT_COUNT = "SELECT \n" +
            "COUNT(sst.id) as count \n" +
            "FROM sellers_sales_target sst \n";
    private static final String QUERY_VERIFY_IF_TARGETS_EXIST = "SELECT \n" +
            "sst.id \n" +
            "FROM sellers_sales_target sst \n" +
            "WHERE sst.c_year = ? AND sst.c_month = ?";
    private static final String QUERY_FIND_ONE_TARGET = "select * from sellers_sales_target where id = ?";
    private static final String QUERY_GET_DETAILS_BY_TARGET_ID = "SELECT \n" +
            "sstd.*, u.name as user_name\n" +
            "FROM sellers_sales_target_details sstd\n" +
            "LEFT JOIN users u ON u.id = sstd.user_id\n" +
            "WHERE sstd.sellers_sales_target_id = ? and sstd.status = 1";
    private static final String QUERY_UPDATE_SELLER_SALES_TARGET = "UPDATE sellers_sales_target_details SET sales_target = ?, updated_at = ?, updated_by = ? WHERE id = ?";

    private static final String QUERY_DELETE_SELLER_SALES_TARGET = "UPDATE sellers_sales_target_details SET status = ?, updated_at = ?, updated_by = ? WHERE id = ?";
}
