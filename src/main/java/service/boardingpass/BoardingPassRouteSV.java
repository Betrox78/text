/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.boardingpass;

import database.boardingpass.BoardingPassRouteDBV;
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
public class BoardingPassRouteSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BoardingPassRouteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/boardingPassesRoutes";
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isContained(body, "ticket_type_route", "1", "2", "0");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isContained(body, "ticket_type_route", "1", "2", "0");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
