package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.employees.EmployeeDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ID;

public class ParcelConfigDelete extends DBHandler<EmployeeDBV> {

    public ParcelConfigDelete(EmployeeDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            Integer id = body.getInteger(ID);
            startTransaction(message, conn -> {
                try {
                    conn.updateWithParams("DELETE FROM parcels_init_config WHERE id = ?", new JsonArray().add(id), reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            this.commit(conn, message, new JsonObject().put("success", true));
                        } catch (Throwable t) {
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

}
