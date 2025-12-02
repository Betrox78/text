/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.routes;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.parcel.ParcelsPackagesTrackingDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.routes.handlers.TravelTrackingDBV.*;
import database.routes.handlers.enums.TRAVELTRACKING_ACTION;
import database.routes.handlers.enums.TRAVELTRACKING_STATUS;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import service.commons.Constants;
import utils.UtilsDate;

import static database.boardingpass.BoardingPassDBV.*;
import static service.commons.Constants.*;

/**
 *
 * @author Saul
 */
public class TravelTrackingDBV extends DBVerticle {

    public static final JsonArray RESPONSE_LIST = new JsonArray()
            .add("OK")
            .add("CANNOT BE CHANGED TO LOADING ")
            .add("CANNOT BE CHANGED TO READY-TO-GO ")
            .add("CANNOT BE CHANGED TO IN-TRANSIT ")
            .add("CANNOT BE CHANGED TO STOPPED ")
            .add("CANNOT BE CHANGED TO DOWNLOADING ")
            .add("CANNOT BE CHANGED TO READY-TO-LOAD ")
            .add("CANNOT BE CHANGED TO PAUSED ")
            .add("CANNOT BE CHANGED TO FINISHED-OK ")
            .add("CANNOT BE CHANGED TO FINISHED-OK ")
            .add("THE STATUS CANNOT BE IDENTIFIED ")
            ;

    //THE ELEMENTS POSITION OF THIS ENUM MUST BE COINCIDE WITH THE ERRORS_LIST
    public enum RESPONSE_CHANGE_STATUS {
        OK,
        CANNOT_BE_CHANGED_TO_LOADING,
        CANNOT_BE_CHANGED_TO_READY_TO_GO,
        CANNOT_BE_CHANGED_TO_IN_TRANSIT,
        CANNOT_BE_CHANGED_TO_STOPPED,
        CANNOT_BE_CHANGED_TO_DOWNLOADING,
        CANNOT_BE_CHANGED_TO_READY_TO_LOAD,
        CANNOT_BE_CHANGED_TO_PAUSED,
        CANNOT_BE_CHANGED_TO_FINISHED_OK,
        STATUS_NOT_IDENTIFIED
    }

    public enum SHIPMENT_TRACKING_STATUS {
        LOADED("loaded"),
        DOWNLOADED("downloaded");

        String name;

        SHIPMENT_TRACKING_STATUS(String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }

    }

    public static final String REGISTER = "TravelTrackingDBV.register";
    public static final String CHECK_STATUS = "TravelTrackingDBV.check_status";
    public static final String INIT_LOAD = "ShipmentsDBV.initLoad";
    public static final String CLOSE_LOAD = "ShipmentsDBV.closeLoad";
    public static final String INIT_DOWNLOAD = "ShipmentsDBV.initDownload";
    public static final String CLOSE_DOWNLOAD = "ShipmentsDBV.closeDownload";
    public static final String MANEUVER_TIME_REPORT = "TravelTrackingDBV.maneuverReport";
    public static final String INIT_TRAVEL = "TravelTrackingDBV.initTravel";

    public static final String DRIVER_ID = "driver_id";
    public static final String SECOND_DRIVER_ID = "second_driver_id";
    public static final String TOTAL_TICKETS = "total_tickets";
    public static final String TOTAL_COMPLEMENTS = "total_complements";
    public static final String TOTAL_PARCELS = "total_parcels";
    public static final String TOTAL_PACKAGES = "total_packages";
    public static final String LEFT_STAMP = "left_stamp";
    public static final String RIGHT_STAMP = "right_stamp";
    public static final String ADDITIONAL_STAMP = "additional_stamp";
    public static final String REPLACEMENT_STAMP = "replacement_stamp";
    public static final String HITCHED_TRAILERS = "hitched_trailers";
    public static final String APPLY_RELEASE = "apply_release";

    
    @Override
    public String getTableName() {
        return "travel_tracking";
    }

