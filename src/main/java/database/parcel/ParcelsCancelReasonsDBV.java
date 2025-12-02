package database.parcel;

import database.commons.DBVerticle;

/**
 *
 * @author daliacarlon
 */
public class ParcelsCancelReasonsDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "parcels_cancel_reasons";
    }

}
