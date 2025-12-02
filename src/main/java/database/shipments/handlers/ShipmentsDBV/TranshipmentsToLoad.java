package database.shipments.handlers.ShipmentsDBV;

import database.commons.DBHandler;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.shipments.ShipmentsDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static database.shipments.ShipmentsDBV.SCHEDULE_ROUTE_ID;
import static service.commons.Constants.TERMINAL_ID;

public class TranshipmentsToLoad extends DBHandler<ShipmentsDBV> {

    public TranshipmentsToLoad(ShipmentsDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        Integer terminalId = message.body().getInteger(TERMINAL_ID);
        Integer scheduleRouteId = message.body().getInteger(SCHEDULE_ROUTE_ID);
        JsonArray params = new JsonArray()
                .add(terminalId).add(scheduleRouteId)
                .add(scheduleRouteId).add(terminalId);
        this.dbClient.queryWithParams(QUERY_GET_PARCELS_PACKAGES_TO_TRANSHIPMENT, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                List<JsonObject> parcelsDistinct = result.stream().map(p -> new JsonObject()
                            .put("parcel_id", p.getInteger("parcel_id"))
                            .put("parcel_tracking_code", p.getString("parcel_tracking_code"))
                            .put("waybill", p.getString("waybill"))
                            .put("created_at", p.getString("created_at"))
                            .put("customer_full_name", p.getString("customer_full_name"))
                            .put("segment", p.getString("segment"))
                            .put("parcel_status", p.getInteger("parcel_status"))).distinct().collect(Collectors.toList());

                for (JsonObject parcelDistinct : parcelsDistinct) {
                    Integer parcelIdDistinct = parcelDistinct.getInteger("parcel_id");
                    JsonArray packages = new JsonArray();
                    parcelDistinct.put("packages", packages);
                    for (JsonObject parcel : result) {
                        String pptLastAction = parcel.getString("ppt_last_action");
                        if (parcelIdDistinct.equals(parcel.getInteger("parcel_id")) && pptLastAction.equals(PARCELPACKAGETRACKING_STATUS.READY_TO_TRANSHIPMENT.getValue())) {
                            packages.add(new JsonObject()
                                .put("parcel_package_id", parcel.getInteger("parcel_package_id"))
                                .put("shipping_type", parcel.getString("shipping_type"))
                                .put("package_code", parcel.getString("package_code"))
                                .put("package_status", parcel.getInteger("package_status"))
                            );
                        }
                    }
                }

                List<JsonObject> parcelsToTranshipment = parcelsDistinct.stream().filter(p -> !p.getJsonArray("packages").isEmpty()).collect(Collectors.toList());

                message.reply(new JsonObject().put("parcels_to_transhipment", parcelsToTranshipment));
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private final static String QUERY_GET_PARCELS_PACKAGES_TO_TRANSHIPMENT = "SELECT\n" +
            "   p.id AS parcel_id,\n" +
            "   p.parcel_tracking_code,\n" +
            "   p.parcel_status,\n" +
            "   p.waybill,\n" +
            "   p.total_packages,\n" +
            "   CONCAT(bo.prefix, ' - ', bd.prefix) as segment, \n" +
            "   CONCAT(c.first_name, ' ', c.last_name) AS customer_full_name,\n" +
            "   TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date) AS delivery_time,\n" +
            "       pp.id AS parcel_package_id,\n" +
            "   pp.shipping_type,\n" +
            "   pp.package_code,\n" +
            "   pp.package_status,\n" +
            "   p.created_at,\n" +
            "   (SELECT action FROM parcels_packages_tracking WHERE parcel_package_id = pp.id AND action NOT IN ('printed', 'incidence') ORDER BY id DESC LIMIT 1) AS ppt_last_action\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "INNER JOIN customer c ON c.id = p.customer_id\n" +
            "WHERE pp.package_status = 10\n" +
            "   AND ? = (SELECT terminal_id FROM parcels_packages_tracking\n" +
            "   WHERE parcel_package_id = pp.id\n" +
            "   AND action NOT IN ('printed', 'incidence')\n" +
            "   ORDER BY id DESC LIMIT 1)\n" +
            "   AND (SELECT COUNT(DISTINCT srd.terminal_destiny_id) FROM schedule_route sr\n" +
            "      INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            "      WHERE srd.terminal_destiny_id = p.terminal_origin_id\n" +
            "      AND sr.id = ?) = 0\n" +
            "   AND (SELECT cd2.id FROM schedule_route_destination srd \n" +
            "       INNER JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            "       INNER JOIN config_destination cd ON cd.config_route_id = sr.config_route_id\n" +
            "           AND cd.terminal_origin_id = srd.terminal_origin_id \n" +
            "           AND cd.terminal_destiny_id = srd.terminal_destiny_id\n" +
            "           AND cd.order_destiny = cd.order_origin + 1\n" +
            "       INNER JOIN config_destination cd2 ON cd2.config_route_id = sr.config_route_id\n" +
            "           AND cd2.order_origin >= cd.order_origin\n" +
            "           AND cd2.order_destiny = cd2.order_origin + 1\n" +
            "       INNER JOIN branchoffice bot ON bot.id = cd2.terminal_destiny_id\n" +
            "           AND bot.receive_transhipments IS TRUE\n" +
            "       WHERE sr.id = ?\n" +
            "           AND srd.terminal_origin_id = ?) > 0;";

}