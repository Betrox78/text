/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.authservices;

import database.authservices.AuthServicesDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static service.commons.Constants.CREATED_BY;
import static service.commons.Constants.USER_ID;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class AuthServicesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return AuthServicesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/authServices";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        super.start(startFuture);

    }

    private void register(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            UtilsValidation.isEmptyAndNotNull(body, AuthServicesDBV.SERVICE);
            JsonArray paths = (JsonArray) body.getValue(AuthServicesDBV.PATHS);
            if(paths == null || paths.size() == 0){
                UtilsResponse.responsePropertyValue(context, new UtilsValidation.PropertyValueException("asda"));
            } else {
                body.put(CREATED_BY, context.<Integer>get(USER_ID));

                this.vertx.eventBus().send(getDBAddress(), body, options(AuthServicesDBV.ACTION_REGISTER),
                        reply -> {
                            this.genericResponse(context, reply);
                        });
            }
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }
}
