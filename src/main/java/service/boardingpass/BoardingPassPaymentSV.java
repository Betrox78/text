/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.boardingpass;

import database.boardingpass.BoardingPassPaymentDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;
import utils.UtilsValidation;
import static utils.UtilsValidation.isContained;

/**
 *
 * @author ulises
 */
public class BoardingPassPaymentSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BoardingPassPaymentDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/boardingPassesPayments";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isContained(body, "payment_status", "1", "2", "0");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isContained(body, "payment_status", "1", "2", "0");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
