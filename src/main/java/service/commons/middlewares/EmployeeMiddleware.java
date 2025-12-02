package service.commons.middlewares;

import database.employees.EmployeeDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class EmployeeMiddleware implements Handler<RoutingContext> {
    private static EmployeeMiddleware instance;
    private static Vertx vertx;

    public static synchronized EmployeeMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new EmployeeMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private EmployeeMiddleware() {
    }
    @Override
    public void handle(RoutingContext context) {
        try {
            vertx.eventBus().send(EmployeeDBV.class.getSimpleName(),
                    new JsonObject().put(USER_ID, context.<Integer>get(USER_ID)),
                    new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_EMPLOYEE_BY_USERE_ID),
                    (AsyncResult<Message<Object>> reply) -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            JsonObject employee = (JsonObject) reply.result().body();
                            context.put(EMPLOYEE, employee);
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
