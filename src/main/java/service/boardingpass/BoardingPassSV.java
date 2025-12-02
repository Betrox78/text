/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.boardingpass;

import database.boardingpass.BoardingPassDBV;
import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.employees.EmployeeDBV;
import database.money.CashOutDBV;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import service.commons.Constants;
import service.commons.ServiceVerticle;
import service.commons.middlewares.*;
import utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.*;
import static database.boardingpass.handlers.PartnerEndRegister.TOKEN_ID;
import static database.money.ExpenseDBV.CURRENCY_ID;
import static database.promos.PromosDBV.FLAG_PROMO;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author ulises
 */
public class BoardingPassSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BoardingPassDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/boardingPasses";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST,"/driver/make/end", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), this::driverEndRegister);
        this.addHandler(HttpMethod.POST, "/salesCancelReport", AuthMiddleware.getInstance(), this::salesCancelReport);
        this.addHandler(HttpMethod.POST,"/register/end", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), PromosMiddleware.getInstance(vertx), PayMiddleware.getInstance(vertx, "boarding_pass"),CreditMiddleware.getInstance(vertx), this::endRegister);
        this.addHandler(HttpMethod.POST, "/partner/register/end", PromosMiddleware.getInstance(vertx), PayMiddleware.getInstance(vertx, "boarding_pass"), this::endRegisterPartner);
        this.addHandler(HttpMethod.POST,"/driver/calculate/end", AuthMiddleware.getInstance(),this::calculateEnd);
        this.addHandler(HttpMethod.POST, "/advancedSearch", AuthMiddleware.getInstance(), this::advancedSearch);
        this.addHandler(HttpMethod.GET, "/reservationDetail/:reservation_code", AuthMiddleware.getInstance(), this::reservationDetail);
        this.addHandler(HttpMethod.GET, "/publicReservationDetail/:reservation_code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::publicReservationDetail);
        this.addHandler(HttpMethod.GET, "/runState/:reservation_code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::runStatus);
        this.addHandler(HttpMethod.GET, "/print/terms/:reservation_code", AuthMiddleware.getInstance(), this::printTermsConditions);
        this.addHandler(HttpMethod.POST, "/tickets/:reservation_code", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), this::getTickets);
        this.addHandler(HttpMethod.POST, "/print/tickets/:reservation_code", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), this::printTickets);
        this.addHandler(HttpMethod.DELETE, "/register/init/cancel/:reservation_code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::cancelRegisterByReservationCode);
        this.addHandler(HttpMethod.GET, "/seats/:srdid", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getSeats);
        this.addHandler(HttpMethod.POST, "/report", AuthMiddleware.getInstance(), this::report);
        this.addHandler(HttpMethod.POST, "/report/accountant", AuthMiddleware.getInstance(), this::reportAccountant);
        this.addHandler(HttpMethod.POST, "/report/totals", AuthMiddleware.getInstance(), this::reportTotals);
        this.addHandler(HttpMethod.POST, "/report/totals/accountant", AuthMiddleware.getInstance(), this::reportTotalsAccountant);
        this.addHandler(HttpMethod.POST, "/exchangeReservation", AuthMiddleware.getInstance(), this::exchangeReservation);
        this.addHandler(HttpMethod.GET, "/seats/:srid/:toid/:tdid", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getSeats);
        this.addHandler(HttpMethod.POST, "/register/checkin/make", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), PromosMiddleware.getInstance(vertx),CreditMiddleware.getInstance(vertx), this::makeCheckIn);
        this.addHandler(HttpMethod.PUT, "/register/cancel/:reservation_code", AuthMiddleware.getInstance(), this::cancelReservation);
        this.addHandler(HttpMethod.POST, "/cancelReservation", AuthMiddleware.getInstance(), this::cancelReservationWithoutCashOut);
        this.addHandler(HttpMethod.POST, "/report/occupation", AuthMiddleware.getInstance(), this::occupationReport);
        this.addHandler(HttpMethod.POST, "/configPrices/list", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::configPricesList);
        this.addHandler(HttpMethod.POST, "/report/phoneReservation", AuthMiddleware.getInstance(), this::getPhoneReservations);
        this.addHandler(HttpMethod.POST, "/reportGeneral", AuthMiddleware.getInstance(), this::reportGeneral); //webdev
        this.addHandler(HttpMethod.POST, "/reportAppSitio",AuthMiddleware.getInstance(),this::reportAppSitio); //webdev
        this.addHandler(HttpMethod.POST, "/reportMonth", AuthMiddleware.getInstance(), this::reportMonth); //webdev
        this.addHandler(HttpMethod.POST, "/reportPassengerType", AuthMiddleware.getInstance(), this::reportPassengerType);
        this.addHandler(HttpMethod.POST,"/reportTravelFrequency", AuthMiddleware.getInstance(), this::reportTravelFrequency);
        this.addHandler(HttpMethod.POST, "/partialCancelReservation", AuthMiddleware.getInstance(), this::partialCancelReservation);
        this.addHandler(HttpMethod.POST, "/changeReservation", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), this::changeReservation);
        this.addHandler(HttpMethod.GET, "/pricesList", AuthMiddleware.getInstance(), this::pricesList);
        this.addHandler(HttpMethod.GET, "/pricesListRegistered", AuthMiddleware.getInstance(), this::pricesListRegistered);
        this.addHandler(HttpMethod.POST, "/pricesListDetail", AuthMiddleware.getInstance(), this::pricesListDetail);
        this.addHandler(HttpMethod.POST, "/registerPricesList", AuthMiddleware.getInstance(), this::registerPriceList);
        this.addHandler(HttpMethod.POST, "/priceListApply",PriceListMiddleware.getInstance(vertx), AuthMiddleware.getInstance(), this::priceListApply);
        this.addHandler(HttpMethod.POST, "/priceListStatus", AuthMiddleware.getInstance(), this::priceListStatus);
        super.start(startFuture);
        this.router.post("/ticketsComplements/:reservation_code").handler(BodyHandler.create());
        this.router.post("/ticketsComplements/:reservation_code").handler(this::getTicketsComplements);
        this.router.get("/cancel/:id").handler(this::cancelRegister);
        this.router.get("/travelDetail/:reservation_code").handler(this::checkinTravelDetail);
        this.router.get("/travelsDetail/:boardingPassStatus").handler(this::travelsDetail);
        this.router.get("/travelsDetail/:customer_id/:boardingpass_status").handler(this::travelsDetail);
        /*this.router.get("/seats/:srdid").handler(this::setPublic);
        this.router.get("/seats/:srdid").handler(this::getSeats);*/
        this.router.post("/register/init").handler(this::setPublic);
        this.router.post("/register/init").handler(BodyHandler.create());
        this.router.post("/register/init").handler(CreditMiddleware.getInstance(vertx));
        this.router.post("/register/init").handler(this::initRegisterWithReservationCode);
        /*this.router.post("/register/end").handler(BodyHandler.create());
        this.router.post("/register/end").handler(this::endRegister);*/
        /*this.router.post("/register/checkin/make").handler(BodyHandler.create());
        this.router.post("/register/checkin/make").handler(this::makeCheckIn);*/
        this.router.post("/register/checkin/calculate").handler(BodyHandler.create());
        this.router.post("/register/checkin/calculate").handler(this::calculateCheckIn);
        this.router.post("/register/change/passengers").handler(BodyHandler.create());
        this.router.post("/register/change/passengers").handler(this::changePassengers);
        this.router.post("/register/checkin/calculateAgain").handler(BodyHandler.create());
        this.router.post("/register/checkin/calculateAgain").handler(this::calculateCheckInAgain);
        this.addHandler(HttpMethod.POST, "/register/checkin/make_again", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), PromosMiddleware.getInstance(vertx),CreditMiddleware.getInstance(vertx), this::makeCheckInAgain);
        this.addHandler(HttpMethod.POST, "/register/initPrepaid", AuthMiddleware.getInstance(), this::initRegisterPrepaid);
        this.addHandler(HttpMethod.POST,"/register/endPrepaid", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), PromosMiddleware.getInstance(vertx), PayMiddleware.getInstance(vertx, "boarding_pass"),CreditMiddleware.getInstance(vertx), this::endRegisterPrepaid);
        this.addHandler(HttpMethod.POST, "/cancelPrepaidTravelInit", AuthMiddleware.getInstance(), this::cancelPrepaidTravelInit);
    }

    private void getTickets(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                .put("tickets", context.getBodyAsJson().getJsonArray("tickets"))
                .put("reservation_code", request.getParam("reservation_code"))
                .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID))
                .put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.TICKETS);
            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    Message<Object> result = reply.result();
                    MultiMap headers = reply.result().headers();
                    if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                    } else {
                        responseOk(context, result.body(), "Found");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }

    }

    private void printTickets(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                .put("tickets", context.getBodyAsJson().getJsonArray("tickets"))
                .put("reservation_code", request.getParam("reservation_code"))
                .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID))
                .put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PRINT_TICKETS);
            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    Message<Object> result = reply.result();
                    MultiMap headers = reply.result().headers();
                    if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                    } else {
                        responseOk(context, result.body(), "Found");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void getTicketsComplements(RoutingContext context) {
        HttpServerRequest request = context.request();
        String jwt = request.getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject body = new JsonObject()
                    .put("tickets", context.getBodyAsJson().getJsonArray("tickets"))
                    .put("reservation_code", request.getParam("reservation_code"))
                    .put("created_by", UtilsJWT.getUserIdFrom(jwt));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.TICKETS_COMPLEMENTS);
            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options,
                    reply -> {
                        try{
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            Message<Object> result = reply.result();
                            MultiMap headers = reply.result().headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                Object body_ = result.body();

                                responseOk(context, body_, "Found");
                            }
                        }catch (Exception ex) {
                            ex.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }

                    }
            );
        } else {
            responseWarning(context, "Out of session", "Sessión json web token is invalid");
        }
    }

    private void exchangeReservation(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            body.put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.EXCHANGE_RESERVATION_CODE);
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
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void reportGeneral(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT_GENERAL);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportAppSitio(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORTAPP);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportMonth(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT_MONTH);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    //WEBDEV TERMINA
    private void changeReservation(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID))
                .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID));
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CHANGE_RESERVATION);
        try {
            JsonArray tickets = body.containsKey("tickets") ? body.getJsonArray("tickets") : null;
            UtilsValidation.isEmptyAndNotNull(body, RESERVATION_CODE);
            if(tickets == null || tickets.isEmpty())
                throw new Exception("No tickets object was found");

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();
            Future<Message<JsonObject>> f5 = Future.future();
            Future<Message<JsonObject>> f6 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", CURRENCY_ID),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", IVA), new DeliveryOptions()
                    .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), new JsonObject().put("tickets", body.getJsonArray("tickets"))
                    .put("passengers", body.getJsonArray("passengers")), new DeliveryOptions()
                    .addHeader(ACTION, GET_TICKETS_PRICE), f3.completer());

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "under_age"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "reservation_time"), new DeliveryOptions()
                    .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "changes_permited_on_travel"), new DeliveryOptions()
                    .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f6.completer());

            CompositeFuture.all(f1, f2, f3, f4, f5, f6).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()) {
                        throw detailReply.cause();
                    }

                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> ticketPricesMsg = detailReply.result().resultAt(2);
                    Message<JsonObject> underAgeMsg = detailReply.result().resultAt(3);
                    Message<JsonObject> timeReservationMsg = detailReply.result().resultAt(4);
                    Message<JsonObject> permitedChangesMsg = detailReply.result().resultAt(5);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject ticketPrices = ticketPricesMsg.body();
                    JsonObject underAge = underAgeMsg.body();
                    JsonObject reservationTime = timeReservationMsg.body();
                    JsonObject permitedChanges = permitedChangesMsg.body();

                    body.put(CURRENCY_ID, Integer.valueOf(currencyId.getString(VALUE)))
                            .put("under_age", Integer.valueOf(underAge.getString(VALUE)))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString(VALUE)))
                            .put("tickets", ticketPrices.getJsonArray("tickets"))
                            .put("permitedChanges", Integer.valueOf(permitedChanges.getString(VALUE)))
                            .put("reservation_time", Integer.valueOf(reservationTime.getString(VALUE)));

                    vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            responseOk(context, reply.result().body());
                        } catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void getSeats(RoutingContext context) {
        HttpServerRequest request = context.request();
        JsonObject body = new JsonObject();
        DeliveryOptions options;
        try {
            if (request.getParam("srdid") != null){
                body.put("schedule_route_destination_id", Integer.valueOf(request.getParam("srdid")));
                isGraterAndNotNull(body, "schedule_route_destination_id", 0);
                options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.SEATS);
            } else {
                body.put("schedule_route_id", Integer.valueOf(request.getParam("srid")));
                body.put("terminal_origin_id", Integer.valueOf(request.getParam("toid")));
                body.put("terminal_destiny_id", Integer.valueOf(request.getParam("tdid")));
                isGraterAndNotNull(body, "schedule_route_id", 0);
                isGraterAndNotNull(body, "terminal_origin_id", 0);
                isGraterAndNotNull(body, "terminal_destiny_id", 0);
                options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.SEATS_WITH_DESTINATION);
            }
            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options,
                    reply -> {
                        try{
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            Message<Object> result = reply.result();
                            MultiMap headers = reply.result().headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, result.body(), "Found");
                            }
                        }catch (Exception ex) {
                            ex.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }
                    }
            );
        } catch (UtilsValidation.PropertyValueException ex){
            responseError(context, ex);
        }
    }

    private void printTermsConditions(RoutingContext context) {
        JsonObject body = new JsonObject()
                .put("reservation_code", context.request().getParam("reservation_code"))
                .put("user_id", context.<Integer>get("user_id"));

        Integer userId = context.<Integer>get("user_id");

        Future<Message<JsonObject>> f1 = Future.future();
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                new JsonObject().put("fieldName", "terms_conditions"),
                new DeliveryOptions()
                        .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());

        Future<Message<JsonObject>> f2 = Future.future();
        vertx.eventBus().send(EmployeeDBV.class.getSimpleName(),
                new JsonObject().put("user_id", userId),
                new DeliveryOptions()
                        .addHeader(ACTION, EmployeeDBV.ACTION_EMPLOYEE_BY_USERE_ID), f2.completer());

        Future<Message<JsonObject>> f3 = Future.future();
        vertx.eventBus().send(getDBAddress(), body,
                new DeliveryOptions()
                        .addHeader(ACTION, PRINT_TERMS_CONDITIONS), f3.completer());

        CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
            try {
                if (detailReply.failed()) {
                    responseWarning(context, detailReply.cause().getMessage());
                } else {
                    Message<JsonObject> terms = detailReply.result().resultAt(0);
                    Message<JsonObject> employee = detailReply.result().resultAt(1);
                    Message<JsonObject> detail = detailReply.result().resultAt(2);

                    JsonObject reservation = detail.body();
                    reservation.put("terms", terms.body().getString("value"));
                    reservation.put("employee", employee.body());

                    responseOk(context, reservation);
                }
            } catch (Exception ex) {
                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
            }
        });


    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDate(body, "travel_date");
            isContained(body, "ticket_type", "1", "2", "0");
            isGrater(body, "seatings", 0);
            isBoolean(body, "has_invoice");
            isContained(body, "purchase_origin", "plataforma", "web", "kiosko", "app cliente", "app chofer");
            isName(body, "first_name");
            isName(body, "last_name");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateAndNotNull(body, "travel_date");
            isContained(body, "ticket_type", "1", "2", "0");
            isGrater(body, "seatings", 0);
            isBoolean(body, "has_invoice");
            isGraterAndNotNull(body, "terminal_origin_id", 0); //is going to be needed later in create method
            isContainedAndNotNull(body, "purchase_origin", "plataforma", "web", "kiosko", "app cliente", "app chofer");
            isName(body, "first_name");
            isName(body, "last_name");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            return super.isValidCreateData(context);
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    @Override
    protected void create(RoutingContext context) {
        this.addReservationCode(context);
        super.create(context);

    }

    private void addReservationCode(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put("reservation_code", UtilsID.generateID("B"));
        context.setBody(body.toBuffer());
    }

    private void checkinTravelDetail(RoutingContext context) {
        this.validateToken(context, userId -> {
            String reservationCode = context.request().getParam("reservation_code");
            if (reservationCode == null || reservationCode.isEmpty()) {
                UtilsResponse.responsePropertyValue(
                        context, new PropertyValueException(
                                "reservation_code", MISSING_REQUIRED_VALUE)
                );
                return;
            }

            // Get user employee
            JsonObject params = new JsonObject().put("user_id", userId);
            this.vertx.eventBus().send(EmployeeDBV.class.getSimpleName(), params, options(EmployeeDBV.ACTION_EMPLOYEE_BY_USERE_ID), replyEmployee -> {
                try{
                    if(replyEmployee.failed()) {
                        throw new Exception(replyEmployee.cause());
                    }
                    JsonObject employee = (JsonObject) replyEmployee.result().body();
                    JsonObject body = new JsonObject().put("reservation_code", reservationCode)
                            .put("user_branchoffice", employee.getInteger("branchoffice_id"));
                    this.vertx.eventBus().send(this.getDBAddress(), body, options(CHECKIN_TRAVEL_DETAIL),
                            reply -> {
                                this.genericResponse(context, reply);
                            });
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, replyEmployee.cause().getMessage());
                }
            });
        });
    }

    private void publicReservationDetail(RoutingContext context) {
        JsonObject body = new JsonObject()
                .put("reservation_code", context.request().getParam("reservation_code"));

        vertx.eventBus().send(getDBAddress(), body,
                options(PUBLIC_RESERVATION_DETAIL),
                reply -> genericResponse(context, reply)
        );
    }

    private void reservationDetail(RoutingContext context) {
        JsonObject body = new JsonObject()
                .put("reservation_code", context.request().getParam("reservation_code"));

        vertx.eventBus().send(getDBAddress(), body,
                options(RESERVATION_DETAIL),
                reply -> {
                    try{
                        if(reply.failed()) {
                            throw reply.cause();
                        }
                        genericResponse(context, reply);
                    }catch (Throwable t) {
                        t.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });
    }

    private void runStatus(RoutingContext context) {
        try {
            JsonObject body = new JsonObject()
                    .put("reservation_code", context.request().getParam("reservation_code"));

            vertx.eventBus().send(getDBAddress(), body,
                    options(RUN_STATUS),
                    reply -> genericResponse(context, reply)
            );
        } catch (Exception e) {
            responseError(context, e);
        }
    }

    private void travelsDetail(RoutingContext context) {
        this.validateToken(context, userID -> {
            Integer userId = userID;
            String boardingPassStatus = context.request().getParam("boardingPassStatus");
            JsonObject body = new JsonObject()
                    .put("userId", userId)
                    .put("boardingPassStatus", boardingPassStatus);
            vertx.eventBus().send(getDBAddress(), body,
                    options(TRAVELS_DETAIL),
                    reply -> genericResponse(context, reply)
            );
        });
    }
    
    private void initRegisterWithReservationCode(RoutingContext context) {
        this.addReservationCode(context);
        this.initRegister(context);
    }

    private void initRegister(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            String jwt = request.getHeader("Authorization");
            EventBus eventBus = vertx.eventBus();
            if (UtilsJWT.isTokenValid(jwt)) {
                JsonObject body = context.getBodyAsJson();
                body.put("created_by", UtilsJWT.getUserIdFrom(jwt));

                isGraterAndNotNull(body, "seatings", 0);
                isBetweenRangeAndNotNull(body, "ticket_type", 1, 3);
                isBetweenRangeAndNotNull(body, "purchase_origin", 1, 5);
                isMail(body, "email");
                isPhoneNumber(body, "phone");
                JsonArray routes = body.getJsonArray("routes");
                JsonArray passengers = body.getJsonArray("passengers");

                int len = routes.size();
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    JsonObject route = routes.getJsonObject(i);
                        if(route.containsKey(SCHEDULE_ROUTE_DESTINATION_ID) && route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID) != null){
                            tasks.add(countAvailableSeats(route, passengers));
                        }
                }
                Future<Message<JsonObject>> f1 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), 
                        new JsonObject().put("fieldName", "under_age"),new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
                Future<Message<JsonObject>> f2 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "time_before_checkin"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
                Future<Message<JsonObject>> f3 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "minimum_hour_reservation"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
                Future<Message<JsonObject>> f4 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "cancel_phone_reservation_time"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
                Future<Message<JsonObject>> f5 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "reservation_time"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());
                Future<Message<JsonObject>> f6 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "boarding_cancelation_time"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f6.completer());

                CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                all.whenComplete((r, t) -> {
                    try {
                        if (t != null) {
                            responseWarning(context, t.getCause().getMessage());
                        } else {
                            CompositeFuture.all(f1, f2, f3, f4, f5, f6).setHandler(detailReply -> {
                                try {
                                    if (detailReply.failed()){
                                        throw new Exception(detailReply.cause());
                                    }
                                    Message<JsonObject> underAgeMsg = detailReply.result().resultAt(0);
                                    Message<JsonObject> timeBeforeCheckinMsg = detailReply.result().resultAt(1);
                                    Message<JsonObject> minimumHourReservationMsg = detailReply.result().resultAt(2);
                                    Message<JsonObject> cancelPhoneReservationTimeMsg = detailReply.result().resultAt(3);
                                    Message<JsonObject> reservationTimeMsg = detailReply.result().resultAt(4);
                                    Message<JsonObject> reservationTimeExpires = detailReply.result().resultAt(5);

                                    JsonObject underAge = underAgeMsg.body();
                                    JsonObject timeBeforeCheckin = timeBeforeCheckinMsg.body();
                                    JsonObject minimumHourReservation = minimumHourReservationMsg.body();
                                    JsonObject cancelPhoneReservationTime = cancelPhoneReservationTimeMsg.body();
                                    JsonObject reservationTime = reservationTimeMsg.body();
                                    JsonObject reservationExpiresTime = reservationTimeExpires.body();
                                    body.put("under_age", Integer.valueOf(underAge.getString("value")))
                                        .put("time_before_checkin", Integer.valueOf(timeBeforeCheckin.getString("value")))
                                        .put("minimum_hour_reservation", Integer.valueOf(minimumHourReservation.getString("value")))
                                        .put("cancel_phone_reservation_time", Integer.valueOf(cancelPhoneReservationTime.getString("value")))
                                        .put("reservation_time", Integer.valueOf(reservationTime.getString("value")))
                                        .put("boarding_cancelation_time", Integer.valueOf(reservationExpiresTime.getString("value")));
                                    DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.INIT_REGISTER);
                                    eventBus.send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, replyInsert -> {
                                        try {
                                            if (replyInsert.failed()){
                                                throw new Exception(replyInsert.cause());
                                            }
                                            Message<Object> resultInsert = replyInsert.result();
                                            MultiMap headers = resultInsert.headers();
                                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                            } else {
                                                responseOk(context, resultInsert.body(), "Created");
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                                        }
                                    });
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                                }
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                    }
                });

            } else {
                responseWarning(context, "Out of session", "Session json web token is invalid");
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void calculateEnd(RoutingContext context){
        try{
            JsonObject body = context.getBodyAsJson();

            UtilsValidation.isGraterAndNotNull(body,"id",0);
            if(body.getJsonArray("passengers")==null){
                throw new PropertyValueException("passengers", MISSING_REQUIRED_VALUE);
            }

            body.put(CREATED_BY,context.<Integer>get(USER_ID));

            vertx.eventBus().send(this.getDBAddress(), body, options(BoardingPassDBV.DRIVER_END_CALCULATE), (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Caculated");
                    }
                }catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        }catch(UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void driverEndRegister(RoutingContext context){
        try{
            JsonObject body = context.getBodyAsJson();

            UtilsValidation.isGraterAndNotNull(body, ID,0);
            if(!body.containsKey("payments")){
                throw new PropertyValueException("payments", MISSING_REQUIRED_VALUE);
            }
            if (body.getJsonObject("cash_change")==null){
                throw new PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
            }
            if(body.getJsonArray("passengers")==null){
                throw new PropertyValueException("passengers", MISSING_REQUIRED_VALUE);
            }

            Future<Message<JsonObject>> f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            Future<Message<JsonObject>> f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }

                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    body
                        .put("currency_id", Integer.valueOf(currencyId.getString("value")))
                        .put("iva_percent", Double.valueOf(ivaPercent.getString("value")));

                    body.put(CREATED_BY,context.<Integer>get(USER_ID))
                            .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID));

                    vertx.eventBus().send(this.getDBAddress(), body, options(BoardingPassDBV.DRIVER_END_MAKE), (AsyncResult<Message<JsonObject>> reply) -> {
                        try{
                            if(reply.failed()) {
                                throw reply.cause();
                            }
                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Registered");
                            }
                        }catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });

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
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void endRegister(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID))
                .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                .put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
        try {
            isGraterAndNotNull(body, ID, 0);
            DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.END_REGISTER);

            Boolean isCredit = body.containsKey("is_credit") ? body.getBoolean("is_credit") : Boolean.FALSE;
            Integer customerId = body.containsKey(BoardingPassDBV.CUSTOMER_ID) ? body.getInteger(BoardingPassDBV.CUSTOMER_ID) : 3;
            JsonObject customer = new JsonObject().put("customer_id", customerId);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "iva"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), customer,
                    new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f3.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "expire_open_tickets_after"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(2);
                    Message<JsonObject> expireOpenTicketsAfterMsg = detailReply.result().resultAt(3);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();
                    JsonObject expireOpenTicketsAfter = expireOpenTicketsAfterMsg.body();

                    if (isCredit) {
                        Double availableCredit = customerCreditData.getDouble("available_credit");
                        Boolean hasCredit = customerCreditData.getBoolean("has_credit");
                        Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                        JsonArray payments = body.getJsonArray("payments");
                        for(int i = 0; i < payments.size();i++){
                            JsonObject pay = payments.getJsonObject(i);
                            if(!pay.isEmpty() && pay.getInteger("payment_method_id") != -1){
                                throw new Exception("Customer: partial credit payment method not available");
                            }
                        }
                        body.put("credit_amount", parcelPaymentsAmount);
                        if (!hasCredit)
                            throw new Exception("Customer: no credit available");
                        if (availableCredit < parcelPaymentsAmount)
                            throw new Exception("Customer: Insufficient funds to apply credit");

                        if(!customerCreditData.getString("services_apply_credit").contains("boarding_pass"))
                            throw new Exception("Customer: service not applicable");
                    }

                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString("value")))
                            .put("customer_credit_data", customerCreditData)
                            .put("payment_condition", isCredit ? "credit" : "cash")
                            .put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString("value")));
                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, replyInsert -> {
                        try {
                            if (replyInsert.failed()){
                                throw replyInsert.cause();
                            }
                            Message<Object> resultInsert = replyInsert.result();
                            MultiMap headers = resultInsert.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, resultInsert.body(), "Updated");
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });


        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void endRegisterPartner(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
        try {
            isGraterAndNotNull(body, ID, 0);
            isGraterAndNotNull(body, TOKEN_ID, 0);
            DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, PARTNER_END_REGISTER);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "iva"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "expire_open_tickets_after"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            CompositeFuture.all(f1, f2, f4).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> expireOpenTicketsAfterMsg = detailReply.result().resultAt(2);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject expireOpenTicketsAfter = expireOpenTicketsAfterMsg.body();

                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString("value")))
                            .put("payment_condition", "cash")
                            .put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString("value")));
                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, replyInsert -> {
                        try {
                            if (replyInsert.failed()){
                                throw replyInsert.cause();
                            }
                            Message<Object> resultInsert = replyInsert.result();
                            MultiMap headers = resultInsert.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, resultInsert.body(), "Updated");
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });


        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void makeCheckIn(RoutingContext context) {
        try {

            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID))
                    .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                    .put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
            isGraterAndNotNull(body, ID, 0);
            DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.CHECK_IN);

            Boolean isCredit = body.containsKey("is_credit") ? body.getBoolean("is_credit") : Boolean.FALSE;
            Integer customerId = body.containsKey(BoardingPassDBV.CUSTOMER_ID) ? body.getInteger(BoardingPassDBV.CUSTOMER_ID) : 3;
            JsonObject customer = new JsonObject().put("customer_id", customerId);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "expire_open_tickets_after"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), customer, new DeliveryOptions()
                            .addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> expireOpenTicketsAfterMsg = detailReply.result().resultAt(2);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(3);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject expireOpenTicketsAfter = expireOpenTicketsAfterMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();

                    if (isCredit) {
                        Double availableCredit = customerCreditData.getDouble("available_credit");
                        Boolean hasCredit = customerCreditData.getBoolean("has_credit");
                        Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");

                        if (!hasCredit)
                            throw new Exception("Customer: no credit available");

                        if (availableCredit < parcelPaymentsAmount)
                            throw new Exception("Customer: Insufficient funds to apply credit");

                        if(!customerCreditData.getString("services_apply_credit").contains("boarding_pass"))
                            throw new Exception("Customer: service not applicable");
                    }

                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString("value")))
                            .put("customer_credit_data", customerCreditData)
                            .put("payment_condition", isCredit ? "credit" : "cash")
                            .put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString("value")));
                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, reply -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            Message<Object> resultInsert = reply.result();
                            MultiMap headers = resultInsert.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, resultInsert.body(), "Updated");
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
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void calculateCheckIn(RoutingContext context) {
        this.validateToken(context, (Integer userId) -> {
            JsonObject body = context.getBodyAsJson();
            body.put("updated_by", userId);
            try {
                isGraterAndNotNull(body, "id", 0);


                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "expire_open_tickets_after"),
                        new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyEOTA -> {
                    try {
                        if (replyEOTA.failed()){
                            throw replyEOTA.cause();
                        }

                        JsonObject expireOpenTicketsAfter = (JsonObject) replyEOTA.result().body();
                        body.put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString(VALUE)));

                        DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.CALCULATE_CHECK_IN);
                        vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert,
                                reply -> {
                                    try{
                                        if(reply.failed()) {
                                            throw new Exception(reply.cause());
                                        }
                                        Message<Object> resultInsert = reply.result();
                                        MultiMap headers = resultInsert.headers();
                                        if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                            responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                        } else {
                                            responseOk(context, resultInsert.body(), "Calculated");
                                        }

                                    }catch (Exception ex) {
                                        ex.printStackTrace();
                                        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                                    }
                                }
                        );

                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });

            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            }
        });
    }

    private void changePassengers(RoutingContext context) {
        this.validateToken(context, (Integer userId) -> {
            JsonObject body = context.getBodyAsJson();
            body.put("updated_by", userId);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, BoardingPassDBV.CHANGE_PASSENGERS);

            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options, (AsyncResult<Message<Object>> reply) -> {
                    try{
                        if(reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        Message<Object> result = reply.result();
                        MultiMap headers = result.headers();

                        if (reply.succeeded()) {
                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Updated");
                            }
                        } else {
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }

                    }catch (Exception ex) {
                        ex.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                }
            );

        });
    }


    private void cancelRegister(RoutingContext context) {
        HttpServerRequest request = context.request();
        String jwt = request.getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject body = new JsonObject().put("id", Integer.valueOf(request.getParam("id")))
                .put("updated_by", UtilsJWT.getUserIdFrom(jwt));
            try {
                isGraterAndNotNull(body, "id", 0);
                DeliveryOptions optionsDelete = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.CANCEL_REGISTER);
                vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsDelete,
                        reply -> {
                            try{
                                if(reply.failed()) {
                                    throw new Exception(reply.cause());
                                }
                                Message<Object> result = reply.result();
                                MultiMap headers = result.headers();
                                if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                } else {
                                    responseOk(context, result.body(), "Canceled");
                                }
                            }catch (Exception ex) {
                                ex.printStackTrace();
                                responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                            }
                        }
                );

            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            }

        } else {
            responseWarning(context, "Out of session", "Session json web token is invalid");
        }
    }

    private void cancelRegisterByReservationCode(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject().put("reservation_code", request.getParam("reservation_code"))
                    .put("updated_by", context.<Integer>get(USER_ID));
            isEmptyAndNotNull(body, "reservation_code");
            DeliveryOptions optionsDelete = new DeliveryOptions().addHeader(ACTION, CANCEL_REGISTER_BY_RESERVATION_CODE);
            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsDelete,
                    reply -> {
                        try{
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            Message<Object> result = reply.result();
                            MultiMap headers = result.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, result.body(), "Canceled");
                            }

                        }catch (Exception ex) {
                            ex.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }
                    }
            );

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void cancelReservation(RoutingContext context) {
        EventBus eventBus = vertx.eventBus();
        try {
            int userId = context.get(USER_ID);
            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            String reservationCode = context.request().getParam(RESERVATION_CODE);

            eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", CURRENCY_ID), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            eventBus.send(CashOutDBV.class.getSimpleName(), new JsonObject().put(RESERVATION_CODE, reservationCode), new DeliveryOptions().addHeader(ACTION, CashOutDBV.ACTION_GET_CASH_OUT_EMPLOYEE), f2.completer());
            eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", IVA), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
            CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }
                    Message<JsonObject> debtsMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> cashOutMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> ivaMsg = detailReply.result().resultAt(2);
                    JsonObject currency = debtsMsg.body();
                    JsonObject cashOut = cashOutMsg.body();
                    JsonObject iva = ivaMsg.body();

                    JsonObject body = new JsonObject()
                            .put(RESERVATION_CODE, reservationCode)
                            .put("customer_id", context.getBodyAsJson().getInteger("customer_id"))
                            .put(UPDATED_BY, userId)
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                            .put(CURRENCY_ID, currency.getInteger(CURRENCY_ID))
                            .put(IVA, Double.parseDouble(iva.getString("value")))
                            .put(CASHOUT_ID, cashOut.getInteger(ID))
                            .put(NOTES, context.getBodyAsJson().getString(NOTES));

                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body,
                            options(BoardingPassDBV.CANCEL_RESERVATION), (AsyncResult<Message<Object>> reply) -> {
                                try {
                                    if (reply.failed()){
                                        throw reply.cause();
                                    }
                                    Message<Object> result = reply.result();
                                    MultiMap headers = result.headers();
                                    if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                        responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                    } else {
                                        responseOk(context, result.body(), "Canceled");
                                    }
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
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, t);
        }
    }

    private void partialCancelReservation(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, RESERVATION_CODE);
            isGraterAndNotNull(body, "boarding_pass_ticket_id", 0);

            body.put(BoardingPassDBV.CUSTOMER_ID, context.getBodyAsJson().getInteger(BoardingPassDBV.CUSTOMER_ID))
                    .put(UPDATED_BY, context.<Integer>get(USER_ID))
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body,
                    options(PARTIAL_CANCEL_RESERVATION), (AsyncResult<Message<Object>> reply) -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            Message<Object> result = reply.result();
                            MultiMap headers = result.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, result.body(), "Canceled");
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t);
                        }
                    });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, t);
        }
    }
 
    private CompletableFuture<JsonObject> countAvailableSeats(JsonObject route, JsonArray passengers) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer scheduleRouteDestinationId = route.getInteger("schedule_route_destination_id");

        int accSeating = 0;
        for (int i = 0, iLen = passengers.size(); i < iLen; i++) {
            JsonObject passenger = passengers.getJsonObject(i);
            JsonArray tickets = passenger.getJsonArray("tickets");
            for (int j = 0, jLen = tickets.size(); j < jLen; j++) {
                JsonObject ticket = tickets.getJsonObject(j);
                Integer srdId = ticket.getInteger("schedule_route_destination_id");
                    if (scheduleRouteDestinationId.equals(srdId)) {
                        accSeating++;
                    }
            }
        }

        final Integer seating = accSeating;
        JsonObject message = new JsonObject().put("schedule_route_destination_id", scheduleRouteDestinationId);
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, ScheduleRouteDBV.ACTION_AVAILABLE_SEATS_BY_DESTINATION);
        vertx.eventBus().send(ScheduleRouteDBV.class.getSimpleName(), message, options, (AsyncResult<Message<JsonObject>> reply) -> {
                    try{
                        if(reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        JsonObject result = reply.result().body();
                        Integer availableSeating = result.getInteger("available_seatings");
                        if (seating > availableSeating) {
                            future.completeExceptionally(new Throwable("Only " + availableSeating.toString() + " available seating for schedule route destination " + scheduleRouteDestinationId.toString()));
                        } else {
                            future.complete(result);
                        }
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
        });
        return future;
    }

    private void advancedSearch(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isBooleanAndNotNull(body, "include_finished");
            isEmptyAndNotNull(body, "first_name");
            isEmptyAndNotNull(body, "last_name");
            isGraterAndNotNull(body, "terminal_origin_id", 0);
            isGraterAndNotNull(body, "terminal_destiny_id", 0);
            if(body.getBoolean("is_date_created_at") != null){
                isEmptyAndNotNull(body, "init_date");
                isEmptyAndNotNull(body, "end_date");
            }
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.BOARDINGPASS_ADVANCED_SEARCH);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                            if (reply.succeeded()) {
                                responseOk(context, reply.result().body());
                            } else {
                                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                            }
                        });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }
    private void salesCancelReport(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.CANCEL_SALES_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void report(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");

            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportAccountant(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            body.put("special_tickets_ignore", 29);
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportTotals(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT_TOTALS);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportTotalsAccountant(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            body.put("special_tickets_ignore", 29);
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT_TOTALS);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void occupationReport(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");

            if(body.getInteger(_TERMINAL_ORIGIN_ID) != null || body.getInteger(_TERMINAL_DESTINY_ID) != null){
                isBooleanAndNotNull(body, "is_passenger_trip");
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BOARDINGPASS_OCCUPATION_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void priceListApply(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, "price_list_id", 0);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions optionsSearch = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_GET_PRICES_LIST_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, optionsSearch , repplySearch ->{
                 try{
                     if(repplySearch.failed()){
                         throw new Exception(repplySearch.cause());
                     }
                     String hash = UtilsJWT.generateHash().substring(0,100);
                     responseOk(context, new JsonObject().put("message", "The request is being processed").put("hash", hash));
                     DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_APPLY_PRICE_LIST);
                     body.put("hash", hash);
                     vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                         try {
                             if (reply.failed()) {
                                 throw new Exception(reply.cause());
                             }
                             //responseOk(context, reply.result().body());
                         } catch (Exception ex) {
                             ex.printStackTrace();
                             // responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                         }
                     });
                 }catch (Exception ex){
                     ex.printStackTrace();
                     responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                 }
             });
        }catch (PropertyValueException e){
            responsePropertyValue(context, e);
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    private void registerPriceList(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_REGISTER_PRICE_LIST);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply ->{
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
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    private void priceListStatus(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_GET_PRICE_LIST_STATUS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply ->{
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
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    private void pricesListDetail(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_GET_PRICES_LIST_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply ->{
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
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    private void pricesListRegistered(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_GET_PRICES_LIST_REGISTERED);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply ->{
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
        }catch (Exception ex){
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    private void pricesList(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_GET_PRICES_LIST);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply ->{
               try{
                   if(reply.failed()){
                       throw new Exception(reply.cause());
                   }
                   responseOk(context, reply.result().body());
               } catch (Exception ex){
                   ex.printStackTrace();
                   responseError(context, UNEXPECTED_ERROR, ex.getCause());
               }
            });
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getCause());
        }
    }
    private void configPricesList(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        // rendondo allende
        if( body.getInteger("LMM01")!=null)
            if( body.getInteger("LMM01")==1) {
                body.remove("LMM01");
                body.remove("terminal_destiny");
                body.put("terminal_destiny", 1);
            }

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.SPECIAL_TICKET_LIST);
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
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void cancelReservationWithoutCashOut(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, "reservation_code");

            DeliveryOptions optionsIva = new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD);
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", IVA), optionsIva, replyIva -> {
                try {
                    if (replyIva.failed()) {
                        throw replyIva.cause();
                    }

                    JsonObject resultCurrency = (JsonObject) replyIva.result().body();
                    Double ivaPercent = Double.valueOf(resultCurrency.getString("value"));

                    body.put("customer_id", context.getBodyAsJson().getInteger("customer_id"))
                            .put("iva_percent", ivaPercent)
                            .put(UPDATED_BY, context.<Integer>get(USER_ID))
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body,
                            options(BoardingPassDBV.CANCEL_RESERVATION_WITH_CASHOUT_CLOSED), (AsyncResult<Message<Object>> reply) -> {
                                try {
                                    if (reply.failed()) {
                                        throw reply.cause();
                                    }
                                    Message<Object> result = reply.result();
                                    MultiMap headers = result.headers();
                                    if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                        responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                    } else {
                                        responseOk(context, result.body(), "Canceled");
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, t);
                                }
                            });


                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, t);
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, t);
        }
    }

    private void getPhoneReservations(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.GET_PHONE_RESERVATION);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

     private void reportPassengerType(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT_PASSENGER);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportTravelFrequency(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "boarding_pass");
            isDateTimeAndNotNull(body, "end_date", "boarding_pass");
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.REPORT_TRAVEL_FREQUENCY);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    if (reply.succeeded()) {
                        responseOk(context, reply.result().body());
                    } else {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void makeCheckInAgain(RoutingContext context) {
        try {

            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID))
                    .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                    .put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
            isGraterAndNotNull(body, ID, 0);
            DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.CHECK_IN_AGAIN);

            Boolean isCredit = body.containsKey("is_credit") ? body.getBoolean("is_credit") : Boolean.FALSE;
            Integer customerId = body.containsKey(BoardingPassDBV.CUSTOMER_ID) ? body.getInteger(BoardingPassDBV.CUSTOMER_ID) : 3;
            JsonObject customer = new JsonObject().put("customer_id", customerId);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), customer, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f3.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "expire_open_tickets_after"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(2);
                    Message<JsonObject> expireOpenTicketsAfterMsg = detailReply.result().resultAt(3);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject expireOpenTicketsAfter = expireOpenTicketsAfterMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();

                    if (isCredit) {
                        Double availableCredit = customerCreditData.getDouble("available_credit");
                        Boolean hasCredit = customerCreditData.getBoolean("has_credit");
                        Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");

                        if (!hasCredit)
                            throw new Exception("Customer: no credit available");

                        if (availableCredit < parcelPaymentsAmount)
                            throw new Exception("Customer: Insufficient funds to apply credit");

                        if(!customerCreditData.getString("services_apply_credit").contains("boarding_pass"))
                            throw new Exception("Customer: service not applicable");
                    }

                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString("value")))
                            .put("customer_credit_data", customerCreditData)
                            .put("payment_condition", isCredit ? "credit" : "cash")
                            .put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString("value")));
                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, reply -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            Message<Object> resultInsert = reply.result();
                            MultiMap headers = resultInsert.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, resultInsert.body(), "Updated");
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
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void calculateCheckInAgain(RoutingContext context) {
        this.validateToken(context, (Integer userId) -> {
            JsonObject body = context.getBodyAsJson();
            body.put("updated_by", userId);
            try {
                isGraterAndNotNull(body, "id", 0);

                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "expire_open_tickets_after"),
                        new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyEOTA -> {
                            try {
                                if (replyEOTA.failed()){
                                    throw replyEOTA.cause();
                                }

                                JsonObject expireOpenTicketsAfter = (JsonObject) replyEOTA.result().body();
                                body.put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString(VALUE)));

                                DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.CALCULATE_CHECK_IN_AGAIN);
                                vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert,
                                        reply -> {
                                            try{
                                                if(reply.failed()) {
                                                    throw new Exception(reply.cause());
                                                }
                                                Message<Object> resultInsert = reply.result();
                                                MultiMap headers = resultInsert.headers();
                                                if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                                    responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                                } else {
                                                    responseOk(context, resultInsert.body(), "Calculated");
                                                }

                                            }catch (Exception ex) {
                                                ex.printStackTrace();
                                                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                                            }
                                        }
                                );

                            } catch (Throwable t){
                                t.printStackTrace();
                                responseError(context, UNEXPECTED_ERROR, t.getMessage());
                            }
                        });

            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            }
        });
    }

    private void initRegisterPrepaid(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            String jwt = request.getHeader("Authorization");
            EventBus eventBus = vertx.eventBus();
            if (UtilsJWT.isTokenValid(jwt)) {
                JsonObject body = context.getBodyAsJson();
                body.put("created_by", UtilsJWT.getUserIdFrom(jwt));

                isGraterAndNotNull(body, "seatings", 0);
                isBetweenRangeAndNotNull(body, "ticket_type", 1, 3);
                isBetweenRangeAndNotNull(body, "purchase_origin", 1, 5);
                isMail(body, "email");
                isPhoneNumber(body, "phone");
                JsonArray routes = body.getJsonArray("routes");
                JsonArray passengers = body.getJsonArray("passengers");

                int len = routes.size();
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    JsonObject route = routes.getJsonObject(i);
                    if(route.containsKey(SCHEDULE_ROUTE_DESTINATION_ID) && route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID) != null){
                        tasks.add(countAvailableSeats(route, passengers));
                    }
                }
                Future<Message<JsonObject>> f1 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "under_age"),new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
                Future<Message<JsonObject>> f2 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "time_before_checkin"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
                Future<Message<JsonObject>> f3 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "minimum_hour_reservation"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
                Future<Message<JsonObject>> f4 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "cancel_phone_reservation_time"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
                Future<Message<JsonObject>> f5 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "reservation_time"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());
                Future<Message<JsonObject>> f6 = Future.future();
                eventBus.send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "boarding_cancelation_time"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f6.completer());

                CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                all.whenComplete((r, t) -> {
                    try {
                        if (t != null) {
                            responseWarning(context, t.getCause().getMessage());
                        } else {
                            CompositeFuture.all(f1, f2, f3, f4, f5, f6).setHandler(detailReply -> {
                                try {
                                    if (detailReply.failed()){
                                        throw new Exception(detailReply.cause());
                                    }
                                    Message<JsonObject> underAgeMsg = detailReply.result().resultAt(0);
                                    Message<JsonObject> timeBeforeCheckinMsg = detailReply.result().resultAt(1);
                                    Message<JsonObject> minimumHourReservationMsg = detailReply.result().resultAt(2);
                                    Message<JsonObject> cancelPhoneReservationTimeMsg = detailReply.result().resultAt(3);
                                    Message<JsonObject> reservationTimeMsg = detailReply.result().resultAt(4);
                                    Message<JsonObject> reservationTimeExpires = detailReply.result().resultAt(5);

                                    JsonObject underAge = underAgeMsg.body();
                                    JsonObject timeBeforeCheckin = timeBeforeCheckinMsg.body();
                                    JsonObject minimumHourReservation = minimumHourReservationMsg.body();
                                    JsonObject cancelPhoneReservationTime = cancelPhoneReservationTimeMsg.body();
                                    JsonObject reservationTime = reservationTimeMsg.body();
                                    JsonObject reservationExpiresTime = reservationTimeExpires.body();
                                    body.put("under_age", Integer.valueOf(underAge.getString("value")))
                                            .put("time_before_checkin", Integer.valueOf(timeBeforeCheckin.getString("value")))
                                            .put("minimum_hour_reservation", Integer.valueOf(minimumHourReservation.getString("value")))
                                            .put("cancel_phone_reservation_time", Integer.valueOf(cancelPhoneReservationTime.getString("value")))
                                            .put("reservation_time", Integer.valueOf(reservationTime.getString("value")))
                                            .put("boarding_cancelation_time", Integer.valueOf(reservationExpiresTime.getString("value")));
                                    DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.INIT_PREPAID);
                                    eventBus.send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, replyInsert -> {
                                        try {
                                            if (replyInsert.failed()){
                                                throw new Exception(replyInsert.cause());
                                            }
                                            Message<Object> resultInsert = replyInsert.result();
                                            MultiMap headers = resultInsert.headers();
                                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                            } else {
                                                responseOk(context, resultInsert.body(), "Created");
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                                        }
                                    });
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                                }
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                    }
                });

            } else {
                responseWarning(context, "Out of session", "Session json web token is invalid");
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void endRegisterPrepaid(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID))
                .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                .put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
        try {
            isGraterAndNotNull(body, ID, 0);
            DeliveryOptions optionsInsert = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.END_PREPAID);

            Boolean isCredit = body.containsKey("is_credit") ? body.getBoolean("is_credit") : Boolean.FALSE;
            Integer customerId = body.containsKey(BoardingPassDBV.CUSTOMER_ID) ? body.getInteger(BoardingPassDBV.CUSTOMER_ID) : 3;
            JsonObject customer = new JsonObject().put("customer_id", customerId);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "iva"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), customer,
                    new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f3.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),new JsonObject().put("fieldName", "expire_open_tickets_after"),
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()){
                        throw detailReply.cause();
                    }
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(2);
                    Message<JsonObject> expireOpenTicketsAfterMsg = detailReply.result().resultAt(3);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();
                    JsonObject expireOpenTicketsAfter = expireOpenTicketsAfterMsg.body();

                    if (isCredit) {
                        Double availableCredit = customerCreditData.getDouble("available_credit");
                        Boolean hasCredit = customerCreditData.getBoolean("has_credit");
                        Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                        JsonArray payments = body.getJsonArray("payments");
                        for(int i = 0; i < payments.size();i++){
                            JsonObject pay = payments.getJsonObject(i);
                            if(!pay.isEmpty() && pay.getInteger("payment_method_id") != -1){
                                throw new Exception("Customer: partial credit payment method not available");
                            }
                        }
                        body.put("credit_amount", parcelPaymentsAmount);
                        if (!hasCredit)
                            throw new Exception("Customer: no credit available");
                        if (availableCredit < parcelPaymentsAmount)
                            throw new Exception("Customer: Insufficient funds to apply credit");

                        if(!customerCreditData.getString("services_apply_credit").contains("boarding_pass"))
                            throw new Exception("Customer: service not applicable");
                    }

                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString("value")))
                            .put("customer_credit_data", customerCreditData)
                            .put("payment_condition", isCredit ? "credit" : "cash")
                            .put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString("value")));
                    vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, optionsInsert, replyInsert -> {
                        try {
                            if (replyInsert.failed()){
                                throw replyInsert.cause();
                            }
                            Message<Object> resultInsert = replyInsert.result();
                            MultiMap headers = resultInsert.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, resultInsert.body(), "Updated");
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void cancelPrepaidTravelInit(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CANCEL_PREPAID_TRAVEL_INIT);
            vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), body, options,
                    reply -> {
                        try{
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            Message<Object> result = reply.result();
                            MultiMap headers = result.headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, result.body(), "Canceled");
                            }

                        }catch (Exception ex) {
                            ex.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }
                    }
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
}
