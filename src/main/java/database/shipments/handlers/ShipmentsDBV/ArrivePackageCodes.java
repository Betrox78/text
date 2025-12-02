package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsPackagesTrackingDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.routes.handlers.TravelTrackingDBV.UtilsTravel;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static service.commons.Constants.*;

public class ArrivePackageCodes extends DBHandler<ShipmentsDBV> {

    public ArrivePackageCodes(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                Integer terminalId = body.getInteger(TERMINAL_ID);
                Integer scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
                Integer createdBy = body.getInteger(CREATED_BY);

               this.updateParcelsArrivedLocated(conn, scheduleRouteId, terminalId, createdBy).whenComplete((resUPAL, errUPAL) -> {
                   try {
                       if (errUPAL != null) {
                           throw errUPAL;
                       }
                       this.commit(conn, message, new JsonObject().put("success", true));
                   } catch (Throwable t) {
                       this.rollback(conn, t, message);
                   }
               });

            } catch (Throwable t) {
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<Boolean> updateParcelsArrivedLocated(SQLConnection conn, Integer scheduleRouteId, Integer terminalId, Integer userId){
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

                execArrivalMovements(conn, parcels, terminalId, userId).whenComplete((movements, errorMovements) -> {
                    try {
                        if (errorMovements != null) {
                            throw errorMovements;
                        }

                        future.complete(true);

                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> execArrivalMovements(SQLConnection conn, JsonArray parcels, Integer terminalId, Integer userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture.allOf(parcels.stream()
                        .map(parcel -> arrivalUpdates(conn, (JsonObject) parcel, terminalId, userId))
                        .toArray(CompletableFuture[]::new))
                .whenComplete((s, t) -> {
                    if (t != null) {
                        future.completeExceptionally(t);
                    } else {
                        future.complete(true);
                    }
                });

        return future;

    }

    private CompletableFuture<Boolean> arrivalUpdates(SQLConnection conn, JsonObject parcel, Integer terminalId, Integer userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<CompletableFuture<Boolean>> execQueryList = new ArrayList<>();
        Integer parcelId = parcel.getInteger("parcel_id"); //
        Integer totalPackages = parcel.getInteger("total_packages");
        Integer terminalDestinyId = parcel.getInteger(TERMINAL_DESTINY_ID);
        boolean isInReplacementTerminal = parcel.getInteger("is_in_replacement_terminal") == 1;
        boolean isInTerminalDestiny = terminalDestinyId.equals(terminalId);

        this.getPackagesByParcelOfShipment(conn, parcelId).whenComplete((packages, errorPackages) -> {
            try {
                if (errorPackages != null){
                    throw errorPackages;
                }

                int countArrived = 0, countLocated = 0;

                for(int iP = 0; iP < packages.size(); iP++){
                    JsonObject pack = packages.getJsonObject(iP);
                    Integer packageId = pack.getInteger(ID);
                    PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[pack.getInteger("package_status")];

                    if (!packageStatus.notApplyArrivedCount()) {
                        int ppAction = PACKAGE_STATUS.ARRIVED.ordinal();
                        String pptAction = ParcelsPackagesTrackingDBV.ACTION.ARRIVED.name().toLowerCase();
                        if(packageStatus.wasLocated() || (packageStatus.wasDownloaded() && !isInTerminalDestiny && !isInReplacementTerminal)) {
                            pptAction = ParcelsPackagesTrackingDBV.ACTION.LOCATED.name().toLowerCase();
                            ppAction = PACKAGE_STATUS.LOCATED.ordinal();
                            countLocated ++;
                        } else if (packageStatus.wasDeliveredOrArrived() || (packageStatus.wasDownloaded() && (isInTerminalDestiny || isInReplacementTerminal))){
                            countArrived ++;
                        }

                        if (packageStatus.wasDownloaded()) {
                            execQueryList.add(UtilsTravel.execGenericQuery(conn, this.generateGenericCreate("parcels_packages_tracking", new JsonObject()
                                    .put("parcel_id", parcelId)
                                    .put("parcel_package_id", packageId)
                                    .put("created_by", userId)
                                    .put(TERMINAL_ID, isInReplacementTerminal ? terminalDestinyId : terminalId)
                                    .put("action", pptAction))));

                            execQueryList.add(UtilsTravel.execGenericQuery(conn, this.generateGenericUpdate("parcels_packages", new JsonObject()
                                    .put("id", packageId)
                                    .put("package_status", ppAction)
                                    .put("updated_by", userId)
                                    .put("updated_at", UtilsDate.sdfDataBase(new Date())))));
                        }
                    }
                }

                if (countLocated > 0 || countArrived > 0) {
                    int parcelStatus;
                    if (countArrived >= countLocated){
                        parcelStatus = totalPackages.equals(countArrived) ? PARCEL_STATUS.ARRIVED.ordinal() : PARCEL_STATUS.ARRIVED_INCOMPLETE.ordinal();
                    } else {
                        parcelStatus = totalPackages.equals(countLocated) ? PARCEL_STATUS.LOCATED.ordinal() : PARCEL_STATUS.LOCATED_INCOMPLETE.ordinal();
                    }

                    execQueryList.add(UtilsTravel.execGenericQuery(conn, this.generateGenericUpdate("parcels", new JsonObject()
                            .put(ID, parcelId)
                            .put("parcel_status", parcelStatus)
                            .put("updated_by", userId)
                            .put("updated_at", UtilsDate.sdfDataBase(new Date())))));
                }

                if(execQueryList.isEmpty()) {
                    future.complete(true);
                    return;
                }

                CompletableFuture.allOf(execQueryList.toArray(new CompletableFuture[execQueryList.size()])).whenComplete((reply, err) -> {
                    try {
                        if (err != null){
                            throw err;
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
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private final static String QUERY_GET_PARCELS_BY_SHIPMENT = "SELECT DISTINCT\n" +
            "  shipppt.parcel_id,\n" +
            "  p.terminal_destiny_id,\n" +
            "  p.total_packages,\n" +
            "  sr.id AS schedule_route_id,\n" +
            "  IF(pt.id IS NULL, FALSE, TRUE) AS is_transhipment,\n" +
            "  IF((SELECT bprc.id FROM branchoffice_parcel_receiving_config bprc \n" +
            "   WHERE bprc.receiving_branchoffice_id = srd.terminal_destiny_id \n" +
            "       AND bprc.of_branchoffice_id = p.terminal_destiny_id AND bprc.status = 1 LIMIT 1) IS NULL, \n" +
            "       FALSE, TRUE) AS is_in_replacement_terminal\n" +
            "FROM shipments_parcel_package_tracking shipppt\n" +
            "INNER JOIN shipments AS s ON s.id = shipppt.shipment_id \n" +
            "LEFT JOIN shipments ship ON ship.id = shipppt.shipment_id\n" +
            "INNER JOIN parcels p ON p.id = shipppt.parcel_id \n" +
            "LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id\n" +
            "INNER JOIN travel_logs tl ON tl.download_id = ship.id\n" +
            "INNER JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id AND srd.terminal_destiny_id = ship.terminal_id \n" +
            "WHERE p.parcel_status NOT IN (2, 4, 6)\n" +
            "AND sr.id = ?\n" +
            "AND srd.terminal_destiny_id = ?;";

    private final static String QUERY_GET_PACKAGES_ARRIVED_BY_PARCEL_OF_SHIPMENT = "SELECT\n" +
            "  pp.id,\n" +
            "  pp.package_status\n" +
            " FROM parcels_packages pp\n" +
            " INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            " WHERE pp.package_status NOT IN (4, 7)\n" +
            " AND p.id = ?;";

}