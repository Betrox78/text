/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.money;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.rental.RentalDBV;
import static database.rental.RentalDBV.ACTION_QUOTATION;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static service.commons.Constants.ACTION;

import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

/**
 *
 * @author daliacarlon
 */
public class TicketDBV extends DBVerticle {

    public static final String PRINT_TICKET = "TicketDBV.printTicket";
    public static final String PRINT_PREPAID = "TicketDBV.printPrepaidTicket";


    @Override
    public String getTableName() {
        return "tickets";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case PRINT_TICKET:
                this.printTicket(message);
                break;
            case PRINT_PREPAID:
                this.printPrepaidTicket(message);
                break;
        }
    }

    public enum ACTION {
        PURCHASE,
        INCOME,
        CHANGE,
        CANCEL,
        EXPENSE,
        WITHDRAWAL,
        RETURN,
    }

    private void printPrepaidTicket(Message<JsonObject> message) {
        try {
            String query = QUERY_PREPAID_TICKET;
            JsonArray params = new JsonArray().add(message.body().getValue("id"));

            if(message.body().containsKey("reservation_code")) {
                String code = message.body().getString("reservation_code");
                params.add(code);
                query=query.concat(" AND  bp.reservation_code = ?");
            }
            this.dbClient.queryWithParams(query, params, reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch(Exception e) {
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            reportQueryError(message, e.getCause());
        }
    }

    private void printTicket(Message<JsonObject> message) {
        startTransaction(message, conn -> {
            try {
                final int ticketId = Integer.valueOf(message.body().getString("ticketId"));
                final String header = message.body().getString("header");
                final String footer = message.body().getString("footer");
                final String rentalReservationCode = message.body().getString("rentalReservationCode");
                final int updatedBy = message.body().getInteger("updated_by");
                final JsonObject debt = message.body().getJsonObject("debt");

                JsonArray params = new JsonArray().add(ticketId);
                conn.queryWithParams(QUERY_TICKET, params, queryReply -> {
                    try {
                        if (queryReply.failed()){
                            throw queryReply.cause();
                        }
                        //get general ticket
                        List<JsonObject> res = queryReply.result().getRows();

                        JsonObject ticket = res.get(0);
                        Integer contPrinted = ticket.getInteger("prints_counter");
                        String ticketCreatedAt = ticket.getString("created_at");


                        String headerStr = header;
                        if(contPrinted > 0){
                            headerStr = "<h4 style='text-align:center;'><strong>REIMPRESIÓN #"+contPrinted+"</strong></h4> " + header;
                        }
                        contPrinted++;
                        final String hdr = headerStr;

                        // get customer and reservation_code
                        String customer = "Público en general";
                        String reservationCode = "";
                        Integer customerId = null;
                        String paymentCondition = "";
                        if (ticket.getString("boarding_pass_customer") != null) {
                            customer = ticket.getString("boarding_pass_customer");
                            paymentCondition = ticket.getString("boardingpass_payment_condition");
                        } else if (ticket.getString("rental_customer") != null) {
                            customer = ticket.getString("rental_customer");
                        } else if (ticket.getString("parcel_customer") != null) {
                            customer = ticket.getString("parcel_customer");
                            paymentCondition = ticket.getString("parcel_payment_condition");
                        } else if (ticket.getString("prepaid_customer") != null) {
                            customer = ticket.getString("prepaid_customer");
                        } else if (ticket.getString("prepaid_travel_customer") != null) {
                            customer = ticket.getString("prepaid_travel_customer");
                            paymentCondition = ticket.getString("prepaid_travel_payment_condition");
                        }
                        ticket.remove("boardingpass_payment_condition");
                        ticket.remove("parcel_payment_condition");
                        ticket.remove("prepaid_travel_payment_condition");
                        ticket.remove("boarding_pass_customer");
                        ticket.remove("rental_customer");
                        ticket.remove("parcel_customer");
                        ticket.remove("prepaid_customer");
                        ticket.remove("prepaid_travel_customer");

                        if(debt.size() > 0) {
                            customer = (String) debt.remove("customer");
                            ticket.put("debt", debt);
                        }

                        ticket.put("customer", customer)
                                .put("payment_condition", paymentCondition);
                        if (ticket.getString("boarding_pass_reservation_code") != null) {
                            reservationCode = ticket.getString("boarding_pass_reservation_code");
                        } else if (ticket.getString("rental_reservation_code") != null) {
                            reservationCode = ticket.getString("rental_reservation_code");
                        } else if (ticket.getString("parcel_reservation_code") != null) {
                            reservationCode = ticket.getString("parcel_reservation_code");
                        }else if (ticket.getString("prepaid_reservation_code") != null) {
                            reservationCode = ticket.getString("prepaid_reservation_code");
                        }else if (ticket.getString("prepaid_travel_reservation_code") != null) {
                            reservationCode = ticket.getString("prepaid_travel_reservation_code");
                        }
                        ticket.remove("boarding_pass_reservation_code");
                        ticket.remove("rental_reservation_code");
                        ticket.remove("parcel_reservation_code");
                        ticket.remove("prepaid_reservation_code");
                        ticket.remove("prepaid_travel_reservation_code");

                        ticket.put("reservation_code", reservationCode);

                        String notes = "";
                        if(ticket.getString("boarding_pass_notes") != null)
                            notes = ticket.getString("boarding_pass_notes");
                        else if(ticket.getString("parcel_notes") != null)
                            notes = ticket.getString("parcel_notes");

                        ticket.remove("boarding_pass_notes");
                        ticket.remove("parcel_notes");
                        ticket.put("notes", notes);

                        if (ticket.getInteger("boarding_pass_customer_id") != null) {
                            customerId = ticket.getInteger("boarding_pass_customer_id");
                        } else if (ticket.getInteger("rental_customer_id") != null) {
                            customerId = ticket.getInteger("rental_customer_id");
                        } else if (ticket.getInteger("parcel_customer_id") != null) {
                            customerId = ticket.getInteger("parcel_customer_id");
                        }else if (ticket.getInteger("prepaid_customer_id") != null) {
                            customerId = ticket.getInteger("prepaid_customer_id");
                        }else if (ticket.getInteger("prepaid_travel_customer_id") != null) {
                            customerId = ticket.getInteger("prepaid_travel_customer_id");
                        }
                        ticket.remove("boarding_pass_customer_id");
                        ticket.remove("rental_customer_id");
                        ticket.remove("parcel_customer_id");
                        ticket.remove("prepaid_customer_id");
                        ticket.remove("prepaid_travel_customer_id");
                        ticket.put("customer_id", customerId);

                        if(rentalReservationCode.equals("0")){
                            ticket.put("header", hdr);
                            ticket.put("footer", footer);
                            Integer finalContPrinted = contPrinted;
                            conn.queryWithParams(QUERY_TICKET_DETAILS, params, detailReply -> {
                                try {
                                    if (detailReply.failed()){
                                        throw detailReply.cause();
                                    }
                                    List<JsonObject> details = detailReply.result().getRows();
                                    details.forEach(elem -> {
                                        if (elem.getString("detail") != null) {
                                            String[] stringArray = elem.getString("detail").split(" ");
                                            if(Arrays.asList(stringArray).contains("del")){
                                                String[] splicedElem = Arrays.copyOfRange(stringArray, Arrays.asList(stringArray).indexOf("al") , stringArray.length -1);
                                                String stringFinished = getDateFromString(splicedElem);
                                                String[] finalArray = Arrays.copyOfRange(stringArray, 0 , Arrays.asList(stringArray).indexOf("al") + 1);

                                                finalArray[finalArray.length -1] = stringFinished;
                                                elem.put("detail_tablet" ,String.join(" " , finalArray) );

                                            }
                                        }
                                    });
                                    ticket.put("details", details);

                                    this.getTicket(conn, ticket, params, updatedBy, ticketId, finalContPrinted).whenComplete((JsonObject ticketContent, Throwable error) -> {
                                        try {
                                            if (error != null){
                                                throw error;
                                            }
                                            this.insertTrackings(conn, ticket, updatedBy).whenComplete((resultTrackings, errorTrackings) -> {
                                                try {
                                                    if (errorTrackings != null){
                                                        throw errorTrackings;
                                                    }
                                                    this.commit(conn, message, ticket);
                                                } catch (Throwable t){
                                                    this.rollback(conn, t, message);
                                                }
                                            });
                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });
                        }
                        else {
                            Future f1 = Future.future();
                            Future f2 = Future.future();
                            Future f3 = Future.future();

                            vertx.eventBus().send(RentalDBV.class.getSimpleName(),
                                    new JsonObject().put("reservationCode", rentalReservationCode),
                                    new DeliveryOptions().addHeader(ACTION, ACTION_QUOTATION),
                                    f1.completer());

                            conn.queryWithParams(QUERY_TICKET_DETAILS, params,
                                    f2.completer());

                            conn.queryWithParams(QUERY_RENTAL_PAYMENT_BALANCE, new JsonArray()
                                            .add(ticketCreatedAt)
                                            .add(rentalReservationCode),
                                    f3.completer());
                            //obtener datos de la renta
                            Integer reFinalContPrinted = contPrinted;
                            CompositeFuture.all(f1, f2, f3).setHandler(r -> {
                                try {
                                    if (r.failed()){
                                        throw r.cause();
                                    }
                                    JsonObject rental = r.result().<Message<JsonObject>>resultAt(0).body();
                                    List<JsonObject> ticketsDetails = r.result().<ResultSet>resultAt(1).getRows();
                                    JsonObject rentalPayments = r.result().<ResultSet>resultAt(2).getRows().get(0);

                                    String rentPaid = ticket.getDouble("total").toString();
                                    String rentVehicle = rental.getString("vehicle_number");
                                    Integer rentPersons = rental.getInteger("seatings");
                                    String rentDestiny = rental.getString("destiny_city_name") + (rental.getString("destiny_state_name") == null ? "" : ", " + rental.getString("destiny_state_name"));

                                    String rentDeparture = "";
                                    try {
                                        Date rentDepartureDate = UtilsDate.parseSdfDatabase(rental.getString("departure_date") + " 00:00:00");
                                        rentDeparture = UtilsDate.format_D_MM_YYYY(rentDepartureDate);
                                    } catch (ParseException e) {
                                        throw e.getCause();
                                    }
                                    String rentReturn = "";
                                    try {
                                        Date rentReturnDate = UtilsDate.parseSdfDatabase(rental.getString("return_date") + " 00:00:00");
                                        rentReturn = UtilsDate.format_D_MM_YYYY(rentReturnDate);
                                    } catch (ParseException e) {
                                        throw e.getCause();
                                    }
                                    Boolean rentHasDriver = rental.getBoolean("has_driver");

                                    Double rentTotalAmount = rental.getDouble("total_amount");
                                    Double rentDeposit = rental.getDouble("guarantee_deposit");
                                    Double rentTotalToPay = rentDeposit + rentTotalAmount;
                                    Double rentMissingPay = rentalPayments.getDouble("missing_to_pay");
                                    Double rentTotalPaid = rentTotalToPay - rentMissingPay;
                                    if(rental.getInteger("rent_status").equals(3)){
                                        rentMissingPay = (rentMissingPay - rentDeposit);
                                    }

                                    ticket.put("footer_detail", new JsonObject()
                                            .put("current_pay", rentPaid)
                                            .put("vehicle", rentVehicle)
                                            .put("vehicle", rentVehicle)
                                            .put("persons", rentPersons.toString())
                                            .put("destiny", rentDestiny)
                                            .put("departure", rentDeparture)
                                            .put("return", rentReturn)
                                            .put("has_driver", rentHasDriver)
                                            .put("service", rentTotalAmount)
                                            .put("deposit", rentDeposit)
                                            .put("total_to_pay", rentTotalToPay)
                                            .put("total_paid", rentTotalPaid)
                                            .put("missing_to_pay", rentMissingPay));

                                    ticket.put("header", hdr);
                                    ticket.put("footer", footer);
                                    ticket.put("details", ticketsDetails);
                                    if (ticket.getDouble("payback_after") != null && ticket.getDouble("payback_money") != null){
                                        ticket.put("payback_total", ticket.getDouble("payback_after")+ticket.getDouble("payback_money"));
                                    }
                                    if (ticket.getInteger("rental_id") != null && ticket.getString("action") != null && ticket.getString("action").equals("cancel")){
                                        conn.queryWithParams("SELECT cancel_reason FROM rental WHERE id = ?",
                                                new JsonArray().add(ticket.getInteger("rental_id")),
                                                replyCancelReason ->{
                                                    try {
                                                        if (replyCancelReason.failed()){
                                                            throw replyCancelReason.cause();
                                                        }
                                                        String cancelReason = replyCancelReason.result().getRows().get(0).getString("cancel_reason");
                                                        ticket.put("cancel_reason", cancelReason);
                                                        this.getTicket(conn, ticket, params, updatedBy, ticketId, reFinalContPrinted).whenComplete((JsonObject ticketContent, Throwable error) -> {
                                                            try {
                                                                if (error != null){
                                                                    throw error;
                                                                }
                                                                this.insertTrackings(conn, ticket, updatedBy).whenComplete((resultTrackings, errorTrackings) -> {
                                                                    try {
                                                                        if (errorTrackings != null){
                                                                            throw errorTrackings;
                                                                        }
                                                                        this.commit(conn, message, ticket);
                                                                    } catch (Throwable t){
                                                                        this.rollback(conn, t, message);
                                                                    }
                                                                });
                                                            } catch (Throwable t){
                                                                this.rollback(conn, t, message);
                                                            }
                                                        });
                                                    } catch (Throwable t){
                                                        this.rollback(conn, t, message);
                                                    }
                                                });
                                    } else {
                                        this.getTicket(conn, ticket, params, updatedBy, ticketId, reFinalContPrinted).whenComplete((JsonObject ticketContent, Throwable error) -> {
                                            try {
                                                if (error != null){
                                                    throw error;
                                                }
                                                this.insertTrackings(conn, ticket, updatedBy).whenComplete((resultTrackings, errorTrackings) -> {
                                                    try {
                                                        if(errorTrackings != null){
                                                            throw errorTrackings;
                                                        }
                                                        this.commit(conn, message, ticket);
                                                    } catch (Throwable t){
                                                        this.rollback(conn, t, message);
                                                    }
                                                });
                                            } catch (Throwable t){
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    }
                                } catch (Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });
                        }
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    private String getDateFromString(String[] element){
        String convertString = element[1] + " " +  element[3]  + " " + element[5];
        Locale l = new Locale("es", "ES");
        DateTimeFormatter f = DateTimeFormatter.ofPattern("d MMMM uuuu", l);
        LocalDate ld = LocalDate.parse(convertString, f);
        String stringFinished = ld.toString() + " a las " + element[element.length -1];

        return stringFinished;
    }

    private CompletableFuture<Boolean> insertTrackings(SQLConnection conn, JsonObject ticket, Integer updatedBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer boardingPassId = ticket.getInteger("boarding_pass_id");
            Integer parcelId = ticket.getInteger("parcel_id");
            if(boardingPassId != null){
                JsonArray bTracking = new JsonArray().add(new JsonObject()
                        .put("boardingpass_id", boardingPassId)
                        .put("prints_counter", ticket.getInteger("prints_counter"))
                        .put("id", ticket.getInteger("id")));
                this.insertTracking(conn, bTracking, "boarding_pass_tracking", "boardingpass_id", "ticket_id", null, "printed", updatedBy)
                        .whenComplete((resultTracking, errorTracking) -> {
                            try{
                                if(errorTracking != null){
                                    throw errorTracking;
                                }
                                future.complete(resultTracking);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
            } else if(parcelId != null){
                JsonArray pTracking = new JsonArray().add(new JsonObject()
                        .put("parcel_id", parcelId)
                        .put("prints_counter", ticket.getInteger("prints_counter"))
                        .put("id", ticket.getInteger("id")));
                this.insertTracking(conn, pTracking, "parcels_packages_tracking", "parcel_id", "ticket_id", null, "printed", updatedBy)
                        .whenComplete((resultTracking, errorTracking) -> {
                            try{
                                if(errorTracking != null){
                                    throw errorTracking;
                                }
                                future.complete(resultTracking);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
            } else {
                future.complete(true);
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getTicket (SQLConnection conn, JsonObject ticket, JsonArray params, Integer updatedBy, Integer ticketId, Integer contPrinted) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            conn.queryWithParams(QUERY_TICKET_PAYMENTS, params, paymentReply -> {
                try {
                    List<JsonObject> paymentsList = paymentReply.result().getRows();
                    int plen = paymentsList.size();
                    List<JsonObject> payments = new ArrayList<>();
                    JsonObject payment = new JsonObject();
                    for (int i = 0; i < plen; i++) {
                        payment = paymentsList.get(i);
                        payments.add(new JsonObject()
                                .put("payment_reference", payment.getString("payment_reference"))
                                .put("payment_method", payment.getString("payment_method"))
                                .put("is_cash", payment.getBoolean("is_cash"))
                                .put("amount", payment.getDouble("amount"))
                        );
                    }
                    ticket.put("payments", payments);

                    GenericQuery update = this.generateGenericUpdate("tickets", new JsonObject()
                            .put("id", ticketId)
                            .put("prints_counter", contPrinted)
                            .put("updated_by", updatedBy)
                            .put("updated_at", UtilsDate.sdfDataBase(new Date())));
                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                        try {
                            if (replyUpdate.failed()){
                                throw replyUpdate.cause();
                            }
                            ticket.put("prints_counter", contPrinted);
                            future.complete(ticket);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_PREPAID_TICKET = "SELECT bp.id, bp.reservation_code, bp.iva, bp.total_amount,bp.created_at, "+
            " bp.terminal_origin_id, bp.terminal_destiny_id, bp.prepaid_id, bp.reservation_code, \n"+
            " b.prefix as terminal_origin,b.id,bo.prefix as terminal_destiny,bo.id, \n"+
            " ppc.max_km \n" +
            " FROM boarding_pass as bp\n"+
            " LEFT JOIN branchoffice AS b ON b.id = bp.terminal_origin_id  \n" +
            " LEFT JOIN branchoffice AS bo ON bo.id = bp.terminal_destiny_id  \n" +
            " LEFT JOIN prepaid_package_travel AS ppt on ppt.id=bp.prepaid_id \n"+
            " LEFT JOIN prepaid_package_config AS ppc on ppc.id=ppt.prepaid_package_config_id \n"+
            " WHERE ppt.id = ?";
    private static final String QUERY_TICKET = "SELECT t.id, t.ticket_code, t.action, t.rental_id, t.boarding_pass_id, t.parcel_id, t.iva, t.parcel_iva, t.total,  +\n" +
            "   t.paid, t.paid_change, t.created_at, t.created_by, t.prints_counter, t.payback_before, t.payback_money,  +\n" +
            "   co.cash_register_id, cr.cash_register, b.name as branchoffice, b.prefix, c.name AS city, s.name AS state, CONCAT(e.name, ' ', e.last_name) AS employee, \n" +
            "   bp.reservation_code AS boarding_pass_reservation_code,\n" +
            "   bp.notes AS boarding_pass_notes,\n" +
            "   pcr.name AS parcel_notes,\n" +
            "   CONCAT(bpc.first_name, ' ', bpc.last_name) AS boarding_pass_customer,\n" +
            "   bpc.id AS boarding_pass_customer_id,\n" +
            "   CONCAT(bpp.first_name, ' ', bpp.last_name) AS boarding_pass_passenger,\n" +
            "   r.reservation_code AS rental_reservation_code,\n" +
            "   CONCAT(rc.first_name, ' ', rc.last_name) AS rental_customer, \n" +
            "   rc.id AS rental_customer_id,\n" +
            "   p.parcel_tracking_code AS parcel_reservation_code,\n" +
            "   CONCAT(pc.first_name, ' ', pc.last_name) AS parcel_customer,\n" +
            "   pc.id AS parcel_customer_id,\n" +
            "   p.payment_condition parcel_payment_condition,\n" +
            "   bp.payment_condition boardingpass_payment_condition,\n" +
            "   prepaid_travel.payment_condition prepaid_travel_payment_condition,\n" +
            "   prept.tracking_code AS prepaid_reservation_code,\n" +
            "   preptC.id AS prepaid_customer_id,\n" +
            "   CONCAT(preptC.first_name, ' ', preptC.last_name) AS prepaid_customer,\n" +
            "   ppd.signature,\n"+
            "   prepaid_travel.reservation_code AS prepaid_travel_reservation_code,\n" +
            "   CONCAT(prepaid_travel_C.first_name, ' ', prepaid_travel_C.last_name) AS prepaid_travel_customer, " +
            "   prepaid_travel_C.id AS prepaid_travel_customer_id" +
            "   FROM tickets as t \n" +
            "   LEFT JOIN cash_out AS co ON co.id = t.cash_out_id \n" +
            "   LEFT JOIN cash_registers AS cr ON cr.id = co.cash_register_id \n" +
            "   LEFT JOIN branchoffice AS b ON b.id = cr.branchoffice_id \n" +
            "   LEFT JOIN city AS c ON c.id = b.city_id \n" +
            "   LEFT JOIN state AS s ON s.id = b.state_id \n" +
            "   LEFT JOIN employee AS e ON e.user_id = t.created_by\n" +
            "   LEFT JOIN boarding_pass AS bp ON bp.id = t.boarding_pass_id\n" +
            "   LEFT JOIN boarding_pass_passenger AS bpp ON bpp.boarding_pass_id = bp.id AND bpp.principal_passenger = 1\n" +
            "   LEFT JOIN customer AS bpc ON bpc.id = bp.customer_id\n" +
            "   LEFT JOIN rental AS r ON r.id = t.rental_id\n" +
            "   LEFT JOIN customer AS rc ON rc.id = r.customer_id\n" +
            "   LEFT JOIN parcels AS p ON p.id = t.parcel_id \n" +
            "   LEFT JOIN customer AS pc ON pc.id = p.customer_id\n" +
            "   LEFT JOIN parcels_cancel_reasons AS pcr ON pcr.id = p.parcels_cancel_reason_id \n" +
            "   LEFT JOIN parcels_packages AS pp ON pp.parcel_id = p.id \n" +
            "   LEFT JOIN parcels_deliveries AS ppd ON ppd.id = pp.parcels_deliveries_id \n" +
            "   LEFT JOIN parcels_prepaid prept ON prept.id = t.parcel_prepaid_id \n" +
            "   LEFT JOIN customer preptC ON preptC.id = prept.customer_id \n" +
            "   LEFT JOIN prepaid_package_travel prepaid_travel ON prepaid_travel.id = t.prepaid_travel_id \n" +
            "   LEFT JOIN customer prepaid_travel_C ON prepaid_travel_C.id = prepaid_travel.customer_id \n" +
            "   WHERE t.id = ?;";

    private static final String QUERY_TICKET_DETAILS = "SELECT quantity, detail, unit_price, discount, amount \n"
            + " FROM tickets_details\n"
            + " WHERE ticket_id = ?;";

    private static final String QUERY_TICKET_PAYMENTS = "SELECT pm.name AS payment_method, pm.is_cash AS is_cash, p.amount, p.reference AS payment_reference \n" +
            "    FROM payment AS p\n" +
            "    LEFT JOIN payment_method AS pm ON pm.id = p.payment_method_id\n" +
            "    WHERE p.ticket_id = ? and p.status = 1 and p.payment_status = 1;";

    private static final String QUERY_RENTAL_PAYMENT_BALANCE = "SELECT r.id, r.rent_status,\n"
            + "	SUM(p.amount) AS total_paid,\n"
            + "	COALESCE(r.driver_cost,0) AS driver_cost,\n"
            + "	(COALESCE(r.extra_charges,0) + COALESCE(r.checklist_charges,0)) AS extra_charges,\n"
            + "	COALESCE(r.guarantee_deposit,0) AS guarantee_deposit,\n"
            + "	COALESCE(r.total_amount,0) AS total_amount,\n"
            + "	( COALESCE((r.total_amount + r.guarantee_deposit), 0) - SUM(p.amount)) AS missing_to_pay\n"
            + "FROM\n"
            + "	payment p\n"
            + "JOIN rental r ON\n"
            + "	p.rental_id = r.id\n"
            + "WHERE\n"
            + " p.status != 3\n"
            + " AND DATE(p.created_at) <= DATE(?)\n"
            + "	AND r.reservation_code = ? GROUP BY r.id;";
}
