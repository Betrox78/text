package database.routes.handlers.enums;

public enum TRAVELTRACKING_STATUS {
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

    String value;

    TRAVELTRACKING_STATUS(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
