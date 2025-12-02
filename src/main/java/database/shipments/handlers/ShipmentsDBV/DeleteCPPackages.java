package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
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

public class DeleteCPPackages extends DBHandler<ShipmentsDBV> {

    public DeleteCPPackages(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer trailerId = body.getInteger(_TRAILER_ID);
            Integer shipmentId = body.getInteger(_SHIPMENT_ID);
            String parcelTrackingCode = body.getString(_PARCEL_TRACKING_CODE);
            JsonArray packageCodes = body.getJsonArray(_PACKAGE_CODES);
            Integer createdBy = body.getInteger(CREATED_BY);

            validateShipment(shipmentId).whenComplete((shipment, errVS) -> {
                try {
                    if (errVS != null) {
                        throw errVS;
                    }
                    Integer scheduleRouteId = shipment.getInteger(_SCHEDULE_ROUTE_ID);
                    Integer terminalId = shipment.getInteger(_TERMINAL_ID);
                    validateTrailer(scheduleRouteId, trailerId).whenComplete((trailer, errVT) -> {
                        try {
                            if (errVT != null) {
                                throw errVT;
                            }
                            String trailerEconomicNumber = Objects.nonNull(trailer) ? trailer.getString(_ECONOMIC_NUMBER) : null;
                            if(Objects.nonNull(parcelTrackingCode)) {
                                deleteParcel(message, shipmentId, terminalId, trailerId, trailerEconomicNumber, parcelTrackingCode, createdBy);
                            } else {
                                deletePackages(message, shipmentId, terminalId, trailerId, trailerEconomicNumber, packageCodes, createdBy);
                            }
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

    private void deleteParcel(Message<JsonObject> message, Integer shipmentId, Integer terminalId, Integer trailerId, String trailerEconomicNumber, String trackingCode, Integer createdBy) {
        getParcelInfo(trackingCode, shipmentId).whenComplete((parcels, PIError) -> {
            try {
                if ((PIError != null)) {
                    throw PIError;
                }

                JsonObject parcel = validateParcelInfo(trailerId, parcels);
                Integer parcelId = parcel.getInteger(ID);
                Integer shipmentParcelId = parcel.getInteger(_SHIPMENT_PARCEL_ID);
                PARCEL_STATUS lastParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_LAST_PARCEL_STATUS)];
                Integer transhipmentCount = parcel.getInteger(_TRANSHIPMENT_COUNT);

                getPackagesInfo(shipmentId, parcelId, trailerId).whenComplete((packages, errPPI) -> {
                    try {
                        if (errPPI != null) {
                            throw errPPI;
                        }
                        validatePackagesInfo(trailerId, packages);

                        startTransaction(message, conn -> {
                            try {
                                List<Future> futures = new ArrayList<>();
                                futures.add(deleteShipmentParcel(conn, shipmentParcelId));
                                futures.add(updateParcelStatus(conn, parcelId, lastParcelStatus, createdBy));
                                if (Objects.nonNull(transhipmentCount)) {
                                    futures.add(deleteParcelTranshipment(conn, shipmentId, parcelId, null, transhipmentCount, createdBy));
                                    futures.add(deleteParcelTranshipmentHistory(conn, shipmentId, parcelId, null));
                                }
                                if (!packages.isEmpty()) {
                                    futures.add(deleteParcelsPackageTracking(conn, parcelId, packages, terminalId, trailerEconomicNumber, createdBy));
                                    futures.add(deleteShipmentsParcelsPackageTracking(conn, shipmentId, parcelId, packages));
                                    futures.add(updatePackagesStatus(conn, packages, createdBy));
                                }
                                CompositeFuture.all(futures).setHandler(processReply -> {
                                    try {
                                        if (processReply.failed()) {
                                            throw processReply.cause();
                                        }
                                        commit(conn, message, new JsonObject().put("deleted", true));
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
    }

    private JsonObject validateParcelInfo(Integer trailerId, List<JsonObject> parcels) throws Exception {
        JsonObject firstParcel = parcels.get(0);
        Integer parcelShipmentId = firstParcel.getInteger(_SHIPMENT_ID);
        Integer parcelTrailerId = firstParcel.getInteger(_TRAILER_ID);

        Integer secondParcelTrailerId = null;
        JsonObject secondParcel = null;
        if (parcels.size() > 1) {
            secondParcel = parcels.get(1);
            secondParcelTrailerId = secondParcel.getInteger(_TRAILER_ID);
        }

        if (Objects.isNull(parcelShipmentId)) {
            throw new Exception("Parcel not found in shipment");
        }

        if (Objects.nonNull(trailerId)) {
            boolean firstMatch = Objects.nonNull(parcelTrailerId) && trailerId.equals(parcelTrailerId);
            boolean secondMatch = Objects.nonNull(secondParcelTrailerId) && trailerId.equals(secondParcelTrailerId);

            if (firstMatch) {
                return firstParcel;
            } else if (secondMatch) {
                return secondParcel;
            } else {
                throw new Exception("Parcel not found in this trailer");
            }
        } else {
            return firstParcel;
        }
    }

    private void validatePackagesInfo(Integer trailerId, List<JsonObject> packages) throws Exception {
        for (JsonObject pack : packages) {
            String packageCode = pack.getString(_PACKAGE_CODE);
            Integer packTrailerId = pack.getInteger(_TRAILER_ID);
            if(Objects.nonNull(trailerId) && (Objects.isNull(packTrailerId) || !trailerId.equals(packTrailerId))) {
                throw new Exception("Package code not found in this trailer: " + packageCode);
            }
        }
    }

    private CompletableFuture<List<JsonObject>> getParcelInfo(String trackingCode, Integer shipmentId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(shipmentId).add(shipmentId).add(trackingCode);
            this.dbClient.queryWithParams(GET_PARCEL_INFO_BY_TRACKING_CODE, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> parcels = reply.result().getRows();
                    if (parcels.isEmpty()) {
                        throw new Exception("Parcel not found");
                    }

                    future.complete(parcels);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getPackagesInfo(Integer shipmentId, Integer parcelId, Integer trailerId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String QUERY = GET_SHIPMENT_PACKAGES_INFO_BY_PARCEL;
            JsonArray params = new JsonArray()
                    .add(shipmentId).add(parcelId).add(shipmentId);
            if (Objects.nonNull(trailerId)) {
                QUERY += " AND sppt.trailer_id = ? \n";
                params.add(trailerId);
            }
            QUERY += " GROUP BY pp.id;";
            this.dbClient.queryWithParams(QUERY, params, reply -> {
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

    private Future<Boolean> updateParcelStatus(SQLConnection conn, Integer parcelId, PARCEL_STATUS parcelStatus, Integer updatedBy) {
        Future<Boolean> future = Future.future();
        try {
            if (parcelStatus.equals(PARCEL_STATUS.CANCELED)) {
                future.complete(true);
            } else {
                GenericQuery update = this.generateGenericUpdate("parcels", new JsonObject()
                        .put(ID, parcelId)
                        .put(_PARCEL_STATUS, parcelStatus.ordinal())
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
            }
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<Boolean> updatePackagesStatus(SQLConnection conn, List<JsonObject> packages, Integer updatedBy) {
        Future<Boolean> future = Future.future();
        try {
            if (packages.isEmpty()) {
                future.complete(true);
            } else {
                List<GenericQuery> updates = new ArrayList<>();
                for (JsonObject pack : packages) {
                    PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[pack.getInteger(_PACKAGE_STATUS)];
                    if (!packageStatus.equals(PACKAGE_STATUS.CANCELED)) {
                        updates.add(this.generateGenericUpdate("parcels_packages", new JsonObject()
                                .put(ID, pack.getInteger(_PARCEL_PACKAGE_ID))
                                .put(_PACKAGE_STATUS, pack.getInteger(_LAST_PACKAGE_STATUS))
                                .put(UPDATED_BY, updatedBy)
                                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))));
                    }
                }

                if (updates.isEmpty()) {
                    future.complete(true);
                } else {
                    List<JsonArray> params = updates.stream().map(GenericQuery::getParams).collect(Collectors.toList());
                    conn.batchWithParams(updates.get(0).getQuery(), params, reply -> {
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
            }
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<Boolean> deleteShipmentParcel(SQLConnection conn, Integer shipmentParcelId) {
        Future<Boolean> future = Future.future();
        try {
            GenericQuery delete = this.generateGenericDelete("shipments_parcels", new JsonObject()
                    .put(ID, shipmentParcelId));
            conn.updateWithParams(delete.getQuery(), delete.getParams(), reply -> {
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

    private Future<Boolean> deleteParcelTranshipment(SQLConnection conn, Integer shipmentId, Integer parcelId, List<JsonObject> packages, Integer transhipmentCount, Integer updatedBy) {
        Future<Boolean> future = Future.future();
        try {
            String query;
            JsonArray params;
            if (transhipmentCount == 1) {
                query = QUERY_DELETE_PARCELS_TRANSHIPMENT_BY_PARCEL_ID;
                params = new JsonArray().add(parcelId).add(shipmentId);
                if(Objects.nonNull(packages) && !packages.isEmpty()) {
                    String paramPackages = packages.stream()
                            .map(pack -> pack.getInteger(_PARCEL_PACKAGE_ID).toString())
                            .collect(Collectors.joining(", "));
                    query = query + String.format(" AND pt.parcel_package_id IN (%s);", paramPackages);
                }
            } else {
                query = QUERY_UPDATE_COUNT_PARCELS_TRANSHIPMENT_BY_PARCEL_ID;
                params = new JsonArray().add(updatedBy).add(UtilsDate.sdfDataBase(new Date())).add(parcelId).add(shipmentId);
                if(Objects.nonNull(packages) && !packages.isEmpty()) {
                    String paramPackages = packages.stream()
                            .map(pack -> pack.getInteger(_PARCEL_PACKAGE_ID).toString())
                            .collect(Collectors.joining(", "));
                    query = query + String.format("AND pt.parcel_package_id IN (%s);", paramPackages);
                }
            }

            conn.updateWithParams(query, params, reply -> {
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

    private Future<Boolean> deleteParcelTranshipmentHistory(SQLConnection conn, Integer shipmentId, Integer parcelId, List<JsonObject> packages) {
        Future<Boolean> future = Future.future();
        String query = QUERY_DELETE_PARCELS_TRANSHIPMENT_HISTORY_BY_PARCEL_AND_SHIPMENT;
        JsonArray params = new JsonArray()
                .add(parcelId).add(shipmentId);

        if(Objects.nonNull(packages) && !packages.isEmpty()) {
            String paramPackages = packages.stream()
                    .map(pack -> "'" + pack.getInteger(_PARCEL_PACKAGE_ID) + "'")
                    .collect(Collectors.joining(", "));
            query = query + String.format("AND pth.parcel_package_id IN (%s);", paramPackages);
        }

        try {
            conn.updateWithParams(query, params, reply -> {
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

    private Future<Boolean> deleteParcelsPackageTracking(SQLConnection conn, Integer parcelId, List<JsonObject> packages, Integer terminalId, String trailerEconomicNumber, Integer createdBy) {
        Future<Boolean> future = Future.future();
        try {
            if (packages.isEmpty()) {
                future.complete(true);
            } else {
                List<GenericQuery> inserts = new ArrayList<>();
                for (JsonObject pack : packages) {
                    inserts.add(this.generateGenericCreate("parcels_packages_tracking", new JsonObject()
                            .put(_PARCEL_ID, parcelId)
                            .put(_PARCEL_PACKAGE_ID, pack.getInteger(_PARCEL_PACKAGE_ID))
                            .put(_TERMINAL_ID, terminalId)
                            .put(ACTION, PARCELPACKAGETRACKING_STATUS.DELETED.getValue())
                            .put(NOTES, "Se baja paquete" + (Objects.nonNull(trailerEconomicNumber) ? (" del remolque " + trailerEconomicNumber) : ""))
                            .put(CREATED_BY, createdBy)));
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

    private Future<Boolean> deleteShipmentsParcelsPackageTracking(SQLConnection conn, Integer shipmentId, Integer parcelId, List<JsonObject> packages) {
        Future<Boolean> future = Future.future();
        try {
            if (packages.isEmpty()) {
                future.complete(true);
            } else {
                List<GenericQuery> deletes = new ArrayList<>();
                for (JsonObject pack : packages) {
                    deletes.add(this.generateGenericDelete("shipments_parcel_package_tracking", new JsonObject()
                            .put(_SHIPMENT_ID, shipmentId)
                            .put(_PARCEL_ID, parcelId)
                            .put(_PARCEL_PACKAGE_ID, pack.getInteger(_PARCEL_PACKAGE_ID))));
                }
                List<JsonArray> params = deletes.stream().map(GenericQuery::getParams).collect(Collectors.toList());

                conn.batchWithParams(deletes.get(0).getQuery(), params, reply -> {
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

    private void deletePackages(Message<JsonObject> message, Integer shipmentId, Integer terminalId, Integer trailerId, String trailerEconomicNumber, JsonArray packageCodes, Integer createdBy) {
        getParcelsInfoByPackages(shipmentId, packageCodes).whenComplete((groupedPackages, PIBPError) -> {
            try {
                if (PIBPError != null) {
                    throw PIBPError;
                }
                groupedPackages.forEach((trackingCode, packages) -> {
                    try {
                        getParcelInfo(trackingCode, shipmentId).whenComplete((parcels, PIError) -> {
                            try {
                                if ((PIError != null)) {
                                    throw PIError;
                                }
                                JsonObject parcel = validateParcelInfo(trailerId, parcels);
                                Integer parcelId = parcel.getInteger(ID);
                                Integer transhipmentCount = parcel.getInteger(_TRANSHIPMENT_COUNT);

                                startTransaction(message, conn -> {
                                    try {
                                        List<Future> futures = new ArrayList<>();
                                        if (Objects.nonNull(transhipmentCount)) {
                                            futures.add(deleteParcelTranshipment(conn, shipmentId, parcelId, packages, transhipmentCount, createdBy));
                                            futures.add(deleteParcelTranshipmentHistory(conn, shipmentId, parcelId, packages));
                                        }
                                        futures.add(deleteParcelsPackageTracking(conn, parcelId, packages, terminalId, trailerEconomicNumber, createdBy));
                                        futures.add(deleteShipmentsParcelsPackageTracking(conn, shipmentId, parcelId, packages));
                                        futures.add(updatePackagesStatus(conn, packages, createdBy));

                                        CompositeFuture.all(futures).setHandler(processReply -> {
                                            try {
                                                if (processReply.failed()) {
                                                    throw processReply.cause();
                                                }
                                                commit(conn, message, new JsonObject().put("deleted", true));
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
        });
    }

    private CompletableFuture<Map<String, List<JsonObject>>> getParcelsInfoByPackages(Integer shipmentId, JsonArray packages) {
        CompletableFuture<Map<String, List<JsonObject>>> future = new CompletableFuture<>();
        try {
            String paramPackages = packages.stream()
                    .map(packageCode -> "'" + packageCode + "'")
                    .collect(Collectors.joining(", "));
            String query = String.format(GET_PARCEL_INFO_BY_PACKAGE_CODES, paramPackages);
            this.dbClient.queryWithParams(query, new JsonArray().add(shipmentId).add(shipmentId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> packagesInfo = reply.result().getRows();
                    if (packagesInfo.isEmpty()) {
                        throw new Exception("Parcel info not found");
                    }

                    Map<String, List<JsonObject>> groupedPackages =
                            packagesInfo.stream().collect(Collectors.groupingBy(w -> w.getString(_PARCEL_TRACKING_CODE)));
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

    private CompletableFuture<JsonObject> validateShipment(Integer shipmentId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
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

                    JsonObject shipment = result.get(0);
                    SHIPMENT_STATUS shipmentStatus = SHIPMENT_STATUS.fromValue(shipment.getInteger(_SHIPMENT_STATUS));
                    if(SHIPMENT_STATUS.CLOSE.equals(shipmentStatus)) {
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

    private CompletableFuture<JsonObject> validateTrailer(Integer scheduleRouteId, Integer trailerId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (Objects.isNull(trailerId)) {
                future.complete(null);
            } else {
                this.dbClient.queryWithParams(QUERY_VALIDATE_SHIPMENT_TRAILER, new JsonArray().add(scheduleRouteId).add(trailerId), reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        List<JsonObject> trailers = reply.result().getRows();
                        if (trailers.isEmpty()) {
                            throw new Exception("Trailer is not assigned to the route or was released or transferred");
                        }
                        future.complete(trailers.get(0));
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT * FROM shipments where id = ? ;";

    private static final String QUERY_VALIDATE_SHIPMENT_TRAILER = "SELECT st.*, tr.economic_number FROM shipments_trailers st\n" +
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
            "   sp.trailer_id,\n" +
            "   tr.count AS transhipment_count,\n" +
            "   IF(tr.id IS NULL, 0, IF(tr.count = 1, 0, 1)) AS last_parcel_status\n" +
            "FROM parcels p\n" +
            "INNER JOIN shipments_parcels sp ON sp.parcel_id = p.id\n" +
            "   AND sp.shipment_id = ?\n" +
            "LEFT JOIN (SELECT pth.id, pt.count, pt.parcel_id FROM parcels_transhipments_history pth \n" +
            "   INNER JOIN parcels_transhipments pt ON pt.parcel_id = pth.parcel_id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.id = pth.schedule_route_destination_id\n" +
            "   INNER JOIN shipments ship ON ship.schedule_route_id = srd.schedule_route_id\n" +
            "   WHERE ship.id = ?) tr ON tr.parcel_id = p.id\n" +
            "WHERE p.parcel_tracking_code = ?\n";
//            "GROUP BY p.id;";

    private static final String GET_PARCEL_INFO_BY_PACKAGE_CODES = "SELECT\n" +
            "   p.id,\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.package_status,\n" +
            "   p.parcel_tracking_code,\n" +
            "   sp.id AS shipment_parcel_id,\n" +
            "   sp.shipment_id,\n" +
            "   sp.trailer_id,\n" +
            "   tr.count AS transhipment_count,\n" +
            "   sp.trailer_id,\n" +
            "   IF(tr.id IS NULL, 0, IF(tr.count = 1, 0, 1)) AS last_parcel_status,\n" +
            "   IF(tr.id IS NULL, 0, IF(tr.count = 1, 0, 10)) AS last_package_status\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN shipments_parcels sp ON sp.parcel_id = p.id\n" +
            "   AND sp.shipment_id = ?\n" +
            "LEFT JOIN (SELECT pth.id, pt.count, pt.parcel_id, pt.parcel_package_id FROM parcels_transhipments_history pth \n" +
            "   INNER JOIN parcels_transhipments pt ON pt.parcel_id = pth.parcel_id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.id = pth.schedule_route_destination_id\n" +
            "   INNER JOIN shipments ship ON ship.schedule_route_id = srd.schedule_route_id\n" +
            "   WHERE ship.id = ?) tr ON tr.parcel_id = p.id AND tr.parcel_package_id = pp.id\n" +
            "WHERE pp.package_code IN (%s)\n" +
            "GROUP BY p.id;";

    private static final String GET_SHIPMENT_PACKAGES_INFO_BY_PARCEL = "SELECT \n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.package_status,\n" +
            "   pp.package_code,\n" +
            "   sppt.trailer_id,\n" +
            "   IF(tr.id IS NULL, 0, IF(tr.count = 1, 0, 10)) AS last_package_status\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_id = p.id\n" +
            "   AND sppt.parcel_package_id = pp.id\n" +
            "LEFT JOIN (SELECT pth.id, pt.count, pt.parcel_id, pt.parcel_package_id FROM parcels_transhipments_history pth \n" +
            "   INNER JOIN parcels_transhipments pt ON pt.parcel_id = pth.parcel_id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.id = pth.schedule_route_destination_id\n" +
            "   INNER JOIN shipments ship ON ship.schedule_route_id = srd.schedule_route_id\n" +
            "   WHERE ship.id = ?) tr ON tr.parcel_id = p.id AND tr.parcel_package_id = pp.id\n" +
            "WHERE p.id = ?\n" +
            "   AND sppt.id = (SELECT sppt2.id FROM shipments_parcel_package_tracking sppt2 \n" +
            "                   WHERE sppt2.shipment_id = ? \n" +
            "                       AND sppt2.status IN ('loaded', 'transfer')\n" +
            "                       AND sppt2.parcel_id = p.id\n" +
            "                       AND sppt2.parcel_package_id = pp.id\n" +
            "                    ORDER BY sppt2.id DESC LIMIT 1) \n";

    private static final String QUERY_DELETE_PARCELS_TRANSHIPMENT_BY_PARCEL_ID = "DELETE pt FROM parcels_transhipments pt\n" +
            "INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = pt.parcel_id AND pth.parcel_package_id = pt.parcel_package_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = pth.schedule_route_destination_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN shipments s ON s.schedule_route_id = sr.id\n" +
            "WHERE pt.parcel_id = ?\n" +
            "   AND s.id = ?\n";

    private static final String QUERY_UPDATE_COUNT_PARCELS_TRANSHIPMENT_BY_PARCEL_ID = "UPDATE parcels_transhipments pt\n" +
            "INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = pt.parcel_id AND pth.parcel_package_id = pt.parcel_package_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = pth.schedule_route_destination_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN shipments s ON s.schedule_route_id = sr.id\n" +
            "SET pt.count = (pt.count - 1), pt.updated_by = ?, pt.updated_at = ?\n" +
            "WHERE pt.parcel_id = ?\n" +
            "  AND s.id = ? \n";

    private static final String QUERY_DELETE_PARCELS_TRANSHIPMENT_HISTORY_BY_PARCEL_AND_SHIPMENT = "DELETE pth FROM parcels_transhipments_history pth\n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = pth.schedule_route_destination_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN shipments s ON s.schedule_route_id = sr.id\n" +
            "WHERE pth.parcel_id = ?\n" +
            "   AND s.id = ?\n";
}
