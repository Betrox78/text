package database.admindashboard;

import database.commons.DBVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

import static service.commons.Constants.*;
import static service.commons.Constants.RESULTS;

public class AdminDashboardDBV extends DBVerticle  {
    public static final String ACTION_GET_SERVICES_TOTALS_BP = "AdminDashboardDBV.getServicesTotalsBp";
    public static final String ACTION_GET_SERVICES_TOTALS_PARCEL = "AdminDashboardDBV.getServicesTotalParcel";
    public static final String ACTION_GET_SERVICES_TOTALS_RENTAL = "AdminDashboardDBV.getServicesTotalsRental";
    public static final String ACTION_GET_SERVICES_TOTALS_FXC_PAID = "AdminDashboardDBV.getParcelFXCpayments";
    public static final String ACTION_GET_SERVICES_TOTALS_GUIAS_PP = "AdminDashboardDBV.getServicesTotalGuiasPP";
    public static final String ACTION_GET_SERVICES_TOTALS_BPP = "AdminDashboardDBV.getServicesTotalBPP";
    public static final String ACTION_GET_PAYMENT_TOTALS = "AdminDashboardDBV.getPaymentTotals";
    public static final String ACTION_GET_SERVICES_TOTALS_FXC_SALES = "AdminDashboardDBV.getParcelFXCsales";
    public static final String ACTION_GET_SERVICES_TOTALS_PARCEL_CREDIT = "AdminDashboardDBV.getParcelCreditSales";
    public static final String ACTION_GET_SERVICES_TOTALS_BP_EXTRAS = "AdminDashboardDBV.getBpExtras";

    private static final String SPECIAL_TICKET_ID_IGNORE = "(28,29,30,38)";
    public static final String CUSTOMERS_NICK_NAMES = "IMSS"; // to ommit or include groups of customers. if neeed add separated by comma, ex. "IMSS,ISSTE"
    private static final String GROUP_BY_SALE_DATE = " GROUP BY sale_date";
    private static final String ORDER_BY_SALE_DATE = " ORDER BY sale_date";
    private static final String GROUP_BY_PAYMENT_DATE = " GROUP BY payment_date";
    private static final String ORDER_BY_PAYMENT_DATE = " ORDER BY payment_date";
    private static final String DEFAULT_TIME_ZONE = "America/Mazatlan";

    @Override
    public String getTableName() { return "adminDashboard"; }

