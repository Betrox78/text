package database.parcel.handlers.GuiappDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import database.money.PaybackDBV;
import database.money.PaymentDBV;
import database.parcel.ParcelDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.parcel.handlers.ParcelsPackagesDBV.models.ParcelPackage;
import database.promos.PromosDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.json.JSONObject;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static database.parcel.ParcelDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsDate.*;

public class Exchange extends DBHandler<ParcelDBV> {

    public Exchange(ParcelDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        // Nota: Falta validar tanto rango de paquetes, como vlidacion de codigo de guiapp que se canjean y descontar las guias usadas  fecha:11/11/2021
        JsonObject parcel = message.body().copy();
        JsonArray codeGuiapp;
        try {
            codeGuiapp = parcel.getJsonArray("codesGuiapp");
            List<CompletableFuture<JsonObject>> validCodeGuiappTasks = new ArrayList<>();
            int parcel_packages_size= parcel.getJsonArray("parcel_packages").size();
            for (int i = 0; i < codeGuiapp.size(); i++) {
                for (int x = 0; x <parcel_packages_size ; x++) {
                    if(parcel.getJsonArray("parcel_packages").getJsonObject(x).containsKey("guiapp_code")){
                        if (parcel.getJsonArray("parcel_packages").getJsonObject(x).getString("guiapp_code").equalsIgnoreCase(codeGuiapp.getJsonObject(i).getString("guiapp_code"))) {
                            validCodeGuiappTasks.add(validCodeGuiapp(codeGuiapp.getJsonObject(i)));
                            parcel.getJsonArray("parcel_packages").getJsonObject(x).remove("guiapp_code");
                        }
                    }
                }
            }
            CompletableFuture.allOf(validCodeGuiappTasks.toArray(new CompletableFuture[codeGuiapp.size()])).whenComplete((ps, pt) -> {
                try {
                    if (pt != null) {
                        throw pt;
                    }
                    parcel.remove("codesGuiapp");
                    this.startTransaction(message, (SQLConnection conn) ->
                        this.register(conn, parcel).whenComplete((resultRegister, errorRegister) -> {
                            try {
                                if (errorRegister != null){
                                    throw errorRegister;
                                }
                                Integer parcelId = resultRegister.getInteger(ID);
                                this.endRegisterGuiapp(conn, codeGuiapp, parcelId).whenComplete((resultRegisterGuiap, errorRegisterGuiapp) -> {
                                    try {
                                        if (errorRegisterGuiapp != null){
                                            throw errorRegisterGuiapp;
                                        }
                                        this.commit(conn, message, resultRegister);
                                    } catch (Throwable t){
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                this.rollback(conn, t, message);
                            }
                    }));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject>  validCodeGuiapp(JsonObject guiapp){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonArray params = new JsonArray().add(guiapp.getString("guiapp_code"));
            dbClient.queryWithParams(GET_CODE_GUIAPP,params, reply ->{
                try{
                    if(reply.succeeded()){
                        List<JsonObject> result = reply.result().getRows();
                        future.complete(new JsonObject().put("data",result));

                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void radEadBody(JsonObject parcel, JsonObject serviceRadEad) {
        if(parcel.containsKey("parcel_rad")){
            parcel.put("parcel_rad",0.0);
        }

        if(parcel.getBoolean("isRad") != null) {
            String stringoRad = parcel.toString();
            JSONObject jObjRad = new JSONObject(stringoRad);
            Object radObj = jObjRad.get("parcel_rad");
            serviceRadEad.put("is_rad", (Boolean) parcel.remove("isRad")).put("zip_code", (Integer) parcel.remove("zip_code_rad")).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "RAD");

            if(radObj instanceof Integer ){
                serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_rad"));
            } else if (radObj instanceof Double ){
                serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_rad"));
            }else
            {
                parcel.remove("parcel_rad");
                serviceRadEad.put("service_amount", 0.00);
            }

            parcel.put("shipment_type", "RAD/OCU");

        }

        if(parcel.getBoolean("isEad") != null){
            String stringoEad = parcel.toString();
            JSONObject jObj = new JSONObject(stringoEad);
            Object aObj = jObj.get("parcel_ead");
            serviceRadEad.put("is_ead",(Boolean) parcel.remove("isEad")).put("zip_code",(Integer) parcel.remove("zip_code_ead")).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "EAD");
            if(aObj instanceof Integer){
                serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_ead"));
            }else if (aObj instanceof Double){
                serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_ead"));
            }

            parcel.put("shipment_type", "EAD");
        }

        if(parcel.getBoolean("isRadEad") != null){
            String stringoRadEad = parcel.toString();
            JSONObject jObjRadEad = new JSONObject(stringoRadEad);
            Object objRadEad = jObjRadEad.get("parcel_rad_ead");

            serviceRadEad.put("is_rad_ead",(Boolean) parcel.remove("isRadEad")).put("zip_code", 0).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "RAD-EAD");
            if(objRadEad instanceof Integer){
                serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_rad_ead"));
            } else if (objRadEad instanceof Double){
                serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_rad_ead"));
            }

            parcel.put("shipment_type", "RAD/EAD");

        }
    }

    private CompletableFuture<Integer> createParcel(SQLConnection conn, JsonObject parcel, int cashRegisterId, boolean internalCustomer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        this.dbVerticle.generateCompuestID("parcel", cashRegisterId, internalCustomer).whenComplete((resultCompuestId, errorCompuestId) -> {
            try {
                if (errorCompuestId != null) {
                    throw errorCompuestId;
                }
                parcel.put("waybill", resultCompuestId);
                this.getPromiseDeliveryDate(parcel).whenComplete((promiseDeliveryDate, errPDD) -> {
                    try {
                        if (errPDD != null) {
                            throw errPDD;
                        }
                        if(Objects.nonNull(promiseDeliveryDate)) {
                            parcel.put(_PROMISE_DELIVERY_DATE, UtilsDate.sdfDataBase(promiseDeliveryDate));
                        }
                        GenericQuery gen = this.generateGenericCreate(parcel);
                        conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> parcelReply) -> {
                            try {
                                if (parcelReply.failed()) {
                                    throw parcelReply.cause();
                                }
                                final int parcelId = parcelReply.result().getKeys().getInteger(0);
                                future.complete(parcelId);
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

    private CompletableFuture<Date> getPromiseDeliveryDate(JsonObject parcel) {
        CompletableFuture<Date> future = new CompletableFuture<>();
        try {
            int terminalOriginId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
            int terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
            String shipmentType = parcel.getString(_SHIPMENT_TYPE, "OCU");
            JsonObject body = new JsonObject()
                    .put(_TERMINAL_ORIGIN_ID, terminalOriginId)
                    .put(_TERMINAL_DESTINY_ID, terminalDestinyId)
                    .put(_SHIPMENT_TYPE, shipmentType);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_GET_PROMISE_DELIVERY_DATE);
            this.getVertx().eventBus().send(ParcelDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    JsonObject result = (JsonObject) reply.result().body();
                    String promiseDeliveryDateString = result.getString(_PROMISE_DELIVERY_DATE);
                    Date promiseDeliveryDate = null;
                    if (Objects.nonNull(promiseDeliveryDateString)) {
                        promiseDeliveryDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(promiseDeliveryDateString);
                    }
                    future.complete(promiseDeliveryDate);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insuranceValidations(Double insuranceValue) throws ParseException {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        if (Objects.nonNull(insuranceValue) && insuranceValue > 0) {
            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<ResultSet> f3 = Future.future();

            this.getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "insurance_percent"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            this.getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "max_insurance_value"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            String now = format_yyyy_MM_dd(new Date());
            this.dbClient.queryWithParams(QUERY_INSURANCE_VALIDATION, new JsonArray().add(now), f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject field1 = reply.result().<Message<JsonObject>>resultAt(0).body();
                    JsonObject field2 = reply.result().<Message<JsonObject>>resultAt(1).body();
                    List<JsonObject> field3 = reply.result().<ResultSet>resultAt(2).getRows();

                    if (field3.isEmpty()) {
                        throw new Exception("There are no insurance policies available");
                    }

                    int insuranceId = field3.get(0).getInteger(ID);
                    double insurancePercent = Double.parseDouble(field1.getString("value"));
                    int maxInsuranceValue = Integer.parseInt(field2.getString("value"));
                    double insuranceAmount = UtilsMoney.round(insuranceValue * (insurancePercent / 100), 2);
                    boolean hasInsurance = insuranceAmount > 0;

                    future.complete(new JsonObject()
                            .put("insurance_id", insuranceId)
                            .put("max_insurance_value", maxInsuranceValue)
                            .put("insurance_amount", insuranceAmount)
                            .put("has_insurance", hasInsurance));

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } else {
            future.complete(new JsonObject().put("has_insurance", false));
        }

        return future;
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject parcel) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            boolean internalCustomer = parcel.containsKey(INTERNAL_CUSTOMER) ? (Boolean) parcel.remove(INTERNAL_CUSTOMER) : false;
            Boolean isInternalParcel = parcel.getBoolean("is_internal_parcel", false);
            Boolean paysSender;
            parcel.remove("guiapp_excess");
            parcel.remove("isGuiappCanje");
            parcel.put("iva_percent",0.0).put(_PARCEL_IVA,0.0);
            try {
                paysSender = parcel.getBoolean(PAYS_SENDER);
            } catch (Exception e){
                paysSender = parcel.getInteger(PAYS_SENDER).equals(1);
            }
            parcel.put(PAYS_SENDER, paysSender);
            Double insuranceValue = parcel.getDouble("insurance_value");
            Integer cashOutId = (Integer) parcel.remove(CASHOUT_ID);
            Integer cashRegisterId = parcel.getInteger(CASH_REGISTER_ID);
            JsonObject customerCreditData = (JsonObject) parcel.remove("customer_credit_data");
            final Boolean is_complement = parcel.containsKey("is_complement") ? ((Boolean) parcel.remove("is_complement")) : false;
            final Boolean is_credit = parcel.containsKey("is_credit") ? ((Boolean) parcel.remove("is_credit")) : false;
            JsonArray parcelPackages = (JsonArray) parcel.remove("parcel_packages");
            JsonArray parcelPackings = (JsonArray) parcel.remove("parcel_packings");
            JsonArray payments = (JsonArray) parcel.remove("payments");
            JsonObject cashChange = (JsonObject) parcel.remove("cash_change");
            final double ivaPercent = (Double) parcel.remove("iva_percent");
            final double parcelIvaPercent = (Double) parcel.remove(_PARCEL_IVA);
            final int currencyId = (Integer) parcel.remove("currency_id");
            final Integer createdBy = parcel.getInteger(CREATED_BY);
            parcel.put(_TOTAL_PACKAGES, parcelPackages.size());
            JsonObject serviceRadEad = new JsonObject();
            radEadBody(parcel, serviceRadEad);
            Boolean finalPaysSender = paysSender;
            boolean isPendingCollection = (boolean) parcel.remove(_IS_PENDING_COLLECTION);
            if(isPendingCollection) {
                parcel.put(_PARCEL_STATUS, PARCEL_STATUS.PENDING_COLLECTION.ordinal());
            }

            List<CompletableFuture<Boolean>> cityValidations = new ArrayList<>();
            cityValidations.add(this.comapreCityId("addressee", parcel.getInteger(_ADDRESSEE_ID), parcel.getInteger(_TERMINAL_DESTINY_ID), is_complement));
            cityValidations.add(this.comapreCityId("sender", parcel.getInteger(_SENDER_ID), parcel.getInteger(_TERMINAL_ORIGIN_ID), is_complement));
            CompletableFuture.allOf(cityValidations.toArray(new CompletableFuture[cityValidations.size()])).whenComplete((rCityValidations, errCityValidations) -> {
                try {
                    if (errCityValidations != null) {
                        throw errCityValidations;
                    }
                    createParcel(conn, parcel, cashRegisterId, internalCustomer).whenComplete((parcelId, errorCreateParcel) -> {
                        try {
                            if (errorCreateParcel != null) {
                                throw errorCreateParcel;
                            }

                            parcel.put(ID, parcelId);
                            Double distanceKM = parcelPackages.getJsonObject(0).getDouble(_DISTANCE_KM);
                            PaybackDBV objPayback = new PaybackDBV();
                            objPayback.calculatePointsParcel(conn, distanceKM, parcelPackages.size(), false).whenComplete((resultCalculate, error) -> {
                                try {
                                    if (error != null) {
                                        throw error;
                                    }

                                    Double paybackMoney = resultCalculate.getDouble("money");
                                    Double paybackPoints = resultCalculate.getDouble("points");

                                    List<CompletableFuture<JsonObject>> packagesTasks = new ArrayList<>();
                                    for (int i = 0; i < parcelPackages.size(); i++) {
                                        packagesTasks.add(registerPackagesInParcel(conn, parcelPackages.getJsonObject(i), parcelId, finalPaysSender, createdBy, parcel.getInteger("terminal_origin_id"), isInternalParcel, isPendingCollection));
                                    }

                                    CompletableFuture.allOf(packagesTasks.toArray(new CompletableFuture[parcelPackages.size()])).whenComplete((ps, pt) -> {
                                        try {
                                            if (pt != null) {
                                                throw pt;
                                            }
                                            CompletableFuture.allOf(parcelPackings.stream()
                                                        .map(p -> registerPackingsInParcel(conn, (JsonObject) p, parcelId, createdBy))
                                                        .toArray(CompletableFuture[]::new))
                                                .whenComplete((ks, kt) -> {
                                                    try {
                                                        if (kt != null) {
                                                            throw kt;
                                                        }

                                                        JsonObject parcelTotalAmount = this.getParcelTotalAmount(parcel, parcelPackages, parcelPackings, serviceRadEad, isInternalParcel);
                                                        Double amount = parcelTotalAmount.getDouble(_AMOUNT);
                                                        Double totalAmount = parcelTotalAmount.getDouble(_TOTAL_AMOUNT);
                                                        Double extraCharges = parcelTotalAmount.getDouble(_EXTRA_CHARGES);

                                                        insuranceValidations(insuranceValue).whenComplete((insuranceObj, errInsurance) -> {
                                                            try {
                                                                if (errInsurance != null) {
                                                                    throw errInsurance;
                                                                }

                                                                Double finalDiscount = parcelTotalAmount.getDouble(_DISCOUNT);
                                                                boolean hasInsurance = insuranceObj.getBoolean("has_insurance");
                                                                parcel.put("has_insurance", hasInsurance);
                                                                if (hasInsurance) {
                                                                    int maxInsuranceValue = insuranceObj.getInteger("max_insurance_value");
                                                                    double insuranceAmount = insuranceObj.getDouble(_INSURANCE_AMOUNT);
                                                                    int insuranceId = insuranceObj.getInteger("insurance_id");

                                                                    parcel.put("insurance_id", insuranceId);
                                                                    if (insuranceAmount > maxInsuranceValue) {
                                                                        parcel.put(_INSURANCE_AMOUNT, maxInsuranceValue);
                                                                    } else {

                                                                        parcel.put(_INSURANCE_AMOUNT, insuranceAmount);
                                                                    }
                                                                }

                                                                double subTotal = totalAmount - this.getIva(totalAmount, ivaPercent - parcelIvaPercent);
                                                                double iva = this.getIva(totalAmount, ivaPercent);
                                                                double parcelIva = subTotal * (parcelIvaPercent / 100);

                                                                parcel.put(_EXTRA_CHARGES, extraCharges)
                                                                        .put(_AMOUNT, amount)
                                                                        .put(_DISCOUNT, finalDiscount)
                                                                        .put(_IVA, iva)
                                                                        .put(_PARCEL_IVA, parcelIva)
                                                                        .put(_TOTAL_AMOUNT, totalAmount)
                                                                        .put(UPDATED_BY, createdBy)
                                                                        .put(UPDATED_AT, sdfDataBase(new Date()));

                                                                this.endRegister(conn, cashOutId, parcel, parcelId, createdBy, payments, is_complement,
                                                                        cashChange, ivaPercent, currencyId, parcelPackages, parcelPackings, objPayback,
                                                                        paybackPoints, paybackMoney, internalCustomer, isInternalParcel, is_credit, customerCreditData , serviceRadEad).whenComplete((resultRegister, errorRegister) -> {
                                                                    try {
                                                                        if (errorRegister != null){
                                                                            throw errorRegister;
                                                                        }
                                                                        future.complete(resultRegister);
                                                                    } catch (Throwable t){
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
        return future;
    }

    private JsonObject getParcelTotalAmount(JsonObject parcel, JsonArray parcelPackages, JsonArray parcelPackings, JsonObject serviceObject, boolean isInternalParcel){

        Double amount = 0.00;
        Double discount = 0.00;
        Double totalAmount = 0.00;
        Double extraCharges = parcel.getDouble(EXTRA_CHARGES, 0.0);

        if (parcel.containsKey(AMOUNT) && parcel.containsKey(PromosDBV.DISCOUNT) && parcel.containsKey(TOTAL_AMOUNT)) {
            amount = parcel.getDouble(_AMOUNT);
            discount = parcel.getDouble("discount");
            totalAmount = parcel.getDouble("total_amount");
        } else {
            for (int i = 0; i < parcelPackages.size(); i++) {
                JsonObject parcelPackage = parcelPackages.getJsonObject(i);
                amount += parcelPackage.containsKey(_AMOUNT) ? parcelPackage.getDouble(_AMOUNT) : parcelPackage.getDouble("cost");
                discount += parcelPackage.getDouble("discount");
                totalAmount += parcelPackage.getDouble("total_amount");
            }
        }

        for (int i = 0; i < parcelPackings.size(); i++) {
            JsonObject parcelPacking = parcelPackings.getJsonObject(i);
            if (isInternalParcel) {
                discount += parcelPacking.getDouble("total_amount", 0.0);
            }
            extraCharges += parcelPacking.getDouble("total_amount", 0.0);

        }

        boolean isRadEad=false;
        if((serviceObject.getBoolean("is_rad") != null  || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null)) {
            isRadEad=true;
            if (isInternalParcel) {
                discount += serviceObject.getDouble("service_amount");
            }
            extraCharges += serviceObject.getDouble("service_amount");
        }

        String promoGuipp=serviceObject.getString("guiapp")!=null?serviceObject.getString("guiapp"):"";
        if ((promoGuipp.equals("GUIAPPALL") ||  promoGuipp.equals("GUIASPP501KM") )  && isRadEad ) {
            discount+=serviceObject.getDouble("service_amount");
            totalAmount -= serviceObject.getDouble("service_amount");
        }

        return new JsonObject()
                .put(_AMOUNT, amount)
                .put("discount", discount)
                .put("total_amount", totalAmount)
                .put("extra_charges", extraCharges);
    }

    private Double getIva(Double amount, Double ivaPercent){
        return amount - (amount / (1 + (ivaPercent/100)));
    }

    private CompletableFuture<Boolean> comapreCityId(String typeCustomer, Integer customerId, Integer terminalId, Boolean is_complement){

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            if(is_complement){
                future.complete(true);
                return future;
            }
            String QUERY_GET_CITY = null;
            if (typeCustomer.equals("sender")){
                QUERY_GET_CITY = QUERY_GET_PARCEL_CITY_SENDER;
            } else if (typeCustomer.equals("addressee")){
                QUERY_GET_CITY = QUERY_GET_PARCEL_CITY_ADDRESSEE;
            }

            this.dbClient.queryWithParams(QUERY_GET_CITY, new JsonArray().add(customerId),replyCustomer ->{
                try{
                    if(replyCustomer.failed()){
                        throw new Exception(replyCustomer.cause());
                    }
                    List<JsonObject> result = replyCustomer.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("city_id of " + typeCustomer + " not found");
                    }
                    int customerCityId = result.get(0).getInteger("city_id");
                    this.dbClient.queryWithParams(QUERY_GET_TERMINAL_CITY, new JsonArray().add(terminalId),replyTerminal ->{
                        try {
                            if (replyTerminal.failed()){
                                throw replyTerminal.cause();
                            }
                            List<JsonObject> resultTerminal = replyCustomer.result().getRows();
                            if (resultTerminal.isEmpty()){
                                throw new Exception("city_id of " + typeCustomer + " not found");
                            }
                            int terminalCityId = resultTerminal.get(0).getInteger("city_id");
                            if (customerCityId != terminalCityId){
                                throw new Exception("terminal_id: " + terminalId + " and addressee city_id: " + terminalCityId + " compared do not match");
                            }
                            future.complete(true);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });

                }catch (Exception ex){
                    future.completeExceptionally(ex);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerPackagesInParcel(SQLConnection conn, JsonObject parcelPackage, Integer parcelId, Boolean paysSender, Integer createdBy, Integer terminalId, Boolean isInternalParcel, boolean isPendingCollection) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        parcelPackage.put(_PARCEL_ID, parcelId);
        parcelPackage.put(CREATED_BY, createdBy);
        parcelPackage.put(_PACKAGE_CODE, UtilsID.generateID("P"));
        if (isPendingCollection) {
            parcelPackage.put(_PACKAGE_STATUS, PACKAGE_STATUS.PENDING_COLLECTION.ordinal());
        }

        Double amount = parcelPackage.getDouble(AMOUNT);
        Double discount = isInternalParcel ? amount : parcelPackage.getDouble(PromosDBV.DISCOUNT);
        Double totalAmount = isInternalParcel ? 0.00 : parcelPackage.getDouble(TOTAL_AMOUNT, amount);
        parcelPackage.put(AMOUNT, amount);
        parcelPackage.put(PromosDBV.DISCOUNT, discount);
        parcelPackage.put(TOTAL_AMOUNT, totalAmount);

        this.insertParcelPackage(conn, parcelPackage.copy(), parcelId, createdBy, paysSender, terminalId, isPendingCollection).whenComplete((parcelPackageId, stThrow) -> {
            try {
                if (stThrow != null) {
                    throw stThrow;
                }

                JsonArray incidences = parcelPackage.containsKey("packages_incidences") ? (JsonArray) parcelPackage.remove("packages_incidences") : new JsonArray();
                this.insertParcelPackageIncidences(conn, incidences, parcelId, parcelPackageId, createdBy).whenComplete((replyInc, errInc) -> {
                    try {
                        if (errInc != null) {
                            throw errInc;
                        }
                        future.complete(parcelPackage);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Integer> insertParcelPackage(SQLConnection conn, JsonObject parcelPackage, Integer parcelId, Integer createdBy, Boolean paysSender, Integer terminalId, boolean isPendingCollection){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        String notes = parcelPackage.getString("notes");

        if (isPendingCollection) {
            parcelPackage.put(_PACKAGE_STATUS, PACKAGE_STATUS.PENDING_COLLECTION.ordinal());
        }

        JsonObject parcelPackageObj = JsonObject.mapFrom(parcelPackage.mapTo(ParcelPackage.class));
        GenericQuery create = this.generateGenericCreate("parcels_packages", parcelPackageObj);

        conn.updateWithParams(create.getQuery(), create.getParams(), (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                final int parcelPackageId = reply.result().getKeys().getInteger(0);

                this.addParcelPackageTracking(conn, parcelId, parcelPackageId, notes, createdBy, paysSender, terminalId, isPendingCollection).whenComplete((s, t) -> {
                    try {
                        if (t != null){
                            throw new Exception(t);
                        }
                        future.complete(parcelPackageId);
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });

            }catch (Exception ex){
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> insertParcelPackageIncidences(SQLConnection conn, JsonArray incidences, Integer parcelId, Integer parcelPackageId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<GenericQuery> inserts = new ArrayList<>();
        for(int i = 0; i < incidences.size(); i++) {
            JsonObject incidence = incidences.getJsonObject(i);
            incidence.put(_PARCEL_ID, parcelId);
            incidence.put(_PARCEL_PACKAGE_ID, parcelPackageId);
            incidence.put(CREATED_BY, createdBy);
            inserts.add(this.generateGenericCreate("parcels_incidences", incidence));
        }
        List<JsonArray> params = inserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());

        if (inserts.isEmpty()) {
            future.complete(true);
        } else {
            conn.batchWithParams(inserts.get(0).getQuery(), params, (AsyncResult<List<Integer>> ar) -> {
                if(ar.succeeded()) {
                    future.complete(true);
                } else {
                    future.completeExceptionally(ar.cause());
                }
            });
        }
        return future;
    }

    private CompletableFuture<JsonObject> addParcelPackageTracking(SQLConnection conn, Integer parcelId, int parcelPackageId, String notes, Integer createdBy, Boolean paysSender, Integer terminalId, boolean isPendingCollection) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            List<GenericQuery> trackingInserts = new ArrayList<>();
            JsonObject parcelPackageTracking = new JsonObject()
                    .put(_PARCEL_ID, parcelId)
                    .put(_PARCEL_PACKAGE_ID, parcelPackageId)
                    .put(_ACTION, PARCELPACKAGETRACKING_STATUS.REGISTER.getValue())
                    .put(_TERMINAL_ID, terminalId)
                    .put(_NOTES, notes)
                    .put(CREATED_BY, createdBy);
            trackingInserts.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));

            if(paysSender) {
                parcelPackageTracking.put(_ACTION, PARCELPACKAGETRACKING_STATUS.PAID.getValue());
                trackingInserts.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));
            }

            parcelPackageTracking.put(_ACTION,
                    isPendingCollection ? PARCELPACKAGETRACKING_STATUS.PENDING_COLLECTION.getValue()
                            : PARCELPACKAGETRACKING_STATUS.IN_ORIGIN.getValue());
            trackingInserts.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));

            List<JsonArray> params = trackingInserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());

            conn.batchWithParams(trackingInserts.get(0).getQuery(), params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(parcelPackageTracking);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerPackingsInParcel(SQLConnection conn, JsonObject parcelPacking, Integer parcelId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        parcelPacking.put(_PARCEL_ID, parcelId)
                .put(CREATED_BY, createdBy);

        Integer packingsId = parcelPacking.getInteger("packing_id");

        if (packingsId == null) {
            future.complete(new JsonObject());
            return future;
        }

        JsonArray params = new JsonArray()
                .add(packingsId);

        conn.queryWithParams(QUERY_PACKINGS_BY_ID, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) {
                    future.completeExceptionally(new Throwable("Packing: Not found"));
                } else {
                    JsonObject packing = rows.get(0);
                    Double cost = packing.getDouble("cost", 0.0);
                    Integer quantity = parcelPacking.getInteger("quantity");
                    Double amount = cost * quantity;
                    parcelPacking.put("unit_price", cost);
                    parcelPacking.put(_AMOUNT, amount);
                    parcelPacking.put("discount", 0.0);
                    parcelPacking.put("total_amount", amount);

                    GenericQuery insert = this.generateGenericCreate("parcels_packings", parcelPacking);
                    conn.updateWithParams(insert.getQuery(), insert.getParams(), replyInsert -> {
                        try {
                            if (replyInsert.failed()){
                                throw new Exception(replyInsert.cause());
                            }
                            future.complete(parcelPacking);
                        }catch(Exception e ){
                            future.completeExceptionally(e);
                        }
                    });
                }
            }catch (Exception ex){
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    protected CompletableFuture<JsonObject> endRegister(SQLConnection conn, Integer cashOutId, JsonObject parcel, Integer parcelId, Integer createdBy,
                                                        JsonArray payments, Boolean is_complement, JsonObject cashChange, Double ivaPercent, Integer currencyId, JsonArray parcelPackages,
                                                        JsonArray parcelPackings, PaybackDBV objPayback, Double paybackPoints, Double paybackMoney, Boolean internalCustomer,  Boolean isInternalParcel, Boolean is_credit, JsonObject customerCreditData, JsonObject radObject){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.internalCustomerValidation(internalCustomer,isInternalParcel, parcel).whenComplete((resultInternalCustomer, errorInternalCustomer) -> {
            try {
                if (errorInternalCustomer != null){
                    throw new Exception(errorInternalCustomer);
                }
                parcel.mergeIn(resultInternalCustomer, true);
                if (internalCustomer && isInternalParcel){
                    parcel.put(PAYS_SENDER, true);
                }

                boolean paysSender = parcel.getBoolean(PAYS_SENDER);
                Double totalAmount = parcel.getDouble(TOTAL_AMOUNT);
                double innerTotalAmount = UtilsMoney.round(totalAmount, 2);

                double creditBalance = 0;
                double debt = 0;

                if(is_credit){
                    parcel.put("debt", innerTotalAmount);
                }

                GenericQuery update = this.generateGenericUpdate("parcels", parcel);

                if (!paysSender) {

                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                        try {
                            if (replyUpdate.failed()){
                                throw replyUpdate.cause();
                            }
                            if (is_credit) {

                                this.updateCustomerCredit(conn, customerCreditData, debt, createdBy, creditBalance)
                                        .whenComplete((replyCustomer, errorCustomer) -> {
                                            try {
                                                if (errorCustomer != null) {
                                                    throw new Exception(errorCustomer);
                                                }
                                                this.insertService(conn, createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                                    try{
                                                        if (errorService != null){
                                                            throw errorService;
                                                        }

                                                        JsonObject result = new JsonObject().put(ID, parcelId);
                                                        result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                        future.complete(result);

                                                    } catch (Throwable t){
                                                        future.completeExceptionally(t);
                                                    }
                                                });

                                            } catch (Throwable t) {
                                                future.completeExceptionally(t);
                                            }
                                        });
                            } else {
                                JsonObject result = new JsonObject().put(ID, parcelId);
                                result.put("tracking_code", parcel.getString(_PARCEL_TRACKING_CODE));
                                this.insertService(conn, createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                    try{
                                        if (errorService != null){
                                            throw errorService;
                                        }

                                        future.complete(result);

                                    } catch (Throwable t){
                                        future.completeExceptionally(t);
                                    }
                                });

                            }
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    double totalPayments = 0.0;
                    if(payments == null && !is_complement && !internalCustomer && !isInternalParcel){
                        throw new Exception("No payment object was found");
                    }
                    if (payments == null && is_complement){
                        JsonObject result = new JsonObject().put(ID, parcelId);
                        result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                        future.complete(result);
                    } else {
                        final int pLen = payments == null ? 0 : payments.size();

                        for (int i = 0; i < pLen; i++) {
                            JsonObject payment = payments.getJsonObject(i);
                            Double paymentAmount = payment.getDouble(_AMOUNT);
                            if (paymentAmount == null || paymentAmount < 0.0) {
                                throw new Exception("Invalid payment amount: " + paymentAmount);
                            }
                            totalPayments += UtilsMoney.round(paymentAmount, 2);
                        }

                        innerTotalAmount=totalPayments > 0 ? totalPayments : 0.0;
                        this.insertTicket(conn, cashOutId, parcelId, innerTotalAmount, cashChange, createdBy, ivaPercent).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                            try {
                                if (ticketError != null) {
                                    throw ticketError;
                                }

                                this.insertTicketDetail(conn, ticket.getInteger(ID), createdBy, parcelPackages, parcelPackings, parcel, internalCustomer , radObject).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                    try {
                                        if (dError != null) {
                                            throw dError;
                                        }

                                        if (is_credit) {
                                            conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                try {
                                                    if (replyUpdate.failed()) {
                                                        throw replyUpdate.cause();
                                                    }
                                                    JsonObject paramMovPayback = new JsonObject()
                                                            .put(_CUSTOMER_ID, parcel.getInteger(_SENDER_ID))
                                                            .put("points", paybackPoints)
                                                            .put("money", paybackMoney)
                                                            .put("type_movement", "I")
                                                            .put("motive", "Envio de paquetería(sender)")
                                                            .put("id_parent", parcelId)
                                                            .put("employee_id", createdBy);
                                                    objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                                                        try {
                                                            if (errorMP != null) {
                                                                throw errorMP;
                                                            }

                                                            this.updateCustomerCredit(conn, customerCreditData, debt, createdBy, creditBalance)
                                                                    .whenComplete((replyCustomer, errorCustomer) -> {
                                                                        try{
                                                                            if (errorCustomer != null) {
                                                                                throw new Exception(errorCustomer);
                                                                            }

                                                                            this.insertService(conn, createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                                                                try{
                                                                                    if (errorService != null){
                                                                                        throw errorService;
                                                                                    }

                                                                                    JsonObject result = new JsonObject().put(ID, parcelId);
                                                                                    result.put("ticket_id", ticket.getInteger(ID))
                                                                                            .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                                    future.complete(result);

                                                                                } catch (Throwable t){
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
                                        } else {
                                            List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                            for (int i = 0; i < pLen; i++) {
                                                JsonObject payment = payments.getJsonObject(i);
                                                payment.put("ticket_id", ticket.getInteger(ID));
                                                pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcelId, cashOutId, createdBy, is_credit));
                                            }
                                            CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));

                                            allPayments.whenComplete((s, tt) -> {
                                                try {
                                                    if (tt != null) {
                                                        throw tt;
                                                    }
                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                        try {
                                                            if (replyUpdate.failed()) {
                                                                throw replyUpdate.cause();
                                                            }
                                                            JsonObject paramMovPayback = new JsonObject()
                                                                    .put(_CUSTOMER_ID, parcel.getInteger(Constants.CUSTOMER_ID))
                                                                    .put("points", paybackPoints)
                                                                    .put("money", paybackMoney)
                                                                    .put("type_movement", "I")
                                                                    .put("motive", "Envio de paquetería(sender)")
                                                                    .put("id_parent", parcelId)
                                                                    .put("employee_id", createdBy);
                                                            objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                                                                try {
                                                                    if (errorMP != null) {
                                                                        throw errorMP;
                                                                    }

                                                                    this.insertService(conn, createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                                                        try{
                                                                            if (errorService != null){
                                                                                throw errorService;
                                                                            }

                                                                            JsonObject result = new JsonObject().put(ID, parcelId);
                                                                            result.put("ticket_id", ticket.getInteger(ID))
                                                                                    .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                            future.complete(result);

                                                                        } catch (Throwable t){
                                                                            future.completeExceptionally(t);
                                                                        }
                                                                    });
                                                                } catch (Throwable t){
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
                                        }
                                    } catch (Throwable t){
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer packageId, Integer cashOutId, Integer createdBy, Boolean is_credit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (!is_credit) {
                JsonObject cashOutMove = new JsonObject();
                payment.put("currency_id", currencyId);
                payment.put(_PARCEL_ID, packageId);
                payment.put(CREATED_BY, createdBy);
                cashOutMove.put("quantity", payment.getDouble(_AMOUNT));
                cashOutMove.put("move_type", "0");
                cashOutMove.put("cash_out_id", cashOutId);
                cashOutMove.put(CREATED_BY, createdBy);
                PaymentDBV objPayment = new PaymentDBV();
                objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        payment.put(ID, resultPayment.getInteger(ID));
                        cashOutMove.put("payment_id", resultPayment.getInteger(ID));
                        GenericQuery insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                        conn.updateWithParams(insertCashOutMove.getQuery(), insertCashOutMove.getParams(), (AsyncResult<UpdateResult> replyMove) -> {
                            try {
                                if (replyMove.failed()) {
                                    throw new Exception(replyMove.cause());
                                }
                                future.complete(payment);


                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
                    }
                });
            } else {
                future.complete(payment);
            }
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    protected CompletableFuture<JsonObject> internalCustomerValidation(Boolean flagInternalCustomer,Boolean isInternalParcel, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (flagInternalCustomer && isInternalParcel){
                Double insuranceAmount = body.getDouble(INSURANCE_AMOUNT, 0.00);
                Double extraCharges = body.getDouble(EXTRA_CHARGES, 0.00);
                Double amount = body.getDouble(AMOUNT);
                body.put(PromosDBV.DISCOUNT, amount + insuranceAmount + extraCharges);
                body.put(TOTAL_AMOUNT, 0.00);
                future.complete(body);
            } else {
                future.complete(body);
            }
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt, Integer createdBy, Double creditBalance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            Double actualCreditBalance = customerCreditData.getDouble("credit_balance");
            JsonObject customerObject = new JsonObject();

            double creditAvailable;
            customerObject.put("credit_balance", creditBalance > 0 ? actualCreditBalance + creditBalance : actualCreditBalance);
            creditAvailable = debt > 0 ? actualCreditAvailable - debt : actualCreditAvailable;

            customerObject
                    .put(ID, customerCreditData.getInteger(ID))
                    .put("credit_available", creditAvailable)
                    .put(UPDATED_BY, createdBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            GenericQuery updateCostumer = this.generateGenericUpdate("customer", customerObject);

            conn.updateWithParams(updateCostumer.getQuery(), updateCostumer.getParams(), (AsyncResult<UpdateResult> replyCustomer) -> {
                try {
                    if (replyCustomer.failed()) {
                        throw replyCustomer.cause();
                    }
                    future.complete(replyCustomer.succeeded());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertService(SQLConnection conn, Integer createdBy, JsonObject serviceObject , Integer parcelId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject service = new JsonObject();
            if(serviceObject.getBoolean("is_rad") != null || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null ){

                service.put(_PARCEL_ID, parcelId);
                service.put(_AMOUNT, serviceObject.getDouble("service_amount"));
                service.put("id_type_service", serviceObject.getInteger("id_type_service"));
                service.put("zip_code", serviceObject.getInteger("zip_code"));
                service.put(CREATED_BY, createdBy);
                service.put(CREATED_AT, sdfDataBase(new Date()));
                if(serviceObject.getBoolean("is_rad") != null){
                    service.put("confirme_rad", 0);
                    service.put(STATUS, 1);
                }

                if(  serviceObject.getBoolean("is_rad_ead") != null){
                    service.put("confirme_rad", 1);
                    service.put(STATUS, 1);
                }

                if(serviceObject.getBoolean("is_ead") != null ){
                    service.put("confirme_rad", 1);
                    service.put(STATUS, 1);
                }

                GenericQuery insert = this.generateGenericCreate("parcels_rad_ead", service);
                conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                    try{
                        if (reply.succeeded()) {

                            future.complete(service);

                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            }
            else{
                future.complete(service);
            }


        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Integer parcelId, Double totalPayments, JsonObject cashChange, Integer createdBy, Double ivaPercent) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            Double iva = this.getIva(totalPayments, ivaPercent);

            ticket.put(_PARCEL_ID, parcelId);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
            ticket.put("total", totalPayments);
            ticket.put(CREATED_BY, createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));
            ticket.put("action", "voucher");

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble("total");
                double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                double differencePaid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if (totalPayments < total) {
                    throw new Throwable("The payment " + total + " is greater than the total " + totalPayments);
                } else if (totalPayments > total) {
                    throw new Throwable("The payment " + total + " is lower than the total " + totalPayments);
                } else if (paid_change > differencePaid) {
                    throw new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                } else if (paid_change < differencePaid) {
                    throw new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                }
            } else {
                ticket.put("paid", totalPayments);
                ticket.put("paid_change", 0.0);
            }

            GenericQuery insert = this.generateGenericCreate("tickets", ticket);

            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if (reply.succeeded()) {
                        final int id = reply.result().getKeys().getInteger(0);
                        ticket.put(ID, id);
                        future.complete(ticket);
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer ticketId, Integer createdBy, JsonArray packages, JsonArray packings, JsonObject parcel, Boolean internalCustomer , JsonObject serviceObject) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();

        conn.query("SELECT id, name_price, shipping_type FROM package_price;", replyPP -> {
            try{
                if(replyPP.failed()){
                    throw new Exception(replyPP.cause());
                }
                List<JsonObject> resultPP = replyPP.result().getRows();
                Map<String, List<JsonObject>> groupedPackages = packages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString("shipping_type")));
                JsonArray details = new JsonArray();

                for (String s : groupedPackages.keySet()) {
                    JsonObject packagePrice = new JsonObject();
                    AtomicReference<Integer> quantity = new AtomicReference<>(0);
                    AtomicReference<Double> unitPrice = new AtomicReference<>(0.00);
                    AtomicReference<Double> amount = new AtomicReference<>(0.00);
                    Optional<JsonObject> packageName = resultPP.stream().filter(x -> x.getInteger(ID).equals(groupedPackages.get(s).get(0).getInteger("package_price_id"))).findFirst();
                    String packageRange = packageName.get().getString("name_price");
                    groupedPackages.get(s).forEach(x -> {
                        quantity.getAndSet(quantity.get() + 1);
                        unitPrice.updateAndGet(v -> v + x.getDouble("total_amount"));
                        amount.updateAndGet(v -> v + x.getDouble("total_amount"));
                        packagePrice.put(PromosDBV.DISCOUNT, x.getDouble(PromosDBV.DISCOUNT));
                    });
                    packagePrice.put("shipping_type", s);
                    packagePrice.put("unit_price", unitPrice.get());
                    packagePrice.put(_AMOUNT, amount.get());
                    packagePrice.put("quantity", quantity.get());
                    if(packagePrice.getInteger("quantity") != null){
                        if(packagePrice.getInteger("quantity") > 0){
                            JsonObject ticketDetail = new JsonObject();
                            String shippingType = packagePrice.getString("shipping_type");
                            switch (shippingType){
                                case "parcel":
                                    shippingType = "paquetería";
                                    break;
                                case "courier":
                                    shippingType = "mensajería";
                                    break;
                                case "pets":
                                    shippingType = "mascota";
                                    break;
                                case "frozen":
                                    shippingType = "carga refrigerada";
                                    break;
                            }
                            ticketDetail.put("ticket_id", ticketId);
                            ticketDetail.put("quantity", packagePrice.getInteger("quantity"));
                            ticketDetail.put("detail", "Envío de " + shippingType + " con rango " + packageRange);
                            ticketDetail.put("unit_price", packagePrice.getDouble("unit_price"));
                            ticketDetail.put(PromosDBV.DISCOUNT, packagePrice.getDouble(PromosDBV.DISCOUNT));
                            ticketDetail.put(_AMOUNT, packagePrice.getDouble(_AMOUNT));
                            ticketDetail.put(CREATED_BY, createdBy);
                            details.add(ticketDetail);
                        }
                    }
                }

                int len = packings.size();
                for (int i = 0; i < len; i++) {
                    JsonObject packing = packings.getJsonObject(i);
                    JsonObject ticketDetail = new JsonObject();

                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", packing.getInteger("quantity"));
                    ticketDetail.put("detail", "Embalaje");
                    ticketDetail.put("unit_price", packing.getDouble("unit_price"));
                    ticketDetail.put(_AMOUNT, packing.getDouble(_AMOUNT));
                    ticketDetail.put(CREATED_BY, createdBy);

                    details.add(ticketDetail);
                }

                if(parcel.getBoolean("has_insurance") != null && parcel.getBoolean("has_insurance")){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Seguro de envío");
                    ticketDetail.put("unit_price", parcel.getDouble("insurance_amount"));
                    ticketDetail.put(_AMOUNT, parcel.getDouble("insurance_amount"));
                    ticketDetail.put(CREATED_BY, createdBy);

                    details.add(ticketDetail);
                }

                if(serviceObject.getBoolean("is_rad") != null || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Servicio " +
                            serviceObject.getString("service"));
                    ticketDetail.put("unit_price", serviceObject.getDouble("service_amount"));
                    ticketDetail.put(_AMOUNT, serviceObject.getDouble("service_amount"));
                    ticketDetail.put(CREATED_BY, createdBy);

                    details.add(ticketDetail);
                }


                if(packages.isEmpty() && packings.isEmpty()) {
                    JsonObject ticketDetail = new JsonObject()
                            .put("ticket_id", ticketId)
                            .put("quantity", 0.00)
                            .put("detail", "Comprobante de entrega de paquetería")
                            .put("unit_price", 0.00)
                            .put(_AMOUNT, 0.00)
                            .put(CREATED_BY, createdBy);

                    details.add(ticketDetail);
                }

                for(int i = 0; i < details.size(); i++){
                    if (internalCustomer){
                        details.getJsonObject(i).put(AMOUNT, 0.00).put("unit_price", 0.00);
                    }
                    inserts.add(this.dbVerticle.generateGenericCreate("tickets_details", details.getJsonObject(i)));
                }

                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                    try {
                        if (replyInsert.failed()){
                            throw new Exception(replyInsert.cause());
                        }
                        future.complete(replyInsert.succeeded());
                    }catch(Exception e ){
                        future.completeExceptionally(e);
                    }
                });


            }catch (Exception ex){
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    protected CompletableFuture<JsonObject> endRegisterGuiapp(SQLConnection conn, JsonArray guiaPPArray,int parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            List<CompletableFuture<JsonObject>> guiappTask = new ArrayList<>();
            for (Object g : guiaPPArray) {
                JsonObject guiaPP = (JsonObject) g;
                Integer idParcelPrepaidDetail = guiaPP.getInteger("id_parcel_prepaid_detail");
                guiappTask.add(updateParcelsPrepaidDetails(conn, idParcelPrepaidDetail, parcelId));
            }
            CompletableFuture.allOf(guiappTask.toArray(new CompletableFuture[guiappTask.size()])).whenComplete(( st, tError) -> {
                try {
                    JsonObject result =new JsonObject().put("result",st);
                    if(tError != null) {
                        throw tError;
                    }
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateParcelsPrepaidDetails(SQLConnection conn, Integer guiappId, int parcelId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(parcelId).add(sdfDataBase(new Date())).add(guiappId);
            conn.updateWithParams(QUERY_UPDATE_USAGE_GUIAPP, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    JsonObject guiappResult = new JsonObject();
                    guiappResult.put("data", true);
                    future.complete(guiappResult);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private static final String GET_CODE_GUIAPP = "Select  pp.id, pp.tracking_code as parcels_tracking_code, pp.shipment_type, \n" +
            " pp.payment_condition, pp.purchase_origin, pp.customer_id, pp.crated_by,\n" +
            " pp.created_at, pp.updated_by, pp.updated_at, pp.parcel_status,\n" +
            " pp.cash_register_id, pp.total_count_guipp, \n" +
            " pp.total_count_guipp_remaining, pp.promo_id, pp.amount, \n" +
            " pp.discount, pp.has_insurance, pp.insurance_value, pp.insurance_amount, \n" +
            " pp.extra_charges, pp.iva, pp.parcel_iva, pp.total_amount, \n" +
            " pp.schedule_route_destination_id,\n" +
            " pp.expire_at as parcels_expire,\n" +
            " ppd.id as id_parcel_prepaid_detail, ppd.guiapp_code,\n" +
            " ppd.ticket_id, ppd.branchoffice_id_exchange, \n" +
            " ppd.customer_id_exchange, ppd.price_km, ppd.price_km_id, ppd.price, \n" +
            " ppd.price_id, ppd.amount, ppd.discount, \n" +
            " ppd.total_amount, ppd.crated_by, ppd.created_at, ppd.updated_by, ppd.updated_at, ppd.status, \n" +
            " ppd.schedule_route_destination_id, \n" +
            " ppd.parcel_status,\n" +
            " ppd.package_type_id,pt.shipping_type, ppd.expire_at as expire_at_prepaid_detail\n" +
            " from parcels_prepaid as pp \n" +
            "inner join parcels_prepaid_detail as ppd on ppd.parcel_prepaid_id=pp.id\n" +
            "inner join package_types as pt  on pt.id=ppd.package_type_id\n" +
            " where (pp.status!=4 and pp.parcel_status=1 and ppd.status!=4 and ppd.parcel_status=1 ) and ppd.expire_at > NOW() and ppd.guiapp_code=?";

    private static final String QUERY_INSURANCE_VALIDATION = "SELECT id FROM insurances WHERE ? BETWEEN init AND end AND status = 1;";

    private static final String QUERY_GET_PARCEL_CITY_SENDER = "SELECT \n" +
            "city_id \n" +
            "FROM customer_addresses AS ca \n" +
            "LEFT JOIN parcels AS p \n" +
            "ON ca.id = p.sender_address_id \n" +
            "WHERE ca.customer_id = ?;";

    private static final String QUERY_GET_PARCEL_CITY_ADDRESSEE = "SELECT \n" +
            "city_id \n" +
            "FROM customer_addresses AS ca \n" +
            "LEFT JOIN parcels AS p \n" +
            "ON ca.id = p.addressee_address_id \n" +
            "WHERE ca.customer_id = ?;";

    private static final String QUERY_GET_TERMINAL_CITY = "SELECT \n"+
            "city_id \n"+
            "FROM branchoffice\n"+
            "WHERE id = ?;";

    private  static  final String QUERY_PACKINGS_BY_ID ="SELECT * FROM packings WHERE id = ? LIMIT 1;";

    private static final String QUERY_UPDATE_USAGE_GUIAPP = "UPDATE parcels_prepaid_detail SET parcel_status=1, parcel_id = ?, updated_at = ? WHERE id = ?;";

}
