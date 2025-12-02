package database.parcel;

import database.commons.DBVerticle;

public class ParcelsPackingsDBV  extends DBVerticle {

    @Override
    public String getTableName() {
        return "parcels_packings";
    }

}
