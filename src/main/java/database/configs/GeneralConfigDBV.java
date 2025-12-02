/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.configs;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

/**
 *
 * @author ulises
 */
public class GeneralConfigDBV extends DBVerticle {

    public static final String ACTION_GET_CONFIG_BY_FIELD = 
            "GeneralConfig.getConfigByField";

    @Override
    public String getTableName() {
        return "general_setting";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_GET_CONFIG_BY_FIELD:
                this.getConfigByField(message);
                break;
        }
    }

    private void getConfigByField(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            String fieldName = body.getString("fieldName");
            JsonArray params = new JsonArray().add(fieldName);
            dbClient.queryWithParams(QUERY_GET_CONFIG_BY_FIELD, params, (reply) -> {
                try{
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> settings = reply.result().getRows();
                    if (settings.isEmpty()) {
                        throw new Exception("General setting not found: ".concat(fieldName));
                    }

                    message.reply(settings.get(0));

                }catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private static final String QUERY_GET_CONFIG_BY_FIELD = "SELECT\n"
            + "	*\n"
            + "FROM\n"
            + "	general_setting\n"
            + "WHERE\n"
            + "	FIELD = ?\n"
            + "	AND status = 1\n"
            + "LIMIT 1;";

}
