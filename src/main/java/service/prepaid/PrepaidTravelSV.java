package service.prepaid;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.*;
import io.vertx.core.Future;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import database.prepaid.PrepaidTravelDBV;
import utils.UtilsID;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.prepaid.PrepaidTravelDBV.ACTION_REGISTER_TICKETS;

import static utils.UtilsResponse.responseError;
import static utils.UtilsValidation.isDateTimeAndNotNull;
import static utils.UtilsValidation.isGraterAndNotNull;

public class PrepaidTravelSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() { return PrepaidTravelDBV.class.getSimpleName(); }

    @Override
    protected String getEndpointAddress() {
        return "/prepaid_package_travel";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST,"/register", AuthMiddleware.getInstance(), CreditMiddleware.getInstance(vertx), CashOutMiddleware.getInstance(vertx),this::register);
        this.addHandler(HttpMethod.GET, "/find", AuthMiddleware.getInstance(), this::findPrepaidTravel);
        this.addHandler(HttpMethod.POST, "/searchBoardingPasses" , AuthMiddleware.getInstance(), this::searchPassesById);
        this.addHandler(HttpMethod.POST, "/terminalDestinyByOriginDistance", AuthMiddleware.getInstance(), this::getTerminalDestinyByOriginDistance);
        this.addHandler(HttpMethod.POST, "/prepaidSalesReport", AuthMiddleware.getInstance(), this::salesReportInfo);
        this.addHandler(HttpMethod.POST, "/prepaidSalesReport/totals", AuthMiddleware.getInstance(), this::salesReportTotals);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        try {
            EventBus eventBus = vertx.eventBus();
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_REGISTER_TICKETS);
            body
                .put(CREATED_BY, context.<Integer>get(USER_ID))
                .put(CASHOUT_ID, Integer.parseInt(body.getString(CASHOUT_ID)))
                .put(CASH_REGISTER_ID, Integer.parseInt(body.getString(CASH_REGISTER_ID)))
                .put("reservation_code", UtilsID.generateID("BP"));

            JsonObject customer = body.getJsonObject("customer");
            customer.put("customer_id" ,customer.getInteger("id") );
            //.put(CASHOUT_ID, context.<Integer>get(CASHOUT_ID))
            //.put(CASH_REGISTER_ID, context.<Integer>get(CASH_REGISTER_ID))

            Boolean isCredit = body.getBoolean("is_credit");

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();

            eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "expire_open_tickets_after"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), customer, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f4.completer());

            CompositeFuture.all(f1, f2, f3, f4).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()) {
                        throw new Exception(detailReply.cause());
                    }
                    Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> expireOpenTicketsAfterMsg = detailReply.result().resultAt(2);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(3);

                    JsonObject currencyId = currencyIdMsg.body();
                    JsonObject ivaPercent = ivaPercentMsg.body();
                    JsonObject expireOpenTicketsAfter = expireOpenTicketsAfterMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();

                    if(isCredit) {
                        Double availableCredit = customerCreditData.getDouble("available_credit");
                        Boolean hasCredit = customerCreditData.getBoolean("has_credit");
                        Double prepaidAmount = body.getJsonObject("prepaid_package").getDouble("money");
                        JsonArray payments = body.getJsonArray("payments");
                        for(int i = 0; i < payments.size(); i++) {
                            JsonObject pay = payments.getJsonObject(i);
                            if(!pay.isEmpty() && pay.getInteger("id") != -1){
                                throw new Exception("Customer: partial credit payment method not available");
                            }
                        }
                        body.put("credit_amount", prepaidAmount);
                        if (!hasCredit)
                            throw new Exception("Customer: no credit available");
                        if(availableCredit < prepaidAmount)
                            throw new Exception("Customer: Insufficient funds to apply credit");
                        if(!customerCreditData.getString("services_apply_credit").contains("prepaid"))
                            throw new Exception("Customer: service not applicable");
                    }

                    body.put("currency_id", Integer.valueOf(currencyId.getString("value")))
                            .put("iva_percent", Double.valueOf(ivaPercent.getString("value")))
                            .put("expire_open_tickets_after", Integer.parseInt(expireOpenTicketsAfter.getString("value")))
                            .put("customer_credit_data", customerCreditData);
                    eventBus.send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if(reply.failed()) {
                                throw reply.cause();
                            }

                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Created");
                            }

                        } catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t);
                        }
                    });
                }catch (Exception e ){
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void findPrepaidTravel(RoutingContext context){
        try {
            JsonObject body = new JsonObject().put("code", context.request().getParam("code"));

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PrepaidTravelDBV.ACTION_GET_PACKAGE_TRAVEL);
            this.vertx.eventBus().send(PrepaidTravelDBV.class.getSimpleName(), body,options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });

        } catch (Exception e) {
            responseError(context, e);
        }
    }

    private void searchPassesById(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PrepaidTravelDBV.ACTION_GET_BOARDINGPASSES_BY_ID);

            this.vertx.eventBus().send(PrepaidTravelDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw  reply.cause();
                    }
                    responseOk(context, reply.result().body(), "Found");
                } catch (Throwable t) {
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                }
            });
        } catch (Exception e) {
            responseError(context, e);
        }
    }

    private void getTerminalDestinyByOriginDistance(RoutingContext context) {
        try {
            JsonObject params = context.getBodyAsJson();
            this.vertx.eventBus().send(PrepaidTravelDBV.class.getSimpleName(), params, options(PrepaidTravelDBV.ACTION_GET_DESTINY_TERMINALS_BY_ORIGIN_DISTANCE), reply -> {
                try {
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, t);
                }
            });
        } catch (Exception e) {
            responseError(context, e);
        }
    }

    private void salesReportInfo(RoutingContext context) {
        this.salesReport(context, false);
    }

    private void salesReportTotals(RoutingContext context) {
        this.salesReport(context, true);
    }

    private void salesReport(RoutingContext context, boolean flagTotals) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, "init_date", "prepaid");
            isDateTimeAndNotNull(body, "end_date", "prepaid");
            body.put("flag_totals", flagTotals);
            if(body.getInteger("page") != null) {
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);
            }
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PrepaidTravelDBV.PREPAID_SALES_REPORT);
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if(reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        responseOk(context, reply.result().body());
                    } catch(Exception e) {
                        e.printStackTrace();
                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                    }
                });
            } catch(Exception ex) {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
            }
        } catch(UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }
}
