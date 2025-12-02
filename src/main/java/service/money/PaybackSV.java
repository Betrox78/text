package service.money;

import database.commons.ErrorCodes;
import database.customers.CustomerDBV;
import database.money.PaybackDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsDate;
import utils.UtilsJWT;
import utils.UtilsResponse;

import java.util.Date;

import static service.commons.Constants.*;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import static utils.UtilsValidation.isEmptyAndNotNull;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gerardo Valdés Uriarte - gerardo@indqtech.com
 */
public class PaybackSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return PaybackDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/payback";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.PUT, "/update", AuthMiddleware.getInstance(), this::updatePayback);
        super.start(startFuture);
    }

    private void updatePayback(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isEmptyAndNotNull(body, "service_type");
            isDecimal(body,"points");
            isGraterAndNotNull(body,"km",0);
            isDecimal(body,"money");
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            body.put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, PaybackDBV.ACTION_UPDATE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Updated");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

}
