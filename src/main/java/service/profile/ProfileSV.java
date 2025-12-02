/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.profile;

import database.profile.ProfileDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;

/**
 *
 * Kriblet
 */
public class ProfileSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ProfileDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/profiles";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/list", AuthMiddleware.getInstance(), this::getList);
        super.start(startFuture);
    }

    private void getList(RoutingContext context) {
        this.vertx.eventBus().send(getDBAddress(), null, options(ProfileDBV.ACTION_GET_LIST),
                reply -> {
                    this.genericResponse(context, reply);
                });
    }

}
