/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.rental;

import database.rental.RentalDriverDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author ulises
 */
public class RentalDriverSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return RentalDriverDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/rentalDrivers";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "first_name");
            isName(body, "last_name");
            isDate(body, "birthday");
            isEmpty(body, "no_licence");
            isDate(body, "expired_at");
            isEmpty(body, "file_licence");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "first_name");
            isNameAndNotNull(body, "last_name");
            isDate(body, "birthday");
            isEmptyAndNotNull(body, "no_licence");
            isDateAndNotNull(body, "expired_at");
            isEmpty(body, "file_licence");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
