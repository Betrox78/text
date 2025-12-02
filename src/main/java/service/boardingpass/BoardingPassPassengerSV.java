/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.boardingpass;

import database.boardingpass.BoardingPassPassengerDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDate;
import models.PropertyError;
import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import service.commons.ServiceVerticle;
import utils.UtilsDate;
import utils.UtilsResponse;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author ulises
 */
public class BoardingPassPassengerSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BoardingPassPassengerDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/boardingPassesPassengers";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "first_name");
            isName(body, "last_name");
            isContained(body, "gender", "hombre", "mujer", "indefinido");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            isBoolean(body, "principal_passenger");
            isBoolean(body, "is_customer");
            isBoolean(body, "check_in");
            isBoolean(body, "has_complements");
            isBoolean(body, "was_printed");
            isDate(body, "birthday");
            isBoolean(body, "need_preferential");
            if (body.getString("birthday") != null && !body.getString("birthday").isEmpty()) {
                int age = UtilsDate.calculateAge(LocalDate.parse(body.getString("birthday")));
                if (age > 130) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("birthday", "age can't be grater than 130"));
                    return false;
                }
                body.put("age", age);
                context.setBody(body.toBuffer());
            }

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
            isContainedAndNotNull(body, "gender", "hombre", "mujer", "indefinido");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            isBoolean(body, "principal_passenger");
            isBoolean(body, "is_customer");
            isBoolean(body, "check_in");
            isBoolean(body, "has_complements");
            isBoolean(body, "was_printed");
            isBoolean(body, "need_preferential");
            isDateAndNotNull(body, "birthday");
            int age = UtilsDate.calculateAge(LocalDate.parse(body.getString("birthday")));
            if (age > 130) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("birthday", "age can't be grater than 130"));
                return false;
            }
            body.put("age", age);
            context.setBody(body.toBuffer());
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
