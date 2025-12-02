package database.parcel.handlers.ParcelsPackagesDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsPackagesDBV;
import database.parcel.enums.TYPE_SERVICE;
import database.parcel.handlers.ParcelsPackagesDBV.models.CostResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import utils.UtilsMoney;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class Cost extends DBHandler<ParcelsPackagesDBV> {

    public Cost(ParcelsPackagesDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject pkg = message.body();
        String shippingType = pkg.getString(_SHIPPING_TYPE);
        Float linearVolume = pkg.getFloat(_LINEAR_VOLUME);
        Double weight = pkg.getDouble(_WEIGHT);
        Integer terminalOriginId = pkg.getInteger(_TERMINAL_ORIGIN_ID);
        Integer terminalDestinyId = pkg.getInteger(_TERMINAL_DESTINY_ID);
        this.getPackagePrice(shippingType, linearVolume, weight).whenComplete((packagePrices, error) -> {
            try {
               if (error != null){
                   throw error;
               }
               JsonObject packageExcessPrice;
               int excessPriceId = 0;
               Double packageExcessPriceValue = 0.0;
               if (packagePrices.getJsonObject("packageExcedentPrice") != null){
                   packageExcessPrice = packagePrices.getJsonObject("packageExcedentPrice");
                   excessPriceId = packageExcessPrice.getInteger(ID);
                   packageExcessPriceValue = packageExcessPrice.getDouble(_PRICE);
               }
               JsonObject packagePrice = packagePrices.getJsonObject("packagePrice");
               Integer packagePriceId = packagePrice.getInteger(ID);
               Double packagePriceValue = packagePrice.getDouble(_PRICE);
               String packagePriceName = packagePrice.getString("name_price");

               Double finalPackageExcessPriceValue = packageExcessPriceValue;
               int finalExcessPriceId = excessPriceId;
               this.getDistanceKmByTerminals(terminalOriginId, terminalDestinyId).whenComplete((distanceKm, errorDistanceKm) -> {
                   try {
                       if (errorDistanceKm != null){
                           throw errorDistanceKm;
                       }
                       this.packagePriceKM(distanceKm).whenComplete((JsonObject packagePriceKm, Throwable errorPackagePriceKm) -> {
                           try {
                               if (errorPackagePriceKm != null) {
                                   throw errorPackagePriceKm;
                               }
                               Integer packagePriceKmId = packagePriceKm.getInteger(ID);
                               Double packagePriceKmValue = packagePriceKm.getDouble(_PRICE);
                               Double shippingCost = getCost(shippingType, packagePriceValue, packagePriceKmValue, packagePrice, packagePriceKm, weight, linearVolume);
                               message.reply(JsonObject.mapFrom(new CostResult(
                                       UtilsMoney.round(shippingCost, 2),
                                       UtilsMoney.round(shippingCost, 2),
                                       0.0,
                                       UtilsMoney.round(shippingCost, 2),
                                       distanceKm,
                                       packagePriceValue,
                                       packagePriceId,
                                       packagePriceName,
                                       packagePriceKmValue,
                                       packagePriceKmId,
                                       packagePriceKmId,
                                       finalExcessPriceId != 0 ? finalExcessPriceId : null,
                                       finalPackageExcessPriceValue,
                                       0.0
                               )));
                           } catch (Throwable t){
                               reportQueryError(message, t);
                           }
                       });
                   } catch (Throwable t){
                       reportQueryError(message, t);
                   }
               });
            } catch (Throwable t){
               reportQueryError(message, t);
            }
       });
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

    private CompletableFuture<JsonObject> getPackagePrice(String shippingType, Float linearVolume, Double weight) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject returnJsonObject = new JsonObject();
            if (Objects.isNull(shippingType)) {
                throw new Throwable("Package: Shipping type required");
            }

            if (shippingType.equals("courier")) {
                getPackagePriceCourier().whenComplete((packagePrice, errorCourierPackagePrice) -> {
                    try {
                        if (Objects.nonNull(errorCourierPackagePrice)) {
                            throw errorCourierPackagePrice;
                        }
                        returnJsonObject.put("packagePrice", packagePrice);
                        future.complete(returnJsonObject);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } else {
                getPackagePriceParcel(linearVolume, weight).whenComplete((packagePrice, errorParcelPackagePrice) -> {
                    try {
                        if (Objects.nonNull(errorParcelPackagePrice)) {
                            throw errorParcelPackagePrice;
                        }
                        returnJsonObject.mergeIn(packagePrice);
                        future.complete(returnJsonObject);
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

    private CompletableFuture<JsonObject> getPackagePriceCourier() {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.dbClient.query(QUERY_PACKAGE_PRICE, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Throwable("Package price not found");
                    }

                    JsonObject packagePrice = results.get(0);
                    future.complete(packagePrice);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getPackagePriceParcel(Float linearVolume, Double weight) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.dbClient.query(QUERY_MAXIMUM_RATE, replyMaxRate -> {
                try {
                    if (replyMaxRate.failed()) {
                        throw replyMaxRate.cause();
                    }
                    List<JsonObject> maxRate = replyMaxRate.result().getRows();

                    Float maxLinearVolume = maxRate.get(0).getFloat("max_m3");
                    Double maxWeight = maxRate.get(0).getDouble("max_weight");
                    if (weight > (maxWeight * 2) || linearVolume > (maxLinearVolume * 2)) {
                        throw new Throwable("Measurement limit exceeded");
                    }

                    Float linearParam;
                    Double weightParam;

                    if (linearVolume > maxLinearVolume || weight > maxWeight) {
                        linearParam = maxLinearVolume;
                        weightParam = maxWeight;
                    } else {
                        linearParam = linearVolume;
                        weightParam = weight;
                    }

                    this.getPackageMaximumPrice(linearParam, weightParam).whenComplete((packagePrice, errorP) -> {
                        try {
                            if (errorP != null) {
                                throw errorP;
                            }
                            JsonObject response = new JsonObject();
                            response.put("packagePrice", packagePrice);
                            if (linearVolume <= maxLinearVolume && weight <= maxWeight) {
                                future.complete(response);
                                return;
                            }

                            float excesslinearVolume = 0.0f;
                            double excessWeight = 0.0;
                            if (linearVolume > maxLinearVolume) {
                                excesslinearVolume = linearVolume - maxLinearVolume;
                            }
                            if (weight > maxWeight) {
                                excessWeight = weight - maxWeight;
                            }
                            this.getPackageMaximumPrice(excesslinearVolume, excessWeight).whenComplete((packageExcedentPrice, errorE) -> {
                                if (errorE != null) {
                                    future.completeExceptionally(errorE);
                                } else {
                                    response.put("packageExcedentPrice", packageExcedentPrice);
                                    future.complete(response);
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
        return future;
    }

    private Double getCost(String shippingType, Double packagePriceValue, Double packagePriceKmValue, JsonObject packagePrice, JsonObject packagePriceKm, Double weight, Float linearVolume) throws Throwable {
        switch (shippingType) {
            case "courier":
                double amountC = ((packagePriceValue * packagePriceKmValue));
                double costC;
                if (Objects.nonNull(packagePriceKm.getDouble("RS"))) {
                    Double amountKM = packagePriceKm.getDouble("RS");
                    costC = UtilsMoney.round(amountC > amountKM ? amountC : amountKM, 2);
                } else {
                    costC = UtilsMoney.round(amountC, 2);
                }
                return costC;
            case "parcel":
                String packageName = packagePrice.getString("name_price");
                if (packageName.equals("R81") || packageName.equals("R82") || packageName.equals("R83") || packageName.equals("R8E")) {
                    // Falta aqui
                    Double priceKg = packagePriceKm.getDouble("price_kg");
                    Double priceCubic = packagePriceKm.getDouble("price_cubic");
                    double amountKg = UtilsMoney.round((priceKg * weight), 2);
                    double amountCubic = UtilsMoney.round((priceCubic * linearVolume), 2);
                    return Math.max(amountKg, amountCubic);
                } else {
                    Double amount = (packagePriceKm.getDouble(packageName));
                    return UtilsMoney.round(amount, 2);
                }
            default:
                throw new Throwable("El shipping type no es valido");
        }
    }

    private CompletableFuture<JsonObject> getPackageMaximumPrice(Float linearVolume, Double weight){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray paramLinear = new JsonArray().add(linearVolume).add(linearVolume);
        JsonArray paramWeight = new JsonArray().add(weight).add(weight);

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        this.dbClient.queryWithParams(QUERY_LINEAR_VOLUME, paramLinear, f1.completer());
        this.dbClient.queryWithParams(QUERY_WEIGHT, paramWeight, f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try{
                if(reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> resultsLinearVolume = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> resultsWeight = reply.result().<ResultSet>resultAt(1).getRows();

                JsonObject linearVolumePrice = resultsLinearVolume.get(0);
                JsonObject weightPrice = resultsWeight.get(0);

                Double linearVolumePriceValue = linearVolumePrice.getDouble(_PRICE);
                Double weightPriceValue = weightPrice.getDouble(_PRICE);

                JsonObject packagePrice = linearVolumePriceValue > weightPriceValue ? linearVolumePrice : weightPrice;

                future.complete(packagePrice);
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getDistanceKmByTerminals(Integer terminalOriginId, Integer terminalDestinyId){
        CompletableFuture<Double> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(terminalOriginId).add(terminalDestinyId)
                .add(terminalDestinyId).add(terminalOriginId);
        this.dbClient.queryWithParams(QUERY_DISTANCE_KM_BY_TERMINALS_ID, params, reply -> {
            try{
                if(reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) {
                    throw new Throwable("Distance km not found");
                }
                JsonObject result = rows.get(0);
                future.complete(result.getDouble(_DISTANCE_KM));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> packagePriceKM(Double distanceKm){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(distanceKm).add(distanceKm);
        this.dbClient.queryWithParams(QUERY_GET_PACKAGE_PRICE_KM_BY_DISTANCE_KM, params, reply -> {
            try{
                if(reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Throwable("Package price km not found");
                }
                future.complete(result.get(0));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_MAXIMUM_RATE = "SELECT \n"
            +" min_linear_volume, \n"
            +" max_linear_volume, \n"
            +" min_weight, \n"
            +" max_weight, \n"
            +" min_m3, \n"
            +" max_m3, \n"
            + "price, \n"
            +" currency_id \n"
            +" FROM package_price \n"
            +" WHERE max_m3 = (SELECT MAX(max_m3) FROM package_price \n"
            +" WHERE shipping_type = 'parcel' AND status = 1);";

    private static final String QUERY_PACKINGS_BY_ID ="select * from packings where id =? limit 1;";

    private static final String QUERY_LINEAR_VOLUME = "SELECT * FROM package_price \n" +
            "WHERE min_m3 <= ? AND max_m3 >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";
    //query to obtain price by weight
    private static final String QUERY_WEIGHT = "SELECT * FROM package_price \n" +
            "WHERE min_weight <= ? AND max_weight >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";

    private static final String QUERY_DISTANCE_KM_BY_TERMINALS_ID = "SELECT ptd.distance_km AS distance_km\n" +
            "FROM package_terminals_distance AS ptd\n" +
            "WHERE (ptd.terminal_origin_id = ? AND ptd.terminal_destiny_id = ?) OR (ptd.terminal_origin_id = ? AND ptd.terminal_destiny_id = ?) \n" +
            "LIMIT 1;";

    private static final String QUERY_PACKAGE_PRICE = "SELECT * FROM package_price \n" +
            "WHERE shipping_type = 'courier' \n" +
            "ORDER BY price DESC LIMIT 1;";

    private static final String QUERY_GET_PACKAGE_PRICE_KM_BY_DISTANCE_KM = "SELECT * FROM package_price_km \n" +
            "WHERE min_km <= ? AND max_km >= ? \n" +
            "LIMIT 1;";
}
