package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.employees.EmployeeDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class ParcelBranchofficeConfigFind extends DBHandler<EmployeeDBV> {

    public ParcelBranchofficeConfigFind(EmployeeDBV dbVerticle) {
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
                   List<JsonObject> results = reply.result().getRows();
                   if (results.isEmpty()) {
                       message.reply(new JsonArray());
                       return;
                   }

                   List<CompletableFuture<JsonObject>> tasksCustomerInfo = new ArrayList<>();
                   for (JsonObject config : results) {
                       Integer senderId = config.getInteger(_SENDER_ID);
                       Integer addresseeId = config.getInteger(_ADDRESSEE_ID);
                       tasksCustomerInfo.add(getCustomerInfo(senderId, _SENDER_INFO, config));
                       tasksCustomerInfo.add(getCustomerInfo(addresseeId, _ADDRESSEE_INFO, config));
                   }
                   CompletableFuture.allOf(tasksCustomerInfo.toArray(new CompletableFuture[tasksCustomerInfo.size()]))
                           .whenComplete((res, err) -> {
                       try {
                           if (Objects.nonNull(err)) {
                               throw err;
                           }
                           message.reply(new JsonArray(results));
                       } catch (Throwable t) {
                           reportQueryError(message, t);
                       }
                   });

               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getCustomerInfo(Integer id, String CUSTOMER_TYPE, JsonObject config) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (Objects.isNull(id)) {
            future.complete(null);
        } else {
            this.dbClient.queryWithParams(QUERY_GET_CUSTOMER_INFO, new JsonArray().add(id), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Customer info not found");
                    }
                    JsonObject info = result.get(0);
                    config.put(CUSTOMER_TYPE, info);
                    future.complete(info);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private static final String QUERY_GET_BY_EMPLOYEE_ID = "SELECT * FROM parcels_branchoffices_init_config WHERE employee_id = ?";

    private static final String QUERY_GET_CUSTOMER_INFO = "SELECT * FROM customer WHERE id = ?";

}
