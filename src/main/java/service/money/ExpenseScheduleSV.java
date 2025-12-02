/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.ExpenseSchedulesDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import utils.UtilsValidation;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ExpenseScheduleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ExpenseSchedulesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/expensesSchedules";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isDate(body, "last_time");
            UtilsValidation.isDate(body, "next_time");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isDate(body, "last_time");
            UtilsValidation.isDate(body, "next_time");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
