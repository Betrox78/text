package database.routes.handlers.ScheduleRouteDBV;

import database.commons.DBHandler;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import java.util.Optional;

public class RouteSeatUnlock extends DBHandler<ScheduleRouteDBV> {
    private static final String SCHEDULE_ROUTE_DESTINATION_SEAT_LOCK = "schedule_route_destination_seat_lock";
    private static final String ID = "id";
    private static final String SEAT = "seat";
    private static final String SCHEDULE_ROUTE_DESTINATION_ID = "schedule_route_destination_id";
    private static final String INTEGRATION_PARTNER_SESSION_ID = "integration_partner_session_id";
    private static final String CREATED_BY = "created_by";
    private static final String TOKEN = "token";

    public RouteSeatUnlock(ScheduleRouteDBV dbVerticle) {
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

                        JsonArray params = new JsonArray()
                                .add(seat)
                                .add(integrationPartnerSessionId)
                                .add(scheduleRouteDestinationId);
                        conn.queryWithParams(QUERY_SEARCH_SEAT_LOCK, params, replySearch -> {
                            try {
                                if (replySearch.failed()) {
                                    replySearch.cause().printStackTrace();
                                    this.rollbackTransaction(message, conn, replySearch.cause());
                                    return;
                                }

                                Optional<JsonObject> result = replySearch.result().getRows().stream().findFirst();
                                if (!result.isPresent()) {
                                    this.commitTransaction(message, conn, null);
                                    return;
                                }

                                JsonObject found = result.get();
                                conn.updateWithParams(QUERY_UPDATE_SEAT_LOCK, params, replyUpdate ->  {
                                    try {
                                        if (replyUpdate.failed()) {
                                            replyUpdate.cause().printStackTrace();
                                            this.rollbackTransaction(message, conn, replyUpdate.cause());
                                            return;
                                        }

                                        int updated = replyUpdate.result().getUpdated();
                                        if (updated == 0) {
                                            this.commitTransaction(message, conn, null);
                                            return;
                                        }

                                        JsonObject response = new JsonObject()
                                                .put(ID, found.getInteger(ID));

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

    private static final String QUERY_SEARCH_SEAT_LOCK =
            " SELECT * FROM schedule_route_destination_seat_lock" +
            " WHERE seat = ? " +
            " AND integration_partner_session_id = ?" +
            " AND schedule_route_destination_id = ?" +
            " AND status != 3;";

    private static final String QUERY_UPDATE_SEAT_LOCK =
            " DELETE FROM schedule_route_destination_seat_lock" +
            " WHERE seat = ?" +
            " AND integration_partner_session_id = ?" +
            " AND schedule_route_destination_id = ?" +
            " AND status = 1;";

}




