/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.suppliers;

import database.commons.ErrorCodes;
import database.suppliers.SupplierDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import static service.commons.Constants.ACTION;
import static service.commons.Constants.CREATED_BY;
import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static service.commons.Constants.UNEXPECTED_ERROR;
import service.commons.ServiceVerticle;
import utils.UtilsJWT;
import utils.UtilsResponse;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseInvalidToken;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author ulises
 */
public class SupplierSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return SupplierDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/suppliers";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.router.post("/register").handler(BodyHandler.create());
        this.router.post("/register").handler(this::register);
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isPhoneNumber(body, "phone");
            isMail(body, "email");
            isBoolean(body, "has_credit");
            isBoolean(body, "has_discounts");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isPhoneNumber(body, "phone");
            if (body.getString("email") != null) {
                    if (body.getString("email").isEmpty()) {
                        body.putNull("email");
                        context.setBody(body.toBuffer());
                    }else{
                        isMail(body, "email");
                    }
                }  
            isBoolean(body, "has_credit");
            isBoolean(body, "has_discounts");
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

    private void register(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject body = context.getBodyAsJson();
            try {
                int userId = UtilsJWT.getUserIdFrom(jwt);
                isPhoneNumber(body, "phone");
                if (body.getString("email") != null) {
                    if (body.getString("email").isEmpty()) {
                        body.putNull("email");
                    }else{
                        isMail(body, "email");
                    }
                }                
                isBoolean(body, "has_credit");
                isBoolean(body, "has_discounts");
                body.put(CREATED_BY, userId);
                //contact info
                JsonObject contactInfo = body.getJsonObject("contact_info");
                if (contactInfo != null) {
                    isNameAndNotNull(contactInfo, "name", "contact_info");
                    isNameAndNotNull(contactInfo, "last_name", "contact_info");
                    isPhoneNumberAndNotNull(contactInfo, "phone", "contact_info");
                    isMail(contactInfo, "email", "contact_info");
                    contactInfo.put(CREATED_BY, userId);
                }
                //bank info
                JsonObject bankInfo = body.getJsonObject("bank_info");
                if (bankInfo != null) {
                    isEmpty(bankInfo, "bank", "bank_info");
                    isEmptyAndNotNull(bankInfo, "account", "bank_info");
                    isEmpty(bankInfo, "clabe", "bank_info");
                    isEmpty(bankInfo, "reference", "bank_info");
                    bankInfo.put(CREATED_BY, userId);
                }
                DeliveryOptions options = new DeliveryOptions()
                        .addHeader(ACTION, SupplierDBV.REGISTER);
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

}
