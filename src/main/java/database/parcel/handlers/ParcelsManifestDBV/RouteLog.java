package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PARCEL_MANIFEST_ROUTE_LOG_TYPE;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static service.commons.Constants.*;

public class RouteLog extends DBHandler<ParcelsManifestDBV> {

    public RouteLog(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            Integer parcelManifestDetailId = body.getInteger(_PARCEL_MANIFEST_DETAIL_ID);
            Double speed = body.getDouble(_SPEED, 0.0);
            Double latitude = body.getDouble(_LATITUDE, 0.0);
            Double longitude = body.getDouble(_LONGITUDE, 0.0);
            PARCEL_MANIFEST_ROUTE_LOG_TYPE type = PARCEL_MANIFEST_ROUTE_LOG_TYPE.fromValue(body.getString(_TYPE, "mark"));
            Integer createdBy = body.getInteger(CREATED_BY);

            if (type.isDelivery() && Objects.isNull(parcelManifestDetailId)) {
                throw new Exception("parcel_manifest_detail_id cannot be null");
            }

            startTransaction(message, conn -> {
                try {
                    JsonObject bodyInsert = new JsonObject()
                            .put(_PARCEL_MANIFEST_ID, parcelManifestId)
                            .put(_SPEED, speed)
                            .put(_LATITUDE, latitude)
                            .put(_LONGITUDE, longitude)
                            .put(_TYPE, type.getValue())
                            .put(CREATED_AT, UtilsDate.sdfDataBase(new Date()))
                            .put(CREATED_BY, createdBy);
                    if (Objects.nonNull(parcelManifestDetailId)) {
                        bodyInsert.put(_PARCEL_MANIFEST_DETAIL_ID, parcelManifestDetailId);
                    }

                    GenericQuery insert = this.generateGenericCreate("parcels_manifest_route_logs", bodyInsert);
                    conn.updateWithParams(insert.getQuery(), insert.getParams(), reply -> {
                       try {
                           if(reply.failed()) {
                               throw reply.cause();
                           }
                           this.commit(conn, message, new JsonObject().put("created", true));
                       } catch (Throwable t) {
                           this.rollback(conn, t, message);
                       }
                    });

                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

}
