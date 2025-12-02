package database.shipments;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.commons.Status;
import database.routes.TravelTrackingDBV;
import database.shipments.handlers.ShipmentsDBV.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.*;
import static database.money.ReportDBV.END_DATE;
import static database.money.ReportDBV.INIT_DATE;
import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static service.commons.Constants.*;

/**
 *
 * @author Saúl
 */
public class ShipmentsDBV  extends DBVerticle {
    @Override
    public String getTableName() {
        return "shipments";
    }

    public static final String GENERIC_REGISTER = "ShipmentsDBV.genericRegister";
    public static final String FIND_SHIPMENTS_BY_TERMINAL = "ShipmentsDBV.findShipmentsByTerminal";
    public static final String CHECK_CODES = "ShipmentsDBV.checkCodes";
    public static final String GET_PASSENGERS_TO_BOARD = "ShipmentsDBV.passengersToBoard";
    public static final String GET_INCIDENCES = "ShipmentsDBV.getIncidences";
    public static final String FIND_SHIPMENTS_BY_TERMINAL_AND_TYPE = "ShipmentsDBV.findShipmentsByTerminalAndType";
    public static final String FIND_SHIPMENTS_TO_DO_LIST_BY_TYPE = "ShipmentsDBV.getToDoListByType";
    public static final String FIND_PARCELS_TO_DO_LIST = "ShipmentsDBV.getParcelsToDoList";
    public static final String ACTION_GET_HISTORIC_LOAD = "ShipmentsDBV.getHistoricLoad";
    public static final String ACTION_GET_HISTORIC_DOWNLOAD = "ShipmentsDBV.getHistoricDownload";
    public static final String GET_PASSENGERS_TO_DOWNLOAD = "ShipmentsDBV.passengersToDownload";
    public static final String ACTION_GET_DAILY_LOGS = "ShipmentsDBV.getDailyLogs";
    public static final String ACTION_GET_DAILY_LOGS_DETAIL = "ShipmentsDBV.getDailyLogsDetail";
    public static final String ACTION_GET_PRE_DAILY_LOGS_DETAIL = "ShipmentsDBV.getPreDailyLogsDetail";
    public static final String CANCEL_SHIPMENT_LOAD_DOWNLOAD = "ShipmentsDBV.cancelShipment";
    public static final String ACTION_GET_SHIPMENT_INFO = "ShipmentsDBV.getShipmentInfo";
    public static final String ACTION_GET_PARCELS_TO_DOWNLOAD = "ShipmentsDBV.getParcelsToDownload";
    public static final String ACTION_GET_DAILY_LOGS_BY_TYPE = "ShipmentsDBV.getDailyLogsByType";
    public static final String ACTION_GET_TRAVEL_LOG_LIST = "ShipmentDBV.getIncidenceLog";
    public static final String ACTION_GET_SHIPMENT_LOAD_INFO = "ShipmentDBV.getShipmentLoadInfo";
    public static final String ACTION_DOWNLOAD_PACKAGE_CODES = "ShipmentsDBV.DownloadPackageCodes";
    public static final String ACTION_TRANSHIPMENTS_TO_LOAD = "ShipmentsDBV.TranshipmentsToLoad";
    public static final String ACTION_GET_PARCELS_TO_DOWNLOAD_V2 = "ShipmentsDBV.getParcelsToDownloadV2";
    public static final String ACTION_ARRIVE_PACKAGE_CODES = "ShipmentsDBV.ArrivePackageCodes";
    public static final String ACTION_GET_DAILY_LOGS_COST_REPORT = "ShipmentsDBV.getDailyLogsCostReport";
    public static final String ACTION_GET_LOAD_CONCILIATION = "ShipmentsDBV.getLoadConciliation";
    public static final String ACTION_GET_DOWNLOAD_CONCILIATION = "ShipmentsDBV.getDownloadConciliation";
    public static final String ACTION_DELETE_PACKAGE_CODES = "ShipmentsDBV.DeletePackageCodes";
    public static final String ACTION_TRANSFER_TRAILER_TRACKING_CODE = "ShipmentsDBV.TransferTrailerTrackingCode";
    public static final String ACTION_GET_LOADED_PACKAGES_BY_SHIPMENT = "ShipmentsDBV.getLoadedPackagesByShipment";
    public static final String ACTION_SEARCH_PARCELS_TO_LOAD = "ShipmentsDBV.searchToLoad";
    public static final String ACTION_GET_LOADING_INFO = "ShipmentsDBV.getLoadingInfo";

    public static final String TYPE_SHIPMENT = "type_shipment";
    public static final String LOAD_ID = "load_id";
    public static final String TRAVEL_DATE = "travel_date";
    public static final String CONFIG_ROUTE_ID = "config_route_id";
    public static final String IS_PARCEL_ROUTE = "is_parcel_route";
    public static final String TERMINAL_ORIGIN_ID = "terminal_origin_id";
    public static final String SCHEDULE_ROUTE_ID = "schedule_route_id";
    public static final String TRAVEL_LOGS_ID = "travel_logs_id";
    public static final String LOAD = "load";
    public static final String DOWNLOAD = "download";
    public static final String DOWNLOAD_ID = "download_id";
    public static final String DETAIL = "detail";
    public static final String PARCELS_INCOMPLETE = "parcels_incomplete";
    public static final String PARCELS_PARTIALS = "parcels_partials";
    public static final String PACKAGES = "packages";
    public static final String PARCELS = "parcels";
    public static final String PARCELS_TRANSHIPMENTS = "parcels_transhipments";
    public static final String PACKAGES_TRANSHIPMENTS = "packages_transhipments";
    public static final String TICKETS = "tickets";
    public static final String COMPLEMENTS = "complements";
    public static final String TICKETS_TO_DOWNLOAD = "tickets_to_download";
    public static final String TICKETS_TO_OTHER_DESTINY = "tickets_to_other_destiny";
    public static final String COMPLEMENTS_TO_DOWNLOAD = "complements_to_download";
    public static final String COMPLEMENTS_TO_OTHER_DESTINY = "complements_to_other_destiny";
    public static final String PARCELS_TO_DOWNLOAD = "parcels_to_download";
    public static final String PARCELS_TRANSHIPMENTS_TO_DOWNLOAD = "parcels_transhipments_to_download";
    public static final String PARCELS_TO_OTHER_DESTINY = "parcels_to_other_destiny";
    public static final String PARCEL_ID = "parcel_id";
    public static final String SCHEDULE_ROUTE_DESTINATION_ID = "schedule_route_destination_id";
    public static final String ORDER_ORIGIN = "order_origin";
    public static final String ORDER_DESTINY = "order_destiny";
    public static final String INCLUDE_PARCELS = "include_parcels";
    private static final Integer MAX_LIMIT = 30;

    public enum SHIPMENT_TYPES {
        LOAD("load"),
        DOWNLOAD("download");

        String type;

        SHIPMENT_TYPES(String type){
            this.type = type;
        }

        public String getName(){
            return type;
        }

        public TravelTrackingDBV.SHIPMENT_TRACKING_STATUS getTrackingStatusByShipmentType(){
            if (this.equals(ShipmentsDBV.SHIPMENT_TYPES.LOAD)){
                return TravelTrackingDBV.SHIPMENT_TRACKING_STATUS.LOADED;
            } else {
                return TravelTrackingDBV.SHIPMENT_TRACKING_STATUS.DOWNLOADED;
            }
        }

    }

    private enum shipmentStatus{
            CANCELED,
            OPEN,
            CLOSE
    }

    public final JsonObject SHIPMENT_TYPE = new JsonObject()
            .put("LOAD","load")
            .put("DOWNLOAD","download");

    private final JsonObject actions = new JsonObject()
            .put("REGISTER","register")
            .put("PAID","paid")
            .put("MOVE","move")
            .put("IN-TRANSIT","intransit")
            .put("LOADED","loaded")
            .put("DOWNLOADED","downloaded")
            .put("DELIVERED","delivered")
            .put("INCIDENCE","incidence")
            .put("CANCELED","canceled")
            .put("CLOSED","closed");

