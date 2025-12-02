package database.parcel.handlers.ParcelDBV.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

@Getter
public class TrackingMessage {
    String title;
    String description;

    @JsonProperty("first_note")
    String firstNote;

    @JsonProperty("second_note")
    String secondNote;

    public TrackingMessage(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public TrackingMessage(String title, String description, String firstNote) {
        this.title = title;
        this.description = description;
        this.firstNote = firstNote;
    }

    public TrackingMessage(String title, String description, String firstNote, String secondNote) {
        this.title = title;
        this.description = description;
        this.firstNote = firstNote;
        this.secondNote = secondNote;
    }

    public JsonObject toJsonObject() {
        return JsonObject.mapFrom(this);
    }

}
