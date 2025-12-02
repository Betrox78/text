package database.shipments.handlers.ShipmentsDBV.enums;

public enum SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS {
    LOADED("loaded"),
    READY_TO_GO("ready-to-go"),
    IN_TRANSIT("in-transit"),
    ARRIVED_TO_TERMINAL("arrived-to-terminal"),
    DOWNLOADED("downloaded"),
    TRANSFER("transfer");

    private final String value;

    SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS fromValue(String value) {
        for (SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS shipmentParcelPackageTrackingStatus : SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS.values()) {
            if (shipmentParcelPackageTrackingStatus.getValue().equals(value)) {
                return shipmentParcelPackageTrackingStatus;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }
}
