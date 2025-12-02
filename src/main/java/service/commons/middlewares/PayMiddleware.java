package service.commons.middlewares;

import database.money.PaymentDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class PayMiddleware implements Handler<RoutingContext> {

    private static PayMiddleware instance = new PayMiddleware();
    private static Vertx vertx;
    private static String origin;
    private static JsonArray origins = new JsonArray().add("boarding_pass").add("parcels").add("rental");

    public static PayMiddleware getInstance(Vertx _vertx, String _origin) {
        if (instance == null) {
            instance = new PayMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }

        origin = _origin;
        return instance;
    }

    private PayMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            Integer origin_id = body.getInteger("id", null);
            if (origin_id == null) throw new Exception("Id: field required");
            if (!origins.contains(origin)) throw new Exception("Origin: not valid");
            JsonObject params = new JsonObject().put("origin_id", origin_id).put("origin", origin);

            vertx.eventBus().send(PaymentDBV.class.getSimpleName(), params, new DeliveryOptions()
                    .addHeader(ACTION, PaymentDBV.ACTION_SET_IN_PAYMENT_STATUS), replySet -> {
                try {
                    if (replySet.succeeded()) {
                        context.next();
                    } else {
                        responseError(context, replySet.cause());
                    }
                } catch (Exception e) {
                    responseError(context, e);
                }
            });

        } catch (Exception e) {
            responseError(context, e);
        }
    }
}
