package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoadParcelsCodes extends DBHandler<ShipmentsDBV> {

    public static final String ACTION = "ShipmentsDBV.LoadParcelsCodes";
    private final String traAction = "loaded";


    public LoadParcelsCodes(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        /*
        startTransaction(message,conn->{
            try {
                JsonObject body = message.body();
                JsonArray codes = body.getJsonArray("codes");

                if(codes.isEmpty()) {
                    commitTransaction(message, conn,  new JsonObject()
                            .put("badCodes", new JsonArray())
                            .put("codes_with_error", new JsonArray())
                    );
                }

                Integer shipmentId = body.getInteger("shipment_parcel_id");
                Integer createdBy = body.getInteger("created_by");
                Boolean notScanned = body.getBoolean("not_scanned") != null ? body.getBoolean("not_scanned") : false;
                String status = body.getString("status");
                JsonArray parcelPackages = new JsonArray();
                JsonArray parcels = new JsonArray();
                JsonArray wrongCodes = new JsonArray();
                JsonArray badCodes = new JsonArray();
                JsonObject resCodes = new JsonObject();

                resCodes
                        .put("packages", parcelPackages)
                        .put("parcels", parcels)
                        .put("badCodes",badCodes)
                        .put("wrongCodes",wrongCodes);

                final int len = codes.size();
                List<CompletableFuture<JsonObject>> task = new ArrayList<>();

                for (int i=0;i<len; i++){
                    task.add(analizeCodes(resCodes,codes.getString(i)));
                }
                CompletableFuture.allOf(task.toArray(new CompletableFuture[len])).whenComplete((res,err)->{
                    try {

                        JsonArray params = new JsonArray()
                                .add(shipmentId);
                        conn.queryWithParams(QUERY_GET_SHIPMENT_INFO, params, reply->{
                            try{
                                if(reply.succeeded()){
                                    if(reply.result().getNumRows()>0){
                                        JsonObject shipment = reply.result().getRows().get(0);
                                        if(!shipment.getInteger("shipment_status").equals(1)){
                                            this.rollback(conn,new Throwable("Shipment is closed"), message);
                                        }else{
                                            CompletableFuture<JsonObject> f1 = insertComplementsCodes(conn,resCodes,shipment, shipmentId,createdBy,status,traAction);
                                            CompletableFuture<JsonObject> f2 = insertTicketsCodes(conn,resCodes,shipment, shipmentId,createdBy,status,traAction, notScanned);
                                            CompletableFuture<JsonObject> f3 = insertPackagesCodes(conn,resCodes,shipment, shipmentId,createdBy,status);

                                            CompletableFuture.allOf(f1,f2,f3).whenComplete((result,error)->{
                                                try {
                                                    if(error != null) {
                                                        throw error;
                                                    }

                                                    JsonObject resultO = new JsonObject()
                                                            .put("tickets", boardingPassTickets.size())
                                                            .put("complements", boardingPassComplements.size())
                                                            .put("packages", parcelPackages.size())
                                                            .put("parcels", parcels.size())
                                                            .put("badCodes",badCodes)
                                                            .put("wrongCodes",wrongCodes)
                                                            .put("badCodes",resCodes.getJsonArray("badCodes"))
                                                            .put("codes_with_error",resCodes.getJsonArray("wrongCodes"));
                                                    this.commit(conn,message,resultO);
                                                }catch (Throwable e){
                                                    this.rollback(conn,e,message);
                                                }
                                            });
                                        }

                                    }else{
                                        this.rollback(conn,new Throwable("Shipment not found"), message);
                                    }
                                }else{
                                    this.rollback(conn,reply.cause(), message);
                                }
                            }catch (Exception e){
                                this.rollback(conn,e, message);
                            }
                        });
                    }catch (Exception e){
                        this.rollback(conn,e, message);
                    }
                });

            }catch (Exception e){
                this.rollback(conn,e,message);
            }
        });
        */
    }


    private CompletableFuture<JsonObject> analizeCodes(JsonObject resCodes, String code){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            switch (code.charAt(0)){
                case 'S':
                    resCodes.getJsonArray("tickets").add(code);
                    break;
                case 'P':
                    resCodes.getJsonArray("packages").add(code);
                    break;
                case 'C':
                    resCodes.getJsonArray("complements").add(code);
                    break;
                case 'G':
                    resCodes.getJsonArray("parcels").add(code);
                    break;
                default:
                    resCodes.getJsonArray("badCodes").add(code);
                    break;
            }
            future.complete(resCodes);
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }


    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT * FROM shipments_parcels where id = ? ;";



}
