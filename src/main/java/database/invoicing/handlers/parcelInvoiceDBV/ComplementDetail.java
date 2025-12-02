package database.invoicing.handlers.parcelInvoiceDBV;

import database.commons.DBHandler;
import database.invoicing.ComplementLetterPorteDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.shipments.ShipmentsDBV.*;
import static database.shipments.ShipmentsDBV.PARCELS_TO_OTHER_DESTINY;
import static service.commons.Constants.ID;

public class ComplementDetail extends DBHandler<ComplementLetterPorteDBV> {
    public ComplementDetail(ComplementLetterPorteDBV dbVerticle) { super(dbVerticle); }

    @Override
    public void handle(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer travelLogsId = body.getInteger(TRAVEL_LOGS_ID);

        this.getTravelLogDetail(travelLogsId).whenComplete((travelLogDetail, errTravelLogsDetail) -> {
            try {
                if (errTravelLogsDetail != null){
                    throw errTravelLogsDetail;
                }
                Integer scheduleRouteId = travelLogDetail.getInteger(SCHEDULE_ROUTE_ID);
                Integer configRouteId = travelLogDetail.getInteger(CONFIG_ROUTE_ID);
                Integer cdOrderOrigin = travelLogDetail.getInteger(ORDER_ORIGIN);
                Integer segmentTerminalDestinyId = travelLogDetail.getInteger("segment_terminal_destiny_id");
                Integer cdOrderDestiny = (Integer) travelLogDetail.remove(ORDER_DESTINY);
                Integer loadId = travelLogDetail.getInteger(LOAD_ID);
                Integer downloadId = travelLogDetail.getInteger(DOWNLOAD_ID);
                this.getLoadInfo(loadId, scheduleRouteId, cdOrderOrigin).whenComplete((resultLoad, errorLoad) -> {
                    try {
                        if (errorLoad != null){
                            throw new Exception(errorLoad);
                        }
                        travelLogDetail.put(LOAD, resultLoad);
                        if (downloadId != null){
                            this.getDownloadInfo(downloadId, scheduleRouteId, configRouteId, cdOrderOrigin, cdOrderDestiny, segmentTerminalDestinyId).whenComplete((resultDownload, errorDownload) -> {
                                try {
                                    if (errorDownload != null){
                                        throw new Exception(errorDownload);
                                    }
                                    travelLogDetail.put(DOWNLOAD, resultDownload);
                                    message.reply(travelLogDetail);
                                } catch (Exception e){
                                    e.printStackTrace();
                                    reportQueryError(message, e.getCause());
                                }
                            });
                        } else {
                            travelLogDetail.put(DOWNLOAD, new JsonObject());
                            message.reply(travelLogDetail);
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        reportQueryError(message, e.getCause());
                    }
                });

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t.getCause());
            }
        });
    }

