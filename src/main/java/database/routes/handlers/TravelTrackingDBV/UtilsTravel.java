package database.routes.handlers.TravelTrackingDBV;

import database.commons.GenericQuery;
import io.vertx.ext.sql.SQLConnection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class UtilsTravel {

    public static CompletableFuture<Boolean> execGenericQuery(SQLConnection conn, GenericQuery genericQuery) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
            conn.updateWithParams(genericQuery.getQuery(), genericQuery.getParams(), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public static String FormatDate(Date date){
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return format1.format(date);
    }

}
