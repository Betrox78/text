/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.promos;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.parcel.ParcelDBV;
import database.parcel.ParcelsPackagesDBV;
import database.promos.enums.DISCOUNT_TYPES;
import database.promos.enums.RULES;
import database.promos.enums.SERVICES;
import database.promos.enums.TYPES_PACKAGES;
import database.promos.handlers.PromosDBV.ApplyMultiplePromo;
import database.promos.handlers.PromosDBV.CalculateMultiplePromo;
import database.promos.handlers.PromosDBV.CustomerPromoReport;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.promos.CustomersPromosDBV.PROMO_ID;
import static database.promos.enums.SERVICES.*;
import static service.commons.Constants.*;
import static utils.UtilsDate.*;

/**
 *
 * @author Indq tech - Gerardo Valdes Uriarte
 */
public class PromosDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "PromosDBV.register";
    public static final String ACTION_CHECK_PROMO_CODE = "PromosDBV.checkPromoCode";
    public static final String ACTION_CHECK_USER_PROMO_CODE = "PromosDBV.checkUserPromoCode";
    public static final String ACTION_CHECK_HAS_SPECIFIC_CUSTOMER = "PromosDBV.checkHasSpecificCustomer";
    public static final String ACTION_GET_LIST = "PromosDBV.getList";
    public static final String ACTION_CALCULATE_PROMO_CODE = "PromosDBV.calculatePromoCode";
    public static final String ACTION_REGISTER_MASSIVE = "PromosDBV.registerMassive";
    public static final String ACTION_GET_GENERAL_BY_ORIGIN = "PromosDBV.getGeneralByOrigin";
    public static final String ACTION_APPLY_PROMO_CODE = "PromosDBV.applyPromoCode";
    public static final String ACTION_CALCULATE_MULTIPLE_PROMO = "PromosDBV.calculateMultiplePromo";
    public static final String ACTION_APPLY_MULTIPLE_PROMO = "PromosDBV.applyMultiplePromo";
    public static final String ACTION_GET_DETAIL = "PromosDBV.getDetail";
    public static final String ACTION_CUSTOMER_PROMO_REPORT = "PromosDBV.customerPromoReport";

    @Override
    public String getTableName() {
        return "promos";
    }

    CalculateMultiplePromo calculateMultiplePromo;
    ApplyMultiplePromo applyMultiplePromo;
    CustomerPromoReport customerPromoReport;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.calculateMultiplePromo = new CalculateMultiplePromo(this);
        this.applyMultiplePromo = new ApplyMultiplePromo(this);
        this.customerPromoReport = new CustomerPromoReport(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message){
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action){
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_CHECK_PROMO_CODE:
                this.checkPromoCode(message);
                break;
            case ACTION_CHECK_USER_PROMO_CODE:
                this.checkUserPromoCode(message);
                break;
            case ACTION_CHECK_HAS_SPECIFIC_CUSTOMER:
                this.checkHasSpecificCustomer(message);
                break;
            case ACTION_GET_LIST:
                this.getList(message);
                break;
            case ACTION_CALCULATE_PROMO_CODE:
                this.calculatePromoCode(message);
                break;
            case ACTION_REGISTER_MASSIVE:
                this.registerMassive(message);
                break;
            case ACTION_GET_GENERAL_BY_ORIGIN:
                this.getGeneralByOrigin(message);
                break;
            case ACTION_APPLY_PROMO_CODE:
                this.applyPromoCode(message);
                break;
            case ACTION_CALCULATE_MULTIPLE_PROMO:
                this.calculateMultiplePromo.handle(message);
                break;
            case ACTION_APPLY_MULTIPLE_PROMO:
                this.applyMultiplePromo.handle(message);
                break;
            case ACTION_GET_DETAIL:
                this.getDetail(message);
                break;
            case ACTION_CUSTOMER_PROMO_REPORT:
                this.customerPromoReport.handle(message);
                break;
        }
    }

    public enum ERRORS {
        IS_VALID_SERVICE("Discount code does not apply with this service"),
        DISCOUNT_CODE_NOT_FOUND("Discount code not found"),
        IS_VALID_DATE("Discount code has expired"),
        IS_VALID_DAY("Discount code is not available this day"),
        IS_VALID_SPECIAL_TICKET("Discount code is not available with this type of ticket"),
        IS_VALID_CUSTOMER("Discount code is not available for this customer"),
        IS_VALID_USER("Discount code is not available for this user"),
        IS_VALID_USAGE("Discount code has reached its limit of use"),
        IS_VALID_NUM_PRODUCTS("Amount of products insufficient"),
        IS_VALID_RULE_FOR_BOARDINGPASS("Boarding pass rule invalid"),
        IS_VALID_PACKAGE_PRICE("Discount code is not available with this type of package price"),
        IS_VALID_TYPE_PACKAGE("Package type invalid"),
        IS_VALID_RULE_FOR_PACKAGE("Package rule invalid"),
        IS_VALID_PACKAGE_PRICE_DISTANCE("Discount code is not available with this type of package price distance"),
        IS_VALID_PURCHASE_ORIGIN("Discount code is not available with this purchase origin"),
        IS_VALID_ONLY_FIRST_PURCHASE("Discount code is available only in first purchase"),
        GENERAL_PROMO_BY_ORIGIN_INACTIVE_OR_NOT_FOUND("General promo by origin inactive or not found"),
        IS_VALID_GENERAL_CUSTOMER_LIMIT("General customer promo has reached its limit of use"),
        TERMINAL_DISTANCE_NOT_FOUND("Distance by terminals not register on database"),
        IS_VALID_BIRTHDAY("Birthday conditions do not apply"),
        IS_VALID_PHONE_VERIFICATION("Discount code is available only with users with phone verified");

        private final String message;

        ERRORS(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final String FLAG_PROMO = "flag_promo";
    public static final String FLAG_USER_PROMO = "flag_user_promo";
    public static final String DISCOUNT_CODE = "discount_code";
    public static final String DISCOUNT_TYPE = "discount_type";
    public static final String SERVICE = "service";
    public static final String BODY_SERVICE = "body_service";
    public static final String RULE = "rule";
    public static final String SINCE = "since";
    public static final String UNTIL = "until";
    public static final String NUM_PRODUCTS = "num_products";
    public static final String APPLY_TO_SPECIAL_TICKETS = "apply_to_special_tickets";
    public static final String HAS_SPECIFIC_CUSTOMER = "has_specific_customer";
    public static final String USAGE_LIMIT = "usage_limit";
    public static final String USED = "used";
    public static final String DISCOUNT = "discount";
    public static final String PROMO_DISCOUNT = "promo_discount";
    public static final String DATE = "date";
    public static final String SPECIAL_TICKET_ID = "special_ticket_id";
    public static final String PACKAGE_PRICE_NAME = "package_price_name";
    public static final String PACKAGE_PRICE_DISTANCE_ID = "package_price_distance_id";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CUSTOMERS = "customers";
    public static final String USERS = "users";
    public static final String DISCOUNT_PER_BASE = "discount_per_base";
    public static final String PRODUCTS = "products";
    public static final String TICKET_TYPE = "ticket_type";
    public static final String TICKET_TYPE_ROUTE = "ticket_type_route";
    public static final String OTHER_PRODUCTS = "other_products";
    public static final String CUSTOMERS_PROMOS = "customers_promos";
    public static final String RULE_FOR_PACKAGES = "rule_for_packages";
    public static final String TYPE_PACKAGES = "type_packages";
    public static final String APPLY_TO_PACKAGE_PRICE = "apply_to_package_price";
    public static final String PROMO_CODES_QUANTITY = "promo_codes_quantity";
    public static final String PARENT_ID = "parent_id";
    public static final String PROMO_CODES = "promo_codes";
    public static final Integer LAST_CHARS_PROMO_CODE = 8;
    public static final String APPLY_TO_PACKAGE_PRICE_DISTANCE = "apply_to_package_price_distance";
    public static final String APPLY_TO_PACKAGE_TYPE = "apply_to_package_type";
    public static final String PURCHASE_ORIGIN = "purchase_origin";
    public static final String APPLY_ONLY_FIRST_PURCHASE = "apply_only_first_purchase";
    public static final String GENERAL_PROMO_ID = "general_promo_id";
    public static final String CUSTOMER_LIMIT = "customer_limit";
    public static final String IS_PROMO_CUSTOMER = "is_promo_customer";

    private void register(Message<JsonObject> message){
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            this.register(conn, body).whenComplete((result, error) -> {
                try {
                    if (error != null){
                        throw error;
                    }

                    this.commit(conn, message, result);

                } catch (Throwable t){
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            Integer createdBy = body.getInteger(CREATED_BY);
            JsonArray customers = body.getValue(CUSTOMERS) != null ? (JsonArray) body.remove(CUSTOMERS) : new JsonArray();
            JsonArray users = body.getValue(USERS) != null ? (JsonArray) body.remove(USERS) : new JsonArray();
            boolean hasCustomerLimit = body.getInteger(CUSTOMER_LIMIT) != null && body.getInteger(CUSTOMER_LIMIT) > 0;

            List<String> permittedApplyReturn = new ArrayList<>();
            permittedApplyReturn.add(RULES.boardingpass_abierto.name());
            permittedApplyReturn.add(RULES.boardingpass_redondo.name());
            String rule = body.getString("rule", null);

            if(body.containsKey("apply_return") && body.getBoolean("apply_return") && (rule == null || !permittedApplyReturn.contains(rule))){
                throw new Throwable(String.format("apply_return only available with promo rules: %s and %s.", RULES.boardingpass_abierto.name(), RULES.boardingpass_redondo.name()));
            }

            GenericQuery createPromo = this.generateGenericCreate(body);
            conn.updateWithParams(createPromo.getQuery(), createPromo.getParams(), replyCreatePromo -> {
                try {
                    if (replyCreatePromo.failed()){
                        throw new Exception(replyCreatePromo.cause());
                    }

                    Integer promoId = replyCreatePromo.result().getKeys().getInteger(0);

                    if (customers.isEmpty() && users.isEmpty()){

                        future.complete(new JsonObject().put(ID, promoId));

                    } else {
                        List<String> batch = new ArrayList<>();
                        customers.forEach(c -> {
                            JsonObject customer = (JsonObject) c;
                            customer.put(PROMO_ID, promoId)
                                    .put(CREATED_BY, createdBy);

                            if(hasCustomerLimit){
                                customer.put(IS_PROMO_CUSTOMER, false);
                            }

                            batch.add(this.generateGenericCreate("customers_promos", customer));
                        });

                        users.forEach(u -> {
                            JsonObject user = (JsonObject) u;
                            user.put(PROMO_ID, promoId)
                                    .put(CREATED_BY, createdBy);

                            batch.add(this.generateGenericCreate("users_promos", user));
                        });

                        conn.batch(batch, replyCreateRelation -> {
                            try {
                                if (replyCreateRelation.failed()){
                                    throw new Exception(replyCreateRelation.cause());
                                }

                                future.complete(new JsonObject().put(ID, promoId));

                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }

        return future;
    }

    private void registerMassive(Message<JsonObject> message){
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            body.put(USAGE_LIMIT, 1)
                    .put(USED, 1);
            Integer promoCodesQuantity = (Integer) body.remove(PROMO_CODES_QUANTITY);

            this.register(conn, body).whenComplete((result, error) -> {
                try {
                    if (error != null){
                        throw error;
                    }

                    Integer parentId = result.getInteger(ID);

                    this.getPromoCodes(promoCodesQuantity).whenComplete((promoCodes, errorCodes) -> {
                        try {
                            if (errorCodes != null){
                                throw errorCodes;
                            }
                            result.put(PROMO_CODES, promoCodes);

                            List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                            for (int i = 0; i < promoCodesQuantity; i++){

                                body.put(PARENT_ID, parentId)
                                        .put(DISCOUNT_CODE, (String) promoCodes.getValue(i))
                                        .put(USAGE_LIMIT, 1)
                                        .put(USED, 0);

                                tasks.add(this.register(conn, body));
                            }

                            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resultMassive, errorMassive) -> {
                                try {
                                    if (errorMassive != null){
                                        throw errorMassive;
                                    }

                                    this.commit(conn, message, result);

                                } catch (Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });

                        } catch (Throwable t){
                            this.rollback(conn, t, message);
                        }
                    });

                } catch (Throwable t){
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private String generatePromoCode(){
        final String random = UUID.randomUUID().toString();
        return random.substring(random.length() - LAST_CHARS_PROMO_CODE).toUpperCase();
    }

    private CompletableFuture<String> getPromoCode(JsonArray promoCodes){
        CompletableFuture<String> future = new CompletableFuture<>();

        String promoCode = this.generatePromoCode();

        this.dbClient.queryWithParams("SELECT id FROM promos WHERE discount_code = ?;", new JsonArray().add(promoCode), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()){

                    promoCodes.add(promoCode);
                    future.complete(promoCode);

                } else {
                    this.getPromoCode(promoCodes).whenComplete((resultPromoCode, errorPromoCode) -> {
                        try {
                            if (errorPromoCode != null){
                                throw errorPromoCode;
                            }

                            future.complete(resultPromoCode);

                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPromoCodes(Integer quantity){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        JsonArray promoCodes = new JsonArray();

        List<CompletableFuture<String>> tasks = new ArrayList<>();

        for (int i = 0; i < quantity; i++){
            tasks.add(this.getPromoCode(promoCodes));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
            try {
                if (error != null){
                    throw error;
                }

                future.complete(promoCodes);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void calculatePromoCode(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer userID = body.getInteger(USER_ID);
        Boolean flagUserPromo = body.getBoolean(FLAG_USER_PROMO);
        JsonObject discount = body.getJsonObject(DISCOUNT);
        String service = discount.getString(SERVICE);
        JsonObject bodyService = body.getJsonObject(SERVICE);
        JsonArray products = body.getJsonArray(PRODUCTS);
        JsonArray otherProducts = body.getJsonArray(OTHER_PRODUCTS, new JsonArray());
        this.applyPromoCode(null, SERVICES.valueOf(service), true, discount, bodyService, products, otherProducts, flagUserPromo, userID).whenComplete((resultApplyPromoCode, errorApplyPromoCode) -> {
            try {
                if (errorApplyPromoCode != null){
                    throw errorApplyPromoCode;
                }
                message.reply(resultApplyPromoCode);
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void applyPromoCode(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer userID = body.getInteger(USER_ID, null);
            Boolean flagUserPromo = body.getBoolean(FLAG_USER_PROMO, false);
            JsonObject discount = body.getJsonObject(DISCOUNT, new JsonObject());
            String service = body.getString(SERVICE);
            JsonObject bodyService = body.getJsonObject(BODY_SERVICE);
            JsonArray products = body.getJsonArray(PRODUCTS);
            JsonArray otherProducts = body.getJsonArray(OTHER_PRODUCTS, new JsonArray());
            boolean flagPromo = body.getBoolean(FLAG_PROMO, false);
            startTransaction(message, conn -> {
                this.applyPromoCode(conn, SERVICES.valueOf(service), flagPromo, discount, bodyService, products, otherProducts, flagUserPromo, userID).whenComplete((resultApplyPromoCode, errorApplyPromoCode) -> {
                    try {
                        if (errorApplyPromoCode != null){
                            throw errorApplyPromoCode;
                        }
                        this.commit(conn, message, resultApplyPromoCode);
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void checkUserPromoCode(Message<JsonObject> message){
        JsonObject body = message.body();
        try {
            String QUERY = QUERY_VALIDATE_USER_PROMO_CODE;
            String service = body.getString(SERVICE);
            String discountCode = body.getString(DISCOUNT_CODE);
            String date = body.getString(DATE);
            JsonArray specialTicketIds = body.getJsonArray(SPECIAL_TICKET_ID, new JsonArray());
            JsonArray packagePriceNames = body.getJsonArray(PACKAGE_PRICE_NAME, new JsonArray());
            JsonArray packagePriceDistanceIds = body.getJsonArray(PACKAGE_PRICE_DISTANCE_ID, new JsonArray());
            Integer userID = body.getInteger(USER_ID);
            Integer numProduct = body.getInteger(NUM_PRODUCTS, 0);
            String rule = body.getString(RULE) == null ? "" : body.getString(RULE);
            String typePackages = body.getString(TYPE_PACKAGES) == null ? "" : body.getString(TYPE_PACKAGES);
            Integer purchaseOrigin = body.getInteger(PURCHASE_ORIGIN) == null ? -1 : body.getInteger(PURCHASE_ORIGIN);
            JsonArray params = new JsonArray();

            String availableST = "";
            for (int i = 0; i < specialTicketIds.size(); i++){
                availableST += " OR FIND_IN_SET(?, p.apply_to_special_tickets) ";
                params.add(String.valueOf(specialTicketIds.getValue(i)));
            }

            String availablePP = "";
            for (int i = 0; i < packagePriceNames.size(); i++){
                availablePP += " OR FIND_IN_SET(?, p.apply_to_package_price) ";
                params.add(String.valueOf(packagePriceNames.getValue(i)));
            }

            String availablePPD = "";
            for (int i = 0; i < packagePriceDistanceIds.size(); i++){
                availablePPD += " OR FIND_IN_SET(?, p.apply_to_package_price_distance) ";
                params.add(String.valueOf(packagePriceDistanceIds.getValue(i)));
            }

            QUERY = String.format(QUERY, availableST, availablePP, availablePPD);

            params.add(service)
                    .add(date).add(date)
                    .add(userID)
                    .add(numProduct)
                    .add(rule)
                    .add(typePackages)
                    .add(purchaseOrigin)
                    .add(userID)
                    .add(userID)
                    .add(userID)
                    .add(discountCode);

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultValidation = reply.result().getRows();
                    if (resultValidation.isEmpty()){
                        throw new Exception(ERRORS.DISCOUNT_CODE_NOT_FOUND.getMessage());
                    }
                    JsonObject validationObject = resultValidation.get(0);
                    boolean applyCode = true;
                    ERRORS error = null;
                    for(Map.Entry<String, Object> value : validationObject){
                        long valid = (long) value.getValue();
                        if (valid == 0) {
                            applyCode = false;
                            error = ERRORS.valueOf(value.getKey().toUpperCase());
                            break;
                        }
                    }
                    if(!applyCode){
                        throw new Exception(error.getMessage());
                    }
                    message.reply(new JsonObject().put(FLAG_PROMO, true));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void checkPromoCode(Message<JsonObject> message){
        JsonObject body = message.body();
        String discountCode = body.getString(DISCOUNT_CODE);
        try {
            this.dbClient.queryWithParams(QUERY_PROMO_INFO_BY_CODE, new JsonArray().add(discountCode), promoInfoResult -> {
                try {
                    if (promoInfoResult.failed()) {
                        throw new Exception(promoInfoResult.cause());
                    }
                    List<JsonObject> promoInfoResultValidation = promoInfoResult.result().getRows();
                    if (promoInfoResultValidation.isEmpty()){
                        throw new Exception(ERRORS.DISCOUNT_CODE_NOT_FOUND.getMessage());
                    }

                    String QUERY = QUERY_VALIDATE_PROMO_CODE_VALIDATIONS;
                    String service = body.getString(SERVICE);
                    String date = body.getString(DATE);
                    JsonArray specialTicketIds = body.getJsonArray(SPECIAL_TICKET_ID, new JsonArray());
                    JsonArray packagePriceNames = body.getJsonArray(PACKAGE_PRICE_NAME, new JsonArray());
                    JsonArray packagePriceDistanceIds = body.getJsonArray(PACKAGE_PRICE_DISTANCE_ID, new JsonArray());
                    Integer customerId = body.getInteger(CUSTOMER_ID, 0);
                    Integer numProduct = body.getInteger(NUM_PRODUCTS, 0);
                    String rule = body.getString(RULE) == null ? "" : body.getString(RULE);
                    String typePackages = body.getString(TYPE_PACKAGES) == null ? "" : body.getString(TYPE_PACKAGES);
                    Integer purchaseOrigin = body.getInteger(PURCHASE_ORIGIN) == null ? -1 : body.getInteger(PURCHASE_ORIGIN);
                    JsonArray params = new JsonArray();

                    JsonObject promoInfo = promoInfoResult.result().getRows().get(0);
                    JsonObject holidayQueryData = getExtraQueryIfHoliday(promoInfo, body);
                    if (holidayQueryData.getBoolean("apply")) {
                        QUERY += "," + holidayQueryData.getString("query");
                    }

                    String availableST = "";
                    for (int i = 0; i < specialTicketIds.size(); i++){
                        availableST += " OR FIND_IN_SET(?, p.apply_to_special_tickets) ";
                        params.add(String.valueOf(specialTicketIds.getValue(i)));
                    }

                    String availablePP = "";
                    for (int i = 0; i < packagePriceNames.size(); i++){
                        availablePP += " OR FIND_IN_SET(?, p.apply_to_package_price) ";
                        params.add(String.valueOf(packagePriceNames.getValue(i)));
                    }

                    String availablePPD = "";
                    for (int i = 0; i < packagePriceDistanceIds.size(); i++){
                        availablePPD += " OR FIND_IN_SET(?, p.apply_to_package_price_distance) ";
                        params.add(String.valueOf(packagePriceDistanceIds.getValue(i)));
                    }

                    QUERY = String.format(QUERY, availableST, availablePP, availablePPD);
                    QUERY += QUERY_VALIDATE_PROMO_CODE_WHERE;

                    // VALIDATIONS params
                    params.add(service)
                            .add(date).add(date)
                            .add(customerId)
                            .add(numProduct)
                            .add(rule)
                            .add(typePackages)
                            .add(purchaseOrigin)
                            .add(customerId)
                            .add(customerId)
                            .add(customerId)
                            .add(customerId)
                            .add(customerId);

                    if(holidayQueryData.getBoolean("apply")) {
                        for (Object holidayParam : holidayQueryData.getJsonArray("params")) {
                            params.add(holidayParam);
                        }
                    }

                    // WHERE params
                    params.add(customerId).add(discountCode);

                    this.dbClient.queryWithParams(QUERY, params, reply -> {
                        try {
                            if (reply.failed()){
                                throw new Exception(reply.cause());
                            }
                            List<JsonObject> resultValidation = reply.result().getRows();
                            if (resultValidation.isEmpty()){
                                throw new Exception(ERRORS.DISCOUNT_CODE_NOT_FOUND.getMessage());
                            }
                            JsonObject validationObject = resultValidation.get(0);
                            boolean applyCode = true;
                            ERRORS error = null;
                            for(Map.Entry<String, Object> value : validationObject){
                                long valid = (long) value.getValue();
                                if (valid == 0) {
                                    applyCode = false;
                                    error = ERRORS.valueOf(value.getKey().toUpperCase());
                                    break;
                                }
                            }
                            if(!applyCode){
                                throw new Exception(error.getMessage());
                            }
                            message.reply(new JsonObject().put(FLAG_PROMO, true));
                        } catch (Throwable t) {
                            reportQueryError(message, t);
                        }
                    });
                } catch(Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private JsonObject getExtraQueryIfHoliday(JsonObject promo, JsonObject body) {
        JsonObject holidayQueryData = new JsonObject().put("apply", false).put("query", "");
        if(promo.getBoolean("is_holiday") && promo.getString("holiday") != null) {
            holidayQueryData.put("apply", true);
            switch (promo.getString("holiday")) {
                case "birthday":
                    holidayQueryData.put("query", QUERY_BIRTHDAY_PROMO_VALIDATION);
                    holidayQueryData.put("params", new JsonArray().add(body.getInteger(CUSTOMER_ID, 0)));
                    break;
            }
        }
        return holidayQueryData;
    }

    private void checkHasSpecificCustomer(Message<JsonObject> message){
        JsonObject body = message.body();
        try {
            Integer promoId = body.getInteger(PROMO_ID);
            this.dbClient.queryWithParams("SELECT has_specific_customer FROM promos WHERE id = ?", new JsonArray().add(promoId), reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultList = reply.result().getRows();
                    if (resultList.isEmpty()){
                        throw new Exception(ERRORS.DISCOUNT_CODE_NOT_FOUND.getMessage());
                    }
                    JsonObject result = resultList.get(0);
                    message.reply(new JsonObject().put(HAS_SPECIFIC_CUSTOMER, result.getBoolean(HAS_SPECIFIC_CUSTOMER)));
                } catch (Exception e){
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e){
            reportQueryError(message, e);
        }
    }

    private void getGeneralByOrigin(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer purchaseOrigin = body.getInteger(PURCHASE_ORIGIN);
        Integer customerId = body.getInteger(CUSTOMER_ID);
        Integer generalPromoId = body.getInteger(GENERAL_PROMO_ID);
        String date = null;

        try {
            date = UtilsDate.format_yyyy_MM_dd(UtilsDate.getDateConvertedTimeZone(timezone, new Date()));
        } catch (ParseException e) {
            reportQueryError(message, e);
        }

        this.dbClient.queryWithParams(QUERY_GET_PROMO_APPLY_FIRST_PURCHASE, new JsonArray().add(customerId).add(purchaseOrigin).add(purchaseOrigin).add(date), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();

                if (!result.isEmpty() && this.validateDatePromo(result.get(0)) && !customerId.equals(0)){

                    JsonObject promo = result.get(0);
                    message.reply(promo);

                } else {

                    String QUERY_GET_PROMO = "SELECT * FROM promos WHERE id = ? AND (usage_limit = 0 OR usage_limit > used) AND status = 1;";
                    this.dbClient.queryWithParams(QUERY_GET_PROMO, new JsonArray().add(generalPromoId), replyGP -> {
                        try {
                            if (replyGP.failed()){
                                throw replyGP.cause();
                            }

                            List<JsonObject> resultGP = replyGP.result().getRows();
                            if (resultGP.isEmpty()){
                                message.reply(new JsonObject());
                            } else {
                                JsonObject generalPromo = resultGP.get(0);

                                if (this.validateDatePromo(generalPromo)){
                                    message.reply(generalPromo);
                                } else {
                                    message.reply(new JsonObject());
                                }
                            }

                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });

                }

            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });

    }

    private boolean validateDatePromo(JsonObject promo) throws Throwable{
        Date today = UtilsDate.parse_yyyy_MM_dd(sdfDataBase(getDateConvertedTimeZone(timezone, new Date())));
        Date since = parseSdfDatabase(promo.getString(SINCE) + " 00:00:00");
        Date until = parseSdfDatabase(promo.getString(UNTIL) + " 00:00:00");

        return !isLowerThan(today, since) && !isGreaterThan(today, until);
    }

    private void getList(Message<JsonObject> message){
        JsonObject body = message.body();

        try {
            String QUERY = QUERY_GET_LIST_PROMOS;
            JsonArray params = new JsonArray();

            if (body.containsKey(APPLY_TO_SPECIAL_TICKETS)){
                JsonArray specialTickets = body.getJsonArray(APPLY_TO_SPECIAL_TICKETS);
                QUERY = QUERY.concat("%s");
                if (!specialTickets.isEmpty()){
                    String availableST = "";
                    for (int i = 0; i < specialTickets.size(); i++){
                        if (i == 0){
                            availableST += " AND (FIND_IN_SET(?, p.apply_to_special_tickets) ";
                        } else {
                            availableST += " OR FIND_IN_SET(?, p.apply_to_special_tickets) ";
                        }
                        params.add(String.valueOf(specialTickets.getValue(i)));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availableST);
                }
                body.remove(APPLY_TO_SPECIAL_TICKETS);
            }

            if (body.containsKey(APPLY_TO_PACKAGE_PRICE)){
                JsonArray packagePriceNames = body.getJsonArray(APPLY_TO_PACKAGE_PRICE);
                QUERY = QUERY.concat("%s");
                if (!packagePriceNames.isEmpty()){
                    String availablePP = "";
                    for (int i = 0; i < packagePriceNames.size(); i++){
                        if (i == 0) {
                            availablePP += " AND (p.apply_to_package_price IS NUL OR FIND_IN_SET(?, p.apply_to_package_price) ";
                        } else {
                            availablePP += " OR FIND_IN_SET(?, p.apply_to_package_price) ";
                        }
                        params.add(String.valueOf(packagePriceNames.getValue(i)));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availablePP);
                }
                body.remove(APPLY_TO_PACKAGE_PRICE);
            }

            if (body.getString(RULE) == null || !body.containsKey(RULE)) {
                QUERY = QUERY.concat(" AND rule IS NULL ");
                body.remove(RULE);
            }

            if (body.containsKey(APPLY_TO_PACKAGE_PRICE_DISTANCE)){
                JsonArray packagePriceNames = body.getJsonArray(APPLY_TO_PACKAGE_PRICE_DISTANCE);
                QUERY = QUERY.concat("%s");
                if (!packagePriceNames.isEmpty()){
                    String availablePP = "";
                    for (int i = 0; i < packagePriceNames.size(); i++){
                        if (i == 0) {
                            availablePP += " AND (p.apply_to_package_price_distance IS NULL OR FIND_IN_SET(?, p.apply_to_package_price_distance) ";
                        } else {
                            availablePP += " OR FIND_IN_SET(?, p.apply_to_package_price_distance) ";
                        }
                        params.add(String.valueOf(packagePriceNames.getInteger(i)));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availablePP);
                }
                body.remove(APPLY_TO_PACKAGE_PRICE_DISTANCE);
            }

            if (body.containsKey(APPLY_TO_PACKAGE_TYPE)){
                JsonArray packageTypes = body.getJsonArray(APPLY_TO_PACKAGE_TYPE);
                QUERY = QUERY.concat("%s");
                if (!packageTypes.isEmpty()){
                    String availablePP = "";
                    for (int i = 0; i < packageTypes.size(); i++){
                        if (i == 0) {
                            availablePP += " AND (p.apply_to_package_type IS NULL OR FIND_IN_SET(?, p.apply_to_package_type) ";
                        } else {
                            availablePP += " OR FIND_IN_SET(?, p.apply_to_package_type) ";
                        }
                        params.add(packageTypes.getInteger(i));
                    }
                    QUERY = QUERY.concat(")");
                    QUERY = String.format(QUERY, availablePP);
                }
                body.remove(APPLY_TO_PACKAGE_TYPE);
            }

            if(body.containsKey(DATE)){
                QUERY = QUERY.concat(" AND ((? BETWEEN p.since AND p.until) " +
                        " AND (p.available_days = 'all' OR p.available_days REGEXP LOWER(LEFT(DAYNAME(?), 3)))) ");
                String date = body.getString(DATE);
                params.add(date).add(date);
                body.remove(DATE);
            }

            if(!body.containsKey(PARENT_ID)){
                QUERY = QUERY.concat(" AND p.parent_id IS NULL ");
            } else {
                QUERY = QUERY.concat(" AND p.parent_id = ? ");
                Integer parentId = body.getInteger(PARENT_ID);
                params.add(parentId);
                body.remove(PARENT_ID);
            }

            if(body.containsKey(NUM_PRODUCTS)){
                QUERY = QUERY.concat(" AND ((p.num_products IS NULL OR p.num_products = 0) OR ? <= p.num_products) ");
                Integer numProducts = body.getInteger(NUM_PRODUCTS);
                params.add(numProducts);
                body.remove(NUM_PRODUCTS);
            }

            if(body.containsKey(CUSTOMER_ID)){
                QUERY = QUERY.concat(" AND (p.customer_limit = 0 OR (p.customer_limit != 0 AND (p.customer_limit > \n" +
                        " (SELECT\n" +
                        "  SUM(cusprom.used) \n" +
                        " FROM customers_promos cusprom \n" +
                        " LEFT JOIN promos prom ON prom.id = cusprom.promo_id \n" +
                        " WHERE prom.id = p.id \n" +
                        " AND cusprom.customer_id = ? \n" +
                        " AND cusprom.is_promo_customer = FALSE) \n" +
                        " OR (p.customer_limit > (SELECT COUNT(bp.id) FROM boarding_pass bp WHERE bp.customer_id = ? AND bp.promo_id = p.id))))) ");
                Integer customerId = (Integer) body.remove(CUSTOMER_ID);
                params.add(customerId).add(customerId);
            }

            for (Map.Entry<String, Object> field : body) {
                QUERY = QUERY.concat(this.buildParam(field.getKey()));
                params.add(field.getValue());
            }

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultList = reply.result().getRows();
                    if (resultList.isEmpty()){
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(resultList));
                    }
                } catch (Exception e){
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e){
            reportQueryError(message, e);
        }
    }

    private String buildParam(String field){
        String returnString = " AND %s = ? ";
        return String.format(returnString, field);
    }

    /**
     * Get the rule to apply the discount, either types of tickets or package rates
     */
    private JsonArray getApplySettingsBoardingPass(JsonObject resultPromos){
        JsonArray result = new JsonArray();
        result.add(resultPromos.getBoolean("apply_return"));

        if (resultPromos.getString(APPLY_TO_SPECIAL_TICKETS) != null){
            String applyToSpecialTickets = resultPromos.getString(APPLY_TO_SPECIAL_TICKETS);
            if (applyToSpecialTickets.contains(",")){
                result = result.addAll(new JsonArray(Arrays.asList(applyToSpecialTickets.split(","))));
            } else {
                result.add(applyToSpecialTickets);
            }
        }
        return result;
    }

    /**
     * Get the rule to apply the discount, either types of tickets or package rates
     */
    private JsonObject getApplySettingsParcel(JsonObject resultPromos){
        JsonObject result = new JsonObject();

        JsonArray packagePriceNames = new JsonArray();
        String applyToPackagePrice = resultPromos.getString(APPLY_TO_PACKAGE_PRICE);
        if (applyToPackagePrice != null){
            packagePriceNames = new JsonArray(Arrays.asList(applyToPackagePrice.split(",")));
        }
        result.put("package_names", packagePriceNames);


        JsonArray packagePriceDistances = new JsonArray();
        String applyToPackagePriceDistance = (String) resultPromos.getValue(APPLY_TO_PACKAGE_PRICE_DISTANCE);
        if (applyToPackagePriceDistance != null){
            packagePriceDistances = new JsonArray(Arrays.asList(applyToPackagePriceDistance.split(",")));
        }
        result.put("package_distances", packagePriceDistances);

        return result;
    }

    public CompletableFuture<JsonObject> applyPromoCode(SQLConnection conn, SERVICES service, Boolean flagPromo, JsonObject discount, JsonObject bodyService, JsonArray products, JsonArray otherProducts, Boolean flagUserPromo, Integer userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (flagPromo) {
                String discountCode = discount.getString(DISCOUNT_CODE);
                this.getPromoInfo(discountCode).whenComplete((resultPromos, errorPromos) -> {
                    try {
                        if (errorPromos != null){
                            throw errorPromos;
                        }
                        Integer customerId = discount.getInteger(CUSTOMER_ID);
                        Boolean hasSpecificCustomer = resultPromos.getBoolean(HAS_SPECIFIC_CUSTOMER);
                        boolean hasCustomerLimit = !resultPromos.getInteger(CUSTOMER_LIMIT).equals(0);
                        Integer promoId = resultPromos.getInteger(ID);
                        bodyService.put(PROMO_ID, promoId);

                        if (flagUserPromo){
                            this.getUserPromoInfo(promoId, userId).whenComplete((resultUserPromos, errorUserPromos) -> {
                                try {
                                    if (errorUserPromos != null){
                                        throw new Exception(errorUserPromos);
                                    }
                                    if (resultUserPromos != null){
                                        this.updatePromoUsage(conn, "users_promos", promoId, null, userId).whenComplete((resultUsage, errorUsage) -> {
                                            try {
                                                if (errorUsage != null){
                                                    throw new Exception(errorUsage);
                                                }
                                                this.applyDiscount(conn, service, bodyService, products, otherProducts, resultPromos, resultUserPromos).whenComplete((resultApplyDiscount, errorApplyDiscount) -> {
                                                    try {
                                                        if (errorApplyDiscount != null) {
                                                            throw new Exception(errorApplyDiscount);
                                                        }
                                                        future.complete(resultApplyDiscount);
                                                    } catch (Exception e){
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    } else {
                                        this.applyDiscount(conn, service, bodyService, products, otherProducts, resultPromos, null).whenComplete((resultApplyDiscount, errorApplyDiscount) -> {
                                            try {
                                                if (errorApplyDiscount != null) {
                                                    throw new Exception(errorApplyDiscount);
                                                }
                                                future.complete(resultApplyDiscount);
                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }
                                } catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });

                        } else if (customerId != null && hasSpecificCustomer || !hasSpecificCustomer && hasCustomerLimit && customerId != null){
                            this.getCustomerPromoInfo(promoId, customerId).whenComplete((resultCustomerPromos, errorCustomerPromos) -> {
                                try {
                                    if (errorCustomerPromos != null){
                                        throw new Exception(errorCustomerPromos);
                                    }
                                    if (resultCustomerPromos != null){
                                        this.updatePromoUsage(conn, "customers_promos", promoId, customerId, null).whenComplete((resultUsage, errorUsage) -> {
                                            try {
                                                if (errorUsage != null){
                                                    throw new Exception(errorUsage);
                                                }
                                                this.applyDiscount(conn, service, bodyService, products, otherProducts, resultPromos, resultCustomerPromos).whenComplete((resultApplyDiscount, errorApplyDiscount) -> {
                                                    try {
                                                        if (errorApplyDiscount != null) {
                                                            throw new Exception(errorApplyDiscount);
                                                        }
                                                        future.complete(resultApplyDiscount);
                                                    } catch (Exception e){
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    } else {
                                        this.applyDiscount(conn, service, bodyService, products, otherProducts, resultPromos, null).whenComplete((resultApplyDiscount, errorApplyDiscount) -> {
                                            try {
                                                if (errorApplyDiscount != null) {
                                                    throw new Exception(errorApplyDiscount);
                                                }
                                                future.complete(resultApplyDiscount);
                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }
                                } catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        } else {
                            this.updatePromoUsage(conn, "promos", promoId, null, null).whenComplete((resultUsage, errorUsage) -> {
                                try {
                                    if (errorUsage != null){
                                        throw new Exception(errorUsage);
                                    }
                                    this.applyDiscount(conn, service, bodyService, products, otherProducts, resultPromos, null).whenComplete((resultApplyDiscount, errorApplyDiscount) -> {
                                        try {
                                            if (errorApplyDiscount != null) {
                                                throw new Exception(errorApplyDiscount);
                                            }
                                            future.complete(resultApplyDiscount);
                                        } catch (Exception e){
                                            future.completeExceptionally(e);
                                        }
                                    });
                                } catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else if (service.equals(parcel)){
                this.calculatePackageCost(products, bodyService, false).whenComplete((resultPackages, errorPackages) -> {
                    try {
                        if (errorPackages != null) {
                            throw errorPackages;
                        }
                        products.stream()
                                .map(p -> (JsonObject) p)
                                .forEach(p -> {
                                    p.put("total_amount", p.getDouble("total_amount") + p.getDouble("excess_cost"));
                                });
                        future.complete(new JsonObject()
                                .put(SERVICE, bodyService)
                                .put(PRODUCTS, products));
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else if (service.equals(guiapp)){
                this.calculatePackageCost(products, bodyService, true).whenComplete((resultPackages, errorPackages) -> {
                    try {
                        if (errorPackages != null) {
                            throw errorPackages;
                        }
                        products.stream()
                                .map(p -> (JsonObject) p)
                                .forEach(p -> {
                                    p.put("total_amount", p.getDouble("total_amount") + p.getDouble("excess_cost"));
                                });
                        future.complete(new JsonObject()
                                .put(SERVICE, bodyService)
                                .put(PRODUCTS, products));
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else {
                future.complete(new JsonObject().put(SERVICE, bodyService).put(PRODUCTS, products));
            }
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Obtains the object of the promo
     */
    private CompletableFuture<JsonObject> getPromoInfo(String discountCode){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray param = new JsonArray().add(discountCode);
        this.dbClient.queryWithParams(QUERY_GET_PROMO_INFO_BY_DISCOUNT_CODE, param, reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                JsonObject promoInfo = reply.result().getRows().get(0);
                future.complete(promoInfo);
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Obtains the object of the customer promo
     */
    private CompletableFuture<JsonObject> getCustomerPromoInfo(Integer promoId, Integer customerId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_CUSTOMER_PROMO_INFO_BY_PROMO_ID_AND_CUSTOMER_ID, new JsonArray().add(promoId).add(customerId), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }

                List<JsonObject> result = reply.result().getRows();

                if (result.isEmpty()){
                    future.complete(null);
                } else {
                    future.complete(result.get(0));
                }

            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Obtains the object of the user promo
     */
    private CompletableFuture<JsonObject> getUserPromoInfo(Integer promoId, Integer userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_USER_PROMO_INFO_BY_PROMO_ID_AND_USER_ID, new JsonArray().add(promoId).add(userId), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }

                List<JsonObject> result = reply.result().getRows();

                if (result.isEmpty()){
                    future.complete(null);
                } else {
                    future.complete(result.get(0));
                }

            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Update 'used' field the tables depending promos or customer promos
     */
    private CompletableFuture<Boolean> updatePromoUsage(SQLConnection conn, String table, Integer promoId, Integer customerId, Integer userId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            if (conn == null){
                future.complete(true);
            } else {
                String QUERY = "";
                JsonArray params = new JsonArray();
                switch (table) {
                    case "promos":
                        QUERY = QUERY_UPDATE_USAGE_PROMOS;
                        params.add(promoId);
                        break;
                    case "customers_promos":
                        QUERY = QUERY_UPDATE_USAGE_CUSTOMER_PROMOS;
                        params.add(promoId).add(customerId);
                        break;
                    case "users_promos":
                        QUERY = QUERY_UPDATE_USAGE_USER_PROMOS;
                        params.add(promoId).add(userId);
                        break;
                }
                conn.updateWithParams(QUERY, params, reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        future.complete(true);
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            }
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Obtains the aplicable products depending by service
     */
    private CompletableFuture<JsonObject> getAplicableProducts(SERVICES service, JsonArray products, JsonArray applySetting, JsonArray applySettingAditional){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        switch (service) {
            case boardingpass:
                this.getBoardingPassAplicableProducts(products, applySetting).whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        future.complete(result);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
                break;
            case parcel:
            case parcel_inhouse:
                this.getParcelAplicableProducts(products, applySetting, applySettingAditional).whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        future.complete(result);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
                break;
            case rental:
                break;
        }
        return future;
    }

    /**
     * The boardingpass products be filtered by type passenger or range package price name and be validated by the field name and value type.
     * When the values equals at the filter rules value is returned in aplicables array and the rest in not aplicables array
     */
    private CompletableFuture<JsonObject> getBoardingPassAplicableProducts(JsonArray products, JsonArray applySettings){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray aplicables = new JsonArray();
        JsonArray notAplicables = products.copy();
        Boolean applyReturn = (Boolean) applySettings.remove(0);
        products.forEach(p -> {
            try {
                JsonObject product = (JsonObject) p;
                Integer specialTicketId = product.getInteger(SPECIAL_TICKET_ID);

                if (specialTicketId == null){
                    throw new Exception(SPECIAL_TICKET_ID.concat(" required in products elements"));
                }

                if(applyReturn && product.getString(TICKET_TYPE_ROUTE).equals("ida")){
                    return;
                }

                for (int i = 0; i < applySettings.size(); i++){
                    if (specialTicketId.equals(Integer.parseInt(applySettings.getString(i)))){
                        aplicables.add(product);
                        notAplicables.remove(product);
                        break;
                    }
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        future.complete(new JsonObject()
                .put(APLICABLE, aplicables)
                .put(NOT_APLICABLE, notAplicables));

        return future;
    }

    /**
     * The boardingpass products be filtered by type passenger or range package price name and be validated by the field name and value type.
     * When the values equals at the filter rules value is returned in aplicables array and the rest in not aplicables array
     * @param products
     * @param applySettingsPrice Array rules package price value
     * @param applySettingsDistance Array rules package price distance value
     * @return JsonObject with array of products aplicables and other array of boardingpass products not aplicables
     */
    private CompletableFuture<JsonObject> getParcelAplicableProducts(JsonArray products, JsonArray applySettingsPrice, JsonArray applySettingsDistance){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray aplicables = new JsonArray();
        JsonArray notAplicables = products.copy();

        products.forEach(p -> {
            try {
                JsonObject product = (JsonObject) p;
                String packagePriceName = product.getString(PACKAGE_PRICE_NAME);
                Integer packagePriceDistanceId = Integer.parseInt(product.getValue(PACKAGE_PRICE_DISTANCE_ID).toString());

                if (packagePriceName == null){
                    throw new Exception(PACKAGE_PRICE_NAME.concat(" required in products elements"));
                }
                if (packagePriceDistanceId == null){
                    throw new Exception(PACKAGE_PRICE_DISTANCE_ID.concat(" required in products elements"));
                }

                if (applySettingsPrice.isEmpty() && applySettingsDistance.isEmpty()) {
                    aplicables.add(product);
                    notAplicables.remove(product);
                } else if (applySettingsPrice.isEmpty() && !applySettingsDistance.isEmpty()) {
                    boolean flagDistance = false;
                    for (int j = 0; j < applySettingsDistance.size(); j++){
                        if (packagePriceDistanceId.equals(Integer.parseInt(applySettingsDistance.getString(j)))){
                            flagDistance = true;
                            break;
                        }
                    }
                    if (flagDistance){
                        aplicables.add(product);
                        notAplicables.remove(product);
                    }
                } else {
                    for (int i = 0; i < applySettingsPrice.size(); i++){
                        boolean flagName = false;
                        boolean flagDistance = false;
                        if (packagePriceName.equals(String.valueOf(applySettingsPrice.getString(i)))){
                            flagName = true;
                            for (int j = 0; j < applySettingsDistance.size(); j++){
                                if (packagePriceDistanceId.equals(Integer.parseInt(applySettingsDistance.getString(j)))){
                                    flagDistance = true;
                                    break;
                                }
                            }
                        }

                        if (flagName && flagDistance){
                            aplicables.add(product);
                            notAplicables.remove(product);
                            break;
                        }
                    }
                }

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        future.complete(new JsonObject()
                .put(APLICABLE, aplicables)
                .put(NOT_APLICABLE, notAplicables));

        return future;
    }

    private CompletableFuture<JsonObject> applyDiscount(SQLConnection conn, SERVICES service, JsonObject bodyService, JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            DISCOUNT_TYPES discountType = DISCOUNT_TYPES.valueOf(promo.getString(DISCOUNT_TYPE));
            Integer numProducts = promo.getInteger(NUM_PRODUCTS);
            boolean isByNumProducts = discountType.isByNumProducts();

            if (isByNumProducts && products.size() < numProducts){
                throw new Exception(ERRORS.IS_VALID_NUM_PRODUCTS.getMessage());
            }

            if (service.equals(parcel) || service.equals(parcel_inhouse)){
                this.applyDiscountParcel(conn, discountType, service, bodyService,
                        products, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        JsonArray productsReply = result.getJsonArray(PRODUCTS);
                        productsReply.stream()
                                .map(p -> (JsonObject) p)
                                .forEach(p -> {
                                    if(p.getDouble("total_amount")!=null)
                                        p.put("total_amount", p.getDouble("total_amount") + p.getDouble("excess_cost"));
                                });
                        future.complete(result);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else {
                this.applyDiscountBoardingPass(conn, discountType, service, bodyService,
                        products, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
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
        return future;
    }

    private CompletableFuture<JsonObject> applyDiscountBoardingPass(SQLConnection conn, DISCOUNT_TYPES discountType, SERVICES service,
                                                                    JsonObject bodyService, JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos, Boolean isByNumProducts){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray applySettings = this.getApplySettingsBoardingPass(promo);

        if (applySettings.size() > 1){
            this.getAplicableProducts(service, products, applySettings, null).whenComplete((promoProducts, errorPromoProducts) -> {
                try {
                    if (errorPromoProducts != null){
                        throw errorPromoProducts;
                    }
                    JsonArray aplicableProducts = promoProducts.getJsonArray(APLICABLE);
                    if (aplicableProducts.isEmpty()){
                        throw new Exception("Aplicable products list is empty");
                    }
                    JsonArray notAplicableProducts = promoProducts.getJsonArray(NOT_APLICABLE);
                    otherProducts.addAll(notAplicableProducts);

                    this.deduce(conn, service, bodyService, discountType, aplicableProducts, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((resultDeduce, errorDeduce) -> {
                        try {
                            if (errorDeduce != null) {
                                throw new Exception(errorDeduce);
                            }
                            future.complete(resultDeduce);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });

                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } else {
            this.deduce(conn, service, bodyService, discountType, products, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((resultDeduce, errorDeduce) -> {
                try {
                    if (errorDeduce != null) {
                        throw new Exception(errorDeduce);
                    }
                    future.complete(resultDeduce);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private CompletableFuture<JsonObject> applyDiscountParcel(SQLConnection conn, DISCOUNT_TYPES discountType, SERVICES service, JsonObject bodyService,
                                                              JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos, Boolean isByNumProducts){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (parcel.equals(service)) {
                this.applyDiscountParcelCalculate(conn, discountType, service, bodyService,
                        products, otherProducts, promo, customerPromos, isByNumProducts, false).whenComplete((result, err) -> {
                    try {
                        if (err != null) {
                            throw err;
                        }
                        future.complete(result);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } else if (guiapp.equals(service)) {
                this.applyDiscountParcelCalculate(conn, discountType, service, bodyService,
                        products, otherProducts, promo, customerPromos, isByNumProducts, true).whenComplete((result, err) -> {
                    try {
                        if (err != null) {
                            throw err;
                        }
                        future.complete(result);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } else {
                this.applyDiscountParcelInHouse(conn, discountType, service, bodyService,
                        products, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((result, err) -> {
                    try {
                        if (err != null) {
                            throw err;
                        }
                        future.complete(result);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> applyDiscountParcelCalculate(SQLConnection conn, DISCOUNT_TYPES discountType, SERVICES service, JsonObject bodyService,
                                                                       JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos, Boolean isByNumProducts, boolean guiapp) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.calculatePackageCost(products, bodyService, guiapp).whenComplete((resultPackages, errorPackages) -> {
            try {
                if (errorPackages != null) {
                    throw errorPackages;
                }
                JsonObject applySettings = this.getApplySettingsParcel(promo);
                JsonArray applySettingsNames = applySettings.getJsonArray("package_names");
                JsonArray applySettingsDistances = applySettings.getJsonArray("package_distances");

                if (!applySettingsNames.isEmpty() || !applySettingsDistances.isEmpty()){
                    this.getAplicableProducts(service, products, applySettingsNames, applySettingsDistances).whenComplete((promoProducts, errorPromoProducts) -> {
                        try {
                            if (errorPromoProducts != null){
                                throw errorPromoProducts;
                            }
                            JsonArray aplicableProducts = promoProducts.getJsonArray(APLICABLE);
                            if (aplicableProducts.isEmpty()){
                                throw new Exception("Aplicable products list is empty");
                            }
                            JsonArray notAplicableProducts = promoProducts.getJsonArray(NOT_APLICABLE);
                            otherProducts.addAll(notAplicableProducts);

                            this.deduce(conn, service, bodyService, discountType, aplicableProducts, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((resultDeduce, errorDeduce) -> {
                                try {
                                    if (errorDeduce != null) {
                                        throw new Exception(errorDeduce);
                                    }

                                    future.complete(resultDeduce);

                                } catch (Exception e) {
                                    future.completeExceptionally(e);
                                }
                            });

                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    this.calculateDeduce(conn, service, bodyService,  products, otherProducts, promo, customerPromos).whenComplete((calculateDeduce, errorCalculateDeduce) -> {
                        try {
                            if (errorCalculateDeduce != null){
                                throw errorCalculateDeduce;
                            }
                            future.complete(calculateDeduce);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                }

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> applyDiscountParcelInHouse(SQLConnection conn, DISCOUNT_TYPES discountType, SERVICES service, JsonObject bodyService,
                                                                     JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos, Boolean isByNumProducts) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject applySettings = this.getApplySettingsParcel(promo);
        JsonArray applySettingsNames = applySettings.getJsonArray("package_names");
        JsonArray applySettingsDistances = applySettings.getJsonArray("package_distances");

        if (!applySettingsNames.isEmpty() || !applySettingsDistances.isEmpty()){
            this.getAplicableProducts(service, products, applySettingsNames, applySettingsDistances).whenComplete((promoProducts, errorPromoProducts) -> {
                try {
                    if (errorPromoProducts != null){
                        throw errorPromoProducts;
                    }
                    JsonArray aplicableProducts = promoProducts.getJsonArray(APLICABLE);
                    if (aplicableProducts.isEmpty()){
                        throw new Exception("Aplicable products list is empty");
                    }
                    JsonArray notAplicableProducts = promoProducts.getJsonArray(NOT_APLICABLE);
                    otherProducts.addAll(notAplicableProducts);

                    this.deduce(conn, service, bodyService, discountType, aplicableProducts, otherProducts, promo, customerPromos, isByNumProducts).whenComplete((resultDeduce, errorDeduce) -> {
                        try {
                            if (errorDeduce != null) {
                                throw new Exception(errorDeduce);
                            }

                            future.complete(resultDeduce);

                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });

                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } else {
            this.calculateDeduce(conn, service, bodyService,  products, otherProducts, promo, customerPromos).whenComplete((calculateDeduce, errorCalculateDeduce) -> {
                try {
                    if (errorCalculateDeduce != null){
                        throw errorCalculateDeduce;
                    }
                    future.complete(calculateDeduce);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private CompletableFuture<JsonArray> calculatePackageCost(JsonArray packages,  JsonObject body, boolean guiapp) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(packages.stream()
                        .map(p -> calculateCost((JsonObject) p, body, guiapp))
                        .toArray(CompletableFuture[]::new))
                .whenComplete((s, t) -> {
                    if (t != null) { future.completeExceptionally(t);
                    } else {
                        future.complete(packages);
                    }
                });

        return future;
    }

    private CompletableFuture<JsonObject> calculateCost(JsonObject body, JsonObject baseBody, boolean guiapp) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject params = new JsonObject();
        String shippingType = body.getString("shipping_type");
        linearVolume(shippingType, body).whenComplete((linear,error)->{
            try {
                if(error != null){
                    throw error;
                }
                if(shippingType.equals("pets")){
                    body.put("width", linear.getDouble("width"));
                    body.put("height", linear.getDouble("height"));
                    body.put("length", linear.getDouble("length"));
                }
                params.put("linear_volume", linear.getDouble("linear_volume"));
                params.put("weight", body.getDouble("weight"));
                params.put("insurance_value", body.getDouble("insurance_value"));
                params.put("packing_id", body.getInteger("packing_id"));
                params.put("shipping_type", shippingType);
                params.put("terminal_origin_id" ,baseBody.getInteger("terminal_origin_id"));
                params.put("terminal_destiny_id" ,baseBody.getInteger("terminal_destiny_id"));
                if(guiapp) {
                    params.put("guiapp", true);
                    params.put("guiapp_excess", body.getDouble("guiapp_excess"));
                }

                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.CALCULATE_COST);
                vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), params, options, (AsyncResult<Message<JsonObject>> reply) -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }

                        body.mergeIn(reply.result().body());
                        future.complete(body);
                    }catch(Exception e){
                        future.completeExceptionally(reply.cause());

                    }

                });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> linearVolume(String ShippingType, JsonObject body){
        JsonObject result = new JsonObject();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (ShippingType.equals("parcel") || ShippingType.equals("frozen")) {
            Double width = body.getDouble("width");
            Double height = body.getDouble("height");
            Double length = body.getDouble("length");
            //result.put("linear_volume", width + height + length);
            //Float ss= Float.parseFloat(String.format("%.3f", (width * height * length) / 1000000));
            //result.put("linear_volume", Float.parseFloat(String.format("%.3f", (width * height * length) / 1000000)));
            result.put("linear_volume", Float.valueOf(String.format("%.3f", (width * height * length) / 1000000)));
            future.complete(result);
        } else if(ShippingType.equals("pets")){
            Integer s = body.getInteger("pets_sizes_id");
            if(s != null && s>0){
                JsonObject bod = new JsonObject().put("id",s);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_PETSSIZES);
                vertx.eventBus().send(ParcelDBV.class.getSimpleName(), bod, options, handler->{
                    try{
                        if(handler.failed()){
                            throw new Exception(handler.cause());
                        }
                        JsonArray res = (JsonArray) handler.result().body();
                        JsonObject sizes = res.getJsonObject(0);
                        if(sizes != null && sizes.containsKey("height") && sizes.containsKey("width") && sizes.containsKey("length")){
                            Double width = sizes.getDouble("width");
                            Double height = sizes.getDouble("height");
                            Double length = sizes.getDouble("length");
                            result.put("width", width);
                            result.put("height", height);
                            result.put("length", length);
                            result.put("linear_volume",width + height + length);
                            future.complete(result);
                        }else{
                            future.completeExceptionally(new Throwable("No se encontro el pets_sizes_id"));
                        }

                    }catch (Exception ex){
                        future.completeExceptionally(handler.cause());
                    }
                });
            }else{
                future.completeExceptionally(new Throwable("En el caso shipping_type='pets' el campo pets_sizes_id es requerido"));
            }
        } else {
            result.put("linear_volume", 0.0);
            future.complete(result);
        }
        return future;
    }

    private void prepareProductToInsert(SERVICES service, JsonObject product){
        switch (service){
            case boardingpass:
                double totalAmount = product.getDouble(TOTAL_AMOUNT) + product.getDouble(EXTRA_CHARGES, 0.00);
                product.remove("boarding_pass_passenger_id");
                product.put(TOTAL_AMOUNT, totalAmount);
                product.remove("ticket_type");
                product.remove("ticket_type_route");
                product.remove(SPECIAL_TICKET_ID);
                product.remove(PROMO_DISCOUNT);
                product.remove(CREATED_AT);
                product.remove("boarding_pass_ticket_id");
                break;
            case parcel:
                product.remove(PACKAGE_PRICE_NAME);
                product.remove(PACKAGE_PRICE_DISTANCE_ID);
            case rental:
                break;
        }
    }

    private CompletableFuture<JsonObject> calculateDeduce(SQLConnection conn, SERVICES service, JsonObject bodyService, JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {

            Double othersAmount = 0.00;
            Double othersDiscount = 0.00;

            for (int i = 0; i < otherProducts.size(); i++){
                JsonObject otherProduct = otherProducts.getJsonObject(i);
                othersAmount += otherProduct.getDouble(AMOUNT, otherProduct.getDouble("cost", 0.00));
                othersDiscount += otherProduct.getDouble(DISCOUNT, 0.0);
            }

            double finalServiceDiscount = othersDiscount;
            double finalServiceAmount = othersAmount;

            List<String> updateTasks = new ArrayList<>();

            for (int i = 0; i<products.size(); i++){
                JsonObject result = products.getJsonObject(i);
                Double productAmount = result.getDouble(AMOUNT, result.getDouble("cost", 0.00));

                Double productDiscount = result.getDouble(DISCOUNT, 0.00);
                boolean isDiscountExceed = productDiscount > productAmount;
                if (isDiscountExceed) {
                    result.put(PROMO_DISCOUNT, productAmount);
                    productDiscount = productAmount;
                }

                Double productTotalAmount = result.getDouble(TOTAL_AMOUNT, result.getDouble("cost"));
                productTotalAmount = productTotalAmount < 0 ? 0 : productTotalAmount;

                finalServiceDiscount += productDiscount;
                finalServiceAmount += productAmount;

                result.put(AMOUNT, UtilsMoney.round(productAmount, service.getMoneyRoundPlaces()));
                result.put(DISCOUNT, UtilsMoney.round(productDiscount, service.getMoneyRoundPlaces()));
                result.put(TOTAL_AMOUNT, UtilsMoney.round(productTotalAmount, service.getMoneyRoundPlaces()));

                if (service.equals(SERVICES.boardingpass)){
                    JsonObject productToInsert = result.copy();
                    this.prepareProductToInsert(service, productToInsert);
                    updateTasks.add(this.generateGenericUpdateString(service.getProductTable(), productToInsert));
                }
            }

            bodyService.put(PROMO_ID, promo.getInteger(ID));
            bodyService.put(AMOUNT, UtilsMoney.round(finalServiceAmount, service.getMoneyRoundPlaces()));
            bodyService.put(DISCOUNT, UtilsMoney.round(finalServiceDiscount, service.getMoneyRoundPlaces()));
            bodyService.put(TOTAL_AMOUNT,  UtilsMoney.round(finalServiceAmount - finalServiceDiscount, service.getMoneyRoundPlaces()));

            otherProducts = new JsonArray(otherProducts.stream().filter(op -> {
                JsonObject other = (JsonObject) op;
                return !other.containsKey("packing_id");
            }).collect(Collectors.toList()));

            JsonObject result = new JsonObject();

            result
                    .put(DISCOUNT, promo)
                    .put(CUSTOMERS_PROMOS, customerPromos)
                    .put(SERVICE, bodyService)
                    .put(PRODUCTS, products.addAll(otherProducts));

            if (conn != null){
                conn.batch(updateTasks, reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        future.complete(result);
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            } else {
                future.complete(result);
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    /**
     * Products filter applied by business rule.
     * Boardingpass: when the trip is round the discount is applied in the return ticket.
     * @param service
     * @param products
     * @return Products to deduce
     */
    private JsonArray getProductsToDeduce(SERVICES service, Boolean isRound, Boolean apply_return, JsonArray products){
        JsonArray productsToDeduce = new JsonArray();
        switch (service){
            case boardingpass:
                if (isRound){
                    for (int i = 0; i < products.size(); i++){
                        JsonObject product = products.getJsonObject(i);
                        if (product.getString("ticket_type_route").equals("regreso") || !apply_return){
                            productsToDeduce.add(product);
                        }
                    }
                } else {
                    productsToDeduce = products;
                }
                break;
            case parcel:
                productsToDeduce = products;
            case parcel_inhouse:
                productsToDeduce = products;
            case rental:
                productsToDeduce = products;
                break;
        }
        return productsToDeduce;
    }

    private CompletableFuture<JsonObject> deduce(SQLConnection conn, SERVICES service, JsonObject bodyService, DISCOUNT_TYPES discountType,
                                                 JsonArray products, JsonArray otherProducts, JsonObject promo, JsonObject customerPromos, Boolean isByNumProducts){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {

            boolean isRound = service.equals(boardingpass) && promo.getString(RULE) != null;
            JsonArray productsToDeduce = this.getProductsToDeduce(service, isRound, promo.getBoolean("apply_return"), products);

            Boolean discountPerBase = promo.getBoolean(DISCOUNT_PER_BASE);
            Double promoDiscount = promo.getDouble(DISCOUNT);

            CompletableFuture<JsonArray> deduceProducts ;
            if (isByNumProducts){
                deduceProducts = deduceOnlyOne(service, discountType, products, productsToDeduce, discountPerBase, promoDiscount);
            } else {
                deduceProducts = deduceAll(service, discountType, productsToDeduce, discountPerBase, promoDiscount);
            }

            deduceProducts.whenComplete((resultDeduceProducts, errorDeduceProducts) -> {
                try {
                    if (errorDeduceProducts != null){
                        throw errorDeduceProducts;
                    }

                    this.calculateDeduce(conn, service, bodyService,  products, otherProducts, promo, customerPromos).whenComplete((calculateDeduce, errorCalculateDeduce) -> {
                        try {
                            if (errorCalculateDeduce != null){
                                throw errorCalculateDeduce;
                            }
                            future.complete(calculateDeduce);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });

                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });

        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonArray> deduceAll(SERVICES service, DISCOUNT_TYPES discountType, JsonArray productsToDeduce, Boolean discountPerBase, Double promoDiscount){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {

            List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
            productsToDeduce.forEach(p -> {
                JsonObject product = (JsonObject) p;
                tasks.add(this.calculateDiscount(service, discountType, product, discountPerBase, promoDiscount));
            });

            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resultCalculateDiscount, errorCalculateDiscount) -> {
                try {
                    if (errorCalculateDiscount != null){
                        throw new Exception(errorCalculateDiscount);
                    }
                    future.complete(productsToDeduce);
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonArray> deduceOnlyOne(SERVICES service, DISCOUNT_TYPES discountType, JsonArray products,
                                                       JsonArray productsToDeduce, Boolean discountPerBase, Double promoDiscount){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            JsonObject productReference = productsToDeduce.getJsonObject(0);
            int indexMinorAmount = 0;
            for(int i = 0; i < productsToDeduce.size(); i++){
                JsonObject product = productsToDeduce.getJsonObject(i);
                indexMinorAmount = (productReference.getDouble(AMOUNT, product.getDouble("cost", 0.00)).compareTo(product.getDouble(AMOUNT, product.getDouble("cost", 0.00))) == 1) ? i : indexMinorAmount;
            }
            productReference = productsToDeduce.getJsonObject(indexMinorAmount);

            this.calculateDiscount(service, discountType, productReference, discountPerBase, promoDiscount).whenComplete((resultCalculateDiscount, errorCalculateDiscount) -> {
                try {
                    if (errorCalculateDiscount != null){
                        throw new Exception(errorCalculateDiscount);
                    }
                    future.complete(products);
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> calculateDiscount(SERVICES service, DISCOUNT_TYPES discountType, JsonObject product,
                                                            Boolean discountPerBase, Double referenceDiscount){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Double amount = product.getDouble(AMOUNT, product.getDouble("cost", 0.00));
        Double discount = product.getDouble(DISCOUNT, 0.0);
        Double totalAmount;

        totalAmount = product.getDouble(TOTAL_AMOUNT, product.getDouble("cost", 0.00));

        this.calculate(service, discountType, product, discountPerBase, amount, discount, totalAmount, referenceDiscount).whenComplete((resultCalculate, errorCalculate) -> {
            try {
                if (errorCalculate != null){
                    throw new Exception(errorCalculate);
                }
                future.complete(resultCalculate);
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> calculate(SERVICES service, DISCOUNT_TYPES discountType, JsonObject product, Boolean discountPerBase, Double amount, Double discount, Double totalAmount, Double referenceDiscount){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject result;
        boolean isBoardingPassService = service.equals(boardingpass);
        if (discountType.isFree()){
            double operation = discount + totalAmount;
            result = this.execCalculate(service, product, amount, discount, operation, true);
        } else if (discountType.isAsPrice()){
            Double operation = totalAmount - referenceDiscount;
            result = this.execCalculate(service, product, amount, discount, operation, false);
        } else {
            boolean appliedToAmount = isBoardingPassService ? discountPerBase : false;
            if (discountType.isPercent()){
                double operation = (amount * (referenceDiscount / 100));
                if (isBoardingPassService && !discountPerBase) {
                    operation = (totalAmount * (referenceDiscount / 100));
                }
                result = this.execCalculate(service, product, amount, discount, operation, appliedToAmount);
            } else {
                result = this.execCalculate(service, product, amount, discount, referenceDiscount, appliedToAmount);
            }
        }
        future.complete(result);
        return future;
    }

    private JsonObject execCalculate(SERVICES service, JsonObject product, Double amount, Double discount, Double operation, Boolean appliedToAmount){
        if (appliedToAmount){
            discount = operation;
        } else {
            discount = discount + operation;
        }
        product.put(TOTAL_AMOUNT, amount - discount);
        product.put(DISCOUNT, discount);
        product.put(PROMO_DISCOUNT, UtilsMoney.round(operation, service.getMoneyRoundPlaces()));
        return product;
    }

    private void getDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        int promoId = body.getInteger(ID);
        getPromoInfo(promoId).whenComplete((promo, promoError) -> {
            try {
                if (promoError != null) {
                    throw promoError;
                }
                getCustomersByPromoId(promoId).whenComplete((customers, customersError) -> {
                    try {
                        if (customersError != null) {
                            throw customersError;
                        }
                        promo.put("customers", customers);
                        getUsersByPromoId(promoId).whenComplete((users, usersError) -> {
                            try {
                                if (usersError != null) {
                                    throw usersError;
                                }
                                promo.put("users", customers);
                                message.reply(promo);
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
    }

    private CompletableFuture<JsonObject> getPromoInfo(int promoId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PROMO_INFO_BY_ID, new JsonArray().add(promoId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Throwable("Promo not found");
                }
                JsonObject promo = result.get(0);
                SERVICES service = SERVICES.valueOf(promo.getString("service"));
                if (!service.isPackageService()) {
                    future.complete(promo);
                    return;
                }

                String typePackages = promo.getString("type_packages");
                String applyToPackagePrice = promo.getString("apply_to_package_price");
                getPackagePriceByNames(applyToPackagePrice, service, typePackages).whenComplete((packagePrices, ppError) -> {
                    try {
                        if (ppError != null) {
                            throw ppError;
                        }
                        promo.put("apply_to_package_price_values", packagePrices);
                        String applyToPackagePriceDistance = promo.getString("apply_to_package_price_distance");
                        getPackagePriceDistancesByIds(applyToPackagePriceDistance, typePackages).whenComplete((packagePriceDistances, pdError) -> {
                            try {
                                if (pdError != null) {
                                    throw pdError;
                                }
                                promo.put("apply_to_package_price_distance_values", packagePriceDistances);
                                String applyToPackageType = promo.getString("apply_to_package_type");
                                getPackageTypesByIds(applyToPackageType, service, typePackages).whenComplete((packageTypes, ptError) -> {
                                    try {
                                        if (ptError != null) {
                                            throw ptError;
                                        }
                                        promo.put("apply_to_package_type_values", packageTypes);
                                        future.complete(promo);
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

    private CompletableFuture<JsonArray> getPackagePriceByNames(String applyToPackagePrice, SERVICES service, String typePackages) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        StringBuilder QUERY = new StringBuilder(QUERY_GET_PACKAGE_PRICES);

        if (Objects.nonNull(applyToPackagePrice) && !applyToPackagePrice.isEmpty()) {
            String params = Arrays.stream(applyToPackagePrice.split(","))
                    .map(pp -> "\'" + pp + "\'")
                    .collect(Collectors.joining(", "));
            QUERY.append("AND name_price IN (").append(params).append(") \n");
        }

        if (Objects.nonNull(typePackages)) {
            if (typePackages.equals("all")) {
                if (service.equals(parcel_inhouse)) {
                    QUERY.append("AND shipping_type = 'inhouse'\n");
                } else {
                    QUERY.append("AND shipping_type != 'inhouse'\n");
                }
            } else {
                QUERY.append("AND shipping_type = ").append(typePackages).append("\n");
            }
        } else {
            if (service.equals(parcel_inhouse)) {
                QUERY.append("AND shipping_type = 'inhouse'\n");
            } else {
                QUERY.append("AND shipping_type != 'inhouse'\n");
            }
        }

        this.dbClient.query(QUERY.toString(), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(new JsonArray(reply.result().getRows()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPackagePriceDistancesByIds(String applyToPackagePriceDistances, String typePackages) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        StringBuilder QUERY = new StringBuilder(QUERY_GET_PACKAGE_DISTANCES);

        if (Objects.nonNull(applyToPackagePriceDistances) && !applyToPackagePriceDistances.isEmpty()) {
            QUERY.append("AND id IN (").append(applyToPackagePriceDistances).append(") \n");
        }

        if (Objects.nonNull(typePackages) && !typePackages.equals("all")) {
            QUERY.append("AND shipping_type = ").append(typePackages).append("\n");
        }

        this.dbClient.query(QUERY.toString(), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(new JsonArray(reply.result().getRows()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPackageTypesByIds(String applyToPackageTypes, SERVICES service, String typePackages) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        StringBuilder QUERY = new StringBuilder(QUERY_GET_PACKAGE_TYPES);

        if (Objects.nonNull(applyToPackageTypes)) {
            QUERY.append("AND id IN (").append(applyToPackageTypes).append(") \n");
        }

        if (Objects.nonNull(typePackages) && !typePackages.equals("all")) {
            QUERY.append("AND shipping_type = ").append(typePackages).append("\n");
        }

        if (service.equals(parcel_inhouse)) {
            QUERY.append("AND allowed_inhouse IS TRUE ").append("\n");
        }

        this.dbClient.query(QUERY.toString(), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(new JsonArray(reply.result().getRows()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getCustomersByPromoId(int promoId) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_CUSTOMERS_PROMO_INFO_BY_ID, new JsonArray().add(promoId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(new JsonArray(reply.result().getRows()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getUsersByPromoId(int promoId) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_USERS_PROMO_INFO_BY_ID, new JsonArray().add(promoId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(new JsonArray(reply.result().getRows()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }



    private static final String QUERY_VALIDATE_USER_PROMO_CODE = "SELECT\n" +
            "  IF(p.apply_to_special_tickets IS NULL %s, true, false) is_valid_special_ticket,\n" +
            "  IF(p.apply_to_package_price IS NULL %s, true, false) is_valid_package_price,\n" +
            "  IF(p.apply_to_package_price_distance IS NULL %s, true, false) is_valid_package_price_distance,\n" +
            "  IF(p.service = ?, true, false) AS is_valid_service,\n" +
            "  IF(? BETWEEN p.since AND p.until, true, false) AS is_valid_date,\n" +
            "  IF(p.available_days = 'all' OR p.available_days REGEXP LOWER(LEFT(DAYNAME(?), 3)), true, false) AS is_valid_day,\n" +
            "  IF((SELECT \n" +
            "      COUNT(usrprom.promo_id)\n" +
            "     FROM users_promos usrprom\n" +
            "     LEFT JOIN users u ON u.id = usrprom.user_id\n" +
            "     WHERE u.status = 1\n" +
            "     AND usrprom.status = 1\n" +
            "     AND usrprom.promo_id = p.id\n" +
            "     AND usrprom.user_id = ?\n" +
            "     AND (usrprom.usage_limit = 0 OR (usrprom.usage_limit > 0 AND usrprom.used < usrprom.usage_limit))) > 0, true, false) AS is_valid_user,\n" +
            "  IF(p.usage_limit = 0 OR (p.usage_limit > 0 AND p.used < p.usage_limit), true, false) AS is_valid_usage,\n" +
            "  IF(((p.discount_type = 'free_n_product' OR p.discount_type = 'discount_n_product') AND (IF(p.rule IS NULL, p.num_products, p.num_products * 2) <= ?)) OR\n" +
            "      (p.discount_type = 'direct_amount' OR p.discount_type = 'direct_percent' OR p.discount_type = 'as_price'), true, false) AS is_valid_num_products,\n" +
            "  IF(p.rule IS NULL OR p.service != 'boardingpass', true, IF(p.service = 'boardingpass' AND p.rule = ?, true, false)) AS is_valid_rule_for_boardingpass,\n" +
            "  IF(p.type_packages IS NULL OR (p.service != 'parcel' OR p.service != 'parcel_inhouse'), true, IF((p.service = 'parcel' OR p.service = 'parcel_inhouse') AND p.type_packages = ?, true, false)) AS is_valid_type_package,\n" +
            "  IF(p.purchase_origin IS NULL OR p.purchase_origin = ?, true, false) AS is_valid_purchase_origin,\n" +
            "  IF(p.customer_limit = 0 OR (p.customer_limit != 0 AND (p.customer_limit > \n" +
            "   (SELECT\n" +
            "       SUM(usrprom.used)\n" +
            "   FROM users_promos usrprom\n" +
            "   LEFT JOIN promos prom ON prom.id = usrprom.promo_id\n" +
            "   WHERE prom.id = p.id\n" +
            "   AND usrprom.user_id = ?))), true, false) AS is_valid_general_user_limit,\n" +
            "  IF(p.apply_only_first_purchase IS FALSE OR (p.apply_only_first_purchase IS TRUE AND (SELECT COUNT(id) FROM boarding_pass WHERE created_by = ? AND boardingpass_status NOT IN (0, 4)) = 0), true, false) AS is_valid_only_first_purchase\n" +
            " FROM promos p\n" +
            " INNER JOIN users_promos up ON up.promo_id = p.id \n" +
            " INNER JOIN users u ON u.id = up.user_id AND u.id = ?\n" +
            " WHERE p.status = 1\n" +
            " AND p.discount_code = ?;";

    private static final String QUERY_VALIDATE_PROMO_CODE_VALIDATIONS = "SELECT\n" +
            "  IF(p.apply_to_special_tickets IS NULL %s, true, false) is_valid_special_ticket,\n" +
            "  IF(p.apply_to_package_price IS NULL %s, true, false) is_valid_package_price,\n" +
            "  IF(p.apply_to_package_price_distance IS NULL %s, true, false) is_valid_package_price_distance,\n" +
            "  IF(p.service = ?, true, false) AS is_valid_service,\n" +
            "  IF(? BETWEEN p.since AND p.until, true, false) AS is_valid_date,\n" +
            "  IF(p.available_days = 'all' OR p.available_days REGEXP LOWER(LEFT(DAYNAME(?), 3)), true, false) AS is_valid_day,\n" +
            "  IF(p.has_specific_customer = 0 OR p.has_specific_customer = 1 AND \n" +
            "    (SELECT \n" +
            "      COUNT(cusprom.promo_id)\n" +
            "     FROM customers_promos cusprom\n" +
            "     LEFT JOIN customer c ON c.id = cusprom.customer_id\n" +
            "     WHERE c.status = 1\n" +
            "     AND cusprom.status = 1\n" +
            "     AND cusprom.promo_id = p.id\n" +
            "     AND cusprom.customer_id = ?\n" +
            "     AND (cusprom.usage_limit = 0 OR (cusprom.usage_limit > 0 AND cusprom.used < cusprom.usage_limit))) > 0, true, false) AS is_valid_customer,\n" +
            "  IF(p.usage_limit = 0 OR (p.usage_limit > 0 AND p.used < p.usage_limit), true, false) AS is_valid_usage,\n" +
            "  IF(((p.discount_type = 'free_n_product' OR p.discount_type = 'discount_n_product') AND (IF(p.rule IS NULL, p.num_products, p.num_products * 2) <= ?)) OR\n" +
            "      (p.discount_type = 'direct_amount' OR p.discount_type = 'direct_percent' OR p.discount_type = 'as_price'), true, false) AS is_valid_num_products,\n" +
            "  IF(p.rule IS NULL OR p.service != 'boardingpass', true, IF(p.service = 'boardingpass' AND p.rule = ?, true, false)) AS is_valid_rule_for_boardingpass,\n" +
            "  IF(p.type_packages IS NULL OR (p.service != 'parcel' OR p.service != 'parcel_inhouse'), true, IF((p.service = 'parcel' OR p.service = 'parcel_inhouse') AND p.type_packages = ?, true, false)) AS is_valid_type_package,\n" +
            "  IF(p.purchase_origin IS NULL OR p.purchase_origin = ?, true, false) AS is_valid_purchase_origin,\n" +
            "  IF(p.customer_limit = 0 OR (p.customer_limit != 0 AND (p.customer_limit > \n" +
            "   (SELECT\n" +
            "       SUM(cusprom.used)\n" +
            "   FROM customers_promos cusprom\n" +
            "   LEFT JOIN promos prom ON prom.id = cusprom.promo_id\n" +
            "   WHERE prom.id = p.id\n" +
            "   AND cusprom.customer_id = ?\n" +
            "   AND cusprom.is_promo_customer = FALSE) " +
            "   OR (p.customer_limit > (SELECT COUNT(bp.id) FROM boarding_pass bp WHERE bp.customer_id = ? AND bp.promo_id = p.id)))), true, false) AS is_valid_general_customer_limit,\n" +
            "  IF(p.apply_only_first_purchase IS FALSE OR (p.apply_only_first_purchase IS TRUE AND (SELECT COUNT(id) FROM boarding_pass WHERE customer_id = ? AND boardingpass_status NOT IN (0, 4)) = 0), true, false) AS is_valid_only_first_purchase,\n" +
            "  IF(p.apply_only_phone_validated = 0 OR (p.apply_only_phone_validated = 1 AND (SELECT user_id FROM customer WHERE id = ?) IS NOT NULL AND (SELECT is_phone_verified FROM users WHERE id = (SELECT user_id FROM customer WHERE id = ?)) = 1), true, false) AS is_valid_phone_verification";

    private static final String QUERY_VALIDATE_PROMO_CODE_WHERE = "\n FROM promos p\n" +
            "  LEFT JOIN customers_promos cp ON cp.promo_id = p.id \n" +
            " LEFT JOIN customer c ON c.id = cp.customer_id AND c.id = ?\n" +
            " LEFT JOIN boarding_pass bp ON bp.customer_id = cp.customer_id \n" +
            " WHERE p.status = 1\n" +
            " AND p.discount_code = ?;";

    private static final String QUERY_GET_LIST_PROMOS = "SELECT\n" +
            " p.*\n" +
            " FROM promos p\n" +
            " WHERE p.status = 1\n" +
            " AND (p.usage_limit = 0 OR (p.usage_limit > 0 AND p.used < p.usage_limit)) ";

    private static final String QUERY_GET_CUSTOMER_PROMO_INFO_BY_PROMO_ID_AND_CUSTOMER_ID = "SELECT usage_limit, used FROM customers_promos WHERE status = 1 AND promo_id = ? AND customer_id = ?;";

    private static final String QUERY_GET_USER_PROMO_INFO_BY_PROMO_ID_AND_USER_ID = "SELECT usage_limit, used FROM users_promos WHERE status = 1 AND promo_id = ? AND user_id = ?;";

    private static final String QUERY_UPDATE_USAGE_PROMOS = "UPDATE promos SET used = used + 1 WHERE id = ?;";

    private static final String QUERY_UPDATE_USAGE_CUSTOMER_PROMOS = "UPDATE customers_promos SET used = used + 1 WHERE promo_id = ? AND customer_id = ?;";

    private static final String QUERY_UPDATE_USAGE_USER_PROMOS = "UPDATE users_promos SET used = used + 1 WHERE promo_id = ? AND user_id = ?;";

    private static final String QUERY_GET_PROMO_INFO_BY_DISCOUNT_CODE = "SELECT \n" +
            " id, \n" +
            " discount_code, \n" +
            " name, \n" +
            " description, \n" +
            " apply_return, \n" +
            " service, \n" +
            " discount_type, \n" +
            " discount,\n" +
            " IF(service = 'boardingpass' AND rule IS NULL, num_products, num_products * 2) AS num_products, \n" +
            " rule, \n" +
            " rule_for_packages, \n" +
            " type_packages, \n" +
            " usage_limit, \n" +
            " used, \n" +
            " available_days,\n" +
            " customer_limit,\n" +
            " apply_to_special_tickets, \n" +
            " apply_to_package_price, \n" +
            " apply_to_package_price_distance, \n" +
            " has_specific_customer, \n" +
            " discount_per_base, \n" +
            " since, \n" +
            " until, \n" +
            " apply_rad, \n" +
            " apply_ead, \n" +
            " status \n" +
            "FROM promos \n" +
            "WHERE status = 1 \n" +
            "AND discount_code = ?;";

    private  static  final String QUERY_PACKINGS_BY_ID ="select * from packings where id =? limit 1;";

    private static final String QUERY_GET_VALUE_FROZEN_COST = "SELECT * FROM general_setting WHERE field = 'frozen_cost';";
    private static final String QUERY_GET_VALUE_PETS_COST = "SELECT * FROM general_setting WHERE field = 'pets_cost';";

    //query to obtain price by dimensions
    private static final String QUERY_LINEAR_VOLUME = "SELECT * FROM package_price \n" +
            "WHERE min_m3 <= ? AND max_m3 >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";
    //query to obtain price by weight
    private static final String QUERY_WEIGHT = "SELECT * FROM package_price \n" +
            "WHERE min_weight <= ? AND max_weight >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";

    private static final String QUERY_MAXIMUM_RATE = "SELECT \n"
            +" min_linear_volume, \n"
            +" max_linear_volume, \n"
            +" min_m3, \n"
            +" max_m3, \n"
            +" min_weight, \n"
            +" max_weight, \n"
            + "price, \n"
            +" currency_id \n"
            +" FROM package_price \n"
            +" WHERE max_m3 = (SELECT MAX(max_m3) FROM package_price \n"
            +" WHERE shipping_type = 'parcel' AND status = 1);";

    public static final String QUERY_DISTANCE_KM_BY_DESTINATION_ID = "SELECT cd.*\n" +
            "FROM config_destination AS cd\n" +
            "JOIN schedule_route_destination AS srd\n" +
            "ON srd.config_destination_id = cd.id \n" +
            "WHERE srd.id = ?\n" +
            "LIMIT 1;";

    private static final String QUERY_GET_PROMO_APPLY_FIRST_PURCHASE = "SELECT\n" +
            "  p.*\n" +
            " FROM promos p\n" +
            " LEFT JOIN customer c ON c.id = ?\n" +
            " LEFT JOIN boarding_pass bp ON bp.customer_id = c.id AND bp.boardingpass_status NOT IN (0,4) AND bp.purchase_origin = ?\n" +
            " WHERE p.service = 'boardingpass'\n" +
            " AND (p.purchase_origin IS NULL OR p.purchase_origin = ?)\n" +
            " AND ? BETWEEN p.since AND p.until\n" +
            " AND (SELECT MAX(bp.id)) IS NULL\n" +
            " AND p.apply_only_first_purchase = TRUE\n" +
            " AND p.status = 1\n" +
            " GROUP BY p.id;";

    //query to obtain price by dimensions
    private static final String QUERY_LINEAR_VOLUME_GUIAPP = "SELECT * FROM package_price \n" +
            "WHERE min_m3 <= ? AND max_m3 >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";

    //query to obtain price by weight
    private static final String QUERY_WEIGHT_GUIAPP = "SELECT * FROM package_price \n" +
            "WHERE min_weight <= ? AND max_weight >= ? AND shipping_type = 'parcel' \n" +
            "ORDER BY price DESC LIMIT 1;";

    private static final String QUERY_PROMO_INFO_BY_CODE = "SELECT * FROM promos WHERE discount_code = ?";

    private static final String QUERY_BIRTHDAY_PROMO_VALIDATION = "IF(\n" +
            "    p.is_holiday = 1 AND \n" +
            "    p.holiday = 'birthday' AND \n" +
            "    MONTH(\n" +
            "        (SELECT c.birthday \n" +
            "         FROM customer c \n" +
            "         WHERE c.id = ? AND \n" +
            "               c.birthday IS NOT NULL AND \n" +
            "               TRIM(c.birthday) <> '')\n" +
            "    ) = MONTH(CURDATE()), \n" +
            "    true, \n" +
            "    false\n" +
            ") AS is_valid_birthday";

    private static final String QUERY_GET_PROMO_INFO_BY_ID = "SELECT\n" +
            "\tp.id, p.name, p.description, p.since, p.until,\n" +
            "    p.discount_code, p.service, p.discount_type, p.discount, p.type_packages,\n" +
            "    p.apply_to_special_tickets, p.apply_to_package_price,\n" +
            "    p.apply_to_package_price_distance, p.apply_to_package_type, p.apply_sender_addressee,\n" +
            "    p.usage_limit, p.purchase_origin, p.num_products, p.rule,\n" +
            "    p.apply_only_first_purchase, p.apply_only_phone_validated,\n" +
            "    p.is_holiday, p.holiday, p.available_days, p.status\n" +
            "FROM promos p\n" +
            "WHERE p.id = ?;";

    private static final String QUERY_GET_PACKAGE_PRICE_BY_NAMES = "SELECT\n" +
            "\tid, name_price\n" +
            "FROM package_price\n" +
            "WHERE name_price IN (%s) \n" +
            "AND shipping_type = %s \n" +
            "AND status = 1;";

    private static final String QUERY_GET_PACKAGE_PRICES = "SELECT\n" +
            "\tid, name_price, shipping_type\n" +
            "FROM package_price\n" +
            "WHERE status = 1 \n";

    private static final String QUERY_GET_PACKAGE_DISTANCES = "SELECT\n" +
            "\tid, min_km, max_km, shipping_type\n" +
            "FROM package_price_km\n" +
            "WHERE status = 1 \n";

    private static final String QUERY_GET_PACKAGE_TYPES = "SELECT\n" +
            "\tid, name, shipping_type\n" +
            "FROM package_types\n" +
            "WHERE status = 1 ";

    private static final String QUERY_GET_CUSTOMERS_PROMO_INFO_BY_ID = "SELECT\n" +
            "\tcp.id,\n" +
            "    c.first_name AS customer_first_name,\n" +
            "    c.last_name AS customer_last_name,\n" +
            "    cp.created_at,\n" +
            "    cp.created_by,\n" +
            "    cp.is_promo_customer,\n" +
            "    cp.promo_id,\n" +
            "    cp.status,\n" +
            "    cp.updated_at,\n" +
            "    cp.updated_by,\n" +
            "    cp.usage_limit,\n" +
            "    cp.used\n" +
            "FROM customers_promos cp\n" +
            "INNER JOIN customer c ON c.id = cp.customer_id\n" +
            "WHERE cp.promo_id = ?\n" +
            "AND c.status = 1;";

    private static final String QUERY_GET_USERS_PROMO_INFO_BY_ID = "SELECT\n" +
            "\tup.id,\n" +
            "    u.id AS user_id,\n" +
            "    u.name AS users_name,\n" +
            "    u.email AS users_email,\n" +
            "    up.created_at,\n" +
            "    up.created_by,\n" +
            "    up.promo_id,\n" +
            "    up.status,\n" +
            "    up.updated_at,\n" +
            "    up.updated_by,\n" +
            "    up.usage_limit,\n" +
            "    up.used\n" +
            "FROM users_promos up\n" +
            "INNER JOIN users u ON u.id = up.user_id\n" +
            "WHERE up.promo_id = ?\n" +
            "AND u.status = 1;";

}
