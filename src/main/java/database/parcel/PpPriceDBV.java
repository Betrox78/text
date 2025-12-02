package database.parcel;

import database.commons.DBVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import database.commons.GenericQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.ACTION;

public class PpPriceDBV extends DBVerticle {

    public static final String REGISTER = "PpPriceDBV.register";
    public static final String GET_PARCEL_BY_TRACKING_CODE = "PpPriceDBV.getParcelByTrackingCode";

    @Override
    public String getTableName() {
        return "pp_price";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                register(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body().copy();
                this.register(conn, body).whenComplete((resultRegister, error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        this.commit(conn, message, resultRegister);
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject ppPrice) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            GenericQuery gen = this.generateGenericCreate(ppPrice);

            conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> registerReply) -> {
                try {
                    if( registerReply.failed()) {
                        throw registerReply.cause();
                    }
                    future.complete(ppPrice);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }



}
