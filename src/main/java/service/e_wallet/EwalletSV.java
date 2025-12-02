package service.e_wallet;

import database.commons.ErrorCodes;
import database.e_wallet.EwalletDBV;
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
import utils.UtilsResponse;
import utils.UtilsValidation;

import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

public class EwalletSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EwalletDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/e_wallet";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/getRechargeRangeList", AuthMiddleware.getInstance(), this::getRechargeRangeList);
        this.addHandler(HttpMethod.POST, "/registerRechargeRange", AuthMiddleware.getInstance(), this::registerRechargeRange);
        this.addHandler(HttpMethod.PUT, "/modify_recharge_range", AuthMiddleware.getInstance(), this::modifyRechargeRange);
        this.addHandler(HttpMethod.PUT, "/change_recharge_range_status", AuthMiddleware.getInstance(), this::updateRechargeRangeStatus);
        this.addHandler(HttpMethod.POST, "/init", AuthMiddleware.getInstance(), this::initWallet);
        this.addHandler(HttpMethod.GET, "/getWalletMoves/:user_id", AuthMiddleware.getInstance(), this::getWalletMoves);
        this.addHandler(HttpMethod.GET, "/getBonificationPercent/:service_type", AuthMiddleware.getInstance(), this::getBonificationPercent);
        this.addHandler(HttpMethod.GET, "/getBasicInfo/:user_id", AuthMiddleware.getInstance(), this::getWalletBasicInfo);
        super.start(startFuture);
    }

    public void registerRechargeRange(RoutingContext context) {
        JsonObject data = context.getBodyAsJson();
        try {
            //Validar data
            JsonObject body = data.getJsonObject("range");
            isDecimal(body,"min_range");
            isDecimal(body,"max_range");
            isDecimal(body,"extra_percent");
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EwalletDBV.ACTION_REGISTER_RECHARGE_RANGE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, "Ocurrio un error inesperado al registrar", t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    public void getRechargeRangeList(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, options(EwalletDBV.ACTION_GET_RECHARGE_RANGE_LIST), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e){
            responseError(context, e);
        }
    }

    private void updateRechargeRangeStatus(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(UPDATED_BY, context.<Integer>get(USER_ID));
        this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, options(EwalletDBV.ACTION_UPDATE_RECHARGE_RANGE_STATUS), reply -> {
            if(reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Updated");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void modifyRechargeRange(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isDecimal(body,"min_range");
            isDecimal(body,"max_range");
            isDecimal(body,"extra_percent");
            body.remove("edit");
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));

            vertx.eventBus().send(this.getDBAddress(), body, options(EwalletDBV.ACTION_UPDATE_RECHARGE_RANGE), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Updated");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, t);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void initWallet(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, "user_id", 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EwalletDBV.ACTION_INIT_WALLET);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, "Ocurrio un error inesperado al registrar", t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    public void getWalletMoves(RoutingContext context) {
        try {
            JsonObject body = new JsonObject()
                    .put("user_id", Integer.valueOf(context.request().getParam("user_id")));
            this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, options(EwalletDBV.ACTION_GET_WALLET_MOVES), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e){
            responseError(context, e);
        }
    }

    public void getBonificationPercent(RoutingContext context) {
        try {
            JsonObject body = new JsonObject()
                    .put("service_type", context.request().getParam("service_type"));
            this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, options(EwalletDBV.ACTION_GET_BONIFICATION_PERCENT), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e){
            responseError(context, e);
        }
    }

    public void getWalletBasicInfo(RoutingContext context) {
        try {
            JsonObject body = new JsonObject()
                    .put("user_id", Integer.valueOf(context.request().getParam("user_id")));
            this.vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, options(EwalletDBV.GET_WALLET_BY_USER_ID), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e){
            responseError(context, e);
        }
    }
}
