package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class ReportDetails extends DBHandler<ParcelsManifestDBV> {

    public ReportDetails(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            JsonArray param = new JsonArray().add(parcelManifestId);

            this.dbClient.queryWithParams(QUERY_GET_PARCEL_MANIFEST_DETAILS, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> results = reply.result().getRows();
                    if (!results.isEmpty()) {
                        List<JsonObject> delivered = results.stream()
                                .filter(p -> PARCEL_MANIFEST_DETAIL_STATUS.DELIVERED.equals(PARCEL_MANIFEST_DETAIL_STATUS.values()[p.getInteger(_PARCEL_MANIFEST_DETAIL_STATUS)]))
                                .collect(Collectors.toList());
                        List<JsonObject> inTransit = results.stream()
                                .filter(p -> PARCEL_MANIFEST_DETAIL_STATUS.OPEN.equals(PARCEL_MANIFEST_DETAIL_STATUS.values()[p.getInteger(_PARCEL_MANIFEST_DETAIL_STATUS)]))
                                .collect(Collectors.toList());
                        List<JsonObject> notDelivered = results.stream()
                                .filter(p -> PARCEL_MANIFEST_DETAIL_STATUS.NOT_DELIVERED.equals(PARCEL_MANIFEST_DETAIL_STATUS.values()[p.getInteger(_PARCEL_MANIFEST_DETAIL_STATUS)]))
                                .collect(Collectors.toList());
                        message.reply(new JsonObject()
                                .put("in_transit", new JsonArray(inTransit))
                                .put("delivered", new JsonArray(delivered))
                                .put("not_delivered", new JsonArray(notDelivered)));
                    } else {
                        message.reply(new JsonObject()
                                .put("in_transit", new JsonArray())
                                .put("delivered", new JsonArray())
                                .put("not_delivered", new JsonArray()));
                    }
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private static final String QUERY_GET_PARCEL_MANIFEST_DETAILS = "SELECT\n" +
            "   pm.id AS parcels_manifest_id,\n" +
            "   pmd.status AS parcel_manifest_detail_status,\n" +
            "    p.waybill,\n" +
            "    p.parcel_tracking_code,\n" +
            "    CONCAT(cd.first_name, ' ', cd.last_name) AS addressee_full_name,\n" +
            "    p.pays_sender,\n" +
            "    p.delivered_at,\n" +
            "    p.parcel_status,\n" +
            "    p.payment_condition,\n" +
            "    COUNT(DISTINCT pp.id) AS total_packages\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN branchoffice b ON b.id = pm.id_branchoffice\n" +
            "INNER JOIN city c ON c.id = b.city_id\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "   AND pmd.status != "+ PARCEL_MANIFEST_DETAIL_STATUS.CANCELED.ordinal() +"\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "LEFT JOIN parcels p ON p.id = pre.parcel_id\n" +
            "   AND p.parcel_status NOT IN ("+ PARCEL_STATUS.CANCELED.ordinal() +")\n" +
            "LEFT JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "   AND pp.package_status NOT IN ("+ PACKAGE_STATUS.CANCELED.ordinal() +")\n" +
            "LEFT JOIN customer cd ON cd.id = p.addressee_id\n" +
            "WHERE \n" +
            "   pm.id = ?\n" +
            "GROUP BY p.id;";

}
