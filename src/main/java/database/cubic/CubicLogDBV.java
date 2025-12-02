package database.cubic;

import database.commons.DBVerticle;

public class CubicLogDBV extends  DBVerticle {
    /**
     * Need to especifie the name of the entity table, to refer in the properties file, the actions names and queries
     *
     * @return the name of the table to manage in this verticle
     */
    @Override
    public String getTableName() {
        return "cubic_log";
    }
}
