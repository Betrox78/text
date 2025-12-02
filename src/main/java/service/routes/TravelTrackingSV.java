/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.commons.ErrorCodes;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import database.routes.TravelTrackingDBV;
import database.shipments.ShipmentsDBV;
import database.vechicle.TrailersDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.EmployeeMiddleware;
import utils.UtilsDate;
import utils.UtilsResponse;

import static database.boardingpass.BoardingPassDBV.SHIPMENT_ID;
import static database.branchoffices.BranchofficeDBV.RECEIVE_TRANSHIPMENTS;
import static database.routes.TravelTrackingDBV.*;
import static database.vechicle.TrailersDBV.TRAILER_ID;
import static service.commons.Constants.*;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.CREATED_BY;
import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.isDateTimeAndNotNull;

import utils.UtilsValidation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Saul
 */
public class TravelTrackingSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TravelTrackingDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/travelTracking";
    }
    
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST,"/register", AuthMiddleware.getInstance(),this::register);
        this.addHandler(HttpMethod.POST,"/", AuthMiddleware.getInstance(),this::register);
        this.addHandler(HttpMethod.POST,"/init-travel/:schedule_route_id/:terminal_origin", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx),this::initTravel);
        this.addHandler(HttpMethod.POST,"/init-travel/manual/:schedule_route_id/:terminal_origin", AuthMiddleware.getInstance(), this::initTravel);
        this.addHandler(HttpMethod.POST,"/pause-travel/:schedule_route_id/:terminal_origin", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::pauseTravel);
        this.addHandler(HttpMethod.POST,"/pause-travel/manual/:schedule_route_id/:terminal_origin", AuthMiddleware.getInstance(), this::pauseTravel);
        this.addHandler(HttpMethod.POST,"/stop-travel/:schedule_route_id/:terminal_destiny", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::stopTravel);
        this.addHandler(HttpMethod.POST,"/stop-travel/manual/:schedule_route_id/:terminal_destiny", AuthMiddleware.getInstance(), this::stopTravel);
        this.addHandler(HttpMethod.POST,"/finish-travel/:schedule_route_id/:terminal_destiny", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::finishTravel);
        this.addHandler(HttpMethod.POST,"/finish-travel/manual/:schedule_route_id/:terminal_destiny", AuthMiddleware.getInstance(),this::finishTravel);
        this.addHandler(HttpMethod.POST, "/load/open/", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::initShipmentLoad);
        this.addHandler(HttpMethod.POST, "/load/open/manual", AuthMiddleware.getInstance(), this::initShipmentLoad);
        this.addHandler(HttpMethod.POST, "/load/close/", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::closeShipmentLoad);
        this.addHandler(HttpMethod.POST, "/load/close/manual", AuthMiddleware.getInstance(), this::closeShipmentLoad);
        this.addHandler(HttpMethod.POST, "/download/open/", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::initShipmentDownload);
        this.addHandler(HttpMethod.POST, "/download/open/manual", AuthMiddleware.getInstance(), this::initShipmentDownload);
        this.addHandler(HttpMethod.POST, "/download/close/", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::closeShipmentDownload);
        this.addHandler(HttpMethod.POST, "/maneuverReport", AuthMiddleware.getInstance(), this:: maneuverReport);
        this.addHandler(HttpMethod.POST, "/download/close/manual", AuthMiddleware.getInstance(), this::closeShipmentDownload);
        super.start(startFuture);
    }

    private void closeShipmentDownload(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            Integer terminalId;
            if (body.containsKey(TERMINAL_ID)){
                terminalId = body.getInteger(TERMINAL_ID);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                terminalId = employee.getInteger(BRANCHOFFICE_ID);
            }
            body.put(TERMINAL_ID, terminalId);
            isGraterAndNotNull(body, TERMINAL_ID,0);
            isGraterAndNotNull(body, SHIPMENT_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TravelTrackingDBV.CLOSE_DOWNLOAD), (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        JsonObject resultCloseDownload = reply.result().body();
                        Boolean applyRelease = resultCloseDownload.getBoolean(APPLY_RELEASE);
                        JsonArray hitchedTrailers = resultCloseDownload.getJsonArray(HITCHED_TRAILERS);

                        if (!applyRelease) {
                            responseOk(context, reply.result().body(), "Download close");
                        } else {
                            execReleaseTrailers(hitchedTrailers, body).whenComplete((resultReleaseTrailers, errReleaseTrailers) -> {
                                try {
                                    if (errReleaseTrailers != null) {
                                        throw errReleaseTrailers;
                                    }
                                    responseOk(context, reply.result().body(), "Download close");
                                } catch (Throwable t) {
                                    responseError(context, "Release hitched trailers error in download close", errReleaseTrailers);
                                }
                            });
                        }

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                }
            });
        } catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private CompletableFuture<Boolean> liteReleaseTrailer(JsonObject body) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        vertx.eventBus().send(TrailersDBV.class.getSimpleName(), body, options(TrailersDBV.ACTION_LITE_RELEASE), (AsyncResult<Message<JsonObject>> reply) -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               future.complete(true);
           } catch (Throwable t) {
               t.printStackTrace();
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<Boolean> execReleaseTrailers(JsonArray hitchedTrailers, JsonObject body) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            CompletableFuture.allOf(hitchedTrailers.stream()
                            .map(ht -> releaseTrailer(body
                                    .put(TRAILER_ID, Integer.parseInt(ht.toString()))))
                            .toArray(CompletableFuture[]::new))
                    .whenComplete((s, t) -> {
                        if (t != null) {
                            future.completeExceptionally(t);
                        } else {
                            future.complete(true);
                        }
                    });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> releaseTrailer(JsonObject body) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        vertx.eventBus().send(TrailersDBV.class.getSimpleName(), body, options(TrailersDBV.ACTION_RELEASE), (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void closeShipmentLoad(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY,context.<Integer>get(USER_ID));

            if (body.containsKey(TERMINAL_ID)){
                Integer terminalId = body.getInteger(TERMINAL_ID);
                body.put(TERMINAL_ID, terminalId);
                isGraterAndNotNull(body, TERMINAL_ID,0);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }

            isGraterAndNotNull(body,_SHIPMENT_ID,0);
            isGrater(body, TRAILER_ID, 0);
            isEmptyAndNotNull(body, LEFT_STAMP);
            isEmptyAndNotNull(body, RIGHT_STAMP);
            isEmpty(body, ADDITIONAL_STAMP);
            isEmpty(body, REPLACEMENT_STAMP);
            isEmpty(body, _FIFTH_STAMP);
            isEmpty(body, _SIXTH_STAMP);
            isGrater(body, _SECOND_TRAILER_ID, 0);
            isEmpty(body, _SECOND_LEFT_STAMP);
            isEmpty(body, _SECOND_RIGHT_STAMP);
            isEmpty(body, _SECOND_ADDITIONAL_STAMP);
            isEmpty(body, _SECOND_REPLACEMENT_STAMP);
            isEmpty(body, _SECOND_FIFTH_STAMP);
            isEmpty(body, _SECOND_SIXTH_STAMP);

            execEventBus(context,TravelTrackingDBV.CLOSE_LOAD,body,"Load close");
        } catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void maneuverReport(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");
            isContained(body, "type", "arrivals", "departures", "both");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, TravelTrackingDBV.MANEUVER_TIME_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }

    }


    private void initShipmentDownload(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY,context.<Integer>get(USER_ID))
                    .put("shipment_type", ShipmentsDBV.SHIPMENT_TYPES.DOWNLOAD.getName());

            if (body.containsKey(TERMINAL_ID)){
                Integer terminalId = body.getInteger(TERMINAL_ID);
                body.put(TERMINAL_ID, terminalId)
                        .put("origin", "web");
                isGraterAndNotNull(body, TERMINAL_ID,0);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }

            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID,0);
            isGrater(body, TRAILER_ID, 0);
            isEmptyAndNotNull(body, LEFT_STAMP);
            isEmptyAndNotNull(body, RIGHT_STAMP);
            isEmpty(body, ADDITIONAL_STAMP);
            isEmpty(body, REPLACEMENT_STAMP);
            isEmpty(body, _FIFTH_STAMP);
            isEmpty(body, _SIXTH_STAMP);
            isGrater(body, _SECOND_TRAILER_ID, 0);
            isEmpty(body, _SECOND_LEFT_STAMP);
            isEmpty(body, _SECOND_RIGHT_STAMP);
            isEmpty(body, _SECOND_ADDITIONAL_STAMP);
            isEmpty(body, _SECOND_REPLACEMENT_STAMP);
            isEmpty(body, _SECOND_FIFTH_STAMP);
            isEmpty(body, _SECOND_SIXTH_STAMP);

            execEventBus(context,TravelTrackingDBV.INIT_DOWNLOAD, body,"Downloading");

        } catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void initShipmentLoad(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY, context.<Integer>get(USER_ID));

            if (body.containsKey(TERMINAL_ID)){
                Integer terminalId = body.getInteger(TERMINAL_ID);
                body.put(TERMINAL_ID, terminalId).put(_ORIGIN, "web");
                isGraterAndNotNull(body, TERMINAL_ID,0);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }

            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID,0);
            isGraterAndNotNull(body, DRIVER_ID,0);
            isGrater(body, SECOND_DRIVER_ID,0);

            execEventBus(context,TravelTrackingDBV.INIT_LOAD,body,"Loading");
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }
    
    public void register(RoutingContext context){
        this.validateToken(context, userID->{
            try{
                EventBus eventBus = vertx.eventBus();
                JsonObject body = context.getBodyAsJson();
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, TravelTrackingDBV.REGISTER);
                body.put(CREATED_BY, userID);
                
                isGraterAndNotNull(body, "schedule_route_id", 0);
                isGraterAndNotNull(body, "schedule_route_destination_id", 0);
                isContainedAndNotNull(body, "status","loading","ready-to-go","in-transit","stopped","downloading","ready-to-load","paused","finished-ok");
                
                eventBus.send(TravelTrackingDBV.class.getSimpleName(), body, options, reply->{
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        }else{
                            responseOk(context, reply.result().body(), "Created");
                        }

                    }catch(Exception e){
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }

                });
                
            }catch(PropertyValueException ex){
                 responsePropertyValue(context, ex);
            }catch(Exception ex){
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex.getMessage());
            }
        });
    }

    private void initTravel(RoutingContext context){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        getDateByTerminal().setHandler(reply -> {
            if(reply.succeeded()){
                try{
                    String started_at = formatter.format(reply.result());
                    JsonObject messageBody = context.getBodyAsJson();
                    String coordinates = messageBody.getString("latitude")+","+messageBody.getString("longitude");
                    JsonObject body = new JsonObject()
                            .put("schedule_route_id",Integer.parseInt(context.request().getParam("schedule_route_id")))
                            .put("terminal_id",Integer.parseInt(context.request().getParam("terminal_origin")))
                            .put("status","in-transit")
                             .put("type_terminal","origin")
                            .put("location_started", coordinates)
                            .put("started_at" , started_at);

                    if (messageBody.containsKey(DRIVER_ID)){
                        Integer driverId = messageBody.getInteger(DRIVER_ID);
                        body.put(DRIVER_ID, driverId);
                    } else {
                        JsonObject employee = context.get(EMPLOYEE);
                        body.put(DRIVER_ID, employee.getInteger(ID));
                    }

                    UtilsValidation.isGraterAndNotNull(body,"schedule_route_id",0);
                    UtilsValidation.isGraterAndNotNull(body,DRIVER_ID,0);
                    UtilsValidation.isGraterAndNotNull(body,"terminal_id",0);
                    body.put(CREATED_BY,context.<Integer>get(USER_ID)).put("table","in-transit");

                    execEventBus(context, TravelTrackingDBV.INIT_TRAVEL,body,"Started");

                }catch(UtilsValidation.PropertyValueException ex){
                    ex.printStackTrace();
                    UtilsResponse.responsePropertyValue(context, ex);
                }catch (Exception e){
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado", e.getMessage());
                }
            }else{
                responseError(context, "Error al obtener la fecha de la terminal");
            }
        });

    }

    private void finishTravel(RoutingContext context){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        getDateByTerminal().setHandler(reply -> {
            if(reply.succeeded()){
                try{
                    String finished_at = formatter.format(reply.result());
                    JsonObject body = new JsonObject()
                            .put("schedule_route_id",Integer.parseInt(context.request().getParam("schedule_route_id")))
                            .put("terminal_id",Integer.parseInt(context.request().getParam("terminal_destiny")))
                            .put("status","finished-ok")
                            .put("type_terminal","destiny")
                            .put("finished_at" ,finished_at);
                    UtilsValidation.isGraterAndNotNull(body,"schedule_route_id",0);
                    UtilsValidation.isGraterAndNotNull(body,"terminal_id",0);
                    body.put(CREATED_BY,context.<Integer>get(USER_ID)).put("table","travel_tracking");
                    execEventBus(context,TravelTrackingDBV.CHECK_STATUS,body,"Finished");
                }catch(UtilsValidation.PropertyValueException ex){
                    ex.printStackTrace();
                    UtilsResponse.responsePropertyValue(context, ex);
                }catch (Exception e){
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado", e.getMessage());
                }
            }else{
                responseError(context, "Error al obtener la  fecha de la terminal");
            }
        });
    }


    private void pauseTravel(RoutingContext context){
        try{
            JsonObject messageBody = context.getBodyAsJson();
            HttpServerRequest request = context.request();
            String latitude = messageBody.getString("latitude");
            String longitude = messageBody.getString("longitude");
            String coordinates = latitude.concat(",").concat(longitude);

            JsonObject body = new JsonObject()
                    .put(CREATED_BY,context.<Integer>get(USER_ID)).put("table","paused")
                    .put(SCHEDULE_ROUTE_ID, Integer.parseInt(request.getParam(SCHEDULE_ROUTE_ID)))
                    .put(TERMINAL_ID, Integer.parseInt(request.getParam("terminal_origin")))
                    .put(STATUS, "paused")
                    .put("type_terminal","origin")
                    .put("location_started", coordinates);

            if (messageBody.containsKey(DRIVER_ID)){
                Integer driverId = messageBody.getInteger(DRIVER_ID);
                body.put(DRIVER_ID, driverId);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(DRIVER_ID, employee.getInteger(ID));
            }

            isGraterAndNotNull(body,SCHEDULE_ROUTE_ID,0);
            isGraterAndNotNull(body,TERMINAL_ID,0);

            execEventBus(context, TravelTrackingDBV.CHECK_STATUS,body,"Paused");

        } catch(PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }
    private String validateCoordinate(String lat, String lng){
        if (lat != null && lng != null)
            return lat.concat(",").concat(lng);
        return null;
    }

    private void stopTravel(RoutingContext context){
        try{
            JsonObject messageBody = context.getBodyAsJson();
            HttpServerRequest request = context.request();
            String latitude = messageBody.getString("latitude");
            String longitude = messageBody.getString("longitude");
            String coordinates = validateCoordinate(latitude,longitude);

            JsonObject body = new JsonObject()
                    .put(CREATED_BY,context.<Integer>get(USER_ID))
                    .put("table", "stopped")
                    .put(SCHEDULE_ROUTE_ID, Integer.parseInt(request.getParam(SCHEDULE_ROUTE_ID)))
                    .put(TERMINAL_ID, Integer.parseInt(request.getParam("terminal_destiny")))
                    .put(STATUS, "stopped")
                    .put("type_terminal","destiny")
                    .put("location_started", coordinates);

            if (messageBody.containsKey(DRIVER_ID)){
                Integer driverId = messageBody.getInteger(DRIVER_ID);
                body.put(DRIVER_ID, driverId);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(DRIVER_ID, employee.getInteger(ID));
            }

            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID,0);
            isGraterAndNotNull(body, TERMINAL_ID,0);
            isGraterAndNotNull(body, DRIVER_ID,0);

            execEventBus(context, TravelTrackingDBV.CHECK_STATUS, body,"Stopped");

        } catch(PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void execEventBus(RoutingContext context, String action, JsonObject body, String devMessage) {
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), devMessage);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, "Ocurrió un error inesperado", ex.getMessage());
            }
        });
    }

    private Future<Date> getDateByTerminal(){
        Future<Date> future = Future.future();
        try{

            Date today = UtilsDate.getDateConvertedTimeZone(UtilsDate.timezone, new Date());

            future.complete(today);
        }catch (Exception e){
            e.printStackTrace();
            future.fail(e);
        }
        return future;
    }


}
