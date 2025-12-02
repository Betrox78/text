package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.shipments.ShipmentsDBV.*;
import static service.commons.Constants.*;

public class DailyLogsDetail extends DBHandler<ShipmentsDBV> {

    public DailyLogsDetail(ShipmentsDBV dbVerticle) { super(dbVerticle); }

    @Override
    public void handle(Message<JsonObject> message){
        JsonObject body = message.body();
        Boolean isPre = body.getBoolean("is_pre");
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
                this.getLoadInfo(loadId, scheduleRouteId, cdOrderOrigin, cdOrderDestiny, !isPre).whenComplete((resultLoad, errorLoad) -> {
                    try {
                        if (errorLoad != null){
                            throw new Exception(errorLoad);
                        }
                        travelLogDetail.put(LOAD, resultLoad);
                        if (downloadId != null){
                            this.getDownloadInfo(downloadId, scheduleRouteId, configRouteId, cdOrderDestiny, segmentTerminalDestinyId).whenComplete((resultDownload, errorDownload) -> {
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
                       travelLogDetail.mergeIn(stamps);
                       future.complete(travelLogDetail);
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

    private CompletableFuture<JsonObject> getLoadInfo(Integer loadShipmentId, Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny, Boolean validateShipmentStatus){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.getShipmentInfo(loadShipmentId, validateShipmentStatus).whenComplete((loadInfo, errorLoad) -> {
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

                        JsonArray loadInfoDetail = new JsonArray();
                        loadInfo.put(_DETAIL, loadInfoDetail);

                        resultTerminals.forEach(t -> {
                            JsonObject terminal = (JsonObject) t;
                            task.add(this.getLoadElementsByTerminal(terminal, scheduleRouteId, loadInfo, loadInfoDetail));
                        });
                        CompletableFuture.allOf(task.toArray(new CompletableFuture[resultTerminals.size()])).whenComplete((resultElementsByTerminal, errorElementsByTerminal)->{
                            try {
                                if(errorElementsByTerminal != null){
                                    throw new Exception(errorElementsByTerminal);
                                }

                                this.getLoadParcelsAndPackagesTranshipments(loadInfo, scheduleRouteId, orderOrigin, orderDestiny).whenComplete((resLoadParcelsTranshipments, errorLoadParcelsTranshipments) -> {
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

    private CompletableFuture<JsonObject> getLoadParcelsAndPackagesTranshipments(JsonObject loadInfo, int scheduleRouteId, int orderOrigin, int orderDestiny) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        List<CompletableFuture<JsonObject>> task = new ArrayList<>();
        task.add(this.getParcelsTranshipmentsDailyLogsLoad(loadInfo, scheduleRouteId, orderOrigin, orderDestiny));
        task.add(this.getPackagesTranshipmentsDailyLogsLoad(loadInfo, scheduleRouteId, orderOrigin, orderDestiny));
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
                SHIPMENT_STATUS shipmentStatus = SHIPMENT_STATUS.fromValue(shipment.getInteger("shipment_status"));

                if (validateShipmentStatus && !shipmentStatus.equals(SHIPMENT_STATUS.CLOSE)) {
                    throw new Exception("The shipment has not been closed");
                }

                this.getTravelLogDrivers(scheduleRouteId, orderOrigin, orderDestiny).whenComplete((resDrivers, errDrivers) -> {
                    try {
                        if (errDrivers != null){
                            throw errDrivers;
                        }
                        shipment.put("driver_name", resDrivers.stream().map(d ->d.getString("driver_name")).collect(Collectors.joining(", ")));
                        shipment.put("second_driver_name", resDrivers.stream().map(d ->d.getString("second_driver_name")).collect(Collectors.joining(", ")));

                        future.complete(shipment);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } catch (Exception e){
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
        task.add(this.getParcelsDailyLogsLoad(terminal, loadInfoDetail, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny, false, false, false));
        task.add(this.getParcelsDailyLogsLoad(terminal, loadInfoDetail, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny, true, false, false));
        task.add(this.getPackagesDailyLogsLoad(terminal, loadInfoDetail, scheduleRouteId, terminalOriginId, terminalDestinyId, orderDestiny, false));

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

    public CompletableFuture<JsonObject> getPackagesDailyLogsLoad(JsonObject dailyLog, JsonArray additionalTerminals, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny, boolean includeTranshipments){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_PACKAGES_DAILY_LOGS_LOAD);
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

        QUERY.append("GROUP BY sppt.parcel_package_id");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> packages = reply.result().getRows();

                if (Objects.nonNull(additionalTerminals)) {
                    processPackagesAdditionalTerminals(packages, additionalTerminals, terminalDestinyId);
                }

                dailyLog.put(_PACKAGES, packages);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void processPackagesAdditionalTerminals(List<JsonObject> packages, JsonArray additionalTerminals, Integer terminalDestinyId) {
        List<JsonObject> packagesToRemove = new ArrayList<>();
        for (JsonObject pack : packages) {
            Integer packDestinyId = pack.getInteger(_TERMINAL_DESTINY_ID);
            if (!packDestinyId.equals(terminalDestinyId)) {
                for (int j = 0; j < additionalTerminals.size(); j++) {
                    JsonObject currentAdditionalTerminal = additionalTerminals.getJsonObject(j);
                    if (currentAdditionalTerminal.getInteger(_TERMINAL_DESTINY_ID).equals(packDestinyId)) {
                        if (!currentAdditionalTerminal.containsKey(_PACKAGES)) {
                            currentAdditionalTerminal.put(_PACKAGES, new JsonArray());
                        }
                        currentAdditionalTerminal.getJsonArray(_PACKAGES).add(pack);
                        packagesToRemove.add(pack);
                        break;
                    }
                }
            }
        }
        for (JsonObject ptr : packagesToRemove) {
            packages.remove(ptr);
        }
    }

    public CompletableFuture<JsonObject> getPackagesTranshipmentsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_TOTAL_PACKAGES_TRANSHIPMENTS_DAILY_LOGS_LOAD,
                new JsonArray()
                        .add(orderDestiny).add(orderDestiny)
                        .add(scheduleRouteId)
                        .add(orderOrigin).add(orderDestiny), reply -> {
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

    public CompletableFuture<JsonObject> getParcelsDailyLogsLoad(JsonObject dailyLog, JsonArray additionalTerminals, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny, boolean isPartial, boolean includeTranshipments, boolean includeParcelsAmount){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        StringBuilder QUERY = new StringBuilder(QUERY_GET_TOTAL_PARCELS_DAILY_LOG_LOAD);
        JsonArray params = new JsonArray()
                .add(orderDestiny).add(orderDestiny)
                .add(scheduleRouteId)
                .add(orderDestiny).add(orderDestiny);

        if(terminalOriginId != null){
            QUERY.append(" AND srd.terminal_origin_id = ?\n");
            params.add(terminalOriginId);
        }

        if(terminalDestinyId != null){
            QUERY.append(" AND srd.terminal_destiny_id = ? \n");
            params.add(terminalDestinyId);
        }

        if (!includeTranshipments) {
            QUERY.append(" AND pth.id IS NULL \n");
        }

        String parcelsAmountReplace = includeParcelsAmount ? SELECT_PARCELS_AMOUNT : "";
        String tempQuery = QUERY.toString().replace("{TOTAL_AMOUNT_SQL}", parcelsAmountReplace);
        QUERY = new StringBuilder(tempQuery);

        QUERY.append(" GROUP BY sppt.parcel_id, sppt.parcel_package_id) as parcels GROUP BY parcels.parcel_id \n ");
        QUERY.append(isPartial ?
                " HAVING parcels.count_total_packages > COUNT(parcels.parcel_package_id)\n"
                : "");

        this.dbClient.queryWithParams(QUERY.toString(), params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> parcels = reply.result().getRows();

                if (Objects.nonNull(additionalTerminals)) {
                    processParcelsAdditionalTerminals(parcels, dailyLog, additionalTerminals, terminalDestinyId, isPartial);
                }

                dailyLog.put(isPartial ? PARCELS_PARTIALS : PARCELS, parcels);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void processParcelsAdditionalTerminals(List<JsonObject> parcels, JsonObject dailyLog, JsonArray additionalTerminals, Integer terminalDestinyId, boolean isPartial) {
        List<JsonObject> parcelsToRemove = new ArrayList<>();
        for (JsonObject parcel : parcels) {
            Integer parcelDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
            String parcelPrefixDestiny = parcel.getString(_TERMINAL_DESTINY_PREFIX);
            JsonObject additionalTerminal = dailyLog.copy()
                    .put(_TERMINAL_DESTINY_ID, parcelDestinyId)
                    .put(_TERMINAL_DESTINY_PREFIX, parcelPrefixDestiny)
                    .put("prefix_destiny", parcelPrefixDestiny);

            boolean containsAT = !additionalTerminals.isEmpty() && additionalTerminals.stream()
                    .map(JsonObject::mapFrom)
                    .anyMatch(at ->
                            at.getInteger(_TERMINAL_DESTINY_ID).equals(parcelDestinyId));

            if (!parcelDestinyId.equals(terminalDestinyId) && !containsAT) {
                additionalTerminal.put(isPartial ? PARCELS_PARTIALS : PARCELS, new JsonArray().add(parcel));
                additionalTerminals.add(additionalTerminal);
                parcelsToRemove.add(parcel);
            } else if (!parcelDestinyId.equals(terminalDestinyId)) {
                for (int j = 0; j < additionalTerminals.size(); j++) {
                    JsonObject currentAdditionalTerminal = additionalTerminals.getJsonObject(j);
                    if (currentAdditionalTerminal.getInteger(_TERMINAL_DESTINY_ID).equals(parcelDestinyId)) {
                        currentAdditionalTerminal.getJsonArray(isPartial ? PARCELS_PARTIALS : PARCELS)
                                .add(parcel);
                        parcelsToRemove.add(parcel);
                        break;
                    }
                }
            }
        }
        for (JsonObject ptr : parcelsToRemove) {
            parcels.remove(ptr);
        }
    }

    public CompletableFuture<JsonObject> getParcelsTranshipmentsDailyLogsLoad(JsonObject dailyLog, Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_TOTAL_PARCELS_TRANSHIPMENTS_DAILY_LOG_LOAD,
                new JsonArray().add(orderDestiny).add(orderDestiny)
                        .add(scheduleRouteId)
                        .add(orderOrigin).add(orderDestiny), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> parcelsTranshipments = reply.result().getRows();

                dailyLog.put(PARCELS_TRANSHIPMENTS, parcelsTranshipments);
                future.complete(dailyLog);

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getDownloadInfo(Integer downloadShipmentId, Integer scheduleRouteId, Integer configRouteId, Integer cdOrderDestiny, Integer segmentTerminalDestinyId){
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
                            downloadInfo.put(_DETAIL, new JsonArray());
                            resultTerminals.forEach(t -> {
                                JsonObject terminal = (JsonObject) t;
                                task.add(this.getDownloadElementsByTerminal(terminal, scheduleRouteId, configRouteId, segmentTerminalDestinyId, cdOrderDestiny, downloadInfo.getJsonArray(_DETAIL)));
                            });
                            CompletableFuture.allOf(task.toArray(new CompletableFuture[resultTerminals.size()])).whenComplete((resultElementsByTerminal, errorElementsByTerminal)->{
                                try {
                                    if(errorElementsByTerminal != null){
                                        throw new Exception(errorElementsByTerminal);
                                    }
                                    this.getParcelsTranshipmentsDailyLogsDownload(scheduleRouteId, segmentTerminalDestinyId, cdOrderDestiny).whenComplete((resultParcelsTranshipments, errorParcelsTranshipments) -> {
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
        tasks.add(this.getParcelsDailyLogsDownload(terminal, scheduleRouteId, terminalOriginId, segmentTerminalDestinyId, orderDestiny));
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

    private CompletableFuture<JsonObject> getParcelsDailyLogsDownload(JsonObject dailyLog, Integer scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer orderDestiny){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(orderDestiny).add(orderDestiny)
                .add(scheduleRouteId)
                .add(terminalDestinyId).add(terminalOriginId);

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

    private CompletableFuture<List<JsonObject>> getParcelsTranshipmentsDailyLogsDownload(Integer scheduleRouteId, Integer terminalDestinyId, Integer orderDestiny){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(terminalDestinyId)
                .add(orderDestiny).add(orderDestiny)
                .add(scheduleRouteId)
                .add(terminalDestinyId);

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
        JsonArray params = new JsonArray()
                .add(orderDestiny).add(orderDestiny)
                .add(scheduleRouteId)
                .add(terminalDestinyId).add(terminalOriginId)
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
            "     tl.status \n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = tl.schedule_route_id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            " LEFT JOIN branchoffice segbo ON segbo.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice segbd ON segbd.id = srd.terminal_destiny_id\n" +
            " LEFT JOIN branchoffice destiny ON destiny.id = tl.terminal_destiny_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN config_vehicle cv ON cv.id = v.config_vehicle_id\n" +
            " WHERE tl.id = ?;";

    private static final String QUERY_DAILY_LOGS_STAMPS_BY_TRAVEL_LOG_ID = "SELECT\n" +
            "   ta.name AS trailer_name_departed,\n" +
            "   shipload.left_stamp AS left_stamp_departed,\n" +
            "   shipload.right_stamp AS right_stamp_departed,\n" +
            "   shipload.additional_stamp AS additional_stamp_departed,\n" +
            "   shipload.replacement_stamp AS replacement_stamp_departed,\n" +
            "   shipload.fifth_stamp AS fifth_stamp_departed,\n" +
            "   shipload.sixth_stamp AS sixth_stamp_departed,\n" +
            "   sta.name AS second_trailer_name_departed,\n" +
            "   shipload.second_left_stamp AS second_left_stamp_departed,\n" +
            "   shipload.second_right_stamp AS second_right_stamp_departed,\n" +
            "   shipload.second_additional_stamp AS second_additional_stamp_departed,\n" +
            "   shipload.second_replacement_stamp AS second_replacement_stamp_departed,\n" +
            "    shipload.second_fifth_stamp AS second_fifth_stamp_departed,\n" +
            "    shipload.second_sixth_stamp AS second_sixth_stamp_departed,\n" +
            "    tdw.name AS trailer_name_arrived,\n" +
            "    shipdownload.left_stamp AS left_stamp_arrived,\n" +
            "    shipdownload.right_stamp AS right_stamp_arrived,\n" +
            "    shipdownload.additional_stamp AS additional_stamp_arrived,\n" +
            "    shipdownload.replacement_stamp AS replacement_stamp_arrived,\n" +
            "    shipdownload.fifth_stamp AS fifth_stamp_arrived,\n" +
            "    shipdownload.sixth_stamp AS sixth_stamp_arrived,\n" +
            "    stdw.name AS second_trailer_name_arrived,\n" +
            "    shipdownload.second_left_stamp AS second_left_stamp_arrived,\n" +
            "    shipdownload.second_right_stamp AS second_right_stamp_arrived,\n" +
            "    shipdownload.second_additional_stamp AS second_additional_stamp_arrived,\n" +
            "    shipdownload.second_replacement_stamp AS second_replacement_stamp_arrived,\n" +
            "    shipdownload.second_fifth_stamp AS second_fifth_stamp_arrived,\n" +
            "    shipdownload.second_sixth_stamp AS second_sixth_stamp_arrived\n" +
            "FROM travel_logs tl\n" +
            "LEFT JOIN shipments shipload ON shipload.id = tl.load_id\n" +
            "LEFT JOIN shipments shipdownload ON shipdownload.id = tl.download_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "LEFT JOIN trailers ta ON ta.id = shipload.trailer_id\n" +
            "LEFT JOIN trailers sta ON sta.id = shipload.second_trailer_id\n" +
            "LEFT JOIN trailers tdw ON tdw.id = shipdownload.trailer_id\n" +
            "LEFT JOIN trailers stdw ON stdw.id = shipdownload.second_trailer_id\n" +
            "WHERE \n" +
            "   tl.id = ?;";

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

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_BY_TERMINAL = "SELECT \n" +
            " shippt.parcel_id,\n" +
            " tr.name trailer_name,\n" +
            " str.Remolque_o_semirremolque trailer_type,\n" +
            " COUNT(shippt.parcel_id) AS total_packages\n" +
            "FROM travel_logs tl \n" +
            "INNER JOIN shipments ship ON ship.id = tl.load_id \n" +
            "INNER JOIN shipments_parcel_package_tracking shippt ON ship.id = shippt.shipment_id\n" +
            "INNER JOIN parcels p ON p.id = shippt.parcel_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "LEFT JOIN trailers tr ON tr.id = shippt.trailer_id  \n" +
            "LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "       AND ((srd.terminal_origin_id = p.terminal_origin_id AND \n" +
            "       (srd.terminal_destiny_id = p.terminal_destiny_id \n" +
            "           OR srd.terminal_destiny_id IN (\n" +
            "               (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc WHERE bprc.of_branchoffice_id = p.terminal_destiny_id AND bprc.status = 1)))\n" +
            "            )) \n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
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
            "WHERE sr.id = ? \n" +
            "AND\n" +
            "   CASE WHEN (st.trailer_id IS NULL)\n" +
            "       THEN shippt.status = 'loaded'\n" +
            "    ELSE \n" +
            "       shippt.trailer_id = st.trailer_id\n" +
            "END \n";

    private static final String QUERY_GET_DAILY_LOGS_DETAIL_PARCELS_TRANSHIPMENTS_BY_TERMINAL = "SELECT DISTINCT\n" +
            " shippt.parcel_id,\n" +
            " tr.name trailer_name,\n" +
            " str.Remolque_o_semirremolque trailer_type,\n" +
            " COUNT(shippt.parcel_package_id) AS total_packages,\n" +
            " p.terminal_origin_id,\n" +
            " bo.prefix AS terminal_origin_prefix,\n" +
            " p.terminal_destiny_id,\n" +
            " bdp.prefix AS terminal_destiny_prefix\n" +
            "FROM travel_logs tl \n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN shipments ship ON ship.id = tl.load_id \n" +
            "   AND ship.schedule_route_id = sr.id \n" +
            "INNER JOIN shipments_parcel_package_tracking shippt ON ship.id = shippt.shipment_id\n" +
            "   AND shippt.status IN ('loaded', 'transfer') AND shippt.latest_movement IS TRUE\n" +
            "INNER JOIN parcels p ON p.id = shippt.parcel_id\n" +
            "INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id\n" +
            "LEFT JOIN trailers tr ON tr.id = shippt.trailer_id  \n" +
            "LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND pth.schedule_route_destination_id = srd.id\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = ?\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bdp ON bdp.id = p.terminal_destiny_id\n" +
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
            "AND\n" +
            "   CASE WHEN (st.trailer_id IS NULL)\n" +
            "       THEN shippt.status = 'loaded'\n" +
            "    ELSE \n" +
            "       shippt.trailer_id = st.trailer_id\n" +
            "END \n" +
            "AND (bd.receive_transhipments IS TRUE OR p.terminal_destiny_id = ?) \n" +
            "GROUP BY shippt.parcel_id, shippt.parcel_package_id;";

    private static final String DOWNLOAD_PARCELS_TO_DOWNLOAD_PARAMS = " AND srd.terminal_destiny_id = ? AND srd.terminal_origin_id = ? GROUP BY shippt.parcel_id;";

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
            "    parcels.id,\n" +
            "    parcels.parcel_tracking_code,\n" +
            "    parcels.waybill,\n" +
            "    (parcels.total_packages - SUM(IF(((parcels.status IN ('loaded') AND parcels.trailer_id IS NULL) \n" +
            "            OR (parcels.status IN ('loaded', 'transfer') AND parcels.latest_movement IS TRUE)), 1, 0))) AS not_loaded,\n" +
            //"    (parcels.total_packages - SUM(IF(parcels.status = 'loaded', 1, 0))) AS not_loaded,\n" +
            "    SUM(IF(parcels.status = 'downloaded', 1, 0)) AS 'packages_downloaded',\n" +
            "    (SUM(IF(((parcels.status IN ('loaded') AND parcels.trailer_id IS NULL) \n" +
            "            OR (parcels.status IN ('loaded', 'transfer') AND parcels.latest_movement IS TRUE)), 1, 0)) - SUM(IF(parcels.status = 'downloaded', 1, 0))) AS 'packages_not_downloaded',\n" +
            //"    (SUM(IF(parcels.status = 'loaded', 1, 0)) - SUM(IF(parcels.status = 'downloaded', 1, 0))) AS 'packages_not_downloaded',\n" +
            "    parcels.parcel_package_id,\n" +
            "    parcels.terminal_destiny_id,\n" +
            "    parcels.terminal_destiny_prefix\n" +
            "FROM (SELECT DISTINCT\n" +
            "\t p.id,\n" +
            "\t p.parcel_tracking_code,\n" +
            "\t p.waybill,\n" +
            "     p.total_packages,\n" +
            "\t shippt.parcel_package_id,\n" +
            "\t shippt.latest_movement,\n" +
            "\t p.terminal_destiny_id,\n" +
            "\t shippt.status,\n" +
            "\t shippt.trailer_id,\n" +
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
    //TODO agrupar por remolque

    private static final String QUERY_GET_DRIVERS_DAILY_LOGS_DETAIL = "SELECT \n" +
            "\tCONCAT(e.name, ' ', e.last_name) AS driver_name,\n" +
            "\tCONCAT(se.name, ' ', se.last_name) AS second_driver_name\n" +
            "FROM schedule_route_driver srdriver\n" +
            "INNER JOIN schedule_route dsr ON dsr.id = srdriver.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination dsrd ON dsrd.schedule_route_id = dsr.id\n" +
            "\tAND dsrd.terminal_origin_id = srdriver.terminal_origin_id\n" +
            "\tAND dsrd.terminal_destiny_id = srdriver.terminal_destiny_id\n" +
            "INNER JOIN config_destination dcd ON dcd.id = dsrd.config_destination_id\n" +
            "INNER JOIN employee e ON e.id = srdriver.employee_id\n" +
            "LEFT JOIN employee se ON se.id = srdriver.second_employee_id\n" +
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
            "   tr.name trailer_name,\n" +
            "   str.Remolque_o_semirremolque trailer_type,\n" +
            "   p.parcel_tracking_code,\n" +
            "   pp.created_at,\n" +
            "   p.total_packages,\n" +
            "   (pp.height +  pp.width + pp.length) AS linear_volume,\n"+
            "   ((pp.height * pp.width * pp.length) / 1000000) AS volume,\n"+
            "   pp.weight,\n" +
            "   bo.prefix AS terminal_origin_prefix,\n" +
            "   bd.id AS terminal_destiny_id,\n" +
            "   bd.prefix AS terminal_destiny_prefix\n" +
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
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
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
            "AND\n" +
            "   CASE WHEN (shiptrai.id IS NULL)\n" +
            "       THEN sppt.status = 'loaded'\n" +
            "    ELSE \n" +
            "       sppt.trailer_id = st.trailer_id\n" +
            "END\n" +
            "AND (cd.order_origin < ? AND cd.order_destiny >= ?)\n";

    private static final String QUERY_GET_TOTAL_PACKAGES_TRANSHIPMENTS_DAILY_LOGS_LOAD = "SELECT \n" +
            "   sppt.parcel_package_id,\n" +
            "   tr.name trailer_name,\n" +
            "   str.Remolque_o_semirremolque trailer_type,\n" +
            "   p.parcel_tracking_code,\n" +
            "   pp.created_at,\n" +
            "   p.total_packages,\n" +
            "   COALESCE((pp.height +  pp.width + pp.length), 0) AS linear_volume,\n" +
            "   COALESCE(((pp.height * pp.width * pp.length) / 1000000), 0) AS volume,\n" +
            "   COALESCE(pp.weight, 0) AS weight,\n" +
            "   bo.prefix AS terminal_origin_prefix,\n" +
            "   bd.prefix AS terminal_destiny_prefix,\n" +
            "   cd.order_origin,\n" +
            "   cd.order_destiny\n" +
            "FROM shipments_parcel_package_tracking sppt    \n" +
            "INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "LEFT JOIN shipments_trailers shiptrai ON shiptrai.schedule_route_id = sr.id  \n" +
            "LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "INNER JOIN config_route cr ON cr.id = sr.cONfig_route_id    \n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
            "INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
//            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id    \n" +
//            "   AND pth.schedule_route_destination_id = srd.id\n" +
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
            "INNER JOIN config_destination cd ON cd.config_route_id = cr.id    \n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id    \n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
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
            "AND\n" +
            "   CASE WHEN (shiptrai.id IS NULL)\n" +
            "       THEN sppt.status = 'loaded'\n" +
            "    ELSE \n" +
            "       sppt.trailer_id = st.trailer_id\n" +
            "END\n" +
            "GROUP BY sppt.parcel_package_id\n" +
            "HAVING cd.order_origin <= ? AND cd.order_destiny >= ?;";

    private static final String QUERY_GET_TOTAL_PARCELS_DAILY_LOG_LOAD = "SELECT \n" +
            "   parcels.parcel_id, \n" +
            "   parcels.waybill, \n" +
            "   parcels.trailer_name, \n" +
            "   parcels.trailer_type, \n" +
            "   parcels.parcel_tracking_code, \n" +
            "   parcels.count_total_packages, \n" +
            "   COUNT(parcels.parcel_package_id) AS total_packages,\n" +
            "   SUM(parcels.weight) AS packages_weight, \n" +
            "   SUM((parcels.height * parcels.width * parcels.length) / 1000000) AS packages_volume,\n" +
            "   parcels.created_at, \n" +
            "   parcels.terminal_origin_prefix, \n" +
            "   parcels.terminal_destiny_id, \n" +
            "   parcels.terminal_destiny_prefix,\n" +
            "   IF(parcels.parcel_transhipment_id IS NULL, 0, 1) is_transhipment\n" +
            "   {TOTAL_AMOUNT_SQL} \n" +
            "FROM (SELECT    \n" +
            "       sppt.parcel_id,\n" +
            "       sppt.parcel_package_id,\n" +
            "       tr.name trailer_name,\n" +
            "       str.Remolque_o_semirremolque trailer_type,\n" +
            "       p.waybill,\n" +
            "       p.parcel_tracking_code,\n" +
            "       p.total_packages AS count_total_packages,\n" +
            "       pp.weight,\n" +
            "       pp.length,\n" +
            "       pp.height,\n" +
            "       pp.width,\n" +
            "       p.created_at,\n" +
            "       bo.prefix AS terminal_origin_prefix,\n" +
            "       bd.id AS terminal_destiny_id,\n" +
            "       bd.prefix AS terminal_destiny_prefix,\n" +
            "       pth.id AS parcel_transhipment_id,\n" +
            "       p.total_amount \n" +
            "   FROM shipments_parcel_package_tracking sppt    \n" +
            "   INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "   INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "   LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "   LEFT JOIN shipments_trailers shiptrai ON shiptrai.schedule_route_id = sr.id  \n" +
            "   LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "   INNER JOIN config_route cr ON cr.id = sr.config_route_id    \n" +
            "   INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
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
            "   INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
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
            "AND\n" +
            "   CASE WHEN (shiptrai.id IS NULL)\n" +
            "       THEN sppt.status = 'loaded'\n" +
            "    ELSE \n" +
            "       sppt.trailer_id = st.trailer_id\n" +
            "END\n" +
            "   AND (cd.order_origin < ? AND cd.order_destiny >= ?)\n";

    private static final String QUERY_GET_TOTAL_PARCELS_TRANSHIPMENTS_DAILY_LOG_LOAD = "SELECT \n" +
            "   parcels.parcel_id, \n" +
            "   parcels.waybill, \n" +
            "   parcels.trailer_name, \n" +
            "   parcels.trailer_type, \n" +
            "   parcels.waybill, \n" +
            "   parcels.parcel_tracking_code, \n" +
            "   parcels.count_total_packages, \n" +
            "   COUNT(parcels.parcel_package_id) AS total_packages,\n" +
            "   COALESCE(SUM(parcels.weight), 0) AS packages_weight, \n" +
            "   COALESCE(SUM((parcels.height * parcels.width * parcels.length) / 1000000), 0) AS packages_volume,\n" +
            "   parcels.created_at, \n" +
            "   parcels.terminal_origin_prefix, \n" +
            "   parcels.terminal_destiny_prefix,\n" +
            "   parcels.order_origin,\n" +
            "   parcels.order_destiny\n" +
            "FROM (SELECT    \n" +
            "       sppt.parcel_id,\n" +
            "       sppt.parcel_package_id,\n" +
            "       tr.name trailer_name,\n" +
            "       str.Remolque_o_semirremolque trailer_type,\n" +
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
            "       cd.order_destiny,\n" +
            "       cd.order_origin\n" +
            "   FROM shipments_parcel_package_tracking sppt    \n" +
            "   INNER JOIN shipments ship ON ship.id = sppt.shipment_id    \n" +
            "   INNER JOIN schedule_route sr ON sr.id = ship.schedule_route_id    \n" +
            "   LEFT JOIN trailers tr ON tr.id = sppt.trailer_id  \n" +
            "   LEFT JOIN shipments_trailers shiptrai ON shiptrai.schedule_route_id = sr.id  \n" +
            "   LEFT JOIN c_SubTipoRem str ON str.id = tr.c_SubTipoRem_id  \n" +
            "   INNER JOIN config_route cr ON cr.id = sr.config_route_id    \n" +
            "   INNER JOIN parcels p ON p.id = sppt.parcel_id    \n" +
            "   INNER JOIN parcels_packages pp ON pp.parcel_id = p.id AND pp.id = sppt.parcel_package_id\n" +
            "   INNER JOIN parcels_transhipments_history pth ON pth.parcel_id = p.id AND pth.parcel_package_id = pp.id\n" +
//            "   INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
//            "       AND pth.schedule_route_destination_id = srd.id\n" +
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
            "   INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
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
            "AND\n" +
            "   CASE WHEN (shiptrai.id IS NULL)\n" +
            "       THEN sppt.status = 'loaded'\n" +
            "    ELSE \n" +
            "       sppt.trailer_id = st.trailer_id\n" +
            "END\n" +
            "   GROUP BY sppt.parcel_id, sppt.parcel_package_id) as parcels GROUP BY parcels.parcel_id \n" +
            "   HAVING parcels.order_origin <= ? AND parcels.order_destiny >= ?\n";

    private final static String QUERY_GET_HITCHED_TRAILERS = "SELECT DISTINCT\n" +
            "   t.id,\n" +
            "   t.name,\n" +
            "   t.plate,\n" +
            "   tr.Clave_tipo_remolque,\n" +
            "   tr.Remolque_o_semirremolque\n" +
            "FROM shipments_trailers st\n" +
            "INNER JOIN shipments s ON s.id = st.shipment_id\n" +
            "INNER JOIN schedule_route sr ON sr.id = st.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.terminal_origin_id = s.terminal_id\n" +
            "INNER JOIN config_destination cd ON cd.config_route_id = sr.config_route_id\n" +
            "   AND cd.terminal_origin_id = srd.terminal_origin_id AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "INNER JOIN trailers t ON t.id = st.trailer_id\n" +
            "INNER JOIN c_SubTipoRem tr ON tr.id = t.c_SubTipoRem_id\n" +
            "WHERE st.schedule_route_id = ?\n" +
            "AND ? BETWEEN cd.order_origin AND cd.order_destiny\n" +
            "AND ? BETWEEN cd.order_origin AND cd.order_destiny\n" +
            "AND st.trailer_id NOT IN (SELECT st2.trailer_id FROM shipments_trailers st2 \n" +
            "   WHERE st2.schedule_route_id = st.schedule_route_id \n" +
            "       AND ((st2.trailer_id = st.trailer_id AND st2.action IN ('release', 'release_transhipment'))\n" +
            "           OR (st.trailer_id IN (SELECT st3.transfer_trailer_id FROM shipments_trailers st3\n" +
            "               WHERE st3.schedule_route_id = st.schedule_route_id\n" +
            "               AND st3.transfer_trailer_id = st.trailer_id))));";

    private static final String SELECT_PARCELS_AMOUNT = ", CASE \n" +
            "    WHEN LEFT(parcels.parcel_tracking_code, 3) = 'GPP' THEN\n" +
            "        (SELECT pp.total_amount / pp.total_count_guipp\n" +
            "        FROM parcels_prepaid_detail ppd\n" +
            "        INNER JOIN parcels_prepaid pp ON pp.id = ppd.parcel_prepaid_id\n" +
            "        WHERE ppd.guiapp_code = parcels.parcel_tracking_code\n" +
            "        LIMIT 1)\n" +
            "    ELSE parcels.total_amount\n" +
            "END AS total_amount";
}
