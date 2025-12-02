package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.List;
import java.util.Objects;

import static service.commons.Constants.*;

public class Report extends DBHandler<ParcelsManifestDBV> {

    public Report(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            Integer typeServiceId = body.getInteger(_TYPE_SERVICE_ID);
            Integer cityId = body.getInteger(_CITY_ID);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);

            String QUERY = QUERY_GET_PARCEL_MANIFEST_LIST;
            JsonArray param = new JsonArray()
                    .add(initDate).add(endDate);

            if (Objects.nonNull(cityId)) {
                param.add(cityId);
                QUERY += " AND c.id = ? \n";
            }

            if (Objects.nonNull(branchofficeId)) {
                param.add(branchofficeId);
                QUERY += " AND b.id = ? \n";
            }

            if (Objects.nonNull(typeServiceId)) {
                param.add(typeServiceId);
                QUERY += " AND pm.id_type_service = ? \n";
            }

            QUERY += " GROUP BY pm.id, v.name, pm.folio, pm.drive_name, pm.num_route,\n" +
                    "         pm.init_load_date, pm.finish_load_date, pm.init_route_date, pm.finish_route_date";

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
            "    pm.id AS parcels_manifest_id,\n" +
            "    v.name AS vehicle_name,\n" +
            "    pm.folio,\n" +
            "    pm.drive_name,\n" +
            "    pm.num_route,\n" +
            "    COALESCE((SELECT COUNT(DISTINCT pmd2.id_parcels_rad_ead) FROM parcels_manifest_detail pmd2\n" +
            "       WHERE pmd2.id_parcels_manifest = pm.id AND pmd2.status != "+ PARCEL_MANIFEST_STATUS.CANCELED.ordinal() +"), 0) AS num_parcels,\n" +
            "    COUNT(CASE WHEN pmd.status = "+PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal()+" THEN pmd.id END) AS in_travel_parcels,\n" +
            "    COUNT(CASE WHEN pmd.status = "+PARCEL_MANIFEST_DETAIL_STATUS.DELIVERED.ordinal()+" THEN pmd.id END) AS delivered_parcels,\n" +
            "    COUNT(CASE WHEN pmd.status = "+PARCEL_MANIFEST_DETAIL_STATUS.NOT_DELIVERED.ordinal()+" THEN pmd.id END) AS not_delivered_parcels,\n" +
            "    ROUND(\n" +
            "        (COUNT(CASE WHEN pmd.status = "+PARCEL_MANIFEST_DETAIL_STATUS.DELIVERED.ordinal()+" THEN pmd.id END) * 100.0) / \n" +
            "        NULLIF(COUNT(DISTINCT CASE WHEN pmd.status != "+ PARCEL_MANIFEST_STATUS.CANCELED.ordinal() +" THEN pmd.id_parcels_rad_ead END), 0), \n" +
            "        2\n" +
            "    ) AS effectiveness_percentage,\n" +
            "    pm.init_load_date,\n" +
            "    pm.finish_load_date,\n" +
            "    pm.init_route_date,\n" +
            "    pm.finish_route_date\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN branchoffice b ON b.id = pm.id_branchoffice\n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "   AND pmd.status != "+ PARCEL_MANIFEST_STATUS.CANCELED.ordinal() +"\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN vehicle_rad_ead vre ON vre.id = pm.id_vehicle_rad_ead\n" +
            "INNER JOIN vehicle v ON v.id = vre.id_vehicle\n" +
            "INNER JOIN users u ON u.id = pm.created_by \n" +
            "INNER JOIN employee e ON e.user_id = u.id\n" +
            "WHERE \n" +
            "   CONVERT_TZ(pm.created_at, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')\n " +
            "       BETWEEN CONVERT_TZ(?, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')\n" +
            "           AND CONVERT_TZ(?, '+00:00', '"+ UtilsDate.getTimeZoneValue() +"')\n";

}
