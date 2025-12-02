package database.sellers;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import service.commons.Constants;
import java.util.List;
import io.vertx.core.json.JsonArray;


public class SellersDBV extends DBVerticle {
    public static final String ACTION_GET_SELLERS_LIST = "SellersDBV.getSellersList";
    @Override
    public String getTableName() {
        return "seller";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case ACTION_GET_SELLERS_LIST:
                this.getSellersList(message);
                break;
        }
    }

    private void getSellersList(Message<JsonObject> message){
        try {
            this.dbClient.query(QUERY_GET_SELLERS_LIST, reply -> {
                try{
                    if( reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()){
                        message.reply(new JsonArray());
                    }
                    message.reply(new JsonArray(results));
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private static final String QUERY_GET_SELLERS_LIST = "SELECT\n" +
            "s.id,\n" +
            "s.name,\n" +
            "s.last_name,\n" +
            "s.branchoffice_id,\n" +
            "b.name as branchoffice_name,\n" +
            "s.status,\n" +
            "s.created_at\n" +
            "FROM seller as s\n" +
            "LEFT JOIN branchoffice b on b.id = s.branchoffice_id\n" +
            "WHERE s.status != 3";
}
