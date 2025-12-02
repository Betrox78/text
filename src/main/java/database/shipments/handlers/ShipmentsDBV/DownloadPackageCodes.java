package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsPackagesTrackingDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.*;
import static database.parcel.ParcelDBV.PARCEL_ID;
import static database.parcel.ParcelsPackagesDBV.PARCEL_PACKAGE_ID;
import static database.vechicle.TrailersDBV.TRAILER_ID;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;

public class DownloadPackageCodes extends DBHandler<ShipmentsDBV> {

    public DownloadPackageCodes(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            Integer shipmentId = body.getInteger(_SHIPMENT_ID);
            Integer createdBy = body.getInteger(CREATED_BY);
            JsonArray codes = body.getJsonArray("codes");
            Integer trailerId = body.getInteger(TRAILER_ID);
            Boolean originTrailers = body.getBoolean("origin_trailers", false);

            JsonArray parcelPackages = new JsonArray();
            JsonArray parcels = new JsonArray();
            JsonArray wrongCodes = new JsonArray();
            JsonArray packagesLogScanner = new JsonArray();
            JsonObject resCodes = new JsonObject();

            resCodes.put("packages", parcelPackages)
                    .put("parcels", parcels)
                    .put("packagesLogScanner", packagesLogScanner)
                    .put("wrongCodes",wrongCodes);

            if (codes.isEmpty()) {
                message.reply(new JsonObject()
                        .put("packages", 0));
                return;
            }

            this.getShipmentInfo(shipmentId).whenComplete((shipment, errShipment) -> {
                try {
                    if (errShipment != null){
                        throw errShipment;
                    }

                    Integer shipmentTerminalId = shipment.getInteger(TERMINAL_ID);
                    Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);

                    List<Future> futures = new ArrayList<>();
                    futures.add(getPackageInfoAndTracking(codes, shipmentTerminalId));
                    if (Objects.nonNull(trailerId)) {
                        futures.add(validateTrailer(scheduleRouteId, trailerId));
                    }

                    CompositeFuture.all(futures).setHandler(reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }

                            List<JsonObject> packageInfoTrackingList = reply.result().resultAt(0);
                            if (packageInfoTrackingList.isEmpty()) {
                                JsonObject wrongCode = new JsonObject()
                                        .put("CODE", "ALL_CODES")
                                        .put("TYPE", "PACKAGE")
                                        .put("CAUSE", "CODES NOT FOUND")
                                        .put(_MESSAGE, new JsonObject()
                                                .put(_ES, "Paquetes no encontrados")
                                                .put(_EN, "Codes not found"));
                                wrongCodes.add(wrongCode);
                                JsonObject response = new JsonObject()
                                        .put("packages", packageInfoTrackingList.size())
                                        .put("parcels", packageInfoTrackingList)
                                        .put("wrongCodes",wrongCodes)
                                        .put("badCodes", resCodes.getJsonArray("badCodes"))
                                        .put("codes_with_error",resCodes.getJsonArray("wrongCodes"))
                                        .put(TERMINAL_ID, shipmentTerminalId)
                                        .put(SCHEDULE_ROUTE_ID, scheduleRouteId);
                                message.reply(response);
                                return;
                            }

                            List<JsonObject> trailersList = new ArrayList<>();
                            if (Objects.nonNull(trailerId)) {
                                trailersList.addAll(reply.result().resultAt(1));
                            }

                            this.validateWrongCodesPackages(wrongCodes, packagesLogScanner, packageInfoTrackingList, shipment, trailerId, originTrailers, trailersList);

                            startTransaction(message, conn -> {
                                try {
                                    this.doInsertsAndUpdates(conn, packageInfoTrackingList, shipmentId, shipmentTerminalId, trailerId, createdBy).whenComplete((res, error) -> {
                                        try {
                                            if (error != null){
                                                throw error;
                                            }

                                            JsonObject response = new JsonObject()
                                                    .put("packages", packageInfoTrackingList.size())
                                                    .put("parcels", packageInfoTrackingList)
                                                    .put("wrongCodes",wrongCodes)
                                                    .put("badCodes", resCodes.getJsonArray("badCodes"))
                                                    .put("codes_with_error",resCodes.getJsonArray("wrongCodes"))
                                                    .put(TERMINAL_ID, shipmentTerminalId)
                                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId);

                                            this.saveLogAndCommit(conn, message, response, packagesLogScanner, createdBy);

                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t) {
                                    this.rollback(conn, t, message);
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
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void saveLogAndCommit(SQLConnection conn, Message<JsonObject> message, JsonObject response, JsonArray packagesLogScanner, Integer createdBy){
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
                .put(_ACTION, "download")
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

    private CompletableFuture<List<JsonObject>> doInsertsAndUpdates(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer shipmentId, Integer shipmentTerminalId, Integer trailerId, Integer createdBy){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            if (packageInfoTrackingList.isEmpty()) {
                future.complete(packageInfoTrackingList);
            } else {
                this.createShipmentParcelPackageTracking(conn, packageInfoTrackingList, shipmentId, trailerId, createdBy).whenComplete((resSPPT, errorSPPT) -> {
                    try {
                        if (errorSPPT != null){
                            throw errorSPPT;
                        }

                        this.execUpdatePackageStatusDownloaded(conn, packageInfoTrackingList, shipmentTerminalId, createdBy).whenComplete((resPPD, errPPD) -> {
                            try {
                                if (errPPD != null){
                                    throw errPPD;
                                }

                                this.insertPackageTrackingDownloaded(conn, packageInfoTrackingList, shipmentTerminalId, createdBy).whenComplete((resIPT, errIPT) -> {
                                    try {
                                        if (errIPT != null) {
                                            throw errIPT;
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

    private Future<List<JsonObject>> getPackageInfoAndTracking(JsonArray packageCodes, Integer shipmentTerminalId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            String packageCodeParams = packageCodes.stream()
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(", "));
            String QUERY = String.format(QUERY_GET_PACKAGE_INFO_DOWNLOAD_AND_TRACKING, shipmentTerminalId, shipmentTerminalId, packageCodeParams);
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

    private CompletableFuture<Boolean> createShipmentParcelPackageTracking(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer shipmentId, Integer trailerId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<GenericQuery> inserts = packageInfoTrackingList.stream()
                .map(p -> {
                    JsonObject body = new JsonObject()
                            .put(PARCEL_ID, p.getInteger(PARCEL_ID))
                            .put(PARCEL_PACKAGE_ID, p.getInteger(PARCEL_PACKAGE_ID))
                            .put(SHIPMENT_ID, shipmentId)
                            .put(STATUS, SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS.DOWNLOADED.getValue())
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
        return future;
    }

    private void validateWrongCodesPackages(
            JsonArray wrongCodes,
            JsonArray packagesLogScanner,
            List<JsonObject> packageInfoTrackingList,
            JsonObject shipment,
            Integer trailerId,
            boolean originTrailers,
            List<JsonObject> trailersList) {

        Integer shipmentId = shipment.getInteger(ID);
        Integer shipTerminalId = shipment.getInteger("terminal_id");
        Integer shipScheduleRouteId = shipment.getInteger("schedule_route_id");
        Integer shipmentStatus = shipment.getInteger("shipment_status");

        List<JsonObject> packageInfoTrackingListCopy = new ArrayList<>(packageInfoTrackingList);

        for (JsonObject pack : packageInfoTrackingListCopy) {
            String packageCode = pack.getString("package_code");
            PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[pack.getInteger(_PACKAGE_STATUS)];
            String shipmentType = pack.getString("shipment_type");
            Integer scheduleRouteId = pack.getInteger("schedule_route_id");
            Integer packageTerminalDestinyId = pack.getInteger("terminal_destiny_id");
            boolean receiveTranshipments = pack.getBoolean("receive_transhipments");
            boolean isTranshipment = pack.getInteger("parcel_transhipment_id") != null;
            boolean isInReplacementTerminal = pack.getInteger("is_in_replacement_terminal") == 1;
            isTranshipment = isTranshipment || (Objects.isNull(scheduleRouteId)
                    && receiveTranshipments
                    && !isInReplacementTerminal
                    && !shipTerminalId.equals(packageTerminalDestinyId));

            if (Objects.nonNull(trailerId) && trailersList.isEmpty()) {
                addWrongCode(wrongCodes, packagesLogScanner, pack, shipmentId, shipScheduleRouteId, shipTerminalId, trailerId,
                        packageCode, "TRAILER IS NOT ASSIGNED TO THE ROUTE OR WAS RELEASED OR TRANSFERRED",
                        "Paquetes no encontrados", "Trailer is not assigned to the route or was released or transferred");
                packageInfoTrackingList.remove(pack);

            } else if (!originTrailers && shipmentStatus.equals(2)) {
                addWrongCode(wrongCodes, packagesLogScanner, pack, shipmentId, shipScheduleRouteId, shipTerminalId, trailerId,
                        packageCode, "SHIPMENT IS CLOSED",
                        "El desembarque esta cerrado", "Shipment is closed");
                packageInfoTrackingList.remove(pack);

            } else if ((Objects.isNull(scheduleRouteId) && !packageStatus.canBeDownloaded()) && !packageStatus.equals(PACKAGE_STATUS.IN_TRANSIT)) {
                String cause = "THE PACKAGE MUST BE IN-TRANSIT, CURRENT STATUS: " + packageStatus.name();
                String msgEs = "El paquete deberia estar en transito, estatus actual: " + packageStatus.name();
                addWrongCode(wrongCodes, packagesLogScanner, pack, shipmentId, shipScheduleRouteId, shipTerminalId, trailerId,
                        packageCode, cause, msgEs, cause);
                packageInfoTrackingList.remove(pack);

            } else if (!originTrailers &&
                    (!Objects.equals(packageTerminalDestinyId, shipTerminalId) && !isInReplacementTerminal)
                    && !(receiveTranshipments && isTranshipment)) {
                addWrongCode(wrongCodes, packagesLogScanner, pack, shipmentId, shipScheduleRouteId, shipTerminalId, trailerId,
                        packageCode, "THE TERMINAL IS NOT THE DESTINY",
                        "La terminal no puede ser destino del paquete", "The terminal is not the destiny");
                packageInfoTrackingList.remove(pack);

            } else if (!originTrailers) {
                packagesLogScanner.add(new JsonObject()
                        .put(_PACKAGE, pack)
                        .put(_SHIPMENT_ID, shipmentId)
                        .put(_SCHEDULE_ROUTE_ID, shipScheduleRouteId)
                        .put(_BRANCHOFFICE_ID, shipTerminalId)
                        .put(_TRAILER_ID, trailerId)
                        .put(_MESSAGE, "Download success"));
            }
        }
    }

    private void addWrongCode(JsonArray wrongCodes, JsonArray packagesLogScanner, JsonObject pack,
                              Integer shipmentId, Integer shipScheduleRouteId, Integer shipTerminalId, Integer trailerId,
                              String packageCode, String cause, String msgEs, String msgEn) {
        wrongCodes.add(new JsonObject()
                .put("CODE", packageCode)
                .put("TYPE", "PACKAGE")
                .put("CAUSE", cause)
                .put(_MESSAGE, new JsonObject()
                        .put(_ES, msgEs)
                        .put(_EN, msgEn)));
        packagesLogScanner.add(new JsonObject()
                .put(_PACKAGE, pack)
                .put(_SHIPMENT_ID, shipmentId)
                .put(_SCHEDULE_ROUTE_ID, shipScheduleRouteId)
                .put(_BRANCHOFFICE_ID, shipTerminalId)
                .put(_TRAILER_ID, trailerId)
                .put(_MESSAGE, msgEn));
    }

    private CompletableFuture<Boolean> updateParcelPackagesStatus(
            SQLConnection conn, List<Integer> parcelPackagesIdList, PACKAGE_STATUS status, Integer updatedBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String parcelPackagesParams = parcelPackagesIdList.stream()
                    .map(id -> "'" + id + "'")
                    .collect(Collectors.joining(", "));

            String updates = String.format(QUERY_UPDATE_PARCELS_PACKAGES, status.ordinal(),
                    updatedBy, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), parcelPackagesParams);

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

    private CompletableFuture<Boolean> insertPackageTrackingDownloaded(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer terminalId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<GenericQuery> inserts = new ArrayList<>();
            for (JsonObject p : packageInfoTrackingList) {
                Integer packScheduleRouteId = p.getInteger(_SCHEDULE_ROUTE_ID);
                boolean isInReplacementTerminal = p.getInteger("is_in_replacement_terminal") == 1;
                Integer terminalDestinyId = p.getInteger(_TERMINAL_DESTINY_ID);
                boolean receiveTranshipments = p.getBoolean("receive_transhipments");

                inserts.add(createPackageTrackingInsert(p, isInReplacementTerminal ? terminalDestinyId : terminalId,
                        PARCELPACKAGETRACKING_STATUS.DOWNLOADED, packScheduleRouteId, createdBy));

                boolean isTranshipment = (Objects.isNull(packScheduleRouteId) && receiveTranshipments && !terminalId.equals(terminalDestinyId))
                        || (Objects.nonNull(p.getInteger("parcel_transhipment_id")) && !terminalId.equals(terminalDestinyId));

                if (isTranshipment && receiveTranshipments && !isInReplacementTerminal) {
                    inserts.add(createPackageTrackingInsert(p, terminalId,
                            PARCELPACKAGETRACKING_STATUS.READY_TO_TRANSHIPMENT, packScheduleRouteId, createdBy));
                }
            }

            List<JsonArray> insertPPTParams = inserts.stream()
                    .map(GenericQuery::getParams)
                    .collect(Collectors.toList());

            conn.batchWithParams(inserts.get(0).getQuery(), insertPPTParams, reply -> {
                if (reply.failed()) {
                    future.completeExceptionally(reply.cause());
                } else {
                    future.complete(!inserts.isEmpty());
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private GenericQuery createPackageTrackingInsert(JsonObject p, Integer terminalId, PARCELPACKAGETRACKING_STATUS action, Integer packScheduleRouteId, Integer createdBy) {
        JsonObject params = new JsonObject()
                .put(_PARCEL_ID, p.getInteger(_PARCEL_ID))
                .put(_PARCEL_PACKAGE_ID, p.getInteger(_PARCEL_PACKAGE_ID))
                .put(_TERMINAL_ID, terminalId)
                .put(_ACTION, action.getValue())
                .put(_NOTES, Objects.isNull(packScheduleRouteId) ? "Se registra sin previo embarque" : null)
                .put(CREATED_BY, createdBy);
        return this.generateGenericCreate(new ParcelsPackagesTrackingDBV().getTableName(), params);
    }

    private CompletableFuture<Boolean> execUpdatePackageStatusDownloaded(SQLConnection conn, List<JsonObject> packageInfoTrackingList, Integer terminalId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            List<CompletableFuture<Boolean>> updatePackageStatus = new ArrayList<>();
            List<Integer> packagesIdList = new ArrayList<>();
            List<Integer> packageIdTranshipmentList = new ArrayList<>();

            packageInfoTrackingList.forEach(p -> {
                boolean receiveTranshipments = p.getBoolean("receive_transhipments");
                boolean differentTerminal = !terminalId.equals(p.getInteger(_TERMINAL_DESTINY_ID));
                boolean hasTranshipmentId = Objects.nonNull(p.getInteger("parcel_transhipment_id"));
                boolean isTranshipment = differentTerminal && (hasTranshipmentId || receiveTranshipments);

                boolean isInReplacementTerminal = p.getInteger("is_in_replacement_terminal") == 1;
                Integer packageId = p.getInteger("parcel_package_id");

                if (isTranshipment && receiveTranshipments && !isInReplacementTerminal) {
                    packageIdTranshipmentList.add(packageId);
                } else {
                    packagesIdList.add(packageId);
                }
            });

            if (!packagesIdList.isEmpty()) {
                updatePackageStatus.add(this.updateParcelPackagesStatus(conn, this.distinct(packagesIdList), PACKAGE_STATUS.DOWNLOADED, createdBy));
            }
            if (!packageIdTranshipmentList.isEmpty()) {
                updatePackageStatus.add(this.updateParcelPackagesStatus(conn, this.distinct(packageIdTranshipmentList), PACKAGE_STATUS.READY_TO_TRANSHIPMENT, createdBy));
            }
            CompletableFuture.allOf(updatePackageStatus.toArray(new CompletableFuture[updatePackageStatus.size()])).whenComplete((res, err)->{
                try {
                    if (err != null) {
                        throw err;
                    }
                    future.complete(true);
                }catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private List<Integer> distinct(List<Integer> list) {
        return list.stream().distinct().collect(toList());
    }

    private static final String QUERY_GET_PACKAGE_INFO_DOWNLOAD_AND_TRACKING = "SELECT \n" +
            "   pp.id as parcel_package_id, \n" +
            "   pp.parcel_id, \n" +
            "   pp.package_status, \n" +
            "   p.terminal_destiny_id, \n" +
            "   p.terminal_origin_id,\n" +
            "   p.total_packages,\n" +
            "   srd.schedule_route_id,\n" +
            "   ptr.id AS parcel_transhipment_id, \n" +
            "   bd.receive_transhipments, \n" +
            "   (SELECT s2.shipment_status FROM shipments_parcel_package_tracking sppt2\n" +
            "       INNER JOIN shipments s2 ON s2.id = sppt2.shipment_id \n" +
            "       WHERE sppt2.parcel_package_id = pp.id ORDER BY sppt2.id DESC LIMIT 1) AS shipment_status,\n" +
            "   (SELECT s2.shipment_type FROM shipments_parcel_package_tracking sppt2\n" +
            "       INNER JOIN shipments s2 ON s2.id = sppt2.shipment_id \n" +
            "       WHERE sppt2.parcel_package_id = pp.id ORDER BY sppt2.id DESC LIMIT 1) AS shipment_type,\n" +
            "   IF((SELECT bprc.id FROM branchoffice_parcel_receiving_config bprc WHERE bprc.of_branchoffice_id = p.terminal_destiny_id AND bprc.status = 1 AND %d IN (bprc.receiving_branchoffice_id) LIMIT 1) IS NULL, FALSE, TRUE) AS is_in_replacement_terminal\n" +
            "FROM parcels_packages AS pp\n" +
            "INNER JOIN parcels AS p ON p.id = pp.parcel_id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = p.schedule_route_destination_id\n" +
            "LEFT JOIN parcels_transhipments ptr ON ptr.parcel_id = p.id AND ptr.parcel_package_id = pp.id\n" +
            "INNER JOIN branchoffice bd ON bd.id = %d\n" +
            "WHERE pp.package_code in (%s) \n" +
            "GROUP BY pp.id, pp.parcel_id, pp.package_status, p.terminal_destiny_id, p.terminal_origin_id, srd.schedule_route_id;";

    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT * FROM shipments where id = ? ;";

    private static final String  QUERY_UPDATE_PARCELS_PACKAGES = "UPDATE parcels_packages SET package_status = %d, updated_by = %d, updated_at = '%s' WHERE id IN (%s);";

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

}