    @Override
    protected void onMessage(Message<JsonObject> message){
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_GET_SERVICES_TOTALS_BP:
                this.getServicesTotalsBp(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_PARCEL:
                this.getServicesTotalParcel(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_RENTAL:
                this.getServicesTotalsRental(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_FXC_PAID:
                this.getParcelFXCpayments(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_GUIAS_PP:
                this.getServicesTotalsGuiasPP(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_BPP:
                this.getServicesTotalsBPP(message);
                break;
            case ACTION_GET_PAYMENT_TOTALS:
                this.getPaymentTotals(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_FXC_SALES:
                this.getParcelFXCsales(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_PARCEL_CREDIT:
                this.getParcelCreditSales(message);
                break;
            case ACTION_GET_SERVICES_TOTALS_BP_EXTRAS:
                this.getBpExtras(message);
                break;
        }
    }

    private void getServicesTotalsBp(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        String QUERY = QUERY_GET_BP_TOTALS_BY_DAY;
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND bp.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getServicesTotalParcel(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        Boolean ommitCustomerCompanyNickname = body.getBoolean("ommit_customer_company_nicknames");
        String customerCompanyNickname = body.getString("customer_company_nick_name");
        String type = body.getString("type");
        String timeZone = body.getString("time_zone");
        String QUERY;

        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }

        params.add(timeZone).add(initDate).add(endDate);

        if(body.getBoolean("ommit_customer_company_nicknames")) {
            if(type == "parcel") {
                QUERY = QUERY_GET_PARCEL_TOTALS_BY_DAY;
            } else {
                QUERY = QUERY_GET_COURIER_TOTALS_BY_DAY;
            }
        } else {
            // get all type of parcels because is an agreement with the customer, like IMSS
            QUERY = QUERY_GET_PARCEL_TOTALS_CUSTOMER_AGREEMENT;
        }

        if(branchOffice != null) {
            QUERY += "  AND p.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        if(customerCompanyNickname != null) {
            String[] nickNames = customerCompanyNickname.split(",");
            StringBuilder placeholders = new StringBuilder();

            for(int i = 0 ; i < nickNames.length ; i++ ){
                String nickname = nickNames[i].trim();
                if (!nickname.isEmpty()) {
                    params.add(nickname);
                    placeholders.append("?");
                    if (i < nickNames.length - 1) {
                        placeholders.append(",");
                    }
                }
            }
            QUERY +=" AND (c.company_nick_name " + (ommitCustomerCompanyNickname ? "NOT IN" : "IN") +
                    "(" + placeholders +") OR c.company_nick_name " + (ommitCustomerCompanyNickname ? "IS NULL" : "IS NOT NULL") + ")";
        }

        QUERY += " AND p.total_amount > 0";
        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getServicesTotalsRental(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        String QUERY = QUERY_GET_RENTAL_TOTALS_BY_DAY;
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND r.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getServicesTotalsGuiasPP(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        String QUERY = QUERY_GET_GUIAPP_TOTALS_BY_DAY;
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND pp.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += " AND pp.total_amount > 0";
        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getServicesTotalsBPP(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        String QUERY = QUERY_GET_BPP_TOTALS_BY_DAY;
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND ppt.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getParcelFXCpayments(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String QUERY = QUERY_GET_PARCEL_FXC_PAID_BY_DAY;
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND p.terminal_destiny_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += " AND p.total_amount > 0";
        QUERY += GROUP_BY_PAYMENT_DATE;
        QUERY += ORDER_BY_PAYMENT_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getPaymentTotals(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        JsonArray paymentMethods = body.getJsonArray("payment_methods");
        String timeZone = body.getString("time_zone");
        String QUERY = QUERY_GET_PAYMENT_TOTALS_BY_METHODS;
        String EXTRA_JOINS_FOR_BRANCHOFFICE = "";
        String OTHER_PARAMETERS = "";

        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }

        params.add(timeZone).add(initDate).add(endDate);

        if(!paymentMethods.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();

            for(int i = 0 ; i < paymentMethods.size() ; i++ ){
                String method = paymentMethods.getString(i).trim();
                if (!method.isEmpty()) {
                    params.add(method);
                    placeholders.append("?");
                    if (i < paymentMethods.size() - 1) {
                        placeholders.append(",");
                    }
                }
            }
            OTHER_PARAMETERS +=" AND pm.alias IN (" + placeholders + ")";
        }

        if(branchOffice != null) {
            EXTRA_JOINS_FOR_BRANCHOFFICE = " INNER JOIN tickets t ON p.ticket_id = t.id\n" +
                    " INNER JOIN cash_out co ON t.cash_out_id = co.id\n";
            OTHER_PARAMETERS += " AND co.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        if(body.getBoolean("requieres_extra_conditions")) {
            switch(body.getString("payment_method_group")) {
                case "cards":
                    OTHER_PARAMETERS += " AND (bp.conekta_order_id IS NULL OR bp.id IS NULL)\n";
                    break;
                case "conekta":
                    OTHER_PARAMETERS += " AND bp.conekta_order_id IS NOT NULL\n";
                    break;
            }
        }

        QUERY = QUERY.replace("{EXTRA_JOINS_FOR_BRANCHOFFICE}", EXTRA_JOINS_FOR_BRANCHOFFICE);
        QUERY = QUERY.replace("{OTHER_PARAMETERS}", OTHER_PARAMETERS);

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getParcelFXCsales(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String QUERY = QUERY_GET_PARCEL_FXC_SOLD_BY_DAY;
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND p.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getParcelCreditSales(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String QUERY = QUERY_GET_PARCEL_CREDIT_SOLD_BY_DAY;
        String timeZone = body.getString("time_zone");
        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            QUERY += "  AND p.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getBpExtras(Message<JsonObject> message){
        JsonObject body = message.body();
        JsonArray params = new JsonArray();
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer branchOffice = body.getInteger("branchoffice_id");
        String timeZone = body.getString("time_zone");
        String EXTRA_JOINS_FOR_BRANCHOFFICE = "";
        String OTHER_PARAMETERS = "";

        if(timeZone == null) {
            timeZone = DEFAULT_TIME_ZONE;
        }
        String QUERY = QUERY_GET_BP_EXTRAS;
        params.add(timeZone).add(initDate).add(endDate);

        if(branchOffice != null) {
            EXTRA_JOINS_FOR_BRANCHOFFICE = " INNER JOIN cash_out co ON t.cash_out_id = co.id\n";
            OTHER_PARAMETERS += " AND co.branchoffice_id = ?\n";
            params.add(branchOffice);
        }

        QUERY += GROUP_BY_SALE_DATE;
        QUERY += ORDER_BY_SALE_DATE;

        QUERY = QUERY.replace("{EXTRA_JOINS_FOR_BRANCHOFFICE}", EXTRA_JOINS_FOR_BRANCHOFFICE);
        QUERY = QUERY.replace("{OTHER_PARAMETERS}", OTHER_PARAMETERS);
        
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch(Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private static final String QUERY_GET_BP_TOTALS_BY_DAY = "SELECT \n" +
            "  DATE(CONVERT_TZ(t.created_at, '+00:00', ?)) as sale_date,\n" +
            "  COALESCE(SUM(t.total - t.extra_charges), 0) as total_amount,\n" +
            "  SUM(\n" +
            "    (SELECT COUNT(bpt.id)\n" +
            "     FROM boarding_pass_ticket bpt\n" +
            "     INNER JOIN boarding_pass_passenger bbpp ON bbpp.id = bpt.boarding_pass_passenger_id\n" +
            "     WHERE bbpp.status = 1 AND bbpp.boarding_pass_id = bp.id\n" +
            "       AND bpt.ticket_status != 0 AND NOT(t.action = 'change' OR (t.action = 'voucher' AND t.total = 0))\n" +
            "       AND bbpp.special_ticket_id NOT IN " + SPECIAL_TICKET_ID_IGNORE +"\n" +
            "    )\n" +
            "  ) as service_count\n" +
            "FROM tickets t\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = t.boarding_pass_id\n" +
            "WHERE bp.boardingpass_status NOT IN (0, 4)\n" +
            "  AND t.created_at BETWEEN ? AND ?\n" +
            "  AND (t.action = 'purchase' OR t.action = 'voucher' OR t.action = 'change')\n";

    private static final String QUERY_GET_BP_EXTRAS = "SELECT \n" +
            "  DATE(CONVERT_TZ(t.created_at, '+00:00', ?)) as sale_date,\n" +
            "  COALESCE(SUM(t.extra_charges), 0) as total_amount\n" +
            "FROM tickets t\n" +
            " {EXTRA_JOINS_FOR_BRANCHOFFICE}\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = t.boarding_pass_id\n" +
            "WHERE bp.boardingpass_status NOT IN (0, 4)\n" +
            "  AND t.created_at BETWEEN ? AND ?\n" +
            " {OTHER_PARAMETERS} \n" +
            "  AND (t.action = 'purchase' OR t.action = 'voucher' OR t.action = 'change')\n";

    private static final String QUERY_GET_PARCEL_TOTALS_BY_DAY = "SELECT \n" +
            "    DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) as sale_date,\n" +
            "    SUM(p.total_amount) - IFNULL(SUM(courier_amount.total_courier_amount), 0) as total_amount,\n" +
            "    SUM(parcel_count.parcel_count) as service_count\n" +
            " FROM parcels p\n" +
            " INNER JOIN (\n" +
            "    SELECT \n" +
            "        pp.parcel_id, \n" +
            "        COUNT(pp.id) as parcel_count\n" +
            "    FROM parcels_packages pp\n" +
            "    WHERE pp.shipping_type = 'parcel'\n" +
            "    GROUP BY pp.parcel_id\n" +
            " ) parcel_count ON p.id = parcel_count.parcel_id\n" +
            " LEFT JOIN (\n" +
            "    SELECT \n" +
            "        pp.parcel_id, \n" +
            "        SUM(pp.total_amount) as total_courier_amount\n" +
            "    FROM parcels_packages pp\n" +
            "    WHERE pp.shipping_type = 'courier'\n" +
            "    GROUP BY pp.parcel_id\n" +
            " ) courier_amount ON p.id = courier_amount.parcel_id\n" +
            " LEFT JOIN customer c ON c.id = p.customer_id\n" +
            " WHERE p.parcel_status != 4\n" +
            " AND p.created_at BETWEEN ? AND ?";

    private static final String QUERY_GET_COURIER_TOTALS_BY_DAY = "SELECT \n" +
            "    DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) as sale_date,\n" +
            "    SUM(\n" +
            "        CASE \n" +
            "            WHEN courier_only.is_only_courier = 1 THEN p.total_amount\n" +
            "            ELSE IFNULL(courier_amount.total_courier_amount, 0)\n" +
            "        END\n" +
            "    ) as total_amount,\n" +
            "    SUM(\n" +
            "        CASE \n" +
            "            WHEN courier_only.is_only_courier = 1 THEN courier_only.courier_count\n" +
            "            ELSE courier_amount.courier_count\n" +
            "        END\n" +
            "    ) as service_count\n" +
            " FROM parcels p\n" +
            " INNER JOIN (\n" +
            "    SELECT DISTINCT parcel_id\n" +
            "    FROM parcels_packages\n" +
            "    WHERE shipping_type = 'courier'\n" +
            " ) as courier_parcels ON p.id = courier_parcels.parcel_id\n" +
            " LEFT JOIN (\n" +
            "    SELECT \n" +
            "        pp.parcel_id, \n" +
            "        SUM(pp.total_amount) as total_courier_amount,\n" +
            "        COUNT(pp.id) as courier_count\n" +
            "    FROM parcels_packages pp\n" +
            "    WHERE pp.shipping_type = 'courier'\n" +
            "    GROUP BY pp.parcel_id\n" +
            " ) courier_amount ON p.id = courier_amount.parcel_id\n" +
            " LEFT JOIN (\n" +
            "    SELECT \n" +
            "        parcel_id, \n" +
            "        COUNT(id) as courier_count,\n" +
            "        MAX(CASE WHEN shipping_type = 'courier' THEN 1 ELSE 0 END) as is_only_courier\n" +
            "    FROM parcels_packages\n" +
            "    GROUP BY parcel_id\n" +
            "    HAVING COUNT(DISTINCT shipping_type) = 1\n" +
            " ) courier_only ON p.id = courier_only.parcel_id\n" +
            " LEFT JOIN customer c ON c.id = p.customer_id\n" +
            " WHERE p.parcel_status != 4\n" +
            " AND p.created_at BETWEEN ? AND ?";

    private static final String QUERY_GET_PARCEL_TOTALS_CUSTOMER_AGREEMENT = "SELECT \n" +
            "    DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) AS sale_date,\n" +
            "    SUM(p.total_amount) AS total_amount,\n" +
            "    SUM(pp.package_count) AS service_count\n" +
            "FROM\n" +
            "    parcels p\n" +
            "LEFT JOIN\n" +
            "    (SELECT \n" +
            "        parcel_id, COUNT(id) AS package_count\n" +
            "    FROM\n" +
            "        parcels_packages\n" +
            "    GROUP BY parcel_id) pp ON p.id = pp.parcel_id\n" +
            "LEFT JOIN\n" +
            "    customer c ON c.id = p.customer_id\n" +
            "WHERE\n" +
            "    p.parcel_status != 4\n" +
            "    AND p.created_at BETWEEN ? AND ?";


    private static final String QUERY_GET_RENTAL_TOTALS_BY_DAY = "SELECT \n" +
            "  DATE(CONVERT_TZ(r.created_at, '+00:00', ?)) as sale_date,\n" +
            "  COALESCE(SUM(r.total_amount), 0) as total_amount,\n" +
            "  COUNT(r.id) as service_count\n" +
            " FROM rental AS r\n" +
            " WHERE r.is_quotation = 0 \n" +
            "  AND rent_status != 0\n" +
            "  AND r.created_at BETWEEN ? AND ?";

    private static final String QUERY_GET_GUIAPP_TOTALS_BY_DAY = "SELECT \n" +
            "    DATE(CONVERT_TZ(pp.created_at, '+00:00', ?)) AS sale_date,\n" +
            "    SUM(IF(pp.payment_condition = 'cash', pp.total_amount, 0)) AS total_cash,\n" +
            "    SUM(IF(pp.payment_condition = 'credit', pp.total_amount, 0)) AS total_credit,\n" +
            "    SUM(pp.total_amount) as total_amount,\n" +
            "    SUM(pp.total_count_guipp) AS service_count\n" +
            " FROM parcels_prepaid pp\n" +
            " WHERE pp.parcel_status != 4\n" +
            " AND pp.created_at BETWEEN ? AND ?";

    private static final String QUERY_GET_BPP_TOTALS_BY_DAY = "SELECT \n" +
            "    DATE(CONVERT_TZ(ppt.created_at, '+00:00', ?)) AS sale_date,\n" +
            "    SUM(IF(ppt.payment_condition = 'cash', ppt.total_amount, 0)) AS total_cash,\n" +
            "    SUM(IF(ppt.payment_condition = 'credit', ppt.total_amount, 0)) AS total_credit,\n" +
            "    SUM(ppt.total_amount) as total_amount,\n" +
            "    SUM(ppt.total_tickets) AS service_count\n" +
            " FROM prepaid_package_travel ppt\n" +
            " WHERE ppt.created_at BETWEEN ? AND ? AND ppt.prepaid_status != 2";

    private static final String QUERY_GET_PARCEL_FXC_PAID_BY_DAY = "SELECT \n" +
            "  DATE(CONVERT_TZ(t.created_at, '+00:00', ?)) as payment_date,\n" +
            "  COALESCE(SUM(t.total), 0) as total_payments,\n" +
            "  COUNT(DISTINCT p.id) as number_of_packages\n" +
            " FROM tickets t\n" +
            " INNER JOIN parcels p ON p.id = t.parcel_id\n" +
            " WHERE t.created_at BETWEEN ? AND ?\n" +
            " AND (t.action = 'purchase' OR t.action = 'change')\n" +
            " AND p.pays_sender = 0 AND p.payment_condition = 'cash'";

    private static final String QUERY_GET_PAYMENT_TOTALS_BY_METHODS = "SELECT \n" +
            " DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) AS payment_date,\n" +
            " SUM(p.amount) AS total_amount,\n" +
            " COUNT(p.id) AS payment_count\n" +
            " FROM payment p\n" +
            " INNER JOIN payment_method pm ON p.payment_method_id = pm.id\n" +
            " {EXTRA_JOINS_FOR_BRANCHOFFICE}\n" +
            " LEFT JOIN boarding_pass bp ON p.boarding_pass_id = bp.id\n" +
            " LEFT JOIN rental r ON p.rental_id = r.id\n" +
            " LEFT JOIN parcels_prepaid pp ON p.parcel_prepaid_id = pp.id\n" +
            " LEFT JOIN parcels pa ON p.parcel_id = pa.id\n" +
            " LEFT JOIN prepaid_package_travel ppt ON p.prepaid_travel_id = ppt.id\n" +
            " WHERE p.created_at BETWEEN ? AND ?\n" +
            " AND (bp.id IS NULL OR bp.boardingpass_status != 0)\n" +
            " AND (r.id IS NULL OR r.rent_status != 0)\n" +
            " AND (pp.id IS NULL OR pp.parcel_status != 4)\n" +
            " AND (pa.id IS NULL OR pa.parcel_status != 4)\n" +
            " AND (ppt.id IS NULL OR ppt.prepaid_status != 2)\n" +
            " {OTHER_PARAMETERS} \n" +
            " GROUP BY payment_date\n" +
            " ORDER BY payment_date";

    private static final String QUERY_GET_PARCEL_FXC_SOLD_BY_DAY = "SELECT \n" +
            "  DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) as sale_date,\n" +
            "  COALESCE(SUM(p.total_amount), 0) as total_amount,\n" +
            "  COUNT(DISTINCT p.id) as service_count\n" +
            " FROM parcels p\n" +
            " WHERE p.parcel_status != 4\n" +
            " AND p.created_at BETWEEN ? AND ?\n" +
            " AND p.pays_sender = 0";

    private static final String QUERY_GET_PARCEL_CREDIT_SOLD_BY_DAY = "SELECT \n" +
            "  DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) as sale_date,\n" +
            "  COALESCE(SUM(p.total_amount), 0) as total_amount,\n" +
            "  COUNT(DISTINCT p.id) as service_count\n" +
            " FROM parcels p\n" +
            " WHERE p.parcel_status != 4\n" +
            " AND p.created_at BETWEEN ? AND ?\n" +
            " AND p.pays_sender = 1 AND p.payment_condition = 'credit'";
}
