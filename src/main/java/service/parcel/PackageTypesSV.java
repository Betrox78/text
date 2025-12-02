package service.parcel;

import database.commons.ErrorCodes;
import database.parcel.GuiappDBV;
import database.parcel.PackageTypesDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseError;

public class PackageTypesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PackageTypesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/packageTypes";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/inhouse", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getPackageTypesInhouse);
        this.addHandler(HttpMethod.GET, "/parcel", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getPackageTypesParcel);
        super.start(startFuture);
    }

    public void getPackageTypesInhouse(RoutingContext context){
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PackageTypesDBV.ACTION_GET_PACKAGE_TYPES_INHOUSE);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Elements found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, t);
                }
            });
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    public void getPackageTypesParcel(RoutingContext context){
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PackageTypesDBV.ACTION_GET_PACKAGE_TYPES_PARCEL);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Elements found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, t);
                }
            });
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
}
