package database.alliances;

import database.commons.DBVerticle;

public class AllianceCityDBV extends DBVerticle {

    private static final String tableName = "alliance_city";

    @Override
    public String getTableName() {
        return tableName;
    }
}
