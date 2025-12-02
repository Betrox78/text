/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.geo;

import database.geo.SuburbDBV;
import static database.geo.SuburbDBV.ACTION_ZIP_CODE_SEARCH;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import static service.commons.Constants.ACTION;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsJWT;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseInvalidToken;
import static utils.UtilsResponse.responseOk;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class SuburbSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return SuburbDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/suburbs";
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
        this.addHandler(HttpMethod.GET, "/zipCodeSearch/:zipCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::zipCodeSearch);
        this.addHandler(HttpMethod.POST, "/zipCodeSAT", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::zipCodeSAT);
        super.start(startFuture);
    }

    private void zipCodeSearch(RoutingContext ctx) {
        String jwt = ctx.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject message = new JsonObject().put("zipCode", ctx.request().getParam("zipCode"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_ZIP_CODE_SEARCH);
            vertx.eventBus().send(this.getDBAddress(), message, options, reply -> {
                if (reply.succeeded()) {
                    responseOk(ctx, reply.result().body(), "Found");
                } else {
                    responseError(ctx, "Ocurrió un error inesperado, consulte con el proveedor de sistemas",
                            reply.cause().getMessage());
                }
            });
        } else {
            responseInvalidToken(ctx);
        }
    }

    private void zipCodeSAT(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        String jwt = ctx.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, SuburbDBV.ACTION_ZIP_CODE_SEARCH_SAT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                if (reply.succeeded()) {
                    responseOk(ctx, reply.result().body(), "Found");
                } else {
                    responseError(ctx, "Ocurrió un error inesperado, consulte con el proveedor de sistemas",
                            reply.cause().getMessage());
                }
            });
        } else {
            responseInvalidToken(ctx);
        }
    }

}
