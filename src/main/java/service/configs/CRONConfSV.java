/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.configs;

import database.configs.CRONConfDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import utils.UtilsValidation;

/**
 *
 * @author ulises
 */
public class CRONConfSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CRONConfDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cronsConfigs";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject ob = context.getBodyAsJson();
        try {
            UtilsValidation.isBoolean(ob, "cron_exchange_rate_update_active");
            if (ob.getString("cron_exchange_rate_updated_last_time") != null) {
                UtilsResponse.responseWarning(context, "You can´t update the cron_exchange_rate_updated_last_time property", "Can´t update", null);
                return false;
            }
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        UtilsResponse.responseWarning(context, "This action is not allowed, Can´t create the configuratión by yourself", "Can´t create the configuratión by yourself", null);
        return false;
    }

}
