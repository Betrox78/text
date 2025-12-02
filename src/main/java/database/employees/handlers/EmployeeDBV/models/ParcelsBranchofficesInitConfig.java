package database.employees.handlers.EmployeeDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParcelsBranchofficesInitConfig {

    @JsonProperty("id")
    private int id;

    @JsonProperty("employee_id")
    private int employeeId;

    @JsonProperty("branchoffice_id")
    private Integer branchofficeId;

    @JsonProperty("sender_id")
    private Integer senderId;

    @JsonProperty("sender_zip_code")
    private Integer senderZipCode;

    @JsonProperty("addressee_id")
    private Integer addresseeId;

    @JsonProperty("addressee_zip_code")
    private Integer addresseeZipCode;

    @JsonProperty("status")
    private byte status;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("created_by")
    private int createdBy;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("updated_by")
    private Integer updatedBy;

    public ParcelsBranchofficesInitConfig() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getBranchofficeId() {
        return branchofficeId;
    }

    public void setBranchofficeId(Integer branchofficeId) {
        this.branchofficeId = branchofficeId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public Integer getSenderZipCode() {
        return senderZipCode;
    }

    public void setSenderZipCode(Integer senderZipCode) {
        this.senderZipCode = senderZipCode;
    }

    public Integer getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(Integer addresseeId) {
        this.addresseeId = addresseeId;
    }

    public Integer getAddresseeZipCode() {
        return addresseeZipCode;
    }

    public void setAddresseeZipCode(Integer addresseeZipCode) {
        this.addresseeZipCode = addresseeZipCode;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }
}
