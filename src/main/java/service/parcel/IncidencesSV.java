package service.parcel;

import database.parcel.IncidencesDBV;
import service.commons.ServiceVerticle;

public class IncidencesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return IncidencesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/incidences";
    }

}
