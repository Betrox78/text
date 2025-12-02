package database.shipments.handlers.ShipmentsDBV.enums;

public enum SHIPMENT_STATUS {
    CANCELED(0),
    OPEN(1),
    CLOSE(2);

    private final int value;

    SHIPMENT_STATUS(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SHIPMENT_STATUS fromValue(int value) {
        for (SHIPMENT_STATUS shipmentStatus : SHIPMENT_STATUS.values()) {
            if (shipmentStatus.getValue() == value) {
                return shipmentStatus;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }
}
