package database.routes;

import database.commons.DBVerticle;

public class ChecklistVansDBV  extends DBVerticle {

    @Override
    public String getTableName() {
        return "checklist_vans";
    }

}
