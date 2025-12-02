package service.commons.middlewares;

import database.promos.PromosDBV;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.promos.PromosSV;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static database.promos.PromosDBV.*;
import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

public class PromosMiddleware implements Handler<RoutingContext> {
    private static PromosMiddleware instance;
    private static Vertx vertx;

    public static synchronized PromosMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new PromosMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private PromosMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            if (body.containsKey(PromosDBV.DISCOUNT) && body.getJsonObject(PromosDBV.DISCOUNT) != null) {

                JsonObject discount = body.getJsonObject(PromosDBV.DISCOUNT);
                discount.put(USER_ID, context.<Integer>get(USER_ID));
                if (new PromosSV().isValidCheckPromoCodeData(context, discount)){
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ACTION_CHECK_USER_PROMO_CODE);
                    vertx.eventBus().send(PromosDBV.class.getSimpleName(), discount, options, reply -> {
                        try {
                            if (reply.failed()){
                                throw reply.cause();
                            }
                            JsonObject resultCheckPromoCode = (JsonObject) reply.result().body();
                            Boolean flagPromoUser = resultCheckPromoCode.getBoolean(FLAG_PROMO, false);
                            context.put(FLAG_USER_PROMO, flagPromoUser);
                            if (!flagPromoUser){
                                checkPromoCode(context, discount);
                            } else {
                                context.put(FLAG_PROMO, true);
                                context.next();
                            }
                        } catch (Throwable t){
                            context.put(FLAG_USER_PROMO, false);
                            checkPromoCode(context, discount);
                        }
                    });
                }
            } else {
                context.put(FLAG_PROMO, false);
                context.put(FLAG_USER_PROMO, false);
                context.next();
            }
        } catch (UtilsValidation.PropertyValueException e) {
            e.printStackTrace();
            UtilsResponse.responsePropertyValue(context, e);
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e);
        }
    }

    public void checkPromoCode(RoutingContext context, JsonObject discount){
        DeliveryOptions optionsC = new DeliveryOptions().addHeader(ACTION, ACTION_CHECK_PROMO_CODE);
        vertx.eventBus().send(PromosDBV.class.getSimpleName(), discount, optionsC, replyC -> {
            try {
                if (replyC.failed()){
                    throw replyC.cause();
                }
                JsonObject resultCheckPromoCodeC = (JsonObject) replyC.result().body();
                context.put(FLAG_PROMO, resultCheckPromoCodeC.getBoolean(FLAG_PROMO, false));
                context.next();
            } catch (Throwable t){
                t.printStackTrace();
                responseError(context, "Error applying discount code", t.getMessage());
            }
        });
    }
}
