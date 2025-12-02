/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.configs;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import static service.commons.Constants.ACTION;

/**
 *
 * @author ulises
 */
public class CRONConfDBV extends DBVerticle {

    public static final String ACTION_CRON_EXCHANGE_RATE_ACTIVE = "ConfDBV.cron_exchange_rate_update_active";

    @Override
    public String getTableName() {
        return "config_system";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_CRON_EXCHANGE_RATE_ACTIVE:
                this.cron_exchange_rate_active(message);
                break;
        }
    }

    private void cron_exchange_rate_active(Message<JsonObject> message) {
        this.dbClient.query(QUERY_CRON_EXCHANGE_RATE_ACTIVE, rep -> {
            if (rep.succeeded()) {
                message.reply(rep.result().getRows().get(0));
            } else {
                message.fail(ErrorCodes.DB_ERROR.ordinal(), rep.cause().getMessage());
            }
        });
    }

    private static final String QUERY_CRON_EXCHANGE_RATE_ACTIVE = "SELECT\n"
            + "	cron_exchange_rate_update_active,\n"
            + "	cron_exchange_rate_abordo_minus_qty,\n"
            + "	cron_exchange_rate_updated_last_time\n"
            + "FROM\n"
            + "	config_system";
}
