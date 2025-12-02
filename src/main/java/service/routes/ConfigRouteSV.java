/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.routes.ConfigRouteDBV;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import service.commons.Constants;

import static database.boardingpass.BoardingPassDBV.*;
import service.commons.ServiceVerticle;

import utils.UtilsJWT;
import utils.UtilsResponse;

import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

import utils.UtilsValidation;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigRouteSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ConfigRouteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/configRoutes";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.router.post("/register").handler(BodyHandler.create()); //needed to catch body of request
        this.router.post("/register").handler(this::register);
        this.router.put("/deleteConfigRoute").handler(BodyHandler.create()).handler(this::delete);
        this.router.get("/report/listRoutes").handler(this::listRoutes);
        this.router.get("/report/listRoutes/:id").handler(this::getOneRoute);
        this.router.get("/report/destinies/:id").handler(this::routeDestinations);
        this.router.get("/report/return/:id").handler(this::routeReturn);
        this.router.get("/report/listRoutes/:branchoffice_id/:date").handler(this::listRoutesByBranchofficeAndDate);
        this.router.get("/report/listRoutes/:branchoffice_id/:date/:type").handler(this::listRoutesByBranchofficeAndDate);
        this.router.get("/report/listRoutes/:branchoffice_id/:date/:type/:all_day").handler(this::listRoutesByBranchofficeAndDate);
        this.router.get("/report/listParcelRoutes/:date/:terminal_id/:flag_scheduled/:type").handler(this::listParcelRoutes);
        this.router.post("/report/listScheduledParcelRoutes").handler(BodyHandler.create()).handler(this::getListScheduledParcelRoutes); //needed to catch body of request
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour(body, "travel_time");
            isBoolean(body, "one_way");
            if (body.getString("expired_at") != null) {
                String token = context.request().getHeader(Constants.AUTHORIZATION);
                int expiredBy = UtilsJWT.getUserIdFrom(token);
                body.put("expired_by", expiredBy);
                context.setBody(body.toBuffer());
            }
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException
                | UnsupportedJwtException | IllegalArgumentException | NullPointerException e) {
            UtilsResponse.responseInvalidToken(context);
            return false;
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour(body, "travel_time");
            isBoolean(body, "one_way");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    private void register(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        this.validateToken(context, id -> {
            try {
                JsonObject reqBody = context.getBodyAsJson();
                reqBody.put("created_by", UtilsJWT.getUserIdFrom(jwt));
                if (this.isValidCreateData(context)) {
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_REGISTER);
                    vertx.eventBus().send(this.getDBAddress(), reqBody, options, reply -> {
                        try {
                            if(reply.failed()){
                                throw reply.cause();
                            }
                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Created");
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t.getMessage());
                        }
                    });
                }
            } catch (Exception e){
                e.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
            }
        });
    }

    private void delete(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGrater(body, CONFIG_ROUTE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_ROUTE_DELETE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void routeDestinations(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject data = new JsonObject();
            try {
                int id = Integer.parseInt(context.request().getParam("id"));
                data.put("id", id);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_ROUTE_DESTINATIONS);
                vertx.eventBus().send(this.getDBAddress(), data, options, reply -> {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Found");
                        }
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception e) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new JsonObject().put("name", "id").put("error", "invalid data"));
            }

        } else {
            responseInvalidToken(context);
        }
    }

    private void listRoutes(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_LIST_ROUTES);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } else {
            responseInvalidToken(context);
        }
    }

    private void getOneRoute(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            int id = Integer.parseInt(context.request().getParam("id"));
            JsonObject data = new JsonObject().put("id", id);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_ONE_ROUTE);
            vertx.eventBus().send(this.getDBAddress(), data, options, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } else {
            responseInvalidToken(context);
        }
    }

    private void routeReturn(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject data = new JsonObject();
            try {
                int id = Integer.parseInt(context.request().getParam("id"));
                data.put("id", id);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_ROUTE_RETURN);
                vertx.eventBus().send(this.getDBAddress(), data, options, reply -> {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Found");
                        }
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception e) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new JsonObject().put("name", "id").put("error", "invalid data"));
            }

        } else {
            responseInvalidToken(context);
        }
    }

    private void listRoutesByBranchofficeAndDate(RoutingContext context) {
        HttpServerRequest request = context.request();
        try {
            JsonObject body = new JsonObject()
                    .put(BRANCHOFFICE_ID, Integer.parseInt(request.getParam(BRANCHOFFICE_ID)))
                    .put(DATE, request.getParam(DATE))
                    .put("type", request.getParam("type") != null ? request.getParam("type") : "load")
                    .put("all_day", request.getParam("all_day") == null || Boolean.parseBoolean(request.getParam("all_day")));

            isGraterAndNotNull(body, BRANCHOFFICE_ID, 0);
            isEmptyAndNotNull(body, "date", "route_list");
            isBoolean(body, "all_day");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_GET_LIST_ROUTES_BY_BRANCHOFFICE_AND_DATE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, INVALID_DATA, INVALID_DATA_MESSAGE, e.getMessage());
        }
    }

    private void listParcelRoutes(RoutingContext context) {
        HttpServerRequest request = context.request();
        try {
            JsonObject body = new JsonObject()
                    .put(DATE, request.getParam(DATE))
                    .put(TERMINAL_ID, Integer.parseInt(request.getParam(TERMINAL_ID)))
                    .put("flag_scheduled", Boolean.parseBoolean(request.getParam("flag_scheduled")))
                    .put("type", request.getParam("type") != null ? request.getParam("type") : "load");

            isDateAndNotNull(body, DATE);
            isGraterAndNotNull(body, TERMINAL_ID, 0);
            isBooleanAndNotNull(body, "flag_scheduled");

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "parcel_routes_time_before_travel_date"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), replyGC -> {
                try {
                    if (replyGC.failed()) {
                        throw replyGC.cause();
                    }

                    JsonObject config = (JsonObject) replyGC.result().body();
                    Integer parcelRoutesTimeBeforeTravelDate = Integer.parseInt(config.getString("value", "6"));
                    body.put("parcel_routes_time_before_travel_date", parcelRoutesTimeBeforeTravelDate);

                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_GET_LIST_PARCEL_ROUTES);
                    vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }

                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Found");
                            }
                        } catch (Throwable t){
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, INVALID_DATA, INVALID_DATA_MESSAGE, e.getMessage());
        }
    }

    private void getListScheduledParcelRoutes(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isDateAndNotNull(body, _INIT_DATE);
            isDateAndNotNull(body, _END_DATE);
            isGraterAndNotNull(body, _TERMINAL_ID, 0);
            isContainedAndNotNull(body, _TYPE, "arrivals", "departures", "both");
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ConfigRouteDBV.ACTION_GET_LIST_SCHEDULED_PARCEL_ROUTES);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, INVALID_DATA, INVALID_DATA_MESSAGE, e.getMessage());
        }
    }

}
