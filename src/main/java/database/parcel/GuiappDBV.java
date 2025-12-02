package database.parcel;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.money.PaymentDBV;
import database.parcel.enums.PARCEL_STATUS;
import database.parcel.handlers.GuiappDBV.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static database.alliances.AllianceDBV.LIMIT;
import static database.alliances.AllianceDBV.PAGE;
import static database.promos.PromosDBV.DISCOUNT;
import static service.commons.Constants.*;
import static utils.UtilsDate.sdfDataBase;

public class GuiappDBV extends DBVerticle {

    public static final String  VALID_CODE_GUIA = "GuiappDBV.validCode";
    public static final String  GET_PACKAGE_TERMINAL_DISTANCE  = "GuiappDBV.getPackageTerminalsDistance";
    public static final String REGISTER = "GuiappDBV.register";
    public static final String GET_PARCEL_BY_TRACKING_CODE = "PpPriceDBV.getParcelByTrackingCode";
    public static final String UPDATE_PARCELS_STATUS_PREPAID_DETAILS = "PpPriceDBV.updateParcelsStatusPrepaidDetails";
    public static final String GET_VALID_RANGO_GUIAPP = "PpPriceDBV.getValidRangoGuipp";
    public static final String GET_PARCEL_BY_TRACKING_CODE_CANCEL = "PpPriceDBV.getParcelByTrackingCodeCancel";
    public static final String PARCEL_SALES_REPORT = "GuiappDBV.salesReport";
    public static final String PARCEL_AVAILABLE_REPORT = "GuiappDBV.availableReport";
    public static final String GET_PARCEL_BY_TRACKING_CODE_CANCEL_GUIAPP_ALL = "PpPriceDBV.getParcelByTrackingCodeCancelGuiappAll";
    public static final String CANCEL_REGISTER_GUIAPP = "GuiappDBV.cancelRegisterGuiapp";
    public static final String GET_LIST_GUIAPP_CODE = "GuiappDBV.getListGuiappCodev2";
    public static final String GET_LIST_GUIAPP_BY_CUSTOMER = "GuiappDBV.getListGuiaPpByCustomer";
    public static final String GET_DETAIL_GUIA = "GuiappDBV.getDetailGuia";
    public static final String GET_MAX_COST = "GuiappDBV.getMaxCost";
    public static final String GET_EXCESS_COST = "GuiappDBV.getExcessCost";
    public static final String GET_GUIAPP_RANGES_BY_CUSTOMER = "GuiappDBV.getCustomerRanges";
    public static final String GET_GUIAPP_KMS_BY_CUSTOMER = "GuiappDBV.getCustomerKms";
    public static final String GET_COST_EXCHANGE = "GuiappDBV.costExchange";
    public static final String VALID_MULTIPLE_CODE_GUIA = "GuiappDBV.multipleValidation";
    public static final String GET_PERCENT_DISCOUNT = "GuiappDBV.getPercentDiscount";



    private static final Integer MAX_LIMIT = 30;

    @Override
    public String getTableName() {
        return "parcels_prepaid";
    }

