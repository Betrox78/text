/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.boardingpass;

import database.boardingpass.ComplementDBV;
import database.boardingpass.ComplementIncidencesDBV;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;

import static utils.UtilsValidation.*;

/**
 *
 * @author ulises
 */
public class ComplementIncidencesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ComplementIncidencesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/complementIncidences";
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, "boarding_pass_complement_id", 0);
            isGraterAndNotNull(body, "incidence_id", 0);
            isGraterAndNotNull(body, "shipment_id", 0);
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

}
