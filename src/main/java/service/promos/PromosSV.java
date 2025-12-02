/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.promos;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.parcel.ParcelsPackagesDBV;
import database.parcel.enums.SHIPMENT_TYPE;
import database.promos.PromosDBV;
import database.promos.enums.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import service.commons.Constants;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PromosMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsDate;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static database.promos.PromosDBV.CUSTOMER_ID;
import static database.promos.PromosDBV.DISCOUNT;
import static database.promos.PromosDBV.*;
import static service.commons.Constants.TICKET_TYPE_ROUTE;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author Indq tech - Gerardo Valdes Uriarte
 */
public class PromosSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PromosDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/promos";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST,"/", AuthMiddleware.getInstance(),this::register);
        this.addHandler(HttpMethod.POST,"/checkPromoCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(),this::checkPromoCode);
        this.addHandler(HttpMethod.POST,"/checkUserPromoCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(),this::checkUserPromoCode);
        this.addHandler(HttpMethod.POST, "/list", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getList);
        this.addHandler(HttpMethod.POST, "/calculatePromoCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), PromosMiddleware.getInstance(vertx), this::calculatePromoCode);
        this.addHandler(HttpMethod.POST,"/register/massive", AuthMiddleware.getInstance(),this::registerMassive);
        this.addHandler(HttpMethod.GET, "/getGeneralByOrigin/:purchase_origin/:customer_id", AuthMiddleware.getInstance(), this::getGeneralByOrigin);
        this.addHandler(HttpMethod.POST, "/calculateMultiplePromo", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::calculateMultiplePromo);
        this.addHandler(HttpMethod.GET, "/getDetail/:id", AuthMiddleware.getInstance(), this::getDetail);
        this.addHandler(HttpMethod.POST, "/customerPromoReport", AuthMiddleware.getInstance(), this::customerPromoReport);
        super.start(startFuture);
    }

    private void register(RoutingContext context){
        if (this.isValidCreateData(context)){
            try {
                JsonObject body = context.getBodyAsJson();
                body.put(CREATED_BY, context.<Integer>get(USER_ID));

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Created");
                        }
                    } catch (Throwable t){
                        responseError(context, t);
                    }
                });
            } catch (Exception e){
                responseError(context, UNEXPECTED_ERROR, e.getMessage());
            }
        }
    }

    private void checkPromoCode(RoutingContext context){
        JsonObject body = context.getBodyAsJson();

        try {
            if (isValidCheckPromoCodeData(context, body)){

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_CHECK_PROMO_CODE), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, reply.result().body(), "Found");
                    } catch (Throwable t){
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });
            }
        } catch (PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void checkUserPromoCode(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        body.put(USER_ID, context.<Integer>get(USER_ID));

        try {
            if (isValidCheckPromoCodeData(context, body)){

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_CHECK_USER_PROMO_CODE), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, reply.result().body(), "Found");
                    } catch (Throwable t){
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });
            }
        } catch (PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void getList(RoutingContext context){
        if (this.isValidGetListData(context)){
            try {
                JsonObject body = context.getBodyAsJson();

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_GET_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, reply.result().body(), "Found");
                    } catch (Throwable t){
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });
            } catch (Exception e){
                responseError(context, e);
            }
        }
    }

    private void calculatePromoCode(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(USER_ID, context.<Integer>get(USER_ID));
            body.put(FLAG_USER_PROMO, context.<Boolean>get(FLAG_USER_PROMO));

            if (this.validateCalculatePromoCode(body)) {
                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_CALCULATE_PROMO_CODE), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, reply.result().body(), "Found");
                    } catch (Throwable t){
                        responseError(context, UNEXPECTED_ERROR, t.getMessage());
                    }
                });
            }
        } catch (PropertyValueException ex){
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void registerMassive(RoutingContext context){
        if (this.isValidCreateData(context)){
            try {
                JsonObject body = context.getBodyAsJson();
                body.put(CREATED_BY, context.<Integer>get(USER_ID));

                isGraterAndNotNull(body, PROMO_CODES_QUANTITY, 0);

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_REGISTER_MASSIVE), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Created");
                        }
                    } catch (Throwable t){
                        responseError(context, t);
                    }
                });
            } catch (PropertyValueException e){
                responsePropertyValue(context, e);
            } catch (Exception e){
                responseError(context, e.getMessage());
            }
        }
    }
    
    private void getGeneralByOrigin(RoutingContext context){
        try {

            HttpServerRequest request = context.request();
            Integer purchaseOrigin = Integer.parseInt(request.getParam(PURCHASE_ORIGIN));
            Integer customerId = Integer.parseInt(request.getParam(CUSTOMER_ID));

            JsonObject body = new JsonObject()
                    .put(PURCHASE_ORIGIN, purchaseOrigin)
                    .put(CUSTOMER_ID, customerId);

            UtilsValidation.isGraterAndNotNull(body, PURCHASE_ORIGIN, 0);
            UtilsValidation.isGraterAndNotNull(body, CUSTOMER_ID, 0);

            String fieldName = purchaseOrigin.equals(2) ? "promo_web" : "promo_app_cliente";
            JsonObject bodyGeneralPromo = new JsonObject().put("fieldName", fieldName);

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), bodyGeneralPromo,
                    new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyGS -> {
                try {
                    if (replyGS.failed()){
                        throw replyGS.cause();
                    }

                    JsonObject generalPromo = (JsonObject) replyGS.result().body();
                    Integer generalPromoId = Integer.parseInt(generalPromo.getString(VALUE));
                    body.put(GENERAL_PROMO_ID, generalPromoId);

                    vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_GET_GENERAL_BY_ORIGIN), (AsyncResult<Message<JsonObject>> reply) -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            responseOk(context, reply.result().body(), "Found");
                        } catch (Throwable t){
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });

                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (PropertyValueException e){
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void calculateMultiplePromo(RoutingContext context){
        try {

            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, _ORIGIN);
            isContainedAndNotNull(body, SERVICE, SERVICES.parcel.name(), SERVICES.parcel_inhouse.name());
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);
            isContainedAndNotNull(_SHIPMENT_TYPE, body, _SHIPMENT_TYPE, SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue(), SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue());
            isGrater(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            isBooleanAndNotNull(body, _PAYS_SENDER);
            isGraterAndNotNull(body, _SENDER_ID, 0);
            if(!body.getBoolean(_PAYS_SENDER)) {
                isGraterAndNotNull(body, _ADDRESSEE_ID, 0);
            } else {
                isGrater(body, _ADDRESSEE_ID, 0);
            }
            isGrater(body, _INVOICE_VALUE, 0.0);
            isEmptyAndNotNull(body.getJsonArray(_PARCEL_PACKAGES), _PARCEL_PACKAGES);
            JsonArray parcelPackages = body.getJsonArray(_PARCEL_PACKAGES);
            for (Object p : parcelPackages) {
                JsonObject parcelPackage = (JsonObject) p;
                isContainedAndNotNull(parcelPackage, _SHIPPING_TYPE, "parcel", "courier");
                isEmpty(parcelPackage, _CONTAINS);
                isGrater(parcelPackage, _PACKAGE_TYPE_ID, 0);
                isGraterAndNotNull(parcelPackage, _QUANTITY, 0);
                if (parcelPackage.getString(_SHIPPING_TYPE).equals("parcel")) {
                    isGraterAndNotNull(parcelPackage, _WEIGHT, 0.0);
                    isGraterAndNotNull(parcelPackage, _HEIGHT, 0.0);
                    isGraterAndNotNull(parcelPackage, _WIDTH, 0.0);
                    isGraterAndNotNull(parcelPackage, _LENGTH, 0.0);
                }
            }

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_CALCULATE_MULTIPLE_PROMO), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (PropertyValueException e){
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void getDetail(RoutingContext context){
        try {
            JsonObject body = new JsonObject()
                    .put(ID, Integer.parseInt(context.request().getParam(ID)));
            isGraterAndNotNull(body, ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_GET_DETAIL), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (PropertyValueException e){
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private void customerPromoReport(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            isGrater(body, USER_ID, 0);
            isGrater(body, BRANCHOFFICE_ID, -1);
            isBoolean(body, "without_plaza");
            isBoolean(body, "without_seller");

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_CUSTOMER_PROMO_REPORT), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (PropertyValueException e){
            UtilsResponse.responsePropertyValue(context, e);
        }
    }

    private boolean validateCalculatePromoCode(JsonObject body) throws PropertyValueException {
        JsonObject discount = body.getJsonObject(DISCOUNT);
        SERVICES service = SERVICES.valueOf(discount.getString(SERVICE));
        String rule = discount.getString(RULE);

        JsonObject bodyService = body.getJsonObject(SERVICE);
        if (bodyService == null){
            throw new PropertyValueException(SERVICE, INVALID_FORMAT);
        } else {
            isGraterAndNotNull(bodyService, ID, 0, SERVICE);
        }

        isEmptyAndNotNull(body.getJsonArray(PRODUCTS), PRODUCTS);
        JsonArray products = body.getJsonArray(PRODUCTS);
        for(int i = 0; i < products.size(); i++){
            JsonObject product = products.getJsonObject(i);
            if (service.equals(SERVICES.boardingpass)){
                isGraterEqualAndNotNull(product, AMOUNT, 0.0, PRODUCTS);
                isGraterEqualAndNotNull(product, DISCOUNT, 0.0, PRODUCTS);
                isGraterEqualAndNotNull(product, TOTAL_AMOUNT, 0.0, PRODUCTS);
                if (rule != null){
                    isEmptyAndNotNull(product, TICKET_TYPE_ROUTE, PRODUCTS);
                }
            }
            if (service.equals(SERVICES.parcel)){
                isEmptyAndNotNull(product, ParcelsPackagesDBV.SHIPPING_TYPE);
                String shippingType = product.getString(ParcelsPackagesDBV.SHIPPING_TYPE);
                if (!shippingType.equals(TYPES_PACKAGES.courier.name())){
                    isGraterAndNotNull(product, ParcelsPackagesDBV.WEIGHT, 0.0, PRODUCTS);
                    isGraterAndNotNull(product, ParcelsPackagesDBV.HEIGHT, 0.0, PRODUCTS);
                    isGraterAndNotNull(product, ParcelsPackagesDBV.WIDTH, 0.0, PRODUCTS);
                    isGraterAndNotNull(product, ParcelsPackagesDBV.LENGTH, 0.0, PRODUCTS);
                }
            }
        }
        return true;
    }

    public boolean isValidCheckPromoCodeData(RoutingContext context, JsonObject body) throws PropertyValueException{

        isContainedAndNotNull(body, SERVICE, SERVICES.boardingpass.name(), SERVICES.parcel.name(), SERVICES.rental.name(), SERVICES.parcel_inhouse.name());
        isEmptyAndNotNull(body, DISCOUNT_CODE);
        isEmptyAndNotNull(body, Constants.DATE);
        isGrater(body, CUSTOMER_ID, 0);
        isGrater(body, PURCHASE_ORIGIN, 0);
        isContained(body, RULE, RULES.boardingpass_abierto.name(), RULES.boardingpass_redondo.name(), RULES.boardingpass_abierto_sencillo.name(), RULES.boardingpass_sencillo.name());
        isContained(body, RULE_FOR_PACKAGES, RULES_FOR_PACKAGES.shipping.name());
        isContained(body, TYPE_PACKAGES, TYPES_PACKAGES.parcel.name(), TYPES_PACKAGES.courier.name(), TYPES_PACKAGES.all.name());

        String ARRAY_NAME_REFERENCE = "";
        try {

            JsonArray specialTicketIds = body.getJsonArray(SPECIAL_TICKET_ID, new JsonArray());
            if (specialTicketIds != null) {
                for(int i = 0; i < specialTicketIds.size(); i++){
                    ARRAY_NAME_REFERENCE = SPECIAL_TICKET_ID;
                    Integer specialTicketId = specialTicketIds.getInteger(i);
                }
            }

            JsonArray packagePriceNames = body.getJsonArray(PACKAGE_PRICE_NAME, new JsonArray());
            if (packagePriceNames != null) {
                for(int i = 0; i < packagePriceNames.size(); i++){
                    ARRAY_NAME_REFERENCE = PACKAGE_PRICE_NAME;
                    String packagePriceName = packagePriceNames.getString(i);
                }
            }

            JsonArray packagePriceDistanceIds = body.getJsonArray(PACKAGE_PRICE_DISTANCE_ID, new JsonArray());
            if (packagePriceDistanceIds != null) {
                for(int i = 0; i < packagePriceDistanceIds.size(); i++){
                    ARRAY_NAME_REFERENCE = PACKAGE_PRICE_DISTANCE_ID;
                    Integer packagePriceDistanceId = packagePriceDistanceIds.getInteger(i);
                }
            }
        } catch (Exception e){
            responseWarning(context, new PropertyError(ARRAY_NAME_REFERENCE.concat(" element"), INVALID_FORMAT));
        }
        return true;
    }

    private boolean isValidGetListData(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();

        if (body.containsKey(DISCOUNT_TYPE)){
            try {
                DISCOUNT_TYPES.fromValue(body.getString(DISCOUNT_TYPE));
            } catch (Exception e){
                errors.add(new PropertyError(DISCOUNT_TYPE, e.getMessage()));
            }
        }

        if (body.containsKey(APPLY_TO_SPECIAL_TICKETS)){
            try {
                JsonArray tickets = (JsonArray) body.getValue(APPLY_TO_SPECIAL_TICKETS, new JsonArray());
                if (tickets == null){
                    errors.add(new PropertyError(APPLY_TO_SPECIAL_TICKETS, INVALID_FORMAT));
                }
                if (!tickets.isEmpty()){
                    tickets.forEach(t -> {
                        try {
                            Integer ticket = (Integer) t;
                        } catch (Exception e){
                            errors.add(new PropertyError(APPLY_TO_SPECIAL_TICKETS.concat(" element"), INVALID_FORMAT));
                        }
                    });
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_TO_SPECIAL_TICKETS, e.getMessage()));
            }
        }

        if (body.containsKey(APPLY_TO_PACKAGE_PRICE)){
            try {
                JsonArray packagesPrice = (JsonArray) body.getValue(APPLY_TO_PACKAGE_PRICE, new JsonArray());
                if (packagesPrice == null){
                    errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE, INVALID_FORMAT));
                }
                if (!packagesPrice.isEmpty()){
                    packagesPrice.forEach(p -> {
                        try {
                            String packagePriceName = (String) p;
                        } catch (Exception e){
                            errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE.concat(" element"), INVALID_FORMAT));
                        }
                    });
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE, e.getMessage()));
            }
        }

        if (body.containsKey(APPLY_TO_PACKAGE_PRICE_DISTANCE)){
            try {
                JsonArray packagesPricesDistance = (JsonArray) body.getValue(APPLY_TO_PACKAGE_PRICE_DISTANCE, new JsonArray());
                if (packagesPricesDistance == null){
                    errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE_DISTANCE, INVALID_FORMAT));
                }
                if (!packagesPricesDistance.isEmpty()){
                    packagesPricesDistance.forEach(p -> {
                        try {
                            Integer packagePriceDistanceId = (Integer) p;
                        } catch (Exception e){
                            errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE_DISTANCE.concat(" element"), INVALID_FORMAT));
                        }
                    });
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE_DISTANCE, e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return true;
    }

    /**
     * Verifies is the data of the request is valid to create a record of this entity
     *
     * @param context context of the request
     * @return true if the data is valid, false othrewise
     */
    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        SERVICES service = null;

        if (!body.containsKey(DISCOUNT_CODE)){
            errors.add(new PropertyError(DISCOUNT_CODE, MISSING_REQUIRED_VALUE));
        } else {
            try {
                String discountCode = body.getString(DISCOUNT_CODE);
                if (discountCode == null){
                    errors.add(new PropertyError(DISCOUNT_CODE, INVALID_FORMAT));
                }
            } catch (Exception e){
                errors.add(new PropertyError(DISCOUNT_CODE, e.getMessage()));
            }
        }

        if (!body.containsKey(NAME)){
            errors.add(new PropertyError(NAME, MISSING_REQUIRED_VALUE));
        } else {
            try {
                String name = body.getString(NAME);
                if (name == null){
                    errors.add(new PropertyError(NAME, INVALID_FORMAT));
                }
            } catch (Exception e){
                errors.add(new PropertyError(NAME, e.getMessage()));
            }
        }

        if (!body.containsKey(DESCRIPTION)){
            errors.add(new PropertyError(DESCRIPTION, MISSING_REQUIRED_VALUE));
        } else {
            try {
                String description = body.getString(DESCRIPTION);
                if (description == null){
                    errors.add(new PropertyError(DESCRIPTION, INVALID_FORMAT));
                }
            } catch (Exception e){
                errors.add(new PropertyError(DESCRIPTION, e.getMessage()));
            }
        }

        if (!body.containsKey(SERVICE)){
            errors.add(new PropertyError(SERVICE, MISSING_REQUIRED_VALUE));
        } else {
            try {
                service = SERVICES.valueOf(body.getString(SERVICE));
                if (service.equals(SERVICES.parcel) || service.equals(SERVICES.guiapp)){
                    body.put(RULE_FOR_PACKAGES, RULES_FOR_PACKAGES.shipping.name());
                    context.setBody(body.toBuffer());
                    if (!body.containsKey(APPLY_TO_PACKAGE_PRICE)){
                        errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE, MISSING_REQUIRED_VALUE));
                    }
                    if (!body.containsKey(APPLY_TO_PACKAGE_PRICE_DISTANCE)){
                        errors.add(new PropertyError(APPLY_TO_PACKAGE_PRICE_DISTANCE, MISSING_REQUIRED_VALUE));
                    }
                }
            } catch (Exception e){
                errors.add(new PropertyError(SERVICE, INVALID_FORMAT));
            }
        }

        if (!body.containsKey(DISCOUNT_TYPE)){
            errors.add(new PropertyError(DISCOUNT_TYPE, MISSING_REQUIRED_VALUE));
        } else {
            try {
                DISCOUNT_TYPES discountType = DISCOUNT_TYPES.fromValue(body.getString(DISCOUNT_TYPE));
                boolean isDiscountNProduct = discountType.equals(DISCOUNT_TYPES.discount_n_product);

                if (discountType.equals(DISCOUNT_TYPES.free_n_product)){
                    body.put(DISCOUNT, 100.00);
                    context.setBody(body.toBuffer());
                } else if(discountType.equals(DISCOUNT_TYPES.direct_percent) || isDiscountNProduct){
                    if (!body.containsKey(DISCOUNT)){
                        errors.add(new PropertyError(DISCOUNT, MISSING_REQUIRED_VALUE));
                    } else {
                        try {
                            Double discount = body.getDouble(DISCOUNT);
                            if (discount == null){
                                errors.add(new PropertyError(DISCOUNT, INVALID_PARAMETER));
                            } else if (discount <= 0 || discount > 100){
                                errors.add(new PropertyError(DISCOUNT, "Value must be greater than 0 and less than or equal to 100"));
                            }
                        } catch (Exception e) {
                            errors.add(new PropertyError(DISCOUNT, e.getMessage()));
                        }
                    }
                }
                if (discountType.equals(DISCOUNT_TYPES.direct_amount) || discountType.equals(DISCOUNT_TYPES.as_price)){
                    try {
                        if (discountType.equals(DISCOUNT_TYPES.as_price)){
                            if (body.containsKey(CUSTOMERS)){
                                try {
                                    JsonArray customers = body.getJsonArray(CUSTOMERS);
                                    if (customers == null || customers.isEmpty()) {
                                        errors.add(new PropertyError(CUSTOMERS, "Discount type only apply to customers"));
                                    }
                                } catch (Throwable t){
                                    errors.add(new PropertyError(CUSTOMERS, INVALID_FORMAT));
                                }
                            }
                        }
                        Double discount = body.getDouble(DISCOUNT);
                        if (discount == null){
                            errors.add(new PropertyError(DISCOUNT, INVALID_PARAMETER));
                        } else if (discount <= 0){
                            errors.add(new PropertyError(DISCOUNT, "Value must be greater than 0"));
                        }
                    } catch (Exception e) {
                        errors.add(new PropertyError(DISCOUNT, e.getMessage()));
                    }
                }
            } catch (Exception e){
                errors.add(new PropertyError(SERVICE, INVALID_FORMAT));
            }
        }

        if (body.containsKey(TYPE_PACKAGES)){
            try {
                TYPES_PACKAGES typePackages = TYPES_PACKAGES.valueOf(body.getString(TYPE_PACKAGES));
                if (!typePackages.equals(TYPES_PACKAGES.parcel) &&
                    !typePackages.equals(TYPES_PACKAGES.courier) &&
                    !typePackages.equals(TYPES_PACKAGES.all)){
                    errors.add(new PropertyError(TYPE_PACKAGES, INVALID_PARAMETER));
                }
            } catch (Exception e) {
                errors.add(new PropertyError(TYPE_PACKAGES, INVALID_FORMAT));
            }
        }

        if (!body.containsKey(SINCE)){
            errors.add(new PropertyError(SINCE, MISSING_REQUIRED_VALUE));
        } else {
            try {
                String since = body.getString(SINCE);
                try {
                    UtilsDate.parse_yyyy_MM_dd(since);
                } catch (ParseException e) {
                    errors.add(new PropertyError(SINCE, e.getMessage()));
                }
            } catch (Exception e) {
                errors.add(new PropertyError(SINCE, INVALID_FORMAT));
            }
        }

        if (!body.containsKey(UNTIL)){
            errors.add(new PropertyError(UNTIL, MISSING_REQUIRED_VALUE));
        } else {
            try {
                String since = body.getString(SINCE);
                String until = body.getString(UNTIL);
                try {
                    Calendar date = Calendar.getInstance();
                    if(UtilsDate.isLowerThan(UtilsDate.parse_yyyy_MM_dd(until), UtilsDate.parse_yyyy_MM_dd(UtilsDate.format_yyyy_MM_dd(date.getTime())))){
                        errors.add(new PropertyError(UNTIL, "Date must be greater than or equal to today's date"));
                    }
                    if (UtilsDate.isLowerThan(UtilsDate.parse_yyyy_MM_dd(until), UtilsDate.parse_yyyy_MM_dd(since))){
                        errors.add(new PropertyError(UNTIL, "Date must be greater than or equal to the start date"));
                    }
                } catch (ParseException e) {
                    errors.add(new PropertyError(UNTIL, e.getMessage()));
                }
            } catch (Exception e){
                errors.add(new PropertyError(SINCE + " or " + UNTIL, INVALID_FORMAT));
            }
        }

        if (body.containsKey(USAGE_LIMIT)){
            try {
                Integer usageLimit = body.getInteger(USAGE_LIMIT);
                if (usageLimit < 0){
                    errors.add(new PropertyError(USAGE_LIMIT, "Value must be greater than or equal to 0"));
                }
            } catch (Exception e){
                errors.add(new PropertyError(USAGE_LIMIT, e.getMessage()));
            }
        }

        if (body.containsKey(USED)){
            errors.add(new PropertyError(USED, INVALID_PARAMETER));
        }

        if (body.containsKey(CUSTOMERS)){
            /*try {
                if (body.containsKey(HAS_SPECIFIC_CUSTOMER) && body.getBoolean(HAS_SPECIFIC_CUSTOMER)){*/
                    try {
                        JsonArray customers = body.getJsonArray(CUSTOMERS);
                        if (customers == null){
                            errors.add(new PropertyError(CUSTOMERS, INVALID_FORMAT));
                        }
                        customers.forEach(c -> {
                            try {
                                JsonObject cu = (JsonObject) c;
                                try {
                                    Integer cid = cu.getInteger(CUSTOMER_ID);
                                    if (cid <= 0) {
                                        errors.add(new PropertyError(CUSTOMER_ID, "Value must be greater than 0"));
                                    }
                                } catch (Exception e){
                                    errors.add(new PropertyError(CUSTOMER_ID, e.getMessage()));
                                }
                                if(cu.containsKey(USAGE_LIMIT)){
                                    try {
                                        Integer lim = cu.getInteger(USAGE_LIMIT);
                                        if (lim < 0) {
                                            errors.add(new PropertyError(USAGE_LIMIT, "Value must be greater than or equal to 0"));
                                        }
                                    } catch (Exception e){
                                        errors.add(new PropertyError(USAGE_LIMIT, e.getMessage()));
                                    }
                                }
                            } catch (Exception e){
                                errors.add(new PropertyError(CUSTOMERS, e.getMessage()));
                            }
                        });
                    } catch (Exception e){
                        errors.add(new PropertyError(CUSTOMERS, e.getMessage()));
                    }
                /*} else {
                    errors.add(new PropertyError(CUSTOMERS, "Discount code does not apply to customers"));
                }
            } catch (Exception e){
                errors.add(new PropertyError(HAS_SPECIFIC_CUSTOMER, e.getMessage()));
            }*/
        }

        if(body.containsKey(APPLY_ONLY_FIRST_PURCHASE)){
            try {
                Boolean applyOnlyFirstPurchase = body.getBoolean(APPLY_ONLY_FIRST_PURCHASE, false);
                if (applyOnlyFirstPurchase){
                    if (!service.equals(SERVICES.boardingpass)){
                        errors.add(new PropertyError(APPLY_ONLY_FIRST_PURCHASE, "Setting only works with " + SERVICES.boardingpass.name() + " service"));
                    }
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_ONLY_FIRST_PURCHASE, e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidCreateData(context);
    }

}
