package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.parcel.GuiappDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsMoney;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class PercentDiscount extends DBHandler<GuiappDBV> {

    public PercentDiscount(GuiappDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer packagePriceId = body.getInteger(_PACKAGE_PRICE_ID);
            Integer packagePriceKmId = body.getInteger(_PACKAGE_PRICE_KM_ID);
            Double unitPrice = body.getDouble(_UNIT_PRICE);

            getPackagePriceName(packagePriceId).whenComplete((namePrice, errNamePrice) -> {
                try {
                    if (errNamePrice != null) {
                        throw new Exception(errNamePrice);
                    }

                    if (Objects.isNull(namePrice)) {
                        message.reply(new JsonObject().put(_PERCENT_DISCOUNT_APPLIED, 0.0));
                        return;
                    }

                    getPackagePriceKmCost(packagePriceKmId, namePrice).whenComplete((ppKmCost, errPpKmCost) -> {
                        try {
                            if (errPpKmCost != null) {
                                throw new Exception(errPpKmCost);
                            }

                            if (Objects.isNull(ppKmCost)) {
                                message.reply(new JsonObject().put(_PERCENT_DISCOUNT_APPLIED, 0.0));
                                return;
                            }

                            Double percentDiscount = UtilsMoney.round(((ppKmCost - unitPrice) / ppKmCost) * 100, 2);
                            message.reply(new JsonObject().put(_PERCENT_DISCOUNT_APPLIED, percentDiscount));

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

    private CompletableFuture<String> getPackagePriceName(Integer packagePriceId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(packagePriceId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_INFO, params, replyPP -> {
                try {
                    if (replyPP.failed()) {
                        throw replyPP.cause();
                    }

                    List<JsonObject> packagePrices = replyPP.result().getRows();
                    if (packagePrices.isEmpty()) {
                        throw new Exception("Package price not found");
                    }

                    JsonObject packagePrice = packagePrices.get(0);
                    String packagePriceName = packagePrice.getString(_NAME_PRICE);

                    String[] validNamePrices = {"RS", "R1", "R2", "R3", "R4", "R5", "R6", "R7"};
                    if (!Arrays.asList(validNamePrices).contains(packagePriceName)) {
                        future.complete(null);
                    } else {
                        future.complete(packagePriceName);
                    }

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Double> getPackagePriceKmCost(Integer packagePriceKmId, String namePrice) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(packagePriceKmId);
            String QUERY = String.format(QUERY_GET_PACKAGE_PRICE_KM_COST, namePrice);
            this.dbClient.queryWithParams(QUERY, params, replyPP -> {
                try {
                    if (replyPP.failed()) {
                        throw replyPP.cause();
                    }

                    List<JsonObject> packagePricesKm = replyPP.result().getRows();
                    if (packagePricesKm.isEmpty()) {
                        throw new Exception("Package price km not found");
                    }

                    JsonObject packagePrice = packagePricesKm.get(0);
                    Double cost = packagePrice.getDouble(namePrice);
                    if (Objects.isNull(cost)) {
                        future.complete(0.0);
                    } else {
                        future.complete(cost);
                    }

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }


    private static final String QUERY_GET_PACKAGE_PRICE_INFO = "SELECT id, name_price FROM package_price WHERE id = ?;";

    private static final String QUERY_GET_PACKAGE_PRICE_KM_COST = "SELECT %s FROM package_price_km WHERE id = ? and status = 1;";

}
