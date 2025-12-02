package service.parcel;

import database.parcel.ParcelsPackingsDBV;
import service.commons.ServiceVerticle;

public class ParcelsPackingsSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ParcelsPackingsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcelsPackings";
    }
}
