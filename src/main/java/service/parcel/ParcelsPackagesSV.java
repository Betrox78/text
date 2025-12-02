package service.parcel;

import database.branchoffices.BranchofficeDBV;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.commons.Action.CREATE;
import database.commons.ErrorCodes;
import database.parcel.ParcelDBV;
import database.parcel.ParcelsPackagesDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.concurrent.CompletableFuture;

import static database.parcel.ParcelsPackagesDBV.CODE_DETAIL;
import static database.shipments.ShipmentsDBV.TERMINAL_ORIGIN_ID;
import static service.commons.Constants.*;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsJWT;
import utils.UtilsResponse;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

public class ParcelsPackagesSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return ParcelsPackagesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcelsPackages";
    }


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/cost/:scheduleRouteDestinationId", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::calculateMultipleCost);
        this.addHandler(HttpMethod.GET, "/routes/:terminalOriginId/:terminalDestinyId/:dateTravel", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getRoutes);
        this.addHandler(HttpMethod.GET, "/getPackageInfo/:code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getCodeDetail);
        super.start(startFuture);
        this.router.post("/register").handler(this::register);
        this.router.delete("/cancel/:parcelPackageId").handler(this::cancelParcelPackage);
        this.addHandler(HttpMethod.POST, "/guiapp/cost/:scheduleRouteDestinationId", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::guiappCalculateMultipleCost);
        this.addHandler(HttpMethod.GET, "/routesGuiapp/:terminalOriginId/:terminalDestinyId/:dateTravel", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getRoutesPP);
        this.addHandler(HttpMethod.POST, "/cost", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::calculateMultipleCostV2);
    }

    private void register(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");

        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CREATE.name());
            int userId = UtilsJWT.getUserIdFrom(jwt);
            body.put(CREATED_BY, userId);

            try{

                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(),"fond");
                        }

                    }catch(Exception e){
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                    }

                });

            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } else {

            responseInvalidToken(context);
        }
    }

    private void calculateMultipleCost(RoutingContext context) {
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                responseError(context, new Throwable("Missing body"));
            } else {
                JsonArray packages = body.getJsonArray("packages");
                if (packages == null) {
                    responseWarning(context, new Throwable("Missing packages list"));
                } else {
                    Integer scheduleRouteDestinationId = Integer.valueOf(context.request().getParam("scheduleRouteDestinationId"));
                    doCalculateMultipleCost(packages, scheduleRouteDestinationId,false)
                            .whenComplete((s, t) -> {
                                if (t != null) {
                                    responseError(context, t.getCause().getMessage());
                                } else {
                                    responseOk(context, body);
                                }
                            });
                }

            }

    }

    private void calculateMultipleCostV2(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (body == null) {
            responseError(context, new Throwable("Missing body"));
        } else {
            JsonArray packages = body.getJsonArray("packages");
            if (packages == null) {
                responseWarning(context, new Throwable("Missing packages list"));
            } else {
                doCalculateMultipleCostV2(packages)
                        .whenComplete((s, t) -> {
                            if (t != null) {
                                responseError(context, t.getCause().getMessage());
                            } else {
                                responseOk(context, body);
                            }
                        });
            }

        }

    }

    private void guiappCalculateMultipleCost(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (body == null) {
            responseError(context, new Throwable("Missing body"));
        } else {
            JsonArray packages = body.getJsonArray("packages");
            if (packages == null) {
                responseWarning(context, new Throwable("Missing packages list"));
            } else {
                Integer scheduleRouteDestinationId = Integer.valueOf(context.request().getParam("scheduleRouteDestinationId"));
                doCalculateMultipleCost(packages, scheduleRouteDestinationId,true)
                        .whenComplete((s, t) -> {
                            if (t != null) {
                                responseError(context, t.getCause().getMessage());
                            } else {
                                responseOk(context, body);
                            }
                        });
            }

        }

    }

    private CompletableFuture<JsonArray> doCalculateMultipleCost(JsonArray packages, Integer scheduleRouteDestinationId,boolean guiapp) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(packages.stream()
                .map(p -> calculateCost((JsonObject) p, scheduleRouteDestinationId,guiapp ))
                .toArray(CompletableFuture[]::new))
                .whenComplete((s, t) -> {
                    if (t != null) { future.completeExceptionally(t);
                    } else {
                        future.complete(packages);
                    }
                });

        return future;

    }

    private CompletableFuture<JsonArray> doCalculateMultipleCostV2(JsonArray packages) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(packages.stream()
                .map(p -> calculateCostV2((JsonObject) p))
                .toArray(CompletableFuture[]::new))
                .whenComplete((s, t) -> {
                    if (t != null) { future.completeExceptionally(t);
                    } else {
                        future.complete(packages);
                    }
                });

        return future;

    }

    private CompletableFuture<JsonObject> calculateCost(JsonObject body, Integer scheduleRouteDestinationId, boolean guiapp) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject params = new JsonObject();
        String shippingType = body.getString("shipping_type");
        linearVolume(shippingType, body).whenComplete((linear,error)->{
            if(error != null){
                future.completeExceptionally(error);
            }else{
                if(shippingType.equals("pets")){
                    body.put("width", linear.getDouble("width"));
                    body.put("height", linear.getDouble("height"));
                    body.put("length", linear.getDouble("length"));
                }
                params.put("linear_volume", linear.getDouble("linear_volume"));
                params.put("weight", body.getDouble("weight"));
                params.put("schedule_route_destination_id", scheduleRouteDestinationId);
                //params.put("parcel_allowed_id", body.getInteger("parcel_allowed_id"));
                params.put("insurance_value", body.getDouble("insurance_value"));
                params.put("packing_id", body.getInteger("packing_id"));
                params.put("shipping_type", shippingType);
                if(guiapp) {
                    params.put("guiapp", true);
                    params.put("guiapp_excess", body.getDouble("guiapp_excess"));
                }


                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.CALCULATE_COST);
                vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), params, options, (AsyncResult<Message<JsonObject>> reply) -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }

                        body.mergeIn(reply.result().body());
                        future.complete(body);
                    }catch(Exception e){
                        future.completeExceptionally(reply.cause());

                    }

                });
            }
        });    
        

        return future;
    }

    private CompletableFuture<JsonObject> calculateCostV2(JsonObject body) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String shippingType = body.getString("shipping_type");
            if(shippingType.equals("parcel")) {
                Double width = body.getDouble(_WIDTH);
                Double height = body.getDouble(_HEIGHT);
                Double length = body.getDouble(_LENGTH);
                body.put(_LINEAR_VOLUME, Float.valueOf(String.format("%.3f", (width * height * length) / 1000000)));
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.CALCULATE_COST_V2);
            vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), body, options, (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }

                    body.mergeIn(reply.result().body());
                    future.complete(body);
                }catch(Exception e){
                    future.completeExceptionally(reply.cause());

                }

            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }
    
    private CompletableFuture<JsonObject> linearVolume(String ShippingType, JsonObject body){
        JsonObject result = new JsonObject();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();    
        if (ShippingType.equals("parcel") || ShippingType.equals("frozen")) {
            Double width = body.getDouble("width");
            Double height = body.getDouble("height");
            Double length = body.getDouble("length");
            //result.put("linear_volume", width + height + length);
            //Float ss= Float.parseFloat(String.format("%.3f", (width * height * length) / 1000000));
            //result.put("linear_volume", Float.parseFloat(String.format("%.3f", (width * height * length) / 1000000)));
            result.put("linear_volume", Float.valueOf(String.format("%.3f", (width * height * length) / 1000000)));
            future.complete(result);
        } else if(ShippingType.equals("pets")){
            Integer s = body.getInteger("pets_sizes_id");
            if(s != null && s>0){
                JsonObject bod = new JsonObject().put("id",s);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_PETSSIZES);
                vertx.eventBus().send(ParcelDBV.class.getSimpleName(), bod, options, handler->{
                    try{
                        if(handler.failed()){
                            throw new Exception(handler.cause());
                        }
                        JsonArray res = (JsonArray) handler.result().body();
                        JsonObject sizes = res.getJsonObject(0);
                        if(sizes != null && sizes.containsKey("height") && sizes.containsKey("width") && sizes.containsKey("length")){
                            Double width = sizes.getDouble("width");
                            Double height = sizes.getDouble("height");
                            Double length = sizes.getDouble("length");
                            result.put("width", width);
                            result.put("height", height);
                            result.put("length", length);
                            result.put("linear_volume",width + height + length);
                            future.complete(result);
                        }else{
                            future.completeExceptionally(new Throwable("No se encontro el pets_sizes_id"));
                        }

                    }catch (Exception ex){
                        ex.printStackTrace();
                        future.completeExceptionally(handler.cause());
                    }
                });
            }else{
                future.completeExceptionally(new Throwable("En el caso shipping_type='pets' el campo pets_sizes_id es requerido"));
            }
        } else {
            result.put("linear_volume", 0.0);
            future.complete(result);
        }
        return future;
    }

    private void getRoutes(RoutingContext context) {
        try {
            JsonObject params = new JsonObject();
            EventBus eventBus = vertx.eventBus();
            JsonObject branchOrigin = new JsonObject().put("id", Integer.parseInt(context.request().getParam("terminalOriginId")));
            JsonObject branchDestiny = new JsonObject().put("id", Integer.parseInt(context.request().getParam("terminalDestinyId")));

            params.put("terminalOriginId", branchOrigin.getInteger("id"));
            params.put("terminalDestinyId", branchDestiny.getInteger("id"));
            params.put("dateTravel", context.request().getParam("dateTravel"));

            params.put("allowed_frozen", context.request().getParam("allowed_frozen") != null ? (context.request().getParam("allowed_frozen").equals("true") ? true : false) : false);
            params.put("allowed_pets", context.request().getParam("allowed_pets") != null ? (context.request().getParam("allowed_pets").equals("true") ? true : false) : false);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();

            eventBus.send(BranchofficeDBV.class.getSimpleName(), branchOrigin, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f1.completer());
            eventBus.send(BranchofficeDBV.class.getSimpleName(), branchDestiny, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(detailReply -> {
                if (detailReply.succeeded()) {
                    Message<JsonObject> branch1Status = detailReply.result().resultAt(0);
                    Message<JsonObject> branch2Status = detailReply.result().resultAt(1);

                    try {
                        differentValues(new JsonObject()
                                .put("terminal_origin_id", branchOrigin.getInteger("id"))
                                .put("terminal_destiny_id", branchDestiny.getInteger("id")),
                                "terminal_origin_id", "terminal_destiny_id");
                        isStatusActive(branch1Status.body(), "status");
                        isStatusActive(branch2Status.body(), "status");

                        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.SCHEDULE_ROUTE_DESTINATION_BY_DATE);
                        vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), params, options, reply -> {
                            try{
                                if(reply.failed()){
                                    throw  new Exception(reply.cause());
                                }

                                responseOk(context, reply.result().body());

                            }catch(Exception e){
                                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                            }

                        });

                    } catch (PropertyValueException ex) {
                        UtilsResponse.responsePropertyValue(context, ex);
                    }
                }
            });
        } catch (Exception e) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private  void cancelParcelPackage(RoutingContext context){
        String jwt = context.request().getHeader("Authorization");

        if (UtilsJWT.isTokenValid(jwt)) {
            int userId = UtilsJWT.getUserIdFrom(jwt);
            JsonObject trackingCode = new JsonObject().put("parcelPackageId",context.request().getParam("parcelPackageId"));
            trackingCode.put("CANCEL_BY", userId);
            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.CANCEL_PARCEL_PACKAGE_BY_ID);
                vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), trackingCode, options, reply -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());

                    }catch(Exception e){
                        responseError(context, "Element not found", reply.cause().getMessage());

                    }

                });

            }catch (Exception ex) {
                responseError(context, "Element not found", ex);
            }

        } else {
                responseInvalidToken(context);
            }
    }

    private void getRoutesPP(RoutingContext context) {
        try {
            JsonObject params = new JsonObject();
            EventBus eventBus = vertx.eventBus();
            JsonObject branchOrigin = new JsonObject().put("id", Integer.parseInt(context.request().getParam("terminalOriginId")));
            JsonObject branchDestiny = new JsonObject().put("id", Integer.parseInt(context.request().getParam("terminalDestinyId")));

            params.put("terminalOriginId", branchOrigin.getInteger("id"));
            params.put("terminalDestinyId", branchDestiny.getInteger("id"));
            params.put("dateTravel", context.request().getParam("dateTravel"));

            params.put("allowed_frozen", context.request().getParam("allowed_frozen") != null ? (context.request().getParam("allowed_frozen").equals("true") ? true : false) : false);
            params.put("allowed_pets", context.request().getParam("allowed_pets") != null ? (context.request().getParam("allowed_pets").equals("true") ? true : false) : false);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();

            eventBus.send(BranchofficeDBV.class.getSimpleName(), branchOrigin, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f1.completer());
            eventBus.send(BranchofficeDBV.class.getSimpleName(), branchDestiny, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(detailReply -> {
                if (detailReply.succeeded()) {
                    Message<JsonObject> branch1Status = detailReply.result().resultAt(0);
                    Message<JsonObject> branch2Status = detailReply.result().resultAt(1);

                    try {
                        differentValues(new JsonObject()
                                        .put("terminal_origin_id", branchOrigin.getInteger("id"))
                                        .put("terminal_destiny_id", branchDestiny.getInteger("id")),
                                "terminal_origin_id", "terminal_destiny_id");
                        isStatusActive(branch1Status.body(), "status");
                        isStatusActive(branch2Status.body(), "status");

                        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.SCHEDULE_ROUTE_GUIAPP_DESTINATION_BY_DATE);
                        vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), params, options, reply -> {
                            try{
                                if(reply.failed()){
                                    throw  new Exception(reply.cause());
                                }

                                responseOk(context, reply.result().body());

                            }catch(Exception e){
                                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                            }

                        });

                    } catch (PropertyValueException ex) {
                        UtilsResponse.responsePropertyValue(context, ex);
                    }
                }
            });
        } catch (Exception e) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    public void getCodeDetail(RoutingContext context){
        JsonObject body = new JsonObject()
                .put("code", context.request().getParam("code"));
        vertx.eventBus().send(getDBAddress(), body,
                options(CODE_DETAIL),
                reply -> {
                    try{
                        if(reply.failed()) {
                            throw reply.cause();
                        }
                        genericResponse(context, reply,"Found");
                    }catch (Throwable t) {
                        t.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });
    }

}
