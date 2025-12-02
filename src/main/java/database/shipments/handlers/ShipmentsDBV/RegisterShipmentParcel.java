package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.shipments.ShipmentsDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import org.checkerframework.checker.units.qual.C;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SHIPMENT_ID;
import static database.vechicle.TrailersDBV.TRAILER_ID;
import static service.commons.Constants.*;
import static service.commons.Constants._EN;

public class RegisterShipmentParcel extends DBHandler<ShipmentsDBV> {
    public static final String ACTION = "ShipmentsDBV.RegisterShipmentParcel";

    public RegisterShipmentParcel(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer trailerId = body.getInteger("trailer_id");
        Integer shipmentId = body.getInteger("shipment_id");
        Integer createdBy = body.getInteger("created_by");
        JsonArray codes = body.getJsonArray("codes");

        if(codes.isEmpty()) {
            replyResult(message, new JsonObject());
            return;
        }

        startTransaction(message, conn -> {
            try {
                List<Future> futures = codes.stream()
                        .map(c -> parseCode(trailerId, shipmentId, (String) c, createdBy))
                        .map(params -> registerParcel(conn, params))
                        .collect(Collectors.toList());

                CompositeFuture.all(futures).setHandler(reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }

                        JsonArray resultCodes = new JsonArray(reply.result().list());
                        JsonArray wrongCodes =  getInvalidCodes(resultCodes);

                        commitTransaction(message, conn, new JsonObject()
                                .put("codes_with_error", wrongCodes)
                        );
                    } catch (Throwable t) {
                        t.printStackTrace();
                        rollbackTransaction(message, conn, t);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                rollbackTransaction(message, conn, t);
            }
        });
    }

    JsonArray getInvalidCodes(JsonArray list) {
        List<JsonObject> codes = list.stream()
                .map(r -> (JsonObject)r)
                .filter(r -> r.getBoolean("is_error"))
                .collect(Collectors.toList());
        return new JsonArray(codes);
    }

    JsonObject parseCode(Integer trailerId, Integer shipmentId, String code, Integer createdBy) {
        return new JsonObject()
                .put(TRAILER_ID, trailerId)
                .put(SHIPMENT_ID, shipmentId)
                .put("code", code)
                .put(CREATED_BY, createdBy);
    }

    Future<JsonObject> registerParcel(SQLConnection conn, JsonObject params) {
       Integer trailerId = params.getInteger(TRAILER_ID);
       Integer shipmentId = params.getInteger(SHIPMENT_ID);
       String parcelCode = params.getString("code");
       Integer createdBy = params.getInteger(CREATED_BY);

       Future<JsonObject> future = Future.future();

       validateParcel(conn, shipmentId, parcelCode, trailerId).setHandler(replyValidation -> {
           try {
               if(replyValidation.failed()) {
                   throw replyValidation.cause();
               }

               JsonObject validationResult = replyValidation.result();
               Integer parcelId = validationResult.getInteger("parcel_id");
               boolean isValid = validationResult.getBoolean("is_valid");
               boolean isRegistered = validationResult.getBoolean("is_registered");
               boolean isCanceled = validationResult.getBoolean("is_canceled");
               boolean hasMissingPackages = validationResult.getBoolean("has_missing_packages");
               boolean isError = !isValid || isRegistered || !hasMissingPackages || isCanceled;
               JsonObject result = new JsonObject()
                       .put("TYPE", "WAYBILL")
                       .put("CODE", parcelCode)
                       .put("is_error", isError);

               if(isError) {
                   if(isCanceled) {
                       result.put(_MESSAGE, new JsonObject()
                                       .put(_ES, "La carta porte está cancelada")
                                       .put(_EN, "The waybill was canceled"))
                               .put("CAUSE", "THE WAYBILL WAS CANCELED");
                   } else if(!isValid) {
                       result.put(_MESSAGE, new JsonObject()
                                       .put(_ES, "La terminal de origen no es la misma que la de embarque")
                                       .put(_EN, "The terminal origin not equals to the load terminal"))
                               .put("CAUSE", "THE TERMINAL ORIGIN NOT EQUALS TO THE LOAD TERMINAL");
                   } else if(isRegistered) {
                       result.put(_MESSAGE, new JsonObject()
                                       .put(_ES, "La carta porte ya fue registrada")
                                       .put(_EN, "The waybill is already registered"))
                               .put("CAUSE", "THE WAYBILL IS ALREADY REGISTERED");
                   } else {
                       result.put(_MESSAGE, new JsonObject()
                                       .put(_ES, "La carta porte no tiene paquetes por subir")
                                       .put(_EN, "The waybill not have missing packages"))
                               .put("CAUSE", "THE WAYBILL NOT HAVE MISSING PACKAGES");
                   }
               }

               JsonObject body = new JsonObject()
                       .put("shipment_id", shipmentId)
                       .put("parcel_id", parcelId)
                       .put("created_by", createdBy);
               if (Objects.nonNull(trailerId)) {
                   body.put(TRAILER_ID, trailerId);
               }

               GenericQuery genericQuery = generateGenericCreate("shipments_parcels", body);

               conn.updateWithParams(genericQuery.getQuery(), genericQuery.getParams(), reply -> {
                   try {
                       if(reply.failed()) {
                           throw reply.cause();
                       }

                       future.complete(result);
                   }catch (Throwable t) {
                       future.fail(t);
                   }
               });
           } catch (Throwable t) {
               future.fail(t);
           }
       });

       return future;
    }

