package database.parcel;

import database.commons.DBVerticle;

public class ParcelsPackagesTrackingDBV  extends DBVerticle {
    @Override
    public String getTableName() {
        return "parcels_packages_tracking";
    }

    public enum ACTION {
        REGISTER,
        PAID,
        MOVE,
        INTRANSIT,
        LOADED,
        DOWNLOADED,
        INCIDENCE,
        CANCELED,
        CLOSED,
        PRINTED,
        DELIVERED,
        DELIVEREDCANCEL,
        LOCATED,
        ARRIVED
    }
}
