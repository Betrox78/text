/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.rental;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import database.employees.EmployeeDBV;
import database.rental.RentalDBV;
import static database.rental.RentalDBV.*;
import database.vechicle.VehicleDBV;

import static database.routes.ScheduleRouteDBV.GET_ROUTE_STATUS_REPORT;
import static database.vechicle.VehicleDBV.ACTION_IS_AVAILABLE_RENTAL_VEHICLE;

import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.PropertyError;
import static service.commons.Constants.*;

import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.CreditMiddleware;
import utils.*;

import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responsePropertyValue;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author ulises
 */
public class RentalSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return RentalDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/rentals";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/report/rangeDates/:travelDate/:arrivalDate", AuthMiddleware.getInstance(), this::getByRangeDates);
        this.addHandler(HttpMethod.GET, "/report/rangeDates/:travelDate/:arrivalDate/:pickupBranchId", AuthMiddleware.getInstance(), this::getByRangeDates);
        this.addHandler(HttpMethod.GET, "/report/quotation/canRent/:reservationCode", AuthMiddleware.getInstance(), this::canRent);
        this.addHandler(HttpMethod.GET, "/sendQuotation/:reservationCode", AuthMiddleware.getInstance(), this::sendQuotation);
        this.addHandler(HttpMethod.POST, "/action/quotationToRent", AuthMiddleware.getInstance(), this::quotationToRent);
        this.addHandler(HttpMethod.POST, "/salesReport", AuthMiddleware.getInstance(), this::salesReport);
        this.addHandler(HttpMethod.POST, "/salesReport/totals", AuthMiddleware.getInstance(), this::salesReportTotals);

        this.addHandler(HttpMethod.GET, "/report/quotationDetail/:reservationCode", AuthMiddleware.getInstance(), this::quotationDetail);
        this.addHandler(HttpMethod.GET, "/report/rentalDetail/:reservationCode", AuthMiddleware.getInstance(), this::reservationDetail);
        this.addHandler(HttpMethod.GET, "/report/rentalCanDeliver/:reservationCode", AuthMiddleware.getInstance(), this::rentalCanDeliver);
        this.addHandler(HttpMethod.GET, "/report/rentalPaymentDetail/:reservationCode", AuthMiddleware.getInstance(), this::reservationPaymentDetail);
        this.addHandler(HttpMethod.GET, "/report/canCancel/:reservationCode", AuthMiddleware.getInstance(), this::canCancel);
        this.addHandler(HttpMethod.GET, "/report/rentalPrice/:vehicleId/:travelDateTime/:arrivalDateTime", AuthMiddleware.getInstance(), this::getRentalPrice);
        this.addHandler(HttpMethod.GET, "/report/driverCost/:travelDateTime/:arrivalDateTime", AuthMiddleware.getInstance(), this::getDriverCost);
        this.addHandler(HttpMethod.GET, "/action/list", AuthMiddleware.getInstance(), this::getList);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), CreditMiddleware.getInstance(vertx), this::register);
        this.addHandler(HttpMethod.POST, "/action/deliver", AuthMiddleware.getInstance(), this::deliver);
        this.addHandler(HttpMethod.POST, "/action/reception", AuthMiddleware.getInstance(), this::reception);
        this.addHandler(HttpMethod.POST, "/action/partialPayment", AuthMiddleware.getInstance(), this::partialPayment);
        this.addHandler(HttpMethod.POST, "/action/cancel", AuthMiddleware.getInstance(), this::cancel);
        this.addHandler(HttpMethod.POST, "/list", AuthMiddleware.getInstance(), this::vansList);
        this.addHandler(HttpMethod.POST,"/updateRent", AuthMiddleware.getInstance(), this::updateRent);

        super.start(startFuture);
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "first_name");
            isName(body, "last_name");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            isDate(body, "departure_date");
            isHour24(body, "departure_time");
            isDate(body, "return_date");
            isHour24(body, "return_time");
            isBoolean(body, "pickitup_at_office");
            isEmpty(body, "init_route");
            isEmpty(body, "init_full_address");
            isEmpty(body, "farthest_route");
            isEmpty(body, "farthest_full_address");
            isBoolean(body, "has_driver");
            isBoolean(body, "pickitup_at_office");
            isBoolean(body, "leave_at_office");
            isBoolean(body, "is_quotation");
            isContained(body, "status_on_reception", "0", "1", "2", "3");
            Integer passengers = body.getInteger("total_passengers");
            if (passengers != null) {
                if (passengers < 1) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError(
                            "total_passengers", "has to be almost 1"));
                    return false;
                }
            }

        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "first_name");
            isNameAndNotNull(body, "last_name");
            isPhoneNumberAndNotNull(body, "phone");
            isMail(body, "email");
            isDateAndNotNull(body, "departure_date");
            isHour24AndNotNull(body, "departure_time");
            isDateAndNotNull(body, "return_date");
            isHour24AndNotNull(body, "return_time");
            isBoolean(body, "pickitup_at_office");
            isEmpty(body, "init_route");
            isEmpty(body, "init_full_address");
            isEmpty(body, "farthest_route");
            isEmpty(body, "farthest_full_address");
            isBoolean(body, "has_driver");
            isBoolean(body, "pickitup_at_office");
            isBoolean(body, "leave_at_office");
            isBoolean(body, "is_quotation");
            isContained(body, "status_on_reception", "0", "1", "2", "3");
            isContained(body, "payment_status", "0", "1", "2");
            int passengers = body.getInteger("total_passengers", 0);
            if (passengers < 1) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("total_passengers", "has to be almost 1"));
                return false;
            }
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    protected boolean isValidCreateDataToRegisterVans(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "first_name");
            isNameAndNotNull(body, "last_name");
            isPhoneNumberAndNotNull(body, "phone");
            isBoolean(body, "is_quotation");
            if(body.getBoolean("is_quotation", false)) isMail(body, "email");
            isDateAndNotNull(body, "departure_date");
            isHour24AndNotNull(body, "departure_time");
            isDateAndNotNull(body, "return_date");
            isHour24AndNotNull(body, "return_time");
            isBoolean(body, "pickitup_at_office");
            isEmpty(body, "init_route");
            isEmpty(body, "init_full_address");
            isEmpty(body, "farthest_route");
            isEmpty(body, "farthest_full_address");
            isBoolean(body, "has_driver");
            isBoolean(body, "pickitup_at_office");
            isBoolean(body, "leave_at_office");
            isContained(body, "status_on_reception", "0", "1", "2", "3");
            isContained(body, "payment_status", "0", "1", "2");
            int passengers = body.getInteger("total_passengers", 0);
            if (passengers < 1) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("total_passengers", "has to be almost 1"));
                return false;
            }
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    private void getByRangeDates(RoutingContext context){
        JsonObject body = new JsonObject();
        HttpServerRequest request = context.request();
        if(request.getParam("pickupBranchId") != null){
            body.put("pickupBranchId", request.getParam("pickupBranchId"));
        }
        body.put("travelDate", request.getParam("travelDate"));
        body.put("arrivalDate", request.getParam("arrivalDate"));
        vertx.eventBus().send(RentalDBV.class.getSimpleName(), body, options(RentalDBV.ACTION_GET_BY_RANGE_DATES), (AsyncResult<Message<Object>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                this.genericResponse(context, reply, "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, t);
            }
        });
    }

    private void getEmployeeBranchoffice(RoutingContext context, Handler<JsonObject> handler) {
        // Get user employee
        vertx.eventBus().send(EmployeeDBV.class.getSimpleName(),
                new JsonObject().put("user_id", context.<Integer>get(USER_ID)),
                options(EmployeeDBV.ACTION_EMPLOYEE_BY_USERE_ID),(AsyncResult<Message<Object>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                JsonObject employee = (JsonObject) reply.result().body();
                JsonObject body = new JsonObject().put("user_employee", employee.getInteger("id"))
                        .put("user_branchoffice", employee.getInteger("branchoffice_id"));
                handler.handle(body);
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, t);
            }
        });
    }

    private void canRent(RoutingContext context){
        try {
            String reservationCode = context.request().getParam("reservationCode");
            if (this.isReservationCodeValid(reservationCode, context)) {
                vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("reservationCode", reservationCode), options(ACTION_CAN_RENT), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        if (reply.result().body() == null) {
                            UtilsResponse.responseWarning(context, "Reservation not found", "Element not found");
                        } else {
                            this.genericResponse(context, reply);
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        UtilsResponse.responseWarning(context, null, t);
                    }
                });
            }
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, t);
        }
    }

    private void quotationToRent(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        try{
            String reservationCode = body.getString("reservation_code");
            isEmptyAndNotNull(body, "reservation_code");
            if(!body.containsKey("cash_change")){
                throw new PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
            }
            if(!body.containsKey("payments")){
                throw new PropertyValueException("payments", MISSING_REQUIRED_VALUE);
            }
            if (this.isReservationCodeValid(reservationCode, context)) {

                vertx.eventBus().send(RentalDBV.class.getSimpleName(), body, options(ACTION_GET_QUOTATION_TO_RENT), reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        JsonObject newBody = (JsonObject) reply.result().body();
                        newBody.put("payments", body.getJsonArray("payments"));
                        newBody.put("cash_change", body.getJsonObject("cash_change"));
                        JsonArray referenceParams = null;
                        if (body.getJsonArray("rental_extra_charges") != null && !body.getJsonArray("rental_extra_charges").isEmpty()){
                            referenceParams = new JsonArray().add("extra_charges").add("amount").add("discount").add("total_amount");
                            this.validationQuotationFields(Double.class.getSimpleName(), context, body, newBody, referenceParams, true);
                            newBody.put("rental_extra_charges", body.getJsonArray("rental_extra_charges"));
                        }
                        if (body.getBoolean("has_driver") != null && body.getBoolean("has_driver")){
                            if(body.containsKey("driver")) {
                                referenceParams = new JsonArray().add("employee_id");
                                this.validationQuotationFields(Integer.class.getSimpleName(), context, body.getJsonObject("driver"), newBody, referenceParams, false);

                                referenceParams = new JsonArray().add("first_name").add("last_name").add("birthday").add("no_license").add("expired_at").add("file_license");
                                this.validationQuotationFields(String.class.getSimpleName(), context, body.getJsonObject("driver"), newBody, referenceParams, false);

                                referenceParams = new JsonArray().add("driver_cost").add("amount").add("discount").add("total_amount");
                                this.validationQuotationFields(Double.class.getSimpleName(), context, body, newBody, referenceParams, true);

                                newBody.put("has_driver", true).put("driver", body.getJsonArray("driver"));
                            }
                        }
                        referenceParams = new JsonArray().add("first_name").add("last_name").add("phone").add("email")
                                .add("address").add("credential_type").add("no_credential").add("file_credential");
                        this.validationQuotationFields(String.class.getSimpleName(), context, body, newBody, referenceParams, false);

                        referenceParams = new JsonArray().add("first_name").add("last_name").add("phone").add("email")
                                .add("address").add("credential_type").add("no_credential").add("file_credential");
                        this.validationQuotationFields(String.class.getSimpleName(), context, body, newBody, referenceParams, false);



                        context.clearUser();
                        context.setBody(newBody.toBuffer());
                        this.register(context);
                    } catch (Throwable t){
                        t.printStackTrace();
                        UtilsResponse.responseError(context, t);
                    }
                });
            }
        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    protected void validationQuotationFields(String refClass, RoutingContext context, JsonObject body, JsonObject newBody, JsonArray params, Boolean required){
        try {
            for(int i=0; i < params.size(); i++){
                if(body.containsKey(params.getString(i))){
                    if (refClass.equals(Double.class.getSimpleName())){
                        newBody.put(params.getString(i), body.getDouble(params.getString(i)));
                    } else if (refClass.equals(String.class.getSimpleName())){
                        newBody.put(params.getString(i), body.getString(params.getString(i)));
                    } else if (refClass.equals(Integer.class.getSimpleName())){
                        newBody.put(params.getString(i), body.getInteger(params.getString(i)));
                    }
                } else if(!body.containsKey(params.getString(i)) && required){
                    throw new PropertyValueException(params.getString(i), MISSING_REQUIRED_VALUE);
                }
            }
        } catch (PropertyValueException e) {
            e.printStackTrace();
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void register(RoutingContext context) {
        try {
            if (this.isValidCreateDataToRegisterVans(context)) {
                this.addReservationCode(context);
                //driver
                JsonObject body = context.getBodyAsJson();
                //body.put("updated_by", userId);
                body.put(UPDATED_BY, context.<Integer>get(USER_ID));
                JsonObject driver = body.getJsonObject("driver");
                JsonArray extras = body.getJsonArray("rental_extra_charges");
                JsonArray evidences = body.getJsonArray("evidences");
                JsonArray payments = body.getJsonArray("payments");
                JsonObject cashChange = body.getJsonObject("cash_change");
                try {
                    // validate branchoffices pickup and leave
                    if (body.getInteger("pickup_branchoffice_id") == null) {
                        throw new PropertyValueException("pickup_branchoffice_id", MISSING_REQUIRED_VALUE);
                    }
                    if (body.getInteger("leave_branchoffice_id") == null) {
                        throw new PropertyValueException("leave_branchoffice_id", MISSING_REQUIRED_VALUE);
                    }

                    //validate rental amount
                    final Double rentalAmount = body.getDouble("total_amount");
                    Double guaranteeDeposit = body.getDouble("guarantee_deposit");
                    if (rentalAmount == null) {
                        throw new PropertyValueException("total_amount", MISSING_REQUIRED_VALUE);
                    } else if (!(rentalAmount > 0)) {
                        throw new PropertyValueException("total_amount", "has to be grater than 0");
                    }
                    if (guaranteeDeposit == null) {
                        throw new PropertyValueException("guarantee_deposit", MISSING_REQUIRED_VALUE);
                    } else if (!(guaranteeDeposit > 0)) {
                        throw new PropertyValueException("guarantee_deposit", "has to be grater than 0");
                    }
                    if (driver != null) {
                        isNameAndNotNull(driver, "first_name", "driver");
                        isNameAndNotNull(driver, "last_name", "driver");
                        isDate(driver, "birthday", "driver");
                        isEmptyAndNotNull(driver, "no_licence", "driver");
                        isDateAndNotNull(driver, "expired_at", "driver");
                        isEmpty(driver, "file_licence", "driver");
                        //driver.put(CREATED_BY, userId);
                        driver.put(CREATED_BY, context.<Integer>get(USER_ID));
                    }
                    double sumExtra = 0;
                    if (extras != null) {
                        for (int i = 0; i < extras.size(); i++) {
                            JsonObject extra = extras.getJsonObject(i);
                            UtilsValidation.isBoolean(extra, "on_reception", "rental_extra_charges");
                            sumExtra += extra.getDouble("amount");
                            //extra.put(CREATED_BY, userId);
                            extra.put(CREATED_BY, context.<Integer>get(USER_ID));
                        }
                    }
                    if (evidences != null) {
                        for (int i = 0; i < evidences.size(); i++) {
                            JsonObject evidence = evidences.getJsonObject(i);
                            //evidence.put(CREATED_BY, userId);
                            evidence.put(CREATED_BY, context.<Integer>get(USER_ID));
                        }
                    }

                    double sumPayments = 0;
                    double sumDeposit = 0;

                    if (payments != null && !payments.isEmpty()) {
                        if (cashChange == null) {
                            throw new PropertyValueException("cash_change", "Needed if has payments");
                        }
                        isGraterEqualAndNotNull(cashChange, "paid", 0, "cash_change");
                        isGraterEqualAndNotNull(cashChange, "total", 0, "cash_change");
                        isGraterEqualAndNotNull(cashChange, "paid_change", 0, "cash_change");
                        double paid = cashChange.getDouble("paid");
                        double total = cashChange.getDouble("total");
                        double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                        for (int i = 0; i < payments.size(); i++) {
                            JsonObject payment = payments.getJsonObject(i);
                            //payment.put(CREATED_BY, userId);
                            payment.put(CREATED_BY, context.<Integer>get(USER_ID));
                            //validate payment amount
                            Double paymentAmount = payment.getDouble("amount");
                            if (paymentAmount == null) {
                                throw new PropertyValueException("payment_amount", MISSING_REQUIRED_VALUE);
                            }
                            if (!(paymentAmount > 0)) {
                                throw new PropertyValueException("payment_amount", "has to be grater than 0");
                            }
                            Boolean extraCharge = payment.getBoolean("is_extra_charge", false);
                            Boolean isDeposit = payment.getBoolean("is_deposit");
                            if (isDeposit != null && isDeposit) {
                                if (extraCharge) {
                                    throw new Exception("is deposit can't be true if is_extra_charge is true");
                                }
                                sumDeposit += paymentAmount;
                                continue;
                            }
                            sumPayments += paymentAmount;
                        }
                        double totalPayments = sumPayments;

                        double difference_paid = UtilsMoney.round(paid - total, 2);
                        double mustPay = rentalAmount;
                        if(sumDeposit > 0){
                            mustPay += guaranteeDeposit;
                            totalPayments  += sumDeposit;
                        }
                        if(paid_change > 0) {
                            if (total < mustPay) {
                                throw new Exception("The total " + total + " is lower than amount that must to be pay " + mustPay);
                            } else if (total > mustPay) {
                                throw new Exception("The total " + total + " is greater than amount that must to be pay " + mustPay);
                            }
                        }

                        if (paid_change > difference_paid) {
                            throw new Exception("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                        } else if (paid_change < difference_paid) {
                            throw new Exception("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                        }
                    }
                    if (sumPayments > rentalAmount) {
                        throw new Exception("can't be lower than the sum of the payments amount");
                    } else if (sumDeposit > guaranteeDeposit) {
                        throw new Exception("can't be lower than the sum of the deposit payments amount");
                    }
                    //check if is totally paid
                    if (sumPayments == rentalAmount && sumDeposit == guaranteeDeposit) {
                        body.put("payment_status", "1");
                    } else {
                        if (sumPayments == 0 && sumDeposit == 0) {
                            body.put("payment_status", "0");
                        } else {
                            body.put("payment_status", "2");
                        }
                    }

                    isBoolean(body, "is_quotation");
                    Boolean isQuotation = body.getBoolean("is_quotation");
                    if (isQuotation != null && isQuotation) {
                        body.put("rent_status", 5);
                    }
                    body.put(CREATED_BY, context.<Integer>get(USER_ID));

                    Future<Message<JsonObject>> f1 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "quotation_expired_after"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f1.completer());

                    Future<Message<JsonObject>> f2 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "rental_minamount_percent"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f2.completer());

                    Future<Message<JsonObject>> f3 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "iva"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f3.completer());

                    Future<Message<JsonObject>> f4 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "currency_id"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f4.completer());

                    Future<Message<JsonObject>> f5 = Future.future();
                    vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                            new JsonObject().put("vehicleId", body.getInteger("vehicle_id"))
                                    .put("travelDate", body.getString("departure_date") + " " + body.getString("departure_time") + ":00")
                                    .put("arrivalDate", body.getString("return_date") + " " + body.getString("return_time") + ":00"),
                            options(ACTION_GET_PRICE), f5.completer());

                    Future<Message<JsonObject>> f6 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "driver_van_id"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f6.completer());

                    Future<Message<JsonObject>> f7 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "van_earnings"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f7.completer());

                    Future<Message<JsonObject>> f10 = Future.future();
                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                            new JsonObject().put("fieldName", "extra_earning_percent"),
                            options(ACTION_GET_CONFIG_BY_FIELD), f10.completer());

                    final double sumPaymentsFinal = sumPayments;
                    final double sumExtraFinal = sumExtra;
                    List<Future> futureVanList = new ArrayList<>(Arrays.asList(f1,f2,f3,f4,f6,f7,f10));
                    CompositeFuture.all(futureVanList).setHandler(confReply -> {
                        if (confReply.succeeded()) {
                            JsonObject field1 = confReply.result().<Message<JsonObject>>resultAt(0).body();
                            JsonObject field2 = confReply.result().<Message<JsonObject>>resultAt(1).body();
                            JsonObject field3 = confReply.result().<Message<JsonObject>>resultAt(2).body();
                            JsonObject field4 = confReply.result().<Message<JsonObject>>resultAt(3).body();

                            JsonObject field6 = confReply.result().<Message<JsonObject>>resultAt(4).body();
                            JsonObject field7 = confReply.result().<Message<JsonObject>>resultAt(5).body();
                            JsonObject field10 = confReply.result().<Message<JsonObject>>resultAt(6).body();

                            int quotationExpiredAfter = Integer.parseInt(field1.getString("value", "15"));
                            double rentalMinAmountPercent = Double.parseDouble(field2.getString("value", "30"));
                            double ivaPercent = Double.parseDouble(field3.getString("value", "16"));
                            int currencyId = Integer.parseInt(field4.getString("value", "0"));

                            Integer driverVanId = Integer.parseInt(field6.getString("value", "39"));
                            Double vanEarning = Double.parseDouble(field7.getString("value", "10"));
                            Double profitPercent = Double.parseDouble(field10.getString("value"));
                            if (currencyId == 0){
                                UtilsResponse.responsePropertyValue(context,
                                        new PropertyValueException("currency_id", "Currency id is not defined"));
                                return;
                            }
                            if (cashChange != null) {
                                cashChange.put("iva_percent", ivaPercent);
                            }
                            body.put("quotation_expired_after", quotationExpiredAfter);
                            body.put("rental_minamount_percent", rentalMinAmountPercent);

                            Future<Message<JsonObject>> f8 = Future.future();
                            vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                                    new JsonObject().put("driverVanId", driverVanId).put("vanEarning", vanEarning)
                                            .put("travelDate", body.getString("departure_date") + " " + body.getString("departure_time") + ":00")
                                            .put("arrivalDate", body.getString("return_date") + " " + body.getString("return_time") + ":00"),
                                    options(ACTION_GET_DRIVER_COST), f8.completer());


                            Future<Message<JsonObject>> f9 = Future.future();
                            vertx.eventBus().send(VehicleDBV.class.getSimpleName(), new JsonObject()
                                            .put("travelDate", body.getString("departure_date"))
                                            .put("arrivalDate", body.getString("return_date"))
                                            .put("vehicleId", body.getInteger("vehicle_id")),
                                    options(ACTION_IS_AVAILABLE_RENTAL_VEHICLE), f9.completer());

                            CompositeFuture.all(f5, f8, f9).setHandler(confDriverReply -> {
                                try {
                                    if (confDriverReply.failed()){
                                        throw confDriverReply.cause();
                                    }

                                    JsonObject field5 = confDriverReply.result().<Message<JsonObject>>resultAt(0).body();
                                    JsonObject field8 = confDriverReply.result().<Message<JsonObject>>resultAt(1).body();
                                    JsonArray availables = confDriverReply.result().<Message<JsonArray>>resultAt(2).body();

                                    int rentalDays = field5.getInteger("days", 0);
                                    double rentalAmountCost = field5.getDouble("total", 0d);
                                    double rentalPrice = field5.getDouble("price", 0d);

                                    double driverPrice = 0.00;
                                    double driverCost = 0.00;
                                    if(field8.getDouble("total", 0d) != null){
                                        driverPrice = field8.getDouble("total", 0d);
                                        driverCost = field8.getDouble("total", 0d);
                                    } else {
                                        throw new Exception("Driver cost not assigned");
                                    }

                                    try {
                                        Integer rentStatus = body.getInteger("rent_status", 1);
                                        if (rentStatus != 5) {
                                            double toPaid = UtilsMoney.round( (rentalAmount * rentalMinAmountPercent / 100), 2);
                                            if (sumPaymentsFinal < toPaid) {
                                                throw new Exception("the rent must have a "
                                                        + rentalMinAmountPercent + "% paid to register");
                                            }
                                        }

                                        // Check rental price
                                        if(rentalAmountCost > body.getDouble("amount")){
                                            throw new Exception("Rental amount $ " + body.getDouble("amount") + " is lower than cost of " + rentalDays + " days per $ " + rentalPrice + " rental's price.");
                                        } else if(rentalAmountCost < body.getDouble("amount")){
                                            throw new Exception("Rental amount $ " + body.getDouble("amount") + " is greather than cost of " + rentalDays + " days per $ " + rentalPrice + " rental's price.");
                                        }

                                        // check driver cost
                                        if(body.getBoolean("has_driver")){
                                            if(driverCost > body.getDouble("driver_cost", 0d)){
                                                throw new Exception("Driver cost $ " + body.getDouble("driver_cost") + " is lower than cost of " + rentalDays + "days per $ " + driverPrice + " driver's price.");
                                            } else if(driverCost < body.getDouble("driver_cost", 0d)){
                                                throw new Exception("Driver cost $ " + body.getDouble("driver_cost") + " is greather than cost of " + rentalDays + "days per $ " + driverPrice + " driver's price.");
                                            }
                                        }
                                        // Check the total_amount
                                        final double driverCostFinal = body.getDouble("driver_cost", 0d);
                                        double discount = body.getDouble("discount", 0d);
                                        double rentalTotalAmount = (sumExtraFinal + driverCostFinal + rentalAmountCost - discount);
                                        if(rentalTotalAmount > rentalAmount){
                                            throw new Exception("Rental total amount is lower than rental's charges.");
                                        } else if(rentalTotalAmount < rentalAmount){
                                            throw new Exception("Rental total amount is greather than rental's charges.");
                                        } else if(sumPaymentsFinal > rentalTotalAmount){
                                            throw new Exception("Payments are greather than rental total amount.");
                                        }
                                        body.put("rental_price", rentalPrice);
                                        // Check available rental van
                                        if(availables.isEmpty()){
                                            throw new Exception("Vehicle is booked already");
                                        } else {
                                            if(body.getInteger("total_passengers") > availables.getJsonObject(0).getInteger("seatings")){
                                                throw new Exception("Vehicle doesn't have enough room for " + body.getInteger("total_passengers").toString() + "passengers");
                                            }
                                            this.getEmployeeBranchoffice(context,(JsonObject employee) -> {
                                                body
                                                    .put("branchoffice_id", employee.getInteger("user_branchoffice"))
                                                    .put("iva", ivaPercent)
                                                    .put("profit_percent",profitPercent);
                                                this.vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_REGISTER), reply -> {
                                                    try {
                                                        if (reply.failed()){
                                                            throw reply.cause();
                                                        }
                                                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                                                        } else {
                                                            JsonObject result = (JsonObject) reply.result().body();
                                                            //responseOk(context, reply.result().body(), "Created");
                                                            if (result.getBoolean("is_quotation")){
                                                                this.vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_SEND_INFORMATIVE_EMAIL), replySendInformativeEmail -> {
                                                                    try {
                                                                        if (reply.failed()){
                                                                            throw reply.cause();
                                                                        }
                                                                        if (replySendInformativeEmail.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                                                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replySendInformativeEmail.result().body());
                                                                        } else {
                                                                            JsonObject resultEmail = (JsonObject) replySendInformativeEmail.result().body();
                                                                            result.put("email_status", resultEmail.getBoolean("email_status"));
                                                                            responseOk(context, result, "Created");
                                                                        }
                                                                    } catch (Throwable t){
                                                                        t.printStackTrace();
                                                                        responseError(context, "Ocurrió un error inesperado", t);
                                                                    }
                                                                });
                                                            } else {
                                                                responseOk(context, result, "Created");
                                                            }
                                                        }
                                                    } catch (PropertyValueException t){
                                                        t.printStackTrace();
                                                        responsePropertyValue(context, t);
                                                    } catch (Throwable t){
                                                        t.printStackTrace();
                                                        responseError(context, "Ocurrió un error inesperado", t.getMessage());
                                                    }
                                                });
                                            });

                                        }
                                    } catch (PropertyValueException e) {
                                        e.printStackTrace();
                                        responsePropertyValue(context, e);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        UtilsResponse.responseError(context, e.getMessage());
                                    }
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    UtilsResponse.responseError(context, t.getMessage());
                                }
                            });


                        } else {
                            UtilsResponse.responseError(context, confReply.cause().getMessage());
                        }
                    });
                } catch (PropertyValueException ex) {
                    ex.printStackTrace();
                    UtilsResponse.responsePropertyValue(context, ex);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    UtilsResponse.responseError(context, ex.getMessage());
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, t);
        }
    }

    private void deliver(RoutingContext context) {
        String deliveredAt = UtilsDate.sdfDataBase(new Date());
        JsonObject body = context.getBodyAsJson();
        if(body.containsKey("total_amount")){
            body.put("total_amount", UtilsMoney.round(body.getDouble("total_amount"), 2));
        }
        if(body.containsKey("cash_change")){
            body.put("cash_change", body.getJsonObject("cash_change").put("total", UtilsMoney.round(body.getJsonObject("cash_change").getDouble("total"), 2)));
        }
        JsonObject cashChange = body.getJsonObject("cash_change");
        Integer userId = context.<Integer>get(USER_ID);
        try {
            //validate confirmation of fields
            if (body.getString("no_credential") == null) {
                throw new PropertyValueException("no_credential",
                        "can´t be null");
            }
            if (body.getString("credential_type") == null) {
                throw new PropertyValueException("credential_type",
                        "can´t be null");
            }
            if (body.getString("file_credential") == null) {
                throw new PropertyValueException("file_credential",
                        "can´t be null");
            }
            if (body.getString("address") == null) {
                throw new PropertyValueException("address",
                        "can´t be null");
            }
            if (body.getInteger("kilometers_init") == null) {
                throw new PropertyValueException("kilometers_init",
                        "can´t be null");
            }

            JsonObject driver = body.getJsonObject("driver");
            boolean hasDriver = body.getBoolean("has_driver", false);
            if (driver != null && hasDriver) {
                Integer employeeId = driver.getInteger("employee_id");
                if (employeeId == null) {
                    throw new PropertyValueException("driver.employee_id",
                            "can´t be null if rental has driver");
                }
            }
            body.put("delivered_by", userId);
            body.put("delivered_at", deliveredAt);
            body.put("updated_by", userId);
            body.put("updated_at", deliveredAt);
            body.put("rent_status", 2);//in-transit

            Future<Message<JsonObject>> f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f1.completer());

            Future<Message<JsonObject>> f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            Future<Message<JsonObject>> f3 = Future.future();
            vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                    new JsonObject().put("reservation_code", body.getString("reservation_code")),
                    options(ACTION_GET_DEPOSIT), f3.completer());

            Future<Message<JsonObject>> f4 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "extra_earning_percent"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(confReply -> {
                try {
                    if (confReply.failed()){
                        throw confReply.cause();
                    }
                    JsonObject field1 = confReply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = confReply.result().<Message<JsonObject>>resultAt(1).body();
                    JsonObject field3 = confReply.result().<Message<JsonObject>>resultAt(2).body();
                    JsonObject field4 = confReply.result().<Message<JsonObject>>resultAt(3).body();
                    Double profitPercent = Double.parseDouble(field4.getString("value"));
                    body
                        .put("guarantee_deposit", field3 != null ? field3.getDouble("guarantee_deposit", 0.0) : 0.00)
                        .put("profit_percent", profitPercent);

                    try {
                        validatePaymentsOnEvent(body, userId);
                        double ivaPercent = Double.parseDouble(field1.getString("value", "16"));
                        int currencyId = Integer.parseInt(field2.getString("value", "0"));
                        if (currencyId == 0) {
                            UtilsResponse.responsePropertyValue(context,
                                    new PropertyValueException("currency_id", "Currency id is not defined"));
                            return;
                        }
                        if (cashChange != null) {
                            cashChange.put("iva_percent", ivaPercent);
                        }
                        body.put("iva_percent", ivaPercent);
                        this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                                body,
                                options(RentalDBV.ACTION_DELIVERY),
                                reply -> {
                                    this.genericResponse(context, reply);
                                });
                    } catch (PropertyValueException e) {
                        e.printStackTrace();
                        UtilsResponse.responseError(context, e);
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    UtilsResponse.responseError(context, t);
                }
            });

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            Logger.getLogger(RentalSV.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void reception(RoutingContext context) {
        try {
            // String receivedAt = UtilsDate.sdfDataBase(new Date());
            Integer userId = context.<Integer>get(USER_ID);
            JsonObject body = context.getBodyAsJson();
            body.put("updated_by", userId);
            body.put("updated_at", UtilsDate.sdfDataBase(new Date()));
            JsonObject cashChange = body.getJsonObject("cash_change");
            JsonArray extras = body.getJsonArray("rental_extra_charges");

            //validate confirmation of fields
            if (body.getInteger("kilometers_end") == null) {
                throw new PropertyValueException("kilometers_end",
                        "can´t be null");
            }

            if (extras != null) {
                for (int i = 0; i < extras.size(); i++) {
                    JsonObject extra = extras.getJsonObject(i);
                    extra.put("on_reception", true);
                }
            }
            JsonArray evidences = body.getJsonArray("evidences");
            if (evidences != null) {
                for (int i = 0; i < evidences.size(); i++) {
                    JsonObject evidence = evidences.getJsonObject(i);
                    evidence.put("on_departure", false);
                }
            }

            body.put("received_by", userId);
            //body.put("received_at", receivedAt);
            body.put("updated_by", userId);
            body.put("updated_at", UtilsDate.sdfDataBase(new Date()));
            body.put("rent_status", 3);//finished

            Future<Message<JsonObject>> f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f1.completer());

            Future<Message<JsonObject>> f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            Future<Message<JsonObject>> f3 = Future.future();
            vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                    new JsonObject().put("reservation_code", body.getString("reservation_code")),
                    options(ACTION_GET_DEPOSIT), f3.completer());

            //AQUIMERO
            Future<Message<JsonObject>> f4 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "extra_earning_percent"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(confReply -> {
                try {
                    if (confReply.failed()){
                        throw confReply.cause();
                    }
                    JsonObject field1 = confReply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = confReply.result().<Message<JsonObject>>resultAt(1).body();
                    JsonObject field3 = confReply.result().<Message<JsonObject>>resultAt(2).body();
                    JsonObject field4 = confReply.result().<Message<JsonObject>>resultAt(3).body();

                    Double profitPercent = Double.parseDouble(field4.getString("value"));
                    body.put("guarantee_deposit", field3 != null ? field3.getDouble("guarantee_deposit", 0.00) : 0.00);


                    try {
                        validatePaymentsOnReception(body, userId);
                        double ivaPercent = Double.parseDouble(field1.getString("value", "16"));
                        int currencyId = Integer.parseInt(field2.getString("value", "0"));
                        if (currencyId == 0) {
                            UtilsResponse.responsePropertyValue(context,
                                    new PropertyValueException("currency_id", "Currency id is not defined"));
                            return;
                        }
                        if (cashChange != null) {
                            cashChange.put("iva_percent", ivaPercent);
                        }
                        body
                            .put("iva", ivaPercent)
                            .put("profit_percent", profitPercent);
                        this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                                body,
                                options(RentalDBV.ACTION_RECEPTION),
                                reply -> {
                                    this.genericResponse(context, reply);
                                });
                    } catch (PropertyValueException e) {
                        UtilsResponse.responseError(context, e);
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    UtilsResponse.responseError(context, t);
                }
            });


        } catch (PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void addReservationCode(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put("reservation_code", UtilsID.generateID("R"));
        context.setBody(body.toBuffer());
    }

    private void reservationPaymentDetail(RoutingContext context) {
        String reservationCode = context.request().getParam("reservationCode");
        if (isReservationCodeValid(reservationCode, context)) {
            this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                    new JsonObject().put("reservationCode", reservationCode),
                    options(RentalDBV.ACTION_PAYMENT_DETAIL),
                    reply -> {
                this.genericResponse(context, reply);
                    });
        }
    }

    private void quotationDetail(RoutingContext context) {
        String reservationCode = context.request()
                .getParam("reservationCode");
        if (isReservationCodeValid(reservationCode, context)) {
            this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                    new JsonObject().put("reservationCode", reservationCode),
                    options(RentalDBV.ACTION_QUOTATION),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        }
    }

    private void reservationDetail(RoutingContext context) {
        this.validateToken(context, userId -> {
            String reservationCode = context.request()
                    .getParam("reservationCode");
            if (isReservationCodeValid(reservationCode, context)) {
                this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                        new JsonObject().put("reservationCode", reservationCode),
                        options(RentalDBV.ACTION_DETAIL),
                        reply -> {
                            this.genericResponse(context, reply);
                        });
            }

        });
    }

    private void getList(RoutingContext context) {
        this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                new JsonObject(),
                options(RentalDBV.ACTION_LIST),
                reply -> {
                    this.genericResponse(context, reply);
                });
    }

    private void partialPayment(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        Integer userId = context.<Integer>get(USER_ID);
        try {
            isEmptyAndNotNull(body, "reservation_code");
            isEmptyAndNotNull(body.getJsonArray("payments"), "payments");
            JsonArray payments = body.getJsonArray("payments");
            JsonObject cashChange = body.getJsonObject("cash_change");
            this.validatePaymentsData(payments, cashChange, userId);

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"),
                    options(ACTION_GET_CONFIG_BY_FIELD), (AsyncResult<Message<JsonObject>> res) -> {
                        try {
                            if (res.failed()){
                                throw res.cause();
                            }
                            float ivaPercent = Float.parseFloat(res.result().body().getString("value", "16"));
                            cashChange.put("iva_percent", ivaPercent);
                            this.vertx.eventBus().send(RentalDBV.class.getSimpleName(), body, options(RentalDBV.ACTION_PARTIAL_PAYMENT), reply -> {
                                try {
                                    if (reply.failed()){
                                        throw reply.cause();
                                    }
                                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                        responseWarning(context, reply.result().body());
                                    } else {
                                        responseOk(context, reply.result().body());
                                    }
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    responseError(context, null, t);
                                }
                            });
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, null, t);
                        }
                    });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        }
    }

    private void canCancel(RoutingContext context) {
        String reservationCode = context.request().getParam("reservationCode");
        if (this.isReservationCodeValid(reservationCode, context)) {
            vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("reservationCode", reservationCode),
                    options(ACTION_CAN_CANCEL), reply -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            if (reply.result().body() == null) {
                                UtilsResponse.responseWarning(context, "Reservation not found", "Element not found");
                            } else {
                                this.genericResponse(context, reply);
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            UtilsResponse.responseWarning(context, null, t);
                        }
                    });
        }
    }

    private void cancel(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            String reservationCode = body.getString("reservation_code");
            Integer paymentMethodId = body.getInteger("payment_method_id");
            Integer userId = context.<Integer>get(USER_ID);

            UtilsValidation.isGraterAndNotNull(body, "payment_method_id", 0);
            UtilsValidation.isDecimal(body, "penalty_amount");
            UtilsValidation.isEmptyAndNotNull(body, "reservation_code");
            UtilsValidation.isEmptyAndNotNull(body, "cancel_reason");

            vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("reservationCode", reservationCode), options(ACTION_CAN_CANCEL),
                    (AsyncResult<Message<JsonObject>> reply) -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            JsonObject canCancelBody = reply.result().body();
                            if (canCancelBody == null) {
                                throw new Exception("Reservation not found");
                            }
                            boolean canCancel = canCancelBody.getBoolean("can_cancel");
                            if (canCancel) {
                                canCancelBody.mergeIn(body).put(CREATED_BY, userId);
                                vertx.eventBus().send(this.getDBAddress(), canCancelBody, options(RentalDBV.ACTION_CANCEL), replyCancel -> {
                                    genericResponse(context, replyCancel);
                                });
                            } else {
                                this.genericResponseJsonObject(context, reply);
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, t);
                        }
                    });

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void rentalCanDeliver(RoutingContext context) {
        String reservationCode = context.request()
                .getParam("reservationCode");
        if (isReservationCodeValid(reservationCode, context)) {
            this.vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                    new JsonObject().put("reservationCode", reservationCode),
                    options(RentalDBV.ACTION_CAN_DELIVER),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        }
    }

    //validate reservation code
    private boolean isReservationCodeValid(String reservationCode, RoutingContext context) {
        if (reservationCode == null) {
            UtilsResponse.responsePropertyValue(context, new PropertyValueException("reservationCode", MISSING_REQUIRED_VALUE));
            return false;
        }
        if (reservationCode.isEmpty()) {
            UtilsResponse.responsePropertyValue(context, new PropertyValueException("reservationCode", MISSING_REQUIRED_VALUE));
            return false;
        }
        return true;
    }

    private void validatePaymentsOnEvent(JsonObject body, int userId) throws PropertyValueException {
        JsonArray payments = body.getJsonArray("payments");
        final Double rentalAmount = body.getDouble("total_amount", 0d);
        Double guaranteeDeposit = body.getDouble("guarantee_deposit", 0d);
        JsonObject cashChange = body.getJsonObject("cash_change");
        JsonObject totals = this.validatePaymentsData(payments, cashChange, userId);

        if (totals.getDouble("sumPayment") > rentalAmount) {
            throw new PropertyValueException("total_amount", "can't be lower than the sum of the payments amount");
        }
        if (totals.getDouble("sumDeposit") > guaranteeDeposit) {
            throw new PropertyValueException("guarantee_deposit", "can't be lower than the sum of the deposit payments amount");
        }
    }

    private void validatePaymentsOnReception(JsonObject body, int userId) throws PropertyValueException {
        JsonArray payments = body.getJsonArray("payments");
        JsonArray checklistCharges = body.getJsonArray("checklist");
        JsonArray extraCharges = body.getJsonArray("rental_extra_charges");
        Double guaranteeDeposit = body.getDouble("guarantee_deposit", 0d);
        Double penalty = body.getDouble("penalty_amount", 0d);
        JsonObject cashChange = body.getJsonObject("cash_change");

        Double charges = 0d;
        Double difference = 0d;
        Double paidPayments = 0d;
        if (extraCharges != null && !extraCharges.isEmpty()) {
            for (int i = 0; i < extraCharges.size(); i++) {
                charges += extraCharges.getJsonObject(i).getDouble("amount");
            }
        }

        if (checklistCharges != null && !checklistCharges.isEmpty()) {
            for (int i = 0; i < checklistCharges.size(); i++) {
                charges += checklistCharges.getJsonObject(i).getDouble("damage_amount");
            }
        }

        if (payments != null && !payments.isEmpty()) {
            for (int i = 0; i < payments.size(); i++) {
                paidPayments += payments.getJsonObject(i).getDouble("amount");
            }
        }
        System.out.println(penalty);
        System.out.println(charges);
        System.out.println(paidPayments);

        difference = UtilsMoney.round(penalty + charges - guaranteeDeposit, 2);
        if(difference > 0){
            if(difference < paidPayments){
                throw new PropertyValueException("payments", "Payments (" + paidPayments + ") can't be lower than the difference (" + difference + ") between extra charges and guarantee deposit ");
            } else if(difference > paidPayments){
                throw new PropertyValueException("payments", "Payments (" + paidPayments + ") can't be greater than the difference (" + difference + ") between extra charges and guarantee deposit ");
            } else {
                if (cashChange == null) {
                    throw new PropertyValueException("cash_change", "Needed if has payments");
                }
                isGraterEqualAndNotNull(cashChange, "paid", 0, "cash_change");
                isGraterEqualAndNotNull(cashChange, "total", 0, "cash_change");
                isGraterEqualAndNotNull(cashChange, "paid_change", 0, "cash_change");
                double paid = cashChange.getDouble("paid");
                double total = cashChange.getDouble("total");
                double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);

                double totalPayments = difference;
                double difference_paid = UtilsMoney.round(paid - total, 2);
                if (total > totalPayments) {
                    throw new PropertyValueException("total", "The payment " + total + " is greater than the total " + totalPayments);
                } else if (total < totalPayments) {
                    throw new PropertyValueException("total", "The payment " + total + " is lower than the total " + totalPayments);
                } else if (paid_change > difference_paid) {
                    throw new PropertyValueException("paid_change", "The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                } else if (paid_change < difference_paid) {
                    throw new PropertyValueException("paid_change", "The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                }
                body.put("expense", guaranteeDeposit);
            }
        } else if(difference < 0){
            body.put("expense", difference * -1);
            if(paidPayments > 0){
                throw new PropertyValueException("payments", "Nothing to pay");
            }
        }

    }


    private JsonObject validatePaymentsData(JsonArray payments, JsonObject cashChange, int userId) throws PropertyValueException {
        double sumPayment = 0;
        double sumDeposit = 0;
        double sumExtra = 0;
        if (payments != null && !payments.isEmpty()) {
            if (cashChange == null) {
                throw new PropertyValueException("cash_change", "Needed if has payments");
            }
            isGraterEqualAndNotNull(cashChange, "paid", 0, "cash_change");
            isGraterEqualAndNotNull(cashChange, "total", 0, "cash_change");
            isGraterEqualAndNotNull(cashChange, "paid_change", 0, "cash_change");
            Double paid = cashChange.getDouble("paid");
            Double total = UtilsMoney.round(cashChange.getDouble("total"), 2);
            Double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
            for (int i = 0; i < payments.size(); i++) {
                JsonObject payment = payments.getJsonObject(i);
                payment.put(CREATED_BY, userId);
                //validate payment amount
                Double paymentAmount = payment.getDouble("amount");
                if (paymentAmount == null) {
                    throw new PropertyValueException("payment_amount", MISSING_REQUIRED_VALUE);
                }
                if (!(paymentAmount > 0)) {
                    throw new PropertyValueException("payment_amount", "has to be grater than 0");
                }
                Boolean extraCharge = payment.getBoolean("is_extra_charge", false);
                if (extraCharge) {
                    sumExtra += paymentAmount;
                    continue;
                } else {
                    payment.put("is_extra_charge", false);
                }
                Boolean isDeposit = payment.getBoolean("is_deposit");
                if (isDeposit != null && isDeposit) {
                    sumDeposit += paymentAmount;
                    if (extraCharge) {
                        throw new PropertyValueException("is_deposit", "is deposit can't be true if is_extra_charge is true");
                    }
                    continue;
                }
                sumPayment += paymentAmount;
            }
            Double totalPayments = sumPayment + sumDeposit + sumExtra;
            Double difference_paid = UtilsMoney.round(paid - total, 2);
            if (total > totalPayments) {
                throw new PropertyValueException("total", "The payment " + total + " is greater than the total " + totalPayments);
            } else if (total < totalPayments) {
                throw new PropertyValueException("total", "The payment " + total + " is lower than the total " + totalPayments);
            } else if (paid_change > difference_paid) {
                throw new PropertyValueException("paid_change", "The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
            } else if (paid_change < difference_paid) {
                throw new PropertyValueException("paid_change", "The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
            }
        }
        return new JsonObject().put("sumPayment", sumPayment).put("sumDeposit", sumDeposit);
    }

    private void getRentalPrice(RoutingContext context){
        JsonObject message = new JsonObject()
                .put("travelDate", context.request().getParam("travelDateTime"))
                .put("arrivalDate", context.request().getParam("arrivalDateTime"))
                .put("vehicleId", Integer.valueOf(context.request().getParam("vehicleId")));
        vertx.eventBus().send(RentalDBV.class.getSimpleName(), message,
                options(ACTION_GET_PRICE),
                reply -> {
                    this.genericResponse(context, reply);
                });
    }

    private void getDriverCost(RoutingContext context){
        JsonObject message = new JsonObject()
                .put("travelDate", context.request().getParam("travelDateTime"))
                .put("arrivalDate", context.request().getParam("arrivalDateTime"));

        Future<Message<JsonObject>> f1 = Future.future();
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                new JsonObject().put("fieldName", "driver_van_id"),
                options(ACTION_GET_CONFIG_BY_FIELD), f1.completer());

        Future<Message<JsonObject>> f2 = Future.future();
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                new JsonObject().put("fieldName", "van_earnings"),
                options(ACTION_GET_CONFIG_BY_FIELD), f2.completer());

        CompositeFuture.all(f1, f2).setHandler(confReply -> {
            try {
                if (confReply.failed()){
                    throw confReply.cause();
                }
                JsonObject field1 = confReply.result().<Message<JsonObject>>resultAt(0).body();
                JsonObject field2 = confReply.result().<Message<JsonObject>>resultAt(1).body();

                int driverVanId = Integer.parseInt(field1.getString("value", "39"));
                int vanEarning = Integer.parseInt(field2.getString("value", "10"));

                message.put("driverVanId", driverVanId)
                        .put("vanEarning", vanEarning);
                vertx.eventBus().send(RentalDBV.class.getSimpleName(), message,
                        options(ACTION_GET_DRIVER_COST),
                        replyPrice -> {
                            this.genericResponse(context, replyPrice);
                        });
            } catch (Throwable t){
                t.printStackTrace();
                UtilsResponse.responseError(context, t);
            }
        });
    }

    private void salesReport(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "rental");
            isDateTimeAndNotNull(body, "end_date", "rental");
            isBooleanAndNotNull(body, "is_date_created_at");
            isGraterAndNotNull(body, "page", 0);
            isGraterAndNotNull(body, "limit", 0);

            Future<Message<JsonObject>> f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "driver_van_id"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f1.completer());

            Future<Message<JsonObject>> f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "extra_earning_percent"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(confReply -> {
                try{
                    if (confReply.failed()){
                        throw confReply.cause();
                    }
                    JsonObject field1 = confReply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = confReply.result().<Message<JsonObject>>resultAt(1).body();

                    Integer driverVanId = Integer.parseInt(field1.getString("value"));
                    Double extraEarningPercent = Double.parseDouble(field2.getString("value"));

                    body
                            .put("driver_van_id", driverVanId)
                            .put("extra_earning_percent", extraEarningPercent);
                    vertx.eventBus().send(RentalDBV.class.getSimpleName(), body,
                            options(ACTION_SALES_REPORT),
                            replyPrice -> {
                                this.genericResponse(context, replyPrice);
                            });
                } catch (Throwable t){
                    t.printStackTrace();
                    UtilsResponse.responseError(context, t);
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }
    private void salesReportTotals(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "rental");
            isDateTimeAndNotNull(body, "end_date", "rental");
            isBooleanAndNotNull(body, "is_date_created_at");

            Future<Message<JsonObject>> f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "driver_van_id"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f1.completer());

            Future<Message<JsonObject>> f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "extra_earning_percent"),
                    options(ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(confReply -> {
                try{
                    if (confReply.failed()){
                        throw confReply.cause();
                    }
                    JsonObject field1 = confReply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = confReply.result().<Message<JsonObject>>resultAt(1).body();

                    Integer driverVanId = Integer.parseInt(field1.getString("value"));
                    Double extraEarningPercent = Double.parseDouble(field2.getString("value"));

                    body
                            .put("driver_van_id", driverVanId)
                            .put("extra_earning_percent", extraEarningPercent);
                    vertx.eventBus().send(RentalDBV.class.getSimpleName(), body,
                            options(ACTION_SALES_TOTALS),
                            replyPrice -> {
                                this.genericResponse(context, replyPrice);
                            });
                } catch (Throwable t){
                    t.printStackTrace();
                    UtilsResponse.responseError(context, t);
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void vansList(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "rental");
            isDateTimeAndNotNull(body, "end_date", "rental");



                vertx.eventBus().send(RentalDBV.class.getSimpleName(), body,
                        options(ACTION_VANS_LIST),
                        reply -> {
                            this.genericResponse(context, reply);
                        });
            } catch (Throwable t){
                t.printStackTrace();
                UtilsResponse.responseError(context, t);
            }
    }

    private void sendQuotation(RoutingContext context){
        String reservationCode = context.request().getParam("reservationCode");
        JsonObject body = new JsonObject().put("reservation_code", reservationCode);

        vertx.eventBus().send(RentalDBV.class.getSimpleName(), body, options(ACTION_SEND_INFORMATIVE_EMAIL), (AsyncResult<Message<Object>> reply) -> {
            this.genericResponse(context, reply, "Found");
        });
    }

    private void updateRent(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, UPDATE_RENTAL_INFO);

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
