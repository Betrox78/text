package database.routes.handlers.enums;

public enum DESTINATION_STATUS {
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

    DESTINATION_STATUS(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static DESTINATION_STATUS fromValue(String value) {
        for (DESTINATION_STATUS scheduleStatus : DESTINATION_STATUS.values()) {
            if (scheduleStatus.getValue().equals(value)) {
                return scheduleStatus;
            }
        }
        throw new IllegalArgumentException("Unknow value: " + value);
    }
}
