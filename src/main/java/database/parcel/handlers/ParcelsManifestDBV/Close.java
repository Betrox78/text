package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_STATUS;
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

public class Close extends DBHandler<ParcelsManifestDBV> {

    public Close(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
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

                   startTransaction(message, conn -> {
                       try {
                           updateParcelManifest(conn, parcelManifestId, createdBy).whenComplete((pmdId, err) -> {
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
                    String finishLoadDate = manifest.getString(_FINISH_LOAD_DATE);
                    if (Objects.nonNull(finishLoadDate)) {
                        throw new Exception("The manifest is already closed");
                    }

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

    private CompletableFuture<Integer> updateParcelManifest(SQLConnection conn, int parcelManifestId, int createdBy) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            GenericQuery update = this.generateGenericUpdate("parcels_manifest", new JsonObject()
                    .put(ID, parcelManifestId)
                    .put(STATUS, 2)
                    .put(_FINISH_LOAD_DATE, UtilsDate.sdfDataBase(new Date()))
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                    .put(UPDATED_BY, createdBy));

            conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    future.complete(reply.result().getUpdated());
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
            "INNER JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest AND pmd.status = 1\n" +
            "WHERE pm.id = ?\n" +
            "   AND p.parcel_status = "+ PARCEL_STATUS.ARRIVED.ordinal() +" \n" +
            "   AND pp.package_status = "+ PACKAGE_STATUS.EAD.ordinal() +" \n" +
            "   AND pmd.status = "+ PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal();

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.finish_load_date,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.id = ?;";

}
