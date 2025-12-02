package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.List;
import java.util.Objects;

import static service.commons.Constants.*;

public class Logs extends DBHandler<ParcelsManifestDBV> {

    public Logs(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);

            String QUERY = QUERY_GET_PARCEL_MANIFEST_LIST;
            JsonArray param = new JsonArray()
                    .add(initDate).add(endDate);

            if (Objects.nonNull(branchofficeId)) {
                param.add(branchofficeId);
                QUERY += "   AND pm.id_branchoffice = ?";
            }

            this.dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    message.reply(new JsonArray(results));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private static final String QUERY_GET_PARCEL_MANIFEST_LIST = "SELECT\n" +
            "   pm.id,\n" +
            "   pm.folio,\n" +
            "   pm.status,\n" +
            "   pm.id_type_service,\n" +
            "   pm.drive_name,\n" +
            "   pm.vehicle_serial_num,\n" +
            "   v.alias AS vehicle_alias,\n" +
            "   v.economic_number AS vehicle_economic_number,\n" +
            "   pm.num_route,\n" +
            "   pm.init_load_date,\n" +
            "   pm.finish_load_date,\n" +
            "   pm.init_route_date,\n" +
            "   pm.finish_route_date,\n" +
            "   b.name AS branchoffice_name,\n" +
            "   b.prefix AS branchoffice_prefix\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN branchoffice b ON b.id = pm.id_branchoffice\n" +
            "INNER JOIN vehicle_rad_ead vre ON vre.id = pm.id_vehicle_rad_ead\n" +
            "INNER JOIN vehicle v ON v.id = vre.id_vehicle\n" +
            "WHERE pm.status != "+PARCEL_MANIFEST_STATUS.CANCELED.ordinal()+"\n" +
            "   AND CONVERT_TZ(pm.created_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"') \n" +
            "   BETWEEN CONVERT_TZ(?, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"') AND CONVERT_TZ(?, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')\n";

}
