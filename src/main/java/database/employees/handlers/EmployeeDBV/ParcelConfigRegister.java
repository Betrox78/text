package database.employees.handlers.EmployeeDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.employees.EmployeeDBV;
import database.employees.handlers.EmployeeDBV.models.ParcelsInitConfig;
import database.parcel.enums.SHIPMENT_TYPE;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import database.promos.handlers.PromosDBV.models.ParcelPackage;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.promos.PromosDBV.ACTION_CALCULATE_MULTIPLE_PROMO;
import static database.promos.PromosDBV.SERVICE;
import static service.commons.Constants.*;
import static utils.UtilsValidation.*;

public class ParcelConfigRegister extends DBHandler<EmployeeDBV> {

    public ParcelConfigRegister(EmployeeDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            GenericQuery insert = this.generateGenericCreate("parcels_init_config", body);
            startTransaction(message, conn -> {
                try {
                    conn.updateWithParams(insert.getQuery(), insert.getParams(), reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }
                            this.commit(conn, message, new JsonObject().put(ID, reply.result().getKeys().getInteger(0)));
                        } catch (Throwable t) {
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

}
