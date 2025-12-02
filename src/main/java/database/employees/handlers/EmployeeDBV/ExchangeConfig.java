package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.employees.EmployeeDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class ExchangeConfig extends DBHandler<EmployeeDBV> {

    public ExchangeConfig(EmployeeDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) throws IOException {
        throw new IOException("Method not implemented");
    }

    public void register(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            GenericQuery insert = this.generateGenericCreate("exchange_init_config", body);
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

    public void update(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            body.put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            GenericQuery update = this.generateGenericUpdate("exchange_init_config", body, true);
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

    public void delete(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            Integer id = body.getInteger(ID);
            startTransaction(message, conn -> {
                try {
                    conn.updateWithParams("DELETE FROM exchange_init_config WHERE id = ?", new JsonArray().add(id), reply -> {
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

    public void find(Message<JsonObject> message) {
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
                    Integer senderId = config.getInteger(_SENDER_ID);
                    Integer addresseeId = config.getInteger(_ADDRESSEE_ID);
                    getCustomerInfo(senderId).whenComplete((terminalOrigin, errTO) -> {
                        try {
                            if (errTO != null) {
                                throw errTO;
                            }
                            config.put(_SENDER_INFO, terminalOrigin);
                            getCustomerInfo(addresseeId).whenComplete((terminalDestiny, errTD) -> {
                                try {
                                    if (errTD != null) {
                                        throw errTD;
                                    }
                                    config.put(_ADDRESSEE_INFO, terminalDestiny);
                                    message.reply(config);
                                } catch (Throwable t) {
                                    reportQueryError(message, t);
                                }
                            });
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

    private CompletableFuture<JsonObject> getCustomerInfo(Integer id) {
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
                    future.complete(result.get(0));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private static final String QUERY_GET_BY_EMPLOYEE_ID = "SELECT * FROM exchange_init_config WHERE employee_id = ?";

    private static final String QUERY_GET_CUSTOMER_INFO = "SELECT * FROM customer WHERE id = ?";
}
