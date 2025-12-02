package service.boardingpass;

import database.boardingpass.LuggagesDBV;
import service.commons.ServiceVerticle;

public class LuggagesSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return LuggagesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/luggages";
    }
}
