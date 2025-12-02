package database.parcel;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ACTION;

/**
 *
 * @author gerardo
 */
public class DeliveryAttemptReasonDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "delivery_attempt_reason";
    }

    public static final String ACTION_LIST_ACTIVE = "ParcelsManifestDBV.getActive";

    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_LIST_ACTIVE:
                this.getActive(message);
                break;
        }
    }

    private void getActive(Message<JsonObject> message) {
        try {
            this.dbClient.query("SELECT * FROM delivery_attempt_reason WHERE status = 1;", reply -> {
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
}
