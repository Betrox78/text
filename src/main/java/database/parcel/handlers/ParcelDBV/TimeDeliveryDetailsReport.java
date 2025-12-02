package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.parcel.ParcelDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import service.commons.FestiveCalendar;
import utils.UtilsDate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class TimeDeliveryDetailsReport extends DBHandler<ParcelDBV> {

    public TimeDeliveryDetailsReport(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            JsonArray shipmentTypes = body.getJsonArray(_SHIPMENT_TYPE);
            getReport(initDate, endDate, terminalId, shipmentTypes).whenComplete((report, errR) -> {
                try {
                   if(errR != null) {
                       throw errR;
                   }
                   message.reply(new JsonArray(report));
               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getReport(String initDate, String endDate, Integer terminalId, JsonArray shipmentTypes) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        String QUERY = QUERY_REPORT;
        JsonArray params = new JsonArray()
                .add(initDate).add(endDate);

        if (Objects.nonNull(terminalId)) {
            QUERY = QUERY.concat("  AND p.terminal_destiny_id = ?\n");
            params.add(terminalId);
        }
        if (Objects.nonNull(shipmentTypes) && !shipmentTypes.isEmpty()) {
            String param = shipmentTypes.stream()
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(", "));
            QUERY = QUERY.concat(String.format("  AND p.shipment_type IN(%s)\n", param));
        }

        QUERY = QUERY.concat(" GROUP BY p.id;");
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                results.forEach(r -> {
                    LocalDate createdAt = LocalDate.parse(r.getString("created_at_tz"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                    LocalDate deliveredAt = LocalDate.parse(r.getString("delivered_at_tz"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                    int realDeliveryTime = FestiveCalendar.getBusinessDifferenceHours(createdAt, deliveredAt);
                    r.put("real_delivery_time", realDeliveryTime);
                });

                future.complete(results);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_REPORT = "SELECT\n" +
            "   HOUR(CASE WHEN p.shipment_type IN ('OCU', 'RAD/OCU') THEN ptd.promise_time_ocu ELSE ptd.promise_time_ead END) AS promise_delivery_time,\n" +
            "   TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date) AS real_promise_delivery_time,\n" +
            "   TIMESTAMPDIFF(HOUR, DATE(CONVERT_TZ(p.created_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')), DATE(CONVERT_TZ(p.delivered_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"'))) AS real_delivery_time,\n" +
            "   CONVERT_TZ(p.created_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"') AS created_at_tz,\n" +
            "   CONVERT_TZ(p.delivered_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"') AS delivered_at_tz ,\n" +
            "   bo.prefix AS origin_prefix,\n" +
            "   bd.prefix AS destiny_prefix,\n" +
            "   p.waybill,\n" +
            "   p.parcel_tracking_code,\n" +
            "   p.created_at  AS documented_date,\n" +
            "   fsr.travel_date AS travel_date,\n" +
            "   ftt.created_at AS real_travel_date,\n" +
            "   lsr.arrival_date AS arrival_date,\n" +
            "   appt.created_at AS real_arrival_date,\n" +
            "   p.promise_delivery_date,\n" +
            "   p.delivered_at\n" +
            "FROM parcels p\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "LEFT JOIN package_terminals_distance ptd ON promise_time_ocu IS NOT NULL AND promise_time_ead IS NOT NULL\n" +
            "   AND (ptd.terminal_origin_id = bo.id AND ptd.terminal_destiny_id = bd.id\n" +
            "       OR ptd.terminal_origin_id = bd.id AND ptd.terminal_destiny_id = bo.id)\n" +
            "LEFT JOIN shipments fs ON fs.id = \n" +
            "   (SELECT sppt.shipment_id FROM shipments_parcel_package_tracking sppt \n" +
            "       WHERE sppt.parcel_id = p.id AND sppt.status = 'loaded' ORDER BY sppt.id LIMIT 1)\n" +
            "LEFT JOIN schedule_route fsr ON fsr.id = fs.schedule_route_id\n" +
            "LEFT JOIN travel_tracking ftt ON ftt.id = \n" +
            "   (SELECT tt.id FROM travel_tracking tt WHERE tt.schedule_route_id = fsr.id \n" +
            "       AND tt.status = 'in-transit' ORDER BY tt.id LIMIT 1)\n" +
            "LEFT JOIN shipments ls ON ls.id = \n" +
            "   (SELECT sppt.shipment_id FROM shipments_parcel_package_tracking sppt \n" +
            "       WHERE sppt.parcel_id = p.id AND sppt.status = 'downloaded' ORDER BY sppt.id DESC LIMIT 1)\n" +
            "LEFT JOIN schedule_route lsr ON lsr.id = ls.schedule_route_id\n" +
            "LEFT JOIN parcels_packages_tracking appt ON appt.id = \n" +
            "   (SELECT ppt.id FROM parcels_packages_tracking ppt WHERE ppt.parcel_id = p.id \n" +
            "       AND ppt.action = 'arrived' ORDER BY ppt.id LIMIT 1)\n" +
            "WHERE p.is_internal_parcel IS FALSE\n" +
            "   AND p.parcel_status IN (2, 3)\n" +
            "   AND (p.promise_delivery_date IS NULL OR DATE(CONVERT_TZ(p.promise_delivery_date, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')) < DATE(CONVERT_TZ(p.delivered_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')))\n" +
            "   AND p.delivered_at BETWEEN ? AND ?\n";

}