    Future<JsonObject> validateParcel(SQLConnection conn, Integer shipmentId, String parcelCode, Integer trailerId) {
        Future<JsonObject> future = Future.future();
        String QUERY;
        JsonArray params = new JsonArray()
                .add(shipmentId)
                .add(parcelCode);
        if (Objects.nonNull(trailerId)) {
            QUERY = QUERY_VALIDATE_PARCEL.replace(
                    "{SHIPMENT_PARCELS_TRAILER_CONDITION}",
                    "AND sp.trailer_id = " + trailerId);
        } else {
            QUERY = QUERY_VALIDATE_PARCEL.replace("{SHIPMENT_PARCELS_TRAILER_CONDITION}", "");
        }
        conn.queryWithParams(QUERY, params, reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }

               List<JsonObject> resultList = reply.result().getRows();

               if(resultList.isEmpty()) {
                   future.complete(new JsonObject()
                           .put("is_valid", false)
                           .put("is_registered", false)
                           .put("is_canceled", false)
                           .put("has_missing_packages", false)
                   );
               }
               JsonObject result = resultList.get(0).copy();
               result
                   .put("is_valid", result.getInteger("is_valid") == 1)
                   .put("is_registered", result.getInteger("is_registered") == 1)
                   .put("is_canceled", result.getInteger("is_canceled") == 1)
                   .put("has_missing_packages", result.getInteger("has_missing_packages") == 1);

               if(Objects.isNull(trailerId)) {
                   future.complete(result);
               } else {
                   Integer scheduleRouteId = result.getInteger(_SCHEDULE_ROUTE_ID);
                   validateTrailer(scheduleRouteId, trailerId).whenComplete((resVT, errVT) -> {
                       try {
                           if (errVT != null) {
                               throw errVT;
                           }
                           future.complete(result);
                       } catch (Throwable t) {
                           future.fail(t);
                       }
                   });
               }
           } catch(Throwable t) {
              future.fail(t);
           }
        });
        return future;
    }

    private CompletableFuture<Boolean> validateTrailer(Integer scheduleRouteId, Integer trailerId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            this.dbClient.queryWithParams(QUERY_VALIDATE_SHIPMENT_TRAILER, new JsonArray().add(scheduleRouteId).add(trailerId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    if (reply.result().getRows().isEmpty()) {
                        throw new Exception("Trailer is not assigned to the route or was released or transferred");
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

    final String  QUERY_VALIDATE_PARCEL = "SELECT\n" +
            "   p.id AS parcel_id,\n" +
            "   s.schedule_route_id,\n" +
            "   IF(p.parcel_status = " + PARCEL_STATUS.CANCELED.ordinal() + ", 1, 0) AS is_canceled,\n" +
            "   IF(p.terminal_origin_id = s.terminal_id \n" +
            "      OR (p.terminal_origin_id != s.terminal_id \n" +
            "          AND (pt.id IS NOT NULL OR\n" +
            "           (SELECT COUNT(pp2.id) FROM parcels_packages pp2 \n" +
            "           WHERE pp2.parcel_id = p.id AND pp2.package_status IN (" +
            "           "+ PACKAGE_STATUS.LOCATED.ordinal() + ", " +
            "           "+ PACKAGE_STATUS.READY_TO_TRANSHIPMENT.ordinal() + ", " +
            "           "+ PACKAGE_STATUS.MERCHANDISE_NOT_RECEIVED.ordinal() +
            "           )) > 0))\n" +
            "      OR (s.terminal_id IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc\n" +
            "                           WHERE bprc.of_branchoffice_id = p.terminal_origin_id AND bprc.status = 1)), 1, 0) AS is_valid,\n" +
            "      IF(sp.id IS NULL, 0, 1) AS is_registered,\n" +
            "   IF((SELECT COUNT(pp2.id) FROM parcels_packages pp2 \n" +
            "       WHERE pp2.parcel_id = p.id AND pp2.package_status IN (" +
            "           "+ PACKAGE_STATUS.DOCUMENTED.ordinal() + ", " +
            "           "+ PACKAGE_STATUS.LOCATED.ordinal() + ", " +
            "           "+ PACKAGE_STATUS.READY_TO_TRANSHIPMENT.ordinal() + ", " +
            "           "+ PACKAGE_STATUS.MERCHANDISE_NOT_RECEIVED.ordinal() +
            "           )) > 0, 1, 0) AS has_missing_packages\n" +
            "FROM parcels p\n" +
            "INNER JOIN shipments s ON s.id = ?\n" +
            "LEFT JOIN shipments_parcels sp ON sp.parcel_id = p.id AND sp.shipment_id = s.id\n" +
            "  {SHIPMENT_PARCELS_TRAILER_CONDITION} \n" +
            "LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id\n" +
            "WHERE p.parcel_tracking_code = ?;\n";

    private static final String QUERY_VALIDATE_SHIPMENT_TRAILER = "SELECT st.* FROM shipments_trailers st\n" +
            "WHERE st.schedule_route_id = ? AND st.trailer_id = ?\n" +
            "AND st.latest_movement IS TRUE\n" +
            "AND (st.trailer_id NOT IN (SELECT st2.trailer_id FROM shipments_trailers st2\n" +
            "    WHERE st2.schedule_route_id = st.schedule_route_id\n" +
            "    AND st2.trailer_id = st.trailer_id AND st2.action IN ('release', 'release_transhipment')\n" +
            "    AND st2.latest_movement IS TRUE\n" +
            ") AND st.trailer_id NOT IN(\n" +
            "SELECT st3.transfer_trailer_id FROM shipments_trailers st3\n" +
            "    WHERE st3.schedule_route_id = st.schedule_route_id\n" +
            "    AND st3.transfer_trailer_id = st.trailer_id\n" +
            "    AND st3.action IN ('release', 'release_transhipment', 'transfer')\n" +
            "    AND st3.latest_movement IS TRUE\n" +
            "    AND st3.transfer_trailer_id NOT IN (\n" +
            "       SELECT st4.trailer_id FROM shipments_trailers st4 \n" +
            "        WHERE st4.schedule_route_id = st3.schedule_route_id \n" +
            "        AND st4.action = 'assign' AND st4.latest_movement IS TRUE)\n" +
            "));";
}
