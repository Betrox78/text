package service.sellers;

import database.sellers.SellersDBV;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import io.vertx.core.http.HttpMethod;

import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsValidation.*;

public class SellersSV extends ServiceVerticle {

@Override
protected String getDBAddress() {
        return SellersDBV.class.getSimpleName();
        }

@Override
protected String getEndpointAddress() {
        return "/sellers";
        }

@Override
public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/getList", AuthMiddleware.getInstance(), this::getList);
        super.start(startFuture);
        }

public void getList(RoutingContext context) {
        try {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(SellersDBV.class.getSimpleName(), body, options(SellersDBV.ACTION_GET_SELLERS_LIST), reply -> {
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
        } catch (Exception e){
        responseError(context, e);
        }
        }


@Override
protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
        isNameAndNotNull(body, "name");
        isNameAndNotNull(body, "last_name");
        isGraterAndNotNull(body, "branchoffice_id", 0);
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
        isNameAndNotNull(body, "last_name");
        isGraterAndNotNull(body, "branchoffice_id", 0);
        } catch (PropertyValueException ex) {
        return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
        }
        }
