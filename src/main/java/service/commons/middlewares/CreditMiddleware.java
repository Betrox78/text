package service.commons.middlewares;

import database.customers.CustomerDBV;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.responseError;

public class CreditMiddleware implements Handler<RoutingContext> {
    private static CreditMiddleware instance = new CreditMiddleware();
    private static Vertx vertx;

    public static  CreditMiddleware getInstance(Vertx _vertx) {
        if (instance == null) {
            instance = new CreditMiddleware();
        }
        if (vertx == null) {
            vertx = _vertx;
        }
        return instance;
    }

    private CreditMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
                    if(body == null){
                        Integer customerId = Integer.valueOf(context.request().getParam("customer"));
                        body = new JsonObject().put("customer_id",customerId);
                    }else{
                        Boolean paySender;
                        if(body.containsKey("pays_sender")){
                            try{
                                paySender = body.getBoolean("pays_sender");
                                if(!paySender){
                                    context.next();
                                    return;
                                }
                            }catch (Exception e){
                                paySender = body.getInteger("pays_sender").equals(1);
                                if(!paySender){
                                    context.next();
                                    return;
                                }
                            }
                        }
                    }
                    if(!body.containsKey("customer_id")){
                        context.next();
                        return;
                    }
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_LOCK_CUSTOMER_CREDIT);
                vertx.eventBus().send(CustomerDBV.class.getSimpleName(), body, options, reply ->{
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        JsonObject result = (JsonObject) reply.result().body();
                        if(result.getBoolean("has_sales_blocked")){
                            throw new Exception("Customer sales are blocked");
                        }
                        context.next();
                    } catch (Exception ex){
                        ex.printStackTrace();
                        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e);
        }
    }
}