package database.parcel.enums;

public enum SHIPMENT_TYPE {
    OCU("OCU"),
    EAD("EAD"),
    RAD_OCU("RAD/OCU"),
    RAD_EAD("RAD/EAD");

    final String value;

    SHIPMENT_TYPE(String value) {
        this.value = value;
    }

    public static SHIPMENT_TYPE fromValue(String value) {
        for (SHIPMENT_TYPE shipmentType : SHIPMENT_TYPE.values()) {
            if (shipmentType.getValue().equals(value)) {
                return shipmentType;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }

    public String getValue() {
        return value;
    }

    public boolean includeRAD() {
        return this.equals(RAD_OCU) || this.equals(RAD_EAD);
    }

    public boolean includeEAD() {
        return this.equals(EAD) || this.equals(RAD_EAD);
    }

    public boolean includeOCU() { return this.equals(OCU) || this.equals(RAD_OCU); }

}
