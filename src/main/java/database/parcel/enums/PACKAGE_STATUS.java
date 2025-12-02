package database.parcel.enums;

public enum PACKAGE_STATUS {
    DOCUMENTED,
    IN_TRANSIT,
    DELIVERED,
    CLOSED,
    CANCELED,
    LOADED,
    DOWNLOADED,
    DELIVERED_CANCEL,
    LOCATED,
    ARRIVED,
    READY_TO_TRANSHIPMENT,
    PENDING_COLLECTION,
    COLLECTING,
    COLLECTED,
    MERCHANDISE_NOT_RECEIVED,
    EAD;

    public boolean notApplyArrivedCount() {
        return this.equals(DOCUMENTED) || this.equals(IN_TRANSIT) || this.equals(EAD)
                || this.equals(CLOSED) || this.equals(LOADED)
                || this.equals(READY_TO_TRANSHIPMENT) || this.equals(PENDING_COLLECTION)
                || this.equals(COLLECTING) || this.equals(COLLECTED) || this.equals(MERCHANDISE_NOT_RECEIVED);
    }

    public boolean wasLocated() {
        return this.equals(LOCATED);
    }

    public boolean wasDeliveredOrArrived() {
        return this.equals(DELIVERED) || this.equals(ARRIVED);
    }

    public boolean wasDownloaded() {
        return this.equals(DOWNLOADED);
    }

    public boolean canBeDocumented() {
        return this.equals(PENDING_COLLECTION) ||
                this.equals(COLLECTING) ||
                this.equals(COLLECTED);
    }

    public boolean canBeDownloaded() {
        return !this.equals(CANCELED) &&
                !this.equals(DELIVERED_CANCEL) &&
                !this.equals(DELIVERED) &&
                !this.equals(DOWNLOADED) &&
                !this.equals(READY_TO_TRANSHIPMENT) &&
                !this.equals(EAD) &&
                !this.equals(ARRIVED);
    }
}