package service.commons.middlewares;

import database.commons.Action;
import database.money.PaymentMethodDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

import static service.commons.Constants.ACTION;
import static utils.UtilsResponse.responseError;

public class PaymentMethodsMiddleware implements Handler<RoutingContext> {

    private static PaymentMethodsMiddleware instance;
    private static Vertx vertx;

    public static synchronized PaymentMethodsMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new PaymentMethodsMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private PaymentMethodsMiddleware() {}

    @Override
    public void handle(RoutingContext context) {
        JsonObject body = new JsonObject().put("where", "status=1");

        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, Action.FIND_ALL_V2.name());
        vertx.eventBus().send(PaymentMethodDBV.class.getSimpleName(), body, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                JsonArray methods = (JsonArray) reply.result().body();
                context.put("payment_methods", methods);
                context.next();
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Error listing payment methods on middleware", t);
            }
        });
    }
}
