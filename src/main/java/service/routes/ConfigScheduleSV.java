/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.routes.ConfigScheduleDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigScheduleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ConfigScheduleDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/configSchedules";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour(body, "travel_hour");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24(body, "travel_hour");
            isBoolean(body, "sun");
            isBoolean(body, "mon");
            isBoolean(body, "tue");
            isBoolean(body, "wen");
            isBoolean(body, "thu");
            isBoolean(body, "fri");
            isBoolean(body, "sat");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
