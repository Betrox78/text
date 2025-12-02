package database.boardingpass;

import database.boardingpass.handlers.ApplyTicketPriceRule;
import database.boardingpass.models.TicketPriceRule;
import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

/**
 *
 * @author AllAbordo
 */
public class TicketPricesRulesDBV extends DBVerticle {

    public static final String ACTION_APPLY_TICKET_PRICE_RULE = "TicketPricesRulesDBV.apply";
    public static final String ACTION_BATCH_APPLY_TICKET_PRICE_RULE = "TicketPricesRulesDBV.batchApply";
    public static final String ACTION_GET_LIST_TICKET_PRICES_RULES_BY_STATUS = "TicketPricesRulesDBV.getList";

    public static final String INIT_DATE = "init_date";
    public static final String END_DATE = "end_date";
    public static final String BASE_SPECIAL_TICKET_ID = "base_special_ticket_id";
    public static final String APPLICABLE_SPECIAL_TICKET = "applicable_special_ticket";
    public static final String PERCENT_INCREMENT = "percent_increment";
    public static final String PERCENT_DECREMENT = "percent_decrement";
    public static final String RETURN_PERCENT_INCREMENT = "return_percent_increment";
    public static final String RETURN_PERCENT_DECREMENT = "return_percent_decrement";
    public static final String AMOUNT_INCREMENT = "amount_increment";
    public static final String AMOUNT_DECREMENT = "amount_decrement";
    public static final String RETURN_AMOUNT_INCREMENT = "return_amount_increment";
    public static final String RETURN_AMOUNT_DECREMENT = "return_amount_decrement";
    public static final String TYPE_TRAVEL = "type_travel";
    public static final String CURRENT_DATETIME = "current_datetime";


    @Override
    public String getTableName() { return "ticket_prices_rules"; }

