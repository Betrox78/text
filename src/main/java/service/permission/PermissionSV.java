/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.permission;

import database.permission.PermissionDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PermissionMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;

import static database.permission.PermissionDBV.*;
import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.responseWarning;

/**
 *
 * Kriblet
 */
public class PermissionSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PermissionDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/permissions";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/profile/:id", AuthMiddleware.getInstance(), this::profilePermissions);
        this.addHandler(HttpMethod.GET, "/user/:id", AuthMiddleware.getInstance(), this::userPermissions);
        this.addHandler(HttpMethod.GET, "/userPlusProfiles/:id", AuthMiddleware.getInstance(), this::userPermissionsPlusProfile);
        this.addHandler(HttpMethod.POST, "/assign/user", AuthMiddleware.getInstance(), this::assignUserPermissions);
        this.addHandler(HttpMethod.POST, "/assign/profile", AuthMiddleware.getInstance(), this::assingProfilePermissions);
        this.addHandler(HttpMethod.GET, "/profile/detail/all-modules/:profile_id", AuthMiddleware.getInstance(), this::getDetailProfileAllModules);
        this.addHandler(HttpMethod.GET, "/profile/detail/:profile_id", AuthMiddleware.getInstance(), this::getDetailProfile);
        this.addHandler(HttpMethod.GET, "/profile/detail/:profile_id/:user_id", AuthMiddleware.getInstance(), this::getDetailProfile);
        this.addHandler(HttpMethod.POST, "/permissionServices", AuthMiddleware.getInstance(), this::setPermissionServices);
        super.start(startFuture);

    }

    private void profilePermissions(RoutingContext context) {
        try {
            Integer profileId = Integer.parseInt(context.request().getParam("id"));
            JsonObject send = new JsonObject()
                    .put("id", profileId);

            this.vertx.eventBus().send(
                    getDBAddress(),
                    send,
                    options(PermissionDBV.ACTION_PROFILE_PERMISSIONS),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        } catch (Exception e) {
            UtilsResponse.responsePropertyValue(
                    context,
                    new UtilsValidation.PropertyValueException("id", UtilsValidation.MISSING_REQUIRED_VALUE));
        }
    }

    private void userPermissions(RoutingContext context) {
        try {
            Integer userId = Integer.parseInt(context.request().getParam("id"));
            JsonObject send = new JsonObject()
                    .put("id", userId);
            this.vertx.eventBus().send(
                    getDBAddress(),
                    send,
                    options(PermissionDBV.ACTION_USER_PERMISSIONS),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        } catch (Exception e) {
            UtilsResponse.responsePropertyValue(
                    context,
                    new UtilsValidation.PropertyValueException("id", UtilsValidation.MISSING_REQUIRED_VALUE));
        }
    }

    private void userPermissionsPlusProfile(RoutingContext context) {
        try {
            Integer userId = Integer.parseInt(context.request().getParam("id"));
            JsonObject send = new JsonObject()
                    .put("id", userId);
            this.vertx.eventBus().send(
                    getDBAddress(),
                    send,
                    options(PermissionDBV.ACTION_USER_PLUS_PROFILES_PERMISSIONS),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        } catch (Exception e) {
            UtilsResponse.responsePropertyValue(
                    context,
                    new UtilsValidation.PropertyValueException("id", UtilsValidation.MISSING_REQUIRED_VALUE));
        }
    }

    private void assignUserPermissions(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            UtilsValidation.isGraterAndNotNull(body, "user_id", 0);
            UtilsValidation.isEmptyAndNotNull(body.getJsonArray("permissions"), "permissions");
            this.vertx.eventBus().send(
                    getDBAddress(),
                    body,
                    options(PermissionDBV.ACTION_ASSIGN_USER_PERMISSIONS),
                    reply -> {
                        this.genericResponse(context, reply, "Permissions assigend");
                    });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void assingProfilePermissions(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            UtilsValidation.isGraterAndNotNull(body, "profile_id", 0);
            UtilsValidation.isEmptyAndNotNull(body.getJsonArray("permissions"), "permissions");
            this.vertx.eventBus().send(
                    getDBAddress(),
                    body,
                    options(PermissionDBV.ACTION_ASSIGN_PROFILE_PERMISSIONS),
                    reply -> {
                        this.genericResponse(context, reply, "Permissions assigend");
                    });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getDetailProfileAllModules(RoutingContext context) {
        try {
            Integer profileId = Integer.parseInt(context.request().getParam("profile_id"));
            JsonObject body = new JsonObject()
                    .put("profile_id", profileId)
                    .put("all_modules", true);
            UtilsValidation.isGraterAndNotNull(body, "profile_id", 0);

            this.vertx.eventBus().send(
                    getDBAddress(),
                    body,
                    options(PermissionDBV.ACTION_PROFILE_DETAIL),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void getDetailProfile(RoutingContext context) {
        try {
            Integer profileId = Integer.parseInt(context.request().getParam("profile_id"));
            String assignedTo = context.request().getParam("assigned_to");
            JsonObject body = new JsonObject()
                    .put("profile_id", profileId)
                    .put("all_modules", false);
            if(assignedTo != null){
                body.put("assigned_to", Integer.parseInt(assignedTo));
            }
            UtilsValidation.isGraterAndNotNull(body, "profile_id", 0);
            if(context.request().getParam("user_id") != null){
                Integer userId = Integer.parseInt(context.request().getParam("user_id"));
                body.put("user_id", userId);
                UtilsValidation.isGraterAndNotNull(body, "user_id", 0);
            }

            this.vertx.eventBus().send(
                    getDBAddress(),
                    body,
                    options(PermissionDBV.ACTION_PROFILE_DETAIL),
                    reply -> {
                        this.genericResponse(context, reply);
                    });
        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }
    }

    private void setPermissionServices(RoutingContext context) {
        if(this.isValidCreatePermissionServices(context)){
            JsonObject body = context.getBodyAsJson();

            this.vertx.eventBus().send(getDBAddress(), body, options(PermissionDBV.ACTION_SET_PERMISSION_SERVICES), reply -> {
                this.genericResponse(context, reply, "Services assigned to permission");
            });
        }
    }

    private boolean isValidCreatePermissionServices(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        if (!body.containsKey(PERMISSION_ID)) {
            errors.add(new PropertyError(PERMISSION_ID, UtilsValidation.MISSING_REQUIRED_VALUE));
        }
        if (!body.containsKey(SERVICES)) {
            errors.add(new PropertyError(SERVICES, UtilsValidation.MISSING_REQUIRED_VALUE));
        } else {
            try {
                JsonArray services = body.getJsonArray(SERVICES);
                if (services == null) {
                    errors.add(new PropertyError(SERVICES, UtilsValidation.INVALID_FORMAT));
                } else {
                    for (int i=0; i<services.size(); i++){
                        JsonObject service = services.getJsonObject(i);
                        if (!service.containsKey(AUTH_SERVICE_ID)) {
                            errors.add(new PropertyError(AUTH_SERVICE_ID, UtilsValidation.MISSING_REQUIRED_VALUE));
                        }
                        if (!service.containsKey(HTTP_METHOD)) {
                            service.put(HTTP_METHOD, "GET");
                            context.setBody(body.toBuffer());
                        }
                    }
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
