/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.rental;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import static service.commons.Constants.ACTION;

/**
 *
 * @author ulises
 */
public class RentalPriceDBV extends DBVerticle{

    public static final String ACTION_REGISTER = "RentalPriceDBV.register";

    @Override
    public String getTableName() {
        return "rental_price";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                register(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject rentalPrice = message.body().copy();
                JsonArray param = new JsonArray().add(rentalPrice.getInteger("vehicle_id"));

                this.dbClient.queryWithParams(QUERY_VALIDATE_RENTAL, param, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        List<JsonObject> result = reply.result().getRows();
                        int status = 0;
                        if (result.size() > 0){
                            status = result.get(0).getInteger("status");
                        }
                        if (result.size() == 0 || (result.size() > 0 && status == 3)){
                            GenericQuery create = this.generateGenericCreate(rentalPrice);
                            conn.updateWithParams(create.getQuery(), create.getParams(), createHandler -> {
                                try {
                                    if (createHandler.failed()){
                                        throw createHandler.cause();
                                    }
                                    final int id = createHandler.result().getKeys().getInteger(0);
                                    List<String> batch = new ArrayList<>();
                                    conn.batch(batch, batchReply -> {
                                        try {
                                            if (batchReply.failed()){
                                                throw batchReply.cause();
                                            }
                                            this.commit(conn, message, new JsonObject().put("id", id));
                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });
                        }else{
                            throw new Exception("The van already has an assigned price");
                        }
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    private static final String QUERY_VALIDATE_RENTAL = "SELECT id, status FROM rental_price WHERE vehicle_id = ? ORDER BY id DESC LIMIT 1;";
    
}
