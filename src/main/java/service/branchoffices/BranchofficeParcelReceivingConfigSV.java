/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.branchoffices;

import database.branchoffices.BranchofficeParcelReceivingConfigDBV;
import database.commons.ErrorCodes;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.branchoffices.BranchofficeParcelReceivingConfigDBV.ACTION_GET_LIST;
import static database.branchoffices.BranchofficeParcelReceivingConfigDBV.ACTION_GET_MISSING_LIST;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseError;
import static utils.UtilsValidation.isGraterAndNotNull;

/**
 *
 * @author AllAbordo
 */
public class BranchofficeParcelReceivingConfigSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BranchofficeParcelReceivingConfigDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/branchofficeParcelReceivingConfig";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/list/:branchoffice_id", AuthMiddleware.getInstance(), this::getList);
        this.addHandler(HttpMethod.GET, "/missing/list/:branchoffice_id", AuthMiddleware.getInstance(), this::getMissingList);
        super.start(startFuture);
    }



    private void getList(RoutingContext context){
        HttpServerRequest request = context.request();
        Integer branchofficeId = Integer.parseInt(request.getParam(BRANCHOFFICE_ID));
        JsonObject body = new JsonObject().put(BRANCHOFFICE_ID, branchofficeId);
        try {
            isGraterAndNotNull(body, BRANCHOFFICE_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_GET_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
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
        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getMissingList(RoutingContext context){
        HttpServerRequest request = context.request();
        Integer branchofficeId = Integer.parseInt(request.getParam(BRANCHOFFICE_ID));
        JsonObject body = new JsonObject().put(BRANCHOFFICE_ID, branchofficeId);
        try {
            isGraterAndNotNull(body, BRANCHOFFICE_ID, 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_GET_MISSING_LIST), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
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
        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

}
