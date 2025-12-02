package service.alliances;

import database.alliances.AllianceCityDBV;
import service.commons.ServiceVerticle;

public class AllianceCitySV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return AllianceCityDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/allianceCities";
    }
}
