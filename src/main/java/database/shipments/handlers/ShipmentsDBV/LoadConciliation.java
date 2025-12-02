package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class LoadConciliation extends DBHandler<ShipmentsDBV> {

    public LoadConciliation(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer shipmentId = body.getInteger(_SHIPMENT_ID);

           this.getMissingInfo(shipmentId).whenComplete((missingParcels, errMP) -> {
               try {
                   if (errMP != null) {
                       throw errMP;
                   }
                   this.getLoadedInfo(shipmentId).whenComplete((loadedParcels, errL) -> {
                       try {
                           if (errL != null) {
                               throw errL;
                           }
                           message.reply(new JsonObject()
                                   .put("missing_parcels", missingParcels)
                                   .put("loaded_parcels", loadedParcels));
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



    private CompletableFuture<List<JsonObject>> getMissingInfo(Integer shipmentId){
        CompletableFuture<List<JsonObject>> future  = new CompletableFuture<>();
        getMissingParcels(shipmentId).whenComplete((missingParcels, errorParcels) -> {
            try {
                if (errorParcels != null){
                    throw errorParcels;
                }
                if (missingParcels.isEmpty()) {
                    future.complete(new ArrayList<>());
                    return;
                }
                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                for (JsonObject parcel : missingParcels) {
                    tasks.add(getMissingPackagesInfoByParcelId(parcel));
                }

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resMP, errMP) -> {
                    try {
                        if (errMP != null) {
                            throw errMP;
                        }

                        future.complete(missingParcels);

                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> getMissingParcels(Integer shipmentId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(shipmentId).add(shipmentId);
        this.dbClient.queryWithParams(QUERY_GET_MISSING_PARCELS, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                future.complete(reply.result().getRows());

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> getMissingPackagesInfoByParcelId(JsonObject parcel){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(ID);
        this.dbClient.queryWithParams(QUERY_GET_MISSING_PACKAGES_INFO, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> results = reply.result().getRows();
                parcel.put(_PARCEL_PACKAGES, results);
                future.complete(true);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getLoadedInfo(Integer shipmentId){
        CompletableFuture<List<JsonObject>> future  = new CompletableFuture<>();
        getLoadedParcels(shipmentId).whenComplete((loadedParcels, errorParcels) -> {
            try {
                if (errorParcels != null){
                    throw errorParcels;
                }
                if (loadedParcels.isEmpty()) {
                    future.complete(new ArrayList<>());
                    return;
                }
                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                for (JsonObject parcel : loadedParcels) {
                    tasks.add(getLoadedPackagesInfoByParcelId(parcel));
                }

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resMP, errMP) -> {
                    try {
                        if (errMP != null) {
                            throw errMP;
                        }

                        future.complete(loadedParcels);

                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> getLoadedParcels(Integer shipmentId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(shipmentId);
        this.dbClient.queryWithParams(QUERY_GET_LOADED_PARCELS, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                future.complete(reply.result().getRows());

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> getLoadedPackagesInfoByParcelId(JsonObject parcel){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(ID);
        this.dbClient.queryWithParams(QUERY_GET_LOADED_PACKAGES_INFO, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> results = reply.result().getRows();
                parcel.put(_PARCEL_PACKAGES, results);
                future.complete(true);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private final static String QUERY_GET_MISSING_PARCELS = "SELECT\n" +
            "    p.id,\n" +
            "    p.waybill,\n" +
            "    p.parcel_tracking_code,\n" +
            "    p.total_packages,\n" +
            "    p.parcel_status,\n" +
            "    p.created_at,\n" +
            "    CONCAT(c.first_name, ' ', COALESCE(c.last_name, '')) AS customer,\n" +
            "    CONCAT(bo.prefix, ' - ', bd.prefix) AS segment\n" +
            "FROM shipments s\n" +
            "INNER JOIN parcels p ON p.terminal_origin_id = s.terminal_id\n" +
            "   AND p.parcel_status NOT IN (4)\n" +
            "INNER JOIN customer c ON c.id = p.customer_id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "   AND pp.package_status IN (0, 8, 10)\n" +
            "WHERE\n" +
            "   s.id = ?\n" +
            "GROUP BY p.id\n" +
            "UNION\n" +
            "SELECT\n" +
            "   p.id,\n" +
            "    p.waybill,\n" +
            "    p.parcel_tracking_code,\n" +
            "    p.total_packages,\n" +
            "    p.parcel_status,\n" +
            "    p.created_at,\n" +
            "    CONCAT(c.first_name, ' ', COALESCE(c.last_name, '')) AS customer,\n" +
            "    CONCAT(bo.prefix, ' - ', bd.prefix) AS segment\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "   AND p.parcel_status NOT IN (4)\n" +
            "INNER JOIN shipments s ON s.id = ?\n" +
            "INNER JOIN parcels_packages_tracking ppt ON ppt.parcel_package_id = pp.id\n" +
            "   AND ppt.action NOT IN ('incidence', 'printed')\n" +
            "    AND s.terminal_id = ppt.terminal_id\n" +
            "INNER JOIN customer c ON c.id = p.customer_id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "WHERE ppt.id = (\n" +
            "   SELECT \n" +
            "       MAX(ppt2.id) \n" +
            "   FROM parcels_packages_tracking ppt2 \n" +
            "    WHERE ppt2.parcel_package_id = pp.id\n" +
            "    AND ppt2.action NOT IN ('incidence', 'printed'))\n" +
            "AND action IN ('ready_to_transhipment', 'located')\n" +
            "AND (SELECT COUNT(DISTINCT srd.terminal_destiny_id) count FROM shipments s2\n" +
            "   INNER JOIN schedule_route sr ON sr.id = s2.schedule_route_id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   WHERE s2.id = s.id\n" +
            "   AND srd.terminal_destiny_id IN (\n" +
            "       SELECT ppt2.terminal_id FROM parcels_packages_tracking ppt2\n" +
            "       WHERE ppt2.parcel_id = p.id AND ppt2.action = 'loaded')) = 0\n" +
            "GROUP BY p.id;\n";

    private final static String QUERY_GET_LOADED_PARCELS = "SELECT\n" +
            "    p.id,\n" +
            "    p.waybill,\n" +
            "    p.parcel_tracking_code,\n" +
            "    p.total_packages,\n" +
            "    p.parcel_status,\n" +
            "    p.created_at,\n" +
            "    CONCAT(c.first_name, ' ', COALESCE(c.last_name, '')) AS customer,\n" +
            "    CONCAT(bo.prefix, ' - ', bd.prefix) AS segment,\n" +
            "    sppt.status\n" +
            "FROM shipments s\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.shipment_id = s.id\n" +
            "INNER JOIN parcels p ON p.id = sppt.parcel_id\n" +
            "   AND p.parcel_status NOT IN (4)\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "   AND pp.package_status NOT IN (0, 3, 4, 10)\n" +
            "INNER JOIN customer c ON c.id = p.customer_id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "WHERE\n" +
            "   s.id = ?\n" +
            "GROUP BY p.id;";

    private final static String QUERY_GET_MISSING_PACKAGES_INFO = "SELECT\n" +
            "   pp.id,\n" +
            "    pp.package_code,\n" +
            "    pp.package_status,\n" +
            "    pt.name\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN package_types pt ON pt.id = pp.package_type_id\n" +
            "WHERE\n" +
            "   pp.parcel_id = ?\n" +
            "    AND pp.package_status IN (0, 10);";

    private final static String QUERY_GET_LOADED_PACKAGES_INFO = "SELECT\n" +
            "   pp.id,\n" +
            "    pp.package_code,\n" +
            "    pp.package_status,\n" +
            "    pt.name\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN package_types pt ON pt.id = pp.package_type_id\n" +
            "WHERE\n" +
            "   pp.parcel_id = ?\n" +
            "    AND pp.package_status NOT IN (0, 3, 4, 10);";

}