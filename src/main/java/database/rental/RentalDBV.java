/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.rental;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static service.commons.Constants.*;
import static utils.UtilsDate.getLocalDateTime;
import static utils.UtilsDate.toLocalDate;

import database.money.PaybackDBV;
import database.vechicle.VehicleDBV;
import database.money.PaymentDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.joda.time.DateTime;
import service.commons.Constants;
import service.commons.MailVerticle;

import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author ulises
 */
public class RentalDBV extends DBVerticle {

    //<editor-fold defaultstate="collapsed" desc="Constants Actions">
    public static final String ACTION_REGISTER = "RentalDBV.register";
    public static final String ACTION_QUOTATION = "RentalDBV.quotationDetail";
    public static final String ACTION_GET_QUOTATION_TO_RENT = "RentalDBV.getQuotationToRent";
    public static final String ACTION_DETAIL = "RentalDBV.reportDetail";
    public static final String ACTION_CAN_RENT = "RentalDBV.canRent";
    public static final String ACTION_PAYMENT_DETAIL = "RentalDBV.reportPaymentDetail";
    public static final String ACTION_DELIVERY = "RentalDBV.delivery";
    public static final String ACTION_RECEPTION = "RentalDBV.reception";
    public static final String ACTION_EXPIRE_RENTALS = "RentalDBV.expireRentals";
    public static final String ACTION_PARTIAL_PAYMENT = "RentalDBV.partialPayment";
    public static final String ACTION_CAN_DELIVER = "RentalDBV.canDeliver";
    public static final String ACTION_CAN_CANCEL = "RentalDBV.canCancel";
    public static final String ACTION_CANCEL = "RentalDBV.cancel";
    public static final String ACTION_LIST = "RentalDBV.getList";
    public static final String ACTION_GET_PRICE = "RentalDBV.getPrice";
    public static final String ACTION_GET_DRIVER_COST = "RentalDBV.getDriverCost";
    public static final String ACTION_GET_DEPOSIT = "RentalDBV.getDeposit";
    public static final String ACTION_GET_PENALTY = "RentalDBV.getPenalty";
    public static final String ACTION_GET_BY_RANGE_DATES = "RentalDBV.getByRangeDates";
    public static final String ACTION_SALES_REPORT = "RentalDBV.salesReport";
    public static final String ACTION_SALES_TOTALS = "RentalDBV.salesReportTotals";
    public static final String ACTION_SEND_INFORMATIVE_EMAIL = "RentalDBV.sendInformativeEmail";
    public static final String ACTION_VANS_LIST = "RentalDBV.vanList";
    public static final String UPDATE_RENTAL_INFO = "RentalDBV.updateRent";
//</editor-fold>
    private static final Integer MAX_LIMIT = 30;

