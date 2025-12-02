package database.alliances;

import database.commons.DBVerticle;

public class AllianceServiceDBV extends DBVerticle {
    /**
     * Need to especifie the name of the entity table, to refer in the properties file, the actions names and queries
     *
     * @return the name of the table to manage in this verticle
     */
    @Override
    public String getTableName() {
        return "alliance_service";
    }
}
