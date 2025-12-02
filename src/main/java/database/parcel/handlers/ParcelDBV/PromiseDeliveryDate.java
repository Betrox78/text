package database.parcel.handlers.ParcelDBV;

import database.branchoffices.BranchofficeDBV;
import database.commons.DBHandler;
import database.parcel.ParcelDBV;
import database.parcel.enums.SHIPMENT_TYPE;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import service.commons.FestiveCalendar;
import utils.UtilsDate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static database.boardingpass.BoardingPassDBV.TERMINAL_DESTINY_ID;
import static database.shipments.ShipmentsDBV.TERMINAL_ORIGIN_ID;
import static service.commons.Constants.*;

public class PromiseDeliveryDate extends DBHandler<ParcelDBV> {

    public PromiseDeliveryDate(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String date = body.getString(DATE);
            Integer terminalOriginId = body.getInteger(_TERMINAL_ORIGIN_ID);
            Integer terminalDestiny = body.getInteger(_TERMINAL_DESTINY_ID);
            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(body.getString(_SHIPMENT_TYPE));
            getPromiseDeliveryDate(date, terminalOriginId, terminalDestiny, shipmentType).whenComplete((promiseDelivery, errPDD) -> {
                try {
                    if(errPDD != null) {
                        throw errPDD;
                    }
                    message.reply(new JsonObject()
                            .put(_PROMISE_DELIVERY_DATE, promiseDelivery.getString(_PROMISE_DELIVERY_DATE))
                            .put(_PROMISE_TIME, promiseDelivery.getInteger(_PROMISE_TIME))
                            .put(_ADDITIONAL_PROMISE_TIME, promiseDelivery.getInteger(_ADDITIONAL_PROMISE_TIME)));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getPromiseDeliveryDate(String date, Integer terminalOriginId, Integer terminalDestinyId, SHIPMENT_TYPE shipmentType) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject body = new JsonObject();
            body.put(DATE, date);
            body.put(TERMINAL_ORIGIN_ID, terminalOriginId);
            body.put(TERMINAL_DESTINY_ID, terminalDestinyId);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, BranchofficeDBV.ACTION_GET_DISTANCE);
            this.getVertx().eventBus().send(BranchofficeDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    JsonObject promiseDelivery = null;
                    int promTime = 0;

                    JsonArray results = (JsonArray) reply.result().body();
                    if(!results.isEmpty()) {
                        JsonObject result = results.getJsonObject(0);

                        if(shipmentType.includeOCU()) {
                            String promiseTimeOcu = result.getString(_PROMISE_TIME_OCU);
                            if (Objects.nonNull(promiseTimeOcu)) {
                                String[] promTimeOcu = promiseTimeOcu.split(":");
                                try {
                                    promTime = Integer.parseInt(promTimeOcu[0]);
                                } catch (Throwable ignored) {
                                    System.out.println("promTimeOcu not found, terminal_origin_id: "+ terminalOriginId + ", terminal_destiny_id: " + terminalDestinyId);
                                }
                                promiseDelivery = FestiveCalendar.getPromiseDeliveryDate(date, promTime);
                            }
                        }
                        if (shipmentType.includeEAD()) {
                            String promiseTimeEad = result.getString(_PROMISE_TIME_EAD);
                            if(Objects.nonNull(promiseTimeEad)) {
                                String[] promTimeEad = promiseTimeEad.split(":");
                                try {
                                    promTime = Integer.parseInt(promTimeEad[0]);
                                } catch (Throwable ignored) {
                                    System.out.println("promTimeEad not found, terminal_origin_id: "+ terminalOriginId + ", terminal_destiny_id: " + terminalDestinyId);
                                }
                                promiseDelivery = FestiveCalendar.getPromiseDeliveryDate(date, promTime);
                            }
                        }
                    }

                    String promiseDeliveryDateString = null;
                    int additionalPromiseTime = 0;
                    if(Objects.nonNull(promiseDelivery)) {
                        LocalDateTime promiseDeliveryDate = UtilsDate.stringDateToLocalDateTime(promiseDelivery.getString(_PROMISE_DELIVERY_DATE));
                        additionalPromiseTime = promiseDelivery.getInteger(_ADDITIONAL_PROMISE_TIME);

                        //convertir a horario servidor
                        promiseDeliveryDate = UtilsDate.convertServerTimeZone(promiseDeliveryDate);

                        promiseDeliveryDateString = UtilsDate.format_YYYY_MM_DD_T_HH_MM_SS(Date.from(promiseDeliveryDate.atZone(ZoneId.systemDefault()).toInstant()));
                    }

                    future.complete(new JsonObject()
                            .put(_PROMISE_DELIVERY_DATE, promiseDeliveryDateString)
                            .put(_PROMISE_TIME, promTime)
                            .put(_ADDITIONAL_PROMISE_TIME, additionalPromiseTime));

                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

}
