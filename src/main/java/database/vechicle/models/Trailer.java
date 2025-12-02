package database.vechicle.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

public class Trailer {
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("economic_number")
    private String economicNumber;
    @JsonProperty("serial_number")
    private String serialNumber;
    @JsonProperty("c_SubTipoRem_id")
    private String cSubTipoRemId;
    @JsonProperty("plate")
    private String plate;
    @JsonProperty("in_use")
    private Boolean inUse;
    private Integer status;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("created_by")
    private Integer createdBy;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("updated_by")
    private Integer updatedBy;
    @JsonProperty("current_route")
    private JsonObject currentRoute;

    public Trailer() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEconomicNumber() { return economicNumber; }

    public void setEconomicNumber(String economicNumber) { this.economicNumber = economicNumber; }

    public String getSerialNumber() { return serialNumber; }

    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getcSubTipoRemId() {
        return cSubTipoRemId;
    }

    public void setcSubTipoRemId(String cSubTipoRemId) {
        this.cSubTipoRemId = cSubTipoRemId;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public Boolean getInUse() {
        return inUse;
    }

    public void setInUse(Boolean in_use) {
        this.inUse = in_use;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public JsonObject getCurrentRoute() { return currentRoute; }

    public void setCurrentRoute(JsonObject currentRoute) { this.currentRoute = currentRoute; }

}
