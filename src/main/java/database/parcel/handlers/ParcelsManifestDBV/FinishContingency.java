package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.util.Date;

import static service.commons.Constants.*;

public class FinishContingency extends DBHandler<ParcelsManifestDBV> {

    public FinishContingency(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int parcelManifestId = body.getInteger(_PARCEL_MANIFEST_ID);
            int updatedBy = body.getInteger(UPDATED_BY);

            startTransaction(message, conn -> {
                try {
                    GenericQuery updatePM = this.generateGenericUpdate("parcels_manifest", new JsonObject()
                            .put(ID, parcelManifestId)
                            .put(STATUS, PARCEL_MANIFEST_STATUS.FINISHED.ordinal())
                            .put(UPDATED_BY, updatedBy)
                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                            .put(_CONTINGENCY_FINISH_ROUTE_DATE, UtilsDate.sdfDataBase(new Date()))
                            .put(_FINISH_ROUTE_DATE, UtilsDate.sdfDataBase(new Date())));
                    conn.updateWithParams(updatePM.getQuery(), updatePM.getParams(), replyUpdatePM -> {
                        try {
                            if (replyUpdatePM.failed()) {
                                throw replyUpdatePM.cause();
                            }

                            this.commit(conn, message, new JsonObject().put("finished", true));
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
