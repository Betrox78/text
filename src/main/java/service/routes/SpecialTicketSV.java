/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.commons.ErrorCodes;
import database.routes.SpecialTicketDBV;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Date;
import models.PropertyError;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsDate;

import utils.UtilsResponse;

import static database.routes.SpecialTicketDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class SpecialTicketSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return SpecialTicketDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/specialTickets";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/:id", AuthMiddleware.getInstance(), this::findById);
        this.addHandler(HttpMethod.GET, "/count", AuthMiddleware.getInstance(), this::count);
        this.addHandler(HttpMethod.GET, "/count/perPage/:num", AuthMiddleware.getInstance(), this::countPerPage);
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::create);
        this.addHandler(HttpMethod.PUT, "/", AuthMiddleware.getInstance(), this::updateV2);
        this.addHandler(HttpMethod.POST, "/updateV2/", AuthMiddleware.getInstance(), this::updateV2);
        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::deleteById);
        this.addHandler(HttpMethod.GET, "/public/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAll);
        super.start(startFuture);

    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isBetweenRange(body, TOTAL_DISCOUNT, 0, 100);
            isBoolean(body, HAS_DISCOUNT);
            isBooleanAndNotNull(body, IS_DEFAULT);
            isBoolean(body, "has_preferent_zone");
            if (body.getBoolean(IS_DEFAULT)){
                if (body.containsKey(HAS_DISCOUNT) && body.getBoolean(HAS_DISCOUNT)){
                    throw new PropertyValueException(HAS_DISCOUNT, "When the field is_default is true, has_discount can't be true");
                }
                if (body.containsKey(TOTAL_DISCOUNT) && body.getInteger(TOTAL_DISCOUNT) > 0){
                    throw new PropertyValueException(TOTAL_DISCOUNT, "When the field is_default is true, total_discount can't be major to 0");
                }
            }
        } catch (PropertyValueException e) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError(e.getName(), e.getError()));
        } catch (Exception e) {
            responseWarning(context, e);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isBetweenRange(body, TOTAL_DISCOUNT, 0, 100);
            isBoolean(body, HAS_DISCOUNT);
            isBoolean(body, HAS_PREFERENT_ZONE);
            isBooleanAndNotNull(body, IS_DEFAULT);
            isBoolean(body, FORCE_DEFAULT);
            if (body.getBoolean(IS_DEFAULT)){
                if (body.containsKey(HAS_DISCOUNT) && body.getBoolean(HAS_DISCOUNT)){
                    throw new PropertyValueException(HAS_DISCOUNT, "When the field is_default is true, has_discount can't be true");
                }
                if (body.containsKey(TOTAL_DISCOUNT) && body.getInteger(TOTAL_DISCOUNT) > 0){
                    throw new PropertyValueException(TOTAL_DISCOUNT, "When the field is_default is true, total_discount can't be major to 0");
                }
            }
        } catch (PropertyValueException e) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError(e.getName(), e.getError()));
        } catch (Exception e){
            responseWarning(context, e);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void deleteById(RoutingContext context) {
        JsonObject body = new JsonObject();

        body.put("id", Integer.valueOf(context.request().getParam("id") ));
        body.put("updated_at", UtilsDate.sdfDataBase(new Date()));
        body.put("updated_by", context.<Integer>get("user_id"));
        body.put("status", 3);
        try{
            vertx.eventBus().send(
                    this.getDBAddress(),
                    body,
                    options(SpecialTicketDBV.UPDATEV2), reply -> {
                        try{
                            if (reply.failed()){
                                throw new Exception(reply.cause());
                            }
                            MultiMap headers = reply.result().headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, "Deleted", reply.result().body());
                            }
                        }catch (Exception t){
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }
                    });

        }catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }


    }
    
    protected void updateV2(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put("updated_at", UtilsDate.sdfDataBase(new Date()));
        body.put("updated_by", context.<Integer>get("user_id"));
        try {
            isGraterAndNotNull(body, "id", 0);
            isGraterAndNotNull(body, "status", 0);
            try{
                vertx.eventBus().send(
                    this.getDBAddress(),
                    body,
                    options(SpecialTicketDBV.UPDATEV2), reply -> {
                            try{
                                if(reply.failed()) {
                                    throw new Exception(reply.cause());
                                }
                                MultiMap headers = reply.result().headers();
                                if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                } else {
                                    responseOk(context, "Updated", reply.result().body());
                                }

                            }catch (Exception ex) {
                                ex.printStackTrace();
                                responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                            }
                    });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }

    }
}
