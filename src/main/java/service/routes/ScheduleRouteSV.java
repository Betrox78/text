/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.boardingpass.BoardingPassDBV;
import database.branchoffices.BranchofficeDBV;
import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.employees.EmployeeDBV;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.Constants;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import service.promos.PromosSV;
import utils.UtilsDate;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicReference;

import static database.boardingpass.BoardingPassDBV.VEHICLE_ID;
import static database.promos.PromosDBV.*;
import static database.routes.ScheduleRouteDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ScheduleRouteSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ScheduleRouteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/schedulesRoutes";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        // Add all generic handlers
        this.addHandler(HttpMethod.GET, "/", AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/v2", AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/:id", AuthMiddleware.getInstance(), this::findById);
        this.addHandler(HttpMethod.GET, "/count", AuthMiddleware.getInstance(), this::count);
        this.addHandler(HttpMethod.GET, "/count/perPage/:num", AuthMiddleware.getInstance(), this::countPerPage);
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::create);
        this.addHandler(HttpMethod.PUT, "/", AuthMiddleware.getInstance(), this::update);
        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::deleteById);
        this.addHandler(HttpMethod.POST,"/scheduleRoutesList", AuthMiddleware.getInstance(), this:: getDeparturesCalendar);
        this.addHandler(HttpMethod.GET, "/scheduleRouteDetail/:scheduleRouteId", AuthMiddleware.getInstance(), this::getScheduleRouteDetail);
        this.addHandler(HttpMethod.GET, "/driver/getDestinations/:sr_id/:terminal_id", AuthMiddleware.getInstance(), this::getDestinations);

        // Custom handlers
        this.addHandler(HttpMethod.POST, "/removeScheduleRoute", AuthMiddleware.getInstance(), this :: removeScheduleRoute);
        this.addHandler(HttpMethod.GET,"/scheduleRouteAvailableSeats/:srid/:toid", AuthMiddleware.getInstance(),this::getScheduleRouteAvailableSeats);
        this.addHandler(HttpMethod.POST, "/availableRoutes", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getRoutesByTerminals);
        this.addHandler(HttpMethod.POST, "/public/availableRoutes", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getRoutesByTerminals);
        this.addHandler(HttpMethod.POST, "/availableOriginRoutes", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getRoutesByOrigin);
        this.addHandler(HttpMethod.POST, "/changeHideStatus/route" , AuthMiddleware.getInstance(),  this::changeHideStatus);

        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/templateList", AuthMiddleware.getInstance(),this::templateList);
        this.addHandler(HttpMethod.POST , "/templateDetail", AuthMiddleware.getInstance(), this::templateDetail);
        this.addHandler(HttpMethod.GET,"/getList/all", AuthMiddleware.getInstance(),this::scheduleRoutesList);
        this.addHandler(HttpMethod.GET,"/terminalOriginId/:toid/terminalDestinyId/:tdid/dateTravel/:dt", AuthMiddleware.getInstance(),this::searchByTerminal);
        this.addHandler(HttpMethod.GET,"/stops/:crid", AuthMiddleware.getInstance(),this::searchStops);
        this.addHandler(HttpMethod.POST, "/seats/lock", this::seatLock);
        this.addHandler(HttpMethod.POST, "/seats/unlock", this::seatUnlock);
        this.addHandler(HttpMethod.GET,"/seats/:srid", AuthMiddleware.getInstance(),this::searchSeats);
        this.addHandler(HttpMethod.GET,"/driver/scheduleRoutes/:date", AuthMiddleware.getInstance(),this::getScheduleRoutesByDriverId);
        this.addHandler(HttpMethod.GET,"/driver/scheduleRoutes/:date/:srid", AuthMiddleware.getInstance(),this::getScheduleRoutesByDriverByDateAndRoute);
        this.addHandler(HttpMethod.POST, "/driver/changeDriver",AuthMiddleware.getInstance(),this::changeDriver);
        this.addHandler(HttpMethod.POST, "/driver/changeSecondDriver",AuthMiddleware.getInstance(),this::changeSecondDriver);
        this.addHandler(HttpMethod.GET,"/driver/scheduleRouteAvailableSeats/:srid/:toid", AuthMiddleware.getInstance(),this::getScheduleRouteAvailableSeatsByDriverId);
        this.addHandler(HttpMethod.PATCH,"/driver/scheduleRoute/start/:srid", AuthMiddleware.getInstance(),this::driverStartScheduleRoute);
        this.addHandler(HttpMethod.PATCH,"/driver/scheduleRouteDestination/start/:sr_id", AuthMiddleware.getInstance(),this::driverStartScheduleRouteDestination);
        this.addHandler(HttpMethod.GET,"/driver/calculateTicketPrice/:current_position/:schedule_route_destination_id", AuthMiddleware.getInstance(),this::driverCalculateTicketPrice);
        this.addHandler(HttpMethod.GET,"/driver/availableSeat/:quantity/:schedule_route_id/:terminal_origin_id/:terminal_destiny_id", AuthMiddleware.getInstance(),this::driverGetAvailableSeat);
        this.addHandler(HttpMethod.POST, "/getInfoByDestiny", AuthMiddleware.getInstance(), this::getScheduleRouteInfoByDestinationId);
        this.addHandler(HttpMethod.POST, "/getInfoByVehicle", AuthMiddleware.getInstance(), this::getScheduleRouteInfoByVehicleId);
        this.addHandler(HttpMethod.POST, "/configRouteStatus", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getConfigRouteStatus);
        this.addHandler(HttpMethod.POST, "/changeVehicle/", AuthMiddleware.getInstance(), this::changeVehicle);
        this.addHandler(HttpMethod.POST, "/statusRouteReport", AuthMiddleware.getInstance(), this::statusRouteReport);
        super.start(startFuture);
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24(body, "travel_hour");
            isDate(body, "travel_date");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isHour24(body, "travel_hour");
            isDate(body, "travel_date");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    private void execEventBus(RoutingContext context, String action, JsonObject body, String devMessage){
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), devMessage);
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado", t);
            }
        });
    }

    private void getDestinations(RoutingContext context){
        try{
            JsonObject body = new JsonObject();
            body.put("driver_id",context.<Integer>get(USER_ID))
                .put("schedule_route_id",Integer.valueOf(context.request().getParam("sr_id")))
                .put("terminal_id",Integer.valueOf(context.request().getParam("terminal_id")));
            execEventBus(context,ScheduleRouteDBV.ACTION_GET_DRIVER_DESTINATIONS,body,"Found");
        }catch (Exception e){
            responseError(context, "Ocurrió un error inesperado", e.getMessage());
        }
    }


    private void changeHideStatus(RoutingContext context){
        try{
            JsonObject body = context.getBodyAsJson();

            vertx.eventBus().send(ScheduleRouteDBV.class.getSimpleName(), body, options(CHANGE_STATUS_HIDE_ROUTE), handler -> {
                try {
                    if(handler.failed()){
                        throw handler.cause();
                    }
                    // responseOk(context , new JsonObject().put("updates", handler.result()) ,"UPDATED" );
                    responseOk(context,"UPDATED" , body);

                }catch (Throwable ex){
                    responseError(context, "Error en el update", ex.getMessage());
                }
            });
        } catch (Exception e){
            responseError(context, "Ocurrio un error inesperado" , e.getMessage());
        }
    }

    private void register(RoutingContext context) {
        JsonObject message = context.getBodyAsJson();
        message.put(CREATED_BY, context.<Integer>get(USER_ID));

        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, ScheduleRouteDBV.ACTION_REGISTER);

        vertx.eventBus().send(this.getDBAddress(), message, options, (AsyncResult<Message<JsonArray>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    Boolean isTemplate = message.getBoolean("is_template");
                    String templateName = message.getString("template_name");
                    if(isTemplate == null || !isTemplate){
                        responseOk(context, reply.result().body(), "Created");
                    }else{
                        DeliveryOptions templateOption = new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.ACTION_CREATE_TEMPLATE);
                        JsonObject result = JsonObject.mapFrom(reply.result().body());
                        vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("template_name", templateName).put( "routes" , result), templateOption, repply ->{
                           try{
                                if(repply.failed()){
                                    throw new Exception("Error save template");
                                }
                               responseOk( context, reply.result().body(), "Create schedule and template");
                           } catch (Exception e){
                               e.printStackTrace();
                               responseWarning(context, "Error save template");
                           }
                        });
                   }
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }
    private void templateDetail(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body , "template_name");
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ScheduleRouteDBV.ACTION_GET_TEMPLATE_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, (AsyncResult<Message<JsonArray>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        }catch (PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getCause());
        }
    }
    private void templateList(RoutingContext context){
        JsonObject message = context.getBodyAsJson();
        try {
            isGraterAndNotNull(message, "config_route_id", 0);
            isGraterAndNotNull(message, "config_schedule_id", 0);
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ScheduleRouteDBV.ACTION_GET_TEMPLATE_LIST);
            vertx.eventBus().send(this.getDBAddress(), message, options, (AsyncResult<Message<JsonArray>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        }catch (PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getCause());
        }
    }

    private void scheduleRoutesList(RoutingContext context) {
        JsonObject message = new JsonObject();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, ScheduleRouteDBV.ACTION_SCHEDULE_ROUTES_LIST);

        vertx.eventBus().send(this.getDBAddress(), message, options, (AsyncResult<Message<JsonArray>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, t);
            }
        });
    }

    private void changeDriver(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, "schedule_route_id", 0);
            isGraterAndNotNull(body, "employee_id", 0);
            isGraterAndNotNull(body, "terminal_origin_id", 0);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.CHANGE_DRIVER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch (Exception e){
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void changeSecondDriver(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, "schedule_route_id", 0);
            isGraterAndNotNull(body, "employee_id", 0);
            isGraterAndNotNull(body, "terminal_origin_id", 0);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.CHANGE_SECOND_DRIVER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch (Exception e){
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void getConfigRouteStatus(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, "config_schedule_id", 0);
            UtilsValidation.isDateTimeAndNotNull(body, "travel_date","schedule_route");
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.GET_CONFIG_ROUTE_STATUS);
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
        }catch(UtilsValidation.PropertyValueException ex){
                UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }

    }

    private void getScheduleRoutesByDriverId(RoutingContext context) {
        this.getDriver(context, (JsonObject driver) -> vertx.eventBus().send(this.getDBAddress(),
            new JsonObject().put("driver", driver).put("date", context.request().getParam("date") ),
            options(ScheduleRouteDBV.ACTION_DRIVER_SCHEDULE_ROUTES),
            (AsyncResult<Message<Object>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, t);
            }
        }));

    }

    private void getScheduleRoutesByDriverByDateAndRoute(RoutingContext context) {
        this.getDriver(context, (JsonObject driver) -> {
            try {
                vertx.eventBus().send(this.getDBAddress(),
                        new JsonObject().put("driver", driver)
                                .put("scheduleRouteDestinationId", Integer.valueOf(context.request().getParam("srid")))
                                .put("date", context.request().getParam("date")),
                        options(ScheduleRouteDBV.ACTION_DRIVER_SCHEDULE_ROUTES_BY_ID),
                        (AsyncResult<Message<Object>> reply) -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }
                                responseOk(context, reply.result().body(), "Found");
                            } catch (Throwable t){
                                t.printStackTrace();
                                responseError(context, t);
                            }
                        });
            } catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, ex);
            }
        });

    }

    private void getDriver(RoutingContext context, Handler<JsonObject> handler) {
        // Get user employee
        vertx.eventBus().send(EmployeeDBV.class.getSimpleName(),
                new JsonObject().put(USER_ID, context.<Integer>get(USER_ID)),
                options(EmployeeDBV.ACTION_EMPLOYEE_BY_USERE_ID),
                (AsyncResult<Message<Object>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                handler.handle((JsonObject) reply.result().body());
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, t);
            }
        });
    }

    private void driverStartScheduleRoute(RoutingContext context) {
        this.getDriver(context, (JsonObject driver) -> vertx.eventBus().send(this.getDBAddress(),
                new JsonObject().put("driver", driver)
                        .put("schedule_route_id", Integer.valueOf(context.request().getParam("srid"))),
                options(ScheduleRouteDBV.ACTION_DRIVER_START_SCHEDULE_ROUTE),
                (AsyncResult<Message<Object>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Started");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, t);
            }
        }));
    }

    private void driverStartScheduleRouteDestination(RoutingContext context) {
        this.getDriver(context, (JsonObject driver) -> {

        });
    }

    private void getScheduleRouteAvailableSeats(RoutingContext context){
        try{
            JsonObject message = new JsonObject()
                    .put("schedule_route_id", Integer.valueOf(context.request().getParam("srid")))
                    .put("terminal_origin_id", Integer.valueOf(context.request().getParam("toid")));

            isGraterAndNotNull(message, "schedule_route_id", 0);
            isGraterAndNotNull(message, "terminal_origin_id", 0);

            execEventBus(context, ScheduleRouteDBV.ACTION_DRIVER_SCHEDULE_ROUTE_AVAILABLE_SEATS,message,"Found");
        }catch(UtilsValidation.PropertyValueException ex){
            UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception e){
            responseError(context, "Ocurrió un error inesperado", e.getMessage());
        }
    }

    private void getScheduleRouteAvailableSeatsByDriverId(RoutingContext context) {
        this.getDriver(context, (JsonObject driver) -> {
            HttpServerRequest request = context.request();
            try {
                // Validate params
                JsonObject message = new JsonObject().put("driver", driver)
                        .put("schedule_route_id", Integer.valueOf(request.getParam("srid")))
                        .put("terminal_origin_id", Integer.valueOf(request.getParam("toid")));

                isGraterAndNotNull(message, "schedule_route_id", 0);
                isGraterAndNotNull(message, "terminal_origin_id", 0);

                // Get available seats
                vertx.eventBus().send(this.getDBAddress(), message,
                        options(ScheduleRouteDBV.ACTION_DRIVER_SCHEDULE_ROUTE_AVAILABLE_SEATS), (AsyncResult<Message<Object>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, reply.result().body(), "Found");
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, t);
                    }
                    });
            } catch (UtilsValidation.PropertyValueException ex) {
                ex.printStackTrace();
                UtilsResponse.responsePropertyValue(context, ex);
            }
        });

    }

    private void searchByTerminal(RoutingContext context) {
        try {
            String stringDate = context.request().getParam("dt");

            Future<Message<JsonArray>> futureDepartureDetail = Future.future();
            Future<Message<JsonArray>> futureOneDayAfter = Future.future();
            Future<Message<JsonArray>> futureOneDayBefore = Future.future();
            Future<Message<JsonArray>> futureOneDay = Future.future();

            JsonObject departureDetail = new JsonObject()
                    .put("toid", Integer.valueOf(context.request().getParam("toid")))
                    .put("tdid", Integer.valueOf(context.request().getParam("tdid")))
                    .put(DATE_TRAVEL, stringDate);

            JsonObject oneDayAfter = departureDetail.copy();
            JsonObject oneDayBefore = departureDetail.copy();

            Date dateRequestet = UtilsDate.parse_yyyy_MM_dd(stringDate);
            Calendar cal = new GregorianCalendar();
            cal.setTime(dateRequestet);

            cal.add(Calendar.DAY_OF_YEAR, 1); //one day after the requested
            oneDayAfter.put(DATE_TRAVEL, UtilsDate.format_yyyy_MM_dd(cal.getTime()));

            cal.add(Calendar.DAY_OF_YEAR, -2);//one day before the requested
            oneDayBefore.put(DATE_TRAVEL, UtilsDate.format_yyyy_MM_dd(cal.getTime()));

            DeliveryOptions optionsDepartureDetail = new DeliveryOptions()
                    .addHeader(ACTION, ScheduleRouteDBV.ACTION_SEARCH_BY_TERMINALS);
            DeliveryOptions optionsOneDay = new DeliveryOptions()
                    .addHeader(ACTION, ScheduleRouteDBV.ACTION_SUMMARY_COST);

            vertx.eventBus().send(this.getDBAddress(), departureDetail, optionsDepartureDetail, futureDepartureDetail.completer());
            vertx.eventBus().send(this.getDBAddress(), departureDetail, optionsOneDay, futureOneDay.completer());
            vertx.eventBus().send(this.getDBAddress(), oneDayAfter, optionsOneDay, futureOneDayAfter.completer());
            vertx.eventBus().send(this.getDBAddress(), oneDayBefore, optionsOneDay, futureOneDayBefore.completer());

            CompositeFuture.all(futureDepartureDetail, futureOneDay, futureOneDayBefore, futureOneDayAfter).setHandler(reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    Message<JsonArray> replyDepartureDetail = reply.result().<Message<JsonArray>>resultAt(0);
                    Message<JsonObject> replyOneDay = reply.result().<Message<JsonObject>>resultAt(1);
                    Message<JsonObject> replyOneyDayBefore = reply.result().<Message<JsonObject>>resultAt(2);
                    Message<JsonObject> replyOneDayAfter = reply.result().<Message<JsonObject>>resultAt(3);

                    MultiMap headers = replyDepartureDetail.headers()
                            .addAll(replyOneDay.headers())
                            .addAll(replyOneDayAfter.headers())
                            .addAll(replyOneyDayBefore.headers());

                    if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, headers.getAll(ErrorCodes.DB_ERROR.name()));
                    } else {
                        JsonObject res = new JsonObject()
                                .put("detail", replyDepartureDetail.body())
                                .put("that_day", replyOneDay.body())
                                .put("one_day_after", replyOneyDayBefore.body())
                                .put("one_day_before", replyOneDayAfter.body());

                        responseOk(context, res);
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, t);
                }
            });
        } catch (ParseException ex) {
            ex.printStackTrace();
            UtilsResponse.responseWarning(context, ex.getMessage());
        }
    }

    private void searchStops(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put(CONFIG_ROUTE_ID, Integer.valueOf(context.request().getParam("crid")));

        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, ScheduleRouteDBV.ACTION_SEARCH_STOPS);

        vertx.eventBus().send(this.getDBAddress(), message, options, (AsyncResult<Message<JsonArray>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, t);
            }
        });
    }

    private void searchSeats(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put(SCHEDULE_ROUTE_ID, Integer.valueOf(context.request().getParam("srid")));
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, ScheduleRouteDBV.ACTION_SEARCH_SEATS);
        vertx.eventBus().send(this.getDBAddress(), message, options, (reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, t);
            }
        });
    }

    private void seatLock(RoutingContext context) {
        JsonObject message = context.getBodyAsJson();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, SEAT_LOCK);
        vertx.eventBus().send(this.getDBAddress(), message, options, (reply) -> {
            try {
                if (reply.failed()) {
                    reply.cause().printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, reply.cause());
                    return;
                }

                Object response = reply.result().body();
                if (response == null) {
                    responseError(context, "Unable to lock seat");
                    return;
                }

                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Locked");
                }

            } catch (Throwable throwable) {
                throwable.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, throwable);
            }
        });
    }


    private void seatUnlock(RoutingContext context) {
        JsonObject message = context.getBodyAsJson();
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, SEAT_UNLOCK);
        vertx.eventBus().send(this.getDBAddress(), message, options, (reply) -> {
            try {
                if (reply.failed()) {
                    reply.cause().printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, reply.cause());
                    return;
                }

                Object response = reply.result().body();
                if (response == null) {
                    responseError(context, "Unable to unlock seat");
                    return;
                }

                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Unlocked");
                }

            } catch (Throwable throwable) {
                throwable.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, throwable);
            }
        });
    }

    private void getDeparturesCalendar(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isDateTimeAndNotNull(body, "init_date","schedule_route");
            UtilsValidation.isDateTimeAndNotNull(body, "end_date", "schedule_route");
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION , ScheduleRouteDBV.DEPARTURES_CALENDAR_LIST);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply->{
               try{
                   if(reply.failed()){
                       throw new Exception(reply.cause());
                   }
                   responseOk(context, reply.result().body());
               } catch (Exception ex){
                   ex.printStackTrace();
                   responseError(context, UNEXPECTED_ERROR, ex.getMessage());
               }
            });
        }catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context , UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    private void getScheduleRouteDetail(RoutingContext context){
        JsonObject body = new JsonObject();
        try {
        body.put("schedule_route_id",Integer.valueOf(context.request().getParam("scheduleRouteId")));
        isGraterAndNotNull(body, "schedule_route_id",0);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION , ScheduleRouteDBV.GET_SCHEDULE_ROUTE_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception ex){
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, ex);
                }
            });
        }catch (PropertyValueException e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e);
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context , UNEXPECTED_ERROR, ex);
        }
    }

    private void removeScheduleRoute(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try{
            UtilsValidation.isGraterAndNotNull(body , "schedule_route_id", 0);
            UtilsValidation.isGraterAndNotNull(body , "terminal_origin_id", 0);
            UtilsValidation.isGraterAndNotNull(body , "terminal_destiny_id", 0);
            vertx.eventBus().send(ScheduleRouteDBV.class.getSimpleName(), body , options(ScheduleRouteDBV.ACTION_SEARCH_SCHEDULE_DETAIL), searchRepply->{
                try{
                    if (searchRepply.failed()){
                        throw new Exception(searchRepply.cause());
                    }
                    JsonObject searchResult = (JsonObject) searchRepply.result().body();
                    if(searchResult.isEmpty()){
                        throw new Exception("Schedule route id not found");
                    }
                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(),searchResult, options(BoardingPassDBV.SEATS), reply ->{
                        try{
                            if(reply.failed()){
                                throw new Exception("Schedule route destination not found");
                            }
                            JsonObject  result = (JsonObject) reply.result().body();
                            if(!result.getJsonArray("busy_seats").isEmpty()){
                                throw  new Exception("It is not possible to delete the route.Already have tickets sold");
                            }
                            vertx.eventBus().send(ScheduleRouteDBV.class.getSimpleName(), searchResult , options(ScheduleRouteDBV.ACTION_REMOVE_SCHEDULE_ROUTE), repply->{
                                try{
                                    if (repply.failed()){
                                        throw new Exception("Schedule route remove failed.");
                                    }
                                    JsonObject resultt = (JsonObject) repply.result().body();
                                    responseOk(context , resultt.getJsonObject("result") ,"UPDATED" );
                                } catch (Exception ex){
                                    ex.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, ex);
                                }
                            });
                        }catch (Exception e) {
                            e.printStackTrace();
                            responseError(context , e.getMessage() , e);
                        }
                    });
                } catch (Exception ex){
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, ex);
                }
            });
        }catch (PropertyValueException e){
            UtilsResponse.responsePropertyValue(context, e);
            e.printStackTrace();
        } catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }

    private void getRoutesByTerminals(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            // cambio rutas de allende por leyva
            if(body.getInteger("LMM01")!=null)
                if(body.getInteger("LMM01")==1){
                    body.remove("tdid");
                    body.put("tdid",body.getInteger("LMM01"));
                }


            // routes_time_before_travel_date
            UtilsValidation.isDate(body, DATE_TRAVEL);
            UtilsValidation.isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            UtilsValidation.isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);
            UtilsValidation.isGrater(body, PURCHASE_ORIGIN, 0);
            UtilsValidation.isGrater(body, PromosDBV.CUSTOMER_ID, 0);

            Integer customerId = body.getInteger(PromosDBV.CUSTOMER_ID);
            JsonArray specialTickets = body.getJsonArray(SPECIAL_TICKETS_LIST);
            String dateTravel = body.getString(DATE_TRAVEL);

            JsonObject branchValid= new JsonObject().put("idOrigin",body.getInteger("toid")).put("idDest",body.getInteger("tdid"));
            if (specialTickets == null) {
                throw new UtilsValidation.PropertyValueException("stl", "missing required value");
            }
            if (dateTravel == null) {
                throw new UtilsValidation.PropertyValueException("dt", "missing required value");
            }
            JsonObject bodySetting = new JsonObject().put("fieldName", "routes_time_before_travel_date");
            if(body.containsKey("origen_request")) {
                if (body.getString("origen_request").equals("pos")) {
                    bodySetting.put("fieldName", "routes_time_before_travel_date_pos");
                }
            }

            vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options(BranchofficeDBV.ACTION_REPORT_BRANCHOFFICE), (reply0) -> {
                try {
                    if (reply0.failed()){
                        throw reply0.cause();
                    }
                    JsonObject branches = (JsonObject) reply0.result().body();
                    JsonArray arrayBranches=(JsonArray) branches.getJsonArray("results") ;
                        arrayBranches.forEach(t->{
                            JsonArray arrayBranchoffice= (JsonArray) t;
                            int idOrigin=branchValid.getInteger("idOrigin");
                            int idDest=branchValid.getInteger("idDest");
                            int value=Integer.parseInt(arrayBranchoffice.getList().get(0).toString());
                            if(idOrigin==value){
                                    branchValid.put("prefixOrigin",arrayBranchoffice.getString(1)).put("cityOrigin",arrayBranchoffice.getInteger(3));
                            }
                            if(idDest==value){
                                branchValid.put("prefixDest",arrayBranchoffice.getString(1)).put("cityDest",arrayBranchoffice.getInteger(3)) ;
                            }
                        });

                    if( (  branchValid.getInteger("cityOrigin")==branchValid.getInteger("cityDest") ||branchValid.getString("prefixOrigin").equals("GML01") || branchValid.getString("prefixDest").equals("GML01"))){
                        responseOk(context, new JsonObject().put("[]",0), "Ruta invalida");

                    }else{
                        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), bodySetting,
                                options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), (AsyncResult<Message<JsonObject>> replySetting) -> {
                                    try {
                                        if (replySetting.failed()) {
                                            throw new Exception(replySetting.cause());
                                        }

                                        JsonObject resultSetting = replySetting.result().body();
                                        Integer minutes = Integer.valueOf(resultSetting.getString("value", "30"));
                                        body.put("minutes", minutes);

                                        Integer purchaseOrigin = (Integer) body.remove(PURCHASE_ORIGIN);

                                        if (purchaseOrigin != null && (body.getInteger("toid")!=14 && body.getInteger("tdid")!=14)){
                                            String fieldName = purchaseOrigin.equals(2) ? "promo_web" : "promo_app_cliente";
                                            JsonObject bodyGeneralPromo = new JsonObject().put("fieldName", fieldName);

                                            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), bodyGeneralPromo,
                                                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyGS -> {
                                                        try {
                                                            if (replyGS.failed()){
                                                                throw replyGS.cause();
                                                            }

                                                            JsonObject generalPromo = (JsonObject) replyGS.result().body();
                                                            Integer generalPromoId = Integer.parseInt(generalPromo.getString(VALUE));

                                                            JsonObject bodyGO = new JsonObject()
                                                                    .put(PromosDBV.CUSTOMER_ID, customerId == null ? 0 : customerId)
                                                                    .put(GENERAL_PROMO_ID, generalPromoId)
                                                                    .put(PURCHASE_ORIGIN, purchaseOrigin);

                                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyGO,
                                                                    new DeliveryOptions().addHeader(ACTION, PromosDBV.ACTION_GET_GENERAL_BY_ORIGIN), replyGO -> {
                                                                        try {
                                                                            if (replyGO.failed()){
                                                                                throw replyGO.cause();
                                                                            }


                                                                            JsonObject initialPromo = (JsonObject) replyGO.result().body();

                                                                            if(initialPromo.isEmpty()) {
                                                                                body.put(FLAG_PROMO, false).put(PROMO_DISCOUNT, initialPromo);

                                                                                vertx.eventBus().send(this.getDBAddress(), body, options(ScheduleRouteDBV.ACTION_GET_ROUTES_BY_TERMINALS), (reply) -> {
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
                                                                            } else {
                                                                                JsonArray specialTicketsIds = new JsonArray();
                                                                                AtomicReference<Integer> numProducts = new AtomicReference<>(0);
                                                                                specialTickets.forEach(st -> {
                                                                                    if(st instanceof JsonObject){
                                                                                        specialTicketsIds.add(((JsonObject) st).getInteger(ID));
                                                                                        numProducts.updateAndGet(v -> v + ((JsonObject) st).getInteger("number"));
                                                                                    }
                                                                                });
                                                                                if(body.getBoolean("is_return", false)) {
                                                                                    numProducts.updateAndGet(v -> v * 2);
                                                                                }
                                                                                JsonObject discount = new JsonObject()
                                                                                        .put(PromosDBV.DATE, body.getString(DATE_TRAVEL))
                                                                                        .put(RULE, body.getString(RULE))
                                                                                        .put(DISCOUNT_CODE, initialPromo.getString(DISCOUNT_CODE))
                                                                                        .put(SERVICE, SERVICES.boardingpass)
                                                                                        .put(PURCHASE_ORIGIN, purchaseOrigin)
                                                                                        .put(SPECIAL_TICKET_ID, specialTicketsIds)
                                                                                        .put(NUM_PRODUCTS, numProducts.get());

                                                                                if(customerId != null) {
                                                                                    discount.put(PromosDBV.CUSTOMER_ID, customerId);
                                                                                }

                                                                                if (new PromosSV().isValidCheckPromoCodeData(context, discount)){
                                                                                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_CHECK_PROMO_CODE);
                                                                                    vertx.eventBus().send(PromosDBV.class.getSimpleName(), discount, options, replyValidate -> {
                                                                                        Boolean flagPromo = false;
                                                                                        if (replyValidate.succeeded()){
                                                                                            JsonObject resultCheckPromoCode = (JsonObject) replyValidate.result().body();
                                                                                            flagPromo = resultCheckPromoCode.getBoolean(FLAG_PROMO, false);
                                                                                        }
                                                                                        body.put(FLAG_PROMO, flagPromo).put(PROMO_DISCOUNT, initialPromo);

                                                                                        vertx.eventBus().send(this.getDBAddress(), body, options(ScheduleRouteDBV.ACTION_GET_ROUTES_BY_TERMINALS), (reply) -> {
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
                                                                                    });
                                                                                }

                                                                            }
                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                                                                        }
                                                                    });

                                                        } catch (Throwable t){
                                                            t.printStackTrace();
                                                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                                                        }
                                                    });
                                        } else {
                                            body.put(FLAG_PROMO, false);
                                            vertx.eventBus().send(this.getDBAddress(), body, options(ScheduleRouteDBV.ACTION_GET_ROUTES_BY_TERMINALS), (reply) -> {
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
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                                    }
                                });

                    }

                } catch (Throwable terr){
                    terr.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, terr.getMessage());
                }
            });


        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getRoutesByOrigin(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            UtilsValidation.isGraterAndNotNull(body, "terminal_origin", 0);
            String dateTravel = body.getString("travel_date");
            if (dateTravel == null) {
                throw new UtilsValidation.PropertyValueException("travel_date", "missing required value");
            }

            JsonObject bodySetting = new JsonObject().put("fieldName", "routes_time_before_travel_date");

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), bodySetting,
                    options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), (AsyncResult<Message<JsonObject>> replySetting) -> {
                        try {
                            if (replySetting.failed()) {
                                throw new Exception(replySetting.cause());
                            }

                            JsonObject resultSetting = replySetting.result().body();
                            Integer minutes = Integer.valueOf(resultSetting.getString("value", "30"));
                            body.put("minutes", minutes);
                            DeliveryOptions options = new DeliveryOptions()
                                    .addHeader(ACTION, ScheduleRouteDBV.ACTION_AVAILABLE_ROUTES_FROM_ORIGIIN);

                            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                                try {
                                    if (reply.failed()){
                                        throw reply.cause();
                                    }
                                    responseOk(context, reply.result().body(), "Found");
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, t);
                                }
                            });
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void driverCalculateTicketPrice(RoutingContext context){
        try{
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put(CURRENT_POSITION, request.getParam(CURRENT_POSITION))
                    .put(BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID, Integer.parseInt(request.getParam(BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID)));

            isEmptyAndNotNull(body, CURRENT_POSITION);
            isGraterAndNotNull(body, BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID, 0);


            Future f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "baggage_cost"),
                    options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            Future f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "driver_max_complements"),
                    options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            Future f3 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "driver_free_complements_limit"),
                    options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(replyGS -> {
               try {
                   if (replyGS.failed()){
                       throw replyGS.cause();
                   }

                   Message<JsonObject> driverBG = replyGS.result().resultAt(0);
                   Message<JsonObject> driverMAXC = replyGS.result().resultAt(1);
                   Message<JsonObject> driverFCL = replyGS.result().resultAt(2);

                   Double driverBaggageCost = Double.parseDouble(driverBG.body().getString("value"));
                   Integer driverMaxComplements = Integer.parseInt(driverMAXC.body().getString("value"));
                   Integer driverFreeComplementsLimit = Integer.parseInt(driverFCL.body().getString("value"));

                   DeliveryOptions options = new DeliveryOptions()
                           .addHeader(ACTION, ScheduleRouteDBV.ACTION_DRIVER_CALCULATE_TICKET_PRICE);

                   vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                       try {
                           if (reply.failed()){
                               throw reply.cause();
                           }
                           JsonObject result = (JsonObject) reply.result().body();
                           result
                               .put("baggage_cost", driverBaggageCost)
                                .put("driver_max_complements", driverMaxComplements)
                                .put("driver_free_complements_limit", driverFreeComplementsLimit);
                           responseOk(context, result, "Calculate");
                       } catch (Throwable t){
                           t.printStackTrace();
                           responseError(context, UNEXPECTED_ERROR, t);
                       }
                   });

               } catch (Throwable t){
                   t.printStackTrace();
                   responseError(context, t);
               }
            });
        }catch(UtilsValidation.PropertyValueException ex){
            UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception e){
            responseError(context, "Ocurrió un error inesperado", e.getMessage());
        }
    }

    private void driverGetAvailableSeat(RoutingContext context){
        try{
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put("quantity", Integer.parseInt(request.getParam("quantity")))
                    .put(BoardingPassDBV.SCHEDULE_ROUTE_ID, Integer.parseInt(request.getParam(BoardingPassDBV.SCHEDULE_ROUTE_ID)))
                    .put(BoardingPassDBV.TERMINAL_ORIGIN_ID, Integer.parseInt(request.getParam(BoardingPassDBV.TERMINAL_ORIGIN_ID)))
                    .put(BoardingPassDBV.TERMINAL_DESTINY_ID, Integer.parseInt(request.getParam(BoardingPassDBV.TERMINAL_DESTINY_ID)));

            isGraterAndNotNull(body, "quantity", 0);
            isGraterAndNotNull(body, BoardingPassDBV.SCHEDULE_ROUTE_ID, 0);
            isGraterAndNotNull(body, BoardingPassDBV.TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, BoardingPassDBV.TERMINAL_DESTINY_ID, 0);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ScheduleRouteDBV.ACTION_DRIVER_GET_AVAILABLE_SEAT);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
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

        }catch(UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado", e.getMessage());
        }
    }

    private void getScheduleRouteInfoByVehicleId(RoutingContext context){
        try {
            JsonObject request = context.getBodyAsJson();

            String initDate = request.getString("init_date");
            String endDate = request.getString("end_date");

            Integer terminalOriginId = request.getInteger(BoardingPassDBV.TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = request.getInteger(BoardingPassDBV.TERMINAL_DESTINY_ID);
            Integer vehicleId = request.getInteger(BoardingPassDBV.VEHICLE_ID);

            JsonObject body = new JsonObject()
                    .put("init_date", initDate)
                    .put("end_date", endDate)
                    .put(BoardingPassDBV.TERMINAL_ORIGIN_ID, terminalOriginId)
                    .put(BoardingPassDBV.TERMINAL_DESTINY_ID, terminalDestinyId)
                    .put(VEHICLE_ID, vehicleId);

            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");
            isGraterEqualAndNotNull(body, BoardingPassDBV.VEHICLE_ID, 0);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ACTION_GET_SCHEDULE_ROUTE_INFO_BY_VEHICLE_ID);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
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
        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }

    }

    private void getScheduleRouteInfoByDestinationId(RoutingContext context){
        try {
            JsonObject request = context.getBodyAsJson();

            String initDate = request.getString("init_date");
            String endDate = request.getString("end_date");

            Integer terminalOriginId = request.getInteger(BoardingPassDBV.TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = request.getInteger(BoardingPassDBV.TERMINAL_DESTINY_ID);
            Integer vehicleId = request.getInteger(BoardingPassDBV.VEHICLE_ID);

            JsonObject body = new JsonObject()
                    .put("init_date", initDate)
                    .put("end_date", endDate)
                    .put(BoardingPassDBV.VEHICLE_ID, vehicleId)
                    .put(BoardingPassDBV.TERMINAL_ORIGIN_ID, terminalOriginId)
                    .put(BoardingPassDBV.TERMINAL_DESTINY_ID, terminalDestinyId);

            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ACTION_GET_SCHEDULE_ROUTE_INFO_BY_DESTINATION_ID);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
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
        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void changeVehicle(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, BoardingPassDBV.SCHEDULE_ROUTE_ID, 0);
            isGraterAndNotNull(body, VEHICLE_ID, 0);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ACTION_CHANGE_VEHICLE);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body(), "Updated");

                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void statusRouteReport(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, GET_ROUTE_STATUS_REPORT);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body(), "OK");

                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        }catch(Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }

    }
}
