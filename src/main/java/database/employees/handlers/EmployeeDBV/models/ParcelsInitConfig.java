package database.employees.handlers.EmployeeDBV.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

public class ParcelsInitConfig {

    @JsonProperty("id")
    private int id;

    @JsonProperty("employee_id")
    private int employeeId;

    @JsonProperty("terminal_origin_id")
    private Integer terminalOriginId;

    @JsonProperty("enable_terminal_origin_id")
    private boolean enableTerminalOriginId;

    @JsonProperty("shipment_type")
    private String shipmentType;

    @JsonProperty("enable_is_rad")
    private boolean enableIsRad;

    @JsonProperty("enable_is_ead")
    private boolean enableIsEad;

    @JsonProperty("sender_id")
    private Integer senderId;

    @JsonProperty("enable_sender_id")
    private boolean enableSenderId;

    @JsonProperty("sender_zip_code")
    private Integer senderZipCode;

    @JsonProperty("enable_sender_zip_code")
    private boolean enableSenderZipCode;

    @JsonProperty("terminal_destiny_id")
    private Integer terminalDestinyId;

    @JsonProperty("enable_terminal_destiny_id")
    private boolean enableTerminalDestinyId;

    @JsonProperty("addressee_id")
    private Integer addresseeId;

    @JsonProperty("enable_addressee_id")
    private boolean enableAddresseeId;

    @JsonProperty("addressee_zip_code")
    private Integer addresseeZipCode;

    @JsonProperty("enable_addressee_zip_code")
    private boolean enableAddresseeZipCode;

    @JsonProperty("pays_sender")
    private boolean paysSender;

    @JsonProperty("enable_pays_sender")
    private boolean enablePaysSender;

    @JsonProperty("is_credit")
    private boolean isCredit;

    @JsonProperty("enable_is_credit")
    private boolean enableIsCredit;

    @JsonProperty("is_internal_parcel")
    private boolean isInternalParcel;

    @JsonProperty("enable_is_internal_parcel")
    private boolean enableIsInternalParcel;

    @JsonProperty("send_whatsapp_notification")
    private boolean sendWhatsappNotification;

    @JsonProperty("enable_send_whatsapp_notification")
    private boolean enableSendWhatsappNotification;

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

    public ParcelsInitConfig() {
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

    public Integer getTerminalOriginId() {
        return terminalOriginId;
    }

    public void setTerminalOriginId(Integer terminalOriginId) {
        this.terminalOriginId = terminalOriginId;
    }

    public boolean isEnableTerminalOriginId() {
        return enableTerminalOriginId;
    }

    public void setEnableTerminalOriginId(boolean enableTerminalOriginId) {
        this.enableTerminalOriginId = enableTerminalOriginId;
    }

    public String getShipmentType() {
        return shipmentType;
    }

    public void setShipmentType(String shipmentType) {
        this.shipmentType = shipmentType;
    }

    public boolean isEnableIsRad() {
        return enableIsRad;
    }

    public void setEnableIsRad(boolean enableIsRad) {
        this.enableIsRad = enableIsRad;
    }

    public boolean isEnableIsEad() {
        return enableIsEad;
    }

    public void setEnableIsEad(boolean enableIsEad) {
        this.enableIsEad = enableIsEad;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public boolean isEnableSenderId() {
        return enableSenderId;
    }

    public void setEnableSenderId(boolean enableSenderId) {
        this.enableSenderId = enableSenderId;
    }

    public Integer getSenderZipCode() {
        return senderZipCode;
    }

    public void setSenderZipCode(Integer senderZipCode) {
        this.senderZipCode = senderZipCode;
    }

    public boolean isEnableSenderZipCode() {
        return enableSenderZipCode;
    }

    public void setEnableSenderZipCode(boolean enableSenderZipCode) {
        this.enableSenderZipCode = enableSenderZipCode;
    }

    public Integer getTerminalDestinyId() {
        return terminalDestinyId;
    }

    public void setTerminalDestinyId(Integer terminalDestinyId) {
        this.terminalDestinyId = terminalDestinyId;
    }

    public boolean isEnableTerminalDestinyId() {
        return enableTerminalDestinyId;
    }

    public void setEnableTerminalDestinyId(boolean enableTerminalDestinyId) {
        this.enableTerminalDestinyId = enableTerminalDestinyId;
    }

    public Integer getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(Integer addresseeId) {
        this.addresseeId = addresseeId;
    }

    public boolean isEnableAddresseeId() {
        return enableAddresseeId;
    }

    public void setEnableAddresseeId(boolean enableAddresseeId) {
        this.enableAddresseeId = enableAddresseeId;
    }

    public Integer getAddresseeZipCode() {
        return addresseeZipCode;
    }

    public void setAddresseeZipCode(Integer addresseeZipCode) {
        this.addresseeZipCode = addresseeZipCode;
    }

    public boolean isEnableAddresseeZipCode() {
        return enableAddresseeZipCode;
    }

    public void setEnableAddresseeZipCode(boolean enableAddresseeZipCode) {
        this.enableAddresseeZipCode = enableAddresseeZipCode;
    }

    public boolean isPaysSender() {
        return paysSender;
    }

    public void setPaysSender(boolean paysSender) {
        this.paysSender = paysSender;
    }

    public boolean isEnablePaysSender() {
        return enablePaysSender;
    }

    public void setEnablePaysSender(boolean enablePaysSender) {
        this.enablePaysSender = enablePaysSender;
    }

    public boolean isCredit() {
        return isCredit;
    }

    public void setCredit(boolean credit) {
        isCredit = credit;
    }

    public boolean isEnableIsCredit() {
        return enableIsCredit;
    }

    public void setEnableIsCredit(boolean enableIsCredit) {
        this.enableIsCredit = enableIsCredit;
    }

    public boolean isInternalParcel() {
        return isInternalParcel;
    }

    public void setInternalParcel(boolean internalParcel) {
        isInternalParcel = internalParcel;
    }

    public boolean isEnableIsInternalParcel() {
        return enableIsInternalParcel;
    }

    public void setEnableIsInternalParcel(boolean enableIsInternalParcel) {
        this.enableIsInternalParcel = enableIsInternalParcel;
    }

    public boolean isSendWhatsappNotification() {
        return sendWhatsappNotification;
    }

    public void setSendWhatsappNotification(boolean sendWhatsappNotification) {
        this.sendWhatsappNotification = sendWhatsappNotification;
    }

    public boolean isEnableSendWhatsappNotification() {
        return enableSendWhatsappNotification;
    }

    public void setEnableSendWhatsappNotification(boolean enableSendWhatsappNotification) {
        this.enableSendWhatsappNotification = enableSendWhatsappNotification;
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
