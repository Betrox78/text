package service.shipments;


import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.LoadPackageCodes;
import database.shipments.handlers.ShipmentsDBV.RegisterShipmentParcel;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.EmployeeMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SHIPMENT_ID;
import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static database.money.ReportDBV.END_DATE;
import static database.money.ReportDBV.INIT_DATE;
import static database.shipments.ShipmentsDBV.*;
import static database.vechicle.TrailersDBV.TRAILER_ID;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author Saul
 */
public class ShipmentsSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return ShipmentsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/shipments";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::genericRegister);
        this.addHandler(HttpMethod.GET, "/scheduleRouteToShipment/:date/:time", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::getReadyForShipments);
        this.addHandler(HttpMethod.GET, "/scheduleRouteToShipment/manual/:terminal_id/:date/:time", AuthMiddleware.getInstance(), this::getReadyForShipments);
        this.addHandler(HttpMethod.GET, "/scheduleRouteToShipment/:date/:time/:branchoffice_id/:type",AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::getReadyForShipmentsByType);
        this.addHandler(HttpMethod.POST, "/load/checkCodes",AuthMiddleware.getInstance(), this::loadCodes);
        this.addHandler(HttpMethod.POST, "/load/checkTrackingCodes",AuthMiddleware.getInstance(), this::loadTrackingCodes);
        this.addHandler(HttpMethod.POST, "/load/checkPackageCodes",AuthMiddleware.getInstance(), this::loadPackageCodes);
        this.addHandler(HttpMethod.POST, "/load/checkPackageCodes/manual", AuthMiddleware.getInstance(), this::loadPackageCodesManual);
        this.addHandler(HttpMethod.POST, "/download/checkCodes",AuthMiddleware.getInstance(), this::downloadCodes);
        this.addHandler(HttpMethod.POST, "/download/checkPackageCodes",AuthMiddleware.getInstance(), this::downloadPackageCodes);
        this.addHandler(HttpMethod.POST, "/delete/checkPackageCodes",AuthMiddleware.getInstance(), this::deletePackageCodes);
        this.addHandler(HttpMethod.POST, "/transferTrailer/checkPackageCodes",AuthMiddleware.getInstance(), this::transferTrailerTrackingCode);
        this.addHandler(HttpMethod.GET,"/passengerToBoard/:schedule_route_id", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::getPassengersToBoard);
        this.addHandler(HttpMethod.GET,"/passengerToBoard/manual/:terminal_id/:schedule_route_id", AuthMiddleware.getInstance(), this::getPassengersToBoard);
        this.addHandler(HttpMethod.GET,"/passengerToDownload/manual/:terminal_id/:schedule_route_id", AuthMiddleware.getInstance(), this::getPassengersToDownload);
        this.addHandler(HttpMethod.GET,"/incidences/:id",AuthMiddleware.getInstance(), this::getIncidences);
        this.addHandler(HttpMethod.POST,"/historic/load",AuthMiddleware.getInstance(), this::getHistoricLoad);
        this.addHandler(HttpMethod.POST, "/historic/download", AuthMiddleware.getInstance(), this::getHistoricDownload);
        this.addHandler(HttpMethod.POST,"/toDoList", AuthMiddleware.getInstance(), this::getToDoListByType);
        this.addHandler(HttpMethod.POST,"/dailyLogs",AuthMiddleware.getInstance(), this::getDailyLogs);
        this.addHandler(HttpMethod.GET,"/dailyLogsDetail/:travel_logs_id",AuthMiddleware.getInstance(), this::getDailyLogsDetail);
        this.addHandler(HttpMethod.GET,"/preDailyLogsDetail/:travel_logs_id",AuthMiddleware.getInstance(), this::getPreDailyLogsDetail);
        this.addHandler(HttpMethod.POST,"/download/cancel", AuthMiddleware.getInstance(), this::cancelShipmentDownload);
        this.addHandler(HttpMethod.GET,"/info/:shipment_id",AuthMiddleware.getInstance(), this::getShipmentInfo);
        this.addHandler(HttpMethod.POST,"/getParcelsToDownload", AuthMiddleware.getInstance(), this::getParcelsToDownload);
        this.addHandler(HttpMethod.POST,"/dailyLogs/load",AuthMiddleware.getInstance(), this::getDailyLogsLoad);
        this.addHandler(HttpMethod.POST,"/dailyLogs/download",AuthMiddleware.getInstance(), this::getDailyLogsDownload);
        this.addHandler(HttpMethod.POST, "/incidence/log", AuthMiddleware.getInstance(), this::getIncidenceLog);
        this.addHandler(HttpMethod.POST, "/toDoListParcels", AuthMiddleware.getInstance(), this::getParcelsToDoList);
        this.addHandler(HttpMethod.GET, "/loadInfo/:shipment_id", AuthMiddleware.getInstance(), this::getShipmentLoadInfo);
        this.addHandler(HttpMethod.GET,"/transhipments/toLoad/:schedule_route_id", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::transhipmentsToLoad);
        this.addHandler(HttpMethod.GET,"/transhipments/toLoad/manual/:terminal_id/:schedule_route_id", AuthMiddleware.getInstance(), this::transhipmentsToLoad);
        this.addHandler(HttpMethod.POST,"/getParcelsToDownload/v2", AuthMiddleware.getInstance(), this::getParcelsToDownloadV2);
        this.addHandler(HttpMethod.POST, "/reports/dailyLogsCost", AuthMiddleware.getInstance(), this::getDailyLogsCostReport);
        this.addHandler(HttpMethod.GET,"/conciliation/load/:shipment_id", AuthMiddleware.getInstance(), this::getLoadConciliation);
        this.addHandler(HttpMethod.GET,"/conciliation/download/:shipment_id", AuthMiddleware.getInstance(), this::getDownloadConciliation);
        this.addHandler(HttpMethod.POST, "/loadedPackagesByShipment", AuthMiddleware.getInstance(), this::loadedPackagesByShipment);
        this.addHandler(HttpMethod.POST, "/searchParcelsToLoad", AuthMiddleware.getInstance(), this::searchParcelsToLoad);
        this.addHandler(HttpMethod.GET, "/loadingInfo/:shipment_id", AuthMiddleware.getInstance(), this::getLoadingInfo);
        super.start(startFuture);
    }
    private void getPassengersToBoard(RoutingContext context){
        try {
            HttpServerRequest request = context.request();
            JsonObject employee = context.get(EMPLOYEE);
            JsonObject body = new JsonObject()
                    .put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put(SCHEDULE_ROUTE_ID, Integer.valueOf(request.getParam(SCHEDULE_ROUTE_ID)));

            if(request.getParam(TERMINAL_ID) != null){
                Integer terminalId = Integer.parseInt(request.getParam(TERMINAL_ID));
                body.put(TERMINAL_ID, terminalId);
            } else {
                body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }

            execEventBus(context,ShipmentsDBV.GET_PASSENGERS_TO_BOARD, body, "Found");
        }catch (Exception e){
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getPassengersToDownload(RoutingContext context){
        try {
            HttpServerRequest request = context.request();
            JsonObject employee = context.get(EMPLOYEE);
            JsonObject body = new JsonObject()
                    .put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put(SCHEDULE_ROUTE_ID, Integer.valueOf(request.getParam(SCHEDULE_ROUTE_ID)));

            if(request.getParam(TERMINAL_ID) != null){
                Integer terminalId = Integer.parseInt(request.getParam(TERMINAL_ID));
                body.put(TERMINAL_ID, terminalId);
            } else {
                body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }

            execEventBus(context,ShipmentsDBV.GET_PASSENGERS_TO_DOWNLOAD, body, "Found");
        }catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getReadyForShipments(RoutingContext context){
        try {
            HttpServerRequest request = context.request();
            String stringDate = request.getParam(DATE) + " " + request.getParam(TIME);
            JsonObject body = new JsonObject()
                    .put(CREATED_BY,context.<Integer>get(USER_ID))
                    .put(DATE, stringDate);

            if (request.getParam(TERMINAL_ID) != null){
                Integer terminalId = Integer.parseInt(request.getParam(TERMINAL_ID));
                body.put(TERMINAL_ID, terminalId);
                body.put(INCLUDE_PARCELS, true);
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(EMPLOYEE_ID, employee.getInteger(ID))
                    .put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "routes_time_before_travel_date"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "parcel_routes_time_before_travel_date"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject field1 = reply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = reply.result().<Message<JsonObject>>resultAt(1).body();

                    Integer routesTimeBeforeTravelDate = Integer.parseInt(field1.getString("value", "6"));
                    Integer parcelRoutesTimeBeforeTravelDate = Integer.parseInt(field2.getString("value", "6"));

                    body
                            .put("routes_time_before_travel_date", routesTimeBeforeTravelDate)
                            .put("parcel_routes_time_before_travel_date", parcelRoutesTimeBeforeTravelDate);
                    execEventBus(context,ShipmentsDBV.FIND_SHIPMENTS_BY_TERMINAL, body, "Found");

                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        }catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getReadyForShipmentsByType(RoutingContext context){
        try {
            Integer branchofficeId = Integer.parseInt(context.request().getParam("branchoffice_id"));
            String type = context.request().getParam("type");
            String stringDate = context.request().getParam("date") + " " + context.request().getParam("time");
            JsonObject body = new JsonObject()
                    .put(EMPLOYEE,context.<JsonObject>get(EMPLOYEE))
                    .put(CREATED_BY,context.<Integer>get(USER_ID))
                    .put("branchoffice_id", branchofficeId)
                    .put("type", type)
                    .put(INCLUDE_PARCELS, true)
                    .put("date", stringDate);
            isEmptyAndNotNull(body,"type");
            UtilsValidation.isGraterAndNotNull(body,"branchoffice_id",0);
            execEventBus(context,ShipmentsDBV.FIND_SHIPMENTS_BY_TERMINAL_AND_TYPE, body, "Found");
        }catch (UtilsValidation.PropertyValueException e ){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void genericRegister(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            if (this.isValidCreateData(context)) {
                execEventBus(context, ShipmentsDBV.GENERIC_REGISTER, body, "Created");
            }
        }catch (Exception e){
            responseError(context,"Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void cancelShipment(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            body.put("isLoad", true);
            isGraterAndNotNull(body, "shipment_id", 0);
            execEventBus(context, ShipmentsDBV.CANCEL_SHIPMENT_LOAD_DOWNLOAD, body, "cancel shipment");

        }catch (Exception e){
            responseError(context,"Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void cancelShipmentDownload(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            body.put("isLoad", false);
            isGraterAndNotNull(body, "shipment_id", 0);
            execEventBus(context, ShipmentsDBV.CANCEL_SHIPMENT_LOAD_DOWNLOAD, body, "cancel shipment");

        }catch (Exception e){
            responseError(context,"Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void loadCodes(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            Integer userId = context.<Integer>get(USER_ID);
            String origin = body.getString("origin", "app");
            body.put(CREATED_BY, userId);
            body.put("status","loaded");

            JsonArray codes = body.getJsonArray("codes");

            if(codes==null){
                throw new PropertyValueException("codes",MISSING_REQUIRED_VALUE);
            }
            isGraterAndNotNull(body,"shipment_id",0);

            if(!this.isValidCreateData(context)){
                return;
            }

            List<String> checkCodes = body.getJsonArray("codes").stream()
                    .map(c -> (String) c)
                    .filter(c -> c.charAt(0) != 'G')
                    .collect(Collectors.toList());

            List<String> parcelCodes = body.getJsonArray("codes").stream()
                    .map(c -> (String) c)
                    .filter(c -> c.charAt(0) == 'G')
                    .collect(Collectors.toList());

            JsonObject parcelsBody = body.copy();

            body.put("codes", checkCodes);
            parcelsBody.put("codes", parcelCodes);

            execService(context, ShipmentsDBV.CHECK_CODES, body, checkCodeResult -> {
                if(parcelCodes.isEmpty() || origin.equals("web")) {
                    responseOk(context, checkCodeResult, "Check Load");
                    return;
                }
                execService(context, RegisterShipmentParcel.ACTION, parcelsBody, parcelsResult -> {
                    checkCodeResult.mergeIn(parcelsResult);
                    responseOk(context, checkCodeResult);
                });
            });

        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void loadTrackingCodes(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            Integer userId = context.<Integer>get(USER_ID);
            body.put(CREATED_BY, userId);
            JsonArray codes = body.getJsonArray("codes");

            if(codes==null){
                throw new PropertyValueException("codes",MISSING_REQUIRED_VALUE);
            }

            isGrater(body, TRAILER_ID,0);
            isGraterAndNotNull(body, SHIPMENT_ID,0);

            if(!this.isValidCreateData(context)){
                return;
            }

            List<String> parcelCodes = body.getJsonArray("codes").stream()
                    .map(c -> (String) c)
                    .filter(c -> c.charAt(0) == 'G')
                    .collect(Collectors.toList());

            JsonObject parcelsBody = body.copy();
            parcelsBody.put("codes", parcelCodes);

            execService(context, RegisterShipmentParcel.ACTION, parcelsBody, parcelsResult -> {
                responseOk(context, parcelsResult);
            });

        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void loadPackageCodes(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            Integer userId = context.<Integer>get(USER_ID);
            body.put(CREATED_BY, userId);
            body.put(STATUS,"loaded");

            JsonArray codes = body.getJsonArray("codes");

            if(codes==null){
                throw new PropertyValueException("codes",MISSING_REQUIRED_VALUE);
            }
            isGraterAndNotNull(body,SHIPMENT_ID,0);
            isBoolean(body, "only_packages");
            isGrater(body, TRAILER_ID, 0);
            boolean onlyPackages = body.getBoolean("only_packages", false);

            execService(LoadPackageCodes.ACTION, body, 3, 1, result -> {
                try {
                    if (onlyPackages) {
                        responseOk(context, result, "Check Load");
                        return;
                    }

                    List<JsonObject> parcelsList = result.containsKey("parcels") ? result.getJsonArray("parcels").getList() : new ArrayList<>();
                    if (parcelsList.isEmpty()) {
                        responseOk(context, result, "Check Load");
                        return;
                    }

                    JsonObject parcelsBody = body.copy();
                    JsonArray parcelTrackingCodeArray = new JsonArray(parcelsList.stream()
                            .map(r -> r.getString("parcel_tracking_code")).distinct().collect(Collectors.toList()));
                    parcelsBody.put("codes", parcelTrackingCodeArray);

                    vertx.eventBus().send(this.getDBAddress(), body, options(RegisterShipmentParcel.ACTION), (AsyncResult<Message<JsonObject>> replyParcel) -> {
                        try {
                            if (replyParcel.failed()) {
                                throw replyParcel.cause();
                            }
                            result.mergeIn(new JsonObject().put("parcels", parcelTrackingCodeArray));
                            responseOk(context, result, "Check Load");
                        } catch (Throwable t) {
                            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, t));
                        }
                    });
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                }
            }, t -> exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t)));

        } catch (UtilsValidation.PropertyValueException e ){
            responseError(context, e.getMessage());
        } catch (Throwable t){
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, t));
        }
    }

    private void loadPackageCodesManual(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            Integer userId = context.<Integer>get(USER_ID);
            body.put(CREATED_BY, userId);

            isGraterAndNotNull(body,SHIPMENT_ID,0);
            isGrater(body, TRAILER_ID, 0);
            JsonArray codes = body.getJsonArray(_CODES);
            isEmptyAndNotNull(codes, _CODES);

            List<String> parcelCodes = codes.stream()
                    .map(c -> (String) c)
                    .filter(c -> c.charAt(0) == 'G')
                    .collect(Collectors.toList());

            List<String> packageCodes = codes.stream()
                    .map(c -> (String) c)
                    .filter(c -> c.charAt(0) == 'P')
                    .collect(Collectors.toList());


            JsonObject parcelsBody = body.copy();
            parcelsBody.put(_CODES, parcelCodes);
            execService(RegisterShipmentParcel.ACTION, parcelsBody, 3, 1, resultParcels -> {
                try {
                    JsonObject packagesBody = body.copy();
                    packagesBody.put(_CODES, packageCodes);
                    execService(LoadPackageCodes.ACTION, packagesBody, 3, 1, resultPackages -> {
                        try {
                            resultPackages.mergeIn(new JsonObject().put("parcels", resultParcels));
                            responseOk(context, resultPackages, "Check Load");
                        } catch (Throwable t) {
                            exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                        }
                    }, t -> exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t)));

                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                }
            }, t -> exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t)));

        } catch (UtilsValidation.PropertyValueException e ){
            responseError(context, e.getMessage());
        } catch (Throwable t){
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, t));
        }
    }

    private void downloadCodes(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY,context.<Integer>get(USER_ID));
            body.put("status","downloaded");

            JsonArray codes = body.getJsonArray("codes");

            if(codes==null){
                throw new UtilsValidation.PropertyValueException("codes",MISSING_REQUIRED_VALUE);
            }
            UtilsValidation.isGraterAndNotNull(body,"shipment_id",0);

            if(this.isValidCreateData(context)){
                execEventBus(context,ShipmentsDBV.CHECK_CODES,body,"Check Load");
            }
        }catch (UtilsValidation.PropertyValueException e ){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void downloadPackageCodes(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY,context.<Integer>get(USER_ID));
            body.put(STATUS,"downloaded");

            JsonArray codes = body.getJsonArray("codes");

            if(codes==null){
                throw new UtilsValidation.PropertyValueException("codes",MISSING_REQUIRED_VALUE);
            }
            UtilsValidation.isGraterAndNotNull(body,SHIPMENT_ID,0);
            isGrater(body, TRAILER_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_DOWNLOAD_PACKAGE_CODES), (AsyncResult<Message<JsonObject>> replyDownload) -> {
                try {
                    if (replyDownload.failed()) {
                        throw replyDownload.cause();
                    }
                    if (replyDownload.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replyDownload.result().body());
                    } else {
                        JsonObject resultDownload = replyDownload.result().body();

                        Integer packagesProcessed = resultDownload.getInteger("packages");
                        if(Objects.isNull(packagesProcessed) || packagesProcessed == 0) {
                            responseOk(context, resultDownload, "Check Download");
                            return;
                        }

                        JsonObject bodyArrive = new JsonObject()
                                .put(TERMINAL_ID, (Integer) resultDownload.remove(TERMINAL_ID))
                                .put(SCHEDULE_ROUTE_ID, (Integer) resultDownload.remove(SCHEDULE_ROUTE_ID))
                                .put(CREATED_BY, context.<Integer>get(USER_ID));

                        vertx.eventBus().send(this.getDBAddress(), bodyArrive, options(ACTION_ARRIVE_PACKAGE_CODES), (AsyncResult<Message<JsonObject>> replyArrive) -> {
                            try {
                                if (replyArrive.failed()) {
                                    throw replyArrive.cause();
                                }
                                if (replyArrive.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replyArrive.result().body());
                                } else {
                                    responseOk(context, resultDownload, "Check Download");
                                }
                            } catch (Throwable t) {
                                exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                            }
                        });
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                }
            });
        }catch (UtilsValidation.PropertyValueException e){
            responseError(context, e.getMessage());
        } catch (Throwable t){
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, t));
        }
    }

    private void deletePackageCodes(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY,context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, _SHIPMENT_ID, 0);
            isGrater(body, _TRAILER_ID, 0);
            isEmpty(body, _PARCEL_TRACKING_CODE);
            JsonArray packageCodes = body.getJsonArray(_PACKAGE_CODES);
            isEmpty(packageCodes, _PACKAGE_CODES);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_DELETE_PACKAGE_CODES), (AsyncResult<Message<JsonObject>> replyArrive) -> {
                try {
                    if (replyArrive.failed()) {
                        throw replyArrive.cause();
                    }
                    if (replyArrive.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replyArrive.result().body());
                    } else {
                        responseOk(context, replyArrive.result().body(), "Check delete");
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                }
            });
        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e.getMessage());
        }  catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, t));
        }
    }

    private void transferTrailerTrackingCode(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY,context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, _SHIPMENT_ID, 0);
            isGraterAndNotNull(body, _FROM_TRAILER_ID, 0);
            isGraterAndNotNull(body, _TO_TRAILER_ID, 0);
            isEmpty(body, _PARCEL_TRACKING_CODE);
            JsonArray packageCodes = body.getJsonArray(_PACKAGE_CODES);
            isEmpty(packageCodes, _PACKAGE_CODES);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_TRANSFER_TRAILER_TRACKING_CODE), (AsyncResult<Message<JsonObject>> replyArrive) -> {
                try {
                    if (replyArrive.failed()) {
                        throw replyArrive.cause();
                    }
                    if (replyArrive.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, replyArrive.result().body());
                    } else {
                        responseOk(context, replyArrive.result().body(), "Check transfer trailer");
                    }
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                }
            });
        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e.getMessage());
        }  catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, t));
        }
    }

    private void getIncidences(RoutingContext context){
        try {
            Integer id = Integer.parseInt(context.request().getParam(ID));
            JsonObject body = new JsonObject().put(ID, id);

            UtilsValidation.isGraterAndNotNull(body, ID,0);

            execEventBus(context,ShipmentsDBV.GET_INCIDENCES, body,"Found");

        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void getToDoListByType(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isGraterAndNotNull(body, TERMINAL_ID, 0);
            isDateTimeAndNotNull(body, DATE, "to_do_list");
            isGrater(body, CONFIG_ROUTE_ID, 0);
            isGrater(body, _TERMINAL_ORIGIN_ID, 0);
            isGrater(body, _TERMINAL_DESTINY_ID, 0);
            isContainedAndNotNull(body, TYPE_SHIPMENT, SHIPMENT_TYPES.LOAD.getName(), SHIPMENT_TYPES.DOWNLOAD.getName());
            body.put(INCLUDE_PARCELS, true);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "routes_time_before_travel_date_pos"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "parcel_routes_time_before_travel_date"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }

                            JsonObject field1 = reply.result().<Message<JsonObject>>resultAt(0).body();
                            JsonObject field2 = reply.result().<Message<JsonObject>>resultAt(1).body();

                            Integer routesTimeBeforeTravelDate = Integer.parseInt(field1.getString("value", "6"));
                            Integer parcelRoutesTimeBeforeTravelDate = Integer.parseInt(field2.getString("value", "6"));

                            body
                                .put("routes_time_before_travel_date_pos", routesTimeBeforeTravelDate)
                                .put("parcel_routes_time_before_travel_date", parcelRoutesTimeBeforeTravelDate);
                            execEventBus(context,ShipmentsDBV.FIND_SHIPMENTS_TO_DO_LIST_BY_TYPE, body, "Found");

                        } catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });

        } catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }
    private void getParcelsToDoList(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isGraterAndNotNull(body, TERMINAL_ID, 0);
            isDateTimeAndNotNull(body, DATE, "to_do_list");
            isGraterAndNotNull(body, "page", 0);
            isGraterAndNotNull(body, "limit", 0);
            isGrater(body, CONFIG_ROUTE_ID, 0);
            isGrater(body, _TERMINAL_ORIGIN_ID, 0);
            isGrater(body, _TERMINAL_DESTINY_ID, 0);
            isContainedAndNotNull(body, TYPE_SHIPMENT, SHIPMENT_TYPES.LOAD.getName(), SHIPMENT_TYPES.DOWNLOAD.getName());
            body.put(INCLUDE_PARCELS, true);

            execEventBus(context,ShipmentsDBV.FIND_PARCELS_TO_DO_LIST, body, "Found");

        } catch (PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getHistoricLoad(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, INIT_DATE);
            isEmptyAndNotNull(body, END_DATE);
            isGrater(body, _TERMINAL_ORIGIN_ID, 0);
            isGrater(body, _TERMINAL_DESTINY_ID, 0);
            isGrater(body, CONFIG_ROUTE_ID, 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_HISTORIC_LOAD, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getHistoricDownload(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, INIT_DATE);
            isEmptyAndNotNull(body, END_DATE);
            isGrater(body, _TERMINAL_ORIGIN_ID, 0);
            isGrater(body, _TERMINAL_DESTINY_ID, 0);
            isGrater(body, CONFIG_ROUTE_ID, 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_HISTORIC_DOWNLOAD, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getDailyLogs(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, "init_travel_date");
            isEmptyAndNotNull(body, "end_travel_date");
            isGrater(body, ShipmentsDBV.CONFIG_ROUTE_ID, 0);
            isGrater(body, ShipmentsDBV.TERMINAL_ORIGIN_ID, 0);
            isGrater(body, "terminal_destiny_id", 0);
            isBoolean(body, "is_parcel_route");

            execEventBus(context,ShipmentsDBV.ACTION_GET_DAILY_LOGS, body,"Found");

        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getDailyLogsDetail(RoutingContext context){
        try {
            JsonObject body = new JsonObject()
                    .put(ShipmentsDBV.TRAVEL_LOGS_ID, Integer.parseInt(context.request().getParam(ShipmentsDBV.TRAVEL_LOGS_ID)));
            isGraterAndNotNull(body, ShipmentsDBV.TRAVEL_LOGS_ID, 0);
            body.put("is_pre", false);

            execEventBus(context,ShipmentsDBV.ACTION_GET_DAILY_LOGS_DETAIL, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e);
        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void getPreDailyLogsDetail(RoutingContext context){
        try {
            JsonObject body = new JsonObject()
                    .put(ShipmentsDBV.TRAVEL_LOGS_ID, Integer.parseInt(context.request().getParam(ShipmentsDBV.TRAVEL_LOGS_ID)));
            isGraterAndNotNull(body, ShipmentsDBV.TRAVEL_LOGS_ID, 0);
            body.put("is_pre", true);

            execEventBus(context,ShipmentsDBV.ACTION_GET_PRE_DAILY_LOGS_DETAIL, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e);
        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void getIncidenceLog(RoutingContext context){
        try{
            JsonObject body = context.getBodyAsJson();
            isDateTimeAndNotNull(body, "init_date", "shipment");
            isDateTimeAndNotNull(body, "end_date", "shipment");
            if(body.getInteger("page")!=null){
                isGraterAndNotNull(body, "page", 0);
                isGraterAndNotNull(body, "limit", 0);
            }
            execEventBus(context,ShipmentsDBV.ACTION_GET_TRAVEL_LOG_LIST, body,"Found");
        }catch (UtilsValidation.PropertyValueException e){
            responseError(context, e);
        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }  

    private void getShipmentInfo(RoutingContext context){
        try {
            HttpServerRequest request = context.request();

            Integer shipmentId = Integer.parseInt(request.getParam(SHIPMENT_ID));
            JsonObject body = new JsonObject()
                    .put(SHIPMENT_ID, shipmentId);

            isGrater(body, SHIPMENT_ID, 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_SHIPMENT_INFO, body,"Found");

            } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }
    private void getParcelsToDownload(RoutingContext context){
        try {

            JsonObject body = context.getBodyAsJson();

            isGraterAndNotNull(body, TERMINAL_ID, 0);
            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID, 0);
            isGraterAndNotNull(body, LIMIT, 0);
            isGraterAndNotNull(body, PAGE, 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_PARCELS_TO_DOWNLOAD, body,"Found");

            } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }
    private void getDailyLogsLoad(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        body.put("shipment_type", SHIPMENT_TYPES.LOAD.getName());
        context.setBody(body.toBuffer());
        this.getDailyLogsByType(context);
    }

    private void getDailyLogsDownload(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        body.put("shipment_type", SHIPMENT_TYPES.DOWNLOAD.getName());
        context.setBody(body.toBuffer());
        this.getDailyLogsByType(context);
    }

    private void getDailyLogsByType(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isDateAndNotNull(body, INIT_DATE);
            isGrater(body, TERMINAL_ID, 0);
            isGrater(body, "config_destination_id", 0);
            isGrater(body, "vehicle_id", 0);
            isGrater(body, "driver_id", 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_DAILY_LOGS_BY_TYPE, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getShipmentLoadInfo(RoutingContext context){
        try {
            HttpServerRequest request = context.request();

            Integer shipmentId = Integer.parseInt(request.getParam(SHIPMENT_ID));
            JsonObject body = new JsonObject()
                    .put(SHIPMENT_ID, shipmentId);

            isGraterAndNotNull(body, SHIPMENT_ID, 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_SHIPMENT_LOAD_INFO, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void transhipmentsToLoad(RoutingContext context){
        try {
            HttpServerRequest request = context.request();

            JsonObject body = new JsonObject();
            if (request.getParam(TERMINAL_ID) != null){
                body.put(TERMINAL_ID, Integer.parseInt(request.getParam(TERMINAL_ID)));
            } else {
                JsonObject employee = context.get(EMPLOYEE);
                body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));
            }
            body.put(SCHEDULE_ROUTE_ID, Integer.parseInt(request.getParam(SCHEDULE_ROUTE_ID)));

            isGraterAndNotNull(body, TERMINAL_ID, 0);
            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID, 0);

            execEventBus(context, ACTION_TRANSHIPMENTS_TO_LOAD, body,"Found");

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getParcelsToDownloadV2(RoutingContext context){
        try {

            JsonObject body = context.getBodyAsJson();

            isGraterAndNotNull(body, TERMINAL_ID, 0);
            isGraterAndNotNull(body, SCHEDULE_ROUTE_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_GET_PARCELS_TO_DOWNLOAD_V2), (AsyncResult<Message<JsonObject>> reply) -> {
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
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, t));
                }
            });

        } catch (UtilsValidation.PropertyValueException e){
            e.printStackTrace();
            responsePropertyValue(context, e);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getDailyLogsCostReport(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");
            isGrater(body, ShipmentsDBV.TERMINAL_ORIGIN_ID, 0);
            isGrater(body, "terminal_destiny_id", 0);

            execEventBus(context,ShipmentsDBV.ACTION_GET_DAILY_LOGS_COST_REPORT, body,"Found");

        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getLoadConciliation(RoutingContext context){
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put(SHIPMENT_ID, Integer.parseInt(request.getParam(_SHIPMENT_ID)));

            isGraterAndNotNull(body, _SHIPMENT_ID, 0);

            execEventBus(context, ACTION_GET_LOAD_CONCILIATION, body,"Found");

        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getDownloadConciliation(RoutingContext context){
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put(SHIPMENT_ID, Integer.parseInt(request.getParam(_SHIPMENT_ID)));

            isGraterAndNotNull(body, _SHIPMENT_ID, 0);

            execEventBus(context, ACTION_GET_DOWNLOAD_CONCILIATION, body,"Found");

        } catch (UtilsValidation.PropertyValueException ex){
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void loadedPackagesByShipment(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();

            isGraterAndNotNull(body, _SHIPMENT_ID, 0);
            isGrater(body, _TRAILER_ID, 0);

            execEventBus(context, ACTION_GET_LOADED_PACKAGES_BY_SHIPMENT, body,"Found");

        } catch (UtilsValidation.PropertyValueException ex){
            responsePropertyValue(context, ex);
        } catch (Exception e){
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void searchParcelsToLoad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, _SCHEDULE_ROUTE_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_ID, 0);
            isEmpty(body, _PARCEL_TRACKING_CODE);
            isGrater(body, _TERMINAL_DESTINY_ID, 0);
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ShipmentsDBV.ACTION_SEARCH_PARCELS_TO_LOAD);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());
                    } catch (Exception e) {
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getLoadingInfo(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            Integer shipmentId = Integer.parseInt(request.getParam(_SHIPMENT_ID));
            JsonObject body = new JsonObject()
                    .put(_SHIPMENT_ID, shipmentId);
            isGraterAndNotNull(body, _SHIPMENT_ID, 0);
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_GET_LOADING_INFO);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());
                    } catch (Exception e) {
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void execService(RoutingContext context, String action, JsonObject body, Handler<JsonObject> handler) {
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    handler.handle(reply.result().body());
                }
            } else {
                responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
            }
        });
    }

    private void execEventBus(RoutingContext context, String action, JsonObject body, String devMessage){
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), devMessage);
                }
            } else {
                responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
            }
        });
    }

}
