/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.routes.ScheduleRouteDriverDBV;
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
public class ScheduleRouteDriverSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ScheduleRouteDriverDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/schedulesRoutesDrivers";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24(body, "time_tracking");
            isBoolean(body, "was_completed");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24(body, "time_tracking");
            isBoolean(body, "was_completed");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
