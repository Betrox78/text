/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.vechicles;

import database.vechicle.TypeServiceDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import utils.UtilsValidation;

/**
 *
 * @author ulises
 */
public class TypeServiceSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TypeServiceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/typesServices";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isName(body, "name");
            return super.isValidUpdateData(context);
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }

    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isNameAndNotNull(body, "name");
            return super.isValidCreateData(context);
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

}
