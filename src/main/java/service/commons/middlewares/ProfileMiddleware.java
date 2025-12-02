package service.commons.middlewares;

import database.employees.EmployeeDBV;
import database.users.UsersDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class ProfileMiddleware implements Handler<RoutingContext> {
    private static ProfileMiddleware instance;
    private static Vertx vertx;

    public static synchronized ProfileMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new ProfileMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private ProfileMiddleware() {
    }
    @Override
    public void handle(RoutingContext context) {
        try {
            vertx.eventBus().send(UsersDBV.class.getSimpleName(),
                new JsonObject().put(USER_ID, context.<Integer>get(USER_ID)),
                new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_GET_PROFILE_BY_USER_ID),
                (AsyncResult<Message<Object>> reply) -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        JsonObject profile = (JsonObject) reply.result().body();
                        context.put(_PROFILE, profile);
                        context.next();
                    } catch (Throwable t) {
                        responseError(context, t.getMessage());
                    }
                });
        } catch (Exception e) {
            responseError(context, e.getMessage());
        }
    }
}
