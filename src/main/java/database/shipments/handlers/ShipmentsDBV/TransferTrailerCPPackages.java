package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import database.shipments.handlers.ShipmentsDBV.models.Shipment;
import database.vechicle.models.Trailer;
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

import static service.commons.Constants.*;

public class TransferTrailerCPPackages extends DBHandler<ShipmentsDBV> {

    public TransferTrailerCPPackages(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer fromTrailerId = body.getInteger(_FROM_TRAILER_ID);
        Integer toTrailerId = body.getInteger(_TO_TRAILER_ID);
        Integer shipmentId = body.getInteger(_SHIPMENT_ID);
        String parcelTrackingCode = body.getString(_PARCEL_TRACKING_CODE);
        JsonArray packageCodes = body.getJsonArray(_PACKAGE_CODES);
        Integer createdBy = body.getInteger(CREATED_BY);

        validateShipment(shipmentId).whenComplete((shipment, errVS) -> {
            try {
                if (errVS != null) {
                    throw errVS;
                }
                Integer scheduleRouteId = shipment.getScheduleRouteId();

                Future<Trailer> f1 = Future.future();
                Future<Trailer> f2 = Future.future();
                validateTrailer(scheduleRouteId, fromTrailerId).setHandler(f1.completer());
                validateTrailer(scheduleRouteId, toTrailerId).setHandler(f2.completer());

                CompositeFuture.all(f1, f2).setHandler(processTrailerValidations -> {
                    try {
                        if (processTrailerValidations.failed()) {
                            throw processTrailerValidations.cause();
                        }

                        Trailer fromTrailer = processTrailerValidations.result().resultAt(0);

                        if(Objects.nonNull(parcelTrackingCode)) {
                            transferParcel(message, shipment, fromTrailer, toTrailerId, parcelTrackingCode, createdBy);
                        } else {
                            transferPackages(message, shipment, fromTrailer, toTrailerId, packageCodes, createdBy);
                        }

                    } catch (Throwable t) {
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable t) {
                reportQueryError(message, t);
            }
        });
    }

    private void transferParcel(Message<JsonObject> message, Shipment shipment, Trailer fromTrailer, Integer toTrailerId, String trackingCode, Integer createdBy) {
        try {
            getParcelInfo(trackingCode, shipment.getId()).whenComplete((parcel, PIError) -> {
                try {
                    if ((PIError != null)) {
                        throw PIError;
                    }
                    Integer parcelId = parcel.getInteger(ID);
                    Integer shipmentParcelId = parcel.getInteger(_SHIPMENT_PARCEL_ID);
                    validateParcelInfo(fromTrailer, parcel);

                    getPackagesInfo(shipment.getId(), parcelId).whenComplete((packages, errPPI) -> {
                        try {
                            if (errPPI != null) {
                                throw errPPI;
                            }
                            validatePackagesInfo(fromTrailer, packages);

                            startTransaction(message, conn -> {
                                try {
                                    List<Future> futures = new ArrayList<>();
                                    futures.add(updateShipmentParcel(conn, shipmentParcelId, toTrailerId, createdBy));
                                    if (!packages.isEmpty()) {
                                        futures.add(updateShipmentsParcelsPackageTracking(conn, shipment.getId(), parcelId, packages, toTrailerId, createdBy));
                                        futures.add(insertScannerTracking(conn, shipment, parcelId, packages, fromTrailer, toTrailerId, createdBy));
                                    }
                                    CompositeFuture.all(futures).setHandler(processReply -> {
                                        try {
                                            if (processReply.failed()) {
                                                throw processReply.cause();
                                            }
                                            this.commit(conn, message, new JsonObject().put("transfered", true));
                                        } catch (Throwable t) {
                                            rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t) {
                                    rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t) {
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void transferPackages(Message<JsonObject> message, Shipment shipment, Trailer fromTrailer, Integer toTrailerId, JsonArray packageCodes, Integer createdBy) {
        try {
            getParcelsInfoByPackages(shipment.getId(), packageCodes).whenComplete((groupedPackages, PIBPError) -> {
                try {
                    if (PIBPError != null) {
                        throw PIBPError;
                    }
                    groupedPackages.forEach((parcelId, packages) -> {
                        try {
                            validatePackagesInfo(fromTrailer, packages);
                            startTransaction(message, conn -> {
                                try {
                                    List<Future> futures = new ArrayList<>();
                                    futures.add(updateShipmentsParcelsPackageTracking(conn, shipment.getId(), parcelId, packages, toTrailerId, createdBy));
                                    futures.add(insertScannerTracking(conn, shipment, parcelId, packages, fromTrailer, toTrailerId, createdBy));
                                    CompositeFuture.all(futures).setHandler(processReply -> {
                                        try {
                                            if (processReply.failed()) {
                                                throw processReply.cause();
                                            }
                                            this.commit(conn, message, new JsonObject().put("transfered", true));
                                        } catch (Throwable t) {
                                            rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t) {
                                    rollback(conn, t, message);
                                }
                            });

                        } catch (Throwable t) {
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<Map<Integer, List<JsonObject>>> getParcelsInfoByPackages(Integer shipmentId, JsonArray packages) {
        CompletableFuture<Map<Integer, List<JsonObject>>> future = new CompletableFuture<>();
        try {
            String paramPackages = packages.stream()
                    .map(packageCode -> "'" + packageCode + "'")
                    .collect(Collectors.joining(", "));
            String query = String.format(GET_PARCEL_INFO_BY_PACKAGE_CODES, paramPackages);
            this.dbClient.queryWithParams(query, new JsonArray().add(shipmentId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> packagesInfo = reply.result().getRows();
                    if (packagesInfo.isEmpty()) {
                        throw new Exception("Parcel info not found");
                    }

                    Map<Integer, List<JsonObject>> groupedPackages =
                            packagesInfo.stream().collect(Collectors.groupingBy(w -> w.getInteger(ID)));
                    future.complete(groupedPackages);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void validateParcelInfo(Trailer trailer, JsonObject parcel) throws Exception {
        Integer parcelShipmentId = parcel.getInteger(_SHIPMENT_ID);
        Integer parcelTrailerId = parcel.getInteger(_TRAILER_ID);
        if(Objects.isNull(parcelShipmentId)) {
            throw new Exception("Parcel not found in shipment");
        }
        if(Objects.isNull(parcelTrailerId) || !trailer.getId().equals(parcelTrailerId)) {
            throw new Exception("Parcel not found in this trailer");
        }
    }

    private void validatePackagesInfo(Trailer trailer, List<JsonObject> packages) throws Exception {
        for (JsonObject pack : packages) {
            String packageCode = pack.getString(_PACKAGE_CODE);
            Integer packTrailerId = pack.getInteger(_TRAILER_ID);
            if(Objects.isNull(packTrailerId) || !trailer.getId().equals(packTrailerId)) {
                throw new Exception("Package code not found in this trailer: " + packageCode);
            }
        }
    }

    private CompletableFuture<JsonObject> getParcelInfo(String trackingCode, Integer shipmentId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(shipmentId).add(trackingCode);
            this.dbClient.queryWithParams(GET_PARCEL_INFO_BY_TRACKING_CODE, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> parcels = reply.result().getRows();
                    if (parcels.isEmpty()) {
                        throw new Exception("Parcel not found");
                    }

                    JsonObject parcel = parcels.get(0);
                    future.complete(parcel);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getPackagesInfo(Integer shipmentId, Integer parcelId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(parcelId).add(shipmentId);
            this.dbClient.queryWithParams(GET_SHIPMENT_PACKAGES_INFO_BY_PARCEL, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(reply.result().getRows());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private Future<Boolean> insertScannerTracking(SQLConnection conn, Shipment shipment, Integer parcelId, List<JsonObject> packages,
                                                  Trailer fromTrailer, Integer toTrailerId, Integer updatedBy) {
        Future<Boolean> future = Future.future();
        try {
            if (packages.isEmpty()) {
                future.complete(true);
            } else {
                List<GenericQuery> inserts = new ArrayList<>();
                for (JsonObject pack : packages) {
                    inserts.add(this.generateGenericCreate("parcels_packages_scanner_tracking", new JsonObject()
                            .put(_PARCEL_ID, parcelId)
                            .put(_PARCEL_PACKAGE_ID, pack.getInteger(ID))
                            .put(_SHIPMENT_ID, shipment.getId())
                            .put(_SCHEDULE_ROUTE_ID, shipment.getScheduleRouteId())
                            .put(_BRANCHOFFICE_ID, shipment.getTerminalId())
                            .put(_TRAILER_ID, toTrailerId)
                            .put(_MESSAGE, "The package is changed from trailer - " + fromTrailer.getEconomicNumber())
                            .put(ACTION, "load")
                            .put(CREATED_BY, updatedBy)));
                }
                List<JsonArray> params = inserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());

                conn.batchWithParams(inserts.get(0).getQuery(), params, reply -> {
                    try {
                        if(reply.failed()) {
                            throw reply.cause();
                        }
                        future.complete(true);
                    } catch (Throwable t) {
                        future.fail(t);
                    }
                });
            }
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<Boolean> updateShipmentParcel(SQLConnection conn, Integer shipmentParcelId, Integer toTrailerId, Integer updatedBy) {
        Future<Boolean> future = Future.future();
        try {
            GenericQuery update = this.generateGenericUpdate("shipments_parcels", new JsonObject()
                    .put(ID, shipmentParcelId)
                    .put(_TRAILER_ID, toTrailerId)
                    .put(UPDATED_BY, updatedBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date())));
            conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<Boolean> updateShipmentsParcelsPackageTracking(SQLConnection conn, Integer shipmentId, Integer parcelId, List<JsonObject> packages, Integer toTrailerId, Integer updatedBy) {
        Future<Boolean> future = Future.future();
        try {
            if (packages.isEmpty()) {
                future.complete(true);
            } else {
                List<JsonArray> params = new ArrayList<>();
                for (JsonObject pack : packages) {
                    params.add(new JsonArray()
                            .add(toTrailerId).add(updatedBy).add(UtilsDate.sdfDataBase(new Date()))
                            .add(shipmentId).add(parcelId).add(pack.getInteger(_PARCEL_PACKAGE_ID)));
                }

                conn.batchWithParams(QUERY_UPDATE_SHIPMENTS_PARCEL_PACKAGE_TRACKING_TRAILER_ID, params, reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        future.complete(true);
                    } catch (Throwable t) {
                        future.fail(t);
                    }
                });
            }
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private CompletableFuture<Shipment> validateShipment(Integer shipmentId) {
        CompletableFuture<Shipment> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_GET_SHIPMENT_INFO, new JsonArray().add(shipmentId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Shipment not found");
                    }

                    Shipment shipment = result.get(0).mapTo(Shipment.class);
                    if(SHIPMENT_STATUS.CLOSE.equals(shipment.getShipmentStatus())) {
                        throw new Exception("Shipment is closed");
                    }

                    future.complete(shipment);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private Future<Trailer> validateTrailer(Integer scheduleRouteId, Integer trailerId) {
        Future<Trailer> future = Future.future();
        try {
            this.dbClient.queryWithParams(QUERY_VALIDATE_SHIPMENT_TRAILER, new JsonArray().add(scheduleRouteId).add(trailerId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> trailers = reply.result().getRows();
                    if (trailers.isEmpty()) {
                        throw new Exception("Trailer is not assigned to the route or was released or transferred");
                    }
                    Trailer trailer = trailers.get(0).mapTo(Trailer.class);
                    future.complete(trailer);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT * FROM shipments where id = ? ;";

    private static final String QUERY_VALIDATE_SHIPMENT_TRAILER = "SELECT tr.* FROM shipments_trailers st\n" +
            "INNER JOIN trailers tr ON tr.id = st.trailer_id\n" +
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
            "       SELECT st4.trailer_id FROM shipments_trailers st4 \n" +
            "        WHERE st4.schedule_route_id = st3.schedule_route_id \n" +
            "        AND st4.action = 'assign' AND st4.latest_movement IS TRUE)\n" +
            "));";

    private static final String GET_PARCEL_INFO_BY_TRACKING_CODE = "SELECT\n" +
            "   p.id,\n" +
            "   p.parcel_tracking_code,\n" +
            "   sp.id AS shipment_parcel_id,\n" +
            "   sp.shipment_id,\n" +
            "   sp.trailer_id\n" +
            "FROM parcels p\n" +
            "INNER JOIN shipments_parcels sp ON sp.parcel_id = p.id\n" +
            "   AND sp.shipment_id = ?\n" +
            "WHERE p.parcel_tracking_code = ?\n" +
            "GROUP BY p.id;";

    private static final String GET_SHIPMENT_PACKAGES_INFO_BY_PARCEL = "SELECT \n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.package_code,\n" +
            "   sppt.trailer_id\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_id = p.id\n" +
            "   AND sppt.parcel_package_id = pp.id\n" +
            "WHERE p.id = ?\n" +
            "   AND sppt.id = (SELECT sppt2.id FROM shipments_parcel_package_tracking sppt2 \n" +
            "                   WHERE sppt2.shipment_id = ? \n" +
            "                       AND sppt2.status IN ('loaded', 'transfer')\n" +
            "                       AND sppt2.parcel_id = p.id\n" +
            "                       AND sppt2.parcel_package_id = pp.id\n" +
            "                    ORDER BY sppt2.id DESC LIMIT 1) \n" +
            "GROUP BY pp.id;";

    private static final String QUERY_UPDATE_SHIPMENTS_PARCEL_PACKAGE_TRACKING_TRAILER_ID = "UPDATE shipments_parcel_package_tracking\n" +
            "SET trailer_id = ?, updated_by = ?, updated_at = ?\n" +
            "WHERE shipment_id = ? AND parcel_id = ? AND parcel_package_id = ?\n" +
            "AND status IN ('loaded', 'transfer') AND latest_movement IS TRUE;";

    private static final String GET_PARCEL_INFO_BY_PACKAGE_CODES = "SELECT\n" +
            "   p.id,\n" +
            "   pp.package_code,\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   sppt.shipment_id,\n" +
            "   sppt.trailer_id\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_id = p.id\n" +
            "   AND sppt.parcel_package_id = pp.id AND sppt.shipment_id = ?\n" +
            "WHERE pp.package_code IN (%s)\n" +
            "GROUP BY pp.id;";
}
