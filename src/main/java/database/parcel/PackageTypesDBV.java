package database.parcel;

import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static service.commons.Constants.ACTION;

/**
 *
 * @author daliacarlon
 */
public class PackageTypesDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "package_types";
    }

    public static final String ACTION_GET_PACKAGE_TYPES_INHOUSE = "PackageTypesDBV.getPackageTypesInhouse";
    public static final String ACTION_GET_PACKAGE_TYPES_PARCEL = "PackageTypesDBV.getPackageTypesParcel";

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_GET_PACKAGE_TYPES_INHOUSE:
                getPackageTypesInhouse(message);
                break;
            case ACTION_GET_PACKAGE_TYPES_PARCEL:
                getPackageTypesParcel(message);
                break;
        }
    }

    private void getPackageTypesInhouse(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_PACKAGE_TYPES_INHOUSE, reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               message.reply(new JsonArray(reply.result().getRows()));
           } catch (Throwable t) {
               t.printStackTrace();
               reportQueryError(message, t);
           }
        });
    }

    private void getPackageTypesParcel(Message<JsonObject> message) {
        this.dbClient.query(QUERY_GET_PACKAGE_TYPES_PARCEL, reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               message.reply(new JsonArray(reply.result().getRows()));
           } catch (Throwable t) {
               reportQueryError(message, t);
           }
        });
    }

    private static final String QUERY_GET_PACKAGE_TYPES_INHOUSE = "SELECT \n" +
            "   pt.id, \n" +
            "   pt.name, \n" +
            "   pt.package_price_id, \n" +
            "   pp.name_price, \n" +
            "   pm.weight, \n" +
            "   pm.height,\n" +
            "   pm.width,\n" +
            "   pm.length\n" +
            "FROM package_types pt\n" +
            "INNER JOIN package_price pp ON pp.id = pt.package_price_id\n" +
            "   AND pp.shipping_type = 'inhouse'\n" +
            "INNER JOIN package_measures pm ON pm.id = pp.package_measures_id\n" +
            "WHERE pt.status = 1\n" +
            "AND pt.package_price_id IS NOT NULL\n" +
            "AND pt.allowed_inhouse IS TRUE;";

    private static final String QUERY_GET_PACKAGE_TYPES_PARCEL = "SELECT \n" +
            "   pt.id, \n" +
            "   pt.name, \n" +
            "   pt.shipping_type, \n" +
            "   pt.allowed_inhouse \n" +
            "FROM package_types pt\n" +
            "WHERE pt.status = 1;";

}
