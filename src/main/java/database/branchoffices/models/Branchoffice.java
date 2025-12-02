package database.branchoffices.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import database.branchoffices.enums.BRANCHOFFICE_TYPE;

public class Branchoffice {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("prefix")
    String prefix;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("address")
    String address;
    @JsonProperty("street_id")
    Integer streetId;
    @JsonProperty("no_ext")
    String noExt;
    @JsonProperty("no_int")
    String noInt;
    @JsonProperty("suburb_id")
    Integer suburbId;
    @JsonProperty("city_id")
    Integer cityId;
    @JsonProperty("county_id")
    Integer countyId;
    @JsonProperty("state_id")
    Integer stateId;
    @JsonProperty("country_id")
    Integer countryId;
    @JsonProperty("zip_code")
    String zipCode;
    @JsonProperty("reference")
    String reference;
    @JsonProperty("phone")
    String phone;
    @JsonProperty("branch_office_type")
    BRANCHOFFICE_TYPE branchOfficeType;
    @JsonProperty("latitude")
    String latitude;
    @JsonProperty("longitude")
    String longitude;
    @JsonProperty("manager_id")
    Integer managerId;
    @JsonProperty("receive_transhipments")
    Boolean receiveTranshipments;
    @JsonProperty("transhipment_site_name")
    String transhipmentSiteName;
    Integer status;
    @JsonProperty("created_at")
    String createdAt;
    @JsonProperty("created_by")
    Integer createdBy;
    @JsonProperty("updated_at")
    String updatedAt;
    @JsonProperty("updated_by")
    Integer updatedBy;
    @JsonProperty("time_zone")
    String timeZone;
    @JsonProperty("time_checkpoint")
    Integer timeCheckpoint;
    @JsonProperty("time_manteinance")
    Integer timeManteinance;
    @JsonProperty("business_segment")
    String businessSegment;
    @JsonProperty("timezone")
    String timezone;
    @JsonProperty("iva")
    Double iva;

    public Branchoffice() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStreetId() {
        return streetId;
    }

    public void setStreetId(Integer streetId) {
        this.streetId = streetId;
    }

    public String getNoExt() {
        return noExt;
    }

    public void setNoExt(String noExt) {
        this.noExt = noExt;
    }

    public String getNoInt() {
        return noInt;
    }

    public void setNoInt(String noInt) {
        this.noInt = noInt;
    }

    public Integer getSuburbId() {
        return suburbId;
    }

    public void setSuburbId(Integer suburbId) {
        this.suburbId = suburbId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getCountyId() {
        return countyId;
    }

    public void setCountyId(Integer countyId) {
        this.countyId = countyId;
    }

    public Integer getStateId() {
        return stateId;
    }

    public void setStateId(Integer stateId) {
        this.stateId = stateId;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BRANCHOFFICE_TYPE getBranchOfficeType() {
        return branchOfficeType;
    }

    public void setBranchOfficeType(BRANCHOFFICE_TYPE branchOfficeType) {
        this.branchOfficeType = branchOfficeType;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public Boolean getReceiveTranshipments() {
        return receiveTranshipments;
    }

    public void setReceiveTranshipments(Boolean receiveTranshipments) {
        this.receiveTranshipments = receiveTranshipments;
    }

    public String getTranshipmentSiteName() {
        return transhipmentSiteName;
    }

    public void setTranshipmentSiteName(String transhipmentSiteName) {
        this.transhipmentSiteName = transhipmentSiteName;
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

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Integer getTimeCheckpoint() {
        return timeCheckpoint;
    }

    public void setTimeCheckpoint(Integer timeCheckpoint) {
        this.timeCheckpoint = timeCheckpoint;
    }

    public Integer getTimeManteinance() {
        return timeManteinance;
    }

    public void setTimeManteinance(Integer timeManteinance) {
        this.timeManteinance = timeManteinance;
    }

    public String getBusinessSegment() {
        return businessSegment;
    }

    public void setBusinessSegment(String businessSegment) {
        this.businessSegment = businessSegment;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Double getIva() {
        return iva;
    }

    public void setIva(Double iva) {
        this.iva = iva;
    }

}

