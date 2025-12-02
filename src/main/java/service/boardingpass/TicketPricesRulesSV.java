package service.boardingpass;

import database.boardingpass.TicketPricesRulesDBV;
import database.commons.ErrorCodes;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsDate;
import utils.UtilsResponse;
import utils.UtilsValidation;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static database.boardingpass.TicketPricesRulesDBV.*;
import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;

/**
 *
 * @author AllAbordo
 */
public class TicketPricesRulesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TicketPricesRulesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/ticketPricesRules";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/list", AuthMiddleware.getInstance(), this::getList);
        super.start(startFuture);
    }

    private void getList(RoutingContext context){
        try{
            JsonObject body = context.getBodyAsJson();
            isContainedAndNotNull(body, STATUS, "current", "expired");
            isEmptyAndNotNull(body, CURRENT_DATETIME);

            vertx.eventBus().send(this.getDBAddress(), body, options(TicketPricesRulesDBV.ACTION_GET_LIST_TICKET_PRICES_RULES_BY_STATUS), (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        responseOk(context, reply.result().body(), "Found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, t);
                }
            });

        }catch(UtilsValidation.PropertyValueException ex){
            UtilsResponse.responsePropertyValue(context, ex);
        }catch (Exception e){
            responseError(context, "Ocurrió un error inesperado", e.getMessage());
        }
    }

    @Override
    protected boolean isValidCreateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, INIT_DATE, "body");
            isDateTimeAndNotNull(body, END_DATE, "body");

            Date initDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(body.getString(INIT_DATE), "SDF");
            Date endDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(body.getString(END_DATE), "SDF");
            Calendar date = Calendar.getInstance();
            if(UtilsDate.isLowerThan(initDate, UtilsDate.parse_yyyy_MM_dd(UtilsDate.format_yyyy_MM_dd(date.getTime())))){
                throw new PropertyValueException(INIT_DATE, "Date must be greater than or equal to today's date");
            }
            if (UtilsDate.isLowerThan(endDate, initDate)){
                throw new PropertyValueException(END_DATE, "Date must be greater than to the date" + INIT_DATE);
            }

            isGrater(body, BASE_SPECIAL_TICKET_ID, 0);
            isGraterAndNotNull(body, PERCENT_INCREMENT, -1);
            isGraterAndNotNull(body, PERCENT_DECREMENT, -1);
            isGraterAndNotNull(body, RETURN_PERCENT_INCREMENT, -1);
            isGraterAndNotNull(body, RETURN_PERCENT_DECREMENT, -1);
            isGraterAndNotNull(body, AMOUNT_INCREMENT, -1);
            isGraterAndNotNull(body, AMOUNT_DECREMENT, -1);
            isGraterAndNotNull(body, RETURN_AMOUNT_INCREMENT, -1);
            isGraterAndNotNull(body, RETURN_AMOUNT_DECREMENT, -1);
            isContained(body, TYPE_TRAVEL,
                    database.boardingpass.enums.TYPE_TRAVEL.local.name(),
                    database.boardingpass.enums.TYPE_TRAVEL.foreign.name());
            return super.isValidCreateData(context);
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            return UtilsResponse.responsePropertyValue(context, ex);
        } catch (ParseException e){
            e.printStackTrace();
            return UtilsResponse.responsePropertyValue(context, new PropertyValueException(INIT_DATE + " or " + END_DATE, INVALID_FORMAT));
        }
    }

    @Override
    protected boolean isValidUpdateData(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isGraterAndNotNull(body, ID, 0);
            isDateTime(body, INIT_DATE);
            isDateTime(body, END_DATE);

            if (Objects.nonNull(body.getString(INIT_DATE)) && Objects.nonNull(body.getString(END_DATE))) {
                Date initDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(body.getString(INIT_DATE), "SDF");
                Date endDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(body.getString(END_DATE), "SDF");
                Calendar date = Calendar.getInstance();
                if(UtilsDate.isLowerThan(initDate, UtilsDate.parse_yyyy_MM_dd(UtilsDate.format_yyyy_MM_dd(date.getTime())))){
                    throw new PropertyValueException(INIT_DATE, "Date must be greater than or equal to today's date");
                }
                if (UtilsDate.isLowerThan(endDate, initDate)){
                    throw new PropertyValueException(END_DATE, "Date must be greater than to the " + INIT_DATE);
                }
            }

            isGrater(body, BASE_SPECIAL_TICKET_ID, 0);
            isGrater(body, PERCENT_INCREMENT, -1);
            isGrater(body, PERCENT_DECREMENT, -1);
            isGrater(body, RETURN_PERCENT_INCREMENT, -1);
            isGrater(body, RETURN_PERCENT_DECREMENT, -1);
            isGrater(body, AMOUNT_INCREMENT, -1);
            isGrater(body, AMOUNT_DECREMENT, -1);
            isGrater(body, RETURN_AMOUNT_INCREMENT, -1);
            isGrater(body, RETURN_AMOUNT_DECREMENT, -1);
            isBetweenRange(body, STATUS, 1, 4);
            isContained(body, TYPE_TRAVEL,
                    database.boardingpass.enums.TYPE_TRAVEL.local.name().toLowerCase(),
                    database.boardingpass.enums.TYPE_TRAVEL.foreign.name().toLowerCase());
            return super.isValidUpdateData(context);
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            return UtilsResponse.responsePropertyValue(context, ex);
        } catch (ParseException e){
            e.printStackTrace();
            return UtilsResponse.responsePropertyValue(context, new PropertyValueException(INIT_DATE+ " or " + END_DATE, INVALID_FORMAT));
        }
    }

}
