/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crons;

import database.commons.ErrorCodes;
import database.configs.CRONConfDBV;
import database.money.ExchangeRateDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.MainVerticle;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

import service.money.ExchangeRateService;
import utils.UtilsDate;

/**
 *
 * @author ulises
 */
public class CronExchangeRate extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();
        startExchangeRateCron();
    }
    
     private void startExchangeRateCron() {
        this.vertx.setPeriodic(1000 * 60 * 60, __ -> {
            System.out.println("running" + new Date().toString());
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, CRONConfDBV.ACTION_CRON_EXCHANGE_RATE_ACTIVE);
            this.vertx.eventBus().send(CRONConfDBV.class.getSimpleName(), new JsonObject(), options,
                    (AsyncResult<Message<JsonObject>> activeReply) -> {
                        try{
                            if(activeReply.failed()) {
                                throw new Exception(activeReply.cause());
                            }
                            if (activeReply.result().body()
                                    .getBoolean("cron_exchange_rate_update_active")) {

                                String lastUpdated = activeReply.result().body()
                                        .getString("cron_exchange_rate_updated_last_time");
                                if (lastUpdated != null) {
                                    try {
                                        Date lastUpdatedDate = UtilsDate.parse_yyyy_MM_dd(lastUpdated);
                                        Calendar cal = Calendar.getInstance();
                                        int actualDay = cal.get(Calendar.DAY_OF_YEAR);
                                        cal.setTime(lastUpdatedDate);
                                        int updatedDay = cal.get(Calendar.DAY_OF_YEAR);
                                        if (actualDay == updatedDay) { //already updated today
                                            return;
                                        }
                                        this.updateExchangeRates(activeReply.result().body()
                                                .getDouble("cron_exchange_rate_abordo_minus_qty"));
                                    } catch (ParseException ex) {
                                        Logger.getLogger(MainVerticle.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                } else {
                                    this.updateExchangeRates(activeReply.result().body()
                                            .getDouble("cron_exchange_rate_abordo_minus_qty"));
                                }
                            } else {
                                System.out.println("CRON update exchage rates not active");
                            }

                        }catch (Exception ex) {
                            ex.printStackTrace();
                            Logger.getLogger(MainVerticle.class.getName()).log(Level.SEVERE, null, activeReply.cause());
                        }
                    });

        });
    }

    private void updateExchangeRates(double minusQuenityAbordoRate) {
        this.vertx.executeBlocking(blocking -> {
            try {
                blocking.complete(ExchangeRateService.getResult());
            } catch (IOException ex) {
                System.out.println("cron exchange rates failed " + ex.getMessage());
            }
        }, (AsyncResult<JsonArray> reply) -> {
            JsonArray list = reply.result();
            JsonObject param = new JsonObject()
                    .put("exchange_rates", list)
                    .put("cron_exchange_rate_abordo_minus_qty", minusQuenityAbordoRate);
            this.vertx.eventBus().send(ExchangeRateDBV.class.getSimpleName(), param,
                    new DeliveryOptions().addHeader(ACTION, ExchangeRateDBV.ACTION_CRON_UPDATE),
                    updateReply -> {
                        try{
                            if(reply.failed()){
                                throw  new Exception(reply.cause());
                            }
                            System.out.println("update cron exchange rates failed " + updateReply.cause().getMessage());


                        }catch(Exception e){
                            System.out.println("succed");

                        }

                    });
        });
    }
    
}
