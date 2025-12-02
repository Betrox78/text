package database.routes;

import database.commons.DBVerticle;

public class TravelIncidencesDBV extends DBVerticle {
    @Override
    public String getTableName() {
        return "travel_incidences";
    }
}
