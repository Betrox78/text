package database.routes.handlers.enums;

public enum TRAVELTRACKING_ACTION {
    REGISTER("register"),
    PAID("paid"),
    MOVE("move"),
    IN_TRANSIT("intransit"),
    LOADED("loaded"),
    DOWNLOADED("downloaded"),
    DELIVERED("delivered"),
    INCIDENCE("incidence"),
    CANCELED("canceled"),
    CLOSED("closed"),
    WAITING("waiting"),
    DRIVING("driving");

    String value;

    TRAVELTRACKING_ACTION(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
