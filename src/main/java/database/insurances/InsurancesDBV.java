package database.insurances;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.insurances.handlers.Cost;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static service.commons.Constants.ACTION;

public class InsurancesDBV extends DBVerticle {

    // Actions
    public static final String REGISTER = "InsurancesDBV.register";
    public static final String EXPIRE_INSURANCE_POLICIES = "InsurancesDBV.expireInsurancePolicies";
    public static final String GET_CURRENT_INSURANCE = "ParcelDBV.getCurrentInsurance";
    public static final String GET_COST = "InsurancesDBV.getCost";

    // Constants
    public static final String ID = "id";

    @Override
    public String getTableName() {
        return "insurances";
    }

    Cost cost;

    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.cost = new Cost(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register(message);
                break;
            case EXPIRE_INSURANCE_POLICIES:
                this.expireInsurancePolicies(message);
                break;
            case GET_CURRENT_INSURANCE:
                this.getCurrentInsurance(message);
                break;
            case GET_COST:
                this.cost.handle(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            Date currentInsuranceInit = this.getDate(conn, message, body.getString("init"), false);
            Date currentInsuranceEnd = this.getDate(conn, message, body.getString("end"), false);
            Date today = this.getDate(conn, message, null, true);
            conn.query("SELECT id, end FROM insurances WHERE status = 1;", reply -> {
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    if(reply.result().getRows().isEmpty()){
                        this.execInsertInsurance(conn, message, body, currentInsuranceInit, currentInsuranceEnd, today);
                    } else {
                        List<JsonObject> result = reply.result().getRows();
                        if (this.getAvailableToRegister(result, conn, message, currentInsuranceInit)){
                            this.rollback(conn, new Throwable("The start date of the insurance policy is less than any of the currently active ones"), message);
                        } else {
                            this.execInsertInsurance(conn, message, body, currentInsuranceInit, currentInsuranceEnd, today);
                        }
                    }

                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.rollback(conn, reply.cause(), message);
                }

            });
        });
    }

    protected boolean getAvailableToRegister(List<JsonObject> result, SQLConnection conn, Message<JsonObject> message, Date finalCurrentInsuranceInit){
        Boolean flagInsurance = false;
        for (int i=0; i < result.size(); i++){
            JsonObject insurance = result.get(i);
            Date insuranceEnd = this.getDate(conn, message, insurance.getString("end"), false);
            if(!flagInsurance && (UtilsDate.isLowerThanEqual(finalCurrentInsuranceInit, insuranceEnd))){ flagInsurance = true; }
        }
        return flagInsurance;
    }

    protected Date getDate(SQLConnection conn, Message<JsonObject> message, String dateString, Boolean newInstance){
        Calendar cal = Calendar.getInstance();
        cal.setTime(UtilsDate.getLocalDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if(!newInstance){
            try {
                cal.setTime(sdf.parse(dateString));
            } catch (ParseException e) {
                this.rollback(conn, e.getCause(), message);
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    protected void execInsertInsurance(SQLConnection conn, Message<JsonObject> message, JsonObject body, Date finalCurrentInsuranceInit, Date finalCurrentInsuranceEnd, Date finalToday){
        if (UtilsDate.isGreaterThanEqual(finalCurrentInsuranceInit, finalToday)){
            if(UtilsDate.isLowerThanEqual(finalCurrentInsuranceEnd, UtilsDate.summCalendar(finalCurrentInsuranceInit, Calendar.YEAR, 1))){
                GenericQuery model = this.generateGenericCreate(body);
                conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> replyInsert) -> {
                    try{
                        if(replyInsert.failed()) {
                            throw new Exception(replyInsert.cause());
                        }
                        Integer insuranceId = replyInsert.result().getKeys().getInteger(0);
                        this.commit(conn, message, new JsonObject().put(ID, insuranceId));

                    }catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, replyInsert.cause(), message);
                    }

                });
            } else {
                this.rollback(conn, new Throwable("The allowed end date of the insurance policy is less than the start date"), message);
            }
        } else {
            this.rollback(conn, new Throwable("The start date of the insurance policy is less than the current date"), message);
        }
    }

    private void expireInsurancePolicies(Message<JsonObject> message) {
        String today = UtilsDate.sdfDataBase(Calendar.getInstance().getTime());
        dbClient.updateWithParams("UPDATE insurances SET status = 2 WHERE end < date(?) AND status = 1;", new JsonArray().add(today), reply -> {

            try{
                if(reply.failed()){
                    throw  new Exception(reply.cause());
                }
                message.reply(null);


            }catch(Exception e){
                message.fail(0, reply.cause().getMessage());

            }

        });
    }


    private void getCurrentInsurance(Message<JsonObject> message) {
        try {
            String now = UtilsDate.format_yyyy_MM_dd(new Date());
            this.dbClient.queryWithParams(QUERY_INSURANCE_VALIDATION, new JsonArray().add(now), replyInsValidation -> {
                try {
                    if (replyInsValidation.succeeded()) {
                        if (replyInsValidation.result().getRows().isEmpty()) {
                            reportQueryError(message, new Throwable("There are no insurance policies available"));
                        } else {
                            JsonObject insurance = replyInsValidation.result().getRows().get(0);
                            message.reply(insurance);

                        }
                    } else {
                        reportQueryError(message, replyInsValidation.cause());
                    }
                } catch (Exception e) {
                    reportQueryError(message, e.getCause());
                }
            });
        } catch (ParseException e){
            reportQueryError(message, e.getCause());
        }
    }

    private static final String QUERY_INSURANCE_VALIDATION = "SELECT id, policy_number, insurance_carrier FROM insurances WHERE ? BETWEEN init AND end AND status = 1;";

}
