/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.commons.ErrorCodes;
import database.routes.ConfigTicketPriceDBV;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsJWT;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static service.commons.Constants.*;
import static service.commons.Constants.USER_ID;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.isGraterAndNotNull;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigTicketPriceSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ConfigTicketPriceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/configTicketsPrices";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
    }

    private void register(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, "config_destination_id", 0);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            vertx.eventBus().send(this.getDBAddress(), body, options(ConfigTicketPriceDBV.ACTION_REGISTER), reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }

                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                }catch(Exception e){
                    responseError(context, reply.cause().getMessage());

                }

            });
        }catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }
}
