/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.commons.ErrorCodes;
import database.money.CashRegisterDBV;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.money.CashRegisterDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.MISSING_REQUIRED_VALUE;
import static utils.UtilsValidation.containsSpecialCharacterAndNotNull;

/**
 *
 * @author Dalia Carlón
 */
public class CashRegisterSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return CashRegisterDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cashRegisters";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.GET, "/actions/getClosed/:branchofficeId", AuthMiddleware.getInstance(), this::getClosed);
        this.addHandler(HttpMethod.POST, "/changeTicket", AuthMiddleware.getInstance(), this::changeTicket);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    responseError(context, "Ocurrió un error inesperado", t);
                }
            });
        }
    }

    private void changeTicket(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        try {
            UtilsValidation.isGraterAndNotNull(body, CASH_REGISTER_ID, 0);
            UtilsValidation.isEmptyAndNotNull(body, NOTES);
            UtilsValidation.isGraterEqual(body, NEW_TICKET, 0);
            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_CHANGE_TICKET), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "updated");
                    }
                } catch (Throwable t) {
                    responseError(context, t.getMessage(), t);
                }
            });
        } catch (UtilsValidation.PropertyValueException e) {
            e.printStackTrace();
            responseError(context, e.getMessage(), e);
        }
    }

    private void getClosed(RoutingContext context) {
        try {
            Integer branchofficeId = Integer.valueOf(context.request().getParam("branchofficeId"));
            vertx.eventBus().send(CashRegisterDBV.class.getSimpleName(), new JsonObject().put("branchoffice_id", branchofficeId),
                options(ACTION_CASH_REGISTERS_CLOSED),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        } catch (Throwable t){
            responseError(context, "Ocurrió un error inesperado", t);
        }
    }
}
