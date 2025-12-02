package database.parcel;

import database.commons.DBVerticle;
import database.parcel.handlers.PackingsDBV.Cost;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ACTION;

/**
 *
 * @author daliacarlon
 */
public class PackingsDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "packings";
    }

    public static final String ACTION_GET_ACTIVE = "PackingsDBV.getActive";
    public static final String ACTION_GET_COST = "PackingsDBV.getCost";

    Cost cost;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.cost = new Cost(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_GET_ACTIVE:
                getActive(message);
                break;
            case ACTION_GET_COST:
                this.cost.handle(message);
                break;
        }
    }

    private void getActive(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_GET_ACTIVE_PACKINGS, reply -> {
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

    private static final String QUERY_GET_ACTIVE_PACKINGS = "SELECT * FROM packings WHERE status = 1 AND cost IS NOT NULL";

}
