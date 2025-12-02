package database.promos.handlers.PromosDBV.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PromoAppliedInfo {

    Integer id;
    @JsonProperty("discount_code")
    String discountCode;
    String name;
    String description;
    @JsonProperty("discount_type")
    String discountType;
    Double discount;
    @JsonProperty("apply_rad")
    private boolean applyRad;
    @JsonProperty("apply_ead")
    private boolean applyEad;
    @JsonProperty("apply_sender_addressee")
    private boolean applySenderAddressee;

    public PromoAppliedInfo() {}

    public PromoAppliedInfo(Integer id, String discountCode, String name, String description, String discountType, Double discount, boolean applyRad, boolean applyEad, boolean applySenderAddressee) {
        this.id = id;
        this.discountCode = discountCode;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discount = discount;
        this.applyRad = applyRad;
        this.applyEad = applyEad;
        this.applySenderAddressee = applySenderAddressee;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
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

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public boolean isApplyRad() {
        return applyRad;
    }

    public void setApplyRad(boolean applyRad) {
        this.applyRad = applyRad;
    }

    public boolean isApplyEad() {
        return applyEad;
    }

    public void setApplyEad(boolean applyEad) {
        this.applyEad = applyEad;
    }

    public boolean isApplySenderAddressee() {
        return applySenderAddressee;
    }

    public void setApplySenderAddressee(boolean applySenderAddressee) {
        this.applySenderAddressee = applySenderAddressee;
    }
}
