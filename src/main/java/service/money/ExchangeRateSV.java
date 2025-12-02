/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.ExchangeRateDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import static service.commons.Constants.ACTION;
import static service.commons.Constants.UNEXPECTED_ERROR;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsJWT;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseInvalidToken;
import static utils.UtilsResponse.responseOk;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ExchangeRateSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ExchangeRateDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/exchangesRates";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/report/currency", AuthMiddleware.getInstance(), this::currencyReport);
        super.start(startFuture);
    }

    private void currencyReport(RoutingContext context) {
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ExchangeRateDBV.ACTION_CURRENCY_REPORT);
        vertx.eventBus().send(this.getDBAddress(), null, options, reply -> {
            this.genericResponse(context, reply);
        });
    }

}
