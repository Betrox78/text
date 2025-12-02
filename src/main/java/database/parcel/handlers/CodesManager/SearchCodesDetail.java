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

public class SearchCodesDetail extends DBHandler<ParcelDBV> {


    public static final String ACTION = "ParcelDBV.SearchCodesDetail";

    public SearchCodesDetail(ParcelDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray().add(body.getInteger("parcel_id"));

        this.dbClient.queryWithParams(QUERY_DETAIL_GUIA, params, handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }
                if(handler.result().getNumRows()>0){
                    JsonObject result = new JsonObject();
                    result.put("result", handler.result().getRows());
                    message.reply(result);
                }else{
                    message.reply(null);
                }
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private static final String QUERY_GET_CODES_GUIASPP_DETAIL = "select p.id, p.guiapp_code, p.price\n" +
            "\t\tFROM parcels_prepaid_detail AS p\n" +
            "\t\tWHERE p.id = ? ;\n" +
            "\t\t;\n" +
            "\n";

    private static final String QUERY_DETAIL_GUIA = "select  ppd.id id_ppd, pp.tracking_code, ppd.guiapp_code,\n" +
            "DATE(pp.created_at) as created_at,\n" +
            "DATE_FORMAT(pp.created_at, \"%H:%i\") as hour,\n" +
            "CONCAT(pkm.min_km, '-', pkm.max_km) as rango,\n" +
            "pprice.name_price as tarifa ,\n" +
            "CONCAT(pprice.min_weight , '-' , pprice.max_weight) as peso,\n" +
            "concat(pprice.min_linear_volume) as m3,\n" +
            "pp.shipment_type,\n" +
            " pp.payment_condition,\n" +
            " CONCAT(c.first_name, ' ', c.last_name) AS name,\n" +
            " c.phone,\n" +
            " ventaBranch.name as 'sucursal_venta'\n" +
            "            from parcels_prepaid_detail ppd\n" +
            "            INNER JOIN parcels_prepaid AS pp ON pp.id = ppd.parcel_prepaid_id\n" +
            "            INNER JOIN pp_price_km AS pkm ON  pkm.id = ppd.price_km_id\n" +
            "            inner join pp_price as pprice ON pprice.id  = ppd.price_id\n" +
            "            INNER JOIN users AS u ON pp.crated_by = u.id\n" +
            "            INNER JOIN customer AS c ON c.id = pp.customer_id\n" +
            "            left join branchoffice as ventaBranch ON ventaBranch.id = pp.branchoffice_id\n" +
            "            WHERE ppd.id = ?;";



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
