package database.parcel;

import database.commons.DBVerticle;

public class IncidencesDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "incidences";
    }

}
