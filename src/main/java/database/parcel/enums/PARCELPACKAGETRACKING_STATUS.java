package database.parcel.enums;

public enum PARCELPACKAGETRACKING_STATUS {
    REGISTER("register", "registrado"),
    PAID("paid", "pagado"),
    MOVE("move", "movido"),
    IN_TRANSIT("intransit", "en transito"),
    LOADED("loaded", "embarcado"),
    DOWNLOADED("downloaded", "desembarcado"),
    INCIDENCE("incidence", "incidencia"),
    CANCELED("canceled", "cancelado"),
    CLOSED("closed", "cerrado"),
    PRINTED("printed", "impreso"),
    DELIVERED("delivered", "entregado"),
    DELIVERED_CANCEL("deliveredcancel", "entregado cancelado"),
    LOCATED("located", "en sucursal no destino"),
    ARRIVED("arrived", "en sucursal destino"),
    CREATED_LOG("createdlog", "createdlog"),
    CANCELED_LOG("canceledlog", "canceledlog"),
    EAD("ead", "ead"),
    RAD("rad", "rad"),
    READY_TO_TRANSHIPMENT("ready_to_transhipment", "listo para trasbordar"),
    TRANSHIPPED("transhipped", "trasbordado"),
    DELETED("deleted", "eliminado"),
    PENDING_COLLECTION("pending_collection", "pendiente de recoleccion"),
    COLLECTING("collecting", "recolectando"),
    COLLECTED("collected", "recolectado"),
    IN_ORIGIN("in_origin", "en origen"),
    DELIVERY_ATTEMPT("delivery_attempt", "intento de entrega");

    final String value;
    final String valueES;

    PARCELPACKAGETRACKING_STATUS(String value, String valueES) {
        this.value = value;
        this.valueES = valueES;
    }

    public static PARCELPACKAGETRACKING_STATUS fromValue(String value) {
        for (PARCELPACKAGETRACKING_STATUS parcelpackagetrackingStatus : PARCELPACKAGETRACKING_STATUS.values()) {
            if (parcelpackagetrackingStatus.getValue().equals(value)) {
                return parcelpackagetrackingStatus;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }

    public String getValue() {
        return value;
    }
    public String getValueES() {
        return valueES;
    }

    public boolean validatePackagesLoad(){
        return this.value.equals(LOADED.getValue())
                || this.value.equals(IN_TRANSIT.getValue())
                || this.value.equals(CLOSED.getValue())
                || this.value.equals(CANCELED.getValue())
                || this.value.equals(ARRIVED.getValue())
                || this.value.equals(EAD.getValue())
                || this.value.equals(RAD.getValue())
                || this.value.equals(DELIVERED.getValue())
                || this.value.equals(PENDING_COLLECTION.getValue())
                || this.value.equals(COLLECTING.getValue())
                || this.value.equals(COLLECTED.getValue());
    }

    public boolean notValidArrivalContingency(){
        return this.value.equals(READY_TO_TRANSHIPMENT.getValue())
                || this.value.equals(CLOSED.getValue())
                || this.value.equals(CANCELED.getValue())
                || this.value.equals(ARRIVED.getValue())
                || this.value.equals(DELIVERED.getValue());
    }

    public boolean isCanceled(){
        return this.value.equals(CANCELED.getValue());
    }
    public boolean canDeliver(){
        return this.value.equals(MOVE.getValue())
                || this.value.equals(DOWNLOADED.getValue())
                || this.value.equals(ARRIVED.getValue())
                || this.value.equals(LOCATED.getValue());
    }
}
