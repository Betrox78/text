package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.parcel.ParcelDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants._TERMINAL_ORIGIN_ID;

public class PendingCollect extends DBHandler<ParcelDBV> {

    public PendingCollect(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer terminalOriginId = body.getInteger(_TERMINAL_ORIGIN_ID);
            dbClient.queryWithParams(GET_PENDING_COLLECT, new JsonArray().add(terminalOriginId), reply -> {
                try {
                    if(reply.failed()) {
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

    private static final String GET_PENDING_COLLECT = "SELECT p.* FROM parcels p\n" +
            "INNER JOIN branchoffice bo ON (bo.id = p.terminal_origin_id\n" +
            "   OR bo.id IN (SELECT bprc.receiving_branchoffice_id \n" +
            "           FROM branchoffice_parcel_receiving_config bprc\n" +
            "            WHERE bprc.of_branchoffice_id = p.terminal_origin_id))\n" +
            "WHERE p.parcel_status = 12\n" +
            "   AND p.terminal_origin_id = ?;";

}
