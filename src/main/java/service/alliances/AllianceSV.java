package service.alliances;

import database.alliances.AllianceDBV;
import database.commons.ErrorCodes;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import service.commons.Constants;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static database.alliances.AllianceDBV.FIND_BY_ID_WITH_DETAILS;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static database.alliances.AllianceDBV.*;

public class AllianceSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return AllianceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/alliances";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/web", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllForWeb);
        this.addHandler(HttpMethod.GET, "/cities", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findCitiesForWeb);
        this.addHandler(HttpMethod.GET, "/states", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findStatesForWeb);
        this.addHandler(HttpMethod.GET, "/:id", AuthMiddleware.getInstance(), this::findByIdWithDetails);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(AllianceDBV.REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Created");
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, ex.getMessage());
                }
            });
        }

    }

    private void findAllForWeb(RoutingContext context) {
        JsonObject message = new JsonObject();
        MultiMap params = context.request().params();
        for (Map.Entry<String, String> entry : params.entries()) {
            message.put(entry.getKey(), entry.getValue());
        }
        vertx.eventBus().send(this.getDBAddress(), message,
                options(FIND_ALL_FOR_WEB), reply ->
                {
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        this.genericResponse(context, reply, "Found");
                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                });
    }

    private void findStatesForWeb(RoutingContext context) {
        JsonObject message = new JsonObject();
        MultiMap params = context.request().params();
        for (Map.Entry<String, String> entry : params.entries()) {
            message.put(entry.getKey(), entry.getValue());
        }
        vertx.eventBus().send(this.getDBAddress(), message,
                options(FIND_STATE_LIST_FOR_WEB), reply ->{
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }

                        this.genericResponse(context, reply, "Found");
                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }

                });
    }

    private void findCitiesForWeb(RoutingContext context) {
        JsonObject message = new JsonObject();
        MultiMap params = context.request().params();
        for (Map.Entry<String, String> entry : params.entries()) {
            message.put(entry.getKey(), entry.getValue());
        }
        vertx.eventBus().send(this.getDBAddress(), message,
                options(FIND_CITY_BY_STATE_LIST_FOR_WEB), reply ->
                {
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }

                        this.genericResponse(context, reply, "Found");
                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                });
    }

    private void findByIdWithDetails(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put(Constants.ID, Integer.valueOf(context.request().getParam(Constants.ID)));
        vertx.eventBus().send(this.getDBAddress(), message,
                options(FIND_BY_ID_WITH_DETAILS), reply ->
                {
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }

                        this.genericResponse(context, reply, "Found");
                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                });
    }

    /**
     * Verifies is the data of the request is valid to create a record of this entity
     *
     * @param context context of the request
     * @return true if the data is valid, false othrewise
     */
    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        if (!body.containsKey(CITIES)) {
            errors.add(new PropertyError(CITIES, UtilsValidation.MISSING_REQUIRED_VALUE));
        } else {
            try {
                JsonArray cities = body.getJsonArray(CITIES);
                if (cities == null) {
                    errors.add(new PropertyError(CITIES, UtilsValidation.INVALID_FORMAT));
                }
            } catch (ClassCastException e) {
                errors.add(new PropertyError(CITIES, UtilsValidation.INVALID_FORMAT));
            }
        }
        if (!body.containsKey(SERVICES)) {
            errors.add(new PropertyError(SERVICES, UtilsValidation.MISSING_REQUIRED_VALUE));
        } else {
            try {
                JsonArray cities = body.getJsonArray(SERVICES);
                if (cities == null) {
                    errors.add(new PropertyError(SERVICES, UtilsValidation.INVALID_FORMAT));
                }
            } catch (ClassCastException e) {
                errors.add(new PropertyError(SERVICES, UtilsValidation.INVALID_FORMAT));
            }
        }
        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidCreateData(context);
    }
}
