package service.parcel;

import database.commons.ErrorCodes;
import database.parcel.ParcelsPackagesTrackingDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsValidation;

import static database.commons.Action.CREATE;
import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

public class ParcelsPackagesTrackingSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ParcelsPackagesTrackingDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcelsPackagesTracking";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::genericRegister);
        super.start(startFuture);
    }
    private void genericRegister(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson();
            UtilsValidation.isGraterAndNotNull(body,"terminal_id",0);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            if (this.isValidCreateData(context)) {
                execEventBus(context, CREATE.name(), body, "Created");
            }
        }catch (Exception e){
            responseError(context,"Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void execEventBus(RoutingContext context, String action, JsonObject body, String devMessage){
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), devMessage);
                }
            } else {
                responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
            }
        });
    }
}
