/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crons;

import database.routes.ConfigRouteDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import static service.commons.Constants.ACTION;

/**
 *
 * @author ulises
 */
public class CronConfigRouteExpirationEvaluation extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();
        startCron();
    }

    private void startCron() {
        this.vertx.setPeriodic(1000 * 60 * 60 , __ -> {
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ConfigRouteDBV.ACTION_EXPIRE_ROUTES);
            this.vertx.eventBus().send(
                    ConfigRouteDBV.class.getSimpleName(),
                    new JsonObject(),
                    options,
                    reply -> {
                        if (reply.failed()) {
                            System.out.println("Can't expire config routes, "
                                    + reply.cause().getMessage());
                        }else{
                            //only to see if something updates
                            //System.out.println(reply.result().body());
                        }
                    });
        });
    }

}
