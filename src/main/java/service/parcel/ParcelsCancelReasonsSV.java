package service.parcel;

import database.parcel.ParcelsCancelReasonsDBV;
import service.commons.ServiceVerticle;

public class ParcelsCancelReasonsSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ParcelsCancelReasonsDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/parcelsCancelReasons";
    }
}
