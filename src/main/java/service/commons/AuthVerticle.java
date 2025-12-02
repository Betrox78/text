/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.commons;

import database.money.CashOutDBV;
import database.permission.PermissionDBV;
import database.users.UsersDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Objects;
import java.util.Random;

import utils.*;

import static service.commons.Constants.*;
import service.commons.middlewares.AuthMiddleware;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.isPasswordAndNotNull;

/**
 *
 * Kriblet
 */
public class AuthVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.post("/login").handler(BodyHandler.create());
        router.post("/login").handler(this::login);
        router.post("/loginCustomer").handler(BodyHandler.create());
        router.post("/loginCustomer").handler(this::loginCustomer);
        router.post("/loginPartner").handler(BodyHandler.create());
        router.post("/loginPartner").handler(this::loginPartner);
        router.get("/validAccessToken/:token").handler(this::validToken);
        router.post("/refreshToken").handler(BodyHandler.create());
        router.post("/refreshToken").handler(this::refreshToken);
        router.post("/recoverPass").handler(BodyHandler.create());
        router.post("/recoverPass").handler(this::recoverPass);
        router.post("/restorePass").handler(BodyHandler.create());
        router.post("/restorePass").handler(this::restorePass);
        router.post("/resetPass").handler(BodyHandler.create());
        router.post("/resetPass").handler(AuthMiddleware.getInstance());
        router.post("/resetPass").handler(this::resetPass);
        UtilsRouter.getInstance(vertx).mountSubRouter("/auth", router);
        Integer portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT);
        if (portNumber == null) {
            System.out.println("Could not start a HTTP server" + this.getClass().getSimpleName() + ", no port speficied in configuration");
        }
        server.requestHandler(UtilsRouter.getInstance(vertx)::accept)
                .listen(portNumber, ar -> {
                    try {
                        if (ar.failed()){
                            throw ar.cause();
                        }
                        System.out.println(this.getClass().getSimpleName() + " running");
                    } catch (Throwable t){
                        t.printStackTrace();
                        System.out.println("Could not start a HTTP server " + this.getClass().getSimpleName() + ", " + t);
                    }
                });
    }

    private void login(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try{
            String email = body.getString("email");
            String module = body.getString("module");;
            String userAgent = context.request().getHeader("user-agent");
            UtilsValidation.isEmptyAndNotNull(body, "module");
            String pass = UtilsSecurity.encodeSHA256(body.getString("pass"));
            JsonObject send = new JsonObject()
                    .put("user_type", "O")
                    .put("email", email)
                    .put("pass", pass)
                    .put("social_auth", false);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_LOGIN);
            this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), send, options,
                    (AsyncResult<Message<JsonObject>> reply) -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            if (reply.result().body() == null) {
                                throw new Exception("User and/or password are invalid");
                            }
                            JsonObject result = reply.result().body();

                            if (userAgent != null &&
                                    (userAgent.toLowerCase().contains("dalvik") || userAgent.toLowerCase().contains("dart"))) {
                                result.put("accessToken", UtilsJWT.generateAccessTokenInternalApp(result.getInteger("id")))
                                    .put("refreshToken", UtilsJWT.generateRefreshToken(result.getInteger("id")));
                            } else {
                                result.put("accessToken", UtilsJWT.generateAccessToken(result.getInteger("id")))
                                    .put("refreshToken", UtilsJWT.generateRefreshToken(result.getInteger("id")));
                            }

                            if(result.getInteger("profile_id") == null){
                                result.put("menu", new JsonObject());
                                UtilsResponse.responseOk(context, result);
                            } else {
                                JsonObject menuParam = new JsonObject()
                                        .put("profile_id", result.getInteger("profile_id"))
                                        .put("user_id", result.getInteger("id"))
                                        .put("module", module);
                                this.vertx.eventBus().send(PermissionDBV.class.getSimpleName(), menuParam,
                                        new DeliveryOptions().addHeader(ACTION, PermissionDBV.ACTION_GET_MENU_USER), replyMenu -> {
                                            try {
                                                if (replyMenu.failed()){
                                                    throw replyMenu.cause();
                                                }
                                                result.put("permissions", replyMenu.result().body());
                                                Integer employeeId = result.getInteger(EMPLOYEE_ID);
                                                if (employeeId != null) {
                                                    this.vertx.eventBus().send(CashOutDBV.class.getSimpleName(), new JsonObject().put(EMPLOYEE_ID, employeeId),
                                                            new DeliveryOptions().addHeader(ACTION, CashOutDBV.ACTION_GET_OPENED_CASH_OUT_ID), replyCashOout -> {
                                                                try {
                                                                    if (replyCashOout.failed()){
                                                                        throw replyCashOout.cause();
                                                                    }
                                                                    result.put("cash_out_id", replyCashOout.result().body());
                                                                    UtilsResponse.responseOk(context, result);
                                                                } catch (Throwable t){
                                                                    t.printStackTrace();
                                                                    responseError(context, UNEXPECTED_ERROR, t.getMessage());
                                                                }
                                                            });
                                                } else {
                                                    UtilsResponse.responseOk(context, result);
                                                }
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                responseError(context, UNEXPECTED_ERROR, t.getMessage());
                                            }
                                        });
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            responseError(context, UNEXPECTED_ERROR, t.getMessage());
                        }
                    });
        } catch (UtilsValidation.PropertyValueException ex) {
            ex.printStackTrace();
            UtilsResponse.responsePropertyValue(context, ex);
        }

    }

    private void loginCustomer(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            String email = body.getString("email");
            String pass = UtilsSecurity.encodeSHA256(body.getString("pass"));
            String system = body.getString("system") ;
            Boolean socialAuth = body.getBoolean("social_auth", false);
            JsonObject send = new JsonObject()
                    .put("user_type", "C")
                    .put("email", email)
                    .put("pass", pass)
                    .put("social_auth", socialAuth);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_LOGIN);
            this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), send, options, (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().body() == null) {
                        throw new Exception("User and/or password are invalid");
                    }
                    JsonObject result = reply.result().body();
                    if(system !=null)
                        result.put("accessToken", UtilsJWT.generateAccessTokenAPP(result.getInteger("id")))
                                .put("refreshToken", UtilsJWT.generateRefreshTokenAPP(result.getInteger("id")));

                  else

                        result.put("accessToken", UtilsJWT.generateAccessToken(result.getInteger("id")))
                                .put("refreshToken", UtilsJWT.generateRefreshToken(result.getInteger("id")));
                    UtilsResponse.responseOk(context, result);
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void loginPartner(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_PARTNER_LOGIN);
            this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), body, options, (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        responseError(context, UNEXPECTED_ERROR, reply.cause());
                        return;
                    }

                    if (reply.result().body() == null) {
                        responseError(context, "Invalid credentials");
                        return;
                    }

                    JsonObject result = reply.result().body();
                    Integer userId = result.getInteger("user_id");
                    if (Objects.nonNull(userId)) {
                        result.put("accessToken", UtilsJWT.generateAccessTokenPartner(userId))
                                .put("refreshToken", UtilsJWT.generateRefreshToken(userId));
                    }
                    UtilsResponse.responseOk(context, result);

                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, t);
        }
    }

    private void validToken(RoutingContext context) {
        String token = context.request().getParam("token");
        if (UtilsJWT.isAccessTokenValid(token)) {
            UtilsResponse.responseOk(context, "valid");
        } else {
            UtilsResponse.responseWarning(context, "not valid");
        }
    }

    private void refreshToken(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            JsonObject newAccessToken = UtilsJWT.refreshToken(body.getString("refreshToken"), body.getString("accessToken"));
            UtilsResponse.responseOk(context, new JsonObject().put("newAccessToken", newAccessToken));
        } catch (Exception ex) {
            ex.printStackTrace();
            UtilsResponse.responseWarning(context, ex.getMessage());
        }

    }

    private void recoverPass(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        Boolean socialAuth = body.getBoolean("social_auth", false);
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_FIND_BY_MAIL);
        this.vertx.eventBus().send(UsersDBV.class.getSimpleName(),body, options, (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                JsonObject resultUser = reply.result().body();
                if (resultUser == null) {
                    throw new Exception("Employee not found");
                }
                if (!socialAuth.equals(resultUser.getBoolean("social_auth"))) {
                    throw new Exception("Incorrect login type");
                }
                Random r = new Random();
                //generar codigo de 8 digitos aleatorios
                String code = String.valueOf(r.nextInt(99));
                code += String.valueOf(r.nextInt(99));
                code += String.valueOf(r.nextInt(99));
                code += String.valueOf(r.nextInt(99));

                final String employeeMail = reply.result().body().getString("email");
                final String recoverCode = code;

                JsonObject send = new JsonObject()
                        .put("employee_email", employeeMail)
                        .put("employee_name", reply.result().body().getString("name"))
                        .put("recover_code", recoverCode);

                DeliveryOptions optionsRecoverPass = new DeliveryOptions().addHeader(ACTION, MailVerticle.ACTION_SEND_RECOVER_PASS);
                this.vertx.eventBus().send(MailVerticle.class.getSimpleName(), send, optionsRecoverPass, mailReply -> {
                    try {
                        if (mailReply.failed()){
                            throw mailReply.cause();
                        }
                        String jws = UtilsJWT.generateRecoverPasswordToken(recoverCode, employeeMail);
                        responseOk(context, new JsonObject().put("recover_token", jws), "Mail with code sended");
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseWarning(context, "can't send mail", t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                responseWarning(context, "can't found employee", t);
            }
        });
    }

    private void restorePass(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            String recoverCode = body.getString("recoverCode");
            String recoverToken = body.getString("recoverToken");
            String newPassword = body.getString("newPassword");
            isPasswordAndNotNull(body, "newPassword");
            UtilsJWT.RecoverValidation validation = UtilsJWT.isRecoverTokenMatching(recoverToken, recoverCode);
            if (validation.isValid()) {
                JsonObject send = new JsonObject()
                        .put("user_email", validation.getEmployeeMail())
                        .put("new_password", UtilsSecurity.encodeSHA256(newPassword));
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_UPDATE_PASSWORD);
                this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), send, options, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        responseOk(context, "Password restored");
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, t);
                    }
                });
            } else {
                UtilsResponse.responseWarning(context, "Recover code or recover token are not matching");
            }
        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e);
        }
    }


    private void resetPass(RoutingContext context){
            JsonObject body = context.getBodyAsJson();
            body.put(USER_ID, context.<Integer>get(USER_ID));
            body.put("password" , UtilsSecurity.encodeSHA256(body.getString("actual_pass")));
            try {
                String actualPass = body.getString("actual_pass");
                String newPass = body.getString("new_pass");
                Integer userId = body.getInteger("user_id");
                DeliveryOptions searchOption = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_FIND_BY_USER_ID);
                this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), body , searchOption , replyP ->{
                    try{
                        if(replyP.failed()){
                            throw new Exception(replyP.cause());
                        }
                        if (replyP.result().body() == null) {
                            throw new Exception("Current password incorrect");
                        }
                        if(actualPass.equals(newPass)){
                            throw new UtilsValidation.PropertyValueException("password", "the password must not be the same as the previous one");
                        }
                        JsonObject send = new JsonObject()
                                .put("user_id", userId)
                                .put("new_password", UtilsSecurity.encodeSHA256(newPass));
                        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, UsersDBV.ACTION_RESET_PASSWORD);
                        this.vertx.eventBus().send(UsersDBV.class.getSimpleName(), send, options, reply -> {
                            try {
                                if (reply.failed()){
                                    throw new Exception(reply.cause());
                                }
                                responseOk(context, "Password restored");
                            } catch (Throwable t){
                                t.printStackTrace();
                                responseError(context, t);
                            }
                        });
                    }catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, t);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
                responseError(context, e);
            }
    }
}
