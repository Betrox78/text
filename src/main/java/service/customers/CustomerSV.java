/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.customers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import database.commons.ErrorCodes;
import database.customers.CustomerDBV;
import database.invoicing.InvoiceDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.CreditMiddleware;
import service.commons.middlewares.PermissionMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsJWT;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.customers.CustomerDBV.ONLY_DEBTORS;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CustomerSV extends ServiceVerticle {

    private AmazonSNS amazonSNSClient;

    @Override
    protected String getDBAddress() {
        return CustomerDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/customers";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(POST, "/externalRegister", PublicRouteMiddleware.getInstance(), this::externalRegister);
        this.addHandler(POST, "/appClientRegister", PublicRouteMiddleware.getInstance(), this::appClientRegister);
        this.addHandler(GET, "/confirmAccount/:token", PublicRouteMiddleware.getInstance(), this::confirmAccount);
        this.addHandler(HttpMethod.POST, "/billingInformation", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::registerBillingInformation);
        this.addHandler(HttpMethod.GET, "/billingInformation/:customerID", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findBillingInformation);
        this.addHandler(HttpMethod.GET, "/billingInformation/triggerInvoices/:customerCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::triggerCustomerInvoices);
        this.addHandler(HttpMethod.GET, "/billingInformation/actions/sync", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::syncPendingBilling);
        this.addHandler(HttpMethod.GET, "/billingInformation/:rfc/:zipCode", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findBillingInformation);
        this.addHandler(HttpMethod.GET, "/getAllBillingInformation/:customer_id", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllBillingInformation);
        this.addHandler(HttpMethod.POST, "/snsNotification", this::receiveSNSCustomer);
        this.addHandler(HttpMethod.POST, "/subscription", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::subscription);
        this.addHandler(HttpMethod.GET, "/search/:search_term", AuthMiddleware.getInstance(), this::searchCustomer);
        this.addHandler(HttpMethod.GET, "/search", AuthMiddleware.getInstance(), this::searchCustomer);
        this.addHandler(HttpMethod.GET, "/checkCredit/:customer", AuthMiddleware.getInstance(), CreditMiddleware.getInstance(vertx), this::checkCreditbyCustomer);
        this.addHandler(HttpMethod.GET, "/deliveryCustomerCredit/:parcelTrackingCode", AuthMiddleware.getInstance(), this::checkCreditbyParcelId);
        this.addHandler(HttpMethod.POST, "/report/wallet", AuthMiddleware.getInstance(), this::walletReport);
        this.addHandler(HttpMethod.POST, "/report/wallet/detail", AuthMiddleware.getInstance(), this::walletDetailReport);
        this.addHandler(HttpMethod.POST,"/report/customerByDate/", AuthMiddleware.getInstance(), this::createReportCustomer);
        this.addHandler(HttpMethod.GET, "/report/debt/:customer_id", AuthMiddleware.getInstance(), this::getDebtCustomer);
        this.addHandler(HttpMethod.POST, "/getServicesWithDebt", AuthMiddleware.getInstance(), this::getServicesWithDebt);
        this.addHandler(HttpMethod.GET,"/getDebt/:customer_id/:code", AuthMiddleware.getInstance(), this::getCustomerDebt);
        this.addHandler(HttpMethod.GET,"/byUserId", AuthMiddleware.getInstance(), this::getCustomerByUserId);
        this.addHandler(HttpMethod.PUT,"/updateCustomerInfo", AuthMiddleware.getInstance(), this::updateCustomerInfo);
        this.addHandler(HttpMethod.POST, "/billingInformation/checkRFC", AuthMiddleware.getInstance(), this::checkRFC);
        this.addHandler(HttpMethod.POST, "/billingInformation/delete", AuthMiddleware.getInstance(), this::deleteBilling);
        this.addHandler(HttpMethod.POST, "/report/debt/accountStatus", AuthMiddleware.getInstance(), this::accountStatusReport);
        this.addHandler(HttpMethod.GET,"/getDetail/:id", AuthMiddleware.getInstance(), this::getDetail);
        this.addHandler(HttpMethod.POST, "/updateBasicInfo", AuthMiddleware.getInstance(), this::updateBasicInfo);
        this.addHandler(HttpMethod.POST, "/passengers", AuthMiddleware.getInstance(), this::createPassenger);
        this.addHandler(HttpMethod.PUT, "/passengers", AuthMiddleware.getInstance(), this::updatePassenger);
        this.addHandler(HttpMethod.GET,"/passengers/:id", AuthMiddleware.getInstance(), this::getPassengersByCustomerId);
        this.addHandler(HttpMethod.POST,"/fcmToken", AuthMiddleware.getInstance(), this::createFCMToken);
        this.addHandler(HttpMethod.POST,"/verifyWithPhone", AuthMiddleware.getInstance(), this::verifyWithPhone);
        this.addHandler(HttpMethod.POST,"/getByAdviserId", AuthMiddleware.getInstance(), this::getByAdviserId);
        this.addHandler(HttpMethod.POST, "/search/v2", AuthMiddleware.getInstance(), this::searchCustomerV2);
        this.addHandler(HttpMethod.POST, "/advancedSearch", AuthMiddleware.getInstance(), PermissionMiddleware.getInstance(vertx, "app.customers"), this::customerCatalogueList);
        this.addHandler(HttpMethod.GET, "/isAssigned/:customer_id", AuthMiddleware.getInstance(), this::isAssigned);

        super.start(startFuture);
        this.router.post("/register").handler(BodyHandler.create());
        this.router.post("/register").handler(this::register);
        this.router.get("/report/allInfo").handler(this::reportAllInfo);
        if (InvoiceDBV.REGISTER_INVOICES) {
            subscribeSNSCustomer();
        }
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            containsSpecialCharacter(body, "first_name");
            isDate(body, "birthday");
            isPhoneNumber(body, "phone");
            isMail(body, "email");

        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            containsSpecialCharacterAndNotNull(body, "first_name");
            isBoolean(body, "has_credit");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

    private void searchCustomer(RoutingContext context) {
        int limit = 0;
        String param = context.request().getParam("search_term") != null ? context.request().getParam("search_term") : "";
        try {
            limit = context.request().getParam("limit") != null ? Integer.parseInt(context.request().getParam("limit")) : 0;
        } catch (Exception ignored) { }
        JsonObject searchTerm = new JsonObject().put("searchTerm", param).put("limit", limit);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_SEARCH_BY_NAME_AND_LASTNAME);
            vertx.eventBus().send(this.getDBAddress(), searchTerm, options, reply -> {
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
    }


    private void searchCustomerV2(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_SEARCH_V2);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }
            });
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e);
        }
    }

    private void register(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject body = context.getBodyAsJson();
            try {
                int userId = UtilsJWT.getUserIdFrom(jwt);
                //validarte customer
                containsSpecialCharacterAndNotNull(body, "first_name");
                isDate(body, "birthday");
                isPhoneNumber(body, "phone");
                isMail(body, "email");
                isBoolean(body, "has_credit");
                body.put(CREATED_BY, userId);

                //customer billing info
                JsonObject billingInfo = body.getJsonObject("billing_info");
                if (billingInfo != null) {
                    isNameAndNotNull(billingInfo, "name", "billing_info");
                    isEmptyAndNotNull(billingInfo, "rfc", "billing_info");
                    isEmptyAndNotNull(billingInfo, "address", "billing_info");
                    billingInfo.put(CREATED_BY, userId);
                }

                DeliveryOptions options = new DeliveryOptions()
                        .addHeader(ACTION, CustomerDBV.ACTION_REGISTER);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Created");
                        }

                    }catch(Exception e){
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());

                    }

                });
            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            }
        } else {
            responseInvalidToken(context);
        }
    }

    private void registerBillingInformation(RoutingContext context) {
            try {
                JsonObject body = context.getBodyAsJson();
                isGraterAndNotNull(body, "customer_id", 0);
                isEmptyAndNotNull(body, "name");
                isEmptyAndNotNull(body, "rfc");
                isEmptyAndNotNull(body, "address");
                body.put(CREATED_BY, context.<Integer>get(USER_ID));

                DeliveryOptions options = new DeliveryOptions()
                        .addHeader(ACTION, CustomerDBV.ACTION_REGISTER_BILLING_INFORMATION);

                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.succeeded()) {
                            Message<Object> result = reply.result();
                            if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                            } else {
                                responseOk(context, result.body(), "Created");
                            }
                        } else {
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }
                    } catch (Exception e) {
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }

                });
            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            } catch (Exception e) {
                responseError(context, UNEXPECTED_ERROR, e.getMessage());
            }
    }

    private void triggerCustomerInvoices(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            DeliveryOptions options = new DeliveryOptions();
            JsonObject body = new JsonObject()
                    .put("cCodigoCliente", request.getParam("customerCode"));

                options.addHeader(ACTION, CustomerDBV.ACTION_TRIGGER_CUSTOMER_INVOICES);
                body.put("cCodigoCliente", request.getParam("customerCode"));

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Triggered");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }

            });
        } catch (Exception ex) {
            responseError(context, ex.getMessage());
        }

    }

    private void syncPendingBilling(RoutingContext context) {
        vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options(CustomerDBV.ACTION_SYNC_CUSTOMER_BILLINGS), reply -> {
            try {
                if (reply.succeeded()) {
                    Message<Object> result = reply.result();
                    if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                    } else {
                        responseOk(context, result.body(), "Synced");
                    }
                } else {
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }
            } catch (Exception e) {
                responseError(context, UNEXPECTED_ERROR, e.getMessage());
            }
        });
    }

    private void findBillingInformation(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions();
            JsonObject body = new JsonObject();

            HttpServerRequest request = context.request();
            String customerID = request.getParam("customerID");
            if (customerID != null) {
                options.addHeader(ACTION, CustomerDBV.ACTION_FIND_BILLING_INFORMATION_BY_CUSTOMER_ID);
                body.put("customerID", Integer.valueOf(customerID));
            } else {
                options.addHeader(ACTION, CustomerDBV.ACTION_FIND_BILLING_INFORMATION_BY_RFC_AND_ZIP_CODE);
                body.put("zipCode", Integer.valueOf(request.getParam("zipCode")))
                    .put("rfc", request.getParam("rfc"));
            }

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Found");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }

            });
        } catch (Exception ex) {
            responseError(context, ex.getMessage());
        }

    }
    private void externalRegister(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            //validate customer
            containsSpecialCharacterAndNotNull(body, "first_name");
            containsSpecialCharacter(body, "last_name");
            if (body.getString("birthday") != null){
                isDate(body, "birthday");
            }
            // isPhoneNumber(body, "phone");
            isMail(body, "email");
            isPasswordAndNotNull(body, "pass");

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, CustomerDBV.ACTION_EXTERNAL_REGISTER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }

                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                }catch(Exception e){
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());

                }

            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void appClientRegister(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            //validate customer
            containsSpecialCharacterAndNotNull(body, "first_name");
            containsSpecialCharacter(body, "last_name");
            if (body.getString("birthday") != null){
                isDate(body, "birthday");
            }
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            isPasswordAndNotNull(body, "pass");

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, CustomerDBV.ACTION_APP_CLIENT_REGISTER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }

                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } catch(Exception e){
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }

            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void confirmAccount(RoutingContext context) {
        String token = context.request().getParam("token");
        JsonObject body = new JsonObject().put("token", token);
        try{
            isEmptyAndNotNull(body, "token");
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_CONFIRM_ACCOUNT);
            JsonObject param = new JsonObject().put("token", token);
            vertx.eventBus().send(this.getDBAddress(), param, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Report");
                    }

                }catch(Exception e){
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());

                }

            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void reportAllInfo(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_REPORT);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {

                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Report");
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                }

            });
        } else {
            responseInvalidToken(context);
        }
    }

    private void subscribeSNSCustomer() {
        JsonObject _config = config();
        try {
            // Create a client
            String topic = _config.getString("aws_sns_topic_customer");
            String topicARN = "arn:aws:sns:us-east-2:738558694912:" + topic;

            BasicAWSCredentials credentials = new BasicAWSCredentials(
                    _config.getString("aws_sns_access_key"),
                    _config.getString("aws_sns_secret_key")
            );
            amazonSNSClient = AmazonSNSClientBuilder.standard()
                    .withRegion(Regions.US_EAST_2)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();

            // Subscribe to topic
            String endpoint = "https://".concat(_config.getString("self_server_host"))
                    .concat("/customers/snsNotification");
            SubscribeRequest subscribeReq = new SubscribeRequest()
                    .withTopicArn(topicARN)
                    .withProtocol("https")
                    .withEndpoint(endpoint);
            System.out.println("Subscribing to: ".concat(topicARN).concat(" | with endpoint: ").concat(endpoint));

            amazonSNSClient.subscribe(subscribeReq);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveSNSCustomer(RoutingContext context) {
        HttpServerResponse res = context.response();
        try {
            // Create a client
            String topicARN = "arn:aws:sns:us-east-2:738558694912:CteProvedorSNSTopic";
            JsonObject message = context.getBodyAsJson();
            System.out.println("message: ".concat(message.encodePrettily()));

            String token = message.getString("Token");
            if (token != null) {
                // Confirm subscription
                ConfirmSubscriptionRequest confirmReq = new ConfirmSubscriptionRequest()
                        .withTopicArn(topicARN)
                        .withToken(token);
                amazonSNSClient.confirmSubscription(confirmReq);
                System.out.println("Confirm subscription to topic: ".concat(topicARN));
                res.setStatusCode(200).end("OK");
            } else {
                String msg = message.getString("Message");
                System.out.println("Getting SNS message: ".concat(msg));
                JsonObject value = new JsonObject(msg);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_CONFIRM_BILLING_INFORMATION);

                String status = value.getString("status");
                if (status != null && status.equalsIgnoreCase("OK")) {
                    Integer instanceId = value.getInteger("empresa");
                    JsonObject body = new JsonObject(value.getString("data"));
                    body.put("instance_id", instanceId);
                    vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if (reply.succeeded()) {
                                Message<Object> result = reply.result();
                                if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    res.setStatusCode(500).end(result.body().toString());
                                } else {
                                    res.setStatusCode(200).end("OK");
                                }
                            } else {
                                reply.cause().printStackTrace();
                                res.setStatusCode(500).end(reply.cause().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            res.setStatusCode(500).end(reply.cause().getMessage());
                        }

                    });
                } else {
                    // TODO: What to do on wrapper exceptions?
                    System.out.println(value.encodePrettily());
                    res.setStatusCode(200).end("OK");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatusCode(500).end(e.getMessage());
        }
    }

    private void subscription(RoutingContext ctx) {
        try {
            JsonObject body = ctx.getBodyAsJson();
            body.put(CREATED_BY, ctx.<Integer>get(USER_ID));
            UtilsValidation.isMailAndNotNull(body, "email");

            vertx.eventBus().send(this.getDBAddress(), body, options(CustomerDBV.ACTION_SUBSCRIPTION), reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(ctx, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(ctx, result.body(), "Created");
                        }
                    } else {
                        responseError(ctx, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(ctx, UNEXPECTED_ERROR, e.getMessage());
                }

            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(ctx, ex);
        } catch (Exception ex) {
            responseError(ctx, ex.getMessage());
        }
    }

    private  void checkCreditbyCustomer (RoutingContext context) {
        String customerId = context.request().getParam("customer");
        JsonObject customer = new JsonObject().put(CUSTOMER_ID, Integer.valueOf(customerId));
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
            vertx.eventBus().send(this.getDBAddress(), customer, options, reply -> {
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch(Exception e){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private  void checkCreditbyParcelId(RoutingContext context) {
        String trackingCode = context.request().getParam("parcelTrackingCode");
        JsonObject customer = new JsonObject().put("parcelTrackingCode", Integer.valueOf(trackingCode));
        try{
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT_BY_PARCELID);
            vertx.eventBus().send(this.getDBAddress(), customer, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }

    }

    private void walletReport(RoutingContext context) {
        try {

            JsonObject body = context.getBodyAsJson();

            isBoolean(body, ONLY_DEBTORS);
            isGrater(body, CUSTOMER_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_CUSTOMER_WALLET_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException t){
            t.printStackTrace();
            responsePropertyValue(context, t);
        } catch (Exception e){
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }

    }

private void getCustomerDebt(RoutingContext context){
    try {
        Integer customerId = Integer.parseInt(context.request().getParam(CUSTOMER_ID));
        String code = context.request().getParam("code");
        JsonObject body = new JsonObject().put(CUSTOMER_ID, customerId).put("code", code);

        isGraterAndNotNull(body, CUSTOMER_ID, 0);

        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_DEBT);
        vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                responseOk(context, reply.result().body());
            } catch(Throwable t){
                t.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, t.getMessage());
            }
        });
    } catch (PropertyValueException ex) {
        ex.printStackTrace();
        responsePropertyValue(context, ex);
    } catch (Exception ex) {
        ex.printStackTrace();
        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
    }
}
    private void walletDetailReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CUSTOMER_ID, Integer.valueOf(body.getString(CUSTOMER_ID)));
            isGraterAndNotNull(body, CUSTOMER_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_CUSTOMER_WALLET_DETAIL_REPORT);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void createReportCustomer(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();

        try {
            isDateTimeAndNotNull(body, "init_date", "parcels");
            isDateTimeAndNotNull(body, "end_date", "parcels");

            try{
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_CREATE_REPORT_CUSTOMERS);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());


                    }catch(Exception e ){
                        e.printStackTrace();
                        responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            }catch (Exception ex) {
                responseError(context, "Ocurri� un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
            }
    }

    private void getDebtCustomer(RoutingContext context){
        try {
            Integer customerId = Integer.parseInt(context.request().getParam(CUSTOMER_ID));
            JsonArray validServices = new JsonArray().add("boarding_pass").add("parcel").add("guiapp").add("prepaid");
            JsonObject customer = new JsonObject().put(CUSTOMER_ID, customerId)
            .put("services", validServices);

            isGraterAndNotNull(customer, CUSTOMER_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_DEBTS);
            vertx.eventBus().send(this.getDBAddress(), customer, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch(Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void getCustomerByUserId(RoutingContext context){
        try{
            Integer userId = Integer.parseInt(context.request().getParam(USER_ID));
            JsonObject body = new JsonObject().put(USER_ID, userId);

            isGraterAndNotNull(body, USER_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_BY_USER_ID);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void updateCustomerInfo(RoutingContext context) {
        try {

            JsonObject body = context.getBodyAsJson();


           this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_UPDATE_CUSTOMER_INFO), reply -> {
               if(reply.succeeded()) {
                   if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                       responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                   } else {
                       responseOk(context, reply.result().body(), "Updated");
                   }
               } else {
                   responseError(context, "Ocurrio un error inesperado");
               }
           });
        } catch(Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void checkRFC(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, CustomerDBV.ACTION_BILLING_INFORMATION_RFC);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Response");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }

            });
        }catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void deleteBilling(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, CustomerDBV.ACTION_BILLING_DELETE_INFO);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Response");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }

            });
        } catch (Exception e) {
            responseError(context,UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void accountStatusReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, "customerID", 0);
            isContainedAndNotNull(body, "service", "all", "parcels", "parcelsPrepaid");
            isDateAndNotNull(body, "initDate");
            isDateAndNotNull(body, "endDate");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_ACCOUNT_STATUS_REPORT);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Found");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }

            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex.getMessage());
        }
    }

    private void getDetail(RoutingContext context){
        Integer id = Integer.parseInt(context.request().getParam(ID));
        JsonObject body = new JsonObject().put(ID, id);
        this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_GET_DETAIL), reply -> {
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
    }

    private void updateBasicInfo(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_UPDATE_BASIC_INFO), reply -> {
                if(reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Updated");
                    }
                } else {
                    responseError(context, "Ocurrio un error inesperado");
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void createPassenger(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_CREATE_PASSENGER), reply -> {
                if(reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } else {
                    responseError(context, "Ocurrio un error inesperado");
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void updatePassenger(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_UPDATE_PASSENGER), reply -> {
                if(reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Updated");
                    }
                } else {
                    responseError(context, "Ocurrio un error inesperado");
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void getPassengersByCustomerId(RoutingContext context){
        Integer customerId = Integer.parseInt(context.request().getParam(ID));
        JsonObject body = new JsonObject().put(CUSTOMER_ID, customerId);
        this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_GET_PASSENGERS_BY_CUSTOMER_ID), reply -> {
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
    }

    private void createFCMToken(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_CREATE_FCM_TOKEN), reply -> {
                if(reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                } else {
                    responseError(context, "Ocurrio un error inesperado");
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void verifyWithPhone(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(CustomerDBV.ACTION_VERIFY_WITH_PHONE), reply -> {
                if(reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Verified");
                    }
                } else {
                    responseError(context, "Ocurrio un error inesperado");
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }

    private void getByAdviserId(RoutingContext context){
        try{
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMERS_BY_ADVISER_ID);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void findAllBillingInformation(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_FIND_ALL_BILLING_INFORMATION_BY_CUSTOMER_ID);
            JsonObject body = new JsonObject();
            HttpServerRequest request = context.request();
            String customerID = request.getParam("customer_id");
            body.put("customer_id", Integer.valueOf(customerID));

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Found");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            responseError(context, ex.getMessage());
        }
    }

    private void getServicesWithDebt(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            try {
                isDateTimeAndNotNull(body, "init_date", "");
                isDateTimeAndNotNull(body, "end_date", "");
                isGraterAndNotNull(body, "customer_id", 0);
                isEmpty(body, "service_type");
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);

                try {
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_SERVICES_WITH_DEBT);
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
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void customerCatalogueList(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(USER_ID, context.<Integer>get(USER_ID));
            body.put("superuser", context.<Boolean>get("superuser"));
            body.put("permissions", context.<JsonArray>get("permissions"));

            vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMERS_CATALOGUE_LIST), replyGS -> {
                        try {
                            if (replyGS.succeeded()){
                                Message<Object> result = replyGS.result();
                                if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                                } else {
                                    Object bodyPayload = result.body();
                                    if (bodyPayload instanceof JsonObject) {
                                        JsonObject payload = (JsonObject) bodyPayload;
                                        context.response()
                                                .setStatusCode(200)
                                                .putHeader("Content-Type", "application/json")
                                                .end(payload.encode());
                                    } else {
                                        responseOk(context, bodyPayload, "Found");
                                    }
                                }
                            } else {
                                responseError(context, UNEXPECTED_ERROR, replyGS.cause().getMessage());
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });
        } catch (Exception e){
            responseError(context, e.getMessage());
        }
    }

    private void isAssigned(RoutingContext context){
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put(USER_ID, context.<Integer>get(USER_ID))
                    .put(_CUSTOMER_ID, Integer.parseInt(request.getParam(_CUSTOMER_ID)));

            vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_IS_ASSIGNED), replyGS -> {
                        try {
                            if (replyGS.failed()) {
                                throw replyGS.cause();
                            }

                            responseOk(context, replyGS.result().body(), "Found");

                        } catch (Throwable t){
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });
        } catch (Exception e){
            responseError(context, e.getMessage());
        }
    }

}
