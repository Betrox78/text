package service.money;

import database.commons.ErrorCodes;
import database.customers.CustomerDBV;
import database.money.CashOutDBV;
import database.money.DebtPaymentDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseError;
import static utils.UtilsValidation.*;
//import static utils.UtilsValidation.isEmptyAndNotNull;

public class DebtPaymentSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return DebtPaymentDBV.class.getSimpleName();
    }

    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/paymentMultiple", AuthMiddleware.getInstance(), this::paymentMultiple);
        super.start(startFuture);
    }
    private void paymentMultiple(RoutingContext context){
        EventBus eventBus = vertx.eventBus();
        JsonObject body = context.getBodyAsJson();
        JsonArray validServices = new JsonArray().add("boarding_pass").add("parcel").add("guiapp").add("prepaid");
        try {
            isGraterAndNotNull(body, CUSTOMER_ID, 0);

            int userId = context.get(USER_ID);
            int customerId = body.getInteger(CUSTOMER_ID);
            Boolean payWithCredit = body.containsKey("pay_with_credit") ? body.getBoolean("pay_with_credit") : Boolean.valueOf(false);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, DebtPaymentDBV.ACTION_MULTIPLE_PAYMENT);
            JsonObject customer = new JsonObject().put(CUSTOMER_ID, customerId);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            eventBus.send(CustomerDBV.class.getSimpleName(), customer.put("services", validServices), new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_DEBTS), f1.completer());
            eventBus.send(CashOutDBV.class.getSimpleName(), new JsonObject().put(USER_ID, userId), new DeliveryOptions().addHeader(ACTION, CashOutDBV.ACTION_GET_CASH_OUT_EMPLOYEE_BY_ID), f2.completer());
            eventBus.send(CustomerDBV.class.getSimpleName(), customer, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f3.completer());
            CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()) {
                        throw new Exception(detailReply.cause());
                    }
                    Message<JsonObject> debtsMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> cashOutMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(2);
                    JsonObject debts = debtsMsg.body();
                    JsonObject cashOut = cashOutMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();

                    body.put("debts", debts)
                            .put(CREATED_BY, userId)
                            .put(CASHOUT_ID, cashOut.getInteger(ID))
                            .put(CASH_REGISTER_ID, cashOut.getInteger(CASH_REGISTER_ID))
                            .put("customer_credit_data", customerCreditData);

                    JsonArray payments = body.getJsonArray("payments");
                    JsonObject cashChange = body.getJsonObject("cash_change");
                    JsonArray services = body.getJsonArray("services");
                    Double totalDebt = customerCreditData.getDouble("total_debt");
                    Double creditBalance = customerCreditData.getDouble("credit_balance");

                    Double servicesTotalAmount = 0.0;
                    for(int i = 0; i < services.size(); i++){
                        JsonObject serviceObject = services.getJsonObject(i);
                        String service = serviceObject.getString("service");
                        Double serviceDebt = customerCreditData.getDouble(serviceObject.getString("service").concat("_debt"));
                        //Double serviceDebt = customerCreditData.getDouble(serviceObject.getString("service").equals("guiapp") ? "parcel".concat("_debt") : serviceObject.getString("service").concat("_debt"));
                        Double serviceAmount = serviceObject.getDouble("amount");
                        servicesTotalAmount += serviceAmount;
                        serviceObject.put("debt", serviceDebt);

                        if(!validServices.contains(serviceObject.getString("service")))
                            throw new Exception("Service: not valid");
                        if(serviceAmount > serviceDebt)
                            throw new Exception(service.concat(": The payment " + serviceAmount + " is greater than the debt " + serviceDebt));
                    }

                    // round servicesTotalAmount
                    BigDecimal bd = new BigDecimal(servicesTotalAmount);
                    bd = bd.setScale(2, RoundingMode.HALF_UP);
                    servicesTotalAmount = bd.doubleValue();

                    Double totalPayment = 0.0;
                    JsonArray paymentMethods = new JsonArray();
                    if(payWithCredit){
                        if(payments == null){
                            JsonObject payment = new JsonObject()
                                    .put("payment_method_id", 0)
                                    .put("amount", servicesTotalAmount);
                            payments = new JsonArray().add(payment);
                            body.put("payments", payments);
                        }
                        else{
                            Double cashPaid = cashChange.getDouble("paid");
                            Double cashTotal = cashChange.getDouble("total");
                            Double totalPayCredit = servicesTotalAmount - cashTotal;
                            JsonObject payment = new JsonObject()
                                    .put("payment_method_id", 0)
                                    .put("amount", totalPayCredit);
                            payments.add(payment);
                            cashChange.put("paid", cashPaid + payment.getDouble("amount"))
                                    .put("total", cashTotal + payment.getDouble("amount"));
                            if(totalPayCredit > creditBalance)
                                throw new Exception("The credit payment " + totalPayCredit + " is greater than the credit balance " + creditBalance);
                        }
                    }

                    for (Object p : payments) {
                        JsonObject payment = new JsonObject(String.valueOf(p));
                        totalPayment += payment.getDouble("amount");
                        paymentMethods.add(payment.getInteger("payment_method_id"));
                        if (payment.getInteger("payment_method_id") == 1 && body.getInteger(CASHOUT_ID) == null) {
                            throw new Exception("Employee needs to have an opened cash out");
                        }
                    }

                    if ((payments == null || payments.isEmpty()) && !payWithCredit)
                        throw new Exception("Payments: not found");

                    if(payWithCredit && servicesTotalAmount > creditBalance && payments.size() == 1)
                        throw new Exception("The payment " + servicesTotalAmount + " is greater than the credit balance " + creditBalance);

                    if(totalPayment > totalDebt && paymentMethods.contains(1))
                        throw new Exception("The payment " + totalPayment + " is greater than the total debt " + totalDebt);

                    Boolean hasCreditBalance = false;
                    Double paymentDifference = 0.0;
                    if(totalPayment < servicesTotalAmount){
                        throw new Exception("The payment " + servicesTotalAmount + " is greater than the total payment " + totalPayment);
                    }
                    if(totalPayment > servicesTotalAmount) {
                        hasCreditBalance = true;
                        paymentDifference = totalPayment - servicesTotalAmount;
                    }
                    else if(totalPayment > totalDebt){
                        hasCreditBalance = true;
                        paymentDifference = totalPayment - totalDebt;
                    }

                    body.put("has_credit_balance", hasCreditBalance)
                            .put("payment_difference", paymentDifference)
                            .put("services_total_amount", servicesTotalAmount);

                    eventBus.send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Saved");
                            }

                        } catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, t);
                        }
                    });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    responseError(context, ex);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, t);
        }
    }

    private void register(RoutingContext context) {
        EventBus eventBus = vertx.eventBus();
        JsonObject body = context.getBodyAsJson();
        JsonArray validServices = new JsonArray().add("boarding_pass").add("parcel").add("guiapp").add("prepaid");
        try {
            isGraterAndNotNull(body, CUSTOMER_ID, 0);

            int userId = context.get(USER_ID);
            int customerId = body.getInteger(CUSTOMER_ID);
            Boolean payWithCredit = body.containsKey("pay_with_credit") ? body.getBoolean("pay_with_credit") : Boolean.valueOf(false);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, DebtPaymentDBV.ACTION_REGISTER);
            JsonObject customer = new JsonObject().put(CUSTOMER_ID, customerId);

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            eventBus.send(CustomerDBV.class.getSimpleName(), customer.put("services", validServices), new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_DEBTS), f1.completer());
            eventBus.send(CashOutDBV.class.getSimpleName(), new JsonObject().put(USER_ID, userId), new DeliveryOptions().addHeader(ACTION, CashOutDBV.ACTION_GET_CASH_OUT_EMPLOYEE_BY_ID), f2.completer());
            eventBus.send(CustomerDBV.class.getSimpleName(), customer, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f3.completer());
            CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
                try {
                    if (detailReply.failed()) {
                        throw new Exception(detailReply.cause());
                    }
                    Message<JsonObject> debtsMsg = detailReply.result().resultAt(0);
                    Message<JsonObject> cashOutMsg = detailReply.result().resultAt(1);
                    Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(2);
                    JsonObject debts = debtsMsg.body();
                    JsonObject cashOut = cashOutMsg.body();
                    JsonObject customerCreditData = customerCreditDataMsg.body();

                    body.put("debts", debts)
                            .put(CREATED_BY, userId)
                            .put(CASHOUT_ID, cashOut.getInteger(ID))
                            .put(CASH_REGISTER_ID, cashOut.getInteger(CASH_REGISTER_ID))
                            .put("customer_credit_data", customerCreditData);

                    JsonArray payments = body.getJsonArray("payments");
                    JsonObject cashChange = body.getJsonObject("cash_change");
                    JsonArray services = body.getJsonArray("services");
                    Double totalDebt = customerCreditData.getDouble("total_debt");
                    Double creditBalance = customerCreditData.getDouble("credit_balance");

                    Double servicesTotalAmount = 0.0;
                    for(int i = 0; i < services.size(); i++){
                        JsonObject serviceObject = services.getJsonObject(i);
                        String service = serviceObject.getString("service");
                        Double serviceDebt = customerCreditData.getDouble(serviceObject.getString("service").concat("_debt"));
                        Double serviceAmount = serviceObject.getDouble("amount");
                        servicesTotalAmount += serviceAmount;
                        serviceObject.put("debt", serviceDebt);
                        if(!validServices.contains(serviceObject.getString("service")))
                            throw new Exception("Service: not valid");
                        if(serviceAmount > serviceDebt)
                            throw new Exception(service.concat(": The payment " + serviceAmount + " is greater than the debt " + serviceDebt));
                    }


                    Double totalPayment = 0.0;
                    JsonArray paymentMethods = new JsonArray();
                    if(payWithCredit){
                        if(payments == null){
                            JsonObject payment = new JsonObject()
                                    .put("payment_method_id", 0)
                                    .put("amount", servicesTotalAmount);
                            payments = new JsonArray().add(payment);
                            body.put("payments", payments);
                        }
                        else{
                            Double cashPaid = cashChange.getDouble("paid");
                            Double cashTotal = cashChange.getDouble("total");
                            Double totalPayCredit = servicesTotalAmount - cashTotal;
                            JsonObject payment = new JsonObject()
                                    .put("payment_method_id", 0)
                                    .put("amount", totalPayCredit);
                            payments.add(payment);
                            cashChange.put("paid", cashPaid + payment.getDouble("amount"))
                                    .put("total", cashTotal + payment.getDouble("amount"));
                            if(totalPayCredit > creditBalance)
                                throw new Exception("The credit payment " + totalPayCredit + " is greater than the credit balance " + creditBalance);
                        }
                    }

                    for (Object p : payments) {
                        JsonObject payment = new JsonObject(String.valueOf(p));
                        totalPayment += payment.getDouble("amount");
                        paymentMethods.add(payment.getInteger("payment_method_id"));
                        if (payment.getInteger("payment_method_id") == 1 && body.getInteger(CASHOUT_ID) == null) {
                            throw new Exception("Employee needs to have an opened cash out");
                        }
                    }

                    if ((payments == null || payments.isEmpty()) && !payWithCredit)
                        throw new Exception("Payments: not found");

                    if(payWithCredit && servicesTotalAmount > creditBalance && payments.size() == 1)
                        throw new Exception("The payment " + servicesTotalAmount + " is greater than the credit balance " + creditBalance);

                    if(totalPayment > totalDebt && paymentMethods.contains(1))
                        throw new Exception("The payment " + totalPayment + " is greater than the total debt " + totalDebt);

                    Boolean hasCreditBalance = false;
                    Double paymentDifference = 0.0;
                    if(totalPayment > servicesTotalAmount) {
                        hasCreditBalance = true;
                        paymentDifference = totalPayment - servicesTotalAmount;
                    }
                    else if(totalPayment > totalDebt){
                        hasCreditBalance = true;
                        paymentDifference = totalPayment - totalDebt;
                    }

                    body.put("has_credit_balance", hasCreditBalance)
                            .put("payment_difference", paymentDifference)
                            .put("services_total_amount", servicesTotalAmount);

                    eventBus.send(this.getDBAddress(), body, options, reply -> {
                        try {
                            if (reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                            } else {
                                responseOk(context, reply.result().body(), "Saved");
                            }

                        } catch (Throwable t) {
                            t.printStackTrace();
                            responseError(context, t);
                        }
                    });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    responseError(context, ex);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            responseError(context, t);
        }
    }

    @Override
    protected String getEndpointAddress() {
        return "/debtPayments";
    }
}
