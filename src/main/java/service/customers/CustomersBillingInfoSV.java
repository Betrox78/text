/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.customers;

import database.commons.ErrorCodes;
import database.customers.CustomerDBV;
import database.customers.CustomersBillingInfoDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.customers.CustomersBillingInfoDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author ulises
 */
public class CustomersBillingInfoSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CustomersBillingInfoDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/customersBillingInfo";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/rfc/:rfc", AuthMiddleware.getInstance(), this::getByRFC);
        this.addHandler(HttpMethod.POST, "/assign", AuthMiddleware.getInstance(), this::assignToCustomer);
        this.addHandler(HttpMethod.GET, "/getUsoCFDI", this::getUsoCFDI);
        this.addHandler(HttpMethod.GET, "/getRegimenFiscal", this::getRegimenFiscal);
        this.addHandler(HttpMethod.GET, "/searchByNameRFC/:search", AuthMiddleware.getInstance(), this::searchByNameRFC);
        this.addHandler(HttpMethod.DELETE, "/:customer_id/:customer_billing_information_id", this::removeRelation);
        super.start(startFuture);
    }

    private void getByRFC(RoutingContext context) {
        try {
            JsonObject body = new JsonObject()
                    .put(_RFC, context.request().getParam(_RFC));
            isEmptyAndNotNull(body, _RFC);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GET_BY_RFC);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, "Found", reply.result().body());


                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (UtilsValidation.PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void assignToCustomer(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, _CUSTOMER_ID, 0);
            isGraterAndNotNull(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, ASSIGN_TO_CUSTOMER);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Assigned");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e);
                }

            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e);
        }
    }

    private void getUsoCFDI(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GET_USO_CFDI);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, "Found", reply.result().body());


                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void getRegimenFiscal(RoutingContext context) {
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GET_REGIMEN_FISCAL);
            vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, "Found", reply.result().body());


                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void removeRelation(RoutingContext context) {
        try {

            JsonObject body = new JsonObject()
                    .put(_CUSTOMER_ID, Integer.parseInt(context.request().getParam(_CUSTOMER_ID)))
                    .put(_CUSTOMER_BILLING_INFORMATION_ID, Integer.parseInt(context.request().getParam(_CUSTOMER_BILLING_INFORMATION_ID)));
            isGraterAndNotNull(body, _CUSTOMER_ID, 0);
            isGraterAndNotNull(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_REMOVE_RELATION);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, "Deleted", reply.result().body());
                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (PropertyValueException t) {
            responsePropertyValue(context, t);
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void searchByNameRFC(RoutingContext context) {
        int limit = 0;
        String param = context.request().getParam("search") != null ? context.request().getParam("search") : "";
        try {
            limit = context.request().getParam("limit") != null ? Integer.parseInt(context.request().getParam("limit")) : 0;
        } catch (Exception ignored) { }
        JsonObject searchTerm = new JsonObject().put("search", param).put("limit", limit);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_SEARCH_BY_NAME_AND_RFC);
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

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmpty(body, "name");
            isEmpty(body, "rfc");
            isEmpty(body, "address");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body, "name");
            isEmptyAndNotNull(body, "rfc");
            isEmptyAndNotNull(body, "address");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

}
