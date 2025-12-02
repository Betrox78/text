package database.vechicle;

import database.branchoffices.models.Branchoffice;
import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.parcel.enums.PACKAGE_STATUS;
import database.shipments.ShipmentsDBV;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENTS_TRAILERS_ACTION;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_TYPE;
import database.shipments.handlers.ShipmentsDBV.models.Shipment;
import database.vechicle.models.Trailer;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_ID;
import static database.boardingpass.BoardingPassDBV.SHIPMENT_ID;
import static database.parcel.ParcelDBV.PARCEL_ID;
import static database.parcel.ParcelsPackagesDBV.PACKAGE_CODE;
import static database.parcel.ParcelsPackagesDBV.PARCEL_PACKAGE_ID;
import static database.shipments.ShipmentsDBV.ACTION_ARRIVE_PACKAGE_CODES;
import static database.shipments.ShipmentsDBV.ACTION_DOWNLOAD_PACKAGE_CODES;
import static service.commons.Constants.*;
import static utils.UtilsResponse.responseError;

/**
 *
 * @author AllAbordo
 */
public class TrailersDBV extends DBVerticle {

    public static final String ACTION_GET_LIST = "TrailersDBV.getList";
    public static final String ACTION_GET_AVAILABLE_LIST = "TrailersDBV.getAvailableList";
    public static final String ACTION_GET_HITCHED = "TrailersDBV.getHitched";
    public static final String ACTION_ASSIGN_TO_SHIPMENT = "TrailersDBV.assignToShipment";
    public static final String ACTION_CHANGE = "TrailersDBV.change";
    public static final String ACTION_RELEASE = "TrailersDBV.release";
    public static final String ACTION_HITCH = "TrailersDBV.hitch";
    public static final String ACTION_LITE_RELEASE = "TrailersDBV.liteRelease";
    public static final String ACTION_GET_TO_HITCH = "TrailersDBV.getToHitch";
    public static final String ACTION_REMOVE_OF_ROUTE = "TrailersDBV.removeOfRoute";

    public static final String TRAILER_ID = "trailer_id";
    public static final String IS_TRANSFER = "is_transfer";
    public static final String TRANSFER_TRAILER_ID = "transfer_trailer_id";
    public static final String NAME = "name";
    public static final String C_SUBTIPOREM_ID = "c_SubTipoRem_id";
    public static final String PLATE = "plate";
    public static final String IN_USE = "in_use";

