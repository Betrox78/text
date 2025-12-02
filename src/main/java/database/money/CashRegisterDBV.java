/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.money;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

/**
 *
 * @author Dalia Carlón daliamcc@gmail.com
 */
public class CashRegisterDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "CashRegisterDBV.register";
    public static final String ACTION_CASH_REGISTERS_CLOSED = "CashRegisterDBV.getClosed";
    public static final String ACTION_CHANGE_TICKET = "CashRegisterDBV.changeTicket";

    private static final String PREFIX = "prefix";
    public static final String BRANCHOFFICE_ID = "branchoffice_id";
    private static final String QUANTITY = "quantity";
    private static final String LAST_TICKET = "last_ticket";
    private static final String ORIGINAL_TICKET = "original_ticket";

    @Override
    public String getTableName() {
        return "cash_registers";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_CASH_REGISTERS_CLOSED:
                this.getClosed(message);
                break;
            case ACTION_CHANGE_TICKET:
                this.changeTicket(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                Integer branchOfficeId = body.getInteger(BRANCHOFFICE_ID);

                this.getCashRegistersQuantity(conn, branchOfficeId).whenComplete((resultCashRegistersQuantity, errorCashRegistersQuantity) -> {
                    try{
                        if (errorCashRegistersQuantity != null){
                            throw errorCashRegistersQuantity;
                        }
                        body.put(PREFIX, this.getPrefixCashRegister(resultCashRegistersQuantity));
                        GenericQuery model = this.generateGenericCreate(body);
                        conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }
                                Integer id = reply.result().getKeys().getInteger(0);
                                this.commit(conn, message, new JsonObject().put(ID, id));
                            } catch (Throwable t){
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch(Throwable t){
                        this.rollback(conn, t ,message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    private String getPrefixCashRegister(Integer cashRegistersQuantity){
        return "C"+cashRegistersQuantity;
    }

    private CompletableFuture<Integer> getCashRegistersQuantity(SQLConnection conn, Integer branchOfficeId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_CASH_REGISTERS_QUANTITY, new JsonArray().add(branchOfficeId), reply -> {
            try{
                if (reply.failed()){
                    throw reply.cause();
                }
                Integer quantity = reply.result().getRows().get(0).getInteger(QUANTITY);
                future.complete(quantity);
            } catch(Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getClosed(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            Integer branchofficeId = body.getInteger("branchoffice_id");
            JsonArray params = new JsonArray().add(branchofficeId);

            dbClient.queryWithParams(QUERY_CASH_REGISTERS_CLOSED, params, (AsyncResult<ResultSet> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> cashRegisters = reply.result().getRows();
                    message.reply(new JsonArray(cashRegisters));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void changeTicket(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection con) -> {
            try {
                JsonObject body = message.body();

                Integer cashRegisterId = body.getInteger(CASH_REGISTER_ID);
                JsonArray params = new JsonArray().add(cashRegisterId);

                dbClient.queryWithParams(QUERY_GET_CASH_REGISTER_BY_ID, params, (AsyncResult<ResultSet> reply) -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        JsonObject cashRegister = reply.result().getRows().get(0);
                        body.put(ORIGINAL_TICKET, cashRegister.getInteger(LAST_TICKET));

                        GenericQuery update = this.generateGenericUpdate(this.getTableName(), new JsonObject()
                                .put(ID, cashRegisterId)
                                .put(LAST_TICKET, body.getInteger(NEW_TICKET))
                                .put(UPDATED_BY, body.getInteger(CREATED_BY))
                                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                        );

                        con.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                            try {
                                if (replyUpdate.failed())
                                    throw new Exception(replyUpdate.cause());

                                String create = this.generateGenericCreate(CashRegisterTicketsLogDBV.class.newInstance().getTableName(), body);
                                con.update(create, replyInsert -> {
                                    try {
                                        if (replyInsert.failed())
                                            throw new Exception(replyInsert.cause());

                                        final int id = replyInsert.result().getKeys().getInteger(0);
                                        JsonObject result = new JsonObject().put(ID, id).mergeIn(body);
                                        this.commit(con, message, result);

                                    } catch (Throwable t) {
                                        this.rollback(con, t, message);
                                    }
                                });

                            } catch (Throwable t) {
                                this.rollback(con, t, message);
                            }
                        });
                    } catch (Throwable t) {
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t) {
                this.rollback(con, t, message);
            }
        });
    }

    /* querys */
    private static final String QUERY_GET_CASH_REGISTERS_QUANTITY = "SELECT COUNT(id)+1 AS quantity FROM cash_registers WHERE branchoffice_id = ?";

    private static final String QUERY_CASH_REGISTERS_CLOSED = "SELECT cr.id, cr.branchoffice_id, cr.cash_register, co.final_fund \n" +
            "FROM cash_registers as cr \n" +
            "LEFT JOIN ( \n" +
            " SELECT id, cash_register_id, final_fund \n" +
            " FROM cash_out \n" +
            " WHERE id IN ( \n" +
            "  SELECT MAX(id) \n" +
            "  FROM cash_out \n" +
            "  WHERE cash_out_status = 2 \n" +
            "  GROUP BY cash_register_id \n" +
            " ) \n" +
            ") AS co ON co.cash_register_id = cr.id \n" +
            "WHERE cr.cash_out_status = 0 AND cr.status = 1 AND cr.branchoffice_id = ?;";

    private static final String QUERY_GET_CASH_REGISTER_BY_ID= "SELECT * FROM cash_registers WHERE id = ?";
}
