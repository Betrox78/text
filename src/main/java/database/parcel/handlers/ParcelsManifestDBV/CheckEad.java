package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.*;
import database.parcel.handlers.ParcelsManifestDBV.Exception.ParcelManifestException;
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
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class CheckEad extends DBHandler<ParcelsManifestDBV> {

    public CheckEad(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String parcelTrackingCode = body.getString(_PARCEL_TRACKING_CODE);
            String packageCode = body.getString(_PACKAGE_CODE);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            Integer createdBy = body.getInteger(CREATED_BY);

            if (Objects.nonNull(parcelTrackingCode)) {
                checkParcelTrackingCode(message, parcelTrackingCode, branchofficeId, parcelManifestId, createdBy);
            } else if (Objects.nonNull(packageCode)) {
                checkPackageCode(message, packageCode, branchofficeId, parcelManifestId, createdBy);
            } else {
                throw new Exception("Parcel tracking code and package code not found");
            }
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void checkParcelTrackingCode(Message<JsonObject> message, String parcelTrackingCode, int branchofficeId, int parcelManifestId, int createdBy) {
        try {
            Future<JsonObject> f1 = Future.future();
            Future<JsonObject> f2 = Future.future();
            getManifestInfo(parcelManifestId, branchofficeId, createdBy).setHandler(f1.completer());
            getParcelInfo(parcelTrackingCode, branchofficeId).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject parcelInfo = reply.result().resultAt(1);
                    int parcelRadEadId = parcelInfo.getInteger(_PARCEL_RAD_EAD_ID);
                    startTransaction(message, conn -> {
                        try {
                            registerParcelManifestDetail(conn, parcelManifestId, parcelRadEadId, createdBy).whenComplete((pmdId, err) -> {
                                try {
                                    if (err != null) {
                                        throw err;
                                    }
                                    this.commit(conn, message, new JsonObject()
                                            .put(_PARCEL_MANIFEST_DETAIL_ID, pmdId));
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

    private void checkPackageCode(Message<JsonObject> message, String packageCode, int branchofficeId, int parcelManifestId, int createdBy) {
        try {
            Future<JsonObject> f1 = Future.future();
            Future<JsonObject> f2 = Future.future();
            getManifestInfo(parcelManifestId, branchofficeId, createdBy).setHandler(f1.completer());
            getPackageInfo(packageCode, branchofficeId, parcelManifestId).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject packageInfo = reply.result().resultAt(1);
                    int packageId = packageInfo.getInteger(ID);
                    startTransaction(message, conn -> {
                        try {
                            updatePackagesStatus(conn, packageId, createdBy).whenComplete((res, err) -> {
                                try {
                                    if (err != null) {
                                        throw err;
                                    }
                                    this.commit(conn, message, new JsonObject()
                                            .put("checked", res));
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

    private Future<JsonObject> getParcelInfo(String parcelTrackingCode, int branchofficeId) {
        Future<JsonObject> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(branchofficeId).add(parcelTrackingCode);
            this.dbClient.queryWithParams(QUERY_GET_PARCEL_INFO, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new ParcelManifestException("Waybill not found");
                    }

                    JsonObject basePack = results.get(0);
                    Integer parcelManifestId = basePack.getInteger(_PARCEL_MANIFEST_ID);
                    boolean isReplacementBranchoffice = basePack.getInteger("is_replacement_branchoffice") > 0;
                    if (Objects.nonNull(parcelManifestId)) {
                        throw new ParcelManifestException("This CP is already registered in a manifest");
                    }

                    List<JsonObject> arrivedPackages = results.stream()
                            .filter(p ->
                                    p.getInteger(_PACKAGE_STATUS).equals(PACKAGE_STATUS.ARRIVED.ordinal()))
                            .collect(Collectors.toList());
                    if (arrivedPackages.isEmpty()) {
                        throw new ParcelManifestException("This CP has no packages at the destination branch");
                    }

                    PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[basePack.getInteger(_PARCEL_STATUS)];
                    if (!parcelStatus.equals(PARCEL_STATUS.ARRIVED)) {
                        throw new ParcelManifestException("This CP is not at the destination branch, current status: " + parcelStatus.name());
                    }

                    SHIPMENT_TYPE basePackShipmenType = SHIPMENT_TYPE.fromValue(basePack.getString(_SHIPMENT_TYPE));
                    if (!basePackShipmenType.includeEAD()) {
                        throw new ParcelManifestException("This CP does not have home delivery");
                    }

                    Integer arrivedTerminalId = basePack.getInteger("arrived_terminal_id");
                    if (Objects.isNull(arrivedTerminalId) || (!arrivedTerminalId.equals(branchofficeId)) && !isReplacementBranchoffice) {
                        throw new ParcelManifestException("This CP is not in the employee's branch");
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

    private Future<JsonObject> getPackageInfo(String packageCode, int branchofficeId, int parcelManifestId) {
        Future<JsonObject> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(branchofficeId).add(packageCode).add(parcelManifestId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGE_INFO, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new ParcelManifestException("Waybill not scanned");
                    }

                    JsonObject basePack = results.get(0);
                    PARCEL_MANIFEST_STATUS parcelManifestStatus = PARCEL_MANIFEST_STATUS.values()[basePack.getInteger(_PARCEL_MANIFEST_STATUS)];
                    boolean isReplacementBranchoffice = basePack.getInteger("is_replacement_branchoffice") > 0;
                    if (!parcelManifestStatus.equals(PARCEL_MANIFEST_STATUS.OPEN)) {
                        throw new ParcelManifestException("The manifest is not open");
                    }

                    PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[basePack.getInteger(_PACKAGE_STATUS)];
                    if (packageStatus.equals(PACKAGE_STATUS.EAD)) {
                        throw new ParcelManifestException("This package has been scanned");
                    }
                    if (!packageStatus.equals(PACKAGE_STATUS.ARRIVED)) {
                        throw new ParcelManifestException("This package is not at the destination branch, current status: " + packageStatus.name());
                    }

                    SHIPMENT_TYPE basePackShipmenType = SHIPMENT_TYPE.fromValue(basePack.getString(_SHIPMENT_TYPE));
                    if (!basePackShipmenType.includeEAD()) {
                        throw new ParcelManifestException("This package does not have home delivery");
                    }

                    Integer arrivedTerminalId = basePack.getInteger("arrived_terminal_id");
                    if (Objects.isNull(arrivedTerminalId) || (!arrivedTerminalId.equals(branchofficeId)) && !isReplacementBranchoffice) {
                        throw new ParcelManifestException("This package is not in the employee's branch");
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
                        throw new ParcelManifestException("Parcel manifest not found");
                    }

                    JsonObject manifest = manifests.get(0);
                    Integer manifestBranchofficeId = manifest.getInteger(_BRANCHOFFICE_ID);
                    if (!manifestBranchofficeId.equals(branchofficeId)) {
                        throw new ParcelManifestException("The manifest was not opened at the employee's branch");
                    }

                    Integer manifestTypeServiceId = manifest.getInteger(_TYPE_SERVICE_ID);
                    if (!manifestTypeServiceId.equals(2)) {
                        throw new ParcelManifestException("The manifest is not EAD");
                    }

                    Integer manifestCreatedBy = manifest.getInteger(CREATED_BY);
                    if (!manifestCreatedBy.equals(createdBy)) {
                        throw new ParcelManifestException("The manifest was not opened by the employee");
                    }

                    Integer manifestStatus = manifest.getInteger(STATUS);
                    if (manifestStatus.equals(4)) {
                        throw new ParcelManifestException("The manifest was canceled");
                    }
                    if (!manifestStatus.equals(1)) {
                        throw new ParcelManifestException("The manifest is not open");
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

    private CompletableFuture<Integer> registerParcelManifestDetail(SQLConnection conn, int parcelManifestId, int parcelRadEadId, int createdBy) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            GenericQuery create = this.generateGenericCreate("parcels_manifest_detail", new JsonObject()
                    .put("id_parcels_manifest", parcelManifestId)
                    .put("id_parcels_rad_ead", parcelRadEadId)
                    .put(STATUS, 1)
                    .put(CREATED_BY, createdBy));

            conn.updateWithParams(create.getQuery(), create.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    future.complete(reply.result().getKeys().getInteger(0));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
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

    private static final String QUERY_GET_PARCEL_INFO = "SELECT\n" +
            "   p.id AS parcel_id,\n" +
            "   p.parcel_status,\n" +
            "   p.shipment_type,\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.package_status,\n" +
            "   (SELECT t.terminal_id FROM parcels_packages_tracking t \n" +
            "       WHERE t.parcel_package_id = pp.id AND t.action = 'arrived' LIMIT 1) AS arrived_terminal_id,\n" +
            "   (SELECT CASE WHEN COUNT(bprc.id) > 0 THEN 1 ELSE 0 END \n" +
            "       FROM branchoffice_parcel_receiving_config bprc \n" +
            "       WHERE bprc.receiving_branchoffice_id = ?\n" +
            "       AND bprc.of_branchoffice_id = p.terminal_destiny_id LIMIT 1) AS is_replacement_branchoffice,\n" +
            "   pm.id AS parcel_manifest_id,\n" +
            "   pre.id AS parcel_rad_ead_id\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "LEFT JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "LEFT JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "   AND pmd.status = "+ PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal() +"\n" +
            "LEFT JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest \n" +
            "   AND pm.status NOT IN ("+PARCEL_MANIFEST_STATUS.FINISHED.ordinal()+", "+PARCEL_MANIFEST_STATUS.CANCELED.ordinal()+")\n" +
            "WHERE p.parcel_tracking_code = ?;";

    private static final String QUERY_GET_PACKAGE_INFO = "SELECT\n" +
            "   pp.id,\n" +
            "   p.id AS parcel_id,\n" +
            "   p.parcel_status,\n" +
            "   p.shipment_type,\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.package_status,\n" +
            "   (SELECT t.terminal_id FROM parcels_packages_tracking t \n" +
            "       WHERE t.parcel_package_id = pp.id AND t.action = 'arrived'\n" +
            "       ORDER BY t.id DESC\n" +
            "       LIMIT 1) AS arrived_terminal_id,\n" +
            "   (SELECT CASE WHEN COUNT(bprc.id) > 0 THEN 1 ELSE 0 END \n" +
            "       FROM branchoffice_parcel_receiving_config bprc \n" +
            "       WHERE bprc.receiving_branchoffice_id = ?\n" +
            "       AND bprc.of_branchoffice_id = p.terminal_destiny_id LIMIT 1) AS is_replacement_branchoffice,\n" +
            "   pm.id AS parcel_manifest_id,\n" +
            "   pm.status AS parcel_manifest_status\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "INNER JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest\n" +
            "WHERE pp.package_code = ?\n" +
            "   AND pm.id = ?\n";

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.id = ?;";

}
