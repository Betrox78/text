package database.shipments.handlers.ShipmentsDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import database.shipments.handlers.ShipmentsDBV.enums.ORIGIN;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_STATUS;
import database.shipments.handlers.ShipmentsDBV.enums.SHIPMENT_TYPE;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Shipment {
    private Integer id;
    @JsonProperty("schedule_route_id")
    private Integer scheduleRouteId;
    @JsonProperty("terminal_id")
    private Integer terminalId;
    @JsonProperty("shipment_type")
    private SHIPMENT_TYPE shipmentType;
    @JsonProperty("shipment_status")
    private SHIPMENT_STATUS shipmentStatus;
    @JsonProperty("driver_id")
    private Integer driverId;
    @JsonProperty("left_stamp")
    private String leftStamp;
    @JsonProperty("right_stamp")
    private String rightStamp;
    @JsonProperty("additional_stamp")
    private String additionalStamp;
    @JsonProperty("replacement_stamp")
    private String replacementStamp;
    @JsonProperty("total_tickets")
    private Integer totalTickets;
    @JsonProperty("total_complements")
    private Integer totalComplements;
    @JsonProperty("total_parcels")
    private Integer totalParcels;
    @JsonProperty("total_packages")
    private Integer totalPackages;
    @JsonProperty("parent_id")
    private Integer parentId;
    @JsonProperty("left_stamp_status")
    private Integer leftStampStatus;
    @JsonProperty("right_stamp_status")
    private Integer rightStampStatus;
    @JsonProperty("additional_stamp_status")
    private Integer additionalStampStatus;
    @JsonProperty("replacement_stamp_status")
    private Integer replacementStampStatus;
    private Integer status;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("created_by")
    private Integer createdBy;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("updated_by")
    private Integer updatedBy;
    @JsonProperty("origin")
    private ORIGIN origin;

    public Shipment() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getScheduleRouteId() {
        return scheduleRouteId;
    }

    public void setScheduleRouteId(Integer scheduleRouteId) {
        this.scheduleRouteId = scheduleRouteId;
    }

    public Integer getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(Integer terminalId) {
        this.terminalId = terminalId;
    }

    public SHIPMENT_TYPE getShipmentType() {
        return shipmentType;
    }

    public void setShipmentType(SHIPMENT_TYPE shipmentType) {
        this.shipmentType = shipmentType;
    }

    public SHIPMENT_STATUS getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(SHIPMENT_STATUS shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public String getLeftStamp() {
        return leftStamp;
    }

    public void setLeftStamp(String leftStamp) {
        this.leftStamp = leftStamp;
    }

    public String getRightStamp() {
        return rightStamp;
    }

    public void setRightStamp(String rightStamp) {
        this.rightStamp = rightStamp;
    }

    public String getAdditionalStamp() {
        return additionalStamp;
    }

    public void setAdditionalStamp(String additionalStamp) {
        this.additionalStamp = additionalStamp;
    }

    public String getReplacementStamp() {
        return replacementStamp;
    }

    public void setReplacementStamp(String replacementStamp) {
        this.replacementStamp = replacementStamp;
    }

    public Integer getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(Integer totalTickets) {
        this.totalTickets = totalTickets;
    }

    public Integer getTotalComplements() {
        return totalComplements;
    }

    public void setTotalComplements(Integer totalComplements) {
        this.totalComplements = totalComplements;
    }

    public Integer getTotalParcels() {
        return totalParcels;
    }

    public void setTotalParcels(Integer totalParcels) {
        this.totalParcels = totalParcels;
    }

    public Integer getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(Integer totalPackages) {
        this.totalPackages = totalPackages;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getLeftStampStatus() {
        return leftStampStatus;
    }

    public void setLeftStampStatus(Integer leftStampStatus) {
        this.leftStampStatus = leftStampStatus;
    }

    public Integer getRightStampStatus() {
        return rightStampStatus;
    }

    public void setRightStampStatus(Integer rightStampStatus) {
        this.rightStampStatus = rightStampStatus;
    }

    public Integer getAdditionalStampStatus() {
        return additionalStampStatus;
    }

    public void setAdditionalStampStatus(Integer additionalStampStatus) {
        this.additionalStampStatus = additionalStampStatus;
    }

    public Integer getReplacementStampStatus() {
        return replacementStampStatus;
    }

    public void setReplacementStampStatus(Integer replacementStampStatus) {
        this.replacementStampStatus = replacementStampStatus;
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

    public ORIGIN getOrigin() {
        return origin;
    }

    public void setOrigin(ORIGIN origin) {
        this.origin = origin;
    }
}
