package database.routes.handlers.ScheduleRouteDBV;

import database.commons.DBHandler;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.Date;

public class RouteSeatLocksExpiration extends DBHandler<ScheduleRouteDBV> {
    public RouteSeatLocksExpiration(ScheduleRouteDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            Date nowMinus30Minutes = new Date(System.currentTimeMillis() - 20 * 60 * 1000);
            String nowMinus30MinutesFormatted = UtilsDate.format_YYYY_MM_DD_HH_MM(nowMinus30Minutes);
            JsonArray params = new JsonArray().add(nowMinus30MinutesFormatted);
            dbClient.updateWithParams(QUERY_EXPIRE_SEAT_LOCKS, params, replyUpdate ->  {
                try {
                    if (replyUpdate.failed()) {
                        replyUpdate.cause().printStackTrace();
                        replyError(message, replyUpdate.cause());
                        return;
                    }

                    JsonObject response = new JsonObject()
                            .put("expired", replyUpdate.result().getUpdated());
                    replyResult(message, response);

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    replyError(message, throwable);
                }
            });

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            replyError(message, throwable);
        }

    }


    private static final String QUERY_EXPIRE_SEAT_LOCKS =
            " DELETE FROM schedule_route_destination_seat_lock" +
                    " WHERE status = 1" +
                    " AND created_at < ?;";
}
