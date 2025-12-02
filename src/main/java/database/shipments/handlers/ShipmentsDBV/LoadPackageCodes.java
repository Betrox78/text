package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsPackagesTrackingDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import database.shipments.handlers.ShipmentsDBV.models.LoadTranshipmentsTransactions;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import static database.boardingpass.BoardingPassDBV.SHIPMENT_ID;
import static database.parcel.ParcelDBV.PARCEL_ID;
import static database.parcel.ParcelsPackagesDBV.PARCEL_PACKAGE_ID;
import static database.vechicle.TrailersDBV.TRAILER_ID;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;

public class LoadPackageCodes extends DBHandler<ShipmentsDBV> {
    public static final String ACTION = "ShipmentsDBV.LoadPackageCodes";

    public LoadPackageCodes(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer shipmentId = body.getInteger(_SHIPMENT_ID);
            Integer trailerId = body.getInteger(TRAILER_ID);
            Integer createdBy = body.getInteger(CREATED_BY);
            JsonArray codes = body.getJsonArray("codes");

            JsonArray parcelPackages = new JsonArray();
            JsonArray parcels = new JsonArray();
            JsonArray wrongCodes = new JsonArray();
            JsonArray packagesLogScanner = new JsonArray();
            JsonObject resCodes = new JsonObject();

            resCodes.put("packages", parcelPackages)
                    .put("parcels", parcels)
                    .put("wrongCodes",wrongCodes)
                    .put("packagesLogScanner", packagesLogScanner);

            if (codes.isEmpty()) {
                message.reply(new JsonObject()
                        .put("packages", new JsonArray())
                        .put("codes_with_error", new JsonArray()));
            } else {
                this.getShipmentInfo(shipmentId).whenComplete((shipment, errShipment) -> {
                    try {
                        if (errShipment != null){
                            throw errShipment;
                        }

                        Integer shipmentTerminalId = shipment.getInteger(_TERMINAL_ID);
                        Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);

                        List<Future> futures = new ArrayList<>();
                        futures.add(getPackageInfoAndTracking(codes, shipmentId, shipmentTerminalId));
                        if (Objects.nonNull(trailerId)) {
                            futures.add(validateTrailer(scheduleRouteId, trailerId));
                        }

                        CompositeFuture.all(futures).setHandler(reply -> {
                            try {
                                if (reply.failed()) {
                                    throw reply.cause();
                                }

                                List<JsonObject> packageInfoTrackingList = reply.result().resultAt(0);
                                if (packageInfoTrackingList.isEmpty()){
                                    JsonObject wrongCode = new JsonObject()
                                            .put("CODE", "ALL_CODES")
                                            .put("TYPE","PACKAGE")
                                            .put("CAUSE","CODES NOT FOUND")
                                            .put(_MESSAGE, new JsonObject()
                                                    .put(_ES, "Paquetes no encontrados")
                                                    .put(_EN, "Codes not found"));
                                    wrongCodes.add(wrongCode);
                                    JsonObject response = new JsonObject()
                                            .put("packages", packageInfoTrackingList.size())
                                            .put("parcels", packageInfoTrackingList)
                                            .put("wrongCodes", wrongCodes)
                                            .put("codes_with_error", wrongCodes);
                                    message.reply(response);
                                    return;
                                }

                                List<JsonObject> trailersList = new ArrayList<>();
                                if (Objects.nonNull(trailerId)) {
                                    trailersList.addAll(reply.result().resultAt(1));
                                }

                                this.validateWrongCodesPackages(wrongCodes, packagesLogScanner, packageInfoTrackingList, trailersList, shipment, trailerId);

                                this.buildSuccessLog(packageInfoTrackingList, packagesLogScanner, shipment, trailerId).whenComplete((resBSL, errBSL) -> {
                                    try {
                                        if (errBSL != null) {
                                            throw errBSL;
                                        }

                                        startTransaction(message, conn ->
                                                this.doInsertsAndUpdates(conn, packageInfoTrackingList, shipment, trailerId, createdBy).whenComplete((res, error) -> {
                                                    try {
                                                        if (error != null){
                                                            throw error;
                                                        }

                                                        JsonObject response = new JsonObject()
                                                                .put("packages", packageInfoTrackingList.size())
                                                                .put("parcels", packageInfoTrackingList)
                                                                .put("wrongCodes", wrongCodes)
                                                                .put("codes_with_error", wrongCodes);

                                                        this.saveScannerLogAndCommit(conn, message, response, packagesLogScanner, createdBy);

                                                    } catch (Throwable t){
                                                        this.rollback(conn, t, message);
                                                    }
                                                }));

                                    } catch (Throwable t) {
                                        reportQueryError(message, t);
                                    }
                                });

                            } catch (Throwable t) {
                                reportQueryError(message, t);
                            }
                        });

                    } catch (Throwable t){
                        reportQueryError(message, t);
                    }
                });
            }
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void saveScannerLogAndCommit(SQLConnection conn, Message<JsonObject> message, JsonObject response, JsonArray packagesLogScanner, Integer createdBy){
        if (packagesLogScanner.isEmpty()) {
            this.commit(conn, message, response);
        } else {
            List<GenericQuery> inserts = packagesLogScanner.stream()
                    .map(l -> getScannerLogInsert((JsonObject) l, createdBy))
                    .collect(toList());
            List<JsonArray> params = inserts.stream().map(GenericQuery::getParams).collect(toList());
            conn.batchWithParams(inserts.get(0).getQuery(), params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    this.commit(conn, message, response);
                } catch (Throwable t) {
                    this.commit(conn, message, response);
                }
            });
        }
    }

    private GenericQuery getScannerLogInsert(JsonObject log, Integer createdBy) {
        JsonObject pack = log.getJsonObject(_PACKAGE);
        return this.generateGenericCreate("parcels_packages_scanner_tracking", new JsonObject()
                .put(_PARCEL_ID, pack.getInteger(_PARCEL_ID))
                .put(_PARCEL_PACKAGE_ID, pack.getInteger(_PARCEL_PACKAGE_ID))
                .put(_SHIPMENT_ID, log.getInteger(_SHIPMENT_ID))
                .put(_SCHEDULE_ROUTE_ID, log.getInteger(_SCHEDULE_ROUTE_ID))
                .put(_BRANCHOFFICE_ID, log.getInteger(_BRANCHOFFICE_ID))
                .put(_TRAILER_ID, log.getInteger(_TRAILER_ID))
                .put(_MESSAGE, log.getString(_MESSAGE))
                .put(_ACTION, "load")
                .put(CREATED_BY, createdBy));
    }

    private CompletableFuture<JsonObject> getShipmentInfo(Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_SHIPMENT_INFO, new JsonArray().add(shipmentId), result -> {
           try {
               if (result.failed()){
                   throw result.cause();
               }

               List<JsonObject> resultList = result.result().getRows();
               if (resultList.isEmpty()){
                   throw new Exception("Shipment not found");
               }

               JsonObject shipment = resultList.get(0);
               future.complete(shipment);
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> doInsertsAndUpdates(SQLConnection conn, List<JsonObject> packageInfoTrackingList, JsonObject shipment, Integer trailerId, Integer createdBy){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            Integer shipmentId = shipment.getInteger(ID);
            Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);
            Integer shipmentTerminalId = shipment.getInteger(_TERMINAL_ID);

            if(packageInfoTrackingList.isEmpty()) {
                future.complete(packageInfoTrackingList);
            } else {
                this.createShipmentParcelPackageTracking(conn, packageInfoTrackingList, shipmentId, trailerId, scheduleRouteId, createdBy).whenComplete((resSPPT, errorSPPT) -> {
                    try {
                        if (errorSPPT != null){
                            throw errorSPPT;
                        }

                        this.setParcelsScheduleRouteDestination(conn, packageInfoTrackingList, createdBy).whenComplete((resPIT, errPIT) -> {
                            try {
                                if (errPIT != null) {
                                    throw errPIT;
                                }

                                this.loadedActions(conn, packageInfoTrackingList, shipmentTerminalId, createdBy).whenComplete((resLoad, errLoad) -> {
                                    try {
                                        if (errLoad != null) {
                                            throw errLoad;
                                        }

                                        this.actionParcelsTranshipments(conn, packageInfoTrackingList, createdBy).whenComplete((resAPT, errAPT) -> {
                                            try {
                                                if(errAPT != null) {
                                                    throw errAPT;
                                                }

                                                future.complete(packageInfoTrackingList);

                                            } catch (Throwable t) {
                                                future.completeExceptionally(t);
                                            }
                                        });

                                    } catch (Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });

                            } catch (Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });

                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            }
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private Future<List<JsonObject>> getPackageInfoAndTracking(JsonArray packageCodes, Integer shipmentId, Integer shipmentTerminalId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            String packageCodeParams = packageCodes.stream()
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(", "));

            String QUERY = String.format(QUERY_GET_PACKAGE_INFO_LOAD_AND_TRACKING,
                    shipmentId, packageCodeParams);
            this.dbClient.query(QUERY, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> packageInfoTrackingList = reply.result().getRows();
                    future.complete(packageInfoTrackingList);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<List<JsonObject>> validateTrailer(Integer scheduleRouteId, Integer trailerId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            this.dbClient.queryWithParams(QUERY_VALIDATE_SHIPMENT_TRAILER, new JsonArray().add(scheduleRouteId).add(trailerId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(reply.result().getRows());
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> createShipmentParcelPackageTracking(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer shipmentId, Integer trailerId, Integer scheduleRouteId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        cleanLatestMovement(conn, packageInfoTrackingList, scheduleRouteId, createdBy).whenComplete((resCM, errCM) -> {
            try {
                if (errCM != null) {
                    throw errCM;
                }

                List<GenericQuery> inserts = packageInfoTrackingList.stream()
                        .map(p -> {
                            JsonObject body = new JsonObject()
                                    .put(PARCEL_ID, p.getInteger(PARCEL_ID))
                                    .put(PARCEL_PACKAGE_ID, p.getInteger(PARCEL_PACKAGE_ID))
                                    .put(SHIPMENT_ID, shipmentId)
                                    .put(LATEST_MOVEMENT, true)
                                    .put(STATUS, PARCELPACKAGETRACKING_STATUS.LOADED.getValue())
                                    .put(CREATED_BY,createdBy);
                            if (Objects.nonNull(trailerId)) {
                                body.put(TRAILER_ID, trailerId);
                            }
                            return generateGenericCreate("shipments_parcel_package_tracking", body);
                        })
                        .collect(Collectors.toList());

                List<JsonArray> insertShipmentPPTParams = inserts.stream()
                        .map(GenericQuery::getParams)
                        .collect(Collectors.toList());

                if (!inserts.isEmpty()) {
                    conn.batchWithParams(inserts.get(0).getQuery(), insertShipmentPPTParams, res ->{
                        try {
                            if(res.failed()){
                                throw res.cause();
                            }

                            future.complete(true);
                        }catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    future.complete(true);
                }

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> cleanLatestMovement(SQLConnection conn, List<JsonObject> packages, Integer scheduleRouteId, int updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String parcelPackageIdParams = packages.stream()
                    .map(pack -> "'" + pack.getInteger(PARCEL_PACKAGE_ID) + "'")
                    .collect(Collectors.joining(", "));
            conn.update(String.format(UPDATE_SHIPMENTS_PARCEL_PACKAGE_TRACKING_CLEAN_LATEST_MOVEMENT, updatedBy, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), scheduleRouteId, parcelPackageIdParams), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void validateWrongCodesPackages(JsonArray wrongCodes, JsonArray packagesLogScanner, List<JsonObject> packageInfoTrackingList, List<JsonObject> trailersList, JsonObject shipment, Integer trailerId){
        Integer shipmentId = shipment.getInteger(ID);
        Integer shipTerminalId = shipment.getInteger(_TERMINAL_ID);
        Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);
        Integer shipStatus = shipment.getInteger(_SHIPMENT_STATUS);
        List<JsonObject> packageInfoTrackingListCopy = new ArrayList<>(packageInfoTrackingList);

        for (JsonObject pack : packageInfoTrackingListCopy) {
            Integer packageTrackingTerminalId = pack.getInteger(_TERMINAL_ID);
            PARCELPACKAGETRACKING_STATUS packageTrackingAction = PARCELPACKAGETRACKING_STATUS.fromValue(pack.getString(_ACTION));
            PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[pack.getInteger(_PACKAGE_STATUS)];
            String packageCode = pack.getString(_PACKAGE_CODE);
            Integer scheduleRouteDestinationId = pack.getInteger("schedule_route_destination_id");
            Integer transhipmentScheduleRouteDestinationId = pack.getInteger("transhipment_schedule_route_destination_id");
            Integer parcelTranshipmentId = pack.getInteger("parcel_transhipment_id");
            boolean isTranshipment = pack.getInteger("is_transhipment") > 0 || Objects.nonNull(parcelTranshipmentId);
            Integer countTerminalsReceiveTranshipments = pack.getInteger("count_terminals_route_receive_transhipments");
            Integer countTranshipments = pack.getInteger("transhipments_count");
            boolean haveReplacementTerminal = pack.getInteger("have_replacement_terminal") == 1;
            Integer shipmentParcelId = pack.getInteger("shipment_parcel_id");
            boolean isReplacementTerminal = pack.getInteger("is_replacement_terminal") == 1;

            if(packageStatus.equals(PACKAGE_STATUS.CANCELED) || packageTrackingAction.isCanceled()){
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","THE PACKAGE WAS CANCELED")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "El paquete fue cancelado")
                                .put(_EN, "The package was canceled")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "The package was canceled"));
                packageInfoTrackingList.remove(pack);
            } else if(Objects.nonNull(trailerId) && trailersList.isEmpty()) {
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","TRAILER IS NOT ASSIGNED TO THE ROUTE OR WAS RELEASED OR TRANSFERRED")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "Remolque no asignado a la ruta, fue despegado o cambiado")
                                .put(_EN, "Trailer is not assigned to the route or was released or transferred")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "Trailer is not assigned to the route or was released or transferred"));
                packageInfoTrackingList.remove(pack);
            } else if(Objects.isNull(shipmentParcelId)) {
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE", "PACKAGE")
                        .put("CAUSE", "WAYBILL NOT SCANNED")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "Carta porte no escaneada")
                                .put(_EN, "Waybill not scanned")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "Waybill not scanned"));
                packageInfoTrackingList.remove(pack);
            } else if(Objects.isNull(scheduleRouteDestinationId) && Objects.isNull(transhipmentScheduleRouteDestinationId)) {
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE", "PACKAGE")
                        .put("CAUSE", "THE PACKAGE CANNOT BE LOAD IN THIS SEGMENT, DESTINY NOT FOUND IN THIS ROUTE")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "El paquete no puede cargarse en este segmento, el destino no se encuentra en la ruta")
                                .put(_EN, "The package cannot be load in this segment, destiny not found in this route")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "The package cannot be load in this segment, destiny not found in this route"));
                packageInfoTrackingList.remove(pack);
            } else if(isTranshipment && countTerminalsReceiveTranshipments == 0 && countTranshipments == 0) {
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","THE ROUTE IS NOT AVAILABLE TO TRANSHIPMENTS")
                        .put(_MESSAGE, new JsonObject()
                            .put(_ES, "La ruta no esta disponible para trasbordos")
                            .put(_EN, "The route is not available to transhipments")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "The route is not available to transhipments"));
                packageInfoTrackingList.remove(pack);
            } else if (scheduleRouteDestinationId == null && !isTranshipment && !haveReplacementTerminal){
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","PACKAGE DESTINATION NOT FOUND")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "El destino del paquete no fue encontrado")
                                .put(_EN, "Package destination not found")));
                packagesLogScanner.add(new JsonObject()
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "Destination not found"));
                packageInfoTrackingList.remove(pack);
            } else if(!shipTerminalId.equals(packageTrackingTerminalId) && !isReplacementTerminal){
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","THE PACKAGE IS NOT IN THE TERMINAL WHERE THE SHIPMENT WAS OPENED")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "El paquete no esta en la terminal del embarque")
                                .put(_EN, "The package is not in the terminal where the shipment was opened")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "The package is not in the terminal of shipment is open"));
                packageInfoTrackingList.remove(pack);
            } else if(!shipStatus.equals(SHIPMENT_STATUS.OPEN.ordinal())){
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","THE LOADING IS ALREADY CLOSED")
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "El embarque esta cerrado")
                                .put(_EN, "The loading is already closed")));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "The loading is already closed"));
                packageInfoTrackingList.remove(pack);
            } else if(packageTrackingAction.validatePackagesLoad()){
                wrongCodes.add(new JsonObject()
                        .put("CODE", packageCode)
                        .put("TYPE","PACKAGE")
                        .put("CAUSE","CANNOT LOAD, CURRENT STATUS: " + packageTrackingAction.getValue())
                        .put(_MESSAGE, new JsonObject()
                                .put(_ES, "No se puede embarcar, estatus actual: " + packageTrackingAction.getValueES())
                                .put(_EN, "Cannot load, current status: " + packageTrackingAction.getValue())));
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "Cannot load, current status: " + packageTrackingAction.getValue()));
                packageInfoTrackingList.remove(pack);
            }
        }

    }

    private CompletableFuture<Boolean> setParcelsScheduleRouteDestination(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer updatedBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            List<String> updates = new ArrayList<>();

            Map<Integer, List<JsonObject>> packageInfoTrackingBySRDList = packageInfoTrackingList.stream()
                    .collect(Collectors.groupingBy(p -> {
                        Integer srdId = p.getInteger("schedule_route_destination_id");
                        Integer transhipmentSrdId = p.getInteger("transhipment_schedule_route_destination_id");
                        return Objects.nonNull(srdId) ? srdId : transhipmentSrdId;
                    }));

            packageInfoTrackingBySRDList.forEach((srdId, pList) -> {
                updates.add(getQueryUpdateParcelSRDID(pList, srdId, updatedBy));
                updates.addAll(getQueryInsertsParcelTranshipmentsHistory(pList, srdId, updatedBy));
            });

            conn.batch(updates, resultBatch -> {
                try {
                    if (resultBatch.failed()){
                        throw resultBatch.cause();
                    }

                    future.complete(!resultBatch.result().isEmpty());
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private String getQueryUpdateParcelSRDID(List<JsonObject> pList, int srdId, int updatedBy) {
        List<Integer> parcelIdList = pList.stream()
                .map(r -> r.getInteger(_PARCEL_ID)).distinct()
                .collect(Collectors.toList());
        String parcelIdParams = parcelIdList.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(", "));
        return String.format(QUERY_UPDATE_PARCELS_SCHEDULE_ROUTE_DESTINATION, srdId, updatedBy, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), parcelIdParams);
    }

    private List<String> getQueryInsertsParcelTranshipmentsHistory(List<JsonObject> pList, int srdId, int updatedBy) {
        List<String> creates = new ArrayList<>();
        for (JsonObject p : pList) {
            int parcelId = p.getInteger(_PARCEL_ID);
            int packageId = p.getInteger(_PARCEL_PACKAGE_ID);
            boolean isTranshipment = p.getInteger("is_transhipment") > 0 || Objects.nonNull(p.getInteger("parcel_transhipment_id"));
            if (isTranshipment) {
                creates.add(String.format(QUERY_INSERT_PARCELS_TRANSHIPMENTS_HISTORY, parcelId, packageId, srdId, updatedBy));
            }
        }
        return creates;
    }

    private CompletableFuture<Boolean> loadedActions(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer terminalId, Integer userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<Integer> parcelPackagesIdList = packageInfoTrackingList.stream()
                    .map(r -> r.getInteger(_PARCEL_PACKAGE_ID)).distinct().collect(Collectors.toList());

            ArrayList<CompletableFuture<Boolean>> tasks = new ArrayList<>();
            tasks.add(setParcelPackageStatus(conn, parcelPackagesIdList, userId));
            tasks.add(insertPackageTracking(conn, packageInfoTrackingList, terminalId, userId));
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
                try {
                    if (error != null){
                        throw error;
                    }
                    future.complete(true);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> setParcelPackageStatus(SQLConnection conn, List<Integer> parcelPackagesIdList, Integer updatedBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String parcelPackagesParams = parcelPackagesIdList.stream()
                    .map(id -> "'" + id + "'")
                    .collect(Collectors.joining(", "));

            String updates = String.format(QUERY_UPDATE_PARCELS_PACKAGES, PACKAGE_STATUS.LOADED.ordinal(), updatedBy, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), parcelPackagesParams);

            conn.update(updates, result -> {
                try {
                    if (result.failed()){
                        throw result.cause();
                    }

                    future.complete(result.result().getUpdated()>0);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertPackageTracking(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer terminalId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            List<GenericQuery> inserts = packageInfoTrackingList.stream()
                    .map(p -> this.generateGenericCreate(new ParcelsPackagesTrackingDBV().getTableName(),
                            new JsonObject()
                                    .put(_PARCEL_ID, p.getInteger(_PARCEL_ID))
                                    .put(_PARCEL_PACKAGE_ID, p.getInteger(_PARCEL_PACKAGE_ID))
                                    .put(_TERMINAL_ID, terminalId)
                                    .put(_ACTION, PARCELPACKAGETRACKING_STATUS.LOADED.getValue())
                                    .put(CREATED_BY,createdBy)))
                    .collect(toList());

            List<JsonArray> insertPPTParams = inserts.stream()
                    .map(GenericQuery::getParams)
                    .collect(Collectors.toList());

            conn.batchWithParams(inserts.get(0).getQuery(), insertPPTParams, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    future.complete(!inserts.isEmpty());
                }catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> actionParcelsTranshipments(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            LoadTranshipmentsTransactions transactions = getTranshipmentsTransactions(packageInfoTrackingList, createdBy);
            batchQuerysParcelsTranshipments(conn, transactions.getCreates()).whenComplete((replyCreates, errCreates) -> {
                try {
                    if (errCreates != null) {
                        throw errCreates;
                    }
                    batchQuerysParcelsTranshipments(conn, transactions.getUpdates()).whenComplete((replyUpdates, errUpdates) -> {
                        try {
                            if (errUpdates != null) {
                                throw errUpdates;
                            }
                            future.complete(true);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private LoadTranshipmentsTransactions getTranshipmentsTransactions(List<JsonObject> packageInfoTrackingList, Integer createdBy) {
        LoadTranshipmentsTransactions transactions = new LoadTranshipmentsTransactions();
        packageInfoTrackingList.forEach(pack -> {
            Integer transhipmentId = pack.getInteger("parcel_transhipment_id");
            boolean isTranshipment = pack.getInteger("is_transhipment") > 0 || Objects.nonNull(transhipmentId);
            if (isTranshipment) {
                if (Objects.isNull(transhipmentId)) {
                    transactions.addCreate(this.generateGenericCreate("parcels_transhipments",
                            new JsonObject()
                                    .put(_PARCEL_ID, pack.getInteger(_PARCEL_ID))
                                    .put(_PARCEL_PACKAGE_ID, pack.getInteger(_PARCEL_PACKAGE_ID))
                                    .put(CREATED_AT, UtilsDate.sdfDataBase(new Date()))
                                    .put(CREATED_BY, createdBy)));
                } else {
                    Integer transhipmentCount = pack.getInteger("parcel_transhipment_count");
                    transactions.addUpdate(this.generateGenericUpdate("parcels_transhipments",
                            new JsonObject()
                                    .put(ID, transhipmentId)
                                    .put(_PARCEL_ID, pack.getInteger(_PARCEL_ID))
                                    .put(_PARCEL_PACKAGE_ID, pack.getInteger(_PARCEL_PACKAGE_ID))
                                    .put("count", transhipmentCount + 1)
                                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                                    .put(UPDATED_BY, createdBy)));
                }
            }
        });
        return transactions;
    }

    private CompletableFuture<Boolean> batchQuerysParcelsTranshipments(SQLConnection conn, List<GenericQuery> queries){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            if (!queries.isEmpty()) {
                List<JsonArray> queriesParams = queries.stream()
                        .map(GenericQuery::getParams)
                        .collect(Collectors.toList());

                conn.batchWithParams(queries.get(0).getQuery(), queriesParams, reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        future.complete(!queries.isEmpty());
                    }catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else {
                future.complete(true);
            }
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> buildSuccessLog(List<JsonObject> packageInfoTrackingList, JsonArray packagesLogScanner, JsonObject shipment, Integer trailerId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            if(packageInfoTrackingList.isEmpty()) {
                future.complete(true);
            } else {
                Integer shipmentId = shipment.getInteger(ID);
                Integer terminalId = shipment.getInteger(_TERMINAL_ID);
                Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);

                Map<Integer, List<JsonObject>> groupedPackages = packageInfoTrackingList.stream()
                        .collect(Collectors.groupingBy(p -> p.getInteger(_PARCEL_ID)));

                List<CompletableFuture<Boolean>> countLoadedQuerys = new ArrayList<>();

                for (Integer parcelId : groupedPackages.keySet()) {
                    List<JsonObject> packages = groupedPackages.get(parcelId);
                    countLoadedQuerys.add(addPackSuccessLog(packages, packagesLogScanner, shipmentId, scheduleRouteId, terminalId, trailerId));
                }

                CompletableFuture.allOf(countLoadedQuerys.toArray(new CompletableFuture[countLoadedQuerys.size()])).whenComplete((resCLQ, errCLQ) -> {
                    try {
                        if(errCLQ != null) {
                            throw errCLQ;
                        }
                        future.complete(true);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> addPackSuccessLog(List<JsonObject> packages, JsonArray packagesLogScanner,
                                                         Integer shipmentId, Integer scheduleRouteId, Integer terminalId, Integer trailerId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            JsonObject basePack = packages.get(0);
            Integer parcelTranshipmentId = basePack.getInteger("parcel_transhipment_id");
            boolean isTranshipment = basePack.getInteger("is_transhipment") > 0 || Objects.nonNull(parcelTranshipmentId);

            getCountLoadInfo(isTranshipment, scheduleRouteId, basePack).whenComplete((countLoadInfo, errCLI) -> {
                try {
                    if (errCLI != null) {
                        throw errCLI;
                    }

                    Integer countLoad = countLoadInfo.getInteger(_COUNT_LOAD);
                    Integer totalPackages = countLoadInfo.getInteger(_TOTAL_PACKAGES);

                    for (JsonObject pack : packages) {
                        packagesLogScanner.add(new JsonObject()
                                .put(_PACKAGE, pack)
                                .put(_SHIPMENT_ID, shipmentId)
                                .put(_SCHEDULE_ROUTE_ID, scheduleRouteId)
                                .put(_BRANCHOFFICE_ID, terminalId)
                                .put(_TRAILER_ID, trailerId)
                                .put(_MESSAGE, "Load success - " + countLoad + " / " + totalPackages));
                        countLoad++;
                    }
                    future.complete(true);

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getCountLoadInfo(boolean isTranshipment, Integer scheduleRouteId, JsonObject pack) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY;
            JsonArray params = new JsonArray();
            if (isTranshipment) {
                QUERY = QUERY_GET_COUNT_LOAD_TRANSHIPMENT;
                params.add(scheduleRouteId).add(pack.getInteger(_PARCEL_ID));
            } else {
                QUERY = QUERY_GET_COUNT_LOAD;
                params.add(pack.getInteger(_PARCEL_ID));
            }
            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> resultList = reply.result().getRows();
                    if(resultList.isEmpty()) {
                        throw new Exception("Count load error");
                    }
                    JsonObject result = resultList.get(0);
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_GET_PACKAGE_INFO_LOAD_AND_TRACKING = "SELECT\n" +
            "    pp.parcel_id,\n" +
            "    pp.id as parcel_package_id,\n" +
            "    pp.package_status,\n" +
            "    pp.package_code,\n" +
            "    p.terminal_origin_id,\n" +
            "    p.terminal_destiny_id,\n" +
            "    p.parcel_tracking_code,\n" +
            "    ppt.action,\n" +
            "    ppt.terminal_id,\n" +
            "    IF(s.terminal_id IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "                       WHERE of_branchoffice_id = p.terminal_origin_id), TRUE, FALSE) AS is_replacement_terminal,\n" +
            "    sp.id AS shipment_parcel_id,\n" +
            "    srd.id AS schedule_route_destination_id,\n" +
            "    (SELECT srd3.id FROM schedule_route_destination srd3\n" +
            "       INNER JOIN schedule_route sr3 ON sr3.id = srd3.schedule_route_id\n" +
            "       INNER JOIN config_destination cd3 ON cd3.config_route_id = sr3.config_route_id\n" +
            "           AND cd3.terminal_origin_id = srd3.terminal_origin_id AND cd3.terminal_destiny_id = srd3.terminal_destiny_id\n" +
            "       INNER JOIN shipments s3 ON s3.schedule_route_id = sr3.id\n" +
            "       INNER JOIN branchoffice bd ON bd.id = srd3.terminal_destiny_id\n" +
            "       WHERE s3.id = s.id\n" +
            "           AND cd3.terminal_origin_id = s.terminal_id\n" +
            "           AND bd.receive_transhipments IS TRUE\n" +
            "       ORDER BY cd3.order_destiny LIMIT 1) AS transhipment_schedule_route_destination_id,\n" +
            "    ptr.id AS parcel_transhipment_id,\n" +
            "    COALESCE(ptr.count, 0) AS parcel_transhipment_count,\n" +
            "    IF((SELECT srd2.id FROM schedule_route_destination srd2\n" +
            "       INNER JOIN schedule_route sr2 ON sr2.id = srd2.schedule_route_id\n" +
            "       WHERE sr2.id = sr.id AND\n" +
            "       (srd2.terminal_destiny_id = p.terminal_destiny_id\n" +
            "           OR srd2.terminal_destiny_id IN (SELECT receiving_branchoffice_id FROM branchoffice_parcel_receiving_config\n" +
            "           WHERE of_branchoffice_id = p.terminal_destiny_id)) LIMIT 1) IS NULL, TRUE, FALSE) AS is_transhipment,\n" +
            "    (SELECT COUNT(bd4.id) FROM branchoffice bd4\n" +
            "       INNER JOIN config_destination cd4 ON cd4.terminal_destiny_id = bd4.id\n" +
            "       INNER JOIN config_route cr4 ON cr4.id = cd4.config_route_id\n" +
            "       INNER JOIN schedule_route sr4 ON sr4.config_route_id = cr4.id\n" +
            "       WHERE bd4.receive_transhipments IS TRUE\n" +
            "       AND sr4.id = sr.id) AS count_terminals_route_receive_transhipments,\n" +
            "    COALESCE(ptr.count) AS transhipments_count,\n" +
            "    (SELECT IF(SUM(IF(srd5.terminal_destiny_id IN\n" +
            "           (SELECT bprc.receiving_branchoffice_id\n" +
            "           FROM branchoffice_parcel_receiving_config bprc\n" +
            "           WHERE bprc.of_branchoffice_id = p.terminal_destiny_id), 1, 0)) > 0, TRUE, FALSE)\n" +
            "     FROM schedule_route sr5\n" +
            "     INNER JOIN schedule_route_destination srd5 ON srd5.schedule_route_id = sr5.id\n" +
            "     WHERE sr5.id = sr.id) AS have_replacement_terminal\n" +
            "FROM parcels_packages AS pp\n" +
            "INNER JOIN parcels AS p ON p.id = pp.parcel_id\n" +
            "INNER JOIN parcels_packages_tracking ppt ON ppt.parcel_id = p.id AND ppt.parcel_package_id = pp.id\n" +
            "LEFT JOIN parcels_transhipments ptr ON ptr.parcel" +
            "_id = p.id AND ptr.parcel_package_id = pp.id\n" +
            "LEFT JOIN shipments s ON s.id = %d\n" +
            "LEFT JOIN shipments_parcels sp ON sp.shipment_id = s.id AND sp.parcel_id = p.id\n" +
            "LEFT JOIN schedule_route sr ON sr.id = s.schedule_route_id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = s.terminal_id\n" +
            "    AND (srd.terminal_destiny_id = p.terminal_destiny_id OR\n" +
            "    (srd.terminal_destiny_id IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "                               WHERE of_branchoffice_id = p.terminal_destiny_id)))\n" +
            "WHERE ppt.id IN\n" +
            "(SELECT MAX(ppt2.id) FROM parcels_packages_tracking ppt2\n" +
            "     WHERE ppt2.parcel_package_id = ppt.parcel_package_id\n" +
            "     AND ppt2.action NOT IN ('incidence', 'printed'))\n" +
            "AND pp.package_code in (%s)\n" +
            "AND (s.terminal_id = p.terminal_origin_id \n" +
            "   OR s.terminal_id = ppt.terminal_id \n" +
            "   OR s.terminal_id IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "                       WHERE of_branchoffice_id = p.terminal_origin_id))\n" +
            "GROUP BY pp.id;";

    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT * FROM shipments where id = ? ;";

    private static final String QUERY_UPDATE_PARCELS_SCHEDULE_ROUTE_DESTINATION = "UPDATE parcels \n" +
            "SET schedule_route_destination_id = %d, \n" +
            "updated_by = %d, \n" +
            "updated_at = '%s' \n" +
            "WHERE id IN (%s);";

    private static final String QUERY_UPDATE_PARCELS_PACKAGES = "UPDATE parcels_packages \n" +
            "SET package_status = %d,\n" +
            "updated_by = %d, updated_at = '%s' \n" +
            "WHERE id IN (%s);";

    private static final String QUERY_INSERT_PARCELS_TRANSHIPMENTS_HISTORY = "INSERT INTO \n" +
            "parcels_transhipments_history(parcel_id, parcel_package_id, schedule_route_destination_id, created_by) \n" +
            "VALUES(%d, %d, %d, %d);";

    private static final String QUERY_VALIDATE_SHIPMENT_TRAILER = "SELECT st.* FROM shipments_trailers st\n" +
            "WHERE st.schedule_route_id = ? AND st.trailer_id = ?\n" +
            "AND st.latest_movement IS TRUE\n" +
            "AND (st.trailer_id NOT IN (SELECT st2.trailer_id FROM shipments_trailers st2\n" +
            "    WHERE st2.schedule_route_id = st.schedule_route_id\n" +
            "    AND st2.trailer_id = st.trailer_id AND st2.action IN ('release', 'release_transhipment')\n" +
            "    AND st2.latest_movement IS TRUE\n" +
            ") AND st.trailer_id NOT IN(\n" +
            "SELECT st3.transfer_trailer_id FROM shipments_trailers st3\n" +
            "    WHERE st3.schedule_route_id = st.schedule_route_id\n" +
            "    AND st3.transfer_trailer_id = st.trailer_id\n" +
            "    AND st3.action IN ('release', 'release_transhipment', 'transfer')\n" +
            "    AND st3.latest_movement IS TRUE\n" +
            "    AND st3.transfer_trailer_id NOT IN (\n" +
            "\t\tSELECT st4.trailer_id FROM shipments_trailers st4 \n" +
            "        WHERE st4.schedule_route_id = st3.schedule_route_id \n" +
            "        AND st4.action = 'assign' AND st4.latest_movement IS TRUE)\n" +
            "));";

    private final static String UPDATE_SHIPMENTS_PARCEL_PACKAGE_TRACKING_CLEAN_LATEST_MOVEMENT = "UPDATE shipments_parcel_package_tracking AS sppt\n" +
            "INNER JOIN shipments AS s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN schedule_route AS sr ON sr.id = s.schedule_route_id\n" +
            "   SET sppt.latest_movement = FALSE,\n" +
            "   sppt.updated_by = %d, \n" +
            "   sppt.updated_at = '%s'\n" +
            "WHERE sr.id = %d\n" +
            "   AND sppt.parcel_package_id IN (%s)\n" +
            "   AND sppt.latest_movement IS TRUE;";

    private final static String QUERY_GET_COUNT_LOAD = "SELECT \n" +
            "   (COALESCE(COUNT(pptc2.parcel_package_id), 0) + 1) AS count_load, \n" +
            "   pc2.total_packages \n" +
            " FROM parcels pc2\n" +
            " INNER JOIN parcels_packages ppc2 ON ppc2.parcel_id = pc2.id\n" +
            " LEFT JOIN parcels_packages_tracking pptc2 ON pptc2.parcel_id = pc2.id AND pptc2.action = 'loaded'\n" +
            " WHERE pc2.id = ?\n" +
            " GROUP BY ppc2.id\n" +
            " LIMIT 1";

    private final static String QUERY_GET_COUNT_LOAD_TRANSHIPMENT = "SELECT \n" +
            "   COALESCE(SUM(DISTINCT IF(pth.parcel_package_id IS NULL, 0, 1)), 0) + 1 AS count_load,\n" +
            "    p.total_packages\n" +
            "FROM parcels p \n" +
            "INNER JOIN schedule_route sr ON sr.id = ?\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "LEFT JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id\n" +
            "   AND srd.id = pth.schedule_route_destination_id\n" +
            "LEFT JOIN parcels_packages pp ON pp.id = pth.parcel_package_id\n" +
            "   AND pp.parcel_id = p.id\n" +
            "LEFT JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_id = p.id AND sppt.status = 'loaded'\n" +
            "LEFT JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "WHERE p.id = ?;";

}