package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.parcel.ParcelDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static service.commons.Constants.*;

public class GlobalCommercialPromiseReport extends DBHandler<ParcelDBV> {

    public GlobalCommercialPromiseReport(ParcelDBV dbVerticle) {
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
            getBranchofficeInfo(terminalId).whenComplete((terminals, errGBI) -> {
                try {
                   if(errGBI != null) {
                       throw errGBI;
                   }

                    this.getReportByTerminal(initDate, endDate, terminals.get(0), shipmentTypes).whenComplete((result, error) -> {
                       try {
                           if (error != null){
                               throw error;
                           }

                           message.reply(terminals.get(0));

                       } catch (Throwable t){
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

    private CompletableFuture<List<JsonObject>> getBranchofficeInfo(Integer branchofficeId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        String QUERY = "SELECT id, prefix FROM branchoffice WHERE status = 1 AND branch_office_type = 'T' AND id = " + branchofficeId;
        this.dbClient.query(QUERY, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> terminals = reply.result().getRows();
                if (terminals.isEmpty()) {
                    throw new Exception("Terminals not found");
                }
                future.complete(terminals);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getReportByTerminal(String initDate, String endDate, JsonObject terminal, JsonArray shipmentTypes) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer terminalId = terminal.getInteger(ID);
            getParcelsinfo(initDate, endDate, terminalId, shipmentTypes).whenComplete((parcels, errGPI) -> {
                try {
                    if (errGPI != null) {
                        throw errGPI;
                    }

                    int totalParcels = parcels.size();
                    int finishedParcels, unfinishedParcels;
                    double finishedPercent, unfinishedPercent;
                    if (totalParcels != 0) {
                        finishedParcels = (int) parcels.stream().filter(p -> p.getInteger("on_time").equals(1)).count();
                        unfinishedParcels = totalParcels - finishedParcels;
                        finishedPercent = UtilsMoney.round(((double) finishedParcels / totalParcels) * 100.00, 2);
                        unfinishedPercent = UtilsMoney.round(100.00 - finishedPercent, 2);
                    } else {
                        finishedParcels = 0;
                        unfinishedParcels = 0;
                        finishedPercent = 100.00;
                        unfinishedPercent = 0.00;
                    }

                    terminal
                            .put("sum_parcels_total", totalParcels)
                            .put("sum_finished_percent", finishedPercent)
                            .put("sum_unfinished_percent", unfinishedPercent)
                            .put("sum_parcels_finished", finishedParcels)
                            .put("sum_parcels_unfinished", unfinishedParcels)
                            .put("report", makeReportByOrigin(parcels));

                    future.complete(terminal);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });

        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<List<JsonObject>> getParcelsinfo(String initDate, String endDate, Integer terminalId, JsonArray shipmentTypes) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_GET_PARCELS_INFO;
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
            QUERY = QUERY.concat(" ORDER BY p.delivered_at;");
            dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(reply.result().getRows());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private List<JsonObject> makeReportByOrigin(List<JsonObject> parcels) {
        List<JsonObject> report = new ArrayList<>();
        Map<String, List<JsonObject>> parcelsByOriginPrefix = parcels.stream().collect(groupingBy(p -> p.getString("terminal_origin_prefix")));

        for(Map.Entry<String, List<JsonObject>> prefix : parcelsByOriginPrefix.entrySet()){

            List<JsonObject> parcelsByOrigin = prefix.getValue();
            int totalParcels = parcelsByOrigin.size();
            int finishedParcels, unfinishedParcels;
            double finishedPercent, unfinishedPercent;
            if (totalParcels != 0) {
                finishedParcels = (int) parcelsByOrigin.stream().filter(p -> p.getInteger("on_time").equals(1)).count();
                unfinishedParcels = totalParcels - finishedParcels;
                finishedPercent = UtilsMoney.round(((double) finishedParcels / totalParcels) * 100.00, 2);
                unfinishedPercent = UtilsMoney.round(100.00 - finishedPercent, 2);
            } else {
                finishedParcels = 0;
                unfinishedParcels = 0;
                finishedPercent = 100.00;
                unfinishedPercent = 0.00;
            }

            report.add(new JsonObject()
                    .put("terminal_origin_prefix", prefix.getKey())
                    .put("parcels_total", totalParcels)
                    .put("parcels_finished", finishedParcels)
                    .put("parcels_unfinished", unfinishedParcels)
                    .put("finished_percent", finishedPercent)
                    .put("unfinished_percent", unfinishedPercent));
        }

        return report;
    }

    private static final String QUERY_GET_PARCELS_INFO = "SELECT\n" +
            " p.id,\n" +
            " p.created_at,\n" +
            " p.delivered_at,\n" +
            " p.promise_delivery_date,\n" +
            " p.shipment_type,\n" +
            " CASE\n" +
            "   WHEN DATE(CONVERT_TZ(p.promise_delivery_date, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')) >= DATE(CONVERT_TZ(p.delivered_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"'))\n" +
            "       THEN TRUE ELSE FALSE\n" +
            " END AS on_time,\n" +
            " p.terminal_origin_id,\n" +
            " bo.name AS terminal_origin_name,\n" +
            " bo.prefix AS terminal_origin_prefix\n" +
            "FROM parcels p\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "WHERE p.parcel_status IN (2, 3)\n" +
            "   AND p.is_internal_parcel IS FALSE\n" +
            "   AND p.delivered_at\n" +
            "       BETWEEN CONVERT_TZ(CONCAT(?, ' 00:00:00'), '"+ UtilsDate.getTimeZoneValue() +"', '+00:00')\n" +
            "       AND CONVERT_TZ(CONCAT(?, ' 23:59:59'), '"+ UtilsDate.getTimeZoneValue() +"', '+00:00')\n";

}
