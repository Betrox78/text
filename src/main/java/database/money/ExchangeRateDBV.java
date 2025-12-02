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
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLOptions;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.ACTION;

import org.json.JSONObject;
import service.money.ExchangeRateService;
import utils.UtilsDate;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ExchangeRateDBV extends DBVerticle {

    public static final String ACTION_CURRENCY_REPORT = "ExchangeRateDBV.currencyEeport";
    public static final String ACTION_CRON_UPDATE = "ExchangeRateDBV.cronUpdate";

    @Override
    public String getTableName() {
        return "exchange_rate";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_CURRENCY_REPORT:
                this.currencyReport(message);
                break;
            case ACTION_CRON_UPDATE:
                this.cronUpdate(message);
        }
    }

    private void cronUpdate(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                List<String> ids = Arrays.stream(ExchangeRateService.SERIE_A_PESO_MEXICANO.values())
                        .map(ExchangeRateService.SERIE_A_PESO_MEXICANO::getId).collect(toList());
                List<String> exchangesInserts = new ArrayList<>();
                String disable = "UPDATE exchange_rate\n"
                        + "SET status = 3\n"
                        + "WHERE status = 1 and serie_id IN (" + "'" + String.join("','", ids) + "'" + ");";
                exchangesInserts.add(disable);
                JsonArray exchangeRates = message.body().getJsonArray("exchange_rates");
                double abordoQty = message.body().getDouble("cron_exchange_rate_abordo_minus_qty");

                if(exchangeRates == null || exchangeRates.size() == 0){
                    throw new Exception("exchange rate must be different from null and contains at least one value.");
                }
                List<Future> list = exchangeRates.stream()
                        .map(exRage -> this.getExchangeRateCurrencyId(con, (JsonObject) exRage))
                        .collect(toList());

                CompositeFuture.all(list).setHandler(reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        for (int i = 0; i < exchangeRates.size(); i++) {
                            JsonObject exRate = exchangeRates.getJsonObject(i);
                            Integer currencyId = exRate.getInteger("currency_id");
                            if (currencyId == null) {
                                continue;
                            }
                            double abordoQtyFinal = (exRate.getDouble("change_amount") - abordoQty);
                            if (abordoQtyFinal < 0) {
                                abordoQtyFinal = 0.01;
                            }
                            String query = "INSERT INTO exchange_rate (base_currency, currency_id, "
                                    + "change_amount, abordo_rate, serie_id, created_by) VALUES\n"
                                    + "(\n"
                                    + "	(SELECT id FROM currency WHERE currency.abr = 'MXN' AND status = 1),\n"
                                    + "	"+ currencyId +",\n"
                                    + "	" + exRate.getDouble("change_amount") + ",\n"
                                    + "	" + abordoQtyFinal + ",\n"
                                    + "	'" + exRate.getString("serie_id") + "',\n"
                                    + "	1\n"
                                    + ");";
                            exchangesInserts.add(query);
                        }
                        exchangesInserts.add("UPDATE config_system SET cron_exchange_rate_updated_last_time = '" + UtilsDate.format_yyyy_MM_dd(new Date()) + "';");
                        con.batch(exchangesInserts, batchReply -> {
                            try {
                                if (batchReply.failed()){
                                    throw batchReply.cause();
                                }
                                this.commit(con, message, null);
                            } catch (Throwable t){
                                this.rollback(con, t, message);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });

            } catch (Exception ex) {
                Logger.getLogger(ExchangeRateDBV.class.getName()).log(Level.SEVERE, null, ex);
                this.rollback(con, ex, message);
            }

        });
    }

    private Future<JsonObject> getExchangeRateCurrencyId(SQLConnection con, JsonObject exRate) {
        Future<JsonObject> future = Future.future();
        try {
            String query = "SELECT id FROM currency WHERE currency.abr = ? AND status = 1";
            JsonArray params = new JsonArray().add(exRate.getString("serie_iso"));
            con.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()) {
                        future.fail(reply.cause());
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        future.complete(exRate);
                    } else {
                        exRate.put("currency_id", results.get(0).getInteger("id"));
                        future.complete(exRate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.complete(exRate);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            future.complete(exRate);
        }

        return future;
    }

    private void currencyReport(Message<JsonObject> message) {
        this.dbClient.query(CURRENCY_REPORT_QUERY, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(new JsonArray(reply.result().getRows()));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

//<editor-fold defaultstate="collapsed" desc="queries">
    private static final String CURRENCY_REPORT_QUERY = "SELECT\n"
            + "	er.*,\n"
            + "	cr.name AS currency_name,\n"
            + "	cr.abr AS currency_abr,\n"
            + "	bcr.name AS base_currency_name,\n"
            + "	bcr.abr AS base_currency_abr\n"
            + "FROM\n"
            + "	exchange_rate er\n"
            + "JOIN currency cr ON\n"
            + "	er.currency_id = cr.id\n"
            + "JOIN currency bcr ON\n"
            + "	er.base_currency = bcr.id\n"
            + "WHERE\n"
            + "	er.status != 3\n"
            + "	AND cr.status != 3;\n"
            + "	";
//</editor-fold>
    
}
