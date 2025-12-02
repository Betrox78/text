package database.parcel.handlers.ParcelsPackagesDBV.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PackageIncidence {

    @JsonProperty("incidence_id")
    Integer incidenceId;

    String notes;

    public PackageIncidence() {
    }

    public Integer getIncidenceId() {
        return incidenceId;
    }

    public void setIncidenceId(Integer incidenceId) {
        this.incidenceId = incidenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
