package database.e_wallet;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
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
import service.commons.Constants;
import utils.UtilsID;
import utils.UtilsMoney;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static service.commons.Constants.ACTION;
import static utils.UtilsDate.sdfDataBase;

public class EwalletDBV extends DBVerticle {

    public static final String ACTION_REGISTER_RECHARGE_RANGE = "EwalletDBV.handleRegisterRechargeRange";
    public static final String ACTION_GET_RECHARGE_RANGE_LIST = "EwalletDBV.getRechargeRangeList";
    public static final String ACTION_UPDATE_RECHARGE_RANGE_STATUS = "EwalletDBV.updateRechargeRangeStatus";
    public static final String ACTION_UPDATE_RECHARGE_RANGE = "EwalletDBV.updateRechargeRange";
    public static final String ACTION_INIT_WALLET = "EwalletDBV.initWallet";
    public static final String ACTION_REGISTER_WALLET_MOVE_SERVICE = "EwalletDBV.handleRegisterWalletMoveService";
    public static final String ACTION_GET_WALLET_MOVES = "EwalletDBV.getWalletMoves";
    public static final String ACTION_GET_BONIFICATION_PERCENT = "EwalletDBV.getBonificationPercent";
    public static final String GET_WALLET_BY_CUSTOMER_ID = "EwalletDBV.getWalletByCustomerId";
    public static final String GET_WALLET_BY_USER_ID = "EwalletDBV.getWalletByUserId";
    public static final String GET_TOTALS_RECHARGE = "EwalletDBV.getTotalsRecharge";
    public static final String ACTION_REGISTER_WALLET_MOVE_RECHARGE_AND_BONUS = "EwalletDBV.handleRegisterWalletMoveRechargeAndBonus";

    public EwalletDBV(){}

