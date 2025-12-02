/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.geo;

import database.geo.CountyDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CountySV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CountyDBV.class.getSimpleName();

    }

    @Override
    protected String getEndpointAddress() {
        return "/counties";
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
        super.start(startFuture);
    }

}
