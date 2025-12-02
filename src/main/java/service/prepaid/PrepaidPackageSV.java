package service.prepaid;

import database.commons.ErrorCodes;
import database.parcel.ParcelsPackagesDBV;
import database.prepaid.PrepaidPackageDBV;

import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.PropertyError;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import java.util.ArrayList;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static database.prepaid.PrepaidPackageDBV.*;
import static utils.UtilsValidation.INVALID_FORMAT;
import static utils.UtilsValidation.MISSING_REQUIRED_VALUE;

public class PrepaidPackageSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() { return PrepaidPackageDBV.class.getSimpleName(); }

    @Override
    protected String getEndpointAddress() { return "/prepaid_package_config";}

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST,"/", AuthMiddleware.getInstance(),this::register);
        this.addHandler(HttpMethod.GET, "/list", AuthMiddleware.getInstance(), this::getList);
        this.addHandler(HttpMethod.PUT, "/updateStatus", AuthMiddleware.getInstance(), this::updateStatus);
        this.addHandler(HttpMethod.GET, "/findOne/:id", AuthMiddleware.getInstance(), this::findOne);
        this.addHandler(HttpMethod.POST, "/list/site", AuthMiddleware.getInstance(), this::getListSite);
        this.addHandler(HttpMethod.PUT, "/", AuthMiddleware.getInstance(), this::updatePrepaidPackage);
        this.addHandler(HttpMethod.POST, "/list/byTerminalsDistance", AuthMiddleware.getInstance(), this::getByTerminalsDistance);
        super.start(startFuture);
    }

    private void getByTerminalsDistance(RoutingContext context) {
        try {
            JsonObject params = context.getBodyAsJson();
            this.vertx.eventBus().send(PrepaidPackageDBV.class.getSimpleName(), params, options(ACTION_GET_LIST_BY_TERMINALS_DISTANCE), reply -> {
                try {
                    if(reply.failed()){
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
        } catch (Exception e) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
        }
    }

    private void register(RoutingContext context) {
        if (this.isValidData(context, "create")){
            try {
                JsonObject body = context.getBodyAsJson();
                body.put(CREATED_BY, context.<Integer>get(USER_ID));

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_PREPAID_REGISTER), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if(reply.failed()){
                            throw reply.cause();
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Created");
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, t);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, e.getMessage());
            }
        }
    }

    private void getList(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(PrepaidPackageDBV.class.getSimpleName(), body, options(ACTION_GET_PREPAID_LIST), reply -> {
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

    private void getListSite(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            body.put("apply_web",1);

            this.vertx.eventBus().send(PrepaidPackageDBV.class.getSimpleName(), body, options(ACTION_GET_PREPAID_LIST), reply -> {
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

    private void findOne(RoutingContext context){
        try {
            JsonObject id = new JsonObject().put("id", Integer.valueOf(context.request().getParam("id")));
            this.vertx.eventBus().send(PrepaidPackageDBV.class.getSimpleName(), id, options(ACTION_GET_ONE_PACKAGE), reply -> {
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

    private void updateStatus(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(UPDATED_BY, context.<Integer>get(USER_ID));
        this.vertx.eventBus().send(PrepaidPackageDBV.class.getSimpleName(), body, options(PrepaidPackageDBV.ACTION_UPDATE_PREPAID_PACKAGE_STATUS), reply -> {
            if(reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Updated");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void updatePrepaidPackage(RoutingContext context) {
        if (this.isValidData(context, "update")) {
            try {
                JsonObject body = context.getBodyAsJson();
                body.put(UPDATED_BY, context.<Integer>get(USER_ID));

                vertx.eventBus().send(this.getDBAddress(), body, options(ACTION_UPDATE_PACKAGE), (AsyncResult<Message<JsonObject>> reply) -> {
                    try {
                        if(reply.failed()){
                            throw reply.cause();
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Updated");
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, t);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                responseError(context, UNEXPECTED_ERROR, e.getMessage());
            }
        }

//        try {
//            JsonObject body = context.getBodyAsJson();
//            this.vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options(PrepaidPackageDBV.ACTION_UPDATE_PACKAGE), reply -> {
//                if(reply.succeeded()) {
//                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
//                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
//                    } else {
//                        responseOk(context, reply.result().body(), "Updated");
//                    }
//                } else {
//                    responseError(context, "Ocurrio un error inesperado");
//                }
//            });
//        } catch(Throwable t) {
//            t.printStackTrace();
//            responseError(context, UNEXPECTED_ERROR, t.getMessage());
//        }
    }

    protected boolean isValidData(RoutingContext context, String type) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        SERVICES service = null;

        if (!body.containsKey(NAME)){
            errors.add(new PropertyError(NAME, MISSING_REQUIRED_VALUE));
        } else {
            try {
                String name = body.getString(NAME);
                if (name == null){
                    errors.add(new PropertyError(NAME, INVALID_FORMAT));
                }
            } catch (Exception e){
                errors.add(new PropertyError(NAME, e.getMessage()));
            }
        }

        if( !body.containsKey(APPLY_WEB)){
            errors.add(new PropertyError(APPLY_WEB, MISSING_REQUIRED_VALUE));
        } else {
            try{
                Boolean web = body.getBoolean(APPLY_WEB);
                if (web == null){
                    errors.add(new PropertyError(APPLY_WEB, INVALID_FORMAT));
                } else {
                    if(web){
                        body.remove(APPLY_WEB);
                        body.put(APPLY_WEB , 1);
                        context.setBody(body.toBuffer());
                    } else if (!web) {
                        body.remove(APPLY_WEB);
                        body.put(APPLY_WEB , 0);
                        context.setBody(body.toBuffer());

                    }
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_WEB, e.getMessage()));
            }
        }

        if( !body.containsKey(APPLY_APP)){
            errors.add(new PropertyError(APPLY_APP, MISSING_REQUIRED_VALUE));
        } else {
            try{
                Boolean app = body.getBoolean(APPLY_APP);
                if (app == null){
                    errors.add(new PropertyError(APPLY_APP, INVALID_FORMAT));
                } else {
                    if(app){
                        body.remove(APPLY_APP);
                        body.put(APPLY_APP , 1);
                        context.setBody(body.toBuffer());
                    } else if (!app) {
                        body.remove(APPLY_APP);
                        body.put(APPLY_APP , 0);
                        context.setBody(body.toBuffer());
                    }
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_APP, e.getMessage()));
            }
        }

        if( !body.containsKey(APPLY_POS)){
            errors.add(new PropertyError(APPLY_POS, MISSING_REQUIRED_VALUE));
        } else {
            try{
                Boolean pos = body.getBoolean(APPLY_POS);
                if (pos == null){
                    errors.add(new PropertyError(APPLY_POS, INVALID_FORMAT));
                } else {
                    if(pos){
                        body.remove(APPLY_POS);
                        body.put(APPLY_POS , 1);
                        context.setBody(body.toBuffer());
                    } else if (!pos) {
                        body.remove(APPLY_POS);
                        body.put(APPLY_POS , 0);
                        context.setBody(body.toBuffer());
                    }
                }
            } catch (Exception e){
                errors.add(new PropertyError(APPLY_POS, e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }

        return type == "create" ? super.isValidCreateData(context) : super.isValidUpdateData(context);
    }
}
