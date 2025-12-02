/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crons;

import database.rental.RentalDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import static service.commons.Constants.ACTION;

/**
 *
 * @author ulises
 */
public class CronRentalExpirationEvaluation extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();
        this.vertx.setPeriodic(1000 * 60 * 60, __ -> {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, RentalDBV.ACTION_EXPIRE_RENTALS);
            this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                    null, options, reply -> {
                        if (!reply.succeeded()) {
                            System.out.println("Can't expire rentals, " + reply.cause().getMessage());
                        }
                    });
        });
    }

}
