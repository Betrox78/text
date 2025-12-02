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

import static database.parcel.ParcelsPackagesDBV.PARCEL_PACKAGE_ID;
import static service.commons.Constants.*;

public class DeleteCP extends DBHandler<ParcelsManifestDBV> {

    public DeleteCP(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer parcelId = body.getInteger(_PARCEL_ID);
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            Integer createdBy = body.getInteger(CREATED_BY);

            Future<List<JsonObject>> f1 = Future.future();
            Future<JsonObject> f2 = Future.future();
            getPackagesInfo(parcelId, parcelManifestId).setHandler(f1.completer());
            getManifestInfo(parcelManifestId, branchofficeId, createdBy).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> packagesInfo = reply.result().resultAt(0);

                    startTransaction(message, conn -> {
                        try {
                            updatePackagesStatus(conn, packagesInfo, createdBy).whenComplete((res, err) -> {
                                try {
                                    if (err != null) {
                                        throw err;
                                    }
                                    JsonObject basePackage = packagesInfo.get(0);
                                    Integer parcelManifestDetailId = basePackage.getInteger(_PARCEL_MANIFEST_DETAIL_ID);
                                    deleteParcelManifestDetail(conn, parcelManifestDetailId).whenComplete((resD, errD) -> {
                                        try {
                                            if (errD != null) {
                                                throw errD;
                                            }
                                            this.commit(conn, message, new JsonObject()
                                                    .put("deleted", res));
                                        } catch (Throwable t) {
                                            this.rollback(conn, t, message);
                                        }
                                    });
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

    private Future<List<JsonObject>> getPackagesInfo(int parcelId, int parcelManifestId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(parcelManifestId).add(parcelId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGE_INFO, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Parcel not found in this manifest");
                    }

                    JsonObject basePackage = results.get(0);
                    PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[basePackage.getInteger(_PARCEL_STATUS)];
                    if (!parcelStatus.equals(PARCEL_STATUS.ARRIVED)) {
                        throw new Exception("Parcel status is different to arrived");
                    }

                    PARCEL_MANIFEST_DETAIL_STATUS parcelManifestDetailStatus = PARCEL_MANIFEST_DETAIL_STATUS.values()[basePackage.getInteger("parcel_manifest_detail_status")];
                    if (parcelManifestDetailStatus.equals(PARCEL_MANIFEST_DETAIL_STATUS.CANCELED)) {
                        throw new Exception("Parcel manifest detail was been canceled");
                    }
                    if (!parcelManifestDetailStatus.equals(PARCEL_MANIFEST_DETAIL_STATUS.OPEN)) {
                        throw new Exception("Parcel manifest detail is not open");
                    }

                    future.complete(results);
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
                    Integer manifestBranchofficeId = manifest.getInteger(_BRANCHOFFICE_ID);
                    if (!manifestBranchofficeId.equals(branchofficeId)) {
                        throw new Exception("The manifest was not opened at the employee's branch");
                    }

                    Integer manifestCreatedBy = manifest.getInteger(CREATED_BY);
                    if (!manifestCreatedBy.equals(createdBy)) {
                        throw new Exception("The manifest was not opened by the employee");
                    }

                    Integer manifestStatus = manifest.getInteger(STATUS);
                    if (manifestStatus.equals(4)) {
                        throw new Exception("The manifest was canceled");
                    }

                    if (!manifestStatus.equals(1)) {
                        throw new Exception("The manifest is not open");
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

    private CompletableFuture<Boolean> updatePackagesStatus(SQLConnection conn, List<JsonObject> packages, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            List<GenericQuery> updates = new ArrayList<>();
            for (JsonObject pack : packages) {
                Integer packId = pack.getInteger(ID);
                if(Objects.nonNull(packId)) {
                    updates.add(this.generateGenericUpdate("parcels_packages",
                            new JsonObject()
                                    .put(ID, pack.getInteger(ID))
                                    .put(_PACKAGE_STATUS, PACKAGE_STATUS.ARRIVED.ordinal())
                                    .put(UPDATED_BY, createdBy)
                                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))));
                }
            }

            List<JsonArray> params = updates.stream()
                    .map(GenericQuery::getParams)
                    .collect(Collectors.toList());
            if (updates.isEmpty()) {
                future.complete(true);
            } else {
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
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> deleteParcelManifestDetail(SQLConnection conn, int parcelManifestDetailId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery delete = this.generateGenericDelete("parcels_manifest_detail", new JsonObject()
                    .put(ID, parcelManifestDetailId));

            conn.updateWithParams(delete.getQuery(), delete.getParams(), reply -> {
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

    private static final String QUERY_GET_PACKAGE_INFO = "SELECT\n" +
            "   pp.id, \n" +
            "   p.parcel_status, \n" +
            "   pmd.id AS parcel_manifest_detail_id, \n" +
            "   pm.status AS parcel_manifest_status, \n" +
            "   pmd.status AS parcel_manifest_detail_status \n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN parcels p ON p.id = pre.parcel_id\n" +
            "LEFT JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "   AND pp.package_status = "+ PACKAGE_STATUS.EAD.ordinal() +"\n"+
            "WHERE pm.id = ?\n" +
            "   AND p.id = ?\n" +
            "   AND p.parcel_status = "+ PARCEL_STATUS.ARRIVED.ordinal() +"\n" +
            "GROUP BY pp.id";

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.id = ?;";

}