    ApplyTicketPriceRule applyTicketPriceRule;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        applyTicketPriceRule = new ApplyTicketPriceRule(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_APPLY_TICKET_PRICE_RULE:
                applyTicketPriceRule.handle(message);
                break;
            case ACTION_BATCH_APPLY_TICKET_PRICE_RULE:
                applyTicketPriceRule.batch(message);
                break;
            case ACTION_GET_LIST_TICKET_PRICES_RULES_BY_STATUS:
                getList(message);
                break;
        }
    }

    /**
     * GENERAL METHODS <!- START -!>
     */

    @Override
    protected void create(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            String initDate = body.getString(INIT_DATE);
            String endDate = body.getString(END_DATE);
            String typeTravel = body.getString(TYPE_TRAVEL);

            checkAvailability(initDate, endDate, typeTravel, null).whenComplete((isAvailable, errorAvailability) -> {
                try {
                    if (errorAvailability != null) {
                        throw errorAvailability;
                    }

                    GenericQuery create = this.generateGenericCreate(body);
                    this.startTransaction(message, conn -> conn.updateWithParams(create.getQuery(), create.getParams(), reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            Integer id = reply.result().getKeys().getInteger(0);
                            this.commit(conn, message, new JsonObject().put(ID, id));
                        } catch (Throwable t) {
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    }));

                } catch (Throwable t) {
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<Boolean> checkAvailability(String initDate, String endDate, String typeTravel, Integer idToCompare) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String QUERY = QUERY_CHECK_AVAILABILITY;
        JsonArray params = new JsonArray().add(typeTravel).add(initDate).add(endDate).add(initDate).add(endDate);
        if (idToCompare != null) {
            QUERY += " AND id != ? ";
            params.add(idToCompare);
        }
        this.dbClient.queryWithParams(QUERY, params, reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               List<TicketPriceRule> result = reply.result().getRows().stream().map(res -> res.mapTo(TicketPriceRule.class)).collect(Collectors.toList());

               if (!result.isEmpty()) {
                   throw new Exception("The rule is already registered with this date range and type travel");
               }

               future.complete(true);
           } catch (Throwable t) {
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    @Override
    protected void update(Message<JsonObject> message) {
        try {
            TicketPriceRule request = message.body().mapTo(TicketPriceRule.class);

            getTicketPriceRule(request.getId()).whenComplete((ticketPriceRule, errorTicketPriceRule) -> {
                try {
                    if (errorTicketPriceRule != null) {
                        throw errorTicketPriceRule;
                    }

                    TicketPriceRule checkAvailabilityObj = getCheckAvailabilityRequest(request, ticketPriceRule);
                    String initDateString = UtilsDate.sdfDataBase(checkAvailabilityObj.getInitDate());
                    String endDateString = UtilsDate.sdfDataBase(checkAvailabilityObj.getEndDate());

                    checkAvailability(initDateString, endDateString, checkAvailabilityObj.getTypeTravel().name(), checkAvailabilityObj.getId()).whenComplete((isAvailable, errorAvailability) -> {
                        try {
                            if (errorAvailability != null) {
                                throw errorAvailability;
                            }

                            GenericQuery update = this.generateGenericUpdate(this.getTableName(), parseBodyUpdate(request), true);
                            this.startTransaction(message, conn -> conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                                try {
                                    if (reply.failed()) {
                                        throw reply.cause();
                                    }
                                    this.commit(conn, message, null);
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            }));

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
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private JsonObject parseBodyUpdate(TicketPriceRule request) {
        JsonObject body = JsonObject.mapFrom(request);
        body.getMap().entrySet().removeIf(entry -> entry.getValue() == null);
        if (Objects.nonNull(request.getInitDate())) {
            String initDateString = UtilsDate.sdfDataBase(request.getInitDate());
            body.put(INIT_DATE, initDateString);
        }
        if (Objects.nonNull(request.getInitDate())) {
            String endDateString = UtilsDate.sdfDataBase(request.getEndDate());
            body.put(END_DATE, endDateString);
        }
        if (Objects.isNull(request.getBaseSepcialTicketId())){
            body.putNull(BASE_SPECIAL_TICKET_ID);
        }
        if (Objects.isNull(request.getApplicableSpecialTicket())){
            body.putNull(APPLICABLE_SPECIAL_TICKET);
        }
        return body;
    }

    private TicketPriceRule getCheckAvailabilityRequest(TicketPriceRule request, TicketPriceRule ticketPriceRule) {
        TicketPriceRule returnValue = new TicketPriceRule();
        returnValue.setId(request.getId());
        returnValue.setTypeTravel(Objects.nonNull(request.getTypeTravel()) ? request.getTypeTravel() : ticketPriceRule.getTypeTravel());
        returnValue.setInitDate(Objects.nonNull(request.getInitDate()) ? request.getInitDate() : ticketPriceRule.getInitDate());
        returnValue.setEndDate(Objects.nonNull(request.getEndDate()) ? request.getEndDate() : ticketPriceRule.getEndDate());
        return returnValue;
    }

    private CompletableFuture<TicketPriceRule> getTicketPriceRule(Integer id) {
        CompletableFuture<TicketPriceRule> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_TICKET_PRICE_RULE_BY_ID, new JsonArray().add(id), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Ticket price rule not found");
                }
                TicketPriceRule ticketPriceRule = result.get(0).mapTo(TicketPriceRule.class);
                future.complete(ticketPriceRule);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getList(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String status = body.getString(STATUS);
            String currentDateTime = body.getString(CURRENT_DATETIME);

            String QUERY = status.equals("current") ? QUERY_GET_LIST_CURRENT_RULES : QUERY_GET_LIST_EXPIRED_RULES;
            this.dbClient.queryWithParams(QUERY, new JsonArray().add(currentDateTime), reply -> {
               try {
                   if (reply.failed()) {
                       throw reply.cause();
                   }
                   message.reply(new JsonArray(reply.result().getRows()));
               } catch (Throwable t) {
                   reportQueryError(message, t);
               }
            });

        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    //</editor-fold>
    private final static String QUERY_CHECK_AVAILABILITY = "SELECT * FROM ticket_prices_rules\n" +
            "WHERE type_travel = ?\n" +
            "AND status IN (1, 2) \n" +
            "AND ((? BETWEEN init_date AND end_date) \n" +
            "   OR (? BETWEEN init_date AND end_date)\n" +
            "   OR (init_date BETWEEN ? AND ?))\n";

    private final static String QUERY_GET_TICKET_PRICE_RULE_BY_ID = "SELECT id,\n" +
            "    init_date,\n" +
            "    end_date,\n" +
            "    base_special_ticket_id,\n" +
            "    percent_increment,\n" +
            "    percent_decrement,\n" +
            "    return_percent_increment,\n" +
            "    return_percent_decrement,\n" +
            "    amount_increment,\n" +
            "    amount_decrement,\n" +
            "    return_amount_increment,\n" +
            "    return_amount_decrement,\n" +
            "    type_travel,\n" +
            "    status,\n" +
            "    created_at,\n" +
            "    created_by,\n" +
            "    updated_at,\n" +
            "    updated_by\n" +
            "FROM ticket_prices_rules WHERE id = ?";

    private final static String QUERY_GET_LIST_CURRENT_RULES = "SELECT \n" +
            "   tpr.*, \n" +
            "   st.name AS base_special_ticket_name, \n" +
            "   (SELECT GROUP_CONCAT(st2.name SEPARATOR ', ') FROM special_ticket st2 WHERE FIND_IN_SET(st2.id, tpr.applicable_special_ticket) > 0) AS applicable_special_ticket_names \n" +
            "FROM ticket_prices_rules tpr \n" +
            "LEFT JOIN special_ticket st ON st.id = tpr.base_special_ticket_id\n" +
            "WHERE ? < tpr.end_date AND tpr.status IN (1, 2) ORDER BY tpr.init_date";

    private final static String QUERY_GET_LIST_EXPIRED_RULES = "SELECT tpr.*, st.name AS base_special_ticket_name FROM ticket_prices_rules tpr \n" +
            "LEFT JOIN special_ticket st ON st.id = tpr.base_special_ticket_id\n" +
            "WHERE ? > tpr.end_date AND tpr.status IN (1, 2) ORDER BY tpr.init_date";
}