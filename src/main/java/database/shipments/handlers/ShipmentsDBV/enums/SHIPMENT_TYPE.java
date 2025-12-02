package database.shipments.handlers.ShipmentsDBV.enums;

import database.routes.TravelTrackingDBV;

public enum SHIPMENT_TYPE {
    load, download;

    public TravelTrackingDBV.SHIPMENT_TRACKING_STATUS getShipmentTrackingStatus(){
        if (this.equals(load)){
            return TravelTrackingDBV.SHIPMENT_TRACKING_STATUS.LOADED;
        } else {
            return TravelTrackingDBV.SHIPMENT_TRACKING_STATUS.DOWNLOADED;
        }
    }
}
