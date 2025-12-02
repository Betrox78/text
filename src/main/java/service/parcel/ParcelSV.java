package service.parcel;

import database.branchoffices.BranchofficeDBV;
import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.parcel.ParcelDBV;
import database.parcel.ParcelsPackagesDBV;
import database.parcel.enums.SHIPMENT_TYPE;
import database.promos.enums.SERVICES;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import service.commons.ServiceVerticle;
import service.commons.middlewares.*;
import utils.UtilsID;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static database.parcel.ParcelDBV.*;
import static database.promos.PromosDBV.*;
import static service.commons.Constants.*;
import static service.commons.Constants.CUSTOMER_ID;
import static service.commons.Constants.DATE;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

public class ParcelSV extends ServiceVerticle {
    private static final Set<String> ALLOWD_HEADERS = new HashSet<>();
    private static final Set<HttpMethod> ALLOWED_METHODS = new HashSet<>();

    static {
        ALLOWD_HEADERS.add("x-requested-with");
        ALLOWD_HEADERS.add("Access-Control-Allow-Origin");
        ALLOWD_HEADERS.add("origin");
        ALLOWD_HEADERS.add("Content-Type");
        ALLOWD_HEADERS.add("accept");
        ALLOWD_HEADERS.add("X-PINGARUNER");
        ALLOWD_HEADERS.add("authorization");
        ALLOWD_HEADERS.add("baggage");
        ALLOWD_HEADERS.add("sentry-trace");

        ALLOWED_METHODS.add(HttpMethod.GET);
        ALLOWED_METHODS.add(HttpMethod.POST);
        ALLOWED_METHODS.add(HttpMethod.OPTIONS);
        ALLOWED_METHODS.add(HttpMethod.DELETE);
        ALLOWED_METHODS.add(HttpMethod.PATCH);
        ALLOWED_METHODS.add(HttpMethod.PUT);
    }

