package database.systemsVersions;

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

public class systemVersionsDBV extends DBVerticle {

    public static final String ACTION_GET_VERSION_APP_MOVIL_IOS= "SiteDBV.getVersionAbordoMovilIos";
    public static final String ACTION_GET_VERSION_APP_MOVIL_ANDROID= "SiteDBV.getVersionAbordoMovilAndroid";

    @Override
    public String getTableName() {
        return "systems_versions";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_GET_VERSION_APP_MOVIL_IOS:
                this.getVersionAbordoMovilIos(message);
                break;
            case ACTION_GET_VERSION_APP_MOVIL_ANDROID:
                this.getVersionAbordoMovilAndroid(message);
                break;


        }
    }

    private void getVersionAbordoMovilIos(Message<JsonObject> message) {
        this.dbClient.queryWithParams(GET_VERSION_APP_ABORDOMOVIL_IOS, null, reply -> {
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
    private void getVersionAbordoMovilAndroid(Message<JsonObject> message) {
        this.dbClient.queryWithParams(GET_VERSION_APP_ABORDOMOVIL_ANDROID, null, reply -> {
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
    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String GET_VERSION_APP_ABORDOMOVIL_IOS = "SELECT \n"+
            " * FROM systems_versions where name='AbordoMovil' AND os='IOS' AND status=1 ;\n";
    private static final String GET_VERSION_APP_ABORDOMOVIL_ANDROID = "SELECT \n"+
            " * FROM systems_versions where name='AbordoMovil' AND os='ANDROID' AND status=1  ;\n";

//</editor-fold>
}
