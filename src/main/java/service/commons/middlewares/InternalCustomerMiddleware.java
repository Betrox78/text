package service.commons.middlewares;

import database.configs.GeneralConfigDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class InternalCustomerMiddleware implements Handler<RoutingContext> {

    private static InternalCustomerMiddleware instance = new InternalCustomerMiddleware();
    private static Vertx vertx;

    public static InternalCustomerMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new InternalCustomerMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private InternalCustomerMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (body.getInteger(CUSTOMER_ID) != null) {
            Integer customerId = body.getInteger(CUSTOMER_ID);
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "internal_customer"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), reply -> {
                        try {
                            if (reply.failed()){
                                throw new Exception(reply.cause());
                            }
                            JsonObject result = (JsonObject) reply.result().body();
                            Integer internalCustomerId = Integer.parseInt(result.getString(VALUE));
                            if (customerId.equals(internalCustomerId)){
                                context.put(INTERNAL_CUSTOMER, true);
                                context.next();
                            } else {
                                context.put(INTERNAL_CUSTOMER, false);
                                context.next();
                            }
                        } catch (Exception e){
                            responseError(context, e);
                        }
                    });
        } else {
            context.put(INTERNAL_CUSTOMER, false);
            context.next();
        }
    }

}
