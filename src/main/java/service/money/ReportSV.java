/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.configs.GeneralConfigDBV;
import database.money.ReportDBV;
import database.money.TicketDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsResponse;

import static database.money.ReportDBV.END_DATE;
import static database.money.ReportDBV.INIT_DATE;
import static service.commons.Constants.*;
import static utils.UtilsResponse.responseDatatable;
import static utils.UtilsResponse.responseError;
import static utils.UtilsValidation.*;

import utils.UtilsValidation;

import java.util.Map;

/**
 *
 * @author daliacarlon
 */
public class ReportSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TicketDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/reports";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/general", AuthMiddleware.getInstance(), this::allServicesReport);
        this.addHandler(HttpMethod.POST, "/general/user", AuthMiddleware.getInstance(), this::allServicesReportByUser);
        super.start(startFuture);
    }

    private void allServicesReport(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, INIT_DATE, "report");
            isDateTimeAndNotNull(body, END_DATE, "report");

            vertx.eventBus().send(
                    ReportDBV.class.getSimpleName(),
                    body, options(ReportDBV.ACTION_ALL_SERVICES_REPORT),
                    reply -> {
                        this.genericResponse(context, reply);
                    });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }

    }
    private void allServicesReportByUser(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isDateTimeAndNotNull(body, INIT_DATE, "report");
            isDateTimeAndNotNull(body, END_DATE, "report");

            vertx.eventBus().send(
                    ReportDBV.class.getSimpleName(),
                    body, options(ReportDBV.ACTION_ALL_SERVICES_REPORT_BY_USER),
                    reply -> {
                        this.genericResponse(context, reply);
                    });

        } catch (UtilsValidation.PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        }

    }
}
