/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.authservices;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.CREATED_BY;

/**
 *
 * @author ulises
 */
public class AuthServicesDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "AuthServicesDBV.register";

    public static final String SERVICE= "service";
    public static final String PATHS= "paths";
    private static final String ROUTE= "route";
    private static final String SUB_ROUTE= "sub_route";
    private static final String AUTH_SERVICE_ID= "auth_service_id";
    private static final String AUTH_SUB_SERVICES= "auth_sub_services";

    @Override
    public String getTableName() {
        return "auth_services";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn ->{
            try {
                JsonObject body = message.body();
                String service = body.getString(SERVICE);
                Integer createdBy = body.getInteger(CREATED_BY);
                GenericQuery insertService = this.generateGenericCreate(new JsonObject().put(ROUTE, service).put(CREATED_BY, createdBy));
                conn.updateWithParams(insertService.getQuery(), insertService.getParams(), (AsyncResult<UpdateResult> reply) -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        JsonArray paths = (JsonArray) body.getValue("paths");
                        Integer serviceId = reply.result().getKeys().getInteger(0);
                        List<String> insertsCities = paths.stream().map(path ->
                                this.generateGenericCreate(AUTH_SUB_SERVICES, new JsonObject()
                                        .put(SUB_ROUTE, path)
                                        .put(AUTH_SERVICE_ID, serviceId)
                                        .put(CREATED_BY, createdBy)
                                )).collect(Collectors.toList());

                        conn.batch(insertsCities, (AsyncResult<List<Integer>> replyCities) -> {
                            try {
                                if (replyCities.failed()){
                                    throw replyCities.cause();
                                }
                                this.commit(conn, message, new JsonObject()
                                        .put("service_id", serviceId)
                                        .put("sub_services", paths));
                            } catch (Throwable t){
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }
}
