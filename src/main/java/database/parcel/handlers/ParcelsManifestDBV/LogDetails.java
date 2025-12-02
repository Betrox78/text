package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static service.commons.Constants.*;

public class LogDetails extends DBHandler<ParcelsManifestDBV> {

    public LogDetails(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);

            this.dbClient.queryWithParams(QUERY_GET_PARCEL_MANIFEST, new JsonArray().add(parcelManifestId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Parcel manifest not found");
                    }
                    JsonObject parcelManifest = results.get(0);

                    this.dbClient.queryWithParams(QUERY_GET_PARCEL_MANIFEST_DETAILS, new JsonArray().add(parcelManifestId), replyDetails -> {
                        try {
                            if (replyDetails.failed()) {
                                throw replyDetails.cause();
                            }

                            List<JsonObject> details = replyDetails.result().getRows();
                            parcelManifest.put(_DETAILS, new JsonArray(details));

                            message.reply(parcelManifest);
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

    private static final String QUERY_GET_PARCEL_MANIFEST = "SELECT\n" +
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
            "INNER JOIN vehicle_rad_ead vre ON vre.id = pm.id_vehicle_rad_ead\n" +
            "INNER JOIN vehicle v ON v.id = vre.id_vehicle\n" +
            "INNER JOIN branchoffice b ON b.id = pm.id_branchoffice\n" +
            "WHERE pm.id = ?;\n";

    private static final String QUERY_GET_PARCEL_MANIFEST_DETAILS = "SELECT\n" +
            "    pmd.id AS parcels_manifest_detail_id,\n" +
            "    pmd.status,\n" +
            "    c.first_name AS customer_first_name,\n" +
            "    c.last_name AS customer_last_name,\n" +
            "    ca.address,\n" +
            "    ca.reference,\n" +
            "    c.phone,\n" +
            "    p.total_packages,\n" +
            "    p.pays_sender,\n" +
            "    p.payment_condition,\n" +
            "    CASE\n" +
            "       WHEN p.payment_condition = 'credit' THEN 0\n" +
            "       WHEN p.pays_sender = TRUE THEN 0\n" +
            "        ELSE p.total_amount\n" +
            "    END AS total_amount,\n" +
            "    p.parcel_tracking_code,\n" +
            "    pmd.created_at,\n" +
            "    dar.name AS delivery_attemp_reason_name,\n" +
            "    pmd.other_reasons_not_rad_ead,\n" +
            "    p.delivered_at\n" +
            "FROM parcels_manifest_detail pmd\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN parcels p ON pre.parcel_id = p.id\n" +
            "INNER JOIN customer c ON c.id = p.addressee_id\n" +
            "INNER JOIN customer_addresses ca ON ca.id = p.addressee_address_id\n" +
            "LEFT JOIN delivery_attempt_reason dar ON dar.id = pmd.id_reason_no_rad_ead\n" +
            "WHERE pmd.id_parcels_manifest = ?\n" +
            "GROUP BY pmd.id;";
}