    private CompletableFuture<JsonObject> getTravelLogDetail(Integer travelLogsId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_BY_TRAVEL_LOGS_ID, new JsonArray().add(travelLogsId), replyDailyLogs -> {
            try {
                if (replyDailyLogs.failed()) {
                    throw replyDailyLogs.cause();
                }
                List<JsonObject> listDailyLogs = replyDailyLogs.result().getRows();
                if (listDailyLogs.isEmpty()) {
                    throw new Throwable("Element not found");
                }
                JsonObject travelLogDetail = listDailyLogs.get(0);
                getTravelLogStamps(travelLogsId).whenComplete((stamps, errStamps) -> {
                    try {
                        if (errStamps != null) {
                            throw errStamps;
                        }
                        getDriverInfo(travelLogDetail.getInteger("schedule_route_id"), travelLogDetail.getInteger("segment_terminal_origin_id"), travelLogDetail.getInteger("segment_terminal_destiny_id")).whenComplete( (drivers, errDrivers) -> {
                            try{
                                if(errDrivers != null){
                                    throw errDrivers;
                                }
                                travelLogDetail.mergeIn(drivers);
                                travelLogDetail.mergeIn(stamps);
                                future.complete(travelLogDetail);
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
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getTravelLogStamps(int travelLogsId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_DAILY_LOGS_STAMPS_BY_TRAVEL_LOG_ID, new JsonArray().add(travelLogsId), replyStamps -> {
            try {
                if (replyStamps.failed()){
                    throw replyStamps.cause();
                }
                List<JsonObject> listDailyLogs = replyStamps.result().getRows();
                if (listDailyLogs.isEmpty()) {
                    throw new Throwable("Stamps not found");
                }
                JsonObject travelLogStamps = listDailyLogs.get(0);
                future.complete(travelLogStamps);
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }
    private CompletableFuture<JsonObject> getDriverInfo(Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDesinyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_DRIVER_INFO, new JsonArray().add(scheduleRouteId).add(terminalOriginId).add(terminalDesinyId), replyDriverInfo ->{
            try{
                List<JsonObject> listDriverInfo = replyDriverInfo.result().getRows();
                if(listDriverInfo.isEmpty()){
                    throw new Throwable("Driver Info Not Found");
                }
                JsonObject travelDriverDetail = listDriverInfo.get(0);
                future.complete(travelDriverDetail);

            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }


    private CompletableFuture<JsonObject> getLoadInfo(Integer loadShipmentId, Integer scheduleRouteId, Integer orderOrigin){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.getShipmentInfo(loadShipmentId, true).whenComplete((loadInfo, errorLoad) -> {
            try {
                if (errorLoad != null){
                    throw new Exception(errorLoad);
                }

                this.getTerminalsByScheduleRouteId(scheduleRouteId, orderOrigin).whenComplete((resultTerminals, errorTerminals) -> {
                    try {
                        if (errorTerminals != null) {
                            throw new Exception(errorTerminals);
                        }
                        List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                        loadInfo.put(DETAIL, new JsonArray());
                        resultTerminals.forEach(t -> {
                            JsonObject terminal = (JsonObject) t;
                            task.add(this.getLoadElementsByTerminal(terminal, scheduleRouteId, loadInfo, loadInfo.getJsonArray(DETAIL)));
                        });
                        CompletableFuture.allOf(task.toArray(new CompletableFuture[resultTerminals.size()])).whenComplete((resultElementsByTerminal, errorElementsByTerminal)->{
                            try {
                                if(errorElementsByTerminal != null){
                                    throw new Exception(errorElementsByTerminal);
                                }
                                this.getLoadParcelsAndPackagesTranshipments(loadInfo, scheduleRouteId).whenComplete((resLoadParcelsTranshipments, errorLoadParcelsTranshipments) -> {
                                    try {
                                        if (errorLoadParcelsTranshipments != null) {
                                            throw errorLoadParcelsTranshipments;
                                        }
                                        future.complete(loadInfo);
                                    } catch (Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });
                            }catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getLoadParcelsAndPackagesTranshipments(JsonObject loadInfo, int scheduleRouteId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        List<CompletableFuture<JsonObject>> task = new ArrayList<>();
        task.add(this.getParcelsTranshipmentsDailyLogsLoad(loadInfo, scheduleRouteId));
        task.add(this.getPackagesTranshipmentsDailyLogsLoad(loadInfo, scheduleRouteId));
        CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((resultElementsByTerminal, errorElementsByTerminal)->{
            try {
                if(errorElementsByTerminal != null){
                    throw new Exception(errorElementsByTerminal);
                }
                future.complete(loadInfo);
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getShipmentInfo(Integer referenceId, boolean validateShipmentStatus){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_DAILY_LOGS_DETAIL_SHIPMENT_INFO, new JsonArray().add(referenceId), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> listDailyLogsLoadOrDownload = reply.result().getRows();
                if (listDailyLogsLoadOrDownload.isEmpty()){
                    throw new Exception("Load or download info not found");
                }

                JsonObject shipment = listDailyLogsLoadOrDownload.get(0);
                Integer scheduleRouteId = shipment.getInteger(SCHEDULE_ROUTE_ID);
                Integer orderOrigin = shipment.getInteger(ORDER_ORIGIN);
                Integer orderDestiny = shipment.getInteger(ORDER_DESTINY);
                SHIPMENT_STATUS shipmentStatus = SHIPMENT_STATUS.values()[shipment.getInteger("shipment_status")];

                if (validateShipmentStatus && !shipmentStatus.equals(SHIPMENT_STATUS.CLOSE)) {
                    throw new Exception("The shipment has not been closed");
                }

                this.getTravelLogDrivers(scheduleRouteId, orderOrigin, orderDestiny).whenComplete((resDrivers, errDrivers) -> {
                    try {
                        if (errDrivers != null){
                            throw errDrivers;
                        }

                        shipment.put("driver_name", resDrivers.stream().map(d ->d.getString("driver_name")).collect(Collectors.joining(", ")));
                        future.complete(shipment);

                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> getTravelLogDrivers(Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(orderOrigin).add(orderDestiny).add(scheduleRouteId);

        this.dbClient.queryWithParams(QUERY_GET_DRIVERS_DAILY_LOGS_DETAIL, params, reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> driversInfo = reply.result().getRows();
                future.complete(driversInfo);
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getTerminalsByScheduleRouteId(Integer scheduleRouteId, Integer orderOrigin) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_DAILY_LOGS_GET_TERMINALS_DESTINY_BY_SCHEDULE_ID, new JsonArray().add(scheduleRouteId).add(orderOrigin).add(orderOrigin), reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> listTerminals = reply.result().getRows();
                if (listTerminals.isEmpty()) {
                    throw new Exception("Terminals by schedule_route_id not found");
                }

                future.complete(new JsonArray(listTerminals));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getLoadElementsByTerminal(JsonObject terminal, Integer scheduleRouteId, JsonObject loadInfo, JsonArray loadInfoDetail){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer orderDestiny = loadInfo.getInteger(ORDER_DESTINY);
        Integer terminalOriginId = terminal.getInteger(TERMINAL_ORIGIN_ID);
        Integer terminalDestinyId = terminal.getInteger(TERMINAL_DESTINY_ID);

        List<CompletableFuture<JsonObject>> task = new ArrayList<>();

        task.add(this.getTicketsDailyLogsLoad(terminal, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny));
        task.add(this.getComplementsDailyLogsLoad(terminal, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny));
        task.add(this.getParcelsDailyLogsLoad(terminal, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny, false, false));
        task.add(this.getParcelsDailyLogsLoad(terminal, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny, true, false));
        task.add(this.getPackagesDailyLogsLoad(terminal, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny, false));

        CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((resultElementsByTerminal, errorElementsByTerminal)->{
            try {
                if(errorElementsByTerminal != null){
                    throw new Exception(errorElementsByTerminal);
                }

                loadInfoDetail.add(terminal);
                future.complete(loadInfo);

            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> getTicketsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_TICKETS_DAILY_LOGS_LOAD);
        JsonArray params = new JsonArray().add(scheduleRouteId).add(orderDestiny).add(orderDestiny);

        if(terminalOriginId != null){
            QUERY.append(" AND srd.terminal_origin_id = ? ");
            params.add(terminalOriginId);
        }

        if(terminalDestinyId != null){
            QUERY.append(" AND srd.terminal_destiny_id = ? ");
            params.add(terminalDestinyId);
        }

        QUERY.append("GROUP BY stt.boarding_pass_ticket_id");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(TICKETS, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> getComplementsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_COMPLEMENTS_DAILY_LOGS_LOAD);
        JsonArray params = new JsonArray().add(scheduleRouteId).add(orderDestiny).add(orderDestiny);

        if(terminalOriginId != null){
            QUERY.append(" AND srd.terminal_origin_id = ? ");
            params.add(terminalOriginId);
        }

        if(terminalDestinyId != null){
            QUERY.append(" AND srd.terminal_destiny_id = ? ");
            params.add(terminalDestinyId);
        }

        QUERY.append("GROUP BY sct.boarding_pass_complement_id");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(COMPLEMENTS, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> getPackagesDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny, boolean includeTranshipments){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_PACKAGES_DAILY_LOGS_LOAD);
        JsonArray params = new JsonArray()
                .add(orderDestiny).add(orderDestiny)
                .add(scheduleRouteId)
                .add(orderDestiny).add(orderDestiny);

        if(terminalOriginId != null){
            QUERY.append(" AND srd.terminal_origin_id = ? ");
            params.add(terminalOriginId);
        }

        if(terminalDestinyId != null){
            QUERY.append(" AND srd.terminal_destiny_id = ? ");
            params.add(terminalDestinyId);
        }

        if (!includeTranshipments) {
            QUERY.append(" AND pth.id IS NULL ");
        }

        QUERY.append("GROUP BY sppt.parcel_package_id");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(PACKAGES, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> getPackagesTranshipmentsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_PACKAGES_TRANSHIPMENTS_DAILY_LOGS_LOAD);
        JsonArray params = new JsonArray().add(scheduleRouteId);

        QUERY.append("GROUP BY sppt.parcel_package_id");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(PACKAGES_TRANSHIPMENTS, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> getParcelsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny, boolean isPartial, boolean includeTranshipments){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_PARCELS_DAILY_LOG_LOAD);
        JsonArray params = new JsonArray()
                .add(orderDestiny).add(orderDestiny)
                .add(scheduleRouteId)
                .add(orderDestiny).add(orderDestiny);

        if(terminalOriginId != null){
            QUERY.append(" AND srd.terminal_origin_id = ? \n");
            params.add(terminalOriginId);
        }

        if(terminalDestinyId != null){
            QUERY.append(" AND srd.terminal_destiny_id = ? \n");
            params.add(terminalDestinyId);
        }

        if (!includeTranshipments) {
            QUERY.append(" AND pth.id IS NULL \n");
        }

        QUERY.append(" GROUP BY sppt.parcel_id, sppt.parcel_package_id) as parcels GROUP BY parcels.parcel_id \n ");
        QUERY.append(isPartial ?
                " HAVING parcels.count_total_packages > COUNT(parcels.parcel_package_id)\n"
                : "");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(isPartial ? PARCELS_PARTIALS : PARCELS, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    public CompletableFuture<JsonObject> getParcelsTranshipmentsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_PARCELS_TRANSHIPMENTS_DAILY_LOG_LOAD);
        JsonArray params = new JsonArray().add(scheduleRouteId);

        QUERY.append(" GROUP BY sppt.parcel_id, sppt.parcel_package_id) as parcels GROUP BY parcels.parcel_id \n ");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(PARCELS_TRANSHIPMENTS, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getDownloadInfo(Integer downloadShipmentId, Integer scheduleRouteId, Integer configRouteId, Integer cdOrderOrigin, Integer cdOrderDestiny, Integer segmentTerminalDestinyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.getShipmentInfo(downloadShipmentId, false).whenComplete((downloadInfo, errorDownload) -> {
            try {
                if (errorDownload != null){
                    throw new Exception(errorDownload);
                } else {
                    this.getDownloadTerminalOrigins(scheduleRouteId, cdOrderDestiny).whenComplete((resultTerminals, errorTerminals) -> {
                        try {
                            if (errorTerminals != null){
                                throw new Exception(errorTerminals);
                            }
                            List<CompletableFuture<JsonArray>> task = new ArrayList<>();
                            downloadInfo.put(DETAIL, new JsonArray());
                            resultTerminals.forEach(t -> {
                                JsonObject terminal = (JsonObject) t;
                                task.add(this.getDownloadElementsByTerminal(terminal, scheduleRouteId, configRouteId, segmentTerminalDestinyId, cdOrderDestiny, downloadInfo.getJsonArray(DETAIL)));
                            });
                            CompletableFuture.allOf(task.toArray(new CompletableFuture[resultTerminals.size()])).whenComplete((resultElementsByTerminal, errorElementsByTerminal)->{
                                try {
                                    if(errorElementsByTerminal != null){
                                        throw new Exception(errorElementsByTerminal);
                                    }
                                    this.getParcelsTranshipmentsDailyLogsDownload(scheduleRouteId, segmentTerminalDestinyId).whenComplete((resultParcelsTranshipments, errorParcelsTranshipments) -> {
                                        try {
                                            if(errorParcelsTranshipments != null) {
                                                throw errorParcelsTranshipments;
                                            }
                                            downloadInfo.put(PARCELS_TRANSHIPMENTS_TO_DOWNLOAD, resultParcelsTranshipments);
                                            this.getDownloadParcelsIncomplete(scheduleRouteId, segmentTerminalDestinyId).whenComplete((resultParcelsIncomplete, errorParcelsIncomplete) -> {
                                                try {
                                                    if (errorParcelsIncomplete != null){
                                                        throw new Exception(errorParcelsIncomplete);
                                                    }
                                                    downloadInfo.put(PARCELS_INCOMPLETE, resultParcelsIncomplete);
                                                    future.complete(downloadInfo);
                                                } catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });
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
                    });
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getDownloadTerminalOrigins(Integer scheduleRouteId, Integer cdOrderDestiny){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(cdOrderDestiny);

        this.dbClient.queryWithParams(QUERY_GET_DOWNLOAD_TERMINAL_ORIGINS, params, reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> listTerminals = reply.result().getRows();
                if (listTerminals.isEmpty()){
                    throw new Exception("Terminals origins by schedule_route_id and order_destiny not found");
                }
                future.complete(new JsonArray(listTerminals));
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getDownloadElementsByTerminal(JsonObject terminal, Integer scheduleRouteId, Integer configRouteId, Integer segmentTerminalDestinyId, Integer orderDestiny, JsonArray downloadInfoDetail){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

        Integer terminalOriginId = terminal.getInteger(ID);

        // TICKETS
        tasks.add(this.getTicketsDailyLogsDownload(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId));
        tasks.add(this.getTicketsDailyLogsOtherDestiny(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId, configRouteId, orderDestiny));

        // COMPLEMENTS
        tasks.add(this.getComplementsDailyLogsDownload(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId));
        tasks.add(this.getComplementsDailyLogsOtherDestiny(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId, configRouteId, orderDestiny));

        // PARCELS
        tasks.add(this.getParcelsDailyLogsDownload(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId));
        tasks.add(this.getParcelsDailyLogsOtherDestiny(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId, configRouteId, orderDestiny));

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resultDownloadElements, errorDownloadElements)->{
            try {
                if(errorDownloadElements != null){
                    throw errorDownloadElements;
                }

                downloadInfoDetail.add(terminal);
                future.complete(downloadInfoDetail);

            }catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getTicketsDailyLogsDownload(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId).add(terminalOriginId);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_TICKETS_BY_TERMINAL.concat(DOWNLOAD_TICKETS_TO_DOWNLOAD_PARAMS), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(TICKETS_TO_DOWNLOAD, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getTicketsDailyLogsOtherDestiny(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId,
                                                                          Integer terminalDestinyId, Integer configRouteId, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId).add(terminalOriginId)
                .add(configRouteId).add(orderDestiny);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_TICKETS_BY_TERMINAL.concat(DOWNLOAD_TICKETS_TO_OTHER_DESTINY_PARAMS), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(TICKETS_TO_OTHER_DESTINY, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getComplementsDailyLogsDownload(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId).add(terminalOriginId);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_COMPLEMENTS_BY_TERMINAL.concat(COMPLEMENTS_TO_DOWNLOAD_PARAMS), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(COMPLEMENTS_TO_DOWNLOAD, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getComplementsDailyLogsOtherDestiny(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId,
                                                                              Integer terminalDestinyId, Integer configRouteId, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId).add(terminalOriginId)
                .add(configRouteId).add(orderDestiny);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_COMPLEMENTS_BY_TERMINAL.concat(DOWNLOAD_COMPLEMENTS_TO_OTHER_DESTINY_PARAMS), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(COMPLEMENTS_TO_OTHER_DESTINY, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getParcelsDailyLogsDownload(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId).add(terminalOriginId);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_BY_TERMINAL.concat(DOWNLOAD_PARCELS_TO_DOWNLOAD_PARAMS), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(PARCELS_TO_DOWNLOAD, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> getParcelsTranshipmentsDailyLogsDownload(Integer scheduleRouteId, Integer terminalDestinyId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(terminalDestinyId).add(scheduleRouteId).add(terminalDestinyId);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_TRANSHIPMENTS_BY_TERMINAL, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                future.complete(result);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getParcelsDailyLogsOtherDestiny(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId,
                                                                          Integer terminalDestinyId, Integer configRouteId, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId).add(terminalOriginId)
                .add(configRouteId).add(orderDestiny);

        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_BY_TERMINAL.concat(DOWNLOAD_PARCELS_TO_OTHER_DESTINY_PARAMS), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                dailyLog.put(PARCELS_TO_OTHER_DESTINY, result);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getDownloadParcelsIncomplete(Integer scheduleRouteId, Integer terminalDestinyId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        JsonArray parcelsIncomplete = new JsonArray();
        JsonArray params = new JsonArray().add(scheduleRouteId).add(terminalDestinyId);
        this.dbClient.queryWithParams(QUERY_GET_DAILY_LOGS_PARCELS_INCOMPLETE, params, reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> listParcelsIncomplete = reply.result().getRows();
                if (listParcelsIncomplete.isEmpty()){
                    future.complete(parcelsIncomplete);
                } else {
                    parcelsIncomplete.addAll(new JsonArray(listParcelsIncomplete));
                    future.complete(new JsonArray(listParcelsIncomplete));
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_BY_TRAVEL_LOGS_ID = "SELECT\n" +
            "     tl.id AS travel_logs_id,\n" +
            "     tl.schedule_route_id,\n" +
            "     srd.id AS schedule_route_destination_id,\n" +
            "     cd.order_origin,\n" +
            "     cd.order_destiny,\n" +
            "     tl.travel_log_code AS travel_logs_code,\n" +
            "     cr.id AS config_route_id,\n" +
            "     cr.parcel_route,\n" +
            "     cr.name AS config_route_name,\n" +
            "     v.id AS vehicle_id,\n" +
            "     v.economic_number AS vehicle_economic_number,\n" +
            "     cv.seatings AS config_vehicle_seatings,\n" +
            "     srd.terminal_origin_id AS segment_terminal_origin_id,\n" +
            "     segbo.prefix AS segment_terminal_origin_prefix,\n" +
            "     segbo.name AS segment_terminal_origin_name,\n" +
            "     srd.terminal_destiny_id AS segment_terminal_destiny_id,\n" +
            "     destiny.prefix AS segment_terminal_destiny_prefix,\n" +
            "     destiny.name AS segment_terminal_destiny_name,\n" +
            "     srd.finished_at AS arrived_at,\n" +
            "     srd.started_at AS departed_at,\n" +
            "     tl.load_id,\n" +
            "     tl.download_id,\n" +
            "     segbo.zip_code as zipCodeOrigin, \n" +
            "     colOrigin.c_Colonia as colOrigin, \n"+
            "     localidad.c_Localidad as localidadOrigin, \n" +
            "     munOrigin.c_Municipio as municipioOrigin, \n" +
            "     munOrigin.c_Estado as estadoOrigin, \n"+
            "     segbd.zip_code as zipCodeDestiny, \n" +
            "     colDestiny.c_Colonia as colDestiny, \n" +
            "     localidadDestiny.c_Localidad as localidadDestiny, \n" +
            "     munDestiny.c_Municipio as munDestiny, \n" +
            "     munDestiny.c_Estado as estadoDestiny, \n" +
            "     segbd.zip_code as zipCodeDestiny, \n"+
            "     tp.clave as tipoPermiso,\n" +
            "     v.sct_license as NumPermisoSCT, \n" +
            "     v.plate AS PlacaVM, \n" +
            "     v.vehicle_year AS AnioModeloVM, \n" +
            "     v.policy AS PolizaRespCivil, \n" +
            "     v.policy_insurance AS AseguraRespCivil, \n" +
            "     cva.clave AS ConfigVehicular, \n" +
            "     pdt.distance_km AS distance, \n" +
            "     tl.status \n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = tl.schedule_route_id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            " INNER JOIN branchoffice segbo ON segbo.id = srd.terminal_origin_id\n" +
            " INNER JOIN county cn ON cn.id = segbo.county_id \n" +
            " INNER join state staOrigin ON staOrigin.id = segbo.state_id\n" +
            " INNER JOIN city cityOrigin ON cityOrigin.id = segbo.city_id\n" +
            " INNER join c_Colonia colOrigin ON segbo.zip_code = colOrigin.c_CodigoPostal\n" +
            " INNER join c_Municipio munOrigin ON staOrigin.name_sat = munOrigin.c_Estado AND munOrigin.description like concat(\"%\",cn.name,\"%\")  \n" +
            " INNER join c_Localidad localidad ON staOrigin.name_sat = localidad.c_Estado AND localidad.descripcion like concat(\"%\",cityOrigin.name,\"%\") \n" +
            " INNER JOIN branchoffice segbd ON segbd.id = srd.terminal_destiny_id\n" +
            " INNER JOIN county AS cnd ON cnd.id = segbd.county_id \n" +
            " INNER join state staDestiny ON staDestiny.id = segbd.state_id\n" +
            " INNER JOIN city cityDestiny ON cityDestiny.id = segbd.city_id\n" +
            " INNER join c_Colonia colDestiny ON segbd.zip_code = colDestiny.c_CodigoPostal\n" +
            " INNER join c_Municipio munDestiny ON staDestiny.name_sat = munDestiny.c_Estado AND munDestiny.description like concat(\"%\",cnd.name,\"%\")  \n" +
            " INNER join c_Localidad localidadDestiny ON staDestiny.name_sat = localidadDestiny.c_Estado AND localidadDestiny.descripcion like concat(\"%\",cityDestiny.name,\"%\") \n" +
            " INNER JOIN branchoffice destiny ON destiny.id = tl.terminal_destiny_id\n" +
            " INNER JOIN package_terminals_distance pdt ON pdt.terminal_origin_id = segbo.id AND pdt.terminal_destiny_id = destiny.id" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN config_vehicle cv ON cv.id = v.config_vehicle_id\n" +
            " LEFT JOIN c_ConfigAutotransporte cva ON cva.id = v.c_ConfigAutotransporte_id\n" +
            " LEFT JOIN c_TipoPermiso AS tp ON tp.id = v.sat_permit_id \n" +
            " WHERE tl.id = ? LIMIT 1;";

    public static final String QUERY_GET_DRIVER_INFO = "           select \n" +
            "           e2.rfc as RFCFiguraDriver, \n" +
            "           e2.driver_license as NumLicenciaDriver, \n" +
            "           CONCAT(e2.name, \" \", e2.last_name) as NombreFiguraDriver,\n" +
            "           localidadD.c_Localidad as LocalidadDriver, \n" +
            "           munOriginD.c_Municipio as MunicipioDriver, \n" +
            "           munOriginD.c_Estado as EstadoDriver,\n" +
            "           bdr.zip_code as CodigoPostalDriver \n" +
            "           from schedule_route_driver srd \n" +
            "           LEFT JOIN employee e2 ON  e2.id = srd.employee_id\n" +
            "           LEFT JOIN branchoffice bdr ON bdr.id = e2.branchoffice_id \n" +
            "           LEFT JOIN county AS cnd ON cnd.id = bdr.county_id \n" +
            "           LEFT join state staOriginD ON staOriginD.id = e2.state_id\n" +
            "           LEFT join c_Municipio munOriginD ON staOriginD.name_sat = munOriginD.c_Estado AND munOriginD.description like concat(\"%\",cnd.name,\"%\")" +
            "           LEFT join c_Localidad localidadD ON staOriginD.name_sat = localidadD.c_Estado AND localidadD.descripcion like concat(\"%\",cnd.name,\"%\") \n" +
            "           where srd.schedule_route_id = ? and srd.terminal_origin_id = ? and srd.terminal_destiny_id = ? order by srd.created_at desc LIMIT 1";

    private static final String QUERY_DAILY_LOGS_STAMPS_BY_TRAVEL_LOG_ID = "SELECT\n" +
            "   shipload.left_stamp AS left_stamp_departed,\n" +
            "   shipload.right_stamp AS right_stamp_departed,\n" +
            "   shipload.additional_stamp AS additional_stamp_departed,\n" +
            "   (SELECT shipload2.replacement_stamp FROM shipments shipload2\n" +
            "           INNER JOIN travel_logs tl2 ON tl2.load_id = shipload2.id\n" +
            "           INNER JOIN schedule_route sr2 ON sr2.id = tl2.schedule_route_id\n" +
            "           INNER JOIN schedule_route_destination srd2 ON srd2.schedule_route_id = sr2.id\n" +
            "               AND srd2.terminal_origin_id = tl2.terminal_origin_id AND srd2.terminal_destiny_id = tl2.terminal_destiny_id\n" +
            "           INNER JOIN config_destination cd2 ON cd2.config_route_id = sr2.config_route_id\n" +
            "               AND cd2.order_origin = 1\n" +
            "           WHERE sr2.id = sr.id\n" +
            "           LIMIT 1) AS replacement_stamp_departed,\n" +
            "    shipdownload.left_stamp AS left_stamp_arrived,\n" +
            "    shipdownload.right_stamp AS right_stamp_arrived,\n" +
            "    shipdownload.additional_stamp AS additional_stamp_arrived,\n" +
            "    CASE\n" +
            "       WHEN (SELECT MAX(cd2.order_destiny) FROM config_destination cd2 WHERE cd2.config_route_id = sr.config_route_id) > cd.order_destiny THEN\n" +
            "           (SELECT shipload2.replacement_stamp FROM shipments shipload2\n" +
            "           INNER JOIN travel_logs tl2 ON tl2.load_id = shipload2.id\n" +
            "           INNER JOIN schedule_route sr2 ON sr2.id = tl2.schedule_route_id\n" +
            "           INNER JOIN schedule_route_destination srd2 ON srd2.schedule_route_id = sr2.id\n" +
            "               AND srd2.terminal_origin_id = tl2.terminal_origin_id AND srd2.terminal_destiny_id = tl2.terminal_destiny_id\n" +
            "           INNER JOIN config_destination cd2 ON cd2.config_route_id = sr2.config_route_id\n" +
            "               AND cd2.order_origin = 1\n" +
            "           WHERE sr2.id = sr.id\n" +
            "           LIMIT 1)\n" +
            "       ELSE \n" +
            "           shipdownload.replacement_stamp\n" +
            "    END replacement_stamp_arrived\n" +
            "FROM travel_logs tl\n" +
            "LEFT JOIN shipments shipload ON shipload.id = tl.load_id\n" +
            "LEFT JOIN shipments shipdownload ON shipdownload.id = tl.download_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "WHERE \n" +
            "   tl.id = ?";

    private static final String QUERY_DAILY_LOGS_DETAIL_SHIPMENT_INFO = "SELECT\n" +
            "     ship.created_at,\n" +
            "     ship.updated_at,\n" +
            "     tl.origin,\n" +
            "     ship.driver_id,\n" +
            "     tl.terminal_origin_id,\n" +
            "     tl.terminal_destiny_id,\n" +
            "     ship.shipment_status,\n" +
            "     ship.total_tickets,\n" +
            "     ship.total_complements,\n" +
            "     ship.total_packages,\n" +
            "     sr.id AS schedule_route_id,\n" +
            "     cd.order_origin,\n" +
            "     cd.order_destiny\n" +
            " FROM shipments ship\n" +
            " LEFT JOIN travel_logs tl ON (tl.load_id = ship.id OR tl.download_id = ship.id)\n" +
            " LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id \n" +
            "   AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " WHERE ship.id = ?";

    private static final String QUERY_DAILY_LOGS_GET_TERMINALS_DESTINY_BY_SCHEDULE_ID = "SELECT DISTINCT\n" +
            "   cd.terminal_origin_id,\n" +
            "   bo.prefix AS prefix_origin,\n" +
            "   cd.terminal_destiny_id,\n" +
            "   bd.prefix AS prefix_destiny,\n" +
            "   cd.order_origin,\n" +
            "   cd.order_destiny\n" +
            " FROM schedule_route sr\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cd.terminal_destiny_id\n" +
            " WHERE sr.id = ?\n" +
            " AND cd.order_origin <= ? AND cd.order_destiny > ?; ";

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_TICKETS_BY_TERMINAL = "SELECT DISTINCT\n" +
            " shiptt.boarding_pass_ticket_id\n" +
            "FROM travel_logs tl \n" +
            "LEFT JOIN shipments ship ON ship.id = tl.load_id \n" +
            "LEFT JOIN shipments ship2 ON ship2.id = tl.download_id \n" +
            "LEFT JOIN shipments_ticket_tracking shiptt ON ship.id = shiptt.shipment_id\n" +
            "LEFT JOIN shipments_ticket_tracking shiptt2 ON ship2.id = shiptt2.shipment_id\n" +
            "LEFT JOIN boarding_pass_ticket bpt ON bpt.id = shiptt.boarding_pass_ticket_id\n" +
            "LEFT JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "WHERE sr.id = ? ";

    private static final String DOWNLOAD_TICKETS_TO_DOWNLOAD_PARAMS = " AND srd.terminal_destiny_id = ? AND srd.terminal_origin_id = ? ;";

    private static final String DOWNLOAD_TICKETS_TO_OTHER_DESTINY_PARAMS = " AND srd.terminal_destiny_id != ?\n" +
            " AND srd.terminal_origin_id = ?\n" +
            " AND cr.id = ?\n" +
            " AND cd.order_destiny > ?";

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_COMPLEMENTS_BY_TERMINAL = "SELECT DISTINCT\n" +
            " shipct.boarding_pass_complement_id\n" +
            "FROM travel_logs tl \n" +
            "LEFT JOIN shipments ship ON ship.id = tl.load_id \n" +
            "LEFT JOIN shipments_complement_tracking shipct ON ship.id = shipct.shipment_id\n" +
            "LEFT JOIN shipments ship2 ON ship2.id = tl.download_id \n" +
            "LEFT JOIN shipments_complement_tracking shipct2 ON ship2.id = shipct2.shipment_id\n" +
            "LEFT JOIN boarding_pass_complement bpc ON bpc.id = shipct.boarding_pass_complement_id\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = bpc.boarding_pass_id\n" +
            "LEFT JOIN boarding_pass_route bpr ON bpr.boarding_pass_id = bp.id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "WHERE sr.id = ? ";

    private static final String COMPLEMENTS_TO_DOWNLOAD_PARAMS = " AND srd.terminal_destiny_id = ? AND srd.terminal_origin_id = ?;";

    private static final String DOWNLOAD_COMPLEMENTS_TO_OTHER_DESTINY_PARAMS = " AND srd.terminal_destiny_id != ?\n" +
            " AND srd.terminal_origin_id = ?\n" +
            " AND cr.id = ?\n" +
            " AND cd.order_destiny > ?";

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_BY_TERMINAL = "SELECT DISTINCT\n" +
            " shippt.parcel_id,\n" +
            " COUNT(shippt.parcel_package_id) AS total_packages\n" +
            "FROM travel_logs tl \n" +
            "INNER JOIN shipments ship ON ship.id = tl.load_id \n" +
            "INNER JOIN shipments_parcel_package_tracking shippt ON ship.id = shippt.shipment_id\n" +
            "INNER JOIN parcels p ON p.id = shippt.parcel_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = p.terminal_origin_id AND srd.terminal_destiny_id = p.terminal_destiny_id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "WHERE \n" +
            " shippt.status = 'loaded'\n" +
            " AND sr.id = ? ";

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_TRANSHIPMENTS_BY_TERMINAL = "SELECT DISTINCT\n" +
            " shippt.parcel_id,\n" +
            " COUNT(shippt.parcel_package_id) AS total_packages,\n" +
            " p.terminal_origin_id,\n" +
            " bo.prefix AS terminal_origin_prefix,\n" +
            " p.terminal_destiny_id,\n" +
            " bdp.prefix AS terminal_destiny_prefix\n" +
            "FROM travel_logs tl \n" +
            "INNER JOIN shipments ship ON ship.id = tl.load_id \n" +
            "INNER JOIN shipments_parcel_package_tracking shippt ON ship.id = shippt.shipment_id\n" +
            "INNER JOIN parcels p ON p.id = shippt.parcel_id\n" +
            "INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND pth.schedule_route_destination_id = srd.id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = ?\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bdp ON bdp.id = p.terminal_destiny_id\n" +
            "WHERE \n" +
            " shippt.status = 'loaded'\n" +
            " AND sr.id = ?\n" +
            "AND (bd.receive_transhipments IS TRUE OR p.terminal_destiny_id = ?) \n" +
            //TODO : agregar status para mostrar solo los paquetes que vayan en curso de la ruta (escala)
            /*"AND p.parcel_status IN (\n" +
                PARCEL_STATUS.IN_TRANSIT.ordinal() + "," +
                PARCEL_STATUS.DOCUMENTED.ordinal() +
            "   ) \n" +*/
            "GROUP BY shippt.parcel_id, shippt.parcel_package_id;";

    private static final String DOWNLOAD_PARCELS_TO_DOWNLOAD_PARAMS = " AND p.terminal_destiny_id = ? AND p.terminal_origin_id = ? GROUP BY shippt.parcel_id;";

    private static final String DOWNLOAD_PARCELS_TO_OTHER_DESTINY_PARAMS = " AND srd.terminal_destiny_id != ?\n" +
            " AND srd.terminal_origin_id = ?\n" +
            " AND cr.id = ?\n" +
            " AND cd.order_destiny > ?\n" +
            " GROUP BY shippt.parcel_id;";

    private static final String QUERY_GET_DOWNLOAD_TERMINAL_ORIGINS = "SELECT DISTINCT\n" +
            "  bo.id, \n" +
            "  bo.prefix,\n" +
            "  bd.id AS terminal_destiny_id,\n" +
            "  cd.order_destiny\n" +
            " FROM config_route cr\n" +
            " INNER JOIN schedule_route sr ON sr.config_route_id = cr.id\n" +
            " LEFT JOIN config_destination cd ON cd.config_route_id = cr.id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cd.terminal_destiny_id\n" +
            " WHERE sr.id = ? \n" +
            " AND cd.order_origin < ? \n" +
            " AND cd.order_origin = cd.order_destiny - 1;";

    private static final String QUERY_GET_DAILY_LOGS_PARCELS_INCOMPLETE = "SELECT \n" +
            "\tparcels.id,\n" +
            "    parcels.parcel_tracking_code,\n" +
            "    parcels.waybill,\n" +
            "    (parcels.total_packages - SUM(IF(parcels.status = 'loaded', 1, 0))) AS not_loaded,\n" +
            "\tSUM(IF(parcels.status = 'downloaded', 1, 0)) AS 'packages_downloaded',\n" +
            "    (SUM(IF(parcels.status = 'loaded', 1, 0)) - SUM(IF(parcels.status = 'downloaded', 1, 0))) AS 'packages_not_downloaded',\n" +
            "    parcels.parcel_package_id,\n" +
            "    parcels.terminal_destiny_id,\n" +
            "    parcels.terminal_destiny_prefix\n" +
            "FROM (SELECT DISTINCT\n" +
            "\t p.id,\n" +
            "\t p.parcel_tracking_code,\n" +
            "\t p.waybill,\n" +
            "     p.total_packages,\n" +
            "\t shippt.parcel_package_id,\n" +
            "\t p.terminal_destiny_id,\n" +
            "\t shippt.status,\n" +
            "\t b.prefix AS terminal_destiny_prefix\n" +
            "\t FROM parcels p\n" +
            "\t INNER JOIN shipments_parcel_package_tracking shippt ON shippt.parcel_id = p.id\n" +
            "\t LEFT JOIN shipments ship ON ship.id = shippt.shipment_id\n" +
            "\t LEFT JOIN schedule_route sr ON sr.id = ship.schedule_route_id\n" +
            "\t LEFT JOIN branchoffice b ON b.id = p.terminal_destiny_id\n" +
            "\t LEFT JOIN parcels_packages pp ON pp.id = shippt.parcel_package_id\n" +
            "\t WHERE (p.parcel_status = 8 OR p.parcel_status = 10)\n" +
            "\t AND sr.id = ?\n" +
            "\t AND terminal_destiny_id = ?) AS parcels\n" +
            "     GROUP BY parcels.id;";

    private static final String QUERY_GET_DRIVERS_DAILY_LOGS_DETAIL = "SELECT \n" +
            "\tCONCAT(e.name, ' ', e.last_name) AS driver_name\n" +
            "FROM schedule_route_driver srdriver\n" +
            "INNER JOIN schedule_route dsr ON dsr.id = srdriver.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination dsrd ON dsrd.schedule_route_id = dsr.id\n" +
            "\tAND dsrd.terminal_origin_id = srdriver.terminal_origin_id\n" +
            "\tAND dsrd.terminal_destiny_id = srdriver.terminal_destiny_id\n" +
            "INNER JOIN config_destination dcd ON dcd.id = dsrd.config_destination_id\n" +
            "INNER JOIN employee e ON e.id = srdriver.employee_id\n" +
            "WHERE \n" +
            "\t? BETWEEN dcd.order_origin AND dcd.order_destiny\n" +
            "\tAND ? BETWEEN dcd.order_origin AND dcd.order_destiny\n" +
            "\tAND dsr.id = ?\n" +
            "GROUP BY e.name, e.last_name";

    private static final String QUERY_GET_TOTAL_TICKETS_DAILY_LOGS_LOAD = "SELECT\n" +
            "   stt.boarding_pass_ticket_id,\n" +
            "   bp.reservation_code,\n" +
            "   bpt.tracking_code,\n" +
            "   bpt.checkedin_at,\n" +
            "   CONCAT(bpp.first_name, ' ',bpp.last_name) AS passenger_full_name,\n" +
            "   (SELECT COUNT(id) FROM boarding_pass_complement WHERE boarding_pass_ticket_id = bpt.id AND complement_status != 1 AND shipping_type = 'baggage') AS total_complements,\n" +
            "   (SELECT IF(shipping_type != 'baggage', 0.0, COALESCE(SUM(weight), 0))  FROM boarding_pass_complement WHERE boarding_pass_ticket_id = bpt.id) AS total_weight,\n" +
            "   (SELECT COALESCE(SUM(height + width + length), 0) FROM boarding_pass_complement WHERE boarding_pass_ticket_id = bpt.id) AS total_linear_volume,\n" +
            "   (SELECT IF(shipping_type != 'baggage',0.0, COALESCE((SUM(height * width * length) / 1000000), 0))  FROM boarding_pass_complement WHERE boarding_pass_ticket_id = bpt.id)  AS total_volume,\n" +
            "   bd.prefix\n" +
            "FROM shipments_ticket_tracking stt\n" +
            "INNER JOIN shipments ship ON ship.id = stt.shipment_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN boarding_pass_ticket bpt ON bpt.id = stt.boarding_pass_ticket_id\n" +
            "INNER JOIN boarding_pass_passenger bpp ON bpp.id = bpt.boarding_pass_passenger_id\n" +
            "INNER JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "INNER JOIN boarding_pass bp ON bp.id = bpr.boarding_pass_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "    AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "WHERE sr.id = ?\n" +
            "AND stt.status IN ('loaded')\n" +
            "AND (cd.order_origin < ? AND cd.order_destiny >= ?) \n" ;

    private static final String QUERY_GET_TOTAL_COMPLEMENTS_DAILY_LOGS_LOAD = "SELECT\n" +
            "   sct.boarding_pass_complement_id,\n" +
            "   COALESCE(SUM(bpc.height + bpc.width + bpc.length), 0) AS volume_linear, \n" +
            "   COALESCE(SUM(bpc.height * bpc.width * bpc.length) / 1000000, 0) AS volume,\n" +
            "   COALESCE(SUM(bpc.weight), 0) AS weight\n" +
            "FROM shipments_complement_tracking sct    \n" +
            "INNER JOIN shipments ship ON ship.id = sct.shipment_id    \n" +
            "INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id    \n" +
            "INNER JOIN boarding_pass_complement bpc ON bpc.id = sct.boarding_pass_complement_id    \n" +
            "INNER JOIN boarding_pass bp ON bp.id = bpc.boarding_pass_id    \n" +
            "INNER JOIN boarding_pass_route bpr ON bpr.boarding_pass_id = bp.id    \n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id    \n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id    \n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id    \n" +
            "WHERE sr.id = ?\n" +
            "AND sct.status IN ('loaded')\n" +
            "AND (cd.order_origin < ? AND cd.order_destiny >= ?)\n";

    private static final String QUERY_GET_TOTAL_PACKAGES_DAILY_LOGS_LOAD = "SELECT \n" +
            "   sppt.parcel_package_id,\n" +
            "   sppt.parcel_id as parcel_id,\n" +
            "   tr.name trailer_name,\n" +
            "   tr.plate trailer_plate,\n" +
            "   str.Remolque_o_semirremolque trailer_type,\n" +
            "   str.Clave_tipo_remolque trailer_type_code,\n" +
            "   p.parcel_tracking_code,\n" +
            "   pp.created_at,\n" +
            "   pp.contains, \n" +
            "   pp.amount, \n" +
            "   p.total_packages,\n" +
            "   (pp.height +  pp.width + pp.length) AS linear_volume,\n"+
            "   ((pp.height * pp.width * pp.length) / 1000000) AS volume,\n"+
            "   pp.weight,\n" +
            "   bo.prefix AS terminal_origin_prefix,\n" +
            "   bd.prefix AS terminal_destiny_prefix,\n" +
            "   pp.is_old,\n" +
            "   (\n" +
            "       SELECT DISTINCT ppp.name_price\n" +
            "       FROM parcels_prepaid_detail ppd\n" +
            "       LEFT JOIN pp_price ppp ON ppp.id = ppd.price_id\n" +
            "       WHERE ppd.id = pp.parcel_prepaid_detail_id\n" +
            "       LIMIT 1\n" +
            "   ) AS pp_package_price_name\n" +
            "FROM shipments_parcel_package_tracking sppt    \n" +
            "INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "LEFT JOIN shipments_trailers shiptrai ON shiptrai.schedule_route_id = sr.id \n" +
            "LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "INNER JOIN config_route cr ON cr.id = sr.cONfig_route_id    \n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
            "LEFT JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "       AND (((srd.terminal_origin_id = p.terminal_origin_id\n" +
            "               OR srd.terminal_origin_id IN (\n" +
            "                   SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "                        WHERE bprc.of_branchoffice_id = p.terminal_origin_id AND bprc.status = 1)) AND  \n" +
            "           (srd.terminal_destiny_id = p.terminal_destiny_id \n" +
            "               OR srd.terminal_destiny_id IN (\n" +
            "                   (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc WHERE bprc.of_branchoffice_id = p.terminal_destiny_id AND bprc.status = 1)))\n" +
            "               ) \n" +
            "           OR (srd.id = pth.schedule_route_destination_id))" +
            "INNER JOIN config_destination cd ON cd.config_route_id = cr.id    \n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id    \n" +
            "INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "   LEFT JOIN (SELECT \n" +
            "                   st.schedule_route_id, st.trailer_id, st.transfer_trailer_id, st.action, st.latest_movement\n" +
            "               FROM shipments_trailers st\n" +
            "               INNER JOIN shipments ship2 ON ship2.id = st.shipment_id\n" +
            "               INNER JOIN schedule_route sr2 ON sr2.id = st.schedule_route_id\n" +
            "               INNER JOIN config_route cr2 ON cr2.id = sr2.config_route_id\n" +
            "               INNER JOIN config_destination cd2 ON cd2.config_route_id = cr2.id\n" +
            "                  AND cd2.terminal_origin_id = ship2.terminal_id AND cd2.order_destiny = cd2.order_origin + 1\n" +
            "               INNER JOIN schedule_route_destination srd2 ON srd2.schedule_route_id = st.schedule_route_id\n" +
            "                  AND srd2.terminal_origin_id = cd2.terminal_origin_id AND srd2.terminal_destiny_id = cd2.terminal_destiny_id\n" +
            "               WHERE st.trailer_id NOT IN \n" +
            "                   (SELECT \n" +
            "                       st2.trailer_id\n" +
            "                   FROM shipments_trailers st2\n" +
            "                   INNER JOIN shipments ship3 ON ship3.id = st2.shipment_id\n" +
            "                   INNER JOIN schedule_route sr3 ON sr3.id = st2.schedule_route_id\n" +
            "                   INNER JOIN config_route cr3 ON cr3.id = sr3.config_route_id\n" +
            "                   INNER JOIN config_destination cd3 ON cd3.config_route_id = cr3.id\n" +
            "                       AND cd3.terminal_origin_id = ship3.terminal_id AND cd3.order_destiny = cd3.order_origin + 1\n" +
            "                   INNER JOIN schedule_route_destination srd3 ON srd3.schedule_route_id = st2.schedule_route_id\n" +
            "                       AND srd3.terminal_origin_id = cd3.terminal_origin_id AND srd3.terminal_destiny_id = cd3.terminal_destiny_id\n" +
            "                   WHERE st2.schedule_route_id = st.schedule_route_id\n" +
            "                       AND st2.action IN ('release', 'release_transhipment')\n" +
            "                   AND (cd3.order_origin < ?))\n" +
            "       AND (cd2.order_origin < ?)) AS st ON st.schedule_route_id = sr.id\n" +
            "WHERE sr.id = ?\n" +
            "AND sppt.status IN ('loaded')\n" +
            "AND (cd.order_origin < ? AND cd.order_destiny >= ?)\n";

    private static final String QUERY_GET_TOTAL_PACKAGES_TRANSHIPMENTS_DAILY_LOGS_LOAD = "SELECT \n" +
            "   sppt.parcel_package_id,\n" +
            "   sppt.parcel_id as parcel_id,\n" +
            "   tr.name trailer_name,\n" +
            "   tr.plate trailer_plate,\n" +
            "   str.Remolque_o_semirremolque trailer_type,\n" +
            "   str.Clave_tipo_remolque trailer_type_code,\n" +
            "   p.parcel_tracking_code,\n" +
            "   pp.created_at,\n" +
            "   p.total_packages,\n" +
            "   COALESCE((pp.height +  pp.width + pp.length), 0) AS linear_volume,\n" +
            "   COALESCE(((pp.height * pp.width * pp.length) / 1000000), 0) AS volume,\n" +
            "   COALESCE(pp.weight, 0) AS weight,\n" +
            "   pp.contains,\n" +
            "   bo.prefix AS terminal_origin_prefix,\n" +
            "   bd.prefix AS terminal_destiny_prefix,\n" +
            "   pp.is_old,\n" +
            "   (\n" +
            "       SELECT DISTINCT ppp.name_price\n" +
            "       FROM parcels_prepaid_detail ppd\n" +
            "       LEFT JOIN pp_price ppp ON ppp.id = ppd.price_id\n" +
            "       WHERE ppd.id = pp.parcel_prepaid_detail_id\n" +
            "       LIMIT 1\n" +
            "   ) AS pp_package_price_name\n" +
            "FROM shipments_parcel_package_tracking sppt    \n" +
            "INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "INNER JOIN config_route cr ON cr.id = sr.cONfig_route_id    \n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
            "INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id    \n" +
            "   AND pth.schedule_route_destination_id = srd.id\n" +
            "INNER JOIN config_destination cd ON cd.config_route_id = cr.id    \n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id    \n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "WHERE sr.id = ?\n" +
            "AND sppt.status IN ('loaded') \n";

    private static final String QUERY_GET_TOTAL_PARCELS_DAILY_LOG_LOAD = "SELECT \n" +
            "   parcels.parcel_id, \n" +
            "   parcels.waybill, \n" +
            "   parcels.trailer_name, \n" +
            "   parcels.trailer_plate, \n" +
            "   parcels.trailer_type, \n" +
            "   parcels.trailer_type_code, \n" +
            "   parcels.parcel_tracking_code, \n" +
            "   parcels.count_total_packages, \n" +
            "   COUNT(parcels.parcel_package_id) AS total_packages,\n" +
            "   SUM(parcels.weight) AS packages_weight, \n" +
            "   SUM((parcels.height * parcels.width * parcels.length) / 1000000) AS packages_volume,\n" +
            "   parcels.created_at, \n" +
            "   parcels.terminal_origin_prefix, \n" +
            "   parcels.terminal_destiny_prefix,\n" +
            "   IF(parcels.parcel_transhipment_id IS NULL, 0, 1) is_transhipment,\n" +
            "   parcels.customer_id, \n" +
            "   parcels.customer_name, \n" +
            "   parcels.customer_company_nick_name \n" +
            "FROM (SELECT    \n" +
            "       sppt.parcel_id,\n" +
            "       sppt.parcel_package_id,\n" +
            "       tr.name trailer_name,\n" +
            "       tr.plate trailer_plate,\n" +
            "       str.Remolque_o_semirremolque trailer_type,\n" +
            "       str.Clave_tipo_remolque trailer_type_code,\n" +
            "       p.waybill,\n" +
            "       p.parcel_tracking_code,\n" +
            "       p.total_packages AS count_total_packages,\n" +
            "       pp.weight,\n" +
            "       pp.length,\n" +
            "       pp.height,\n" +
            "       pp.width,\n" +
            "       p.created_at,\n" +
            "       bo.prefix AS terminal_origin_prefix,\n" +
            "       bd.prefix AS terminal_destiny_prefix,\n" +
            "       pth.id AS parcel_transhipment_id,\n" +
            "       CONCAT(cu.first_name, ' ', cu.last_name) AS customer_name,\n" +
            "       cu.company_nick_name AS customer_company_nick_name,\n" +
            "       p.customer_id \n" +
            "   FROM shipments_parcel_package_tracking sppt    \n" +
            "   INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "   INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "   LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "   LEFT JOIN shipments_trailers shiptrai ON shiptrai.schedule_route_id = sr.id  \n" +
            "   LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "   INNER JOIN config_route cr ON cr.id = sr.config_route_id    \n" +
            "   INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
            "   LEFT JOIN customer cu ON p.customer_id = cu.id\n" +
            "   INNER JOIN parcels_packages pp ON pp.parcel_id = p.id AND pp.id = sppt.parcel_package_id\n" +
            "   LEFT JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "       AND (((srd.terminal_origin_id = p.terminal_origin_id\n" +
            "               OR srd.terminal_origin_id IN (\n" +
            "                   SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "                        WHERE bprc.of_branchoffice_id = p.terminal_origin_id AND bprc.status = 1)) AND  \n" +
            "           (srd.terminal_destiny_id = p.terminal_destiny_id \n" +
            "               OR srd.terminal_destiny_id IN (\n" +
            "                   (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
            "                       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id AND bprc.status = 1)))\n" +
            "               ) \n" +
            "           OR (srd.id = pth.schedule_route_destination_id))" +
            "   INNER JOIN config_destination cd ON cd.config_route_id = cr.id    \n" +
            "       AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id    \n" +
            "   INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "   INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "   LEFT JOIN (SELECT \n" +
            "                   st.schedule_route_id, st.trailer_id, st.transfer_trailer_id, st.action, st.latest_movement\n" +
            "               FROM shipments_trailers st\n" +
            "               INNER JOIN shipments ship2 ON ship2.id = st.shipment_id\n" +
            "               INNER JOIN schedule_route sr2 ON sr2.id = st.schedule_route_id\n" +
            "               INNER JOIN config_route cr2 ON cr2.id = sr2.config_route_id\n" +
            "               INNER JOIN config_destination cd2 ON cd2.config_route_id = cr2.id\n" +
            "                  AND cd2.terminal_origin_id = ship2.terminal_id AND cd2.order_destiny = cd2.order_origin + 1\n" +
            "               INNER JOIN schedule_route_destination srd2 ON srd2.schedule_route_id = st.schedule_route_id\n" +
            "                  AND srd2.terminal_origin_id = cd2.terminal_origin_id AND srd2.terminal_destiny_id = cd2.terminal_destiny_id\n" +
            "               WHERE st.trailer_id NOT IN \n" +
            "                   (SELECT \n" +
            "                       st2.trailer_id\n" +
            "                   FROM shipments_trailers st2\n" +
            "                   INNER JOIN shipments ship3 ON ship3.id = st2.shipment_id\n" +
            "                   INNER JOIN schedule_route sr3 ON sr3.id = st2.schedule_route_id\n" +
            "                   INNER JOIN config_route cr3 ON cr3.id = sr3.config_route_id\n" +
            "                   INNER JOIN config_destination cd3 ON cd3.config_route_id = cr3.id\n" +
            "                       AND cd3.terminal_origin_id = ship3.terminal_id AND cd3.order_destiny = cd3.order_origin + 1\n" +
            "                   INNER JOIN schedule_route_destination srd3 ON srd3.schedule_route_id = st2.schedule_route_id\n" +
            "                       AND srd3.terminal_origin_id = cd3.terminal_origin_id AND srd3.terminal_destiny_id = cd3.terminal_destiny_id\n" +
            "                   WHERE st2.schedule_route_id = st.schedule_route_id\n" +
            "                       AND st2.action IN ('release', 'release_transhipment')\n" +
            "                   AND (cd3.order_origin < ?))\n" +
            "       AND (cd2.order_origin < ?)) AS st ON st.schedule_route_id = sr.id\n" +
            "   WHERE sr.id = ? \n" +
            "   AND sppt.status IN ('loaded')    \n" +
            "   AND (cd.order_origin < ? AND cd.order_destiny >= ?)\n";

    private static final String QUERY_GET_TOTAL_PARCELS_TRANSHIPMENTS_DAILY_LOG_LOAD = "SELECT \n" +
            "   parcels.parcel_id, \n" +
            "   parcels.waybill, \n" +
            "   parcels.trailer_name, \n" +
            "   parcels.trailer_plate, \n" +
            "   parcels.trailer_type, \n" +
            "   parcels.trailer_type_code, \n" +
            "   parcels.parcel_tracking_code, \n" +
            "   parcels.count_total_packages, \n" +
            "   COUNT(parcels.parcel_package_id) AS total_packages,\n" +
            "   COALESCE(SUM(parcels.weight), 0) AS packages_weight, \n" +
            "   COALESCE(SUM((parcels.height * parcels.width * parcels.length) / 1000000), 0) AS packages_volume,\n" +
            "   parcels.created_at, \n" +
            "   parcels.terminal_origin_prefix, \n" +
            "   parcels.terminal_destiny_prefix,\n" +
            "   parcels.customer_id,\n" +
            "   parcels.customer_name,\n" +
            "   parcels.customer_company_nick_name \n" +
            "FROM (SELECT    \n" +
            "       sppt.parcel_id,\n" +
            "       sppt.parcel_package_id,\n" +
            "       tr.name trailer_name,\n" +
            "       tr.plate trailer_plate,\n" +
            "       str.Remolque_o_semirremolque trailer_type,\n" +
            "       str.Clave_tipo_remolque trailer_type_code,\n" +
            "       p.waybill,\n" +
            "       p.parcel_tracking_code,\n" +
            "       p.total_packages AS count_total_packages,\n" +
            "       pp.weight,\n" +
            "       pp.length,\n" +
            "       pp.height,\n" +
            "       pp.width,\n" +
            "       p.created_at,\n" +
            "       bo.prefix AS terminal_origin_prefix,\n" +
            "       bd.prefix AS terminal_destiny_prefix,\n" +
            "       p.customer_id,\n" +
            "       CONCAT(cu.first_name, ' ', cu.last_name) AS customer_name,\n" +
            "       cu.company_nick_name AS customer_company_nick_name\n" +
            "   FROM shipments_parcel_package_tracking sppt    \n" +
            "   INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "   INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "   LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "   LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "   INNER JOIN config_route cr ON cr.id = sr.config_route_id    \n" +
            "   INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
            "   LEFT JOIN customer cu ON p.customer_id = cu.id\n" +
            "   INNER JOIN parcels_packages pp ON pp.parcel_id = p.id AND pp.id = sppt.parcel_package_id\n" +
            "   INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
            "   INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "       AND pth.schedule_route_destination_id = srd.id\n" +
            "   INNER JOIN config_destination cd ON cd.config_route_id = cr.id    \n" +
            "       AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id    \n" +
            "   INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "   INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "   WHERE sr.id = ? \n" +
            "   AND sppt.status IN ('loaded') \n";
    //TODO : agregar condicion para mostrar solo los parcels en curso
            /*"   AND p.parcel_status IN (\n" +
                PARCEL_STATUS.IN_TRANSIT.ordinal() + "," +
                PARCEL_STATUS.DOCUMENTED.ordinal() +
            "   ) \n";*/
}
