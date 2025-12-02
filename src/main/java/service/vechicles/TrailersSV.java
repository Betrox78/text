package service.vechicles;

import database.commons.ErrorCodes;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.LoadPackageCodes;
import database.shipments.handlers.ShipmentsDBV.RegisterShipmentParcel;
import database.vechicle.TrailersDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SHIPMENT_ID;
import static database.shipments.ShipmentsDBV.SCHEDULE_ROUTE_ID;
import static database.vechicle.TrailersDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author AllAbordo
 */
public class TrailersSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TrailersDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/trailers";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/list", AuthMiddleware.getInstance(), this::getList);
        this.addHandler(HttpMethod.GET, "/availableList", AuthMiddleware.getInstance(), this::getAvailableList);
        this.addHandler(HttpMethod.POST, "/getHitched", AuthMiddleware.getInstance(), this::getHitched);
        this.addHandler(HttpMethod.POST, "/assignToShipment", AuthMiddleware.getInstance(), this::assignToShipment);
        this.addHandler(HttpMethod.POST, "/change", AuthMiddleware.getInstance(), this::change);
        this.addHandler(HttpMethod.POST, "/release", AuthMiddleware.getInstance(), this::release);
        this.addHandler(HttpMethod.POST, "/hitch", AuthMiddleware.getInstance(), this::hitch);
        this.addHandler(HttpMethod.GET, "/getToHitch/:terminal_id", AuthMiddleware.getInstance(), this::getToHitch);
        this.addHandler(HttpMethod.POST, "/removeOfRoute", AuthMiddleware.getInstance(), this::removeOfRoute);
        super.start(startFuture);
    }

    private void getList(RoutingContext context){
        vertx.eventBus().send(this.getDBAddress(), null, options(TrailersDBV.ACTION_GET_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
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
                t.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, t);
            }
        });
    }

    private void getAvailableList(RoutingContext context){
        vertx.eventBus().send(this.getDBAddress(), null, options(TrailersDBV.ACTION_GET_AVAILABLE_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
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
                t.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, t);
            }
        });
    }

    private void getHitched(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_GET_HITCHED), (AsyncResult<Message<JsonObject>> reply) -> {
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
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body, NAME);
            isGraterAndNotNull(body, C_SUBTIPOREM_ID, -1);
            isEmptyAndNotNull(body, PLATE, "body");
            return super.isValidCreateData(context);
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, ID, 0);
            isEmpty(body, NAME);
            isGrater(body, C_SUBTIPOREM_ID, -1);
            isEmpty(body, PLATE, "body");
            isBoolean(body, IN_USE, "body");
            isBetweenRange(body, STATUS, 1, 4);
            return super.isValidUpdateData(context);
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            return UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void assignToShipment(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, TRAILER_ID, 0);
            isGraterAndNotNull(body, SHIPMENT_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_ASSIGN_TO_SHIPMENT), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Assigned");
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException e) {
                responsePropertyValue(context, e);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void change(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, SHIPMENT_ID, 0);
            isGraterAndNotNull(body, TRAILER_ID, 0);
            isGraterAndNotNull(body, TRANSFER_TRAILER_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_CHANGE), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Changed");
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException e) {
                responsePropertyValue(context, e);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void release(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, SHIPMENT_ID, 0);
            isGraterAndNotNull(body, TRAILER_ID, 0);
            isGraterAndNotNull(body, TERMINAL_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_RELEASE), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Released");
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void hitch(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, SHIPMENT_ID, 0);
            isGraterAndNotNull(body, TRAILER_ID, 0);
            isGraterAndNotNull(body, TERMINAL_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_HITCH), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        return;
                    }
                    hitchPackages(context, reply.result().body());
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void hitchPackages(RoutingContext context, JsonObject body) {
        vertx.eventBus().send(ShipmentsDBV.class.getSimpleName(), body, new DeliveryOptions().addHeader(ACTION, LoadPackageCodes.ACTION), (AsyncResult<Message<JsonObject>> replyLoad) -> {
            try {
                if (replyLoad.failed()) {
                    throw replyLoad.cause();
                }
                if (replyLoad.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE + " hitchPackageCodes", replyLoad.result().body());
                    return;
                }
                JsonObject result = replyLoad.result().body();

                List<JsonObject> parcelsList = result.containsKey("parcels") ? result.getJsonArray("parcels").getList() : new ArrayList<>();
                if (parcelsList.isEmpty()) {
                    responseOk(context, new JsonObject().put("success", true), "Hitched");
                    return;
                }

                JsonObject parcelsBody = body.copy();
                JsonArray parcelTrackingCodeArray = new JsonArray(parcelsList.stream()
                        .map(r -> r.getString("parcel_tracking_code")).distinct().collect(Collectors.toList()));
                parcelsBody.put("codes", parcelTrackingCodeArray);

                vertx.eventBus().send(ShipmentsDBV.class.getSimpleName(), body, new DeliveryOptions().addHeader(ACTION, RegisterShipmentParcel.ACTION), (AsyncResult<Message<JsonObject>> replyParcelLoad) -> {
                    try {
                        if (replyParcelLoad.failed()) {
                            throw replyParcelLoad.cause();
                        }
                        if (replyParcelLoad.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE + " hitchParcelCodes", replyParcelLoad.result().body());

                            return;
                        }
                        responseOk(context, new JsonObject().put("success", true), "Hitched");
                    } catch (Throwable t) {
                        responseError(context, UNEXPECTED_ERROR, t);
                    }
                });
            } catch (Throwable t) {
                responseError(context, UNEXPECTED_ERROR, t);
            }
        });
    }

    private void getToHitch(RoutingContext context){
        HttpServerRequest request = context.request();
        try {
            JsonObject body = new JsonObject().put(TERMINAL_ID, Integer.parseInt(request.getParam(TERMINAL_ID)));
            isGraterAndNotNull(body, TERMINAL_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_GET_TO_HITCH), (AsyncResult<Message<JsonObject>> reply) -> {
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
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void removeOfRoute(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID, 0);
            isGraterAndNotNull(body, TRAILER_ID, 0);
            isGraterAndNotNull(body, TERMINAL_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(TrailersDBV.ACTION_REMOVE_OF_ROUTE), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Assigned");
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException e) {
            responsePropertyValue(context, e);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }
}
