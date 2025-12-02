package database.boardingpass.handlers;

import database.boardingpass.TicketPricesRulesDBV;
import database.boardingpass.enums.TICKET_TYPE;
import database.boardingpass.models.TicketPriceRule;
import database.commons.DBHandler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsMoney;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.CONFIG_ROUTE_ID;
import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.routes.SpecialTicketDBV.SPECIAL_TICKET_ID;
import static database.shipments.ShipmentsDBV.TERMINAL_ORIGIN_ID;
import static database.shipments.ShipmentsDBV.TRAVEL_DATE;
import static service.commons.Constants.*;

public class ApplyTicketPriceRule extends DBHandler<TicketPricesRulesDBV> {

    public ApplyTicketPriceRule(TicketPricesRulesDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        apply(body).whenComplete((price, errorTicketPriceRule) -> {
            try {
                if (errorTicketPriceRule != null) {
                    throw errorTicketPriceRule;
                }

                message.reply(price);
            } catch (Throwable t) {
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    public void batch(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray prices = body.getJsonArray("prices");

            List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
            prices.forEach(price -> {
                tasks.add(apply((JsonObject) price));
            });
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
                try {
                    if (error != null){
                        throw error;
                    }

                    message.reply(body);
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> apply(JsonObject price) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String ticketTypeRoute;
            if (Objects.nonNull(price.getString(TICKET_TYPE_ROUTE))) {
                ticketTypeRoute = (String) price.remove(TICKET_TYPE_ROUTE);
            } else {
                ticketTypeRoute = "ida";
            }

            database.boardingpass.enums.TICKET_TYPE ticketType;
            if (Objects.nonNull(price.getValue(TICKET_TYPE))) {
                ticketType = database.boardingpass.enums.TICKET_TYPE.valueOf((String) price.remove(TICKET_TYPE));
                if (ticketType.equals(database.boardingpass.enums.TICKET_TYPE.abierto_sencillo) && ticketTypeRoute.equals("ida")) {
                    throw new ApplyTicketPriceRuleException();
                }
                if (ticketType.equals(database.boardingpass.enums.TICKET_TYPE.abierto_redondo) && !ticketTypeRoute.equals("ida")) {
                    throw new ApplyTicketPriceRuleException();
                }
            }

            Integer configRouteId = (Integer) price.remove(CONFIG_ROUTE_ID);
            String travelDate = (String) price.remove(TRAVEL_DATE);
            Integer terminalOriginId = (Integer) price.remove(TERMINAL_ORIGIN_ID);
            Integer terminalDestinyId = (Integer) price.remove(TERMINAL_DESTINY_ID);
            Integer specialTicketId = price.getInteger(SPECIAL_TICKET_ID);

            getTicketPriceRule(travelDate, terminalOriginId, terminalDestinyId).whenComplete((ticketPriceRule, errorTicketPriceRule) -> {
                try {
                    if (errorTicketPriceRule != null) {
                        throw errorTicketPriceRule;
                    }

                    if (ticketPriceRule == null) {
                        future.complete(price);
                        return;
                    }

                    validateApplicableSpecialTicket(ticketPriceRule, specialTicketId);

                    if (ticketPriceRule.getBaseSepcialTicketId() != null) {
                        getBaseTicketPrice(configRouteId, terminalOriginId, terminalDestinyId, ticketPriceRule.getBaseSepcialTicketId()).whenComplete((basePrice, errorBasePrice) -> {
                            try {
                                if (errorBasePrice != null) {
                                    throw errorBasePrice;
                                }
                                price.put(AMOUNT, basePrice.getDouble(AMOUNT));
                                price.put(DISCOUNT, basePrice.getDouble(DISCOUNT));
                                price.put(TOTAL_AMOUNT, basePrice.getDouble(TOTAL_AMOUNT));

                                applyRule(ticketPriceRule, price, ticketTypeRoute);
                                future.complete(price);
                            } catch (Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
                    } else {
                        applyRule(ticketPriceRule, price, ticketTypeRoute);
                        future.complete(price);
                    }
                } catch (ApplyTicketPriceRuleException ex) {
                    future.complete(price);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (ApplyTicketPriceRuleException ex) {
            future.complete(price);
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<TicketPriceRule> getTicketPriceRule(String travelDate, Integer terminalOriginId, Integer terminalDestinyId){
        CompletableFuture<TicketPriceRule> future = new CompletableFuture<>();
        if (Objects.isNull(travelDate)) {
            future.complete(null);
        } else {
            this.dbClient.queryWithParams(QUERY_GET_TICKET_PRICE_RULE, new JsonArray().add(travelDate).add(terminalOriginId).add(terminalDestinyId), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();
                    future.complete(result.isEmpty() ? null : result.get(0).mapTo(TicketPriceRule.class));
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    private void validateApplicableSpecialTicket(TicketPriceRule ticketPriceRule, Integer specialTicketId) throws ApplyTicketPriceRuleException {
        if (ticketPriceRule.getApplicableSpecialTicket() != null) {
            int count = 0;
            String[] applicableSpecialTicket = ticketPriceRule.getApplicableSpecialTicket().split(",");
            for (String s : applicableSpecialTicket) {
                if (specialTicketId.equals(Integer.parseInt(s))) {
                    count++;
                    break;
                }
            }
            if (count == 0) {
                throw new ApplyTicketPriceRuleException();
            }
        }
    }

    private CompletableFuture<JsonObject> getBaseTicketPrice(Integer configRouteId, Integer terminalOriginId, Integer terminalDestinyId, Integer specialTicketId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_GET_BASE_TICKET_PRICE, new JsonArray().add(configRouteId).add(terminalOriginId).add(terminalDestinyId).add(specialTicketId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }

                List<JsonObject> result = reply.result().getRows();
                future.complete(result.isEmpty() ? null : result.get(0));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void applyRule(TicketPriceRule ticketPriceRule, JsonObject price, String ticketTypeRoute){
        Double priceAmount = price.getDouble(AMOUNT);
        Double priceDiscount = price.getDouble(DISCOUNT);
        Double priceTotalAmount = price.getDouble(TOTAL_AMOUNT);
        boolean isOneWay = ticketTypeRoute.equals("ida");
        if (isOneWay) {
            Integer amountIncrement = ticketPriceRule.getAmountIncrement();
            Integer amountDecrement = ticketPriceRule.getAmountDecrement();
            if(!amountIncrement.equals(0) || !amountDecrement.equals(0)) {
                calculateAmountOneWay(price, amountIncrement, amountDecrement, priceAmount, priceTotalAmount);
                return;
            }

            Integer percentIncrement = ticketPriceRule.getPercentIncrement();
            Integer percentDecrement = ticketPriceRule.getPercentDecrement();
            calculatePercentOneWay(price, percentIncrement, percentDecrement, priceAmount, priceDiscount, priceTotalAmount);

        } else {
            Integer returnAmountIncrement = ticketPriceRule.getReturnAmountIncrement();
            Integer returnAmountDecrement = ticketPriceRule.getReturnAmountDecrement();
            if(!returnAmountIncrement.equals(0) || !returnAmountDecrement.equals(0)) {
                calculateAmountReturn(price, returnAmountIncrement, returnAmountDecrement, priceAmount, priceTotalAmount);
                return;
            }

            Integer returnPercentIncrement = ticketPriceRule.getReturnPercentIncrement();
            Integer returnPercentDecrement = ticketPriceRule.getReturnPercentDecrement();
            calculatePercentReturn(price, returnPercentIncrement, returnPercentDecrement, priceAmount, priceDiscount, priceTotalAmount);
        }
    }

    private void calculateAmountOneWay(JsonObject price, Integer amountIncrement, Integer amountDecrement, Double ticketAmount, Double ticketTotalAmount) {
        Double amount = ticketAmount + amountIncrement - amountDecrement;
        Double totalAmount = ticketTotalAmount + amountIncrement - amountDecrement;
        price.put(AMOUNT, amount);
        price.put(TOTAL_AMOUNT, totalAmount);
    }

    private void calculatePercentOneWay(JsonObject price, Integer percentIncrement, Integer percentDecrement, Double ticketAmount, Double ticketDiscount, Double ticketTotalAmount) {
        if (!percentIncrement.equals(0)) {
            double amount = ticketAmount * (1 + (percentIncrement / 100.0));
            double discount = ticketDiscount * (1 + (percentIncrement / 100.0));
            double totalAmount = ticketTotalAmount * (1 + (percentIncrement / 100.0));
            price.put(AMOUNT, UtilsMoney.round(amount, 2));
            price.put(DISCOUNT, UtilsMoney.round(discount, 2));
            price.put(TOTAL_AMOUNT, UtilsMoney.round(totalAmount, 2));
        } else if (!percentDecrement.equals(0)) {
            double amount = ticketAmount - (ticketAmount * (percentDecrement / 100.0));
            double discount = ticketDiscount - (ticketDiscount * (percentDecrement / 100.0));
            double totalAmount = ticketTotalAmount - (ticketTotalAmount * (percentDecrement / 100.0));
            price.put(AMOUNT, UtilsMoney.round(amount, 2));
            price.put(DISCOUNT, UtilsMoney.round(discount, 2));
            price.put(TOTAL_AMOUNT, UtilsMoney.round(totalAmount, 2));
        }
    }

    private void calculateAmountReturn(JsonObject price, Integer returnAmountIncrement, Integer returnAmountDecrement, Double ticketAmount, Double ticketTotalAmount) {
        Double amount = ticketAmount + returnAmountIncrement - returnAmountDecrement;
        Double totalAmount = ticketTotalAmount + returnAmountIncrement - returnAmountDecrement;
        price.put(AMOUNT, amount);
        price.put(TOTAL_AMOUNT, totalAmount);
    }

    private void calculatePercentReturn(JsonObject price, Integer returnPercentIncrement, Integer returnPercentDecrement, Double ticketAmount, Double ticketDiscount, Double ticketTotalAmount) {
        if (!returnPercentIncrement.equals(0)) {
            double amount = ticketAmount * (1 + (returnPercentIncrement / 100.0));
            double discount = ticketDiscount * (1 + (returnPercentIncrement / 100.0));
            double totalAmount = ticketTotalAmount * (1 + (returnPercentIncrement / 100.0));
            price.put(AMOUNT, UtilsMoney.round(amount, 2));
            price.put(DISCOUNT, UtilsMoney.round(discount, 2));
            price.put(TOTAL_AMOUNT, UtilsMoney.round(totalAmount, 2));
        } else if (!returnPercentDecrement.equals(0)) {
            double amount = ticketAmount - (ticketAmount * (returnPercentDecrement / 100.0));
            double discount = ticketDiscount - (ticketDiscount * (returnPercentDecrement / 100.0));
            double totalAmount = ticketTotalAmount - (ticketTotalAmount * (returnPercentDecrement / 100.0));
            price.put(AMOUNT, UtilsMoney.round(amount, 2));
            price.put(DISCOUNT, UtilsMoney.round(discount, 2));
            price.put(TOTAL_AMOUNT, UtilsMoney.round(totalAmount, 2));
        }
    }

    private static final String QUERY_GET_TICKET_PRICE_RULE = "SELECT\n" +
            "   tpr.id,\n" +
            "   tpr.init_date,\n" +
            "   tpr.end_date,\n" +
            "   tpr.base_special_ticket_id,\n" +
            "   tpr.percent_increment,\n" +
            "   tpr.percent_decrement,\n" +
            "   tpr.return_percent_increment,\n" +
            "   tpr.return_percent_decrement,\n" +
            "   tpr.amount_increment,\n" +
            "   tpr.amount_decrement,\n" +
            "   tpr.return_amount_increment,\n" +
            "   tpr.return_amount_decrement,\n" +
            "   tpr.type_travel,\n" +
            "   tpr.applicable_special_ticket,\n" +
            "   tpr.status,\n" +
            "   tpr.created_at,\n" +
            "   tpr.created_by,\n" +
            "   tpr.updated_at,\n" +
            "   tpr.updated_by\n" +
            "FROM ticket_prices_rules tpr\n" +
            "WHERE tpr.status = 1 \n" +
            "AND ? BETWEEN tpr.init_date AND tpr.end_date\n" +
            "AND tpr.type_travel = (SELECT cbtt.type_travel FROM config_branchoffices_travel_type cbtt \n" +
            "WHERE cbtt.terminal_origin_id = ? AND cbtt.terminal_destiny_id = ?);";

    private static final String QUERY_GET_BASE_TICKET_PRICE = "SELECT\n" +
            "   ctp.amount,\n" +
            "   ctp.discount,\n" +
            "   ctp.total_amount\n" +
            "FROM config_route cr\n" +
            "INNER JOIN config_destination cd ON cd.config_route_id = cr.id\n" +
            "INNER JOIN config_ticket_price ctp ON ctp.config_destination_id = cd.id\n" +
            "WHERE ctp.status = 1 AND cr.id = ?\n" +
            "   AND cd.terminal_origin_id = ? AND cd.terminal_destiny_id = ?\n" +
            "   AND ctp.special_ticket_id = ?;";

}

class ApplyTicketPriceRuleException extends Exception {
    ApplyTicketPriceRuleException(){
        super();
    }
}