/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.rental;

import database.rental.ExtraChargeDBV;
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
public class ExtraChargeSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ExtraChargeDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/extraCharges";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "name");
            isBoolean(body, "on_reception");
            isBoolean(body, "required_reference");
            isBoolean(body, "on_driver");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "name");
            isBoolean(body, "on_reception");
            isBoolean(body, "required_reference");
            isBoolean(body, "on_driver");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
