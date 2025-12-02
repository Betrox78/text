package service.commons.middlewares;

import database.boardingpass.BoardingPassDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.responseError;

public class PriceListMiddleware implements Handler<RoutingContext> {
    private static PriceListMiddleware instance = new PriceListMiddleware();
    private static Vertx vertx;

    public static  PriceListMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new PriceListMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private PriceListMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        try {
           DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BoardingPassDBV.ACTION_GET_COUNT_PRICELIST_HASH);
                vertx.eventBus().send(BoardingPassDBV.class.getSimpleName(), new JsonObject(), options, reply ->{
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        JsonObject result = (JsonObject) reply.result().body();
                        if(result.getInteger("count") > 0){
                            throw new Exception("Request apply price list is already in progress");
                        }
                        context.next();
                    } catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, ex);
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e);
        }
    }
}
