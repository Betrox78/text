package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class RouteLogReport extends DBHandler<ParcelsManifestDBV> {

    public RouteLogReport(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);

            this.dbClient.queryWithParams(QUERY_PARCEL_MANIFEST_INFO, new JsonArray().add(parcelManifestId), reply -> {
               try {
                   if (reply.failed()) {
                       throw reply.cause();
                   }
                   List<JsonObject> result = reply.result().getRows();
                   if(result.isEmpty()) {
                       message.reply(new JsonObject());
                       return;
                   }
                   JsonObject info = result.get(0);
                   getLogsDetail(info, parcelManifestId).whenComplete((resLogs, errLogs) -> {
                       try {
                           if (errLogs != null) {
                               throw errLogs;
                           }
                           message.reply(info);
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

    private CompletableFuture<Boolean> getLogsDetail(JsonObject info, Integer parcelManifestId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject().put(_PARCEL_MANIFEST_ID, parcelManifestId);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsManifestDBV.ACTION_GET_ROUTE_LOG_DETAIL);
            getVertx().eventBus().send(ParcelsManifestDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    info.put("logs", reply.result().body());
                    future.complete(true);

                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private final static String QUERY_PARCEL_MANIFEST_INFO = "SELECT\n" +
            "    pm.init_route_date,\n" +
            "    pm.finish_route_date,\n" +
            "    pm.folio,\n" +
            "    pm.drive_name,\n" +
            "    pm.num_route,\n" +
            "    pm.status,\n" +
            "    v.alias AS vehicle_alias,\n" +
            "    v.economic_number AS vehicle_economic_number,\n" +
            "    b.name AS branchoffice_name,\n" +
            "    b.prefix AS branchoffice_prefix,\n" +
            "    b.latitude AS branchoffice_latitude,\n" +
            "    b.longitude AS branchoffice_longitude,\n" +
            "    COALESCE(stats.total_parcels, 0) AS total_parcels,\n" +
            "    COALESCE(stats.total_parcels_delivered, 0) AS total_parcels_delivered,\n" +
            "    COALESCE(stats.total_parcels_not_delivered, 0) AS total_parcels_not_delivered,\n" +
            "    COALESCE(stats.total_parcels_canceled, 0) AS total_parcels_canceled,\n" +
            "    COALESCE(TRUNCATE((stats.total_parcels_delivered / NULLIF(stats.total_parcels, 0)) * 100, 2), 0) AS percent_delivered,\n" +
            "    TIME_FORMAT(TIMEDIFF(pm.finish_route_date, pm.init_route_date), '%H:%i') AS duration_time\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN branchoffice b ON b.id = pm.id_branchoffice\n" +
            "INNER JOIN vehicle_rad_ead vre ON vre.id = pm.id_vehicle_rad_ead\n" +
            "INNER JOIN vehicle v ON v.id = vre.id_vehicle\n" +
            "LEFT JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT \n" +
            "        id_parcels_manifest,\n" +
            "        COUNT(CASE WHEN status != 4 THEN 1 END) AS total_parcels,\n" +
            "        COUNT(CASE WHEN status = 2 THEN 1 END) AS total_parcels_delivered,\n" +
            "        COUNT(CASE WHEN status = 3 THEN 1 END) AS total_parcels_not_delivered,\n" +
            "        COUNT(CASE WHEN status = 4 THEN 1 END) AS total_parcels_canceled\n" +
            "    FROM parcels_manifest_detail\n" +
            "    GROUP BY id_parcels_manifest\n" +
            ") AS stats ON stats.id_parcels_manifest = pm.id\n" +
            "WHERE pm.id = ?\n" +
            "GROUP BY pm.id;";
}
