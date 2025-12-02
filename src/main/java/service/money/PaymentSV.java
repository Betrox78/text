/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.commons.ErrorCodes;
import database.money.PaymentDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;

import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;

/**
 *
 * @author ulises
 */
public class PaymentSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return PaymentDBV.class.getSimpleName();
    }

    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::genericRegister);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/processTransaction", this::processTransaction);
        super.start(startFuture);
    }

    private void genericRegister(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(PaymentDBV.GENERIC_REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
                }
            });
        }
    }

    private void register(RoutingContext context) {
        JsonArray body = context.getBodyAsJsonArray();
        for(int i = 0; i < body.size(); i++){
            body.getJsonObject(i).put(CREATED_BY, context.<Integer>get(USER_ID));
        }
        if (this.isValidCreateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(PaymentDBV.REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
                }
            });
        }
    }

    @Override
    protected String getEndpointAddress() {
        return "/payments";
    }

    /**
     * Intended to use on Banorte PayWorks
     * @param ctx Http routing context
     */
    private void processTransaction(RoutingContext ctx) {
        HttpServerResponse res = ctx.response();
        res.setStatusCode(500);
        res.end("Not implemented\n");
    }
}
