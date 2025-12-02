package service.products;

import database.commons.ErrorCodes;
import database.products.ProductsDBV;
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
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;

import static utils.UtilsValidation.*;

import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;

public class ProductsSV extends ServiceVerticle{
    @Override
    protected String getDBAddress() {
        return ProductsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() { return "/products"; }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::genericRegister);
        this.addHandler(HttpMethod.POST, "/register", AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/associate/products", AuthMiddleware.getInstance(), this::associateProducts);
        this.addHandler(HttpMethod.POST, "/associate/branchoffices", AuthMiddleware.getInstance(), this::associateBranchoffices);
        this.addHandler(HttpMethod.GET, "/report/", AuthMiddleware.getInstance(), this::report);
        this.addHandler(HttpMethod.GET, "/report/:id", AuthMiddleware.getInstance(), this::report);
        super.start(startFuture);
    }

    private void register(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            execEventBus(context, ProductsDBV.REGISTER, body, "Created");
        }
    }

    private void genericRegister(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put(CREATED_BY, context.<Integer>get(USER_ID));
        if (this.isValidCreateData(context)) {
            execEventBus(context, ProductsDBV.GENERIC_REGISTER, body, "Created");
        }
    }

    private void associateProducts(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (this.isValidProductsData(context)) {
            execEventBus(context, ProductsDBV.ASSOCIATE_PRODUCTS, body, "Created");
        }
    }

    private void associateBranchoffices(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        if (this.isValidBranchofficesData(context)) {
            execEventBus(context, ProductsDBV.ASSOCIATE_BRANCHOFFICES, body, "Created");
        }
    }

    private void report(RoutingContext context) {
        //String jwt = context.request().getHeader("Authorization");
        JsonObject body = new JsonObject();
        if(context.request().getParam("id") != null) {
            body.put("id", Integer.parseInt(context.request().getParam("id")));
        }
        execEventBus(context, ProductsDBV.REPORT, body, "Report");
        /*if (UtilsJWT.isTokenValid(jwt)) {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_REPORT);
            this.vertx.eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
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
        } else {
            responseInvalidToken(context);
        }*/
    }

    private void execEventBus(RoutingContext context, String action, JsonObject body, String devMessage){
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), devMessage);
                }


            }catch (Exception ex) {
                ex.printStackTrace();
                responseError(context, ex.getMessage());
            }
        });
    }

    protected boolean isValidBranchofficesData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        if (!body.containsKey("product_id")) {
            errors.add(new PropertyError("product_id", UtilsValidation.MISSING_REQUIRED_VALUE));
        } else if(body.containsKey("product_id")) {
            try {
                if (!body.containsKey("branchoffices")) {
                    errors.add(new PropertyError("branchoffices", UtilsValidation.MISSING_REQUIRED_VALUE));
                }
            } catch (ClassCastException e) {
                errors.add(new PropertyError("branchoffices", UtilsValidation.INVALID_FORMAT));
            }
        }
        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidCreateData(context);
    }

    protected boolean isValidProductsData(RoutingContext context){
        JsonObject body = context.getBodyAsJson();
        List<PropertyError> errors = new ArrayList<>();
        if (!body.containsKey("branchoffice_id")) {
            errors.add(new PropertyError("branchoffice_id", UtilsValidation.MISSING_REQUIRED_VALUE));
        } else if(body.containsKey("branchoffice_id")) {
            try {
                if (!body.containsKey("products")) {
                    errors.add(new PropertyError("products", UtilsValidation.MISSING_REQUIRED_VALUE));
                }
            } catch (ClassCastException e) {
                errors.add(new PropertyError("products", UtilsValidation.INVALID_FORMAT));
            }
        }
        if (!errors.isEmpty()) {
            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, errors);
            return false;
        }
        return super.isValidCreateData(context);
    }
}
