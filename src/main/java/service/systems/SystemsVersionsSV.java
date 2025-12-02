package service.systems;



import database.commons.ErrorCodes;

import database.systemsVersions.systemVersionsDBV;
import database.users.UsersDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PermissionMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsResponse;
import utils.UtilsSecurity;
import utils.UtilsValidation;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import static utils.UtilsValidation.isPhoneNumber;




public class SystemsVersionsSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return systemVersionsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/systems";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/getVersionAbordoMovilIos", PublicRouteMiddleware.getInstance(), this::getVersionAbordoMovilIos);
        this.addHandler(HttpMethod.GET, "/getVersionAbordoMovilAndroid", PublicRouteMiddleware.getInstance(), this::getVersionAbordoMovilAndroid);
        super.start(startFuture);
    }


    private void getVersionAbordoMovilIos(RoutingContext context) {
        // Use a service account
        JsonObject body = new JsonObject();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(systemVersionsDBV.class.getSimpleName(), body, options(systemVersionsDBV.ACTION_GET_VERSION_APP_MOVIL_IOS), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getVersionAbordoMovilAndroid(RoutingContext context) {
        // Use a service account
        JsonObject body = new JsonObject();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(systemVersionsDBV.class.getSimpleName(), body, options(systemVersionsDBV.ACTION_GET_VERSION_APP_MOVIL_ANDROID), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }


}
