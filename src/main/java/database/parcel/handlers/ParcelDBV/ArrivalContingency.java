package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;

public class ArrivalContingency extends DBHandler<ParcelDBV> {

    public ArrivalContingency(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String notes = body.getString(_NOTES);
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Integer createdBy = body.getInteger(CREATED_BY);
            Integer parcelId = body.getInteger(_PARCEL_ID);
            JsonArray packages = (JsonArray) body.getValue(PACKAGES);
            JsonArray returnArray = new JsonArray();
            this.startTransaction(message, conn -> {
                try {
                    this.getParcelsDetail(conn, parcelId, packages, terminalId).whenComplete((resultParcelsDetail, errorParcelsDetail) -> {
                        try {
                            if (errorParcelsDetail != null){
                                throw errorParcelsDetail;
                            }

                            this.checkPackagesArrivalContingency(conn, packages, terminalId, returnArray, resultParcelsDetail, notes, createdBy).whenComplete((resultCheckPackagesArrivalContingency, errorCheckPackagesArrivalContingency) ->{
                                try {
                                    if (errorCheckPackagesArrivalContingency != null){
                                        throw errorCheckPackagesArrivalContingency;
                                    }

                                    this.checkParcelArrivalContingency(conn, terminalId, new JsonArray(resultParcelsDetail), createdBy).whenComplete((resultCheckParcelArrivalContingency, errorCheckParcelArrivalContingency) -> {
                                        try {
                                            if (errorCheckParcelArrivalContingency != null){
                                                throw errorCheckParcelArrivalContingency;
                                            }

                                            this.checkParcelArrivalContingencyManifestRadEad(conn, new JsonArray(resultParcelsDetail), createdBy).whenComplete((resultCheckParcelArrivalContingencyManifestRadEad, errorCheckParcelArrivalContingencyManifestRadEad) -> {
                                                try {
                                                    if (errorCheckParcelArrivalContingencyManifestRadEad != null){
                                                        throw errorCheckParcelArrivalContingencyManifestRadEad;
                                                    }

                                                    this.commit(conn, message, new JsonObject().put("updated", true).put("result", returnArray));
                                                } catch (Throwable t){
                                                    this.rollback(conn, t, message);
                                                }
                                            });
                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t){
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getParcelsDetail(SQLConnection conn, Integer parcelId, JsonArray packages, Integer terminalId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.getParcelsIdList(conn, parcelId, packages, terminalId).whenComplete((resultGetParcelsIdList, errorGetParcelsIdList) -> {
            try {
                if (errorGetParcelsIdList != null) {
                    throw errorGetParcelsIdList;
                }

                List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                for (JsonObject parcel : resultGetParcelsIdList) {
                    tasks.add(this.getPackagesDetail(conn, parcel));
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resultParcelsDetail, errorParcelsDetail) -> {
                    try {
                        if (errorParcelsDetail != null){
                            throw errorParcelsDetail;
                        }
                        future.complete(resultGetParcelsIdList);
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

    private CompletableFuture<Boolean> checkPackagesArrivalContingency(SQLConnection conn, JsonArray packages, Integer terminalId, JsonArray returnArray, List<JsonObject> parcelsList, String notes, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
        for (Object pack : packages) {
            tasks.add(checkPackageTracking(conn, terminalId, (int) pack, returnArray, parcelsList, notes, createdBy));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
            try {
                if (error != null){
                    throw error;
                }
                future.complete(true);
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;

    }

    private CompletableFuture<JsonArray> checkParcelArrivalContingency(SQLConnection conn, Integer terminalId, JsonArray parcelsList, Integer updatedBy){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(parcelsList.stream()
            .map(parcel -> updateParcelStatusContingency(conn, terminalId, (JsonObject) parcel, updatedBy))
            .toArray(CompletableFuture[]::new))
                .whenComplete((s, tt) -> {
                    try {
                        if (tt != null) {
                            throw tt;
                        }
                        future.complete(parcelsList);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonArray> checkParcelArrivalContingencyManifestRadEad(SQLConnection conn, JsonArray parcelsList, int createdBy){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(parcelsList.stream()
                        .map(parcel -> updateParcelRadEadStatusContingency(conn, (JsonObject) parcel, createdBy))
                        .toArray(CompletableFuture[]::new))
                .whenComplete((s, tt) -> {
                    try {
                        if (tt != null) {
                            throw tt;
                        }
                        future.complete(parcelsList);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getParcelsIdList(SQLConnection conn, Integer parcelId, JsonArray packages, Integer terminalId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String paramString;
            if(!packages.isEmpty()) {
                paramString = "pp.id IN (";
                for(int i=0; i<packages.size(); i++){
                    if (i < packages.size() - 1){
                        paramString = paramString.concat(packages.getInteger(i).toString()).concat(",");
                    } else {
                        paramString = paramString.concat(packages.getInteger(i).toString());
                    }
                }
                paramString = paramString.concat(")");
            } else {
                paramString = "p.id = " + parcelId;
            }
            paramString = paramString.concat(" GROUP BY pp.parcel_id;");

            conn.queryWithParams(QUERY_GET_PARCELS_ID_LIST.concat(paramString), new JsonArray().add(terminalId), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()) {
                        throw new Exception("Parcels id not found");
                    }

                    for (JsonObject parcel : result) {
                        Integer terminalOriginId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
                        if(terminalOriginId.equals(terminalId)) {
                            throw new Exception("The terminal is the same as the origin");
                        }
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

    private CompletableFuture<JsonArray> getPackagesDetail(SQLConnection conn, JsonObject parcel){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(ID);
        conn.queryWithParams("SELECT id, package_status FROM parcels_packages WHERE parcel_id = ?;", new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                parcel.put(PACKAGES, result);
                future.complete(new JsonArray(result));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> checkPackageTracking(SQLConnection conn, Integer terminalId, Integer parcelPackageId, JsonArray returnArray, List<JsonObject> parcelsList, String notes, Integer created_by){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcelPackageId);
        conn.queryWithParams(QUERY_GET_PACKAGE_TRACKING, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> packagesTracking = reply.result().getRows();
                if (packagesTracking.isEmpty()){
                    this.comparePackageDestiny(conn, terminalId, parcelPackageId, parcelsList, notes, created_by).whenComplete((resultComparePackageDestiny, errorComparePackageDestiny) -> {
                        try {
                            if (errorComparePackageDestiny != null){
                                throw errorComparePackageDestiny;
                            }
                            returnArray.add(resultComparePackageDestiny);
                            future.complete(returnArray);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    JsonObject packageTrack = packagesTracking.get(0);
                    Integer packTerminalId = packageTrack.getInteger(_TERMINAL_ID);
                    PARCELPACKAGETRACKING_STATUS trackingStatus = PARCELPACKAGETRACKING_STATUS.fromValue(packageTrack.getString(_ACTION));
                    if(Objects.nonNull(packTerminalId) && packTerminalId.equals(terminalId) && trackingStatus.notValidArrivalContingency()) {
                        returnArray.add(new JsonObject().put(_PARCEL_PACKAGE_ID, parcelPackageId).put(STATUS, "in_terminal"));
                        future.complete(returnArray);
                    } else {
                        this.comparePackageDestiny(conn, terminalId, parcelPackageId, parcelsList, notes, created_by).whenComplete((resultComparePackageDestiny, errorComparePackageDestiny) -> {
                            try {
                                if (errorComparePackageDestiny != null){
                                    throw errorComparePackageDestiny;
                                }
                                returnArray.add(resultComparePackageDestiny);
                                future.complete(returnArray);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateParcelStatusContingency(SQLConnection conn, Integer terminalId, JsonObject parcel, Integer updatedBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Integer terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
        PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
        boolean isInReplacementTerminal = parcel.getInteger("is_in_replacement_terminal") == 1;
        if(parcelStatus.equals(PARCEL_STATUS.EAD) || terminalId.equals(terminalDestinyId) || isInReplacementTerminal) {
            JsonArray parcelPackages = parcel.getJsonArray(PACKAGES);
            JsonObject contingencyInfo = this.getContingencyInfoByParcel(parcelPackages);
            Integer totalPackages = contingencyInfo.getInteger(TOTAL);
            Integer locatedPackages = contingencyInfo.getInteger(PARCEL_STATUS.LOCATED.name().toLowerCase());
            Integer arrivedPackages = contingencyInfo.getInteger(PARCEL_STATUS.ARRIVED.name().toLowerCase());

            GenericQuery updateParcel = this.getUpdateParcelContingency(locatedPackages, arrivedPackages, totalPackages, parcel.getInteger(ID), updatedBy);
            if (Objects.isNull(updateParcel)) {
                future.complete(true);
                return future;
            }

            conn.updateWithParams(updateParcel.getQuery(), updateParcel.getParams(), replyUpdate ->{
                try {
                    if (replyUpdate.failed()){
                        throw replyUpdate.cause();
                    }
                    future.complete(replyUpdate.succeeded());
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } else {
            GenericQuery updateParcel = this.generateGenericUpdate("parcels", new JsonObject()
                    .put(ID, parcel.getInteger(ID))
                    .put(UPDATED_BY, updatedBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                    .put(_PARCEL_STATUS, PARCEL_STATUS.IN_TRANSIT.ordinal()));
            conn.updateWithParams(updateParcel.getQuery(), updateParcel.getParams(), replyUpdate ->{
                try {
                    if (replyUpdate.failed()){
                        throw replyUpdate.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private JsonObject getContingencyInfoByParcel(JsonArray packages){
        JsonObject contingencyInfo = new JsonObject();
        int arrived = this.countPackgesByStatus(packages, PACKAGE_STATUS.ARRIVED.ordinal());
        int located = this.countPackgesByStatus(packages, PACKAGE_STATUS.LOCATED.ordinal());
        int total = packages.size();
        contingencyInfo
                .put(PARCEL_STATUS.LOCATED.name().toLowerCase(), located)
                .put(PARCEL_STATUS.ARRIVED.name().toLowerCase(), arrived)
                .put(TOTAL, total);
        return contingencyInfo;
    }

    private int countPackgesByStatus(JsonArray packages, int status){
        int count = 0;
        for (int i = 0; i < packages.size(); i++){
            JsonObject pack = packages.getJsonObject(i);
            int packageStatus = pack.getInteger(_PACKAGE_STATUS);
            if (packageStatus == status) {
                count++;
            }
        }
        return count;
    }

    private GenericQuery getUpdateParcelContingency(Integer locatedPackages, Integer arrivedPackages, Integer totalPackages, Integer parcelId, Integer updatedBy){
        JsonObject body = new JsonObject().put(ID, parcelId);
        PARCEL_STATUS parcelStatus = null;
        if (!locatedPackages.equals(0) && totalPackages.equals(locatedPackages)){
            parcelStatus = PARCEL_STATUS.LOCATED;
        } else if(locatedPackages > 0 && totalPackages > locatedPackages && arrivedPackages.equals(0)){
            parcelStatus = PARCEL_STATUS.LOCATED_INCOMPLETE;
        } else if (!arrivedPackages.equals(0) && totalPackages.equals(arrivedPackages)) {
            parcelStatus = PARCEL_STATUS.ARRIVED;
        } else if (arrivedPackages > 0 && totalPackages > arrivedPackages){
            parcelStatus = PARCEL_STATUS.ARRIVED_INCOMPLETE;
        }
        return Objects.isNull(parcelStatus) ?
                null :
                this.generateGenericUpdate("parcels",
                        body
                            .put(UPDATED_BY, updatedBy)
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                            .put(_PARCEL_STATUS, parcelStatus.ordinal()));
    }

    private CompletableFuture<Boolean> updateParcelRadEadStatusContingency(SQLConnection conn, JsonObject parcel, int createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(ID);
        conn.query(GET_PARCEL_RAD_EAD_ID_CONTINGENCY.concat( String.valueOf(parcelId)), reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                } else {
                    if(reply.result().getNumRows()>0) {
                        JsonArray params = new JsonArray()
                                .add(createdBy)
                                .add(parcelId);
                        conn.updateWithParams(UPDATE_STATUS_PARCELS_RAD_EAD_CONTINGENCY,params, (AsyncResult<UpdateResult> parcelReply) -> {
                            try {
                                if (parcelReply.failed()) {
                                    throw parcelReply.cause();
                                }
                                future.complete(parcelReply.succeeded());
                            } catch (Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
                    }else{
                        future.complete(reply.succeeded());
                    }
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> comparePackageDestiny(SQLConnection conn, Integer terminalId, Integer parcelPackageId, List<JsonObject> parcelsList, String notes, Integer updatedBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKAGE_DESTINY, new JsonArray().add(terminalId).add(terminalId).add(parcelPackageId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()){
                    throw new Exception(parcelPackageId + " parcel_package_id not exists");
                }
                JsonObject resultPackage = results.get(0);
                Integer parcelId = resultPackage.getInteger(_PARCEL_ID);
                Integer parcelTranshipmentId = resultPackage.getInteger("parcel_transhipment_id");
                boolean isInReplacementTerminal = resultPackage.getInteger("is_in_replacement_terminal") == 1;
                Integer terminalDestinyId = resultPackage.getInteger(_TERMINAL_DESTINY_ID);
                boolean receiveTranshipments = resultPackage.getBoolean("receive_transhipments");
                PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[resultPackage.getInteger(_PACKAGE_STATUS)];
                PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[resultPackage.getInteger(_PARCEL_STATUS)];

                if ((packageStatus.equals(PACKAGE_STATUS.ARRIVED) && !parcelStatus.equals(PARCEL_STATUS.EAD)) ||
                        packageStatus.equals(PACKAGE_STATUS.CANCELED) ||
                        packageStatus.equals(PACKAGE_STATUS.DELIVERED) ||
                        packageStatus.equals(PACKAGE_STATUS.DELIVERED_CANCEL)) {
                    throw new Exception(parcelPackageId + " parcel_package_id have status " + packageStatus.name().toLowerCase());
                }

                JsonObject parcelReference = this.getJsonObjectReference(parcelsList, parcelId);
                List<JsonObject> parcelPackagesReference = new ArrayList<>();
                parcelReference.getJsonArray(PACKAGES).forEach(p -> parcelPackagesReference.add((JsonObject) p));

                String packageStatusName;
                int packageStatusOrdinal;
                if (terminalId.equals(terminalDestinyId) || isInReplacementTerminal){
                    packageStatusName = PACKAGE_STATUS.ARRIVED.name().toLowerCase();
                    packageStatusOrdinal = PACKAGE_STATUS.ARRIVED.ordinal();
                } else if (Objects.nonNull(parcelTranshipmentId) || receiveTranshipments){
                    packageStatusName = PACKAGE_STATUS.READY_TO_TRANSHIPMENT.name().toLowerCase();
                    packageStatusOrdinal = PACKAGE_STATUS.READY_TO_TRANSHIPMENT.ordinal();
                } else {
                    packageStatusName = PACKAGE_STATUS.LOCATED.name().toLowerCase();
                    packageStatusOrdinal = PACKAGE_STATUS.LOCATED.ordinal();
                }
                Integer finalTerminalId = isInReplacementTerminal ? terminalDestinyId : terminalId;
                this.insertTrackingContingency(conn, parcelId, parcelPackageId, finalTerminalId, packageStatusName, notes, updatedBy)
                        .whenComplete((resultInsertTrackingContingency, errorInsertTrackingContingency) -> {
                            try {
                                if (errorInsertTrackingContingency != null) {
                                    throw errorInsertTrackingContingency;
                                }
                                this.updatePackageStatus(conn, parcelPackageId, packageStatusOrdinal, updatedBy).whenComplete((packageUpdated, errorUpdatePackageStatus) -> {
                                    try {
                                        if (errorUpdatePackageStatus != null){
                                            throw errorUpdatePackageStatus;
                                        }
                                        JsonObject packageReference = this.getJsonObjectReference(parcelPackagesReference, parcelPackageId);
                                        packageReference.mergeIn(packageUpdated, true);

                                        future.complete(new JsonObject()
                                                .put(_PARCEL_PACKAGE_ID, parcelPackageId)
                                                .put("pack", resultPackage)
                                                .put(STATUS, packageStatusName));
                                    } catch (Throwable t){
                                        future.completeExceptionally(t);
                                    }
                                });
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

    private JsonObject getJsonObjectReference(List<JsonObject> compareList, Integer idToCompare){
        List<JsonObject> list = compareList.stream()
                .filter(obj -> obj.getInteger(ID).equals(idToCompare))
                .collect(toList());
        return list.get(0);
    }

    private CompletableFuture<Boolean> insertTrackingContingency(SQLConnection conn, Integer parcelId, Integer parcelPackageId, Integer terminalId, String actionName, String notes, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject insertObj = new JsonObject()
                .put(_PARCEL_ID, parcelId)
                .put(_PARCEL_PACKAGE_ID, parcelPackageId)
                .put(ACTION, actionName)
                .put(_IS_CONTINGENCY, true)
                .put(_TERMINAL_ID, terminalId)
                .put(_NOTES, notes)
                .put(CREATED_BY, createdBy);
        GenericQuery insert = this.generateGenericCreate("parcels_packages_tracking", insertObj);
        conn.updateWithParams(insert.getQuery(), insert.getParams(), reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                future.complete(reply.succeeded());
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> updatePackageStatus(SQLConnection conn, Integer parcelPackageId, Integer action, Integer updatedBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject updatePackage = new JsonObject()
                .put(ID, parcelPackageId)
                .put(UPDATED_BY, updatedBy)
                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                .put(_PACKAGE_STATUS, action);

        GenericQuery update = this.generateGenericUpdate("parcels_packages", updatePackage);
        conn.updateWithParams(update.getQuery(), update.getParams(), reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                future.complete(updatePackage);
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_GET_PARCELS_ID_LIST= "SELECT\n" +
            "   pp.parcel_id AS id,\n" +
            "   p.terminal_origin_id,\n" +
            "   p.terminal_destiny_id,\n" +
            "   p.parcel_status,\n" +
            "   pt.id AS parcel_transhipment_id,\n" +
            "   IF((SELECT COUNT(bprc.id) FROM branchoffice_parcel_receiving_config bprc \n" +
            "       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id\n" +
            "       AND bprc.receiving_branchoffice_id = ? AND bprc.status = 1) > 0, TRUE, FALSE) AS is_in_replacement_terminal\n" +
            " FROM parcels_packages pp\n" +
            " INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            " LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            " WHERE ";

    private static final String QUERY_GET_PACKAGE_TRACKING = "SELECT\n" +
            "   ppt.id,\n" +
            "   ppt.parcel_id,\n" +
            "   pp.package_code,\n" +
            "   ppt.parcel_package_id,\n" +
            "   ppt.terminal_id,\n" +
            "   ppt.action\n" +
            " FROM parcels_packages_tracking ppt\n" +
            " LEFT JOIN parcels_packages pp ON pp.id = ppt.parcel_package_id\n" +
            " LEFT JOIN parcels p ON p.id = ppt.parcel_id\n" +
            " WHERE parcel_package_id = ?\n" +
            " AND ppt.action NOT IN ('" + PARCELPACKAGETRACKING_STATUS.PRINTED.getValue() + "')\n" +
            " AND pp.status = 1\n" +
            " ORDER BY id DESC LIMIT 1;";

    private static final String GET_PARCEL_RAD_EAD_ID_CONTINGENCY  = "select * from parcels_rad_ead where parcel_id = ";

    private static final String UPDATE_STATUS_PARCELS_RAD_EAD_CONTINGENCY = "UPDATE parcels_rad_ead \n " +
            " SET  status = 8 ,\n " +
            "confirme_rad = 1 ,\n "+
            "updated_by = ? \n " +
            " WHERE parcel_id = ? AND status != 4 ;";

    private static final String QUERY_GET_PACKAGE_DESTINY = "SELECT\n" +
            "   pp.id,\n" +
            "   pp.package_code,\n" +
            "   pp.package_status,\n" +
            "   p.id AS parcel_id,\n" +
            "   p.parcel_status,\n" +
            "   pat.name AS package_type_name,\n" +
            "   p.terminal_destiny_id,\n" +
            "   pt.id AS parcel_transhipment_id,\n" +
            "   IF((SELECT COUNT(bprc.id) FROM branchoffice_parcel_receiving_config bprc \n" +
            "       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id\n" +
            "       AND bprc.receiving_branchoffice_id = ? AND bprc.status = 1) > 0, TRUE, FALSE) AS is_in_replacement_terminal,\n" +
            "   b.receive_transhipments\n" +
            " FROM parcels_packages pp\n" +
            " LEFT JOIN package_types pat ON pat.id = pp.package_type_id\n" +
            " LEFT JOIN parcels p ON p.id = pp.parcel_id\n" +
            " LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            " LEFT JOIN branchoffice b ON b.id = ?\n" +
            " WHERE pp.id = ?;";
}
