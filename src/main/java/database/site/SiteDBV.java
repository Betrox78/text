package database.site;

import database.commons.DBVerticle;
import database.permission.PermissionDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import javax.swing.text.html.parser.Parser;

import static service.commons.Constants.ACTION;

public class SiteDBV extends DBVerticle {

    public static final String ACTION_SITE_CARUSEL = "SiteDBV.getCarusel";
    public static final String ACTION_SITE_CARUSEL_ADMIN = "SiteDBV.getCaruselAdmin";
    public static final String ACTION_SITE_CARUSEL_ADMIN_UPDATE = "SiteDBV.updateCaruselAdmin";
    public static final String ACTION_SITE_CARUSEL_ADMIN_INSERT = "SiteDBV.insertCaruselAdmin";
    public static final String ACTION_SITE_CARUSEL_ADMIN_DELETE="SiteDBV.deleteCaruselAdmin";


    @Override
    public String getTableName() {
        return "site";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_SITE_CARUSEL:
                this.getCarusel(message);
                break;
            case ACTION_SITE_CARUSEL_ADMIN:
                this.getCaruselAdmin(message);
                break;
            case ACTION_SITE_CARUSEL_ADMIN_UPDATE:
                this.updateStatusCaruselAdmin(message);
                break;
            case ACTION_SITE_CARUSEL_ADMIN_INSERT:
                this.insertCaruselAdmin(message);
                break;
            case ACTION_SITE_CARUSEL_ADMIN_DELETE:
                this.deleteCaruselAdmin(message);
                break;

        }
    }

    private void getCarusel(Message<JsonObject> message) {
        this.dbClient.queryWithParams(GET_CARUSEL_WEBSITE, null, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    JsonObject result = reply.result().getRows().get(0);

                            message.reply(new JsonArray().add(reply.result().getRows()));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }
    private void getCaruselAdmin(Message<JsonObject> message) {
        this.dbClient.queryWithParams(GET_CARUSEL_ADMIN, null, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    JsonObject result = reply.result().getRows().get(0);

                    message.reply(new JsonArray().add(reply.result().getRows()));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void updateStatusCaruselAdmin(Message<JsonObject> message) {
         JsonObject body = message.body();
        Integer status=Integer.parseInt(body.getString("status"));
            JsonArray params = new JsonArray()
                    .add(status)
                  .add(Integer.parseInt(body.getString("updated_by"))).add(body.getInteger("Id"));

        this.dbClient.queryWithParams(UPDATE_STATUS_IMG, params, reply -> {
            if (reply.succeeded()) {

                    message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void  insertCaruselAdmin(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();



                this.insertSlide(conn,data).whenComplete((res, error) -> {

                    try {
                        if(error != null){
                            this.rollback(conn, error, message);
                        } else {
                            this.commit(conn, message, res);
                        }


                    } catch (Exception e) {
                        this.rollback(conn, error, message);

                    }

                });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }

    private CompletableFuture<JsonObject> insertSlide(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();


        JsonObject paramRAD = new JsonObject()
                .put("Slide",     params.getString("Slide"))
                .put("Descripcion",     params.getString("Descripcion"))
                .put("orden",     Integer.parseInt(params.getString("orden")))
                .put("created_by", Integer.parseInt(params.getString("idUser")));
        String insert = this.generateGenericCreate("site_carusel",paramRAD);

        conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    future.complete(reply.result().toJson());
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });



        return future;
    }




    private void deleteCaruselAdmin(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray()
                .add(body.getInteger("Id"));

        this.dbClient.queryWithParams(DELETE_IMG_SLIDE, params, reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }


    //<editor-fold defaultstate="collapsed" desc="queries">


    private static final String GET_CARUSEL_WEBSITE = "SELECT \n"+
            " * FROM site_carusel where status=1  order by orden asc;\n";
    private static final String GET_CARUSEL_ADMIN = "SELECT \n"+
            " * FROM site_carusel order by orden asc;\n";

    private static final String UPDATE_STATUS_IMG = "UPDATE site_carusel \n" +
            " SET  status = ? ,\n" +
            "updated_by = ?\n" +
            " WHERE Id = ?;";

    private static final String DELETE_IMG_SLIDE = "DELETE FROM site_carusel \n" +

            " WHERE Id = ?;";




//</editor-fold>
}
