package service.parcel;

import database.commons.ErrorCodes;
import database.parcel.ParcelsIncidencesDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsValidation;

import static database.commons.Action.CREATE;
import static service.commons.Constants.*;
import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.*;

public class ParcelsIncidencesSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return ParcelsIncidencesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcelsIncidences";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::create);
        super.start(startFuture);
    }

    @Override
    protected void create(RoutingContext context) {
        try{
            if (this.isValidCreateData(context)) {
                JsonObject reqBody = context.getBodyAsJson();
                //clean properties if exist any of this
                reqBody.remove(CREATED_AT);
                reqBody.remove(UPDATED_AT);
                reqBody.remove(UPDATED_BY);
                UtilsValidation.isGraterAndNotNull(reqBody,"parcel_id",0);
                UtilsValidation.isGraterAndNotNull(reqBody,"parcel_package_id",0);
                UtilsValidation.isGraterAndNotNull(reqBody,"incidence_id",0);

                //set the user requesting to create
                reqBody.put(CREATED_BY, context.<Integer>get(USER_ID));
                vertx.eventBus().send(this.getDBAddress(), reqBody, options(CREATE.name()), reply -> {
                    try{
                        if(reply.failed()){
                            throw  new Exception(reply.cause());
                        }
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(), "Created");
                        }

                    }catch(Exception e){
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());

                    }

                });
            }
        }catch (Exception e){
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }

    }
}
