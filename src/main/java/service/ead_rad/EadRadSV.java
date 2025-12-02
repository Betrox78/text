package service.ead_rad;

import database.commons.ErrorCodes;
import database.ead_rad.EadRadDBV;
import database.parcel.ParcelDBV;
import database.site.SiteDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.InternalCustomerMiddleware;
import service.commons.middlewares.PermissionMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsResponse;
import utils.UtilsSecurity;
import utils.UtilsValidation;

import java.util.Date;

import static database.ead_rad.VehicleRadEadDBV.ACTION_UPDATE_DATA_VEHICLE;
import static database.parcel.ParcelDBV.REGISTER_POSTAL_CODE;
import static database.ead_rad.EadRadDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsDate.sdfDataBase;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import static utils.UtilsValidation.isPhoneNumber;

public class EadRadSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EadRadDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/ead_rad";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/attemptReason", PublicRouteMiddleware.getInstance(), this::getAttemptReason);
        this.addHandler(HttpMethod.POST, "/insertAttemptReason", AuthMiddleware.getInstance(), InternalCustomerMiddleware.getInstance(vertx), this::insertAttemptReason);
        this.addHandler(HttpMethod.PUT, "/updateStatus", AuthMiddleware.getInstance(), this::updateStatus);
        this.addHandler(HttpMethod.POST, "/insert_schedules_rad_ead", AuthMiddleware.getInstance(), InternalCustomerMiddleware.getInstance(vertx), this::insertScheleRadEad);
        this.addHandler(HttpMethod.GET, "/get_schedules_rad_ead",  AuthMiddleware.getInstance(),  this::getScheleRadEad);
        this.addHandler(HttpMethod.PUT, "/update_schedule_change_status", AuthMiddleware.getInstance(), this::updateStatusChangeSchedule);
        this.addHandler(HttpMethod.POST, "/get_parcels_manifest_rad_ead",  AuthMiddleware.getInstance(),  this::getParcelsManifestRadEad);
        this.addHandler(HttpMethod.GET, "/get_service_rad_ead",  AuthMiddleware.getInstance(),  this::getServiceTypeRadEad);
        this.addHandler(HttpMethod.GET, "/get_service_rad_ead_pos",  AuthMiddleware.getInstance(),  this::getServiceTypeRadEadPos);
        this.addHandler(HttpMethod.GET, "/get_service_rad_ead_site",  PublicRouteMiddleware.getInstance(),  this::getServiceTypeRadEadPos);
        this.addHandler(HttpMethod.POST, "/insertManifest_rad_ead",  AuthMiddleware.getInstance(),  this::insertManifest);
        this.addHandler(HttpMethod.PUT, "/updateDataService", AuthMiddleware.getInstance(), this::updateDataService);
        this.addHandler(HttpMethod.POST, "/getManifest",  AuthMiddleware.getInstance(),  this::getManifest);
        this.addHandler(HttpMethod.POST, "/getManifestAllPackages",  AuthMiddleware.getInstance(),  this::getManifestAllPackages);
        this.addHandler(HttpMethod.POST, "/updateManifestConfirmedDelivery",  AuthMiddleware.getInstance(),  this::updateManifestConfirmedDelivery);
        this.addHandler(HttpMethod.PUT, "/updateManifestStatus", AuthMiddleware.getInstance(), this::updateManifestStatus);
        this.addHandler(HttpMethod.POST, "/getManifestCancel",  AuthMiddleware.getInstance(),  this::getManifestCancel);
        this.addHandler(HttpMethod.POST, "/cancelManifest",  AuthMiddleware.getInstance(),  this::cancelManifest);
        this.addHandler(HttpMethod.POST, "/checkStatusFXC",  AuthMiddleware.getInstance(),  this::checkStatusFXC);
        this.addHandler(HttpMethod.POST, "/getPackagesFXC",  AuthMiddleware.getInstance(),  this::getPackagesFXC);
        this.addHandler(HttpMethod.GET, "/getCustomersEadRad" , AuthMiddleware.getInstance(), this::getCustomersEadRad);
        this.addHandler(HttpMethod.POST, "/register_customer_RadEad" , AuthMiddleware.getInstance(), this::registerCustomerRadEad);
        this.addHandler(HttpMethod.PUT, "/update_customer_RadEad", AuthMiddleware.getInstance(), this::updateCustomerRadEad);
        super.start(startFuture);
    }

    private void getAttemptReason(RoutingContext context) {
        // Use a service account
        JsonObject body = new JsonObject();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_ATTEMPT_REASON), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void insertAttemptReason(RoutingContext context){
        try{
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, REGISTER_ATTEMPT_REASON);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            body.put("status",1);

            eventBus.send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())){
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {

                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        }catch (Exception e){
            responseError(context, "Ocurriò un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }

    }

    private void updateStatus(RoutingContext context) {
        // Use a service account
        JsonObject body = context.getBodyAsJson();
        //body.put(UPDATED_BY, context.<Integer>get(USER_ID));
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_UPDATE_ATTEMPT_STATUS), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }


    private void insertScheleRadEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_INSERT_SCHEDULES_RAD_EAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "insert schule RAD and EAD");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getScheleRadEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_GET_SCHEDULES_RAD_EAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get schedules");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void updateStatusChangeSchedule(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_UPDATE_STATUS_SCHEDULES_RAD_EAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Change Status");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getParcelsManifestRadEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_PARCELS_MANIFEST), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get package manifest");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getServiceTypeRadEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_SERVICE_TYPE_EAD_RAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get service type ead_Rad");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void getServiceTypeRadEadPos(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_SERVICE_TYPE_EAD_RAD_POS), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get service type ead_Rad");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    //ACTION_GET_SERVICE_TYPE_EAD_RAD_POS
    private void insertManifest(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_INSERT_MANIFEST), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "update status and confirmed, manifest RAD and EAD");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getManifest(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_MANIFEST), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "insert manifest RAD and EAD");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getManifestAllPackages(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_MANIFEST_ALL_PACKAGES), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "insert manifest RAD and EAD");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void updateDataService(RoutingContext context) {

        try{
            EventBus  eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            body.put(UPDATED_AT, sdfDataBase(new Date()));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_UPDATE_DATA_SERVICE);

            eventBus.send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())){
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {

                        responseOk(context, reply.result().body(), "Updated");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        }catch (Exception e){
            responseError(context, "Ocurriò un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }
    private void updateManifestConfirmedDelivery(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_UPDATE_MANIFEST_CONFIRMED_DELIVERY), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "update status and confirmed, manifest RAD and EAD");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void updateManifestStatus(RoutingContext context) {

        try{
            EventBus  eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson().getJsonObject("objUpdate");
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            body.put(UPDATED_AT, sdfDataBase(new Date()));

            DeliveryOptions options = new DeliveryOptions();
            if(body.getBoolean("reprint")){
                options.addHeader(ACTION, ACTION_REPRINT_MANIFEST_BY_ID);
            } else {
                options.addHeader(ACTION, ACTION_UPDATE_MANIFEST_BY_ID);
            }

            eventBus.send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())){
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {

                        responseOk(context, reply.result().body(), "Updated");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        }catch (Exception e){
            responseError(context, "Ocurriò un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }
    private void getManifestCancel(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_MANIFEST_CANCEL), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get manifest");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void cancelManifest(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_CANCEL_MANIFEST), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "cancel manifest");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void checkStatusFXC(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_CHECK_STATUS_FXC), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get FXC");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getPackagesFXC(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_GET_PACKAGES_FXC), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get packagesFXC");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void getCustomersEadRad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(ACTION_GET_CUSTOMERS_EAD_RAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "OK");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void registerCustomerRadEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        this.vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, options(EadRadDBV.ACTION_REGISTER_CUSTOMER_RAD_EAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Cliente agregado");
                }
            } else {
                responseError(context, reply.cause().getMessage(), reply.cause().getMessage());
            }
        });
    }

    private void updateCustomerRadEad(RoutingContext context) {

        try{
            EventBus  eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            body.put(UPDATED_AT, sdfDataBase(new Date()));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_UPDATE_CUSTOMER_RAD_EAD);

            eventBus.send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())){
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {

                        responseOk(context, reply.result().body(), "Updated");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        }catch (Exception e){
            responseError(context, "Ocurriò un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }


}
