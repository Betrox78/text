package database.promos.handlers.PromosDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import database.parcel.handlers.ParcelsPackagesDBV.models.PackageIncidence;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParcelPackage {
    private double cost;
    private double discount;
    private double iva;
    @JsonProperty("parcel_iva")
    private double parcelIva;
    @JsonProperty("total_amount")
    private double totalAmount;
    @JsonProperty("distance_km")
    private double distanceKm;
    private double price;
    @JsonProperty("package_price_id")
    private int packagePriceId;
    @JsonProperty("package_price_name")
    private String packagePriceName;
    @JsonProperty("price_km")
    private double priceKm;
    @JsonProperty("package_price_km_id")
    private int packagePriceKmId;
    @JsonProperty("package_price_distance_id")
    private String packagePriceDistanceId;
    private double packingCost;
    @JsonProperty("excess_price_id")
    private Integer excessPriceId;
    @JsonProperty("excess_price")
    private double excessPrice;
    @JsonProperty("excess_cost")
    private double excessCost;
    @JsonProperty("excess_discount")
    private double excessDiscount;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("shipping_type")
    private String shippingType;
    @JsonProperty("package_type_id")
    private Integer packageTypeId;
    @JsonProperty("pets_sizes_id")
    private Integer petsSizesId;
    private Double weight;
    private Double width;
    private Double height;
    private Double length;
    private String notes;
    private String contains;
    private Double amount;
    @JsonProperty("promo_id")
    private Integer promoId;
    @JsonProperty("customers_promos_id")
    Integer customersPromosId;
    @JsonProperty("promo_discount")
    private Double promoDiscount;
    @JsonProperty("apply_rad")
    private boolean applyRad;
    @JsonProperty("apply_ead")
    private boolean applyEad;
    @JsonProperty("is_discount_by_excess")
    private boolean isDiscountByExcess;
    @JsonProperty("md5")
    private String md5;
    @JsonProperty("promo_applied_info")
    private PromoAppliedInfo promoAppliedInfo;
    @JsonProperty("packages_incidences")
    private List<Object> packagesIncidences = new ArrayList<>();

    public ParcelPackage() {
    }

    public void setValues(JsonObject parcelPackageJson) {
        ParcelPackage parcelPackage = parcelPackageJson.mapTo(ParcelPackage.class);
        this.cost = parcelPackage.getCost();
        this.discount = parcelPackage.getDiscount();
        this.iva = parcelPackage.getIva();
        this.parcelIva = parcelPackage.getParcelIva();
        this.totalAmount = parcelPackage.getTotalAmount();
        this.distanceKm = parcelPackage.getDistanceKm();
        this.price = parcelPackage.getPrice();
        this.packagePriceId = parcelPackage.getPackagePriceId();
        this.packagePriceName = parcelPackage.getPackagePriceName();
        this.priceKm = parcelPackage.getPriceKm();
        this.packagePriceKmId = parcelPackage.getPackagePriceKmId();
        this.packagePriceDistanceId = parcelPackage.getPackagePriceDistanceId();
        this.packingCost = parcelPackage.getPackingCost();
        this.excessPriceId = parcelPackage.getExcessPriceId();
        this.excessPrice = parcelPackage.getExcessPrice();
        this.excessCost = parcelPackage.getExcessCost();
        this.quantity = parcelPackage.getQuantity();
        this.shippingType = parcelPackage.getShippingType();
        this.packageTypeId = parcelPackage.getPackageTypeId();
        this.petsSizesId = parcelPackage.getPetsSizesId();
        this.weight = parcelPackage.getWeight();
        this.width = parcelPackage.getWidth();
        this.height = parcelPackage.getHeight();
        this.length = parcelPackage.getLength();
        this.notes = parcelPackage.getNotes();
        this.contains = parcelPackage.getContains();
        this.amount = parcelPackage.getAmount();
        this.promoId = parcelPackage.getPromoId();
        this.customersPromosId = parcelPackage.getCustomersPromosId();
        this.promoDiscount = parcelPackage.getPromoDiscount();
    }

    public void setValuesInvoiceValue(JsonObject parcelPackageJson, Double amount) {
        ParcelPackage parcelPackage = parcelPackageJson.mapTo(ParcelPackage.class);
        this.cost = amount;
        this.discount = 0.0;
        this.iva = 0.0;
        this.parcelIva = 0.0;
        this.totalAmount = amount;
        this.distanceKm = parcelPackage.getDistanceKm();
        this.price = parcelPackage.getPrice();
//        this.packagePriceId = parcelPackage.getPackagePriceId();
//        this.packagePriceName = parcelPackage.getPackagePriceName();
        this.priceKm = parcelPackage.getPriceKm();
        this.packagePriceKmId = parcelPackage.getPackagePriceKmId();
        this.packagePriceDistanceId = parcelPackage.getPackagePriceDistanceId();
        this.packingCost = parcelPackage.getPackingCost();
        this.excessPriceId = null;
        this.excessPrice = 0.0;
        this.excessCost = 0.0;
        this.quantity = parcelPackage.getQuantity();
        this.shippingType = parcelPackage.getShippingType();
        this.packageTypeId = parcelPackage.getPackageTypeId();
        this.petsSizesId = parcelPackage.getPetsSizesId();
        this.weight = parcelPackage.getWeight();
        this.width = parcelPackage.getWidth();
        this.height = parcelPackage.getHeight();
        this.length = parcelPackage.getLength();
        this.notes = parcelPackage.getNotes();
        this.contains = parcelPackage.getContains();
        this.amount = amount;
        this.promoId = parcelPackage.getPromoId();
        this.customersPromosId = parcelPackage.getCustomersPromosId();
        this.promoDiscount = parcelPackage.getPromoDiscount();
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getIva() {
        return iva;
    }

    public void setIva(double iva) {
        this.iva = iva;
    }

    public double getParcelIva() {
        return parcelIva;
    }

    public void setParcelIva(double parcelIva) {
        this.parcelIva = parcelIva;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getPackagePriceId() {
        return packagePriceId;
    }

    public void setPackagePriceId(int packagePriceId) {
        this.packagePriceId = packagePriceId;
    }

    public String getPackagePriceName() {
        return packagePriceName;
    }

    public void setPackagePriceName(String packagePriceName) {
        this.packagePriceName = packagePriceName;
    }

    public double getPriceKm() {
        return priceKm;
    }

    public void setPriceKm(double priceKm) {
        this.priceKm = priceKm;
    }

    public int getPackagePriceKmId() {
        return packagePriceKmId;
    }

    public void setPackagePriceKmId(int packagePriceKmId) {
        this.packagePriceKmId = packagePriceKmId;
    }

    public String getPackagePriceDistanceId() {
        return packagePriceDistanceId;
    }

    public void setPackagePriceDistanceId(String packagePriceDistanceId) {
        this.packagePriceDistanceId = packagePriceDistanceId;
    }

    public double getPackingCost() {
        return packingCost;
    }

    public void setPackingCost(double packingCost) {
        this.packingCost = packingCost;
    }

    public Integer getExcessPriceId() {
        return excessPriceId;
    }

    public void setExcessPriceId(Integer excessPriceId) {
        this.excessPriceId = excessPriceId;
    }

    public double getExcessPrice() {
        return excessPrice;
    }

    public void setExcessPrice(double excessPrice) {
        this.excessPrice = excessPrice;
    }

    public double getExcessCost() {
        return excessCost;
    }

    public void setExcessCost(double excessCost) {
        this.excessCost = excessCost;
    }

    public double getExcessDiscount() {
        return excessDiscount;
    }

    public void setExcessDiscount(double excessDiscount) {
        this.excessDiscount = excessDiscount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getShippingType() {
        return shippingType;
    }

    public void setShippingType(String shippingType) {
        this.shippingType = shippingType;
    }

    public Integer getPackageTypeId() {
        return packageTypeId;
    }

    public void setPackageTypeId(Integer packageTypeId) {
        this.packageTypeId = packageTypeId;
    }

    public Integer getPetsSizesId() {
        return petsSizesId;
    }

    public void setPetsSizesId(Integer petsSizesId) {
        this.petsSizesId = petsSizesId;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getContains() {
        return contains;
    }

    public void setContains(String contains) {
        this.contains = contains;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public Integer getCustomersPromosId() {
        return customersPromosId;
    }

    public void setCustomersPromosId(Integer customersPromosId) {
        this.customersPromosId = customersPromosId;
    }

    public Double getPromoDiscount() {
        return promoDiscount;
    }

    public void setPromoDiscount(Double promoDiscount) {
        this.promoDiscount = promoDiscount;
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

    public boolean isDiscountByExcess() {
        return isDiscountByExcess;
    }

    public void setDiscountByExcess(boolean discountByExcess) {
        isDiscountByExcess = discountByExcess;
    }

    public List<Object> getPackagesIncidences() {
        return packagesIncidences;
    }

    public void setPackagesIncidences(List<Object> packagesIncidences) {
        this.packagesIncidences = packagesIncidences;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public PromoAppliedInfo getPromoAppliedInfo() {
        return promoAppliedInfo;
    }

    public void setPromoAppliedInfo(PromoAppliedInfo promoAppliedInfo) {
        this.promoAppliedInfo = promoAppliedInfo;
    }
}