    MaxCost maxCost;
    CalculateExcess calculateExcess;
    CostExchange costExchange;
    Register register;
    PercentDiscount percentDiscount;

    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.maxCost = new MaxCost(this);
        this.calculateExcess = new CalculateExcess(this);
        this.costExchange = new CostExchange(this);
        this.register = new Register(this);
        this.percentDiscount = new PercentDiscount(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case VALID_CODE_GUIA:
                VALID_CODE_GUIA(message);
                break;
            case GET_PACKAGE_TERMINAL_DISTANCE:
                getPackageTerminalsDistance(message);
                break;
            case REGISTER:
                this.register.handle(message);
                break;
            case GET_PARCEL_BY_TRACKING_CODE :
                getParcelByTrackingCode(message);
                break;
            case UPDATE_PARCELS_STATUS_PREPAID_DETAILS :
                updateParcelsStatusPrepaidDetails(message);
                break;
            case GET_VALID_RANGO_GUIAPP :
                getValidRangoGuipp(message);
                break;
            case PARCEL_SALES_REPORT:
                this.salesReport(message);
                break;
            case GET_PARCEL_BY_TRACKING_CODE_CANCEL :
                getParcelByTrackingCodeCancel(message);
                break;
            case PARCEL_AVAILABLE_REPORT:
                availableReport(message);
                break;
            case GET_PARCEL_BY_TRACKING_CODE_CANCEL_GUIAPP_ALL:
                getParcelByTrackingCodeCancelGuiappAll(message);
                break;
            case CANCEL_REGISTER_GUIAPP:
                cancelRegisterGuiapp(message);
                break;
            case GET_LIST_GUIAPP_CODE:
                getListGuiappCodev2(message);
                break;
            case GET_LIST_GUIAPP_BY_CUSTOMER:
                getListGuiaPpByCustomer(message);
                break;
            case GET_DETAIL_GUIA:
                getDetailGuia(message);
                break;
            case GET_MAX_COST:
                this.maxCost.handle(message);
                break;
            case GET_EXCESS_COST:
                this.calculateExcess.handle(message);
                break;
            case GET_GUIAPP_RANGES_BY_CUSTOMER:
                this.getCustomerRanges(message);
                break;
            case GET_GUIAPP_KMS_BY_CUSTOMER:
                this.getCustomerKms(message);
                break;
            case GET_COST_EXCHANGE:
                this.costExchange.handle(message);
                break;
            case VALID_MULTIPLE_CODE_GUIA:
                this.multipleValidation(message);
                break;
            case GET_PERCENT_DISCOUNT:
                this.percentDiscount.handle(message);
                break;
        }
    }

    private void VALID_CODE_GUIA(Message<JsonObject> message) {

        String trackingCode = message.body().getString("code");
        JsonArray param = new JsonArray().add(trackingCode);

        this.dbClient.queryWithParams(GET_CODE_GUIAPP, param, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Throwable("Element not found");
                }

                JsonObject code = reply.result().getRows().get(0);
                Integer ppStatus = code.getInteger("pp_status");
                Integer ppParcelStatus = code.getInteger("pp_parcel_status");
                Integer ppdStatus = code.getInteger("ppd_status");
                Integer ppdParcelStatus = code.getInteger("ppd_parcel_status");
                Integer parcelId = code.getInteger("parcel_id");
                String parcelWaybill = code.getString("parcel_waybill");
                Date expireAt = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(code.getString("expire_at_prepaid_detail"));
                Date now = Calendar.getInstance().getTime();

                if(ppParcelStatus == 4 || ppdParcelStatus == 4 || ppStatus == 4 || ppdStatus == 4) throw new Throwable("Code was canceled");
                if(parcelId != null) throw new Throwable("Code was exchanged in " + parcelWaybill);
                if(UtilsDate.isGreaterThan(now, expireAt)) throw new Throwable("Code was expire");

                message.reply(code);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });


    }

    private void getPackageTerminalsDistance(Message<JsonObject> message) {

        this.dbClient.query(GET_TERMIANLS_DISTANCE, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                JsonObject result =new JsonObject().put("data", reply.result().getRows());
                if(result.isEmpty()){
                    throw new Exception("Element not found");
                }

                message.reply(result);
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });


    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject parcel = message.body().copy();
            this.register(conn, parcel).whenComplete((resultRegister, errorRegister) -> {
                try {
                    if (errorRegister != null){
                        throw errorRegister;
                    }
                    this.commit(conn, message, resultRegister);
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject guiapp) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {

            Integer cashOutId = (Integer) guiapp.remove(CASHOUT_ID);
            Integer cashRegisterId = guiapp.getInteger(CASH_REGISTER_ID);
            JsonObject customerCreditData = (JsonObject) guiapp.remove("customer_credit_data");
            Double availableCredit = customerCreditData.getDouble("available_credit");
            String reservation_code = UtilsID.generateID("PP");
            final Boolean is_credit = guiapp.containsKey("is_credit") ? ((Boolean) guiapp.remove("is_credit")) : false;
            JsonObject ppDetail = (JsonObject) guiapp.remove("pp_detail");
            JsonArray payments = (JsonArray) guiapp.remove("payments");
            JsonObject objPayment = new JsonObject();
            JsonArray packages = guiapp.getJsonArray("packages");
            //borrar
            if(!is_credit){
                objPayment =  payments.getJsonObject(0);
                objPayment.put("alias", guiapp.getString("payment_condition"));
            }

            Integer promo_id = (Integer) guiapp.remove("promo_id");

            final JsonObject finalObjPayment = objPayment;
            final double ivaPercent = (Double) guiapp.remove("iva_percent");
            final int currencyId = (Integer) guiapp.remove("currency_id");
            final Integer createdBy = guiapp.getInteger("created_by");

            double creditBalance = 0;
            double debt = 0;

            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.YEAR, 1);


            JsonObject guiappObject = new JsonObject();
            Double totalAmountInner = guiapp.getDouble("total_amount");

            if(is_credit) {
                creditBalance = availableCredit - totalAmountInner;
                debt = totalAmountInner;
            }

            final Double finalDebt = debt;
            final Double finalCreditBalance = creditBalance;

            guiappObject.put("tracking_code", reservation_code)
            .put("shipment_type", guiapp.getString("shipment_type"))
            .put("payment_condition", guiapp.getString("payment_condition"))
            .put("purchase_origin", guiapp.getString("purchase_origin"))
            .put("customer_id", guiapp.getInteger("customer_id"))
            .put("crated_by", guiapp.getInteger("created_by"))
            .put("created_at", UtilsDate.sdfDataBase(new Date()))
            .put("parcel_status",1)
            .put("status", 1)
            .put("cash_register_id",guiapp.getInteger("cash_register_id"))
            .put("total_count_guipp", guiapp.getInteger("total_count_guipp"))
            .put("total_count_guipp_remaining", 0)
            .put("amount",guiapp.getInteger("amount"))
            .put("discount", guiapp.getDouble("discount"))
            .put("has_insurance", false)
            .put("insurance_value", guiapp.getInteger("insurance_value"))
            .put("insurance_amount", guiapp.getInteger("insurance_amount"))
            .put("extra_charges", guiapp.getInteger("extra_charges"))
            .put("iva",guiapp.getDouble("iva_percent") )
            .put("parcel_iva", guiapp.getDouble("parcel_iva"))
            .put("total_amount", totalAmountInner)
            .put("debt", debt)
            .put("expire_at", UtilsDate.sdfDataBase(c.getTime()))
            .put("branchoffice_id", guiapp.getInteger("terminal_origin_id"))
            .put("seller_user_id", guiapp.containsKey("seller_id")?guiapp.getInteger("seller_id"):null)
            .put("billing_address_id", guiapp.getInteger("billing_address_id"))
            .put("percent_discount_applied", guiapp.getDouble("percent_discount_applied", 0.00))
            .put("customer_billing_information_id", guiapp.getInteger("customer_billing_information_id"));

            if(promo_id != null) {
                guiappObject.put("promo_id", promo_id);
            }

            GenericQuery gen = this.generateGenericCreate(guiappObject);

            conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> guiappReply) -> {
               try {
                   if(guiappReply.failed()) {
                       throw guiappReply.cause();
                   }

                   final int guiappID = guiappReply.result().getKeys().getInteger(0);
                   guiappObject.put(ID, guiappID);

                   this.insertPrepaidDetail(conn, guiappID, guiappObject,ppDetail, createdBy, packages).whenComplete((resultDetail, errorDetail) -> {
                       try {
                           if(errorDetail != null) {
                               throw  errorDetail;
                           }

                           this.endRegister(conn, guiappObject, finalObjPayment, createdBy, cashOutId, cashRegisterId, guiappID, ivaPercent, currencyId, ppDetail, is_credit, customerCreditData, finalCreditBalance, finalDebt).whenComplete((resultRegister, errorRegister) -> {
                               try {
                                   if (errorRegister != null) {
                                       throw errorRegister;
                                   }
                                   future.complete(resultRegister);
                               } catch (Throwable t){
                                   future.completeExceptionally(t);
                               }
                           });

                       } catch (Throwable t){
                           future.completeExceptionally(t);
                       }
                   });
               } catch (Throwable t) {
                   future.completeExceptionally(t);
               }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    protected CompletableFuture<JsonObject> endRegister(SQLConnection conn, JsonObject guiapp, JsonObject objPayment, Integer createdBy,  Integer cashOutId, Integer cashRegisterId, Integer guiappID, Double ivaPercent, Integer currencyId, JsonObject ppDetail, Boolean isCredit, JsonObject customerCreditData,  Double finalCreditBalance, Double finalDebt ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Double paid;
            Double total = guiapp.getDouble("total_amount");
            if(isCredit) {
                paid = guiapp.getDouble("amount");
            } else {
                paid = objPayment.getDouble("amount");
            }
            Double iva = this.getIva(total, ivaPercent);
            Double paid_change = paid - total;

            JsonArray ticketArray = new JsonArray();

            List<CompletableFuture<JsonObject>> ticketsTask = new ArrayList<>();
            ticketArray.add(new JsonObject());

            final int numTickets = ticketArray.size();
            for(int i = 0; i < numTickets ; i++){
                ticketsTask.add(insertTicketsPP(conn, cashOutId, guiapp.getInteger("id"), objPayment, cashRegisterId, createdBy, true, total, iva, paid, paid_change, ticketArray.getJsonObject(i), ppDetail, isCredit ));
            }


            /**
             final int numPack = guiapp.getInteger("total_count_guipp");
             ticketArray.add(new JsonObject());
            for(int i = 0; i < numPack; i++ ) {
                ticketArray.add(new JsonObject());
                ticketsTask.add(insertTicketsPP(conn, cashOutId, guiapp.getInteger("id"), objPayment, cashRegisterId, createdBy, false, total, iva, paid, paid_change, ticketArray.getJsonObject(i), ppDetail ));
            }*/

            CompletableFuture.allOf(ticketsTask.toArray(new CompletableFuture[ticketsTask.size()])).whenComplete((st, tError) -> {
                try {
                    if(tError != null) {
                        throw tError;
                    }

                    this.insertTicketDetail(conn, ticketArray, createdBy, guiapp).whenComplete((Boolean detailSuccess, Throwable dError) -> {
                        try {
                            if(dError != null) {
                                throw  dError;
                            }

                            if(isCredit) {

                                this.updateCustomerCredit(conn, customerCreditData, finalDebt, createdBy, false, false, finalCreditBalance)
                                        .whenComplete( (replyCustomer, errorCustomer) -> {
                                            try {
                                                if(errorCustomer != null) {
                                                    throw new Exception(errorCustomer);
                                                }

                                                JsonObject result = new JsonObject().put("id", guiapp.getInteger("id"));
                                                result.put("tracking_code", guiapp.getString("tracking_code"));
                                                result.put("ticket_prepaid", ticketArray.getValue(0));

                                                future.complete(result);
                                            } catch (Throwable t) {
                                                t.printStackTrace();
                                                future.completeExceptionally(t);
                                            }
                                        });

                            } else {

                                List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();

                                for (int pNum = 0; pNum < ticketArray.size(); pNum++) {
                                    pTasks.add(insertPaymentAndCashOutMove(conn, objPayment, ticketArray.getJsonObject(pNum), guiapp ,createdBy, cashOutId, cashRegisterId, currencyId));
                                }

                                CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pTasks.size()])).whenComplete((pSuccess, pError) -> {
                                    try {
                                        if(pError != null) {
                                            throw pError;
                                        }

                                        JsonObject result = new JsonObject().put("id", guiapp.getInteger("id"));
                                        result.put("tracking_code", guiapp.getString("tracking_code"));
                                        result.put("ticket_prepaid", ticketArray.getValue(0));

                                        future.complete(result);

                                    } catch (Throwable t) {
                                        future.completeExceptionally(t);
                                    }
                                });

                            }

                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private Double getIva(Double amount, Double ivaPercent) {
        return amount - (amount /(1 + (ivaPercent/100)));
    }

    private CompletableFuture<JsonObject> insertTicketsPP(SQLConnection conn, Integer cashOutId, Integer idService, JsonObject objPayment, Integer cashRegisterId, Integer createdBy, Boolean isPrepaid, Double total, Double iva, Double paid, Double paidChange, JsonObject ticket, JsonObject ppDetail, Boolean isCredit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {

            String action = isCredit ? "voucher" : "purchase";

            if(isPrepaid) {
                ticket.put("parcel_prepaid_id", idService);
            }

            ticket.put("iva",iva);
            ticket.put("total", total);
            ticket.put("paid", paid);
            ticket.put("paid_change", paidChange);
            //Qué es payback y para que se utiliza?
            ticket.put("payback_before", 0);
            ticket.put("payback_money", 0);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("created_by", createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));

            ticket.put("action", action);
            ticket.put("created_at", sdfDataBase(new Date()));
            ticket.put("extra_charges",0);
            ticket.put("has_extras",0);

            GenericQuery insert = this.generateGenericCreateSendTableName("tickets", ticket);

            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.succeeded()) {
                        final int id = reply.result().getKeys().getInteger(0);
                        ticket.put("id", id);
                        future.complete(ticket);
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, JsonArray tickets, Integer createdBy, JsonObject guiapp) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();

        JsonArray details = new JsonArray();
        try {
            for(int i = 0 ; i < tickets.size(); i++){
                JsonObject tick = tickets.getJsonObject(i);

                JsonObject detailTicket = new JsonObject();

                detailTicket.put("ticket_id", tick.getInteger("id"));

                detailTicket.put("detail", "Paquete PP");
                detailTicket.put("unit_price", tick.getDouble("total"));
                detailTicket.put("amount", tick.getDouble("total"));
                detailTicket.put("discount",guiapp.getDouble("discount"));
                detailTicket.put("created_at", UtilsDate.sdfDataBase(new Date()));
                detailTicket.put("created_by",createdBy);
                detailTicket.put("quantity", 1);

                details.add(detailTicket);


                JsonObject packagesPP = new JsonObject();
                packagesPP.put("ticket_id", tick.getInteger("id"));
                packagesPP.put("unit_price", 0.0);
                packagesPP.put("amount", 0.0);
                packagesPP.put("discount",0.0);
                packagesPP.put("created_at", UtilsDate.sdfDataBase(new Date()));
                packagesPP.put("created_by",createdBy);
                packagesPP.put("quantity", guiapp.getInteger("total_count_guipp"));
                packagesPP.put("detail", "Paquetes canjeables");

                details.add(packagesPP);
            }

            for(int z = 0; z < details.size(); z++) {
                inserts.add(this.generateGenericCreate("tickets_details", details.getJsonObject(z)));
            }

            conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                try {
                    if (replyInsert.failed()){
                        throw new Exception(replyInsert.cause());
                    }
                    future.complete(replyInsert.succeeded());

                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    protected CompletableFuture<JsonObject> insertPrepaidDetail(SQLConnection conn, Integer guiaPpId,JsonObject guiappObject, JsonObject objPayment, Integer createdBy, JsonArray packages){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray detailArray = new JsonArray();

            final int numberPackages = packages.size();

            List<CompletableFuture<JsonObject>> detailTask = new ArrayList<>();
            //detailArray.add(new JsonObject())
            for (int i = 0;i < numberPackages; i++) {
                int numInside = packages.getJsonObject(i).getInteger("quantity");
                for(int x = 0; x < numInside; x++) {
                    detailArray.add(new JsonObject());
                    detailTask.add(insertPrepaidDetailPP(conn, guiappObject, objPayment, createdBy, detailArray.getJsonObject(i ), packages.getJsonObject(i) ));
                }
            }

            CompletableFuture.allOf(detailTask.toArray(new CompletableFuture[detailTask.size()])).whenComplete((dP, dpError) -> {
                try {
                    if( dpError != null) {
                        throw  dpError;
                    }
                    future.complete(guiappObject);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertPrepaidDetailPP(SQLConnection conn, JsonObject ppObject, JsonObject objPayment, Integer createdBy, JsonObject detailObject, JsonObject ppPack) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {

            String shipping_type = ppPack.getString("shipping_type");
            Integer idRango = ppPack.getInteger("idRango");

            conn.queryWithParams(QUERY_VALID_PACKAGE, new JsonArray().add(shipping_type).add(idRango), replyValidation -> {
                try {
                    if (replyValidation.failed()) {
                        throw replyValidation.cause();
                    }
                    if (replyValidation.result().getRows().isEmpty()) {
                        throw new Exception("El rango no es del mismo tipo de shipping");
                    }



                    conn.queryWithParams(QUERY_GET_SHIPPING_TYPE, new JsonArray().add(shipping_type), replyShippingType -> {
                       try {
                           if(replyShippingType.failed()) {
                               throw replyShippingType.cause();
                           }

                           Integer shippingTypeId  =  replyShippingType.result().getRows().get(0).getInteger("id");

                           detailObject.put("guiapp_code", UtilsID.generateGuiaPpID("GPP"));
                           detailObject.put("parcel_prepaid_id", ppObject.getInteger("id"));
                           detailObject.put("price_km", ppPack.getDouble("price_km"));
                           detailObject.put("price_km_id",  ppPack.getInteger("idKm"));
                           detailObject.put("price", ppPack.getDouble("price"));
                           detailObject.put("price_id", idRango);
                           detailObject.put("amount", 0);
                           detailObject.put("discount",0);
                           detailObject.put("parcel_status", 0);
                           detailObject.put("crated_by", ppObject.getInteger("created_by") );
                           detailObject.put("created_at", ppObject.getString("created_at"));
                           detailObject.put("expire_at", ppObject.getString("expire_at"));
                           detailObject.put("package_type_id", shippingTypeId);

                           String insert = this.generateGenericCreate("parcels_prepaid_detail", detailObject);

                           conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                               try {
                                   if (reply.succeeded()) {
                                       future.complete(detailObject);
                                   } else {
                                       future.completeExceptionally(reply.cause());
                                   }
                               } catch (Exception e){
                                   future.completeExceptionally(e);
                               }
                           });

                       } catch (Throwable t) {
                           future.completeExceptionally(t);
                       }
                    });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject objPayment, JsonObject ticket, JsonObject guiapp, Integer createdBy, Integer cashOutId, Integer cashRegisterId,  Integer currencyId ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject payment = new JsonObject();
            JsonObject cashOutMove = new JsonObject();

            if(ticket.getInteger("parcel_prepaid_id") != null) {
                payment.put("parcel_prepaid_id",ticket.getInteger("parcel_prepaid_id"));
            }

            payment.put("payment_method_id", objPayment.getInteger("payment_method_id"));
            payment.put("payment_method", objPayment.getString("alias"));
            payment.put("amount", ticket.getDouble("total"));
            payment.put("currency_id",currencyId );
            payment.put("created_by",createdBy);
            payment.put("created_at",UtilsDate.sdfDataBase(new Date()));
            payment.put("ticket_id",ticket.getInteger("id"));

            PaymentDBV objPaymentDBV = new PaymentDBV();
            objPaymentDBV.insertPayment(conn, payment).whenComplete((resultPayment, errorPayment) -> {
                if (errorPayment != null) {
                    future.completeExceptionally(errorPayment);
                } else {
                    payment.put("id", resultPayment.getInteger("id"));
                    cashOutMove.put("payment_id", resultPayment.getInteger("id"));
                    cashOutMove.put("cash_out_id", cashOutId);
                    cashOutMove.put("quantity", payment.getDouble("amount"));
                    cashOutMove.put("move_type", "0");
                    cashOutMove.put("created_by", createdBy);
                    String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);

                    conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyMove) -> {
                        try {
                            if (replyMove.failed()) {
                                throw new Exception(replyMove.cause());
                            }

                            future.complete(payment);

                        } catch (Exception e) {
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void getParcelByTrackingCode (Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            String trackingCode = message.body().getString("parcelTrackingCode");
            Integer updatedBy = message.body().getInteger("updated_by");
            Boolean toPrint = message.body().getBoolean("print");
            JsonArray param = new JsonArray().add(trackingCode);
            conn.queryWithParams(QUERY_PARCEL_BY_TRACKING_CODE,param,reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultParcel = reply.result().getRows();
                    if(resultParcel.isEmpty()){
                        throw new Exception("Element not found");
                    }
                    JsonObject parcel = resultParcel.get(0);
                    Integer parcelId = parcel.getInteger("id");
                    JsonArray paramParcelPackage = new JsonArray().add(parcelId);
                    conn.queryWithParams(QUERY_PARCELS_PACKAGES_BY_ID_PARCEL,paramParcelPackage,replyParcelPackage->{
                        try {
                            if (replyParcelPackage.failed()){
                                throw replyParcelPackage.cause();
                            }
                            List<JsonObject> parcelsPackages = replyParcelPackage.result().getRows();
                            List<CompletableFuture<JsonObject>>  incidencesTasks = new ArrayList<>();
                            final int len = parcelsPackages.size();
                            if(len > 0) {
                                parcel.put("parcel_packages", parcelsPackages);
                            }

                            this.commit(conn,message,parcel);


                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    private CompletableFuture<Boolean> updateCustomerCredit(SQLConnection conn, JsonObject customerCreditData, Double debt, Integer createdBy, boolean is_cancel, boolean reissue, Double creditBalance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Double actualCreditAvailable = customerCreditData.getDouble("available_credit");
            Double actualCreditBalance = customerCreditData.getDouble("credit_balance");
            JsonObject customerObject = new JsonObject();

            double creditAvailable = 0;
            if(is_cancel){
                creditAvailable = actualCreditAvailable + debt;
            } else {
                if (reissue){
                    //double diffPaidDebt = reissuePaid - debt;
                    customerObject.put("credit_balance", creditBalance > 0 ? actualCreditBalance + creditBalance : actualCreditBalance);
                    creditAvailable = debt > 0 ? actualCreditAvailable - debt : actualCreditAvailable;
                } else {
                    creditAvailable = actualCreditAvailable - debt;
                }
            }

            customerObject
                    .put(ID, customerCreditData.getInteger(ID))
                    .put("credit_available", creditAvailable)
                    .put(UPDATED_BY, createdBy)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
            String updateCostumer = this.generateGenericUpdateString("customer", customerObject);

            conn.update(updateCostumer, (AsyncResult<UpdateResult> replyCustomer) -> {
                try {
                    if (replyCustomer.failed()) {
                        throw replyCustomer.cause();
                    }
                    future.complete(replyCustomer.succeeded());
                } catch (Throwable t) {
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void updateParcelsStatusPrepaidDetails(Message<JsonObject> message) {

//        JsonArray guiapp = message.body().getJsonArray("guiapp");
       // JsonArray param = new JsonArray().add(trackingCode);

        this.startTransaction(message, (SQLConnection conn) -> {
         //   JsonObject parcel = message.body().copy();
            JsonArray guiapp = message.body().getJsonArray("guiapp");
            int id_parcels= message.body().getInteger("id_parcels");
            this.endRegisterGuiapp(conn, guiapp,id_parcels).whenComplete((resultRegister, errorRegister) -> {
                try {
                    if (errorRegister != null){
                        throw errorRegister;
                    }
                    this.commit(conn, message, resultRegister);
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }
    protected CompletableFuture<JsonObject> endRegisterGuiapp(SQLConnection conn, JsonArray guiapp,int id_parcels){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
           // JsonArray guiappArray = new JsonArray();
            List<CompletableFuture<JsonObject>> guiappTask = new ArrayList<>();
           // guiappArray.add(new JsonObject());
            final int numGuiapp = guiapp.size();
            for(int i = 0; i < numGuiapp ; i++){
                guiappTask.add(updateParcelsPrepaidDetails(conn,guiapp.getJsonObject(i).getInteger("id_parcel_prepaid_detail"),id_parcels ));
            }
            CompletableFuture.allOf(guiappTask.toArray(new CompletableFuture[guiappTask.size()])).whenComplete(( st, tError) -> {
                try {
                    JsonObject result =new JsonObject().put("result",st);
                    if(tError != null) {
                        throw tError;
                    }
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }
    private CompletableFuture<JsonObject> updateParcelsPrepaidDetails(SQLConnection conn, Integer idGuiapp,int id_parcels) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String update =QUERY_UPDATE_USAGE_GUIAPP+id_parcels+",updated_at=now()" +" WHERE id ="+idGuiapp;

            conn.update(update, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.succeeded()) {
                        JsonObject guiappResult = new JsonObject();
                       // final int id = reply.result().getKeys().getInteger(0);
                        guiappResult.put("data", true);
                        future.complete(guiappResult);
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }
    private void getValidRangoGuipp(Message<JsonObject> message) {
        String query_pp_price ="SELECT * FROM pp_price";
        String query_pp_price_km ="SELECT * FROM pp_price_km";
        JsonArray packages= message.body().getJsonArray("packages");
        try {
            this.dbClient.query(query_pp_price, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> pp_price = reply.result().getRows();
                    if(pp_price.isEmpty()){
                        throw new Exception("Element not found");
                    }
                    this.dbClient.query(query_pp_price_km, reply2 -> {
                        try {
                            if (reply2.failed()){
                                throw reply2.cause();
                            }
                            List<JsonObject> pp_price_km = reply2.result().getRows();
                            if(pp_price_km.isEmpty()){
                                throw new Exception("Element not found");
                            }
                            this.GetValidGuiapp(packages).whenComplete((resultValid, error) -> {
                                try {
                                    if (error != null){
                                        throw error;
                                    }
                                    int packageSize= packages.size();
                                    double volumen_linear=0;
                                    double weight=0;
                                    JsonArray validPackages=new JsonArray();
                                    List<JsonObject> guiappValid = resultValid.getJsonArray("result").getList();
                                    for(int i=0;i<packageSize;i++){
                                        //Medidas del paquete pos
                                        JsonObject packageToValidate=packages.getJsonObject(i);
                                        volumen_linear=packages.getJsonObject(i).getString("shipping_type").equals("courier")?0.0:packages.getJsonObject(i).getDouble("linear_volume");
                                        weight=packages.getJsonObject(i).getString("shipping_type").equals("courier")?0.0:Double.valueOf( packages.getJsonObject(i).getString("weight"));
                                        JsonObject volumen_price= new JsonObject();
                                        JsonObject weight_price= new JsonObject();
                                        JsonObject package_price= new JsonObject();
                                        double finalVolumen_linear = volumen_linear;
                                        pp_price.forEach(pp->{
                                            if(finalVolumen_linear >=pp.getDouble("min_linear_volume") && finalVolumen_linear <=pp.getDouble("max_linear_volume") ){
                                                volumen_price.put("name_price",pp);
                                            }

                                        });
                                        double finalWeight = weight;
                                        pp_price.forEach(pp->{
                                            if(finalWeight >=pp.getDouble("min_weight") && finalWeight <=pp.getDouble("max_weight") ){
                                                weight_price.put("weight_price",pp);
                                            }
                                        });
                                        package_price=volumen_price.getJsonObject("name_price").getDouble("price")>=weight_price.getJsonObject("weight_price").getDouble("price")?volumen_price:weight_price;
                                        //verificamos  rango del paquete con la inforamcion de la guiapp
                                        JsonObject finalPackage_price = package_price;
                                        guiappValid.forEach(g->{
                                            if(packageToValidate.getString("guiapp_code").equals(g.getString("guiapp_code"))){
                                                //Vericamos el rango de la guiapp
                                                pp_price.forEach(pp->{
                                                    try{
                                                        if( g.getInteger("price_id")==pp.getInteger("id")){
                                                            /*if((((finalPackage_price.getJsonObject("name_price").getDouble("price")<= pp.getDouble("price") || finalPackage_price.getJsonObject("name_price").getString("shipping_type").equals("courier") ))&& ( finalPackage_price.getJsonObject("name_price").getDouble("max_linear_volume") <=pp.getDouble("max_linear_volume")  || finalPackage_price.getJsonObject("name_price").getDouble("max_weight") <=pp.getDouble("max_weight") )) ){
                                                                validPackages.add(finalPackage_price);
                                                            }else{
                                                                throw error;
                                                            }*/
                                                            validPackages.add(finalPackage_price);
                                                        }
                                                    }catch (Throwable t){
                                                        message.body().put("errorGuiapp",finalPackage_price);
                                                        reportQueryError(message, t);
                                                    }

                                                });
                                            }
                                        });
                                    }
                                    if(validPackages.size()==packages.size()){
                                        message.reply(validPackages);
                                    }else{
                                        message.reply(new JsonObject().put("error","guiapp:informacion incorrecta"));
                                    }
                                } catch (Throwable t){
                                    t.printStackTrace();
                                }
                            });
                        } catch (Throwable t){
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
             t.printStackTrace();
            reportQueryError(message, t);
         }

    }

    private CompletableFuture<JsonObject> GetValidGuiapp( JsonArray packages) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            final int numGuiapp = packages.size();
            String params="";
            if(numGuiapp>1){
                for(int i = 0; i < numGuiapp ; i++){
                    if(i==0){
                        params="'"+packages.getJsonObject(i).getString("guiapp_code")+"'";
                    }else{
                        params=params+",'"+packages.getJsonObject(i).getString("guiapp_code")+"'";
                    }
                }
            }else {
                params="'"+packages.getJsonObject(0).getString("guiapp_code")+"'";
            }

            String query=GET_CODE_GUIAPP_ALL+" in("+params+")";

            dbClient.queryWithParams(query, null, reply -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> rows = reply.result().getRows();
                    if (rows.isEmpty()) {
                        future.completeExceptionally(new Throwable("Packings: Not found"));
                    } else {
                        JsonObject packing = new JsonObject().put("result",rows);

                        future.complete(packing);
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                    future.completeExceptionally(reply.cause());
                }
            });
        } catch (Exception ex){
            ex.printStackTrace();
            future.completeExceptionally(ex.getCause());
        }


        return future;
    }
    private void getParcelByTrackingCodeCancelGuiappAll (Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            String trackingCode = message.body().getString("parcelTrackingCode");
            Integer updatedBy = message.body().getInteger("updated_by");
            Boolean toPrint = message.body().getBoolean("print");
            JsonArray param = new JsonArray().add(trackingCode);
            conn.queryWithParams(QUERY_PARCEL_BY_TRACKING_CODE_CANCEL_GUIAPP_ALL,param,reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultParcel = reply.result().getRows();
                    if(resultParcel.isEmpty()){
                        throw new Exception("Element not found");
                    }
                    JsonObject parcel = new  JsonObject();
                    parcel.put("data",resultParcel);
                    this.commit(conn,message,parcel);
                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }
      private void getParcelByTrackingCodeCancel (Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            String trackingCode = message.body().getString("parcelTrackingCode");
            Integer updatedBy = message.body().getInteger("updated_by");
            Boolean toPrint = message.body().getBoolean("print");
            JsonArray param = new JsonArray().add(trackingCode);
            conn.queryWithParams(QUERY_PARCEL_BY_TRACKING_CODE_CANCEL,param,reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> resultParcel = reply.result().getRows();
                    if(resultParcel.isEmpty()){
                        throw new Exception("Element not found");
                    }
                    JsonObject parcel = resultParcel.get(0);
                    Integer parcelId = parcel.getInteger("id");
                    JsonArray paramParcelPackage = new JsonArray().add(parcel.getInteger("id"));
                    conn.queryWithParams(QUERY_PARCELS_PACKAGES_BY_ID_PARCEL,paramParcelPackage,replyParcelPackage->{
                        try {
                            if (replyParcelPackage.failed()){
                                throw replyParcelPackage.cause();
                            }
                            List<JsonObject> parcelsPackages = replyParcelPackage.result().getRows();
                            List<CompletableFuture<JsonObject>>  incidencesTasks = new ArrayList<>();
                            final int len = parcelsPackages.size();
                            for (JsonObject parcelPackage:parcelsPackages) {
                                incidencesTasks.add(insertIncidences(parcelPackage, parcel));
                                //incidencesTasks.add(insertTracking(parcelPackage));
                            }
                            CompletableFuture<Void> allIncidences = CompletableFuture.allOf(incidencesTasks.toArray(new CompletableFuture[len]));
                            allIncidences.whenComplete((s, tt) -> {
                                try {
                                    if (tt != null){
                                        throw tt;
                                    }

                                    conn.queryWithParams(GET_PARCELS_PACKAGES_TRACKING_BY_PARCEL_ID, new JsonArray().add(parcelId), replyPPT -> {
                                        try {
                                            if (replyPPT.failed()){
                                                throw replyPPT.cause();
                                            }
                                            List<JsonObject> parcelsPackagesTracking = replyPPT.result().getRows();
                                            parcel.put("parcel_packages_tracking",parcelsPackagesTracking);
                                            parcel.put("parcel_packages",parcelsPackages);
                                            if(!parcel.containsKey("has_incidences")){
                                                parcel.put("has_incidences", false);
                                            }

                                            List<String> parcelsPackagesList = parcelsPackagesTracking.stream().map(x -> (JsonObject)x)
                                                    .map(x -> String.valueOf(x.getInteger("parcel_package_id"))).filter(x -> x != "null").collect(Collectors.toList());
                                            String parcelPackagesIds = String.join(", ", parcelsPackagesList);

                                            conn.queryWithParams(GET_PARCELS_DELIVERIES_BY_PARCELS_PACKAGE, new JsonArray().add(parcelPackagesIds), replyPD -> {
                                                try {
                                                    if (replyPD.failed()){
                                                        throw replyPD.cause();
                                                    }
                                                    List<JsonObject> parcelsDeliveries = replyPD.result().getRows();
                                                    parcel.put("parcels_deliveries", parcelsDeliveries);
                                                    //parcels_packages
                                                    if(parcel.getInteger("parcel_status").equals(PARCEL_STATUS.CANCELED.ordinal())){
                                                        conn.queryWithParams(QUERY_PARCEL_CANCEL_DETAIL_BY_ID, paramParcelPackage, replyPCD -> {
                                                            try {
                                                                if (replyPCD.failed()){
                                                                    throw replyPCD.cause();
                                                                }
                                                                List<JsonObject> parcelsCanceledDetails = replyPCD.result().getRows();
                                                                parcel.put("parcel_cancel_details",parcelsCanceledDetails);
                                                                //this.commit(conn, message, parcel);
                                                                conn.queryWithParams(QUERY_PARCELS_RAD_BY_ID_PARCEL_PRINT, new JsonArray().add(parcelId) , replyRad -> {
                                                                    try{
                                                                        if (replyRad.failed()){
                                                                            throw new Exception(replyRad.cause());
                                                                        }
                                                                        List<JsonObject> parcelRad = replyRad.result().getRows();
                                                                        parcel.put("parcel_service_type", parcelRad);
                                                                        this.commit(conn,message,parcel);
                                                                    } catch (Exception e){
                                                                        e.printStackTrace();
                                                                        this.rollback(conn, replyRad.cause(), message);
                                                                    }
                                                                });
                                                            } catch (Throwable t){
                                                                t.printStackTrace();
                                                                this.rollback(conn, t, message);
                                                            }
                                                        });
                                                    }else{
                                                        conn.queryWithParams(QUERY_PARCELS_RAD_BY_ID_PARCEL_PRINT, new JsonArray().add(parcelId) , replyRad -> {
                                                            try{
                                                                if (replyRad.failed()){
                                                                    throw new Exception(replyRad.cause());
                                                                }
                                                                List<JsonObject> parcelRad = replyRad.result().getRows();
                                                                parcel.put("parcel_service_type", parcelRad);
                                                                this.commit(conn,message,parcel);
                                                            } catch (Exception e){
                                                                e.printStackTrace();
                                                                this.rollback(conn, replyRad.cause(), message);
                                                            }
                                                        });
                                                        //this.commit(conn, message, parcel);
                                                    }
                                                } catch (Throwable t){
                                                    t.printStackTrace();
                                                    this.rollback(conn, t, message);
                                                }
                                            });

                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });


                                } catch (Throwable t){
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }
    private CompletableFuture<JsonObject> insertIncidences(JsonObject parcelPackage, JsonObject parcel) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray paramIncidences = new JsonArray().add(parcelPackage.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_PARCEL_PACKAGE_INCIDENCES,paramIncidences,replyIncidences->{
            try {
                if (replyIncidences.succeeded()) {
                    List<JsonObject> resultsIncidences = replyIncidences.result().getRows();
                    if (!resultsIncidences.isEmpty()) {
                        parcelPackage.put("packages_incidences", resultsIncidences);
                        parcel.put("has_incidences", true);
                    } else {
                        parcelPackage.put("packages_incidences", new JsonArray());
                    }
                    future.complete(parcelPackage);
                } else {
                    future.completeExceptionally(replyIncidences.cause());
                }
            } catch (Exception e) {
                future.completeExceptionally(e.getCause());
            }

        });

        return future;
    }

    private void salesReport (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            boolean flagTotals = body.getBoolean("flag_totals");
            String shippingType = body.getString("shipping_type");
            Integer terminalCityId = body.getInteger("terminal_city_id");
            Integer terminalId = body.getInteger("terminal_id");
            Integer terminalOriginCityId = body.getInteger("terminal_origin_city_id");
            Integer terminalOriginId = body.getInteger("terminal_origin_id");
            Integer terminalDestinyCityId = body.getInteger("terminal_destiny_city_id");
            Integer terminalDestinyId = body.getInteger("terminal_destiny_id");
            String shippingParcelType = body.getString("shipping_parcel");
            JsonArray customers = body.getJsonArray("customer_array");
            JsonArray sellers = body.getJsonArray("seller_array");
            Integer branchofficeId = body.getInteger("branchoffice_id");
            String customerParcelType = body.getString("customer_parcel_type");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer sellerName = body.getInteger("seller_name_id");
            Boolean includeGeneralPublic = body.getBoolean("include_general_public");
            JsonObject complementQueryData = new JsonObject()
                    .put("sellers", sellers)
                    .put("branchoffice_id", branchofficeId)
                    .put("customer_parcel_type", customerParcelType);

            this.getCustomersByCriteriaForParcelReport(complementQueryData).whenComplete((resultCustomersIds, errorCustomersIds) -> {
                if (errorCustomersIds != null) {
                    reportQueryError(message, errorCustomersIds.getCause());
                } else {
                    String QUERY = QUERY_SALES_REPORT;
                    JsonArray params = new JsonArray();
                    params.add(initDate).add(endDate);

                    if(terminalCityId != null){
                        QUERY += " AND b.city_id = ?";
                        params.add(terminalCityId);
                    }
                    if(terminalId != null){
                        QUERY += " AND p.branchoffice_id = ?";
                        params.add(terminalId);
                    }
                    if(terminalOriginCityId != null){
                        QUERY += " AND b.city_id = ?";
                        params.add(terminalOriginCityId);
                    }
                    if(terminalOriginId != null){
                        QUERY += " AND p.branchoffice_id = ?";
                        params.add(terminalOriginId);
                    }
                    if(terminalDestinyCityId != null){
                        message.reply(new JsonArray());
                    }
                    if(terminalDestinyId != null){
                        message.reply(new JsonArray());
                    }
                    if(shippingType != null){
                        message.reply(new JsonArray());
                    }
                    if(shippingType != null){
                        message.reply(new JsonArray());
                    }
                    if(shippingParcelType != null){
                        QUERY += " AND p.shipment_type = ?";
                        params.add(shippingParcelType);
                    }

                    // OBTENER PAQUETES POR USUARIO QUE REGISTRO LA VENTA DEL PAQUETE
                    if(sellerName != null){
                        params.add(sellerName);
                        QUERY = QUERY.concat(SELLER_NAME_ID);
                    }

                    if(customers != null){
                        String concatQuery = resultCustomersIds.getString("customers_ids");
                        if(!concatQuery.isEmpty()) {
                            concatQuery = concatQuery + ",";
                        }
                        for(int i = 0 ; i < customers.size() ; i++ ){
                            Integer idCustomer = customers.getJsonObject(i).getInteger("id");
                            if((customers.size() - 1) == i){
                                concatQuery = concatQuery + idCustomer + "";
                            } else {
                                concatQuery = concatQuery + idCustomer + ",";
                            }
                        }
                        resultCustomersIds.put("customers_ids", concatQuery);
                    }

                    if(resultCustomersIds.getBoolean("has_criteria") || customers != null) {
                        String[] customerIds = resultCustomersIds.getString("customers_ids").split(",");
                        StringBuilder placeholders = new StringBuilder();
                        for (int i = 0; i < customerIds.length; i++) {
                            String customerId = customerIds[i].trim();
                            if (!customerId.isEmpty()) {
                                params.add(Integer.parseInt(customerId));
                                placeholders.append("?");
                                if (i < customerIds.length - 1) {
                                    placeholders.append(",");
                                }
                            }
                        }
                        if (placeholders.length() == 0) {
                            placeholders.append("?");
                            params.add("");
                        }
                        if(resultCustomersIds.getBoolean("has_criteria") &&
                                complementQueryData.getInteger("branchoffice_id") != null &&
                                includeGeneralPublic != null) {
                            QUERY = QUERY.concat(" AND (p.customer_id IN (" + placeholders +") OR p.branchoffice_id = ?)");
                            params.add(complementQueryData.getInteger("branchoffice_id"));
                        } else {
                            QUERY = QUERY.concat(" AND p.customer_id IN (" + placeholders +")");
                        }
                    }

                    if(!resultCustomersIds.getBoolean("has_criteria") && includeGeneralPublic != null) {
                        QUERY += " AND cc.parcel_type IS NULL";
                    }

                    if (!flagTotals){
                        QUERY = QUERY.concat(" GROUP BY p.tracking_code ").concat(SALES_REPORT_ORDER_BY);
                        Integer page = body.getInteger(PAGE);
                        String QUERY_COUNT = "SELECT COUNT(*) AS count FROM ("+QUERY+") AS parcels_sales_report;";
                        List<Future> taskList = new ArrayList<>();

                        if(page!=null){
                            Future f1 = Future.future();
                            this.dbClient.queryWithParams(QUERY_COUNT, params.copy(), f1.completer());
                            taskList.add(f1);
                            Integer limit = body.getInteger(LIMIT);
                            QUERY += " LIMIT ? OFFSET ? ";
                            params.add(limit).add((page - 1) * limit);
                        }

                        Future f2 = Future.future();
                        this.dbClient.queryWithParams(QUERY, params, f2.completer());
                        taskList.add(f2);

                        CompositeFuture.all(taskList).setHandler(reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }

                                JsonObject result = new JsonObject();
                                Integer index = taskList.size() == 1 ? 0 : 1;

                                List<JsonObject> parcelsList = reply.result().<ResultSet>resultAt(index).getRows();

                                if (parcelsList.isEmpty()) {
                                    if(page!=null){
                                        result.put("count", 0)
                                                .put(LIMIT, parcelsList.size())
                                                .put(RESULTS, parcelsList);

                                        message.reply(result);
                                    }else{
                                        message.reply(new JsonArray(parcelsList));
                                    }
                                } else {
                                    Integer count = 0;
                                    if(page!=null){
                                        count = reply.result().<ResultSet>resultAt(0).getRows().get(0).getInteger("count");
                                    }

                                    List<CompletableFuture<JsonObject>> parcelTask = new ArrayList<>();

                                    parcelsList.forEach(parcel -> {
                                        parcel.put("shipping_type" , shippingType);
                                        parcelTask.add(getSalesPackages(parcel));
                                        parcelTask.add(getPaymentsInfo(parcel));
                                    });
                                    Integer finalCount = count;
                                    CompletableFuture.allOf(parcelTask.toArray(new CompletableFuture[parcelTask.size()])).whenComplete((ps, pt) -> {
                                        try {
                                            if (pt != null) {
                                                reportQueryError(message, pt.getCause());
                                            } else {
                                                if(page!=null){
                                                    result.put("count", finalCount)
                                                            .put("items", parcelsList.size())
                                                            .put(RESULTS, parcelsList);
                                                    message.reply(result);
                                                }else{
                                                    message.reply(new JsonArray(parcelsList));
                                                }
                                            }
                                        } catch (Exception e){
                                            reportQueryError(message, e.getCause());
                                        }
                                    });
                                }

                            } catch (Throwable t){
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    } else {
                        QUERY = QUERY.concat(QUERY_EXCLUDE_ZERO_AMOUNT).concat(" GROUP BY p.tracking_code ").concat(SALES_REPORT_ORDER_BY);
                        QUERY = QUERY_SALES_REPORT_TOTALS + QUERY + ") AS t;";
                        //QUERY = QUERY_AVAILABLE_REPORT_TOTALS + QUERY + ") AS t;";

                        this.dbClient.queryWithParams(QUERY, params, reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }
                                List<JsonObject> parcelsList = reply.result().getRows();
                                message.reply(new JsonArray(parcelsList));
                            } catch (Throwable t){
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    }
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private CompletableFuture<JsonObject> getSalesPackages(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        String shippingType = parcel.getString("shipping_type");
        String QUERY = QUERY_SALES_REPORT_PACKAGES;
        /*if(shippingType != null){
            params.add(shippingType);
            QUERY += " AND pp.shipping_type = ?";
        }*/

        QUERY += " GROUP BY pp.price_id";

        this.dbClient.queryWithParams(QUERY, params, handler->{
            try {
                if (handler.succeeded()) {
                    List<JsonObject> resultsTracking = handler.result().getRows();
                    if (!resultsTracking.isEmpty()) {
                        parcel.put("packages", resultsTracking);
                    } else {
                        parcel.put("packages", new JsonArray());
                    }
                    parcel.remove("shipping_type");
                    future.complete(parcel);
                } else {
                    future.completeExceptionally(handler.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e.getCause());
            }
        });

        return future;
    }
    private CompletableFuture<JsonObject> getPaymentsInfo(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_SALES_REPORT_PAYMENT_INFO, params, handler->{
            try {
                if (handler.succeeded()) {
                    List<JsonObject> result = handler.result().getRows();
                    parcel.put("payment_info", result);
                    future.complete(parcel);
                } else {
                    future.completeExceptionally(handler.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e.getCause());
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> getCustomersByCriteriaForParcelReport(JsonObject data){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Boolean hasSellers = data.getJsonArray("sellers") != null;
        Boolean hasBranchoffice = data.getInteger("branchoffice_id") != null;
        Boolean hasCustomerParcelType = data.getString("customer_parcel_type") != null;
        String QUERY = QUERY_GET_CUSTOMERS_BY_USER_SELLER_ID;

        if(!hasSellers && !hasBranchoffice && !hasCustomerParcelType) {
            data.put("customers_ids", "");
            data.put("has_criteria", false);
            future.complete(data);
        } else {
            JsonArray params = new JsonArray();

            if(hasSellers) {
                JsonArray sellers = data.getJsonArray("sellers");
                StringBuilder placeholders = new StringBuilder();

                for(int i = 0 ; i < sellers.size() ; i++ ){
                    Integer id = sellers.getJsonObject(i).getInteger("id");
                    params.add(id);
                    placeholders.append("?");
                    if (i < sellers.size() - 1) {
                        placeholders.append(",");
                    }
                }
                QUERY +=" AND user_seller_id IN (" + placeholders +")";
            }

            if(hasBranchoffice) {
                QUERY += " AND branchoffice_id = ?";
                params.add(data.getInteger("branchoffice_id"));
            }

            if(hasCustomerParcelType) {
                QUERY += " AND parcel_type = ?";
                params.add(data.getString("customer_parcel_type"));
            }

            this.dbClient.queryWithParams(QUERY, params, handler->{
                try {
                    if (handler.succeeded()) {
                        List<JsonObject> results = handler.result().getRows();
                        data.put("has_criteria", true);
                        if (!results.isEmpty()) {
                            String customersIds = "";

                            for(int i = 0 ; i < results.size() ; i++ ){
                                Integer id = results.get(i).getInteger("id");
                                if((results.size() - 1) == i){
                                    customersIds = customersIds + id + "";
                                } else {
                                    customersIds = customersIds + id + ",";
                                }
                            }
                            data.put("customers_ids", customersIds);
                        } else {
                            data.put("customers_ids", "");
                        }
                        future.complete(data);
                    } else {
                        future.completeExceptionally(handler.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e.getCause());
                }
            });
        }
        return future;
    }

    private void availableReport (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            boolean flagTotals = body.getBoolean("flag_totals");
            JsonArray customers = body.getJsonArray("customer_array");
            JsonArray sellers = body.getJsonArray("seller_array");
            Integer sellerName = body.getInteger("seller_name_id");
            Integer availableGuiasPP = body.getInteger("available_guiaspp");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            JsonObject complementQueryData = new JsonObject()
                    .put("sellers", sellers);

            this.getCustomersByCriteriaForParcelReport(complementQueryData).whenComplete((resultCustomersIds, errorCustomersIds) -> {
                if (errorCustomersIds != null) {
                    reportQueryError(message, errorCustomersIds.getCause());
                } else {
                    String QUERY = QUERY_AVAILABLE_REPORT;
                    if(!initDate.isEmpty() && !endDate.isEmpty()) {
                        QUERY += " AND pp.created_at BETWEEN ? AND ?";
                        params.add(initDate).add(endDate);
                    }

                    if(sellerName != null){
                        params.add(sellerName);
                        QUERY = QUERY.concat(SELLER_NAME_ID);
                    }

                    if(customers != null) {
                        String concatQuery = resultCustomersIds.getString("customers_ids");
                        if(!concatQuery.isEmpty()) {
                            concatQuery = concatQuery + ",";
                        }
                        for(int i = 0 ; i < customers.size() ; i++ ){
                            Integer idCustomer = customers.getJsonObject(i).getInteger("id");
                            if((customers.size() - 1) == i){
                                concatQuery = concatQuery + idCustomer + "";
                            } else {
                                concatQuery = concatQuery + idCustomer + ",";
                            }
                        }
                        resultCustomersIds.put("customers_ids", concatQuery);
                    }

                    if(resultCustomersIds.getBoolean("has_criteria") || customers != null) {
                        String[] customerIds = resultCustomersIds.getString("customers_ids").split(",");
                        StringBuilder placeholders = new StringBuilder();
                        for (int i = 0; i < customerIds.length; i++) {
                            String customerId = customerIds[i].trim();
                            if (!customerId.isEmpty()) {
                                params.add(Integer.parseInt(customerId));
                                placeholders.append("?");
                                if (i < customerIds.length - 1) {
                                    placeholders.append(",");
                                }
                            }
                        }
                        if (placeholders.length() == 0) {
                            placeholders.append("?");
                            params.add("");
                        }
                        QUERY = QUERY.concat(" AND pp.customer_id IN (" + placeholders +")");
                    }

                    QUERY = QUERY.concat(" GROUP BY pp.id HAVING total_available > 0");

                    if(availableGuiasPP != null) {
                        QUERY = QUERY.concat(" AND total_available <= ?");
                        params.add(availableGuiasPP);
                    }

                    QUERY = QUERY.concat("  ORDER BY pp.created_at");

                    if (!flagTotals){
                        Integer page = body.getInteger(PAGE);
                        String QUERY_COUNT = "SELECT COUNT(*) AS count FROM ("+QUERY+") AS parcels_sales_report;";
                        List<Future> taskList = new ArrayList<>();

                        if(page!=null){
                            Future f1 = Future.future();
                            this.dbClient.queryWithParams(QUERY_COUNT, params.copy(), f1.completer());
                            taskList.add(f1);
                            Integer limit = body.getInteger(LIMIT);
                            QUERY += " LIMIT ? OFFSET ? ";
                            params.add(limit).add((page - 1) * limit);
                        }

                        Future f2 = Future.future();
                        this.dbClient.queryWithParams(QUERY, params, f2.completer());
                        taskList.add(f2);

                        CompositeFuture.all(taskList).setHandler(reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }

                                JsonObject result = new JsonObject();
                                Integer index = taskList.size() == 1 ? 0 : 1;

                                List<JsonObject> parcelsList = reply.result().<ResultSet>resultAt(index).getRows();

                                if (parcelsList.isEmpty()) {
                                    if(page!=null){
                                        result.put("count", 0)
                                                .put(LIMIT, parcelsList.size())
                                                .put(RESULTS, parcelsList);

                                        message.reply(result);
                                    }else{
                                        message.reply(new JsonArray(parcelsList));
                                    }
                                } else {
                                    Integer count = 0;
                                    if(page!=null){
                                        count = reply.result().<ResultSet>resultAt(0).getRows().get(0).getInteger("count");
                                    }

                                    if(page!=null){
                                        result.put("count", count)
                                                .put("items", parcelsList.size())
                                                .put(RESULTS, parcelsList);
                                        message.reply(result);
                                    }else{
                                        message.reply(new JsonArray(parcelsList));
                                    }
                                }
                            } catch (Throwable t){
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    } else {
                        QUERY = QUERY_AVAILABLE_REPORT_TOTAL + QUERY + ") AS t;";

                        this.dbClient.queryWithParams(QUERY, params, reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }

                                List<JsonObject> parcelsList = reply.result().getRows();
                                message.reply(new JsonArray(parcelsList));
                            } catch (Throwable t){
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    }
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Integer parcel_prepaid_id, Double totalPayments, JsonObject cashChange, Integer createdBy, Double ivaPercent, String action) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            Double iva = this.getIva(totalPayments, ivaPercent);

            // Create ticket_code
            // ticket.put("ticket_code", ticketCode);
            ticket.put("parcel_prepaid_id", parcel_prepaid_id);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
            ticket.put("total", totalPayments);
            ticket.put("created_by", createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));
            if(action != null){
                ticket.put("action", action);
            }

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble("total");
                Double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                Double difference_paid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if(!Objects.equals(action, "voucher")) {
                    if (totalPayments < total) {
                        throw new Throwable("The payment " + total + " is greater than the total " + totalPayments);
                    } else if (totalPayments > total) {
                        throw new Throwable("The payment " + total + " is lower than the total " + totalPayments);
                    } else if (paid_change > difference_paid) {
                        throw new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                    } else if (paid_change < difference_paid) {
                        throw new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                    }
                }
            } else {
                ticket.put("paid", totalPayments);
                ticket.put("paid_change", 0.0);
            }

            String insert = this.generateGenericCreate("tickets", ticket);

            conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                try{
                    if (reply.succeeded()) {
                        final int id = reply.result().getKeys().getInteger(0);
                        ticket.put("ticket_id", id);
                        future.complete(ticket);
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer ticketId, Integer createdBy, JsonArray packages, JsonArray packings, JsonObject parcel, Boolean internalCustomer , JsonObject serviceObject) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();

        conn.query("SELECT id, name_price, shipping_type FROM package_price;", replyPP -> {
            try{
                if(replyPP.failed()){
                    throw new Exception(replyPP.cause());
                }
                List<JsonObject> resultPP = replyPP.result().getRows();
                Map<String, List<JsonObject>> groupedPackages = packages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString("shipping_type")));
                JsonArray details = new JsonArray();

                for (String s : groupedPackages.keySet()) {
                    JsonObject packagePrice = new JsonObject();
                    AtomicReference<Integer> quantity = new AtomicReference<>(0);
                    AtomicReference<Double> unitPrice = new AtomicReference<>(0.00);
                    AtomicReference<Double> amount = new AtomicReference<>(0.00);
                    Optional<JsonObject> packageName = resultPP.stream().filter(x -> x.getInteger("id").equals(groupedPackages.get(s).get(0).getInteger("package_price_id"))).findFirst();
                    String packageRange = packageName.get().getString("name_price");
                    groupedPackages.get(s).forEach(x -> {
                        quantity.getAndSet(quantity.get() + 1);
                        unitPrice.updateAndGet(v -> v + x.getDouble("total_amount"));
                        amount.updateAndGet(v -> v + x.getDouble("total_amount"));
                        packagePrice.put(DISCOUNT, x.getDouble(DISCOUNT));
                    });
                    packagePrice.put("shipping_type", s);
                    packagePrice.put("unit_price", unitPrice.get());
                    packagePrice.put("amount", amount.get());
                    packagePrice.put("quantity", quantity.get());
                    if(packagePrice.getInteger("quantity") != null){
                        if(packagePrice.getInteger("quantity") > 0){
                            JsonObject ticketDetail = new JsonObject();
                            JsonObject packageDetail = packagePrice;
                            String shippingType = packageDetail.getString("shipping_type");
                            switch (shippingType){
                                case "parcel":
                                    shippingType = "paquetería";
                                    break;
                                case "courier":
                                    shippingType = "mensajería";
                                    break;
                                case "pets":
                                    shippingType = "mascota";
                                    break;
                                case "frozen":
                                    shippingType = "carga refrigerada";
                                    break;
                            }
                            ticketDetail.put("ticket_id", ticketId);
                            ticketDetail.put("quantity", packageDetail.getInteger("quantity"));
                            ticketDetail.put("detail", "Envío de " + shippingType + " con rango " + packageRange);
                            ticketDetail.put("unit_price", packageDetail.getDouble("unit_price"));
                            ticketDetail.put(DISCOUNT, packageDetail.getDouble(DISCOUNT));
                            ticketDetail.put("amount", packageDetail.getDouble("amount"));
                            ticketDetail.put("created_by", createdBy);
                            details.add(ticketDetail);
                        }
                    }
                }

                int len = packings.size();
                for (int i = 0; i < len; i++) {
                    JsonObject packing = packings.getJsonObject(i);
                    JsonObject ticketDetail = new JsonObject();

                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", packing.getInteger("quantity"));
                    ticketDetail.put("detail", "Embalaje");
                    ticketDetail.put("unit_price", packing.getDouble("unit_price"));
                    ticketDetail.put("amount", packing.getDouble("amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                if(parcel.getBoolean("has_insurance") != null && parcel.getBoolean("has_insurance")){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Seguro de envío");
                    ticketDetail.put("unit_price", parcel.getDouble("insurance_amount"));
                    ticketDetail.put("amount", parcel.getDouble("insurance_amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                if(serviceObject.getBoolean("is_rad") != null || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Servicio " +
                            serviceObject.getString("service"));
                    ticketDetail.put("unit_price", serviceObject.getDouble("service_amount"));
                    ticketDetail.put("amount", serviceObject.getDouble("service_amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }


                if(packages.isEmpty() && packings.isEmpty()) {
                    JsonObject ticketDetail = new JsonObject()
                            .put("ticket_id", ticketId)
                            .put("quantity", 0.00)
                            .put("detail", "Comprobante de entrega de paquetería")
                            .put("unit_price", 0.00)
                            .put("amount", 0.00)
                            .put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                for(int i = 0; i < details.size(); i++){
                    if (internalCustomer){
                        details.getJsonObject(i).put(AMOUNT, 0.00).put("unit_price", 0.00);
                    }
                    inserts.add(this.generateGenericCreate("tickets_details", details.getJsonObject(i)));
                }

                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                    try {
                        if (replyInsert.failed()){
                            throw new Exception(replyInsert.cause());
                        }
                        future.complete(replyInsert.succeeded());
                    }catch(Exception e ){
                        future.completeExceptionally(e);
                    }
                });


            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private void cancelRegisterGuiapp(Message<JsonObject> message) {

        JsonObject parcel_prepaid_cancel = message.body();
        this.startTransaction(message, (SQLConnection conn) -> {
            this.cancelParcelsPrepaidDetail(conn, parcel_prepaid_cancel).whenComplete((resultRegister, errorRegister) -> {
                try {
                    if (errorRegister != null){
                        throw errorRegister;
                    }
                    this.insertTicket(conn, parcel_prepaid_cancel.getInteger("cashOutId"), parcel_prepaid_cancel.getInteger("parcel_prepaid_id"), 0.0, null,  parcel_prepaid_cancel.getInteger("canceled_by"), 0.0, "cancel")
                            .whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                try {
                                    if (ticketError != null) {
                                        throw new Exception(ticketError);
                                    }

                                    JsonObject ticketDetail = new JsonObject();
                                    ticketDetail.put("ticket_id", ticket.getInteger("ticket_id"));
                                    ticketDetail.put("quantity", 1);
                                    ticketDetail.put("detail", "Comprobante de cancelación de paquetería");
                                    ticketDetail.put("unit_price", 0.0);
                                    ticketDetail.put("amount", 0.0);
                                    ticketDetail.put("created_by", parcel_prepaid_cancel.getInteger("canceled_by"));

                                    String insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);
                                    conn.update(insertTicketDetail, replyInsertTicketDetail -> {
                                        try {
                                            if (replyInsertTicketDetail.failed()) {
                                                throw new Exception(replyInsertTicketDetail.cause());
                                            }
                                            this.commit(conn, message,new JsonObject().put("ticket_id",ticket.getInteger("ticket_id")));

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            this.rollback(conn, e, message);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                } catch (Throwable t){
                    t.printStackTrace();
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> cancelParcelsPrepaidDetail(SQLConnection conn, JsonObject parcel_prepaid_cancel) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String update ="UPDATE parcels_prepaid SET parcel_status = 4 "+",canceled_at=NOW() ,canceled_by ="+parcel_prepaid_cancel.getInteger("canceled_by")+" , parcels_cancel_reason_id ="+parcel_prepaid_cancel.getInteger("parcels_cancel_reason_id") +" WHERE (id = "+parcel_prepaid_cancel.getInteger("parcel_prepaid_id")+");";

            conn.update(update, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.succeeded()) {
                        String updateDetail ="UPDATE parcels_prepaid_detail SET parcel_status = 4 "+",canceled_at=NOW() ,canceled_by ="+parcel_prepaid_cancel.getInteger("canceled_by")+"  WHERE (parcel_prepaid_id = "+parcel_prepaid_cancel.getInteger("parcel_prepaid_id")+" AND parcel_status=0 );";
                        conn.update(updateDetail, (AsyncResult<UpdateResult> reply2) -> {
                            try {
                                if (reply2.succeeded()) {
                                    JsonObject guiappResult = new JsonObject();
                                    // final int id = reply.result().getKeys().getInteger(0);
                                    guiappResult.put("data", true);
                                    future.complete(guiappResult);
                                } else {
                                    future.completeExceptionally(reply2.cause());
                                }
                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }
    private void getListGuiappCode (Message<JsonObject> message) {
        String guiappCode = message.body().getString("code");
        JsonArray param = new JsonArray().add(guiappCode);
        this.dbClient.queryWithParams(QUERY_GET_LIST_GUIAPP_CODE, param, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    List<JsonObject>result = reply.result().getRows();
                    JsonObject parcel = new  JsonObject();
                    parcel.put("data",result);
                    message.reply(parcel);
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void getListGuiappCodev2 (Message<JsonObject> message) {
        try{
            String guiappCode = message.body().getString("code");
            JsonArray param = new JsonArray().add(guiappCode);
            this.dbClient.queryWithParams(QUERY_GET_LIST_GUIAPP_CODE, param, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().getNumRows() == 0) {
                        message.reply(null);
                    } else {
                        JsonObject result = reply.result().getRows().get(0);
                        message.reply(result);
                    }
                } else {
                    reportQueryError(message, reply.cause());
                }
            });
        }  catch (Exception ex) {
        reportQueryError(message, ex);
    }
    }

    private void getListGuiaPpByCustomer (Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer rangeId = body.getInteger("range_id");
        Integer kmId = body.getInteger("km_id");
        boolean countOnly = body.getBoolean("count_only", false);
        String query = QUERY_GET_CODES_FROM;

        JsonArray params = new JsonArray()
                .add(body.getInteger("customer_id"));

        if(body.getString("guiapp_code").isEmpty()) {
            Integer status = body.getInteger("filter");
            params.add(status);
            query = query.concat(" AND pp.parcel_status = ?");
            switch(status) {
                case 0:
                    query = query.concat(" AND pp.parcel_id IS NULL AND pp.expire_at > NOW()");
                    break;
                case 1:
                    query = query.concat(" AND pp.parcel_id IS NOT NULL");
                    break;
                case 4:
                    query = query.concat(" OR pp.expire_at < NOW()");
                    break;
            }

            if(rangeId != null && rangeId != 0) {
                query = query.concat(" AND pp.price_id = ?");
                params.add(body.getInteger("range_id"));
            }
            if(kmId != null && kmId != 0) {
                query = query.concat(" AND pp.price_km_id = ?");
                params.add(body.getInteger("km_id"));
            }
        } else {
            params.add(body.getString("guiapp_code"));
            query = query.concat(" AND pp.guiapp_code = ?");
        }
        // Copy query to count
        String queryCount = "SELECT COUNT(DISTINCT pp.id) AS items ".concat(query);
        JsonArray paramsCount = params.copy();

        if(countOnly) {
            this.dbClient.queryWithParams(queryCount, paramsCount, replyCount -> {
                try {
                    if (replyCount.failed()) {
                        throw replyCount.cause();
                    }
                    Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                    JsonObject result = new JsonObject()
                            .put("count", count);
                    message.reply(result);
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } else {
            String queryItems = QUERY_SELECT_CODES.concat(query);

            // Add the limit for pagination
            int page = Integer.parseInt(String.valueOf(body.getInteger(PAGE, 1)));
            int limit = Integer.parseInt(String.valueOf(body.getInteger(LIMIT, Integer.valueOf(MAX_LIMIT.toString()))));
            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }
            int skip = limit * (page-1);
            queryItems = queryItems.concat(" LIMIT ?,? ");
            params.add(skip).add(limit);

            String finalQueryItems = queryItems;

            this.dbClient.queryWithParams(queryCount, paramsCount, replyCount -> {
                try{
                    if (replyCount.failed()) {
                        throw replyCount.cause();
                    }
                    this.dbClient.queryWithParams(finalQueryItems, params, handler->{
                        try{
                            if (handler.failed()){
                                throw handler.cause();
                            }
                            Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                            List<JsonObject> items = handler.result().getRows();
                            JsonObject result = new JsonObject()
                                    .put("count", count)
                                    .put("items", items.size())
                                    .put("results", items);
                            message.reply(result);
                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });

                } catch (Throwable ex) {
                    reportQueryError(message, ex.getCause());
                }
            });
        }
    }

    private void getDetailGuia(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray().add(body.getInteger("parcel_id"));

        this.dbClient.queryWithParams(QUERY_DETAIL_GUIA, params, handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }
                if(handler.result().getNumRows()>0){
                    JsonObject result = new JsonObject();
                    result.put("result", handler.result().getRows());
                    message.reply(result);
                }else{
                    message.reply(null);
                }
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void getCustomerRanges(Message<JsonObject> message) {
        JsonObject body = message.body();
        boolean onlyAvailables = body.getBoolean("only_availables", false);
        String QUERY = onlyAvailables ? QUERY_GET_AVAILABLE_CUSTOMER_RANGES : QUERY_GET_CUSTOMER_RANGES;
        JsonArray params = new JsonArray().add(body.getInteger("customer_id"));

        this.dbClient.queryWithParams(QUERY, params, handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }

                List<JsonObject> result = handler.result().getRows();
                if (result.isEmpty()) {
                    message.reply(new JsonArray());
                } else {
                    message.reply(new JsonArray(result));
                }
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void getCustomerKms(Message<JsonObject> message) {
        JsonObject body = message.body();
        boolean onlyAvailables = body.getBoolean("only_availables", false);
        String QUERY = onlyAvailables ? QUERY_GET_AVAILABLE_CUSTOMER_KMS : QUERY_GET_CUSTOMER_KMS;
        JsonArray params = new JsonArray().add(body.getInteger("customer_id"));

        this.dbClient.queryWithParams(QUERY, params, handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }

                List<JsonObject> result = handler.result().getRows();
                if (result.isEmpty()) {
                    message.reply(new JsonArray());
                } else {
                    message.reply(new JsonArray(result));
                }
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void multipleValidation(Message<JsonObject> message) {

        JsonArray codes = message.body().getJsonArray(_CODES);
        String codesParam = codes.stream()
                .filter(Objects::nonNull)
                .map(c -> "'" + c + "'")
                .collect(Collectors.joining(","));
        String QUERY = String.format(GET_MULTIPLE_CODE_GUIAPP, codesParam);

        this.dbClient.query(QUERY, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                List<JsonObject> response = new ArrayList<>();

                if(result.isEmpty()){
                    for (Object c : codes) {
                        String code = c.toString();
                        response.add(new JsonObject()
                                .put("code", code)
                                .putNull("info")
                                .put("selected", false)
                                .put("validation", new JsonObject()
                                        .put("is_error", true)
                                        .put("message", "Element not found")));
                    }
                    message.reply(new JsonArray(response));
                    return;
                }

                List<String> codesNotFounded = codes.stream()
                        .filter(codeParam -> result.stream()
                                .noneMatch(code -> code.getString("guiapp_code").equals(codeParam)))
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                for (String code : codesNotFounded) {
                    response.add(new JsonObject()
                            .put("code", code)
                            .putNull("info")
                            .put("selected", false)
                            .put("validation", new JsonObject()
                                    .put("is_error", true)
                                    .put("message", "Element not found")));
                }

                for (JsonObject codeInfo : result) {
                    String code = codeInfo.getString("guiapp_code");
                    Integer ppStatus = codeInfo.getInteger("pp_status");
                    Integer ppParcelStatus = codeInfo.getInteger("pp_parcel_status");
                    Integer ppdStatus = codeInfo.getInteger("ppd_status");
                    Integer ppdParcelStatus = codeInfo.getInteger("ppd_parcel_status");
                    Integer parcelId = codeInfo.getInteger("parcel_id");
                    String parcelWaybill = codeInfo.getString("parcel_waybill");
                    Date expireAt = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(codeInfo.getString("expire_at_prepaid_detail"));
                    Date now = Calendar.getInstance().getTime();

                    if (ppParcelStatus == 4 || ppdParcelStatus == 4 || ppStatus == 4 || ppdStatus == 4) {
                        response.add(new JsonObject()
                                .put("code", code)
                                .putNull("info")
                                .put("selected", false)
                                .put("validation", new JsonObject()
                                        .put("is_error", true)
                                        .put("message", "Code was canceled")));
                    } else if (parcelId != null) {
                        response.add(new JsonObject()
                                .put("code", code)
                                .putNull("info")
                                .put("selected", false)
                                .put("validation", new JsonObject()
                                        .put("is_error", true)
                                        .put("message", "Code was exchanged in " + parcelWaybill)));
                    } else if (UtilsDate.isGreaterThan(now, expireAt)) {
                        response.add(new JsonObject()
                                .put("code", code)
                                .putNull("info")
                                .put("selected", false)
                                .put("validation", new JsonObject()
                                        .put("is_error", true)
                                        .put("message", "Code was expire")));
                    } else {
                        response.add(new JsonObject()
                                .put("code", code)
                                .put("info", codeInfo)
                                .put("selected", false)
                                .put("validation", new JsonObject()
                                        .put("is_error", false)
                                        .put("message", "Valid")));
                    }

                }

                message.reply(new JsonArray(response));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });


    }

    //<editor-fold defaultstate="collapsed" desc="queries">


    private static final String GET_CODE_GUIAPP = "SELECT \n" +
            "   pp.id, \n" +
            "    pp.tracking_code as parcels_tracking_code, \n" +
            "    pp.shipment_type, \n" +
            "   pp.payment_condition, pp.purchase_origin, pp.customer_id, pp.crated_by,\n" +
            "   pp.created_at, pp.updated_by, pp.updated_at, pp.parcel_status,\n" +
            "   pp.cash_register_id, pp.total_count_guipp, \n" +
            "   pp.total_count_guipp_remaining, pp.promo_id, pp.amount, \n" +
            "   pp.discount, pp.has_insurance, pp.insurance_value, pp.insurance_amount, \n" +
            "   pp.extra_charges, pp.iva, pp.parcel_iva, pp.total_amount, \n" +
            "   pp.schedule_route_destination_id,\n" +
            "   pp.expire_at as parcels_expire,\n" +
            "   ppd.id as id_parcel_prepaid_detail, ppd.guiapp_code,\n" +
            "   ppd.ticket_id, ppd.branchoffice_id_exchange, \n" +
            "   ppd.customer_id_exchange, ppd.price_km, ppd.price_km_id, ppd.price, \n" +
            "   ppd.price_id, ppd.amount, ppd.discount, \n" +
            "   ppd.total_amount, ppd.crated_by, ppd.created_at, ppd.updated_by, ppd.updated_at, ppd.status, \n" +
            "   ppd.schedule_route_destination_id, \n" +
            "   ppd.parcel_status,\n" +
            "   ppd.package_type_id,pt.shipping_type, ppd.expire_at as expire_at_prepaid_detail, \n" +
            "   pp.status as pp_status, \n" +
            "   pp.parcel_status as pp_parcel_status, \n" +
            "   ppd.status as ppd_status, \n" +
            "   ppd.is_old as ppd_is_old, \n" +
            "   ppd.parcel_status as ppd_parcel_status, \n" +
            "   p.id as parcel_id, \n" +
            "   pp.total_amount as parcel_prepaid_total_amount, \n" +
            "   p.waybill as parcel_waybill, \n" +
            "   pp.percent_discount_applied, \n" +
            "   pp.total_count_guipp, \n" +
            "    pppricekm.min_km, pppricekm.max_km,\n" +
            "    ppprice.name_price, ppprice.min_weight, ppprice.max_weight, ppprice.min_m3, ppprice.max_m3\n" +
            "FROM parcels_prepaid as pp \n" +
            "INNER JOIN parcels_prepaid_detail as ppd on ppd.parcel_prepaid_id=pp.id\n" +
            "INNER JOIN pp_price ppprice on ppprice.id = ppd.price_id\n" +
            "INNER JOIN pp_price_km pppricekm on pppricekm.id = ppd.price_km_id\n" +
            "INNER JOIN package_types as pt  on pt.id=ppd.package_type_id\n" +
            "LEFT JOIN parcels p on p.id = ppd.parcel_id\n" +
            "WHERE pp.total_count_guipp>pp.total_count_guipp_remaining\n" +
            "   AND ppd.guiapp_code = ?;";

    private static final String GET_MULTIPLE_CODE_GUIAPP = "SELECT \n" +
            "   pp.id, \n" +
            "    pp.tracking_code as parcels_tracking_code, \n" +
            "    pp.shipment_type, \n" +
            "   pp.payment_condition, pp.customer_id, \n" +
            "   pp.created_at, pp.updated_by, pp.updated_at, pp.parcel_status,\n" +
            "   pp.total_count_guipp, \n" +
            "   pp.total_count_guipp_remaining, pp.promo_id, pp.amount, \n" +
            "   pp.discount, pp.has_insurance, pp.insurance_value, pp.insurance_amount, \n" +
            "   pp.extra_charges, pp.iva, pp.parcel_iva, pp.total_amount, \n" +
            "   pp.schedule_route_destination_id,\n" +
            "   pp.expire_at as parcels_expire,\n" +
            "   ppd.id as id_parcel_prepaid_detail, ppd.guiapp_code,\n" +
            "   ppd.ticket_id, ppd.branchoffice_id_exchange, \n" +
            "   ppd.customer_id_exchange, ppd.price_km, ppd.price_km_id, ppd.price, \n" +
            "   ppd.price_id, ppd.amount, ppd.discount, \n" +
            "   ppd.total_amount, ppd.status, \n" +
            "   ppd.parcel_status,\n" +
            "   ppd.package_type_id,pt.shipping_type, ppd.expire_at as expire_at_prepaid_detail, \n" +
            "   pp.status as pp_status, \n" +
            "   pp.parcel_status as pp_parcel_status, \n" +
            "   ppd.status as ppd_status, \n" +
            "   ppd.is_old as ppd_is_old, \n" +
            "   ppd.parcel_status as ppd_parcel_status, \n" +
            "   p.id as parcel_id, \n" +
            "   pp.total_amount as parcel_prepaid_total_amount, \n" +
            "   p.waybill as parcel_waybill, \n" +
            "   pp.percent_discount_applied, \n" +
            "   pp.total_count_guipp, \n" +
            "    pppricekm.min_km, pppricekm.max_km,\n" +
            "    ppprice.name_price, ppprice.min_weight, ppprice.max_weight, ppprice.min_m3, ppprice.max_m3\n" +
            "FROM parcels_prepaid as pp \n" +
            "INNER JOIN parcels_prepaid_detail as ppd on ppd.parcel_prepaid_id=pp.id\n" +
            "INNER JOIN pp_price ppprice on ppprice.id = ppd.price_id\n" +
            "INNER JOIN pp_price_km pppricekm on pppricekm.id = ppd.price_km_id\n" +
            "INNER JOIN package_types as pt  on pt.id=ppd.package_type_id\n" +
            "LEFT JOIN parcels p on p.id = ppd.parcel_id\n" +
            "WHERE pp.total_count_guipp>pp.total_count_guipp_remaining\n" +
            "   AND ppd.guiapp_code IN (%s);";
    private static final String GET_TERMIANLS_DISTANCE="SELECT * FROM package_terminals_distance";

    private static final  String QUERY_PARCEL_BY_TRACKING_CODE = "SELECT a.*," +
            "concat(c.first_name , ' ' , c.last_name) AS customer_name, " +
            "CONCAT(e.name, ' ', e.last_name) as created_name," +
            "iv.document_id,\n" +
            "iv.media_document_pdf_name,\n"+
            "iv.media_document_xml_name,\n"+
            "COUNT(debp.id) AS debt_payments_quantity,\n" +
            "SUM(debp.amount) AS debt_payments, " +
            "t.cash_out_id\n"+
            "FROM parcels_prepaid a \n" +
            "left join employee e on e.user_id = a.crated_by \n" +
            "LEFT JOIN customer c ON c.id = a.customer_id \n" +
            "LEFT JOIN tickets AS t ON t.parcel_prepaid_id = a.id " +
            "LEFT JOIN invoice AS iv ON iv.id = a.invoice_id\n" +
            "LEFT JOIN debt_payment debp ON debp.parcel_id = a.id " +
            "where a.tracking_code = ? " +
            "GROUP BY a.id, e.name, e.last_name \n" +
            "order by a.created_at;";

    private static final  String QUERY_PARCELS_PACKAGES_BY_ID_PARCEL = "SELECT a.*, d.name_price AS package_price_name,pp.id as parcels_id,pp.parcel_tracking_code " +
            "FROM parcels_prepaid_detail a \n" +
            "left join pp_price as d ON d.id = a.price_id " +
            "left join parcels as pp ON pp.id = a.parcel_id " +
            "where a.parcel_prepaid_id = ? and a.status = 1 order by a.created_at";

    private static final String QUERY_VALID_PACKAGE = "select distinct * from pp_price \n" +
            "where shipping_type = ? \n" +
            "and id = ? ;";

    private static final String QUERY_GET_SHIPPING_TYPE = "select *\n" +
            "from package_types \n" +
            "where shipping_type = ? limit 1";
    private static final String QUERY_UPDATE_USAGE_GUIAPP = "UPDATE parcels_prepaid_detail SET parcel_status=1, parcel_id= ";
    private static final String GET_CODE_GUIAPP_ALL = "Select  pp.id, pp.tracking_code as parcels_tracking_code, pp.shipment_type, \n" +
            " pp.payment_condition, pp.purchase_origin, pp.customer_id, pp.crated_by,\n" +
            " pp.created_at, pp.updated_by, pp.updated_at, pp.parcel_status,\n" +
            " pp.cash_register_id, pp.total_count_guipp, \n" +
            " pp.total_count_guipp_remaining, pp.promo_id, pp.amount, \n" +
            " pp.discount, pp.has_insurance, pp.insurance_value, pp.insurance_amount, \n" +
            " pp.extra_charges, pp.iva, pp.parcel_iva, pp.total_amount, \n" +
            " pp.schedule_route_destination_id,\n" +
            " pp.expire_at as parcels_expire,\n" +
            " ppd.id as id_parcel_prepaid_detail, ppd.guiapp_code,\n" +
            " ppd.ticket_id, ppd.branchoffice_id_exchange, \n" +
            " ppd.customer_id_exchange, ppd.price_km, ppd.price_km_id, ppd.price, \n" +
            " ppd.price_id, ppd.amount, ppd.discount, \n" +
            " ppd.total_amount, ppd.crated_by, ppd.created_at, ppd.updated_by, ppd.updated_at, ppd.status, \n" +
            " ppd.schedule_route_destination_id, \n" +
            " ppd.parcel_status,\n" +
            " ppd.package_type_id,pt.shipping_type, ppd.expire_at as expire_at_prepaid_detail\n" +
            " from parcels_prepaid as pp \n" +
            "inner join parcels_prepaid_detail as ppd on ppd.parcel_prepaid_id=pp.id\n" +
            "inner join package_types as pt  on pt.id=ppd.package_type_id\n" +
            " where (pp.status!=4 and pp.parcel_status=1 and ppd.status!=4 and ppd.parcel_status=0 )" +
            " and pp.total_count_guipp>pp.total_count_guipp_remaining"+
            " and ppd.expire_at > NOW() and ppd.guiapp_code ";
              private  static final String QUERY_PARCEL_PACKAGE_INCIDENCES ="SELECT\n" +
            "   a.id,\n" +
            "   a.parcel_package_id,\n" +
            "   a.incidence_id,\n" +
            "   b.name,\n" +
            "   a.notes,\n" +
            "   a.status,\n" +
            "   a.created_at,\n" +
            "   a.created_by,\n" +
            "   e.name AS created_employee_name,\n" +
            "   e.last_name AS created_employee_last_name,\n" +
            "   a.updated_at,\n" +
            "   a.updated_by\n" +
            " FROM parcels_incidences a\n" +
            " INNER JOIN incidences b ON b.id = a.incidence_id\n" +
            " LEFT JOIN employee e ON e.id = a.created_by\n" +
            " WHERE a.parcel_package_id = ? AND a.status = 1 ORDER BY a.created_at;";
    private static final String GET_PARCELS_PACKAGES_TRACKING_BY_PARCEL_ID = "SELECT ppt.*," +
            " city.name AS terminal_city_name, state.name AS terminal_state_name, " +
            " bo.prefix AS terminal_prefix, bo.name AS terminal_name, " +
            " p.notes AS parcel_notes, " +
            " CONCAT(e.name, ' ', e.last_name) as created_name, pp.package_code " +
            " FROM parcels_packages_tracking as ppt " +
            " LEFT JOIN employee e ON e.user_id = ppt.created_by" +
            " LEFT JOIN parcels p ON p.id = ppt.parcel_id" +
            " LEFT JOIN branchoffice AS bo ON ppt.terminal_id = bo.id " +
            " LEFT JOIN city ON bo.city_id = city.id " +
            " LEFT JOIN state ON bo.state_id = state.id " +
            " LEFT JOIN parcels_packages AS pp ON pp.id=ppt.parcel_package_id\n" +
            " LEFT JOIN package_types AS pt ON pt.id=pp.package_type_id" +
            " WHERE ppt.parcel_id = ? ORDER BY ppt.id DESC";
    private static final String GET_PARCELS_DELIVERIES_BY_PARCELS_PACKAGE = "SELECT pp.id as parcels_packages_id, pd.* FROM parcels_packages pp\n" +
            "LEFT JOIN parcels_deliveries pd on pd.id = pp.parcels_deliveries_id\n" +
            "where pp.package_status = 2 AND pp.id IN (?);";
    private static final String QUERY_PARCEL_CANCEL_DETAIL_BY_ID = "SELECT\n" +
            "u.name canceled_by,\n" +
            "p.updated_by canceled_by_id,\n" +
            "p.cancel_code,\n" +
            "pcr.name reason,\n" +
            "pp.notes\n" +
            "FROM parcels p\n" +
            "INNER JOIN parcels_cancel_reasons pcr ON pcr.id = p.parcels_cancel_reason_id\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "INNER JOIN parcels_packages_tracking ppt ON ppt.parcel_id = p.id\n" +
            "INNER JOIN users u ON u.id = ppt.created_by\n" +
            "WHERE ppt.action = 'deliveredcancel'\n" +
            "AND p.id = ?;";
    private static final String QUERY_PARCELS_RAD_BY_ID_PARCEL_PRINT = "SELECT\n" +
            "pre.amount ,\n" +
            "pre.created_at,\n" +
            "pre.created_by, \n" +
            "pst.type_service,\n" +
            "pre.id , \n" +
            "pre.id_type_service,\n" +
            "pre.parcel_id ,\n" +
            "pre.status,\n" +
            "pre.zip_code\n" +
            "FROM parcels_rad_ead pre\n" +
            "LEFT JOIN parcel_service_type pst ON pre.id_type_service = pst.id\n" +
            "WHERE pre.parcel_id = ? limit 1 ";
    private static final  String QUERY_PARCEL_BY_TRACKING_CODE_CANCEL = "SELECT ppd.id as id_parcels_prepaid_detail , a.*, CONCAT(b.prefix, a.parcel_tracking_code) AS parcel_code," +
            "b.prefix as terminal_origin_prefix, b.name as terminal_origin_name, b.address AS terminal_origin_address, " +
            "c.prefix as terminal_destiny_prefix, c.name as terminal_destiny_name, " +
            "ca_sender.address AS sender_address, " +
            "ca_addressee.address AS addressee_address, " +
            "i.policy_number, i.insurance_carrier, SUM(t.total) AS paid, \n" +
            "srd.travel_date, " +
            "srd.arrival_date, " +
            "srd.started_at, " +
            "srd.finished_at, " +
            "CONCAT(e.name, ' ', e.last_name) as created_name," +
            "iv.document_id,\n" +
            "iv.media_document_pdf_name,\n"+
            "iv.media_document_xml_name,\n"+
            "COUNT(debp.id) AS debt_payments_quantity,\n" +
            "SUM(debp.amount) AS debt_payments, " +
            "t.cash_out_id\n"+
            "FROM parcels a \n" +
            "inner join branchoffice b on b.id = a.terminal_origin_id \n" +
            "inner join branchoffice c on c.id = a.terminal_destiny_id \n" +
            "INNER JOIN schedule_route_destination AS srd ON srd.id = a.schedule_route_destination_id \n" +
            "left join employee e on e.user_id = a.created_by \n" +
            "LEFT JOIN customer_addresses ca_sender ON ca_sender.id = a.sender_address_id \n" +
            "LEFT JOIN customer_addresses ca_addressee ON ca_addressee.id = a.addressee_address_id \n" +
            "left join insurances as i ON i.id = a.insurance_id " +
            "LEFT JOIN tickets AS t ON t.parcel_id = a.id " +
            "LEFT JOIN invoice AS iv ON iv.id = a.invoice_id\n" +
            "LEFT JOIN debt_payment debp ON debp.parcel_id = a.id " +
            "LEFT JOIN parcels_prepaid_detail ppd ON ppd.parcel_prepaid_id=a.id \n" +
            "where a.parcel_tracking_code = ? " +
            "GROUP BY a.id, e.name, e.last_name \n" +
            "order by a.created_at;";

    private static final String QUERY_SALES_REPORT = "SELECT DISTINCT p.id,  p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city,  p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, IF(ISNULL(em.name), 'N/A', em.name) AS seller_name, \n" +
            " p.payment_condition, p.tracking_code, p.cancel_code, \n" +
            " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount, p.total_count_guipp, p.total_count_guipp_remaining,p.created_at\n" +
            " FROM parcels_prepaid AS p \n" +
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " INNER JOIN parcels_prepaid_detail AS pp ON pp.parcel_prepaid_id = p.id\n" +
            " LEFT JOIN users AS em ON em.id = p.seller_user_id\n" +
            " WHERE p.parcel_status != 4  AND p.created_at BETWEEN ? AND ? ";

    private static final String SELLER_NAME_ID = " AND p.seller_user_id = ? ";
    private static final String SALES_REPORT_ORDER_BY = " ORDER BY p.created_at";
    private static final String QUERY_SALES_REPORT_PACKAGES = "SELECT   pp.id, pp.package_type_id, pty.name_price AS package_type , ppr.min_km, ppr.max_km\n" +
            " FROM parcels_prepaid_detail AS pp \n" +
            " LEFT JOIN pp_price AS pty ON pty.id = pp.price_id\n" +
            " LEFT JOIN pp_price_km AS ppr ON ppr.id = pp.price_km_id\n" +
            " WHERE pp.parcel_prepaid_id = ? ";
    private static final String QUERY_SALES_REPORT_PAYMENT_INFO = "SELECT\n" +
            "   p.payment_method_id,\n" +
            "   p.payment_method,\n" +
            "   p.amount,\n" +
            "   pm.name,\n" +
            "   pm.is_cash,\n" +
            "   pm.alias,\n" +
            "   pm.icon,\n" +
            "   p.created_at\n" +
            " FROM payment p \n" +
            " LEFT JOIN payment_method pm ON pm.id = p.payment_method_id\n" +
            " WHERE p.parcel_prepaid_id = ?;";
    private static final String QUERY_SALES_REPORT_TOTALS = "SELECT \n" +
            " COALESCE(SUM(IF(t.payment_condition = 'cash', t.total_amount, 0)), 0) AS cash,\n" +
            " COALESCE(SUM(IF(t.payment_condition = 'credit', t.total_amount, 0)), 0) AS credit,\n" +
            " COALESCE(SUM(IF(t.payment_condition = 'cash', t.total_count_guipp, 0)), 0) AS cash_packages,\n" +
            " COALESCE(SUM(IF(t.payment_condition = 'credit', t.total_count_guipp, 0)), 0) AS credit_packages,\n" +
            " COUNT(t.id) AS total\n" +
            " FROM (";

    private static final String QUERY_AVAILABLE_REPORT = "SELECT \n" +
            "    pp.id,\n" +
            "    pp.created_at AS sale_date,\n" +
            "    pp.tracking_code AS tracking_code,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "    pp.total_count_guipp AS total,\n" +
            "    pp.total_amount AS total_amount,\n" +
            "    (pp.total_amount / pp.total_count_guipp) * COALESCE(SUM(CASE WHEN ppd.parcel_status = 0 THEN 1 ELSE 0 END), 0) AS amount_for_available,\n" +
            "    COALESCE(SUM(CASE WHEN ppd.parcel_status IN (1, 4) THEN 1 ELSE 0 END), 0) AS total_redeemed,\n" +
            "    COALESCE(SUM(CASE WHEN ppd.parcel_status = 0 THEN 1 ELSE 0 END), 0) AS total_available,\n" +
            "    GROUP_CONCAT(DISTINCT CONCAT(pty.name_price, ' desde ', ppr.min_km, ' km hasta ', ppr.max_km, ' km') SEPARATOR '; ') AS rate,\n" +
            "     COALESCE(u.name, '') AS user_seller_name,\n" +
            "    COALESCE(b.prefix, '') AS place\n" +
            "FROM parcels_prepaid pp\n" +
            "LEFT JOIN customer c ON pp.customer_id = c.id\n" +
            "LEFT JOIN parcels_prepaid_detail ppd ON pp.id = ppd.parcel_prepaid_id\n" +
            "LEFT JOIN pp_price pty ON ppd.price_id = pty.id\n" +
            "LEFT JOIN pp_price_km ppr ON ppd.price_km_id = ppr.id\n" +
            "LEFT JOIN users u ON u.id = c.user_seller_id\n" +
            "LEFT JOIN branchoffice b ON b.id = c.branchoffice_id\n" +
            "WHERE pp.parcel_status != 4";

    private static final String QUERY_AVAILABLE_REPORT_TOTAL = "SELECT \n" +
            " COUNT(t.id) AS total\n" +
            " FROM (";

    private static final String QUERY_AVAILABLE_REPORT_PACKAGES = "SELECT   pp.id, pp.package_type_id, pty.name_price AS package_type , pp.parcel_status , ppr.min_km, ppr.max_km\n" +
            " FROM parcels_prepaid_detail AS pp \n" +
            " LEFT JOIN pp_price AS pty ON pty.id = pp.price_id\n" +
            " LEFT JOIN pp_price_km AS ppr ON ppr.id = pp.price_km_id\n" +
            " WHERE pp.parcel_prepaid_id = ? ";
    private static final  String QUERY_PARCEL_BY_TRACKING_CODE_CANCEL_GUIAPP_ALL="select pp.id as parcels_prepaid_id,b.prefix,pp.payment_condition,pp.purchase_origin,pp.created_at,pp.parcel_status,pp.shipment_type, c.id as addressee_id \n" +
            " ,pp.tracking_code,t.id as ticket_id, c.first_name,c.last_name from \n" +
            "parcels_prepaid as pp\n" +
            "INNER JOIN  parcels_prepaid_detail as ppd on  ppd.parcel_prepaid_id=pp.id\n" +
            "LEFT JOIN tickets as t on t.parcel_prepaid_id=pp.id \n" +
            "LEFT JOIN customer as c on c.id=pp.customer_id\n" +
            "LEFT JOIN branchoffice as b on b.id=pp.branchoffice_id\n" +
            "where pp.tracking_code = ?";
    private static final  String QUERY_GET_LIST_GUIAPP_CODE="SELECT \n" +
            "    p.id,\n" +
            "    p.tracking_code AS parcels_tracking_code,\n" +
            "    p.shipment_type,\n" +
            "    p.payment_condition,\n" +
            "    p.purchase_origin,\n" +
            "    p.customer_id,\n" +
            "    p.crated_by,\n" +
            "    p.created_at,\n" +
            "    p.updated_by,\n" +
            "    p.updated_at,\n" +
            "    p.parcel_status,\n" +
            "    p.cash_register_id,\n" +
            "    p.total_count_guipp,\n" +
            "    p.total_count_guipp_remaining,\n" +
            "    p.promo_id,\n" +
            "    p.amount,\n" +
            "    p.discount,\n" +
            "    p.has_insurance,\n" +
            "    p.insurance_value,\n" +
            "    p.insurance_amount,\n" +
            "    p.extra_charges,\n" +
            "    p.iva,\n" +
            "    p.parcel_iva,\n" +
            "    p.total_amount,\n" +
            "    p.schedule_route_destination_id,\n" +
            "    p.expire_at AS parcels_expire,\n" +
            "    pp.id AS id_parcel_prepaid_detail,\n" +
            "    pp.guiapp_code,\n" +
            "    pp.ticket_id,\n" +
            "    pp.branchoffice_id_exchange,\n" +
            "    pp.customer_id_exchange,\n" +
            "    pp.price_km,\n" +
            "    pp.price_km_id,\n" +
            "    pp.price,\n" +
            "    pp.price_id,\n" +
            "    pp.amount,\n" +
            "    pp.discount,\n" +
            "    pp.total_amount,\n" +
            "    pp.crated_by,\n" +
            "    pp.created_at,\n" +
            "    pp.updated_by,\n" +
            "    pp.updated_at,\n" +
            "    pp.status,\n" +
            "    pp.schedule_route_destination_id,\n" +
            "    pp.parcel_status,\n" +
            "    pp.package_type_id,\n" +
            "    pt.shipping_type,\n" +
            "    pp.parcel_status,\n" +
            "    pp.expire_at AS expire_at_prepaid_detail,\n" +
            "    pa.parcel_tracking_code\n" +
            "    FROM\n" +
            "    parcels_prepaid_detail AS pp\n" +
            "        LEFT JOIN\n" +
            "    parcels_prepaid AS p ON pp.parcel_prepaid_id = p.id\n" +
            "        INNER JOIN\n" +
            "    package_types AS pt ON pt.id = pp.package_type_id\n" +
            "\n" +
            "    LEFT OUTER JOIN parcels as pa ON pp.parcel_id=pa.id\n" +
            "    \n" +
            "WHERE\n" +
            "    pp.guiapp_code = ? \n" +
            "        AND (p.status != 4 AND p.parcel_status = 1\n" +
            "        AND pp.status != 4)\n" +
            "        AND  p.total_count_guipp < (select count(id) from parcels_prepaid_detail where status != 4\n" +
            "        AND parcel_status = 0  )        AND pp.expire_at > NOW();";

    private static final String QUERY_GET_CODES_FROM = "\tFROM parcels_prepaid_detail AS pp\n" +
            "\tINNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "\tLEFT JOIN parcels AS pa ON pa.id = pp.parcel_id\n" +
            "\tLEFT JOIN pp_price as d ON d.id = pp.price_id " +
            "\tINNER JOIN pp_price_km AS pkm ON  pkm.id = pp.price_km_id\n" +
            "\tWHERE p.customer_id=? AND pp.status = 1 ";
    private static final String QUERY_SELECT_CODES = "SELECT pp.id, pp.guiapp_code AS tracking_code, p.shipment_type, CONCAT(pkm.min_km, ' - ', pkm.max_km, ' KM') as rango, pa.parcel_tracking_code,\n" +
            "\tCONCAT(pa.addressee_name, ' ', pa.addressee_last_name  ) AS recibe,\n" +
            "\tCONCAT(pa.sender_name, ' ', pa.sender_last_name  ) AS envia, \n" +
            "\tpp.parcel_status AS estatus, \n" +
            "\td.name_price AS package_price_name \n";

    private static final String QUERY_DETAIL_GUIA = "select  ppd.id id_ppd, pp.tracking_code, ppd.guiapp_code,\n" +
            "DATE(pp.created_at) as created_at,\n" +
            "DATE_FORMAT(pp.created_at, \"%H:%i\") as hour,\n" +
            "CONCAT(pkm.min_km, '-', pkm.max_km) as rango,\n" +
            "pprice.name_price as tarifa ,\n" +
            "CONCAT(pprice.min_weight , '-' , pprice.max_weight) as peso,\n" +
            "concat(pprice.min_linear_volume) as m3,\n" +
            "pp.shipment_type,\n" +
            " pp.payment_condition,\n" +
            " CONCAT(c.first_name, ' ', c.last_name) AS name,\n" +
            " c.phone,\n" +
            " ventaBranch.name as 'sucursal_venta'\n" +
            "            from parcels_prepaid_detail ppd\n" +
            "            INNER JOIN parcels_prepaid AS pp ON pp.id = ppd.parcel_prepaid_id\n" +
            "            INNER JOIN pp_price_km AS pkm ON  pkm.id = ppd.price_km_id\n" +
            "            inner join pp_price as pprice ON pprice.id  = ppd.price_id\n" +
            "            INNER JOIN users AS u ON pp.crated_by = u.id\n" +
            "            INNER JOIN customer AS c ON c.id = pp.customer_id\n" +
            "            left join branchoffice as ventaBranch ON ventaBranch.id = pp.branchoffice_id\n" +
            "            WHERE ppd.id = ?;";

    private static final String QUERY_GET_CUSTOMER_RANGES = "SELECT \n" +
            "d.id AS 'id',\n" +
            "d.name_price AS 'range'\n" +
            "FROM pp_price AS d\n" +
            "INNER JOIN parcels_prepaid_detail AS pp ON pp.price_id = d.id\n" +
            "INNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "WHERE \n" +
            "p.customer_id = ?\n" +
            "GROUP BY d.name_price\n" +
            "ORDER BY d.max_m3";

    private static final String QUERY_GET_AVAILABLE_CUSTOMER_RANGES = "SELECT \n" +
            "d.id AS 'id',\n" +
            "d.name_price AS 'range', \n" +
            "COUNT(pp.id) AS 'count'\n" +
            "FROM pp_price AS d\n" +
            "INNER JOIN parcels_prepaid_detail AS pp ON pp.price_id = d.id\n" +
            "INNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "WHERE \n" +
            "pp.parcel_status = 0 \n" +
            "AND pp.status = 1 \n" +
            "AND p.customer_id = ?\n" +
            "AND pp.expire_at > NOW()\n" +
            "GROUP BY d.name_price\n" +
            "ORDER BY d.max_m3";

    private static final String QUERY_GET_CUSTOMER_KMS = "SELECT \n" +
            "pkm.id AS 'id',\n" +
            "CONCAT(pkm.min_km, ' - ', pkm.max_km, ' KM') AS 'kms'\n" +
            "FROM pp_price_km AS pkm\n" +
            "INNER JOIN parcels_prepaid_detail AS pp ON pp.price_km_id = pkm.id\n" +
            "INNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "WHERE \n" +
            "p.customer_id = ?\n" +
            "GROUP BY pkm.min_km, pkm.max_km\n" +
            "ORDER BY pkm.min_km";

    private static final String QUERY_GET_AVAILABLE_CUSTOMER_KMS = "SELECT \n" +
            "pkm.id AS 'id',\n" +
            "CONCAT(pkm.min_km, ' - ', pkm.max_km, ' KM') AS 'kms',\n" +
            "COUNT(pp.id) AS 'count'\n" +
            "FROM pp_price_km AS pkm\n" +
            "INNER JOIN parcels_prepaid_detail AS pp ON pp.price_km_id = pkm.id\n" +
            "INNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "WHERE \n" +
            "p.customer_id = ?\n" +
            "AND pp.status = 1 \n" +
            "AND pp.parcel_status = 0\n" +
            "AND pp.expire_at > NOW()\n" +
            "GROUP BY pkm.min_km, pkm.max_km\n" +
            "ORDER BY pkm.min_km";

    private static final String QUERY_GET_CUSTOMERS_BY_USER_SELLER_ID = "SELECT id FROM customer WHERE id > 0";

    private static final String QUERY_EXCLUDE_ZERO_AMOUNT = " AND p.total_amount > 0";
//</editor-fold>
}
