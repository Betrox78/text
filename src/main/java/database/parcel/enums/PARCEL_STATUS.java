package database.parcel.enums;

public enum PARCEL_STATUS {
    DOCUMENTED,
    IN_TRANSIT,
    DELIVERED_OK,
    DELIVERED_WITH_INCIDENCES,
    CANCELED,
    DELIVERED_PARTIAL,
    DELIVERED_CANCEL,
    LOCATED,
    LOCATED_INCOMPLETE,
    ARRIVED,
    ARRIVED_INCOMPLETE,
    EAD,
    PENDING_COLLECTION,
    COLLECTING,
    COLLECTED,
    MERCHANDISE_NOT_RECEIVED_PARTIAL,
    MERCHANDISE_NOT_RECEIVED_TOTAL;

    public boolean canDeliver() {
        return this.equals(ARRIVED) ||
                this.equals(ARRIVED_INCOMPLETE) ||
                this.equals(DELIVERED_PARTIAL) ||
                this.equals(EAD);
    }

    public boolean wasDelivered() {
        return this.equals(DELIVERED_OK);
    }

    public boolean wasCanceled() {
        return this.equals(CANCELED);
    }

    public boolean wasArrived() {
        return this.equals(ARRIVED);
    }

}
