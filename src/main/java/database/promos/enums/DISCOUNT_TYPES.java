package database.promos.enums;

public enum DISCOUNT_TYPES {
    direct_amount(false, false, false, false),
    direct_percent(true, false, false, false),
    free_n_product(false, true, true, false),
    discount_n_product(true, false, true, false),
    as_price(false, false, false, true);

    private final Boolean isPercent;
    private final Boolean isFree;
    private final Boolean isByNumProducts;
    private final Boolean isAsPrice;

    DISCOUNT_TYPES(Boolean isPercent, Boolean isFree, Boolean isByNumProducts, Boolean isAsPrice) {
        this.isPercent = isPercent;
        this.isFree = isFree;
        this.isByNumProducts = isByNumProducts;
        this.isAsPrice = isAsPrice;
    }

    public static DISCOUNT_TYPES fromValue(String value) {
        for (DISCOUNT_TYPES discountType : values()) {
            if (discountType.name().equalsIgnoreCase(value)) {
                return discountType;
            }
        }
        throw new IllegalArgumentException("Unknow discount_type value " + value);
    }

    public Boolean isPercent() {
        return isPercent;
    }

    public Boolean isFree() {
        return isFree;
    }

    public Boolean isByNumProducts() {
        return isByNumProducts;
    }

    public Boolean isAsPrice() {
        return isAsPrice;
    }

}