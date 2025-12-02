package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.employees.EmployeeDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ID;

public class ParcelBranchofficeConfigRegister extends DBHandler<EmployeeDBV> {

    public ParcelBranchofficeConfigRegister(EmployeeDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            GenericQuery insert = this.generateGenericCreate("parcels_branchoffices_init_config", body);
            startTransaction(message, conn -> {
                try {
                    conn.updateWithParams(insert.getQuery(), insert.getParams(), reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            this.commit(conn, message, new JsonObject().put(ID, reply.result().getKeys().getInteger(0)));
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
