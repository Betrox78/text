/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.geo;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static service.commons.Constants.ACTION;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class SuburbDBV extends DBVerticle {

    public static final String ACTION_ZIP_CODE_SEARCH = "SuburbDBV.zipCodeSearch";
    public static final String ACTION_ZIP_CODE_SEARCH_SAT = "SuburbDBV.zipCodeSearchSAT";

    @Override
    public String getTableName() {
        return "suburb";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_ZIP_CODE_SEARCH:
                this.zipCodeSearch(message);
                break;
            case ACTION_ZIP_CODE_SEARCH_SAT:
                this.zipCodeSearchSAT(message);
                break;
        }

    }

    private void zipCodeSearch(Message<JsonObject> message) {
        JsonArray params = new JsonArray().add(message.body().getString("zipCode"));
        dbClient.queryWithParams(ZIP_CODE_SEARCH, params, reply -> {
            if (reply.succeeded()) {
                message.reply(new JsonArray(reply.result().getRows()));
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void zipCodeSearchSAT(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String name_sat = body.getString("name_sat");
            String county_name = body.getString("county_name");
            String city_name = body.getString("city_name");
            Integer zipcode = body.getInteger("zipcode");
            JsonArray params = new JsonArray()
                    .add(name_sat)
                    .add(county_name)
                    .add(name_sat)
                    .add(city_name)
                    .add(zipcode);
            dbClient.queryWithParams(ZIP_CODE_SEARCH_SAT, params, reply -> {
                try {
                    if (reply.succeeded()) {
                        JsonObject result = reply.result().getRows().get(0);
                        message.reply(result);
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                }catch ( Throwable t) {
                    reportQueryError(message, t);
                }

            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }

    }

    private static final String ZIP_CODE_SEARCH = "SELECT suburb.id AS suburb_id, suburb.name AS suburb_name,\n" +
            "suburb.suburb_type, suburb.external,\n" +
            "city.id AS city_id, city.name AS city_name,\n" +
            "county.id AS county_id, county.name AS county_name,\n" +
            "state.id AS state_id, state.name AS state_name,\n" +
            "country.id AS country_id, country.name AS country_name\n" +
            "FROM suburb\n" +
            "JOIN county ON county.id = suburb.county_id\n" +
            "LEFT JOIN city ON county.id = city.county_id\n" +
            "JOIN state ON state.id = county.state_id\n" +
            "JOIN country ON country.id = state.country_id\n" +
            "WHERE suburb.zip_code = ?;";

    private static final String ZIP_CODE_SEARCH_SAT = " select \n" +
            "            co.c_Colonia as  Colonia,\n" +
            "            lo.c_Localidad as c_Localidad,\n" +
            "            mu.c_Estado as c_Estado,\n" +
            "            mu.c_Municipio as c_Municipio\n" +
            "            from c_Colonia as co\n" +
            "            left join c_Municipio as  mu ON mu.c_Estado = ? AND mu.description like ? \n" +
            "            left join c_Localidad as lo ON lo.c_Estado = ? AND lo.descripcion like ?\n" +
            "            where co.c_CodigoPostal = ? ";

}
