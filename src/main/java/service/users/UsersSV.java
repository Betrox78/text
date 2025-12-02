/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.users;

import database.commons.ErrorCodes;
import database.users.UsersDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PermissionMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsResponse;
import utils.UtilsSecurity;
import utils.UtilsValidation;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * Kriblet
 */
public class UsersSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return UsersDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/users";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/detail/:id", AuthMiddleware.getInstance(), this::getDetail);

        this.router.get("/profiles/:id").handler(this::profiles);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.GET, "/birthday/:id", PublicRouteMiddleware.getInstance(), this::getBirthday);
        this.addHandler(HttpMethod.GET, "/search/:search_term/:limit", AuthMiddleware.getInstance(), this::searchUser);
        this.addHandler(HttpMethod.GET, "/search/:terminal_id/:search_term/:limit", AuthMiddleware.getInstance(), this::searchUser);
        this.addHandler(HttpMethod.GET, "/getUsersByJobName/:job_name", AuthMiddleware.getInstance(), this::getUsersByJobName);
        this.addHandler(HttpMethod.POST, "/userExists", this::userExists);
        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::delete);
        super.start(startFuture);
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isName(body, "name");
            isMail(body, "email");
            isPhoneNumber(body, "phone");
            body.remove("pass");
            context.setBody(body.toBuffer());
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidUpdateData(context);
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isNameAndNotNull(body, "name");
            isMailAndNotNull(body, "email");
            isPhoneNumber(body, "phone");
            body.put("pass", UtilsSecurity.encodeSHA256(body.getString("pass")));
            context.setBody(body.toBuffer());
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context); //To change body of generated methods, choose Tools | Templates.
    }

    private void profiles(RoutingContext context) {
        this.validateToken(context, __ -> {
                    this.vertx.eventBus().send(
                            this.getDBAddress(),
                            new JsonObject().put("id", Integer.parseInt(context.request().getParam("id"))),
                            new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_PROFILES),
                            r -> {
                                this.genericResponse(context, r);
                            }
                    );
                }
        );
    }

    private void getDetail(RoutingContext context) {
        JsonObject body = new JsonObject();
        body.put("id", Integer.parseInt(context.request().getParam("id")));
            //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
            this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), body, options(UsersDBV.ACTION_DEATIL), reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Report");
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
    }

    private void register(RoutingContext context) {

        if(isValidCreateData(context)){
            this.vertx.eventBus().send( this.getDBAddress(), context.getBodyAsJson(),
                    new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_REGISTER),
                    r -> {
                        this.genericResponse(context, r);
                    }
            );
        }

    }



    private void getBirthday(RoutingContext context) {
        JsonObject body = new JsonObject();
        body.put("id", Integer.parseInt(context.request().getParam("id")));
        //DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DEATIL);
        this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), body, options(UsersDBV.ACTION_BIRTHDAY), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void searchUser(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            int limit = 0;
            Integer terminalId = request.getParam(_TERMINAL_ID) != null ? Integer.parseInt(request.getParam(_TERMINAL_ID)) : null;
            String param = request.getParam("search_term") != null ? request.getParam("search_term") : "";
            try {
                limit = request.getParam("limit") != null ? Integer.parseInt(request.getParam("limit")) : 0;
            } catch (Exception ignored) { }
            JsonObject searchTerm = new JsonObject()
                    .put(_TERMINAL_ID, terminalId)
                    .put("searchTerm", param)
                    .put("limit", limit);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_SEARCH_BY_EMAIL);
            vertx.eventBus().send(this.getDBAddress(), searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }

    private void getUsersByJobName(RoutingContext context) {
        try {
            String param = context.request().getParam("job_name") != null ? context.request().getParam("job_name") : "";
            JsonObject body = new JsonObject().put("job_name", param);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_SEARCH_BY_JOB_NAME);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }

    private void userExists(RoutingContext context) {
        JsonObject body;
        try {
            body = context.getBodyAsJson();
            isMailAndNotNull(body, "email");
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);

            return;
        }

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_USER_EXISTS);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }

    private void delete(RoutingContext context) {
        JsonObject body = new JsonObject();
        try {
            body.put("user_id", Integer.parseInt(context.request().getParam("id")));
            isGrater(body, "user_id", 0);
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);

            return;
        }

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_DELETE_USER);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    responseOk(context, reply.result().body());
                } catch (Throwable t) {
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex);
        }
    }
}
