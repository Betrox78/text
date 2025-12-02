/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.money;

import database.commons.DBVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import utils.UtilsDate;

import java.util.List;

import static service.commons.Constants.ACTION;

/**
 *
 * @author daliacarlon
 */
public class ReportDBV extends DBVerticle {

    public static final String ACTION_ALL_SERVICES_REPORT = "ReportDBV.allServicesReport";
    public static final String ACTION_ALL_SERVICES_REPORT_BY_USER = "ReportDBV.allServicesReportByUser";
    public static final String INIT_DATE = "init_date";
    public static final String END_DATE = "end_date";
    private static final String CITY_ID = "city_id";
    private static final String PURCHASE_ORIGIN = "purchase_origin";
    private static final String TERMINAL_ID = "terminal_id";
    private static final String PURCHASE_DATE = "purchase_date";
    private static final String SALES_CREDIT = "sales_credit";
    private static final String CREDIT_BOARDINGPASS = "credit_boardingpass";
    private static final String CREDIT_PARCELS = "credit_parcels";
    private static final String CREDIT_RENTALS = "credit_rentals";

    @Override
    public String getTableName() {
        return "";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_ALL_SERVICES_REPORT:
                this.allServicesReport(message);
                break;
            case ACTION_ALL_SERVICES_REPORT_BY_USER:
                this.allServicesReportByUser(message);
                break;
        }
    }

    private void allServicesReport(Message<JsonObject> message){
        try {
            JsonObject body = message.body();

            String initDate = body.getString(INIT_DATE);
            String endDate = body.getString(END_DATE);
            String tzOffset = UtilsDate.getTimeZoneValue();
            JsonArray params = new JsonArray().add(tzOffset).add(initDate).add(endDate);
            JsonArray paramsAll = new JsonArray().add(tzOffset).add(initDate).add(endDate);

            Future<ResultSet> f1 = Future.future();
            Future<ResultSet> f2 = Future.future();
            Future<ResultSet> f3 = Future.future();
            Future<ResultSet> f4 = Future.future();

            String QUERY_BP = QUERY_REPORT_ALL_SERVICES_BOARDING;
            String QUERY_PARCELS = QUERY_REPORT_ALL_SERVICES_PARCELS;
            String QUERY_RENTALS = QUERY_REPORT_ALL_SERVICES_RENTALS;
            String QUERY_ALL_SERVICES = QUERY_REPORT_ALL_SERVICES;



            if (body.getString(PURCHASE_ORIGIN) != null){
                String purchaseOrigin = body.getString(PURCHASE_ORIGIN);
                params.add(purchaseOrigin);
                paramsAll.add(purchaseOrigin).add(purchaseOrigin).add(purchaseOrigin);
                QUERY_BP += PARAMS_PURCHASE_ORIGIN_BOARDINGPASS;
                QUERY_PARCELS += PARAMS_PURCHASE_ORIGIN_PARCELS;
                QUERY_RENTALS += PARAMS_PURCHASE_ORIGIN_RENTALS;
                QUERY_ALL_SERVICES += PARAMS_PURCHASE_ORIGIN;
            }

            if (body.getInteger(TERMINAL_ID) != null){
                Integer terminalId = body.getInteger(TERMINAL_ID);
                params.add(terminalId);
                paramsAll.add(terminalId);
                QUERY_BP += PARAM_TERMINAL_ID;
                QUERY_PARCELS += PARAM_TERMINAL_ID;
                QUERY_RENTALS += PARAM_TERMINAL_ID;
                QUERY_ALL_SERVICES += PARAM_TERMINAL_ID;
            }

            if (body.getInteger(CITY_ID) != null){
                Integer cityId = body.getInteger(CITY_ID);
                params.add(cityId);
                paramsAll.add(cityId);
                QUERY_BP += PARAM_CITY_ID;
                QUERY_PARCELS += PARAM_CITY_ID;
                QUERY_RENTALS += PARAM_CITY_ID;
                QUERY_ALL_SERVICES += PARAM_CITY_ID;
            }

            QUERY_BP += GROUP_BY_PURCHASE_DATE;
            QUERY_PARCELS += GROUP_BY_PURCHASE_DATE;
            QUERY_RENTALS += GROUP_BY_PURCHASE_DATE;
            QUERY_ALL_SERVICES += GROUP_BY_PURCHASE_DATE;

            this.dbClient.queryWithParams(QUERY_BP, params, f1.completer());
            this.dbClient.queryWithParams(QUERY_PARCELS, params, f2.completer());
            this.dbClient.queryWithParams(QUERY_RENTALS, params, f3.completer());
            this.dbClient.queryWithParams(QUERY_ALL_SERVICES, paramsAll, f4.completer());

            CompositeFuture.all(f1, f2, f3 ,f4).setHandler(reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> boardingPasses = reply.result().<ResultSet>resultAt(0).getRows();
                    List<JsonObject> parcels = reply.result().<ResultSet>resultAt(1).getRows();
                    List<JsonObject> rentals = reply.result().<ResultSet>resultAt(2).getRows();
                    List<JsonObject> allServices = reply.result().<ResultSet>resultAt(3).getRows();

                    allServices.forEach(all -> {
                        String purchaseDate = all.getString(PURCHASE_DATE);
                        for (int i = 0; i < boardingPasses.size(); i ++){
                            JsonObject boardingPass = boardingPasses.get(i);
                            String bpPurchaseDate = boardingPass.getString(PURCHASE_DATE);
                            if (purchaseDate.equals(bpPurchaseDate)){
                                boardingPass.remove(PURCHASE_DATE);
                                all.mergeIn(boardingPass);
                                break;
                            }
                        }

                        for (int i = 0; i < parcels.size(); i ++){
                            JsonObject parcel = parcels.get(i);
                            String pPurchaseDate = parcel.getString(PURCHASE_DATE);
                            if (purchaseDate.equals(pPurchaseDate)){
                                parcel.remove(PURCHASE_DATE);
                                all.mergeIn(parcel);
                                break;
                            }
                        }

                        for (int i = 0; i < rentals.size(); i ++){
                            JsonObject rental = rentals.get(i);
                            String rPurchaseDate = rental.getString(PURCHASE_DATE);
                            if (purchaseDate.equals(rPurchaseDate)){
                                rental.remove(PURCHASE_DATE);
                                all.mergeIn(rental);
                                break;
                            }
                        }

                        all.put(SALES_CREDIT, all.getDouble(CREDIT_BOARDINGPASS,0.0 ) + all.getDouble(CREDIT_PARCELS, 0.0) + all.getDouble(CREDIT_RENTALS, 0.0));
                        all.remove(CREDIT_BOARDINGPASS);
                        all.remove(CREDIT_PARCELS);
                        all.remove(CREDIT_RENTALS);

                    });

                    message.reply(new JsonArray(allServices));

                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }
    private void allServicesReportByUser(Message<JsonObject> message){
        try {
            JsonObject body = message.body();

            String initDate = body.getString(INIT_DATE);
            String endDate = body.getString(END_DATE);
            String tzOffset = UtilsDate.getTimeZoneValue();
            JsonArray params = new JsonArray().add(tzOffset).add(initDate).add(endDate);
            JsonArray paramsAll = new JsonArray().add(tzOffset).add(initDate).add(endDate);

            Future<ResultSet> f1 = Future.future();
            Future<ResultSet> f2 = Future.future();
            Future<ResultSet> f3 = Future.future();
            Future<ResultSet> f4 = Future.future();

            String QUERY_BP = QUERY_REPORT_ALL_SERVICES_BOARDING;
            String QUERY_PARCELS = QUERY_REPORT_ALL_SERVICES_PARCELS;
            String QUERY_RENTALS = QUERY_REPORT_ALL_SERVICES_RENTALS;
            String QUERY_ALL_SERVICES = QUERY_REPORT_ALL_SERVICES_BY_USER;



            if (body.getString(PURCHASE_ORIGIN) != null){
                String purchaseOrigin = body.getString(PURCHASE_ORIGIN);
                params.add(purchaseOrigin);
                paramsAll.add(purchaseOrigin).add(purchaseOrigin).add(purchaseOrigin);
                QUERY_BP += PARAMS_PURCHASE_ORIGIN_BOARDINGPASS;
                QUERY_PARCELS += PARAMS_PURCHASE_ORIGIN_PARCELS;
                QUERY_RENTALS += PARAMS_PURCHASE_ORIGIN_RENTALS;
                QUERY_ALL_SERVICES += PARAMS_PURCHASE_ORIGIN;
            }

            if (body.getInteger(TERMINAL_ID) != null){
                Integer terminalId = body.getInteger(TERMINAL_ID);
                params.add(terminalId);
                paramsAll.add(terminalId);
                QUERY_BP += PARAM_TERMINAL_ID;
                QUERY_PARCELS += PARAM_TERMINAL_ID;
                QUERY_RENTALS += PARAM_TERMINAL_ID;
                QUERY_ALL_SERVICES += PARAM_TERMINAL_ID;
            }

            if (body.getInteger(CITY_ID) != null){
                Integer cityId = body.getInteger(CITY_ID);
                params.add(cityId);
                paramsAll.add(cityId);
                QUERY_BP += PARAM_CITY_ID;
                QUERY_PARCELS += PARAM_CITY_ID;
                QUERY_RENTALS += PARAM_CITY_ID;
                QUERY_ALL_SERVICES += PARAM_CITY_ID;
            }
            if (body.getInteger("user_id") != null){
                Integer userId = body.getInteger("user_id");
                params.add(userId);
                paramsAll.add(userId);
                QUERY_BP += PARAM_BY_USER;
                QUERY_PARCELS += PARAM_BY_USER;
                QUERY_RENTALS += PARAM_BY_USER;
                QUERY_ALL_SERVICES += PARAM_BY_USER;
            }

            QUERY_BP += GROUP_BY_PURCHASE_DATE;
            QUERY_PARCELS += GROUP_BY_PURCHASE_DATE;
            QUERY_RENTALS += GROUP_BY_PURCHASE_DATE;
            QUERY_ALL_SERVICES += GROUP_BY_PURCHASE_DATE;

            this.dbClient.queryWithParams(QUERY_BP, params, f1.completer());
            this.dbClient.queryWithParams(QUERY_PARCELS, params, f2.completer());
            this.dbClient.queryWithParams(QUERY_RENTALS, params, f3.completer());
            this.dbClient.queryWithParams(QUERY_ALL_SERVICES, paramsAll, f4.completer());

            CompositeFuture.all(f1, f2, f3 ,f4).setHandler(reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> boardingPasses = reply.result().<ResultSet>resultAt(0).getRows();
                    List<JsonObject> parcels = reply.result().<ResultSet>resultAt(1).getRows();
                    List<JsonObject> rentals = reply.result().<ResultSet>resultAt(2).getRows();
                    List<JsonObject> allServices = reply.result().<ResultSet>resultAt(3).getRows();

                    allServices.forEach(all -> {
                        String purchaseDate = all.getString(PURCHASE_DATE);
                        for (int i = 0; i < boardingPasses.size(); i ++){
                            JsonObject boardingPass = boardingPasses.get(i);
                            String bpPurchaseDate = boardingPass.getString(PURCHASE_DATE);
                            if (purchaseDate.equals(bpPurchaseDate)){
                                boardingPass.remove(PURCHASE_DATE);
                                all.mergeIn(boardingPass);
                                break;
                            }
                        }

                        for (int i = 0; i < parcels.size(); i ++){
                            JsonObject parcel = parcels.get(i);
                            String pPurchaseDate = parcel.getString(PURCHASE_DATE);
                            if (purchaseDate.equals(pPurchaseDate)){
                                parcel.remove(PURCHASE_DATE);
                                all.mergeIn(parcel);
                                break;
                            }
                        }

                        for (int i = 0; i < rentals.size(); i ++){
                            JsonObject rental = rentals.get(i);
                            String rPurchaseDate = rental.getString(PURCHASE_DATE);
                            if (purchaseDate.equals(rPurchaseDate)){
                                rental.remove(PURCHASE_DATE);
                                all.mergeIn(rental);
                                break;
                            }
                        }

                        all.put(SALES_CREDIT, all.getDouble(CREDIT_BOARDINGPASS,0.0 ) + all.getDouble(CREDIT_PARCELS, 0.0) + all.getDouble(CREDIT_RENTALS, 0.0));
                        all.remove(CREDIT_BOARDINGPASS);
                        all.remove(CREDIT_PARCELS);
                        all.remove(CREDIT_RENTALS);

                    });

                    message.reply(new JsonArray(allServices));

                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }
    private static final String QUERY_REPORT_ALL_SERVICES_BOARDING = "SELECT \n" +
            " DATE(CONVERT_TZ(bp.created_at, '+00:00', ?)) AS purchase_date,\n" +
            " COUNT(bpt.id) AS boarding_passes,\n" +
            " SUM(IF(bp.payment_condition = 'credit', bp.total_amount, 0)) AS credit_boardingpass\n" +
            "FROM boarding_pass bp \n" +
            "INNER JOIN boarding_pass_passenger bpp ON bpp.boarding_pass_id = bp.id \n" +
            "INNER JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id \n" +
            "LEFT JOIN users u ON u.id = bp.created_by\n" +
            "LEFT JOIN employee e ON e.user_id = u.id\n" +
            "LEFT JOIN branchoffice b ON b.id = e.branchoffice_id\n" +
            "LEFT JOIN city ci ON ci.id = b.city_id\n" +
            "WHERE bp.created_at BETWEEN ? AND ? \n" +
            "AND bp.boardingpass_status NOT IN (0, 4) ";

    private static final String QUERY_REPORT_ALL_SERVICES_PARCELS = "SELECT\n" +
            " DATE(CONVERT_TZ(p.created_at, '+00:00', ?)) AS purchase_date,\n" +
            " COUNT(IF(p.pays_sender, p.id, null)) AS paid_parcels,\n" +
            " SUM(IF(p.payment_condition = 'credit', p.total_amount, 0)) AS credit_parcels\n" +
            "FROM parcels p\n" +
            "LEFT JOIN users u ON u.id = p.created_by\n" +
            "LEFT JOIN employee e ON e.user_id = u.id\n" +
            "LEFT JOIN branchoffice b ON b.id = e.branchoffice_id\n" +
            "LEFT JOIN city ci ON ci.id = b.city_id\n" +
            "WHERE p.created_at BETWEEN ? AND ? \n" +
            "AND p.parcel_status NOT IN (4, 6) ";

    private static final String QUERY_REPORT_ALL_SERVICES_RENTALS = "SELECT\n" +
            " DATE(CONVERT_TZ(r.created_at, '+00:00', ?)) AS purchase_date,\n" +
            " COUNT(r.id) AS rentals,\n" +
            " SUM(IF(r.payment_condition = 'credit', r.total_amount, 0)) AS credit_rentals\n" +
            "FROM rental r\n" +
            "LEFT JOIN users u ON u.id = r.created_by\n" +
            "LEFT JOIN employee e ON e.user_id = u.id\n" +
            "LEFT JOIN branchoffice b ON b.id = e.branchoffice_id\n" +
            "LEFT JOIN city ci ON ci.id = b.city_id\n" +
            "WHERE r.created_at BETWEEN ? AND ? \n" +
            "AND r.rent_status NOT IN (0, 5)";

    private static final String QUERY_REPORT_ALL_SERVICES = "SELECT\n" +
            "    DATE(CONVERT_TZ(t.created_at, '+00:00', ?)) AS purchase_date,    \n" +
            "    SUM(IF(t.boarding_pass_id IS NOT NULL, pm.amount, 0)) AS sale_boarding,\n" +
            "    COUNT(IF(t.parcel_id IS NULL OR p.pays_sender, NULL , 1)) AS fxc_parcels,\n" +
            "    SUM(IF(t.parcel_id IS NOT NULL , IF(p.pays_sender, pm.amount, 0), 0)) AS sale_paid_parcels,\n" +
            "    SUM(IF(t.parcel_id IS NOT NULL , IF(p.pays_sender, 0, pm.amount), 0)) AS sale_fxc_parcels,\n" +
            "    SUM(IF(t.rental_id IS NOT NULL, pm.amount, 0)) AS sale_rental,\n" +
            "    SUM(COALESCE(dpm.amount, 0)) AS debt_payment,\n" +
            "    SUM(IF(pm.payment_method = 'cash', pm.amount, 0)) AS cash,\n" +
            "    SUM(IF((bp.purchase_origin IN (1, 3) AND pm.payment_method IN ('debit'))\n" +
            "        OR (p.purchase_origin IN (1, 3) AND pm.payment_method = 'debit')\n" +
            "        OR (r.purchase_origin IN (1, 3) AND pm.payment_method = 'debit'), pm.amount, 0)) AS debit,\n" +
            "    SUM(IF((bp.purchase_origin IN (1, 3) AND pm.payment_method IN ('card'))\n" +
            "        OR (p.purchase_origin IN (1, 3) AND pm.payment_method = 'card')\n" +
            "        OR (r.purchase_origin IN (1, 3) AND pm.payment_method = 'card'), pm.amount, 0)) AS card,\n" +
            "    SUM(IF((bp.purchase_origin IN (2, 4) AND pm.payment_method IN ('card', 'debit'))\n" +
            "        OR (p.purchase_origin IN (2, 4) AND pm.payment_method IN ('card', 'debit'))\n" +
            "        OR (r.purchase_origin IN (2, 4) AND pm.payment_method IN ('card', 'debit')), pm.amount, 0)) AS card_online\n" +
            "FROM tickets t\n" +
            "LEFT JOIN cash_out co ON co.id = t.cash_out_id\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = t.boarding_pass_id AND bp.boardingpass_status NOT IN (4, 0)\n" +
            "LEFT JOIN parcels p ON p.id = t.parcel_id AND p.parcel_status NOT IN (4, 6) \n" +
            "LEFT JOIN rental r ON r.id = t.rental_id AND r.rent_status NOT IN (0, 5)\n" +
            "LEFT JOIN payment pm ON pm.ticket_id = t.id\n" +
            "LEFT JOIN debt_payment dpm ON dpm.ticket_id = t.id\n" +
            "LEFT JOIN branchoffice b ON b.id = co.branchoffice_id\n" +
            "LEFT JOIN city ci ON ci.id = b.city_id\n" +
            "WHERE t.action IN ('purchase', 'change', 'income') \n" +
            "AND t.created_at BETWEEN ? AND ? ";
    private static final String QUERY_REPORT_ALL_SERVICES_BY_USER = "SELECT\n" +
            "    DATE(CONVERT_TZ(t.created_at, '+00:00', ?)) AS purchase_date,    \n" +
            "    SUM(IF(t.boarding_pass_id IS NOT NULL, pm.amount, 0)) AS sale_boarding,\n" +
            "    COUNT(IF(t.parcel_id IS NULL OR p.pays_sender, NULL , 1)) AS fxc_parcels,\n" +
            "    SUM(IF(t.parcel_id IS NOT NULL , IF(p.pays_sender, pm.amount, 0), 0)) AS sale_paid_parcels,\n" +
            "    SUM(IF(t.parcel_id IS NOT NULL , IF(p.pays_sender, 0, pm.amount), 0)) AS sale_fxc_parcels,\n" +
            "    SUM(IF(t.rental_id IS NOT NULL, pm.amount, 0)) AS sale_rental,\n" +
            "    SUM(COALESCE(dpm.amount, 0)) AS debt_payment,\n" +
            "    SUM(IF(pm.payment_method = 'cash', pm.amount, 0)) AS cash,\n" +
            "    SUM(IF((bp.purchase_origin IN (1, 3) AND pm.payment_method IN ('debit'))\n" +
            "        OR (p.purchase_origin IN (1, 3) AND pm.payment_method = 'debit')\n" +
            "        OR (r.purchase_origin IN (1, 3) AND pm.payment_method = 'debit'), pm.amount, 0)) AS debit,\n" +
            "    SUM(IF((bp.purchase_origin IN (1, 3) AND pm.payment_method IN ('card'))\n" +
            "        OR (p.purchase_origin IN (1, 3) AND pm.payment_method = 'card')\n" +
            "        OR (r.purchase_origin IN (1, 3) AND pm.payment_method = 'card'), pm.amount, 0)) AS card,\n" +
            "    SUM(IF((bp.purchase_origin IN (2, 4) AND pm.payment_method IN ('card', 'debit'))\n" +
            "        OR (p.purchase_origin IN (2, 4) AND pm.payment_method IN ('card', 'debit'))\n" +
            "        OR (r.purchase_origin IN (2, 4) AND pm.payment_method IN ('card', 'debit')), pm.amount, 0)) AS card_online\n" +
            "FROM tickets t\n" +
            "LEFT JOIN cash_out co ON co.id = t.cash_out_id\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = t.boarding_pass_id AND bp.boardingpass_status NOT IN (4, 0)\n" +
            "LEFT JOIN parcels p ON p.id = t.parcel_id AND p.parcel_status NOT IN (4, 6) \n" +
            "LEFT JOIN rental r ON r.id = t.rental_id AND r.rent_status NOT IN (0, 5)\n" +
            "LEFT JOIN payment pm ON pm.ticket_id = t.id\n" +
            "LEFT JOIN debt_payment dpm ON dpm.ticket_id = t.id\n" +
            "LEFT JOIN branchoffice b ON b.id = co.branchoffice_id\n" +
            "LEFT JOIN city ci ON ci.id = b.city_id\n" +
            "LEFT JOIN users u ON u.id = t.created_by\n" +
            "LEFT JOIN employee e ON e.user_id = u.id \n"+
            "WHERE t.action IN ('purchase', 'change', 'income') \n" +
            "AND t.created_at BETWEEN ? AND ? ";
    private static final String PARAM_TERMINAL_ID = " AND b.id = ? ";

    private static final String PARAM_CITY_ID = " AND ci.id = ? ";

    private static final String PARAMS_PURCHASE_ORIGIN = " AND (bp.purchase_origin = ? OR p.purchase_origin = ? OR r.purchase_origin = ?) ";

    private static final String PARAMS_PURCHASE_ORIGIN_BOARDINGPASS = "AND bp.purchase_origin = ? ";

    private static final String PARAMS_PURCHASE_ORIGIN_PARCELS = " AND p.purchase_origin = ? ";

    private static final String PARAMS_PURCHASE_ORIGIN_RENTALS = "AND r.purchase_origin = ? ";

    private static final String GROUP_BY_PURCHASE_DATE = " GROUP BY purchase_date;";

    private static final String PARAM_BY_USER = " AND e.user_id = ? ";

}
