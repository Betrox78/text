package service.ead_rad;

import database.commons.ErrorCodes;
import database.ead_rad.EadRadDBV;
import database.ead_rad.VehicleRadEadDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.InternalCustomerMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

import java.util.Date;

import static database.parcel.ParcelDBV.REGISTER_POSTAL_CODE;
import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsDate.sdfDataBase;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseError;
import static database.ead_rad.VehicleRadEadDBV.*;

public class VehicleEadRadSV  extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return VehicleRadEadDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/vehicleEadRad";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.addHandler(HttpMethod.GET, "/get_vehicle", AuthMiddleware.getInstance(),  this::getVehicleeRadEad);
        this.addHandler(HttpMethod.POST, "/insertVehicleTerminal", AuthMiddleware.getInstance(), InternalCustomerMiddleware.getInstance(vertx), this::insertVehicleTerminal);
        this.addHandler(HttpMethod.PUT, "/updateVehicleTerminal", AuthMiddleware.getInstance(), this::updateStatusVehicle);
        this.addHandler(HttpMethod.PUT, "/updateDataVehicle", AuthMiddleware.getInstance(), this::updateDataVehicle);

        super.start(startFuture);
    }


    private void getVehicleeRadEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(VehicleRadEadDBV.class.getSimpleName(), body, options(VehicleRadEadDBV.ACTION_GET_SCHEDULES_RAD_EAD), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "get vehicle EAD_RAD");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void insertVehicleTerminal(RoutingContext context){
        try{
            EventBus  eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_REGISTER_VEHICLE_TERMINAL);
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
                        JsonObject result = (JsonObject) reply.result().body();
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

    private void updateStatusVehicle(RoutingContext context) {

        try{
            EventBus  eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_UPDATE_VEHICLE_TERMINAL_STATUS);

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

    private void updateDataVehicle(RoutingContext context) {

        try{
            EventBus  eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            body.put(UPDATED_BY, context.<Integer>get(USER_ID));
            body.put(UPDATED_AT, sdfDataBase(new Date()));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_UPDATE_DATA_VEHICLE);

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
