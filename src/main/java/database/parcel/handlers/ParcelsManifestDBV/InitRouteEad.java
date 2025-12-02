package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class InitRouteEad extends DBHandler<ParcelsManifestDBV> {

    public InitRouteEad(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            Double latitude = body.getDouble(_LATITUDE);
            Double longitude = body.getDouble(_LONGITUDE);
            Integer createdBy = body.getInteger(CREATED_BY);

            Future<JsonObject> f1 = Future.future();
            Future<List<JsonObject>> f2 = Future.future();
            getManifestInfo(parcelManifestId, branchofficeId, createdBy).setHandler(f1.completer());
            getPackagesInfoByManifest(parcelManifestId).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> packagesInfo = reply.result().resultAt(1);
                    startTransaction(message, conn -> {
                        try {
                            List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                            tasks.add(updateParcelManifest(conn, parcelManifestId, createdBy));
                            tasks.add(updateParcelsStatus(conn, packagesInfo, createdBy));
                            tasks.add(registerPackageTracking(conn, packagesInfo, parcelManifestId, branchofficeId, createdBy));
                            if(Objects.nonNull(latitude) && Objects.nonNull(longitude)) {
                                tasks.add(registerParcelManifestRouteLog(conn, parcelManifestId, latitude, longitude, createdBy));
                            }

                            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                                try {
                                    if (err != null) {
                                        throw err;
                                    }
                                    this.commit(conn, message, new JsonObject());
                                } catch (Throwable t) {
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
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private Future<List<JsonObject>> getPackagesInfoByManifest(int parcelManifestId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(parcelManifestId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGES_INFO_BY_MANIFEST, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> packages = reply.result().getRows();
                    if (packages.isEmpty()) {
                        throw new Exception("This manifest has no registered CP");
                    }

                    future.complete(packages);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<JsonObject> getManifestInfo(int parcelManifestId, int branchofficeId, int createdBy) {
        Future<JsonObject> future = Future.future();
        try {
            this.dbClient.queryWithParams(QUERY_GET_MANIFEST_INFO, new JsonArray().add(parcelManifestId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> manifests = reply.result().getRows();
                    if (manifests.isEmpty()) {
                        throw new Exception("Parcel manifest not found");
                    }

                    JsonObject manifest = manifests.get(0);
                    String initRouteDate = manifest.getString(_INIT_ROUTE_DATE);
                    if (Objects.nonNull(initRouteDate)) {
                        throw new Exception("The manifest is already on its way");
                    }

                    Integer manifestBranchofficeId = manifest.getInteger(_BRANCHOFFICE_ID);
                    if (!manifestBranchofficeId.equals(branchofficeId)) {
                        throw new Exception("The manifest was not opened at the employee's branch");
                    }

                    Integer manifestCreatedBy = manifest.getInteger(CREATED_BY);
                    if (!manifestCreatedBy.equals(createdBy)) {
                        throw new Exception("The manifest was not created by the employee");
                    }

                    Integer manifestStatus = manifest.getInteger(STATUS);
                    if (manifestStatus.equals(4)) {
                        throw new Exception("The manifest was canceled");
                    }
                    if (!manifestStatus.equals(2)) {
                        throw new Exception("The manifest is not closed");
                    }

                    future.complete(manifest);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateParcelManifest(SQLConnection conn, int parcelManifestId, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery update = this.generateGenericUpdate("parcels_manifest", new JsonObject()
                    .put(ID, parcelManifestId)
                    .put(STATUS, 2)
                    .put(_INIT_ROUTE_DATE, UtilsDate.sdfDataBase(new Date()))
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                    .put(UPDATED_BY, createdBy));

            conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
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

    private CompletableFuture<Boolean> updateParcelsStatus(SQLConnection conn, List<JsonObject> packages, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<GenericQuery> updates = new ArrayList<>();
            List<Integer> parcelsIds = packages.stream()
                    .map(p -> p.getInteger(_PARCEL_ID))
                    .distinct()
                    .collect(Collectors.toList());
            for (Integer parcelId : parcelsIds) {
                updates.add(this.generateGenericUpdate("parcels", new JsonObject()
                        .put(ID, parcelId)
                        .put(_PARCEL_STATUS, PARCEL_STATUS.EAD.ordinal())
                        .put(UPDATED_BY, createdBy)
                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))));
            }
            List<JsonArray> params = updates.stream().map(GenericQuery::getParams).collect(Collectors.toList());

            conn.batchWithParams(updates.get(0).getQuery(), params, reply -> {
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

    private CompletableFuture<Boolean> registerPackageTracking(SQLConnection conn, List<JsonObject> packages,
                                       int parcelManifestId, int branchofficeId, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<GenericQuery> tasks = new ArrayList<>();
            for (JsonObject pack : packages) {
                tasks.add(this.generateGenericCreate("parcels_packages_tracking", new JsonObject()
                        .put(_PARCEL_ID, pack.getInteger(_PARCEL_ID))
                        .put(_PARCEL_PACKAGE_ID, pack.getInteger(_PARCEL_PACKAGE_ID))
                        .put(_PARCEL_MANIFEST_ID, parcelManifestId)
                        .put(_ACTION, PARCELPACKAGETRACKING_STATUS.EAD.getValue())
                        .put(_TERMINAL_ID, branchofficeId)
                        .put(CREATED_BY, createdBy)));
            }
            List<JsonArray> params = tasks.stream().map(GenericQuery::getParams).collect(Collectors.toList());

            conn.batchWithParams(tasks.get(0).getQuery(), params, reply -> {
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

    private CompletableFuture<Boolean> registerParcelManifestRouteLog(SQLConnection conn, int parcelManifestId, Double latitude, Double longitude, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery insert = this.generateGenericCreate("parcels_manifest_route_logs", new JsonObject()
                            .put(_PARCEL_MANIFEST_ID, parcelManifestId)
                            .put(_LATITUDE, latitude)
                            .put(_LONGITUDE, longitude)
                            .put(_TYPE, PARCEL_MANIFEST_ROUTE_LOG_TYPE.INIT.getValue())
                            .put(CREATED_AT, UtilsDate.sdfDataBase(new Date()))
                            .put(CREATED_BY, createdBy));

            conn.updateWithParams(insert.getQuery(), insert.getParams(), reply -> {
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

    private static final String QUERY_GET_PACKAGES_INFO_BY_MANIFEST = "SELECT\n" +
            "   p.id AS parcel_id,\n" +
            "   pp.id AS parcel_package_id\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "INNER JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest \n" +
            "   AND pmd.status = " + PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal() + "\n" +
            "WHERE pm.id = ?\n" +
            "   AND p.parcel_status IN ("+ PARCEL_STATUS.ARRIVED.ordinal() +", "+ PARCEL_STATUS.ARRIVED_INCOMPLETE.ordinal() +") \n" +
            "   AND pp.package_status = "+ PACKAGE_STATUS.EAD.ordinal();

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.init_route_date,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.id = ?;";

    private static final String QUERY_UPDATE_PARCELS_STATUS = "UPDATE parcels \n" +
            "SET parcel_status = " + PARCEL_STATUS.EAD.ordinal() + ", updated_by = ?, updated_at = ? WHERE parcel_status = "+ PARCEL_STATUS.ARRIVED.ordinal() +" AND id IN (%s)";

}
