package database.routes.handlers.enums;

import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_PARCEL_PACKAGE_TRACKING_STATUS;

public enum SCHEDULE_STATUS {
    SCHEDULED("scheduled"),
    LOADING("loading"),
    READY_TO_GO("ready-to-go"),
    IN_TRANSIT("in-transit"),
    STOPPED("stopped"),
    DOWNLOADING("downloading"),
    READY_TO_LOAD("ready-to-load"),
    PAUSED("paused"),
    FINISHED_OK("finished-ok"),
    FINISHED("finished");

    final String value;

    SCHEDULE_STATUS(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static SCHEDULE_STATUS fromValue(String value) {
        for (SCHEDULE_STATUS scheduleStatus : SCHEDULE_STATUS.values()) {
            if (scheduleStatus.getValue().equals(value)) {
                return scheduleStatus;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }
}
