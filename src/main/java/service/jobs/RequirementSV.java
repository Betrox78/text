/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.jobs;

import database.jobs.RequirementDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class RequirementSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return RequirementDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/requirements";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "name");
            isName(body, "recurrent_type");
            isBoolean(body, "is_recurrent");
            isBoolean(body, "is_group");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        if (body.getString("type_req") == null) {
            boolean isGroup = body.getBoolean("is_group", false);
            if (!isGroup) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("type_req", MISSING_REQUIRED_VALUE));
                return false;
            }
        }
        //validate job is not deleted or archived

        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "name");
            isBoolean(body, "is_recurrent");
            isBoolean(body, "is_group");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        if (body.getString("type_req") == null) {
            boolean isGroup = body.getBoolean("is_group", false);
            if (!isGroup) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("type_req", MISSING_REQUIRED_VALUE));
                return false;
            }
        }
        if (body.getString("recurrent_type") == null) {
            boolean isRecurrent = body.getBoolean("is_recurrent", false);
            if (isRecurrent) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("recurrent_type", MISSING_REQUIRED_VALUE));
                return false;
            }
        }
        return super.isValidCreateData(context);
    }

}
