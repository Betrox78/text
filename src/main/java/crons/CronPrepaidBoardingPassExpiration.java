package crons;

import database.boardingpass.BoardingPassDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import static service.commons.Constants.ACTION;


public class CronPrepaidBoardingPassExpiration extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        super.start();
        // 600000
        this.vertx.setPeriodic(600000, __ -> {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.EXPIRE_RESERVATIONS_PREPAID);
            this.vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(),
                    null, options, reply -> {
                        if (!reply.succeeded()) {
                            System.out.println("Can't expire prepaid reservations, " + reply.cause().getMessage());
                        }
                    });
        });
    }
}