/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.vechicles;

import database.vechicle.DriverTrackingDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import static utils.UtilsResponse.responsePropertyValue;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author ulises
 */
public class DriverTrackingSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return DriverTrackingDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/driversTracking";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour(body, "time_tracking");
            isBoolean(body, "was_completed");
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHourAndNotNull(body, "time_tracking");
            isBoolean(body, "was_completed");
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
