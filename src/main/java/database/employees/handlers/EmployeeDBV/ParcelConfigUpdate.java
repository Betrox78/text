package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.employees.EmployeeDBV;
import database.employees.handlers.EmployeeDBV.models.ParcelsInitConfig;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.Date;

import static service.commons.Constants.ID;
import static service.commons.Constants.UPDATED_AT;

public class ParcelConfigUpdate extends DBHandler<EmployeeDBV> {

    public ParcelConfigUpdate(EmployeeDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            body.put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            GenericQuery update = this.generateGenericUpdate("parcels_init_config", body, true);
            startTransaction(message, conn -> {
                try {
                    conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
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
