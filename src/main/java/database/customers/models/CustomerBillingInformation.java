package database.customers.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Date;


@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerBillingInformation {

    private Integer id;

    @JsonProperty("customer_id")
    private Integer customerId;

    private String name;

    private String rfc;

    private String address;

    @JsonProperty("street_id")
    private Integer streetId;

    @JsonProperty("no_ext")
    private String noExt;

    @JsonProperty("no_int")
    private String noInt;

    @JsonProperty("suburb_id")
    private Integer suburbId;

    @JsonProperty("city_id")
    private Integer cityId;

    @JsonProperty("county_id")
    private Integer countyId;

    @JsonProperty("state_id")
    private Integer stateId;

    @JsonProperty("country_id")
    private Integer countryId;

    private String reference;

    @JsonProperty("zip_code")
    private Integer zipCode;

    private Byte status;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("updated_by")
    private Integer updatedBy;

    @JsonProperty("legal_person")
    private String legalPerson;

    @JsonProperty("contpaq_status")
    private String contpaqStatus;

    @JsonProperty("contpaq_parcel_status")
    private String contpaqParcelStatus;

    @JsonProperty("contpaq_id")
    private Integer contpaqId;

    @JsonProperty("contpaq_parcel_id")
    private Integer contpaqParcelId;

    @JsonProperty("tax_regimen")
    private String taxRegimen;

    public CustomerBillingInformation() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getZipCode() {
        return zipCode;
    }

    public void setZipCode(Integer zipCode) {
        this.zipCode = zipCode;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
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

    public String getLegalPerson() {
        return legalPerson;
    }

    public void setLegalPerson(String legalPerson) {
        this.legalPerson = legalPerson;
    }

    public String getContpaqStatus() {
        return contpaqStatus;
    }

    public void setContpaqStatus(String contpaqStatus) {
        this.contpaqStatus = contpaqStatus;
    }

    public String getContpaqParcelStatus() {
        return contpaqParcelStatus;
    }

    public void setContpaqParcelStatus(String contpaqParcelStatus) {
        this.contpaqParcelStatus = contpaqParcelStatus;
    }

    public Integer getContpaqId() {
        return contpaqId;
    }

    public void setContpaqId(Integer contpaqId) {
        this.contpaqId = contpaqId;
    }

    public Integer getContpaqParcelId() {
        return contpaqParcelId;
    }

    public void setContpaqParcelId(Integer contpaqParcelId) {
        this.contpaqParcelId = contpaqParcelId;
    }

    public String getTaxRegimen() {
        return taxRegimen;
    }

    public void setTaxRegimen(String taxRegimen) {
        this.taxRegimen = taxRegimen;
    }
}
