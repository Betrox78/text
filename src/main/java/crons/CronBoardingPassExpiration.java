package crons;

import database.boardingpass.BoardingPassDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;

import static service.commons.Constants.ACTION;

public class CronBoardingPassExpiration extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        super.start();
        this.vertx.setPeriodic(600000, __ -> {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.EXPIRE_RESERVATION);
            this.vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(),
                    null, options, reply -> {
                        if (!reply.succeeded()) {
                            System.out.println("Can't expire reservations, " + reply.cause().getMessage());
                        } else {
                            System.out.println("Reservations expired");
                        }
                    });
        });
    }
}
