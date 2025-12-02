package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import database.parcel.enums.SHIPMENT_TYPE;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class CheckEadContingency extends DBHandler<ParcelsManifestDBV> {

    public CheckEadContingency(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String packageCode = body.getString(_PACKAGE_CODE);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            Integer createdBy = body.getInteger(CREATED_BY);

            Future<JsonObject> f1 = Future.future();
            Future<JsonObject> f2 = Future.future();
            getPackageInfo(packageCode, branchofficeId, parcelManifestId).setHandler(f1.completer());
            getManifestInfo(parcelManifestId, branchofficeId, createdBy).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject packageInfo = reply.result().resultAt(0);
                    int packageId = packageInfo.getInteger(ID);
                    startTransaction(message, conn -> {
                        try {
                            updatePackagesStatus(conn, packageId, createdBy).whenComplete((res, err) -> {
                                try {
                                    if (err != null) {
                                        throw err;
                                    }
                                    registerPackageTracking(conn, packageInfo, branchofficeId, createdBy).whenComplete((resRPT, errRPT) -> {
                                        try {
                                            if (errRPT != null) {
                                                throw errRPT;
                                            }
                                            this.commit(conn, message, new JsonObject()
                                                    .put("checked", resRPT));
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

    private Future<JsonObject> getPackageInfo(String packageCode, int branchofficeId, int parcelManifestId) {
        Future<JsonObject> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(parcelManifestId).add(packageCode);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGE_INFO, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("This package is not in destination branch");
                    }

                    JsonObject basePack = results.get(0);
                    SHIPMENT_TYPE basePackShipmenType = SHIPMENT_TYPE.fromValue(basePack.getString(_SHIPMENT_TYPE));
                    if (!basePackShipmenType.includeEAD()) {
                        throw new Exception("This package does not have home delivery");
                    }

                    Integer arrivedTerminalId = basePack.getInteger("arrived_terminal_id");
                    if (Objects.isNull(arrivedTerminalId) || !arrivedTerminalId.equals(branchofficeId)) {
                        throw new Exception("This package is not in the employee's branch");
                    }

                    future.complete(basePack);
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

                    Integer manifestTypeServiceId = manifest.getInteger(_TYPE_SERVICE_ID);
                    if (!manifestTypeServiceId.equals(2)) {
                        throw new Exception("The manifest is not EAD");
                    }

                    Integer manifestCreatedBy = manifest.getInteger(CREATED_BY);
                    if (!manifestCreatedBy.equals(createdBy)) {
                        throw new Exception("The manifest was not opened by the employee");
                    }

                    Integer manifestStatus = manifest.getInteger(STATUS);
                    if (manifestStatus.equals(4)) {
                        throw new Exception("The manifest was canceled");
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

    private CompletableFuture<Boolean> updatePackagesStatus(SQLConnection conn, int packageId, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery update = this.generateGenericUpdate("parcels_packages",
                    new JsonObject()
                            .put(ID, packageId)
                            .put(_PACKAGE_STATUS, PACKAGE_STATUS.EAD.ordinal())
                            .put(UPDATED_BY, createdBy)
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date())));
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

    private CompletableFuture<Boolean> registerPackageTracking(SQLConnection conn, JsonObject pack, int branchofficeId, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery insert = this.generateGenericCreate("parcels_packages_tracking", new JsonObject()
                    .put(_PARCEL_ID, pack.getInteger(_PARCEL_ID))
                    .put(_PARCEL_PACKAGE_ID, pack.getInteger(ID))
                    .put(_ACTION, PARCELPACKAGETRACKING_STATUS.EAD.getValue())
                    .put(_TERMINAL_ID, branchofficeId)
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

    private static final String QUERY_GET_PACKAGE_INFO = "SELECT\n" +
            "   pp.id,\n" +
            "   p.id AS parcel_id,\n" +
            "   p.shipment_type,\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   (SELECT t.terminal_id FROM parcels_packages_tracking t \n" +
            "       WHERE t.parcel_package_id = pp.id AND t.action = 'arrived' LIMIT 1) AS arrived_terminal_id,\n" +
            "   pm.id AS parcel_manifest_id\n" +
            "FROM parcels_packages pp\n" +
            "LEFT JOIN parcels p ON p.id = pp.parcel_id\n" +
            "LEFT JOIN parcels_manifest pm ON pm.id = ? \n" +
            "   AND pm.status = "+ PARCEL_MANIFEST_STATUS.OPEN.ordinal() +"\n" +
            "WHERE pp.package_code = ?\n" +
            "   AND pp.package_status = "+ PACKAGE_STATUS.ARRIVED.ordinal();

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.id = ?;";

}
