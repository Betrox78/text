/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.employees;

import database.employees.EmployeeSchedulesDBV;
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
public class EmployeeScheduleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EmployeeSchedulesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/employeeSchedules";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24(body, "hour_start");
            isHour24(body, "hour_end");
            isHour24(body, "break_start");
            isHour24(body, "break_end");
            isDate(body, "date_start");
            isDate(body, "date_end");
            isBoolean(body, "sun");
            isBoolean(body, "mon");
            isBoolean(body, "tue");
            isBoolean(body, "wen");
            isBoolean(body, "thu");
            isBoolean(body, "fri");
            isBoolean(body, "sat");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24AndNotNull(body, "hour_start");
            isHour24AndNotNull(body, "hour_end");
            isHour24(body, "break_start");
            isHour24(body, "break_end");
            isDateAndNotNull(body, "date_start");
            isDate(body, "date_end");
            isBoolean(body, "sun");
            isBoolean(body, "mon");
            isBoolean(body, "tue");
            isBoolean(body, "wen");
            isBoolean(body, "thu");
            isBoolean(body, "fri");
            isBoolean(body, "sat");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
