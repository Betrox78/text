/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.money;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.UpdateResult;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import utils.UtilsDate;

import static service.commons.Constants.*;

/**
 *
 * @author ulises
 */
public class CashOutDBV extends DBVerticle {

    public static final String ACTION_OPEN_CASH_OUT = "CashOutDBV.openCashOut";
    public static final String ACTION_OPEN_CASH_OUT_DRIVER = "CashOutDBV.openCashOutDriver";
    public static final String ACTION_CASH_OUT_CUT = "CashOutDBV.closeReport";
    public static final String ACTION_CASH_OUT_DETAIL = "CashOutDBV.getDetail";
    public static final String ACTION_CHECK_CASH_OUT_EMPLOYEE = "CashOutDBV.checkCashOutEmployee";
    public static final String ACTION_GET_OPENED_CASH_OUT_ID = "CashOutDBV.getOpenedCashOut";
    public static final String ACTION_GET_CASH_OUT_OPENED_WITHOUT_EMPLOYEE = "CashOutDBV.getCashOutWithoutEmployee";
    public static final String ACTION_GET_CASH_OUT_EMPLOYEE = "CashOutDBV.getCashOutEmployee";
    public static final String ACTION_GET_CASH_OUT_EMPLOYEE_BY_ID = "CashOutDBV.getCashOutEmployeeById";
    public static final String ACTION_CASH_OUT_EXTENDED = "CashOutDBV.getExtended";
    public static final String ACTION_CASH_OUT_RESUME_SALES = "CashOutDBV.getResumeSales";
    public static final String Z_CASH_OUT_REPORT = "CashOutDBV.zReport";
    public static final String ACTION_CASH_OUT_REPORT = "CashOutDBV.getReport";

    public static final String CASH_OUT_ORIGIN = "cash_out_origin";
    public static final String DRIVER = "driver";

    private final JsonArray ACTIONS = new JsonArray().add("purchase").add("cancel").add("changes").add("expense").add("income").add("withdrawall").add("guarantee_deposit");
    private final JsonArray ACTIONS_EXTENDED = new JsonArray().add("income").add("expense").add("withdrawal");
    private final JsonArray SOME = new JsonArray().add("boarding_pass").add("rental").add("parcel");
    private final List<String> VOUCHERS = new ArrayList<>(Arrays.asList("card", "debit"));

