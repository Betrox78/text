/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.customers;

import database.customers.CustomerAddressesDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import static service.commons.Constants.ACTION;

import org.json.HTTP;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.UtilsJWT;
import static utils.UtilsResponse.responseInvalidToken;

/**
 *
 * @author Saul
 */
public class CustomerAddressesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CustomerAddressesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/customerAddresses";
    }
    
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/customer/:id", AuthMiddleware.getInstance(), this::findByCustomerId);
        super.start(startFuture);
    }
    
    private void findByCustomerId(RoutingContext context) {
        JsonObject message = new JsonObject().put("customer_id", Integer.valueOf(context.request().getParam("id")));
        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerAddressesDBV.GET_BY_CUSTOMERID);
        vertx.eventBus().send(this.getDBAddress(), message, options, reply -> {
            this.genericResponse(context, reply, "Found");
        });
    }
}
