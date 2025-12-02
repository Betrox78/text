/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.promos;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.promos.CustomersPromosDBV;
import database.promos.PromosDBV;
import database.promos.UsersPromosDBV;
import database.promos.enums.DISCOUNT_TYPES;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.eventbus.DeliveryOptions;

import static database.promos.PromosDBV.*;
import static database.promos.PromosDBV.APPLY_TO_PACKAGE_PRICE;
import static database.promos.PromosDBV.CUSTOMER_ID;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author Indq tech - Gerardo Valdes Uriarte
 */
public class CustomersPromosSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CustomersPromosDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/customersPromos";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/list", AuthMiddleware.getInstance(), this::getList);
        this.addHandler(HttpMethod.POST,"/listCustomers", AuthMiddleware.getInstance(), this::getListCustomers );
        this.addHandler(HttpMethod.GET, "/find/:id", AuthMiddleware.getInstance(), this::getPromosByCustomerId);
        super.start(startFuture);
    }

    private void register(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            UtilsValidation.isGraterAndNotNull(body, CustomersPromosDBV.PROMO_ID, 0);
            UtilsValidation.isGraterAndNotNull(body, CUSTOMER_ID, 0);

            if (body.containsKey(PromosDBV.USED)){
                throw new UtilsValidation.PropertyValueException(PromosDBV.USED, UtilsValidation.INVALID_PARAMETER);
            }

            vertx.eventBus().send(PromosDBV.class.getSimpleName(), body, options(PromosDBV.ACTION_CHECK_HAS_SPECIFIC_CUSTOMER), (AsyncResult<Message<JsonObject>> reply) -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else if (reply.result().body().getBoolean(PromosDBV.HAS_SPECIFIC_CUSTOMER)){
                        vertx.eventBus().send(this.getDBAddress(), body, options(CustomersPromosDBV.ACTION_REGISTER), (AsyncResult<Message<JsonObject>> replyRegister) -> {
                            if (replyRegister.succeeded()) {
                                if (replyRegister.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replyRegister.result().body());
                                } else {
                                    responseOk(context, replyRegister.result().body(), "Created");
                                }
                            } else {
                                replyRegister.cause().printStackTrace();
                                responseError(context, "Ocurrió un error inesperado", replyRegister.cause().getMessage());
                            }
                        });
                    } else {
                        responseError(context, "Discount code does not apply to customers");
                    }
                } else {
                    reply.cause().printStackTrace();
                    responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void getList(RoutingContext context){
        if(isValidGetListData(context)){
            JsonObject body = context.getBodyAsJson();
            body.put(USER_ID, context.<Integer>get(USER_ID));

            vertx.eventBus().send(UsersPromosDBV.class.getSimpleName(), body, options(UsersPromosDBV.ACTION_GET_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        JsonArray resultUserPromos = reply.result().body().getJsonArray("data");
                        if(!resultUserPromos.isEmpty()){
                            responseOk(context, resultUserPromos, "Found");
                        } else {
                            body.remove(USER_ID);
                            vertx.eventBus().send(this.getDBAddress(), body, options(CustomersPromosDBV.ACTION_GET_LIST), (AsyncResult<Message<JsonObject>> replyC) -> {
                                try {
                                    if (replyC.failed()){
                                        throw replyC.cause();
                                    }
                                    if (replyC.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replyC.result().body());
                                    } else {
                                        responseOk(context, replyC.result().body(), "Found");
                                    }
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                                }
                            });
                        }
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        }
    }

    public boolean isValidGetListData(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();

        if (!body.containsKey(CUSTOMER_ID)){
            errors.add(new PropertyError(CUSTOMER_ID, MISSING_REQUIRED_VALUE));
        }

        if (!body.containsKey(PromosDBV.DATE)){
            errors.add(new PropertyError(PromosDBV.DATE, MISSING_REQUIRED_VALUE));
        }

        if (body.containsKey(DISCOUNT_TYPE)){
            try {
                DISCOUNT_TYPES.fromValue(body.getString(DISCOUNT_TYPE));
            } catch (Exception e){
                errors.add(new PropertyError(DISCOUNT_TYPE, e.getMessage()));
            }
        }

        if (body.containsKey(APPLY_TO_SPECIAL_TICKETS)){
            try {
                JsonArray tickets = (JsonArray) body.getValue(APPLY_TO_SPECIAL_TICKETS);
                if (tickets == null){
                    errors.add(new PropertyError(APPLY_TO_SPECIAL_TICKETS, INVALID_FORMAT));
                }
                if (!tickets.isEmpty()){
                    tickets.forEach(t -> {
                        try {
                            Integer ticket = (Integer) t;
                        } catch (Exception e){
                            errors.add(new PropertyError(APPLY_TO_SPECIAL_TICKETS.concat(" element"), INVALID_FORMAT));
                        }
                    });
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_TO_SPECIAL_TICKETS, e.getMessage()));
            }
        }

        if (body.containsKey(APPLY_TO_PACKAGE_PRICE)){
            try {
                JsonArray packagePrices = (JsonArray) body.getValue(APPLY_TO_PACKAGE_PRICE);
                if (packagePrices == null){
                    errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE, INVALID_FORMAT));
                }
                if (!packagePrices.isEmpty()){
                    packagePrices.forEach(p -> {
                        try {
                            String packagePriceName = (String) p;
                        } catch (Exception e){
                            errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE.concat(" element"), INVALID_FORMAT));
                        }
                    });
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE, e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return true;
    }

    private void getListCustomers(RoutingContext context){
        try {

            JsonObject body = context.getBodyAsJson();

            vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, CustomersPromosDBV.ACTION_GET_CUSTOMERS_LIST), replyGS -> {
                        try {
                            if (replyGS.succeeded()){
                                Message<Object> result = replyGS.result();
                                if(result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                                } else {
                                    responseOk(context, result.body(), "Found");
                                }
                            } else {
                                responseError(context, UNEXPECTED_ERROR, replyGS.cause().getMessage());
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });

        } catch (Exception e){
            responseError(context, e.getMessage());
        }
    }

    private void getPromosByCustomerId(RoutingContext context){
        try {

            HttpServerRequest request = context.request();
            Integer id = Integer.parseInt(request.getParam("id"));
            JsonObject body = new JsonObject()
                    .put("id", id);

            vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, CustomersPromosDBV.ACTION_GET_PROMOS_BY_CUSTOMER_ID), replyGS -> {
                        try {
                            if (replyGS.succeeded()){
                                Message<Object> result = replyGS.result();
                                if(result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                                } else {
                                    responseOk(context, result.body(), "Found");
                                }
                            } else {
                                responseError(context, UNEXPECTED_ERROR, replyGS.cause().getMessage());
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });

        } catch (Exception e){
            responseError(context, e.getMessage());
        }
    }

}
