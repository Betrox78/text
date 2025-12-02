package service.commons.middlewares;

import database.money.CashOutDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class CashOutMiddleware implements Handler<RoutingContext> {

    private static CashOutMiddleware instance = new CashOutMiddleware();
    private static Vertx vertx;

    public static CashOutMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new CashOutMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private CashOutMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        Integer userID = context.get(USER_ID);
        vertx.eventBus().send(CashOutDBV.class.getSimpleName(),
                new JsonObject().put(USER_ID, userID),
                new DeliveryOptions().addHeader(ACTION, CashOutDBV.ACTION_CHECK_CASH_OUT_EMPLOYEE),
                reply -> {
                    try {
                        if (reply.succeeded()) {
                            JsonObject result = (JsonObject) reply.result().body();
                            context.put(CASHOUT_ID, result.getInteger(ID));
                            context.put(CASH_REGISTER_ID, result.getInteger(CASH_REGISTER_ID));
                            context.next();
                        } else {
                            reply.cause().printStackTrace();
                            responseError(context, reply.cause());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, e);
                    }
                });
    }

}
