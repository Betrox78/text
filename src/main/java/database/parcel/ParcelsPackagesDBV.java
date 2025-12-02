package database.parcel;

import database.commons.DBVerticle;
import database.parcel.handlers.ParcelsPackagesDBV.Cost;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class ParcelsPackagesDBV extends DBVerticle {


    public static final String CALCULATE_COST = "ParcelsPackagesDBV.calculateCost";
    public static final String CALCULATE_COST_V2 = "ParcelsPackagesDBV.calculateCostV2";
    public  static final String CANCEL_PARCEL_PACKAGE_BY_ID = "ParcelsPackagesDBV.cancelParcelPackageById";
    public static final String SCHEDULE_ROUTE_DESTINATION_BY_DATE = "ParcelsPackagesDBV.scheduleRouteDestination";
    public static final String PRINT_PACKAGE = "ParcelsPackagesDBV.printPackage";
    public static final String SCHEDULE_ROUTE_GUIAPP_DESTINATION_BY_DATE = "ParcelsPackagesDBV.scheduleRouteDestinationPP";
    public static final String CODE_DETAIL = "ParcelsPackagesDBV.codeDetail";
    @Override
    public String getTableName() {
        return "parcels_packages";
    }

    Cost cost;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.cost = new Cost(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case CALCULATE_COST:
                calculateCost(message);
                break;
            case CANCEL_PARCEL_PACKAGE_BY_ID:
                cancelParcelPackageById(message);
                break;
            case SCHEDULE_ROUTE_DESTINATION_BY_DATE:
                scheduleRouteDestinationByDate(message);
                break;
            case PRINT_PACKAGE:
                printPackage(message);
                break;
            case SCHEDULE_ROUTE_GUIAPP_DESTINATION_BY_DATE:
                scheduleRouteDestinationPP(message);
                break;
            case CODE_DETAIL:
                packageCodeDetail(message);
                break;
            case CALCULATE_COST_V2:
                this.cost.handle(message);
                break;
        }
    }

    public static final String PARCEL_PACKAGE_ID = "parcel_package_id";
    public static final String PACKAGE_CODE = "package_code";
    public static final String WEIGHT = "weight";
    public static final String HEIGHT = "height";
    public static final String WIDTH = "width";
    public static final String LENGTH = "length";
    public static final String SHIPPING_TYPE = "shipping_type";

    private void calculateCost(Message<JsonObject> message) {
        JsonObject pkg = message.body();
        String shippingType = pkg.getString("shipping_type");
        boolean guiapp = false;
        if(pkg.containsKey("guiapp")){
            if(pkg.getBoolean("guiapp")){
                guiapp=true;
            }
        }

        final boolean guiapp_flag = guiapp;
        // evaluar si se pasa y sacar datos hasta el limite max
        this.getPackagePrice(pkg).whenComplete((packagePrices, error) -> {
            try {
                if (error != null){
                    throw error;
                }
                JsonObject packageExcessPrice;
                int excessPriceId = 0;
                Double packageExcessPriceValue = 0.0;
                if (packagePrices.getJsonObject("packageExcedentPrice") != null){
                    packageExcessPrice = packagePrices.getJsonObject("packageExcedentPrice");
                    excessPriceId = packageExcessPrice.getInteger("id");
                    packageExcessPriceValue = packageExcessPrice.getDouble("price");
                }
                JsonObject packagePrice = packagePrices.getJsonObject("packagePrice");
                Double packagePriceValue = packagePrice.getDouble("price");

                Double finalPackageExcessPriceValue = packageExcessPriceValue;
                int finalExcessPriceId = excessPriceId;
                this.getPackagePriceByKM(pkg).whenComplete((result, stThrow) -> {
                    try {
                        if (stThrow != null){
                            throw stThrow;
                        }
                        JsonObject packagePriceKm = result.getJsonObject("packagePriceKm");
                        Double packagePriceKmValue = packagePriceKm.getDouble("price");
                        Double excessCost = (finalPackageExcessPriceValue *packagePriceKmValue);
                        getFrozenCost(shippingType, packagePriceValue, packagePriceKmValue, packagePrice, packagePriceKm, pkg).whenComplete((res,err)->{
                            try {
                                if (err != null){
                                    throw err;
                                }
                                if(!res.containsKey("shippingCost")){
                                    throw new Exception("No se pudo calcular el costo");
                                }
                                Double finalShippingCost = res.getDouble("shippingCost");
                                getPackingCost(pkg).whenComplete((pp, tt) -> {
                                    try {
                                        if(tt != null){
                                            throw tt;
                                        }
                                        Double guiapp_excess = 0.0;
                                        if(guiapp_flag) {
                                            guiapp_excess = pkg.getDouble("guiapp_excess") != null ? pkg.getDouble("guiapp_excess") : 0.0;
                                        }

                                        message.reply(new JsonObject()
                                                .put("cost", finalShippingCost)
                                                .put(DISCOUNT, 0.00)
                                                .put(TOTAL_AMOUNT, UtilsMoney.round(finalShippingCost, 2))
                                                .put(_DISTANCE_KM, result.getDouble(_DISTANCE_KM))
                                                .put("price", packagePriceValue)
                                                .put("package_price_id", packagePrice.getInteger("id"))
                                                .put("package_price_name", packagePrice.getString("name_price"))
                                                .put("price_km", packagePriceKmValue)
                                                .put("package_price_km_id", packagePriceKm.getInteger("id"))
                                                .put("package_price_distance_id", packagePriceKm.getInteger(ID))
                                                .put("packing_cost", pkg.getDouble("packing_cost"))
                                                .put("excess_price_id", finalExcessPriceId != 0 ? finalExcessPriceId : null)
                                                .put("excess_price", finalPackageExcessPriceValue)
                                                .put("excess_cost", excessCost + guiapp_excess));
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
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });

    }
    private CompletableFuture<JsonObject> getFrozenCost(String ShippingType, Double packagePriceValue, Double packagePriceKmValue, JsonObject packagePrice, JsonObject packagePriceKm, JsonObject paquete){
        CompletableFuture<JsonObject> future = new CompletableFuture();
        try{
            switch (ShippingType) {
                case "courier":
                    double amountC = ((packagePriceValue * packagePriceKmValue));
                    double costC;
                    if (Objects.nonNull(packagePriceKm.getDouble("RS"))) {
                        Double amountKM = packagePriceKm.getDouble("RS");
                        costC = UtilsMoney.round(amountC > amountKM ? amountC : amountKM, 2);
                    } else {
                        costC = UtilsMoney.round(amountC, 2);
                    }
                    future.complete(new JsonObject().put("shippingCost", costC));
                    break;
                case "parcel":
                    String packageName = packagePrice.getString("name_price");
                    if (packageName.equals("R81") || packageName.equals("R82") || packageName.equals("R83") || packageName.equals("R8E")) {
                        // Falta aqui
                        Double priceKg = packagePriceKm.getDouble("price_kg");
                        Double priceCubic = packagePriceKm.getDouble("price_cubic");
                        Double amountKg = UtilsMoney.round((priceKg * paquete.getDouble("weight")), 2);
                        Double amountCubic = UtilsMoney.round((priceCubic * paquete.getDouble("linear_volume")), 2);
                        if (amountKg > amountCubic) {
                            future.complete(new JsonObject().put("shippingCost", amountKg));
                        } else {
                            future.complete(new JsonObject().put("shippingCost", amountCubic));

                        }
                    } else {
                        Double amountTarifario = packagePriceKm.getDouble(packageName);
                        Double amount = (amountTarifario);
                        Double Cost = UtilsMoney.round(amount, 2);
                        future.complete(new JsonObject().put("shippingCost", Cost));
                    }
                    break;
                case "frozen":
                    this.dbClient.query(QUERY_GET_VALUE_FROZEN_COST, handler -> {
                        try {
                            if (handler.failed()) {
                                throw new Exception(handler.cause());
                            }
                            if (handler.result().getNumRows() > 0) {
                                Double.parseDouble(handler.result().getRows().get(0).getValue("value").toString());
                                Double amountF = ((packagePriceValue * packagePriceKmValue) + packagePriceKmValue + Double.parseDouble(handler.result().getRows().get(0).getValue("value").toString()));
                                Double costF = UtilsMoney.round(amountF, 2);
                                future.complete(new JsonObject().put("shippingCost", costF));
                            } else {
                                future.completeExceptionally(new Throwable("No se encontro el registro"));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            future.completeExceptionally(handler.cause());
                        }

                    });
                    break;
                case "pets":
                    this.dbClient.query(QUERY_GET_VALUE_PETS_COST, handler -> {
                        try {
                            if (handler.failed()) {
                                throw new Exception(handler.cause());
                            }
                            if (handler.result().getNumRows() > 0) {
                                Double.parseDouble(handler.result().getRows().get(0).getValue("value").toString());
                                Double amountPe = ((packagePriceValue * packagePriceKmValue) + packagePriceKmValue + Double.parseDouble(handler.result().getRows().get(0).getValue("value").toString()));
                                Double costPe = UtilsMoney.round(amountPe, 2);
                                future.complete(new JsonObject().put("shippingCost", costPe));
                            } else {
                                future.completeExceptionally(new Throwable("No se encontro el registro"));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            future.completeExceptionally(handler.cause());
                        }
                    });
                    break;
                default:
                    future.completeExceptionally(new Throwable("El shipping type no es valido"));
                    break;
            }
        }catch (Exception e){
            future.completeExceptionally(e.getCause());
        }
        return future;
    }
    private CompletableFuture<JsonObject> getPackagePrice(JsonObject pkg) {
        JsonObject returnJsonObject = new JsonObject();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String shippingType = pkg.getString("shipping_type");
        boolean guiapp=false;
        if(pkg.containsKey("guiapp")){
            if(pkg.getBoolean("guiapp")){
                pkg.remove("guiapp");
                guiapp=true;
            }
        }
        if (shippingType == null) {
            future.completeExceptionally(new Throwable("Package: Shipping type required"));
            return future;
        }

        if (shippingType.equals("courier")) {
            //query to obtain price by dimensions
            String QUERY_PACKAGE_PRICE = "SELECT * FROM package_price \n" +
                    "WHERE shipping_type = 'courier' \n" +
                    "ORDER BY price DESC LIMIT 1;";


            if(guiapp) {
                //comienza guia prepago
                QUERY_PACKAGE_PRICE = "SELECT * FROM pp_price \n" +
                        "WHERE shipping_type = 'courier' \n" +
                        "ORDER BY price DESC LIMIT 1;";
                this.dbClient.queryWithParams(QUERY_PACKAGE_PRICE, new JsonArray(), reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        List<JsonObject> results = reply.result().getRows();

                        if (results.isEmpty()) {
                            future.completeExceptionally(new Throwable("Package price not found"));
                        } else {
                            JsonObject packagePrice = results.get(0);
                            returnJsonObject.put("packagePrice", packagePrice);
                            future.complete(returnJsonObject);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(reply.cause());
                    }
                });
            }
            else{
                this.dbClient.queryWithParams(QUERY_PACKAGE_PRICE, new JsonArray(), reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        List<JsonObject> results = reply.result().getRows();

                        if (results.isEmpty()) {
                            future.completeExceptionally(new Throwable("Package price not found"));
                        } else {
                            JsonObject packagePrice = results.get(0);
                            returnJsonObject.put("packagePrice", packagePrice);
                            future.complete(returnJsonObject);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(reply.cause());
                    }
                });
            }

        } else {
            Float linearVolume = pkg.getFloat("linear_volume");
            Double weight = pkg.getDouble("weight");
            String _QUERY_MAXIMUM_RATE=QUERY_MAXIMUM_RATE;

            if (guiapp) {
                this.dbClient.query(_QUERY_MAXIMUM_RATE, replyMaxRate -> {
                    try {
                        if (replyMaxRate.failed()) {
                            throw new Exception(replyMaxRate.cause());
                        }
                        List<JsonObject> maxRate = replyMaxRate.result().getRows();

                        Float maxLinearVolume = maxRate.get(0).getFloat("max_m3");
                        Double maxWeight = maxRate.get(0).getDouble("max_weight");
                        if (weight <= (maxWeight * 2) && linearVolume <= (maxLinearVolume * 2)) {

                            Float linearParam = 0.0f;
                            Double weightParam = 0.0;

                            if (linearVolume > maxLinearVolume || weight > maxWeight) {
                                linearParam = maxLinearVolume;
                                weightParam = maxWeight;
                            } else {
                                linearParam = linearVolume;
                                weightParam = weight;
                            }

                            //if have excedent add return object with excedent price
                            this.getPackageMaximumPrice(linearParam, weightParam,true).whenComplete((packagePrice, errorP) -> {
                                if (errorP != null) {
                                    future.completeExceptionally(errorP);
                                } else {
                                    returnJsonObject.put("packagePrice", packagePrice);
                                    if (linearVolume > maxLinearVolume || weight > maxWeight) {
                                        Float excesslinearVolume = 0.0f;
                                        Double excessWeight = 0.0;
                                        if (linearVolume > maxLinearVolume) {
                                            excesslinearVolume = linearVolume - maxLinearVolume;
                                        }
                                        if (weight > maxWeight) {
                                            excessWeight = weight - maxWeight;
                                        }
                                        this.getPackageMaximumPrice(excesslinearVolume, excessWeight,true).whenComplete((packageExcedentPrice, errorE) -> {
                                            if (errorE != null) {
                                                future.completeExceptionally(errorE);
                                            } else {
                                                returnJsonObject.put("packageExcedentPrice", packageExcedentPrice);
                                                future.complete(returnJsonObject);
                                            }
                                        });
                                    } else {
                                        future.complete(returnJsonObject);
                                    }
                                }
                            });
                        } else {
                            future.completeExceptionally(new Throwable("Measurement limit exceeded"));
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(replyMaxRate.cause());
                    }
                });
            }else{
                this.dbClient.query(_QUERY_MAXIMUM_RATE, replyMaxRate -> {
                    try {
                        if (replyMaxRate.failed()) {
                            throw new Exception(replyMaxRate.cause());
                        }
                        List<JsonObject> maxRate = replyMaxRate.result().getRows();

                        Float maxLinearVolume = maxRate.get(0).getFloat("max_m3");
                        Double maxWeight = maxRate.get(0).getDouble("max_weight");
                        if (weight <= (maxWeight * 2) && linearVolume <= (maxLinearVolume * 2)) {
                            Float linearParam = 0.0f;
                            Double weightParam = 0.0;

                            if (linearVolume > maxLinearVolume || weight > maxWeight) {
                                linearParam = maxLinearVolume;
                                weightParam = maxWeight;
                            } else {
                                linearParam = linearVolume;
                                weightParam = weight;
                            }

                            //if have excedent add return object with excedent price
                            this.getPackageMaximumPrice(linearParam, weightParam,false).whenComplete((packagePrice, errorP) -> {
                                if (errorP != null) {
                                    future.completeExceptionally(errorP);
                                } else {
                                    returnJsonObject.put("packagePrice", packagePrice);
                                    if (linearVolume > maxLinearVolume || weight > maxWeight) {
                                        Float excesslinearVolume = 0.0f;
                                        Double excessWeight = 0.0;
                                        if (linearVolume > maxLinearVolume) {
                                            excesslinearVolume = linearVolume - maxLinearVolume;
                                        }
                                        if (weight > maxWeight) {
                                            excessWeight = weight - maxWeight;
                                        }
                                        this.getPackageMaximumPrice(excesslinearVolume, excessWeight,false).whenComplete((packageExcedentPrice, errorE) -> {
                                            if (errorE != null) {
                                                future.completeExceptionally(errorE);
                                            } else {
                                                returnJsonObject.put("packageExcedentPrice", packageExcedentPrice);
                                                future.complete(returnJsonObject);
                                            }
                                        });
                                    } else {
                                        future.complete(returnJsonObject);
                                    }
                                }
                            });
                        } else {
                            future.completeExceptionally(new Throwable("Measurement limit exceeded"));
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(replyMaxRate.cause());
                    }
                });
            }
        }
        return future;
    }
    private CompletableFuture<JsonObject> getPackageMaximumPrice(Float linearVolume, Double weight,boolean guiapp){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray paramLinear = new JsonArray().add(linearVolume).add(linearVolume);
        JsonArray paramWeight = new JsonArray().add(weight).add(weight);

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        if(guiapp){
            this.dbClient.queryWithParams(QUERY_LINEAR_VOLUME_GUIAPP, paramLinear, f1.completer());
            this.dbClient.queryWithParams(QUERY_WEIGHT_GUIAPP, paramWeight, f2.completer());
        }else{
            this.dbClient.queryWithParams(QUERY_LINEAR_VOLUME, paramLinear, f1.completer());
            this.dbClient.queryWithParams(QUERY_WEIGHT, paramWeight, f2.completer());
        }

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> resultsLinearVolume = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> resultsWeight = reply.result().<ResultSet>resultAt(1).getRows();

                JsonObject linearVolumePrice = resultsLinearVolume.get(0);
                JsonObject weightPrice = resultsWeight.get(0);

                Double linearVolumePriceValue = linearVolumePrice.getDouble("price");
                Double weightPriceValue = weightPrice.getDouble("price");

                JsonObject packagePrice = linearVolumePriceValue > weightPriceValue ? linearVolumePrice : weightPrice;

                future.complete(packagePrice);
            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }
    private  CompletableFuture<JsonObject> getPackagePriceByKM(JsonObject results) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        getDistanceKmByDestinationId(results)
                .whenComplete((JsonObject parcelPackageResult, Throwable error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(parcelPackageResult);
                    }
                });

        return  future;
    }

    private  CompletableFuture<JsonObject> packagePriceKM(JsonObject results){

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Double distanceKm = results.getDouble(_DISTANCE_KM);
        String QUERY = "SELECT * FROM package_price_km \n" +
                "WHERE min_km <= ? AND max_km >= ? \n" +
                "LIMIT 1;";
        JsonArray params = new JsonArray().add(distanceKm).add(distanceKm);

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    future.completeExceptionally(new Throwable("Package price km not found"));
                } else {
                    JsonObject pkgPriceKM = result.get(0);
                    results.put("packagePriceKm", pkgPriceKM);
                    future.complete(results);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }
    private  CompletableFuture<JsonObject> getDistanceKmByDestinationId(JsonObject results){

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer scheduleRouteDestinationId;
        JsonArray params = new JsonArray();
        if(results.containsKey("schedule_route_destination_id")){
            scheduleRouteDestinationId = results.getInteger("schedule_route_destination_id");
            params.add(scheduleRouteDestinationId);
        }else {
            params.add(results.getInteger("terminal_origin_id"));
            params.add(results.getInteger("terminal_destiny_id"));
            params.add(results.getInteger("terminal_destiny_id"));
            params.add(results.getInteger("terminal_origin_id"));
        }
        String QUERY_FINAL = results.containsKey("schedule_route_destination_id") ? QUERY_DISTANCE_KM_BY_DESTINATION_ID : QUERY_DISTANCE_KM_BY_TERMINALS_ID;
        this.dbClient.queryWithParams(QUERY_FINAL, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }

                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) {
                    System.out.println("Distance km not found");
                    future.completeExceptionally(new Throwable("Distance km not found"));
                } else {
                    JsonObject result = rows.get(0);
                    results.put(_DISTANCE_KM, result.getDouble(_DISTANCE_KM));

                    this.packagePriceKM(results)
                            .whenComplete((JsonObject parcelPackageResult, Throwable error) -> {
                                if (error != null) {
                                    future.completeExceptionally(error);
                                } else {
                                    parcelPackageResult.put(_DISTANCE_KM, result.getDouble(_DISTANCE_KM));
                                    future.complete(parcelPackageResult);
                                }
                            });
                }

            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });
        return future;

    }
    private  void cancelParcelPackageById(Message<JsonObject> message){

        JsonArray params= new JsonArray();
        params.add(message.body().getInteger("CANCEL_BY"));
        params.add(message.body().getString("parcelPackageId"));

        this.startTransaction(message,conn ->{
            conn.updateWithParams(CANCEL_PARCEL_PACKAGE,params,updateResult ->{
                try{
                    if(updateResult.failed()){
                        throw new Exception(updateResult.cause());
                    }
                    boolean updated = updateResult.result().getUpdated() == 1;

                    if(updated){
                        JsonArray parcelId = new JsonArray().add(message.body().getString("parcelPackageId"));

                        conn.queryWithParams(QUERY_PARCEL_PACKAGE_BY_ID,parcelId, parcelPackageResult-> {
                            try{
                                if(parcelPackageResult.failed()){
                                    throw  new Exception(parcelPackageResult.cause());
                                }
                                List<JsonObject> parcelsPackages = parcelPackageResult.result().getRows();
                                if(parcelsPackages.isEmpty()){
                                    this.rollback(conn,new Throwable("Element not found"),message);

                                } else {

                                    JsonObject parcelPackage = parcelsPackages.get(0);
                                    parcelPackage.put("CANCEL_BY",message.body().getInteger("CANCEL_BY"));

                                    this.insertParcelPackageTracking(conn,parcelPackage).whenComplete((result, stThrow) -> {
                                        if (stThrow != null) {
                                            this.rollback(conn,stThrow,message);
                                        } else {
                                            this.commit(conn,message,new JsonObject().put("updated",result));
                                        }
                                    });
                                }

                            }catch(Exception e){
                                this.rollback(conn,parcelPackageResult.cause(),message);

                            }

                        });

                    } else {
                        this.rollback(conn,new Throwable("Element not found"),message);
                    }


                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn,updateResult.cause(),message);
                }
            });
        });
    }

    private CompletableFuture<Boolean> insertParcelPackageTracking(SQLConnection conn, JsonObject parcelPackage) {

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        JsonObject parcelPackageTracking = new JsonObject();

        parcelPackageTracking.put("parcel_id",parcelPackage.getInteger("parcel_id"));
        parcelPackageTracking.put("parcel_package_id",parcelPackage.getInteger("id"));
        parcelPackageTracking.put("action","canceled");
        parcelPackageTracking.put("notes",parcelPackage.getString("notes"));
        parcelPackageTracking.put("status",4);
        parcelPackageTracking.put("created_by",parcelPackage.getInteger("CANCEL_BY"));
        String insert = this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking);

        conn.update(insert,(AsyncResult<UpdateResult> replyInsert) -> {
            try{
                if(replyInsert.failed()){
                    throw  new Exception(replyInsert.cause());
                }
                future.complete(replyInsert.succeeded());


            }catch(Exception e){
                future.completeExceptionally(replyInsert.cause());

            }

        });

        return future;
    }

    private CompletableFuture<JsonObject> calculateInsuranceCost(JsonObject parcelPackage) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer parcelAllowedId = parcelPackage.getInteger("parcel_allowed_id");

        Double insuranceValue = parcelPackage.getDouble("insurance_value");
        if (insuranceValue == null) {
            parcelPackage.put("insurance_amount", 0.0);
            future.complete(parcelPackage);
            return future;
        }

        if (parcelAllowedId == null) {
            future.completeExceptionally(new Throwable("Parcel allowed id required"));
            return future;
        }

        JsonArray params = new JsonArray()
                .add(parcelAllowedId);

        /*dbClient.queryWithParams(QUERY_PARCEL_ALLOWED_BY_ID, params, reply -> {
            if (reply.failed()) {
                future.completeExceptionally(reply.cause());
            } else {
                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) {
                    future.completeExceptionally(new Throwable("Parcel allowed: Not found"));
                } else {
                    Double insuranceAmount = 0.0;
                    JsonObject allowed = rows.get(0);
                    Boolean hasInsurance = allowed.getBoolean("has_insurance");
                    if (!hasInsurance) {
                        parcelPackage.put("insurance_amount", insuranceAmount);
                        future.complete(parcelPackage);
                    } else {
                        Double costPerRange = allowed.getDouble("cost_per_range");
                        Double costInsuranceRange = allowed.getDouble("cost_insurance_range");
                        Double insuranceMaxAmount = allowed.getDouble("insurance_max_amount");
                        Double value = insuranceValue > insuranceMaxAmount ? insuranceMaxAmount : insuranceValue;
                        insuranceAmount = value / costInsuranceRange * costPerRange;
                        parcelPackage.put("insurance_amount", insuranceAmount);
                        future.complete(parcelPackage);
                    }
                }
            }
        });*/

        return future;
    }

    private CompletableFuture<JsonObject> getPackingCost(JsonObject parcelPackage) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer packingsId = parcelPackage.getInteger("packing_id");

        if (packingsId == null) {
            parcelPackage.put("packing_cost", 0);
            future.complete(parcelPackage);
            return future;
        }

        JsonArray params = new JsonArray()
                .add(packingsId);

        dbClient.queryWithParams(QUERY_PACKINGS_BY_ID, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) {
                    future.completeExceptionally(new Throwable("Packings: Not found"));
                } else {
                    JsonObject packing = rows.get(0);
                    Double cost = packing.getDouble("cost", 0.0);
                    parcelPackage.put("packing_cost", cost);
                    future.complete(parcelPackage);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });

        return future;
    }

    private  void scheduleRouteDestinationByDate(Message<JsonObject> message) {

        JsonArray params = new JsonArray();
        JsonObject body = message.body();
        String startDate = body.getString("dateTravel");
        Boolean allowed_frozen = body.getBoolean("allowed_frozen");
        Boolean allowed_pets = body.getBoolean("allowed_pets");

        try {
            params.add(UtilsDate.sdfDataBase(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(startDate)));
            params.add(UtilsDate.addTime(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(startDate), 1200));
            params.add(body.getInteger("terminalOriginId"));
            params.add(body.getInteger("terminalDestinyId"));

            //String query = QUERY_DISTANCE_KM_BY_TERMINALS_ID;
            String query = QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE;
            if(allowed_frozen){
                query += " AND cv.allow_frozen = true";
            }
            if(allowed_pets){
                query += " AND cv.allow_pets = true";
            }

            dbClient.queryWithParams(query, params, reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    List<JsonObject> results = reply.result().getRows();
                    message.reply(new JsonArray(results));

                }catch(Exception e){
                    reportQueryError(message, reply.cause());

                }

            });

        } catch (ParseException ex) {
            reportQueryError(message, new Throwable("Invalid date: ".concat(startDate)));
        }
    }

    private void printPackage(Message<JsonObject> message) {

        JsonArray params = new JsonArray();
        JsonObject body = message.body();
        Integer updatedBy = body.getInteger("updated_by");

        params.add(body.getInteger("parcel_package_id"));

        this.startTransaction(message,conn -> {
            conn.queryWithParams(QUERY_PARCEL_PACKAGE_INFO, params, reply -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    JsonObject parcel_package = reply.result().getRows().get(0);
                    System.out.print(parcel_package.encodePrettily());
                    this.execUpdatePrintsCounter(conn, new JsonArray().add(parcel_package), "parcels_packages", updatedBy).whenComplete((resultUpdatePrintsCounter, errorPrintsCounter) -> {
                        try{
                            if (errorPrintsCounter != null){
                                this.rollback(conn, errorPrintsCounter, message);
                            } else {
                                this.insertTracking(conn, new JsonArray().add(parcel_package), "parcels_packages_tracking", "parcel_id", "parcel_package_id", null, "printed", updatedBy)
                                        .whenComplete((resultTracking, errorTracking) -> {
                                            try{
                                                if(errorTracking != null){
                                                    this.rollback(conn, errorTracking, message);
                                                } else {
                                                    this.commit(conn, message, parcel_package);
                                                }
                                            } catch (Exception e){
                                                this.rollback(conn, e, message);
                                            }
                                        });
                            }
                        } catch (Exception e){
                            this.rollback(conn, e, message);
                        }
                    });

                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, reply.cause() ,message);
                }
            });
        });
    }

    private  void scheduleRouteDestinationPP(Message<JsonObject> message) {

        JsonArray params = new JsonArray();
        JsonObject body = message.body();
        String startDate = body.getString("dateTravel");
        Boolean allowed_frozen = body.getBoolean("allowed_frozen");
        Boolean allowed_pets = body.getBoolean("allowed_pets");

        try {
            params.add(UtilsDate.sdfDataBase(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(startDate)));
            params.add(UtilsDate.addTime(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(startDate), 219000));
            params.add(body.getInteger("terminalOriginId"));
            params.add(body.getInteger("terminalDestinyId"));

            String query = QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE;
            if(allowed_frozen){
                query += " AND cv.allow_frozen = true";
            }
            if(allowed_pets){
                query += " AND cv.allow_pets = true";
            }
            dbClient.queryWithParams(query, params, reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    List<JsonObject> results = reply.result().getRows();
                    message.reply(new JsonArray(results));

                }catch(Exception e){
                    reportQueryError(message, reply.cause());

                }

            });

        } catch (ParseException ex) {
            reportQueryError(message, new Throwable("Invalid date: ".concat(startDate)));
        }
    }
    private  void packageCodeDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        String code = body.getString("code");
        JsonArray codeParams = new JsonArray().add(code);
        this.dbClient.queryWithParams(GET_PACKAGE_CODE_DETAIL, codeParams, reply-> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> branches = reply.result().getRows();
                message.reply(new JsonArray(branches));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private  static  final String QUERY_PARCEL_PACKAGE_BY_ID ="select * from parcels_packages where id =? limit 1;";

    private  static  final String QUERY_PACKINGS_BY_ID ="select * from packings where id =? limit 1;";

    private  static  final String  CANCEL_PARCEL_PACKAGE ="UPDATE parcels_packages SET package_status = 4 , updated_by = ? \n" +
            "  WHERE id = ? and package_status = 0;";

    private static final String QUERY_GET_VALUE_FROZEN_COST = "SELECT * FROM general_setting WHERE field = 'frozen_cost';";
    private static final String QUERY_GET_VALUE_PETS_COST = "SELECT * FROM general_setting WHERE field = 'pets_cost';";

    private static final String QUERY_SCHEDULE_ROUTE_DESTINATION_BY_DATE = "SELECT \n"
            + "	srd.id AS schedule_route_destination_id, \n"
            + "	srd.schedule_route_id AS schedule_route_id, \n"
            + "	srd.config_destination_id AS config_destination_id, \n"
            + "	srd.travel_date AS departure_date, \n"
            + "	srd.arrival_date AS arrival_date, \n"
            + "	cd.order_origin AS order_origin, \n"
            + "	cd.order_destiny AS order_destiny, \n"
            + "	cd.distance_km AS distance_km, \n"
            + " cd.terminal_origin_id AS terminal_origin_id, \n"
            + " cd.terminal_destiny_id AS terminal_destiny_id, \n"
            + " cd.travel_time, \n"
            + "	cr.discount_tickets AS discount_tickets, \n"
            + " sr.vehicle_id AS vehicle_id, \n"
            + " COALESCE(cv.allow_frozen, false) AS allow_frozen, \n"
            + " COALESCE(cv.allow_pets, false) AS allow_pets, \n"
            + " sr.config_schedule_id AS config_schedule_id, \n"
            + " sr.config_route_id AS config_route_id \n"
            + " FROM schedule_route_destination AS srd \n"
            + " INNER JOIN schedule_route AS sr \n"
            + " ON sr.id=srd.schedule_route_id \n"
            + " INNER JOIN config_destination AS cd \n"
            + " ON cd.id=srd.config_destination_id \n"
            + " INNER JOIN config_route AS cr \n"
            + " ON cr.id=cd.config_route_id \n"
            + " INNER JOIN vehicle AS v \n"
            + " ON v.id=sr.vehicle_id \n"
            + " LEFT JOIN config_vehicle AS cv \n"
            + " ON cv.id=v.config_vehicle_id \n"
            + " WHERE \n"
            + "	srd.travel_date BETWEEN ? AND ? \n"
            + "	AND srd.terminal_origin_id = ?  \n"
            + "	AND srd.terminal_destiny_id = ? \n"
            + " AND srd.status = 1 AND srd.destination_status IN ('scheduled', 'ready-to-load', 'loading')"
            + " AND sr.status = 1   AND sr.status_hide != 1 AND sr.schedule_status IN ('scheduled', 'ready-to-load', 'loading')";

    //query to obtain price by dimensions
    private static final String QUERY_LINEAR_VOLUME = "SELECT * FROM package_price \n" +
            "WHERE min_m3 <= ? AND max_m3 >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";
    //query to obtain price by weight
    private static final String QUERY_WEIGHT = "SELECT * FROM package_price \n" +
            "WHERE min_weight <= ? AND max_weight >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";
    //query to obtain price by dimensions
    private static final String QUERY_LINEAR_VOLUME_GUIAPP = "SELECT * FROM package_price \n" +
            "WHERE min_m3 <= ? AND max_m3 >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";
    //query to obtain price by weight
    private static final String QUERY_WEIGHT_GUIAPP = "SELECT * FROM package_price \n" +
            "WHERE min_weight <= ? AND max_weight >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";

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
    private static final String QUERY_MAXIMUM_RATE_GUIAPP = "SELECT \n"
            +" min_linear_volume, \n"
            +" max_linear_volume, \n"
            +" min_weight, \n"
            +" max_weight, \n"
            + "price, \n"
            +" currency_id \n"
            +" FROM pp_price \n"
            +" WHERE max_linear_volume = (SELECT MAX(max_linear_volume) FROM package_price \n"
            +" WHERE shipping_type = 'parcel' AND status = 1);";

    private static String QUERY_RATES = "SELECT name_price, " +
            "min_linear_volume, " +
            "max_linear_volume, " +
            "min_weight, " +
            "max_weight, " +
            "price, " +
            "currency_id \n" +
            "FROM package_price \n" +
            "WHERE shipping_type = 'parcel' AND status = 1;";

    private static final String QUERY_PARCEL_PACKAGE_INFO = "SELECT pp.id, pp.parcel_id, " +
            "pp.shipping_type, pp.package_code, pp.prints_counter, pp.package_type_id, pt.name AS package_type_name, " +
            "pp.weight, pp.height, pp.width, pp.length, pp.notes, pp.schedule_route_destination_id, a.created_at, TIMESTAMPDIFF(HOUR, a.created_at, a.promise_delivery_date) AS delivery_time, a.total_packages, a.pays_sender, " +
            "CONCAT(b.prefix, a.parcel_tracking_code) AS parcel_code, a.parcel_tracking_code, a.waybill, " +
            "a.sender_id, a.sender_name, a.sender_last_name, a.sender_phone, a.sender_email, a.sender_address_id, ca_sender.address AS sender_address, " +
            "a.addressee_id, a.addressee_name, a.addressee_last_name, a.addressee_phone, a.addressee_email, a.addressee_address_id, ca_addressee.address AS addressee_address, " +
            "b.prefix as terminal_origin_prefix, b.name as terminal_origin_name, b.address AS terminal_origin_address, " +
            "c.prefix as terminal_destiny_prefix, c.name as terminal_destiny_name, " +
            "CONCAT(e.name, ' ', e.last_name) as created_name, a.shipment_type, a.payment_condition " +
            "FROM parcels_packages as pp " +
            "INNER JOIN parcels a ON a.id = pp.parcel_id \n" +
            "LEFT JOIN package_types pt ON pt.id = pp.package_type_id \n" +
            "LEFT JOIN customer_addresses ca_sender ON ca_sender.id = a.sender_address_id \n" +
            "LEFT JOIN customer_addresses ca_addressee ON ca_addressee.id = a.addressee_address_id \n" +
            "inner join branchoffice b on b.id = a.terminal_origin_id \n" +
            "inner join branchoffice c on c.id = a.terminal_destiny_id \n" +
            "left join employee e on e.user_id = a.created_by \n" +
            "where pp.id = ?";

    public static final String QUERY_DISTANCE_KM_BY_DESTINATION_ID = "SELECT cd.*\n" +
            "FROM config_destination AS cd\n" +
            "JOIN schedule_route_destination AS srd\n" +
            "ON srd.config_destination_id = cd.id \n" +
            "WHERE srd.id = ?\n" +
            "LIMIT 1;";
    public static final String QUERY_DISTANCE_KM_BY_TERMINALS_ID = "SELECT ptd.distance_km AS distance_km\n" +
            "FROM package_terminals_distance AS ptd\n" +
            "WHERE (ptd.terminal_origin_id = ? AND ptd.terminal_destiny_id = ?) OR (ptd.terminal_origin_id = ? AND ptd.terminal_destiny_id = ?) \n" +
            "LIMIT 1;";
    public static final String GET_PACKAGE_CODE_DETAIL = "select pp.*, p.terminal_origin_id , p.terminal_destiny_id, pt.name as package_type_name from parcels_packages pp \n" +
            "\tinner join parcels p on p.id = pp.parcel_id \n" +
            "\tleft join package_types pt on pt.id = pp.package_type_id \n" +
            "\twhere package_code  = ?;";
}



