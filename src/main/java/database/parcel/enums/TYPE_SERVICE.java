package database.parcel.enums;

public enum TYPE_SERVICE {
    EAD("EAD"),
    RAD_OCU("RAD/OCU"),
    RAD_EAD("RAD/EAD");

    final String value;

    TYPE_SERVICE(String value) {
        this.value = value;
    }

    public static TYPE_SERVICE fromValue(String value) {
        for (TYPE_SERVICE shipmentType : TYPE_SERVICE.values()) {
            if (shipmentType.getValue().equals(value)) {
                return shipmentType;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }

    public String getValue() {
        return value;
    }

}
