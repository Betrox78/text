package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.parcel.ParcelDBV;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class SignatureDeliveriesReport extends DBHandler<ParcelDBV> {

    public SignatureDeliveriesReport(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Integer userId = body.getInteger(_USER_ID);
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            String shipmentType = body.getString(_SHIPMENT_TYPE);

            getUsersReport(terminalId, userId, initDate, endDate, shipmentType).whenComplete((resUR, errUR) -> {
                try {
                    if (errUR != null) {
                        throw errUR;
                    }
                    replyResult(message, new JsonArray(resUR));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<List<JsonObject>> getUsersReport(Integer terminalId, Integer userId, String initDate, String endDate, String shipmentType) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        String QUERY = QUERY_GET_USERS_REPORT;
        JsonArray params = new JsonArray()
                .add(terminalId)
                .add(initDate).add(endDate);
        if(Objects.nonNull(shipmentType)) {
            if(shipmentType.equals("OCU/EAD")) {
                QUERY += "AND (INSTR(p.shipment_type, 'OCU') > 0 OR INSTR(p.shipment_type, 'EAD') > 0)\n";
            } else if(shipmentType.equals("OCU")) {
                QUERY += "AND INSTR(p.shipment_type, 'OCU') > 0\n";
            } else if(shipmentType.equals("EAD")) {
                QUERY += "AND INSTR(p.shipment_type, 'EAD') > 0\n";
            }
        }
        if(Objects.nonNull(userId)) {
            QUERY += "AND u.id = ?\n";
            params.add(userId);
        }

        QUERY += "GROUP BY u.id;";
        this.dbClient.queryWithParams(QUERY, params, reply -> {
           try {
               if(reply.failed()) {
                   throw reply.cause();
               }
               List<JsonObject> result = reply.result().getRows();
               future.complete(result);
           } catch (Throwable t) {
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getInfoReport(Integer userId, Integer terminalId, String initDate, String endDate, String shipmentType) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        String QUERY = QUERY_GET_INFO_REPORT;
        JsonArray params = new JsonArray()
                .add(userId).add(terminalId)
                .add(initDate).add(endDate);
        if(Objects.nonNull(shipmentType)) {
            if(shipmentType.equals("OCU/EAD")) {
                QUERY += "AND (INSTR(p.shipment_type, 'OCU') > 0 OR INSTR(p.shipment_type, 'EAD') > 0)\n";
            } else if(shipmentType.equals("OCU")) {
                QUERY += "AND INSTR(p.shipment_type, 'OCU') > 0\n";
            } else if(shipmentType.equals("EAD")) {
                QUERY += "AND INSTR(p.shipment_type, 'EAD') > 0\n";
            }
        }
        QUERY += "GROUP BY p.id;";

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public void detail(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer terminalId = body.getInteger(_TERMINAL_ID);
            Integer userId = body.getInteger(_USER_ID);
            String initDate = body.getString(_INIT_DATE);
            String endDate = body.getString(_END_DATE);
            String shipmentType = body.getString(_SHIPMENT_TYPE);

            getInfoReport(userId, terminalId, initDate, endDate, shipmentType).whenComplete((result, error) -> {
               try {
                   if(error != null) {
                       throw error;
                   }
                   replyResult(message, new JsonArray(result));
               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private static final String QUERY_GET_USERS_REPORT = "SELECT\n" +
            "   u.id,\n" +
            "   u.name,\n" +
            "   pm.folio,\n" +
            "   pm.num_route,\n" +
            "   COUNT(DISTINCT p.id) AS total_delivered,\n" +
            "   COUNT(DISTINCT IF(pd.signature IS NOT NULL, p.id, NULL)) AS total_signature_deliveries,\n" +
            "   COUNT(DISTINCT IF(pd.signature IS NULL, p.id, NULL)) AS total_deliveries_without_signature\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "INNER JOIN customer co ON co.id = p.sender_id\n" +
            "INNER JOIN customer cd ON cd.id = p.addressee_id\n" +
            "INNER JOIN parcels_deliveries pd ON pd.id = pp.parcels_deliveries_id\n" +
            "INNER JOIN users u ON u.id = pd.created_by\n" +
            "LEFT JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "LEFT JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "LEFT JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest\n" +
            "WHERE bd.id = ?\n" +
            "  AND p.delivered_at BETWEEN ? AND ?\n";

    private static final String QUERY_GET_INFO_REPORT = "SELECT\n" +
            "   p.id,\n" +
            "    bo.prefix AS origin_prefix,\n" +
            "    CONCAT(co.first_name, ' ', co.last_name) AS customer_origin_full_name,\n" +
            "    bd.prefix AS destiny_prefix,\n" +
            "    CONCAT(cd.first_name, ' ', cd.last_name) AS customer_destiny_full_name,\n" +
            "    p.parcel_tracking_code,\n" +
            "    p.created_at,\n" +
            "    p.delivered_at,\n" +
            "    p.shipment_type,\n" +
            "    pd.signature,\n" +
            "    pm.folio,\n" +
            "    pm.num_route\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "INNER JOIN customer co ON co.id = p.sender_id\n" +
            "INNER JOIN customer cd ON cd.id = p.addressee_id\n" +
            "INNER JOIN parcels_deliveries pd ON pd.id = pp.parcels_deliveries_id\n" +
            "INNER JOIN users u ON u.id = pd.created_by\n" +
            "LEFT JOIN parcels_rad_ead pre ON pre.parcel_id = p.id\n" +
            "LEFT JOIN parcels_manifest_detail pmd ON pmd.id_parcels_rad_ead = pre.id\n" +
            "LEFT JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest\n" +
            "WHERE u.id = ?\n" +
            "   AND bd.id = ?\n" +
            "   AND p.delivered_at BETWEEN ? AND ?\n";

}
