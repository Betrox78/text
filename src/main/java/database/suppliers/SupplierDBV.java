/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.suppliers;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

/**
 *
 * @author ulises
 */
public class SupplierDBV extends DBVerticle {

    public static final String REGISTER = "SupplierDBV.register";

    @Override
    public String getTableName() {
        return "supplier";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            JsonObject supplier = message.body().copy();
            supplier.remove("contact_info");
            supplier.remove("bank_info");

            GenericQuery supplierCreate = this.generateGenericCreate(supplier);

            con.updateWithParams(supplierCreate.getQuery(), supplierCreate.getParams(), supplierReply -> {
                try{
                    if(supplierReply.failed()) {
                        throw new Exception(supplierReply.cause());
                    }
                    int id = supplierReply.result().getKeys().getInteger(0);
                    List<String> batch = new ArrayList<>();
                    //inser batch of contact info and bank info
                    JsonObject contactInfo = message.body().getJsonObject("contact_info");
                    if (contactInfo != null) {
                        contactInfo.put("supplier_id", id);
                        batch.add(generateGenericCreate("supplier_contact", contactInfo));
                    }
                    JsonObject bankInfo = message.body().getJsonObject("bank_info");
                    if (bankInfo != null) {
                        bankInfo.put("supplier_id", id);
                        batch.add(generateGenericCreate("supplier_bank_info", bankInfo));
                    }
                    if (!batch.isEmpty()) {
                        con.batch(batch, batchReply -> {
                            try{
                                if(batchReply.failed()){
                                    throw  new Exception(batchReply.cause());
                                }
                                this.commit(con, message, new JsonObject().put("id", id));


                            }catch(Exception e){
                                this.rollback(con, batchReply.cause(), message);
                            }
                        });
                    } else {
                        this.commit(con, message, new JsonObject().put("id", id));
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(con, supplierReply.cause(), message);
                }
            });
        });

    }

}
