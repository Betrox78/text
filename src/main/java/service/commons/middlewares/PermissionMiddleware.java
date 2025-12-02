package service.commons.middlewares;

import database.users.UsersDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.USER_ID;
import static utils.UtilsResponse.responseError;

public class PermissionMiddleware implements Handler<RoutingContext> {

    private static PermissionMiddleware instance = new PermissionMiddleware();
    private static Vertx vertx;
    private static String subModule;

    public static PermissionMiddleware getInstance(Vertx _vertx, String subModuleName) {
        if (instance == null) {
            instance = new PermissionMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        subModule = subModuleName;
        return instance;
    }

    private PermissionMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        Integer userID = context.get(USER_ID);
        this.isSuperUser(userID).whenComplete((resultSuperUser, errorSuperUser) -> {
            if(errorSuperUser != null) {
                responseError(context, errorSuperUser);
            } else {
                this.checkPermission(resultSuperUser ? null : userID).whenComplete((resultCheckPermission, errorCheckPermission) -> {
                    if(errorCheckPermission != null) {
                        responseError(context, errorCheckPermission);
                    } else {
                        context.put("superuser", resultSuperUser);
                        context.put("permissions", resultCheckPermission);
                        context.next();
                    }
                });
            }
        });
    }

    private CompletableFuture<JsonArray> checkPermission(Integer userId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonObject body = new JsonObject()
                .put(USER_ID, userId)
                .put("sub_module_name", subModule);

        vertx.eventBus().send(UsersDBV.class.getSimpleName(), body,
                new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_CHECK_PERMISSION),
                (AsyncResult<Message<JsonArray>> reply) -> {
                    try {
                        if (reply.succeeded()) {
                            future.complete(reply.result().body());
                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
        return future;
    }

    private CompletableFuture<Boolean> isSuperUser(Integer userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        vertx.eventBus().send(UsersDBV.class.getSimpleName(),
                new JsonObject().put(USER_ID, userId),
                new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_IS_SUPERUSER),
                (AsyncResult<Message<Object>> reply) -> {
                    try {
                        if (reply.succeeded()) {
                            Boolean result = (Boolean) reply.result().body();
                            future.complete(result);
                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
        return future;
    }

}
