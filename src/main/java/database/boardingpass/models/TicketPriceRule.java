package database.boardingpass.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import database.boardingpass.enums.TYPE_TRAVEL;
import utils.UtilsDate;

import java.text.ParseException;
import java.util.Date;

public class TicketPriceRule {

    private Integer id;
    @JsonProperty("init_date")
    private Date initDate;
    @JsonProperty("end_date")
    private Date endDate;
    @JsonProperty("base_special_ticket_id")
    private Integer baseSepcialTicketId;
    @JsonProperty("percent_increment")
    private Integer percentIncrement;
    @JsonProperty("percent_decrement")
    private Integer percentDecrement;
    @JsonProperty("return_percent_increment")
    private Integer returnPercentIncrement;
    @JsonProperty("return_percent_decrement")
    private Integer returnPercentDecrement;
    @JsonProperty("amount_increment")
    private Integer amountIncrement;
    @JsonProperty("amount_decrement")
    private Integer amountDecrement;
    @JsonProperty("return_amount_increment")
    private Integer returnAmountIncrement;
    @JsonProperty("return_amount_decrement")
    private Integer returnAmountDecrement;
    @JsonProperty("type_travel")
    private TYPE_TRAVEL typeTravel;
    @JsonProperty("applicable_special_ticket")
    private String applicableSpecialTicket;
    private Integer status;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("created_by")
    private Integer createdBy;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("updated_by")
    private Integer updatedBy;

    public TicketPriceRule() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getInitDate() {
        return initDate;
    }

    public void setInitDate(Date initDate) {
        this.initDate = initDate;
    }

    public void setInitDate(String initDate) throws ParseException {
        try {
            this.initDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(initDate, "SDF");
        } catch (ParseException ex) {
            this.initDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(initDate);
        }
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setEndDate(String endDate) throws ParseException {
        try {
            this.endDate = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(endDate, "SDF");
        } catch (ParseException ex) {
            this.endDate = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(endDate);
        }
    }

    public Integer getBaseSepcialTicketId() {
        return baseSepcialTicketId;
    }

    public void setBaseSepcialTicketId(Integer baseSepcialTicketId) {
        this.baseSepcialTicketId = baseSepcialTicketId;
    }

    public Integer getPercentIncrement() {
        return percentIncrement;
    }

    public void setPercentIncrement(Integer percentIncrement) {
        this.percentIncrement = percentIncrement;
    }

    public Integer getPercentDecrement() {
        return percentDecrement;
    }

    public void setPercentDecrement(Integer percentDecrement) {
        this.percentDecrement = percentDecrement;
    }

    public Integer getReturnPercentIncrement() {
        return returnPercentIncrement;
    }

    public void setReturnPercentIncrement(Integer returnPercentIncrement) {
        this.returnPercentIncrement = returnPercentIncrement;
    }

    public Integer getReturnPercentDecrement() {
        return returnPercentDecrement;
    }

    public void setReturnPercentDecrement(Integer returnPercentDecrement) {
        this.returnPercentDecrement = returnPercentDecrement;
    }

    public Integer getAmountIncrement() {
        return amountIncrement;
    }

    public void setAmountIncrement(Integer amountIncrement) {
        this.amountIncrement = amountIncrement;
    }

    public Integer getAmountDecrement() {
        return amountDecrement;
    }

    public void setAmountDecrement(Integer amountDecrement) {
        this.amountDecrement = amountDecrement;
    }

    public Integer getReturnAmountIncrement() {
        return returnAmountIncrement;
    }

    public void setReturnAmountIncrement(Integer returnAmountIncrement) {
        this.returnAmountIncrement = returnAmountIncrement;
    }

    public Integer getReturnAmountDecrement() {
        return returnAmountDecrement;
    }

    public void setReturnAmountDecrement(Integer returnAmountDecrement) {
        this.returnAmountDecrement = returnAmountDecrement;
    }

    public TYPE_TRAVEL getTypeTravel() {
        return typeTravel;
    }

    public void setTypeTravel(TYPE_TRAVEL typeTravel) {
        this.typeTravel = typeTravel;
    }

    public String getApplicableSpecialTicket() {
        return applicableSpecialTicket;
    }

    public void setApplicableSpecialTicket(String applicableSpecialTicket) {
        this.applicableSpecialTicket = applicableSpecialTicket;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }
}
