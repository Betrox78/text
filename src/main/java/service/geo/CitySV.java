/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.geo;

import database.commons.ErrorCodes;
import database.geo.CityDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

import static service.commons.Constants.*;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseError;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CitySV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CityDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cities";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        // Add all generic handlers
        this.addHandler(HttpMethod.GET, "/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/:id", AuthMiddleware.getInstance(), this::findById);
        this.addHandler(HttpMethod.GET, "/count", AuthMiddleware.getInstance(), this::count);
        this.addHandler(HttpMethod.GET, "/count/perPage/:num", AuthMiddleware.getInstance(), this::countPerPage);
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::create);
        this.addHandler(HttpMethod.PUT, "/", AuthMiddleware.getInstance(), this::update);
        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::deleteById);
        this.addHandler(HttpMethod.GET, "/active/terminals", AuthMiddleware.getInstance(), this::getCitiesTerminals);
        super.start(startFuture);
    }

    public void getCitiesTerminals(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CityDBV.ACTION_GET_CITIES_TERMINALS);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

}
