/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.rental;

import database.commons.ErrorCodes;
import database.rental.RentalPriceDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import static service.commons.Constants.*;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsJWT;
import static utils.UtilsResponse.*;

/**
 *
 * @author ulises
 */
public class RentalPriceSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return RentalPriceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/rentalsPrices";
    }
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY,context.<Integer>get(USER_ID));

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, RentalPriceDBV.ACTION_REGISTER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (Throwable t){
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }
    
}
