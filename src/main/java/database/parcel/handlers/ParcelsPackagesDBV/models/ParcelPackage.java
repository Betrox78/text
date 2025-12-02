package database.parcel.handlers.ParcelsPackagesDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import static service.commons.Constants._PARCEL_PREPAID_DETAIL_ID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParcelPackage {
    @JsonIgnore
    @JsonProperty("id")
    private int id;

    @JsonProperty("parcel_id")
    private Integer parcelId;

    @JsonProperty("shipping_type")
    private String shippingType;

    @JsonProperty("is_valid")
    private boolean isValid;

    @JsonProperty("need_auth")
    private Boolean needAuth;

    @JsonProperty("auth_by")
    private Integer authBy;

    @JsonProperty("auth_code")
    private String authCode;

    @JsonProperty("auth_status")
    private Boolean authStatus;

    @JsonProperty("package_status")
    private int packageStatus;

    @JsonProperty("need_documentation")
    private Boolean  needDocumentation;

    @JsonProperty("package_code")
    private String packageCode;

    @JsonProperty("prints_counter")
    private Integer printsCounter;

    @JsonProperty("parcels_deliveries_id")
    private Integer parcelsDeliveriesId;

    @JsonProperty("package_type_id")
    private Integer packageTypeId;

    @JsonProperty("package_price_id")
    private Integer packagePriceId;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("package_price_km_id")
    private Integer packagePriceKmId;

    @JsonProperty("price_km")
    private Double priceKm;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("discount")
    private Double discount;

    @JsonProperty("total_amount")
    private Double totalAmount;

    @JsonProperty("weight")
    private Double weight;

    @JsonProperty("height")
    private Double height;

    @JsonProperty("width")
    private Double width;

    @JsonProperty("length")
    private Double length;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("promo_id")
    private Integer promoId;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("updated_by")
    private Integer updatedBy;

    @JsonProperty("schedule_route_destination_id")
    private Integer scheduleRouteDestinationId;

    @JsonProperty("excess_price_id")
    private Integer excessPriceId;

    @JsonProperty("excess_price")
    private Double excessPrice;

    @JsonProperty("excess_cost")
    private Double excessCost;

    @JsonProperty("excess_discount")
    private Double excessDiscount;

    @JsonProperty("iva")
    private Double iva;

    @JsonProperty("parcel_iva")
    private Double parcelIva;

    @JsonProperty("pets_sizes_id")
    private Integer petsSizesId;

    @JsonProperty("excess_promo_id")
    private Integer excessPromoId;

    @JsonProperty("contains")
    private String contains;

    @JsonProperty(_PARCEL_PREPAID_DETAIL_ID)
    private String parcelPrepaidDetailId;

    @JsonProperty("is_old")
    private Boolean is_old;

}
