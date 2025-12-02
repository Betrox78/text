/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.employees;

import database.employees.EmployeeContactDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class EmployeeContactSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EmployeeContactDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/employeeContacts";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "name");
            isName(body, "last_name");
            isPhoneNumber(body, "phone");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        if (body.getInteger("relationship") != null) {
            if (body.getInteger("relationship") < 0 || body.getInteger("relationship") > 4) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("relationship", INVALID_FORMAT));
                return false;
            }
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "name");
            isNameAndNotNull(body, "last_name");
            isPhoneNumberAndNotNull(body, "phone");
            isBetweenRange(body, "relationship", 0, 4);
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
