package service.parcel;

import database.branchoffices.BranchofficeDBV;
import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.parcel.GuiappDBV;
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
import service.commons.ServiceVerticle;
import service.commons.middlewares.*;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.parcel.GuiappDBV.REGISTER;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

import static service.commons.Constants.*;

public class GuiappSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return GuiappDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/guiapp";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/valid/:code", PublicRouteMiddleware.getInstance(), this::validCode);
        this.addHandler(HttpMethod.GET, "/terminals/distance", PublicRouteMiddleware.getInstance(), this::getTerminalsDistance);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), CashOutMiddleware.getInstance(vertx), InternalCustomerMiddleware.getInstance(vertx),CreditMiddleware.getInstance(vertx), this::register);
        this.addHandler(HttpMethod.GET, "/trackingCode/:parcelTrackingCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getParcelByTrackingCode);
        this.addHandler(HttpMethod.POST, "/valid/rango/", AuthMiddleware.getInstance(), this::validRangoGuiapp);
        this.addHandler(HttpMethod.GET, "/cancel/detail/:parcelTrackingCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getParcelByTrackingCodeCancel);
        this.addHandler(HttpMethod.POST, "/salesReport", AuthMiddleware.getInstance(), this::salesReportInfo);
        this.addHandler(HttpMethod.POST, "/salesReport/totals", AuthMiddleware.getInstance(), this::salesReportTotals);
        this.addHandler(HttpMethod.POST, "/availableReport", AuthMiddleware.getInstance(), this::availableReportInfo);
        this.addHandler(HttpMethod.POST, "/availableReport/totals", AuthMiddleware.getInstance(), this::availableReportTotals);
        this.addHandler(HttpMethod.GET, "/cancel/detail/:parcelTrackingCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getParcelByTrackingCodeCancel);
        this.addHandler(HttpMethod.GET, "/cancel/guiapp/all/:parcelTrackingCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getParcelByTrackingCodeCancelGuiappAll);
        this.addHandler(HttpMethod.POST, "/cancel/register", AuthMiddleware.getInstance(), this::cancelRegisterGuiapp);
        this.addHandler(HttpMethod.GET, "/list/guiappcode/:code",  AuthMiddleware.getInstance(), this::getListGuiappCodev2);
        this.addHandler(HttpMethod.POST, "/searchCodes",  AuthMiddleware.getInstance(), this::getListGuiappByCustomer);
        this.addHandler(HttpMethod.POST, "/searchCodes/parcelsDetail/",  AuthMiddleware.getInstance(), this::getDetailGuia);
        this.addHandler(HttpMethod.POST, "/maxCost",  AuthMiddleware.getInstance(), this::getMaxCost);
        this.addHandler(HttpMethod.POST, "/excessCost",  AuthMiddleware.getInstance(), this::getExcessCost);
        this.addHandler(HttpMethod.POST, "/getCustomerRanges",  AuthMiddleware.getInstance(), this::getCustomerRanges);
        this.addHandler(HttpMethod.POST, "/getCustomerKms",  AuthMiddleware.getInstance(), this::getCustomerKms);
        this.addHandler(HttpMethod.POST, "/costExchange",  AuthMiddleware.getInstance(), this::costExchange);
        this.addHandler(HttpMethod.POST, "/multiple/valid",  AuthMiddleware.getInstance(), this::multipleValidation);


        super.start(startFuture);
    }

    public void validCode(RoutingContext context) {
        JsonObject data = context.getBodyAsJson();
        try {
            //Validar data


            HttpServerRequest request = context.request();
            String guiappCode = request.getParam("code");

            JsonObject body = new JsonObject().put("code", guiappCode);
            isEmptyAndNotNull(body, "code");
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.VALID_CODE_GUIA);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Element found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    public void getTerminalsDistance(RoutingContext context) {
        try {

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_PACKAGE_TERMINAL_DISTANCE);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Element found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, "Ocurrio un error inesperado al registrar", t);
                }
            });
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex.getMessage());
        }
    }

    private void register(RoutingContext context) {
        try {
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, REGISTER);
            body
                    .put(CREATED_BY, context.<Integer>get(USER_ID))
                    .put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
                    .put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID))
                    .put(INTERNAL_CUSTOMER, context.<Boolean>get(INTERNAL_CUSTOMER));

            Boolean isCreditParcel = Boolean.FALSE;
            if (body.containsKey("is_credit")){
                isCreditParcel = body.getBoolean("is_credit");
            }

            JsonObject branchOrigin = new JsonObject().put("id", body.getInteger("branchoffice_id"));

            Boolean paysSender = null;
            try {
                paysSender = body.getBoolean("pays_sender");
            } catch (Exception e){
                paysSender = body.getInteger("pays_sender").equals(1);
            }
            body.put("pays_sender", paysSender);

            JsonObject customerId = new JsonObject().put(CUSTOMER_ID, body.getInteger(CUSTOMER_ID));

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();
            Future<Message<JsonObject>> f5 = Future.future();

            if (branchOrigin.getInteger("id") != null ) {
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchOrigin, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f1.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "parcel_iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
                eventBus.send(CustomerDBV.class.getSimpleName(), customerId, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f5.completer());

                Boolean finalIsCreditParcel = isCreditParcel;
                CompositeFuture.all(f1, f2, f3, f4, f5).setHandler(detailReply -> {
                    try {
                        if (detailReply.failed()) {
                            throw new Exception(detailReply.cause());
                        }
                        Message<JsonObject> branch1Status = detailReply.result().resultAt(0);
                        Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                        Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(2);
                        Message<JsonObject> parcelIvaMsg = detailReply.result().resultAt(3);
                        Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(4);

                        JsonObject ivaPercent = ivaPercentMsg.body();
                        JsonObject currencyId = currencyIdMsg.body();
                        JsonObject parcelIva = parcelIvaMsg.body();
                        JsonObject customerCreditData = customerCreditDataMsg.body();

                        body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                        body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                        body.put("parcel_iva", Double.valueOf(parcelIva.getString("value")));
                        body.put("payment_condition", finalIsCreditParcel ? "credit" : "cash");
                        body.put("customer_credit_data", customerCreditData);

                        try {
                            isGraterAndNotNull(body, "customer_id", 0);
                            isStatusActive(branch1Status.body(), "status");

                            if (finalIsCreditParcel ) {
                                Double parcelAvailableCredit = customerCreditData.getDouble("available_credit");
                                Boolean parcelHasCredit = customerCreditData.getBoolean("has_credit");
                                if (parcelAvailableCredit == null) parcelAvailableCredit = Double.valueOf(0);
                                Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                                body.put("debt", parcelPaymentsAmount);

                                if (!parcelHasCredit) {
                                    throw new Exception("Customer: no credit available");
                                }
                                if (parcelAvailableCredit < parcelPaymentsAmount) {
                                    throw new Exception("Customer: Insufficient funds to apply credit");
                                }
                                if(!customerCreditData.getString("services_apply_credit").contains("parcel"))
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

                                } catch (Throwable t){
                                    t.printStackTrace();
                                    responseError(context, UNEXPECTED_ERROR, t);
                                }
                            });
                        } catch (UtilsValidation.PropertyValueException ex) {
                            ex.printStackTrace();
                            UtilsResponse.responsePropertyValue(context, ex);
                        }

                    }catch(Exception e ){
                        e.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            } else {
                responseError(context, "Se deben especificar las sucursales tanto de origen como de destino.", "You have to specify origin branch an destiny branch");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private  void getParcelByTrackingCode (RoutingContext context) {
        int userId = context.get(USER_ID);
        JsonObject trackingCode = new JsonObject().put("parcelTrackingCode",context.request().getParam("parcelTrackingCode")).put("updated_by", userId);
        try{

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_PARCEL_BY_TRACKING_CODE);
            vertx.eventBus().send(this.getDBAddress(), trackingCode, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    public void validRangoGuiapp(RoutingContext context) {
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_VALID_RANGO_GUIAPP);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Element found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, "Ocurrio un error inesperado al registrar", t);
                }
            });


    }

      private void salesReportInfo(RoutingContext context){
        this.salesReport(context, false);
    }

    private void salesReportTotals(RoutingContext context){
        this.salesReport(context, true);
    }

    private void salesReport(RoutingContext context, boolean flagTotals){
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");
            body.put("flag_totals", flagTotals);
            if (!flagTotals && body.getInteger("page")!=null){
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);
            }

            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.PARCEL_SALES_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    }catch(Exception e ){
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }
    private  void getParcelByTrackingCodeCancel (RoutingContext context) {
        int userId = context.get(USER_ID);
        JsonObject trackingCode = new JsonObject().put("parcelTrackingCode",context.request().getParam("parcelTrackingCode")).put("updated_by", userId);
        try{

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_PARCEL_BY_TRACKING_CODE_CANCEL);
            vertx.eventBus().send(this.getDBAddress(), trackingCode, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void availableReportInfo(RoutingContext context){
        this.availableReport(context, false);
    }

    private void availableReportTotals(RoutingContext context){
        this.availableReport(context, true);
    }

    private void availableReport(RoutingContext context, boolean flagTotals){
        JsonObject body = context.getBodyAsJson();

        try {
            body.put("flag_totals", flagTotals);
            if (!flagTotals && body.getInteger("page")!=null){
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);
            }

            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.PARCEL_AVAILABLE_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    }catch(Exception e ){
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            }catch (Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }


    private  void getParcelByTrackingCodeCancelGuiappAll (RoutingContext context) {
        int userId = context.get(USER_ID);
        JsonObject trackingCode = new JsonObject().put("parcelTrackingCode",context.request().getParam("parcelTrackingCode")).put("updated_by", userId);
        try{

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_PARCEL_BY_TRACKING_CODE_CANCEL_GUIAPP_ALL);
            vertx.eventBus().send(this.getDBAddress(), trackingCode, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    public void cancelRegisterGuiapp(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.CANCEL_REGISTER_GUIAPP);
        vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                if(reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA , INVALID_DATA_MESSAGE , reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Element found");
                }
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Ocurrio un error inesperado al registrar", t);
            }
        });


    }

    private  void getListGuiappCode (RoutingContext context) {
        try{
            JsonObject code = new JsonObject().put("code",context.request().getParam("code"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_LIST_GUIAPP_CODE);
            vertx.eventBus().send(this.getDBAddress(), code, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());


                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private  void getListGuiappCodev2 (RoutingContext context) {
        try{
            JsonObject code = new JsonObject().put("code",context.request().getParam("code"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_LIST_GUIAPP_CODE);
            vertx.eventBus().send(this.getDBAddress(), code, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private  void getListGuiappByCustomer (RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_LIST_GUIAPP_BY_CUSTOMER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
    public void getDetailGuia(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_DETAIL_GUIA);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void getCustomerRanges(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_GUIAPP_RANGES_BY_CUSTOMER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void getCustomerKms(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_GUIAPP_KMS_BY_CUSTOMER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }


    public void getMaxCost(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();

            isContainedAndNotNull(body, _EXCESS_BY, _WEIGHT, _VOLUME);
            isEmptyAndNotNull(body, _BASE_PACKAGE_PRICE_NAME);
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_MAX_COST);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    public void getExcessCost(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();

            isBooleanAndNotNull(body, _COST_BREAKDOWN);
            isEmptyAndNotNull(body, _BASE_PACKAGE_PRICE_NAME);
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);
            isGraterAndNotNull(body, _WEIGHT, -1);
            isGraterAndNotNull(body, _HEIGHT, -1);
            isGraterAndNotNull(body, _LENGTH, -1);
            isGraterAndNotNull(body, _WIDTH, -1);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_EXCESS_COST);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    public void costExchange(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();

            isBoolean(body, _COST_BREAKDOWN);
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);
            isGrater(body, _CUSTOMER_ID, 0);
            isGrater(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            isContained(body, _SHIPMENT_TYPE, "OCU", "RAD/OCU", "EAD", "RAD/EAD");

            JsonArray parcelPackages = body.getJsonArray(_PARCEL_PACKAGES);
            isEmptyAndNotNull(parcelPackages, _PARCEL_PACKAGES, _PARCEL_PACKAGES);
            for (Object pp : parcelPackages) {
                JsonObject pack = (JsonObject) pp;
                isGraterAndNotNull(pack, _PARCEL_PREPAID_ID, 0);
                isNotNull(pack, _DETAILS);
                JsonObject details = pack.getJsonObject(_DETAILS);
                JsonArray codes = details.getJsonArray(_CODES);
                isEmptyAndNotNull(codes, _CODES, _CODES);
                for (Object c : codes) {
                    JsonObject code = (JsonObject) c;
                    isEmptyAndNotNull(code, _SHIPPING_TYPE);
                    String shippingType = code.getString(_SHIPPING_TYPE);
                    if (shippingType.equals("parcel")) {
                        isGraterAndNotNull(code, _WEIGHT, 0.0);
                        isGraterAndNotNull(code, _HEIGHT, 0.0);
                        isGraterAndNotNull(code, _LENGTH, 0.0);
                        isGraterAndNotNull(code, _WIDTH, 0.0);
                    }
                    isGrater(code, _QUANTITY, 0);
                }
            }

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.GET_COST_EXCHANGE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    public void multipleValidation(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            JsonArray codes = body.getJsonArray(_CODES);
            isEmptyAndNotNull(codes, _CODES, _CODES);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.VALID_MULTIPLE_CODE_GUIA);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                }catch(Exception e ){
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }
}