    @Override
    protected String getDBAddress() {
        return ParcelDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcels";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/advancedSearch", AuthMiddleware.getInstance(), this::advancedSearch);
        this.addHandler(HttpMethod.POST, "/stockReport", AuthMiddleware.getInstance(), this::stockReport);
        this.addHandler(HttpMethod.POST, "/cancel", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx), this::cancel);
        this.addHandler(HttpMethod.PUT, "/deliver", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), CashOutMiddleware.getInstance(vertx), this::deliver);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx), PromosMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), this::register);
        this.addHandler(HttpMethod.POST, "/register/inhouse", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx), PromosMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), this::registerInHouse);
        this.addHandler(HttpMethod.POST, "/scanningPackagesReport", AuthMiddleware.getInstance(), this::scanningPackagesReport);
        this.addHandler(HttpMethod.POST, "/arrivalContingency", AuthMiddleware.getInstance(), this::arrivalContingency);
        this.addHandler(HttpMethod.POST, "/unregisteredPackageReport", AuthMiddleware.getInstance(), this::unregisteredPackageReport);
        this.addHandler(HttpMethod.GET, "/trackingCode/:parcelTrackingCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getParcelByTrackingCode);
        this.addHandler(HttpMethod.POST, "/transitPackageReport", AuthMiddleware.getInstance(), this::transitPackageReport);
        this.addHandler(HttpMethod.POST, "/salesReport", AuthMiddleware.getInstance(), this::salesReportInfo);
        this.addHandler(HttpMethod.POST, "/salesReport/totals", AuthMiddleware.getInstance(), this::salesReportTotals);
        this.addHandler(HttpMethod.POST, "/salesReport/transhipment", AuthMiddleware.getInstance(), this::salesReportInfoTranshipment);
        this.addHandler(HttpMethod.POST, "/salesReport/totals/transhipment", AuthMiddleware.getInstance(), this::salesReportTotalsTranshipment);
        this.addHandler(HttpMethod.POST, "/salesReport/webservice", AuthMiddleware.getInstance(), this::salesReportInfoWebService);
        this.addHandler(HttpMethod.POST, "/salesReport/totals/webservice", AuthMiddleware.getInstance(), this::salesReportTotalsWebService);
        this.addHandler(HttpMethod.POST, "/transitPackageReport/totals", AuthMiddleware.getInstance(), this::transitTotalsPackageReport);
        this.addHandler(HttpMethod.POST, "/salesReportIngresos", AuthMiddleware.getInstance(), this::salesReport_FechaIngresos); //movdev
        this.addHandler(HttpMethod.POST, "/salesMonth", AuthMiddleware.getInstance(), this::salesMonth); //webdev
        this.addHandler(HttpMethod.POST, "/contingencyReport", AuthMiddleware.getInstance(), this::contingencyReport);
        this.addHandler(HttpMethod.POST, "/cancelParcelReport", AuthMiddleware.getInstance(), this::cancelReportInfo);
        this.addHandler(HttpMethod.POST, "/cancelParcelReport/totals", AuthMiddleware.getInstance(), this::cancelReportTotals);
        this.addHandler(HttpMethod.POST, "/report/commercialPromise", AuthMiddleware.getInstance(), this::commercialPromiseReport);
        this.addHandler(HttpMethod.POST, "/report/timelyDeliveryDetails", AuthMiddleware.getInstance(), this::timelyDeliveryDetailsReport);
        this.addHandler(HttpMethod.POST, "/report/commercialPromise/globalDemeanor", AuthMiddleware.getInstance(), this::globalDemeanorCommercialPromiseReport);
        this.addHandler(HttpMethod.POST, "/salesReportGeneral", AuthMiddleware.getInstance(), this::salesReportGeneral);
        this.addHandler(HttpMethod.POST, "/salesReportCobranza", AuthMiddleware.getInstance(), this::salesCobranzaGeneral);
        this.addHandler(HttpMethod.POST, "/report/customerByDate/", AuthMiddleware.getInstance(), this::createReportCustomer);
        this.addHandler(HttpMethod.POST, "/registerPostalCode", AuthMiddleware.getInstance(), InternalCustomerMiddleware.getInstance(vertx), this::registerPostalCode);
        this.addHandler(HttpMethod.POST, "/searchValidCodes", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::searchValidCodes);
        this.addHandler(HttpMethod.GET, "/branchofficesWParcelCoverage", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::branchofficesWParcelCoverage);
        this.addHandler(HttpMethod.POST, "/searchValidCodes/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::searchValidCodesV2);
        this.addHandler(HttpMethod.GET, "/zipCodeIsOnCoverage/:zip_code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::zipCodeIsOnCoverage);
        this.addHandler(HttpMethod.GET, "/generatePDF/:parcel_prepaid_id", AuthMiddleware.getInstance(), this::generatePDF);
        this.addHandler(HttpMethod.GET, "/generatePDFPrepaidSite/:customer_id", AuthMiddleware.getInstance(), this::generatePDFPrepaidSite);
        this.addHandler(HttpMethod.PUT, "/updatePostalCode", AuthMiddleware.getInstance(), this::updatePostalCode);
        this.addHandler(HttpMethod.POST, "/accumulatedParcelByAdviserAdviser", AuthMiddleware.getInstance(), this::reportAccumulatedParcelByAdviser);
        this.addHandler(HttpMethod.POST, "/accumulatedParcelByAdviserDetail", AuthMiddleware.getInstance(), this::reportAccumulatedParcelByAdviserDetail);
        this.addHandler(HttpMethod.POST, "/register/v2", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), ProfileMiddleware.getInstance(vertx), this::registerV2);
        this.addHandler(HttpMethod.POST, "/register/partner/v2", AuthMiddleware.getInstance(), ProfileMiddleware.getInstance(vertx), /*CreditMiddleware.getInstance(vertx),*/ this::registerPartnerV2);
        this.addHandler(HttpMethod.POST, "/exchange", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), ProfileMiddleware.getInstance(vertx), this::exchange);
        this.addHandler(HttpMethod.POST, "/getPromiseDeliveryDate", AuthMiddleware.getInstance(), this::getPromiseDeliveryDate);
        this.addHandler(HttpMethod.GET, "/getPendingCollect/:terminal_origin_id", AuthMiddleware.getInstance(), this::getPendingCollect);
        this.addHandler(HttpMethod.POST, "/toCollecting", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::toCollecting);
        this.addHandler(HttpMethod.POST, "/toCollected", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::toCollected);
        this.addHandler(HttpMethod.POST, "/toDocumented", AuthMiddleware.getInstance(), EmployeeMiddleware.getInstance(vertx), this::toDocumented);
        this.addHandler(HttpMethod.GET, "/getExtendedTracking/:parcel_tracking_code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getExtendedTracking);
        this.addHandler(HttpMethod.POST, "/originArrivalContingency", AuthMiddleware.getInstance(), this::originArrivalContingency);
        this.addHandler(HttpMethod.POST, "/exchange/v2", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), ProfileMiddleware.getInstance(vertx), this::exchangeV2);
        this.addHandler(HttpMethod.POST, "/register/v3", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), ProfileMiddleware.getInstance(vertx), this::registerV3);
        this.addHandler(HttpMethod.POST, "/exchange/v3", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx), CreditMiddleware.getInstance(vertx), ProfileMiddleware.getInstance(vertx), this::exchangeV3);
        this.addHandler(HttpMethod.POST, "/report/signatureDelivery", AuthMiddleware.getInstance(), this::signatureDeliveryReport);
        this.addHandler(HttpMethod.POST, "/report/signatureDelivery/detail", AuthMiddleware.getInstance(), this::signatureDeliveryReportDetail);
        super.start(startFuture);
        this.router.put("/print/packages").handler(BodyHandler.create());
        this.router.put("/print/packages").handler(this::printParcelPackages);
        this.router.put("/print/onePackage").handler(BodyHandler.create());
        this.router.put("/print/onePackage").handler(this::printParcelPackage);
        this.router.put("/print").handler(BodyHandler.create());
        this.router.put("/print").handler(this::printParcel);
        this.router.route().handler(CorsHandler.create("*").allowedHeaders(ALLOWD_HEADERS).allowedMethods(ALLOWED_METHODS));
        this.addHandler(HttpMethod.GET, "/get/letterporte/complement/:id/:date_init/:date_end", AuthMiddleware.getInstance(), this::getLetterPorteComplement);
    }

    private void register(RoutingContext context) {
        context.put(SERVICE, SERVICES.parcel);
        this.registerParcel(context);
    }

    private void registerInHouse(RoutingContext context) {
        context.put(SERVICE, SERVICES.parcel_inhouse);
        this.registerParcel(context);
    }

    private void registerParcel(RoutingContext context) {
        try {
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            body.remove("cartaporte");
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, REGISTER);
            body
                    .put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                    .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID))
                    .put(INTERNAL_CUSTOMER, context.<Boolean>get(INTERNAL_CUSTOMER))
                    .put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO))
                    .put(FLAG_USER_PROMO, context.<Boolean>get(FLAG_USER_PROMO))
                    .put(SERVICE, context.<SERVICES>get(SERVICE));

            Integer cancelTicketId = null;
            if (body.containsKey("reissue")) {
                if (body.containsKey("cancel_ticket_id")) {
                    cancelTicketId = body.getInteger("cancel_ticket_id");
                    body.remove("cancel_ticket_id");
                }
            }

            Boolean isCreditParcel = Boolean.FALSE;
            if (body.containsKey("is_credit")) {
                isCreditParcel = body.getBoolean("is_credit");
            }

            JsonObject branchOrigin = new JsonObject().put("id", body.getInteger("terminal_origin_id"));
            JsonObject branchDestiny = new JsonObject().put("id", body.getInteger("terminal_destiny_id"));
            Boolean paysSender = null;
            try {
                paysSender = body.getBoolean("pays_sender");
            } catch (Exception e) {
                paysSender = body.getInteger("pays_sender").equals(1);
            }
            body.put("pays_sender", paysSender);

            JsonObject customerId = new JsonObject().put(CUSTOMER_ID, body.getInteger(CUSTOMER_ID));

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();
            Future<Message<JsonObject>> f5 = Future.future();
            Future<Message<JsonObject>> f6 = Future.future();
            if (branchOrigin.getInteger("id") != null && branchDestiny.getInteger("id") != null) {
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchOrigin, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f1.completer());
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchDestiny, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f2.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "parcel_iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());
                eventBus.send(CustomerDBV.class.getSimpleName(), customerId, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f6.completer());

                Boolean finalIsCreditParcel = isCreditParcel;
                CompositeFuture.all(f1, f2, f3, f4, f5, f6).setHandler(detailReply -> {
                    try {
                        if (detailReply.failed()) {
                            throw new Exception(detailReply.cause());
                        }
                        Message<JsonObject> branch1Status = detailReply.result().resultAt(0);
                        Message<JsonObject> branch2Status = detailReply.result().resultAt(1);
                        Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(2);
                        Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(3);
                        Message<JsonObject> parcelIvaMsg = detailReply.result().resultAt(4);
                        Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(5);

                        JsonObject ivaPercent = ivaPercentMsg.body();
                        JsonObject currencyId = currencyIdMsg.body();
                        JsonObject parcelIva = parcelIvaMsg.body();
                        JsonObject customerCreditData = customerCreditDataMsg.body();

                        body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                        body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                        body.put("parcel_iva", Double.valueOf(parcelIva.getString("value")));
                        if(body.containsKey("isGuiappCanje")){
                            if(body.getBoolean("isGuiappCanje"))
                                //body.put("parcel_tracking_code", UtilsID.generateGuiaPpID("GPP"));
                                body.put("parcel_tracking_code",(String) body.remove("tracking_code"));
                                else {
                                body.put("parcel_tracking_code", UtilsID.generateGuiaPpID("G"));
                            }
                        } else {
                            body.put("parcel_tracking_code", UtilsID.generateID("G"));
                        }
                        body.put("payment_condition", finalIsCreditParcel ? "credit" : "cash");
                        body.put("customer_credit_data", customerCreditData);

                        try {
                            isGraterAndNotNull(body, "customer_id", 0);
                            isGraterAndNotNull(body, "sender_address_id", 0);
                            isGraterAndNotNull(body, "addressee_address_id", 0);
                            differentValues(body, "terminal_origin_id", "terminal_destiny_id");
                            isStatusActive(branch1Status.body(), "status");
                            isStatusActive(branch2Status.body(), "status");
                            isGrater(body, _INVOICE_VALUE, 0.0);

                            JsonArray parcelsPackages = body.getJsonArray("parcel_packages");
                            //Integer scheduleRouteDestinationId = body.getInteger("schedule_route_destination_id");

                            body.put("parcel_packages", parcelsPackages);

                            if (finalIsCreditParcel && body.getBoolean("pays_sender")) {
                                Double parcelAvailableCredit = customerCreditData.getDouble("available_credit");
                                Boolean parcelHasCredit = customerCreditData.getBoolean("has_credit");
                                if (parcelAvailableCredit == null) parcelAvailableCredit = 0.0;
                                Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                                body.put("debt", parcelPaymentsAmount);

                                if (!parcelHasCredit) {
                                    throw new Exception("Customer: no credit available");
                                }
                                if (parcelAvailableCredit < parcelPaymentsAmount) {
                                    throw new Exception("Customer: Insufficient funds to apply credit");
                                }
                                if (!customerCreditData.getString("services_apply_credit").contains("parcel"))
                                    throw new Exception("Customer: service not applicable");
                            }
                            eventBus.send(this.getDBAddress(), body, options, reply -> {
                                try {
                                    if (reply.failed()) {
                                        throw reply.cause();
                                    }

                                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                                    } else {
                                        responseOk(context, reply.result().body(), "Created");
                                    }

                                } catch (Throwable t) {
                                    responseError(context, UNEXPECTED_ERROR, t);
                                }
                            });
                        } catch (UtilsValidation.PropertyValueException ex) {
                            UtilsResponse.responsePropertyValue(context, ex);
                        }

                    } catch (Exception e) {
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } else {
                responseError(context, "Se deben especificar las sucursales tanto de origen como de destino.", "You have to specify origin branch an destiny branch");
            }
        } catch (Exception e) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void registerV2(RoutingContext context) {
        context.put(ACTION, REGISTER_V2);
        try {
            JsonObject body = context.getBodyAsJson();
            isContainedAndNotNull(_SHIPMENT_TYPE, body, _SHIPMENT_TYPE, SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue(), SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue());
            isGrater(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            this.registerParcelV2(context);
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void registerV3(RoutingContext context) {
        context.put(ACTION, ACTION_REGISTER_V3);
        try {
            JsonObject body = context.getBodyAsJson();
            isContainedAndNotNull(_SHIPMENT_TYPE, body, _SHIPMENT_TYPE, SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue(), SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue());
            isGrater(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            this.registerParcelV2(context);
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void registerPartnerV2(RoutingContext context) {
        context.put(ACTION, REGISTER_PARTNER_V2);
        this.registerParcelV2(context);
    }

    private void exchange(RoutingContext context) {
        context.put(ACTION, EXCHANGE);
        context.setBody(context.getBodyAsJson().put("is_exchange", true).toBuffer());
        this.registerParcelV2(context);
    }

    private void exchangeV2(RoutingContext context) {
        context.put(ACTION, ACTION_EXCHANGE_V2);
        context.setBody(context.getBodyAsJson().put("is_exchange", true).toBuffer());
        try {
            JsonObject body = context.getBodyAsJson();
            JsonArray parcelPackages = body.getJsonArray(_PARCEL_PACKAGES);
            isEmptyAndNotNull(parcelPackages, _PARCEL_PACKAGES, _PARCEL_PACKAGES);


            this.registerParcelV2(context);
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }

    private void exchangeV3(RoutingContext context) {
        context.put(ACTION, ACTION_EXCHANGE_V3);
        context.setBody(context.getBodyAsJson().put("is_exchange", true).toBuffer());
        try {
            JsonObject body = context.getBodyAsJson();
            JsonArray parcelPackages = body.getJsonArray(_PARCEL_PACKAGES);
            isEmptyAndNotNull(parcelPackages, _PARCEL_PACKAGES, _PARCEL_PACKAGES);


            this.registerParcelV2(context);
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }

    private void registerParcelV2(RoutingContext context) {
        try {
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            body.remove("cartaporte");
            String action = context.get(ACTION);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, action);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
//            JsonObject profile = context.get(_PROFILE);
//            boolean isExternalDocument = Objects.nonNull(profile) && profile.getInteger("is_external_document") == 1;
            boolean isPendingCollection = false;

            if (!REGISTER_PARTNER_V2.equals(action)) {
                body
                        .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                        .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID));
            }

//            if(Objects.nonNull(body.getString(_SHIPMENT_TYPE))) {
//                SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(body.getString(_SHIPMENT_TYPE));
//                boolean isWebServiceRad = REGISTER_PARTNER_V2.equals(action) && shipmentType.includeRAD();
//                boolean isExternalDocumentRad = isExternalDocument && shipmentType.includeRAD();
//                isPendingCollection = isWebServiceRad || isExternalDocumentRad;
//            }
            body.put(_IS_PENDING_COLLECTION, isPendingCollection);

            Integer cancelTicketId = null;
            if (body.containsKey("reissue")) {
                if (body.containsKey("cancel_ticket_id")) {
                    cancelTicketId = body.getInteger("cancel_ticket_id");
                    body.remove("cancel_ticket_id");
                }
            }

            Boolean isCreditParcel = Boolean.FALSE;
            if (body.containsKey("is_credit")) {
                isCreditParcel = body.getBoolean("is_credit");
            }

            JsonObject branchOrigin = new JsonObject().put("id", body.getInteger("terminal_origin_id"));
            JsonObject branchDestiny = new JsonObject().put("id", body.getInteger("terminal_destiny_id"));
            Boolean paysSender = null;
            try {
                paysSender = body.getBoolean("pays_sender");
            } catch (Exception e) {
                paysSender = body.getInteger("pays_sender").equals(1);
            }
            body.put("pays_sender", paysSender);

            JsonObject customerId = new JsonObject().put(CUSTOMER_ID, body.getInteger(CUSTOMER_ID));

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();
            Future<Message<JsonObject>> f5 = Future.future();
            Future<Message<JsonObject>> f6 = Future.future();
            if (branchOrigin.getInteger("id") != null && branchDestiny.getInteger("id") != null) {
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchOrigin, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f1.completer());
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchDestiny, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f2.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "parcel_iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());
                eventBus.send(CustomerDBV.class.getSimpleName(), customerId, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f6.completer());

                Boolean finalIsCreditParcel = isCreditParcel;
                CompositeFuture.all(f1, f2, f3, f4, f5, f6).setHandler(detailReply -> {
                    try {
                        if (detailReply.failed()) {
                            throw new Exception(detailReply.cause());
                        }
                        Message<JsonObject> branch1Status = detailReply.result().resultAt(0);
                        Message<JsonObject> branch2Status = detailReply.result().resultAt(1);
                        Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(2);
                        Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(3);
                        Message<JsonObject> parcelIvaMsg = detailReply.result().resultAt(4);
                        Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(5);

                        JsonObject ivaPercent = ivaPercentMsg.body();
                        JsonObject currencyId = currencyIdMsg.body();
                        JsonObject parcelIva = parcelIvaMsg.body();
                        JsonObject customerCreditData = customerCreditDataMsg.body();
                        JsonObject terminalDestiny = branch2Status.body();

                        if(Objects.nonNull(body.getString(_SHIPMENT_TYPE))) {
                            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(body.getString(_SHIPMENT_TYPE));
                            Boolean isVirtualDestiny = terminalDestiny.getBoolean(_IS_VIRTUAL);
                            if (shipmentType.includeOCU() && isVirtualDestiny) {
                                throw new Exception("Cannot send packages to virtual branch office");
                            }
                        }

                        body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                        body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                        body.put("parcel_iva", Double.valueOf(parcelIva.getString("value")));

                        boolean isExchange = body.getBoolean("is_exchange", false);
                        if(isExchange){
                            body.remove("is_exchange");
                            body.put("parcel_tracking_code",(String) body.remove("tracking_code"));
                        } else {
                            body.put("parcel_tracking_code", UtilsID.generateID("G"));
                        }
                        body.put("payment_condition", finalIsCreditParcel ? "credit" : "cash");
                        body.put("customer_credit_data", customerCreditData);

                        try {
                            isGraterAndNotNull(body, "customer_id", 0);
                            isGraterAndNotNull(body, "sender_address_id", 0);
                            isGraterAndNotNull(body, "addressee_address_id", 0);
                            differentValues(body, "terminal_origin_id", "terminal_destiny_id");
                            isStatusActive(branch1Status.body(), "status");
                            isStatusActive(terminalDestiny, "status");

//                            JsonArray parcelsPackages = body.getJsonArray("parcel_packages");
                            //Integer scheduleRouteDestinationId = body.getInteger("schedule_route_destination_id");

//                            body.put("parcel_packages", parcelsPackages);

                            if (finalIsCreditParcel && body.getBoolean("pays_sender") && !REGISTER_PARTNER_V2.equals(action)) {
                                Double parcelAvailableCredit = customerCreditData.getDouble("available_credit");
                                Boolean parcelHasCredit = customerCreditData.getBoolean("has_credit");
                                if (parcelAvailableCredit == null) parcelAvailableCredit = 0.0;
                                Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                                body.put("debt", parcelPaymentsAmount);

                                if (!parcelHasCredit) {
                                    throw new Exception("Customer: no credit available");
                                }
                                if (parcelAvailableCredit < parcelPaymentsAmount) {
                                    throw new Exception("Customer: Insufficient funds to apply credit");
                                }
                                if (!customerCreditData.getString("services_apply_credit").contains("parcel"))
                                    throw new Exception("Customer: service not applicable");
                            }
                            eventBus.send(this.getDBAddress(), body, options, reply -> {
                                try {
                                    if (reply.failed()) {
                                        throw reply.cause();
                                    }

                                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                                    } else {
                                        responseOk(context, reply.result().body(), "Created");
                                    }

                                } catch (Throwable t) {
                                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                                }
                            });
                        } catch (UtilsValidation.PropertyValueException ex) {
                            UtilsResponse.responsePropertyValue(context, ex);
                        }

                    } catch (Throwable t) {
                        exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                    }
                });
            } else {
                exceptionLogger(context, body,
                        new Throwable("Se deben especificar las sucursales tanto de origen como de destino."),
                        exceptionLogHandler ->
                                responseError(context, "Se deben especificar las sucursales tanto de origen como de destino.", "You have to specify origin branch an destiny branch"));
            }
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
        }
    }

    private void deliver(RoutingContext context) {
        try {
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, DELIVER);
            body.put(UPDATED_BY, context.<Integer>get(USER_ID))
                    .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                    .put(EMPLOYEE, context.<JsonObject>get(EMPLOYEE));
            isEmptyAndNotNull(body, "credential_type");
            isEmptyAndNotNull(body, "no_credential");
            JsonArray parcelsPackages = body.getJsonArray("parcel_packages");
            isEmptyAndNotNull(parcelsPackages, "parcels_packages");

            Boolean isCredit = body.getBoolean("is_credit", false);
            body.put("payment_condition", isCredit ? "credit" : "cash");

            JsonObject trackingCode = new JsonObject().put("parcelTrackingCode", body.getInteger("id"));

            JsonObject creditParam = new JsonObject();
            if (body.containsKey(CUSTOMER_ID) && body.getInteger(CUSTOMER_ID) != null) {
                creditParam.put(CUSTOMER_ID, body.getInteger(CUSTOMER_ID));
            }

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();

            eventBus.send(this.getDBAddress(), trackingCode, new DeliveryOptions().addHeader(ACTION, CHECK_TRACKING_CODE), f1.completer());
            eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()) {
                        throw new Exception(detailReply.cause());
                    }
                    Message<JsonObject> parcelMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(2);

                    JsonObject parcel = parcelMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject currencyId = currencyIdMsg.body();

                    body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                    body.put("parcel", parcel);

                    eventBus.send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Found");
                            }
                        } catch (Exception e) {
                            exceptionLogger(context, body, e, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, e.getMessage()));
                        }
                    });
                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            exceptionLogger(context, null, ex, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, ex.getMessage()));
        }
    }

    private void salesReport_FechaIngresos(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_SALES_REPORT_FECHA_INGRESOS);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getParcelByTrackingCode(RoutingContext context) {
        int userId = context.get(USER_ID);
        JsonObject trackingCode = new JsonObject().put("parcelTrackingCode", context.request().getParam("parcelTrackingCode")).put("updated_by", userId);
        try {

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_PARCEL_BY_TRACKING_CODE);
            vertx.eventBus().send(this.getDBAddress(), trackingCode, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void cancel(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(UPDATED_BY, context.<Integer>get(USER_ID))
                .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID))
                .put(INTERNAL_CUSTOMER, context.<Boolean>get(INTERNAL_CUSTOMER))
                .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID));
        try {
            isGraterAndNotNull(body, PARCEL_ID, 0);
            isGraterAndNotNull(body, PARCELS_CANCEL_REASON_ID, 0);
            isBooleanAndNotNull(body, "apply_insurance");

            Future f1 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"),
                    options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());

            Future f2 = Future.future();
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    options(GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(f1, f2).setHandler(replyGS -> {
                try {
                    if (replyGS.failed()) {
                        throw new Exception(replyGS.cause());
                    }

                    Message<JsonObject> ivaPercentObj = replyGS.result().resultAt(0);
                    Message<JsonObject> currencyObj = replyGS.result().resultAt(1);

                    Double ivaPercent = Double.parseDouble(ivaPercentObj.body().getString("value"));
                    Integer currencyId = Integer.parseInt(currencyObj.body().getString("value"));

                    body.put("iva_percent", ivaPercent)
                            .put("currency_id", currencyId);

                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CANCEL_PARCEL_PACKAGE_BY_ID);
                    vertx.eventBus().send(ParcelDBV.class.getSimpleName(), body, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw new Exception(reply.cause());
                            }

                            responseOk(context, reply.result().body());

                        } catch (Exception e) {
                            e.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void printParcel(RoutingContext context) {
        // { id: 1, parcel_tracking_code: "G1222333333" }
        this.validateToken(context, userID -> {
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                responseError(context, new Throwable("Missing body"));
            } else {
                JsonObject trackingCode = new JsonObject().put("parcelTrackingCode", body.getString("parcel_tracking_code")).put("updated_by", userID)
                        .put("print", true);

                try {
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_PRINT);
                    vertx.eventBus().send(this.getDBAddress(), trackingCode, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            responseOk(context, reply.result().body());


                        } catch (Exception e) {
                            e.printStackTrace();
                            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                        }

                    });

                } catch (Exception ex) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex.getCause());
                }
            }

        });
    }

    private void printParcelPackages(RoutingContext context) {
        // { parcel_id: 1 }
        this.validateToken(context, userID -> {
            JsonObject body = context.getBodyAsJson();
            body.put("created_by", userID);
            if (body == null) {
                responseError(context, new Throwable("Missing body"));
            } else {
                Future<Message<JsonArray>> f1 = Future.future();
                Future<Message<JsonArray>> f2 = Future.future();

                vertx.eventBus().send(ParcelDBV.class.getSimpleName(), body, new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_PACKAGES_SUMMARY), f1.completer());
                vertx.eventBus().send(ParcelDBV.class.getSimpleName(), body, new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_PACKAGES), f2.completer());

                CompositeFuture.all(f1, f2).setHandler(detailReply -> {
                    try {
                        if (detailReply.failed()) {
                            throw new Exception(detailReply.cause());
                        }
                        Message<JsonArray> summaryInfo = detailReply.result().resultAt(0);
                        Message<JsonArray> parcelsInfo = detailReply.result().resultAt(1);

                        JsonArray summary = summaryInfo.body();
                        JsonArray packages = parcelsInfo.body();
                        for (int i = 0; i < packages.size(); i++) {
                            packages.getJsonObject(i).put("updated_by", userID);
                        }
                        body.put("packages", packages);
                        body.put("summary", summary);
                        doPrintParcelPackages(body)
                                .whenComplete((s, t) -> {
                                    if (t != null) {
                                        responseError(context, t.getCause().getMessage());
                                    } else {
                                        body.remove("created_by");
                                        body.remove("parcel_id");
                                        responseOk(context, body);
                                    }
                                });


                    } catch (Exception ex) {
                        ex.printStackTrace();
                        responseError(context, detailReply.cause().getMessage());
                    }
                });

            }
        });
    }

    private void printParcelPackage(RoutingContext context) {
        // { parcel_package_id: 1 }
        this.validateToken(context, userID -> {
            JsonObject body = context.getBodyAsJson();
            if (body == null) {
                responseError(context, new Throwable("Missing body"));
            } else {
                body.put("updated_by", userID);

                printPackage(body)
                        .whenComplete((s, t) -> {
                            if (t != null) {
                                responseError(context, t.getCause().getMessage());
                            } else {
                                responseOk(context, body);
                            }
                        });
            }

        });
    }

    private CompletableFuture<JsonArray> doPrintParcelPackages(JsonObject body) {
        JsonArray packages = body.getJsonArray("packages");
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(packages.stream()
                        .map(p -> printPackage((JsonObject) p))
                        .toArray(CompletableFuture[]::new))
                .whenComplete((s, t) -> {
                    if (t != null) {
                        future.completeExceptionally(t);
                    } else {
                        future.complete(packages);
                    }
                });

        return future;

    }

    private CompletableFuture<JsonObject> printPackage(JsonObject body) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.PRINT_PACKAGE);
        vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), body, options, (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }
                body.mergeIn(reply.result().body());

                future.complete(body);

            } catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });


        return future;
    }

    private void scanningPackagesReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.SCANNING_PACKAGES_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }


    private void advancedSearch(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isBooleanAndNotNull(body, "include_finished");
            isBooleanAndNotNull(body, "pays_sender");
            isEmptyAndNotNull(body, "sender_name");
            isEmptyAndNotNull(body, "sender_last_name");
            isEmptyAndNotNull(body, "addressee_name");
            isEmptyAndNotNull(body, "addressee_last_name");
            isGraterAndNotNull(body, "terminal_origin_id", 0);
            isGraterAndNotNull(body, "terminal_destiny_id", 0);
            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_ADVANCED_SEARCH);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {

                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                    }


                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void unregisteredPackageReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.UNREGISTERED_PACKAGES_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }

    }

    private void stockReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_STOCK_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());

                    } catch (Exception e) {
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());

                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void transitPackageReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");
            if (body.getInteger("page") != null) {
                isGraterAndNotNull(body, "page", 0);
                isGraterAndNotNull(body, "limit", 0);
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.TRANSIT_PACKAGES_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }

    }

    private void transitTotalsPackageReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.TRANSIT_PACKAGES_REPORT_TOTALS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }

    }

    //WEBDEV
    private void salesMonth(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_SALES_MONTH);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void cancelReportInfo(RoutingContext context) {
        this.cancelReport(context, false);
    }

    private void cancelReportTotals(RoutingContext context) {
        this.cancelReport(context, true);
    }

    private void cancelReport(RoutingContext context, boolean flagTotals) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");
            body.put("flag_totals", flagTotals);

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.CANCEL_PARCEL_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void salesReportInfo(RoutingContext context) {
        this.salesReport(context, false, false, false);
    }

    private void salesReportTotals(RoutingContext context) {
        this.salesReport(context, true, false, false);
    }

    private void salesReportInfoTranshipment(RoutingContext context) {
        this.salesReport(context, false, true, false);
    }

    private void salesReportTotalsTranshipment(RoutingContext context) {
        this.salesReport(context, true, true, false);
    }

    private void salesReportTotalsWebService(RoutingContext context) {
        this.salesReport(context, true, false, true);
    }

    private void salesReportInfoWebService(RoutingContext context) {
        this.salesReport(context, false, false, true);
    }

    private void salesReport(RoutingContext context, boolean flagTotals, boolean flagTranshipment, boolean flagWebService) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");
            body.put("flag_totals", flagTotals);
            if (!flagTotals && body.getInteger("page") != null) {
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);
            }
            body.put("flag_transhipment", flagTranshipment);
            body.put("flag_web_service", flagWebService);

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_SALES_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void contingencyReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");
            if (body.getInteger("page") != null) {
                isGraterAndNotNull(body, "page", 0);
                isGraterAndNotNull(body, "limit", 0);
            }
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.CONTINGENCY_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());
                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }


    private void arrivalContingency(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        try {
            isGraterAndNotNull(body, _TERMINAL_ID, 0);
            isGrater(body, _PARCEL_ID, 0);
            Integer parcelId = body.getInteger(_PARCEL_ID);
            if(Objects.isNull(parcelId)) {
                isEmptyAndNotNull(body.getJsonArray(_PACKAGES), _PACKAGES);
            }
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_ARRIVAL_CONTINGENCY);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }

                        responseOk(context, reply.result().body());
                    } catch (Throwable t) {
                        exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
                    }
                });
            } catch (Throwable t) {
                exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t.getMessage()));
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void commercialPromiseReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isGrater(body, _TERMINAL_ID, 0);
            if (Objects.nonNull(body.getJsonArray(_SHIPMENT_TYPE))) {
                JsonArray shipmentTypes = body.getJsonArray(_SHIPMENT_TYPE);
                for (Object shipmentType : shipmentTypes) {
                    JsonObject stBody = new JsonObject().put(_SHIPMENT_TYPE, String.valueOf(shipmentType));
                    isContained(stBody, _SHIPMENT_TYPE, "OCU", "RAD/OCU", "EAD", "RAD/EAD");
                }
            }

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_COMMERCIAL_PROMISE_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());

                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void timelyDeliveryDetailsReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isGrater(body, _TERMINAL_ID, 0);
            if (Objects.nonNull(body.getJsonArray(_SHIPMENT_TYPE))) {
                JsonArray shipmentTypes = body.getJsonArray(_SHIPMENT_TYPE);
                for (Object shipmentType : shipmentTypes) {
                    JsonObject stBody = new JsonObject().put(_SHIPMENT_TYPE, String.valueOf(shipmentType));
                    isContained(stBody, _SHIPMENT_TYPE, "OCU", "RAD/OCU", "EAD", "RAD/EAD");
                }
            }

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_TIMELY_DELIVERY_DETAILS_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());

                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void globalDemeanorCommercialPromiseReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isGrater(body, _TERMINAL_ID, 0);
            if (Objects.nonNull(body.getJsonArray(_SHIPMENT_TYPE))) {
                JsonArray shipmentTypes = body.getJsonArray(_SHIPMENT_TYPE);
                for (Object shipmentType : shipmentTypes) {
                    JsonObject stBody = new JsonObject().put(_SHIPMENT_TYPE, String.valueOf(shipmentType));
                    isContained(stBody, _SHIPMENT_TYPE, "OCU", "RAD/OCU", "EAD", "RAD/EAD");
                }
            }

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_GLOBAL_DEMEANOR_COMMERCIAL_PROMISE_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());

                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, ex.getMessage());
            }
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void salesReportGeneral(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_SALES_REPORT_GEN);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void salesCobranzaGeneral(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.PARCEL_COBRANZA_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void createReportCustomer(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_CREATE_REPORT_CUSTOMERS);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void registerPostalCode(RoutingContext context) {
        try {
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, REGISTER_POSTAL_CODE);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            body.put("status", 1);

            eventBus.send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        JsonObject result = (JsonObject) reply.result().body();
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e) {
            responseError(context, "Ocurriò un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }

    }

    private void searchValidCodes(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, GET_VALID_POSTAL_CODES);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body(), "OK");

                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }

    }

    private void branchofficesWParcelCoverage(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, GET_BRANCHOFFICES_W_PARCEL_COVERAGE);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body(), "OK");

                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }

    }

    private void searchValidCodesV2(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGrater(body, "zip_code", 0);
            isGrater(body, _BRANCHOFFICE_ID, 0);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, GET_VALID_POSTAL_CODES_V2);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body(), "OK");

                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }

    }

    private void zipCodeIsOnCoverage(RoutingContext context) {
        try {
            String zipCode = context.request().getParam(ZIP_CODE);
            System.out.println(zipCode);
            JsonObject body = new JsonObject().put(ZIP_CODE, zipCode);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ZIP_CODE_IS_ON_COVERAGE);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body(), "OK");

                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }

    }

    private void generatePDF(RoutingContext context) {
        try {
            String parcelPrepaidID = context.request().getParam("parcel_prepaid_id");
            JsonObject body = new JsonObject();
            if(context.request().getParam("start") != null) {
                body.put("start", context.request().getParam("start"));
            }
            if(context.request().getParam("end") != null) {
                body.put("end", context.request().getParam("end"));
            }
            body.put("parcel_prepaid_id", parcelPrepaidID);
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, GENERATE_PDF);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    byte[] data2 = (byte[]) reply.result().body();
                    sendPDFFile(data2, context, "OK");
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void generatePDFPrepaidSite(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject message = new JsonObject()
                    .put("customer_id", Integer.valueOf(request.getParam("customer_id")))
                    .put("to_print", Integer.valueOf(request.getParam("customer_id")));

            isGraterAndNotNull(message, "customer_id", 0);
            isGraterAndNotNull(message, "to_print", 0);
            Integer customerId = Integer.valueOf(request.getParam("customer_id"));
            Integer toPrint = Integer.valueOf(request.getParam("to_print"));
            Integer rangeId = Integer.valueOf(request.getParam("range_id"));
            Integer kmId = Integer.valueOf(request.getParam("km_id"));
            JsonObject body = new JsonObject()
                    .put("customer_id", customerId)
                    .put("to_print", toPrint)
                    .put("range_id", rangeId)
                    .put("km_id", kmId);

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, GENERATE_PDF_PREPAID_SITE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    byte[] data2 = (byte[]) reply.result().body();
                    sendPDFFile(data2, context, "OK");
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void updatePostalCode(RoutingContext context) {
        // Use a service account
        JsonObject body = context.getBodyAsJson();
        //  body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(ParcelDBV.class.getSimpleName(), body, options(ParcelDBV.ACTION_UPDATE_POSTAL_CODE), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void getLetterPorteComplement(RoutingContext context) {
        HttpServerRequest request = context.request();
        String id = request.getParam("id");
        String date_init = request.getParam("date_init");
        String date_end = request.getParam("date_end");


        JsonObject body = new JsonObject().put("terminal_origin_id", id)
                .put("date_init", date_init)
                .put("date_end", date_end);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_LETTER_PORTE_COMPLEMENT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    responseOk(context, reply.result().body());

                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void reportAccumulatedParcelByAdviser(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.ACCUMULATED_PARCEL_BY_ADVISER_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportAccumulatedParcelByAdviserDetail(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.ACCUMULATED_PARCEL_BY_ADVISER_REPORT_DETAIL);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    } catch (Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getPromiseDeliveryDate(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            isEmpty(body, DATE);
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);
            isContainedAndNotNull(body, _SHIPMENT_TYPE,
                    SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue(),
                    SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_GET_PROMISE_DELIVERY_DATE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    t.printStackTrace();
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            t.printStackTrace();
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void getPendingCollect(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put(_TERMINAL_ORIGIN_ID, Integer.valueOf(request.getParam(_TERMINAL_ORIGIN_ID)));
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_GET_PENDING_COLLECT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    t.printStackTrace();
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            t.printStackTrace();
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void toCollecting(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            JsonObject employee = context.get(EMPLOYEE);
            body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));

            JsonArray parcelsIds = body.getJsonArray(_PARCEL_ID);
            isEmptyAndNotNull(parcelsIds, _PARCEL_ID);
            for (Object parcelId : parcelsIds) {
                isGraterAndNotNull(new JsonObject().put(_PARCEL_ID, parcelId), _PARCEL_ID, 0);
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_TO_COLLECTING);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    t.printStackTrace();
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            t.printStackTrace();
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void toCollected(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            JsonObject employee = context.get(EMPLOYEE);
            body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));

            JsonArray parcelsIds = body.getJsonArray(_PARCEL_ID);
            isEmptyAndNotNull(parcelsIds, _PARCEL_ID);
            for (Object parcelId : parcelsIds) {
                isGraterAndNotNull(new JsonObject().put(_PARCEL_ID, parcelId), _PARCEL_ID, 0);
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_TO_COLLECTED);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    t.printStackTrace();
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            t.printStackTrace();
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void toDocumented(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            JsonObject employee = context.get(EMPLOYEE);
            body.put(TERMINAL_ID, employee.getInteger(BRANCHOFFICE_ID));

            JsonArray parcelsIds = body.getJsonArray(_PARCEL_ID);
            isEmptyAndNotNull(parcelsIds, _PARCEL_ID);
            for (Object parcelId : parcelsIds) {
                isGraterAndNotNull(new JsonObject().put(_PARCEL_ID, parcelId), _PARCEL_ID, 0);
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_TO_DOCUMENTED);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    t.printStackTrace();
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t) {
            t.printStackTrace();
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void getExtendedTracking(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            String parcelTrackingCode = request.getParam(_PARCEL_TRACKING_CODE);
            JsonObject body = new JsonObject()
                    .put(_PARCEL_TRACKING_CODE, parcelTrackingCode);
            isEmptyAndNotNull(body, _PARCEL_TRACKING_CODE);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_GET_EXTENDED_TRACKING);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    t.printStackTrace();
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void originArrivalContingency(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            JsonArray packIds = body.getJsonArray(_PACKAGES);
            isEmptyAndNotNull(packIds, _PACKAGES);
            for (Object packId : packIds) {
                isGraterAndNotNull(new JsonObject().put(_PACKAGES, packId), _PACKAGES, 0);
            }

            isEmpty(body, _NOTES);
            isGraterAndNotNull(body, _TERMINAL_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_ORIGIN_ARRIVAL_CONTINGENCY);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void signatureDeliveryReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _TERMINAL_ID, 0);
            isGrater(body, _USER_ID, 0);
            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isEmptyAndNotNull(body, _SHIPMENT_TYPE);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_SIGNATURE_DELIVERIES_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

    private void signatureDeliveryReportDetail(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _TERMINAL_ID, 0);
            isGraterAndNotNull(body, _USER_ID, 0);
            isEmptyAndNotNull(body, _INIT_DATE);
            isEmptyAndNotNull(body, _END_DATE);
            isEmptyAndNotNull(body, _SHIPMENT_TYPE);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_SIGNATURE_DELIVERIES_DETAIL_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    responseOk(context, reply.result().body());

                } catch (Throwable t) {
                    exceptionLogger(context, body, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            exceptionLogger(context, null, t, exceptionLogHandler -> responseError(context, UNEXPECTED_ERROR, t));
        }
    }

}
