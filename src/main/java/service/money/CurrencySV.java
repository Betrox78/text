/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.CurrencyDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import utils.UtilsValidation;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CurrencySV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CurrencyDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/currencies";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isName(body, "name");
            UtilsValidation.isName(body, "abr");
            return super.isValidUpdateData(context);
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isNameAndNotNull(body, "name");
            UtilsValidation.isNameAndNotNull(body, "abr");
            return super.isValidCreateData(context);
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

}
