package service.routes;

import database.routes.TravelIncidencesDBV;
import service.commons.ServiceVerticle;

public class TravelIncidencesSV extends ServiceVerticle {
    @Override
    protected  String getDBAddress() {
        return TravelIncidencesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/travelIncidences";
    }
}
