package database.promos.handlers.PromosDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParcelPacking {

    @JsonProperty("packing_id")
    private int packingId;
    private int quantity;
    @JsonProperty("unit_price")
    private double unitPrice;
    private double amount;
    private double discount;
    private double iva;
    @JsonProperty("total_amount")
    private double totalAmount;


    public ParcelPacking() {
    }

    public void setValues(JsonObject parcelPackingJson) {
        ParcelPacking parcelPacking = parcelPackingJson.mapTo(ParcelPacking.class);
        this.unitPrice = parcelPacking.getUnitPrice();
        this.amount = parcelPacking.getAmount();
        this.discount = parcelPacking.getDiscount();
        this.iva = parcelPacking.getIva();
        this.totalAmount = parcelPacking.getTotalAmount();
    }

    public int getPackingId() {
        return packingId;
    }

    public void setPackingId(int packingId) {
        this.packingId = packingId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}