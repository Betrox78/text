/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.customers;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

/**
 *
 * @author Saul
 */
public class CustomerAddressesDBV extends DBVerticle {
    
    public static final String GET_BY_CUSTOMERID = "CustomerAddressesDBV.findByCustomerId";
    
    @Override
    public String getTableName() {
        return "customer_addresses";
    }
    
    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case GET_BY_CUSTOMERID:
                this.findByCustomerId(message);
                break;
        }
    }

    private void findByCustomerId(Message<JsonObject> message) {
        try {
            JsonArray params = new JsonArray().add(message.body().getInteger("customer_id"));
            dbClient.queryWithParams(QUERY_GET_ADDRESSES, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(null);
                    } else {
                        message.reply(new JsonArray(result));
                    }
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }
    
    private static final String QUERY_GET_ADDRESSES = "SELECT " +
            "customer_addresses.id , " +
            "customer_addresses.customer_id, " +
            "customer_addresses.address, " +
            "customer_addresses.street_id, " +
            "street.name as 'street_name', " +
            "customer_addresses.no_ext, " +
            "customer_addresses.no_int, " +
            "customer_addresses.suburb_id, " +
            "suburb.name as 'suburb_name', " +
            "customer_addresses.city_id, " +
            "city.name as 'city_name', " +
            "customer_addresses.county_id, " +
            "county.name as 'county_name', " +
            "customer_addresses.state_id, " +
            "state.name as 'state_name', " +
            "customer_addresses.country_id, " +
            "country.name as 'country_name', " +
            "customer_addresses.reference, " +
            "customer_addresses.zip_code, " +
            "customer_addresses.created_at, " +
            "customer_addresses.created_by, " +
            "customer_addresses.updated_at, " +
            "customer_addresses.updated_by, " +
            "customer_addresses.latitud, " +
            "customer_addresses.longitud " +
            "FROM customer_addresses " +
            "left join city on city.id = customer_addresses.city_id " +
            "left join country on country.id = customer_addresses.country_id " +
            "left join county on county.id = customer_addresses.county_id " +
            "left join state on state.id = customer_addresses.state_id " +
            "left join street on street.id = customer_addresses.street_id " +
            "left join suburb on suburb.id = customer_addresses.suburb_id " +
            "where customer_addresses.customer_id = ? and customer_addresses.status = 1 ";

}
