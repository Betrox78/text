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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class DeliveryAttempt extends DBHandler<ParcelsManifestDBV> {

    public DeliveryAttempt(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            Integer parcelManifestDetailId = body.getInteger(_PARCEL_MANIFEST_DETAIL_ID);
            Integer deliveryAttemptReasonId = body.getInteger(_DELIVERY_ATTEMPT_REASON_ID);
            String notes = body.getString(_NOTES);
            String imageName = body.getString(_IMAGE_NAME);
            Integer createdBy = body.getInteger(CREATED_BY);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);

            Future<JsonObject> f1 = Future.future();
            Future<List<JsonObject>> f2 = Future.future();
            getManifestInfo(parcelManifestDetailId, branchofficeId, createdBy).setHandler(f1.completer());
            getPackagesInfoByManifest(parcelManifestDetailId).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject parcelManifest = reply.result().resultAt(0);
                    List<JsonObject> packages = reply.result().resultAt(1);
                    JsonObject basePackage = packages.get(0);
                    int parcelManifestId = parcelManifest.getInteger(ID);
                    int parcelId = basePackage.getInteger(_PARCEL_ID);

                    startTransaction(message, conn -> {
                        try {
                            List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                            tasks.add(insertParcelDeliveryAttempt(conn, parcelId, parcelManifestDetailId, deliveryAttemptReasonId, notes, imageName, createdBy));
                            for (JsonObject pack : packages) {
                                Integer packageId = pack.getInteger(_PARCEL_PACKAGE_ID);
                                tasks.add(insertParcelPackageTracking(conn, parcelManifestId, branchofficeId, parcelId, packageId, notes, createdBy));
                            }
                            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                                try {
                                    if (err != null) {
                                        throw err;
                                    }
                                    this.commit(conn, message, new JsonObject()
                                            .put("created", true));
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

    private Future<List<JsonObject>> getPackagesInfoByManifest(int parcelManifestDetailId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(parcelManifestDetailId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGES_INFO_BY_MANIFEST, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> packages = reply.result().getRows();
                    if (packages.isEmpty()) {
                        throw new Exception("Manifest detail packages in EAD status not found");
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

    private Future<JsonObject> getManifestInfo(int parcelManifestDetailId, int branchofficeId, int createdBy) {
        Future<JsonObject> future = Future.future();
        try {
            this.dbClient.queryWithParams(QUERY_GET_MANIFEST_INFO, new JsonArray().add(parcelManifestDetailId), reply -> {
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
                    if (manifestStatus.equals(PARCEL_MANIFEST_STATUS.CANCELED.ordinal())) {
                        throw new Exception("The manifest was canceled");
                    }
                    if (manifestStatus.equals(PARCEL_MANIFEST_STATUS.FINISHED.ordinal())) {
                        throw new Exception("The manifest was finished");
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

    private CompletableFuture<Boolean> insertParcelDeliveryAttempt(SQLConnection conn, int parcelId, int parcelManifestDetailId,
                                                   int deliveryAttemptReasonId, String notes, String imageName, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery create = this.generateGenericCreate("parcels_delivery_attempts", new JsonObject()
                    .put(_PARCEL_ID, parcelId)
                    .put(_PARCEL_MANIFEST_DETAIL_ID, parcelManifestDetailId)
                    .put(_DELIVERY_ATTEMPT_REASON_ID, deliveryAttemptReasonId)
                    .put(_NOTES, notes)
                    .put(_IMAGE_NAME, imageName)
                    .put(CREATED_BY, createdBy));

            conn.updateWithParams(create.getQuery(), create.getParams(), reply -> {
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

    private CompletableFuture<Boolean> insertParcelPackageTracking(SQLConnection conn, int parcelManifestId, int branchOfficeId, int parcelId, int packageId, String notes, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery create = this.generateGenericCreate("parcels_packages_tracking", new JsonObject()
                    .put(_PARCEL_ID, parcelId)
                    .put(_PARCEL_PACKAGE_ID, packageId)
                    .put(_PARCEL_MANIFEST_ID, parcelManifestId)
                    .put(_TERMINAL_ID, branchOfficeId)
                    .put(_NOTES, notes)
                    .put(_ACTION, PARCELPACKAGETRACKING_STATUS.DELIVERY_ATTEMPT.getValue())
                    .put(CREATED_BY, createdBy));

            conn.updateWithParams(create.getQuery(), create.getParams(), reply -> {
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
            "   p.total_packages,\n" +
            "   pp.id AS parcel_package_id\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "INNER JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest AND pmd.status = 1\n" +
            "WHERE pmd.id = ?\n" +
            "   AND p.parcel_status = "+ PARCEL_STATUS.EAD.ordinal() +" \n" +
            "   AND pp.package_status = "+ PACKAGE_STATUS.EAD.ordinal() +" \n" +
            "   AND pmd.status = "+ PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal();

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "WHERE pmd.id = ?;";

}
