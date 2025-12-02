package database.insurances.handlers.InsurancesDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import database.promos.handlers.PromosDBV.models.ParcelPacking;
import io.vertx.core.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Insurance {

    @JsonProperty("insurance_id")
    private Integer insuranceId;

    @JsonProperty("max_insurance_value")
    private Double maxInsuranceValue;

    @JsonProperty("insurance_value")
    private Double insuranceValue;

    @JsonProperty("iva")
    private Double iva;

    @JsonProperty("insurance_amount")
    private Double insuranceAmount;

    @JsonProperty("has_insurance")
    private boolean hasInsurance;


    public Insurance() {
        this.insuranceId = null;
        this.maxInsuranceValue = null;
        this.insuranceValue = 0.0;
        this.iva = 0.0;
        this.insuranceAmount = 0.0;
        this.hasInsurance = false;
    }

    public void setValues(JsonObject insuranceJson) {
        Insurance insurance = insuranceJson.mapTo(Insurance.class);
        this.insuranceId = insurance.getInsuranceId();
        this.maxInsuranceValue = insurance.getMaxInsuranceValue();
        this.insuranceValue = insurance.getInsuranceValue();
        this.iva = insurance.getIva();
        this.insuranceAmount = insurance.getInsuranceAmount();
        this.hasInsurance = insurance.getInsuranceAmount() > 0;
    }


    public Integer getInsuranceId() {
        return insuranceId;
    }

    public void setInsuranceId(Integer insuranceId) {
        this.insuranceId = insuranceId;
    }

    public Double getMaxInsuranceValue() {
        return maxInsuranceValue;
    }

    public void setMaxInsuranceValue(Double maxInsuranceValue) {
        this.maxInsuranceValue = maxInsuranceValue;
    }

    public Double getInsuranceValue() {
        return insuranceValue;
    }

    public void setInsuranceValue(Double insuranceValue) {
        this.insuranceValue = insuranceValue;
    }

    public Double getIva() {
        return iva;
    }

    public void setIva(Double iva) {
        this.iva = iva;
    }

    public Double getInsuranceAmount() {
        return insuranceAmount;
    }

    public void setInsuranceAmount(Double insuranceAmount) {
        this.insuranceAmount = insuranceAmount;
    }

    public boolean isHasInsurance() {
        return hasInsurance;
    }

    public void setHasInsurance(boolean hasInsurance) {
        this.hasInsurance = hasInsurance;
    }
}
