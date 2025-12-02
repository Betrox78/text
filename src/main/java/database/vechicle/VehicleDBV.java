/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.vechicle;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import utils.UtilsDate;

import static service.commons.Constants.*;


/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class VehicleDBV extends DBVerticle {

    public enum SCHEDULE_STATUS {
        UNDEFINED,
        CANCELED,
        SCHEDULED,
        LOADING,
        READY_TO_GO,
        IN_TRANSIT,
        STOPPED,
        DOWNLOADING,
        READY_TO_LOAD,
        PAUSED,
        FINISHED_OK
    }

    public enum RENT_STATUS {
        CANCELED,
        SCHEDULED,
        IN_TRANSIT,
        FINISHED,
        NOT_ASSIST,
        QUOTATION
    }
    
    public static final String ACTION_AVAILABLE_VEHICLES = "VehicleDBV.availableVehicles";
    public static final String ACTION_AVAILABLE_VEHICLES_V2 = "VehicleDBV.availableVehiclesV2";
    public static final String ACTION_AVAILABLE_RENTAL_VEHICLES = "VehicleDBV.availableRentalVehicles";
    public static final String ACTION_AVAILABLE_RENTAL_VEHICLES_UPDATE = "VehicleDBV.availableRentalVehiclesUpdate";
    public static final String ACTION_IS_AVAILABLE_RENTAL_VEHICLE = "VehicleDBV.isAvailableRentalVehicle";
    public static final String ACTION_SET_CHARACTERISTICS = "VehicleDBV.setCharacteristics";
    public static final String ACTION_DISABLE_VEHICLE = "VehicleDBV.disableVehicle";
    public static final String ACTION_CHANGE_CONFIG = "VehicleDBV.changeConfig";
    public static final String ACTION_GET_TRAILERS = "VehicleDBV.getTrailers";
    public static final String ACTION_GET_TRAILERS_VEHICLE = "VehicleDBV.getVehicleTrailer";
    public static final String ACTION_GET_TRAILERS_BY_ID = "VehicleDBV.getTrailerById";
    public static final String ACTION_REGISTER_TRAILER = "VehicleDBV.registerTrailer";
    public static final String ACTION_GET_PERMISO_SAT = "VehicleDBV.listTipoPermiso";
    public static final String ACTION_DISABLE_TRAILER = "VehicleDBV.disableTrailer";
    public static final String ACTION_AVAILABLE_TRACTORS = "VehicleDBV.availableTractors";
    public static final String ACTION_GET_CONFIG_AUTOTRANSPORTE_LIST = "VehicleDBV.configAutotransporteList";
    public static final String ACTION_GET_RAD_EAD_VEHICLES = "VehicleDBV.getRadEadVehicles";


    @Override
    public String getTableName() {
        return "vehicle";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_AVAILABLE_VEHICLES:
                this.availableVehicles(message);
                break;
            case ACTION_SET_CHARACTERISTICS:
                this.setCharacteristics(message);
                break;
            case ACTION_AVAILABLE_RENTAL_VEHICLES:
                this.availableRentalVehicles(message);
                break;
            case ACTION_IS_AVAILABLE_RENTAL_VEHICLE:
                this.isAvailableRentalVehicle(message);
                break;
            case ACTION_DISABLE_VEHICLE:
                this.disableVehicle(message);
                break;
            case ACTION_CHANGE_CONFIG:
                this.changeConfig(message);
                break;
            case ACTION_AVAILABLE_RENTAL_VEHICLES_UPDATE:
                this.availableRentalVehiclesUpdate(message);
                break;
            case ACTION_GET_TRAILERS:
                this.getTrailers(message);
                break;
            case ACTION_GET_TRAILERS_BY_ID:
                this.getTrailerById(message);
                break;
            case ACTION_GET_TRAILERS_VEHICLE:
                this.getVehicleTrailer(message);
                break;
            case ACTION_REGISTER_TRAILER:
                this.registerTrailer(message);
                break;
            case ACTION_GET_PERMISO_SAT:
                this.listTipoPermiso(message);
                break;
            case ACTION_DISABLE_TRAILER:
                this.disableTrailer(message);
                break;
            case ACTION_AVAILABLE_VEHICLES_V2:
                this.availableVehiclesV2(message);
                break;
            case ACTION_AVAILABLE_TRACTORS:
                this.availableTractors(message);
                break;
            case ACTION_GET_CONFIG_AUTOTRANSPORTE_LIST:
                this.configAutotransporteList(message);
                break;
            case ACTION_GET_RAD_EAD_VEHICLES:
                this.getRadEadVehicles(message);
                break;
        }
    }

    @Override
    protected void executeUpdate(Message<JsonObject> message) {
        try {
            GenericQuery gc = this.generateGenericUpdate(this.getTableName(), message.body(), true);
            dbClient.updateWithParams(gc.getQuery(), gc.getParams(), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(null);
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void availableVehicles(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String travelDate = body.getString("travelDate");
            String arrivalDate = body.getString("arrivalDate");

            JsonArray params = new JsonArray()
                    .add(travelDate).add(arrivalDate)
                    .add(travelDate).add(arrivalDate);

            this.dbClient.queryWithParams(QUERY_AVAILABLE_VEHICLES, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void getTrailers(Message<JsonObject> message) {
        try {
            this.dbClient.query("SELECT * FROM c_SubTipoRem where status = 1", reply -> {
               try {
                   if(reply.failed()) {
                       throw reply.cause();
                   }
                   message.reply(new JsonArray(reply.result().getRows()));
               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void getVehicleTrailer(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_GET_VEHICLES, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> vehicles = reply.result().getRows();
                    List<CompletableFuture<JsonObject>> vehicleTask = new ArrayList<>();

                    vehicles.forEach(v -> {
                        vehicleTask.add(getTrailerCompletableById(v));
                    });

                    CompletableFuture.allOf(vehicleTask.toArray(new CompletableFuture[vehicleTask.size()])).whenComplete((ps,pt) -> {
                        try {
                            if (pt != null) {
                                reportQueryError(message, pt.getCause());
                            } else {
                                message.reply(new JsonArray(vehicles));
                            }
                        } catch (Exception e) {
                            reportQueryError(message, e.getCause());
                        }
                    });
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void getTrailerById(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            JsonArray params = new JsonArray();
            params.add(body.getInteger("vehicle_id"));

            this.dbClient.queryWithParams(QUERY_GET_TRAILERS_ADMIN, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> trailers = reply.result().getRows();
                    message.reply(new JsonArray(trailers));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getTrailerCompletableById(JsonObject vehicle) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(vehicle.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_GET_TRAILERS_BY_VEHICLE , params, handler -> {
            try {
                if (handler.succeeded()) {
                    List<JsonObject> result = handler.result().getRows();
                    vehicle.put("trailers", result);
                    future.complete(vehicle);
                }
            } catch (Exception e) {
                future.completeExceptionally(e.getCause());
            }
        });
        return future;
    }

    private void availableRentalVehicles(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String travelDate = body.getString("travelDate");
            String arrivalDate = body.getString("arrivalDate");


            JsonArray params = new JsonArray()
                    .add(travelDate).add(arrivalDate);

            this.dbClient.queryWithParams(QUERY_AVAILABLE_RENTAL_VEHICLES, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> vehicles = reply.result().getRows();
                    if(vehicles.isEmpty()){
                        message.reply(null);
                        return;
                    }
                    List<CompletableFuture<List<JsonObject>>> tasks = new ArrayList<CompletableFuture<List<JsonObject>>>();
                    for (int i = 0; i < vehicles.size(); i++) {
                        JsonObject vehicle = vehicles.get(i);
                        tasks.add(getCharacteristics(vehicle));
                    }
                    CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[vehicles.size()]));
                    all.whenComplete((s, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonArray(vehicles));
                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void availableRentalVehiclesUpdate(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            String travelDate = body.getString("travelDate");
            String arrivalDate = body.getString("arrivalDate");
            String rentId = body.getString("rentId");
            String vehicleId = body.getString("vehicleId");
            String QUERY = QUERY_AVAILABLE_RENTAL_VEHICLES_UPDATE_LIST;

            JsonArray params = new JsonArray()
                    .add(travelDate).add(arrivalDate).add(travelDate).add(arrivalDate);

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> vehicles = reply.result().getRows();
                    if(vehicles.isEmpty()){
                        message.reply(null);
                        return;
                    }

                    Set<Integer> unique = new HashSet<>();
                    Set<Integer> duplicate = new HashSet<>();

                    List<Integer> vehicleArray = new ArrayList<Integer>();

                    for (int y = 0 ; y < vehicles.size() ; y++){
                        vehicleArray.add(vehicles.get(y).getInteger("vehicle_id"));
                    }

                    for (int val : vehicleArray )
                        (unique.contains(val) ? duplicate : unique).add(val);

                    if(!duplicate.contains(Integer.parseInt(vehicleId))){
                        unique.remove(Integer.parseInt(vehicleId));
                    }

                    //List<Integer> vehicleArray = new ArrayList<Integer>();

                   /* for (int y = 0 ; y < vehicles.size() ; y++){
                        JsonObject d = vehicles.get(y);
                        if (Integer.parseInt(vehicleId) != d.getInteger("vehicle_id")) {
                            //vehicles.remove(y);
                            vehicleArray.add(d.getInteger("vehicle_id"));
                        }
                    }




                    for(int z = 0 ; z < vehicleArray.size() ; z++){
                        integerSet.add(new Integer(vehicleArray.get(z)));

                        if(z == vehicleArray.size()){
                            break;
                        }
                    }*/

                    String QUERY_GET_DISTINCTS_VEHICLES = "SELECT v.id AS vehicle_id, v.name AS vehicle_name, v.description  AS vehicle_description, v.brand AS vehicle_brand, v.model  AS vehicle_model, v.fuel_capacity AS vehicle_fuel_capacity, v.img_file AS vehicle_img_file, v.economic_number AS vehicle_economic_number, v.vehicle_year, v.status  AS vehicle_status, \n" +
                            "rp.*,  cv.seatings, b.id AS branchoffice_id, b.name AS branchoffice_name, b.prefix AS branchoffice_prefix, \n" +
                            "c.id AS city_id, c.name AS city_name, s.id AS state_id, s.name AS state_name \n" +
                            "FROM vehicle v \n" +
                            "JOIN rental_price rp ON (v.id = rp.vehicle_id AND rp.status=1) \n" +
                            "LEFT JOIN config_vehicle cv ON v.config_vehicle_id = cv.id \n" +
                            "LEFT JOIN branchoffice b ON v.branchoffice_id = b.id \n" +
                            "LEFT JOIN city c ON b.city_id = c.id \n" +
                            "LEFT JOIN state s ON b.state_id = s.id \n" +
                            "WHERE v.work_type = '0' AND v.status = 1 ";


                   if(unique.size() > 0){

                       String inQuery = " AND v.id NOT IN (";
                       int iterator = 0;
                       for(Integer ele: unique){
                           //arr[iterator++] = ele;
                           if(iterator == 0){
                               inQuery = inQuery + ele;

                           }else {
                               inQuery = inQuery + "," + ele;
                           }
                           iterator++;
                       }

                       QUERY_GET_DISTINCTS_VEHICLES = QUERY_GET_DISTINCTS_VEHICLES + inQuery + ")";


                   }

                    this.dbClient.query(QUERY_GET_DISTINCTS_VEHICLES , replyDistinct -> {
                        try {
                            if (replyDistinct.failed()){
                                throw replyDistinct.cause();
                            }
                            List<JsonObject> vehiclesDistinct = replyDistinct.result().getRows();
                            if(vehiclesDistinct.isEmpty()){
                                message.reply(null);
                                return;
                            }
                            List<CompletableFuture<List<JsonObject>>> tasks = new ArrayList<CompletableFuture<List<JsonObject>>>();
                            for (int i = 0; i < vehiclesDistinct.size(); i++) {
                                JsonObject vehicle = vehiclesDistinct.get(i);
                                tasks.add(getCharacteristics(vehicle));
                            }
                            CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[vehiclesDistinct.size()]));
                            all.whenComplete((s, error) -> {
                                try {
                                    if (error != null){
                                        throw error;
                                    }
                                    message.reply(new JsonArray(vehiclesDistinct));
                                } catch (Throwable t){
                                    reportQueryError(message, t);
                                }
                            });
                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });

                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void isAvailableRentalVehicle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String travelDate = body.getString("travelDate");
            String arrivalDate = body.getString("arrivalDate");
            Integer vehicleId = body.getInteger("vehicleId");

            JsonArray params = new JsonArray()
                    .add(travelDate).add(arrivalDate)
                    .add(vehicleId);

            this.dbClient.queryWithParams(QUERY_IS_AVAILABLE_RENTAL_VEHICLE, params, reply -> {
                this.genericResponse(message, reply);
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getCharacteristics(JsonObject vehicle) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(vehicle.getInteger("id"));
            this.dbClient.queryWithParams(QUERY_CHARACTERISTICS, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> characteristics = reply.result().getRows();
                    vehicle.put("characteristics", characteristics);
                    future.complete(characteristics);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private void setCharacteristics(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                int vehicleId = body.getInteger("vehicle_id");
                int createdBy = body.getInteger(CREATED_BY);
                JsonArray charactIds = body.getJsonArray("characteristic_ids");
                List<String> values = new ArrayList<>();
                for (int i = 0; i < charactIds.size(); i++) {
                    int characteristicId = charactIds.getInteger(i);
                    values.add("(" + vehicleId + "," + characteristicId + "," + createdBy + ")");
                }
                List<String> batch = new ArrayList<>();
                batch.add(QUERY_DELETE_CHARACTERISTICS_RELATION + vehicleId);
                batch.add(QUERY_INSERT_CHARACTERISTICS_RELATION + String.join(",", values));
                conn.batch(batch, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        this.commit(conn, message, body);
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    private void disableVehicle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer vehicleID = body.getInteger("vehicle_id");
            this.hasRoutes(vehicleID)
                    .thenCompose(s -> this.hasRentals(vehicleID))
                    .thenCompose(ss -> this.doDisableVehicle(vehicleID))
                    .whenComplete((sss, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonObject().put("id", vehicleID));
                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });
        } catch (Throwable t){
            reportQueryError(message, t);
        }

    }

    private CompletableFuture<JsonObject> hasRoutes(Integer vehicleID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(UtilsDate.sdfDataBase(new Date()))
                    .add(vehicleID);

            this.dbClient.queryWithParams(QUERY_VEHICLE_HAS_ROUTES, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    JsonObject result = reply.result().getRows().get(0);
                    Integer items = result.getInteger("items");
                    if (!items.equals(0)) {
                        throw new Exception("Vehicle: Has routes scheduled");
                    }
                    future.complete(result);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> hasRentals(Integer vehicleID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(UtilsDate.sdfDataBase(new Date()))
                    .add(vehicleID);

            this.dbClient.queryWithParams(QUERY_VEHICLE_HAS_RENTALS, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    JsonObject result = reply.result().getRows().get(0);
                    Integer items = result.getInteger("items");
                    if (!items.equals(0)) {
                        throw new Exception("Vehicle: Has rentals scheduled");
                    }
                    future.complete(result);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Integer> doDisableVehicle(Integer vehicleID) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(vehicleID);

            this.dbClient.updateWithParams(DISABLE_VEHICLE, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    boolean updated = reply.result().getUpdated() > 0;
                    if (!updated) {
                        throw new Exception("Vehicle: Not updated");
                    }
                    future.complete(vehicleID);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }


    private void changeConfig(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer vehicleID = body.getInteger("vehicle_id");
            Integer configVehicleID = body.getInteger("config_vehicle_id");
            this.hasActiveRoutes(vehicleID)
                    .thenCompose(s -> this.hasActiveRentals(vehicleID))
                    .thenCompose(ss -> this.doChangeConfigVehicle(configVehicleID, vehicleID))
                    .whenComplete((sss, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonObject().put("id", vehicleID));
                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> hasActiveRoutes(Integer vehicleID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(vehicleID);

            this.dbClient.queryWithParams(QUERY_VEHICLE_HAS_ACTIVE_ROUTES, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    JsonObject result = reply.result().getRows().get(0);
                    Integer items = result.getInteger("items");
                    if (!items.equals(0)) {
                        throw new Exception("Vehicle: Has active routes scheduled");
                    }
                    future.complete(result);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> hasActiveRentals(Integer vehicleID) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(vehicleID);

            this.dbClient.queryWithParams(QUERY_VEHICLE_HAS_ACTIVE_RENTALS, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    JsonObject result = reply.result().getRows().get(0);
                    Integer items = result.getInteger("items");
                    if (!items.equals(0)) {
                        throw new Exception("Vehicle: Has active rentals scheduled");
                    }
                    future.complete(result);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Integer> doChangeConfigVehicle(Integer configVehicleID, Integer vehicleID) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(configVehicleID).add(vehicleID);

            this.dbClient.updateWithParams(CHANGE_CONFIG_VEHICLE, params, reply -> {
                try {
                    boolean updated = reply.result().getUpdated() > 0;
                    if (!updated) {
                        throw new Exception("Vehicle: Not updated");
                    }
                    future.complete(vehicleID);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private void registerTrailer(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject trailer = message.body();
                //GenericQuery register = generateGenericCreate("vehicle_trailer", trailer);
                conn.update(generateGenericCreate("vehicle_trailer", trailer), replyInsert -> {
                    try {
                        if (replyInsert.failed()) {
                            throw new Exception(replyInsert.cause());
                        }
                        Integer id = replyInsert.result().getKeys().getInteger(0);
                        this.commit(conn, message, new JsonObject().put("id", id));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                reportQueryError(message,e);
            }
        });
    }

    private void listTipoPermiso(Message<JsonObject> message) {
        try {


            this.dbClient.query(QUERY_GET_TIPO_PERMISO, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> trailers = reply.result().getRows();
                    message.reply(new JsonArray(trailers));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void disableTrailer(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer id = body.getInteger("id");
            Integer status = body.getInteger("status");
            JsonArray params = new JsonArray();
            if(status != null) {
                if(status == 0) {
                    params.add(1);
                } else if(status == 1) {
                    params.add(0);
                }
            }

            params.add(id);

            this.dbClient.queryWithParams(QUERY_UPDATE_STATUS_TRAILER, params , reply -> {
                if(reply.succeeded()) {
                    message.reply(reply.succeeded());
                } else {
                    reportQueryError(message, reply.cause());
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }

    }

    private void availableVehiclesV2(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String workType = body.getString("work_type");

            this.dbClient.queryWithParams(QUERY_AVAILABLE_VEHICLES_V2, new JsonArray().add(workType), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void availableTractors(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_AVAILABLE_TRACTORS, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void configAutotransporteList(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_GET_CONFIG_AUTOTRANSPORTE, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void getRadEadVehicles(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            this.dbClient.queryWithParams(QUERY_GET_RAD_EAD_VEHICLES_BY_EMPLOYEE_BRANCHOFFICE_ID,
                    new JsonArray().add(branchofficeId), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_AVAILABLE_VEHICLES = "SELECT \n"
            + " vh.id, vh.name, vh.economic_number, vh.can_use_trailer \n"
            + " FROM vehicle AS vh \n"
            + " LEFT JOIN schedule_route AS sr \n"
            + " ON sr.vehicle_id = vh.id \n"
            + " LEFT JOIN config_route AS cr \n"
            + " ON cr.id = sr.config_route_id \n"
            + " WHERE (NOT EXISTS ( \n"
            + "     SElECT nesr.vehicle_id FROM schedule_route AS  nesr \n"
            + "     WHERE nesr.vehicle_id = vh.id AND nesr.status = 1 AND nesr.schedule_status != 'canceled' \n"
            + " ) \n"
            + " OR ( \n"
            + "     sr.status = 1 AND sr.schedule_status != 'canceled' \n"
            + "     AND ( \n"
            + "         (NOT sr.travel_date BETWEEN ? AND ?) \n"
            + "         AND (NOT sr.arrival_date BETWEEN ? AND ?) \n"
            + "     ) \n"
            + " )) \n"
            + " AND (vh.status = 1 AND vh.work_type = '1') \n"
            + " GROUP BY vh.id, vh.name, vh.economic_number;";

    private static final String QUERY_AVAILABLE_RENTAL_VEHICLES = "SELECT v.id AS vehicle_id, v.name AS vehicle_name, v.description  AS vehicle_description, v.brand AS vehicle_brand, v.model  AS vehicle_model, v.fuel_capacity AS vehicle_fuel_capacity, v.img_file AS vehicle_img_file, v.economic_number AS vehicle_economic_number, v.vehicle_year, v.status  AS vehicle_status, \n" +
            "rp.*,  cv.seatings, b.id AS branchoffice_id, b.name AS branchoffice_name, b.prefix AS branchoffice_prefix, \n" +
            "c.id AS city_id, c.name AS city_name, s.id AS state_id, s.name AS state_name \n" +
            "FROM vehicle v \n" +
            "JOIN rental_price rp ON (v.id = rp.vehicle_id AND rp.status=1) \n" +
            "LEFT JOIN config_vehicle cv ON v.config_vehicle_id = cv.id \n" +
            "LEFT JOIN branchoffice b ON v.branchoffice_id = b.id \n" +
            "LEFT JOIN city c ON b.city_id = c.id \n" +
            "LEFT JOIN state s ON b.state_id = s.id \n" +
            "WHERE v.work_type = '0' AND v.status = 1 " +
            "AND v.id NOT IN ( \n" +
            "    SELECT DISTINCT (r.vehicle_id) AS id \n" +
            "    FROM rental r \n" +
            "    WHERE (? BETWEEN r.departure_date AND r.return_date \n" +
            "    OR ? BETWEEN r.departure_date AND r.return_date) \n" +
            "    AND r.rent_status > 0 AND r.rent_status < 3 AND r.status != 3)";

    private static final String QUERY_AVAILABLE_RENTAL_VEHICLES_UPDATE_LIST = "SELECT r.id  , r.vehicle_id , r.reservation_code \n" +
            "                FROM rental r \n" +
           " WHERE r.departure_date BETWEEN  ? AND ? \n" +
            "                OR r.return_date BETWEEN ? AND ? \n" +
            "                AND r.rent_status > 0 AND r.rent_status < 3 AND r.status != 3 ";
            //"                AND r.id not in ( ? )";

    private static final String QUERY_IS_AVAILABLE_RENTAL_VEHICLE = "SELECT v.id AS vehicle_id, v.name AS vehicle_name, v.description  AS vehicle_description, v.brand AS vehicle_brand, v.model  AS vehicle_model, v.fuel_capacity AS vehicle_fuel_capacity, v.img_file AS vehicle_img_file, v.economic_number AS vehicle_economic_number, v.vehicle_year, v.status  AS vehicle_status, \n" +
            "rp.*,  cv.seatings, b.id AS branchoffice_id, b.name AS branchoffice_name, b.prefix AS branchoffice_prefix, \n" +
            "c.id AS city_id, c.name AS city_name, s.id AS state_id, s.name AS state_name \n" +
            "FROM vehicle v \n" +
            "JOIN rental_price rp ON (v.id = rp.vehicle_id AND rp.status=1) \n" +
            "LEFT JOIN config_vehicle cv ON v.config_vehicle_id = cv.id \n" +
            "LEFT JOIN branchoffice b ON v.branchoffice_id = b.id \n" +
            "LEFT JOIN city c ON b.city_id = c.id \n" +
            "LEFT JOIN state s ON b.state_id = s.id \n" +
            "WHERE v.work_type = '0' AND v.status = 1 " +
            "AND v.id NOT IN ( \n" +
            "    SELECT DISTINCT (r.vehicle_id) AS id \n" +
            "    FROM rental r \n" +
            "    WHERE ( ? BETWEEN r.departure_date AND r.return_date \n" +
            "    OR ? BETWEEN r.departure_date AND r.return_date ) \n" +
            "    AND r.rent_status > 0 AND r.rent_status < 3 AND r.status != 3) AND v.id = ?";

    private static final String QUERY_CHARACTERISTICS = "SELECT * \n"
            + "FROM\n"
            + "	vehicle_characteristic\n"
            + "WHERE\n"
            + "	vehicle_id = ?";

    private static final String QUERY_DELETE_CHARACTERISTICS_RELATION = "DELETE\n"
            + "FROM\n"
            + "	vehicle_characteristic\n"
            + "WHERE\n"
            + "	vehicle_id = ";

    private static final String QUERY_INSERT_CHARACTERISTICS_RELATION = "INSERT\n"
            + "	INTO\n"
            + "		vehicle_characteristic( vehicle_id,\n"
            + "		characteristic_id,\n"
            + "		created_by )\n"
            + "	VALUES ";

    private static final String QUERY_VEHICLE_HAS_ROUTES = "SELECT COUNT(id) AS items \n" +
            "FROM schedule_route \n" +
            "WHERE travel_date > ? AND vehicle_id = ?;";

    private static final String QUERY_VEHICLE_HAS_ACTIVE_ROUTES = "SELECT COUNT(id) AS items \n" +
            "FROM schedule_route \n" +
            "WHERE vehicle_id = ? AND (schedule_status != " + SCHEDULE_STATUS.CANCELED.ordinal() + " AND schedule_status != " + RENT_STATUS.FINISHED.ordinal() + ");";

    private static final String QUERY_VEHICLE_HAS_RENTALS = "SELECT COUNT(id) AS items \n" +
            "FROM rental \n" +
            "WHERE departure_date > ? AND vehicle_id = ?;";

    private static final String QUERY_VEHICLE_HAS_ACTIVE_RENTALS = "SELECT COUNT(id) AS items \n" +
            "FROM rental \n" +
            "WHERE vehicle_id = ? AND (rent_status = " + RENT_STATUS.SCHEDULED.ordinal() + " OR rent_status = " + RENT_STATUS.IN_TRANSIT.ordinal() + ");";

    private static final String DISABLE_VEHICLE = "UPDATE vehicle \n" +
            "SET status = 2\n" +
            "WHERE id = ? AND status = 1;";

    private static final String CHANGE_CONFIG_VEHICLE = "UPDATE vehicle \n" +
            "SET config_vehicle_id = ?\n" +
            "WHERE id = ? ;";

    private static final String QUERY_GET_TRAILERS_BY_VEHICLE = "select \n" +
            "str.* ,\n" +
            "vt.plate \n" +
            "from vehicle_trailer vt\n" +
            "left join c_SubTipoRem str ON vt.c_SubTipoRem_id = str.id\n" +
            "where vt.vehicle_id =  ?  \n" +
            "and str.status = 1 ;";

    private static final String QUERY_GET_VEHICLES = "select \n" +
            " id,\n" +
            " name\n" +
            "from vehicle where status = 1 ";

    private static final String QUERY_GET_TRAILERS_ADMIN = "select \n" +
            "sbr.id,\n" +
            "sbr.Clave_tipo_remolque,\n" +
            "sbr.status,\n" +
            "sbr.Remolque_o_semirremolque,\n" +
            "vt.id as trailer_id,\n" +
            "vt.status as trailer_status,\n" +
            "vt.plate\n" +
            "from  vehicle_trailer vt\n" +
            "left join c_SubTipoRem sbr ON vt.c_SubTipoRem_id = sbr.id\n" +
            " where vt.vehicle_id = ? ";

    private static final String QUERY_GET_TIPO_PERMISO = "select \n" +
            "id,\n" +
            "Clave,\n" +
            "Descripcion,\n" +
            "Clave_transporte\n" +
            " from c_TipoPermiso\n" +
            " where status = 1";

    private static final String QUERY_UPDATE_STATUS_TRAILER = "update vehicle_trailer set status = ? where id = ?";

    private static final String QUERY_AVAILABLE_VEHICLES_V2 = "SELECT \n"
            + " vh.id, \n "
            + " vh.economic_number, \n "
            + " vh.name, \n "
            + " vh.description, \n "
            + " vh.alias, \n "
            + " vh.model, \n "
            + " vh.work_type, \n "
            + " IF(vh.can_use_trailer AND ca.remolque NOT IN ('0'), TRUE, FALSE) AS can_use_trailer, \n"
            + " ca.clave AS config_clave, \n"
            + " ca.description AS config_description, \n"
            + " ca.num_ejes AS config_num_ejes, \n"
            + " ca.num_llantas AS config_num_llantas, \n"
            + " ca.remolque AS config_remolque \n"
            + " FROM vehicle AS vh \n"
            + " LEFT JOIN c_ConfigAutotransporte ca ON ca.id = vh.c_ConfigAutotransporte_id AND ca.status = 1\n"
            + " WHERE vh.status = 1 AND vh.work_type = ?\n"
            + " GROUP BY vh.id, vh.name, vh.economic_number, ca.clave, ca.description, ca.num_ejes, ca.num_llantas, ca.remolque;";

    private static final String QUERY_AVAILABLE_TRACTORS = "SELECT \n"
            + " vh.id, \n "
            + " vh.economic_number, \n "
            + " vh.name, \n "
            + " vh.description, \n "
            + " vh.alias, \n "
            + " vh.model, \n "
            + " vh.work_type, \n "
            + " IF(vh.can_use_trailer AND ca.remolque NOT IN ('0'), TRUE, FALSE) AS can_use_trailer, \n"
            + " ca.clave AS config_clave, \n"
            + " ca.description AS config_description, \n"
            + " ca.num_ejes AS config_num_ejes, \n"
            + " ca.num_llantas AS config_num_llantas, \n"
            + " ca.remolque AS config_remolque \n"
            + " FROM vehicle AS vh \n"
            + " INNER JOIN c_ConfigAutotransporte ca ON ca.id = vh.c_ConfigAutotransporte_id AND ca.status = 1\n"
            + " WHERE vh.status = 1 AND vh.work_type = '4'\n"
            + " GROUP BY vh.id, vh.name, vh.economic_number, ca.clave, ca.description, ca.num_ejes, ca.num_llantas, ca.remolque;";

    private static final String QUERY_GET_CONFIG_AUTOTRANSPORTE = "SELECT\n" +
            "\t*\n" +
            "FROM c_ConfigAutotransporte\n" +
            "WHERE status = 1";

    private static final String QUERY_GET_RAD_EAD_VEHICLES_BY_EMPLOYEE_BRANCHOFFICE_ID = "SELECT \n" +
            "   v.id,\n" +
            "   v.name,\n" +
            "   v.description,\n" +
            "   v.economic_number,\n" +
            "   v.brand\n" +
            "FROM vehicle_rad_ead vre\n" +
            "INNER JOIN vehicle v ON v.id = vre.id_vehicle\n" +
            "WHERE vre.status = 1\n" +
            "   AND v.status = 1\n" +
            "   AND vre.id_branchoffice = ?\n" +
            "GROUP BY v.id";


//</editor-fold>

}
