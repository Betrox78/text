package database.parcel.handlers.ParcelsPackagesDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CostResult {
    private double amount;
    private double cost;
    private double discount;
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
    private int packagePriceDistanceId;
    @JsonProperty("excess_price_id")
    private Integer excessPriceId;
    @JsonProperty("excess_price")
    private double excessPrice;
    @JsonProperty("excess_cost")
    private double excessCost;

    public CostResult() {
    }

    public CostResult(double cost, double amount, double discount, double totalAmount, double distanceKm, double price, int packagePriceId, String packagePriceName, double priceKm, int packagePriceKmId, int packagePriceDistanceId, Integer excessPriceId, double excessPrice, double excessCost) {
        this.cost = cost;
        this.amount = amount;
        this.discount = discount;
        this.totalAmount = totalAmount;
        this.distanceKm = distanceKm;
        this.price = price;
        this.packagePriceId = packagePriceId;
        this.packagePriceName = packagePriceName;
        this.priceKm = priceKm;
        this.packagePriceKmId = packagePriceKmId;
        this.packagePriceDistanceId = packagePriceDistanceId;
        this.excessPriceId = excessPriceId;
        this.excessPrice = excessPrice;
        this.excessCost = excessCost;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

    public int getPackagePriceDistanceId() {
        return packagePriceDistanceId;
    }

    public void setPackagePriceDistanceId(int packagePriceDistanceId) {
        this.packagePriceDistanceId = packagePriceDistanceId;
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
}
