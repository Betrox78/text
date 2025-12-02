/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.jobs;

import database.jobs.JobDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import static service.commons.Constants.ACTION;
import static service.commons.Constants.UNEXPECTED_ERROR;
import service.commons.ServiceVerticle;
import utils.UtilsJWT;
import utils.UtilsResponse;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseInvalidToken;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class JobSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return JobDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/jobs";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.router.get("/:id/withRequirements").handler(this::reportWithRequirements);
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isName(body, "name");
            isEmpty(body, "description");
            isBoolean(body, "is_group");
            isBoolean(body, "is_recurrent");
            isBoolean(body, "is_required");
            Float salary = body.getFloat("salary");
            if (salary != null) {
                if (salary < 1f) {
                    throw new PropertyValueException("salary", INVALID_FORMAT);
                }
            }
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isNameAndNotNull(body, "name");
            isEmpty(body, "description");
            if (body.getFloat("salary", 0f) < 1f) {
                throw new PropertyValueException("salary", INVALID_FORMAT);
            }
        } catch (PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }

    private void reportWithRequirements(RoutingContext context) {
        String jwt = context.request().getHeader("Authorization");
        if (UtilsJWT.isTokenValid(jwt)) {
            JsonObject message = new JsonObject().put("id", Integer.valueOf(context.request().getParam("id")));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, JobDBV.JOB_WITH_REQ);
            vertx.eventBus().send(JobDBV.class.getSimpleName(), message, options, reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body(), "Found");


                }catch(Exception e){
                    responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());

                }

            });
        } else {
            responseInvalidToken(context);
        }
    }

}
