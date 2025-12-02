/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.suppliers;

import database.suppliers.SupplierContactDBV;
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
public class SupplierContactSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return SupplierContactDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/suppliersContact";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "name");
            isName(body, "last_name");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "name");
            isNameAndNotNull(body, "last_name");
            isPhoneNumberAndNotNull(body, "phone");
            isMail(body, "email");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
