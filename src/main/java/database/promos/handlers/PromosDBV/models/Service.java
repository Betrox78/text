package database.promos.handlers.PromosDBV.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Service {
    Double amount;
    @JsonProperty("extra_charges")
    Double extraCharges;
    @JsonProperty("excess_amount")
    Double excessAmount;
    Double discount;
    @JsonProperty("excess_discount")
    Double excessDiscount;
    @JsonProperty("services_rad_amount")
    Double servicesRadAmount;
    @JsonProperty("services_ead_amount")
    Double servicesEadAmount;
    Double iva;
    @JsonProperty("parcel_iva")
    Double parcelIva;
    @JsonProperty("total_amount")
    Double totalAmount;

    public Service(Double amount, Double extraCharges, Double excessAmount, Double discount, Double excessDiscount, Double servicesRadAmount, Double servicesEadAmount, Double iva, Double parcelIva, Double totalAmount) {
        this.amount = amount;
        this.extraCharges = extraCharges;
        this.servicesRadAmount = servicesRadAmount;
        this.servicesEadAmount = servicesEadAmount;
        this.iva = iva;
        this.parcelIva = parcelIva;
        this.excessAmount = excessAmount;
        this.discount = discount;
        this.excessDiscount = excessDiscount;
        this.totalAmount = totalAmount;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getExtraCharges() { return extraCharges; }

    public void setExtraCharges(Double extraCharges) { this.extraCharges = extraCharges; }

    public Double getExcessAmount() {
        return excessAmount;
    }

    public void setExcessAmount(Double excessAmount) {
        this.excessAmount = excessAmount;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getExcessDiscount() {
        return excessDiscount;
    }

    public void setExcessDiscount(Double excessDiscount) {
        this.excessDiscount = excessDiscount;
    }

    public Double getServicesRadAmount() {
        return servicesRadAmount;
    }

    public void setServicesRadAmount(Double servicesRadAmount) {
        this.servicesRadAmount = servicesRadAmount;
    }

    public Double getServicesEadAmount() {
        return servicesEadAmount;
    }

    public void setServicesEadAmount(Double servicesEadAmount) {
        this.servicesEadAmount = servicesEadAmount;
    }

    public Double getIva() {
        return iva;
    }

    public void setIva(Double iva) {
        this.iva = iva;
    }

    public Double getParcelIva() {
        return parcelIva;
    }

    public void setParcelIva(Double parcelIva) {
        this.parcelIva = parcelIva;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
