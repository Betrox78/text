/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.branchoffices;

import database.branchoffices.BranchofficeDBV;
import database.commons.ErrorCodes;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import models.PropertyError;

import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import service.commons.ServiceVerticle;
import utils.UtilsResponse;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.shipments.ShipmentsDBV.TERMINAL_ORIGIN_ID;
import static service.commons.Constants.*;
import static service.commons.Constants.USER_ID;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.isEmptyAndNotNull;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.isGraterAndNotNull;

import utils.UtilsValidation;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class BranchofficeSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BranchofficeDBV.class.getSimpleName();
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/report/list", AuthMiddleware.getInstance(), this::report);
        this.addHandler(HttpMethod.GET, "/report/list/:id", AuthMiddleware.getInstance(), this::report);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.GET, "/public/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/report/schedules", AuthMiddleware.getInstance(), this::schedulesReport);
        this.addHandler(HttpMethod.GET, "/report/schedules/public", PublicRouteMiddleware.getInstance(), this::schedulesReportPublic);
        this.addHandler(HttpMethod.GET, "/schedules/public/web", PublicRouteMiddleware.getInstance(), this::schedulesReportPublicWeb);
        this.addHandler(HttpMethod.GET, "/terminalAndCorp", AuthMiddleware.getInstance(), this::terminalAndCorp);
        this.addHandler(HttpMethod.GET, "/terminalAndCorpAndSites", AuthMiddleware.getInstance(), this::terminalAndCorpAndSites);
        this.addHandler(HttpMethod.GET, "/getTerminals", AuthMiddleware.getInstance(), this::getTerminals);
        this.addHandler(HttpMethod.POST, "/getDistanceTime", AuthMiddleware.getInstance(), this::getDistanceDurationBetweenTerminals);
        this.addHandler(HttpMethod.GET, "/getTerminalsDistanceAndTime/:terminal_origin_id", AuthMiddleware.getInstance(), this::getTerminalsDistanceAndTime);
        this.addHandler(HttpMethod.POST, "/createDistanceTime", AuthMiddleware.getInstance(), this::createDistanceTime);
        this.addHandler(HttpMethod.PUT, "/updateDistanceTime", AuthMiddleware.getInstance(), this::updateDistanceTime);
        this.addHandler(HttpMethod.GET, "/getDistance/:terminal_origin_id/:terminal_destiny_id", AuthMiddleware.getInstance(), this::getDistance);
        this.addHandler(HttpMethod.POST, "/getTerminalsByDistance", AuthMiddleware.getInstance(), this::getTerminalsByDistance);
        super.start(startFuture);
    }

    @Override
    protected String getEndpointAddress() {
        return "/branchoffices";
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        String branchOfficeType = body.getString("branch_office_type");
        if (branchOfficeType != null) {
            if (branchOfficeType.length() != 1) {
                errors.add(new PropertyError("branch_office_type", UtilsValidation.INVALID_FORMAT));
            } else {
                if (!(branchOfficeType.equals("A")
                        || branchOfficeType.equals("O")
                        || branchOfficeType.equals("T")
                        || branchOfficeType.equals("V"))) {
                    errors.add(new PropertyError("branch_office_type", "Unique posible values are: 'A', 'O', 'T', 'V'"));
                }
                if(branchOfficeType.equals("T")) {
                    if (body.getJsonArray("distances") == null) {
                        errors.add(new PropertyError("distances", "distances is null"));
                    }
                    if (body.getJsonArray("distances").isEmpty()) {
                        errors.add(new PropertyError("distances", "distances is empty"));
                    }
                }
            }
        } else {
            errors.add(new PropertyError("suburb", UtilsValidation.MISSING_REQUIRED_VALUE));
        }
        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        String branchOfficeType = body.getString("branch_office_type");
        if (branchOfficeType != null) {
            if (branchOfficeType.length() > 1) {
                errors.add(new PropertyError("branch_office_type", UtilsValidation.INVALID_FORMAT));
            } else {
                if (!(branchOfficeType.equals("A")
                        || branchOfficeType.equals("O")
                        || branchOfficeType.equals("T")
                        || branchOfficeType.equals("V"))) {
                    errors.add(new PropertyError("branch_office_type", "Unique posible values are: 'A', 'O', 'T', 'V'"));
                }
            }
        }
        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    private void report(RoutingContext context) {
        JsonObject body = new JsonObject();
        body.put("id", Integer.parseInt(context.request().getParam("id")));
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    private void register(RoutingContext context) {
        JsonObject reqBody = context.getBodyAsJson();
        reqBody.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            //validate schedules not null
            JsonArray schedules = context.getBodyAsJson().getJsonArray("schedules");
            if (schedules != null) {
                if (!schedules.isEmpty()) {
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.REGISTER);
                    vertx.eventBus().send(this.getDBAddress(), reqBody, options, reply -> {
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
                            t.printStackTrace();
                            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
                        }
                    });
                } else {
                    UtilsResponse.responsePropertyValue(context, new UtilsValidation.PropertyValueException("schedules", INVALID_DATA));
                }
            } else {
                UtilsResponse.responsePropertyValue(context, new UtilsValidation.PropertyValueException("schedules", UtilsValidation.MISSING_REQUIRED_VALUE));
            }
        }
    }

    private void schedulesReport(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT_SCHEDULES);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), null, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }
    private void schedulesReportPublic(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT_SCHEDULES_PUBLIC);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), null, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }
    private void schedulesReportPublicWeb(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT_SCHEDULES_PUBLIC_WEB);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), null, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    private void terminalAndCorp(RoutingContext context) {
        JsonObject body = new JsonObject();
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT_BRANCHOFFICE_CORP);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }
               if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                   responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
               } else {
                   responseOk(context, reply.result().body(), "Report");
               }
           } catch (Throwable t) {
               t.printStackTrace();
               responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
           }
        });
    }

    private void terminalAndCorpAndSites(RoutingContext context) {
        JsonObject body = new JsonObject();
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT_BRANCHOFFICE_CORP_SITES);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }
               if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                   responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
               } else {
                   responseOk(context, reply.result().body(), "Report");
               }
           } catch (Throwable t) {
               t.printStackTrace();
               responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
           }
        });
    }

    private void getTerminals(RoutingContext context) {
        JsonObject body = new JsonObject();
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_GET_TERMINALS);
        this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }
               if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                   responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
               } else {
                   responseOk(context, reply.result().body(), "Found");
               }
           } catch (Throwable t) {
               t.printStackTrace();
               responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
           }
        });
    }

    private void getDistanceDurationBetweenTerminals(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, ORIGIN);
            isEmptyAndNotNull(body.getJsonArray(DESTINIES), DESTINIES);
            JsonArray destinies = body.getJsonArray(DESTINIES);
            for(int i = 0; i < destinies.size(); i++) {
                isEmptyAndNotNull(destinies.getJsonObject(i), TERMINAL_ADDRESS);
            }
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_GET_DISTANCE_DURATION_BETWEEN_TERMINALS);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
                }
            });
        } catch (UtilsValidation.PropertyValueException t) {
            responsePropertyValue(context, t);
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, t.getMessage());
        }
    }

    private void getDistance(RoutingContext context) {
        try {

            JsonObject body = new JsonObject();
            body.put(TERMINAL_ORIGIN_ID, Integer.parseInt(context.request().getParam(TERMINAL_ORIGIN_ID)));
            body.put(TERMINAL_DESTINY_ID, Integer.parseInt(context.request().getParam(TERMINAL_DESTINY_ID)));

            isGraterAndNotNull(body, TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, TERMINAL_DESTINY_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_GET_DISTANCE);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
                }
            });
        } catch (UtilsValidation.PropertyValueException t) {
            responsePropertyValue(context, t);
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, t.getMessage());
        }
    }

    private void getTerminalsDistanceAndTime(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put(TERMINAL_ORIGIN_ID, Integer.parseInt(request.getParam(TERMINAL_ORIGIN_ID)));
            isGraterAndNotNull(body, TERMINAL_ORIGIN_ID, 0);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_GET_TERMINALS_DISTANCE_AND_TIME);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex);
        }
    }

    private void createDistanceTime(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isEmptyAndNotNull(body.getJsonArray(DESTINIES), DESTINIES);
            JsonArray destinies = body.getJsonArray(DESTINIES);
            for (Object d : destinies) {
                JsonObject destiny = (JsonObject) d;
                isGraterAndNotNull(destiny, TERMINAL_ORIGIN_ID, 0);
                isGraterAndNotNull(destiny, TERMINAL_DESTINY_ID, 0);
                isGraterAndNotNull(destiny, "distance_km", -1);
                isEmptyAndNotNull(destiny, "travel_time");
            }
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_CREATE_DISTANCE_AND_TIME);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex);
        }
    }

    private void updateDistanceTime(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            isEmptyAndNotNull(body.getJsonArray(DESTINIES), DESTINIES);
            JsonArray destinies = body.getJsonArray(DESTINIES);
            for (Object d : destinies) {
                JsonObject destiny = (JsonObject) d;
                isGraterAndNotNull(destiny, ID, 0);
                isGraterAndNotNull(destiny, "distance_km", -1);
                isEmptyAndNotNull(destiny, "travel_time");
            }
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_UPDATE_DISTANCE_AND_TIME);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex);
        }
    }

    private void getTerminalsByDistance(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _TERMINAL_ID, 0);
            isGraterAndNotNull(body, _DISTANCE_KM, 0.00);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_GET_TERMINALS_BY_DISTANCE);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex);
        }
    }

}
