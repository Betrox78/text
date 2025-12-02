package service.parcel;

import database.parcel.DeliveryAttemptReasonDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;

import static database.parcel.DeliveryAttemptReasonDBV.ACTION_LIST_ACTIVE;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.responseError;

public class DeliveryAttemptReasonSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return DeliveryAttemptReasonDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/deliveryAttemptReason";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/list/active", AuthMiddleware.getInstance(), this::getActive);
        super.start(startFuture);
    }

    public void getActive(RoutingContext context) {
        try {
            vertx.eventBus().send(this.getDBAddress(), new JsonObject(), options(ACTION_LIST_ACTIVE), reply -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    this.genericResponse(context, reply, "Found");
                }catch (Exception ex){
                    responseError(context, ex.getMessage());
                }
            });
        } catch (Throwable t) {
            responseError(context, UNEXPECTED_ERROR, t.getMessage());
        }
    }
}
