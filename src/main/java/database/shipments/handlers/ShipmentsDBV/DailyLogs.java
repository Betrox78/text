package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static database.shipments.ShipmentsDBV.*;

public class DailyLogs extends DBHandler<ShipmentsDBV> {

    DailyLogsDetail dailyLogsDetail;
    public DailyLogs(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
        dailyLogsDetail = new DailyLogsDetail(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message){
        JsonObject body = message.body();
        String initTravelDate = body.getString("init_travel_date");
        String endTravelDate = body.getString("end_travel_date");
        String QUERY = QUERY_GET_DAILY_LOGS_LIST;

        QUERY = QUERY.replace("{DATE_PARAMETER}",
                "load_ship.created_at BETWEEN ? AND ?");

        JsonArray params = new JsonArray().add(initTravelDate).add(endTravelDate);

        if (body.getInteger(CONFIG_ROUTE_ID) != null){
            int configRouteId = body.getInteger(CONFIG_ROUTE_ID);
            QUERY = QUERY.concat(DAILY_LOGS_PARAM_CONFIG_ROUTE_ID);
            params.add(configRouteId);
        }

        if (body.getBoolean(IS_PARCEL_ROUTE) != null){
            boolean isParcelRoute = body.getBoolean(IS_PARCEL_ROUTE);
            QUERY = QUERY.concat(DAILY_LOGS_PARAM_IS_PARCEL_ROUTE);
            params.add(isParcelRoute);
        }

        if(body.getInteger(TERMINAL_ORIGIN_ID) != null){
            int terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
            QUERY = QUERY.concat(DAILY_LOGS_PARAM_SCHEDULE_ROUTE_DESTINATION_ORIGIN_ID);
            params.add(terminalOriginId);
        }

        if(body.getInteger("terminal_destiny_id") != null){
            int terminalDestinyId = body.getInteger("terminal_destiny_id");
            QUERY = QUERY.concat(" AND srd.terminal_destiny_id = ? ");
            params.add(terminalDestinyId);
        }

        QUERY = QUERY.concat(" GROUP BY tl.id ");

        this.dbClient.queryWithParams(QUERY, params, replyDailyLogs -> {
            try {
                if (replyDailyLogs.failed()){
                    throw new Exception(replyDailyLogs.cause());
                }
                List<JsonObject> listDailyLogs = replyDailyLogs.result().getRows();
                if (listDailyLogs.isEmpty()){
                    throw new Exception("Elements not found");
                }

                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                listDailyLogs.forEach(dl -> {
                    Integer scheduleRouteId = dl.getInteger(SCHEDULE_ROUTE_ID);
                    Integer orderDestiny = dl.getInteger(ORDER_DESTINY);

                    tasks.add(dailyLogsDetail.getTicketsDailyLogsLoad(dl, scheduleRouteId, null, null, orderDestiny));
                    tasks.add(dailyLogsDetail.getComplementsDailyLogsLoad(dl, scheduleRouteId, null, null, orderDestiny));
                    tasks.add(dailyLogsDetail.getParcelsDailyLogsLoad(dl, null, scheduleRouteId, null, null, orderDestiny, false, true, false));
                    tasks.add(dailyLogsDetail.getPackagesDailyLogsLoad(dl, null, scheduleRouteId, null, null, orderDestiny, true));

                });

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }

                        message.reply(new JsonArray(listDailyLogs));

                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });

            } catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    public static final String QUERY_GET_DAILY_LOGS_LIST = "SELECT\n" +
            "   tl.id AS travel_logs_id,\n" +
            "   tl.travel_log_code AS travel_logs_code,\n" +
            "   sr.id AS schedule_route_id,\n" +
            "   cr.name AS config_route_name,\n" +
            "   sr.travel_date AS schedule_route_travel_date,\n" +
            "   cd.order_origin,\n" +
            "   bo.id AS segment_origin_id,\n" +
            "   bo.prefix AS segment_origin_prefix,\n" +
            "   cd.order_destiny,\n" +
            "   bd.id AS segment_destiny_id,\n" +
            "   bd.prefix AS segment_destiny_prefix,\n" +
            "   cr.terminal_destiny_id AS route_destiny_id,\n" +
            "   rbd.prefix AS route_destiny_prefix,\n" +
            "   v.economic_number AS vehicle_economic_number,\n" +
            "   CONCAT(e.name, ' ', e.last_name) AS driver_name,\n" +
            "   srd.travel_date AS aprox_departed,\n" +
            "   srd.arrival_date AS aprox_arrival,\n" +
            "   srd.started_at AS departed,\n" +
            "   srd.finished_at AS arrived,\n" +
            "   srd.destination_status AS segment_status,\n" +
            "   load_ship.shipment_status AS load_shipment_status,\n" +
            "   download_ship.shipment_status AS download_shipment_status,\n" +
            "   tl.travel_logs_ccp_id as travel_logs_ccp_id,\n" +
            "   tl.has_stamp as travel_has_stamp,\n" +
            "   tl.is_stamped as travel_is_stamped\n" +
            " FROM travel_logs tl\n" +
            " LEFT JOIN schedule_route sr ON sr.id = tl.schedule_route_id\n" +
            " LEFT JOIN shipments load_ship ON load_ship.schedule_route_id = sr.id AND load_ship.id = tl.load_id AND load_ship.shipment_type = 'load'\n" +
            " LEFT JOIN shipments download_ship ON download_ship.schedule_route_id = sr.id AND download_ship.id = tl.download_id AND download_ship.shipment_type = 'download'\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id \n" +
            "   AND srd.terminal_origin_id = tl.terminal_origin_id AND srd.terminal_destiny_id = tl.terminal_destiny_id\n" +
            " INNER JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = tl.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = tl.terminal_destiny_id\n" +
            " LEFT JOIN branchoffice rbd ON rbd.id = cr.terminal_destiny_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN employee e ON e.id = load_ship.driver_id\n" +
            " WHERE " +
            " {DATE_PARAMETER} \n";

    private static final String DAILY_LOGS_PARAM_CONFIG_ROUTE_ID = " AND cr.id = ? ";
    private static final String DAILY_LOGS_PARAM_IS_PARCEL_ROUTE = " AND cr.parcel_route = ? ";

    private static final String DAILY_LOGS_PARAM_SCHEDULE_ROUTE_DESTINATION_ORIGIN_ID = " AND srd.terminal_origin_id = ? ";
}
