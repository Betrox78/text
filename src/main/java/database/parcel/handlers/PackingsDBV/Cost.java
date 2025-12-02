package database.parcel.handlers.PackingsDBV;

import database.commons.DBHandler;
import database.parcel.PackingsDBV;
import database.promos.handlers.PromosDBV.models.ParcelPacking;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsMoney;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class Cost extends DBHandler<PackingsDBV> {

    public Cost(PackingsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            Integer id = body.getInteger(_PACKING_ID);
            Integer quantity = body.getInteger(_QUANTITY);
            ParcelPacking parcelPacking = new ParcelPacking();
            parcelPacking.setPackingId(id);
            parcelPacking.setQuantity(quantity);

            getPackingCost(parcelPacking).whenComplete((resPC, errPC) -> {
                try {
                    if (errPC != null) {
                        throw errPC;
                    }
                    getIvaValue().whenComplete((ivaPercent, errIP) -> {
                        try {
                            if (errIP != null) {
                                throw errIP;
                            }

                            double unitPrice = parcelPacking.getUnitPrice() / (ivaPercent + 1);
                            double amountPacking = unitPrice * quantity;
                            double ivaPacking = amountPacking * ivaPercent;
                            parcelPacking.setUnitPrice(UtilsMoney.round(unitPrice, 2));
                            parcelPacking.setAmount(UtilsMoney.round(amountPacking, 2));
                            parcelPacking.setIva(UtilsMoney.round(ivaPacking, 2));
                            parcelPacking.setTotalAmount(UtilsMoney.round(amountPacking * (ivaPercent + 1), 2));

                            message.reply(JsonObject.mapFrom(parcelPacking));
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

    private CompletableFuture<Double> getPackingCost(ParcelPacking parcelPacking) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {
            int packingId = parcelPacking.getPackingId();
            JsonArray param = new JsonArray().add(packingId);
            this.dbClient.queryWithParams(QUERY_GET_PACKING_COST, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Packing ID not found: " + packingId);
                    }
                    double cost = result.get(0).getDouble(_COST);
                    parcelPacking.setUnitPrice(cost);
                    future.complete(cost);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Double> getIvaValue() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        this.dbClient.query("SELECT value FROM general_setting WHERE FIELD = 'iva';", reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Iva value not found");
                }
                future.complete(Double.parseDouble(result.get(0).getString(VALUE)) / 100);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_GET_PACKING_COST = "SELECT \n" +
            "   id, \n" +
            "    name, \n" +
            "    cost \n" +
            "FROM packings \n" +
            "WHERE id = ? \n" +
            "   AND status = 1 \n" +
            "   AND cost IS NOT NULL;";
}
