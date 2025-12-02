/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.vechicles;

import database.commons.ErrorCodes;
import database.vechicle.VehicleDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import static service.commons.Constants.*;
import service.commons.ServiceVerticle;
import service.commons.middlewares.EmployeeMiddleware;
import utils.UtilsJWT;
import utils.UtilsResponse;
import static utils.UtilsResponse.*;
import utils.UtilsValidation;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;
import service.commons.middlewares.AuthMiddleware;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class VehicleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return VehicleDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/vehicles";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/action/changeConfig/:id", AuthMiddleware.getInstance(), this::changeConfig);
        this.addHandler(HttpMethod.GET, "/report/availableVehicles/:travelDate/:arrivalDate", AuthMiddleware.getInstance(), this::availableVehicles);
        this.addHandler(HttpMethod.GET, "/report/availableBuses", AuthMiddleware.getInstance(), this::availableBuses);
        this.addHandler(HttpMethod.GET, "/report/availableTractorTrucks", AuthMiddleware.getInstance(), this::availableTractorTrucks);
        this.addHandler(HttpMethod.GET, "/report/availableRentalVehicles/:travelDate/:arrivalDate", AuthMiddleware.getInstance(), this::availableRentalVehicles);
        this.addHandler(HttpMethod.GET, "/report/availableRentalVehiclesUpdate/:travelDate/:arrivalDate/:vehicleId/:rentId", AuthMiddleware.getInstance(), this::availableRentalVehiclesUpdate);
        this.addHandler(HttpMethod.POST, "/action/setCharacteristics", AuthMiddleware.getInstance(), this::setCharacteristics);
        this.addHandler(HttpMethod.DELETE, "/action/disableVehicle/:id", AuthMiddleware.getInstance(), this::disableVehicle);
        this.addHandler(HttpMethod.GET,"/report/getRemolques", AuthMiddleware.getInstance(), this::getRemolques);
        this.addHandler(HttpMethod.GET,"/report/getVehicleTrailer", AuthMiddleware.getInstance(), this::getVehicleTrailer);
        this.addHandler(HttpMethod.GET,"/getRemolquesById/:id", AuthMiddleware.getInstance(), this::getRemolquesById);
        this.addHandler(HttpMethod.POST,"/trailer",AuthMiddleware.getInstance(), this::insertTrailer);
        this.addHandler(HttpMethod.GET, "/listTipoPermisos", AuthMiddleware.getInstance(), this::listTipoPermisos);
        this.addHandler(HttpMethod.DELETE, "/deleteTrailer/:id/:status", AuthMiddleware.getInstance(), this::deleteTrailer);
        this.addHandler(HttpMethod.GET, "/ConfigAutotransporte/list", AuthMiddleware.getInstance(), this::configAutotransporteList);
        this.addHandler(HttpMethod.GET, "/getRadEadVehicles", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::getRadEadVehicles);
        super.start(startFuture);

    }


    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isDate(body, "circulation_card_date");
            return super.isValidUpdateData(context);
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }

    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            UtilsValidation.isDate(body, "circulation_card_date");
            return super.isValidCreateData(context);
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void availableVehicles(RoutingContext context) {
        HttpServerRequest request = context.request();
        JsonObject message = new JsonObject()
                .put("travelDate", request.getParam("travelDate"))
                .put("arrivalDate", request.getParam("arrivalDate"));

        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, VehicleDBV.ACTION_AVAILABLE_VEHICLES);

        vertx.eventBus().send(this.getDBAddress(), message, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Found");
                }
            } catch (Throwable t){
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    private void availableBuses(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put("work_type", "1");

        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, VehicleDBV.ACTION_AVAILABLE_VEHICLES_V2);

        vertx.eventBus().send(this.getDBAddress(), message, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Found");
                }
            } catch (Throwable t){
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }

    private void availableTractorTrucks(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, VehicleDBV.ACTION_AVAILABLE_TRACTORS);

        vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Found");
                }
            } catch (Throwable t){
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", t);
            }
        });
    }
    
    private void availableRentalVehicles(RoutingContext context){
        JsonObject message = new JsonObject()
                .put("travelDate", context.request().getParam("travelDate"))
                .put("arrivalDate", context.request().getParam("arrivalDate"));

        vertx.eventBus().send(VehicleDBV.class.getSimpleName(), message,
                options(VehicleDBV.ACTION_AVAILABLE_RENTAL_VEHICLES),
                reply -> {
                    this.genericResponse(context, reply);
                });
    }

    private void availableRentalVehiclesUpdate(RoutingContext context){
        JsonObject message = new JsonObject()
                .put("travelDate", context.request().getParam("travelDate"))
                .put("arrivalDate", context.request().getParam("arrivalDate"))
                .put("vehicleId", context.request().getParam("vehicleId"))
                .put("rentId" , context.request().getParam("rentId"));

        vertx.eventBus().send(VehicleDBV.class.getSimpleName(), message,
                options(VehicleDBV.ACTION_AVAILABLE_RENTAL_VEHICLES_UPDATE),
                reply -> {
                    this.genericResponse(context, reply);
                });
    }

    private void setCharacteristics(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        //validate body
        try {
            isGrater(body, "vehicle_id", 0);
            isEmptyAndNotNull(body.getJsonArray("characteristic_ids"), "characteristic_ids");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, VehicleDBV.ACTION_SET_CHARACTERISTICS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t){
                    responseError(context, t);
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        }
    }

    private void insertTrailer(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        vertx.eventBus().send(this.getDBAddress(), body, options(VehicleDBV.ACTION_REGISTER_TRAILER), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Created");
                }
            } catch (Throwable t) {
                t.printStackTrace();
                responseError(context, t);
            }
        });
    }

    private void listTipoPermisos(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        vertx.eventBus().send(this.getDBAddress(), body,options(VehicleDBV.ACTION_GET_PERMISO_SAT), reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                   responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
               } else {
                   responseOk(context, reply.result().body(), "Created");
               }
           }catch (Throwable t) {
               t.printStackTrace();
               responseError(context, t);
           }
        });
    }

    private void disableVehicle(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put("vehicle_id", Integer.valueOf(context.request().getParam("id")));

        DeliveryOptions options = options(VehicleDBV.ACTION_DISABLE_VEHICLE);
        vertx.eventBus().send(VehicleDBV.class.getSimpleName(), message, options, (reply) -> this.genericResponse(context, reply));
    }

    private void getRemolquesById(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put("vehicle_id", Integer.valueOf(context.request().getParam("id")));
        DeliveryOptions options = options(VehicleDBV.ACTION_GET_TRAILERS_BY_ID);
        vertx.eventBus().send(this.getDBAddress(), message, options, (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Success");
                }
            } catch (Throwable t) {
                responseError(context,"Ocurrio un error inesperado", t);
            }
        });
    }

    private void changeConfig(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(UPDATED_BY, context.<Integer>get(USER_ID));
        body.put("vehicle_id", Integer.valueOf(context.request().getParam("id")));

        vertx.eventBus().send(this.getDBAddress(), body, options(VehicleDBV.ACTION_CHANGE_CONFIG), (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Modified");
                }
            } catch (Throwable t){
                responseError(context, "Ocurrió un error inesperado", t);
            }
        });

    }

    private void getRemolques(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        vertx.eventBus().send(this.getDBAddress(), body, options(VehicleDBV.ACTION_GET_TRAILERS), (AsyncResult<Message<JsonObject>> reply) -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                   responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
               } else {
                   responseOk(context, reply.result().body(), "Modified");
               }
           } catch (Throwable t) {
               responseError(context,"Ocurrio un error inesperado", t);
           }
        });
    }

    private void getVehicleTrailer(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        vertx.eventBus().send(this.getDBAddress(), body, options(VehicleDBV.ACTION_GET_TRAILERS_VEHICLE), (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Modified");
                }
            } catch (Throwable t) {
                responseError(context,"Ocurrio un error inesperado", t);
            }
        });
    }

    private void deleteTrailer(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put("id", Integer.valueOf(context.request().getParam("id")))
                .put("status", Integer.valueOf(context.request().getParam("status")));
        DeliveryOptions options = options(VehicleDBV.ACTION_DISABLE_TRAILER);
        //vertx.eventBus().send(VehicleDBV.class.getSimpleName(), message, options, (reply) -> this.genericResponse(context, reply));
        vertx.eventBus().send(VehicleDBV.class.getSimpleName(), message, options, reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(),"Deleted");
                }
            } else {
                responseError(context, "No se pudo cambiar el estatus al remolque", reply.cause().getMessage());
            }
        });

    }

    private void configAutotransporteList(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        vertx.eventBus().send(this.getDBAddress(), body, options(VehicleDBV.ACTION_GET_CONFIG_AUTOTRANSPORTE_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Found");
                }
            } catch (Throwable t) {
                responseError(context,"Ocurrio un error inesperado", t);
            }
        });
    }

    private void getRadEadVehicles(RoutingContext context) {
        try {
            JsonObject employee = context.get(EMPLOYEE);
            JsonObject body = new JsonObject()
                    .put(_BRANCHOFFICE_ID, employee.getInteger(_BRANCHOFFICE_ID));
            vertx.eventBus().send(this.getDBAddress(), body, options(VehicleDBV.ACTION_GET_RAD_EAD_VEHICLES), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
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
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }


}
