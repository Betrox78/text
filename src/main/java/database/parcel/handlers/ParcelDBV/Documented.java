package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class Documented extends DBHandler<ParcelDBV> {

    public Documented(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray parcelsIds = body.getJsonArray(_PARCEL_ID);
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Integer createdBy = body.getInteger(CREATED_BY);

            startTransaction(message, conn -> {
                try {
                    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                    for (Object pId : parcelsIds) {
                        Integer parcelId = Integer.parseInt(String.valueOf(pId));
                        tasks.add(inOrigin(conn, parcelId, terminalId, createdBy));
                    }

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((reply, error) -> {
                        try {
                            if(error != null) {
                                throw error;
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
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<Boolean> inOrigin(SQLConnection conn, Integer parcelId,
                                                 Integer terminalId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Future<Boolean> f1 = Future.future();
        Future<List<Integer>> f2 = Future.future();
        validateParcel(parcelId).setHandler(f1.completer());
        getParcelPackagesIdByParcelId(parcelId).setHandler(f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<Integer> packagesIds = reply.result().resultAt(1);
                List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                tasks.add(updateStatus(conn, parcelId));
                tasks.add(insertParcelPackagesTracking(conn, parcelId, packagesIds, terminalId, createdBy));

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resUP, errUP) -> {
                    try {
                        if(errUP != null) {
                            throw errUP;
                        }
                        future.complete(true);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private Future<Boolean> validateParcel(Integer parcelId) {
        Future<Boolean> future = Future.future();
        dbClient.queryWithParams(GET_PARCEL_INFO, new JsonArray().add(parcelId), reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               List<JsonObject> result = reply.result().getRows();
               if (result.isEmpty()) {
                   throw new Exception("Parcel info not found");
               }
               JsonObject parcel = result.get(0);
               PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
               if (!parcelStatus.equals(PARCEL_STATUS.COLLECTED)) {
                   throw new Exception("Parcel status must be collected");
               }
               future.complete(true);
           } catch (Throwable t) {
               future.fail(t);
           }
        });
        return future;
    }

    private Future<List<Integer>> getParcelPackagesIdByParcelId(Integer parcelId) {
        Future<List<Integer>> future = Future.future();
        dbClient.queryWithParams(GET_PACKAGES_INFO, new JsonArray().add(parcelId), reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               List<JsonObject> result = reply.result().getRows();
               if (result.isEmpty()) {
                   throw new Exception("Packages not found");
               }
               future.complete(result.stream().map(p -> p.getInteger(ID)).collect(Collectors.toList()));
           } catch (Throwable t) {
               future.fail(t);
           }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateStatus(SQLConnection conn, Integer parcelId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Future<UpdateResult> f1 = Future.future();
        Future<UpdateResult> f2 = Future.future();
        conn.updateWithParams(UPDATE_PARCEL_TO_DOCUMENTED, new JsonArray().add(parcelId), f1.completer());
        conn.updateWithParams(UPDATE_PACKAGES_TO_DOCUMENTED, new JsonArray().add(parcelId), f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> insertParcelPackagesTracking(SQLConnection conn, Integer parcelId, List<Integer> packagesIds, Integer terminalId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<GenericQuery> inserts = new ArrayList<>();
        for (Integer packageId : packagesIds) {
            inserts.add(generateGenericCreate("parcels_packages_tracking",
                    new JsonObject().put(_PARCEL_ID, parcelId)
                            .put(_PARCEL_PACKAGE_ID, packageId)
                            .put(_ACTION, PARCELPACKAGETRACKING_STATUS.IN_ORIGIN.getValue())
                            .put(_TERMINAL_ID, terminalId)
                            .put(CREATED_BY, createdBy)
                            .put(CREATED_AT, UtilsDate.sdfDataBase(new Date()))));
        }

        List<JsonArray> params = inserts.stream().map(GenericQuery::getParams).collect(Collectors.toList());
        conn.batchWithParams(inserts.get(0).getQuery(), params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String GET_PARCEL_INFO = "SELECT parcel_status FROM parcels WHERE id = ?";
    private static final String GET_PACKAGES_INFO = "SELECT id FROM parcels_packages WHERE parcel_id = ?";

    private static final String UPDATE_PARCEL_TO_DOCUMENTED = "UPDATE parcels SET parcel_status = " + PARCEL_STATUS.DOCUMENTED.ordinal() + " WHERE id = ?;";

    private static final String UPDATE_PACKAGES_TO_DOCUMENTED = "UPDATE parcels_packages SET package_status = " + PACKAGE_STATUS.DOCUMENTED.ordinal() + " WHERE parcel_id = ?;";

}
