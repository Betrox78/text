package database.parcel.enums;

public enum PARCEL_MANIFEST_ROUTE_LOG_TYPE {
    INIT("init"),
    MARK("mark"),
    DELIVERY("delivery"),
    DELIVERY_ATTEMPT("delivery_attempt"),
    END("end");

    String value;

    PARCEL_MANIFEST_ROUTE_LOG_TYPE(String value) {
        this.value = value;
    }

    public static PARCEL_MANIFEST_ROUTE_LOG_TYPE fromValue(String value) {
        for (PARCEL_MANIFEST_ROUTE_LOG_TYPE parcelManifestRouteLogType : PARCEL_MANIFEST_ROUTE_LOG_TYPE.values()) {
            if (parcelManifestRouteLogType.getValue().equals(value)) {
                return parcelManifestRouteLogType;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }

    public String getValue() {
        return value;
    }

    public boolean isDelivery() {
        return this.equals(DELIVERY) || this.equals(DELIVERY_ATTEMPT);
    }
}
