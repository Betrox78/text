package service.alliances;

import database.alliances.AllianceServiceDBV;
import service.commons.ServiceVerticle;

public class AllianceServiceSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return AllianceServiceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/allianceServices";
    }
}
