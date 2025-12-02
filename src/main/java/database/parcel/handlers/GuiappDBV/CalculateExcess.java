package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.parcel.GuiappDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsMoney;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class CalculateExcess extends DBHandler<GuiappDBV> {

    public CalculateExcess(GuiappDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            boolean costBreakdown = body.getBoolean(_COST_BREAKDOWN);
            String basePackagePriceName = body.getString(_BASE_PACKAGE_PRICE_NAME);
            int terminalOriginId = body.getInteger(_TERMINAL_ORIGIN_ID);
            int terminalDestinyId = body.getInteger(_TERMINAL_DESTINY_ID);
            String shippingType = body.getString(_SHIPPING_TYPE);
            Double weight = body.getDouble(_WEIGHT);
            Double height = body.getDouble(_HEIGHT);
            Double length = body.getDouble(_LENGTH);
            Double width = body.getDouble(_WIDTH);
            Float m3 = getVolumeM3(height, length, width);

            JsonObject response = new JsonObject();

            getPackagePriceKm(response, terminalOriginId, terminalDestinyId).whenComplete((packagePriceKm, errPPKM) -> {
               try {
                   if (errPPKM != null) {
                       throw errPPKM;
                   }

                   getPackagePriceByMeasures(weight, m3, shippingType).whenComplete((packagePrice, errPP) -> {
                       try {
                           if (errPP != null) {
                               throw errPP;
                           }

                           response.put(_PACKAGE_PRICE_ID, packagePrice.getInteger(ID));
                           response.put(_NAME_PRICE, packagePrice.getString(_NAME_PRICE));
                           Double priceKg = packagePriceKm.getDouble(_PRICE_KG);
                           Double priceCubic = packagePriceKm.getDouble(_PRICE_CUBIC);

                           if (Objects.isNull(priceKg)) {
                               throw new Exception("price_kg is null");
                           }
                           if (Objects.isNull(priceCubic)) {
                               throw new Exception("price_cubic is null");
                           }

                           getExcessByMeasures(response, priceKg, priceCubic, basePackagePriceName, weight, m3, shippingType).whenComplete((excessCost, errEC) -> {
                               try {
                                   if (errEC != null) {
                                       throw errEC;
                                   }
                                   if (costBreakdown) {
                                       this.getIvaValue().whenComplete((iva, errIva) -> {
                                           try {
                                               if (errIva != null) {
                                                   throw errIva;
                                               }
                                               response.put(_IVA, UtilsMoney.round(excessCost * iva, 2))
                                                .put(_EXCESS_COST, UtilsMoney.round(excessCost / (iva + 1), 2));
                                               message.reply(response);
                                           } catch (Throwable t) {
                                               reportQueryError(message, t);
                                           }
                                       });
                                   } else {
                                       response.put(_EXCESS_COST, UtilsMoney.round(excessCost, 2));
                                       message.reply(response);
                                   }
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
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private Float getVolumeM3(Double height, Double length, Double width) {
        return Float.valueOf(String.format("%.3f", (width * height * length) / 1000000));
    }

    private CompletableFuture<JsonObject> getPackagePriceKm(JsonObject response, int terminalOriginId, int terminalDestinyId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        getTerminalsDistance(terminalOriginId, terminalDestinyId).whenComplete((distanceKm, errDKM) -> {
            try {
                if (errDKM != null) {
                    throw errDKM;
                }
                response.put(_DISTANCE_KM, distanceKm);
                getPackagePriceKmByDistance(distanceKm).whenComplete((packagePriceKm, errPPKM) -> {
                    try {
                        if (errPPKM != null) {
                            throw errPPKM;
                        }
                        response.put(_PACKAGE_PRICE_KM_ID, packagePriceKm.getInteger(ID));
                        future.complete(packagePriceKm);
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

    private CompletableFuture<JsonObject> getPackagePriceByMeasures(double weight, double m3, String shippingType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String QUERY = QUERY_GET_PACKAGE_PRICE_BY_MEASURES;
        if (Objects.isNull(shippingType)) {
            QUERY = String.format(QUERY, " IN ('courier', 'parcel')");
        } else {
            QUERY = String.format(QUERY, " = ('"+shippingType+"')");
        }
        dbClient.queryWithParams(QUERY, new JsonArray().add(weight).add(m3), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Package price not found, weight: " + weight + " m3: " + m3);
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

    private CompletableFuture<Double> getExcessByMeasures(JsonObject response, double priceKg, double priceCubic, String basePackagePriceName, Double weight, Float m3, String shippingType) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        getPackagePriceByName(basePackagePriceName, shippingType).whenComplete((basePackagePrice, errBPP) -> {
            try {
                if (errBPP != null) {
                    throw errBPP;
                }
                int baseMaxWeight = basePackagePrice.getDouble(_MAX_WEIGHT).intValue();
                Double baseMaxM3 = basePackagePrice.getDouble(_MAX_M3);

                double excessWeight = weight - baseMaxWeight;
                double excessM3 = m3 - baseMaxM3;
                CompletableFuture<Double> getExcessCost;
                CompletableFuture<JsonObject> getExcessPackagePrice;

                if (excessWeight > 0 && excessM3 > 0) {
                    getExcessPackagePrice = null;
                    getExcessCost = getMaxCostExcess(response, priceKg, priceCubic, weight, excessWeight, m3, excessM3, shippingType);
                } else if (excessWeight > 0) {
                    response.put(_EXCESS_BY, _WEIGHT);
                    getExcessCost = getExcessCostByWeight(priceKg, excessWeight);
                    getExcessPackagePrice = getExcessPackagePriceByWeight(weight, shippingType);
                } else if(excessM3 > 0) {
                    response.put(_EXCESS_BY, _VOLUME);
                    getExcessCost = getExcessCostByM3(priceCubic, excessM3);
                    getExcessPackagePrice = getExcessPackagePriceByVolume(Double.valueOf(m3), shippingType);
                } else {
                    //Aunque no tenga excedente, se necesita identificar
                    //el costo mayor (peso o volumen) para los casos R8+
                    getExcessPackagePrice = null;
                    getExcessCost = getMaxCostExcess(response, priceKg, priceCubic, weight, 0, m3, 0, shippingType);
                }

                getExcessCost.whenComplete((excessCost, errCE) -> {
                    try {
                        if (errCE != null) {
                            throw errCE;
                        }
                        if (Objects.isNull(getExcessPackagePrice)) {
                            future.complete(excessCost);
                            return;
                        }

                        getExcessPackagePrice.whenComplete((packagePrice, errPP) -> {
                            try {
                                if (errPP != null) {
                                    throw errPP;
                                }

                                response.put(_PACKAGE_PRICE_ID, packagePrice.getInteger(ID));
                                response.put(_NAME_PRICE, packagePrice.getString(_NAME_PRICE));
                                future.complete(excessCost);

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

    private CompletableFuture<JsonObject> getPackagePriceByName(String basePackagePriceName, String shippingType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String QUERY = QUERY_GET_PACKAGE_PRICE_BY_NAME;
        JsonArray params = new JsonArray().add(shippingType);
        if (!shippingType.equals("parcel")) {
            QUERY = QUERY_GET_PACKAGE_PRICE_COURIER_BY_NAME;
        } else {
            params.add(basePackagePriceName);
        }
        dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Base package price name not found");
                }
                future.complete(result.get(0));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getMaxCostExcess(JsonObject response, double priceKg, double priceCubic, double weight, double excessWeight, double m3, double excessM3, String shippingType) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        getExcessCostByWeight(priceKg, excessWeight).whenComplete((costExcessWeight, errCEW) -> {
            try {
                if (errCEW != null) {
                    throw errCEW;
                }
                getExcessCostByM3(priceCubic, excessM3).whenComplete((costExcessM3, errCEM3) -> {
                    try {
                        if (errCEM3 != null) {
                            throw errCEM3;
                        }
                        CompletableFuture<JsonObject> getExcessPackagePrice;
                        String excessBy;
                        if (costExcessWeight <= 0 && costExcessM3 <= 0) {
                            getExcessPackagePrice = getPackagePriceByMaxValue(weight, m3, shippingType);
                            excessBy = _WEIGHT;
                        } else if (costExcessWeight > costExcessM3) {
                            getExcessPackagePrice = getExcessPackagePriceByWeight(weight, shippingType);
                            excessBy = _WEIGHT;
                        } else {
                            getExcessPackagePrice = getExcessPackagePriceByVolume(m3, shippingType);
                            excessBy = _VOLUME;
                        }

                        getExcessPackagePrice.whenComplete((packagePrice, errPP) -> {
                            try {
                                if (errPP != null) {
                                    throw errPP;
                                }

                                response.put(_PACKAGE_PRICE_ID, packagePrice.getInteger(ID));
                                response.put(_NAME_PRICE, packagePrice.getString(_NAME_PRICE));
                                response.put(_EXCESS_BY, excessBy);
                                future.complete(Double.max(costExcessWeight, costExcessM3));

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

    private CompletableFuture<JsonObject> getExcessPackagePriceByWeight(Double weight, String shippingType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_BY_WEIGHT, new JsonArray().add(shippingType).add(weight), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Base package price name not found");
                }
                future.complete(result.get(0));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getExcessPackagePriceByVolume(Double volumeParam, String shippingType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        BigDecimal volume = new BigDecimal(volumeParam).setScale(3, RoundingMode.DOWN);
        dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_BY_VOLUME, new JsonArray().add(shippingType).add(volume.doubleValue()), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Base package price name not found");
                }
                future.complete(result.get(0));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getPackagePriceByMaxValue(Double weight, Double volumeParam, String shippingType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        BigDecimal volume = new BigDecimal(volumeParam).setScale(3, RoundingMode.DOWN);
        JsonArray params = new JsonArray()
                .add(shippingType)
                .add(weight)
                .add(volume.doubleValue());
        dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_BY_MAX_VALUE, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Base package price name not found");
                }
                future.complete(result.get(0));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getExcessCostByWeight(double priceKg, double excessWeight) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        future.complete(UtilsMoney.round(priceKg * excessWeight, 0));
        return future;
    }

    private CompletableFuture<Double> getExcessCostByM3(double priceCubic, double excessM3) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        future.complete(UtilsMoney.round(priceCubic * excessM3, 0));
        return future;
    }

    private CompletableFuture<Double> getIvaValue() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        this.dbClient.query(QUERY_GET_IVA_VALUE, reply -> {
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

    private final static String QUERY_GET_PACKAGE_PRICE_KM_BY_DISTANCE = "SELECT * FROM package_price_km WHERE shipping_type != 'inhouse' AND ? BETWEEN min_km AND max_km AND status = 1;";

    private final static String QUERY_GET_PACKAGE_PRICE_BY_MEASURES = "SELECT * FROM package_price\n" +
            "WHERE (? BETWEEN min_weight AND max_weight\n" +
            "OR ? BETWEEN min_m3 AND max_m3)\n" +
            "AND shipping_type %s\n" +
            "AND status = 1\n" +
            "ORDER BY price DESC LIMIT 1;";

    private final static String QUERY_GET_DISTANCE_BETWEEN_TERMINALS = "SELECT distance_km FROM package_terminals_distance\n" +
            "WHERE (terminal_origin_id = ? AND terminal_destiny_id = ?)\n" +
            "OR (terminal_destiny_id = ? AND terminal_origin_id = ?);";

    private final static String QUERY_GET_PACKAGE_PRICE_BY_NAME = "SELECT id, max_weight, max_m3 FROM package_price WHERE shipping_type = ? AND name_price = ?;";
    private final static String QUERY_GET_PACKAGE_PRICE_COURIER_BY_NAME = "SELECT id, max_weight, max_m3 FROM package_price WHERE shipping_type = ? LIMIT 1;";

    private final static String QUERY_GET_PACKAGE_PRICE_BY_WEIGHT = "SELECT * FROM package_price WHERE shipping_type = ? AND ? BETWEEN min_weight AND max_weight;";

    private final static String QUERY_GET_PACKAGE_PRICE_BY_VOLUME = "SELECT * FROM package_price WHERE shipping_type = ? AND ? BETWEEN min_m3 AND max_m3;";

    private final static String QUERY_GET_PACKAGE_PRICE_BY_MAX_VALUE = "SELECT * FROM package_price \n" +
            "WHERE shipping_type = ? \n" +
            "   AND ((? BETWEEN min_weight AND max_weight) \n" +
            "   OR (? BETWEEN min_m3 AND max_m3)) \n" +
            " ORDER BY id DESC LIMIT 1;";

    private static final String QUERY_GET_IVA_VALUE = "SELECT value FROM general_setting WHERE FIELD = 'iva';";

}
