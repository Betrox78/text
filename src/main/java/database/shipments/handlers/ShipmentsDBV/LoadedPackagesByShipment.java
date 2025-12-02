package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class LoadedPackagesByShipment extends DBHandler<ShipmentsDBV> {

    public LoadedPackagesByShipment(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer shipmentId = body.getInteger(_SHIPMENT_ID);
            Integer trailerId = body.getInteger(_TRAILER_ID);

            this.getLoadedInfo(shipmentId, trailerId).whenComplete((loadedParcels, errL) -> {
                try {
                    if (errL != null) {
                        throw errL;
                    }
                    message.reply(new JsonArray(loadedParcels));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getLoadedInfo(Integer shipmentId, Integer trailerId){
        CompletableFuture<List<JsonObject>> future  = new CompletableFuture<>();
        getLoadedParcels(shipmentId, trailerId).whenComplete((loadedParcels, errorParcels) -> {
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
                    tasks.add(getLoadedPackagesInfoByParcelId(parcel, shipmentId));
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

    private CompletableFuture<List<JsonObject>> getLoadedParcels(Integer shipmentId, Integer trailerId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        String QUERY = QUERY_GET_LOADED_PARCELS;
        JsonArray params = new JsonArray().add(shipmentId);
        if (Objects.nonNull(trailerId)) {
            QUERY += " AND sppt.trailer_id = ?";
            params.add(trailerId);
        }
        QUERY = QUERY.concat(" GROUP BY p.id;");
        this.dbClient.queryWithParams(QUERY, params, reply -> {
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

    private CompletableFuture<Boolean> getLoadedPackagesInfoByParcelId(JsonObject parcel, Integer shipmentId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(_PARCEL_ID);
        Integer trailerId = parcel.getInteger(_TRAILER_ID);
        String QUERY = QUERY_GET_LOADED_PACKAGES_INFO;
        JsonArray params = new JsonArray().add(parcelId).add(shipmentId);
        if (Objects.nonNull(trailerId)) {
            QUERY += " AND sppt.trailer_id = ?";
            params.add(trailerId);
        }
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> results = reply.result().getRows();
                parcel.put(_PACKAGES, results);
                future.complete(true);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private final static String QUERY_GET_LOADED_PARCELS = "SELECT\n" +
            "   p.id AS parcel_id,\n" +
            "    p.parcel_status,\n" +
            "    CONCAT(bo.prefix, ' - ', bd.prefix) AS segment,\n" +
            "    p.parcel_tracking_code,\n" +
            "    p.waybill,\n" +
            "    p.total_packages,\n" +
            "    sppt.trailer_id,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) customer_full_name,\n" +
            "    true AS show_packages\n" +
            "FROM shipments_parcel_package_tracking sppt\n" +
            "INNER JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN parcels p ON p.id = sppt.parcel_id\n" +
            "   AND p.parcel_status NOT IN ("+ PARCEL_STATUS.CANCELED.ordinal() +")\n" +
            "INNER JOIN shipments_parcels sp ON sp.shipment_id = s.id\n" +
            "   AND sp.parcel_id = p.id\n" +
            "INNER JOIN customer c ON c.id = p.customer_id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "WHERE s.id = ?\n";

    private final static String QUERY_GET_LOADED_PACKAGES_INFO = "SELECT\n" +
            "   pp.id,\n" +
            "    pp.package_code,\n" +
            "    pp.package_status,\n" +
            "    pt.name AS package_type,\n" +
            "    pp.height,\n" +
            "    pp.length,\n" +
            "    pp.weight,\n" +
            "    pp.width,\n" +
            "    pp.shipping_type\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN package_types pt ON pt.id = pp.package_type_id\n" +
            "LEFT JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_package_id = pp.id\n" +
            "WHERE pp.parcel_id = ?\n" +
            "   AND sppt.shipment_id = ? \n" +
            "   AND pp.package_status IN ("+PACKAGE_STATUS.LOADED.ordinal()+") \n";

}