    @Override
    public String getTableName() {
        return "rental";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_QUOTATION:
                this.quotationDetail(message);
                break;
            case ACTION_GET_QUOTATION_TO_RENT:
                this.getQuotationToRent(message);
                break;
            case ACTION_DETAIL:
                this.reservationDetail(message);
                break;
            case ACTION_DELIVERY:
                this.delivery(message);
                break;
            case ACTION_RECEPTION:
                this.reception(message);
                break;
            case ACTION_EXPIRE_RENTALS:
                this.expireRentals(message);
                break;
            case ACTION_PAYMENT_DETAIL:
                this.reservationPaymentDetail(message);
                break;
            case ACTION_PARTIAL_PAYMENT:
                this.partialPayment(message);
                break;
            case ACTION_CAN_DELIVER:
                this.canDeliver(message);
                break;
            case ACTION_CAN_RENT:
                this.canRent(message);
                break;
            case ACTION_CAN_CANCEL:
                this.canCancel(message);
                break;
            case ACTION_CANCEL:
                this.cancel(message);
                break;
            case ACTION_LIST:
                this.getList(message);
                break;
            case ACTION_GET_PRICE:
                this.getPrice(message);
                break;
            case ACTION_GET_DRIVER_COST:
                this.getDriverCost(message);
                break;
            case ACTION_GET_DEPOSIT:
                this.getDeposit(message);
                break;
            case ACTION_GET_PENALTY:
                this.getPenalty(message);
                break;
            case ACTION_GET_BY_RANGE_DATES:
                this.getByRangeDates(message);
                break;
            case ACTION_SALES_REPORT:
                this.salesReport(message);
                break;
            case ACTION_SEND_INFORMATIVE_EMAIL:
                this.sendInformativeEmail(message);
                break;
            case ACTION_VANS_LIST:
                this.vansList(message);
                break;
            case ACTION_SALES_TOTALS:
                this.salesReportTotals(message);
                break;
            case UPDATE_RENTAL_INFO:
                this.updateRent(message);
                break;
        }
    }

    private void sendInformativeEmail(Message<JsonObject> message){
        String reservationCode = message.body().getString("reservation_code");
        this.execReservationDetail(reservationCode).whenComplete((resultDetail, errorDetail) ->{
            try{
                if (errorDetail != null){
                    throw errorDetail;
                }
                JsonObject emailObject = new JsonObject();

                    try {
                        String createdAt = UtilsDate.format_D_MM_YYYY_HH_MM(UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(resultDetail.getString("created_at"), "UTC"));
                        String departureDate = UtilsDate.format_D_MM_YYYY(UtilsDate.parse_yyyy_MM_dd(resultDetail.getString("departure_date")));
                        String returnDate = UtilsDate.format_D_MM_YYYY(UtilsDate.parse_yyyy_MM_dd(resultDetail.getString("return_date")));
                        String totalAmount = String.format("%,.2f", resultDetail.getDouble("total_amount"));
                        String warranty = String.format("%,.2f", resultDetail.getDouble("guarantee_deposit"));
                        Double total = resultDetail.getDouble("guarantee_deposit", 0.0) + resultDetail.getDouble("total_amount", 0.0);

                        emailObject.put("FACEBOOK", "https://www.facebook.com/allAbordoTours/");
                        emailObject.put("INSTAGRAM", "https://www.instagram.com/allabordotours/");
                        emailObject.put("title", "");
                        emailObject.put("email", resultDetail.getString("email"));
                        emailObject.put("reservation_code", reservationCode);
                        emailObject.put("created_at", createdAt);
                        emailObject.put("first_name", resultDetail.getString("first_name"));
                        emailObject.put("last_name", resultDetail.getString("last_name"));
                        emailObject.put("pickup_city_name", resultDetail.getString("pickup_city_name"));
                        emailObject.put("pickup_state_name", resultDetail.getString("pickup_state_name"));
                        emailObject.put("departure_date", departureDate);
                        emailObject.put("departure_time", resultDetail.getString("departure_time"));
                        emailObject.put("destiny_city_name", resultDetail.getString("destiny_city_name"));
                        //emailObject.put("destiny_state_name", resultDetail.getString("destiny_state_name"));
                        emailObject.put("return_date", returnDate);
                        emailObject.put("return_time", resultDetail.getString("return_time"));
                        emailObject.put("total_amount", totalAmount);
                        emailObject.put("total_amount_letter", UtilsMoney.numberToLetter(String.valueOf(resultDetail.getDouble("total_amount"))));
                        emailObject.put("warranty", warranty);
                        emailObject.put("warranty_letter", UtilsMoney.numberToLetter(String.valueOf(resultDetail.getDouble("guarantee_deposit"))));
                        emailObject.put("total", total);
                        emailObject.put("total_letter", UtilsMoney.numberToLetter(String.valueOf(total)));
                        emailObject.put("has_driver_letter", (resultDetail.getBoolean("has_driver") ? "La presente cotización incluye combustible, casetas chofer y viáticos del chofer." : "La presente cotización NO incluye servicio de traslado con chofer."));
                        emailObject.put("characteristics", resultDetail.getValue("characteristics").toString());
                        emailObject.put("quotation_expired_after", resultDetail.getValue("quotation_expired_after").toString());
                        emailObject.put("rental_minamount_percent", resultDetail.getValue("rental_minamount_percent").toString());

                        JsonObject bodyEmail = new JsonObject()
                                .put("template", "rental_quotation.html")
                                .put("to", resultDetail.getString("email"))
                                .put("subject", "allAbordo - Cotización de renta de van")
                                .put("body", emailObject);


                        DeliveryOptions options = new DeliveryOptions()
                                .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);

                        this.vertx.eventBus().send(MailVerticle.class.getSimpleName(), bodyEmail, options, replySend -> {
                            try{
                                if (replySend.succeeded()) {
                                    message.reply(new JsonObject().put("email_status", replySend.succeeded()));
                                } else {
                                    reportQueryError(message, replySend.cause());
                                }
                            } catch (Exception e){
                                reportQueryError(message, e);
                            }
                        });


                    } catch (ParseException e){
                        reportQueryError(message, e);
                    }
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void canRent(Message<JsonObject> message){
        JsonObject body = message.body();
        JsonArray param = new JsonArray().add(body.getString("reservationCode"));
        execCanRent(param).whenComplete((result, error) -> {
            try {
                if (error != null){
                    throw error;
                }
                message.reply(result);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<JsonObject> execCanRent(JsonArray param){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_DATES_RESERVATION, param, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()){
                    throw new Exception("The reservation code don't exists or is invalid");
                }
                JsonObject datesRental = results.get(0);
                Integer vehicleId = datesRental.getInteger("vehicle_id");
                JsonObject paramAvVehicles = new JsonObject()
                        .put("travelDate", datesRental.getString("departure_date"))
                        .put("arrivalDate", datesRental.getString("return_date"))
                        .put("vehicleId", vehicleId);
                vertx.eventBus().send(VehicleDBV.class.getSimpleName(), paramAvVehicles, new DeliveryOptions().addHeader(ACTION, VehicleDBV.ACTION_IS_AVAILABLE_RENTAL_VEHICLE), replyAvVehicles -> {
                    try {
                        if (replyAvVehicles.failed()){
                            throw replyAvVehicles.cause();
                        }
                        JsonArray availableVehicles = (JsonArray) replyAvVehicles.result().body();
                        if (availableVehicles.isEmpty()){
                            future.complete(new JsonObject()
                                    .put("canRent", false)
                                    .put("message", "The vehicle isn't available"));
                        } else {
                            future.complete(new JsonObject()
                                    .put("canRent", true)
                                    .put("message", "The vehicle is available"));
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getQuotationToRent(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray param = new JsonArray().add(body.getString("reservation_code"));

        this.execCanRent(param).whenComplete((resultCanRent, error) -> {
            try {
                if (error != null){
                    throw error;
                }
                this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_QUOTATION_TO_RENT, param, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        List<JsonObject> results = reply.result().getRows();
                        if (results.isEmpty()) {
                            throw new Exception("The reservation code don't exists or is invalid");
                        }
                        JsonObject rental = reply.result().getRows().get(0);

                        int rentalId = rental.getInteger("id");
                        JsonArray detailsParam = new JsonArray().add(rentalId);

                        this.dbClient.queryWithParams(
                                QUERY_RESERVATION_DETAIL_RENTAL_EXTRA_CHARGUE_TO_RENT,
                                detailsParam, detailReply -> {
                                    try {
                                        if (detailReply.failed()){
                                            throw detailReply.cause();
                                        }
                                        List<JsonObject> rentalExtraCharges
                                                = detailReply.result().getRows();
                                        rental.put("rental_extra_charges", rentalExtraCharges);
                                        rental.put("quotation_id", rentalId);
                                        rental.remove("id");
                                        message.reply(rental);
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        reportQueryError(message, t);
                                    }
                                });
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getByRangeDates(Message<JsonObject> message){
        JsonObject body = message.body();
        String travelDate = body.getString("travelDate");
        String arrivalDate = body.getString("arrivalDate");
        String QUERY_AVAILABLE = QUERY_GET_AVAILABLE_RENTALS_BY_RANGE_DATES;
        String QUERY_UNAVAILABLE = QUERY_GET_UNAVAILABLE_RENTALS_BY_RANGE_DATES;
        JsonArray params = new JsonArray()
                .add(travelDate).add(arrivalDate)
                .add(travelDate).add(arrivalDate);
        if(body.getString("pickupBranchId") != null) {
            params.add(Integer.parseInt(body.getString("pickupBranchId")));
            QUERY_AVAILABLE = QUERY_GET_AVAILABLE_RENTALS_BY_RANGE_DATES + " AND v.branchoffice_id = ? ";
            QUERY_UNAVAILABLE = QUERY_GET_UNAVAILABLE_RENTALS_BY_RANGE_DATES + " AND r.pickup_branchoffice_id = ? ";
        }
        Future f1 = Future.future();
        Future f2 = Future.future();
        dbClient.queryWithParams(QUERY_AVAILABLE + " GROUP BY v.id;", params, f1);
        dbClient.queryWithParams(QUERY_UNAVAILABLE + ";", params, f2);

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> availables = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> unavailables = reply.result().<ResultSet>resultAt(1).getRows();
                message.reply(new JsonObject()
                        .put("availables", availables)
                        .put("unavailable", unavailables));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                JsonObject body = message.body();
                JsonObject rental = body.copy();
                rental.remove("driver");
                rental.remove("rental_extra_charges");
                rental.remove("evidences");
                rental.remove("payments");
                rental.remove("cash_change");
                Double iva = (Double) rental.remove("iva");
                Double profitPercent = (Double) rental.remove("profit_percent");
                String QUERY_CITIES = "SELECT p.city_id as pickup_city_id, l.city_id as leave_city_id FROM branchoffice p RIGHT JOIN branchoffice l ON l.id=? WHERE p.id = ?;";
                dbClient.queryWithParams(QUERY_CITIES, new JsonArray().add(rental.getInteger("leave_branchoffice_id"))
                        .add(rental.getInteger("pickup_branchoffice_id")), replyBranchoffice -> {
                    try {
                        if (replyBranchoffice.failed()){
                            throw replyBranchoffice.cause();
                        }
                        JsonObject cities = replyBranchoffice.result().getRows().get(0);
                        rental.put("pickup_city_id", cities.getInteger("pickup_city_id"));
                        rental.put("leave_city_id", cities.getInteger("leave_city_id"));
                        Double amount = rental.getDouble("amount");
                        Double profitIva = this.getIva(amount, iva);
                        Double profit = amount - profitIva;
                        Double driverCost = rental.getDouble("driver_cost", 0.0);
                        Double extraCharges = rental.getDouble("extra_charges", 0.0);
                        Double sumCharges = driverCost + extraCharges;
                        Double costIva = sumCharges - this.getIva(sumCharges, iva);
                        Double utilityCost = this.getIva(costIva, profitPercent);
                        Double cost =  costIva - utilityCost;
                        rental
                            .put("cost", cost)
                            .put("profit", profit + utilityCost);
                        GenericQuery gc = this.generateGenericCreate(rental);
                        con.updateWithParams(gc.getQuery(), gc.getParams(), reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }
                                int rentalId = reply.result().getKeys().getInteger(0);
                                List<String> batch = new ArrayList<>();
                                //driver
                                JsonObject driver = body.getJsonObject("driver");
                                if (driver != null) {
                                    driver.put("rental_id", rentalId);
                                    batch.add(this.generateGenericCreate("rental_driver", driver));
                                }

                                //extra charges
                                JsonArray extras = body.getJsonArray("rental_extra_charges");
                                if (extras != null) {
                                    for (int i = 0; i < extras.size(); i++) {
                                        JsonObject extra = extras.getJsonObject(i);
                                        extra.put("rental_id", rentalId);
                                        batch.add(this.generateGenericCreate("rental_extra_charge", extra));
                                    }
                                }
                                //evidences
                                JsonArray evidences = body.getJsonArray("evidences");
                                if (evidences != null) {
                                    for (int i = 0; i < evidences.size(); i++) {
                                        JsonObject evidence = evidences.getJsonObject(i);
                                        evidence.put("rental_id", rentalId);
                                        batch.add(this.generateGenericCreate("rental_evidence", evidence));
                                    }
                                }

                                // if rental comes from quotation
                                if(rental.getInteger("quotation_id") != null){
                                    JsonObject quotation = new JsonObject().put("id", rental.getInteger("quotation_id")).put("rent_status", 6);
                                    batch.add(this.generateGenericUpdateString("rental", quotation));
                                }
                                con.batch(batch, batchReply -> {
                                    try {
                                        if (batchReply.failed()){
                                            throw batchReply.cause();
                                        }
                                        List<Integer> batchResult = batchReply.result();
                                        //payments
                                        JsonArray payments = body.getJsonArray("payments");
                                        if (payments != null && !payments.isEmpty()) {
                                            List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                                            JsonObject responseResult = new JsonObject();
                                            ArrayList<Integer> ticketList = new ArrayList<>();
                                            double summDeposit = 0.0;
                                            for (int i = 0; i < payments.size(); i++){
                                                JsonObject payment = payments.getJsonObject(i);
                                                boolean isDeposit = payment.getBoolean("is_deposit", false);
                                                double amountDeposit = payment.getDouble(AMOUNT);
                                                summDeposit += isDeposit ? amountDeposit : 0;
                                            }

                                            for (int i = 0; i < payments.size(); i++) {
                                                JsonArray paymentTickets = new JsonArray();
                                                paymentTickets.add(payments.getJsonObject(i));
                                                tasks.add(insertRentalPayments(con, paymentTickets, "Anticipo",  body.getString("reservation_code"), rentalId,responseResult, message, ticketList, summDeposit));
                                            }
                                            CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                                            all.whenComplete((resultt, error) -> {
                                                try {
                                                    if (error != null){
                                                        throw error;
                                                    }
                                                    responseResult.put("tickets" , ticketList);
                                                    this.commit(con, message, responseResult);

                                                } catch (Throwable t){
                                                    t.printStackTrace();
                                                    this.rollback(con, t, message);
                                                }
                                            });
                                        } else {
                                            this.commit(con, message, new JsonObject()
                                                    .put("id", rentalId)
                                                    .put("reservation_code", body.getString("reservation_code"))
                                                    .put("is_quotation", true));
                                        }
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(con, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(con, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private void reservationPaymentDetail(Message<JsonObject> message) {
        JsonArray params = new JsonArray().add(message.body().getString("reservationCode"));

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        Future<ResultSet> f3 = Future.future();
        Future<ResultSet> f4 = Future.future();

        dbClient.queryWithParams(QUERY_PAYMENT_DETAILS, params, f1.completer());
        dbClient.queryWithParams(QUERY_PAYMENT_BALANCE, params, f2.completer());
        dbClient.queryWithParams(QUERY_DEPOSIT_BALANCE_TOTAL, params, f3.completer());
        dbClient.queryWithParams(QUERY_EXTRA_BALANCE, params, f4.completer());

        CompositeFuture.all(f1, f2, f3, f4).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> paymentDetail = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> paymentBalanceResultSet = reply.result().<ResultSet>resultAt(1).getRows();
                List<JsonObject> depositBalanceResultSet = reply.result().<ResultSet>resultAt(2).getRows();
                List<JsonObject> extraBalanceResultSet = reply.result().<ResultSet>resultAt(3).getRows();

                JsonObject payBalance = paymentBalanceResultSet.get(0);
                if (payBalance.getDouble("total_paid") == null) {
                    payBalance.put("total_paid", 0f);
                }
                if (payBalance.getDouble("missing_to_pay") == null) {
                    payBalance.put("missing_to_pay", payBalance.getDouble("total_amount"));
                }
                if (payBalance.getDouble("total_amount") == null) {
                    message.reply(null); //dont exist reservation
                    return;
                }

                JsonObject depositBalance = new JsonObject();
                if (depositBalanceResultSet.isEmpty()) {
                    depositBalance.put("missing_to_deposit", payBalance.getDouble("guarantee_deposit"));
                    depositBalance.put("total_deposited", 0f);
                } else {
                    depositBalance = depositBalanceResultSet.get(0);
                    if (depositBalance.getDouble("missing_to_deposit") == null) {
                        depositBalance.put("missing_to_deposit", depositBalance.getDouble("guarantee_deposit"));
                        depositBalance.put("total_deposited", 0f);
                    }

                }

                JsonObject extraBalance = new JsonObject();
                if (extraBalanceResultSet.isEmpty()) {
                    extraBalance.put("total_extra_charge", 0f);
                } else {
                    extraBalance = extraBalanceResultSet.get(0);
                    if (extraBalance.getDouble("total_extra_charge") == null) {
                        extraBalance.put("total_extra_charge", 0f);
                    }
                }

                JsonObject result = new JsonObject()
                        .put("payments", paymentDetail)
                        .put("payment_balance", payBalance)
                        .put("deposit_balance",depositBalance)
                        .put("extra_balance", extraBalance);
                message.reply(result);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void quotationDetail(Message<JsonObject> message) {
        JsonArray params = new JsonArray() 
                .add(message.body().getString("reservationCode"));
        dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_QUOTATION, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    message.reply(null);
                    return;
                }
                JsonObject rental = results.get(0);
                Future<ResultSet> f1 = Future.future();
                Future<ResultSet> f2 = Future.future();
                Future<ResultSet> f3 = Future.future();

                int rentalId = rental.getInteger("id");
                JsonArray detailsParam = new JsonArray().add(rentalId);

                int vehicleId = rental.getInteger("vehicle_id");
                JsonArray characteristicsParam = new JsonArray().add(vehicleId);

                this.dbClient.queryWithParams(
                        QUERY_RESERVATION_DETAIL_RENTAL_CHECKLIST,
                        detailsParam,
                        f1.completer());
                this.dbClient.queryWithParams(
                        QUERY_RESERVATION_DETAIL_RENTAL_EXTRA_CHARGUE,
                        detailsParam,
                        f2.completer());
                this.dbClient.queryWithParams(
                        QUERY_VEHICLE_CHARACTERISTICS,
                        characteristicsParam,
                        f3.completer());
                CompositeFuture.all(f1, f2, f3).setHandler(detailReply -> {
                    try {
                        if (detailReply.failed()){
                            throw detailReply.cause();
                        }
                        List<JsonObject> rentalChecklist
                                = detailReply.result()
                                .<ResultSet>resultAt(0).getRows();
                        List<JsonObject> rentalExtraCharges
                                = detailReply.result()
                                .<ResultSet>resultAt(1).getRows();
                        List<JsonObject> rentalCharacteristicVehicle
                                = detailReply.result()
                                .<ResultSet>resultAt(2).getRows();
                        rental.put("checklist", rentalChecklist);
                        rental.put("rental_extra_charges", rentalExtraCharges);
                        rental.put("characteristic_vehicle", rentalCharacteristicVehicle);
                        message.reply(rental);
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void reservationDetail(Message<JsonObject> message) {
        this.execReservationDetail(message.body().getString("reservationCode")).whenComplete((resultDetail, errorDetail) -> {
            try{
                if (errorDetail != null){
                    throw errorDetail;
                }
                message.reply(resultDetail);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<JsonObject> execReservationDetail(String reservationCode){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(reservationCode);
        dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_GENERAL, params, reply -> {
            try{
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    future.complete(null);
                } else {
                    JsonObject rental = reply.result().getRows().get(0);
                    Future<ResultSet> f1 = Future.future();
                    Future<ResultSet> f2 = Future.future();
                    Future<ResultSet> f3 = Future.future();
                    Future<ResultSet> f4 = Future.future();
                    Future<ResultSet> f5 = Future.future();
                    Future<ResultSet> f6 = Future.future();

                    int rentalId = rental.getInteger("id");
                    int vehicleId = rental.getInteger("vehicle_id");
                    JsonArray detailsParam = new JsonArray().add(rentalId);

                    this.dbClient.queryWithParams(
                            QUERY_RESERVATION_DETAIL_RENTAL_CHECKLIST,
                            detailsParam,
                            f1.completer());
                    this.dbClient.queryWithParams(
                            QUERY_RESERVATION_DETAIL_RENTAL_DRIVER,
                            detailsParam,
                            f2.completer());
                    this.dbClient.queryWithParams(
                            QUERY_RESERVATION_DETAIL_RENTAL_EXTRA_CHARGUE,
                            detailsParam,
                            f3.completer());
                    this.dbClient.queryWithParams(
                            QUERY_RESERVATION_DETAIL_RENTAL_EVIDENCE,
                            detailsParam,
                            f4.completer());
                    this.dbClient.queryWithParams(
                            QUERY_RESERVATION_DETAIL_PAYMENT_AMOUNT,
                            detailsParam,
                            f5.completer());
                    this.dbClient.queryWithParams(
                            QUERY_CONCAT_VEHICLE_CHARACTERISTICS,
                            new JsonArray().add(vehicleId),
                            f6.completer());

                    CompositeFuture.all(f1, f2, f3, f4, f5, f6).setHandler(detailReply -> {
                        try {
                            if (detailReply.failed()){
                                throw detailReply.cause();
                            }
                            List<JsonObject> rentalChecklist
                                    = detailReply.result()
                                    .<ResultSet>resultAt(0).getRows();
                            List<JsonObject> rentalDrivers
                                    = detailReply.result()
                                    .<ResultSet>resultAt(1).getRows();
                            List<JsonObject> rentalExtraChargues
                                    = detailReply.result()
                                    .<ResultSet>resultAt(2).getRows();
                            List<JsonObject> rentalEvidences
                                    = detailReply.result()
                                    .<ResultSet>resultAt(3).getRows();
                            List<JsonObject> paymentAmount
                                    = detailReply.result()
                                    .<ResultSet>resultAt(4).getRows();
                            List<JsonObject> vehicleCharacteristics
                                    = detailReply.result()
                                    .<ResultSet>resultAt(5).getRows();

                            if (!rentalDrivers.isEmpty()) {
                                rental.put("driver", rentalDrivers.get(0));
                            } else {
                                rental.putNull("driver");
                            }
                            if (!paymentAmount.isEmpty()) {
                                rental.put("payment_amount", paymentAmount.get(0));
                            } else {
                                rental.putNull("payment_amount");
                            }
                            rental.put("characteristics", (vehicleCharacteristics.get(0).getString("vehicle_characteristics") == null ? "" : vehicleCharacteristics.get(0).getString("vehicle_characteristics").trim() ));
                            rental.put("checklist", rentalChecklist);
                            rental.put("rental_extra_charges", rentalExtraChargues);
                            rental.put("evidences", rentalEvidences);
                            future.complete(rental);
                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                }
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void delivery(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                List<String> batch = new ArrayList<>();
                JsonObject body = message.body();
                JsonArray checklist = body.getJsonArray("checklist");
                JsonArray extraChargesRental = body.getJsonArray("rental_extra_charges");
                //update rental
                JsonObject rental = body.copy();
                rental.put("checklist_charges", 0.0);
                rental.remove("checklist");
                rental.remove("rental_extra_charges");
                rental.remove("evidences");
                rental.remove("created_at");
                rental.remove("created_by");
                rental.remove("payments");
                rental.remove("cash_change");
                Double iva = (Double) rental.remove("iva_percent");
                Double profitPercent = (Double) rental.remove("profit_percent");

                Double extraCharges = 0.0;
                for (int i = 0; i < extraChargesRental.size(); i++) {
                    JsonObject extra = extraChargesRental.getJsonObject(i);
                    extraCharges += extra.getDouble("amount");
                }
                final double extraChargesFinal = extraCharges;
                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                for (int i = 0; i < checklist.size(); i++) {
                    JsonObject checklistVan = checklist.getJsonObject(i);
                    pTasks.add(getAmountChecklist(con, checklistVan, rental));
                }
                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[checklist.size()]));
                allPayments.whenComplete((ps, pt) -> {
                    try {
                        if (pt != null) {
                            throw pt;
                        }
                        this.dbClient.queryWithParams("SELECT driver_cost, checklist_charges, extra_charges, amount, discount, total_amount, cost, profit FROM rental WHERE id=?",
                                new JsonArray().add(rental.getInteger("id")), reply -> {
                                    try {
                                        if (reply.failed()){
                                            throw reply.cause();
                                        }
                                        JsonObject rentalActual = reply.result().getRows().get(0);
                                        rental.put("extra_charges", rentalActual.getDouble("extra_charges") + extraChargesFinal);
                                        rental.put("total_amount", rentalActual.getDouble("total_amount") + extraChargesFinal + rental.getDouble("checklist_charges"));
                                        Double actualProfit = rentalActual.getDouble("profit",0.0);
                                        Double actualCost = rentalActual.getDouble("cost",0.0);
                                        Double actualDriverCost = rentalActual.getDouble("driver_cost",0.0);
                                        Double actualExtraCharges = rentalActual.getDouble("extra_charges",0.0);
                                        Double actualChecklistCharges = rentalActual.getDouble("checklist_charges",0.0);
                                        if(!extraChargesRental.isEmpty()){
                                            Double newDriverCost =  Math.abs(rental.getDouble("driver_cost", 0.0) - actualDriverCost);
                                            Double newExtraCharges = Math.abs(rental.getDouble("extra_charges", 0.0) - actualExtraCharges);
                                            Double checkListCharges =  Math.abs(rental.getDouble("checklist_charges",0.0) - actualChecklistCharges);
                                            Double sumCharges = newDriverCost + newExtraCharges + checkListCharges;
                                            Double costIva = sumCharges - this.getIva(sumCharges, iva);
                                            Double utilityCost = this.getIva(costIva, profitPercent);
                                            Double cost = costIva - utilityCost;
                                            rental
                                                    .put("cost", actualCost + cost)
                                                    .put("profit", actualProfit + utilityCost);
                                        }
                                        batch.add(this.generateGenericUpdateString("rental", rental));

                                        //update or insert rental driver
                                        this.evaluateUpdateInsert(batch, "rental_driver", "driver", body);
                                        //update or insert checklist
                                        this.evaluateUpdateInsertList(batch, "rental_checklist", "checklist", body);
                                        //update or insert extra charges
                                        this.evaluateUpdateInsertList(batch, "rental_extra_charge", "rental_extra_charges", body);
                                        //update or insert evidence
                                        this.evaluateUpdateInsertList(batch, "rental_evidence", "evidences", body);

                                        con.batch(batch, replyBatch -> {
                                            try {
                                                if (replyBatch.failed()){
                                                    throw replyBatch.cause();
                                                }
                                                JsonArray payments = body.getJsonArray("payments");
                                                if (payments == null || payments.isEmpty()) {
                                                    this.commit(con, message, new JsonObject().put("reservation_code", rental.getString("reservation_code")));
                                                } else {
                                                    this.validateAndInsertPayments(body, "Entrega", con, message);
                                                }
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(con, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(con, t, message);
                                    }
                                });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> getAmountChecklist(SQLConnection conn, JsonObject checklist, JsonObject rental) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        conn.queryWithParams("SELECT id, name, is_default, default_value, use_price FROM checklist_vans WHERE id = ?",
                new JsonArray().add(checklist.getInteger("checklist_van_id")), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                JsonObject checklistVan = reply.result().getRows().get(0);
                checklist.put("calculated_amount", 0.0);

                if(checklistVan.getBoolean("is_default")){
                    if(checklist.getInteger("delivery_quantity") > checklistVan.getInteger("default_value")){
                        double amount = (checklist.getInteger("delivery_quantity") - checklistVan.getInteger("default_value")) * checklistVan.getInteger("use_price");
                        checklist.put("calculated_amount", amount);
                    }
                } else {
                    double amount = checklist.getInteger("delivery_quantity") * checklistVan.getInteger("use_price");
                    checklist.put("calculated_amount", amount);
                }
                if(checklist.getDouble("calculated_amount") < checklist.getDouble("amount")){
                    future.completeExceptionally(new Throwable("Checklist " + checklistVan.getString("name") + " amount " + checklist.getDouble("amount") + " is greather than calculated on system: $ " + checklist.getDouble("calculated_amount")));
                } else if(checklist.getDouble("calculated_amount") < checklist.getDouble("amount")){
                    future.completeExceptionally(new Throwable("Checklist " + checklistVan.getString("name") + " amount " + checklist.getDouble("amount") + " is lower than calculated on system: $ " + checklist.getDouble("calculated_amount")));
                } else {
                    rental.put("checklist_charges", rental.getDouble("checklist_charges") + checklist.getDouble("calculated_amount"));
                    checklist.remove("calculated_amount");
                    future.complete(checklist);
                }
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void reception(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                JsonObject body = message.body();
                List<String> batch = new ArrayList<>();
                JsonArray checklist = (body.getJsonArray("checklist") == null ? new JsonArray() : body.getJsonArray("checklist"));
                JsonArray extraChargesRental = (body.getJsonArray("rental_extra_charges") == null ? new JsonArray() : body.getJsonArray("rental_extra_charges"));
                //update rental
                JsonObject rental = body.copy();
                rental.put("checklist_charges", 0.0);
                rental.remove("checklist");
                rental.remove("rental_extra_charges");
                rental.remove("evidences");
                rental.remove("payments");
                rental.remove("cash_change");
                rental.remove("expense");
                Double iva = (Double) rental.remove("iva");
                Double profitPercent = (Double) rental.remove("profit_percent");
                Integer rentalId = body.getInteger("id");
                Integer updatedBy = body.getInteger("updated_by");

                Double extraCharges = 0.0;
                Double checklistCharges = 0.0;
                for (int i = 0; i < extraChargesRental.size(); i++) {
                    JsonObject extra = extraChargesRental.getJsonObject(i);
                    extraCharges += extra.getDouble("amount");
                }
                final double extraChargesFinal = extraCharges;

                List<Integer> checklistVanIds = new ArrayList<>();
                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                for (int i = 0; i < checklist.size(); i++) {
                    JsonObject checklistVan = checklist.getJsonObject(i);
                    pTasks.add(getDamageChecklist(con, checklistVan, rental));
                    checklistVanIds.add(checklistVan.getInteger("checklist_van_id"));
                    checklistCharges += checklistVan.getDouble("damage_amount");
                }
                pTasks.add(getChecklist(con, checklistVanIds, rental.getInteger("id")));
                final double checklistChargesFinal = checklistCharges;

                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[checklist.size() + 1]));
                allPayments.whenComplete((ps, pt) -> {
                    try {
                        if (pt != null) {
                            throw pt;
                        }
                        this.dbClient.queryWithParams("SELECT kilometers_init, driver_cost, checklist_charges, extra_charges, amount, discount, total_amount, cost, profit FROM rental WHERE id=?",
                                new JsonArray().add(rental.getInteger("id")), reply -> {
                                    try {
                                        if (reply.failed()){
                                            throw reply.cause();
                                        }
                                        JsonObject rentalActual = reply.result().getRows().get(0);
                                        if(rentalActual.getInteger("kilometers_init") >= rental.getInteger("kilometers_end") ){
                                            throw new Exception("Kilometers must be greather than " + rentalActual.getInteger("kilometers_init"));
                                        }
                                        Double totalKM = Double.valueOf(rental.getInteger("kilometers_end")) - Double.valueOf(rentalActual.getInteger("kilometers_init"));
                                        rental.put("checklist_charges", rentalActual.getDouble("checklist_charges") + checklistChargesFinal);
                                        rental.put("extra_charges", rentalActual.getDouble("extra_charges") + extraChargesFinal);
                                        rental.put("total_amount", rentalActual.getDouble("total_amount") + extraChargesFinal + checklistChargesFinal + rental.getDouble("penalty_amount"));

                                        PaybackDBV objPayback = new PaybackDBV();
                                        objPayback.calculatePointsRental(con, totalKM).whenComplete((resultCalculate, errorC) ->{
                                            try {
                                                if (errorC != null){
                                                    throw errorC;
                                                }
                                                Double paybackMoney = resultCalculate.getDouble("money");
                                                Double paybackPoints = resultCalculate.getDouble("points");
                                                rental.put("payback", paybackMoney);

                                                if (body.getInteger("customer_id") != null){
                                                    JsonObject paramMovPayback = new JsonObject()
                                                            .put("customer_id", body.getInteger("customer_id"))
                                                            .put("points", paybackPoints)
                                                            .put("money", paybackMoney)
                                                            .put("type_movement", "I")
                                                            .put("motive", "Renta de van")
                                                            .put("id_parent", rentalId)
                                                            .put("employee_id", updatedBy);
                                                    objPayback.generateMovementPayback(con, paramMovPayback).whenComplete((movementPayback, errorMP) ->{
                                                        try {
                                                            if(errorMP != null){
                                                                throw errorMP;
                                                            }
                                                            JsonObject finalResult = new JsonObject()
                                                                    .put("payback_points", movementPayback.getDouble("points_payback"))
                                                                    .put("payback_money", movementPayback.getDouble("money_payback"));
                                                            System.out.println("reception");
                                                            System.out.println(finalResult);
                                                        } catch (Throwable t){
                                                            t.printStackTrace();
                                                            this.rollback(con, t, message);
                                                        }
                                                    });
                                                }
                                                Double penaltyAmount = rental.getDouble("penalty_amount");
                                                Double profitIva = this.getIva(penaltyAmount, iva);
                                                Double profit = penaltyAmount - profitIva;
                                                Double actualProfit = rentalActual.getDouble("profit",0.0);
                                                Double actualCost = rentalActual.getDouble("cost",0.0);
                                                Double actualDriverCost = rentalActual.getDouble("driver_cost",0.0);
                                                Double actualExtraCharges = rentalActual.getDouble("extra_charges",0.0);
                                                Double actualChecklistCharges = rentalActual.getDouble("checklist_charges",0.0);
                                                if(!extraChargesRental.isEmpty()){
                                                    Double newDriverCost =  Math.abs(rental.getDouble("driver_cost", 0.0) - actualDriverCost);
                                                    Double newExtraCharges = Math.abs(rental.getDouble("extra_charges", 0.0) - actualExtraCharges);
                                                    Double checkListCharges =  Math.abs(rental.getDouble("checklist_charges",0.0) - actualChecklistCharges);
                                                    Double sumCharges = newDriverCost + newExtraCharges + checkListCharges;
                                                    Double costIva = sumCharges - this.getIva(sumCharges, iva);
                                                    Double utilityCost = this.getIva(costIva, profitPercent);
                                                    Double cost =  costIva - utilityCost;
                                                    rental
                                                            .put("cost", actualCost + cost)
                                                            .put("profit", actualProfit + utilityCost + profit);
                                                }
                                                batch.add(this.generateGenericUpdateString("rental", rental));

                                                //update or insert checklist
                                                this.evaluateUpdateInsertList(batch, "rental_checklist", "checklist", body);
                                                //update or insert extra charges
                                                this.evaluateUpdateInsertList(batch, "rental_extra_charge", "rental_extra_charges", body);
                                                //update or insert evidence
                                                this.evaluateUpdateInsertList(batch, "rental_evidence", "evidences", body);

                                                con.batch(batch, replyBatch -> {
                                                    try {
                                                        if (replyBatch.failed()){
                                                            throw replyBatch.cause();
                                                        }
                                                        JsonArray payments = body.getJsonArray("payments");
                                                        Double returnMoney = body.getDouble("expense");
                                                        if (payments == null || payments.isEmpty()) {
                                                            if(returnMoney == null){
                                                                this.commit(con, message, null);
                                                            }
                                                            else {
                                                                this.validateAndInsertReturnMoney(body, extraChargesFinal, checklistChargesFinal, con, message);
                                                            }
                                                        } else {
                                                            this.validateReceptionAndInsertPayments(body, "Recepcion", con, message);
                                                        }
                                                    } catch (Throwable t){
                                                        t.printStackTrace();
                                                        this.rollback(con, t, message);
                                                    }
                                                });
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(con, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(con, t, message);
                                    }

                                });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });

    }

    private CompletableFuture<JsonObject> getChecklist(SQLConnection conn, List<Integer> checklist, Integer rentalId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        conn.queryWithParams("SELECT rc.*, cv.name FROM rental_checklist rc JOIN checklist_vans cv ON cv.id=rc.checklist_van_id WHERE rc.rental_id = ?",
                new JsonArray().add(rentalId), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        List<JsonObject> checklistVan = reply.result().getRows();
                        for(int i = 0; i < checklistVan.size(); i ++){
                            int checklistVanId = checklistVan.get(i).getInteger("checklist_van_id");
                            if(!checklist.contains(checklistVanId)){
                                throw new Exception("Item van checklist " + checklistVan.get(i).getInteger("name") + " doesn't exist on rental checklist");
                            }
                        }
                        future.complete(new JsonObject().put("checklist_ok", true));
                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });

        return future;
    }

    private CompletableFuture<JsonObject> getDamageChecklist(SQLConnection conn, JsonObject checklist, JsonObject rental) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        conn.queryWithParams("SELECT cv.id, cv.name, cv.damage_price, rc.delivery_quantity FROM checklist_vans cv LEFT JOIN rental_checklist rc ON rc.checklist_van_id=cv.id WHERE rc.id = ?",
                new JsonArray().add(checklist.getInteger("id")), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        JsonObject checklistVan = reply.result().getRows().get(0);
                        double amount = 0.0;
                        if(checklist.getInteger("reception_quantity") < checklistVan.getInteger("delivery_quantity")){
                            amount = (checklistVan.getInteger("delivery_quantity") - checklist.getInteger("reception_quantity")) * checklistVan.getDouble("damage_price");
                        }
                        System.out.println(checklist.getInteger("reception_quantity") +" : "+ checklistVan.getInteger("delivery_quantity") + " = " + amount);
                        if(checklist.getInteger("damage_quantity") > 0){
                            amount += checklist.getInteger("damage_quantity") * checklist.getInteger("damage_percent") * checklistVan.getDouble("damage_price");
                        }
                        System.out.println(checklist.getInteger("damage_quantity") +" * "+ checklist.getInteger("damage_percent") +" * "+ checklistVan.getDouble("damage_price") + " = " + amount);
                        checklist.put("calculated_amount", amount);
                        if(checklist.getDouble("calculated_amount") < checklist.getDouble("damage_amount")){
                            throw new Exception("Checklist " + checklistVan.getString("name") + " damage amount is greather than calculated on system: $ " + checklist.getDouble("calculated_amount"));
                        }
                        if(checklist.getDouble("calculated_amount") < checklist.getDouble("damage_amount")){
                            throw new Exception("Checklist " + checklistVan.getString("name") + " damage amount is lower than calculated on system: $ " + checklist.getDouble("calculated_amount"));
                        }
                        rental.put("checklist_charges", rental.getDouble("checklist_charges") + checklist.getDouble("calculated_amount"));
                        checklist.remove("calculated_amount");
                        future.complete(checklist);
                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });

        return future;
    }

    private void expireRentals(Message<JsonObject> message) {
        Future f1 = Future.future();
        Future f2 = Future.future();

        this.dbClient.update(QUERY_EXPIRE_QUOTATIONS, f1.completer());

        CompositeFuture.all(f1 , f2).setHandler(reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(null);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });

    }

    private void partialPayment(Message<JsonObject> message) {
        JsonObject body = message.body();
        this.startTransaction(message, con -> {
            this.validateAndInsertPayments(body, "Pago parcial", con, message);
        });
    }

    private void canDeliver(Message<JsonObject> message) {
        JsonArray params = new JsonArray().add(message.body().getString("reservationCode"));
        this.dbClient.queryWithParams(QUERY_RENTAL_CAN_DELIVER, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    message.reply(null);
                    return;
                }
                JsonObject result = reply.result().getRows().get(0);
                String paymentStatus = result.getString("payment_status");
                Integer rentStatus = result.getInteger("rent_status");
                Integer status = result.getInteger("status");
                String departureDate = result.getString("departure_date");
                String departureTime = result.getString("departure_time");

                if (paymentStatus.equals("1")
                        && rentStatus == 1
                        && status == 1
                        && departureDateValid(departureDate)) {
                    result.put("valid", true);
                } else {
                    result.put("valid", false);
                }
                message.reply(result);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }
    private CompletableFuture<JsonObject> insertRentalPayments(SQLConnection con,JsonArray payments, String eventName, String reservationCode, int rentalId, JsonObject result, Message<JsonObject> message, ArrayList<Integer> tickets, Double summDeposit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        final int userId = payments.getJsonObject(0).getInteger(CREATED_BY);
        con.queryWithParams(QUERY_CASH_OUT_EMPLOYEE, new JsonArray().add(userId), queryReply -> {
            try {
                if (queryReply.failed()){
                    throw queryReply.cause();
                }

                List<JsonObject> res = queryReply.result().getRows();
                if (res.isEmpty()) {
                    throw new PropertyValueException("payments", "Employee needs to have an opened cash out");
                }
                int cashOutId = res.get(0).getInteger("id");
                JsonObject cashChange = message.body().getJsonObject("cash_change");

                double summDepositP = 0.0;
                boolean isDeposit = false;
                double amount = 0.0;
                for (int i = 0; i < payments.size(); i++){
                    JsonObject payment = payments.getJsonObject(i);
                     isDeposit = payment.getBoolean("is_deposit", false);
                     amount = payment.getDouble(AMOUNT);
                    summDepositP += isDeposit ? amount : 0;
                }
                JsonObject ticket = new JsonObject()
                        .put("cash_out_id", cashOutId)
                        .put("rental_id", rentalId)
                        .put("iva", isDeposit ? 0 : this.getIva(amount - summDepositP, cashChange.getDouble("iva_percent")))
                        .put("total",isDeposit ?  summDeposit : amount - summDepositP)
                        .put("paid",isDeposit ? summDeposit : cashChange.getDouble("paid"))
                        .put("paid_change", isDeposit ? 0 : cashChange.getDouble("paid_change"))
                        .put("action", isDeposit ? "income" : "purchase")
                        .put("created_by", userId)
                        .put("ticket_code", UtilsID.generateID("T"));
                JsonArray ticketParams = new JsonArray()
                        .add(cashOutId)
                        .add(rentalId)
                        .add(isDeposit ? 0 : this.getIva(amount - summDepositP, cashChange.getDouble("iva_percent")))
                        .add(isDeposit ?  summDeposit : amount - summDepositP)
                        .add(isDeposit ? summDeposit : cashChange.getDouble("paid"))
                        .add(isDeposit ? 0 : cashChange.getDouble("paid_change"))
                        .add(isDeposit ? "income" : "purchase")
                        .add(userId)
                        .add(UtilsID.generateID("T"));

                con.updateWithParams(INSERT_TICKET_PAYMENT, ticketParams, insertTicketReply -> {
                    try {
                        if (insertTicketReply.failed()){
                            throw insertTicketReply.cause();
                        }
                        int ticketId = insertTicketReply.result().getKeys().getInteger(0);
                        ticket.put("id", ticketId);
                        JsonArray insertTicketDetailsParams = this.generateInsertTicketsDetailsParams(ticket, payments, eventName, userId);
                        con.updateWithParams(INSERT_TICKET_DETAIL,insertTicketDetailsParams, ticketDetailRaply -> {
                            try {
                                 if (ticketDetailRaply.failed()){
                                    throw ticketDetailRaply.cause();
                                }
                                for (int i = 0; i < payments.size(); i++) {
                                    payments.getJsonObject(i).put("rental_id", rentalId).put("ticket_id", ticketId);
                                }
                                PaymentDBV objPayment = new PaymentDBV();
                                objPayment.insertPayments(con, payments).whenComplete((paymentResult, error) -> {
                                    try {
                                        if(error != null){
                                            throw error;
                                        }
                                        List<JsonArray> batchParams = new ArrayList<>();
                                        for (int i = 0; i < payments.size(); i++) {
                                            int paymentId = paymentResult.getJsonObject(i).getInteger("id");
                                            double quantity = payments.getJsonObject(i).getDouble("amount");
                                            batchParams.add(new JsonArray().add(cashOutId).add(paymentId).add(quantity).add(userId));
                                        }
                                        con.batchWithParams(INSERT_CASH_OUT_MOVE ,batchParams, cashOutMoveReply -> {
                                            try {
                                                if (cashOutMoveReply.failed()){
                                                    throw cashOutMoveReply.cause();
                                                }
                                                tickets.add(ticketId);
                                                result.put("id", rentalId)
                                                        .put("reservation_code", reservationCode)
                                                        .put("is_quotation", false);
                                                future.complete(result);
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                future.completeExceptionally(t);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Inserts the payments with cash out moves, and tickets
     *
     * @param payments JsonArray with payments to insert
     * @param eventName name of the event of the payment, Register, Delivery, Reception, Other
     * @param reservationCode rental reservation code to pay
     * @param rentalId rental identifier
     * @param con sql connection in actual transaction
     * @param message message to response the result of the transaction
     */
    private void paymentInsert(JsonArray payments, String eventName, String reservationCode, int rentalId, SQLConnection con, Message<JsonObject> message) {
        final int userId = payments.getJsonObject(0).getInteger(CREATED_BY);
        con.queryWithParams(QUERY_CASH_OUT_EMPLOYEE, new JsonArray().add(userId), queryReply -> {
            try {
                if (queryReply.failed()){
                    throw queryReply.cause();
                }

                List<JsonObject> res = queryReply.result().getRows();
                if (res.isEmpty()) {
                    throw new PropertyValueException("payments", "Employee needs to have an opened cash out");
                }
                int cashOutId = res.get(0).getInteger("id");
                JsonObject cashChange = message.body().getJsonObject("cash_change");

                double summDeposit = 0.0;
                for (int i = 0; i < payments.size(); i++){
                    JsonObject payment = payments.getJsonObject(i);
                    boolean isDeposit = payment.getBoolean("is_deposit", false);
                    double amount = payment.getDouble(AMOUNT);
                    summDeposit += isDeposit ? amount : 0;
                }

                JsonObject ticket = new JsonObject()
                        .put("cash_out_id", cashOutId)
                        .put("rental_id", rentalId)
                        .put("iva", this.getIva(cashChange.getDouble("total") - summDeposit, cashChange.getDouble("iva_percent")))
                        .put("total", cashChange.getDouble("total"))
                        .put("paid", cashChange.getDouble("paid"))
                        .put("paid_change", cashChange.getDouble("paid_change"))
                        .put("action", "income")
                        .put("created_by", userId)
                        .put("ticket_code", UtilsID.generateID("T"));
                String insertTicket = this.generateGenericCreate("tickets", ticket);
                con.update(insertTicket, insertTicketReply -> {
                    try {
                        if (insertTicketReply.failed()){
                            throw insertTicketReply.cause();
                        }
                        int ticketId = insertTicketReply.result().getKeys().getInteger(0);
                        ticket.put("id", ticketId);
                        String insertTicketDetails = this.generateInsertTicketDetails(ticket, payments, eventName, userId);
                        con.update(insertTicketDetails, ticketDetailRaply -> {
                            try {
                                if (ticketDetailRaply.failed()){
                                    throw ticketDetailRaply.cause();
                                }
                                for (int i = 0; i < payments.size(); i++) {
                                    payments.getJsonObject(i).put("rental_id", rentalId).put("ticket_id", ticketId);
                                }
                                //////////////////////
                                PaymentDBV objPayment = new PaymentDBV();
                                objPayment.insertPayments(con, payments).whenComplete((paymentResult, error) -> {
                                    try {
                                        if(error != null){
                                            throw error;
                                        }
                                        List<String> cashOutMoveBatch = new ArrayList<>();
                                        for (int i = 0; i < payments.size(); i++) {
                                            int paymentId = paymentResult.getJsonObject(i).getInteger("id");
                                            double quantity = payments.getJsonObject(i).getDouble("amount");
                                            cashOutMoveBatch.add(this.generateGenericCreate("cash_out_move", new JsonObject()
                                                    .put("cash_out_id", cashOutId)
                                                    .put("payment_id", paymentId)
                                                    .put("quantity", quantity)
                                                    .put("move_type", "0")
                                                    .put(CREATED_BY, userId)));
                                        }
                                        con.batch(cashOutMoveBatch, cashOutMoveReply -> {
                                            try {
                                                if (cashOutMoveReply.failed()){
                                                    throw cashOutMoveReply.cause();
                                                }
                                                this.commit(con, message, new JsonObject()
                                                        .put("id", rentalId)
                                                        .put("reservation_code", reservationCode)
                                                        .put("is_quotation", false)
                                                        .put("ticket_id", ticketId));
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(con, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(con, t, message);
                                    }
                                });
                                /*String paymentsInsertSQL = this.generateGenericInsertWithValues("payment", payments);
                                con.update(paymentsInsertSQL, paymentsReply -> {
                                    if (paymentsReply.succeeded()) {
                                        JsonArray paymentResult = paymentsReply.result().getKeys();
                                        List<String> cashOutMoveBatch = new ArrayList<>();
                                        for (int i = 0; i < payments.size(); i++) {
                                            int paymentId = paymentResult.getInteger(i);
                                            double quantity = payments.getJsonObject(i).getDouble("amount");
                                            cashOutMoveBatch.add(this.generateGenericCreate("cash_out_move", new JsonObject()
                                                    .put("cash_out_id", cashOutId)
                                                    .put("payment_id", paymentId)
                                                    .put("quantity", quantity)
                                                    .put("move_type", "0")
                                                    .put(CREATED_BY, userId)));
                                        }
                                        con.batch(cashOutMoveBatch, cashOutMoveReply -> {
                                            if (cashOutMoveReply.succeeded()) {
                                                this.commit(con, message, new JsonObject()
                                                        .put("id", rentalId)
                                                        .put("reservation_code", reservationCode)
                                                        .put("is_quotation", false)
                                                        .put("ticket_id", ticketId));
                                            } else {
                                                this.rollback(con, cashOutMoveReply.cause(), message);
                                            }
                                        });
                                    } else {
                                        this.rollback(con, paymentsReply.cause(), message);
                                    }
                                });*/
                                ///////////////////////
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(con, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private double getIva(double amount, double ivaPercent) {
        return amount - (amount / (1 + (ivaPercent / 100)));
    }

    private void evaluateUpdateInsertList(List<String> batch, String tableName,
            String propertieName, JsonObject body) {
        JsonArray elements = body.getJsonArray(propertieName);
        int rentalId = body.getInteger("id");
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                JsonObject element = elements.getJsonObject(i);
                element.put("rental_id", rentalId);
                Integer elementId = element.getInteger("id");
                if (elementId != null) {
                    element.remove("created_at");
                    element.remove("created_by");
                    element.put("updated_by", body.getInteger("updated_by"));
                    element.put("updated_at", body.getString("updated_at"));
                    batch.add(this.generateGenericUpdateString(tableName, element));
                } else {
                    element.put("created_by", body.getInteger("updated_by"));
                    batch.add(this.generateGenericCreate(tableName, element));
                }
            }
        }
    }

    private void evaluateUpdateInsert(List<String> batch, String tableName,
            String propertieName, JsonObject body) {
        int rentalId = body.getInteger("id");
        JsonObject element = body.getJsonObject(propertieName);
        if (element != null) {
            Integer elementId = element.getInteger("id");
            element.put("rental_id", rentalId);
            if (elementId != null) {
                element.remove("created_at");
                element.remove("created_by");
                element.put("updated_by", body.getInteger("updated_by"));
                element.put("updated_at", body.getString("updated_at"));
                batch.add(this.generateGenericUpdateString(tableName, element));
            } else {
                element.put("created_by", body.getInteger("updated_by"));
                batch.add(this.generateGenericCreate(tableName, element));
            }
        }
    }

    private boolean departureDateValid(String date) {
        try {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            cal.setTime(sdf.parse(date));
            int rentalDay = cal.get(Calendar.DAY_OF_YEAR);
            cal.setTimeInMillis(System.currentTimeMillis());
            int actualDay = cal.get(Calendar.DAY_OF_YEAR);
            if (rentalDay == actualDay || (rentalDay + 1) == actualDay) {
                return true;
            }
            return false;
        } catch (ParseException ex) {
            return false;
        }
    }

    private void validateAndInsertReturnMoney(JsonObject body, Double extraCharges, Double checklistCharges, SQLConnection con, Message<JsonObject> message) {
        try {
            String reservationCode = body.getString("reservation_code");
            Integer createdBy = body.getInteger("updated_by");
            Double toReturn = body.getDouble("expense");
            Double penalty = body.getDouble("penalty_amount");
            Integer penaltyDays = body.getInteger("penalty_days");
            Double guaranteeDeposit = body.getDouble("guarantee_deposit");

            if (reservationCode == null) {
                throw new Exception("reservation_code is needed");
            }
            JsonArray params = new JsonArray().add(reservationCode);
            con.queryWithParams(QUERY_DEPOSIT, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    JsonObject deposit = reply.result().getRows().get(0);
                    //insert cash out move
                    con.queryWithParams(QUERY_CASH_OUT_EMPLOYEE, new JsonArray().add(createdBy), queryReply -> {
                        try {
                            if (queryReply.failed()){
                                throw queryReply.cause();
                            }

                            List<JsonObject> res = queryReply.result().getRows();
                            if (res.isEmpty()) {
                                throw new PropertyValueException("expense", "Employee needs to have an opened cash out");
                            }
                            int cashOutId = res.get(0).getInteger("id");

                            Future f1 = Future.future();
                            Future f2 = Future.future();

                            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                    new JsonObject().put("fieldName", "currency_id"),
                                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                                    f1.completer());

                            con.query(QUERY_EXPENSE_CONCEPT_RETURN, f2.completer());

                            CompositeFuture.all(f1, f2).setHandler(r -> {
                                try {
                                    if (r.failed()){
                                        throw r.cause();
                                    }
                                    JsonObject currencyId = r.result().<Message<JsonObject>>resultAt(0).body();
                                    List<JsonObject> expenses = r.result().<ResultSet>resultAt(1).getRows();
                                    JsonArray details = new JsonArray();
                                    Integer expenseConceptId = null;
                                    if (!expenses.isEmpty()) {
                                        expenseConceptId = expenses.get(0).getInteger("id");
                                    }
                                    JsonObject expense = new JsonObject()
                                            .put("rental_id", body.getInteger("id"))
                                            .put("payment_method_id", deposit.getInteger("payment_method_id"))
                                            .put("amount", toReturn)
                                            .put("reference", "Devolución por renta de Vans #"+ reservationCode)
                                            .put("currency_id", Integer.parseInt(currencyId.getString("value")))
                                            .put("created_by", createdBy)
                                            .put("expense_concept_id", expenseConceptId)
                                            .put("description", "Devolución por renta de Vans #"+ reservationCode);

                                    details.add(new JsonObject().put("quantity", 1)
                                            .put("detail", "Regreso de depósito en grarantía")
                                            .put("unit_price", guaranteeDeposit)
                                            .put("amount", guaranteeDeposit) );
                                    if(penaltyDays > 0) {
                                        details.add(new JsonObject().put("quantity", penaltyDays)
                                                .put("detail", "Penalización por dias extras")
                                                .put("unit_price", (penalty / penaltyDays) * -1)
                                                .put("amount", penalty * -1));
                                    }
                                    if(extraCharges > 0) {
                                        details.add(new JsonObject().put("quantity", 1)
                                                .put("detail", "Cargos extras")
                                                .put("unit_price", extraCharges * -1)
                                                .put("amount", extraCharges * -1));
                                    }
                                    if(checklistCharges > 0) {
                                        details.add(new JsonObject().put("quantity", 1)
                                                .put("detail", "Cargos por daño")
                                                .put("unit_price", checklistCharges * -1)
                                                .put("amount", checklistCharges * -1));
                                    }
                                    Double totalPayments = toReturn;
                                    JsonObject cashChange = new JsonObject().put("paid", totalPayments).put("total", totalPayments).put("paid_change", 0.0);
                                    this.insertTicket(con, cashOutId, totalPayments, cashChange, createdBy, 0.0, "return", body.getInteger("id"), details).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                        try {
                                            if (ticketError != null){
                                                throw ticketError;
                                            }
                                            expense.put("ticket_id", ticket.getInteger("id"));
                                            this.insertExpense(con, expense, cashOutId, createdBy).whenComplete((JsonObject expenseReturn, Throwable expenseError) -> {
                                                try {
                                                    if (expenseError != null) {
                                                        throw expenseError;
                                                    }
                                                    this.commit(con, message, new JsonObject().put("reservation_code", reservationCode).put("ticket_id", ticket.getInteger("id")));
                                                } catch (Throwable t){
                                                    t.printStackTrace();
                                                    this.rollback(con, t, message);
                                                }
                                            });
                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(con, t, message);
                                        }
                                    });
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    rollback(con, t, message);
                                }
                            });
                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(con, t, message);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(con, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(con, t, message);
        }
    }

    private void validateReceptionAndInsertPayments(JsonObject body, String eventName, SQLConnection con, Message<JsonObject> message) {
        try {
            String reservationCode = body.getString("reservation_code");
            if (reservationCode == null) {
                throw new Exception("reservation_code is needed");
            }
            JsonArray params = new JsonArray().add(reservationCode);
            con.queryWithParams(QUERY_DEPOSIT, params, reply -> {
                try {
                    if (reply.failed()){
                        reply.cause();
                    }
                    JsonObject deposit = reply.result().getRows().get(0);
                    Integer rentalId = body.getInteger("id");
                    Double penalty = body.getDouble("penalty_amount");
                    JsonArray payments = body.getJsonArray("payments");
                    JsonArray extraCharges = body.getJsonArray("rental_extra_charges");
                    JsonArray checklistCharges = body.getJsonArray("checklist");
                    double sumPayment = 0;
                    double sumExtraCharges = 0;
                    for (int i = 0; i < payments.size(); i++) {
                        JsonObject payment = payments.getJsonObject(i);
                        Double amount = payment.getDouble("amount");
                        payment.put("created_by", body.getInteger("updated_by"));
                        sumPayment += amount;
                    }
                    if(extraCharges != null) {
                        for (int i = 0; i < extraCharges.size(); i++) {
                            JsonObject extraCharge = extraCharges.getJsonObject(i);
                            Double amount = extraCharge.getDouble("amount");

                            sumExtraCharges += amount;
                        }
                    }
                    if(checklistCharges != null) {
                        for (int i = 0; i < checklistCharges.size(); i++) {
                            JsonObject checklistCharge = checklistCharges.getJsonObject(i);
                            Double amount = checklistCharge.getDouble("damage_amount");

                            sumExtraCharges += amount;
                        }
                    }
                    double difference = penalty + sumExtraCharges - deposit.getDouble("guarantee_deposit");

                    if(difference > sumPayment){
                        throw new PropertyValueException("payments", "lower than difference to pay");
                    }
                    if (difference < sumPayment) {
                        throw new PropertyValueException("payments", "greater than difference to pay");
                    }

                    con.updateWithParams(QUERY_SET_RENTAL_PAID, new JsonArray().add(rentalId), updateReply -> {
                        try {
                            if (updateReply.failed()){
                                throw updateReply.cause();
                            }
                            this.paymentInsert(payments, eventName, reservationCode, rentalId, con, message);
                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(con, t, message);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(con, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(con, t, message);
        }
    }
    private void validateAndInsertPayments(JsonObject body, String eventName, SQLConnection con, Message<JsonObject> message) {
        try {
            String reservationCode = body.getString("reservation_code");
            if (reservationCode == null) {
                throw new Exception("reservation_code is needed");
            }
            JsonArray params = new JsonArray().add(reservationCode);
            Future<ResultSet> f1 = Future.future();
            Future<ResultSet> f2 = Future.future();
            dbClient.queryWithParams(QUERY_PAYMENT_BALANCE_WITH_ID, params, f1.completer());
            dbClient.queryWithParams(QUERY_DEPOSIT_BALANCE_TOTAL, params, f2.completer());
            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> paymentBalanceResultSet = reply.result().<ResultSet>resultAt(0).getRows();
                    List<JsonObject> depositBalanceResultSet = reply.result().<ResultSet>resultAt(1).getRows();
                    JsonObject payBalance = paymentBalanceResultSet.get(0);
                    Integer rentalId = payBalance.getInteger("rental_id");
                    if (rentalId == null) {
                        throw new Exception("rental does not exist");
                    }
                    if (payBalance.getDouble("total_paid") == null) {
                        payBalance.put("total_paid", 0f);
                    }
                    if (payBalance.getDouble("missing_to_pay") == null) {
                        payBalance.put("missing_to_pay", payBalance.getDouble("total_amount"));
                    }

                    JsonObject depositBalance = new JsonObject();
                    if (depositBalanceResultSet.isEmpty()) {
                        depositBalance.put("guarantee_deposit", payBalance.getDouble("guarantee_deposit"));
                        depositBalance.put("missing_to_deposit", payBalance.getDouble("guarantee_deposit"));
                        depositBalance.put("total_deposited", 0f);
                    } else {
                        depositBalance = depositBalanceResultSet.get(0);

                        if (depositBalance.getDouble("missing_to_deposit") == null) {
                            depositBalance.put("missing_to_deposit", depositBalance.getDouble("guarantee_deposit"));
                            depositBalance.put("total_deposited", 0f);
                        }

                    }

                    //validate payments amount with the already total amount paid of the rental
                    JsonArray payments = body.getJsonArray("payments");
                    double sumPayment = 0;
                    double sumDeposit = 0;
                    for (int i = 0; i < payments.size(); i++) {
                        JsonObject payment = payments.getJsonObject(i);
                        payment.put("amount", Double.parseDouble(String.format("%.2f", payment.getDouble("amount"))));
                        Double amount = payment.getDouble("amount");
                        Boolean extraCharge = payment.getBoolean("is_extra_charge");
                        if (extraCharge != null && extraCharge) {
                            continue;
                        }
                        Boolean isDeposit = payment.getBoolean("is_deposit");
                        if (isDeposit != null && isDeposit) {
                            sumDeposit += amount;
                            continue;
                        }
                        sumPayment += amount;
                    }

                    double missingToPay = payBalance.getDouble("missing_to_pay");
                    double missingToDeposit = depositBalance.getDouble("missing_to_deposit");
                    String myEvent = eventName;
                    Double totalMissing = payBalance.getDouble("missing_to_pay") + depositBalance.getDouble("missing_to_deposit");
                    if(totalMissing == sumPayment){
                        myEvent = "Liquidación ";
                    }
                    if (sumPayment > missingToPay) {
                        throw new Exception("Can't pay more than the missing amount of the rental");
                    }
                    if (sumDeposit > missingToDeposit) {
                        throw new Exception("Can't pay more deposit than the missing deposit amount of the rental");
                    }

                    final double totalAmount = payBalance.getDouble("total_amount");
                    final double totalDeposit = depositBalance.getDouble("guarantee_deposit");
                    final double actualTotalAmount = sumPayment + payBalance.getDouble("total_paid");
                    final double actualTotalDeposit = sumDeposit + depositBalance.getDouble("total_deposited");
                    final String myEventName = myEvent;

                    String query;
                    if (actualTotalAmount == totalAmount && actualTotalDeposit == totalDeposit) {
                        query = QUERY_SET_RENTAL_PAID;
                    } else {
                        query = QUERY_SET_RENTAL_PARTIAL_PAID;
                    }

                    con.updateWithParams(query, new JsonArray().add(rentalId), updateReply -> {
                        try {
                            if (updateReply.failed()){
                                throw updateReply.cause();
                            }
                            this.paymentInsert(payments, myEventName, reservationCode, rentalId, con, message);
                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(con, t, message);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(con, t, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(con, t, message);
        }
    }

    /**
     * Generates the insert statement for ticket details with the concepts of the payments
     *
     * @param ticket ticket header
     * @param payments jsonarray with payments
     * @param eventName name of the event, Reception, Delivery, Register
     * @param userId user id working
     * @return String with the id
     */
    private JsonArray generateInsertTicketsDetailsParams(JsonObject ticket, JsonArray payments, String eventName, int userId){
        HashMap<String, Double> paymentTypesAmount = new HashMap<>();
        for (int i = 0; i < payments.size(); i++) {
            Boolean isDeposit = payments.getJsonObject(i).getBoolean("is_deposit");
            Double paymentAmount = payments.getJsonObject(i).getDouble("amount");
            String concept;
            if (isDeposit == null) {
                isDeposit = false;
            }
            if (isDeposit) {
                concept = "Depósito en garantía";
            } else {
                Boolean isExtra = payments.getJsonObject(i).getBoolean("is_extra_charge");
                if (isExtra == null) {
                    isExtra = false;
                }
                if (isExtra) {
                    concept = eventName + " cargos extras";
                } else {
                    concept = eventName + " renta";
                }
            }
            Double amount = paymentTypesAmount.get(concept);
            if (amount == null) {
                paymentTypesAmount.put(concept, paymentAmount);
            } else {
                paymentTypesAmount.put(concept, amount + paymentAmount);
            }
        }
        JsonArray ticketDetails = new JsonArray();
        for (Map.Entry<String, Double> entry : paymentTypesAmount.entrySet()) {
            ticketDetails
                    .add(entry.getValue())
                    .add(1)
                    .add(entry.getKey())
                    .add(ticket.getInteger("id"))
                    .add(entry.getValue())
                    .add(userId);
        }

        return  ticketDetails;
    }
    private String generateInsertTicketDetails(JsonObject ticket, JsonArray payments, String eventName, int userId) {
        HashMap<String, Double> paymentTypesAmount = new HashMap<>();
        for (int i = 0; i < payments.size(); i++) {
            Boolean isDeposit = payments.getJsonObject(i).getBoolean("is_deposit");
            Double paymentAmount = payments.getJsonObject(i).getDouble("amount");
            String concept;
            if (isDeposit == null) {
                isDeposit = false;
            }
            if (isDeposit) {
                concept = "Depósito en garantía";
            } else {
                Boolean isExtra = payments.getJsonObject(i).getBoolean("is_extra_charge");
                if (isExtra == null) {
                    isExtra = false;
                }
                if (isExtra) {
                    concept = eventName + " cargos extras";
                } else {
                    concept = eventName + " renta";
                }
            }
            Double amount = paymentTypesAmount.get(concept);
            if (amount == null) {
                paymentTypesAmount.put(concept, paymentAmount);
            } else {
                paymentTypesAmount.put(concept, amount + paymentAmount);
            }
        }
        JsonArray ticketDetails = new JsonArray();
        for (Map.Entry<String, Double> entry : paymentTypesAmount.entrySet()) {
            ticketDetails.add(new JsonObject()
                    .put("ticket_id", ticket.getInteger("id"))
                    .put("quantity", 1)
                    .put("detail", entry.getKey())
                    .put("unit_price", entry.getValue())
                    .put("amount", entry.getValue())
                    .put("created_by", userId));
        }
        return this.generateGenericInsertWithValues("tickets_details", ticketDetails);
    }

    private void canCancel(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String reservationCode = body.getString("reservationCode");
            Future f1 = Future.future();
            Future f2 = Future.future();
            Future f3 = Future.future();

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "time_before_cancel_van"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                    f1.completer());

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "cancel_penalty_percent"),
                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                    f2.completer());

            dbClient.queryWithParams(QUERY_RENTAL_CANCEL_DATA,
                    new JsonArray().add(reservationCode).add(reservationCode),
                    f3.completer());

            //obtener total pagado de la renta
            //obtener datos de la renta
            CompositeFuture.all(f1, f2, f3).setHandler(r -> {
                if (r.succeeded()) {
                    try {
                        if (r.failed()){
                            throw r.cause();
                        }
                        JsonObject timeToCancelConf = r.result().<Message<JsonObject>>resultAt(0).body();
                        JsonObject cancelPenaltyconf = r.result().<Message<JsonObject>>resultAt(1).body();
                        List<JsonObject> rentalPaymentData = r.result().<ResultSet>resultAt(2).getRows();
                        if (rentalPaymentData.isEmpty()) {
                            throw new Exception("Rental not found");
                        }
                        JsonObject result = new JsonObject();
                        JsonObject rentalData = rentalPaymentData.get(0);
                        int hoursToCancel = Integer.parseInt(timeToCancelConf.getString("value", "24"));
                        double cancelPenaltyPercent = Double.parseDouble(cancelPenaltyconf.getString("value", "30"));

                        String departureDate = rentalData.getString("departure_date");
                        String departureTime = rentalData.getString("departure_time");
                        if (departureDate == null || departureTime == null) {
                            message.reply(null);
                            return;
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        try {
                            Date departure = sdf.parse(departureDate + " " + departureTime);
                            long departureMillis = departure.getTime();
                            Date local = UtilsDate.getDateConvertedTimeZone(UtilsDate.timezone, new Date());
                            long actualMillis = local.getTime();
                            if (actualMillis > departureMillis) {// the departure date has already passed
                                result.put("can_cancel", Boolean.FALSE)
                                        .put("reason", "departure date has already passed but not return money")
                                        .put("rental_data", rentalData);
                            } else {
                                long diference = departureMillis - actualMillis;
                                double horasDiferencia = diference / 1000.0 / 60.0 / 60.0;
                                if (horasDiferencia > hoursToCancel) {
                                    //calculate amount of percent as penalty
                                    double totalAmount = rentalData.getDouble("total_amount");
                                    double penalty = Math.ceil(totalAmount * (cancelPenaltyPercent / 100d));

                                    double totalPaid = rentalData.getDouble("total_paid");
                                    double toReturn = UtilsMoney.round(totalPaid - penalty, 2);
                                    if (toReturn < 0) {
                                        toReturn = 0;
                                    }
                                    result.put("can_cancel", Boolean.TRUE)
                                            .put("reason", "on time to cancel")
                                            .put("to_return", toReturn)
                                            .put("penalty_amount", penalty)
                                            .put("rental_data", rentalData);
                                } else {
                                    result.put("can_cancel", Boolean.FALSE)
                                            .put("reason", "not on time to cancel")
                                            .put("to_return", 0)
                                            .put("rental_data", rentalData);
                                }
                            }
                            message.reply(result);
                        } catch (ParseException ex) {
                            message.fail(ErrorCodes.DB_ERROR.ordinal(), ex.getMessage());
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                } else {
                    message.fail(ErrorCodes.DB_ERROR.ordinal(), r.cause().getMessage());
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void cancel(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String reservationCode = body.getString("reservation_code");
            String cancelReason = body.getString("cancel_reason");
            Integer createdBy = body.getInteger(CREATED_BY);
            double penalty = body.getDouble("penalty_amount");
            double toReturn = body.getDouble("to_return");
            Integer paymentMethodId = body.getInteger("payment_method_id");
            JsonObject rentalData = body.getJsonObject("rental_data");

            //update rental to status cancel
            this.startTransaction(message, con -> {
                JsonArray cancelParams = new JsonArray().add(cancelReason).add(createdBy).add(reservationCode);
                con.updateWithParams(QUERY_CANCEL_RENTAL, cancelParams, updateResult -> {
                    try {
                        if (updateResult.failed()){
                            throw updateResult.cause();
                        }
                        //insert cash out move
                        con.queryWithParams(QUERY_CASH_OUT_EMPLOYEE, new JsonArray().add(createdBy), queryReply -> {
                            try {
                                if (queryReply.failed()){
                                    queryReply.cause();
                                }
                                List<JsonObject> res = queryReply.result().getRows();

                                if (res.isEmpty()) {
                                    throw new PropertyValueException("payments", "Employee needs to have an opened cash out");
                                }
                                int cashOutId = res.get(0).getInteger("id");
                                Future f1 = Future.future();
                                Future f2 = Future.future();

                                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                        new JsonObject().put("fieldName", "currency_id"),
                                        new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                                        f1.completer());

                                con.query(QUERY_EXPENSE_CONCEPT_CANCEL, f2.completer());

                                CompositeFuture.all(f1, f2).setHandler(r -> {
                                    try {
                                        if (r.failed()){
                                            throw r.cause();
                                        }
                                        JsonObject currencyId = r.result().<Message<JsonObject>>resultAt(0).body();
                                        List<JsonObject> expenses = r.result().<ResultSet>resultAt(1).getRows();
                                        Integer expenseConceptId = null;
                                        if (!expenses.isEmpty()) {
                                            expenseConceptId = expenses.get(0).getInteger("id");
                                        }

                                        JsonArray details = new JsonArray();
                                        JsonArray expensesArray = new JsonArray();
                                        JsonObject expense = new JsonObject()
                                                .put("rental_id", rentalData.getInteger("id"))
                                                .put("payment_method_id", paymentMethodId)
                                                .put("amount", toReturn)
                                                .put("reference", "Cancelación de renta")
                                                .put("currency_id", Integer.parseInt(currencyId.getString("value")))
                                                .put("created_by", createdBy)
                                                .put("expense_concept_id", expenseConceptId)
                                                .put("description", "Cancelación de renta, retorno de anticipo");
                                        expensesArray.add(expense);

                                        details.add(new JsonObject().put("quantity", 1)
                                                .put("detail", "Cancelación de renta.")
                                                .put("unit_price", rentalData.getDouble("total_paid"))
                                                .put("amount", rentalData.getDouble("total_paid")));
                                        details.add(new JsonObject().put("quantity", 1)
                                                .put("detail", "Penalización por cancelación de renta.")
                                                .put("unit_price", penalty * -1)
                                                .put("amount", penalty * -1));

                                        // return deposit
                                        if(rentalData.getDouble("total_deposited") > 0){
                                            expense = new JsonObject()
                                                    .put("rental_id", rentalData.getInteger("id"))
                                                    .put("payment_method_id", paymentMethodId)
                                                    .put("amount", rentalData.getDouble("total_deposited"))
                                                    .put("reference", "Cancelación de renta")
                                                    .put("currency_id", Integer.parseInt(currencyId.getString("value")))
                                                    .put("created_by", createdBy)
                                                    .put("expense_concept_id", expenseConceptId)
                                                    .put("description", "Regreso de depósito en grarantía");
                                            details.add(new JsonObject().put("quantity", 1)
                                                    .put("detail", "Regreso de depósito en grarantía")
                                                    .put("unit_price", rentalData.getDouble("total_deposited"))
                                                    .put("amount", rentalData.getDouble("total_deposited")));
                                            expensesArray.add(expense);
                                        }
                                        // create ticket
                                        Double totalPayments = toReturn + rentalData.getDouble("total_deposited");
                                        JsonObject cashChange = new JsonObject().put("paid", totalPayments).put("total", totalPayments).put("paid_change", 0.0);
                                        this.insertTicket(con, cashOutId, totalPayments, cashChange, createdBy, 0.0, "cancel", rentalData.getInteger("id"), details).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                            try {
                                                if (ticketError != null) {
                                                    throw ticketError;
                                                }
                                                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                for (int i = 0; i < expensesArray.size(); i++) {
                                                    expensesArray.getJsonObject(i).put("ticket_id", ticket.getInteger("id"));
                                                    pTasks.add(insertExpense(con, expensesArray.getJsonObject(i), cashOutId, createdBy));
                                                }
                                                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[expensesArray.size()]));
                                                allPayments.whenComplete((ps, pt) -> {
                                                    try {
                                                        if (pt != null) {
                                                            throw pt;
                                                        }
                                                        this.commit(con, message, new JsonObject().put("rental_id", rentalData.getInteger("id"))
                                                                .put("reservation_code", reservationCode)
                                                                .put("ticket_id", ticket.getInteger("id")));
                                                    } catch (Throwable t){
                                                        t.printStackTrace();
                                                        this.rollback(con, t, message);
                                                    }
                                                });
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(con, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        rollback(con, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(con, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> insertExpense(SQLConnection conn, JsonObject expense, Integer cashOutId, Integer createdBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String insertExpenses = generateGenericCreate("expense", expense);

            JsonObject cashOutMove = new JsonObject()
                    .put("cash_out_id", cashOutId)
                    .put("quantity", expense.getDouble("amount"))
                    .put("move_type", "1")
                    .put("created_by", createdBy);

            conn.update(insertExpenses, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    final int id = reply.result().getKeys().getInteger(0);
                    expense.put("id", id);
                    cashOutMove.put("expense_id", id);
                    String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                    conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyInsert) -> {
                        try {
                            if (replyInsert.failed()){
                                throw replyInsert.cause();
                            }
                            future.complete(expense);
                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;

    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Double totalPayments, JsonObject cashChange,
                                                       Integer createdBy, Double ivaPercent, String action, Integer rentalId, JsonArray details) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            Double iva = this.getIva(totalPayments, ivaPercent);
            List<String> inserts = new ArrayList<>();

            // Create ticket_code
            // ticket.put("ticket_code", ticketCode);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
            ticket.put("total", totalPayments);
            ticket.put("created_by", createdBy);
            ticket.put("rental_id", rentalId);
            ticket.put("action", action);
            ticket.put("ticket_code", UtilsID.generateID("T"));

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble("total");
                Double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                Double difference_paid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if (totalPayments < total) {
                    throw new Exception("The payment " + total + " is greater than the total " + totalPayments);
                }
                if (totalPayments > total) {
                    throw new Exception("The payment " + total + " is lower than the total " + totalPayments);
                }
                if (paid_change > difference_paid){
                    throw new Exception("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                }
                if (paid_change < difference_paid){
                    throw new Exception("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                }
            } else {
                ticket.put("paid", totalPayments);
                ticket.put("paid_change", 0.0);
            }
            String insert = this.generateGenericCreate("tickets", ticket);

            conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    final int id = reply.result().getKeys().getInteger(0);
                    ticket.put("id", id);
                    for (int i = 0; i < details.size(); i++) {
                        JsonObject detail = details.getJsonObject(i);
                        JsonObject ticketDetail = new JsonObject();
                        ticketDetail.put("ticket_id", id);
                        ticketDetail.put("quantity", detail.getInteger("quantity"));
                        ticketDetail.put("detail", detail.getString("detail"));
                        ticketDetail.put("unit_price", detail.getDouble("unit_price"));
                        ticketDetail.put("amount", detail.getDouble("amount"));
                        ticketDetail.put("created_by", createdBy);

                        inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    }
                    conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                        try {
                            if (replyInsert.failed()){
                                throw replyInsert.cause();
                            }
                            future.complete(ticket);
                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private void getList(Message<JsonObject> message) {
        dbClient.query(QUERY_RENTALS, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    message.reply(null);
                    return;
                }
                List<JsonObject> rentals = reply.result().getRows();
                message.reply(new JsonArray(rentals));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void sendQuotation(Message<JsonObject> message){
        this.sendInformativeEmail(message);
    }

    private void getPrice(Message<JsonObject> message) {
        JsonObject body = message.body();
        String travelDate = body.getString("travelDate");
        String arrivalDate = body.getString("arrivalDate");
        Integer vehicleId = body.getInteger("vehicleId");

        JsonArray params = new JsonArray()
                .add(arrivalDate).add(travelDate)
                .add(arrivalDate).add(travelDate)
                .add(arrivalDate).add(travelDate)
                .add(vehicleId);

        dbClient.queryWithParams(QUERY_RENTAL_PRICE, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    message.reply(null);
                    return;
                }
                JsonObject rentalPrice = reply.result().getRows().get(0);
                message.reply(rentalPrice);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getDriverCost(Message<JsonObject> message) {
        JsonObject body = message.body();
        execGetDriverCost(body).whenComplete((resultGetDriverCost, errorGetDriverCost) -> {
           try{
               if (errorGetDriverCost != null){
                   throw errorGetDriverCost;
               }
               message.reply(resultGetDriverCost);
           } catch (Throwable t){
               t.printStackTrace();
               reportQueryError(message, t);
           }
        });
    }

    private CompletableFuture<JsonObject> execGetDriverCost(JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String travelDate = body.getString("travelDate");
            String arrivalDate = body.getString("arrivalDate");
            Integer driverVanId = body.getInteger("driverVanId");
            double vanEarning = UtilsMoney.round((body.getDouble("vanEarning") / 100d), 2);

            JsonArray params = new JsonArray()
                    .add(arrivalDate).add(travelDate)
                    .add(vanEarning).add(vanEarning)
                    .add(arrivalDate).add(travelDate)
                    .add(arrivalDate).add(travelDate)
                    .add(driverVanId);

            dbClient.queryWithParams(QUERY_DRIVER_COST, params, reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().getRows().isEmpty()) {
                        future.complete(null);
                    }
                    JsonObject rentalPrice = reply.result().getRows().get(0);
                    rentalPrice.put("total", UtilsMoney.round(rentalPrice.getDouble("total"), 2));
                    future.complete(rentalPrice);
                } catch(Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private void getDeposit(Message<JsonObject> message) {
        JsonObject body = message.body();
        String reservationCode = body.getString("reservation_code");
        if (reservationCode == null) {
            reportQueryError(message, new Throwable("reservation_code is needed"));
            return;
        }

        JsonArray params = new JsonArray().add(reservationCode);
        dbClient.queryWithParams(QUERY_DEPOSIT, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> deposit = reply.result().getRows();

                if (deposit.isEmpty()){
                    message.reply(null);
                } else {
                    message.reply(reply.result().getRows().get(0));
                }

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getPenalty(Message<JsonObject> message) {
        JsonObject body = message.body();
        String receivedAt = body.getString("received_at");
        String code = body.getString("reservation_code");

        JsonArray params = new JsonArray()
                .add(receivedAt).add(receivedAt)
                .add(receivedAt).add(receivedAt)
                .add(code);

        dbClient.queryWithParams(QUERY_RENTAL_PENALTY_DAYS, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                if (reply.result().getRows().isEmpty()) {
                    message.reply(null);
                    return;
                }
                Double penalty = 0.00;
                Integer penalty_days = 0;
                JsonObject rentalPrice = reply.result().getRows().get(0);
                if(rentalPrice.getInteger("time_diff") == 1) {
                    if (rentalPrice.getInteger("hours_diff") > 1 && rentalPrice.getInteger("hours_diff") <= 24) {
                        penalty = rentalPrice.getDouble("rental_price");
                        penalty_days = 1;
                    } else if (rentalPrice.getInteger("hours_diff") > 24) {
                        penalty = UtilsMoney.round(rentalPrice.getDouble("total"), 2);
                        penalty_days = rentalPrice.getInteger("days");
                    }
                }
                body.put("penalty_days", penalty_days);
                body.put("penalty_amount", penalty);
                body.put("rental_price", rentalPrice.getValue("rental_price"));
                message.reply(body);

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void salesReport (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_SALES_REPORT;
            Integer driverVanId = body.getInteger("driver_van_id");
            Double extraEarningPercent = body.getDouble("extra_earning_percent");

            if(body.getBoolean("is_date_created_at")){
                QUERY += " AND r.created_at BETWEEN ? AND ?";
                params.add(body.getString("init_date"))
                        .add(body.getString("end_date"));
            }
            else {
                QUERY += " AND r.departure_date BETWEEN ? AND ?";
                params.add(body.getString("init_date"))
                        .add(body.getString("end_date"));
            }
            if(body.getInteger("branchoffice_id") != null){
                QUERY += " AND r.branchoffice_id = ?";
                params.add(body.getInteger("branchoffice_id"));
            }
            if(body.getInteger("city_id") != null){
                QUERY += " AND b.city_id = ?";
                params.add(body.getInteger("city_id"));
            }
            if(body.getInteger("pickup_branchoffice_id") != null){
                QUERY += " AND r.pickup_branchoffice_id = ?";
                params.add(body.getInteger("pickup_branchoffice_id"));
            }
            if(body.getInteger("pickup_city_id") != null){
                QUERY += " AND r.pickup_city_id = ?";
                params.add(body.getInteger("pickup_city_id"));
            }
            JsonArray paramsCount = params.copy();

            String queryFields = QUERY_SALES_REPORT_FIELDS.concat(QUERY).concat(REPORT_ORDER_BY);
            String queryCount = QUERY_SALES_REPORT_COUNT.concat(QUERY);
            Integer page = body.getInteger("page", 1);
            Integer limit = body.getInteger("limit", MAX_LIMIT);
            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }
            int skip = limit * (page-1);
            queryFields = queryFields.concat(" LIMIT ?,? ");
            params.add(skip).add(limit);
            String finalQueryFields = queryFields;

            this.dbClient.queryWithParams(queryCount, paramsCount, replyCount ->{
               try{
                   if(replyCount.failed()){
                       throw replyCount.cause();
                   }
                   Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                   this.dbClient.queryWithParams(finalQueryFields, params, reply ->{
                       try {
                           if (reply.failed()){
                               throw reply.cause();
                           }
                           List<JsonObject> result = reply.result().getRows();

                           if (result.isEmpty()) {
                               JsonObject totalResult = new JsonObject()
                                       .put("count", count)
                                       .put("items", result.size())
                                       .put("results", result);
                               message.reply(totalResult);
                           } else {
                               List<CompletableFuture<JsonObject>> rentalTasks = new ArrayList<>();
                               List<CompletableFuture<JsonObject>> rentalDaysTasks = new ArrayList<>();
                               List<CompletableFuture<JsonObject>> rentalServiceCostTasks = new ArrayList<>();
                               for (int i = 0; i < result.size(); i++) {
                                   rentalTasks.add(getSalesPayments(result.get(i)));
                                   rentalDaysTasks.add(getSalesPrice(result.get(i)));
                                   rentalServiceCostTasks.add(getServiceCostAndGeneratedUtility(result.get(i), driverVanId, extraEarningPercent));
                               }

                               CompletableFuture.allOf(rentalDaysTasks.toArray(new CompletableFuture[result.size()])).whenComplete((ps, pt) -> {
                                   try {
                                       if (pt != null) {
                                           throw pt;
                                       }
                                       CompletableFuture.allOf(rentalTasks.toArray(new CompletableFuture[result.size()])).whenComplete((s, tt) -> {
                                           try {
                                               if (tt != null) {
                                                   throw tt;
                                               }
                                               CompletableFuture.allOf(rentalServiceCostTasks.toArray(new CompletableFuture[result.size()])).whenComplete((sc, tc) -> {
                                                   try {
                                                       if (tc != null) {
                                                           throw tc;
                                                       }
                                                       JsonObject totalResult = new JsonObject()
                                                               .put("count", count)
                                                               .put("items", result.size())
                                                               .put("results", result);

                                                       message.reply(totalResult);
                                                   } catch (Throwable t){
                                                       t.printStackTrace();
                                                       reportQueryError(message, t);
                                                   }
                                               });
                                           } catch (Throwable t){
                                               t.printStackTrace();
                                               reportQueryError(message, t);
                                           }
                                       });
                                   } catch (Throwable t){
                                       t.printStackTrace();
                                       reportQueryError(message, t);
                                   }
                               });
                           }
                       } catch (Throwable t){
                           t.printStackTrace();
                           reportQueryError(message, t);
                       }
                   });
               } catch (Throwable t){
                   t.printStackTrace();
                   reportQueryError(message, t);
               }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private void salesReportTotals (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_SALES_REPORT_FIELDS.concat(QUERY_SALES_REPORT);
            Integer driverVanId = body.getInteger("driver_van_id");
            Double extraEarningPercent = body.getDouble("extra_earning_percent");

            if(body.getBoolean("is_date_created_at")){
                QUERY += " AND r.created_at BETWEEN ? AND ?";
                params.add(body.getString("init_date"))
                        .add(body.getString("end_date"));
            }
            else {
                QUERY += " AND r.departure_date BETWEEN ? AND ?";
                params.add(body.getString("init_date"))
                        .add(body.getString("end_date"));
            }
            if(body.getInteger("branchoffice_id") != null){
                QUERY += " AND r.branchoffice_id = ?";
                params.add(body.getInteger("branchoffice_id"));
            }
            if(body.getInteger("city_id") != null){
                QUERY += " AND b.city_id = ?";
                params.add(body.getInteger("city_id"));
            }
            if(body.getInteger("pickup_branchoffice_id") != null){
                QUERY += " AND r.pickup_branchoffice_id = ?";
                params.add(body.getInteger("pickup_branchoffice_id"));
            }
            if(body.getInteger("pickup_city_id") != null){
                QUERY += " AND r.pickup_city_id = ?";
                params.add(body.getInteger("pickup_city_id"));
            }

            QUERY = QUERY.concat(REPORT_ORDER_BY);

            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();

                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        List<CompletableFuture<JsonObject>> rentalTasks = new ArrayList<>();
                        List<CompletableFuture<JsonObject>> rentalDaysTasks = new ArrayList<>();
                        List<CompletableFuture<JsonObject>> rentalServiceCostTasks = new ArrayList<>();
                        for (int i = 0; i < result.size(); i++) {
                            rentalTasks.add(getSalesPayments(result.get(i)));
                            rentalDaysTasks.add(getSalesPrice(result.get(i)));
                            rentalServiceCostTasks.add(getServiceCostAndGeneratedUtility(result.get(i), driverVanId, extraEarningPercent));
                        }

                        CompletableFuture.allOf(rentalDaysTasks.toArray(new CompletableFuture[result.size()])).whenComplete((ps, pt) -> {
                            try {
                                if (pt != null) {
                                    throw pt;
                                }
                                CompletableFuture.allOf(rentalTasks.toArray(new CompletableFuture[result.size()])).whenComplete((s, tt) -> {
                                    try {
                                        if (tt != null) {
                                            throw tt;
                                        }
                                        CompletableFuture.allOf(rentalServiceCostTasks.toArray(new CompletableFuture[result.size()])).whenComplete((sc, tc) -> {
                                            try {
                                                if (tc != null) {
                                                    throw tc;
                                                }
                                                Double totalAmount = result.stream().mapToDouble(r->r.getDouble("total_amount")).sum();
                                                Double paymentBalance = result.stream().mapToDouble(r->r.getDouble("payment_balance")).sum();
                                                Double cost = result.stream().mapToDouble(r->r.getDouble("cost")).sum();
                                                Double utility = result.stream().mapToDouble(r->r.getDouble("utility")).sum();
                                                Double profit = result.stream().mapToDouble(r->r.getDouble("profit")).sum();
                                                JsonObject totals = new JsonObject()
                                                    .put("total_amount", totalAmount)
                                                    .put("payment_balance", paymentBalance)
                                                    .put("cost", cost)
                                                    .put("utility", utility)
                                                    .put("profit", profit);
                                                message.reply(totals);
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                reportQueryError(message, t);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        reportQueryError(message, t);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }
    private CompletableFuture<JsonObject> getServiceCostAndGeneratedUtility(JsonObject rental, Integer driverVanId, Double extraEarningPercent){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (rental.getBoolean("has_driver")){
            this.getDirectDriverCost(rental, driverVanId).whenComplete((resultDirectDriverCost, errorDirectDriverCost) -> {
                try{
                    if (errorDirectDriverCost != null){
                        throw errorDirectDriverCost;
                    }
                    Double totalExtracharges = this.getReportTotalExtraCharges(rental.getDouble("extra_charges"), extraEarningPercent);
                    Double cost = this.getReportCost(totalExtracharges, resultDirectDriverCost);
                    rental
                            .put("utility", this.getReportUtility(rental.getDouble("total_amount"), cost));
                    future.complete(rental);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } else {
            Double totalExtraCharges = this.getReportTotalExtraCharges(rental.getDouble("extra_charges"), extraEarningPercent);
            Double cost = this.getReportCost(totalExtraCharges, 0.00);
            rental
                    .put("utility", this.getReportUtility(rental.getDouble("total_amount"), cost));
            future.complete(rental);
        }
        return future;
    }

    private Double getReportCost(Double extraCharges, Double directDriverCost){
        return extraCharges + directDriverCost;
    }

    private Double getReportTotalExtraCharges(Double extraCharges, Double extraEarningPercent){
        return extraCharges / (1 + (extraEarningPercent / 100));
    }

    private Double getReportUtility(Double totalAmount, Double cost){
        return totalAmount - cost;
    }

    private CompletableFuture<Double> getDirectDriverCost(JsonObject rental, Integer driverVanId){
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {
            String departureDate = rental.getString("departure_date");
            String departureTime = rental.getString("departure_time");
            String returnDate = rental.getString("return_date");
            String returnTime = rental.getString("return_time");
            JsonObject params = new JsonObject()
                    .put("travelDate", departureDate.concat(" ").concat(departureTime))
                    .put("arrivalDate", returnDate.concat(" ").concat(returnTime))
                    .put("driverVanId", driverVanId)
                    .put("vanEarning", 0);

            this.execGetDriverCost(params).whenComplete((resultGetDriverCost, errorGetDriverCost) -> {
                try{
                    if (errorGetDriverCost != null){
                        throw errorGetDriverCost;
                    }
                    Double cost = resultGetDriverCost.getDouble("cost");
                    future.complete(cost);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getSalesPayments(JsonObject rental){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(rental.getString("reservation_code") != null ? rental.getString("reservation_code") : "");

        if(rental.getString("payment_status").equals("2")) {
            this.dbClient.queryWithParams(QUERY_PAYMENT_BALANCE, params, handler -> {
                try {
                    if (handler.failed()){
                        throw handler.cause();
                    }
                    List <JsonObject> resultsTracking = handler.result().getRows();
                    if (!resultsTracking.isEmpty()) {
                        rental.put("payment_balance", resultsTracking.get(0).getDouble("missing_to_pay"));
                    } else {
                        rental.put("payment_balance", rental.getDouble("total_amount"));
                    }

                    this.getSalesDeposits(rental).whenComplete((resultDeposit, error) -> {
                        try {
                            if (error != null) {
                                throw error;
                            }
                            future.complete(rental);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } else if(rental.getString("payment_status").equals("0")) {
            rental.put("payment_balance", rental.getDouble("total_amount") + rental.getDouble("guarantee_deposit"));
            future.complete(rental);
        }  else {
            rental.put("payment_balance", 0.0);
            future.complete(rental);
        }

        return future;
    }

    private CompletableFuture<JsonObject> getSalesDeposits(JsonObject rental){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray params = new JsonArray().add(rental.getString("reservation_code") != null ? rental.getString("reservation_code") : "");
        this.dbClient.queryWithParams(QUERY_DEPOSIT_BALANCE_TOTAL, params, handler2->{
            try {
                if (handler2.failed()){
                    throw handler2.cause();
                }
                List <JsonObject> resultsTracking = handler2.result().getRows();
                if (!resultsTracking.isEmpty()) {
                    rental.put("payment_deposit", resultsTracking.get(0).getDouble("missing_to_deposit"));
                } else {
                    rental.put("payment_deposit", rental.getDouble("guarantee_deposit", 0.0));
                }
                future.complete(rental);
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getSalesPrice(JsonObject rental){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray params = new JsonArray()
                .add(rental.getString("return_date") + " " + rental.getString("return_time")).add(rental.getString("departure_date") + " " + rental.getString("departure_time"))
                .add(rental.getString("return_date") + " " + rental.getString("return_time")).add(rental.getString("departure_date") + " " + rental.getString("departure_time"))
                .add(rental.getString("return_date") + " " + rental.getString("return_time")).add(rental.getString("departure_date") + " " + rental.getString("departure_time"))
                .add(rental.getInteger("vehicle_id"));

        this.dbClient.queryWithParams(QUERY_RENTAL_PRICE, params, handler3->{
            try {
                if (handler3.failed()){
                    throw handler3.cause();
                }
                List <JsonObject> resultsTracking = handler3.result().getRows();
                if (!resultsTracking.isEmpty()) {
                    rental.put("rental_days", resultsTracking.get(0).getInteger("days"));
                } else {
                    rental.put("rental_days", 0);
                }
                future.complete(rental);
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void vansList(Message<JsonObject> message) {
        JsonObject body = message.body();

        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        Integer config_vehicle_id = body.getInteger("config_vehicle_id");
        Integer vehicle_id = body.getInteger("vehicle_id");

        String query = QUERY_VANS_LIST;
        JsonArray params = new JsonArray()
            .add(initDate)
            .add(endDate)
            .add(initDate)
            .add(endDate);

        if(vehicle_id != null){
            query = query.concat("AND r.vehicle_id = ?\n");
            params.add(vehicle_id);
        }

        if(config_vehicle_id != null){
            query = query.concat("AND v.config_vehicle_id = ?\n");
            params.add(config_vehicle_id);
        }

        dbClient.queryWithParams(query, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                message.reply(new JsonArray(result));

            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void updateRent(Message<JsonObject> message){
        try{
            startTransaction(message,conn -> {
                JsonObject body = message.body().getJsonObject("update");
                Integer id = body.getInteger("id");
                try{

                    String query = generateGenericUpdateString("rental", body );
                    conn.update(query, reply -> {
                        try {
                            if(reply.failed()){
                                throw reply.cause();
                            }
                            this.commit(conn, message, new JsonObject().put("success", true ).put( "ruta" ,body));

                        }catch (Throwable ex){
                            this.rollback(conn, ex, message);
                            ex.printStackTrace();

                        }
                    });

                }catch (Exception ex){
                    this.rollback(conn, ex , message);
                }
            });
        }catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message,t);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="queries">

    private static final String QUERY_RENTAL_PENALTY_DAYS = "SELECT " +
            "IF (? > CONCAT(r.return_date, ' ', r.return_time), TRUE, FALSE) AS time_diff, " +
            "HOUR(TIMEDIFF(?, CONCAT(r.return_date, ' ', r.return_time))) AS hours_diff, " +
            "CEIL(HOUR(TIMEDIFF(?, CONCAT(r.return_date, ' ', r.return_time))) / 24) AS days, " +
            "r.rental_price, \n" +
            " ((CEIL(HOUR(TIMEDIFF(?, CONCAT(r.return_date, ' ', r.return_time))))\n" +
            "    /\n" +
            "    CONVERT((SELECT value FROM general_setting WHERE FIELD = 'penalties_hours'), DECIMAL(12,2)))\n" +
            "    *\n" +
            "    (CONVERT((SELECT value FROM general_setting WHERE FIELD = 'penalties_percent'), DECIMAL(12,2))\n" +
            "    / 100)\n" +
            "    * r.rental_price) AS total\n" +
            "FROM rental AS r WHERE r.reservation_code = ?;";

    private static final String QUERY_RENTAL_PRICE = "SELECT " +
            "HOUR(TIMEDIFF(?, ?)) AS hours_diff, " +
            "CEIL(HOUR(TIMEDIFF(?, ?)) / 24) AS days, " +
            "price, (price *  CEIL(HOUR(TIMEDIFF(?, ?)) / 24) ) AS total " +
            "FROM rental_price WHERE vehicle_id = ? AND status = 1;";

    private static final String QUERY_DRIVER_COST = "SELECT " +
            "CEIL(HOUR(TIMEDIFF(?, ?)) / 24) AS days, " +
            "salary, " +
            "(salary * ?) AS earning, " +
            "(salary * (1 + ?) *  CEIL(HOUR(TIMEDIFF(?, ?)) / 24) ) AS total, " +
            "(salary * CEIL(HOUR(TIMEDIFF(?, ?)) / 24) ) AS cost " +
            "FROM job WHERE id=? AND status=1;";

    private static final String QUERY_RENTALS = "SELECT\n"
            + "	r.id,\n"
            + "	r.first_name,\n"
            + "	r.last_name,\n"
            + "	r.departure_date,\n"
            + "	r.departure_time,\n"
            + "	r.return_date,\n"
            + "	r.return_time,\n"
            + "	r.reservation_code,\n"
            + "	r.is_quotation,\n"
            + "	r.rent_status,\n"
            + "	r.payment_status,\n"
            + "	r.created_at,\n"
            + "	pc.name AS pickup_city_name,\n"
            + "	ps.name AS pickup_state_name,\n"
            + "	lc.name AS leave_city_name,\n"
            + "	ls.name AS leave_state_name,\n"
            + "	r.destiny_city_name,\n"
            + "	v.name AS vehicle_name,\n"
            + "	v.brand AS vehicle_brand,\n"
            + "	v.vehicle_year,\n"
            + "	v.model AS vehicle_model,\n"
            + "	v.img_file AS vehicle_img_file\n"
            + "FROM\n"
            + "	rental r\n"
            + "LEFT JOIN branchoffice bop ON\n"
            + "	r.pickup_branchoffice_id = bop.id\n"
            + "LEFT JOIN branchoffice bor ON\n"
            + "	r.leave_branchoffice_id = bor.id\n"
            + "LEFT JOIN city pc ON\n"
            + "	r.pickup_city_id = pc.id\n"
            + "LEFT JOIN county pcu ON\n"
            + "	pc.county_id = pcu.id\n"
            + "LEFT JOIN state ps ON\n"
            + "	pcu.state_id = ps.id\n"
            + "LEFT JOIN city lc ON\n"
            + "	r.leave_city_id = lc.id\n"
            + "LEFT JOIN county lcu ON\n"
            + "	lc.county_id = lcu.id\n"
            + "LEFT JOIN state ls ON\n"
            + "	lcu.state_id = ls.id\n"
            + "LEFT JOIN vehicle v ON\n"
            + "	r.vehicle_id = v.id ORDER BY r.id DESC";

    private static final String QUERY_RESERVATION_DETAIL_GENERAL = "SELECT\n" +
            "             r.*,\n" +
            "             CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "             cb.name as customer_billing_name,\n" +
            "             cb.rfc as customer_billing_rfc,\n" +
            "             cb.legal_person as customer_billing_legal_person,\n" +
            "              rq.reservation_code AS quotation_code,\n" +
            "             bop.name AS pickup_branchoffice_name,\n" +
            "             bop.prefix AS pickup_branchoffice_prefix,\n" +
            "             bop.address AS pickup_branchoffice_address,\n" +
            "             bop.latitude AS pickup_branchoffice_latitude,\n" +
            "             bop.longitude AS pickup_branchoffice_longitude,\t\n" +
            "             bor.name AS leave_branchoffice_name,\n" +
            "             bor.prefix AS leave_branchoffice_prefix,\n" +
            "             bor.address AS leave_branchoffice_address,\n" +
            "             bor.latitude AS leave_branchoffice_latitude,\n" +
            "             bor.longitude AS leave_branchoffice_longitude,\t\n" +
            "             pc.name AS pickup_city_name,\n" +
            "             ps.name AS pickup_state_name,\n" +
            "             lc.name AS leave_city_name,\n" +
            "             ls.name AS leave_state_name,\n" +
            "             r.destiny_city_name AS destiny_city_name,\n" +
            "             ds.name AS destiny_state_name,\n" +
            "             v.name AS vehicle_name,\n" +
            "             v.description AS vehicle_description,\n" +
            "             v.brand AS vehicle_brand,\n" +
            "             v.vehicle_year,\n" +
            "             v.serial_num AS vehicle_serial_num,\n" +
            "             v.model AS vehicle_model,\n" +
            "             v.plate AS vehicle_plate,\n" +
            "             v.plate_state AS vehicle_plate_state,\n" +
            "             v.sct_license AS vehicle_sct_license,\n" +
            "             v.img_file AS vehicle_img_file,\n" +
            "             cv.seatings AS vehicle_seatings,\n" +
            "             rp.price AS rental_price,\n" +
            "             rp.type_price AS rental_price_type_price,\n" +
            "              iv.document_id,\n" +
            "              iv.media_document_pdf_name,\n" +
            "              iv.media_document_xml_name\n" +
            "             FROM\n" +
            "             rental r\n" +
            "             LEFT JOIN customer c ON\n" +
            "             r.customer_id = c.id\n" +
            "             LEFT JOIN invoice AS iv ON iv.id = r.invoice_id \n" +
            "             LEFT JOIN customer_billing_information cb ON\n" +
            "             cb.customer_id = c.id\n" +
            "             LEFT JOIN branchoffice bop ON\n" +
            "             r.pickup_branchoffice_id = bop.id\n" +
            "             LEFT JOIN branchoffice bor ON\n" +
            "             r.leave_branchoffice_id = bor.id\n" +
            "             LEFT JOIN city pc ON\n" +
            "             r.pickup_city_id = pc.id\n" +
            "             LEFT JOIN county pcu ON\n" +
            "             pc.county_id = pcu.id\n" +
            "             LEFT JOIN state ps ON\n" +
            "             pcu.state_id = ps.id\n" +
            "             LEFT JOIN city lc ON\n" +
            "             r.leave_city_id = lc.id\n" +
            "             LEFT JOIN county lcu ON\n" +
            "             lc.county_id = lcu.id\n" +
            "             LEFT JOIN state ls ON\n" +
            "             lcu.state_id = ls.id\n" +
            "             LEFT JOIN city dc ON\n" +
            "             r.destiny_city_id = dc.id\n" +
            "             LEFT JOIN county dcu ON\n" +
            "             dc.county_id = dcu.id\n" +
            "             LEFT JOIN state ds ON\n" +
            "             dcu.state_id = ds.id\n" +
            "             LEFT JOIN vehicle v ON\n" +
            "             r.vehicle_id = v.id\n" +
            "             LEFT JOIN config_vehicle cv ON\n" +
            "             v.config_vehicle_id = cv.id\n" +
            "             LEFT JOIN rental_price rp ON\n" +
            "             r.rental_price_id = rp.id\n" +
            "             LEFT JOIN rental rq ON\n" +
            "             r.quotation_id = rq.id\n" +
            "             WHERE\n" +
            "             r.reservation_code = ? GROUP BY r.id;";

    private static final String QUERY_RESERVATION_DETAIL_QUOTATION = "SELECT\n"
            + "	r.id,\n"
            + "	r.reservation_code,\n"
            + "	r.first_name,\n"
            + "	r.last_name,\n"
            + "	r.email,\n"
            + "	r.rental_minamount_percent,\n"
            + "	r.quotation_expired_after,\n"
            + "	r.total_passengers,\n"
            + "	r.created_at,	\n"
            + "	r.departure_date,\n"
            + "	r.departure_time,\n"
            + "	r.return_date,\n"
            + "	r.return_time,\n"
            + "	r.pickup_at_office,\n"
            + "	r.branchoffice_id,\n"
            + "	r.cancel_reason,\n"
            + "	b.name AS branchoffice_name,\n"
            + "	bc.name AS branchoffice_city,\n"
            + "	bs.name AS branchoffice_state,\n"
            + "	r.vehicle_id,\n"
            + "	v.name AS vehicle_name,\n"
            + "	v.economic_number AS vehicle_number,\n"
            + "	cv.seatings,\n"
            + "	r.has_driver,\n"
            + "	r.driver_cost,\n"
            + "	r.checklist_charges,\n"
            + "	r.extra_charges,\n"
            + "	r.amount,\n"
            + "	r.discount,\n"
            + "	r.total_amount,\n"
            + "	r.guarantee_deposit,\n"
            + "	r.rent_status,\n"
            + "	r.status,\n"
            + "	r.pickup_city_id,\n"
            + "	pc.name AS pickup_city_name,\n"
            + "	ps.name AS pickup_state_name,\n"
            + "	r.leave_city_id,\n"
            + "	lc.name AS leave_city_name,\n"
            + "	ls.name AS leave_state_name,\n"
            + "	r.destiny_city_id,\n"
            + "	r.destiny_city_name,\n"
            + "	ds.name AS destiny_state_name\n"
            + "FROM\n"
            + "	rental r\n"
            + "LEFT JOIN vehicle v ON\n"
            + "	v.id = r.vehicle_id\n"
            + "LEFT JOIN config_vehicle cv ON\n"
            + "	cv.id = v.config_vehicle_id\n"
            + "LEFT JOIN branchoffice b ON\n"
            + "	b.id = r.branchoffice_id\n"
            + "LEFT JOIN city bc ON\n"
            + "	bc.id = b.city_id\n"
            + "LEFT JOIN state bs ON\n"
            + "	bs.id = b.state_id\n"
            + "LEFT JOIN city pc ON\n"
            + "	r.pickup_city_id = pc.id\n"
            + "LEFT JOIN county pcu ON\n"
            + "	pc.county_id = pcu.id\n"
            + "LEFT JOIN state ps ON\n"
            + "	pcu.state_id = ps.id\n"
            + "LEFT JOIN city lc ON\n"
            + "	r.leave_city_id = lc.id\n"
            + "LEFT JOIN county lcu ON\n"
            + "	lc.county_id = lcu.id\n"
            + "LEFT JOIN state ls ON\n"
            + "	lcu.state_id = ls.id\n"
            + "LEFT JOIN city dc ON\n"
            + "	r.destiny_city_id = dc.id\n"
            + "LEFT JOIN county dcu ON\n"
            + "	dc.county_id = dcu.id\n"
            + "LEFT JOIN state ds ON\n"
            + "	dcu.state_id = ds.id\n"
            + "WHERE\n"
            + "	r.reservation_code = ?";

    private static final String QUERY_RESERVATION_DETAIL_QUOTATION_TO_RENT = "SELECT\n"
            + "	id,\n"
            + "	init_route,\n"
            + "	init_full_address,\n"
            + "	farthest_route,\n"
            + "	farthest_full_address,\n"
            + "	total_passengers,\n"
            + "	vehicle_id,\n"
            + "	has_driver,\n"
            + "	amount,\n"
            + "	extra_charges,\n"
            + "	discount,\n"
            + "	driver_cost,\n"
            + "	total_amount,\n"
            + "	departure_date,\n"
            + "	departure_time,\n"
            + "	return_date,\n"
            + "	return_time,\n"
            + "	pickup_at_office,\n"
            + "	leave_at_office,\n"
            + "	destiny_city_name,\n"
            + "	pickup_branchoffice_id,\n"
            + "	leave_branchoffice_id,\n"
            + "	rental_price_id,\n"
            + "	guarantee_deposit,\n"
            + "	customer_id,\n"
            + "	first_name,\n"
            + "	last_name,\n"
            + "	email,\n"
            + "	phone,\n"
            + "	address,\n"
            + "	credential_type,\n"
            + "	no_credential,\n"
            + "	file_credential\n"
            + "FROM\n"
            + "	rental\n"
            + "WHERE\n"
            + "	reservation_code = ?;";

    private static final String QUERY_VEHICLE_CHARACTERISTICS = " SELECT \n"
            + " c.name AS characteristic_name, \n"
            + " vc.status, \n"
            + " vc.id \n"
            + " FROM vehicle_characteristic vc \n"
            + " JOIN characteristic c ON \n"
            + " c.id = vc.characteristic_id \n"
            + "WHERE vc.vehicle_id = ? AND vc.status=1\n";

    private static final String QUERY_CONCAT_VEHICLE_CHARACTERISTICS = "SELECT\n" +
            " GROUP_CONCAT(' ', c.name) AS vehicle_characteristics\n" +
            " FROM vehicle_characteristic vc\n" +
            "    JOIN characteristic c ON\n" +
            "    c.id = vc.characteristic_id\n" +
            "    WHERE vc.vehicle_id = ? AND vc.status=1";

    private static final String QUERY_RESERVATION_DETAIL_RENTAL_CONFIG_VEHICLE
            = "SELECT\n"
            + "	rcv.*,\n"
            + "	av.name AS addon_vehicle_name,\n"
            + "	av.lost_price AS addon_vehicle_lost_price,\n"
            + " av.removable AS addon_vehicle_removable\n"
            + "FROM\n"
            + "	rental_config_vehicle rcv\n"
            + "JOIN addon_vehicle av ON\n"
            + "	rcv.addon_vehicle_id = av.id\n"
            + "WHERE\n"
            + "	rental_id = ?;";

    private static final String QUERY_RESERVATION_DETAIL_RENTAL_CHECKLIST
            = "SELECT\n"
            + "	rc.*,\n"
            + "	cv.name AS checklist_name,\n"
            + "	cv.category AS checklist_category,\n"
            + "	cv.is_default AS checklist_is_default,\n"
            + "	cv.default_value AS checklist_default_value,\n"
            + "	cv.use_price AS checklist_use_price,\n"
            + "	cv.damage_price AS checklist_damage_price\n"
            + "FROM\n"
            + "	rental_checklist rc\n"
            + "JOIN checklist_vans cv ON cv.id = rc.checklist_van_id\n"
            + "WHERE\n"
            + " rc.rental_id = ?;";

    private static final String QUERY_RESERVATION_DETAIL_RENTAL_DRIVER
            = "SELECT\n"
            + "	*\n"
            + "FROM\n"
            + "	rental_driver\n"
            + "WHERE\n"
            + "	rental_id = ?;";

    private static final String QUERY_RESERVATION_DETAIL_RENTAL_EXTRA_CHARGUE
            = "SELECT\n"
            + "	rxc.*,\n"
            + "	ec.name\n"
            + "FROM\n"
            + "	rental_extra_charge rxc\n"
            + "JOIN extra_charge ec ON\n"
            + "	rxc.extra_charge_id = ec.id\n"
            + "WHERE\n"
            + "	rental_id = ?;";

    private static final String QUERY_RESERVATION_DETAIL_RENTAL_EXTRA_CHARGUE_TO_RENT
            = "SELECT\n"
            + " extra_charge_id,\n"
            + " reference,\n"
            + " amount\n"
            + "FROM rental_extra_charge\n"
            + "WHERE rental_id = ?;";

    private static final String QUERY_RESERVATION_DETAIL_RENTAL_EVIDENCE
            = "SELECT\n"
            + "	*\n"
            + "FROM\n"
            + "	rental_evidence\n"
            + "WHERE\n"
            + "	rental_id = ? AND status != 3;";

    private static final String QUERY_RESERVATION_DETAIL_PAYMENT_AMOUNT = "SELECT r.id, COUNT( p.id ) AS payment_count,\n"
            + "	SUM(p.amount) AS payment_amount,\n"
            + "	COALESCE(r.driver_cost,0) AS driver_cost,\n"
            + "	(COALESCE(r.extra_charges,0) + COALESCE(r.checklist_charges,0)) AS extra_charges,\n"
            + "	COALESCE(r.guarantee_deposit,0) AS guarantee_deposit,\n"
            + "	COALESCE(r.total_amount,0) AS rental_amount,\n"
            + "	(COALESCE((r.total_amount + r.guarantee_deposit), 0) - SUM(p.amount)) AS missing_to_pay\n"
            + "FROM\n"
            + "	payment p\n"
            + "JOIN rental r ON\n"
            + "	p.rental_id = r.id\n"
            + "WHERE\n"
            + " p.status != 3\n"
            + "	AND r.id = ? GROUP BY r.id;";

    private static final String QUERY_EXPIRE_RENTALS_CANCEL = "UPDATE \n"
            + "	rental r\n"
            + "JOIN (\n"
            + "	SELECT\n"
            + "		t.rental_id,\n"
            + "		SUM(t.payment) AS payment,\n"
            + "		SUM(t.deposit) AS deposit\n"
            + "	FROM\n"
            + "		(\n"
            + "		SELECT\n"
            + "			p.rental_id,\n"
            + "			CASE\n"
            + "				WHEN p.is_deposit = FALSE\n"
            + "				AND p.is_extra_charge = FALSE THEN p.amount\n"
            + "				ELSE 0\n"
            + "			END AS payment,\n"
            + "			CASE\n"
            + "				WHEN p.is_deposit = TRUE\n"
            + "				AND p.is_extra_charge = FALSE THEN p.amount\n"
            + "				ELSE 0\n"
            + "			END AS deposit\n"
            + "		FROM\n"
            + "			payment p\n"
            + "		WHERE\n"
            + "			p.status != 3\n"
            + "			AND p.rental_id IS NOT NULL) t\n"
            + "	GROUP BY\n"
            + "		t.rental_id) balance ON\n"
            + "	balance.rental_id = r.id\n"
            + "SET r.rent_status = 0\n"
            + "WHERE\n"
            + "	rent_status = 1\n"
            + "	AND departure_date < CURDATE()\n"
            + "	AND r.total_amount > balance.payment\n"
            + "	AND r.guarantee_deposit > balance.deposit";

    private static final String QUERY_EXPIRE_RENTALS_NOT_PRESENTED = "update\n"
            + "	rental r\n"
            + "JOIN (\n"
            + "	SELECT\n"
            + "		t.rental_id,\n"
            + "		SUM(t.payment) AS payment,\n"
            + "		SUM(t.deposit) AS deposit\n"
            + "	FROM\n"
            + "		(\n"
            + "		SELECT\n"
            + "			p.rental_id,\n"
            + "			CASE\n"
            + "				WHEN p.is_deposit = FALSE\n"
            + "				AND p.is_extra_charge = FALSE THEN p.amount\n"
            + "				ELSE 0\n"
            + "			END AS payment,\n"
            + "			CASE\n"
            + "				WHEN p.is_deposit = TRUE\n"
            + "				AND p.is_extra_charge = FALSE THEN p.amount\n"
            + "				ELSE 0\n"
            + "			END AS deposit\n"
            + "		FROM\n"
            + "			payment p\n"
            + "		WHERE\n"
            + "			p.status != 3\n"
            + "			AND p.rental_id IS NOT NULL) t\n"
            + "	GROUP BY\n"
            + "		t.rental_id) balance ON\n"
            + "	balance.rental_id = r.id\n"
            + "SET r.rent_status = 4\n"
            + "WHERE\n"
            + "	rent_status = 1\n"
            + "	AND departure_date < CURDATE()\n"
            + "	AND r.total_amount = balance.payment\n"
            + "	AND r.guarantee_deposit = balance.deposit";

    private static final String QUERY_EXPIRE_QUOTATIONS = "UPDATE\n"
            + "	rental\n"
            + "SET\n"
            + "	rent_status = 0\n"
            + " WHERE\n"
            + "	rent_status = 5\n"
            + "	AND created_at < (\n"
            + "	SELECT\n"
            + "		DATE_ADD( CURDATE(),\n"
            + "		INTERVAL (\n"
            + "		SELECT\n"
            + "			(\n"
            + "			SELECT\n"
            + "				COALESCE( MAX(value),\n"
            + "				16 ) AS value\n"
            + "			FROM\n"
            + "				general_setting\n"
            + "			WHERE\n"
            + "				FIELD = 'quotation_expired_after'\n"
            + "				AND status = 1 ) *- 1 ) DAY ))";

    private static final String QUERY_CASH_OUT_EMPLOYEE = "SELECT\n"
            + "	co.id\n"
            + "FROM\n"
            + "	cash_out co\n"
            + "JOIN employee e ON e.id = co.employee_id\n"
            + "WHERE\n"
            + "	e.user_id = ? \n"
            + "	AND co.cash_out_status = 1;";

    private static final String QUERY_PAYMENT_BALANCE = "SELECT r.id, \n"
            + "	SUM(p.amount) AS total_paid,\n"
            + "	r.total_amount, r.guarantee_deposit,\n"
            + "	( r.total_amount - SUM(p.amount)) AS missing_to_pay\n"
            + "FROM\n"
            + "	payment p\n"
            + "JOIN rental r ON\n"
            + "	p.rental_id = r.id\n"
            + "WHERE\n"
            + "	p.is_deposit = FALSE\n"
            + "	AND p.is_extra_charge = FALSE\n"
            + "	AND p.status != 3\n"
            + "	AND r.reservation_code = ? GROUP BY r.id;";

    private static final String QUERY_PAYMENT_BALANCE_WITH_ID = "SELECT \n"
            + "	SUM(p.amount) AS total_paid,\n"
            + "	r.total_amount, r.guarantee_deposit, \n"
            + "	( r.total_amount - SUM(p.amount)) AS missing_to_pay,\n"
            + "	r.id AS rental_id\n"
            + "FROM\n"
            + "	payment p\n"
            + "JOIN rental r ON\n"
            + "	p.rental_id = r.id\n"
            + "WHERE\n"
            + "	p.is_deposit = FALSE\n"
            + "	AND p.is_extra_charge = FALSE\n"
            + "	AND p.status != 3\n"
            + "	AND r.reservation_code = ? GROUP BY r.id;";

    private static final String QUERY_DEPOSIT_BALANCE_TOTAL = "SELECT r.id, \n"
            + "COALESCE(SUM(p.amount), 0) AS total_deposited,\n"
            + "  r.guarantee_deposit,\n"
            + "  ( r.guarantee_deposit - COALESCE(SUM(p.amount), 0)) AS missing_to_deposit\n"
            + "FROM\n"
            + "  payment p\n"
            + "RIGHT JOIN rental r ON\n"
            + "  p.rental_id = r.id\n"
            + "WHERE\n"
            + "  p.is_deposit = TRUE\n"
            + "  AND p.status != 3\n"
            + "  AND r.reservation_code = ? GROUP BY r.id;";

    private static final String QUERY_DEPOSIT = "SELECT r.id, \n"
            + " COALESCE(SUM(p.amount), 0) AS total_deposited, \n"
            + " p.payment_method_id, \n"
            + "  r.guarantee_deposit,\n"
            + "  ( r.guarantee_deposit - COALESCE(SUM(p.amount), 0)) AS missing_to_deposit\n"
            + "FROM\n"
            + "  payment p\n"
            + "RIGHT JOIN rental r ON\n"
            + "  p.rental_id = r.id\n"
            + "WHERE\n"
            + "  p.is_deposit = TRUE\n"
            + "  AND p.status != 3\n"
            + "  AND r.reservation_code = ? GROUP BY r.id, p.payment_method_id;";

    private static final String QUERY_PAYMENT_DETAILS = "SELECT\n"
            + "	p.id,\n"
            + "	p.rental_id,\n"
            + "	p.payment_method_id,\n"
            + "	pm.name AS payment_method_name,\n"
            + "	p.amount,\n"
            + "	p.reference,\n"
            + "	p.exchange_rate_id,\n"
            + "	p.currency_id,\n"
            + "	p.is_deposit,\n"
            + "	p.is_extra_charge,\n"
            + "	p.status,\n"
            + "	p.created_at\n"
            + "FROM\n"
            + "	payment p\n"
            + "JOIN rental r ON\n"
            + "	p.rental_id = r.id\n"
            + "JOIN payment_method pm ON\n"
            + "	pm.id = p.payment_method_id\n"
            + "WHERE\n"
            + "	p.status != 3\n"
            + "	AND r.reservation_code = ?;";

    private static final String QUERY_EXTRA_BALANCE = "SELECT\n"
            + "  SUM(p.amount) AS total_extra_charge\n"
            + "FROM\n"
            + "  payment p\n"
            + "JOIN rental r ON\n"
            + "  p.rental_id = r.id\n"
            + "WHERE\n"
            + "  p.is_extra_charge = TRUE\n"
            + "  AND p.status != 3\n"
            + "  AND r.reservation_code = ?;";

    private static final String QUERY_SET_RENTAL_PAID = "UPDATE\n"
            + "	rental\n"
            + "SET\n"
            + "	payment_status = '1'\n"
            + "WHERE\n"
            + "	id = ?;";

    private static final String QUERY_SET_RENTAL_PARTIAL_PAID = "UPDATE\n"
            + "	rental\n"
            + "SET\n"
            + "	payment_status = '2'\n"
            + "WHERE\n"
            + "	id = ?;";

    private static final String QUERY_RENTAL_CAN_DELIVER = "SELECT\n"
            + "	payment_status, \n"
            + "	rent_status, \n"
            + "	status, \n"
            + "	departure_date,\n"
            + "	departure_time\n"
            + "FROM\n"
            + "	rental\n"
            + "WHERE\n"
            + "	reservation_code = ?;";

    private static final String QUERY_RENTAL_CANCEL_DATA = "SELECT\n"
            + " r.id,\n"
            + "	r.departure_date,\n"
            + "	r.departure_time,\n"
            + "	r.status,\n"
            + "	r.rent_status,\n"
            + "	r.total_amount,\n"
            + "	r.guarantee_deposit,\n"
            + "	SUM(t.total_paid) AS total_paid,\n"
            + "	SUM(t.total_deposited) AS total_deposited\n"
            + "FROM\n"
            + "	rental r,\n"
            + "	(\n"
            + "	SELECT\n"
            + "		CASE\n"
            + "			WHEN p.is_deposit = FALSE THEN p.amount\n"
            + "			ELSE 0\n"
            + "		END AS total_paid,\n"
            + "		CASE\n"
            + "			WHEN p.is_deposit = TRUE THEN p.amount\n"
            + "			ELSE 0\n"
            + "		END AS total_deposited\n"
            + "	FROM\n"
            + "		payment p\n"
            + "	JOIN rental r ON\n"
            + "		p.rental_id = r.id\n"
            + "	WHERE\n"
            + "		r.reservation_code = ?\n"
            + "		AND p.status != 3 ) t\n"
            + "WHERE\n"
            + "	r.reservation_code = ? GROUP BY r.id;";

    private static final String QUERY_CANCEL_RENTAL = "UPDATE\n"
            + "	rental\n"
            + "SET\n"
            + "	rent_status = 0,\n"
            + "	updated_at = NOW(),\n"
            + " cancel_reason = ?,\n"
            + "	updated_by = ?"
            + " WHERE\n"
            + "	reservation_code = ?;";

    private static final String QUERY_EXPENSE_CONCEPT_CANCEL = "SELECT\n"
            + "	id\n"
            + "FROM\n"
            + "	expense_concept\n"
            + "WHERE\n"
            + "	name = 'Cancelación renta'";

    private static final String QUERY_GET_UNAVAILABLE_RENTALS_BY_RANGE_DATES = "SELECT \n" +
            " r.id, \n" +
            " r.first_name, \n" +
            " r.last_name, \n" +
            " r.departure_date, \n" +
            " r.departure_time, \n" +
            " r.return_date, \n" +
            " r.return_time, \n" +
            " DATEDIFF(return_date, departure_date) AS travel_days, \n" +
            " TIMESTAMPDIFF(HOUR, concat(departure_date, ' ', departure_time), concat(return_date, ' ', return_time)) AS travel_hours, \n" +
            " r.reservation_code, \n" +
            " r.rent_status, \n" +
            " v.id AS vehicle_id, \n" +
            " v.name AS vehicle_name, \n" +
            " v.alias AS vehicle_alias, \n" +
            " v.brand AS vehicle_brand, \n" +
            " v.economic_number AS vehicle_economic_number, \n" +
            " v.description AS vehicle_description, \n" +
            " pc.name AS pickup_city_name, \n" +
            " ps.name AS pickup_state_name, \n" +
            " lc.name AS leave_city_name, \n" +
            " ls.name AS leave_state_name, \n" +
            " r.destiny_city_name, \n" +
            " cv.seatings \n" +
            " FROM rental r \n" +
            " LEFT JOIN branchoffice bop ON r.pickup_branchoffice_id = bop.id \n" +
            " LEFT JOIN branchoffice bor ON r.leave_branchoffice_id = bor.id \n" +
            " LEFT JOIN city pc ON r.pickup_city_id = pc.id \n" +
            " LEFT JOIN county pcu ON pc.county_id = pcu.id \n" +
            " LEFT JOIN state ps ON pcu.state_id = ps.id \n" +
            " LEFT JOIN city lc ON r.leave_city_id = lc.id \n" +
            " LEFT JOIN county lcu ON lc.county_id = lcu.id \n" +
            " LEFT JOIN state ls ON lcu.state_id = ls.id \n" +
            " LEFT JOIN vehicle v ON r.vehicle_id = v.id\n" +
            " LEFT JOIN config_vehicle cv ON v.config_vehicle_id = cv.id\n" +
            " WHERE r.is_quotation = 0\n" +
            " AND (r.departure_date BETWEEN ? AND ? OR \n" +
            " r.return_date BETWEEN ? AND ?) \n" +
            " AND (r.rent_status = 1 OR r.rent_status = 2) \n";

    private static final String QUERY_GET_AVAILABLE_RENTALS_BY_RANGE_DATES = "SELECT \n" +
            " v.id, \n" +
            " v.name, \n" +
            " v.alias, \n" +
            " v.brand, \n" +
            " v.economic_number, \n" +
            " v.description, \n" +
            " cv.seatings, \n" +
            " cv.id AS config_vehicle_id \n" +
            " FROM vehicle v \n" +
            " LEFT JOIN config_vehicle cv ON v.config_vehicle_id = cv.id \n" +
            " LEFT JOIN rental_price rp ON v.id = rp.vehicle_id \n" +
            " WHERE rp.status = 1 \n" +
            " AND v.work_type = '0' \n" +
            " AND v.status = 1 \n" +
            " AND v.id NOT IN( \n" +
            " SELECT distinct r.vehicle_id \n" +
            " FROM rental r \n" +
            " WHERE r.is_quotation = 0 \n" +
            " AND (r.rent_status = 1 OR r.rent_status = 2) \n" +
            " AND (r.departure_date BETWEEN ? AND ? OR \n" +
            " r.return_date BETWEEN ? AND ?)) \n";

    private static final String QUERY_EXPENSE_CONCEPT_RETURN = "SELECT\n"
            + "	id\n"
            + "FROM\n"
            + "	expense_concept\n"
            + "WHERE\n"
            + "	name = 'Devolución por liquidación de renta de van'";

    private static String QUERY_GET_DATES_RESERVATION = "SELECT " +
            "CONCAT(departure_date, ' ', departure_time) AS departure_date, " +
            "CONCAT(return_date, ' ', return_time) AS return_date, " +
            "vehicle_id " +
            "FROM rental " +
            "WHERE is_quotation = 1 " +
            "AND status = 1 " +
            "AND reservation_code = ?;";

    private static String QUERY_SALES_REPORT =
            "FROM rental AS r  \n" +
            "LEFT JOIN branchoffice AS b ON (b.id = r.branchoffice_id) \n" +
            "LEFT JOIN city AS c ON (c.id = b.city_id) \n" +
            "LEFT JOIN branchoffice AS ob ON (ob.id = r.pickup_branchoffice_id) \n" +
            "LEFT JOIN city AS oc ON (oc.id = r.pickup_city_id) \n" +
            "LEFT JOIN customer AS cc ON cc.id = r.customer_id \n" +
            "WHERE r.is_quotation = 0 and rent_status != 0 ";
    private static String QUERY_SALES_REPORT_FIELDS = "SELECT r.id, r.vehicle_id, r.reservation_code, \n" +
            "r.branchoffice_id, b.name AS branchoffice_name, b.prefix AS branchoffice_prefix, b.city_id AS branchoffice_city_id, c.name AS branchoffice_city,  \n" +
            "r.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, CONCAT(r.first_name, ' ', r.last_name) AS responsable_name,  \n" +
            "r.pickup_branchoffice_id, ob.prefix AS pickup_branchoffice_prefix, r.pickup_city_id, oc.name AS pickup_city, r.destiny_city_name, \n" +
            "r.created_at, r.departure_date, r.departure_time, r.return_date, r.return_time, r.has_driver, \n" +
            "r.payment_condition, r.payment_status, r.driver_cost, r.rental_price, r.guarantee_deposit,  \n" +
            "r.amount, r.discount, r.checklist_charges, r.extra_charges, r.penalty_days, r.penalty_amount, r.total_amount, r.cost, r.profit " ;
    private static String QUERY_SALES_REPORT_COUNT = "SELECT COUNT(r.id) AS items \n";

    private static String REPORT_ORDER_BY = " ORDER BY branchoffice_id AND branchoffice_city_id ";

    private static final String QUERY_VANS_LIST = "SELECT r.id, r.first_name, r.last_name, r.departure_date, r.departure_time, r.return_date, r.return_time, \n" +
            "r.reservation_code, r.is_quotation, r.rent_status, r.payment_status, r.created_at,\n" +
            "cp.name as pickup_city_name, sp.name as pickup_state_name,\n" +
            "cl.name as leave_city_name, sl.name as leave_state_name,\n" +
            "r.destiny_city_name as destiny_city_name, v.config_vehicle_id as config_vehicle_id, v.id as vehicle_id, v.name as vehicle_name, v.brand as vehicle_brand,\n" +
            "v.vehicle_year, v.model as vehicle_model, v.img_file as vehicle_img_file\n" +
            "FROM rental r\n" +
            "LEFT JOIN city cp ON cp.id = r.pickup_city_id\n" +
            "LEFT JOIN county ct ON ct.id = cp.county_id\n" +
            "LEFT JOIN state sp ON sp.id = ct.state_id\n" +
            "LEFT JOIN city cl ON cl.id = r.leave_city_id\n" +
            "LEFT JOIN county ctl ON ctl.id = cl.county_id\n" +
            "LEFT JOIN state sl ON sl.id = ctl.state_id\n" +
            "LEFT JOIN city cd ON cd.id = r.destiny_city_id\n" +
            "LEFT JOIN vehicle v ON v.id = r.vehicle_id\n" +
            "WHERE (r.departure_date BETWEEN ? AND ? OR r.return_date BETWEEN ? AND ?)  \n";
    //</editor-fold>
    private static final String INSERT_CASH_OUT_MOVE = "INSERT INTO cash_out_move (cash_out_id, payment_id, quantity, move_type, created_by) " +
            "VALUES (?, ?, ?, '0', ?);";
    private static final String INSERT_TICKET_PAYMENT = "INSERT INTO tickets (cash_out_id, rental_id, iva, total, paid, paid_change, action, created_by, ticket_code ) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_TICKET_DETAIL = "insert into tickets_details(amount,quantity,detail,ticket_id,unit_price,created_by) values (?,?,?,?,?,?)";
}
