package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class RouteLogDetailReport extends DBHandler<ParcelsManifestDBV> {

    public RouteLogDetailReport(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);

            getLogsDetail(parcelManifestId).whenComplete((logs, errLogs) -> {
                try {
                    if (errLogs != null) {
                        throw errLogs;
                    }
                    message.reply(new JsonArray(logs));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getLogsDetail(Integer parcelManifestId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_GET_LOGS_DETAIL, new JsonArray().add(parcelManifestId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> logs = reply.result().getRows();
                    List<CompletableFuture<Boolean>> logsTasks = new ArrayList<>();
                    for (JsonObject log : logs) {
                        logsTasks.add(getParcelInfo(log));
                    }

                    if (logsTasks.isEmpty()) {
                        future.complete(new ArrayList<>());
                        return;
                    }

                    CompletableFuture.allOf(logsTasks.toArray(new CompletableFuture[logsTasks.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            future.complete(logs);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> getParcelInfo(JsonObject log) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelManifestDetailId = log.getInteger(_PARCEL_MANIFEST_DETAIL_ID);
            if(Objects.isNull(parcelManifestDetailId)) {
                future.complete(true);
            } else {
                this.dbClient.queryWithParams(QUERY_GET_PARCEL_INFO, new JsonArray().add(parcelManifestDetailId), reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        List<JsonObject> parcels = reply.result().getRows();
                        if(parcels.isEmpty()) {
                            future.complete(true);
                            return;
                        }

                        JsonObject parcel = parcels.get(0);
                        log.put("parcel", parcel);
                        getPackagestInfo(parcel).whenComplete((resPack, errPack) -> {
                           try {
                               if(errPack != null) {
                                   throw errPack;
                               }
                               getDeliveryAttempts(parcel, parcelManifestDetailId).whenComplete((resDA, errDA) -> {
                                   try {
                                       if(errDA != null) {
                                           throw errDA;
                                       }
                                       future.complete(true);
                                   } catch (Throwable t) {
                                       future.completeExceptionally(t);
                                   }
                               });
                           } catch (Throwable t) {
                               future.completeExceptionally(t);
                           }
                        });
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

    private CompletableFuture<Boolean> getPackagestInfo(JsonObject parcel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelId = parcel.getInteger(_PARCEL_ID);
            this.dbClient.queryWithParams(QUERY_GET_PARCEL_PACKAGES_INFO, new JsonArray().add(parcelId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    parcel.put(_PARCEL_PACKAGES, reply.result().getRows());
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

    private CompletableFuture<Boolean> getDeliveryAttempts(JsonObject parcel, Integer parcelManifestDetailId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelId = parcel.getInteger(_PARCEL_ID);
            this.dbClient.queryWithParams(QUERY_GET_PARCEL_DELIVERY_ATTEMPTS, new JsonArray().add(parcelId).add(parcelManifestDetailId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    parcel.put("delivery_attempts", reply.result().getRows());
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

    private final static String QUERY_GET_LOGS_DETAIL = "SELECT\n" +
            "   pmrl.id,\n" +
            "   pmrl.parcel_manifest_id,\n" +
            "   pmrl.parcel_manifest_detail_id,\n" +
            "   pmrl.speed,\n" +
            "   pmrl.latitude,\n" +
            "   pmrl.longitude,\n" +
            "   pmrl.type,\n" +
            "   pmrl.created_at AS log_date,\n" +
            "   TIME_FORMAT(TIMEDIFF(pmrl.created_at, pm.init_route_date), '%H:%i') AS duration_time\n" +
            "FROM parcels_manifest_route_logs pmrl\n" +
            "INNER JOIN parcels_manifest pm ON pm.id = pmrl.parcel_manifest_id\n" +
            "LEFT JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id AND pmd.id = pmrl.parcel_manifest_detail_id\n" +
            "WHERE pm.id = ?\n" +
            "GROUP BY pmrl.id\n" +
            "ORDER BY pmrl.created_at;";

    private final static String QUERY_GET_PARCEL_INFO = "SELECT\n" +
            "    p.id AS parcel_id,\n" +
            "    p.parcel_tracking_code,\n" +
            "    p.parcel_status,\n" +
            "    p.delivered_at,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_full_name,\n" +
            "    c.phone AS customer_phone,\n" +
            "    ca.address AS customer_address,\n" +
            "    pd.name AS parcels_deliveries_name,\n" +
            "    pd.last_name AS parcels_deliveries_last_name,\n" +
            "    pd.signature AS parcels_deliveries_signature\n" +
            "FROM parcels_manifest_detail pmd\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN parcels p ON p.id = pre.parcel_id\n" +
            "LEFT JOIN \n" +
            "\t(SELECT pp.parcel_id, pd.name, pd.last_name, pd.signature FROM parcels_deliveries pd\n" +
            "    INNER JOIN parcels_packages pp ON pp.parcels_deliveries_id = pd.id) AS pd\n" +
            "    ON pd.parcel_id = p.id\n" +
            "INNER JOIN customer c ON c.id = p.customer_id\n" +
            "INNER JOIN customer_addresses ca ON ca.id = p.addressee_address_id\n" +
            "WHERE pmd.id = ?\n" +
            "GROUP BY p.id;";

    private final static String QUERY_GET_PARCEL_PACKAGES_INFO = "SELECT\n" +
            "   COUNT(pp.id) AS quantity,\n" +
            "    pp.package_status,\n" +
            "    pp.width,\n" +
            "    pp.length,\n" +
            "    pp.height,\n" +
            "    pp.weight,\n" +
            "    pprice.name_price,\n" +
            "    pt.name package_type_name\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN package_types pt ON pt.id = pp.package_type_id\n" +
            "INNER JOIN package_price pprice ON pprice.id = pp.package_price_id\n" +
            "WHERE pp.parcel_id = ?\n" +
            "GROUP BY pprice.id;\n";

    private final static String QUERY_GET_PARCEL_DELIVERY_ATTEMPTS = "SELECT \n" +
            "   pda.id, \n" +
            "   pda.notes,\n" +
            "   pda.image_name,\n" +
            "   pda.created_at,\n" +
            "   dar.name AS delivery_attempt_reason_name\n" +
            "FROM parcels_delivery_attempts pda\n" +
            "INNER JOIN delivery_attempt_reason dar ON dar.id = pda.delivery_attempt_reason_id\n" +
            "WHERE pda.parcel_id = ? AND pda.parcel_manifest_detail_id = ?\n" +
            "GROUP BY pda.id;";
}
