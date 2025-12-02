/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.commons;

import static database.commons.Action.*;

import database.commons.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import service.commons.middlewares.AuthMiddleware;
import models.ModelReponse;
import static models.ModelReponse.Status.OK;
import models.PropertyError;
import static service.commons.Constants.*;
import utils.UtilsDate;
import utils.UtilsJWT;
import utils.UtilsResponse;
import static utils.UtilsResponse.*;
import utils.UtilsRouter;
import utils.UtilsValidation;

import javax.management.ObjectName;

/**
 * Base Verticle to work with LCRUD default operations
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public abstract class ServiceVerticle extends AbstractVerticle {

    /**
     * The router for this verticle service instance
     */
    protected final Router router = Router.router(vertx);

    /**
     * Need to specifie the address of the verticles in the event bus with the access of the db that contains the table
     *
     * @return the name of the registered DBVerticle to work with
     */
    protected abstract String getDBAddress();

    /**
     * Need to especifie the endpoint domain for this verticles begining with "/", ex: return "/example";
     *
     * @return the name to register the verticle in the main router
     */
    protected abstract String getEndpointAddress();

    public ServiceVerticle() {
        // Verify allowed methods
        this.globalMiddlewares.add((context) -> {
            HttpMethod method = context.request().method();
            if (this.getAllowedMethods().contains(method)) {
                context.next();
            } else {
                responseWarning(context, "Method not allowed", "Private method");
            }
        });
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        HttpServer server = vertx.createHttpServer();

        // Add all generic handlers
        this.addHandler(HttpMethod.GET, "/", AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/v2", AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/:id", AuthMiddleware.getInstance(), this::findById);
        this.addHandler(HttpMethod.GET, "/count", AuthMiddleware.getInstance(), this::count);
        this.addHandler(HttpMethod.GET, "/count/perPage/:num", AuthMiddleware.getInstance(), this::countPerPage);
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::create);
        this.addHandler(HttpMethod.PUT, "/", AuthMiddleware.getInstance(), this::update);
        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::deleteById);

        UtilsRouter.getInstance(vertx).mountSubRouter(getEndpointAddress(), router);
        Integer portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
        if (portNumber == null) {
            startFuture.fail(new Exception("No port speficied in configuration"));
            System.out.println("Could not start a HTTP server" + this.getClass().getSimpleName() + ", no port speficied in configuration");
        } else {
            server.requestHandler(UtilsRouter.getInstance(vertx)::accept).listen(portNumber, ar -> {
                if (ar.succeeded()) {
                    System.out.println("[Route:" + getEndpointAddress() + "] " + this.getClass().getSimpleName() + " running");
                    startFuture.complete();
                } else {
                    System.out.println("Could not start a HTTP server " + this.getClass().getSimpleName() + ", " + ar.cause());
                    startFuture.fail(ar.cause());
                }
            });
        }
    }

    protected List<HttpMethod> getAllowedMethods() {
        List<HttpMethod> methods = new ArrayList<>();
        methods.add(HttpMethod.GET);
        methods.add(HttpMethod.DELETE);
        methods.add(HttpMethod.POST);
        methods.add(HttpMethod.PUT);
        return methods;
    }
    /**
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "findAll"
     *
     * @param context the routing context running in the request
     */
    protected final void findAll(RoutingContext context) {
        JsonObject message = new JsonObject()
                .put("query", context.request().getParam("query"))
                .put("from", context.request().getParam("from"))
                .put("to", context.request().getParam("to"));
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, FIND_ALL.name());
        vertx.eventBus().send(this.getDBAddress(), message, options, reply -> {
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
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "findAll"
     *
     * @param context the routing context running in the request
     */
    protected final void findAllV2(RoutingContext context) {
        JsonObject message = new JsonObject();
        MultiMap params = context.request().params();
        for (Map.Entry<String, String> entry : params.entries()) {
            message.put(entry.getKey(), entry.getValue());
        }
        vertx.eventBus().send(this.getDBAddress(), message,
                options(FIND_ALL_V2.name()), reply -> {
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
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "findById"
     *
     * @param context the routing context running in the request
     */
    protected void findById(RoutingContext context) {
        try {
            JsonObject message = new JsonObject()
                .put("id", Integer.valueOf(context.request().getParam("id")));
            vertx.eventBus().send(this.getDBAddress(), message,
                options(FIND_BY_ID.name()), reply -> {
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
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex.getMessage());
        }

    }

    /**
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "update"
     *
     * @param context the routing context running in the request
     */
    protected void update(RoutingContext context) {
        if (this.isValidUpdateData(context)) {
            JsonObject reqBody = context.getBodyAsJson();
            //clean properties if exist any of this
            reqBody.remove(CREATED_AT);
            reqBody.remove(CREATED_BY);
            //set the user requesting to update
            reqBody.put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            reqBody.put(UPDATED_BY, context.<Integer>get(USER_ID));
            vertx.eventBus().send(this.getDBAddress(), reqBody,
                    options(UPDATE.name()), reply -> {
                        try{
                            if(reply.failed()){
                                throw new Exception(reply.cause());
                            }
                            MultiMap headers = reply.result().headers();
                            if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                            } else {
                                responseOk(context, "Updated");
                            }
                        }catch (Exception ex){
                            ex.printStackTrace();
                            responseError(context, ex.getMessage());
                        }
            });
        }

    }

    /**
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "create"
     *
     * @param context the routing context running in the request
     */
    protected void create(RoutingContext context) {
        try {
            if (this.isValidCreateData(context)) {
                JsonObject reqBody = context.getBodyAsJson();
                //clean properties if exist any of this
                reqBody.remove(CREATED_AT);
                reqBody.remove(UPDATED_AT);
                reqBody.remove(UPDATED_BY);
                //set the user requesting to create
                reqBody.put(CREATED_BY, context.<Integer>get(USER_ID));
                vertx.eventBus().send(this.getDBAddress(), reqBody, options(CREATE.name()), reply -> {
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Created");
                        }

                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex.getMessage());
        }
    }

    /**
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "deleteById"
     *
     * @param context the routing context running in the request
     */
    protected void deleteById(RoutingContext context) {
        JsonObject reqBody = new JsonObject()
                .put("id", Integer.valueOf(context.request().getParam("id")));
        vertx.eventBus().send(this.getDBAddress(), reqBody, options(DELETE_BY_ID.name()),
                reply -> {
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        MultiMap headers = reply.result().headers();
                        if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                        } else {
                            responseOk(context, "Deleted");
                        }

                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                }
        );

    }

    /**
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "deleteById"
     *
     * @param context the routing context running in the request
     */
    protected final void count(RoutingContext context) {
        vertx.eventBus().send(this.getDBAddress(), null, options(COUNT.name()),
                reply -> {
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        MultiMap headers = reply.result().headers();
                        if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                        } else {
                            responseOk(context, reply.result().body(), "Counted");
                        }

                    }catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, ex.getMessage());
                    }
                }
        );
    }

    /**
     * Sends a message to the verticle registered with DBAddress especified in this instance the action of "deleteById"
     *
     * @param context the routing context running in the request
     */
    protected final void countPerPage(RoutingContext context) {
        vertx.eventBus().send(this.getDBAddress(), null, options(COUNT.name()),
                (AsyncResult<Message<JsonObject>> reply) -> {
                    if (reply.succeeded()) {
                        MultiMap headers = reply.result().headers();
                        if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                        } else {
                            try {
                                int num = Integer.parseInt(context.request().getParam("num"));
                                if (num < 1) {
                                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE,
                                            new JsonObject().put("name", "num").put("error", "can't be less than 1"));
                                } else {
                                    int count = reply.result().body().getInteger("count");
                                    Float pages = (float) count / (float) num;
                                    int entero = pages.intValue();
                                    int numPages;
                                    float dif = (pages - entero);
                                    if (dif > 0) {
                                        numPages = entero + 1;
                                    } else {
                                        numPages = entero;
                                    }
                                    JsonObject res = new JsonObject()
                                            .put("count", count)
                                            .put("pages", numPages);

                                    responseOk(context, res, "Counted");
                                }
                            } catch (NumberFormatException e) {
                                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE,
                                        new JsonObject().put("name", "num").put("error", "Is not a number"));
                            }
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                }
        );

    }

    /**
     * Verifies is the data of the request is valid to create a record of this entity
     *
     * @param context context of the request
     * @return true if the data is valid, false othrewise
     */
    protected boolean isValidCreateData(RoutingContext context) {
        if (context.getBodyAsJson().getInteger("id") != null) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("id", UtilsValidation.INVALID_PARAMETER));
            return false;
        }
        return true;
    }

    /**
     * Verifies is the data of the request is valid to update a record of this entity
     *
     * @param context context of the request
     * @return true if the data is valid, false othrewise
     */
    protected boolean isValidUpdateData(RoutingContext context) {
        if (context.getBodyAsJson().getInteger("id") == null) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError("id", UtilsValidation.MISSING_REQUIRED_VALUE));
            return false;
        }
        return true;
    }

    /**
     * Generic response to avoid boilerplate
     *
     * @param context context to reply
     * @param reply reply from the async result
     */
    protected final void genericResponse(RoutingContext context, AsyncResult<Message<Object>> reply) {
        try{
            if(reply.failed()) {
                throw new Exception(reply.cause());
            }
            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                return;
            }
            ModelReponse res = new ModelReponse(OK);
            res.setData(reply.result().body());
            HttpServerResponse response = context.response();
            response.putHeader("Content-Type", "application/json");
            response.end(Json.encode(res));

        }catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex.getMessage());
        }
    }

    /**
     * Generic response to avoid boilerplate
     *
     * @param context context to reply
     * @param reply reply from the async result
     */
    protected final void genericResponseJsonObject(RoutingContext context, AsyncResult<Message<JsonObject>> reply) {
        try{
            if(reply.failed()) {
                throw new Exception(reply.cause());
            }
            if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                return;
            }
            ModelReponse res = new ModelReponse(OK);
            res.setData(reply.result().body());
            HttpServerResponse response = context.response();
            response.putHeader("Content-Type", "application/json");
            response.end(Json.encode(res));

        }catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex.getMessage());
        }
    }

    /**
     * Generic response to avoid boilerplate
     *
     * @param context context to reply
     * @param reply reply from the async result
     * @param message string message to send to the final user
     */
    protected final void genericResponse(RoutingContext context, AsyncResult<Message<Object>> reply, String message) {
        try{
            if(reply.failed()) {
                throw new Exception(reply.cause());
            }
            ModelReponse res = new ModelReponse(OK);
            res.setData(reply.result().body());
            res.setMessage(message);
            HttpServerResponse response = context.response();
            response.putHeader("Content-Type", "application/json");
            response.end(Json.encode(res));
        }catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, ex.getMessage());
        }

    }

    /**
     * Validates is the access token in the header Authorization is still valid
     * @param context context from the http request
     * @param handler handler to proceed if access token is valid
     * @deprecated Now we'll validate token with middlewares see.
     *  Replaced by {@link #addHandler(HttpMethod, String, Handler[])}}
     */
    @Deprecated
    protected final void validateToken(RoutingContext context, Handler<Integer> handler) {
        String token = context.request().headers().get(AUTHORIZATION);
        if (UtilsJWT.isTokenValid(token)) {
            handler.handle(UtilsJWT.getUserIdFrom(token));
        } else {
            UtilsResponse.responseInvalidToken(context);
        }
    }

    /**
     * @deprecated Now we'll set public token with middleware.
     *  Replaced by {@link #addHandler(HttpMethod, String, Handler[])}}
     */
    @Deprecated
    protected void setPublic(RoutingContext context) {
        MultiMap headers = context.request().headers();
        String authorization = headers.get(AUTHORIZATION);
        if (authorization == null) {
            headers.add(AUTHORIZATION, UtilsJWT.getPublicToken());
        }
        context.next();
    }

    /**
     * creates a generic DeliveryOptions with a header ACTION
     *
     * @param action action to add as header in ACTION key
     * @return a new DeliveryOptions instance
     */
    protected final DeliveryOptions options(String action) {
        return new DeliveryOptions().addHeader(ACTION, action);
    }

    @SafeVarargs
    protected final void addHandler(HttpMethod method, String path, Handler<RoutingContext>... middlewares) {
        if (middlewares.length > 0) {
            ArrayList<Handler<RoutingContext>> list = new ArrayList<>(Arrays.asList(middlewares));
            Route route = this.router.route(method, path);
            globalMiddlewares.forEach(route::handler);

            if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                Handler<RoutingContext> handler = list.remove(list.size()-1);
                route.handler(BodyHandler.create());
                list.forEach(route::handler);
                route.handler(handler);
            } else {
                list.forEach(route::handler);
            }
        } else {
            System.out.println("Warning | ".concat(method.toString())
                    .concat(" | ").concat(path).concat(" | ")
                    .concat(" Handler not specified"));
        }

    }

    private final List<Handler<RoutingContext>> globalMiddlewares = new ArrayList<>();

    protected void transactionLogger(RoutingContext context, JsonObject customBody, Handler<JsonObject> handler) {
        JsonObject transactionLog = new JsonObject()
                .put("method", context.request().method())
                .put("path", context.request().path())
                .put("payload", Objects.nonNull(customBody) ? customBody.encodePrettily() : context.getBodyAsJson().encodePrettily());
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, TRANSACTION_LOGGER.name());
        vertx.eventBus().send(this.getDBAddress(), transactionLog, options, (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                handler.handle(reply.result().body());
            } catch (Throwable t) {
                Logger.getLogger("transactionLogger").log(Level.SEVERE, "Error transaction logger", t);
                handler.handle(reply.result().body());
            }
        });
    }

    protected void exceptionLogger(RoutingContext context, JsonObject customBody, Throwable t, Handler<JsonObject> handler) {
        JsonObject transactionLog = new JsonObject()
                .put("method", context.request().method())
                .put("path", context.request().path())
                .put("payload", Objects.nonNull(customBody) ? customBody.encodePrettily() : context.getBodyAsJson().encodePrettily())
                .put("exception", t.getMessage());
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, EXCEPTION_LOGGER.name());
        vertx.eventBus().send(this.getDBAddress(), transactionLog, options, (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                handler.handle(reply.result().body());
            } catch (Throwable tt) {
                Logger.getLogger("exceptionLogger").log(Level.SEVERE, "Error exception logger", tt);
                handler.handle(reply.result().body());
            }
        });
    }

    protected void execService(String action, JsonObject body, int retries, int attempt, Handler<JsonObject> handler, Handler<Throwable> handlerThrowable) {
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                handler.handle(reply.result().body());
            } catch (Throwable t) {
                if (t.getMessage().contains("Deadlock") && attempt <= retries) {
                    this.getVertx().setTimer(30, dl -> execService(action, body, retries, attempt + 1, handler, handlerThrowable));
                } else {
                    handlerThrowable.handle(t);
                }
            }
        });
    }
}
