package service.insurances;

import database.commons.ErrorCodes;
import database.insurances.InsurancesDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsJWT;
import utils.UtilsValidation;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.isEmptyAndNotNull;
import static utils.UtilsValidation.isGraterAndNotNull;

public class InsurancesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return InsurancesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/insurances";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.GET, "/current", AuthMiddleware.getInstance(), this::getCurrentInsurance);
        this.addHandler(HttpMethod.POST, "/cost", AuthMiddleware.getInstance(), this::getCost);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(InsurancesDBV.REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, ex.getMessage());
                }
            });
        }
    }
    private  void getCurrentInsurance (RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");

        if (UtilsJWT.isTokenValid(jwt)) {

            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InsurancesDBV.GET_CURRENT_INSURANCE);
                vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options, reply -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());

                    }catch(Exception e){
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                    }


                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } else {
            responseInvalidToken(context);
        }
    }

    public void getCost(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _INSURANCE_VALUE, 0);
            isEmptyAndNotNull(body, _SERVICE);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InsurancesDBV.GET_COST);
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
