package database.products;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import static service.commons.Constants.ACTION;

public class ProductsDBV extends DBVerticle {

    // Actions
    public static final String REGISTER = "ProductsDBV.register";
    public static final String GENERIC_REGISTER = "ProductsDBV.genericRegister";
    public static final String ASSOCIATE_PRODUCTS = "ProductsDBV.associateProducts";
    public static final String ASSOCIATE_BRANCHOFFICES = "ProductsDBV.associateBranchoffices";
    public static final String REPORT = "ProductsDBV.report";

    @Override
    public String getTableName() {
        return "products";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register(message);
                break;
            case GENERIC_REGISTER:
                this.genericRegister(message);
                break;
            case ASSOCIATE_PRODUCTS:
                this.associateProducts(message);
                break;
            case ASSOCIATE_BRANCHOFFICES:
                this.associateBranchoffices(message);
                break;
            case REPORT:
                this.report(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body().copy();
            String sku = body.getString("sku");
            body.remove("branchoffices");

            conn.queryWithParams(QUERY_CHECK_SKU_PRODUCT, new JsonArray().add(sku), replyCheck ->{
                try{
                    if(replyCheck.failed()) {
                        throw new Exception(replyCheck.cause());
                    }
                    if(replyCheck.result().getRows().get(0).getInteger("quantity").equals(0)){
                        GenericQuery model = this.generateGenericCreate(body);
                        conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {

                            try{
                                if(reply.failed()){
                                    throw  new Exception(reply.cause());
                                }

                                Integer productId = reply.result().getKeys().getInteger(0);
                                if(message.body().getValue("branchoffices") != null){
                                    JsonArray branchoffices = (JsonArray) message.body().getValue("branchoffices");
                                    this.createRelation(conn, "product", productId, branchoffices).whenComplete((result, error) -> {
                                        if(error != null){
                                            this.rollback(conn, error, message);
                                        } else {
                                            this.commit(conn, message, new JsonObject().put("id", productId));
                                        }
                                    });
                                } else {
                                    this.commit(conn, message, new JsonObject().put("id", productId));
                                }
                            }catch(Exception e){
                                this.rollback(conn, reply.cause(), message);

                            }

                        });
                    } else {
                        this.rollback(conn, new Throwable("The sku entered has already been registered"), message);
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, replyCheck.cause(), message);
                }
            });
        });
    }

    private void genericRegister(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            String sku = body.getString("sku");

            conn.queryWithParams(QUERY_CHECK_SKU_PRODUCT, new JsonArray().add(sku), replyCheck ->{
                try{
                    if(replyCheck.failed()) {
                        throw new Exception(replyCheck.cause());
                    }
                    if(replyCheck.result().getRows().get(0).getInteger("quantity").equals(0)){
                        GenericQuery model = this.generateGenericCreate(body);
                        conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
                            try{
                                if(reply.failed()){
                                    throw  new Exception(reply.cause());
                                }
                                Integer productId = reply.result().getKeys().getInteger(0);
                                this.commit(conn, message, new JsonObject().put("id", productId));

                            }catch(Exception e){
                                this.rollback(conn, reply.cause(), message);

                            }

                        });
                    } else {
                        this.rollback(conn, new Throwable("The sku entered has already been registered"), message);
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, replyCheck.cause(), message);
                }
            });
        });
    }

    private void associateBranchoffices(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            Integer productId = body.getInteger("product_id");
            JsonArray branchoffices = (JsonArray) body.getValue("branchoffices");

            createRelation(conn, "product", productId, branchoffices).whenComplete((result, error) -> {
                if(error != null){
                    this.rollback(conn, error, message);
                } else {
                    this.commit(conn, message, new JsonObject().put("result", result));
                }
            });
        });
    }

    private void associateProducts(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            Integer branchofficeId = body.getInteger("branchoffice_id");
            JsonArray products = (JsonArray) body.getValue("products");

            createRelation(conn, "branchoffice", branchofficeId, products).whenComplete((result, error) -> {
                if(error != null){
                    this.rollback(conn, error, message);
                } else {
                    this.commit(conn, message, new JsonObject().put("result", result));
                }
            });
        });
    }

    public CompletableFuture<Boolean> createRelation(SQLConnection conn, String type, Integer referenceId, JsonArray arrayToAssociate){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray();
        Integer productId = null;
        Integer branchofficeId = null;
        for (int i = 0, max = arrayToAssociate.size(); i < max; i++) {
            if (type.equals("product")){
                productId = referenceId;
                branchofficeId = arrayToAssociate.getInteger(i);
            } else if(type.equals("branchoffice")){
                branchofficeId = referenceId;
                productId = arrayToAssociate.getInteger(i);
            }
            JsonObject obj = new JsonObject()
                    .put("product_id", productId)
                    .put("branchoffice_id", branchofficeId);
            params.add(obj);
        }

        String QUERY_DELETE_RELATION = type.equals("product") ? QUERY_DELETE_RELATION_PRODUCT_BRANCHOFFICES : type.equals("branchoffice") ? QUERY_DELETE_RELATION_BRANCHOFFICE_PRODUCTS : null;

        conn.queryWithParams(QUERY_DELETE_RELATION, new JsonArray().add(referenceId), reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                this.execInserts(conn, params).whenComplete((result, error) ->{
                    if(error != null){
                        future.completeExceptionally(error);
                    } else {
                        future.complete(true);
                    }
                });
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> execInserts(SQLConnection conn, JsonArray relation){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(relation.stream()
                .map(rel -> insertRelation(conn, (JsonObject) rel))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(relation);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonObject> insertRelation(SQLConnection conn, JsonObject relation){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer branchofficeId = relation.getInteger("branchoffice_id");
        Integer productId = relation.getInteger("product_id");
        JsonArray params = new JsonArray().add(productId).add(branchofficeId);
        conn.queryWithParams(QUERY_COUNT_BRANCHOFFICES_PRODUCTS, params, replyExists -> {
            try{
                if(replyExists.failed()) {
                    throw new Exception(replyExists.cause());
                }
                if(replyExists.result().getRows().get(0).getInteger("quantity").equals(0)){
                    String query = this.generateGenericCreate("branchoffices_products", relation);
                    conn.query(query, insRelationReply -> {
                        if(insRelationReply.succeeded()){
                            future.complete(relation);
                        } else {
                            future.completeExceptionally(insRelationReply.cause());
                        }
                    });
                } else {
                    future.completeExceptionally(new Throwable("The relation product_id:"+productId+" with branchoffice_id:"+branchofficeId+" is already exists"));
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(replyExists.cause());
            }
        });
        return future;
    }

    private void report(Message<JsonObject> message) {
        Integer productId = message.body().getInteger("id");
        String QUERY_GET_PRODUCT= "";
        if (productId != null){
            QUERY_GET_PRODUCT = String.format(QUERY_PRODUCT_BY_ID, productId);
        } else {
            QUERY_GET_PRODUCT = QUERY_PRODUCTS;
        }

        this.dbClient.query(QUERY_GET_PRODUCT, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> products = reply.result().getRows();
                this.execGetBranchofficesByProduct(new JsonArray(products)).whenComplete((resultProducts, error) -> {
                    if(error != null){
                        reportQueryError(message, error);
                    } else {
                        message.reply(resultProducts);
                    }
                });

            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
    }

    private CompletableFuture<JsonArray> execGetBranchofficesByProduct(JsonArray branches){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(branches.stream()
                .map(rel -> getBranchofficesByProduct((JsonObject) rel))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(branches);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonObject> getBranchofficesByProduct(JsonObject branch){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_RELATION_PRODUCT_BRANCHOFFICES, new JsonArray().add(branch.getInteger("id")), replyP ->{
            try{
                if(replyP.failed()) {
                    throw new Exception(replyP.cause());
                }
                List<JsonObject> productsRelation = replyP.result().getRows();
                if(productsRelation.size() != 0){
                    for(JsonObject branchoffice : productsRelation){
                        if(branch.containsKey("branchoffices")){
                            JsonArray branchoffices = (JsonArray) branch.getValue("branchoffices");
                            branch.put("branchoffices", branchoffices.add(branchoffice.getInteger("id")));
                        } else {
                            branch.put("branchoffices", new JsonArray().add(branchoffice.getInteger("id")));
                        }
                    }
                } else {
                    branch.put("branchoffices", new JsonArray());
                }
                future.complete(branch);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(replyP.cause());
            }
        });
        return future;
    }

    private static final String QUERY_PRODUCT_BY_ID = "SELECT * FROM products WHERE id = %d AND (status = 1 OR status = 2);";

    private static final String QUERY_PRODUCTS = "SELECT * FROM products WHERE status = 1 OR status = 2;";

    private static final String QUERY_RELATION_PRODUCT_BRANCHOFFICES = "SELECT\n" +
            " b.id \n" +
            "FROM branchoffice AS b \n" +
            "LEFT JOIN branchoffices_products AS bp ON b.id = bp.branchoffice_id \n" +
            "WHERE bp.product_id = ? AND b.status = 1;";

    private static final String QUERY_COUNT_BRANCHOFFICES_PRODUCTS = "SELECT count(*) AS quantity FROM branchoffices_products WHERE product_id = ? AND branchoffice_id = ?;";

    private static final String QUERY_CHECK_SKU_PRODUCT = "SELECT count(id) AS quantity FROM products WHERE sku = ? AND status = 1;";

    private static final String QUERY_DELETE_RELATION_PRODUCT_BRANCHOFFICES = "DELETE FROM branchoffices_products WHERE product_id = ?;";

    private static final String QUERY_DELETE_RELATION_BRANCHOFFICE_PRODUCTS = "DELETE FROM branchoffices_products WHERE branchoffice_id = ?;";
}
