/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.commons.ErrorCodes;
import database.routes.ConfigDestinationDBV;
import static database.routes.ConfigDestinationDBV.CONFIG_ROUTE_ID;

import database.shipments.ShipmentsDBV;
import database.shipments.ShipmentsDBV.SHIPMENT_TYPES;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static database.shipments.ShipmentsDBV.TRAVEL_DATE;
import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;

import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsJWT;
import utils.UtilsResponse;
import utils.UtilsValidation;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigDestinationSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ConfigDestinationDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/configDestinations";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/getSegment", AuthMiddleware.getInstance(), this::getSegment);
        super.start(startFuture);
        this.router.get("/prices/:crid").handler(this::getDestinationPrices);
    }

    @Override
    protected void create(RoutingContext context) {
        super.create(context);
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isHour(body, "travel_time");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isHour(body, "travel_time");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    private void getDestinationPrices(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject message = new JsonObject()
                    .put(CONFIG_ROUTE_ID, Integer.valueOf(context.request().getParam("crid")));

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ConfigDestinationDBV.ACTION_DESTINATION_PRICES);

            vertx.eventBus().send(this.getDBAddress(), message, options, reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body(), "Found");
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }

            });
        } else {
            responseInvalidToken(context);
        }
    }

    private void getSegment(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            UtilsValidation.isContainedAndNotNull(body, "shipment_type", SHIPMENT_TYPES.LOAD.getName(), SHIPMENT_TYPES.DOWNLOAD.getName());
            UtilsValidation.isEmpty(body, TRAVEL_DATE);
            UtilsValidation.isGrater(body, TERMINAL_ID, 0);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ConfigDestinationDBV.ACTION_GET_SEGMENTS);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body(), "Found");
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                }
            });

        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

}
