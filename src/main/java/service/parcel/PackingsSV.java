package service.parcel;

import database.parcel.PackingsDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsValidation;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.isGraterAndNotNull;

public class PackingsSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PackingsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/packings";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/active", AuthMiddleware.getInstance(), this::getActive);
        this.addHandler(HttpMethod.POST, "/cost", AuthMiddleware.getInstance(), this::getCost);
        super.start(startFuture);
    }

    public void getActive(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PackingsDBV.ACTION_GET_ACTIVE);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    public void getCost(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _PACKING_ID, 0);
            isGraterAndNotNull(body, _QUANTITY, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PackingsDBV.ACTION_GET_COST);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }
}