    @Override
    public String getTableName() { return "trailers"; }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_GET_LIST:
                getList(message);
                break;
            case ACTION_GET_AVAILABLE_LIST:
                getAvailableList(message);
                break;
            case ACTION_GET_HITCHED:
                getHitched(message);
                break;
            case ACTION_ASSIGN_TO_SHIPMENT:
                assignToShipment(message);
                break;
            case ACTION_CHANGE:
                change(message);
                break;
            case ACTION_RELEASE:
                release(message);
                break;
            case ACTION_HITCH:
                hitch(message);
                break;
            case ACTION_LITE_RELEASE:
                liteRelease(message);
                break;
            case ACTION_GET_TO_HITCH:
                getToHitch(message);
                break;
            case ACTION_REMOVE_OF_ROUTE:
                removeOfRoute(message);
                break;
        }
    }

    /**
     * GENERAL METHODS <!- START -!>
     */

    @Override
    protected void create(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String plate = body.getString(PLATE);
            checkPlate(null, plate).whenComplete((isAvailable, errorAvailability) -> {
                try {
                    if (errorAvailability != null) {
                        throw errorAvailability;
                    }

                    GenericQuery create = this.generateGenericCreate(body);
                    this.startTransaction(message, conn -> conn.updateWithParams(create.getQuery(), create.getParams(), reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            Integer id = reply.result().getKeys().getInteger(0);
                            this.commit(conn, message, new JsonObject().put(ID, id));
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    }));

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

    private CompletableFuture<Boolean> checkPlate(Integer id, String plate) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (Objects.nonNull(plate)) {
            String QUERY = QUERY_CHECK_PLATE;
            JsonArray params = new JsonArray().add(plate);
            if (Objects.nonNull(id)) {
                QUERY += " AND id != ? ";
                params.add(id);
            }
            this.dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    if (!reply.result().getRows().isEmpty()) {
                        throw new Exception("The plate has already been captured");
                    }

                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } else {
            future.complete(true);
        }
        return future;
    }

    @Override
    protected void update(Message<JsonObject> message) {
        try {
            Trailer request = message.body().mapTo(Trailer.class);
            getTrailer(request.getId()).whenComplete((trailer, errorTrailer) -> {
                try {
                    if (errorTrailer != null) {
                        throw errorTrailer;
                    }
                    checkPlate(request.getId(), request.getPlate()).whenComplete((isAvailable, errorAvailability) -> {
                        try {
                            if (errorAvailability != null) {
                                throw errorAvailability;
                            }
                            GenericQuery update = this.generateGenericUpdate(this.getTableName(), parseBodyUpdate(request));
                            this.startTransaction(message, conn -> conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                                try {
                                    if (reply.failed()) {
                                        throw reply.cause();
                                    }
                                    this.commit(conn, message, null);
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            }));
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

    private JsonObject parseBodyUpdate(Trailer request) {
        JsonObject body = JsonObject.mapFrom(request);
        body.getMap().entrySet().removeIf(entry -> entry.getValue() == null);
        return body;
    }

    private CompletableFuture<Trailer> getTrailer(Integer id) {
        CompletableFuture<Trailer> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_TRAILER_BY_ID, new JsonArray().add(id), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Trailer not found");
                }
                Trailer trailer = result.get(0).mapTo(Trailer.class);
                future.complete(trailer);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getList(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_GET_LIST, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<Trailer> trailers = reply.result().getRows().stream().map(t -> t.mapTo(Trailer.class)).collect(Collectors.toList());
                    List<Trailer> trailersInUse = trailers.stream().filter(Trailer::getInUse).collect(Collectors.toList());
                    if (trailersInUse.isEmpty()) {
                        message.reply(new JsonArray(trailers.stream().map(JsonObject::mapFrom).collect(Collectors.toList())));
                    } else {
                        List<Future> tasks = new ArrayList<>();
                        for (Trailer trailerInUse : trailersInUse) {
                            tasks.add(getCurrentRoute(trailerInUse));
                        }
                        CompositeFuture.all(tasks).setHandler(replyTasks -> {
                            try {
                                if (replyTasks.failed()) {
                                    throw replyTasks.cause();
                                }

                                message.reply(new JsonArray(trailers.stream().map(trailer -> {
                                    for (Trailer trailerInUse : trailersInUse) {
                                        if (trailerInUse.getId().equals(trailer.getId())) {
                                            trailer.setCurrentRoute(trailerInUse.getCurrentRoute());
                                            break;
                                        }
                                    }
                                    return JsonObject.mapFrom(trailer);
                                }).collect(Collectors.toList())));
                            } catch (Throwable t) {
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    }
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

    private Future<Boolean> getCurrentRoute(Trailer trailer) {
        Future<Boolean> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_CURRENT_ROUTE_BY_TRAILER, new JsonArray().add(trailer.getId()), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (!result.isEmpty()) {
                    trailer.setCurrentRoute(result.get(0));
                }
                future.complete(true);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private void getAvailableList(Message<JsonObject> message) {
        try {
            this.dbClient.query(QUERY_GET_AVAILABLE_LIST, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void getHitched(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer scheduleRouteId = body.getInteger(ShipmentsDBV.SCHEDULE_ROUTE_ID);
            this.dbClient.queryWithParams(QUERY_GET_HITCHED, new JsonArray().add(scheduleRouteId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void assignToShipment(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int trailerId = body.getInteger(TRAILER_ID);
            int shipmentId = body.getInteger(SHIPMENT_ID);
            int createdBy = body.getInteger(CREATED_BY);

            Future<Shipment> f1 = Future.future();
            Future<Trailer> f2 = Future.future();
            checkShipment(shipmentId, SHIPMENT_TYPE.load).setHandler(f1.completer());
            checkTrailer(trailerId, true).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    Shipment shipment = reply.result().resultAt(0);
                    Integer scheduleRouteId = shipment.getScheduleRouteId();
                    startTransaction(message, conn -> {
                        try {
                            cleanLatestMovementShipmentsTrailers(conn, scheduleRouteId, trailerId, createdBy).whenComplete((resClean, errClean) -> {
                               try {
                                   if (errClean != null) {
                                       throw errClean;
                                   }
                                   setTrailerInUse(conn, trailerId, true, createdBy).whenComplete((resSIU, errorSIU) -> {
                                       try {
                                           if (errorSIU != null) {
                                               throw errorSIU;
                                           }
                                           JsonObject shipmentTrailer = new JsonObject()
                                                   .put(SHIPMENT_ID, shipmentId)
                                                   .put(TRAILER_ID, trailerId)
                                                   .put(ACTION, SHIPMENTS_TRAILERS_ACTION.assign.name())
                                                   .put(LATEST_MOVEMENT, true)
                                                   .put(SCHEDULE_ROUTE_ID, shipment.getScheduleRouteId())
                                                   .put(CREATED_BY, createdBy);
                                           registerShipmentTrailer(conn, shipmentTrailer).whenComplete((shipmentTrailerId, errorST) -> {
                                               try {
                                                   if (errorST != null) {
                                                       throw errorST;
                                                   }
                                                   this.commit(conn, message, new JsonObject().put(ID, shipmentTrailerId));
                                               } catch (Throwable t) {
                                                   t.printStackTrace();
                                                   this.rollback(conn, t, message);
                                               }
                                           });
                                       } catch (Throwable t) {
                                           t.printStackTrace();
                                           this.rollback(conn, t, message);
                                       }
                                   });
                               } catch (Throwable t) {
                                   this.rollback(conn, t, message);
                               }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
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

    private Future<Shipment> checkShipment(int shipmentId, SHIPMENT_TYPE shipmentType) {
        Future<Shipment> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_SHIPMENT, new JsonArray().add(shipmentId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Shipment not found");
                }

                Shipment shipment = result.get(0).mapTo(Shipment.class);
                if (!shipment.getShipmentType().equals(shipmentType)) {
                    throw new Exception("Shipment type must be " + shipmentType.name());
                }

                future.complete(shipment);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Shipment> getShipment(int shipmentId, SHIPMENT_TYPE shipmentType) {
        Future<Shipment> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_SHIPMENT, new JsonArray().add(shipmentId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Shipment not found");
                }

                Shipment shipment = result.get(0).mapTo(Shipment.class);
                if (!shipment.getShipmentType().equals(shipmentType)) {
                    throw new Exception("Shipment type must be " + shipmentType.name());
                }

                future.complete(shipment);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> setTrailerInUse(SQLConnection conn, Integer trailerId, boolean inUse, Integer updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            GenericQuery update = this.generateGenericUpdate(this.getTableName(), new JsonObject()
                    .put(ID, trailerId)
                    .put(UPDATED_BY, updatedBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                    .put(IN_USE, inUse));
            conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                try {
                    if (reply.failed()) {
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

    private CompletableFuture<Integer> registerShipmentTrailer(SQLConnection conn, JsonObject shipmentTrailer) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        GenericQuery create = this.generateGenericCreateSendTableName("shipments_trailers", shipmentTrailer);
        conn.updateWithParams(create.getQuery(), create.getParams(), replyCreate -> {
            try {
                if (replyCreate.failed()){
                    throw replyCreate.cause();
                }
                Integer id = replyCreate.result().getKeys().getInteger(0);
                future.complete(id);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> cleanLatestMovementShipmentsTrailers(SQLConnection conn, Integer scheduleRouteId, Integer trailerId, Integer updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(updatedBy).add(UtilsDate.sdfDataBase(new Date()))
                .add(scheduleRouteId).add(trailerId);
        conn.updateWithParams(UPDATE_CLEAN_LATEST_MOVEMENT_SHIPMENTS_TRAILERS, params, replyCreate -> {
            try {
                if (replyCreate.failed()){
                    throw replyCreate.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> cleanLatestMovementShipmentsTrailersById(SQLConnection conn, Integer shipmentTrailerId, Integer trailerId, Integer updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(updatedBy).add(UtilsDate.sdfDataBase(new Date()))
                .add(shipmentTrailerId).add(trailerId);
        conn.updateWithParams(UPDATE_CLEAN_LATEST_MOVEMENT_BY_SHIPMENTS_TRAILERS_ID, params, replyCreate -> {
            try {
                if (replyCreate.failed()){
                    throw replyCreate.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void change(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int trailerId = body.getInteger(TRAILER_ID);
            int transferTrailerId = body.getInteger(TRANSFER_TRAILER_ID);
            int shipmentId = body.getInteger(SHIPMENT_ID);
            int createdBy = body.getInteger(CREATED_BY);

            Future<Shipment> f1 = Future.future();
            Future<Trailer> f2 = Future.future();
            checkShipment(shipmentId, SHIPMENT_TYPE.load).setHandler(f1.completer());
            checkTrailer(transferTrailerId, true).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    Shipment shipment = reply.result().resultAt(0);
                    Integer scheduleRouteId = shipment.getScheduleRouteId();
                    checkTrailerIsAssigned(scheduleRouteId, trailerId).setHandler(ctia -> {
                        try {
                            if (ctia.failed()) {
                                throw ctia.cause();
                            }
                            getPackagesToTransfer(trailerId, shipment.getScheduleRouteId()).whenComplete((packages, errorPackages) -> {
                                try {
                                    if (errorPackages != null) {
                                        throw errorPackages;
                                    }
                                    startTransaction(message, conn -> {
                                        try {
                                            List<CompletableFuture> tasksTransactions = new ArrayList<>();
                                            tasksTransactions.add(cleanLatestMovementShipmentsTrailers(conn, scheduleRouteId, trailerId, createdBy));
                                            tasksTransactions.add(registerShipmentTrailer(conn, new JsonObject()
                                                    .put(SHIPMENT_ID, shipmentId)
                                                    .put(TRAILER_ID, transferTrailerId)
                                                    .put(ACTION, SHIPMENTS_TRAILERS_ACTION.transfer.name())
                                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                                    .put(IS_TRANSFER, true)
                                                    .put(LATEST_MOVEMENT, true)
                                                    .put(TRANSFER_TRAILER_ID, trailerId)
                                                    .put(CREATED_BY, createdBy)));
                                            tasksTransactions.add(setTrailerInUse(conn, trailerId, false, createdBy));
                                            tasksTransactions.add(setTrailerInUse(conn, transferTrailerId, true, createdBy));
                                            if (!packages.isEmpty()) {
                                                tasksTransactions.add(transferPackages(conn, shipmentId, transferTrailerId, packages, scheduleRouteId, createdBy));
                                            }
                                            CompletableFuture.allOf(tasksTransactions.toArray(new CompletableFuture[tasksTransactions.size()])).whenComplete((replyGS, errorGS) -> {
                                                try {
                                                    if (errorGS != null) {
                                                        throw errorGS;
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
                            });
                        } catch (Throwable t) {
                            reportQueryError(message, t);
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

    private CompletableFuture<List<JsonObject>> getPackagesToDownload(Integer trailerId, Integer scheduleRouteId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PACKAGES_TO_DOWNLOAD, new JsonArray().add(trailerId).add(scheduleRouteId), reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getRows());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getPackagesToTransfer(Integer trailerId, Integer scheduleRouteId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PACKAGES_TO_TRANSFER, new JsonArray().add(trailerId).add(scheduleRouteId), reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getRows());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getPackagesToTranshipment(Integer trailerId, Integer scheduleRouteId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PACKAGES_TO_TRANSHIPMENT, new JsonArray().add(trailerId).add(scheduleRouteId), reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getRows());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> transferPackages(SQLConnection conn, Integer shipmentId, Integer transferTrailerId, List<JsonObject> packages, Integer scheduleRouteId, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        cleanLatestMovementShipmentsParcelsTracking(conn, packages, scheduleRouteId, createdBy).whenComplete((resCM, errCM) -> {
            try {
                if (errCM != null) {
                    throw errCM;
                }
                ArrayList<GenericQuery> creates = new ArrayList<>();
                for (JsonObject pack : packages) {
                    creates.add(this.generateGenericCreateSendTableName("shipments_parcel_package_tracking", new JsonObject()
                            .put(SHIPMENT_ID, shipmentId)
                            .put(TRAILER_ID, transferTrailerId)
                            .put(PARCEL_ID, pack.getInteger(PARCEL_ID))
                            .put(LATEST_MOVEMENT, true)
                            .put(PARCEL_PACKAGE_ID, pack.getInteger(PARCEL_PACKAGE_ID))
                            .put(STATUS, SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS.TRANSFER.getValue())
                            .put(CREATED_BY, createdBy)));
                }
                List<JsonArray> createParams = creates.stream()
                        .map(GenericQuery::getParams)
                        .collect(Collectors.toList());
                conn.batchWithParams(creates.get(0).getQuery(), createParams, reply -> {
                    try {
                        if (reply.failed()) {
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
        });
        return future;
    }

    private CompletableFuture<Boolean> cleanLatestMovementShipmentsParcelsTracking(SQLConnection conn, List<JsonObject> packages, Integer scheduleRouteId, int updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String parcelPackageIdParams = packages.stream()
                    .map(pack -> "\'" + pack.getInteger(PARCEL_PACKAGE_ID) + "\'")
                    .collect(Collectors.joining(", "));
            conn.update(String.format(UPDATE_SHIPMENTS_PARCEL_PACKAGE_TRACKING_CLEAN_LATEST_MOVEMENT, updatedBy, UtilsDate.sdfDataBase(UtilsDate.getLocalDate()), scheduleRouteId, parcelPackageIdParams), reply -> {
                try {
                    if (reply.failed()) {
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

    private void release(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int trailerId = body.getInteger(TRAILER_ID);
            int shipmentId = body.getInteger(SHIPMENT_ID);
            int terminalId = body.getInteger(TERMINAL_ID);
            int createdBy = body.getInteger(CREATED_BY);

            Future<Shipment> f1 = Future.future();
            Future<Trailer> f2 = Future.future();
            Future<Branchoffice> f3 = Future.future();
            checkShipment(shipmentId, SHIPMENT_TYPE.download).setHandler(f1.completer());
            checkTrailer(trailerId, false).setHandler(f2.completer());
            checkTerminal(terminalId).setHandler(f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    Shipment shipment = reply.result().resultAt(0);
                    Integer scheduleRouteId = shipment.getScheduleRouteId();
                    Branchoffice branchoffice = reply.result().resultAt(2);
                    getPackagesToDownload(trailerId, scheduleRouteId).whenComplete((packages, errorPackages) -> {
                        try {
                            if (errorPackages != null) {
                                throw errorPackages;
                            }
                            JsonObject bodyDownloadPackageCodes = new JsonObject()
                                    .put(SHIPMENT_ID, shipmentId)
                                    .put(TRAILER_ID, trailerId)
                                    .put("codes", packages.stream().map(pp -> pp.getString(PACKAGE_CODE)).collect(Collectors.toList()))
                                    .put(CREATED_BY, createdBy);
                            downloadPackageCodes(message, bodyDownloadPackageCodes).setHandler(replyDownloadPC -> {
                                try {
                                    if (replyDownloadPC.failed()) {
                                        throw replyDownloadPC.cause();
                                    }
                                    arrivePackageCodes(message, replyDownloadPC.result()).setHandler(resArrivePC -> {
                                        try {
                                            if (resArrivePC.failed()) {
                                                throw resArrivePC.cause();
                                            }
                                            getPackagesToTranshipment(trailerId, scheduleRouteId).whenComplete((packagesCheck, errorPackagesCheck) -> {
                                                try {
                                                    if(errorPackagesCheck != null) {
                                                        throw errorPackagesCheck;
                                                    }
                                                    SHIPMENTS_TRAILERS_ACTION shipmentsTrailersAction = getReleaseShipmentsTrailersAction(branchoffice.getReceiveTranshipments(), packagesCheck.size());
                                                    startTransaction(message, conn -> {
                                                        try {
                                                            List<CompletableFuture> tasksTransactions = new ArrayList<>();
                                                            tasksTransactions.add(cleanLatestMovementShipmentsTrailers(conn, scheduleRouteId, trailerId, createdBy));
                                                            tasksTransactions.add(registerShipmentTrailer(conn, new JsonObject()
                                                                    .put(SHIPMENT_ID, shipmentId)
                                                                    .put(TRAILER_ID, trailerId)
                                                                    .put(ACTION, shipmentsTrailersAction.name())
                                                                    .put(LATEST_MOVEMENT, true)
                                                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                                                    .put(CREATED_BY, createdBy)));
                                                            if (shipmentsTrailersAction.equals(SHIPMENTS_TRAILERS_ACTION.release)) {
                                                                tasksTransactions.add(setTrailerInUse(conn, trailerId, false, createdBy));
                                                            }
                                                            CompletableFuture.allOf(tasksTransactions.toArray(new CompletableFuture[tasksTransactions.size()])).whenComplete((replyGS, errorGS) -> {
                                                                try {
                                                                    if (errorGS != null) {
                                                                        throw errorGS;
                                                                    }
                                                                    this.commit(conn, message, new JsonObject().put("success", true));
                                                                } catch (Throwable t) {
                                                                    t.printStackTrace();
                                                                    this.rollback(conn, t, message);
                                                                }
                                                            });
                                                        } catch (Throwable t) {
                                                            t.printStackTrace();
                                                            this.rollback(conn, t, message);
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

    private SHIPMENTS_TRAILERS_ACTION getReleaseShipmentsTrailersAction(boolean branchofficeReceiveTranshipments, Integer packagesCheckSize) {
        if (branchofficeReceiveTranshipments) {
            if (packagesCheckSize == 0) {
                return SHIPMENTS_TRAILERS_ACTION.release;
            } else {
                return SHIPMENTS_TRAILERS_ACTION.release_transhipment;
            }
        } else {
            return SHIPMENTS_TRAILERS_ACTION.release;
        }
    }

    private Future<Trailer> checkTrailer(int trailerId, boolean inUse) {
        Future<Trailer> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_TRAILER, new JsonArray().add(trailerId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Trailer not found");
                }

                Trailer trailer = result.get(0).mapTo(Trailer.class);
                if (inUse) {
                    if (trailer.getInUse()) {
                        throw new Exception("Trailer not available");
                    }
                } else {
                    if (!trailer.getInUse()) {
                        throw new Exception("Trailer is not in use");
                    }
                }
                if (trailer.getStatus() != 1) {
                    throw new Exception("Trailer is not active");
                }

                future.complete(trailer);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Branchoffice> checkTerminal(int terminalId) {
        Future<Branchoffice> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_BRANCHOFFICE, new JsonArray().add(terminalId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Terminal not found");
                }

                Branchoffice branchoffice = result.get(0).mapTo(Branchoffice.class);
                if (branchoffice.getStatus() != 1) {
                    throw new Exception("Terminal is not active");
                }

                future.complete(branchoffice);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<JsonObject> downloadPackageCodes(Message<JsonObject> message, JsonObject body) {
        Future<JsonObject> future = Future.future();
        body.put("origin_trailers", true);
        if (body.getJsonArray("codes").isEmpty()) {
            future.complete(null);
        } else {
            vertx.eventBus().send(ShipmentsDBV.class.getSimpleName(), body, new DeliveryOptions().addHeader(ACTION, ACTION_DOWNLOAD_PACKAGE_CODES), (AsyncResult<Message<JsonObject>> replyDownload) -> {
                try {
                    if (replyDownload.failed()) {
                        throw replyDownload.cause();
                    }
                    if (replyDownload.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        future.fail(INVALID_DATA_MESSAGE + " downloadPackageCodes");
                        return;
                    }
                    JsonObject resultDownload = replyDownload.result().body();
                    future.complete(resultDownload);
                } catch (Throwable t) {
                    t.printStackTrace();
                    JsonObject transactionLog = new JsonObject()
                            .put("method", "POST")
                            .put("path", "TrailersDBV - /shipments/download/checkPackageCodes")
                            .put("payload", message.body())
                            .put("exception", t.getMessage());
                    this.exceptionLoggerFuture(message, transactionLog).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            future.fail(t);
                        } catch (Throwable tt) {
                            tt.printStackTrace();
                            future.fail(t);
                        }
                    });
                }
            });
        }
        return future;
    }

    private Future<Boolean> arrivePackageCodes(Message<JsonObject> message, JsonObject body) {
        Future<Boolean> future = Future.future();
        if (Objects.isNull(body)) {
            future.complete(true);
        } else {
            JsonObject bodyArrive = new JsonObject()
                    .put(TERMINAL_ID, body.getInteger(TERMINAL_ID))
                    .put(ShipmentsDBV.SCHEDULE_ROUTE_ID, body.getInteger(ShipmentsDBV.SCHEDULE_ROUTE_ID))
                    .put(CREATED_BY, body.getInteger(CREATED_BY));

            vertx.eventBus().send(ShipmentsDBV.class.getSimpleName(), bodyArrive, new DeliveryOptions().addHeader(ACTION, ACTION_ARRIVE_PACKAGE_CODES), (AsyncResult<Message<JsonObject>> replyArrive) -> {
                try {
                    if (replyArrive.failed()) {
                        throw replyArrive.cause();
                    }
                    if (replyArrive.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        future.fail(INVALID_DATA_MESSAGE + " arrivePackageCodes");
                        return;
                    }
                    future.complete(true);
                } catch (Throwable t) {
                    t.printStackTrace();
                    JsonObject transactionLog = new JsonObject()
                            .put("method", "POST")
                            .put("path", "TrailersDBV.arrivePackageCodes")
                            .put("payload", bodyArrive)
                            .put("exception", t.getMessage());
                    this.exceptionLoggerFuture(message, transactionLog).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }
                            future.fail(t);
                        } catch (Throwable tt) {
                            tt.printStackTrace();
                            future.fail(t);
                        }
                    });
                }
            });
        }
        return future;
    }

    private void hitch(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int trailerId = body.getInteger(TRAILER_ID);
            int shipmentId = body.getInteger(SHIPMENT_ID);
            int terminalId = body.getInteger(TERMINAL_ID);
            int createdBy = body.getInteger(CREATED_BY);

            Future<Shipment> f1 = Future.future();
            Future<JsonObject> f2 = Future.future();
            Future<Branchoffice> f3 = Future.future();
            checkShipment(shipmentId, SHIPMENT_TYPE.load).setHandler(f1.completer());
            checkHitchTrailer(trailerId).setHandler(f2.completer());
            checkTerminal(terminalId).setHandler(f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    Shipment shipment = reply.result().resultAt(0);
                    JsonObject shipmentTrailer = reply.result().resultAt(1);
                    getPackagesToHitch(trailerId, terminalId).whenComplete((packages, errorPackages) -> {
                        try {
                            if (errorPackages != null) {
                                throw errorPackages;
                            }
                            startTransaction(message, conn -> {
                                try {
                                    List<CompletableFuture> tasksTransactions = new ArrayList<>();
                                    tasksTransactions.add(registerShipmentTrailer(conn, new JsonObject()
                                            .put(SHIPMENT_ID, shipmentId)
                                            .put(TRAILER_ID, trailerId)
                                            .put(ACTION, SHIPMENTS_TRAILERS_ACTION.hitch.name())
                                            .put(LATEST_MOVEMENT, true)
                                            .put(SCHEDULE_ROUTE_ID, shipment.getScheduleRouteId())
                                            .put(CREATED_BY, createdBy)));
                                    tasksTransactions.add(cleanLatestMovementShipmentsTrailersById(conn, shipmentTrailer.getInteger(ID), trailerId, createdBy));
                                    tasksTransactions.add(setTrailerInUse(conn, trailerId, true, createdBy));
                                    CompletableFuture.allOf(tasksTransactions.toArray(new CompletableFuture[tasksTransactions.size()])).whenComplete((replyGS, errorGS) -> {
                                        try {
                                            if (errorGS != null) {
                                                throw errorGS;
                                            }
                                            this.commit(conn, message, new JsonObject()
                                                    .put(SHIPMENT_ID, shipmentId)
                                                    .put(TRAILER_ID, trailerId)
                                                    .put("codes", packages.stream().map(pp -> pp.getString(PACKAGE_CODE)).collect(Collectors.toList()))
                                                    .put(CREATED_BY, createdBy));
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                    reportQueryError(message, ex);
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

    private Future<JsonObject> checkHitchTrailer(int trailerId) {
        Future<JsonObject> future = Future.future();
        this.dbClient.queryWithParams(QUERY_GET_LAST_ACTION_SHIPMENTS_TRAILERS_BY_TRAILER_ID, new JsonArray().add(trailerId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Trailer not found");
                }

                JsonObject trailer = result.get(0);
                if (!trailer.getBoolean(IN_USE)) {
                    throw new Exception("Trailer not available to hitch");
                }
                if (trailer.getInteger(STATUS) != 1) {
                    throw new Exception("Trailer is not active");
                }
                SHIPMENTS_TRAILERS_ACTION shipmentsTrailersAction = SHIPMENTS_TRAILERS_ACTION.valueOf(trailer.getString(ACTION));
                if (!shipmentsTrailersAction.equals(SHIPMENTS_TRAILERS_ACTION.release_transhipment)) {
                    throw new Exception("The last action of the trailer is: " + shipmentsTrailersAction.name() + ", must be " + SHIPMENTS_TRAILERS_ACTION.release_transhipment.name());
                }

                future.complete(trailer);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getPackagesToHitch(Integer trailerId, Integer terminalId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PACKAGES_TO_HITCH, new JsonArray().add(trailerId).add(terminalId), reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getRows());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void liteRelease(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int trailerId = body.getInteger(TRAILER_ID);
            int shipmentId = body.getInteger(SHIPMENT_ID);
            int createdBy = body.getInteger(CREATED_BY);

            Future<Shipment> f1 = Future.future();
            Future<Trailer> f2 = Future.future();
            getShipment(shipmentId, SHIPMENT_TYPE.download).setHandler(f1.completer());
            checkTrailer(trailerId, false).setHandler(f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    Shipment shipment = reply.result().resultAt(0);
                    Integer scheduleRouteId = shipment.getScheduleRouteId();
                    startTransaction(message, conn -> {
                        try {
                            List<CompletableFuture> tasksTransactions = new ArrayList<>();
                            tasksTransactions.add(cleanLatestMovementShipmentsTrailers(conn, scheduleRouteId, trailerId, createdBy));
                            tasksTransactions.add(setTrailerInUse(conn, trailerId, false, createdBy));
                            tasksTransactions.add(registerShipmentTrailer(conn, new JsonObject()
                                    .put(SHIPMENT_ID, shipmentId)
                                    .put(TRAILER_ID, trailerId)
                                    .put(ACTION, SHIPMENTS_TRAILERS_ACTION.release.name())
                                    .put(LATEST_MOVEMENT, true)
                                    .put(SCHEDULE_ROUTE_ID, scheduleRouteId)
                                    .put(CREATED_BY, createdBy)));
                            CompletableFuture.allOf(tasksTransactions.toArray(new CompletableFuture[tasksTransactions.size()])).whenComplete((replyGS, errorGS) -> {
                                try {
                                    if (errorGS != null) {
                                        throw errorGS;
                                    }
                                    this.commit(conn, message, new JsonObject().put("success", true));
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
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

    private void getToHitch(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer terminalId = body.getInteger(TERMINAL_ID);
        this.dbClient.queryWithParams(QUERY_GET_TRAILERS_TO_HITCH, new JsonArray().add(terminalId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                message.reply(new JsonArray(reply.result().getRows()));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void removeOfRoute(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int scheduleRouteId = body.getInteger(SCHEDULE_ROUTE_ID);
            int trailerId = body.getInteger(TRAILER_ID);
            int terminalId = body.getInteger(TERMINAL_ID);
            int createdBy = body.getInteger(CREATED_BY);

            List<Future> futures = new ArrayList<>();
            futures.add(checkFirstStepOfRoute(scheduleRouteId, terminalId));
            futures.add(checkTrailerIsAssigned(scheduleRouteId, trailerId));
            futures.add(checkIfTrailerHasPackages(scheduleRouteId, trailerId));

            CompositeFuture.all(futures).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    startTransaction(message, conn -> {
                        try {
                            List<CompletableFuture<Boolean>> futuresTransacction = new ArrayList<>();
                            futuresTransacction.add(deleteTrailerOfRoute(conn, scheduleRouteId, trailerId));
                            futuresTransacction.add(setTrailerInUse(conn, trailerId, false, createdBy));
                            CompletableFuture.allOf(futuresTransacction.toArray(new CompletableFuture[futuresTransacction.size()])).whenComplete((replyGS, errorGS) -> {
                                try {
                                    if (errorGS != null) {
                                        throw errorGS;
                                    }
                                    this.commit(conn, message, new JsonObject().put("success", true));
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
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

    private Future<Boolean> checkFirstStepOfRoute(Integer scheduleRouteId, Integer terminalId) {
        Future<Boolean> future = Future.future();
        this.dbClient.queryWithParams(QUERY_CHECK_IF_IS_FIRST_STEP_OF_ROUTE, new JsonArray().add(scheduleRouteId).add(terminalId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    throw new Exception("The terminal not is the first step of the route");
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Boolean> checkTrailerIsAssigned(Integer scheduleRouteId, Integer trailerId) {
        Future<Boolean> future = Future.future();
        this.dbClient.queryWithParams(QUERY_CHECK_IF_TRAILER_IS_ASSIGNED, new JsonArray().add(scheduleRouteId).add(trailerId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    throw new Exception("The trailer is not assigned to the route");
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Boolean> checkIfTrailerHasPackages(Integer scheduleRouteId, Integer trailerId) {
        Future<Boolean> future = Future.future();
        this.dbClient.queryWithParams(QUERY_CHECK_IF_TRAILER_HAS_PACKAGES, new JsonArray().add(scheduleRouteId).add(trailerId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                if (!reply.result().getRows().isEmpty()) {
                    throw new Exception("The trailer cannot be deleted from the route, it has loaded packages");
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.fail(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> deleteTrailerOfRoute(SQLConnection conn, Integer scheduleRouteId, Integer terminalId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        conn.updateWithParams("DELETE FROM shipments_trailers WHERE schedule_route_id = ? AND trailer_id = ?;", new JsonArray().add(scheduleRouteId).add(terminalId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    //</editor-fold>
    private final static String QUERY_CHECK_PLATE = "SELECT * FROM trailers WHERE plate = ? AND status = 1 ";

    private final static String QUERY_GET_TRAILER_BY_ID = "SELECT id,\n" +
            "    c_SubTipoRem_id,\n" +
            "    plate,\n" +
            "    in_use,\n" +
            "    status,\n" +
            "    created_at,\n" +
            "    created_by,\n" +
            "    updated_at,\n" +
            "    updated_by\n" +
            "FROM trailers WHERE id = ?";

    private final static String QUERY_GET_LIST = "SELECT id,\n" +
            "    name,\n" +
            "    economic_number,\n" +
            "    serial_number,\n" +
            "    c_SubTipoRem_id,\n" +
            "    plate,\n" +
            "    in_use,\n" +
            "    status,\n" +
            "    created_at,\n" +
            "    created_by,\n" +
            "    updated_at,\n" +
            "    updated_by\n" +
            "FROM trailers WHERE status != 3 ORDER BY in_use DESC";

    private final static String QUERY_GET_AVAILABLE_LIST = "SELECT id,\n" +
            "    name,\n" +
            "    economic_number,\n" +
            "    serial_number,\n" +
            "    c_SubTipoRem_id,\n" +
            "    plate,\n" +
            "    in_use,\n" +
            "    status,\n" +
            "    created_at,\n" +
            "    created_by,\n" +
            "    updated_at,\n" +
            "    updated_by\n" +
            "FROM trailers WHERE in_use IS FALSE AND status = 1";

    private final static String QUERY_GET_HITCHED = "SELECT\n" +
            "   t.id,\n" +
            "   t.name,\n" +
            "   t.economic_number,\n" +
            "   t.serial_number,\n" +
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

    private final static String QUERY_GET_TRAILER = "SELECT id,\n" +
            "    name,\n" +
            "    economic_number,\n" +
            "    serial_number,\n" +
            "    c_SubTipoRem_id,\n" +
            "    plate,\n" +
            "    in_use,\n" +
            "    status,\n" +
            "    created_at,\n" +
            "    created_by,\n" +
            "    updated_at,\n" +
            "    updated_by\n" +
            "FROM trailers WHERE id = ?";

    private final static String QUERY_GET_CURRENT_ROUTE_BY_TRAILER = "SELECT\n" +
            "    cr.name,\n" +
            "   bo.prefix origin_prefix,\n" +
            "   bd.prefix destiny_prefix\n" +
            "FROM trailers t\n" +
            "INNER JOIN shipments_trailers st ON st.trailer_id = t.id\n" +
            "   AND st.action IN ('assign', 'hitch', 'transfer') AND st.latest_movement IS TRUE\n" +
            "INNER JOIN schedule_route sr ON sr.id = st.schedule_route_id\n" +
            "   AND sr.schedule_status NOT IN ('canceled', 'scheduled', 'finished-ok')\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "   AND srd.destination_status NOT IN ('canceled', 'scheduled', 'finished-ok')\n" +
            "INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "   AND cd.order_destiny = cd.order_origin + 1\n" +
            "INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "WHERE t.id = ?\n" +
            "ORDER BY sr.travel_date DESC\n" +
            "LIMIT 1;";

    private final static String QUERY_GET_SHIPMENT = "SELECT id,\n" +
            "    schedule_route_id,\n" +
            "    terminal_id,\n" +
            "    shipment_type,\n" +
            "    shipment_status,\n" +
            "    driver_id,\n" +
            "    left_stamp,\n" +
            "    right_stamp,\n" +
            "    additional_stamp,\n" +
            "    replacement_stamp,\n" +
            "    total_tickets,\n" +
            "    total_complements,\n" +
            "    total_parcels,\n" +
            "    total_packages,\n" +
            "    parent_id,\n" +
            "    left_stamp_status,\n" +
            "    right_stamp_status,\n" +
            "    additional_stamp_status,\n" +
            "    replacement_stamp_status,\n" +
            "    created_by,\n" +
            "    created_at,\n" +
            "    updated_by,\n" +
            "    updated_at,\n" +
            "    origin\n" +
            "FROM shipments WHERE id = ?";

    private final static String QUERY_GET_PACKAGES_TO_DOWNLOAD = "SELECT sppt.parcel_id, sppt.parcel_package_id, pp.package_code\n" +
            "FROM shipments_parcel_package_tracking sppt\n" +
            "INNER JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "   AND pp.package_status = " + PACKAGE_STATUS.IN_TRANSIT.ordinal() +" \n" +
            "WHERE sppt.trailer_id = ?\n" +
            "AND s.schedule_route_id = ?\n" +
            "AND sppt.status IN ('loaded', 'transfer')\n" +
            "AND (SELECT sppt2.status FROM shipments_parcel_package_tracking sppt2\n" +
            "   WHERE sppt2.parcel_package_id = sppt.parcel_package_id\n" +
            "    ORDER BY sppt2.id DESC LIMIT 1) NOT IN ('downloaded');";

    private final static String QUERY_GET_PACKAGES_TO_TRANSFER = "SELECT sppt.parcel_id, sppt.parcel_package_id, pp.package_code\n" +
            "FROM shipments_parcel_package_tracking sppt\n" +
            "INNER JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "   AND pp.package_status IN (" +
                PACKAGE_STATUS.IN_TRANSIT.ordinal() +", \n" +
                PACKAGE_STATUS.LOADED.ordinal() +", \n" +
                PACKAGE_STATUS.READY_TO_TRANSHIPMENT.ordinal() +") \n" +
            "WHERE sppt.trailer_id = ?\n" +
            "AND s.schedule_route_id = ?\n" +
            "AND sppt.status IN ('loaded', 'transfer')\n" +
            "AND (SELECT sppt2.status FROM shipments_parcel_package_tracking sppt2\n" +
            "   WHERE sppt2.parcel_package_id = sppt.parcel_package_id\n" +
            "    ORDER BY sppt2.id DESC LIMIT 1) NOT IN ('downloaded');";

    private final static String QUERY_GET_PACKAGES_TO_TRANSHIPMENT = "SELECT sppt.parcel_id, sppt.parcel_package_id, pp.package_code\n" +
            "FROM shipments_parcel_package_tracking sppt\n" +
            "INNER JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "   AND pp.parcel_id = sppt.parcel_id\n" +
            "WHERE sppt.trailer_id = ?\n" +
            "AND s.schedule_route_id = ?\n" +
            "AND sppt.status IN ('loaded', 'transfer')\n" +
            "AND pp.package_status IN (10)";

    private final static String QUERY_GET_BRANCHOFFICE = "SELECT id,\n" +
            "    prefix,\n" +
            "    name,\n" +
            "    description,\n" +
            "    address,\n" +
            "    street_id,\n" +
            "    no_ext,\n" +
            "    no_int,\n" +
            "    suburb_id,\n" +
            "    city_id,\n" +
            "    county_id,\n" +
            "    state_id,\n" +
            "    country_id,\n" +
            "    zip_code,\n" +
            "    reference,\n" +
            "    phone,\n" +
            "    branch_office_type,\n" +
            "    latitude,\n" +
            "    longitude,\n" +
            "    manager_id,\n" +
            "    receive_transhipments,\n" +
            "    transhipment_site_name,\n" +
            "    status,\n" +
            "    created_at,\n" +
            "    created_by,\n" +
            "    updated_at,\n" +
            "    updated_by,\n" +
            "    time_zone,\n" +
            "    time_checkpoint,\n" +
            "    time_manteinance,\n" +
            "    business_segment,\n" +
            "    timezone,\n" +
            "    iva\n" +
            "FROM branchoffice WHERE id = ?;";

    private final static String QUERY_GET_LAST_ACTION_SHIPMENTS_TRAILERS_BY_TRAILER_ID = "SELECT \n" +
            "   st.id, \n" +
            "   st.action,\n" +
            "   t.status,\n" +
            "   t.in_use \n" +
            "FROM shipments_trailers st\n" +
            "INNER JOIN trailers t ON t.id = st.trailer_id\n" +
            "WHERE st.trailer_id = ?\n" +
            "ORDER BY st.id DESC\n" +
            "LIMIT 1;";

    private final static String QUERY_GET_PACKAGES_TO_HITCH = "SELECT DISTINCT\n" +
            "   pp.package_code\n" +
            "FROM shipments_parcel_package_tracking sppt\n" +
            "INNER JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN parcels_packages pp ON pp.id = sppt.parcel_package_id\n" +
            "WHERE sppt.status = 'downloaded'\n" +
            "AND sppt.trailer_id = ?\n" +
            "AND s.terminal_id = ?\n" +
            "AND pp.package_status = 10;";

    private final static String UPDATE_SHIPMENTS_PARCEL_PACKAGE_TRACKING_CLEAN_LATEST_MOVEMENT = "UPDATE shipments_parcel_package_tracking AS sppt\n" +
            "INNER JOIN shipments AS s ON s.id = sppt.shipment_id\n" +
            "INNER JOIN schedule_route AS sr ON sr.id = s.schedule_route_id\n" +
            "   SET sppt.latest_movement = FALSE,\n" +
            "   sppt.updated_by = %d, \n" +
            "   sppt.updated_at = '%s'\n" +
            "WHERE sr.id = %d\n" +
            "   AND sppt.parcel_package_id IN (%s)\n" +
            "   AND sppt.latest_movement IS TRUE;";

    private final static String UPDATE_CLEAN_LATEST_MOVEMENT_SHIPMENTS_TRAILERS = "UPDATE shipments_trailers SET latest_movement = 0, updated_by = ?, updated_at = ? WHERE schedule_route_id = ? AND trailer_id = ? AND latest_movement IS TRUE;";
    private final static String UPDATE_CLEAN_LATEST_MOVEMENT_BY_SHIPMENTS_TRAILERS_ID = "UPDATE shipments_trailers SET latest_movement = 0, updated_by = ?, updated_at = ? WHERE id = ? AND trailer_id = ? AND latest_movement IS TRUE;";

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
            "WHERE s.terminal_id = ?\n" +
            "   AND st.action = 'release_transhipment'\n" +
            "   AND st.id = (SELECT st2.id FROM shipments_trailers st2\n" +
            "               WHERE st2.trailer_id = st.trailer_id order by st2.id desc limit 1)\n" +
            "GROUP BY t.id\n" +
            "ORDER BY st.id DESC;";

    private static final String QUERY_CHECK_IF_IS_FIRST_STEP_OF_ROUTE = "SELECT sr.id FROM schedule_route sr\n" +
            "INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "WHERE sr.id = ?\n" +
            "AND cr.terminal_origin_id = ?";

    private static final String QUERY_CHECK_IF_TRAILER_IS_ASSIGNED = "SELECT * FROM shipments_trailers \n" +
            "WHERE schedule_route_id = ? \n" +
            "AND trailer_id = ? AND action IN ('hitch', 'assign', 'transfer') \n" +
            "AND latest_movement IS TRUE;";

    private static final String QUERY_CHECK_IF_TRAILER_HAS_PACKAGES = "SELECT DISTINCT st.id FROM shipments_trailers st\n" +
            "INNER JOIN shipments s ON s.id = st.shipment_id\n" +
            "INNER JOIN shipments_parcel_package_tracking sppt ON sppt.shipment_id = s.id AND sppt.trailer_id = st.trailer_id\n" +
            "WHERE st.schedule_route_id = ?\n" +
            "AND st.trailer_id = ?;";
}