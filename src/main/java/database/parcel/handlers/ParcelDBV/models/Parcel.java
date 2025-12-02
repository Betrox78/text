package database.parcel.handlers.ParcelDBV.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Parcel {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("customer_id")
    private Integer customerId;

    @JsonProperty("customer_billing_information_id")
    private Integer customerBillingInformationId;

    @JsonProperty("branchoffice_id")
    private Integer branchofficeId;

    @JsonProperty("boarding_pass_id")
    private Integer boardingPassId;

    @JsonProperty("is_customer")
    private Boolean isCustomer;

    @JsonProperty("total_packages")
    private Integer totalPackages;

    @JsonProperty("promise_delivery_date")
    private String promiseDeliveryDate;

    @JsonProperty("delivery_time")
    private Integer deliveryTime;

    @JsonProperty("shipment_type")
    private String shipmentType;

    @JsonProperty("sender_id")
    private Integer senderId;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("sender_last_name")
    private String senderLastName;

    @JsonProperty("sender_phone")
    private String senderPhone;

    @JsonProperty("sender_email")
    private String senderEmail;

    @JsonProperty("sender_zip_code")
    private Integer senderZipCode;

    @JsonProperty("sender_address_id")
    private Integer senderAddressId;

    @JsonProperty("terminal_origin_id")
    private Integer terminalOriginId;

    @JsonProperty("terminal_destiny_id")
    private Integer terminalDestinyId;

    @JsonProperty("has_invoice")
    private Boolean hasInvoice;

    @JsonProperty("num_invoice")
    private String numInvoice;

    @JsonProperty("exchange_rate_id")
    private Integer exchangeRateId;

    @JsonProperty("cash_register_id")
    private Integer cashRegisterId;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("discount")
    private Double discount;

    @JsonProperty("services_amount")
    private Double servicesAmount;

    @JsonProperty("excess_amount")
    private Double excessAmount;

    @JsonProperty("excess_discount")
    private Double excessDiscount;

    @JsonProperty("has_insurance")
    private Boolean hasInsurance;

    @JsonProperty("insurance_value")
    private Double insuranceValue;

    @JsonProperty("insurance_amount")
    private Double insuranceAmount;

    @JsonProperty("extra_charges")
    private Double extraCharges;

    @JsonProperty("iva")
    private Double iva;

    @JsonProperty("parcel_iva")
    private Double parcelIva;

    @JsonProperty("total_amount")
    private Double totalAmount;

    @JsonProperty("promo_id")
    private Integer promoId;

    @JsonProperty("has_multiple_addressee")
    private Boolean hasMultipleAddressee;

    @JsonProperty("addressee_id")
    private Integer addresseeId;

    @JsonProperty("addressee_name")
    private String addresseeName;

    @JsonProperty("addressee_last_name")
    private String addresseeLastName;

    @JsonProperty("addressee_phone")
    private String addresseePhone;

    @JsonProperty("addressee_email")
    private String addresseeEmail;

    @JsonProperty("addressee_zip_code")
    private Integer addresseeZipCode;

    @JsonProperty("addressee_address_id")
    private Integer addresseeAddressId;

    @JsonProperty("addressee_customer_billing_information_id")
    private Integer addresseeCustomerBillingInformationId;

    @JsonProperty("parcel_tracking_code")
    private String parcelTrackingCode;

    @JsonProperty("waybill")
    private String waybill;

    @JsonProperty("prints_counter")
    private Integer printsCounter;

    @JsonProperty("pays_sender")
    private Boolean paysSender;

    @JsonProperty("parcel_status")
    private Integer parcelStatus;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("notes_invoice")
    private String notesInvoice;

    @JsonProperty("status")
    private Byte status;

    @JsonProperty("delivered_at")
    private LocalDateTime deliveredAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("updated_by")
    private Integer updatedBy;

    @JsonProperty("canceled_at")
    private LocalDateTime canceledAt;

    @JsonProperty("canceled_by")
    private Integer canceledBy;

    @JsonProperty("payback")
    private Double payback;

    @JsonProperty("schedule_route_destination_id")
    private Integer scheduleRouteDestinationId;

    @JsonProperty("payment_condition")
    private String paymentCondition;

    @JsonProperty("debt")
    private Double debt;

    @JsonProperty("purchase_origin")
    private String purchaseOrigin;

    @JsonProperty("insurance_id")
    private Integer insuranceId;

    @JsonProperty("invoice_id")
    private Integer invoiceId;

    @JsonProperty("parcels_cancel_reason_id")
    private Integer parcelsCancelReasonId;

    @JsonProperty("invoice_is_global")
    private Boolean invoiceIsGlobal;

    @JsonProperty("cancel_code")
    private String cancelCode;

    @JsonProperty("parent_id")
    private Integer parentId;

    @JsonProperty("is_internal_parcel")
    private Boolean isInternalParcel;

    @JsonProperty("in_payment")
    private Boolean inPayment;

    @JsonProperty("send_whatsapp_notification")
    private Boolean sendWhatsappNotification;

    @JsonProperty("integration_partner_session_id")
    private Integer integrationPartnerSessionId;

    @JsonProperty("ticket_id")
    private Integer ticketId;



    public JsonObject toJsonObject() {
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Map<String, Object> map = mapper.convertValue(this, Map.class);
        return new JsonObject(map);
    }
}