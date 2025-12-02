package database.segments;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import service.commons.Constants;

import java.util.List;

public class SegmentsDBV extends DBVerticle {
    public static final String ACTION_GET_SEGMENT_LIST = "SegmentsDBV.getSegmentList";
    @Override
    public String getTableName() {
        return "segment";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case ACTION_GET_SEGMENT_LIST:
                this.getSegmentList(message);
                break;
        }
    }

    private void getSegmentList(Message<JsonObject> message){
        try {
            this.dbClient.query(QUERY_GET_SEGMENT_LIST, reply -> {
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

    private static final String QUERY_GET_SEGMENT_LIST = "SELECT\n" +
            "s.*\n" +
            "FROM segment as s\n" +
            "WHERE s.status != 3";
}