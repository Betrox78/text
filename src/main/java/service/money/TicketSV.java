/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.commons.ErrorCodes;
import database.configs.GeneralConfigDBV;
import database.money.DebtPaymentDBV;
import database.money.TicketDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.json.HTTP;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;

import static service.commons.Constants.*;

import utils.UtilsValidation;


import static utils.UtilsResponse.*;
import static utils.UtilsValidation.MISSING_REQUIRED_VALUE;

/**
 *
 * @author daliacarlon
 */
public class TicketSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TicketDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/paymentsTicket";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/print/:ticketId", AuthMiddleware.getInstance(), this::printTicketNormal);
        this.addHandler(HttpMethod.GET, "/printRental/:ticketId/:rentalReservationCode", AuthMiddleware.getInstance(), this::printTicketRental);
        this.addHandler(HttpMethod.GET, "/printPrepaid/:ticketId", AuthMiddleware.getInstance(), this::printPrepaidTicket);
        super.start(startFuture);
    }

    private void printTicketRental(RoutingContext context) {
        String rentalReservationCode = context.request()
                .getParam("rentalReservationCode");
        printTicket(context, rentalReservationCode);
    }

    private void printTicketNormal(RoutingContext context) {
        printTicket(context, "0");
    }

    private void printPrepaidTicket(RoutingContext context) {
        try {
            String ticketId = context.request().getParam("ticketId");
            String code = "";
            if(context.request().params().contains("reservation_code")) {
                code = context.request().getParam("reservation_code");
            }

            if(isTicketIdValid(ticketId, context)){
                DeliveryOptions options = new DeliveryOptions();
                JsonObject body = new JsonObject();

                options.addHeader(ACTION, TicketDBV.PRINT_PREPAID);
                body.put("id", Integer.valueOf(ticketId));
                if(!code.isEmpty()) {
                    body.put("reservation_code", code);
                }
                vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                    try {
                        if(reply.succeeded()) {
                            Message<Object> result = reply.result();
                            if(result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                            } else {
                                responseOk(context, result.body(), "Found");
                            }
                        } else {
                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                        }
                    } catch(Exception e) {
                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                    }
                });
            }
        } catch(Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getCause());
        }
   }

    private void printTicket(RoutingContext context, String rentalReservationCode) {
        try {
            Integer userId = context.<Integer>get(USER_ID);
            String ticketId = context.request()
                    .getParam("ticketId");
            if (isTicketIdValid(ticketId, context)) {
                Future<Message<JsonObject>> f1 = Future.future();
                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "tickets_header"),
                        new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
                Future<Message<JsonObject>> f2 = Future.future();
                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                        new JsonObject().put("fieldName", "tickets_footer"), new DeliveryOptions()
                                .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
                Future<Message<JsonObject>> f3 = Future.future();
                vertx.eventBus().send(DebtPaymentDBV.class.getSimpleName(),
                        new JsonObject().put("ticket_id", ticketId), new DeliveryOptions()
                                .addHeader(ACTION, DebtPaymentDBV.ACTION_GET_INI_END_DEBT), f3.completer());

                CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
                    try {
                        if (detailReply.failed()){
                            throw detailReply.cause();
                        }
                        Message<JsonObject> headerMsg = detailReply.result().resultAt(0);
                        Message<JsonObject> footerMsg = detailReply.result().resultAt(1);
                        Message<JsonObject> debtMsg = detailReply.result().resultAt(2);

                        JsonObject header = headerMsg.body();
                        JsonObject footer = footerMsg.body();
                        JsonObject debt = debtMsg.body();

                        this.vertx.eventBus().send(TicketDBV.class.getSimpleName(),
                                new JsonObject().put("ticketId", ticketId)
                                        .put("header", header.getString("value"))
                                        .put("footer", footer.getString("value"))
                                        .put("rentalReservationCode", rentalReservationCode)
                                        .put("updated_by", userId)
                                        .put("debt", debt),
                                options(TicketDBV.PRINT_TICKET),
                                reply -> {
                                    this.genericResponse(context, reply);
                                });
                    } catch (Throwable t){
                        responseError(context, UNEXPECTED_ERROR, t.getCause());
                    }
                });

            }
        } catch (Throwable t){
            responseError(context, UNEXPECTED_ERROR, t.getCause());
        }
    }

    //validate ticket id to print
    private boolean isTicketIdValid(String ticketId,
            RoutingContext context) {
        if (ticketId == null) {
            UtilsResponse.responsePropertyValue(context,
                    new UtilsValidation.PropertyValueException("ticketId",
                            MISSING_REQUIRED_VALUE)
            );
            return false;
        }
        if (ticketId.isEmpty()) {
            UtilsResponse.responsePropertyValue(context,
                    new UtilsValidation.PropertyValueException("ticketId",
                            MISSING_REQUIRED_VALUE)
            );
            return false;
        }

        return true;
    }
}
