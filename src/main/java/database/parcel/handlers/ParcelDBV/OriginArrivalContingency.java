package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class OriginArrivalContingency extends DBHandler<ParcelDBV> {

    public OriginArrivalContingency(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Integer updatedBy = body.getInteger(CREATED_BY);
            JsonArray packageIds = body.getJsonArray(_PACKAGES);
            String notes = body.getString(_NOTES);
            List<JsonObject> result = new ArrayList<>();
            
            getPackagesInfo(packageIds, terminalId).whenComplete((packages, errPackInfo) -> {
                try {
                    if (errPackInfo != null) {
                        throw errPackInfo;
                    }

                    validatePackages(result, packages);
                    if (packages.isEmpty()) {
                        message.reply(new JsonObject().put("result", result));
                        return;
                    }

                    startTransaction(message, conn -> {
                        try {
                            updateStatus(conn, packages, updatedBy).whenComplete((resUpd, errUpd) -> {
                                try {
                                    if (errUpd != null) {
                                        throw errUpd;
                                    }
                                    insertTracking(conn, packages, terminalId, notes, updatedBy).whenComplete((resIT, errIT) -> {
                                        try {
                                            if (errIT != null) {
                                                throw errIT;
                                            }
                                            this.commit(conn, message, new JsonObject().put("result", result));
                                        } catch (Throwable t) {
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t) {
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
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getPackagesInfo(JsonArray packagesIds, Integer terminalId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String paramPackagesIds = packagesIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            String QUERY = String.format(GET_PACKAGES_INFO, paramPackagesIds);
            dbClient.queryWithParams(QUERY, new JsonArray().add(terminalId).add(terminalId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Packages info not found");
                    }
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }
    
    private void validatePackages(List<JsonObject> result, List<JsonObject> packages) {
        List<JsonObject> packagesCopy = new ArrayList<>(packages);
        for (JsonObject pack : packagesCopy) {
            PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[pack.getInteger(_PACKAGE_STATUS)];
            boolean isValidOrigin = pack.getInteger("is_valid_origin") == 1;

            if (!packageStatus.canBeDocumented()) {
                result.add(pack
                        .put("is_error", true)
                        .put(_MESSAGE, "Cannot be documented due to its current status")
                        .put(_PACKAGE_STATUS, packageStatus.ordinal()));
                packages.remove(pack);
            } else if (!isValidOrigin) {
                result.add(pack
                        .put("is_error", true)
                        .put(_MESSAGE, "Invalid terminal origin")
                        .put(_PACKAGE_STATUS, packageStatus.ordinal()));
                packages.remove(pack);
            } else {
                result.add(pack
                        .put("is_error", false)
                        .put(_MESSAGE, "Success")
                        .put(_PACKAGE_STATUS, PACKAGE_STATUS.DOCUMENTED.ordinal()));
            }
        }
    }

    private CompletableFuture<Boolean> updateStatus(SQLConnection conn, List<JsonObject> packages, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String paramPackagesIds = packages.stream()
                    .map(pack -> pack.getInteger(ID).toString())
                    .distinct()
                    .collect(Collectors.joining(", "));
            String upadtedAt = UtilsDate.sdfDataBase(new Date());

            JsonArray params = new JsonArray()
                    .add(createdBy).add(createdBy)
                    .add(upadtedAt).add(upadtedAt);

            String QUERY = String.format(UPDATE_STATUS, paramPackagesIds);

            conn.updateWithParams(QUERY, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertTracking(SQLConnection conn, List<JsonObject> packages, Integer terminalId, String notes, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<GenericQuery> inserts = packages.stream().map(pp -> this.generateGenericCreate("parcels_packages_tracking",
                    new JsonObject()
                            .put(_PARCEL_ID, pp.getInteger(_PARCEL_ID))
                            .put(_PARCEL_PACKAGE_ID, pp.getInteger(ID))
                            .put(_TERMINAL_ID, terminalId)
                            .put(_ACTION, PARCELPACKAGETRACKING_STATUS.IN_ORIGIN.getValue())
                            .put(_NOTES, notes)
                            .put(_IS_CONTINGENCY, true)
                            .put(CREATED_BY, createdBy)
                            .put(CREATED_AT, UtilsDate.sdfDataBase(new Date())))
            ).collect(Collectors.toList());
            List<JsonArray> paramsList = inserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());

            conn.batchWithParams(inserts.get(0).getQuery(), paramsList, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String GET_PACKAGES_INFO = "SELECT\n" +
            "   pp.id,\n" +
            "   pp.package_code,\n" +
            "   pp.package_status,\n" +
            "   p.id AS parcel_id,\n" +
            "   IF(? = p.terminal_origin_id \n" +
            "   OR (? IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc WHERE bprc.of_branchoffice_id = p.terminal_origin_id))\n" +
            "    , true, false) AS is_valid_origin\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "WHERE pp.id IN (%s);";

    private static final String UPDATE_STATUS = "UPDATE parcels_packages pp\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "SET pp.package_status = 0, p.parcel_status = 0, \n" +
            "   pp.updated_by = ?, p.updated_by = ?, \n" +
            "   pp.updated_at = ?, p.updated_at = ? \n" +
            "WHERE pp.id IN (%s);";

}
