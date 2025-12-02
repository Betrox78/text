package service.commons.middlewares;

import database.money.CashOutDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class CheckCashOutMiddleware implements Handler<RoutingContext> {

    private static CheckCashOutMiddleware instance = new CheckCashOutMiddleware();
    private static Vertx vertx;

    public static CheckCashOutMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new CheckCashOutMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private CheckCashOutMiddleware() {
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
                        }
                        context.next();
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, e);
                    }
                });
    }

}
