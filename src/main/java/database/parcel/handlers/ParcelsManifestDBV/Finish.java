package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_ROUTE_LOG_TYPE;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
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

import static service.commons.Constants.*;

public class Finish extends DBHandler<ParcelsManifestDBV> {

    public Finish(ParcelsManifestDBV dbVerticle) {
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
            Integer deliveryAttemptReasonId = body.getInteger(_DELIVERY_ATTEMPT_REASON_ID);
            String otherReasonsNotRadEad = body.getString(_OTHER_REASONS_NOT_RAD_EAD);
            Integer createdBy = body.getInteger(CREATED_BY);

            Future<JsonObject> f1 = Future.future();
            Future<List<JsonObject>> f2 = Future.future();
            getManifestInfo(parcelManifestId, branchofficeId, createdBy).setHandler(f1.completer());
            getDetailsByManifest(parcelManifestId).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
               try {
                   if (reply.failed()) {
                       throw reply.cause();
                   }

                   List<JsonObject> details = reply.result().resultAt(1);
                   if (!details.isEmpty() && Objects.isNull(deliveryAttemptReasonId)) {
                       throw new Exception("A delivery attempt reason must be specified");
                   }

                   startTransaction(message, conn -> {
                       try {

                           List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                           for (JsonObject detail : details) {
                               int parcelManifestDetailId = detail.getInteger(_PARCEL_MANIFEST_DETAIL_ID);
                               tasks.add(returnPackage(branchofficeId, parcelManifestDetailId, deliveryAttemptReasonId, otherReasonsNotRadEad, createdBy));
                           }
                           tasks.add(updateParcelManifest(conn, parcelManifestId, createdBy));
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

    private Future<List<JsonObject>> getDetailsByManifest(int parcelManifestId) {
        Future<List<JsonObject>> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(parcelManifestId);
            this.dbClient.queryWithParams(QUERY_GET_DETAIL_INFO_BY_MANIFEST, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> details = reply.result().getRows();
                    future.complete(details);
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
                    if (manifestStatus.equals(3)) {
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

    private CompletableFuture<Boolean> updateParcelManifest(SQLConnection conn, int parcelManifestId, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery update = this.generateGenericUpdate("parcels_manifest", new JsonObject()
                    .put(ID, parcelManifestId)
                    .put(STATUS, 3)
                    .put(_FINISH_ROUTE_DATE, UtilsDate.sdfDataBase(new Date()))
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

    private CompletableFuture<Boolean> returnPackage(int branchOfficeId, int parcelManifestDetailId, int deliveryAttemptReasonId, String otherReasonsNotRadEad, int createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            otherReasonsNotRadEad = Objects.nonNull(otherReasonsNotRadEad) ? otherReasonsNotRadEad : "Cierre de bitácora";
            JsonObject body = new JsonObject()
                    .put(_BRANCHOFFICE_ID, branchOfficeId)
                    .put(_PARCEL_MANIFEST_DETAIL_ID, parcelManifestDetailId)
                    .put(_DELIVERY_ATTEMPT_REASON_ID, deliveryAttemptReasonId)
                    .put(_OTHER_REASONS_NOT_RAD_EAD, otherReasonsNotRadEad)
                    .put(CREATED_BY, createdBy);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_RETURN_PACKAGE_TO_ARRIVED);
            this.getVertx().eventBus().send(ParcelsManifestDBV.class.getSimpleName(), body, options, reply -> {
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
                    .put(_TYPE, PARCEL_MANIFEST_ROUTE_LOG_TYPE.END.getValue())
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

    private static final String QUERY_GET_DETAIL_INFO_BY_MANIFEST = "SELECT\n" +
            "   pmd.id AS parcel_manifest_detail_id\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "INNER JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest AND pmd.status = 1\n" +
            "WHERE pm.id = ?\n" +
            "   AND p.parcel_status = "+ PARCEL_STATUS.EAD.ordinal() +" \n" +
            "   AND pp.package_status = "+ PACKAGE_STATUS.EAD.ordinal() +" \n" +
            "   AND pmd.status = "+ PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal() +" \n" +
            "GROUP BY pmd.id";

    private static final String QUERY_GET_MANIFEST_INFO = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.id_type_service AS type_service_id,\n" +
            "   pm.id_branchoffice AS branchoffice_id,\n" +
            "   pm.status,\n" +
            "   pm.created_by\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.id = ?;";

}
