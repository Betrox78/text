package service.authorizationcodes;

import database.authorizationcodes.AuthorizationCodesDBV;
import database.commons.ErrorCodes;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static database.authorizationcodes.AuthorizationCodesDBV.*;

public class AuthorizationCodesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() { return AuthorizationCodesDBV.class.getSimpleName(); }

    @Override
    protected String getEndpointAddress() {
        return "/authorizationCodes";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/validate", AuthMiddleware.getInstance(), this::validate);
        this.addHandler(HttpMethod.GET, "/v2", AuthMiddleware.getInstance(), this::findAllV2);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(AuthorizationCodesDBV.REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
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

    private void validate(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(UPDATED_BY, context.<Integer>get(USER_ID));
        body.put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
        if (this.isValidValidateData(context)) {
            vertx.eventBus().send(this.getDBAddress(), body, options(AuthorizationCodesDBV.VALIDATE), (AsyncResult<Message<JsonObject>> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Validated");
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    responseError(context, ex.getMessage());
                }
            });
        }
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
        if (!body.containsKey(SCH_ROUTE_DEST_ID)) {
            errors.add(new PropertyError(SCH_ROUTE_DEST_ID, UtilsValidation.MISSING_REQUIRED_VALUE));
        }
        if (!body.containsKey(AUTHORIZATED_BY)) {
            errors.add(new PropertyError(AUTHORIZATED_BY, UtilsValidation.MISSING_REQUIRED_VALUE));
        }
        if (!body.containsKey(CONTENT)) {
            errors.add(new PropertyError(CONTENT, UtilsValidation.MISSING_REQUIRED_VALUE));
        }
        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidCreateData(context);
    }

    protected boolean isValidValidateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (!body.containsKey(CODE)) {
            responseWarning(context, INVALID_DATA);
            return false;
        }
        return super.isValidCreateData(context);
    }
}
