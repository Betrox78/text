package service.routes;

import database.routes.ChecklistVansDBV;
import service.commons.ServiceVerticle;

public class ChecklistVansSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ChecklistVansDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/checklistVans";
    }
}
