package database.promos.handlers.PromosDBV.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import database.promos.enums.DISCOUNT_TYPES;
import database.promos.enums.RULES_FOR_PACKAGES;
import database.promos.enums.TYPES_PACKAGES;

public class Promo {
    Integer id;
    String name;
    String description;
    @JsonProperty("customers_promos_id")
    Integer customersPromosId;
    @JsonProperty("discount_code")
    String discountCode;
    @JsonProperty("discount_type")
    DISCOUNT_TYPES discountType;
    Double discount;
    @JsonProperty("num_products")
    Integer numProducts;
    @JsonProperty("rule_for_packages")
    RULES_FOR_PACKAGES ruleForPackages;
    @JsonProperty("type_packages")
    TYPES_PACKAGES typePackages;
    @JsonProperty("apply_rad")
    boolean applyRad;
    @JsonProperty("apply_ead")
    boolean applyEad;

    @JsonProperty("apply_sender_addressee")
    boolean applySenderAddressee;

    public Promo() {
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCustomersPromosId() {
        return customersPromosId;
    }

    public void setCustomersPromosId(Integer customersPromosId) {
        this.customersPromosId = customersPromosId;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public DISCOUNT_TYPES getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DISCOUNT_TYPES discountType) {
        this.discountType = discountType;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Integer getNumProducts() {
        return numProducts;
    }

    public void setNumProducts(Integer numProducts) {
        this.numProducts = numProducts;
    }

    public RULES_FOR_PACKAGES getRuleForPackages() {
        return ruleForPackages;
    }

    public void setRuleForPackages(RULES_FOR_PACKAGES ruleForPackages) {
        this.ruleForPackages = ruleForPackages;
    }

    public TYPES_PACKAGES getTypePackages() {
        return typePackages;
    }

    public void setTypePackages(TYPES_PACKAGES typePackages) {
        this.typePackages = typePackages;
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