package service.parcel;

import database.commons.ErrorCodes;
import database.parcel.PackagePriceDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

import utils.UtilsResponse;

import static utils.UtilsValidation.*;

import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;


public class PackagePriceSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PackagePriceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/packagePrices";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        super.start(startFuture);
    }

    public void register(RoutingContext context) {
        JsonObject data = context.getBodyAsJson();
        try {
            //Validar data
            JsonObject body = data.getJsonObject("ppPackage");

            isDecimal(body,"min_linear_volume");
            isDecimal(body,"max_linear_volume");
            isDecimal(body,"min_weight");
            isDecimal(body,"max_weight");
            isDecimal(body,"price");
            isDecimal(body, "currency_id");
            isNameAndNotNull(body,"shipping_type");

            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PackagePriceDBV.REGISTER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        JsonObject result = (JsonObject) reply.result().body();
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, "Ocurrio un error inesperado al registrar", t);
                }
            });

        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

}