    @Override
    public String getTableName() {
        return "cash_out";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_OPEN_CASH_OUT:
                this.openCashOut(message);
                break;
            case ACTION_OPEN_CASH_OUT_DRIVER:
                this.openCashOutDriver(message);
                break;
            case ACTION_CASH_OUT_CUT:
                this.closeReport(message);
                break;
            case ACTION_CASH_OUT_DETAIL:
                this.getDetail(message);
                break;
            case ACTION_CHECK_CASH_OUT_EMPLOYEE:
                this.checkCashOutEmployee(message);
                break;
            case ACTION_GET_OPENED_CASH_OUT_ID:
                this.getOpenedCashOut(message);
                break;
            case ACTION_GET_CASH_OUT_OPENED_WITHOUT_EMPLOYEE:
                this.getCashOutWithoutEmployee(message);
                break;
            case ACTION_CASH_OUT_EXTENDED:
                this.getExtended(message);
                break;
            case ACTION_GET_CASH_OUT_EMPLOYEE:
                this.getCashOutEmployee(message);
                break;
            case ACTION_GET_CASH_OUT_EMPLOYEE_BY_ID:
                this.getCashOutEmployeeById(message);
                break;
            case Z_CASH_OUT_REPORT:
                this.zReport(message);
                break;
            case ACTION_CASH_OUT_RESUME_SALES:
                this.handleResumeSales(message);
                break;
            case ACTION_CASH_OUT_REPORT:
                this.getReport(message);
                break;

        }
    }

    public enum STATUS {
        CANCELED,
        OPEN,
        CLOSED
    }

    private void zReport(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer terminalId = body.getInteger("branchoffice_id");
        String initialDate = body.getString("init_date");
        String endDate = body.getString("end_date");
        JsonArray paymentMethods = body.getJsonArray("payment_methods");
        JsonObject totalResult = new JsonObject();
        getCashRegisters(terminalId, initialDate, endDate).whenComplete((result, error) -> {
            try {
                if (error != null) {
                    throw new Exception(error);
                }
                JsonArray cashRegisters = new JsonArray();
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                result.forEach(r -> {
                    tasks.add(this.getCashRegistersDetail(r, cashRegisters, initialDate, endDate, paymentMethods));
                });
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                    try {
                        if (err != null) {
                            throw new Exception(err);
                        }
                        totalResult.put("cash_registers", cashRegisters);
                        Double initialFund = 0.0;
                        Double vouchers = 0.0;
                        Double totalOnRegister = 0.0;
                        Double totalReported = 0.0;
                        Double diference;
                        JsonObject netAmounts = new JsonObject();
                        for (int i = 0; i < cashRegisters.size(); i++) {
                            JsonObject resp = cashRegisters.getJsonObject(i);
                            JsonArray cashOuts = resp.getJsonArray("cashouts", new JsonArray());
                            if (!cashOuts.isEmpty()) {
                                initialFund = initialFund + cashOuts.getJsonObject(0).getDouble("starting_amount");
                                for (int x = 0; x < cashOuts.size(); x++) {
                                    JsonObject cashOut = cashOuts.getJsonObject(x);
                                    getCashOutPaymentMethods(paymentMethods)
                                            .forEach(pm -> {
                                                Double cashOutPayment = cashOut.getDouble(pm, 0.0);
                                                netAmounts.put(pm, netAmounts.getDouble(pm, 0.0) + (cashOutPayment != null ? cashOutPayment : 0.0));
                                            });
                                    vouchers = vouchers + cashOut.getDouble("vouchers");
                                    totalOnRegister = totalOnRegister + cashOut.getDouble("total_on_register");
                                    totalReported = totalReported + cashOut.getDouble("total_report");
                                }
                            }
                        }
                        diference = totalOnRegister - totalReported;

                        netAmounts
                                .put("initial_fund", initialFund)
                                .put("vouchers", vouchers);
                        JsonObject totalsDay = new JsonObject()
                                .put("total_registered", totalOnRegister)
                                .put("total_report", totalReported)
                                .put("diference", diference);
                        JsonArray params = new JsonArray()
                                .add(terminalId).add(initialDate).add(endDate);

                        totalResult.put("net_amounts", netAmounts).put("totals_day", totalsDay);
                        Future<ResultSet> querySearchBoardingPassAndVansTotals = Future.future();
                        Future<ResultSet> querySearchServiceDetailExpense = Future.future();
                        this.dbClient.queryWithParams(QUERY_GET_SERVICES, params, querySearchBoardingPassAndVansTotals.completer());
                        this.dbClient.queryWithParams(QUERY_GET_SERVICE_DETAIL_EXPENSE, params, querySearchServiceDetailExpense.completer());
                        CompositeFuture.all(querySearchBoardingPassAndVansTotals, querySearchServiceDetailExpense).setHandler(replyRes -> {
                            try {
                                if (replyRes.failed()) {
                                    throw replyRes.cause();
                                }
                                JsonObject resultSells = replyRes.result().<ResultSet>resultAt(0).getRows().get(0);
                                JsonObject resultExpense = replyRes.result().<ResultSet>resultAt(1).getRows().get(0);
                                JsonObject sellsBoardingPassess = new JsonObject()
                                        .put("concept", "Boletos vendidos")
                                        .put("count", resultSells.getInteger("boarding_pass_count"))
                                        .put("total_amount", resultSells.getDouble("boarding_pass_total"));
                                JsonObject cancellBoardingPassess = new JsonObject()
                                        .put("concept", "Boletos cancelados")
                                        .put("count", resultExpense.getInteger("boarding_pass_count"))
                                        .put("total_amount", resultExpense.getDouble("boarding_pass_total"));
                                JsonObject vansRental = new JsonObject()
                                        .put("concept", "Renta de vans")
                                        .put("count", resultSells.getInteger("rental_count"))
                                        .put("total_amount", resultSells.getDouble("rental_total"));
                                JsonObject vansRentalCancel = new JsonObject()
                                        .put("concept", "Rentas canceladas")
                                        .put("count", resultExpense.getInteger("rental_count"))
                                        .put("total_amount", resultExpense.getDouble("rental_total"));
                                JsonObject sellsParcel = new JsonObject()
                                        .put("concept", "Venta de paqueteria")
                                        .put("count", resultSells.getInteger("parcel_sells_count"))
                                        .put("total_amount", resultSells.getDouble("parcel_sells_total"));
                                JsonObject chargeParcel = new JsonObject()
                                        .put("concept", "Cobro de paqueteria")
                                        .put("count", resultSells.getInteger("parcel_charge_count"))
                                        .put("total_amount", resultSells.getDouble("parcel_charge_total"));
                                JsonObject cancelParcel = new JsonObject()
                                        .put("concept", "Cancelaciones")
                                        .put("count", resultExpense.getInteger("parcel_count"))
                                        .put("total_amount", resultExpense.getDouble("parcel_total"));
                                totalResult.put("services_detail", new JsonObject()
                                        .put("travels", new JsonArray().add(sellsBoardingPassess).add(cancellBoardingPassess))
                                        .put("rentals", new JsonArray().add(vansRental).add(vansRentalCancel))
                                        .put("parcels", new JsonArray().add(sellsParcel).add(chargeParcel).add(cancelParcel))
                                );
                                message.reply(totalResult);
                            } catch (Throwable ex) {
                                ex.printStackTrace();
                                reportQueryError(message, ex);
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private String getCashOutPaymentMethodAlias(String alias) {
        switch(alias) {
            case "check":
                return "checks";
            default:
                return alias;
        }
    }

    private List<String> getCashOutPaymentMethods(JsonArray methods) {
        List<JsonObject> methodsList = methods.stream().map(m -> (JsonObject)m).collect(Collectors.toList());
        return getNonVouchersPaymentMethods(methodsList).stream()
                .map(pm -> getCashOutPaymentMethodAlias(pm.getString("alias")))
                .collect(Collectors.toList());
    }

    private CompletableFuture<JsonObject> getCashRegistersDetail(JsonObject cashRegister, JsonArray cashRegisters, String initDate, String endDate, JsonArray paymentMethods) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Integer cashRegisterId = cashRegister.getInteger("id");
        String cashRegisterName = cashRegister.getString("cash_register");
        String QUERY = QUERY_GET_CASH_REGISTER_DETAIL;
        String paymentMethodsQuery = getCashOutPaymentMethods(paymentMethods).stream()
                .map(pm -> "co." + pm)
                .collect(Collectors.joining(","));
        QUERY = QUERY.replace("{PM}", paymentMethodsQuery);
        this.dbClient.queryWithParams(QUERY, new JsonArray().add(cashRegisterId).add(initDate).add(endDate), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                JsonObject resultDetail = new JsonObject();
                if(result.isEmpty()){
                    future.complete(new JsonObject());
                    return;
                }
                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                for(int i = 0; i < result.size();i++){
                    JsonObject cashOut = result.get(i);
                    tasks.add(cashRegisterDetails(getPaymentMethodsByOrigin(paymentMethods, cashOut), cashOut));
                }
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));

                allTasks.whenComplete((s, t) -> {
                    try {
                       if(t != null){
                           throw new Exception(t.getCause());
                       }
                       Double totalIncome = 0.0;
                       Double totalCancel = 0.0;
                       Double total = 0.0;
                       for(int x = 0; x < result.size(); x++){
                            totalIncome += result.get(x).getDouble("total_income");
                            totalCancel += result.get(x).getDouble("total_cancel");
                            total += result.get(x).getDouble("totals");
                       }
                       resultDetail
                               .put("total_income", totalIncome)
                               .put("total_cancel",totalCancel)
                               .put("total", total);
                        cashRegisters.add(resultDetail);
                        resultDetail.put("name", cashRegisterName).put("cashouts", result);
                        future.complete(new JsonObject());
                    } catch (Exception e){
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
    private CompletableFuture<JsonObject> cashRegisterDetails(List<JsonObject> paymentMethods, JsonObject cashOut) {
        Integer cashOutId = cashOut.getInteger(ID);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        CompositeFuture.all(
                getTotalOnRegister(paymentMethods, cashOutId),
                getTotalCancel(paymentMethods, cashOutId)
        ).setHandler(replyTotals -> {
            try {
                if (replyTotals.failed()) {
                    throw new Exception(replyTotals.cause());
                }
                JsonObject totalOnRegister = replyTotals.result().resultAt(0);
                JsonObject totalCancel = replyTotals.result().resultAt(1);
                cashOut
                    .put("total_income", totalOnRegister.getDouble("total_income", 0.0))
                    .put("total_cancel", totalCancel.getDouble("total", 0.0))
                    .put("totals", totalOnRegister.getDouble("total", 0.0));
                future.complete(new JsonObject());
            } catch (Throwable t) {
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }
    private CompletableFuture<List<JsonObject>> getCashRegisters(Integer terminalId, String initDate, String endDate){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try{
            this.dbClient.queryWithParams(QUERY_GET_CASH_REGISTER_BY_TERMINAL_ID,new JsonArray().add(terminalId).add(initDate).add(endDate), reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Cash registers not found");
                    }
                    future.complete(result);
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

    private void openCashOut(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                JsonObject body = message.body();
                Integer cashRegisterId = body.getInteger("cash_register_id");
                Integer createdBy = body.getInteger("created_by");
                GenericQuery gen = this.generateGenericCreate(body);
                con.setOptions(new SQLOptions().setAutoGeneratedKeys(true));
                String origin = body.containsKey("cash_out_origin") ? body.getString("cash_out_origin") : "branchoffice";
                if(origin.equals("driver")){
                    con.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> cashOutReply) -> {
                        try{
                            if(cashOutReply.failed()) {
                                throw new Exception(cashOutReply.cause());
                            }
                            final int cashOutId = cashOutReply.result().getKeys().getInteger(0);
                            this.commit(con, message, new JsonObject().put("id", cashOutId));
                        }catch (Exception ex) {
                            ex.printStackTrace();
                            this.rollback(con, ex, message);
                        }
                    });
                }else{
                    this.updateCashRegister(con, cashRegisterId, 1, createdBy).whenComplete((JsonObject cashRegister, Throwable error) -> {
                        if (error != null) {
                            this.rollback(con, error, message);
                        } else {
                            con.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> cashOutReply) -> {
                                try{
                                    if(cashOutReply.failed()) {
                                        throw new Exception(cashOutReply.cause());
                                    }

                                    final int cashOutId = cashOutReply.result().getKeys().getInteger(0);

                                    getLastTicketCashRegister(con, cashRegisterId).whenComplete((lastTicket, errorLastTicket) -> {
                                        try {
                                            if (errorLastTicket != null) {
                                                throw errorLastTicket;
                                            }

                                            JsonObject cashOutUpdate = new JsonObject().put("id", cashOutId).put("init_ticket", lastTicket);
                                            GenericQuery updateInitticket = this.generateGenericUpdate("cash_out", cashOutUpdate);
                                            con.updateWithParams(updateInitticket.getQuery(), updateInitticket.getParams(), (AsyncResult<UpdateResult> reply) ->{
                                                try {
                                                    if (reply.failed()) {
                                                        throw new Exception(cashOutReply.cause());
                                                    }

                                                    this.commit(con, message, new JsonObject()
                                                            .put("id", cashOutId)
                                                            .put("last_ticket", lastTicket)
                                                    );
                                                }catch (Exception ex) {
                                                    ex.printStackTrace();
                                                    this.rollback(con, ex, message);
                                                }
                                            });

                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                            this.rollback(con, ex, message);
                                        }
                                    });
                                }catch (Exception ex) {
                                    ex.printStackTrace();
                                    this.rollback(con, ex, message);
                                }
                            });
                        }
                    });
                }
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private void openCashOutDriver(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                body.put(CASH_OUT_ORIGIN, DRIVER);
                Integer employeeId = body.getInteger(EMPLOYEE_ID);
                conn.queryWithParams(QUERY_GET_OPENED_CASH_OUT_BY_DRIVER, new JsonArray().add(employeeId), replyDriverCSOpen -> {
                    try {
                        if (replyDriverCSOpen.failed()){
                            throw replyDriverCSOpen.cause();
                        }

                        List<JsonObject> resultDriverCSOpen = replyDriverCSOpen.result().getRows();
                        if(!resultDriverCSOpen.isEmpty()){
                            throw new Exception("Driver have a opened cash out");
                        }

                        GenericQuery gen = this.generateGenericCreate(body);
                        conn.setOptions(new SQLOptions().setAutoGeneratedKeys(true));
                        conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> cashOutReply) -> {
                            try{
                                if(cashOutReply.failed()) {
                                    throw new Exception(cashOutReply.cause());
                                }
                                final int cashOutId = cashOutReply.result().getKeys().getInteger(0);
                                this.commit(conn, message, new JsonObject().put(ID, cashOutId));
                            }catch (Exception ex) {
                                ex.printStackTrace();
                                this.rollback(conn, ex, message);
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

    private CompletableFuture<Integer> getLastTicketCashRegister(SQLConnection con, Integer cashRegisterId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        con.queryWithParams(QUERY_GET_LAST_TICKET_CASH_REGISTER, new JsonArray().add(cashRegisterId), replyFind -> {
            try {
                if (replyFind.failed()) {
                    throw replyFind.cause();
                }

                List<JsonObject> results = replyFind.result().getRows();
                if (results.isEmpty()) {
                    throw new Exception("Cash register not found");
                }

                Integer lastTicket = results.get(0).getInteger("last_ticket");
                future.complete(lastTicket);
            } catch (Throwable ex) {
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private void closeReport(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                List<String> batch = new ArrayList<>();
                JsonObject body = message.body();
                JsonArray paymentMethods = body.getJsonArray("payment_methods");
                final int cashOutId = body.getInteger("cash_out_id");
                //add details
                JsonArray details = body.getJsonArray("cash_out_details");
                if (details != null && !details.isEmpty()) {
                    for (int i = 0; i < details.size(); i++) {
                        JsonObject detail = details.getJsonObject(i);
                        detail.put("cash_out_id", cashOutId);
                        batch.add(this.generateGenericCreate("cash_out_detail", detail));
                    }
                }
                String origin = body.containsKey("cash_out_origin") ? body.getString("cash_out_origin") : "branchoffice";

                if(origin.equals("driver")){
                    Integer driverBranchOfficeClose = (Integer) body.remove("branchoffice_id");
                    JsonArray params = new JsonArray()
                            .add(cashOutId).add(cashOutId).add(cashOutId);
                    con.queryWithParams(QUERY_CASH_OUT_SUM_QUANTITY, params, queryReply -> {
                        try{
                            if(queryReply.failed()) {
                                throw new Exception(queryReply.cause());
                            }
                            //add update cashout
                            List<JsonObject> res = queryReply.result().getRows();
                            Float totalOnRegister = res.get(0).getFloat("total_on_register", 0f);
                            Float initialFund = res.get(0).getFloat("initial_fund", 0f);
                            if (totalOnRegister == null) {
                                totalOnRegister = 0f;
                            }
                            if (initialFund != null) {
                                totalOnRegister = totalOnRegister + initialFund;
                            }
                            String hasDiference = this.hasDiference(
                                    body.getFloat("total_reported"),
                                    totalOnRegister);
                            JsonObject update = new JsonObject()
                                    .put("id", cashOutId)
                                    .put("branchoffice_id",driverBranchOfficeClose)
                                    .put("final_fund", body.getFloat("final_fund"))
                                    .put("vouchers", body.getFloat("vouchers"))
                                    .put("total_reported", body.getFloat("total_reported"))
                                    .put("total_on_register", totalOnRegister)
                                    .put("notes", body.getString("notes"))
                                    .put("has_diference", hasDiference)
                                    .put("cash_out_status", 2)
                                    .put("updated_by", body.getInteger("created_by"))
                                    .put("updated_at", UtilsDate.sdfDataBase(new Date()));

                            getCashOutPaymentMethods(paymentMethods)
                                    .forEach(pm -> {
                                        String key = pm.equals("checks") ? "check" : pm;
                                        update.put(pm, body.getFloat(key));
                                    });

                            batch.add(this.generateGenericUpdateString("cash_out", update));
                            con.batch(batch, reply -> {
                                try{
                                    if(reply.failed()){
                                        throw  new Exception(reply.cause());
                                    }

                                    this.commit(con, message, null);

                                }catch(Exception e){
                                    this.rollback(con, e, message);

                                }

                            });

                        }catch (Exception ex) {
                            ex.printStackTrace();
                            this.rollback(con, ex, message);
                        }
                    });
                }else{
                    // close cash register
                    this.updateCashRegister(con, body.getInteger("cash_register_id"), 0, body.getInteger("created_by")).whenComplete((JsonObject cashRegister, Throwable error) -> {
                         try{
                             if (error != null) {
                                throw error;
                             } else {
                                 //query moves
                                 JsonArray params = new JsonArray()
                                         .add(cashOutId).add(cashOutId).add(cashOutId);
                                 con.queryWithParams(QUERY_CASH_OUT_SUM_QUANTITY, params, queryReply -> {
                                     try{
                                         if(queryReply.failed()) {
                                             throw new Exception(queryReply.cause());
                                         }

                                         getLastTicketCashRegister(con, body.getInteger("cash_register_id")).whenComplete((lastTicket, errorLastTicket) -> {
                                             try {
                                                 if (errorLastTicket != null) {
                                                     throw errorLastTicket;
                                                 }

                                                 //add update cashout
                                                 List<JsonObject> res = queryReply.result().getRows();
                                                 Float totalOnRegister = res.get(0).getFloat("total_on_register", 0f);
                                                 Float initialFund = res.get(0).getFloat("initial_fund", 0f);
                                                 if (totalOnRegister == null) {
                                                     totalOnRegister = 0f;
                                                 }
                                                 if (initialFund != null) {
                                                     totalOnRegister = totalOnRegister + initialFund;
                                                 }
                                                 String hasDiference = this.hasDiference(
                                                         body.getFloat("total_reported"),
                                                         totalOnRegister);
                                                 JsonObject update = new JsonObject()
                                                         .put("id", cashOutId)
                                                         .put("final_fund", body.getFloat("final_fund"))
                                                         .put("vouchers", body.getFloat("vouchers"))
                                                         .put("total_reported", body.getFloat("total_reported"))
                                                         .put("total_on_register", totalOnRegister)
                                                         .put("notes", body.getString("notes"))
                                                         .put("has_diference", hasDiference)
                                                         .put("cash_out_status", 2)
                                                         .put("updated_by", body.getInteger("created_by"))
                                                         .put("updated_at", UtilsDate.sdfDataBase(new Date()))
                                                         .put("last_ticket", lastTicket);

                                                 getCashOutPaymentMethods(paymentMethods)
                                                         .forEach(pm -> {
                                                             String key = pm.equals("checks") ? "check" : pm;
                                                             update.put(pm, body.getFloat(key));
                                                         });

                                                 batch.add(this.generateGenericUpdateString("cash_out", update));
                                                 con.batch(batch, reply -> {
                                                     try{
                                                         if(reply.failed()){
                                                             throw  new Exception(reply.cause());
                                                         }
                                                         this.commit(con, message, null);
                                                     }catch(Exception e){
                                                         this.rollback(con, e, message);
                                                     }
                                                 });
                                             } catch (Throwable ex) {
                                                 ex.printStackTrace();
                                                 this.rollback(con, ex, message);
                                             }
                                         });

                                     }catch (Exception ex) {
                                         ex.printStackTrace();
                                         this.rollback(con, ex, message);
                                     }
                                 });
                             }
                         }catch (Throwable t){
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                    });
                }
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> updateCashRegister(SQLConnection conn, Integer cashRegisterId, Integer cashOutStatus, Integer updatedBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
         try{
            JsonObject cashRegister = new JsonObject();
            cashRegister.put("id", cashRegisterId)
                    .put("cash_out_status", cashOutStatus)
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

    private void getDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer cashOutId = body.getInteger("cash_out_id");
        dbClient.queryWithParams(QUERY_CASH_OUT_DETAIL, new JsonArray()
                .add(cashOutId), queryReply -> {
            try{
                if(queryReply.failed()) {
                    throw new Exception(queryReply.cause());
                }
                List<JsonObject> results = queryReply.result().getRows();
                if (results.size() > 0) {
                    JsonObject cashOut = results.get(0);
                    Float difference = 0f;
                    String hasDifference = cashOut.getString("has_diference");

                    if (hasDifference.equals("1")) {
                        Float totalReported = cashOut.getFloat("total_reported");
                        Float totalOnRegister = cashOut.getFloat("total_on_register");
                        difference = totalReported - totalOnRegister;
                    }
                    cashOut.put("difference", difference);

                    List<CompletableFuture<JsonObject>> task = new ArrayList<>();

                    for(int j=0;j<SOME.size(); j++){
                        final String som = SOME.getString(j);
                        for(int i =0; i< ACTIONS.size(); i++){
                            final String action = ACTIONS.getString(i);
                            task.add(this.getGeneral(cashOut,cashOutId, SOME.getString(j)+"_id", ACTIONS.getString(i)));
                        }
                    }

                    CompletableFuture.allOf(task.toArray(new CompletableFuture[task.size()]))
                            .whenComplete((r,e)->{
                                try{
                                    if(e!=null){
                                        throw e;
                                    }else{
                                        message.reply(cashOut);
                                    }
                                }catch (Throwable t){
                                    t.printStackTrace();
                                    reportQueryError(message, t);
                                }
                            });

                } else {
                    reportQueryError(message, new Throwable("Cash out not found or not closed"));
                }

            }catch (Exception ex) {
                reportQueryError(message, ex);
            }
        });

    }

    private void getExtended(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer cashOutId = body.getInteger("cash_out_id");
        JsonArray rawPm = body.getJsonArray("payment_methods");
        final JsonArray paymentMethods = rawPm != null
                ? rawPm
                : new JsonArray();

        dbClient.query(QUERY_GET_PAYMENT_METHODS, pmReply -> {
            try {
                if (pmReply.failed()) {
                    throw pmReply.cause();
                }
                JsonArray paymentMethodsFromDB = new JsonArray(pmReply.result().getRows());
                JsonArray methodsToUse = !paymentMethods.isEmpty()
                        ? paymentMethods
                        : paymentMethodsFromDB;

                dbClient.queryWithParams(QUERY_GET_EXTENDED, new JsonArray().add(cashOutId), queryReply -> {
                    try {
                        if (queryReply.failed()) {
                            throw queryReply.cause();
                        }
                        List<JsonObject> results = queryReply.result().getRows();
                        if(results.isEmpty()) {
                            throw new Throwable("Cash out not found");
                        }
                        JsonObject cashOut = results.get(0);
                        Integer userId = cashOut.getInteger("created_by");
                        getTotalsExtended(getPaymentMethodsByOrigin(methodsToUse, cashOut), cashOutId, userId, cashOut).setHandler(totalsReply -> {
                            try {
                                if(totalsReply.failed()) {
                                    throw totalsReply.cause();
                                }
                                JsonObject totals = totalsReply.result();
                                cashOut.mergeIn(totals);
                                message.reply(cashOut);
                            } catch (Throwable t) {
                                reportQueryError(message, t);
                            }
                        });
                    } catch (Throwable ex) {
                        reportQueryError(message, ex);
                    }
                });
            }  catch (Throwable ex) {
                reportQueryError(message, ex);
            }
        });
    }

    private List<JsonObject> getPaymentMethodsByOrigin(JsonArray paymentMethods, JsonObject cashOut) {
        String cashOutOrigin = cashOut.getString("cash_out_origin");

        String origin = cashOutOrigin.equals("branchoffice") ? "pos" : "driver";
        return paymentMethods.stream()
                .map(pm -> (JsonObject)pm)
                .filter(pm -> pm.getString("allow_origin").contains(origin) || pm.getBoolean("is_cash"))
                .collect(Collectors.toList());
    }

    private Future<JsonObject> getTotalsExtended(List<JsonObject> methods, Integer cashOutId, Integer userId, JsonObject cashOut) {
        Future<JsonObject> future = Future.future();

        List<Future> futures = new ArrayList<>();
        futures.add(getTotalOnRegister(methods, cashOutId));
        futures.add(getTransactions(methods, cashOutId));
        futures.add(getTotalPurchase(methods, cashOutId, "boarding_pass"));
        futures.add(getTotalPurchase(methods, cashOutId, "prepaid"));
        futures.add(getTotalPurchase(methods, cashOutId, "parcel"));
        futures.add(getTotalPurchase(methods, cashOutId, "guiapp"));
        futures.add(getTotalPurchase(methods, cashOutId, "rental"));
        futures.add(getTotalPurchase(methods, cashOutId, "parcel_fxc"));
        futures.add(getTotalPurchase(methods, cashOutId, "all_boarding_pass_service"));
        futures.add(getTotalPurchase(methods, cashOutId, "all_parcel_service"));
        futures.add(getTotalOutcome(methods, cashOutId));
        futures.add(getTotalCancel(methods, cashOutId));
        futures.add(getActionExtended(methods, cashOutId, "income"));
        futures.add(getActionExtended(methods, cashOutId, "expense"));
        futures.add(getActionExtended(methods, cashOutId, "withdrawal"));
        futures.add(getIncomeByService(methods, cashOutId, "boarding_pass"));
        futures.add(getIncomeByService(methods, cashOutId, "parcel"));
        futures.add(getIncomeByService(methods, cashOutId, "rental"));

        futures.add(getCreditSalesAmount(userId, cashOut.getString("created_at"), cashOut.getString("updated_at"), "boarding_pass"));
        futures.add(getCreditSalesAmount(userId, cashOut.getString("created_at"), cashOut.getString("updated_at"), "prepaid"));
        futures.add(getCreditSalesAmount(userId, cashOut.getString("created_at"), cashOut.getString("updated_at"), "parcel"));
        futures.add(getCreditSalesAmount(userId, cashOut.getString("created_at"), cashOut.getString("updated_at"), "guiapp"));
        futures.add(getCreditSalesAmount(userId, cashOut.getString("created_at"), cashOut.getString("updated_at"), "parcel_fxc"));
        futures.add(getCreditSalesAmount(userId, cashOut.getString("created_at"), cashOut.getString("updated_at"), "rental"));

        CompositeFuture.all(futures).setHandler(reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                JsonObject totalOnRegister = reply.result().resultAt(0);
                JsonObject transactions = reply.result().resultAt(1);
                JsonObject totalPurchaseBoardingPass = reply.result().resultAt(2);
                JsonObject totalPurchasePrepaid = reply.result().resultAt(3);
                JsonObject totalPurchaseParcel = reply.result().resultAt(4);
                JsonObject totalPurchaseGuiaPP = reply.result().resultAt(5);
                JsonObject totalPurchaseRental = reply.result().resultAt(6);
                JsonObject totalPurchaseParcelFxC = reply.result().resultAt(7);
                JsonObject totalPurchaseAllBpService = reply.result().resultAt(8);
                JsonObject totalPurchaseAllParcelService = reply.result().resultAt(9);
                JsonObject totalOutcome = reply.result().resultAt(10);
                JsonObject totalCancel = reply.result().resultAt(11);
                JsonObject totalIncome = reply.result().resultAt(12);
                JsonObject totalExpense = reply.result().resultAt(13);
                JsonObject totalWithdrawal = reply.result().resultAt(14);
                JsonObject totalIncomeBoardingPass = reply.result().resultAt(15);
                JsonObject totalIncomeParcel = reply.result().resultAt(16);
                JsonObject totalIncomeRental = reply.result().resultAt(17);
                JsonObject totalCreditSalesBoardingPass = reply.result().resultAt(18);
                JsonObject totalCreditSalesPrepaid = reply.result().resultAt(19);
                JsonObject totalCreditSalesParcel = reply.result().resultAt(20);
                JsonObject totalCreditSalesGuiaPP = reply.result().resultAt(21);
                JsonObject totalCreditSalesParcelFxC = reply.result().resultAt(22);
                JsonObject totalCreditSalesRental = reply.result().resultAt(23);

                totalPurchaseBoardingPass.put("credit", totalCreditSalesBoardingPass.getDouble("credit"));
                totalPurchaseBoardingPass.put("total", totalPurchaseBoardingPass.getDouble("total") + totalCreditSalesBoardingPass.getDouble("credit"));
                totalPurchasePrepaid.put("credit", totalCreditSalesPrepaid.getDouble("credit"));
                totalPurchasePrepaid.put("total", totalPurchasePrepaid.getDouble("total") + totalPurchasePrepaid.getDouble("credit"));
                totalPurchaseParcel.put("credit", totalCreditSalesParcel.getDouble("credit"));
                totalPurchaseParcel.put("total", totalPurchaseParcel.getDouble("total") + totalPurchaseParcel.getDouble("credit"));
                totalPurchaseGuiaPP.put("credit", totalCreditSalesGuiaPP.getDouble("credit"));
                totalPurchaseGuiaPP.put("total", totalPurchaseGuiaPP.getDouble("total") + totalPurchaseGuiaPP.getDouble("credit"));
                totalPurchaseParcelFxC.put("credit", totalCreditSalesParcelFxC.getDouble("credit"));
                totalPurchaseParcelFxC.put("total", totalPurchaseParcelFxC.getDouble("total") + totalPurchaseParcelFxC.getDouble("credit"));
                totalPurchaseRental.put("credit", totalCreditSalesRental.getDouble("credit"));
                totalPurchaseRental.put("total", totalPurchaseRental.getDouble("total") + totalPurchaseRental.getDouble("credit"));
                totalPurchaseAllBpService.put("credit",
                        totalCreditSalesBoardingPass.getDouble("credit") + totalCreditSalesPrepaid.getDouble("credit"));
                totalPurchaseAllBpService.put("total", totalPurchaseAllBpService.getDouble("total") + totalPurchaseAllBpService.getDouble("credit"));
                totalPurchaseAllParcelService.put("credit",
                        totalCreditSalesParcel.getDouble("credit") +
                                totalCreditSalesGuiaPP.getDouble("credit") +
                                totalCreditSalesParcelFxC.getDouble("credit"));
                totalPurchaseAllParcelService.put("total", totalPurchaseAllParcelService.getDouble("total") + totalPurchaseAllParcelService.getDouble("credit"));
                totalOnRegister.fieldNames()
                        .stream()
                        .filter(key -> !key.contains("total"))
                        .forEach(key -> {
                            Double totalOut = totalCancel.getDouble(key ,0.0) + totalOutcome.getDouble(key, 0.0);
                            totalOnRegister.put(key, totalOnRegister.getDouble(key) - totalOut);
                        });

                future.complete(new JsonObject()
                        .put("total_on_register", totalOnRegister)
                        .put("total_purchase_boarding_pass", totalPurchaseBoardingPass)
                        .put("total_purchase_prepaid", totalPurchasePrepaid)
                        .put("total_purchase_parcel", totalPurchaseParcel)
                        .put("total_purchase_guiapp", totalPurchaseGuiaPP)
                        .put("total_purchase_rental", totalPurchaseRental)
                        .put("total_purchase_parcel_fxc", totalPurchaseParcelFxC)
                        .put("total_purchase_all_boarding_pass_service", totalPurchaseAllBpService)
                        .put("total_purchase_all_parcel_service", totalPurchaseAllParcelService)
                        .put("total_cancel", totalCancel)
                        .put("transactions", transactions)
                        .put("total_income", totalIncome)
                        .put("total_outcome", totalExpense)
                        .put("total_withdrawal", totalWithdrawal)
                        .put("total_income_boarding_pass", totalIncomeBoardingPass)
                        .put("total_income_parcel", totalIncomeParcel)
                        .put("total_income_rental", totalIncomeRental)
                );
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<JsonObject> getTotalCancel(List<JsonObject> methods, Integer cashOutId) {
        Future<JsonObject> future = Future.future();

        String QUERY = QUERY_GET_TOTAL_OUTCOME;

        JsonArray params = new JsonArray().add("cancel").add("return").add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT SUM(quantity)\n" +
                    "FROM cash_out co\n" +
                    "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                    "INNER JOIN expense e ON e.id = com.expense_id\n" +
                    "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
                    "WHERE co.id = ?\n" +
                    "AND (t.action = ? OR t.action = ?)\n" +
                    "AND com.move_type = '1'";

            String conditionals = voucherMethods.stream()
                    .map(m -> "e.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));

            QUERY += "AND (" + conditionals + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId)
                    .add("cancel").add("return");
        }

        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId)
                            .add("cancel").add("return");

                    String query = "";
                    query += "COALESCE((SELECT SUM(quantity)\n" +
                            "FROM cash_out co\n" +
                            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                            "INNER JOIN expense e ON e.id = com.expense_id\n" +
                            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
                            "WHERE co.id = ?\n" +
                            "AND (t.action = ? OR t.action = ?)\n";
                    String alias = method.getString("alias");
                    Integer id = method.getInteger("id");
                    query += "AND (e.payment_method_id = " + id + ")\n" +
                            "AND com.move_type = '1'), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";
        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getTotalOutcome(List<JsonObject> methods, Integer cashOutId) {
        Future<JsonObject> future = Future.future();

        String QUERY = QUERY_GET_TOTAL_OUTCOME;

        JsonArray params = new JsonArray().add("expense").add("withdrawal").add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT SUM(quantity)\n" +
                    "FROM cash_out co\n" +
                    "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                    "INNER JOIN expense e ON e.id = com.payment_id\n" +
                    "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
                    "WHERE co.id = ?\n" +
                    "AND (t.action = ? OR t.action = ?)\n" +
                    "AND com.move_type = '1'\n";

            String conditionals = voucherMethods.stream()
                    .map(m -> "e.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));

            QUERY += "AND (" + conditionals + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId)
                    .add("expense").add("withdrawal");
        }

        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId)
                            .add("expense").add("withdrawal");

                    String query = "";
                    query += "COALESCE((SELECT SUM(quantity)\n" +
                            "FROM cash_out co\n" +
                            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                            "INNER JOIN expense e ON e.id = com.payment_id\n" +
                            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
                            "WHERE co.id = ?\n" +
                            "AND (t.action = ? OR t.action = ?)\n";
                    String alias = method.getString("alias");
                    Integer id = method.getInteger("id");
                    query += "AND (e.payment_method_id = " + id + ")\n" +
                            "AND com.move_type = '1'), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";
        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getTotalPurchase(List<JsonObject> methods, Integer cashOutId, String serviceType) {
        Future<JsonObject> future = Future.future();
        String conditionalsServiceType = getAttributesForService(serviceType);
        String extraJoins = getExtraJoinsForService(serviceType);
        String QUERY = QUERY_GET_TOTALS;
        QUERY += extraJoins;
        QUERY += " WHERE (t.action = ? OR t.action = ?) AND t.cash_out_id = ?";
        QUERY += " AND (" + conditionalsServiceType + ")" + "), 0) total,\n";

        JsonArray params = new JsonArray().add("purchase").add("change").add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT SUM(quantity)\n" +
                    "FROM cash_out co\n" +
                    "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                    "INNER JOIN payment p ON p.id = com.payment_id\n" +
                    "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
                    extraJoins + "\n" +
                    "WHERE co.id = ?\n" +
                    "AND (t.action = ? OR t.action = ?)\n" +
                    "AND com.move_type = '0'\n";

            String conditionals = voucherMethods.stream()
                    .map(m -> "p.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));

            QUERY += "AND (" + conditionals + ")";
            QUERY += " AND (" + conditionalsServiceType + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId)
                    .add("purchase").add("change");
        }

        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId).add("purchase").add("change");

                    String query = "";
                    query += "COALESCE((SELECT SUM(quantity)\n" +
                            "FROM cash_out co\n" +
                            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                            "INNER JOIN payment p ON p.id = com.payment_id\n" +
                            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
                            extraJoins + "\n" +
                            "WHERE co.id = ?\n" +
                            "AND (t.action = ? OR t.action = ?)\n";
                    String alias = method.getString("alias");
                    Integer id = method.getInteger("id");
                    query += "AND (p.payment_method_id = " + id + ")\n" +
                            "AND (" + conditionalsServiceType + ")\n" +
                            "AND com.move_type = '0'), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";

        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getTransactions(List<JsonObject> methods, Integer cashOutId) {
        Future<JsonObject> future = Future.future();

        String QUERY = QUERY_GET_TRANSACTIONS;

        JsonArray params = new JsonArray()
                .add(cashOutId).add(cashOutId).add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT COUNT(quantity)\n" +
                    "FROM cash_out_move com\n" +
                    "LEFT JOIN payment p ON p.id = com.payment_id\n" +
                    "LEFT JOIN expense e ON e.id = com.expense_id\n" +
                    "WHERE com.cash_out_id = ?\n";

            String conditionals = voucherMethods.stream()
                    .map(m -> "p.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));
            String eConditionals = conditionals.replace("p.", "e.");

            QUERY += "AND (" + conditionals + " OR "  + eConditionals + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId);
        }

        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId);

                    String query = "";
                    query += "COALESCE((SELECT COUNT(quantity)\n" +
                            "FROM cash_out_move com\n" +
                            "LEFT JOIN payment p ON p.id = com.payment_id\n" +
                            "LEFT JOIN expense e ON e.id = com.expense_id\n" +
                            "WHERE com.cash_out_id = ?\n";
                    String alias = method.getString("alias");
                    Integer id = method.getInteger("id");
                    query += "AND (p.payment_method_id = '" + id.toString() + "' OR e.payment_method_id = '" + id.toString() + "')), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";
        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getTotalOnRegister(List<JsonObject> methods, Integer cashOutId) {
        Future<JsonObject> future = Future.future();

        String QUERY = QUERY_GET_TOTAL_ON_REGISTER;
        JsonArray params = new JsonArray()
                .add(cashOutId).add(cashOutId).add(cashOutId).add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT SUM(quantity)\n" +
                    "FROM cash_out co\n" +
                    "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                    "INNER JOIN payment p ON p.id = com.payment_id\n" +
                    "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
                    "WHERE co.id = ?\n" +
                    "AND com.move_type = '0'\n";

            String conditionals = voucherMethods.stream()
                    .map(m -> "p.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));

            QUERY += "AND (" + conditionals + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId);
        }


        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId);

                    String query = "";
                    query += "COALESCE((SELECT SUM(quantity)\n" +
                            "FROM cash_out co\n" +
                            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                            "INNER JOIN payment p ON p.id = com.payment_id\n" +
                            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
                            "WHERE co.id = ?\n";
                    String alias = method.getString("alias");
                    Integer id = method.getInteger(ID);
                    query += "AND (p.payment_method_id = '" + id.toString() + "')\n";
                    query += "AND com.move_type = '0'), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";
        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getActionExtended(List<JsonObject> methods, Integer cashOutId, String action) {
        Future<JsonObject> future = Future.future();
        String QUERY = QUERY_GET_TOTALS_BY_ACTION;

        JsonArray params = new JsonArray().add(action).add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT SUM(quantity)\n" +
                    "FROM cash_out co\n" +
                    "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                    "INNER JOIN payment p ON p.id = com.payment_id\n" +
                    "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
                    "WHERE co.id = ?\n" +
                    "AND (t.action = ?)\n" +
                    "AND com.move_type = '0'\n";

            String conditionals = voucherMethods.stream()
                    .map(m -> "p.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));

            QUERY += "AND (" + conditionals + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId).add(action);
        }


        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId).add(action);

                    String query = "";
                    query += "COALESCE((SELECT SUM(quantity)\n" +
                            "FROM cash_out co\n" +
                            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
                            "INNER JOIN payment p ON p.id = com.payment_id\n" +
                            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
                            "WHERE co.id = ?\n" +
                            "AND (t.action = ?)\n" +
                            "AND com.move_type = '0'\n";
                    String alias = method.getString("alias");
                    Integer id = method.getInteger(ID);
                    query += "AND (p.payment_method_id = '" + id.toString() + "')\n";
                    query += "), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";

        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getIncomeByService(List<JsonObject> methods, Integer cashOutId, String serviceType) {
        Future<JsonObject> future = Future.future();
        String QUERY = QUERY_GET_TOTALS_INCOME_BY_SERVICE;
        String conditionalsServiceType = getAttributesForIncomeByService(serviceType);
        QUERY += " AND (" + conditionalsServiceType + ")" + "), 0) total,\n";

        JsonArray params = new JsonArray().add(cashOutId);

        List<JsonObject> voucherMethods = getVouchersPaymentMethods(methods);

        if(!voucherMethods.isEmpty()) {
            QUERY += "COALESCE((SELECT SUM(dp.amount)\n" +
                    "FROM debt_payment dp\n" +
                    "LEFT JOIN payment p ON p.id = dp.payment_id\n" +
                    "WHERE dp.ticket_id IN (select id from tickets t WHERE t.action = 'income' and t.cash_out_id = ?)\n";

            String conditionals = voucherMethods.stream()
                    .map(m -> "p.payment_method_id = " + m.getInteger(ID).toString())
                    .collect(Collectors.joining(" OR "));

            QUERY += "AND (" + conditionals + ")";
            QUERY += " AND (" + conditionalsServiceType + ")";
            QUERY += "), 0) vouchers,";
            params.add(cashOutId);
        }


        String queryTotals = getNonVouchersPaymentMethods(methods).stream()
                .map(method -> {
                    params.add(cashOutId);

                    String query = "";
                    query += "COALESCE((SELECT SUM(dp.amount)\n" +
                            "FROM debt_payment dp\n" +
                            "LEFT JOIN payment p ON p.id = dp.payment_id\n" +
                            "WHERE dp.ticket_id IN (select id from tickets t WHERE t.action = 'income' and t.cash_out_id = ?)\n";

                    String alias = method.getString("alias");
                    Integer id = method.getInteger(ID);
                    query += "AND (p.payment_method_id = '" + id.toString() + "')\n" +
                    "AND (" + conditionalsServiceType + ")\n" +
                            "), 0) '" + alias + "'";
                    return query;
                })
                .collect(Collectors.joining(",\n"));

        QUERY += queryTotals + ";";

        return getExtendedQueryResult(future, QUERY, params);
    }

    private Future<JsonObject> getCreditSalesAmount(Integer userId, String dateInit, String dateEnd, String serviceType) {
        try {
            Future<JsonObject> future = Future.future();
            Date init = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(dateInit);
            Date end = new Date();
            if(dateEnd != null) {
                end = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(dateEnd);
            }
            String QUERY = getCreditSalesQueryByService(serviceType);
            JsonArray params = new JsonArray().add(userId).add(UtilsDate.sdfDataBase(init)).add(UtilsDate.sdfDataBase(end));
            return getExtendedQueryResult(future, QUERY, params);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    List<JsonObject> getNonVouchersPaymentMethods(List<JsonObject> methods) {
        return methods.stream()
                .filter(m -> !VOUCHERS.contains(m.getString("alias")))
                .collect(Collectors.toList());
    }

    List<JsonObject> getVouchersPaymentMethods(List<JsonObject> methods){
        return methods.stream()
                .filter(m -> VOUCHERS.contains(m.getString("alias")))
                .collect(Collectors.toList());
    }

    Future<JsonObject> getExtendedQueryResult(Future<JsonObject> future, String QUERY, JsonArray params) {
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> totals = reply.result().getRows();

                if(totals.isEmpty()) {
                    future.complete(new JsonObject());
                } else {
                    future.complete(totals.get(0));
                }
            }catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private String getAttributesForService(String serviceType) {
        String query = "";
        switch(serviceType) {
            case "boarding_pass":
                query = "p.boarding_pass_id IS NOT NULL";
                break;
            case "prepaid":
                query = "p.prepaid_travel_id IS NOT NULL";
                break;
            case "all_boarding_pass_service":
                query = "p.boarding_pass_id IS NOT NULL OR p.prepaid_travel_id IS NOT NULL";
                break;
            case "parcel":
                query = "p.parcel_id IS NOT NULL AND pa.pays_sender = 1";
                break;
            case "guiapp":
                query = "p.parcel_prepaid_id IS NOT NULL";
                break;
            case "all_parcel_service":
                query = "p.parcel_id IS NOT NULL OR p.parcel_prepaid_id IS NOT NULL";
                break;
            case "rental":
                query = "p.rental_id IS NOT NULL";
                break;
            case "parcel_fxc":
                query = "p.parcel_id IS NOT NULL AND pa.pays_sender = 0";
                break;
        }
        return query;
    }

    private String getAttributesForIncomeByService(String serviceType) {
        String query = "";
        switch(serviceType) {
            case "boarding_pass":
                query = "(dp.boarding_pass_id IS NOT NULL OR dp.prepaid_travel_id IS NOT NULL) AND dp.parcel_id IS NULL AND dp.parcel_prepaid_id IS NULL";
                break;
            case "parcel":
                query = "(dp.parcel_id IS NOT NULL OR dp.parcel_prepaid_id IS NOT NULL) AND dp.prepaid_travel_id IS NULL";
                break;
            case "rental":
                query = "dp.rental_id IS NOT NULL";
                break;
        }
        return query;
    }

    private String getCreditSalesQueryByService(String serviceType) {
        String query = "";
        switch(serviceType) {
            case "boarding_pass":
                query = QUERY_GET_BP_CREDIT_AMOUNT;
                break;
            case "prepaid":
                query = QUERY_GET_PREPAID_CREDIT_AMOUNT;
                break;
            case "parcel":
                query = QUERY_GET_PARCEL_CREDIT_AMOUNT;
                break;
            case "guiapp":
                query = QUERY_GET_GUIAPP_CREDIT_AMOUNT;
                break;
            case "parcel_fxc":
                query = QUERY_GET_PARCEL_FXC_CREDIT_AMOUNT;
                break;
            case "rental":
                query = QUERY_GET_RENTAL_CREDIT_AMOUNT;
                break;
        }
        return query;
    }

    private String getExtraJoinsForService(String serviceType) {
        String query = "";
        switch(serviceType) {
            case "parcel_fxc":
            case "parcel":
                query = " LEFT JOIN parcels pa on p.parcel_id = pa.id";
                break;
        }
        return query;
    }

    private CompletableFuture<List<JsonObject>> getTickets(Integer cashOutId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        dbClient.queryWithParams(QUERY_CASH_OUT_MOVES, new JsonArray()
                .add(cashOutId), queryReply -> {
            try{
                if(queryReply.failed()){
                    throw  new Exception(queryReply.cause());
                }
                List<JsonObject> cashMoves = queryReply.result().getRows();
                List<JsonObject> tickets = new ArrayList<>();
                int mLen = cashMoves.size();
                for (int i = 0; i < mLen; i++) {
                    JsonObject move = cashMoves.get(i);
                    JsonObject ticket = new JsonObject();
                    ticket.put("id", move.getInteger("ticket_id"))
                            .put("ticket_code", move.getString("ticket_code"))
                            .put("iva", move.getFloat("ticket_iva"))
                            .put("total", move.getFloat("ticket_total"))
                            .put("paid", move.getFloat("ticket_paid"))
                            .put("paid_change", move.getFloat("ticket_paid_change"));
                    tickets.add(ticket);
                }
                future.complete(tickets);

            }catch(Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getPayments(JsonObject ticket) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            Integer ticketId = ticket.getInteger("id");
            dbClient.queryWithParams(QUERY_PAYMENTS_FROM_TICKET, new JsonArray()
                .add(ticketId), queryReply -> {
            try{
                if(queryReply.failed()) {
                    throw new Exception(queryReply.cause());
                }
                List<JsonObject> paymentsMoves = queryReply.result().getRows();
                if (paymentsMoves.isEmpty()) {
                    ticket.put("payments", new ArrayList<>());
                } else {
                    JsonObject item = paymentsMoves.get(0);
                    if (item.getInteger("boarding_pass_id") != null) {
                        JsonObject boardingPass = new JsonObject().put("boarding_pass_id", item.getInteger("boarding_pass_id"))
                                .put("reservation_code", item.getString("reservation_code"))
                                .put("ticket_type", item.getString("ticket_type"))
                                .put("seatings", item.getInteger("seatings"));
                        ticket.put("boarding_pass", boardingPass);
                    } else if (item.getInteger("rental_id") != null) {
                        JsonObject rental = new JsonObject().put("rental_id", item.getInteger("rental_id"))
                                .put("reservation_code", item.getString("rental_reservation_code"));
                        ticket.put("rental", rental);
                    }
                    List<JsonObject> payments = new ArrayList<>();
                    int pLen = paymentsMoves.size();

                    for (int v = 0; v < pLen; v++) {
                        JsonObject paymentMove = paymentsMoves.get(v);
                        JsonObject payment = new JsonObject().put("id", paymentMove.getInteger("id"))
                                .put("payment_method", paymentMove.getString("payment_method"))
                                .put("icon", paymentMove.getString("icon"))
                                .put("reference", paymentMove.getString("reference"))
                                .put("exchange_rate_id", paymentMove.getInteger("exchange_rate_id"))
                                .put("currency", paymentMove.getString("currency"))
                                .put("currency_abr", paymentMove.getString("currency_abr"))
                                .put("payment_status", paymentMove.getInteger("payment_status"));
                        payments.add(payment);
                    }
                    ticket.put("payments", payments);
                }
                future.complete(ticket);


            }catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        return future;
    }

    private CompletableFuture<JsonObject> getGeneral(JsonObject cashOut, Integer cashOutId, String type, String action){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray();
            params.add(action);
            params.add(cashOutId);

            String query = "SELECT " +
                    "COUNT(tr."+type+") AS "+action+", " +
                    "sum(tr.total)as 'total_"+action+"' " +
                    "FROM cash_out AS co " +
                    "LEFT JOIN tickets AS tr ON tr.cash_out_id = co.id AND tr.action = ? AND tr."+type+" is not null " +
                    "WHERE co.cash_out_status = 2 AND co.id = ? " +
                    "group by co.id;";

            dbClient.queryWithParams(query, params, handler->{
                try{
                    if(handler.failed()) {
                        throw new Exception(handler.cause());
                    }
                    JsonObject result = handler.result().getRows().get(0);
                    final String name = type.replace("_id", "")+"_"+action;

                    this.getTickets2(cashOutId, type, action).whenComplete((List<JsonObject> res, Throwable tderr)->{
                    try{
                        if(tderr!=null){
                            throw tderr;
                        }else{
                            int mLen = res.size();
                            if (mLen > 0) {
                                List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                                for (JsonObject moveTicket : res) {
                                    tasks.add(getPayments(moveTicket));
                                }
                                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[res.size()]))
                                        .whenComplete((s, t) -> {
                                        try{
                                            if (t != null) {
                                                throw t;
                                            } else {
                                                List<CompletableFuture<JsonObject>> ticketsDetails = new ArrayList<>();
                                                for (JsonObject moveTicket : res) {
                                                    ticketsDetails.add(getTicketDetails(moveTicket));
                                                }

                                                CompletableFuture.allOf(ticketsDetails.toArray(new CompletableFuture[res.size()]))
                                                        .whenComplete((ss, tt) -> {
                                                            try{
                                                                if (tt != null) {
                                                                    throw tt;
                                                                } else {
                                                                    result.put("tickets", res);
                                                                    cashOut.put(name, result);
                                                                    future.complete(cashOut);
                                                                }
                                                             } catch (Throwable tr){
                                                                tr.printStackTrace();
                                                                future.completeExceptionally(tr);
                                                            }

                                                        });
                                            }
                                        } catch (Throwable e){
                                            e.printStackTrace();
                                            future.completeExceptionally(e);
                                        }
                                        });
                            } else {
                                cashOut.put(name, result);
                                future.complete(cashOut);
                            }

                        }
                    }catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                    });

                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
         } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
         }

        
        return future;
    }
    
    private CompletableFuture<List<JsonObject>> getTickets2(Integer cashOutId, String type, String action) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String Query = "SELECT " +
                    "	p.ticket_id, t.action, t.ticket_code, t.iva AS ticket_iva, t.total AS ticket_total, t.paid AS ticket_paid, t.paid_change AS ticket_paid_change " +
                    "	FROM cash_out_move AS com " +
                    "	LEFT JOIN payment AS p ON p.id = com.payment_id " +
                    "	inner JOIN tickets AS t ON t.cash_out_id = com.cash_out_id and t." + type + " IS NOT NULL AND t.action = ? " +
                    "	WHERE com.cash_out_id = ? AND com.status = 1 AND p.ticket_id IS NOT NULL " +
                    "	GROUP BY t.id";
            JsonArray params = new JsonArray();
            params.add(action);
            params.add(cashOutId);

            dbClient.queryWithParams(Query, params, queryReply -> {
                try {
                    if (queryReply.failed()) {
                        throw new Exception(queryReply.cause());
                    }
                    List<JsonObject> cashMoves = queryReply.result().getRows();
                    List<JsonObject> tickets = new ArrayList<>();
                    int mLen = cashMoves.size();
                    for (int i = 0; i < mLen; i++) {
                        JsonObject move = cashMoves.get(i);
                        JsonObject ticket = new JsonObject();
                        ticket.put("id", move.getInteger("ticket_id"))
                                .put("ticket_code", move.getString("ticket_code"))
                                .put("iva", move.getFloat("ticket_iva"))
                                .put("total", move.getFloat("ticket_total"))
                                .put("paid", move.getFloat("ticket_paid"))
                                .put("paid_change", move.getFloat("ticket_paid_change"));
                        tickets.add(ticket);
                    }
                    future.complete(tickets);


                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });

        }catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }


        return future;
    }
    
    private CompletableFuture<JsonObject> getTicketDetails(JsonObject ticket) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
                Integer ticketId = ticket.getInteger("id");
                dbClient.queryWithParams(QUERY_TICKET_DETAILS, new JsonArray()
                        .add(ticketId), queryReply -> {
                    try{
                        if(queryReply.failed()) {
                            throw new Exception(queryReply.cause());
                        }
                        List<JsonObject> details = queryReply.result().getRows();
                        if (details.isEmpty()) {
                            ticket.put("details", new ArrayList<>());
                        } else {
                            ticket.put("details", details);
                        }

                        future.complete(ticket);
                    }catch (Exception ex) {
                        ex.printStackTrace();
                        future.completeExceptionally(ex);
                    }
                });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> isValidBranchOffice(SQLConnection conn, Integer schedule_route_id, Integer branchoffice_id){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            JsonArray params = new JsonArray().add(schedule_route_id);
            conn.queryWithParams(QUERY_GET_DESTINATIONS,params, reply->{
                try{
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    boolean res = false;
                    if(reply.result().getNumRows()>0){
                        for (int i=0; i<reply.result().getNumRows();i++){
                            if(reply.result().getRows().get(i).getInteger("terminal_destiny_id").equals(branchoffice_id)){
                                res = true;
                            }
                        }
                        future.complete(res);
                    }else{
                        future.completeExceptionally(new Throwable("DESTINATIONS NOT FOUND"));
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });

        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }

        return future;
    }

    /**
     * Verifica si tiene diferencias entre total_reported y total_on_register
     * <br>
     * (0): No hay diferencias
     * <br>
     * (1): Hay menos dinero de lo que debería
     * <br>
     * (2): Hay más dinero de lo que debería
     *
     * @param totalReported cantidad de dinero fisica
     * @param totalOnRegister cantidad de dinero registrada en sistema
     * @return String con 0, 1, ó 2, según sea el caso
     */
    private String hasDiference(float totalReported, float totalOnRegister) {
        if (totalReported == totalOnRegister) {
            return "0";
        } else if (totalReported > totalOnRegister) {
            return "1";
        } else {
            return "2";
        }
    }

    private void checkCashOutEmployee(Message<JsonObject> message){

        Integer userId = message.body().getInteger("user_id");
        this.dbClient.queryWithParams(QUERY_CASH_OUT_EMPLOYEE, new JsonArray().add(userId), queryReply -> {
            try{
                if (queryReply.succeeded()) {
                    List<JsonObject> res = queryReply.result().getRows();
                    if (res.isEmpty()) {
                        reportQueryError(message, new Throwable("Employee needs to have an opened cash out"));
                    } else {
                        message.reply(res.get(0));
                    }
                } else {
                    reportQueryError(message, queryReply.cause());
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });

    }

    private void getOpenedCashOut(Message<JsonObject> message){
        Integer employeeId = message.body().getInteger(EMPLOYEE_ID);
        this.dbClient.queryWithParams(QUERY_GET_OPENED_CASH_OUT_BY_DRIVER, new JsonArray().add(employeeId), queryReply -> {
            try{
                if (queryReply.failed()){
                    throw queryReply.cause();
                }
                List<JsonObject> res = queryReply.result().getRows();
                if (res.isEmpty()) {
                    message.reply(null);
                } else {
                    message.reply(res.get(0).getInteger(ID));
                }
            } catch(Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void getCashOutEmployee(Message<JsonObject> message){
        String userId = message.body().getString("reservation_code");
        this.dbClient.queryWithParams(QUERY_GET_CASH_OUT_ORIGIN, new JsonArray().add(userId), queryReply -> {
            try{
                if (queryReply.succeeded()) {
                    List<JsonObject> res = queryReply.result().getRows();
                    if (res.isEmpty()) {
                        reportQueryError(message, new Throwable("Cash out origin is closed"));
                    } else {
                        message.reply(res.get(0));
                    }
                } else {
                    reportQueryError(message, queryReply.cause());
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });
     }

    private void getCashOutEmployeeById(Message<JsonObject> message){
        Integer userId = message.body().getInteger(USER_ID);
        this.dbClient.queryWithParams(QUERY_CASH_OUT_EMPLOYEE, new JsonArray().add(userId), queryReply -> {
            try{
                if (queryReply.succeeded()) {
                    List<JsonObject> res = queryReply.result().getRows();
                    if (res.isEmpty()) {
                        message.reply(new JsonObject());
                    } else {
                        message.reply(res.get(0));
                    }
                } else {
                    reportQueryError(message, queryReply.cause());
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });
    }

    private void getCashOutWithoutEmployee(Message<JsonObject> message){
        Integer userId = message.body().getInteger("user_id");
        this.dbClient.queryWithParams(QUERY_CASH_OUT_OPEN_WITHOUT_EMPLOYEE, new JsonArray().add(userId), queryReply -> {
            try{
                if (queryReply.succeeded()) {
                    List<JsonObject> res = queryReply.result().getRows();
                    if (res.isEmpty()) {
                        message.reply(new JsonObject());
                    } else {
                        message.reply(res.get(0));
                    }
                } else {
                    reportQueryError(message, queryReply.cause());
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });
    }

    private CompletableFuture<JsonObject> getCashRegister(Integer cashRegisterId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            dbClient.queryWithParams(QUERY_GET_CASH_REGISTER, new JsonArray().add(cashRegisterId), queryReply -> {
                try{
                    if(queryReply.failed()) {
                        throw new Exception(queryReply.cause());
                    }
                    List<JsonObject> cashRegister = queryReply.result().getRows();
                    JsonObject result = cashRegister.get(0);
                    if (cashRegister.size() < 1) {
                        throw new Exception("Cash register not found");
                    }

                    future.complete(result);
                }catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private void handleResumeSales(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer cashOutId = body.getInteger("cash_out_id");
        JsonArray paymentMethods = body.getJsonArray("payment_methods");
        dbClient.queryWithParams(QUERY_GET_BASIC_INFO, new JsonArray().add(cashOutId), queryReply -> {
            try {
                if (queryReply.failed()) {
                    throw queryReply.cause();
                }
                List<JsonObject> results = queryReply.result().getRows();
                if(results.isEmpty()) {
                    throw new Throwable("Cash out not found");
                }
                JsonObject cashOut = results.get(0);
                Integer userId = cashOut.getInteger("created_by");
                getResumeSales(userId, cashOut).setHandler(totalsReply -> {
                    try {
                        if(totalsReply.failed()) {
                            throw totalsReply.cause();
                        }
                        JsonObject totals = totalsReply.result();
                        cashOut.mergeIn(totals);
                        message.reply(cashOut);
                    } catch (Throwable t) {
                        reportQueryError(message, t);
                    }
                });
            } catch (Throwable ex) {
                reportQueryError(message, ex);
            }
        });
    }

    private Future<JsonObject> getResumeSales(Integer userId, JsonObject cashOut) {
        Future<JsonObject> future = Future.future();

        List<Future> futures = new ArrayList<>();
        futures.add(getSalesResumeParcel(cashOut.getInteger("id"), 1)); // envio pagado
        futures.add(getSalesResumeParcel(cashOut.getInteger("id"), 0)); // fxc
        futures.add(getDebtPaymentsResume(cashOut.getInteger("id"), "parcel"));
        futures.add(getDebtPaymentsResume(cashOut.getInteger("id"), "guiapp"));
        futures.add(getSalesCounts(cashOut, userId, "parcel"));

        CompositeFuture.all(futures).setHandler(reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                JsonArray parcelSalesResume = reply.result().resultAt(0);
                JsonArray parcelFxCSalesResume = reply.result().resultAt(1);
                JsonArray parcelDebtPayments = reply.result().resultAt(2);
                JsonArray guiappDebtPayments = reply.result().resultAt(3);
                JsonObject salesCountParcel = reply.result().resultAt(4);
                JsonArray allParcelDebtPayments = new JsonArray();
                for (Object element : parcelDebtPayments) {
                    allParcelDebtPayments.add(element);
                }
                for (Object element : guiappDebtPayments) {
                    allParcelDebtPayments.add(element);
                }

                future.complete(new JsonObject()
                        .put("parcel_sales_resume", parcelSalesResume)
                        .put("total_parcel_sales", getTotalAmount(parcelSalesResume))
                        .put("parcel_fxc_sales_resume", parcelFxCSalesResume)
                        .put("total_parcel_fxc_sales", getTotalAmount(parcelFxCSalesResume))
                        .put("all_parcel_debt_payments", allParcelDebtPayments)
                        .put("total_parcel_debt_payments", getTotalAmount(allParcelDebtPayments))
                        .put("sales_count_parcel", salesCountParcel)
                );
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Double getTotalAmount(JsonArray arr) {
        double totalAmount = 0.0;
        for (Object obj : arr) {
            if (obj instanceof JsonObject) {
                JsonObject jsonObject = (JsonObject) obj;
                if (jsonObject.containsKey("amount")) {
                    double amount = jsonObject.getDouble("amount", 0.0);
                    totalAmount += amount;
                }
            }
        }
        return totalAmount;
    }

    private Future<JsonArray> getDebtPaymentsResume(Integer cashoutId, String serviceType) {
        Future<JsonArray> future = Future.future();
        String QUERY = getDebtPaymentsQueryByService(serviceType);
        JsonArray params = new JsonArray().add(cashoutId);
        this.dbClient.queryWithParams(QUERY, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> totals = reply.result().getRows();

                if(totals.isEmpty()) {
                    future.complete(new JsonArray());
                } else {
                    future.complete(new JsonArray(totals));
                }
            }catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<JsonArray> getSalesResumeParcel(Integer cashoutId, Integer paySender ) {
        Future<JsonArray> future = Future.future();
        JsonArray params = new JsonArray().add(cashoutId).add(paySender);
        this.dbClient.queryWithParams(QUERY_GET_PURCHASE_RESUME_PARCEL, params, reply -> {
            try {
                if(reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> totals = reply.result().getRows();

                if(totals.isEmpty()) {
                    future.complete(new JsonArray());
                } else {
                    future.complete(new JsonArray(totals));
                }
            }catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private String getDebtPaymentsQueryByService(String serviceType) {
        String query = "";
        switch(serviceType) {
            case "parcel":
                query = QUERY_GET_PARCEL_INFO_FROM_DEBT_PAYMENTS;
                break;
            case "guiapp":
                query = QUERY_GET_GUIAPP_INFO_FROM_DEBT_PAYMENTS;
                break;
        }
        return query;
    }

    private Future<JsonObject> getSalesCounts(JsonObject cashOut, Integer userId, String serviceType) {
        Future<JsonObject> future = Future.future();
        try {
            String query = "";
            JsonArray params = new JsonArray();
            switch(serviceType) {
                case "parcel":
                    query = QUERY_GET_SALES_COUNT_PARCEL;
                    Date init = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(cashOut.getString("created_at"));
                    Date end = new Date();
                    if(cashOut.getString("updated_at") != null) {
                        end = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(cashOut.getString("updated_at"));
                    }
                    params.add(userId).add(UtilsDate.sdfDataBase(init)).add(UtilsDate.sdfDataBase(end));
                    break;
            }
            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }

                    List<JsonObject> totals = reply.result().getRows();

                    if(totals.isEmpty()) {
                        future.complete(new JsonObject());
                    } else {
                        future.complete(totals.get(0));
                    }
                }catch (Throwable t) {
                    future.fail(t);
                }
            });
            return future;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void getReport(Message<JsonObject> message) {
        JsonObject body = message.body();
        String initDate = body.getString(_INIT_DATE);
        String endDate = body.getString(_END_DATE);
        Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
        Integer createdBy = body.getInteger(CREATED_BY);

        String QUERY = QUERY_GET_REPORT;
        JsonArray params = new JsonArray().add(initDate).add(endDate);
        if (Objects.nonNull(branchofficeId)) {
            params.add(branchofficeId);
            QUERY += " AND b.id = ? \n";
        }
        if (Objects.nonNull(createdBy)) {
            params.add(createdBy);
            QUERY += " AND co.created_by = ? \n";
        }

        QUERY += "ORDER BY co.id;";

        dbClient.queryWithParams(QUERY, params, queryReply -> {
            try {
                if (queryReply.failed()) {
                    throw queryReply.cause();
                }
                List<JsonObject> results = queryReply.result().getRows();
                message.reply(new JsonArray(results));
            } catch (Throwable ex) {
                reportQueryError(message, ex);
            }
        });
    }

    private static final String QUERY_GET_CASH_REGISTER = "SELECT id, cash_register\n" +
            "FROM cash_registers where id = ?;";

    private static final String QUERY_CASH_OUT_EMPLOYEE = "SELECT\n" +
            "   co.id,\n" +
            "   co.cash_register_id\n" +
            " FROM cash_out co\n" +
            " LEFT JOIN employee e ON e.id = co.employee_id\n" +
            " WHERE\n" +
            "   e.user_id = ? \n" +
            "   AND co.cash_out_status = 1;";

    private static final String QUERY_GET_CASH_OUT_ORIGIN = "SELECT\n" +
            "  co.id,\n" +
            "  co.cash_register_id\n" +
            "FROM cash_out co\n" +
            "INNER JOIN tickets t ON t.cash_out_id = co.id\n" +
            "INNER JOIN boarding_pass bp on bp.id = t.boarding_pass_id\n" +
            "WHERE\n" +
            "  bp.reservation_code = ? \n" +
            "  AND co.cash_out_status = 1\n" +
            "  ORDER BY t.created_at\n" +
            "  LIMIT 1;";

    private static final String QUERY_CASH_OUT_OPEN_WITHOUT_EMPLOYEE = "SELECT\n" +
            "co.id,\n" +
            "co.cash_register_id\n" +
            "FROM cash_out co\n" +
            "LEFT JOIN employee e ON e.branchoffice_id = co.branchoffice_id\n" +
            "WHERE\n" +
            "e.user_id = ?\n" +
            "AND co.cash_out_status = 1;";

    private static final String QUERY_CASH_OUT_SUM_QUANTITY = "SELECT \n"
            + "COALESCE((SELECT sum(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '0'), 0) - \n"
            + "COALESCE((SELECT sum(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '1'), 0) AS total_on_register, \n"
            + "(SELECT initial_fund FROM cash_out WHERE id = ?) AS initial_fund;";

    private static final String QUERY_CASH_OUT_DETAIL = "SELECT co.id, \n"
            + "co.branchoffice_id, b.name AS branchoffice, \n"
            + "co.employee_id, CONCAT(e.name, ' ', e.last_name) AS employee, \n"
            + "co.initial_fund, co.final_fund, co.cash, co.vouchers, co.total_reported, co.total_on_register, \n"
            + "co.has_diference, co.notes, \n"
            + "co.cash_register_id, cr.cash_register, \n"
            + "co.created_at,  co.updated_at \n"
            + "FROM cash_out AS co \n"
            + "LEFT JOIN branchoffice AS b ON b.id = co.branchoffice_id \n"
            + "LEFT JOIN employee AS e ON e.id = co.employee_id \n"
            + "LEFT JOIN cash_registers AS cr ON cr.id = co.cash_register_id \n"
            + "WHERE co.cash_out_status = 2 AND co.id = ?;";

    private static final String QUERY_CASH_OUT_MOVES = "SELECT \n"
            + "p.ticket_id, t.ticket_code, t.iva AS ticket_iva, t.total AS ticket_total, t.paid AS ticket_paid, t.paid_change AS ticket_paid_change, \n"
            + "COUNT(p.boarding_pass_id) AS boarding_passes, COUNT(p.rental_id) AS rentals, COUNT(p.parcel_id) AS parcels \n"
            + "FROM cash_out_move AS com \n"
            + "LEFT JOIN payment AS p ON p.id = com.payment_id \n"
            + "LEFT JOIN tickets AS t ON t.id = p.ticket_id \n"
            + "WHERE com.cash_out_id = ? AND com.status = 1 AND p.ticket_id IS NOT NULL \n"
            + "GROUP BY p.ticket_id;";

    private static final String QUERY_PAYMENTS_FROM_TICKET = "SELECT p.id, p.payment_method_id, pm.name AS payment_method, pm.icon, p.amount, p.reference, p.exchange_rate_id, \n"
            + "p.currency_id, c.name AS currency, c.abr AS currency_abr, p.payment_status, \n"
            + "p.boarding_pass_id, bp.reservation_code, bp.ticket_type, bp.seatings, \n"
            + "p.rental_id, r.reservation_code AS rental_reservation_code \n"
            + "FROM payment AS p \n"
            + "LEFT JOIN boarding_pass AS bp ON bp.id = p.boarding_pass_id \n"
            + "LEFT JOIN rental AS r ON r.id = p.rental_id \n"
            + "LEFT JOIN payment_method AS pm ON pm.id = p.payment_method_id \n"
            + "LEFT JOIN currency AS c ON c.id = p.currency_id \n"
            + "WHERE p.ticket_id = ?;";

    private static final String QUERY_GET_COUNT_TOTAL = "SELECT " +
"COUNT(tr.rental_id) AS 'purchase', " +
"sum(tr.total)as 'total_purchase' " +
"FROM cash_out AS co " +
"LEFT JOIN tickets AS tr ON tr.cash_out_id = co.id AND tr.action= ? AND tr.rental_id is not null " +
"WHERE co.cash_out_status = 2 AND co.id = ? " +
"group by co.id;";
    
    private static final String QUERY_TICKET_GENERAL = "SELECT * FROM tickets where cash_out_id = ? and ? is not null and action = ?;";
    
    private static final String QUERY_TICKET_DETAILS = "SELECT quantity, detail, unit_price, amount FROM tickets_details WHERE ticket_id = ?;";

    private static final String QUERY_GET_DESTINATIONS = "SELECT terminal_destiny_id, terminal_origin_id FROM schedule_route_destination where schedule_route_id = ?";

    private static final String QUERY_GET_OPENED_CASH_OUT_BY_DRIVER = "SELECT id FROM cash_out WHERE cash_out_status = 1 AND cash_out_origin = 'driver' AND employee_id = ?;";

    private static final String QUERY_GET_OPENED_CASH_OUT = "SELECT id FROM cash_out WHERE cash_out_status = 1 AND employee_id = ?;";

    private static final String QUERY_GET_LAST_TICKET_CASH_REGISTER = "SELECT last_ticket FROM cash_registers WHERE id = ?;";
    
    private static final String QUERY_GET_CASH_REGISTER_BY_TERMINAL_ID = "SELECT cr.id, cr.cash_register FROM cash_registers AS cr\n" +
            "   LEFT JOIN cash_out AS co ON co.cash_register_id = cr.id\n" +
            "WHERE cr.branchoffice_id = ? AND cr.status = 1 AND co.updated_at BETWEEN ? AND ? AND co.cash_out_origin != 'driver' GROUP BY cr.id;";

    private static final String QUERY_GET_CASH_REGISTER_DETAIL = "SELECT CONCAT(e.name , ' ', e.last_name) AS name_employee, co.created_at AS open_at," +
            "    co.updated_at AS close_at, co.initial_fund AS starting_amount,co.final_fund AS final_amount," +
            "    co.total_reported AS total_report,(co.total_on_register - co.total_reported) AS diference ,co.init_ticket, co.last_ticket," +
            "    {PM},co.vouchers, co.total_on_register, co.id, co.cash_out_origin, co.notes\n" +
            "    FROM cash_out AS co\n" +
            "    INNER JOIN employee AS e ON e.id = co.employee_id\n" +
            "    WHERE co.cash_register_id = ? AND co.updated_at BETWEEN ? AND ? AND co.cash_out_origin != 'driver' AND co.cash_out_status = 2 GROUP BY co.id";

    private static final String QUERY_GET_TOTAL_INCOME = "SELECT \n" +
            "COALESCE((SELECT SUM(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '0'), 0) total,\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (p.payment_method = 'card' OR p.payment_method = 'debit')\n" +
            "AND com.move_type = '0'), 0) vouchers,\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND p.payment_method = 'check'\n" +
            "AND com.move_type = '0'), 0) AS 'check',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND p.payment_method = 'transfer'\n" +
            "AND com.move_type = '0'), 0) AS 'transfer',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND p.payment_method = 'deposit'\n" +
            "AND com.move_type = '0'), 0) AS 'deposit',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (p.payment_method = 'cash')\n" +
            "AND com.move_type = '0'), 0) cash;";

    private static final String QUERY_GET_TOTAL_OUTCOME= "SELECT \n" +
            "coalesce((select sum(total) from tickets where (action = ? OR action = ?) and cash_out_id = ?), 0) total,\n";

    private static final String QUERY_GET_TOTALS = "SELECT \n" +
            "coalesce((select sum(t.total) from tickets t INNER JOIN payment p on p.ticket_id = t.id";
    private static final String QUERY_GET_TOTALS_BY_ACTION = "SELECT \n" +
            "coalesce((select sum(total) from tickets where action = ? and cash_out_id = ?), 0) total,\n";
    private static final String QUERY_GET_TOTALS_INCOME_BY_SERVICE = "SELECT \n" +
            "coalesce((select SUM(dp.amount) from debt_payment dp\n" +
            "WHERE dp.ticket_id IN (\n" +
            "SELECT id FROM tickets t WHERE t.action = 'income' and t.cash_out_id = ?)";
    private static final String QUERY_GET_BP_CREDIT_AMOUNT = "SELECT\n" +
            "COALESCE(sum(bp.total_amount), 0) as credit\n" +
            "FROM boarding_pass bp\n" +
            "WHERE bp.payment_condition = 'credit'\n" +
            "AND bp.created_by = ? AND bp.created_at BETWEEN ? AND ?";
    private static final String QUERY_GET_PREPAID_CREDIT_AMOUNT = "SELECT\n" +
            "COALESCE(sum(ppt.total_amount), 0) as credit\n" +
            "FROM prepaid_package_travel ppt\n" +
            "WHERE ppt.payment_condition = 'credit'\n" +
            "AND ppt.created_by = ? AND ppt.created_at BETWEEN ? AND ?";
    private static final String QUERY_GET_PARCEL_CREDIT_AMOUNT = "SELECT\n" +
            "COALESCE(sum(p.total_amount), 0) as credit\n" +
            "FROM parcels p\n" +
            "WHERE p.payment_condition = 'credit'\n" +
            "AND p.created_by = ? AND pays_sender = 1 AND p.created_at BETWEEN ? AND ?";
    private static final String QUERY_GET_GUIAPP_CREDIT_AMOUNT = "SELECT\n" +
            "COALESCE(sum(pp.total_amount), 0) as credit\n" +
            "FROM parcels_prepaid pp\n" +
            "WHERE pp.payment_condition = 'credit'\n" +
            "AND pp.crated_by = ? AND pp.created_at BETWEEN ? AND ?";
    private static final String QUERY_GET_PARCEL_FXC_CREDIT_AMOUNT = "SELECT\n" +
            "COALESCE(sum(p.total_amount), 0) as credit\n" +
            "FROM parcels p\n" +
            "WHERE p.payment_condition = 'credit'\n" +
            "AND p.created_by = ? AND pays_sender = 0 AND p.created_at BETWEEN ? AND ?";
    private static final String QUERY_GET_RENTAL_CREDIT_AMOUNT = "SELECT\n" +
            "COALESCE(sum(r.total_amount), 0) as credit\n" +
            "FROM rental r\n" +
            "WHERE r.payment_condition = 'credit'\n" +
            "AND r.created_by = ? AND r.created_at BETWEEN ? AND ?";
    private static final String QUERY_GET_TOTALS_Z = "SELECT \n" +
            "coalesce((select sum(total) from tickets where action = ? and cash_out_id = ?), 0) total,\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND (p.payment_method = 'card' OR p.payment_method = 'debit')\n" +
            "AND com.move_type = '0'), 0) vouchers,\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND p.payment_method = 'check'\n" +
            "AND com.move_type = '0'), 0) 'check',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND p.payment_method = 'transfer'\n" +
            "AND com.move_type = '0'), 0) 'transfer',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND p.payment_method = 'deposit'\n" +
            "AND com.move_type = '0'), 0) 'deposit',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND p.payment_method = 'cash'\n" +
            "AND com.move_type = '0'), 0) 'cash';\n";

    private static final String QUERY_GET_TOTAL_ON_REGISTER = "SELECT \n" +
            "COALESCE((SELECT SUM(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '0'), 0) total_income,\n" +
            "COALESCE((SELECT SUM(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '1'), 0) total_outcome,\n" +
            "COALESCE((SELECT SUM(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '0'), 0) - \n" +
            "COALESCE((SELECT SUM(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '1'), 0) total,\n";

    private static final String QUERY_GET_TOTAL_CANCEL = "SELECT \n" +
            "coalesce((select sum(total) from tickets where action = ? and cash_out_id = ?), 0) total,\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN expense e ON e.id = com.expense_id\n" +
            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND e.payment_method_id = 2 OR e.payment_method_id = 3\n" +
            "AND com.move_type = '1'), 0) vouchers,\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN expense e ON e.id = com.expense_id\n" +
            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND e.payment_method_id = 4\n" +
            "AND com.move_type = '1'), 0) 'check',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN expense e ON e.id = com.expense_id\n" +
            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND e.payment_method_id = 5\n" +
            "AND com.move_type = '1'), 0) 'transfer',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN expense e ON e.id = com.expense_id\n" +
            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND e.payment_method_id = 6\n" +
            "AND com.move_type = '1'), 0) 'deposit',\n" +
            "COALESCE((SELECT SUM(quantity)\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN expense e ON e.id = com.expense_id\n" +
            "INNER JOIN tickets t ON t.id = e.ticket_id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = ?)\n" +
            "AND e.payment_method_id = 1\n" +
            "AND com.move_type = '1'), 0) 'cash';";

    private static final String QUERY_GET_SERVICES = "SELECT \n" +
            "    COUNT(DISTINCT CASE WHEN pa.pays_sender = 1 THEN p.parcel_id END) parcel_sells_count,\n" +
            "    SUM(IF(p.parcel_id IS NOT NULL AND pa.pays_sender = 1, com.quantity, 0)) parcel_sells_total,\n" +
            "    COUNT(DISTINCT CASE WHEN pa.pays_sender = 0 THEN p.parcel_id END) parcel_charge_count,\n" +
            "    SUM(IF(p.parcel_id IS NOT NULL AND pa.pays_sender = 0, com.quantity, 0)) parcel_charge_total,\n" +
            "    COUNT(DISTINCT p.boarding_pass_id) boarding_pass_count,\n" +
            "    SUM(IF(p.boarding_pass_id IS NOT NULL, com.quantity, 0)) boarding_pass_total,\n" +
            "    COUNT(DISTINCT p.rental_id) rental_count,\n" +
            "    SUM(IF(p.rental_id IS NOT NULL, com.quantity, 0)) rental_total\n" +
            "FROM cash_out_move com\n" +
            "    INNER JOIN cash_out co ON co.id = com.cash_out_id\n" +
            "    INNER JOIN payment p ON p.id = com.payment_id\n" +
            "    LEFT JOIN parcels pa ON pa.id = p.parcel_id\n" +
            "WHERE com.move_type='0' AND co.branchoffice_id = ? AND co.updated_at BETWEEN ? AND ? AND co.cash_out_origin != 'driver';";

    private static final String QUERY_GET_SERVICE_DETAIL_EXPENSE =  "SELECT \n" +
            "    COUNT(e.parcel_id) parcel_count,\n" +
            "    SUM(IF(e.parcel_id IS NOT NULL, com.quantity, 0)) parcel_total,\n" +
            "    COUNT(e.boarding_pass_id) boarding_pass_count,\n" +
            "    SUM(IF(e.boarding_pass_id IS NOT NULL, com.quantity, 0)) boarding_pass_total,\n" +
            "    COUNT(e.rental_id) rental_count,\n" +
            "    SUM(IF(e.rental_id IS NOT NULL, com.quantity, 0)) rental_total\n" +
            "FROM cash_out_move com\n" +
            "    INNER JOIN cash_out co ON co.id = com.cash_out_id\n" +
            "    INNER JOIN expense e ON e.id = com.expense_id\n" +
            "WHERE com.move_type='1' AND co.branchoffice_id = ? AND co.updated_at BETWEEN ? AND ? AND co.cash_out_origin != 'driver';";

    private static final String QUERY_GET_SELL_AND_CHARGE_DETAIL = "SELECT \n" +
            "    SUM(IF(com.move_type = '0' AND p.parcel_id IS NOT NULL AND pa.pays_sender = 1 , com.quantity,0.0)) AS totals_parcels_sells, \n" +
            "   SUM(IF(com.move_type = '0' AND p.parcel_id IS NOT NULL AND pa.pays_sender = 0, p.amount,0.0)) AS total_charges_parcels\n" +
            "FROM\n" +
            "    cash_out co\n" +
            "    INNER JOIN cash_out_move com on com.cash_out_id = co.id  AND com.move_type = '0'\n" +
            "    INNER JOIN (SELECT * FROM payment GROUP BY id) AS p ON p.id = com.payment_id\n"+
            "    INNER JOIN parcels AS pa ON pa.id = p.parcel_id\n"+
            "WHERE\n" +
            "    co.branchoffice_id = ? AND co.updated_at BETWEEN ? AND ? AND co.cash_out_origin != 'driver';";

    private static final String QUERY_GET_SELL_AND_CHARGE_DETAIL_COUNT = "SELECT  \n" +
            "              COALESCE(COUNT(IF(com.move_type = '0' AND p.parcel_id IS NOT NULL AND pa.pays_sender = 1 , p.parcel_id,null) ))  AS totals_parcels_sells_count, \n" +
            "              COALESCE(COUNT(IF(com.move_type = '0' AND p.parcel_id IS NOT NULL AND pa.pays_sender = 0, com.quantity,null)))  AS total_charges_parcels_count\n" +
            "            FROM\n" +
            "                cash_out co\n" +
            "                INNER JOIN cash_out_move com on com.cash_out_id = co.id\n" +
            "                INNER JOIN payment AS p ON p.id = com.payment_id\n" +
            "                INNER JOIN parcels AS pa ON pa.id = p.parcel_id\n" +
            "            WHERE\n" +
            "                co.branchoffice_id = ? AND co.updated_at BETWEEN ? AND ? AND co.cash_out_origin != 'driver'";
    /* FALTA QUERY DE PAQUETERIA */

    private static final String QUERY_GET_EXTENDED = "SELECT \n" +
            "    co.id, \n" +
            "    coalesce(co.initial_fund, 0) initial_fund,\n" +
            "    coalesce(co.final_fund, 0) final_fund,\n" +
            "    co.init_ticket, \n" +
            "    co.cash_out_origin, \n" +
            "    cr.last_ticket,\n" +
            "    co.created_at,\n" +
            "    co.updated_at,\n" +
            "    co.total_reported,\n" +
            "    co.created_by\n" +
            "FROM\n" +
            "    cash_out co\n" +
            "    left join tickets t on t.cash_out_id = co.id\n" +
            "    left join cash_out_move com on com.cash_out_id = co.id\n" +
            "    left join cash_registers cr on cr.id = co.cash_register_id\n" +
            "WHERE\n" +
            "    co.id = ? LIMIT 1;";

    private static final String QUERY_GET_TRANSACTIONS = "SELECT \n" +
            "COALESCE((SELECT COUNT(quantity) FROM cash_out_move WHERE cash_out_id = ?), 0) total,\n" +
            "COALESCE((SELECT COUNT(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '0'), 0) total_income,\n" +
            "COALESCE((SELECT COUNT(quantity) FROM cash_out_move WHERE cash_out_id = ? AND move_type = '1'), 0) total_outcome,\n";

    private static final String QUERY_GET_BASIC_INFO = "SELECT * from cash_out co WHERE co.id = ?";

    private static final String QUERY_GET_PARCEL_INFO_FROM_DEBT_PAYMENTS = "SELECT\n" +
            "pa.created_at as date_sale,\n" +
            "u.name as seller_name,\n" +
            "CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "pa.parcel_tracking_code as code,\n" +
            "pm.name as payment_method,\n" +
            "dp.amount as amount,\n" +
            "CASE \n" +
            "  WHEN c.company_nick_name = 'IMSS' THEN 'IMSS'\n" +
            "  ELSE COALESCE(adviser.name, '')\n" +
            "END AS customer_adviser_name,\n" +
            "CASE \n" +
            "  WHEN c.parcel_type = 'agreement' THEN 'CONVENIO'\n" +
            "  WHEN c.parcel_type = 'guiapp' THEN 'GUIA PP'\n" +
            "  ELSE ''\n" +
            "END AS customer_parcel_type\n" +
            "FROM debt_payment dp\n" +
            "LEFT JOIN parcels pa ON dp.parcel_id = pa.id\n" +
            "LEFT JOIN customer c on pa.customer_id = c.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT \n" +
            "        p.id AS parcel_id,\n" +
            "        CASE\n" +
            "            WHEN p.pays_sender = 0\n" +
            "                AND cc.user_seller_id IS NULL\n" +
            "                AND cc.branchoffice_id IS NULL\n" +
            "                AND cc.parcel_type IS NULL\n" +
            "                AND cs.user_seller_id IS NOT NULL\n" +
            "                AND cs.branchoffice_id IS NOT NULL\n" +
            "                AND cs.parcel_type IS NOT NULL\n" +
            "            THEN cs.user_seller_id\n" +
            "            ELSE cc.user_seller_id\n" +
            "        END AS final_user_seller_id\n" +
            "    FROM parcels AS p\n" +
            "    LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            "    LEFT JOIN customer AS cs ON cs.id = p.sender_id\n" +
            ") pa_asesor ON pa_asesor.parcel_id = pa.id\n" +
            "LEFT JOIN users u on pa.created_by = u.id\n" +
            "LEFT JOIN users adviser ON adviser.id = pa_asesor.final_user_seller_id\n" +
            "LEFT JOIN payment p ON p.id = dp.payment_id\n" +
            "LEFT JOIN payment_method pm ON p.payment_method_id = pm.id\n" +
            "WHERE dp.ticket_id IN \n" +
            "(SELECT id FROM tickets t WHERE t.action = 'income' and t.cash_out_id = ?) \n" +
            "AND dp.parcel_id IS NOT NULL AND dp.prepaid_travel_id IS NULL";

    private static final String QUERY_GET_GUIAPP_INFO_FROM_DEBT_PAYMENTS = "SELECT\n" +
            "pp.created_at as date_sale,\n" +
            "u.name as seller_name,\n" +
            "CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "pp.tracking_code as code,\n" +
            "pm.name as payment_method,\n" +
            "dp.amount as amount,\n" +
            "CASE \n" +
            "  WHEN c.company_nick_name = 'IMSS' THEN 'IMSS'\n" +
            "  ELSE COALESCE(adviser.name, '')\n" +
            "END AS customer_adviser_name,\n" +
            "CASE \n" +
            "  WHEN c.parcel_type = 'agreement' THEN 'CONVENIO'\n" +
            "  WHEN c.parcel_type = 'guiapp' THEN 'GUIA PP'\n" +
            "  ELSE ''\n" +
            "END AS customer_parcel_type\n" +
            "FROM debt_payment dp\n" +
            "LEFT JOIN parcels_prepaid pp ON dp.parcel_prepaid_id = pp.id\n" +
            "LEFT JOIN customer c on pp.customer_id = c.id\n" +
            "LEFT JOIN users u on pp.crated_by = u.id\n" +
            "LEFT JOIN users adviser ON adviser.id = c.user_seller_id\n" +
            "LEFT JOIN payment p ON p.id = dp.payment_id\n" +
            "LEFT JOIN payment_method pm ON p.payment_method_id = pm.id\n" +
            "WHERE dp.ticket_id IN \n" +
            "(SELECT id FROM tickets t WHERE t.action = 'income' and t.cash_out_id = ?) \n" +
            "AND dp.parcel_prepaid_id IS NOT NULL";

    private static final String QUERY_GET_PURCHASE_RESUME_PARCEL = "SELECT \n" +
            "pa.created_at as date_sale,\n" +
            "u.name as seller_name,\n" +
            "CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "com.quantity as amount,\n" +
            "pa.parcel_tracking_code as code,\n" +
            "pm.name as payment_method,\n" +
            "CASE \n" +
            "  WHEN c.company_nick_name = 'IMSS' THEN 'IMSS'\n" +
            "  ELSE COALESCE(adviser.name, '')\n" +
            "END AS customer_adviser_name,\n" +
            "CASE \n" +
            "  WHEN c.parcel_type = 'agreement' THEN 'CONVENIO'\n" +
            "  WHEN c.parcel_type = 'guiapp' THEN 'GUIA PP'\n" +
            "  ELSE ''\n" +
            "END AS customer_parcel_type\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_out_move com ON com.cash_out_id = co.id\n" +
            "INNER JOIN payment p ON p.id = com.payment_id\n" +
            "INNER JOIN tickets t ON t.id = p.ticket_id\n" +
            "LEFT JOIN parcels pa on p.parcel_id = pa.id\n" +
            "LEFT JOIN users u on pa.created_by = u.id\n" +
            "LEFT JOIN customer c on pa.customer_id = c.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT \n" +
            "        p.id AS parcel_id,\n" +
            "        CASE\n" +
            "            WHEN p.pays_sender = 0\n" +
            "                AND cc.user_seller_id IS NULL\n" +
            "                AND cc.branchoffice_id IS NULL\n" +
            "                AND cc.parcel_type IS NULL\n" +
            "                AND cs.user_seller_id IS NOT NULL\n" +
            "                AND cs.branchoffice_id IS NOT NULL\n" +
            "                AND cs.parcel_type IS NOT NULL\n" +
            "            THEN cs.user_seller_id\n" +
            "            ELSE cc.user_seller_id\n" +
            "        END AS final_user_seller_id\n" +
            "    FROM parcels AS p\n" +
            "    LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            "    LEFT JOIN customer AS cs ON cs.id = p.sender_id\n" +
            ") pa_asesor ON pa_asesor.parcel_id = pa.id\n" +
            "LEFT JOIN users adviser ON adviser.id = pa_asesor.final_user_seller_id\n" +
            "LEFT JOIN payment_method pm ON p.payment_method_id = pm.id\n" +
            "WHERE co.id = ?\n" +
            "AND (t.action = 'purchase' OR t.action = 'change')\n" +
            "AND (p.parcel_id IS NOT NULL AND pa.pays_sender = ?)\n" +
            "AND com.move_type = '0'";

    private static final String QUERY_GET_SALES_COUNT_PARCEL = "SELECT\n" +
            "COALESCE(count(p.id), 0) as parcel\n" +
            "FROM parcels p\n" +
            "WHERE p.payment_condition = 'cash'\n" +
            "AND p.created_by = ? AND p.created_at BETWEEN ? AND ?";

    private static final String QUERY_GET_REPORT = "SELECT\n" +
            "   co.*,\n" +
            "    e.name AS employee_name,\n" +
            "    e.last_name AS employee_last_name,\n" +
            "    cr.prefix AS cash_register_prefix,\n" +
            "    cr.cash_register AS cash_register_cash_register,\n" +
            "    b.prefix AS branchoffice_prefix\n" +
            "FROM cash_out co\n" +
            "INNER JOIN cash_registers cr ON cr.id = co.cash_register_id\n" +
            "INNER JOIN employee e ON e.id = co.employee_id\n" +
            "INNER JOIN branchoffice b ON b.id = co.branchoffice_id\n" +
            "WHERE co.created_at BETWEEN ? AND ?";

    private static final String QUERY_GET_PAYMENT_METHODS = "SELECT * from payment_method where status = 1";
}