    RegisterNoTraveled registerNoTraveled;
    CloseLoad closeLoad;
    CloseDownload closeDownload;
    InitTravel initTravel;
    InitLoad initLoad;
    InitDownload initDownload;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        registerNoTraveled = new RegisterNoTraveled(this);
        closeLoad = new CloseLoad(this);
        closeDownload = new CloseDownload(this);
        initTravel = new InitTravel(this);
        initLoad = new InitLoad(this);
        initDownload = new InitDownload(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message){
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action){
            case REGISTER:
                this.register(message);
                break;
            case CHECK_STATUS:
                this.check_status(message);
                break;
            case INIT_LOAD:
                initLoad.handle(message);
                break;
            case CLOSE_LOAD:
                closeLoad.handle(message);
                break;
            case INIT_DOWNLOAD:
                initDownload.handle(message);
                break;
            case MANEUVER_TIME_REPORT:
                this.maneuverReport(message);
                break;
            case CLOSE_DOWNLOAD:
                closeDownload.handle(message);
                break;
            case INIT_TRAVEL:
                initTravel.handle(message);
                break;
        }
    }

    private void maneuverReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_MANEUVER_TIME_REPORT_V2;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originCityId = body.getInteger("origin_city_id");
            Integer originTerminalId = body.getInteger("origin_terminal_id");
            Integer routeId = body.getInteger("route_id");
            String type = body.getString("type");
            params.add(initDate).add(endDate).add(initDate).add(endDate);
            switch(type) {
                case "arrivals":
                    QUERY = QUERY.replace("{JOIN_SHIPMENTS}", "INNER JOIN shipments ship ON ship.id = tl.download_id");
                    QUERY += " AND srd.terminal_destiny_id = ship.terminal_id \n";
                    if(originCityId != null){
                        QUERY += " AND segment_destiny.city_id = ? \n";
                        params.add(originCityId);
                    }
                    if(originTerminalId != null){
                        QUERY += " AND segment_destiny.id = ? \n";
                        params.add(originTerminalId);
                    }
                    break;
                case "departures":
                    QUERY = QUERY.replace("{JOIN_SHIPMENTS}", "INNER JOIN shipments ship ON ship.id = tl.load_id");
                    QUERY += " AND srd.terminal_origin_id = ship.terminal_id \n" ;
                    if(originCityId != null){
                        QUERY += " AND segment_origin.city_id = ? \n";
                        params.add(originCityId);
                    }
                    if(originTerminalId != null){
                        QUERY += " AND segment_origin.id = ? \n";
                        params.add(originTerminalId);
                    }
                    break;
                case "both":
                    QUERY = QUERY.replace("{JOIN_SHIPMENTS}", "INNER JOIN shipments ship ON ship.id = tl.load_id OR ship.id = tl.download_id");
                    QUERY += " AND (srd.terminal_origin_id = ship.terminal_id OR srd.terminal_destiny_id = ship.terminal_id) \n";
                    if(originCityId != null){
                        QUERY += " AND (segment_origin.city_id = ? OR segment_destiny.city_id = ? )\n";
                        params.add(originCityId).add(originCityId);
                    }
                    if(originTerminalId != null){
                        QUERY += " AND (segment_origin.id = ? OR segment_destiny.id = ?) \n";
                        params.add(originTerminalId).add(originTerminalId);
                    }
                    break;
            }
            if(routeId != null){
                QUERY += " AND sr.config_route_id = ? \n";
                params.add(routeId);
            }
            QUERY += " GROUP BY srd.id ORDER BY srd.travel_date ASC, info_ant.arrival_date DESC;";

            this.dbClient.queryWithParams(QUERY, params , reply ->{
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    List<JsonObject> resultScanningPackages = reply.result().getRows();
                    if (resultScanningPackages.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        List<JsonObject> result = new ArrayList<>();
                        for(JsonObject m : resultScanningPackages){
                            if(!result.isEmpty()){
                                String travelDateScan = m.getString("travel_date");
                                Integer idScan = m.getInteger("id");
                                Boolean contains = false;
                                for(JsonObject s : new ArrayList<>(result)){
                                    String travelDate = s.getString("travel_date");
                                    Integer id = s.getInteger("id");
                                    if(travelDate.equals(travelDateScan) && idScan.equals(id)){
                                        contains = true;
                                        if(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(travelDate).after(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(travelDateScan))){
                                            result.remove(m);
                                            result.add(s);
                                        }
                                    }
                                }
                                if(!contains){
                                    result.add(m);
                                }
                            }else{
                                result.add(m);
                            }
                        }
                        message.reply(new JsonArray(result));
                    }
                }catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }

    }

    private void initDownload(Message<JsonObject> message){
        startTransaction(message,conn->{
            try {
                JsonObject body = message.body();
                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer scheduleRouteId = body.getInteger(_SCHEDULE_ROUTE_ID);
                /*String leftStamp = body.getString(LEFT_STAMP);
                String rightStamp = body.getString(RIGHT_STAMP);
                String additionalStamp = body.getString(ADDITIONAL_STAMP);
                String replacementStamp = body.getString(REPLACEMENT_STAMP);*/

                JsonObject bodyTra = new JsonObject()
                        .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                        .put(TERMINAL_ID, terminalId)
                        .put("schedule_status","downloading")
                        .put("table","downloading")
                        .put(CREATED_BY, body.getInteger(CREATED_BY));

                JsonArray params = new JsonArray()
                        .add(terminalId)
                        .add(scheduleRouteId);
                conn.queryWithParams(GET_SHIPMENT_LOAD_FOR_ROUTE_BY_TERMINAL,params,replyS->{
                    try {
                        if(replyS.succeeded()){
                            if(replyS.result().getNumRows()>0){
                                JsonObject shipmentLoad = replyS.result().getRows().get(0);
                                body.put("shipment_id", shipmentLoad.getInteger(ID));
                                if(!shipmentLoad.getString("shipment_type").equals("load")){
                                    this.rollback(conn,new Throwable("The previous shipment record is not a shipment load"),message);

                                //}else if(!leftStamp.equals(shipmentLoad.getString("left_stamp")) || !rightStamp.equals(shipmentLoad.getString("right_stamp"))){
                                //    this.rollback(conn,new Throwable("The stamps does not match"),message);
                                }else{
                                    updateScheduleRouteStatus(conn,bodyTra).whenComplete((biRes,biError)->{
                                        try {
                                            if(biError!=null){
                                                this.rollback(conn,biError,message);
                                            }else{
                                                registerShipment(conn,body).whenComplete((res,error)-> {
                                                    try {
                                                        if(error != null){
                                                            this.rollback(conn,error,message);
                                                        }else{

                                                            this.commit(conn,message,res);
                                                        }
                                                    }catch (Exception e){
                                                        this.rollback(conn,e,message);
                                                    }
                                                });
                                            }
                                        }catch (Exception e){
                                            this.rollback(conn,e,message);
                                        }
                                    });
                                }
                            }else{
                                this.rollback(conn,new Throwable("Information of Shipment load not found"),message);
                            }
                        }else{
                            this.rollback(conn,replyS.cause(),message);
                        }
                    }catch (Exception e){
                        this.rollback(conn,e,message);
                    }
                });
            }catch (Exception e){
                this.rollback(conn,e,message);
            }
        });
    }

    private void initLoad(Message<JsonObject> message){
        startTransaction(message,conn->{
            try {
                JsonObject body = message.body();

                Integer createdBy = body.getInteger(CREATED_BY);
                Integer terminalId = body.getInteger(TERMINAL_ID);

                body.put(TERMINAL_ID, terminalId);
                body.put(CREATED_BY, createdBy);

                JsonObject bodyTra = new JsonObject()
                        .put(SCHEDULE_ROUTE_ID, body.getInteger(SCHEDULE_ROUTE_ID))
                        .put(TERMINAL_ID, terminalId)
                        .put("schedule_status","loading")
                        .put("table","loading")
                        .put(CREATED_BY, createdBy);
                this.loadingTravel(conn,bodyTra).whenComplete((res,err)->{
                    try {
                        if(err!=null){
                            throw new Exception(err);
                        }
                        Boolean success = res.getBoolean("success");
                        if(!success) {
                            throw new Exception("No se puede iniciar el embarque");
                        }
                        registerShipment(conn,body).whenComplete((result,error)-> {
                            try {
                                if(error != null){
                                    throw new Exception(error);
                                }
                                this.commit(conn,message,result);
                            }catch (Exception e){
                                e.printStackTrace();
                                this.rollback(conn,e,message);
                            }
                        });

                    }catch (Exception e){
                        e.printStackTrace();
                        this.rollback(conn,e,message);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn,e,message);
            }
        });
    }

    private CompletableFuture<JsonObject> registerShipment(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer scheduleRouteId = body.getInteger("schedule_route_id");
            getShipmentsStatusByRoute(conn,scheduleRouteId,1).whenComplete((res,err)->{
                try {
                    if(err != null){
                        throw new Exception(err);
                    }
                    if(res.size()>0){
                        throw new Exception("SHIPMENT LOADING OR DOWNLOADING IS OPEN FOR THIS ROUTE");
                    }
                    Integer shipmentId = null;
                    if (body.getInteger("shipment_id") != null){
                        shipmentId = body.getInteger("shipment_id");
                        body.remove("shipment_id");
                    }
                    String gq = this.generateGenericCreate("shipments",body);
                    Integer finalShipmentId = shipmentId;
                    conn.update(gq, reply->{
                        try {
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            // create or update travel_log
                            if (finalShipmentId != null){
                                conn.queryWithParams("SELECT * FROM travel_logs WHERE load_id = ? LIMIT 1;", new JsonArray().add(finalShipmentId), replyCheckTravelLogs -> {
                                   try {
                                       if(replyCheckTravelLogs.failed()) {
                                           throw new Exception(replyCheckTravelLogs.cause());
                                       }
                                       List<JsonObject> resultTravelLogs = replyCheckTravelLogs.result().getRows();
                                       if (resultTravelLogs.isEmpty()){
                                           throw new Exception("There are no travel records started in logs");
                                       }
                                       Integer shipmentDownloadId = reply.result().getKeys().getInteger(0);
                                       JsonObject travelLog = resultTravelLogs.get(0);
                                       JsonObject objUpdateTravelLog = new JsonObject()
                                               .put(ID, travelLog.getInteger(ID))
                                               .put("download_id", shipmentDownloadId)
                                               .put("status", "close");
                                       String updateTravelLog = this.generateGenericUpdateString("travel_logs", objUpdateTravelLog);
                                       conn.update(updateTravelLog, replyUpdateTravelLog ->{
                                          try {
                                              if(replyUpdateTravelLog.failed()) {
                                                  throw new Exception(replyUpdateTravelLog.cause());
                                              }
                                              future.complete(new JsonObject().put("id", shipmentDownloadId));
                                          } catch (Exception e){
                                              e.printStackTrace();
                                              future.completeExceptionally(e);
                                          }
                                       });
                                   } catch (Exception e){
                                       e.printStackTrace();
                                       future.completeExceptionally(e);
                                   }
                                });
                            } else {
                                JsonObject travelLog = new JsonObject().put("schedule_route_id", scheduleRouteId)
                                        .put("terminal_origin_id", body.getInteger("terminal_id"))
                                        .put("origin", body.getString("origin"))
                                        .put("load_id", reply.result().getKeys().getInteger(0));
                                registerTravelLog(conn, travelLog).whenComplete((ress,errorT)->{
                                    try {
                                        if(errorT != null){
                                            throw new Exception(errorT);
                                        }
                                        future.complete(new JsonObject().put("id",reply.result().getKeys().getInteger(0)));
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        future.completeExceptionally(e);
                                    }
                                });
                            }

                            // preguntar de donde viene si de load o download
                            // consultar si existe un registro en travel_logs con un load o download
                            // si no tiene registro ?
                            // si si
                            // si tiene status diferente a cerrado, cerrarlo
                            // si viene de download y tiene un registro de descarga, cerrarlo y por que borran el resto? :(
                        }catch (Exception e){
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerTravelLog(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(body.getInteger("schedule_route_id"))
                .add(body.getInteger("terminal_origin_id"));
        conn.queryWithParams(QUERY_GET_TERMINAL_DESTINY, params, reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0) {
                    throw new Exception("Schedule route doesnt have terminal destiny");
                }
                body.put("terminal_destiny_id", reply.result().getRows().get(0).getInteger("terminal_destiny_id"));
                // Generar el travel_log_code
                getTravelLogCode(conn, body).whenComplete((ress,errorT)->{
                    try {
                        if(errorT != null){
                            throw new Exception(errorT);
                        }
                        // Guardar el travel_log
                        String gq = this.generateGenericCreate("travel_logs",body);
                        conn.update(gq,replyGq->{
                            try {
                                if(replyGq.failed()) {
                                    throw new Exception(replyGq.cause());
                                }
                                body.put("id", replyGq.result().getKeys().getInteger(0));
                                future.complete(body);
                            }catch (Exception e){
                                e.printStackTrace();
                                future.completeExceptionally(e);
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getTravelLogCode(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(body.getInteger("schedule_route_id"))
                .add(body.getInteger("terminal_origin_id"));
        conn.queryWithParams(QUERY_GET_TRAVEL_LOG_CODE, params, reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0) {
                    throw new Exception("Schedule route doesnt have terminal destiny");
                }
                JsonObject route = reply.result().getRows().get(0);
                // Generar el travel_log_code
                conn.queryWithParams(QUERY_GET_SCHEDULE_ROW, new JsonArray().add(route.getInteger("config_route_id")), replyCode->{
                    try {
                        if(replyCode.failed()) {
                            throw new Exception(replyCode.cause());
                        }
                        if(replyCode.result().getNumRows() == 0) {
                            throw new Exception("Element not found from schedule elements");
                        }
                        List<JsonObject> schedules = replyCode.result().getRows();
                        Integer rowSchedule = 0;
                        String routeId = route.getInteger("config_route_id").toString();
                        if(routeId.length() < 3){
                            int iRouteId = route.getInteger("config_route_id");
                            routeId = String.format("%03d", iRouteId);
                        }

                        for(int i = 0; i < schedules.size(); i ++){
                            if(schedules.get(i).getInteger("id") == route.getInteger("config_schedule_id")){
                                rowSchedule = schedules.get(i).getInteger("row");
                            }
                        }
                        String travel = UtilsDate.format_DD_MM_YYYY(UtilsDate.parse_yyyy_MM_dd(route.getString("travel")));
                        travel = travel.replace("/", "");

                        String code = routeId + "-" + travel + "-" + rowSchedule + "-" + route.getInteger("segment").toString();

                        body.put("travel_log_code", code);
                        future.complete(body);
                    }catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getShipmentsStatusByRoute(SQLConnection conn, Integer scheduleRouteId, Integer status){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(scheduleRouteId)
                .add(status);

        conn.queryWithParams(QUERY_GET_SHIPMENT_BY_STATUS, params, reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                future.complete(reply.result().getRows());
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> updateScheduleRouteStatus(SQLConnection conn,JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.downloadingTravel(conn,body).whenComplete((res,err)->{
                try {
                    if(err!=null){
                        future.completeExceptionally(err);
                    }else {
                        JsonObject bodyRes = res;
                        Boolean success = bodyRes.getBoolean("success");
                        if(success){
                            future.complete(bodyRes);
                        }else {
                            future.completeExceptionally(new Throwable("Downloading not started"));
                        }
                        future.complete(res);
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getTotalsByShipment(SQLConnection conn, String shipmentTrackingStatus, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(shipmentTrackingStatus)
                .add(shipmentTrackingStatus)
                .add(shipmentTrackingStatus)
                .add(shipmentTrackingStatus)
                .add(shipmentId);

        String QUERY_GET_TOTALS_BY_SHIPMENT = "SELECT\n" +
                " COUNT(DISTINCT IF(shiptt.status = ?, shiptt.boarding_pass_ticket_id, NULL)) AS total_tickets,\n" +
                " COUNT(DISTINCT IF(shipct.status = ?, shipct.boarding_pass_complement_id, NULL)) AS total_complements,\n" +
                " COUNT(DISTINCT IF(shipppt.status = ?, shipppt.parcel_id, NULL)) AS total_parcels,\n" +
                " COUNT(DISTINCT IF(shipppt.status = ?, shipppt.parcel_package_id, NULL)) AS total_packages\n" +
                "FROM shipments ship\n" +
                "LEFT JOIN shipments_ticket_tracking shiptt ON shiptt.shipment_id = ship.id\n" +
                "LEFT JOIN shipments_parcel_package_tracking shipppt ON shipppt.shipment_id = ship.id\n" +
                "LEFT JOIN shipments_complement_tracking shipct ON shipct.shipment_id = ship.id\n" +
                "WHERE ship.id = ?;";
        conn.queryWithParams(QUERY_GET_TOTALS_BY_SHIPMENT, params, reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }

               future.complete(reply.result().getRows().get(0));

           } catch (Throwable t){
               t.printStackTrace();
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getShipment(SQLConnection conn, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(shipmentId);
        conn.queryWithParams(QUERY_GET_SHIPMENT,params, reply->{
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0) {
                    throw new Exception("SHIPMENT NOT FOUND");
                }
                future.complete(reply.result().getRows().get(0));
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getTicketInfo(SQLConnection conn, List<JsonObject> tickets, Integer boardingPassTicketId, Integer scheduleRoteId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray paramsInfoTicket = new JsonArray()
                .add(boardingPassTicketId);
        conn.queryWithParams(GET_TICKET_INFO,paramsInfoTicket, reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows()>0){
                    JsonObject ticket = reply.result().getRows().get(0);
                    Integer parcelId = (Integer) ticket.remove("parcel_id");
                    List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                    if(parcelId!=null){
                        task.add(getPackagesToDownloadByPassenger(conn,ticket, scheduleRoteId,parcelId));
                    } else {
                        tickets.add(ticket);
                    }
                } else {
                    future.complete(tickets);
                }
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getPackagesToDownloadByPassenger(SQLConnection conn, JsonObject ticket, Integer scheduleRouteId, Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(parcelId);
            conn.queryWithParams(QUERY_GET_PARCEL_INFO, params, reply->{
                try {
                    if(reply.succeeded()){
                        JsonObject parcel = reply.result().getRows().get(0);
                        JsonArray paramsGetPackages = new JsonArray()
                                .add(scheduleRouteId)
                                .add("load")
                                .add(scheduleRouteId)
                                .add("load")
                                .add(parcelId);
                        conn.queryWithParams(GET_PACKAGES_TO_DOWNLOAD_BY_PARCEL_ID, paramsGetPackages, replyPackages->{
                            try {
                                if(replyPackages.succeeded()){
                                    List<JsonObject> packagesIds = replyPackages.result().getRows();
                                    List<CompletableFuture<List<JsonObject>>> task = new ArrayList<>();
                                    List<JsonObject> packages = new ArrayList<>();
                                    final int len = packagesIds.size();
                                    for(int i=0;i<len; i++){
                                        JsonObject shipPackage = packagesIds.get(i);
                                        task.add(getPackageInfo(conn, packages, parcelId ,shipPackage.getInteger("parcel_package_id")));
                                    }
                                    CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                                        try {
                                            if(err!=null){
                                                future.completeExceptionally(err);
                                            }else{
                                                parcel.put("parcels_packages",packages);
                                                ticket.put("parcel",parcel);
                                                future.complete(ticket);
                                            }
                                        }catch (Exception e){
                                            future.completeExceptionally(e);
                                        }
                                    });
                                }else {
                                    future.completeExceptionally(reply.cause());
                                }
                            }catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    }else{
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getParcelInfo(SQLConnection conn, List<JsonObject> parcels, Integer scheduleRouteId, Integer parcelId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {

            //get info parcel
            JsonArray params = new JsonArray()
                    .add(parcelId);
            conn.queryWithParams(QUERY_GET_PARCEL_INFO, params, reply->{
                try {
                    if(reply.succeeded()){
                        JsonObject parcel = reply.result().getRows().get(0);
                        JsonArray paramsGetPackages = new JsonArray()
                                .add(scheduleRouteId)
                                .add("load")
                                .add(scheduleRouteId)
                                .add("load")
                                .add(parcelId);
                        conn.queryWithParams(GET_PACKAGES_TO_DOWNLOAD_BY_PARCEL_ID, paramsGetPackages, replyPackages->{
                            try {
                                if(replyPackages.succeeded()){
                                    List<JsonObject> packagesIds = replyPackages.result().getRows();
                                    List<CompletableFuture<List<JsonObject>>> task = new ArrayList<>();
                                    List<JsonObject> packages = new ArrayList<>();
                                    final int len = packagesIds.size();
                                    for(int i=0;i<len; i++){
                                        JsonObject shipPackage = packagesIds.get(i);
                                        task.add(getPackageInfo(conn, packages, parcelId ,shipPackage.getInteger("parcel_package_id")));
                                    }
                                    CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                                        try {
                                            if(err!=null){
                                                future.completeExceptionally(err);
                                            }else{
                                                parcel.put("parcels_packages",packages);
                                                parcels.add(parcel);
                                                future.complete(parcels);
                                            }
                                        }catch (Exception e){
                                            future.completeExceptionally(e);
                                        }
                                    });
                                }else {
                                    future.completeExceptionally(reply.cause());
                                }
                            }catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    }else{
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getPackageInfo(SQLConnection conn, List<JsonObject> packages, Integer parcelId, Integer id){
        CompletableFuture<List<JsonObject>> future =  new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(parcelId)
                    .add(id);
            conn.queryWithParams(QUERY_GET_PACKAGE_DETAILS_BY_ID,params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            packages.add(reply.result().getRows().get(0));
                            future.complete(packages);
                        }else{
                            future.completeExceptionally(new Throwable("Parcel package Info not found"));
                        }
                    }else{
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private void register(Message<JsonObject> message){
        GenericQuery gc = this.generateGenericCreate(message.body());
        this.startTransaction(message, (SQLConnection conn) -> {
            try{
            JsonArray params = new JsonArray();
            params.add(message.body().getInteger("schedule_route_id"));
            params.add(message.body().getInteger("schedule_route_destination_id"));

            conn.queryWithParams(QUERY_STATUS_TRACKING_TRAVEL, params, resultHandler->{
                try{
                    if(resultHandler.failed()) {
                        throw new Exception(resultHandler.cause());
                    }
                    if(resultHandler.result().getNumRows()>0){
                        List<JsonObject> rows = resultHandler.result().getRows();
                        String status = rows.get(0).getString("schedule_status");
                        if(status != null){
                            String st = message.body().getString("schedule_status");
                            switch (status) {
                                case "loading":
                                    if(st.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                                        conn.updateWithParams(gc.getQuery(), gc.getParams(), result->{
                                            try{
                                                if(result.failed()){
                                                    throw  new Exception(result.cause());
                                                }
                                                this.commit(conn, message, new JsonObject().put("id", result.result().getKeys().getInteger(0)));
                                            }catch(Exception e){
                                                this.rollback(conn, result.cause(), message);
                                            }
                                        });
                                    }else{
                                        this.rollback(conn, new Throwable("el status desde loading debe cambiar a in-transit"), message);
                                    }
                                    break;
                                case "in-transit":
                                    if(st.equals(TRAVELTRACKING_STATUS.STOPPED.getValue()) || st.equals(TRAVELTRACKING_STATUS.PAUSED.getValue())){
                                        conn.updateWithParams(gc.getQuery(), gc.getParams(), result->{
                                            try{
                                                if(result.failed()){
                                                    throw  new Exception(result.cause());
                                                }
                                                this.commit(conn, message, new JsonObject().put("id", result.result().getKeys().getInteger(0)));
                                            }catch(Exception e){
                                                this.rollback(conn, result.cause(), message);
                                            }
                                        });
                                    }else{
                                        this.rollback(conn, new Exception("el status desde in-transit debe cambiar a stopped o paused"), message);
                                    }
                                    break;
                                case "stopped":
                                    if(st.equals(TRAVELTRACKING_STATUS.DOWNLOADING.getValue())){
                                        conn.updateWithParams(gc.getQuery(), gc.getParams(), result->{
                                            try{
                                                if(result.failed()){
                                                    throw  new Exception(result.cause());
                                                }
                                                this.commit(conn, message, new JsonObject().put("id", result.result().getKeys().getInteger(0)));


                                            }catch(Exception e){
                                                this.rollback(conn, result.cause(), message);

                                            }

                                        });
                                    }else{
                                        this.rollback(conn, new Exception("el status desde stopped debe cambiar a downloading"), message);
                                    }
                                    break;
                                case "downloading":
                                    if(st.equals(TRAVELTRACKING_STATUS.READY_TO_LOAD.getValue()) || st.equals(TRAVELTRACKING_STATUS.FINISHED_OK.getValue())){
                                        conn.updateWithParams(gc.getQuery(), gc.getParams(), result->{
                                            try{
                                                if(result.failed()){
                                                    throw  new Exception(result.cause());
                                                }
                                                this.commit(conn, message, new JsonObject().put("id", result.result().getKeys().getInteger(0)));


                                            }catch(Exception e){
                                                this.rollback(conn, result.cause(), message);
                                            }

                                        });
                                    }else{
                                        this.rollback(conn, new Exception("el status desde downloading debe cambiar a loading o finished"), message);
                                    }
                                    break;
                                case "finished-ok":
                                    this.rollback(conn, new Exception("este viaje ya fue finalizado"), message);
                                    break;
                                case "paused":
                                    if(st.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                                        conn.updateWithParams(gc.getQuery(), gc.getParams(), result->{
                                            try{
                                                if(result.failed()){
                                                    throw  new Exception(result.cause());
                                                }
                                                this.commit(conn, message, new JsonObject().put("id", result.result().getKeys().getInteger(0)));


                                            }catch(Exception e){
                                                this.rollback(conn, result.cause(), message);

                                            }


                                        });
                                    }else{
                                        this.rollback(conn, new Exception("el status desde paused debe cambiar a in-transit"), message);
                                    }
                                    break;
                                default:
                                    this.rollback(conn, new Exception("status incorrecto encontrado: "+status+", consulte a su administrador de sistemas"), message);
                                    break;
                            }

                        }else{
                            this.rollback(conn, new Exception("status incorrecto encontrado: "+status+", consulte a su administrador de sistemas"), message);
                        }
                    }else{
                        String status = message.body().getString("status");
                        if(status != null && status.equals(TRAVELTRACKING_STATUS.LOADING.getValue())){
                            conn.updateWithParams(gc.getQuery(), gc.getParams(), result->{
                                try{
                                    if(result.failed()){
                                        throw  new Exception(result.cause());
                                    }

                                    this.commit(conn, message, new JsonObject().put("id", result.result().getKeys().getValue(0).toString()));

                                }catch(Exception e){
                                    this.rollback(conn, result.cause(), message);

                                }

                            });
                        }else{
                            this.rollback(conn, new Exception("El log del travel no se puede iniciar con stadus distinto a Embarcando"), message);
                        }
                    }

                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, resultHandler.cause(), message);
                }
            });
        }catch (Exception e){
            this.rollback(conn,e,message);
        }
        });
    }

    private void check_status(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn)->{
            try{
                JsonObject body = message.body();
                Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer createdBy = body.getInteger(CREATED_BY);
                Integer driverId = body.getInteger(DRIVER_ID);
                String coordinates = body.getString("location_started");
                String typeTerminal = (String) body.remove("type_terminal");

                switch (body.getString("table")){
                    case "travel_tracking":
                        this.changeStatus(conn, body, typeTerminal).whenComplete((res,err)->{
                            try{
                                if(err!=null){
                                    this.rollback(conn,err,message);
                                }else{
                                    this.commit(conn,message,res);
                                }
                            }
                            catch (Exception e){
                                this.rollback(conn,e,message);
                            }
                        });
                        break;
                    case "in-transit":
                        this.changeStatus(conn, body, typeTerminal).whenComplete((res,err)->{
                            try{
                                if(err!=null){
                                    this.rollback(conn,err,message);
                                }else{
                                    this.InsertTracking(conn, terminalId, scheduleRouteId,"loaded", "IN-TRANSIT", createdBy)
                                            .whenComplete((resp,error)->{
                                                try {
                                                    if(error!=null){
                                                        this.rollback(conn,error,message);
                                                    }else{
                                                        this.insertDriverTracking(conn, scheduleRouteId, driverId, terminalId, TRAVELTRACKING_STATUS.IN_TRANSIT.getValue(), createdBy, coordinates).whenComplete((result, errorD)->{
                                                            try {
                                                                if(errorD!=null){
                                                                    this.rollback(conn,errorD,message);
                                                                }else {
                                                                    initScheduleRouteDriver(conn, terminalId, driverId, scheduleRouteId, createdBy).whenComplete((upRes,upErr)->{
                                                                        try {
                                                                            if(upErr!=null){
                                                                                this.rollback(conn,upErr,message);
                                                                            }else{
                                                                                this.commit(conn,message,res);
                                                                            }
                                                                        }catch (Exception e){
                                                                            this.rollback(conn,e,message);
                                                                        }
                                                                    });
                                                                }
                                                            }catch (Exception e){
                                                                this.rollback(conn,e,message);
                                                            }
                                                        });
                                                    }
                                                }catch (Exception e){
                                                    this.rollback(conn,e,message);
                                                }
                                            });
                                }
                            }
                            catch (Exception e){
                                this.rollback(conn,e,message);
                            }
                        });
                        break;
                    case "paused":
                        this.changeStatus(conn, body, typeTerminal).whenComplete((res,err)->{
                            try{
                                if(err!=null){
                                    this.rollback(conn,err,message);
                                }else{
                                    this.insertDriverTracking(conn,scheduleRouteId, driverId,terminalId, TRAVELTRACKING_STATUS.PAUSED.getValue(), createdBy, coordinates).whenComplete((result, errorD)->{
                                        try {
                                            if(errorD!=null){
                                                this.rollback(conn,errorD,message);
                                            }else {
                                                this.commit(conn,message,res);
                                            }
                                        }catch (Exception e){
                                            this.rollback(conn,e,message);
                                        }
                                    });
                                }
                            }
                            catch (Exception e){
                                this.rollback(conn,e,message);
                            }
                        });
                        break;
                    case "stopped":
                        this.changeStatus(conn, body, typeTerminal).whenComplete((res,err)->{
                            try{
                                if(err != null){
                                    throw err;
                                }
                                this.insertDriverTracking(conn,scheduleRouteId, driverId,terminalId, TRAVELTRACKING_STATUS.STOPPED.getValue(), createdBy, coordinates).whenComplete((result, errorD)->{
                                    try {
                                        if(errorD != null){
                                            throw errorD;
                                        }
                                        completeScheduleRouteDriver(conn, terminalId, driverId, scheduleRouteId, createdBy).whenComplete((upRes,upErr)->{
                                            try {
                                                if(upErr != null){
                                                    throw upErr;
                                                }
                                                this.commit(conn,message,res);
                                            }catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    }catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            }
                            catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                        break;
                    case "finished-ok":
                        this.changeStatus(conn, body, typeTerminal).whenComplete((res,err)->{
                            try{
                                if(err!=null){
                                    this.rollback(conn,err,message);
                                }else{
                                    this.insertDriverTracking(conn,scheduleRouteId, driverId,terminalId, TRAVELTRACKING_STATUS.STOPPED.getValue(), createdBy, coordinates).whenComplete((result, errorD)->{
                                        try {
                                            if(errorD!=null){
                                                this.rollback(conn,errorD,message);
                                            }else {
                                                completeScheduleRouteDriver(conn, terminalId, driverId, scheduleRouteId, createdBy).whenComplete((upRes,upErr)->{
                                                    try {
                                                        if(upErr!=null){
                                                            this.rollback(conn,upErr,message);
                                                        }else{
                                                            this.commit(conn,message,res);
                                                        }
                                                    }catch (Exception e){
                                                        this.rollback(conn,e,message);
                                                    }
                                                });
                                            }
                                        }catch (Exception e){
                                            this.rollback(conn,e,message);
                                        }
                                    });
                                }
                            }
                            catch (Exception e){
                                this.rollback(conn,e,message);
                            }
                        });
                    default:
                        this.rollback(conn,new Throwable("OPTION NOT FOUND"),message);
                        break;
                }
            }catch (Exception e){
                this.rollback(conn,e,message);
            }
        });
    }

    private CompletableFuture<Boolean> completeScheduleRouteDriver(SQLConnection conn, Integer terminalId, Integer driverId, Integer scheduleRouteId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_SCHEDULE_DRIVER + " AND terminal_destiny_id = ? " + QUERY_GET_SCHEDULE_DRIVER_ORDER_BY_ASC;
            JsonArray params =  new JsonArray()
                    .add(driverId)
                    .add(scheduleRouteId)
                    .add(terminalId);

            conn.queryWithParams(QUERY, params, reply->{
                try {
                    if(reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()){
                        future.completeExceptionally(new Throwable("Not found schedule_route_driver open"));
                    }

                    JsonObject currentSRD = reply.result().getRows().get(0);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                    Date updatedAt = formatter.parse(currentSRD.getString(UPDATED_AT));
                    Calendar calendar = Calendar.getInstance();
                    Long milliseconds = calendar.getTime().getTime()-updatedAt.getTime();

                    Long h = TimeUnit.MILLISECONDS.toHours(milliseconds);
                    Long min = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - h*60;
                    Long seg = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
                    String timeTracking = Time.valueOf(String.format("%d:%d:%d", h, min, seg)).toString();
                    String UPDATE = this.generateGenericUpdateString("schedule_route_driver",
                            currentSRD.put("time_tracking", timeTracking)
                                    .put("driver_status", "3")
                                    .put(Constants.STATUS, 2)
                                    .put(CREATED_BY, createdBy)
                                    .put("was_completed", 1));

                    conn.update(UPDATE, replyBatch -> {
                        try {
                            if (replyBatch.failed()){
                                throw replyBatch.cause();
                            }

                            future.complete(replyBatch.succeeded());

                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });

                }catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> initScheduleRouteDriver(SQLConnection conn, Integer terminalId, Integer driverId, Integer scheduleRouteId, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_SCHEDULE_DRIVER + " AND terminal_origin_id = ? " + QUERY_GET_SCHEDULE_DRIVER_ORDER_BY_ASC;
            JsonArray params =  new JsonArray()
                    .add(driverId)
                    .add(scheduleRouteId)
                    .add(terminalId);

            conn.queryWithParams(QUERY, params, reply->{
                try {
                    if(reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()){
                        future.completeExceptionally(new Throwable("Not found schedule_route_driver open"));
                    }

                    JsonObject currentSRD = reply.result().getRows().get(0);
                    String UPDATE = this.generateGenericUpdateString("schedule_route_driver",
                            currentSRD.put("driver_status", "2")
                                    .put(Constants.STATUS, 1)
                                    .put(UPDATED_BY,createdBy)
                                    .put(UPDATED_AT, FormatDate(Calendar.getInstance().getTime())));

                    conn.update(UPDATE, replyBatch -> {
                        try {
                            if (replyBatch.failed()){
                                throw replyBatch.cause();
                            }

                            future.complete(replyBatch.succeeded());

                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });

                }catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private String FormatDate(Date date){
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return format1.format(date);
    }

    //status of tracking
    private CompletableFuture<Boolean> insertDriverTracking(SQLConnection conn, Integer scheduleRouteId, Integer driverId, Integer terminalId, String Status, Integer createdBy, String location){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {

            JsonArray params = new JsonArray()
                    .add(driverId)
                    .add(terminalId)
                    .add(terminalId);

            conn.queryWithParams(QUERY_GET_DRIVER_TRACKING_ACTIVE, params, reply->{
                try {
                    if (reply.succeeded()){
                        if (reply.result().getNumRows()>0){
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");

                            JsonObject tracking = reply.result().getRows().get(0);
                            JsonObject update = new JsonObject()
                                    .put(ID,tracking.getInteger(ID))
                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                    .put(EMPLOYEE_ID, driverId)
                                    .put("location_finished",location)
                                    .put("updated_at",formatter.format(calendar.getTime()))
                                    .put("updated_by",createdBy);

                            Date date = formatter.parse(tracking.getString("created_at"));

                            Long milliseconds = calendar.getTime().getTime()-date.getTime();

                            Long h = TimeUnit.MILLISECONDS.toHours(milliseconds);
                            Long min = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - h*60;
                            Long seg = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
                            String timeTracking = Time.valueOf(String.format("%d:%d:%d",
                                    h,
                                    min,
                                    seg
                            )).toString();

                            update.put("time_tracking",timeTracking)
                                    .put("status", TRAVELTRACKING_STATUS.FINISHED.getValue());

                            String status = tracking.getString("status");
                            String action = tracking.getString("action");

                            //si se cambia a paused
                            JsonArray paramsOrder = new JsonArray()
                                    .add(terminalId)
                                    .add(scheduleRouteId);
                            String QUERY_GET_ORDER;
                            if(Status.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                                QUERY_GET_ORDER = QUERY_GET_ORDER_DESTINY;
                            }else {
                                QUERY_GET_ORDER = QUERY_GET_ORDER_ORIGIN;
                            }
                            conn.queryWithParams(QUERY_GET_ORDER, paramsOrder, result-> {
                                try{
                                    if(result.succeeded()){
                                        if(result.result().getNumRows()>0){
                                            JsonObject configDestination = result.result().getRows().get(0);
                                            String query = this.generateGenericUpdateString("driver_tracking",update);
                                            conn.update(query,res->{
                                                try{
                                                    if(res.succeeded()){
                                                        if(Status.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                                                            future.complete(res.succeeded());
                                                        }else{
                                                            configDestination.put("employee_id", driverId)
                                                                    .put("time_tracking",Time.valueOf("00:00:00").toString())
                                                                    .put("was_completed",0)
                                                                    .put("created_by",createdBy)
                                                                    .put("location_started", location);
                                                            if(Status.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                                                                configDestination
                                                                        .put("action","driving");
                                                            }else{
                                                                configDestination
                                                                        .put("action","waiting");
                                                            }
                                                            conn.update(this.generateGenericCreate("driver_tracking",configDestination),resp->{
                                                                try{
                                                                    if(resp.succeeded()){
                                                                        future.complete(resp.succeeded());
                                                                    }else {
                                                                        future.completeExceptionally(resp.cause());
                                                                    }
                                                                }catch (Exception e){
                                                                    future.completeExceptionally(e);
                                                                }
                                                            });
                                                        }
                                                    }else {
                                                        future.completeExceptionally(res.cause());
                                                    }
                                                }catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });


                                        }else {
                                            future.completeExceptionally(new Throwable("No se pudo obtener la configuracion de la ruta"));
                                        }
                                    }else{
                                        future.completeExceptionally(new Throwable("Error insertando el tracking del driver"));
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }
                        else{
                            //creamos un nuevo registro
                            JsonArray paramsOrder = new JsonArray()
                                    .add(terminalId)
                                    .add(scheduleRouteId);
                            String QUERY_GET_ORDER;
                            if(Status.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                                QUERY_GET_ORDER = QUERY_GET_ORDER_DESTINY;
                            }else {
                                QUERY_GET_ORDER = QUERY_GET_ORDER_ORIGIN;
                            }
                            conn.queryWithParams(QUERY_GET_ORDER, paramsOrder, result->{
                                try {
                                    if(result.succeeded()){
                                        if(result.result().getNumRows()>0){
                                            JsonObject info = result.result().getRows().get(0);
                                            info.put("employee_id", driverId)
                                                .put("time_tracking", "00:00:00")
                                                .put("was_completed",0)
                                                .put("created_by",createdBy)
                                                .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                                .put("location_started", location);
                                            String query = this.generateGenericCreate("driver_tracking",info);
                                            conn.update(query,res->{
                                                try {
                                                    if(res.succeeded()){
                                                        future.complete(res.succeeded());
                                                    }else{
                                                        future.completeExceptionally(res.cause());
                                                    }
                                                }catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        }else{
                                            future.completeExceptionally(new Throwable("No se pudo obtener la configuracion de la ruta"));
                                        }
                                    }else{
                                        future.completeExceptionally(result.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });

                        }
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> InsertTracking(SQLConnection conn, Integer terminalId, Integer scheduleRouteId, String status, String value, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(terminalId)
                    .add(scheduleRouteId)
                    .add(status);
            conn.queryWithParams(GET_COMPLEMENTS_TO_INSERT_TRACKING, params, reply->{
                try {
                        if(reply.succeeded()){
                            List<JsonObject> complements = reply.result().getRows();
                            conn.queryWithParams(GET_TICKETS_TO_INSERT_TRACKING, params, replyTickets->{
                                try {
                                    if(replyTickets.succeeded()){
                                        List<JsonObject> tickets = replyTickets.result().getRows();
                                        conn.queryWithParams(GET_PACKAGES_TO_INSERT_TRACKING, params, replyPackages->{
                                            try {
                                                if(replyPackages.succeeded()){
                                                    List<JsonObject> packages = replyPackages.result().getRows();
                                                    Integer lenC = complements.size();
                                                    Integer lenT = tickets.size();
                                                    Integer lenP = packages.size();

                                                    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();

                                                    for(int i=0; i<lenC;i++){
                                                        JsonObject shipmentComDetail = complements.get(i);
                                                        tasks.add(insertBoardingTracking(conn,"boardingpass_complement_id",shipmentComDetail.getInteger("boarding_pass_complement_id"), TRAVELTRACKING_ACTION.valueOf(value).getValue(),null,createdBy));
                                                    }

                                                    for(int i=0; i<lenT;i++){
                                                        JsonObject shipmentTickDetail = tickets.get(i);
                                                        tasks.add(insertBoardingTracking(conn,"boardingpass_ticket_id",shipmentTickDetail.getInteger("boarding_pass_ticket_id"), TRAVELTRACKING_ACTION.valueOf(value).getValue(),null,createdBy));
                                                    }

                                                    for(int i=0; i<lenP;i++){
                                                        JsonObject shipmentPackageDetail = packages.get(i);
                                                        tasks.add(insertPackageTracking(conn,shipmentPackageDetail.getInteger("parcel_id"), shipmentPackageDetail.getInteger("parcel_package_id"),terminalId, TRAVELTRACKING_ACTION.valueOf(value).getValue(),null,createdBy));
                                                    }

                                                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((bol,err)->{
                                                        try {
                                                            if(err!=null){
                                                                future.completeExceptionally(err);
                                                            }else{
                                                                future.complete(true);
                                                            }
                                                        }catch (Exception e){
                                                            future.completeExceptionally(e);
                                                        }
                                                    });
                                                }else{
                                                    future.completeExceptionally(replyPackages.cause());
                                                }
                                            }catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }else{
                                        future.completeExceptionally(replyTickets.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else{
                            future.completeExceptionally(reply.cause());
                        }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertPackageTracking(SQLConnection conn, Integer parcelId, Integer packageId,Integer terminalId, String action, String notes, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject()
                    .put("parcel_id",parcelId)
                    .put("parcel_package_id",packageId)
                    .put("terminal_id",terminalId)
                    .put("action",action)
                    .put("notes",notes)
                    .put(CREATED_BY,createdBy);
            String update = this.generateGenericCreate("parcels_packages_tracking",body);
            conn.update(update,reply->{
                try {
                    if(reply.succeeded()){
                        future.complete(true);
                    }else{
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertBoardingTracking(SQLConnection conn, String reference, Integer id, String action, String notes, Integer createdBy)                      {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String QUERY;
            if(reference.equals("boardingpass_complement_id")){
                QUERY = QUERY_GET_BOARDING_PASS_ID_OF_COMPLEMENT;
            }else{
                QUERY = QUERY_GET_BOARDING_PASS_ID_OF_TICKET;
            }
            JsonArray params = new JsonArray()
                    .add(id);
            conn.queryWithParams(QUERY,params,reply->{
                try {
                    if(reply.succeeded()){
                        Integer boardingPassId = reply.result().getRows().get(0).getInteger("boarding_pass_id");
                        JsonArray items = new JsonArray()
                                .add(new JsonObject()
                                        .put(ID,id)
                                        .put("boardingpass_id",boardingPassId));

                        String actionss = action;

                        if(action.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                            actionss = "init-route";
                        }

                        this.insertTracking(conn,items,"boarding_pass_tracking","boardingpass_id",reference,notes,actionss,createdBy)
                                .whenComplete((res,err)->{
                                    try {
                                        if(err!=null){
                                            future.completeExceptionally(err);
                                        }else{
                                            future.complete(res);
                                        }
                                    }catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> downloadingTravel(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer scheduleRouteId = body.getInteger("schedule_route_id");
            String scheduleRouteStatus = body.getString("schedule_status");
            Integer terminalId = body.getInteger("terminal_id");
            validateScheduleRouteLoading(conn,scheduleRouteId,scheduleRouteStatus).whenComplete((res,err)->{
                try {
                    if(err!=null){
                        future.completeExceptionally(err);
                    }else {
                        if(res){
                            JsonArray params = new JsonArray()
                                    .add(scheduleRouteId)
                                    .add(terminalId);
                            conn.queryWithParams(GET_DESTINATION_DESTINY_STATUS,params,reply->{
                            //conn.queryWithParams(GET_DESTINATION_ORIGIN_STATUS,params,reply->{
                                try {
                                    if(reply.succeeded()){
                                        if(reply.result().getNumRows()==0){
                                            future.completeExceptionally(new Throwable("NOT DESTINATIONS FOUND"));
                                        }else {
                                            String destinyStatus = reply.result().getRows().get(0).getString("destination_status");
                                            if(destinyStatus.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                                                JsonObject bodyUpd = new JsonObject()
                                                        .put("schedule_route_id",scheduleRouteId)
                                                        .put("terminal_id",terminalId)
                                                        .put("status","downloading")
                                                        .put(CREATED_BY, body.getInteger(CREATED_BY));
                                                changeStatus(conn, bodyUpd,"destiny").whenComplete((result,error)->{
                                                    try {
                                                        if(error!=null){
                                                            future.completeExceptionally(error);
                                                        }else{
                                                            future.complete(result);
                                                        }
                                                    }catch (Exception e){
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                            }else {
                                                future.completeExceptionally(new Throwable("CAN NOT INIT THE DOWNLOADING"));
                                            }
                                        }
                                    }else {
                                        future.completeExceptionally(reply.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else {
                            future.completeExceptionally(new Throwable("CANT INIT THE DOWNLOADING FOR THIS ROUTE"));
                        }
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> loadingTravel(SQLConnection conn, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer scheduleRouteId = body.getInteger("schedule_route_id");
            String scheduleRouteStatus = body.getString("schedule_status");
            Integer terminalId = body.getInteger("terminal_id");
            validateScheduleRouteLoading(conn,scheduleRouteId,scheduleRouteStatus).whenComplete((res,err)->{
                try {
                    if(err != null){
                        throw new Exception(err);
                    }
                    if(!res) {
                        throw new Exception("CANT INIT THE LOADING FOR THIS ROUTE");
                    }
                    JsonArray params = new JsonArray()
                            .add(scheduleRouteId)
                            .add(terminalId);
                    conn.queryWithParams(GET_DESTINATION_DESTINY_STATUS,params,reply->{
                        try {
                            if(reply.failed()) {
                                throw new Exception(reply.cause());
                            }
                            if(reply.result().getNumRows() == 0){
                                validateDestinationStatusLoading(conn,GET_DESTINATION_ORIGIN_STATUS,scheduleRouteId,terminalId,scheduleRouteStatus).whenComplete((desRes,desErr)->{
                                    try {
                                        if(desErr!=null){
                                            throw new Exception(desErr);
                                        }
                                        JsonObject bodyUpd = new JsonObject()
                                            .put("schedule_route_id",scheduleRouteId)
                                            .put("terminal_id",terminalId)
                                            .put("status","loading")
                                            .put(CREATED_BY, body.getInteger(CREATED_BY));
                                        changeStatus(conn, bodyUpd, "origin").whenComplete((result,error)->{
                                            try {
                                                if(error!=null){
                                                    throw new Exception(error);
                                                }
                                                JsonObject rep = new JsonObject()
                                                        .put("success", desRes);
                                                future.complete(rep);
                                            }catch (Exception e){
                                                e.printStackTrace();
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        future.completeExceptionally(e);
                                    }
                                });
                            }else {
                                String destinyStatus = reply.result().getRows().get(0).getString("destination_status");
                                //if(!destinyStatus.equals(STATUS.getString("FINISHED-OK"))) {
                                //    throw new Exception("CAN NOT INIT THE LOADING");
                                //}
                                validateDestinationStatusLoading(conn,GET_DESTINATION_ORIGIN_STATUS,scheduleRouteId,terminalId,scheduleRouteStatus).whenComplete((desRes,desErr)->{
                                    try {
                                        if(desErr!=null){
                                            throw new Exception(desErr);
                                        }
                                        JsonObject bodyUpd = new JsonObject()
                                                .put("schedule_route_id",scheduleRouteId)
                                                .put("terminal_id",terminalId)
                                                .put("status","loading")
                                                .put(CREATED_BY, body.getInteger(CREATED_BY));
                                        changeStatus(conn, bodyUpd, "origin").whenComplete((result,error)->{
                                            try {
                                                if(error!=null){
                                                    throw new Exception(error);
                                                }
                                                future.complete(new JsonObject()
                                                        .put("success", desRes));
                                            }catch (Exception e){
                                                e.printStackTrace();
                                                future.completeExceptionally(e);
                                            }
                                        });

                                    }catch (Exception e){
                                        e.printStackTrace();
                                        future.completeExceptionally(e);
                                    }
                                });
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    //Actualiza travel_tracking, schedule_route, schedule_route_destination
    private CompletableFuture<JsonObject> changeStatus(SQLConnection conn, JsonObject body, String typeTerminal){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray paramsGetStatus = new JsonArray()
                .add(body.getInteger("schedule_route_id"));
        Integer shipmentId = body.getInteger("shipment_id");
        body.remove("shipment_id");
        String status = body.getString("status");

        Integer scheduleRouteId = body.getInteger("schedule_route_id");
        Integer terminalId = body.getInteger("terminal_id");
        this.getStatusTravel(conn, QUERY_STATUS_TRACKING_TRAVEL, paramsGetStatus).whenComplete((res, err) -> {
            try {
                if (err != null) {
                    throw new Exception(err);
                }

                String scheduleStatus = res.getString("schedule_status");

                this.getDestinations(conn, scheduleRouteId, terminalId, typeTerminal).whenComplete((desReply,desErr)->{
                    try{
                        if(desErr!=null){
                            throw new Exception(desErr);
                        }
                        this.isValidStatus(status, scheduleStatus != null ? scheduleStatus : "", desReply).whenComplete((result, error) -> {
                            try {
                                if (error!=null){
                                    throw new Exception(error);
                                }
                                if(!RESPONSE_LIST.getString(0).equals(RESPONSE_LIST.getString(result))) {
                                    throw new Exception(RESPONSE_LIST.getString(result) + "FROM "
                                            + (res.getString("status") != null ? res.getString("status") : scheduleStatus !=null ? scheduleStatus:" TRAVEL STATUS NOT DEFINED"));
                                }
                                List<CompletableFuture> tasks = new ArrayList<>();
                                for(JsonObject destination:desReply){
                                    tasks.add(this.updDestination(conn, status, scheduleRouteId, destination.getInteger("id"),destination, terminalId, body ));
                                }
                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((__, errTasks) -> {
                                    try {
                                        if (errTasks != null) {
                                            throw new Exception(errTasks);
                                        }
                                        List<JsonObject> finishedOK = desReply.stream()
                                                .filter(d -> d.getString("status").equalsIgnoreCase(TRAVELTRACKING_STATUS.FINISHED_OK.getValue()))
                                                .collect(Collectors.toList());

                                        if (finishedOK.size() == desReply.size()) {
                                            body.put("status", TRAVELTRACKING_STATUS.FINISHED_OK.getValue());
                                        }

                                        this.getOriginTerminal(conn , body.getInteger("schedule_route_id"))
                                                .whenComplete( (resulT , errorT) ->{
                                                    try{
                                                        if(errorT!= null){
                                                            throw new Exception(errorT);
                                                        }
                                                        body.put("terminal_origin" , resulT);
                                                        this.updateRouteStatus(conn,body)
                                                                .whenComplete((resu,erro)->{
                                                                    try {
                                                                        if(erro!= null){
                                                                            throw new Exception(erro);
                                                                        }
                                                                        if(!resu) {
                                                                            throw new Exception("NOT UPDATED");
                                                                        }

                                                                        if(desReply.size() == 0) {
                                                                            throw new Exception("DESTINATIONS NOT FOUND");
                                                                        }

                                                                        if (shipmentId != null){
                                                                            this.updateParcels(conn, scheduleRouteId, terminalId).whenComplete((resultUP, errorUP) -> {
                                                                                try {
                                                                                    if (errorUP != null){
                                                                                        throw new Exception(errorUP);
                                                                                    }

                                                                                    future.complete(new JsonObject().put("success", true));

                                                                                } catch (Exception e){
                                                                                    e.printStackTrace();
                                                                                    future.completeExceptionally(e);
                                                                                }
                                                                            });
                                                                        } else {
                                                                            future.complete(new JsonObject().put("success", true));
                                                                        }

                                                                    } catch (Exception e){
                                                                        e.printStackTrace();
                                                                        future.completeExceptionally(e);
                                                                    }
                                                                });


                                                    }catch (Exception e){
                                                        e.printStackTrace();
                                                        future.completeExceptionally(e);
                                                    }
                                                });

                                    } catch(Exception ex) {
                                        ex.printStackTrace();
                                        future.completeExceptionally(ex);
                                    }
                                });

                            }catch (Exception e){
                                e.printStackTrace();
                                future.completeExceptionally(e);
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateParcels(SQLConnection conn, Integer scheduleRouteId, Integer terminalId){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();

        this.getParcelsByShipment(conn, scheduleRouteId, terminalId).whenComplete((parcels, errorParcels) -> {
           try {
               if (errorParcels != null){
                   throw errorParcels;
               }
                if (parcels.isEmpty()){
                    future.complete(true);
                    return;
                }
               parcels.forEach(p -> {
                   JsonObject parcel = (JsonObject) p;
                   Integer parcelId = parcel.getInteger("parcel_id"); //
                   Integer totalPackages = parcel.getInteger("total_packages");
                   Integer terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
                   boolean isTranshipment = parcel.getInteger("is_transhipment") > 0;
                   Integer transhipmentTerminalDestinyId = parcel.getInteger("transhipment_terminal_destiny_id");
                   boolean isInTranshipmentDestiny = terminalDestinyId.equals(transhipmentTerminalDestinyId);
                   // obtener si s transbordo e ignorar si es transbordo y no esta en su destino final
                   if (!isTranshipment || isInTranshipmentDestiny) {
                       this.getPackagesByParcelOfShipment(conn, parcelId).whenComplete((packages, errorPackages) -> {
                           try {
                               if (errorPackages != null){
                                   throw errorPackages;
                               }

                               List<String> querys = new ArrayList<>();
                               boolean isArrived = true;
                               int parcelStatus, countArrived = 0, countLocated = 0;

                               for(int i = 0; i < packages.size(); i++){
                                   JsonObject pack = packages.getJsonObject(i);
                                   Integer packageId = pack.getInteger(ID);

                                   JsonObject pptracking = new JsonObject()
                                           .put("parcel_id", parcelId)
                                           .put("parcel_package_id", packageId)
                                           .put(TERMINAL_ID, terminalId);

                                   String pptAction;
                                   int ppAction;

                                   if (terminalDestinyId.equals(terminalId)){
                                       pptAction = ParcelsPackagesTrackingDBV.ACTION.ARRIVED.name().toLowerCase();
                                       ppAction = PACKAGE_STATUS.ARRIVED.ordinal();
                                       countArrived ++;
                                   } else {
                                       isArrived = false;
                                       pptAction = ParcelsPackagesTrackingDBV.ACTION.LOCATED.name().toLowerCase();
                                       ppAction = PACKAGE_STATUS.LOCATED.ordinal();
                                       countLocated ++;
                                   }

                                   pptracking.put(ACTION, pptAction);

                                   querys.add(this.generateGenericCreate("parcels_packages_tracking", pptracking));

                                   JsonObject packageBody = new JsonObject().put("id", packageId).put("package_status", ppAction);
                                   querys.add(this.generateGenericUpdateString("parcels_packages", packageBody));
                               }

                               if (isArrived){
                                   parcelStatus = totalPackages.equals(countArrived) ? PARCEL_STATUS.ARRIVED.ordinal() : PARCEL_STATUS.ARRIVED_INCOMPLETE.ordinal();
                               } else {
                                   parcelStatus = totalPackages.equals(countLocated) ? PARCEL_STATUS.LOCATED.ordinal() : PARCEL_STATUS.LOCATED_INCOMPLETE.ordinal();
                               }

                               JsonObject updateParcel = new JsonObject()
                                       .put(ID, parcelId)
                                       .put("parcel_status", parcelStatus);

                               querys.add(this.generateGenericUpdateString("parcels", updateParcel));


                               conn.batch(querys, reply -> {
                                   try {
                                       if (reply.failed()){
                                           throw reply.cause();
                                       }

                                       future.complete(true);

                                   } catch (Throwable t){
                                       t.printStackTrace();
                                       future.completeExceptionally(t);
                                   }
                               });

                           } catch (Throwable t){
                               t.printStackTrace();
                               future.completeExceptionally(t);
                           }
                       });
                   }
               });
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getParcelsByShipment(SQLConnection conn, Integer scheduleRouteId, Integer terminalDestinyId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PARCELS_BY_SHIPMENT, new JsonArray().add(scheduleRouteId).add(terminalDestinyId), reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }

               List<JsonObject> results = reply.result().getRows();
               if (results.isEmpty()){
                   future.complete(new JsonArray());
               } else {
                   future.complete(new JsonArray(results));
               }

           } catch (Throwable t){
               t.printStackTrace();
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPackagesByParcelOfShipment(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKAGES_ARRIVED_BY_PARCEL_OF_SHIPMENT, new JsonArray().add(parcelId), reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }

               List<JsonObject> results = reply.result().getRows();
               future.complete(new JsonArray(results));

           } catch (Throwable t){
               t.printStackTrace();
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<Boolean> updDestination(SQLConnection conn, String Status, Integer scheduleRoute, Integer scheduleRouteDestination, JsonObject destination, Integer terminal, JsonObject body ){
        CompletableFuture<Boolean> future  = new CompletableFuture<>();
        try{
            Integer createdBy = body.getInteger(CREATED_BY);
            String status = Status;
            JsonArray params = new JsonArray();

            if(status.equals("ready-to-load")){
                if(destination.getInteger("terminal_destiny_id").equals(terminal)){
                    status = TRAVELTRACKING_STATUS.FINISHED_OK.getValue();
                    params.add(UtilsDate.sdfDataBase(UtilsDate.getDateConvertedTimeZone(UtilsDate.timezone, new Date())));
                }
            }

            if (status.equals("in-transit") || status.equals("stopped")){
                params.add(UtilsDate.sdfDataBase(UtilsDate.getDateConvertedTimeZone(UtilsDate.timezone, new Date())));
            }

            String QUERY = getQuery(status);


            params.add(scheduleRouteDestination);

            String finalStatus = status;

            conn.updateWithParams(QUERY,params, reply->{
                try{
                    if(reply.succeeded()){
                        destination.put("status", finalStatus);

                        JsonObject bodyTracking = new JsonObject().put(SCHEDULE_ROUTE_ID, scheduleRoute)
                                .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestination)
                                .put(CREATED_BY, createdBy)
                                .put(Constants.STATUS, finalStatus);
                        conn.update(this.generateGenericCreate("travel_tracking", bodyTracking), replyInsertTravelTracking -> {
                            try {
                                if (replyInsertTravelTracking.failed()){
                                    throw replyInsertTravelTracking.cause();
                                }

                                future.complete(reply.succeeded());

                            } catch (Throwable t){
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });

                    }else {
                        reply.cause().printStackTrace();
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateRouteStatus(SQLConnection conn,JsonObject body){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String status = body.getString("status");
            Integer id = body.getInteger("schedule_route_id");
            Integer updated_by = body.getInteger(CREATED_BY);

            String query = UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT;
            JsonArray params = new JsonArray()
                    .add(status)
                    .add(updated_by)
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()))
                    .add(UtilsDate.sdfDataBase(UtilsDate.getLocalDate()));
            if(body.getInteger("terminal_id").equals(body.getInteger("terminal_origin"))){
                query = UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT;
            }

            params.add(id);

            conn.updateWithParams(query,params,reply->{
                if(reply.succeeded()){
                    future.complete(true);
                }else {
                    future.completeExceptionally(reply.cause());
                }

            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getDestinations(SQLConnection conn, Integer schedule_route_id, Integer terminal_id, String typeTerminal){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try{

            String QUERY = GET_ROUTE_DESTINATIONS_BY_TERMINAL_TYPE;

            JsonArray params = new JsonArray()
                    .add(schedule_route_id)
                    .add(terminal_id);

            if (typeTerminal.equals("origin")){
                QUERY += " AND terminal_origin_id = ? ";
            } else if (typeTerminal.equals("destiny")){
                QUERY += " AND terminal_destiny_id = ? ";
            } else {
                QUERY = GET_ROUTE_DESTINATIONS_ALL;
                params.add(terminal_id);
            }

            conn.queryWithParams(QUERY, params,reply->{
                try {
                    if (reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> result = reply.result().getRows();

                    if (result.isEmpty()){
                        throw new Exception("DESTINATIONS NOT FOUND");
                    }

                    future.complete(result);
                    /*if (reply.succeeded()){
                        if (reply.result().getNumRows()>0){
                            List<CompletableFuture<List<JsonObject>>> task = new ArrayList<>();
                            for(JsonObject destination:reply.result().getRows()){
                                task.add(getAllDestinations(conn,schedule_route_id,destination.getInteger("terminal_origin_id")));
                            }
                            CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                                try {
                                    if(err!= null){
                                        future.completeExceptionally(err);
                                    }else {
                                        List<JsonObject> listReturn = new ArrayList<>();
                                        for(CompletableFuture<List<JsonObject>> list:task){
                                            List<JsonObject> listTwoCopy = new ArrayList<>(list.get());
                                            listTwoCopy.removeAll(listReturn);
                                            listReturn.addAll(listTwoCopy);
                                        }
                                        future.complete(listReturn);
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else{
                            future.completeExceptionally(new Throwable("DESTINATIONS NOT FOUND"));
                        }
                    }else{
                        future.completeExceptionally(reply.cause());
                    }*/
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
            return future;
        }catch (Exception e){
            future.completeExceptionally(e);
            return future;
        }

    }

    private String getQuery(String Status){
        String query = "";
        switch (Status){
            case "loading":
                query=UPDATE_SCHEDULE_ROUTE_DESTINATIONS_LOADING;
                break;
            case "in-transit":
                query = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_IN_TRANSIT;
                break;
            case "ready-to-go":
                query=UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_GO;
                break;
            case "paused":
                query=UPDATE_SCHEDULE_ROUTE_DESTINATIONS_PAUSED;
                break;
            case "stopped":
                query = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_STOPPED;
                break;
            case "downloading":
                query = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_DOWNLOADING;
                break;
            case "ready-to-load":
                query = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_LOAD;
                break;
            case "finished-ok":
                query = UPDATE_SCHEDULE_ROUTE_DESTINATIONS_FINISHED_OK;
                break;
        }
        return query;
    }

    private CompletableFuture<Integer> getOriginTerminal(SQLConnection conn,Integer schedule_route){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray()
                    .add(schedule_route);
            conn.queryWithParams(GET_TERMINAL_ORIGIN,params,reply->{
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    Integer destiny = reply.result().getRows().get(0).getInteger("terminal_origin_id");
                    future.complete(destiny);

                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Integer> isValidStatus(String status, String actStatus, List<JsonObject> destinations){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        if(destinations.size()<=0){
            future.completeExceptionally(new Throwable("DESTINATIONS NOT FOUND"));
            return future;
        }/*else if(destinations.size()==1 && status.equals(STATUS.getString("READY-TO-LOAD"))){
            future.completeExceptionally(new Throwable("CAN'T PASS TO READY-TO-LOAD; THERE NOT MOST DESTINATIONS, THE TRAVEL MUST BE FINISHED"));
            return future;
        }*/else{
            switch (status) {
                case "loading":
                    if((actStatus.equals("scheduled") ) || actStatus.equals(TRAVELTRACKING_STATUS.READY_TO_LOAD.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_LOADING.ordinal());
                    }
                    break;
                case "ready-to-go":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.LOADING.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_READY_TO_GO.ordinal());
                    }
                    break;
                case "in-transit":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.READY_TO_LOAD.getValue()) || actStatus.equals(TRAVELTRACKING_STATUS.PAUSED.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_IN_TRANSIT.ordinal());
                    }
                    break;
                case "stopped":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_STOPPED.ordinal());
                    }
                    break;
                case "downloading":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_DOWNLOADING.ordinal());
                    }
                    break;
                case "ready-to-load":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.DOWNLOADING.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_READY_TO_LOAD.ordinal());
                    }
                    break;
                case "paused":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_PAUSED.ordinal());
                    }
                    break;
                case "finished-ok":
                    if(actStatus.equals(TRAVELTRACKING_STATUS.DOWNLOADING.getValue())){
                        future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                    }else{
                        future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_FINISHED_OK.ordinal());
                    }
                    break;
                default:
                    future.complete(RESPONSE_CHANGE_STATUS.STATUS_NOT_IDENTIFIED.ordinal());
                    break;
            }
        }
        return future;
    }

    private CompletableFuture<Integer> isValidStatus(String status, String actStatus){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        switch (status) {
            case "loading":
                if(actStatus.equals("scheduled") || actStatus.equals(TRAVELTRACKING_STATUS.READY_TO_LOAD.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_LOADING.ordinal());
                }
                break;
            case "ready-to-go":
                if(actStatus.equals(TRAVELTRACKING_STATUS.LOADING.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_READY_TO_GO.ordinal());
                }
                break;
            case "in-transit":
                if(actStatus.equals(TRAVELTRACKING_STATUS.READY_TO_GO.getValue()) || actStatus.equals(TRAVELTRACKING_STATUS.PAUSED.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_IN_TRANSIT.ordinal());
                }
                break;
            case "stopped":
                if(actStatus.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_STOPPED.ordinal());
                }
                break;
            case "downloading":
                if(actStatus.equals(TRAVELTRACKING_STATUS.STOPPED.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_DOWNLOADING.ordinal());
                }
                break;
            case "ready-to-load":
                if(actStatus.equals(TRAVELTRACKING_STATUS.DOWNLOADING.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_READY_TO_LOAD.ordinal());
                }
                break;
            case "paused":
                if(actStatus.equals(TRAVELTRACKING_STATUS.IN_TRANSIT.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_PAUSED.ordinal());
                }
                break;
            case "finished-ok":
                if(actStatus.equals(TRAVELTRACKING_STATUS.DOWNLOADING.getValue())){
                    future.complete(RESPONSE_CHANGE_STATUS.OK.ordinal());
                }else{
                    future.complete(RESPONSE_CHANGE_STATUS.CANNOT_BE_CHANGED_TO_FINISHED_OK.ordinal());
                }
                break;
            default:
                future.complete(RESPONSE_CHANGE_STATUS.STATUS_NOT_IDENTIFIED.ordinal());
                break;
        }
        return future;
    }

    private CompletableFuture<Boolean> validateScheduleRouteLoading(SQLConnection conn, Integer scheduleRouteID, String status){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
            .add(scheduleRouteID);
        conn.queryWithParams(GET_ROUTE_STATUS, params, reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows()==0) {
                    throw new Exception("STATUS FOR SCHEDULE ROUTE NOT FOUND");
                }
                String scheduleRouteStatus = reply.result().getRows().get(0).getString("schedule_status");
                isValidStatus(status,scheduleRouteStatus).whenComplete((res,err)->{
                    try {
                        if(err != null){
                            throw new Exception(err);
                        }
                        future.complete(RESPONSE_LIST.getString(0).equals(RESPONSE_LIST.getString(res)));
                    }catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> validateDestinationStatusLoading(SQLConnection conn,String QUERY, Integer scheduleRouteID, Integer terminalId, String status){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(scheduleRouteID)
                .add(terminalId);

        conn.queryWithParams(QUERY, params, reply->{
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0) {
                    throw new Exception("STATUS FOR SCHEDULE ROUTE NOT FOUND");
                }
                String scheduleStatus = reply.result().getRows().get(0).getString("destination_status");
                isValidStatus(status,scheduleStatus).whenComplete((res,err)->{
                    try {
                        if(err!=null){
                            throw new Exception(err);
                        }
                        future.complete(RESPONSE_LIST.getString(0).equals(RESPONSE_LIST.getString(res)));
                    } catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getStatusTravel(SQLConnection conn, String QUERY, JsonArray params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY,params, reply->{
            try{
                if(reply.succeeded()){
                    if(reply.result().getNumRows()>0){
                        JsonObject result = reply.result().getRows().get(0);
                        result.put("success",true);
                        future.complete(result);
                    }else{
                        future.completeExceptionally(new Throwable("STATUS NOT FOUND FOR THAT ROUTE AND DESTINATION"));
                    }
                }else{
                    future.completeExceptionally(reply.cause());
                }
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }


    //<editor-fold defaultstate="collapsed" desc="queries">

    private final String QUERY_STATUS_TRACKING_TRAVEL = "SELECT schedule_status FROM schedule_route where id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_LOADING = "UPDATE schedule_route_destination set destination_status = 'loading' where id = ? and (destination_status='scheduled' or destination_status='ready-to-load')";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_GO = "UPDATE schedule_route_destination set destination_status='ready-to-go' where id = ? and destination_status='loading'";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_IN_TRANSIT = "UPDATE schedule_route_destination set destination_status='in-transit', started_at = ? where id= ?  and (destination_status='ready-to-go' or destination_status='paused')";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_PAUSED = "UPDATE schedule_route_destination set destination_status='paused' where id = ? and destination_status='in-transit' ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_STOPPED = "UPDATE schedule_route_destination set destination_status='stopped', finished_at = ? where id = ? and (destination_status='in-transit')";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_DOWNLOADING = "UPDATE schedule_route_destination set destination_status = 'downloading' where id = ? and (destination_status='stopped')";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_READY_TO_LOAD = "UPDATE schedule_route_destination set destination_status='ready-to-load' where id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_DESTINATIONS_FINISHED_OK = "UPDATE schedule_route_destination set destination_status = 'finished-ok', finished_at = ? where id = ? and (destination_status='downloading')";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_FINISHED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?, updated_at = ?, finished_at = ? WHERE id = ? ";

    private final static String UPDATE_SCHEDULE_ROUTE_STATUS_AND_STARTED_AT = "UPDATE schedule_route set schedule_status = ?, updated_by = ?,updated_at = ?, started_at = ? WHERE id = ?  ";

    private final static String GET_ROUTE_DESTINATIONS_ALL = "SELECT * FROM schedule_route_destination where schedule_route_id = ? and (terminal_origin_id = ? or terminal_destiny_id = ?) and destination_status != 'canceled' and destination_status != 'finished-ok'";

    private final static String GET_ROUTE_DESTINATIONS_BY_TERMINAL_TYPE = "SELECT * FROM schedule_route_destination " +
            "where schedule_route_id = ? \n" +
            "and destination_status != 'canceled' \n" +
            "and destination_status != 'finished-ok' ";

    private final static String GET_ROUTE_STATUS = "SELECT schedule_status FROM schedule_route where id = ? ;";

    private final static String GET_DESTINATION_DESTINY_STATUS = "SELECT destination_status FROM schedule_route_destination where schedule_route_id = ? AND terminal_destiny_id = ? AND destination_status NOT IN ('canceled', 'finished-ok') LIMIT 1 ;";

    private final static String GET_DESTINATION_ORIGIN_STATUS = "SELECT destination_status FROM schedule_route_destination where schedule_route_id = ? AND terminal_origin_id = ? AND destination_status!='canceled' and finished_at IS NULL LIMIT 1 ;";

    private final static String QUERY_GET_TERMINAL_DESTINY = "SELECT srd.terminal_destiny_id, cd.order_destiny, sr.config_route_id, sr.config_schedule_id " +
            " FROM schedule_route_destination AS srd " +
            " JOIN config_destination AS cd ON cd.id=srd.config_destination_id " +
            " JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id " +
            " where srd.schedule_route_id = ? AND srd.terminal_origin_id = ? AND srd.destination_status!='canceled' order by cd.order_destiny asc limit 1;";

    private final static String QUERY_GET_TRAVEL_LOG_CODE = "SELECT sr.config_route_id, DATE(sr.travel_date) AS travel, sr.config_schedule_id, (cd.order_destiny - 1 ) AS segment " +
            " FROM schedule_route_destination AS srd " +
            " JOIN config_destination AS cd ON cd.id=srd.config_destination_id " +
            " JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id " +
            " where srd.schedule_route_id = ? AND srd.terminal_origin_id = ? AND srd.destination_status!='canceled' order by cd.order_destiny asc limit 1;";

    private final static String QUERY_GET_SCHEDULE_ROW = "select @row := @row + 1 as row, cs.id, cs.config_route_id " +
            " from config_schedule AS cs, (SELECT @row := 0) r where cs.config_route_id = ? and cs.config_schedule_origin_id IS NULL;";

    private final static String GET_COMPLEMENTS_TO_INSERT_TRACKING = "SELECT " +
            "s.total_complements, " +
            "sc.* " +
            "FROM shipments_complement_tracking AS sc " +
            "INNER JOIN shipments AS s ON s.id = sc.shipment_id " +
            "WHERE s.terminal_id = ? AND s.schedule_route_id = ? AND sc.status = ? ";

    private final static String GET_TICKETS_TO_INSERT_TRACKING = "SELECT " +
            "s.total_tickets, " +
            "sc.* " +
            "FROM shipments_ticket_tracking AS sc " +
            "INNER JOIN shipments AS s ON s.id = sc.shipment_id " +
            "WHERE s.terminal_id = ? AND s.schedule_route_id = ? AND sc.status = ? ";

    private final static String GET_PACKAGES_TO_INSERT_TRACKING = "SELECT " +
            "s.total_packages, " +
            "sc.* " +
            "FROM shipments_parcel_package_tracking AS sc " +
            "INNER JOIN shipments AS s ON s.id = sc.shipment_id " +
            "WHERE s.terminal_id = ? AND s.schedule_route_id = ? AND sc.status = ? ";

    private static final String QUERY_GET_BOARDING_PASS_ID_OF_COMPLEMENT = "SELECT boarding_pass_id FROM boarding_pass_complement WHERE id = ? ; ";

    private static final String QUERY_GET_BOARDING_PASS_ID_OF_TICKET = "SELECT bpp.boarding_pass_id FROM boarding_pass_ticket AS bpt " +
            "INNER JOIN boarding_pass_passenger AS bpp ON bpp.id=bpt.boarding_pass_passenger_id " +
            "WHERE bpt.id = ? ;";

    private static final String QUERY_GET_DRIVER_TRACKING_ACTIVE = "SELECT id, time_tracking, status, action, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at FROM driver_tracking WHERE employee_id = ? and (terminal_origin_id = ? or terminal_destiny_id = ?) AND status = 'in-transit' ;";

    private static final String QUERY_GET_ORDER_ORIGIN = "SELECT " +
            "sr.vehicle_id, " +
            "cd.terminal_origin_id, " +
            "cd.terminal_destiny_id " +
            "FROM config_destination as cd " +
            "INNER JOIN schedule_route_destination as srd ON srd.config_destination_id = cd.id " +
            "INNER JOIN schedule_route as sr ON sr.id = srd.schedule_route_id " +
            "INNER JOIN branchoffice as bo ON bo.id = cd.terminal_origin_id " +
            "INNER JOIN branchoffice as bd ON bd.id = cd.terminal_destiny_id " +
            "where cd.terminal_origin_id = ? AND srd.schedule_route_id = ? AND (cd.order_origin+1)=cd.order_destiny;";

    private static final String QUERY_GET_ORDER_DESTINY = "SELECT sr.vehicle_id,cd.terminal_origin_id, cd.terminal_destiny_id " +

            "FROM config_destination as cd " +
            "INNER JOIN schedule_route_destination as srd ON srd.config_destination_id = cd.id " +
            "INNER JOIN schedule_route as sr ON sr.id = srd.schedule_route_id " +
            "INNER JOIN branchoffice as bo ON bo.id = cd.terminal_origin_id " +
            "INNER JOIN branchoffice as bd ON bd.id = cd.terminal_destiny_id " +
            "where cd.terminal_destiny_id = ? AND srd.schedule_route_id = ? AND (cd.order_origin+1)=cd.order_destiny;";

    private static final String QUERY_GET_SCHEDULE_DRIVER = "SELECT " +
            "  id, " +
            "  DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at, " +
            "  DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updated_at " +
            "FROM schedule_route_driver " +
            "WHERE employee_id = ? " +
            "AND schedule_route_id = ? " +
            "AND status = 1 ";
    private static final String QUERY_GET_SCHEDULE_DRIVER_ORDER_BY_ASC = " ORDER BY id ASC LIMIT 1; ";

    //nuevas querys de shipments
    private static final String QUERY_GET_SHIPMENT = "SELECT * FROM shipments where id = ? ;";

    private static final String QUERY_GET_SHIPMENT_BY_STATUS = "SELECT * FROM shipments WHERE schedule_route_id = ? AND (shipment_type = 'load' OR shipment_type = 'download') AND shipment_status = ?;";

    private static final String GET_SHIPMENT_LOAD_FOR_ROUTE_BY_TERMINAL = "SELECT s.* FROM shipments AS s \n" +
            "INNER JOIN schedule_route AS sr ON sr.id = s.schedule_route_id \n" +
            "INNER JOIN (select cd.* from config_destination AS cd " +
            "   INNER JOIN config_route AS cr ON cd.config_route_id = cr.id AND (cd.order_origin+1 = cd.order_destiny) ) AS config ON " +
            "   (config.terminal_destiny_id=? AND config.config_route_id=sr.config_route_id)\n" +
            "WHERE s.schedule_route_id = ? AND s.shipment_status <> 0 order by s.id desc LIMIT 1;";

    private static final String QUERY_GET_PACKAGES_BY_PARCEL = "SELECT " +
            "pp.id, " +
            "pp.shipping_type, " +
            "pp.package_status, " +
            "pp.package_code, " +
            "pp.total_amount, " +
            "pp.weight, " +
            "pp.height, " +
            "pp.width, " +
            "pp.length, " +
            "pp.notes, " +
            "pp.status, " +
            "pt.name AS package_type " +
            "FROM parcels_packages AS pp " +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id " +
            "where pp.parcel_id = ? ";

    private static final String QUERY_GET_PACKAGE_DETAILS_BY_ID = QUERY_GET_PACKAGES_BY_PARCEL + " AND pp.id = ? ";

    private static final String GET_PACKAGES_TO_DOWNLOAD_BY_PARCEL_ID = "SELECT sct.* " +
            "           FROM shipments_parcel_package_tracking AS sct " +
            "           INNER JOIN shipments AS s ON s.id = sct.shipment_id " +
            "           WHERE  " +
            "           sct.parcel_package_id NOT IN ( " +
            "              SELECT sct2.parcel_package_id FROM shipments_parcel_package_tracking AS sct2 " +
            "               INNER JOIN shipments AS s2 ON s2.id = sct2.shipment_id " +
            "               WHERE s2.schedule_route_id = ? " +
            "               AND s2.shipment_type != ? " +
            "               GROUP BY sct2.parcel_package_id " +
            "           ) " +
            "           AND s.schedule_route_id = ? " +
            "           AND s.shipment_type = ? " +
            "           AND sct.parcel_id = ? " +
            "           GROUP BY sct.parcel_package_id;";

    private static final String QUERY_GET_PARCEL_DETAILS_PASSENGER = "SELECT " +
            "id, " +
            "total_packages, " +
            "TIMESTAMPDIFF(HOUR, created_at, promise_delivery_date) AS delivery_time, " +
            "terminal_origin_id, " +
            "terminal_destiny_id, " +
            "parcel_tracking_code, " +
            "parcel_status, " +
            "status " +
            "FROM parcels where id = ? ";

    private static final String QUERY_GET_PARCEL_INFO = QUERY_GET_PARCEL_DETAILS_PASSENGER ;

    private static final String QUERY_MANEUVER_TIME_REPORT_V2 = "SELECT \n" +
            "    sr.id, \n" +
            "    srd.id AS destination, \n" +
            "    segment_origin.prefix, \n" +
            "    sr.vehicle_id, \n" +
            "    v.economic_number, \n" +
            "    CONCAT(e.name , ' ', e.last_name, COALESCE(CONCAT(', ', se.name , ' ', se.last_name), '')) AS operator,\n" +
            "    CONCAT(route_origin.prefix, '-', route_destiny.prefix) AS route, \n" +
            "    CONCAT(segment_origin.prefix, '-', segment_destiny.prefix) AS segment,\n" +
            "    info_ant.arrival_date,\n" +
            "    info_ant.init_download_at AS finished_at,\n" +
            "    srd.travel_date,\n" +
            "    ship.updated_at AS started_at,\n" +
            "    tl.travel_log_code,\n" +
            "    sr.code AS schedule_route_code,\n" +
            "    cd.order_origin\n" +
            "FROM travel_logs tl\n" +
            "{JOIN_SHIPMENTS}\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "INNER JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "INNER JOIN schedule_route_driver AS srdri ON  srdri.schedule_route_id = sr.id \n" +
            "   AND srd.terminal_origin_id = srdri.terminal_origin_id AND srd.terminal_destiny_id = srdri.terminal_destiny_id\n" +
            "INNER JOIN employee e ON e.id = srdri.employee_id\n" +
            "LEFT JOIN employee se ON se.id = srdri.second_employee_id\n" +
            "INNER JOIN branchoffice route_origin ON route_origin.id = cr.terminal_origin_id\n" +
            "INNER JOIN branchoffice route_destiny ON route_destiny.id = cr.terminal_destiny_id\n" +
            "INNER JOIN branchoffice segment_origin ON segment_origin.id = srd.terminal_origin_id\n" +
            "INNER JOIN branchoffice segment_destiny ON segment_destiny.id = srd.terminal_destiny_id\n" +
            "LEFT JOIN (\n" +
            "   SELECT \n" +
            "       cd_ant.order_origin, cd_ant.order_destiny, sr_ant.id AS schedule_route_id, srd_ant.arrival_date, sdown_ant.created_at AS init_download_at, srd_ant.terminal_origin_id, srd_ant.terminal_destiny_id, sdown_ant.id\n" +
            "   FROM config_destination cd_ant\n" +
            "   INNER JOIN schedule_route_destination srd_ant ON srd_ant.config_destination_id = cd_ant.id\n" +
            "   INNER JOIN schedule_route sr_ant ON sr_ant.id = srd_ant.schedule_route_id\n" +
            "   INNER JOIN travel_logs tl_ant ON tl_ant.schedule_route_id = sr_ant.id\n" +
            "       AND tl_ant.terminal_origin_id = srd_ant.terminal_origin_id AND tl_ant.terminal_destiny_id = srd_ant.terminal_destiny_id\n" +
            "   INNER JOIN shipments sdown_ant ON sdown_ant.id = tl_ant.download_id\n" +
            "    WHERE sr_ant.travel_date BETWEEN ? AND ?\n" +
            "       AND cd_ant.order_destiny = cd_ant.order_origin + 1\n" +
            ") AS info_ant ON info_ant.order_destiny = cd.order_origin\n" +
            "   AND info_ant.schedule_route_id = sr.id\n" +
            "WHERE (cd.order_destiny - cd.order_origin) = 1 \n" +
            "   AND sr.travel_date BETWEEN ? AND ? \n";
    private static final String QUERY_MANEUVER_TIME_REPORT = "SELECT sr.id, srd.id AS destination, b.prefix, sr.travel_date, sr.vehicle_id, v.economic_number, CONCAT(e.name , ' ', e.last_name ) AS operator, CONCAT(origin.prefix, '-', destiny.prefix)  AS route, CONCAT(ori.prefix, '-', des.prefix)  AS segment,\n" +
            "            srdA.arrival_date, srdA.finished_at, ROUND(( TIME_TO_SEC(TIMEDIFF(srdA.arrival_date, srdA.finished_at))/60))  AS arrival_dif,\n" +
            "            srd.travel_date, srd.started_at, ROUND(( TIME_TO_SEC(TIMEDIFF(srd.travel_date, srd.started_at))/60 )) AS departure_dif,\n" +
            "            ROUND(( ABS(TIME_TO_SEC(TIMEDIFF(srd.travel_date, srdA.arrival_date))/60 ))) AS tmd, ROUND(( TIME_TO_SEC(TIMEDIFF(srd.started_at, srdA.finished_at))/60 )) AS real_tmd, tl.travel_log_code,\n" +
            "            sr.code AS schedule_route_code\n" +
            "            FROM schedule_route_destination AS srd\n" +
            "            INNER JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id\n" +
            "            INNER JOIN config_route as cr ON cr.id = sr.config_route_id\n" +
            "            INNER JOIN branchoffice AS b ON b.id = srd.terminal_origin_id\n" +
            "            INNER JOIN vehicle AS v ON v.id = sr.vehicle_id\n" +
            "            INNER JOIN schedule_route_driver AS srdri ON  srdri.schedule_route_id = sr.id AND srd.terminal_origin_id=srdri.terminal_origin_id AND srd.terminal_destiny_id=srdri.terminal_destiny_id\n" +
            "            INNER JOIN employee AS e ON e.id = srdri.employee_id\n" +
            "            INNER JOIN config_destination AS cd ON cd.id=srd.config_destination_id\n" +
            "            LEFT JOIN (\n" +
            "            select srr.vehicle_id, srdd.schedule_route_id, srdd.travel_date, srdd.arrival_date, srdd.finished_at, cdd.order_origin, cdd.order_destiny, srdd.terminal_destiny_id\n" +
            "            from schedule_route_destination AS srdd\n" +
            "            INNER JOIN config_destination AS cdd ON cdd.id=srdd.config_destination_id\n" +
            "            INNER JOIN schedule_route AS srr ON srr.id=srdd.schedule_route_id\n" +
            "            WHERE (cdd.order_destiny - cdd.order_origin)=1   AND srr.travel_date BETWEEN ? AND ?) AS srdA\n" +
            "            ON srdA.vehicle_id=sr.vehicle_id and srdA.terminal_destiny_id=srd.terminal_origin_id and srdA.arrival_date < srd.travel_date AND srd.arrival_date >= DATE(srd.travel_date)\n" +
            "            INNER JOIN branchoffice AS destiny ON destiny.id=cr.terminal_destiny_id\n" +
            "            INNER JOIN branchoffice AS origin ON origin.id=cr.terminal_origin_id\n" +
            "            INNER JOIN branchoffice AS des ON des.id=srd.terminal_destiny_id\n" +
            "            INNER JOIN branchoffice AS ori ON ori.id=srd.terminal_origin_id\n" +
            "            LEFT JOIN travel_logs AS tl ON tl.schedule_route_id = sr.id  AND tl.terminal_origin_id = srd.terminal_origin_id AND tl.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "            where (cd.order_destiny - cd.order_origin)=1 AND sr.travel_date BETWEEN ? AND ?";

    private static final String GET_TICKET_INFO = "SELECT " +
            "   bpt.id, " +
            "   bpp.first_name, " +
            "   bpp.last_name, " +
            "   bpt.tracking_code, " +
            "   bpt.ticket_status, " +
            "   bpp.gender, " +
            "   bpt.parcel_id, " +
            "   st.name as special_ticket, " +
            "   srd.terminal_destiny_id, " +
            "   srd.schedule_route_id, " +
            "   srd.id as schedule_route_destination_id " +
            "   FROM boarding_pass_ticket as bpt " +
            "   INNER join boarding_pass_route as bpr on bpr.id = bpt.boarding_pass_route_id " +
            "   INNER join schedule_route_destination as srd on bpr.schedule_route_destination_id = srd.id " +
            "   INNER JOIN boarding_pass_passenger AS bpp ON bpp.id = bpt.boarding_pass_passenger_id  " +
            "   INNER JOIN special_ticket AS st ON st.id = bpp.special_ticket_id " +
            "   " +
            "   WHERE bpt.id = ? " +
            "   ORDER BY bpt.id";
    private static final String GET_TERMINAL_ORIGIN = "SELECT cr.terminal_origin_id FROM schedule_route AS sr JOIN config_route AS cr ON cr.id=sr.config_route_id where sr.id = ?";

    private static final String QUERY_GET_PARCELS_BY_SHIPMENT = "SELECT DISTINCT\n" +
            "  shipppt.parcel_id,\n" +
            "  p.terminal_destiny_id,\n" +
            "  p.total_packages,\n" +
            "  IF(pt.id IS NULL, FALSE, TRUE) AS is_transhipment,\n" +
            "  COALESCE(pt.terminal_destiny_id) AS transhipment_terminal_destiny_id\n" +
            "FROM shipments_parcel_package_tracking shipppt\n" +
            "INNER JOIN shipments AS s ON s.id = shipppt.shipment_id \n" +
            "LEFT JOIN shipments ship ON ship.id = shipppt.shipment_id\n" +
            "LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id\n" +
            "INNER JOIN travel_logs tl ON tl.download_id = ship.id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id AND srd.terminal_destiny_id = ship.terminal_id \n" +
            "INNER JOIN parcels p ON p.id = shipppt.parcel_id \n" +
            "WHERE p.parcel_status NOT IN (2, 4, 6)\n" +
            "AND sr.id = ?\n" +
            "AND srd.terminal_destiny_id = ?;";


    private static final String QUERY_GET_PACKAGES_ARRIVED_BY_PARCEL_OF_SHIPMENT = "SELECT\n" +
            "  pp.id\n" +
            " FROM parcels_packages pp\n" +
            " INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            " WHERE pp.package_status NOT IN (4, 7)\n" +
            " AND pp.package_status = 6\n" +
            " AND p.id = ?;";

    //</editor-fold>
}
