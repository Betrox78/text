package service.admindashboard;

import database.admindashboard.AdminDashboardDBV;
import database.conekta.conektaDBV;
import database.prepaid.PrepaidTravelDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.CashOutMiddleware;
import service.commons.middlewares.CreditMiddleware;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;

public class AdminDashboardSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() { return AdminDashboardDBV.class.getSimpleName(); }

    @Override
    protected String getEndpointAddress() {
        return "/adminDashboard";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/accumulatedServicesReport", AuthMiddleware.getInstance(), this::accumulatedServicesReport);
        super.start(startFuture);
    }

    private void accumulatedServicesReport(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            List<Future> futures = new ArrayList<>();
            Future<Message<JsonArray>> fCouriersTotals = Future.future();
            Future<Message<JsonArray>> fParcelsTotals = Future.future();
            Future<Message<JsonArray>> fParcelsIMSSTotals = Future.future();
            Future<Message<JsonArray>> fParcelFXCpaidTotals = Future.future();
            Future<Message<JsonArray>> fGuiasPPTotals = Future.future();
            Future<Message<JsonArray>> fCashPayments = Future.future();
            Future<Message<JsonArray>> fTransferCheckPayments = Future.future();
            Future<Message<JsonArray>> fcardsPayments = Future.future();
            Future<Message<JsonArray>> fConektaPayments = Future.future();
            Future<Message<JsonArray>> fParcelFXCSoldTotals = Future.future();
            Future<Message<JsonArray>> fParcelCreditSoldTotals = Future.future();

            JsonObject bodyCourier = body.copy()
                    .put("type", "courier")
                    .put("ommit_customer_company_nicknames", true)
                    .put("customer_company_nick_name", AdminDashboardDBV.CUSTOMERS_NICK_NAMES);
            JsonObject bodyParcel = body.copy()
                    .put("type", "parcel")
                    .put("ommit_customer_company_nicknames", true)
                    .put("customer_company_nick_name", AdminDashboardDBV.CUSTOMERS_NICK_NAMES);
            JsonObject bodyParcelIMSS = body.copy()
                    .put("ommit_customer_company_nicknames", false)
                    .put("customer_company_nick_name", "IMSS");

            this.vertx.eventBus().send(this.getDBAddress(), bodyCourier,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_PARCEL), fCouriersTotals.completer());
            this.vertx.eventBus().send(this.getDBAddress(), bodyParcel,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_PARCEL), fParcelsTotals.completer());
            this.vertx.eventBus().send(this.getDBAddress(), bodyParcelIMSS,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_PARCEL), fParcelsIMSSTotals.completer());
            this.vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_FXC_PAID), fParcelFXCpaidTotals.completer());
            this.vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_GUIAS_PP), fGuiasPPTotals.completer());

            JsonObject bodyCash = body.copy()
                    .put("payment_methods", new JsonArray().add("cash").add("deposit").add("oxxo").add("codi"))
                    .put("requieres_extra_conditions", false);
            JsonObject bodyTransferCheck = body.copy()
                    .put("payment_methods", new JsonArray().add("transfer").add("check"))
                    .put("requieres_extra_conditions", false);
            JsonObject bodyCards = body.copy()
                    .put("payment_methods", new JsonArray().add("card").add("debit"))
                    .put("requieres_extra_conditions", true)
                    .put("payment_method_group", "cards");
            JsonObject bodyConekta = body.copy()
                    .put("payment_methods", new JsonArray().add("card").add("debit"))
                    .put("requieres_extra_conditions", true)
                    .put("payment_method_group", "conekta");

            this.vertx.eventBus().send(this.getDBAddress(), bodyCash,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_PAYMENT_TOTALS), fCashPayments.completer());
            this.vertx.eventBus().send(this.getDBAddress(), bodyTransferCheck,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_PAYMENT_TOTALS), fTransferCheckPayments.completer());
            this.vertx.eventBus().send(this.getDBAddress(), bodyCards,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_PAYMENT_TOTALS), fcardsPayments.completer());
            this.vertx.eventBus().send(this.getDBAddress(), bodyConekta,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_PAYMENT_TOTALS), fConektaPayments.completer());
            this.vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_FXC_SALES), fParcelFXCSoldTotals.completer());
            this.vertx.eventBus().send(this.getDBAddress(), body,
                    new DeliveryOptions().addHeader(ACTION, AdminDashboardDBV.ACTION_GET_SERVICES_TOTALS_PARCEL_CREDIT), fParcelCreditSoldTotals.completer());

            futures.add(fCouriersTotals);
            futures.add(fParcelsTotals);
            futures.add(fParcelsIMSSTotals);
            futures.add(fParcelFXCpaidTotals);
            futures.add(fGuiasPPTotals);
            // payment methods calls
            futures.add(fCashPayments);
            futures.add(fTransferCheckPayments);
            futures.add(fcardsPayments);
            futures.add(fConektaPayments);
            futures.add(fParcelFXCSoldTotals);
            futures.add(fParcelCreditSoldTotals);

            CompositeFuture.all(futures).setHandler(reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    // sales totals
                    Message<JsonArray> msgCourierTotals = reply.result().resultAt(0);
                    Message<JsonArray> msgParcelTotals = reply.result().resultAt(1);
                    Message<JsonArray> msgParcelTotalsIMSS = reply.result().resultAt(2);
                    Message<JsonArray> msgParcelFXCPaid = reply.result().resultAt(3);
                    Message<JsonArray> msgGuiasPPTotals = reply.result().resultAt(4);
                    // payment methods
                    Message<JsonArray> msgCashPayments = reply.result().resultAt(5);
                    Message<JsonArray> msgTransferCheckPayments = reply.result().resultAt(6);
                    Message<JsonArray> msgCardsPayments = reply.result().resultAt(7);
                    Message<JsonArray> msgConektaPayments = reply.result().resultAt(8);
                    Message<JsonArray> msgParcelFXCSoldTotals = reply.result().resultAt(9);
                    Message<JsonArray> msgParcelCreditSoldTotals = reply.result().resultAt(10);

                    JsonArray courierTotals = msgCourierTotals.body();
                    JsonArray parcelTotals = msgParcelTotals.body();
                    JsonArray parcelTotalsIMSS = msgParcelTotalsIMSS.body();
                    JsonArray parcelFXCpaid = msgParcelFXCPaid.body();
                    JsonArray guiasPpTotals = msgGuiasPPTotals.body();
                    //payments methods
                    JsonArray cashPayments = msgCashPayments.body();
                    JsonArray transferCheckPayments = msgTransferCheckPayments.body();
                    JsonArray cardsPayments = msgCardsPayments.body();
                    JsonArray conektaPayments = msgConektaPayments.body();
                    JsonArray parcelFXCSoldTotals = msgParcelFXCSoldTotals.body();
                    JsonArray parcelCreditSoldTotals = msgParcelCreditSoldTotals.body();

                    JsonObject responseObj = new JsonObject()
                            .put("courier_totals", courierTotals)
                            .put("parcel_totals", parcelTotals)
                            .put("parcel_totals_IMSS", parcelTotalsIMSS)
                            .put("parcel_fxc_paid", parcelFXCpaid)
                            .put("guias_pp_totals", guiasPpTotals)

                            .put("cash_payments", cashPayments)
                            .put("transfer_check_payments", transferCheckPayments)
                            .put("cards_payments", cardsPayments)
                            .put("conekta_payments", conektaPayments)
                            .put("parcel_fxc_sold_totals", parcelFXCSoldTotals)
                            .put("parcel_credit_sold_totals", parcelCreditSoldTotals);

                    responseOk(context, responseObj, "Found");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, ex.getMessage());
                }
            });
        } catch (Exception e) {
            responseError(context, e);
        }
    }
}
