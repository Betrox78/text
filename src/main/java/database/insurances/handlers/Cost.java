package database.insurances.handlers;

import database.commons.DBHandler;
import database.insurances.InsurancesDBV;
import database.promos.enums.SERVICES;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsMoney;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsDate.format_yyyy_MM_dd;

public class Cost extends DBHandler<InsurancesDBV> {

    public Cost(InsurancesDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            Double insuranceValue = body.getDouble(_INSURANCE_VALUE);
            SERVICES service = SERVICES.valueOf(body.getString(_SERVICE));

            List<Future> tasksSettings = new ArrayList<>();
            tasksSettings.add(validateCurrentInsurance());
            tasksSettings.add(getIvaValue());
            tasksSettings.add(getInsurancePercent(service));
            tasksSettings.add(getInsuranceMaxValue());

            CompositeFuture.all(tasksSettings).setHandler(reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    Integer insuranceId = reply.result().result().resultAt(0);
                    Double ivaPercent = reply.result().result().resultAt(1);
                    Double insurancePercent = reply.result().result().resultAt(2);
                    Double insuranceMaxValue = reply.result().result().resultAt(3);

                    if (insuranceValue > insuranceMaxValue) {
                        throw new Exception("Insurance value exceeds the maximum allowed value");
                    }

                    double insuranceAmount = UtilsMoney.round((insuranceValue * insurancePercent) / 100, 2);
                    double insuranceAmountBeforeIva = UtilsMoney.round(insuranceAmount / (ivaPercent + 1), 2);
                    double iva = UtilsMoney.round(insuranceAmountBeforeIva * ivaPercent, 2);

                    message.reply(new JsonObject()
                            .put(_INSURANCE_ID, insuranceId)
                            .put(_INSURANCE_PERCENT, insurancePercent)
                            .put(_MAX_INSURANCE_VALUE, UtilsMoney.round(insuranceMaxValue, 2))
                            .put(_INSURANCE_AMOUNT, UtilsMoney.round(insuranceAmountBeforeIva, 2))
                            .put(_IVA, UtilsMoney.round(iva, 2)));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private Future<Integer> validateCurrentInsurance() throws ParseException {
        Future<Integer> future = Future.future();
        String now = format_yyyy_MM_dd(new Date());
        this.dbClient.queryWithParams(QUERY_INSURANCE_VALIDATION, new JsonArray().add(now), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("There are no insurance policies available");
                }
                future.complete(result.get(0).getInteger(ID));
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }


    private static final String QUERY_INSURANCE_VALIDATION = "SELECT id FROM insurances WHERE ? BETWEEN init AND end AND status = 1;";

    private Future<Double> getIvaValue() {
        Future<Double> future = Future.future();
        this.dbClient.query("SELECT value FROM general_setting WHERE FIELD = 'iva';", reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("Iva value not found");
                }
                future.complete(Double.parseDouble(result.get(0).getString(VALUE)) / 100);
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Double> getInsurancePercent(SERVICES service) {
        Future<Double> future = Future.future();
        try {
            String serviceParam = service.equals(SERVICES.parcel_inhouse) ? "insurance_percent_inhouse" : "insurance_percent";

            JsonArray param = new JsonArray().add(serviceParam);
            this.dbClient.queryWithParams("SELECT value FROM general_setting WHERE FIELD = ?;", param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception(serviceParam + " value not found");
                    }
                    future.complete(Double.parseDouble(result.get(0).getString(VALUE)));
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<Double> getInsuranceMaxValue() {
        Future<Double> future = Future.future();
        this.dbClient.query("SELECT value FROM general_setting WHERE FIELD = 'max_insurance_value';", reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if (result.isEmpty()) {
                    throw new Exception("max_insurance_value value not found");
                }
                future.complete(Double.parseDouble(result.get(0).getString(VALUE)));
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

}
