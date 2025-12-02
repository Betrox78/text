package database.health;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ACTION;

public class HealthDBV extends DBVerticle {
    public static final String HEALTH_CHECK = "health.check";
    @Override
    public String getTableName() {
        return "fake"; // Fake table to disable generic services
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case HEALTH_CHECK:
                healthCheck(message);
                break;
        }
    }

    private void healthCheck(Message<JsonObject> message) {
        this.dbClient.query(QUERY_COUNT_MIGRATIONS, reply -> {
            if (reply.failed()) {
                reportQueryError(message, reply.cause());
            } else {
                message.reply(new JsonObject());
            }
        });
    }

    // Using migrations table to check database health
    private static final String QUERY_COUNT_MIGRATIONS = "SELECT COUNT(id) AS items FROM migrations";
}
