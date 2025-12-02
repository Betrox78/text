/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.CashOutDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.EmployeeIDMiddleware;
import service.commons.middlewares.PaymentMethodsMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.money.CashOutDBV.*;
import static database.money.CashRegisterDBV.BRANCHOFFICE_ID;
import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsValidation.*;


/**
 *
 * @author ulises
 */
public class CashOutSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CashOutDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cashOuts";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/actions/open", AuthMiddleware.getInstance(), EmployeeIDMiddleware.getInstance(vertx), this::openCashOut);
        this.addHandler(HttpMethod.POST, "/actions/open/driver", AuthMiddleware.getInstance(), EmployeeIDMiddleware.getInstance(vertx), this::openCashOutDriver);
        this.addHandler(HttpMethod.POST, "/actions/zReport", AuthMiddleware.getInstance(), PaymentMethodsMiddleware.getInstance(vertx), this::zReport);
        this.addHandler(HttpMethod.GET, "/actions/getExtended/:cashOutId", AuthMiddleware.getInstance(), PaymentMethodsMiddleware.getInstance(vertx), this::getExtended);
        this.addHandler(HttpMethod.POST, "/actions/closeReport", AuthMiddleware.getInstance(), PaymentMethodsMiddleware.getInstance(vertx), this::closeReport);
        this.addHandler(HttpMethod.GET, "/actions/getDetail/:cashOutId", AuthMiddleware.getInstance(), this::getDetail);
        this.addHandler(HttpMethod.GET, "/actions/getResumeSales/:cashOutId", AuthMiddleware.getInstance(), PaymentMethodsMiddleware.getInstance(vertx), this::getResumeSales);
        this.addHandler(HttpMethod.POST, "/report", AuthMiddleware.getInstance(), this::getReport);
        super.start(startFuture);
    }

    private void openCashOut(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put(EMPLOYEE_ID, context.<Integer>get(EMPLOYEE_ID))
                    .put(TOKEN, context.<String>get(TOKEN))
                    .put(IP, context.request().remoteAddress().host());

            UtilsValidation.isGraterAndNotNull(body, BRANCHOFFICE_ID,0);
            UtilsValidation.isGraterAndNotNull(body, CASH_REGISTER_ID,0);

            vertx.eventBus().send(CashOutDBV.class.getSimpleName(),body, options(ACTION_OPEN_CASH_OUT), reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    this.genericResponse(context, reply);

                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void openCashOutDriver(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID))
                .put(EMPLOYEE_ID, context.<Integer>get(EMPLOYEE_ID))
                .put(TOKEN, context.<String>get(TOKEN))
                .put(IP, context.request().remoteAddress().host());

            UtilsValidation.isGraterAndNotNull(body,"vehicle_id",0);
            UtilsValidation.isGraterAndNotNull(body,"schedule_route_id",0);

            vertx.eventBus().send(CashOutDBV.class.getSimpleName(),body, options(ACTION_OPEN_CASH_OUT_DRIVER), reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    this.genericResponse(context, reply);

                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
        }
    }


    private void closeReport(RoutingContext context) {
        Integer userId = context.<Integer>get(USER_ID);
        JsonArray paymentMethods = context.get("payment_methods");
            try {
                JsonObject body = context.getBodyAsJson();
                body.put("payment_methods", paymentMethods);
                if(body.containsKey("cash_out_origin")){
                    UtilsValidation.isContainedAndNotNull(body,"cash_out_origin","branchoffice","driver");
                    if(!body.getString("cash_out_origin").equals("driver")){
                        UtilsValidation.isGraterAndNotNull(body,"cash_register_id",0);
                        UtilsValidation.isGraterAndNotNull(body,"branchoffice_id",0);
                    }else{
                        UtilsValidation.isGraterAndNotNull(body,"branchoffice_id",0);
                    }
                }else{
                    UtilsValidation.isGraterAndNotNull(body,"cash_register_id",0);
                }

                body.put("created_by", userId);
                //add user id to details
                JsonArray details = body.getJsonArray("cash_out_details");
                if (details != null && !details.isEmpty()) {
                    for (int i = 0; i < details.size(); i++) {
                        JsonObject detail = details.getJsonObject(i);
                        detail.put("created_by", userId);
                    }
                }
                vertx.eventBus().send(
                        CashOutDBV.class.getSimpleName(),
                        body, options(ACTION_CASH_OUT_CUT),
                        reply -> {
                        try{
                            if (reply.failed()){
                                throw new Exception(reply.cause());
                            }
                            this.genericResponse(context, reply);

                        }catch(Exception ex){
                            ex.printStackTrace();
                            responseError(context, ex.getMessage());
                        }
                        });
            } catch (UtilsValidation.PropertyValueException e) {
                UtilsResponse.responsePropertyValue(context, e);
            }
    }
    private void getDetail(RoutingContext context) {
            Integer cashOutId = Integer.valueOf(context.request().getParam("cashOutId"));
            JsonObject params = new JsonObject().put("cash_out_id", cashOutId);
            vertx.eventBus().send(
                    CashOutDBV.class.getSimpleName(),
                    params, options(ACTION_CASH_OUT_DETAIL),
                    reply -> {
                        try{
                            if (reply.failed()){
                                    throw new Exception(reply.cause());
                            }
                            this.genericResponse(context, reply);
                        }catch(Exception ex){
                            ex.printStackTrace();
                            responseError(context, ex.getMessage());
                        }
                    });
    }

    private void getResumeSales(RoutingContext context) {
        Integer cashOutId = Integer.valueOf(context.request().getParam("cashOutId"));
        JsonObject params = new JsonObject()
                .put("cash_out_id", cashOutId)
                .put("payment_methods", context.<JsonArray>get("payment_methods"));

        vertx.eventBus().send(
                CashOutDBV.class.getSimpleName(),
                params, options(ACTION_CASH_OUT_RESUME_SALES),
                reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        this.genericResponse(context, reply);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                });
    }



    private void getExtended(RoutingContext context) {
        Integer cashOutId = Integer.valueOf(context.request().getParam("cashOutId"));
        JsonObject params = new JsonObject()
                .put("cash_out_id", cashOutId)
                .put("payment_methods", context.<JsonArray>get("payment_methods"));

        vertx.eventBus().send(
                CashOutDBV.class.getSimpleName(),
                params, options(ACTION_CASH_OUT_EXTENDED),
                reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        this.genericResponse(context, reply);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                });
    }
        private void zReport(RoutingContext context){
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CashOutDBV.Z_CASH_OUT_REPORT);
            JsonObject body = context.getBodyAsJson();
            isDateTimeAndNotNull(body , "init_date" , "cash_out");
            isDateTimeAndNotNull(body , "end_date" , "cash_out");
            body.put("payment_methods", context.<JsonArray>get("payment_methods"));
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
                }
            });
        }catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            responseError(context,"Date entered is not valid.Invalid Format", ex.getMessage());
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void getReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body,_INIT_DATE);
            isEmptyAndNotNull(body,_END_DATE);
            isGrater(body, _BRANCHOFFICE_ID, 0);
            isGrater(body, CREATED_BY, 0);

            vertx.eventBus().send(CashOutDBV.class.getSimpleName(),body, options(ACTION_CASH_OUT_REPORT), reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    this.genericResponse(context, reply);

                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
        }
    }
}
