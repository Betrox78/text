package database.promos.handlers.PromosDBV;

import database.commons.DBHandler;
import database.parcel.ParcelsManifestDBV;
import database.promos.PromosDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Objects;

import static service.commons.Constants.*;

public class CustomerPromoReport extends DBHandler<PromosDBV> {

    public CustomerPromoReport(PromosDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer userId = body.getInteger(USER_ID);
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            boolean withoutPlaza = body.getBoolean("without_plaza", false);
            boolean withoutSeller = body.getBoolean("without_seller", false);

            String QUERY = QUERY_CUSTOMER_PROMO_REPORT;
            JsonArray param = new JsonArray();

            if (Objects.nonNull(userId)) {
                param.add(userId);
                if (withoutSeller) {
                    QUERY += " AND (u.id = ? OR u.id IS NULL)\n";
                } else {
                    QUERY += " AND u.id = ?\n";
                }
            } else if(withoutSeller) {
                QUERY += " AND u.id IS NULL\n";
            } else {
                QUERY += " AND u.id IS NOT NULL\n";
            }

            if (Objects.nonNull(branchofficeId)) {
                if (branchofficeId == 0) {
                    if (!withoutPlaza) {
                        QUERY += " AND c.branchoffice_id IS NOT NULL\n";
                    }
                } else {
                    param.add(branchofficeId);
                    if (withoutPlaza) {
                        QUERY += " (AND c.branchoffice_id = ? OR c.branchoffice_id IS NULL)\n";
                    } else {
                        QUERY += " AND c.branchoffice_id = ?\n";
                    }
                }
            } else if(withoutPlaza) {
                QUERY += " AND c.branchoffice_id IS NULL\n";
            } else {
                QUERY += " AND c.branchoffice_id IS NOT NULL\n";
            }

            QUERY += " ORDER BY p.until DESC";
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

    private final static String QUERY_CUSTOMER_PROMO_REPORT = "SELECT\n" +
            "    c.id AS customer_id,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_full_name,\n" +
            "    c.phone AS customer_phone,\n" +
            "    c.email AS customer_email,\n" +
            "    u.id AS seller_id,\n" +
            "    u.name AS seller_name,\n" +
            "    b.name AS branchoffice_name,\n" +
            "    b.prefix AS branchoffice_prefix,\n" +
            "    c.parcel_type AS customer_parcel_type,\n" +
            "    cbinfo.address AS customer_billing_address,\n" +
            "    p.id AS promo_id,\n" +
            "    p.name AS promo_name,\n" +
            "    p.discount_code AS promo_discount_code,\n" +
            "    p.service AS promo_service,\n" +
            "    p.discount_type AS promo_discount_type,\n" +
            "    p.discount AS promo_discount,\n" +
            "    p.apply_to_package_price AS promo_ranges,\n" +
            "    SUBSTRING_INDEX(p.apply_to_package_price, ',', -1) AS promo_max_range,\n" +
            "    pk.max_km AS promo_max_km,\n" +
            "    p.until AS promo_until,\n" +
            "    p.apply_sender_addressee AS promo_apply_sender_addressee,\n" +
            "    p.apply_rad AS promo_apply_rad,\n" +
            "    p.apply_ead AS promo_apply_ead,\n" +
            "    (SELECT GROUP_CONCAT(CONCAT(ppkm.min_km, 'km -', ppkm.max_km, 'km')) from package_price_km ppkm where FIND_IN_SET(ppkm.id, p.apply_to_package_price_distance)) AS promo_package_price_kms,\n" +
            "    (SELECT GROUP_CONCAT(pt.name) FROM package_types pt WHERE pt.status = 1 AND FIND_IN_SET(pt.id, p.apply_to_package_type)) AS promo_package_types\n" +
            "FROM customer c\n" +
            "LEFT JOIN branchoffice AS b ON b.id = c.branchoffice_id\n" +
            "LEFT JOIN users AS u ON u.id = c.user_seller_id\n" +
            "LEFT JOIN (\n" +
            "    SELECT cbinfo.customer_id, cbinfo.address\n" +
            "    FROM customer_billing_information cbinfo\n" +
            "    INNER JOIN (\n" +
            "        SELECT customer_id, MAX(created_at) AS max_created_at\n" +
            "        FROM customer_billing_information\n" +
            "        GROUP BY customer_id\n" +
            "    ) latest_cbinfo ON cbinfo.customer_id = latest_cbinfo.customer_id\n" +
            "                    AND cbinfo.created_at = latest_cbinfo.max_created_at\n" +
            ") cbinfo ON cbinfo.customer_id = c.id\n" +
            "LEFT JOIN customers_promos cp ON cp.customer_id = c.id\n" +
            "LEFT JOIN promos p ON p.id = cp.promo_id\n" +
            "LEFT JOIN (\n" +
            "    SELECT id, max_km\n" +
            "    FROM package_price_km\n" +
            "    WHERE status = 1\n" +
            ") pk ON pk.id = CAST(SUBSTRING_INDEX(p.apply_to_package_price_distance, ',', -1) AS UNSIGNED)\n" +
            "    AND p.apply_to_package_price_distance IS NOT NULL\n" +
            "    AND p.apply_to_package_price_distance <> ''\n" +
            "WHERE p.id IS NOT NULL\n" +
            " AND p.status = 1\n";
}
