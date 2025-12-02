package database.routes.handlers.TravelTrackingDBV;

import database.commons.DBHandler;
import database.routes.TravelTrackingDBV;
import database.shipments.ShipmentsDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import service.commons.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class RegisterNoTraveled extends DBHandler<TravelTrackingDBV> {

    public RegisterNoTraveled(TravelTrackingDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        message.fail(500, "Service not implement");
    }

    public Future<JsonObject> finishLeftReservations(SQLConnection conn, Integer shipmentId, Integer userId) {
        Future<JsonObject> future= Future.future();

        CompositeFuture.all(
                updateLeftReservations(conn, shipmentId, userId),
                updateLeftComplements(conn, shipmentId, userId)
        ).setHandler(reply -> {
            try {
                if(reply.failed()){
                    throw reply.cause();
                }

                JsonArray tickets = reply.result().resultAt(0);
                JsonArray complements = reply.result().resultAt(1);

                JsonObject result = new JsonObject()
                        .put("tickets", tickets.size())
                        .put("complements", complements.size());

                future.complete(result);
            } catch(Throwable t) {
                t.printStackTrace();
                future.fail(t);
            }
        });

        return future;
    }

    Future<JsonArray> updateLeftReservations(SQLConnection conn, Integer shipmentId, Integer userId) {
        Future<JsonArray> future = Future.future();

        getTickets(shipmentId).setHandler(reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> ticketsResult = reply.result();

                List<Integer> ticketIds = ticketsResult.stream()
                        .map(r -> r.getInteger("id"))
                        .collect(Collectors.toList());


                CompositeFuture ticketUpdates = CompositeFuture.all(
                        ticketIds.stream()
                        .map(id -> updateTicket(conn, id, userId))
                        .collect(Collectors.toList())
                );

                CompositeFuture reservationUpdates = CompositeFuture.all(
                        ticketsResult.stream()
                        .map(ticket -> updateReservation(conn, ticket.getInteger(BOARDINGPASS_ID), ticket.getString(TICKET_TYPE), ticket.getString(TICKET_TYPE_ROUTE), userId))
                        .collect(Collectors.toList())
                );

                CompositeFuture.all(reservationUpdates, ticketUpdates).setHandler(replyUpdates -> {
                    try {
                        if(replyUpdates.failed()) {
                            throw replyUpdates.cause();
                        }

                        future.complete(new JsonArray(ticketIds));
                    } catch (Throwable t) {
                        future.fail(t);
                    }
                });

            }catch (Throwable t) {
                future.fail(t);
            }
        });

        return future;
    }

    Future<JsonArray> updateLeftComplements(SQLConnection conn, Integer shipmentId, Integer userId) {
        Future<JsonArray> future = Future.future();

        getComplements(shipmentId).setHandler(reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<Integer> complementIds = reply.result().stream()
                        .map(c -> c.getInteger("id"))
                        .collect(Collectors.toList());

                List<Future> complementUpdates = complementIds.stream()
                        .map(id -> updateComplement(conn, id, userId))
                        .collect(Collectors.toList());

                CompositeFuture.all(complementUpdates).setHandler(replyUpdates -> {
                    try {
                        if(replyUpdates.failed()) {
                            throw replyUpdates.cause();
                        }

                        future.complete(new JsonArray(complementIds));
                    } catch (Throwable t) {
                        future.fail(t);
                    }
                });

            } catch (Throwable t) {
                future.fail(t);
            }
        });

        return future;
    }

    Future<UpdateResult> updateTicket(SQLConnection conn, Integer ticketId, Integer updatedBy) {
        JsonArray body = new JsonArray()
                .add(4)
                .add(updatedBy)
                .add(ticketId);
        return doUpdate(conn, ShipmentsDBV.UPDATE_TICKET_STATUS, body);
    }

    Future<UpdateResult> updateComplement(SQLConnection conn, Integer complementId, Integer updatedBy) {
        JsonArray body = new JsonArray()
                .add(4)
                .add(updatedBy)
                .add(complementId);
        return doUpdate(conn, ShipmentsDBV.UPDATE_COMPLEMENT_STATUS, body);
    }

    Future<UpdateResult> updateReservation(SQLConnection conn, Integer reservationId, String boarding_pass_type, String ticketTypeRoute, Integer updatedBy) {
        List<String> toStatusPending = new ArrayList<>();
        toStatusPending.add(Constants.TICKET_TYPES.ABIERTO_REDONDO.getValue());
        toStatusPending.add(Constants.TICKET_TYPES.REDONDO.getValue());
        JsonArray body = new JsonArray()
                .add(toStatusPending.contains(boarding_pass_type) && ticketTypeRoute.equals("ida")
                    ? Constants.BOARDING_PASS_STATUS.PENDING_RETURN.getValue()
                    : Constants.BOARDING_PASS_STATUS.FINISHED.getValue())
                .add(updatedBy)
                .add(reservationId);
        return doUpdate(conn, UPDATE_BOARDINGPASS_STATUS, body);
    }

    Future<List<JsonObject>> getTickets(Integer shipmentId) {
        JsonArray params = new JsonArray()
                .add(shipmentId);
        return doQuery(QUERY_LEFT_TICKETS, params);
    }

    Future<List<JsonObject>> getComplements(Integer shipmentId) {
        JsonArray params = new JsonArray()
                .add(shipmentId);
        return doQuery(QUERY_LEFT_COMPLEMENTS, params);
    }

    Future<UpdateResult> doUpdate(SQLConnection conn, String QUERY, JsonArray body) {
        Future<UpdateResult> future = Future.future();

        conn.updateWithParams(QUERY, body, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result());
            } catch (Throwable t) {
                future.fail(t);
            }
        });

        return future;
    }

    Future<List<JsonObject>> doQuery(String QUERY, JsonArray params) {
        Future<List<JsonObject>> future = Future.future();

        dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getRows());
            } catch (Throwable t) {
                future.fail(t);
            }
        });

        return future;
    }

    final String QUERY_LEFT_TICKETS = "SELECT bp.id AS boardingpass_id, bpt.id, bpt.tracking_code, bpt.ticket_status, bp.ticket_type, bpr.ticket_type_route FROM boarding_pass_ticket bpt\n" +
            "INNER JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "INNER JOIN boarding_pass bp ON bp.id = bpr.boarding_pass_id AND bp.boardingpass_status != 0 AND bp.status = 1\n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "INNER JOIN shipments s ON s.id = ? AND s.terminal_id = srd.terminal_destiny_id AND s.schedule_route_id = srd.schedule_route_id\n" +
            "WHERE bpt.ticket_status=1";

    final String QUERY_LEFT_COMPLEMENTS = "SELECT bpc.id, bpc.tracking_code, bpc.complement_status FROM boarding_pass_complement bpc\n" +
            "INNER JOIN boarding_pass_ticket bpt ON bpt.id = bpc.boarding_pass_ticket_id\n" +
            "INNER JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "INNER JOIN boarding_pass bp ON bp.id = bpr.boarding_pass_id AND bp.boardingpass_status != 0 AND bp.status = 1\n" +
            "INNER JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "INNER JOIN shipments s ON s.id = ? AND s.terminal_id = srd.terminal_destiny_id AND s.schedule_route_id = srd.schedule_route_id\n" +
            "WHERE bpc.complement_status=1";;

    final String UPDATE_BOARDINGPASS_STATUS = "UPDATE boarding_pass\n" +
            "SET\n" +
            "boardingpass_status = ? ,\n" +
            "updated_at = CURRENT_TIMESTAMP,\n" +
            "updated_by = ?\n" +
            "WHERE id = ?\n";
}
