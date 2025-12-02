package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.configs.GeneralConfigDBV;
import database.parcel.ParcelDBV;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static service.commons.Constants.*;

public class CommercialPromiseReport extends DBHandler<ParcelDBV> {

    public CommercialPromiseReport(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            boolean oneTerminal = Objects.nonNull(terminalId);
            JsonArray shipmentTypes = body.getJsonArray(_SHIPMENT_TYPE);
            getBranchofficesInfo(terminalId).whenComplete((terminals, errGBI) -> {
                try {
                   if(errGBI != null) {
                       throw errGBI;
                   }

                   List<CompletableFuture<JsonObject>> terminalsTasks = new ArrayList<>();
                    for (int i = 0; i < terminals.size(); i++) {
                        terminalsTasks.add(this.getReportByTerminal(initDate, endDate, terminals, i, shipmentTypes, oneTerminal));
                    }

                   CompletableFuture.allOf(terminalsTasks.toArray(new CompletableFuture[terminals.size()])).whenComplete((result, error) -> {
                       try {
                           if (error != null){
                               throw error;
                           }
                           this.getVertx().eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                   new JsonObject().put("fieldName", "parcel_delivery_target"),
                                   new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyParcelDeliveryTarget -> {
                                       try {
                                           if (replyParcelDeliveryTarget.failed()){
                                               throw replyParcelDeliveryTarget.cause();
                                           }

                                           JsonObject parcelDeliveryTarget = (JsonObject) replyParcelDeliveryTarget.result().body();
                                           Double valuePDT = Double.parseDouble(parcelDeliveryTarget.getString(VALUE));

                                           JsonObject resultData = this.getSumm(terminals);

                                           message.reply(resultData.put("parcel_delivery_target", valuePDT)
                                                   .put("terminals", new JsonArray(terminals))
                                                   .put("totals_per_day", this.getSummPerDay(terminals)));

                                       } catch (Throwable t){
                                           reportQueryError(message, t);
                                       }
                                   });

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

    private CompletableFuture<List<JsonObject>> getBranchofficesInfo(Integer branchofficeId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        String QUERY = "SELECT id, prefix FROM branchoffice WHERE status = 1 AND branch_office_type = 'T' ";
        if (Objects.nonNull(branchofficeId)) {
            QUERY = QUERY.concat("AND id = " + branchofficeId);
        }
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

    private CompletableFuture<JsonObject> getReportByTerminal(String initDate, String endDate, List<JsonObject> terminals, Integer index, JsonArray shipmentTypes, boolean oneTerminal) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject terminal = terminals.get(index);
            Integer terminalId = terminal.getInteger(ID);
            getParcelsinfo(initDate, endDate, terminalId, shipmentTypes).whenComplete((parcels, errGPI) -> {
                try {
                    if (errGPI != null) {
                        throw errGPI;
                    }

                    int totalParcels = parcels.size();

                    if (totalParcels > 0) {
                        int finishedParcels, unfinishedParcels;
                        double finishedPercent, unfinishedPercent;
                        finishedParcels = (int) parcels.stream().filter(p -> p.getInteger("on_time").equals(1)).count();
                        unfinishedParcels = totalParcels - finishedParcels;
                        finishedPercent = UtilsMoney.round(((double) finishedParcels / totalParcels) * 100.00, 2);
                        unfinishedPercent = UtilsMoney.round(100.00 - finishedPercent, 2);
                        terminal
                                .put("sum_parcels_total", totalParcels)
                                .put("sum_finished_percent", finishedPercent)
                                .put("sum_unfinished_percent", unfinishedPercent)
                                .put("sum_parcels_finished", finishedParcels)
                                .put("sum_parcels_unfinished", unfinishedParcels)
                                .put("report", makeDailyReport(initDate, endDate, parcels));
                    } else if (oneTerminal) {
                        terminal
                                .put("sum_parcels_total", 0)
                                .put("sum_finished_percent", 100)
                                .put("sum_unfinished_percent", 0.0)
                                .put("sum_parcels_finished", 0.0)
                                .put("sum_parcels_unfinished", 0.0)
                                .put("report", makeDailyReport(initDate, endDate, parcels));
                    } else {
                        terminals.remove(terminal);
                    }

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
            QUERY = QUERY.concat(" ORDER BY day;");
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

    private List<JsonObject> makeDailyReport(String initDateString, String endDateString, List<JsonObject> parcels) throws ParseException {
        List<JsonObject> report = new ArrayList<>();
        Date initDate = UtilsDate.parse_yyyy_MM_dd(initDateString);
        Date endDate = UtilsDate.parse_yyyy_MM_dd(endDateString);

        while(initDate.before(endDate) || initDate.equals(endDate)) {
            Date currentDate = initDate;
            List<JsonObject> parcelsOfTheDate = parcels.stream()
                    .filter(p -> {
                        try {
                            return p.getString("date").equals(UtilsDate.format_yyyy_MM_dd(currentDate));
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            int totalParcels = parcelsOfTheDate.size();
            int finishedParcels, unfinishedParcels;
            double finishedPercent, unfinishedPercent;
            if (totalParcels != 0) {
                finishedParcels = (int) parcelsOfTheDate.stream().filter(p -> p.getInteger("on_time").equals(1)).count();
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
                    .put("number_day", UtilsDate.getDayOfMonth(currentDate))
                    .put("date", UtilsDate.format_yyyy_MM_dd(currentDate))
                    .put("parcels_total", totalParcels)
                    .put("parcels_finished", finishedParcels)
                    .put("parcels_unfinished", unfinishedParcels)
                    .put("finished_percent", finishedPercent)
                    .put("unfinished_percent", unfinishedPercent));


            Calendar calendar = Calendar.getInstance();
            calendar.setTime(initDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            initDate = calendar.getTime();
        }

        return report;
    }

    private JsonObject getSumm(List<JsonObject> list){
        double totalFinishedPercent;
        int sumTotal, sumFinished, sumUnfinished;

        sumTotal = list.stream().mapToInt(r -> r.getInteger("sum_parcels_total")).sum();
        sumFinished = list.stream().mapToInt(r -> r.getInteger("sum_parcels_finished")).sum();
        sumUnfinished = list.stream().mapToInt(r -> r.getInteger("sum_parcels_unfinished")).sum();
        totalFinishedPercent = sumTotal != 0 ? (sumFinished * 100.00) / sumTotal : 100;

        return new JsonObject().put("sum_finished_percent", Double.parseDouble(String.format("%.2f", totalFinishedPercent)))
                .put("sum_parcels_total", sumTotal)
                .put("sum_parcels_finished", sumFinished)
                .put("sum_parcels_unfinished", sumUnfinished);
    }

    private JsonArray getSummPerDay(List<JsonObject> terminals){

        List<List<Object>> reports = terminals.stream().map(terminal -> terminal.getJsonArray(Constants.REPORT).stream().collect(Collectors.toList())).collect(Collectors.toList());
        Map<Integer, List<JsonObject>> listDays = reports.stream().flatMap(List::stream).map(r -> (JsonObject) r).collect(groupingBy(r -> r.getInteger("number_day")));

        JsonArray resultPerDay = new JsonArray();
        for(Map.Entry<Integer, List<JsonObject>> key : listDays.entrySet()){
            int parcelsTotal = listDays.get(key.getKey()).stream().mapToInt(d -> d.getInteger("parcels_total")).sum();
            int parcelsFinished = listDays.get(key.getKey()).stream().mapToInt(d -> d.getInteger("parcels_finished")).sum();
            int parcelsUnfinished = listDays.get(key.getKey()).stream().mapToInt(d -> d.getInteger("parcels_unfinished")).sum();

            Double totalFinishedPercent = parcelsTotal != 0 ? (parcelsFinished * 100.00) / parcelsTotal : 100;

            JsonObject summDay = new JsonObject()
                    .put("number_day", key.getKey())
                    .put("date", key.getValue().get(0).getString("date"))
                    .put("parcels_total", parcelsTotal)
                    .put("parcels_finished", parcelsFinished)
                    .put("parcels_unfinished", parcelsUnfinished)
                    .put("finished_percent", Double.parseDouble(String.format("%.2f", totalFinishedPercent)));

            resultPerDay.add(summDay);
        }

        return resultPerDay;
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
            " DAY(CONVERT_TZ(p.delivered_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')) day,\n" +
            " DATE(CONVERT_TZ(p.delivered_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')) date\n" +
            "FROM parcels p\n" +
            "WHERE p.parcel_status IN (2, 3)\n" +
            "   AND p.is_internal_parcel IS FALSE\n" +
            "   AND p.delivered_at\n" +
            "       BETWEEN CONVERT_TZ(CONCAT(?, ' 00:00:00'), '"+ UtilsDate.getTimeZoneValue() +"', '+00:00')\n" +
            "       AND CONVERT_TZ(CONCAT(?, ' 23:59:59'), '"+ UtilsDate.getTimeZoneValue() +"', '+00:00')\n";

}
