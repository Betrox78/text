package database.ead_rad;


import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsID;


import static database.promos.PromosDBV.DISCOUNT;
import static service.commons.Constants.*;
import static utils.UtilsDate.sdfDataBase;

public class EadRadDBV extends DBVerticle {

    public static final String ACTION_ATTEMPT_REASON = "EadRadDBV.getAttemptReason";
    public static final String REGISTER_ATTEMPT_REASON = "EadRadDBV.insertAttemptReason";
    public static final String ACTION_UPDATE_ATTEMPT_STATUS = "EadRadDBV.updateStatus";
    public static final String ACTION_INSERT_SCHEDULES_RAD_EAD = "EadRadDBV.insert_schedules_rad_ead";
    public static final String ACTION_GET_SCHEDULES_RAD_EAD = "EadRadDBV.get_schedules_rad_ead";
    public static final String ACTION_UPDATE_STATUS_SCHEDULES_RAD_EAD = "EadRadDBV.updateScheduleChangeStatus";
    public static final String ACTION_GET_PARCELS_MANIFEST = "EadRadDBV.get_parcels_manifest";
    public static final String ACTION_GET_SERVICE_TYPE_EAD_RAD = "EadRadDBV.get_service_type_ead_rad";
    public static final String ACTION_GET_SERVICE_TYPE_EAD_RAD_POS = "EadRadDBV.get_service_type_ead_rad_pos";
    public static final String ACTION_INSERT_MANIFEST = "EadRadDBV.insert_manifest";
    public static final String ACTION_GET_MANIFEST = "EadRadDBV.get_manifest";
    public static final String ACTION_GET_MANIFEST_ALL_PACKAGES = "EadRadDBV.get_manifest_all_packages";
    public static final String ACTION_UPDATE_DATA_SERVICE = "EadRadDBV.updateDataService";
    public static final String ACTION_UPDATE_MANIFEST_CONFIRMED_DELIVERY = "EadRadDBV.update_manifest_confirmed_delivery";
    public static final String ACTION_UPDATE_MANIFEST_BY_ID = "EadRadDBV.updateManifestStatus";
    public static final String ACTION_REPRINT_MANIFEST_BY_ID = "EadRadDBV.reprintManifest";
    public static final String ACTION_GET_MANIFEST_CANCEL = "EadRadDBV.get_manifest_cancel";
    public static final String ACTION_CANCEL_MANIFEST = "EadRadDBV.cancel_manifest";
    public static final String ACTION_CHECK_STATUS_FXC = "EadRadDBV.checkStatusFXC";
    public static final String ACTION_GET_PACKAGES_FXC = "EadRadDBV.getPackagesFXC";
    public static final String ACTION_GET_CUSTOMERS_EAD_RAD = "EadRadDBV.getCustomersEadRad";
    public static final String ACTION_REGISTER_CUSTOMER_RAD_EAD = "EadRadDBV.registerCustomerEadRad";
    public static final String ACTION_UPDATE_CUSTOMER_RAD_EAD = "EadRadDBV.updateCustomerEadRad";
    public static final String ACTION_VALID_CUSTOMER_SPECIAL_SERVICE = "EadRadDBV.getValidCustomersSpecialService";
    public static final String ACTION_VALID_CUSTOMER_SPECIAL_ID = "EadRadDBV.getValidCustomersSpecialId";
    @Override
    public String getTableName() {
        return "ead_rad";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_ATTEMPT_REASON:
                this.getAttemptReason(message);
                break;
            case REGISTER_ATTEMPT_REASON:
                this.insertAttemptReason(message);
                break;
            case ACTION_UPDATE_ATTEMPT_STATUS:
                this.updateStatus(message);
                break;
            case ACTION_INSERT_SCHEDULES_RAD_EAD:
                this.insertSchecule_rad_ead(message);
                break;
            case ACTION_GET_SCHEDULES_RAD_EAD:
                this.get_schedules_rad_ead(message);
                break;
            case ACTION_UPDATE_STATUS_SCHEDULES_RAD_EAD:
                this.updateScheduleChangeStatus(message);
                break;
            case ACTION_GET_PARCELS_MANIFEST:
                this.get_parcels_manifest(message);
                break;
            case ACTION_GET_SERVICE_TYPE_EAD_RAD:
                this.get_service_type_ead_rad(message);
                break;
            case ACTION_GET_SERVICE_TYPE_EAD_RAD_POS:
                this.get_service_type_ead_rad_pos(message);
                break;
            case ACTION_INSERT_MANIFEST:
                this.insert_manifest(message);
                break;
            case ACTION_UPDATE_DATA_SERVICE:
                this.updateDataService(message);
                break;
            case ACTION_GET_MANIFEST:
                this.getManifest(message);
                break;
            case ACTION_GET_MANIFEST_ALL_PACKAGES:
                this.getManifestAllPackages(message);
                break;
            case ACTION_UPDATE_MANIFEST_CONFIRMED_DELIVERY:
                this.update_manifest_confirmed_delivery(message);
                break;
            case ACTION_UPDATE_MANIFEST_BY_ID:
                this.updateManifestStatus(message);
                break;
            case ACTION_REPRINT_MANIFEST_BY_ID:
                this.reprintManifest(message);
                break;
            case ACTION_GET_MANIFEST_CANCEL:
                this.getManifestCancel(message);
                break;
            case ACTION_CANCEL_MANIFEST:
                this.cancelManifest(message);
                break;
            case ACTION_CHECK_STATUS_FXC:
                this.checkStatusFXC(message);
                break;
            case ACTION_GET_PACKAGES_FXC:
                this.getPackagesFXC(message);
                break;
            case ACTION_GET_CUSTOMERS_EAD_RAD:
                this.getCustomersEadRad(message);
                break;
            case ACTION_REGISTER_CUSTOMER_RAD_EAD:
                this.registerCustomerEadRad(message);
                break;
            case ACTION_UPDATE_CUSTOMER_RAD_EAD:
                this.updateCustomerEadRad(message);
                break;
        }
    }

    private void getAttemptReason(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            String QUERY = GET_ATTEMPT_REASON;

            //JsonArray params = new JsonArray().add(initDate).add(endDate);


            this.dbClient.query(QUERY, reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> codes = reply.result().getRows();
                    if (codes.isEmpty()){
                        throw new Exception("Attempt reason not found");
                    }
                    message.reply(new JsonArray(codes));

                }  catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void insertAttemptReason(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            try{

                JsonObject copy = message.body().copy();
                copy.put(CREATED_AT,sdfDataBase(new Date()));

                this.insertDeliveryAttempt(conn,copy).whenComplete((res,error) -> {
                    try{
                        if(error != null){
                            this.rollback(conn, error, message);
                        } else {
                            this.commit(conn, message , res);
                        }
                    } catch (Exception e){
                        this.rollback(conn,error,message);
                    }
                });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn,t , message);
            }
        });
    }

    private CompletableFuture<JsonObject> insertDeliveryAttempt(SQLConnection conn, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("delivery_attempt_reason",params);

        conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    future.complete(reply.result().toJson());
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void updateStatus(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer status=Integer.parseInt(body.getString("status"));
        JsonArray params = new JsonArray()
                .add(status)
                .add(sdfDataBase(new Date()))
                .add(Integer.parseInt(body.getString("updated_by"))).add(body.getInteger("id"));

        this.dbClient.queryWithParams(QUERY_UPDATE_ATTEMPT_REASON, params, reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void  insertSchecule_rad_ead(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();
                String query="SELECT * FROM schedule_rad_ead where (idBranchOffice ="+data.getInteger("idBranchOffice")+"    AND type_service ='"+data.getString("type_service")+"') AND (status = 2 OR status = 1) AND (privateSchedule =" +data.getString("privateSchedule")+")" ;
                this.dbClient.queryWithParams(query, null, reply -> {
                    if (reply.succeeded()) {
                        if (reply.result().getNumRows()>0) {
                            message.reply("true");
                        } else {
                            this.insertSchedule(conn,data).whenComplete((res, error) -> {

                                try {
                                    if(error != null){
                                        this.rollback(conn, error, message);
                                    } else {
                                        this.commit(conn, message, res);

                                    }

                                } catch (Exception e) {
                                    this.rollback(conn, error, message);
                                }
                            });
                        }
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                });


            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }
    private CompletableFuture<JsonObject> insertSchedule(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();


        JsonObject paramRAD = new JsonObject()
                .put("type_service",     params.getString("type_service"))
                .put("idBranchOffice",     params.getInteger("idBranchOffice"))
                .put("privateSchedule",     Integer.parseInt(params.getString("privateSchedule")))
                .put("created_by",    Integer.parseInt(params.getString("created_by")))
                .put("sunSchedule",    params.getString("sunSchedule"))
                .put("monSchedule",    params.getString("monSchedule"))
                .put("thuSchedule",    params.getString("thuSchedule"))
                .put("wenSchedule",    params.getString("wenSchedule"))
                .put("tueSchedule",    params.getString("tueSchedule"))
                .put("friSchedule",    params.getString("friSchedule"))
                .put("satSchedule",    params.getString("satSchedule"))

                .put("sun",    params.getInteger("sun"))
                .put("mon",    params.getInteger("mon"))
                .put("thu",    params.getInteger("thu"))
                .put("wen",    params.getInteger("wen"))
                .put("tue",    params.getInteger("tue"))
                .put("fri",    params.getInteger("fri"))
                .put("sat",    params.getInteger("sat"));



        String insert = this.generateGenericCreate("schedule_rad_ead",paramRAD);

        conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    future.complete(reply.result().toJson());
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });



        return future;
    }

    private void get_schedules_rad_ead(Message<JsonObject> message) { this.dbClient.queryWithParams(GET_SCHEDULES_RAD_EAD, null, reply -> {
        if (reply.succeeded()) {
            if (reply.result().getNumRows() == 0) {
                message.reply(null);
            } else {


                message.reply(new JsonArray().add(reply.result().getRows()));
            }
        } else {
            reportQueryError(message, reply.cause());
        }
    });
    }
    private void updateScheduleChangeStatus(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer status=Integer.parseInt(body.getString("status"));
        JsonArray params = new JsonArray()
                .add(status)
                .add(Integer.parseInt(body.getString("updated_by"))).add(body.getInteger("Id"));

        this.dbClient.queryWithParams(UPDATE_STATUS_SCHEDULE, params, reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void get_parcels_manifest(Message<JsonObject> message) {
        try {
            JsonObject body = message.body().getJsonObject("data");
            String type_service=body.getString("type_service");
            Integer id_branchoffice=body.getInteger("id_branchoffice");
            String query ="";
            if(type_service.equals("EAD") || type_service.equals("RAD/EAD")) {
                query = GET_PARCELS_EAD + " p.terminal_destiny_id=" + id_branchoffice + ";";
            }else{
                query = GET_PARCELS_RAD +" p.terminal_origin_id="  + id_branchoffice + ";";
            }

            this.dbClient.queryWithParams(query, null, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().getNumRows() == 0) {
                        message.reply(null);
                    } else {


                        message.reply(new JsonArray().add(reply.result().getRows()));
                    }
                } else {
                    reportQueryError(message, reply.cause());
                }
            });

        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }



    }
    private void get_service_type_ead_rad(Message<JsonObject> message) { this.dbClient.queryWithParams(GET_SERVICE_RAD_EAD, null, reply -> {
        if (reply.succeeded()) {

            if (reply.result().getNumRows() == 0) {
                message.reply(null);
            } else {


                message.reply(new JsonArray().add(reply.result().getRows()));
            }
        } else {
            reportQueryError(message, reply.cause());
        }
    });
    }

    private void get_service_type_ead_rad_pos(Message<JsonObject> message) { this.dbClient.queryWithParams(GET_SERVICE_RAD_EAD_POS, null, reply -> {
        if (reply.succeeded()) {
            if (reply.result().getNumRows() == 0) {
                message.reply(null);
            } else {


                message.reply(new JsonArray().add(reply.result().getRows()));
            }
        } else {
            reportQueryError(message, reply.cause());
        }
    });
    }

    private void  insert_manifest(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();
                this.insertManifestMaster(conn,data).whenComplete((res, error) -> {

                    try {
                        if(error != null){
                            this.rollback(conn, error, message);
                        } else {
                            JsonArray value =res.getJsonArray("keys");
                            int id_parcels_manifest=value.getInteger(0);

                            this.insertManifestDetail(conn,data.getJsonObject("data"),id_parcels_manifest).whenComplete((res0, error0) -> {

                                try {
                                    if(error0 != null){
                                        this.rollback(conn, error0, message);
                                    } else {
                                        this.insertFolioManifest(conn,data.getJsonObject("data"),id_parcels_manifest).whenComplete((res1, error1) -> {

                                            try {
                                                if(error0 != null){
                                                    this.rollback(conn, error1, message);
                                                } else {
                                                    this.updateStatusParcelsEadRAd(conn,data.getJsonObject("data"),id_parcels_manifest).whenComplete((res2, error2) -> {

                                                        try {
                                                            if(error0 != null){
                                                                this.rollback(conn, error2, message);
                                                            } else {
                                                                res2.put("folio",res1);
                                                                res2.put("id",id_parcels_manifest);
                                                                res2.put("id_branchoffice", data.getJsonObject("data").getInteger("id_branchoffice"));
                                                                this.commit(conn, message,res2);

                                                            }

                                                        } catch (Exception e) {
                                                            this.rollback(conn, error2, message);
                                                        }
                                                    });

                                                }

                                            } catch (Exception e) {
                                                this.rollback(conn, error0, message);
                                            }
                                        });

                                    }

                                } catch (Exception e) {
                                    this.rollback(conn, error0, message);
                                }
                            });


                        }

                    } catch (Exception e) {
                        this.rollback(conn, error, message);
                    }
                });


            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }
    private CompletableFuture<JsonObject> insertManifestMaster(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();


        JsonObject paramRAD = new JsonObject()
                .put("created_by",     Integer.parseInt(params.getJsonObject("data").getString("created_by")))
                .put("id_type_service",     params.getJsonObject("data").getInteger("id_type_service"))
                .put("id_branchoffice",    params.getJsonObject("data").getInteger("id_branchoffice"))
                .put("id_vehicle_rad_ead",    params.getJsonObject("data").getInteger("id_vehicle_rad_ead"))
                .put("vehicle_serial_num",    params.getJsonObject("data").getString("vehicle_serial_num"))
                .put("drive_name",    params.getJsonObject("data").getString("drive_name"))
                .put("branchoffice_origin",    params.getJsonObject("data").getString("prefix"));


        String insert = this.generateGenericCreate("parcels_manifest",paramRAD);

        conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    future.complete(reply.result().toJson());
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });


        return future;
    }
    private CompletableFuture<JsonObject> insertManifestDetail(SQLConnection conn, JsonObject  params,int id_parcels_manifest ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray value =params.getJsonArray("manifest_detail");
        for (int i = 0 ; i < value.size(); i++) {
            JsonObject paramRAD = new JsonObject()
                    .put("id_parcels_manifest",    id_parcels_manifest)
                    .put("id_parcels_rad_ead",   value.getValue(i))
                    .put("created_by",    Integer.parseInt(params.getString("created_by")));
            String insert = this.generateGenericCreate("parcels_manifest_detail",paramRAD);

            conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
                try{
                    if(reply.succeeded()){
                        future.complete(reply.result().toJson());

                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });


        }

        return future;
    }
    private CompletableFuture<JsonObject> insertFolioManifest(SQLConnection conn, JsonObject  params,int id_parcels_manifest ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray value =params.getJsonArray("manifest_detail");

        String folio=params.getString("prefixService").replace('/','0')+params.getString("prefix")+id_parcels_manifest;
        String update="UPDATE parcels_manifest SET  folio = '"+folio+"', updated_by = "+  Integer.parseInt(params.getString("created_by"))+" WHERE id="+id_parcels_manifest;


        conn.update(update,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    JsonObject data=new JsonObject();
                    data.put("resolve",folio);
                    future.complete(data);

                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });



        return future;
    }
    private CompletableFuture<JsonObject> updateStatusParcelsEadRAd(SQLConnection conn, JsonObject  params,int id_parcels_manifest ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray value =params.getJsonArray("manifest_detail");
        for (int i = 0 ; i < value.size(); i++) {

            String update="UPDATE parcels_rad_ead SET  status = 2, updated_by = "+ Integer.parseInt(params.getString("created_by"))+" WHERE Id = "+value.getValue(i);

            conn.update(update,(AsyncResult<UpdateResult> reply) -> {
                try{
                    if(reply.succeeded()){
                        JsonObject data=new JsonObject();
                        data.put("resolve",true);
                        future.complete(data);

                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }


    private void getManifest(Message<JsonObject> message) {
        JsonObject body = message.body().getJsonObject("data");
        Boolean isSearch=body.getBoolean("isSearchFolio");
        String query="";
        if(isSearch){
            String folio=body.getString("folio");
            query = GET_PARCELS_MANIFEST_FOLIO +"'"+folio+"' order by id desc";
        }else {
            String dateInit = body.getString("date_init");
            String dateEnd = body.getString("date_end");
            query = GET_PARCELS_MANIFEST_DATE +"'"+dateInit+"' AND '"+dateEnd+"' order by id desc";
        }


        this.dbClient.queryWithParams(query, null, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    message.reply(new JsonArray().add(reply.result().getRows()));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }
    private void getManifestAllPackages(Message<JsonObject> message) {
        JsonObject body = message.body().getJsonObject("data");

        String folio=body.getString("folio");
        String isRad=folio.substring(0,3);
        String  query="";
        if(isRad.equals("RAD"))
            query =GET_MANIFEST_ALL_PARCELS_RAD+"'"+folio+"'";
        else
            query =GET_MANIFEST_ALL_PARCELS_EAD+"'"+folio+"'";


        this.dbClient.queryWithParams(query, null, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    message.reply(new JsonArray().add(reply.result().getRows()));

                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void updateDataService(Message<JsonObject> message) {
        JsonObject body = message.body();

        GenericQuery update = this.generateGenericUpdate("parcel_service_type", body);

        this.dbClient.updateWithParams(update.getQuery(), update.getParams(), reply -> {
            if (reply.succeeded()) {

               this.dbClient.query(QUERY_AMOUNT_SERVICES , replySelect -> {
                   if (replySelect.succeeded()) {

                       List<JsonObject> si = replySelect.result().getRows();
                       //Integer ko = si.get(0).getInteger("amount");
                       JsonObject jsRadEad = new JsonObject();
                       jsRadEad.put("amount", si.get(0).getInteger("amount"));
                       jsRadEad.put("id" , si.get(0).getInteger("id"));
                       String updateRadEad = this.generateGenericUpdateString("parcel_service_type", jsRadEad);

                       this.dbClient.query(updateRadEad , replyUpdateRadEad -> {
                          if(replyUpdateRadEad.succeeded()) {
                              message.reply(reply.succeeded());
                          }else {
                              reportQueryError(message, reply.cause());
                          }
                       });
                  //     message.reply(reply.succeeded());
                   }
               });
                // message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }


    private void update_manifest_confirmed_delivery(Message<JsonObject> message) {
        JsonObject body = message.body();
        int user_id=Integer.parseInt(body.getJsonObject("data").getJsonObject("data").getString("user_id"));
        String folio=body.getJsonObject("data").getJsonObject("data").getJsonObject("data").getString("folio");
        String type_service_manifest=body.getJsonObject("data").getJsonObject("data").getJsonObject("data").getString("type_service");
        JsonArray  parcelsDetails=body.getJsonObject("data").getJsonObject("data").getJsonArray("dataDetails");
        String date_captured=body.getJsonObject("data").getJsonObject("data").getString("date_captured");

        Integer branch = body.getJsonObject("data").getJsonObject("data").getJsonObject("data").getInteger("id_branchoffice");
        JsonArray parmsParcels=new JsonArray().add(date_captured).add(user_id).add(folio);
        AtomicInteger isCollectedRadEad= new AtomicInteger(0);

        int cash_out_id= Integer.parseInt(body.getJsonObject("data").getJsonObject("data").getString("cash_out_id"));

        this.startTransaction(message,conn -> {

            conn.updateWithParams(UPDATE_STATUS_MANIFEST_CONFIRMED, parmsParcels, reply -> {
                int numParcels=parcelsDetails.size();

                //TICKET FXC
                List<CompletableFuture<JsonObject>> FXCTasks = new ArrayList<>();

                if (reply.succeeded()) {
                    try {/**/
                        for (int i = 0; i < numParcels; i++) {
                            String update = null;
                            JsonArray parmsParcelsDetails = new JsonArray();
                            int confirmed = parcelsDetails.getJsonObject(i).getInteger("id_reason_no_rad_ead");
                            //Creamos el insert del tracking

                            JsonArray paramParcelId = new JsonArray().add(parcelsDetails.getJsonObject(i).getInteger("parcel_id"));
                            // CREATE  TICKET FXC
                            if((parcelsDetails.getJsonObject(i).getBoolean("pays_sender")!=null && !parcelsDetails.getJsonObject(i).getBoolean("pays_sender")) && confirmed==0 ) {
                                JsonObject paramTicket=new JsonObject();
                                paramTicket.put("user_id", user_id);
                                paramTicket.put("parcel_id", parcelsDetails.getJsonObject(i).getInteger("parcel_id"));
                                paramTicket.put("cash_out_id", cash_out_id);
                                paramTicket.put("total", parcelsDetails.getJsonObject(i).getDouble("total_amount"));
                                paramTicket.put("iva", parcelsDetails.getJsonObject(i).getDouble("iva"));
                                paramTicket.put("insurance_amount", parcelsDetails.getJsonObject(i).getDouble("insurance_amount"));
                                FXCTasks.add(insertTicketFXC(conn, paramTicket));
                            }


                            if (confirmed == 0) {
                                parmsParcelsDetails = new JsonArray()
                                        .add(parcelsDetails.getJsonObject(i).getString("confirmation_time_ead_rad"))
                                        .add(user_id)
                                        .add(parcelsDetails.getJsonObject(i).getInteger("id_parcels_manifest"))
                                        .add(parcelsDetails.getJsonObject(i).getInteger("id_parcels_rad_ead")
                                        );
                                update = UPDATE_STATUS_MANIFEST_DETAILS__YES_CONFIRMED;
                            } else {
                                parmsParcelsDetails = new JsonArray()
                                        .add(parcelsDetails.getJsonObject(i).getString("confirmation_time_ead_rad"))
                                        .add(parcelsDetails.getJsonObject(i).getInteger("id_reason_no_rad_ead"))
                                        .add(user_id)
                                        .add(parcelsDetails.getJsonObject(i).getInteger("id_parcels_manifest"))
                                        .add(parcelsDetails.getJsonObject(i).getInteger("id_parcels_rad_ead")
                                        );
                                update = UPDATE_STATUS_MANIFEST_DETAILS__NO_CONFIRMED;
                            }
                            int countFor = i;
                            conn.updateWithParams(update, parmsParcelsDetails, reply2 -> {

                                if (reply2.succeeded()) {

                                    JsonArray parcelsDetails1 = body.getJsonObject("data").getJsonObject("data").getJsonArray("dataDetails");
                                    int confirmed1 = parcelsDetails1.getJsonObject(countFor).getInteger("id_reason_no_rad_ead"); //Si id es 0 se confirma la entrega
                                    String type_service_parcels = parcelsDetails1.getJsonObject(countFor).getString("type_service");
                                    int isCollected = parcelsDetails1.getJsonObject(countFor).getInteger("isCollected");
                                    int packageStatus=  Integer.parseInt(parcelsDetails1.getJsonObject(countFor).getString("packageStatus"));
                                    JsonArray parmsParcelsDetails1 = new JsonArray()
                                            .add(3) // entregado
                                            .add(user_id)
                                            .add(parcelsDetails.getJsonObject(countFor).getInteger("id_parcels_rad_ead"));
                                    String updateParcelsEadRad = null;
                                    if (confirmed1 == 0) {
                                        if (type_service_parcels.equals("RAD"))
                                            updateParcelsEadRad = UPDATE_STATUS_PARCELS_RAD_EAD_YES_CONFIRME_RAD;
                                        else if (type_service_parcels.equals("RAD/EAD")) {
                                            updateParcelsEadRad = UPDATE_STATUS_PARCELS_RAD_EAD_YES_CONFIRME_RAD;
                                            if (isCollected > 0 && packageStatus>0)
                                                parmsParcelsDetails1 = new JsonArray()
                                                        .add(3)
                                                        .add(user_id)
                                                        .add(parcelsDetails.getJsonObject(countFor).getInteger("id_parcels_rad_ead"));
                                            else
                                                parmsParcelsDetails1 = new JsonArray()
                                                        .add(6) // en sucursal (no destino(paquete recolectado))
                                                        .add(user_id)
                                                        .add(parcelsDetails.getJsonObject(countFor).getInteger("id_parcels_rad_ead"));
                                        } else
                                            updateParcelsEadRad = UPDATE_STATUS_PARCELS_RAD__EAD_YES_CONFIRMED;
                                    } else {
                                        updateParcelsEadRad = UPDATE_STATUS_PARCELS_RAD__EAD_NO_CONFIRMED;
                                        parmsParcelsDetails1 = new JsonArray()
                                                .add(user_id)
                                                .add(parcelsDetails.getJsonObject(countFor).getInteger("id_parcels_rad_ead"));
                                    }
                                    conn.updateWithParams(updateParcelsEadRad, parmsParcelsDetails1, reply3 -> {

                                        if (reply3.succeeded()) {
                                            JsonArray parcelsParams = new JsonArray();
                                            if (confirmed1 == 0) {
                                                if (type_service_parcels.equals("RAD/EAD"))
                                                    if (isCollected > 0 && packageStatus>0) {
                                                        parcelsParams = new JsonArray()
                                                                .add(2) //entregado
                                                                .add(user_id)
                                                                .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));
                                                    } else {
                                                        parcelsParams = new JsonArray()
                                                                .add(7) //en sucursal no destino(recolectado)
                                                                .add(user_id)
                                                                .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));
                                                        isCollectedRadEad.set(1);
                                                    }
                                                else if (type_service_parcels.equals("EAD") || type_service_parcels.equals("RAD/EAD"))
                                                    parcelsParams = new JsonArray()
                                                            .add(2) //entregado
                                                            .add(user_id)
                                                            .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));
                                                else if (type_service_parcels.equals("RAD"))
                                                    parcelsParams = new JsonArray()
                                                            .add(7)//en sucursal no destino(recolectado)
                                                            .add(user_id)
                                                            .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));


                                                else
                                                    parcelsParams = new JsonArray()
                                                            .add(7)//en sucursal no destino
                                                            .add(user_id)
                                                            .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));
                                            } else {
                                                if (type_service_manifest.equals("EAD") && type_service_parcels.equals("RAD/EAD"))
                                                    parcelsParams = new JsonArray()
                                                            .add(9) //sucursal destino
                                                            .add(user_id)
                                                            .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));
                                                else if (type_service_manifest.equals("EAD") && (type_service_parcels.equals("EAD") || type_service_parcels.equals("RAD/EAD")))
                                                    parcelsParams = new JsonArray()
                                                            .add(9) //sucursal destino
                                                            .add(user_id)
                                                            .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));
                                                else
                                                    parcelsParams = new JsonArray()
                                                            .add(0)   //no se recolecto o entrego
                                                            .add(user_id)
                                                            .add(parcelsDetails1.getJsonObject(countFor).getString("parcel_tracking_code"));

                                            }

                                            conn.updateWithParams(UPDATE_PARCELS_STATUS__DELIVERED, parcelsParams, reply4 -> {
                                                if (reply4.succeeded()) {
                                                    this.dbClient.queryWithParams("select id ,package_status, parcel_id from parcels_packages where parcel_id = ? ", paramParcelId, replyPackage -> {
                                                        try {
                                                            if (replyPackage.succeeded()) {
                                                                List<JsonObject> resultsGet = replyPackage.result().getRows();
                                                                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                                List<CompletableFuture<JsonObject>> pPackageTask = new ArrayList<>();
                                                                if (!resultsGet.isEmpty()) {
                                                                    for (int z = 0; z < resultsGet.size(); z++) {
                                                                        JsonObject trackingObject = new JsonObject();
                                                                        trackingObject.put("parcel_id", resultsGet.get(z).getInteger("parcel_id"));
                                                                        trackingObject.put("parcel_package_id", resultsGet.get(z).getInteger("id"));
                                                                        if (confirmed1 == 0) { //Si el paquete fue entregado
                                                                            if (isCollectedRadEad.get() == 1) { //RAD/EAD
                                                                                trackingObject.put("action", "located");
                                                                                trackingObject.put("notes", "Paquete recolectado");
                                                                                trackingObject.put("package_status", 8);
                                                                            } else if (isCollected == 0 && !type_service_manifest.equals("RAD")) { //EAD
                                                                                trackingObject.put("action", "arrived");
                                                                                trackingObject.put("notes", "Paquete en camino al domicilio ");
                                                                                trackingObject.put("package_status", 9);
                                                                            } else if (isCollectedRadEad.get() == 0 && type_service_manifest.equals("RAD")) { //RAD
                                                                                trackingObject.put("action", "located");
                                                                                trackingObject.put("notes", "recolectado ");
                                                                                trackingObject.put("package_status", 8);
                                                                            } else {
                                                                                trackingObject.put("action", "delivered");
                                                                                trackingObject.put("notes", "Paquete entregado(Bitácora EAD) ");
                                                                                trackingObject.put("package_status", 2);
                                                                            }
                                                                        } else if (type_service_manifest.equals("EAD")) {
                                                                            trackingObject.put("action", "located");
                                                                            trackingObject.put("notes", "No entregado");
                                                                            trackingObject.put("package_status", 9);
                                                                        } else if (type_service_manifest.equals("RAD")) {
                                                                            trackingObject.put("action", "arrived");
                                                                            trackingObject.put("notes", "No recolectado");
                                                                            trackingObject.put("package_status", 0);
                                                                        }
                                                                        trackingObject.put("terminal_id", branch);
                                                                        trackingObject.put("is_contingency", 0);
                                                                        trackingObject.put("created_by", body.getInteger("updated_by"));
                                                                        //String insert


                                                                        pPackageTask.add(updatePackagesManifest(conn,trackingObject));
                                                                        trackingObject.remove("package_status");
                                                                        pTasks.add(insertTrackingPackage(conn,trackingObject));

                                                                    }

                                                                    CompletableFuture<Void> track = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[resultsGet.size()]));
                                                                    CompletableFuture<Void> trackTicketFXC = CompletableFuture.allOf(FXCTasks.toArray(new CompletableFuture[FXCTasks.size()]));

                                                                    track.whenComplete((s, tt) -> {
                                                                        try {
                                                                            if (tt != null) {
                                                                                throw tt;
                                                                            }

                                                                            CompletableFuture<Void> pack = CompletableFuture.allOf(pPackageTask.toArray(new CompletableFuture[resultsGet.size()]));

                                                                            pack.whenComplete((packUp, packErr) -> {
                                                                                try {
                                                                                    if (packErr != null) {
                                                                                        throw packErr;
                                                                                    }
                                                                                    if(FXCTasks.size()>0) {
                                                                                        trackTicketFXC.whenComplete((s2, tt2) -> {
                                                                                            try {
                                                                                                if (tt2 != null) {
                                                                                                    throw tt2;
                                                                                                }

                                                                                                CompletableFuture<Void> insertTicket = CompletableFuture.allOf(FXCTasks.toArray(new CompletableFuture[FXCTasks.size()]));

                                                                                                insertTicket.whenComplete((packUp2, packErr2) -> {
                                                                                                    try {
                                                                                                        if (packErr2 != null) {
                                                                                                            throw packErr2;
                                                                                                        }

                                                                                                        message.reply(reply4.succeeded());
                                                                                                        this.commit(conn, message, reply4.result().toJson());
                                                                                                    } catch (Throwable t) {
                                                                                                        this.rollback(conn, reply.cause(), message);
                                                                                                        reportQueryError(message, t.getCause());
                                                                                                    }
                                                                                                });
                                                                                                // message.reply(reply4.succeeded());

                                                                                            } catch (Throwable t) {
                                                                                                this.rollback(conn, reply.cause(), message);
                                                                                                reportQueryError(message, t.getCause());
                                                                                            }
                                                                                        });
                                                                                    }else {
                                                                                        message.reply(reply4.succeeded());
                                                                                        this.commit(conn, message, reply4.result().toJson());
                                                                                    }

                                                                                } catch (Throwable t) {
                                                                                    this.rollback(conn, reply.cause(), message);
                                                                                    reportQueryError(message, t.getCause());
                                                                                }
                                                                            });


                                                                        } catch (Throwable t) {
                                                                            this.rollback(conn, reply.cause(), message);
                                                                            reportQueryError(message, t.getCause());
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                            reportQueryError(message, e.getCause());
                                                        }
                                                    });

                                                } else {
                                                    this.rollback(conn, reply.cause(), message);
                                                    reportQueryError(message, reply4.cause());
                                                }
                                            });

                                        } else {
                                            this.rollback(conn, reply.cause(), message);
                                            reportQueryError(message, reply3.cause());
                                        }
                                    });

                                } else {
                                    reportQueryError(message, reply2.cause());
                                    this.rollback(conn, reply.cause(), message);
                                }
                            });

                        }

                    }  catch( Throwable t){
                        reportQueryError(message, reply.cause());
                    this.rollback(conn, reply.cause(), message);
                }
                } else {
                    reportQueryError(message, reply.cause());
                    this.rollback(conn, reply.cause(), message);
                }
            });

        });

    }
    private void updateManifestStatus(Message<JsonObject> message ) {

        JsonObject body = message.body();
        body.remove("reprint");
        Integer idManifest = body.getInteger("id");
        JsonArray params = new JsonArray().add(idManifest);

        JsonObject manifestObject = new JsonObject();
        manifestObject.put("id", idManifest);


        String selectManifest = "SELECT print_number FROM parcels_manifest where id = ? ";

        this.startTransaction(message,conn -> {


            conn.queryWithParams(QUERY_GET_ID_PARCELS_RAD_EAD, params , reply -> {
                try {
                    if(reply.succeeded()){
                        List<JsonObject> resultsGet = reply.result().getRows();
                        if(resultsGet.isEmpty()){
                            message.reply(new JsonArray());
                        } else {

                            for(int i = 0 ; i < resultsGet.size() ; i++ ){
                                //Parcel Manifest
                                JsonObject radEadObject = new JsonObject();
                                radEadObject.put("id",resultsGet.get(i).getInteger("preId"));
                                radEadObject.put("status", 5); //transito
                                //Parcel ead rad
                                JsonObject manifestDetail = new JsonObject();
                                manifestDetail.put("id", resultsGet.get(i).getInteger("pmdId"));
                                manifestDetail.put("confirmation_ead_rad", 0);

                                //Strings update
                                String updateRadEad = this.generateGenericUpdateString("parcels_rad_ead", radEadObject);
                                String updateManifestDetail = this.generateGenericUpdateString("parcels_manifest_detail", manifestDetail);

                                conn.query(updateRadEad,replyRadEad -> {
                                    try{
                                        if(replyRadEad.succeeded()){
                                            this.dbClient.query(updateManifestDetail, replyManifestDetail -> {
                                                try{
                                                    if(replyManifestDetail.failed()){
                                                        reportQueryError(message , reply.cause());
                                                        this.rollback(conn, replyManifestDetail.cause(), message);
                                                    }
                                                    this.dbClient.queryWithParams(selectManifest , params , replySelect -> {
                                                        try{
                                                            if(replySelect.succeeded()){

                                                                List<JsonObject> resultsSelect = replySelect.result().getRows();
                                                                if(resultsSelect.isEmpty()){
                                                                    message.reply(new JsonArray());
                                                                } else {

                                                                    Integer printNumer = resultsSelect.get(0).getInteger("print_number");

                                                                    if(printNumer > 0) {
                                                                        manifestObject.put("print_number", printNumer + 1);
                                                                        manifestObject.put("printing_date_updated_at", sdfDataBase(new Date()));
                                                                    } else {
                                                                        manifestObject.put("printing_date", sdfDataBase(new Date()));
                                                                        manifestObject.put("print_number", printNumer + 1);
                                                                    }
                                                                    String updateManifest = this.generateGenericUpdateString("parcels_manifest", manifestObject);
                                                                    conn.query(updateManifest,  replyInsert -> {
                                                                        try{
                                                                            if(replyInsert.succeeded()){
                                                                                this.commit(conn,message,new JsonObject().put("register",true));
                                                                                message.reply(new JsonObject().put("register",true));


                                                                            }else {

                                                                                this.rollback(conn, replyInsert.cause(), message);
                                                                                reportQueryError(message , reply.cause());
                                                                            }

                                                                        } catch (Exception e){

                                                                            this.rollback(conn, replyInsert.cause(),message);
                                                                            reportQueryError(message , reply.cause());
                                                                        }
                                                                    });
                                                                }

                                                            } else {
                                                                reportQueryError(message , reply.cause());
                                                            }
                                                        } catch (Exception e) {
                                                            reportQueryError(message, e.getCause());
                                                        }
                                                    });
                                                    //this.dbClient.queryWithParams(GET_PARCEL_BY_PARCEL_RAD_EAD_ID , paramsParcelId.remove(0) , replyParcelId -> {
                                                    //});
                                                }catch (Throwable t) {
                                                    reportQueryError(message , reply.cause());
                                                    this.rollback(conn, replyManifestDetail.cause() ,message);
                                                }
                                            });
                                        } else {
                                            reportQueryError(message , reply.cause());
                                            this.rollback(conn, replyRadEad.cause(),message);
                                        }
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                    }
                                });
                            }



                        }
                    }else {
                        reportQueryError(message, reply.cause());
                    }
                } catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        });
    }

    private void reprintManifest(Message<JsonObject> message){
        JsonObject body = message.body();
        body.remove("reprint");
        JsonObject manifestObject = new JsonObject();
        manifestObject.put("id", body.getInteger("id"));
        String selectManifest = "SELECT print_number FROM parcels_manifest where id =  " + body.getInteger("id");
        //manifestObject.put()

        this.startTransaction(message, (SQLConnection conn) -> {
            try{

            conn.query(selectManifest, replySelect -> {
               try{
                   if(replySelect.succeeded()) {

                       List<JsonObject> resultsSelect = replySelect.result().getRows();
                       if(resultsSelect.isEmpty()){
                           message.reply(new JsonArray());
                       } else {

                           Integer printNumer = resultsSelect.get(0).getInteger("print_number");

                           if(printNumer > 0) {
                               manifestObject.put("print_number", printNumer + 1);
                               manifestObject.put("printing_date_updated_at", sdfDataBase(new Date()));
                           } else {
                               manifestObject.put("printing_date", sdfDataBase(new Date()));
                               manifestObject.put("print_number", printNumer + 1);
                           }

                           String updateManifest = this.generateGenericUpdateString("parcels_manifest", manifestObject);

                           conn.query(updateManifest, reply -> {
                               try{
                                   if(reply.succeeded()){
                                       this.commit(conn,message,new JsonObject().put("registro",true));
                                       message.reply(new JsonObject().put("registro",true));
                                   }else {
                                       this.rollback(conn, reply.cause(), message);
                                       reportQueryError(message, replySelect.cause());
                                   }
                               } catch (Exception e){
                                   this.rollback(conn, reply.cause(), message);
                                   reportQueryError(message, replySelect.cause());
                               }
                           });
                       }
                   } else {
                       this.rollback(conn,replySelect.cause() , message);
                       reportQueryError(message, replySelect.cause());
                   }
               } catch (Exception e){
                   this.rollback(conn,e , message);
                   reportQueryError(message, replySelect.cause());

               }
            });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn,t , message);
            }
        });

    }
    private void getManifestCancel(Message<JsonObject> message) {
        String folio  = message.body().getString("data");
        String query="";
        query = GET_PARCELS_MANIFEST_CANCEL_FOLIO +"'"+folio+"' order by p.id desc";



        this.dbClient.queryWithParams(query, null, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    message.reply(new JsonArray().add(reply.result().getRows()));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }
    private void cancelManifest(Message<JsonObject> message) {
        JsonObject body  = message.body();

        String folio=body.getString("folio");
        int parcelManifestId = body.getInteger("id_parcels_manifest");

        JsonArray paramsManifest=  new JsonArray().add(folio);
        this.startTransaction(message, (SQLConnection conn) -> {
            conn.updateWithParams(UPDATE_PARCELS_MANIFEST_CANCEL, paramsManifest, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().getUpdated()==0) {
                        reportQueryError(message, reply.cause());
                    } else {
                        this.dbClient.queryWithParams(GET_PARCELS_MANIFEST_DETAILS_CANCEL, new JsonArray().add(parcelManifestId), reply2 -> {
                            try {
                                if (reply2.failed()) {
                                    throw reply2.cause();
                                }
                                List<JsonObject> details = reply2.result().getRows();
                                if (details.isEmpty()) {
                                    throw new Exception("Parcel manifest details not found");
                                }

                                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                                for (JsonObject detail : details) {
                                    Integer parcelManifestDetailId = detail.getInteger(ID);
//                                    Integer parcelRadEadId = detail.getInteger("id_parcels_rad_ead");

                                    tasks.add(updateParcelsManifestDetailsCancel(parcelManifestDetailId));
//                                    tasks.add(updatePacelsRadEadCancel(parcelRadEadId));
                                }
                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                                    try {
                                        if (err != null) {
                                            throw err;
                                        }
                                        this.commit(conn,message,reply2.result().toJson());
                                    } catch (Throwable t) {
                                        this.rollback(conn,t , message);
                                        reportQueryError(message, t.getCause());
                                    }
                                });

                            } catch (Throwable t) {
                                this.rollback(conn, t, message);
                            }
                        });
                    }
                } else {
                    this.rollback(conn, reply.cause(), message);
                }
            });
        });

    }
    private CompletableFuture<JsonObject> updateParcelsManifestDetailsCancel (Integer parcelManifestDetailId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            String update = UPDATE_PARCELS_MANIFEST_DETAILS_CANCEL+parcelManifestDetailId;

            this.dbClient.query(update , replyTracking -> {
                try{
                    if(replyTracking.succeeded()){
                        future.complete(new JsonObject().put("value","true"));
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }
    private CompletableFuture<JsonObject> updatePacelsRadEadCancel (Integer parcelRadEadId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            String update = UPDATE_PARCELS_RAD_EAD_CANCEL+parcelRadEadId;

            this.dbClient.query(update , replyTracking -> {
                try{
                    if(replyTracking.succeeded()){
                        future.complete(new JsonObject().put("value","true"));
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertTrackingPackage (SQLConnection conn,JsonObject objTracking){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            String insertTracking = this.generateGenericCreate("parcels_packages_tracking" , objTracking);

            conn.query(insertTracking , replyTracking -> {
               try{
                   if(replyTracking.succeeded()){
                    future.complete(objTracking);
                   }
               } catch (Throwable t){
                   t.printStackTrace();
                   future.completeExceptionally(t);
               }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private CompletableFuture<JsonObject> updatePackagesManifest (SQLConnection conn,JsonObject packages){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            JsonObject objPackage = new JsonObject();
            objPackage.put("id", packages.getInteger("parcel_package_id"));
            objPackage.put("package_status" ,  packages.getInteger("package_status"));

            String updatePackage = this.generateGenericUpdateString("parcels_packages" , objPackage);

            conn.query(updatePackage, replyUpdatePackage -> {
               try{
                   if(replyUpdatePackage.succeeded()){
                       future.complete(objPackage);
                   }
               } catch (Throwable t){
                   t.printStackTrace();
                   future.completeExceptionally(t);
               }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return  future;
    }
    private void checkStatusFXC(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer id_parcels=body.getJsonObject("data").getInteger("parcel_id");
        JsonArray params = new JsonArray()
                .add(id_parcels);
        this.dbClient.queryWithParams(GET_CHECK_STATUS_FXC, params, reply -> {
            if (reply.succeeded()) {

                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {


                    message.reply(new JsonArray().add(reply.result().getRows()));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
     });
    }

    private void getPackagesFXC(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer id_parcels=body.getInteger("data");
        JsonArray params = new JsonArray()
                .add(id_parcels);
        this.dbClient.queryWithParams(GET_PARCEL_FXC, params, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    JsonArray parcels= new JsonArray();
                    parcels.add( new JsonObject().put("parcels",reply.result().getRows()));
                    this.dbClient.queryWithParams(GET_PACKAGES_FXC, params, reply2 -> {
                        if (reply.succeeded()) {

                            if (reply.result().getNumRows() == 0) {
                                message.reply(null);
                            } else {

                                parcels.add( new JsonObject().put("packages",reply2.result().getRows()));
                                message.reply(parcels);
                            }
                        } else {
                            reportQueryError(message, reply.cause());
                        }
                    });
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }
    private CompletableFuture<JsonObject> insertTicketFXC (SQLConnection conn,JsonObject objTicket){

      CompletableFuture<JsonObject> future = new CompletableFuture<>();

      JsonObject ticket = new JsonObject();
        // Create ticket_code
        ticket.put("created_by", objTicket.getInteger("user_id"));
        ticket.put("parcel_id", objTicket.getInteger("parcel_id"));
        ticket.put("action", "purchase");
        ticket.put("ticket_code", UtilsID.generateID("T"));
        ticket.put("cash_out_id", objTicket.getInteger("cash_out_id"));
        ticket.put("total", objTicket.getDouble("total"));
        ticket.put("iva", objTicket.getDouble("iva"));
        ticket.put("paid", objTicket.getDouble("total"));
        ticket.put("paid_change", 0.00);

        try{
            String insertTicket = this.generateGenericCreate("tickets" , ticket);

            conn.update(insertTicket , (AsyncResult<UpdateResult> replyTicket)-> {
                try{
                    if(replyTicket.succeeded()){
                        final int idTicket = replyTicket.result().getKeys().getInteger(0);
                        JsonArray parm = new JsonArray().add(objTicket.getInteger("parcel_id"));
                        this.dbClient.queryWithParams(GET_PACKAGES_FXC,parm,replyPackages->{
                            try {
                                if (replyPackages.succeeded()) {
                                    this.dbClient.queryWithParams(GET_PACKINGS_FXC, parm, replyPackings -> {
                                        try {
                                            if (replyPackings.succeeded()) {
                                                this.dbClient.queryWithParams(GET_SERVICE_FXC, parm, replyService -> {
                                                            try {
                                                                if (replyPackings.succeeded()) {
                                                                    List<JsonObject> resultsService = replyService.result().getRows();

                                                                    List<JsonObject> resultsPackings = replyPackings.result().getRows();

                                                                    List<JsonObject> resultsPackages = replyPackages.result().getRows();

                                                                    List<CompletableFuture<JsonObject>> ticketDetailTasks = new ArrayList<>();
                                                                    //ticket detail servicio
                                                                    for (int  i=0;i<resultsService.size();i++ ){
                                                                        JsonObject ticketDetails = new JsonObject();
                                                                        ticketDetails.put("created_by", objTicket.getInteger("user_id"));
                                                                        ticketDetails.put("ticket_id", idTicket);
                                                                        ticketDetails.put("quantity", 1);
                                                                        ticketDetails.put("detail", resultsService.get(i).getString("type_service"));
                                                                        ticketDetails.put("unit_price", resultsService.get(i).getDouble("amount") );
                                                                        ticketDetails.put("discount", 0);
                                                                        ticketDetails.put("amount",resultsService.get(i).getDouble("amount"));
                                                                        ticketDetailTasks.add(insertTicketDetailsFXC(conn, ticketDetails));
                                                                    }
                                                                    // Create ticket_code Packings
                                                                    for (int  i=0;i<resultsPackings.size();i++ ){
                                                                        JsonObject ticketDetails = new JsonObject();
                                                                        ticketDetails.put("created_by", objTicket.getInteger("user_id"));
                                                                        ticketDetails.put("ticket_id", idTicket);
                                                                        ticketDetails.put("quantity", resultsPackings.get(i).getDouble("quantity"));
                                                                        ticketDetails.put("detail", "Embalaje");
                                                                        ticketDetails.put("unit_price", resultsPackings.get(i).getDouble("unit_price") );
                                                                        ticketDetails.put("discount", resultsPackings.get(i).getDouble("discount"));
                                                                        ticketDetails.put("amount",resultsPackings.get(i).getDouble("total_amount"));
                                                                        ticketDetailTasks.add(insertTicketDetailsFXC(conn, ticketDetails));
                                                                    }
                                                                    //ticket detalle seguro
                                                                    if(objTicket.getDouble("insurance_amount")>0){

                                                                        JsonObject ticketDetails = new JsonObject();
                                                                        ticketDetails.put("created_by", objTicket.getInteger("user_id"));
                                                                        ticketDetails.put("ticket_id", idTicket);
                                                                        ticketDetails.put("quantity", 1);
                                                                        ticketDetails.put("detail", "seguro");

                                                                        ticketDetails.put("unit_price",objTicket.getDouble("insurance_amount")  );
                                                                        ticketDetails.put("discount", 0);
                                                                        ticketDetails.put("amount",objTicket.getDouble("insurance_amount")  );
                                                                        ticketDetailTasks.add(insertTicketDetailsFXC(conn, ticketDetails));
                                                                    }
                                                                    List<JsonObject> resultPP = replyPackages.result().getRows();
                                                                    Map<String, List<JsonObject>> groupedPackages = resultsPackages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString("shipping_type")));
                                                                    JsonArray details = new JsonArray();

                                                                    for (String s : groupedPackages.keySet()) {
                                                                        JsonObject packagePrice = new JsonObject();
                                                                        AtomicReference<Integer> quantity = new AtomicReference<>(0);
                                                                        AtomicReference<Double> unitPrice = new AtomicReference<>(0.00);
                                                                        AtomicReference<Double> amount = new AtomicReference<>(0.00);
                                                                        Optional<JsonObject> packageName = resultPP.stream().filter(x -> x.getInteger("package_price_id").equals(groupedPackages.get(s).get(0).getInteger("package_price_id"))).findFirst();
                                                                        String packageRange = packageName.get().getString("name_price");
                                                                        groupedPackages.get(s).forEach(x -> {
                                                                            quantity.getAndSet(quantity.get() + 1);
                                                                            unitPrice.updateAndGet(v -> v + x.getDouble("total_amount"));
                                                                            amount.updateAndGet(v -> v + x.getDouble("total_amount"));
                                                                            packagePrice.put(DISCOUNT, x.getDouble(DISCOUNT));
                                                                        });
                                                                        packagePrice.put("shipping_type", s);
                                                                        packagePrice.put("unit_price", unitPrice.get());
                                                                        packagePrice.put("amount", amount.get());
                                                                        packagePrice.put("quantity", quantity.get());
                                                                        if(packagePrice.getInteger("quantity") != null){
                                                                            if(packagePrice.getInteger("quantity") > 0){
                                                                                JsonObject ticketDetail = new JsonObject();
                                                                                JsonObject packageDetail = packagePrice;
                                                                                String shippingType = packageDetail.getString("shipping_type");
                                                                                switch (shippingType){
                                                                                    case "parcel":
                                                                                        shippingType = "paquetería";
                                                                                        break;
                                                                                    case "courier":
                                                                                        shippingType = "mensajería";
                                                                                        break;
                                                                                    case "pets":
                                                                                        shippingType = "mascota";
                                                                                        break;
                                                                                    case "frozen":
                                                                                        shippingType = "carga refrigerada";
                                                                                        break;
                                                                                }
                                                                                ticketDetail.put("ticket_id", idTicket);
                                                                                ticketDetail.put("quantity", packageDetail.getInteger("quantity"));
                                                                                ticketDetail.put("detail", "Envío de " + shippingType + " con rango " + packageRange);
                                                                                ticketDetail.put("unit_price", packageDetail.getDouble("unit_price"));
                                                                                ticketDetail.put(DISCOUNT, packageDetail.getDouble(DISCOUNT));
                                                                                ticketDetail.put("amount", packageDetail.getDouble("amount"));
                                                                                ticketDetail.put("created_by", objTicket.getInteger("user_id"));
                                                                                ticketDetailTasks.add(insertTicketDetailsFXC(conn, ticketDetail));
                                                                            }
                                                                        }
                                                                    }

                                                                    JsonObject payment = new JsonObject();
                                                                    payment.put("created_by", objTicket.getInteger("user_id"));
                                                                    payment.put("parcel_id", objTicket.getInteger("parcel_id"));
                                                                    payment.put("payment_method_id", 1);
                                                                    payment.put("payment_method", "cash");
                                                                    payment.put("amount", ticket.getDouble("total"));
                                                                    payment.put("currency_id", 22);
                                                                    payment.put("ticket_id", idTicket);
                                                                    CompletableFuture<Void> trackTicketFXC = CompletableFuture.allOf(ticketDetailTasks.toArray(new CompletableFuture[ticketDetailTasks.size()]));
                                                                    trackTicketFXC.whenComplete((s2, tt2) -> {
                                                                        try {
                                                                            if (tt2 != null) {
                                                                                throw tt2;
                                                                            }

                                                                            CompletableFuture<Void> insertTicketDetails = CompletableFuture.allOf(ticketDetailTasks.toArray(new CompletableFuture[ticketDetailTasks.size()]));

                                                                            insertTicketDetails.whenComplete((packUp2, packErr2) -> {
                                                                                try {
                                                                                    if (packErr2 != null) {
                                                                                        throw packErr2;
                                                                                    }

                                                                                    this.insertPaymentCashOutMoveFXC(conn,payment,objTicket.getInteger("cash_out_id")).whenComplete((res, error) -> {

                                                                                        try {
                                                                                            if(error != null){
                                                                                                future.completeExceptionally(error.getCause());
                                                                                                System.out.println("errror masiso");
                                                                                            } else {
                                                                                                future.complete(new JsonObject().put("ok",true));

                                                                                            }

                                                                                        } catch (Exception t) {

                                                                                            future.completeExceptionally(t.getCause());
                                                                                        }
                                                                                    });


                                                                                } catch (Throwable t) {
                                                                                    future.completeExceptionally(t.getCause());
                                                                                }
                                                                            });

                                                                        } catch (Throwable t) {
                                                                            future.completeExceptionally(t.getCause());

                                                                        }
                                                                    });

                                                                }else{
                                                                    future.completeExceptionally(replyService.cause());
                                                                }
                                                             } catch (Throwable t) {
                                                                  future.completeExceptionally(t.getCause());

                                                            }
                                                });


                                            } else {
                                                future.completeExceptionally(replyPackings.cause());
                                            }
                                        } catch (Throwable t) {
                                            future.completeExceptionally(t.getCause());

                                        }
                                    });

                                }
                                else{
                                    future.completeExceptionally(replyPackages.cause());
                                }
                            } catch (Throwable t) {
                                future.completeExceptionally(t.getCause());

                            }
                        });
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }
    private CompletableFuture<JsonObject> insertTicketDetailsFXC (SQLConnection conn,JsonObject objTicketDetails){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            String insertTicketDetails = this.generateGenericCreate("tickets_details" , objTicketDetails);
            conn.query(insertTicketDetails , replyTicket -> {
                try{
                    if(replyTicket.succeeded()){
                        future.complete(objTicketDetails);
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentCashOutMoveFXC (SQLConnection conn,JsonObject objPayment,int cash_out_id){

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            String insertPayment = this.generateGenericCreate("payment" , objPayment);
                JsonObject objCashOutMove= objPayment.copy() ;
            conn.update(insertPayment , replyPayment -> {
                try{
                    if(replyPayment.succeeded()){

                        final int idPayment = replyPayment.result().getKeys().getInteger(0);
                        JsonObject cashOutMove = new JsonObject();
                        // Create ticket_code
                        cashOutMove.put("created_by", objCashOutMove.getInteger("created_by"));
                        cashOutMove.put("cash_out_id",cash_out_id);
                        cashOutMove.put("payment_id", idPayment);
                        cashOutMove.put("quantity", objCashOutMove.getDouble("amount"));
                        cashOutMove.put("move_type", "0");
                        String insertCashOut = this.generateGenericCreate("cash_out_move" , cashOutMove);
                        conn.query(insertCashOut , replyCashOut -> {
                            try{
                                if(replyCashOut.succeeded()){
                                    future.complete(new JsonObject().put("ok",true));
                                }else {
                                    System.out.println("failes");
                                    future.completeExceptionally(replyCashOut.cause().getCause());
                                }
                            } catch (Throwable t){
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });
                    }else{
                        future.complete(new JsonObject().put("error",true));
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private void getCustomersEadRad(Message<JsonObject> message){

        try {

            String QUERY = QUERY_CUSTOMERS_EAD_RAD;

            this.dbClient.query(QUERY, reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> customers = reply.result().getRows();
                    if (customers.isEmpty()){
                        throw new Exception("Customers not found");
                    }
                    message.reply(new JsonArray(customers));

                }  catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void registerCustomerEadRad(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            try{

                JsonObject copy = message.body().copy();
                copy.put(CREATED_AT,sdfDataBase(new Date()));

                conn.queryWithParams("SELECT id FROM customer_rad_ead WHERE customer_id = ?", new JsonArray().add(copy.getInteger("customer_id")) , replySelect -> {
                   try{
                       if (replySelect.failed()){
                           throw replySelect.cause();
                       }
                       List<JsonObject> result = replySelect.result().getRows();
                       if (result.isEmpty()){
                           this.insertCustomerEadRad(conn,copy).whenComplete((res,error) -> {
                               try{
                                   if(error != null){
                                       this.rollback(conn, error, message);
                                   } else {

                                       int customerID = res.getJsonArray("keys").getInteger(0);
                                       String QUERY = QUERY_CUSTOMERS_EAD_RAD;
                                       QUERY = QUERY.concat(" AND cre.id = ?");
                                       conn.queryWithParams(QUERY , new JsonArray().add(customerID) , resultSelect -> {
                                           List<JsonObject> responseResult = resultSelect.result().getRows();
                                           if(responseResult.isEmpty()){
                                               this.rollback(conn,error,message);
                                           } else {
                                               this.commit(conn,message , new JsonObject().put("customer", responseResult));
                                           }
                                       });

                                   }
                               } catch (Exception e){
                                   this.rollback(conn,error,message);
                               }
                           });
                       } else {
                           throw new Exception("Cliente ya se encuentra agregado a la BD");
                       }

                   }catch (Throwable t){
                       t.printStackTrace();
                       reportQueryError(message, t);
                   }
                });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn,t , message);
            }
        });
    }

    private CompletableFuture<JsonObject> insertCustomerEadRad(SQLConnection conn, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("customer_rad_ead",params);

        conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    future.complete(reply.result().toJson());
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void updateCustomerEadRad(Message<JsonObject> message) {
        JsonObject body = message.body();

        GenericQuery update = this.generateGenericUpdate("customer_rad_ead", body);

        this.dbClient.updateWithParams(update.getQuery(), update.getParams(), reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    /* Status
   * parcels_manifest status:
    1:ENTRANSITO
    2:Documentado
    3:Entregado
    4:cancelado

   * parcels_manifest_detail status:
    1:ENTRANSITO
    2:Documentado
    3:Entregado
    4:cancelado
    5:NoEntregado

    parcels_manifest_detail confirmation_ead_rad:
    0:Documentado
    1:Entregado
    2:NoEntregado

   * parcels_rad_ead status:
    1:documentadoPOS (Disponible para agregar a un manifest(bitacora))
    2:DocumentadoEnManifest
    3:Entregado
    4:NoEntregado(Intentar nuevo entrega)
    5:ENTRANSITO
    6:recolectado
    7:CANCELADO
    8:CONTIGENCIA
    *

  *parcels
   0:documentado
   1:en transito
   2:entregado
   4:cancelado
   8:sucursal no destino
   9:enSucursalDestino



    */
    private static final String UPDATE_PARCELS_RAD_EAD_CANCEL = " UPDATE  parcels_rad_ead SET status = 4 where id = "; // al cancelar  manifiesto el parcels_rad_ead cambia status 1 para que pueda ser documentado de nuevo en un manifesto.
    private static final String GET_PARCELS_MANIFEST_DETAILS_CANCEL = "SELECT id,id_parcels_rad_ead FROM parcels_manifest_detail  where id_parcels_manifest = ? ";
    private static final String UPDATE_PARCELS_MANIFEST_DETAILS_CANCEL = "UPDATE  parcels_manifest_detail SET status = 4 where id =  ";
    private static final String UPDATE_PARCELS_MANIFEST_CANCEL = "UPDATE  parcels_manifest SET status = 4  where folio = ? and status != 3";
    private static final String GET_PARCELS_MANIFEST_CANCEL_FOLIO = "SELECT p.id as \"id_parcels_manifest\" , p.date_captured, p.drive_name as \"driver\",p.folio, s.type_service,p.vehicle_serial_num as \"name\", p.branchoffice_origin as \"prefix\",p.status, p.created_at,p.printing_date,p.printing_date_updated_at,p.print_number, p.updated_at FROM parcels_manifest as p\n" +
            "inner join branchoffice as b  on p.id_branchoffice=b.id \n" +
            "inner join parcel_service_type as s on p.id_type_service=s.id\n" +
            "inner join vehicle_rad_ead as v on v.id=p.id_vehicle_rad_ead\n" +
            "inner join vehicle as vv on vv.id=v.id_vehicle\n" +
            "inner join employee as em on em.id=v.id_employee"+
            " where  (p.status=2) AND p.folio=";
    private static final String UPDATE_PARCELS_STATUS__DELIVERED = "UPDATE parcels \n " +
            " SET  parcel_status = ?,\n " +
            "updated_by = ? \n " +
            " WHERE parcel_tracking_code = ? AND (status != 4);";
    private static final String UPDATE_STATUS_PARCELS_RAD_EAD_YES_CONFIRME_RAD = "UPDATE parcels_rad_ead \n " +
            " SET  status = ? ,\n " +
            "confirme_rad = 1 ,\n "+
            "updated_by = ? \n " +
            " WHERE id = ? AND (status = 5 or status = 6 );";
    private static final String UPDATE_STATUS_PARCELS_RAD__EAD_YES_CONFIRMED = "UPDATE parcels_rad_ead \n " +
            " SET  status = ? ,\n " +
            "updated_by = ? \n " +
            " WHERE id = ? AND (status = 5 OR status = 4 );";
    private static final String UPDATE_STATUS_PARCELS_RAD__EAD_NO_CONFIRMED = "UPDATE parcels_rad_ead \n" +
            " SET  status = 4 ,\n" +
            "updated_by = ? \n" +
            " WHERE id = ? AND (status = 5 OR status = 4  );";
    private static final String UPDATE_STATUS_MANIFEST_DETAILS__YES_CONFIRMED = "UPDATE parcels_manifest_detail \n" +
            " SET  status = 2 ,\n" +
            "confirmation_time_ead_rad = ?,"+
            "confirmation_ead_rad = 1,"+
            "updated_by = ? \n" +
            " WHERE id_parcels_manifest = ? AND id_parcels_rad_ead = ? ;";
    private static final String UPDATE_STATUS_MANIFEST_DETAILS__NO_CONFIRMED = "UPDATE parcels_manifest_detail \n" +
            " SET  status = 3 ,\n" +
            "confirmation_time_ead_rad = ?,"+
            "confirmation_ead_rad = 2,"+
            "id_reason_no_rad_ead = ?, "+
            "updated_by = ? \n" +
            " WHERE id_parcels_manifest = ? AND id_parcels_rad_ead = ? ;";
    private static final String UPDATE_STATUS_MANIFEST_CONFIRMED = "UPDATE parcels_manifest \n" +
            "SET date_captured = ? \n,"+
            "status = 3 ,\n" +
            "updated_by = ?\n" +
            " WHERE folio = ?;";
    private static final String GET_MANIFEST_ALL_PARCELS_RAD = "SELECT \n" +
            "pre.confirme_rad as isCollected,"+
            "pst.type_service, pmd.confirmation_ead_rad,pmd.confirmation_time_ead_rad,pmd.id_reason_no_rad_ead ,pmd.status ,"+
            "pmd.id_parcels_rad_ead,"+
            "pmd.id_parcels_manifest,"+
            "pre.parcel_id,"+
            "CONCAT(par.sender_name,par.sender_last_name) as \"nombreClienteRAD\" ,\n" +
            "ca2.address as \"direccionRecoleccion\", \n" +
            "ca2.reference ,"+
            "ca.reference, "+
            "par.sender_phone as \"telefono\",\n" +
            " CONCAT(em.name,\" \",em.last_name) as \"quienSolicita\" ,\n" +
            "ca.address as \"adondeEnvia\", \n" +
            "pm.folio,\n" +
            "(SELECT   GROUP_CONCAT( DISTINCT( pp.contains) )  FROM parcels_packages as pp where pp.parcel_id=par.id) as \"queEnvia\" ,\n" +
            "(SELECT   GROUP_CONCAT( DISTINCT( pp.package_status) )  FROM parcels_packages as pp where pp.parcel_id=par.id) as \"packageStatus\" ,\n" +
            "par.total_packages,"+
            "par.parcel_tracking_code,"+
            " par.created_at as \"fechaCaptura\"\n" +
            " FROM parcels_rad_ead AS pre\n" +
            "INNER JOIN parcels_manifest_detail AS pmd ON pmd.id_parcels_rad_ead=pre.id \n" +
            "INNER JOIN parcels_manifest AS pm ON pm.id=pmd.id_parcels_manifest\n" +
            "INNER JOIN parcels AS par ON par.id=pre.parcel_id\n" +
            "INNER JOIN customer_addresses AS ca ON ca.id=par.addressee_address_id\n" +
            "INNER JOIN customer_addresses AS ca2 ON ca2.id=par.sender_address_id\n" +
            "INNER JOIN employee AS em ON em.user_id=par.created_by\n" +
            "INNER JOIN parcel_service_type AS pst ON pst.id=pre.id_type_service"+
            "\n" +
            "WHERE pm.folio=";

    private static final String GET_MANIFEST_ALL_PARCELS_EAD = "SELECT  pst.type_service, pmd.confirmation_ead_rad, pmd.confirmation_time_ead_rad,pmd.id_reason_no_rad_ead ,pmd.status ," +
            "pre.confirme_rad as isCollected,"+
            "pmd.id_parcels_rad_ead,"+
            "pmd.id_parcels_manifest,"+
            " pre.parcel_id,pre.id,CONCAT(em2.name,\" \",em2.last_name) as \"colaboradorVehicle\",\n" +
            "CONCAT(par.addressee_name,par.addressee_last_name) as \"nombreClienteDestino\" ,\n" +
            "ca.address as \"direccion\",\n" +
            "ca.reference, \n" +
            "par.pays_sender,\n" +
            "par.total_packages,\n" +
            "par.addressee_phone as \"telefono\",\n" +
            "pm.folio,\n" +
            "par.total_packages,"+
            "par.total_amount,"+
            "par.iva,"+
            "par.insurance_amount,"+
            "par.parcel_tracking_code,"+
            "(SELECT   GROUP_CONCAT( DISTINCT( pp.contains))  FROM parcels_packages as pp where pp.parcel_id=par.id) as \"queEnvia\" ,\n" +
            "(SELECT   GROUP_CONCAT( DISTINCT( pp.package_status) )  FROM parcels_packages as pp where pp.parcel_id=par.id) as \"packageStatus\" ,\n" +
            " par.created_at as \"fechaCaptura\"\n" +
            " FROM parcels_rad_ead AS pre\n" +
            "INNER JOIN parcels_manifest_detail AS pmd ON pmd.id_parcels_rad_ead=pre.id \n" +
            "INNER JOIN parcels_manifest AS pm ON pm.id=pmd.id_parcels_manifest\n" +
            "INNER JOIN parcels AS par ON par.id=pre.parcel_id\n" +
            "INNER JOIN customer_addresses AS ca ON ca.id=par.addressee_address_id\n" +
            "INNER JOIN employee AS em ON em.user_id=par.created_by\n" +
            "INNER JOIN employee AS em2 \n" +
            "INNER JOIN vehicle_rad_ead AS vea ON vea.id=pm.id_vehicle_rad_ead AND vea.id_employee=em2.id\n" +
            "INNER JOIN parcel_service_type AS pst ON pst.id=pre.id_type_service \n"+
            "WHERE pm.folio=";

    private static final String GET_PARCELS_MANIFEST_FOLIO = "SELECT p.date_captured, p.drive_name as \"driver\",p.id,p.folio, s.type_service,p.vehicle_serial_num as \"name\", p.branchoffice_origin as \"prefix\",p.id_branchoffice ,p.status, p.created_at,p.printing_date,p.printing_date_updated_at,p.print_number, p.updated_at, p.parcels_manifest_ccp_id FROM parcels_manifest as p\n" +
            "inner join branchoffice as b  on p.id_branchoffice=b.id \n" +
            "inner join parcel_service_type as s on p.id_type_service=s.id\n" +
            "inner join vehicle_rad_ead as v on v.id=p.id_vehicle_rad_ead\n" +
            "inner join vehicle as vv on vv.id=v.id_vehicle\n" +
            "inner join employee as em on em.id=v.id_employee"+
            " where  (p.status!=4) AND p.folio=";
    private static final String GET_PARCELS_MANIFEST_DATE = "SELECT p.date_captured, p.drive_name as \"driver\",p.id,p.folio, s.type_service,p.vehicle_serial_num as \"name\", p.branchoffice_origin as \"prefix\", p.id_branchoffice,p.status,p.created_at,p.printing_date,p.printing_date_updated_at,p.print_number, p.updated_at, p.parcels_manifest_ccp_id FROM parcels_manifest as p\n" +
            "inner join branchoffice as b  on p.id_branchoffice=b.id \n" +
            "inner join parcel_service_type as s on p.id_type_service=s.id\n" +
            "inner join vehicle_rad_ead as v on v.id=p.id_vehicle_rad_ead\n" +
            "inner join vehicle as vv on vv.id=v.id_vehicle\n" +
            "inner join employee as em on em.id=v.id_employee"+
            " where  (p.status!=4) AND   DATE_FORMAT(p.created_at, \"%Y-%m-%d\") BETWEEN ";
    private static final String GET_SERVICE_RAD_EAD = "SELECT distinct * FROM  parcel_service_type where status=1 AND (type_service='EAD' OR type_service='RAD/OCU')";
    private static final String GET_SERVICE_RAD_EAD_POS = "SELECT * FROM parcel_service_type";
    private static final String GET_PARCELS_EAD = "    SELECT pic.tipo_cfdi, p.id as \"id_parcel\", pic.status_cfdi,pp.collection_attempts, ps.type_service, pp.id,pp.status,p.total_packages,p.parcel_tracking_code,pp.zip_code,pp.created_at,pic.id as parcel_invoice_complement_id,pic.cfdi_body\n" +
            "                         FROM\n" +
            "                        parcels_rad_ead as pp\n" +
            "                         inner join parcels as p on p.id=pp.parcel_id \n" +
            "                         inner join parcel_service_type as ps on pp.id_type_service=ps.id \n" +
            "                         inner join parcel_invoice_complement as pic on pic.id_parcel=p.id\n" +
            "                         where \n" +
            "                         (pp.status=1 OR pp.status=4 OR pp.status=6 OR pp.status=8 )\n" +
            "                         AND ( p.parcel_status=9 )\n" +
            "                         AND\n" +
            "                         pp.confirme_rad=1\n" +
            "                         AND\n" +
            "                         (\n" +
            "\t\tps.type_service=\"RAD/EAD\" OR  ps.type_service=\"EAD\")\n" +
            "        AND (select count(id_parcel) from parcel_invoice_complement where status_cfdi=\"timbrado\" and status_cfdi=\"documentado\" and tipo_cfdi=\"traslado sin complemento carta porte\" and p.id=id_parcel)=0\n" +
            "        AND pic.tipo_cfdi != \"traslado sin complemento carta porte\"\n" +
            "        AND ";
    private static final String GET_PARCELS_RAD = "SELECT pp.collection_attempts, ps.type_service, pp.id,pp.status,p.total_packages,p.parcel_tracking_code,pp.zip_code,pp.created_at\n" +
            " FROM\n" +
            " parcels_rad_ead as pp\n" +
            " inner join parcels as p on p.id=pp.parcel_id \n" +
            " inner join parcel_service_type as ps on pp.id_type_service=ps.id \n" +
            " where \n" +
            " (pp.status=1 OR pp.status=4 )\n" +
            " AND (p.parcel_status=0 OR p.parcel_status=9 )\n" +
            " AND\n" +
            " pp.confirme_rad=1\n" +
            " AND\n" +
            " (\n" +
            " ps.type_service=\"RAD\" OR ps.type_service=\"RAD/EAD\" OR ps.type_service=\"RAD/OCU\")\n" +
            " AND\n ";
    private static final String UPDATE_STATUS_SCHEDULE = "UPDATE schedule_rad_ead \n" +
            " SET  status = ? ,\n" +
            "updated_by = ?\n" +
            " WHERE Id = ?;";
    private static final String GET_SCHEDULES_RAD_EAD = "SELECT * FROM schedule_rad_ead where status!=4";
    private static final String GET_ATTEMPT_REASON = "select id,name,status,created_at , created_by from delivery_attempt_reason";
    private static final String QUERY_UPDATE_ATTEMPT_REASON = "update delivery_attempt_reason set status = ? , updated_at = ? ,updated_by = ?  where id = ?";
    private static final String QUERY_GET_ID_PARCELS_RAD_EAD = "select \n" +
            "pm.id ,\n" +
            "pmd.id as pmdId,\n" +
            "pre.id as preId ,\n" +
            "pre.parcel_id as parcelId\n" +
            "from parcels_manifest pm \n" +
            "inner join parcels_manifest_detail pmd ON pm.id = pmd.id_parcels_manifest\n" +
            "inner join parcels_rad_ead pre ON pmd.id_parcels_rad_ead = pre.id\n" +
            "where pm.id = ?";
    private static final String GET_PARCEL_BY_PARCEL_RAD_EAD_ID = "select \n" +
            "parcel_id\n" +
            "from parcels_rad_ead pre\n" +
            "where id = ? ";

    private static final String QUERY_AMOUNT_SERVICES = "select id, (select sum(amount) \n" +
            "from parcel_service_type\n" +
            "where type_service = 'RAD/OCU' OR type_service = 'EAD'\n" +
            ") as amount\n" +
            "from parcel_service_type where type_service = 'RAD/EAD'";
    private static final String GET_CHECK_STATUS_FXC = "SELECT id,parcel_status FROM parcels where id=? AND parcel_status=11 ";
    private static final String GET_PARCEL_FXC = "SELECT id,total_packages,discount,amount,extra_charges,total_amount  FROM parcels where id=?";
    private static final String GET_PACKAGES_FXC = "SELECT  pp.name_price, p.package_price_id,p.package_type_id,p.price,p.package_price_km_id,p.price_km,p.id,p.shipping_type,p.parcel_id,p.package_code,p.amount,p.discount,p.total_amount  FROM  parcels_packages as p INNER JOIN  package_price AS pp ON pp.id=p.package_price_id  where parcel_id= ? ";
    private static final String GET_PACKINGS_FXC = "SELECT  id,parcel_id,packing_id,amount,total_amount,unit_price,quantity,discount FROM  parcels_packings  where parcel_id= ? ";
    private static final String GET_SERVICE_FXC = "SELECT pd.id,pd.amount,pd.parcel_id, s.type_service FROM parcels_rad_ead as pd INNER JOIN   parcel_service_type as s  ON  pd.id_type_service = s.id where pd.parcel_id= ? ";
    private static final String QUERY_CUSTOMERS_EAD_RAD = "select \n" +
            "cre.*,\n" +
            "concat(cus.first_name , ' ', cus.last_name) as name\n" +
            "from customer_rad_ead cre\n" +
            "left join customer cus ON cre.customer_id = cus.id\n" +
            "where cus.status = 1 ";
    private static final String QUERY_VALID_CUSTOMERS_SPECIAL_SERVICE = "select \n" +
            "cre.*,\n" +
            "concat(cus.first_name , ' ', cus.last_name) as name\n" +
            "from customer_rad_ead cre\n" +
            "left join customer cus ON cre.customer_id = cus.id\n" +
            "where cus.status = 1  AND cre.status = 1 AND cus.id=";
    private static final String QUERY_VALID_CUSTOMERS_SPECIAL_ID = "select \n" +
            "cre.*,\n" +
            "concat(cus.first_name , ' ', cus.last_name) as name\n" +
            "from customer_rad_ead cre\n" +
            "left join customer cus ON cre.customer_id = cus.id\n" +
            "where cus.status = 1   AND cre.status = 1 AND cus.id=";
}
