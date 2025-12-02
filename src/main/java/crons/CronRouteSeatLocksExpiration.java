package crons;

import database.routes.ScheduleRouteDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

import static service.commons.Constants.ACTION;

public class CronRouteSeatLocksExpiration extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        super.start();
        System.out.println("Running seat locks expiration every 2 minutes");
        this.vertx.setPeriodic(5 * 60 * 1000, __ -> {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.EXPIRE_SEAT_LOCKS);
            this.vertx.eventBus().<JsonObject>send(ScheduleRouteDBV.class.getSimpleName(),
                    null, options, reply -> {
                try {
                    if (!reply.succeeded()) {
                        System.out.println("Can't expire seat locks, " + reply.cause().getMessage());
                    } else {
                        Message<JsonObject> result = reply.result();
                        Integer expired = result.body().getInteger("expired");
                        if (Objects.nonNull(expired) && expired > 0) {
                            System.out.println(expired + " seat locks expired");
                        }
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
        });
    }
}
