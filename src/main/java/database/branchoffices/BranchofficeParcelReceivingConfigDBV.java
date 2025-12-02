/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.branchoffices;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;

import static service.commons.Constants.*;

/**
 *
 * @author AllAbordo
 */
public class BranchofficeParcelReceivingConfigDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "branchoffice_parcel_receiving_config";
    }

    public static final String ACTION_GET_LIST = "BranchofficeParcelReceivingConfigDBV.getList";
    public static final String ACTION_GET_MISSING_LIST = "BranchofficeParcelReceivingConfigDBV.getMissingList";

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_GET_LIST:
                this.getList(message);
                break;
            case ACTION_GET_MISSING_LIST:
                this.getMissingList(message);
                break;
        }
    }

    @Override
    protected void create(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                JsonObject body = message.body();
                Integer receivingBranchofficeId = body.getInteger("receiving_branchoffice_id");
                Integer ofBranchofficeId = body.getInteger("of_branchoffice_id");

                con.queryWithParams(QUERY_GET_RECEIVING_TERMINALS_CONFIG, new JsonArray().add(receivingBranchofficeId).add(ofBranchofficeId), handler -> {
                   try {
                       if (handler.failed()){
                           throw handler.cause();
                       }
                       List<JsonObject> result = handler.result().getRows();
                       if (result.isEmpty()) {
                           register(con, message);
                       } else {
                           JsonObject config = result.get(0);
                           if (config.getInteger(STATUS) == 1) {
                               throw new Exception("The config already exists");
                           } else {
                               update(con, message, config.getInteger(ID));
                           }
                       }
                   } catch (Throwable t) {
                       this.rollback(con, t, message);
                   }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private void register(SQLConnection con, Message<JsonObject> message) {
        GenericQuery model = this.generateGenericCreate(message.body());
        con.updateWithParams(model.getQuery(), model.getParams(), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                Integer id = reply.result().getKeys().getInteger(0);
                this.commit(con, message, new JsonObject().put("id", id));
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private void update(SQLConnection con, Message<JsonObject> message, Integer id) {
        GenericQuery genericQuery = this.generateGenericUpdate(this.getTableName(), new JsonObject().put(ID, id).put(STATUS, 1));
        con.updateWithParams(genericQuery.getQuery(), genericQuery.getParams(), handler -> {
           try {
               if (handler.failed()) {
                   throw handler.cause();
               }
               this.commit(con, message, new JsonObject().put("id", id));
           } catch (Throwable t){
               t.printStackTrace();
               this.rollback(con, t, message);
           }
        });
    }

    private void getList(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer branchofficeId = body.getInteger(BRANCHOFFICE_ID);
        this.dbClient.queryWithParams(QUERY_GET_RECEIVING_TERMINALS_BY_BRANCHOFFICE_ID, new JsonArray().add(branchofficeId), handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }
                message.reply(new JsonArray(handler.result().getRows()));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void getMissingList(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer branchofficeId = body.getInteger(BRANCHOFFICE_ID);
        JsonArray params = new JsonArray().add(branchofficeId).add(branchofficeId).add(branchofficeId);
        this.dbClient.queryWithParams(QUERY_GET_MISSING_RECEIVING_TERMINALS, params, handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }
                message.reply(new JsonArray(handler.result().getRows()));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private final static String QUERY_GET_RECEIVING_TERMINALS_CONFIG = "SELECT id, status FROM branchoffice_parcel_receiving_config WHERE receiving_branchoffice_id = ? AND of_branchoffice_id = ?;";

    private final static String QUERY_GET_RECEIVING_TERMINALS_BY_BRANCHOFFICE_ID = "SELECT\n" +
            "   bprc.id,\n" +
            "   b.prefix,\n" +
            "   b.name\n" +
            "FROM branchoffice_parcel_receiving_config bprc \n" +
            "INNER JOIN branchoffice b ON b.id = bprc.of_branchoffice_id\n" +
            "WHERE bprc.receiving_branchoffice_id = ? AND bprc.status = 1;";

    private final static String QUERY_GET_MISSING_RECEIVING_TERMINALS = "SELECT\n" +
            "   b.id,\n" +
            "   b.prefix,\n" +
            "   b.name\n" +
            "FROM branchoffice b \n" +
            "LEFT JOIN branchoffice_parcel_receiving_config bprc ON bprc.of_branchoffice_id = b.id\n" +
            "WHERE ((bprc.receiving_branchoffice_id != ? OR bprc.id IS NULL) \n" +
            "   OR (bprc.receiving_branchoffice_id = ? AND bprc.status != 1))\n" +
            "   AND b.id != ?\n" +
            "   AND b.branch_office_type = 'T'\n" +
            "GROUP BY b.id;";

}
