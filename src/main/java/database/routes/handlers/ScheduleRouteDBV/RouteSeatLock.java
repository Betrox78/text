package database.routes.handlers.ScheduleRouteDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class RouteSeatLock extends DBHandler<ScheduleRouteDBV> {
    private static final String SCHEDULE_ROUTE_DESTINATION_SEAT_LOCK = "schedule_route_destination_seat_lock";
    private static final String ID = "id";
    private static final String SEAT = "seat";
    private static final String SCHEDULE_ROUTE_DESTINATION_ID = "schedule_route_destination_id";
    private static final String INTEGRATION_PARTNER_SESSION_ID = "integration_partner_session_id";
    private static final String TOKEN = "token";
    private static final String CREATED_BY = "created_by";

    public RouteSeatLock(ScheduleRouteDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();

                String token = body.getString(TOKEN);
                JsonArray searchSessionParams = new JsonArray()
                        .add(token);

                conn.queryWithParams(QUERY_SEARCH_PARTNER_SESSION, searchSessionParams, replySearchSession -> {
                    try {
                        if (replySearchSession.failed()) {
                            replySearchSession.cause().printStackTrace();
                            this.rollbackTransaction(message, conn, replySearchSession.cause());
                            return;
                        }

                        Optional<JsonObject> sessionResult = replySearchSession.result().getRows().stream().findFirst();
                        if (!sessionResult.isPresent()) {
                            this.commitTransaction(message, conn, null);
                            return;
                        }

                        JsonObject sessionFound = sessionResult.get();
                        Integer integrationPartnerSessionId = sessionFound.getInteger(ID);
                        String seat = body.getString(SEAT);
                        Integer scheduleRouteDestinationId = body.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);

                        JsonObject params = new JsonObject()
                                .put(SEAT, seat)
                                .put(CREATED_BY, 1)
                                .put(INTEGRATION_PARTNER_SESSION_ID,integrationPartnerSessionId)
                                .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId);

                        GenericQuery insert = generateGenericCreate(SCHEDULE_ROUTE_DESTINATION_SEAT_LOCK, params);
                        conn.updateWithParams(insert.getQuery(), insert.getParams(), replyInsert ->  {
                            try {
                                if (replyInsert.failed()) {
                                    replyInsert.cause().printStackTrace();
                                    this.rollbackTransaction(message, conn, replyInsert.cause());
                                    return;
                                }

                                Integer id = replyInsert.result().getKeys().getInteger(0);
                                JsonObject response = new JsonObject()
                                        .put(ID, id);

                                this.commitTransaction(message, conn, response);
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                                this.rollbackTransaction(message, conn, throwable);
                            }
                        });


                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        this.rollbackTransaction(message, conn, throwable);
                    }
                });

            } catch (Throwable throwable) {
                throwable.printStackTrace();
                this.rollbackTransaction(message, conn, throwable);
            }
        });
    }

    private static final String QUERY_SEARCH_PARTNER_SESSION =
            " SELECT IPS.*" +
            " FROM integration_partner_session as IPS" +
            " INNER JOIN integration_partner as IP" +
            " ON IP.id = IPS.integration_partner_id AND IP.status != 3" +
            " WHERE IPS.token = ?" +
            " AND IPS.status != 3";

}
