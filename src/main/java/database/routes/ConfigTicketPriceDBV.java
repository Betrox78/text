/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.routes;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

import org.json.JSONArray;
import utils.UtilsDate;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigTicketPriceDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "ConfigRouteDBV.register";

    public static final String CONFIG_TICKET_PRICE = "config_ticket_price";
    public static final String ID = "id";
    public static final String CONFIG_DESTINATION_ID = "config_destination_id";
    public static final String AMOUNT = "amount";
    public static final String DISCOUNT = "discount";
    public static final String TOTAL_AMOUNT = "total_amount";
    public static final String SPECIAL_TICKET_ID = "special_ticket_id";
    public static final String CREATED_BY = "created_by";
    public static final String HAS_DISCOUNT = "has_discount";
    public static final String TOTAL_DISCOUNT = "total_discount";

    @Override
    public String getTableName() {
        return "config_ticket_price";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
        }

    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try{
                JsonObject body = message.body();
                JsonArray tickets = body.getJsonArray("tickets");
                JsonObject ticketBase = new JsonObject();
                for (int i = 0, size = tickets.size(); i < size; i++) {
                    JsonObject ticket = tickets.getJsonObject(i);
                    if(ticket.getInteger("id") == null){
                        ticket.put(CREATED_BY , body.getInteger(CREATED_BY));
                    }
                    ticket.put("config_destination_id" , body.getInteger("config_destination_id"));
                    if(ticket.getBoolean("base")){
                        ticketBase = ticket;
                        ticketBase
                                .put("total_amount", ticket.getDouble("amount"))
                                .put("discount" , 0);
                    }
                }
                List<String> pricesInserts = new ArrayList<>();

                for (int i = 0, size = tickets.size(); i < size; i++) {
                    JsonObject ticket = tickets.getJsonObject(i);
                    Double total_amount =  ticket.getDouble("amount");
                    Double amount =  ticketBase.getDouble("total_amount");
                    Double discount = amount -total_amount;

                    if(!ticket.getBoolean("base")){
                        ticket
                                .put("total_amount", total_amount)
                                .put("amount" ,amount)
                                .put("discount" , amount - total_amount);
                    }
                    ticket.remove("base");
                    String insert = this.generateGenericCreate(CONFIG_TICKET_PRICE, ticket);
                    insert += " ON DUPLICATE KEY UPDATE amount = " + amount + ", discount = " + discount + ", total_amount = " + total_amount + ", updated_by = " + body.getInteger(CREATED_BY) + ", updated_at = '" + UtilsDate.sdfDataBase(new Date()) + "'";
                    pricesInserts.add(insert);
                }
                this.batchInsert( message , conn , pricesInserts , ids -> {
                    this.commit(conn , message , new JsonObject().put("tickets" , ids));
                });

            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn , e , message);
            }
        });

    }

    private List<String> queriesInsertPrices(List<JsonObject> specialTickets, JsonObject baseConfigTicketPrice) {
        List<String> pricesInserts = new ArrayList<>();
        double baseAmount = baseConfigTicketPrice.getDouble(AMOUNT);
        for (int i = 0, size = specialTickets.size(); i < size; i++) {
            JsonObject specialTicket = specialTickets.get(i);
            JsonObject configTicketPrice = baseConfigTicketPrice.copy();
            boolean hasDiscount = specialTicket.getBoolean(HAS_DISCOUNT);
            double discount = 0f;
            double totalAmount = baseAmount;
            if (hasDiscount) {
                int totalDiscount = specialTicket.getInteger(TOTAL_DISCOUNT);
                if (totalDiscount >= 0 && totalDiscount <= 100) {
                    discount = baseAmount * (totalDiscount / 100.0);
                    totalAmount = baseAmount - discount;
                    configTicketPrice.put(DISCOUNT, discount);
                    configTicketPrice.put(TOTAL_AMOUNT, totalAmount);
                }
            }
            int updatedBy = baseConfigTicketPrice.getInteger(CREATED_BY);
            configTicketPrice.put(SPECIAL_TICKET_ID, specialTicket.getInteger(ID));
            String insert = this.generateGenericCreate(CONFIG_TICKET_PRICE, configTicketPrice);
            insert += " ON DUPLICATE KEY UPDATE amount = " + baseAmount + ", discount = " + discount + ", total_amount = " + totalAmount + ", updated_by = " + updatedBy + ", updated_at = '" + UtilsDate.sdfDataBase(new Date()) + "'";
            pricesInserts.add(insert);
        }
        return pricesInserts;
    }

    private void getSpecialTickets(Message<JsonObject> message, SQLConnection conn, Handler<List<JsonObject>> handler) {
        conn.query(QUERY_ALL_SPECIAL_TICKETS, resultHandler -> {
            if (resultHandler.succeeded()) {
                List<JsonObject> specialTickets = resultHandler.result().getRows();
                handler.handle(specialTickets);
            } else {
                this.rollback(conn, resultHandler.cause(), message);
            }
        });
    }

    private void batchInsert(Message<JsonObject> message, SQLConnection conn, List<String> inserts, Handler<List<Integer>> handler) {
        conn.batch(inserts, resultHandler -> {
            try{
                if(resultHandler.failed()) {
                    throw new Exception(resultHandler.cause());
                }
                List<Integer> insertIds = resultHandler.result();
                handler.handle(insertIds);
            }catch (Exception ex) {
                ex.printStackTrace();
                this.rollback(conn, resultHandler.cause(), message);
            }

        });
    }

    private void getBaseSpecialTicket(Message<JsonObject> message, SQLConnection conn, Handler<Integer> handler) {
        conn.querySingle(QUERY_SPECIAL_TICKET_BASE, resultHandler -> {
            if (resultHandler.succeeded()) {
                int specialTicketBaseId = resultHandler.result().getInteger(0);
                handler.handle(specialTicketBaseId);
            } else {
                this.rollback(conn, resultHandler.cause(), message);
            }
        });
    }

    private static final String QUERY_SPECIAL_TICKET_BASE = "SELECT id FROM special_ticket WHERE base=1 and status=1;";
    private static final String QUERY_ALL_SPECIAL_TICKETS = "SELECT id, has_discount, total_discount\n"
            + " FROM special_ticket WHERE status=1";

}
