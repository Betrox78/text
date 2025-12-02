package database.parcel;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class ParcelsIncidencesDBV extends DBVerticle  {

    @Override
    public String getTableName() {
        return "parcels_incidences";
    }

    @Override
    protected void create(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer terminalId = body.containsKey(_TERMINAL_ID) ? (Integer) body.remove(_TERMINAL_ID) : null;
        Integer incidenceId = body.getInteger("incidence_id");
        Integer parcelId = body.getInteger(_PARCEL_ID);
        Integer parcelPackageId = body.getInteger(_PARCEL_PACKAGE_ID);
        String notes = body.getString(_NOTES);
        Integer createdBy = body.getInteger(CREATED_BY);
        startTransaction(message, conn->{
            try {
                GenericQuery model = this.generateGenericCreate(body);
                conn.updateWithParams(model.getQuery(), model.getParams(), reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        int parcelIncidenceId = reply.result().getKeys().getInteger(0);

                        GenericQuery insertTracking = this.generateGenericCreateSendTableName("parcels_packages_tracking", new JsonObject()
                                .put(_PARCEL_ID, parcelId)
                                .put(_PARCEL_PACKAGE_ID, parcelPackageId)
                                .put(_TERMINAL_ID, terminalId)
                                .put(ACTION, "incidence")
                                .put(_NOTES, notes)
                                .put(CREATED_BY, createdBy));
                        conn.updateWithParams(insertTracking.getQuery(), insertTracking.getParams(), replyTracking->{
                            try {
                                if (replyTracking.failed()) {
                                    throw replyTracking.cause();
                                }

                                updatePackageStatus(conn, incidenceId, parcelPackageId, createdBy).whenComplete((haveNotReceived, errUpdPP) -> {
                                    try {
                                        if (errUpdPP != null) {
                                            throw errUpdPP;
                                        }

                                        if (!haveNotReceived) {
                                            this.commit(conn, message, new JsonObject().put(ID, parcelIncidenceId));
                                            return;
                                        }

                                        updateParcelStatus(conn, parcelId, createdBy).whenComplete((resUpdP, errUpdP) -> {
                                            try {
                                                if (errUpdP != null) {
                                                    throw errUpdP;
                                                }

                                                this.commit(conn, message, new JsonObject().put(ID, parcelIncidenceId));

                                            } catch (Throwable t) {
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Throwable t) {
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
            } catch (Exception e){
                this.rollback(conn,e,message);
            }
        });
    }

    private CompletableFuture<Boolean> updatePackageStatus(SQLConnection conn, Integer incidenceId, Integer parcelPackageId, Integer updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(QUERY_GET_INCIDENCE_BY_ID, new JsonArray().add(incidenceId), replyIncidence -> {
                try {
                    if (replyIncidence.failed()) {
                        throw replyIncidence.cause();
                    }
                    List<JsonObject> resultIncidence = replyIncidence.result().getRows();
                    if(resultIncidence.isEmpty()) {
                        throw new Exception("Incidence not found");
                    }

                    JsonObject incidenceInfo = resultIncidence.get(0);
                    if(!incidenceInfo.getString(_NAME).contains("No Recibida")) {
                        future.complete(false);
                        return;
                    }

                    GenericQuery updatePackage = this.generateGenericUpdate("parcels_packages", new JsonObject()
                            .put(ID, parcelPackageId)
                            .put(_PACKAGE_STATUS, PACKAGE_STATUS.MERCHANDISE_NOT_RECEIVED.ordinal())
                            .put(UPDATED_BY, updatedBy)
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date())));
                    conn.updateWithParams(updatePackage.getQuery(), updatePackage.getParams(), replyUpdatePackg->{
                        try {
                            if (replyUpdatePackg.failed()) {
                                throw replyUpdatePackg.cause();
                            }

                            future.complete(true);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> updateParcelStatus(SQLConnection conn, Integer parcelId, Integer updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(QUERY_GET_TOTAL_PACKAGES_MERCHANDISE_NOT_RECEIVED_BY_PARCEL_ID, new JsonArray().add(parcelId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()) {
                        throw new Exception("Packages not found");
                    }

                    JsonObject info = results.get(0);
                    int totalPackages = info.getInteger(_TOTAL_PACKAGES);
                    int totalMerchandiseNotReceived = info.getInteger("total_merchandise_not_received");


                    GenericQuery updatePackage = this.generateGenericUpdate("parcels", new JsonObject()
                            .put(ID, parcelId)
                            .put(_PARCEL_STATUS, (totalPackages == totalMerchandiseNotReceived ? PARCEL_STATUS.MERCHANDISE_NOT_RECEIVED_TOTAL : PARCEL_STATUS.MERCHANDISE_NOT_RECEIVED_PARTIAL).ordinal())
                            .put(UPDATED_BY, updatedBy)
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date())));
                    conn.updateWithParams(updatePackage.getQuery(), updatePackage.getParams(), replyUpdate->{
                        try {
                            if (replyUpdate.failed()) {
                                throw replyUpdate.cause();
                            }

                            future.complete(true);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_GET_INCIDENCE_BY_ID = "SELECT * FROM incidences WHERE id = ?;";

    private static final String QUERY_GET_TOTAL_PACKAGES_MERCHANDISE_NOT_RECEIVED_BY_PARCEL_ID = "SELECT\n" +
            "   p.id, \n" +
            "   p.total_packages,\n" +
            "   SUM(CASE WHEN pp.package_status = "+ PACKAGE_STATUS.MERCHANDISE_NOT_RECEIVED.ordinal() +" THEN 1 ELSE 0 END) total_merchandise_not_received\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "WHERE p.id = ?\n" +
            "GROUP BY p.id;";
}
