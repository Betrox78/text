package service.parcel;

import database.commons.ErrorCodes;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PARCEL_MANIFEST_ROUTE_LOG_TYPE;
import database.parcel.handlers.ParcelsManifestDBV.Exception.ParcelManifestException;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.CheckCashOutMiddleware;
import service.commons.middlewares.EmployeeMiddleware;
import service.commons.middlewares.PaymentMethodsMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.util.Objects;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

public class ParcelsManifestSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ParcelsManifestDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcelsManifest";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), CheckCashOutMiddleware.getInstance(vertx), EmployeeMiddleware.getInstance(vertx), this::register);
        this.addHandler(HttpMethod.GET, "/openList", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), PaymentMethodsMiddleware.getInstance(vertx), this::getOpenList);
        this.addHandler(HttpMethod.POST, "/checkEad", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::checkEad);
        this.addHandler(HttpMethod.POST, "/close", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::close);
        this.addHandler(HttpMethod.POST, "/initRouteEad", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::initRouteEad);
        this.addHandler(HttpMethod.POST, "/finish", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::finish);
        this.addHandler(HttpMethod.POST, "/report", AuthMiddleware.getInstance(), this::report);
        this.addHandler(HttpMethod.POST, "/report/details", AuthMiddleware.getInstance(), this::reportDetails);
        this.addHandler(HttpMethod.POST, "/checkEadContingency", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::checkEadContingency);
        this.addHandler(HttpMethod.GET, "/detail/:id", AuthMiddleware.getInstance(), PaymentMethodsMiddleware.getInstance(vertx), this::getDetail);
        this.addHandler(HttpMethod.POST, "/deleteParcelManifestDetail", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::deleteParcelManifestDetail);
        this.addHandler(HttpMethod.POST, "/returnPackageToArrived", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::returnPackageToArrived);
        this.addHandler(HttpMethod.POST, "/logs", AuthMiddleware.getInstance(), this::getLogs);
        this.addHandler(HttpMethod.GET, "/logDetail/:parcel_manifest_id", AuthMiddleware.getInstance(), this::getLogDetails);
        this.addHandler(HttpMethod.POST, "/finishContingency", AuthMiddleware.getInstance(), this::finishContingency);
        this.addHandler(HttpMethod.POST, "/deliveryAttempt", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::deliveryAttempt);
        this.addHandler(HttpMethod.POST, "/routeLog", AuthMiddleware.getInstance(), this::routeLog);
        this.addHandler(HttpMethod.GET, "/routeLog/:parcel_manifest_id", AuthMiddleware.getInstance(), this::getRouteLog);
        this.addHandler(HttpMethod.GET, "/routeLogDetail/:parcel_manifest_id", AuthMiddleware.getInstance(), this::getRouteDetailLog);
        super.start(startFuture);
    }

    public void register(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            JsonObject employee = context.get(EMPLOYEE);
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));

            Integer cashoutId = context.<Integer>get(CASHOUT_ID);
            if (Objects.nonNull(cashoutId)) {
                throw new Exception("The employee has an open cashout");
            }

            //data
            isGraterAndNotNull(body,_TYPE_SERVICE_ID, 0);
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);

            //cashout
            JsonObject cashOut = body.getJsonObject(_CASH_OUT);
            cashOut.put(_EMPLOYEE_ID, employee.getInteger(ID))
                    .put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID))
                    .put(TOKEN, context.<String>get(TOKEN))
                    .put(IP, context.request().remoteAddress().host());
            isGraterAndNotNull(cashOut, CASH_REGISTER_ID,0, _CASH_OUT);
            isGraterAndNotNull(cashOut, _VEHICLE_ID,0, _CASH_OUT);
            isGraterEqualAndNotNull(cashOut, _INITIAL_FUND,0.0, _CASH_OUT);

            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_REGISTER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void getOpenList(RoutingContext context) {
        try {
            JsonObject employee = context.get(EMPLOYEE);

            JsonObject body = new JsonObject()
                    .put(EMPLOYEE_ID, employee.getInteger(ID))
                    .put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put("payment_methods", context.<JsonArray>get("payment_methods"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_OPEN_LIST);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    public void checkEad(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);
            isEmpty(body, _PARCEL_TRACKING_CODE);
            isEmpty(body, _PACKAGE_CODE);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_EMPLOYEE_ID, employee.getInteger(ID));
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_CHECK_EAD);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Checked");
                    }
                } catch (ParcelManifestException ex){
                    responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void close(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_EMPLOYEE_ID, employee.getInteger(ID));
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_CLOSE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Closed");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void initRouteEad(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_EMPLOYEE_ID, employee.getInteger(ID));
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_INIT_ROUTE_EAD);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Inited");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void finish(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);
            isGrater(body, _DELIVERY_ATTEMPT_REASON_ID, 0);
            isEmpty(body, _OTHER_REASONS_NOT_RAD_EAD);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_EMPLOYEE_ID, employee.getInteger(ID));
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_FINISH);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Finished");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void report(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isGrater(body, _TYPE_SERVICE_ID, 0);
            isGrater(body, _CITY_ID, 0);
            isGrater(body, _BRANCHOFFICE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    public void reportDetails(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_REPORT_DETAILS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    public void checkEadContingency(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);
            isEmptyAndNotNull(body, _PACKAGE_CODE);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_EMPLOYEE_ID, employee.getInteger(ID));
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _EMPLOYEE_ID, 0);
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_CHECK_EAD_CONTINGENCY);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Checked");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void getDetail(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            Integer id = Integer.parseInt(request.getParam(ID));
            JsonObject body = new JsonObject()
                    .put(ID, id)
                    .put("payment_methods", context.<JsonArray>get("payment_methods"));;
            isGraterAndNotNull(body, ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    public void deleteParcelManifestDetail(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_ID, 0);
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_DELETE_PARCEL_MANIFEST_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Deleted");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void returnPackageToArrived(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_DETAIL_ID, 0);
            isGraterAndNotNull(body, _DELIVERY_ATTEMPT_REASON_ID, 0);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_RETURN_PACKAGE_TO_ARRIVED);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Updated");
                    }
                } catch (Throwable t){
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    public void getLogs(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isGrater(body, _BRANCHOFFICE_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_LOGS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    public void getLogDetails(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            Integer parcelManifestId = Integer.parseInt(request.getParam(_PARCEL_MANIFEST_ID));
            JsonObject body = new JsonObject().put(_PARCEL_MANIFEST_ID, parcelManifestId);
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_LOG_DETAILS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    public void finishContingency(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);


            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_FINISH_CONTINGENCY);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Finished");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    public void deliveryAttempt(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_DETAIL_ID, 0);
            isGraterAndNotNull(body, _DELIVERY_ATTEMPT_REASON_ID, 0);
            isEmpty(body, _IMAGE_NAME);

            JsonObject employee = context.get(EMPLOYEE);
            body.put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            isGraterAndNotNull(body, _BRANCHOFFICE_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_DELIVERY_ATTEMPT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    public void routeLog(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);
            isGrater(body, _PARCEL_MANIFEST_DETAIL_ID, 0);
            isGrater(body, _SPEED, 0.0);
            isNotNull(body, _LONGITUDE);
            isNotNull(body, _LATITUDE);
            isContained(body, _TYPE,
                    PARCEL_MANIFEST_ROUTE_LOG_TYPE.INIT.getValue(),
                    PARCEL_MANIFEST_ROUTE_LOG_TYPE.MARK.getValue(),
                    PARCEL_MANIFEST_ROUTE_LOG_TYPE.DELIVERY.getValue(),
                    PARCEL_MANIFEST_ROUTE_LOG_TYPE.DELIVERY_ATTEMPT.getValue(),
                    PARCEL_MANIFEST_ROUTE_LOG_TYPE.END.getValue());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACITON_ROUTE_LOG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    public void getRouteLog(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            Integer parcelManifestId = Integer.parseInt(request.getParam(_PARCEL_MANIFEST_ID));
            JsonObject body = new JsonObject()
                    .put(_PARCEL_MANIFEST_ID, parcelManifestId);
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_ROUTE_LOG);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    public void getRouteDetailLog(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            Integer parcelManifestId = Integer.parseInt(request.getParam(_PARCEL_MANIFEST_ID));
            JsonObject body = new JsonObject()
                    .put(_PARCEL_MANIFEST_ID, parcelManifestId);
            isGraterAndNotNull(body, _PARCEL_MANIFEST_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_ROUTE_LOG_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Founded");
                    }
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

}
