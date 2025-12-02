/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.boardingpass;

import database.boardingpass.enums.TICKET_TYPE;
import database.boardingpass.handlers.PartnerEndRegister;
import database.branchoffices.BranchofficeDBV;
import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.money.ExpenseDBV;
import database.money.PaybackDBV;
import database.money.PaymentDBV;
import database.parcel.ParcelDBV;
import database.parcel.ParcelsPackagesDBV;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.UpdateResult;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.format.DateTimeFormat;
import service.commons.Constants;
import service.commons.MailVerticle;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;
import utils.UtilsValidation;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static database.boardingpass.TicketPricesRulesDBV.ACTION_APPLY_TICKET_PRICE_RULE;
import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static database.money.ExpenseDBV.CURRENCY_ID;
import static database.parcel.ParcelDBV.REGISTER;
import static database.parcel.ParcelDBV.TICKET_ID;
import static database.promos.PromosDBV.TICKET_TYPE_ROUTE;
import static database.promos.PromosDBV.*;
import static database.routes.ConfigTicketPriceDBV.CONFIG_TICKET_PRICE;
import static database.routes.ScheduleRouteDBV.CURRENT_POSITION;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.DISCOUNT;
import static service.commons.Constants.*;
import static utils.UtilsValidation.differentValues;
import static utils.UtilsValidation.isStatusActive;

/**
 *
 * @author ulises
 */
public class BoardingPassDBV extends DBVerticle {

    // Vertex actions
    public static final String INIT_REGISTER = "BoardingPassDBV.initRegister";
    public static final String END_REGISTER = "BoardingPassDBV.endRegister";
    public static final String PARTNER_END_REGISTER = "BoardingPassDBV.endRegisterPartner";
    public static final String CANCEL_REGISTER = "BoardingPassDBV.cancelRegister";
    public static final String CANCEL_REGISTER_BY_RESERVATION_CODE = "BoardingPassDBV.cancelRegisterByReservationCode";
    public static final String CANCEL_RESERVATION = "BoardingPassDBV.cancelReservation";
    public static final String CANCEL_RESERVATION_WITH_CASHOUT_CLOSED = "BoardingPassDBV.cancelReservationWithCashOutClosed";
    public static final String TICKETS = "BoardingPassDBV.tickets";
    public static final String PRINT_TICKETS = "BoardingPassDBV.printTickets";
    public static final String TICKETS_COMPLEMENTS = "BoardingPassDBV.tickets_complements";
    public static final String EXCHANGE_RESERVATION_CODE = "BoardingPassDBV.exchangeReservation";
    public static final String SEATS = "BoardingPassDBV.seats";
    public static final String SEATS_WITH_DESTINATION = "BoardingPassDBV.getSeatsWithDestination";
    public static final String RESERVATION_DETAIL = "BoardingPassDBV.reservationDetail";
    public static final String PRINT_TERMS_CONDITIONS = "BoardingPassDBV.printTermsConditions";
    public static final String PUBLIC_RESERVATION_DETAIL = "BoardingPassDBV.publicReservationDetail";
    public static final String CHECKIN_TRAVEL_DETAIL = "BoardingPassDBV.checkinTravelDetail";
    public static final String TRAVEL_DETAIL = "BoardingPassDBV.travelDetail";
    public static final String TRAVELS_DETAIL = "BoardingPassDBV.travelsDetail";
    public static final String CHECK_IN = "BoardingPassDBV.checkIn";
    public static final String CALCULATE_CHECK_IN = "BoardingPassDBV.calculateCheckIn";
    public static final String CALCULATE_CHECK_IN_AGAIN = "BoardingPassDBV.calculateCheckInAgain";
    public static final String CHANGE_PASSENGERS = "BoardingPassDBV.changePassengers";
    public static final String GET_TICKETS_PRICE = "BoardingPassDBV.getTicketsPrice";
    public static final String BOARDINGPASS_ADVANCED_SEARCH = "BoardingPassDBV.advancedSearch";
    public static final String BOARDINGPASS_OCCUPATION_REPORT = "BoardingPassDBV.occupationReport";
    public static final String GET_PHONE_RESERVATION = "BoardingPassDBV.getPhoneReservation";
    public static final String CHECK_IN_AGAIN = "BoardingPassDBV.checkInAgain";
    public static final String INIT_PREPAID = "BoardingPassDBV.initPrepaid";
    public static final String END_PREPAID = "BoardingPassDBV.endRegisterPrepaid";
    public static final String CANCEL_PREPAID_TRAVEL_INIT = "BoardingPassDBV.cancelPrepaidTravelInit";
    public static final String EXPIRE_RESERVATIONS_PREPAID = "BoardingPassDBV.expirePrepaidReservation";

    //WEBDEV
    public static final String REPORT_GENERAL ="BoardingPassDBV.reportGeneral";
    public static final String REPORTAPP = "BoardingPassDBV.reportApp";
    public static final String REPORT_MONTH = "BoardingPassDBV.reportMonth"; //webdev
    //WEBDEV
    public static final String PARTIAL_CANCEL_RESERVATION = "BoardingPassDBV.partialCancelReservation";
    public static final String CHANGE_RESERVATION = "BoardingPassDBV.changeReservation";
    public static final String EXPIRE_RESERVATION = "BoardingPassDBV.expireReservation";

    public static final String DRIVER_END_CALCULATE = "BoardingPassDBV.endCalculate";
    public static final String DRIVER_END_MAKE = "BoardingPassDBV.endMake";
    public static final String REPORT = "BoardingPassDBV.report";
    public static final String REPORT_PASSENGER = "BoardingPassDBV.reportPassengerType";
    public static final String REPORT_TRAVEL_FREQUENCY ="BoardingPassDBV.reportTravelFrequency";
    public static final String REPORT_TOTALS = "BoardingPassDBV.reportTotals";
    
    public static final String SPECIAL_TICKET_LIST = "BoardingPassDBV.specialTicketList";
    public static final String ACTION_GET_PRICES_LIST = "BoardingPassDBV.pricesList";
    public static final String ACTION_GET_PRICES_LIST_REGISTERED = "BoardingPassDBV.pricesListRegistered";
    public static final String ACTION_GET_PRICES_LIST_DETAIL = "BoardingPassDBV.pricesListDetail";
    public static final String ACTION_GET_PRICE_LIST_STATUS = "BoardingPassDBV.pricesListStatus";
    public static final String ACTION_GET_COUNT_PRICELIST_HASH = "BoardingPassDBV.priceListCount";
    public static final String ACTION_REGISTER_PRICE_LIST = "BoardingPassDBV.registerPriceList";
    public static final String ACTION_APPLY_PRICE_LIST = "BoardingPassDBV.priceListApply";
    public static final String RUN_STATUS = "BoardingPassDBV.runStatus";
    public static final String CANCEL_SALES_REPORT = "BoardingPassDBV.salesCancelReport";
    // Field names
    public static final String TERMINAL_ORIGIN_ID = "terminal_origin_id";
    public static final String TERMINAL_DESTINY_ID = "terminal_destiny_id";
    public static final String SCHEDULE_ROUTE_ID = "schedule_route_id";
    public static final String SCHEDULE_ROUTE_DESTINATION_ID = "schedule_route_destination_id";
    public static final String CONFIG_DESTINATION_ID = "config_destination_id";
    public static final String CONFIG_ROUTE_ID = "config_route_id";
    public static final String ORDER_ROUTE = "order_route";
    public static final String SEAT = "seat";
    public static final String BOARDINGPASS_STATUS = "boardingpass_status";
    public static final String RESERVATION_CODE = "reservation_code";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String PRICES = "prices";
    public static final String CONFIG_TICKET_PRICE_ID = "config_ticket_price_id";
    public static final String ORDER_ORIGIN = "order_origin";
    public static final String ORDER_DESTINY = "order_destiny";
    public static final String TICKET_TYPE = "ticket_type";
    public static final String EXPIRES_AT = "expires_at";
    public static final String VEHICLE_ID = "vehicle_id";
    public static final String SHIPMENT_ID = "shipment_id";

    private PartnerEndRegister partnerEndRegister;

    @Override
    public String getTableName() {
        return "boarding_pass";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.partnerEndRegister = new PartnerEndRegister(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case INIT_REGISTER:
                this.initRegister(message);
                break;
            case END_REGISTER:
                this.endRegister(message);
                break;
            case PARTNER_END_REGISTER:
                this.partnerEndRegister.handle(message);
                break;
            case TICKETS:
                this.getTickets(message);
                break;
            case PRINT_TICKETS:
                this.printTickets(message);
                break;
            case TICKETS_COMPLEMENTS:
                this.getTicketsComplements(message);
                break;
            case EXCHANGE_RESERVATION_CODE:
                this.exchangeReservation(message);
                break;
            case SEATS:
                this.getSeats(message);
                break;
            case SEATS_WITH_DESTINATION:
                this.getSeatsWithDestination(message);
                break;
            case CHANGE_PASSENGERS:
                this.changePassengers(message);
                break;
            case RESERVATION_DETAIL:
                this.reservationDetail(message);
                break;
            case PUBLIC_RESERVATION_DETAIL:
                this.publicReservationDetail(message);
                break;
            case CHECKIN_TRAVEL_DETAIL:
                this.checkinTravelDetail(message);
                break;
            case TRAVEL_DETAIL:
                this.travelDetail(message);
                break;
            case TRAVELS_DETAIL:
                this.travelsDetail(message);
                break;
            case CANCEL_REGISTER:
                this.cancelRegister(message);
                break;
            case CANCEL_REGISTER_BY_RESERVATION_CODE:
                this.cancelRegisterByReservationCode(message);
                break;
            case CANCEL_RESERVATION:
                this.cancelReservation(message);
                break;
            case CHECK_IN:
                this.makeCheckIn(message);
                break;
            case CALCULATE_CHECK_IN:
                this.calculateCheckIn(message);
                break;
            case CALCULATE_CHECK_IN_AGAIN:
                this.calculateCheckInAgain(message);
                break;
            case BOARDINGPASS_ADVANCED_SEARCH:
                this.advancedSearch(message);
                break;
            case DRIVER_END_CALCULATE:
                this.endCalculate(message);
                break;
            case DRIVER_END_MAKE:
                this.endMake(message);
                break;
            case REPORTAPP:
                this.reportApp(message);
                break;
            case REPORT_GENERAL:
                this.reportGeneral(message);
                break;
            case REPORT_MONTH: //webdev
                this.reportMonth(message);
                break;
            case RUN_STATUS:
                this.runStatus(message);
                break;
            case PRINT_TERMS_CONDITIONS:
                this.printTermsConditions(message);
                break;
            case CANCEL_SALES_REPORT:
                this.salesCancelReport(message);
                break;
            case REPORT:
                this.report(message);
                break;
            case REPORT_PASSENGER:
                this.reportPassengerType(message);
                break;
            case REPORT_TRAVEL_FREQUENCY:
                this.reportTravelFrequency(message);
                break;
            case REPORT_TOTALS:
                this.reportTotals(message);
                break;
            case BOARDINGPASS_OCCUPATION_REPORT:
                this.occupationReport(message);
                break;
            case SPECIAL_TICKET_LIST:
                this.specialTicketList(message);
                break;
            case PARTIAL_CANCEL_RESERVATION:
                this.cancelPartialReservation(message);
                break;
            case CHANGE_RESERVATION:
                this.changeReservation(message);
                break;
            case GET_TICKETS_PRICE:
                this.getTicketsPrice(message);
                break;
            case CANCEL_RESERVATION_WITH_CASHOUT_CLOSED:
                this.cancelReservationWithCashOutClosed(message);
                break;
            case EXPIRE_RESERVATION:
                this.expireReservation(message);
                break;
            case GET_PHONE_RESERVATION:
                this.getPhoneReservation(message);
                break;
             case ACTION_GET_PRICES_LIST:
                this.pricesList(message);
                break;
            case ACTION_GET_PRICES_LIST_REGISTERED:
                this.pricesListRegistered(message);
                break;
            case ACTION_GET_PRICES_LIST_DETAIL:
                this.pricesListDetail(message);
                break;
            case ACTION_REGISTER_PRICE_LIST:
                this.registerPriceList(message);
                break;
            case ACTION_APPLY_PRICE_LIST:
                this.priceListApply(message);
                break;
            case ACTION_GET_PRICE_LIST_STATUS:
                this.priceListStatus(message);
                break;
            case ACTION_GET_COUNT_PRICELIST_HASH:
                this.priceListCount(message);
                break;
            case CHECK_IN_AGAIN:
                this.makeCheckInAgain(message);
                break;
            case INIT_PREPAID:
                this.initPrepaid(message);
                break;
            case END_PREPAID:
                this.endRegisterPrepaid(message);
                break;
            case CANCEL_PREPAID_TRAVEL_INIT:
                this.cancelPrepaidTravelInit(message);
                break;
            case EXPIRE_RESERVATIONS_PREPAID:
                this.expirePrepaidReservation(message);
                break;
        }
    }

    /**
     * GENERAL METHODS <!- START -!>
     */

    public CompletableFuture<JsonObject> getBoardingPassById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String query = "SELECT bp.*, ship.id AS shipment_id FROM boarding_pass bp\n" +
                    " LEFT JOIN boarding_pass_route bpr ON bpr.boarding_pass_id = bp.id\n" +
                    " LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
                    " LEFT JOIN shipments ship ON ship.schedule_route_id = srd.schedule_route_id\n" +
                    "   AND ship.shipment_type = 'load' AND ship.terminal_id = srd.terminal_origin_id\n" +
                    " WHERE bp.id = ? AND bp.status = 1 LIMIT 1;";
            conn.queryWithParams(query, new JsonArray().add(id), (AsyncResult<ResultSet> reply) -> {
                try {
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Boarding pass not found");
                    }
                    JsonObject boardingPass = result.get(0);
                    future.complete(boardingPass);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getBoardingPassByReservationCode(String reservationCode, Integer boardingPassStatus) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String query = "SELECT * FROM boarding_pass WHERE reservation_code = ? AND status = 1 ";
        JsonArray params = new JsonArray().add(reservationCode);
        if (boardingPassStatus != null){
            query = query.concat(" AND (boardingpass_status = ? OR (purchase_origin = 'app chofer' AND boardingpass_status = 2 ))");
            params.add(boardingPassStatus);
        }
        this.dbClient.queryWithParams(query, params, (AsyncResult<ResultSet> reply) -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Boarding pass not found");
                }
                future.complete(result.get(0));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getBoardingPassTicketById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String query = "SELECT * FROM boarding_pass_ticket WHERE id=? AND status=1 LIMIT 1;";
        conn.queryWithParams(query, new JsonArray().add(id), (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet result = reply.result();
                if (result.getNumRows() == 0) {
                    throw new Exception("Boarding pass ticket not found");
                }
                JsonObject boardingPass = result.getRows().get(0);
                future.complete(boardingPass);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }

        });

        return future;
    }


    public CompletableFuture<JsonObject> getTotalAmountTicketsForBoardingPassById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(id);
        conn.queryWithParams(QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_BOARDING_PASS, params, (AsyncResult<ResultSet> reply) -> {
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet resultTickets = reply.result();
                if (resultTickets.getNumRows() == 0) {
                    throw new Exception("Tickets not found for boarding pass");
                }
                JsonObject accumTickets = resultTickets.getRows().get(0);
                future.complete(accumTickets);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getTotalAmountTicketsForBoardingPassById(SQLConnection conn, Integer id, Integer boardingPassStatus) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(id);
        String QUERY = boardingPassStatus == 1 ? QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_BOARDING_PASS : QUERY_TOTAL_AMOUNT_TICKETS_PASSENGER_CANCELED;
        conn.queryWithParams(QUERY, params, (AsyncResult<ResultSet> reply) -> {
            try {
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet resultTickets = reply.result();
                if (resultTickets.getNumRows() == 0) {
                    throw new Exception("Tickets not found for boarding pass");
                }
                JsonObject accumTickets = resultTickets.getRows().get(0);
                future.complete(accumTickets);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getTotalAmountPaymentsForBoardingPassById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(id);
        conn.queryWithParams(QUERY_TOTAL_AMOUNT_FOR_PAYMENTS_BY_BOARDING_PASS, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet resultTickets = reply.result();
                if (resultTickets.getNumRows() == 0) {
                    future.completeExceptionally(new Throwable("Payments not found for boarding pass"));
                } else {
                    JsonObject accumTickets = resultTickets.getRows().get(0);
                    future.complete(accumTickets);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getComplementById(SQLConnection conn, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(id);
        conn.queryWithParams(QUERY_GET_COMPLEMENT_BY_ID, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet result = reply.result();
                if (result.getNumRows() == 0) {
                    future.completeExceptionally(new Throwable("Complement not found: " + id.toString()));
                } else {
                    JsonObject complement = result.getRows().get(0);
                    future.complete(complement);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private Future<JsonObject> getTickets(String reservationCode, JsonArray tickets, Integer cashRegisterId) {
        Future<JsonObject> future = Future.future();

        try {
            String query = QUERY_TICKETS;
            JsonArray params = new JsonArray().add(reservationCode);
            if (tickets != null && tickets.size() > 0) {
                StringBuilder sql = new StringBuilder().append("(");
                int len = tickets.size();
                for (int i = 0; i < len; i++) {
                    sql.append(" boarding_pass_ticket.id = ? ");
                    if (i < len-1) {
                        sql.append("OR");
                    }
                }
                sql.append(")");
                query = QUERY_TICKETS.concat(" AND ").concat(sql.toString());
                params.addAll(tickets);
            }

            dbClient.queryWithParams(query, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    dbClient.queryWithParams(QUERY_GET_LAST_TICKET_CASH_REGISTER, new JsonArray().add(cashRegisterId), replyFind -> {
                        try {
                            if (replyFind.failed()) {
                                throw replyFind.cause();
                            }

                            List<JsonObject> results = replyFind.result().getRows();
                            if (results.isEmpty()) {
                                throw new Exception("Cash register not found");
                            }

                            JsonObject cashRegister = results.get(0);
                            List<JsonObject> passes = reply.result().getRows();
                            future.complete(cashRegister.put("tickets", passes));
                        } catch (Throwable ex) {
                            future.fail(ex);
                        }
                    });


                } catch (Throwable ex) {
                    future.fail(ex);
                }
            });
        } catch (Throwable ex) {
            future.fail(ex);
        }

        return future;
    }

    private CompletableFuture<Integer> setTicketNumber(SQLConnection conn, JsonObject ticket, Integer ticketNumber) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            String query = "UPDATE boarding_pass_ticket SET ticket_number = ? WHERE id = ?;";
            JsonArray params = new JsonArray()
                    .add(ticketNumber)
                    .add(ticket.getInteger("id"));

            conn.updateWithParams(query, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    if (reply.result().getUpdated() == 0) {
                        future.completeExceptionally(new Exception("Ticket not found"));
                    }

                    future.complete(ticketNumber);
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }


    /**
     * GENERAL METHODS <!- START -!>
     */

    private void getTickets(Message<JsonObject> message) {
            try {
                JsonObject body = message.body();
                String reservationCode = body.getString("reservation_code");
                JsonArray tickets = body.getJsonArray("tickets");
                Integer cashRegisterId = body.getInteger(CASH_REGISTER_ID);
                getTickets(reservationCode, tickets, cashRegisterId).setHandler(reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }

                        message.reply(reply.result());
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
    }

    private void printTickets(Message<JsonObject> message) {
        startTransaction(message, conn -> {
            JsonObject body = message.body();
            String reservationCode = body.getString("reservation_code");
            JsonArray tickets = body.getJsonArray("tickets");
            Integer createdBy = body.getInteger(CREATED_BY);
            Integer cashRegisterId = body.getInteger(CASH_REGISTER_ID);
            getTickets(reservationCode, tickets, cashRegisterId).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }

                    JsonObject cashRegister = reply.result();
                    AtomicReference<Integer> lastTicket = new AtomicReference<>(cashRegister.getInteger("last_ticket"));
                    JsonArray passes = cashRegister.getJsonArray("tickets");
                    this.execUpdatePrintsCounter(conn, passes, "boarding_pass_ticket", createdBy).whenComplete((resultUpdatePrintsCounter, errorPrintsCounter) -> {
                        try {
                            if (errorPrintsCounter != null) {
                                throw errorPrintsCounter;
                            }
                            this.insertTracking(conn, passes, "boarding_pass_tracking", "boardingpass_id","boardingpass_ticket_id", null, "printed", createdBy).whenComplete((resultTracking, errorTracking) -> {
                                try {
                                    if (errorTracking != null) {
                                        throw errorTracking;
                                    }

                                    CompletableFuture.allOf(passes.stream().map(ticket -> {
                                        lastTicket.set(lastTicket.get() + 1);
                                        return setTicketNumber(conn, (JsonObject) ticket, lastTicket.get());
                                    }).toArray(CompletableFuture[]::new)).whenComplete((__, error) -> {
                                        try {
                                            if (error != null) {
                                                throw error;
                                            }
                                            
                                            this.updateCashRegister(conn, cashRegisterId, lastTicket.get(), createdBy).whenComplete((resultCashRegister, errorCashRegister) -> {
                                                try {
                                                    if (errorCashRegister != null) {
                                                        throw  errorCashRegister;
                                                    }
                                                    this.commit(conn, message, new JsonObject()
                                                            .put("tickets", passes)
                                                            .put("last_ticket", lastTicket.get())
                                                    );

                                                } catch (Throwable throwable) {
                                                    throwable.printStackTrace();
                                                }
                                            });


                                        } catch (Throwable ex) {
                                            this.rollback(conn, ex, message);
                                        }
                                    });

                                } catch(Throwable e) {
                                    this.rollback(conn, e, message);
                                }
                            });
                        } catch (Throwable e) {
                            this.rollback(conn, e, message);
                        }
                    });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> updateCashRegister(SQLConnection conn, Integer cashRegisterId, Integer lastTicket, Integer updatedBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            JsonObject cashRegister = new JsonObject();
            cashRegister.put("id", cashRegisterId)
                    .put("last_ticket", lastTicket)
                    .put("updated_by", updatedBy)
                    .put("updated_at", UtilsDate.sdfDataBase(new Date()));
            GenericQuery update = this.generateGenericUpdate("cash_registers", cashRegister);

            conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    future.complete(cashRegister);
                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(reply.cause());
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }


    private void printTermsConditions(Message<JsonObject> message) {
        String reservationCode = message.body().getString("reservation_code");

        if (reservationCode == null) {
            reportQueryError(message, new Throwable("Send boarding pass reservation code"));
            return;
        }


        this.dbClient.queryWithParams(QUERY_GET_PRINCIPAL_PASSANGER, new JsonArray().add(reservationCode), reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() > 0){
                    JsonObject terms = reply.result().getRows().get(0);
                    terms.put("reservation_code", reservationCode);

                    message.reply(terms);
                } else {
                    reportQueryError(message, new Throwable("Reservation code does not exists"));
                    return;
                }

            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
        });


    }

    private void getTicketsComplements(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            String reservationCode = message.body().getString("reservation_code");
            Integer createdBy = message.body().getInteger("created_by");
            if (reservationCode == null) {
                this.rollback(conn, new Throwable("Send boarding pass reservation code"), message);
            }
            JsonArray tickets = message.body().getJsonArray("tickets");
            if (tickets == null || tickets.isEmpty()) {
                this.rollback(conn, new Throwable("Send ticket ids"), message);
            } else {
                List<JsonObject> results = new ArrayList<>();
                List<CompletableFuture<List<JsonObject>>> tasks = tickets.stream()
                        .map(t -> getTicketsComplementsByTicketId(conn, reservationCode, (Integer) t, results))
                        .collect(Collectors.toList());
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t) -> {
                    if (t != null) {
                        this.rollback(conn, t.getCause(), message);
                    } else {
                        this.execUpdatePrintsCounter(conn, new JsonArray(results), "boarding_pass_complement", createdBy).whenComplete((resultUpdatePrintsCounter, errorPrintsCounter) -> {
                            try{
                                if (errorPrintsCounter != null){
                                    this.rollback(conn, errorPrintsCounter, message);
                                } else {
                                    this.insertTracking(conn, new JsonArray(results), "boarding_pass_tracking", "boardingpass_id", "boardingpass_complement_id", null, "printed", createdBy)
                                            .whenComplete((resultTracking, errorTracking) -> {
                                                try{
                                                    if(errorTracking != null){
                                                        this.rollback(conn, errorTracking, message);
                                                    } else {
                                                        this.commit(conn, message, new JsonObject().put("complements", new JsonArray(results)));

                                                    }
                                                } catch(Exception e){
                                                    this.rollback(conn, e, message);
                                                }
                                            });
                                }
                            } catch(Exception e){
                                this.rollback(conn, e, message);
                            }
                        });
                    }
                });
            }
        });

    }

    private CompletableFuture<List<JsonObject>> getTicketsComplementsByTicketId(SQLConnection conn, String reservationCode, Integer boardingPassTicketId, List<JsonObject> result) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(reservationCode)
                .add(boardingPassTicketId);

        conn.queryWithParams(QUERY_TICKETS_COMPLEMENTS, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.size() > 0) {
                    AtomicInteger currentItem = new AtomicInteger();
                    int items = results.size();
                    List<JsonObject> complements = results.stream()
                            .map(p -> p.put("item", currentItem.addAndGet(1)).put("items", items))
                            .collect(Collectors.toList());
                    result.addAll(complements);
                    future.complete(complements);
                } else {
                    future.complete(results);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    /**
     * INIT REGISTER METHODS <!- START -!>
     */

    private void initRegister(Message<JsonObject> message) {

        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            JsonArray routes = (JsonArray) body.remove("routes");
            CompletableFuture<Boolean> task = null;
            JsonObject routeTravel;
            Optional<JsonObject> routeT = (Optional) routes.stream().filter(r->{
                    JsonObject routeR = (JsonObject) r;
                    return routeR.getInteger(TICKET_TYPE_ROUTE).equals(1);
                }).findFirst();
                if(!routeT.isPresent()){
                    this.rollback(conn, new Throwable("Ticket type route not found"), message);
                    return;
             }
            routeTravel = routeT.get();
            Integer configDestinationIT = routeTravel.getInteger(CONFIG_DESTINATION_ID);
            Integer scheduleRouteDestinationIdT = routeTravel.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            if(configDestinationIT != null){
                task = getConfigDestinationTerminals(conn, configDestinationIT, body);
            }else{
                task = getScheduleRouteDestinationTerminals(conn, scheduleRouteDestinationIdT, body);
            }
                    task.whenComplete((ss, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            JsonArray passengers = (JsonArray) body.remove("passengers");
                            Integer underAge = (Integer) body.remove("under_age");
                            Integer timeBeforeCheckin = (Integer) body.remove("time_before_checkin");
                            Integer origin = body.containsKey("purchase_origin") ? body.getInteger("purchase_origin") : 1;
                            Double distance = body.containsKey("distance") ? (Double) body.remove("distance") : 0.00 ;
                            String reservationCode = body.getString(RESERVATION_CODE);
                            Boolean isPhoneReservation = body.containsKey("is_phone_reservation") ? body.getBoolean("is_phone_reservation") : Boolean.valueOf(false);
                            Integer minimumHourReservation = (Integer) body.remove("minimum_hour_reservation");
                            Integer cancelPhoneReservationTime = (Integer) body.remove("cancel_phone_reservation_time");
                            Integer reservationTime = (Integer) body.remove("reservation_time");
                            Integer reservationExpiresTime = (Integer) body.remove("boarding_cancelation_time");
                            // String timeCurrent = (String) body.remove("time_current");
                            String currentPosition = (String) body.remove(CURRENT_POSITION);
                            GenericQuery gen = this.generateGenericCreate(body);
                            Integer createdBy = body.getInteger("created_by");
                            Integer ticketType = body.getInteger("ticket_type");
                            Integer integrationPartnerSessionId = body.getInteger("integration_partner_session_id");
                            AtomicReference<Integer> scheduleRouteDestinationId = new AtomicReference<>(new Integer(0));
                            conn.setOptions(new SQLOptions().setAutoGeneratedKeys(true));

                            org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
                            DateTime travelDate = body.getValue("travel_date") != null ? formatter.parseDateTime(body.getString("travel_date")) : new DateTime();
                            Hours hoursTravelDateDifference = Hours.hoursBetween(UtilsDate.getLocalDateTime(), travelDate);
                            Integer travelDateDifference = hoursTravelDateDifference.getHours();

                            Date expiresAt;
                            Date phoneExpiresAt = travelDate.toDate();
                            if (isPhoneReservation) {
                                if (minimumHourReservation > travelDateDifference)
                                    throw new Exception("Minimum time for reservation exceeded");
                                if (ticketType == 1 || ticketType == 4)
                                    throw new Exception("ticket_type: not valid for phone reservation");

                                if (travelDateDifference <= reservationTime)
                                    expiresAt = UtilsDate.addHours(phoneExpiresAt, cancelPhoneReservationTime * -1);
                                else
                                    expiresAt = UtilsDate.addHours(new Date(), reservationTime);
                            } else {
                                    expiresAt = UtilsDate.addMinutes(new Date(), Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(reservationExpiresTime)));
                            }

                            if (Objects.nonNull(integrationPartnerSessionId)) {
                                // Expire reservations from partners after 2 hours for payments like oxxo
                                expiresAt = UtilsDate.addHours(new Date(), 2);
                            }
                            Date finalExpiresAt = expiresAt;
                            conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> boardingPassReply) -> {
                                try{
                                    if(boardingPassReply.failed()) {
                                        throw new Exception(boardingPassReply.cause());
                                    }
                                    final int boardingPassId = boardingPassReply.result().getKeys().getInteger(0);
                                    final int len = routes.size();
                                    List<CompletableFuture<JsonObject>> routesTasks = new ArrayList<>();
                                    for (int i = 0; i < len; i++) {
                                        JsonObject route = routes.getJsonObject(i);
                                        if(route.getInteger("ticket_type_route") == 1 && route.getInteger("order_route") == 1){
                                            scheduleRouteDestinationId.set(route.getInteger("schedule_route_destination_id"));
                                        }
                                        routesTasks.add(insertBoardingPassRoute(conn, route, boardingPassId, createdBy, route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID)));
                                    }
                                    CompletableFuture<Void> allRoutes = CompletableFuture.allOf(routesTasks.toArray(new CompletableFuture[routesTasks.size()]));
                                    allRoutes.whenComplete((s, t) -> {
                                        if (t != null) {
                                            this.rollback(conn, t, message);
                                        } else {
                                            if(origin.equals(5)){

                                                JsonObject route = routes.getJsonObject(0);
                                                this.dbClient.queryWithParams("SELECT bo.prefix FROM schedule_route_destination srd\n" +
                                                        "inner join branchoffice bo on bo.id = terminal_destiny_id WHERE srd.id = ?",
                                                        new JsonArray().add(route.getInteger("schedule_route_destination_id")),
                                                        reply ->{
                                                    try{
                                                        if (reply.failed()){
                                                            throw new Exception(reply.cause());
                                                        }
                                                        List<JsonObject> result = reply.result().getRows();
                                                        if(result.isEmpty()){
                                                            throw new Exception("schedule route destination not found");
                                                        }

                                                        String prefix = result.get(0).getString("prefix");
                                                        this.driverFinishInitRegister(conn, message, boardingPassId, prefix, passengers, routes, underAge, origin, distance, createdBy, reservationCode, currentPosition, finalExpiresAt);

                                                    }catch (Exception e){
                                                        e.printStackTrace();
                                                        this.rollback(conn, boardingPassReply.cause(), message);
                                                    }
                                                        });
                                            } else {
                                                this.finishInitRegister(conn, message, boardingPassId, passengers, routes, underAge, origin, createdBy, reservationCode, isPhoneReservation, finalExpiresAt, travelDateDifference, reservationTime, cancelPhoneReservationTime, integrationPartnerSessionId);
                                            }
                                        }
                                    });

                                }catch (Exception ex) {
                                    ex.printStackTrace();
                                    this.rollback(conn, boardingPassReply.cause(), message);
                                }
                            });


                        } catch(Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
        });
    }

    private CompletableFuture<JsonObject> getExpiresDays(){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams("SELECT * from general_setting where FIELD = ?", new JsonArray().add("abierto_expires_days") , reply ->{
               try{
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("General Setting not found");
                }
                future.complete(result.get(0));
               }catch (Exception e){
                   e.printStackTrace();
                   future.completeExceptionally(e);
               }
            });
        } catch (Exception ex){
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Boolean> getScheduleRouteDestinationTerminals(SQLConnection conn, Integer scheduleRouteDestinationId, JsonObject body ){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray().add(scheduleRouteDestinationId);
            conn.queryWithParams("SELECT terminal_origin_id, terminal_destiny_id FROM schedule_route_destination WHERE id = ?;", params, repply ->{
                try{
                    if (repply.failed()){
                        throw new Exception(repply.cause());
                    }
                    List<JsonObject> result = repply.result().getRows();
                    if (result.isEmpty()){
                        throw new Exception("Config destination not found");
                    }
                    Integer terminalOriginId = result.get(0).getInteger(TERMINAL_ORIGIN_ID);
                    Integer terminalDestinyId = result.get(0).getInteger("terminal_destiny_id");
                    body.put(TERMINAL_ORIGIN_ID, terminalOriginId ).put("terminal_destiny_id", terminalDestinyId);
                    future.complete(true);

                }catch (Exception e ){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }



    private CompletableFuture<Boolean> getConfigDestinationTerminals(SQLConnection conn, Integer configDestinationId, JsonObject body ){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray().add(configDestinationId);
            conn.queryWithParams("SELECT terminal_origin_id, terminal_destiny_id FROM config_destination WHERE id = ?;", params, repply ->{
                try{
                    if (repply.failed()){
                        throw new Exception(repply.cause());
                    }
                    List<JsonObject> result = repply.result().getRows();
                    if (result.isEmpty()){
                        throw new Exception("Config destination not found");
                    }
                    Integer terminalOriginId = result.get(0).getInteger(TERMINAL_ORIGIN_ID);
                    Integer terminalDestinyId = result.get(0).getInteger("terminal_destiny_id");
                    body.put(TERMINAL_ORIGIN_ID, terminalOriginId ).put("terminal_destiny_id", terminalDestinyId);
                    this.getExpiresDays().whenComplete((resultt, errorr) ->{
                        try{
                            if(errorr != null){
                                throw new Exception(errorr.getCause());
                            }
                            String daysExpired = resultt.getString("value");
                            Date currentTime = new Date();
                            Date expiresAt = UtilsDate.addDays(currentTime , Integer.parseInt(daysExpired));
                            body.put("expires_at", new Date(expiresAt.getTime()).toInstant());
                            future.complete(true);
                        }catch (Exception e){
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });
                }catch (Exception e ){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void driverFinishInitRegister(SQLConnection conn, Message<JsonObject> message, Integer boardingPassId, String prefix, JsonArray passengers,
                                    JsonArray routes, Integer underAge, Integer origin, Double distance, Integer createdBy, String reservationCode, String currentPosition , Date expiresAt){
        try {
            List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
            JsonObject principalPassenger = null;

            for (int i = 0; i < passengers.size(); i++){
                JsonObject passenger = passengers.getJsonObject(i);
                JsonArray tickets = passenger.getJsonArray("tickets");
                passenger.put("first_name","Pasajero Abordo");
                passenger.put("last_name", prefix.substring(0, 3));
                passenger.put("gender","n");
                passenger.put("need_preferential", false);
                passenger.put("birthday","2000-01-01");

                Boolean isPrincipalPassenger = passenger.getBoolean("principal_passenger");
                if (isPrincipalPassenger) {
                    principalPassenger = passenger;
                }
                pTasks.add(insertBoardingPassPassenger(conn, passenger, boardingPassId, createdBy, routes, 0, underAge, origin, distance, currentPosition, null));

            }

            if (principalPassenger == null) {
                throw new Exception("Set principal passenger");
            }

            final JsonObject finalPrincipalPassenger = principalPassenger;
            CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[passengers.size()])).whenComplete((ps, pt) -> {
                try {
                    if (pt != null){
                        throw pt;
                    }

                    int seating = passengers.size();
                    Integer principalPassengerId = finalPrincipalPassenger.getInteger(ID);
                    Double amount = 0.0;
                    Double discount = 0.0;
                    Double totalAmount = 0.0;
                    for (int i = 0; i < seating; i++) {
                        JsonObject passenger = passengers.getJsonObject(i);
                        JsonArray tickets = passenger.getJsonArray("tickets");

                        for (int j = 0; j < tickets.size(); j++) {
                            JsonObject ticket = tickets.getJsonObject(j);
                            amount += ticket.getDouble(AMOUNT);
                            discount += ticket.getDouble(DISCOUNT);
                            totalAmount += ticket.getDouble(TOTAL_AMOUNT);
                        }
                    }

                    String updateBoarding = "UPDATE boarding_pass\n"
                            + "SET principal_passenger_id=?, seatings=?, amount=?, discount=?, total_amount=?, expires_at = ?\n"
                            + "WHERE id=?;";

                    JsonArray params = new JsonArray()
                            .add(principalPassengerId).add(seating)
                            .add(amount).add(discount).add(totalAmount)
                            .add(UtilsDate.sdfDataBase(expiresAt)).add(boardingPassId);
                    conn.updateWithParams(updateBoarding, params, replyUpdate -> {
                        try {
                            if(replyUpdate.failed()) {
                                throw replyUpdate.cause();
                            }
                            JsonObject result = new JsonObject().put(ID, boardingPassId)
                                    .put(RESERVATION_CODE, reservationCode);
                            result.put("passengers", passengers);

                            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                    new JsonObject().put("fieldName", "driver_boardingpass_cancel_time"),
                                    new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), replyC -> {
                                        try {
                                            if (replyC.failed()){
                                                throw replyC.cause();
                                            }
                                            JsonObject driverCancelationTimeResult = (JsonObject) replyC.result().body();

                                            Long driverCancelationTime = Long.valueOf(driverCancelationTimeResult.getString("value"));

                                            this.vertx.setTimer(driverCancelationTime, cRegister -> remindRegister(boardingPassId));

                                            this.commit(conn, message, result);

                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
    }

    private void finishInitRegister(SQLConnection conn, Message<JsonObject> message, Integer boardingPassId, JsonArray passengers,
                                    JsonArray routes, Integer underAge, Integer origin, Integer createdBy, String reservationCode,
                                    Boolean isPhoneReservation, Date expiresAt, Integer travelDateDifference, Integer reservationTime, Integer cancelPhoneReservationTime, Integer integrationPartnerSessionId) {
        final int pLen = passengers.size();
        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
        JsonObject principalPassenger = null;
        database.boardingpass.enums.TICKET_TYPE ticketType = database.boardingpass.enums.TICKET_TYPE.values()[message.body().getInteger(TICKET_TYPE)];
        for (int i = 0; i < pLen; i++) {
            JsonObject passenger = passengers.getJsonObject(i);
            Boolean isPrincipalPassenger = passenger.getBoolean("principal_passenger");
            if (isPrincipalPassenger) {
                principalPassenger = passenger;
            }
            pTasks.add(insertBoardingPassPassenger(conn, passenger, boardingPassId, createdBy, routes, 0, underAge, origin, integrationPartnerSessionId, new JsonObject(), ticketType));
        }

        if (principalPassenger == null) {
            this.rollback(conn, new Throwable("Set principal passenger"), message);
            return;
        }

        final JsonObject finalPrincipalPassenger = principalPassenger;
        CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen])).whenComplete((ps, pt) -> {
            try {
                if (pt != null) {
                    this.rollback(conn, pt.getCause(), message);
                } else {
                    int seating = passengers.size();
                    Integer principalPassengerId = finalPrincipalPassenger.getInteger("id");
                    Double amount = 0.0;
                    Double discount = 0.0;
                    Double totalAmount = 0.0;
                    for (int i = 0; i < seating; i++) {
                        JsonObject passenger = passengers.getJsonObject(i);
                        JsonArray tickets = passenger.getJsonArray("tickets");
                        final int numTickets = tickets.size();
                        for (int j = 0; j < numTickets; j++) {
                            JsonObject ticket = tickets.getJsonObject(j);
                            amount += ticket.getDouble("amount");
                            discount += ticket.getDouble("discount");
                            totalAmount += ticket.getDouble("total_amount");
                        }
                    }

                    String updateBoarding;
                    JsonArray params = new JsonArray();

                    updateBoarding = "UPDATE boarding_pass\n"
                            + "SET principal_passenger_id=?, seatings=?, amount=?, discount=?, total_amount=?, expires_at=? \n"
                            + "WHERE id=?;";
                    params
                            .add(principalPassengerId).add(seating)
                            .add(amount).add(discount).add(totalAmount).add(UtilsDate.format_YYYY_MM_DD_HH_MM(expiresAt))
                            .add(boardingPassId);

                    conn.updateWithParams(updateBoarding, params, replyUpdate -> {
                        try {
                            if (replyUpdate.succeeded()) {
                                JsonObject result = new JsonObject().put("id", boardingPassId)
                                        .put("reservation_code", reservationCode);
                                result.put("passengers", passengers);
                                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                        new JsonObject().put("fieldName", "boarding_cancelation_time"),
                                        new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                                        replyC -> {
                                            try {
                                                if (replyC.succeeded()){
                                                    JsonObject cancelationTimeResult = (JsonObject) replyC.result().body();
                                                    Long cancelationTime;
                                                    if(isPhoneReservation && travelDateDifference > reservationTime)
                                                        cancelationTime = Long.valueOf(reservationTime) * 60 * 60 * 1000;
                                                    else if(isPhoneReservation && travelDateDifference <= reservationTime)
                                                        cancelationTime = (Long.valueOf(travelDateDifference - cancelPhoneReservationTime)) * 60 * 60 * 1000;
                                                    else if (Objects.nonNull(integrationPartnerSessionId))
                                                        cancelationTime = (long) (2 * 60 * 60 * 1000);
                                                    else cancelationTime = Long.valueOf(cancelationTimeResult.getString("value"));
                                                    // Verify purchase origin to set cancel or remind to the user (sucursal = 1, web = 2)

                                                    if (origin.equals(2)) { // Purchase origin: 2 (web)
                                                        System.out.println("Sending remind email: ".concat(Integer.toString(boardingPassId)));
                                                        this.vertx.setTimer(cancelationTime, cRegister -> remindRegister(boardingPassId));
                                                    } else {
                                                        System.out.println("Canceling reservation ".concat(Integer.toString(boardingPassId))
                                                                .concat(" in ").concat(Long.toString(cancelationTime))
                                                                .concat(" miliseconds"));
                                                        this.vertx.setTimer(cancelationTime, cRegister -> cancelRegister(boardingPassId));
                                                    }
                                                    if(isPhoneReservation) {
                                                        this.vertx.setTimer(100, cEmail -> this.reservationConfirmationEmailRegister(boardingPassId, Integer.valueOf(cancelationTimeResult.getString("value"))));
                                                    }

                                                    if (Objects.nonNull(integrationPartnerSessionId)) {
                                                        JsonArray deleteSeatLocksParams = new JsonArray().add(integrationPartnerSessionId);
                                                        conn.updateWithParams(QUERY_DELETE_LOCKED_SEATS_BY_SESSION_ID, deleteSeatLocksParams, replyDeleteLockedSeats -> {
                                                            try {
                                                                if (replyDeleteLockedSeats.failed()) {
                                                                    this.rollback(conn, replyDeleteLockedSeats.cause(), message);
                                                                    return;
                                                                }

                                                                Integer seatLocksDeleted = replyDeleteLockedSeats.result().getUpdated();
                                                                System.out.println("Deleted seat locks: ".concat(String.valueOf(seatLocksDeleted)));

                                                                this.commit(conn, message, result);

                                                            } catch (Throwable throwable) {
                                                                throwable.printStackTrace();
                                                                this.rollback(conn, throwable, message);
                                                            }
                                                        });
                                                    } else {
                                                        this.commit(conn, message, result);
                                                    }
                                                } else {
                                                    this.rollback(conn, replyC.cause(), message);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                this.rollback(conn, e, message);
                                            }
                                        });
                            } else {
                                this.rollback(conn, replyUpdate.cause(), message);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            this.rollback(conn, e, message);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.rollback(conn, e, message);
            }
        });
    }

    private void cancelRegister(Integer boardingPassId){
        this.getPaymentStatus(boardingPassId).whenComplete((boardingpassStatus, errorP) -> {
            if (errorP != null){
                System.out.println("TimerCancelRegister by boarding_pass_id = "+boardingPassId+" - "+errorP.getMessage());
            } else if (boardingpassStatus == 4){
                this.dbClient.queryWithParams(QUERY_CRON_CANCEL_RESERVATION, new JsonArray().add(boardingPassId), reply -> {
                    if (reply.succeeded()){
                        System.out.println("TimerCancelRegister by boarding_pass_id = "+boardingPassId+" - BoardingPass canceled");
                    } else {
                        System.out.println("TimerCancelRegister by boarding_pass_id = "+boardingPassId+" - "+reply.cause().getMessage());
                    }
                });
            } else {
                System.out.println("TimerCancelRegister by boarding_pass_id = "+boardingPassId+" - BoardingPass is not preboarding (status = " + boardingpassStatus + ")");
            }
        });
    }

    private void remindRegister(Integer boardingPassId) {
        System.out.println("Try to send remind email: ".concat(Integer.toString(boardingPassId)));
        this.getPaymentStatus(boardingPassId).whenComplete((paymentStatus, errorPayment) -> {
            try {
                System.out.println("Getting payment status for: ".concat(Integer.toString(boardingPassId))
                        .concat(" | Payment status -> ").concat(Integer.toString(paymentStatus)));
                if (errorPayment != null) {
                    showRemindEmailError(boardingPassId, errorPayment.getMessage());
                } else if (paymentStatus != 1) {
                    JsonArray params = new JsonArray().add(boardingPassId);
                    this.dbClient.queryWithParams(QUERY_GET_CUSTOMER_TO_REMIND_BOARDING_PASS_PAYMENT, params, reply -> {
                        try {
                            if (reply.failed()) {
                                showRemindEmailError(boardingPassId, reply.cause().getMessage());
                            } else {
                                List<JsonObject> results = reply.result().getRows();
                                if (results.isEmpty()) {
                                    showRemindEmailError(boardingPassId, "Customer not found");
                                } else {
                                    this.sendRemindEmail(results.get(0), boardingPassId);
                                }
                            }
                        } catch (Exception e) {
                            showRemindEmailError(boardingPassId, e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                showRemindEmailError(boardingPassId, e.getMessage());
            }
        });
    }

    private void reservationConfirmationEmailRegister(Integer boardingPassId, Integer reservationTime) {
        this.sendBoardingPassPhoneEmail(boardingPassId, reservationTime).setHandler(reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                JsonObject detail = reply.result();
                String email = (String) detail.remove("passenger_email");
                String customerEmail = (String) detail.remove("customer_email");
                email = email != null ? email : customerEmail;
                JsonObject body = new JsonObject()
                        .put("template", "boardingpass_reservation.html")
                        .put("subject", "AllAbordo | Reservacón de boletos")
                        .put("to", email)
                        .put("body", detail);
                DeliveryOptions options = new DeliveryOptions()
                        .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);
                this.vertx.eventBus().send(MailVerticle.class.getSimpleName(), body, options, replySend -> {
                    if (replySend.failed()) {
                        replySend.cause().printStackTrace();
                    }
                });
            }catch (Exception ex){
                ex.printStackTrace();
                reply.cause().printStackTrace();
            }
        });
    }

    private Future<JsonObject> sendBoardingPassPhoneEmail(Integer boardingPassId, Integer reservationTime) {

        Future<JsonObject> future = Future.future();

        Future<ResultSet> detailFuture = Future.future();
        Future<ResultSet> passengersFuture = Future.future();
        Future<ResultSet> ticketsFuture = Future.future();
        Future<ResultSet> routesFuture = Future.future();

        JsonArray params = new JsonArray().add(boardingPassId);

        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_DETAIL, params, detailFuture.completer());
        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_PASSENGERS, params, passengersFuture.completer());
        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_TICKETS, params, ticketsFuture.completer());
        this.dbClient.queryWithParams(QUERY_EMAIL_BOARDING_PASS_ROUTES, params, routesFuture.completer());

        CompositeFuture.all(detailFuture, passengersFuture, ticketsFuture, routesFuture).setHandler(reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }

                ResultSet rsDetail = reply.result().resultAt(0);
                ResultSet rsPassengers = reply.result().resultAt(1);
                ResultSet rsTickets = reply.result().resultAt(2);
                ResultSet rsRoutes = reply.result().resultAt(3);

                JsonObject detail = rsDetail.getRows().get(0);

                // Set customer name
                String customer = (String) detail.remove("customer_fullname");
                String passenger = (String) detail.remove("passenger_fullname");
                customer = customer != null ? customer : passenger;
                detail.put("customer", customer);

                // Set date
                String createdAt = (String) detail.remove("created_at");
                Date date = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt);
                Date cancelAt = UtilsDate.addHours(date, reservationTime);
                detail.put("date", UtilsDate.format_D_MM_YYYY(date))
                        .put("hour", UtilsDate.format_HH_MM(date))
                        .put("cancel_date", UtilsDate.format_D_MM_YYYY(cancelAt))
                        .put("cancel_hour", UtilsDate.format_HH_MM(cancelAt));

                // Set passengers
                List<JsonObject> passengers = rsPassengers.getRows();
                List<JsonObject> tickets = rsTickets.getRows();
                for (JsonObject p: passengers) {
                    for (JsonObject t: tickets) {
                        Integer passengerId = p.getInteger("id");
                        Integer ticketPassengerId = t.getInteger("boarding_pass_passenger_id");
                        String ticketTypeRoute = t.getString("ticket_type_route");
                        if (passengerId.equals(ticketPassengerId)) {
                            String key = ticketTypeRoute.equals("ida") ? "departure_seat" : "return_seat";
                            String seat = t.getString("seat");
                            p.put(key, seat);
                        }
                    }
                }

                // Set routes
                List<JsonObject> routes = rsRoutes.getRows();

                for (JsonObject route: routes) {
                    Date travelDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss((String) route.remove("travel_date"));
                    Date arrivalDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss((String) route.remove("arrival_date"));
                    route.put("departure_date", UtilsDate.format_D_MM_YYYY(travelDate));
                    route.put("departure_hour", UtilsDate.format_HH_MM(travelDate));
                    route.put("arrival_date", UtilsDate.format_D_MM_YYYY(arrivalDate));
                    route.put("arrival_hour", UtilsDate.format_HH_MM(arrivalDate));

                    String travelType = (String) route.remove("type_travel");
                    route.put("travel_type", travelType.equals("0") ? "Express" :
                            travelType.equals("1") ? "Directo" :
                                    "Ordinario");
                }

                // Set payments
                Double subtotal = UtilsMoney.round((Double) detail.remove(AMOUNT), 2);
                Double discount = UtilsMoney.round((Double) detail.remove(DISCOUNT), 2);
                Double totalAmount = UtilsMoney.round((Double) detail.remove(TOTAL_AMOUNT), 2);
                JsonArray paymentConcepts = new JsonArray()
                        .add(new JsonObject()
                                .put("name", "Subtotal")
                                .put("amount", String.valueOf(subtotal)))
                        .add(new JsonObject()
                                .put("name", "Descuento")
                                .put("amount", String.valueOf(discount)))
                        .add(new JsonObject()
                                .put("name", "Importe total del viaje")
                                .put("amount", String.valueOf(totalAmount)));

                JsonObject result = detail.copy()
                        .put("passengers", passengers)
                        .put("routes", routes)
                        .put("payment_concepts", paymentConcepts);

                future.complete(result);

            } catch (Exception e) {
                e.printStackTrace();
                future.fail(e);
            }
        });

        return future;
    }

    private static String QUERY_GET_CUSTOMER_TO_REMIND_BOARDING_PASS_PAYMENT = "SELECT customer.*, \n" +
            "boarding_pass.reservation_code \n" +
            "FROM boarding_pass \n" +
            "JOIN customer ON customer.id=boarding_pass.customer_id \n" +
            "WHERE boarding_pass.id=?;";

    private void showRemindEmailError(Integer boardingPassId, String message) {
        System.out.println("Remind register -> ID: ".concat(Integer.toString(boardingPassId)).concat(" -> ").concat(message));
    }

    private void sendRemindEmail(JsonObject customer, Integer boardingPassId) {
        try {
            String email = customer.getString("email");
            String reservationCode = customer.getString("reservation_code");
            String fullName = customer.getString("first_name").concat(" ")
                    .concat(customer.getString("last_name"));
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);

            this.vertx.eventBus().send(MailVerticle.class.getSimpleName(),  new JsonObject()
                    .put("template", "remindemail.html")
                    .put("to", email)
                    .put("subject", "Termina la compra de tu(s) boleto(s) en allAbordo")
                    .put("body", new JsonObject()
                            .put("name", fullName)
                            .put("link", config().getString(Constants.WEB_SERVER_HOSTNAME)
                                    .concat("/compraboleto?code=").concat(reservationCode))
                    ), options, replySend -> {
                                try {
                                    if (replySend.failed()) {
                                        showRemindEmailError(boardingPassId, replySend.cause().getMessage());
                                    } else {
                                        System.out.println("Remind register SEND to: ".concat(email)
                                                .concat(" ->  ID: ".concat(Integer.toString(boardingPassId))));
                                    }
                                } catch (Exception e) {
                                    showRemindEmailError(boardingPassId, e.getMessage());
                                }
            });
        } catch (Exception e) {
            showRemindEmailError(boardingPassId, e.getMessage());
        }


    }

    private static final String QUERY_CRON_CANCEL_RESERVATION = "UPDATE boarding_pass SET boardingpass_status = 0 WHERE id = ?";

    private CompletableFuture<Integer> getPaymentStatus(Integer boardingPassId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PAYMENT_STATUS_BY_BOARDINGPASS_ID, new JsonArray().add(boardingPassId), reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                Integer paymentStatus = reply.result().getRows().get(0).getInteger("boardingpass_status");
                future.complete(paymentStatus);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private static final String QUERY_GET_PAYMENT_STATUS_BY_BOARDINGPASS_ID = "SELECT boardingpass_status FROM boarding_pass WHERE id = ?;";

    private CompletableFuture<JsonObject> insertBoardingPassRoute(SQLConnection conn, JsonObject route, Integer boardingPassId, Integer createdBy, Integer scheduleRouteDestinationId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        route.put("boarding_pass_id", boardingPassId);
        route.put("created_by", createdBy);
        if(scheduleRouteDestinationId == null){
            route.put("route_status" , 4);
        }

        String insert = this.generateGenericCreate("boarding_pass_route", route);
        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try {
                if (reply.succeeded()) {
                    final int id = reply.result().getKeys().getInteger(0);
                    route.put("id", id);
                    future.complete(route);
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> insertBoardingPassPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer createdBy, JsonArray routes, Integer passengerParentId, Integer underAge, Integer origin, Integer integrationPartnerSessionId, JsonObject prepaidInfo, TICKET_TYPE ticketType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {

            JsonArray tickets = (JsonArray) passenger.remove("tickets");
            Boolean isChild = passengerParentId != 0;
            passenger.put("boarding_pass_id", boardingPassId);
            passenger.put("created_by", createdBy);
            String birth = passenger.getString("birthday");

            // Copy tickets to child object
            JsonObject child = (JsonObject) passenger.remove("child");
            if (child != null) {
                child.put("tickets", tickets.copy());
            }

            // Add parent id to this passenger and is child under age
            if (passengerParentId > 0) {
                try {
                    Date birthday = UtilsDate.parse_yyyy_MM_dd(birth);
                    LocalDate d = LocalDate.of(1900+birthday.getYear(), birthday.getMonth()+1, birthday.getDate());
                    Integer years = UtilsDate.calculateAge(d);
                    if (years <= underAge) {
                        passenger.put("parent_id", passengerParentId);
                        passenger.put("is_child_under_age", true);
                        saveBoardingPassPassenger(conn, passenger, boardingPassId, tickets, routes, child, createdBy, true, underAge, origin, integrationPartnerSessionId, prepaidInfo, ticketType)
                                .whenComplete((s, t) -> {
                                    try {
                                        if (t != null) {
                                            future.completeExceptionally(t);
                                        } else {
                                            future.complete(passenger);
                                        }
                                    } catch (Exception e) {
                                        future.completeExceptionally(e);
                                    }
                                });
                    } else {
                        future.completeExceptionally(new Throwable("The child is not under age ".concat(underAge.toString()).concat(" years old")));
                    }

                } catch (ParseException e) {
                    future.completeExceptionally(new Throwable("Birthday: invalid format ".concat(birth)));
                }

            } else {
                saveBoardingPassPassenger(conn, passenger, boardingPassId, tickets, routes, child, createdBy, isChild, underAge, origin, integrationPartnerSessionId, prepaidInfo, ticketType)
                        .whenComplete((s, t) -> {
                            try {
                                if (t != null) {
                                    future.completeExceptionally(t);
                                } else {
                                    future.complete(passenger);
                                }
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertBoardingPassPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer createdBy, JsonArray routes, Integer passengerParentId, Integer underAge, Integer origin, Double distance, String currentPosition, Integer integrationParnterSessionId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray tickets = (JsonArray) passenger.remove("tickets");
        Boolean isChild = passengerParentId != 0;
        passenger.put("boarding_pass_id", boardingPassId);
        passenger.put(CREATED_BY, createdBy);
        String birth = passenger.getString("birthday");

        // Copy tickets to child object
        JsonObject child = (JsonObject) passenger.remove("child");
        if (child != null) {
            child.put("tickets", tickets.copy());
        }

        // Add parent id to this passenger and is child under age
        if (passengerParentId > 0) {
            try {
                Date birthday = UtilsDate.parse_yyyy_MM_dd(birth);
                LocalDate d = LocalDate.of(1900+birthday.getYear(), birthday.getMonth()+1, birthday.getDate());
                Integer years = UtilsDate.calculateAge(d);
                if (years <= underAge) {
                    passenger.put("parent_id", passengerParentId);
                    passenger.put("is_child_under_age", true);
                    this.saveBoardingPassPassenger(conn, passenger, boardingPassId, tickets, routes, child, createdBy, true, underAge, origin, distance, currentPosition, integrationParnterSessionId)
                            .whenComplete((s, t) -> {
                                if (t != null) {
                                    future.completeExceptionally(t);
                                } else {
                                    future.complete(passenger);
                                }
                            });
                } else {
                    future.completeExceptionally(new Throwable("The child is not under age ".concat(underAge.toString()).concat(" years old")));
                }

            } catch (ParseException e) {
                future.completeExceptionally(new Throwable("Birthday: invalid format ".concat(birth)));
            }

        } else {
            this.saveBoardingPassPassenger(conn, passenger, boardingPassId, tickets, routes, child, createdBy, isChild, underAge, origin,distance, currentPosition, integrationParnterSessionId)
                    .whenComplete((s, t) -> {
                        if (t != null) {
                            future.completeExceptionally(t);
                        } else {
                            future.complete(passenger);
                        }
                    });
        }


        return future;
    }

    private CompletableFuture<JsonObject> saveBoardingPassPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, JsonArray tickets, JsonArray routes, JsonObject child, Integer createdBy, Boolean isChild, Integer underAge, Integer origin, Integer integrationPartnerSessionId, JsonObject prepaidInfo, TICKET_TYPE ticketType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String hash = (String) passenger.remove("hash");
        Boolean isPrepaid = passenger.containsKey("prepaid") ? passenger.getBoolean("prepaid") : false;
        if(isPrepaid != null) passenger.remove("prepaid");

        String insert = this.generateGenericCreate("boarding_pass_passenger", passenger);

        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                final int boardingPassPassengerId = reply.result().getKeys().getInteger(0);
                final int specialTicketId = passenger.getInteger("special_ticket_id");
                passenger.clear();

                passenger.put("id", boardingPassPassengerId)
                        .put("hash", hash).put("tickets", tickets);
                final int iLen = tickets.size();
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for (int i = 0; i < iLen; i++) {
                    JsonObject ticket = tickets.getJsonObject(i);

                    Integer scheduleRouteDestinationId = ticket.getInteger("schedule_route_destination_id");
                    Integer configDestinationId = ticket.getInteger("config_destination_id");
                    Integer boardingPassRouteId = null;
                    String ticketTypeRouteEnum = routes.getJsonObject(i).getInteger(Constants.TICKET_TYPE_ROUTE) == 1 ? "ida" : "regreso";
                    ticket.put(Constants.TICKET_TYPE_ROUTE, ticketTypeRouteEnum);
                    for (int j = 0, jLen = routes.size(); j < jLen; j++) {
                        JsonObject route = routes.getJsonObject(j);
                        Integer x = route.getInteger("schedule_route_destination_id");
                        if ( route.containsKey(SCHEDULE_ROUTE_DESTINATION_ID) &&  scheduleRouteDestinationId != null && scheduleRouteDestinationId.equals(x)) {
                            boardingPassRouteId = route.getInteger("id");
                            break;
                        }
                        Integer y = route.getInteger("config_destination_id");
                        if ( route.containsKey(CONFIG_DESTINATION_ID) &&  configDestinationId != null && configDestinationId.equals(y)) {
                            boardingPassRouteId = route.getInteger("id");
                            break;
                        }
                    }

                    if (scheduleRouteDestinationId != null &&  boardingPassRouteId == null) {
                        future.completeExceptionally(new Throwable("Invalid schedule route destination id for ticket object"));
                        return;
                    } else {
                        tasks.add(insertBoardingPassTicket(conn, ticket, boardingPassPassengerId, boardingPassRouteId, specialTicketId, createdBy, isChild, origin, integrationPartnerSessionId, prepaidInfo, ticketType));
                    }
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[iLen])).whenComplete((s, t) -> {
                    if (t != null) {
                        future.completeExceptionally(t.getCause());
                    } else {
                        if (child != null) {
                            insertBoardingPassPassenger(conn, child, boardingPassId, createdBy, routes, boardingPassPassengerId, underAge, origin, integrationPartnerSessionId, prepaidInfo, ticketType)
                                    .whenComplete((cs, st) -> {
                                        if (st != null) {
                                            future.completeExceptionally(st);
                                        } else {
                                            passenger.put("child", child);
                                            future.complete(passenger);
                                        }
                                    });
                        } else {
                            future.complete(passenger);
                        }
                    }
                });

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> saveBoardingPassPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, JsonArray tickets, JsonArray routes, JsonObject child, Integer createdBy, Boolean isChild, Integer underAge, Integer origin, Double distance, String currentPosition, Integer integrationPartnerSessionId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String hash = (String) passenger.remove("hash");

        String insert = this.generateGenericCreate("boarding_pass_passenger", passenger);

        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }

                final int boardingPassPassengerId = reply.result().getKeys().getInteger(0);
                final int specialTicketId = passenger.getInteger("special_ticket_id");
                passenger.clear();
                passenger.put(ID, boardingPassPassengerId)
                        .put("hash", hash).put("tickets", tickets);
                final int iLen = tickets.size();
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for (int i = 0; i < iLen; i++) {
                    JsonObject ticket = tickets.getJsonObject(i);

                    Integer scheduleRouteDestinationId = ticket.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
                    Integer boardingPassRouteId = null;
                    for (int j = 0, jLen = routes.size(); j < jLen; j++) {
                        JsonObject route = routes.getJsonObject(j);
                        int x = route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
                        if (scheduleRouteDestinationId.equals(x)) {
                            boardingPassRouteId = route.getInteger(ID);
                            break;
                        }
                    }

                    if (boardingPassRouteId == null) {
                        future.completeExceptionally(new Throwable("Invalid schedule route destination id for ticket object"));
                        return;
                    } else {
                        tasks.add(insertBoardingPassTicket(conn, ticket, boardingPassPassengerId, boardingPassRouteId, specialTicketId, createdBy, isChild, currentPosition, origin, integrationPartnerSessionId));
                    }
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[iLen])).whenComplete((s, t) -> {
                    if (t != null) {
                        future.completeExceptionally(t.getCause());
                    } else {
                        if (child != null) {
                            insertBoardingPassPassenger(conn, child, boardingPassId, createdBy, routes, boardingPassPassengerId, underAge,origin, distance, currentPosition, integrationPartnerSessionId)
                                    .whenComplete((cs, st) -> {
                                        if (st != null) {
                                            future.completeExceptionally(st);
                                        } else {
                                            passenger.put("child", child);
                                            future.complete(passenger);
                                        }
                                    });
                        } else {
                            future.complete(passenger);
                        }
                    }
                });
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> insertBoardingPassTicket(SQLConnection conn, JsonObject ticket, Integer boardingPassPassengerId, Integer boardingPassRouteId, Integer specialTicketId, Integer createdBy, Boolean isChild, Integer origin, Integer integrationPartnerSessionId, JsonObject prepaidInfo, TICKET_TYPE ticketType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String seat = ticket.getString("seat");
        Integer configDestinationId = ticket.getInteger(CONFIG_DESTINATION_ID);
        Integer scheduleRouteDestinationId = (Integer) ticket.remove("schedule_route_destination_id");
        searchSeat(scheduleRouteDestinationId, seat, integrationPartnerSessionId).whenComplete((s, t) -> {
            try{
                if (t != null) {
                    throw new Exception(t.getCause());
                }
                String QUERY;
                JsonArray params = new JsonArray();

                if(scheduleRouteDestinationId == null){
                    QUERY = QUERY_CONFIG_TICKET_PRICE_BY_CONFIG_ROUTE_ID;
                    params.add(configDestinationId);
                    params.add(specialTicketId);
                }else{
                    QUERY = QUERY_CONFIG_TICKET_PRICE_BY_DESTINATION_V2;
                    params.add(scheduleRouteDestinationId);
                    params.add(specialTicketId);
                }
                conn.queryWithParams(QUERY, params, (AsyncResult<ResultSet> replyPrices) -> {
                    try{
                        if(replyPrices.failed()) {
                            throw new Exception(replyPrices.cause());
                        }
                        List<JsonObject> results = replyPrices.result().getRows();
                        if (results.isEmpty()) {
                            throw new Exception("Config ticket price not found for schedule destination " + scheduleRouteDestinationId);
                        }
                        JsonObject bodyApplyTicketPriceRule = results.get(0)
                                .put(TICKET_TYPE, ticketType)
                                .put(Constants.TICKET_TYPE_ROUTE, database.boardingpass.enums.TICKET_TYPE_ROUTE.valueOf((String) ticket.remove(TICKET_TYPE_ROUTE)));

                        vertx.eventBus().send(TicketPricesRulesDBV.class.getSimpleName(), bodyApplyTicketPriceRule, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_TICKET_PRICE_RULE), replyC -> {
                            try {
                                if (replyC.failed()){
                                    throw replyC.cause();
                                }
                                JsonObject price = (JsonObject) replyC.result().body();
                                if (isChild) {
                                    price.mergeIn(new JsonObject()
                                            .put("amount", 0)
                                            .put("discount", 0)
                                            .put("total_amount", 0)
                                            .put("seat", ""));
                                }
                                if (!prepaidInfo.isEmpty()) {
                                    // is from a prepaid travel
                                    Double prepaid_amount = 0.0;
                                    Double prepaid_extra_charges = 0.0;
                                    Double prepaid_discount = 0.0;
                                    double prepaid_total_amount = 0.0;

                                    if(prepaidInfo.containsKey("is_inside_prepaid_range") &&
                                            !prepaidInfo.getBoolean("is_inside_prepaid_range")) {
                                        // prepaid_amount = price.getDouble("amount") - prepaidInfo.getDouble("max_distance_base_price");
                                        prepaid_total_amount = price.getDouble("total_amount") - prepaidInfo.getDouble("max_distance_base_price");
                                    }
                                    price.mergeIn(new JsonObject()
                                            .put("amount", prepaid_total_amount)
                                            .put("extra_charges", prepaid_extra_charges)
                                            .put("discount", prepaid_discount)
                                            .put("total_amount", prepaid_total_amount));
                                }
                                ticket.put("boarding_pass_passenger_id", boardingPassPassengerId);
                                ticket.put("boarding_pass_route_id", boardingPassRouteId);
                                ticket.put("created_by", createdBy);
                                ticket.put("tracking_code",UtilsID.generateID("S"));
                                ticket.put("price_ticket", price.getDouble("total_amount"));
                                Integer configDestId = ticket.getInteger(CONFIG_DESTINATION_ID);
                                if(ticket.containsKey(CONFIG_DESTINATION_ID) && configDestId != null){
                                    ticket.put("seat", "");
                                }
                                ticket.remove(CONFIG_DESTINATION_ID);
                                ticket.remove("special_ticket_id");
                                price.remove("special_ticket_id");

                                GenericQuery insert = this.generateGenericCreateSendTableName("boarding_pass_ticket", ticket.mergeIn(price));
                                conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                    if (reply.succeeded()) {
                                        final int boardingPassTicketId = reply.result().getKeys().getInteger(0);
                                        ticket.clear();
                                        ticket.mergeIn(price);
                                        ticket.put("id", boardingPassTicketId)
                                                .put("schedule_route_destination_id", scheduleRouteDestinationId)
                                                .put("config_destination_id",configDestId);
                                        future.complete(ticket);
                                    } else {
                                        future.completeExceptionally(reply.cause());
                                    }
                                });
                            } catch (Throwable tr) {
                                future.completeExceptionally(tr);
                            }
                        });
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
            }catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(t);
            }

        });

        return future;
    }

    private CompletableFuture<JsonObject> insertBoardingPassTicket(SQLConnection conn, JsonObject ticket, Integer boardingPassPassengerId, Integer boardingPassRouteId, Integer specialTicketId, Integer createdBy, Boolean isChild, String currentPosition, Integer origin, Integer integrationPartnerSessionId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        ticket.put("boarding_pass_passenger_id", boardingPassPassengerId);
        ticket.put("boarding_pass_route_id", boardingPassRouteId);
        ticket.put(CREATED_BY, createdBy);
        ticket.put("tracking_code",UtilsID.generateID("S"));
        String seat = ticket.getString(SEAT);
        Integer scheduleRouteDestinationId = (Integer) ticket.remove(SCHEDULE_ROUTE_DESTINATION_ID);

        this.searchSeat(scheduleRouteDestinationId, seat, integrationPartnerSessionId).whenComplete((resultSearchSeat, errorSearchSeat) -> {
            try {
                if (errorSearchSeat != null) {
                    throw errorSearchSeat;
                }

                DeliveryOptions options = new DeliveryOptions()
                        .addHeader(ACTION, ScheduleRouteDBV.ACTION_DRIVER_CALCULATE_TICKET_PRICE);
                JsonObject bodyDriverCalculateTicketPrice = new JsonObject()
                        .put(CURRENT_POSITION, currentPosition)
                        .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId);
                vertx.eventBus().send(ScheduleRouteDBV.class.getSimpleName(), bodyDriverCalculateTicketPrice, options, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        JsonObject resultTicketPrices = (JsonObject) reply.result().body();
                        JsonArray prices = resultTicketPrices.getJsonArray(PRICES);

                        for (int i = 0; i < prices.size(); i ++){
                            JsonObject price = prices.getJsonObject(i);
                            if (price.getInteger("special_ticket_id").equals(specialTicketId)){

                                Double totalAmount = price.getDouble(TOTAL_AMOUNT);
                                Integer configTicketPriceId = price.getInteger(CONFIG_TICKET_PRICE_ID);

                                ticket.put(AMOUNT, totalAmount)
                                        .put(DISCOUNT, 0.00)
                                        .put(TOTAL_AMOUNT, totalAmount)
                                        .put(CONFIG_TICKET_PRICE_ID, configTicketPriceId)
                                        .put(SEAT, seat);
                                break;
                            }
                        }

                        if (isChild) {
                            ticket.put(AMOUNT, 0)
                                  .put(DISCOUNT, 0)
                                  .put(TOTAL_AMOUNT, 0)
                                  .put(SEAT, "");
                        }

                        String insert = this.generateGenericCreate("boarding_pass_ticket", ticket);
                        conn.update(insert, (AsyncResult<UpdateResult> replyInsert) -> {
                            try{
                                if(replyInsert.failed()) {
                                    throw replyInsert.cause();
                                }
                                final int boardingPassTicketId = replyInsert.result().getKeys().getInteger(0);

                                ticket.put("boarding_pass_ticket_id", boardingPassTicketId);

                                future.complete(ticket);
                            }catch (Throwable t) {
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
        });

        return future;
    }

    /**
     * INIT REGISTER METHODS <!- END -!>
     */



    /**
     * END REGISTER METHODS <!- START -!>
     */

    private void endCalculate(Message<JsonObject> message){
        this.driverEndRegister(message,false);
    }

    private void endMake(Message<JsonObject> message){
        this.driverEndRegister(message,true);
    }

    private void driverEndRegister(Message<JsonObject> message, Boolean persist){
        this.startTransaction(message,(SQLConnection conn)->{
            try {
                JsonObject body = message.body();

                final int boardingPassId = body.getInteger(ID);
                final int createdBy = body.getInteger(CREATED_BY);
                Integer cashOutId = body.containsKey(CASHOUT_ID) ? (Integer) body.remove(CASHOUT_ID) : null;

                JsonArray passengers = (JsonArray) body.remove("passengers");
                JsonArray payments = (JsonArray) body.remove("payments");
                JsonObject cashChange = (JsonObject) body.remove("cash_change");

                this.getBoardingPassById(conn,boardingPassId).whenComplete((boardingPass,err)->{
                    try{
                        if(err!=null){
                            throw err;
                        }

                        Integer boardingPassStatus = boardingPass.getInteger(BOARDINGPASS_STATUS);
                        String reservationCode = boardingPass.getString(RESERVATION_CODE);
                        Integer shipmentId = boardingPass.getInteger(SHIPMENT_ID);

                        if (this.validateBoardingPassStatusRegister(boardingPassStatus)){

                            this.getTotalAmountTicketsForBoardingPassById(conn,boardingPassId).whenComplete((totalTickets,ticketsErr)->{
                                try{
                                    if (ticketsErr != null){
                                        throw ticketsErr;
                                    }

                                    final Double amount = totalTickets.getDouble(AMOUNT);
                                    final Double discount = totalTickets.getDouble(DISCOUNT);
                                    final Double totalAmount = totalTickets.getDouble(TOTAL_AMOUNT);

                                    final int len = passengers.size();
                                    List<CompletableFuture<JsonObject>> tasks = IntStream.range(0, len)
                                            .mapToObj(passengers::getJsonObject)
                                            .map(passenger -> checkInPassenger(conn, passenger, boardingPassId, createdBy, persist, true, shipmentId))
                                            .collect(Collectors.toList());

                                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len])).whenComplete((resultcheckInPassenger, errorcheckInPassenger)->{
                                        try{
                                            if(errorcheckInPassenger != null){
                                                throw errorcheckInPassenger;
                                            }
                                            // Calculate total for complements
                                            Double extraCharges = 0.0;
                                            Integer extraBaggage = 0;
                                            JsonArray complementTrackingCodes = new JsonArray();

                                            for (int i = 0; i < len; i++) {
                                                JsonObject passenger = passengers.getJsonObject(i);
                                                extraBaggage += passenger.getInteger("extra_baggage");
                                                extraCharges += passenger.getDouble(EXTRA_CHARGES);
                                                JsonArray complementsPassenger = passenger.getJsonArray("complements");
                                                complementsPassenger.forEach(c -> {
                                                    JsonObject complement = (JsonObject) c;
                                                    complement.remove("complement_id");
                                                    complement.remove("boarding_pass_id");
                                                    complement.remove("created_by");
                                                    complement.remove("tracking_code");
                                                    complement.remove("linear_volume");
                                                    complement.remove("weight");
                                                    complement.remove("number");

                                                });
                                                JsonObject passengerObj = new JsonObject()
                                                        .put(ID, passenger.getInteger(ID))
                                                        .put("complements", passenger.getJsonArray("complements"));

                                                complementTrackingCodes.add(passengerObj);
                                            }
                                            // Get total previous payments
                                            Double finalExtraCharges = extraCharges;
                                            Integer finalExtraBaggage = extraBaggage;

                                            if(persist){
                                                final int currencyId = body.getInteger(CURRENCY_ID);
                                                final double ivaPercent = body.getInteger("iva_percent");

                                                Double totalPayments = 0.0;
                                                final int pLen = payments.size();
                                                for (int i = 0; i < pLen; i++) {
                                                    JsonObject payment = payments.getJsonObject(i);
                                                    Double paymentAmount = payment.getDouble(AMOUNT);
                                                    if (paymentAmount == null || paymentAmount < 0.0) {
                                                        this.rollback(conn, new Throwable("Invalid payment amount: " + paymentAmount), message);
                                                        return;
                                                    }
                                                    totalPayments += paymentAmount;
                                                }

                                                final Double innerAmount = amount + finalExtraCharges;
                                                final Double innerTotalAmount = UtilsMoney.round(totalAmount + finalExtraCharges, 2);

                                                if (totalPayments > innerTotalAmount) {
                                                    throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
                                                }
                                                if (totalPayments < innerTotalAmount) {
                                                    throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
                                                }

                                                final Double finalIva = this.getIva(innerTotalAmount, ivaPercent);

                                                GenericQuery update = this.generateGenericUpdate("boarding_pass", new JsonObject()
                                                        .put(ID, boardingPassId)
                                                        .put(BOARDINGPASS_STATUS, 2)
                                                        .put(AMOUNT, innerAmount)
                                                        .put(DISCOUNT, discount)
                                                        .put(IVA, finalIva)
                                                        .put(TOTAL_AMOUNT, innerTotalAmount)
                                                        .put("payback", 0.0)
                                                        .put(UPDATED_BY, createdBy)
                                                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date())));
                                                conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                    try{
                                                        if(replyUpdate.failed()) {
                                                            throw replyUpdate.cause();
                                                        }

                                                        this.insertTicket(conn,"purchase", boardingPassId, cashOutId, innerTotalAmount, cashChange, createdBy, ivaPercent, null, finalExtraCharges).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                            try {
                                                                if (ticketError != null){
                                                                    throw ticketError;
                                                                }

                                                                Integer ticketId = ticket.getInteger(ID);

                                                                this.insertTicketDetail(conn, boardingPassId, ticketId, true,finalExtraCharges, finalExtraBaggage, "purchase", createdBy).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                    try {
                                                                        if(dError != null){
                                                                            throw dError;
                                                                        }

                                                                        // insert payments
                                                                        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                                        for (int i = 0; i < pLen; i++) {
                                                                            JsonObject payment = payments.getJsonObject(i);
                                                                            payment.put("ticket_id", ticketId);
                                                                            pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId, cashOutId, createdBy));
                                                                        }
                                                                        CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));
                                                                        allPayments.whenComplete((ps, pt) -> {
                                                                            try {
                                                                                if (pt != null){
                                                                                    throw pt;
                                                                                }

                                                                                conn.updateWithParams(QUERY_UPDATE_BOARDINGPASS_ROUTE_INTRANSIT, new JsonArray().add(boardingPassId), replyUpdateBPR -> {
                                                                                    try {
                                                                                        if (replyUpdateBPR.failed()){
                                                                                            throw replyUpdateBPR.cause();
                                                                                        }

                                                                                        JsonObject finalResult = new JsonObject()
                                                                                                .put("reservation_code", reservationCode)
                                                                                                .put("ticket_id", ticketId)
                                                                                                .put("payback_money", 0.00)
                                                                                                .put("payback_points", 0.00)
                                                                                                .put("passengers", complementTrackingCodes);
                                                                                        this.commit(conn, message, finalResult);

                                                                                    } catch (Throwable t){
                                                                                        t.printStackTrace();
                                                                                        this.rollback(conn, t, message);
                                                                                    }
                                                                                });

                                                                            } catch (Throwable t){
                                                                                t.printStackTrace();
                                                                                this.rollback(conn, t, message);
                                                                            }
                                                                        });
                                                                    } catch (Throwable t){
                                                                        t.printStackTrace();
                                                                        this.rollback(conn, t, message);
                                                                    }
                                                                });
                                                            } catch (Throwable tt){
                                                                tt.printStackTrace();
                                                                this.rollback(conn, tt, message);
                                                            }
                                                        });

                                                    } catch (Throwable tt) {
                                                        tt.printStackTrace();
                                                        this.rollback(conn, tt, message);
                                                    }
                                                });

                                            } else {
                                                body.put("extra_charges",finalExtraCharges)
                                                        .put("extra_baggage",finalExtraBaggage)
                                                        .put("passengers",passengers);
                                                this.commit(conn,message,body);

                                            }
                                        }catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn,e,message);
            }
        });
    }

    private void endRegister(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                final int cashOutId = (int) body.remove(CASHOUT_ID);
                final boolean flagPromo = (boolean) body.remove(PromosDBV.FLAG_PROMO);
                final Double creditAmount = (Double) body.remove("credit_amount");
                final int boardingPassId = body.getInteger("id");
                final int createdBy = body.getInteger(CREATED_BY);
                final int currencyId = body.getInteger("currency_id");
                final int customerId = body.getInteger(CUSTOMER_ID);
                final double ivaPercent = body.getInteger("iva_percent");
                JsonArray payments = (JsonArray) body.remove("payments");
                JsonObject cashChange = (JsonObject) body.remove("cash_change");
                JsonObject promoDiscount = (JsonObject) body.remove(DISCOUNT);
                int expireOpenTicketsAfter = (int) body.remove("expire_open_tickets_after");
                final Boolean is_credit = (Boolean) body.containsKey("is_credit") ? ((Boolean) body.remove("is_credit")) : false;
                JsonObject customerCreditData = (JsonObject) body.remove("customer_credit_data");

                this.getBoardingPassById(conn, boardingPassId).whenComplete((boardingPass, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        String reservationCode = boardingPass.getString(RESERVATION_CODE);
                        Integer boardingPassStatus = boardingPass.getInteger(BOARDINGPASS_STATUS);
                        String ticketType = boardingPass.getString(TICKET_TYPE);
                        String createdAt = boardingPass.getString(CREATED_AT);

                        if (this.validateBoardingPassStatusRegister(boardingPassStatus)) {
                            this.getDistanceAndSeatings(conn, boardingPassId).whenComplete((resultDistanceAndSeatings, errorDistanceAndSeatings) -> {
                                try {
                                    if (errorDistanceAndSeatings != null) {
                                        throw errorDistanceAndSeatings;
                                    }
                                    Double distanceKm  = resultDistanceAndSeatings.getDouble("distance_km");
                                    Integer seatings = resultDistanceAndSeatings.getInteger("seatings");

                                    this.paybackBoardingPassRegister(conn, distanceKm, seatings, boardingPassId, customerId, createdBy).whenComplete((resultPayback, errorPayback) -> {
                                        try {
                                            if (errorPayback != null){
                                                throw errorPayback;
                                            }
                                            Double paybackMoney = resultPayback.getDouble("money_payback");
                                            Double paybackPoints = resultPayback.getDouble("points_payback");

                                            this.getTotalAmountTicketsForBoardingPassById(conn, boardingPassId).whenComplete((JsonObject tickets, Throwable terror) -> {
                                                try {
                                                    if (terror != null) {
                                                        throw terror;
                                                    }
                                                    Double amount = tickets.getDouble("amount");
                                                    Double discount = tickets.getDouble("discount");
                                                    Double totalAmount = tickets.getDouble("total_amount");
                                                    final Double iva = UtilsMoney.round(this.getIva(totalAmount, ivaPercent), 2);

                                                    this.getBoardingPassTicketsList(conn, boardingPassId).whenComplete((resultBPT, errorBPT) -> {
                                                        try {
                                                            if (errorBPT != null){
                                                                throw errorBPT;
                                                            }

                                                            JsonObject bodyService = new JsonObject()
                                                                    .put("id", boardingPassId)
                                                                    .put("boardingpass_status", 1)
                                                                    .put("amount", amount)
                                                                    .put("discount", discount)
                                                                    .put("iva", iva)
                                                                    .put("total_amount", totalAmount)
                                                                    .put("payback", paybackMoney)
                                                                    .put("updated_by", createdBy)
                                                                    .put("updated_at", UtilsDate.sdfDataBase(new Date()));

                                                            JsonObject bodyPromo = new JsonObject()
                                                                    .put(USER_ID, createdBy)
                                                                    .put(FLAG_USER_PROMO, false)
                                                                    .put(PromosDBV.DISCOUNT, promoDiscount)
                                                                    .put(SERVICE, SERVICES.boardingpass)
                                                                    .put(BODY_SERVICE, bodyService)
                                                                    .put(PRODUCTS, resultBPT)
                                                                    .put(OTHER_PRODUCTS, new JsonArray())
                                                                    .put(FLAG_PROMO, flagPromo);
                                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                                try {
                                                                    if(replyPromos.failed()) {
                                                                        throw replyPromos.cause();
                                                                    }
                                                                    JsonObject resultApplyDiscount = replyPromos.result().body();
                                                                    JsonObject service = resultApplyDiscount.getJsonObject(PromosDBV.SERVICE);
                                                                    Double innerTotalAmount = UtilsMoney.round(service.getDouble(TOTAL_AMOUNT), 2);
                                                                    service.put("payment_condition", body.getString("payment_condition"));
                                                                    if(is_credit) service.put("debt", creditAmount );

                                                                    String expiresAt = null;
                                                                    if (ticketType.contains("abierto")){
                                                                        Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt), Calendar.DAY_OF_YEAR, expireOpenTicketsAfter);
                                                                        expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
                                                                    }
                                                                    service.put(EXPIRES_AT, expiresAt);

                                                                    GenericQuery update = this.generateGenericUpdate(SERVICES.boardingpass.getTable(), service, true);

                                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                                        try {
                                                                            if (replyUpdate.failed()){
                                                                                throw replyUpdate.cause();
                                                                            }

                                                                            String action = is_credit ? "voucher" : "purchase";

                                                                            this.insertTicket(conn,action, boardingPassId, cashOutId, innerTotalAmount, cashChange, createdBy, ivaPercent, resultPayback, 0.0).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                                                try {
                                                                                    if (ticketError != null){
                                                                                        throw ticketError;
                                                                                    }
                                                                                    Integer ticketId = ticket.getInteger(ID);

                                                                                    this.insertTicketDetail(conn, boardingPassId, ticketId, true, 0.0, 0, action, createdBy).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                                        try {
                                                                                            if (dError != null) {
                                                                                                throw dError;
                                                                                            }

                                                                                            if (is_credit) {
                                                                                                Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
                                                                                                Double creditAvailable = actualCreditAvailable - creditAmount;
                                                                                                JsonObject customerObject = new JsonObject()
                                                                                                        .put(ID, customerId)
                                                                                                        .put("credit_available", creditAvailable)
                                                                                                        .put(UPDATED_BY, createdBy)
                                                                                                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                                                                String updateCostumer = this.generateGenericUpdateString("customer", customerObject);
                                                                                                conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCostumer) -> {
                                                                                                    try{
                                                                                                        if (replyCostumer.failed()) {
                                                                                                            throw replyCostumer.cause();
                                                                                                        }

                                                                                                        this.commit(conn, message, new JsonObject()
                                                                                                                .put("reservation_code", reservationCode)
                                                                                                                .put("credit_available", creditAvailable)
                                                                                                                .put("ticket_id", ticket.getInteger("id"))
                                                                                                                .put("payback_money", paybackMoney)
                                                                                                                .put("payback_points", paybackPoints)
                                                                                                                .put(DISCOUNT_APPLIED, flagPromo));
                                                                                                                setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                                
                                                                                                    } catch (Throwable t) {
                                                                                                        t.printStackTrace();
                                                                                                        this.rollback(conn, t, message);
                                                                                                        setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                    }
                                                                                                });
                                                                                            } else {
                                                                                                this.insertPaymentsBoardingPassRegister(conn, payments, ticketId, currencyId, boardingPassId, cashOutId, createdBy,
                                                                                                        innerTotalAmount).whenComplete((resultInsertPayment, errorInsertPayment) -> {
                                                                                                    try {
                                                                                                        if (errorInsertPayment != null) {
                                                                                                            throw errorInsertPayment;
                                                                                                        }

                                                                                                        this.commit(conn, message, new JsonObject()
                                                                                                                .put("reservation_code", reservationCode)
                                                                                                                .put("ticket_id", ticket.getInteger("id"))
                                                                                                                .put("payback_money", paybackMoney)
                                                                                                                .put("payback_points", paybackPoints)
                                                                                                                .put(DISCOUNT_APPLIED, flagPromo));
                                                                                                        setInPaymentStatus(this.getTableName(), boardingPassId);

                                                                                                    } catch (Throwable t) {
                                                                                                        t.printStackTrace();
                                                                                                        this.rollback(conn, t, message);
                                                                                                        setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                    }
                                                                                                });
                                                                                            }
                                                                                        } catch (Throwable t){
                                                                                            t.printStackTrace();
                                                                                            this.rollback(conn, t ,message);
                                                                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                        }
                                                                                    });
                                                                                } catch (Throwable t){
                                                                                    t.printStackTrace();
                                                                                    this.rollback(conn, t, message);
                                                                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                }
                                                                            });

                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            this.rollback(conn, t, message);
                                                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                        }
                                                                    });
                                                                } catch (Throwable t){
                                                                    t.printStackTrace();
                                                                    this.rollback(conn, t, message);
                                                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                }
                                                            });
                                                        } catch (Throwable t){
                                                            t.printStackTrace();
                                                            this.rollback(conn, t, message);
                                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                                        }
                                                    });
                                                } catch (Throwable t) {
                                                    t.printStackTrace();
                                                    this.rollback(conn, t, message);
                                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                                }
                                            });
                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                        }
                                    });
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                }
                            });
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                        setInPaymentStatus(this.getTableName(), boardingPassId);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
                setInPaymentStatus(this.getTableName(), message.body().getInteger(ID));
            }
        });
    }

    private void endRegisterPrepaid(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                final int cashOutId = (int) body.remove(CASHOUT_ID);
                final int prepaidId = body.getInteger("prepaid_id");
                final boolean flagPromo = (boolean) body.remove(PromosDBV.FLAG_PROMO);
                final Double creditAmount = (Double) body.remove("credit_amount");
                final int boardingPassId = body.getInteger("id");
                final int createdBy = body.getInteger(CREATED_BY);
                final int currencyId = body.getInteger("currency_id");
                final int customerId = body.getInteger(CUSTOMER_ID);
                final double ivaPercent = body.getInteger("iva_percent");
                JsonArray payments = (JsonArray) body.remove("payments");
                JsonObject cashChange = (JsonObject) body.remove("cash_change");
                JsonObject promoDiscount = (JsonObject) body.remove(DISCOUNT);
                int expireOpenTicketsAfter = (int) body.remove("expire_open_tickets_after");
                final Boolean is_credit = (Boolean) body.containsKey("is_credit") ? ((Boolean) body.remove("is_credit")) : false;
                JsonObject customerCreditData = (JsonObject) body.remove("customer_credit_data");

                this.getBoardingPassById(conn, boardingPassId).whenComplete((boardingPass, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        String reservationCode = boardingPass.getString(RESERVATION_CODE);
                        Integer boardingPassStatus = boardingPass.getInteger(BOARDINGPASS_STATUS);
                        String ticketType = boardingPass.getString(TICKET_TYPE);
                        String createdAt = boardingPass.getString(CREATED_AT);

                        if (this.validateBoardingPassStatusRegister(boardingPassStatus)) {
                            this.getDistanceAndSeatings(conn, boardingPassId).whenComplete((resultDistanceAndSeatings, errorDistanceAndSeatings) -> {
                                try {
                                    if (errorDistanceAndSeatings != null) {
                                        throw errorDistanceAndSeatings;
                                    }
                                    Double distanceKm  = resultDistanceAndSeatings.getDouble("distance_km");
                                    Integer seatings = resultDistanceAndSeatings.getInteger("seatings");

                                    this.paybackBoardingPassRegister(conn, distanceKm, seatings, boardingPassId, customerId, createdBy).whenComplete((resultPayback, errorPayback) -> {
                                        try {
                                            if (errorPayback != null){
                                                throw errorPayback;
                                            }
                                            Double paybackMoney = resultPayback.getDouble("money_payback");
                                            Double paybackPoints = resultPayback.getDouble("points_payback");

                                            this.getTotalAmountTicketsForBoardingPassById(conn, boardingPassId).whenComplete((JsonObject tickets, Throwable terror) -> {
                                                try {
                                                    if (terror != null) {
                                                        throw terror;
                                                    }
                                                    Double amount = tickets.getDouble("amount");
                                                    Double discount = tickets.getDouble("discount");
                                                    Double totalAmount = tickets.getDouble("total_amount");
                                                    final Double iva = UtilsMoney.round(this.getIva(totalAmount, ivaPercent), 2);

                                                    this.getBoardingPassTicketsList(conn, boardingPassId).whenComplete((resultBPT, errorBPT) -> {
                                                        try {
                                                            if (errorBPT != null){
                                                                throw errorBPT;
                                                            }

                                                            JsonObject bodyService = new JsonObject()
                                                                    .put("id", boardingPassId)
                                                                    .put("boardingpass_status", 1)
                                                                    .put("amount", amount)
                                                                    .put("discount", discount)
                                                                    .put("iva", iva)
                                                                    .put("total_amount", totalAmount)
                                                                    .put("payback", paybackMoney)
                                                                    .put("updated_by", createdBy)
                                                                    .put("updated_at", UtilsDate.sdfDataBase(new Date()));

                                                            JsonObject bodyPromo = new JsonObject()
                                                                    .put(USER_ID, createdBy)
                                                                    .put(FLAG_USER_PROMO, false)
                                                                    .put(PromosDBV.DISCOUNT, promoDiscount)
                                                                    .put(SERVICE, SERVICES.boardingpass)
                                                                    .put(BODY_SERVICE, bodyService)
                                                                    .put(PRODUCTS, resultBPT)
                                                                    .put(OTHER_PRODUCTS, new JsonArray())
                                                                    .put(FLAG_PROMO, flagPromo);
                                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                                try {
                                                                    if(replyPromos.failed()) {
                                                                        throw replyPromos.cause();
                                                                    }
                                                                    JsonObject resultApplyDiscount = replyPromos.result().body();
                                                                    JsonObject service = resultApplyDiscount.getJsonObject(PromosDBV.SERVICE);
                                                                    Double innerTotalAmount = UtilsMoney.round(service.getDouble(TOTAL_AMOUNT), 2);
                                                                    service.put("payment_condition", body.getString("payment_condition"));
                                                                    if(is_credit) service.put("debt", creditAmount );

                                                                    String expiresAt = null;
                                                                    if (ticketType.contains("abierto")){
                                                                        Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt), Calendar.DAY_OF_YEAR, expireOpenTicketsAfter);
                                                                        expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
                                                                    }
                                                                    service.put(EXPIRES_AT, expiresAt);

                                                                    GenericQuery update = this.generateGenericUpdate(SERVICES.boardingpass.getTable(), service, true);

                                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                                        try {
                                                                            if (replyUpdate.failed()){
                                                                                throw replyUpdate.cause();
                                                                            }

                                                                            String action = is_credit ? "voucher" : "purchase";

                                                                            this.insertTicket(conn,action, boardingPassId, cashOutId, innerTotalAmount, cashChange, createdBy, ivaPercent, resultPayback, 0.0).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                                                try {
                                                                                    if (ticketError != null){
                                                                                        throw ticketError;
                                                                                    }
                                                                                    Integer ticketId = ticket.getInteger(ID);

                                                                                    this.insertTicketDetail(conn, boardingPassId, ticketId, true, 0.0, 0, action, createdBy).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                                        try {
                                                                                            if (dError != null) {
                                                                                                throw dError;
                                                                                            }

                                                                                            if (is_credit) {
                                                                                                Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
                                                                                                Double creditAvailable = actualCreditAvailable - creditAmount;
                                                                                                JsonObject customerObject = new JsonObject()
                                                                                                        .put(ID, customerId)
                                                                                                        .put("credit_available", creditAvailable)
                                                                                                        .put(UPDATED_BY, createdBy)
                                                                                                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                                                                String updateCostumer = this.generateGenericUpdateString("customer", customerObject);
                                                                                                conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCostumer) -> {
                                                                                                    try{
                                                                                                        if (replyCostumer.failed()) {
                                                                                                            throw replyCostumer.cause();
                                                                                                        }

                                                                                                        this.updatePackageTravelCounts(conn, prepaidId, createdBy).whenComplete((replyPkgTravelCount, pkgTError) -> {
                                                                                                            try {
                                                                                                                if(pkgTError != null) {
                                                                                                                    throw new Exception(pkgTError);
                                                                                                                }
                                                                                                                this.commit(conn, message, new JsonObject()
                                                                                                                        .put("reservation_code", reservationCode)
                                                                                                                        .put("credit_available", creditAvailable)
                                                                                                                        .put("ticket_id", ticket.getInteger("id"))
                                                                                                                        .put("payback_money", paybackMoney)
                                                                                                                        .put("payback_points", paybackPoints)
                                                                                                                        .put(DISCOUNT_APPLIED, flagPromo));
                                                                                                                setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                            } catch (Exception e) {
                                                                                                                e.printStackTrace();
                                                                                                                this.rollback(conn, e, message);
                                                                                                            }
                                                                                                        });
                                                                                                    } catch (Throwable t) {
                                                                                                        t.printStackTrace();
                                                                                                        this.rollback(conn, t, message);
                                                                                                        setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                    }
                                                                                                });
                                                                                            } else {
                                                                                                this.insertPaymentsBoardingPassRegister(conn, payments, ticketId, currencyId, boardingPassId, cashOutId, createdBy,
                                                                                                        innerTotalAmount).whenComplete((resultInsertPayment, errorInsertPayment) -> {
                                                                                                    try {
                                                                                                        if (errorInsertPayment != null) {
                                                                                                            throw errorInsertPayment;
                                                                                                        }

                                                                                                        this.updatePackageTravelCounts(conn, prepaidId, createdBy).whenComplete((replyPkgTravelCount, pkgTError) -> {
                                                                                                            try {
                                                                                                                if(pkgTError != null) {
                                                                                                                    throw new Exception(pkgTError);
                                                                                                                }
                                                                                                                this.commit(conn, message, new JsonObject()
                                                                                                                        .put("reservation_code", reservationCode)
                                                                                                                        .put("ticket_id", ticket.getInteger("id"))
                                                                                                                        .put("payback_money", paybackMoney)
                                                                                                                        .put("payback_points", paybackPoints)
                                                                                                                        .put(DISCOUNT_APPLIED, flagPromo));
                                                                                                                setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                            } catch (Exception e) {
                                                                                                                e.printStackTrace();
                                                                                                                this.rollback(conn, e, message);
                                                                                                            }
                                                                                                        });
                                                                                                    } catch (Throwable t) {
                                                                                                        t.printStackTrace();
                                                                                                        this.rollback(conn, t, message);
                                                                                                        setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                                    }
                                                                                                });
                                                                                            }
                                                                                        } catch (Throwable t){
                                                                                            t.printStackTrace();
                                                                                            this.rollback(conn, t ,message);
                                                                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                        }
                                                                                    });
                                                                                } catch (Throwable t){
                                                                                    t.printStackTrace();
                                                                                    this.rollback(conn, t, message);
                                                                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                                }
                                                                            });

                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            this.rollback(conn, t, message);
                                                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                        }
                                                                    });
                                                                } catch (Throwable t){
                                                                    t.printStackTrace();
                                                                    this.rollback(conn, t, message);
                                                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                                                }
                                                            });
                                                        } catch (Throwable t){
                                                            t.printStackTrace();
                                                            this.rollback(conn, t, message);
                                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                                        }
                                                    });
                                                } catch (Throwable t) {
                                                    t.printStackTrace();
                                                    this.rollback(conn, t, message);
                                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                                }
                                            });
                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                            setInPaymentStatus(this.getTableName(), boardingPassId);
                                        }
                                    });
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                    setInPaymentStatus(this.getTableName(), boardingPassId);
                                }
                            });
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                        setInPaymentStatus(this.getTableName(), boardingPassId);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
                setInPaymentStatus(this.getTableName(), message.body().getInteger(ID));
            }
        });
    }

    public boolean validateBoardingPassStatusRegister(Integer boardingPassStatus) throws Exception {
        if (boardingPassStatus == 1) {
            throw new Exception("Boarding pass already paid");
        } else if (boardingPassStatus != 4) {
            throw new Exception("Boarding pass status is not pre boarding");
        } else {
            return true;
        }
    }

    private void insertTicketBoarding(SQLConnection conn, Message<JsonObject> message, Integer cashOutId, Double finalTotalAmount,
                                      JsonObject cashChange, Integer createdBy, Double ivaPercent, Integer boardingPassId, JsonArray payments,
                                      Integer pLen, Integer currencyId, String reservationCode, Double paybackMoney, Double paybackPoints, JsonObject movementPayback){
        // Insert ticket
        this.insertTicket(conn,"purchase", boardingPassId, cashOutId, finalTotalAmount, cashChange, createdBy, ivaPercent, movementPayback, 0.0).whenComplete((JsonObject ticket, Throwable ticketError) -> {
            if (ticketError != null) {
                this.rollback(conn, ticketError, message);
            } else {
                System.out.println("Ticket = " + ticket.getInteger("id"));
                // Insert ticket detail
                this.insertTicketDetail(conn, boardingPassId, ticket.getInteger("id"), true,0.0, 0, "purchase", createdBy).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                    if (dError != null) {
                        this.rollback(conn, dError, message);
                    } else {
                        System.out.println("Ticket detail = " + detailsSuccess);
                        // insert payments
                        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                        for (int i = 0; i < pLen; i++) {
                            JsonObject payment = payments.getJsonObject(i);
                            payment.put("ticket_id", ticket.getInteger("id"));
                            pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId, cashOutId, createdBy));
                        }
                        CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));
                        allPayments.whenComplete((ps, pt) -> {
                            if (pt != null) {
                                this.rollback(conn, pt.getCause(), message);
                            } else {
                                JsonObject finalResult = new JsonObject()
                                        .put("reservation_code", reservationCode)
                                        .put("ticket_id", ticket.getInteger("id"))
                                        .put("payback_money", paybackMoney)
                                        .put("payback_points", paybackPoints);
                                this.commit(conn, message, finalResult);
                            }
                        });
                    }
                });
            }
        });
    }

    public CompletableFuture<JsonObject> getDistanceAndSeatings(SQLConnection conn, Integer boardingPassId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_DISTANCE_AND_SEATINGS, new JsonArray().add(boardingPassId), replyDistance ->{
            try {
                if (replyDistance.failed()){
                    throw replyDistance.cause();
                }
                List<JsonObject> list = replyDistance.result().getRows();
                if (list.isEmpty()) {
                    throw new Exception("Can't get distance from boarding pass Id");
                } else {
                    future.complete(list.get(0));
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<JsonObject> paybackBoardingPassRegister(SQLConnection conn, Double km, Integer seatings, Integer boardingPassId, Integer customerId, Integer createdBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        PaybackDBV objPayback = new PaybackDBV();
        objPayback.calculatePointsBoardingPass(conn, km, seatings).whenComplete((resultCalculate, errorC) ->{
            try {
                if (errorC != null){
                    throw errorC;
                }
                Double paybackMoney = resultCalculate.getDouble("money");
                Double paybackPoints = resultCalculate.getDouble("points");
                if (customerId != null){
                    JsonObject paramMovPayback = new JsonObject()
                            .put("customer_id", customerId)
                            .put("points", paybackPoints)
                            .put("money", paybackMoney)
                            .put("type_movement", "I")
                            .put("motive", "Compra de boletos")
                            .put("id_parent", boardingPassId)
                            .put("employee_id", createdBy);
                    objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) ->{
                        try {
                            if (errorMP != null){
                                throw errorMP;
                            }
                            future.complete(movementPayback);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                }else {
                    future.complete(null);
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> paybackBoardingPassChange(SQLConnection conn, Integer boardingPassId, Integer customerId, Integer createdBy, Double money, Boolean generateMovement) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        PaybackDBV objPayback = new PaybackDBV();
        if (customerId != null && generateMovement) {
            JsonObject paramMovPayback = new JsonObject()
                    .put(CUSTOMER_ID, customerId)
                    .put("points", 0)
                    .put("money", money)
                    .put("type_movement", "I")
                    .put("motive", "Compra de boletos")
                    .put("id_parent", boardingPassId)
                    .put(EMPLOYEE_ID, createdBy);
            objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                try {
                    if (errorMP != null) {
                        throw errorMP;
                    }
                    future.complete(movementPayback);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } else {
            future.complete(null);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertPaymentsBoardingPassRegister(SQLConnection conn, JsonArray payments, Integer ticketId, Integer currencyId, Integer boardingPassId,
                                                                       Integer cashOutId, Integer createdBy, Double innerTotalAmount){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double totalPayments = 0.0;
            for (int i = 0; i < payments.size(); i++) {
                JsonObject payment = payments.getJsonObject(i);
                Double paymentAmount = payment.getDouble("amount");
                if (paymentAmount == null || paymentAmount < 0.0) {
                    throw new Exception("Invalid payment amount: " + paymentAmount);
                }
                totalPayments += UtilsMoney.round(paymentAmount, 2);
            }
            if (totalPayments > innerTotalAmount) {
                throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
            } else if (totalPayments < innerTotalAmount) {
                throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
            } else {
                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                for (int i = 0; i < payments.size(); i++) {
                    JsonObject payment = payments.getJsonObject(i);
                    payment.put("ticket_id", ticketId);
                    pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId, cashOutId, createdBy));
                }
                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[payments.size()]));
                allPayments.whenComplete((result, error) -> {
                    try {
                        if (error != null){
                            throw error;
                        }
                        future.complete(true);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    public CompletableFuture<JsonArray> getBoardingPassTicketsList(SQLConnection conn, Integer boardingPassId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_BOARDING_PASS_TICKETS_LIST, new JsonArray().add(boardingPassId), reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                future.complete(new JsonArray(results));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public Double getIva(Double amount, Double ivaPercent){
        Double iva = 0.00;

        iva = amount - (amount / (1 + (ivaPercent/100)));

        return iva;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer boardingPassId, Integer cashOutId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject cashOutMove = new JsonObject();
            payment.put("currency_id", currencyId)
                    .put("boarding_pass_id", boardingPassId)
                    .put("created_by", createdBy);

            cashOutMove.put("quantity", payment.getDouble("amount"))
                    .put("move_type", "0")
                    .put("cash_out_id", cashOutId)
                    .put("created_by", createdBy);

            PaymentDBV objPayment = new PaymentDBV();
            objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                try {
                    if (error != null) {
                        throw new Exception(error);
                    }
                    payment.put("id", resultPayment.getInteger("id"));
                    cashOutMove.put("payment_id", resultPayment.getInteger("id"));
                    String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                    conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyMove) -> {
                        try {
                            if (replyMove.failed()) {
                                throw new Exception(replyMove.cause());
                            }
                            future.complete(payment);


                        } catch (Exception ex) {
                            ex.printStackTrace();
                            future.completeExceptionally(ex);
                        }
                    });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<JsonObject> insertTicket(SQLConnection conn, String action, Integer boardingPassId, Integer cashOutId, Double totalPayments, JsonObject cashChange, Integer createdBy, Double ivaPercent, JsonObject movementPayback, Double extraCharges) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            totalPayments = UtilsMoney.round(totalPayments, 2);
            Double iva = UtilsMoney.round(this.getIva(totalPayments, ivaPercent), 2);

            // Create ticket_code
            // ticket.put("ticket_code", ticketCode);
            ticket.put("boarding_pass_id", boardingPassId);
            ticket.put("action", action);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
            ticket.put("total", totalPayments);
            ticket.put("created_by", createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));
            if (movementPayback != null){
                Double beforeMoney = movementPayback.getDouble("before_money_payback");
                ticket.put("payback_before", beforeMoney);
                ticket.put("payback_money", beforeMoney + movementPayback.getDouble("money_payback"));
            }

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble("total");
                Double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                Double difference_paid = UtilsMoney.round(paid - total, 2);
    
                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);
    
                if (totalPayments < total) {
                    future.completeExceptionally(new Throwable("The payment " + total + " is greater than the total " + totalPayments));
                } else if (totalPayments > total) {
                    future.completeExceptionally(new Throwable("The payment " + total + " is lower than the total " + totalPayments));
                } else if (paid_change > difference_paid) {
                    future.completeExceptionally(new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")"));
                } else if (paid_change < difference_paid) {
                    future.completeExceptionally(new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")"));
                }
            } else {
                ticket.put("paid", totalPayments);
                ticket.put("paid_change", 0.0);
            }
            if(extraCharges > 0f){
               ticket.put("extra_charges", extraCharges);
               ticket.put("has_extras", true);
            }
            String insert = this.generateGenericCreate("tickets", ticket);

            conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    final int id = reply.result().getKeys().getInteger(0);
                    ticket.put("id", id);
                    future.complete(ticket);
                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer boardingPassId, Integer ticketId, Boolean completePurchase, Double extras, Integer createdBy, JsonArray detailsParcels, Integer boardingPassStatus) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();

        final Double extrasFinal = extras;
        if(completePurchase){
            this.getBoardingPassDetail(conn, boardingPassId).whenComplete((List<JsonObject> details, Throwable detailsError) -> {
                if (detailsError != null) {
                    future.completeExceptionally(detailsError);
                } else {
                    int detailLen = details.size();
                    JsonObject ticketDetail = new JsonObject();
                    Double realExtras = extrasFinal;
                    if(detailsParcels.size()>0){
                        for(int i=0; i<detailsParcels.size(); i++){
                            JsonObject tempDetail = detailsParcels.getJsonObject(i);
                            ticketDetail.put("ticket_id", ticketId);
                            ticketDetail.put("quantity", 1);
                            if(tempDetail.getString("shipping_type").equals("pets")){
                                ticketDetail.put("detail", "Cargo de Paqueteria; Envío de mascota."
                                        +" Medidas: longitud="
                                        +tempDetail.getDouble("length")
                                        +" ancho="+tempDetail.getDouble("width")
                                        +" alto="+tempDetail.getDouble("height"));
                            }else if(tempDetail.getString("shipping_type").equals("frozen")){
                                ticketDetail.put("detail", "Cargo de Paqueteria; Envío de carga refrigerada"
                                        +". Medidas: longitud="
                                        +tempDetail.getDouble("length")
                                        +" ancho="+tempDetail.getDouble("width")
                                        +" alto="+tempDetail.getDouble("height"));
                            }else{
                                ticketDetail.put("detail", "Cargo de Paqueteria; Envío de paquete"
                                        +". Medidas: longitud="
                                        +tempDetail.getDouble("length")
                                        +" ancho="+tempDetail.getDouble("width")
                                        +" alto="+tempDetail.getDouble("height"));
                            }

                            ticketDetail.put("unit_price", tempDetail.getDouble("cost"));
                            ticketDetail.put("amount", tempDetail.getDouble("cost"));
                            ticketDetail.put("created_by", createdBy);
                            inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                            realExtras -= tempDetail.getDouble("cost");
                        }
                    }
                    if(realExtras > 0f){
                        ticketDetail.put("ticket_id", ticketId);
                        ticketDetail.put("quantity", 1);
                        ticketDetail.put("detail", "Cargos extras en documentación");
                        ticketDetail.put("unit_price", realExtras);
                        ticketDetail.put("amount", realExtras);
                        ticketDetail.put("created_by", createdBy);
                        inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    }
                    if(boardingPassStatus == 4){
                        for (int i = 0; i < detailLen; i++) {
                            JsonObject detail = details.get(i);
                            ticketDetail.put("ticket_id", ticketId);
                            ticketDetail.put("quantity", detail.getInteger("quantity"));
                            ticketDetail.put("detail", detail.getString("detail"));
                            ticketDetail.put("unit_price", detail.getDouble("unit_price"));
                            ticketDetail.put(DISCOUNT, detail.getDouble(DISCOUNT));
                            ticketDetail.put("amount", detail.getDouble("amount"));
                            ticketDetail.put("created_by", createdBy);

                            inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                        }
                    }

                    conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                        try{
                            if(replyInsert.failed()) {
                                throw new Exception(replyInsert.cause());
                            }
                            future.complete(replyInsert.succeeded());
                        }catch (Exception ex) {
                            ex.printStackTrace();
                            future.completeExceptionally(replyInsert.cause());
                        }
                    });
                }
            });
        } else {
            JsonObject ticketDetail = new JsonObject();
            Double realExtras = extrasFinal;
            if(detailsParcels.size()>0){
                for(int i=0; i<detailsParcels.size(); i++){
                    JsonObject tempDetail = detailsParcels.getJsonObject(i);
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    System.out.println(tempDetail.getString("shipping_type"));
                    if(tempDetail.getString("shipping_type").equals("pets")){
                        ticketDetail.put("detail", "Cargo de Paqueteria; Envío de mascota."
                                +" Medidas: longitud="
                                +tempDetail.getDouble("length")
                                +" ancho="+tempDetail.getDouble("width")
                                +" alto="+tempDetail.getDouble("height"));
                    }else if(tempDetail.getString("shipping_type").equals("frozen")){
                        ticketDetail.put("detail", "Cargo de Paqueteria; Envío de carga refrigerada"
                                +". Medidas: longitud="
                                +tempDetail.getDouble("length")
                                +" ancho="+tempDetail.getDouble("width")
                                +" alto="+tempDetail.getDouble("height"));
                    }else{
                        ticketDetail.put("detail", "Cargo de Paqueteria; Envío de paquete"
                                +". Medidas: longitud="
                                +tempDetail.getDouble("length")
                                +" ancho="+tempDetail.getDouble("width")
                                +" alto="+tempDetail.getDouble("height"));
                    }

                    ticketDetail.put("unit_price", tempDetail.getDouble("cost"));
                    ticketDetail.put("amount", tempDetail.getDouble("cost"));
                    ticketDetail.put("created_by", createdBy);
                    inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    realExtras -= tempDetail.getDouble("cost");
                }
            }
            if(realExtras > 0f){
                ticketDetail.put("ticket_id", ticketId);
                ticketDetail.put("quantity", 1);
                ticketDetail.put("detail", "Cargos extras en documentación");
                ticketDetail.put("unit_price", realExtras);
                ticketDetail.put("amount", realExtras);
                ticketDetail.put("created_by", createdBy);
                inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
            }
            if(inserts.size() > 0){
                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                    try{
                        if(replyInsert.failed()) {
                            throw new Exception(replyInsert.cause());
                        }
                        future.complete(replyInsert.succeeded());
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(replyInsert.cause());
                    }
                });
            } else {
                future.completeExceptionally(new Throwable("Nothing to put on ticket details"));
            }
        }
        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer boardingPassId, Integer ticketId, Boolean completePurchase, Double extras, Integer extraBaggage, String action, Integer createdBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();

        if(completePurchase){
            this.getBoardingPassDetail(conn, boardingPassId).whenComplete((List<JsonObject> details, Throwable detailsError) -> {
                try {
                    if (detailsError != null){
                        throw new Exception(detailsError);
                    }
                    int detailLen = details.size();
                    JsonObject ticketDetail = new JsonObject();
                    if(extras > 0f){
                        ticketDetail.put("ticket_id", ticketId);
                        ticketDetail.put("quantity", extraBaggage);
                        ticketDetail.put("detail", "Cargo extra por maleta");
                        ticketDetail.put("unit_price", extras);
                        ticketDetail.put("amount", extras);
                        ticketDetail.put("created_by", createdBy);
                        inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    }

                    for (int i = 0; i < detailLen; i++) {
                        JsonObject detail = details.get(i);
                        ticketDetail.put("ticket_id", ticketId);
                        ticketDetail.put("quantity", detail.getInteger("quantity"));
                        ticketDetail.put("detail", action.equals("cancel") ? "Cancelación " + detail.getString("detail") : detail.getString("detail"));
                        ticketDetail.put("unit_price", detail.getDouble("unit_price"));
                        ticketDetail.put(DISCOUNT, detail.getDouble(DISCOUNT));
                        ticketDetail.put("amount", detail.getDouble("amount"));
                        ticketDetail.put("created_by", createdBy);

                        inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                    }
                    conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                        try{
                            if(replyInsert.failed()) {
                                throw new Exception(replyInsert.cause());
                            }
                            future.complete(replyInsert.succeeded());
                        }catch (Exception ex) {
                            ex.printStackTrace();
                            future.completeExceptionally(ex);
                        }

                    });
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } else {
            try {
                if(extras > 0f){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Cargos extras en documentación");
                    ticketDetail.put("unit_price", extras);
                    ticketDetail.put("amount", extras);
                    ticketDetail.put("created_by", createdBy);
                    inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));

                    conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                        try{
                            if(replyInsert.failed()) {
                                throw new Exception(replyInsert.cause());
                            }
                            future.complete(replyInsert.succeeded());

                        }catch (Exception ex) {
                            future.completeExceptionally(ex);
                        }
                    });
                } else {
                    throw new Exception("Nothing to put on ticket details");
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        }


        return future;
    }

    private CompletableFuture<List<JsonObject>> getBoardingPassDetail(SQLConnection conn, Integer boardingPassId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        List<JsonObject> ticketDetails = new ArrayList<>();
        conn.queryWithParams(QUERY_TRAVEL_TICKET_DETAIL, new JsonArray().add(boardingPassId), reply -> {
            try{
                if(reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    throw new Exception("Boarding pass detail not found");
                }
                int rLen = results.size();
                for (int i = 0; i < rLen; i++) {
                    JsonObject detailResult = results.get(i);
                    JsonObject detail = new JsonObject();
                    String detailText = null;
                    String travelDate = detailResult.getString("travel_date");

                    if(detailResult.getString("type_passanger") != null){
                        detailText = detailResult.getString("type_passanger")
                                .concat(" De ").concat(detailResult.getString("terminal_origin_prefix"))
                                .concat(" a ").concat(detailResult.getString("terminal_destiny_prefix"));
                        if(travelDate != null){
                            detailText = detailText.concat(" ").concat(UtilsDate.format_es_MX_dd_MMMM_yyyy_HH_mm(travelDate));
                        }else{
                            detailText = detailText.concat(" ").concat("Abierto");
                        }
                    }

                    detail.put("quantity", detailResult.getInteger("quantity"));
                    detail.put("detail", detailText.concat(" # asiento - ").concat(detailResult.getString("seat_num")));
                    detail.put("unit_price", detailResult.getDouble("unit_price"));
                    detail.put(DISCOUNT, detailResult.getDouble(DISCOUNT));
                    detail.put("amount", detailResult.getDouble("amount"));

                    ticketDetails.add(detail);

                }
                future.complete(ticketDetails);
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * END REGISTER METHODS <!- END -!>
     */

    /**
     * CANCEL RESERVATION METHODS <!- START ->
     */

    private void cancelReservation(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                Integer cashOutId = (Integer) body.remove(CASHOUT_ID);
                String reservationCode = body.getString("reservation_code");
                Integer updatedBy = body.getInteger(UPDATED_BY);
                Integer currencyId = (Integer) body.remove(CURRENCY_ID);
                Double boardingPassIva = (Double) body.remove(IVA);


                this.getBoardingPassByReservationCode(reservationCode, 1).whenComplete((resultBoardingPass, errorBoardingPass) -> {
                    try {
                        if (errorBoardingPass != null) {
                            throw errorBoardingPass;
                        }
                        Integer boardingPassId = resultBoardingPass.getInteger(ID);
                        body.put(ID, boardingPassId)
                            .put(BOARDINGPASS_STATUS, 0)
                            .put("debt", 0);
                        Double boardingPassTotalAmount = resultBoardingPass.getDouble(TOTAL_AMOUNT);
                        Integer customerId = resultBoardingPass.getInteger(CUSTOMER_ID) != null ? resultBoardingPass.getInteger(CUSTOMER_ID) : body.getInteger(CUSTOMER_ID);
                        String ticketType = resultBoardingPass.getString("ticket_type");
                        boolean is_credit = resultBoardingPass.getString("payment_condition").equals("credit");
                        Double debt = resultBoardingPass.getDouble("debt");
                        this.cancelCheckTravelStatusByBoardingPassId(boardingPassId, ticketType).whenComplete((resultCheckTS, errorCheckTS) -> {
                           try {
                               if (errorCheckTS != null){
                                   throw errorCheckTS;
                               }
                               this.checkCashOutByBoardingPassId(null, boardingPassId).whenComplete((resultCheckCO, errorCheckCO) -> {
                                   try {
                                       if (errorCheckCO != null){
                                           throw errorCheckCO;
                                       }
                                       if (customerId == null) {
                                           throw new Exception("Customer: Required to cancel reservation");
                                       }
                                       String actionTicket = is_credit ? "voucher" : "cancel";
                                       String update = this.generateGenericUpdateString(this.getTableName() , body);
                                       this.insertTicket(conn, actionTicket, boardingPassId, cashOutId, boardingPassTotalAmount, null, updatedBy, boardingPassIva, null,0.0).whenComplete( (JsonObject ticket, Throwable ticketError)->{
                                           try{
                                               if (ticketError != null){
                                                   throw ticketError;
                                               }
                                               Integer ticketId = ticket.getInteger(ID);
                                               this.insertTicketDetail(conn, boardingPassId, ticketId, true, 0.00, null , actionTicket, updatedBy).whenComplete((resultTicketDetail, errorTicketDetail)->{
                                                   try{
                                                       if (errorTicketDetail != null){
                                                           throw errorTicketDetail;
                                                       }
                                                       conn.update(update, (AsyncResult<UpdateResult> replyUpdate) -> {
                                                           try {
                                                               if (replyUpdate.failed()) {
                                                                   throw replyUpdate.cause();
                                                               }
                                                               resultBoardingPass.put(BOARDINGPASS_STATUS, 0);
                                                               this.cancelPassengers(conn, boardingPassId, updatedBy).whenComplete((List<JsonObject> passengers, Throwable cancelError) -> {
                                                                   try {
                                                                       if (cancelError != null){
                                                                           throw cancelError;
                                                                       }

                                                                       if (is_credit) {
                                                                           Double totalDbt = resultBoardingPass.getDouble("debt");
                                                                           Double totalAmount = resultBoardingPass.getDouble("total_amount");
                                                                           if(totalDbt < totalAmount){
                                                                               throw new Exception("It is not possible to cancel. Payment process already started.");
                                                                           }
                                                                           JsonObject trackingCancelBoardingPass = new JsonObject()
                                                                                   .put("boardingpass_id", resultBoardingPass.getInteger("id"))
                                                                                   .put("ticket_id", ticket.getInteger(ID))
                                                                                   .put("action", "canceled")
                                                                                   .put("status", 0)
                                                                                   .put("created_at", UtilsDate.sdfDataBase(new Date()))
                                                                                   .put("created_by", updatedBy);
                                                                           String insert = this.generateGenericCreate("boarding_pass_tracking", trackingCancelBoardingPass);

                                                                           conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                                                                               try {
                                                                                   if (reply.failed()) {
                                                                                       throw new Exception(reply.cause());
                                                                                   }

                                                                                   DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                                                                   JsonObject paramsCredit = new JsonObject().put("customer_id", customerId);
                                                                                   vertx.eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                                                                       try{
                                                                                           if(replyCredit.failed()) {
                                                                                               throw new Exception(replyCredit.cause());
                                                                                           }
                                                                                           Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                                                                           JsonObject customerCreditData = customerCreditDataMsg.body();

                                                                                           this.updateCustomerCredit(conn, customerCreditData, debt, customerId, updatedBy, true)
                                                                                                   .whenComplete((replyCustomer, errorCustomer) -> {
                                                                                                       try{
                                                                                                           if (errorCustomer != null) {
                                                                                                               throw new Exception(errorCustomer);
                                                                                                           }

                                                                                                           JsonObject result = new JsonObject()
                                                                                                                   .put("ticket_id", ticket.getInteger(ID))
                                                                                                                   .put("boardingPass", resultBoardingPass);
                                                                                                           this.commit(conn, message, result);

                                                                                                       } catch (Throwable t) {
                                                                                                           t.printStackTrace();
                                                                                                           this.rollback(conn, t, message);
                                                                                                       }
                                                                                                   });

                                                                                       } catch (Exception e) {
                                                                                           e.printStackTrace();
                                                                                           this.rollback(conn, e, message);
                                                                                       }
                                                                                   });

                                                                               } catch (Exception e) {
                                                                                   e.printStackTrace();
                                                                                   this.rollback(conn, e, message);
                                                                               }
                                                                           });
                                                                       }
                                                                       else {
                                                                           JsonObject paramExpense = new JsonObject()
                                                                                   .put("boarding_pass_id", boardingPassId)
                                                                                   .put("ticket_id", ticketId)
                                                                                   .put(CURRENCY_ID, currencyId)
                                                                                   .put(AMOUNT, boardingPassTotalAmount)
                                                                                   .put(ExpenseDBV.SERVICE, "boardingpass")
                                                                                   .put(ExpenseDBV.ACTION, ExpenseDBV.ACTIONS.CANCEL.getName());

                                                                           ExpenseDBV expenseObj = new ExpenseDBV();
                                                                           expenseObj.register(conn, paramExpense).whenComplete((resultExpense, errorExpense) -> {
                                                                               try {
                                                                                   if (errorExpense != null){
                                                                                       throw errorExpense;
                                                                                   }
                                                                                   JsonObject trackingCancelBoardingPass = new JsonObject()
                                                                                           .put("boardingpass_id", resultBoardingPass.getInteger("id"))
                                                                                           .put("ticket_id" , ticket.getInteger(ID))
                                                                                           .put("action", "canceled")
                                                                                           .put("status",0)
                                                                                           .put("created_at",UtilsDate.sdfDataBase(new Date()))
                                                                                           .put("created_by",updatedBy);
                                                                                   String insert = this.generateGenericCreate("boarding_pass_tracking", trackingCancelBoardingPass);

                                                                                   conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                                                                                       try {
                                                                                           if (reply.failed()) {
                                                                                               throw new Exception(reply.cause());
                                                                                           }

                                                                                           JsonObject cashOutMove = new JsonObject().put("cash_out_id", cashOutId)
                                                                                                   .put("expense_id", resultExpense.getInteger("expense_id"))
                                                                                                   .put("quantity", boardingPassTotalAmount)
                                                                                                   .put("move_type", "1")
                                                                                                   .put(CREATED_BY, updatedBy);
                                                                                           String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                                                                                           conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyCom) -> {
                                                                                               try {
                                                                                                   if (replyCom.failed()) {
                                                                                                       throw new Exception(replyCom.cause());
                                                                                                   }

                                                                                                   JsonObject result = resultExpense.mergeIn(new JsonObject()
                                                                                                           .put("ticket_id", ticket.getInteger(ID))
                                                                                                           .put("boardingPass", resultBoardingPass));

                                                                                                   this.commit(conn, message, result);

                                                                                               } catch (Exception e) {
                                                                                                   e.printStackTrace();
                                                                                                   this.rollback(conn, e, message);
                                                                                               }
                                                                                           });
                                                                                       } catch (Exception e) {
                                                                                           e.printStackTrace();
                                                                                           this.rollback(conn, e, message);
                                                                                       }
                                                                                   });

                                                                               } catch (Throwable t){
                                                                                   t.printStackTrace();
                                                                                   this.rollback(conn, t, message);
                                                                               }
                                                                           });
                                                                       }

                                                                   } catch (Throwable t){
                                                                       t.printStackTrace();
                                                                       this.rollback(conn, t, message);
                                                                   }
                                                               });
                                                           } catch (Throwable t){
                                                               t.printStackTrace();
                                                               this.rollback(conn, t, message);
                                                           }
                                                       });
                                                   }catch(Throwable t){
                                                       t.printStackTrace();
                                                       this.rollback(conn, t, message);
                                                   }
                                               });
                                           }catch(Throwable t){
                                               t.printStackTrace();
                                               this.rollback(conn, t, message);
                                           }
                                       });
                                   } catch (Throwable t){
                                       t.printStackTrace();
                                       this.rollback(conn, t, message);
                                   }
                               });
                           } catch (Throwable t){
                               t.printStackTrace();
                               this.rollback(conn, t, message);
                           }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private void cancelReservationWithCashOutClosed(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                String reservationCode = body.getString(RESERVATION_CODE);
                Integer updatedBy = body.getInteger(UPDATED_BY);
                String notes = body.getString(NOTES);
                body.remove("iva_percent");

                this.getBoardingPassByReservationCode(reservationCode, null).whenComplete((resultBoardingPass, errorBoardingPass) -> {
                    try {
                        if (errorBoardingPass != null) {
                            throw errorBoardingPass;
                        }
                        Integer boardingPassId = resultBoardingPass.getInteger(ID);
                        Double debt = resultBoardingPass.getDouble("debt");
                        body.put(ID, boardingPassId)
                                .put(BOARDINGPASS_STATUS, 0)
                                .put("debt", 0.0);
                        Integer customerId = resultBoardingPass.getInteger(CUSTOMER_ID) == null
                                                ? body.getInteger(CUSTOMER_ID)
                                                : resultBoardingPass.getInteger(CUSTOMER_ID);

                        boolean is_credit = resultBoardingPass.getString("payment_condition").equals("credit");
                        Double boardingPassTotalAmount = resultBoardingPass.getDouble(TOTAL_AMOUNT);
                        if (customerId == null) {
                            throw new Exception("Customer: Required to cancel reservation");
                        }
                        if(is_credit){
                            Double totalDbt = resultBoardingPass.getDouble("debt");
                            Double totalAmount = resultBoardingPass.getDouble("total_amount");
                            if(totalDbt < totalAmount){
                                throw new Exception("It is not possible to cancel. Payment process already started.");
                            }
                        }
                        String update = this.generateGenericUpdateString(this.getTableName(), body);
                        conn.update(update, (AsyncResult<UpdateResult> replyUpdate) -> {
                            try {
                                if (replyUpdate.failed()) {
                                    throw replyUpdate.cause();
                                }
                                resultBoardingPass.put(BOARDINGPASS_STATUS, 0);
                                this.cancelPassengers(conn, boardingPassId, updatedBy).whenComplete((List<JsonObject> passengers, Throwable cancelError) -> {
                                    try {
                                        if (cancelError != null) {
                                            throw cancelError;
                                        }

                                        JsonObject trackingCancelBoardingPass = new JsonObject()
                                                .put("boardingpass_id", boardingPassId)
                                                .put(ACTION, "canceled")
                                                .put(STATUS, 1)
                                                .put(NOTES, notes)
                                                .put(CREATED_AT, UtilsDate.sdfDataBase(new Date()))
                                                .put(CREATED_BY, updatedBy);
                                        String insert = this.generateGenericCreate("boarding_pass_tracking", trackingCancelBoardingPass);

                                        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                                            try {
                                                if (reply.failed()) {
                                                    throw new Exception(reply.cause());
                                                }
                                                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                                JsonObject paramsCredit = new JsonObject().put("customer_id", customerId);
                                                vertx.eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                                    try{
                                                        if(replyCredit.failed()) {
                                                            throw new Exception(replyCredit.cause());
                                                        }
                                                        Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                                        JsonObject customerCreditData = customerCreditDataMsg.body();
                                                        this.updateCustomerCredit(conn, customerCreditData, debt, customerId, updatedBy, true)
                                                                .whenComplete((replyCustomer, errorCustomer) -> {
                                                                    try{
                                                                        if (errorCustomer != null) {
                                                                            throw new Exception(errorCustomer);
                                                                        }
                                                                        JsonObject result = new JsonObject()
                                                                                .put(NOTES, notes)
                                                                                .put("boardingPass", resultBoardingPass);
                                                                        this.commit(conn, message, result);
                                                                    } catch (Throwable t) {
                                                                        t.printStackTrace();
                                                                        this.rollback(conn, t, message);
                                                                    }
                                                                });
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        this.rollback(conn, e, message);
                                                    }
                                                });
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                this.rollback(conn, e, message);
                                            }
                                        });

                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t) {
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });

                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<Integer> cancelCheckTravelStatusByBoardingPassId(Integer boardingPassId, String typeTicket){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_CHECK_TRAVEL_STATUS_BOARDING_PASS_ID, new JsonArray().add(boardingPassId), reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }
               JsonObject result = reply.result().getRows().get(0);
               Integer checkTravelStatus = result.getInteger("check_travel_status");
               if (checkTravelStatus.equals(0) && !typeTicket.contains("abierto")){
                   throw new Exception("Tracking not exists or the trip has left");
               }
               future.complete(checkTravelStatus);
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<Boolean> checkCashOutByBoardingPassId(Integer cashOutId, Integer boardingPassId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(boardingPassId);
        this.dbClient.queryWithParams(QUERY_CHECK_OPEN_CASHOUT_BY_BOARDING_PASS_ID, params, reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }
               List<JsonObject> result = reply.result().getRows();
               if (result.isEmpty()){
                   throw new Exception("Cash out not match or was closed");
               }
               future.complete(true);
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private CompletableFuture<Double> getMoneyReturnInPayback(Double amount){
        CompletableFuture<Double> future = new CompletableFuture<>();
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                new JsonObject().put("fieldName", "cancel_penalty_percent_travel"),
                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                replyC -> {
                    try{
                        if(replyC.failed()) {
                            throw new Exception(replyC.cause());
                        }
                        JsonObject result = (JsonObject) replyC.result().body();
                        Double percentPenality = Double.valueOf(result.getString("value"));
                        Double moneyPenality = (amount * percentPenality) / 100;
                        Double moneyReturnPayback = amount - moneyPenality;
                        future.complete(moneyReturnPayback);

                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer createdBy, String action, Integer boardingPassId, JsonObject movementPayback) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject ticket = new JsonObject();
        List<String> inserts = new ArrayList<>();

        // Create ticket_code
        ticket.put("created_by", createdBy);
        ticket.put("boarding_pass_id", boardingPassId);
        ticket.put("action", action);
        ticket.put("ticket_code", UtilsID.generateID("T"));
        if (movementPayback != null){
            ticket.put("payback_before", movementPayback.getDouble("before_money_payback"));
            ticket.put("payback_money", movementPayback.getDouble("money_payback"));
        }

        String insert = this.generateGenericCreate("tickets", ticket);

        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                final int id = reply.result().getKeys().getInteger(0);
                ticket.put("id", id);
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put("ticket_id", id);
                ticketDetail.put("quantity", 1);
                ticketDetail.put("detail", "Cancelación de boleto");
                ticketDetail.put("created_by", createdBy);
                inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                    if (replyInsert.succeeded()) {
                        future.complete(ticket);
                    } else {
                        future.completeExceptionally(replyInsert.cause());
                    }
                });

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<Integer> countTicketsWithCheckin(SQLConnection conn, Integer boardingPassId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        String queryCountTickets = "SELECT COUNT(bpt.id) AS count\n" +
                "FROM boarding_pass_passenger AS bpp\n" +
                "JOIN boarding_pass_ticket AS bpt\n"+
                "ON bpp.id = bpt.boarding_pass_passenger_id AND bpt.check_in = 1 AND bpt.status = 1\n" +
                "WHERE bpp.boarding_pass_id = ? AND bpp.status = 1;";

        JsonArray params = new JsonArray()
                .add(boardingPassId);

        conn.queryWithParams(queryCountTickets, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> checkIns = reply.result().getRows();
                Integer count = checkIns.get(0).getInteger("count");
                future.complete(count);

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> cancelPassengers(SQLConnection conn, Integer boardingPassId, Integer updatedBy) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        String queryPassengers = "SELECT * FROM boarding_pass_passenger " +
                "WHERE boarding_pass_id = ? AND status = 1;";

        JsonArray params = new JsonArray()
                .add(boardingPassId);

        conn.queryWithParams(queryPassengers, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> passengers = reply.result().getRows();
                if (passengers.isEmpty()) {
                    future.complete(passengers);
                } else {
                    CompletableFuture.allOf(passengers.stream().map(p -> cancelPassenger(conn, p, updatedBy))
                            .toArray(CompletableFuture[]::new)).whenComplete((s, t)  -> {
                        if (t != null) {
                            future.completeExceptionally(t.getCause());
                        } else {
                            future.complete(passengers);
                        }
                    });
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> cancelPassenger(SQLConnection conn, JsonObject passenger, Integer updatedBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer boardingPassPassengerId = passenger.getInteger("id");

        GenericQuery delete = this.generateGenericUpdate("boarding_pass_passenger", new JsonObject()
                .put("id", boardingPassPassengerId)
                .put("status", 0)
                .put("updated_by", updatedBy)
                .put("updated_at", UtilsDate.sdfDataBase(new Date()))
        );

        conn.updateWithParams(delete.getQuery(), delete.getParams(), (AsyncResult<UpdateResult> replyUpdate) -> {
            try{
                if(replyUpdate.failed()) {
                    throw new Exception(replyUpdate.cause());
                }
                passenger.put("status", 3);

                String queryTickets = "SELECT * FROM boarding_pass_ticket " +
                        "WHERE boarding_pass_passenger_id = ? AND status = 1;";

                JsonArray params = new JsonArray()
                        .add(boardingPassPassengerId);

                conn.queryWithParams(queryTickets, params, (AsyncResult<ResultSet> replyTickets) -> {
                    if (replyTickets.failed()) {
                        future.completeExceptionally(replyTickets.cause());
                    } else {
                        List<JsonObject> tickets = replyTickets.result().getRows();
                        if (tickets.isEmpty()) {
                            future.complete(passenger);
                        } else {
                            CompletableFuture.allOf(tickets.stream().map(t -> cancelTicket(conn, t, updatedBy))
                                    .toArray(CompletableFuture[]::new)).whenComplete((s, t)  -> {
                                if (t != null) {
                                    future.completeExceptionally(t.getCause());
                                } else {
                                    future.complete(passenger);
                                }
                            });
                        }
                    }
                });
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }

        });

        return future;
    }

    private CompletableFuture<JsonObject> cancelPartialPassenger(SQLConnection conn, Integer boardingPassPassengerId, Integer updatedBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject boardingPassPassenger = new JsonObject()
                .put(ID, boardingPassPassengerId)
                .put(STATUS, 3)
                .put(UPDATED_BY, updatedBy)
                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

        conn.queryWithParams(GET_TICKETS_BY_PASSENGER_ID, new JsonArray().add(boardingPassPassengerId), (AsyncResult<ResultSet> reply) -> {
            try {
                if (reply.failed()) throw new Exception(reply.cause());
                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) throw new Exception("Tickets: Not Found");

                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                rows.forEach(boardingPassTicket -> {
                    tasks.add(this.cancelTicket(conn, boardingPassTicket, updatedBy));
                });

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((replyTasks, errorTasks) -> {
                    try {
                        if (errorTasks != null) {
                            throw new Exception(errorTasks);
                        }

                        GenericQuery delete = this.generateGenericUpdate("boarding_pass_passenger", boardingPassPassenger);
                        conn.updateWithParams(delete.getQuery(), delete.getParams(), (AsyncResult<UpdateResult> replyUpdate) -> {
                            try {
                                if (replyUpdate.failed()) {
                                    throw new Exception(replyUpdate.cause());
                                }

                                future.complete(boardingPassPassenger);

                            } catch (Throwable t) {
                                t.printStackTrace();
                                future.completeExceptionally(t);
                            }
                        });

                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> cancelTicket(SQLConnection conn, JsonObject ticket, Integer updatedBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer boardingPassTicketId = ticket.getInteger("id");

        GenericQuery delete = this.generateGenericUpdate("boarding_pass_ticket", new JsonObject()
                .put("id", boardingPassTicketId)
                .put("ticket_status", 0)
                .put("status", 3)
                .put("updated_by", updatedBy)
                .put("updated_at", UtilsDate.sdfDataBase(new Date()))
        );

        conn.updateWithParams(delete.getQuery(), delete.getParams(), (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ticket.put("status", 3);
                future.complete(ticket);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);

            }
        });

        return future;
    }

    /**
     * CANCEL RESERVATION METHODS <!- END ->
     */


    private void exchangeReservation(Message<JsonObject> message){
        this.startTransaction(message , conn ->{
            try{
                JsonObject body = message.body();
                Integer boardingPassRouteId = body.getInteger("boarding_pass_route_id");
                Integer scheduleRouteDestinationId = body.getInteger("schedule_route_destination_id");
                String reservationCode = body.getString("reservation_code");
                JsonArray tickets = body.getJsonArray("tickets");
                Integer cashOutId = body.getInteger(CASHOUT_ID);
                Integer createdBy = body.getInteger(CREATED_BY);
                getReservationDetail(reservationCode).whenComplete((boardingPassDetail , error) ->{
                    try{
                        if(error != null ){
                         throw new Exception(error.getCause());
                        }

                        Integer boardinPassId = boardingPassDetail.getInteger("id");

                        getBoardingPassRouteDetail(boardingPassRouteId).whenComplete((boardingPassRouteDetail, errorr) ->{
                            try{
                                if(errorr != null){
                                    throw new Exception(errorr.getCause());
                                }
                                String typeRoute = boardingPassRouteDetail.getString("ticket_type_route");
                                Integer terminalOriginId = boardingPassRouteDetail.getInteger("terminal_origin_id");
                                Integer terminalDestinyId = boardingPassRouteDetail.getInteger("terminal_destiny_id");
                                if(boardingPassRouteDetail.getInteger("schedule_route_destination_id") != null){
                                    throw new Exception("Travel type ".concat(typeRoute).concat(" for reservation code already is exchange"));
                                }

                                getScheduleRouteDestinationDetail(scheduleRouteDestinationId).whenComplete((scheduleRouteDetail , err) ->{
                                   try{
                                     if(err != null){
                                        throw new Exception(err);
                                     }
                                       Integer scheduleTerminalOriginId = scheduleRouteDetail.getInteger("terminal_origin_id");
                                       Integer scheduleTerminalDestinyId = scheduleRouteDetail.getInteger("terminal_destiny_id");
                                       String travelDate = scheduleRouteDetail.getString("travel_date");
                                       Boolean isReturn = true;

                                       if (!scheduleTerminalDestinyId.equals(terminalDestinyId) && !scheduleTerminalOriginId.equals(terminalOriginId) ){
                                           throw new Exception("Terminal is not valid change");
                                       }
                                       if(typeRoute.equals("ida")){
                                           isReturn = false;
                                       }
                                       //Verify seats for this schedule route destination
                                       Boolean finalIsReturn = isReturn;
                                       getAvailableSeat(scheduleRouteDestinationId ,tickets, null).whenComplete( (s , er) ->{
                                           try{
                                               if(er != null){
                                                   throw new Exception(er.getCause());
                                               }
                                               List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                                               task.add(updateBoardingPass(conn, boardinPassId, finalIsReturn, travelDate ));
                                               task.add(updateBoardingPassRoute(conn , scheduleRouteDestinationId , boardingPassRouteId));
                                               for(int i = 0; i<tickets.size();i++){
                                                   JsonObject ticket = tickets.getJsonObject(i);
                                                   task.add(updateBoardingPassTicket(conn,ticket.getInteger("id"),ticket.getString("seat"), boardingPassRouteId));
                                               }
                                               CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res,errr)->{
                                                   try {
                                                       if(errr != null){
                                                           throw new Exception(errr.getCause());
                                                       }

                                                       this.insertTicket(conn, "voucher", boardinPassId, cashOutId, 0.0, null, createdBy, 0.0, null,0.0).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                           try {
                                                               if (ticketError != null) {
                                                                   throw new Exception(ticketError);
                                                               }

                                                               String detailText = "Canje de viaje de ".concat(typeRoute).concat(" ")
                                                                       .concat(boardingPassRouteDetail.getString("terminal_origin_prefix"))
                                                                       .concat(" - ").concat(boardingPassRouteDetail.getString("terminal_destiny_prefix")).concat(" ")
                                                                       .concat(UtilsDate.format_es_MX_dd_MMMM_yyyy_HH_mm(travelDate));

                                                               JsonObject ticketDetails = new JsonObject()
                                                                       .put(TICKET_ID, ticket.getInteger(ID))
                                                                       .put(QUANTITY, tickets.size())
                                                                       .put(DETAIL, detailText)
                                                                       .put(UNIT_PRICE, 0.0)
                                                                       .put(AMOUNT, 0.0)
                                                                       .put(CREATED_BY, createdBy);

                                                               String insertDetails = this.generateGenericCreate("tickets_details", ticketDetails);
                                                               conn.update(insertDetails, (AsyncResult<UpdateResult> reply) -> {
                                                                   try {
                                                                       if (reply.failed()) {
                                                                           throw new Exception(reply.cause());
                                                                       }

                                                                       JsonObject result = new JsonObject();
                                                                       result.put(RESERVATION_CODE, reservationCode)
                                                                               .put(TICKET_ID, ticket.getInteger(ID))
                                                                               .put(STATUS,"update");
                                                                       this.commit(conn,message,result);

                                                                   } catch (Exception e) {
                                                                       e.printStackTrace();
                                                                       this.rollback(conn, e, message);
                                                                   }
                                                               });

                                                           } catch (Exception e) {
                                                               e.printStackTrace();
                                                               this.rollback(conn, e, message);
                                                           }
                                                       });
                                                   }catch (Exception e){
                                                       e.printStackTrace();
                                                       this.rollback(conn, e, message);
                                                   }
                                               });
                                           }catch (Exception e){
                                               e.printStackTrace();
                                               this.rollback(conn, e, message);
                                           }
                                       });
                                   } catch (Exception e) {
                                       e.printStackTrace();
                                       this.rollback(conn, e, message);
                                   }
                                });
                            }catch (Exception e){
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn, e, message);
            }
        });
    }

    private void changeReservation(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                String reservationCode = body.getString(RESERVATION_CODE);
                JsonArray tickets = body.getJsonArray("tickets");
                JsonArray payments = (JsonArray) body.remove("payments");
                JsonArray passengers = (JsonArray) body.remove("passengers");
                JsonObject cashChange = (JsonObject) body.remove("cash_change");
                Integer cashOutId = body.getInteger(CASHOUT_ID);
                Integer createdBy = body.getInteger(CREATED_BY);
                Double ivaPercent = body.getDouble("iva_percent");
                Integer currencyId = body.getInteger(CURRENCY_ID);
                Boolean onlyTravelReturn = body.containsKey("only_travel_return") ? body.getBoolean("only_travel_return") : Boolean.valueOf(false);
                Integer underAge = (Integer) body.remove("under_age");
                Integer permitedChanges = (Integer) body.remove("permitedChanges");
                Integer customerId = body.containsKey(CUSTOMER_ID) ? body.getInteger(CUSTOMER_ID) : null;
                Integer origin = 1;

                getReservationDetail(reservationCode).whenComplete((boardingPassDetail, error) -> {
                    try {
                        if (error != null) {
                            throw new Exception(error.getCause());
                        }
                        Integer reservationTime = body.getInteger("reservation_time");

                        Integer boardingPassId = boardingPassDetail.getInteger(ID);
                        Double actualTotalAmount = boardingPassDetail.getDouble(TOTAL_AMOUNT);
                        Double payback = boardingPassDetail.getDouble("payback");
                        String ticketType = boardingPassDetail.getString("ticket_type");
                        Integer boardingPassStatus = boardingPassDetail.getInteger(BOARDINGPASS_STATUS);
                        Integer boardingChanges = boardingPassDetail.getInteger("changes");
                        AtomicReference<Integer> seatings = new AtomicReference<>(boardingPassDetail.getInteger("seatings"));
                        Integer bodySeatings = tickets.size();
                        Boolean generateMovementPayback = false;

                        if(boardingChanges >= permitedChanges){
                            throw new Exception("Reservation exceeded the times of changes permitted");
                        }
                        if(boardingPassDetail.getString("travel_date") ==  null){
                            throw new Exception("Boarding pass has not been exchanged");
                        }

                        if (boardingPassStatus.equals(0))
                            throw new Exception("Reservation code is canceled");

                        if (boardingPassStatus.equals(4))
                            throw new Exception("The boarding pass is not paid yet");

                        if (bodySeatings < seatings.get())
                            throw new Exception("The number of passengers cannot decrease in a change");

                        if (payments == null)
                            throw new Exception("No payment object was found");

                        Double cashAmount = cashChange.getDouble(TOTAL);
                        Double innerTotalAmount = UtilsMoney.round(cashAmount, 2);

                        List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                        Double amount = 0.0;
                        Double discount = 0.0;
                        Double totalAmount = 0.0;
                        for(int i = 0; i < tickets.size(); i++) {
                            JsonObject ticket = tickets.getJsonObject(i);
                            amount += ticket.getDouble(AMOUNT);
                            discount += ticket.getDouble(DISCOUNT);
                            totalAmount += ticket.getDouble(TOTAL_AMOUNT);
                        }

                        Double differenceVal = totalAmount - actualTotalAmount;
                        Double difference = UtilsMoney.round(differenceVal, 2);
                        IntStream.range(0, tickets.size()).forEach(i -> {
                            JsonObject ticket = tickets.getJsonObject(i);
                            if (!ticket.containsKey(ID) ) return;
                            task.add(changeReservationTicket(conn, tickets.getJsonObject(i), createdBy,
                                    boardingPassId, ticketType, onlyTravelReturn, difference, reservationTime, origin, null));
                        });
                        if(passengers != null && !passengers.isEmpty()){
                            database.boardingpass.enums.TICKET_TYPE ticketTypeEnum = database.boardingpass.enums.TICKET_TYPE.valueOf(ticketType);
                            IntStream.range(0, passengers.size()).mapToObj(passengers::getJsonObject).forEach(passenger -> {
                                task.add(addBoardingPassPassenger(conn, passenger, boardingPassId, createdBy, origin, ticketTypeEnum));
                                seatings.set(seatings.get() + 1);
                            });
                        }
                        if (difference < 0.0) {
                            payback += (difference * -1);
                            generateMovementPayback = true;
                        }

                        task.add(updateBoardingPass(conn, boardingPassId, amount, totalAmount, discount, difference, seatings.get(), payback, boardingChanges));

                        if (difference < innerTotalAmount && difference >= 0 && boardingPassStatus.equals(1))
                            throw new Exception("The payment " + innerTotalAmount + " is greater than the difference " + difference);

                        if (difference > innerTotalAmount && difference >= 0 && boardingPassStatus.equals(1))
                            throw new Exception("The payment " + innerTotalAmount + " is lower than the difference " + difference);

                        Boolean finalGenerateMovementPayback = generateMovementPayback;
                        CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res, errr) -> {
                            try {
                                if (errr != null) {
                                    throw new Exception(errr.getCause());
                                }

                                this.paybackBoardingPassChange(conn, boardingPassId, customerId, createdBy, (difference * -1), finalGenerateMovementPayback).whenComplete((resultPayback, errorPayback) -> {
                                    try {
                                        if (errorPayback != null) {
                                            throw errorPayback;
                                        }

                                        this.insertTicket(conn, "change", boardingPassId, cashOutId, innerTotalAmount, cashChange, createdBy, ivaPercent, resultPayback,0.0).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                            try {
                                                if (ticketError != null) {
                                                    throw ticketError;
                                                }

                                                Integer ticketId = ticket.getInteger(ID);
                                                this.insertTicketDetail(conn, boardingPassId, ticketId, true, 0.0, 0, "change", createdBy).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                    try {
                                                        if (dError != null) {
                                                            throw dError;
                                                        }

                                                        this.insertPaymentsBoardingPassChange(conn, payments, ticketId, currencyId, boardingPassId, cashOutId, createdBy, innerTotalAmount).whenComplete((resultInsertPayment, errorInsertPayment) -> {
                                                            try {
                                                                if (errorInsertPayment != null) {
                                                                    throw errorInsertPayment;
                                                                }

                                                                JsonObject result = new JsonObject();
                                                                result.put(RESERVATION_CODE, reservationCode)
                                                                        .put(TICKET_ID, ticket.getInteger(ID))
                                                                        .put(STATUS, "updated");
                                                                this.commit(conn, message, result);

                                                            } catch (Throwable tt) {
                                                                tt.printStackTrace();
                                                                this.rollback(conn, tt, message);
                                                            }
                                                        });

                                                    } catch (Throwable tt) {
                                                        tt.printStackTrace();
                                                        this.rollback(conn, tt, message);
                                                    }
                                                });
                                            } catch (Throwable tt) {
                                                tt.printStackTrace();
                                                this.rollback(conn, tt, message);
                                            }
                                        });
                                    } catch (Throwable tt) {
                                        tt.printStackTrace();
                                        this.rollback(conn, tt, message);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                this.rollback(conn, e, message);
            }
        });
    }

    private CompletableFuture<JsonObject> changeReservationTicket(SQLConnection conn, JsonObject ticket, Integer createdBy,
                                                                  Integer boardingPassId, String ticketType, Boolean onlyTravelReturn, Double difference, Integer reservationTime, Integer origin, Integer integrationPartnerSessionId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject boardingPassRoute = ticket.getJsonObject("route");
            Integer boardingPassRouteId = boardingPassRoute.getInteger("boarding_pass_route_id");
            Integer scheduleRouteDestinationId = boardingPassRoute.getInteger("schedule_route_destination_id");
            getBoardingPassRouteDetail(boardingPassRouteId).whenComplete((boardingPassRouteDetail, errorr) -> {
                try {
                    if (errorr != null) {
                        throw new Exception(errorr.getCause());
                    }

                    String typeRoute = boardingPassRouteDetail.getString("ticket_type_route");
                    Integer terminalOriginId = boardingPassRouteDetail.getInteger("terminal_origin_id");

                    if (boardingPassRoute == null || boardingPassRoute.size() < 1)
                        throw new Exception("No Routes object was found");

                    if(onlyTravelReturn && boardingPassRoute.size() > 1)
                        throw new Exception("Route: just one route needed");
                    if(scheduleRouteDestinationId == null){
                        JsonObject result = new JsonObject();
                        result.put(STATUS, "updated");
                        future.complete(result);
                    }else{
                        getScheduleRouteDestinationDetail(scheduleRouteDestinationId).whenComplete((scheduleRouteDetail, err) -> {
                            try {
                                if (err != null) {
                                    throw new Exception(err);
                                }
                                Integer scheduleTerminalOriginId = scheduleRouteDetail.getInteger("terminal_origin_id");
                                String travelDate = scheduleRouteDetail.getString("travel_date");
                                Boolean isReturn = true;


                                //if (!scheduleTerminalOriginId.equals(terminalOriginId)) throw new Exception("Terminal is not valid change");
                                if (typeRoute.equals("ida")) isReturn = false;

                                if (typeRoute.equals("ida") && UtilsDate.isLowerThan(UtilsDate.addHours(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(travelDate), reservationTime), UtilsDate.getLocalDate()) && ticketType.equals("sencillo"))
                                    throw new Exception("travel date has already passed");

                                if (typeRoute.equals("ida") && onlyTravelReturn)
                                    throw new Exception("Only travel return necessary: this type is '" + typeRoute + "'");

                                if (ticketType.equals("redondo") && onlyTravelReturn && UtilsDate.isGreaterThan(UtilsDate.parse_yyyy_MM_dd_HH_mm(travelDate), UtilsDate.getLocalDate()))
                                    throw new Exception("the 'ida' travel has not passed");

                                //Verify seats for this schedule route destination
                                Boolean finalIsReturn = isReturn;
                                getAvailableSeatChange(scheduleRouteDestinationId, new JsonArray().add(ticket), integrationPartnerSessionId).whenComplete((s, er) -> {
                                    try {
                                        if (er != null) {
                                            throw new Exception(er.getCause());
                                        }
                                        List<CompletableFuture<JsonObject>> task = new ArrayList<>();
                                        task.add(updateBoardingPassTicket(conn, ticket.getInteger(ID),
                                                ticket.getString(SEAT), boardingPassRouteId, ticket.getInteger("special_ticket_id"),
                                                scheduleRouteDestinationId, ticket.getInteger("config_ticket_price_id"), createdBy, boardingPassId, difference, origin));
                                        task.add(updateBoardingPass(conn, boardingPassId, finalIsReturn, travelDate));
                                        task.add(updateBoardingPassRoute(conn, scheduleRouteDestinationId, boardingPassRouteId));
                                        CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()])).whenComplete((res, errr) -> {
                                            try {
                                                if (errr != null) {
                                                    throw new Exception(errr.getCause());
                                                }

                                                JsonObject result = new JsonObject();
                                                result.put(STATUS, "updated");
                                                future.complete(result);

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        future.completeExceptionally(e);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                future.completeExceptionally(e);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> insertPaymentsBoardingPassChange(SQLConnection conn, JsonArray payments, Integer ticketId, Integer currencyId, Integer boardingPassId,
                                                                        Integer cashOutId, Integer createdBy, Double innerTotalAmount) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double totalPayments = 0.0;
            for (int i = 0; i < payments.size(); i++) {
                JsonObject payment = payments.getJsonObject(i);
                Double paymentAmount = payment.getDouble("amount");
                if (paymentAmount == null || paymentAmount < 0.0) {
                    throw new Exception("Invalid payment amount: " + paymentAmount);
                }
                totalPayments += UtilsMoney.round(paymentAmount, 2);
            }
            if (totalPayments > innerTotalAmount) {
                throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
            } else if (totalPayments < innerTotalAmount) {
                throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
            } else {
                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                for (int i = 0; i < payments.size(); i++) {
                    JsonObject payment = payments.getJsonObject(i);
                    payment.put("ticket_id", ticketId);
                    pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId, cashOutId, createdBy));
                }
                CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[payments.size()]));
                allPayments.whenComplete((result, error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        future.complete(true);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPassRoute(SQLConnection conn , Integer scheduleRouteDestination , Integer boardingPassRouteId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
           conn.updateWithParams(QUERY_UPDATE_BOARDING_PASS_ROUTE , new JsonArray().add(scheduleRouteDestination).add(boardingPassRouteId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    if(reply.result().getUpdated() < 1){
                        throw new Exception("Boarding pass route not found");
                    }
                    future.complete(new JsonObject());
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPass(SQLConnection conn, Integer boardingPassId, Boolean typeTravel, String travelDate){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            String QUERY;
            if(typeTravel){
                QUERY = UPDATE_BOARDING_PASS_RETURN;
            }else{
                QUERY = UPDATE_BOARDING_PASS_DEPART;
            }
            conn.updateWithParams(QUERY , new JsonArray().add(travelDate).add(boardingPassId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    if(reply.result().getUpdated() < 1){
                        throw new Exception("Boarding pass not found");
                    }
                    future.complete(new JsonObject());
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPassPassenger(SQLConnection conn, Integer boardingPassId, Boolean typeTravel, String travelDate){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            conn.updateWithParams(UPDATE_BOARDING_PASS_PASSENGER , new JsonArray().add(travelDate).add(boardingPassId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }

                    if(reply.result().getUpdated() < 1){
                        throw new Exception("Boarding pass passenger not found");
                    }
                    future.complete(new JsonObject());
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPass(SQLConnection conn, Integer boardingPassId, Boolean typeTravel,
                                                             String travelDate, Double amount, Double totalAmount, Double discount) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject boardingPass = new JsonObject()
                    .put(ID, boardingPassId)
                    .put(AMOUNT, amount)
                    .put(TOTAL_AMOUNT, totalAmount)
                    .put(DISCOUNT, discount);

            if (typeTravel) boardingPass.put("travel_return_date", travelDate);
            else boardingPass.put("travel_date", travelDate);

            GenericQuery update = this.generateGenericUpdate(this.getTableName(), boardingPass);
            conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    if (reply.result().getUpdated() < 1) {
                        throw new Exception("Boarding pass not found");
                    }
                    future.complete(new JsonObject());
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPass(SQLConnection conn, Integer boardingPassId, Double amount, Double totalAmount, Double discount, Double difference, Integer seatings, Double payback, Integer boardingpassChanges) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject boardingPass = new JsonObject()
                    .put(ID, boardingPassId)
                    .put("seatings", seatings)
                    .put("payback", payback)
                    .put("changes", boardingpassChanges + 1)
                    .put(BOARDINGPASS_STATUS, 1);

            if (difference > 0.0) {
                boardingPass.put(AMOUNT, amount)
                        .put(TOTAL_AMOUNT, totalAmount)
                        .put(DISCOUNT, discount);
            }

            GenericQuery update = this.generateGenericUpdate(this.getTableName(), boardingPass);
            conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    if (reply.result().getUpdated() < 1) {
                        throw new Exception("Boarding pass not found");
                    }
                    future.complete(new JsonObject());
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPassTicket(SQLConnection conn , Integer ticketId, String seat, Integer boardingPassRoute){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            conn.updateWithParams(QUERY_UPDATE_BOARDING_PASS_TICKET , new JsonArray().add(seat).add(ticketId).add(boardingPassRoute), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    if(reply.result().getUpdated() < 1){
                        throw new Exception("Ticket not found");
                    }
                    future.complete(new JsonObject());
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> updateBoardingPassTicket(SQLConnection conn, Integer ticketId, String seat, Integer boardingPassRoute, Integer specialTicketId, Integer scheduleRouteDestinationId, Integer configTicketPriceId, Integer createdBy, Integer boardingPassId, Double difference, Integer origin) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_CONFIG_TICKET_PRICE_BY_DESTINATION_AND_TICKET;
            JsonArray params = new JsonArray().add(ticketId).add(scheduleRouteDestinationId).add(specialTicketId);
            conn.queryWithParams(QUERY, params, (AsyncResult<ResultSet> replyPrices) -> {
                try {
                    if (replyPrices.failed()) {
                        throw new Exception(replyPrices.cause());
                    }

                    List<JsonObject> results = replyPrices.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Config ticket price not found for schedule destination ");
                    }

                    vertx.eventBus().send(TicketPricesRulesDBV.class.getSimpleName(), results.get(0), new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_TICKET_PRICE_RULE), replyC -> {
                        try {
                            if (replyC.failed()) {
                                throw replyC.cause();
                            }
                            JsonObject price = (JsonObject) replyC.result().body();
                            conn.queryWithParams(QUERY_GET_BOARDING_PASS_TICKET_BY_ID, new JsonArray().add(ticketId), (AsyncResult<ResultSet> replyTicket) -> {
                                try {
                                    if (replyTicket.failed()) {
                                        throw new Exception(replyTicket.cause());
                                    }
                                    List<JsonObject> resultsTickets = replyTicket.result().getRows();
                                    if (resultsTickets.isEmpty())
                                        future.completeExceptionally(new Throwable("Boarding pass ticket not found"));
                                    JsonObject ticketObject = resultsTickets.get(0);
                                    JsonObject boardingPassPassenger = new JsonObject()
                                            .put(ID, ticketObject.getInteger("boarding_pass_passenger_id"))
                                            .put("special_ticket_id", specialTicketId);

                                    GenericQuery updatePassenger = this.generateGenericUpdate("boarding_pass_passenger", boardingPassPassenger);
                                    conn.updateWithParams(updatePassenger.getQuery(), updatePassenger.getParams(), replyUpdatePassenger -> {
                                        try{
                                            if (replyUpdatePassenger.failed()) {
                                                throw new Exception(replyUpdatePassenger.cause());
                                            }

                                            JsonObject ticket = new JsonObject().put(ID, ticketId)
                                                    .put("boarding_pass_route_id", boardingPassRoute)
                                                    .put("config_ticket_price_id", configTicketPriceId)
                                                    .put(UPDATED_BY, createdBy)
                                                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                            if(difference > 0){
                                                ticket.put(AMOUNT, price.getDouble(AMOUNT))
                                                        .put(TOTAL_AMOUNT, price.getDouble(TOTAL_AMOUNT))
                                                        .put("price_ticket", price.getDouble(TOTAL_AMOUNT))
                                                        .put(DISCOUNT, price.getDouble(DISCOUNT));
                                            }

                                            if(seat != null)
                                                ticket.put(SEAT, seat);

                                            GenericQuery update = this.generateGenericUpdate("boarding_pass_ticket", ticket);
                                            conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                try {
                                                    if (replyUpdate.failed()) {
                                                        throw new Exception(replyUpdate.cause());
                                                    }
                                                    if (replyUpdate.result().getUpdated() < 1) {
                                                        throw new Exception("Ticket not found");
                                                    }

                                                    JsonObject trackingChangeBoardingPass = new JsonObject()
                                                            .put("boardingpass_id", boardingPassId)
                                                            .put("boardingpass_ticket_id", ticketId)
                                                            .put(ACTION, "changed-route")
                                                            .put(STATUS, 1)
                                                            .put(NOTES, "changed route")
                                                            .put(CREATED_BY, createdBy);

                                                    GenericQuery insert = this.generateGenericCreateSendTableName("boarding_pass_tracking", trackingChangeBoardingPass);
                                                    conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                                        try {
                                                            if (reply.failed()) {
                                                                throw new Exception(reply.cause());
                                                            }

                                                            future.complete(ticket);

                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            future.completeExceptionally(e);
                                                        }
                                                    });

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    future.completeExceptionally(e);
                                                }
                                            });

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            future.completeExceptionally(e);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    future.completeExceptionally(e);
                                }
                            });
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private void getTicketsPrice(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray tickets = body.getJsonArray("tickets");
        JsonArray passengers = body.getJsonArray("passengers");
        JsonArray ticketsReply = new JsonArray();
        if(passengers != null && !passengers.isEmpty()) {
            IntStream.range(0, passengers.size()).mapToObj(passengers::getJsonObject).forEach(passenger -> {
                JsonArray ticketsPassenger = passenger.getJsonArray("tickets");
                IntStream.range(0, ticketsPassenger.size()).mapToObj(ticketsPassenger::getJsonObject).map(ticketPassenger -> new JsonObject()
                        .put("special_ticket_id", passenger.getInteger("special_ticket_id"))
                        .put(SEAT, ticketPassenger.getString(SEAT))
                        .put("config_destination_id", ticketPassenger.getInteger(CONFIG_DESTINATION_ID))
                        .put("route", new JsonObject().put(SCHEDULE_ROUTE_DESTINATION_ID, ticketPassenger.getInteger(SCHEDULE_ROUTE_DESTINATION_ID)).put("config_destination_id", ticketPassenger.getInteger(CONFIG_DESTINATION_ID)))).forEach(tickets::add);
            });
        }
        try {
            for(int i = 0; i < tickets.size(); i++) {
                JsonObject ticket = tickets.getJsonObject(i);
                Integer ticketId = ticket.getInteger(ID);
                JsonObject route = ticket.getJsonObject("route");
                Integer scheduleRouteDestinationId = route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
                Integer specialTicketId = ticket.getInteger("special_ticket_id");
                JsonArray params = new JsonArray();
                String QUERY;
                if(scheduleRouteDestinationId == null){
                    QUERY = QUERY_CONFIG_TICKET_PRICE_BY_CONFIG_ROUTE_ID_V2;
                    params.add(ticketId);
                    params.add(route.getInteger(CONFIG_DESTINATION_ID));
                    params.add(specialTicketId);
                }else{
                    QUERY = QUERY_CONFIG_TICKET_PRICE_BY_DESTINATION_AND_TICKET;
                    params.add(ticketId);
                    params.add(scheduleRouteDestinationId);
                    params.add(specialTicketId);
                }

                int finalI = i;
                this.dbClient.queryWithParams(QUERY, params, (AsyncResult<ResultSet> replyPrices) -> {
                    try {
                        if (replyPrices.failed()) {
                            throw new Exception(replyPrices.cause());
                        }
                        List<JsonObject> results = replyPrices.result().getRows();
                        if (results.isEmpty()) {
                            throw new Exception("Config ticket price not found for schedule destination ");
                        }

                        JsonObject bodyApplyTicketPriceRule = results.get(0);

                        vertx.eventBus().send(TicketPricesRulesDBV.class.getSimpleName(), bodyApplyTicketPriceRule, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_TICKET_PRICE_RULE), replyC -> {
                            try {
                                if (replyC.failed()) {
                                    throw replyC.cause();
                                }
                                JsonObject price = (JsonObject) replyC.result().body();
                                    ticket.put(AMOUNT, price.getDouble("amount"))
                                        .put(TOTAL_AMOUNT, price.getDouble("total_amount"))
                                        .put("config_ticket_price_id", price.getInteger("config_ticket_price_id"))
                                        .put(DISCOUNT, price.getDouble("discount"));
                                ticketsReply.add(ticket);

                                if(ticketsReply.isEmpty()) throw new Exception("Config ticket price not found");
                                if(finalI == tickets.size() - 1)
                                    message.reply(new JsonObject().put("tickets", ticketsReply));
                            } catch (Throwable t) {
                                this.reportQueryError(message, t);
                            }
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.reportQueryError(message, ex);
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            this.reportQueryError(message, ex);
        }
    }

    private CompletableFuture<JsonObject> getAvailableSeat(Integer scheduleRouteDestinationId , JsonArray tickets, Integer integrationPartnerSessionId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            List<CompletableFuture<Integer>> seatsTasks = new ArrayList<>();
            for(int i = 0; i<tickets.size();i++){
                JsonObject ticket = tickets.getJsonObject(i);
                seatsTasks.add(this.searchSeat(scheduleRouteDestinationId,ticket.getString(SEAT), integrationPartnerSessionId));
            }
            CompletableFuture.allOf(seatsTasks.toArray(new CompletableFuture[tickets.size()])).whenComplete( (r,e ) ->{
                try{
                   if(e != null){
                       throw new Exception(e.getCause());
                   }
                   System.out.println(r);
                   future.complete(new JsonObject());
                }catch (Exception ex){
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getAvailableSeatChange(Integer scheduleRouteDestinationId, JsonArray tickets, Integer integrationPartnerSessionId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            List<CompletableFuture<Integer>> seatsTasks = new ArrayList<>();
            for (int i = 0; i < tickets.size(); i++) {
                JsonObject ticket = tickets.getJsonObject(i);
                if (ticket.containsKey(SEAT))
                    seatsTasks.add(this.searchSeat(scheduleRouteDestinationId, ticket.getString(SEAT), integrationPartnerSessionId));
            }
            if (seatsTasks.size() > 1) {
                CompletableFuture.allOf(seatsTasks.toArray(new CompletableFuture[tickets.size()])).whenComplete((r, e) -> {
                    try {
                        if (e != null) {
                            throw new Exception(e.getCause());
                        }
                        System.out.println(r);
                        future.complete(new JsonObject());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
            } else{
                future.complete(new JsonObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }


    private CompletableFuture<JsonObject> getScheduleRouteDestinationDetail(Integer scheduleRouteDestinationId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams("SELECT * FROM schedule_route_destination where id = ?;",new JsonArray().add(scheduleRouteDestinationId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Schedule Route Destination not found");
                    }
                    JsonObject scheduleRouteDestination = result.get(0);
                    future.complete(scheduleRouteDestination);
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getScheduleRouteDestinationDetailWithJoins(Integer scheduleRouteDestinationId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams(QUERY_SCHEDULE_DESTINATION_JOINS, new JsonArray().add(scheduleRouteDestinationId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Schedule Route Destination not found");
                    }
                    JsonObject scheduleRouteDestination = result.get(0);
                    future.complete(scheduleRouteDestination);
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }


    private CompletableFuture<JsonObject> getBoardingPassRouteDetail(Integer boardingPassRouteId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams(QUERY_CONFIG_TERMINAL_BY_BOARDING_PASS_ROUTE,new JsonArray().add(boardingPassRouteId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Boarding pass route not found");
                    }
                    JsonObject boardingPassRouteDetail = result.get(0);
                    future.complete(boardingPassRouteDetail);
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonArray> getBoardingPassRouteDetail(JsonArray boardingPassRoute) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray routes = new JsonArray();
        try {
            for (Object r : boardingPassRoute) {
                JsonObject routeObject = new JsonObject(String.valueOf(r));
                this.dbClient.queryWithParams(QUERY_CONFIG_TERMINAL_BY_BOARDING_PASS_ROUTE, new JsonArray().add(routeObject.getInteger("boarding_pass_route_id")), reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        List<JsonObject> result = reply.result().getRows();
                        if (result.isEmpty()) {
                            throw new Exception("Boarding pass route not found");
                        }
                        JsonObject boardingPassRouteDetail = result.get(0);
                        routes.add(boardingPassRouteDetail);
                        if(routeObject == boardingPassRoute.getJsonObject(boardingPassRoute.size() - 1))
                            future.complete(routes);
                    } catch (Exception e) {
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getReservationDetail(String reservationCode){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams(QUERY_GET_RESERVATION_DETAIL,new JsonArray().add(reservationCode), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Reservation code not found");
                    }
                    JsonObject reservationCodeDetail = result.get(0);
                    future.complete(reservationCodeDetail);
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getPassengersDetail(String reservationCode){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams(QUERY_GET_PASSENGERS_DETAIL,new JsonArray().add(reservationCode), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Reservation code not found");
                    }
                    JsonObject reservationCodeDetail = result.get(0);
                    future.complete(reservationCodeDetail);
                }catch (Exception e){
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }


    private void getSeats(Message<JsonObject> message) {
        JsonObject body = message.body();

        String QUERY = QUERY_GET_ORDERS_BY_DESTINATION.concat(GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_DESTINATION_PARAM);
        JsonArray params = new JsonArray().add(body.getInteger(SCHEDULE_ROUTE_DESTINATION_ID));

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.size() > 0) {
                    JsonObject scheduleRouteDestination = results.get(0);
                    Integer scheduleRouteId = scheduleRouteDestination.getInteger("schedule_route_id");
                    Integer orderOrigin = scheduleRouteDestination.getInteger("order_origin");
                    Integer orderDestiny = scheduleRouteDestination.getInteger("order_destiny");
                    final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                            .add(orderOrigin).add(orderDestiny)
                            .add(orderDestiny).add(orderDestiny)
                            .add(orderOrigin).add(orderDestiny);
                    this.dbClient.queryWithParams(QUERY_AVAILABLE_SEATING_BY_DESTINATION.concat(QUERY_ADD_GROUP_BY_SEATS), paramsList, replyList -> {
                        if (replyList.succeeded()) {
                            List<JsonObject> resultsList = replyList.result().getRows();
                            List<String> boardingSeats = resultsList.stream()
                                    .map(p -> p.getString(SEAT)).collect(Collectors.toList());

                            this.getSeatLocks(scheduleRouteId, orderOrigin, orderDestiny).setHandler(replyLocked -> {
                                try {
                                    if (replyLocked.failed()) {
                                        reportQueryError(message, replyLocked.cause());
                                        return;
                                    }

                                    List<String> lockedSeats = replyLocked.result();
                                    System.out.println(lockedSeats);
                                    boardingSeats.addAll(lockedSeats);

                                    message.reply(new JsonObject()
                                            .put("busy_seats", new JsonArray(boardingSeats))
                                            .put("config", scheduleRouteDestination));
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    reportQueryError(message, throwable);
                                }
                            });

                        } else {
                            reportQueryError(message, replyList.cause());
                        }
                    });
                } else {
                    reportQueryError(message, new Throwable("Schedule route destination not found"));
                }

            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
        });
    }

    private void  getSeatsWithDestination(Message<JsonObject> message) {
        JsonObject body = message.body();

        JsonArray params = new JsonArray()
                .add(body.getInteger(SCHEDULE_ROUTE_ID))
                .add(body.getInteger(TERMINAL_ORIGIN_ID))
                .add(body.getInteger("terminal_destiny_id"));
        String QUERY = QUERY_GET_ORDERS_BY_DESTINATION
                    .concat(GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_PARAM)
                    .concat(AND_PARAM)
                    .concat(GET_ORDERS_BY_DESTINATION_TERMINAL_ORIGIN_ID_PARAM)
                    .concat(AND_PARAM)
                    .concat(GET_ORDERS_BY_DESTINATION_TERMINAL_DESTINY_ID_PARAM)
                    .concat(GET_ORDERS_BY_DESTINATION_ORDER_BY_DESC);

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    throw new Exception("Schedule route destination not found");
                }
                JsonObject scheduleRouteDestination = results.get(0);
                Integer scheduleRouteId = scheduleRouteDestination.getInteger("schedule_route_id");
                Integer orderOrigin = scheduleRouteDestination.getInteger("order_origin");
                Integer orderDestiny = scheduleRouteDestination.getInteger("order_destiny");
                final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                        .add(orderOrigin).add(orderDestiny)
                        .add(orderDestiny).add(orderDestiny)
                        .add(orderOrigin).add(orderDestiny);
                this.dbClient.queryWithParams(QUERY_AVAILABLE_SEATING_BY_DESTINATION, paramsList, replyList -> {
                    try {
                        if (replyList.failed()){
                            throw new Exception(replyList.cause());
                        }
                        List<JsonObject> resultsList = replyList.result().getRows();
                        resultsList.forEach(res -> res.remove(SCHEDULE_ROUTE_ID));

                        this.getLockedSeatsWithDestination(scheduleRouteId, orderOrigin, orderDestiny).setHandler(replyLocked -> {
                            try {
                                if (replyLocked.failed()) {
                                    reportQueryError(message, replyLocked.cause());
                                    return;
                                }

                                List<JsonObject> lockedSeats = replyLocked.result();
                                resultsList.addAll(lockedSeats);


                                message.reply(new JsonObject()
                                        .put("busy_seats", new JsonArray(resultsList))
                                        .put("config", scheduleRouteDestination));

                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                                reportQueryError(message, throwable);
                            }
                        });

                    } catch (Exception e){
                        e.printStackTrace();
                        reportQueryError(message, e);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
        });
    }

    private Future<List<String>> getSeatLocks(Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny) {
        Future<List<String>> future = Future.future();
        final JsonArray paramsList = new JsonArray()
                .add(0) // IntegrationPartnerSessionId: In this case we need every seat locked
                .add(scheduleRouteId)
                .add(orderOrigin).add(orderDestiny)
                .add(orderDestiny).add(orderDestiny)
                .add(orderOrigin).add(orderDestiny);

        this.dbClient.queryWithParams(QUERY_LOCKED_SEATING_BY_DESTINATION.concat(QUERY_ADD_GROUP_BY_LOCKED_SEATS), paramsList, replyList -> {
            try {
                if (replyList.failed()) {
                    future.fail(replyList.cause());
                    return;
                }

                List<JsonObject> resultsList = replyList.result().getRows();
                List<String> boardingSeats = resultsList.stream()
                        .map(p -> p.getString(SEAT)).collect(Collectors.toList());
                future.complete(boardingSeats);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
                future.fail(throwable);
            }

        });

        return future;
    }

    private Future<List<JsonObject>>  getLockedSeatsWithDestination(Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny) {
        Future<List<JsonObject>> future = Future.future();

        final JsonArray paramsList = new JsonArray()
                .add(0) // integrationTokenSessionId: In this case we need every seat locked
                .add(scheduleRouteId)
                .add(orderOrigin).add(orderDestiny)
                .add(orderDestiny).add(orderDestiny)
                .add(orderOrigin).add(orderDestiny);
        this.dbClient.queryWithParams(QUERY_LOCKED_SEATING_BY_DESTINATION, paramsList, replyList -> {
            try {
                if (replyList.failed()) {
                    future.fail(replyList.cause());
                    return;
                }

                List<JsonObject> resultsList = replyList.result().getRows();
                resultsList.forEach(res -> res.remove(SCHEDULE_ROUTE_ID));
                future.complete(resultsList);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
                future.fail(throwable);
            }
        });


        return future;
    }

    private CompletableFuture<Integer> searchSeat(Integer scheduleRouteDestinationId, String seat, Integer integrationPartnerSessionId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        if(scheduleRouteDestinationId == null){
            future.complete(0);
        }else {
            final JsonArray params = new JsonArray().add(scheduleRouteDestinationId);
            this.dbClient.queryWithParams(QUERY_GET_ORDERS_BY_DESTINATION.concat(GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_DESTINATION_PARAM), params, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.size() > 0) {
                        JsonObject scheduleRouteDestination = results.get(0);
                        Integer scheduleRouteId = scheduleRouteDestination.getInteger("schedule_route_id");
                        Integer orderOrigin = scheduleRouteDestination.getInteger("order_origin");
                        Integer orderDestiny = scheduleRouteDestination.getInteger("order_destiny");
                        final JsonArray paramsList = new JsonArray().add(scheduleRouteId)
                                .add(orderOrigin).add(orderDestiny)
                                .add(orderDestiny).add(orderDestiny)
                                .add(orderOrigin).add(orderDestiny)
                                .add(seat);
                        this.dbClient.queryWithParams(QUERY_SEARCH_AVAILABLE_SEATING_BY_DESTINATION, paramsList, replyList -> {
                            if (replyList.succeeded()) {
                                int count = replyList.result().getNumRows();
                                if (count > 0) {
                                    future.completeExceptionally(new Throwable("Seat busy: " + seat));
                                } else {

                                    this.searchLockedSeat(scheduleRouteId, orderOrigin, orderDestiny, seat, integrationPartnerSessionId).setHandler(replyLocked -> {
                                        try {
                                            if (replyLocked.failed()) {
                                                future.completeExceptionally(replyLocked.cause());
                                                return;
                                            }

                                            Integer locked = replyLocked.result();
                                            future.complete(count+locked);

                                        } catch (Throwable throwable) {
                                            throwable.printStackTrace();
                                            future.completeExceptionally(throwable);
                                        }
                                    });
                                }
                            } else {
                                future.completeExceptionally(replyList.cause());
                            }
                        });
                    } else {
                        future.completeExceptionally(new Throwable("Schedule route destination not found"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        }
        return future;
    }

    private Future<Integer> searchLockedSeat(Integer scheduleRouteId, Integer orderOrigin, Integer orderDestiny, String seat, Integer integrationPartnerSessionId) {
        Future<Integer> future = Future.future();

        try {
            integrationPartnerSessionId = Objects.isNull(integrationPartnerSessionId) ? 0 : integrationPartnerSessionId;
            final JsonArray paramsList = new JsonArray()
                    .add(integrationPartnerSessionId)
                    .add(scheduleRouteId)
                    .add(orderOrigin).add(orderDestiny)
                    .add(orderDestiny).add(orderDestiny)
                    .add(orderOrigin).add(orderDestiny)
                    .add(seat);
            this.dbClient.queryWithParams(QUERY_SEARCH_LOCKED_SEATING_BY_DESTINATION, paramsList, replyList -> {
                if (replyList.succeeded()) {
                    int count = replyList.result().getNumRows();
                    if (count > 0) {
                        future.fail(new Throwable("Seat busy: " + seat));
                    } else {
                        future.complete(count);
                    }
                } else {
                    replyList.cause().printStackTrace();
                    future.fail(replyList.cause());
                }
            });

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            future.fail(throwable);
        }
        return future;
    }

    public void publicReservationDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        body.put("isPublic", true);
        reservationDetail(message);
    }

    private void reservationDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        String reservationCode = body.getString("reservation_code");
        Boolean isPublic = body.getBoolean("isPublic", false);

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        Future<ResultSet> f3 = Future.future();
        Future<ResultSet> f4 = Future.future();
        Future<ResultSet> f5 = Future.future();


        JsonArray passengersParams = new JsonArray().add(reservationCode);

        String queryReservationDetail = isPublic ? QUERY_PUBLIC_RESERVATION_DETAIL : QUERY_RESERVATION_DETAIL;
        this.dbClient.queryWithParams(queryReservationDetail, passengersParams, f1.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_ROUTES, passengersParams, f2.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_PASSENGERS, passengersParams, f3.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_PAYMENT, passengersParams, f4.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_BRANCHOFFICE, passengersParams, f5.completer());

        CompositeFuture.all(f1, f2, f3, f4, f5).setHandler(reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet rsDetail = reply.result().resultAt(0);
                ResultSet rsRoutes = reply.result().resultAt(1);
                ResultSet rsPassengers = reply.result().resultAt(2);
                ResultSet rsPayment = reply.result().resultAt(3);
                ResultSet rsBranchoffice = reply.result().resultAt(4);
                if (rsDetail.getNumRows() > 0) {
                    JsonObject detail = rsDetail.getRows().get(0);
                    JsonObject payment = rsPayment.getNumRows() > 0 ? rsPayment.getRows().get(0) : new JsonObject();
                    detail.mergeIn(payment);
                    List<JsonObject> passengers = rsPassengers.getRows();
                    detail.put("passengers", passengers);
                    List<JsonObject> routes = rsRoutes.getRows();
                    detail.put("routes", routes);
                    List<JsonObject> branchoffices = rsBranchoffice.getRows();
                    detail.put("branchoffice", branchoffices.isEmpty() ? null : branchoffices.get(0));

                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                    for (JsonObject passenger : passengers){
                        tasks.add(getChildPassengerDetail(passenger));
                        tasks.add(getPassengerDetails(passenger, null));
                    }
                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[passengers.size()]))
                            .whenComplete((s, t) -> {
                                if (t != null) {
                                    this.reportQueryError(message, t.getCause());
                                } else {
                                    message.reply(detail);
                                }
                            });
                } else {
                    message.fail(ErrorCodes.DB_ERROR.ordinal(), "Element not found");
                }

            }catch (Exception ex) {
                ex.printStackTrace();
                this.reportQueryError(message, ex);
            }
        });
    }

    /**
     * RESERVATION DETAIL METHODS <!- START -!>
     */

    private void checkinTravelDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        String reservationCode = body.getString("reservation_code");
        Integer userBranchoffice = body.getInteger("user_branchoffice");

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        Future<ResultSet> f3 = Future.future();
        Future<ResultSet> f4 = Future.future();

        JsonArray detailParams =  new JsonArray()
                .add(userBranchoffice).add(userBranchoffice).add(userBranchoffice).add(reservationCode);
        JsonArray passengersParams = new JsonArray().add(reservationCode);
        this.dbClient.queryWithParams(QUERY_TRAVEL_CHECKIN_DETAIL, detailParams, f1.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_PASSENGERS, passengersParams, f2.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_PAYMENT, passengersParams, f3.completer());
        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_ALL_TICKETS, passengersParams, f4.completer());

        CompositeFuture.all(f1, f2, f3, f4).setHandler(reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet rsDetail = reply.result().resultAt(0);
                ResultSet rsPassengers = reply.result().resultAt(1);
                ResultSet rsPayment = reply.result().resultAt(2);
                ResultSet rsTickets = reply.result().resultAt(3);
                if (rsDetail.getNumRows() > 0) {
                    JsonObject detail = rsDetail.getRows().get(0);
                    if (detail.getInteger("in_travel_terminal").equals(1)){
                        JsonObject payment = rsPayment.getRows().get(0);
                        detail.mergeIn(payment);
                        List<JsonObject> passengers = rsPassengers.getRows();
                        detail.put("passengers", passengers);
                        detail.put("tickets", rsTickets.getRows());

                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                        for (JsonObject passenger : passengers) {
                            Integer boardingPassRouteId = passenger.getInteger("boarding_pass_route_id");
                            tasks.add(getPassengerDetails(passenger, boardingPassRouteId));
                        }
                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[passengers.size()]))
                                .whenComplete((s, t) -> {
                                    if (t != null) {
                                        this.reportQueryError(message, t.getCause());
                                    } else {
                                        message.reply(detail);
                                    }
                                });
                    } else {
                        message.fail(ErrorCodes.BUSINESS.ordinal(), "Can't do checkin in this terminal");
                    }
                } else {
                    JsonArray prepaidParams = new JsonArray().add(reservationCode);
                    this.dbClient.queryWithParams(QUERY_TRAVEL_PREPAID, prepaidParams, replyPrepaid -> {
                        try {
                            if(replyPrepaid.failed()) {
                                throw replyPrepaid.cause();
                            }
                            List<JsonObject> resultsPrepaid = replyPrepaid.result().getRows();
                            if(resultsPrepaid.isEmpty()) {
                                message.fail(ErrorCodes.DB_ERROR.ordinal(), "Element not found");
                            }
                            JsonObject res = new JsonObject();
                            res.put("results", resultsPrepaid.get(0));
                            message.reply(res.getJsonObject("results"));
                        } catch(Throwable t) {
                            this.reportQueryError(message, t);
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                this.reportQueryError(message, ex);
            }
        });
    }

    private CompletableFuture<JsonObject> getChildPassengerDetail(JsonObject passenger){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer boardingPassPassengerId = passenger.getInteger("id");
        JsonArray params = new JsonArray().add(boardingPassPassengerId);
        String query =  QUERY_CHILD_NAME_PASSENGER;
        this.dbClient.queryWithParams(query, params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                String childPassengers = null;
                if(reply.result().getNumRows() > 0){
                    childPassengers = reply.result().getRows().get(0).getString("child_under_age");

                }
                passenger.put("child_under_age",childPassengers);
                future.complete(passenger);
            }catch (Exception e ){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }



    private CompletableFuture<JsonObject> getPassengerDetails(JsonObject passenger, Integer boardingPassRouteId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer boardingPassPassengerId = passenger.getInteger("id");
        JsonArray params = new JsonArray().add(boardingPassPassengerId);

        String query = QUERY_RESERVATION_DETAIL_TICKETS;

        if (boardingPassRouteId != null) {
            params.add(boardingPassRouteId);
            query = QUERY_CHECKIN_TRAVEL_DETAIL_TICKETS;
        }

        this.dbClient.queryWithParams(query, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> tickets = reply.result().getRows();
                passenger.put("tickets", tickets);

                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for (JsonObject ticket : tickets) tasks.add(getTicketComplements(ticket));

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tickets.size()]))
                        .whenComplete((s, t) -> {
                            if (t != null) {
                                future.completeExceptionally(t.getCause());
                            } else {
                                future.complete(passenger);
                            }
                        });


            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }


    private CompletableFuture<JsonObject> getTicketComplements(JsonObject ticket) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer boardingPassTicketId = ticket.getInteger("id");
        JsonArray params = new JsonArray().add(boardingPassTicketId);

        this.dbClient.queryWithParams(QUERY_RESERVATION_DETAIL_COMPLEMENTS, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> complements = reply.result().getRows();
                ticket.put("complements", complements);
                future.complete(ticket);

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /**
     * RESERVATION DETAIL METHODS <!- END -!>
     */


    /**
     * CHECKIN METHODS <!- START -!>
     */

    private void makeCheckIn(Message<JsonObject> message) {
        this.checkIn(message, true);
    }

    private void calculateCheckIn(Message<JsonObject> message) {
        this.checkIn(message, false);
    }

    private void makeCheckInAgain(Message<JsonObject> message) {
        this.checkInAgain(message, true);
    }

    private void calculateCheckInAgain(Message<JsonObject> message){
        this.checkInAgain(message,false);
    }

    private void checkIn(Message<JsonObject> message, Boolean persist) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                final int boardingPassId = body.getInteger(ID);
                final int updatedBy = body.getInteger(UPDATED_BY);
                JsonArray passengers = (JsonArray) body.remove("passengers");
                JsonArray payments = body.containsKey("payments") ? (JsonArray) body.remove("payments") : new JsonArray();
                JsonObject cashChange = (JsonObject) body.remove("cash_change");
                Integer cashOutId = (Integer) body.remove(CASHOUT_ID);
                boolean flagPromo = persist && (boolean) body.remove(PromosDBV.FLAG_PROMO);
                JsonObject promoDiscount = persist ? (JsonObject) body.remove(DISCOUNT) : null;
                int expireOpenTicketsAfter = (int) body.remove("expire_open_tickets_after");
                final Boolean is_credit = (Boolean) body.containsKey("is_credit") ? ((Boolean) body.remove("is_credit")) : false;
                JsonObject customerCreditData = (JsonObject) body.remove("customer_credit_data");
                Integer customerId = body.getInteger(CUSTOMER_ID);
                Boolean checkedAlready = body.getBoolean("check_in_already");

                this.getBoardingPassById(conn, boardingPassId).whenComplete((JsonObject boardingPass, Throwable error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        Integer boardingPassStatus = boardingPass.getInteger("boardingpass_status");
                        String reservationCode = boardingPass.getString("reservation_code");
                        String createdAt = boardingPass.getString(CREATED_AT);

                        if (boardingPassStatus != 1 && boardingPassStatus != 2 && boardingPassStatus != 5 && boardingPassStatus != 4) {
                            throw new Exception("Status is not pre boarding or active");
                        }
                        String ticketType = boardingPass.getString("ticket_type");
                        Boolean completePurchase = true ;
                        if(ticketType.equals("abierto_sencillo") || ticketType.equals("abierto_redondo")){
                            if(boardingPassStatus == 4){
                                throw new Exception("Ticket type open cant be checked");
                            }
                            completePurchase = false;
                        }
//                        if(boardingPass.getString("payment_condition").equals("credit")) is_credit[0] = true;
                        Boolean finalCompletePurchase = completePurchase;
                        this.getTotalAmountTicketsForBoardingPassById(conn, boardingPassId, boardingPassStatus).whenComplete((JsonObject tickets, Throwable terror) -> {
                            try {
                                if (terror != null) {
                                    throw terror;
                                }

                                final Double amount = tickets.getDouble(AMOUNT);
                                final Double discount = tickets.getDouble(DISCOUNT);
                                final Double totalAmount = tickets.getDouble(TOTAL_AMOUNT);

                                final int len = passengers.size();
                                List<CompletableFuture<JsonObject>> tasks = IntStream.range(0, len)
                                        .mapToObj(passengers::getJsonObject)
                                        .map(passenger -> checkInPassenger(conn, passenger, boardingPassId, updatedBy, persist, customerId , checkedAlready))
                                        .collect(Collectors.toList());

                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len])).whenComplete((resultCheckInPassenger, errorCheckInPassenger) -> {
                                    try {
                                        if (errorCheckInPassenger != null) {
                                            throw errorCheckInPassenger;
                                        }
                                        // Calculate total for complements
                                        Double extraCharges = 0.0;
                                        JsonArray parcelPackagesDetail = new JsonArray();
                                        JsonArray parcelIds = new JsonArray();
                                        for (int i = 0; i < len; i++) {
                                            JsonObject passenger = passengers.getJsonObject(i);
                                            extraCharges += passenger.getDouble("extra_charges");
                                            if(passenger.containsKey("parcel_packages_detail")){
                                                parcelPackagesDetail.addAll((JsonArray) passenger.remove("parcel_packages_detail"));
                                            }
                                            if(passenger.containsKey("parcel_id")){
                                                parcelIds.add(new JsonObject().put(ID,passenger.remove("parcel_id")));
                                            }
                                        }

                                        // Get total previous payments
                                        Double finalExtraCharges = extraCharges;
                                        this.getTotalAmountPaymentsForBoardingPassById(conn, boardingPassId).whenComplete((JsonObject accPayments, Throwable pError) -> {
                                            try {
                                                if (pError != null) {
                                                    throw pError;
                                                }
                                                if (persist) {
                                                    this.getBoardingPassTicketsList(conn, boardingPassId).whenComplete((resultBPT, errorBPT) -> {
                                                        try {
                                                            if (errorBPT != null) {
                                                                throw errorBPT;
                                                            }

                                                            body.put(AMOUNT, amount)
                                                                    .put(DISCOUNT, discount)
                                                                    .put(TOTAL_AMOUNT, totalAmount);

                                                            boolean resetFlagPromo = (accPayments != null && flagPromo);

                                                            JsonObject bodyPromo = new JsonObject()
                                                                    .put(USER_ID, updatedBy)
                                                                    .put(FLAG_USER_PROMO, false)
                                                                    .put(PromosDBV.DISCOUNT, promoDiscount)
                                                                    .put(SERVICE, SERVICES.boardingpass)
                                                                    .put(BODY_SERVICE, body)
                                                                    .put(PRODUCTS, resultBPT)
                                                                    .put(OTHER_PRODUCTS, new JsonArray())
                                                                    .put(FLAG_PROMO, resetFlagPromo);
                                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                                try {
                                                                    if(replyPromos.failed()) {
                                                                        throw replyPromos.cause();
                                                                    }
                                                                    JsonObject resultApplyDiscount = replyPromos.result().body();
                                                                    JsonObject service = resultApplyDiscount.getJsonObject(PromosDBV.SERVICE);
                                                                    service.put("payment_condition", body.getString("payment_condition"))
                                                                            .put(TOTAL_AMOUNT, service.getDouble(TOTAL_AMOUNT) + finalExtraCharges);
                                                                    if(is_credit) service.put("debt", service.getDouble(TOTAL_AMOUNT));
                                                                    Double innerTotalAmount = UtilsMoney.round(service.getDouble(TOTAL_AMOUNT), 2);

                                                                    Double accPaymentsAmount = accPayments.getDouble(AMOUNT);
                                                                    Double totalPayments = UtilsMoney.round(accPaymentsAmount, 2);

                                                                    final int currencyId = body.getInteger(CURRENCY_ID);
                                                                    final double ivaPercent = body.getInteger("iva_percent");

                                                                    Double totalPaymentsPaid = 0.0;
                                                                    final int pLen = payments.size();
                                                                    for (int i = 0; i < pLen; i++) {
                                                                        JsonObject payment = payments.getJsonObject(i);
                                                                        payment.put("boarding_pass_id", boardingPassId);
                                                                        payment.put(CREATED_BY, updatedBy);
                                                                        Double paymentAmount = payment.getDouble(AMOUNT);
                                                                        if (paymentAmount == null || paymentAmount < 0.0) {
                                                                            this.rollback(conn, new Throwable("Invalid payment amount: " + paymentAmount), message);
                                                                            return;
                                                                        }
                                                                        totalPayments += paymentAmount;
                                                                        totalPaymentsPaid += paymentAmount;

                                                                    }
                                                                    totalPayments = UtilsMoney.round(totalPayments, 2);
                                                                    totalPaymentsPaid = UtilsMoney.round(totalPaymentsPaid, 2);

                                                                    if(!is_credit && !boardingPass.getString("payment_condition").equals("credit")) {
                                                                        if (totalPayments > innerTotalAmount) {
                                                                            throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
                                                                        }
                                                                        if (totalPayments < innerTotalAmount) {
                                                                            throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
                                                                        }
                                                                    }

                                                                    service.put(BOARDINGPASS_STATUS, 1)
                                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                                    service.remove("currency_id");
                                                                    service.remove("iva_percent");

                                                                    String expiresAt = null;
                                                                    if (ticketType.contains("abierto")){
                                                                        Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt), Calendar.DAY_OF_YEAR, expireOpenTicketsAfter);
                                                                        expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
                                                                    }
                                                                    service.put(EXPIRES_AT, expiresAt);

                                                                    GenericQuery update = this.generateGenericUpdate(SERVICES.boardingpass.getTable(), service, true);

                                                                    Double finalTotalPaymentsPaid = totalPaymentsPaid;
                                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                                        try {
                                                                            if (replyUpdate.failed()){
                                                                                throw replyUpdate.cause();
                                                                            }

                                                                            if(finalTotalPaymentsPaid == 0 && (boardingPassStatus.equals(1) || boardingPassStatus.equals(2) || boardingPassStatus.equals(5))){

                                                                                JsonObject finalResult = new JsonObject()
                                                                                        .put("reservation_code", reservationCode)
                                                                                        .put("parcels",parcelIds)
                                                                                        .put(DISCOUNT_APPLIED, flagPromo);
                                                                                this.commit(conn, message, finalResult);

                                                                            } else {
                                                                                String action = is_credit ? "voucher" : "purchase";
//                                                                                Double ticketTotalPayments = !is_credit ? finalTotalPaymentsPaid : body.getDouble("debt");
                                                                                // Insert ticket
                                                                                this.insertTicket(conn, action, boardingPassId, cashOutId, finalTotalPaymentsPaid, cashChange, updatedBy, ivaPercent, null, finalExtraCharges).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                                                    try {
                                                                                        if (ticketError != null) {
                                                                                            throw ticketError;
                                                                                        }
                                                                                        this.insertTicketDetail(conn, boardingPassId, ticket.getInteger("id"), finalCompletePurchase, finalExtraCharges, updatedBy, parcelPackagesDetail, boardingPassStatus).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                                            try {
                                                                                                if (dError != null) {
                                                                                                    throw dError;
                                                                                                }

                                                                                                if (is_credit) {
                                                                                                    Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
                                                                                                    Double creditAvailable = actualCreditAvailable - finalTotalPaymentsPaid;
                                                                                                    JsonObject customerObject = new JsonObject()
                                                                                                            .put(ID, customerCreditData.getInteger(ID))
                                                                                                            .put("credit_available", creditAvailable)
                                                                                                            .put(UPDATED_BY, updatedBy)
                                                                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                                                                    String updateCostumer = this.generateGenericUpdateString("customer", customerObject);
                                                                                                    conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCostumer) -> {
                                                                                                        try{
                                                                                                            if (replyCostumer.failed()) {
                                                                                                                throw replyCostumer.cause();
                                                                                                            }
                                                                                                            JsonObject finalResult = new JsonObject()
                                                                                                                    .put("reservation_code", reservationCode)
                                                                                                                    .put("credit_available", creditAvailable)
                                                                                                                    .put("ticket_id", ticket.getInteger("id"))
                                                                                                                    .put("parcels", parcelIds)
                                                                                                                    .put(DISCOUNT_APPLIED, flagPromo);
                                                                                                            this.commit(conn, message, finalResult);
                                                                                                        } catch (Throwable t) {
                                                                                                            t.printStackTrace();
                                                                                                            this.rollback(conn, t, message);
                                                                                                        }
                                                                                                    });

                                                                                                } else {
                                                                                                    // insert payments
                                                                                                    List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                                                                    for (int i = 0; i < pLen; i++) {
                                                                                                        JsonObject payment = payments.getJsonObject(i);
                                                                                                        payment.put("ticket_id", ticket.getInteger("id"));
                                                                                                        pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId, cashOutId, updatedBy));
                                                                                                    }
                                                                                                    CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));
                                                                                                    allPayments.whenComplete((ps, pt) -> {
                                                                                                        try {
                                                                                                            if (pt != null) {
                                                                                                                throw pt;
                                                                                                            }

                                                                                                            JsonObject finalResult = new JsonObject()
                                                                                                                    .put("reservation_code", reservationCode)
                                                                                                                    .put("ticket_id", ticket.getInteger("id"))
                                                                                                                    .put("parcels", parcelIds)
                                                                                                                    .put(DISCOUNT_APPLIED, flagPromo);
                                                                                                            this.commit(conn, message, finalResult);

                                                                                                        } catch (Throwable t) {
                                                                                                            t.printStackTrace();
                                                                                                            this.rollback(conn, t, message);
                                                                                                        }
                                                                                                    });
                                                                                                }

                                                                                            } catch (Throwable t) {
                                                                                                t.printStackTrace();
                                                                                                this.rollback(conn, t, message);
                                                                                            }
                                                                                        });
                                                                                    } catch (Throwable t){
                                                                                        t.printStackTrace();
                                                                                        this.rollback(conn, t, message);
                                                                                    }
                                                                                });
                                                                            }

                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            this.rollback(conn, t, message);
                                                                        }
                                                                    });
                                                                } catch (Throwable tx) {
                                                                    tx.printStackTrace();
                                                                    this.rollback(conn, tx, message);
                                                                }
                                                            });
                                                        } catch (Throwable tx){
                                                            tx.printStackTrace();
                                                            this.rollback(conn, tx, message);
                                                        }
                                                    });
                                                } else {
                                                    body.put("passengers", passengers)
                                                            .put("extra_charges", finalExtraCharges);
                                                    this.commit(conn, message, body);
                                                }
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }

    private CompletableFuture<JsonObject> checkInPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer updatedBy, Boolean persist, Integer customerId, Boolean checkAlready) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray boardingComplements = passenger.getJsonArray("complements");
        if (boardingComplements == null) {
            future.completeExceptionally(new Throwable("complements: missing for passenger " + passenger.getInteger("id")));
            return future;
        }
        Integer boardingPassTicketId = passenger.getInteger("boarding_pass_ticket_id");

        this.getDestinationByTicket(conn, boardingPassTicketId , checkAlready)
                .whenComplete((JsonObject destination, Throwable throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {

                        final int len = boardingComplements.size();
                        List<CompletableFuture<JsonObject>> tasks = IntStream.range(0, len)
                                .mapToObj(boardingComplements::getJsonObject)
                                .map(boardingComplement -> insertCheckInComplement(conn, passenger, boardingComplement, boardingPassId, boardingPassTicketId, destination, updatedBy, persist))
                                .collect(Collectors.toList());

                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len])).whenComplete((s, t) -> {
                            if (t != null) {
                                future.completeExceptionally(t.getCause());
                            } else {

                                // Calculate total for complements
                                Double extraCharges = 0.0;
                                Double extraWeight = 0.0;
                                Double extraLinearVolume = 0.0;
                                for (int i = 0; i < len; i++) {
                                    JsonObject boardingComplement = boardingComplements.getJsonObject(i);
                                    extraCharges += boardingComplement.getDouble("extra_charges");

                                    if(boardingComplement.containsKey("parcel_packages_detail")){
                                        passenger.put("parcel_packages_detail", boardingComplement.remove("parcel_packages_detail"));
                                    }
                                    if(boardingComplement.containsKey("parcel_id")){
                                        passenger.put("parcel_id", boardingComplement.remove("parcel_id"));
                                    }
                                    else {
                                        extraWeight += boardingComplement.getDouble("extra_weight");
                                        extraLinearVolume += boardingComplement.getDouble("extra_linear_volume");
                                    }
                                }
                                final Double finalExtraCharges = extraCharges;
                                final Double finalExtraWeight = extraWeight;
                                final Double finalExtraLinearVolume = extraLinearVolume;
                                this.getBoardingPassTicketById(conn, boardingPassTicketId)
                                        .whenComplete((JsonObject ticket, Throwable error) -> {
                                            Double ticketAmount = ticket.getDouble("amount");
                                            Double ticketDiscount = ticket.getDouble("discount");
                                            Double ticketTotalAmount = ticketAmount + finalExtraCharges - ticketDiscount;
                                            passenger.put("extra_charges", finalExtraCharges);

                                            if (persist) {
                                                GenericQuery update = this.generateGenericUpdate("boarding_pass_ticket", new JsonObject()
                                                        .put("id", boardingPassTicketId)
                                                        .put("check_in", true)
                                                        .put("extra_charges", finalExtraCharges)
                                                        .put("extra_weight", finalExtraWeight)
                                                        .put("extra_linear_volume", finalExtraLinearVolume)
                                                        .put("total_amount", ticketTotalAmount)
                                                        .put("checkedin_at", UtilsDate.sdfDataBase(new Date())));

                                                conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                                    try{
                                                        if(reply.failed()) {
                                                            throw new Exception(reply.cause());
                                                        }
                                                        future.complete(passenger);
                                                    }catch (Exception ex) {
                                                        ex.printStackTrace();
                                                        future.completeExceptionally(ex);
                                                    }
                                                });
                                            } else {
                                                future.complete(passenger);
                                            }
                                        });

                            }
                        });

                    }
                });

        return future;

    }

    private CompletableFuture<JsonObject> getCostBaggage(SQLConnection conn,
                                                         JsonObject boardingComplement,
                                                         int boardingPassId, int boardingPassTicketId,
                                                         JsonObject destination, Integer createdBy, Boolean persist){
        CompletableFuture<JsonObject> future = new CompletableFuture();

        Integer complementId = boardingComplement.getInteger("complement_id");
        JsonArray items = (JsonArray) boardingComplement.remove("items");
        if (items == null || items.size() == 0) {
            boardingComplement.put("extra_charges", 0);
            boardingComplement.put("extra_weight", 0);
            boardingComplement.put("extra_linear_volume", 0);
            future.complete(boardingComplement);
        } else {
            this.getComplementById(conn, complementId)
                    .whenComplete((JsonObject complement, Throwable error) -> {
                        if (error != null) {
                            future.completeExceptionally(error);
                        } else {
                            final int number = items.size();
                            List<String> inserts = new ArrayList<>();

                            boardingComplement
                                    .put("boarding_pass_id", boardingPassId)
                                    .put("boarding_pass_ticket_id", boardingPassTicketId)
                                    .put("created_by", createdBy);

                            double accLinearVolume = 0.0;
                            double accWeight = 0.0;
                            for (int i = 0; i < number; i++) {
                                JsonObject item = items.getJsonObject(i);
                                item.mergeIn(boardingComplement);

                                Double height = item.getDouble("height");
                                Double length = item.getDouble("length");
                                Double width = item.getDouble("width");
                                Double weight = item.getDouble("weight");
                                accLinearVolume += height + length + width;
                                accWeight += weight;

                                item.put("tracking_code", UtilsID.generateID("C"));
                                if (persist) {
                                    inserts.add(this.generateGenericCreate("boarding_pass_complement", item));
                                }
                            }

                            boardingComplement.put("linear_volume", accLinearVolume);
                            boardingComplement.put("weight", accWeight);
                            boardingComplement.put("number", number);
                            this.complementExtras(conn, boardingComplement, destination, items)
                                    .whenComplete((JsonObject complementUpdated, Throwable cError) -> {
                                        if (cError != null) {
                                            future.completeExceptionally(cError);
                                        } else {
                                            if (persist) {
                                                conn.batch(inserts, (AsyncResult<List<Integer>> reply) -> {
                                                    if (reply.succeeded()) {
                                                        future.complete(boardingComplement);
                                                    } else {
                                                        future.completeExceptionally(reply.cause());
                                                    }
                                                });
                                            } else {
                                                future.complete(boardingComplement);
                                            }

                                        }

                                    });
                        }

                    });
        }

        return future;
    }

    private CompletableFuture<JsonObject> checkInPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer updatedBy, Boolean persist, Boolean chofer, Integer shipmentId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray boardingComplements = passenger.getJsonArray("complements");
            if (boardingComplements == null) {
                throw new Exception("complements: missing for passenger " + passenger.getInteger(ID));
            }
            Integer boardingPassTicketId = passenger.getInteger("boarding_pass_ticket_id");


            final int len = boardingComplements.size();
            List<CompletableFuture<JsonObject>> tasks = IntStream.range(0, len)
                    .mapToObj(boardingComplements::getJsonObject)
                    .map(boardingComplement -> insertCheckInComplement(conn, boardingComplement, boardingPassId, boardingPassTicketId, updatedBy, persist, chofer, shipmentId))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len])).whenComplete((resultInsertCheckinComplement, errorInsertCheckinComplement) -> {
                try {
                    if (errorInsertCheckinComplement != null) {
                        throw errorInsertCheckinComplement;
                    }

                    // Calculate total for complements
                    Double extraCharges = 0.0;
                    Integer extraBaggage = 0;
                    for (int i = 0; i < len; i++) {
                        JsonObject boardingComplement = boardingComplements.getJsonObject(i);
                        extraBaggage += boardingComplement.getInteger("extra_baggage", 0);
                        extraCharges += boardingComplement.getDouble(EXTRA_CHARGES, 0.00);
                    }
                    final Integer finalExtraBaggage = extraBaggage;
                    final Double finalExtraCharges = extraCharges;
                    this.getBoardingPassTicketById(conn, boardingPassTicketId).whenComplete((JsonObject ticket, Throwable error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            Double ticketAmount = ticket.getDouble(AMOUNT);
                            Double ticketDiscount = ticket.getDouble(DISCOUNT);
                            Double ticketTotalAmount = ticketAmount + finalExtraCharges - ticketDiscount;
                            passenger.put("extra_baggage", finalExtraBaggage);
                            passenger.put(EXTRA_CHARGES, finalExtraCharges);

                            if (persist) {
                                GenericQuery update = this.generateGenericUpdate("boarding_pass_ticket", new JsonObject()
                                        .put(ID, boardingPassTicketId)
                                        .put("check_in", true)
                                        .put(EXTRA_CHARGES, finalExtraCharges)
                                        .put(TOTAL_AMOUNT, ticketTotalAmount)
                                        .put("ticket_status", 2)
                                        .put("checkedin_at", UtilsDate.sdfDataBase(new Date()))
                                        .put(UPDATED_BY, updatedBy)
                                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date())));

                                conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                    try{
                                        if(reply.failed()) {
                                            throw reply.cause();
                                        }

                                        if (shipmentId != null){

                                            List<String> updateShipmentTrackings = new ArrayList<>();

                                            JsonObject ticketShipTrack = new JsonObject()
                                                    .put("boarding_pass_ticket_id", boardingPassTicketId)
                                                    .put(STATUS, "loaded")
                                                    .put("shipment_id", shipmentId)
                                                    .put(CREATED_BY, updatedBy);
                                            updateShipmentTrackings.add(this.generateGenericCreate("shipments_ticket_tracking", ticketShipTrack));

                                            ticketShipTrack.put(STATUS, "in-transit");
                                            updateShipmentTrackings.add(this.generateGenericCreate("shipments_ticket_tracking", ticketShipTrack));

                                            conn.batch(updateShipmentTrackings, replyInsertTickShipTrack -> {
                                                try {
                                                    if (replyInsertTickShipTrack.failed()){
                                                        throw replyInsertTickShipTrack.cause();
                                                    }

                                                    future.complete(passenger);

                                                } catch (Throwable tr){
                                                    future.completeExceptionally(tr);
                                                }
                                            });
                                        } else {
                                            future.complete(passenger);
                                        }

                                    }catch (Throwable t) {
                                        t.printStackTrace();
                                        future.completeExceptionally(t);
                                    }
                                });
                            } else {
                                future.complete(passenger);
                            }
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

    private CompletableFuture<JsonObject> getDestinationByTicket(SQLConnection conn, Integer boardingPassTicketId , Boolean checkedAlready) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(boardingPassTicketId);
        conn.queryWithParams(QUERY_GET_ROUTE_DESTINATION_DISTANCE_KM, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet result = reply.result();
                if (result.getNumRows() == 0) {
                    future.completeExceptionally(new Throwable("Destination for boarding pass " + Integer.toString(boardingPassTicketId) + " ticket not found"));
                } else {
                    JsonObject ticketDestination = result.getRows().get(0);
                    Boolean checkIn = ticketDestination.getBoolean("check_in");
                    if (checkIn && !checkedAlready) {
                        future.completeExceptionally(new Throwable("Ticket " + Integer.toString(boardingPassTicketId) + " already checked in"));
                    } else {
                        future.complete(ticketDestination);
                    }
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);

            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getParcel (
            SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer boardingPassTicketId,
            Integer createdBy, Boolean persist, JsonObject complement
    ){
        CompletableFuture<JsonObject> future = new CompletableFuture();

        JsonArray params = new JsonArray().add(passenger.getInteger("id"));
        conn.queryWithParams(QUERY_GET_DETAIL_PARCEL_BY_PASSENGER, params, resultHandler -> {
            try{
                if(resultHandler.failed()) {
                    throw new Exception(resultHandler.cause());
                }
                if (resultHandler.result().getNumRows() > 0) {
                    JsonObject result = resultHandler.result().getRows().get(0);

                    getPackages(conn, result, boardingPassId, boardingPassTicketId, passenger, complement, createdBy, persist).whenComplete((packRes, packErr) -> {
                        if (packErr != null) {
                            future.completeExceptionally(packErr);
                        } else {
                            future.complete(packRes);
                        }
                    });

                } else {
                    future.completeExceptionally(new Throwable("No se encontro información del passenger, no se puede guardar los paquetes"));
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);

            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> registerParcel (JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture();
        try {
            EventBus eventBus = vertx.eventBus();
            body.put(SERVICE, SERVICES.parcel);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, REGISTER);

            JsonObject branchOrigin = new JsonObject().put("id", body.getInteger("terminal_origin_id"));
            JsonObject branchDestiny = new JsonObject().put("id", body.getInteger("terminal_destiny_id"));

            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();
            Future<Message<JsonObject>> f5 = Future.future();
            if (branchOrigin.getInteger("id") != null && branchDestiny.getInteger("id") != null) {
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchOrigin, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f1.completer());
                eventBus.send(BranchofficeDBV.class.getSimpleName(), branchDestiny, new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.IS_ACTIVE_BRANCH), f2.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
                eventBus.send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "parcel_iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());

                CompositeFuture.all(f1, f2, f3, f4, f5).setHandler(detailReply -> {
                    if (detailReply.succeeded()) {
                        Message<JsonObject> branch1Status = detailReply.result().resultAt(0);
                        Message<JsonObject> branch2Status = detailReply.result().resultAt(1);
                        Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(2);
                        Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(3);
                        Message<JsonObject> parcelIvaMsg = detailReply.result().resultAt(4);

                        JsonObject ivaPercent = ivaPercentMsg.body();
                        JsonObject currencyId = currencyIdMsg.body();
                        JsonObject parcelIva = parcelIvaMsg.body();

                        body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                        body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                        body.put("parcel_iva", Double.valueOf(parcelIva.getString("value")));

                        body.put("parcel_tracking_code", UtilsID.generateID("G"));
                        body.put("status", 0);

                        try {

                            differentValues(body, "terminal_origin_id", "terminal_destiny_id");
                            isStatusActive(branch1Status.body(), "status");
                            isStatusActive(branch2Status.body(), "status");

                            JsonArray parcelsPackages = body.getJsonArray("parcel_packages");
                            Integer scheduleRouteDestinationId = body.getInteger("schedule_route_destination_id");

                            doCalculateMultipleCost(parcelsPackages, scheduleRouteDestinationId)
                                    .whenComplete((packages, error) -> {
                                        if (error != null) {
                                            future.completeExceptionally(error);
                                        } else {
                                            body.put("parcel_packages", packages);
                                            eventBus.send(ParcelDBV.class.getSimpleName(), body, options, reply -> {
                                                if (reply.succeeded()) {

                                                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                                        future.completeExceptionally(new Throwable("Ocurrió un error inesperado, consulte con el proveedor de sistemas"));
                                                    } else {
                                                        JsonObject result = (JsonObject) reply.result().body();
                                                        future.complete(result);
                                                    }
                                                } else {
                                                    future.completeExceptionally(reply.cause());
                                                }
                                            });
                                        }
                                    });

                        } catch (UtilsValidation.PropertyValueException ex) {
                            future.completeExceptionally(ex);
                        }
                    } else {
                        future.completeExceptionally(detailReply.cause());
                    }
                });
            } else {
                future.completeExceptionally(new Throwable("Se deben especificar las sucursales tanto de origen como de destino."));
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }


    private CompletableFuture<JsonObject> getPackages (SQLConnection conn, JsonObject parcel, Integer
            boardingPassId, Integer boardingPassTicketId, JsonObject passenger, JsonObject complement, Integer
                                                               createdBy, Boolean persist){
        CompletableFuture<JsonObject> future = new CompletableFuture();

        JsonArray packages = (JsonArray) complement.remove("items");

        for (int i = 0; i < packages.size(); i++) {
            if (!packages.contains("packages_incidences")) {
                packages.getJsonObject(i).put("packages_incidences", new JsonArray());
            }
        }
        parcel.put("parcel_packages", packages);
        parcel.put("parcel_packings", complement.getJsonArray("parcel_packings"));
        getCostParcel(conn, parcel, boardingPassId, boardingPassTicketId, complement, createdBy, persist, passenger.getInteger("id")).whenComplete((cRes, cErr) -> {
            if (cErr != null) {
                future.completeExceptionally(cErr);
            } else {
                future.complete(cRes);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getCostParcel (SQLConnection conn, JsonObject parcel, Integer
            boardingPassId, Integer boardingPassTicketId, JsonObject complement, Integer createdBy, Boolean persist, Integer
                                                                 passenger_id){
        CompletableFuture<JsonObject> future = new CompletableFuture();
        JsonArray packages = parcel.getJsonArray("parcel_packages");
        Integer scheduleRouteDestinationId = parcel.getInteger("schedule_route_destination_id");
        doCalculateMultipleCost(packages, scheduleRouteDestinationId)
                .whenComplete((s, t) -> {
                    if (t != null) {
                        future.completeExceptionally(t);
                    } else {
                        parcel.put("pays_sender", 1);
                        parcel.put("created_by", createdBy);
                        parcel.put("amount", complement.containsKey("amount") ? complement.getDouble("amount") : 0.0);
                        parcel.put("total_amount", complement.containsKey("total_amount") ? complement.getDouble("total_amount") : 0.0);
                        complement.put("parcel", parcel);

                        Double extraCharges = 0.0;
                        Double extraWeight = 0.0;
                        Double accLinearVolume = 0.0;

                        List<String> inserts = new ArrayList<>();
                        JsonArray parcelPacakgesDetail = new JsonArray();
                        complement
                                .put("boarding_pass_id", boardingPassId)
                                .put("boarding_pass_ticket_id", boardingPassTicketId)
                                .put("created_by", createdBy);
                        JsonArray items = new JsonArray();
                        for (int i = 0; i < s.size(); i++) {
                            JsonObject item = new JsonObject()
                                    .put("width", s.getJsonObject(i).getDouble("width"))
                                    .put("height", s.getJsonObject(i).getDouble("height"))
                                    .put("length", s.getJsonObject(i).getDouble("length"))
                                    .put("shipping_type", s.getJsonObject(i).getString("shipping_type"))
                                    .put("weight", s.getJsonObject(i).getDouble("weight"));
                            parcelPacakgesDetail.add(s.getJsonObject(i));
                            item
                                    .put("complement_id", complement.getInteger("complement_id"))
                                    .put("boarding_pass_ticket_id", complement.getInteger("boarding_pass_ticket_id"))
                                    .put("created_by", complement.getInteger("created_by"))
                                    .put("boarding_pass_id", complement.getInteger("boarding_pass_id"));

                            items.add(item);

                            accLinearVolume += (item.getDouble("width") + s.getJsonObject(i).getDouble("height") + s.getJsonObject(i).getDouble("length"));
                            extraWeight += item.getDouble("weight");
                            extraCharges += s.getJsonObject(i).getDouble("cost");
                        }
                        complement.put("parcel_packages_detail", parcelPacakgesDetail);
                        complement.put("extra_linear_volume", accLinearVolume);
                        complement.put("extra_weight", extraWeight);
                        complement.put("extra_charges", extraCharges);
                        if (persist) {
                            registerParcel(parcel.put("is_complement", true)).whenComplete((rParcelRes, rParcelErr) -> {
                                if (rParcelErr != null) {
                                    future.completeExceptionally(rParcelErr);
                                } else {
                                    for (int i = 0; i < items.size(); i++) {
                                        JsonObject item = items.getJsonObject(i);
                                        item.put("tracking_code", rParcelRes.getString("tracking_code"));
                                        if (item.containsKey("package_type_id")) {
                                            item.remove("package_type_id");
                                        }
                                        inserts.add(this.generateGenericCreate("boarding_pass_complement", item));
                                    }
                                    conn.batch(inserts, (AsyncResult<List<Integer>> reply) -> {
                                        if (reply.succeeded()) {
                                            JsonArray params = new JsonArray()
                                                    .add(rParcelRes.getInteger("id"))
                                                    .add(createdBy)
                                                    .add(boardingPassTicketId)
                                                    .add(passenger_id);
                                            conn.queryWithParams(QUERY_UPDATE_BOARDING_PASS_TICKET_PARCEL_ID, params, replyUp -> {
                                                try{
                                                    if(reply.failed()) {
                                                        throw new Exception(reply.cause());
                                                    }
                                                    System.out.println("UPDATED boarding_pass_ticket insert parcel_id");
                                                    complement.put("parcel_id", rParcelRes.getInteger("id"));
                                                    future.complete(complement);
                                                }catch (Exception ex) {
                                                    ex.printStackTrace();
                                                    future.completeExceptionally(replyUp.cause());
                                                }
                                            });
                                        } else {
                                            future.completeExceptionally(reply.cause());
                                        }
                                    });
                                }
                            });
                        } else {
                            future.complete(complement);
                        }

                    }
                });
        return future;
    }

    private CompletableFuture<JsonArray> doCalculateMultipleCost (JsonArray packages, Integer
            scheduleRouteDestinationId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(packages.stream()
                .map(p -> calculateCost((JsonObject) p, scheduleRouteDestinationId))
                .toArray(CompletableFuture[]::new))
                .whenComplete((s, t) -> {
                    if (t != null) {
                        future.completeExceptionally(t);
                    } else {
                        future.complete(packages);
                    }
                });

        return future;
    }

    private CompletableFuture<JsonObject> calculateCost (JsonObject body, Integer scheduleRouteDestinationId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject params = new JsonObject();
        String shippingType = body.getString("shipping_type");
        linearVolume(shippingType, body).whenComplete((linear, error) -> {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                if (shippingType.equals("pets")) {
                    body.put("width", linear.getDouble("width"));
                    body.put("height", linear.getDouble("height"));
                    body.put("length", linear.getDouble("length"));
                }
                params.put("linear_volume", linear.getDouble("linear_volume"));
                params.put("weight", body.getDouble("weight"));
                params.put("schedule_route_destination_id", scheduleRouteDestinationId);

                params.put("insurance_value", body.getDouble("insurance_value"));
                params.put("packing_id", body.getInteger("packing_id"));
                params.put("shipping_type", shippingType);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelsPackagesDBV.CALCULATE_COST);
                vertx.eventBus().send(ParcelsPackagesDBV.class.getSimpleName(), params, options, (AsyncResult<Message<JsonObject>> reply) -> {
                    try{
                        if(reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        body.mergeIn(reply.result().body());
                        future.complete(body);
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
            }
        });


        return future;
    }

    private CompletableFuture<JsonObject> linearVolume (String ShippingType, JsonObject body){
        JsonObject result = new JsonObject();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (ShippingType.equals("parcel") || ShippingType.equals("frozen")) {
            Double width = body.getDouble("width");
            Double height = body.getDouble("height");
            Double length = body.getDouble("length");
            result.put("linear_volume", width + height + length);
            future.complete(result);
        } else if (ShippingType.equals("pets")) {
            Integer s = body.getInteger("pets_sizes_id");
            if (s != null && s > 0) {
                JsonObject bod = new JsonObject().put("id", s);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ParcelDBV.GET_PETSSIZES);
                vertx.eventBus().send(ParcelDBV.class.getSimpleName(), bod, options, handler -> {
                    try{
                        if(handler.failed()) {
                            throw new Exception(handler.cause());
                        }
                        JsonArray res = (JsonArray) handler.result().body();
                        JsonObject sizes = res.getJsonObject(0);
                        if (sizes != null && sizes.containsKey("height") && sizes.containsKey("width") && sizes.containsKey("length")) {
                            Double width = sizes.getDouble("width");
                            Double height = sizes.getDouble("height");
                            Double length = sizes.getDouble("length");
                            result.put("width", width);
                            result.put("height", height);
                            result.put("length", length);
                            result.put("linear_volume", width + height + length);
                            future.complete(result);
                        } else {
                            future.completeExceptionally(new Throwable("No se encontro el pets_sizes_id"));
                        }

                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
            } else {
                future.completeExceptionally(new Throwable("En el caso shipping_type='pets' el campo pets_sizes_id es requerido"));
            }
        } else {
            result.put("linear_volume", 0.0);
            future.complete(result);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertCheckInComplement (SQLConnection conn, JsonObject boardingComplement,
                                                                   int boardingPassId, int boardingPassTicketId, Integer createdBy, Boolean persist, Boolean chofer, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer complementId = boardingComplement.getInteger("complement_id");
        Integer items = (Integer) boardingComplement.remove("items");
        if (items == null || items == 0) {
            boardingComplement.put("extra_charges", 0);
            boardingComplement.put("extra_weight", 0);
            boardingComplement.put("extra_linear_volume", 0);
            future.complete(boardingComplement);
        } else {
            this.getGeneralSetting(conn, "driver_max_complements").whenComplete((resultDriverMaxComplements, errorDriverMaxComplements) -> {
               try {
                   if (errorDriverMaxComplements != null){
                       throw errorDriverMaxComplements;
                   }
                   Integer driverMaxComplements = Integer.parseInt(resultDriverMaxComplements.getValue("value").toString());

                   if (items > driverMaxComplements){
                       throw new Exception("Complements exceed limit");
                   }

                   this.getGeneralSetting(conn, "driver_free_complements_limit").whenComplete((resultDriverFreeCompLim, errorDriverFreeCompLim) -> {
                       try {
                           if (errorDriverFreeCompLim != null){
                               throw errorDriverFreeCompLim;
                           }
                           Integer driverFreeCompLim = Integer.parseInt(resultDriverFreeCompLim.getValue("value").toString());

                           this.getComplementById(conn, complementId).whenComplete((JsonObject complement, Throwable error) -> {
                               try {
                                   if (error != null){
                                       throw error;
                                   }

                                   JsonArray newItems = new JsonArray();
                                   for (int i = 0; i < items; i++) {
                                       newItems.add(new JsonObject()
                                               .put("weight", 0)
                                               .put("height", 0)
                                               .put("width", 0)
                                               .put("length", 0));
                                   }

                                   List<CompletableFuture> inserts = new ArrayList<>();

                                   boardingComplement
                                           .put("boarding_pass_id", boardingPassId)
                                           .put("boarding_pass_ticket_id", boardingPassTicketId)
                                           .put(CREATED_BY, createdBy);

                                   double accLinearVolume = 0.0;
                                   double accWeight = 0.0;

                                   JsonArray complementTrackingCodes = new JsonArray();
                                   for (int i = 0; i < newItems.size(); i++) {
                                       JsonObject item = newItems.getJsonObject(i);
                                       item.mergeIn(boardingComplement);

                                       String complementTrackingCode = UtilsID.generateID("C");
                                       item.put("tracking_code", complementTrackingCode);
                                       item.put("complement_status", 2);
                                       boardingComplement.put("tracking_code", complementTrackingCode);
                                       complementTrackingCodes.add(complementTrackingCode);
                                       if (persist) {
                                           inserts.add(this.insertDriverComplementAndShipmentTracking(conn, item, createdBy, shipmentId));
                                       }
                                   }

                                   boardingComplement.put("linear_volume", accLinearVolume);
                                   boardingComplement.put("weight", accWeight);
                                   boardingComplement.put("number", newItems.size());

                                   this.getGeneralSetting(conn, "baggage_cost").whenComplete((JsonObject generalSetting, Throwable cError) -> {
                                       try {
                                           if (cError != null) {
                                               throw cError;
                                           }

                                           Double extraPrice = Double.parseDouble(generalSetting.getValue("value").toString());

                                           if (newItems.size() > driverFreeCompLim) {
                                               Double extraCost = (newItems.size() - driverFreeCompLim) * extraPrice;
                                               boardingComplement.put("extra_baggage", newItems.size() - driverFreeCompLim);
                                               boardingComplement.put("extra_charges", extraCost);
                                           } else {
                                               boardingComplement.put("extra_charges", 0.00);
                                           }

                                           if (persist) {
                                               CompletableFuture.allOf(inserts.toArray(new CompletableFuture[inserts.size()])).whenComplete((result, errorInsertComplements) -> {
                                                   try {
                                                       if (errorInsertComplements != null){
                                                           throw errorInsertComplements;
                                                       }

                                                       boardingComplement.put("tracking_codes", complementTrackingCodes);

                                                       future.complete(boardingComplement);

                                                   } catch (Throwable t){
                                                       future.completeExceptionally(t);
                                                   }
                                               });
                                           } else {
                                               future.complete(boardingComplement);
                                           }
                                       } catch (Throwable t) {
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
                   });

               } catch (Throwable t){
                   future.completeExceptionally(t);
               }
            });
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertDriverComplementAndShipmentTracking(SQLConnection conn, JsonObject complement, Integer createdBy, Integer shipmentId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        String queryInsertComplement = this.generateGenericCreate("boarding_pass_complement", complement);
        conn.update(queryInsertComplement, replyInsertComplement -> {
            try {
                if (replyInsertComplement.failed()){
                    throw replyInsertComplement.cause();
                }

                Integer complementId = replyInsertComplement.result().getKeys().getInteger(0);
                complement.put(ID, complementId);

                if (shipmentId != null){
                    JsonObject complementShipTrack = new JsonObject().put("boarding_pass_complement_id", complementId)
                            .put(STATUS, "loaded")
                            .put(CREATED_BY, createdBy)
                            .put("shipment_id", shipmentId);

                    List<String> shipmentTrackings = new ArrayList<>();

                    shipmentTrackings.add(this.generateGenericCreate("shipments_complement_tracking", complementShipTrack));

                    complementShipTrack.put(STATUS, "in-transit");
                    shipmentTrackings.add(this.generateGenericCreate("shipments_complement_tracking", complementShipTrack));

                    conn.batch(shipmentTrackings, replyInsertShipmentTracking -> {
                        try {
                            if (replyInsertShipmentTracking.failed()){
                                throw replyInsertShipmentTracking.cause();
                            }

                            future.complete(complement);

                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    future.complete(complement);
                }

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertCheckInComplement(SQLConnection conn, JsonObject passenger, JsonObject boardingComplement,
                                                                  int boardingPassId, int boardingPassTicketId, JsonObject destination, Integer createdBy, Boolean persist) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Boolean is_parcel = boardingComplement.containsKey("is_parcel") ? boardingComplement.getBoolean("is_parcel") : false;

        if(is_parcel){
            getParcel(conn,  passenger, boardingPassId, boardingPassTicketId, createdBy, persist, boardingComplement).whenComplete((pRes,pErr)->{
                if(pErr!=null){
                    future.completeExceptionally(pErr);
                }else{
                    future.complete(pRes);
                }
            });
        }else{
            getCostBaggage(conn, boardingComplement, boardingPassId, boardingPassTicketId, destination, createdBy, persist).whenComplete((res,err)->{
                if(err != null){
                    future.completeExceptionally(err);
                }else{
                    future.complete(res);
                }
            });
        }
        return future;
    }


    private CompletableFuture<JsonObject> getGeneralSetting (SQLConnection conn, String fieldName){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(fieldName);
        conn.queryWithParams(QUERY_GET_CONFIG_BY_FIELD, params, reply -> {
            try {
                if (reply.succeeded()) {
                    List<JsonObject> settings = reply.result().getRows();
                    if (settings.isEmpty()) {
                        future.completeExceptionally(new Throwable("General "
                                + "setting not found: ".concat(fieldName)));
                    } else {
                        future.complete(settings.get(0));
                    }
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> complementExtras (SQLConnection conn, JsonObject
            boardingComplement, JsonObject destination, JsonArray items){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer complementId = boardingComplement.getInteger("complement_id");
        Integer number = boardingComplement.getInteger("number");

        String QUERY = "SELECT * FROM complement_rule \n" +
                "WHERE complement_id = ? AND max_quantity <= ? ORDER BY max_quantity DESC \n" +
                "LIMIT 1;";

        JsonArray params = new JsonArray().add(complementId).add(number);

        conn.queryWithParams(QUERY, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    future.completeExceptionally(new Throwable("Complement rule not found"));
                } else {

                    JsonObject rule = result.get(0);
                    Integer complementAccepted = rule.getInteger("max_quantity");
                    Double maxWeight = rule.getDouble("max_weight");
                    Double maxLinearVolume = rule.getDouble("max_linear_volume");
                    Double weight = boardingComplement.getDouble("weight");
                    Double linearVolume = boardingComplement.getDouble("linear_volume");

                    Double extraWeight = 0d;
                    Double extraLinearVolume = 0d;
                    if (complementAccepted == number) {
                        extraWeight = (weight > maxWeight) ? (weight - maxWeight) : 0d;
                        extraLinearVolume = (linearVolume > maxLinearVolume) ? (linearVolume - maxLinearVolume) : 0d;
                    } else {
                        double accLinearVolume = 0.0;
                        double accWeight = 0.0;

                        double acumLinearVolume = 0.0;
                        double acumWeight = 0.0;
                        for (int i = 0; i < number; i++) {
                            JsonObject item = items.getJsonObject(i);

                            Double heightItem = item.getDouble("height");
                            Double lengthItem = item.getDouble("length");
                            Double widthItem = item.getDouble("width");
                            Double weightItem = item.getDouble("weight");

                            if (i < complementAccepted) {
                                acumLinearVolume += heightItem + lengthItem + widthItem;
                                acumWeight += weightItem;
                                accWeight = (acumWeight > maxWeight) ? (acumWeight - maxWeight) : 0d;
                                accLinearVolume = (acumLinearVolume > maxLinearVolume) ? (acumLinearVolume - maxLinearVolume) : 0d;
                            } else {
                                accLinearVolume += heightItem + lengthItem + widthItem;
                                accWeight += item.getDouble("weight");
                            }

                        }
                        extraWeight = accWeight;
                        extraLinearVolume = accLinearVolume;
                    }
                    boardingComplement.put("extra_weight", extraWeight);
                    boardingComplement.put("extra_linear_volume", extraLinearVolume);
                    this.packagePrice(conn, boardingComplement, destination)
                            .whenComplete((JsonObject complementUpdated, Throwable error) -> {
                                if (error != null) {
                                    future.completeExceptionally(error);
                                } else {
                                    // Calculate extra charges
                                    Double pkgPrice = boardingComplement.getDouble("package_price");
                                    Double pkgPriceKM = boardingComplement.getDouble("package_price_km");
                                    Double amount = 0.0;
                                    if (boardingComplement.getDouble("extra_weight") > 0 || boardingComplement.getDouble("extra_linear_volume") > 0) {
                                        amount = (pkgPrice * pkgPriceKM);
                                    }
                                    boardingComplement.put("extra_charges", amount);
                                    future.complete(boardingComplement);
                                }
                            });
                }

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> packagePrice (SQLConnection conn, JsonObject
            boardingComplement, JsonObject destination){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Double extraWeight = boardingComplement.getDouble("extra_weight");
        Double extraLinearVolume = boardingComplement.getDouble("extra_linear_volume");

        String QUERY_LINEAR_VOLUME = "SELECT * FROM package_price \n" +
                "WHERE min_linear_volume <= ? AND max_linear_volume >= ?\n" +
                "ORDER BY price DESC LIMIT 1;";
        String QUERY_WEIGHT = "SELECT * FROM package_price \n" +
                "WHERE min_weight <= ? AND max_weight >= ? \n" +
                "ORDER BY price DESC LIMIT 1;";

        Future<ResultSet> futureLinearVolume = Future.future();
        Future<ResultSet> futureWeight = Future.future();

        JsonArray paramsLinearVolume = new JsonArray().add(extraLinearVolume).add(extraLinearVolume);
        JsonArray paramsWeight = new JsonArray().add(extraWeight).add(extraWeight);

        conn.queryWithParams(QUERY_LINEAR_VOLUME, paramsLinearVolume, futureLinearVolume.completer());
        conn.queryWithParams(QUERY_WEIGHT, paramsWeight, futureWeight.completer());

        CompositeFuture.all(futureLinearVolume, futureWeight).setHandler((AsyncResult<CompositeFuture> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet setLinearVolume = reply.result().resultAt(0);
                ResultSet setWeight = reply.result().resultAt(1);
                List<JsonObject> resultLinearVolume = setLinearVolume.getRows();
                List<JsonObject> resultWeight = setWeight.getRows();
                if (resultLinearVolume.isEmpty() || resultWeight.isEmpty()) {
                    future.completeExceptionally(new Throwable("Package price not found"));
                } else {
                    JsonObject pkgPriceLinearVolume = resultLinearVolume.get(0);
                    JsonObject pkgPriceWeight = resultWeight.get(0);
                    Double linearVolumePrice = pkgPriceLinearVolume.getDouble("price");
                    Double weightPrice = pkgPriceWeight.getDouble("price");
                    boardingComplement.put("package_price", linearVolumePrice > weightPrice ? linearVolumePrice : weightPrice);
                    this.packagePriceKM(conn, boardingComplement, destination)
                            .whenComplete((JsonObject complementUpdated, Throwable error) -> {
                                if (error != null) {
                                    future.completeExceptionally(error);
                                } else {
                                    future.complete(complementUpdated);
                                }
                            });
                }

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> packagePriceKM (SQLConnection conn, JsonObject
            boardingComplement, JsonObject destination){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Double distanceKm = destination.getDouble("distance_km");

        String QUERY = "SELECT * FROM package_price_km \n" +
                "WHERE min_km <= ? AND max_km >= ? \n" +
                "LIMIT 1;";

        JsonArray params = new JsonArray()
                .add(distanceKm).add(distanceKm);

        conn.queryWithParams(QUERY, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    future.completeExceptionally(new Throwable("Package price km not found"));
                } else {
                    JsonObject pkgPriceKM = result.get(0);
                    Double price = pkgPriceKM.getDouble("price");
                    boardingComplement.put("package_price_km", price);
                    future.complete(boardingComplement);
                }

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> CanCheckin (SQLConnection conn, Integer scheduleRouteDestinationId,
                                                   int timeBeforeCheckin, String timeCurrent){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        // get time to travel
        String QUERY = "select DATE_FORMAT(travel_date, '%Y-%m-%d %H:%i:%s') AS travel_date from schedule_route_destination where id=?;";


        if (scheduleRouteDestinationId == 0) {
            future.completeExceptionally(new Throwable("Schedule route not define"));
        } else {
            conn.queryWithParams(QUERY, new JsonArray()
                    .add(scheduleRouteDestinationId), reply -> {
                if (reply.succeeded()) {
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        future.completeExceptionally(new Throwable("Schedule route not found"));
                    } else {
                        JsonObject scheduleRouteDestination = result.get(0);

                        try {
                            Date current = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(timeCurrent, "DATABASE");
                            try {
                                Date travelDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(scheduleRouteDestination.getString("travel_date"), "DATABASE");
                                Date timeToCheckinMin = getTimeToCheckin(travelDate, timeBeforeCheckin, "hours");
                                Date timeToCheckinMax = getTimeToCheckin(travelDate, 5, "minutes");
                                Boolean canCheckinMin = UtilsDate.isLowerThanEqual(timeToCheckinMin, current);
                                Boolean canCheckinMax = UtilsDate.isGreaterThanEqual(timeToCheckinMax, current);
                                Boolean canCheckin = (canCheckinMin && canCheckinMax);
                                final Boolean canCheckinAlready = canCheckin;

                                System.out.print(current.toString() + ' ' + timeToCheckinMin.toString() + ' ' + timeToCheckinMax.toString() + (canCheckinMin && canCheckinMax));
                                future.complete(canCheckinAlready);

                            } catch (ParseException e) {
                                future.completeExceptionally(new Throwable("Schedule time has not right format " + scheduleRouteDestination.getString("travel_date")));
                            }
                        } catch (ParseException e) {
                            future.completeExceptionally(new Throwable("Time has not right format"));
                        }

                    }
                } else {
                    future.completeExceptionally(reply.cause());
                }
            });
        }

        return future;

    }

    public Date getTimeToCheckin (Date timeTravel,int timeBeforeCheckin, String type){
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(timeTravel);
        if (type.equals("hours")) {
            calendar.add(Calendar.HOUR, -timeBeforeCheckin);
        } else {
            calendar.add(Calendar.MINUTE, -timeBeforeCheckin);
        }

        return calendar.getTime();
    }
    /**
     * CHECKIN METHODS <!- END -!>
     */

    /**
     * CHANGE PASSENGER METHODS <!- START ->
     */

    private void changePassengers (Message < JsonObject > message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            String reservationCode = body.getString("reservation_code");
            Integer updatedBy = body.getInteger("updated_by");


            JsonArray passengers = body.getJsonArray("passengers");
            if (passengers == null || passengers.isEmpty()) {
                reportQueryError(message, new Throwable("Passengers: Missing required value"));
            } else if (reservationCode == null) {
                reportQueryError(message, new Throwable("Reservation code: Missing required value"));
            } else {

                JsonArray params = new JsonArray()
                        .add(reservationCode);

                String query = "SELECT * FROM boarding_pass " +
                        "WHERE reservation_code = ? " +
                        "AND status = 1 " +
                        "AND boardingpass_status = 1 " +
                        "LIMIT 1;";

                conn.queryWithParams(query, params, (AsyncResult<ResultSet> reply) -> {
                    try{
                        if(reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        List<JsonObject> rows = reply.result().getRows();
                        if (rows.isEmpty()) {
                            reportQueryError(message, new Throwable("Boarding pass: Not found"));
                        } else {
                            JsonObject boardingPass = rows.get(0);
                            Integer boardingPassId = boardingPass.getInteger("id");
                            CompletableFuture.allOf(passengers.stream()
                                    .map(p -> changeOnePassenger(conn, (JsonObject) p, boardingPassId, updatedBy))
                                    .toArray(CompletableFuture[]::new))
                                    .whenComplete((s, t) -> {
                                        if (t != null) {
                                            this.rollback(conn, t.getCause(), message);
                                        } else {
                                            this.commit(conn, message, new JsonObject()
                                                    .put("reservation_code", reservationCode)
                                                    .put("passengers", passengers)
                                            );
                                        }
                                    });
                        }
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        reportQueryError(message, ex);

                    }
                });
            }


        });
    }

    private CompletableFuture<JsonObject> changeOnePassenger (SQLConnection conn, JsonObject passenger, Integer
            boardingPassId, Integer updatedBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer oldPassengerId = (Integer) passenger.remove("old_passenger_id");

        getOldPassenger(conn, oldPassengerId, boardingPassId).whenComplete((oldPassenger, error) -> {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                GenericQuery query = this.generateGenericUpdate("boarding_pass_passenger",
                        new JsonObject()
                                .put("id", oldPassengerId)
                                .put("updated_by", updatedBy)
                                .put("updated_at", UtilsDate.sdfDataBase(new Date()))
                                .put("status", 3)
                );

                conn.updateWithParams(query.getQuery(), query.getParams(), (AsyncResult<UpdateResult> reply) -> {
                    try{
                        if(reply.failed()) {
                            throw new Exception(reply.cause());
                        }
                        JsonObject newPassenger = oldPassenger.copy();
                        newPassenger.remove("id");
                        newPassenger.put("first_name", passenger.getString("first_name"))
                                .put("last_name", passenger.getString("last_name"))
                                .put("gender", passenger.getInteger("gender"))
                                .put("birthday", passenger.getString("birthday"))
                                .put("created_by", updatedBy)
                                .put("created_at", UtilsDate.sdfDataBase(new Date()));

                        String insert = this.generateGenericCreate("boarding_pass_passenger", newPassenger);
                        conn.update(insert, (AsyncResult<UpdateResult> replyInsert) -> {
                            if (replyInsert.failed()) {
                                future.completeExceptionally(replyInsert.cause());
                            } else {
                                Integer newPassengerId = replyInsert.result().getKeys().getInteger(0);
                                newPassenger.put("id", newPassengerId);
                                passenger.put("id", newPassengerId);

                                updatePrincipalPassenger(conn, newPassenger).whenComplete((principalPassenger, principalError) -> {
                                    if (principalError != null) {
                                        future.completeExceptionally(principalError);
                                    } else {

                                        updateChildPassenger(conn, newPassenger, oldPassengerId).whenComplete((childPassenger, childError) -> {
                                            if (childError != null) {
                                                future.completeExceptionally(childError);
                                            } else {

                                                cloneTickets(conn, oldPassengerId, newPassengerId, updatedBy).whenComplete((ids, cloneError) -> {
                                                    if (cloneError != null) {
                                                        future.completeExceptionally(cloneError);
                                                    } else {
                                                        future.complete(newPassenger);
                                                    }
                                                });
                                            }

                                        });

                                    }

                                });

                            }
                        });
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> updatePrincipalPassenger (SQLConnection conn, JsonObject passenger){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Boolean principalPassenger = passenger.getBoolean("principal_passenger");

        if (!principalPassenger) {
            future.complete(passenger);
        } else {

            Integer boardingPassId = passenger.getInteger("boarding_pass_id");
            Integer principalPassengerId = passenger.getInteger("id");

            GenericQuery update = this.generateGenericUpdate("boarding_pass",
                    new JsonObject()
                            .put("id", boardingPassId)
                            .put("principal_passenger_id", principalPassengerId)
            );

            conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    future.complete(passenger);

                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);

                }
            });

        }

        return future;
    }

    private CompletableFuture<JsonObject> updateChildPassenger (SQLConnection conn, JsonObject passenger, Integer
            oldPassengerId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer passengerId = passenger.getInteger("id");

        String update = "UPDATE boarding_pass_passenger " +
                "SET parent_id = ? " +
                "WHERE parent_id = ?";

        JsonArray params = new JsonArray()
                .add(passengerId)
                .add(oldPassengerId);

        conn.updateWithParams(update, params, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                future.complete(passenger);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);

            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getOldPassenger (SQLConnection conn, Integer oldPassengerId, Integer
            boardingPassId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        if (oldPassengerId == null) {
            future.completeExceptionally(new Throwable("Passenger ID: Missing required value"));
        } else {
            String query = "SELECT * FROM boarding_pass_passenger " +
                    "WHERE id = ? " +
                    "AND status = 1 " +
                    "AND is_child_under_age = 0 " +
                    "AND boarding_pass_id = ? " +
                    "LIMIT 1;";

            JsonArray params = new JsonArray()
                    .add(oldPassengerId)
                    .add(boardingPassId);

            conn.queryWithParams(query, params, (AsyncResult<ResultSet> reply) -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        future.completeExceptionally(new Throwable("Passenger: Not found"));
                    } else {
                        future.complete(results.get(0));
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        }

        return future;
    }

    private CompletableFuture<List<JsonObject>> cloneTickets (SQLConnection conn, Integer oldPassengerId, Integer
            newPassengerId, Integer updatedBy){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        getOldTickets(conn, oldPassengerId).whenComplete((tickets, error) -> {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                CompletableFuture.allOf(tickets.stream()
                        .map(t -> replaceTicket(conn, t, newPassengerId, updatedBy))
                        .toArray(CompletableFuture[]::new))
                        .whenComplete((s, t) -> {
                            if (t != null) {
                                future.completeExceptionally(t.getCause());
                            } else {
                                future.complete(tickets);
                            }
                        });
            }
        });

        return future;
    }

    private CompletableFuture<List<JsonObject>> getOldTickets (SQLConnection conn, Integer oldPassengerId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();

        String querySelect = "SELECT * FROM boarding_pass_ticket " +
                "WHERE boarding_pass_passenger_id = ? AND check_in = 0 AND status = 1;";

        JsonArray params = new JsonArray()
                .add(oldPassengerId);

        conn.queryWithParams(querySelect, params, (AsyncResult<ResultSet> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> tickets = reply.result().getRows();
                if (tickets.isEmpty()) {
                    future.completeExceptionally(new Throwable("Tickets: Not found"));
                } else {
                    future.complete(tickets);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }


    private CompletableFuture<JsonObject> replaceTicket (SQLConnection conn, JsonObject ticket, Integer
            newPassengerId, Integer updatedBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonObject delete = new JsonObject()
                .put("id", ticket.getInteger("id"))
                .put("updated_at", UtilsDate.sdfDataBase(new Date()))
                .put("updated_by", updatedBy)
                .put("status", 3);

        GenericQuery query = this.generateGenericUpdate("boarding_pass_ticket", delete);

        conn.updateWithParams(query.getQuery(), query.getParams(), (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                cloneTicket(conn, ticket, newPassengerId, updatedBy).whenComplete((newTicket, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(newTicket);
                    }
                });

            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> cloneTicket (SQLConnection conn, JsonObject ticket, Integer
            newPassengerId, Integer updatedBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonObject newTicket = ticket.copy()
                .put("boarding_pass_passenger_id", newPassengerId)
                .put("created_by", updatedBy)
                .put("created_at", UtilsDate.sdfDataBase(new Date()));

        String string = this.generateGenericCreate("boarding_pass_ticket", newTicket);

        conn.update(string, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                future.complete(newTicket);
            }catch (Exception ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /**
     * CHANGE PASSENGER METHODS <!- END ->
     */

    private void travelDetail (Message < JsonObject > message) {
        String reservationCode = message.body().getString("reservation_code");
        JsonArray params = new JsonArray().add(reservationCode);

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();

        this.dbClient.queryWithParams(QUERY_TRAVEL_DETAIL, params, f1.completer());
        this.dbClient.queryWithParams(QUERY_TRAVEL_ROUTES_DETAIL, params, f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                ResultSet rsDetail = reply.result().resultAt(0);
                ResultSet rsRoutes = reply.result().resultAt(1);
                JsonObject detail = rsDetail.getRows().get(0);
                if (detail != null) {
                    detail.put("routes", rsRoutes.getRows());
                    message.reply(detail);
                } else {
                    message.fail(ErrorCodes.DB_ERROR.ordinal(), "Element not found");
                }


            }catch (Exception ex) {
                ex.printStackTrace();
                this.reportQueryError(message, ex);
            }
        });
    }

    private void travelsDetail (Message < JsonObject > message) {
        //Integer user_id = message.body().getInteger("user_id");
        Integer customer_id = message.body().getInteger("userId");
        String boardingpass_status = message.body().getString("boardingPassStatus");
        String QUERY_PARAMS = "";
        switch (boardingpass_status) {
            case "previous":
                QUERY_PARAMS = " AND bp.boardingpass_status = 3;";
                break;
            case "canceled":
                QUERY_PARAMS = " AND bp.boardingpass_status = 0;";
                break;
            case "reservation":
                QUERY_PARAMS = " AND bp.boardingpass_status = 4;";
                break;
            default:
                QUERY_PARAMS = " AND bp.boardingpass_status != 0 AND bp.boardingpass_status != 3 AND bp.boardingpass_status != 4;";
                break;
        }

        JsonArray param = new JsonArray().add(customer_id);

        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        Future<ResultSet> f3 = Future.future();
        this.dbClient.queryWithParams(QUERY_USER_TRAVELS + QUERY_PARAMS, param, f1.completer());
        this.dbClient.queryWithParams(QUERY_TRAVELS_ROUTES, param, f2.completer());
        this.dbClient.queryWithParams(QUERY_TRAVELS_TICKETS, param, f3.completer());

        CompositeFuture.all(f1, f2, f3).setHandler(reply -> {

            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> travels = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> travelsRoutes = reply.result().<ResultSet>resultAt(1).getRows();
                List<JsonObject> travelsTickets = reply.result().<ResultSet>resultAt(2).getRows();
                if (travels.size() == 0) {
                    message.fail(ErrorCodes.DB_ERROR.ordinal(), "Elements not found");
                } else {
                    for (JsonObject travel : travels) {
                        List<JsonObject> userTravelsRoutes = travelsRoutes.stream()
                                .filter(b -> b.getInteger("boarding_pass_id").equals(travel.getInteger("id")))
                                .collect(toList());

                        List<JsonObject> userTravelsTickets = travelsTickets.stream()
                                .filter(b -> b.getInteger("boarding_pass_id").equals(travel.getInteger("id")))
                                .collect(toList());

                        travel.put("routes", userTravelsRoutes);
                        travel.put("tickets", userTravelsTickets);
                    }
                    System.out.println(new JsonArray(travels));
                    message.reply(new JsonArray(travels));
                }


            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
        });

    }

    private void advancedSearch (Message < JsonObject > message) {
        JsonObject body = message.body();
        Boolean finished = body.getBoolean("include_finished");
        String firstName = body.getString("first_name");
        String lastName = body.getString("last_name");
        Integer terminalOriginId = body.getInteger("terminal_origin_id");
        Integer terminalDestinyId = body.getInteger("terminal_destiny_id");
        JsonArray param = new JsonArray()
                .add(firstName)
                .add(lastName)
                .add(terminalOriginId)
                .add(terminalDestinyId)
                .add(3); // Status Finished = 3

        String QUERY = QUERY_BOARDINGPASS_ADVANCED_SEARCH;

        if(finished){  QUERY = QUERY.concat(" AND bp.boardingpass_status = ? "); } else {  QUERY = QUERY.concat(" AND bp.boardingpass_status != ? AND bp.boardingpass_status != 4 "); }
        if (body.getBoolean("is_date_created_at") != null){
            Boolean isDateCreatedAt = body.getBoolean("is_date_created_at");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            param.add(initDate).add(endDate);
            if (isDateCreatedAt){
                QUERY = QUERY.concat(BOARDING_PASS_ADVANCED_SEARCH_CREATED_AT);
            } else {
                QUERY = QUERY.concat(BOARDING_PASS_ADVANCED_SEARCH_TRAVEL_DATE);
            }
            QUERY = QUERY.concat(BOARDING_PASS_ADVANCED_SEARCH_DATE_CONDITION);
        }

        this.dbClient.queryWithParams(QUERY, param, reply ->{
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                List<JsonObject> resultBoarding = reply.result().getRows();
                if (resultBoarding.isEmpty()) {
                    message.reply(new JsonArray());
                } else {
                    message.reply(new JsonArray(resultBoarding));
                }


            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);

            }
        });
    }

    //REPORT MONTH

    private void reportMonth(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd);
        String QUERY = QUERY_REPORT_MONTH;

        Integer originCityId = body.getInteger("origin_city_id");
        Integer originBranchofficeId =  body.getInteger("origin_branchoffice_id");
        Integer destinyCityId = body.getInteger("destiny_city_id");
        Integer destinyBranchofficeId = body.getInteger("destiny_branchoffice_id");
        Integer specialTickets = body.getInteger("special_tickets");
        String purchaseOrigin = body.getString("purchase_origin");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        Integer purchaseBranchofficeId = body.getInteger("purchase_branchoffice_id");


        if (originCityId != null) {
            params.add(originCityId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchofficeId != null) {
            params.add(originBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchofficeId != null) {
            params.add(destinyBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if(specialTickets != null){
            params.add(specialTickets);
            QUERY = QUERY.concat(SPECIAL_TICKET_ID);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchofficeId != null) {
                    params.add(purchaseBranchofficeId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }

        QUERY = QUERY.concat(REPORT_ORDER_BY);

        this.getReportMonth(QUERY, params).whenComplete((resultReport, errorReport) -> {
            if (errorReport != null) {
                reportQueryError(message, errorReport);
            } else {
                message.reply(resultReport);
            }
        });
    }

    private CompletableFuture<JsonArray> getReportMonth (String QUERY, JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params, replyReport -> {
            try{
                if (replyReport.succeeded()){
                    List<JsonObject> resultReport = replyReport.result().getRows();
                    List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                    resultReport.forEach(r -> {
                        tasks.add(this.getPaymentsInfo(r));
                    });
                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t) -> {
                        try {
                            if (t != null) {
                                throw new Exception(t);
                            }

                            this.execGetReportSpecialTicketGeneral(new JsonArray(resultReport)).whenComplete((resultSpecialTicket, errorSpecialTicket) -> {
                                if (errorSpecialTicket != null){
                                    future.completeExceptionally(errorSpecialTicket);
                                } else {
                                    future.complete(new JsonArray(resultReport));
                                }
                            });
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReport.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    private void priceListApply(Message<JsonObject> message){
        this.startTransaction(message, conn ->{
            JsonObject body = message.body().copy();
            Integer priceListId = body.getInteger("price_list_id");
            String hash = body.getString("hash");
            conn.queryWithParams("SELECT * FROM prices_lists WHERE id = ?", new JsonArray().add(priceListId), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Price list not found");
                    }
                    saveHashPriceList(priceListId, hash, message, handler ->{
                        conn.updateWithParams(UPDATE_IS_DEFAULT_BOARDINGPASS, new JsonArray().add(priceListId), replyUpdate ->{
                            try{
                                if (replyUpdate.failed()){
                                    throw new Exception(replyUpdate.cause());
                                }
                                conn.queryWithParams(LIST_PRICES_DETAIL.concat("  GROUP BY pld.terminal_origin_id, terminal_destiny_id, stops"), new JsonArray().add(priceListId), replyDetail ->{
                                    try{
                                        if (replyDetail.failed()){
                                            throw new Exception(replyDetail.cause());
                                        }
                                        List<JsonObject> applyPriceBody = new ArrayList<>();

                                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                                        List<JsonObject> resultDetail = replyDetail.result().getRows();
                                        for(int i = 0; i <  resultDetail.size();i++){
                                            JsonObject ticketsPrices = resultDetail.get(i);
                                            tasks.add(updateConfigTicketPrice(conn , ticketsPrices,applyPriceBody , message));
                                        }
                                        CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                                        all.whenComplete((resultt, error) -> {
                                            try {
                                                if (error != null){
                                                    throw error;
                                                }
                                                List<CompletableFuture<JsonObject>> tasksUpdate = new ArrayList<>();
                                                for(int x = 0; x < applyPriceBody.size();x++){
                                                    JsonObject priceBody = applyPriceBody.get(x);
                                                    tasksUpdate.add(updateConfigPrice(conn, priceBody, message));
                                                }
                                                CompletableFuture<Void> allUpdate = CompletableFuture.allOf(tasksUpdate.toArray(new CompletableFuture[tasksUpdate.size()]));
                                                allUpdate.whenComplete( (resultUpdate, errorr)->{
                                                    try{
                                                        if(errorr != null){
                                                            throw errorr;
                                                        }
                                                        conn.updateWithParams("UPDATE prices_lists SET apply_status = 'ok' WHERE hash = ? ", new JsonArray().add(hash), replyStatus ->{
                                                            try{
                                                                if(replyStatus.failed()){
                                                                    throw new Exception(replyStatus.cause());
                                                                }
                                                                this.commit(conn , message , new JsonObject().put("message" , "Price list is applied"));
                                                            }catch (Exception ex){
                                                                this.rollback(conn, ex, message);
                                                                setPriceListError(priceListId, hash, ex);
                                                            }

                                                        });
                                                    } catch (Throwable e){
                                                        e.printStackTrace();
                                                        this.rollback(conn, e, message);
                                                        setPriceListError(priceListId, hash, e);
                                                    }
                                                });
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                                setPriceListError(priceListId, hash, t);
                                            }
                                        });

                                    }catch (Exception e){
                                        e.printStackTrace();
                                        setPriceListError(priceListId, hash, e);
                                        this.rollback(conn, e, message);
                                    }
                                });
                            }catch (Exception e){
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                                setPriceListError(priceListId, hash, e);
                            }
                        });
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    this.rollback(conn, e, message);
                    setPriceListError(priceListId, hash, e);
                }
            });
         });
    }

    private void setPriceListError(Integer priceListLid, String hash, Throwable error) {
        JsonArray params = new JsonArray().add(hash)
            .add(error.getMessage())
            .add(priceListLid);
        dbClient.updateWithParams(QUERY_UPDATE_PRICE_LIST, params, reply -> {
                if (reply.failed()) {
                    reply.cause().printStackTrace();
                } else {
                    System.out.println("Error saved on price list: ".concat(String.valueOf(priceListLid)).concat(hash));
                }
        });
    }
    private void saveHashPriceList(Integer priceListId, String hash,Message<JsonObject> message,Handler<Integer> handler){
        dbClient.updateWithParams(UPDATE_HASH_PRICE_LIST, new JsonArray().add(hash).add(priceListId) , reply->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                handler.handle(1);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message , t);
            }
        });
    }

    private void registerPriceList(Message<JsonObject> message){
        this.startTransaction(message, conn -> {
            Integer priceListId = message.body().getInteger("id");
            Integer createdBy = message.body().getInteger("created_by");

            if(priceListId != null){
                updatePriceList(conn,priceListId , message, reply ->{
                    JsonObject result = reply;
                    JsonArray details = result.getJsonArray("destinations");
                    List<String> pricesInserts = new ArrayList<>();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();

                    for(int i = 0; i < details.size(); i++){
                        JsonObject detail = details.getJsonObject(i);
                        Integer terminalOriginId = detail.getInteger("terminal_origin_id");
                        Integer terminalDestinyId = detail.getInteger("terminal_destiny_id");
                        Integer stops = detail.getInteger("stops");
                        JsonArray specialTickets = detail.getJsonArray("special_tickets");
                        for(int x = 0; x < specialTickets.size(); x++){
                            JsonObject specialTicket = specialTickets.getJsonObject(x);
                            specialTicket
                                    .put("price_list_id" , priceListId)
                                    .put("terminal_origin_id", terminalOriginId)
                                    .put("terminal_destiny_id", terminalDestinyId)
                                    .put("stops", stops)
                                    .put("special_ticket_id", specialTicket.getInteger("id"))
                                    .put("created_by", createdBy);
                            tasks.add(updateSpecialTickets(conn, specialTicket, pricesInserts, message));
                        }
                    }
                    CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                    all.whenComplete((resultt, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            this.batchInsert( message , conn , pricesInserts , ids -> {
                                this.commit(conn , message , result.put("message" , "Price list updated"));
                            });
                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
                });
            }else{
                priceListRegister(conn, message, reply ->{
                    Integer createPriceId = reply.getInteger("id");
                    JsonObject result = reply;
                    JsonArray details = reply.getJsonArray("destinations");
                    for(int i = 0; i < details.size(); i++){
                        JsonObject detail = details.getJsonObject(i);
                        Integer terminalOriginId = detail.getInteger("terminal_origin_id");
                        Integer terminalDestinyId = detail.getInteger("terminal_destiny_id");
                        Integer stops = detail.getInteger("stops");
                        JsonArray specialTickets = detail.getJsonArray("special_tickets");
                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                        for(int x = 0; x < specialTickets.size(); x++){
                            JsonObject specialTicket = specialTickets.getJsonObject(x);
                            specialTicket
                                    .put("price_list_id" , createPriceId)
                                    .put("terminal_origin_id", terminalOriginId)
                                    .put("terminal_destiny_id", terminalDestinyId)
                                    .put("stops", stops)
                                    .put("created_by", createdBy);
                            tasks.add(insertListPricesDetail(conn, specialTicket,  message));
                        }
                        CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                        all.whenComplete((resultt, error) -> {
                            try {
                                if (error != null){
                                    throw error;
                                }
                                this.commit(conn, message, result.put("message", "Price list created"));
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    }
                });
            }
        });
    }
    private void batchInsert(Message<JsonObject> message, SQLConnection conn, List<String> inserts, Handler<List<Integer>> handler) {
        conn.batch(inserts, resultHandler -> {
            try{
                if(resultHandler.failed()) {
                    throw new Exception(resultHandler.cause());
                }
                List<Integer> insertIds = resultHandler.result();
                handler.handle(insertIds);
            }catch (Exception ex) {
                ex.printStackTrace();
                this.rollback(conn, ex, message);
            }

        });
    }

    private CompletableFuture<JsonObject> updateConfigPrice(SQLConnection conn, JsonObject detailPrice, Message<JsonObject> message){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
                JsonObject body = detailPrice;
                JsonArray tickets = body.getJsonArray("tickets");
                List<String> pricesInserts = new ArrayList<>();
                Integer configDestinationId = body.getInteger("config_destination_id");
                for (int i = 0, size = tickets.size(); i < size; i++) {
                    JsonObject ticket = tickets.getJsonObject(i);
                    Double total_amount =  ticket.getDouble("total_amount");
                    Double amount =  ticket.getDouble("amount");
                    Double discount = ticket.getDouble("discount");
                    if(discount == null){
                        discount = 0.0;
                    }
                    ticket.put("config_destination_id", configDestinationId);
                    ticket.remove("base");
                    String insert = this.generateGenericCreate(CONFIG_TICKET_PRICE, ticket);
                    insert += " ON DUPLICATE KEY UPDATE amount = " + amount + ", discount = " + discount + ", total_amount = " + total_amount + ", updated_by = " + body.getInteger(CREATED_BY) + ", updated_at = '" + UtilsDate.sdfDataBase(new Date()) + "'";
                    pricesInserts.add(insert);
                }
                this.batchInsert( message , conn , pricesInserts , ids -> {
                    future.complete(new JsonObject());
                });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }
    private CompletableFuture<JsonObject> updateConfigTicketPrice(SQLConnection conn, JsonObject ticketPrices,List<JsonObject> applyPriceBody ,Message<JsonObject> message){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            Integer terminalOriginId = ticketPrices.getInteger("terminal_origin_id");
            Integer terminalDestinyId = ticketPrices.getInteger("terminal_destiny_id");
            Integer stops = ticketPrices.getInteger("stops");
            JsonArray params = new JsonArray()
                    .add(terminalOriginId)
                    .add(terminalDestinyId)
                    .add(stops);
            conn.queryWithParams("SELECT id FROM config_destination AS cd where terminal_origin_id = ? AND terminal_destiny_id = ? AND abs(order_origin - order_destiny) - 1 = ?", params, reply->{
               try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (!result.isEmpty()){
                        conn.queryWithParams("SELECT special_ticket_id, discount, amount, total_amount,base FROM prices_lists_details WHERE terminal_origin_id = ? AND terminal_destiny_id = ? AND stops = ?", params, replyDetail ->{
                            try{
                                if(replyDetail.failed()){
                                    throw new Exception(replyDetail.cause());
                                }
                                List<JsonObject> resultDetail = replyDetail.result().getRows();
                                for(JsonObject priceBase : result){
                                    Integer configDestinationId = priceBase.getInteger("id");
                                        applyPriceBody.add(new JsonObject().put("config_destination_id",configDestinationId).put("tickets", resultDetail));
                                }
                                future.complete(new JsonObject());
                            }catch (Exception e){
                                e.printStackTrace();
                                future.completeExceptionally(e);
                            }
                        });
                    }else{
                        future.complete(new JsonObject());
                    }
               }catch (Exception ex){
                   ex.printStackTrace();
                   future.completeExceptionally(ex);
               }
            });
        }catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }
    private CompletableFuture<JsonObject> updateSpecialTickets(SQLConnection conn,JsonObject specialTicket, List<String> pricesInserts, Message<JsonObject> message) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer specialTicketId = (Integer) specialTicket.remove("id");

            conn.queryWithParams("SELECT base FROM special_ticket WHERE id = ?" , new JsonArray().add(specialTicketId),reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    Boolean isBase = reply.result().getRows().get(0).getBoolean("base");
                    specialTicket
                            .put("base", isBase);
                    Double amount = specialTicket.getDouble("amount");
                    Double discount = specialTicket.getDouble("discount");
                    Double total_amount = specialTicket.getDouble("total_amount");
                    String insert = this.generateGenericCreate("prices_lists_details", specialTicket);
                    insert += " ON DUPLICATE KEY UPDATE amount = " + amount + ", discount = " + discount + ", total_amount = " + total_amount + ", updated_by = " + specialTicket.getInteger("created_by") + ", updated_at = '" + UtilsDate.sdfDataBase(new Date()) + "'" + ", base = " + isBase;
                    pricesInserts.add(insert);
                    future.complete(new JsonObject());
                }catch (Exception e){
                    e.printStackTrace();
                    this.rollback(conn, e, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertListPricesDetail(SQLConnection conn,JsonObject specialTicket, Message<JsonObject> message) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer specialTicketId = (Integer) specialTicket.remove("id");

            conn.queryWithParams("SELECT base FROM special_ticket WHERE id = ?" , new JsonArray().add(specialTicketId),reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    Boolean isBase = reply.result().getRows().get(0).getBoolean("base");
                    specialTicket
                            .put("base", isBase)
                            .put("special_ticket_id", specialTicketId);
                    GenericQuery gc = this.generateGenericCreateSendTableName("prices_lists_details",specialTicket);
                    conn.updateWithParams(gc.getQuery() , gc.getParams() , repply ->{
                        try{
                            if(repply.failed()){
                                throw new Exception(repply.cause());
                            }
                            future.complete(new JsonObject());
                        }catch (Exception e ){
                            this.rollback(conn, e, message);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    this.rollback(conn,e, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
        return future;
    }
    private CompletableFuture<JsonObject> updatePriceList(SQLConnection conn,Integer priceListId ,Message<JsonObject> message, Handler<JsonObject> handler) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            //BUSCAR PRICE LIST
            conn.queryWithParams("SELECT * FROM prices_lists WHERE id = ?" , new JsonArray().add(priceListId), reply->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("Price list id not found");
                }
                JsonObject body = message.body();
                System.out.println(body);
                String name = body.getString("name");
                String description = body.getString("description");
                JsonObject updatePrice = new JsonObject()
                        .put("id",priceListId)
                        .put("name",name)
                        .put("description",description)
                        .put("updated_by",body.getInteger("created_by"))
                        .put("updated_at", UtilsDate.sdfDataBase(new Date()));;

                GenericQuery priceListUpdate = this.generateGenericUpdate("prices_lists", updatePrice);

                conn.updateWithParams(priceListUpdate.getQuery(), priceListUpdate.getParams(), priceListReply -> {
                    try{
                        if (priceListReply.failed()){
                            throw new Exception(priceListReply.cause());
                        }
                        handler.handle(body);
                    } catch (Exception e){
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                this.rollback(conn, e, message);
            }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
        return future;
    }
    private CompletableFuture<JsonObject> priceListRegister(SQLConnection conn, Message<JsonObject> message, Handler<JsonObject> handler) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject body = message.body();
            Integer createdBy = body.getInteger("created_by");
            String name = body.getString("name");
            String description = body.getString("description");
            JsonObject insertPrice = new JsonObject()
                    .put("name",name)
                    .put("description",description)
                    .put("created_by", createdBy);
            GenericQuery gc = this.generateGenericCreateSendTableName("prices_lists",insertPrice);
            conn.updateWithParams(gc.getQuery() , gc.getParams() , reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    int idPriceList = reply.result().getKeys().getInteger(0);
                    body.put("id",idPriceList);
                    handler.handle(body);
                }catch (Exception e ){
                    this.rollback(conn, e, message);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            this.rollback(conn, t, message);
        }
        return future;
    }

    private void priceListCount(Message<JsonObject> message){
        this.dbClient.queryWithParams("SELECT COUNT(apply_status) AS total, hash, id FROM prices_lists WHERE apply_status = 'pending'", new JsonArray(), reply->{
           try{
               if(reply.failed()){
                   throw new Exception(reply.cause());
               }
               JsonObject result = reply.result().getRows().get(0);
               message.reply(new JsonObject().put("count", result.getInteger("total")).put("hash", result.getString("hash")).put("id", result.getInteger("id")));
           } catch (Exception ex){
               ex.printStackTrace();
               reportQueryError(message, ex);
           }
        });
    }
    private void priceListStatus(Message<JsonObject> message){
        JsonObject body = message.body();
        String hash = body.getString("hash");
        this.dbClient.queryWithParams(LIST_PRICE_STATUS, new JsonArray().add(hash), repply->{
            try{
                if(repply.failed()){
                    throw new Exception(repply.cause());
                }
                List<JsonObject> specialTicketsBase = repply.result().getRows();
                if(specialTicketsBase.isEmpty()){
                    throw new Exception("hash not found");
                }
                JsonObject priceBase = specialTicketsBase.get(0);
                message.reply(priceBase);
            }catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private void pricesListDetail(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer priceListId = body.getInteger("price_list_id");
        this.dbClient.queryWithParams(LIST_PRICES_BASE, new JsonArray().add(priceListId), repply->{
            try{
                if(repply.failed()){
                    throw new Exception(repply.cause());
                }
                List<JsonObject> specialTicketsBase = repply.result().getRows();
                if(specialTicketsBase.isEmpty()){
                    throw new Exception("Price list not found");
                }
                this.dbClient.queryWithParams(LIST_PRICES_DETAIL , new JsonArray().add(priceListId), reply->{
                    try{
                        if(reply.failed()){
                            throw new Exception(reply.cause());
                        }
                        List<JsonObject> specialTickets = reply.result().getRows();
                        message.reply(new JsonObject().put("detail", specialTicketsBase.get(0)).put("destinations", specialTickets));
                    }catch (Exception e){
                        e.printStackTrace();
                        reportQueryError(message, e);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }
    private void pricesListRegistered(Message<JsonObject> message){
        this.dbClient.queryWithParams(LIST_PRICES_REGISTERED , new JsonArray(), reply->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> specialTickets = reply.result().getRows();
                if(specialTickets.isEmpty()){
                    throw new Exception("Special tickets not found");
                }
                message.reply(new JsonObject().put("list", specialTickets));
            }catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }
    private void pricesList(Message<JsonObject> message){
        this.dbClient.queryWithParams(LIST_SPECIAL_TICKET , new JsonArray(), reply->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> specialTickets = reply.result().getRows();
                if(specialTickets.isEmpty()){
                    throw new Exception("Special tickets not found");
                }
                this.dbClient.queryWithParams(SPECIAL_TICKET_DESTINATIONS, new JsonArray(), replyDestinations->{
                   try{
                       if(replyDestinations.failed()){
                           throw new Exception(replyDestinations.cause());
                       }
                       List<JsonObject> destinations = replyDestinations.result().getRows();
                       message.reply(new JsonObject().put("special_tickets", specialTickets).put("destinations", destinations));
                   } catch (Exception e){
                       e.printStackTrace();
                       reportQueryError(message, e.getCause());
                   }
                });
            }catch (Exception e){
                e.printStackTrace();
                reportQueryError(message, e.getCause());
            }
        });
    }

    private CompletableFuture<JsonArray> execGetReportSpecialTicketGeneral(JsonArray reports){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(reports.stream()
            .map(report -> this.getReportSpecialTicketGeneral((JsonObject) report))
            .toArray(CompletableFuture[]::new))
            .whenComplete((result, error) -> {
                if (error != null) {
                    future.completeExceptionally(error);
                } else {
                    future.complete(reports);
                }
            });
        return future;
    }

    private CompletableFuture<JsonArray> getReportSpecialTicketGeneral(JsonObject report){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer boardingPassId = report.getInteger("boarding_pass_id");
        this.dbClient.queryWithParams(REPORT_SPECIAL_TICKETS_GENERAL, new JsonArray().add(boardingPassId).add(boardingPassId), replyReportSpecialTickets -> {
            try{
                if(replyReportSpecialTickets.succeeded()){
                    List<JsonObject> resultSpecialTickets = replyReportSpecialTickets.result().getRows();
                    report.put("tickets", resultSpecialTickets);
                    this.execGetReportComplementsBySpecialTicket(new JsonArray(resultSpecialTickets), boardingPassId).whenComplete((resultComplements, errorComplements) -> {
                        if (errorComplements != null){
                            future.completeExceptionally(errorComplements);
                        } else {
                            future.complete(resultComplements);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReportSpecialTickets.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    //TERMINA REPORT MONTH

    public static String addOneDay(String date , int plus){
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDate
            .parse(date,formatter)
            .plusDays(plus)
            .toString();
    }
    private void reportApp(Message<JsonObject> message){
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        String origen = body.getString("origin");

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final long days = ChronoUnit.DAYS.between(LocalDate.parse(dateInit,formatter) ,LocalDate.parse(dateEnd,formatter) );
        //JsonArray params = new JsonArray().add(dateInit).add(dateEnd).add(origen);
        String QUERY = QUERY_APP_SITIO;


        ArrayList<JsonArray> entero = new ArrayList<JsonArray>();
        List<JsonObject> listini = new ArrayList<>();
        for(int count = 0 ; count < days ; count++){
            JsonArray params = new JsonArray().add(addOneDay(dateInit,count) + " 06:00:00").add(addOneDay(dateInit,count+1)+  " 05:59:59").add(origen);
            final int finCount = count + 1;
            this.getAppSitio(QUERY, params).whenComplete((resultReport, errorReport) -> {
                JsonObject listu = resultReport.getJsonObject(0);
                listini.add(listu);
                if (errorReport != null) {
                    reportQueryError(message, errorReport);
                } else {
                    if(finCount == days){
                        entero.add(new JsonArray(listini));
                        message.reply(new JsonArray(entero));
                    }
                }
            });
        }
    }
    
     private void reportGeneral(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        Integer branch = body.getInteger("branch");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        String appSite = body.getString("origin");

        String QUERY = QUERY_REPORT_GENERAL_V2;

        JsonArray params = new JsonArray().add(dateInit).add(dateEnd).add(dateInit).add(dateEnd);
        JsonArray paramsTask = new JsonArray().add(dateInit).add(dateEnd);

        if(purchaseCityId != null){
            params.add(purchaseCityId);
            paramsTask.add(purchaseCityId);
            paramsTask.add(branch);
            QUERY = QUERY.concat("AND bp.purchase_origin = 'sucursal' AND ci.id = ? ");
        }else{
            QUERY = QUERY.concat(" AND bp.purchase_origin = '" + appSite +"' " );
        }

        this.getReportGeneral(QUERY, params,REPORT_SPECIAL_TICKETS, body , paramsTask).whenComplete((resultReport, errorReport) -> {

            if (errorReport != null) {
                reportQueryError(message, errorReport);
            } else {
                message.reply(resultReport);
            }
        });
    }

    private CompletableFuture<JsonArray> getReportGeneral (String QUERY , JsonArray params , String queryTickets , JsonObject dateFilters , JsonArray paramsTask){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        this.dbClient.queryWithParams(QUERY, params, replyReport -> {
            try{
                if (replyReport.succeeded()){
                    List<JsonObject> resultReport = replyReport.result().getRows();
                    List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                    // JsonArray paramsExtra = new JsonArray().add(params.getValue(0)).add(params.getValue(1)).add(params.getValue(4));

                    resultReport.forEach(r -> {
                        tasks.add(this.getTicketsInfoGen(r, dateFilters));
                    });

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t) -> {
                        try {
                            if (t != null) {
                                throw new Exception(t);
                            }

                            this.execGetReportSpecialTicket(new JsonArray(resultReport), queryTickets).whenComplete((resultSpecialTicket, errorSpecialTicket) -> {
                                if (errorSpecialTicket != null){
                                    future.completeExceptionally(errorSpecialTicket);
                                } else {

                                    future.complete(new JsonArray(resultReport));

                                }
                            });

                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReport.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getTicketsInfoGen(JsonObject report, JsonObject filters){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer boardingPassId = report.getInteger("boarding_pass_id");
        String dateInit = filters.getString("init_date");
        String dateEnd = filters.getString("end_date");
        Integer originCityId = filters.getInteger("origin_city_id");
        Integer originBranchOfficeId =  filters.getInteger("origin_branchoffice_id");
        Integer destinyCityId = filters.getInteger("destiny_city_id");
        Integer destinyBranchOfficeId = filters.getInteger("destiny_branchoffice_id");
        Integer specialTickets = filters.getInteger("special_tickets");
        String purchaseOrigin = filters.getString("purchase_origin");
        Integer purchaseCityId = filters.getInteger("purchase_city_id");
        Integer purchaseBranchOfficeId = filters.getInteger("purchase_branchoffice_id");
        String travelType = filters.getString("ticket_type");

        JsonArray params = new JsonArray().add(boardingPassId).add(dateInit).add(dateEnd);
        String QUERY = QUERY_REPORT_GET_TICKETS_INFO_GEN;

        if(travelType != null){
            params.add(travelType);
            QUERY = QUERY.concat(REPORT_TICKET_TYPE_FILTER);
        }
        if (originCityId != null) {
            params.add(originCityId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchOfficeId != null) {
            params.add(originBranchOfficeId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchOfficeId != null) {
            params.add(destinyBranchOfficeId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if(specialTickets != null){
            params.add(specialTickets);
            QUERY = QUERY.concat(SPECIAL_TICKET_ID);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchOfficeId != null) {
                    params.add(purchaseBranchOfficeId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }

        Double extracharges = (Double) report.remove("extra_charges");
        JsonObject extraCharges = new JsonObject();
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                result.forEach(r -> {
                    if(!report.containsKey("purchase_date")) report.put("purchase_date", r.getString("created_at"));
                    if(!report.containsKey("dayFormat")) report.put("dayFormat", r.getString("dayFormat"));
                    Double totalAmount = r.getDouble("amount");
                    tasks.add(this.getPaymentsInfoByTicketId(r));
                    if(r.getInteger("has_extra") == 1 && r.getString(ACTION).equals("purchase")) {
                        extraCharges.put(CREATED_AT, r.getString(CREATED_AT))
                                .put(AMOUNT, extracharges)
                                .put(ACTION, "extra_charges");
                        r.put(AMOUNT, totalAmount - extracharges);
                    }
                });

                if(!extraCharges.isEmpty())
                    result.add(extraCharges);
                report.put("income_info", result);
                future.complete(new JsonArray(result));
            } catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPagos (JsonObject report , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(ULTIMO_QUERY, params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                result.forEach(r -> {
                    tasks.add(this.getPaymentsInfo(r));
                });
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s,t)-> {
                    try{
                        if(t != null){
                            throw new Exception(t);
                        }
                        report.put("pago_info",result);
                        future.complete(new JsonArray(result));
                    }catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
                //report.put("pago_info" , result);
                //future.complete(new JsonArray(result));
            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getSuma (JsonObject report , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_PARCELS_FPX, params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("suma_fpx" , result);
                future.complete(new JsonArray(result));
            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getAppSitio( String QUERY, JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                result.forEach(r -> {
                    tasks.add(this.getDateApp(r,params));
                    tasks.add(this.getBoardingCountApp(r,params));
                });
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((o,l) -> {
                    try{
                        if(l != null){
                            throw new Exception(l);
                        }
                        future.complete(new JsonArray(result));

                    }catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });

                //future.complete(new JsonArray(result));

            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getFPX (JsonObject report , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_FXC, params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("fxc_info" , result);
                future.complete(new JsonArray(result));
            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    //Webdev
    private CompletableFuture<JsonArray> getCobranza(JsonObject report , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_COBRANZA, params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("cobranza_info", result);
                future.complete(new JsonArray(result));
            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    //Webdev
    private CompletableFuture<JsonArray> getSobresGen(JsonObject report , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_SOBRES_GEN,params , reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("sobres_info",result);
                future.complete(new JsonArray(result));
            }catch (Exception e ){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getBoardingCountApp(JsonObject report,JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_BOARDINGPASS_COUNT_APP, params , reply ->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("amount_boardingpass",result);
                future.complete(new JsonArray(result));
            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getDateApp(JsonObject report,JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_BOARDINGPASS_DIA_APP, params , reply ->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("dia_app",result);
                future.complete(new JsonArray(result));
            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getCountBoardingpass(JsonObject report,JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_BOARDINGPASS_COUNT, params, reply ->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("amount_boardingpass",result);
                future.complete(new JsonArray(result));
            }catch(Exception e ){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> getPaymentsInfoAbordo(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_SALES_REPORT_PAYMENT_INFO_ABORDO, params, handler->{
            try {
                if (handler.succeeded()) {
                    List<JsonObject> result = handler.result().getRows();
                    parcel.put("payment_info", result);
                    future.complete(parcel);
                } else {
                    future.completeExceptionally(handler.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e.getCause());
            }
        });

        return future;
    }
    private CompletableFuture<JsonObject> getSalesPackagesAbordo(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_SALES_REPORT_PACKAGES_ABORDO, params, handler->{
            try {
                if (handler.succeeded()) {
                    List<JsonObject> resultsTracking = handler.result().getRows();
                    if (!resultsTracking.isEmpty()) {
                        parcel.put("packages", resultsTracking);
                    } else {
                        parcel.put("packages", new JsonArray());
                    }
                    future.complete(parcel);
                } else {
                    future.completeExceptionally(handler.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e.getCause());
            }
        });

        return future;
    }

    private String generateRandomUUID () {
        return UUID.randomUUID().toString();
    }

    private void cancelRegister (Message < JsonObject > message) {
        JsonObject body = message.body();
        Integer boardingPassId = body.getInteger("id");
        this.startTransaction(message, (SQLConnection conn) -> {
            String query = "SELECT id FROM boarding_pass WHERE id=? AND status=1 AND boardingpass_status=4;";
            final JsonArray params = new JsonArray().add(boardingPassId);
            conn.queryWithParams(query, params, reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    ResultSet result = reply.result();
                    if (result.getNumRows() == 0) {
                        this.rollback(conn, new Throwable("boarding pass not found"), message);
                    } else {
                        String update = "UPDATE boarding_pass SET boardingpass_status=0, status=3 WHERE id=? AND status=1 AND boardingpass_status=4;";
                        conn.updateWithParams(update, params, replyUpdate -> {
                            if (replyUpdate.succeeded()) {
                                this.commit(conn, message, new JsonObject().put("boarding_pass", new JsonObject().put("id", boardingPassId)));
                            } else {
                                this.rollback(conn, replyUpdate.cause(), message);
                            }
                        });
                    }


                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    private void runStatus(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String reservationCode = body.getString("reservation_code");

            JsonArray params = new JsonArray().add(reservationCode);

            this.dbClient.queryWithParams(QUERY_RUN_STATUS, params, reply -> {
                try {
                    if (reply.failed()) {
                        reportQueryError(message, reply.cause());
                    } else {
                        List<JsonObject> items = reply.result().getRows();
                        if (items.isEmpty()) {
                            reportQueryError(message, new Throwable("Reservation not found"));
                        } else {
                            message.reply(new JsonArray(items));
                        }
                    }
                } catch (Exception e) {
                    reportQueryError(message, e);
                }

            });
        } catch (Exception e) {
            reportQueryError(message, e);
        }
    }

    private void cancelRegisterByReservationCode(Message<JsonObject> message) {
        JsonObject body = message.body();
        String reservationCode = body.getString("reservation_code");
        this.startTransaction(message, (SQLConnection conn) -> {
            String query = "SELECT id FROM boarding_pass WHERE reservation_code=? AND status=1 AND boardingpass_status=4;";
            final JsonArray params = new JsonArray().add(reservationCode);
            conn.queryWithParams(query, params, reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    ResultSet result = reply.result();
                    if (result.getNumRows() == 0) {
                        this.rollback(conn, new Throwable("boarding pass not found"), message);
                    } else {
                        String update = "UPDATE boarding_pass SET boardingpass_status=0, status=3 WHERE reservation_code=? AND status=1 AND boardingpass_status=4;";
                        conn.updateWithParams(update, params, replyUpdate -> {
                            if (replyUpdate.succeeded()) {
                                this.commit(conn, message, new JsonObject().put("boarding_pass", new JsonObject().put("reservation_code", reservationCode)));
                            } else {
                                this.rollback(conn, replyUpdate.cause(), message);
                            }
                        });
                    }

                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }
    private void salesCancelReport(Message<JsonObject> message){
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd);
        String QUERY = QUERY_CANCEL_REPORT;

        Integer originCityId = body.getInteger("origin_city_id");
        Integer originBranchofficeId =  body.getInteger("origin_branchoffice_id");
        Integer destinyCityId = body.getInteger("destiny_city_id");
        Integer destinyBranchofficeId = body.getInteger("destiny_branchoffice_id");
        Integer specialTickets = body.getInteger("special_tickets");
        String purchaseOrigin = body.getString("purchase_origin");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        Integer purchaseBranchofficeId = body.getInteger("purchase_branchoffice_id");

        String ticketType = body.getString("ticket_type");
        Integer specialTicketId = body.getInteger("special_ticket_id");
        Integer terminalOriginId = body.getInteger("terminal_origin_id");
        Integer terminalDestinyCityId = body.getInteger("terminal_destiny_id");
        Integer terminalCityId = body.getInteger("terminal_city_id");
        Integer terminalId = body.getInteger("terminal_id");

        if(terminalCityId != null){
            params.add(terminalCityId);
            QUERY = QUERY.concat(" AND ci.id = ?");
        }
        if(terminalId != null){
            params.add(terminalId);
            QUERY = QUERY.concat(" AND b.id = ?");
        }
        if(terminalOriginId != null){
            params.add(terminalOriginId);
            QUERY = QUERY.concat(" AND bp.terminal_origin_id = ?");
        }
        if(terminalDestinyCityId != null){
            params.add(terminalDestinyCityId);
            QUERY = QUERY.concat(" AND bp.terminal_destiny_id = ?");
        }
        if(ticketType != null){
            params.add(ticketType);
            QUERY = QUERY.concat(" AND bp.ticket_type = ?");
        }
        if(specialTicketId != null){
            params.add(specialTicketId);
            QUERY = QUERY.concat(" AND st.id = ?");
        }
        if (originCityId != null) {
            params.add(originCityId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchofficeId != null) {
            params.add(originBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchofficeId != null) {
            params.add(destinyBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if(specialTickets != null){
            params.add(specialTickets);
            QUERY = QUERY.concat(SPECIAL_TICKET_ID);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchofficeId != null) {
                    params.add(purchaseBranchofficeId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }
        QUERY = QUERY.concat(" GROUP BY bp.reservation_code ").concat(REPORT_ORDER_BY);
        this.getReport(QUERY, params, REPORT_CANCEL_SPECIAL_TICKETS).whenComplete((resultReport, errorReport) -> {
            if (errorReport != null) {
                reportQueryError(message, errorReport);
            } else {
                message.reply(resultReport);
            }
        });
    }

    //reportPassengerType
    private void reportPassengerType(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
         JsonArray params = new JsonArray().add(dateInit).add(dateEnd).add(dateInit).add(dateEnd).add(dateInit).add(dateEnd);
        //JsonArray params = new JsonArray().add(dateInit).add(dateEnd);
        String QUERY = QUERY_REPORT;

        Integer originCityId = body.getInteger("origin_city_id");
        Integer originBranchofficeId =  body.getInteger("origin_branchoffice_id");
        Integer destinyCityId = body.getInteger("destiny_city_id");
        Integer destinyBranchofficeId = body.getInteger("destiny_branchoffice_id");
        Integer specialTickets = body.getInteger("special_tickets");
        String purchaseOrigin = body.getString("purchase_origin");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        Integer purchaseBranchofficeId = body.getInteger("purchase_branchoffice_id");
        String paymentCondition = body.getString("payment_condition");
        String travelType = body.getString("ticket_type");
        Integer sellerName = body.getInteger("seller_name_id");
        Integer customerName = body.getInteger("customer_id");

        if(travelType != null){
            params.add(travelType);
            QUERY = QUERY.concat(REPORT_TICKET_TYPE_FILTER);
        }
        if (originCityId != null) {
            params.add(originCityId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchofficeId != null) {
            params.add(originBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchofficeId != null) {
            params.add(destinyBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if(specialTickets != null){
            params.add(specialTickets);
            QUERY = QUERY.concat(SPECIAL_TICKET_ID);
        }

        if(sellerName != null){
             params.add(sellerName);
            QUERY = QUERY.concat(SELLER_NAME_ID);
        }


        if(customerName != null){
            params.add(customerName);
            QUERY = QUERY.concat(CUSTOMER_ID_REPORT);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchofficeId != null) {
                    params.add(purchaseBranchofficeId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }

        if(paymentCondition != null){
            params.add(paymentCondition);
            QUERY = QUERY.concat(REPORT_PAYMENT_CONDITION);
        }

        QUERY = QUERY.concat(" ORDER BY purchase_date ");

        this.getReportBoardingPassTicketType(QUERY, params, REPORT_SPECIAL_TICKETS_TYPE, body).whenComplete((resultReport, errorReport) -> {
            if (errorReport != null) {
                reportQueryError(message, errorReport);
            } else {
                message.reply(resultReport);
            }
        });
    }

    private CompletableFuture<JsonArray> getReportBoardingPassTicketType(String QUERY, JsonArray params, String queryTickets, JsonObject filters){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params, replyReport -> {
            try{
                if (replyReport.succeeded()){
                    List<JsonObject> resultReport = replyReport.result().getRows();

                    List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                    resultReport.forEach(r -> {
                        tasks.add(this.getTicketsInfo(r, filters));
                    });

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t) -> {
                        try {
                            if (t != null) {
                                throw new Exception(t);
                            }

                            this.execGetReportSpecialTicketAbordo(new JsonArray(resultReport), queryTickets).whenComplete((resultSpecialTicket, errorSpecialTicket) -> {
                                if (errorSpecialTicket != null){
                                    future.completeExceptionally(errorSpecialTicket);
                                } else {
                                    future.complete(new JsonArray(resultReport));
                                }
                            });
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReport.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> execGetReportSpecialTicketAbordo(JsonArray reports, String queryTickets){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(reports.stream()
                .map(report -> this.getReportSpecialTicketAbordo((JsonObject) report, queryTickets))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(reports);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonArray> getReportSpecialTicketAbordo(JsonObject report, String queryTickets){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer boardingPassId = report.getInteger("boarding_pass_id");
        this.dbClient.queryWithParams(queryTickets, new JsonArray().add(boardingPassId).add(boardingPassId), replyReportSpecialTickets -> {
            try{
                if(replyReportSpecialTickets.succeeded()){
                    List<JsonObject> resultSpecialTickets = replyReportSpecialTickets.result().getRows();
                    report.put("tickets", resultSpecialTickets);
                    this.execGetReportComplementsBySpecialTicket(new JsonArray(resultSpecialTickets), boardingPassId).whenComplete((resultComplements, errorComplements) -> {
                        if (errorComplements != null){
                            future.completeExceptionally(errorComplements);
                        } else {
                            // future.complete(resultComplements);
                            this.execGetReportRouteBySpecialTicket(new JsonArray(resultSpecialTickets)).whenComplete((resultRoute, errorRoute)-> {
                                if(errorRoute != null){
                                    future.completeExceptionally(errorRoute);
                                }else {
                                    future.complete(resultRoute);
                                }
                            });
                        }
                    });
                } else {
                    future.completeExceptionally(replyReportSpecialTickets.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> execGetReportRouteBySpecialTicket(JsonArray tickets){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(tickets.stream()
                .map(tick -> this.getReportRouteBySpecialTicket((JsonObject) tick))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(tickets);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonArray> getReportRouteBySpecialTicket (JsonObject ticket){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer routeId = ticket.getInteger("ruta_ticket");

        this.dbClient.queryWithParams(REPORT_ROUTE_BY_SPECIAL_TICKET, new JsonArray().add(routeId), replyReportTicket -> {
            try{
                if(replyReportTicket.succeeded()){
                    List<JsonObject> resultRoutes = replyReportTicket.result().getRows();
                    if (resultRoutes.isEmpty()){
                        ticket
                                .put("tipo_ruta", "No tiene")
                                .put("origin", "No tiene" )
                                .put("destiny", "No tiene");
                    } else {
                        JsonObject route = resultRoutes.get(0);
                        ticket
                                .put("tipo_ruta", route.getString("tipo_ruta") )
                                .put("origin", route.getString("origen"))
                                .put("destiny", route.getString("destino"));
                    }
                    future.complete(new JsonArray(resultRoutes));
                }else {
                    future.completeExceptionally(replyReportTicket.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });


        return future;
    }

    private void reportTravelFrequency(Message<JsonObject> message){
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd);
        String QUERY = QUERY_TRAVEL_FREQUENCY;

        this.getReportTravelFrequency(QUERY, params ).whenComplete(( resultReport , errorReport ) -> {
           if (errorReport != null){
               reportQueryError(message, errorReport);
           } else {
               message.reply(resultReport);
           }
        });
    }

    private CompletableFuture<JsonArray> getReportTravelFrequency(String QUERY , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY , params , replyReport -> {
           try{
               if(replyReport.succeeded()){
                   List<JsonObject> resultReport = replyReport.result().getRows();

                   List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                   resultReport.forEach(r -> {
                       tasks.add(this.getTravelsInfo( r , params ));
                       tasks.add(this.getCustomerInfo( r ));
                   });

                   CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete( (s , t) -> {
                      try{
                          if( t != null ){
                              throw new Exception(t);
                          }
                          future.complete(new JsonArray(resultReport));
                      }catch (Exception e){
                          future.completeExceptionally(e);
                      }
                   });
               }
           } catch (Exception e){
               future.completeExceptionally(e);
           }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getCustomerInfo( JsonObject report ){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer customerId = report.getInteger("customer_id");

        JsonArray param = new JsonArray().add(customerId);
        String QUERY = QUERY_REPORT_GET_CUSTOMER_INFO;

        this.dbClient.queryWithParams( QUERY , param , reply -> {
            try{
                if (reply.failed()){
                    throw  new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                report.put("name", result.get(0).getString("name"));
                report.put("birthday",result.get(0).getString("birthday"));
                report.put("email", result.get(0).getString("email"));
                report.put("phone", result.get(0).getString("phone"));
                future.complete(new JsonArray(result));
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonArray> getTravelsInfo(JsonObject report , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer customerId = report.getInteger("customer_id");

        JsonArray newParam = new JsonArray().add(params.getValue(0)).add(params.getValue(1)).add(customerId);;


        String QUERY = QUERY_REPORT_GET_TRAVELS_INFO;

        this.dbClient.queryWithParams( QUERY , newParam , reply -> {
            try{
                if (reply.failed()){
                    throw  new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                report.put("travel_info", result);
                future.complete(new JsonArray(result));
            }catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private void report(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        Integer limit = body.getInteger("limit");
        Integer page = body.getInteger("page");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd).add(dateInit).add(dateEnd).add(dateInit).add(dateEnd);
        String QUERY = QUERY_REPORT;

        Integer originCityId = body.getInteger("origin_city_id");
        Integer originBranchofficeId =  body.getInteger("origin_branchoffice_id");
        Integer destinyCityId = body.getInteger("destiny_city_id");
        Integer destinyBranchofficeId = body.getInteger("destiny_branchoffice_id");
        Integer specialTickets = body.getInteger("special_tickets");
        Integer specialTicketsIgnore = body.getInteger("special_tickets_ignore");
        String purchaseOrigin = body.getString("purchase_origin");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        Integer purchaseBranchofficeId = body.getInteger("purchase_branchoffice_id");
        String paymentCondition = body.getString("payment_condition");
        String travelType = body.getString("ticket_type");
        Integer sellerName = body.getInteger("seller_name_id");
        JsonArray sellers = body.getJsonArray("seller_array");
        //Integer customerName = body.getInteger("customer_id");
        JsonArray customers = body.getJsonArray("customer_array");

        if(travelType != null){
            params.add(travelType);
            QUERY = QUERY.concat(REPORT_TICKET_TYPE_FILTER);
        }
        if (originCityId != null) {
            params.add(originCityId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchofficeId != null) {
            params.add(originBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchofficeId != null) {
            params.add(destinyBranchofficeId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if(specialTickets != null){
            params.add(specialTickets);
            QUERY = QUERY.concat(SPECIAL_TICKET_ID);
        }

        if(specialTicketsIgnore != null){
            //params.add(specialTicketsIgnore);
            QUERY = QUERY.concat(SPECIAL_TICKET_ID_IGNORE + " AND t.total > 0 ");
        }

        if(sellerName != null){
            params.add(sellerName);
            QUERY = QUERY.concat(SELLER_NAME_ID);
        }

        if(sellerName != null){
            params.add(sellerName);
            QUERY = QUERY.concat(SELLER_NAME_ID);
        }

        if(sellers != null){
            String concatQuery = "";
            QUERY = QUERY.concat("AND bp.created_by in ( ");

            for(int i = 0; i < sellers.size() ; i++){
                Integer idSeller = sellers.getJsonObject(i).getInteger("id");
                if((sellers.size() - 1) == i){
                    concatQuery = concatQuery  +idSeller + ")";
                } else {
                    concatQuery = concatQuery + idSeller + ",";
                }
            }
            QUERY = QUERY.concat(concatQuery);
        }


        if(customers != null){

            String e = "";
            QUERY = QUERY.concat("AND bp.customer_id in ( ");

            for(int i = 0 ; i < customers.size() ; i++){
                Integer c = customers.getJsonObject(i).getInteger("id");
                if((customers.size() - 1) == i ){
                    e = e + c + ")";
                } else {
                    e = e + c + ",";
                }

            }
            QUERY = QUERY.concat(e);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchofficeId != null) {
                    params.add(purchaseBranchofficeId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }

        if(paymentCondition != null){
            params.add(paymentCondition);
            QUERY = QUERY.concat(REPORT_PAYMENT_CONDITION);
        }

        QUERY = QUERY.concat(" ORDER BY purchase_date");

        String QUERY_COUNT = "SELECT count(*) as count from ({QUERY_REPORT}) as f".replace("{QUERY_REPORT}", QUERY);
        JsonArray count_params = params.copy();

        if(page != null){
            QUERY = QUERY.concat(" LIMIT ?, ? ");
            params.add((page-1)*limit).add(limit);
        }

        this.getReportBoardingPass(QUERY, params, REPORT_SPECIAL_TICKETS, body).whenComplete((resultReport, errorReport) -> {
            if (errorReport != null) {
                reportQueryError(message, errorReport);
            } else {
                if(page!= null){
                    this.dbClient.queryWithParams(QUERY_COUNT, count_params, replyCount -> {
                        try {
                            if (replyCount.failed()) {
                                throw replyCount.cause();
                            }
                            JsonObject reply = replyCount.result().getRows().get(0);
                            JsonObject result = new JsonObject()
                                    .put("count", reply.getInteger("count"))
                                    .put("items", resultReport.size())
                                    .put("results", resultReport);

                            message.reply(result);

                        } catch (Throwable ex) {
                            reportQueryError(message, ex);
                        }
                    });
                }else{
                    message.reply(resultReport);
                }
            }
        });
    }

    private void reportTotals(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd).add(dateInit).add(dateEnd).add(dateInit).add(dateEnd);

        Integer originCityId = body.getInteger("origin_city_id");
        Integer originBranchofficeId =  body.getInteger("origin_branchoffice_id");
        Integer destinyCityId = body.getInteger("destiny_city_id");
        Integer destinyBranchofficeId = body.getInteger("destiny_branchoffice_id");
        Integer specialTickets = body.getInteger("special_tickets");
        Integer specialTicketsIgnore = body.getInteger("special_tickets_ignore");
        String purchaseOrigin = body.getString("purchase_origin");
        Integer purchaseCityId = body.getInteger("purchase_city_id");
        Integer purchaseBranchofficeId = body.getInteger("purchase_branchoffice_id");
        String paymentCondition = body.getString("payment_condition");
        String travelType = body.getString("ticket_type");
        Integer sellerName = body.getInteger("seller_name_id");
        //Integer customerName = body.getInteger("customer_id");
        JsonArray sellers = body.getJsonArray("seller_array");
        JsonArray customers = body.getJsonArray("customer_array");

        String OTHER_PARAMETERS = "";
        String QUERY_BOARDING_PASS = "SELECT boarding_pass_id from (SELECT DISTINCT\n" +
                "\tbp.id AS boarding_pass_id\n" +
                "\tFROM boarding_pass bp\n" +
                "\tLEFT JOIN tickets t ON bp.id = t.boarding_pass_id AND (t.action = 'purchase' OR t.action = 'change' OR t.action = 'voucher'  ) \n" +
                " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
                " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
                " LEFT JOIN city ci ON b.city_id = ci.id\n" +
                " LEFT JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
                " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
                " LEFT JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
                " LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
                " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
                " LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
                " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
                " LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
                " LEFT JOIN boarding_pass_passenger bpp ON bpp.boarding_pass_id = bp.id\n" +
                " LEFT JOIN invoice inv ON inv.id = bp.invoice_id AND inv.invoice_status = 'done' AND inv.is_global = false\n" +
                " LEFT JOIN promos pr ON pr.id = bp.promo_id\n" +
                "\tWHERE\n" +
                "\tbp.boardingpass_status != 0\n" +
                "\tAND bp.boardingpass_status != 4\n" +
                "\tAND (t.created_at BETWEEN ? AND ? \n" +
                "    OR bp.created_at BETWEEN ? AND ?) {OTHER_PARAMETERS} ) as F";

        if(travelType != null){
            params.add(travelType);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_TICKET_TYPE_FILTER);
        }
        if (originCityId != null) {
            params.add(originCityId);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchofficeId != null) {
            params.add(originBranchofficeId);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchofficeId != null) {
            params.add(destinyBranchofficeId);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if(specialTickets != null){
            params.add(specialTickets);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(SPECIAL_TICKET_ID);
        }
        if(specialTicketsIgnore != null){
            //params.add(specialTicketsIgnore);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(SPECIAL_TICKET_ID_IGNORE + " AND t.total > 0 ");
        }

        if(sellerName != null){
            params.add(sellerName);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(SELLER_NAME_ID);
        }

        if(sellers != null){

            String concatQuery = "";
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat("AND bp.created_by in ( ");

            for(int i = 0 ; i < sellers.size() ; i++){
                Integer idSeller = sellers.getJsonObject(i).getInteger("id");
                if((sellers.size() - 1) == i ){
                    concatQuery = concatQuery + idSeller + ")";
                } else {
                    concatQuery = concatQuery + idSeller + ",";
                }

            }
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(concatQuery);
        }

        if(customers != null){

            String e = "";
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat("AND bp.customer_id in ( ");

            for(int i = 0 ; i < customers.size() ; i++){
                Integer c = customers.getJsonObject(i).getInteger("id");
                if((customers.size() - 1) == i ){
                    e = e + c + ")";
                } else {
                    e = e + c + ",";
                }

            }
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(e);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchofficeId != null) {
                    params.add(purchaseBranchofficeId);
                    OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }

        if(paymentCondition != null){
            params.add(paymentCondition);
            OTHER_PARAMETERS = OTHER_PARAMETERS.concat(REPORT_PAYMENT_CONDITION);
        }

        QUERY_BOARDING_PASS = QUERY_BOARDING_PASS.replace("{OTHER_PARAMETERS}", OTHER_PARAMETERS);
        String QUERY_GET_TOTALS = "SELECT \n" +
                "COALESCE(sum(invoice_total_amount), 0) as total_invoice,\n" +
                "sum(boarding_pass_tickets) as num_tickets," +
                "sum(IF(action = 'change', 0, IF(has_extras = TRUE AND action = 'purchase' AND extra_charges != amount, amount - extra_charges, amount))) as purchases,\n" +
                "sum(IF(action = 'change', amount, IF(has_extras = TRUE AND action = 'purchase' AND extra_charges != amount, extra_charges,0))) as extras,\n" +
                "sum(amountCredit) as creditAmountTotal,\n" +
                "sum( credBoletos2) as creditTotalTickets, \n"+
                "sum(amountCash) as cashAmountTotal\n," +
                "sum(amount) as total\n" +
                "FROM (SELECT \n" +
                "   COALESCE(IF(bp.payment_condition = 'credit', t.total, 0)) as amountCredit,\n"+
                "   COALESCE(IF(bp.payment_condition = 'cash', t.total, 0)) as amountCash,\n"+
                " (select bpp2.id from boarding_pass bpp2 where bpp2.id = bp.id and bpp2.payment_condition = \"credit\") as credBoletos ,\n " +
               "   COALESCE((SELECT COUNT(bpt.id)\n" +
                "   FROM boarding_pass_ticket bpt\n" +
                "   INNER JOIN boarding_pass_passenger bbpp ON bbpp.id = bpt.boarding_pass_passenger_id \n" +
                "   INNER JOIN boarding_pass as bp0 ON bp0.id = bbpp.boarding_pass_id\n" +
                "   WHERE bbpp.status = 1 AND bbpp.boarding_pass_id = bp.id AND bp0.payment_condition = \"credit\"\n" +
                "   AND bpt.ticket_status != 0 AND NOT(action = 'change' OR (t.action = 'voucher' AND t.total = 0))), 0) credBoletos2,"+
                "   t.id,\n" +
                "   t.has_extras,\n" +
                "   COALESCE(t.extra_charges, 0) as extra_charges,\n" +
                "   COALESCE(t.total, 0) as amount,\n" +
                "   COALESCE((SELECT COUNT(bpt.id)\n" +
                "   FROM boarding_pass_ticket bpt\n" +
                "   INNER JOIN boarding_pass_passenger bbpp ON bbpp.id = bpt.boarding_pass_passenger_id\n" +
                "   WHERE bbpp.status = 1 AND bbpp.boarding_pass_id = bp.id\n"+
                "   AND bpt.ticket_status != 0 AND NOT(action = 'change' OR (t.action = 'voucher' AND t.total = 0))), 0) boarding_pass_tickets,\n" +
                "   bp.id as f,\n" +
                "   COALESCE(inv.total_amount, 0) AS invoice_total_amount,\n" +
                "   IF((SELECT SUM(bbpt.extra_charges) FROM boarding_pass bbp \n" +
                "       INNER JOIN boarding_pass_passenger bbpp ON bbpp.boarding_pass_id = bbp.id\n" +
                "       INNER JOIN boarding_pass_ticket bbpt ON bbpt.boarding_pass_passenger_id = bbpp.id\n" +
                "       WHERE bbp.id = bp.id AND bbp.status = 1 AND bbpp.status = 1 AND bbpt.status = 1\n" +
                "       GROUP BY bbp.id) = t.total AND t.total > 0, \n" +
                "       IF((SELECT COUNT(tt.id) FROM tickets tt \n" +
                "       WHERE tt.boarding_pass_id = bp.id AND tt.total > 0) != 1, 'extra_charges', 'purchase'), \n" +
                "            IF(t.action = 'voucher', 'purchase', t.action)) as action\n" +
                " FROM boarding_pass bp\n" +
                " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id AND (t.action = 'voucher' OR t.action = 'purchase' OR t.action = 'change')\n" +
                " AND t.created_at BETWEEN ? AND ?\n" +
                " LEFT JOIN invoice inv ON inv.id = bp.invoice_id AND inv.invoice_status = 'done' AND inv.is_global = false\n" +
                " WHERE bp.id IN ({QUERY_BOARDING_PASS})\n" +
                "    ) as F";
        this.dbClient.queryWithParams(QUERY_GET_TOTALS.replace("{QUERY_BOARDING_PASS}", QUERY_BOARDING_PASS) , params , reply ->{
            try{
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();

                message.reply(result.get(0));

            }catch (Exception e){
                reportQueryError(message, e);
            }
        });


    }
    
    private void specialTicketList(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer terminalOrigin = body.getInteger("terminal_origin");
        Integer terminalDestiny = body.getInteger("terminal_destiny");
        JsonArray params = new JsonArray().add(terminalOrigin).add(terminalDestiny);
        this.getSpecialTicketList(QUERY_SPECIAL_TICKET_LIST_HIGHER_PRICE, params).whenComplete( (result,error) ->{
            try{
                if(error != null){
                    throw new Exception(error);
                }
                Integer configDestinationId = result.get(0).getInteger("id_total");
                this.dbClient.queryWithParams(QUERY_DETAIL_HIGHER_PRICE, new JsonArray().add(configDestinationId), res ->{
                    try{
                        if(res.failed()){
                            throw new Exception(res.cause());
                        }
                        List<JsonObject> configResult = res.result().getRows();
                        message.reply(new JsonArray().add(new JsonObject().put("config_destination_id", configDestinationId).put("prices", configResult)));
                    } catch (Exception ex){
                        reportQueryError(message, ex);
                    }
                });

            }catch (Exception e){
                reportQueryError(message, e);
            }
        });
    }
    private CompletableFuture<JsonArray> getReport (String QUERY, JsonArray params, String queryTickets){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params, replyReport -> {
            try{
                if (replyReport.succeeded()){
                    List<JsonObject> resultReport = replyReport.result().getRows();


                    List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                    resultReport.forEach(r -> {
                        tasks.add(this.getPaymentsInfo(r));
                    });

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t) -> {
                        try {
                            if (t != null) {
                                throw new Exception(t);
                            }

                            this.execGetReportSpecialTicket(new JsonArray(resultReport), queryTickets).whenComplete((resultSpecialTicket, errorSpecialTicket) -> {
                                if (errorSpecialTicket != null){
                                    future.completeExceptionally(errorSpecialTicket);
                                } else {
                                    future.complete(new JsonArray(resultReport));
                                }
                            });
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReport.cause());
                }
           } catch(Exception e){
               future.completeExceptionally(e);
           }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getReportBoardingPass(String QUERY, JsonArray params, String queryTickets, JsonObject filters){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY, params, replyReport -> {
            try{
                if (replyReport.succeeded()){
                    List<JsonObject> resultReport = replyReport.result().getRows();

                    List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                    resultReport.forEach(r -> {
                        tasks.add(this.getTicketsInfo(r, filters));
                    });

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t) -> {
                        try {
                            if (t != null) {
                                throw new Exception(t);
                            }

                            this.execGetReportSpecialTicket(new JsonArray(resultReport), queryTickets).whenComplete((resultSpecialTicket, errorSpecialTicket) -> {
                                if (errorSpecialTicket != null){
                                    future.completeExceptionally(errorSpecialTicket);
                                } else {
                                    future.complete(new JsonArray(resultReport));
                                }
                            });
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReport.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<List<JsonObject>> getSpecialTicketList(String QUERY, JsonArray params){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY , params , reply ->{
            try{
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                if(reply.result().getNumRows() == 0){
                    future.completeExceptionally(new Throwable("route configuration not available"));
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("Prices list config terminals not found");
                }
                future.complete(result);
            }catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPaymentsInfo(JsonObject report){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_REPORT_GET_PAYMENT_INFO, new JsonArray().add(report.getInteger("boarding_pass_id")), reply -> {
           try {
               if (reply.failed()){
                   throw new Exception(reply.cause());
               }
               List<JsonObject> result = reply.result().getRows();
               report.put("payment_info", result);
               future.complete(new JsonArray(result));
           } catch (Exception e){
               future.completeExceptionally(e);
           }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPaymentsInfoByTicketId(JsonObject ticket){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer ticketId = (Integer) ticket.remove(ID);
        this.dbClient.queryWithParams(QUERY_REPORT_GET_PAYMENT_INFO_BY_TICKET_ID, new JsonArray().add(ticketId), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                ticket.put("payment_info", result);
                future.complete(new JsonArray(result));
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> execGetReportSpecialTicket(JsonArray reports, String queryTickets){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(reports.stream()
                .map(report -> this.getReportSpecialTicket((JsonObject) report, queryTickets))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(reports);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonArray> getReportSpecialTicket(JsonObject report, String queryTickets){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer boardingPassId = report.getInteger("boarding_pass_id");
        this.dbClient.queryWithParams(queryTickets, new JsonArray().add(boardingPassId).add(boardingPassId), replyReportSpecialTickets -> {
            try{
                if(replyReportSpecialTickets.succeeded()){
                    List<JsonObject> resultSpecialTickets = replyReportSpecialTickets.result().getRows();
                    report.put("tickets", resultSpecialTickets);
                    this.execGetReportComplementsBySpecialTicket(new JsonArray(resultSpecialTickets), boardingPassId).whenComplete((resultComplements, errorComplements) -> {
                        if (errorComplements != null){
                            future.completeExceptionally(errorComplements);
                        } else {
                            future.complete(resultComplements);
                        }
                    });
                } else {
                    future.completeExceptionally(replyReportSpecialTickets.cause());
                }
           } catch(Exception e){
                future.completeExceptionally(e);
           }
        });
        return future;
    }

    private CompletableFuture<JsonArray> execGetReportComplementsBySpecialTicket(JsonArray specialTickets, Integer boardingPassId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(specialTickets.stream()
                .map(specialTicket -> this.getReportComplementsBySpecialTicket((JsonObject) specialTicket, boardingPassId))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(specialTickets);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonArray> getReportComplementsBySpecialTicket (JsonObject specialTicket, Integer boardingPassId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer specialTicketId = specialTicket.getInteger("special_ticket_id");
        String trackingCode = specialTicket.getString("tracking_code");
        this.dbClient.queryWithParams(REPORT_COMPLEMENTS_BY_SPECIAL_TICKET, new JsonArray().add(boardingPassId).add(specialTicketId).add(trackingCode), replyReportComplements -> {
            try{
                if(replyReportComplements.succeeded()){
                    List<JsonObject> resultComplements = replyReportComplements.result().getRows();
                    if (resultComplements.isEmpty()){
                        specialTicket
                                .put("complements_quantity", 0)
                                .put("complements_weight", 0.00)
                                .put("complements_linear_volume", 0.00)
                                .put("complements_ volume", 0.00);
                    } else {
                        JsonObject complement = resultComplements.get(0);
                        specialTicket
                                .put("complements_quantity", complement.getInteger("quantity"))
                                .put("complements_weight", complement.getDouble("weight"))
                                .put("complements_linear_volume", complement.getDouble("linear_volume"))
                                .put("complements_ volume", complement.getDouble("volume"));
                    }
                    future.complete(new JsonArray(resultComplements));
                } else {
                    future.completeExceptionally(replyReportComplements.cause());
                }
           } catch(Exception e){
                future.completeExceptionally(e);
           }
        });
        return future;
    }

    private void occupationReport(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Boolean isPassengerTrip = body.getBoolean("is_passenger_trip");
            Integer terminalOriginId = body.getInteger(TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = body.getInteger("terminal_destiny_id");
            Integer vehicleId = body.getInteger(VEHICLE_ID);
            JsonArray scheduleRouteId = body.getJsonArray(SCHEDULE_ROUTE_ID);
            JsonArray params = new JsonArray();

            if(isPassengerTrip != null && isPassengerTrip){

                String query = QUERY_BOARDING_PASS_OCUPATION_IS_PASSENGER_TRIP;
                params.add(initDate).add(endDate);

                if(terminalOriginId != null){
                    query = query.concat("AND srd.terminal_origin_id = ?\n");
                    params.add(terminalOriginId);
                }

                if(terminalDestinyId != null){
                    query = query.concat("AND srd.terminal_destiny_id = ?");
                    params.add(terminalDestinyId);
                }

                if (scheduleRouteId != null){
                    String[] array = (String[]) scheduleRouteId.getList().stream().map(obj -> obj.toString()).toArray(String[]::new);
                    query = query.concat(String.format(" AND sr.id IN (%S) ",String.join(",", array)));
                }

                if(vehicleId != null){
                    query = query.concat(" AND v.id = ? ");
                    params.add(vehicleId);
                }

                this.dbClient.queryWithParams(query, params, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }

                        message.reply(new JsonArray(reply.result().getRows()));

                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });

            }else{
                this.getScheduleRouteAndOrdersList(initDate, endDate, scheduleRouteId, terminalOriginId, terminalDestinyId).whenComplete((resultGSRO, errorGSRO) -> {
                    try {
                        if (errorGSRO != null){
                            throw errorGSRO;
                        }

                        String QUERY_REPORT = this.createOcuppationReportQuery(resultGSRO);

                        if(vehicleId != null){
                            QUERY_REPORT = QUERY_REPORT.concat(" AND v.id = ? ");
                            params.add(vehicleId);
                        }

                        this.dbClient.queryWithParams(QUERY_REPORT, params, reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }

                                message.reply(new JsonArray(reply.result().getRows()));

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
    }

    private String createOcuppationReportQuery(JsonArray resultGSRO){
        String QUERY_PARAMS_ORDER = "";

        for(int i = 0; i < resultGSRO.size(); i++){
            JsonObject param = resultGSRO.getJsonObject(i);
            Integer paramSRDID = param.getInteger(SCHEDULE_ROUTE_ID);
            Integer paramOO = param.getInteger(ORDER_ORIGIN);
            Integer paramOD = param.getInteger(ORDER_DESTINY);

            QUERY_PARAMS_ORDER = i == 0 ? QUERY_PARAMS_ORDER.concat(" AND (") : QUERY_PARAMS_ORDER.concat(" OR ");

            QUERY_PARAMS_ORDER = QUERY_PARAMS_ORDER.concat("(sr.id = {scheduleRouteId} " +
                    " AND ((cd.order_destiny > {orderOrigin} AND cd.order_destiny <= {orderDestiny})\n" +
                    " OR ({orderDestiny} > cd.order_origin AND {orderDestiny} <= cd.order_destiny)\n" +
                    " OR (cd.order_origin = {orderOrigin} AND cd.order_destiny = {orderDestiny})))");

            QUERY_PARAMS_ORDER = QUERY_PARAMS_ORDER.replace("{scheduleRouteId}", paramSRDID.toString())
                    .replace("{orderOrigin}", paramOO.toString())
                    .replace("{orderDestiny}", paramOD.toString());

        }

        if(!resultGSRO.isEmpty()){
            QUERY_PARAMS_ORDER = QUERY_PARAMS_ORDER.concat(" ) ");
        }

        return QUERY_OCCUPATION_REPORT.concat(QUERY_PARAMS_ORDER);
    }

    private CompletableFuture<JsonArray> getScheduleRouteAndOrdersList(String initDate, String endDate, JsonArray scheduleRouteId, Integer terminalOriginId, Integer terminalDestinyId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();

        String QUERY = "SELECT sr.id AS schedule_route_id, cd.order_origin, cd.order_destiny FROM schedule_route sr\n" +
                "LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
                "LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
                "WHERE sr.travel_date BETWEEN ? AND ?\n";

        JsonArray params = new JsonArray().add(initDate).add(endDate);

        if(terminalOriginId != null){
            QUERY = QUERY.concat("AND srd.terminal_origin_id = ?\n");
            params.add(terminalOriginId);
        }

        if(terminalDestinyId != null){
            QUERY = QUERY.concat("AND srd.terminal_destiny_id = ?\n");
            params.add(terminalDestinyId);
        }

        if (scheduleRouteId != null){
            QUERY = QUERY.concat(" AND sr.id IN (?) ");
            String[] array = (String[]) scheduleRouteId.getList().stream().map(obj -> obj.toString()).toArray(String[]::new);
            params.add(String.join(",", array));
        }

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> results = reply.result().getRows();

                if(results.isEmpty()){
                   throw new Exception("Schedule routes and orders not found");
                }

                future.complete(new JsonArray(results));

            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void cancelPartialReservation(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                Integer boardingPassPassengerId = (Integer) body.remove("boarding_pass_ticket_id");
                String reservationCode = body.getString(RESERVATION_CODE);
                String notes = (String) body.remove(NOTES);
                Integer updatedBy = body.getInteger(UPDATED_BY);

                this.getBoardingPassByReservationCode(reservationCode, null).whenComplete((resultBoardingPass, errorBoardingPass) -> {
                    try {
                        if (errorBoardingPass != null) {
                            throw errorBoardingPass;
                        }

                        Integer principalPassengerId = resultBoardingPass.getInteger("principal_passenger_id");
                        Integer boardingPassId = resultBoardingPass.getInteger(ID);
                        body.put(ID, boardingPassId);

                        boolean isPrincipalPassenger = principalPassengerId.equals(boardingPassPassengerId);

                        conn.queryWithParams(GET_PASSENGERS_BY_BOARDING_PASS_ID, new JsonArray().add(boardingPassId), (AsyncResult<ResultSet> replyPassengers) -> {
                            try {
                                if (replyPassengers.failed()) {
                                    throw replyPassengers.cause();
                                }
                                List<JsonObject> rows = replyPassengers.result().getRows();
                                Integer seatings = 1;
                                JsonObject newPrincipalPassenger;
                                if (rows.isEmpty()) throw new Exception("Passengers: Not Found");
                                else if (rows.size() == 1) {
                                    body.put(BOARDINGPASS_STATUS, 0);
                                    resultBoardingPass.put(BOARDINGPASS_STATUS, 0);
                                    newPrincipalPassenger = rows.get(0);
                                } else {
                                    seatings = rows.size() - 1;
                                    newPrincipalPassenger = rows.get(1);
                                }

                                JsonObject finalNewPrincipalPassenger = newPrincipalPassenger;
                                body.put("seatings", seatings);

                                this.cancelPartialPassenger(conn, boardingPassPassengerId, updatedBy).whenComplete((JsonObject bpp, Throwable cancelError) -> {
                                    try {
                                        if (cancelError != null) {
                                            throw cancelError;
                                        }

                                        Integer bppStatus = bpp.getInteger(STATUS);
                                        if (isPrincipalPassenger && bppStatus == 3) {
                                            body.put("principal_passenger_id", finalNewPrincipalPassenger.getInteger(ID));
                                            finalNewPrincipalPassenger.put("principal_passenger", 1)
                                                    .put(UPDATED_BY, updatedBy)
                                                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                        }

                                        String updatePassenger = this.generateGenericUpdateString("boarding_pass_passenger", finalNewPrincipalPassenger);
                                        conn.update(updatePassenger, (AsyncResult<UpdateResult> replyUpdatePassenger) -> {
                                            try {
                                                if (replyUpdatePassenger.failed()) {
                                                    throw replyUpdatePassenger.cause();
                                                }

                                                String update = this.generateGenericUpdateString(this.getTableName(), body);
                                                conn.update(update, (AsyncResult<UpdateResult> replyUpdate) -> {
                                                    try {
                                                        if (replyUpdate.failed()) {
                                                            throw replyUpdate.cause();
                                                        }

                                                        JsonObject trackingCancelBoardingPass = new JsonObject()
                                                                .put("boardingpass_id", resultBoardingPass.getInteger("id"))
                                                                .put(ACTION, "canceled")
                                                                .put(STATUS, 0)
                                                                .put(NOTES, notes)
                                                                .put(CREATED_BY, updatedBy);
                                                        String insert = this.generateGenericCreate("boarding_pass_tracking", trackingCancelBoardingPass);

                                                        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                                                            try {
                                                                if (reply.failed()) {
                                                                    throw new Exception(reply.cause());
                                                                }
                                                                JsonObject result = new JsonObject()
                                                                        .put("boardingPass", resultBoardingPass);

                                                                this.commit(conn, message, result);

                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                                this.rollback(conn, e, message);
                                                            }
                                                        });
                                                    } catch (Throwable t) {
                                                        t.printStackTrace();
                                                        this.rollback(conn, t, message);
                                                    }
                                                });
                                            } catch (Throwable t) {
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t) {
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    public void setInPaymentStatus(String table, Integer id) {
        try {
            JsonObject params = new JsonObject().put(ID, id).put("in_payment", false);
            GenericQuery update = generateGenericUpdate(table, params);
            this.dbClient.queryWithParams(update.getQuery(), update.getParams(), queryReply -> {
                try {
                    if (queryReply.failed()) {
                        queryReply.cause().printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void expireReservation(Message<JsonObject> message) {
        String today = UtilsDate.format_YYYY_MM_DD_HH_MM(new Date());
        JsonArray params = new JsonArray().add(today);
        this.dbClient.queryWithParams(QUERY_EXPIRE_RESERVATIONS, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                message.reply(null);
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getPhoneReservation(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        String status = body.getString("status");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd);
        String QUERY = QUERY_GET_PHONE_RESERVATIONS;

        if (status != null) {
            if (status.equals("active")) QUERY = QUERY.concat(QUERY_GET_PHONE_RESERVATIONS_ACTIVE);
            if (status.equals("expired")) QUERY = QUERY.concat(QUERY_GET_PHONE_RESERVATIONS_CANCELED);
            if (status.equals("paid")) QUERY = QUERY.concat(QUERY_GET_PHONE_RESERVATIONS_PAID);
        }

        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                JsonArray reservations = new JsonArray();
                List<JsonObject> reservationRows = reply.result().getRows();
                if (reservationRows.isEmpty()) {
                    message.reply(new JsonObject().put("reservations", reservations));
                } else {
                    reservationRows.forEach(reservation -> {
                        String phoneReservationStatus;
                        if(reservation.getInteger("boardingpass_status") == 4) phoneReservationStatus = "active";
                        else if(reservation.getInteger("boardingpass_status") == 0) phoneReservationStatus = "expired";
                        else phoneReservationStatus = "paid";
                        reservation.put("phone_reservation_status", phoneReservationStatus);
                        reservations.add(reservation);
                        if (reservation == reservationRows.get(reservationRows.size() - 1)) {
                            message.reply(new JsonObject().put("reservations", reservations));
                        }
                    });
                }
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<JsonArray> getTicketsInfo(JsonObject report, JsonObject filters){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer boardingPassId = report.getInteger("boarding_pass_id");
        String dateInit = filters.getString("init_date");
        String dateEnd = filters.getString("end_date");
        Integer originCityId = filters.getInteger("origin_city_id");
        Integer originBranchOfficeId =  filters.getInteger("origin_branchoffice_id");
        Integer destinyCityId = filters.getInteger("destiny_city_id");
        Integer destinyBranchOfficeId = filters.getInteger("destiny_branchoffice_id");
        String purchaseOrigin = filters.getString("purchase_origin");
        Integer purchaseCityId = filters.getInteger("purchase_city_id");
        Integer purchaseBranchOfficeId = filters.getInteger("purchase_branchoffice_id");
        String travelType = filters.getString("ticket_type");

        JsonArray params = new JsonArray().add(boardingPassId).add(dateInit).add(dateEnd);
        String QUERY = QUERY_REPORT_GET_TICKETS_INFO;

        if(travelType != null){
            params.add(travelType);
            QUERY = QUERY.concat(REPORT_TICKET_TYPE_FILTER);
        }
        if (originCityId != null) {
            params.add(originCityId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_CITY_ID);
        }
        if (originBranchOfficeId != null) {
            params.add(originBranchOfficeId);
            QUERY = QUERY.concat(REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID);
        }
        if (destinyCityId != null) {
            params.add(destinyCityId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_CITY_ID);
        }
        if (destinyBranchOfficeId != null) {
            params.add(destinyBranchOfficeId);
            QUERY = QUERY.concat(REPORT_PARAM_DESTINY_BRANCHOFFICE_ID);
        }

        if (purchaseOrigin != null) {
            params.add(purchaseOrigin);
            QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_ORIGIN);
            if (purchaseOrigin.equals("sucursal")) {
                if (purchaseCityId != null) {
                    params.add(purchaseCityId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_CITY_ID);
                }
                if (purchaseBranchOfficeId != null) {
                    params.add(purchaseBranchOfficeId);
                    QUERY = QUERY.concat(REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID);
                }
            }
        }
        Double extracharges = (Double) report.remove("extra_charges");
        JsonObject extraCharges = new JsonObject();
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                result.forEach(r -> {
                    if(!report.containsKey("purchase_date")) report.put("purchase_date", r.getString("created_at"));
                    Double totalAmount = r.getDouble("amount");
                    tasks.add(this.getPaymentsInfoByTicketId(r));
                    Boolean hasExtras = r.getBoolean("has_extras");
                    if(hasExtras && r.getString(ACTION).equals("purchase") && !totalAmount.equals(r.getDouble("extra_charges"))) {
                        extraCharges.put(CREATED_AT, r.getString(CREATED_AT))
                                .put(AMOUNT, r.getDouble("extra_charges"))
                                .put(ACTION, "extra_charges");
                        r.put(AMOUNT, Math.abs(totalAmount - extracharges));
                    }
                });
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res,err)->{
                    try {
                        if(err!=null){
                            future.completeExceptionally(err);
                        }else{
                            if(!extraCharges.isEmpty())
                                result.add(extraCharges);
                            report.put("income_info", result);
                            future.complete(new JsonArray(result));
                        }
                    }catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt, Integer customerId, Integer createdBy, Boolean is_cancel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
        Double creditAvailable;
        if (is_cancel) creditAvailable = actualCreditAvailable + debt;
        else creditAvailable = actualCreditAvailable - debt;
        JsonObject customerObject = new JsonObject()
                .put(ID, customerId)
                .put("credit_available", creditAvailable)
                .put(UPDATED_BY, createdBy)
                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
        String updateCostumer = this.generateGenericUpdateString("customer", customerObject);
        conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCustomer) -> {
            try {
                if (replyCustomer.failed()) {
                    throw replyCustomer.cause();
                }
                future.complete(replyCustomer.succeeded());
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }
    private CompletableFuture<JsonObject> addBoardingPassPassenger(SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer createdBy, Integer origin, TICKET_TYPE ticketType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray tickets = (JsonArray) passenger.remove("tickets");
            String hash = (String) passenger.remove("hash");
            passenger.put("boarding_pass_id", boardingPassId)
                    .put(CREATED_BY, createdBy);

            String insert = this.generateGenericCreate("boarding_pass_passenger", passenger);
            conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    final int boardingPassPassengerId = reply.result().getKeys().getInteger(0);
                    final int specialTicketId = passenger.getInteger("special_ticket_id");
                    passenger.clear();

                    passenger.put(ID, boardingPassPassengerId)
                            .put("hash", hash).put("tickets", tickets);
                    final int iLen = tickets.size();
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                    for (int i = 0; i < iLen; i++) {
                        JsonObject ticket = tickets.getJsonObject(i);
                        tasks.add(insertBoardingPassTicket(conn, ticket, boardingPassPassengerId, ticket.getInteger("boarding_pass_route_id"), specialTicketId, createdBy, false, origin, null, new JsonObject(), ticketType));
                    }

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[iLen])).whenComplete((s, t) -> {
                        if (t != null) {
                            future.completeExceptionally(t.getCause());
                        }
                        future.complete(passenger);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private void checkInAgain(Message<JsonObject> message, Boolean persist) {
        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject body = message.body();
                final int boardingPassId = body.getInteger(ID);
                final int updatedBy = body.getInteger(UPDATED_BY);
                JsonArray passengers = (JsonArray) body.remove("passengers");
                JsonArray payments = body.containsKey("payments") ? (JsonArray) body.remove("payments") : new JsonArray();
                JsonObject cashChange = (JsonObject) body.remove("cash_change");
                Integer cashOutId = (Integer) body.remove(CASHOUT_ID);
                boolean flagPromo = persist && (boolean) body.remove(PromosDBV.FLAG_PROMO);
                JsonObject promoDiscount = persist ? (JsonObject) body.remove(DISCOUNT) : null;
                final Boolean is_credit = (Boolean) body.containsKey("is_credit") ? ((Boolean) body.remove("is_credit")) : false;
                JsonObject customerCreditData = (JsonObject) body.remove("customer_credit_data");
                int expireOpenTicketsAfter = (int) body.remove("expire_open_tickets_after");
                Integer customerId = body.getInteger(CUSTOMER_ID);
                Boolean checkedAlready = body.getBoolean("check_in_already");

                this.getBoardingPassById(conn, boardingPassId).whenComplete((JsonObject boardingPass, Throwable error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        Integer boardingPassStatus = boardingPass.getInteger("boardingpass_status");
                        String reservationCode = boardingPass.getString("reservation_code");
                        String createdAt = boardingPass.getString(CREATED_AT);

                        if (boardingPassStatus != 1 && boardingPassStatus != 2 && boardingPassStatus != 5 && boardingPassStatus != 4) {
                            throw new Exception("Status is not pre boarding or active");
                        }
                        String ticketType = boardingPass.getString("ticket_type");
                        Boolean completePurchase = true ;
                        if(ticketType.equals("abierto_sencillo") || ticketType.equals("abierto_redondo")){
                            if(boardingPassStatus == 4){
                                throw new Exception("Ticket type open cant be checked");
                            }
                            completePurchase = false;
                        }
//                        if(boardingPass.getString("payment_condition").equals("credit")) is_credit[0] = true;
                        Boolean finalCompletePurchase = completePurchase;
                        this.getTotalAmountTicketsForBoardingPassById(conn, boardingPassId, boardingPassStatus).whenComplete((JsonObject tickets, Throwable terror) -> {
                            try {
                                if (terror != null) {
                                    throw terror;
                                }

                                final Double amount = tickets.getDouble(AMOUNT);
                                final Double discount = tickets.getDouble(DISCOUNT);
                                final Double totalAmount = tickets.getDouble(TOTAL_AMOUNT);

                                final int len = passengers.size();
                                List<CompletableFuture<JsonObject>> tasks = IntStream.range(0, len)
                                        .mapToObj(passengers::getJsonObject)
                                        .map(passenger -> checkInPassengerAgain(conn, passenger, boardingPassId, updatedBy, persist, customerId , checkedAlready))
                                        .collect(Collectors.toList());

                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len])).whenComplete((resultCheckInPassenger, errorCheckInPassenger) -> {
                                    try {
                                        if (errorCheckInPassenger != null) {
                                            throw errorCheckInPassenger;
                                        }
                                        // Calculate total for complements
                                        Double extraCharges = 0.0;
                                        JsonArray parcelPackagesDetail = new JsonArray();
                                        JsonArray parcelIds = new JsonArray();
                                        for (int i = 0; i < len; i++) {
                                            JsonObject passenger = passengers.getJsonObject(i);
                                            extraCharges += passenger.getDouble("extra_charges");
                                            if(passenger.containsKey("parcel_packages_detail")){
                                                parcelPackagesDetail.addAll((JsonArray) passenger.remove("parcel_packages_detail"));
                                            }
                                            if(passenger.containsKey("parcel_id")){
                                                parcelIds.add(new JsonObject().put(ID,passenger.remove("parcel_id")));
                                            }
                                        }

                                        // Get total previous payments
                                        Double finalExtraCharges = extraCharges;
                                        this.getTotalAmountPaymentsForBoardingPassById(conn, boardingPassId).whenComplete((JsonObject accPayments, Throwable pError) -> {
                                            try {
                                                if (pError != null) {
                                                    throw pError;
                                                }
                                                if (persist) {
                                                    this.getBoardingPassTicketsList(conn, boardingPassId).whenComplete((resultBPT, errorBPT) -> {
                                                        try {
                                                            if (errorBPT != null) {
                                                                throw errorBPT;
                                                            }

                                                            body.put(AMOUNT, amount)
                                                                    .put(DISCOUNT, discount)
                                                                    .put(TOTAL_AMOUNT, totalAmount);

                                                            boolean resetFlagPromo = (accPayments != null && flagPromo);

                                                            JsonObject bodyPromo = new JsonObject()
                                                                    .put(USER_ID, updatedBy)
                                                                    .put(FLAG_USER_PROMO, false)
                                                                    .put(PromosDBV.DISCOUNT, promoDiscount)
                                                                    .put(SERVICE, SERVICES.boardingpass)
                                                                    .put(BODY_SERVICE, body)
                                                                    .put(PRODUCTS, resultBPT)
                                                                    .put(OTHER_PRODUCTS, new JsonArray())
                                                                    .put(FLAG_PROMO, resetFlagPromo);
                                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                                try {
                                                                    if(replyPromos.failed()) {
                                                                        throw replyPromos.cause();
                                                                    }
                                                                    JsonObject resultApplyDiscount = replyPromos.result().body();
                                                                    JsonObject service = resultApplyDiscount.getJsonObject(PromosDBV.SERVICE);
                                                                    service.put("payment_condition", body.getString("payment_condition"))
                                                                            .put(TOTAL_AMOUNT, service.getDouble(TOTAL_AMOUNT) + finalExtraCharges);
                                                                    if(is_credit) service.put("debt", service.getDouble(TOTAL_AMOUNT));
                                                                    Double innerTotalAmount = UtilsMoney.round(service.getDouble(TOTAL_AMOUNT), 2);


                                                                    Double accPaymentsAmount = accPayments.getDouble(AMOUNT);
                                                                    Double totalPayments = UtilsMoney.round(accPaymentsAmount, 2);

                                                                    final int currencyId = body.getInteger(CURRENCY_ID);
                                                                    final double ivaPercent = body.getInteger("iva_percent");

                                                                    Double totalPaymentsPaid = 0.0;
                                                                    final int pLen = payments.size();
                                                                    for (int i = 0; i < pLen; i++) {
                                                                        JsonObject payment = payments.getJsonObject(i);
                                                                        payment.put("boarding_pass_id", boardingPassId);
                                                                        payment.put(CREATED_BY, updatedBy);
                                                                        Double paymentAmount = payment.getDouble(AMOUNT);
                                                                        if (paymentAmount == null || paymentAmount < 0.0) {
                                                                            this.rollback(conn, new Throwable("Invalid payment amount: " + paymentAmount), message);
                                                                            return;
                                                                        }
                                                                        totalPayments += paymentAmount;
                                                                        totalPaymentsPaid += paymentAmount;

                                                                    }
                                                                    totalPayments = UtilsMoney.round(totalPayments, 2);
                                                                    totalPaymentsPaid = UtilsMoney.round(totalPaymentsPaid, 2);
/*
                                                                    if(!is_credit && !boardingPass.getString("payment_condition").equals("credit")) {
                                                                        if (totalPayments > innerTotalAmount) {
                                                                            throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
                                                                        }
                                                                        if (totalPayments < innerTotalAmount) {
                                                                            throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
                                                                        }
                                                                    }*/

                                                                    service.put(BOARDINGPASS_STATUS, 1)
                                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                                    service.remove("currency_id");
                                                                    service.remove("iva_percent");

                                                                    String expiresAt = null;
                                                                    if (ticketType.contains("abierto")){
                                                                        Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(createdAt), Calendar.DAY_OF_YEAR, expireOpenTicketsAfter);
                                                                        expiresAt = UtilsDate.sdfDataBase(expiredAtDate);
                                                                    }
                                                                    service.put(EXPIRES_AT, expiresAt);

                                                                    if(service.getBoolean("check_in_already") != null && service.getBoolean("check_in_already") ){
                                                                        service.remove("check_in_already");
                                                                        service.remove("customer_id");
                                                                        service.remove("expires_at");
                                                                        service.remove("discount");
                                                                        service.remove("boardingpass_status");
                                                                        service.remove("payment_condition");//service.remove("");
                                                                    }

                                                                    GenericQuery update = this.generateGenericUpdate(SERVICES.boardingpass.getTable(), service, true);

                                                                    Double finalTotalPaymentsPaid = totalPaymentsPaid;
                                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                                        try {
                                                                            if (replyUpdate.failed()){
                                                                                throw replyUpdate.cause();
                                                                            }

                                                                            if(finalTotalPaymentsPaid == 0 && (boardingPassStatus.equals(1) || boardingPassStatus.equals(2) || boardingPassStatus.equals(5))){

                                                                                JsonObject finalResult = new JsonObject()
                                                                                        .put("reservation_code", reservationCode)
                                                                                        .put("parcels",parcelIds)
                                                                                        .put(DISCOUNT_APPLIED, flagPromo);
                                                                                this.commit(conn, message, finalResult);

                                                                            } else {
                                                                                String action = is_credit ? "voucher" : "purchase";
//                                                                                Double ticketTotalPayments = !is_credit ? finalTotalPaymentsPaid : body.getDouble("debt");
                                                                                // Insert ticket
                                                                                this.insertTicket(conn, action, boardingPassId, cashOutId, finalTotalPaymentsPaid, cashChange, updatedBy, ivaPercent, null, finalExtraCharges).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                                                    try {
                                                                                        if (ticketError != null) {
                                                                                            throw ticketError;
                                                                                        }
                                                                                        this.insertTicketDetail(conn, boardingPassId, ticket.getInteger("id"), finalCompletePurchase, finalExtraCharges, updatedBy, parcelPackagesDetail, boardingPassStatus).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                                            try {
                                                                                                if (dError != null) {
                                                                                                    throw dError;
                                                                                                }

                                                                                                if (is_credit) {
                                                                                                    Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
                                                                                                    Double creditAvailable = actualCreditAvailable - finalTotalPaymentsPaid;
                                                                                                    JsonObject customerObject = new JsonObject()
                                                                                                            .put(ID, customerCreditData.getInteger(ID))
                                                                                                            .put("credit_available", creditAvailable)
                                                                                                            .put(UPDATED_BY, updatedBy)
                                                                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                                                                    String updateCostumer = this.generateGenericUpdateString("customer", customerObject);
                                                                                                    conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCostumer) -> {
                                                                                                        try{
                                                                                                            if (replyCostumer.failed()) {
                                                                                                                throw replyCostumer.cause();
                                                                                                            }
                                                                                                            JsonObject finalResult = new JsonObject()
                                                                                                                    .put("reservation_code", reservationCode)
                                                                                                                    .put("credit_available", creditAvailable)
                                                                                                                    .put("ticket_id", ticket.getInteger("id"))
                                                                                                                    .put("parcels", parcelIds)
                                                                                                                    .put(DISCOUNT_APPLIED, flagPromo);
                                                                                                            this.commit(conn, message, finalResult);
                                                                                                        } catch (Throwable t) {
                                                                                                            t.printStackTrace();
                                                                                                            this.rollback(conn, t, message);
                                                                                                        }
                                                                                                    });

                                                                                                } else {
                                                                                                    // insert payments
                                                                                                    List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                                                                    for (int i = 0; i < pLen; i++) {
                                                                                                        JsonObject payment = payments.getJsonObject(i);
                                                                                                        payment.put("ticket_id", ticket.getInteger("id"));
                                                                                                        pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, boardingPassId, cashOutId, updatedBy));
                                                                                                    }
                                                                                                    CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));
                                                                                                    allPayments.whenComplete((ps, pt) -> {
                                                                                                        try {
                                                                                                            if (pt != null) {
                                                                                                                throw pt;
                                                                                                            }

                                                                                                            JsonObject finalResult = new JsonObject()
                                                                                                                    .put("reservation_code", reservationCode)
                                                                                                                    .put("ticket_id", ticket.getInteger("id"))
                                                                                                                    .put("parcels", parcelIds)
                                                                                                                    .put(DISCOUNT_APPLIED, flagPromo);
                                                                                                            this.commit(conn, message, finalResult);

                                                                                                        } catch (Throwable t) {
                                                                                                            t.printStackTrace();
                                                                                                            this.rollback(conn, t, message);
                                                                                                        }
                                                                                                    });
                                                                                                }

                                                                                            } catch (Throwable t) {
                                                                                                t.printStackTrace();
                                                                                                this.rollback(conn, t, message);
                                                                                            }
                                                                                        });
                                                                                    } catch (Throwable t){
                                                                                        t.printStackTrace();
                                                                                        this.rollback(conn, t, message);
                                                                                    }
                                                                                });
                                                                            }
                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
                                                                            this.rollback(conn, t, message);
                                                                        }
                                                                    });
                                                                } catch (Throwable tx) {
                                                                    tx.printStackTrace();
                                                                    this.rollback(conn, tx, message);
                                                                }
                                                            });
                                                        } catch (Throwable tx){
                                                            tx.printStackTrace();
                                                            this.rollback(conn, tx, message);
                                                        }
                                                    });
                                                } else {
                                                    body.put("passengers", passengers)
                                                            .put("extra_charges", finalExtraCharges);
                                                    this.commit(conn, message, body);
                                                }
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }

    private CompletableFuture<JsonObject> checkInPassengerAgain(SQLConnection conn, JsonObject passenger, Integer boardingPassId, Integer updatedBy, Boolean persist, Integer customerId, Boolean checkAlready) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        JsonArray boardingComplements = passenger.getJsonArray("complements");
        if (boardingComplements == null) {
            future.completeExceptionally(new Throwable("complements: missing for passenger " + passenger.getInteger("id")));
            return future;
        }
        Integer boardingPassTicketId = passenger.getInteger("boarding_pass_ticket_id");

        this.getDestinationByTicket(conn, boardingPassTicketId , checkAlready)
                .whenComplete((JsonObject destination, Throwable throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {

                        final int len = boardingComplements.size();
                        List<CompletableFuture<JsonObject>> tasks = IntStream.range(0, len)
                                .mapToObj(boardingComplements::getJsonObject)
                                .map(boardingComplement -> insertCheckInComplementNew(conn, passenger, boardingComplement, boardingPassId, boardingPassTicketId, destination, updatedBy, persist, customerId))
                                .collect(Collectors.toList());

                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[len])).whenComplete((s, t) -> {
                            try {
                                if (t != null) {
                                    throw new Exception(t);
                                } else {

                                    // Calculate total for complements
                                    Double extraCharges = 0.0;
                                    Double extraWeight = 0.0;
                                    Double extraLinearVolume = 0.0;
                                    for (int i = 0; i < len; i++) {
                                        JsonObject boardingComplement = boardingComplements.getJsonObject(i);
                                        extraCharges += boardingComplement.getDouble("extra_charges");

                                        if(boardingComplement.containsKey("parcel_packages_detail")){
                                            passenger.put("parcel_packages_detail", boardingComplement.remove("parcel_packages_detail"));
                                        }
                                        if(boardingComplement.containsKey("parcel_id")){
                                            passenger.put("parcel_id", boardingComplement.remove("parcel_id"));
                                        }
                                        else {
                                            extraWeight += boardingComplement.getDouble("extra_weight");
                                            extraLinearVolume += boardingComplement.getDouble("extra_linear_volume");
                                        }
                                    }
                                    final Double finalExtraCharges = extraCharges;
                                    final Double finalExtraWeight = extraWeight;
                                    final Double finalExtraLinearVolume = extraLinearVolume;
                                    this.getBoardingPassTicketById(conn, boardingPassTicketId).whenComplete((JsonObject ticket, Throwable error) -> {
                                        try {
                                            if(error != null) {
                                                throw new Exception(error);
                                            }

                                            Double ticketAmount = ticket.getDouble("amount");
                                            Double ticketDiscount = ticket.getDouble("discount");
                                            Double ticketExtraChargesOriginal = ticket.getDouble("extra_charges");
                                            Double ticketTotalAmount = ticketAmount + finalExtraCharges - ticketDiscount;
                                            passenger.put("extra_charges", (finalExtraCharges - ticketExtraChargesOriginal));

                                            if (persist) {
                                                Double ticketExtraCharges = ticket.getDouble("extra_charges");
                                                Double ticketTotalAmountOriginal = ticket.getDouble("total_amount");
                                                Double extraWeightOriginal = ticket.getDouble("extra_weight");
                                                Double extraLinearVolumeOriginal = ticket.getDouble("extra_linear_volume");
                                                  GenericQuery update = this.generateGenericUpdate("boarding_pass_ticket", new JsonObject()
                                                        .put("id", boardingPassTicketId)
                                                        .put("extra_charges", (finalExtraCharges - ticketExtraChargesOriginal) + ticketExtraChargesOriginal )
                                                        .put("extra_weight", (finalExtraWeight - extraWeightOriginal ) + extraWeightOriginal)
                                                        .put("extra_linear_volume", ( finalExtraLinearVolume - extraLinearVolumeOriginal ) + extraLinearVolumeOriginal)
                                                        .put("total_amount", ( finalExtraCharges - ticketExtraChargesOriginal) + ticketTotalAmountOriginal )
                                                        .put("updated_by", updatedBy)
                                                        .put("updated_at", UtilsDate.sdfDataBase(new Date())));

                                                conn.updateWithParams(update.getQuery(), update.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                                    try{
                                                        if(reply.failed()) {
                                                            throw new Exception(reply.cause());
                                                        }
                                                        future.complete(passenger);
                                                    }catch (Exception ex) {
                                                        ex.printStackTrace();
                                                        future.completeExceptionally(ex);
                                                    }
                                                });
                                            } else {
                                               // passenger.put("original_extra_charges",ticket.getDouble("extra_charges"));
                                                future.complete(passenger);
                                            }

                                        } catch (Exception e){
                                            future.completeExceptionally(e);
                                        }
                                    });

                                }
                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });

                    }
                });

        return future;

    }

    private CompletableFuture<JsonObject> insertCheckInComplementNew(SQLConnection conn, JsonObject passenger, JsonObject boardingComplement,
                                                                  int boardingPassId, int boardingPassTicketId, JsonObject destination, Integer createdBy, Boolean persist, Integer customerId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Boolean is_parcel = boardingComplement.containsKey("is_parcel") ? boardingComplement.getBoolean("is_parcel") : false;

        if(is_parcel){
            getParcel(conn,  passenger, boardingPassId, boardingPassTicketId, createdBy, persist, boardingComplement).whenComplete((pRes,pErr)->{
                if(pErr!=null){
                    future.completeExceptionally(pErr);
                }else{
                    future.complete(pRes);
                }
            });
        }else{
            getCostBaggageNew(conn, boardingComplement, boardingPassId, boardingPassTicketId, destination, createdBy, persist).whenComplete((res,err)->{
                if(err != null){
                    future.completeExceptionally(err);
                }else{
                    future.complete(res);
                }
            });
        }
        return future;
    }

    private CompletableFuture<JsonObject> getCostBaggageNew(SQLConnection conn,
                                                         JsonObject boardingComplement,
                                                         int boardingPassId, int boardingPassTicketId,
                                                         JsonObject destination, Integer createdBy, Boolean persist){
        CompletableFuture<JsonObject> future = new CompletableFuture();

        Integer complementId = boardingComplement.getInteger("complement_id");
        JsonArray items = (JsonArray) boardingComplement.remove("items");
        if (items == null || items.size() == 0) {
            boardingComplement.put("extra_charges", 0);
            boardingComplement.put("extra_weight", 0);
            boardingComplement.put("extra_linear_volume", 0);
            future.complete(boardingComplement);
        } else {
            this.getComplementById(conn, complementId)
                    .whenComplete((JsonObject complement, Throwable error) -> {
                        if (error != null) {
                            future.completeExceptionally(error);
                        } else {
                            final int number = items.size();
                            List<String> inserts = new ArrayList<>();

                            boardingComplement
                                    .put("boarding_pass_id", boardingPassId)
                                    .put("boarding_pass_ticket_id", boardingPassTicketId)
                                    .put("created_by", createdBy);

                            double accLinearVolume = 0.0;
                            double accWeight = 0.0;
                            for (int i = 0; i < number; i++) {
                                JsonObject item = items.getJsonObject(i);
                                Integer idComplement = item.getInteger("id");
                                if(idComplement == null) {
                                    item.mergeIn(boardingComplement);

                                    Double height = item.getDouble("height");
                                    Double length = item.getDouble("length");
                                    Double width = item.getDouble("width");
                                    Double weight = item.getDouble("weight");
                                    accLinearVolume += height + length + width;
                                    accWeight += weight;

                                    item.put("tracking_code", UtilsID.generateID("C"));
                                    if (persist) {
                                        inserts.add(this.generateGenericCreate("boarding_pass_complement", item));
                                    }
                                }
                            }

                            boardingComplement.put("linear_volume", accLinearVolume);
                            boardingComplement.put("weight", accWeight);
                            boardingComplement.put("number", number);
                            this.complementExtras(conn, boardingComplement, destination, items)
                                    .whenComplete((JsonObject complementUpdated, Throwable cError) -> {
                                        if (cError != null) {
                                            future.completeExceptionally(cError);
                                        } else {
                                            if (persist) {
                                                conn.batch(inserts, (AsyncResult<List<Integer>> reply) -> {
                                                    if (reply.succeeded()) {
                                                        future.complete(boardingComplement);
                                                    } else {
                                                        future.completeExceptionally(reply.cause());
                                                    }
                                                });
                                            } else {
                                                future.complete(boardingComplement);
                                            }

                                        }

                                    });
                        }

                    });
        }

        return future;
    }

    /**
     *  INIT PREPAID METHODS
     */

    private void initPrepaid(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            JsonObject body = message.body();
            JsonArray routes = (JsonArray) body.remove("routes");
            CompletableFuture<Boolean> task = null;
            JsonObject routeTravel;
            Optional<JsonObject> routeT = (Optional) routes.stream().filter(r -> {
                JsonObject router = (JsonObject) r;
                return router.getInteger(TICKET_TYPE_ROUTE).equals(1);
            }).findFirst();
            if(!routeT.isPresent()){
                this.rollback(conn, new Throwable("Ticket type route not found"), message);
            }
            routeTravel = routeT.get();
            Integer configDestinatioIT = routeTravel.getInteger(CONFIG_DESTINATION_ID);
            Integer scheduleRouteDestinationIdT = routeTravel.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            database.boardingpass.enums.TICKET_TYPE ticketType = database.boardingpass.enums.TICKET_TYPE.values()[body.getInteger(TICKET_TYPE)];
            if(configDestinatioIT != null) {
                task = getConfigDestinationTerminals(conn, configDestinatioIT, body);
            } else {
                task = getScheduleRouteDestinationTerminals(conn, scheduleRouteDestinationIdT, body);
            }
            task.whenComplete((ss, error) -> {
                try {
                    if (error != null) {
                        throw  error;
                    }

                    JsonArray passengers = (JsonArray) body.remove("passengers");
                    String reservationCode = body.getString(RESERVATION_CODE);
                    Integer reservationExpiresTime = (Integer) body.remove("boarding_cancelation_time");
                    JsonObject prepaidInfo = body.getJsonObject("prepaid_info");

                    Integer createdBy = body.getInteger(CREATED_BY);
                    JsonObject departure = body.getJsonObject("departure_schedule");

                    conn.setOptions(new SQLOptions().setAutoGeneratedKeys(true));

                    org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
                    DateTime travelDate = body.getValue("travel_date") != null ? formatter.parseDateTime(body.getString("travel_date")) : new DateTime();

                    Date expiresAt;
                    expiresAt = UtilsDate.addMinutes(new Date(), Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(reservationExpiresTime)));

                    Integer boardingPassId = body.getInteger("id");

                    List<CompletableFuture<JsonObject>> routesTasks = new ArrayList<>();
                    for (int i = 0; i < routes.size(); i++) {
                        JsonObject route = routes.getJsonObject(i);
                        Integer scheduleRouteDestinationId = route.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
                        routesTasks.add(insertBoardingPassRoute(conn, route, boardingPassId, createdBy, scheduleRouteDestinationId ));
                    }
                    CompletableFuture<Void> allRoutes = CompletableFuture.allOf(routesTasks.toArray(new CompletableFuture[routes.size()]));
                    allRoutes.whenComplete((resultAllRoutes, errorAllRoutes) -> {
                        try {
                            if(errorAllRoutes != null) {
                                throw errorAllRoutes;
                            }

                            final int pLen = passengers.size();
                            List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                            JsonObject principalPassenger = null;
                            for (int i = 0; i < pLen; i++) {
                                JsonObject passenger = passengers.getJsonObject(i);
                                passenger.remove("min_date");
                                principalPassenger = passenger;
                                pTasks.add(insertBoardingPassPassenger(conn, passenger, boardingPassId, createdBy, routes, 0, 0, departure.getInteger("terminal_origin_id"), null, prepaidInfo, ticketType));
                            }

                            if (principalPassenger == null) {
                                this.rollback(conn, new Throwable("Set principal passenger"), message);
                                return;
                            }

                            final JsonObject finalPrincipalPassenger = principalPassenger;
                            CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen])).whenComplete((ps, pt) -> {
                                try {
                                    if (pt != null) {
                                        this.rollback(conn, pt, message);
                                    } else {
                                        Integer principalPassengerId = finalPrincipalPassenger.getInteger("id");
                                        Double amount = 0.0;
                                        Double discount = 0.0;
                                        Double totalAmount = 0.0;

                                        JsonObject bodyUpdate = new JsonObject()
                                                .put("terminal_origin_id",departure.getInteger("terminal_origin_id"))
                                                .put("terminal_destiny_id", departure.getInteger("terminal_destiny_id"))
                                                .put("updated_by", createdBy)
                                                .put("travel_date", body.getString("travel_date"))
                                                .put("principal_passenger_id", principalPassengerId)
                                                .put("id", boardingPassId);

                                        for (int i = 0; i < pLen; i++) {
                                            JsonObject passenger = passengers.getJsonObject(i);
                                            JsonArray tickets = passenger.getJsonArray("tickets");
                                            final int numTickets = tickets.size();
                                            for (int j = 0; j < numTickets; j++) {
                                                JsonObject ticket = tickets.getJsonObject(j);
                                                amount += ticket.getDouble("amount");
                                                discount += ticket.getDouble("discount");
                                                totalAmount += ticket.getDouble("total_amount");
                                            }
                                        }
                                        bodyUpdate.put("amount", amount);
                                        bodyUpdate.put("discount", discount);
                                        bodyUpdate.put("total_amount", totalAmount);

                                        if(!prepaidInfo.getBoolean("is_inside_prepaid_range")) {
                                            // to be able to register payments, cuz is already in paid status
                                            bodyUpdate.put("boardingpass_status", 4);
                                            bodyUpdate.put("expires_at", UtilsDate.format_YYYY_MM_DD_HH_MM(expiresAt));
                                        }

                                        GenericQuery updateBoardingpass = this.generateGenericUpdate("boarding_pass", bodyUpdate);

                                        conn.updateWithParams(updateBoardingpass.getQuery(), updateBoardingpass.getParams(), replyUpdate -> {
                                            try {
                                                if (replyUpdate.succeeded()) {

                                                    if(prepaidInfo.getBoolean("is_inside_prepaid_range")) {
                                                        JsonObject dataFinish = new JsonObject()
                                                                .put("passengers", passengers)
                                                                .put("totalPayments", 0.0)
                                                                .putNull("cashChange")
                                                                .put("ivaPercent", 0.0)
                                                                .put("extraCharges", 0.0);
                                                        this.finishInitPrepaid(conn, message, body, dataFinish);
                                                    } else {
                                                        JsonObject result = new JsonObject()
                                                                .put("id", boardingPassId)
                                                                .put("reservation_code", reservationCode)
                                                                .put("passengers", passengers);
                                                        this.commit(conn, message, result);
                                                    }
                                                } else {
                                                    throw new Exception(replyUpdate.cause());
                                                }
                                            } catch (Throwable t) {
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t , message);
                }
            });
        });
    }

    private void finishInitPrepaid(SQLConnection conn, Message<JsonObject> message, JsonObject body, JsonObject data) {
        Integer createdBy = body.getInteger(CREATED_BY);
        Integer boardingPassId = body.getInteger("id");
        JsonArray passengers = (JsonArray) data.remove("passengers");
        Integer prepaidId = (Integer) body.remove("prepaid_id");
        Integer cashOutId = (Integer) body.remove("cash_out_id");

        this.insertTicket(conn,"voucher", boardingPassId, cashOutId, data.getDouble("totalPayments"), data.getJsonObject("cashChange"), createdBy,
                data.getDouble("ivaPercent"), null, data.getDouble("extraCharges")).whenComplete((JsonObject ticketR, Throwable ticketError) -> {
            try {
                if(ticketError != null) {
                    throw new Exception(ticketError);
                }

                String detailText = "Canje de viaje de ida "
                        .concat(body.getString("terminal_origin_prefix"))
                        .concat(" - ").concat(body.getString("terminal_destiny_prefix")).concat(" ")
                        .concat(body.getString("travel_date"));

                JsonObject ticketDetails = new JsonObject()
                        .put("ticket_id", ticketR.getInteger(ID))
                        .put(QUANTITY, 1)
                        .put(DETAIL, detailText)
                        .put(UNIT_PRICE, 0.0)
                        .put(AMOUNT, 0.0)
                        .put(CREATED_BY, createdBy);

                String insertDetails = this.generateGenericCreate("tickets_details", ticketDetails);

                conn.update(insertDetails, (AsyncResult<UpdateResult> reply) -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        JsonObject result = new JsonObject();
                        result.put(RESERVATION_CODE, body.getString("reservation_code"))
                                .put("ticket_id", ticketR.getInteger(ID))
                                .put("boarding_id", boardingPassId)
                                .put(STATUS, "update");

                        this.getBoardingPassById(conn, boardingPassId).whenComplete((boardingPass, bErr) -> {
                            try {
                                if(bErr != null) {
                                    throw new Exception(bErr);
                                }
                                Integer boardingPassStatus = boardingPass.getInteger(BOARDINGPASS_STATUS);

                                if (boardingPassStatus != 1 && boardingPassStatus != 2 && boardingPassStatus != 5  && boardingPassStatus != 4) {
                                    throw new Exception("Status is not pre boarding or active");
                                }

                                if (boardingPassStatus == 4) {
                                    throw new Exception("Ticket type open can't be checked");
                                }

                                Integer boardingTicketId = passengers.getJsonObject(0).getJsonArray("tickets").getJsonObject(0).getInteger("id");

                                this.updatePackageTravelCounts(conn, prepaidId, createdBy).whenComplete((replyPkgTravelCount, pkgTError) -> {
                                    try {
                                        if(pkgTError != null) {
                                            throw new Exception(pkgTError);
                                        }
                                        result.put("boarding_ticket_id", boardingTicketId);
                                        this.commit(conn, message, result);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                            } catch (Exception e){
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, e , message);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                this.rollback(conn, e , message);
            }
        });
    }

    private CompletableFuture<JsonObject> updatePackageTravelCounts(SQLConnection conn, Integer prepaidId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String query = "SELECT active_tickets, used_tickets, total_tickets FROM prepaid_package_travel WHERE id = ?";
            conn.queryWithParams(query, new JsonArray().add(prepaidId), (AsyncResult<ResultSet> replyPackage) -> {
                try {
                    if (replyPackage.failed()) {
                        throw new Exception("Can't find prepaid package");
                    }

                    List<JsonObject> results = replyPackage.result().getRows();

                    if (results.isEmpty()) {
                        throw new Exception("Can't find prepaid package");
                    }

                    JsonObject prepaidPasses = results.get(0);

                    Integer active = prepaidPasses.getInteger("active_tickets");
                    Integer used = prepaidPasses.getInteger("used_tickets");
                    Integer total = prepaidPasses.getInteger("total_tickets");

                    if (active == 0) throw new Exception("No active tickets left");
                    if (used > total) throw new Exception("Used tickets greater than the total tickets in the package");

                    int newActive = active - 1;
                    int newUsed = used + 1;

                    if (newUsed > total) throw new Exception("Used tickets greater than the total tickets in the package");

                    JsonObject updatePaquete = new JsonObject()
                            .put("active_tickets", newActive)
                            .put("used_tickets", newUsed)
                            .put("updated_at", UtilsDate.sdfDataBase(new Date()))
                            .put("updated_by", createdBy)
                            .put("id", prepaidId);

                    if (newActive == 0) updatePaquete.put("prepaid_status", 1); // set as finished
                    GenericQuery updatePackage = this.generateGenericUpdate("prepaid_package_travel", updatePaquete);

                    conn.updateWithParams(updatePackage.getQuery(), updatePackage.getParams(), replyUpdatePackage -> {
                        try {
                            if (replyUpdatePackage.succeeded()) {
                                future.complete(new JsonObject());
                            } else {
                                throw new Exception(replyUpdatePackage.cause());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            future.completeExceptionally(e);
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


    private void cancelPrepaidTravelInit (Message <JsonObject> message) {
        JsonObject body = message.body();
        Integer boardingPassId = body.getInteger("id");
        this.startTransaction(message, (SQLConnection conn) -> {
            String query = "SELECT id FROM boarding_pass WHERE id=? AND status=1 AND boardingpass_status=4;";
            conn.queryWithParams(query, new JsonArray().add(boardingPassId), reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    ResultSet result = reply.result();
                    if (result.getNumRows() == 0) {
                        this.rollback(conn, new Throwable("boarding pass not found"), message);
                    } else {
                        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                new JsonObject().put("fieldName", "expire_open_tickets_after"),
                                new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyEOTA -> {
                                    try {
                                        if (replyEOTA.failed()) {
                                            throw replyEOTA.cause();
                                        }
                                        JsonObject expireOpenTicketsAfter = (JsonObject) replyEOTA.result().body();
                                        Integer expireTicketAfter = Integer.parseInt(expireOpenTicketsAfter.getString(VALUE));
                                        Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd(UtilsDate.sdfDataBase(new Date())), Calendar.DAY_OF_YEAR, expireTicketAfter);
                                        String expiresAt = UtilsDate.sdfDataBase(expiredAtDate);

                                        String getBpRoute = "select id FROM boarding_pass_route where boarding_pass_id = ?;";
                                        String getBpTicket = "select id FROM boarding_pass_ticket where boarding_pass_route_id = ?;";

                                        String delBpTicket = "DELETE FROM boarding_pass_ticket where id = ?";
                                        String delBpPass = "DELETE FROM boarding_pass_passenger where boarding_pass_id = ?";
                                        String delBpRoute = "DELETE FROM boarding_pass_route where id = ?";

                                        String updateBp = "UPDATE boarding_pass SET travel_date = NULL, amount = 0.00, discount = 0.00, " +
                                                "total_amount = 0.00, principal_passenger_id = NULL, boardingpass_status = 1, debt = 0, status = 1, " +
                                                "updated_by = NULL, iva = 0.00, payback = 0, expires_at = ? where id = ?";

                                        conn.updateWithParams(updateBp, new JsonArray().add(expiresAt).add(boardingPassId), replyUpdateBP -> {
                                            if (replyUpdateBP.succeeded()) {
                                                conn.queryWithParams(getBpRoute, new JsonArray().add(boardingPassId), replyGetBpRoute -> {
                                                    try {
                                                        if(replyGetBpRoute.failed()) {
                                                            throw new Exception(replyGetBpRoute.cause());
                                                        }
                                                        List<JsonObject> resultBpRoute = replyGetBpRoute.result().getRows();
                                                        Integer bpRouteId = resultBpRoute.get(0).getInteger("id");

                                                        conn.queryWithParams(getBpTicket, new JsonArray().add(bpRouteId), replyGetBpTicket -> {
                                                            try {
                                                                if(replyGetBpTicket.failed()) {
                                                                    throw new Exception(replyGetBpTicket.cause());
                                                                }
                                                                List<JsonObject> resultBpTicket = replyGetBpTicket.result().getRows();
                                                                Integer bpTicketId = resultBpTicket.get(0).getInteger("id");

                                                                conn.updateWithParams(delBpTicket, new JsonArray().add(bpTicketId), replyDelBpTicket -> {
                                                                    if (replyDelBpTicket.succeeded()) {
                                                                        conn.updateWithParams(delBpPass, new JsonArray().add(boardingPassId), replyDelBpPass -> {
                                                                            if (replyDelBpPass.succeeded()) {
                                                                                conn.updateWithParams(delBpRoute, new JsonArray().add(bpRouteId), replyDelBpRoute -> {
                                                                                    if (replyDelBpRoute.succeeded()) {
                                                                                        this.commit(conn, message, new JsonObject().put("boarding_pass", new JsonObject().put("id", boardingPassId)));
                                                                                    } else {
                                                                                        this.rollback(conn, replyDelBpRoute.cause(), message);
                                                                                    }
                                                                                });
                                                                            } else {
                                                                                this.rollback(conn, replyDelBpPass.cause(), message);
                                                                            }
                                                                        });
                                                                    } else {
                                                                        this.rollback(conn, replyDelBpTicket.cause(), message);
                                                                    }
                                                                });

                                                            } catch (Exception ex) {
                                                                ex.printStackTrace();
                                                                this.rollback(conn, ex, message);
                                                            }
                                                        });

                                                    } catch (Exception ex) {
                                                        ex.printStackTrace();
                                                        this.rollback(conn, ex, message);
                                                    }
                                                });
                                            } else {
                                                this.rollback(conn, replyUpdateBP.cause(), message);
                                            }
                                        });

                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        reportQueryError(message, t);
                                    }
                                });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    private void expirePrepaidReservation(Message<JsonObject> message) {
        String today = UtilsDate.format_YYYY_MM_DD_HH_MM(new Date());
        JsonArray params = new JsonArray().add(today);
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                new JsonObject().put("fieldName", "expire_open_tickets_after"),
                new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyEOTA -> {
                    try {
                        if (replyEOTA.failed()) {
                            throw replyEOTA.cause();
                        }
                        JsonObject expireOpenTicketsAfter = (JsonObject) replyEOTA.result().body();
                        Integer expireTicketAfter = Integer.parseInt(expireOpenTicketsAfter.getString(VALUE));

                        this.dbClient.queryWithParams(QUERY_GET_BP_IDS_FOR_EXPIRATION, params, reply -> {
                            try {
                                if (reply.failed()) {
                                    throw reply.cause();
                                }
                                ResultSet result = reply.result();
                                List<JsonObject> rows = result.getRows();
                                List<Integer> bpIds = rows.stream()
                                        .map(row -> row.getInteger("id"))
                                        .collect(Collectors.toList());

                                if (result.getNumRows() > 0) {
                                    Date expiredAtDate = UtilsDate.summCalendar(UtilsDate.parse_yyyy_MM_dd(UtilsDate.sdfDataBase(new Date())), Calendar.DAY_OF_YEAR, expireTicketAfter);
                                    String expiresAt = UtilsDate.sdfDataBase(expiredAtDate);

                                    String updateBp = "UPDATE boarding_pass SET travel_date = NULL, amount = 0.00, discount = 0.00, " +
                                            "total_amount = 0.00, principal_passenger_id = NULL, boardingpass_status = 1, debt = 0, status = 1, " +
                                            "updated_by = NULL, iva = 0.00, payback = 0, expires_at = ? where id IN " +
                                            "(" + String.join(",", Collections.nCopies(bpIds.size(), "?")) + ")";

                                    JsonArray paramsBpIds = new JsonArray();
                                    JsonArray paramsBpUpdateBp = new JsonArray();
                                    paramsBpUpdateBp.add(expiresAt);
                                    bpIds.forEach(id -> {
                                        paramsBpIds.add(id);
                                        paramsBpUpdateBp.add(id);
                                    });

                                    this.dbClient.updateWithParams(updateBp, paramsBpUpdateBp, replyUpdateBP -> {
                                        try {
                                            if (replyUpdateBP.succeeded()) {
                                                String getBpRoute = "select id FROM boarding_pass_route where boarding_pass_id IN " +
                                                        "(" + String.join(",", Collections.nCopies(bpIds.size(), "?")) + ")";

                                                this.dbClient.queryWithParams(getBpRoute, paramsBpIds, replyGetBpRoute -> {
                                                    try {
                                                        if(replyGetBpRoute.failed()) {
                                                            throw new Exception(replyGetBpRoute.cause());
                                                        }

                                                        List<JsonObject> rowsBpRoute = replyGetBpRoute.result().getRows();
                                                        List<Integer> bpRoutesIds = rowsBpRoute.stream()
                                                                .map(row -> row.getInteger("id"))
                                                                .collect(Collectors.toList());
                                                        JsonArray paramsBpRouteIds = new JsonArray();
                                                        bpRoutesIds.forEach(paramsBpRouteIds::add);

                                                        String getBpTicket = "select id FROM boarding_pass_ticket where boarding_pass_route_id in " +
                                                                "(" + String.join(",", Collections.nCopies(bpRoutesIds.size(), "?")) + ")";

                                                        this.dbClient.queryWithParams(getBpTicket, paramsBpRouteIds, replyGetBpTicket -> {
                                                            try {
                                                                if(replyGetBpTicket.failed()) {
                                                                    throw new Exception(replyGetBpTicket.cause());
                                                                }

                                                                List<JsonObject> rowsBpTicket = replyGetBpTicket.result().getRows();
                                                                List<Integer> bpTicketIds = rowsBpTicket.stream()
                                                                        .map(row -> row.getInteger("id"))
                                                                        .collect(Collectors.toList());
                                                                JsonArray paramsBpTicketIds = new JsonArray();
                                                                bpTicketIds.forEach(paramsBpTicketIds::add);

                                                                String delBpTicket = "DELETE FROM boarding_pass_ticket where id IN " +
                                                                        "(" + String.join(",", Collections.nCopies(bpTicketIds.size(), "?")) + ")";

                                                                this.dbClient.updateWithParams(delBpTicket, paramsBpTicketIds, replyDelBpTicket -> {
                                                                    try {
                                                                        if (replyDelBpTicket.succeeded()) {
                                                                            String delBpPass = "DELETE FROM boarding_pass_passenger where boarding_pass_id IN " +
                                                                                    "(" + String.join(",", Collections.nCopies(bpIds.size(), "?")) + ")";

                                                                            this.dbClient.updateWithParams(delBpPass, paramsBpIds, replyDelBpPass -> {
                                                                                try {
                                                                                    if (replyDelBpPass.succeeded()) {
                                                                                        String delBpRoute = "DELETE FROM boarding_pass_route where id IN " +
                                                                                                "(" + String.join(",", Collections.nCopies(bpRoutesIds.size(), "?")) + ")";

                                                                                        this.dbClient.updateWithParams(delBpRoute, paramsBpRouteIds, replyDelBpRoute -> {
                                                                                            try {
                                                                                                if (replyDelBpRoute.succeeded()) {
                                                                                                    message.reply(null);
                                                                                                } else {
                                                                                                    throw new Exception(replyDelBpRoute.cause());
                                                                                                }
                                                                                            } catch(Throwable t) {
                                                                                                t.printStackTrace();
                                                                                                reportQueryError(message, t);
                                                                                            }
                                                                                        });
                                                                                    } else {
                                                                                        throw new Exception(replyDelBpPass.cause());
                                                                                    }
                                                                                } catch(Throwable t) {
                                                                                    t.printStackTrace();
                                                                                    reportQueryError(message, t);
                                                                                }
                                                                            });
                                                                        } else {
                                                                            throw new Exception(replyDelBpTicket.cause());
                                                                        }
                                                                    } catch(Throwable t) {
                                                                        t.printStackTrace();
                                                                        reportQueryError(message, t);
                                                                    }
                                                                });
                                                            } catch (Throwable t) {
                                                                t.printStackTrace();
                                                                reportQueryError(message, t);
                                                            }
                                                        });
                                                    } catch (Throwable t) {
                                                        t.printStackTrace();
                                                        reportQueryError(message, t);
                                                    }
                                                });
                                            } else {
                                                throw new Exception(replyUpdateBP.cause());
                                            }
                                        } catch(Throwable t) {
                                            t.printStackTrace();
                                            reportQueryError(message, t);
                                        }
                                    });
                                } else {
                                    message.reply(null);
                                }
                            } catch (Throwable t) {
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

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_CANCEL_REPORT = "SELECT DISTINCT\n" +
            " bp.purchase_origin,\n" +
            " st.id,\n"+
            " bp.id AS boarding_pass_id,\n" +
            " bp.travel_date,\n" +
            " bp.travel_return_date,\n" +
            " bp.updated_at AS canceled_at,\n"+
            " bp.boardingpass_status,\n"+
            " bp.notes,\n"+
            " t.created_at AS purchase_date,\n" +
            " b.id AS purchase_branchoffice_id,\n" +
            " b.prefix AS purchase_branchoffice_prefix,\n" +
            " ci.id AS purchase_city_id,\n" +
            " ci.name AS purchase_city_name,\n" +
            " bo.prefix AS branchoffice_origin,\n" +
            " bd.prefix AS branchoffice_destiny,\n" +
            " bp.reservation_code,\n" +
            " COALESCE(CONCAT(cu.first_name, ' ', cu.last_name), 'Público general') AS customer_full_name,\n" +
            " CONCAT(bpp.first_name, ' ', bpp.last_name) AS principal_passenger_full_name,\n" +
            " bp.ticket_type,\n" +
            " t.action,\n" +
            " inv.document_id AS invoice_document_id,\n" +
            " (SELECT DISTINCT\n" +
            "  bctp.total_amount\n" +
            "  FROM special_ticket bst\n" +
            "  LEFT JOIN config_ticket_price bctp ON bctp.special_ticket_id = bst.id\n" +
            "  LEFT JOIN config_destination bcd ON bctp.config_destination_id = bcd.id\n" +
            "  LEFT JOIN schedule_route_destination bsrd ON bcd.id = bsrd.config_destination_id\n" +
            "  LEFT JOIN boarding_pass_route bbpr ON bsrd.id = bbpr.schedule_route_destination_id\n" +
            "  LEFT JOIN boarding_pass bbp ON bbpr.boarding_pass_id = bbp.id\n" +
            "  LEFT JOIN boarding_pass_passenger bbpp ON bst.id = bbpp.special_ticket_id AND bbpp.boarding_pass_id = bbp.id\n" +
            "  WHERE bbp.id = bp.id AND bst.base = 1 LIMIT 1) AS base_ticket_price\n" +
            " FROM boarding_pass bp\n" +
            " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
            " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
            " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
            " LEFT JOIN city ci ON b.city_id = ci.id\n" +
            " LEFT JOIN schedule_route_destination srd ON \n" +
            " (SELECT schedule_route_destination_id FROM boarding_pass_route WHERE boarding_pass_id = bp.id AND ticket_type_route = 'ida') = srd.id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
            " LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
            " LEFT JOIN boarding_pass_passenger bpp ON bp.principal_passenger_id = bpp.id AND bpp.principal_passenger = 1\n" +
            " LEFT JOIN special_ticket AS st ON st.id = bpp.special_ticket_id\n"+
            " LEFT JOIN invoice inv ON inv.id = bp.invoice_id\n" +
            " WHERE\n" +
            " bp.boardingpass_status = 0 AND bp.status != 3\n"+
            " AND t.created_at BETWEEN ? AND ? ";

    private static final String QUERY_REPORT = "SELECT DISTINCT\n" +
            " bp.purchase_origin,\n" +
            " bp.id AS boarding_pass_id,\n" +
            " srd.travel_date,\n" +
            " bp.travel_return_date,\n" +
            " bp.payment_condition ,\n" +
            " (SELECT tt.created_at FROM tickets tt WHERE tt.boarding_pass_id = bp.id AND tt.created_at BETWEEN ? AND ?  LIMIT 1 ) purchase_date,\n" +
            " bo.prefix AS branchoffice_origin,\n" +
            " bd.prefix AS branchoffice_destiny,\n" +
            " bp.reservation_code,\n" +
            " inv.document_id AS invoice_document_id,\n" +
            " inv.total_amount AS invoice_total_amount, \n"+
            " (SELECT CONCAT(first_name, ' ', last_name) FROM boarding_pass_passenger \n" +
            " WHERE id = bp.principal_passenger_id AND principal_passenger = 1) AS principal_passenger_full_name,\n" +
            " COALESCE(CONCAT(cu.first_name, ' ', cu.last_name), 'Público general') AS customer_full_name,\n" +
            " bp.ticket_type,\n" +
            " (SELECT SUM(bbpt.extra_charges) FROM boarding_pass bbp \n" +
            "       INNER JOIN boarding_pass_passenger bbpp ON bbpp.boarding_pass_id = bbp.id\n" +
            "       INNER JOIN boarding_pass_ticket bbpt ON bbpt.boarding_pass_passenger_id = bbpp.id\n" +
            "       WHERE bbp.id = bp.id AND bbp.status = 1 AND bbpp.status = 1 AND bbpt.status = 1\n" +
            "       GROUP BY bbp.id) as extra_charges,\n" +
            " bp.promo_id,\n" +
            " pr.discount_code promo_code,\n" +
            " usr.name as sellerName,\n"+
            " usr.email as sellerEmail \n"+
            " FROM boarding_pass bp\n" +
            " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id AND (t.action = 'purchase' OR t.action = 'change' OR t.action = 'voucher') \n" +
            " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
            " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
            " LEFT JOIN city ci ON b.city_id = ci.id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
            " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
            " LEFT JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
            " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
            " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
            " LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
            " LEFT JOIN boarding_pass_passenger bpp ON bpp.boarding_pass_id = bp.id\n" +
            " LEFT JOIN invoice inv ON inv.id = bp.invoice_id AND inv.invoice_status = 'done' AND inv.is_global = false\n" +
            " LEFT JOIN promos pr ON pr.id = bp.promo_id\n" +
            "LEFT JOIN users usr ON bp.created_by = usr.id \n" +
            " WHERE\n" +
            " bp.boardingpass_status != 0\n" +
            " AND bp.boardingpass_status != 4\n" +
            " AND (t.created_at BETWEEN ? AND ? OR bp.created_at BETWEEN ? AND ?) ";

    private static final String REPORT_PARAM_PURCHASE_CITY_ID = " AND ci.id = ? ";

    private static final String REPORT_PARAM_PURCHASE_BRANCHOFFICE_ID = " AND co.branchoffice_id = ? ";

    private static final String REPORT_PARAM_ORIGIN_BRANCHOFFICE_ID = " AND bo.id = ? ";

    private static final String REPORT_PARAM_ORIGIN_CITY_ID = " AND cio.id = ? ";

    private static final String REPORT_TICKET_TYPE_FILTER = " AND bp.ticket_type = ? ";

    private static final String REPORT_PAYMENT_CONDITION = " AND bp.payment_condition = ? ";

    private static final String REPORT_PARAM_DESTINY_BRANCHOFFICE_ID = " AND bd.id = ? ";

    private static final String SPECIAL_TICKET_ID = " AND bpp.special_ticket_id = ? ";

    private static final String SPECIAL_TICKET_ID_IGNORE = " AND bpp.special_ticket_id NOT IN (28,29,30,38) ";

    private static final String SELLER_NAME_ID = " AND bp.created_by = ? ";

    private static final String CUSTOMER_ID_REPORT = " AND bp.customer_id = ? ";

    private static final String REPORT_PARAM_PURCHASE_ORIGIN = " AND bp.purchase_origin = ? ";

    private static final String REPORT_PARAM_DESTINY_CITY_ID = " AND cid.id = ? ";

    private static final String REPORT_ORDER_BY = " ORDER BY purchase_branchoffice_id,purchase_city_id ";

    private static final String REPORT_SPECIAL_TICKETS = "SELECT  \n" +
            "                COUNT(st.id) AS quantity, \n" +
            "                st.id AS special_ticket_id, \n" +
            "                st.name AS special_ticket_name, \n" +
            "                bpt.tracking_code, \n"+
            "                st.description AS special_ticket_description, \n" +
            "                bpt.id AS bptid,\n" +
            "                SUM(bpt.amount) AS config_ticket_price_amount, \n" +
            "                SUM(bpt.discount) AS config_ticket_price_discount, \n" +
            "                SUM(bpt.extra_charges) AS boarding_pass_ticket_extra_charges, \n" +
            "                SUM(bpt.extra_linear_volume) AS boarding_pass_ticket_extra_linear_volume, \n" +
            "                SUM(bpt.extra_weight) AS boarding_pass_ticket_extra_extra_weight, \n" +
            "                SUM(bpt.total_amount - bpt.extra_charges)  AS boarding_pass_ticket_total_amount, \n" +
            "                concat(bpp.first_name , ' ', bpp.last_name) as passenger_name  \n" +
            "             FROM special_ticket st \n" +
            "             LEFT JOIN boarding_pass_passenger bpp ON st.id = bpp.special_ticket_id AND bpp.boarding_pass_id = ? \n" +
            "             LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id \n" +
            "             WHERE bpp.boarding_pass_id = ?\n" +
            "             AND bpp.status = 1 \n" +
            "              GROUP BY st.id;";
    private static final String REPORT_CANCEL_SPECIAL_TICKETS = "SELECT  \n" +
            "                COUNT(st.id) AS quantity, \n" +
            "                st.id AS special_ticket_id, \n" +
            "                st.name AS special_ticket_name, \n" +
            "                st.description AS special_ticket_description, \n" +
            "                bpt.id AS bptid,\n" +
            "                bpt.tracking_code,\n" +
            "                SUM(bpt.amount) AS config_ticket_price_amount, \n" +
            "                SUM(bpt.discount) AS config_ticket_price_discount, \n" +
            "                SUM(bpt.extra_charges) AS boarding_pass_ticket_extra_charges, \n" +
            "                SUM(bpt.extra_linear_volume) AS boarding_pass_ticket_extra_linear_volume, \n" +
            "                SUM(bpt.extra_weight) AS boarding_pass_ticket_extra_extra_weight, \n" +
            "                SUM(bpt.total_amount) AS boarding_pass_ticket_total_amount \n" +
            "             FROM special_ticket st \n" +
            "             LEFT JOIN boarding_pass_passenger bpp ON st.id = bpp.special_ticket_id AND bpp.boarding_pass_id = ? \n" +
            "             LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id \n" +
            "             WHERE bpp.boarding_pass_id = ?\n" +
            "             AND st.status = 1 \n" +
            "             AND bpp.status = 0 \n" +
            "              GROUP BY st.id;";

    private static final String REPORT_COMPLEMENTS_BY_SPECIAL_TICKET = "SELECT\n" +
            " COUNT(bpc.id) AS quantity,\n" +
            " SUM(bpc.weight) AS weight,\n" +
            " SUM(bpc.height + bpc.width + bpc.length) AS linear_volume,\n" +
            " SUM(bpc.height * bpc.width * bpc.length) AS volume\n" +
            " FROM boarding_pass_complement bpc\n" +
            " LEFT JOIN boarding_pass_ticket bpt ON bpt.id = bpc.boarding_pass_ticket_id\n" +
            " LEFT JOIN boarding_pass_passenger bpp ON bpp.id = bpt.boarding_pass_passenger_id\n" +
            " LEFT JOIN boarding_pass bp ON bp.id = bpp.boarding_pass_id\n" +
            " WHERE bp.id = ?\n" +
            " AND bpc.status = 1\n" +
            " AND bpp.special_ticket_id = ?\n" +
            " AND bpt.tracking_code = ?\n"+
            " AND bpp.status = 1;";

    private static final String QUERY_TICKETS = "SELECT\n"
        + "	boarding_pass.id AS boardingpass_id,\n"
        + "	boarding_pass_ticket.id AS id,\n"
        + "	boarding_pass_ticket.tracking_code AS tracking_code,\n"
        + "	boarding_pass_passenger.first_name AS first_name,\n"
        + "	boarding_pass_passenger.last_name AS last_name,\n"
        + "	CONCAT( bpp_child.first_name, ' ', bpp_child.last_name) AS child_name,\n"
        + "	boarding_pass_ticket.seat AS seat,\n"
        + "	boarding_pass_ticket.price_ticket AS price_ticket,\n"
        + "	boarding_pass_ticket.amount AS amount,\n"
        + "	boarding_pass_ticket.discount AS discount,\n"
        + "	boarding_pass_ticket.extra_charges AS extra_charges,\n"
        + "	(boarding_pass_ticket.total_amount - boarding_pass_ticket.extra_charges) AS total_amount,\n"
        + "	boarding_pass_ticket.prints_counter AS prints_counter,\n"
        + "	origin_city.name AS terminal_origin_city,\n"
        + "	origin_state.name AS terminal_origin_state,\n"
        + "	destiny_state.name AS terminal_destiny_state,\n"
        + "	origin.prefix AS terminal_origin_prefix,\n"
        + "	destiny.prefix AS terminal_destiny_prefix,\n"
        + "	destiny_city.name AS terminal_destiny_city,\n"
        + "	vehicle.economic_number AS vehicle_name,\n"
        + "	schedule_route_destination.travel_date AS travel_date,\n"
        + "	special_ticket.name AS special_ticket_name\n"
        + "	FROM\n"
        + "	boarding_pass\n"
        + "	INNER JOIN boarding_pass_passenger ON\n"
        + "	boarding_pass_passenger.boarding_pass_id = boarding_pass.id\n"
        + "	INNER JOIN boarding_pass_ticket ON\n"
        + "	boarding_pass_passenger.id = boarding_pass_ticket.boarding_pass_passenger_id\n"
        + " LEFT JOIN boarding_pass_passenger as bpp_child\n"
        + " ON bpp_child.parent_id = boarding_pass_ticket.boarding_pass_passenger_id AND bpp_child.is_child_under_age = 1\n"
        + "	INNER JOIN special_ticket ON\n"
        + "	special_ticket.id = boarding_pass_passenger.special_ticket_id\n"
        + "	INNER JOIN boarding_pass_route \n"
        + "	ON boarding_pass_route.id = boarding_pass_ticket.boarding_pass_route_id\n"
        + "	INNER JOIN schedule_route_destination ON\n"
        + "	schedule_route_destination.id = boarding_pass_route.schedule_route_destination_id\n"
        + "	INNER JOIN schedule_route ON\n"
        + "	schedule_route.id = schedule_route_destination.schedule_route_id\n"
        + "	INNER JOIN vehicle ON\n"
        + "	vehicle.id = schedule_route.vehicle_id\n"
        + "	INNER JOIN branchoffice AS origin ON\n"
        + "	origin.id = schedule_route_destination.terminal_origin_id\n"
        + "	INNER JOIN branchoffice AS destiny ON\n"
        + "	destiny.id = schedule_route_destination.terminal_destiny_id\n"
        + "	INNER JOIN city AS origin_city ON\n"
        + "	origin_city.id = origin.city_id\n"
        + "	INNER JOIN city AS destiny_city ON\n"
        + "	destiny_city.id = destiny.city_id\n"
        + "	INNER JOIN state AS origin_state ON\n"
        + "	origin_state.id = origin.state_id\n"
        + "	INNER JOIN state AS destiny_state ON\n"
        + "	destiny_state.id = destiny.state_id\n"
        + "	WHERE\n"
        + "	boarding_pass_passenger.status != 3\n"
        + "	AND boarding_pass.reservation_code = ? AND boarding_pass_ticket.seat != ''";

    private static final String QUERY_TICKETS_COMPLEMENTS = "SELECT\n"
        + " boarding_pass.id AS boardingpass_id,\n"
        + " boarding_pass_passenger.first_name,\n"
        + " boarding_pass_passenger.last_name,\n"
        + " boarding_pass_complement.id AS id,\n"
        + " boarding_pass_complement.boarding_pass_ticket_id AS boarding_pass_ticket_id,\n"
        + " boarding_pass_complement.tracking_code AS tracking_code,\n"
        + " boarding_pass_complement.prints_counter,\n"
        + "	boarding_pass.reservation_code AS reservation_code,\n"
        + "	boarding_pass_ticket.seat AS seat,\n"
        + "	origin_city.name AS terminal_origin_city,\n"
        + "	origin_state.name AS terminal_origin_state,\n"
        + "	destiny_state.name AS terminal_destiny_state,\n"
        + "	origin.prefix AS terminal_origin_prefix,\n"
        + "	destiny.prefix AS terminal_destiny_prefix,\n"
        + "	destiny_city.name AS terminal_destiny_city,\n"
        + "	vehicle.economic_number AS vehicle_name,\n"
        + "	schedule_route_destination.travel_date AS travel_date\n"
        + "	FROM boarding_pass_complement\n"
        + "	RIGHT JOIN boarding_pass_ticket ON\n"
        + "	boarding_pass_ticket.id = boarding_pass_complement.boarding_pass_ticket_id\n"
        + "	RIGHT JOIN boarding_pass_passenger ON\n"
        + "	boarding_pass_passenger.id = boarding_pass_ticket.boarding_pass_passenger_id\n"
        + "	RIGHT JOIN boarding_pass ON\n"
        + "	boarding_pass.id = boarding_pass_passenger.boarding_pass_id\n"
        + "	RIGHT JOIN boarding_pass_route ON\n"
        + "	boarding_pass_route.id = boarding_pass_ticket.boarding_pass_route_id\n"
        + "	RIGHT JOIN schedule_route_destination ON\n"
        + "	schedule_route_destination.id = boarding_pass_route.schedule_route_destination_id\n"
        + "	RIGHT JOIN schedule_route ON\n"
        + "	schedule_route.id = schedule_route_destination.schedule_route_id\n"
        + "	RIGHT JOIN vehicle ON\n"
        + "	vehicle.id = schedule_route.vehicle_id\n"
        + "	RIGHT JOIN branchoffice AS origin ON\n"
        + "	origin.id = schedule_route_destination.terminal_origin_id\n"
        + "	RIGHT JOIN branchoffice AS destiny ON\n"
        + "	destiny.id = schedule_route_destination.terminal_destiny_id\n"
        + "	RIGHT JOIN city AS origin_city ON\n"
        + "	origin_city.id = origin.city_id\n"
        + "	RIGHT JOIN city AS destiny_city ON\n"
        + "	destiny_city.id = destiny.city_id\n"
        + "	RIGHT JOIN state AS origin_state ON\n"
        + "	origin_state.id = origin.state_id\n"
        + "	RIGHT JOIN state AS destiny_state ON\n"
        + "	destiny_state.id = destiny.state_id\n"
        + "	WHERE boarding_pass_complement.complement_status = 1 AND boarding_pass.reservation_code = ? AND boarding_pass_complement.boarding_pass_ticket_id = ?;";

    private static final String QUERY_UPDATE_BOARDING_PASS_TICKET_PARCEL_ID = "UPDATE boarding_pass_ticket\n" +
            "SET\n" +
            "parcel_id = ? ,\n" +
            "updated_at = CURRENT_TIMESTAMP(), \n" +
            "updated_by = ? \n" +
            "WHERE id = ? AND boarding_pass_passenger_id = ? ";

    private static final String QUERY_CASH_OUT_EMPLOYEE = "SELECT\n"
            + "	co.id\n"
            + " FROM\n"
            + "	cash_out co\n"
            + " JOIN employee e ON e.id = co.employee_id\n"
            + " WHERE\n"
            + "	e.user_id = ? \n"
            + "	AND co.cash_out_status = 1;";

    private static final String QUERY_TOTAL_AMOUNT_FOR_TICKETS_BY_BOARDING_PASS = "SELECT bp.id, \n"
            + "	COALESCE(SUM(bpt.amount),0) AS amount, \n"
            + "	COALESCE(SUM(bpt.discount),0) AS discount, \n"
            + "	COALESCE(SUM(bpt.total_amount),0) AS total_amount \n"
            + "	FROM boarding_pass AS bp \n"
            + "	LEFT JOIN boarding_pass_route AS bpr ON bpr.boarding_pass_id=bp.id \n"
            + "	LEFT JOIN boarding_pass_ticket AS bpt ON bpt.boarding_pass_route_id=bpr.id \n"
            + "	WHERE bp.id = ? \n"
            + "	GROUP BY bp.id;";

    private static final String QUERY_TOTAL_AMOUNT_TICKETS_PASSENGER_CANCELED = "SELECT bp.id, \n"
            + "	COALESCE(SUM(bpt.amount),0) AS amount, \n"
            + "	COALESCE(SUM(bpt.discount),0) AS discount, \n"
            + "	COALESCE(SUM(bpt.total_amount),0) AS total_amount \n"
            + "	FROM boarding_pass AS bp \n"
            + "	LEFT JOIN boarding_pass_route AS bpr ON bpr.boarding_pass_id=bp.id \n"
            + "	LEFT JOIN boarding_pass_ticket AS bpt ON bpt.boarding_pass_route_id=bpr.id \n"
            + "	WHERE bp.id = ? \n"
            + "	AND bpt.ticket_status != 0 \n"
            + "	GROUP BY bp.id;";

    private static final String QUERY_GET_ROUTE_DESTINATION_DISTANCE_KM = "SELECT cd.id AS config_destination_id,\n"
            + "	bpt.*, distance_km\n"
            + "	FROM boarding_pass_ticket AS bpt\n"
            + "	LEFT JOIN boarding_pass_route AS bpr ON bpt.boarding_pass_route_id=bpr.id\n"
            + " LEFT JOIN schedule_route_destination AS srd ON bpr.schedule_route_destination_id=srd.id\n"
            + " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id\n"
            + "	WHERE bpt.id = ?;";

    private static final String QUERY_TOTAL_AMOUNT_FOR_COMPLEMENTS_BY_BOARDING_PASS = "SELECT bp.id, \n"
            + "	COALESCE(SUM(bpc.amount),0) AS amount, \n"
            + "	COALESCE(SUM(bpc.discount),0) AS discount, \n"
            + "	COALESCE(SUM(bpc.total_amount),0) AS total_amount \n"
            + "	FROM boarding_pass AS bp \n"
            + "	LEFT JOIN boarding_pass_complement AS bpc ON bpc.boarding_pass_id=bp.id \n"
            + "	WHERE bp.id = ? \n"
            + "	GROUP BY bp.id;";

    private static final String QUERY_TOTAL_AMOUNT_FOR_PAYMENTS_BY_BOARDING_PASS = "SELECT bp.id, \n"
            + "	COALESCE(SUM(bpp.amount),0) AS amount \n"
            + "	FROM boarding_pass AS bp \n"
            + "	LEFT JOIN payment AS bpp ON bpp.boarding_pass_id=bp.id \n"
            + "	WHERE bp.id = ? \n"
            + "	GROUP BY bp.id;";

    private static final String QUERY_GET_COMPLEMENT_BY_ID = "SELECT * FROM complement WHERE id = ?";

    private static final String QUERY_GENERAL_SETTING = "SELECT * FROM general_setting WHERE FIELD = ? and status = 1";

    private static final String QUERY_CONFIG_TICKET_PRICE_BY_DESTINATION = "SELECT \n" +
            "\tctp.id AS config_ticket_price_id, \n" +
            "    ctp.amount, \n" +
            "\tctp.discount AS discount, \n" +
            "\tctp.total_amount AS total_amount\n" +
            "FROM schedule_route_destination AS srd \n" +
            " INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            " INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "INNER JOIN config_ticket_price AS ctp ON ctp.config_destination_id=srd.config_destination_id \n" +
            "WHERE srd.id = ? AND ctp.special_ticket_id = ?";

    private static final String QUERY_CONFIG_TICKET_PRICE_BY_DESTINATION_V2 = "SELECT \n" +
            "   ctp.id AS config_ticket_price_id, \n" +
            "   ctp.special_ticket_id, \n" +
            "   cd.config_route_id, \n" +
            "   ctp.amount, \n" +
            "   ctp.discount AS discount, \n" +
            "   ctp.total_amount AS total_amount,\n" +
            "   srd.travel_date,\n" +
            "   srd.terminal_origin_id,\n" +
            "   srd.terminal_destiny_id\n" +
            "FROM schedule_route_destination AS srd \n" +
            "INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "INNER JOIN config_ticket_price AS ctp ON ctp.config_destination_id=srd.config_destination_id \n" +
            "INNER JOIN config_destination cd ON cd.id = ctp.config_destination_id \n" +
            "INNER JOIN special_ticket st ON st.id = ctp.special_ticket_id\n" +
            "WHERE srd.id = ? AND ctp.special_ticket_id = ? \n";

    private static final String QUERY_CONFIG_TICKET_PRICE_BY_DESTINATION_AND_TICKET = "SELECT \n" +
            "   ctp.id AS config_ticket_price_id, \n" +
            "   ctp.special_ticket_id, \n" +
            "   cd.config_route_id, \n" +
            "   ctp.amount, \n" +
            "   ctp.discount AS discount, \n" +
            "   ctp.total_amount AS total_amount,\n" +
            "   srd.travel_date,\n" +
            "   srd.terminal_origin_id,\n" +
            "   srd.terminal_destiny_id,\n" +
            "   bp.ticket_type\n" +
            "FROM schedule_route_destination AS srd \n" +
            "INNER JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "INNER JOIN config_ticket_price AS ctp ON ctp.config_destination_id=srd.config_destination_id \n" +
            "INNER JOIN config_destination cd ON cd.id = ctp.config_destination_id \n" +
            "INNER JOIN special_ticket st ON st.id = ctp.special_ticket_id\n" +
            "LEFT JOIN boarding_pass_ticket bpt ON bpt.id = ?\n" +
            "LEFT JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = bpr.boarding_pass_id\n" +
            "WHERE srd.id = ? AND ctp.special_ticket_id = ? \n" +
            "GROUP BY ctp.id, cd.config_route_id, ctp.amount, ctp.discount, ctp.total_amount, srd.travel_date, srd.terminal_origin_id, srd.terminal_destiny_id, bp.ticket_type";

    private static final String QUERY_GET_BOARDING_PASS_TICKET_BY_ID = "SELECT * from boarding_pass_ticket where id = ?";

    private static final String QUERY_CONFIG_TICKET_PRICE_BY_CONFIG_ROUTE_ID = "SELECT\n" +
            " ctp.id AS config_ticket_price_id, ctp.amount, ctp.discount, ctp.total_amount\n" +
            " FROM config_ticket_price AS ctp \n" +
            "    WHERE ctp.config_destination_id = ? AND ctp.special_ticket_id = ? AND ctp.status = 1 AND ctp.price_status = 1 ORDER BY ctp.amount DESC LIMIT 1";

    private static final String QUERY_CONFIG_TICKET_PRICE_BY_CONFIG_ROUTE_ID_V2 = "SELECT\n" +
            " ctp.id AS config_ticket_price_id, \n" +
            " ctp.special_ticket_id, \n" +
            " cd.config_route_id, \n" +
            " ctp.amount, \n" +
            " ctp.discount, \n" +
            " ctp.total_amount,\n" +
            " srd.travel_date,\n" +
            " srd.terminal_origin_id,\n" +
            " srd.terminal_destiny_id,\n" +
            " bp.ticket_type\n" +
            "FROM config_ticket_price AS ctp\n" +
            "INNER JOIN config_destination cd ON cd.id = ctp.config_destination_id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.config_destination_id = cd.id\n" +
            "LEFT JOIN boarding_pass_ticket bpt ON bpt.id = ?\n" +
            "LEFT JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            "LEFT JOIN boarding_pass bp ON bp.id = bpr.boarding_pass_id\n" +
            "WHERE ctp.config_destination_id = ? AND ctp.special_ticket_id = ?\n" +
            "AND ctp.status = 1 \n" +
            "AND ctp.price_status = 1 \n" +
            "ORDER BY ctp.amount DESC LIMIT 1;";

    // PARAMS: $SCHEDULE_ROUTE_ID=76, $ORDER_ORIGIN=2, $ORDER_DESTINY=3
    /** WHERE: srd.schedule_route_id = $SCHEDULE_ROUTE_ID
     AND ((cd.order_destiny > $ORDER_ORIGIN AND cd.order_destiny <= $ORDER_DESTINY)
     OR ($ORDER_DESTINY > cd.order_origin AND $ORDER_DESTINY <= cd.order_destiny)
     OR (cd.order_origin = $ORDER_ORIGIN AND cd.order_destiny = $ORDER_DESTINY)
     )
     AND bp.status = 1 AND (bp.boardingpass_status = 1 OR bp.boardingpass_status = 4)
     GROUP BY srd.schedule_route_id;
     */
    private static final String QUERY_AVAILABLE_SEATING_BY_DESTINATION = "SELECT \n"
        + " DISTINCT bpt.seat, srd.schedule_route_id, cd.terminal_destiny_id, branch.prefix AS terminal_destiny_prefix\n"
        + " FROM schedule_route_destination AS srd \n"
        + " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
        + " LEFT JOIN branchoffice AS branch ON branch.id=cd.terminal_destiny_id \n"
        + " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
        + " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
        + " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
        + " LEFT JOIN boarding_pass_route AS bpr ON srd.id=bpr.schedule_route_destination_id \n"
        + " INNER JOIN boarding_pass AS bp ON bp.id=bpr.boarding_pass_id \n"
        + " AND bp.status = 1 AND bp.boardingpass_status != 0 \n"
        + " LEFT JOIN boarding_pass_ticket AS bpt ON bpr.id=bpt.boarding_pass_route_id AND bpt.status = 1 \n"
        + " WHERE srd.schedule_route_id = ? AND bpt.seat != '' \n"
        + " AND ((cd.order_destiny > ? AND cd.order_destiny <= ?)  \n"
        + "     OR (? > cd.order_origin AND ? <= cd.order_destiny) \n"
        + "     OR (cd.order_origin = ? AND cd.order_destiny = ?) \n"
        + " ) ";

    private static final String QUERY_SEARCH_AVAILABLE_SEATING_BY_DESTINATION =  QUERY_AVAILABLE_SEATING_BY_DESTINATION
        + " AND bpt.seat = ?";


    private static final String QUERY_LOCKED_SEATING_BY_DESTINATION = "SELECT \n"
            + " DISTINCT srdsl.seat, srd.schedule_route_id, cd.terminal_destiny_id, branch.prefix AS terminal_destiny_prefix\n"
            + " FROM schedule_route_destination AS srd \n"
            + " LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
            + " LEFT JOIN branchoffice AS branch ON branch.id=cd.terminal_destiny_id \n"
            + " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
            + " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
            + " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
            + " INNER JOIN schedule_route_destination_seat_lock \n"
            + " AS srdsl ON srd.id=srdsl.schedule_route_destination_id \n"
            + " AND srdsl.status = 1 AND srdsl.integration_partner_session_id != ? \n"
            + " WHERE srd.schedule_route_id = ? AND srdsl.seat != '' \n"
            + " AND ((cd.order_destiny > ? AND cd.order_destiny <= ?)  \n"
            + "     OR (? > cd.order_origin AND ? <= cd.order_destiny) \n"
            + "     OR (cd.order_origin = ? AND cd.order_destiny = ?) \n"
            + " ) ";

    private static final String QUERY_SEARCH_LOCKED_SEATING_BY_DESTINATION =  QUERY_LOCKED_SEATING_BY_DESTINATION
            + " AND srdsl.seat = ?";

    private static final String QUERY_ADD_GROUP_BY_LOCKED_SEATS = " group by srdsl.seat ";

    private static final String QUERY_DELETE_LOCKED_SEATS_BY_SESSION_ID =
            " DELETE FROM schedule_route_destination_seat_lock" +
            " WHERE integration_partner_session_id = ?;";

    // PARAMS: $SCHEDULE_ROUTE_DESTINATION_ID=40
    private static final String QUERY_GET_ORDERS_BY_DESTINATION = "SELECT \n"
        + " cv.*, srd.schedule_route_id, cd.order_origin, cd.order_destiny \n"
        + " FROM schedule_route_destination AS srd \n"
        + " INNER JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n"
        + " INNER JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
        + " INNER JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n"
        + " INNER JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id \n"
        + " WHERE ";

    private static final String QUERY_GET_RESERVATION_DETAIL = "SELECT * from boarding_pass where reservation_code = ?";

    private static final String QUERY_GET_PASSENGERS_DETAIL = "SELECT * from boarding_pass_passenger where boarding_pass_id = ?;";

    private static final String AND_PARAM  = " AND ";

    private static final String GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_DESTINATION_PARAM = " srd.id = ? ";

    private static final String GET_ORDERS_BY_DESTINATION_SCHEDULE_ROUTE_PARAM = " srd.schedule_route_id = ? ";

    private static final String GET_ORDERS_BY_DESTINATION_TERMINAL_ORIGIN_ID_PARAM = " cd.terminal_origin_id = ? ";

    private static final String GET_ORDERS_BY_DESTINATION_TERMINAL_DESTINY_ID_PARAM = " cd.terminal_destiny_id = ? ";

    private static final String GET_ORDERS_BY_DESTINATION_ORDER_BY_DESC = " ORDER BY srd.id DESC ";

        private static final String QUERY_GET_PRINCIPAL_PASSANGER = "SELECT\n"
        + "bp.id,\n"
        + "bp.boardingpass_status,\n"
        + "bp.purchase_origin,\n"
        + "bp.created_at,\n"
        + "CONCAT(bpp.first_name,' ', bpp.last_name) AS passanger\n"
        + "FROM boarding_pass AS bp\n"
        + "LEFT JOIN boarding_pass_passenger as bpp ON bp.id= bpp.boarding_pass_id \n"
        + "WHERE bp.reservation_code = ? AND bpp.principal_passenger = 1";

    private static final String QUERY_RESERVATION_DETAIL = "SELECT\n"
            + "bp.id,\n"
            + "bp.amount,\n"
            + "bp.changes,\n"
            + "bp.discount,\n"
            + "bp.total_amount,\n"
            + "bp.ticket_type,\n"
            + "bp.status,\n"
            + "t.cash_out_id, \n"
            + "bp.boardingpass_status,\n"
            + "bp.purchase_origin,\n"
            + "bp.created_at,\n"
            + "bp.customer_id,\n"
            + "i.document_id,\n"
            + "i.media_document_pdf_name,\n"
            + "i.media_document_xml_name,\n"
            + "bp.expires_at,\n"
            + "bp.is_phone_reservation,\n"
            + "bp.wish_pet_travel,\n"
            + "bp.wish_frozen_travel,\n"
            + "bp.debt,\n"
            + "SUM(dbt.amount) AS debt_payment_amount,\n"
            + "currency.name AS currency_name\n"
            + "FROM boarding_pass AS bp\n"
            + "LEFT JOIN invoice AS i ON i.id = bp.invoice_id\n"
            + "LEFT JOIN exchange_rate AS ex ON bp.exchange_rate_id = ex.id\n"
            + "LEFT JOIN currency ON ex.currency_id = currency.id\n"
            + "LEFT JOIN debt_payment AS dbt ON dbt.boarding_pass_id = bp.id\n"
            + "LEFT JOIN tickets AS t ON t.boarding_pass_id = bp.id\n"
            + "WHERE bp.reservation_code = ? AND bp.status = 1 AND bp.boardingpass_status IN (0, 1, 2, 3, 4, 5) GROUP BY bp.id";

    private static final String QUERY_PUBLIC_RESERVATION_DETAIL = "SELECT\n"
            + "bp.id,\n"
            + "bp.changes,\n"
            + "bp.amount,\n"
            + "bp.discount,\n"
            + "bp.total_amount,\n"
            + "bp.ticket_type,\n"
            + "bp.status,\n"
            + "t.cash_out_id, \n"
            + "bp.boardingpass_status,\n"
            + "bp.purchase_origin,\n"
            + "bp.created_at,\n"
            + "bp.customer_id,\n"
            + "bp.debt,\n"
            + "SUM(dbt.amount) AS debt_payment_amount,\n"
            + "bp.expires_at,\n"
            + "i.document_id,\n"
            + "i.media_document_pdf_name,\n"
            + "i.media_document_xml_name,\n"
            + "currency.name AS currency_name\n"
            + "FROM boarding_pass AS bp\n"
            + "LEFT JOIN invoice AS i ON i.id = bp.invoice_id"
            + "LEFT JOIN exchange_rate AS ex ON bp.exchange_rate_id = ex.id\n"
            + "LEFT JOIN tickets AS t ON t.boarding_pass_id = bp.id\n"
            + "LEFT JOIN currency ON ex.currency_id = currency.id\n"
            + "LEFT JOIN debt_payment AS dbt ON dbt.boarding_pass_id = bp.id\n"
            + "WHERE bp.reservation_code = ? AND bp.status = 1 AND bp.boardingpass_status = 4 GROUP BY bp.id";

    private static final String QUERY_RESERVATION_DETAIL_ROUTES = "SELECT\n"
            + "bpr.schedule_route_destination_id,\n"
            + "bpr.id AS boarding_pass_route_id,\n"
            + "bpr.ticket_type_route AS ticket_type_route,\n"
            + "tor.id AS terminal_origin_id,\n"
            + "tor.name AS terminal_origin_name,\n"
            + "tor.prefix AS terminal_origin_prefix,\n"
            + "co.name AS terminal_origin_city_name,\n"
            + "so.name AS terminal_origin_state_name,\n"
            + "td.id AS terminal_destiny_id,\n"
            + "td.name AS terminal_destiny_name,\n"
            + "td.prefix AS terminal_destiny_prefix,\n"
            + "cd.name AS terminal_destiny_city_name,\n"
            + "sd.name AS terminal_destiny_state_name,\n"
            + "srd.travel_date AS travel_date,\n"
            + "srd.arrival_date AS arrival_date,\n"
            + "vh.economic_number AS vehicle_economic_number,\n"
            + "sr.code AS schedule_route_code\n"
            + "FROM boarding_pass AS bp\n"
            + "INNER JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id\n"
            + "INNER JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id\n"
            + "INNER JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id\n"
            + "INNER JOIN vehicle AS vh ON vh.id=sr.vehicle_id\n"
            + "INNER JOIN branchoffice AS tor ON srd.terminal_origin_id = tor.id\n"
            + "INNER JOIN city AS co ON\n tor.city_id = co.id\n"
            + "INNER JOIN state AS so ON tor.state_id = so.id\n"
            + "INNER JOIN branchoffice AS td ON srd.terminal_destiny_id = td.id\n"
            + "INNER JOIN city AS cd ON td.city_id = cd.id\n"
            + "INNER JOIN state AS sd ON td.state_id = sd.id\n"
            + "WHERE bp.reservation_code = ? AND bp.status = 1 \n"
            + "ORDER BY bpr.ticket_type_route";

    private static final String QUERY_TRAVEL_CHECKIN_DETAIL = "SELECT\n" +
            " bp.*,\n" +
            " cdd.terminal_origin_id,\n" +
            " cdd.terminal_destiny_id,\n" +
            " srd.travel_date,\n" +
            " srd.arrival_date,\n" +
            " srd.destination_status,\n" +
            " bpr.id AS boarding_pass_route_id,\n" +
            " tor.name AS terminal_origin_name,\n" +
            " tor.prefix AS terminal_origin_prefix,\n" +
            " co.name AS terminal_origin_city_name,\n" +
            " so.name AS terminal_origin_state_name,\n" +
            " td.name AS terminal_destiny_name,\n" +
            " td.prefix AS terminal_destiny_prefix,\n" +
            " cd.name AS terminal_destiny_city_name,\n" +
            " sd.name AS terminal_destiny_state_name,\n" +
            " vh.economic_number AS vehicle_economic_number,\n" +
            " cv.allow_frozen AS vehicle_allow_frozen,\n" +
            " cv.allow_pets AS vehicle_allow_pets,\n" +
            " currency.name AS currency_name,\n" +
            " IF(bp.ticket_type = 'redondo' OR bp.ticket_type = 'abierto_redondo' AND (cdd.terminal_origin_id = ? OR cdd.terminal_destiny_id = ?), true, IF(cdd.terminal_origin_id = ?, true, false)) AS in_travel_terminal\n" +
            " FROM boarding_pass AS bp\n" +
            " INNER JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
            " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
            " INNER JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            " LEFT JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id\n" +
            " LEFT JOIN vehicle AS vh ON vh.id=sr.vehicle_id\n" +
            " LEFT JOIN config_vehicle AS cv ON cv.id=vh.config_vehicle_id\n" +
            " INNER JOIN branchoffice AS tor ON cdd.terminal_origin_id = tor.id\n" +
            " INNER JOIN city AS co ON tor.city_id = co.id\n" +
            " INNER JOIN state AS so ON tor.state_id = so.id\n" +
            " INNER JOIN branchoffice AS td ON cdd.terminal_destiny_id = td.id\n" +
            " INNER JOIN city AS cd ON td.city_id = cd.id\n" +
            " INNER JOIN state AS sd ON td.state_id = sd.id\n" +
            " LEFT JOIN exchange_rate AS ex ON bp.exchange_rate_id = ex.id\n" +
            " LEFT JOIN currency ON ex.currency_id = currency.id\n" +
            " WHERE bp.reservation_code = ? AND bp.status = 1 AND bp.boardingpass_status IN(1, 2, 4, 5)";

    private static final String QUERY_RESERVATION_DETAIL_PASSENGERS = "SELECT \n" +
            " srd.travel_date,\n" +
            " srd.arrival_date,\n" +
            " bo.name AS terminal_origin_name,\n" +
            " bd.name AS terminal_destiny_name,\n" +
            " bpr.id AS boarding_pass_route_id,\n" +
            " bpr.ticket_type_route,\n" +
            " v.economic_number,\n" +
            " bpp.*\n" +
            " FROM boarding_pass AS bp\n" +
            " INNER JOIN boarding_pass_passenger AS bpp ON bp.id = bpp.boarding_pass_id AND bpp.status != 3 AND bpp.is_child_under_age = 0\n" +
            " LEFT JOIN boarding_pass_route bpr ON bpr.boarding_pass_id = bp.id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            " LEFT JOIN schedule_route sr ON sr.id = srd.schedule_route_id\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            " WHERE bp.reservation_code = ? AND bp.status = 1;";

    private static final String QUERY_RESERVATION_DETAIL_PAYMENT = "SELECT\n"
            + "COALESCE(SUM(bpp.amount),0) AS payment\n"
            + "FROM boarding_pass AS bp\n"
            + "LEFT JOIN payment AS bpp ON bp.id=bpp.boarding_pass_id AND bpp.status=1 AND bpp.payment_status=1\n"
            + "WHERE bp.reservation_code = ? AND bp.status = 1 \n"
            + "GROUP BY bp.id;";

    private static final String QUERY_RESERVATION_DETAIL_BRANCHOFFICE = "SELECT bo.id, bo.name, bo.description\n"
            + "FROM tickets AS t\n"
            + "JOIN payment as p ON p.ticket_id=t.id\n"
            + "JOIN boarding_pass AS bp ON bp.id=p.boarding_pass_id\n"
            + "JOIN cash_out AS co ON co.id=t.cash_out_id\n"
            + "JOIN branchoffice AS bo ON bo.id=co.branchoffice_id\n"
            + "WHERE bp.reservation_code = ?\n"
            + "ORDER BY t.created_at\n"
            + "LIMIT 1";

    private static final String QUERY_CHILD_NAME_PASSENGER = "SELECT CONCAT(bpp.first_name, ' ', bpp.last_name) as child_under_age\n"
            + "FROM boarding_pass AS bp\n"
            + "INNER JOIN boarding_pass_passenger AS bpp ON bp.id = bpp.boarding_pass_id AND bpp.status != 3 AND bpp.is_child_under_age = 1\n"
            + "WHERE bpp.parent_id = ? AND bp.status = 1 AND (bp.boardingpass_status = 1 OR bp.boardingpass_status = 4 OR bp.boardingpass_status = 0)\n";


    private static final String QUERY_RESERVATION_DETAIL_ALL_TICKETS = "SELECT\n"
            + "ctp.special_ticket_id, bpt.total_amount, bpr.ticket_type_route\n"
            + "FROM boarding_pass AS bp\n"
            + "INNER JOIN boarding_pass_passenger AS bpp ON bp.id=bpp.boarding_pass_id AND bpp.status=1 AND bpp.is_child_under_age = 0\n"
            + "INNER JOIN boarding_pass_ticket AS bpt ON bpp.id=bpt.boarding_pass_passenger_id AND bpt.status=1\n"
            + "INNER JOIN boarding_pass_route AS bpr ON bpr.id=bpt.boarding_pass_route_id AND bpr.status=1\n"
            + "INNER JOIN config_ticket_price AS ctp ON ctp.id=bpt.config_ticket_price_id AND ctp.status=1\n"
            + "WHERE bp.reservation_code = ? AND bp.status = 1 AND bp.boardingpass_status IN (1, 2, 4, 5);";

    private static final String QUERY_RESERVATION_DETAIL_TICKETS = "SELECT bpt.*\n"
            + "FROM boarding_pass_ticket AS bpt\n"
            + "WHERE bpt.boarding_pass_passenger_id = ?\n";

    private static final String QUERY_CHECKIN_TRAVEL_DETAIL_TICKETS = "SELECT \n"
            + " bpt.*,\n"
            + " srd.terminal_origin_id,\n"
            + " srd.terminal_destiny_id\n"
            + " FROM boarding_pass_ticket AS bpt\n"
            + " LEFT JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n"
            + " LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id \n"
            + " WHERE bpt.boarding_pass_passenger_id = ? AND bpt.boarding_pass_route_id = ?\n";

    private static final String QUERY_RESERVATION_DETAIL_COMPLEMENTS = "SELECT bpc.*\n"
            + "FROM boarding_pass_complement AS bpc\n"
            + "WHERE bpc.boarding_pass_ticket_id = ?\n";

    private static final String QUERY_TRAVEL_DETAIL = "SELECT\n"
            + "	bp.*,\n"
            + "	boo.name AS terminal_origin_name,\n"
            + "	boo.prefix AS terminal_origin_prefix,\n"
            + "	bod.name AS terminal_destiny_name,\n"
            + "	bod.prefix AS terminal_destiny_prefix\n"
            + "FROM\n"
            + "	boarding_pass bp\n"
            + "JOIN branchoffice boo ON\n"
            + "	bp.terminal_origin_id = boo.id\n"
            + "JOIN branchoffice bod ON\n"
            + "	bp.terminal_destiny_id = bod.id\n"
            + "WHERE\n"
            + "	bp.reservation_code = ?;";

    private static final String QUERY_TRAVEL_ROUTES_DETAIL = "SELECT\n"
            + "	bpr.*,\n"
            + "	boo.name AS terminal_origin_name,\n"
            + "	boo.prefix AS terminal_origin_prefix,\n"
            + "	bod.name AS terminal_destiny_name,\n"
            + "	bod.prefix AS terminal_destiny_prefix\n"
            + "FROM\n"
            + "	boarding_pass bp\n"
            + "JOIN boarding_pass_route bpr ON\n"
            + "	bp.id = bpr.boarding_pass_id\n"
            + "JOIN branchoffice boo ON\n"
            + "	bpr.terminal_origin_id = boo.id\n"
            + "JOIN branchoffice bod ON\n"
            + "	bpr.terminal_destiny_id = bod.id\n"
            + "WHERE\n"
            + "	bp.reservation_code = ?;";

    private static final String QUERY_TRAVEL_TICKET_DETAIL = "SELECT\n" +
            "   bp.id,\n" +
            "   bpp.special_ticket_id,\n" +
            "   sp.name AS type_passanger,\n" +
            "   COUNT(bpp.special_ticket_id) AS quantity,\n" +
            "   COALESCE(AVG(bpt.total_amount - bpt.extra_charges), 0) AS unit_price,\n" +
            "   IF(p.id IS NOT NULL, IF(p.discount_per_base, COALESCE(SUM(bpt.discount), 0), COALESCE(SUM(bpt.discount - ctp.discount), 0)), 0) AS discount,\n" +
            "   COALESCE(SUM(bpt.total_amount - bpt.extra_charges), 0) AS amount,\n" +
            "   origin.prefix AS terminal_origin_prefix,\n" +
            "   origin_city.name AS terminal_origin_city,\n" +
            "   origin_state.name AS terminal_origin_state,\n" +
            "   destiny.prefix AS terminal_destiny_prefix,\n" +
            "   destiny_city.name AS terminal_destiny_city,\n" +
            "   destiny_state.name AS terminal_destiny_state,\n" +
            "   srd.travel_date, \n" +
            "   bpp.is_child_under_age,\n" +
            "   group_concat( bpt.seat) as seat_num\n" +
            " FROM boarding_pass_ticket bpt\n" +
            " LEFT JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            " LEFT JOIN boarding_pass bp ON bpr.boarding_pass_id = bp.id\n" +
            " LEFT JOIN promos p ON p.id = bp.promo_id\n" +
            " LEFT JOIN boarding_pass_passenger as bpp ON bpp.id = bpt.boarding_pass_passenger_id AND bpp.is_child_under_age = 0\n" +
            " LEFT JOIN special_ticket AS sp ON bpp.special_ticket_id = sp.id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            " LEFT JOIN config_destination AS cd ON cd.id = srd.config_destination_id OR bpr.config_destination_id = cd.id\n"+
            " LEFT JOIN config_ticket_price ctp ON ctp.config_destination_id = cd.id AND ctp.special_ticket_id = sp.id\n" +
            " LEFT JOIN branchoffice AS origin ON origin.id = cd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice AS destiny ON destiny.id = cd.terminal_destiny_id\n" +
            " LEFT JOIN city AS origin_city ON origin_city.id = origin.city_id\n" +
            " LEFT JOIN city AS destiny_city ON destiny_city.id = destiny.city_id\n" +
            " LEFT JOIN state AS origin_state ON origin_state.id = origin.state_id\n" +
            " LEFT JOIN state AS destiny_state ON destiny_state.id = destiny.state_id\n" +
            " WHERE bpt.status = 1\n" +
            " AND bpp.is_child_under_age = false \n" +
            " AND bp.id = ?\n" +
            " GROUP BY \n" +
            "   bp.id,\n" +
            "   bpp.special_ticket_id,\n" +
            "   origin_city.name,\n" +
            "   origin_state.name,\n" +
            "   destiny_state.name,\n" +
            "   origin.prefix,\n" +
            "   destiny.prefix,\n" +
            "   destiny_city.name,\n" +
            "   srd.travel_date\n" +
            " ORDER BY srd.travel_date;";

    private static final String QUERY_PAYMENTS_TICKET = "SELECT * FROM payment WHERE boarding_pass_id = ? AND ticket_id IS NULL;";

    private static final String QUERY_USER_TRAVELS = "SELECT \n" +
            " bp.id,\n" +
            " bp.reservation_code,\n" +
            " bp.ticket_type,\n" +
            " bp.total_amount, \n" +
            " bp.status, \n" +
            " bp.boardingpass_status,\n" +
            " bp.seatings,\n" +
            " cu.symbol AS currency_symbol,\n" +
            " cu.abr AS currency_abr\n" +
            " FROM boarding_pass AS bp \n" +
            " LEFT JOIN exchange_rate AS ex ON bp.exchange_rate_id = ex.id \n" +
            " LEFT JOIN currency AS cu ON ex.currency_id = cu.id\n" +
            " LEFT JOIN customer AS c ON bp.customer_id = c.id\n" +
            " WHERE c.user_id = ?\n" +
            " AND bp.status != 3";

    private static final String QUERY_TRAVELS_ROUTES = "SELECT\n" +
            "              bpr.id,\n" +
            "              bpr.boarding_pass_id,\n" +
            "              bpr.ticket_type_route,\n" +
            "              tor.name AS origin_name,\n" +
            "              tor.prefix AS origin_prefix,\n" +
            "              tor.address AS origin_address,\n" +
            "              co.name AS origin_city_name,\n" +
            "              so.name AS origin_state_name,\n" +
            "              srd.travel_date,\n" +
            "              td.name AS destiny_name,\n" +
            "              td.prefix AS destiny_prefix,\n" +
            "              td.address AS destiny_address,\n" +
            "              cd.name AS destiny_city_name,\n" +
            "              sd.name AS destiny_state_name,\n" +
            "              srd.arrival_date,\n" +
            "              TIMESTAMPDIFF(MINUTE, srd.travel_date, srd.arrival_date) as travel_duration\n" +
            "              FROM boarding_pass_route as bpr\n" +
            "              LEFT JOIN boarding_pass AS bp ON bpr.boarding_pass_id = bp.id\n" +
            "              LEFT JOIN schedule_route_destination as srd ON srd.id = bpr.schedule_route_destination_id \n" +
            "              LEFT JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            "              LEFT JOIN branchoffice AS tor ON srd.terminal_origin_id = tor.id\n" +
            "              LEFT JOIN city AS co ON tor.city_id = co.id \n" +
            "              LEFT JOIN state AS so ON tor.state_id = so.id\n" +
            "              LEFT JOIN branchoffice AS td ON srd.terminal_destiny_id = td.id\n" +
            "              LEFT JOIN city AS cd ON td.city_id = cd.id \n" +
            "              LEFT JOIN state AS sd ON td.state_id = sd.id\n" +
            "              LEFT JOIN customer AS c ON bp.customer_id = c.id\n" +
            "              WHERE c.user_id = ?\n" +
            "              AND bpr.status = 1;";

    private static final String QUERY_TRAVELS_TICKETS = "SELECT\n"
            + " bpt.id,\n"
            + " bpr.boarding_pass_id,\n"
            + " ctp.special_ticket_id,\n"
            + " bpt.seat,\n"
            + " bpt.cost,\n"
            + " bpt.amount,\n"
            + " bpt.extra_charges,\n"
            + " bpt.discount,\n"
            + " bpt.total_amount\n"
            + " FROM boarding_pass_ticket AS bpt\n"
            + " LEFT JOIN boarding_pass_route AS bpr ON bpt.boarding_pass_route_id = bpr.id\n"
            + " LEFT JOIN boarding_pass AS bp ON bpr.boarding_pass_id = bp.id\n"
            + " LEFT JOIN config_ticket_price AS ctp ON bpt.config_ticket_price_id = ctp.id\n"
            + " LEFT JOIN customer AS c ON bp.customer_id = c.id\n"
            + " WHERE c.user_id = ? \n"
            + " AND bpt.status = 1";

    private static final String QUERY_GET_DISTANCE_AND_SEATINGS = "SELECT \n" +
            " cd.distance_km,\n" +
            " bp.seatings\n" +
            " FROM config_destination AS cd \n" +
            " LEFT JOIN schedule_route_destination AS srd ON srd.config_destination_id = cd.id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bpr.schedule_route_destination_id = srd.id\n" +
            " OR bpr.config_destination_id = cd.id\n"+
            " LEFT JOIN boarding_pass AS bp ON bpr.boarding_pass_id = bp.id\n" +
            " WHERE bp.id = ? LIMIT 1;";

    private static final String QUERY_GET_DETAIL_PARCEL_BY_PASSENGER = "SELECT\n" +
            "bpp.first_name as 'sender_name',\n" +
            "bpp.last_name as 'sender_last_name',\n" +
            "'000000000000' as 'sender_phone',\n" +
            "'' as 'sender_email',\n" +
            "bpr.schedule_route_destination_id as 'schedule_route_destination_id',\n" +
            "srd.terminal_origin_id as 'terminal_origin_id',\n" +
            "srd.terminal_destiny_id as 'terminal_destiny_id',\n" +
            "boo.zip_code as 'sender_zip_code',\n" +
            "0.0 as 'amount',\n" +
            "0.0 as 'total_amount',\n" +
            "bpp.first_name as 'addressee_name',\n" +
            "bpp.last_name as 'addressee_last_name',\n" +
            "'0000000000' as 'addressee_phone',\n" +
            "'' as 'addressee_email',\n" +
            "bod.zip_code as 'addressee_zip_code'\n" +
            "\n" +
            "FROM boarding_pass_passenger as bpp\n" +
            "inner join boarding_pass_route as bpr on bpp.boarding_pass_id = bpr.boarding_pass_id\n" +
            "inner join schedule_route_destination as srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "inner join branchoffice as boo ON boo.id = srd.terminal_origin_id\n" +
            "inner join branchoffice as bod ON bod.id = srd.terminal_destiny_id\n" +
            "\n" +
            "WHERE bpp.id = ?";

    private static final String QUERY_GET_DETAIL_PETSSIZES = "SELECT * FROM pets_sizes WHERE id = ?";

    private static final String QUERY_BOARDINGPASS_ADVANCED_SEARCH = "SELECT \n" +
            " bp.id, \n" +
            " bp.reservation_code, \n" +
            " date(bp.travel_date) AS travel_date, \n" +
            " date_format(bp.travel_date, '%H:%i') AS travel_hour, \n" +
            " bp.ticket_type, \n" +
            " bp.amount, \n" +
            " bp.discount, \n" +
            " bp.total_amount, \n" +
            " bp.status, \n" +
            " bp.boardingpass_status, \n" +
            " bp.wish_frozen_travel AS wish_frozen_travel, \n" +
            " bp.wish_pet_travel AS wish_pet_travel, \n" +
            " bo.id AS terminal_origin_id, \n" +
            " bo.prefix AS terminal_origin_prefix, \n" +
            " bo.name AS terminal_origin_name, \n" +
            " bd.id AS terminal_destiny_id, \n" +
            " bd.prefix AS terminal_destiny_prefix, \n" +
            " bd.name AS terminal_destiny_name, \n" +
            " c.first_name AS customer_first_name, \n" +
            " c.last_name AS customer_last_name,\n" +
            " bpp.first_name,\n" +
            " bpp.last_name,\n" +
            " sr.vehicle_id,\n" +
            " v.economic_number\n" +
            " FROM boarding_pass AS bp\n" +
            " LEFT JOIN boarding_pass_passenger AS bpp ON bpp.boarding_pass_id = bp.id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bp.id = bpr.boarding_pass_id\n" +
            " LEFT JOIN schedule_route_destination AS srd ON bpr.schedule_route_destination_id = srd.id\n" +
            " LEFT JOIN schedule_route AS sr ON srd.schedule_route_id = sr.id\n" +
            " LEFT JOIN vehicle AS v ON sr.vehicle_id = v.id\n" +
            " LEFT JOIN customer AS c ON bp.customer_id = c.id\n" +
            " LEFT JOIN branchoffice AS bo ON srd.terminal_origin_id = bo.id\n" +
            " LEFT JOIN branchoffice AS bd ON srd.terminal_destiny_id = bd.id\n" +
            " WHERE  \n" +
            " bpp.first_name LIKE CONCAT('%', ?, '%') \n" +
            " AND bpp.last_name LIKE CONCAT('%', ?, '%')\n" +
            " AND bo.id = ?\n" +
            " AND bd.id = ? ";

    private static final String BOARDING_PASS_ADVANCED_SEARCH_TRAVEL_DATE = " AND DATE(bp.travel_date) ";

    private static final String BOARDING_PASS_ADVANCED_SEARCH_CREATED_AT = " AND DATE(bp.created_at) ";

    private static final String BOARDING_PASS_ADVANCED_SEARCH_DATE_CONDITION = " BETWEEN DATE(?) AND DATE(?) ";

    private static final String QUERY_RUN_STATUS = "SELECT " +
            "bpr.ticket_type_route AS type_route, \n" +
            "vh.economic_number AS vehicle_number,\n" +
            "srd.travel_date, \n" +
            "srd.arrival_date, \n" +
            "origin_city.name AS origin_city_name, \n" +
            "destiny_city.name  AS destiny_city_name, \n" +
            "srd.destination_status \n" +
            "FROM boarding_pass AS bp \n" +
            "JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id \n" +
            "JOIN schedule_route_destination AS srd ON bpr.schedule_route_destination_id=srd.id \n" +
            "JOIN branchoffice AS origin ON origin.id=srd.terminal_origin_id \n" +
            "JOIN branchoffice AS destiny ON destiny.id=srd.terminal_destiny_id \n" +
            "JOIN city AS origin_city ON origin.city_id=origin_city.id \n" +
            "JOIN city AS destiny_city ON destiny_city.id=destiny.city_id \n" +
            "JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n" +
            "JOIN vehicle AS vh ON vh.id=sr.vehicle_id \n" +
            "WHERE reservation_code = ?;";

    private static final String QUERY_GET_DISTANCE_SCHEDULE_ROUTE_DESTINATION = "SELECT  cd.distance_km " +
            "FROM schedule_route_destination as srd " +
            "INNER JOIN config_destination as cd on srd.config_destination_id = cd.id " +
            "WHERE srd.id = ? ";

    private static final String QUERY_GET_CONFIG_BY_FIELD = "SELECT\n"
            + "	*\n"
            + "FROM\n"
            + "	general_setting\n"
            + "WHERE\n"
            + "	FIELD = ?\n"
            + "	AND status = 1\n"
            + "LIMIT 1;";

    private static final String QUERY_REPORT_GET_PAYMENT_INFO = "SELECT\n" +
            "   p.payment_method_id,\n" +
            "   p.payment_method,\n" +
            "   p.amount,\n" +
            "   pm.name,\n" +
            "   pm.is_cash,\n" +
            "   pm.alias,\n" +
            "   pm.icon\n" +
            " FROM payment p \n" +
            " LEFT JOIN payment_method pm ON pm.id = p.payment_method_id\n" +
            " WHERE p.boarding_pass_id = ? GROUP BY p.payment_method;";

    private static final String QUERY_REPORT_GET_PAYMENT_INFO_BY_TICKET_ID = "SELECT\n" +
            "   p.payment_method_id,\n" +
            "   p.payment_method,\n" +
            "   p.amount,\n" +
            "   pm.name,\n" +
            "   pm.is_cash,\n" +
            "   pm.alias,\n" +
            "   pm.icon\n" +
            " FROM payment p \n" +
            " LEFT JOIN payment_method pm ON pm.id = p.payment_method_id\n" +
            " WHERE p.ticket_id = ?;";

    private static final String QUERY_REPORT_GET_TICKETS_INFO = "SELECT DISTINCT\n" +
            " b.id AS purchase_branchoffice_id,\n" +
            " b.prefix AS purchase_branchoffice_prefix,\n" +
            " ci.id AS purchase_city_id,\n" +
            " ci.name AS purchase_city_name,\n" +
            "   t.id,\n" +
            "   t.created_at,\n" +
            "   t.total amount,\n" +
            "   t.extra_charges,\n"+
            "   t.has_extras,\n"+
            "   IF(td.detail like '%extras%', true, false) has_extra,\n" +
            "   IF((SELECT SUM(bbpt.extra_charges) FROM boarding_pass bbp \n" +
            "       INNER JOIN boarding_pass_passenger bbpp ON bbpp.boarding_pass_id = bbp.id\n" +
            "       INNER JOIN boarding_pass_ticket bbpt ON bbpt.boarding_pass_passenger_id = bbpp.id\n" +
            "       WHERE bbp.id = bp.id AND bbp.status = 1 AND bbpp.status = 1 AND bbpt.status = 1\n" +
            "       GROUP BY bbp.id) = t.total AND t.total > 0, IF((SELECT COUNT(tt.id) FROM tickets tt \n" +
            "       WHERE tt.boarding_pass_id = bp.id AND tt.total > 0) != 1, 'extra_charges', 'purchase'), IF(t.action = 'voucher', 'purchase', IF( t.total = t.extra_charges ,   'extra_charges'   , t.action))) AS action\n" +
            " FROM tickets t \n" +
            " INNER JOIN boarding_pass bp ON bp.id = t.boarding_pass_id \n" +
            " INNER JOIN tickets_details td ON t.id = td.ticket_id AND td.id = (SELECT MIN(id) FROM tickets_details WHERE ticket_id = t.id) \n" +
            " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
            " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
            " LEFT JOIN city ci ON b.city_id = ci.id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
            " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
            " LEFT JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
            " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
            " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
            " WHERE t.boarding_pass_id = ? AND t.created_at BETWEEN ? AND ? ";


    public static final String QUERY_SPECIAL_TICKET_LIST = "SELECT \n" +
            "          st.id AS special_ticket_id, st.name AS special_ticket_name, \n" +
            "          st.base AS is_base, \n" +
            "          st.origin_allowed, \n" +
            "          ctp.amount AS amount, \n" +
            "          ctp.discount AS discount,\n" +
            "          ctp.total_amount AS total_amount \n" +
            "          FROM config_ticket_price AS ctp \n" +
            "          INNER JOIN config_destination AS cd ON cd.id=ctp.config_destination_id \n" +
            "          INNER JOIN special_ticket AS st ON st.id=ctp.special_ticket_id AND st.status = 1 \n" +
            "          WHERE ctp.config_destination_id = ? AND st.base != 1  ORDER BY ctp.amount ;";
    public static final String QUERY_SPECIAL_TICKET_LIST_HIGHER_PRICE = " SELECT MAX(res_total.total_sum) AS special_ticket_total, res_total.id_total FROM \n" +
            " (SELECT    \n" +
            "                  cd.id AS id_total,\n" +
            "                  SUM(ctp.total_amount) AS total_sum\n" +
            "             FROM config_ticket_price AS ctp     \n" +
            "            INNER JOIN config_destination AS cd ON cd.id=ctp.config_destination_id     \n" +
            "            INNER JOIN config_route AS cr ON cr.id = cd.config_route_id   \n" +
            "            INNER JOIN special_ticket AS st ON st.id=ctp.special_ticket_id AND st.status = 1 \n" +
            "            WHERE cd.terminal_origin_id = ? AND cd.terminal_destiny_id = ?     \n" +
            "            AND ctp.status = 1 AND ctp.price_status = 1 AND cd.status = 1 AND cr.status = 1 GROUP BY cd.id ORDER BY cd.id DESC ) AS res_total";
    public static final String QUERY_DETAIL_HIGHER_PRICE = "SELECT    \n" +
            "                  st.id AS special_ticket_id, st.name AS special_ticket_name,     \n" +
            "                  st.base AS is_base,     \n" +
            "                  st.origin_allowed,   \n" +
            "                  ctp.amount AS amount, \n" +
            "                  ctp.id AS id_price,\n" +
            "                  ctp.discount AS discount,    \n" +
            "                  cd.id AS config_destination_id,    \n" +
            "                  ctp.total_amount AS total_amount     \n" +
            "             FROM config_ticket_price AS ctp     \n" +
            "            INNER JOIN config_destination AS cd ON cd.id=ctp.config_destination_id     \n" +
            "            INNER JOIN config_route AS cr ON cr.id = cd.config_route_id   \n" +
            "            INNER JOIN special_ticket AS st ON st.id=ctp.special_ticket_id AND st.status = 1 \n" +
            "            WHERE cd.id = ?;";
            public static final String LIST_SPECIAL_TICKET = "SELECT id, name, has_discount,total_discount,base FROM special_ticket WHERE status = 1";
    public static final String LIST_PRICES_REGISTERED = "SELECT * FROM prices_lists";
    public static final String LIST_PRICES_DETAIL = "SELECT pld.*, origin.name AS origin_name, origin.prefix as origin_prefix,destiny.name AS destiny_name, destiny.prefix as destiny_prefix, st.name AS special_ticket_name, st.has_discount\n" +
            "FROM prices_lists_details AS pld\n" +
            " INNER JOIN special_ticket AS st ON st.id = pld.special_ticket_id \n"+
            "               INNER JOIN branchoffice AS destiny ON destiny.id=pld.terminal_destiny_id\n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=pld.terminal_origin_id \n" +
            "    where pld.price_list_id = ?";
    public static final String LIST_PRICES_BASE = "SELECT pl.id, pl.name, pl.description,pl.is_default FROM prices_lists AS pl WHERE pl.id = ?";
    public static final String LIST_PRICE_STATUS = "SELECT pl.id, pl.name, pl.description,pl.hash,pl.error, pl.apply_status FROM prices_lists AS pl WHERE pl.hash = ?";
    public static final String SPECIAL_TICKET_DESTINATIONS = " SELECT   \n" +
            "            origin.id AS terminal_origin_id,  \n" +
            "            origin.name AS origin_name,  \n" +
            "            origin.prefix as origin_prefix,    \n" +
            "            destiny.id AS terminal_destiny_id,  \n" +
            "            destiny.name AS destiny_name,   \n" +
            "            destiny.prefix as destiny_prefix, \n" +
            "            ABS(cd.order_origin - cd.order_destiny) - 1 AS stops\n" +
            "            FROM config_destination AS cd  \n" +
            "               INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id   \n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            GROUP BY origin.id, destiny.id";


    public static final String QUERY_GET_BOARDING_PASS_TICKETS_LIST = "SELECT \n" +
            "   bp.ticket_type, \n" +
            "   bpp.id AS boarding_pass_passenger_id, \n" +
            "   bpr.ticket_type_route, \n" +
            "   bpt.id, bpt.amount, \n" +
            "   bpt.discount, \n" +
            "   (bpt.total_amount - bpt.extra_charges) AS total_amount, \n" +
            "   bpt.extra_charges, \n" +
            "   ctp.special_ticket_id\n" +
            " FROM boarding_pass_ticket bpt\n" +
            " LEFT JOIN config_ticket_price ctp ON ctp.id = bpt.config_ticket_price_id\n" +
            " LEFT JOIN boarding_pass_passenger bpp ON bpp.id = bpt.boarding_pass_passenger_id\n" +
            " LEFT JOIN boarding_pass_route bpr ON bpr.id = bpt.boarding_pass_route_id\n" +
            " LEFT JOIN boarding_pass bp ON bp.id = bpp.boarding_pass_id\n" +
            " WHERE bp.id = ?;";

    private static final String QUERY_CHECK_OPEN_CASHOUT_BY_BOARDING_PASS_ID = "SELECT co.id, co.cash_out_status FROM tickets t\n" +
            " LEFT JOIN cash_out co ON co.id = t.cash_out_id\n" +
            " WHERE co.cash_out_status = 1\n" +
            " AND t.boarding_pass_id = ?;";

    private static final String QUERY_CHECK_TRAVEL_STATUS_BOARDING_PASS_ID = "SELECT\n" +
            "  IF(srd.destination_status = 'scheduled' OR srd.destination_status = 'loading' OR (srd.destination_status = 'in-transit' AND bp.purchase_origin = 'app chofer'), true, false) AS check_travel_status\n" +
            " FROM boarding_pass bp\n" +
            " LEFT JOIN boarding_pass_route bpr ON bpr.boarding_pass_id = bp.id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            " WHERE bp.id = ?\n" +
            " ORDER BY srd.id DESC LIMIT 1;";

    private static final String QUERY_GET_LAST_TICKET_CASH_REGISTER = "SELECT last_ticket FROM cash_registers WHERE id = ?;";

//</editor-fold>
    private static final String QUERY_OCCUPATION_REPORT = "SELECT \n" +
            "  bo.prefix AS origin_prefix, \n" +
            "  bd.prefix AS destiny_prefix, \n" +
            "  srd.travel_date, \n" +
            "  bp.reservation_code, \n" +
            "  CONCAT(bpp.first_name, ' ', bpp.last_name) AS passenger,\n" +
            "  cu.phone,\n" +
            "  bp.purchase_origin, \n" +
            "  bp.boardingpass_status, \n" +
            "  st.name,\n" +
            "  bpt.total_amount, \n" +
            "  bp.created_at, \n" +
            "  bpt.seat,\n" +
            "  srd.id AS schedule_route_destination_id,\n" +
            "  cd.id AS config_destination_id,\n" +
            "  v.economic_number AS vehicle_economic_number,\n" +
            "  sr.code AS schedule_route_code\n" +
            " FROM schedule_route sr\n" +
            " LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            " LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            " INNER JOIN boarding_pass_route bpr ON bpr.schedule_route_destination_id = srd.id\n" +
            " INNER JOIN boarding_pass bp ON bp.id=bpr.boarding_pass_id\n" +
            " LEFT JOIN customer cu ON cu.id = bp.customer_id\n" +
            " INNER JOIN boarding_pass_passenger bpp ON bpp.boarding_pass_id=bp.id\n" +
            " INNER JOIN boarding_pass_ticket bpt ON bpr.id=bpt.boarding_pass_route_id AND bpp.id=bpt.boarding_pass_passenger_id\n" +
            " INNER JOIN special_ticket st ON bpp.special_ticket_id=st.id\n" +
            " WHERE bp.boardingpass_status != 0 and bp.boardingpass_status != 4\n";

    private static final String OCCUPATION_REPORT_PARAM_ORDERS = " AND ((cd.order_destiny > ? AND cd.order_destiny <= ?) \n" +
            "     OR (? > cd.order_origin AND ? <= cd.order_destiny)\n" +
            "     OR (cd.order_origin = ? AND cd.order_destiny = ?)\n" +
            "     OR (cd.order_origin = ? AND cd.order_destiny >= ?)); ";

    private static final String QUERY_GET_ORDERS_BY_SCHEDULE_ROUTE_ID_AND_TERMINALS = "SELECT\n" +
            "  cd.order_origin,\n" +
            "  cd.order_destiny\n" +
            " FROM schedule_route sr\n" +
            " INNER JOIN schedule_route_destination srd ON srd.schedule_route_id = sr.id\n" +
            " INNER JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            " WHERE sr.id = ?\n" +
            " AND srd.terminal_origin_id = ?\n" +
            " AND srd.terminal_destiny_id = ?;";

    private static final String QUERY_UPDATE_BOARDING_PASS_ROUTE ="UPDATE boarding_pass_route SET schedule_route_destination_id = ?, route_status = 1 WHERE id = ?;";

    private static final String QUERY_UPDATE_BOARDING_PASS_TICKET = "UPDATE boarding_pass_ticket SET seat = ? WHERE id = ? AND boarding_pass_route_id = ?";
    private static final String QUERY_CONFIG_TERMINAL_BY_BOARDING_PASS_ROUTE = "SELECT bpr.ticket_type_route,\n" +
            "bpr.schedule_route_destination_id, cd.terminal_origin_id, cd.terminal_destiny_id,\n" +
            "origin.prefix AS terminal_origin_prefix, destiny.prefix AS terminal_destiny_prefix\n" +
            "FROM boarding_pass_route AS bpr\n" +
            "    LEFT JOIN config_destination AS cd ON cd.id = bpr.config_destination_id\n" +
            "    LEFT JOIN branchoffice AS origin ON origin.id = cd.terminal_origin_id\n" +
            "    LEFT JOIN branchoffice AS destiny ON destiny.id = cd.terminal_destiny_id\n" +
            "where bpr.id = ?; ";
    private static final String UPDATE_BOARDING_PASS_DEPART = "UPDATE boarding_pass SET travel_date = ? WHERE id = ?";
    private static final String UPDATE_BOARDING_PASS_RETURN = "UPDATE boarding_pass SET travel_return_date = ? WHERE id = ?";

    private static final String UPDATE_BOARDING_PASS_PASSENGER = "UPDATE boarding_pass_passenger SET special_ticket_id = ? WHERE id = ?";

    private static final String QUERY_GET_PHONE_RESERVATIONS = "SELECT\n" +
            "bp.id,\n" +
            "bp.created_at purchase_date,\n" +
            "bp.travel_date,\n" +
            "bp.customer_id,\n" +
            "bp.reservation_code,\n" +
            "bp.ticket_type,\n" +
            "bo.prefix AS branchoffice_origin,\n" +
            "bd.prefix AS branchoffice_destiny,\n" +
            "COALESCE(CONCAT(cu.first_name, ' ', cu.last_name), 'Público general') AS customer_full_name,\n" +
            "bp.payment_condition,\n" +
            "bp.amount, \n" +
            "bp.discount,\n" +
            "bp.boardingpass_status,\n" +
            "bp.total_amount\n" +
            "FROM boarding_pass AS bp\n" +
            "INNER JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id\n" +
            "LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id\n" +
            "INNER JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            "LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
            "LEFT JOIN city cio ON cio.id = bo.city_id\n" +
            "LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
            "LEFT JOIN city cid ON cid.id = bd.city_id\n" +
            "LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
            "WHERE is_phone_reservation = 1\n" +
            "AND bp.status = 1\n" +
            "AND bp.created_at BETWEEN ? AND ? ";

    private static final String QUERY_GET_PHONE_RESERVATIONS_ACTIVE = "AND bp.boardingpass_status = 4 ";

    private static final String QUERY_GET_PHONE_RESERVATIONS_CANCELED = "AND bp.boardingpass_status = 0 ";

    private static final String QUERY_GET_PHONE_RESERVATIONS_PAID = "AND bp.boardingpass_status != 0 AND bp.boardingpass_status != 4";
    
    //WEBDEV EMPIEZA
    private static final String QUERY_REPORT_GENERAL =
        "SELECT  DISTINCT\n" +
            " COUNT(st.id) AS quantity, \n" +
            // " DATE_FORMAT(bp.created_at, '%d/%m') as dia,\n"+
            " SUM(bpt.discount) AS config_ticket_price_discount, \n" +
            " SUM(bpt.total_amount) AS boarding_pass_ticket_total_amount  \n" +
            " FROM special_ticket st \n" +
            " LEFT JOIN boarding_pass_passenger bpp ON st.id = bpp.special_ticket_id \n" +
            " LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id \n" +
            " left join boarding_pass bp ON  bpp.boarding_pass_id = bp.id\n" +
            " left join tickets t on bp.id = t.boarding_pass_id\n" +
            " left join cash_out co ON co.id = t.cash_out_id\n" +
            " left join branchoffice b ON co.branchoffice_id = b.id\n" +
            " left join city c on b.city_id = c.id\n" +
            " inner join boarding_pass_route bpr ON bp.id = bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
            " left join schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            " WHERE\n" +
            " bp.boardingpass_status != 0\n" +
            " and t.status = 1\n" +
            " AND st.status = 1 \n"+
            " AND bp.boardingpass_status != 4\n" +
            " AND t.created_at BETWEEN  ?  AND ?  AND bp.purchase_origin = 'sucursal'  AND c.id = ?  AND co.branchoffice_id = ?  ";

    private static final String QUERY_GET_BOARDINGPASS_COUNT = "SELECT \n" +
        "  ifnull(sum(td.quantity),0) as sumaBoleto\n" +
        " FROM boarding_pass bp\n" +
        " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
        " left join tickets_details td ON t.id = td.ticket_id\n" +
        " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
        " WHERE\n" +
        " bp.boardingpass_status != 0\n" +
        " AND bp.boardingpass_status != 4\n" +
        " AND t.created_at BETWEEN ? AND ?  AND bp.purchase_origin = 'sucursal'    AND co.branchoffice_id = ? ";

    private static final String QUERY_GET_BOARDINGPASS_COUNT_APP = "SELECT \n" +
        "  ifnull(sum(td.quantity),0) as sumaBoleto\n" +
        " FROM boarding_pass bp\n" +
        " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
        " left join tickets_details td ON t.id = td.ticket_id\n" +
        " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
        " WHERE\n" +
        " bp.boardingpass_status =1 \n" +
        " AND t.created_at BETWEEN ? AND ?  AND bp.purchase_origin = ?    ";

    private static final String QUERY_GET_BOARDINGPASS_DIA_APP = "SELECT \n" +
        " DATE_FORMAT(bp.created_at, '%d/%m')as dia\n" +
        //"ifnull(count(bp.id),0) as dia \n" +
        " FROM boarding_pass bp\n" +
        " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
        " left join tickets_details td ON t.id = td.ticket_id\n" +
        " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
        " WHERE\n" +
        " bp.boardingpass_status =1 \n" +
        " AND t.created_at BETWEEN ? AND ?  AND bp.purchase_origin = ? LIMIT 1   ";

    private static final String QUERY_REPORT_MONTH = "SELECT DISTINCT\n" +
        " bp.purchase_origin,\n" +
        " bp.id AS boarding_pass_id,\n" +
        " bp.travel_date,\n" +
        " bp.travel_return_date,\n" +
        " t.created_at AS purchase_date,\n" +
        "    b.id AS purchase_branchoffice_id,\n" +
        "    b.prefix AS purchase_branchoffice_prefix,\n" +
        "    ci.id AS purchase_city_id,\n" +
        "    ci.name AS purchase_city_name,\n" +
        "    bo.prefix AS branchoffice_origin,\n" +
        "    bd.prefix AS branchoffice_destiny,\n" +
        "    bp.reservation_code,\n" +
        "    COALESCE(CONCAT(cu.first_name, ' ', cu.last_name), 'Público general') AS customer_full_name,\n" +
        "    CONCAT(bpp.first_name, ' ', bpp.last_name) AS principal_passenger_full_name,\n" +
        "    bp.ticket_type,\n" +
        "    t.action,\n" +
        "    (SELECT DISTINCT\n" +
        "       bctp.total_amount\n" +
        "       FROM special_ticket bst\n" +
        "       LEFT JOIN config_ticket_price bctp ON bctp.special_ticket_id = bst.id\n" +
        "       LEFT JOIN config_destination bcd ON bctp.config_destination_id = bcd.id\n" +
        "       LEFT JOIN schedule_route_destination bsrd ON bcd.id = bsrd.config_destination_id\n" +
        "       LEFT JOIN boarding_pass_route bbpr ON bsrd.id = bbpr.schedule_route_destination_id\n" +
        "       LEFT JOIN boarding_pass bbp ON bbpr.boarding_pass_id = bbp.id\n" +
        "       LEFT JOIN boarding_pass_passenger bbpp ON bst.id = bbpp.special_ticket_id AND bbpp.boarding_pass_id = bbp.id\n" +
        "       WHERE bbp.id = bp.id AND bst.base = 1 LIMIT 1) AS base_ticket_price\n" +
        " FROM boarding_pass bp\n" +
        " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
        " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
        " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
        " LEFT JOIN city ci ON b.city_id = ci.id\n" +
        " LEFT JOIN schedule_route_destination srd ON \n" +
        " (SELECT schedule_route_destination_id FROM boarding_pass_route WHERE boarding_pass_id = bp.id AND ticket_type_route = 'ida') = srd.id\n" +
        " LEFT JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
        " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
        " LEFT JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
        " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
        " LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
        " LEFT JOIN boarding_pass_passenger bpp ON bp.principal_passenger_id = bpp.id AND bpp.principal_passenger = 1\n" +
        " WHERE\n" +
        " bp.boardingpass_status != 0\n" +
        " AND bp.boardingpass_status != 4\n" +
        " AND t.created_at BETWEEN ? AND ? ";

    private static final String QUERY_GET_PARCELS_GENERAL = "select \n" +
        "ifnull(count(pp.id),0) as sumaPaquetes,\n" +
        "ifnull(  sum(pp.total_amount),0  ) as importePaquetes,\n" +
        "ifnull(sum(payT.amount),0) as paqueteTarjeta,\n" +
        "ifnull(sum(payC.amount),0) as paqueteCash\n" +
        //"DATE_FORMAT(pp.created_at, '%d/%m') as diaParcel\n"+
        " from parcels par\n" +
        " left join payment payT on payT.parcel_id = par.id and payT.payment_method_id = 2\n" +
        " left join payment payC on payC.parcel_id = par.id and payC.payment_method_id = 1\n" +
        "  LEFT JOIN parcels_packages AS PP ON par.id = PP.parcel_id\n" +
        " LEFT JOIN customer AS cc ON cc.id = par.customer_id\n" +
        "where par.created_at  BETWEEN ? AND ? \n" +
        "and par.branchoffice_id = ? \n" +
        "and par.parcel_status != 4\n" +
        "and PP.shipping_type = 'parcel' and cc.id != (select value from general_setting where field = 'internal_customer')";

    private static final String QUERY_GET_SOBRES_GEN = "\n" +
        "select \n" +
        "ifnull(count(pp.id),0) as sumaSobres,\n" +
        "ifnull(  sum(pp.total_amount) ,0) as importeSobres,\n" +
        "ifnull(sum(payT.amount),0) as sobreTarjeta,\n" +
        "ifnull(sum(payC.amount),0) as sobreCash\n" +
        //"DATE_FORMAT(pp.created_at, '%d/%m') as diaSobre\n"+
        " from parcels par\n" +
        " left join payment payT on payT.parcel_id = par.id and payT.payment_method_id = 2\n" +
        " left join payment payC on payC.parcel_id = par.id and payC.payment_method_id = 1\n" +
        " LEFT JOIN parcels_packages AS PP ON par.id = PP.parcel_id\n" +
        " LEFT JOIN customer AS cc ON cc.id = par.customer_id\n" +
        "where par.created_at  BETWEEN ? AND ?  \n" +
        "and par.branchoffice_id = ? \n" +
        "and par.parcel_status != 4\n" +
        "and PP.shipping_type = 'courier' and cc.id != (select value from general_setting where field = 'internal_customer')";


    private static final String QUERY_APP_SITIO = " SELECT  DISTINCT\n" +
        "IFNULL(COUNT(st.id),0) AS sumaBoleto, \n" +
        //"DATE_FORMAT(bp.created_at, '%d/%m') as dia,\n" +
        "ifnull(SUM(bpt.discount),0) AS config_ticket_price_discount, \n" +
        "ifnull(SUM(bpt.total_amount),0) AS importeBoleto  \n" +
        "FROM special_ticket st \n" +
        "LEFT JOIN boarding_pass_passenger bpp ON st.id = bpp.special_ticket_id \n" +
        "LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id \n" +
        "left join boarding_pass bp ON  bpp.boarding_pass_id = bp.id\n" +
        "inner join boarding_pass_route bpr ON bp.id = bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
        "left join schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
        "WHERE \n" +
        "bp.created_at BETWEEN ? AND ? \n" +
        "AND st.status = 1 \n" +
        "and bp.boardingpass_status != 0\n" +
        "and bp.boardingpass_status != 4\n" +
        "AND bp.purchase_origin = ?\n" +
        "AND bpp.status = 1  " ;

    private static final String QUERY_REPORT_GET_PAYMENT_INFO_GENERAL = "SELECT\n" +
        "   p.payment_method_id,\n" +
        "   p.payment_method,\n" +
        "   p.amount,\n" +
        "   pm.name,\n" +
        "   pm.is_cash,\n" +
        "   pm.alias,\n" +
        "   pm.icon\n" +
        " FROM payment p \n" +
        " LEFT JOIN payment_method pm ON pm.id = p.payment_method_id\n" +
        " WHERE p.boarding_pass_id = ?;";

    private static final String REPORT_SPECIAL_TICKETS_GENERAL = "SELECT \n" +
        "    COUNT(st.id) AS quantity,\n" +
        "    st.id AS special_ticket_id,\n" +
        "    st.name AS special_ticket_name,\n" +
        "    st.description AS special_ticket_description,\n" +
        "    SUM(ctp.amount) AS config_ticket_price_amount,\n" +
        "    SUM(ctp.discount) AS config_ticket_price_discount,\n" +
        "    SUM(bpt.extra_charges) AS boarding_pass_ticket_extra_charges,\n" +
        "    SUM(bpt.extra_linear_volume) AS boarding_pass_ticket_extra_linear_volume,\n" +
        "    SUM(bpt.extra_weight) AS boarding_pass_ticket_extra_extra_weight,\n" +
        "    SUM(bpt.total_amount) AS boarding_pass_ticket_total_amount\n" +
        " FROM special_ticket st\n" +
        " LEFT JOIN config_ticket_price ctp ON ctp.special_ticket_id = st.id\n" +
        " LEFT JOIN config_destination cd ON ctp.config_destination_id = cd.id\n" +
        " LEFT JOIN schedule_route_destination srd ON cd.id = srd.config_destination_id AND\n" +
        " (SELECT schedule_route_destination_id FROM boarding_pass_route WHERE boarding_pass_id = ? AND ticket_type_route = 'ida') = srd.id\n" +
        " LEFT JOIN boarding_pass_route bpr ON srd.id = bpr.schedule_route_destination_id\n" +
        " LEFT JOIN boarding_pass bp ON bpr.boarding_pass_id = bp.id\n" +
        " LEFT JOIN boarding_pass_passenger bpp ON st.id = bpp.special_ticket_id AND bpp.boarding_pass_id = bp.id\n" +
        " LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id\n" +
        " WHERE bpp.boarding_pass_id = ?\n" +
        " AND st.status = 1\n" +
        " AND bpp.status = 1\n" +
        " GROUP BY st.id;";

    private static final String QUERY_GET_COBRANZA = "SELECT \n" +
        "ifnull(count(p.id),0) as sumaPaquete,\n" +
        "ifnull(sum(payT.amount),0) as cobranzaTarjeta,\n" +
        "ifnull(sum(payC.amount),0) as cobranzaCash\n" +
        " FROM parcels AS p \n" +
        " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
        " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
        " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
        " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
        " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
        " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
        " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
        " left join payment payT on p.id = payT.parcel_id  and payT.payment_method_id != 1\n" +
        " left join payment payC on  p.id =  payC.parcel_id  and payC.payment_method_id = 1\n" +
        " WHERE p.created_at BETWEEN ? AND ?  and p.parcel_status = 2 and  p.pays_sender =0 \n" +
        " and  p.parcel_status != 4  and p.parcel_status != 6 AND p.terminal_destiny_id = ? and cc.id != (select value from general_setting where field = 'internal_customer')";

    private static final String QUERY_GET_FXC = "select \n" +
        "ifnull(  sum(par.total_amount),0  ) as importeFXC, \n" +
        " ifnull(sum(payT.amount),0) as fxcTarjeta,\n" +
        "ifnull(sum(payC.amount),0) as fxcCash\n" +
        " from parcels par\n" +
        " LEFT JOIN customer AS cc ON cc.id = par.customer_id\n" +
        " left join payment payT on payT.parcel_id = par.id and payT.payment_method_id = 2\n" +
        " left join payment payC on payC.parcel_id = par.id and payC.payment_method_id = 1\n" +
        "where par.created_at  BETWEEN ? AND  ? \n" +
        "and par.branchoffice_id = ? \n" +
        "and par.pays_sender = 0 and cc.id != (select value from general_setting where field = 'internal_customer')"  ;

    private static final String PARCEL_QUERY_ABORDO ="SELECT p.id,\n" +
        "  p.total_amount, p.created_at\n" +
        " FROM parcels AS p \n" +
        " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
        " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
        " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
        " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
        " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
        " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
        " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
        " left join general_setting as gs ON  cc.id = gs.value and gs.id = 66 \n"+
        " WHERE p.created_at BETWEEN ? AND ?  and  p.parcel_status != 4  and p.parcel_status != 6 AND b.city_id = ? AND p.branchoffice_id = ? and cc.id != (select value from general_setting where field = 'internal_customer') ";

    private static final String QUERY_GET_PARCELS_FPX = "SELECT \n" +
        "ifnull(sum(FPX.total_amount),0) as FPX,\n" +
        "ifnull(sum(fpxT.amount),0) as FPXT,\n" +
        "ifnull(sum(fpxC.amount),0)as FPXC, \n" +
        "ifnull(sum(contado.total_amount),0)as Contado\n" +
        "FROM parcels AS p \n" +
        "left join parcels AS totales ON p.id = totales.id \n" +
        "LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
        "LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
        "LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
        "LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
        "LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
        "LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
        "LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
        "left join payment as parT ON p.id = parT.parcel_id and parT.payment_method_id != 1\n" +
        "left join payment as parC ON p.id = parC.parcel_id and parC.payment_method_id = 1\n" +
        "left join parcels AS FPX ON p.id = FPX.id AND FPX.pays_sender = 0\n" +
        "left join payment AS fpxT ON FPX.id = fpxT.parcel_id and fpxT.payment_method_id != 1\n" +
        "left join payment AS fpxC on FPX.id = fpxC.parcel_id and fpxC.payment_method_id = 1 \n" +
        "left join parcels AS contado ON p.id = contado.id AND contado.pays_sender = 1\n" +
        " left join general_setting as gs ON  cc.id = gs.value and gs.id = 66 \n"+
        "WHERE p.created_at BETWEEN ? AND ? and p.parcel_status !=6 and  p.parcel_status != 4  and c.id = ?  and b.id = ? and cc.id != (select value from general_setting where field = 'internal_customer') ";

    private static final String ULTIMO_QUERY = "SELECT DISTINCT\n" +
        "DATE_FORMAT(bp.created_at, '%d/%m') as dia,\n" +
        " bp.id AS boarding_pass_id\n" +
        " FROM boarding_pass bp\n" +
        " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
        " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
        " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
        " LEFT JOIN city ci ON b.city_id = ci.id\n" +
        " INNER JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
        " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
        " INNER JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
        " LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
        " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
        " LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
        " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
        " LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
        " LEFT JOIN boarding_pass_passenger bpp ON bp.principal_passenger_id = bpp.id AND bpp.principal_passenger = 1\n" +
        " LEFT JOIN invoice inv ON inv.id = bp.invoice_id\n" +
        " WHERE\n" +
        " bp.boardingpass_status != 0\n" +
        " AND bp.boardingpass_status != 4\n" +
        " AND t.created_at BETWEEN  ?  AND ?  AND bp.purchase_origin = 'sucursal'  AND ci.id = ?  AND co.branchoffice_id = ? ";

    private static final String QUERY_SALES_REPORT_PACKAGES_ABORDO = "SELECT pp.parcel_id,\n " +
        "IFNULL(SUM(pp.total_amount),0) as sumaPaquete,\n" +
        " COUNT(pp.parcel_id) AS packages, pp.shipping_type, pp.package_type_id, pt.name AS package_type, pp.package_price_id,\n " +
        " ppr.name_price AS package_range, SUM(pp.weight) AS weight, SUM(pp.height + pp.width + pp.length) AS volumen_lineal, SUM(pp.height * pp.width * pp.length) / 1000000 AS volumen\n" +
        " FROM parcels_packages AS pp \n" +
        " INNER JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
        " INNER JOIN package_price AS ppr ON ppr.id = pp.package_price_id \n" +
        " WHERE pp.parcel_id = ?\n" +
        " GROUP BY pp.shipping_type, pp.package_type_id, pp.package_price_id";

    private static final String QUERY_SALES_REPORT_PAYMENT_INFO_ABORDO = "SELECT\n" +
        "   p.payment_method_id,\n" +
        "   p.payment_method,\n" +
        "   p.amount,\n" +
        "   pm.name,\n" +
        "   pm.is_cash,\n" +
        "   pm.alias,\n" +
        "   pm.icon\n" +
        " FROM payment p \n" +
        " LEFT JOIN payment_method pm ON pm.id = p.payment_method_id\n" +
        " WHERE p.parcel_id = ? and p.status = 1;";
//WEBDEV CIERRA
    private static final String GET_PASSENGERS_BY_BOARDING_PASS_ID = "SELECT id, boarding_pass_id, principal_passenger " +
            "FROM boarding_pass_passenger WHERE boarding_pass_id = ? AND status = 1 ORDER BY principal_passenger DESC;";

    private static final String GET_TICKETS_BY_PASSENGER_ID = "SELECT * FROM boarding_pass_ticket " +
            "WHERE boarding_pass_passenger_id = ? AND status = 1;";
            
    private static final String QUERY_EMAIL_BOARDING_PASS_DETAIL = "SELECT bp.reservation_code, \n" +
            "bp.amount, bp.discount, \n" +
            "bp.total_amount, bp.created_at, (bp.ticket_type + 0) AS ticket_type, \n" +
            "CONCAT(cst.first_name, ' ', cst.last_name) AS customer_fullname, \n" +
            "CONCAT(bpp.first_name, ' ', bpp.last_name) AS passenger_fullname, \n" +
            "bp.email AS passenger_email, cst.email AS customer_email \n " +
            "FROM boarding_pass AS bp \n" +
            "LEFT JOIN customer AS cst ON cst.id = bp.customer_id \n" +
            "LEFT JOIN boarding_pass_passenger AS bpp ON bp.principal_passenger_id = bpp.id \n" +
            "WHERE bp.id = ?;";

    private static final String QUERY_EMAIL_BOARDING_PASS_PASSENGERS = "SELECT bpp.id, \n" +
            "CONCAT(bpp.first_name, ' ', bpp.last_name) AS name \n" +
            "FROM boarding_pass_passenger AS bpp \n" +
            "WHERE bpp.boarding_pass_id = ?;";

    private static final String QUERY_EMAIL_BOARDING_PASS_TICKETS = "SELECT bpt.id, bpt.boarding_pass_passenger_id, bpt.seat, bpr.ticket_type_route \n" +
            "FROM boarding_pass_ticket AS bpt\n" +
            "INNER JOIN boarding_pass_passenger AS bpp ON bpt.boarding_pass_passenger_id = bpp.id\n" +
            "INNER JOIN boarding_pass AS bp ON bpp.boarding_pass_id = bp.id\n" +
            "INNER JOIN boarding_pass_route AS bpr ON bpt.boarding_pass_route_id = bpr.id\n" +
            "WHERE bp.id = ?;";

    private static final String QUERY_EMAIL_BOARDING_PASS_ROUTES = "SELECT origin.prefix AS origin_prefix, \n" +
            "destination.prefix AS destination_prefix,\n" +
            "o_state.name AS origin_state,\n" +
            "o_city.name AS origin_city,\n" +
            "d_state.name AS destination_state,\n" +
            "d_city.name AS destination_city, \n" +
            "srd.travel_date, \n" +
            "srd.arrival_date,\n" +
            "cr.type_travel,\n" +
            "cd.travel_time\n" +
            "FROM boarding_pass_route AS bpr \n" +
            "INNER JOIN schedule_route_destination AS srd ON bpr.schedule_route_destination_id = srd.id \n" +
            "INNER JOIN config_destination AS cd ON srd.config_destination_id = cd.id \n" +
            "INNER JOIN schedule_route AS sr ON srd.schedule_route_id = sr.id\n" +
            "INNER JOIN config_route AS cr ON sr.config_route_id = cr.id\n" +
            "INNER JOIN branchoffice AS origin ON cd.terminal_origin_id = origin.id\n" +
            "INNER JOIN state AS o_state ON origin.state_id = o_state.id \n" +
            "INNER JOIN city AS o_city ON origin.city_id = o_city.id \n" +
            "INNER JOIN branchoffice AS destination ON cd.terminal_destiny_id = destination.id\n" +
            "INNER JOIN state AS d_state ON destination.state_id = d_state.id \n" +
            "INNER JOIN city AS d_city ON destination.city_id = d_city.id \n" +
            "WHERE bpr.boarding_pass_id = ?;";

    private static final String QUERY_EXPIRE_RESERVATIONS = "UPDATE boarding_pass SET boardingpass_status = 0, status = 3 where " +
            "boardingpass_status = 4 AND prepaid_id IS NULL and expires_at < ?;";

    private static final String QUERY_BOARDING_PASS_OCUPATION_IS_PASSENGER_TRIP = "SELECT\n" +
            " bo.prefix AS origin_prefix,\n" +
            "  bd.prefix AS destiny_prefix,\n" +
            "  srd.travel_date,\n" +
            "  bp.reservation_code,\n" +
            "  CONCAT(bpp.first_name, ' ', bpp.last_name) AS passenger,\n" +
            "  cu.phone,\n" +
            "  bp.purchase_origin,\n" +
            "  bp.boardingpass_status,\n" +
            "  st.name,\n" +
            "  bpt.total_amount,\n" +
            "  bp.created_at,\n" +
            "  bpt.seat,\n" +
            "  srd.id AS schedule_route_destination_id,\n" +
            "  cd.id AS config_destination_id,\n" +
            "  v.economic_number AS vehicle_economic_number\n" +
            "FROM schedule_route_destination srd\n" +
            "INNER JOIN schedule_route sr ON sr.id=srd.schedule_route_id\n" +
            "LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "LEFT JOIN config_destination cd ON cd.id = srd.config_destination_id\n" +
            "LEFT JOIN branchoffice bo ON bo.id = srd.terminal_origin_id\n" +
            "LEFT JOIN branchoffice bd ON bd.id = srd.terminal_destiny_id\n" +
            "INNER JOIN boarding_pass_route bpr ON bpr.schedule_route_destination_id = srd.id\n" +
            "INNER JOIN boarding_pass bp ON bp.id=bpr.boarding_pass_id\n" +
            "INNER JOIN boarding_pass_ticket bpt ON bpr.id=bpt.boarding_pass_route_id\n" +
            "INNER JOIN boarding_pass_passenger bpp ON bpp.id=bpt.boarding_pass_passenger_id\n" +
            "LEFT JOIN special_ticket st ON bpp.special_ticket_id=st.id\n" +
            "LEFT JOIN customer cu ON cu.id = bp.customer_id\n" +
            "WHERE bp.boardingpass_status NOT IN (0,4) and srd.travel_date BETWEEN ? AND ?\n";
            private static final String UPDATE_IS_DEFAULT_BOARDINGPASS = "UPDATE prices_lists\n" +
            "SET is_default = \n" +
            " (case when id != ? then 0 else 1 end)";
    private static final String QUERY_UPDATE_PRICE_LIST = "UPDATE prices_lists\n" +
        "SET hash = ?, error = ?, apply_status = 'error' WHERE id = ?;";
    private static final String UPDATE_HASH_PRICE_LIST = "UPDATE prices_lists SET hash = ?, apply_status = 'pending' WHERE id = ?";
            private static final String REPORT_SPECIAL_TICKETS_TYPE = "SELECT  \n" +
            "COUNT(st.id) AS quantity, \n" +
            "st.id AS special_ticket_id, \n" +
            "st.name AS special_ticket_name, \n" +
            "bpt.tracking_code, \n"+
            "st.description AS special_ticket_description, \n" +
            "bpt.id AS bptid,\n" +
            "SUM(bpt.amount) AS config_ticket_price_amount, \n" +
            "SUM(bpt.discount) AS config_ticket_price_discount, \n" +
            "SUM(bpt.extra_charges) AS boarding_pass_ticket_extra_charges, \n" +
            "SUM(bpt.extra_linear_volume) AS boarding_pass_ticket_extra_linear_volume, \n" +
            "SUM(bpt.extra_weight) AS boarding_pass_ticket_extra_extra_weight, \n" +
            "SUM(bpt.total_amount - bpt.extra_charges)  AS boarding_pass_ticket_total_amount ,\n" +
            "CONCAT(bpp.first_name , ' ' , bpp.last_name) as name ,\n" +
            "bpt.boarding_pass_route_id  as ruta_ticket \n" +
            "FROM special_ticket st \n" +
            "LEFT JOIN boarding_pass_passenger bpp ON st.id = bpp.special_ticket_id AND bpp.boarding_pass_id = ? \n" +
            "LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id \n" +
            "WHERE bpp.boarding_pass_id = ?\n" +
            "AND bpp.status = 1 \n" +
            "GROUP BY st.id, bpt.id, bpt.tracking_code;";

    private static final String REPORT_ROUTE_BY_SPECIAL_TICKET = "SELECT \n" +
            "bpr.ticket_type_route as tipo_ruta,\n" +
            "o_city.name as origen,\n" +
            "d_city.name as destino\n" +
            " FROM boarding_pass_route bpr\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            " LEFT JOIN branchoffice origin ON origin.id = srd.terminal_origin_id\n" +
            " LEFT JOIN branchoffice destiny ON destiny.id = srd.terminal_destiny_id\n" +
            " LEFT JOIN city o_city ON  origin.city_id = o_city.id\n" +
            " LEFT JOIN city d_city ON destiny.city_id = d_city.id\n" +
            " where bpr.id = ? ";

    private static final String QUERY_TRAVEL_FREQUENCY = "SELECT DISTINCT \n" +
            "customer_id\n" +
            "FROM boarding_pass \n" +
            "WHERE boardingpass_status != 0 AND boardingpass_status != 4\n" +
            "AND customer_id is not null \n" +
            "AND travel_date BETWEEN ? AND ? \n" +
            "ORDER BY customer_id  ";

    private  static final String QUERY_REPORT_GET_TRAVELS_INFO ="SELECT \n" +
            "bp.customer_id as customer_id ,\n" +
            "bp.reservation_code as reservation_code ,\n" +
            "bp.travel_date as  travel_date,\n" +
            "bp.travel_return_date as travel_return_date ,\n" +
            "bp.ticket_type as ticket_type,\n" +
            "bp.total_amount as total_amount,\n" +
            "bp.purchase_origin as purchase_origin,\n" +
            "bp.terminal_origin_id as terminal_origin_id,\n" +
            "bp.terminal_destiny_id as terminal_destiny_id,\n" +
            "bp.branchoffice_id as branchoffice_id,\n" +
            "b_origin.name as name_origin ,\n" +
            "b_destiny.name as name_destiny \n" +
            "FROM boarding_pass bp\n" +
            "LEFT JOIN branchoffice b_origin ON b_origin.id = bp.terminal_origin_id\n" +
            "LEFT JOIN branchoffice b_destiny ON b_destiny.id = bp.terminal_destiny_id\n" +
            "WHERE bp.boardingpass_status != 0 AND bp.boardingpass_status != 4\n" +
            "AND bp.customer_id is not null \n" +
            "AND bp.travel_date BETWEEN ? AND ? \n " +
            "AND bp.customer_id = ? " ;

    private static final String QUERY_REPORT_GET_CUSTOMER_INFO = "SELECT \n" +
            "coalesce(concat(first_name, ' ' , last_name)) as name,\n" +
            "IFNULL(birthday, 'No registrado') as birthday,\n" +
            "IFNULL(email, 'No registrado') as email,\n" +
            "IFNULL(phone, 'No registrado') as phone\n" +
            "FROM customer \n" +
            "WHERE id = ? ";

    private static final String QUERY_REPORT_GENERAL_V2 = "SELECT DISTINCT\n" +
            " bp.id AS boarding_pass_id,\n" +
            " (SELECT SUM(bbpt.extra_charges) FROM boarding_pass bbp \n" +
            "       INNER JOIN boarding_pass_passenger bbpp ON bbpp.boarding_pass_id = bbp.id\n" +
            "       INNER JOIN boarding_pass_ticket bbpt ON bbpt.boarding_pass_passenger_id = bbpp.id\n" +
            "       WHERE bbp.id = bp.id AND bbp.status = 1 AND bbpp.status = 1 AND bbpt.status = 1\n" +
            "       GROUP BY bbp.id) as extra_charges\n" +
            " FROM boarding_pass bp\n" +
            " LEFT JOIN tickets t ON bp.id = t.boarding_pass_id\n" +
            " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
            " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
            " LEFT JOIN city ci ON b.city_id = ci.id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
            " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
            " LEFT JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
            " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
            " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
            " LEFT JOIN customer cu ON bp.customer_id = cu.id\n" +
            " LEFT JOIN boarding_pass_passenger bpp ON bp.principal_passenger_id = bpp.id AND bpp.principal_passenger = 1\n" +
            " LEFT JOIN invoice inv ON inv.id = bp.invoice_id\n" +
            " LEFT JOIN promos pr ON pr.id = bp.promo_id\n" +
            " WHERE\n" +
            " bp.boardingpass_status != 0\n" +
            " AND bp.boardingpass_status != 4\n" +
            " AND (t.created_at BETWEEN ? AND ? OR bp.created_at BETWEEN ? AND ?) \n" +
            " AND t.total > 0    ";

    private static final String QUERY_REPORT_GET_TICKETS_INFO_GEN = "SELECT DISTINCT\n" +
            " b.id AS purchase_branchoffice_id,\n" +
            " b.prefix AS purchase_branchoffice_prefix,\n" +
            " ci.id AS purchase_city_id,\n" +
            " ci.name AS purchase_city_name,\n" +
            "   t.id,\n" +
            "   t.created_at,\n" +
            "   DATE_FORMAT(DATE_SUB(t.created_at, INTERVAL 7 HOUR),'%m/%d') as dayFormat,\n" +
            "   t.total amount,\n" +
            "   IF(td.detail like '%extras%', true, false) has_extra,\n" +
            "   IF((SELECT SUM(bbpt.extra_charges) FROM boarding_pass bbp \n" +
            "       INNER JOIN boarding_pass_passenger bbpp ON bbpp.boarding_pass_id = bbp.id\n" +
            "       INNER JOIN boarding_pass_ticket bbpt ON bbpt.boarding_pass_passenger_id = bbpp.id\n" +
            "       WHERE bbp.id = bp.id AND bbp.status = 1 AND bbpp.status = 1 AND bbpt.status = 1\n" +
            "       GROUP BY bbp.id) = t.total, IF((SELECT COUNT(tt.id) FROM boarding_pass bbp \n" +
            "       INNER JOIN boarding_pass_passenger bbpp ON bbpp.boarding_pass_id = bbp.id\n" +
            "       INNER JOIN boarding_pass_ticket bbpt ON bbpt.boarding_pass_passenger_id = bbpp.id\n" +
            "       INNER JOIN tickets tt ON tt.boarding_pass_id = bbp.id\n" +
            "       WHERE bbp.id = bp.id AND bbp.status = 1 AND bbpp.status = 1 AND bbpt.status = 1 AND tt.id <= t.id\n" +
            "       GROUP BY bbp.id) != 1, 'extra_charges', 'purchase'), IF(t.action = 'voucher', 'purchase', t.action)) AS action\n" +
            " FROM tickets t \n" +
            " INNER JOIN boarding_pass bp ON bp.id = t.boarding_pass_id \n" +
            " INNER JOIN tickets_details td ON t.id = td.ticket_id AND td.id = (SELECT MIN(id) FROM tickets_details WHERE ticket_id = t.id) \n" +
            " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
            " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id\n" +
            " LEFT JOIN city ci ON b.city_id = ci.id\n" +
            " LEFT JOIN boarding_pass_route AS bpr ON bp.id=bpr.boarding_pass_id AND bpr.ticket_type_route = 'ida'\n" +
            " LEFT JOIN schedule_route_destination AS srd ON srd.id=bpr.schedule_route_destination_id \n" +
            " LEFT JOIN config_destination AS cdd ON cdd.id = bpr.config_destination_id OR srd.config_destination_id = cdd.id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = cdd.terminal_origin_id\n" +
            " LEFT JOIN city cio ON cio.id = bo.city_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = cdd.terminal_destiny_id\n" +
            " LEFT JOIN city cid ON cid.id = bd.city_id\n" +
            " WHERE t.boarding_pass_id = ? AND t.created_at BETWEEN ? AND ? ";
    private static final String QUERY_UPDATE_BOARDINGPASS_ROUTE_INTRANSIT = "UPDATE boarding_pass_route SET route_status = 2 WHERE boarding_pass_id = ?;";

    private static final String QUERY_ADD_GROUP_BY_SEATS = " group by bpt.seat ";

    private static final String QUERY_TRAVEL_PREPAID = "SELECT \n" +
            "bp.*,\n" +
            "ppt.active_tickets,\n" +
            "ppt.used_tickets,\n" +
            "ppt.total_tickets, \n" +
            "ppt.prepaid_package_config_id, \n" +
            "ppc.max_km \n" +
            "FROM boarding_pass bp\n" +
            "LEFT JOIN prepaid_package_travel ppt ON ppt.id = bp.prepaid_id\n" +
            "LEFT JOIN prepaid_package_config ppc ON ppc.id = ppt.prepaid_package_config_id\n" +
            "WHERE bp.reservation_code = ? AND bp.boardingpass_status in (1,2,4,5) AND bp.status = 1 ";

    private static final String QUERY_SCHEDULE_DESTINATION_JOINS = "SELECT \n" +
            " srd.*, srd.terminal_origin_id, srd.terminal_destiny_id,\n" +
            " origin.prefix AS terminal_origin_prefix, destiny.prefix AS terminal_destiny_prefix\n" +
            " FROM schedule_route_destination srd " +
            " LEFT JOIN branchoffice AS origin ON origin.id = srd.terminal_origin_id \n" +
            " LEFT JOIN branchoffice AS destiny ON destiny.id = srd.terminal_destiny_id \n" +
            " WHERE srd.id = ?";

    private static final String QUERY_GET_BP_IDS_FOR_EXPIRATION = "SELECT \n" +
            "id FROM boarding_pass \n" +
            "WHERE status = 1 AND boardingpass_status = 4 AND expires_at < ? AND prepaid_id IS NOT NULL";
    //</editor-fold>
}