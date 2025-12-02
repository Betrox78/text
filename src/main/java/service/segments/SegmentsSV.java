package service.segments;

import database.segments.SegmentsDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;
import utils.UtilsValidation;

import static service.commons.Constants.UNEXPECTED_ERROR;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsValidation.containsSpecialCharacterAndNotNull;
import static utils.UtilsValidation.isNameAndNotNull;

public class SegmentsSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return SegmentsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/segments";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/getList", AuthMiddleware.getInstance(), this::getList);
        super.start(startFuture);
    }

    public void getList(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(SegmentsDBV.class.getSimpleName(), body, options(SegmentsDBV.ACTION_GET_SEGMENT_LIST), reply -> {
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

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            containsSpecialCharacterAndNotNull(body, "name");
        } catch (UtilsValidation.PropertyValueException ex) {
            return UtilsResponse.responsePropertyValue(context, ex);
        }
        return super.isValidCreateData(context);
    }
}
