package database.ead_rad;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;
import static service.commons.Constants.CREATED_AT;
import static utils.UtilsDate.sdfDataBase;

public class VehicleRadEadDBV extends DBVerticle {

    public static final String ACTION_GET_SCHEDULES_RAD_EAD = "VehicleRadEadDBV.get_vehicle_rad_ead";
    public static final String ACTION_REGISTER_VEHICLE_TERMINAL = "VehicleRadEadDBV.insertVehicleTerminal";
    public static final String ACTION_UPDATE_VEHICLE_TERMINAL_STATUS = "VehicleRadEadDBV.updateStatusVehicleTerminal";
    public static final String ACTION_UPDATE_DATA_VEHICLE = "VehicleRadEadDBV.updateDataVehicle";

    @Override
    public String getTableName() {
        return "vehicle_rad_ead";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {

            case ACTION_GET_SCHEDULES_RAD_EAD:
                this.get_vehicle_rad_ead(message);
                break;
            case ACTION_REGISTER_VEHICLE_TERMINAL:
                this.insertVehicleTerminal(message);
                break;
            case ACTION_UPDATE_VEHICLE_TERMINAL_STATUS:
                this.updateStatusVehicleTerminal(message);
                break;
            case ACTION_UPDATE_DATA_VEHICLE:
                this.updateDataVehicle(message);
                break;

        }
    }


    private void get_vehicle_rad_ead(Message<JsonObject> message) {
        this.dbClient.queryWithParams(GET_VEHICLE_RAD_EAD, null, reply -> {
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

    private void insertVehicleTerminal(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            try{
                //JsonObject postal = message.body().copy();
                JsonObject copy = message.body().copy();
                copy.put(CREATED_AT,sdfDataBase(new Date()));

                this.registerVehicle(conn,copy).whenComplete((res,error) -> {
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

    private CompletableFuture<JsonObject> registerVehicle(SQLConnection conn, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("vehicle_rad_ead",params);

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

    private void updateStatusVehicleTerminal(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer status= Integer.parseInt(body.getString("status"));
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

    private void updateDataVehicle(Message<JsonObject> message) {
        JsonObject body = message.body();

        GenericQuery update = this.generateGenericUpdate("vehicle_rad_ead", body);

        this.dbClient.updateWithParams(update.getQuery(), update.getParams(), reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }


    private static final String QUERY_UPDATE_ATTEMPT_REASON = "update vehicle_rad_ead set status = ? , updated_at = ? ,updated_by = ?  where id = ?";
    private static final String GET_VEHICLE_RAD_EAD = " SELECT\n" +
            " br.name,\n" +
            " br.prefix,\n" +
            " vea.status,\n" +
            " vea.id as id_vehicle_rad_ead, \n" +
            " vea.id_vehicle,\n" +
            " vea.id_branchoffice , \n" +
            " vea.id_vehicle,\n" +
            " concat(emp.name,' ' , emp.last_name) as driver,\n" +
            " v.*,\n" +
            " vea.id_employee \n" +
            " \n" +
            " \n" +
            " FROM vehicle_rad_ead as vea \n" +
            " inner join vehicle v on vea.id_vehicle=v.id \n" +
            " left join branchoffice br ON vea.id_branchoffice = br.id \n" +
            " left join employee emp ON vea.id_employee = emp.id\n" +
            " where vea.status=1";
}