    @Override
    public String getTableName() {
        return "e_wallet";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case ACTION_REGISTER_RECHARGE_RANGE:
                this.handleRegisterRechargeRange(message);
                break;
            case ACTION_GET_RECHARGE_RANGE_LIST:
                this.getRechargeRangeList(message);
                break;
            case ACTION_UPDATE_RECHARGE_RANGE_STATUS:
                this.updateRechargeRangeStatus(message);
                break;
            case ACTION_UPDATE_RECHARGE_RANGE:
                this.updateRechargeRange(message);
                break;
            case ACTION_INIT_WALLET:
                this.initWallet(message);
                break;
            case ACTION_REGISTER_WALLET_MOVE_SERVICE:
                this.handleRegisterWalletMoveService(message);
                break;
            case ACTION_GET_WALLET_MOVES:
                this.getWalletMoves(message);
                break;
            case ACTION_GET_BONIFICATION_PERCENT:
                this.getBonificationPercent(message);
                break;
            case GET_WALLET_BY_CUSTOMER_ID:
                this.getWalletByCustomerId(message);
                break;
            case GET_TOTALS_RECHARGE:
                this.getTotalsRecharge(message);
                break;
            case ACTION_REGISTER_WALLET_MOVE_RECHARGE_AND_BONUS:
                this.handleRegisterWalletMoveRechargeAndBonus(message);
                break;
            case GET_WALLET_BY_USER_ID:
                this.getWalletByUserId(message);
                break;
        }
    }

    private void handleRegisterRechargeRange(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body().copy();
                this.registerRechargeRange(conn, body).whenComplete((resultRegister, error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        this.commit(conn, message, resultRegister);
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> registerRechargeRange(SQLConnection conn, JsonObject rechargeRange) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String insert = this.generateGenericCreate("e_wallet_recharges_range", rechargeRange);
            conn.update(insert, (AsyncResult<UpdateResult> registerReply) -> {
                try {
                    if( registerReply.failed()) {
                        throw registerReply.cause();
                    }
                    future.complete(rechargeRange);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void getRechargeRangeList(Message<JsonObject> message){
        try {
            this.dbClient.query(QUERY_GET_RECHARGE_RANGE_LIST, reply -> {
                try{
                    if( reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()){
                        message.reply(new JsonArray());
                    }
                    message.reply(new JsonArray(results));
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

    private void updateRechargeRangeStatus(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer status = body.getInteger("status");
        JsonArray params = new JsonArray()
                .add(status)
                .add(sdfDataBase(new Date()))
                .add(body.getInteger("updated_by"))
                .add(body.getInteger("id"));

        this.dbClient.queryWithParams(QUERY_UPDATE_STATUS_RECHARGE_RANGE , params, reply -> {
            if(reply.succeeded()){

                message.reply(reply.succeeded());
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void updateRechargeRange(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject service = new JsonObject()
                    .put("id", body.getInteger("id"))
                    .put("min_range", body.getFloat("min_range"))
                    .put("max_range", body.getFloat("max_range"))
                    .put("extra_percent", body.getInteger("extra_percent"))
                    .put("updated_at", sdfDataBase(new Date()))
                    .put("updated_by", body.getInteger("updated_by"));

            GenericQuery gen = this.generateGenericUpdate("e_wallet_recharges_range", service);
            this.dbClient.updateWithParams(gen.getQuery(), gen.getParams(), replyUpdate -> {
                try {
                    if (replyUpdate.failed()) {
                        throw replyUpdate.cause();
                    } else {
                        message.reply(new JsonObject().put("message", "Updated"));
                    }
                } catch (Throwable e) {
                    reportQueryError(message, e);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void initWallet(Message<JsonObject> message) {
        try {
            this.startTransaction(message, conn -> {
                JsonObject body = message.body();

                Integer user_id = body.getInteger("user_id");
                String referral_code = body.getString("referral_code");

                conn.queryWithParams(QUERY_GET_WALLET_ID_BY_USER_ID, new JsonArray().add(user_id), replyWexist -> {
                    try {
                        if(replyWexist.failed()) {
                            throw new Exception(replyWexist.cause());
                        }
                        List<JsonObject> walletsByUser = replyWexist.result().getRows();
                        if(!walletsByUser.isEmpty()){
                            throw new Exception("El usuario ya cuenta con monedero electronico");
                        }

                        String wallet_code = UtilsID.generateEwalletCode("CO", user_id);
                        JsonObject wallet = new JsonObject()
                                .put("user_id", user_id)
                                .put("code", wallet_code);

                        String insertWallet = this.generateGenericCreate("e_wallet", wallet);

                        conn.update(insertWallet, replyInsertWallet -> {
                            try {
                                if (replyInsertWallet.failed()) {
                                    throw new Exception(replyInsertWallet.cause());
                                }

                                Integer walletId = replyInsertWallet.result().getKeys().getInteger(0);

                                this.registerReferral(conn, walletId, referral_code).whenComplete((resultRegisterReferral, errReferral) -> {
                                    try {
                                        if (errReferral != null) {
                                            throw new Exception("Error al registrar referido");
                                        }
                                        this.commit(conn, message, new JsonObject().put("code", wallet_code));
                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch(Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }
                });
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> registerReferral(SQLConnection conn, Integer walletId, String referral_code) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if(referral_code == null || referral_code.isEmpty()) {
                future.complete(new JsonObject());
            } else {
                conn.queryWithParams(QUERY_GET_ID_BY_WALLET_CODE, new JsonArray().add(referral_code), replyReferral -> {
                    if( replyReferral.failed()) {
                        future.completeExceptionally(replyReferral.cause());
                    }

                    List<JsonObject> referrals = replyReferral.result().getRows();
                    JsonObject referralObj = referrals.get(0);
                    Integer referral_id = referralObj.getInteger("id");

                    conn.updateWithParams(QUERY_SET_REFERRAL_ID_ON_WALLET, new JsonArray().add(referral_id).add(walletId), replyUpdateReferral -> {
                        try {
                            if (replyUpdateReferral.failed()) {
                                throw new Exception(replyUpdateReferral.cause());
                            }
                            future.complete(new JsonObject().put("updated", true));
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void handleRegisterWalletMoveService(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body().copy();
                this.registerWalletMoveService(conn, body).whenComplete((resultMove, error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }
                        this.commit(conn, message, resultMove);
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private CompletableFuture<JsonObject> registerWalletMoveService(SQLConnection conn, JsonObject data) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if(data.getInteger("customer_id") != null) {
                conn.queryWithParams(QUERY_GET_WALLET_BY_CUSTOMER_ID, new JsonArray().add(data.getInteger("customer_id")), replyWalletId -> {
                    if( replyWalletId.failed()) {
                        future.completeExceptionally(replyWalletId.cause());
                    }

                    List<JsonObject> walletRows = replyWalletId.result().getRows();
                    if(walletRows.isEmpty()) {
                        future.complete(new JsonObject());
                    } else {
                        JsonObject walletObj = walletRows.get(0);
                        Future<Message<JsonObject>> fConfig = Future.future();
                        String serviceTypeField = "";
                        switch(data.getString("service_type")) {
                            case "boarding_pass":
                                serviceTypeField = "boarding_pass_e_wallet_percent";
                                break;
                            case "parcel":
                                serviceTypeField = "parcel_e_wallet_percent";
                                break;
                        }

                        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", serviceTypeField),
                                new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), fConfig.completer());

                        List<Future> futures = new ArrayList<>();
                        futures.add(fConfig);

                        CompositeFuture.all(futures).setHandler(detailReply -> {
                            if (detailReply.succeeded()) {
                                Message<JsonObject> bonusPercentMsg = detailReply.result().resultAt(0);
                                JsonObject bonusPercentObj = bonusPercentMsg.body();
                                Double bonusPercent = Double.valueOf(bonusPercentObj.getString("value"));

                                Double bonusAmount = UtilsMoney.round(((data.getDouble("service_amount") * bonusPercent) / 100), 2);
                                Double oldAmount = 0.00;
                                String query = "";
                                switch(data.getString("wallet_type")) {
                                    case "bonus":
                                        oldAmount = walletObj.getDouble("available_bonus");
                                        query = QUERY_UPDATE_WALLET_AVAILABLE_BONUS;
                                        break;
                                    case "wallet_recharge":
                                        oldAmount = walletObj.getDouble("available_amount");
                                        query = QUERY_UPDATE_WALLET_AVAILABLE_AMOUNT;
                                        break;
                                }

                                Double newAmount = UtilsMoney.round((oldAmount + bonusAmount), 2);
                                JsonObject move = new JsonObject()
                                        .put("e_wallet_id", walletObj.getInteger("id"))
                                        .put("wallet_type", data.getString("wallet_type"))
                                        .put("move_type", "income")
                                        .put("service_type", data.getString("service_type"))
                                        .put("payment_id", data.getInteger("payment_id"))
                                        .put("before_amount", oldAmount)
                                        .put("amount", bonusAmount)
                                        .put("after_amount", newAmount);

                                String insertMove = this.generateGenericCreate("e_wallet_move", move);

                                String updateAmountQuery = query;
                                conn.update(insertMove, (AsyncResult<UpdateResult> replyInsertM) -> {
                                    try {
                                        if (replyInsertM.succeeded()) {
                                            JsonArray paramsUpdateW = new JsonArray()
                                                    .add(newAmount)
                                                    .add(sdfDataBase(new Date()))
                                                    .add(walletObj.getInteger("id"));

                                            conn.queryWithParams(updateAmountQuery, paramsUpdateW, replyUpdateW -> {
                                                if(replyUpdateW.succeeded()){
                                                    future.complete(new JsonObject());
                                                } else {
                                                    future.completeExceptionally(replyUpdateW.cause());
                                                }
                                            });
                                        } else {
                                            future.completeExceptionally(replyInsertM.cause());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        future.completeExceptionally(e);
                                    }
                                });
                            } else {
                                future.completeExceptionally(detailReply.cause());
                            }
                        });
                    }
                });
            } else {
                future.complete(new JsonObject());
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> registerWalletMove(SQLConnection conn, JsonObject data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String insertMove = this.generateGenericCreate("e_wallet_move", data);

            conn.update(insertMove, (AsyncResult<UpdateResult> replyInsertM) -> {
                try {
                    if(replyInsertM.failed()) {
                        future.completeExceptionally(replyInsertM.cause());
                    } else {
                        future.complete(replyInsertM.succeeded());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void getWalletMoves(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer user_id = body.getInteger("user_id");
            if(user_id == null) {
                message.reply(new JsonArray());
            } else {
                this.dbClient.queryWithParams(QUERY_GET_WALLET_ID_BY_USER_ID, new JsonArray().add(user_id), replyWexist -> {
                    try {
                        if(replyWexist.failed()) {
                            throw new Exception(replyWexist.cause());
                        }
                        List<JsonObject> walletsByUser = replyWexist.result().getRows();
                        if(walletsByUser.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            Integer walletId = walletsByUser.get(0).getInteger("id");
                            this.dbClient.queryWithParams(QUERY_GET_WALLET_MOVES, new JsonArray().add(walletId), reply -> {
                                try{
                                    if( reply.failed()){
                                        throw reply.cause();
                                    }
                                    List<JsonObject> results = reply.result().getRows();
                                    if(results.isEmpty()){
                                        message.reply(new JsonArray());
                                    }
                                    message.reply(new JsonArray(results));
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    reportQueryError(message, t);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            }
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    public void getBonificationPercent(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String serviceType = body.getString("service_type");
            if(serviceType.isEmpty()) {
                message.reply(new JsonObject());
            } else {
                String serviceTypeField = "";
                switch(serviceType) {
                    case "boarding_pass":
                        serviceTypeField = "boarding_pass_e_wallet_percent";
                        break;
                    case "parcel":
                        serviceTypeField = "parcel_e_wallet_percent";
                        break;
                }
                vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", serviceTypeField),
                        new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), replyPercent -> {
                            try {
                                if (replyPercent.succeeded()) {
                                    JsonObject resultObj = (JsonObject) replyPercent.result().body();
                                    Double percent = Double.valueOf(resultObj.getString("value"));
                                    if(percent == null) {
                                        message.reply(new JsonObject());
                                    } else {
                                        message.reply(new JsonObject().put("percent", percent));
                                    }
                                } else {
                                    replyPercent.cause().printStackTrace();
                                    reportQueryError(message, replyPercent.cause());
                                }
                            } catch(Throwable t) {
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
            }
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getWalletByCustomerId(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer customer_id = body.getInteger("idCustomer");
            if(customer_id == null) {
                message.reply(new JsonArray());
            } else {
                this.dbClient.queryWithParams(QUERY_GET_WALLET_BY_CUSTOMER_ID, new JsonArray().add(customer_id), replyWexist -> {
                    try{
                        if( replyWexist.failed()){
                            throw replyWexist.cause();
                        }
                        List<JsonObject> results = replyWexist.result().getRows();
                        if(results.isEmpty()){
                            message.reply(new JsonObject());
                        } else {
                            message.reply(results.get(0));
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            }
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getWalletByUserId(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer user_id = body.getInteger("user_id");
            if(user_id == null) {
                message.reply(new JsonArray());
            } else {
                this.dbClient.queryWithParams(QUERY_GET_WALLET_BASIC_INFO_USER_ID, new JsonArray().add(user_id), replyWexist -> {
                    try{
                        if( replyWexist.failed()){
                            throw replyWexist.cause();
                        }
                        List<JsonObject> results = replyWexist.result().getRows();
                        if(results.isEmpty()){
                            message.reply(new JsonObject());
                        } else {
                            message.reply(results.get(0));
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            }
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getTotalsRecharge(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject payment = body.getJsonObject("payment");
            this.dbClient.queryWithParams(QUERY_GET_RANGE_BY_TOTAL,
                    new JsonArray().add(payment.getDouble("amount")).add(payment.getDouble("amount")), replyRanges -> {
                try {
                    if(replyRanges.failed()) {
                        throw new Exception(replyRanges.cause());
                    }
                    List<JsonObject> ranges = replyRanges.result().getRows();
                    if(ranges.isEmpty()) {
                        message.reply(new JsonObject().put("bonus_amount", 0));
                    } else {
                        Double extra_percent = ranges.get(0).getDouble("extra_percent");
                        Double bonus_amount = UtilsMoney.round(((payment.getDouble("amount") * (extra_percent / 100))), 2);
                        message.reply(new JsonObject()
                                .put("range", ranges.get(0))
                                .put("bonus_amount", bonus_amount)
                        );
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    public CompletableFuture<JsonObject> registerWalletRecharge(SQLConnection conn, JsonObject data) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject totalsRecharge = (JsonObject) data.remove("totals_recharge");
            if(totalsRecharge.getJsonObject("range") != null) {
                data.put("e_wallet_recharges_range_id", totalsRecharge.getJsonObject("range").getInteger("id"));
            }

            String insert = this.generateGenericCreate("e_wallet_recharge", data);
            conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.succeeded()) {
                        Integer rechargeId = reply.result().getKeys().getInteger(0);
                        future.complete(new JsonObject().put("id", rechargeId));
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    public CompletableFuture<Boolean> updateWalletAmount(SQLConnection conn, JsonObject data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonObject wallet = (JsonObject) data.remove("wallet");
            Double amount = 0.00;
            String query = "";
            switch(data.getString("wallet_type")) {
                case "bonus":
                    amount = wallet.getDouble("available_bonus");
                    query = QUERY_UPDATE_WALLET_AVAILABLE_BONUS;
                    break;
                case "wallet_recharge":
                    amount = wallet.getDouble("available_amount");
                    query = QUERY_UPDATE_WALLET_AVAILABLE_AMOUNT;
                    break;
            }
            Double newAmount = UtilsMoney.round((amount + data.getDouble("amount")), 2);

            JsonArray paramsUpdateW = new JsonArray()
                    .add(newAmount)
                    .add(sdfDataBase(new Date()))
                    .add(wallet.getInteger("id"));

            conn.queryWithParams(query, paramsUpdateW, replyUpdateW -> {
                if(replyUpdateW.succeeded()){
                    future.complete(replyUpdateW.succeeded());
                } else {
                    future.completeExceptionally(replyUpdateW.cause());
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    public CompletableFuture<Boolean> verifyReferral(SQLConnection conn, JsonObject data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            if(data.getInteger("customer_id") != null) {
                conn.queryWithParams(QUERY_GET_PURCHASES_COUNT_BY_CUSTOMER_ID, new JsonArray().add(data.getInteger("customer_id")), replyCount -> {
                    if( replyCount.failed()) {
                        future.completeExceptionally(replyCount.cause());
                    }
                    List<JsonObject> resultCount = replyCount.result().getRows();
                    Integer purchasesCount = resultCount.get(0).getInteger("purchases_count");

                    if(purchasesCount == 1) {
                        conn.queryWithParams(QUERY_GET_WALLET_BY_CUSTOMER_ID, new JsonArray().add(data.getInteger("customer_id")), replyWalletId -> {
                            if( replyWalletId.failed()) {
                                future.completeExceptionally(replyWalletId.cause());
                            }

                            List<JsonObject> walletRows = replyWalletId.result().getRows();
                            if(walletRows.isEmpty()) {
                                // NO TIENE WALLET
                                future.complete(true);
                            } else {
                                JsonObject wallet = walletRows.get(0);
                                if(wallet.getInteger("referenced_by") != null) {
                                    conn.queryWithParams(QUERY_GET_WALLET_BY_ID,
                                            new JsonArray().add(wallet.getInteger("referenced_by")), replyReferral -> {
                                                try{
                                                    if(replyReferral.failed()){
                                                        future.completeExceptionally(replyReferral.cause());
                                                    }

                                                    List<JsonObject> resultReferral = replyReferral.result().getRows();
                                                    if(resultReferral.isEmpty()) {
                                                        future.complete(true);
                                                    } else {
                                                        JsonObject walletReferral = resultReferral.get(0);
                                                        Double bonus_referral_amount = UtilsMoney.round(((data.getDouble("total_amount") * (data.getDouble("bonus_referral") / 100))), 2);

                                                        JsonObject walletUpdateObj = new JsonObject()
                                                                .put("wallet", walletReferral)
                                                                .put("wallet_type", "bonus")
                                                                .put("amount", bonus_referral_amount);

                                                        updateWalletAmount(conn, walletUpdateObj)
                                                                .whenComplete((Boolean replyWalletUpdate, Throwable ErrorWalletUpdate) -> {
                                                                    try {
                                                                        if(ErrorWalletUpdate != null) {
                                                                            throw ErrorWalletUpdate;
                                                                        }

                                                                        Double newAmount = UtilsMoney.round(((walletReferral.getDouble("available_bonus") + bonus_referral_amount)), 2);
                                                                        JsonObject walletDataMove = new JsonObject()
                                                                                .put("e_wallet_id", walletReferral.getInteger("id"))
                                                                                .put("wallet_type", "bonus")
                                                                                .put("move_type", "income")
                                                                                .put("service_type", "referral")
                                                                                .put("before_amount", walletReferral.getDouble("available_bonus"))
                                                                                .put("amount", bonus_referral_amount)
                                                                                .put("after_amount", newAmount);

                                                                        registerWalletMove(conn, walletDataMove)
                                                                                .whenComplete((Boolean replyMove, Throwable ErrorMove) -> {
                                                                                    try {
                                                                                        if(ErrorMove != null) {
                                                                                            throw ErrorMove;
                                                                                        }
                                                                                        future.complete(replyMove);
                                                                                    } catch(Throwable t) {
                                                                                        t.printStackTrace();
                                                                                        future.completeExceptionally(t);
                                                                                    }
                                                                                });
                                                                    } catch(Throwable t) {
                                                                        t.printStackTrace();
                                                                        future.completeExceptionally(t);
                                                                    }
                                                                });
                                                    }
                                                }catch(Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                } else {
                                    // NO FUE REFERENCIADO POR NADIE Y ES SU PRIMER COMPRA
                                    future.complete(true);
                                }
                            }
                        });
                    } else {
                        // TIENE MAS DE 1 COMPRA
                        future.complete(true);
                    }
                });
            } else {
                // compra como cliente invitado
                future.complete(true);
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void handleRegisterWalletMoveRechargeAndBonus(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body().copy();
                this.registerWalletMove(conn, body.getJsonObject("recharge_move")).whenComplete((resultMove, error) -> {
                    try {
                        if (error != null) {
                            throw error;
                        }

                        if(body.getJsonObject("bonus_move").getDouble("amount") > 0) {
                            this.registerWalletMove(conn, body.getJsonObject("bonus_move")).whenComplete((resultBonusMove, errorBonus) -> {
                                try {
                                    if (errorBonus != null) {
                                        throw errorBonus;
                                    }
                                    this.commit(conn, message, new JsonObject());
                                } catch (Throwable t){
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        } else {
                            this.commit(conn, message, new JsonObject());
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            }catch (Throwable t) {
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    public CompletableFuture<JsonObject> calculateBonificationByService(SQLConnection conn, JsonObject data) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Double bonusAmount = UtilsMoney.round(((data.getDouble("service_amount") * data.getDouble("bonus_bp")) / 100), 2);

            if(data.getInteger("customer_id") == null) {
                future.complete(new JsonObject()
                        .put("bonification", bonusAmount)
                        .put("status", "not_generated"));
            } else {
                conn.queryWithParams(QUERY_GET_WALLET_BY_CUSTOMER_ID, new JsonArray().add(data.getInteger("customer_id")), replyWalletId -> {
                    if( replyWalletId.failed()) {
                        future.completeExceptionally(replyWalletId.cause());
                    }

                    List<JsonObject> walletRows = replyWalletId.result().getRows();
                    if(walletRows.isEmpty()) {
                        future.complete(new JsonObject()
                                .put("bonification", bonusAmount)
                                .put("status", "not_generated"));
                    } else {
                        future.complete(new JsonObject()
                                .put("bonification", bonusAmount)
                                .put("status", "generated"));
                    }
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }
    private static final String QUERY_GET_RECHARGE_RANGE_LIST = "SELECT\n" +
            "ewr.id,\n" +
            "ewr.min_range,\n" +
            "ewr.max_range,\n" +
            "ewr.extra_percent,\n" +
            "ewr.status,\n" +
            "ewr.created_at\n" +
            "FROM e_wallet_recharges_range as ewr\n" +
            "WHERE ewr.status != 3";
    private static final String QUERY_UPDATE_STATUS_RECHARGE_RANGE = "UPDATE e_wallet_recharges_range SET status = ? , updated_at = ? ,updated_by = ? WHERE id = ?";
    private static final String QUERY_GET_WALLET_ID_BY_USER_ID = "SELECT ew.id FROM e_wallet ew WHERE ew.user_id = ?";
    private static final String QUERY_GET_WALLET_BY_CUSTOMER_ID = "SELECT * FROM e_wallet WHERE user_id = (SELECT user_id FROM customer WHERE id = ?)";
    private static final String QUERY_GET_WALLET_BY_USER_ID = "SELECT * FROM e_wallet WHERE user_id = ?";
    private static final String QUERY_GET_WALLET_BASIC_INFO_USER_ID = "SELECT \n" +
            "ew.*, dl.link\n" +
            " FROM\n" +
            "e_wallet ew\n" +
            " LEFT JOIN\n" +
            "deep_link_referral AS dl ON dl.e_wallet_id = ew.id\n" +
            " WHERE\n" +
            "ew.user_id = ?";
    private static final String QUERY_GET_ID_BY_WALLET_CODE = "SELECT ew.* FROM e_wallet ew WHERE ew.code = ?";
    private static final String QUERY_SET_REFERRAL_ID_ON_WALLET = "UPDATE e_wallet SET referenced_by = ? WHERE id = ?";
    private static final String QUERY_GET_WALLET_BY_ID = "SELECT ew.* FROM e_wallet ew WHERE ew.id = ?";
    private static final String QUERY_UPDATE_WALLET_AVAILABLE_AMOUNT = "UPDATE e_wallet SET available_amount = ?, updated_at = ? WHERE id = ?";
    private static final String QUERY_UPDATE_WALLET_AVAILABLE_BONUS = "UPDATE e_wallet SET available_bonus = ?, updated_at = ? WHERE id = ?";
    private static final String QUERY_GET_WALLET_MOVES = "SELECT\n" +
            "    ewm.id AS move_id,\n" +
            "    ewm.wallet_type,\n" +
            "    ewm.move_type,\n" +
            "    ewm.service_type,\n" +
            "    ewm.before_amount,\n" +
            "    ewm.amount,\n" +
            "    ewm.after_amount,\n" +
            "    pm.id AS payment_id,\n" +
            "    CASE\n" +
            "        WHEN ewm.service_type = 'boarding_pass' THEN bp.reservation_code\n" +
            "        WHEN ewm.service_type = 'wallet_recharge' THEN ewr.code\n" +
            "        WHEN ewm.service_type = 'parcel' THEN pa.parcel_tracking_code\n" +
            "        ELSE NULL\n" +
            "    END AS service_code,\n" +
            "    ewm.created_at\n" +
            "FROM\n" +
            "    e_wallet_move AS ewm\n" +
            "    INNER JOIN payment AS pm ON ewm.payment_id = pm.id\n" +
            "    LEFT JOIN boarding_pass AS bp ON pm.boarding_pass_id = bp.id\n" +
            "    LEFT JOIN parcels AS pa ON pm.parcel_id = pa.id\n" +
            "    LEFT JOIN e_wallet_recharge AS ewr ON pm.e_wallet_recharge_id = ewr.id\n" +
            "WHERE\n" +
            "    ewm.e_wallet_id = ?\n" +
            "ORDER BY\n" +
            "    ewm.created_at DESC";
    private static final String QUERY_GET_RANGE_BY_TOTAL = "SELECT\n" +
            "ewr.* FROM\n" +
            "e_wallet_recharges_range ewr\n" +
            "WHERE ? >= ewr.min_range AND ? <= ewr.max_range\n" +
            "AND ewr.status = 1";

    private static final String QUERY_GET_PURCHASES_COUNT_BY_CUSTOMER_ID = "SELECT\n" +
            "COUNT(*) as purchases_count\n" +
            "FROM\n" +
            "    boarding_pass\n" +
            "WHERE\n" +
            "    customer_id = ? AND boardingpass_status NOT IN (0 , 4)";
}