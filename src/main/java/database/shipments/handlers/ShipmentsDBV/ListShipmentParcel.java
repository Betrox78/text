package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.shipments.ShipmentsDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ListShipmentParcel extends DBHandler<ShipmentsDBV> {
    public ListShipmentParcel(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {

        message.fail(500, "Service not implement");
    }

    public CompletableFuture<JsonObject> setParcelsOnScheduleRoute(JsonObject scheduleRoute, Integer shipmentId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        getShipmentParcels(shipmentId).setHandler(reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }

               scheduleRoute.put("parcels", reply.result());
               future.complete(scheduleRoute);
           } catch(Throwable t) {
               future.completeExceptionally(t);
           }
        });

        return future;
    }

    Future<JsonArray> getShipmentParcels(Integer shipmentId) {
        Future<JsonArray> future = Future.future();

        JsonArray params = new JsonArray()
                .add(shipmentId);

        dbClient.queryWithParams(QUERY_SHIPMENT_PARCELS, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> parcels = reply.result().getRows();

                List<Future> futures = parcels.stream()
                        .map(this::getParcelPackages)
                        .collect(Collectors.toList());

                CompositeFuture.all(futures).setHandler(replyPackages -> {
                   try {
                       if(replyPackages.failed()) {
                           throw replyPackages.cause();
                       }

                       future.complete(new JsonArray(parcels));

                   } catch(Throwable t) {
                       future.fail(t);
                   }
                });
            } catch(Throwable t) {
                future.fail(t);
            }
        });

        return future;
    }

    Future<JsonArray> getParcelPackages(JsonObject parcel) {
        Future<JsonArray> future = Future.future();

        Integer parcelId = parcel.getInteger("id");

        JsonArray params = new JsonArray()
                .add(parcelId);

        dbClient.queryWithParams(QUERY_PARCEL_PACKAGES, params, reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }

               JsonArray result = new JsonArray(reply.result().getRows());

               parcel.put("packages", result);
               future.complete(result);
           } catch (Throwable t) {
               future.fail(t);
           }
        });

        return future;
    }

    final String QUERY_SHIPMENT_PARCELS = "SELECT\n" +
            "p.id,\n" +
            "p.parcel_tracking_code,\n" +
            "p.parcel_status\n" +
            "FROM shipments_parcels sp\n" +
            "INNER JOIN parcels p ON p.id = sp.parcel_id\n" +
            "WHERE p.parcel_status IN (0,1,10) AND p.status = 1 AND sp.shipment_id = ?";

    final String QUERY_PARCEL_PACKAGES = "SELECT\n" +
            "pp.id,\n" +
            "pp.package_code,\n" +
            "pp.package_status,\n" +
            "pp.shipping_type\n" +
            "FROM parcels_packages pp\n" +
            "WHERE pp.package_status IN (0,1,5) AND pp.status = 1 AND pp.parcel_id = ?";
}
