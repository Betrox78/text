package database.parcel.handlers.CodesManager;

import database.commons.DBHandler;
import database.commons.ErrorCodes;
import database.parcel.ParcelDBV;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static database.alliances.AllianceDBV.LIMIT;
import static database.alliances.AllianceDBV.PAGE;

public class SearchCodes extends DBHandler<ParcelDBV> {
    private static final Integer MAX_LIMIT = 30;

    public static final String ACTION = "ParcelDBV.SearchCodes";

    public SearchCodes(ParcelDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String query = QUERY_GET_CODES_FROM;

        JsonArray params = new JsonArray()
                .add(body.getInteger("customer_id"))
                .add(body.getInteger("filter"));
        // Copy query to count
        String queryCount = "SELECT COUNT(DISTINCT pp.id) AS items ".concat(query);
        JsonArray paramsCount = params.copy();
        String queryItems = QUERY_SELECT_CODES.concat(query);

        // Add the limit for pagination
        int page = Integer.parseInt(String.valueOf(body.getInteger(PAGE, 1)));
        int limit = Integer.parseInt(String.valueOf(body.getInteger(LIMIT, Integer.valueOf(MAX_LIMIT.toString()))));
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        int skip = limit * (page-1);
        queryItems = queryItems.concat(" LIMIT ?,? ");
        params.add(skip).add(limit);

        String finalQueryItems = queryItems;

        this.dbClient.queryWithParams(queryCount, paramsCount, replyCount -> {
           try{
               if (replyCount.failed()) {
                   throw replyCount.cause();
               }
               this.dbClient.queryWithParams(finalQueryItems, params, handler->{
                   try{
                       if (handler.failed()){
                           throw handler.cause();
                       }
                       Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                       List<JsonObject> items = handler.result().getRows();
                       JsonObject result = new JsonObject()
                               .put("count", count)
                               .put("items", items.size())
                               .put("results", items);
                       replyResult(message, result);
                   } catch (Throwable t){
                       reportQueryError(message, t);
                   }
               });

           } catch (Throwable ex) {
               replyError(message, ex);
           }
        });
    }
    private static final String QUERY_GET_CODES_FROM = "\tFROM parcels_prepaid_detail AS pp\n" +
            "\tINNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "\tINNER JOIN parcels AS pa ON pa.id = pp.parcel_id\n" +
            "\tINNER JOIN pp_price_km AS pkm ON  pkm.id = pp.price_km_id\n" +
            "\tWHERE p.customer_id=? AND pp.status = ? ";
    private static final String QUERY_SELECT_CODES = "SELECT pp.id, pp.guiapp_code AS tracking_code, p.shipment_type, CONCAT(pkm.min_km, ' HASTA ', pkm.max_km, ' KM.') as rango, pa.parcel_tracking_code,\n" +
            "\tCASE\n" +
            "\t    WHEN pp.status = 0 THEN \"Vigente\"\n" +
            "\t    WHEN pp.status = 1 THEN \"Cancelada\"\n" +
            "\t    ELSE \"Expirada\"\n" +
            "\tEND AS estatus,\n" +
            "\tCONCAT(pa.addressee_name, ' ', pa.addressee_last_name  ) AS recibe,\n" +
            "\tCONCAT(pa.sender_name, ' ', pa.sender_last_name  ) AS envia \n" ;
    private static final String QUERY_GET_CODES_GUIASPP = "SELECT pp.id, pp.guiapp_code AS tracking_code, p.shipment_type, CONCAT(pkm.min_km, ' HASTA ', pkm.max_km, ' KM.') as rango, pa.parcel_tracking_code,\n" +
            "\tCASE\n" +
            "\t    WHEN pp.status = 1 THEN \"Vigente\"\n" +
            "\t    WHEN pp.status = 0 THEN \"Cancelada\"\n" +
            "\t    ELSE \"Expirada\"\n" +
            "\tEND AS estatus,\n" +
            "\tCONCAT(pa.addressee_name, ' ', pa.addressee_last_name  ) AS recibe,\n" +
            "\tCONCAT(pa.sender_name, ' ', pa.sender_last_name  ) AS envia\n" +
            "\tFROM parcels_prepaid_detail AS pp\n" +
            "\tINNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "\tINNER JOIN parcels AS pa ON pa.id = pp.parcel_id\n" +
            "\tINNER JOIN pp_price_km AS pkm ON  pkm.id = pp.price_km_id\n" +
            "\tWHERE  pp.status = ? LIMIT 5;";




    /**
     * This methos has the behaivor for reporting an error in a query execute
     *
     * @param message the message from the eventu bus
     * @param cause the exception giving problems
     */
    /*protected final void reportQueryError(Message<JsonObject> message, Throwable cause) {
        try {
            JsonObject catchedError = null;
            if (cause.getMessage().contains("foreign key")) { //a foreign key constraint fails
                Pattern p = Pattern.compile("`(.+?)`");
                Matcher m = p.matcher(cause.getMessage());
                List<String> incidencias = new ArrayList<>(5);
                while (m.find()) {
                    incidencias.add(m.group(1));
                }
                catchedError = new JsonObject().put("name", incidencias.get(incidencias.size() - 3)).put("error", "does not exist in the catalog");
            }
            if (cause.getMessage().contains("Data too long")) { //data to long for column
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", "to long data");
            }
            if (cause.getMessage().contains("Unknown column")) {//unkown column
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", UtilsValidation.PARAMETER_DOES_NOT_EXIST);
            }
            if (cause.getMessage().contains("doesn't have a default")) { //not default value in not null
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", UtilsValidation.MISSING_REQUIRED_VALUE);
            }
            if (cause.getMessage().contains("Duplicate entry")) { //already exist (duplicate key for unique values)
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String value = m.group(1);
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", "value: " + value + " in " + UtilsValidation.ALREADY_EXISTS);
            }
            if (cause.getMessage().contains("Data truncation")) {
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", "to long data");
            }
            if (catchedError != null) {
                message.reply(catchedError, new DeliveryOptions().addHeader(ErrorCodes.DB_ERROR.toString(), catchedError.getString("error")));
            } else {
                message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            message.fail(500, ex.getMessage());
        }

    }*/
}