    RegisterShipmentParcel registerShipmentParcel;
    ListShipmentParcel listShipmentParcel;
    LoadPackageCodes loadPackageCodes;
    DailyLogs dailyLogs;
    DailyLogsDetail dailyLogsDetail;
    DownloadPackageCodes downloadPackageCodes;
    TranshipmentsToLoad transhipmentsToLoad;
    ArrivePackageCodes arrivePackageCodes;
    LoadConciliation loadConciliation;
    DownloadConciliation downloadConciliation;
    DeleteCPPackages deleteCPPackages;
    TransferTrailerCPPackages transferTrailerCPPackages;
    LoadedPackagesByShipment loadedPackagesByShipment;
    SearchParcelsToLoad searchParcelsToLoad;
    LoadingInfo loadingInfo;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        registerShipmentParcel = new RegisterShipmentParcel(this);
        listShipmentParcel = new ListShipmentParcel(this);
        loadPackageCodes = new LoadPackageCodes(this);
        dailyLogs = new DailyLogs(this);
        dailyLogsDetail = new DailyLogsDetail(this);
        downloadPackageCodes = new DownloadPackageCodes(this);
        transhipmentsToLoad = new TranshipmentsToLoad(this);
        arrivePackageCodes = new ArrivePackageCodes(this);
        loadConciliation = new LoadConciliation(this);
        downloadConciliation = new DownloadConciliation(this);
        deleteCPPackages = new DeleteCPPackages(this);
        transferTrailerCPPackages = new TransferTrailerCPPackages(this);
        loadedPackagesByShipment = new LoadedPackagesByShipment(this);
        this.searchParcelsToLoad = new SearchParcelsToLoad(this);
        this.loadingInfo = new LoadingInfo(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case GENERIC_REGISTER:
                this.genericRegister(message);
                break;
            case FIND_SHIPMENTS_BY_TERMINAL:
                this.findShipmentsByTerminal(message);
                break;
            case CHECK_CODES:
                this.checkCodes(message);
                break;
            case GET_PASSENGERS_TO_BOARD:
                this.passengersToBoard(message);
                break;
            case GET_INCIDENCES:
                this.getIncidences(message);
                break;
            case FIND_SHIPMENTS_BY_TERMINAL_AND_TYPE:
                this.findShipmentsByTerminalAndType(message);
                break;
            case FIND_SHIPMENTS_TO_DO_LIST_BY_TYPE:
                this.getToDoListByType(message);
                break;
            case FIND_PARCELS_TO_DO_LIST:
                this.getParcelsToDoList(message);
                break;
            case ACTION_GET_HISTORIC_LOAD:
                this.getHistoricLoad(message);
                break;
            case ACTION_GET_HISTORIC_DOWNLOAD:
                this.getHistoricDownload(message);
                break;
            case GET_PASSENGERS_TO_DOWNLOAD:
                this.passengersToDownload(message);
                break;
            case ACTION_GET_DAILY_LOGS:
                dailyLogs.handle(message);
                break;
            case ACTION_GET_DAILY_LOGS_DETAIL:
                dailyLogsDetail.handle(message);
                break;
            case CANCEL_SHIPMENT_LOAD_DOWNLOAD:
                this.cancelShipment(message);
                break;
            case ACTION_GET_SHIPMENT_INFO:
                this.getShipmentInfo(message);
                break;
            case ACTION_GET_PARCELS_TO_DOWNLOAD:
                this.getParcelsToDownload(message);
                break;
            case ACTION_GET_DAILY_LOGS_BY_TYPE:
                this.getDailyLogsByType(message);
                break;
            case ACTION_GET_TRAVEL_LOG_LIST:
                this.getIncidenceLog(message);
                break;
            case RegisterShipmentParcel.ACTION:
                registerShipmentParcel.handle(message);
                break;
            case LoadPackageCodes.ACTION:
                loadPackageCodes.handle(message);
                break;
            case ACTION_GET_SHIPMENT_LOAD_INFO:
                this.getShipmentLoadInfo(message);
                break;
            case ACTION_DOWNLOAD_PACKAGE_CODES:
                downloadPackageCodes.handle(message);
                break;
            case ACTION_TRANSHIPMENTS_TO_LOAD:
                transhipmentsToLoad.handle(message);
                break;
            case ACTION_GET_PARCELS_TO_DOWNLOAD_V2:
                this.getParcelsToDownloadV2(message);
                break;
            case ACTION_ARRIVE_PACKAGE_CODES:
                arrivePackageCodes.handle(message);
                break;
            case ACTION_GET_DAILY_LOGS_COST_REPORT:
                this.getDailyLogsCostReport(message);
                break;
            case ACTION_GET_PRE_DAILY_LOGS_DETAIL:
                dailyLogsDetail.handle(message);
                break;
            case ACTION_GET_LOAD_CONCILIATION:
                loadConciliation.handle(message);
                break;
            case ACTION_GET_DOWNLOAD_CONCILIATION:
                downloadConciliation.handle(message);
                break;
            case ACTION_DELETE_PACKAGE_CODES:
                deleteCPPackages.handle(message);
                break;
            case ACTION_TRANSFER_TRAILER_TRACKING_CODE:
                transferTrailerCPPackages.handle(message);
                break;
            case ACTION_GET_LOADED_PACKAGES_BY_SHIPMENT:
                loadedPackagesByShipment.handle(message);
                break;
            case ACTION_SEARCH_PARCELS_TO_LOAD:
                this.searchParcelsToLoad.handle(message);
                break;
            case ACTION_GET_LOADING_INFO:
                this.loadingInfo.handle(message);
                break;
        }
    }

    @Override
    protected void findAll(Message<JsonObject> message) {
        String queryParam = message.body().getString("query");
        String fromParam = message.body().getString("from");
        String toParam = message.body().getString("to");

        String queryToExcecute;
        if (queryParam != null) {
            queryToExcecute = this.select(queryParam);
        } else {
            queryToExcecute = "select * from " + this.getTableName() + " where shipment_status != " + Status.DELETED.getValue();
        }
        //adds the limit for pagination
        if (fromParam != null && toParam != null) {
            queryToExcecute += " limit " + fromParam + "," + toParam;
        } else if (toParam != null) {
            queryToExcecute += " limit " + toParam;
        }
        dbClient.query(queryToExcecute, reply -> {
            if (reply.succeeded()) {
                message.reply(new JsonArray(reply.result().getRows()));
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void passengersToBoard(Message<JsonObject> message){
        startTransaction(message,conn->{
            try{
                JsonObject body = message.body();

                Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
                Integer terminalId = body.getInteger(TERMINAL_ID);
                JsonObject result = new JsonObject();

                CompletableFuture f1 = getTickets(conn,result,scheduleRouteId,terminalId, null);

                CompletableFuture.allOf(f1).whenComplete((res,error)->{
                    try {
                        if(error != null){
                            this.rollback(conn,error,message);
                        }else {
                            this.commit(conn,message,result);
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

    private void checkCodes(Message<JsonObject> message){
        startTransaction(message,conn->{
            try {
                JsonObject body = message.body();
                JsonArray codes = body.getJsonArray("codes");

                if(codes.isEmpty()) {
                    this.commit(conn, message, new JsonObject()
                            .put("badCodes", new JsonArray())
                            .put("codes_with_error", new JsonArray())
                    );
                    return;
                }

                Integer shipmentId = body.getInteger("shipment_id");
                Integer createdBy = body.getInteger("created_by");
                Boolean notScanned = body.getBoolean("not_scanned") != null ? body.getBoolean("not_scanned") : false;
                String status = body.getString("status");
                JsonArray boardingPassTickets = new JsonArray();
                JsonArray boardingPassComplements = new JsonArray();
                JsonArray parcelPackages = new JsonArray();
                JsonArray parcels = new JsonArray();
                JsonArray wrongCodes = new JsonArray();
                JsonArray badCodes = new JsonArray();
                JsonObject resCodes = new JsonObject();

                resCodes.put("tickets", boardingPassTickets)
                    .put("complements", boardingPassComplements)
                    .put("packages", parcelPackages)
                    .put("parcels", parcels)
                    .put("badCodes",badCodes)
                    .put("wrongCodes",wrongCodes);

                final Integer len = codes.size();
                List<CompletableFuture<JsonObject>> task = new ArrayList<>();

                for (int i=0;i<len; i++){
                    task.add(analizeCodes(resCodes,codes.getString(i)));
                }
                CompletableFuture.allOf(task.toArray(new CompletableFuture[len])).whenComplete((res,err)->{
                    try {
                        String traAction ;
                        if(status.equals(actions.getString("LOADED"))){
                            traAction = "loaded";
                        }else {
                            traAction = "downloaded";
                        }
                        JsonArray params = new JsonArray()
                                .add(shipmentId);
                        conn.queryWithParams(QUERY_GET_SHIPMENT_INFO, params, reply->{
                            try{
                                if(reply.succeeded()){
                                    if(reply.result().getNumRows()>0){
                                        JsonObject shipment = reply.result().getRows().get(0);
                                        if(!shipment.getInteger("shipment_status").equals(1)){
                                            throw new Throwable("Shipment is closed");
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
                                                    e.printStackTrace();
                                                    this.rollback(conn,e,message);
                                                }
                                            });
                                        }

                                    }else{
                                         throw new Throwable("Shipment not found");
                                    }
                                }else{
                                    throw reply.cause();
                                }
                            }catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                        this.rollback(conn,e, message);
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn,e,message);
            }
        });
    }

    private CompletableFuture<JsonObject> insertComplementsCodes(SQLConnection conn, JsonObject resCodes, JsonObject shipment, Integer ShipmentID, Integer createdBy, String status, String traAction){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray complementsCodes = resCodes.getJsonArray("complements");
            JsonArray wrongCodes = resCodes.getJsonArray("wrongCodes");
            final int len = complementsCodes.size();
            List<CompletableFuture<JsonArray>> task = new ArrayList<>();
            for (int i=0; i<len; i++){
                String code = complementsCodes.getString(i);
                if(status.equals(actions.getString("LOADED"))){
                    task.add(insertComplement(conn,wrongCodes,code, complementsCodes, shipment, ShipmentID, createdBy, status, traAction));
                }else{
                    task.add(insertComplementDownload(conn,wrongCodes,code, complementsCodes, shipment, ShipmentID, createdBy, status, traAction));
                }

            }
            CompletableFuture.allOf(task.toArray(new CompletableFuture[len])).whenComplete((res,err)->{
                try {
                    if (err != null){
                        future.completeExceptionally(err);
                    }else{
                        future.complete(resCodes);
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

    private CompletableFuture<JsonArray> insertComplement(SQLConnection conn, JsonArray wrongCodes, String code, JsonArray complementCodes, JsonObject shipment, Integer shipmentId, Integer createdBy, String status, String traAction){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(code)
                    .add(shipment.getValue("terminal_id"));
            conn.queryWithParams(QUERY_GET_COMPLEMENT_INFO.replace("{TERMINAL_ID}", "terminal_origin_id"),params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject complementInfo = reply.result().getRows().get(0);
                            Integer ticketId = complementInfo.getInteger("boarding_pass_ticket_id");
                            Integer complementId = complementInfo.getInteger("boarding_pass_complement_id");
                            Integer boardingPassId = complementInfo.getInteger("boarding_pass_id");
                            Integer boardingPassRouteId = complementInfo.getInteger("boarding_pass_route_id");
                            Boolean isRound = complementInfo.getLong("is_round") == 1;

                            if(!shipment.getInteger("schedule_route_id").equals(complementInfo.getInteger("schedule_route_id"))){
                                JsonObject wrongCode = new JsonObject()
                                        .put("CODE", code)
                                        .put("TYPE","COMPLEMENT")
                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                        .put("CAUSE","SCHEDULE ROUTE FOR COMPLEMENT DOES NOT MATCH WITH THE SCHEDULE ROUTE IN SHIPMENT");
                                wrongCodes.add(wrongCode);
                                complementCodes.remove(code);
                                future.complete(wrongCodes);
                            }else if(!complementInfo.getInteger("complement_status").equals(1)){
                                JsonObject wrongCode = new JsonObject()
                                        .put("CODE", code)
                                        .put("TYPE","COMPLEMENT")
                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                        .put("CAUSE","THE COMPLEMENT STATUS IS DIFFERENT TO REGISTERED");
                                wrongCodes.add(wrongCode);
                                complementCodes.remove(code);
                                future.complete(wrongCodes);
                            }else{
                                updateTrackingStatus(conn, UPDATE_COMPLEMENT_STATUS,2, complementId, createdBy)
                                        .whenComplete((traRes,traErr)->{
                                    try{
                                        if(traErr!=null){
                                            future.completeExceptionally(traErr);
                                        }else {
                                            if(traRes){

                                                this.validateShipmentBoardingPassTracking(conn, "complement", complementId, shipmentId, status).whenComplete((resultV, errorV) -> {
                                                    try {
                                                        if (errorV != null){
                                                            throw errorV;
                                                        }

                                                        if (resultV){
                                                            JsonObject create = new JsonObject()
                                                                    .put("boarding_pass_complement_id", complementId)
                                                                    .put("shipment_id",shipmentId)//boarding_pass_ticket_id
                                                                    .put("status",status)
                                                                    .put("created_by",createdBy);
                                                            String qCreate = this.generateGenericCreate("shipments_complement_tracking",create);
                                                            conn.update(qCreate, res->{
                                                                try {
                                                                    if(res.succeeded()){
                                                                        insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, traAction, null, createdBy)
                                                                                .whenComplete((resTra,errTra)->{
                                                                                    try {
                                                                                        if(errTra!=null){
                                                                                            future.completeExceptionally(errTra);
                                                                                        }else{
                                                                                            if(resTra){
                                                                                                future.complete(wrongCodes);
                                                                                            }else{
                                                                                                future.completeExceptionally(new Throwable("CAN NOT INSERT IN BOARDING TRACKING"));
                                                                                            }
                                                                                        }
                                                                                    }catch (Exception e){
                                                                                        future.completeExceptionally(e);
                                                                                    }
                                                                                });
                                                                    }else {
                                                                        future.completeExceptionally(res.cause());
                                                                    }
                                                                }catch (Exception e){
                                                                    future.completeExceptionally(e);
                                                                }
                                                            });
                                                        } else {
                                                            insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, traAction, null, createdBy)
                                                                    .whenComplete((resTra,errTra)->{
                                                                        try {
                                                                            if(errTra!=null){
                                                                                future.completeExceptionally(errTra);
                                                                            }else{
                                                                                if(resTra){
                                                                                    future.complete(wrongCodes);
                                                                                }else{
                                                                                    future.completeExceptionally(new Throwable("CAN NOT INSERT IN BOARDING TRACKING"));
                                                                                }
                                                                            }
                                                                        }catch (Exception e){
                                                                            future.completeExceptionally(e);
                                                                        }
                                                                    });
                                                        }
                                                    } catch (Throwable t){
                                                        future.completeExceptionally(t);
                                                    }
                                                });

                                            }else{
                                                future.completeExceptionally(new Throwable("CAN NOT UPDATE STATUS"));
                                            }
                                        }
                                    }catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                            }
                        }else {
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","COMPLEMENT")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","CODE NOT FOUND IN COMPLEMENTS");
                            wrongCodes.add(wrongCode);
                            complementCodes.remove(code);
                            future.complete(wrongCodes);
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

    private CompletableFuture<Boolean> insertBoardingTracking(SQLConnection conn, Integer boardingPassId, Boolean isRound, Integer boardingPassRouteId,
                                                              Integer boardingPassTicketId, Integer boardingPassComplementId, String action, String notes, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject insertTrackBody = new JsonObject()
                .put("boardingpass_id", boardingPassId)
                .put("notes", notes)
                .put("action", action)
                .put("created_by", createdBy)
                .put("boardingpass_ticket_id", boardingPassTicketId);
        if (boardingPassComplementId != null){
            insertTrackBody.put("boardingpass_complement_id", boardingPassComplementId);
        }

        String insertTracking = this.generateGenericCreate("boarding_pass_tracking", insertTrackBody);

        conn.update(insertTracking, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                int bpStatus, bprStatus;
                if(action.equals(actions.getString("LOADED"))){
                    bpStatus = 2;
                    bprStatus = 2;
                }else{
                    bpStatus = isRound ? 5 : 3;
                    bprStatus = 3;
                }

                List<String> task = new ArrayList<>();
                String queryUpdateBP = this.generateGenericUpdateString("boarding_pass", new JsonObject()
                        .put(ID, boardingPassId)
                        .put(BOARDINGPASS_STATUS, bpStatus)
                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                        .put(UPDATED_BY, createdBy));
                task.add(queryUpdateBP);

                String queryUpdateBPR = this.generateGenericUpdateString("boarding_pass_route", new JsonObject()
                        .put(ID, boardingPassRouteId)
                        .put("route_status", bprStatus)
                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                        .put(UPDATED_BY, createdBy));
                task.add(queryUpdateBPR);

                conn.batch(task, replyBatch -> {
                   try {
                       if (replyBatch.failed()){
                           throw replyBatch.cause();
                       }

                       future.complete(true);

                   } catch (Throwable t){
                       future.completeExceptionally(t);
                   }
                });

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertTicketsCodes(SQLConnection conn, JsonObject resCodes, JsonObject shipment, Integer ShipmentID, Integer createdBy, String status, String traAction, Boolean notScanned){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray ticketsCodes = resCodes.getJsonArray("tickets");
            JsonArray wrongCodes = resCodes.getJsonArray("wrongCodes");
            final int len = ticketsCodes.size();
            List<CompletableFuture<JsonArray>> task = new ArrayList<>();
            for (int i=0; i<len; i++){
                String code = ticketsCodes.getString(i);
                if(status.equals(actions.getString("LOADED"))){
                    task.add(insertTickets(conn,wrongCodes,code, ticketsCodes,shipment, ShipmentID, createdBy, status, traAction));
                }else{
                    task.add(insertTicketsDownload(conn,wrongCodes,code, ticketsCodes,shipment, ShipmentID, createdBy, status, traAction, notScanned));
                }
            }
            CompletableFuture.allOf(task.toArray(new CompletableFuture[len])).whenComplete((res,err)->{
                try {
                    if (err != null){
                        future.completeExceptionally(err);
                    }else{
                        future.complete(resCodes);
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

    private CompletableFuture<JsonArray> insertComplementDownload(SQLConnection conn, JsonArray wrongCodes, String code, JsonArray complementCodes, JsonObject shipment, Integer shipmentId, Integer createdBy, String status, String traAction){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(code)
                    .add(shipment.getValue("terminal_id"));
            conn.queryWithParams(QUERY_GET_COMPLEMENT_INFO.replace("{TERMINAL_ID}", "terminal_destiny_id"), params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject complementInfo = reply.result().getRows().get(0);
                            Integer ticketId = complementInfo.getInteger("boarding_pass_ticket_id");
                            Integer complementId = complementInfo.getInteger("boarding_pass_complement_id");
                            Integer boardingPassId = complementInfo.getInteger("boarding_pass_id");
                            Integer boardingPassRouteId = complementInfo.getInteger("boarding_pass_route_id");
                            Boolean isRound = complementInfo.getLong("is_round") == 1;

                            conn.queryWithParams(QUERY_GET_SHIPMENT_INFO_BY_COMPLEMENT,new JsonArray()
                                    .add(complementId),responseShipLoad->{
                                try{
                                    if(responseShipLoad.succeeded()){
                                        if(responseShipLoad.result().getNumRows()>0){
                                            JsonObject shipmentLoad = responseShipLoad.result().getRows().get(0);

                                            if(!shipmentLoad.getString("shipment_type").equals(SHIPMENT_TYPE.getString("LOAD"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","COMPLEMENT")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","CODE NOT FOUND IN LOAD INFO");
                                                wrongCodes.add(wrongCode);
                                                complementCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipmentLoad.getInteger("schedule_route_id").equals(shipment.getInteger("schedule_route_id"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","COMPLEMENT")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","SCHEDULE ROUTE NOT MATCH");
                                                wrongCodes.add(wrongCode);
                                                complementCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipmentLoad.getInteger("shipment_status").equals(2)){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","COMPLEMENT")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","THE SHIPMENT LOAD IN NOT CLOSE");
                                                wrongCodes.add(wrongCode);
                                                complementCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else{
                                                updateTrackingStatus(conn, UPDATE_COMPLEMENT_STATUS,3, complementId,createdBy).whenComplete((traRes,traErr)->{
                                                    try {
                                                        if (traErr != null) {
                                                            future.completeExceptionally(traErr);
                                                        } else {
                                                            if (traRes) {

                                                                this.validateShipmentBoardingPassTracking(conn, "complement", ticketId, shipmentId, status).whenComplete((resultV, errorV) -> {
                                                                    try {
                                                                        if (errorV != null){
                                                                            throw errorV;
                                                                        }

                                                                        if (resultV){
                                                                            JsonObject create = new JsonObject()
                                                                                    .put("boarding_pass_complement_id", complementId)
                                                                                    .put("shipment_id",shipmentId)
                                                                                    .put("status",status)
                                                                                    .put("created_by",createdBy);
                                                                            String qCreate = this.generateGenericCreate("shipments_complement_tracking",create);
                                                                            conn.update(qCreate, res->{
                                                                                try {
                                                                                    if(res.succeeded()){
                                                                                        List<CompletableFuture> tasksBPCTracking = new ArrayList<>();
                                                                                        tasksBPCTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, traAction,null,createdBy));

                                                                                        if (traAction.equals("downloaded")){
                                                                                            tasksBPCTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, "finished", null, createdBy));
                                                                                        }

                                                                                        CompletableFuture.allOf(tasksBPCTracking.toArray(new CompletableFuture[tasksBPCTracking.size()])).whenComplete((resTra,errTra)->{
                                                                                            try {
                                                                                                if(errTra!=null){
                                                                                                    future.completeExceptionally(errTra);
                                                                                                }else{
                                                                                                    future.complete(wrongCodes);
                                                                                                }
                                                                                            }catch (Exception e){
                                                                                                future.completeExceptionally(e);
                                                                                            }
                                                                                        });
                                                                                    }else {
                                                                                        future.completeExceptionally(res.cause());
                                                                                    }
                                                                                }catch (Exception e){
                                                                                    future.completeExceptionally(e);
                                                                                }
                                                                            });
                                                                        } else {
                                                                            List<CompletableFuture> tasksBPCTracking = new ArrayList<>();
                                                                            tasksBPCTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, traAction,null,createdBy));

                                                                            if (traAction.equals("downloaded")){
                                                                                tasksBPCTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, "finished", null, createdBy));
                                                                            }

                                                                            CompletableFuture.allOf(tasksBPCTracking.toArray(new CompletableFuture[tasksBPCTracking.size()])).whenComplete((resTra,errTra)->{
                                                                                try {
                                                                                    if(errTra!=null){
                                                                                        future.completeExceptionally(errTra);
                                                                                    }else{
                                                                                        future.complete(wrongCodes);
                                                                                    }
                                                                                }catch (Exception e){
                                                                                    future.completeExceptionally(e);
                                                                                }
                                                                            });
                                                                        }
                                                                    } catch (Throwable t){
                                                                        future.completeExceptionally(t);
                                                                    }
                                                                });

                                                            }else{
                                                                future.completeExceptionally(new Throwable("CAN NOT UPDATE STATUS"));
                                                            }
                                                        }
                                                    }catch (Exception e){
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                            }
                                        }else{
                                            JsonObject wrongCode = new JsonObject()
                                                    .put("CODE", code)
                                                    .put("TYPE","COMPLEMENT")
                                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                    .put("CAUSE","CODE NOT FOUND IN LOAD INFO");
                                            wrongCodes.add(wrongCode);
                                            complementCodes.remove(code);
                                            future.complete(wrongCodes);
                                        }
                                    }else {
                                        future.completeExceptionally(responseShipLoad.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else {
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","COMPLEMENT")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","CODE NOT FOUND IN TICKETS");
                            wrongCodes.add(wrongCode);
                            complementCodes.remove(code);
                            future.complete(wrongCodes);
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

    private CompletableFuture<JsonArray> insertTicketsDownload(SQLConnection conn, JsonArray wrongCodes, String code, JsonArray ticketCodes, JsonObject shipment, Integer shipmentId, Integer createdBy, String status, String traAction, Boolean notScanned){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(code)
                    .add(shipment.getInteger("terminal_id"));
            conn.queryWithParams(QUERY_GET_TICKET_INFO.replace("{TERMINAL_ID}", "terminal_destiny_id"),params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject ticketInfo = reply.result().getRows().get(0);
                            Integer ticketId = ticketInfo.getInteger("boarding_pass_ticket_id");
                            Integer complementId = ticketInfo.getInteger("boarding_pass_complement_id");
                            Integer boardingPassId = ticketInfo.getInteger("boarding_pass_id");
                            Integer boardingPassRouteId = ticketInfo.getInteger("boarding_pass_route_id");
                            Boolean isRound = ticketInfo.getLong("is_round") == 1 && ticketInfo.getString("ticket_type_route").equals("ida");

                            conn.queryWithParams(QUERY_GET_SHIPMENT_INFO_BY_TICKET,new JsonArray().add(ticketId),responseShipLoad->{
                                try{
                                    if(responseShipLoad.succeeded()){
                                        if(responseShipLoad.result().getNumRows()>0){
                                            JsonObject shipmentLoad = responseShipLoad.result().getRows().get(0);
                                            if(!shipmentLoad.getString("shipment_type").equals(SHIPMENT_TYPE.getString("LOAD"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","TICKET")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","CODE NOT FOUND IN LOAD INFO");
                                                wrongCodes.add(wrongCode);
                                                ticketCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipmentLoad.getInteger("schedule_route_id").equals(shipment.getInteger("schedule_route_id"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","TICKET")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","SCHEDULE ROUTE NOT MATCH");
                                                wrongCodes.add(wrongCode);
                                                ticketCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipmentLoad.getInteger("shipment_status").equals(2)){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","TICKET")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","THE SHIPMENT LOAD IN NOT CLOSE");
                                                wrongCodes.add(wrongCode);
                                                ticketCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else{
                                                updateTrackingStatus(conn,UPDATE_TICKET_STATUS,3, ticketId,createdBy).whenComplete((traRes,traErr)->{
                                                    try {
                                                        if (traErr != null) {
                                                            future.completeExceptionally(traErr);
                                                        } else {
                                                            if (traRes) {

                                                                this.validateShipmentBoardingPassTracking(conn, "ticket", ticketId, shipmentId, status).whenComplete((resultV, errorV) -> {
                                                                    try {
                                                                        if (errorV != null){
                                                                            throw errorV;
                                                                        }

                                                                        if (resultV){
                                                                            JsonObject create = new JsonObject()
                                                                                    .put("boarding_pass_ticket_id", ticketId)
                                                                                    .put("shipment_id",shipmentId)
                                                                                    .put("status",status)
                                                                                    .put("created_by",createdBy);
                                                                            if(notScanned){
                                                                                create.put("not_scanned",true);
                                                                            }
                                                                            String qCreate = this.generateGenericCreate("shipments_ticket_tracking",create);
                                                                            conn.update(qCreate, res->{
                                                                                try {
                                                                                    if(res.succeeded()){
                                                                                        List<CompletableFuture> taskBPTTracking = new ArrayList<>();
                                                                                        taskBPTTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, "finished", null, createdBy));
                                                                                        if (traAction.equals("downloaded")){
                                                                                            taskBPTTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, "finished", null, createdBy));
                                                                                        }

                                                                                        CompletableFuture.allOf(taskBPTTracking.toArray(new CompletableFuture[taskBPTTracking.size()]))
                                                                                                .whenComplete((resTra,errTra)->{
                                                                                                    try {
                                                                                                        if(errTra!=null){
                                                                                                            future.completeExceptionally(errTra);
                                                                                                        }else{
                                                                                                            future.complete(wrongCodes);
                                                                                                        }
                                                                                                    }catch (Exception e){
                                                                                                        future.completeExceptionally(e);
                                                                                                    }
                                                                                                });

                                                                                    }else {
                                                                                        future.completeExceptionally(res.cause());
                                                                                    }
                                                                                }catch (Exception e){
                                                                                    future.completeExceptionally(e);
                                                                                }
                                                                            });
                                                                        } else {
                                                                            List<CompletableFuture> taskBPTTracking = new ArrayList<>();
                                                                            taskBPTTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, "finished", null, createdBy));
                                                                            if (traAction.equals("downloaded")){
                                                                                taskBPTTracking.add(insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, "finished", null, createdBy));
                                                                            }

                                                                            CompletableFuture.allOf(taskBPTTracking.toArray(new CompletableFuture[taskBPTTracking.size()]))
                                                                                    .whenComplete((resTra,errTra)->{
                                                                                        try {
                                                                                            if(errTra!=null){
                                                                                                future.completeExceptionally(errTra);
                                                                                            }else{
                                                                                                future.complete(wrongCodes);
                                                                                            }
                                                                                        }catch (Exception e){
                                                                                            future.completeExceptionally(e);
                                                                                        }
                                                                                    });
                                                                        }
                                                                    } catch (Throwable t){
                                                                        future.completeExceptionally(t);
                                                                    }
                                                                });

                                                            }else{
                                                                future.completeExceptionally(new Throwable("CAN NOT UPDATE STATUS"));
                                                            }
                                                        }
                                                    }catch (Exception e){
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                            }
                                        }else{
                                            JsonObject wrongCode = new JsonObject()
                                                    .put("CODE", code)
                                                    .put("TYPE","TICKET")
                                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                    .put("CAUSE","CODE NOT FOUND IN LOAD INFO");
                                            wrongCodes.add(wrongCode);
                                            ticketCodes.remove(code);
                                            future.complete(wrongCodes);
                                        }
                                    }else {
                                        future.completeExceptionally(responseShipLoad.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else {
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","TICKET")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","CODE NOT FOUND IN TICKETS");
                            wrongCodes.add(wrongCode);
                            ticketCodes.remove(code);
                            future.complete(wrongCodes);
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

    private CompletableFuture<JsonArray> insertTickets(SQLConnection conn, JsonArray wrongCodes, String code, JsonArray ticketCodes, JsonObject shipment, Integer shipmentId, Integer createdBy, String status, String traAction){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(code)
                    .add(shipment.getValue("terminal_id"));
            conn.queryWithParams(QUERY_GET_TICKET_INFO.replace("{TERMINAL_ID}", "terminal_origin_id"),params,reply->{
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    if(reply.result().getNumRows()>0){
                        JsonObject ticketInfo = reply.result().getRows().get(0);
                        Integer ticketId = ticketInfo.getInteger("boarding_pass_ticket_id");
                        Integer complementId = ticketInfo.getInteger("boarding_pass_complement_id");
                        Integer boardingPassId = ticketInfo.getInteger("boarding_pass_id");
                        Integer boardingPassRouteId = ticketInfo.getInteger("boarding_pass_route_id");
                        Boolean isRound = ticketInfo.getLong("is_round") == 1;

                        if(!shipment.getInteger("schedule_route_id").equals(ticketInfo.getInteger("schedule_route_id"))){
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","TICKET")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","SCHEDULE ROUTE FOR TICKET DOES NOT MATCH WITH THE SCHEDULE ROUTE IN SHIPMENT");
                            wrongCodes.add(wrongCode);
                            ticketCodes.remove(code);
                            future.complete(wrongCodes);
                        }else if(!shipment.getInteger("shipment_status").equals(1)){
                            if(shipment.getInteger("shipment_status").equals(0)){
                                future.completeExceptionally(new Throwable("SHIPMENT ARE CANCELED"));
                            }else {
                                future.completeExceptionally(new Throwable("SHIPMENT IS ALREADY CLOSE"));
                            }
                        }else if(!ticketInfo.getInteger("ticket_status").equals(1)){
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","TICKET")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","THE TICKET STATUS IS DIFFERENT TO REGISTERED");
                            wrongCodes.add(wrongCode);
                            ticketCodes.remove(code);
                            future.complete(wrongCodes);
                        }else{
                            updateTrackingStatus(conn,UPDATE_TICKET_STATUS,2, ticketId,createdBy).whenComplete((traRes,traErr)->{
                                try {
                                    if (traErr != null) {
                                        future.completeExceptionally(traErr);
                                    } else {
                                        if (traRes) {

                                            this.validateShipmentBoardingPassTracking(conn, "ticket", ticketId, shipmentId, status).whenComplete((resultV, errorV) -> {
                                               try {
                                                   if (errorV != null){
                                                       throw errorV;
                                                   }

                                                   if (resultV){
                                                       JsonObject create = new JsonObject()
                                                               .put("boarding_pass_ticket_id", ticketId)
                                                               .put("shipment_id",shipmentId)
                                                               .put("status",status)
                                                               .put("created_by",createdBy);
                                                       String qCreate = this.generateGenericCreate("shipments_ticket_tracking",create);
                                                       conn.update(qCreate, res->{
                                                           try {
                                                               if(res.succeeded()){
                                                                   insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, traAction, null, createdBy)
                                                                           .whenComplete((resTra,errTra)->{
                                                                               try {
                                                                                   if(errTra!=null){
                                                                                       future.completeExceptionally(errTra);
                                                                                   }else{
                                                                                       if(resTra){
                                                                                           future.complete(wrongCodes);
                                                                                       }else{
                                                                                           future.completeExceptionally(new Throwable("CAN NOT INSERT IN BOARDING TRACKING"));
                                                                                       }
                                                                                   }
                                                                               }catch (Exception e){
                                                                                   future.completeExceptionally(e);
                                                                               }
                                                                           });
                                                               }else {
                                                                   future.completeExceptionally(res.cause());
                                                               }
                                                           }catch (Exception e){
                                                               future.completeExceptionally(e);
                                                           }
                                                       });
                                                   } else {
                                                       insertBoardingTracking(conn, boardingPassId, isRound, boardingPassRouteId, ticketId, complementId, traAction, null, createdBy)
                                                               .whenComplete((resTra,errTra)->{
                                                                   try {
                                                                       if(errTra!=null){
                                                                           future.completeExceptionally(errTra);
                                                                       }else{
                                                                           if(resTra){
                                                                               future.complete(wrongCodes);
                                                                           }else{
                                                                               future.completeExceptionally(new Throwable("CAN NOT INSERT IN BOARDING TRACKING"));
                                                                           }
                                                                       }
                                                                   }catch (Exception e){
                                                                       future.completeExceptionally(e);
                                                                   }
                                                               });
                                                   }

                                               } catch (Throwable t){
                                                   future.completeExceptionally(t);
                                               }
                                            });

                                        }else{
                                            future.completeExceptionally(new Throwable("CAN NOT UPDATE STATUS"));
                                        }
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }
                    }else {
                        JsonObject wrongCode = new JsonObject()
                                .put("CODE", code)
                                .put("TYPE","TICKET")
                                .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                .put("CAUSE","CODE NOT FOUND IN TICKETS");
                        wrongCodes.add(wrongCode);
                        ticketCodes.remove(code);
                        future.complete(wrongCodes);
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

    private CompletableFuture<Boolean> validateShipmentBoardingPassTracking(SQLConnection conn, String type, Integer referenceId, Integer shipmentId, String status){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String QUERY = type.equals("ticket") ? QUERY_VALIDATE_SHIPMENT_TICKET_TRACKING : QUERY_VALIDATE_SHIPMENT_COMPLEMENT_TRACKING;

        JsonArray params = new JsonArray()
                .add(referenceId)
                .add(shipmentId)
                .add(status);

        conn.queryWithParams(QUERY, params, reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }

               Boolean result = reply.result().getRows().isEmpty();
               future.complete(result);

           } catch (Throwable t) {
               future.completeExceptionally(t);
           }
        });

        return future;
    }

    private CompletableFuture<JsonObject> insertPackagesCodes(SQLConnection conn, JsonObject resCodes, JsonObject shipment, Integer ShipmentID, Integer createdBy, String status){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray ticketsCodes = resCodes.getJsonArray("packages");
            JsonArray wrongCodes = resCodes.getJsonArray("wrongCodes");
            final int len = ticketsCodes.size();
            List<CompletableFuture<JsonArray>> task = new ArrayList<>();
            for (int i=0; i<len; i++){
                String code = ticketsCodes.getString(i);
                if(status.equals(actions.getString("LOADED"))){
                    task.add(insertPackages(conn,wrongCodes,code, ticketsCodes, shipment, ShipmentID, createdBy, status));
                }else {
                    task.add(insertPackagesDownload(conn,wrongCodes,code, ticketsCodes, shipment, ShipmentID, createdBy, status));
                }

            }
            CompletableFuture.allOf(task.toArray(new CompletableFuture[len])).whenComplete((res,err)->{
                try {
                    if (err != null){
                        future.completeExceptionally(err);
                    }else{
                        future.complete(resCodes);
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

    private CompletableFuture<JsonArray> insertPackagesDownload(SQLConnection conn, JsonArray wrongCodes, String code, JsonArray packageCodes, JsonObject shipment, Integer shipmentId, Integer createdBy, String status){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            Integer shipmentTerminalId = shipment.getInteger("terminal_id");
            JsonArray params = new JsonArray()
                    .add(code)
                    .add(shipmentTerminalId);
            conn.queryWithParams(QUERY_GET_PACKAGE_INFO.replace("{TERMINAL_ID}", "terminal_destiny_id"),params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject packageInfo = reply.result().getRows().get(0);
                            Integer parcelPackageId = packageInfo.getInteger("parcel_package_id");
                            Integer parcelId = packageInfo.getInteger("parcel_id");

                            conn.queryWithParams(QUERY_GET_SHIPMENT_INFO_BY_PACKAGE,new JsonArray()
                                    .add(parcelPackageId),responseShipLoad->{
                                try{
                                    if(responseShipLoad.succeeded()){
                                        if(responseShipLoad.result().getNumRows()>0){
                                            JsonObject shipmentLoad = responseShipLoad.result().getRows().get(0);
                                            if(!shipmentLoad.getString("shipment_type").equals(SHIPMENT_TYPE.getString("LOAD"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","PACKAGE")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","CODE NOT FOUND IN LOAD INFO");
                                                wrongCodes.add(wrongCode);
                                                packageCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipmentLoad.getInteger("schedule_route_id").equals(shipment.getInteger("schedule_route_id"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","PACKAGE")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","SCHEDULE ROUTE NOT MATCH");
                                                wrongCodes.add(wrongCode);
                                                packageCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipmentLoad.getInteger("shipment_status").equals(2)){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","PACKAGE")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","THE SHIPMENT LOAD IS NOT CLOSE");
                                                wrongCodes.add(wrongCode);
                                                packageCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else{
                                                if(!packageInfo.getInteger("terminal_destiny_id").equals(shipmentTerminalId)){
                                                    JsonObject wrongCode = new JsonObject()
                                                            .put("CODE", code)
                                                            .put("TYPE","PACKAGE")
                                                            .put("MESSAGE","PACKAGE DOWNLOADED")
                                                            .put("CAUSE","THE TERMINAL ISN´T THE DESTINY");
                                                    wrongCodes.add(wrongCode);
                                                    packageCodes.remove(code);
                                                    future.complete(wrongCodes);
                                                    return;
                                                }

                                                this.validateShipmentParcelPackageTracking(conn, parcelId, parcelPackageId, shipmentId, status).whenComplete((resultValidation, errorV) -> {
                                                    try {
                                                        if (errorV != null) {
                                                            throw errorV;
                                                        }

                                                        if (resultValidation) {
                                                            JsonObject create = new JsonObject()
                                                                    .put("parcel_id", parcelId)
                                                                    .put("parcel_package_id", parcelPackageId)
                                                                    .put("shipment_id",shipmentId)
                                                                    .put("status",status)
                                                                    .put("created_by",createdBy);
                                                            String qCreate = this.generateGenericCreate("shipments_parcel_package_tracking",create);
                                                            conn.update(qCreate, resu->{
                                                                try {
                                                                    if(resu.succeeded()){
                                                                        updateParcelPackages(conn, parcelId, parcelPackageId,6,createdBy, shipmentTerminalId, "downloaded", packageInfo, shipmentId).whenComplete((upd,error)->{
                                                                            try {
                                                                                if(error!=null){
                                                                                    future.completeExceptionally(error);
                                                                                }else {
                                                                                    if(upd){

                                                                                        future.complete(wrongCodes);

                                                                                    }else {
                                                                                        future.completeExceptionally(new Throwable("Ocurrio un error inesperado insertando el tracking"));
                                                                                    }
                                                                                }
                                                                            }catch (Exception e){
                                                                                future.completeExceptionally(e);
                                                                            }
                                                                        });
                                                                    }else {
                                                                        future.completeExceptionally(resu.cause());
                                                                    }
                                                                }catch (Exception e){
                                                                    future.completeExceptionally(e);
                                                                }
                                                            });
                                                        } else {
                                                            updateParcelPackages(conn, parcelId, parcelPackageId,6,createdBy, shipmentTerminalId, "downloaded", packageInfo, shipmentId).whenComplete((upd,error)->{
                                                                try {
                                                                    if(error!=null){
                                                                        future.completeExceptionally(error);
                                                                    }else {
                                                                        if(upd){

                                                                            future.complete(wrongCodes);

                                                                        }else {
                                                                            future.completeExceptionally(new Throwable("Ocurrio un error inesperado insertando el tracking"));
                                                                        }
                                                                    }
                                                                }catch (Exception e){
                                                                    future.completeExceptionally(e);
                                                                }
                                                            });
                                                        }
                                                    } catch (Throwable t) {
                                                        future.completeExceptionally(t);
                                                    }
                                                });

                                                                    JsonObject create = new JsonObject()
                                                                            .put("parcel_id", parcelId)
                                                                            .put("parcel_package_id", parcelPackageId)
                                                                            .put("shipment_id",shipmentId)
                                                                            .put("status",status)
                                                                            .put("created_by",createdBy);
                                                                    String qCreate = this.generateGenericCreate("shipments_parcel_package_tracking",create);
                                                                    conn.update(qCreate, resu->{
                                                                        try {
                                                                            if(resu.succeeded()){
                                                                                updateParcelPackages(conn, parcelId, parcelPackageId,6,createdBy, shipmentTerminalId, "downloaded", packageInfo, shipmentId).whenComplete((upd,error)->{
                                                                                    try {
                                                                                        if(error!=null){
                                                                                            future.completeExceptionally(error);
                                                                                        }else {
                                                                                            if(upd){

                                                                                                future.complete(wrongCodes);

                                                                                            }else {
                                                                                                future.completeExceptionally(new Throwable("Ocurrio un error inesperado insertando el tracking"));
                                                                                            }
                                                                                        }
                                                                                    }catch (Exception e){
                                                                                        future.completeExceptionally(e);
                                                                                    }
                                                                                });
                                                                            }else {
                                                                                future.completeExceptionally(resu.cause());
                                                                            }
                                                                        }catch (Exception e){
                                                                            future.completeExceptionally(e);
                                                                        }
                                                                    });

                                            }
                                        }else{
                                            JsonObject wrongCode = new JsonObject()
                                                    .put("CODE", code)
                                                    .put("TYPE","TICKET")
                                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                    .put("CAUSE","CODE NOT FOUND IN LOAD INFO");
                                            wrongCodes.add(wrongCode);
                                            packageCodes.remove(code);
                                            future.complete(wrongCodes);
                                        }
                                    }else {
                                        future.completeExceptionally(responseShipLoad.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else {
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","TICKET")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","CODE NOT FOUND IN TICKETS");
                            wrongCodes.add(wrongCode);
                            packageCodes.remove(code);
                            future.complete(wrongCodes);
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

    private CompletableFuture<JsonArray> insertPackages(SQLConnection conn, JsonArray wrongCodes, String code, JsonArray packageCodes, JsonObject shipment, Integer shipmentId, Integer createdBy, String status){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(code)
                    .add(shipment.getValue("terminal_id"));
            conn.queryWithParams(QUERY_GET_PACKAGE_INFO.replace("{TERMINAL_ID}", "terminal_origin_id"),params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject packageInfo = reply.result().getRows().get(0);
                            Integer parcelId = packageInfo.getInteger("parcel_id");
                            Integer packageId = packageInfo.getInteger("parcel_package_id");
                            JsonArray paramsC = new JsonArray()
                                    .add(packageId);
                            conn.queryWithParams(QUERY_GET_TRACKING_PACKAGE,paramsC,ress->{
                                try {
                                    if(ress.succeeded()){
                                        if(ress.result().getNumRows()>0){
                                            JsonObject packageTracking = ress.result().getRows().get(0);
                                            if(!shipment.getInteger("terminal_id").equals(packageTracking.getInteger("terminal_id"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","PACKAGE")
                                                        .put("MESSAGE","SHIPMENT CLOSE")
                                                        .put("CAUSE","THE PACKAGE IS NOT IN THE TERMINAL OF SHIPMENT IS OPEN");
                                                wrongCodes.add(wrongCode);
                                                packageCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(!shipment.getInteger("shipment_status").equals(shipmentStatus.OPEN.ordinal())){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","PACKAGE")
                                                        .put("MESSAGE","SHIPMENT CLOSE")
                                                        .put("CAUSE","THE LOADING IS ALREADY CLOSED");
                                                wrongCodes.add(wrongCode);
                                                packageCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else if(packageTracking.getString("action").equals(actions.getString("LOADED"))
                                                    || packageTracking.getString("action").equals(actions.getString("CANCELED"))
                                                    || packageTracking.getString("action").equals(actions.getString("IN-TRANSIT"))
                                                    || packageTracking.getString("action").equals(actions.getString("CLOSED"))){
                                                JsonObject wrongCode = new JsonObject()
                                                        .put("CODE", code)
                                                        .put("TYPE","PACKAGE")
                                                        .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                                        .put("CAUSE","IS NOT POSSIBLE REGISTER THIS PACKAGE");
                                                wrongCodes.add(wrongCode);
                                                packageCodes.remove(code);
                                                future.complete(wrongCodes);
                                            }else{
                                                updateTrackingStatus(conn,UPDATE_PACKAGE_STATUS,1, packageId, createdBy)
                                                        .whenComplete((res,err)->{
                                                            try {
                                                                if(err!=null){
                                                                    future.completeExceptionally(err);
                                                                }else{

                                                                    this.validateShipmentParcelPackageTracking(conn, parcelId, packageId, shipmentId, status).whenComplete((resultValidation, errorV) -> {
                                                                        try {
                                                                            if (errorV != null){
                                                                                throw errorV;
                                                                            }

                                                                            if (resultValidation){
                                                                                JsonObject create = new JsonObject()
                                                                                        .put("parcel_id", parcelId)
                                                                                        .put("parcel_package_id", packageId)
                                                                                        .put("shipment_id",shipmentId)
                                                                                        .put("status",status)
                                                                                        .put("created_by",createdBy);
                                                                                String qCreate = this.generateGenericCreate("shipments_parcel_package_tracking",create);
                                                                                conn.update(qCreate, resu->{
                                                                                    try {
                                                                                        if(resu.succeeded()){
                                                                                            updateParcelPackages(conn, parcelId, packageId,1, createdBy, shipment.getInteger("terminal_id"),"loaded", packageInfo, shipmentId)
                                                                                                    .whenComplete((upd,error)->{
                                                                                                        try {
                                                                                                            if(error!=null){
                                                                                                                future.completeExceptionally(error);
                                                                                                            }else {
                                                                                                                if(upd){
                                                                                                                    future.complete(wrongCodes);
                                                                                                                }else {
                                                                                                                    future.completeExceptionally(new Throwable("Ocurrio un error inesperado insertando el tracking"));
                                                                                                                }
                                                                                                            }
                                                                                                        }catch (Exception e){
                                                                                                            future.completeExceptionally(e);
                                                                                                        }
                                                                                                    });
                                                                                        }else {
                                                                                            future.completeExceptionally(resu.cause());
                                                                                        }
                                                                                    }catch (Exception e){
                                                                                        future.completeExceptionally(e);
                                                                                    }
                                                                                });
                                                                            } else {
                                                                                updateParcelPackages(conn, parcelId, packageId,1, createdBy, shipment.getInteger("terminal_id"),"loaded", packageInfo, shipmentId)
                                                                                        .whenComplete((upd,error)->{
                                                                                            try {
                                                                                                if(error!=null){
                                                                                                    future.completeExceptionally(error);
                                                                                                }else {
                                                                                                    if(upd){
                                                                                                        future.complete(wrongCodes);
                                                                                                    }else {
                                                                                                        future.completeExceptionally(new Throwable("Ocurrio un error inesperado insertando el tracking"));
                                                                                                    }
                                                                                                }
                                                                                            }catch (Exception e){
                                                                                                future.completeExceptionally(e);
                                                                                            }
                                                                                        });
                                                                            }

                                                                        } catch (Throwable t){
                                                                            future.completeExceptionally(t);
                                                                        }
                                                                    });

                                                                }
                                                            }catch (Exception e){
                                                                future.completeExceptionally(e);
                                                            }
                                                        });
                                            }
                                        }else{
                                            future.completeExceptionally(new Throwable("NO INFORMATION FOUND FOR PACKAGE"));
                                        }
                                    }else{
                                        future.completeExceptionally(ress.cause());
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else {
                            JsonObject wrongCode = new JsonObject()
                                    .put("CODE", code)
                                    .put("TYPE","PACKAGE")
                                    .put("MESSAGE","CANT REGISTER THE CODE IN SHIPMENTS DETAILS")
                                    .put("CAUSE","CODE NOT FOUND IN PACKAGE");
                            wrongCodes.add(wrongCode);
                            packageCodes.remove(code);
                            future.complete(wrongCodes);
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

    private CompletableFuture<Boolean> validateShipmentParcelPackageTracking(SQLConnection conn, Integer parcelId, Integer packageId, Integer shipmentId, String status){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        JsonArray params = new JsonArray()
                .add(parcelId)
                .add(packageId)
                .add(shipmentId)
                .add(status);

        conn.queryWithParams(QUERY_VALIDATE_SHIPMENT_PARCEL_PACKAGE_TRACKING, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                Boolean result = reply.result().getRows().isEmpty();
                future.complete(result);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

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

    private CompletableFuture<Boolean> updateParcelPackages(SQLConnection conn, Integer parcelId, Integer packageId, Integer statusAct, Integer createdBy, Integer terminalId, String action, JsonObject packageInfo, Integer shipmentId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(shipmentId).add(packageInfo.getInteger("terminal_origin_id")).add(packageInfo.getInteger("terminal_destiny_id"));
            this.dbClient.queryWithParams(QUERY_GET_SCHEDULE_ROUTE_DESTINATION, params, replySrd -> {
                try {
                    if (replySrd.failed()) {
                        throw new Exception(replySrd.cause());
                    }
                    List<JsonObject> result = replySrd.result().getRows();
                    if(result.isEmpty() && action.equals("loaded")){
                        throw new Exception("Schedule route destination not found for this route");
                    }
                    Integer scheduleRouteDestinationId = result.get(0).getInteger("schedule_route_destination_id");

                    Calendar calendar = Calendar.getInstance();
                    JsonObject bodyParcel = new JsonObject()
                            .put("id", parcelId)
                            .put("parcel_status", 1)
                            .put("updated_by", createdBy)
                            .put("updated_at", FormatDate(calendar.getTime()));
                    if(action.equals("loaded")) bodyParcel.put("schedule_route_destination_id", scheduleRouteDestinationId);
                    GenericQuery gq = this.generateGenericUpdate("parcels", bodyParcel);

                    conn.updateWithParams(gq.getQuery(), gq.getParams(), reply -> {
                        try {
                            if (reply.succeeded()) {
                                JsonObject bodyPackage = new JsonObject()
                                        .put("id", packageId)
                                        .put("package_status", statusAct)
                                        .put("updated_by", createdBy)
                                        .put("updated_at", FormatDate(calendar.getTime()));

                                GenericQuery gqPackages = this.generateGenericUpdate("parcels_packages",bodyPackage);

                                conn.updateWithParams(gqPackages.getQuery(), gqPackages.getParams(),res->{
                                    try {
                                        if (res.succeeded()) {
                                            insertPackageTracking(conn, parcelId, packageId, terminalId, action, null, createdBy).whenComplete((tra, error) -> {
                                                try {
                                                    if (error != null) {
                                                        future.completeExceptionally(error);
                                                    } else {
                                                        if (tra) {
                                                            future.complete(true);
                                                        } else {
                                                            future.completeExceptionally(new Throwable("Ocurrio un error inesperado insertando el tracking"));
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        } else {
                                            future.completeExceptionally(res.cause());
                                        }
                                    } catch (Exception e) {
                                        future.completeExceptionally(e);
                                    }
                                });
                            } else {
                                future.completeExceptionally(reply.cause());
                            }
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateTrackingStatus(SQLConnection conn, String QUERY, Integer status, Integer id, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(status)
                    .add(createdBy)
                    .add(id);
            conn.updateWithParams(QUERY,params,reply->{
                try {
                    if(reply.succeeded()){
                        future.complete(reply.succeeded());
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

    //
    private void findShipmentsByTerminal(Message<JsonObject> message){
        startTransaction(message,conn->{
           try{
               JsonObject body = message.body();

               Integer employeeId = body.getInteger(EMPLOYEE_ID);
               Integer terminalId = body.getInteger(TERMINAL_ID);
               boolean includeParcels = body.getBoolean(INCLUDE_PARCELS, false);
               Integer routesTimeBeforeTravelDate = body.getInteger("routes_time_before_travel_date");
               Integer parcelRoutesTimeBeforeTravelDate = body.getInteger("parcel_routes_time_before_travel_date");
               String date = body.getString(DATE);
               JsonObject result = new JsonObject();

               CompletableFuture f1 = getRoutesToLoad(conn,result,date,employeeId,terminalId, null, null, null, includeParcels, false, 1, parcelRoutesTimeBeforeTravelDate);
               CompletableFuture f2 = getRoutesToDownload(conn,result,date,employeeId,terminalId, null, null, null, true, true, 1, parcelRoutesTimeBeforeTravelDate);
               //CompletableFuture f3 = getPackagesToShipment(conn,result,terminalId, null, null, null);

               CompletableFuture.allOf(f1,f2).whenComplete((res,err)->{
                   try {
                        if(err != null){
                            err.printStackTrace();
                            this.rollback(conn,err,message);
                        }else {
                            this.commit(conn,message,result);
                        }
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

    //
    private void findShipmentsByTerminalAndType(Message<JsonObject> message){
        startTransaction(message,conn->{
           try{
               JsonObject body = message.body();

               String type = body.getString("type");
               JsonObject employee = body.getJsonObject("employee");
               Integer employeeId = employee.getInteger(ID);
               Integer terminalId = body.getInteger("branchoffice_id");
               boolean includeParcels = body.getBoolean(INCLUDE_PARCELS, false);
               String date = body.getString("date");
               JsonObject result = new JsonObject();
               CompletableFuture fut = null;

               if (type.equals("load")){
                   fut = getRoutesToLoad(conn,result,date,employeeId,terminalId, null, null, null, includeParcels, true, 6, 6);
               } else if (type.equals("download")){
                   fut = getRoutesToDownload(conn,result, date,employeeId,terminalId, null, null, null, false, false, 6, 6);
               } else {
                   this.rollback(conn, new Throwable("Type not recognized"), message);
               }

               CompletableFuture.allOf(fut).whenComplete((res,err)->{
                   try {
                        if(err != null){
                            this.rollback(conn,err,message);
                        }else {
                            this.commit(conn,message,result);
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

    private CompletableFuture<JsonObject> getParcelsToLoad(SQLConnection conn, JsonObject result, Integer terminalId, Integer configRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer page , Integer limit){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.getParcelsWithLoadPackages(conn, configRouteId, terminalOriginId, terminalDestinyId, page, limit).whenComplete((listParcels,error)->{
                try {
                    if(error != null) {
                        throw error;
                    }

                    List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                    List<JsonObject> parcelsToList = listParcels.getJsonArray("results").getList();
                        for (int i=0; i<parcelsToList.size(); i++){
                            JsonObject parcel = parcelsToList.get(i);
                            Integer parcelId = parcel.getInteger("parcel_id");
                            task.add(getPackagesListPackages(conn,parcel,terminalId,parcelId));
                        }
                        CompletableFuture.allOf(task.toArray(new CompletableFuture[parcelsToList.size()])).whenComplete((res,pErr)->{
                            try {
                                if (pErr != null){
                                    future.completeExceptionally(pErr);
                                }else {
                                    result
                                        .put("count", listParcels.getInteger("count"))
                                        .put("items", listParcels.getInteger("items"))
                                        .put("results", parcelsToList);
                                    future.complete(listParcels);
                                }
                            }catch (Exception e){
                                future.completeExceptionally(e);
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

    private CompletableFuture<JsonObject> getRoutesToLoad(SQLConnection conn, JsonObject result, String date, Integer employeeId, Integer terminalId, Integer configRouteId, Integer terminalOriginId, Integer terminalDestinyId, boolean includeParcels, Boolean toLoad, Integer routesTimeBeforeTravelDate, Integer parcelRoutesTimeBeforeTravelDate){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();

            Date date1 = format1.parse(date);
            calendar.setTime(date1);
            calendar.add(Calendar.MINUTE,(routesTimeBeforeTravelDate.equals(1) ? 60 : routesTimeBeforeTravelDate) * -1);
            String pDate = format1.format(calendar.getTime());
            calendar.setTime(date1);
            calendar.add(Calendar.MINUTE, routesTimeBeforeTravelDate.equals(1) ? 360 : routesTimeBeforeTravelDate);
            String pDate2 = format1.format(calendar.getTime());

            Date parcelDate1 = format1.parse(date);
            calendar.setTime(parcelDate1);
            calendar.add(Calendar.MINUTE,(parcelRoutesTimeBeforeTravelDate.equals(1) ? 60 : parcelRoutesTimeBeforeTravelDate) * -1);
            String parcelDate = format1.format(calendar.getTime());
            calendar.setTime(parcelDate1);
            calendar.add(Calendar.MINUTE, parcelRoutesTimeBeforeTravelDate.equals(1) ? 360 : parcelRoutesTimeBeforeTravelDate);
            String parcelDate2 = format1.format(calendar.getTime());

            String query = String.format(QUERY_GET_SCHEDULE_ROUTE_ID_LOAD_V2, pDate, pDate2, parcelDate, parcelDate2);
            JsonArray params = new JsonArray();

            if (employeeId != null){
                String JOIN_EMPLOYEE = "INNER JOIN employee AS e ON srd.{TERMINAL_ID} = e.branchoffice_id";
                query = query.replace("{JOIN_EMPLOYEE}", JOIN_EMPLOYEE)
                        .replace("{TERMINAL_ID}", "terminal_origin_id");
                params.add(employeeId);
                query += " AND e.id = ?";
            } else {
                query = query.replace("{JOIN_EMPLOYEE}", " ");
            }

            if (configRouteId != null){
                query += " AND cd.config_route_id = ? ";
                params.add(configRouteId);
            }

            if (terminalOriginId != null){
                query += " AND cd.terminal_origin_id = ? ";
                params.add(terminalOriginId);
            }

            if (terminalDestinyId != null){
                query += " AND cd.terminal_destiny_id = ? ";
                params.add(terminalDestinyId);
            }

            query += " GROUP BY srd.schedule_route_id ORDER BY srd.travel_date;";

            conn.queryWithParams(query, params, reply->{
                try {
                    if(reply.succeeded()){
                        List<JsonObject> scheduleRoutes = reply.result().getRows();
                        if(!scheduleRoutes.isEmpty()){
                            final int lenRoutes = scheduleRoutes.size();
                            List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                            for (JsonObject scheduleRoute : scheduleRoutes) {
                                Integer scheduleRouteId = scheduleRoute.getInteger("schedule_route_id");
                                Integer vehicleId = (Integer) scheduleRoute.remove("vehicle_id");
                                Boolean isParcelRoute = scheduleRoute.getBoolean("parcel_route");
                                task.add(getShipmentByRoute(conn, scheduleRoute, scheduleRouteId, terminalId, "load"));
                                task.add(getDriverInformation(conn, scheduleRoute, scheduleRouteId));
                                task.add(getVehicleInformation(conn, scheduleRoute, vehicleId));
                                if (!isParcelRoute) {
                                    task.add(getDestinations(conn, scheduleRoute, scheduleRouteId, employeeId, terminalId, terminalDestinyId, toLoad, pDate, pDate2));
                                    task.add(getTickets(conn, scheduleRoute, scheduleRouteId, terminalId, terminalDestinyId));
                                } else {
                                    task.add(getDestinations(conn, scheduleRoute, scheduleRouteId, employeeId, terminalId, terminalDestinyId, toLoad, parcelDate, parcelDate2));
                                    task.add(getHitchedTrailers(scheduleRoute, scheduleRouteId));
                                    task.add(getTrailersToHitch(conn, scheduleRoute, terminalId));
                                }
                            }

                            CompletableFuture.allOf(task.toArray(new CompletableFuture[lenRoutes])).whenComplete((res,err)->{
                                try {
                                    if(err != null){
                                        future.completeExceptionally(err);
                                    }else {
                                        List<JsonObject> scheduleRoutesFiltered = scheduleRoutes.stream()
                                                    .filter(s -> s.containsKey("destinations"))
                                                    .collect(Collectors.toList());

                                        Collections.sort(scheduleRoutesFiltered, new Comparator<JsonObject>() {
                                            @Override
                                            public int compare(JsonObject t1, JsonObject t2) {
                                                try { return  t1.getJsonArray("destinations").getJsonObject(0).getString("origin_travel_date")
                                                .compareTo(t2.getJsonArray("destinations").getJsonObject(0).getString("origin_travel_date"));
                                                } catch (Exception e) {
                                                    throw new IllegalArgumentException(e);
                                                }
                                            }
                                        });

                                        result.put("schedule_routes_to_load",scheduleRoutesFiltered);
                                        future.complete(result);
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else{
                            result.put("schedule_routes_to_load",reply.result().getRows());
                            future.complete(result);
                        }
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getShipmentByRoute(SQLConnection conn,JsonObject scheduleRoute, Integer scheduleRouteId, Integer terminalId, String shipmentType){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(scheduleRouteId)
                    .add(terminalId)
                    .add(shipmentType);
            conn.queryWithParams(GET_SHIPMENT_OPEN, params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows() == 0){
                            future.complete(scheduleRoute);
                            return;
                        }
                        Integer shipmentId = reply.result().getRows().get(0).getInteger(ID);
                        scheduleRoute.put("schedule_shipment_id",shipmentId);
                        listShipmentParcel.setParcelsOnScheduleRoute(scheduleRoute, shipmentId).whenComplete((r, t) -> {
                            if(t != null) {
                               future.completeExceptionally(t);
                           } else {
                               future.complete(scheduleRoute);
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

    private CompletableFuture<JsonObject> getRoutesToDownload(SQLConnection conn, JsonObject result, String date, Integer employeeId, Integer terminalId, Integer configRouteId, Integer terminalOriginId, Integer terminalDestinyId, Boolean parcelDetail, Boolean notParcels, Integer routesTimeBeforeTravelDate, Integer parcelRoutesTimeBeforeTravelDate){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();

            Date parcelDate1 = format1.parse(date);
            calendar.setTime(parcelDate1);
            calendar.add(Calendar.MINUTE,(parcelRoutesTimeBeforeTravelDate.equals(1) ? 60 : parcelRoutesTimeBeforeTravelDate) * -1);
            String parcelDate = format1.format(calendar.getTime());
            calendar.setTime(parcelDate1);
            calendar.add(Calendar.MINUTE, parcelRoutesTimeBeforeTravelDate.equals(1) ? 360 : parcelRoutesTimeBeforeTravelDate);
            String parcelDate2 = format1.format(calendar.getTime());

            String query = String.format(QUERY_GET_SCHEDULE_ROUTE_ID_DOWNLOAD_V2, parcelDate, parcelDate2);
            JsonArray params = new JsonArray();

            if (employeeId != null){
                String JOIN_EMPLOYEE = "INNER JOIN employee AS e ON srd.{TERMINAL_ID} = e.branchoffice_id";
                query = query.replace("{JOIN_EMPLOYEE}", JOIN_EMPLOYEE)
                        .replace("{TERMINAL_ID}", "terminal_destiny_id");
                params.add(employeeId);
                query += " AND e.id = ?";
            } else {
                query = query.replace("{JOIN_EMPLOYEE}", " ");
            }

            if (configRouteId != null){
                query += " AND cd.config_route_id = ? ";
                params.add(configRouteId);
            }

            if (terminalOriginId != null){
                query += " AND cd.terminal_origin_id = ? ";
                params.add(terminalOriginId);
            }

            if (terminalDestinyId != null){
                query += " AND cd.terminal_destiny_id = ? ";
                params.add(terminalDestinyId);
            }

            query += " GROUP BY srd.schedule_route_id ORDER BY srd.arrival_date;";

            conn.queryWithParams(query, params, reply->{
                try {
                    if(reply.succeeded()){
                        List<JsonObject> scheduleRoutes = reply.result().getRows();
                        if(!scheduleRoutes.isEmpty()){
                            List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                            for (JsonObject scheduleRoute : scheduleRoutes) {
                                Integer scheduleRouteId = scheduleRoute.getInteger("schedule_route_id");
                                Integer vehicleId = (Integer) scheduleRoute.remove("vehicle_id");
                                Boolean isParcelRoute = scheduleRoute.getBoolean("parcel_route");
                                task.add(getShipmentByRoute(conn, scheduleRoute, scheduleRouteId, terminalId, "download"));
                                task.add(getDriverInformation(conn, scheduleRoute, scheduleRouteId));
                                task.add(getOrigins(conn, scheduleRoute, scheduleRouteId, employeeId, terminalId, terminalOriginId));
                                task.add(getVehicleInformation(conn, scheduleRoute, vehicleId));
                                if (!isParcelRoute) {
                                    task.add(getTicketsToDownload(conn, scheduleRoute, scheduleRouteId, terminalId, notParcels));
                                } else {
                                    task.add(getHitchedTrailers(scheduleRoute, scheduleRouteId));
                                }
                            }

                            CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                                try {
                                    if(err != null){
                                        future.completeExceptionally(err);
                                    }else {
//                                        List<JsonObject> scheduleRoutesFiltered = scheduleRoutes.stream()
//                                                .filter(s -> s.containsKey("origins")).sorted(new Comparator<JsonObject>() {
//                                                    @Override
//                                                    public int compare(JsonObject t1, JsonObject t2) {
//                                                        try {
//                                                            return t1.getJsonArray("origins").getJsonObject(0).getString("origin_travel_date")
//                                                                    .compareTo(t2.getJsonArray("origins").getJsonObject(0).getString("origin_travel_date"));
//                                                        } catch (Exception e) {
//                                                            throw new IllegalArgumentException(e);
//                                                        }
//                                                    }
//                                                }).collect(Collectors.toList());

                                            result.put("schedule_routes_to_download", scheduleRoutes);
                                        future.complete(result);
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else{
                            result.put("schedule_routes_to_download",reply.result().getRows());
                            future.complete(result);
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

    private CompletableFuture<JsonObject> getTicketsToDownload(SQLConnection conn,JsonObject scheduleRoute, Integer scheduleRouteId, Integer terminalId, Boolean notParcels){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY = GET_TICKETS_TO_DOWNLOAD;
            JsonArray params = new JsonArray()
                    .add(terminalId)
                    .add(scheduleRouteId)
                    .add("load");

            QUERY += " GROUP BY bpt.id";

            conn.queryWithParams(QUERY, params, reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getRows().isEmpty()){
                            scheduleRoute.put("tickets", new JsonArray());
                            future.complete(scheduleRoute);
                        } else {
                            List<JsonObject> shipmentsTickets = reply.result().getRows();
                            List<CompletableFuture<List<JsonObject>>> task = new ArrayList<>();
                            List<JsonObject> tickets = new ArrayList<>();
                            final Integer len = shipmentsTickets.size();
                            for(int i=0; i<len; i++){
                                JsonObject sTicket = shipmentsTickets.get(i);
                                task.add(getTicketInfo(conn,tickets, sTicket.getInteger("boarding_pass_ticket_id"), scheduleRouteId, notParcels));
                            }
                            CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                                try {
                                    if(err!=null){
                                        future.completeExceptionally(err);
                                    }else{
                                        scheduleRoute.put("tickets",tickets);
                                        future.complete(scheduleRoute);
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

    private CompletableFuture<List<JsonObject>> getTicketInfo(SQLConnection conn, List<JsonObject> tickets, Integer boardingPassTicketId, Integer scheduleRoteId, Boolean notParcels){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            JsonArray paramsInfoTicket = new JsonArray()
                    .add(boardingPassTicketId);
            conn.queryWithParams(GET_TICKET_INFO,paramsInfoTicket, reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject ticket = reply.result().getRows().get(0);
                            Integer parcelId = (Integer) ticket.remove("parcel_id");
                            List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                            if(parcelId!=null && !notParcels){
                                task.add(getPackagesToDownloadByPassenger(conn,ticket, scheduleRoteId,parcelId));
                            }

                            task.add(getComplementsByPassenger(conn,ticket,scheduleRoteId, true));
                            tickets.add(ticket);

                            CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((result, error) -> {
                                try {
                                    if (error != null){
                                        throw error;
                                    }

                                    future.complete(tickets);

                                } catch (Throwable t){
                                    future.completeExceptionally(t);
                                }
                            });
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
                                /*.add(scheduleRouteId)
                                .add("load")*/
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

    private CompletableFuture<JsonObject> getParcelsToDownload(JsonObject scheduleRoute, Integer scheduleRouteId, Integer terminalID){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(terminalID)
                    .add(scheduleRouteId);

            this.dbClient.queryWithParams(QUERY_GET_PARCEL_DETAILS_BY_SCHEDULE_ROUTE_ID, params, reply->{
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> resultParcels = reply.result().getRows();
                    List<CompletableFuture<Boolean>> tasksPackages = new ArrayList<>();
                    for (JsonObject parcel : resultParcels) {
                        tasksPackages.add(getPackagesDetailToDownload(parcel));
                    }
                    CompletableFuture.allOf(tasksPackages.toArray(new CompletableFuture[tasksPackages.size()])).whenComplete((res, err) -> {
                       try {
                           if (err != null) {
                               throw err;
                           }
                           scheduleRoute.put("parcels", new JsonArray(resultParcels));
                           future.complete(scheduleRoute);
                       } catch (Throwable t) {
                           future.completeExceptionally(t);
                       }
                    });
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> getPackagesDetailToDownload(JsonObject parcel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelId = parcel.getInteger(ID);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGE_DETAILS_BY_SCHEDULE_ROUTE_ID_V2, new JsonArray().add(parcelId), reply -> {
                try {
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    parcel.put("parcels_packages", new JsonArray(reply.result().getRows()));
                    future.complete(true);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getParcelsTranshipmentsToDownload(JsonObject scheduleRoute,Integer scheduleRouteId, int terminalId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(terminalId)
                    .add(scheduleRouteId);

            this.dbClient.queryWithParams(QUERY_GET_PACKAGE_TRANSHIPMENTS_DETAILS_BY_SCHEDULE_ROUTE_ID, params, reply->{
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> resultParcels = reply.result().getRows();
                    List<CompletableFuture<Boolean>> tasksPackages = new ArrayList<>();
                    for (JsonObject parcel : resultParcels) {
                        tasksPackages.add(getPackagesDetailToDownload(parcel));
                    }
                    CompletableFuture.allOf(tasksPackages.toArray(new CompletableFuture[tasksPackages.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            scheduleRoute.put("parcels_transhipments", new JsonArray(resultParcels));
                            future.complete(scheduleRoute);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getOptionalsParcelsToDownload(JsonObject scheduleRoute, Integer scheduleRouteId, Integer terminalID){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(terminalID)
                    .add(scheduleRouteId);

            this.dbClient.queryWithParams(QUERY_GET_OPTIONAL_PARCEL_DETAILS_BY_SCHEDULE_ROUTE_ID, params, reply->{
                try {
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> resultParcels = reply.result().getRows();
                    List<CompletableFuture<Boolean>> tasksPackages = new ArrayList<>();
                    for (JsonObject parcel : resultParcels) {
                        tasksPackages.add(getPackagesDetailToDownload(parcel));
                    }
                    CompletableFuture.allOf(tasksPackages.toArray(new CompletableFuture[tasksPackages.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            JsonArray parcels = scheduleRoute.getJsonArray("parcels");
                            scheduleRoute.put("parcels", parcels.addAll(new JsonArray(resultParcels)));
                            future.complete(scheduleRoute);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonArray> processWaybillsToDownload(List<JsonObject> resultParcels) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray waybills = new JsonArray();
        try {
            resultParcels.stream()
                    .collect(Collectors.groupingBy(x -> x.getInteger("parcel_id")))
                    .forEach((key, value) -> waybills.add(new JsonObject()
                            .put("id", value.get(0).getInteger("parcel_id"))
                            .put("total_packages", value.get(0).getInteger("total_packages"))
                            .put("segment", value.get(0).getString("segment"))
                            .put("delivery_time", value.get(0).getInteger("delivery_time"))
                            .put("terminal_origin_id", value.get(0).getInteger("terminal_origin_id"))
                            .put("terminal_destiny_id", value.get(0).getInteger("terminal_destiny_id"))
                            .put("parcel_tracking_code", value.get(0).getString("parcel_tracking_code"))
                            .put("waybill", value.get(0).getString("waybill"))
                            .put("parcel_status", value.get(0).getInteger("parcel_status"))
                            .put("optional", value.get(0).getInteger("optional") == 1)
                            .put("status", value.get(0).getInteger("status"))
                            .put("created_at", value.get(0).getString("created_at"))
                            .put("parcels_packages", new JsonArray(value))));

            waybills.stream().map(x -> (JsonObject)x)
                    .forEach(x ->
                            x.getJsonArray("parcels_packages").stream()
                                    .map(y->(JsonObject)y)
                                    .forEach(y -> {
                                        y.remove("parcel_id");
                                        y.remove("total_packages");
                                        y.remove("delivery_time");
                                        y.remove("terminal_origin_id");
                                        y.remove("terminal_destiny_id");
                                        y.remove("parcel_tracking_code");
                                        y.remove("parcel_status");
                                        y.remove("optional");
                                        y.remove("status");
                                        y.remove("created_at");
                                    })
                    );
            future.complete(waybills);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private void getParcelsToDownload(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer terminalId =  body.getInteger(TERMINAL_ID);
            Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
            int page = body.getInteger(PAGE);
            int limit  = body.getInteger(LIMIT);

            JsonArray params = new JsonArray()
                    .add(terminalId)
                    .add(terminalId)
                    .add(scheduleRouteId);

            String QUERY_COUNT = "SELECT COUNT(*) AS count FROM("+GET_PARCELS_TO_DOWNLOAD+") AS parcels_to_download;";

            Future f1 = Future.future();
            this.dbClient.queryWithParams(QUERY_COUNT, params.copy(), f1.completer());

            Future f2 = Future.future();
            params.add(limit).add((page - 1) * limit);
            this.dbClient.queryWithParams(GET_PARCELS_TO_DOWNLOAD.concat(" ORDER BY p.created_at DESC ").concat(" LIMIT ? OFFSET ? "), params, f2.completer());

            this.startTransaction(message, conn -> {

                CompositeFuture.all(f1, f2).setHandler(reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }

                        List<JsonObject> parcelsList = reply.result().<ResultSet>resultAt(1).getRows();

                        if (parcelsList.isEmpty()){
                            this.commit(conn, message, new JsonObject()
                                    .put("count", 0)
                                    .put(ITEMS, parcelsList.size())
                                    .put(RESULTS, parcelsList));
                        } else {
                            List<CompletableFuture<List<JsonObject>>> task = new ArrayList<>();
                            List<JsonObject> parcels = new ArrayList<>();
                            final int len = parcelsList.size();
                            for(int i=0; i<len; i++){
                                JsonObject parcel = parcelsList.get(i);
                                Integer parcelId = parcel.getInteger(ID);
                                task.add(getParcelInfo(conn, parcels, scheduleRouteId, parcelId));
                            }
                            CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                                try {
                                    if(err != null){
                                        throw new Exception(err);
                                    }

                                    this.commit(conn, message, new JsonObject()
                                            .put("count", reply.result().<ResultSet>resultAt(0).getRows().get(0).getInteger("count"))
                                            .put(ITEMS, parcels.size())
                                            .put(RESULTS, parcels));

                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                        }

                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });

            });

        } catch (Exception e){
            e.printStackTrace();
            reportQueryError(message, e);
        }

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
                                /*.add(scheduleRouteId)
                                .add("load")*/
                                .add(scheduleRouteId)
                                .add("load")
                                .add(parcelId);

                        conn.queryWithParams(GET_PACKAGES_TO_DOWNLOAD_BY_PARCEL_ID, paramsGetPackages, replyPackages->{
                            try {
                                if(replyPackages.succeeded()){
                                    if (replyPackages.result().getRows().isEmpty()){
                                        parcel.put("parcels_packages", new JsonArray());
                                        parcels.add(parcel);
                                        future.complete(parcels);
                                    } else {
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
                                    }
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

    private CompletableFuture<JsonObject> getVehicleInformation(SQLConnection conn,JsonObject scheduleRoute, Integer vehicleId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray();
            params.add(vehicleId);
            conn.queryWithParams(QUERY_GET_VEHICLE_INFORMATION, params, reply -> {
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            scheduleRoute.put("vehicle",reply.result().getRows().get(0));
                            future.complete(scheduleRoute);
                        }else {
                            future.completeExceptionally(new Throwable("VEHICLE INFORMATION NOT FOUND"));
                        }
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getDriverInformation(SQLConnection conn,JsonObject scheduleRoute, Integer scheduleRouteId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray();
            params.add(scheduleRouteId);
            conn.queryWithParams(QUERY_GET_DRIVER_INFORMATION, params, reply -> {
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            List<JsonObject> result = reply.result().getRows();
                            int pos = 0;
                            for(int i = 0; i < result.size(); i++){
                                if(!result.get(i).getString("driver_status").equals("3")){
                                    pos = i;
                                    break;
                                }
                            }
                            result.get(pos).remove("driver_status");
                            scheduleRoute.put("driver",result.get(pos));
                            future.complete(scheduleRoute);
                        }else {
                            future.completeExceptionally(new Throwable("DRIVER INFORMATION NOT FOUND"));
                        }
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getDestinations(SQLConnection conn, JsonObject scheduleRoute, Integer scheduleRouteId, Integer employeeId, Integer terminalId, Integer terminalDestinyId, Boolean toLoad, String pDate, String pDate2){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_CITIES_INFORMATION;
            JsonArray params = new JsonArray()
                    .add(scheduleRouteId)
                    .add(pDate)
                    .add(pDate2);
            if(toLoad){
                QUERY = QUERY_GET_CITIES_INFORMATION_TO_LOAD;
            }
            if (employeeId != null){
                QUERY = QUERY.replace("{JOIN_EMPLOYEE}", " inner join employee as e on e.branchoffice_id = srd.terminal_origin_id ");
                QUERY += " and e.id = ? ";
                params.add(employeeId);
            } else {
                QUERY = QUERY.replace("{JOIN_EMPLOYEE}", " ");
            }

            if (terminalId != null){
                QUERY += " AND srd.terminal_origin_id = ? ";
                params.add(terminalId);
            }

            if (terminalDestinyId != null){
                QUERY += " AND srd.terminal_destiny_id = ? ";
                params.add(terminalDestinyId);
            }
            if(toLoad){
                QUERY += " GROUP BY srd.id ";
            }

            conn.queryWithParams(QUERY, params, reply -> {
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            scheduleRoute.put("destinations", reply.result().getRows());
                            future.complete(scheduleRoute);
                        }else {
                            System.out.println("Empty schedule route destinations: ".concat(String.valueOf(scheduleRouteId)));
                            future.complete(scheduleRoute);
                        }
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getHitchedTrailers(JsonObject scheduleRoute, Integer scheduleRouteId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_GET_HITCHED_TRAILERS, new JsonArray().add(scheduleRouteId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> trailers = reply.result().getRows();
                    scheduleRoute.put("hitched_trailers", trailers);
                    future.complete(scheduleRoute);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getTrailersToHitch(SQLConnection conn, JsonObject scheduleRoute, Integer terminalId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(QUERY_GET_TRAILERS_TO_HITCH, new JsonArray().add(terminalId), reply -> {
               try {
                   if(reply.failed()) {
                       throw reply.cause();
                   }
                   List<JsonObject> trailers = reply.result().getRows();
                   scheduleRoute.put("trailers_to_hitch", trailers);
                   future.complete(scheduleRoute);
               } catch (Throwable t) {
                   future.completeExceptionally(t);
               }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getOrigins(SQLConnection conn, JsonObject scheduleRoute, Integer scheduleRouteId, Integer employeeId, Integer terminalId, Integer terminalOriginId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_CITIES_ORIGIN;
            JsonArray params = new JsonArray()
                    .add(scheduleRouteId);

            if (employeeId != null){
                QUERY = QUERY.replace("{JOIN_EMPLOYEE}", " INNER JOIN employee AS e on e.branchoffice_id = srd.terminal_destiny_id ");
                QUERY += " and e.id = ? ";
                params.add(employeeId);
            } else {
                QUERY = QUERY.replace("{JOIN_EMPLOYEE}", " ");
            }

            if (terminalId != null){
                QUERY += " AND srd.terminal_destiny_id = ? ";
                params.add(terminalId);
            }

            if (terminalOriginId != null){
                QUERY += " AND srd.terminal_origin_id = ? ";
                params.add(terminalOriginId);
            }

            QUERY += " GROUP BY srd.id \n" +
                    " ORDER BY srd.travel_date DESC\n" +
                    " LIMIT 1;";

            conn.queryWithParams(QUERY, params, reply -> {
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonArray origins = new JsonArray(reply.result().getRows());
                            scheduleRoute.put("origins", origins);
                            List<CompletableFuture<JsonObject>> taskTotals = new ArrayList<>();

                            origins.forEach(o -> {
                                JsonObject origin = (JsonObject) o;
                                taskTotals.add(this.getTotalsByShipment(origin));
                            });

                            CompletableFuture.allOf(taskTotals.toArray(new CompletableFuture[taskTotals.size()])).whenComplete((resultTasks, errorTasks) -> {
                                try {
                                    if (errorTasks != null){
                                        throw errorTasks;
                                    }

                                    future.complete(scheduleRoute);

                                } catch (Throwable t){
                                    future.completeExceptionally(t);
                                }
                            });


                        }else {
                            System.out.println("Empty schedule route: ".concat(String.valueOf(scheduleRouteId)));
                            future.complete(scheduleRoute);
                        }
                    }else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<JsonObject> getTotalsByShipment(JsonObject origin){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        List<CompletableFuture<JsonObject>> tasksTotals = new ArrayList<>();
        tasksTotals.add(getTotals(origin, QUERY_GET_TOTAL_PARCELS));
        tasksTotals.add(getTotals(origin, QUERY_GET_TOTAL_PACKAGES));
        tasksTotals.add(getTotals(origin, QUERY_GET_TOTAL_TICKETS_AND_COMPLEMENTS));

        CompletableFuture.allOf(tasksTotals.toArray(new CompletableFuture[tasksTotals.size()])).whenComplete((reply, error) -> {
           try {
               if(error != null) {
                   throw error;
               }
               future.complete(origin);
           } catch (Throwable t) {
               t.printStackTrace();
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getTotals(JsonObject origin, String QUERY) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer scheduleRouteId = origin.getInteger(SCHEDULE_ROUTE_ID);
        Integer orderOrigin = origin.getInteger(ORDER_ORIGIN);
        Integer orderDestiny = origin.getInteger(ORDER_DESTINY);
        this.dbClient.queryWithParams(QUERY, new JsonArray().add(scheduleRouteId).add(orderOrigin).add(orderDestiny), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                JsonObject resultTotals = reply.result().getRows().get(0);
                origin.mergeIn(resultTotals);
                future.complete(origin);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getTickets(SQLConnection conn, JsonObject scheduleRoute, Integer scheduleRouteId, Integer terminalId, Integer terminalDestinyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_TICKETS;
            JsonArray params = new JsonArray()
                .add(scheduleRouteId)
                .add(terminalId);

            if (terminalDestinyId != null){
                QUERY += " AND srd.terminal_destiny_id = ? ";
                params.add(terminalDestinyId);
            }

            QUERY += " ORDER BY bpt.id;";

            conn.queryWithParams(QUERY,params,reply->{
                try {
                    if(reply.succeeded()){
                        List<JsonObject> tickets = reply.result().getRows();
                        List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                        for(int i=0; i<tickets.size(); i++){
                            JsonObject ticket = tickets.get(i);
                            Integer parcelId = (Integer) ticket.remove("parcel_id");
                            if(parcelId!=null){
                                task.add(getParcelByPassenger(conn,ticket,parcelId));
                            }
                            task.add(getComplementsByPassenger(conn,ticket,scheduleRouteId, false));
                        }
                        CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,err)->{
                            try {
                                if(err!=null){
                                    future.completeExceptionally(err);
                                }else{
                                    scheduleRoute.put("tickets",tickets);
                                    future.complete(scheduleRoute);
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

    private CompletableFuture<JsonObject> getParcelByPassenger(SQLConnection conn, JsonObject ticket, Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                    .add(parcelId);
            conn.queryWithParams(QUERY_GET_PARCEL_DETAILS_PASSENGER,params,reply->{
                try {
                    if(reply.succeeded()){
                        if(reply.result().getNumRows()>0){
                            JsonObject parcel = reply.result().getRows().get(0);
                            getPackagesByPassenger(conn,parcel,parcelId).whenComplete((res,err)->{
                                try {
                                    if(err != null){
                                        future.completeExceptionally(err);
                                    }else {
                                        ticket.put("parcel",parcel);
                                        future.complete(ticket);
                                    }
                                }catch (Exception e){
                                    future.completeExceptionally(e);
                                }
                            });
                        }else {
                            future.completeExceptionally(new Throwable("PARCEL DETAILS NOT FOUND FOR PASSENGER"));
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

    private CompletableFuture<JsonObject> getPackagesByPassenger(SQLConnection conn, JsonObject ticket, Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray()
                .add(parcelId);
            conn.queryWithParams(QUERY_GET_PACKAGES_BY_PARCEL,params,reply->{
                try {
                    if(reply.succeeded()){
                        ticket.put("packages",reply.result().getRows());
                        future.complete(ticket);
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

    private CompletableFuture<JsonObject> getComplementsByPassenger(SQLConnection conn, JsonObject ticket, Integer scheduleRouteId, Boolean registeredFilter){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
           JsonArray params = new JsonArray()
               .add(scheduleRouteId)
               .add(ticket.getInteger("id"));
           String QUERY = QUERY_GET_COMPLEMENTS_BY_ROUTE + FILTER_BY_PASSENGER;
           if(registeredFilter){
               QUERY += " AND bpc.complement_status != 1 ";
           }
           conn.queryWithParams(QUERY,params, reply->{
               try {
                   if(reply.succeeded()){
                       ticket.put("complements",reply.result().getRows());
                       future.complete(ticket);
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

    private CompletableFuture<JsonObject> getPackagesListPackages(SQLConnection conn, JsonObject parcel, Integer terminalId, Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray params = new JsonArray().add(parcelId).add(terminalId);
            conn.queryWithParams(QUERY_GET_PACKAGES_TO_SHIPMENT_BY_TERMINAL_AND_PARCEL,params,reply->{
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> resultList = reply.result().getRows();
                    parcel.put("packages", resultList);
                    future.complete(parcel);

                }catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getParcelsWithLoadPackages(SQLConnection conn, Integer configRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer page, Integer limit){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_PARCELS_WITH_PACKAGES_TO_SHIPMENT_BY_TERMINAL;
            JsonArray params = new JsonArray()
                    .add(configRouteId);

            if (terminalOriginId != null){
                QUERY += " AND p.terminal_origin_id = ? ";
                params.add(terminalOriginId);
            }

            if (terminalDestinyId != null){
                QUERY += " AND p.terminal_destiny_id = ? ";
                params.add(terminalDestinyId);
            }
            String queryCount = QUERY;
            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }
            int skip = limit * (page-1);
            JsonArray paramsCount = params.copy();
            QUERY = QUERY.concat(" ORDER BY p.created_at DESC LIMIT ?,?  ");
            params.add(skip).add(limit);
            String finalQUERY = QUERY;
            conn.queryWithParams("SELECT COUNT(*) AS items FROM ( \n".concat(queryCount).concat(" ) AS items"), paramsCount, replyCount ->{
                try {
                    if(replyCount.failed()){
                        throw new Exception(replyCount.cause());
                    }

                    Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                    conn.queryWithParams(finalQUERY, params, reply->{
                        try {
                            if(reply.failed()){
                                throw new Exception(reply.cause());
                            }

                            List<JsonObject> result = reply.result().getRows();
                            JsonObject totalResult = new JsonObject()
                                    .put("count", count)
                                    .put("items", result.size())
                                    .put("results", result);
                            future.complete(totalResult);
                        }catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                }catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private String FormatDate(Date date){
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        return format1.format(date);
    }

    private void genericRegister(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try{
                JsonObject body = message.body();
                GenericQuery model = this.generateGenericCreate(body);
                conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
                    if (reply.succeeded()) {
                        Integer shipmentId = reply.result().getKeys().getInteger(0);
                        this.commit(conn, message, new JsonObject().put("id", shipmentId));
                    } else {
                        this.rollback(conn, reply.cause(), message);
                    }
                });
            } catch (Exception e){
                this.rollback(conn, e, message);
            }
        });
    }

    private void cancelShipment(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try{
                JsonObject body = message.body();
                Integer shipmentId = body.getInteger("shipment_id");
                Boolean isLoad = body.getBoolean("isLoad");

                AtomicReference<String> query = new AtomicReference<>(GET_SHIPMENT_BY_ID);
                AtomicReference<JsonArray> params = new AtomicReference<>(new JsonArray().add(shipmentId));
                if(isLoad){
                    params.set(params.get().add("loading").add("load"));
                }else{
                    params.set(params.get().add("downloading").add("download"));
                }
                //get shipment
                conn.queryWithParams(query.get(), params.get(), (AsyncResult<ResultSet> reply) -> {
                    try {
                        if(reply.failed()){
                            throw reply.cause();
                        }

                        if(reply.result().getRows().size() == 0){
                            throw new Exception("Shipment not found or not able to be cancel");
                        }

                        query.set(UPDATE_SCHEDULE_STATUS);

                        String scheduleStatus = isLoad ? "(SELECT IF(tl.terminal_origin_id = cr.terminal_origin_id, 'scheduled', 'ready-to-load')" +
                                " FROM travel_logs tl where tl.load_id = s.id order by id desc limit 1)" : "\"stopped\"";

                        String destinationStatus = "IF(srd.destination_status = \"{STATUS}\", {SCHEDULE_STATUS}, srd.destination_status)"
                                .replace("{STATUS}", isLoad ? "loading" : "downloading")
                                .replace("{SCHEDULE_STATUS}", scheduleStatus);

                        query.set(query.get()
                                .replace("{SCHEDULE_STATUS}", scheduleStatus)
                                .replace("{DESTINATION_STATUS}", destinationStatus));

                        params.set(new JsonArray().add(shipmentId));
                        //update status
                        conn.updateWithParams(query.get(), params.get(), (AsyncResult<UpdateResult> statusReply) -> {
                            try {
                                if (statusReply.failed()){
                                    throw statusReply.cause();
                                }

                                query.set("SELECT tl.id FROM travel_logs tl where tl.{IS_LOAD}load_id = ? order by id desc limit 1"
                                        .replace("{IS_LOAD}", isLoad ? "" : "down"));

                                conn.queryWithParams(query.get(), params.get(), (AsyncResult<ResultSet> logReply) -> {
                                    try {
                                        if(logReply.failed()){
                                            throw logReply.cause();
                                        }

                                        if(logReply.result().getRows().size() == 0){
                                            throw new Exception("not found travel logs");
                                        }

                                        Integer tlId = logReply.result().getRows().get(0).getInteger("id");

                                        List<CompletableFuture<Boolean>> trackingUpdates = new ArrayList<>();

                                        trackingUpdates.add(updateGen(conn, "update boarding_pass_ticket bpt\n" +
                                                "inner join shipments_ticket_tracking stt on stt.boarding_pass_ticket_id = bpt.id\n" +
                                                "SET bpt.ticket_status = 1\n" +
                                                "where stt.shipment_id = ? and stt.status = \"{IS_LOADED}loaded\"\n"
                                                        .replace("{IS_LOADED}", isLoad ? "" : "down"), params.get()));

                                        trackingUpdates.add(updateGen(conn, "update boarding_pass_complement bpc\n" +
                                                "inner join shipments_complement_tracking sct on sct.boarding_pass_complement_id = bpc.id\n" +
                                                "SET bpc.complement_status = 1\n" +
                                                "where sct.shipment_id = ? and sct.status = \"{IS_LOADED}loaded\"\n"
                                                        .replace("{IS_LOADED}", isLoad ? "" : "down"), params.get()));

                                        trackingUpdates.add(updateGen(conn, "update parcels_packages pp\n" +
                                                "inner join shipments_parcel_package_tracking sppt on sppt.parcel_package_id = pp.id\n" +
                                                "SET pp.package_status = 1\n" +
                                                "where sppt.shipment_id = ? and sppt.status = \"{IS_LOADED}loaded\"\n"
                                                        .replace("{IS_LOADED}", isLoad ? "" : "down"), params.get()));

                                        trackingUpdates.add(updateGen(conn, "delete from shipments_complement_tracking where shipment_id = ? and status = \"{IS_LOADED}loaded\"\n".replace("{IS_LOADED}", isLoad ? "" : "down"), params.get()));
                                        trackingUpdates.add(updateGen(conn, "delete from shipments_parcel_package_tracking where shipment_id = ? and status = \"{IS_LOADED}loaded\"\n".replace("{IS_LOADED}", isLoad ? "" : "down"), params.get()));
                                        trackingUpdates.add(updateGen(conn, "delete from shipments_ticket_tracking where shipment_id = ? and status = \"{IS_LOADED}loaded\"".replace("{IS_LOADED}", isLoad ? "" : "down"), params.get()));

                                        if(isLoad){
                                            trackingUpdates.add(updateGen(conn, "delete from travel_logs where id = ?", new JsonArray().add(tlId)));
                                        }else{
                                            trackingUpdates.add(updateGen(conn, "update travel_logs SET download_id = null, status = 'close' where id = ?", new JsonArray().add(tlId)));
                                        }

                                        CompletableFuture.allOf(trackingUpdates.toArray(new CompletableFuture[trackingUpdates.size()])).whenComplete((result, error) -> {
                                            try {
                                                if (error != null) {
                                                    throw error;
                                                }

                                                this.commit(conn, message, new JsonObject().put("id", shipmentId));

                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    }catch (Throwable exe){
                                        this.rollback(conn, exe, message);
                                    }
                                });

                            }catch (Throwable ex){
                                this.rollback(conn, ex, message);
                            }
                        });
                    }catch (Throwable e){
                        this.rollback(conn, e, message);
                    }
                });

            } catch (Throwable e){
                this.rollback(conn, e, message);
            }
        });
    }

    private CompletableFuture<Boolean> updateGen(SQLConnection conn, String updateQuery, JsonArray params){
        CompletableFuture future = new CompletableFuture();
        conn.updateWithParams(updateQuery, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                future.complete(reply.succeeded());

            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getIncidences(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer shipmentId = body.getInteger(ID);

        Future f1 = Future.future();
        Future f2 = Future.future();

        this.dbClient.queryWithParams(GET_COMPLEMENTS_INCIDENCES_BY_SHIPMENT_ID, new JsonArray().add(shipmentId), f1.completer());
        this.dbClient.queryWithParams(GET_PARCELS_INCIDENCES_BY_SHIPMENT_ID, new JsonArray().add(shipmentId), f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try{
                if (reply.succeeded()) {
                    List<JsonObject> complementIncidences = reply.result().<ResultSet>resultAt(0).getRows();
                    List<JsonObject> parcelsIncidences = reply.result().<ResultSet>resultAt(1).getRows();
                    message.reply(new JsonObject()
                            .put("complements", complementIncidences)
                            .put("parcels", parcelsIncidences));
                } else {
                    reportQueryError(message, reply.cause());
                }
            } catch (Exception e){
                reportQueryError(message, e);
            }
        });
    }

    private void getToDoListByType(Message<JsonObject> message) {
        startTransaction(message,conn->{
            try{
                JsonObject body = message.body();

                Integer terminalId = body.getInteger(TERMINAL_ID);
                String date = body.getString(DATE);
                Integer configRouteId = body.getInteger(CONFIG_ROUTE_ID);
                Integer terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
                Integer terminalDestinyId = body.getInteger(TERMINAL_DESTINY_ID);
                boolean includeParcels = body.getBoolean(INCLUDE_PARCELS, false);
                SHIPMENT_TYPES shipmentType = SHIPMENT_TYPES.valueOf(body.getString(TYPE_SHIPMENT).toUpperCase());
                JsonObject result = new JsonObject();
                Integer routesTimeBeforeTravelDate = body.getInteger("routes_time_before_travel_date_pos");
                Integer parcelRoutesTimeBeforeTravelDate = body.getInteger("parcel_routes_time_before_travel_date");

                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                if (shipmentType.equals(SHIPMENT_TYPES.LOAD)){
                    tasks.add(getRoutesToLoad(conn, result, date, null, terminalId, configRouteId, terminalOriginId, terminalDestinyId, includeParcels, true, routesTimeBeforeTravelDate, parcelRoutesTimeBeforeTravelDate));
                } else {
                    tasks.add(getRoutesToDownload(conn, result, date, null, terminalId, configRouteId, terminalOriginId, terminalDestinyId, false, false, routesTimeBeforeTravelDate, parcelRoutesTimeBeforeTravelDate));
                }

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res,err)->{
                    try {
                        if(err != null){
                            throw err;
                        }

                        this.commit(conn, message, result);

                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });


            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn,e,message);
            }
        });
    }
    private void getParcelsToDoList(Message<JsonObject> message) {
        startTransaction(message,conn->{
            try{
                JsonObject body = message.body();

                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer configRouteId = body.getInteger(CONFIG_ROUTE_ID);
                Integer terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
                Integer terminalDestinyId = body.getInteger(TERMINAL_DESTINY_ID);
                Integer page = body.getInteger("page");
                Integer limit = body.getInteger("limit");
                JsonObject result = new JsonObject();
                this.getParcelsToLoad(conn, result,  terminalId, configRouteId, terminalOriginId, terminalDestinyId, page, limit).whenComplete((res,err) ->{
                    try {
                        if(err != null){
                            throw err;
                        }
                        this.commit(conn, message, result);
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn,e,message);
            }
        });
    }

    private void getHistoricLoad(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            String QUERY = QUERY_GET_HISTORICAL_LOAD;
            String initDate = body.getString(INIT_DATE);
            String endDate = body.getString(END_DATE);
            JsonArray params = new JsonArray()
                    .add(initDate).add(endDate);

            if (body.getInteger(TERMINAL_ORIGIN_ID) != null){
                Integer terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
                params.add(terminalOriginId);
                QUERY += " AND tl.terminal_origin_id = ? ";
            }

            if (body.getInteger(TERMINAL_DESTINY_ID) != null){
                Integer terminalOriginId = body.getInteger(TERMINAL_DESTINY_ID);
                params.add(terminalOriginId);
                QUERY += " AND tl.terminal_destiny_id = ? ";
            }

            if (body.getInteger(CONFIG_ROUTE_ID) != null){
                Integer configRouteId = body.getInteger(CONFIG_ROUTE_ID);
                params.add(configRouteId);
                QUERY += " AND cr.id = ? ";
            }

            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();

                    if(!result.isEmpty()) {
                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                        for (JsonObject load : result) {
                            Integer scheduleRouteId = load.getInteger(_SCHEDULE_ROUTE_ID);
                            Integer orderDestiny = load.getInteger(_ORDER_DESTINY);
                            tasks.add(dailyLogsDetail.getParcelsDailyLogsLoad(load, null, scheduleRouteId, null, null, orderDestiny, false, true, false));
                            tasks.add(dailyLogsDetail.getPackagesDailyLogsLoad(load, null, scheduleRouteId, null, null, orderDestiny, true));
                        }

                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resTotals, error) -> {
                            try {
                                if (error != null){
                                    throw error;
                                }

                                message.reply(new JsonArray(result));

                            } catch (Throwable t){
                                reportQueryError(message, t);
                            }
                        });

                    } else {
                        message.reply(new JsonArray(result));
                    }

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

    private void getHistoricDownload(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            String QUERY = QUERY_GET_HISTORICAL_DOWNLOAD;

            String initDate = body.getString(INIT_DATE);
            String endDate = body.getString(END_DATE);
            JsonArray params = new JsonArray()
                    .add(initDate).add(endDate);

            if (body.getInteger(TERMINAL_ORIGIN_ID) != null){
                Integer terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
                params.add(terminalOriginId);
                QUERY += " AND tl.terminal_origin_id = ? ";
            }

            if (body.getInteger(TERMINAL_DESTINY_ID) != null){
                Integer terminalOriginId = body.getInteger(TERMINAL_DESTINY_ID);
                params.add(terminalOriginId);
                QUERY += " AND tl.terminal_destiny_id = ? ";
            }

            if (body.getInteger(CONFIG_ROUTE_ID) != null){
                Integer configRouteId = body.getInteger(CONFIG_ROUTE_ID);
                params.add(configRouteId);
                QUERY += " AND cr.id = ? ";
            }
            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();
                    message.reply(new JsonArray(result));

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

    private void passengersToDownload(Message<JsonObject> message){
        startTransaction(message,conn->{
            try{
                JsonObject body = message.body();

                Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
                Integer terminalId = body.getInteger(TERMINAL_ID);
                JsonObject result = new JsonObject();

                getTicketsToDownload(conn,result,scheduleRouteId, terminalId, false).whenComplete((res,error)->{
                    try {
                        if(error != null){
                            this.rollback(conn,error,message);
                        }else {
                            this.commit(conn,message,result);
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

    private void getDailyLogsByType(Message<JsonObject> message){
        JsonObject body = message.body();

        String travelDate = body.getString(INIT_DATE);
        String shipmentType = body.getString("shipment_type");
        JsonArray params = new JsonArray().add(travelDate);
        StringBuilder QUERY = new StringBuilder(shipmentType.equals(SHIPMENT_TYPES.LOAD.getName()) ? QUERY_GET_DAILY_LOGS_LIST_LOAD : QUERY_GET_DAILY_LOGS_LIST_DOWNLOAD);

        this.completeQuery(QUERY, params, body, TERMINAL_ID, " AND ship.terminal_id = ? ");
        this.completeQuery(QUERY, params, body, "config_destination_id", " AND cd.terminal_origin_id =\n" +
                "(SELECT cdo.terminal_origin_id FROM config_destination cdo\n" +
                " WHERE cdo.id = ?) ");
        this.completeQuery(QUERY, params, body, "config_destination_id", " AND cd.terminal_destiny_id =\n" +
                " (SELECT cdd.terminal_destiny_id FROM config_destination cdd\n" +
                " WHERE cdd.id = ?) ");
        this.completeQuery(QUERY, params, body, VEHICLE_ID, " AND v.id = ? ");
        this.completeQuery(QUERY, params, body, "driver_id", " AND e.id = ? ");

        this.dbClient.queryWithParams(QUERY.toString(), params, replyDailyLogs -> {
            try {
                if (replyDailyLogs.failed()){
                    throw new Exception(replyDailyLogs.cause());
                }
                List<JsonObject> listDailyLogs = replyDailyLogs.result().getRows();
                if (listDailyLogs.isEmpty()){
                    throw new Exception("Elements not found");
                }
                message.reply(new JsonArray(listDailyLogs));
            } catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }
    private void getIncidenceLog(Message<JsonObject> message){
        JsonObject body = message.body();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer driverId = body.getInteger("driver_id");
        JsonArray params = new JsonArray().add(initDate).add(endDate);
        String QUERY = QUERY_GET_INCIDENCES_LOG;
        if(driverId != null){
            QUERY = QUERY.concat(" AND til.driver_id = ?");
            params.add(driverId);
        }
        JsonArray paramsCount = params.copy();
        String queryCounts = " SELECT COUNT(f.travel_incidence_id) AS count FROM ( ".concat(QUERY).concat(" ) AS f ");
        if(body.getInteger("page")!=null){
           Integer page = body.getInteger("page", 1);
           Integer limit = body.getInteger("limit", MAX_LIMIT);
           if (limit > MAX_LIMIT) {
               limit = MAX_LIMIT;
           }
           int skip = limit * (page-1);
           QUERY = QUERY.concat(" LIMIT ?,? ");
           params.add(skip).add(limit);
       }
        String finalQueryFields = QUERY;
        this.dbClient.queryWithParams(queryCounts, paramsCount , replyCount ->{
            try {
                if (replyCount.failed()){
                    throw new Exception(replyCount.cause());
                }
                Integer count = replyCount.result().getRows().get(0).getInteger("count", 0);
                this.dbClient.queryWithParams(finalQueryFields, params, replyFields -> {
                    try {
                        if (replyFields.failed()){
                            throw new Exception(replyFields.cause());
                        }
                        List<JsonObject> result = replyFields.result().getRows();
                        JsonObject totalResult = new JsonObject();
                        if(body.getInteger("page")!=null){
                            totalResult
                                    .put("count", count)
                                    .put("items", result.size())
                                    .put("results", result);
                        }else{
                            totalResult
                                .put("result", result);
                        }
                        message.reply(totalResult);
                    } catch (Exception e){
                        e.printStackTrace();
                        reportQueryError(message, e);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private void getShipmentInfo(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer shipmentId = body.getInteger(SHIPMENT_ID);
        this.dbClient.queryWithParams("SELECT shipment_type FROM shipments where id = ? ;", new JsonArray().add(shipmentId), replyShipment -> {
            try {
                if (replyShipment.failed()){
                    throw replyShipment.cause();
                }
                List<JsonObject> shipmentResult = replyShipment.result().getRows();
                if(shipmentResult.isEmpty()){
                    throw new Exception("Shipment not found");
                }
                boolean isLoad = shipmentResult.get(0).getString("shipment_type").equals("load");

                JsonArray params = new JsonArray().add(shipmentId);
                List<Future> futures = new ArrayList<>();

                Future<ResultSet> f1 = Future.future();
                String QUERY_PARCELS = isLoad ? QUERY_SCANNED_PARCELS : QUERY_SCANNED_DOWNLOAD_PARCELS;
                this.dbClient.queryWithParams(QUERY_PARCELS, params, f1.completer());
                futures.add(f1);

                CompositeFuture.all(futures).setHandler(reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        List<JsonObject> scannedParcels = reply.result().<ResultSet>resultAt(0).getRows();

                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                        scannedParcels.forEach(parcel -> tasks.add(this.getPackagesByParcel(parcel, isLoad, shipmentId)));

                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
                            try {
                                if (error != null){
                                    throw error;
                                }

                                message.reply(new JsonObject()
                                        .put("scanned_parcels", scannedParcels));

                            } catch (Throwable t){
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
        });
    }

    private CompletableFuture<JsonObject> getPackagesByParcel(JsonObject parcel, boolean isLoad, int shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(ID);
        String QUERY;
        JsonArray params = new JsonArray();
        if(isLoad) {
            QUERY = QUERY_GET_PACKAGES_BY_PARCEL_ID;
            params.add(parcelId);
        } else {
            QUERY = QUERY_SCANNED_PACKAGES_BY_SHIPMENT_ID;
            params.add(shipmentId).add(parcelId);
        }
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> packages = reply.result().getRows();
                parcel.put("packages", packages);

                future.complete(parcel);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void getShipmentLoadInfo(Message<JsonObject> message) {
        try{
            JsonObject body = message.body();

            Integer shipmentId = body.getInteger(SHIPMENT_ID);

            this.dbClient.queryWithParams(QUERY_GET_SHIPMENT_LOAD_INFO, new JsonArray().add(shipmentId), result -> {
                try {
                    if (result.failed()){
                        throw result.cause();
                    }

                    List<JsonObject> resultList = result.result().getRows();
                    if (resultList.isEmpty()){
                        throw new Throwable("Load shipment not found");
                    }

                    message.reply(resultList.get(0));

                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        }catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    public void getParcelsToDownloadV2(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
        Integer terminalId = body.getInteger(TERMINAL_ID);
        JsonObject response = new JsonObject();
        try {
            getParcelsToDownload(response, scheduleRouteId, terminalId).whenComplete((resP, errP) -> {
                try {
                    if(errP != null) {
                        throw errP;
                    }
                    getOptionalsParcelsToDownload(response, scheduleRouteId, terminalId).whenComplete((resPO, errPO) -> {
                        try {
                            if(errPO != null) {
                                throw errPO;
                            }
                            getParcelsTranshipmentsToDownload(response, scheduleRouteId, terminalId).whenComplete((resPT, errPT) -> {
                                try {
                                    if(errPT != null) {
                                        throw errPT;
                                    }
                                    message.reply(response);
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    reportQueryError(message, t);
                                }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getDailyLogsCostReport(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String QUERY = DailyLogs.QUERY_GET_DAILY_LOGS_LIST;
            QUERY = QUERY.replace("{DATE_PARAMETER}", "DATE_FORMAT(sr.travel_date, '%Y-%m-%d') >= ? AND DATE_FORMAT(sr.travel_date, '%Y-%m-%d') <= ?");
            JsonArray params = new JsonArray().add(body.getString("init_date")).add(body.getString("end_date"));

            if(body.getInteger(TERMINAL_ORIGIN_ID) != null){
                int terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
                QUERY = QUERY.concat("  AND srd.terminal_origin_id = ? ");
                params.add(terminalOriginId);
            }

            if(body.getInteger("terminal_destiny_id") != null){
                int terminalDestinyId = body.getInteger("terminal_destiny_id");
                QUERY = QUERY.concat(" AND srd.terminal_destiny_id = ? ");
                params.add(terminalDestinyId);
            }

            QUERY = QUERY.concat(" GROUP BY tl.id ");

            this.dbClient.queryWithParams(QUERY, params, replyDailyLogs -> {
                try {
                    if (replyDailyLogs.failed()){
                        throw new Exception(replyDailyLogs.cause());
                    }
                    List<JsonObject> listDailyLogs = replyDailyLogs.result().getRows();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                    listDailyLogs.forEach(dl -> {
                        Integer scheduleRouteId = dl.getInteger(SCHEDULE_ROUTE_ID);
                        Integer orderDestiny = dl.getInteger(ORDER_DESTINY);

                        tasks.add(dailyLogsDetail.getParcelsDailyLogsLoad(dl, null, scheduleRouteId, null, null, orderDestiny, false, true, true));
                        tasks.add(dailyLogsDetail.getPackagesDailyLogsLoad(dl, null, scheduleRouteId, null, null, orderDestiny, true));
                    });

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            message.reply(new JsonArray(listDailyLogs));
                        } catch (Throwable t){
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_GET_SCHEDULE_ROUTE_ID = "SELECT srd.schedule_route_id, sr.schedule_status, sr.vehicle_id, sr.config_route_id, cr.name AS config_route_name, cr.parcel_route\n" +
            "FROM schedule_route_destination AS srd\n" +
            "INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN config_route AS cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN config_destination as cd ON srd.config_destination_id = cd.id\n" +
            "{JOIN_EMPLOYEE}\n" +
            "WHERE {STATUS_FILTER}\n" +
            "AND srd.{DATE_FIELD} between ?  AND ? AND sr.status_hide != 1 ";

    private static final String QUERY_GET_SCHEDULE_ROUTE_ID_DOWNLOAD_V2 = "SELECT srd.schedule_route_id,\n" +
            "sr.schedule_status,\n" +
            "sr.vehicle_id,\n" +
            "sr.config_route_id,\n" +
            "cr.name AS config_route_name,\n" +
            "cr.parcel_route,\n" +
            "IF(srd.terminal_destiny_id = cr.terminal_destiny_id, TRUE, FALSE) AS is_last_step\n" +
            "FROM schedule_route_destination AS srd\n" +
            "INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN config_route AS cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN config_destination as cd ON cd.config_route_id = cr.id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "   AND cd.order_destiny = cd.order_origin + 1\n" +
            "{JOIN_EMPLOYEE}\n" +
            "WHERE srd.destination_status IN ('stopped', 'downloading', 'in-transit')\n" +
            "AND sr.status_hide != 1 \n" +
            "AND cr.parcel_route IS TRUE \n" +
            "AND CONVERT_TZ(srd.arrival_date, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"') between '%s' AND '%s' \n";

    private static final String QUERY_GET_SCHEDULE_ROUTE_ID_LOAD_V2 = "SELECT " +
            "   srd.schedule_route_id,\n" +
            "   sr.schedule_status,\n" +
            "   sr.vehicle_id,\n" +
            "   sr.config_route_id,\n" +
            "   cr.name AS config_route_name,\n" +
            "   cr.parcel_route,\n" +
            "   IF(srd.terminal_origin_id = cr.terminal_origin_id, TRUE, FALSE) AS is_first_step\n" +
            "FROM schedule_route_destination AS srd\n" +
            "INNER JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN config_route AS cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN config_destination as cd ON srd.config_destination_id = cd.id\n" +
            "{JOIN_EMPLOYEE}\n" +
            "WHERE ((sr.schedule_status = 'scheduled' AND cd.order_origin = 1) OR srd.destination_status = 'loading' \n" +
            "   OR srd.destination_status = 'ready-to-go' OR srd.destination_status  = 'ready-to-load')\n" +
            "AND sr.status_hide != 1 \n" +
            "AND ((cr.parcel_route IS FALSE AND srd.travel_date between '%s' AND '%s') OR (cr.parcel_route IS TRUE AND srd.travel_date between '%s' AND '%s')) \n";

    //params schedule_route_id
    private static final String QUERY_GET_DRIVER_INFORMATION = "SELECT " +
            "e.id as 'id', " +
            "e.name as 'first_name', " +
            "e.last_name 'last_name', " +
            "e.cellphone 'cellphone', " +
            "se.id as 'second_id', " +
            "se.name as 'second_first_name', " +
            "se.last_name 'second_last_name', " +
            "se.cellphone 'second_cellphone', " +
            " srd.driver_status " +
            "FROM schedule_route_driver as srd " +
            "INNER join employee e ON e.id = srd.employee_id " +
            "LEFT join employee se ON se.id = srd.second_employee_id " +
            "WHERE srd.schedule_route_id = ? ";

    //params schedule_route_id, branchoffice_id
    private static final String QUERY_GET_CITIES_INFORMATION = "SELECT " +
            " srd.id as 'schedule_route_destination_id', " +
            " tl.id as travel_logs_id,\n" +
            " tl.travel_log_code, " +
            " srd.destination_status, \n" +
            " origin.id as 'origin_terminal_id', \n" +
            " origin.name as 'origin_terminal_name', \n" +
            " origin.prefix as 'origin_terminal_prefix', " +
            " origin.address as 'origin_terminal_address', \n" +
            " ocity.id as 'origin_city_id', \n" +
            " ocity.name as 'origin_city_name', \n" +
            " srd.travel_date as 'origin_travel_date', \n" +
            " srd.arrival_date as arrived_at, \n" +
            " cd.order_origin, \n" +
            " destiny.id as 'destiny_terminal_id', \n" +
            " destiny.name as 'destiny_terminal_name', \n" +
            " destiny.prefix as 'destiny_terminal_prefix', " +
            " destiny.address as 'destiny_terminal_address', \n" +
            " dcity.id as 'destiny_city_id', \n" +
            " dcity.name as 'destiny_city_name', \n" +
            " srd.arrival_date as 'destiny_date', \n" +
            " cd.order_destiny \n" +
            " FROM schedule_route_destination as srd \n" +
            " left join travel_logs tl on tl.schedule_route_id = srd.schedule_route_id \n" +
            "   and tl.terminal_destiny_id = srd.terminal_destiny_id \n" +
            "   and tl.terminal_origin_id = srd.terminal_origin_id \n" +
            " {JOIN_EMPLOYEE} \n" +
            " inner join branchoffice as origin on srd.terminal_origin_id = origin.id \n" +
            " inner join branchoffice as destiny on srd.terminal_destiny_id = destiny.id \n" +
            " inner join city as ocity on ocity.id = origin.city_id \n" +
            " inner join city as dcity on dcity.id = destiny.city_id \n" +
            " inner join config_destination as cd on cd.id = srd.config_destination_id \n" +
            " where srd.schedule_route_id = ? \n" +
            " and srd.destination_status not in ('canceled', 'finished-ok', 'downloading') \n" +
            " and cd.order_origin >= coalesce((select cd.order_origin from shipments s \n" +
            " inner join schedule_route_destination srd2 on srd2.schedule_route_id = s.schedule_route_id and srd2.terminal_origin_id = s.terminal_id \n" +
            " left join config_destination cd on cd.id = srd2.config_destination_id\n" +
            " where s.schedule_route_id = srd.schedule_route_id and shipment_type = 'load' order by order_origin desc limit 1), 0) AND srd.travel_date between ? AND ? \n";

    private static final String QUERY_GET_CITIES_INFORMATION_TO_LOAD = "SELECT  \n" +
            "             srd.id as 'schedule_route_destination_id',  \n" +
            "             tl.id as travel_logs_id, \n" +
            "             tl.travel_log_code,  \n" +
            "             srd.destination_status,  \n" +
            "             origin.id as 'origin_terminal_id',  \n" +
            "             origin.name as 'origin_terminal_name',  \n" +
            "             origin.prefix as 'origin_terminal_prefix',  \n" +
            "             origin.address as 'origin_terminal_address',  \n" +
            "             ocity.id as 'origin_city_id',  \n" +
            "             ocity.name as 'origin_city_name',  \n" +
            "             srd.travel_date as 'origin_travel_date',  \n" +
            "             cd.order_origin,  \n" +
            "             destiny.id as 'destiny_terminal_id',  \n" +
            "             destiny.name as 'destiny_terminal_name',  \n" +
            "             destiny.prefix as 'destiny_terminal_prefix',  \n" +
            "             destiny.address as 'destiny_terminal_address',  \n" +
            "              srdA.finished_at AS arrived_at,\n" +
            "             dcity.id as 'destiny_city_id',  \n" +
            "             dcity.name as 'destiny_city_name',  \n" +
            "             srd.arrival_date as 'destiny_date',  \n" +
            "             cd.order_destiny  \n" +
            "             FROM schedule_route_destination as srd  \n" +
            "             left join travel_logs tl on tl.schedule_route_id = srd.schedule_route_id  \n" +
            "               and tl.terminal_destiny_id = srd.terminal_destiny_id  \n" +
            "               and tl.terminal_origin_id = srd.terminal_origin_id  \n" +
            "             {JOIN_EMPLOYEE}  \n" +
            "             inner join branchoffice as origin on srd.terminal_origin_id = origin.id  \n" +
            "             inner join branchoffice as destiny on srd.terminal_destiny_id = destiny.id  \n" +
            "             inner join city as ocity on ocity.id = origin.city_id  \n" +
            "             inner join city as dcity on dcity.id = destiny.city_id  \n" +
            "             inner join config_destination as cd on cd.id = srd.config_destination_id  \n" +
            "                          LEFT JOIN ( \n" +
            "                        select srr.vehicle_id, srdd.schedule_route_id, srdd.travel_date, srdd.arrival_date, srdd.finished_at, cdd.order_origin, cdd.order_destiny, srdd.terminal_destiny_id \n" +
            "                        from schedule_route_destination AS srdd \n" +
            "                        INNER JOIN config_destination AS cdd ON cdd.id=srdd.config_destination_id \n" +
            "                        INNER JOIN schedule_route AS srr ON srr.id=srdd.schedule_route_id \n" +
            "                        WHERE (cdd.order_destiny - cdd.order_origin)=1) AS srdA \n" +
            "                        ON  srdA.terminal_destiny_id=srd.terminal_origin_id and srdA.arrival_date < srd.travel_date \n" +
            "             where srd.schedule_route_id = ?  \n" +
            "             and srd.destination_status in ('scheduled', 'stopped', 'loading', 'ready-to-go', 'ready-to-load') \n" +
            "             and cd.order_destiny = cd.order_origin + 1  " +
            "             AND srd.travel_date between ? AND ?";

    private static final String QUERY_GET_VEHICLE_INFORMATION = "SELECT \n" +
            "\tv.id, \n" +
            "\tv.economic_number, \n" +
            "    v.name, \n" +
            "    v.description, \n" +
            "    v.alias, \n" +
            "    v.model, \n" +
            "    v.work_type, \n" +
            "    IF(v.can_use_trailer AND c.remolque NOT IN ('0'), TRUE, FALSE) AS can_use_trailer,\n" +
            "    c.remolque\n" +
            "FROM vehicle v \n" +
            "LEFT JOIN c_ConfigAutotransporte c ON c.id = v.c_ConfigAutotransporte_id AND c.status = 1\n" +
            "WHERE v.id = ?";

    private static final String QUERY_GET_CITIES_ORIGIN = "SELECT " +
            " srd.id as 'schedule_route_destination_id',\n" +
            "    srd.schedule_route_id, srd.destination_status,\n" +
            "    tl.id as travel_logs_id,\n" +
            "    tl.travel_log_code,\n" +
            "    tl.load_id,\n" +
            "    origin.id as 'origin_terminal_id',\n" +
            "    origin.name as 'origin_terminal_name',\n" +
            "    origin.prefix as 'origin_terminal_prefix',\n" +
            "    origin.address as 'origin_terminal_address',\n" +
            "    ocity.id as 'origin_city_id', \n" +
            "    ocity.name as 'origin_city_name', \n" +
            "    srd.travel_date as 'origin_travel_date',\n" +
            "    (SELECT\n" +
            "       srd2.travel_date\n" +
            "   FROM schedule_route_destination srd2 \n" +
            "       LEFT JOIN config_destination cd2 ON cd2.id = srd2.config_destination_id\n" +
            "   WHERE srd2.schedule_route_id = srd.schedule_route_id \n" +
            "       AND srd2.terminal_origin_id = srd.terminal_destiny_id\n" +
            "       AND cd2.order_origin = cd.order_destiny\n" +
            "       AND cd2.order_destiny = cd2.order_origin + 1) 'next_travel_date',\n" +
            "   cd.order_origin,\n" +
            "    destiny.id as 'destiny_terminal_id',\n" +
            "    destiny.name as 'destiny_terminal_name',\n" +
            "    destiny.prefix as 'destiny_terminal_prefix',\n" +
            "    destiny.address as 'destiny_terminal_address',\n" +
            "    dcity.id as 'destiny_city_id',\n" +
            "    dcity.name as 'destiny_city_name',\n" +
            "    srd.arrival_date as 'destiny_date',\n" +
            "    srd.finished_at as arrived_at,\n" +
            "    ship.total_tickets,\n" +
            "    ship.total_complements,\n" +
            "    ship.total_parcels,\n" +
            "    ship.total_packages,\n" +
            "    cd.order_destiny,\n" +
            "    (SELECT \n" +
            "       MAX(cdd.order_destiny)\n" +
            "   FROM config_destination cdd \n" +
            "    WHERE cdd.config_route_id = cd.config_route_id) AS last_order_destiny" +
            " FROM schedule_route_destination as srd " +
            " LEFT JOIN travel_logs tl ON tl.terminal_origin_id = srd.terminal_origin_id \n" +
            "   AND tl.terminal_destiny_id = srd.terminal_destiny_id \n" +
            "   AND tl.schedule_route_id = srd.schedule_route_id \n" +
            " {JOIN_EMPLOYEE} \n" +
            " LEFT JOIN shipments ship ON ship.id = tl.load_id \n" +
            " INNER JOIN branchoffice as origin on srd.terminal_origin_id = origin.id \n" +
            " INNER JOIN branchoffice as destiny on srd.terminal_destiny_id = destiny.id \n" +
            " INNER JOIN city as ocity on ocity.id = origin.city_id \n" +
            " INNER JOIN city as dcity on dcity.id = destiny.city_id \n" +
            " INNER JOIN config_destination AS cd ON cd.terminal_origin_id = srd.terminal_origin_id\n" +
            "   AND cd.terminal_destiny_id = srd.terminal_destiny_id \n" +
            " WHERE srd.schedule_route_id = ?  \n" +
            " AND srd.destination_status NOT IN ('scheduled', 'canceled', 'finished-ok') \n" +
            " AND cd.order_destiny = cd.order_origin + 1 \n";

    private static final String QUERY_GET_COMPLEMENT_INFO = "SELECT " +
            " bpc.id as boarding_pass_complement_id, " +
            " bpc.boarding_pass_ticket_id, " +
            " bpr.id AS boarding_pass_route_id, " +
            " bpc.boarding_pass_id, " +
            " srd.schedule_route_id, " +
            " bpc.complement_status," +
            " (bp.ticket_type = 'redondo' OR bp.ticket_type = 'abierto_redondo') AS is_round " +
            "FROM boarding_pass_complement as bpc " +
            "inner join boarding_pass_ticket as bpt on bpt.id = bpc.boarding_pass_ticket_id " +
            "inner join boarding_pass_route as bpr on bpr.id = bpt.boarding_pass_route_id " +
            "inner join boarding_pass as bp on bp.id = bpr.boarding_pass_id " +
            "inner join schedule_route_destination as srd on srd.id = bpr.schedule_route_destination_id " +
            "where bpc.tracking_code = ? AND srd.{TERMINAL_ID} = ? ;";

    private static final String QUERY_GET_TICKET_INFO = "SELECT \n" +
            " bpt.id as boarding_pass_ticket_id,\n" +
            " bp.id as boarding_pass_id,\n" +
            " bpr.id as boarding_pass_route_id,\n" +
            " srd.schedule_route_id, \n" +
            " bpr.ticket_type_route, \n" +
            " bpt.ticket_status, \n" +
            " IF(bp.ticket_type = 'redondo' OR bp.ticket_type = 'abierto_redondo', true, false) AS is_round \n" +
            "FROM boarding_pass_ticket as bpt \n" +
            "inner join boarding_pass_route as bpr on bpr.id = bpt.boarding_pass_route_id \n" +
            "inner join boarding_pass as bp on bp.id = bpr.boarding_pass_id\n" +
            "inner join schedule_route_destination as srd on srd.id = bpr.schedule_route_destination_id \n" +
            "where bpt.tracking_code = ? AND srd.{TERMINAL_ID} = ? ;";

    private static final String QUERY_GET_PACKAGE_INFO = "SELECT pp.id as parcel_package_id, pp.parcel_id, pp.package_status, p.terminal_destiny_id, p.terminal_origin_id " +
            "FROM parcels_packages AS pp " +
            "INNER JOIN parcels AS p ON p.id = pp.parcel_id " +
            "where pp.package_code = ? AND p.{TERMINAL_ID} = ? ;";

    private static final String QUERY_GET_SCHEDULE_ROUTE_DESTINATION = "SELECT srd.id schedule_route_destination_id FROM shipments s\n" +
            "INNER JOIN schedule_route sr ON sr.id = s.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "WHERE s.id = ?\n" +
            "AND srd.terminal_origin_id = ?\n" +
            "AND srd.terminal_destiny_id = ?;";

    private static final String QUERY_GET_SHIPMENT_INFO = "SELECT * FROM shipments where id = ? ;";

    public static final String UPDATE_COMPLEMENT_STATUS = "UPDATE boarding_pass_complement " +
            "SET " +
            "complement_status = ? , " +
            "updated_at = CURRENT_TIMESTAMP, " +
            "updated_by = ? " +
            "WHERE id = ? ";

    public static final String UPDATE_TICKET_STATUS = "UPDATE boarding_pass_ticket " +
            "SET " +
            "ticket_status = ? , " +
            "updated_at = CURRENT_TIMESTAMP, " +
            "updated_by = ? " +
            "WHERE id = ? ";

    private static final String UPDATE_PACKAGE_STATUS = "UPDATE parcels_packages " +
            "SET " +
            "package_status = ? , " +
            "updated_at = CURRENT_TIMESTAMP, " +
            "updated_by = ? " +
            "WHERE id = ? ";

    private static final String QUERY_GET_TICKETS = "SELECT " +
            "            bpt.id, " +
            "            bpp.first_name, " +
            "            bpp.last_name, " +
            "            bpt.tracking_code, " +
            "            bpt.ticket_status, " +
            "            bpp.gender, " +
            "            bpt.parcel_id, " +
            "            st.name as special_ticket, " +
            "            srd.terminal_destiny_id, " +
            "            srd.schedule_route_id, " +
            "            srd.id as schedule_route_destination_id, " +
            "            bpt.checkedin_at, " +
            "            CONCAT(bo.prefix, ' - ', bd.prefix) as segment " +
            "            FROM boarding_pass_ticket as bpt " +
            "            INNER join boarding_pass_route as bpr on bpr.id = bpt.boarding_pass_route_id " +
            "            INNER join schedule_route_destination as srd on bpr.schedule_route_destination_id = srd.id " +
            "            INNER join branchoffice as bo on bo.id = srd.terminal_origin_id " +
            "            INNER join branchoffice as bd on bd.id = srd.terminal_destiny_id " +
            "            INNER JOIN boarding_pass_passenger AS bpp ON bpp.id = bpt.boarding_pass_passenger_id " +
            "            INNER JOIN special_ticket AS st ON st.id = bpp.special_ticket_id " +
            "            INNER JOIN boarding_pass AS bp ON bp.id = bpp.boarding_pass_id \n" +
            "            WHERE bpt.checkedin_at IS NOT NULL AND bp.boardingpass_status != 0 AND srd.schedule_route_id = ? AND srd.terminal_origin_id = ? ";

    private static final String QUERY_GET_COMPLEMENTS_BY_ROUTE = "SELECT \n" +
            "bpc.id, \n" +
            "bpc.tracking_code, \n" +
            "bpc.complement_status, \n" +
            "bpc.shipping_type, \n" +
            "srd.terminal_destiny_id, \n" +
            "srd.schedule_route_id, \n" +
            "srd.id as schedule_route_destination_id\n" +
            "FROM boarding_pass_complement AS bpc \n" +
            "INNER JOIN boarding_pass_ticket AS bpt ON bpt.id = bpc.boarding_pass_ticket_id \n" +
            "INNER JOIN boarding_pass_route AS bpr ON bpr.id = bpt.boarding_pass_route_id \n" +
            "INNER JOIN schedule_route_destination AS srd ON srd.id = bpr.schedule_route_destination_id \n" +
            "WHERE srd.schedule_route_id = ?  AND bpc.shipping_type!='pets' AND bpc.shipping_type != 'frozen'";

    private static final String FILTER_BY_PASSENGER = "AND bpc.boarding_pass_ticket_id = ? ";

    //if this query change, analize QUERY_GET_PACKAGE_DETAILS_BY_ID
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

    //if this query change, analize QUERY_GET_PARCEL_INFO
    private static final String QUERY_GET_PARCEL_DETAILS_PASSENGER = "SELECT \n" +
            " p.id, \n" +
            " p.total_packages, \n" +
            "TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date) AS delivery_time, " +
            " p.terminal_origin_id, \n" +
            " p.terminal_destiny_id, \n" +
            " CONCAT(bo.prefix, ' - ', bd.prefix) AS segment,\n" +
            " p.parcel_tracking_code, \n" +
            " p.parcel_status, \n" +
            " p.status, \n" +
            " p.created_at\n" +
            "FROM parcels p\n" +
            "LEFT JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "LEFT JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "WHERE p.id = ? ";

    private static final String QUERY_GET_PARCELS_WITH_PACKAGES_TO_SHIPMENT_BY_TERMINAL = "SELECT DISTINCT\n" +
            "   ppt.parcel_id,\n" +
            "   p.parcel_status,\n" +
            "   p.terminal_origin_id,\n" +
            "   p.terminal_destiny_id,\n" +
            "   p.total_packages, \n" +
            "   TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date) AS delivery_time,\n" +
            "   CONCAT(bo.prefix, ' - ', bd.prefix) AS segment,\n" +
            "   p.parcel_tracking_code, \n" +
            "   CONCAT(c.first_name, ' ', c.last_name) customer_full_name, \n" +
            "   p.waybill, \n" +
            "   p.status, \n" +
            "   p.created_at\n" +
            " FROM parcels_packages_tracking ppt\n" +
            " INNER JOIN parcels p ON p.id = ppt.parcel_id\n" +
            " INNER JOIN customer c ON c.id = p.customer_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            " INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            " INNER JOIN config_route AS cr2 ON cr2.id = ? \n" +
            " INNER JOIN config_destination AS cd2 ON cd2.config_route_id = cr2.id \n" +
            " AND cd2.terminal_origin_id = p.terminal_origin_id AND cd2.terminal_destiny_id = p.terminal_destiny_id\n" +
            " WHERE (p.parcel_status IN (0, 5, 8, 10) AND pp.package_status IN (0, 8)) AND DATE(p.created_at) > '2022-11-13'\n";
            // " WHERE p.parcel_status IN (0, 5, 8, 10) AND DATE(p.created_at) > '2022-11-13' \n";

    private static final String QUERY_GET_PARCEL_INFO = QUERY_GET_PARCEL_DETAILS_PASSENGER ;

    private static final String QUERY_GET_PACKAGES_TO_SHIPMENT_BY_TERMINAL_AND_PARCEL = "SELECT \n" +
            "\tpp.id, \n" +
            "\tpp.shipping_type, \n" +
            "\tpp.package_status, \n" +
            "\tpp.package_code, \n" +
            "\tpp.total_amount, \n" +
            "\tpp.weight, \n" +
            "\tpp.height, \n" +
            "\tpp.width, \n" +
            "\tpp.length, \n" +
            "\tpp.notes, \n" +
            "\tpp.status, \n" +
            "\tpt.name AS package_type \n" +
            /*"\tpt.name AS package_type, \n" +
            "\tt.name AS trailer_name,\n" +
            "\ttr.Remolque_o_semirremolque AS trailer_type \n" +*/
            "FROM parcels_packages_tracking ppt\n" +
            "LEFT JOIN parcels_packages pp ON pp.id = ppt.parcel_package_id \n" +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id \n" +
            /*"LEFT JOIN shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = pp.parcel_id AND sppt.parcel_package_id = pp.id \n" +
            "   AND sppt.status IN ('loaded', 'transfer') AND sppt.latest_movement IS TRUE\n" +
            "LEFT JOIN shipments_trailers AS st ON st.shipment_id = sppt.shipment_id \n" +
            "LEFT JOIN trailers t ON t.id = st.trailer_id\n" +
            "LEFT JOIN c_SubTipoRem tr ON tr.id = t.c_SubTipoRem_id\n" +*/
            "WHERE pp.parcel_id = ?\n" +
            "AND (pp.package_status = 0 \n" +
            "   OR (pp.package_status = 8 AND ? = (SELECT ppt2.terminal_id FROM parcels_packages_tracking ppt2 \n" +
            "       WHERE ppt2.parcel_package_id = pp.id AND ppt2.parcel_id = pp.parcel_id \n" +
            "       AND ppt2.terminal_id IS NOT NULL ORDER BY ppt2.id DESC LIMIT 1))) \n" +
            "GROUP BY pp.id";


    private static final String QUERY_GET_PACKAGE_DETAILS_BY_ID = QUERY_GET_PACKAGES_BY_PARCEL + " AND pp.id = ? ";

    private static final String QUERY_GET_PARCEL_DETAILS_BY_SCHEDULE_ROUTE_ID = "SELECT distinct\n" +
            "p.id, \n" +
            "p.total_packages, \n" +
            "CONCAT(bo.prefix, ' - ', bd.prefix) AS segment,\n" +
            "p.delivery_time, \n" +
            "p.terminal_origin_id, \n" +
            "p.terminal_destiny_id, \n" +
            "p.parcel_tracking_code, \n" +
            "p.waybill, \n" +
            "p.parcel_status, \n" +
            "0 AS optional,\n" +
            "p.status, \n" +
            "p.created_at\n" +
            "FROM parcels p \n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments AS s ON s.id = sppt.shipment_id  \n" +
            "WHERE p.terminal_destiny_id = ?\n" +
            "AND p.parcel_status IN (1, 10)  \n" +
            "AND pp.package_status NOT IN (2, 3, 4, 6, 7, 9)  \n" +
            "AND s.shipment_type = 'load'\n" +
            "AND s.schedule_route_id = ?;";

    private static final String QUERY_GET_PACKAGE_DETAILS_BY_SCHEDULE_ROUTE_ID_V2 = "SELECT DISTINCT\n" +
            "pp.id parcel_package_id,\n" +
            "pp.shipping_type, \n" +
            "pp.package_status, \n" +
            "pp.package_code, \n" +
            "pp.total_amount,\n" +
            "pp.weight,\n" +
            "pp.height, \n" +
            "pp.width, \n" +
            "pp.length, \n" +
            "pp.notes,\n" +
            "pt.name AS package_type,\n" +
            "sppt.trailer_id\n" +
            "FROM parcels_packages pp\n" +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.id =\n" +
            "    (SELECT sppt2.id FROM shipments_parcel_package_tracking sppt2 \n" +
            "       WHERE sppt2.parcel_package_id = pp.id\n" +
            "           AND sppt2.status = 'loaded'\n" +
            "        ORDER BY sppt2.id DESC LIMIT 1)\n" +
            "WHERE pp.package_status NOT IN (2, 3, 4, 6, 7, 9)  \n" +
            "AND pp.parcel_id = ?;";

    private static final String QUERY_GET_OPTIONAL_PARCEL_DETAILS_BY_SCHEDULE_ROUTE_ID = "SELECT distinct\n" +
            "p.id, \n" +
            "p.parcel_tracking_code, \n" +
            "p.waybill, \n" +
            "p.total_packages, \n" +
            "p.delivery_time, \n" +
            "p.terminal_origin_id, \n" +
            "p.terminal_destiny_id, \n" +
            "p.parcel_status, \n" +
            "p.status, \n" +
            "p.created_at,\n" +
            "1 AS optional\n" +
            "FROM parcels p \n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments AS s ON s.id = sppt.shipment_id  \n" +
            "WHERE ? IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc\n" +
            "   WHERE bprc.of_branchoffice_id = p.terminal_destiny_id)\n" +
            "AND p.parcel_status IN (1, 10)\n" +
            "AND pp.package_status NOT IN (2, 3, 4, 6, 7, 9)\n" +
            "AND s.shipment_type = 'load'\n" +
            "AND s.schedule_route_id = ?;";

    private static final String QUERY_GET_PACKAGE_TRANSHIPMENTS_DETAILS_BY_SCHEDULE_ROUTE_ID = "SELECT distinct\n" +
            "p.id,\n" +
            "p.parcel_tracking_code, \n" +
            "p.waybill, \n" +
            "p.total_packages, \n" +
            "p.delivery_time, \n" +
            "p.terminal_origin_id, \n" +
            "p.terminal_destiny_id, \n" +
            "p.parcel_status, \n" +
            "p.status, \n" +
            "p.created_at,\n" +
            "0 AS optional,\n" +
            "CONCAT(bo.prefix, ' - ', bd.prefix) AS segment\n" +
            "FROM parcels p \n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_transhipments ptran ON ptran.parcel_id = p.id AND ptran.parcel_package_id = pp.id\n" +
            "LEFT JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments AS s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN branchoffice b ON b.id = ?\n" +
            "WHERE b.receive_transhipments IS TRUE\n" +
            "AND p.parcel_status IN (1, 10)\n" +
            "AND pp.package_status NOT IN (2, 3, 4, 6, 7, 9)\n" +
            "AND s.shipment_type = 'load'\n" +
            "AND s.schedule_route_id = ?;";

    private static final String QUERY_GET_TRACKING_PACKAGE = "SELECT * FROM parcels_packages_tracking " +
            "WHERE parcel_package_id = ? AND (action!='incidence' AND action!='printed') ORDER BY ID DESC LIMIT 1;";

    private static final String QUERY_GET_SHIPMENT_INFO_BY_TICKET = "SELECT s.* " +
            "FROM shipments_ticket_tracking AS st " +
            "INNER JOIN shipments AS s ON s.id = st.shipment_id " +
            "WHERE boarding_pass_ticket_id = ? order by s.id desc ;";

    private static final String QUERY_GET_SHIPMENT_INFO_BY_COMPLEMENT = "SELECT s.* " +
            " FROM shipments_complement_tracking AS st " +
            " INNER JOIN shipments AS s ON s.id = st.shipment_id " +
            " WHERE boarding_pass_complement_id = ? order by s.id desc";

    private static final String QUERY_GET_SHIPMENT_INFO_BY_PACKAGE = "SELECT s.* " +
            "             FROM shipments_parcel_package_tracking AS st " +
            "             INNER JOIN shipments AS s ON s.id = st.shipment_id " +
            "             WHERE st.parcel_package_id = ? order by s.id desc";


    private static final String GET_PARCELS_TO_DOWNLOAD = "SELECT DISTINCT p.id FROM parcels p \n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "            LEFT JOIN shipments AS s ON s.id = sppt.shipment_id  \n" +
            "            WHERE (p.terminal_destiny_id = ? OR (? IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "               WHERE bprc.of_branchoffice_id = p.terminal_destiny_id))) \n" +
            "             AND p.parcel_status = 1  \n" +
            "             AND s.shipment_type = 'load'\n"+
            "             AND s.schedule_route_id = ? ";

    private static final String GET_PARCELS_TO_DOWNLOAD_TOTALS = "SELECT \n" +
            " COUNT(DISTINCT p.id) total_parcels, \n" +
            " COUNT(DISTINCT pp.id) total_packages\n" +
            "FROM parcels p \n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments AS s ON s.id = sppt.shipment_id  \n" +
            "WHERE p.terminal_destiny_id = ?\n" +
            "AND p.parcel_status IN (1, 10)  \n" +
            "AND pp.package_status NOT IN (2, 3, 4, 6, 7, 9)  \n" +
            /*"AND p.parcel_status IN (1, 7, 8, 9, 10)  \n" +
            "AND pp.package_status IN (1, 8, 9)  \n" +*/
            "AND s.shipment_type = 'load'\n" +
            "AND s.schedule_route_id = ? ";

    private static final String GET_OPTIONAL_PARCELS_TO_DOWNLOAD_TOTALS = "SELECT \n" +
            " COUNT(DISTINCT p.id) total_parcels, \n" +
            " COUNT(DISTINCT pp.id) total_packages\n" +
            "FROM parcels p \n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments AS s ON s.id = sppt.shipment_id  \n" +
            "WHERE ? IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc\n" +
            "   WHERE bprc.of_branchoffice_id = p.terminal_destiny_id)\n" +
            "AND p.parcel_status IN (1)  \n" +
            "AND pp.package_status IN (1)  \n" +
            /*"AND p.parcel_status IN (1, 7, 8, 9, 10)  \n" +
            "AND pp.package_status IN (1, 8, 9)  \n" +*/
            "AND s.shipment_type = 'load'\n" +
            "AND s.schedule_route_id = ? ";

    private static final String GET_PARCELS_TRANSHIPMENTS_TO_DOWNLOAD_TOTALS = "SELECT \n" +
            " COUNT(DISTINCT p.id) total_parcels, \n" +
            " COUNT(DISTINCT pp.id) total_packages\n" +
            "FROM parcels p \n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_transhipments ptran ON ptran.parcel_id = p.id AND ptran.parcel_package_id = pp.id\n" +
            "LEFT JOIN  shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments AS s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN branchoffice b ON b.id = ?\n" +
            "WHERE b.receive_transhipments IS TRUE\n" +
            "AND p.parcel_status IN (1)  \n" +
            "AND pp.package_status IN (1)  \n" +
            /*"AND p.parcel_status IN (1, 7, 8, 9, 10)  \n" +
            "AND pp.package_status IN (1, 8, 9)  \n" +*/
            "AND s.shipment_type = 'load'\n" +
            "AND s.schedule_route_id = ?;";

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
            "   srd.id as schedule_route_destination_id, " +
            "   bpt.checkedin_at, " +
            "   CONCAT(bo.prefix, ' - ', bd.prefix) as segment " +
            "   FROM boarding_pass_ticket as bpt " +
            "   INNER join boarding_pass_route as bpr on bpr.id = bpt.boarding_pass_route_id " +
            "   INNER join schedule_route_destination as srd on bpr.schedule_route_destination_id = srd.id " +
            "   INNER join branchoffice bo on bo.id = srd.terminal_origin_id " +
            "   INNER join branchoffice bd on bd.id = srd.terminal_destiny_id " +
            "   INNER JOIN boarding_pass_passenger AS bpp ON bpp.id = bpt.boarding_pass_passenger_id  " +
            "   INNER JOIN special_ticket AS st ON st.id = bpp.special_ticket_id " +
            "   " +
            "   WHERE bpt.id = ? " +
            "   ORDER BY bpt.id";

    private static final String GET_TICKETS_TO_DOWNLOAD = "SELECT sct.* FROM shipments_ticket_tracking AS sct " +
            "            INNER JOIN shipments AS s ON s.id = sct.shipment_id " +
            "            INNER JOIN boarding_pass_ticket AS bpt ON bpt.id = sct.boarding_pass_ticket_id " +
            "            INNER JOIN boarding_pass_route AS  bpr ON bpr.id = bpt.boarding_pass_route_id " +
            "            INNER JOIN schedule_route_destination AS srd ON srd.id = bpr.schedule_route_destination_id " +
            "            WHERE  " +
            "             srd.terminal_destiny_id = ? " +
            "            AND s.schedule_route_id = ? " +
            "            AND s.shipment_type = ? "+
            "            AND sct.status = 'loaded'";

    private static final String GET_PACKAGES_TO_DOWNLOAD_BY_PARCEL_ID = "SELECT sct.* " +
            "           FROM shipments_parcel_package_tracking AS sct " +
            "           INNER JOIN shipments AS s ON s.id = sct.shipment_id " +
            "           WHERE  " +
            /*"           sct.parcel_package_id NOT IN ( " +
            "              SELECT sct2.parcel_package_id FROM shipments_parcel_package_tracking AS sct2 " +
            "               INNER JOIN shipments AS s2 ON s2.id = sct2.shipment_id " +
            "               WHERE s2.schedule_route_id = ? " +
            "               AND s2.shipment_type != ? " +
            "               GROUP BY sct2.parcel_package_id " +
            "           ) " +
            "           AND s.schedule_route_id = ? " +*/
            "           s.schedule_route_id = ? " +
            "           AND s.shipment_type = ? " +
            "           AND sct.parcel_id = ? " +
            "           GROUP BY sct.parcel_id, sct.parcel_package_id, sct.status, sct.shipment_id;";

    private static final String GET_SHIPMENT_OPEN = "SELECT ship.id FROM shipments ship \n" +
            "LEFT JOIN schedule_route sr ON sr.id = ship.schedule_route_id\n" +
            "WHERE ship.schedule_route_id = ?\n" +
            "AND ship.terminal_id = ?\n" +
            "AND ship.shipment_type = ?\n" +
            "AND ship.shipment_status = 1\n" +
            "AND sr.schedule_status = IF(ship.shipment_type = 'load', 'loading', 'downloading');";

    private static final String GET_COMPLEMENTS_INCIDENCES_BY_SHIPMENT_ID = "SELECT\n" +
            "  ci.boarding_pass_complement_id,\n" +
            "  ci.incidence_id,\n" +
            "  i.name AS incidence_name,\n" +
            "  i.description AS incidence_description,\n" +
            "  shi.shipment_type,\n" +
            "  ci.notes\n" +
            " FROM complement_incidences ci\n" +
            " LEFT JOIN boarding_pass_complement bpc ON bpc.id = ci.boarding_pass_complement_id\n" +
            " LEFT JOIN incidences i ON i.id = ci.incidence_id\n" +
            " LEFT JOIN shipments shi ON shi.id = ci.shipment_id\n" +
            " WHERE shi.id = ?;";

    private static final String GET_PARCELS_INCIDENCES_BY_SHIPMENT_ID = "SELECT\n" +
            "  pi.parcel_id,\n" +
            "  pi.parcel_package_id,\n" +
            "  pi.incidence_id,\n" +
            "  i.name AS incidence_name,\n" +
            "  i.description AS incidence_description,\n" +
            "  shi.shipment_type,\n" +
            "  pi.notes\n" +
            " FROM parcels_incidences pi\n" +
            " LEFT JOIN incidences i ON i.id = pi.incidence_id\n" +
            " LEFT JOIN shipments shi ON shi.id = pi.shipment_id\n" +
            " WHERE shi.id = ?;";

    private static final String QUERY_GET_HISTORICAL_LOAD = "SELECT\n" +
            "   sr.id AS schedule_route_id,\n" +
            "   cd.order_destiny,\n" +
            "   tl.load_id,\n" +
            "   DATE(ship.created_at) AS date,\n" +
            "   tl.id AS travel_logs_id,\n" +
            "   tl.travel_log_code AS travel_log,\n" +
            "   cr.name AS route,\n" +
            "   ship.created_at AS loaded_at,\n" +
            "   CONCAT(bo.prefix, '-', bd.prefix) AS segment,\n" +
            "   v.economic_number AS vehicle,\n" +
            "   srd.started_at AS started_at,\n" +
            "   IF(ship.updated_at IS NULL, NULL, CAST(TIMEDIFF(ship.updated_at, ship.created_at) AS char)) AS load_time,\n" +
            "   IF(ship.updated_at IS NULL, 'open', 'closed') AS status,\n" +
            "   CONCAT(e.name, ' ', e.last_name) AS created_by\n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN shipments ship ON ship.id = tl.load_id\n" +
            " LEFT JOIN schedule_route sr ON sr.id = ship.schedule_route_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = tl.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = tl.terminal_destiny_id\n" +
            " LEFT JOIN employee e ON e.id = ship.created_by\n" +
            " WHERE ship.created_at BETWEEN ? AND ? ";

    private static final String QUERY_GET_HISTORICAL_DOWNLOAD = "SELECT\n" +
            "   DATE(ship.created_at) AS date,\n" +
            "   tl.id AS travel_logs_id,\n" +
            "   tl.travel_log_code AS travel_log,\n" +
            "   cr.name AS route,\n" +
            "   tl.download_id,\n" +
            "   ship.created_at AS loaded_at,\n" +
            "   CONCAT(bo.prefix, '-', bd.prefix) AS segment,\n" +
            "   v.economic_number AS vehicle,\n" +
            "   TIME(sr.started_at) AS started_at,\n" +
            "   IF(ship.updated_at IS NULL, NULL, TIMEDIFF(ship.updated_at, ship.created_at)) AS load_time,\n" +
            "   IF(ship.shipment_status = 1, 'open', 'closed') AS status,\n" +
            "   IF(ship.updated_at IS NULL, NULL, ship.total_packages) AS packages,\n" +
            "   IF(ship.updated_at IS NULL, NULL, (SELECT COUNT(DISTINCT pp.id) FROM parcels pp\n" +
            "   LEFT JOIN shipments_parcel_package_tracking shippt ON shippt.parcel_id = pp.id\n" +
            "   LEFT JOIN shipments pship ON pship.id = shippt.shipment_id\n" +
            "   WHERE pship.id = tl.download_id)) AS waybills,\n" +
            "   CONCAT(e.name, ' ', e.last_name) AS created_by,\n" +
            "   (SELECT asrd.finished_at FROM schedule_route_destination asrd\n" +
            "\tLEFT JOIN config_destination acd ON acd.id = asrd.config_destination_id\n" +
            "    WHERE asrd.schedule_route_id = sr.id AND \n" +
            "    acd.order_destiny = cd.order_origin AND acd.order_origin = cd.order_origin -1) AS arrived_at,\n" +
            "    (SELECT asrd.travel_date FROM schedule_route_destination asrd\n" +
            "    LEFT JOIN config_destination acd ON acd.id = asrd.config_destination_id\n" +
            "    WHERE asrd.schedule_route_id = sr.id AND \n" +
            "    acd.order_origin = cd.order_destiny AND acd.order_destiny = cd.order_destiny + 1) AS origin_travel_date\n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN shipments ship ON ship.id = tl.download_id\n" +
            " LEFT JOIN schedule_route sr ON sr.id = ship.schedule_route_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = tl.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = tl.terminal_destiny_id\n" +
            " LEFT JOIN employee e ON e.id = ship.created_by\n" +
            " WHERE ship.created_at BETWEEN ? AND ? ";

    private static final String QUERY_GET_INCIDENCES_LOG = "select til.id, til.travel_incidence_id, tl.name AS travel_incidence_name, til.schedule_route_id, til.terminal_origin_id , bo.name AS terminal_origin_name, \n" +
            "            til.terminal_destiny_id, bd.name AS terminal_destiny_name, til.notes, til.driver_id, CONCAT(e.name, ' ', e.last_name) AS  driver_name  \n" +
            "            from travel_incidences_log AS til \n" +
            "                INNER JOIN travel_incidences AS tl on tl.id = til.travel_incidence_id \n" +
            "                LEFT JOIN employee AS e ON e.id = til.driver_id \n" +
            "                INNER JOIN branchoffice AS bo ON bo.id = til.terminal_origin_id \n" +
            "                INNER JOIN branchoffice AS bd ON bd.id = til.terminal_destiny_id \n" +
            "                WHERE til.created_at BETWEEN ? AND ? ";

    private static final String QUERY_GET_DAILY_LOGS_LIST_LOAD = "SELECT\n" +
            "   tl.id AS travel_logs_id,\n" +
            "   tl.travel_log_code AS travel_logs_code,\n" +
            "   cr.name AS config_route_name,\n" +
            "   sr.travel_date AS schedule_route_travel_date,\n" +
            "   bo.id AS segment_origin_id,\n" +
            "   bo.prefix AS segment_origin_prefix,\n" +
            "   bd.id AS segment_destiny_id,\n" +
            "   bd.prefix AS segment_destiny_prefix,\n" +
            "   cr.terminal_destiny_id AS route_destiny_id,\n" +
            "   rbd.prefix AS route_destiny_prefix,\n" +
            "   v.economic_number AS vehicle_economic_number,\n" +
            "   CONCAT(e.name, ' ', e.last_name) AS driver_name,\n" +
            "   srd.travel_date AS aprox_departed,\n" +
            "   srd.arrival_date AS aprox_arrival,\n" +
            "   srd.started_at AS departed,\n" +
            "   srd.finished_at AS arrived,\n" +
            "   srd.destination_status AS segment_status,\n" +
            "   (SELECT count(DISTINCT cstt.boarding_pass_ticket_id) FROM shipments_ticket_tracking cstt\n" +
            "       LEFT JOIN boarding_pass_ticket bpt ON bpt.id = cstt.boarding_pass_ticket_id\n" +
            "       WHERE cstt.shipment_id = ship.id \n" +
            "       AND bpt.status = 1\n" +
            "       AND cstt.status = 'loaded') AS total_tickets,\n" +
            "   (SELECT count(DISTINCT csct.boarding_pass_complement_id) FROM shipments_complement_tracking csct\n" +
            "       LEFT JOIN boarding_pass_complement bpc ON bpc.id = csct.boarding_pass_complement_id\n" +
            "       WHERE csct.shipment_id = ship.id \n" +
            "       AND bpc.status = 1\n" +
            "       AND csct.status = 'loaded') AS total_complements,\n" +
            "   (SELECT count(DISTINCT csppt.parcel_package_id) FROM shipments_parcel_package_tracking csppt\n" +
            "       LEFT JOIN parcels_packages pp ON pp.id = csppt.parcel_package_id\n" +
            "       WHERE csppt.shipment_id = ship.id \n" +
            "       AND pp.status = 1\n" +
            "       AND csppt.status = 'loaded') AS load_total_packages,\n" +
            "   (SELECT COUNT(DISTINCT cspt.parcel_id) FROM shipments_parcel_package_tracking cspt\n" +
            "       LEFT JOIN parcels p ON p.id = cspt.parcel_id\n" +
            "       WHERE cspt.shipment_id = ship.id \n" +
            "       AND p.status = 1\n" +
            "       AND cspt.status = 'loaded') AS total_parcels\n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            " LEFT JOIN shipments ship ON ship.schedule_route_id = sr.id AND ship.id = tl.load_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = tl.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = tl.terminal_destiny_id\n" +
            " LEFT JOIN branchoffice rbd ON rbd.id = cr.terminal_destiny_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN employee e ON e.id = (SELECT srdriver.employee_id FROM schedule_route_driver srdriver\n" +
            "                               INNER JOIN schedule_route dsr ON dsr.id = srdriver.schedule_route_id\n" +
            "                               INNER JOIN schedule_route_destination dsrd ON dsrd.schedule_route_id = dsr.id\n" +
            "                                 AND dsrd.terminal_origin_id = srdriver.terminal_origin_id\n" +
            "                                 AND dsrd.terminal_destiny_id = srdriver.terminal_destiny_id\n" +
            "                               INNER JOIN config_destination dcd ON dcd.id = dsrd.config_destination_id\n" +
            "                               WHERE cd.order_origin BETWEEN dcd.order_origin AND dcd.order_destiny\n" +
            "                                 AND cd.order_destiny BETWEEN dcd.order_origin AND dcd.order_destiny\n" +
            "                                 AND dsr.id = sr.id)\n" +
            " WHERE DATE(sr.travel_date) = ? ";

    private static final String QUERY_GET_DAILY_LOGS_LIST_DOWNLOAD = "SELECT\n" +
            "   tl.id AS travel_logs_id,\n" +
            "   tl.travel_log_code AS travel_logs_code,\n" +
            "   cr.name AS config_route_name,\n" +
            "   sr.travel_date AS schedule_route_travel_date,\n" +
            "   bo.id AS segment_origin_id,\n" +
            "   bo.prefix AS segment_origin_prefix,\n" +
            "   bd.id AS segment_destiny_id,\n" +
            "   bd.prefix AS segment_destiny_prefix,\n" +
            "   cr.terminal_destiny_id AS route_destiny_id,\n" +
            "   rbd.prefix AS route_destiny_prefix,\n" +
            "   v.economic_number AS vehicle_economic_number,\n" +
            "   CONCAT(e.name, ' ', e.last_name) AS driver_name,\n" +
            "   (SELECT srd2.travel_date FROM schedule_route_destination srd2\n" +
            "   LEFT JOIN config_destination cd2 ON cd2.id = srd2.config_destination_id\n" +
            "   WHERE srd2.schedule_route_id = sr.id\n" +
            "   AND cd2.order_origin = cd.order_destiny AND cd2.order_destiny = cd.order_destiny + 1) AS aprox_departed,\n" +
            "   (SELECT srd2.arrival_date FROM schedule_route_destination srd2\n" +
            "   LEFT JOIN config_destination cd2 ON cd2.id = srd2.config_destination_id\n" +
            "   WHERE srd2.schedule_route_id = sr.id\n" +
            "   AND cd2.order_origin = cd.order_destiny AND cd2.order_destiny = cd.order_destiny + 1) AS aprox_departed,\n" +
            "   srd.started_at AS departed,\n" +
            "   srd.finished_at AS arrived,\n" +
            "   srd.destination_status AS segment_status,\n" +
            "   (SELECT count(DISTINCT cstt.boarding_pass_ticket_id) FROM shipments_ticket_tracking cstt\n" +
            "       LEFT JOIN boarding_pass_ticket bpt ON bpt.id = cstt.boarding_pass_ticket_id\n" +
            "       WHERE cstt.shipment_id = ship.id \n" +
            "       AND bpt.status = 1\n" +
            "       AND cstt.status = 'downloaded') AS total_tickets,\n" +
            "   (SELECT count(DISTINCT csct.boarding_pass_complement_id) FROM shipments_complement_tracking csct\n" +
            "       LEFT JOIN boarding_pass_complement bpc ON bpc.id = csct.boarding_pass_complement_id\n" +
            "       WHERE csct.shipment_id = ship.id \n" +
            "       AND bpc.status = 1\n" +
            "       AND csct.status = 'downloaded') AS total_complements,\n" +
            "   (SELECT count(DISTINCT csppt.parcel_package_id) FROM shipments_parcel_package_tracking csppt\n" +
            "       LEFT JOIN parcels_packages pp ON pp.id = csppt.parcel_package_id\n" +
            "       WHERE csppt.shipment_id = ship.id \n" +
            "       AND pp.status = 1\n" +
            "       AND csppt.status = 'downloaded') AS download_total_packages,\n" +
            "   (SELECT COUNT(DISTINCT cspt.parcel_id) FROM shipments_parcel_package_tracking cspt\n" +
            "       LEFT JOIN parcels p ON p.id = cspt.parcel_id\n" +
            "       WHERE cspt.shipment_id = ship.id \n" +
            "       AND p.status = 1\n" +
            "       AND cspt.status = 'downloaded') AS total_parcels\n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            " LEFT JOIN shipments ship ON ship.schedule_route_id = sr.id AND ship.id = tl.download_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = tl.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = tl.terminal_destiny_id\n" +
            " LEFT JOIN branchoffice rbd ON rbd.id = cr.terminal_destiny_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN employee e ON e.id = (SELECT srdriver.employee_id FROM schedule_route_driver srdriver\n" +
            "                               INNER JOIN schedule_route dsr ON dsr.id = srdriver.schedule_route_id\n" +
            "                               INNER JOIN schedule_route_destination dsrd ON dsrd.schedule_route_id = dsr.id\n" +
            "                                 AND dsrd.terminal_origin_id = srdriver.terminal_origin_id\n" +
            "                                 AND dsrd.terminal_destiny_id = srdriver.terminal_destiny_id\n" +
            "                               INNER JOIN config_destination dcd ON dcd.id = dsrd.config_destination_id\n" +
            "                               WHERE cd.order_origin BETWEEN dcd.order_origin AND dcd.order_destiny\n" +
            "                                 AND cd.order_destiny BETWEEN dcd.order_origin AND dcd.order_destiny\n" +
            "                                 AND dsr.id = sr.id)\n" +
            " WHERE DATE(sr.travel_date) = ? ";

    private static final String GET_SHIPMENT_BY_ID = "SELECT * from shipments s left join schedule_route sr on sr.id = s.schedule_route_id where s.id = ? AND sr.schedule_status = ? AND shipment_type = ? and shipment_status = 1";

    private static final String UPDATE_SCHEDULE_STATUS = "update\n" +
            "\tshipments s\n" +
            "\tinner join schedule_route sr on sr.id = s.schedule_route_id\n" +
            "\tinner join config_route cr on cr.id = sr.config_route_id\n" +
            "\tinner join schedule_route_destination srd on srd.schedule_route_id = sr.id\n" +
            "SET s.shipment_status = 0, \n" +
            "\tsr.schedule_status = {SCHEDULE_STATUS},\n" +
            "\tsrd.destination_status = {DESTINATION_STATUS}\n" +
            "where s.id = ?;";
    private static final String QUERY_VALIDATE_SHIPMENT_PARCEL_PACKAGE_TRACKING = "SELECT \n" +
            " id \n" +
            "FROM shipments_parcel_package_tracking \n" +
            "WHERE parcel_id = ?\n" +
            "AND parcel_package_id = ?\n" +
            "AND shipment_id = ?\n" +
            "AND status = ?;";

    private static final String QUERY_VALIDATE_SHIPMENT_TICKET_TRACKING = "SELECT \n" +
            " id\n" +
            "FROM shipments_ticket_tracking\n" +
            "WHERE boarding_pass_ticket_id = ?\n" +
            "AND shipment_id = ?\n" +
            "AND status = ?;";

    private static final String QUERY_VALIDATE_SHIPMENT_COMPLEMENT_TRACKING = "SELECT \n" +
            " id\n" +
            "FROM shipments_complement_tracking\n" +
            "WHERE boarding_pass_complement_id = ?\n" +
            "AND shipment_id = ?\n" +
            "AND status = ?;";

    private static final String QUERY_SCANNED_PARCELS = "SELECT\n" +
            "   p.id,\n" +
            "   p.parcel_tracking_code,\n" +
            "   p.parcel_status,\n" +
            "   p.total_packages,\n" +
            "   IF(sp.id IS NULL, FALSE, TRUE) AS flag_scanned,\n" +
            "   sp.trailer_id,\n" +
            "   shipppt.trailer_id AS packages_trailer_id\n" +
            "FROM shipments_parcels sp\n" +
            "LEFT JOIN shipments ship ON ship.id = sp.shipment_id\n" +
            "LEFT JOIN shipments_parcel_package_tracking shipppt ON shipppt.shipment_id = ship.id\n" +
            "AND (shipppt.trailer_id IS NULL OR shipppt.latest_movement IS TRUE)\n" +
            "LEFT JOIN parcels p ON p.id = sp.parcel_id\n" +
            "WHERE sp.shipment_id = ? GROUP BY p.parcel_tracking_code;";

    private static final String QUERY_SCANNED_DOWNLOAD_PARCELS = "SELECT\n" +
            "   p.id,\n" +
            "   p.parcel_tracking_code,\n" +
            "   p.parcel_status,\n" +
            "   p.total_packages,\n" +
            "   IF(shipppt.id IS NULL, FALSE, TRUE) AS flag_scanned,\n" +
            "   shipppt.trailer_id,\n" +
            "   shipppt.trailer_id AS packages_trailer_id\n" +
            "FROM shipments_parcel_package_tracking shipppt\n" +
            "LEFT JOIN shipments ship ON ship.id = shipppt.shipment_id\n" +
            "LEFT JOIN parcels p ON p.id = shipppt.parcel_id\n" +
            "WHERE ship.id = ? GROUP BY p.parcel_tracking_code;";

    private static final String QUERY_GET_PACKAGES_BY_PARCEL_ID = "SELECT\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.shipping_type,\n" +
            "   pp.package_code,\n" +
            "   pp.package_status,\n" +
            "   shipppt.trailer_id,\n" +
            "   IF(pt.id IS NULL, FALSE, TRUE) AS is_transhipment\n" +
            "FROM parcels_packages pp\n" +
            "LEFT JOIN shipments_parcel_package_tracking shipppt ON shipppt.parcel_package_id = pp.id\n" +
            "LEFT JOIN parcels p ON p.id = pp.parcel_id\n" +
            "LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            "WHERE p.id = ? group by pp.id;";

    private static final String QUERY_SCANNED_PACKAGES_BY_SHIPMENT_ID = "SELECT\n" +
            "   pp.id AS parcel_package_id,\n" +
            "   pp.shipping_type,\n" +
            "   pp.package_code,\n" +
            "   pp.package_status,\n" +
            "   shipppt.trailer_id,\n" +
            "   IF(pt.id IS NULL, FALSE, TRUE) AS is_transhipment\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN shipments_parcel_package_tracking shipppt ON shipppt.parcel_package_id = pp.id\n" +
            "LEFT JOIN parcels p ON p.id = pp.parcel_id\n" +
            "LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            "WHERE shipppt.shipment_id = ? AND p.id = ? group by pp.id;";

    private static final String QUERY_GET_SHIPMENT_LOAD_INFO = "SELECT\n" +
            "   s.id AS shipment_id,\n" +
            "   tl.travel_log_code,\n" +
            "   v.economic_number AS vehicle_economic_number,\n" +
            "   e.name AS driver_name,\n" +
            "   e.last_name AS driver_last_name,\n" +
            "   cr.name AS config_route_name,\n" +
            "   bo.id AS origin_terminal_id,\n" +
            "   bo.prefix AS origin_terminal_prefix,\n" +
            "   bd.id AS destiny_terminal_id,\n" +
            "   bd.prefix AS destiny_terminal_prefix,\n" +
            "   cd.order_origin\n" +
            "FROM shipments s\n" +
            "INNER JOIN schedule_route sr ON sr.id = s.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "INNER JOIN travel_logs tl ON tl.load_id = s.id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "INNER JOIN employee e ON e.id = s.driver_id\n" +
            "INNER JOIN branchoffice bo ON bo.id = tl.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = tl.terminal_destiny_id\n" +
            "WHERE s.id = ?";

    private final static String QUERY_GET_HITCHED_TRAILERS = "SELECT\n" +
            "   t.id,\n" +
            "   t.name,\n" +
            "   t.economic_number,\n" +
            "   t.plate,\n" +
            "   tr.Clave_tipo_remolque,\n" +
            "   tr.Remolque_o_semirremolque\n" +
            "FROM shipments_trailers st\n" +
            "INNER JOIN trailers t ON t.id = st.trailer_id\n" +
            "INNER JOIN c_SubTipoRem tr ON tr.id = t.c_SubTipoRem_id\n" +
            "WHERE st.schedule_route_id = ?\n" +
            "AND st.action IN ('assign', 'transfer', 'hitch')\n" +
            "AND st.latest_movement IS TRUE\n" +
            "AND t.in_use IS TRUE;";

    private static final String QUERY_GET_TRAILERS_TO_HITCH = "SELECT\n" +
            "   t.id,\n" +
            "   t.name,\n" +
            "   t.plate,\n" +
            "   tr.Clave_tipo_remolque,\n" +
            "   tr.Remolque_o_semirremolque,\n" +
            "   true AS transhipment\n" +
            "FROM trailers t\n" +
            "INNER JOIN c_SubTipoRem tr ON tr.id = t.c_SubTipoRem_id\n" +
            "INNER JOIN shipments_trailers st ON st.trailer_id = t.id\n" +
            "   AND st.created_at > SUBDATE(NOW(), 10)\n" +
            "INNER JOIN schedule_route sr ON sr.id = st.schedule_route_id\n" +
            "INNER JOIN shipments s ON s.id = st.shipment_id\n" +
            "WHERE 'release_transhipment' = (SELECT st2.action FROM shipments_trailers st2 \n" +
            "           INNER JOIN shipments s2 ON s2.id = st2.shipment_id\n" +
            "           WHERE st2.schedule_route_id = sr.id\n" +
            "           AND st2.trailer_id = st.trailer_id\n" +
            "           AND s2.terminal_id = s.terminal_id\n" +
            "           ORDER BY st2.id DESC LIMIT 1)\n" +
            "   AND s.terminal_id = ?\n" +
            "GROUP BY t.id;";

    private static final String QUERY_GET_TOTAL_PARCELS = "SELECT (parcels.total_parcels + parcels.total_transhipments) AS total_parcels FROM (SELECT\n" +
            "    COUNT(DISTINCT p.id) AS total_parcels,\n" +
            "    (SELECT COUNT(DISTINCT pth.parcel_id) FROM parcels_transhipments_history pth\n" +
            "    INNER JOIN schedule_route_destination srd2 ON srd2.id = pth.schedule_route_destination_id\n" +
            "    INNER JOIN config_destination cd2 ON cd2.id = srd2.config_destination_id\n" +
            "        AND cd2.terminal_origin_id = srd2.terminal_origin_id\n" +
            "        AND cd2.terminal_destiny_id = srd2.terminal_destiny_id\n" +
            "    WHERE srd2.id = srd.id\n" +
            "    AND cd2.order_origin <= cd.order_origin\n" +
            "    AND cd2.order_destiny >= cd.order_destiny) AS total_transhipments\n" +
            "FROM\n" +
            "    parcels p\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = p.schedule_route_destination_id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "    AND cd.terminal_origin_id = srd.terminal_origin_id\n" +
            "    AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "WHERE\n" +
            "    srd.schedule_route_id = ?\n" +
            "    AND cd.order_origin <= ?\n" +
            "    AND cd.order_destiny >= ?\n" +
            "    AND ((sppt.status IN ('loaded') AND sppt.trailer_id IS NULL)\n" +
            "       OR (sppt.status IN ('loaded', 'transfer') AND sppt.latest_movement IS TRUE))) AS parcels;\n";

    private static final String QUERY_GET_TOTAL_PACKAGES = "SELECT\n" +
            "\tCOUNT(DISTINCT pp.id) AS total_packages\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "LEFT JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = p.schedule_route_destination_id\n" +
            "   OR pth.schedule_route_destination_id = srd.id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN shipments s ON s.schedule_route_id = srd.schedule_route_id\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.shipment_id = s.id \n" +
            "AND sppt.parcel_id = p.id AND sppt.parcel_package_id = pp.id\n" +
            "WHERE srd.schedule_route_id = ?\n" +
            "AND cd.order_origin <= ? AND cd.order_destiny >= ?\n" +
            "AND ((sppt.status IN ('loaded') AND sppt.trailer_id IS NULL)\n" +
            "       OR (sppt.status IN ('loaded', 'transfer') AND sppt.latest_movement IS TRUE));";

    private static final String QUERY_GET_TOTAL_TICKETS_AND_COMPLEMENTS = "SELECT \n" +
            " COUNT(DISTINCT bpt.id) AS total_tickets,\n" +
            " COUNT(DISTINCT bpc.id) AS total_complements\n" +
            "FROM schedule_route_destination srd\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "\tAND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "LEFT JOIN boarding_pass_route bpr ON bpr.schedule_route_destination_id = srd.id\n" +
            "LEFT JOIN boarding_pass_passenger bpp ON bpp.boarding_pass_id = bpr.boarding_pass_id\n" +
            "LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id\n" +
            "LEFT JOIN boarding_pass_complement bpc ON bpc.boarding_pass_ticket_id = bpt.id\n" +
            "WHERE srd.schedule_route_id = ?\t\n" +
            "AND cd.order_origin <= ? AND cd.order_destiny >= ?\n" +
            "AND bpt.checkedin_at IS NOT NULL;";

    //</editor-fold>
}
