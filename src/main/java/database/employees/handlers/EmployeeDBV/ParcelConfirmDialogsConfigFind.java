package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.employees.EmployeeDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static service.commons.Constants._EMPLOYEE_ID;

public class ParcelConfirmDialogsConfigFind extends DBHandler<EmployeeDBV> {

    public ParcelConfirmDialogsConfigFind(EmployeeDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer employeeId = body.getInteger(_EMPLOYEE_ID);
        try {
            this.dbClient.queryWithParams(QUERY_GET_BY_EMPLOYEE_ID, new JsonArray().add(employeeId), reply -> {
               try {
                   if (reply.failed()) {
                       throw reply.cause();
                   }
                   List<JsonObject> result = reply.result().getRows();
                   if (result.isEmpty()) {
                       message.reply(new JsonObject());
                       return;
                   }
                   JsonObject config = result.get(0);
                   message.reply(config);
               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private static final String QUERY_GET_BY_EMPLOYEE_ID = "SELECT * FROM parcels_confirm_dialogs_init_config WHERE employee_id = ?";

}
