/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.rental;

import database.rental.AddonVehicleDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import utils.UtilsValidation;
import static utils.UtilsValidation.*;

/**
 *
 * @author ulises
 */
public class AddonVehicleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return AddonVehicleDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/addonVehicles";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {            
            isBoolean(body, "is_an_extra");
            isBoolean(body, "removable");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body, "name");
            isBoolean(body, "is_an_extra");
            isBoolean(body, "removable");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
