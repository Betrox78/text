package service.site;

import database.commons.ErrorCodes;
import database.site.SiteDBV;
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




public class siteSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return SiteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/site";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/web", PublicRouteMiddleware.getInstance(), this::getCarusel);
        this.addHandler(HttpMethod.GET, "/carusel_admin", AuthMiddleware.getInstance(), this::getCaruselAdmin);
        this.addHandler(HttpMethod.PUT, "/status_carusel_admin", AuthMiddleware.getInstance(), this::updateCaruselAdmin);
        this.addHandler(HttpMethod.POST, "/carusel_admin", AuthMiddleware.getInstance(), this::insertImg);

        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::deleteCaruselAdmin);
        super.start(startFuture);
}


    private void getCarusel(RoutingContext context) {
        // Use a service account
        JsonObject body = new JsonObject();
      //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(SiteDBV.class.getSimpleName(), body, options(SiteDBV.ACTION_SITE_CARUSEL), reply -> {
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
    private void getCaruselAdmin(RoutingContext context) {
        // Use a service account
        JsonObject body = new JsonObject();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(SiteDBV.class.getSimpleName(), body, options(SiteDBV.ACTION_SITE_CARUSEL_ADMIN), reply -> {
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

    private void updateCaruselAdmin(RoutingContext context) {
        // Use a service account
        JsonObject body = context.getBodyAsJson();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(SiteDBV.class.getSimpleName(), body, options(SiteDBV.ACTION_SITE_CARUSEL_ADMIN_UPDATE), reply -> {
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


    private void insertImg(RoutingContext context) {
        // Use a service account
        JsonObject body = context.getBodyAsJson();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(SiteDBV.class.getSimpleName(), body, options(SiteDBV.ACTION_SITE_CARUSEL_ADMIN_INSERT), reply -> {
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
    private void deleteCaruselAdmin(RoutingContext context) {
        // Use a service account
        JsonObject message = new JsonObject()
                .put("Id", Integer.valueOf(context.request().getParam("id")));
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(SiteDBV.class.getSimpleName(), message, options(SiteDBV.ACTION_SITE_CARUSEL_ADMIN_DELETE), reply -> {
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
