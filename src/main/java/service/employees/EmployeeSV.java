/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.employees;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.employees.EmployeeDBV;
import database.parcel.enums.SHIPMENT_TYPE;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsResponse;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

import utils.UtilsValidation;
import utils.UtilsValidation.PropertyValueException;

import java.util.Arrays;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class EmployeeSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EmployeeDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/employees";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/attendance/register", AuthMiddleware.getInstance(), this::registerAttendance);
        this.addHandler(HttpMethod.GET, "/attendance/report", AuthMiddleware.getInstance(), this::getAttendanceReport);
        this.addHandler(HttpMethod.GET, "/attendance/report/driver", AuthMiddleware.getInstance(), this::driverAttendanceReport);
        this.addHandler(HttpMethod.GET, "/report/avalaibleEmployees", AuthMiddleware.getInstance(), this::avalaibleEmployees);
        this.addHandler(HttpMethod.GET, "/report/avalaibleEmployeesByJobId/:jobId", AuthMiddleware.getInstance(), this::avalaibleEmployeesByJobId);
        this.addHandler(HttpMethod.GET, "/report/availableVanDriver/:pickupCityId", AuthMiddleware.getInstance(), this::availableVanDriver);
        this.addHandler(HttpMethod.GET, "/report/byJob", AuthMiddleware.getInstance(), this::employeesByJob);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/fingerPrint/register", AuthMiddleware.getInstance(), this::fingerPrintRegister);
        this.addHandler(HttpMethod.GET, "/search/:search_term", AuthMiddleware.getInstance(), this::searchEmployee);
        this.addHandler(HttpMethod.POST, "/searchV2", AuthMiddleware.getInstance(), this::searchEmployeeV2);
        this.addHandler(HttpMethod.GET, "/allOperator", PublicRouteMiddleware.getInstance(), this::getAllOperator);
        this.addHandler(HttpMethod.POST, "/config/reception", AuthMiddleware.getInstance(), this::registerParcelInitConfig);
        this.addHandler(HttpMethod.PUT, "/config/reception", AuthMiddleware.getInstance(), this::updateParcelInitConfig);
        this.addHandler(HttpMethod.GET, "/config/reception/:employee_id", AuthMiddleware.getInstance(), this::getParcelInitConfig);
        this.addHandler(HttpMethod.DELETE, "/config/reception/:id", AuthMiddleware.getInstance(), this::deleteParcelInitConfig);
        this.addHandler(HttpMethod.POST, "/config/reception/branchoffices", AuthMiddleware.getInstance(), this::registerParcelBranchofficeInitConfig);
        this.addHandler(HttpMethod.PUT, "/config/reception/branchoffices", AuthMiddleware.getInstance(), this::updateParcelBranchofficeInitConfig);
        this.addHandler(HttpMethod.GET, "/config/reception/branchoffices/:employee_id", AuthMiddleware.getInstance(), this::getParcelBranchofficeInitConfig);
        this.addHandler(HttpMethod.POST, "/config/reception/confirmDialogs", AuthMiddleware.getInstance(), this::registerParcelConfirmDialogsInitConfig);
        this.addHandler(HttpMethod.PUT, "/config/reception/confirmDialogs", AuthMiddleware.getInstance(), this::updateParcelConfirmDialogsInitConfig);
        this.addHandler(HttpMethod.GET, "/config/reception/confirmDialogs/:employee_id", AuthMiddleware.getInstance(), this::getParcelConfirmDialogsInitConfig);
        this.addHandler(HttpMethod.POST, "/config/exchange", AuthMiddleware.getInstance(), this::registerExchangeInitConfig);
        this.addHandler(HttpMethod.PUT, "/config/exchange", AuthMiddleware.getInstance(), this::updateExchangeInitConfig);
        this.addHandler(HttpMethod.GET, "/config/exchange/:employee_id", AuthMiddleware.getInstance(), this::getExchangeInitConfig);
        this.addHandler(HttpMethod.DELETE, "/config/exchange/:id", AuthMiddleware.getInstance(), this::deleteExchangeInitConfig);
        super.start(startFuture);
    }

    @Override

    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "name");
            isName(body, "last_name");
            isDate(body, "birthday");
            isPhoneNumber(body, "cellphone");
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            isDate(body, "start_working_at");
            isDate(body, "finish_working_at");
            isBetweenRange(body, "blood_type", 0, 7);
            isContained(body, "marital_status", "single", "married", "widowed");
            isContained(body, "scholarship", "elementary", "middleschool", "technical", "highschool", "college", "master", "phd", "other");
            isGrater(body, "user_id", 0);
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "name");
            isNameAndNotNull(body, "last_name");
            isDateAndNotNull(body, "birthday");
            isPhoneNumberAndNotNull(body, "cellphone");
            isPhoneNumber(body, "phone");
            isMailAndNotNull(body, "email");
            isDate(body, "start_working_at");
            isDate(body, "finish_working_at");
            isBetweenRangeAndNotNull(body, "blood_type", 0, 7);
            isContained(body, "marital_status", "single", "married", "widowed");
            isContainedAndNotNull(body, "scholarship", "elementary", "middleschool", "technical", "highschool", "college", "master", "phd", "other");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }


    protected void employeesByJob(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.EMPLOYEES_BY_JOB);
        vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    protected void availableVanDriver(RoutingContext context) {
        Integer pickupCityId = Integer.valueOf(context.request().getParam("pickupCityId"));
        this.vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "driver_van_id"),
                new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replySettings -> {
            try {
                if (replySettings.failed()){
                    throw replySettings.cause();
                }
                JsonObject generalSetting = (JsonObject) replySettings.result().body();
                Integer driverVanId = Integer.valueOf(generalSetting.getString("value"));
                JsonObject body = new JsonObject().put("driver_van_id", driverVanId)
                        .put("pickup_city_id", pickupCityId);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_AVAILABLE_VAN_DRIVER);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, reply.result().body(), "Found");
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Vans driver id not found", t);
            }
        });
    }

    protected void avalaibleEmployees(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_AVALAIBLE_EMPLOYEES);
        vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Found");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    protected void avalaibleEmployeesByJobId(RoutingContext context) {
        try {
            Integer jobId = Integer.valueOf(context.request().getParam("jobId"));
            JsonObject body = new JsonObject().put("jobId", jobId);
            isGraterAndNotNull(body, "jobId", 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_AVALAIBLE_EMPLOYEES_BY_JOB_ID);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    private void fingerPrintRegister(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.FINGER_PRINT_REGISTER);
        vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body(), "Created");
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    private void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            //validates the employee data
            isBoolean(body, "dissociate");
            isNameAndNotNull(body, "name");
            isNameAndNotNull(body, "last_name");
            isDateAndNotNull(body, "birthday");
            isPhoneNumberAndNotNull(body, "cellphone");
            isPhoneNumber(body, "phone");
            isMailAndNotNull(body, "email");
            isDate(body, "start_working_at");
            isDate(body, "finish_working_at");
            isBetweenRangeAndNotNull(body, "blood_type", 0, 7);
            isContained(body, "marital_status", "single", "married", "widowed");
            isContainedAndNotNull(body, "scholarship", "elementary", "middleschool", "technical", "highschool", "college", "master", "phd", "other");
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            //validates schedules values
            JsonArray schedules = body.getJsonArray("schedules");
            if (schedules != null) {
                for (int i = 0; i < schedules.size(); i++) {
                    JsonObject schedule = schedules.getJsonObject(i);
                    isHour24AndNotNull(schedule, "hour_start", "schedules");
                    isHour24AndNotNull(schedule, "hour_end", "schedules");
                    isHour24(schedule, "break_start", "schedules");
                    isHour24(schedule, "break_end", "schedules");
                    isDateAndNotNull(schedule, "date_start", "schedules");
                    isDate(schedule, "date_end", "schedules");
                    isBoolean(schedule, "sun", "schedules");
                    isBoolean(schedule, "mon", "schedules");
                    isBoolean(schedule, "tue", "schedules");
                    isBoolean(schedule, "wen", "schedules");
                    isBoolean(schedule, "thu", "schedules");
                    isBoolean(schedule, "fri", "schedules");
                    isBoolean(schedule, "sat", "schedules");
                    schedule.put(CREATED_BY, context.<Integer>get(USER_ID));
                }
            }
            //validates contacts
            JsonArray contacts = body.getJsonArray("contacts");
            JsonObject contact;
            if (contacts != null) {
                for (int i = 0; i < contacts.size(); i++) {
                    contact = contacts.getJsonObject(i);
                    isNameAndNotNull(contact, "name", "contacts");
                    isNameAndNotNull(contact, "last_name", "contacts");
                    isPhoneNumberAndNotNull(contact, "phone", "contacts");
                    isBetweenRange(contact, "relationship", 0, 4, "contacts");
                    contact.put(CREATED_BY, context.<Integer>get(USER_ID));
                }
            }
            //validates casefiles if exists
            JsonArray casefiles = body.getJsonArray("casefiles");
            if (casefiles != null) {
                JsonObject casefile;
                for (int i = 0; i < casefiles.size(); i++) {
                    casefile = casefiles.getJsonObject(i);
                    isEmptyAndNotNull(casefile, "file", "casefiles");
                    casefile.put(CREATED_BY, context.<Integer>get(USER_ID));
                }
            }
            if (this.isValidCreateData(context)) {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.REGISTER);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
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
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void registerAttendance(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        try {
            // Validates the attendance data
            isDateTimeAndNotNull(body, "register_at", "employee_attendance");
            isGraterAndNotNull(body, "employee_id", 0);
            isContainedAndNotNull(body, "state", "in", "out");
            body.put(CREATED_BY, ctx.<Integer>get(USER_ID));

            vertx.eventBus().send(this.getDBAddress(), body, options(EmployeeDBV.ACTION_REGISTER_ATTENDANCE), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(ctx, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(ctx, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(ctx, t);
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(ctx, ex);
        }
    }

    private void getAttendanceReport(RoutingContext ctx) {
        HttpServerRequest req = ctx.request();
        try {
            // Validates the attendance data
            String branchofficeID = req.getParam("branchoffice_id");
            if (branchofficeID == null) {
                throw new PropertyValueException("branchoffice_id", MISSING_REQUIRED_VALUE);
            }
            JsonObject body = new JsonObject()
                    .put("start_date", req.getParam("start_date"))
                    .put("end_date", req.getParam("end_date"))
                    .put("branchoffice_id", Integer.valueOf(branchofficeID));
            isDateAndNotNull(body, "start_date", "employee");
            isDateAndNotNull(body, "end_date", "employee");
            isGraterAndNotNull(body, "branchoffice_id", 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(EmployeeDBV.ACTION_GET_ATTENDANCE_REPORT), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(ctx, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(ctx, reply.result().body(), "Found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(ctx, t);
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(ctx, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(ctx, ex.getMessage());
        }
    }
    
    private void driverAttendanceReport(RoutingContext ctx) {
        try {
            String sStart = ctx.queryParams().get("start_date");
            String sEnd = ctx.queryParams().get("end_date");
            if(sStart == null) {
                throw new PropertyValueException("start_date", "missing required value");
            }
            if(sEnd == null) {
                throw new PropertyValueException("end_date", "missing required value");
            }
            JsonObject body = new JsonObject()
                    .put("start_date", sStart)
                    .put("end_date", sEnd);

            vertx.eventBus().send(this.getDBAddress(), body, options(EmployeeDBV.ACTION_ATTENDANCE_DRIVER_REPORT), reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(ctx, reply.result().body(), "Found");
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(ctx, ex.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(ctx, ex);
        }
    }

    private void searchEmployee(RoutingContext context) {
        HttpServerRequest request = context.request();
        String searchTerm = request.getParam("search_term") != null ? request.getParam("search_term") : "";

        JsonObject body = new JsonObject()
                .put("searchTerm", searchTerm);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_SEARCH_BY_NAME_AND_LASTNAME);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }

    private void searchEmployeeV2(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, "searchTerm");
            isGraterAndNotNull(body, "job_id", 0);
            isGrater(body, _BRANCHOFFICE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_SEARCH_BY_NAME_AND_LASTNAME);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }

    private void getAllOperator(RoutingContext context) {
        String profile_id = context.request().getParam("profile_id") != null ? context.request().getParam("profile_id") : "";
        String id_branch = context.request().getParam("id_branch") != null ? context.request().getParam("id_branch") : "";
        JsonObject body= new JsonObject().put("profile_id",profile_id).put("id_branch",id_branch);

        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_GET_EMPLOYEES_OPERATOR);
        vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }
                responseOk(context, reply.result().body());
            } catch (Exception e) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    protected void registerParcelInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isContainedAndNotNull(body, _SHIPMENT_TYPE, SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue(), SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_REGISTER_PARCEL_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void updateParcelInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_UPDATE_PARCEL_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void getParcelInitConfig(RoutingContext context) {
        try {
            Integer employeeId = Integer.valueOf(context.request().getParam(_EMPLOYEE_ID));
            JsonObject body = new JsonObject().put(_EMPLOYEE_ID, employeeId);
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_GET_PARCEL_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void deleteParcelInitConfig(RoutingContext context) {
        try {
            Integer id = Integer.valueOf(context.request().getParam(ID));
            JsonObject body = new JsonObject().put(ID, id);
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_DELETE_PARCEL_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Deleted");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void registerParcelBranchofficeInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_REGISTER_PARCEL_BRANCHOFFICE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void updateParcelBranchofficeInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_UPDATE_PARCEL_BRANCHOFFICE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void getParcelBranchofficeInitConfig(RoutingContext context) {
        try {
            Integer employeeId = Integer.valueOf(context.request().getParam(_EMPLOYEE_ID));
            JsonObject body = new JsonObject().put(_EMPLOYEE_ID, employeeId);
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_GET_PARCEL_BRANCHOFFICE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void registerParcelConfirmDialogsInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_REGISTER_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void updateParcelConfirmDialogsInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_UPDATE_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void getParcelConfirmDialogsInitConfig(RoutingContext context) {
        try {
            Integer employeeId = Integer.valueOf(context.request().getParam(_EMPLOYEE_ID));
            JsonObject body = new JsonObject().put(_EMPLOYEE_ID, employeeId);
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_GET_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        }
    }

    protected void registerExchangeInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isContainedAndNotNull(body, _SHIPMENT_TYPE, SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue(), SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_REGISTER_EXCHANGE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Created");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    protected void updateExchangeInitConfig(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_UPDATE_EXCHANGE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Updated");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    protected void getExchangeInitConfig(RoutingContext context) {
        try {
            Integer employeeId = Integer.valueOf(context.request().getParam(_EMPLOYEE_ID));
            JsonObject body = new JsonObject().put(_EMPLOYEE_ID, employeeId);
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_GET_EXCHANGE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    protected void deleteExchangeInitConfig(RoutingContext context) {
        try {
            Integer id = Integer.valueOf(context.request().getParam(ID));
            JsonObject body = new JsonObject().put(ID, id);
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EmployeeDBV.ACTION_DELETE_EXCHANGE_INIT_CONFIG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Deleted");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex){
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }
    
}
