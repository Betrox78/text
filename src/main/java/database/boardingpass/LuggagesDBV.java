package database.boardingpass;

import database.commons.DBVerticle;

public class LuggagesDBV extends DBVerticle {
    @Override
    public String getTableName() {
        return "luggages";
    }
}
