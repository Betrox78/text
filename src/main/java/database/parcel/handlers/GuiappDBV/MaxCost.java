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

public class MaxCost extends DBHandler<GuiappDBV> {

    public MaxCost(GuiappDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String excessBy = body.getString(_EXCESS_BY);
            String basePackagePriceName = body.getString(_BASE_PACKAGE_PRICE_NAME);
            int terminalOriginId = body.getInteger(_TERMINAL_ORIGIN_ID);
            int terminalDestinyId = body.getInteger(_TERMINAL_DESTINY_ID);

            getPackagePriceKmCost(excessBy, terminalOriginId, terminalDestinyId, basePackagePriceName).whenComplete((cost, errPPKM) -> {
                try {
                    if (errPPKM != null) {
                        throw errPPKM;
                    }
                    message.reply(new JsonObject().put(_COST, cost));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<Double> getPackagePriceKmCost(String excessBy, int terminalOriginId, int terminalDestinyId, String basePackagePriceName) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        getTerminalsDistance(terminalOriginId, terminalDestinyId).whenComplete((distanceKm, errDKM) -> {
            try {
                if (errDKM != null) {
                    throw errDKM;
                }
                getPackagePriceKmByDistance(distanceKm).whenComplete((packagePriceKm, errPPKM) -> {
                    try {
                        if (errPPKM != null) {
                            throw errPPKM;
                        }

                        Integer priceId = packagePriceKm.getInteger(ID);
                        getPackagePriceKmCost(priceId, basePackagePriceName).whenComplete((ppKmCost, errPpKmCost) -> {
                            try {
                                if (errPpKmCost != null) {
                                    throw new Exception(errPpKmCost);
                                }

                                if (Objects.nonNull(ppKmCost)) {
                                    future.complete(ppKmCost);
                                    return;
                                }

                                Double priceKg = packagePriceKm.getDouble(_PRICE_KG);
                                Double priceCubic = packagePriceKm.getDouble(_PRICE_CUBIC);
                                Double price = excessBy.equals(_WEIGHT) ? priceKg : priceCubic;
                                if (Objects.isNull(price)) {
                                    throw new Exception("price by " + excessBy + " is null");
                                }

                                getPackagePriceMaxValue(excessBy, basePackagePriceName).whenComplete((maxValue, errMM3) -> {
                                    try {
                                        if (errMM3 != null) {
                                            throw errMM3;
                                        }

                                        future.complete(UtilsMoney.round(price * maxValue, 2));
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
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getPackagePriceKmByDistance(double distanceKm) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_KM_BY_DISTANCE, new JsonArray().add(distanceKm), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Package price km not found, distance: " + distanceKm);
                }
                future.complete(result.get(0));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getTerminalsDistance(int terminalOriginId, int terminalDestinyId) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(terminalOriginId).add(terminalDestinyId)
                .add(terminalOriginId).add(terminalDestinyId);
        dbClient.queryWithParams(QUERY_GET_DISTANCE_BETWEEN_TERMINALS, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Terminals distance not found in package_terminals_distance");
                }
                future.complete(result.get(0).getDouble(_DISTANCE_KM));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }



    private CompletableFuture<Double> getPackagePriceMaxValue(String excessBy, String basePackagePriceName) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_BY_NAME, new JsonArray().add(basePackagePriceName), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Base package price name not found");
                }

                Double doubleMaxWeight = result.get(0).getDouble(_MAX_WEIGHT);
                Double maxM3 = result.get(0).getDouble(_MAX_M3);
                Double maxValue;
                if(doubleMaxWeight.intValue() == 0) {
                    maxValue = excessBy.equals(_WEIGHT) ? doubleMaxWeight : maxM3;
                } else {
                    maxValue = excessBy.equals(_WEIGHT) ? doubleMaxWeight.intValue() : maxM3;
                }
                if (Objects.isNull(maxValue)) {
                    throw  new Exception("max value by "+ excessBy +" is null");
                }

                future.complete(maxValue);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getPackagePriceKmCost(Integer packagePriceKmId, String namePrice) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {

            String[] validNamePrices = {"RS", "R1", "R2", "R3", "R4", "R5", "R6", "R7"};
            if (!Arrays.asList(validNamePrices).contains(namePrice)) {
                future.complete(null);
            } else {
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
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }


    private final static String QUERY_GET_PACKAGE_PRICE_KM_BY_DISTANCE = "SELECT * FROM package_price_km WHERE ? BETWEEN min_km AND max_km AND status = 1;";

    private final static String QUERY_GET_DISTANCE_BETWEEN_TERMINALS = "SELECT distance_km FROM package_terminals_distance\n" +
            "WHERE (terminal_origin_id = ? AND terminal_destiny_id = ?)\n" +
            "OR (terminal_destiny_id = ? AND terminal_origin_id = ?);";

    private final static String QUERY_GET_PACKAGE_PRICE_BY_NAME = "SELECT id, max_weight, max_m3 FROM package_price where name_price = ?;";

    private static final String QUERY_GET_PACKAGE_PRICE_KM_COST = "SELECT %s FROM package_price_km WHERE id = ? and status = 1;";

}
